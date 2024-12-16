package com.sentimentanalysis.core.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.spark.ml.PipelineModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.UserDefinedFunction;
import org.apache.spark.sql.functions;
import static org.apache.spark.sql.functions.udf;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.types.StructField;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

public class SentimentAnalysisService implements SentimentAnalyzer {
    private static SparkSession spark;
    private final Properties config;
    private PipelineModel model;
    private StanfordCoreNLP pipeline;

    public SentimentAnalysisService() {
        config = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            config.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration", e);
        }

        // Initialize Stanford NLP pipeline
        Properties stanfordProps = new Properties();
        stanfordProps.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
        pipeline = new StanfordCoreNLP(stanfordProps);

        // Initialize Spark session
        if (spark == null) {
            spark = SparkSession.builder()
                .appName("SentimentAnalysis")
                .master("local[*]")
                .config("spark.driver.memory", "2g")
                .config("spark.executor.memory", "2g")
                .getOrCreate();
        }

        // Load and use the HDFS model
        try {
            String modelPath = "hdfs://localhost:9000/models/sentiment_analysis_model";
            model = PipelineModel.load(modelPath);
        } catch (Exception e) {
            System.err.println("Failed to load HDFS model: " + e.getMessage());
            // Will fallback to HuggingFace API
        }
    }

    public static class TextRow {
        private String text;
        
        public TextRow() {}
        
        public TextRow(String text) {
            this.text = text;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
    }

    public String analyzeWithHDFSModel(String text) throws Exception {
        // Create schema for DataFrame
        StructType schema = new StructType(new StructField[]{
            new StructField("text", DataTypes.StringType, false, org.apache.spark.sql.types.Metadata.empty())
        });

        // Create UDF for text normalization
        UserDefinedFunction normalizeText = udf((String inputText) -> {
            if (inputText == null) return "";
            inputText = inputText.replaceAll("http[s]?://\\S+", "");
            inputText = inputText.replaceAll("@\\w+", "");
            inputText = inputText.replaceAll("#\\w+", "");
            inputText = inputText.toLowerCase();
            inputText = inputText.replaceAll("[^a-zA-Z0-9\\s]", "");
            inputText = Pattern.compile("(.)\\1{2,}").matcher(inputText).replaceAll("$1$1");
            return inputText;
        }, DataTypes.StringType);

        // Create DataFrame with input text
        List<Row> data = new ArrayList<>();
        data.add(org.apache.spark.sql.RowFactory.create(text));
        Dataset<Row> inputData = spark.createDataFrame(data, schema)
            .withColumn("SentimentText", normalizeText.apply(functions.col("text")));

        // Use the model
        if (model == null) {
            throw new Exception("HDFS model not available. Falling back to HuggingFace API.");
        }
        Dataset<Row> predictions = model.transform(inputData);
        Row prediction = predictions.select("prediction").first();
        double predictionValue = prediction.getDouble(0);
        return predictionValue == 1.0 ? "Positive" : "Negative";
    }

    public String analyzeWithHuggingFaceAPI(String text) throws IOException {
        String apiKey = config.getProperty("api_key");
        String urlString = "https://api-inference.huggingface.co/models/cardiffnlp/twitter-roberta-base-sentiment";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        // Convert the single text input into JSON format for the request body
        String jsonInputString = "{\"inputs\": \"" + text.replace("\"", "\\\"") + "\"}";

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            return parseSentimentResult(content.toString());
        }
    }

    public String analyzeWithStanfordNLP(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be empty");
        }

        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);

        // Get the sentiment scores for each sentence
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        if (sentences == null || sentences.isEmpty()) {
            throw new IllegalStateException("No sentences found in the text");
        }

        // Calculate average sentiment
        double totalSentiment = 0;
        for (CoreMap sentence : sentences) {
            Tree tree = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
            int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
            totalSentiment += sentiment;
        }
        double averageSentiment = totalSentiment / sentences.size();

        // Convert numeric sentiment to text
        // Stanford NLP uses a 5-point scale: 0 (very negative) to 4 (very positive)
        if (averageSentiment <= 1) {
            return "Very Negative";
        } else if (averageSentiment <= 2) {
            return "Negative";
        } else if (averageSentiment < 3) {
            return "Neutral";
        } else if (averageSentiment < 4) {
            return "Positive";
        } else {
            return "Very Positive";
        }
    }

    private String parseSentimentResult(String result) {
        JsonArray jsonArray = JsonParser.parseString(result).getAsJsonArray();
        JsonArray scores = jsonArray.get(0).getAsJsonArray();
        
        double maxScore = Double.NEGATIVE_INFINITY;
        String sentiment = "";
        
        for (JsonElement element : scores) {
            JsonObject score = element.getAsJsonObject();
            double currentScore = score.get("score").getAsDouble();
            if (currentScore > maxScore) {
                maxScore = currentScore;
                String label = score.get("label").getAsString();
                // Convert RoBERTa model labels to human-readable format
                switch (label) {
                    case "LABEL_0":
                        sentiment = "Negative";
                        break;
                    case "LABEL_1":
                        sentiment = "Neutral";
                        break;
                    case "LABEL_2":
                        sentiment = "Positive";
                        break;
                    default:
                        sentiment = label;
                }
            }
        }
        
        return sentiment;
    }

    @Override
    public String analyzeSentiment(String text) {
        try {
            return analyzeWithHDFSModel(text);
        } catch (Exception e) {
            System.err.println("HDFS model failed, falling back to HuggingFace API: " + e.getMessage());
            try {
                return analyzeWithHuggingFaceAPI(text);
            } catch (IOException ex) {
                System.err.println("HuggingFace API failed, falling back to Stanford NLP: " + ex.getMessage());
                return analyzeWithStanfordNLP(text);
            }
        }
    }

    @Override
    public String getName() {
        return "Spark MLlib Model";
    }
}

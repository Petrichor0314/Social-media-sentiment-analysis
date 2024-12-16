package com.sentimentanalysis.core.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sentimentanalysis.util.PythonScriptRunner;
import com.sentimentanalysis.core.service.SentimentAnalysisService;
import com.sentimentanalysis.core.service.HDFSService;

public class RedditService {
    private static final Logger logger = LoggerFactory.getLogger(RedditService.class);
    private static final String PYTHON_SCRIPTS_PATH = "C:/Projects/GUI-Practice/src_new/main/python";
    private static final String BASE_DATA_DIR = "C:/Projects/GUI-Practice/data";
    private static final String RAW_DATA_DIR = BASE_DATA_DIR + "/raw";
    private static final String PREPROCESSED_DATA_DIR = BASE_DATA_DIR + "/preprocessed";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private SentimentAnalysisService sentimentAnalysisService = new SentimentAnalysisService();
    private HDFSService hdfsService = new HDFSService();

    public RedditService() {
        try {
            // Create base directories if they don't exist
            Files.createDirectories(Paths.get(BASE_DATA_DIR));
            Files.createDirectories(Paths.get(RAW_DATA_DIR));
            Files.createDirectories(Paths.get(PREPROCESSED_DATA_DIR));
            logger.info("Initialized base data directories");
        } catch (IOException e) {
            logger.error("Failed to create base directories: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize data directories", e);
        }
    }

    private String createOutputPath(String type, String identifier) {
        try {
            // Create base directory structure: data/raw/[type]/[date]/[identifier]
            String timestamp = LocalDateTime.now().format(DATE_FORMAT);
            
            // Create the directory structure
            Path typeDir = Paths.get(RAW_DATA_DIR, type);
            Path dateDir = typeDir.resolve(timestamp);
            Files.createDirectories(dateDir);
            
            // Create filename with timestamp and identifier
            String filename = String.format("%s_%s.csv", 
                identifier.replaceAll("[^a-zA-Z0-9-_]", "_"), 
                timestamp);
            Path outputPath = dateDir.resolve(filename);
            
            logger.info("Created local output path for raw data: {}", outputPath);
            return outputPath.toString();
        } catch (Exception e) {
            logger.error("Error creating output paths: {}", e.getMessage());
            throw new RuntimeException("Failed to create output directories", e);
        }
    }

    public void savePreprocessedCommentsNIO(String originalFilePath, List<String> comments, List<String> sentiments) throws IOException {
        logger.info("Saving preprocessed data for file: {}", originalFilePath);
        
        // Parse the original file path to get type and identifier
        Path originalPath = Paths.get(originalFilePath);
        String filename = originalPath.getFileName().toString();
        
        // Extract type from the path (e.g., "subreddit", "keyword", "post")
        String type = originalPath.getParent() // timestamp directory
                               .getParent()    // type directory
                               .getFileName()
                               .toString();
        
        // Extract identifier from filename (everything before the first underscore)
        String identifier = filename.split("_")[0];
        
        // Create preprocessed directory structure
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        Path preprocessedTypeDir = Paths.get(PREPROCESSED_DATA_DIR, type);
        Path preprocessedDateDir = preprocessedTypeDir.resolve(timestamp);
        
        try {
            Files.createDirectories(preprocessedDateDir);
            logger.info("Created preprocessed directory: {}", preprocessedDateDir);
        } catch (IOException e) {
            logger.error("Failed to create preprocessed directory: {}", e.getMessage());
            throw e;
        }

        // Create output filename and path
        String outputFileName = String.format("preprocessed_%s_%s.csv", identifier, timestamp);
        Path outputFilePath = preprocessedDateDir.resolve(outputFileName);
        logger.info("Writing preprocessed data to: {}", outputFilePath);

        // Prepare the CSV content with proper escaping
        List<String> linesToWrite = new ArrayList<>();
        linesToWrite.add("Preprocessed_Comment,Sentiment");
        
        // Ensure we have matching numbers of comments and sentiments
        if (comments.size() != sentiments.size()) {
            throw new IOException("Mismatch between comments and sentiments count");
        }
        
        // Write the data with proper CSV escaping
        for (int i = 0; i < comments.size(); i++) {
            String escapedComment = comments.get(i)
                .replace("\"", "\"\"")
                .replace(",", "\",\"");
            String line = String.format("\"%s\",\"%s\"", escapedComment, sentiments.get(i));
            linesToWrite.add(line);
        }

        try {
            // Save locally
            Files.write(outputFilePath, linesToWrite, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.info("Successfully wrote {} preprocessed comments to local file: {}", comments.size(), outputFilePath);

            // Save to HDFS with the same structure
            String hdfsOutputFileName = String.format("preprocessed_%s_%s.csv", identifier, timestamp);
            String hdfsTypeDir = hdfsService.PREPROCESSED_DIR + "/" + type;
            String hdfsDateDir = hdfsTypeDir + "/" + timestamp;
            String hdfsOutputPath = hdfsDateDir + "/" + hdfsOutputFileName;
            
            hdfsService.saveToHDFS(hdfsOutputPath, linesToWrite);
            logger.info("Successfully wrote preprocessed data to HDFS: {}", hdfsOutputPath);
        } catch (IOException e) {
            logger.error("Failed to write preprocessed data: {}", e.getMessage());
            throw e;
        }
    }

    public List<String> analyzeCommentsFromFile(String filePath, int batchSize, String fetchType) throws Exception {
        // Log the input parameters
        logger.info("Analyzing comments from file: {} with batch size: {} and fetch type: {}", filePath, batchSize, fetchType);

        // Read all lines from the CSV file
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        logger.info("Read {} lines from file", lines.size());

        // Determine the comment column index based on fetch type
        int commentColumnIndex;
        switch (fetchType.toLowerCase()) {
            case "keyword":
            case "subreddit":
                commentColumnIndex = 2; // Comment is in the third column
                break;
            case "post":
                commentColumnIndex = 1; // Comment is in the second column
                break;
            default:
                throw new IllegalArgumentException("Invalid fetch type: " + fetchType);
        }
        logger.info("Using comment column index: {}", commentColumnIndex);

        // Skip the header row and extract comments
        List<String> comments = lines.stream().skip(1)
            .map(line -> {
                String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (columns.length <= commentColumnIndex) {
                    logger.warn("Line has fewer columns than expected. Line: {}", line);
                    return "";
                }
                return columns[commentColumnIndex].trim();
            })
            .filter(comment -> !comment.isEmpty())
            .collect(Collectors.toList());
        logger.info("Extracted {} comments for analysis", comments.size());

        // Process comments in batches
        List<String> allResults = new ArrayList<>();
        for (int i = 0; i < comments.size(); i += batchSize) {
            int batchEnd = Math.min(i + batchSize, comments.size());
            List<String> batch = comments.subList(i, batchEnd);
            logger.info("Processing batch {}-{} of {}", i, batchEnd, comments.size());
            
            List<String> batchResults = new ArrayList<>();
            for (String comment : batch) {
                String sentiment = sentimentAnalysisService.analyzeSentiment(comment);
                batchResults.add(sentiment);
            }
            allResults.addAll(batchResults);
            logger.info("Completed batch with {} results", batchResults.size());
        }
        logger.info("Completed sentiment analysis for all comments. Total results: {}", allResults.size());

        try {
            // Save preprocessed comments and sentiments
            savePreprocessedCommentsNIO(filePath, comments, allResults);
            logger.info("Successfully saved preprocessed comments and sentiments");
        } catch (IOException e) {
            logger.error("Failed to save preprocessed data: {}", e.getMessage(), e);
            throw e;
        }

        return allResults;
    }

    public String fetchByKeyword(String keyword, int numPosts, int commentsPerPost, String sortBy, String outputFile) {
        try {
            String localOutputFile;
            if (outputFile != null) {
                // If output file is provided, ensure it's in the correct directory structure
                String timestamp = LocalDateTime.now().format(DATE_FORMAT);
                Path outputDir = Paths.get(RAW_DATA_DIR, "keyword", timestamp);
                Files.createDirectories(outputDir);
                // Ensure output file has .csv extension
                if (!outputFile.toLowerCase().endsWith(".csv")) {
                    outputFile = outputFile + ".csv";
                }
                localOutputFile = outputDir.resolve(outputFile).toString();
            } else {
                localOutputFile = createOutputPath("keyword", keyword);
            }
            
            logger.info("Using output file path: {}", localOutputFile);
            
            // Run Python script to fetch data
            List<String> args = new ArrayList<>();
            args.add(keyword);
            args.add(String.valueOf(numPosts));
            args.add(String.valueOf(commentsPerPost));
            args.add(sortBy);
            args.add(localOutputFile);
            
            PythonScriptRunner.runScript(PYTHON_SCRIPTS_PATH + "/fetch_by_keyword.py", args);
            
            // Save the same data to HDFS
            if (Files.exists(Paths.get(localOutputFile))) {
                String hdfsPath = hdfsService.createHDFSOutputPath("keyword", keyword);
                hdfsService.saveToHDFS(hdfsPath, Files.readAllLines(Paths.get(localOutputFile)));
            }
            
            return localOutputFile;
        } catch (Exception e) {
            logger.error("Error in fetchByKeyword: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch data by keyword", e);
        }
    }

    public String fetchBySubreddit(String subreddit, int numPosts, int commentsPerPost, String sortBy, String outputFile) {
        try {
            String localOutputFile;
            if (outputFile != null) {
                // If output file is provided, ensure it's in the correct directory structure
                String timestamp = LocalDateTime.now().format(DATE_FORMAT);
                Path outputDir = Paths.get(RAW_DATA_DIR, "subreddit", timestamp);
                Files.createDirectories(outputDir);
                // Ensure output file has .csv extension
                if (!outputFile.toLowerCase().endsWith(".csv")) {
                    outputFile = outputFile + ".csv";
                }
                localOutputFile = outputDir.resolve(outputFile).toString();
            } else {
                localOutputFile = createOutputPath("subreddit", subreddit);
            }
            
            logger.info("Using output file path: {}", localOutputFile);
            
            List<String> args = new ArrayList<>();
            args.add(subreddit.trim());
            args.add(Integer.toString(numPosts));
            args.add(Integer.toString(commentsPerPost));
            args.add(sortBy.trim());
            args.add(localOutputFile);
            
            logger.info("Running script with args: subreddit={}, numPosts={}, commentsPerPost={}, sortBy={}", 
                subreddit, numPosts, commentsPerPost, sortBy);
            
            PythonScriptRunner.runScript(PYTHON_SCRIPTS_PATH + "/fetch_by_subreddit.py", args);
            
            // Save to HDFS
            if (Files.exists(Paths.get(localOutputFile))) {
                String hdfsPath = hdfsService.createHDFSOutputPath("subreddit", subreddit);
                hdfsService.saveToHDFS(hdfsPath, Files.readAllLines(Paths.get(localOutputFile)));
            }
            
            return localOutputFile;
        } catch (Exception e) {
            logger.error("Error in fetchBySubreddit: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch data by subreddit", e);
        }
    }

    public String fetchByPostLink(String postLink, int commentsPerPost, String outputFile) {
        try {
            String localOutputFile;
            if (outputFile != null) {
                // If output file is provided, ensure it's in the correct directory structure
                String timestamp = LocalDateTime.now().format(DATE_FORMAT);
                Path outputDir = Paths.get(RAW_DATA_DIR, "post", timestamp);
                Files.createDirectories(outputDir);
                // Ensure output file has .csv extension
                if (!outputFile.toLowerCase().endsWith(".csv")) {
                    outputFile = outputFile + ".csv";
                }
                localOutputFile = outputDir.resolve(outputFile).toString();
            } else {
                localOutputFile = createOutputPath("post", postLink);
            }
            
            logger.info("Using output file path: {}", localOutputFile);
            
            List<String> args = new ArrayList<>();
            args.add(postLink);
            args.add(String.valueOf(commentsPerPost));
            args.add(localOutputFile);
            
            PythonScriptRunner.runScript(PYTHON_SCRIPTS_PATH + "/fetch_by_post.py", args);
            
            // Save to HDFS
            if (Files.exists(Paths.get(localOutputFile))) {
                String hdfsPath = hdfsService.createHDFSOutputPath("post", postLink);
                hdfsService.saveToHDFS(hdfsPath, Files.readAllLines(Paths.get(localOutputFile)));
            }
            
            return localOutputFile;
        } catch (Exception e) {
            logger.error("Error in fetchByPostLink: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch data by post link", e);
        }
    }
}

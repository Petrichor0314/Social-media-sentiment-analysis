package com.sentimentanalysis.ui.views;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.collections.FXCollections;
import javafx.scene.chart.PieChart;
import javafx.geometry.Pos;
import com.sentimentanalysis.core.service.SentimentAnalysisService;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

public class SentimentAnalysisTabContent {
    private final SentimentAnalysisService analysisService = new SentimentAnalysisService();
    private TextArea inputTextArea;
    private TextArea resultTextArea;
    private ChoiceBox<String> modelChoice;
    private PieChart sentimentChart;
    private Map<String, Double> sentimentScores;

    public VBox getContent() {
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setAlignment(Pos.TOP_CENTER);

        // Input Section
        VBox inputSection = new VBox(10);
        Label inputLabel = new Label("Enter Text:");
        inputLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        inputTextArea = new TextArea();
        inputTextArea.setPromptText("Enter text to analyze");
        inputTextArea.setPrefRowCount(5);
        inputTextArea.setWrapText(true);
        
        inputSection.getChildren().addAll(inputLabel, inputTextArea);

        // Model Selection Section
        VBox modelSection = new VBox(10);
        Label modelLabel = new Label("Select Model:");
        modelLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        modelChoice = new ChoiceBox<>(FXCollections.observableArrayList(
            "HDFS Model", "RoBERTa Model", "StanfordNLP Model"
        ));
        modelChoice.setValue("RoBERTa Model");
        modelChoice.setMaxWidth(Double.MAX_VALUE);
        
        modelSection.getChildren().addAll(modelLabel, modelChoice);

        // Button Section
        Button analyzeButton = new Button("Analyze Sentiment");
        analyzeButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                             "-fx-font-weight: bold; -fx-font-size: 14px; " +
                             "-fx-padding: 10 20; -fx-background-radius: 5;");
        analyzeButton.setMaxWidth(300);  
        analyzeButton.setPrefWidth(200); 
        HBox buttonBox = new HBox(analyzeButton);
        buttonBox.setAlignment(Pos.CENTER);
        analyzeButton.setOnAction(e -> analyzeSentiment());

        // Results Section
        VBox resultsSection = new VBox(10);
        Label resultsLabel = new Label("Results:");
        resultsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        resultTextArea = new TextArea();
        resultTextArea.setEditable(false);
        resultTextArea.setPrefRowCount(2);
        resultTextArea.setStyle("-fx-control-inner-background: #f8f8f8;");
        
        // Initialize sentiment chart
        sentimentChart = new PieChart();
        sentimentChart.setTitle("Sentiment Distribution");
        sentimentChart.setLabelsVisible(true);
        sentimentChart.setLegendVisible(true);
        sentimentChart.setPrefHeight(300);
        
        resultsSection.getChildren().addAll(resultsLabel, resultTextArea, sentimentChart);
        VBox.setVgrow(sentimentChart, Priority.ALWAYS);

        // Add all sections to main container
        mainContainer.getChildren().addAll(
            inputSection,
            modelSection,
            buttonBox,
            resultsSection
        );

        return mainContainer;
    }

    private void updateChart(String sentiment) {
        if (sentimentScores == null) {
            sentimentScores = new HashMap<>();
        }

        // Update scores
        sentimentScores.merge(sentiment, 1.0, Double::sum);
        
        // Clear existing data
        sentimentChart.getData().clear();
        
        // Add new data
        sentimentScores.forEach((label, score) -> {
            PieChart.Data slice = new PieChart.Data(label + " (" + String.format("%.1f%%", (score / sentimentScores.values().stream().mapToDouble(Double::doubleValue).sum()) * 100) + ")", 
                                                  score);
            sentimentChart.getData().add(slice);
        });
    }

    public void analyzeSentiment() {
        String text = inputTextArea.getText();
        if (text == null || text.trim().isEmpty()) {
            resultTextArea.setText("Please enter some text to analyze.");
            return;
        }

        try {
            String result;
            switch (modelChoice.getValue()) {
                case "HDFS Model":
                    result = analysisService.analyzeWithHDFSModel(text);
                    break;
                case "StanfordNLP Model":
                    result = analysisService.analyzeWithStanfordNLP(text);
                    break;
                default:
                    result = analysisService.analyzeWithHuggingFaceAPI(text);
                    break;
            }
            resultTextArea.setText("Sentiment: " + result);
            updateChart(result);
        } catch (IOException e) {
            if (e.getMessage().contains("rate limit exceeded")) {
                resultTextArea.setText("⚠️ " + e.getMessage() + "\n\nPlease try using the HDFS Model instead, or wait for the rate limit to reset.");
            } else {
                resultTextArea.setText("Error: " + e.getMessage());
            }
        } catch (Exception e) {
            resultTextArea.setText("Error analyzing text: " + e.getMessage());
        }
    }
}

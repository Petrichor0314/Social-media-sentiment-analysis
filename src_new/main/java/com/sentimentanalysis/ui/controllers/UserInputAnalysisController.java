package com.sentimentanalysis.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import com.sentimentanalysis.core.service.SentimentAnalysisService;

public class UserInputAnalysisController {
    private SentimentAnalysisService sentimentService = new SentimentAnalysisService();

    @FXML
    private TextArea textArea;
    @FXML
    private RadioButton hdfsModelRadioButton;
    @FXML
    private Button analyzeButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label resultLabel;

    @FXML
    private void handleAnalyzeButtonAction() {
        String inputText = textArea.getText();
        if (inputText == null || inputText.trim().isEmpty()) {
            showAlert("Error", "Please enter some text to analyze.");
            return;
        }

        // Disable analyze button while processing
        analyzeButton.setDisable(true);
        progressIndicator.setVisible(true);

        Task<String> analysisTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                try {
                    if (hdfsModelRadioButton.isSelected()) {
                        return sentimentService.analyzeWithHDFSModel(inputText);
                    } else {
                        return sentimentService.analyzeWithHuggingFaceAPI(inputText);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error during sentiment analysis: " + e.getMessage(), e);
                }
            }
        };

        analysisTask.setOnSucceeded(event -> {
            String result = analysisTask.getValue();
            resultLabel.setText("Sentiment: " + result);
            analyzeButton.setDisable(false);
            progressIndicator.setVisible(false);
        });

        analysisTask.setOnFailed(event -> {
            Throwable exception = analysisTask.getException();
            showAlert("Error", "Analysis failed: " + exception.getMessage());
            analyzeButton.setDisable(false);
            progressIndicator.setVisible(false);
        });

        // Start the analysis in a background thread
        Thread analysisThread = new Thread(analysisTask);
        analysisThread.setDaemon(true);
        analysisThread.start();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

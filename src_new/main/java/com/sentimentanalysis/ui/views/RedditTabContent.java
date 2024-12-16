package com.sentimentanalysis.ui.views;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import com.sentimentanalysis.core.service.RedditService;
import com.sentimentanalysis.core.service.SentimentAnalysisService;
import com.sentimentanalysis.core.service.StanfordNLPSentimentService;
import com.sentimentanalysis.core.service.SentimentAnalyzer;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.concurrent.Task;

public class RedditTabContent {
    private static final Logger logger = Logger.getLogger(RedditTabContent.class.getName());

    private TabPane redditTabPane;
    private Tab keywordTab;
    private Tab subredditTab;
    private Tab postLinkTab;
    private TableView<CommentSentiment> commentTable;
    private final RedditService redditService;
    private SentimentAnalyzer sentimentAnalyzer;
    private VBox content;
    private ProgressBar progressBar;
    private Label statusLabel;

    public RedditTabContent() {
        this.redditService = new RedditService();
        this.sentimentAnalyzer = new SentimentAnalysisService();
    }

    public VBox getContent() {
        content = new VBox(10);
        content.setPadding(new Insets(10));

        // Create input fields section
        HBox inputFields = createInputFields();
        content.getChildren().add(inputFields);

        // Create progress bar and status label (initially invisible)
        VBox progressBox = new VBox(5);
        progressBox.setAlignment(Pos.CENTER);
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        progressBar.setVisible(false);
        
        statusLabel = new Label("");
        statusLabel.setVisible(false);
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        
        progressBox.getChildren().addAll(progressBar, statusLabel);
        content.getChildren().add(progressBox);

        // Create table view for comments
        commentTable = createCommentTable();
        content.getChildren().add(commentTable);

        return content;
    }

    private HBox createInputFields() {
        HBox inputFieldsBox = new HBox(10);
        inputFieldsBox.setAlignment(Pos.CENTER_LEFT);
        inputFieldsBox.setPadding(new Insets(10));

        // Create tabs for different Reddit data fetching methods
        redditTabPane = new TabPane();
        redditTabPane.getStyleClass().add("accent");

        keywordTab = createKeywordTab();
        keywordTab.setClosable(false);
        subredditTab = createSubredditTab();
        subredditTab.setClosable(false);
        postLinkTab = createPostLinkTab();
        postLinkTab.setClosable(false);

        redditTabPane.getTabs().addAll(keywordTab, subredditTab, postLinkTab);
        HBox.setHgrow(redditTabPane, Priority.ALWAYS);
        inputFieldsBox.getChildren().add(redditTabPane);

        return inputFieldsBox;
    }

    private Spinner<Integer> createEditableSpinner(int min, int max, int initial) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initial);
        spinner.setEditable(true);
        
        // Add input validation
        spinner.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                spinner.getEditor().setText(oldValue);
            }
        });
        
        // Handle manual input
        spinner.getEditor().setOnAction(event -> {
            try {
                String text = spinner.getEditor().getText();
                int value = Integer.parseInt(text);
                if (value < min) {
                    spinner.getValueFactory().setValue(min);
                } else if (value > max) {
                    spinner.getValueFactory().setValue(max);
                } else {
                    spinner.getValueFactory().setValue(value);
                }
            } catch (NumberFormatException e) {
                spinner.getValueFactory().setValue(initial);
            }
        });
        
        return spinner;
    }

    private Tab createKeywordTab() {
        Tab tab = new Tab("Fetch by Keyword");
        VBox content = new VBox(15);
        content.setPadding(new Insets(10));

        // Create model selector
        ComboBox<SentimentAnalyzer> modelSelector = new ComboBox<>();
        modelSelector.getItems().addAll(
            new SentimentAnalysisService(),
            new StanfordNLPSentimentService()
        );
        modelSelector.setValue(sentimentAnalyzer);
        modelSelector.setMaxWidth(200);
        modelSelector.setCellFactory(lv -> new ListCell<SentimentAnalyzer>() {
            @Override
            protected void updateItem(SentimentAnalyzer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getName());
            }
        });
        modelSelector.setButtonCell(new ListCell<SentimentAnalyzer>() {
            @Override
            protected void updateItem(SentimentAnalyzer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getName());
            }
        });
        modelSelector.setOnAction(e -> sentimentAnalyzer = modelSelector.getValue());

        TextField keywordField = new TextField();
        keywordField.setPromptText("Enter keyword");

        Spinner<Integer> numberOfPostsSpinner = createEditableSpinner(1, 100, 10);
        Spinner<Integer> commentsPerPostSpinner = createEditableSpinner(1, 100, 5);

        ComboBox<String> sortByComboBox = new ComboBox<>();
        sortByComboBox.getItems().addAll("hot", "new", "top");
        sortByComboBox.setValue("hot");

        HBox inputFieldsBox = new HBox(15);
        inputFieldsBox.setAlignment(Pos.CENTER_LEFT);
        inputFieldsBox.getChildren().addAll(
            new Label("Model:"), modelSelector,
            new Label("Keyword:"), keywordField,
            new Label("Number of Posts:"), numberOfPostsSpinner,
            new Label("Comments per Post:"), commentsPerPostSpinner,
            new Label("Sort By:"), sortByComboBox
        );

        content.getChildren().addAll(inputFieldsBox);
        runSelectedAnalysisScript(inputFieldsBox, "keyword");
        tab.setContent(content);
        return tab;
    }

    private Tab createSubredditTab() {
        Tab tab = new Tab("Fetch by Subreddit");
        VBox content = new VBox(15);
        content.setPadding(new Insets(10));

        // Create model selector
        ComboBox<SentimentAnalyzer> modelSelector = new ComboBox<>();
        modelSelector.getItems().addAll(
            new SentimentAnalysisService(),
            new StanfordNLPSentimentService()
        );
        modelSelector.setValue(sentimentAnalyzer);
        modelSelector.setMaxWidth(200);
        modelSelector.setCellFactory(lv -> new ListCell<SentimentAnalyzer>() {
            @Override
            protected void updateItem(SentimentAnalyzer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getName());
            }
        });
        modelSelector.setButtonCell(new ListCell<SentimentAnalyzer>() {
            @Override
            protected void updateItem(SentimentAnalyzer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getName());
            }
        });
        modelSelector.setOnAction(e -> sentimentAnalyzer = modelSelector.getValue());

        TextField subredditField = new TextField();
        subredditField.setPromptText("Enter subreddit name");

        Spinner<Integer> numberOfPostsSpinner = createEditableSpinner(1, 100, 10);
        Spinner<Integer> commentsPerPostSpinner = createEditableSpinner(1, 100, 5);

        ComboBox<String> sortByComboBox = new ComboBox<>();
        sortByComboBox.getItems().addAll("hot", "new", "top");
        sortByComboBox.setValue("hot");

        HBox inputFieldsBox = new HBox(15);
        inputFieldsBox.setAlignment(Pos.CENTER_LEFT);
        inputFieldsBox.getChildren().addAll(
            new Label("Model:"), modelSelector,
            new Label("Subreddit:"), subredditField,
            new Label("Number of Posts:"), numberOfPostsSpinner,
            new Label("Comments per Post:"), commentsPerPostSpinner,
            new Label("Sort By:"), sortByComboBox
        );

        content.getChildren().addAll(inputFieldsBox);
        runSelectedAnalysisScript(inputFieldsBox, "subreddit");
        tab.setContent(content);
        return tab;
    }

    private Tab createPostLinkTab() {
        Tab tab = new Tab("Fetch by Post Link");
        VBox content = new VBox(15);
        content.setPadding(new Insets(10));

        // Create model selector
        ComboBox<SentimentAnalyzer> modelSelector = new ComboBox<>();
        modelSelector.getItems().addAll(
            new SentimentAnalysisService(),
            new StanfordNLPSentimentService()
        );
        modelSelector.setValue(sentimentAnalyzer);
        modelSelector.setMaxWidth(200);
        modelSelector.setCellFactory(lv -> new ListCell<SentimentAnalyzer>() {
            @Override
            protected void updateItem(SentimentAnalyzer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getName());
            }
        });
        modelSelector.setButtonCell(new ListCell<SentimentAnalyzer>() {
            @Override
            protected void updateItem(SentimentAnalyzer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getName());
            }
        });
        modelSelector.setOnAction(e -> sentimentAnalyzer = modelSelector.getValue());

        TextField postLinkField = new TextField();
        postLinkField.setPromptText("Enter Reddit post URL");

        Spinner<Integer> commentsPerPostSpinner = createEditableSpinner(1, 100, 10);

        HBox inputFieldsBox = new HBox(15);
        inputFieldsBox.setAlignment(Pos.CENTER_LEFT);
        inputFieldsBox.getChildren().addAll(
            new Label("Model:"), modelSelector,
            new Label("Post Link:"), postLinkField,
            new Label("Comments to Fetch:"), commentsPerPostSpinner
        );

        content.getChildren().addAll(inputFieldsBox);
        runSelectedAnalysisScript(inputFieldsBox, "postLink");
        tab.setContent(content);
        return tab;
    }

    private void runSelectedAnalysisScript(HBox inputFieldsBox, String fetchMethod) {
        Button analyzeButton = new Button("Analyze");
        analyzeButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
        inputFieldsBox.getChildren().add(analyzeButton);
        
        analyzeButton.setOnAction(event -> {
            logger.info("Analyze button clicked.");
            
            // Get the input field values first
            Tab selectedTab = redditTabPane.getSelectionModel().getSelectedItem();
            VBox content = (VBox) selectedTab.getContent();
            HBox fieldsBox = (HBox) content.getChildren().get(0);

            String keyword = "";
            String subreddit = "";
            String postLink = "";
            int numberOfPosts = 0;
            int commentsPerPost = 0;
            String sortBy = "";

            // Validate input fields based on tab
            if (selectedTab.getText().equals("Fetch by Keyword")) {
                keyword = ((TextField) fieldsBox.getChildren().get(3)).getText().trim();
                if (keyword.isEmpty()) {
                    statusLabel.setText("Please enter a keyword");
                    statusLabel.setVisible(true);
                    return;
                }
            } else if (selectedTab.getText().equals("Fetch by Subreddit")) {
                subreddit = ((TextField) fieldsBox.getChildren().get(3)).getText().trim();
                if (subreddit.isEmpty()) {
                    statusLabel.setText("Please enter a subreddit name");
                    statusLabel.setVisible(true);
                    return;
                }
            } else if (selectedTab.getText().equals("Fetch by Post Link")) {
                postLink = ((TextField) fieldsBox.getChildren().get(3)).getText().trim();
                if (postLink.isEmpty()) {
                    statusLabel.setText("Please enter a post link");
                    statusLabel.setVisible(true);
                    return;
                }
            }

            // If validation passes, proceed with analysis
            analyzeButton.setDisable(true);
            progressBar.setVisible(true);
            statusLabel.setVisible(true);
            statusLabel.setText("Initializing analysis...");
            
            // Hide input fields during analysis
            inputFieldsBox.setVisible(false);
            
            Task<Void> analysisTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        updateMessage("Fetching data from Reddit...");
                        logger.info("Starting analysis task.");
                        Tab selectedTab = redditTabPane.getSelectionModel().getSelectedItem();
                        VBox content = (VBox) selectedTab.getContent();
                        HBox fieldsBox = (HBox) content.getChildren().get(0);

                        String keyword = "";
                        String subreddit = "";
                        String postLink = "";
                        int numberOfPosts = 0;
                        int commentsPerPost = 0;
                        String sortBy = "";

                        if (selectedTab.getText().equals("Fetch by Keyword")) {
                            keyword = ((TextField) fieldsBox.getChildren().get(3)).getText();
                            numberOfPosts = ((Spinner<Integer>) fieldsBox.getChildren().get(5)).getValue();
                            commentsPerPost = ((Spinner<Integer>) fieldsBox.getChildren().get(7)).getValue();
                            sortBy = ((ComboBox<String>) fieldsBox.getChildren().get(9)).getValue();
                        } else if (selectedTab.getText().equals("Fetch by Subreddit")) {
                            subreddit = ((TextField) fieldsBox.getChildren().get(3)).getText();
                            numberOfPosts = ((Spinner<Integer>) fieldsBox.getChildren().get(5)).getValue();
                            commentsPerPost = ((Spinner<Integer>) fieldsBox.getChildren().get(7)).getValue();
                            sortBy = ((ComboBox<String>) fieldsBox.getChildren().get(9)).getValue();
                        } else if (selectedTab.getText().equals("Fetch by Post Link")) {
                            postLink = ((TextField) fieldsBox.getChildren().get(3)).getText();
                            commentsPerPost = ((Spinner<Integer>) fieldsBox.getChildren().get(5)).getValue();
                        } else {
                            return null;
                        }

                        // Update progress
                        updateProgress(0.1, 1);

                        String actualOutputFile = null;  // This will store the actual file path

                        String outputFile = keyword != null ? keyword.replaceAll("[^a-zA-Z0-9-_]", "_") : 
                            subreddit != null ? subreddit.replaceAll("[^a-zA-Z0-9-_]", "_") : 
                            postLink.replaceAll("[^a-zA-Z0-9-_]", "_");

                        if (!keyword.isEmpty()) {
                            actualOutputFile = redditService.fetchByKeyword(keyword, numberOfPosts, commentsPerPost, sortBy, outputFile);
                        } else if (!subreddit.isEmpty()) {
                            // Validate and clean subreddit name
                            subreddit = subreddit.trim();
                            if (subreddit.startsWith("r/")) {
                                subreddit = subreddit.substring(2);
                            }
                            if (subreddit.isEmpty()) {
                                throw new IllegalArgumentException("Please enter a valid subreddit name");
                            }
                            actualOutputFile = redditService.fetchBySubreddit(subreddit, numberOfPosts, commentsPerPost, sortBy, outputFile);
                        } else if (!postLink.isEmpty()) {
                            actualOutputFile = redditService.fetchByPostLink(postLink, commentsPerPost, outputFile);
                        }

                        logger.info("Data fetching completed.");

                        // Update progress
                        updateProgress(0.5, 1);
                        updateMessage("Processing comments...");

                        if (actualOutputFile == null) {
                            throw new IllegalStateException("No output file path returned from Reddit service");
                        }

                        Path outputPath = Paths.get(actualOutputFile);
                        if (!Files.exists(outputPath)) {
                            throw new IOException("Output file not found: " + outputPath);
                        }

                        try (CSVReader csvReader = new CSVReader(new FileReader(outputPath.toFile()))) {
                            String[] parts;
                            int positive = 0, negative = 0;
                            int totalComments = 0;
                            List<CommentSentiment> commentSentiments = new ArrayList<>();
                            Map<String, Integer> postCommentCount = new HashMap<>();
                            Map<String, Integer> subredditCommentCount = new HashMap<>();

                            // Determine column indices based on the selected tab
                            int commentIndex = selectedTab.getText().equals("Fetch by Post Link") ? 1 : 2;
                            int postTitleIndex = selectedTab.getText().equals("Fetch by Post Link") ? 0 : 1;
                            int subredditIndex = selectedTab.getText().equals("Fetch by Keyword") ? 0 : -1;

                            // Skip header row
                            csvReader.readNext();

                            while ((parts = csvReader.readNext()) != null) {
                                if (parts.length > commentIndex) {
                                    String comment = parts[commentIndex].trim();
                                    if (!comment.isEmpty()) {
                                        totalComments++;
                                        updateMessage("Analyzing comment " + totalComments + "...");
                                        String sentiment = sentimentAnalyzer.analyzeSentiment(comment);
                                        if (sentiment.equals("Positive")) {
                                            positive++;
                                        } else {
                                            negative++;
                                        }
                                        String postId = parts[postTitleIndex];
                                        String subredditName = selectedTab.getText().equals("Fetch by Keyword") ? parts[subredditIndex] : parts[postTitleIndex];
                                        commentSentiments.add(new CommentSentiment(comment, sentiment, postId, subredditName));

                                        // Track post and subreddit comment counts
                                        postCommentCount.put(postId, postCommentCount.getOrDefault(postId, 0) + 1);
                                        subredditCommentCount.put(subredditName, subredditCommentCount.getOrDefault(subredditName, 0) + 1);
                                    }
                                }
                            }

                            if (totalComments == 0) {
                                throw new IllegalStateException("No valid comments found for analysis");
                            }

                            // Calculate percentages
                            double positivePercentage = (positive / (double) totalComments) * 100;
                            double negativePercentage = (negative / (double) totalComments) * 100;

                            String finalResult = String.format("""
                                Analysis Complete!
                                Total Comments Analyzed: %d
                                Positive Comments: %d (%.1f%%)
                                Negative Comments: %d (%.1f%%)
                                """, totalComments, positive, positivePercentage, negative, negativePercentage);

                            updateCommentTable(commentSentiments, fetchMethod);

                            updateUIWithResults(commentSentiments, fetchMethod);

                            // Update progress
                            updateProgress(1, 1);
                            updateMessage("Analysis complete!");
                        }

                        return null;

                    } catch (Exception ex) {
                        updateMessage("Error: " + ex.getMessage());
                        throw ex;
                    }
                }
            };

            progressBar.progressProperty().bind(analysisTask.progressProperty());
            statusLabel.textProperty().bind(analysisTask.messageProperty());

            // Handle task completion
            analysisTask.setOnSucceeded(eventSuccess -> {
                analyzeButton.setDisable(false);
                progressBar.setVisible(false);
                statusLabel.setVisible(false);
                progressBar.progressProperty().unbind();
                statusLabel.textProperty().unbind();
                logger.info("Analysis task completed successfully.");
                inputFieldsBox.setVisible(true); // Show input fields again after analysis
            });

            analysisTask.setOnFailed(eventFailure -> {
                analyzeButton.setDisable(false);
                progressBar.setVisible(false);
                statusLabel.textProperty().unbind();
                progressBar.progressProperty().unbind();
                logger.log(Level.WARNING, "Analysis task failed.");
                inputFieldsBox.setVisible(true); // Show input fields again after analysis
            });

            // Start the analysis
            Thread analysisThread = new Thread(analysisTask);
            analysisThread.setDaemon(true);
            analysisThread.start();
        });
    }

    private TableView<CommentSentiment> createCommentTable() {
        commentTable = new TableView<>();
        
        TableColumn<CommentSentiment, String> commentColumn = new TableColumn<>("Comments");
        commentColumn.setCellValueFactory(new PropertyValueFactory<>("comment"));
        commentColumn.setPrefWidth(400);

        TableColumn<CommentSentiment, String> sentimentColumn = new TableColumn<>("Sentiment");
        sentimentColumn.setCellValueFactory(new PropertyValueFactory<>("sentiment"));
        sentimentColumn.setPrefWidth(100);

        // Start with basic columns that are always shown
        commentTable.getColumns().addAll(commentColumn, sentimentColumn);
        commentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Set a fixed height for the table
        commentTable.setPrefHeight(200);
        commentTable.setMaxHeight(200);
        
        // Add margin to separate from input fields
        VBox.setMargin(commentTable, new Insets(10, 0, 10, 0));

        return commentTable;
    }

    private void updateCommentTable(List<CommentSentiment> commentSentiments, String fetchMethod) {
        Platform.runLater(() -> {
            // Clear existing columns except Comments and Sentiment
            while (commentTable.getColumns().size() > 2) {
                commentTable.getColumns().remove(2);
            }

            // Add additional columns based on fetch method
            if ("keyword".equals(fetchMethod)) {
                // For keyword search, show both post title and subreddit
                TableColumn<CommentSentiment, String> postTitleColumn = new TableColumn<>("Post Title");
                postTitleColumn.setCellValueFactory(new PropertyValueFactory<>("postTitle"));
                postTitleColumn.setPrefWidth(200);

                TableColumn<CommentSentiment, String> subredditColumn = new TableColumn<>("Subreddit");
                subredditColumn.setCellValueFactory(new PropertyValueFactory<>("subreddit"));
                subredditColumn.setPrefWidth(100);

                commentTable.getColumns().addAll(postTitleColumn, subredditColumn);
            } else if ("subreddit".equals(fetchMethod)) {
                // For subreddit search, show only post title
                TableColumn<CommentSentiment, String> postTitleColumn = new TableColumn<>("Post Title");
                postTitleColumn.setCellValueFactory(new PropertyValueFactory<>("postTitle"));
                postTitleColumn.setPrefWidth(200);

                commentTable.getColumns().add(postTitleColumn);
            }
            // For post link search, only show comments and sentiment

            commentTable.getItems().setAll(commentSentiments);
            commentTable.getColumns().get(0).setText("Comments (" + commentSentiments.size() + ")");
        });
    }

    private HBox createCharts(List<CommentSentiment> comments, String fetchMethod) {
        HBox chartBox = new HBox(20); // 20px spacing between charts
        chartBox.setAlignment(Pos.CENTER);

        // Always show sentiment distribution pie chart
        Node sentimentChart = createSentimentPieChart(comments);
        chartBox.getChildren().add(sentimentChart);

        // Additional charts based on fetch method
        if ("keyword".equals(fetchMethod)) {
            // For keyword search, show both subreddit and post distribution
            Map<String, List<CommentSentiment>> commentsBySubreddit = comments.stream()
                .collect(Collectors.groupingBy(CommentSentiment::getSubreddit));
            BarChart<String, Number> subredditChart = createSubredditBarChart(commentsBySubreddit);
            subredditChart.setPrefSize(250, 200);
            
            Map<String, List<CommentSentiment>> commentsByPost = comments.stream()
                .collect(Collectors.groupingBy(CommentSentiment::getPostTitle));
            BarChart<String, Number> postChart = createPostBarChart(commentsByPost);
            postChart.setPrefSize(250, 200);
            
            chartBox.getChildren().addAll(subredditChart, postChart);
        } else if ("subreddit".equals(fetchMethod)) {
            // For subreddit search, show only post distribution
            Map<String, List<CommentSentiment>> commentsByPost = comments.stream()
                .collect(Collectors.groupingBy(CommentSentiment::getPostTitle));
            BarChart<String, Number> postChart = createPostBarChart(commentsByPost);
            postChart.setPrefSize(250, 200);
            chartBox.getChildren().add(postChart);
        }
        // For post link search, only show sentiment pie chart

        return chartBox;
    }

    private Node createSentimentPieChart(List<CommentSentiment> comments) {
        PieChart sentimentChart = new PieChart();
        sentimentChart.setTitle("Sentiment Distribution");
        
        // Keep chart container size moderate but increase pie size
        sentimentChart.setPrefSize(350, 350);
        sentimentChart.setMinSize(350, 350);
        
        // Increase the radius of the pie itself
        sentimentChart.setStartAngle(90);
        sentimentChart.setLabelsVisible(true);
        sentimentChart.setLabelLineLength(20);
        
        // Create an HBox to position the chart
        HBox chartContainer = new HBox();
        chartContainer.setAlignment(Pos.CENTER_LEFT);
        chartContainer.setPadding(new Insets(0, 0, 0, 20)); // Add left padding
        chartContainer.getChildren().add(sentimentChart);
        
        // Calculate total comments for percentage
        int totalComments = comments.size();
        
        // Define colors for each sentiment
        Map<String, String> colorMap = new HashMap<>();
        colorMap.put("Very Positive", "#2ecc71");  // Bright Green
        colorMap.put("Positive", "#27ae60");       // Dark Green
        colorMap.put("Neutral", "#95a5a6");        // Gray
        colorMap.put("Negative", "#e74c3c");       // Red
        colorMap.put("Very Negative", "#c0392b");  // Dark Red

        // Process comments and create data
        Map<String, Integer> sentimentCounts = new LinkedHashMap<>(); // Use LinkedHashMap to maintain order
        
        if (sentimentAnalyzer.isDetailedSentiment()) {
            sentimentCounts.put("Very Positive", 0);
            sentimentCounts.put("Positive", 0);
            sentimentCounts.put("Neutral", 0);
            sentimentCounts.put("Negative", 0);
            sentimentCounts.put("Very Negative", 0);
            
            for (CommentSentiment comment : comments) {
                String sentiment = comment.getSentiment();
                sentimentCounts.merge(sentiment, 1, Integer::sum);
            }
        } else {
            sentimentCounts.put("Positive", 0);
            sentimentCounts.put("Neutral", 0);
            sentimentCounts.put("Negative", 0);
            
            for (CommentSentiment comment : comments) {
                String sentiment = comment.getSentiment();
                sentimentCounts.merge(sentiment, 1, Integer::sum);
            }
        }
        
        // Add data to chart
        for (Map.Entry<String, Integer> entry : sentimentCounts.entrySet()) {
            if (entry.getValue() > 0) {
                double percentage = (entry.getValue() * 100.0) / totalComments;
                PieChart.Data data = new PieChart.Data(
                    String.format("%s (%.1f%%)", entry.getKey(), percentage),
                    entry.getValue()
                );
                sentimentChart.getData().add(data);
            }
        }
        
        // Apply colors and tooltips
        for (PieChart.Data data : sentimentChart.getData()) {
            String sentiment = data.getName().split(" \\(")[0];
            String color = colorMap.getOrDefault(sentiment, "#95a5a6");
            
            data.getNode().setStyle("-fx-pie-color: " + color + ";");
            
            // Create tooltip with exact percentage
            Tooltip tooltip = new Tooltip(data.getName());
            Tooltip.install(data.getNode(), tooltip);
        }
        
        // Hide legend
        sentimentChart.setLegendVisible(false);
        
        return chartContainer;
    }
    
    private BarChart<String, Number> createSubredditBarChart(Map<String, List<CommentSentiment>> commentsBySubreddit) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        
        // Make chart smaller and remove gaps
        barChart.setPrefSize(250, 200);
        barChart.setMinSize(250, 200);
        barChart.setBarGap(0);
        barChart.setCategoryGap(0);
        
        barChart.setTitle("Comments per Subreddit");
        xAxis.setLabel("Subreddit");
        yAxis.setLabel("Comments");
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        
        // Sort by comment count
        List<Map.Entry<String, List<CommentSentiment>>> sortedEntries = new ArrayList<>(commentsBySubreddit.entrySet());
        sortedEntries.sort((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()));
        
        String[] barColors = {
            "#3498db", "#e74c3c", "#2ecc71", "#f1c40f", 
            "#9b59b6", "#e67e22", "#1abc9c", "#34495e"
        };
        
        int colorIndex = 0;
        for (Map.Entry<String, List<CommentSentiment>> entry : sortedEntries) {
            // Use index numbers instead of names
            XYChart.Data<String, Number> data = new XYChart.Data<>(
                String.valueOf(colorIndex + 1), 
                entry.getValue().size()
            );
            series.getData().add(data);
            
            // Store full name for tooltip
            data.setExtraValue(entry.getKey());
            
            colorIndex++;
        }
        
        barChart.getData().add(series);
        barChart.setLegendVisible(false);
        
        // Apply colors and tooltips after the chart is shown
        Platform.runLater(() -> {
            for (int i = 0; i < series.getData().size(); i++) {
                XYChart.Data<String, Number> data = series.getData().get(i);
                String color = barColors[i % barColors.length];
                Node node = data.getNode();
                
                // Set bar color
                node.setStyle("-fx-bar-fill: " + color + ";");
                
                // Add tooltip with full name and count
                String fullName = (String) data.getExtraValue();
                Tooltip tooltip = new Tooltip(String.format("%s: %d comments", 
                    fullName, data.getYValue().intValue()));
                Tooltip.install(node, tooltip);
            }
        });
        
        return barChart;
    }
    
    private BarChart<String, Number> createPostBarChart(Map<String, List<CommentSentiment>> commentsByPost) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        
        // Make chart smaller and remove gaps
        barChart.setPrefSize(250, 200);
        barChart.setMinSize(250, 200);
        barChart.setBarGap(0);
        barChart.setCategoryGap(0);
        
        barChart.setTitle("Comments per Post");
        xAxis.setLabel("Post");
        yAxis.setLabel("Comments");
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        
        // Sort by comment count
        List<Map.Entry<String, List<CommentSentiment>>> sortedEntries = new ArrayList<>(commentsByPost.entrySet());
        sortedEntries.sort((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()));
        
        String[] barColors = {
            "#16a085", "#d35400", "#2980b9", "#c0392b", 
            "#8e44ad", "#27ae60", "#f39c12", "#2c3e50"
        };
        
        int colorIndex = 0;
        for (Map.Entry<String, List<CommentSentiment>> entry : sortedEntries) {
            // Use index numbers instead of names
            XYChart.Data<String, Number> data = new XYChart.Data<>(
                String.valueOf(colorIndex + 1),
                entry.getValue().size()
            );
            series.getData().add(data);
            
            // Store full title for tooltip
            data.setExtraValue(entry.getKey());
            
            colorIndex++;
        }
        
        barChart.getData().add(series);
        barChart.setLegendVisible(false);
        
        // Apply colors and tooltips after the chart is shown
        Platform.runLater(() -> {
            for (int i = 0; i < series.getData().size(); i++) {
                XYChart.Data<String, Number> data = series.getData().get(i);
                String color = barColors[i % barColors.length];
                Node node = data.getNode();
                
                // Set bar color
                node.setStyle("-fx-bar-fill: " + color + ";");
                
                // Add tooltip with full title and count
                String fullTitle = (String) data.getExtraValue();
                Tooltip tooltip = new Tooltip(String.format("%s: %d comments", 
                    fullTitle, data.getYValue().intValue()));
                Tooltip.install(node, tooltip);
            }
        });
        
        return barChart;
    }

    private void updateUIWithResults(List<CommentSentiment> comments, String fetchMethod) {
        HBox chartBox = createCharts(comments, fetchMethod);
        
        Platform.runLater(() -> {
            // Clear previous charts if they exist
            // Keep input fields (0), progress bar (1), and table (2)
            if (content.getChildren().size() > 3) {
                content.getChildren().remove(3, content.getChildren().size());
            }

            // Add margin to separate charts from table
            VBox.setMargin(chartBox, new Insets(10, 0, 0, 0));
            
            // Add charts below the table
            content.getChildren().add(chartBox);
            
            // Force a layout pass
            content.requestLayout();
        });
    }

    // Inner class to represent comment and sentiment
    public static class CommentSentiment {
        private final String comment;
        private final String sentiment;
        private final String postTitle;
        private final String subreddit;

        public CommentSentiment(String comment, String sentiment, String postTitle, String subreddit) {
            this.comment = comment;
            this.sentiment = sentiment;
            this.postTitle = postTitle;
            this.subreddit = subreddit;
        }

        public String getComment() {
            return comment;
        }

        public String getSentiment() {
            return sentiment;
        }

        public String getPostTitle() {
            return postTitle;
        }

        public String getSubreddit() {
            return subreddit;
        }
    }
}

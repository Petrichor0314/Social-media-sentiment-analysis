package com.sentimentanalysis.application;

import atlantafx.base.theme.CupertinoLight;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import com.sentimentanalysis.ui.views.RedditTabContent;
import com.sentimentanalysis.ui.views.SentimentAnalysisTabContent;

public class SentimentAnalysisApplication extends Application {
    private RedditTabContent redditTabContent;

    @Override
    public void start(Stage primaryStage) {
        Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());

        BorderPane mainPane = new BorderPane();
        TabPane tabPane = new TabPane();

        // Reddit Tab
        redditTabContent = new RedditTabContent();
        Tab redditTab = new Tab("Reddit Data");
        redditTab.setContent(redditTabContent.getContent());
        redditTab.setClosable(false);

        // User Input Tab
        var sentimentAnalysisTabContent = new SentimentAnalysisTabContent();
        Tab sentimentTab = new Tab("User Input");
        sentimentTab.setContent(sentimentAnalysisTabContent.getContent());
        sentimentTab.setClosable(false);

        // Add tabs and configure TabPane
        tabPane.getTabs().addAll(redditTab, sentimentTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setMinWidth(450);

        // Set up the layout
        mainPane.setCenter(tabPane);
        Scene scene = new Scene(mainPane, 1000, 700);

        primaryStage.setTitle("Social Media Sentiment Analysis Application");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

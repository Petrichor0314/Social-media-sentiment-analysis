package com.sentimentanalysis.ui.controllers;

import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import com.sentimentanalysis.core.service.RedditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedditDataController {
    private static final Logger logger = LoggerFactory.getLogger(RedditDataController.class);
    private final RedditService redditService = new RedditService();

    public void analyze(Tab selectedTab) {
        VBox content = (VBox) selectedTab.getContent();

        try {
            if (selectedTab.getText().equals("Fetch by Keyword")) {
                String keyword = ((TextField) content.getChildren().get(1)).getText();
                int numPosts = ((Spinner<Integer>) content.getChildren().get(3)).getValue();
                int commentsPerPost = ((Spinner<Integer>) content.getChildren().get(5)).getValue();
                String sortBy = ((ChoiceBox<String>) content.getChildren().get(7)).getValue();
                String outputFile = ((TextField) content.getChildren().get(9)).getText();

                redditService.fetchByKeyword(keyword, numPosts, commentsPerPost, sortBy, outputFile);

            } else if (selectedTab.getText().equals("Fetch by Subreddit")) {
                String subreddit = ((TextField) content.getChildren().get(1)).getText();
                int numPosts = ((Spinner<Integer>) content.getChildren().get(3)).getValue();
                int commentsPerPost = ((Spinner<Integer>) content.getChildren().get(5)).getValue();
                String sortBy = ((ChoiceBox<String>) content.getChildren().get(7)).getValue();
                String outputFile = ((TextField) content.getChildren().get(9)).getText();

                redditService.fetchBySubreddit(subreddit, numPosts, commentsPerPost, sortBy, outputFile);

            } else if (selectedTab.getText().equals("Fetch by Post Link")) {
                String postLink = ((TextField) content.getChildren().get(1)).getText();
                int commentsPerPost = ((Spinner<Integer>) content.getChildren().get(3)).getValue();
                String outputFile = ((TextField) content.getChildren().get(5)).getText();

                redditService.fetchByPostLink(postLink, commentsPerPost, outputFile);
            }
        } catch (Exception e) {
            logger.error("Error during analysis", e);
            throw new RuntimeException("Failed to analyze Reddit data", e);
        }
    }
}

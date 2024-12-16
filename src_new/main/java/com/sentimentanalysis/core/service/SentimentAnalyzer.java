package com.sentimentanalysis.core.service;

public interface SentimentAnalyzer {
    String analyzeSentiment(String text);
    String getName();
    default boolean isDetailedSentiment() {
        return false;
    }
}

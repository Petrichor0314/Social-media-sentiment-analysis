package com.sentimentanalysis.core.model;

public class SentimentResult {
    private final String text;
    private final double score;
    private final String sentiment;
    private final String source;

    public SentimentResult(String text, double score, String sentiment, String source) {
        this.text = text;
        this.score = score;
        this.sentiment = sentiment;
        this.source = source;
    }

    public String getText() {
        return text;
    }

    public double getScore() {
        return score;
    }

    public String getSentiment() {
        return sentiment;
    }

    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return String.format("Text: %s%nSentiment: %s%nScore: %.2f%nSource: %s", 
            text, sentiment, score, source);
    }
}

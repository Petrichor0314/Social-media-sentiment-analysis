package com.sentimentanalysis.core.service;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import java.util.Properties;

public class StanfordNLPSentimentService implements SentimentAnalyzer {
    private final StanfordCoreNLP pipeline;

    public StanfordNLPSentimentService() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
        this.pipeline = new StanfordCoreNLP(props);
    }

    @Override
    public String analyzeSentiment(String text) {
        CoreDocument doc = new CoreDocument(text);
        pipeline.annotate(doc);
        return doc.sentences().get(0).sentiment();
    }

    @Override
    public String getName() {
        return "Stanford CoreNLP";
    }

    @Override
    public boolean isDetailedSentiment() {
        return true;
    }
}

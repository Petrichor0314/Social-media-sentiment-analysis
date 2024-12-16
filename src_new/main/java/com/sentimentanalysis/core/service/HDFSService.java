package com.sentimentanalysis.core.service;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HDFSService {
    private static final Logger logger = LoggerFactory.getLogger(HDFSService.class);
    private static final String HDFS_URI = "hdfs://localhost:9000";
    private static final String BASE_DATA_DIR = "/sentiment-analysis/data";
    public static final String PREPROCESSED_DIR = "/sentiment-analysis/preprocessed";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final FileSystem fileSystem;

    public HDFSService() {
        try {
            Configuration conf = new Configuration();
            conf.set("fs.defaultFS", HDFS_URI);
            this.fileSystem = FileSystem.get(conf);
            createBaseDirectories();
        } catch (IOException e) {
            logger.error("Failed to initialize HDFS connection: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize HDFS connection", e);
        }
    }

    private void createBaseDirectories() throws IOException {
        fileSystem.mkdirs(new Path(BASE_DATA_DIR));
        fileSystem.mkdirs(new Path(PREPROCESSED_DIR));
    }

    public String createHDFSOutputPath(String type, String identifier) {
        try {
            String timestamp = LocalDateTime.now().format(DATE_FORMAT);
            Path outputDir = new Path(BASE_DATA_DIR, type + "/" + timestamp);
            fileSystem.mkdirs(outputDir);
            
            String filename = String.format("%s_%s.csv", 
                identifier.replaceAll("[^a-zA-Z0-9-_]", "_"), 
                timestamp);
            Path outputPath = new Path(outputDir, filename);
            
            logger.info("Created HDFS output path: {}", outputPath);
            return outputPath.toString();
        } catch (Exception e) {
            logger.error("Error creating HDFS output path: {}", e.getMessage());
            throw new RuntimeException("Failed to create HDFS output directory", e);
        }
    }

    public void saveToHDFS(String hdfsPath, List<String> lines) throws IOException {
        Path path = new Path(hdfsPath);
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(fileSystem.create(path, true)))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
            logger.info("Successfully wrote data to HDFS: {}", hdfsPath);
        } catch (IOException e) {
            logger.error("Failed to write data to HDFS: {}", e.getMessage());
            throw e;
        }
    }

    public void savePreprocessedToHDFS(String originalFilePath, List<String> comments, List<String> sentiments) throws IOException {
        logger.info("Starting to save preprocessed data to HDFS");
        logger.info("Original file path: {}", originalFilePath);
        logger.info("Number of comments: {}, Number of sentiments: {}", comments.size(), sentiments.size());
        
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String originalFileName = new Path(originalFilePath).getName();
        String outputFileName = String.format("preprocessed_%s_%s.csv", 
            originalFileName.replaceAll("\\.csv$", ""),
            timestamp);
        
        Path outputPath = new Path(PREPROCESSED_DIR, outputFileName);
        logger.info("Generated output path: {}", outputPath);
        
        List<String> linesToWrite = new ArrayList<>();
        linesToWrite.add("Preprocessed Comment,Sentiment");
        for (int i = 0; i < comments.size(); i++) {
            linesToWrite.add(String.format("%s,%s", 
                comments.get(i).replace(",", "\\,").replace("\"", "\\\""),
                sentiments.get(i)));
        }
        
        logger.info("Prepared {} lines to write", linesToWrite.size());
        try {
            saveToHDFS(outputPath.toString(), linesToWrite);
            logger.info("Successfully saved preprocessed data to HDFS at: {}", outputPath);
        } catch (IOException e) {
            logger.error("Failed to save preprocessed data to HDFS: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void close() {
        try {
            if (fileSystem != null) {
                fileSystem.close();
            }
        } catch (IOException e) {
            logger.error("Error closing HDFS connection: {}", e.getMessage());
        }
    }
}

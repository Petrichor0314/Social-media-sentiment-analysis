package com.sentimentanalysis.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.error("Unable to find config.properties");
                throw new RuntimeException("config.properties not found");
            }
            properties.load(input);
        } catch (IOException e) {
            logger.error("Error loading properties", e);
            throw new RuntimeException("Failed to load application properties", e);
        }
    }

    public static String getRedditClientId() {
        return properties.getProperty("reddit.client.id");
    }

    public static String getRedditClientSecret() {
        return properties.getProperty("reddit.client.secret");
    }

    public static String getRedditUserAgent() {
        return properties.getProperty("reddit.user.agent");
    }

    public static String getPythonScriptPath() {
        return properties.getProperty("script_path");
    }

    public static String getHuggingFaceApiKey() {
        return properties.getProperty("huggingface.api.key");
    }
}

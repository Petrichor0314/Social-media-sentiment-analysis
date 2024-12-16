package com.sentimentanalysis.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PythonScriptRunner {
    private static final Logger logger = LoggerFactory.getLogger(PythonScriptRunner.class);
    private static final String PYTHON_EXECUTABLE = "C:\\Python312\\python.exe";

    public static void runScript(String scriptPath, List<String> args) {
        try {
            List<String> command = new ArrayList<>();
            command.add(PYTHON_EXECUTABLE);
            command.add(scriptPath);
            
            // Add arguments to the command
            if (args != null && !args.isEmpty()) {
                command.addAll(args);
                
                // If the last argument is a file path (output file), ensure its directory exists
                String lastArg = args.get(args.size() - 1);
                if (lastArg.endsWith(".csv")) {
                    Path outputPath = Paths.get(lastArg);
                    Path parentDir = outputPath.getParent();
                    if (parentDir != null) {
                        Files.createDirectories(parentDir);
                        logger.info("Created directory structure for output file: {}", parentDir);
                    }
                }
            }
            
            logger.info("Running Python script: {}", scriptPath);
            logger.info("Full command: {}", String.join(" ", command));
            logger.info("Arguments passed: {}", args);
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            
            // Set working directory to the script's directory
            Path scriptDir = Paths.get(scriptPath).getParent();
            if (scriptDir != null) {
                processBuilder.directory(scriptDir.toFile());
                logger.info("Set working directory to: {}", scriptDir);
            }
            
            Process process = processBuilder.start();
            
            // Read the output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info("Python output: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Python script failed with exit code: {}", exitCode);
                logger.error("Python script output:\n{}", output.toString());
                throw new RuntimeException("Python script failed with exit code " + exitCode + "\nOutput: " + output.toString());
            }
            
        } catch (Exception e) {
            logger.error("Failed to run Python script: {}", e.getMessage());
            throw new RuntimeException("Failed to run Python script: " + e.getMessage(), e);
        }
    }
}

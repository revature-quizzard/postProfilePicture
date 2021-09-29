package com.revature.post_profile_image.stubs;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class TestLogger implements LambdaLogger {

    // Fancy colors
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";

    private final Writer logFileWriter;

    public TestLogger() {
        String logDirectoryPath = "src/test/resources/logs";
        String fileName = "" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-hh-mm-ss"));
        String filePath = logDirectoryPath + "/" + fileName + ".log";

        File directory = new File(logDirectoryPath);
        if (! directory.exists()){
            directory.mkdir();
        }

        try {
            logFileWriter = new FileWriter(filePath, true);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    private void printMessageToConsole(String level, String message) {
        switch(level) {
            case "INFO":
                System.out.println(message);
                break;
            case "ERROR":
                System.out.println(ANSI_RED + message + ANSI_RESET);
        }
    }

    public void close() {
        try {
            logFileWriter.close();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Override
    public void log(String s) {
        try {
            String formattedString = "TEST LOG: " + s;
            logFileWriter.write(formattedString + "\n");
            printMessageToConsole("INFO", formattedString);
        } catch (IOException ioe) {
            printMessageToConsole("ERROR", "Could not write message to file.");
        }
    }

    @Override
    public void log(byte[] bytes) {
        try {
            String formattedString = "TEST LOG: " + Arrays.toString(bytes);
            logFileWriter.write(formattedString + "\n");
            printMessageToConsole("INFO", formattedString);
        } catch (IOException ioe) {
            printMessageToConsole("ERROR", "Could not write message to file!");
        }
    }
}

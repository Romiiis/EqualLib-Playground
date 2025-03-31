package com.romiis.equallibtestapp.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.romiis.equallibtestapp.CacheUtil;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;

/**
 * FileManager.java
 * <p>
 * Handles file saving and loading
 */
@Log4j2
public class FileManager {

    /**
     * File extension
     */
    private static final String EXTENSION = ".json";

    /**
     * Folder where the files are saved
     */
    private static final String SAVE_FOLDER = "savedFiles";


    /**
     * Get the names of the saved files
     *
     * @return An array of the names of the saved files
     */
    public static String[] getSavedFiles() {
        File folder = new File(SAVE_FOLDER);
        File[] files = folder.listFiles();

        if (files == null) {
            return new String[0];
        }

        String[] fileNames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            fileNames[i] = files[i].getName().replace(EXTENSION, "");
        }

        return fileNames;
    }

    /**
     * Save a file with the given name and content
     *
     * @param name    The name of the file
     * @param content The content of the file
     * @return True if the file was saved successfully, false otherwise
     */
    public static boolean saveFile(String name, String content) {

        // Create an ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        // Create the save folder if it doesn't exist
        if (!new File(SAVE_FOLDER).exists()) {

            boolean result = new File(SAVE_FOLDER).mkdir();

            if (!result) {
                log.error("Error creating save folder");
                return false;
            }
        }

        try {

            // Convert the JSON string into a JsonNode (tree model)
            JsonNode jsonNode = objectMapper.readTree(content);

            // Configure ObjectMapper to pretty print
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

            // Write the JSON tree to the file with pretty printing
            File file = new File(SAVE_FOLDER + File.separator + name + EXTENSION);
            objectMapper.writeValue(file, jsonNode);

            log.info("File saved: {}", file.getAbsolutePath());

            CacheUtil.getInstance().updateCache();

            return true;
        } catch (IOException e) {
            log.error("Error saving file", e);
        }

        return false;
    }


    /**
     * Load a file with the given name
     *
     * @param name The name of the file to load
     * @return The content of the file as a JSON string
     */
    public static String loadFile(String name) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            File file = new File(SAVE_FOLDER + File.separator + name + EXTENSION);
            JsonNode jsonNode = objectMapper.readTree(file);

            return objectMapper.writeValueAsString(jsonNode);
        } catch (IOException e) {
            log.error("Error loading file", e);
        }

        return null;
    }


}

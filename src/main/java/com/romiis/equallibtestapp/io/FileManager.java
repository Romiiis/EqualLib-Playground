package com.romiis.equallibtestapp.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

@Slf4j
public class FileManager {

    private static final String EXTENSION = ".json";
    private static final String SAVE_FOLDER = "savedFiles";

    public static boolean saveFile(String name, String content) {

        ObjectMapper objectMapper = new ObjectMapper();

        if (!new File(SAVE_FOLDER).exists()) {
            new File(SAVE_FOLDER).mkdir();
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

            return true;
        } catch (IOException e) {
            log.error("Error saving file", e);
        }

        return false;
    }


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

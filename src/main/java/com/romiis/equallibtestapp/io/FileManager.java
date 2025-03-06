package com.romiis.equallibtestapp.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

public class FileManager {

    private static final String SAVE_FOLDER = "savedFiles";

    public static void saveFile(String name, String content) {
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
            File file = new File(SAVE_FOLDER + File.separator + name + ".json");
            objectMapper.writeValue(file, jsonNode);

            System.out.println("File saved successfully at " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error saving file: " + e.getMessage());
        }
    }



}

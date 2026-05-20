package com.mybot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybot.model.Book;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NHentaiService {

    // python3 for mac/linux, python for windows
    private static final String PYTHON_COMMAND = "python3";
    private static final String PYTHON_SCRIPT_PATH = "crawler.py";
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public Book search(String query) {
        try {
            ProcessBuilder pb = new ProcessBuilder(PYTHON_COMMAND, PYTHON_SCRIPT_PATH, query);
            pb.directory(new File(System.getProperty("user.dir")));

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String jsonOutput = reader.lines().collect(Collectors.joining());

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // python problem
                return new Book("Python error with exit code: " + exitCode);
            }

            JsonNode rootNode = jsonMapper.readTree(jsonOutput);

            if (rootNode.get("success").asBoolean()) {
                String title = rootNode.get("title").asText();
                String url = rootNode.get("url").asText();
                String cover = rootNode.get("cover").asText();

                // make lists for every category
                List<String> artists = jsonToList(rootNode, "artists");
                List<String> groups = jsonToList(rootNode, "groups");
                List<String> languages = jsonToList(rootNode, "languages");
                List<String> tags = jsonToList(rootNode, "tags");
                List<String> parodies = jsonToList(rootNode, "parodies");
                List<String> characters = jsonToList(rootNode, "characters");
                List<String> pagesList = jsonToList(rootNode, "pages");
                String pages = "N/A";
                if (!pagesList.isEmpty()) {
                    pages = pagesList.get(0);
                }

                // return the Book
                return new Book(title, url, cover, artists, groups, languages, tags, parodies, characters, pages);
            } else {
                return new Book(rootNode.get("error").asText());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Book("Java error: " + e.getMessage());
        }
    }

    private List<String> jsonToList(JsonNode rootNode, String key) {
        List<String> list = new ArrayList<>();
        if (rootNode.has(key)) {
            rootNode.get(key).forEach(node -> list.add(node.asText()));
        }
        return list;
    }
}
package com.mybot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybot.model.BookResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NHentaiService {

    // python3 for mac/linux, python for windows
    private static final String PYTHON_COMMAND = "python3";
    private static final String PYTHON_SCRIPT_PATH = "crawler.py";
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public BookResult search(String query) {
        try {
            ProcessBuilder pb = new ProcessBuilder(PYTHON_COMMAND, PYTHON_SCRIPT_PATH, query);
            pb.directory(new File(System.getProperty("user.dir")));

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String jsonOutput = reader.lines().collect(Collectors.joining());

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // 如果 Python 崩潰，回傳一個裝著錯誤訊息的盒子
                return new BookResult("Python 腳本執行錯誤");
            }

            // 解析 JSON
            JsonNode rootNode = jsonMapper.readTree(jsonOutput);

            if (rootNode.get("success").asBoolean()) {
                String title = rootNode.get("title").asText();
                String url = rootNode.get("url").asText();
                String cover = rootNode.get("cover").asText();

                // 抽取分類的 Helper 方法 (下面定義)
                List<String> artists = jsonToList(rootNode, "artists");
                List<String> groups = jsonToList(rootNode, "groups");
                List<String> languages = jsonToList(rootNode, "languages");
                List<String> tags = jsonToList(rootNode, "tags");
                List<String> parodies = jsonToList(rootNode, "parodies");
                List<String> characters = jsonToList(rootNode, "characters");

                // 回傳新版的 BookResult
                return new BookResult(title, url, cover, artists, groups, languages, tags, parodies, characters);
            } else {
                return new BookResult(rootNode.get("error").asText());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new BookResult("Java 內部錯誤: " + e.getMessage());
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
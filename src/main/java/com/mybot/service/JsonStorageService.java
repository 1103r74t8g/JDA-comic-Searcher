package com.mybot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mybot.model.User;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JsonStorageService {

    // 資料庫檔案名稱 (會出現在你的專案根目錄)
    private static final String DB_FILE = "database.json";

    // Jackson 的核心工具 (負責把 Java 物件變文字，或把文字變物件)
    private final ObjectMapper mapper = new ObjectMapper();

    // 快取 (Cache)：這是我們的「記憶體資料庫」
    // 結構是：Map<User ID, User物件>
    // 這樣我們要找某個使用者的資料時，不用讀硬碟，直接從這裡拿，速度最快
    private Map<String, User> userCache;

    public JsonStorageService() {
        // 設定：輸出的 JSON 要自動換行縮排 (讓人眼看得懂)
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // 程式啟動時，先去硬碟把舊資料載入進來
        loadDatabase();
    }

    // --- 核心功能 ---

    // 1. 取得使用者資料 (如果他是第一次來，就自動幫他開戶)
    public User getUser(String userId) {
        // putIfAbsent: 如果 Map 裡沒這個人，就 new 一個新的放進去
        userCache.putIfAbsent(userId, new User(userId));
        return userCache.get(userId);
    }

    // 2. 儲存資料到硬碟 (Save)
    // 通常在有人按了收藏按鈕，或是機器人關機前呼叫
    public void saveDatabase() {
        try {
            // writeValue(目標檔案, 資料物件)
            mapper.writeValue(new File(DB_FILE), userCache);
            System.out.println("💾 資料庫已儲存！");
        } catch (IOException e) {
            System.err.println("❌ 儲存失敗: " + e.getMessage());
        }
    }

    // 3. 從硬碟讀取資料 (Load)
    private void loadDatabase() {
        File file = new File(DB_FILE);

        // 如果檔案根本不存在 (第一次執行)，就創一個空的 Map
        if (!file.exists()) {
            userCache = new HashMap<>();
            System.out.println("🆕 找不到舊資料庫，已建立新的。");
            saveDatabase(); // 先存一個空的檔案
            return;
        }

        try {
            // readValue 比較複雜，因為 Map 裡面包著 User，要用 TypeReference 告訴 Jackson 結構
            userCache = mapper.readValue(file, new TypeReference<Map<String, User>>() {
            });
            System.out.println("📂 資料庫載入成功，目前有 " + userCache.size() + " 位使用者的資料。");
        } catch (IOException e) {
            System.err.println("❌ 讀取資料庫失敗 (可能是格式壞了): " + e.getMessage());
            // 如果讀失敗，為了避免程式崩潰，還是給一個空的
            userCache = new HashMap<>();
        }
    }
}
package org.codetracker.util;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MethodCache {

    private final String cacheFilePath;
    private Map<String, Object> cacheMap = new HashMap<>();;
    private final ObjectMapper objectMapper;

    public MethodCache(String cacheFilePath) {
        this.cacheFilePath = cacheFilePath;
        this.objectMapper = new ObjectMapper();
        File file = new File(cacheFilePath);
        if (file.exists()) {
            try {
                this.cacheMap = loadCacheFromFile();
                System.out.println("Loaded from cache: " + this.cacheMap.size() + " entries");
            } catch (Exception ignored){}
        }
    }

    public void put(String key, Object value) throws IOException {
        if (!cacheMap.containsKey(key)){
            cacheMap.put(key, value);
//            saveCacheToFile();
        }
    }

    public String get(String key) throws IOException {
        Object value = cacheMap.get(key);
        if (value != null) {
            return value.toString();
        } else {
            return null;
        }
    }

    public boolean hasKey(String key) throws IOException {
        return cacheMap.containsKey(key);
    }

    private Map<String, Object> loadCacheFromFile() throws IOException {
        return objectMapper.readValue(new File(cacheFilePath), Map.class);
    }

    public void saveCacheToFile() throws IOException {
        File file = new File(cacheFilePath);
        if (file.exists()) {
            // Load existing cache data from file
            Map<String, Object> existingCacheData = loadCacheFromFile();
            System.out.println("Existing cache: " + existingCacheData.size() + " entries");
            // Merge existing cache data with current cache data
            existingCacheData.putAll(cacheMap);

            // Serialize merged cache data to JSON and write to file
            objectMapper.writeValue(file, existingCacheData);
            System.out.println("Updated cache: " + existingCacheData.size() + " entries");

        } else {
            // Serialize cache data to JSON and write to file
            objectMapper.writeValue(file, cacheMap);
            System.out.println("New cache: " + cacheMap.size() + " entries");
        }
    }
}

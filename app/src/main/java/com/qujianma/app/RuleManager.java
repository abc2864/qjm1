package com.qujianma.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 规则管理器 - 处理规则和关键词的离线加载
 */
public class RuleManager {
    private static final String TAG = "RuleManager";
    private static final String RULES_FILE_NAME = "pickup_rules.json";
    private static final String PREFS_NAME = "RuleManagerPrefs";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    
    private Context context;
    private Gson gson;
    
    public RuleManager(Context context) {
        this.context = context;
        this.gson = new Gson();
    }
    
    /**
     * 从本地文件加载规则和关键词
     * @return RulesData对象，包含规则和关键词列表
     */
    public RulesData loadRulesFromLocal() {
        try {
            File file = new File(context.getFilesDir(), RULES_FILE_NAME);
            if (file.exists()) {
                // 从本地文件读取
                FileInputStream fis = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                Type type = new TypeToken<RulesData>(){}.getType();
                RulesData rulesData = gson.fromJson(isr, type);
                isr.close();
                fis.close();
                Log.d(TAG, "成功从本地加载规则数据");
                return rulesData;
            } else {
                // 首次运行，复制assets中的默认规则文件
                return loadDefaultRules();
            }
        } catch (Exception e) {
            Log.e(TAG, "从本地加载规则失败", e);
            return loadDefaultRules();
        }
    }
    
    /**
     * 加载默认规则文件
     * @return RulesData对象
     */
    private RulesData loadDefaultRules() {
        try {
            InputStream is = context.getAssets().open("default_rules.json");
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            Type type = new TypeToken<RulesData>(){}.getType();
            RulesData rulesData = gson.fromJson(isr, type);
            isr.close();
            is.close();
            Log.d(TAG, "成功加载默认规则数据");
            return rulesData;
        } catch (Exception e) {
            Log.e(TAG, "加载默认规则失败", e);
            return new RulesData(); // 返回空对象
        }
    }
    
    /**
     * 保存规则数据到本地文件
     * @param rulesData 规则数据
     */
    public void saveRulesToLocal(RulesData rulesData) {
        try {
            String json = gson.toJson(rulesData);
            File file = new File(context.getFilesDir(), RULES_FILE_NAME);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(json.getBytes(StandardCharsets.UTF_8));
            fos.close();
            Log.d(TAG, "规则数据已保存到本地");
        } catch (IOException e) {
            Log.e(TAG, "保存规则数据到本地失败", e);
        }
    }
    
    /**
     * 首次启动时初始化规则
     * 自动下载网络规则并保存，无论网络是否可用
     */
    public void initializeRulesOnFirstLaunch() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        
        if (isFirstLaunch) {
            Log.d(TAG, "首次启动应用，开始初始化规则");
            
            // 总是尝试从网络获取最新规则
            Log.d(TAG, "尝试从网络获取最新规则");
            downloadAndSaveRules(() -> {
                // 标记首次启动已完成
                prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
            });
        }
    }
    
    /**
     * 从网络下载最新的规则数据并保存到本地
     * 如果下载失败，则使用默认规则
     */
    public void downloadAndSaveRules() {
        downloadAndSaveRules(null);
    }
    
    /**
     * 从网络下载最新的规则数据并保存到本地
     * 如果下载失败，则使用默认规则
     * @param callback 下载完成后的回调函数
     */
    public void downloadAndSaveRules(Runnable callback) {
        new Thread(() -> {
            try {
                URL url = new URL("http://8.137.57.54/api.php?action=get_rules");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                // 添加默认API密钥进行认证
                String defaultApiKey = "86849c0093c372c3edc7971ca49039b524720668950115e698a82677982eafa6";
                connection.setRequestProperty("X-API-Key", defaultApiKey);
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "网络请求响应码: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // 读取响应内容
                    InputStream is = connection.getInputStream();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, length);
                    }
                    
                    String response = baos.toString(StandardCharsets.UTF_8.name());
                    Log.d(TAG, "服务器响应内容: " + response);
                    is.close();
                    baos.close();
                    
                    // 解析JSON数据
                    Type type = new TypeToken<RulesData>(){}.getType();
                    RulesData rulesData = gson.fromJson(response, type);
                    
                    if (rulesData != null) {
                        // 保存到本地
                        saveRulesToLocal(rulesData);
                        
                        // 同时保存到SettingsActivity使用的SharedPreferences中
                        saveRulesToSettings(rulesData);
                        
                        Log.d(TAG, "成功下载并保存最新规则数据");
                    } else {
                        Log.e(TAG, "解析服务器响应失败，使用默认规则");
                        RulesData defaultRules = loadDefaultRules();
                        saveRulesToLocal(defaultRules);
                        saveRulesToSettings(defaultRules);
                    }
                } else {
                    Log.e(TAG, "网络请求失败，响应码: " + responseCode);
                    // 下载失败时使用默认规则
                    RulesData defaultRules = loadDefaultRules();
                    saveRulesToLocal(defaultRules);
                    saveRulesToSettings(defaultRules);
                    Log.d(TAG, "使用默认规则");
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "下载规则数据失败", e);
                // 下载失败时使用默认规则
                RulesData defaultRules = loadDefaultRules();
                saveRulesToLocal(defaultRules);
                saveRulesToSettings(defaultRules);
                Log.d(TAG, "使用默认规则");
            } finally {
                // 执行回调
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(callback);
                }
            }
        }).start();
    }
    
    /**
     * 保存规则到SettingsActivity使用的SharedPreferences中
     * @param rulesData 规则数据
     */
    private void saveRulesToSettings(RulesData rulesData) {
        try {
            if (rulesData.getRules() != null) {
                // 转换规则格式
                List<com.qujianma.app.Rule> networkRules = new java.util.ArrayList<>();
                for (Rule rule : rulesData.getRules()) {
                    // 注意：Rule类中的id是只读的，不能通过setId设置，需要在构造函数中设置
                    com.qujianma.app.Rule appRule = new com.qujianma.app.Rule(
                        rule.getId() != null ? rule.getId() : "",  // id参数
                        rule.getName() != null ? rule.getName() : "",  // name参数
                        rule.getRuleType() != null ? rule.getRuleType() : "regex",  // ruleType参数
                        rule.getDescription() != null ? rule.getDescription() : "",  // description参数
                        rule.getPattern() != null ? rule.getPattern() : "",  // pattern参数
                        "",  // tagPrefix参数（服务端暂未使用）
                        "",  // tagSuffix参数（服务端暂未使用）
                        "",  // phonePrefix参数（服务端暂未使用）
                        "",  // phoneSuffix参数（服务端暂未使用）
                        rule.getCodePrefix() != null ? rule.getCodePrefix() : "",  // codePrefix参数
                        rule.getCodeSuffix() != null ? rule.getCodeSuffix() : "",  // codeSuffix参数
                        rule.getAddressPrefix() != null ? rule.getAddressPrefix() : "",  // addressPrefix参数
                        rule.getAddressSuffix() != null ? rule.getAddressSuffix() : "",  // addressSuffix参数
                        rule.getStationPrefix() != null ? rule.getStationPrefix() : "",  // stationPrefix参数
                        rule.getStationSuffix() != null ? rule.getStationSuffix() : "",  // stationSuffix参数
                        true  // enabled参数（网络规则默认启用）
                    );
                    
                    // 为网络规则添加特定前缀以标识它们是网络规则
                    String newId = appRule.getId();
                    if (newId != null && !newId.startsWith("network-")) {
                        newId = "network-" + newId;
                        // 由于id是只读的，我们需要创建一个新对象
                        appRule = new com.qujianma.app.Rule(
                            newId,  // 新的id
                            appRule.getName(),
                            appRule.getRuleType(),
                            appRule.getDescription(),
                            appRule.getPattern(),
                            appRule.getTagPrefix(),
                            appRule.getTagSuffix(),
                            appRule.getPhonePrefix(),
                            appRule.getPhoneSuffix(),
                            appRule.getCodePrefix(),
                            appRule.getCodeSuffix(),
                            appRule.getAddressPrefix(),
                            appRule.getAddressSuffix(),
                            appRule.getStationPrefix(),
                            appRule.getStationSuffix(),
                            appRule.getEnabled()  // 修复：使用正确的getter方法
                        );
                    }
                    
                    networkRules.add(appRule);
                }
                
                // 保存到SharedPreferences
                SharedPreferences sharedPrefs = context.getSharedPreferences("downloaded_data", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                
                String rulesJson = gson.toJson(networkRules);
                editor.putString("downloaded_rules", rulesJson);
                editor.putLong("rules_last_updated", System.currentTimeMillis());
                editor.apply();
                
                Log.d(TAG, "规则已保存到SettingsActivity使用的SharedPreferences中");
            }
        } catch (Exception e) {
            Log.e(TAG, "保存规则到SettingsActivity SharedPreferences失败", e);
        }
    }
    
    /**
     * 规则数据类
     */
    public static class RulesData {
        private String version;
        private List<Rule> rules;
        private List<Keyword> keywords;
        
        public RulesData() {
            // 默认构造函数
        }
        
        // Getters and Setters
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public List<Rule> getRules() {
            return rules;
        }
        
        public void setRules(List<Rule> rules) {
            this.rules = rules;
        }
        
        public List<Keyword> getKeywords() {
            return keywords;
        }
        
        public void setKeywords(List<Keyword> keywords) {
            this.keywords = keywords;
        }
    }
    
    /**
     * 规则类（增强版，支持正则规则和自定义前后缀规则）
     */
    public static class Rule {
        private String id;
        private String name;
        private String rule_type; // "regex" 或 "custom"
        // 正则规则字段
        private String pattern;
        // 自定义前后缀规则字段
        private String sender_prefix;
        private String sender_suffix;
        private String code_prefix;
        private String code_suffix;
        private String station_prefix;
        private String station_suffix;
        private String address_prefix;
        private String address_suffix;
        private String description;
        private int is_active;
        
        public Rule() {
            // 默认构造函数
        }
        
        // Getters and Setters
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getRuleType() {
            return rule_type;
        }
        
        public void setRuleType(String rule_type) {
            this.rule_type = rule_type;
        }
        
        public String getPattern() {
            return pattern;
        }
        
        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
        
        public String getSenderPrefix() {
            return sender_prefix;
        }
        
        public void setSenderPrefix(String sender_prefix) {
            this.sender_prefix = sender_prefix;
        }
        
        public String getSenderSuffix() {
            return sender_suffix;
        }
        
        public void setSenderSuffix(String sender_suffix) {
            this.sender_suffix = sender_suffix;
        }
        
        public String getCodePrefix() {
            return code_prefix;
        }
        
        public void setCodePrefix(String code_prefix) {
            this.code_prefix = code_prefix;
        }
        
        public String getCodeSuffix() {
            return code_suffix;
        }
        
        public void setCodeSuffix(String code_suffix) {
            this.code_suffix = code_suffix;
        }
        
        public String getStationPrefix() {
            return station_prefix;
        }
        
        public void setStationPrefix(String station_prefix) {
            this.station_prefix = station_prefix;
        }
        
        public String getStationSuffix() {
            return station_suffix;
        }
        
        public void setStationSuffix(String station_suffix) {
            this.station_suffix = station_suffix;
        }
        
        public String getAddressPrefix() {
            return address_prefix;
        }
        
        public void setAddressPrefix(String address_prefix) {
            this.address_prefix = address_prefix;
        }
        
        public String getAddressSuffix() {
            return address_suffix;
        }
        
        public void setAddressSuffix(String address_suffix) {
            this.address_suffix = address_suffix;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public int getIsActive() {
            return is_active;
        }
        
        public void setIsActive(int is_active) {
            this.is_active = is_active;
        }
    }
    
    /**
     * 关键词类
     */
    public static class Keyword {
        private String id;
        private String keyword;
        private String type; // "sender" 或 "content"
        private String description;
        
        public Keyword() {
            // 默认构造函数
        }
        
        // Getters and Setters
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getKeyword() {
            return keyword;
        }
        
        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
}
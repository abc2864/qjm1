package com.qujianma.app

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

// 定义Rule数据类，用于解析服务器返回的规则数据
data class ServerRule(
    val id: String = "",
    val name: String = "",
    val rule_type: String = "regex",
    val pattern: String = "",
    val code_prefix: String = "",
    val code_suffix: String = "",
    val station_prefix: String = "",
    val station_suffix: String = "",
    val address_prefix: String = "",
    val address_suffix: String = "",
    val description: String = "",
    val is_active: String = "1"
)

class ServerSyncManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
        
    private val baseUrl = "http://8.137.57.54" // 使用新的Web规则管理系统API
    
    companion object {
        private const val TAG = "ServerSyncManager"
        private const val PREFS_NAME = "server_sync"
        private const val LAST_SYNC_TIME = "last_sync_time"
        private const val SYNC_INTERVAL = 4 * 60 * 60 * 1000L // 4小时同步间隔
    }
    
    /**
     * 检查是否需要同步（距离上次同步超过4小时）
     */
    fun shouldSync(): Boolean {
        val lastSyncTime = getLastSyncTime()
        val currentTime = System.currentTimeMillis()
        return currentTime - lastSyncTime > SYNC_INTERVAL
    }
    
    /**
     * 从服务器同步规则和关键词
     */
    fun syncFromServer(apiKey: String, callback: (Boolean, String) -> Unit) {
        // 检查网络连接
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback(false, "网络不可用，请检查网络连接")
            return
        }
        
        // 如果没有提供API密钥，则使用默认API密钥
        val actualApiKey = if (apiKey.isNotEmpty()) apiKey else DataManager.DEFAULT_API_KEY
        // 同步规则和关键词
        syncRulesAndKeywords(actualApiKey) { success, message ->
            if (success) {
                // 保存同步时间
                saveLastSyncTime()
                callback(true, "规则和关键词同步成功")
            } else {
                callback(false, message)
            }
        }
    }
    
    /**
     * 同步规则和关键词
     */
    private fun syncRulesAndKeywords(apiKey: String, callback: (Boolean, String) -> Unit) {
        val request = Request.Builder()
            .url("$baseUrl/api.php?action=get_rules")
            .addHeader("X-API-Key", apiKey) // 添加API密钥到HTTP头部
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to sync rules and keywords", e)
                callback(false, "同步规则和关键词失败: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    try {
                        val responseBody = response.body?.string()
                        val (rulesList, keywordsList) = parseRulesAndKeywordsFromJson(responseBody ?: "")
                        saveDownloadedRules(rulesList)
                        saveDownloadedKeywords(keywordsList)
                        callback(true, "规则和关键词同步成功")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse rules and keywords", e)
                        callback(false, "解析规则和关键词失败: ${e.message}")
                    }
                } else {
                    // 检查是否是认证失败
                    if (response.code == 401 || response.code == 403) {
                        callback(false, "API密钥无效或认证失败")
                    } else {
                        callback(false, "服务器响应错误: ${response.code}")
                    }
                }
            }
        })
    }
    
    /**
     * 从JSON响应中解析规则和关键词数据
     */
    private fun parseRulesAndKeywordsFromJson(json: String): Pair<List<Rule>, List<Map<String, String>>> {
        return try {
            val gson = Gson()
            val jsonObject = JsonParser.parseString(json).asJsonObject
            
            // 检查是否有错误信息
            if (jsonObject.has("success") && !jsonObject.get("success").asBoolean) {
                val message = jsonObject.get("message")?.asString ?: "未知错误"
                throw Exception(message)
            }
            
            // 解析规则
            val rulesList = if (jsonObject.has("rules")) {
                val rulesArray = jsonObject.getAsJsonArray("rules")
                rulesArray.map { ruleElement ->
                    val ruleObject = ruleElement.asJsonObject
                    val serverRule = gson.fromJson(ruleObject, ServerRule::class.java)
                    
                    // 转换为本地Rule对象
                    Rule(
                        id = serverRule.id,
                        name = serverRule.name,
                        ruleType = serverRule.rule_type,
                        pattern = serverRule.pattern,
                        codePrefix = serverRule.code_prefix,
                        codeSuffix = serverRule.code_suffix,
                        stationPrefix = serverRule.station_prefix,
                        stationSuffix = serverRule.station_suffix,
                        addressPrefix = serverRule.address_prefix,
                        addressSuffix = serverRule.address_suffix,
                        description = serverRule.description,
                        enabled = serverRule.is_active == "1"
                    )
                }
            } else {
                emptyList()
            }
            
            // 解析关键词
            val keywordsList = if (jsonObject.has("keywords")) {
                val keywordsArray = jsonObject.getAsJsonArray("keywords")
                keywordsArray.map { keywordElement ->
                    val keywordObject = keywordElement.asJsonObject
                    mapOf(
                        "id" to (keywordObject.get("id")?.asString ?: ""),
                        "keyword" to (keywordObject.get("keyword")?.asString ?: ""),
                        "type" to (keywordObject.get("type")?.asString ?: ""),
                        "description" to (keywordObject.get("description")?.asString ?: "")
                    )
                }
            } else {
                emptyList()
            }
            
            Pair(rulesList, keywordsList)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse rules and keywords", e)
            Pair(emptyList(), emptyList())
        }
    }
    
    /**
     * 保存下载的规则
     */
    private fun saveDownloadedRules(rules: List<Rule>) {
        val sharedPrefs = context.getSharedPreferences("downloaded_data", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        
        // 保存规则
        val rulesJson = Gson().toJson(rules)
        editor.putString("downloaded_rules", rulesJson)
        
        editor.apply()
    }
    
    /**
     * 保存下载的关键词
     */
    private fun saveDownloadedKeywords(keywords: List<Map<String, String>>) {
        val sharedPrefs = context.getSharedPreferences("downloaded_data", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        
        // 保存关键词
        val keywordsJson = Gson().toJson(keywords)
        editor.putString("downloaded_keywords", keywordsJson)
        
        editor.apply()
    }
    
    /**
     * 保存最后同步时间
     */
    private fun saveLastSyncTime() {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putLong(LAST_SYNC_TIME, System.currentTimeMillis())
        editor.apply()
    }
    
    /**
     * 获取最后同步时间
     */
    fun getLastSyncTime(): Long {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getLong(LAST_SYNC_TIME, 0)
    }
    
    /**
     * 加载下载的规则
     */
    fun loadDownloadedRules(): List<Rule> {
        val sharedPrefs = context.getSharedPreferences("downloaded_data", Context.MODE_PRIVATE)
        val rulesJson = sharedPrefs.getString("downloaded_rules", null)
        
        return if (rulesJson != null) {
            try {
                val type = object : TypeToken<List<Rule>>() {}.type
                Gson().fromJson(rulesJson, type)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse downloaded rules", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * 加载下载的关键词
     */
    fun loadDownloadedKeywords(): List<Map<String, String>> {
        val sharedPrefs = context.getSharedPreferences("downloaded_data", Context.MODE_PRIVATE)
        val keywordsJson = sharedPrefs.getString("downloaded_keywords", null)
        
        return if (keywordsJson != null) {
            try {
                val type = object : TypeToken<List<Map<String, String>>>() {}.type
                Gson().fromJson(keywordsJson, type)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse downloaded keywords", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}
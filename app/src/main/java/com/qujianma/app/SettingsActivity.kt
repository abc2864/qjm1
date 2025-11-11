package com.qujianma.app

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var rulesRecyclerView: RecyclerView
    private lateinit var addRuleButton: Button
    private lateinit var updateFromServerButton: Button
    private lateinit var saveApiKeyButton: Button
    private lateinit var rulesAdapter: com.qujianma.app.SettingRulesAdapter
    
    // 添加规则列表
    private val rules = mutableListOf<Rule>()
    
    // 添加一个标志来防止重复点击
    private var isUpdating = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "设置"
        
        initViews()
        setupRecyclerView()
        loadSavedRules()
    }
    
    private fun initViews() {
        rulesRecyclerView = findViewById(R.id.rules_recycler_view)
        addRuleButton = findViewById(R.id.add_rule_button)
        updateFromServerButton = findViewById(R.id.btn_update_from_server)
        saveApiKeyButton = findViewById(R.id.btn_save_api_key)
        val networkDiagnosisButton = findViewById<Button>(R.id.btn_network_diagnosis)
        
        addRuleButton.setOnClickListener {
            val intent = Intent(this, RuleEditActivity::class.java)
            addRuleLauncher.launch(intent)
        }
        
        updateFromServerButton.setOnClickListener {
            // 检查是否正在更新中，防止重复点击
            if (isUpdating) {
                Toast.makeText(this, "正在更新中，请稍候...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // 设置更新状态为true
            isUpdating = true
            
            // 获取保存的API密钥（自定义或默认）
            val savedApiKey = getSavedApiKey()
            // 直接使用保存的API密钥更新规则（无论是自定义还是默认）
            updateRulesAndKeywordsFromServer(savedApiKey)
        }
        
        saveApiKeyButton.setOnClickListener {
            showSaveApiKeyDialog()
        }
        
        networkDiagnosisButton.setOnClickListener {
            val intent = Intent(this, NetworkDiagnosisActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun showSaveApiKeyDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_express, null)
        val apiKeyInput = EditText(this)
        apiKeyInput.hint = "请输入API密钥"
        
        // 设置已保存的API密钥（只有是非默认API密钥时才显示）
        val sharedPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val savedApiKey = sharedPrefs.getString("api_key", "") ?: ""
        if (savedApiKey.isNotEmpty() && savedApiKey != DataManager.DEFAULT_API_KEY) {
            apiKeyInput.setText(savedApiKey)
        }
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("保存API密钥")
            .setView(apiKeyInput)
            .setPositiveButton("保存") { _, _ ->
                val apiKey = apiKeyInput.text.toString().trim()
                if (apiKey.isNotEmpty()) {
                    saveApiKey(apiKey)
                    Toast.makeText(this, "API密钥已保存", Toast.LENGTH_SHORT).show()
                    // 保存后立即使用新密钥更新规则
                    updateRulesAndKeywordsFromServer(apiKey)
                } else {
                    Toast.makeText(this, "请输入有效的API密钥", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .create()
        
        dialog.show()
    }
    
    private fun showApiKeyDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_express, null)
        val apiKeyInput = EditText(this)
        apiKeyInput.hint = "请输入API密钥"
        
        // 设置已保存的API密钥（只有是非默认API密钥时才显示）
        val sharedPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val savedApiKey = sharedPrefs.getString("api_key", "") ?: ""
        if (savedApiKey.isNotEmpty() && savedApiKey != DataManager.DEFAULT_API_KEY) {
            apiKeyInput.setText(savedApiKey)
        }
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("API密钥验证")
            .setView(apiKeyInput)
            .setPositiveButton("确定") { _, _ ->
                val apiKey = apiKeyInput.text.toString().trim()
                if (apiKey.isNotEmpty()) {
                    updateRulesAndKeywordsFromServer(apiKey)
                } else {
                    Toast.makeText(this, "请输入有效的API密钥", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消") { _, _ ->
                // 用户取消输入，使用默认API密钥
                updateRulesAndKeywordsFromServer(DataManager.DEFAULT_API_KEY)
            }
            .create()
        
        dialog.show()
    }
    
    private fun setupRecyclerView() {
        rulesRecyclerView.layoutManager = LinearLayoutManager(this)
        rulesAdapter = com.qujianma.app.SettingRulesAdapter(rules) { rule, action, position ->
            when (action) {
                "edit" -> {
                    val intent = Intent(this, RuleEditActivity::class.java)
                    intent.putExtra(RuleEditActivity.EXTRA_RULE, rule)
                    intent.putExtra(RuleEditActivity.EXTRA_RULE_POSITION, position)
                    editRuleLauncher.launch(intent)
                }
                "view" -> showRuleDetail(rule)
                "delete" -> deleteRule(rule)
                "toggle" -> toggleRule(rule)
            }
        }
        rulesRecyclerView.adapter = rulesAdapter
    }
    
    private fun loadSavedRules() {
        // 加载本地规则
        val localRules = loadRules(this)
        
        // 加载下载的网络规则
        val downloadedRules = loadDownloadedRules(this)
        
        // 合并规则列表，网络规则在前，本地规则在后
        rules.clear()
        rules.addAll(downloadedRules)
        rules.addAll(localRules)
        
        rulesAdapter.notifyDataSetChanged()
    }
    
    private fun updateRulesAndKeywordsFromServer(apiKey: String) {
        // 如果没有提供API密钥，则使用默认API密钥
        val actualApiKey = if (apiKey.isNotEmpty()) apiKey else DataManager.DEFAULT_API_KEY
        
        // 首先检查网络连接
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "网络不可用，请检查网络连接", Toast.LENGTH_LONG).show()
            // 重置更新状态
            isUpdating = false
            return
        }
        
        // 使用ProgressBar替代已弃用的ProgressDialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_express, null)
        val progressBar = ProgressBar(this)
        val messageText = TextView(this).apply {
            text = "正在从服务器更新规则和关键词..."
            setPadding(16, 16, 16, 16)
        }
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(progressBar)
            addView(messageText)
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(layout)
            .setCancelable(false)
            .create()
        
        dialog.show()
        
        // 创建OkHttpClient实例
        val client = OkHttpClient()
        
        // 创建请求更新规则和关键词的请求（使用新的Web规则管理系统API）
        val rulesRequest = Request.Builder()
            .url("http://8.137.57.54/api.php?action=get_rules") // 使用新的Web规则管理系统API
            .addHeader("X-API-Key", actualApiKey) // 添加API密钥到HTTP头部
            .build()
        
        // 异步执行请求
        client.newCall(rulesRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 在主线程中更新UI
                Handler(Looper.getMainLooper()).post {
                    dialog.dismiss()
                    Log.e("SettingsActivity", "Network request failed", e)
                    Toast.makeText(this@SettingsActivity, "网络请求失败: ${e.message}", Toast.LENGTH_LONG).show()
                    // 重置更新状态
                    isUpdating = false
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                Log.d("SettingsActivity", "Received HTTP response. Code: ${response.code}")
                
                if (response.isSuccessful) {
                    try {
                        val responseBody = response.body?.string()
                        Log.d("SettingsActivity", "Response body: $responseBody")
                        
                        // 检查响应体是否为空
                        if (responseBody.isNullOrEmpty()) {
                            throw Exception("服务器返回空响应")
                        }
                        
                        // 检查响应是否为有效的JSON
                        if (!responseBody.trim().startsWith("{") && !responseBody.trim().startsWith("[")) {
                            throw Exception("服务器返回非JSON响应: $responseBody")
                        }
                        
                        // 解析规则和关键词数据
                        val (rulesList, keywordsList) = parseRulesAndKeywordsFromJson(responseBody)
                        
                        // 保存规则到SharedPreferences
                        saveDownloadedRules(rulesList)
                        
                        // 保存关键词到SharedPreferences
                        saveDownloadedKeywords(keywordsList)
                        
                        // 在主线程中更新UI
                        Handler(Looper.getMainLooper()).post {
                            dialog.dismiss()
                            Toast.makeText(this@SettingsActivity, "规则和关键词更新成功", Toast.LENGTH_SHORT).show()
                            loadSavedRules() // 重新加载规则列表
                            // 重置更新状态
                            isUpdating = false
                        }
                    } catch (e: Exception) {
                        Log.e("SettingsActivity", "解析规则数据失败", e)
                        Handler(Looper.getMainLooper()).post {
                            dialog.dismiss()
                            Toast.makeText(this@SettingsActivity, "解析规则数据失败: ${e.message}", Toast.LENGTH_LONG).show()
                            // 重置更新状态
                            isUpdating = false
                        }
                    }
                } else {
                    val errorBody = response.body?.string()
                    Log.e("SettingsActivity", "Server returned error code: ${response.code}, message: ${response.message}, body: $errorBody")
                    
                    Handler(Looper.getMainLooper()).post {
                        dialog.dismiss()
                        // 检查是否是认证错误
                        if (response.code == 401 || response.code == 403) {
                            Toast.makeText(this@SettingsActivity, "API密钥验证失败，请检查API密钥", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@SettingsActivity, "服务器返回错误: ${response.message}", Toast.LENGTH_LONG).show()
                        }
                        // 重置更新状态
                        isUpdating = false
                    }
                }
            }
        })
    }
    
    /**
     * 从JSON响应中解析规则和关键词数据
     */
    private fun parseRulesAndKeywordsFromJson(json: String): Pair<List<Rule>, List<Map<String, Any>>> {
        return try {
            Log.d("SettingsActivity", "Parsing JSON: $json")
            val gson = Gson()
            val jsonObject = com.google.gson.JsonParser.parseString(json).asJsonObject
            Log.d("SettingsActivity", "Parsed JSON object: $jsonObject")
            
            // 检查是否有错误信息
            if (jsonObject.has("success") && !jsonObject.get("success").asBoolean) {
                val message = jsonObject.get("message")?.asString ?: "未知错误"
                throw Exception(message)
            }
            
            // 解析规则
            val rulesList = if (jsonObject.has("rules")) {
                val rulesArray = jsonObject.getAsJsonArray("rules")
                Log.d("SettingsActivity", "Rules array size: ${rulesArray.size()}")
                rulesArray.mapNotNull { ruleElement ->
                    try {
                        val ruleObject = ruleElement.asJsonObject
                        Log.d("SettingsActivity", "Parsing rule: $ruleObject")
                        Rule(
                            id = getJsonStringSafely(ruleObject, "id"),
                            name = getJsonStringSafely(ruleObject, "name"),
                            ruleType = getJsonStringSafely(ruleObject, "ruleType", "rule_type"),
                            pattern = getJsonStringSafely(ruleObject, "pattern"),
                            codePrefix = getJsonStringSafely(ruleObject, "codePrefix", "code_prefix"),
                            codeSuffix = getJsonStringSafely(ruleObject, "codeSuffix", "code_suffix"),
                            stationPrefix = getJsonStringSafely(ruleObject, "stationPrefix", "station_prefix"),
                            stationSuffix = getJsonStringSafely(ruleObject, "stationSuffix", "station_suffix"),
                            addressPrefix = getJsonStringSafely(ruleObject, "addressPrefix", "address_prefix"),
                            addressSuffix = getJsonStringSafely(ruleObject, "addressSuffix", "address_suffix"),
                            tagPrefix = getJsonStringSafely(ruleObject, "tagPrefix"),
                            tagSuffix = getJsonStringSafely(ruleObject, "tagSuffix"),
                            phonePrefix = getJsonStringSafely(ruleObject, "phonePrefix"),
                            phoneSuffix = getJsonStringSafely(ruleObject, "phoneSuffix"),
                            description = getJsonStringSafely(ruleObject, "description"),
                            enabled = getJsonBooleanSafely(ruleObject, "enabled", "is_active")
                        )
                    } catch (e: Exception) {
                        Log.e("SettingsActivity", "Failed to parse individual rule: $ruleElement", e)
                        null // Skip this rule and continue with others
                    }
                }
            } else {
                Log.d("SettingsActivity", "No rules found in response")
                emptyList()
            }
            
            // 解析关键词
            val keywordsList = if (jsonObject.has("keywords")) {
                val keywordsArray = jsonObject.getAsJsonArray("keywords")
                Log.d("SettingsActivity", "Keywords array size: ${keywordsArray.size()}")
                keywordsArray.mapNotNull { keywordElement ->
                    try {
                        val keywordObject = keywordElement.asJsonObject
                        Log.d("SettingsActivity", "Parsing keyword: $keywordObject")
                        mapOf(
                            "id" to getJsonStringSafely(keywordObject, "id"),
                            "keyword" to getJsonStringSafely(keywordObject, "keyword"),
                            "type" to getJsonStringSafely(keywordObject, "type"),
                            "description" to getJsonStringSafely(keywordObject, "description")
                        )
                    } catch (e: Exception) {
                        Log.e("SettingsActivity", "Failed to parse individual keyword: $keywordElement", e)
                        null // Skip this keyword and continue with others
                    }
                }
            } else {
                Log.d("SettingsActivity", "No keywords found in response")
                emptyList()
            }
            
            Log.d("SettingsActivity", "Successfully parsed ${rulesList.size} rules and ${keywordsList.size} keywords")
            Pair(rulesList, keywordsList)
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Failed to parse rules and keywords", e)
            Log.e("SettingsActivity", "JSON content that failed to parse: $json")
            throw e // 重新抛出异常，让调用者处理
        }
    }
    
    /**
     * 安全地从JSON对象中获取字符串
     */
    private fun getJsonStringSafely(jsonObject: JsonObject, key: String, fallbackKey: String? = null): String {
        return try {
            var element = jsonObject.get(key)
            // 如果主键不存在且提供了备用键，则尝试备用键
            if ((element == null || element.isJsonNull) && fallbackKey != null) {
                element = jsonObject.get(fallbackKey)
            }
            
            if (element != null && !element.isJsonNull) {
                element.asString
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.w("SettingsActivity", "Failed to get string value for key: $key, fallback: $fallbackKey", e)
            ""
        }
    }
    
    /**
     * 安全地从JSON对象中获取布尔值
     */
    private fun getJsonBooleanSafely(jsonObject: JsonObject, key: String, fallbackKey: String? = null): Boolean {
        return try {
            var element = jsonObject.get(key)
            // 如果主键不存在且提供了备用键，则尝试备用键
            if ((element == null || element.isJsonNull) && fallbackKey != null) {
                element = jsonObject.get(fallbackKey)
            }
            
            if (element != null && !element.isJsonNull) {
                // 处理字符串形式的布尔值（如"1"表示true）
                if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
                    val stringValue = element.asString
                    // "1"或"true"表示true，其他值表示false
                    stringValue == "1" || stringValue.equals("true", ignoreCase = true)
                } else {
                    element.asBoolean
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w("SettingsActivity", "Failed to get boolean value for key: $key, fallback: $fallbackKey", e)
            false
        }
    }
    
    private fun parseRulesFromJson(json: String): List<Rule> {
        return try {
            val gson = Gson()
            val type = object : TypeToken<List<Rule>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Failed to parse rules", e)
            emptyList()
        }
    }
    
    private fun parseKeywordsFromJson(json: String): List<Map<String, Any>> {
        return try {
            val gson = Gson()
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Failed to parse keywords", e)
            emptyList()
        }
    }
    
    private fun saveDownloadedRules(rules: List<Rule>) {
        val sharedPrefs = getSharedPreferences("downloaded_data", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        
        // 为网络规则添加特定前缀以标识它们是网络规则，但保持ID唯一
        val networkRules = rules.map { rule ->
            // 如果规则ID已经包含network前缀，则保留原ID，否则添加前缀
            val newId = if (rule.id.startsWith("network-")) {
                rule.id
            } else {
                "network-${rule.id}"
            }
            rule.copy(id = newId)
        }
        
        // 检查是否与已保存的规则相同，避免重复保存
        val existingRulesJson = sharedPrefs.getString("downloaded_rules", "")
        val newRulesJson = Gson().toJson(networkRules)
        
        // 如果规则没有变化，则不重复保存
        if (existingRulesJson == newRulesJson) {
            Log.d("SettingsActivity", "Rules unchanged, skipping save")
            return
        }
        
        // 保存规则
        editor.putString("downloaded_rules", newRulesJson)
        
        // 保存更新时间
        editor.putLong("rules_last_updated", System.currentTimeMillis())
        
        editor.apply()
    }
    
    private fun saveDownloadedKeywords(keywords: List<Map<String, Any>>) {
        val sharedPrefs = getSharedPreferences("downloaded_data", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        
        // 保存关键词
        val keywordsJson = Gson().toJson(keywords)
        editor.putString("downloaded_keywords", keywordsJson)
        
        // 保存更新时间
        editor.putLong("keywords_last_updated", System.currentTimeMillis())
        
        editor.apply()
    }
    
    /**
     * 保存API密钥到SharedPreferences
     */
    private fun saveApiKey(apiKey: String) {
        val sharedPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("api_key", apiKey).apply()
    }
    
    /**
     * 从SharedPreferences获取保存的API密钥
     */
    private fun getSavedApiKey(): String {
        val sharedPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val savedKey = sharedPrefs.getString("api_key", "") ?: ""
        // 如果没有保存的API密钥，则使用默认API密钥
        return if (savedKey.isNotEmpty()) savedKey else DataManager.DEFAULT_API_KEY
    }
    
    private fun deleteRule(rule: Rule) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirm, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // 设置对话框文本
        dialogView.findViewById<TextView>(R.id.dialog_title).text = "确认删除"
        dialogView.findViewById<TextView>(R.id.dialog_message).text = "确定要删除这条规则吗？"

        // 设置按钮点击事件
        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btn_delete).setOnClickListener {
            rules.remove(rule)
            
            // 如果是网络规则，还需要从网络规则中删除
            if (rule.id.startsWith("network-")) {
                deleteDownloadedRule(rule)
            } else {
                // 如果是本地规则，则从本地规则中删除
                saveRules(rules.toList())
            }
            
            // 更新适配器而不是重新加载所有规则
            rulesAdapter.notifyDataSetChanged()
            Toast.makeText(this, "规则已删除", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
    
    private fun deleteDownloadedRule(rule: Rule) {
        val sharedPrefs = getSharedPreferences("downloaded_data", Context.MODE_PRIVATE)
        val downloadedRulesJson = sharedPrefs.getString("downloaded_rules", "")
        
        if (!downloadedRulesJson.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<Rule>>() {}.type
                val downloadedRules = Gson().fromJson<List<Rule>>(downloadedRulesJson, type).toMutableList()
                
                // 从下载规则列表中移除该规则
                downloadedRules.removeAll { it.id == rule.id }
                
                // 保存更新后的规则列表
                val editor = sharedPrefs.edit()
                if (downloadedRules.isEmpty()) {
                    editor.remove("downloaded_rules")
                } else {
                    val rulesJson = Gson().toJson(downloadedRules)
                    editor.putString("downloaded_rules", rulesJson)
                }
                editor.apply()
            } catch (e: Exception) {
                Log.e("SettingsActivity", "Failed to delete downloaded rule", e)
            }
        }
    }
    
    private fun toggleRule(rule: Rule) {
        rule.enabled = !rule.enabled
        saveRules(rules.toList())
        // 使用Handler推迟notifyDataSetChanged()调用，避免在RecyclerView布局过程中更新
        Handler(Looper.getMainLooper()).post {
            rulesAdapter.notifyDataSetChanged()
        }
    }
    
    
    private fun saveRules(rules: List<Rule>) {
        val jsonString = Gson().toJson(rules)
        val sharedPrefs = getSharedPreferences("rules", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("rules_data", jsonString).apply()
    }
    
    companion object {
        const val REQUEST_CODE_ADD_RULE = 1000
        const val REQUEST_CODE_EDIT_RULE = 1001

        fun loadRules(context: Context): List<Rule> {
            val sharedPrefs = context.getSharedPreferences("rules", Context.MODE_PRIVATE)
            val jsonString = sharedPrefs.getString("rules_data", "")
            
            return if (!jsonString.isNullOrEmpty()) {
                try {
                    val type = object : TypeToken<List<Rule>>() {}.type
                    Gson().fromJson(jsonString, type)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
        
        fun loadDownloadedRules(context: Context): List<Rule> {
            val sharedPrefs = context.getSharedPreferences("downloaded_data", Context.MODE_PRIVATE)
            val jsonString = sharedPrefs.getString("downloaded_rules", "")
            
            return if (!jsonString.isNullOrEmpty()) {
                try {
                    val type = object : TypeToken<List<Rule>>() {}.type
                    Gson().fromJson(jsonString, type)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
        
        fun loadDownloadedKeywords(context: Context): List<Map<String, Any>> {
            val sharedPrefs = context.getSharedPreferences("downloaded_data", Context.MODE_PRIVATE)
            val jsonString = sharedPrefs.getString("downloaded_keywords", "")
            
            return if (!jsonString.isNullOrEmpty()) {
                try {
                    val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                    Gson().fromJson(jsonString, type)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }
    
    // 注册Activity Result回调替代startActivityForResult
    private val addRuleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        handleRuleActivityResult(result, REQUEST_CODE_ADD_RULE)
    }
    
    private val editRuleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        handleRuleActivityResult(result, REQUEST_CODE_EDIT_RULE)
    }
    
    private fun handleRuleActivityResult(result: ActivityResult, requestCode: Int) {
        if (result.resultCode == RuleEditActivity.RESULT_CODE && result.data != null) {
            val rule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getSerializableExtra(RuleEditActivity.EXTRA_RULE_RESULT, Rule::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getSerializableExtra(RuleEditActivity.EXTRA_RULE_RESULT) as? Rule
            } ?: return
            
            when (requestCode) {
                REQUEST_CODE_ADD_RULE -> {
                    rules.add(rule)
                    refreshRules()
                    Toast.makeText(this, "规则已添加", Toast.LENGTH_SHORT).show()
                }
                REQUEST_CODE_EDIT_RULE -> {
                    val position = result.data?.getIntExtra(RuleEditActivity.EXTRA_RULE_POSITION, -1) ?: -1
                    if (position in rules.indices) {
                        rules[position] = rule
                        refreshRules()
                        Toast.makeText(this, "规则已更新", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun refreshRules() {
        saveRules(rules.toList())
        loadSavedRules()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    /**
     * 检查网络是否可用
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            networkCapabilities != null && networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true
        }
    }
    
    private fun showRuleDetail(rule: Rule) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rule_view, null)
        
        // 填充规则详情
        dialogView.findViewById<TextView>(R.id.rule_detail_id).text = rule.id
        dialogView.findViewById<TextView>(R.id.rule_detail_name).text = rule.name
        dialogView.findViewById<TextView>(R.id.rule_detail_type).text = if (rule.id.startsWith("network-")) "网络规则" else "本地规则"
        dialogView.findViewById<TextView>(R.id.rule_detail_rule_type).text = when (rule.ruleType) {
            "regex" -> "正则规则"
            "custom" -> "自定义规则"
            else -> rule.ruleType
        }
        dialogView.findViewById<TextView>(R.id.rule_detail_description).text = rule.description
        dialogView.findViewById<TextView>(R.id.rule_detail_pattern).text = rule.pattern
        dialogView.findViewById<TextView>(R.id.rule_detail_code_prefix).text = rule.codePrefix
        dialogView.findViewById<TextView>(R.id.rule_detail_code_suffix).text = rule.codeSuffix
        dialogView.findViewById<TextView>(R.id.rule_detail_station_prefix).text = rule.stationPrefix
        dialogView.findViewById<TextView>(R.id.rule_detail_station_suffix).text = rule.stationSuffix
        dialogView.findViewById<TextView>(R.id.rule_detail_address_prefix).text = rule.addressPrefix
        dialogView.findViewById<TextView>(R.id.rule_detail_address_suffix).text = rule.addressSuffix
        dialogView.findViewById<TextView>(R.id.rule_detail_enabled).text = if (rule.enabled) "已启用" else "已禁用"
        
        // 根据规则类型显示或隐藏相关字段
        when (rule.ruleType) {
            "regex" -> {
                // 正则规则：显示正则表达式，隐藏自定义字段
                dialogView.findViewById<View>(R.id.row_pattern).visibility = View.VISIBLE
                dialogView.findViewById<View>(R.id.row_code_prefix_suffix).visibility = View.GONE
                dialogView.findViewById<View>(R.id.row_station_prefix_suffix).visibility = View.GONE
                dialogView.findViewById<View>(R.id.row_address_prefix_suffix).visibility = View.GONE
            }
            "custom" -> {
                // 自定义规则：隐藏正则表达式，显示自定义字段
                dialogView.findViewById<View>(R.id.row_pattern).visibility = View.GONE
                dialogView.findViewById<View>(R.id.row_code_prefix_suffix).visibility = View.VISIBLE
                dialogView.findViewById<View>(R.id.row_station_prefix_suffix).visibility = View.VISIBLE
                dialogView.findViewById<View>(R.id.row_address_prefix_suffix).visibility = View.VISIBLE
            }
            else -> {
                // 其他类型：显示所有字段
                dialogView.findViewById<View>(R.id.row_pattern).visibility = View.VISIBLE
                dialogView.findViewById<View>(R.id.row_code_prefix_suffix).visibility = View.VISIBLE
                dialogView.findViewById<View>(R.id.row_station_prefix_suffix).visibility = View.VISIBLE
                dialogView.findViewById<View>(R.id.row_address_prefix_suffix).visibility = View.VISIBLE
            }
        }
        
        // 如果描述为空，则隐藏描述字段
        if (rule.description.isNullOrEmpty()) {
            dialogView.findViewById<View>(R.id.row_description).visibility = View.GONE
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<Button>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
        
        // 设置对话框宽度和最大高度
        val window = dialog.window
        window?.let {
            val metrics = resources.displayMetrics
            val width = (metrics.widthPixels * 0.9).toInt() // 设置为屏幕宽度的90%
            val height = (metrics.heightPixels * 0.8).toInt() // 设置为屏幕高度的80%
            it.setLayout(width, height)
            it.setGravity(Gravity.CENTER)
            
            // 增加圆角和背景透明度
            it.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }
}

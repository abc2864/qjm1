package com.qujianma.app

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class KeywordsManagerActivity : AppCompatActivity() {
    
    private lateinit var keywordsRecyclerView: RecyclerView
    private lateinit var addKeywordButton: Button
    private lateinit var keywordsAdapter: KeywordsAdapter
    
    private val keywords = mutableListOf<Map<String, Any>>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_manager) // 可以复用规则管理的布局
        
        // 设置标题
        setTitle("关键词管理")
        
        initViews()
        setupRecyclerView()
        loadSavedKeywords()
    }
    
    private fun initViews() {
        keywordsRecyclerView = findViewById(R.id.rules_recycler_view)
        addKeywordButton = findViewById(R.id.add_rule_button)
        addKeywordButton.text = "添加关键词"
        
        addKeywordButton.setOnClickListener {
            showAddKeywordDialog()
        }
    }
    
    private fun setupRecyclerView() {
        keywordsRecyclerView.layoutManager = LinearLayoutManager(this)
        keywordsAdapter = KeywordsAdapter(keywords) { keyword, action, position ->
            when (action) {
                "edit" -> editKeyword(keyword, position)
                "delete" -> deleteKeyword(keyword, position)
                else -> {}
            }
        }
        keywordsRecyclerView.adapter = keywordsAdapter
    }
    
    private fun showAddKeywordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rule_settings, null)
        
        // 隐藏不需要的视图
        dialogView.findViewById<View>(R.id.regex_layout).visibility = View.GONE
        dialogView.findViewById<View>(R.id.custom_layout).visibility = View.GONE
        
        // 显示关键词输入字段
        val keywordInput = dialogView.findViewById<EditText>(R.id.rule_name)
        keywordInput.hint = "输入关键词"
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("添加关键词")
            .setPositiveButton("保存") { _, _ ->
                val keywordText = keywordInput.text.toString().trim()
                if (keywordText.isNotEmpty()) {
                    addKeyword(keywordText)
                } else {
                    Toast.makeText(this, "请输入关键词", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .create()
            
        dialog.show()
    }
    
    private fun addKeyword(keywordText: String) {
        // 检查是否已存在相同的关键词
        val exists = keywords.any { 
            it["keyword"] == keywordText 
        }
        
        if (exists) {
            Toast.makeText(this, "关键词已存在", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 创建新的关键词对象
        val newKeyword = mapOf(
            "id" to System.currentTimeMillis().toString(),
            "keyword" to keywordText,
            "type" to "content",
            "description" to "",
            "is_active" to true
        )
        
        keywords.add(newKeyword)
        saveKeywordsToPreferences()
        keywordsAdapter.notifyDataSetChanged()
        Toast.makeText(this, "关键词已添加", Toast.LENGTH_SHORT).show()
    }
    
    private fun editKeyword(keyword: Map<String, Any>, position: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rule_settings, null)
        
        // 隐藏不需要的视图
        dialogView.findViewById<View>(R.id.regex_layout).visibility = View.GONE
        dialogView.findViewById<View>(R.id.custom_layout).visibility = View.GONE
        
        // 显示关键词输入字段
        val keywordInput = dialogView.findViewById<EditText>(R.id.rule_name)
        keywordInput.hint = "输入关键词"
        keywordInput.setText(keyword["keyword"].toString())
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("编辑关键词")
            .setPositiveButton("保存") { _, _ ->
                val keywordText = keywordInput.text.toString().trim()
                if (keywordText.isNotEmpty()) {
                    updateKeyword(position, keywordText)
                } else {
                    Toast.makeText(this, "请输入关键词", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .create()
            
        dialog.show()
    }
    
    private fun updateKeyword(position: Int, keywordText: String) {
        // 检查是否已存在相同的关键词（排除当前编辑的关键词）
        val exists = keywords.any { 
            it["keyword"] == keywordText && keywords.indexOf(it) != position
        }
        
        if (exists) {
            Toast.makeText(this, "关键词已存在", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 更新关键词
        val updatedKeyword = keywords[position].toMutableMap()
        updatedKeyword["keyword"] = keywordText
        
        keywords[position] = updatedKeyword
        saveKeywordsToPreferences()
        keywordsAdapter.notifyDataSetChanged()
        Toast.makeText(this, "关键词已更新", Toast.LENGTH_SHORT).show()
    }
    
    private fun deleteKeyword(keyword: Map<String, Any>, position: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirm, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // 设置对话框文本
        dialogView.findViewById<android.widget.TextView>(R.id.dialog_title).text = "确认删除"
        dialogView.findViewById<android.widget.TextView>(R.id.dialog_message).text = "确定要删除这个关键词吗？"

        // 设置按钮点击事件
        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btn_delete).setOnClickListener {
            keywords.removeAt(position)
            saveKeywordsToPreferences()
            keywordsAdapter.notifyDataSetChanged()
            Toast.makeText(this, "关键词已删除", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
    
    private fun loadSavedKeywords() {
        // 从SharedPreferences加载已保存的关键词
        val prefs = getSharedPreferences("custom_data", Context.MODE_PRIVATE)
        val keywordsString = prefs.getString("custom_keywords", "")

        if (!keywordsString.isNullOrEmpty()) {
            keywords.clear()
            try {
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                val savedKeywords: List<Map<String, Any>> = Gson().fromJson(keywordsString, type)
                keywords.addAll(savedKeywords)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        keywordsAdapter.notifyDataSetChanged()
    }
    
    private fun saveKeywordsToPreferences() {
        val prefs = getSharedPreferences("custom_data", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val keywordsJson = Gson().toJson(keywords)
        editor.putString("custom_keywords", keywordsJson)
        editor.apply()
    }
    
    override fun onBackPressed() {
        // 确保正确返回到设置界面而不是主页
        super.onBackPressed()
    }
}
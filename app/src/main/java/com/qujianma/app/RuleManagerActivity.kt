package com.qujianma.app

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RuleManagerActivity : AppCompatActivity() {

    private lateinit var rulesRecyclerView: RecyclerView
    private lateinit var addRuleButton: Button
    private lateinit var rulesAdapter: com.qujianma.app.RulesAdapter

    private val rules = mutableListOf<Rule>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_manager)

        initViews()
        setupRecyclerView()
        loadSavedRules()
    }

    private fun initViews() {
        rulesRecyclerView = findViewById(R.id.rules_recycler_view)
        addRuleButton = findViewById(R.id.add_rule_button)

        addRuleButton.setOnClickListener {
            showRuleSettingsDialog()
        }
    }

    private fun setupRecyclerView() {
        rulesRecyclerView.layoutManager = LinearLayoutManager(this)
        rulesAdapter = com.qujianma.app.RulesAdapter(rules) { rule, action, position ->
            when (action) {
                "edit" -> editRule(rule)
                "delete" -> deleteRule(rule)
                "toggle" -> toggleRule(rule)
                else -> {}
            }
        }
        rulesRecyclerView.adapter = rulesAdapter
    }

    private fun showRuleSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rule_settings, null)
        
        val testSmsContent = dialogView.findViewById<EditText>(R.id.test_sms_content)
        val parsingResult = dialogView.findViewById<TextView>(R.id.parsing_result)
        
        // 规则类型选择
        val ruleTypeGroup = dialogView.findViewById<RadioGroup>(R.id.rule_type_group)
        val regexLayout = dialogView.findViewById<LinearLayout>(R.id.regex_layout)
        val customLayout = dialogView.findViewById<LinearLayout>(R.id.custom_layout)
        val patternEditText = dialogView.findViewById<EditText>(R.id.pattern)
        
        // 规则类型切换监听
        ruleTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.regex_radio -> {
                    regexLayout.visibility = View.VISIBLE
                    customLayout.visibility = View.GONE
                }
                R.id.custom_radio -> {
                    regexLayout.visibility = View.GONE
                    customLayout.visibility = View.VISIBLE
                }
            }
        }

        // 提前获取 EditText 引用
        val tagPrefixEditText = dialogView.findViewById<EditText>(R.id.tag_prefix)
        val tagSuffixEditText = dialogView.findViewById<EditText>(R.id.tag_suffix)
        val codePrefixEditText = dialogView.findViewById<EditText>(R.id.code_prefix)
        val codeSuffixEditText = dialogView.findViewById<EditText>(R.id.code_suffix)
        val addressPrefixEditText = dialogView.findViewById<EditText>(R.id.address_prefix)
        val addressSuffixEditText = dialogView.findViewById<EditText>(R.id.address_suffix)
        val stationPrefixEditText = dialogView.findViewById<EditText>(R.id.station_prefix)
        val stationSuffixEditText = dialogView.findViewById<EditText>(R.id.station_suffix)

        // 添加文本监听器实现实时解析
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateDialogParsingResult(dialogView, testSmsContent, parsingResult)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 空实现
            }
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 空实现
            }
        }

        tagPrefixEditText.addTextChangedListener(watcher)
        tagSuffixEditText.addTextChangedListener(watcher)
        codePrefixEditText.addTextChangedListener(watcher)
        codeSuffixEditText.addTextChangedListener(watcher)
        addressPrefixEditText.addTextChangedListener(watcher)
        addressSuffixEditText.addTextChangedListener(watcher)
        stationPrefixEditText.addTextChangedListener(watcher)
        stationSuffixEditText.addTextChangedListener(watcher)
        patternEditText.addTextChangedListener(watcher)
        testSmsContent.addTextChangedListener(watcher)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("自定义规则设置")
            .setPositiveButton("保存并使用") { _, _ ->
                saveAndUseRule(dialogView)
            }
            .setNegativeButton("取消") { _, _ -> }
            .setNeutralButton("重置") { _, _ -> }
            .create()
            
        dialog.show()
        
        // 重写重置按钮的点击事件
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            // 重置所有输入框
            dialogView.findViewById<EditText>(R.id.rule_name).setText("")
            dialogView.findViewById<RadioButton>(R.id.custom_radio).isChecked = true
            dialogView.findViewById<EditText>(R.id.tag_prefix).setText("")
            dialogView.findViewById<EditText>(R.id.tag_suffix).setText("")
            dialogView.findViewById<EditText>(R.id.code_prefix).setText("")
            dialogView.findViewById<EditText>(R.id.code_suffix).setText("")
            dialogView.findViewById<EditText>(R.id.address_prefix).setText("")
            dialogView.findViewById<EditText>(R.id.address_suffix).setText("")
            dialogView.findViewById<EditText>(R.id.station_prefix).setText("")
            dialogView.findViewById<EditText>(R.id.station_suffix).setText("")
            dialogView.findViewById<EditText>(R.id.pattern).setText("")
            dialogView.findViewById<EditText>(R.id.test_sms_content).setText("")
            
            // 重置解析结果显示
            dialogView.findViewById<TextView>(R.id.parsing_result).text = "暂无解析结果"
            
            // 显示提示信息
            Toast.makeText(this, "已重置所有输入", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateDialogParsingResult(dialogView: View, testSmsContent: EditText, parsingResult: TextView) {
        val smsContent = testSmsContent.text.toString().trim()
        if (smsContent.isEmpty()) {
            parsingResult.text = "请输入测试短信内容"
            return
        }
        
        val tagPrefix = dialogView.findViewById<EditText>(R.id.tag_prefix).text.toString().trim()
        val tagSuffix = dialogView.findViewById<EditText>(R.id.tag_suffix).text.toString().trim()
        val codePrefix = dialogView.findViewById<EditText>(R.id.code_prefix).text.toString().trim()
        val codeSuffix = dialogView.findViewById<EditText>(R.id.code_suffix).text.toString().trim()
        val addressPrefix = dialogView.findViewById<EditText>(R.id.address_prefix).text.toString().trim()
        val addressSuffix = dialogView.findViewById<EditText>(R.id.address_suffix).text.toString().trim()
        val stationPrefix = dialogView.findViewById<EditText>(R.id.station_prefix).text.toString().trim()
        val stationSuffix = dialogView.findViewById<EditText>(R.id.station_suffix).text.toString().trim()
        
        if ((tagPrefix.isNotEmpty() || tagSuffix.isNotEmpty()) ||
            (codePrefix.isNotEmpty() || codeSuffix.isNotEmpty()) ||
            (addressPrefix.isNotEmpty() || addressSuffix.isNotEmpty()) ||
            (stationPrefix.isNotEmpty() || stationSuffix.isNotEmpty())) {
            
            try {
                var result = "解析结果:\n"
                var hasMatch = false
                
                // 测试标签匹配
                if (tagPrefix.isNotEmpty() || tagSuffix.isNotEmpty()) {
                    val tagPattern = "${java.util.regex.Pattern.quote(tagPrefix)}(.*?)${java.util.regex.Pattern.quote(tagSuffix)}"
                    val tagMatcher = java.util.regex.Pattern.compile(tagPattern).matcher(smsContent)
                    if (tagMatcher.find()) {
                        result += "标签: ${tagMatcher.group(1)?.trim()}\n"
                        hasMatch = true
                    } else {
                        result += "标签: 未匹配\n"
                    }
                }
                
                // 测试取件码匹配
                if (codePrefix.isNotEmpty() || codeSuffix.isNotEmpty()) {
                    val codePattern = "${java.util.regex.Pattern.quote(codePrefix)}(.*?)${java.util.regex.Pattern.quote(codeSuffix)}"
                    val codeMatcher = java.util.regex.Pattern.compile(codePattern).matcher(smsContent)
                    if (codeMatcher.find()) {
                        result += "取件码: ${codeMatcher.group(1)?.trim()}\n"
                        hasMatch = true
                    } else {
                        result += "取件码: 未匹配\n"
                    }
                }
                
                // 测试地址匹配
                if (addressPrefix.isNotEmpty() || addressSuffix.isNotEmpty()) {
                    val addressPattern = "${java.util.regex.Pattern.quote(addressPrefix)}(.*?)${java.util.regex.Pattern.quote(addressSuffix)}"
                    val addressMatcher = java.util.regex.Pattern.compile(addressPattern).matcher(smsContent)
                    if (addressMatcher.find()) {
                        result += "地址: ${addressMatcher.group(1)?.trim()}\n"
                        hasMatch = true
                    } else {
                        result += "地址: 未匹配\n"
                    }
                }
                
                // 测试驿站匹配
                if (stationPrefix.isNotEmpty() || stationSuffix.isNotEmpty()) {
                    val stationPattern = "${java.util.regex.Pattern.quote(stationPrefix)}(.*?)${java.util.regex.Pattern.quote(stationSuffix)}"
                    val stationMatcher = java.util.regex.Pattern.compile(stationPattern).matcher(smsContent)
                    if (stationMatcher.find()) {
                        result += "驿站: ${stationMatcher.group(1)?.trim()}\n"
                        hasMatch = true
                    } else {
                        result += "驿站: 未匹配\n"
                    }
                }
                
                if (!hasMatch) {
                    result = "未匹配到任何内容，请检查规则设置"
                }
                
                parsingResult.text = result
            } catch (e: Exception) {
                parsingResult.text = "解析出错: ${e.message}"
            }
        } else {
            parsingResult.text = "请输入规则内容进行测试"
        }
    }

    private fun saveAndUseRule(view: View) {
        // 获取输入的规则数据
        val ruleName = view.findViewById<EditText>(R.id.rule_name).text.toString()
        
        // 获取规则类型
        val ruleTypeGroup = view.findViewById<RadioGroup>(R.id.rule_type_group)
        val ruleType = if (ruleTypeGroup.checkedRadioButtonId == R.id.regex_radio) "regex" else "custom"
        
        // 正则规则字段
        val pattern = view.findViewById<EditText>(R.id.pattern).text.toString()
        
        // 自定义前后缀规则字段
        val tagPrefix = view.findViewById<EditText>(R.id.tag_prefix).text.toString()
        val tagSuffix = view.findViewById<EditText>(R.id.tag_suffix).text.toString()
        val codePrefix = view.findViewById<EditText>(R.id.code_prefix).text.toString()
        val codeSuffix = view.findViewById<EditText>(R.id.code_suffix).text.toString()
        val addressPrefix = view.findViewById<EditText>(R.id.address_prefix).text.toString()
        val addressSuffix = view.findViewById<EditText>(R.id.address_suffix).text.toString()
        val stationPrefix = view.findViewById<EditText>(R.id.station_prefix).text.toString()
        val stationSuffix = view.findViewById<EditText>(R.id.station_suffix).text.toString()

        // 创建新的规则对象
        val newRule = Rule(
            name = ruleName,
            ruleType = ruleType,
            pattern = pattern,
            tagPrefix = tagPrefix,
            tagSuffix = tagSuffix,
            codePrefix = codePrefix,
            codeSuffix = codeSuffix,
            addressPrefix = addressPrefix,
            addressSuffix = addressSuffix,
            stationPrefix = stationPrefix,
            stationSuffix = stationSuffix,
            enabled = true
        )

        // 添加到规则集合
        rules.add(newRule)

        // 保存规则到SharedPreferences
        saveRulesToPreferences()

        // 更新RecyclerView
        rulesAdapter.notifyDataSetChanged()

        Toast.makeText(this, "规则已保存", Toast.LENGTH_SHORT).show()
    }

    private fun loadSavedRules() {
        // 从SharedPreferences加载已保存的规则
        val prefs = getSharedPreferences("rules", Context.MODE_PRIVATE)
        val rulesString = prefs.getString("rules_data", "")

        if (!rulesString.isNullOrEmpty()) {
            rules.clear()
            val ruleStrings = rulesString.split("|")
            ruleStrings.forEach { ruleStr ->
                val parts = ruleStr.split("~")
                if (parts.size >= 13) {
                    val rule = Rule(
                        name = parts[0],
                        ruleType = parts[1],
                        pattern = parts[2],
                        tagPrefix = parts[3],
                        tagSuffix = parts[4],
                        codePrefix = parts[7],
                        codeSuffix = parts[8],
                        addressPrefix = parts[9],
                        addressSuffix = parts[10],
                        stationPrefix = parts[11],
                        stationSuffix = parts[12],
                        enabled = parts[13].toBoolean()
                    )
                    rules.add(rule)
                }
            }
        }

        rulesAdapter.notifyDataSetChanged()
    }

    private fun saveRulesToPreferences() {
        val prefs = getSharedPreferences("rules", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val rulesString = rules.joinToString("|") { rule ->
            "${rule.name}~${rule.ruleType}~${rule.pattern}~${rule.tagPrefix}~${rule.tagSuffix}~~~" +
                    "${rule.codePrefix}~${rule.codeSuffix}~${rule.addressPrefix}~${rule.addressSuffix}~${rule.stationPrefix}~${rule.stationSuffix}~${rule.enabled}"
        }

        editor.putString("rules_data", rulesString)
        editor.apply()
    }

    private fun editRule(rule: Rule) {
        showEditRuleDialog(rule)
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
            saveRulesToPreferences()
            rulesAdapter.notifyDataSetChanged()
            Toast.makeText(this, "规则已删除", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun toggleRule(rule: Rule) {
        rule.enabled = !rule.enabled
        saveRulesToPreferences()
        rulesAdapter.notifyDataSetChanged()
        Toast.makeText(this, "规则${if (rule.enabled) "启用" else "禁用"}", Toast.LENGTH_SHORT).show()
    }

    // 显示编辑规则对话框
    private fun showEditRuleDialog(rule: Rule) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rule_settings, null)
        
        val testSmsContent = dialogView.findViewById<EditText>(R.id.test_sms_content)
        val parsingResult = dialogView.findViewById<TextView>(R.id.parsing_result)
        
        // 规则类型选择
        val ruleTypeGroup = dialogView.findViewById<RadioGroup>(R.id.rule_type_group)
        val regexLayout = dialogView.findViewById<LinearLayout>(R.id.regex_layout)
        val customLayout = dialogView.findViewById<LinearLayout>(R.id.custom_layout)
        val patternEditText = dialogView.findViewById<EditText>(R.id.pattern)
        
        // 设置规则类型
        if (rule.ruleType == "regex") {
            dialogView.findViewById<RadioButton>(R.id.regex_radio).isChecked = true
            regexLayout.visibility = View.VISIBLE
            customLayout.visibility = View.GONE
        } else {
            dialogView.findViewById<RadioButton>(R.id.custom_radio).isChecked = true
            regexLayout.visibility = View.GONE
            customLayout.visibility = View.VISIBLE
        }
        
        // 规则类型切换监听
        ruleTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.regex_radio -> {
                    regexLayout.visibility = View.VISIBLE
                    customLayout.visibility = View.GONE
                }
                R.id.custom_radio -> {
                    regexLayout.visibility = View.GONE
                    customLayout.visibility = View.VISIBLE
                }
            }
        }

        // 设置现有规则
        dialogView.findViewById<EditText>(R.id.rule_name).setText(rule.name)
        dialogView.findViewById<EditText>(R.id.pattern).setText(rule.pattern)
        dialogView.findViewById<EditText>(R.id.tag_prefix).setText(rule.tagPrefix)
        dialogView.findViewById<EditText>(R.id.tag_suffix).setText(rule.tagSuffix)
        dialogView.findViewById<EditText>(R.id.code_prefix).setText(rule.codePrefix)
        dialogView.findViewById<EditText>(R.id.code_suffix).setText(rule.codeSuffix)
        dialogView.findViewById<EditText>(R.id.address_prefix).setText(rule.addressPrefix)
        dialogView.findViewById<EditText>(R.id.address_suffix).setText(rule.addressSuffix)
        dialogView.findViewById<EditText>(R.id.station_prefix).setText(rule.stationPrefix)
        dialogView.findViewById<EditText>(R.id.station_suffix).setText(rule.stationSuffix)

        // 获取EditText引用
        val tagPrefixEditText = dialogView.findViewById<EditText>(R.id.tag_prefix)
        val tagSuffixEditText = dialogView.findViewById<EditText>(R.id.tag_suffix)
        val codePrefixEditText = dialogView.findViewById<EditText>(R.id.code_prefix)
        val codeSuffixEditText = dialogView.findViewById<EditText>(R.id.code_suffix)
        val addressPrefixEditText = dialogView.findViewById<EditText>(R.id.address_prefix)
        val addressSuffixEditText = dialogView.findViewById<EditText>(R.id.address_suffix)
        val stationPrefixEditText = dialogView.findViewById<EditText>(R.id.station_prefix)
        val stationSuffixEditText = dialogView.findViewById<EditText>(R.id.station_suffix)
        val patternEdit = dialogView.findViewById<EditText>(R.id.pattern)
        
        // 添加文本监听器实现实时解析
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateDialogParsingResult(dialogView, testSmsContent, parsingResult)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 空实现
            }
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 空实现
            }
        }

        tagPrefixEditText.addTextChangedListener(watcher)
        tagSuffixEditText.addTextChangedListener(watcher)
        codePrefixEditText.addTextChangedListener(watcher)
        codeSuffixEditText.addTextChangedListener(watcher)
        addressPrefixEditText.addTextChangedListener(watcher)
        addressSuffixEditText.addTextChangedListener(watcher)
        stationPrefixEditText.addTextChangedListener(watcher)
        stationSuffixEditText.addTextChangedListener(watcher)
        patternEdit.addTextChangedListener(watcher)
        testSmsContent.addTextChangedListener(watcher)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("编辑规则")
            .setPositiveButton("保存") { _, _ ->
                // 更新规则
                rule.name = dialogView.findViewById<EditText>(R.id.rule_name).text.toString()
                rule.ruleType = if (ruleTypeGroup.checkedRadioButtonId == R.id.regex_radio) "regex" else "custom"
                rule.pattern = dialogView.findViewById<EditText>(R.id.pattern).text.toString()
                rule.tagPrefix = dialogView.findViewById<EditText>(R.id.tag_prefix).text.toString()
                rule.tagSuffix = dialogView.findViewById<EditText>(R.id.tag_suffix).text.toString()
                rule.codePrefix = dialogView.findViewById<EditText>(R.id.code_prefix).text.toString()
                rule.codeSuffix = dialogView.findViewById<EditText>(R.id.code_suffix).text.toString()
                rule.addressPrefix = dialogView.findViewById<EditText>(R.id.address_prefix).text.toString()
                rule.addressSuffix = dialogView.findViewById<EditText>(R.id.address_suffix).text.toString()
                rule.stationPrefix = dialogView.findViewById<EditText>(R.id.station_prefix).text.toString()
                rule.stationSuffix = dialogView.findViewById<EditText>(R.id.station_suffix).text.toString()

                saveRulesToPreferences()
                rulesAdapter.notifyDataSetChanged()
                Toast.makeText(this, "规则已更新", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消") { _, _ -> }
            .setNeutralButton("重置") { _, _ -> }
            .create()
            
        dialog.show()
        
        // 重写重置按钮的点击事件
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            // 重置所有输入框
            dialogView.findViewById<EditText>(R.id.rule_name).setText("")
            dialogView.findViewById<RadioButton>(R.id.custom_radio).isChecked = true
            dialogView.findViewById<EditText>(R.id.tag_prefix).setText("")
            dialogView.findViewById<EditText>(R.id.tag_suffix).setText("")
            dialogView.findViewById<EditText>(R.id.code_prefix).setText("")
            dialogView.findViewById<EditText>(R.id.code_suffix).setText("")
            dialogView.findViewById<EditText>(R.id.address_prefix).setText("")
            dialogView.findViewById<EditText>(R.id.address_suffix).setText("")
            dialogView.findViewById<EditText>(R.id.station_prefix).setText("")
            dialogView.findViewById<EditText>(R.id.station_suffix).setText("")
            dialogView.findViewById<EditText>(R.id.pattern).setText("")
            dialogView.findViewById<EditText>(R.id.test_sms_content).setText("")
            
            // 重置解析结果显示
            dialogView.findViewById<TextView>(R.id.parsing_result).text = "暂无解析结果"
            
            // 显示提示信息
            Toast.makeText(this, "已重置所有输入", Toast.LENGTH_SHORT).show()
        }
    }

    // 获取启用的规则列表
    fun getEnabledRules(): List<Rule> {
        return rules.filter { it.enabled }
    }
    
    companion object {
        fun loadEnabledRules(context: Context): List<Rule> {
            val prefs = context.getSharedPreferences("rules", Context.MODE_PRIVATE)
            val rulesString = prefs.getString("rules_data", "")
            val rules = mutableListOf<Rule>()
            
            if (!rulesString.isNullOrEmpty()) {
                val ruleStrings = rulesString.split("|")
                ruleStrings.forEach { ruleStr ->
                    val parts = ruleStr.split("~")
                    if (parts.size >= 9) {
                        val rule = Rule(
                            tagPrefix = parts[0],
                            tagSuffix = parts[1],
                            codePrefix = parts[4],
                            codeSuffix = parts[5],
                            addressPrefix = parts[6],
                            addressSuffix = parts[7],
                            enabled = parts[8].toBoolean()
                        )
                        if (rule.enabled) {
                            rules.add(rule)
                        }
                    }
                }
            }
            
            return rules
        }
    }
}
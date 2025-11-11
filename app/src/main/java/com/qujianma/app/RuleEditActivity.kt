package com.qujianma.app

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar

class RuleEditActivity : AppCompatActivity() {
    
    private lateinit var ruleNameInput: EditText
    private lateinit var tagPrefixInput: EditText
    private lateinit var tagSuffixInput: EditText
    private lateinit var codePrefixInput: EditText
    private lateinit var codeSuffixInput: EditText
    private lateinit var addressPrefixInput: EditText
    private lateinit var addressSuffixInput: EditText
    private lateinit var enabledSwitch: SwitchCompat
    private lateinit var saveButton: Button
    private lateinit var resetButton: Button
    private lateinit var testSmsContent: EditText
    private lateinit var parsingResult: TextView
    
    private var ruleToEdit: Rule? = null
    private var rulePosition: Int = -1
    
    companion object {
        const val EXTRA_RULE = "rule"
        const val EXTRA_RULE_POSITION = "rule_position"
        const val RESULT_CODE = 1001
        const val EXTRA_RULE_RESULT = "rule_result"
        const val EXTRA_DELETED_POSITION = "deleted_position"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_edit)
        
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        initViews()
        handleIntentData()
        setListeners()
    }
    
    private fun initViews() {
        ruleNameInput = findViewById(R.id.rule_name)
        tagPrefixInput = findViewById(R.id.tag_prefix)
        tagPrefixInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateParsingResult()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        tagSuffixInput = findViewById(R.id.tag_suffix)
        tagSuffixInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateParsingResult()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        codePrefixInput = findViewById(R.id.code_prefix)
        codePrefixInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateParsingResult()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        codeSuffixInput = findViewById(R.id.code_suffix)
        codeSuffixInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateParsingResult()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        addressPrefixInput = findViewById(R.id.address_prefix)
        addressPrefixInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateParsingResult()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        addressSuffixInput = findViewById(R.id.address_suffix)
        addressSuffixInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateParsingResult()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        enabledSwitch = findViewById(R.id.rule_enabled_switch)
        saveButton = findViewById(R.id.save_rule_button)
        resetButton = findViewById(R.id.reset_rule_button)
        testSmsContent = findViewById(R.id.test_sms_content)
        testSmsContent.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateParsingResult()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        parsingResult = findViewById(R.id.parsing_result)
        
        supportActionBar?.title = "添加规则"
    }
    
    private fun handleIntentData() {
        ruleToEdit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_RULE, Rule::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_RULE) as? Rule
        }
        
        rulePosition = intent.getIntExtra(EXTRA_RULE_POSITION, -1)
        
        if (ruleToEdit != null) {
            supportActionBar?.title = "编辑规则"
            populateRuleData(ruleToEdit!!)
        }
    }
    
    private fun populateRuleData(rule: Rule) {
        ruleNameInput.setText(rule.name)
        tagPrefixInput.setText(rule.tagPrefix)
        tagSuffixInput.setText(rule.tagSuffix)
        codePrefixInput.setText(rule.codePrefix)
        codeSuffixInput.setText(rule.codeSuffix)
        addressPrefixInput.setText(rule.addressPrefix)
        addressSuffixInput.setText(rule.addressSuffix)
        enabledSwitch.isChecked = rule.enabled
    }
    
    private fun setListeners() {
        saveButton.setOnClickListener {
            saveRule()
        }
        
        resetButton.setOnClickListener {
            resetRule()
        }
    }
    
    private fun saveRule() {
        val ruleName = ruleNameInput.text.toString().trim()
        val tagPrefix = tagPrefixInput.text.toString().trim()
        val tagSuffix = tagSuffixInput.text.toString().trim()
        val codePrefix = codePrefixInput.text.toString().trim()
        val codeSuffix = codeSuffixInput.text.toString().trim()
        val addressPrefix = addressPrefixInput.text.toString().trim()
        val addressSuffix = addressSuffixInput.text.toString().trim()
        val isEnabled = enabledSwitch.isChecked

        if (tagPrefix.isEmpty()) {
            Toast.makeText(this, "请输入规则名称", Toast.LENGTH_SHORT).show()
            return
        }

        if (tagSuffix.isEmpty()) {
            Toast.makeText(this, "请输入短信内容匹配模式", Toast.LENGTH_SHORT).show()
            return
        }

        if (codePrefix.isEmpty()) {
            Toast.makeText(this, "请输入取件码匹配模式", Toast.LENGTH_SHORT).show()
            return
        }

        val rule = ruleToEdit ?: Rule()
        rule.name = ruleName
        rule.tagPrefix = tagPrefix
        rule.tagSuffix = tagSuffix
        rule.codePrefix = codePrefix
        rule.codeSuffix = codeSuffix
        rule.addressPrefix = addressPrefix
        rule.addressSuffix = addressSuffix
        rule.enabled = isEnabled
        
        val resultIntent = intent
        resultIntent.putExtra(EXTRA_RULE_RESULT, rule)
        if (rulePosition >= 0) {
            resultIntent.putExtra(EXTRA_RULE_POSITION, rulePosition)
        }
        setResult(RESULT_CODE, resultIntent)
        finish()
    }
    
    private fun resetRule() {
        ruleNameInput.setText("")
        tagPrefixInput.setText("")
        tagSuffixInput.setText("")
        codePrefixInput.setText("")
        codeSuffixInput.setText("")
        addressPrefixInput.setText("")
        addressSuffixInput.setText("")
        enabledSwitch.isChecked = true
        testSmsContent.setText("")
        parsingResult.text = "暂无解析结果"
        Toast.makeText(this, "已重置所有输入", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateParsingResult() {
        val tagPrefix = tagPrefixInput.text.toString().trim()
        val tagSuffix = tagSuffixInput.text.toString().trim()
        val codePrefix = codePrefixInput.text.toString().trim()
        val codeSuffix = codeSuffixInput.text.toString().trim()
        val addressPrefix = addressPrefixInput.text.toString().trim()
        val addressSuffix = addressSuffixInput.text.toString().trim()
        val testContent = testSmsContent.text.toString().trim()
        
        if (testContent.isEmpty()) {
            parsingResult.text = "请输入测试短信内容"
            return
        }
        
        if ((tagPrefix.isNotEmpty() || tagSuffix.isNotEmpty()) ||
            (codePrefix.isNotEmpty() || codeSuffix.isNotEmpty()) ||
            (addressPrefix.isNotEmpty() || addressSuffix.isNotEmpty())) {
            
            try {
                var result = "解析结果:\n"
                var hasMatch = false
                
                // 测试标签匹配
                if (tagPrefix.isNotEmpty() || tagSuffix.isNotEmpty()) {
                    val tagPattern = "${java.util.regex.Pattern.quote(tagPrefix)}(.*?)${java.util.regex.Pattern.quote(tagSuffix)}"
                    val tagMatcher = java.util.regex.Pattern.compile(tagPattern).matcher(testContent)
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
                    val codeMatcher = java.util.regex.Pattern.compile(codePattern).matcher(testContent)
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
                    val addressMatcher = java.util.regex.Pattern.compile(addressPattern).matcher(testContent)
                    if (addressMatcher.find()) {
                        result += "地址: ${addressMatcher.group(1)?.trim()}\n"
                        hasMatch = true
                    } else {
                        result += "地址: 未匹配\n"
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
}
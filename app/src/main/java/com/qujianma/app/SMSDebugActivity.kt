package com.qujianma.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.qujianma.app.ExpressParser
import java.util.regex.Pattern

class SMSDebugActivity : AppCompatActivity() {
    
    private lateinit var smsInput: EditText
    private lateinit var parseButton: Button
    private lateinit var resultText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_debug)
        
        initViews()
    }
    
    private fun initViews() {
        smsInput = findViewById(R.id.et_sms_content)
        parseButton = findViewById(R.id.btn_parse)
        resultText = findViewById(R.id.tv_result)
        
        parseButton.setOnClickListener {
            val smsContent = smsInput.text.toString().trim()
            if (smsContent.isEmpty()) {
                Toast.makeText(this, "请输入短信内容", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            parseSMS(smsContent)
        }
    }
    
    private fun parseSMS(smsContent: String) {
        try {
            // 使用ParcelInfoExtractor直接解析
            val codePattern = Pattern.compile("(?<!\\d)([A-Z0-9]{1,2}[-]?){1,4}[A-Z0-9]{1,12}(?!\\d)")
            val codeMatches = codePattern.matcher(smsContent).findAll().map { it.group() }.toList()
            
            // 使用ExpressParser解析
            val expressInfo = ExpressParser.parse(smsContent, System.currentTimeMillis())
            
            val result = buildString {
                append("=== 原始短信 ===\n")
                append("$smsContent\n\n")
                
                append("=== 提取的可能取件码 ===\n")
                append("找到的所有可能取件码: $codeMatches\n\n")
                
                append("=== ExpressParser 结果 ===\n")
                if (expressInfo != null) {
                    append("取件码: ${expressInfo.pickupCode}\n")
                    append("所有取件码: ${expressInfo.pickupCodes}\n")
                    append("驿站名称: ${expressInfo.stationName}\n")
                    append("地址: ${expressInfo.address}\n")
                } else {
                    append("未解析到快递信息\n")
                }
            }
            
            resultText.text = result
        } catch (e: Exception) {
            resultText.text = "解析过程中发生错误: ${e.message}"
        }
    }
    
    // 扩展函数，用于获取所有匹配项
    private fun java.util.regex.Matcher.findAll(): List<java.util.regex.MatchResult> {
        val matches = mutableListOf<java.util.regex.MatchResult>()
        while (this.find()) {
            matches.add(this.toMatchResult())
        }
        return matches
    }
}
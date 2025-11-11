package com.qujianma.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.IOException

class NetworkDiagnosisActivity : AppCompatActivity() {
    private lateinit var networkStatusText: TextView
    private lateinit var testConnectionButton: Button
    private lateinit var resultText: TextView
    
    private val client = OkHttpClient()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_diagnosis)
        
        setupViews()
        updateNetworkStatus()
    }
    
    private fun setupViews() {
        networkStatusText = findViewById(R.id.network_status_text)
        testConnectionButton = findViewById(R.id.test_connection_button)
        resultText = findViewById(R.id.result_text)
        
        testConnectionButton.setOnClickListener {
            testServerConnection()
        }
    }
    
    private fun updateNetworkStatus() {
        val isNetworkAvailable = NetworkUtils.isNetworkAvailable(this)
        val networkType = NetworkUtils.getNetworkType(this)
        
        networkStatusText.text = "网络状态: ${if (isNetworkAvailable) "可用" else "不可用"}\n网络类型: $networkType"
    }
    
    private fun testServerConnection() {
        resultText.text = "正在测试连接..."
        
        val request = Request.Builder()
            .url("http://8.137.57.54/api.php?action=get_rules")
            .addHeader("X-API-Key", DataManager.DEFAULT_API_KEY)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    resultText.text = "连接失败: ${e.message}"
                    Toast.makeText(this@NetworkDiagnosisActivity, "连接测试失败", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    resultText.text = "连接成功\n响应码: ${response.code}\n响应消息: ${response.message}"
                    Toast.makeText(this@NetworkDiagnosisActivity, "连接测试成功", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
package com.qujianma.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SmsReceiver"
        
        // 测试服务号短信处理功能
        fun testServiceNumberHandling() {
            Log.d(TAG, "开始测试服务号短信处理功能")
            
            // 测试不同类型的服务号短信
            val testCases = listOf(
                // 服务号短信
                "10690123456789: 【菜鸟驿站】您的快递包裹已到达菜鸟驿站，取件码：A-1234，请及时领取。",
                "10690987654321: 【妈妈驿站】您的圆通快递已到店，取件码：B-5678，请凭此码取件。",
                "10654321098765: 【丰巢快递柜】您的顺丰快递已存入丰巢快递柜，取件码：123456，请及时取件。",
                
                // 普通短信（应该保持不变）
                "【菜鸟驿站】您的快递包裹已到达菜鸟驿站，取件码：A-1234，请及时领取。",
                "【妈妈驿站】您的圆通快递已到店，取件码：B-5678，请凭此码取件。"
            )
            
            for ((index, sms) in testCases.withIndex()) {
                Log.d(TAG, "测试用例 ${index + 1}: $sms")
                
                // 模拟SmsReceiver中的处理逻辑
                val (senderNumber, messageBody) = extractSenderAndBody(sms)
                val processedBody = processServiceNumberSMS(senderNumber, messageBody)
                
                Log.d(TAG, "处理后内容: $processedBody")
                
                // 测试解析
                val expressInfo = ExpressParser.parse(processedBody, System.currentTimeMillis())
                if (expressInfo != null) {
                    Log.d(TAG, "解析成功: 取件码=${expressInfo.pickupCode}, 驿站=${expressInfo.stationName}")
                } else {
                    Log.d(TAG, "解析失败: 无法识别为快递短信")
                }
                Log.d(TAG, "------------------------")
            }
        }
        
        // 模拟从SmsMessage中提取发送方和内容
        private fun extractSenderAndBody(rawSms: String): Pair<String, String> {
            // 简化的模拟实现
            if (rawSms.contains(": ") && rawSms.substring(0, rawSms.indexOf(": ")).all { it.isDigit() || it == '+' }) {
                val parts = rawSms.split(": ", limit = 2)
                return Pair(parts[0], parts[1])
            }
            return Pair("unknown", rawSms)
        }
        
        // 模拟SmsReceiver中的服务号处理逻辑
        private fun processServiceNumberSMS(senderNumber: String, messageBody: String): String {
            // 处理服务号短信格式（服务号: 短信内容）
            // 增加更严格的检查，确保服务号长度合理且以106开头
            if (senderNumber.startsWith("106") && senderNumber.length >= 8 && senderNumber.length <= 20 && 
                messageBody.contains(": ")) {
                val parts = messageBody.split(": ", limit = 2)
                if (parts.size == 2) {
                    return parts[1]
                }
            }
            return messageBody
        }
    }
    
    private val TAG = "SmsReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle: Bundle? = intent.extras
            if (bundle != null) {
                @Suppress("DEPRECATION")
                val pdus = bundle.getParcelableArray("pdus")
                @Suppress("DEPRECATION")
                val format = bundle.getString("format")
                val messages = arrayOfNulls<SmsMessage>(pdus?.size ?: 0)
                
                for (i in pdus?.indices ?: emptyList()) {
                    @Suppress("DEPRECATION")
                    messages[i] = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(pdus?.get(i) as ByteArray, format)
                    } else {
                        SmsMessage.createFromPdu(pdus?.get(i) as ByteArray)
                    }
                    
                    val smsMessage = messages[i]
                    if (smsMessage != null) {
                        val senderNumber = smsMessage.displayOriginatingAddress
                        var messageBody = smsMessage.messageBody
                        val timestamp = smsMessage.timestampMillis
                        
                        // 处理服务号短信格式（服务号: 短信内容）
                        // 增加更严格的检查，确保服务号长度合理且以106开头
                        if (senderNumber.startsWith("106") && senderNumber.length >= 8 && senderNumber.length <= 20 && 
                            messageBody.contains(": ")) {
                            val parts = messageBody.split(": ", limit = 2)
                            if (parts.size == 2) {
                                messageBody = parts[1]
                            }
                        }
                        
                        Log.d(TAG, "收到短信: 来自 $senderNumber 内容: $messageBody")
                        
                        // 解析短信内容，传入context参数以支持下载的规则
                        val expressInfo = ExpressParser.parse(messageBody, timestamp, context)
                        if (expressInfo != null) {
                            // 保存快递信息
                            DataManager.getInstance(context).saveManualExpressInfo(expressInfo)
                        }
                    }
                }
            }
        }
    }
}
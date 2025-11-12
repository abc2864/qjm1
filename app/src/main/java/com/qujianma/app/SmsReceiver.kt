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
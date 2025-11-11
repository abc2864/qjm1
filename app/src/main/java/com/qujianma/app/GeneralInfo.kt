package com.qujianma.app

data class GeneralInfo(
    val title: String = "",            // 信息标题/类型
    val content: String = "",          // 信息内容
    val sender: String = "",           // 发送方
    val smsContent: String = "",       // 原始短信内容
    val timestamp: Long = 0L           // 短信时间戳
)
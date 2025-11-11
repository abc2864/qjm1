package com.qujianma.app

/**
 * 快递信息组，用于将同一天同一驿站的多个取件码组合在一起显示
 */
data class ExpressGroup(
    val stationName: String,           // 驿站名称
    val address: String,               // 地址
    val expressItems: MutableList<ExpressItem> // 该组下的取件码列表
)

/**
 * 快递项目，包含单个取件码的信息
 */
data class ExpressItem(
    val id: String,           // 唯一标识符
    val pickupCode: String,   // 取件码
    val pickupCodes: List<String>, // 所有取件码（支持多取件码）
    val timestamp: Long,      // 时间戳
    val isPickedUp: Boolean,  // 是否已取件
    val note: String,         // 备注
    val smsContent: String    // 原始短信内容
)
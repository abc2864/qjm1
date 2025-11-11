package com.qujianma.app

import java.security.MessageDigest
import java.util.UUID

data class ExpressInfo(
    val id: String = UUID.randomUUID().toString(), // 唯一标识符
    val pickupCode: String = "",       // 主取件码
    val pickupCodes: List<String> = listOf(), // 所有取件码
    val stationName: String = "",      // 驿站名称
    val address: String = "",          // 地址
    val smsContent: String = "",       // 原始短信内容
    val timestamp: Long = 0L,          // 短信时间戳
    val isPickedUp: Boolean = false,   // 是否已取件，默认为false（未取）
    val note: String = ""              // 备注信息
) {
    companion object {
        /**
         * 根据取件码、驿站名称、地址和时间戳生成稳定的ID
         * 加入时间戳可以确保即使是相同信息但在不同时间收到的快递项也能获得不同的ID
         */
        fun generateStableId(pickupCode: String, stationName: String, address: String, timestamp: Long): String {
            // 为了防止时间戳的微小差异导致ID不同，我们将时间戳四舍五入到分钟级别
            val roundedTimestamp = timestamp / 60000 * 60000 // 四舍五入到分钟级
            val input = "$pickupCode|$stationName|$address|$roundedTimestamp"
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
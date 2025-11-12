package com.qujianma.app

import android.content.Context
import java.util.regex.Pattern

/**
 * 高级快递信息解析器，用于处理更复杂的短信格式
 */
object AdvancedExpressParser {
    
    /**
     * 高级解析方法，尝试多种策略解析短信
     */
    fun parse(smsContent: String, timestamp: Long, context: Context): ExpressInfo? {
        // 1. 尝试使用现有的解析器
        var result = ExpressParser.parse(smsContent, timestamp, context)
        if (result != null) {
            return result
        }
        
        // 2. 尝试使用特殊格式解析器
        result = parseSpecialFormats(smsContent, timestamp)
        if (result != null) {
            return result
        }
        
        // 3. 尝试使用通用模式解析器
        result = parseWithUniversalPatterns(smsContent, timestamp)
        if (result != null) {
            return result
        }
        
        // 4. 尝试使用关键词增强解析器
        result = parseWithKeywordEnhancement(smsContent, timestamp)
        if (result != null) {
            return result
        }
        
        return null
    }
    
    /**
     * 解析特殊格式的短信
     */
    private fun parseSpecialFormats(smsContent: String, timestamp: Long): ExpressInfo? {
        // 处理包含"取货码"的短信
        if (smsContent.contains("取货码") || smsContent.contains("取件码")) {
            val codePattern = Regex("(取货码|取件码)[:：]?\\s*([A-Za-z0-9\\-]+)")
            val codeMatcher = codePattern.find(smsContent)
            if (codeMatcher != null) {
                val pickupCode = codeMatcher.groupValues[2]
                if (isValidPickupCode(pickupCode, smsContent)) {
                    // 提取驿站名称
                    var stationName = extractStationName(smsContent)
                    if (stationName.isNullOrEmpty()) {
                        stationName = "未知驿站"
                    }
                    
                    // 提取地址
                    var address = extractAddress(smsContent)
                    if (address.isNullOrEmpty()) {
                        address = "地址未知"
                    }
                    
                    return ExpressInfo(
                        id = ExpressInfo.generateStableId(pickupCode, stationName, address, timestamp),
                        pickupCode = pickupCode,
                        pickupCodes = listOf(pickupCode),
                        stationName = stationName,
                        address = address,
                        smsContent = smsContent,
                        timestamp = timestamp
                    )
                }
            }
        }
        
        // 处理包含"凭xxx到"的短信
        if (smsContent.contains("凭") && smsContent.contains("到")) {
            val pattern = Regex("凭([A-Za-z0-9\\-]+)到")
            val matcher = pattern.find(smsContent)
            if (matcher != null) {
                val pickupCode = matcher.groupValues[1]
                if (isValidPickupCode(pickupCode, smsContent)) {
                    var stationName = extractStationName(smsContent)
                    if (stationName.isNullOrEmpty()) {
                        stationName = "未知驿站"
                    }
                    
                    var address = extractAddress(smsContent)
                    if (address.isNullOrEmpty()) {
                        address = "地址未知"
                    }
                    
                    return ExpressInfo(
                        id = ExpressInfo.generateStableId(pickupCode, stationName, address, timestamp),
                        pickupCode = pickupCode,
                        pickupCodes = listOf(pickupCode),
                        stationName = stationName,
                        address = address,
                        smsContent = smsContent,
                        timestamp = timestamp
                    )
                }
            }
        }
        
        return null
    }
    
    /**
     * 使用通用模式解析短信
     */
    private fun parseWithUniversalPatterns(smsContent: String, timestamp: Long): ExpressInfo? {
        // 尝试提取所有可能的取件码
        val codePattern = Regex("([A-Z0-9]{2,}[\\-]?[A-Z0-9]+)")
        val codes = codePattern.findAll(smsContent)
            .map { it.value }
            .filter { isValidPickupCode(it, smsContent) }
            .toList()
        
        if (codes.isNotEmpty()) {
            val pickupCode = codes[0]
            
            var stationName = extractStationName(smsContent)
            if (stationName.isNullOrEmpty()) {
                stationName = "未知驿站"
            }
            
            var address = extractAddress(smsContent)
            if (address.isNullOrEmpty()) {
                address = "地址未知"
            }
            
            return ExpressInfo(
                id = ExpressInfo.generateStableId(pickupCode, stationName, address, timestamp),
                pickupCode = pickupCode,
                pickupCodes = codes,
                stationName = stationName,
                address = address,
                smsContent = smsContent,
                timestamp = timestamp
            )
        }
        
        return null
    }
    
    /**
     * 使用关键词增强解析
     */
    private fun parseWithKeywordEnhancement(smsContent: String, timestamp: Long): ExpressInfo? {
        // 检查是否包含快递相关关键词
        val expressKeywords = listOf(
            "快递", "取件", "取货", "驿站", "快递柜", "自提", "包裹", "物流", "配送", "送达", "领取",
            "丰巢", "菜鸟", "京东", "顺丰", "邮政", "中通", "圆通", "申通", "韵达", 
            "百世", "德邦", "EMS", "天天", "宅急送", "UPS", "DHL", "FedEx", "极兔", "兔喜", 
            "袋鼠智柜", "妈妈驿站", "韵达超市"
        )
        
        var hasExpressKeyword = false
        for (keyword in expressKeywords) {
            if (smsContent.contains(keyword)) {
                hasExpressKeyword = true
                break
            }
        }
        
        // 如果包含快递关键词，尝试提取信息
        if (hasExpressKeyword) {
            // 尝试提取取件码
            val codePattern = Regex("([A-Z0-9]{2,}[\\-]?[A-Z0-9]+)")
            val codes = codePattern.findAll(smsContent)
                .map { it.value }
                .filter { isValidPickupCode(it, smsContent) }
                .toList()
            
            if (codes.isNotEmpty()) {
                val pickupCode = codes[0]
                
                var stationName = extractStationName(smsContent)
                if (stationName.isNullOrEmpty()) {
                    stationName = "未知驿站"
                }
                
                var address = extractAddress(smsContent)
                if (address.isNullOrEmpty()) {
                    address = "地址未知"
                }
                
                return ExpressInfo(
                    id = ExpressInfo.generateStableId(pickupCode, stationName, address, timestamp),
                    pickupCode = pickupCode,
                    pickupCodes = codes,
                    stationName = stationName,
                    address = address,
                    smsContent = smsContent,
                    timestamp = timestamp
                )
            }
        }
        
        return null
    }
    
    /**
     * 提取驿站名称
     */
    private fun extractStationName(smsContent: String): String? {
        // 匹配常见的驿站名称
        val stationPatterns = listOf(
            Regex("【([^】]+)】"),
            Regex("(菜鸟驿站|妈妈驿站|快递驿站|代收点|圆通快递|申通快递|极兔速递|兔喜生活|袋鼠智柜|韵达超市|快递超市|菜鸟)"),
            Regex("(([^\\s]+驿站))")
        )
        
        for (pattern in stationPatterns) {
            val matcher = pattern.find(smsContent)
            if (matcher != null) {
                return if (matcher.groupValues.size > 1) {
                    matcher.groupValues[1]
                } else {
                    matcher.value
                }
            }
        }
        
        return null
    }
    
    /**
     * 提取地址
     */
    private fun extractAddress(smsContent: String): String? {
        // 匹配地址相关模式
        val addressPatterns = listOf(
            Regex("地址[:：]?\\s*([^，。\\[\\]【】]+)"),
            Regex("到([^，。]+)"),
            Regex("位于([^，。]+)"),
            Regex("在([^，。]+)")
        )
        
        for (pattern in addressPatterns) {
            val matcher = pattern.find(smsContent)
            if (matcher != null) {
                val address = matcher.groupValues[1].trim()
                if (address.isNotEmpty() && address.length > 2) {
                    return address
                }
            }
        }
        
        return null
    }
    
    /**
     * 验证取件码是否有效
     */
    private fun isValidPickupCode(code: String, smsContent: String): Boolean {
        // 使用现有的验证逻辑
        return ParcelInfoExtractor.isValidPickupCode(code, smsContent)
    }
}
package com.qujianma.app

import java.util.regex.Pattern

/**
 * 高级快递信息解析器，用于处理更复杂的短信格式
 */
object AdvancedExpressParser {
    
    /**
     * 高级解析方法，尝试多种策略解析短信
     */
    fun parse(smsContent: String, timestamp: Long): ExpressInfo? {
        // 1. 尝试使用现有的解析器（ExpressParser的简化版本）
        var result = parseSpecialFormats(smsContent, timestamp)
        if (result != null) {
            return result
        }
        
        // 2. 尝试使用通用模式解析器
        result = parseWithUniversalPatterns(smsContent, timestamp)
        if (result != null) {
            return result
        }
        
        // 3. 尝试使用关键词增强解析器
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
        // 检查是否为空
        if (code.isEmpty()) {
            return false
        }
        
        // 检查是否只包含数字、字母和连字符
        val pattern = "^[A-Za-z0-9\\-]+$"
        if (!code.matches(Regex(pattern))) {
            return false
        }
        
        // 检查是否包含至少一个数字
        if (!code.any { it.isDigit() }) {
            return false
        }
        
        // 长度应在合理范围内
        if (code.length < 2 || code.length > 20) {
            return false
        }
        
        // 排除时间格式（如 08:30-18:00）
        val timePattern = Regex("\\d{1,2}:\\d{2}(-\\d{1,2}:\\d{2})?")
        if (code.matches(timePattern)) {
            return false
        }
        
        // 排除日期时间格式（如 2023-02-17 10:41:00）
        val dateTimePattern = Regex("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}")
        if (code.matches(dateTimePattern)) {
            return false
        }
        
        // 排除日期格式（如 2023-02-17）
        val datePattern = Regex("\\d{4}-\\d{2}-\\d{2}")
        if (code.matches(datePattern)) {
            return false
        }
        
        // 排除时间范围格式（如 08:30-19:00 中的 30-19）
        val timeRangePattern = Regex("\\d{1,2}-\\d{1,2}")
        if (code.matches(timeRangePattern)) {
            // 检查这个代码是否在时间范围上下文中
            val contextPattern = Regex("\\d{1,2}:\\d{2}\\s*[-~至到]\\s*" + code + "\\s*[-~至到]\\s*\\d{1,2}:\\d{2}")
            if (contextPattern.containsMatchIn(smsContent)) {
                return false
            }
            // 特殊处理：检查是否在时间格式如 08:30-19:00 中
            val timeContextPattern = Regex("\\d{1,2}:\\d{2}\\s*-\\s*" + code + "\\s*-\\s*\\d{1,2}:\\d{2}")
            if (timeContextPattern.containsMatchIn(smsContent)) {
                return false
            }
            // 特殊处理：检查是否在时间格式如 08:30-21:00 中
            val timeContextPattern2 = Regex("\\d{1,2}:\\d{2}.*?" + code + ".*?\\d{1,2}:\\d{2}")
            if (timeContextPattern2.containsMatchIn(smsContent)) {
                return false
            }
            // 特殊处理：检查是否在时间范围格式如 10:00-22:00 中（00-22）
            val timeContextPattern3 = Regex("\\d{1,2}:(" + code + ")\\s*-\\s*\\d{1,2}:\\d{2}")
            if (timeContextPattern3.containsMatchIn(smsContent)) {
                return false
            }
            // 特殊处理：检查是否在时间范围格式如 08:30-19:00 中（30-19）
            val timeContextPattern4 = Regex("\\d{1,2}:\\d{2}\\s*-\\s*\\d{1,2}:(" + code + ")")
            if (timeContextPattern4.containsMatchIn(smsContent)) {
                return false
            }
            // 特殊处理：检查是否在中文时间范围格式中 如 08:30至19:00
            val timeContextPattern5 = Regex("\\d{1,2}:\\d{2}[至~到]" + code + "[至~到]\\d{1,2}:\\d{2}")
            if (timeContextPattern5.containsMatchIn(smsContent)) {
                return false
            }
            // 特殊处理：检查是否在中文时间范围格式中 如 08:30至19:00（前半部分）
            val timeContextPattern6 = Regex("\\d{1,2}:(" + code + ")\\s*[至~到]\\s*\\d{1,2}:\\d{2}")
            if (timeContextPattern6.containsMatchIn(smsContent)) {
                return false
            }
            // 特殊处理：检查是否在中文时间范围格式中 如 08:30至19:00（后半部分）
            val timeContextPattern7 = Regex("\\d{1,2}:\\d{2}\\s*[至~到]\\s*\\d{1,2}:(" + code + ")")
            if (timeContextPattern7.containsMatchIn(smsContent)) {
                return false
            }
            // 特殊处理：检查是否在复杂时间格式中
            val timeContextPattern8 = Regex("\\d{1,2}[:：]\\d{2}\\s*[-~至到]?\\s*" + code + "\\s*[-~至到]?\\s*\\d{1,2}[:：]\\d{2}")
            if (timeContextPattern8.containsMatchIn(smsContent)) {
                return false
            }
            // 特殊处理：检查是否在完整时间范围格式中（如 08:30-19:00）
            val timeContextPattern9 = Regex("\\d{1,2}:\\d{2}\\s*-\\s*\\d{1,2}:\\d{2}")
            if (timeContextPattern9.containsMatchIn(smsContent)) {
                // 如果在完整时间范围格式中，检查code是否是其中的一部分
                val timeRangeMatches = timeContextPattern9.findAll(smsContent)
                for (match in timeRangeMatches) {
                    val fullTimeRange = match.value
                    if (fullTimeRange.contains(code)) {
                        return false
                    }
                }
            }
        }
        
        // 排除运单尾号格式
        if (smsContent.contains("运单尾号") && smsContent.contains(code)) {
            val tailNumberPattern = Regex("运单尾号\\s*[:：]?\\s*" + code)
            if (tailNumberPattern.containsMatchIn(smsContent)) {
                return false
            }
        }
        
        // 排除"后四位"格式
        if (smsContent.contains("后四位") && smsContent.contains(code)) {
            val lastFourPattern = Regex("后四位\\s*[:：]?\\s*" + code)
            if (lastFourPattern.containsMatchIn(smsContent)) {
                return false
            }
        }
        
        // 排除银行尾号格式
        if (smsContent.contains("尾号") && smsContent.contains(code)) {
            val tailPattern = Regex("尾号[:：]?[为]?\\s*" + code)
            if (tailPattern.containsMatchIn(smsContent)) {
                return false
            }
        }
        
        // 检查是否全为数字
        val isAllDigits = code.all { it.isDigit() }
        
        // 如果全是数字，需要满足特定条件
        if (isAllDigits) {
            // 排除明显不是取件码的数字（如手机号部分、年份等）
            // 检查是否像手机号
            if (code.startsWith("1") && code.length == 11) {
                return false
            }
            // 检查是否像年份
            if (code.startsWith("20") && code.length == 4) {
                return false
            }
            // 排除太短的纯数字（如130）
            if (code.length < 4) {
                return false
            }
            // 排除太长的纯数字（如和多号副号中的数字）
            if (code.length > 15) {
                return false
            }
            
            // 排除明显的快递单号（13-15位数字通常为快递单号）
            if (code.length in 13..15) {
                // 检查是否在"快递XXXX"或"包裹XXXX"的上下文中
                val expressPattern = Regex("(快递|包裹)\\s*" + code)
                if (expressPattern.containsMatchIn(smsContent)) {
                    return false
                }
            }
            
            // 排除400/800客服电话
            if ((code.startsWith("400") || code.startsWith("800")) && code.length == 10) {
                return false
            }
            
            // 排除URL中的数字组合
            val urlPatterns = listOf(
                Regex("http[s]?://[\\w./?=&#]*" + Regex.escape(code) + "[\\w./?=&#]*"),
                Regex("u\\.cmread\\.com/[\\w]*" + Regex.escape(code) + "[\\w]*"),
                Regex("a\\.189\\.cn/[\\w]*" + Regex.escape(code) + "[\\w]*"),
                Regex("f\\.10086\\.cn/[\\w/]*#?[\\w]*" + Regex.escape(code) + "[\\w]*")
            )
            for (pattern in urlPatterns) {
                if (pattern.containsMatchIn(smsContent)) {
                    return false
                }
            }
            
            // 排除明显的快递单号（4-15位数字通常为快递单号）
            if (code.length in 4..15) {
                // 检查是否在"快递XXXX"或"包裹XXXX"的上下文中
                val expressPattern = Regex("(快递|包裹)\\s*" + code)
                if (expressPattern.containsMatchIn(smsContent)) {
                    return false
                }
                
                // 检查是否在"极兔XXXX包裹"格式中
                val specificExpressPattern = Regex("[极兔顺丰申通圆通中通韵达德邦EMS]\\s*" + code + "\\s*包裹")
                if (specificExpressPattern.containsMatchIn(smsContent)) {
                    return false
                }
                
                // 排除纯数字形式的营业时间（如0830）
                val timeContextPattern = Regex("([0-2]?[0-9][:：]\\d{2})[^\\d]+[至-][^\\d]+([0-2]?[0-9][:：]\\d{2})")
                if (timeContextPattern.containsMatchIn(smsContent)) {
                    // 查找时间范围附近的数字
                    val timeRangeNumbers = Regex("\\d{4}").findAll(smsContent)
                    for (match in timeRangeNumbers) {
                        if (match.value == code) {
                            return false
                        }
                    }
                }
                
                // 排除时间范围格式中的数字组合（如 09:00-21:00 中的 0900 和 2100）
                val timeRangePattern1 = Regex("([0-2]?[0-9]):([0-5][0-9])\\s*[-~至到]?\\s*([0-2]?[0-9]):([0-5][0-9])")
                val timeRangeMatches = timeRangePattern1.findAll(smsContent)
                for (match in timeRangeMatches) {
                    val hour1 = match.groupValues[1]
                    val minute1 = match.groupValues[2]
                    val hour2 = match.groupValues[3]
                    val minute2 = match.groupValues[4]
                    
                    // 检查是否匹配时间组合（如 0900, 2100）
                    if (code == "$hour1$minute1" || code == "$hour2$minute2") {
                        return false
                    }
                }
                
                // 排除中文时间格式（如 8:00 至 22:00）
                val chineseTimePattern = Regex("([0-2]?[0-9])[:：]([0-5][0-9])\\s*[至~到]?\\s*([0-2]?[0-9])[:：]([0-5][0-9])")
                val chineseTimeMatches = chineseTimePattern.findAll(smsContent)
                for (match in chineseTimeMatches) {
                    val hour1 = match.groupValues[1]
                    val minute1 = match.groupValues[2]
                    val hour2 = match.groupValues[3]
                    val minute2 = match.groupValues[4]
                    
                    // 检查是否匹配时间组合（如 0800, 2200）
                    if (code == "$hour1$minute1" || code == "$hour2$minute2") {
                        return false
                    }
                }
                
                // 排除复杂时间格式（如 09:00 - 21:00）
                val complexTimePattern = Regex("([0-2]?[0-9])[:：]([0-5][0-9])\\s*[-~至到]?\\s*([0-2]?[0-9])[:：]([0-5][0-9])")
                val complexTimeMatches = complexTimePattern.findAll(smsContent)
                for (match in complexTimeMatches) {
                    val hour1 = match.groupValues[1]
                    val minute1 = match.groupValues[2]
                    val hour2 = match.groupValues[3]
                    val minute2 = match.groupValues[4]
                    
                    // 检查是否匹配时间组合（如 0900, 2100）
                    if (code == "$hour1$minute1" || code == "$hour2$minute2") {
                        return false
                    }
                }
                
                // 排除单独出现的时间格式（如短信中出现"0900"和"2100"表示营业时间）
                val isolatedTimePattern = Regex("(?<!\\d)([0-2]?[0-9])([0-5][0-9])(?!\\d)")
                val isolatedTimeMatches = isolatedTimePattern.findAll(smsContent)
                for (match in isolatedTimeMatches) {
                    val fullMatch = match.value
                    val hour = match.groupValues[1]
                    val minute = match.groupValues[2]
                    
                    // 检查是否匹配时间组合（如 0900, 2100）
                    if (code == fullMatch || code == "$hour$minute") {
                        // 进一步检查上下文，看是否与时间相关词汇相邻
                        val contextCheckPattern = Regex("[营业时间时至到-]" + Regex.escape(fullMatch) + "[营业时间时至到-]")
                        if (contextCheckPattern.containsMatchIn(smsContent)) {
                            return false
                        }
                        
                        // 检查是否在常见时间上下文中
                        val timeContextCheckPattern = Regex("[时间营业至到-]" + Regex.escape(fullMatch))
                        if (timeContextCheckPattern.containsMatchIn(smsContent)) {
                            return false
                        }
                    }
                }
            }
            
            // 特殊处理：检查是否在掩码号码中（如178****7923中的7923）
            if (smsContent.contains("****") && code.length >= 4) {
                // 查找掩码号码模式
                val maskedNumberPattern = Regex("\\d+\\*+\\d+")
                val maskedNumbers = maskedNumberPattern.findAll(smsContent).map { it.value }.toList()
                for (maskedNumber in maskedNumbers) {
                    if (maskedNumber.contains(code)) {
                        return false
                    }
                }
            }
            
            // 特殊处理：检查是否在普通号码中（如1787923中的7923）
            if (!smsContent.contains("****") && code.length >= 4) {
                // 查找可能的长数字
                val longNumberPattern = Regex("\\d{7,}")
                val longNumbers = longNumberPattern.findAll(smsContent).map { it.value }.toList()
                for (longNumber in longNumbers) {
                    if (longNumber != code && longNumber.contains(code)) {
                        return false
                    }
                }
            }
        }
        
        // 检查是否包含连字符的格式（典型的取件码格式）
        val hasHyphen = code.contains("-")
        
        // 如果既不是全数字也不是包含连字符的格式，需要额外检查
        if (!isAllDigits && !hasHyphen) {
            // 检查是否包含字母
            val hasLetters = code.any { it.isLetter() }
            // 如果没有连字符也没有字母，可能是无效的取件码
            if (!hasLetters) {
                return false
            }
            
            // 排除纯字母词汇（如URL中的5G）
            if (code.all { it.isLetter() }) {
                return false
            }
        }
        
        // 排除流量等单位
        if (code.endsWith("GB") || code.endsWith("MB") || code.endsWith("KB")) {
            return false
        }
        
        // 特殊处理：排除在特定上下文中的数字（如"丰网8951包裹"中的8951）
        if (smsContent.contains(code)) {
            // 检查是否在"XX数字包裹"的上下文中
            val contextPattern = Regex("[\\w\\d]{1,10}" + code + "包裹")
            if (contextPattern.containsMatchIn(smsContent)) {
                return false
            }
            
            // 检查是否在"数字XX包裹"的上下文中
            val contextPattern2 = Regex(code + "[\\w\\d]{1,10}包裹")
            if (contextPattern2.containsMatchIn(smsContent)) {
                return false
            }
        }
        
        return true
    }
}
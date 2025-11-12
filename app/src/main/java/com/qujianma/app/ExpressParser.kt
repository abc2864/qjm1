package com.qujianma.app

import android.content.Context
import java.util.regex.Pattern

// 导入Rule类
import com.qujianma.app.Rule
import com.qujianma.app.RuleManager  // 添加这行导入

class ExpressParser {

    companion object {
        // 快递相关关键词
        private val EXPRESS_KEYWORDS = listOf(
            "快递", "取件", "取货", "驿站", "快递柜", "自提", "包裹", "物流", "配送", "送达", "领取",
            "丰巢", "菜鸟", "京东", "顺丰", "邮政", "中通", "圆通", "申通", "韵达", 
            "百世", "德邦", "EMS", "天天", "宅急送", "UPS", "DHL", "FedEx", "极兔", "兔喜", "袋鼠智柜", "妈妈驿站", "韵达超市"
        )
        
        // 银行相关关键词（需要排除）
        private val BANK_KEYWORDS = listOf(
            "银行", "信用卡", "储蓄卡", "账户", "余额", "转账", "汇款", "ATM", "网银", 
            "手机银行", "还款", "贷款", "利息", "理财", "基金", "股票", "保险", "证券",
            "面签", "实体卡片", "中国邮政", "邮政"
        )
        
        // 需要排除的其他非快递类关键词
        private val EXCLUDED_KEYWORDS = listOf(
            "验证码", "通知", "提醒", "公告", "通告", "消息", "信息" // 除了取件验证码，其他验证码都排除
        )
        
        // 电商相关关键词（需要排除）
        private val E_COMMERCE_KEYWORDS = listOf(
            "美团", "饿了么", "淘宝", "天猫", "京东", "拼多多", "抖音", "快手", 
            "苏宁", "唯品会", "小红书", "网易严选", "小米有品", "华为商城", "Apple Store",
            "购买", "下单", "订单", "商品", "购物", "优选"
        )
        
        // 医疗相关关键词（需要排除）
        private val MEDICAL_KEYWORDS = listOf(
            "医院", "门诊", "科室", "就诊", "挂号", "预约", "签到", "附院", "自助机"
        )
        
        // 快递单号关键词（需要排除）
        private val TRACKING_NUMBER_KEYWORDS = listOf(
            "快递单号", "运单号", "订单号", "物流单号", "跟踪号"
        )
        
        // 需要排除的特殊关键词
        private val SPECIAL_EXCLUDED_KEYWORDS = listOf(
            "系统", "平台", "网站", "APP", "应用", "软件", "程序", "服务", "客服",
            "活动", "优惠", "促销", "折扣", "返现", "返利", "奖励", "积分", "会员",
            "注册", "登录", "密码", "账号", "账户", "充值", "支付", "付款", "退款",
            "订单", "购买", "购物", "商城", "商店", "店铺", "商品", "产品", "货物",
            "生日", "礼包", "包邮", "拒收", "回复", "详情", "查看", "评价", "满意度",
            "调查", "问卷", "反馈", "建议", "投诉", "表扬", "表扬信", "表扬信", "福利",
            "流量", "GB", "MB", "数字人", "链接", "网址", "网站", "点击", "戳"
        )
        
        // 物流状态更新相关关键词（需要排除）
        private val LOGISTICS_STATUS_KEYWORDS = listOf(
            "物流状态", "已更新", "问题", "解决", "处理详情", "尾号", "快件", "查看", "详情", "跟踪", "查询"
        )
        
        // 已完成状态关键词（需要排除）
        private val COMPLETED_STATUS_KEYWORDS = listOf(
            "已完成", "已取件", "已签收", "已送达", "已领取", "已出库", "已发货",
            "完成取件", "取件完成"
        )
        
        // 退货/售后相关关键词（需要排除）
        private val RETURN_SERVICE_KEYWORDS = listOf(
            "退货", "退款", "售后", "寄回", "二次销售", "退货快递", "退货单号", "退货申请", "维修",
            "服务单", "闪电退款", "退换"
        )

        /**
         * 解析短信内容，提取快递信息
         */
        fun parse(smsContent: String, timestamp: Long, context: Context): ExpressInfo? {
            // 处理服务号短信格式（服务号: 短信内容）
            var processedContent = smsContent
            if (smsContent.contains(": ") && 
                (smsContent.substring(0, smsContent.indexOf(": ")).all { it.isDigit() || it == '+' })) {
                val parts = smsContent.split(": ", limit = 2)
                if (parts.size == 2) {
                    processedContent = parts[1]
                }
            }
            
            // 首先尝试使用下载的规则解析
            val downloadedRules = SettingsActivity.loadDownloadedRules(context)
            if (downloadedRules.isNotEmpty()) {
                val downloadedParsed = parseWithCustomRules(processedContent, timestamp, downloadedRules)
                if (downloadedParsed != null) {
                    return downloadedParsed
                }
            }
            
            // 然后尝试使用本地自定义规则解析
            val localRules = SettingsActivity.loadRules(context)
            if (localRules.isNotEmpty()) {
                val localParsed = parseWithCustomRules(processedContent, timestamp, localRules)
                if (localParsed != null) {
                    return localParsed
                }
            }
            
            // 特殊处理兔喜生活短信
            val tuXiLifeParsed = parseTuXiLifeSMS(processedContent, timestamp)
            if (tuXiLifeParsed != null) {
                return tuXiLifeParsed
            }
            
            // 最后使用内置提取器解析
            return parseWithExtractor(processedContent, timestamp)
        }
        
        /**
         * 解析短信内容，提取快递信息（重载版本，不使用自定义规则）
         */
        fun parse(smsContent: String, timestamp: Long): ExpressInfo? {
            // 直接使用新的提取器解析，不使用自定义规则
            return parseWithExtractor(smsContent, timestamp)
        }
        
        /**
         * 使用下载的规则和关键词解析短信
         */
        fun parseWithDownloadedRules(smsContent: String, timestamp: Long, context: Context): ExpressInfo? {
            // 加载下载的规则和关键词
            val ruleManager = RuleManager(context)
            val rulesData = ruleManager.loadRulesFromLocal()
            
            // 如果有下载的规则，优先使用下载的规则
            if (rulesData.rules != null && rulesData.rules.isNotEmpty()) {
                // 转换下载的规则为本地Rule对象
                val downloadedRules = rulesData.rules.map { ruleData ->
                    Rule(
                        name = ruleData.name ?: "",
                        ruleType = "regex", // 默认使用正则类型
                        pattern = ruleData.pattern ?: "",
                        enabled = true
                    )
                }
                
                val parsed = parseWithCustomRules(smsContent, timestamp, downloadedRules)
                if (parsed != null) {
                    return parsed
                }
            }
            
            return null
        }
        
        /**
         * 特殊处理兔喜生活短信
         */
        private fun parseTuXiLifeSMS(smsContent: String, timestamp: Long): ExpressInfo? {
            // 检查是否为兔喜生活短信格式
            if (smsContent.contains("兔喜生活") && smsContent.contains("取货码")) {
                // 提取取货码
                val codePattern = Regex("取货码[:：]?\\s*(\\w+)")
                val codeMatcher = codePattern.find(smsContent)
                val pickupCode = codeMatcher?.groupValues?.get(1)?.trim()
                
                if (pickupCode != null && isValidPickupCode(pickupCode, smsContent)) {
                    // 提取驿站名称
                    var stationName = "兔喜生活"
                    val stationPattern = Regex("[【\\[](.*?)[】\\]]")
                    val stationMatcher = stationPattern.find(smsContent)
                    if (stationMatcher != null) {
                        stationName = stationMatcher.groupValues[1].trim()
                    }
                    
                    // 提取地址
                    var address = "地址未知"
                    // 尝试从短信中提取地址
                    val addressKeywords = listOf("地址", "位于", "位置")
                    for (keyword in addressKeywords) {
                        val addressPattern = Regex("$keyword[:：]?\\s*([^\\n\\r]+)")
                        val addressMatcher = addressPattern.find(smsContent)
                        if (addressMatcher != null) {
                            address = addressMatcher.groupValues[1].trim()
                            break
                        }
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
            return null
        }
        
        /**
         * 判断是否不是快递短信
         */
        private fun isNotExpressSMS(smsContent: String): Boolean {
            // 包含银行相关关键词的短信不是快递短信
            for (keyword in BANK_KEYWORDS) {
                if (smsContent.contains(keyword, ignoreCase = true)) {  // 忽略大小写匹配
                    return true
                }
            }
            
            // 特殊处理：包含"银联"且不包含快递相关关键词的短信不是快递短信
            if (smsContent.contains("银联") && !isExpressRelatedSMS(smsContent)) {
                return true
            }
            
            // 特殊处理：包含银行服务代码（如LPR、ATM编号等）且不包含快递相关关键词的短信不是快递短信
            val bankServiceCodePattern = Regex("([A-Z]{3}\\d{4,}|\\d+[A-Z]+)")
            if (bankServiceCodePattern.containsMatchIn(smsContent) && !isExpressRelatedSMS(smsContent)) {
                return true
            }

            // 包含其他需要排除的关键词且不包含取件关键词的短信不是快递短信
            for (keyword in EXCLUDED_KEYWORDS) {
                if (smsContent.contains(keyword) && !smsContent.contains("取件") && !smsContent.contains("取货")) {
                    return true
                }
            }
            
            // 包含退订相关关键词的短信不是快递短信
            val unsubscribeKeywords = listOf("退订", "回T", "TD", "退订回复")
            for (keyword in unsubscribeKeywords) {
                if (smsContent.contains(keyword)) {
                    return true
                }
            }
            
            // 检查是否为退货/售后类短信
            if (isReturnServiceSMS(smsContent)) {
                return true
            }
            
            // 特殊处理：满意度调查类短信不是快递短信
            val satisfactionKeywords = listOf("满意度", "评价", "调查", "反馈", "建议", "投诉", "表扬")
            val rewardKeywords = listOf("福利", "流量", "GB", "MB", "领取", "数字人", "链接", "网址")
            
            var satisfactionCount = 0
            var rewardCount = 0
            
            for (keyword in satisfactionKeywords) {
                if (smsContent.contains(keyword)) {
                    satisfactionCount++
                }
            }
            
            for (keyword in rewardKeywords) {
                if (smsContent.contains(keyword)) {
                    rewardCount++
                }
            }
            
            // 如果同时包含满意度相关和福利相关关键词，且不包含典型的取件关键词，则认为不是快递短信
            if (satisfactionCount > 0 && rewardCount > 0 && 
                !smsContent.contains("取件码") && 
                !smsContent.contains("取货码") && 
                !smsContent.contains("包裹") && 
                !smsContent.contains("驿站")) {
                return true
            }

            return false
        }
        
        /**
         * 检查是否为退货/售后类短信
         */
        private fun isReturnServiceSMS(smsContent: String): Boolean {
            // 检查是否包含退货/售后相关关键词
            for (keyword in RETURN_SERVICE_KEYWORDS) {
                if (smsContent.contains(keyword)) {
                    // 如果包含退货相关关键词，且不包含典型的取件关键词，则认为是退货/售后类短信
                    if (!smsContent.contains("请") ||
                        (!smsContent.contains("取件") &&
                         !smsContent.contains("取货") &&
                         !smsContent.contains("领取"))) {
                        return true
                    }
                    // 特殊处理：即使包含取件关键词，但如果明确是退货相关，则仍认为是退货/售后类短信
                    if (smsContent.contains("退货") || 
                        smsContent.contains("退款") || 
                        smsContent.contains("售后") ||
                        smsContent.contains("寄回") ||
                        smsContent.contains("退货申请")) {
                        return true
                    }
                }
            }
            
            // 特殊处理：如果同时包含"退款"和"上门取件"，则认为是退货/售后类短信
            if (smsContent.contains("退款") && smsContent.contains("上门取件")) {
                return true
            }
            
            // 特殊处理：如果同时包含"服务单"和"上门取件"，则认为是退货/售后类短信
            if (smsContent.contains("服务单") && smsContent.contains("上门取件")) {
                return true
            }
            
            // 特殊处理：如果包含"退货申请"，则认为是退货/售后类短信
            if (smsContent.contains("退货申请")) {
                return true
            }
            
            return false
        }
        
        /**
         * 判断是否是快递相关短信
         */
        private fun isExpressRelatedSMS(smsContent: String): Boolean {
            // 检查是否包含至少一个快递相关关键词
            for (keyword in EXPRESS_KEYWORDS) {
                if (smsContent.contains(keyword)) {
                    return true
                }
            }
            
            // 如果不包含快递相关关键词，进一步检查是否包含取件相关关键词
            if (smsContent.contains("取件") || smsContent.contains("取货") || smsContent.contains("码")) {
                return true
            }
            
            return false
        }
        
        /**
         * 检查是否为快递单号通知
         */
        private fun isTrackingNumberNotification(smsContent: String): Boolean {
            // 如果包含快递单号相关关键词，但不包含取件相关关键词，则认为是快递单号通知
            for (keyword in TRACKING_NUMBER_KEYWORDS) {
                if (smsContent.contains(keyword) && !smsContent.contains("取件") && !smsContent.contains("取货")) {
                    return true
                }
            }
            return false
        }
        
        /**
         * 检查是否包含特殊排除关键词
         */
        private fun containsSpecialExcludedKeywords(smsContent: String): Boolean {
            // 检查是否包含特殊排除关键词
            for (keyword in SPECIAL_EXCLUDED_KEYWORDS) {
                // 如果包含特殊排除关键词，且不包含取件相关关键词，则认为不是快递短信
                if (smsContent.contains(keyword) && 
                    !smsContent.contains("取件") && 
                    !smsContent.contains("取货") && 
                    !smsContent.contains("包裹") && 
                    !smsContent.contains("驿站")) {
                    return true
                }
            }
            
            // 特殊处理：如果包含医疗相关关键词，则认为不是快递短信
            for (keyword in MEDICAL_KEYWORDS) {
                if (smsContent.contains(keyword)) {
                    return true
                }
            }
            
            // 特殊处理：如果包含电商相关关键词，则认为不是快递短信
            for (keyword in E_COMMERCE_KEYWORDS) {
                if (smsContent.contains(keyword)) {
                    return true
                }
            }
            
            return false
        }
        
        /**
         * 检查是否为物流状态更新短信
         */
        private fun isLogisticsStatusUpdate(smsContent: String): Boolean {
            // 检查是否包含物流状态更新相关关键词
            var count = 0
            for (keyword in LOGISTICS_STATUS_KEYWORDS) {
                if (smsContent.contains(keyword)) {
                    count++
                }
            }
            
            // 检查是否包含已完成状态关键词
            for (keyword in COMPLETED_STATUS_KEYWORDS) {
                if (smsContent.contains(keyword)) {
                    // 如果包含已完成状态关键词，且不包含待取件相关关键词，则认为是物流状态更新短信
                    if (!smsContent.contains("请") || 
                        (!smsContent.contains("取件") && 
                         !smsContent.contains("取货") && 
                         !smsContent.contains("领取"))) {
                        return true
                    }
                }
            }
            
            // 特殊处理：如果包含"完成"和"取件"，则认为是已完成状态
            if (smsContent.contains("完成") && smsContent.contains("取件")) {
                return true
            }
            
            // 如果包含多个物流状态更新关键词，且不包含取件相关关键词，则认为是物流状态更新短信
            if (count >= 2 && 
                !smsContent.contains("取件") && 
                !smsContent.contains("取货") && 
                !smsContent.contains("领取")) {
                return true
            }
            
            // 特殊处理：如果包含"尾号"和"物流状态"，且不包含取件相关关键词，则认为是物流状态更新短信
            if (smsContent.contains("尾号") && smsContent.contains("物流状态") &&
                !smsContent.contains("取件") && 
                !smsContent.contains("取货") && 
                !smsContent.contains("领取")) {
                return true
            }
            
            return false
        }
        
        /**
         * 检查是否包含取件相关关键词
         */
        private fun containsPickupRelatedKeywords(smsContent: String): Boolean {
            // 如果包含"退订"、"回T"、"回TD"等退订相关关键词，则不是取件短信
            if (smsContent.contains("退订") || 
                smsContent.contains("回T") ||
                smsContent.contains("TD")) {
                return false
            }
            
            return smsContent.contains("取件") || 
                   smsContent.contains("取货") || 
                   smsContent.contains("领取") ||
                   smsContent.contains("凭") ||
                   (smsContent.contains("取") && 
                    !smsContent.contains("自助机签到") && 
                    !smsContent.contains("签到")) ||
                   (smsContent.contains("码") && 
                    !smsContent.contains("验证码") &&
                    !smsContent.contains("退订"))
        }
        
        /**
         * 验证取件码是否为非文本（数字或数字字母组合）
         */
        private fun isValidPickupCode(code: String, smsContent: String = ""): Boolean {
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
            
            // 排除URL中的数字组合
            val urlPatterns = listOf(
                Regex("http[s]?://[\\w./?=&]*" + code + "[\\w./?=&]*"),
                Regex("u\\.cmread\\.com/[\\w]*" + code + "[\\w]*"),
                Regex("a\\.189\\.cn/[\\w]*" + code + "[\\w]*"),
                Regex("f\\.10086\\.cn/[\\w/]*#?[\\w]*" + code + "[\\w]*")
            )
            for (pattern in urlPatterns) {
                if (pattern.containsMatchIn(smsContent)) {
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
        
        /**
         * 清理和标准化地址信息
         */
        private fun cleanAddress(address: String): String {
            // 如果地址为空或无效，返回"地址未知"
            if (address == "地址未知" || address.trim().isEmpty()) {
                return "地址未知"
            }
            
            var cleaned = address
            
            // 移除多余的空格和特殊字符
            cleaned = cleaned.trim().replace("\\s+".toRegex(), " ")
            
            // 移除常见的非地址信息
            // 移除结尾的"收"、"发"等字
            cleaned = cleaned.replace(Regex("[收发]$")) { "" }
            // 移除开头的"地址"、"地址："等信息
            cleaned = cleaned.replace(Regex("^地址[:：]?\\s*")) { "" }
            
            // 移除开头的冗余信息（多次处理直到没有匹配项）
            val redundantStartings = listOf(
                "到", "在", "于", "请", "至", "往", "送达", "送至", 
                "：", ":", " ", "　" // 包含全角空格
            )
            
            var continueStripping = true
            while (continueStripping && cleaned.isNotEmpty()) {
                continueStripping = false
                for (starting in redundantStartings) {
                    if (cleaned.startsWith(starting)) {
                        cleaned = cleaned.substring(starting.length)
                        continueStripping = true
                        break // 从头开始重新检查
                    }
                }
            }
            
            // 如果清理后地址为空，返回"地址未知"
            if (cleaned.isEmpty()) {
                return "地址未知"
            }
            
            // 移除地址末尾的逗号或顿号
            
            // 特殊处理：如果地址包含"（"和"）"，则移除括号及内容
            cleaned = cleaned.replace(Regex("（[^）]*）")) { "" }
            
            // 进一步优化地址清理，处理更多边界情况
            // 去除地址末尾的提示性文字
            val promptEndings = listOf(
                ",请", "，请", "请您", "佩戴", "口罩", "个人", "防护",
                "及时", "取件", "联系", "电话"
            )
            
            // 循环去除末尾的提示性文字
            var changed = true
            while (changed) {
                changed = false
                for (ending in promptEndings) {
                    if (cleaned.endsWith(ending)) {
                        cleaned = cleaned.substring(0, cleaned.length - ending.length).trim()
                        changed = true
                        break
                    }
                }
            }
            
            // 去除逗号或顿号后的内容（如"创业路小羊馆中通,请您..."）
            val commaIndex = cleaned.indexOfFirst { it == ',' || it == '，' }
            if (commaIndex > 0) {
                cleaned = cleaned.substring(0, commaIndex)
            }
            
            // 特殊处理：移除地址末尾的"取包裹"、"领取包裹"、"拿包裹"、"取快递"、"领取快递"、"拿快递"、"运单尾号XXXX包裹"等字样
            val pickupEndings = listOf("取包裹", "领取包裹", "拿包裹", "取快递", "领取快递", "拿快递")
            for (ending in pickupEndings) {
                if (cleaned.endsWith(ending)) {
                    cleaned = cleaned.substring(0, cleaned.length - ending.length).trim()
                }
            }
            // 特殊处理运单尾号模式
            cleaned = cleaned.replace(Regex("运单尾号\\d+包裹.*$")) { "" }.trim()
            
            // 确保地址不是太短
            if (cleaned.length < 2) {
                return "地址未知"
            }
            
            // 最终验证地址有效性
            // 如果地址看起来像电话号码或邮政编码，则标记为未知
            val likelyNotAddressPattern = Regex("^(\\d{11}|\\d{6})$") // 可能是手机号或邮编
            if (cleaned.matches(likelyNotAddressPattern)) {
                return "地址未知"
            }
            
            return cleaned.trim()
        }
        
        /**
         * 使用新的提取器解析短信
         */
        private fun parseWithExtractor(smsContent: String, timestamp: Long): ExpressInfo? {
            val extractedInfo = extractParcelInfo(smsContent)
            
            val pickupCodesAny = extractedInfo["codes"]
            val stationName = extractedInfo["station"] as? String
            val address = extractedInfo["address"] as? String
            
            // 必须至少提取到一个有效的取件码和驿站名称
            if (pickupCodesAny == null || stationName == null) {
                // 如果标准解析器无法解析，尝试使用高级解析器
                return AdvancedExpressParser.parse(smsContent, timestamp, android.app.Application())
            }
            
            // 转换取件码列表
            val pickupCodes = when (pickupCodesAny) {
                is List<*> -> pickupCodesAny.filterIsInstance<String>()
                else -> emptyList()
            }
            
            if (pickupCodes.isEmpty()) {
                // 如果标准解析器无法解析，尝试使用高级解析器
                return AdvancedExpressParser.parse(smsContent, timestamp, android.app.Application())
            }
            
            // 验证至少有一个取件码是有效的
            val validPickupCodes = pickupCodes.filter { isValidPickupCode(it, smsContent) }
            if (validPickupCodes.isEmpty()) {
                // 如果标准解析器无法解析，尝试使用高级解析器
                return AdvancedExpressParser.parse(smsContent, timestamp, android.app.Application())
            }
            
            return ExpressInfo(
                id = ExpressInfo.generateStableId(validPickupCodes[0], stationName, cleanAddress(address ?: "地址未知"), timestamp),
                pickupCode = validPickupCodes[0], // 主取件码
                pickupCodes = validPickupCodes,   // 所有有效取件码
                stationName = stationName,
                address = cleanAddress(address ?: "地址未知"),
                smsContent = smsContent,
                timestamp = timestamp
            )
        }
        
        /**
         * 使用高级提取器解析短信（后备方案）
         */
        private fun parseWithAdvancedExtractor(smsContent: String, timestamp: Long): ExpressInfo? {
            return AdvancedExpressParser.parse(smsContent, timestamp, android.app.Application())
        }

        /**
         * 使用自定义规则解析短信
         */
        private fun parseWithCustomRules(smsContent: String, timestamp: Long, rules: List<Rule>): ExpressInfo? {
            // 如果没有自定义规则，直接返回null
            if (rules.isEmpty()) {
                return null
            }
            
            // 首先进行基本过滤，排除银行等非快递短信
            if (isNotExpressSMS(smsContent)) {
                return null
            }
            
            // 检查是否包含快递相关关键词
            if (!isExpressRelatedSMS(smsContent)) {
                return null
            }

            // 确保不是快递单号通知
            if (isTrackingNumberNotification(smsContent)) {
                return null
            }
            
            // 检查是否包含特殊排除关键词
            if (containsSpecialExcludedKeywords(smsContent)) {
                return null
            }
            
            // 检查是否为物流状态更新短信
            if (isLogisticsStatusUpdate(smsContent)) {
                return null
            }
            
            // 检查是否包含取件相关关键词
            if (!containsPickupRelatedKeywords(smsContent)) {
                return null
            }

            // 遍历启用的规则
            for (rule in rules) {
                if (!rule.enabled) continue
                
                try {
                    var pickupCode: String? = null
                    var stationName: String? = null
                    var address: String? = null
                    var phone: String? = null

                    // 根据规则类型处理
                    if (rule.ruleType == "regex") {
                        // 正则规则处理
                        if (rule.pattern.isNotEmpty()) {
                            val pattern = Pattern.compile(rule.pattern)
                            val matcher = pattern.matcher(smsContent)
                            if (matcher.find()) {
                                // 尝试提取不同的组作为取件码、驿站和地址
                                if (matcher.groupCount() >= 1) {
                                    pickupCode = matcher.group(1)?.trim()
                                }
                                if (matcher.groupCount() >= 2) {
                                    stationName = matcher.group(2)?.trim()
                                }
                                if (matcher.groupCount() >= 3) {
                                    address = matcher.group(3)?.trim()
                                }
                            }
                        }
                    } else {
                        // 自定义前后缀规则处理
                        
                        // 构建取件码正则表达式
                        if (rule.codePrefix.isNotEmpty() || rule.codeSuffix.isNotEmpty()) {
                            val codePattern = "${Pattern.quote(rule.codePrefix)}(.*?)${Pattern.quote(rule.codeSuffix)}"
                            val codeMatcher = Pattern.compile(codePattern).matcher(smsContent)
                            if (codeMatcher.find()) {
                                pickupCode = codeMatcher.group(1)?.trim()
                            }
                        }

                        // 构建驿站名称正则表达式
                        if (rule.tagPrefix.isNotEmpty() || rule.tagSuffix.isNotEmpty()) {
                            val tagPattern = "${Pattern.quote(rule.tagPrefix)}(.*?)${Pattern.quote(rule.tagSuffix)}"
                            val tagMatcher = Pattern.compile(tagPattern).matcher(smsContent)
                            if (tagMatcher.find()) {
                                stationName = tagMatcher.group(1)?.trim()
                            }
                        }

                        // 构建地址正则表达式
                        if (rule.addressPrefix.isNotEmpty() || rule.addressSuffix.isNotEmpty()) {
                            val addressPattern = "${Pattern.quote(rule.addressPrefix)}(.*?)${Pattern.quote(rule.addressSuffix)}"
                            val addressMatcher = Pattern.compile(addressPattern).matcher(smsContent)
                            if (addressMatcher.find()) {
                                address = addressMatcher.group(1)?.trim()
                            }
                        }

                        // 构建电话正则表达式
                        if (rule.phonePrefix.isNotEmpty() || rule.phoneSuffix.isNotEmpty()) {
                            val phonePattern = "${Pattern.quote(rule.phonePrefix)}(.*?)${Pattern.quote(rule.phoneSuffix)}"
                            val phoneMatcher = Pattern.compile(phonePattern).matcher(smsContent)
                            if (phoneMatcher.find()) {
                                phone = phoneMatcher.group(1)?.trim()
                            }
                        }
                        
                        // 构建驿站正则表达式
                        if (rule.stationPrefix.isNotEmpty() || rule.stationSuffix.isNotEmpty()) {
                            val stationPattern = "${Pattern.quote(rule.stationPrefix)}(.*?)${Pattern.quote(rule.stationSuffix)}"
                            val stationMatcher = Pattern.compile(stationPattern).matcher(smsContent)
                            if (stationMatcher.find()) {
                                stationName = stationMatcher.group(1)?.trim()
                            }
                        }
                    }

                    
                    if (pickupCode != null && stationName != null && address != null) {
                        // 检查取件码是否有效
                        if (!isValidPickupCode(pickupCode, smsContent)) {
                            continue
                        }

                        // 检查地址是否有效
                        address = cleanAddress(address)

                        // 创建并返回 ExpressInfo 对象
                        return ExpressInfo(
                            pickupCode = pickupCode,
                        )
                    }
                } catch (e: Exception) {
                    // 忽略单个规则的解析错误，继续尝试其他规则
                    e.printStackTrace()
                }
            }

            // 没有匹配的自定义规则
            return null
        }
    }
}
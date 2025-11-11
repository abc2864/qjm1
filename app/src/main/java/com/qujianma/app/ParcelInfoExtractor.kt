package com.qujianma.app

// 定义需要排除的特殊关键词
private val SPECIAL_EXCLUDED_KEYWORDS = listOf(
    "系统", "平台", "网站", "APP", "应用", "软件", "程序", "服务", "客服",
    "活动", "优惠", "促销", "折扣", "返现", "返利", "奖励", "积分", "会员",
    "注册", "登录", "密码", "账号", "账户", "充值", "支付", "付款", "退款",
    "订单", "购买", "购物", "商城", "商店", "店铺", "商品", "产品", "货物",
    "通知", "提醒", "公告", "通告", "消息", "信息",
    "生日", "礼包", "包邮", "拒收", "回复", "详情", "查看"
)

// 医疗相关关键词（需要排除）
private val MEDICAL_KEYWORDS = listOf(
    "医院", "门诊", "科室", "就诊", "挂号", "预约", "签到", "附院", "自助机"
)

// 电商相关关键词（需要排除）
private val E_COMMERCE_KEYWORDS = listOf(
    "美团", "饿了么", "淘宝", "天猫", "京东", "拼多多", "抖音", "快手", 
    "苏宁", "唯品会", "小红书", "网易严选", "小米有品", "华为商城", "Apple Store",
    "购买", "下单", "订单", "商品", "购物", "优选"
)

// 物流状态更新相关关键词（需要排除）
private val LOGISTICS_STATUS_KEYWORDS = listOf(
    "物流状态", "已更新", "问题", "解决", "处理详情", "尾号"
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

// 银行相关关键词（需要排除）
private val BANK_KEYWORDS = listOf(
    "银行", "信用卡", "储蓄卡", "账户", "余额", "转账", "汇款", "ATM", "网银", 
    "手机银行", "还款", "贷款", "利息", "理财", "基金", "股票", "保险", "证券",
    "面签", "实体卡片", "中国邮政", "邮政"
)

// 彩铃、视频等营销服务关键词（需要排除）
private val RING_SERVICE_KEYWORDS = listOf(
    "视频彩铃", "彩铃", "彩铃包", "视频", "视讯", "来电", "新视界"
)

// 定义正则表达式常量
object ParcelInfoRegex {
    // 优化取件码正则表达式，使其更精确
    val CODE_PATTERN = Regex("(?<!\\d)([A-Z0-9]{1,2}[-]?){1,4}[A-Z0-9]{1,12}(?!\\d)")
    val STATION_PATTERN = Regex("(菜鸟驿站|妈妈驿站|快递驿站|代收点|圆通快递|申通快递|极兔速递|兔喜生活|袋鼠智柜|韵达超市|快递超市|菜鸟)")
    // 优化驿站名称匹配模式，更好地处理包含【】的情况，但只提取第一个【】中的内容
    val TAG_PATTERNS = listOf(
        Regex("【([^】]+)】"),  // 匹配第一个【】中的内容
        Regex("(菜鸟驿站|妈妈驿站|快递驿站|代收点|圆通快递|申通快递|极兔速递|兔喜生活|袋鼠智柜|韵达超市|快递超市|菜鸟)")
    )
    val ADDRESS_PATTERNS = listOf(
        Regex("地址[:：]\\s*([^，。\\[\\]【】]+?(?=\\s*\\[|\\s*【|\\s*$|\\s*取件码|\\s*请凭|\\s*凭))"),  // 优先提取"地址:"后的内容
        Regex("地址[:：]\\s*([^，。\\[\\]【】]+)"),  // 备用的"地址:"提取模式
        Regex("到([^，。]+?)(?=取您的快递|取包裹|领取)"),  // 针对短信1的模式
        Regex("到((?:[^，。]*?(?:镇|街道|路|街|巷|村|区|市|县|乡|十字|政府|小区|门店|店|部|超市|驿站|塔)[^，。]*?)+?)\\s*(?=凭|\\d+-\\d+-\\d+|,|，|$)"),  // 针对包含地标词的地址提取
        Regex("到([^，。]+?)(?=取您的快递|取包裹|领取|凭|请凭|,请|，请|,|，|$)"),  // 更通用的模式，增加终止条件
        Regex("到([^，。]+?(?=(取|领取|拿|拿取).*?(包裹|快递)))"),  // 针对"取包裹"、"领取包裹"等模式
        Regex("到([^，。]*?(?:驿站|快递|代收点|服务站|自提点|营业部|超市|门店|店|部)[^，。]*?)(?=,|，|。|凭|请凭|$)"),  // 针对包含驿站相关关键词的地址提取
        Regex("到([^，。]*?街[^，。]+?)(?=,|，|。|凭|请凭|$)"),  // 针对包含"街"的地址提取
        Regex("到((?:[^，。]*?(?:镇|街道|路|街|巷|村|区|市|县|乡|十字|政府|小区|门店|店|部|超市|驿站|塔)[^，。]*?)+?)(?=,|，|。|凭|请凭|\\d+-\\d+-\\d+|$)"),  // 优化的地址提取，确保包含地标词的完整地址
        Regex("到([^，。]+)"),  // 基础模式
        Regex("([^，。]+)(?=请凭|取货码|取件码)"),
        Regex("已到([^，。]+)"),
        Regex("(([\\S]{2,8}[·•.]){0,3}[\\S]{2,8}(驿站|快递柜|服务站|自提点|营业部|代收点|快递超市|速递超市|门店))"), 
        Regex("([\\S]{2,20}(路|街|巷|村|镇|区|市|县|乡|十字|政府|小区|门店|店|部|超市|驿站)[\\S]{0,20})"),
        Regex("([^，。]+(路|街|巷|村|镇|区|市|县|乡|十字|政府|小区|门店|店|部|超市|驿站)[^，。]+)"),  // 新增：匹配包含地标词的完整地址
        Regex("到([^，。]*?路[^，。]+?)(?=,|，|。|凭|请凭|$)"),  // 针对包含"路"的地址提取
        Regex("地址[:：]\\s*([^，。]+)"),  // 优先提取"地址："后的完整内容
        Regex("暂存((?:[^，。]*?(?:镇|街道|路|街|巷|村|区|市|县|乡|十字|政府|小区|门店|店|部|超市|驿站|塔)[^，。]*?)+?)(?=,|，|。|凭|请凭|\\d+-\\d+-\\d+|$)"), // 针对"暂存XXX"格式的地址，确保包含地标词
        Regex("暂存([^，。]+)"), // 针对"暂存XXX"格式的地址
        // 特别针对用户反馈的问题进行优化
        Regex("到(北杜镇\\s*千佛塔向东\\d+米路南中通快递门店内)(?=,|，|。|凭|请凭|\\d+-\\d+-\\d+|$)"), // 针对北杜镇千佛塔的具体地址格式
        Regex("暂存(北杜镇\\s*千佛塔向东\\d+米路南中通快递门店内)(?=,|，|。|凭|请凭|\\d+-\\d+-\\d+|$)"), // 针对暂存北杜镇千佛塔的具体地址格式
        Regex("到(北杜镇\\s*千佛塔向东\\d+米路南.*?门店内)(?=,|，|。|凭|请凭|\\d+-\\d+-\\d+|$)"), // 针对北杜镇千佛塔的通用地址格式
        Regex("暂存(北杜镇\\s*千佛塔向东\\d+米路南.*?门店内)(?=,|，|。|凭|请凭|\\d+-\\d+-\\d+|$)"), // 针对暂存北杜镇千佛塔的通用地址格式
        Regex("([^\\s，。）]+(?=，询|【))"),
        Regex("([^，。]+(?=,|，|。|$))") // 新增：更通用的地址提取模式，匹配到逗号、句号或行尾
    )
}

// 检查是否包含特殊排除关键词
fun containsSpecialExcludedKeywords(smsContent: String): Boolean {
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
    
    // 特殊处理：如果包含彩铃等营销服务关键词，则认为不是快递短信
    for (keyword in RING_SERVICE_KEYWORDS) {
        if (smsContent.contains(keyword)) {
            return true
        }
    }
    
    return false
}

// 检查是否为物流状态更新短信
fun isLogisticsStatusUpdate(smsContent: String): Boolean {
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
    
    // 如果包含多个物流状态更新关键词，且不包含取件相关关键词，则认为是物流状态更新短信
    if (count >= 2 && 
        !smsContent.contains("取件") && 
        !smsContent.contains("取货") && 
        !smsContent.contains("领取")) {
        return true
    }
    
    return false
}

// 检查是否为退货/售后类短信
fun isReturnServiceSMS(smsContent: String): Boolean {
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

// 检查是否包含取件相关关键词
fun containsPickupRelatedKeywords(smsContent: String): Boolean {
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

// 提取信息的函数
fun extractParcelInfo(smsContent: String): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    
    // 检查是否包含特殊排除关键词
    if (containsSpecialExcludedKeywords(smsContent)) {
        return result
    }
    
    // 检查是否为物流状态更新短信
    if (isLogisticsStatusUpdate(smsContent)) {
        return result
    }
    
    // 检查是否为退货/售后类短信
    if (isReturnServiceSMS(smsContent)) {
        return result
    }
    
    // 检查是否包含取件相关关键词
    if (!containsPickupRelatedKeywords(smsContent)) {
        return result
    }
    
    // 提取所有取件码
    val codeMatches = ParcelInfoRegex.CODE_PATTERN.findAll(smsContent)
    // 过滤取件码，确保它们符合常见的取件码格式
    val pickupCodes = codeMatches.map { it.value }
        .filter { isValidPickupCode(it, smsContent) }
        .toList()
    
    if (pickupCodes.isNotEmpty()) {
        result["codes"] = pickupCodes
        result["code"] = pickupCodes[0] // 主取件码设为第一个
    }
    
    // 提取驿站 - 尝试多种模式，优先处理包含【】的情况，但只提取第一个【】中的内容
    var stationName: String? = null
    
    // 首先尝试匹配第一个【】中的内容
    val firstTagMatch = ParcelInfoRegex.TAG_PATTERNS[0].find(smsContent)
    if (firstTagMatch != null) {
        // 只提取第一个【】中的内容
        stationName = firstTagMatch.groupValues[1].trim()
    } else {
        // 如果没有【】标签，则使用传统方式匹配
        val stationMatch = ParcelInfoRegex.STATION_PATTERN.find(smsContent)
        stationName = stationMatch?.value
    }
    
    stationName?.let {
        result["station"] = it
    }
    
    // 提取地址 - 尝试多种模式，优先使用"地址:"关键词
    for (pattern in ParcelInfoRegex.ADDRESS_PATTERNS) {
        val addressMatch = pattern.find(smsContent)
        if (addressMatch != null) {
            val address = if (addressMatch.groupValues.size > 1) {
                // 优先使用第一个捕获组
                addressMatch.groupValues[1].trim()
            } else {
                // 否则使用整个匹配
                addressMatch.value.trim()
            }
            
            // 过滤掉太短或明显不正确的地址
            if (address.isNotEmpty() && address.length >= 2 && address.length <= 100 && 
                !address.contains("【") && !address.contains("...") && !address.contains("】") &&
                !address.startsWith("您有") && !address.startsWith("凭") && !address.startsWith("请") &&
                !address.contains("取件码") && !address.contains("验证码") && 
                !address.contains("联系") && !address.contains("有问题") &&
                !address.contains("电话") && !address.contains("快递员")) {
                // 额外检查，确保地址不包含驿站名称
                val station = result["station"]?.toString() ?: ""
                if (station.isEmpty() || !address.contains(station)) {
                    // 进一步处理地址，去除可能的多余内容
                    val refinedAddress = refineAddress(address)
                    result["address"] = refinedAddress
                    break
                } else if (address.length > station.length) {
                    // 如果地址包含了驿站名称，尝试去除驿站名称部分
                    val refinedAddress = address.replace(station, "").trim()
                    if (refinedAddress.isNotEmpty() && refinedAddress.length >= 2) {
                        result["address"] = refineAddress(refinedAddress)
                        break
                    }
                } else {
                    result["address"] = refineAddress(address)
                    break
                }
            }
        }
    }
    
    // 如果没有提取到地址，但提取到了驿站名称，则使用驿站名称作为地址
    if (!result.containsKey("address") && result.containsKey("station")) {
        // 检查驿站名称是否足够作为地址（避免使用过于简单的名称）
        val station = result["station"].toString()
        if (station.length >= 4) { // 只有当驿站名称足够长时才用作地址
            result["address"] = station
        }
    }
    
    return result
}

// 验证取件码是否有效的函数
fun isValidPickupCode(code: String, smsContent: String = ""): Boolean {
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

// 简化的地址清理函数（只做基本清理）
fun cleanAddress(address: String): String {
    var cleanedAddress = address
    
    // 只去除开头的冗余信息
    val redundantStartings = listOf(
        "到", "在", "于", "：", ":", " "
    )
    
    for (starting in redundantStartings) {
        if (cleanedAddress.startsWith(starting)) {
            cleanedAddress = cleanedAddress.substring(starting.length)
            break
        }
    }
    
    return cleanedAddress.trim()
}

/**
 * 精炼地址，去除可能的多余内容
 */
fun refineAddress(address: String): String {
    // 去除地址末尾的提示性文字
    var refined = address
    val promptEndings = listOf(
        ",请", "，请", "请您", "请", 
        "做好", "佩戴", "口罩", "个人", "防护",
        "及时", "取件", "联系", "电话"
    )
    
    // 循环去除末尾的提示性文字
    var changed = true
    while (changed) {
        changed = false
        for (ending in promptEndings) {
            if (refined.endsWith(ending)) {
                refined = refined.substring(0, refined.length - ending.length).trim()
                changed = true
                break
            }
        }
    }
    
    // 去除逗号或顿号后的内容（如"创业路小羊馆中通,请您..."）
    val commaIndex = refined.indexOfFirst { it == ',' || it == '，' }
    if (commaIndex > 0) {
        refined = refined.substring(0, commaIndex)
    }
    
    // 进一步优化地址清理，处理更多边界情况
    // 去除地址开头的冗余信息
    val redundantStartings = listOf(
        "到", "在", "于", "：", ":", " "
    )
    
    for (starting in redundantStartings) {
        if (refined.startsWith(starting)) {
            refined = refined.substring(starting.length).trim()
            break
        }
    }
    
    // 处理地址中的多余空格
    refined = refined.replace("\\s+".toRegex(), " ").trim()
    
    // 特殊处理：如果地址包含"【"和"】"，则只提取第一个【】中的内容
    val firstTagPattern = Regex("【([^】]+)】")
    val firstTagMatch = firstTagPattern.find(refined)
    if (firstTagMatch != null) {
        val tagContent = firstTagMatch.groupValues[1].trim()
        if (tagContent.isNotEmpty()) {
            refined = tagContent
        }
    }
    
    // 特殊处理：如果地址包含"（"和"）"，则移除括号及内容
    refined = refined.replace(Regex("（[^）]*）")) { "" }
    
    // 特殊处理：移除地址末尾的"取包裹"、"领取包裹"、"拿包裹"、"取快递"、"领取快递"、"拿快递"、"运单尾号\\d+包裹"等字样
    val pickupEndings = listOf("取包裹", "领取包裹", "拿包裹", "取快递", "领取快递", "拿快递")
    for (ending in pickupEndings) {
        if (refined.endsWith(ending)) {
            refined = refined.substring(0, refined.length - ending.length).trim()
        }
    }
    // 特殊处理运单尾号模式
    refined = refined.replace(Regex("运单尾号\\d+包裹.*$"), "").trim()
    
    // 确保地址不是太短
    if (refined.length < 2) {
        return "地址未知"
    }
    
    return refined.trim()
}
package com.qujianma.app

import java.util.regex.Pattern

/**
 * 通用信息解析工具类
 */
class GeneralParser {
    companion object {
        /**
         * 解析短信内容，提取通用信息
         * @param content 短信内容
         * @param sender 发送方
         * @param timestamp 时间戳
         * @return 如果是通用信息，返回GeneralInfo对象；否则返回null
         */
        fun parse(content: String, sender: String = "", timestamp: Long = 0): GeneralInfo? {
            // 银行类信息
            if (isBankInfo(content)) {
                return parseBankInfo(content, sender, timestamp)
            }
            
            // 社交媒体验证码
            if (isVerificationCode(content)) {
                return parseVerificationCode(content, sender, timestamp)
            }
            
            // 通知类信息
            if (isNotification(content)) {
                return parseNotification(content, sender, timestamp)
            }
            
            return null
        }
        
        /**
         * 判断是否是银行信息
         */
        private fun isBankInfo(content: String): Boolean {
            val keywords = arrayOf("银行", "账户", "余额", "转账", "支出", "收入", "消费", "储蓄卡", "信用卡", "借记卡", "ATM")
            return keywords.any { content.contains(it) }
        }
        
        /**
         * 解析银行信息
         */
        private fun parseBankInfo(content: String, sender: String, timestamp: Long): GeneralInfo {
            val title = "银行通知"
            
            // 提取金额
            val amountPattern = Pattern.compile("(?:支出|收入|消费|余额)[\\s:：]*([￥$]?[0-9,.]+(?:元)?)")
            val amountMatcher = amountPattern.matcher(content)
            val amount = if (amountMatcher.find()) amountMatcher.group(1) else ""
            
            // 提取账户后四位
            val accountPattern = Pattern.compile("(?:尾号|卡号)[\\s:：]*([0-9]{4})")
            val accountMatcher = accountPattern.matcher(content)
            val account = if (accountMatcher.find()) "尾号${accountMatcher.group(1)}" else ""
            
            val displayContent = when {
                amount.isNotEmpty() && account.isNotEmpty() -> "$account $amount"
                amount.isNotEmpty() -> amount
                account.isNotEmpty() -> account
                else -> content.take(50) // 取前50个字符
            }
            
            return GeneralInfo(
                title = title,
                content = displayContent,
                sender = sender,
                smsContent = content,
                timestamp = timestamp
            )
        }
        
        /**
         * 判断是否是验证码
         */
        private fun isVerificationCode(content: String): Boolean {
            val keywords = arrayOf("验证码", "校验码", "动态码", "安全码", "确认码")
            val codePattern = Pattern.compile("(?:验证码|校验码|动态码|安全码|确认码)[\\s:：]*([0-9]{4,8})")
            return keywords.any { content.contains(it) } || codePattern.matcher(content).find()
        }
        
        /**
         * 解析验证码
         */
        private fun parseVerificationCode(content: String, sender: String, timestamp: Long): GeneralInfo {
            val title = "验证码"
            
            // 提取验证码
            val codePattern = Pattern.compile("(?:验证码|校验码|动态码|安全码|确认码)[\\s:：]*([0-9]{4,8})")
            val codeMatcher = codePattern.matcher(content)
            val code = if (codeMatcher.find()) codeMatcher.group(1) else ""
            
            val displayContent = if (code.isNotEmpty()) "验证码 $code" else content.take(50)
            
            return GeneralInfo(
                title = title,
                content = displayContent,
                sender = sender,
                smsContent = content,
                timestamp = timestamp
            )
        }
        
        /**
         * 判断是否是通知类信息
         */
        private fun isNotification(content: String): Boolean {
            val keywords = arrayOf("通知", "提醒", "提示", "公告", "系统消息", "重要", "请及时")
            return keywords.any { content.contains(it) }
        }
        
        /**
         * 解析通知类信息
         */
        private fun parseNotification(content: String, sender: String, timestamp: Long): GeneralInfo {
            val title = "通知提醒"
            
            // 提取关键信息
            val keyInfo = when {
                content.contains("请及时") -> "请及时处理相关事宜"
                content.contains("重要") -> "重要通知"
                content.contains("提醒") -> "一般提醒"
                else -> content.take(50)
            }
            
            return GeneralInfo(
                title = title,
                content = keyInfo,
                sender = sender,
                smsContent = content,
                timestamp = timestamp
            )
        }
    }
}
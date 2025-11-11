package com.qujianma.app

import java.io.Serializable
import java.util.UUID

data class Rule(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var ruleType: String = "custom", // "regex" 或 "custom"
    var description: String = "",
    // 正则规则字段
    var pattern: String = "",
    // 自定义前后缀规则字段
    var tagPrefix: String = "",  // 服务端暂未使用但App端保留
    var tagSuffix: String = "",  // 服务端暂未使用但App端保留
    var phonePrefix: String = "",  // 服务端暂未使用但App端保留
    var phoneSuffix: String = "",  // 服务端暂未使用但App端保留
    var codePrefix: String = "",
    var codeSuffix: String = "",
    var addressPrefix: String = "",
    var addressSuffix: String = "",
    var stationPrefix: String = "",
    var stationSuffix: String = "",
    var enabled: Boolean = true
) : Serializable
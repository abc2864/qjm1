<?php
// 数据库更新脚本，用于添加对正则规则和自定义前后缀规则的支持

// 包含配置文件
require_once 'config.php';

echo "正在更新数据库结构以支持增强的规则功能...\n";

try {
    // 创建数据库连接
    $pdo = getDBConnection();
    
    // 添加新字段到rules表
    echo "1. 更新rules表结构...\n";
    
    // 检查是否已存在rule_type字段
    $stmt = $pdo->query("SHOW COLUMNS FROM rules LIKE 'rule_type'");
    if (!$stmt->fetch()) {
        // 添加rule_type字段
        $pdo->exec("ALTER TABLE rules ADD COLUMN rule_type ENUM('regex', 'custom') NOT NULL DEFAULT 'regex' AFTER name");
        echo "   ✓ 添加rule_type字段\n";
    } else {
        echo "   ✓ rule_type字段已存在\n";
    }
    
    // 检查是否已存在自定义前后缀字段
    $custom_fields = [
        'sender_prefix' => 'VARCHAR(100)',
        'sender_suffix' => 'VARCHAR(100)',
        'code_prefix' => 'VARCHAR(100)',
        'code_suffix' => 'VARCHAR(100)',
        'station_prefix' => 'VARCHAR(100)',
        'station_suffix' => 'VARCHAR(100)',
        'address_prefix' => 'VARCHAR(100)',
        'address_suffix' => 'VARCHAR(100)'
    ];
    
    foreach ($custom_fields as $field => $type) {
        $stmt = $pdo->query("SHOW COLUMNS FROM rules LIKE '$field'");
        if (!$stmt->fetch()) {
            $pdo->exec("ALTER TABLE rules ADD COLUMN $field $type AFTER pattern");
            echo "   ✓ 添加$field字段\n";
        } else {
            echo "   ✓ $field字段已存在\n";
        }
    }
    
    // 更新现有规则的rule_type为'regex'
    $stmt = $pdo->prepare("UPDATE rules SET rule_type = 'regex' WHERE rule_type IS NULL OR rule_type = ''");
    $stmt->execute();
    echo "   ✓ 更新现有规则的rule_type为'regex'\n";
    
    // 插入示例自定义规则（如果不存在）
    $stmt = $pdo->prepare("SELECT COUNT(*) as count FROM rules WHERE name = '自定义规则示例'");
    $stmt->execute();
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($result['count'] == 0) {
        $stmt = $pdo->prepare("INSERT INTO rules (name, rule_type, sender_prefix, sender_suffix, code_prefix, code_suffix, station_prefix, station_suffix, address_prefix, address_suffix, description, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([
            '自定义规则示例',
            'custom',
            '【',
            '】',
            '取件码:',
            '到',
            '地址:',
            '，',
            '凭',
            '来取',
            '自定义前后缀规则示例',
            1
        ]);
        echo "   ✓ 插入示例自定义规则\n";
    } else {
        echo "   ✓ 示例自定义规则已存在\n";
    }
    
    echo "\n数据库更新完成！\n";
    echo "现在支持两种规则类型：\n";
    echo "1. 正则规则 (regex) - 使用正则表达式匹配\n";
    echo "2. 自定义前后缀规则 (custom) - 使用前后缀匹配\n";
    
} catch (PDOException $e) {
    echo "数据库更新失败: " . $e->getMessage() . "\n";
}
?>
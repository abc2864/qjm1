<?php
// 数据库更新脚本 - 添加API密钥支持
require_once 'config.php';

try {
    $pdo = getDBConnection();
    
    // 为用户表添加API密钥字段
    $pdo->exec("ALTER TABLE users ADD COLUMN api_key VARCHAR(64) UNIQUE AFTER password");
    echo "✓ 已添加api_key字段到users表\n";
    
    // 为用户表添加API密钥创建时间字段
    $pdo->exec("ALTER TABLE users ADD COLUMN api_key_created_at TIMESTAMP NULL AFTER api_key");
    echo "✓ 已添加api_key_created_at字段到users表\n";
    
    echo "\n数据库更新完成！\n";
    echo "现在用户表支持API密钥功能\n";
    
} catch (PDOException $e) {
    // 如果字段已存在，会抛出异常，我们忽略这个错误
    if (strpos($e->getMessage(), 'Duplicate column name') !== false) {
        echo "✓ API密钥字段已存在\n";
    } else {
        echo "数据库更新失败: " . $e->getMessage() . "\n";
        exit(1);
    }
}
?>
<?php
// 数据库更新脚本 V3 - 添加API密钥支持
require_once 'config.php';

echo "正在更新数据库以支持API密钥功能...\n\n";

try {
    $pdo = getDBConnection();
    
    // 为用户表添加API密钥字段
    try {
        $pdo->exec("ALTER TABLE users ADD COLUMN api_key VARCHAR(64) UNIQUE");
        echo "✓ 已添加api_key字段到users表\n";
    } catch (PDOException $e) {
        if (strpos($e->getMessage(), 'Duplicate column name') !== false) {
            echo "✓ api_key字段已存在\n";
        } else {
            throw $e;
        }
    }
    
    // 为用户表添加API密钥创建时间字段
    try {
        $pdo->exec("ALTER TABLE users ADD COLUMN api_key_created_at TIMESTAMP NULL");
        echo "✓ 已添加api_key_created_at字段到users表\n";
    } catch (PDOException $e) {
        if (strpos($e->getMessage(), 'Duplicate column name') !== false) {
            echo "✓ api_key_created_at字段已存在\n";
        } else {
            throw $e;
        }
    }
    
    echo "\n数据库更新完成！\n";
    echo "现在支持API密钥认证功能\n";
    echo "\n请执行以下操作：\n";
    echo "1. 登录Web管理系统\n";
    echo "2. 在主页点击'API密钥管理'\n";
    echo "3. 生成新的API密钥\n";
    echo "4. 在App端配置使用该API密钥\n";
    
} catch (PDOException $e) {
    echo "数据库更新失败: " . $e->getMessage() . "\n";
    exit(1);
}
?>
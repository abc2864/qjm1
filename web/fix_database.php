<?php
// 数据库修复脚本

// 包含配置文件
require_once 'config.php';

try {
    // 创建数据库连接（不指定数据库）
    $pdo = new PDO("mysql:host=" . DB_HOST . ";charset=utf8mb4", DB_USER, DB_PASS);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // 创建数据库
    $pdo->exec("CREATE DATABASE IF NOT EXISTS " . DB_NAME . " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    echo "数据库创建成功或已存在\n";
    
    // 使用数据库
    $pdo->exec("USE " . DB_NAME);
    
    // 创建用户表
    $pdo->exec("CREATE TABLE IF NOT EXISTS users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(50) NOT NULL UNIQUE,
        password VARCHAR(255) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )");
    echo "用户表创建成功或已存在\n";
    
    // 创建规则表
    $pdo->exec("CREATE TABLE IF NOT EXISTS rules (
        id INT AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        pattern TEXT NOT NULL,
        description TEXT,
        is_active TINYINT(1) DEFAULT 1,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    )");
    echo "规则表创建成功或已存在\n";
    
    // 创建关键词表
    $pdo->exec("CREATE TABLE IF NOT EXISTS keywords (
        id INT AUTO_INCREMENT PRIMARY KEY,
        keyword VARCHAR(100) NOT NULL UNIQUE,
        type ENUM('sender', 'content') NOT NULL DEFAULT 'content',
        description TEXT,
        is_active TINYINT(1) DEFAULT 1,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    )");
    echo "关键词表创建成功或已存在\n";
    
    // 检查是否已存在用户
    $stmt = $pdo->prepare("SELECT COUNT(*) as count FROM users");
    $stmt->execute();
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($result['count'] == 0) {
        // 插入默认管理员用户 (用户名: admin, 密码: admin123)
        $hashedPassword = password_hash('admin123', PASSWORD_DEFAULT);
        $stmt = $pdo->prepare("INSERT INTO users (username, password) VALUES (?, ?)");
        $stmt->execute(['admin', $hashedPassword]);
        echo "默认管理员用户已创建\n";
    } else {
        echo "用户已存在，跳过创建\n";
    }
    
    echo "\n数据库修复完成！\n";
    echo "您可以使用以下凭据登录：\n";
    echo "用户名: admin\n";
    echo "密码: admin123\n";
    
} catch (PDOException $e) {
    echo "数据库操作失败: " . $e->getMessage() . "\n";
}
?>
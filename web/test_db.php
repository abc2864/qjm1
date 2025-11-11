<?php
// 数据库连接测试脚本

// 包含配置文件
require_once 'config.php';

try {
    // 创建数据库连接
    $pdo = getDBConnection();
    
    // 测试查询
    $stmt = $pdo->query("SELECT COUNT(*) as count FROM users");
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    echo "数据库连接成功！\n";
    echo "用户表中的用户数量: " . $result['count'] . "\n";
    
    // 查询用户信息
    $stmt = $pdo->query("SELECT id, username, created_at FROM users");
    $users = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo "\n用户列表:\n";
    foreach ($users as $user) {
        echo "ID: " . $user['id'] . ", 用户名: " . $user['username'] . ", 创建时间: " . $user['created_at'] . "\n";
    }
    
    // 测试密码验证
    $test_username = 'admin';
    $test_password = 'admin123';
    
    $stmt = $pdo->prepare("SELECT id, username, password FROM users WHERE username = ?");
    $stmt->execute([$test_username]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($user) {
        echo "\n测试用户 '$test_username' 存在\n";
        if (password_verify($test_password, $user['password'])) {
            echo "密码验证成功！\n";
        } else {
            echo "密码验证失败！\n";
        }
    } else {
        echo "\n测试用户 '$test_username' 不存在\n";
    }
    
} catch (PDOException $e) {
    echo "数据库连接失败: " . $e->getMessage() . "\n";
}
?>
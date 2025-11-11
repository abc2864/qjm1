<?php
// 重置管理员用户脚本

// 包含配置文件
require_once 'config.php';

echo "正在重置管理员用户...\n";

try {
    // 创建数据库连接
    $pdo = getDBConnection();
    
    // 检查是否已存在admin用户
    $stmt = $pdo->prepare("SELECT id, username FROM users WHERE username = ?");
    $stmt->execute(['admin']);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    
    // 默认凭据
    $default_username = 'admin';
    $default_password = 'admin123';
    $hashed_password = password_hash($default_password, PASSWORD_DEFAULT);
    
    if ($user) {
        // 用户存在，更新密码
        $stmt = $pdo->prepare("UPDATE users SET password = ? WHERE username = ?");
        $stmt->execute([$hashed_password, $default_username]);
        echo "管理员用户密码已重置!\n";
    } else {
        // 用户不存在，创建新用户
        $stmt = $pdo->prepare("INSERT INTO users (username, password) VALUES (?, ?)");
        $stmt->execute([$default_username, $hashed_password]);
        echo "管理员用户已创建!\n";
    }
    
    echo "\n用户信息:\n";
    echo "用户名: " . $default_username . "\n";
    echo "密码: " . $default_password . "\n";
    echo "\n请使用以上凭据登录系统。\n";
    echo "登录后请立即修改密码以确保安全。\n";
    
} catch (PDOException $e) {
    echo "操作失败: " . $e->getMessage() . "\n";
    
    // 如果是数据库连接问题，提供额外的诊断信息
    if (strpos($e->getMessage(), 'Access denied') !== false) {
        echo "\n数据库连接被拒绝，请检查以下配置:\n";
        echo "- 数据库主机: " . DB_HOST . "\n";
        echo "- 数据库用户: " . DB_USER . "\n";
        echo "- 数据库密码: " . (empty(DB_PASS) ? "(空)" : "(已设置)") . "\n";
        echo "- 数据库名称: " . DB_NAME . "\n";
    }
}
?>
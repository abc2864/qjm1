<?php
// 登录问题诊断脚本

// 包含配置文件
require_once 'config.php';

echo "=== 登录问题诊断工具 ===\n\n";

// 1. 测试数据库连接
echo "1. 测试数据库连接...\n";
try {
    $pdo = getDBConnection();
    echo "   ✓ 数据库连接成功\n";
} catch (PDOException $e) {
    echo "   ✗ 数据库连接失败: " . $e->getMessage() . "\n";
    exit(1);
}

// 2. 检查表是否存在
echo "\n2. 检查用户表...\n";
try {
    $stmt = $pdo->query("SHOW TABLES LIKE 'users'");
    $tableExists = $stmt->fetch();
    
    if ($tableExists) {
        echo "   ✓ 用户表存在\n";
    } else {
        echo "   ✗ 用户表不存在\n";
        exit(1);
    }
} catch (PDOException $e) {
    echo "   ✗ 检查用户表时出错: " . $e->getMessage() . "\n";
    exit(1);
}

// 3. 检查用户记录
echo "\n3. 检查用户记录...\n";
try {
    $stmt = $pdo->query("SELECT COUNT(*) as count FROM users");
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    echo "   用户表中的记录数: " . $result['count'] . "\n";
    
    if ($result['count'] > 0) {
        echo "   ✓ 用户记录存在\n";
        
        // 显示用户信息
        $stmt = $pdo->query("SELECT id, username, created_at FROM users");
        $users = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        foreach ($users as $user) {
            echo "   - ID: " . $user['id'] . ", 用户名: " . $user['username'] . ", 创建时间: " . $user['created_at'] . "\n";
        }
    } else {
        echo "   ✗ 用户表为空\n";
    }
} catch (PDOException $e) {
    echo "   ✗ 检查用户记录时出错: " . $e->getMessage() . "\n";
    exit(1);
}

// 4. 测试密码验证
echo "\n4. 测试密码验证...\n";
$test_username = 'admin';
$test_password = 'admin123';

try {
    $stmt = $pdo->prepare("SELECT id, username, password FROM users WHERE username = ?");
    $stmt->execute([$test_username]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($user) {
        echo "   找到用户: " . $user['username'] . "\n";
        
        if (password_verify($test_password, $user['password'])) {
            echo "   ✓ 密码验证成功\n";
            echo "\n=== 诊断结果 ===\n";
            echo "数据库连接正常，用户记录存在，密码验证成功。\n";
            echo "请检查以下可能的问题：\n";
            echo "1. 确保在登录页面输入的用户名和密码完全正确\n";
            echo "2. 检查是否有大小写问题\n";
            echo "3. 检查键盘布局是否正确（如Caps Lock是否开启）\n";
        } else {
            echo "   ✗ 密码验证失败\n";
            echo "\n=== 诊断结果 ===\n";
            echo "用户存在但密码不正确，建议重置密码。\n";
            echo "运行 reset_admin_user.bat 或 php reset_admin_user.php 重置密码。\n";
        }
    } else {
        echo "   ✗ 未找到用户: " . $test_username . "\n";
        echo "\n=== 诊断结果 ===\n";
        echo "未找到管理员用户，建议创建用户。\n";
        echo "运行 reset_admin_user.bat 或 php reset_admin_user.php 创建用户。\n";
    }
} catch (PDOException $e) {
    echo "   ✗ 密码验证时出错: " . $e->getMessage() . "\n";
    exit(1);
}

echo "\n诊断完成。\n";
?>
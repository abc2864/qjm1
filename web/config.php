<?php
// 数据库配置
define('DB_HOST', '8.137.57.54');
define('DB_USER', 'root');
define('DB_PASS', '');
define('DB_NAME', 'pickup_code_db');

// 创建数据库连接
function getDBConnection() {
    try {
        $pdo = new PDO("mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4", DB_USER, DB_PASS);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        return $pdo;
    } catch(PDOException $e) {
        die("数据库连接失败: " . $e->getMessage());
    }
}

// 启动会话
session_start();

// 检查用户是否已登录
function isLoggedIn() {
    return isset($_SESSION['user_id']) && isset($_SESSION['username']);
}

// 验证API密钥
function isValidApiKey($apiKey) {
    if (empty($apiKey)) {
        return false;
    }
    
    try {
        $pdo = getDBConnection();
        $stmt = $pdo->prepare("SELECT id, username FROM users WHERE api_key = ?");
        $stmt->execute([$apiKey]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if ($user) {
            return true;
        }
    } catch (PDOException $e) {
        // 数据库错误
        return false;
    }
    
    return false;
}

// 生成新的API密钥
function generateApiKey() {
    return bin2hex(random_bytes(32)); // 生成64字符的十六进制字符串
}

// 获取用户的API密钥
function getUserApiKey($userId) {
    try {
        $pdo = getDBConnection();
        $stmt = $pdo->prepare("SELECT api_key, api_key_created_at FROM users WHERE id = ?");
        $stmt->execute([$userId]);
        return $stmt->fetch(PDO::FETCH_ASSOC);
    } catch (PDOException $e) {
        return false;
    }
}

// 为用户生成并保存API密钥
function generateAndSaveApiKey($userId) {
    $apiKey = generateApiKey();
    
    try {
        $pdo = getDBConnection();
        $stmt = $pdo->prepare("UPDATE users SET api_key = ?, api_key_created_at = NOW() WHERE id = ?");
        $stmt->execute([$apiKey, $userId]);
        return $apiKey;
    } catch (PDOException $e) {
        return false;
    }
}

// 删除用户的API密钥
function deleteApiKey($userId) {
    try {
        $pdo = getDBConnection();
        $stmt = $pdo->prepare("UPDATE users SET api_key = NULL, api_key_created_at = NULL WHERE id = ?");
        $stmt->execute([$userId]);
        return true;
    } catch (PDOException $e) {
        return false;
    }
}

// 重定向到登录页面
function redirectToLogin() {
    header('Location: login.php');
    exit();
}

// 重定向到主页
function redirectToHome() {
    header('Location: index.php');
    exit();
}

// 用户登出
function logout() {
    session_destroy();
    redirectToLogin();
}
?>
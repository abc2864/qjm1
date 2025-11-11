<?php
require_once 'config.php';

// 首先检查API密钥
$apiKey = $_SERVER['HTTP_X_API_KEY'] ?? $_GET['api_key'] ?? '';

// 检查是否提供了API密钥
if (!empty($apiKey)) {
    // 验证API密钥
    if (!isValidApiKey($apiKey)) {
        header('Content-Type: application/json');
        echo json_encode(['success' => false, 'message' => '无效的API密钥']);
        exit;
    }
    // API密钥验证通过，继续执行
} else {
    // 检查用户是否已登录（原有的会话验证）
    if (!isLoggedIn()) {
        header('Content-Type: application/json');
        echo json_encode(['success' => false, 'message' => '未登录或未提供API密钥']);
        exit;
    }
}

$action = $_GET['action'] ?? '';

try {
    $pdo = getDBConnection();
    
    if ($action === 'get_rules') {
        // 获取所有启用的规则和关键词
        $stmt = $pdo->query("SELECT * FROM rules WHERE is_active = 1 ORDER BY id DESC");
        $rules = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        $stmt = $pdo->query("SELECT * FROM keywords WHERE is_active = 1 ORDER BY id DESC");
        $keywords = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        // 调整规则数据结构以匹配App端的Rule类
        $formattedRules = array_map(function($rule) {
            return [
                'id' => $rule['id'],
                'name' => $rule['name'],
                'ruleType' => $rule['rule_type'],
                'description' => $rule['description'] ?? '',
                'pattern' => $rule['pattern'] ?? '',
                'tagPrefix' => '',  // App端有但数据库中没有的字段，设为空字符串
                'tagSuffix' => '',  // App端有但数据库中没有的字段，设为空字符串
                'phonePrefix' => '',  // App端有但数据库中没有的字段，设为空字符串
                'phoneSuffix' => '',  // App端有但数据库中没有的字段，设为空字符串
                'codePrefix' => $rule['code_prefix'] ?? '',
                'codeSuffix' => $rule['code_suffix'] ?? '',
                'addressPrefix' => $rule['address_prefix'] ?? '',
                'addressSuffix' => $rule['address_suffix'] ?? '',
                'stationPrefix' => $rule['station_prefix'] ?? '',
                'stationSuffix' => $rule['station_suffix'] ?? '',
                'enabled' => (bool)($rule['is_active'] ?? false)
            ];
        }, $rules);
        
        header('Content-Type: application/json');
        echo json_encode([
            'success' => true, 
            'version' => date('Y-m-d H:i:s'),
            'rules' => $formattedRules,
            'keywords' => $keywords
        ]);
        exit;
    }
    
    header('Content-Type: application/json');
    echo json_encode(['success' => false, 'message' => '未知操作']);
    
} catch (PDOException $e) {
    header('Content-Type: application/json');
    echo json_encode(['success' => false, 'message' => '数据库错误: ' . $e->getMessage()]);
}
?>
<?php
// 测试API接口 - 查看返回的数据格式
require_once 'config.php';

try {
    $pdo = getDBConnection();
    
    // 获取启用的规则（支持正则规则和自定义前后缀规则）
    $stmt = $pdo->query("SELECT * FROM rules WHERE is_active = 1 ORDER BY id");
    $rules = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // 获取启用的关键词
    $stmt = $pdo->query("SELECT id, keyword, type, description FROM keywords WHERE is_active = 1 ORDER BY id");
    $keywords = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // 构造JSON数据
    $data = [
        'version' => date('Y-m-d H:i:s'),
        'rules' => $rules,
        'keywords' => $keywords
    ];
    
    // 输出JSON数据
    echo "<pre>";
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    echo "</pre>";
} catch (PDOException $e) {
    http_response_code(500);
    echo "数据库查询失败: " . $e->getMessage();
} catch (Exception $e) {
    http_response_code(500);
    echo "服务器错误: " . $e->getMessage();
}
?>
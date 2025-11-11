<?php
require_once 'config.php';

// 检查用户是否已登录
if (!isLoggedIn()) {
    redirectToLogin();
}

try {
    $pdo = getDBConnection();
    
    // 获取所有启用的规则
    $stmt = $pdo->query("SELECT * FROM rules WHERE is_active = 1 ORDER BY id ASC");
    $rules = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // 获取所有启用的关键词
    $stmt = $pdo->query("SELECT * FROM keywords WHERE is_active = 1 ORDER BY type, keyword");
    $keywords = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // 构造导出数据
    $exportData = [
        'version' => date('Y-m-d H:i:s'),
        'rules' => [],
        'keywords' => []
    ];
    
    // 处理规则数据
    foreach ($rules as $rule) {
        $ruleData = [
            'id' => $rule['id'],
            'name' => $rule['name'],
            'description' => $rule['description']
        ];
        
        if ($rule['rule_type'] === 'regex') {
            // 正则规则
            $ruleData['rule_type'] = 'regex';
            $ruleData['pattern'] = $rule['pattern'];
        } else {
            // 自定义前后缀规则
            $ruleData['rule_type'] = 'custom';
            $ruleData['code_prefix'] = $rule['code_prefix'];
            $ruleData['code_suffix'] = $rule['code_suffix'];
            $ruleData['station_prefix'] = $rule['station_prefix'];
            $ruleData['station_suffix'] = $rule['station_suffix'];
            $ruleData['address_prefix'] = $rule['address_prefix'];
            $ruleData['address_suffix'] = $rule['address_suffix'];
        }
        
        $exportData['rules'][] = $ruleData;
    }
    
    // 处理关键词数据
    foreach ($keywords as $keyword) {
        $exportData['keywords'][] = [
            'id' => $keyword['id'],
            'keyword' => $keyword['keyword'],
            'type' => $keyword['type'],
            'description' => $keyword['description']
        ];
    }
    
    // 设置响应头以下载JSON文件
    header('Content-Type: application/json');
    header('Content-Disposition: attachment; filename="pickup_code_rules_' . date('YmdHis') . '.json"');
    
    // 输出JSON数据
    echo json_encode($exportData, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    
} catch (PDOException $e) {
    // 如果发生错误，显示错误页面
    header('Content-Type: text/html; charset=utf-8');
    echo '<!DOCTYPE html>
    <html lang="zh-CN">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>导出失败</title>
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    </head>
    <body>
        <div class="container mt-4">
            <div class="alert alert-danger" role="alert">
                <h4 class="alert-heading">导出失败</h4>
                <p>导出过程中发生错误: ' . htmlspecialchars($e->getMessage()) . '</p>
                <hr>
                <a href="index.php" class="btn btn-primary">返回首页</a>
            </div>
        </div>
    </body>
    </html>';
}
?>

<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>导出JSON - 取件码规则管理系统</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.7.2/font/bootstrap-icons.css" rel="stylesheet">
    <link href="mobile.css" rel="stylesheet">
</head>
<body>
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
        <div class="container">
            <a class="navbar-brand" href="index.php">取件码规则管理系统</a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav" aria-controls="navbarNav" aria-expanded="false" aria-label="Toggle navigation">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarNav">
                <div class="navbar-nav">
                    <a class="nav-link" href="rules.php">规则管理</a>
                    <a class="nav-link" href="keywords.php">关键词管理</a>
                    <a class="nav-link active" href="export.php">导出JSON</a>
                    <a class="nav-link" href="api.php?action=get_rules" target="_blank">API接口</a>
                    <a class="nav-link" href="api_keys.php">API密钥</a>
                    <a class="nav-link" href="info.php">系统信息</a>
                </div>
                <div class="navbar-nav ms-auto">
                    <span class="navbar-text me-3 d-none d-lg-inline">欢迎, <?php echo htmlspecialchars($_SESSION['username']); ?></span>
                    <a class="nav-link" href="change_password.php">修改密码</a>
                    <a class="nav-link" href="logout.php">登出</a>
                </div>
                <div class="navbar-text d-lg-none user-info-mobile">
                    欢迎, <?php echo htmlspecialchars($_SESSION['username']); ?>
                </div>
            </div>
        </div>
    </nav>

    <div class="container mt-4">
        <div class="row">
            <div class="col-md-12">
                <h2>导出JSON数据</h2>
                
                <?php if ($message): ?>
                    <div class="alert alert-info"><?php echo htmlspecialchars($message); ?></div>
                <?php endif; ?>
                
                <div class="card mb-4">
                    <div class="card-header">
                        <h5>导出信息</h5>
                    </div>
                    <div class="card-body">
                        <p>点击下面的按钮可以将当前的规则和关键词数据导出为JSON格式，供App端下载使用。</p>
                        <div class="alert alert-info">
                            <strong>导出说明：</strong>导出的数据仅包含启用状态的规则和关键词，导出后会自动保存为rules_data.json文件。
                        </div>
                        <a href="rules_data.json" class="btn btn-primary" download>
                            <i class="bi bi-download"></i> 下载JSON文件
                        </a>
                        <a href="api.php?action=get_rules" class="btn btn-success" target="_blank">
                            <i class="bi bi-link"></i> API接口地址
                        </a>
                    </div>
                </div>
                
                <div class="card">
                    <div class="card-header">
                        <h5>JSON数据预览</h5>
                    </div>
                    <div class="card-body">
                        <?php if ($json_data): ?>
                            <pre><code class="language-json"><?php echo htmlspecialchars($json_data); ?></code></pre>
                        <?php else: ?>
                            <p>暂无数据可显示</p>
                        <?php endif; ?>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
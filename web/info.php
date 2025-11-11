<?php
require_once 'config.php';

// 检查用户是否已登录
if (!isLoggedIn()) {
    redirectToLogin();
}

// 获取统计信息
try {
    $pdo = getDBConnection();
    
    // 规则总数
    $stmt = $pdo->query("SELECT COUNT(*) as total FROM rules");
    $rules_count = $stmt->fetch(PDO::FETCH_ASSOC)['total'];
    
    // 启用规则数
    $stmt = $pdo->query("SELECT COUNT(*) as active FROM rules WHERE is_active = 1");
    $active_rules_count = $stmt->fetch(PDO::FETCH_ASSOC)['active'];
    
    // 关键词总数
    $stmt = $pdo->query("SELECT COUNT(*) as total FROM keywords");
    $keywords_count = $stmt->fetch(PDO::FETCH_ASSOC)['total'];
    
    // 启用关键词数
    $stmt = $pdo->query("SELECT COUNT(*) as active FROM keywords WHERE is_active = 1");
    $active_keywords_count = $stmt->fetch(PDO::FETCH_ASSOC)['active'];
    
} catch (PDOException $e) {
    $rules_count = 0;
    $active_rules_count = 0;
    $keywords_count = 0;
    $active_keywords_count = 0;
}
?>

<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>系统信息 - 取件码规则管理系统</title>
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
                    <a class="nav-link" href="export.php">导出JSON</a>
                    <a class="nav-link" href="api.php?action=get_rules" target="_blank">API接口</a>
                    <a class="nav-link" href="api_keys.php">API密钥</a>
                    <a class="nav-link active" href="info.php">系统信息</a>
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
                <h2>系统信息</h2>
                
                <div class="row">
                    <div class="col-md-6 mb-4">
                        <div class="card">
                            <div class="card-header">
                                <h5>统计信息</h5>
                            </div>
                            <div class="card-body">
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <div class="border rounded p-3 text-center">
                                            <div class="fs-4"><?php echo $rules_count; ?></div>
                                            <div>总规则数</div>
                                        </div>
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <div class="border rounded p-3 text-center">
                                            <div class="fs-4"><?php echo $active_rules_count; ?></div>
                                            <div>启用规则数</div>
                                        </div>
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <div class="border rounded p-3 text-center">
                                            <div class="fs-4"><?php echo $keywords_count; ?></div>
                                            <div>总关键词数</div>
                                        </div>
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <div class="border rounded p-3 text-center">
                                            <div class="fs-4"><?php echo $active_keywords_count; ?></div>
                                            <div>启用关键词数</div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="col-md-6 mb-4">
                        <div class="card">
                            <div class="card-header">
                                <h5>使用说明</h5>
                            </div>
                            <div class="card-body">
                                <ol>
                                    <li>在"规则管理"中添加或编辑取件码解析规则</li>
                                    <li>在"关键词管理"中管理发件人和内容关键词</li>
                                    <li>确保规则和关键词处于启用状态</li>
                                    <li>定期导出JSON数据以供App端使用</li>
                                </ol>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function copyToClipboard(text) {
            navigator.clipboard.writeText(text).then(() => {
                alert('已复制到剪贴板');
            }).catch(err => {
                console.error('复制失败: ', err);
            });
        }
    </script>
</body>
</html>
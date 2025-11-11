<?php
require_once 'config.php';

// 检查用户是否已登录
if (!isLoggedIn()) {
    redirectToLogin();
}

$message = '';
$userApiKey = null;
$apiKeyCreatedAt = null;

// 获取当前用户的API密钥信息
$apiKeyInfo = getUserApiKey($_SESSION['user_id']);
if ($apiKeyInfo && $apiKeyInfo['api_key']) {
    $userApiKey = $apiKeyInfo['api_key'];
    $apiKeyCreatedAt = $apiKeyInfo['api_key_created_at'];
}

// 处理表单提交
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (isset($_POST['action'])) {
        switch ($_POST['action']) {
            case 'generate':
                // 生成新的API密钥
                $newApiKey = generateAndSaveApiKey($_SESSION['user_id']);
                if ($newApiKey) {
                    $userApiKey = $newApiKey;
                    $message = '新的API密钥已生成';
                } else {
                    $message = '生成API密钥失败';
                }
                break;
                
            case 'delete':
                // 删除API密钥
                if (deleteApiKey($_SESSION['user_id'])) {
                    $userApiKey = null;
                    $apiKeyCreatedAt = null;
                    $message = 'API密钥已删除';
                } else {
                    $message = '删除API密钥失败';
                }
                break;
        }
    }
}
?>

<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>API密钥管理 - 取件码规则管理系统</title>
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
                    <a class="nav-link active" href="api_keys.php">API密钥</a>
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
                <h2>API密钥管理</h2>
                
                <?php if ($message): ?>
                    <div class="alert alert-info"><?php echo htmlspecialchars($message); ?></div>
                <?php endif; ?>
                
                <div class="card mb-4">
                    <div class="card-header">
                        <h5>API密钥信息</h5>
                    </div>
                    <div class="card-body">
                        <?php if ($userApiKey): ?>
                            <div class="mb-3">
                                <label class="form-label">您的API密钥:</label>
                                <div class="input-group">
                                    <input type="text" class="form-control" id="api_key" value="<?php echo htmlspecialchars($userApiKey); ?>" readonly>
                                    <button class="btn btn-outline-secondary" type="button" onclick="copyToClipboard(document.getElementById('api_key').value)">
                                        <i class="bi bi-clipboard"></i> 复制
                                    </button>
                                </div>
                                <?php if ($apiKeyCreatedAt): ?>
                                    <div class="form-text">
                                        创建时间: <?php echo htmlspecialchars($apiKeyCreatedAt); ?>
                                    </div>
                                <?php endif; ?>
                            </div>
                            
                            <form method="POST" class="d-inline">
                                <input type="hidden" name="action" value="delete">
                                <button type="submit" class="btn btn-danger" onclick="return confirm('确定要删除当前API密钥吗？删除后需要重新生成才能使用API。')">
                                    <i class="bi bi-trash"></i> 删除API密钥
                                </button>
                            </form>
                        <?php else: ?>
                            <p>您当前没有API密钥。</p>
                            <form method="POST">
                                <input type="hidden" name="action" value="generate">
                                <button type="submit" class="btn btn-primary">
                                    <i class="bi bi-key"></i> 生成API密钥
                                </button>
                            </form>
                        <?php endif; ?>
                    </div>
                </div>
                
                <div class="card">
                    <div class="card-header">
                        <h5>API使用说明</h5>
                    </div>
                    <div class="card-body">
                        <h6>如何使用API密钥</h6>
                        <p>您可以通过以下两种方式之一使用API密钥访问API接口：</p>
                        
                        <ol>
                            <li><strong>HTTP头部方式（推荐）</strong>:
                                <pre class="bg-light p-2">X-API-Key: <?php echo $userApiKey ? htmlspecialchars($userApiKey) : 'YOUR_API_KEY'; ?></pre>
                            </li>
                            <li><strong>URL参数方式</strong>:
                                <pre class="bg-light p-2">GET /api.php?action=get_rules&api_key=<?php echo $userApiKey ? htmlspecialchars($userApiKey) : 'YOUR_API_KEY'; ?></pre>
                            </li>
                        </ol>
                        
                        <h6>API接口</h6>
                        <p>当前可用的API接口:</p>
                        <ul>
                            <li><code>GET /api.php?action=get_rules</code> - 获取所有启用的规则和关键词</li>
                        </ul>
                        
                        <h6>安全性建议</h6>
                        <ul>
                            <li>请妥善保管您的API密钥，不要在公开代码中暴露</li>
                            <li>如果怀疑API密钥泄露，请立即删除并生成新的API密钥</li>
                            <li>推荐使用HTTP头部方式传递API密钥，避免密钥出现在URL中</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function copyToClipboard(text) {
            navigator.clipboard.writeText(text).then(() => {
                alert('API密钥已复制到剪贴板');
            }).catch(err => {
                console.error('复制失败: ', err);
            });
        }
    </script>
</body>
</html>
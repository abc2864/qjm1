<?php
require_once 'config.php';

// 检查用户是否已登录
if (!isLoggedIn()) {
    redirectToLogin();
}
?>

<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>取件码规则管理系统</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.7.2/font/bootstrap-icons.css" rel="stylesheet">
    <link href="mobile.css" rel="stylesheet">
    <style>
        .feature-card {
            transition: transform 0.2s;
            height: 100%;
        }
        .feature-card:hover {
            transform: translateY(-5px);
        }
        .card-icon {
            font-size: 2rem;
            color: #0d6efd;
        }
    </style>
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
                    <a class="nav-link" href="batch_rules.php">批量规则</a>
                    <a class="nav-link" href="keywords.php">关键词管理</a>
                    <a class="nav-link" href="export.php">导出JSON</a>
                    <a class="nav-link" href="api.php?action=get_rules" target="_blank">API接口</a>
                    <a class="nav-link" href="api_keys.php">API密钥</a>
                    <a class="nav-link" href="info.php">系统信息</a>
                </div>
                <div class="navbar-nav ms-auto">
                    <span class="navbar-text me-3 d-none d-lg-inline">欢迎, <?php echo htmlspecialchars($_SESSION['username']); ?></span>
                    <a class="nav-link" href="change_password.php">修改密码</a>
                    <?php if ($_SESSION['username'] === 'admin'): ?>
                        <a class="nav-link d-none d-lg-inline" href="admin_panel.php">管理员面板</a>
                    <?php endif; ?>
                    <a class="nav-link" href="logout.php">登出</a>
                </div>
                <div class="navbar-text d-lg-none user-info-mobile">
                    欢迎, <?php echo htmlspecialchars($_SESSION['username']); ?>
                    <?php if ($_SESSION['username'] === 'admin'): ?>
                        <div class="mt-2">
                            <a class="nav-link p-0" href="admin_panel.php">管理员面板</a>
                        </div>
                    <?php endif; ?>
                </div>
            </div>
        </div>
    </nav>

    <div class="container mt-4">
        <div class="row">
            <div class="col-12">
                <div class="jumbotron bg-light p-4 rounded">
                    <h1 class="display-4">取件码规则管理系统</h1>
                    <p class="lead">欢迎使用取件码规则管理系统！本系统可以帮助您管理和配置短信取件码的识别规则。</p>
                    <hr class="my-4">
                    <p>通过这个系统，您可以添加、编辑和测试各种规则，以便从短信中准确提取取件码信息。</p>
                </div>
            </div>
        </div>

        <div class="row mt-4">
            <div class="col-md-4 mb-4">
                <div class="card feature-card">
                    <div class="card-body text-center">
                        <div class="card-icon mb-3">
                            <i class="bi bi-list-check"></i>
                        </div>
                        <h5 class="card-title">规则管理</h5>
                        <p class="card-text">添加、编辑和删除取件码识别规则，支持正则表达式和自定义前后缀规则。</p>
                        <a href="rules.php" class="btn btn-primary">进入管理</a>
                    </div>
                </div>
            </div>
            
            <div class="col-md-4 mb-4">
                <div class="card feature-card">
                    <div class="card-body text-center">
                        <div class="card-icon mb-3">
                            <i class="bi bi-collection"></i>
                        </div>
                        <h5 class="card-title">批量规则</h5>
                        <p class="card-text">批量添加规则，或根据短信内容自动生成规则建议。</p>
                        <a href="batch_rules.php" class="btn btn-primary">进入管理</a>
                    </div>
                </div>
            </div>
            
            <div class="col-md-4 mb-4">
                <div class="card feature-card">
                    <div class="card-body text-center">
                        <div class="card-icon mb-3">
                            <i class="bi bi-keyboard"></i>
                        </div>
                        <h5 class="card-title">关键词管理</h5>
                        <p class="card-text">管理用于过滤和识别快递短信的关键词列表。</p>
                        <a href="keywords.php" class="btn btn-primary">进入管理</a>
                    </div>
                </div>
            </div>
            
            <div class="col-md-4 mb-4">
                <div class="card feature-card">
                    <div class="card-body text-center">
                        <div class="card-icon mb-3">
                            <i class="bi bi-file-earmark-arrow-down"></i>
                        </div>
                        <h5 class="card-title">导出JSON</h5>
                        <p class="card-text">将规则和关键词导出为JSON格式，便于备份或在其他系统中使用。</p>
                        <a href="export.php" class="btn btn-primary">导出数据</a>
                    </div>
                </div>
            </div>
            
            <div class="col-md-4 mb-4">
                <div class="card feature-card">
                    <div class="card-body text-center">
                        <div class="card-icon mb-3">
                            <i class="bi bi-code-square"></i>
                        </div>
                        <h5 class="card-title">API接口</h5>
                        <p class="card-text">查看和测试系统的API接口，用于获取规则和关键词数据。</p>
                        <a href="api.php?action=get_rules" class="btn btn-primary" target="_blank">查看接口</a>
                    </div>
                </div>
            </div>
            
            <div class="col-md-4 mb-4">
                <div class="card feature-card">
                    <div class="card-body text-center">
                        <div class="card-icon mb-3">
                            <i class="bi bi-shield-lock"></i>
                        </div>
                        <h5 class="card-title">API密钥</h5>
                        <p class="card-text">管理API访问密钥，确保接口访问的安全性。</p>
                        <a href="api_keys.php" class="btn btn-primary">管理密钥</a>
                    </div>
                </div>
            </div>
        </div>

        <div class="row mt-4">
            <div class="col-12">
                <div class="card">
                    <div class="card-header">
                        <h5>系统使用说明</h5>
                    </div>
                    <div class="card-body">
                        <ol>
                            <li><strong>规则管理</strong>：创建和管理取件码识别规则，支持正则表达式和自定义前后缀两种方式。</li>
                            <li><strong>关键词管理</strong>：配置用于过滤和识别快递短信的关键词。</li>
                            <li><strong>导出JSON</strong>：将所有规则和关键词导出为JSON格式，便于备份或在其他系统中使用。</li>
                            <li><strong>API接口</strong>：提供获取规则和关键词的API接口，供外部系统调用。</li>
                            <li><strong>API密钥</strong>：管理API访问密钥，确保接口访问的安全性。</li>
                        </ol>
                        <p>通过合理配置规则和关键词，系统可以准确识别各种格式的快递取件码短信，提高取件效率。</p>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
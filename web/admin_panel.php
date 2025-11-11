<?php
require_once 'config.php';

// 检查用户是否已登录且为管理员
if (!isLoggedIn()) {
    redirectToLogin();
}

// 检查是否为管理员
if ($_SESSION['username'] !== 'admin') {
    die('需要管理员权限');
}

$message = '';

// 处理重新排列请求
if (isset($_POST['action']) && $_POST['action'] === 'reorder') {
    try {
        $pdo = getDBConnection();
        
        // 重新排列规则ID
        if (isset($_POST['reorder_rules'])) {
            $pdo->beginTransaction();
            
            // 获取所有规则，按ID排序
            $stmt = $pdo->query("SELECT id FROM rules ORDER BY id ASC");
            $rules = $stmt->fetchAll(PDO::FETCH_ASSOC);
            
            if (!empty($rules)) {
                // 为每个规则分配新的连续ID
                $new_id = 1;
                foreach ($rules as $rule) {
                    if ($rule['id'] != $new_id) {
                        $stmt = $pdo->prepare("UPDATE rules SET id = ? WHERE id = ?");
                        $stmt->execute([$new_id, $rule['id']]);
                    }
                    $new_id++;
                }
                
                // 重置自增计数器（注意：ALTER TABLE不支持预处理语句的参数绑定）
                $pdo->query("ALTER TABLE rules AUTO_INCREMENT = " . $new_id);
                
                $message .= "规则ID已重新排列完成。\n";
            } else {
                $message .= "没有规则需要重新排列。\n";
            }
            
            $pdo->commit();
        }
        
        // 重新排列关键词ID
        if (isset($_POST['reorder_keywords'])) {
            $pdo->beginTransaction();
            
            // 获取所有关键词，按ID排序
            $stmt = $pdo->query("SELECT id FROM keywords ORDER BY id ASC");
            $keywords = $stmt->fetchAll(PDO::FETCH_ASSOC);
            
            if (!empty($keywords)) {
                // 为每个关键词分配新的连续ID
                $new_id = 1;
                foreach ($keywords as $keyword) {
                    if ($keyword['id'] != $new_id) {
                        $stmt = $pdo->prepare("UPDATE keywords SET id = ? WHERE id = ?");
                        $stmt->execute([$new_id, $keyword['id']]);
                    }
                    $new_id++;
                }
                
                // 重置自增计数器（注意：ALTER TABLE不支持预处理语句的参数绑定）
                $pdo->query("ALTER TABLE keywords AUTO_INCREMENT = " . $new_id);
                
                $message .= "关键词ID已重新排列完成。\n";
            } else {
                $message .= "没有关键词需要重新排列。\n";
            }
            
            $pdo->commit();
        }
        
        if (empty($message)) {
            $message = "请选择要重新排列的项目。";
        }
        
    } catch (PDOException $e) {
        $message = "操作失败: " . $e->getMessage();
    }
}

// 获取当前规则和关键词数量
try {
    $pdo = getDBConnection();
    
    // 获取规则数量
    $stmt = $pdo->query("SELECT COUNT(*) FROM rules");
    $rules_count = $stmt->fetchColumn();
    
    // 获取关键词数量
    $stmt = $pdo->query("SELECT COUNT(*) FROM keywords");
    $keywords_count = $stmt->fetchColumn();
    
    // 检查ID是否连续
    $rules_continuous = true;
    $keywords_continuous = true;
    
    if ($rules_count > 0) {
        $stmt = $pdo->query("SELECT id FROM rules ORDER BY id ASC");
        $rule_ids = $stmt->fetchAll(PDO::FETCH_COLUMN);
        for ($i = 0; $i < count($rule_ids); $i++) {
            if ($rule_ids[$i] != ($i + 1)) {
                $rules_continuous = false;
                break;
            }
        }
    }
    
    if ($keywords_count > 0) {
        $stmt = $pdo->query("SELECT id FROM keywords ORDER BY id ASC");
        $keyword_ids = $stmt->fetchAll(PDO::FETCH_COLUMN);
        for ($i = 0; $i < count($keyword_ids); $i++) {
            if ($keyword_ids[$i] != ($i + 1)) {
                $keywords_continuous = false;
                break;
            }
        }
    }
    
} catch (PDOException $e) {
    $rules_count = 0;
    $keywords_count = 0;
    $rules_continuous = false;
    $keywords_continuous = false;
    $message = "获取数据失败: " . $e->getMessage();
}
?>

<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>管理员面板 - 取件码规则管理系统</title>
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
                    <a class="nav-link" href="info.php">系统信息</a>
                </div>
                <div class="navbar-nav ms-auto">
                    <span class="navbar-text me-3 d-none d-lg-inline">欢迎, <?php echo htmlspecialchars($_SESSION['username']); ?></span>
                    <a class="nav-link" href="change_password.php">修改密码</a>
                    <a class="nav-link active" href="admin_panel.php">管理员面板</a>
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
                <h2>管理员面板</h2>
                
                <?php if ($message): ?>
                    <div class="alert alert-info alert-dismissible fade show" role="alert">
                        <?php echo nl2br(htmlspecialchars($message)); ?>
                        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                    </div>
                <?php endif; ?>
                
                <div class="card mb-4">
                    <div class="card-header">
                        <h5>系统状态</h5>
                    </div>
                    <div class="card-body">
                        <div class="row">
                            <div class="col-md-6">
                                <div class="d-flex justify-content-between align-items-center">
                                    <span>规则数量:</span>
                                    <span class="badge bg-primary"><?php echo $rules_count; ?></span>
                                </div>
                                <div class="d-flex justify-content-between align-items-center mt-2">
                                    <span>ID连续性:</span>
                                    <?php if ($rules_continuous): ?>
                                        <span class="badge bg-success">连续</span>
                                    <?php else: ?>
                                        <span class="badge bg-warning">不连续</span>
                                    <?php endif; ?>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="d-flex justify-content-between align-items-center">
                                    <span>关键词数量:</span>
                                    <span class="badge bg-primary"><?php echo $keywords_count; ?></span>
                                </div>
                                <div class="d-flex justify-content-between align-items-center mt-2">
                                    <span>ID连续性:</span>
                                    <?php if ($keywords_continuous): ?>
                                        <span class="badge bg-success">连续</span>
                                    <?php else: ?>
                                        <span class="badge bg-warning">不连续</span>
                                    <?php endif; ?>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="card">
                    <div class="card-header">
                        <h5>ID重新排列工具</h5>
                    </div>
                    <div class="card-body">
                        <div class="alert alert-warning">
                            <strong>注意：</strong>此操作将重新排列所有规则和关键词的ID，使其连续。操作不可逆，请谨慎使用。
                        </div>
                        <form method="POST">
                            <input type="hidden" name="action" value="reorder">
                            
                            <div class="mb-3">
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" id="reorder_rules" name="reorder_rules" value="1">
                                    <label class="form-check-label" for="reorder_rules">
                                        重新排列规则ID
                                    </label>
                                </div>
                            </div>
                            
                            <div class="mb-3">
                                <div class="form-check">
                                    <input class="form-check-input" type="checkbox" id="reorder_keywords" name="reorder_keywords" value="1">
                                    <label class="form-check-label" for="reorder_keywords">
                                        重新排列关键词ID
                                    </label>
                                </div>
                            </div>
                            
                            <button type="submit" class="btn btn-warning" onclick="return confirm('确定要重新排列ID吗？此操作不可逆，请确保已备份数据。')">
                                <i class="bi bi-arrow-repeat"></i> 执行重新排列
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
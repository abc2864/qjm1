<?php
require_once 'config.php';

// 检查用户是否已登录
if (!isLoggedIn()) {
    redirectToLogin();
}

$message = '';

// 处理添加/编辑关键词
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $keyword = trim($_POST['keyword']);
    $type = $_POST['type'];
    $description = trim($_POST['description']);
    $is_active = isset($_POST['is_active']) ? 1 : 0;
    
    if (empty($keyword)) {
        $message = '关键词不能为空';
    } else {
        try {
            $pdo = getDBConnection();
            
            if (isset($_POST['id']) && !empty($_POST['id'])) {
                // 更新关键词
                $stmt = $pdo->prepare("UPDATE keywords SET keyword = ?, type = ?, description = ?, is_active = ? WHERE id = ?");
                $stmt->execute([$keyword, $type, $description, $is_active, $_POST['id']]);
                $message = '关键词更新成功';
            } else {
                // 检查关键词是否已存在
                $stmt = $pdo->prepare("SELECT id FROM keywords WHERE keyword = ?");
                $stmt->execute([$keyword]);
                if ($stmt->fetch()) {
                    $message = '关键词已存在';
                } else {
                    // 添加新关键词
                    $stmt = $pdo->prepare("INSERT INTO keywords (keyword, type, description, is_active) VALUES (?, ?, ?, ?)");
                    $stmt->execute([$keyword, $type, $description, $is_active]);
                    $message = '关键词添加成功';
                }
            }
        } catch (PDOException $e) {
            $message = '操作失败: ' . $e->getMessage();
        }
    }
}

// 处理删除关键词
if (isset($_GET['action']) && $_GET['action'] === 'delete' && isset($_GET['id'])) {
    try {
        $pdo = getDBConnection();
        $stmt = $pdo->prepare("DELETE FROM keywords WHERE id = ?");
        $stmt->execute([$_GET['id']]);
        $message = '关键词删除成功';
    } catch (PDOException $e) {
        $message = '删除失败: ' . $e->getMessage();
    }
    
    // 删除后重定向，避免重复提交
    header('Location: keywords.php?message=' . urlencode($message));
    exit;
}

// 获取要编辑的关键词
$editKeyword = null;
if (isset($_GET['action']) && $_GET['action'] === 'edit' && isset($_GET['id'])) {
    try {
        $pdo = getDBConnection();
        $stmt = $pdo->prepare("SELECT * FROM keywords WHERE id = ?");
        $stmt->execute([$_GET['id']]);
        $editKeyword = $stmt->fetch(PDO::FETCH_ASSOC);
    } catch (PDOException $e) {
        $message = '获取关键词信息失败: ' . $e->getMessage();
    }
}

// 获取关键词列表
try {
    $pdo = getDBConnection();
    $stmt = $pdo->query("SELECT * FROM keywords ORDER BY type, keyword");
    $keywords = $stmt->fetchAll(PDO::FETCH_ASSOC);
} catch (PDOException $e) {
    $keywords = [];
    $message = '获取关键词列表失败: ' . $e->getMessage();
}
?>

<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>关键词管理 - 取件码规则管理系统</title>
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
                    <a class="nav-link" href="batch_rules.php">批量规则</a>
                    <a class="nav-link active" href="keywords.php">关键词管理</a>
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
            <div class="col-md-12">
                <h2>关键词管理</h2>
                
                <?php if ($message || isset($_GET['message'])): ?>
                    <div class="alert alert-info alert-dismissible fade show" role="alert">
                        <?php echo htmlspecialchars($message ?? $_GET['message']); ?>
                        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                    </div>
                <?php endif; ?>
                
                <div class="card mb-4">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0"><?php echo $editKeyword ? '编辑关键词' : '添加新关键词'; ?></h5>
                        <?php if ($editKeyword): ?>
                            <a href="keywords.php" class="btn btn-sm btn-outline-secondary">添加新关键词</a>
                        <?php endif; ?>
                    </div>
                    <div class="card-body">
                        <form method="POST">
                            <?php if ($editKeyword): ?>
                                <input type="hidden" name="id" value="<?php echo htmlspecialchars($editKeyword['id']); ?>">
                            <?php endif; ?>
                            
                            <div class="mb-3">
                                <label for="keyword" class="form-label">关键词</label>
                                <input type="text" class="form-control" id="keyword" name="keyword" 
                                       value="<?php echo $editKeyword ? htmlspecialchars($editKeyword['keyword']) : ''; ?>" required>
                            </div>
                            
                            <div class="mb-3">
                                <label for="type" class="form-label">类型</label>
                                <select class="form-select" id="type" name="type" required>
                                    <option value="sender" <?php echo ($editKeyword && $editKeyword['type'] === 'sender') ? 'selected' : ''; ?>>发件人</option>
                                    <option value="content" <?php echo (!$editKeyword || $editKeyword['type'] === 'content') ? 'selected' : ''; ?>>内容</option>
                                </select>
                            </div>
                            
                            <div class="mb-3">
                                <label for="description" class="form-label">描述</label>
                                <textarea class="form-control" id="description" name="description" rows="3"><?php echo $editKeyword ? htmlspecialchars($editKeyword['description']) : ''; ?></textarea>
                            </div>
                            
                            <div class="mb-3 form-check">
                                <input type="checkbox" class="form-check-input" id="is_active" name="is_active" 
                                       <?php echo (!$editKeyword || $editKeyword['is_active']) ? 'checked' : ''; ?>>
                                <label class="form-check-label" for="is_active">启用关键词</label>
                            </div>
                            
                            <button type="submit" class="btn btn-primary"><?php echo $editKeyword ? '更新关键词' : '添加关键词'; ?></button>
                            <?php if ($editKeyword): ?>
                                <a href="keywords.php" class="btn btn-secondary">取消</a>
                            <?php endif; ?>
                        </form>
                    </div>
                </div>
                
                <div class="card">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0">关键词列表</h5>
                        <span class="badge bg-secondary"><?php echo count($keywords); ?> 个关键词</span>
                    </div>
                    <div class="card-body">
                        <?php if (empty($keywords)): ?>
                            <p>暂无关键词数据</p>
                        <?php else: ?>
                            <div class="table-responsive">
                                <table class="table table-striped table-hover">
                                    <thead class="table-dark">
                                        <tr>
                                            <th>ID</th>
                                            <th>关键词</th>
                                            <th>类型</th>
                                            <th>描述</th>
                                            <th>状态</th>
                                            <th>创建时间</th>
                                            <th>操作</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <?php foreach ($keywords as $kw): ?>
                                            <tr>
                                                <td><?php echo htmlspecialchars($kw['id']); ?></td>
                                                <td><?php echo htmlspecialchars($kw['keyword']); ?></td>
                                                <td>
                                                    <?php if ($kw['type'] === 'sender'): ?>
                                                        <span class="badge bg-primary">发件人</span>
                                                    <?php else: ?>
                                                        <span class="badge bg-success">内容</span>
                                                    <?php endif; ?>
                                                </td>
                                                <td>
                                                    <?php 
                                                    if (!empty($kw['description'])) {
                                                        echo strlen($kw['description']) > 50 ? 
                                                            htmlspecialchars(substr($kw['description'], 0, 50)) . '...' : 
                                                            htmlspecialchars($kw['description']);
                                                    } else {
                                                        echo '<span class="text-muted">无描述</span>';
                                                    }
                                                    ?>
                                                </td>
                                                <td>
                                                    <?php if ($kw['is_active']): ?>
                                                        <span class="badge bg-success">启用</span>
                                                    <?php else: ?>
                                                        <span class="badge bg-secondary">禁用</span>
                                                    <?php endif; ?>
                                                </td>
                                                <td>
                                                    <small><?php echo date('Y-m-d', strtotime($kw['created_at'])); ?></small>
                                                </td>
                                                <td>
                                                    <div class="btn-group btn-group-sm" role="group">
                                                        <a href="keywords.php?action=edit&id=<?php echo $kw['id']; ?>" class="btn btn-outline-primary" title="编辑">
                                                            <i class="bi bi-pencil"></i>
                                                        </a>
                                                        <a href="keywords.php?action=delete&id=<?php echo $kw['id']; ?>" 
                                                           class="btn btn-outline-danger" 
                                                           title="删除"
                                                           onclick="return confirm('确定要删除这个关键词吗？\n\n关键词: <?php echo htmlspecialchars($kw['keyword']); ?>')">
                                                            <i class="bi bi-trash"></i>
                                                        </a>
                                                    </div>
                                                </td>
                                            </tr>
                                        <?php endforeach; ?>
                                    </tbody>
                                </table>
                            </div>
                        <?php endif; ?>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
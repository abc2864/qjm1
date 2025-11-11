<?php
require_once 'config.php';

// 检查用户是否已登录
if (!isLoggedIn()) {
    redirectToLogin();
}

$message = '';
$error = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $current_password = $_POST['current_password'];
    $new_password = $_POST['new_password'];
    $confirm_password = $_POST['confirm_password'];
    
    // 验证输入
    if (empty($current_password) || empty($new_password) || empty($confirm_password)) {
        $error = '请填写所有字段';
    } elseif ($new_password !== $confirm_password) {
        $error = '新密码和确认密码不匹配';
    } elseif (strlen($new_password) < 6) {
        $error = '新密码长度至少6位';
    } else {
        try {
            $pdo = getDBConnection();
            $stmt = $pdo->prepare("SELECT id, password FROM users WHERE id = ?");
            $stmt->execute([$_SESSION['user_id']]);
            $user = $stmt->fetch(PDO::FETCH_ASSOC);
            
            if ($user && password_verify($current_password, $user['password'])) {
                // 更新密码
                $hashed_password = password_hash($new_password, PASSWORD_DEFAULT);
                $update_stmt = $pdo->prepare("UPDATE users SET password = ? WHERE id = ?");
                $update_stmt->execute([$hashed_password, $_SESSION['user_id']]);
                
                $message = '密码修改成功';
            } else {
                $error = '当前密码错误';
            }
        } catch (PDOException $e) {
            $error = '修改密码时发生错误: ' . $e->getMessage();
        }
    }
}
?>

<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>修改密码 - 取件码规则管理系统</title>
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
                    <a class="nav-link active" href="change_password.php">修改密码</a>
                    <a class="nav-link" href="logout.php">登出</a>
                </div>
                <div class="navbar-text d-lg-none user-info-mobile">
                    欢迎, <?php echo htmlspecialchars($_SESSION['username']); ?>
                </div>
            </div>
        </div>
    </nav>

    <div class="container mt-4">
        <div class="row justify-content-center">
            <div class="col-md-6">
                <div class="card">
                    <div class="card-header">
                        <h5>修改密码</h5>
                    </div>
                    <div class="card-body">
                        <?php if ($message): ?>
                            <div class="alert alert-success"><?php echo htmlspecialchars($message); ?></div>
                        <?php endif; ?>
                        
                        <?php if ($error): ?>
                            <div class="alert alert-danger"><?php echo htmlspecialchars($error); ?></div>
                        <?php endif; ?>
                        
                        <form method="POST">
                            <div class="mb-3">
                                <label for="current_password" class="form-label">当前密码</label>
                                <input type="password" class="form-control" id="current_password" name="current_password" required>
                            </div>
                            
                            <div class="mb-3">
                                <label for="new_password" class="form-label">新密码</label>
                                <input type="password" class="form-control" id="new_password" name="new_password" required>
                                <div class="form-text">密码长度至少6位</div>
                            </div>
                            
                            <div class="mb-3">
                                <label for="confirm_password" class="form-label">确认新密码</label>
                                <input type="password" class="form-control" id="confirm_password" name="confirm_password" required>
                            </div>
                            
                            <button type="submit" class="btn btn-primary">修改密码</button>
                            <a href="index.php" class="btn btn-secondary">返回首页</a>
                        </form>
                        <div class="mt-3">
                            <div class="alert alert-info">
                                <strong>密码要求：</strong>密码长度至少6位，建议使用包含字母、数字和特殊字符的组合以提高安全性。
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
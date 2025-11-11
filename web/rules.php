<?php
require_once 'config.php';

// 检查用户是否已登录
if (!isLoggedIn()) {
    redirectToLogin();
}

// 处理表单提交
$message = '';
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (isset($_POST['generated_rules'])) {
        // 处理自动生成的规则
        try {
            $pdo = getDBConnection();
            $added_count = 0;
            
            foreach ($_POST['generated_rules'] as $rule_data) {
                if (!isset($rule_data['selected'])) continue;
                
                $name = trim($rule_data['name']);
                $rule_type = $rule_data['rule_type'];
                $description = trim($rule_data['description']);
                
                if (empty($name)) continue;
                
                if ($rule_type === 'regex') {
                    // 正则规则
                    $pattern = trim($rule_data['pattern']);
                    if (empty($pattern)) continue;
                    
                    $stmt = $pdo->prepare("INSERT INTO rules (name, rule_type, pattern, description, is_active) VALUES (?, ?, ?, ?, 1)");
                    $stmt->execute([$name, $rule_type, $pattern, $description]);
                    $added_count++;
                } else {
                    // 自定义前后缀规则
                    $code_prefix = trim($rule_data['code_prefix']);
                    $code_suffix = trim($rule_data['code_suffix']);
                    $station_prefix = trim($rule_data['station_prefix']);
                    $station_suffix = trim($rule_data['station_suffix']);
                    $address_prefix = trim($rule_data['address_prefix']);
                    $address_suffix = trim($rule_data['address_suffix']);
                    
                    $stmt = $pdo->prepare("INSERT INTO rules (name, rule_type, code_prefix, code_suffix, station_prefix, station_suffix, address_prefix, address_suffix, description, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)");
                    $stmt->execute([$name, $rule_type, $code_prefix, $code_suffix, $station_prefix, $station_suffix, $address_prefix, $address_suffix, $description]);
                    $added_count++;
                }
            }
            
            $message = "成功添加 {$added_count} 条规则";
        } catch (PDOException $e) {
            $message = '操作失败: ' . $e->getMessage();
        }
    } else {
        $name = trim($_POST['name']);
        $rule_type = $_POST['rule_type'];
        $description = trim($_POST['description']);
        $is_active = isset($_POST['is_active']) ? 1 : 0;
        
        if (empty($name)) {
            $message = '规则名称不能为空';
        } else {
            try {
                $pdo = getDBConnection();
                
                if ($rule_type === 'regex') {
                    // 正则规则
                    $pattern = trim($_POST['pattern']);
                    if (empty($pattern)) {
                        $message = '正则表达式不能为空';
                    } else {
                        if (isset($_POST['id']) && !empty($_POST['id'])) {
                            // 更新正则规则
                            $stmt = $pdo->prepare("UPDATE rules SET name = ?, rule_type = ?, pattern = ?, description = ?, is_active = ? WHERE id = ?");
                            $stmt->execute([$name, $rule_type, $pattern, $description, $is_active, $_POST['id']]);
                            $message = '正则规则更新成功';
                        } else {
                            // 添加新正则规则
                            $stmt = $pdo->prepare("INSERT INTO rules (name, rule_type, pattern, description, is_active) VALUES (?, ?, ?, ?, ?)");
                            $stmt->execute([$name, $rule_type, $pattern, $description, $is_active]);
                            $message = '正则规则添加成功';
                        }
                    }
                } else {
                    // 自定义前后缀规则（移除发件人设置）
                    $code_prefix = trim($_POST['code_prefix']);
                    $code_suffix = trim($_POST['code_suffix']);
                    $station_prefix = trim($_POST['station_prefix']);
                    $station_suffix = trim($_POST['station_suffix']);
                    $address_prefix = trim($_POST['address_prefix']);
                    $address_suffix = trim($_POST['address_suffix']);
                    
                    if (isset($_POST['id']) && !empty($_POST['id'])) {
                        // 更新自定义规则
                        $stmt = $pdo->prepare("UPDATE rules SET name = ?, rule_type = ?, code_prefix = ?, code_suffix = ?, station_prefix = ?, station_suffix = ?, address_prefix = ?, address_suffix = ?, description = ?, is_active = ? WHERE id = ?");
                        $stmt->execute([$name, $rule_type, $code_prefix, $code_suffix, $station_prefix, $station_suffix, $address_prefix, $address_suffix, $description, $is_active, $_POST['id']]);
                        $message = '自定义规则更新成功';
                    } else {
                        // 添加新自定义规则
                        $stmt = $pdo->prepare("INSERT INTO rules (name, rule_type, code_prefix, code_suffix, station_prefix, station_suffix, address_prefix, address_suffix, description, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                        $stmt->execute([$name, $rule_type, $code_prefix, $code_suffix, $station_prefix, $station_suffix, $address_prefix, $address_suffix, $description, $is_active]);
                        $message = '自定义规则添加成功';
                    }
                }
            } catch (PDOException $e) {
                $message = '操作失败: ' . $e->getMessage();
            }
        }
    }
}

// 处理删除请求
if (isset($_GET['action']) && $_GET['action'] === 'delete' && isset($_GET['id'])) {
    $pdo = null;
    $message = '';
    try {
        $pdo = getDBConnection();
        
        // 开始事务
        $pdo->beginTransaction();
        
        // 获取要删除的规则ID
        $delete_id = (int)$_GET['id'];
        
        // 删除指定的规则
        $stmt = $pdo->prepare("DELETE FROM rules WHERE id = ?");
        $stmt->execute([$delete_id]);
        
        // 重新分配剩余规则的ID，使其连续
        // 首先获取所有剩余规则，按ID排序
        $stmt = $pdo->query("SELECT id FROM rules ORDER BY id ASC");
        $remaining_rules = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        // 为每个规则分配新的连续ID
        $new_id = 1;
        foreach ($remaining_rules as $rule) {
            if ($rule['id'] != $new_id) {
                $stmt = $pdo->prepare("UPDATE rules SET id = ? WHERE id = ?");
                $stmt->execute([$new_id, $rule['id']]);
            }
            $new_id++;
        }
        
        // 重置自增计数器（注意：ALTER TABLE不支持预处理语句的参数绑定）
        // 使用prepare语句可能会导致问题，直接执行查询
        $pdo->exec("ALTER TABLE rules AUTO_INCREMENT = " . $new_id);
        
        // 提交事务
        $pdo->commit();
        
        $message = '规则删除成功，ID已重新排列';
    } catch (PDOException $e) {
        // 只有在有活动事务时才回滚
        if ($pdo && $pdo->inTransaction()) {
            $pdo->rollBack();
        }
        $message = '删除失败: ' . $e->getMessage();
    } catch (Exception $e) {
        // 处理其他可能的异常
        if ($pdo && $pdo->inTransaction()) {
            $pdo->rollBack();
        }
        $message = '删除失败: ' . $e->getMessage();
    }
    
    // 删除后重定向到规则列表页面，避免重复提交
    header('Location: rules.php?message=' . urlencode($message));
    exit;
}

// 获取规则列表，按ID升序排列
try {
    $pdo = getDBConnection();
    $stmt = $pdo->query("SELECT * FROM rules ORDER BY id ASC");
    $rules = $stmt->fetchAll(PDO::FETCH_ASSOC);
} catch (PDOException $e) {
    $rules = [];
    $message = '获取规则列表失败: ' . $e->getMessage();
}

// 获取要编辑的规则
$editRule = null;
if (isset($_GET['action']) && $_GET['action'] === 'edit' && isset($_GET['id'])) {
    try {
        $pdo = getDBConnection();
        $stmt = $pdo->prepare("SELECT * FROM rules WHERE id = ?");
        $stmt->execute([$_GET['id']]);
        $editRule = $stmt->fetch(PDO::FETCH_ASSOC);
    } catch (PDOException $e) {
        $message = '获取规则信息失败: ' . $e->getMessage();
    }
}
?>

<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>规则管理 - 取件码规则管理系统</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.7.2/font/bootstrap-icons.css" rel="stylesheet">
    <link href="mobile.css" rel="stylesheet">
    <style>
        .rule-content {
            max-width: 300px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .table th {
            white-space: nowrap;
        }
        .badge {
            font-size: 0.75em;
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
                    <a class="nav-link active" href="rules.php">规则管理</a>
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
            <div class="col-md-12">
                <h2>规则管理</h2>
                
                <?php if ($message || isset($_GET['message'])): ?>
                    <div class="alert alert-info alert-dismissible fade show" role="alert">
                        <?php echo htmlspecialchars($message ?? $_GET['message']); ?>
                        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                    </div>
                <?php endif; ?>
                
                <div class="card mb-4">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0"><?php echo $editRule ? '编辑规则' : '添加新规则'; ?></h5>
                        <?php if ($editRule): ?>
                            <a href="rules.php" class="btn btn-sm btn-outline-secondary">添加新规则</a>
                        <?php endif; ?>
                    </div>
                    <div class="card-body">
                        <form method="POST" id="ruleForm">
                            <?php if ($editRule): ?>
                                <input type="hidden" name="id" value="<?php echo htmlspecialchars($editRule['id']); ?>">
                            <?php endif; ?>
                            
                            <div class="mb-3">
                                <label for="name" class="form-label">规则名称</label>
                                <input type="text" class="form-control" id="name" name="name" 
                                       value="<?php echo $editRule ? htmlspecialchars($editRule['name']) : ''; ?>" required>
                            </div>
                            
                            <div class="mb-3">
                                <label for="rule_type" class="form-label">规则类型</label>
                                <select class="form-select" id="rule_type" name="rule_type" required>
                                    <option value="regex" <?php echo (!$editRule || $editRule['rule_type'] === 'regex') ? 'selected' : ''; ?>>正则表达式规则</option>
                                    <option value="custom" <?php echo ($editRule && $editRule['rule_type'] === 'custom') ? 'selected' : ''; ?>>自定义前后缀规则</option>
                                </select>
                            </div>
                            
                            <!-- 正则表达式规则部分 -->
                            <div id="regexSection" style="<?php echo (!$editRule || $editRule['rule_type'] === 'regex') ? 'display: block;' : 'display: none;'; ?>">
                                <div class="mb-3">
                                    <label for="pattern" class="form-label">正则表达式</label>
                                    <input type="text" class="form-control" id="pattern" name="pattern" 
                                           value="<?php echo ($editRule && $editRule['rule_type'] === 'regex') ? htmlspecialchars($editRule['pattern']) : ''; ?>">
                                    <div class="form-text">用于匹配取件码的正则表达式</div>
                                </div>
                                
                                <!-- 正则表达式示例 -->
                                <div class="card mb-3">
                                    <div class="card-header">
                                        <h6 class="mb-0">正则表达式示例</h6>
                                    </div>
                                    <div class="card-body">
                                        <div class="mb-2">
                                            <strong>示例1: 匹配数字取件码</strong>
                                            <div class="form-text">正则表达式: <code>(\d{4,6})</code></div>
                                            <div class="form-text">匹配4到6位数字，如：1234、12345、123456</div>
                                        </div>
                                        <div class="mb-2">
                                            <strong>示例2: 匹配包含特定关键词的取件码</strong>
                                            <div class="form-text">正则表达式: <code>取件码[:：](\d{4,6})</code></div>
                                            <div class="form-text">匹配"取件码:"或"取件码："后跟4到6位数字</div>
                                        </div>
                                        <div class="mb-2">
                                            <strong>示例3: 匹配复杂格式的取件码</strong>
                                            <div class="form-text">正则表达式: <code>(\d{1,3}-\d{1,3}-\d{3,4})</code></div>
                                            <div class="form-text">匹配格式如：1-2-5913、10-5-5913等</div>
                                        </div>
                                        <div class="mb-2">
                                            <strong>示例4: 匹配字母数字组合取件码</strong>
                                            <div class="form-text">正则表达式: <code>([A-Z0-9]{6,8})</code></div>
                                            <div class="form-text">匹配6到8位大写字母和数字的组合</div>
                                        </div>
                                        <div class="mb-2">
                                            <strong>示例5: 同时提取取件码、驿站和地址</strong>
                                            <div class="form-text">正则表达式: <code>/\【(.+?)\】.*取件码[：:](\w+).*地址[：:](.+?)(?=,|，|。|$)/</code></div>
                                            <div class="form-text">第一个捕获组(\w+)提取取件码，第二个捕获组(.+?)提取驿站名称，第三个捕获组(.+?)提取地址</div>
                                        </div>
                                        <div class="mb-2">
                                            <strong>示例6: 针对菜鸟驿站格式的正则表达式</strong>
                                            <div class="form-text">正则表达式: <code>/\【(.+?)\】取件码[:：](\w+)到地址[:：]([^，]+)，凭\w+来取/</code></div>
                                            <div class="form-text">针对你提供的示例短信格式，第一个捕获组(.+?)提取驿站名称，第二个(\w+)提取取件码，第三个([^，]+)提取地址</div>
                                        </div>
                                        <div class="mb-2">
                                            <strong>示例7: 针对兔喜生活格式的正则表达式</strong>
                                            <div class="form-text">正则表达式: <code>/\【(.+?)\】您的(.+?)包裹已到(.+?)，凭([\d\-]+)来取/</code></div>
                                            <div class="form-text">针对兔喜生活短信格式，第一个捕获组提取驿站名称，第二个提取快递公司，第三个提取地址，第四个提取取件码</div>
                                        </div>
                                        <div class="alert alert-info">
                                            <strong>提示:</strong> 正则表达式中的圆括号()用于捕获匹配的内容，这是提取取件码的关键。<br>
                                            第一个捕获组通常用于提取取件码，第二个捕获组用于提取驿站名称，第三个捕获组用于提取地址。<br>
                                            注意：在PHP中，正则表达式需要用分隔符（如/）包围，方括号【】需要使用反斜杠\进行转义。
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <!-- 自定义前后缀规则部分（移除发件人设置） -->
                            <div id="customSection" style="<?php echo ($editRule && $editRule['rule_type'] === 'custom') ? 'display: block;' : 'display: none;'; ?>">
                                <div class="row">
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label for="code_prefix" class="form-label">取件码前缀</label>
                                            <input type="text" class="form-control" id="code_prefix" name="code_prefix" 
                                                   value="<?php echo ($editRule && $editRule['rule_type'] === 'custom') ? htmlspecialchars($editRule['code_prefix']) : ''; ?>">
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label for="code_suffix" class="form-label">取件码后缀</label>
                                            <input type="text" class="form-control" id="code_suffix" name="code_suffix" 
                                                   value="<?php echo ($editRule && $editRule['rule_type'] === 'custom') ? htmlspecialchars($editRule['code_suffix']) : ''; ?>">
                                        </div>
                                    </div>
                                </div>
                                
                                <div class="row">
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label for="station_prefix" class="form-label">驿站前缀</label>
                                            <input type="text" class="form-control" id="station_prefix" name="station_prefix" 
                                                   value="<?php echo ($editRule && $editRule['rule_type'] === 'custom') ? htmlspecialchars($editRule['station_prefix']) : ''; ?>">
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label for="station_suffix" class="form-label">驿站后缀</label>
                                            <input type="text" class="form-control" id="station_suffix" name="station_suffix" 
                                                   value="<?php echo ($editRule && $editRule['rule_type'] === 'custom') ? htmlspecialchars($editRule['station_suffix']) : ''; ?>">
                                        </div>
                                    </div>
                                </div>
                                
                                <div class="row">
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label for="address_prefix" class="form-label">地址前缀</label>
                                            <input type="text" class="form-control" id="address_prefix" name="address_prefix" 
                                                   value="<?php echo ($editRule && $editRule['rule_type'] === 'custom') ? htmlspecialchars($editRule['address_prefix']) : ''; ?>">
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label for="address_suffix" class="form-label">地址后缀</label>
                                            <input type="text" class="form-control" id="address_suffix" name="address_suffix" 
                                                   value="<?php echo ($editRule && $editRule['rule_type'] === 'custom') ? htmlspecialchars($editRule['address_suffix']) : ''; ?>">
                                        </div>
                                    </div>
                                </div>
                            </div>
                            
                            <div class="mb-3">
                                <label for="description" class="form-label">描述</label>
                                <textarea class="form-control" id="description" name="description" rows="3"><?php echo $editRule ? htmlspecialchars($editRule['description']) : ''; ?></textarea>
                            </div>
                            
                            <div class="mb-3 form-check">
                                <input type="checkbox" class="form-check-input" id="is_active" name="is_active" 
                                       <?php echo (!$editRule || $editRule['is_active']) ? 'checked' : ''; ?>>
                                <label class="form-check-label" for="is_active">启用规则</label>
                            </div>
                            
                            <button type="submit" class="btn btn-primary"><?php echo $editRule ? '更新规则' : '添加规则'; ?></button>
                            <?php if ($editRule): ?>
                                <a href="rules.php" class="btn btn-secondary">取消</a>
                            <?php endif; ?>
                        </form>
                        <div class="mt-3">
                            <div class="alert alert-info">
                                <strong>操作提示：</strong>
                                <?php if ($editRule): ?>
                                    点击"更新规则"保存修改，点击"取消"返回规则列表。
                                <?php else: ?>
                                    填写表单后点击"添加规则"创建新规则。
                                <?php endif; ?>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="card">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0">规则列表</h5>
                        <span class="badge bg-secondary"><?php echo count($rules); ?> 条规则</span>
                    </div>
                    <div class="card-body">
                        <?php if (empty($rules)): ?>
                            <p>暂无规则数据</p>
                        <?php else: ?>
                            <div class="table-responsive">
                                <table class="table table-striped table-hover">
                                    <thead class="table-dark">
                                        <tr>
                                            <th>ID</th>
                                            <th>名称</th>
                                            <th>类型</th>
                                            <th>规则内容</th>
                                            <th>描述</th>
                                            <th>状态</th>
                                            <th>创建时间</th>
                                            <th>操作</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <?php foreach ($rules as $rule): ?>
                                            <tr>
                                                <td><?php echo htmlspecialchars($rule['id']); ?></td>
                                                <td><?php echo htmlspecialchars($rule['name']); ?></td>
                                                <td>
                                                    <?php if ($rule['rule_type'] === 'regex'): ?>
                                                        <span class="badge bg-primary">正则</span>
                                                    <?php else: ?>
                                                        <span class="badge bg-success">自定义</span>
                                                    <?php endif; ?>
                                                </td>
                                                <td class="rule-content">
                                                    <?php if ($rule['rule_type'] === 'regex'): ?>
                                                        <small><?php echo htmlspecialchars($rule['pattern']); ?></small>
                                                    <?php else: ?>
                                                        <small>
                                                            取件码: <?php echo htmlspecialchars($rule['code_prefix'] . '...' . $rule['code_suffix']); ?><br>
                                                            驿站: <?php echo htmlspecialchars($rule['station_prefix'] . '...' . $rule['station_suffix']); ?><br>
                                                            地址: <?php echo htmlspecialchars($rule['address_prefix'] . '...' . $rule['address_suffix']); ?>
                                                        </small>
                                                    <?php endif; ?>
                                                </td>
                                                <td>
                                                    <?php 
                                                    if (!empty($rule['description'])) {
                                                        echo strlen($rule['description']) > 50 ? 
                                                            htmlspecialchars(substr($rule['description'], 0, 50)) . '...' : 
                                                            htmlspecialchars($rule['description']);
                                                    } else {
                                                        echo '<span class="text-muted">无描述</span>';
                                                    }
                                                    ?>
                                                </td>
                                                <td>
                                                    <?php if ($rule['is_active']): ?>
                                                        <span class="badge bg-success">启用</span>
                                                    <?php else: ?>
                                                        <span class="badge bg-secondary">禁用</span>
                                                    <?php endif; ?>
                                                </td>
                                                <td>
                                                    <small><?php echo date('Y-m-d', strtotime($rule['created_at'])); ?></small>
                                                </td>
                                                <td>
                                                    <div class="btn-group btn-group-sm" role="group">
                                                        <a href="rules.php?action=edit&id=<?php echo $rule['id']; ?>" class="btn btn-outline-primary" title="编辑">
                                                            <i class="bi bi-pencil"></i>
                                                        </a>
                                                        <a href="rules.php?action=delete&id=<?php echo $rule['id']; ?>" 
                                                           class="btn btn-outline-danger" 
                                                           title="删除"
                                                           onclick="return confirm('确定要删除这个规则吗？\n\n规则ID: <?php echo htmlspecialchars($rule['id']); ?>\n规则名称: <?php echo htmlspecialchars($rule['name']); ?>')">
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
                
                <!-- 正则表达式说明 -->
                <div class="card mt-4">
                    <div class="card-header">
                        <h5>正则表达式使用说明</h5>
                    </div>
                    <div class="card-body">
                        <p>正则表达式是一种强大的文本匹配工具，可以精确地从短信中提取所需信息。</p>
                        
                        <h6>常用正则表达式语法</h6>
                        <ul>
                            <li><code>\d</code> - 匹配数字 (0-9)</li>
                            <li><code>\d{4}</code> - 匹配恰好4个数字</li>
                            <li><code>\d{4,6}</code> - 匹配4到6个数字</li>
                            <li><code>[A-Z]</code> - 匹配大写字母</li>
                            <li><code>[A-Z0-9]</code> - 匹配大写字母或数字</li>
                            <li><code>[A-Z0-9]{6}</code> - 匹配恰好6个大写字母或数字</li>
                            <li><code>()</code> - 捕获组，用于提取匹配的内容</li>
                            <li><code>.*</code> - 匹配任意字符（除换行符）零次或多次</li>
                            <li><code>.+?</code> - 匹配任意字符（除换行符）一次或多次（非贪婪模式）</li>
                        </ul>
                        
                        <h6>多信息提取说明</h6>
                        <p>正则规则支持同时提取取件码、驿站和地址信息：</p>
                        <ul>
                            <li>第一个捕获组（第一个括号内的表达式）用于提取<strong>取件码</strong></li>
                            <li>第二个捕获组（第二个括号内的表达式）用于提取<strong>驿站名称</strong></li>
                            <li>第三个捕获组（第三个括号内的表达式）用于提取<strong>地址</strong></li>
                        </ul>
                        <div class="alert alert-warning">
                            <strong>注意:</strong> 特殊字符如方括号【】需要使用反斜杠\进行转义，正则表达式需要用分隔符（如/）包围。
                        </div>
                        
                        <h6>使用建议</h6>
                        <ol>
                            <li>使用圆括号 <code>()</code> 将需要提取的部分括起来</li>
                            <li>确保正则表达式足够精确，避免匹配到错误内容</li>
                            <li>使用测试功能验证正则表达式的正确性</li>
                            <li>对于复杂的短信格式，可以使用在线正则表达式测试工具进行调试</li>
                        </ol>
                        
                        <div class="alert alert-warning">
                            <strong>重要提示:</strong> 在PHP中，正则表达式需要用分隔符包围，常用分隔符为斜杠(/)。例如：<code>/\d+/</code>。
                            特殊字符如 <code>[]\^$.|?*+()</code> 等如果需要匹配字面意思，需要使用反斜杠 <code>\</code> 进行转义。
                        </div>
                    </div>
                </div>
                
                <!-- 规则演示区域（简化输入类型选择） -->
                <div class="card mt-4">
                    <div class="card-header">
                        <h5>规则演示</h5>
                    </div>
                    <div class="card-body">
                        <form id="demoForm">
                            <div class="mb-3">
                                <label for="demo_rule_id" class="form-label">选择规则</label>
                                <select class="form-select" id="demo_rule_id" name="demo_rule_id" required>
                                    <option value="">请选择一个规则</option>
                                    <?php foreach ($rules as $rule): ?>
                                        <option value="<?php echo $rule['id']; ?>">
                                            <?php echo htmlspecialchars($rule['name']); ?> 
                                            (<?php echo $rule['rule_type'] === 'regex' ? '正则' : '自定义'; ?>)
                                        </option>
                                    <?php endforeach; ?>
                                </select>
                            </div>
                            
                            <div class="mb-3">
                                <label for="demo_input" class="form-label">输入文本</label>
                                <textarea class="form-control" id="demo_input" name="demo_input" rows="3" placeholder="请输入要测试的文本..."></textarea>
                            </div>
                            
                            <button type="button" class="btn btn-success" onclick="testRule()">
                                <i class="bi bi-play-circle"></i> 测试规则
                            </button>
                            <!-- 添加一个测试示例按钮 -->
                            <button type="button" class="btn btn-secondary" onclick="fillTestExample()">
                                <i class="bi bi-file-earmark-text"></i> 填入测试示例
                            </button>
                        </form>
                        
                        <div id="demo_result" class="mt-4" style="display: none;">
                            <h6>测试结果:</h6>
                            <!-- 添加显示模式切换按钮 -->
                            <div class="mb-2">
                                <button type="button" class="btn btn-outline-primary btn-sm" onclick="switchDisplayMode('json')">
                                    <i class="bi bi-code-square"></i> JSON模式
                                </button>
                                <button type="button" class="btn btn-outline-primary btn-sm" onclick="switchDisplayMode('text')">
                                    <i class="bi bi-file-text"></i> 文本模式
                                </button>
                            </div>
                            <div class="alert alert-info" role="alert">
                                <pre id="result_content_json" style="display: none;"></pre>
                                <div id="result_content_text" style="display: none;"></div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // 规则类型切换
        document.getElementById('rule_type').addEventListener('change', function() {
            if (this.value === 'regex') {
                document.getElementById('regexSection').style.display = 'block';
                document.getElementById('customSection').style.display = 'none';
            } else {
                document.getElementById('regexSection').style.display = 'none';
                document.getElementById('customSection').style.display = 'block';
            }
        });
        
        // 填入测试示例
        function fillTestExample() {
            document.getElementById('demo_input').value = "【菜鸟驿站】取件码:123456到地址:北京市朝阳区某某街道123号，凭123456来取";
        }
        
        // 当前显示模式
        let currentDisplayMode = 'json';
        let lastTestResult = null;
        
        // 切换显示模式
        function switchDisplayMode(mode) {
            currentDisplayMode = mode;
            
            // 更新按钮状态
            document.querySelectorAll('#demo_result .btn-outline-primary').forEach(btn => {
                btn.classList.remove('active');
            });
            
            if (mode === 'json') {
                document.getElementById('result_content_json').style.display = 'block';
                document.getElementById('result_content_text').style.display = 'none';
            } else {
                document.getElementById('result_content_json').style.display = 'none';
                document.getElementById('result_content_text').style.display = 'block';
            }
            
            // 如果有测试结果，重新显示
            if (lastTestResult) {
                displayTestResult(lastTestResult);
            }
        }
        
        // 显示测试结果
        function displayTestResult(data) {
            lastTestResult = data;
            
            const resultJsonDiv = document.getElementById('result_content_json');
            const resultTextDiv = document.getElementById('result_content_text');
            
            if (currentDisplayMode === 'json') {
                resultJsonDiv.textContent = JSON.stringify(data.result, null, 2);
                resultJsonDiv.style.display = 'block';
                resultTextDiv.style.display = 'none';
            } else {
                // 文本模式显示
                let textResult = '';
                
                if (data.success) {
                    textResult += '规则名称: ' + data.result.rule_name + '\n';
                    textResult += '规则类型: ' + (data.result.rule_type === 'regex' ? '正则规则' : '自定义前后缀规则') + '\n';
                    textResult += '输入文本: ' + data.result.input_text + '\n\n';
                    
                    if (data.result.rule_type === 'regex') {
                        // 正则规则结果
                        textResult += '正则表达式: ' + data.result.pattern + '\n';
                        if (data.result.matched) {
                            textResult += '匹配结果: 成功\n';
                            textResult += '匹配内容:\n';
                            for (let i = 0; i < data.result.matches.length; i++) {
                                textResult += '  [' + i + ']: ' + data.result.matches[i] + '\n';
                            }
                        } else {
                            textResult += '匹配结果: 未匹配到内容\n';
                        }
                    } else {
                        // 自定义前后缀规则结果
                        for (const [key, test] of Object.entries(data.result.tests)) {
                            let typeName = '';
                            switch (key) {
                                case 'code': typeName = '取件码'; break;
                                case 'station': typeName = '驿站'; break;
                                case 'address': typeName = '地址'; break;
                                default: typeName = key;
                            }
                            
                            textResult += typeName + ':\n';
                            textResult += '  前缀: "' + test.prefix + '"\n';
                            textResult += '  后缀: "' + test.suffix + '"\n';
                            
                            if (test.matched) {
                                textResult += '  匹配结果: 成功\n';
                                textResult += '  提取内容: "' + test.extracted + '"\n';
                            } else {
                                textResult += '  匹配结果: 未匹配到内容\n';
                            }
                            textResult += '\n';
                        }
                    }
                } else {
                    textResult = '测试失败: ' + data.message;
                }
                
                resultTextDiv.innerHTML = '<pre>' + textResult + '</pre>';
                resultTextDiv.style.display = 'block';
                resultJsonDiv.style.display = 'none';
            }
        }
        
        // 测试规则功能（简化参数）
        function testRule() {
            const ruleId = document.getElementById('demo_rule_id').value;
            const inputText = document.getElementById('demo_input').value;
            
            if (!ruleId) {
                alert('请选择一个规则');
                return;
            }
            
            if (!inputText.trim()) {
                alert('请输入要测试的文本');
                return;
            }
            
            // 发送AJAX请求到服务器测试规则
            fetch('test_rule.php', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `rule_id=${encodeURIComponent(ruleId)}&input_text=${encodeURIComponent(inputText)}`
            })
            .then(response => response.json())
            .then(data => {
                const resultDiv = document.getElementById('demo_result');
                
                if (data.success) {
                    displayTestResult(data);
                    resultDiv.style.display = 'block';
                } else {
                    document.getElementById('result_content_json').textContent = '测试失败: ' + data.message;
                    document.getElementById('result_content_json').style.display = 'block';
                    document.getElementById('result_content_text').style.display = 'none';
                    resultDiv.style.display = 'block';
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('测试过程中发生错误');
            });
        }
    </script>
</body>
</html>
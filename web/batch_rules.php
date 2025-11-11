<?php
require_once 'config.php';

// 检查用户是否已登录
if (!isLoggedIn()) {
    redirectToLogin();
}

// 处理表单提交
$message = '';
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (isset($_POST['action']) && $_POST['action'] === 'batch_add') {
        // 批量添加规则
        $rules_data = trim($_POST['rules_data']);
        if (empty($rules_data)) {
            $message = '规则数据不能为空';
        } else {
            try {
                $pdo = getDBConnection();
                $pdo->beginTransaction();
                
                $lines = explode("\n", $rules_data);
                $added_count = 0;
                
                foreach ($lines as $line) {
                    $line = trim($line);
                    if (empty($line)) continue;
                    
                    // 解析每行数据，格式: 名称|类型|规则数据|描述
                    $parts = explode('|', $line, 4);
                    if (count($parts) < 3) continue;
                    
                    $name = trim($parts[0]);
                    $rule_type = trim($parts[1]);
                    $rule_data = trim($parts[2]);
                    $description = isset($parts[3]) ? trim($parts[3]) : '';
                    
                    if (empty($name) || empty($rule_type)) continue;
                    
                    if ($rule_type === 'regex') {
                        // 正则规则
                        $stmt = $pdo->prepare("INSERT INTO rules (name, rule_type, pattern, description, is_active) VALUES (?, ?, ?, ?, 1)");
                        $stmt->execute([$name, $rule_type, $rule_data, $description]);
                        $added_count++;
                    } else if ($rule_type === 'custom') {
                        // 自定义规则，格式: code_prefix,code_suffix,station_prefix,station_suffix,address_prefix,address_suffix
                        $custom_parts = explode(',', $rule_data, 6);
                        if (count($custom_parts) >= 6) {
                            $code_prefix = trim($custom_parts[0]);
                            $code_suffix = trim($custom_parts[1]);
                            $station_prefix = trim($custom_parts[2]);
                            $station_suffix = trim($custom_parts[3]);
                            $address_prefix = trim($custom_parts[4]);
                            $address_suffix = trim($custom_parts[5]);
                            
                            $stmt = $pdo->prepare("INSERT INTO rules (name, rule_type, code_prefix, code_suffix, station_prefix, station_suffix, address_prefix, address_suffix, description, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)");
                            $stmt->execute([$name, $rule_type, $code_prefix, $code_suffix, $station_prefix, $station_suffix, $address_prefix, $address_suffix, $description]);
                            $added_count++;
                        }
                    }
                }
                
                $pdo->commit();
                $message = "成功批量添加 {$added_count} 条规则";
            } catch (PDOException $e) {
                $pdo->rollBack();
                $message = '批量添加失败: ' . $e->getMessage();
            }
        }
    } else if (isset($_POST['action']) && $_POST['action'] === 'generate_rules') {
        // 根据短信内容生成规则
        $sms_content = trim($_POST['sms_content']);
        if (empty($sms_content)) {
            $message = '短信内容不能为空';
        } else {
            // 生成规则的逻辑将在页面中处理并显示给用户选择
        }
    }
}

// 获取现有规则列表
try {
    $pdo = getDBConnection();
    $stmt = $pdo->query("SELECT * FROM rules ORDER BY id ASC");
    $rules = $stmt->fetchAll(PDO::FETCH_ASSOC);
} catch (PDOException $e) {
    $rules = [];
    $message = '获取规则列表失败: ' . $e->getMessage();
}
?>

<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>批量规则管理 - 取件码规则管理系统</title>
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
        .generated-rule {
            border: 1px solid #dee2e6;
            border-radius: 0.375rem;
            padding: 1rem;
            margin-bottom: 1rem;
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
                    <a class="nav-link active" href="batch_rules.php">批量规则</a>
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
                <h2>批量规则管理</h2>
                
                <?php if ($message): ?>
                    <div class="alert alert-info alert-dismissible fade show" role="alert">
                        <?php echo htmlspecialchars($message); ?>
                        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                    </div>
                <?php endif; ?>
                
                <!-- 批量添加规则 -->
                <div class="card mb-4">
                    <div class="card-header">
                        <h5 class="mb-0">批量添加规则</h5>
                    </div>
                    <div class="card-body">
                        <form method="POST">
                            <input type="hidden" name="action" value="batch_add">
                            <div class="mb-3">
                                <label for="rules_data" class="form-label">规则数据（每行一条规则）</label>
                                <textarea class="form-control" id="rules_data" name="rules_data" rows="6" placeholder="格式: 名称|类型(regex/custom)|规则数据|描述&#10;正则规则示例: 顺丰速运|regex|顺丰速运.*取件码.*([A-Z0-9]{4,6})|匹配顺丰速运的取件短信&#10;自定义规则示例: 自定义规则|custom|取件码:,到,地址:,,凭,来取|自定义前后缀规则示例"></textarea>
                                <div class="form-text">
                                    正则规则格式: 名称|regex|正则表达式|描述<br>
                                    自定义规则格式: 名称|custom|取件码前缀,取件码后缀,驿站前缀,驿站后缀,地址前缀,地址后缀|描述
                                </div>
                            </div>
                            <button type="submit" class="btn btn-primary">批量添加规则</button>
                        </form>
                    </div>
                </div>
                
                <!-- 自动生成规则 -->
                <div class="card mb-4">
                    <div class="card-header">
                        <h5 class="mb-0">自动生成规则</h5>
                    </div>
                    <div class="card-body">
                        <form method="POST">
                            <input type="hidden" name="action" value="generate_rules">
                            <div class="mb-3">
                                <label for="sms_content" class="form-label">短信内容</label>
                                <textarea class="form-control" id="sms_content" name="sms_content" rows="3" placeholder="请输入短信内容，系统将根据内容自动生成可能的规则"><?php echo isset($_POST['sms_content']) ? htmlspecialchars($_POST['sms_content']) : ''; ?></textarea>
                            </div>
                            <button type="submit" class="btn btn-success">生成规则</button>
                        </form>
                        
                        <?php if (isset($_POST['action']) && $_POST['action'] === 'generate_rules' && !empty($_POST['sms_content'])): ?>
                            <?php
                            $sms_content = trim($_POST['sms_content']);
                            $generated_rules = generateRulesFromSms($sms_content);
                            ?>
                            <div class="mt-4">
                                <h6>生成的规则建议：</h6>
                                <?php if (!empty($generated_rules)): ?>
                                    <form method="POST" action="rules.php">
                                        <?php foreach ($generated_rules as $index => $rule): ?>
                                            <div class="generated-rule">
                                                <h6><?php echo htmlspecialchars($rule['name']); ?></h6>
                                                <div class="form-check mb-2">
                                                    <input class="form-check-input" type="checkbox" name="generated_rules[<?php echo $index; ?>][selected]" id="rule_<?php echo $index; ?>" value="1">
                                                    <label class="form-check-label" for="rule_<?php echo $index; ?>">
                                                        添加此规则
                                                    </label>
                                                </div>
                                                <input type="hidden" name="generated_rules[<?php echo $index; ?>][name]" value="<?php echo htmlspecialchars($rule['name']); ?>">
                                                <input type="hidden" name="generated_rules[<?php echo $index; ?>][rule_type]" value="<?php echo htmlspecialchars($rule['rule_type']); ?>">
                                                <?php if ($rule['rule_type'] === 'regex'): ?>
                                                    <div class="mb-2">
                                                        <strong>正则表达式:</strong> 
                                                        <code><?php echo htmlspecialchars($rule['pattern']); ?></code>
                                                        <input type="hidden" name="generated_rules[<?php echo $index; ?>][pattern]" value="<?php echo htmlspecialchars($rule['pattern']); ?>">
                                                    </div>
                                                <?php else: ?>
                                                    <div class="row">
                                                        <div class="col-md-6">
                                                            <div class="mb-1">
                                                                <strong>取件码前缀:</strong> <?php echo htmlspecialchars($rule['code_prefix']); ?>
                                                                <input type="hidden" name="generated_rules[<?php echo $index; ?>][code_prefix]" value="<?php echo htmlspecialchars($rule['code_prefix']); ?>">
                                                            </div>
                                                            <div class="mb-1">
                                                                <strong>取件码后缀:</strong> <?php echo htmlspecialchars($rule['code_suffix']); ?>
                                                                <input type="hidden" name="generated_rules[<?php echo $index; ?>][code_suffix]" value="<?php echo htmlspecialchars($rule['code_suffix']); ?>">
                                                            </div>
                                                        </div>
                                                        <div class="col-md-6">
                                                            <div class="mb-1">
                                                                <strong>驿站前缀:</strong> <?php echo htmlspecialchars($rule['station_prefix']); ?>
                                                                <input type="hidden" name="generated_rules[<?php echo $index; ?>][station_prefix]" value="<?php echo htmlspecialchars($rule['station_prefix']); ?>">
                                                            </div>
                                                            <div class="mb-1">
                                                                <strong>驿站后缀:</strong> <?php echo htmlspecialchars($rule['station_suffix']); ?>
                                                                <input type="hidden" name="generated_rules[<?php echo $index; ?>][station_suffix]" value="<?php echo htmlspecialchars($rule['station_suffix']); ?>">
                                                            </div>
                                                        </div>
                                                        <div class="col-md-6">
                                                            <div class="mb-1">
                                                                <strong>地址前缀:</strong> <?php echo htmlspecialchars($rule['address_prefix']); ?>
                                                                <input type="hidden" name="generated_rules[<?php echo $index; ?>][address_prefix]" value="<?php echo htmlspecialchars($rule['address_prefix']); ?>">
                                                            </div>
                                                        </div>
                                                        <div class="col-md-6">
                                                            <div class="mb-1">
                                                                <strong>地址后缀:</strong> <?php echo htmlspecialchars($rule['address_suffix']); ?>
                                                                <input type="hidden" name="generated_rules[<?php echo $index; ?>][address_suffix]" value="<?php echo htmlspecialchars($rule['address_suffix']); ?>">
                                                            </div>
                                                        </div>
                                                    </div>
                                                <?php endif; ?>
                                                <div class="mb-2">
                                                    <strong>描述:</strong> <?php echo htmlspecialchars($rule['description']); ?>
                                                    <input type="hidden" name="generated_rules[<?php echo $index; ?>][description]" value="<?php echo htmlspecialchars($rule['description']); ?>">
                                                </div>
                                            </div>
                                        <?php endforeach; ?>
                                        <button type="submit" class="btn btn-primary">添加选中的规则</button>
                                    </form>
                                <?php else: ?>
                                    <div class="alert alert-warning">无法从短信内容生成规则，请手动添加。</div>
                                <?php endif; ?>
                            </div>
                        <?php endif; ?>
                    </div>
                </div>
                
                <!-- 现有规则列表 -->
                <div class="card">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0">现有规则列表</h5>
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

<?php
// 根据短信内容生成规则的函数
function generateRulesFromSms($sms_content) {
    $rules = [];
    
    // 检查是否为兔喜生活格式（新格式）
    // 示例: 【兔喜生活】您有包裹已到达北杜中通快递店，取件码为6-4-9641，地址:北杜镇千佛塔向东130米路南中通快递门店内
    if (preg_match('/【(兔喜生活)】您有包裹已到达([^，。]+)，取件码为([\\d\\-]+).*地址[:：]([^，。\\[\\]【】]+)/u', $sms_content, $matches)) {
        // 生成通用正则规则（可匹配所有兔喜生活新格式短信，提取驿站、取件码、地址）
        $pattern = '/【(兔喜生活)】您有包裹已到达([^，。]+)，取件码为([\\d\\-]+).*地址[:：]([^，。\\[\\]【】]+)/';
        $rules[] = [
            'name' => "通用-兔喜生活(新格式)",
            'rule_type' => 'regex',
            'pattern' => $pattern,
            'description' => "通用规则，用于匹配兔喜生活新格式的短信，提取驿站、取件码和地址"
        ];
    }
    
    // 检查是否为兔喜生活格式（旧格式）
    // 示例: 【兔喜生活】您的中通快递包裹已到北杜镇 千佛塔向东130米路南中通快递门店内，凭1-2-7923来取
    else if (preg_match('/【(兔喜生活)】您的(.+?)包裹已到(.+?)，凭([\d\-]+)来取/u', $sms_content, $matches)) {
        // 生成通用正则规则（可匹配所有兔喜生活旧格式短信，提取驿站、取件码、地址）
        $pattern = '/【(兔喜生活)】您的(.+?)包裹已到(.+?)，凭([\d\-]+)来取/';
        $rules[] = [
            'name' => "通用-兔喜生活(旧格式)",
            'rule_type' => 'regex',
            'pattern' => $pattern,
            'description' => "通用规则，用于匹配兔喜生活旧格式的短信，提取驿站、取件码和地址"
        ];
    }
    
    // 检查是否为妈妈驿站格式
    // 示例: 【妈妈驿站】取货码2-4-26，您有圆通快递包裹，已到圆通速递宿舍楼一楼超市
    else if (preg_match('/【(妈妈驿站)】取货码([\d\-]+)，您有(.+?)包裹，已到(.+)/u', $sms_content, $matches)) {
        // 生成通用正则规则（可匹配所有妈妈驿站格式短信，提取驿站、取件码、地址）
        $pattern = '/【(妈妈驿站)】取货码([\d\-]+)，您有(.+?)包裹，已到(.+)/';
        $rules[] = [
            'name' => "通用-妈妈驿站",
            'rule_type' => 'regex',
            'pattern' => $pattern,
            'description' => "通用规则，用于匹配妈妈驿站格式的短信，提取驿站、取件码和地址"
        ];
        
        // 生成自定义规则
        $rules[] = [
            'name' => "通用-妈妈驿站(自定义)",
            'rule_type' => 'custom',
            'code_prefix' => '取货码',
            'code_suffix' => '，',
            'station_prefix' => '【',
            'station_suffix' => '】',
            'address_prefix' => '已到',
            'address_suffix' => '',
            'description' => "通用自定义规则，用于匹配妈妈驿站格式的短信，提取驿站、取件码和地址"
        ];
    }
    
    // 检查是否为典型的正则表达式格式短信
    // 示例: 【菜鸟驿站】取件码:123456到地址:北京市朝阳区某某街道123号，凭123456来取
    else if (preg_match('/【(.+?)】.*取件码[:：](\w+).*地址[:：]([^，。]+)/u', $sms_content, $matches)) {
        $station_name = $matches[1];
        
        // 生成通用正则规则（提取驿站、取件码、地址）
        $pattern = '/【(.+?)】.*取件码[:：](\w+).*地址[:：]([^，。]+)/';
        $rules[] = [
            'name' => "通用-{$station_name}",
            'rule_type' => 'regex',
            'pattern' => $pattern,
            'description' => "通用规则，用于匹配{$station_name}格式的短信，提取驿站、取件码和地址"
        ];
        
        // 生成自定义规则
        $rules[] = [
            'name' => "通用-{$station_name}(自定义)",
            'rule_type' => 'custom',
            'code_prefix' => '取件码:',
            'code_suffix' => '到',
            'station_prefix' => '【',
            'station_suffix' => '】',
            'address_prefix' => '地址:',
            'address_suffix' => '，',
            'description' => "通用自定义规则，用于匹配{$station_name}格式的短信，提取驿站、取件码和地址"
        ];
    }
    
    // 尝试基于模式生成通用规则
    // 检测是否有标签、取件码和地址模式
    else {
        $has_tag = preg_match('/【(.+?)】/u', $sms_content, $tag_matches);
        $has_code = preg_match('/([\d\-]{4,12})/u', $sms_content, $code_matches);
        $has_address_keywords = preg_match('/(地址|到|已到|位于)[:：]?([^，。]+)/u', $sms_content, $address_matches);
        
        // 如果同时包含标签和取件码，生成通用规则
        if ($has_tag && $has_code) {
            $station_name = $tag_matches[1];
            
            // 生成基于标签和取件码的通用规则
            $pattern = '/【(.+?)】.*([\\d\\-]{4,12}).*/';
            $rules[] = [
                'name' => "通用-标签取件码规则",
                'rule_type' => 'regex',
                'pattern' => $pattern,
                'description' => "通用规则，匹配包含标签和取件码的短信"
            ];
        }
        
        // 如果包含取件码和地址关键词，生成通用规则
        if ($has_code && $has_address_keywords) {
            // 生成基于取件码和地址的通用规则
            $pattern = '/([\\d\\-]{4,12}).*(地址|到|已到|位于)[:：]([^，。]+)/';
            $rules[] = [
                'name' => "通用-取件码地址规则",
                'rule_type' => 'regex',
                'pattern' => $pattern,
                'description' => "通用规则，匹配包含取件码和地址的短信"
            ];
        }
        
        // 如果只包含取件码，生成通用取件码规则
        else if ($has_code) {
            // 生成通用取件码规则
            $pattern = '/([\\d\\-]{4,12})/';
            $rules[] = [
                'name' => '通用-取件码规则',
                'rule_type' => 'regex',
                'pattern' => $pattern,
                'description' => '通用规则，匹配包含取件码的短信'
            ];
        }
        
        // 如果包含标签，生成通用标签规则
        else if ($has_tag) {
            // 生成通用标签规则
            $pattern = '/【(.+?)】/';
            $rules[] = [
                'name' => "通用-标签规则",
                'rule_type' => 'regex',
                'pattern' => $pattern,
                'description' => '通用规则，匹配包含标签的短信'
            ];
        }
    }
    
    return $rules;
}
?>
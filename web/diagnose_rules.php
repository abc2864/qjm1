<?php
require_once 'config.php';

// 检查用户是否已登录
if (!isLoggedIn()) {
    echo "未登录\n";
    exit;
}

try {
    $pdo = getDBConnection();
    
    // 获取所有规则
    $stmt = $pdo->query("SELECT * FROM rules ORDER BY id DESC");
    $rules = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo "数据库中的规则列表:\n";
    echo "========================\n";
    
    foreach ($rules as $rule) {
        echo "ID: " . $rule['id'] . "\n";
        echo "名称: " . $rule['name'] . "\n";
        echo "类型: " . $rule['rule_type'] . "\n";
        echo "是否启用: " . ($rule['is_active'] ? '是' : '否') . "\n";
        
        if ($rule['rule_type'] === 'regex') {
            echo "正则表达式: " . $rule['pattern'] . "\n";
        } else {
            echo "取件码前缀: '" . $rule['code_prefix'] . "'\n";
            echo "取件码后缀: '" . $rule['code_suffix'] . "'\n";
            echo "驿站前缀: '" . $rule['station_prefix'] . "'\n";
            echo "驿站后缀: '" . $rule['station_suffix'] . "'\n";
            echo "地址前缀: '" . $rule['address_prefix'] . "'\n";
            echo "地址后缀: '" . $rule['address_suffix'] . "'\n";
        }
        
        echo "描述: " . $rule['description'] . "\n";
        echo "------------------------\n";
    }
    
    // 测试一个具体的规则
    if (!empty($rules)) {
        echo "\n测试第一个自定义规则:\n";
        echo "========================\n";
        
        foreach ($rules as $rule) {
            if ($rule['rule_type'] === 'custom') {
                echo "测试规则 ID: " . $rule['id'] . " - " . $rule['name'] . "\n";
                
                // 使用测试文本
                $testText = "【菜鸟驿站】取件码:123456到地址:北京市朝阳区某某街道123号，凭123456来取";
                echo "测试文本: " . $testText . "\n";
                
                // 手动测试前后缀匹配
                echo "\n手动测试结果:\n";
                
                // 测试取件码
                $code_prefix = $rule['code_prefix'] ?? '';
                $code_suffix = $rule['code_suffix'] ?? '';
                echo "取件码前缀: '$code_prefix'\n";
                echo "取件码后缀: '$code_suffix'\n";
                
                $matched = true;
                $extracted = $testText;
                
                // 处理前缀
                if (!empty($code_prefix)) {
                    if (strpos($testText, $code_prefix) === 0) {
                        $extracted = substr($extracted, strlen($code_prefix));
                        echo "前缀匹配成功，移除前缀后: '$extracted'\n";
                    } else {
                        $matched = false;
                        echo "前缀匹配失败\n";
                    }
                }
                
                // 处理后缀
                if ($matched && !empty($code_suffix)) {
                    if (strlen($extracted) >= strlen($code_suffix) && 
                        substr($extracted, -strlen($code_suffix)) === $code_suffix) {
                        $extracted = substr($extracted, 0, -strlen($code_suffix));
                        echo "后缀匹配成功，移除后缀后: '$extracted'\n";
                    } else {
                        $matched = false;
                        echo "后缀匹配失败\n";
                    }
                }
                
                if ($matched) {
                    $extracted = trim($extracted);
                    echo "最终提取结果: '$extracted'\n";
                } else {
                    echo "匹配失败\n";
                }
                
                break;
            }
        }
    }
    
} catch (PDOException $e) {
    echo "数据库错误: " . $e->getMessage() . "\n";
}
?>
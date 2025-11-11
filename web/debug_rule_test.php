<?php
require_once 'config.php';

// 检查用户是否已登录
if (!isLoggedIn()) {
    echo "未登录";
    exit;
}

function debugTestPrefixSuffix($text, $prefix, $suffix) {
    echo "<h4>调试信息</h4>";
    echo "<p><strong>输入文本:</strong> " . htmlspecialchars($text) . "</p>";
    echo "<p><strong>前缀:</strong> '" . htmlspecialchars($prefix) . "'</p>";
    echo "<p><strong>后缀:</strong> '" . htmlspecialchars($suffix) . "'</p>";
    
    // 如果前后缀都为空，则认为匹配成功（不需要提取任何内容）
    if (empty($prefix) && empty($suffix)) {
        echo "<p><strong>结果:</strong> 前后缀都为空，匹配成功</p>";
        return [
            'matched' => true,
            'extracted' => $text  // 返回原文本
        ];
    }
    
    $matched = true;
    $extracted = $text;
    
    echo "<h5>处理过程:</h5>";
    
    // 处理前缀
    if (!empty($prefix)) {
        $prefixPos = strpos($text, $prefix);
        echo "<p>查找前缀位置: " . ($prefixPos !== false ? "找到，位置在 $prefixPos" : "未找到") . "</p>";
        
        if ($prefixPos !== false) {
            // 文本包含指定前缀，移除前缀前的部分
            $extracted = substr($extracted, $prefixPos + strlen($prefix));
            echo "<p>移除前缀后的内容: '" . htmlspecialchars($extracted) . "'</p>";
        } else {
            // 文本不包含指定前缀，匹配失败
            $matched = false;
            echo "<p>前缀匹配失败</p>";
        }
    }
    
    // 处理后缀（仅在前缀匹配成功或没有前缀时处理）
    if ($matched && !empty($suffix)) {
        $suffixPos = strpos($extracted, $suffix);
        echo "<p>查找后缀位置: " . ($suffixPos !== false ? "找到，位置在 $suffixPos" : "未找到") . "</p>";
        
        if ($suffixPos !== false) {
            // 文本包含指定后缀，移除后缀后的部分
            $extracted = substr($extracted, 0, $suffixPos);
            echo "<p>移除后缀后的内容: '" . htmlspecialchars($extracted) . "'</p>";
        } else {
            // 文本不包含指定后缀，匹配失败
            $matched = false;
            echo "<p>后缀匹配失败</p>";
        }
    }
    
    // 清理提取结果（去除前后空格）
    if ($matched && is_string($extracted)) {
        $extracted = trim($extracted);
        echo "<p>清理空格后的内容: '" . htmlspecialchars($extracted) . "'</p>";
    }
    
    echo "<p><strong>最终结果:</strong> " . ($matched ? "匹配成功" : "匹配失败") . "</p>";
    if ($matched) {
        echo "<p><strong>提取内容:</strong> '" . htmlspecialchars($extracted) . "'</p>";
    }
    
    return [
        'matched' => $matched,
        'extracted' => $matched ? $extracted : null
    ];
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $text = $_POST['text'] ?? '';
    $prefix = $_POST['prefix'] ?? '';
    $suffix = $_POST['suffix'] ?? '';
    
    echo "<h3>规则匹配调试</h3>";
    debugTestPrefixSuffix($text, $prefix, $suffix);
    echo "<hr>";
}
?>

<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>规则匹配调试工具</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="mobile.css" rel="stylesheet">
</head>
<body>
    <div class="container mt-4">
        <h2>规则匹配调试工具</h2>
        
        <div class="card">
            <div class="card-body">
                <form method="POST">
                    <div class="mb-3">
                        <label for="text" class="form-label">输入文本</label>
                        <input type="text" class="form-control" id="text" name="text" value="【申通快递】请凭10-5-5913到韵达取件，地址：陕西省">
                    </div>
                    
                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label for="prefix" class="form-label">前缀</label>
                                <input type="text" class="form-control" id="prefix" name="prefix" value="【">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label for="suffix" class="form-label">后缀</label>
                                <input type="text" class="form-control" id="suffix" name="suffix" value="】">
                            </div>
                        </div>
                    </div>
                    
                    <button type="submit" class="btn btn-primary">调试测试</button>
                    <button type="button" class="btn btn-secondary" onclick="fillExample()">填入示例</button>
                </form>
            </div>
        </div>
    </div>

    <script>
        function fillExample() {
            document.getElementById('text').value = "【申通快递】请凭10-5-5913到韵达取件，地址：陕西省";
            document.getElementById('prefix').value = "【";
            document.getElementById('suffix').value = "】";
        }
    </script>
</body>
</html>
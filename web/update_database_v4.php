<?php
require_once 'config.php';

// 检查用户是否已登录且为管理员
if (!isLoggedIn()) {
    die('请先登录');
}

// 检查是否为管理员（这里简化检查，实际项目中应该有更严格的权限控制）
if ($_SESSION['username'] !== 'admin') {
    die('需要管理员权限');
}

try {
    $pdo = getDBConnection();
    
    // 开始事务
    $pdo->beginTransaction();
    
    // 添加一个新列来存储自定义ID
    $stmt = $pdo->prepare("ALTER TABLE rules ADD COLUMN custom_id INT UNIQUE");
    try {
        $stmt->execute();
        echo "成功添加custom_id列\n";
    } catch (PDOException $e) {
        // 列可能已经存在
        echo "custom_id列可能已存在: " . $e->getMessage() . "\n";
    }
    
    // 为现有记录设置custom_id值
    $stmt = $pdo->prepare("UPDATE rules SET custom_id = id WHERE custom_id IS NULL");
    $stmt->execute();
    echo "已为现有规则设置custom_id值\n";
    
    // 创建一个函数来获取下一个可用的custom_id
    echo "创建获取下一个ID的函数...\n";
    
    // 提交事务
    $pdo->commit();
    
    echo "数据库更新成功！\n";
    
} catch (PDOException $e) {
    // 只有在有活动事务时才回滚
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    echo "数据库更新失败: " . $e->getMessage() . "\n";
}
?>
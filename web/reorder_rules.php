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
    
    // 获取所有规则ID，按ID排序
    $stmt = $pdo->query("SELECT id FROM rules ORDER BY id ASC");
    $rule_ids = $stmt->fetchAll(PDO::FETCH_COLUMN);
    
    if (!empty($rule_ids)) {
        // 创建新ID映射
        $new_id = 1;
        $id_mapping = [];
        
        foreach ($rule_ids as $old_id) {
            $id_mapping[$old_id] = $new_id;
            $new_id++;
        }
        
        // 更新所有规则的ID
        foreach ($id_mapping as $old_id => $new_id) {
            if ($old_id != $new_id) {
                $stmt = $pdo->prepare("UPDATE rules SET id = ? WHERE id = ?");
                $stmt->execute([$new_id, $old_id]);
            }
        }
        
        // 重置自增计数器（注意：ALTER TABLE不支持预处理语句的参数绑定）
        $max_id = max($id_mapping);
        $pdo->query("ALTER TABLE rules AUTO_INCREMENT = " . ($max_id + 1));
        
        echo "规则ID已重新排列完成。新的ID映射：\n";
        foreach ($id_mapping as $old_id => $new_id) {
            echo "旧ID: $old_id -> 新ID: $new_id\n";
        }
    } else {
        echo "没有规则需要重新排列。\n";
    }
    
    // 提交事务
    $pdo->commit();
    
    echo "数据库更新成功！\n";
    
} catch (PDOException $e) {
    // 回滚事务
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    echo "数据库更新失败: " . $e->getMessage() . "\n";
}
?>
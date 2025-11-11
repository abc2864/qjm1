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

function reorderTableIds($pdo, $table_name) {
    // 开始事务
    $pdo->beginTransaction();
    
    try {
        // 获取所有记录ID，按ID排序
        $stmt = $pdo->query("SELECT id FROM $table_name ORDER BY id ASC");
        $ids = $stmt->fetchAll(PDO::FETCH_COLUMN);
        
        if (!empty($ids)) {
            // 创建新ID映射
            $new_id = 1;
            $id_mapping = [];
            
            foreach ($ids as $old_id) {
                $id_mapping[$old_id] = $new_id;
                $new_id++;
            }
            
            // 更新所有记录的ID
            foreach ($id_mapping as $old_id => $new_id) {
                if ($old_id != $new_id) {
                    $stmt = $pdo->prepare("UPDATE $table_name SET id = ? WHERE id = ?");
                    $stmt->execute([$new_id, $old_id]);
                }
            }
            
            // 重置自增计数器（注意：ALTER TABLE不支持预处理语句的参数绑定）
            $max_id = max($id_mapping);
            $pdo->query("ALTER TABLE $table_name AUTO_INCREMENT = " . ($max_id + 1));
            
            echo "表 $table_name 的ID已重新排列完成。\n";
            foreach ($id_mapping as $old_id => $new_id) {
                echo "  旧ID: $old_id -> 新ID: $new_id\n";
            }
        } else {
            echo "表 $table_name 没有记录需要重新排列。\n";
        }
        
        // 提交事务
        $pdo->commit();
        
    } catch (PDOException $e) {
        // 回滚事务
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        throw $e;
    }
}

try {
    $pdo = getDBConnection();
    
    echo "开始重新排列数据库中的ID...\n\n";
    
    // 重新排列规则表的ID
    reorderTableIds($pdo, 'rules');
    
    echo "\n";
    
    // 重新排列关键词表的ID
    reorderTableIds($pdo, 'keywords');
    
    echo "\n所有表的ID重新排列完成！\n";
    
} catch (PDOException $e) {
    // 只有在有活动事务时才回滚
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    echo "操作失败: " . $e->getMessage() . "\n";
}
?>
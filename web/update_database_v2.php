<?php
// 数据库更新脚本 V2 - 移除发件人字段
require_once 'config.php';

try {
    $pdo = getDBConnection();
    
    // 开始事务
    $pdo->beginTransaction();
    
    // 1. 创建新的规则表（不包含发件人字段）
    $pdo->exec("
        CREATE TABLE IF NOT EXISTS rules_new (
            id INT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            rule_type ENUM('regex', 'custom') NOT NULL DEFAULT 'regex',
            -- 正则规则字段
            pattern TEXT,
            -- 自定义前后缀规则字段（移除发件人字段）
            code_prefix VARCHAR(100),
            code_suffix VARCHAR(100),
            station_prefix VARCHAR(100),
            station_suffix VARCHAR(100),
            address_prefix VARCHAR(100),
            address_suffix VARCHAR(100),
            description TEXT,
            is_active TINYINT(1) DEFAULT 1,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
    ");
    
    // 2. 复制现有数据到新表（忽略发件人字段）
    $pdo->exec("
        INSERT INTO rules_new (
            id, name, rule_type, pattern, 
            code_prefix, code_suffix, station_prefix, station_suffix, 
            address_prefix, address_suffix, description, is_active, 
            created_at, updated_at
        )
        SELECT 
            id, name, rule_type, pattern,
            code_prefix, code_suffix, station_prefix, station_suffix,
            address_prefix, address_suffix, description, is_active,
            created_at, updated_at
        FROM rules
    ");
    
    // 3. 删除旧表
    $pdo->exec("DROP TABLE rules");
    
    // 4. 重命名新表
    $pdo->exec("ALTER TABLE rules_new RENAME TO rules");
    
    // 提交事务
    $pdo->commit();
    
    echo "数据库更新成功！发件人字段已移除。\n";
    
} catch (Exception $e) {
    // 回滚事务
    if ($pdo->inTransaction()) {
        $pdo->rollback();
    }
    echo "数据库更新失败: " . $e->getMessage() . "\n";
}
?>
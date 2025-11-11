-- 创建数据库
CREATE DATABASE IF NOT EXISTS pickup_code_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE pickup_code_db;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 规则表（增强版，支持正则规则和自定义前后缀规则，移除发件人字段）
CREATE TABLE IF NOT EXISTS rules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    rule_type ENUM('regex', 'custom') NOT NULL DEFAULT 'regex', -- 规则类型：regex=正则规则, custom=自定义前后缀规则
    -- 正则规则字段
    pattern TEXT, -- 正则表达式模式
    -- 自定义前后缀规则字段（移除发件人字段）
    code_prefix VARCHAR(100), -- 取件码前缀
    code_suffix VARCHAR(100), -- 取件码后缀
    station_prefix VARCHAR(100), -- 驿站前缀
    station_suffix VARCHAR(100), -- 驿站后缀
    address_prefix VARCHAR(100), -- 地址前缀
    address_suffix VARCHAR(100), -- 地址后缀
    description TEXT,
    is_active TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 关键词表
CREATE TABLE IF NOT EXISTS keywords (
    id INT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(100) NOT NULL UNIQUE,
    type ENUM('sender', 'content') NOT NULL DEFAULT 'content',
    description TEXT,
    is_active TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 插入默认管理员用户 (用户名: admin, 密码: admin123)
-- 使用PHP password_hash('admin123', PASSWORD_DEFAULT) 生成的哈希值
INSERT INTO users (username, password) VALUES ('admin', '$2y$10$u4BBr4.EzHh6bFpEi0.PUO9zH3kHEcT8jYCE1gQe20hGzV3.Eu1bG');

-- 插入一些示例正则规则
INSERT INTO rules (name, rule_type, pattern, description, is_active) VALUES 
('顺丰速运', 'regex', '顺丰速运.*取件码.*([A-Z0-9]{4,6})', '匹配顺丰速运的取件短信', 1),
('菜鸟驿站', 'regex', '【菜鸟驿站】.*取件码.*([A-Z0-9]{4,6})', '匹配菜鸟驿站的取件短信', 1),
('京东物流', 'regex', '京东物流.*验证码.*([A-Z0-9]{4,6})', '匹配京东物流的取件短信', 1);

-- 插入一些示例自定义前后缀规则（移除发件人字段）
INSERT INTO rules (name, rule_type, code_prefix, code_suffix, station_prefix, station_suffix, address_prefix, address_suffix, description, is_active) VALUES 
('自定义规则示例', 'custom', '取件码:', '到', '地址:', '，', '凭', '来取', '自定义前后缀规则示例', 1);
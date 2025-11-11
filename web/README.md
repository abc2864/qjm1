# Web规则管理系统

这是一个用于管理取件码解析规则和关键词的Web管理系统，支持PHP+MySQL技术栈。

## 功能特性

1. 用户登录认证系统
2. 规则管理（增删改查）
3. 关键词管理（增删改查）
4. 数据导出为JSON格式
5. 提供API接口供App端下载规则和关键词
6. API密钥认证机制
7. 支持离线使用

## 部署说明

### 环境要求

- PHP 7.0 或更高版本
- MySQL 5.6 或更高版本
- Web服务器（Apache/Nginx）

### 安装步骤

1. 将web目录中的所有文件复制到Web服务器目录中
2. 创建MySQL数据库并导入 [database.sql](database.sql) 文件
3. 修改 [config.php](config.php) 文件中的数据库连接配置
4. 启动Web服务器

### 默认登录信息

- 用户名: admin
- 密码: admin123

建议在首次登录后立即修改密码。

## 故障排除

### 数据库导入后为空的问题

如果在导入 [database.sql](database.sql) 文件后发现数据库为空，请尝试以下解决方案：

1. **使用修复脚本**：
   - 运行 `fix_database.bat` (Windows) 或执行 `php fix_database.php` (Linux/Mac)
   - 该脚本会自动创建数据库表并插入默认用户

2. **手动导入**：
   - 登录MySQL：`mysql -u root -p`
   - 选择数据库：`USE pickup_code_db;`
   - 手动执行SQL语句：
     ```sql
     CREATE TABLE IF NOT EXISTS users (
         id INT AUTO_INCREMENT PRIMARY KEY,
         username VARCHAR(50) NOT NULL UNIQUE,
         password VARCHAR(255) NOT NULL,
         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
     );
     
     INSERT INTO users (username, password) VALUES ('admin', '$2y$10$u4BBr4.EzHh6bFpEi0.PUO9zH3kHEcT8jYCE1gQe20hGzV3.Eu1bG');
     ```

## API接口

### 获取规则和关键词数据

有两种方式可以访问API接口：

1. **传统会话认证方式**（需要登录Web界面）
   ```
   GET /api.php
   ```

2. **API密钥认证方式**（推荐用于App端）
   ```
   GET /api.php?api_key=YOUR_API_KEY
   ```
   或使用HTTP头部：
   ```
   X-API-Key: YOUR_API_KEY
   ```

返回JSON格式的数据，包含规则和关键词信息。

## 数据结构

### 规则表 (rules)

| 字段名 | 类型 | 描述 |
|--------|------|------|
| id | INT | 主键，自增ID |
| name | VARCHAR(100) | 规则名称 |
| pattern | TEXT | 正则表达式模式 |
| description | TEXT | 规则描述 |
| is_active | TINYINT(1) | 是否启用 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

### 关键词表 (keywords)

| 字段名 | 类型 | 描述 |
|--------|------|------|
| id | INT | 主键，自增ID |
| keyword | VARCHAR(100) | 关键词内容 |
| type | ENUM('sender', 'content') | 关键词类型 |
| description | TEXT | 关键词描述 |
| is_active | TINYINT(1) | 是否启用 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

#### 常见登录问题及解决方案：

1. **用户名或密码错误**：
   - 确保输入的用户名和密码完全正确
   - 检查是否有大小写问题
   - 运行重置脚本重新设置密码

2. **数据库连接失败**：
   - 检查 [config.php](config.php) 中的数据库配置
   - 确认MySQL服务正在运行
   - 验证数据库用户权限

3. **用户表不存在**：
   - 运行修复脚本创建数据库表
   - 手动执行SQL创建表语句

4. **密码验证失败**：
   - 运行重置脚本重新生成密码哈希
   - 确保使用的是正确的密码哈希算法

## App端集成

App端可以通过以下方式集成：

1. 定期从 `/api.php` 接口下载最新的规则和关键词数据
2. 将下载的数据保存到本地文件系统
3. 在无网络情况下使用本地保存的数据进行解析
4. 当网络恢复时，再次同步最新的数据

## 安全说明

1. 所有密码都经过哈希处理存储
2. 用户会话管理采用PHP原生Session机制
3. 建议在生产环境中使用HTTPS协议
4. 定期更新密码，避免使用默认密码

## 维护说明

1. 定期备份数据库
2. 监控API接口的访问日志
3. 及时更新规则和关键词以适应新的短信格式
# 短信权限应用

这是一个简单的Android应用，它会在启动时自动弹窗申请短信权限，获得权限后在主页显示短信数量，并提供检查权限的按钮。

## 功能

1. 应用启动时自动申请短信读取权限
2. 获得权限后显示设备中的短信数量
3. 提供检查权限状态的按钮
4. 智能解析快递短信并提取取件码信息
5. Web规则管理系统，用于管理取件码解析规则和关键词

## 使用说明

1. 启动应用后，系统会自动弹出权限申请对话框
2. 授予短信读取权限后，应用会显示短信数量
3. 可以点击"检查短信权限"按钮来检查当前权限状态

## 注意事项

- 该应用需要读取短信的权限才能正常工作
- 应用只会读取短信数量，不会读取短信内容
- 请确保在支持的Android版本上运行（minSdk 33）

## 取件码解析优化

本应用针对取件码解析进行了多项优化，解决了以下问题：

### 已修复的问题

1. **菜鸟驿站短信问题**：
   - 问题：取件码被错误识别为"Z-03-1819"和"8951"
   - 修复：改进正则表达式和上下文判断，只识别真正的取件码"Z-03-1819"

2. **营销短信误识别问题**：
   - 问题：中国移动营销短信被错误识别为快递信息，取件码识别为"5G"
   - 修复：增强过滤机制，排除包含"彩铃"、"视频"等营销关键词的短信

3. **时间范围误识别问题**：
   - 问题：兔喜生活短信中的时间范围"08:30-19:00"中的"30-19"被错误识别为取件码
   - 修复：改进时间范围的排除逻辑

### 优化特性

- 更精确的取件码识别算法
- 增强的垃圾短信过滤机制
- 改进的正则表达式匹配
- 更好的上下文理解能力

## Web规则管理系统

本项目包含一个完整的Web规则管理系统，用于管理取件码解析规则和关键词：

### 功能特性

1. 用户登录认证系统
2. 规则管理（增删改查）
3. 关键词管理（增删改查）
4. 数据导出为JSON格式
5. 提供API接口供App端下载规则和关键词
6. 支持离线使用

### 部署说明

1. 将[web](web)目录中的所有文件复制到Web服务器目录中
2. 创建MySQL数据库并导入[web/database.sql](web/database.sql)文件
3. 修改[web/config.php](web/config.php)文件中的数据库连接配置
4. 启动Web服务器

### 默认登录信息

- 用户名: admin
- 密码: admin123

建议在首次登录后立即修改密码。

### 常见问题及解决方案

#### 数据库导入后为空的问题

如果在导入 database.sql 文件后发现数据库为空，请尝试以下解决方案：

1. **使用修复脚本**：
   - 进入 web 目录并运行 `fix_database.bat` (Windows) 或执行 `php fix_database.php` (Linux/Mac)
   - 该脚本会自动创建数据库表并插入默认用户

2. **手动导入**：
   - 登录MySQL：`mysql -u root -p`
   - 选择数据库：`USE pickup_code_db;`
   - 手动执行SQL语句创建表和用户

#### 用户名密码无法登录的问题

如果使用默认凭据无法登录，请按以下步骤进行诊断和修复：

1. **运行诊断工具**：
   - 进入 web 目录并运行 `diagnose_login.bat` (Windows) 或执行 `php diagnose_login.php` (Linux/Mac)
   - 该工具将检查数据库连接、用户表和密码验证

2. **重置管理员用户**：
   - 运行 `reset_admin_user.bat` (Windows) 或执行 `php reset_admin_user.php` (Linux/Mac)
   - 该脚本将创建或重置admin用户

3. **常见登录问题检查**：
   - 确保输入的用户名和密码完全正确（注意大小写）
   - 检查 [web/config.php](web/config.php) 中的数据库配置是否正确
   - 确认MySQL服务正在运行

详细故障排除指南请参考 [web/README.md](web/README.md) 文件。

## 构建和运行

使用Android Studio打开项目并运行，或者使用以下命令：

```bash
./gradlew assembleDebug
```

## 国内源配置

为了提高依赖下载速度，项目已配置使用国内镜像源：

1. 阿里云Maven仓库已添加到[settings.gradle.kts](file:///c:/Users/Administrator/Downloads/qujianma/settings.gradle.kts)和[build.gradle.kts](file:///c:/Users/Administrator/Downloads/qujianma/app/build.gradle.kts)
2. [gradle.properties](file:///c:/Users/Administrator/Downloads/qujianma/gradle.properties)中配置了代理和性能优化参数

这些配置将显著提高在中国大陆地区的依赖下载速度。

## GitHub Actions 自动编译

本项目已配置GitHub Actions，可以自动编译和测试应用：

1. 每次推送到main分支时自动触发构建
2. 自动生成Debug APK文件
3. 运行单元测试
4. 构建产物可在Actions页面下载

详细推送步骤请参考 [GITHUB_INSTRUCTIONS.md](file:///c:/Users/Administrator/Downloads/qujianma/GITHUB_INSTRUCTIONS.md) 文件。
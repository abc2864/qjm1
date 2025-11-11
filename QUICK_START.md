# 快速开始指南

## 问题诊断

如果您遇到"NetworkError when attempting to fetch resource"错误，请按照以下步骤排查：

### 1. 使用调试工具诊断

打开 [simple-backend/debug.html](file:///c:/Users/Administrator/Downloads/qujianma/simple-backend/debug.html) 文件，按照以下步骤操作：

1. 点击"检查服务状态"按钮
2. 如果失败，检查后端服务是否启动
3. 如果成功，继续测试其他功能

### 2. 启动后端服务

#### 方法一：使用启动脚本（推荐）
双击运行 [simple-backend/start.bat](file:///c:/Users/Administrator/Downloads/qujianma/simple-backend/start.bat) 文件

#### 方法二：手动启动
```bash
# 打开命令提示符（cmd）
cd c:\Users\Administrator\Downloads\qujianma\simple-backend

# 安装依赖（首次运行时）
npm install

# 启动服务
npm start
```

启动成功后，您应该看到：
```
简易后端服务器运行在端口 3001
```

### 3. 使用管理界面

1. 启动后端服务后，打开 [simple-frontend/index.html](file:///c:/Users/Administrator/Downloads/qujianma/simple-frontend/index.html) 文件
2. 使用以下凭据登录：
   - 用户名：admin
   - 密码：admin123

### 4. 常见问题解决

#### 问题1：端口被占用
如果端口3001被占用，修改 [server.js](file:///c:/Users/Administrator/Downloads/qujianma/simple-backend/server.js) 文件中的端口号：
```javascript
const PORT = process.env.PORT || 3002; // 改为3002或其他可用端口
```

#### 问题2：Node.js未安装
从 https://nodejs.org/ 下载并安装Node.js

#### 问题3：防火墙阻止
将Node.js添加到防火墙例外中，或临时关闭防火墙测试

#### 问题4：CORS错误
确保 [server.js](file:///c:/Users/Administrator/Downloads/qujianma/simple-backend/server.js) 中包含：
```javascript
const cors = require('cors');
app.use(cors());
```

## 系统功能

### 后端API
- 管理员认证：`POST /api/admin/login`
- 规则管理：`GET/POST/PUT/DELETE /api/admin/rules`
- 关键词管理：`GET/POST/PUT/DELETE /api/admin/keywords`
- API密钥管理：`GET/POST/DELETE /api/admin/apikeys`
- 公开API：`GET /api/public/rules` 和 `GET /api/public/keywords`（需要API密钥）

### Web管理界面
- 管理员登录/退出
- 规则的增删改查
- 关键词的增删改查
- API密钥的生成和管理

### Android应用集成
使用 [simple-android/SyncManager.kt](file:///c:/Users/Administrator/Downloads/qujianma/simple-android/SyncManager.kt) 文件集成数据同步功能

## 测试步骤

1. 启动后端服务
2. 打开调试工具 [debug.html](file:///c:/Users/Administrator/Downloads/qujianma/simple-backend/debug.html) 检查服务状态
3. 打开管理界面 [index.html](file:///c:/Users/Administrator/Downloads/qujianma/simple-frontend/index.html) 登录系统
4. 创建一些测试规则和关键词
5. 生成API密钥用于Android应用
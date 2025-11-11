# 部署指南

## 系统架构概述

本系统包含以下组件：
1. Android应用端 - 用于解析短信和管理取件信息
2. Web管理后台 - 用于管理规则、关键词和API密钥
3. 后端API服务 - 提供数据存储和API接口
4. MongoDB数据库 - 存储规则、关键词和用户数据

## 部署步骤

### 1. 环境准备

确保安装了以下软件：
- Node.js (v14或更高版本)
- MongoDB (v4.4或更高版本)
- Android Studio (用于构建Android应用)

### 2. 后端服务部署

1. 进入后端目录：
   ```
   cd web-backend
   ```

2. 安装依赖：
   ```
   npm install
   ```

3. 配置环境变量：
   - 复制 [.env.example](file:///c:/Users/Administrator/Downloads/qujianma/web-backend/.env.example) 文件为 [.env](file:///c:/Users/Administrator/Downloads/qujianma/web-backend/.env)
   - 修改配置参数：
     ```
     PORT=3001
     MONGODB_URI=mongodb://localhost:27017/qujianma
     JWT_SECRET=your_jwt_secret_key
     ADMIN_USERNAME=admin
     ADMIN_PASSWORD=your_admin_password
     ```

4. 启动服务：
   ```
   npm start
   ```

### 3. Web前端部署

1. 进入前端目录：
   ```
   cd web-frontend
   ```

2. 安装依赖：
   ```
   npm install
   ```

3. 构建生产版本：
   ```
   npm run build
   ```

4. 部署构建文件到Web服务器（如Nginx、Apache等）

### 4. Android应用部署

1. 使用Android Studio打开项目
2. 构建APK文件：
   - 选择 Build > Build Bundle(s) / APK(s) > Build APK
3. 安装APK到Android设备

## 系统功能测试

### 1. Web管理后台测试

1. 访问管理后台：http://localhost:3000
2. 使用管理员账号登录
3. 测试以下功能：
   - 创建、编辑、删除规则
   - 创建、编辑、删除关键词分类
   - 生成、禁用、删除API密钥

### 2. Android应用测试

1. 安装应用到Android设备
2. 启动应用并授予短信权限
3. 测试以下功能：
   - 短信解析功能
   - 手动添加取件信息
   - 从服务器下载规则和关键词
   - 离线使用功能

### 3. API接口测试

1. 使用API密钥访问以下接口：
   - GET /api/public/rules - 获取所有启用的规则
   - GET /api/public/keywords - 获取所有关键词

## 常见问题解决

### 1. 后端服务无法启动

- 检查MongoDB是否正在运行
- 检查端口是否被占用
- 检查环境变量配置是否正确

### 2. Android应用无法连接到后端

- 检查网络连接
- 确保后端服务正在运行
- 检查API密钥是否正确

### 3. 数据同步问题

- 检查API密钥是否有效
- 检查网络连接
- 查看日志文件获取更多信息

## 安全建议

1. 使用强密码和JWT密钥
2. 定期更新API密钥
3. 限制API密钥的权限
4. 使用HTTPS加密通信
5. 定期备份数据库

## 维护和监控

1. 定期检查日志文件
2. 监控系统性能
3. 定期备份数据库
4. 及时更新依赖包
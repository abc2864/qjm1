# 简化版系统部署指南

## 系统概述

这是一个简化版的快递取件码管理系统，包含以下组件：
1. 简化后端服务（Node.js + Express）
2. 简化Web管理界面（纯HTML+JavaScript）
3. Android应用数据同步功能

## 部署步骤

### 1. 部署简化后端服务

1. 进入简化后端目录：
   ```
   cd simple-backend
   ```

2. 安装依赖：
   ```
   npm install
   ```

3. 启动服务：
   ```
   npm start
   ```

   默认运行在端口3001

### 2. 使用简化Web管理界面

1. 直接在浏览器中打开 [simple-frontend/index.html](file:///c:/Users/Administrator/Downloads/qujianma/simple-frontend/index.html) 文件
2. 或者将simple-frontend目录部署到Web服务器

### 3. 配置Android应用

1. 将 [simple-android/SyncManager.kt](file:///c:/Users/Administrator/Downloads/qujianma/simple-android/SyncManager.kt) 文件集成到您的Android项目中
2. 确保添加了网络权限和OkHttp依赖

## 使用说明

### 1. 管理员登录

- 用户名：admin
- 密码：admin123

### 2. 功能说明

#### 规则管理
- 添加、编辑、删除解析规则
- 控制规则启用/禁用状态

#### 关键词管理
- 管理短信分类关键词
- 支持多个关键词的分类

#### API密钥管理
- 生成用于Android应用访问的API密钥
- 控制API密钥的启用/禁用状态

### 3. Android应用数据同步

1. 在Web管理后台生成API密钥
2. 在Android应用中输入API密钥
3. 应用会自动从服务器下载规则和关键词
4. 支持离线使用已下载的数据

## 优势特点

1. **简化架构**：无需复杂数据库，使用JSON文件存储数据
2. **易于部署**：只需要Node.js环境即可运行
3. **轻量级**：代码简洁，功能明确
4. **快速上手**：无需复杂配置，开箱即用
5. **前后端分离**：Web界面与后端服务完全分离

## 注意事项

1. 此为简化版本，适用于测试和小规模使用
2. 生产环境建议使用完整版系统
3. 数据存储在本地JSON文件中，重启服务数据不会丢失
4. API密钥请妥善保管，避免泄露
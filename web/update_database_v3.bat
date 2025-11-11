@echo off
title 数据库更新脚本 V3 - API密钥支持

echo 正在更新数据库以支持API密钥功能...

REM 检查PHP是否已安装
where php >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未安装PHP或未添加到PATH环境变量
    pause
    exit /b 1
)

REM 运行数据库更新脚本
echo 正在运行数据库更新脚本...
php update_database_v3.php

echo.
echo 数据库更新完成！
echo.
echo 现在支持API密钥认证功能。
echo.
echo 请执行以下操作：
echo 1. 登录Web管理系统
echo 2. 在主页点击"API密钥管理"
echo 3. 生成新的API密钥
echo 4. 在App端配置使用该API密钥

pause
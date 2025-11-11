@echo off
title 重置管理员用户

echo 正在重置管理员用户...

REM 检查PHP是否已安装
where php >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未安装PHP或未添加到PATH环境变量
    pause
    exit /b 1
)

REM 运行重置脚本
echo 正在运行重置脚本...
php reset_admin_user.php

echo.
echo 重置完成！
echo.
echo 默认登录凭据:
echo 用户名: admin
echo 密码: admin123
echo.
echo 请使用以上凭据登录系统，登录后请立即修改密码以确保安全。

pause
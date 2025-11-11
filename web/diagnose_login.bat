@echo off
title 登录问题诊断工具

echo 正在诊断登录问题...

REM 检查PHP是否已安装
where php >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未安装PHP或未添加到PATH环境变量
    pause
    exit /b 1
)

REM 运行诊断脚本
echo 正在运行诊断脚本...
php diagnose_login.php

echo.
echo 诊断完成！
echo.
echo 根据诊断结果采取相应措施：
echo 1. 如果提示重置密码，请运行 reset_admin_user.bat
echo 2. 如果提示创建用户，请运行 reset_admin_user.bat
echo 3. 如果数据库连接失败，请检查 config.php 中的配置

pause
@echo off
title 数据库修复脚本

echo 正在修复数据库...

REM 检查PHP是否已安装
where php >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未安装PHP或未添加到PATH环境变量
    pause
    exit /b 1
)

REM 运行数据库修复脚本
echo 正在运行数据库修复脚本...
php fix_database.php

echo.
echo 数据库修复完成！
echo.
echo 请确保您的数据库配置正确：
echo - 数据库主机: 8.137.57.54
echo - 数据库用户名: root
echo - 数据库密码: (空)
echo - 数据库名称: pickup_code_db
echo.
echo 如果您的数据库配置不同，请修改 config.php 文件。

pause
@echo off
title 数据库更新脚本

echo 正在更新数据库以支持增强的规则功能...

REM 检查PHP是否已安装
where php >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未安装PHP或未添加到PATH环境变量
    pause
    exit /b 1
)

REM 运行数据库更新脚本
echo 正在运行数据库更新脚本...
php update_database.php

echo.
echo 数据库更新完成！
echo.
echo 现在支持两种规则类型：
echo 1. 正则规则 - 使用正则表达式匹配
echo 2. 自定义前后缀规则 - 使用前后缀匹配
echo.
echo 请刷新规则管理页面查看新功能。

pause
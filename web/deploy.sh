#!/bin/bash

# Web规则管理系统部署脚本

echo "开始部署Web规则管理系统..."

# 检查是否安装了必要的软件
if ! command -v php &> /dev/null
then
    echo "错误: 未安装PHP"
    exit 1
fi

if ! command -v mysql &> /dev/null
then
    echo "错误: 未安装MySQL"
    exit 1
fi

# 创建数据库
echo "正在创建数据库..."
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS pickup_code_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入数据库结构
echo "正在导入数据库结构..."
mysql -u root -p pickup_code_db < database.sql

# 提示用户修改配置文件
echo "请修改config.php文件中的数据库连接信息"

# 提示部署完成
echo "部署完成！请将web目录中的所有文件复制到Web服务器目录中，并确保Web服务器已启动。"

echo "默认登录信息:"
echo "用户名: admin"
echo "密码: admin123"
echo "建议在首次登录后立即修改密码。"
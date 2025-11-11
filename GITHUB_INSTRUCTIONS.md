# 推送项目到GitHub的步骤

## 1. 在GitHub上创建新仓库
1. 登录到GitHub
2. 点击右上角的"+"号，选择"New repository"
3. 输入仓库名称（例如：qujianma）
4. 选择公开或私有（根据需要）
5. 不要初始化README、.gitignore或license
6. 点击"Create repository"

## 2. 将本地仓库推送到GitHub
在项目根目录执行以下命令：

```bash
git remote add origin https://github.com/你的用户名/仓库名.git
git branch -M main
git push -u origin main
```

## 3. GitHub Actions自动编译
本项目已配置GitHub Actions，在每次推送到main分支时会自动执行以下操作：
- 构建项目
- 运行测试
- 生成Debug APK
- 上传APK作为构建产物

## 4. 查看构建结果
1. 在GitHub仓库页面点击"Actions"选项卡
2. 选择相应的工作流运行
3. 查看构建日志和结果
4. 在工作流完成后的"Artifacts"部分可以下载生成的APK文件
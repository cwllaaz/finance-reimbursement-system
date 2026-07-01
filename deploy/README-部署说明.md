# 财务报销与预算管理系统部署说明

这份文档用于把当前项目部署到一台 Linux 云服务器上。推荐系统为 Ubuntu 22.04 / 24.04。

## 1. 服务器需要准备什么

服务器需要安装：

- JDK 21
- MySQL 8
- Nginx
- Node.js 20 或更新版本
- Maven 可以不用单独安装，后端自带 `mvnw`

常见安装命令示例：

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk mysql-server nginx unzip
```

Node.js 建议用官方 NodeSource 或 nvm 安装。只要服务器执行 `node -v`、`npm -v` 能正常显示版本即可。

## 2. 推荐的服务器目录结构

```text
/opt/finance-reimbursement-system
├── backend
│   └── backend.jar
└── uploads

/var/www/finance-reimbursement-system
└── Vue 打包后的 dist 文件

/etc/finance-reimbursement-system
└── backend.env
```

含义：

- `/opt/finance-reimbursement-system/backend`：放 Spring Boot 后端 jar。
- `/opt/finance-reimbursement-system/uploads`：放发票图片和附件。
- `/var/www/finance-reimbursement-system`：放 Vue 前端静态文件。
- `/etc/finance-reimbursement-system/backend.env`：放线上环境变量。

## 3. 本地开发配置

本地开发仍然按原来的方式启动：

```powershell
cd "C:\Users\13289\Desktop\极客工坊文件\第一次作业\finance-reimbursement-system"
.\start-backend.ps1
```

再开一个 PowerShell：

```powershell
cd "C:\Users\13289\Desktop\极客工坊文件\第一次作业\finance-reimbursement-system"
.\start-frontend.ps1
```

前端现在默认请求 `/api`，本地由 Vite 自动代理到 `http://localhost:8080`。所以本地访问仍然是：

```text
http://localhost:5173
```

## 4. 生产环境配置文件

后端新增了：

```text
backend/src/main/resources/application-prod.properties
```

线上启动时使用：

```bash
--spring.profiles.active=prod
```

它会从环境变量读取数据库密码、上传目录、登录 token 有效期和百度 OCR 配置。默认 token 有效期为 120 分钟，可通过 `AUTH_TOKEN_TTL_MINUTES` 调整。

系统启动时会自动将旧用户的明文密码迁移为 BCrypt，不需要手工重置账号密码。上传目录不能再由 Nginx 公开暴露，附件下载统一通过 `/api/attachments/.../download` 完成登录和业务权限校验。

前端新增了：

```text
frontend/.env.production
```

生产环境接口地址是：

```text
/api
```

这表示浏览器访问网站时，请求会由 Nginx 转发到后端。

## 5. 打包前端

在项目的 `frontend` 目录执行：

```bash
npm install
npm run build
```

打包完成后会生成：

```text
frontend/dist
```

把 `dist` 里面的文件上传到服务器：

```bash
sudo mkdir -p /var/www/finance-reimbursement-system
sudo cp -r dist/* /var/www/finance-reimbursement-system/
```

## 6. 打包后端

在项目的 `backend` 目录执行：

```bash
./mvnw -DskipTests package
```

Windows 本地执行：

```powershell
.\mvnw.cmd -DskipTests package
```

打包后 jar 通常在：

```text
backend/target/backend-0.0.1-SNAPSHOT.jar
```

上传到服务器并改名：

```bash
sudo mkdir -p /opt/finance-reimbursement-system/backend
sudo cp backend-0.0.1-SNAPSHOT.jar /opt/finance-reimbursement-system/backend/backend.jar
```

## 7. 配置 MySQL

建议线上不要使用 root 账号运行项目。可以创建专门账号：

```sql
CREATE DATABASE IF NOT EXISTS finance_reimbursement
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'finance_app'@'localhost' IDENTIFIED BY '换成你的强密码';
GRANT ALL PRIVILEGES ON finance_reimbursement.* TO 'finance_app'@'localhost';
FLUSH PRIVILEGES;
```

如果要导入本地备份数据库：

```bash
mysql -u finance_app -p finance_reimbursement < finance_reimbursement_backup.sql
```

## 8. 配置后端环境变量

复制模板：

```bash
sudo mkdir -p /etc/finance-reimbursement-system
sudo cp deploy/backend.env.example /etc/finance-reimbursement-system/backend.env
sudo nano /etc/finance-reimbursement-system/backend.env
```

需要重点修改：

```text
MYSQL_PASSWORD=你的数据库密码
BAIDU_OCR_API_KEY=你的百度OCR API Key
BAIDU_OCR_SECRET_KEY=你的百度OCR Secret Key
```

如果暂时不使用百度 OCR，两个 OCR 配置可以留空，系统会继续使用模拟 OCR。

## 9. 配置上传目录

```bash
sudo mkdir -p /opt/finance-reimbursement-system/uploads
sudo chown -R finance:finance /opt/finance-reimbursement-system
```

如果服务器还没有 `finance` 用户，可以创建：

```bash
sudo useradd -r -s /usr/sbin/nologin finance
```

## 10. 配置 systemd 后端服务

复制服务模板：

```bash
sudo cp deploy/systemd/finance-backend.service /etc/systemd/system/finance-backend.service
sudo systemctl daemon-reload
sudo systemctl enable finance-backend
sudo systemctl start finance-backend
```

查看启动状态：

```bash
sudo systemctl status finance-backend
```

查看日志：

```bash
sudo journalctl -u finance-backend -f
```

## 11. 配置 Nginx

复制 Nginx 模板：

```bash
sudo cp deploy/nginx/finance-reimbursement-system.conf /etc/nginx/sites-available/finance-reimbursement-system.conf
sudo ln -s /etc/nginx/sites-available/finance-reimbursement-system.conf /etc/nginx/sites-enabled/finance-reimbursement-system.conf
```

打开配置文件：

```bash
sudo nano /etc/nginx/sites-available/finance-reimbursement-system.conf
```

把：

```text
server_name your-domain.com;
```

改成你的域名。如果没有域名，可以先写服务器公网 IP。

测试并重启 Nginx：

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## 12. 上线后测试清单

打开网站后，按这个顺序测试：

1. 登录 `admin / 123456`
2. 首页仪表盘能显示数据和图表
3. 报销申请列表能加载
4. 员工账号能新增报销
5. 部门负责人能审批
6. 财务能审批并扣减预算
7. 财务或管理员能导出 Excel
8. 发票图片能上传和查看
9. OCR 表单能识别、保存、确认
10. 管理员能新增、修改、禁用用户

## 13. 常见问题

### 页面能打开，但接口失败

检查 Nginx 是否正确转发 `/api`：

```bash
curl http://127.0.0.1:8080/api/health
sudo nginx -t
```

### 后端启动失败

查看日志：

```bash
sudo journalctl -u finance-backend -n 100
```

重点看数据库账号、密码、库名是否正确。

### 上传图片看不到

检查：

```bash
ls -la /opt/finance-reimbursement-system/uploads
```

确认 `finance` 用户有写入权限。

### 修改前端后没有变化

需要重新执行：

```bash
npm run build
sudo cp -r dist/* /var/www/finance-reimbursement-system/
sudo systemctl reload nginx
```

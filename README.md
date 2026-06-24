# 财务报销与预算管理系统

这是一个使用 Vibe Coding 和 AI 辅助完成的大学生练习项目，主题为“财务报销与预算管理系统”。项目模拟企业内部报销流程，包含员工提交报销、部门负责人审批、财务审批、预算扣减、发票上传、OCR 识别、Excel 导出、用户管理和操作日志审计等功能。

## 功能概览

- 登录与角色权限：员工、部门负责人、财务、管理员四类角色。
- 报销申请：新增、编辑、删除、查询、提交报销单。
- 审批流程：部门审批、财务审批、审批意见记录、审批流程时间轴。
- 预算管理：部门预算设置、预算使用统计、报销通过后扣减预算。
- 首页仪表盘：报销总数、本月金额、待审批数量、状态统计、预算使用图表。
- 发票附件：上传发票图片，查看报销详情。
- OCR 识别：支持模拟 OCR，也预留百度 OCR API 配置。
- OCR 金额校验：比较发票金额与报销金额，提示一致、不一致或未识别。
- 用户管理：管理员新增、编辑、删除、启用、禁用用户。
- 个人资料：用户修改姓名、手机号、邮箱和密码。
- Excel 导出：财务和管理员导出全部报销申请数据。
- 操作日志：记录登录、报销、审批、预算、OCR、导出、用户管理等关键操作。

## 技术栈

- 前端：Vue 3、Vite、Element Plus、ECharts、Axios
- 后端：Spring Boot、Spring Data JPA、MySQL、Apache POI
- 部署：Ubuntu、Nginx、systemd、MySQL
- 第三方能力：百度 OCR API（可选）

## 项目结构

```text
finance-reimbursement-system
├── backend        Spring Boot 后端
├── frontend       Vue3 前端
├── database       数据库脚本
├── deploy         服务器部署配置模板
├── scripts        打包脚本
├── start-backend.ps1
└── start-frontend.ps1
```

## 本地运行环境

建议安装：

- JDK 21
- Node.js 20+
- MySQL 8
- IntelliJ IDEA
- VS Code
- DBeaver 或其他数据库工具

## 创建数据库

先在 MySQL 中创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS finance_reimbursement
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

也可以在项目根目录运行：

```powershell
.\database\create-database.ps1
```

## 启动后端

在项目根目录运行：

```powershell
.\start-backend.ps1
```

脚本会要求输入本机 MySQL root 密码，并通过环境变量传给 Spring Boot。后端默认启动在：

```text
http://localhost:8080
```

健康检查接口：

```text
http://localhost:8080/api/health
```

## 启动前端

在项目根目录运行：

```powershell
.\start-frontend.ps1
```

前端默认启动在：

```text
http://localhost:5173
```

## 测试账号

系统启动后会自动初始化演示账号：

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| employee | 123456 | 员工 |
| manager | 123456 | 部门负责人 |
| finance | 123456 | 财务 |
| admin | 123456 | 管理员 |

这些账号仅用于课程演示和本地开发。

## 百度 OCR 配置（可选）

不配置百度 OCR 时，系统会使用模拟 OCR，方便演示完整流程。

如果需要接入真实百度 OCR，可以设置环境变量：

```powershell
$env:BAIDU_OCR_API_KEY="你的 API Key"
$env:BAIDU_OCR_SECRET_KEY="你的 Secret Key"
```

服务器部署时请写入 `/etc/finance-reimbursement-system/backend.env`，不要把真实 Key 提交到 GitHub。

## 生产部署

`deploy` 目录中包含：

- `backend.env.example`：后端环境变量模板
- `nginx/finance-reimbursement-system.conf`：Nginx 配置模板
- `systemd/finance-backend.service`：后端 systemd 服务模板
- `README-部署说明.md`：部署说明

线上访问可以配置为：

```text
http://服务器公网IP/finance/
```

## GitHub 公开注意事项

本仓库不应提交以下内容：

- `node_modules`
- `backend/target`
- `frontend/dist`
- 上传的发票图片
- 本地 `.env` 文件
- 真实数据库密码
- 真实百度 OCR API Key / Secret Key
- 打包压缩包和备份文件

这些内容已通过根目录 `.gitignore` 排除。

## 项目说明

本项目重点展示了如何借助 AI 和 Vibe Coding 完成一个较完整的业务系统：先用自然语言拆解需求，再逐步生成后端接口、前端页面、数据库表、部署配置和项目文档，最后通过本地测试和服务器部署验证完整流程。

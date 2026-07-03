# 内部凭证财务系统

这是一个使用 Vibe Coding 和 AI 辅助完成的大学生实践项目。系统从基础财务报销作业逐步扩展为面向研究院内部凭证流转的综合管理系统，覆盖申请、审批、付款、财务复核、预算、资产、收入、台账和安全审计。

## 当前完成度

- 后端：Spring Boot，已通过 75 项自动化测试。
- 前端：Vue 3 + Element Plus，生产构建通过。
- 前端结构：已按 `layouts`、`views`、`components`、`api`、`composables` 和 `router` 拆分，使用 Vue Router 管理独立页面。
- 界面验收：8 类演示角色、18 条路由权限校验通过，并完成桌面、笔记本和手机宽度检查。
- 数据库：MySQL 8，提供完整初始化脚本并支持 JPA 自动兼容旧数据。
- 部署：提供 Nginx、systemd 和生产环境变量模板。
- 演示数据：报销、申购、劳务、借款均包含草稿、审批中、待后续处理和已完成示例。

## 核心功能

### 统一工作台

- 首页仪表盘：部门负责人查看本部门，财务、执行院长和管理员查看全院；六张统计卡片、双图表、最近业务单据和待办事项组成三层布局。
- 报销状态图：状态通过外部引导线展示名称、数量和占比，中心显示报销单总数，图例固定在底部。
- 首页待办：调用统一工作台接口，显示真实待办总数和最近 5 条，包含业务、编号、标题、申请人、金额、当前节点及等待时间。
- 我的申请：只显示当前用户创建的报销、申购、劳务和借款。
- 我的待办：按照角色、当前审批节点和部门范围返回待处理事项，可在当前页面完成审批、驳回、付款、财务复核和冲账。
- 已办事项：根据真实审批、付款和复核记录生成。
- 支持业务类型、状态和关键词筛选。

### 前端界面与交互

- 固定深色侧边栏、统一顶部栏和可折叠菜单，当前菜单使用蓝色选中状态。
- 左侧菜单按工作台、申请业务、资产管理、财务处理、预算统计和系统管理分组，空分组会根据角色权限自动隐藏。
- “我的待办”显示实时数量徽标；出纳付款统一在“我的待办”处理，不再显示重复的付款任务入口。
- 每个菜单对应独立路由，刷新后保持当前页面，浏览器前进和后退可正常使用。
- 无权限路由会跳转到当前角色的默认页面；登录过期后清理本地状态并返回登录页。
- 业务列表统一页面标题、筛选工具栏、数据表格、状态标签和操作区。
- 新增、编辑、详情和审批弹窗统一必填提示、滚动区域、固定底部操作栏和防重复提交。
- 仪表盘和业务页面适配常见桌面、笔记本及手机宽度，表格在小屏幕下可横向滚动。

### 业务模块

- 报销管理：`BX + 日期 + 三位序号`，提交时申请人电话、银行账号、收款人全称、报销事由和至少一份发票必填/必传，其他凭证按实际业务上传；支持关联申购、百度/模拟 OCR 和金额校验。
- 申购管理：`CG + 日期 + 三位序号`，支持多条采购明细；总额大于 5 万元时必须上传院务委员会审议材料。
- 资产验收：办公室验收入库并自动记录验收人；办公室选择实际使用人办理领用，生成 `ZC` 资产编号和 `LY` 领用编号。
- 劳务酬金：`LW + 日期 + 三位序号`，支持多人领款、金额大写、敏感信息脱敏和 Excel 导出。
- 暂借款/预付款：`YF + 日期 + 三位序号`，支持付款、冲账、部分冲账和逾期提醒。
- 收入登记与总台账：汇总收入、报销、劳务、申购和借款数据，支持筛选与 Excel 导出。
- 预算管理：财务复核通过后扣减预算，预算不足时阻止完成。
- 操作日志：记录登录、申请、审批、付款、OCR、附件、预算、导出和用户管理等关键操作。
- 完整单据 PDF：报销、申购、劳务和借款/预付款支持下载适合打印归档的完整单据，包含基本信息、明细、审批记录、付款复核信息及附件清单。

### 报销审批流程

```text
申请人提交
  -> 财务初审
  -> 部门负责人审批
  -> 执行院长审批
  -> 出纳付款并上传银行回执
  -> 财务复核
  -> 完成并扣减预算
```

报销单保存草稿时可暂不完整，提交审批时必须填写申请人电话、银行账号、收款人全称和报销事由，并至少上传一份发票；合同、会议记录、付款凭证及其他材料按实际业务上传。金额超过 5 万元的报销还需填写大额说明并上传会议审议材料；申购总额大于 5 万元时必须上传院务委员会审议材料。

资产验收入库自动记录办公室验收人。办理领用时由办公室选择实际使用人，系统分别保存验收人、领用人、当前保管人、验收时间和领用时间，并写入流转历史。

## 角色权限

| 账号 | 密码 | 角色 | 主要权限 |
| --- | --- | --- | --- |
| `employee` | `123456` | 员工 | 提交并查看个人申请 |
| `manager` | `123456` | 部门负责人 | 查看本部门数据并处理部门审批 |
| `finance` | `123456` | 财务 | 初审、复核、预算、收入、台账与导出 |
| `office` | `123456` | 办公室 | 申购管理和资产验收入库 |
| `executive` | `123456` | 执行院长 | 全院仪表盘与院长审批 |
| `committee` | `123456` | 院务委员会 | 查看全院业务单据和完整资产台账 |
| `cashier` | `123456` | 出纳 | 付款任务和银行回执 |
| `admin` | `123456` | 管理员 | 用户、全量业务数据和安全审计 |

以上账号只用于课程演示。系统首次启动会自动创建账号，并将旧明文密码迁移为 BCrypt。

## 安全机制

- BCrypt 密码加密及旧密码自动迁移。
- 登录 token 默认 120 分钟有效。
- 退出登录或修改密码后旧 token 立即失效。
- 附件不允许通过公开 `/uploads/**` 直接访问。
- 附件下载需要登录，并复用对应业务单据的数据权限。
- 上传文件限制 10MB，同时校验文件名、扩展名和 MIME。
- 禁止可执行文件、危险路径和伪造类型文件。
- 真实数据库密码和百度 OCR Key 不应提交到 GitHub。

## 技术栈

- 前端：Vue 3、Vite、Element Plus、ECharts、Axios
- 后端：Java 21、Spring Boot、Spring Data JPA、Apache POI
- 数据库：MySQL 8
- 测试：JUnit 6、Mockito、H2
- 部署：Ubuntu、Nginx、systemd
- 第三方能力：百度 OCR API（可选）

## 项目结构

```text
finance-reimbursement-system
├── backend                 Spring Boot 后端和测试
├── frontend                Vue 3 前端
├── database                数据库初始化脚本
├── deploy                  Nginx、systemd、环境变量和部署说明
├── scripts                 项目文档生成脚本
├── start-backend.ps1       Windows 后端启动脚本
└── start-frontend.ps1      Windows 前端启动脚本
```

## 在新电脑上运行

### 1. 安装环境

- Git
- JDK 21
- Node.js 20 或更高版本
- MySQL 8

### 2. 克隆项目

```powershell
git clone https://github.com/cwllaaz/finance-reimbursement-system.git
cd finance-reimbursement-system
```

### 3. 创建数据库

登录 MySQL 后执行：

```sql
CREATE DATABASE IF NOT EXISTS finance_reimbursement
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

也可以在 Windows PowerShell 运行：

```powershell
.\database\create-database.ps1
```

### 4. 启动后端

推荐在项目根目录运行：

```powershell
.\start-backend.ps1
```

脚本会要求输入本机 MySQL root 密码。启动成功后访问：

```text
http://localhost:8080/api/health
```

也可以手动运行：

```powershell
cd backend
$env:MYSQL_ROOT_PASSWORD="你的 MySQL 密码"
.\mvnw.cmd spring-boot:run
```

### 5. 启动前端

另开一个 PowerShell：

```powershell
cd frontend
npm ci
npm run dev
```

浏览器访问：

```text
http://localhost:5173
```

## 从零验证项目

后端测试使用 H2 内存数据库，不要求本机 MySQL 正在运行：

```powershell
cd backend
.\mvnw.cmd clean test package
```

前端验证：

```powershell
cd frontend
npm ci
npm run test:roles
npm run build
```

当前验证结果：

```text
Backend: 60 tests passed, BUILD SUCCESS
Frontend role access: 7 roles and 18 routes passed
Frontend: production build completed
Frontend dependencies: npm audit found 0 vulnerabilities
```

前端构建可能显示第三方 `PURE` 注释和大 chunk 警告，不影响生成 `dist`。

## 百度 OCR 配置

不配置百度 OCR 时，系统使用模拟识别，仍能演示完整流程。使用真实 OCR 时，在启动后端前设置：

```powershell
$env:BAIDU_OCR_API_KEY="你的 API Key"
$env:BAIDU_OCR_SECRET_KEY="你的 Secret Key"
```

不要将真实 Key 写入仓库。

## 生产部署

`deploy` 目录包含：

- `backend.env.example`
- `nginx/finance-reimbursement-system.conf`
- `systemd/finance-backend.service`
- `README-部署说明.md`

推荐访问路径：

```text
http://服务器公网IP/finance/
```

详细步骤请阅读 [部署说明](deploy/README-部署说明.md)。

## Vibe Coding 实践说明

本项目采用小步迭代方式完成：先将老师意见拆成单一、可验证的提示词，再由 AI 协助修改数据库、后端、前端、权限、测试和文档。每次功能完成后运行测试和生产构建，并通过角色账号验证真实业务流程。

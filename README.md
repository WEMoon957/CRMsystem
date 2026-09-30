# SaaS 客户管理系统（CRM）

面向多账号的客户管理系统：**主账号拥有全部客户信息与账号管理权限，子账号拥有客户添加与管理功能，但仅能查看/操作自己名下的客户数据**。客户资料与意向等级（五档）统一录入管理，支持跟进记录时间线。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 21 + Spring Boot 3.4 + Spring Security + MyBatis-Plus + Flyway |
| 数据库 | MySQL 8.0 |
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router |
| 认证 | JWT（短时效访问令牌 + 服务端刷新令牌轮换） |

## 功能清单

### 账号体系
- 主账号（`ADMIN`）：全量客户可见、新增/禁用/删除子账号、将客户分配给任意子账号
- 子账号（`MEMBER`）：新增、编辑、删除、跟进客户，**但仅限自己名下的客户**（数据隔离）
- 登录 / 登出 / 令牌静默刷新

### 客户管理
- 客户列表：关键词搜索（姓名/电话/公司）、意向筛选、分页
- 新增 / 编辑客户：姓名、电话、公司、职位、来源、行业、意向等级、地址、备注、下次跟进时间
- 客户分配（主账号专属）
- 删除（逻辑删除）

### 意向等级（五档）
| 枚举 | 中文 | 语义 |
| --- | --- | --- |
| `PENDING` | 待跟进 | 新建客户默认 |
| `INTERESTED` | 有意向 | 有合作意愿 |
| `NOT_INTERESTED` | 无意向 | 暂无意愿 |
| `COOPERATED` | 已合作 | 已成交 |
| `REFUSED` | 不合作 | 明确拒绝 |

### 跟进记录
- 客户详情页时间线展示跟进记录
- 添加跟进：内容 + 本次结论（可同步更新意向）+ 下次跟进时间

### 产品资料
- 每个客户可挂多条产品记录：产品名称、规格型号、数量、单价、备注
- 增删改查，权限与客户数据隔离一致

### 图片 / 附件
- 客户详情页图片墙 + 上传 / 预览 / 删除
- 支持图片（JPG/PNG/GIF/WebP 等）与文档（PDF/Office/TXT/CSV）
- 文件存后端磁盘（`backend/uploads/`），经 `/uploads/**` 静态映射访问（生产可替换对象存储）

### 数据看板
- 五档意向客户数量卡片
- 意向分布进度条

## 本地开发（Windows 便携环境）

### 0. 环境要求
已随项目内置便携版工具（`tools/` 目录），无需额外安装：
- JDK 21（`tools/jdk21/`）
- Maven 3.9（`tools/maven/`，已配置阿里云镜像）
- MySQL 8.0（`tools/mysql/`，端口 33061）
- Node 22（系统 managed runtime）

> 注：MySQL 依赖 VC++ 2015-2022 x64 运行库（`vcruntime140_1.dll`），若缺失会导致 mysqld 静默退出。已通过微软官方 `vc_redist.x64.exe` 安装。

### 1. 一键启动（Windows）
```powershell
# 依次启动 MySQL、后端、前端
powershell -ExecutionPolicy Bypass -File .\start-all.ps1
```

或分别启动：
```powershell
.\start-mysql.ps1      # 1. 启动数据库（首次会自动初始化）
.\start-backend.ps1    # 2. 启动后端 http://localhost:8080
.\start-frontend.ps1   # 3. 启动前端 http://localhost:5173
```

### 2. 访问
浏览器打开 **http://localhost:5173**

### 3. 初始账号
| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `Admin@123` | 主账号（全量权限） |
| `sales01` | `Sales@123` | 子账号（仅自己名下客户） |

## 生产部署（Docker Compose，推荐）

```bash
# 1. 配置环境变量（必填：MYSQL_PASSWORD / JWT_SECRET）
cp .env.example .env && vi .env

# 2. 首次启动创建主账号：.env 中设 SEED_ENABLED=true
docker compose up -d --build

# 3. 主账号创建完成后，将 SEED_ENABLED 改回 false 并重启
docker compose up -d
```

访问 **http://localhost**（端口由 `WEB_PORT` 控制）。后端健康检查：`/actuator/health`。

## Windows 桌面客户端（分发给同事使用）

**架构**：Electron 壳 → 拉起本机内置后端（安装包内置裁剪版 JRE，用户无需装 Java）→ 连接**公司共享 MySQL**。多人数据一致性由共享数据库保证；客户附件默认存本机，多人互看附件需在首次配置时填写共享目录（UNC 路径）。

### 构建安装包（在开发机上执行一次）

```powershell
# 前端构建 → 后端打包 → jlink 裁剪 JRE → NSIS 安装包
powershell -ExecutionPolicy Bypass -File .\desktop\build-installer.ps1
```

产出 `desktop\dist\SaaS CRM Setup 1.0.0.exe`，直接分发给同事安装（免管理员权限，可选安装目录，自动创建桌面快捷方式）。

### 同事侧使用

1. 双击安装包，按向导安装
2. 首次启动进入**配置向导**：填写公司 MySQL 地址/库名/账号密码，（可选）共享附件目录；数据库全新时可勾选"创建初始主账号"
3. 配置保存后自动启动并进入系统；之后每次双击即用

> 配置保存在本机 `%APPDATA%\saas-crm-desktop\config.json`；后端日志在 `%APPDATA%\saas-crm-desktop\logs\backend.log`，启动失败页可一键打开。

## 环境变量

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | 运行环境（`dev` / `prod`） | `dev` |
| `MYSQL_URL` | 数据库连接串（prod 必填） | 本地 33061 开发库 |
| `MYSQL_USER` / `MYSQL_PASSWORD` | 数据库账号（prod 必填） | 开发占位值 |
| `JWT_SECRET` | JWT 签名密钥（prod 必填，≥32 字节，`openssl rand -hex 32`） | dev 占位值 |
| `FRONTEND_ORIGIN` | 前端来源（CORS） | `http://localhost:5173` |
| `SEED_ENABLED` | 空库时创建主账号（一次性初始化用） | `false`（dev 默认 `true`） |
| `SEED_ADMIN_USERNAME` / `SEED_ADMIN_PASSWORD` | 初始主账号 | `admin` / `Admin@123` |
| `UPLOAD_DIR` | 上传文件目录 | `./uploads` |
| `SERVER_PORT` | 后端端口 | `8080` |
| `DB_POOL_MAX` / `DB_POOL_MIN` | 数据库连接池大小 | `10` / `2` |
| `LOG_FILE` | 日志文件路径（仅 dev 输出滚动文件日志；prod 只走 stdout） | `./logs/crm.log` |

> prod profile 下敏感配置**无默认值**，缺失即启动失败（fail-fast）；演示数据只在 dev 播种。

## 生产级能力清单

- **安全**：登录滑动窗口限流（IP+账号，10 分钟 5 次）、安全响应头、BCrypt 密码、刷新令牌服务端轮换+定时清理、数据隔离守卫
- **可观测**：`/actuator/health` 健康检查、全链路 traceId（MDC + `X-Request-Id` 响应头）、结构化滚动日志
- **可靠**：Flyway 迁移、全局异常兜底（不泄露堆栈）、优雅停机（20s）、连接池上限与生命周期管理
- **测试**：`mvn test`（18 个单元测试：JWT 签发/篡改/过期、登录与令牌轮换、数据隔离守卫）

## 目录结构

```
├── backend/                 # Spring Boot 后端（含 Dockerfile）
│   └── src/main/java/com/saas/crm/
│       ├── auth/            # 认证（登录限流/令牌轮换/定时清理）
│       ├── customer/        # 客户管理（CRUD + 数据隔离 + 统计）
│       ├── followup/        # 跟进记录
│       ├── user/            # 子账号管理（仅主账号）
│       └── common/          # 统一响应/异常/安全/请求追踪/种子数据
├── frontend/                # Vue3 + Element Plus 前端（含 Dockerfile + nginx.conf）
│   └── src/
│       ├── api/             # axios 封装（401 自动刷新）+ 业务接口
│       ├── stores/          # Pinia 认证状态
│       ├── router/          # 路由 + 权限守卫
│       └── views/           # 登录/看板/客户列表/客户详情/账号管理
├── desktop/                 # Windows 桌面客户端（Electron 壳 + 内嵌后端/JRE + NSIS 安装包）
│   ├── main.js              # 主进程：拉起本地后端、健康检查、窗口生命周期
│   ├── setup.html           # 首次配置向导（公司 MySQL 连接）
│   └── build-installer.ps1  # 一键打包脚本
├── docker-compose.yml       # 生产一键部署（MySQL + 后端 + 前端）
├── .env.example             # 部署环境变量模板
├── tools/                   # 便携 JDK / Maven / MySQL
└── start-*.ps1              # 本地启动脚本
```

## 主要 API

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | 公开 | 登录 |
| POST | `/api/auth/refresh` | 公开 | 刷新令牌 |
| POST | `/api/auth/logout` | 登录 | 登出 |
| GET | `/api/auth/me` | 登录 | 当前用户 |
| GET | `/api/customers` | 登录 | 客户分页（子账号自动隔离） |
| POST | `/api/customers` | 登录 | 新增客户 |
| GET | `/api/customers/{id}` | 登录 | 客户详情 |
| PUT | `/api/customers/{id}` | 登录 | 编辑客户 |
| DELETE | `/api/customers/{id}` | 登录 | 删除客户 |
| PUT | `/api/customers/{id}/assign` | 主账号 | 分配客户 |
| GET | `/api/customers/stats` | 登录 | 意向统计 |
| GET | `/api/customers/{id}/followups` | 登录 | 跟进记录列表 |
| POST | `/api/customers/{id}/followups` | 登录 | 新增跟进 |
| GET/POST | `/api/customers/{id}/products` | 登录 | 产品资料列表 / 新增 |
| PUT/DELETE | `/api/customers/{id}/products/{pid}` | 登录 | 产品编辑 / 删除 |
| GET/POST | `/api/customers/{id}/attachments` | 登录 | 附件列表 / 上传（multipart） |
| DELETE | `/api/customers/{id}/attachments/{aid}` | 登录 | 附件删除 |
| GET/POST/PUT/DELETE | `/api/users` | 主账号 | 子账号管理 |

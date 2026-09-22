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

## 快速启动

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

## 环境变量（生产部署需覆盖）

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `MYSQL_URL` | 数据库连接串 | `jdbc:mysql://localhost:33061/crm_db...` |
| `MYSQL_USER` | 数据库用户 | `root` |
| `MYSQL_PASSWORD` | 数据库密码 | `crm_dev_2026` |
| `JWT_SECRET` | JWT 签名密钥（**生产必改**，≥32 字节） | dev 占位值 |
| `FRONTEND_ORIGIN` | 前端来源（CORS） | `http://localhost:5173` |

## 目录结构

```
├── backend/                 # Spring Boot 后端
│   └── src/main/java/com/saas/crm/
│       ├── auth/            # 认证（登录/刷新/登出/用户实体）
│       ├── customer/        # 客户管理（CRUD + 数据隔离 + 统计）
│       ├── followup/        # 跟进记录
│       ├── user/            # 子账号管理（仅主账号）
│       └── common/          # 统一响应/异常/安全/分页/种子数据
├── frontend/                # Vue3 + Element Plus 前端
│   └── src/
│       ├── api/             # axios 封装（401 自动刷新）+ 业务接口
│       ├── stores/          # Pinia 认证状态
│       ├── router/          # 路由 + 权限守卫
│       └── views/           # 登录/看板/客户列表/客户详情/账号管理
├── tools/                   # 便携 JDK / Maven / MySQL
└── start-*.ps1              # 启动脚本
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

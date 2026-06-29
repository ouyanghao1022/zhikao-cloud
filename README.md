# 智考云 - 在线考试与学习平台

> 全栈在线考试 + 学习社区系统,覆盖考试出题/答题/判分/报告/证书/小组/PK 等完整闭环。

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4-4FC08D?logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker&logoColor=white)](docker-compose.yml)

---

## 项目简介

智考云是一套面向**学生 / 教师 / 管理员**三角色的在线考试与学习平台,既支持严肃考试场景(试卷、分发、监考、判分、证书),也兼顾日常学习场景(题库练习、错题本、收藏夹、学习小组、组队 PK、讨论区)。

后端 `Spring Boot 3.2.5 + Spring Security 6 + JWT + MyBatis-Plus`,前端 `Vue 3 + TypeScript + Vite + Element Plus`,数据库 `MySQL 8.0+`,部署支持 `Docker Compose` 一键编排,可搭配 `Cloudflare Tunnel` 零成本暴露公网。

### 核心特性

- **完整的考试闭环** — 试卷创建 → 题目分发 → 答题埋点 → 自动判分 → 错题收录 → 成绩报告 → 证书颁发
- **三类角色权限分明** — 学生(学/考/练)、教师(出卷/管题/查报告)、管理员(用户/系统)
- **学情可视化** — 个人雷达图、知识点热力图、成绩趋势线、薄弱分析、班级对比
- **互动学习社区** — 讨论区版块/帖子/评论/点赞、学习小组任务、资源、即时聊天
- **组队 PK** — 自助组队、好友匹配、对战答题、排行榜
- **错题本 + 收藏夹** — 自动/手动收录错题、分类收藏、专项练习
- **证书闭环** — 通过考试自动发证 + 教师/管理员在班级报告内手动补发证书
- **运维就绪** — Docker 多阶段构建、健康检查、SMS 短信登录、JWT 无状态鉴权、操作日志

---

## 快速启动

### 环境要求

| 工具 | 版本 | 说明 |
|---|---|---|
| JDK | **21** | `D:\AppData\Java\JDK\JDK21`(本机配置) |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | 前端开发服务器 |
| MySQL | 8.0+ | 数据存储(默认 root/123456) |

### 1. 初始化数据库

```bash
mysql -u root -p123456 -e "CREATE DATABASE IF NOT EXISTS zhikao_cloud DEFAULT CHARSET utf8mb4"
mysql -u root -p123456 zhikao_cloud < sql/schema.sql
mysql -u root -p123456 zhikao_cloud < sql/test_data.sql   # 可选:测试数据
```

> 增量升级脚本按顺序执行 `sql/*.sql`(命名带 `add_*` / `fix_*` 前缀)。

### 2. 启动后端

**方式 A — IDE**
1. IDEA → `File → Open` → 选 `backend/pom.xml` → 作为 Maven 项目打开
2. `File → Project Structure → SDK` 选 JDK 21
3. 运行 `ZhikaoCloudApplication` 启动类,绿色三角启动
4. 控制台看到 `Started ZhikaoCloudApplication` 即成功,默认端口 `8080`

**方式 B — 命令行**
```bash
cd backend
mvn spring-boot:run
```

> 8080 端口被占用:`Run Configuration → Environment variables` 加 `server.port=9090`,同时改 `frontend/vite.config.ts` 的 `server.proxy[ '/api' ].target`。

### 3. 启动前端

```bash
cd frontend
npm install        # 首次运行
npm run dev        # 开发模式,HMR 热更新
```

浏览器打开 **<http://localhost:3000>**,所有 `/api/**` 请求会通过 Vite 代理转发到 `localhost:8080`。

### 4. 内置账号

| 角色 | 用户名 | 密码 | 权限范围 |
|---|---|---|---|
| 管理员 | `admin` | `123123` | 全部功能 + 用户/日志管理 |
| 教师 | `teacher1` | `123123` | 考试/题库/班级报告/手动发证 |
| 学生 | `student1` | `123123` | 考试/练习/错题/PK/小组/讨论 |

> 第一个学生账号请通过「注册」自助创建,或由管理员在用户管理页添加。

---

## Docker 一键部署

环境变量 → 构建 → 启动 → 暴露公网,完整一套走完大约 15 分钟。

### 1. 准备 `.env`

```bash
cp .env.example .env
openssl rand -hex 32 >  jwt_secret.txt    # JWT_SECRET
openssl rand -hex 16 >  mysql_pwd.txt     # MYSQL_PASSWORD
# 把生成的值填进 .env
```

`.env` 关键变量:
```
MYSQL_ROOT_PASSWORD=<your-root-pw>
MYSQL_PASSWORD=<your-app-pw>       # 应用账号,不要直接用 root
JWT_SECRET=<64-char-random-hex>    # JWT 签名密钥,生产必改
SPRING_PROFILES_ACTIVE=prod
```

### 2. 构建并启动

```bash
docker compose up -d --build
docker compose ps          # 查看三个服务状态(mysql/backend/frontend)
docker compose logs -f     # 实时日志
```

| 服务 | 端口 | 镜像 | 健康检查 |
|---|---|---|---|
| `mysql` | 3306 | `mysql:8.0` | `mysqladmin ping` |
| `backend` | 8080 | 多阶段 Maven → `eclipse-temurin:21-jre` | `/api/v1/auth/me` 返回非 5xx |
| `frontend` | 80 | Node 20 → `nginx:1.27-alpine` | `wget http://localhost/` |

> 数据库 SQL 不会自动执行,首次启动前需要手动导入 `sql/schema.sql`。详见 [deploy/SQL.md](deploy/SQL.md)。

### 3. 暴露到公网(Cloudflare Tunnel)

零成本方案:无需备案、无需端口转发。完整步骤见 [deploy/CLOUDFLARE-TUNNEL.md](deploy/CLOUDFLARE-TUNNEL.md)。

```bash
# 装 cloudflared
curl -fsSL https://pkg.cloudflare.com/cloudflare-main.gpg | sudo tee /usr/share/keyrings/cloudflare-main.gpg >/dev/null
echo "deb [signed-by=/usr/share/keyrings/cloudflare-main.gpg] https://pkg.cloudflare.com/cloudflared $(lsb_release -cs) main" \
  | sudo tee /etc/apt/sources.list.d/cloudflared.list
sudo apt update && sudo apt install -y cloudflared

# 登录、创建隧道、配置路由
cloudflared tunnel login
cloudflared tunnel create zhikao
cp deploy/cloudflared-config.yml.example ~/.cloudflared/config.yml
# 改 config.yml 填 tunnel id / credentials file / 域名

# 开机自启
sudo cloudflared service install
```

部署完成后会得到形如 `https://zhikao.example.com` 的公网 HTTPS 域名。

---

## 目录结构

```
zhikao-cloud/
├── backend/                    # Spring Boot 3.2.5 后端
│   ├── Dockerfile              # Maven 多阶段构建 → temurin:21-jre
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/zhikao/
│       │   ├── common/         # Result、PageRequest、PageResult、异常
│       │   ├── config/         # Security、WebMvc、MybatisPlus、Knife4j
│       │   ├── controller/     # 23 个 REST 控制器(详见下表)
│       │   ├── dto/            # 入参校验
│       │   ├── entity/         # 27 个数据库实体
│       │   ├── filter/         # JwtAuthenticationFilter
│       │   ├── mapper/         # MyBatis-Plus Mapper + 自定义 @Select
│       │   ├── service/        # 业务接口 + impl 实现
│       │   ├── utils/          # JwtUtils、密码加密、工具
│       │   └── vo/             # 响应体(LoginVO 等)
│       └── resources/
│           ├── application.yml          # 本地开发默认配置
│           └── application-prod.yml     # 生产环境配置(env 注入)
│
├── frontend/                   # Vue 3 + TS + Vite
│   ├── Dockerfile              # Node 构建 → nginx 静态服务
│   ├── nginx.conf              # SPA 路由兜底 + /api 反代
│   ├── vite.config.ts          # 开发代理 /api → :8080
│   └── src/
│       ├── api/                # 10 个 API 模块(纯 TS 封装 axios)
│       ├── components/layout/  # MainLayout、Sidebar、Topbar
│       ├── router/             # 路由 + 角色守卫(meta.roles)
│       ├── stores/             # Pinia userStore
│       ├── utils/              # axios 拦截器(Token 自动注入)
│       └── views/
│           ├── admin/          # 用户/考试/题库/数据看板/讨论管理/PK 管理
│           ├── auth/           # Login、Register
│           ├── dashboard/      # 首页仪表盘
│           ├── discuss/        # 版块/帖子/详情
│           ├── exam/           # 考试列表/答题/结果
│           ├── favorite/       # 收藏夹
│           ├── group/          # 学习小组
│           ├── pk/             # PK 大厅/排行榜
│           ├── question/       # 题库练习
│           ├── report/         # 学情报告
│           ├── user/           # 个人中心
│           └── wrongbook/      # 错题本
│
├── sql/
│   ├── schema.sql              # 完整 DDL(916 行)
│   ├── test_data.sql           # 测试账号 + 种子数据
│   ├── test_accounts.sql       # 仅测试账号
│   ├── add_discussion_audit_fields.sql
│   ├── add_exam_teacher_scope.sql
│   ├── add_practice_support.sql
│   ├── fix-expired-exams.sql
│   └── fix-orphan-sessions.sql
│
├── deploy/                     # 部署/运维
│   ├── README.md               # 部署总览
│   ├── CLOUDFLARE-TUNNEL.md    # 公网暴露详细步骤
│   ├── SQL.md                  # 数据库初始化说明
│   ├── init-server.sh          # 服务器首次初始化
│   └── cloudflared-config.yml.example
│
├── docs/                       # 项目文档
│   ├── README.md               # 文档目录索引
│   ├── changelog/              # bug 报告与修复记录(BR-XXX)
│   └── archive/                # 已落地的设计稿
│
├── docker-compose.yml          # 一键编排 mysql+backend+frontend
├── .env.example                # 环境变量模板
├── .gitignore
└── README.md                   # 你正在读的这个
```

---

## 后端控制器清单

| 控制器 | 路径前缀 | 说明 |
|---|---|---|
| `AuthController` | `/auth` | 注册/登录/刷新 Token/当前用户 |
| `UserController` | `/user` | 用户管理(管理员) |
| `ExamPaperController` | `/exam` | 试卷 CRUD / 列表 / 详情 |
| `ExamGradeController` | `/exam` | 判分 / 提交答案 |
| `ExamMonitorController` | `/exam` | 监考日志 |
| `QuestionController` | `/question` | 题库 CRUD |
| `QuestionCategoryController` | `/question` | 题库分类 |
| `ReportController` | `/report` | 个人/班级报告、趋势、热力 |
| `ClassController` | `/class` | 班级、成员、报告 |
| `WrongNotebookController` | `/wrongbook` | 错题本 |
| `FavoriteController` | `/favorite` | 收藏夹/项 |
| `PointsController` | `/points` | 积分/签到/排行 |
| `CertificateController` | `/cert` | 证书颁发/查询 |
| `GroupController` | `/group` | 学习小组 |
| `DiscussionController` | `/discuss` | 帖子/评论/版块 |
| `PkController` | `/pk` | PK 队伍/对战 |
| `LeaderboardController` | `/leaderboard` | 排行榜 |
| `FileUploadController` | `/file` | 头像/资源上传 |
| `ImportController` | `/import` | Excel 导入题库/用户 |
| `AiGradingController` | `/ai` | 主观题 AI 辅助判分 |
| `SmsController` | `/sms` | 短信验证码 |
| `SysMessageController` | `/message` | 站内消息 |
| `SysLogController` | `/log` | 操作日志 |
| `NotificationSettingController` | `/notification` | 通知偏好 |

---

## 技术栈

### 后端

| 层 | 技术 | 版本 | 用途 |
|---|---|---|---|
| 框架 | Spring Boot | 3.2.5 | 核心 |
| 安全 | Spring Security + JWT | 6.2 / 0.11.5 | 鉴权 + `@PreAuthorize` |
| ORM | MyBatis-Plus | 3.5.7 | 数据库访问 |
| 数据库 | MySQL + HikariCP | 8.0 | 主存储 |
| 文档 | Knife4j | 4.x | Swagger UI(`/doc.html`) |
| 工具 | Lombok + Hutool | - | 减少样板 |
| 校验 | Jakarta Validation | - | DTO 入参 |

### 前端

| 层 | 技术 | 版本 | 用途 |
|---|---|---|---|
| 框架 | Vue | 3.4 | Composition API |
| 语言 | TypeScript | 5.x | 类型安全 |
| 构建 | Vite | 5.x | HMR 构建 |
| 路由 | Vue Router | 4 | 路由 + meta.roles 守卫 |
| 状态 | Pinia | 2 | userStore |
| UI | Element Plus | 2.5 | 组件 |
| 图表 | ECharts | 5.5 | 报告可视化 |
| HTTP | Axios | - | 拦截器自动注入 JWT |

---

## 角色权限矩阵

| 模块 | 学生 | 教师 | 管理员 |
|---|:-:|:-:|:-:|
| 参加考试 / 答题 | ✅ | — | — |
| 题库练习 / 错题本 / 收藏夹 | ✅ | — | — |
| 学情报告(个人) | ✅ | ✅ | — |
| 组队 PK / 学习小组 / 讨论区 | ✅ | ✅ | ✅ |
| 班级报告 / 手动发证 | — | ✅ | ✅ |
| 考试管理 / 题库管理 / 班级管理 | — | ✅ | ✅ |
| 用户管理 / 操作日志 | — | — | ✅ |

---

## 接口规范

- **前缀** `/api/v1`
- **鉴权** 请求头 `Authorization: Bearer <jwt>`
- **响应** `application/json; charset=utf-8`

```json
{
  "code": 200,
  "message": "ok",
  "data": { ... }
}
```

完整接口列表(分模块):参见下方章节,或启动后端访问 **<http://localhost:8080/api/v1/doc.html>**(Knife4j)交互式调试。

### 鉴权
```http
POST /api/v1/auth/register    # 注册
POST /api/v1/auth/login       # 登录 → { token, userInfo }
POST /api/v1/auth/refresh     # 用 refresh token 换新 access
GET  /api/v1/auth/me          # 当前登录用户
POST /api/v1/auth/logout      # 退出
```

### 考试
```http
GET    /api/v1/exam/list
GET    /api/v1/exam/detail/{id}
GET    /api/v1/exam/take/{id}          # 题目脱敏
POST   /api/v1/exam/submit             # 提交 → 自动判分 + 错题收录
GET    /api/v1/exam/my-records
POST   /api/v1/exam/create             # 教师/管理员
PUT    /api/v1/exam/publish/{id}
GET    /api/v1/exam/class-report/{paperId}  # 班级报告 + 学生列表 + 证书状态
POST   /api/v1/cert/issue/{sessionId} # 教师/管理员手动补发证书
```

### 学情报告
```http
GET /api/v1/report/personal
GET /api/v1/report/exam-trend
GET /api/v1/report/knowledge-heatmap
GET /api/v1/report/weak-areas
```

### 题库 / 收藏 / 错题 / 积分
```http
GET    /api/v1/question/list
POST   /api/v1/question/create
GET    /api/v1/favorite/folders
POST   /api/v1/favorite/item
GET    /api/v1/wrongbook/list
POST   /api/v1/points/checkin
```

### 讨论 / 小组 / PK
```http
GET    /api/v1/discuss/posts
POST   /api/v1/discuss/post
POST   /api/v1/discuss/comment
GET    /api/v1/group/list
POST   /api/v1/group/{id}/task
POST   /api/v1/pk/team
POST   /api/v1/pk/match/start
GET    /api/v1/leaderboard
```

---

## 关键设计

### JWT 鉴权

- 前端登录成功后,`token` 存 `localStorage`
- Axios 拦截器自动加 `Authorization: Bearer <token>`
- `JwtAuthenticationFilter` 解析 token → 注入 `SecurityContext`
- `userId` 从自定义 claim `userId` 取(**不是** `claims.getId()`)
- 角色挂 `ROLE_` 前缀(Spring Security 约定:`@PreAuthorize("hasRole('SUPER_ADMIN')")`)

### 角色权限控制

- 后端用 `@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TEACHER')")` 在 Controller 方法级别强制
- 前端用 `router.beforeEach` + `route.meta.roles` 双重兜底
- 角色定义在 `sys_role` 表,用户通过 `sys_user_role` 关联

### 文件上传

- 默认上传到 `uploads/(avatars|certs|resources)/`
- 生产建议挂载到对象存储或独立数据卷
- 静态访问路径:`http://host:8080/uploads/**`

### 证书生成

- 通过考试 → 自动生成 `exam_certificate` 记录 + 模板填充(当前为 URL 占位)
- 教师/管理员可在班级报告 → 学生列表 → 「颁发证书」手动补发
- 前端展示:`/cert/preview/{certNo}` → PNG 渲染(基于模板)

---

## 已知约束与注意事项

- **JDK 必须 21**(已切 `temurin:21`),Maven `compiler-plugin` 锁 `21`
- **数据库** `sys_user` 必须有 `deleted` 列;角色查询不要带 `AND deleted = 0`(本系统逻辑删除字段在 service 层处理)
- **Spring Security 6.2** 配置里必须加 `.securityMatcher("/**")`,否则会拦截静态资源
- **Vite 代理** 需要保留 `headers: { 'Authorization': req.headers.authorization }`,否则 JWT 头丢失
- **MySQL 字符集** 严格 `utf8mb4`,导 SQL 前确认一致
- **短信登录** 默认未开启(无 SMS 网关),生产需对接阿里云/腾讯云;`.env` 加 `SMS_ACCESS_KEY/SECRET` 后启用
- **错题自动收录** 只对 `ex.status >= 2`(已提交)的作答有效
- **Certificate 颁发** 限定 `hasAnyRole('SUPER_ADMIN','TEACHER')`,学生身份调用会 403

---

## 开发工作流

```bash
# 改前端
cd frontend && npm run dev             # HMR

# 改后端
cd backend && mvn spring-boot:run      # 热重启需加 devtools

# 加新表/字段
1. 改 entity
2. 写 mapper.xml 或 @Select
3. service 实现
4. controller 暴露
5. 前端 api/*.ts 封装
6. 路由添加(roles 限制)
7. 增加 sql/add_*.sql 增量脚本(保持幂等)
```

### 代码风格

- Java:Standard + Lombok,Controller 注入用构造器注入(IOC 友好)
- TS:Vue3 `<script setup lang="ts">` + 类型推导,避免 `any`
- 提交:`<type>: <subject>`(`feat / fix / style / refactor / docs / chore`)
- 改动控制在单一语义,不大杂烩

---

## 文档

- [docs/README.md](docs/README.md) — 文档索引
- [docs/changelog/](docs/changelog/) — bug 报告(BR-XXX)
- [docs/archive/](docs/archive/) — 已落地的设计稿
- [deploy/README.md](deploy/README.md) — 部署总览
- [deploy/CLOUDFLARE-TUNNEL.md](deploy/CLOUDFLARE-TUNNEL.md) — 公网暴露
- [deploy/SQL.md](deploy/SQL.md) — 数据库迁移脚本说明

---

## Roadmap

- [ ] AI 主观题判分(`AiGradingController` 已留口)
- [ ] 老师互评 / 双评机制
- [ ] 试卷随机抽题组卷策略优化(难度系数 + 知识点分布)
- [ ] 移动端 H5 / 小程序适配
- [ ] 短信登录接入阿里云
- [ ] 七牛 / OSS 文件存储抽象

---

## 贡献

欢迎 PR / Issue!大改前请先开 Issue 讨论方向。

- Fork → 分支 → 提 PR
- 提交前本地 `mvn -q -DskipTests=false test`(如已有测试)
- 前端 `npm run build` 必须通过

---

## License

[MIT](LICENSE)

---

## 致谢

- [Element Plus](https://element-plus.org/)
- [MyBatis-Plus](https://baomidou.com/)
- [Knife4j](https://doc.xiaominfo.com/)
- [ECharts](https://echarts.apache.org/)
- [Cloudflare Tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/)

— 由 **[ouyanghao1022](https://github.com/ouyanghao1022)** 维护

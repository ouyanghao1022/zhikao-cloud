# 智考云 - 在线考试与学习平台

> SpringBoot 3.2.5 + Vue 3 + MyBatis-Plus + MySQL — 全功能在线考试学习系统

---

## 快速启动

### 环境要求
- **JDK 21**（路径：`D:\AppData\Java\JDK\JDK21`）
- **MySQL 8.0+**（root / 123456）
- **Node.js 18+**
- **Maven 3.8+**

### 1. 初始化数据库

```sql
CREATE DATABASE IF NOT EXISTS zhikao_cloud DEFAULT CHARSET utf8mb4;
-- 然后导入 sql/schema.sql
```

或用命令行：
```bash
mysql -u root -p123456 -e "CREATE DATABASE IF NOT EXISTS zhikao_cloud DEFAULT CHARSET utf8mb4"
mysql -u root -p123456 zhikao_cloud < sql/schema.sql
```

### 2. 启动后端（IDEA）

1. File → Open → 选择 `backend/pom.xml` → Open as Project
2. File → Project Structure → SDK 选 JDK 21
3. 打开 `ZhikaoCloudApplication.java`，点击绿色三角启动
4. 看到 `Started ZhikaoCloudApplication` 即启动成功

> **若端口 8080 被占用**：Run Configuration → Environment variables → 添加 `server.port=9090`，同时修改 `frontend/vite.config.ts` 中 proxy target 为 `http://localhost:9090`

### 3. 启动前端

```bash
cd frontend
npm install   # 首次运行
npm run dev
```

浏览器访问 **http://localhost:3000**

### 4. 登录账号

| 角色 | 用户名      | 密码     | 权限 |
|------|----------|--------|------|
| 管理员 | admin    | 123123 | 全部功能 + 用户管理 |
| 教师 | teacher1 | 123123 | 考试/题库管理 |
| 学生 | student1 | 123123 | 考试/练习/错题本/PK |

> 第一次使用建议点「注册」创建学生账号，管理员可登录 `admin` 体验全部菜单。

---

## 项目结构

```
zhikao-cloud/
├── backend/                    # SpringBoot 3.2.5 后端
│   └── src/main/java/com/zhikao/
│       ├── common/             # Result、PageRequest、PageResult
│       ├── config/             # SecurityConfig、WebMvcConfig、MybatisPlusConfig
│       ├── controller/         # 10 个 REST 控制器
│       ├── dto/                # 请求体校验
│       ├── entity/             # 20+ 数据库实体
│       ├── filter/             # JwtAuthenticationFilter
│       ├── interceptor/        # JwtInterceptor（备用）
│       ├── mapper/             # 20+ MyBatis-Plus Mapper
│       ├── service/            # 业务接口 + impl 实现
│       ├── utils/              # JwtUtils
│       └── vo/                 # LoginVO 等响应体
│
├── frontend/                   # Vue 3 + TS + Vite
│   └── src/
│       ├── api/                # 10 个 API 模块
│       ├── components/layout/  # MainLayout 主布局
│       ├── router/             # 路由 + 角色守卫
│       ├── stores/             # Pinia 用户状态
│       ├── utils/              # Axios 拦截器（Token自动注入）
│       └── views/
│           ├── admin/          # 用户管理/考试管理/题库管理/数据看板
│           ├── auth/           # 登录/注册
│           ├── dashboard/      # 首页仪表盘
│           ├── discuss/        # 讨论区（版块/帖子/详情/编辑）
│           ├── exam/           # 考试列表
│           ├── favorite/       # 收藏夹
│           ├── group/          # 学习小组
│           ├── pk/             # PK大厅/排行榜
│           ├── question/       # 题库练习
│           ├── report/         # 学情报告
│           ├── user/           # 个人中心
│           └── wrongbook/      # 错题本
│
└── sql/schema.sql              # 完整数据库 DDL
```

---

## 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 框架 | Spring Boot 3.2.5 | 核心框架 |
| 安全 | Spring Security 6.2 + JWT 0.11.5 | 认证 + 角色鉴权 |
| ORM | MyBatis-Plus 3.5.7 | 数据库操作 |
| 数据库 | MySQL 8.0 + HikariCP | 主存储 |
| 前端 | Vue 3.4 + TypeScript + Vite 5 | SPA 应用 |
| UI | Element Plus 2.5 | 组件库 |
| 图表 | ECharts 5.5 | 学情报告可视化 |
| 路由 | Vue Router 4 | 路由 + 权限守卫 |
| 状态 | Pinia 2 | 用户状态管理 |
| HTTP | Axios | API 请求 + Token 自动注入 |

---

## 角色权限体系

| 模块 | 学生 | 教师 | 管理员 |
|------|:--:|:--:|:--:|
| 参加考试 | ✅ | — | — |
| 题库练习 | ✅ | — | — |
| 错题本 | ✅ | — | — |
| 收藏夹 | ✅ | — | — |
| 学情报告 | ✅ | ✅ | — |
| 组队PK | ✅ | — | — |
| 学习小组 | ✅ | — | — |
| 讨论区 | ✅ | ✅ | ✅ |
| 考试管理 | — | ✅ | ✅ |
| 题库管理 | — | ✅ | ✅ |
| 班级报告 | — | ✅ | ✅ |
| 用户管理 | — | — | ✅ |
| 个人中心 | ✅ | ✅ | ✅ |

---

## API 规范

所有接口前缀：`/api/v1`

### 认证
```
POST /auth/register     # 注册
POST /auth/login        # 登录 → 返回 JWT Token
POST /auth/refresh      # 刷新 Token
GET  /auth/me           # 当前用户信息
POST /auth/logout       # 退出
```

### 考试
```
GET  /exam/list         # 试卷列表
GET  /exam/detail/{id}  # 试卷详情
GET  /exam/take/{id}    # 获取考试题目（脱敏）
POST /exam/submit       # 提交答案（自动判分+错题收录）
GET  /exam/my-records   # 我的考试记录
POST /exam/create       # 创建考试（教师/管理员）
PUT  /exam/publish/{id} # 发布考试
```

### 题库
```
GET  /question/list     # 题目列表
POST /question/create   # 创建题目
GET  /question/detail/{id} # 题目详情
```

### 收藏夹
```
GET    /favorite/folders       # 收藏夹列表
POST   /favorite/folder        # 创建收藏夹
PUT    /favorite/folder/{id}   # 更新收藏夹
DELETE /favorite/folder/{id}   # 删除收藏夹
GET    /favorite/items         # 收藏项列表
POST   /favorite/item          # 添加收藏
DELETE /favorite/item/{id}     # 取消收藏
```

### 学情报告
```
GET /report/personal           # 个人报告（雷达图/折线图）
GET /report/exam-trend         # 成绩趋势
GET /report/knowledge-heatmap  # 知识点热力图
GET /report/weak-areas         # 薄弱分析
GET /report/class/{paperId}    # 班级报告（教师）
```

### 讨论区
```
GET  /discuss/sections           # 版块列表
GET  /discuss/posts              # 帖子列表
GET  /discuss/post/{id}          # 帖子详情
POST /discuss/post               # 发帖
POST /discuss/comment            # 评论
POST /discuss/like               # 点赞
```

### 组队PK
```
GET  /pk/teams           # 队伍列表
POST /pk/team            # 创建队伍
POST /pk/team/join       # 加入队伍
POST /pk/match/start     # 发起对战
GET  /pk/leaderboard     # 排行榜
```

### 学习小组
```
GET  /group/list             # 小组列表
POST /group                  # 创建小组
POST /group/{id}/join        # 加入小组
GET  /group/{id}/members     # 成员列表
POST /group/{id}/task        # 发布任务
```

---

## 已知注意事项

1. **JWT Token** 解析依赖 `claims.get("userId")`（不是 `claims.getId()`）
2. **数据库** sys_user 表需有 `deleted` 列；角色查询 SQL 不能含 `AND deleted = 0`
3. **Spring Security** 配置必须在 `SecurityFilterChain` 中加 `.securityMatcher("/**")`
4. **Vite Proxy** 需显式转发 `Authorization` 请求头
5. **JDK 要求** 21（非 17），Maven compiler 配置为 21

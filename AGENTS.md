# AGENTS.md — 智考云 (zhikao-cloud) 开发手册

> 给后续 AI Agent / 开发者的项目速查。基于 2026-06-22 ~ 06-23 两日 30+ 次修复的经验总结。

---

## 项目概览

| 项 | 值 |
|------|------|
| **项目名** | 智考云 — 在线考试与学习平台 |
| **后端** | Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 + Spring Security 6.2 + JWT 0.11.5 |
| **前端** | Vue 3.4 + TypeScript + Element Plus 2.5 + ECharts 5.5 + Vite 5 |
| **数据库** | MySQL 8.0 `zhikao_cloud` · root/123456 |
| **端口** | 后端 8080 (context-path `/api/v1`), 前端 3000 (Vite proxy → 8080) |
| **JDK** | 21 (非 17) |

---

## 架构速记

```
backend/src/main/java/com/zhikao/
  ├─ controller/    ← 20 个 REST 控制器 (@RestController)
  │   AuthController        - 登录/注册/Token刷新/当前用户
  │   ExamPaperController   - 试卷CRUD/发布/提交/批阅
  │   QuestionController    - 题库CRUD
  │   QuestionCategoryController - 题库分类(学科/题库/知识点树)
  │   ClassController       - 班级管理/加入/退出
  │   PkController          - PK组队/对战/排行榜
  │   GroupController       - 学习小组CRUD/加入/任务
  │   DiscussionController  - 讨论区版块/帖子/评论/点赞
  │   ReportController      - 个人/班级学情报告(雷达图/趋势/热力图)
  │   WrongNotebookController - 错题本
  │   FavoriteController    - 收藏夹(文件夹+收藏项)
  │   UserController        - 用户管理(超管)
  │   PointsController      - 积分查询/每日签到
  │   SysMessageController  - 系统消息/未读计数/已读标记
  │   AiGradingController   - AI主观题批改(/ai/grade)
  │   SmartRecommendController - 智能错题推荐(/smart/review, /smart/similar)
  │   CertificateController - 成绩证书PNG生成(/cert/generate)
  │   ExcelImportController - Excel批量导入题目(/import/questions)
  │   SmsController         - 短信验证码(模拟)(/sms/send, /sms/verify)
  │   FileUploadController  - 头像上传(/upload/avatar)
  │
  ├─ service/       ← 19 个业务接口
  │   核心: UserService, ExamPaperService, QuestionService, ExamSessionService
  │   AI: AiGradingService, SmartRecommendService
  │   社区: DiscussionService, PkService, GroupService
  │   辅助: PointsService, SysMessageService, SensitiveWordService
  │
  ├─ service/impl/  ← 16 个服务实现 (@Service + @Transactional)
  ├─ entity/        ← 32 个 MyBatis-Plus 实体 (@TableName)
  ├─ mapper/        ← 34 个 MyBatis-Plus Mapper (@Mapper + BaseMapper<T>)
  ├─ config/        ← 6 个配置类
  │   SecurityConfig       - Spring Security + JWT 过滤链
  │   MybatisPlusConfig    - 分页插件
  │   WebMvcConfig         - 静态资源映射(/uploads/**)
  │   WebConfig            - CORS 跨域
  │   SpaConfig            - SPA fallback (history mode)
  │   WebSocketConfig      - WebSocket /ws/pk
  │
  ├─ filter/        ← JwtAuthenticationFilter (OncePerRequestFilter)
  ├─ interceptor/   ← JwtInterceptor（备用）
  ├─ handler/       ← GlobalExceptionHandler (@RestControllerAdvice)
  ├─ websocket/     ← PkWebSocketHandler (PK对战实时通信)
  ├─ aspect/        ← AuditLogAspect (操作审计日志)
  ├─ common/        ← Result, PageRequest, PageResult
  ├─ dto/           ← LoginDTO, RegisterDTO（请求体校验）
  ├─ vo/            ← LoginVO 等响应体
  └─ utils/         ← JwtUtils

frontend/src/
  ├─ views/         ← 27 个页面组件
  │   admin/ (8): UserManage, ClassManage, ExamManage, QuestionManage,
  │               ClassReport, PKManage, GroupManage, DiscussManage
  │   auth/ (1): Login
  │   dashboard/ (1): Dashboard
  │   discuss/ (4): SectionList, PostList, PostDetail, PostEditor
  │   exam/ (3): ExamList, ExamDetail, ExamTaking
  │   favorite/ (1): Favorite
  │   group/ (2): GroupDiscovery, GroupDetail
  │   pk/ (3): PKLobby, PkMatch, Leaderboard
  │   question/ (1): QuestionBank
  │   report/ (1): PersonalReport
  │   user/ (1): Profile
  │   wrongbook/ (1): WrongBook
  │
  ├─ api/           ← 12 个 API 模块 (auth, exam, question, favorite,
  │                    report, wrongbook, profile, pk, discuss, group,
  │                    class, message)
  ├─ components/layout/ ← MainLayout 主布局(侧边栏+顶栏)
  ├─ router/        ← 路由配置 + 角色守卫 (index.ts)
  ├─ stores/        ← Pinia 用户状态 (user.ts)
  └─ utils/         ← request.ts (axios 拦截器 + token 自动注入)
```

---

## 🔴 高频陷阱 (MUST READ)

### 陷阱 1: Transient 字段永远为 null

**症状**: 前端展示空白，后端实体有 `@TableField(exist = false)` 字段但从未赋值。

**涉及实体**: `ClassMember.nickname/username`, `PkTeamMember.nickname/username`, `ClassEntity.teacherName`, `PkMatchRecord.username`

**修复模式**:
```java
// 在 ServiceImpl 的查询方法末尾加：
List<Long> userIds = records.stream().map(Entity::getUserId).distinct().toList();
List<User> users = userMapper.selectBatchIds(userIds);
Map<Long, User> userMap = new HashMap<>();
for (User u : users) userMap.put(u.getId(), u);
for (Entity e : records) {
    User u = userMap.get(e.getUserId());
    if (u != null) { e.setNickname(u.getNickname()); e.setUsername(u.getUsername()); }
}
```
> 已修复的查询: `ClassServiceImpl.listMembers()`, `listAllClasses()`, `PkServiceImpl.listTeamMembers()`, `getMatchRecords()`

### 陷阱 2: 前端假数据/硬编码覆盖

**症状**: 页面显示永远为 0，或刷新后状态丢失。

**经典案例**:
- 个人中心 `stats = reactive({examCount:0, accuracy:0,...})` 从未调用 API
- 签到 `Object.assign(pointsData, r.data, { checkedToday: false })` 硬编码覆盖后端
- Dashboard 统计直接 `reactive` 初始化不请求后端

**修复**: 所有数据必须从 API 获取，`Object.assign` 不要硬编码覆盖后端字段。

### 陷阱 3: 数据库列缺失

**症状**: `Unknown column 'xxx' in 'field list'`

**检查方法**: `DESCRIBE table_name` vs 实体 `@TableField` 定义

**已修复的表**:
- `study_group_member`: 缺 `created_at`, `updated_at`
- `study_group_chat`: 缺 `updated_at`
- `study_group_task`: 缺 `updated_at`

### 陷阱 4: JWT 过期处理

**症状**: 显示 "Request failed with status code 403"

**根因**: `JwtAuthenticationFilter` catch 了异常但不返回错误状态码，继续 filterChain → Spring Security 返回裸 403

**正确做法**:
```java
// JwtAuthenticationFilter catch 块:
response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
response.setContentType("application/json;charset=UTF-8");
response.getWriter().write("{\"code\":401,\"message\":\"身份过期，请重新登陆！\"}");
return;  // 不继续 filterChain
```
```ts
// request.ts error handler:
if (error.response?.status === 401) {
    ElMessage.error('身份过期，请重新登陆！')
    useUserStore().logout()
    window.location.href = '/auth/login'
}
```

### 陷阱 5: 数据库列名 vs 实体字段名

**注意**: MyBatis-Plus 默认 `map-underscore-to-camel-case: true`

| 数据库 | 实体字段 |
|--------|---------|
| `class_id` | `classId` |
| `user_id` | `userId` |
| `created_at` | `createdAt` |
| `teacher_name` | `teacherName` (transient) |

### 陷阱 6: JWT 解析方式

**症状**: `NullPointerException` 或 token 解析后 userId 为 null

**正确**: `claims.get("userId")` — 不是 `claims.getId()`，也不要用 `claims.getSubject()`

### 陷阱 7: 角色查询不能带 deleted 过滤

**症状**: 用户登录后角色始终为空

**根因**: `sys_role` 表没有 `deleted` 列，但查询 SQL 中带了 `AND deleted = 0`

**修复**: 去掉角色查询 SQL 中的 `deleted` 条件。

### 陷阱 8: Vite Proxy 不转发 Authorization 头

**症状**: 前端请求到达后端但 403

**修复**: `vite.config.ts` proxy 配置中显式添加:
```ts
headers: { 'Authorization': '' }
```

### 陷阱 9: SecurityFilterChain 缺少 securityMatcher

**症状**: `/auth/login` 也被拦截需要 token

**修复**: `SecurityConfig` 的 `SecurityFilterChain` 中加 `.securityMatcher("/**")`

### 陷阱 10: 实体内嵌枚举 getter 不返回 String

### 陷阱 11: 新增 Entity 字段但未同步 ALTER TABLE

**症状**: `Unknown column 'xxx' in 'field list'` → 全局异常处理返回 500，前端 axios 拦截器弹 toast 后静默吞掉，UI 看起来像「没反应」

**典型案例**: `Question.allowPractice` 字段在实体中已定义，但 `question_bank` 表缺少该列 → `WHERE allow_practice = 1` 直接抛 SQL 异常 → 用户点击分类后右侧无反应

**预防规则**:
- 实体新增字段后，**必须**同步 `sql/schema.sql` 的 CREATE TABLE 语句
- 同时**必须**创建增量迁移脚本（如 `sql/add_xxx.sql`）并在目标环境执行
- 可用 `SHOW COLUMNS FROM table_name` 快速验证列是否存在

**症状**: JSON 序列化后枚举字段是对象而非字符串

**涉及**: 实体类中直接定义 `getXxxText()` 方法匹配 `@TableField(exist = false)` 的前端展示字段

---

## 📊 核心表速查（完整 32 表）

### 用户与权限 (5)
| 表名 | 用途 | 关键字段 |
|------|------|---------|
| `sys_user` | 用户 | id, username, password, nickname, phone, avatar_url, deleted |
| `sys_user_role` | 用户-角色关联 | user_id, role_id |
| `sys_role` | 角色(无deleted列!) | id=1 SUPER_ADMIN, 2 TEACHER, 3 STUDENT, 4 GUEST |
| `user_profile` | 用户扩展资料 | user_id, real_name, school, grade, bio |
| `user_points` | 积分系统 | user_id, total_points, checkin_streak, last_checkin_date |

### 班级 (2)
| 表名 | 用途 | 关键字段 |
|------|------|---------|
| `sys_class` | 班级 | id, class_code, class_name, teacher_id, grade |
| `class_member` | 班级成员 | class_id, user_id, role(0学生/1教师), status(0待审核/1已加入) |

### 考试与题库 (6)
| 表名 | 用途 | 关键字段 |
|------|------|---------|
| `exam_paper` | 试卷 | id, title, creator_id, duration, total_score, status(0草稿/1发布) |
| `exam_paper_question` | 试卷-题目关联 | paper_id, question_id, sort |
| `exam_paper_class` | 试卷-班级关联 | paper_id, class_id |
| `exam_session` | 考试记录 | id, paper_id, user_id, score, status(1已提交/3已批阅) |
| `exam_answer` | 答题记录 | session_id, question_id, user_answer, is_correct, score |
| `question_category` | 题目分类 | id, category_name, category_type(1学科/2题库/3知识点), parent_id |

### 题库与选项 (3)
| 表名 | 用途 | 关键字段 |
|------|------|---------|
| `question_bank` | 题目主表 | id, title, question_type, difficulty, answer, score |
| `question_option` | 题目选项 | question_id, option_key(A/B/C/D), option_content |
| `wrong_notebook` | 错题本 | user_id, question_id, error_type, master_status |

### 收藏 (2)
| 表名 | 用途 | 关键字段 |
|------|------|---------|
| `favorite_folder` | 收藏文件夹 | user_id, folder_name |
| `favorite_item` | 收藏项 | user_id, folder_id, item_type, item_id |

### 讨论区 (5)
| 表名 | 用途 | 关键字段 |
|------|------|---------|
| `discussion_section` | 讨论版块 | id, section_name, description |
| `discussion_post` | 帖子 | id, section_id, user_id, title, content, view_count |
| `discussion_comment` | 评论 | id, post_id, user_id, parent_id, content |
| `discussion_like` | 点赞 | user_id, target_type, target_id |
| `discussion_follow` | 关注 | user_id, followed_user_id |

### PK对战 (5)
| 表名 | 用途 | 关键字段 |
|------|------|---------|
| `pk_team` | PK队伍 | id, team_name, captain_id, win_count, lose_count |
| `pk_team_member` | PK队员 | team_id, user_id, role, status(0待审核/1已加入) |
| `pk_match` | PK对战 | id, team_a_id, team_b_id, paper_id, status, winner_team_id |
| `pk_match_record` | 对战记录 | match_id, user_id, score |
| `leaderboard` | 排行榜 | user_id, total_score, rank |

### 学习小组 (4)
| 表名 | 用途 | 关键字段 |
|------|------|---------|
| `study_group` | 小组 | id, group_name, creator_id, max_members, current_members |
| `study_group_member` | 成员 | group_id, user_id, role(0成员/1组长), status |
| `study_group_task` | 任务 | group_id, title, content, due_date, status |
| `study_group_resource` | 资源 | group_id, user_id, resource_type, resource_url |

### 系统 (1)
| 表名 | 用途 | 关键字段 |
|------|------|---------|
| `sys_message` | 系统消息 | id, user_id, sender_id, content, type, is_read |

---

## 🗺️ 完整路由表

| 路径 | 页面 | 权限 |
|------|------|------|
| `/` | → 重定向到 `/dashboard` | - |
| `/auth/login` | 登录页 | 公开 |
| `/dashboard` | 仪表盘 | 登录用户 |
| `/exam` | 考试列表 | 登录用户 |
| `/exam/:id` | 试卷详情 | 登录用户 |
| `/exam/take/:id` | 参加考试 | 登录用户 |
| `/question` | 题库练习 | 登录用户 |
| `/wrongbook` | 错题本 | 登录用户 |
| `/favorite` | 收藏夹 | 登录用户 |
| `/user/profile` | 个人中心 | 登录用户 |
| `/report` | 学情报告 | 登录用户 |
| `/discuss` | 讨论区版块列表 | 登录用户 |
| `/discuss/create` | 发帖 | 登录用户 |
| `/discuss/posts` | 帖子列表 | 登录用户 |
| `/discuss/post/:postId` | 帖子详情 | 登录用户 |
| `/discuss/edit/:postId` | 编辑帖子 | 登录用户 |
| `/discuss/:sectionId` | 版块帖子列表 | 登录用户 |
| `/group` | 学习小组发现 | 登录用户 |
| `/group/:id` | 小组详情 | 登录用户 |
| `/pk` | PK大厅 | 登录用户 |
| `/pk/match/:id` | PK对战 | 登录用户 |
| `/pk/leaderboard` | PK排行榜 | 登录用户 |
| `/admin/exams` | 考试管理 | TEACHER+ |
| `/admin/questions` | 题库管理 | TEACHER+ |
| `/admin/reports` | 班级报告 | TEACHER+ |
| `/admin/discuss` | 讨论区管理 | TEACHER+ |
| `/admin/users` | 用户管理 | SUPER_ADMIN |
| `/admin/classes` | 班级管理 | TEACHER+ |
| `/admin/pk` | PK管理 | SUPER_ADMIN |
| `/admin/groups` | 小组管理 | SUPER_ADMIN |

**路由守卫逻辑** (`router/index.ts`):
- `meta.requiresAuth` → 未登录跳转 `/auth/login`
- `meta.requiresAdmin` → 非超级管理员跳转 `/dashboard`
- `meta.requiresTeacher` → 非教师/管理员跳转 `/dashboard`

**注意**：`/discuss/:sectionId` 是通配路由，必须放在精确路由之后，否则会拦截 `/discuss/create`。

---

## 🔌 完整 API 参考

所有接口前缀: `/api/v1`

### 认证 (`/auth`)
```
POST /auth/register          # 注册
POST /auth/login             # 登录 → 返回 JWT Token
POST /auth/refresh           # 刷新 Token
GET  /auth/me                # 当前用户信息
POST /auth/logout            # 退出
```

### 考试 (`/exam`)
```
GET    /exam/list            # 试卷列表
GET    /exam/detail/{id}     # 试卷详情
GET    /exam/take/{id}       # 获取考试题目（脱敏）
POST   /exam/submit          # 提交答案（自动判分+错题收录）
GET    /exam/my-records      # 我的考试记录
POST   /exam/create          # 创建考试（教师/管理员）
PUT    /exam/publish/{id}    # 发布考试
DELETE /exam/{id}            # 删除试卷
POST   /exam/grade/{sessionId} # 批阅试卷（教师）
```

### 题库 (`/question`) + 分类 (`/categories`)
```
GET    /question/list        # 题目列表（分页+多条件筛选）
POST   /question/create      # 创建题目
PUT    /question/{id}        # 更新题目
DELETE /question/{id}        # 删除题目
GET    /question/detail/{id} # 题目详情
GET    /categories           # 分类树（学科/题库/知识点三层）
POST   /categories           # 新增分类
```

### 错题本 (`/wrongbook`)
```
GET  /wrongbook/list         # 错题列表
POST /wrongbook/remove/{id}  # 移除错题
PUT  /wrongbook/master/{id}  # 标记已掌握
```

### 收藏夹 (`/favorite`)
```
GET    /favorite/folders     # 收藏夹列表
POST   /favorite/folder      # 创建收藏夹
PUT    /favorite/folder/{id} # 更新收藏夹
DELETE /favorite/folder/{id} # 删除收藏夹
GET    /favorite/items       # 收藏项列表
POST   /favorite/item        # 添加收藏
DELETE /favorite/item/{id}   # 取消收藏
```

### 学情报告 (`/report`)
```
GET /report/personal           # 个人报告（雷达图/折线图）
GET /report/exam-trend         # 成绩趋势
GET /report/knowledge-heatmap  # 知识点热力图
GET /report/weak-areas         # 薄弱分析
GET /report/class/{paperId}    # 班级报告（教师）
```

### 讨论区 (`/discuss`)
```
GET    /discuss/sections       # 版块列表
GET    /discuss/posts          # 帖子列表（按版块筛选）
GET    /discuss/post/{id}      # 帖子详情
POST   /discuss/post           # 发帖（含敏感词过滤）
POST   /discuss/comment        # 评论
POST   /discuss/like           # 点赞/取消点赞
DELETE /discuss/post/{id}      # 删除帖子
DELETE /discuss/comment/{id}   # 删除评论
```

### 组队PK (`/pk`)
```
GET    /pk/teams               # 队伍列表
POST   /pk/team                # 创建队伍
POST   /pk/team/join           # 加入队伍
POST   /pk/match/start         # 发起对战
GET    /pk/match/{id}          # 对战详情
GET    /pk/leaderboard         # 排行榜
```

### 学习小组 (`/group`)
```
GET    /group/list             # 小组列表
POST   /group                  # 创建小组
POST   /group/{id}/join        # 加入小组
GET    /group/{id}/members     # 成员列表
POST   /group/{id}/task        # 发布任务
```

### 班级 (`/classes`)
```
GET    /classes/list           # 班级列表
GET    /classes/my             # 我的班级
POST   /classes                # 创建班级
POST   /classes/join/{classId} # 加入班级
DELETE /classes/leave/{classId}# 退出班级
GET    /classes/members/{id}   # 班级成员
```

### 用户 (`/users`) — 超管
```
GET    /users/list             # 用户列表（分页）
PUT    /users/{id}/role        # 修改角色
DELETE /users/{id}             # 删除用户（软删除）
```

### 积分 (`/points`)
```
GET  /points/my                # 我的积分
POST /points/checkin           # 每日签到
```

### 系统消息 (`/messages`)
```
GET  /messages/list            # 消息列表
GET  /messages/unread-count    # 未读消息数
PUT  /messages/read/{id}       # 标记单条已读
PUT  /messages/read-all        # 标记全部已读
```

### AI 功能
```
POST /ai/grade                 # AI 主观题批改
GET  /smart/review             # 今日推荐复习错题
GET  /smart/similar/{id}       # 相似题目推荐
```

### 其他
```
GET    /cert/generate?name=&score=    # 成绩证书 PNG
POST   /import/questions              # Excel 批量导入题目
POST   /upload/avatar                 # 头像上传(JPG/PNG ≤2MB)
POST   /sms/send                      # 发送验证码(模拟)
POST   /sms/verify                    # 验证验证码
```

### API 响应格式

所有 API 通过 `GlobalExceptionHandler` 统一返回:
```json
{"code": 200, "message": "ok", "data": { ... }}
```

- 业务异常 `RuntimeException` → code=400
- 权限不足 `AccessDeniedException` → code=403
- 服务器异常 `Exception` → code=500
- JWT 过期 → HTTP 401 + JSON body `{"code":401,"message":"身份过期，请重新登陆！"}`

**前端 axios 拦截器**: `code === 200` 直接返回 `Result` 对象（已解包 `.data`），非 200 自动 `ElMessage.error`。

---

## 🎯 特色功能模块

### AI 智能批改 (`AiGradingService/AiGradingController`)
- `POST /ai/grade` — 接收学生答案+参考答案，模拟打分
- 前端可在主观题提交后调用此接口获得 AI 评分

### 智能推荐 (`SmartRecommendService`)
- `GET /smart/review` — 基于错题频率排序，推荐今日复习题目(Top 20)
- `GET /smart/similar/:id` — 同类型同难度相似题目推荐(Top 5)

### 证书生成 (`CertificateController`)
- `GET /cert/generate?name=张三&exam=期末考试&score=95` → 返回 PNG 图片
- 纯 Java AWT 渲染，无需第三方库

### 积分与签到 (`PointsService/PointsController`)
- 每日签到获得积分，连续签到有额外奖励(`checkin_streak`)

### 系统消息 (`SysMessageService/SysMessageController`)
- 支持系统广播消息和点对点消息
- 前端 `api/message.ts` 封装了列表/未读/已读标记

### Excel 批量导入 (`ExcelImportController`)
- `POST /import/questions` — 上传 .xlsx 文件批量导入题目
- Apache POI 解析，7列格式: 类型|题目|难度|...|答案|分值|选项A-D

### WebSocket PK (`WebSocketConfig / PkWebSocketHandler`)
- 端点: `ws://localhost:8080/ws/pk`
- PK 对战时实时推送双方答题进度

### 审计日志 (`AuditLogAspect`)
- 自动记录所有 Controller 请求 (除 AuthController)
- 格式: `[时间] METHOD /path | userId=xxx | Controller.method()`
- 异常时自动记录 error 日志

---

## 🔧 修复模式汇总

### 模式 A: 批量填充用户信息
**触发**: 列表/详情返回的对象没有 nickname/username 等 transient 字段
**文件**: `*ServiceImpl.java`
**方法**: `userMapper.selectBatchIds(userIds)` + 遍历赋值

### 模式 B: 数据库列补全
**触发**: `Unknown column 'xxx'`
**方法**: `ALTER TABLE xxx ADD COLUMN yyy ...`

### 模式 C: 前端数据层
**触发**: 页面永远显示 0 / 刷新后状态丢失
**方法**: `onMounted` → 调 API → `Object.assign` 不加硬编码覆盖

### 模式 D: 班级/队伍约束
**触发**: 学生可加入多个班级
**方法**: 后端加 `selectCount(status=1) > 0` 检查 + 前端按钮根据 `myClasses.length` 切换

### 模式 E: Report SQL 修复
**触发**: 图表数据全空
**方法**: 检查 `question_category.category_type` 过滤值是否匹配实际数据

### 模式 F: 敏感词过滤
**触发**: 讨论区帖子/评论需要内容审核
**方法**: `SensitiveWordService` 在发布前校验，含默认词库

---

## ⚡ 快速启动

```bash
# 1. 数据库
mysql -u root -p123456 -e "CREATE DATABASE IF NOT EXISTS zhikao_cloud DEFAULT CHARSET utf8mb4"
mysql -u root -p123456 zhikao_cloud < sql/schema.sql

# 2. 后端 (IDEA 打开 backend/pom.xml, JDK 21, 运行 ZhikaoCloudApplication)
#    或命令行:
cd backend && mvn spring-boot:run

# 3. 前端
cd frontend && npm install && npm run dev
```

**测试账号**:

| 角色 | 用户名 | 密码 | 权限 |
|------|--------|------|------|
| 管理员 | admin | 123123 | 全部功能 + 用户管理 |
| 教师 | teacher1 | 123123 | 考试/题库管理/班级报告 |
| 学生 | student1 | 123123 | 考试/练习/错题本/PK/小组 |

---

## 💡 UX 原则

1. **所有数据必须来自数据库查询** — 不允许前端硬编码假数据
2. **空状态友好** — 0 和 "未设置" 分开处理，不要让用户误以为是 bug
3. **按钮状态跟随数据** — 有班级则显示"退出"，无则显示"加入"
4. **布局不用 flex:1 撑满** — 内容决定高度比强制撑满更自然
5. **敏感词拦截要在发布前** — 调用 `SensitiveWordService` 而非事后审核
6. **前端路由参数路由必须后置** — `/discuss/:sectionId` 放在 `/discuss/create` 之后

---

## 📁 关键文件索引

| 用途 | 文件路径 |
|------|---------|
| 数据库DDL | `sql/schema.sql` |
| 测试数据 | `sql/test_data.sql` |
| 测试账号 | `sql/test_accounts.sql` |
| 安全配置 | `backend/.../config/SecurityConfig.java` |
| JWT工具 | `backend/.../utils/JwtUtils.java` |
| 全局异常处理 | `backend/.../handler/GlobalExceptionHandler.java` |
| 前端请求拦截器 | `frontend/src/utils/request.ts` |
| 前端路由守卫 | `frontend/src/router/index.ts` |
| 用户状态管理 | `frontend/src/stores/user.ts` |

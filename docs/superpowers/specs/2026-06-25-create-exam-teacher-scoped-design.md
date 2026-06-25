# 创建考试 — 教师范围框定组卷 (teacher-scoped create-exam)

- Date: 2026-06-25
- Status: Design — pending user review

## 背景

当前"创建考试"流程直接跳到选题，缺乏对题库来源的框定。管理员创建随机试卷时曾因 `randomPaper` 的 creator-identity 过滤导致 0 题（已修复：移除按创建者身份过滤）。本重构引入"显式选择出题老师"作为题库范围，区别于已移除的身份过滤——范围由管理员主动选择，而非按试卷创建者身份推断。

## 决策

1. **仅管理员**走"选择出题老师"步骤；教师创建时自动框定为自己的题库（保持现有教师隔离，不出选择步骤）。
2. `exam_paper` 持久化 `teacher_ids` + `random_config`，支持日后"重新抽题"。
3. "重新抽题"仅限**草稿 (status=0)** 的随机试卷（已发布/已结束试卷一旦有作答记录，重抽会破坏 `exam_answer`）。
4. 向导结构：**3 步**（① 选老师 → ② 基本信息+组卷模式 → ③ 选题/随机配置）；发布保持现有独立流程（创建→草稿→列表点"发布"）。
5. 空题库校验：**逐个老师判空**——任一选中老师名下 0 题→阻断并指名（"老师X名下暂无可用试题"）。

## 数据模型

`exam_paper` 新增两列：
- `teacher_ids VARCHAR(255)` — 选中老师 id 逗号分隔（如 `"1,13,34"`）；教师创建的试卷存自己的 id。
- `random_config TEXT` — 随机组卷配置 JSON（`typeCountMap` / `totalCount` / `difficultyPercentMap`），用于"重新抽题"。固定组卷可为空。

迁移脚本 `sql/add_exam_teacher_scope.sql`：
```sql
ALTER TABLE exam_paper ADD COLUMN teacher_ids VARCHAR(255) NULL COMMENT '出题老师id逗号分隔';
ALTER TABLE exam_paper ADD COLUMN random_config TEXT NULL COMMENT '随机组卷配置JSON，用于重抽';
```
实体 `ExamPaper.java` 同步加 `teacherIds` / `randomConfig` 字段。

## 后端

1. **`QuestionService.pageList` / `randomPaper`**：`creatorId: Long` → `creatorIds: List<Long>`，用 `IN (...)` 查询。`randomPaper` 新签名：`randomPaper(Long categoryId, Integer totalCount, Map<Integer,Integer> typeCountMap, Map<Integer,Integer> difficultyPercentMap, List<Long> creatorIds)`。`creatorIds` 非空时过滤，空时不过滤。
2. **`POST /exam/create`**：payload 增 `teacherIds: Long[]`（admin 必填；教师端后端自动填 `[自己id]`）；`randomConfig` 已有。`createPaper` 把 `teacherIds` 存入 `teacher_ids`，`randomConfig` 序列化存入 `random_config`；调 `randomPaper(..., creatorIds=teacherIds)` 抽题。
3. **`POST /exam/{id}/reassemble`（新）**：`@PreAuthorize hasAnyRole('TEACHER','SUPER_ADMIN')`；校验 `paperType==2 && status==0`，否则 400；用存储的 `teacher_ids` + `random_config` 重跑 `randomPaper`；事务内删除该 paper 旧 `exam_paper_question` 行→插入新的→重算 `totalScore` 更新回 `exam_paper`。返回新题目数+总分。
4. **`GET /exam/detail/{id}`**：解析 `teacher_ids`→批量查 `sys_user` 拿昵称→响应里加 `teacherNames: String[]`。

## 前端 (`ExamManage.vue`)

- 向导改 3 步：`el-steps :active="createStep"` 三段——① 选老师（admin 多选必填，复用发布弹窗已有的 `GET /class/teachers`；教师角色跳过此步，`createStep` 直接从 1 开始）② 基本信息+组卷模式 ③ 选题(fixed)/随机配置(random)。
- ①→② 校验：admin 必选 ≥1 老师；**逐个老师**查题目数（`getQuestionList({ creatorIds:[X], size:1 })` 看 `total`），任一为 0 → `ElMessage.warning("老师X名下暂无可用试题")` 并阻断。
- 在 ③ 切换老师：`ElMessageBox.confirm("题库范围已变更，是否重新选题？")` → 确认后清空 `selectedQuestions`/`selectedScores`/`randomTypes` 计数并重新 `searchQuestions`。
- `searchQuestions()` 传 `creatorIds = selectedTeacherIds`。
- `handleCreate()` payload 增 `teacherIds`；`randomConfig` 已发送。
- 详情弹窗 `el-descriptions` 加一行"出题来源：张老师, 李老师"。
- 随机草稿试卷的详情弹窗加"重新抽题"按钮 → `POST /exam/{id}/reassemble` → 成功后重载详情。

## 与最近 randomPaper 修复的关系

最近移除的是 **creator-identity 过滤**（按试卷创建者身份抽题）；本设计引入 **explicit-selection 过滤**（按管理员显式选的老师抽题）。机制不同，不冲突——`randomPaper` 的 `creatorIds` 参数现在由前端的老师选择驱动，而非 `paper.getCreatorId()`。

## 验证

- admin 选 1 个有题的老师→随机组卷→试卷有题（q_count>0）；选一个 0 题老师→阻断且指名。
- 教师创建→自动框定自己→选题正常。
- 草稿随机试卷"重新抽题"→题目列表变化、totalScore 重算；已发布试卷重抽→接口拒绝（400）。
- 详情弹窗显示"出题来源"。
- curl：`POST /exam/create` 带 `teacherIds` → 200，DB 中 `teacher_ids`/`random_config` 已存；`POST /exam/{id}/reassemble` → 200 且题目数>0。

## 不做（YAGNI）

- 不做"教研组"实体（schema 无 department 表；多老师=多选，不建组）。
- 不集成发布到向导（保持创建→草稿→独立发布）。
- 不做已发布试卷的重抽（安全）。

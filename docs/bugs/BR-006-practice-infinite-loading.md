# BR-006：题库练习选中分类后无限加载

| 字段 | 值 |
|------|-----|
| **Bug ID** | BR-006 |
| **严重程度** | 🔴 P0 - 阻塞 |
| **模块** | 题库练习（学生端） |
| **影响范围** | 所有已入班学生 |
| **报告人** | 肖先生 |
| **发现日期** | 2026-06-24 |
| **状态** | ✅ 已修复 |

---

## 复现步骤

1. 使用学生账号登录系统（如 mark / 123456）
2. 确保该学生已加入班级（高三冲刺班，编号 EAD476）
3. 点击左侧导航「题库练习」
4. 页面自动加载左侧分类树（显示「数学」「高中数学」）
5. 点击左侧「高中数学」分类

## 预期结果

- 右侧题目列表区域加载完成，显示 44 道高中数学题目
- 或显示「该分类下暂无题目」的空状态提示
- Loading 状态应在 3 秒内结束

## 实际结果

- 右侧显示持续的 Loading 转圈动画，不消失
- 题目列表始终不出现
- 浏览器 DevTools Network 面板显示 `/question/practice` 请求处于 Pending 状态

---

## 根因分析

1. **主因**：前端 `dist/` 构建产物未更新（最后构建时间 Jun 23 21:45，早于源码修复），浏览器加载的是旧代码，缺少 API 失败后的错误兜底处理。

2. **次因**：`frontend/src/views/question/QuestionBank.vue` 的 `onMounted` 缺少 `catch` 块——当 `/question/category/practice-tree` API 因 JWT 过期（401）或网络异常返回错误时，异常未被捕获，`categories` 保持为空数组，但 `catLoading` 在 `finally` 中已置为 false，模板渲染空白区域而非错误提示。

3. **诱因**：JWT access token 有效期仅 2 小时，学生长时间使用后 token 过期，API 返回 401，触发加载僵死。

## 修复方案

| # | 文件 | 改动 | 行号 |
|---|------|------|------|
| 1 | `frontend/src/views/question/QuestionBank.vue` | `onMounted` 添加 `catch` 块：清空 `categories`，确保空状态提示正确显示 | +214-217 |
| 2 | 同上 | `loadQuestions` `catch` 块简化：移除条件判断，改为始终清空 `questions` 并输出 `console.error` | ~167-170 |
| 3 | `frontend/dist/` | 重新构建 `npm run build`，确保修复生效 | — |

---

## 排查方向（供后续类似问题参考）

1. **Network 面板**
   - 检查 `/question/category/practice-tree` 是否返回 HTTP 200 且 `data` 数组非空
   - 检查 `/question/practice?categoryId=XX` 是否处于 Pending 状态或返回 401/403/500

2. **Console 面板**
   - 查看是否有 `[Practice] 加载题目失败:` 前缀的 console.error 日志

3. **数据库链路验证**
   ```sql
   -- 学生 → 班级 → 教师 → 分类 → 题目 完整链路
   SELECT cm.user_id, sc.teacher_id
   FROM class_member cm
   JOIN sys_class sc ON cm.class_id = sc.id
   WHERE cm.user_id = 51 AND cm.status = 1;

   SELECT id, category_name, allow_practice
   FROM question_category
   WHERE creator_id = 13 AND allow_practice = 1 AND status = 1;

   SELECT COUNT(*) FROM question_bank
   WHERE category_id = 27 AND allow_practice = 1 AND status = 1;
   ```

4. **JWT 有效期**
   - 检查 `accessTokenExpire` 字段（默认 7200000ms = 2h）
   - 检查浏览器 localStorage 中 `access_token` 是否过期

5. **后端日志**
   - 搜索 `JWT认证失败` 关键词确认是否为 token 过期
   - 搜索 `系统异常` 关键词确认是否有其他错误

---

## 验证方法

1. 重新构建前端：`cd frontend && npm run build`
2. 启动开发服务器：`cd frontend && npm run dev`
3. 用 mark / 123456 登录系统
4. 进入「题库练习」→ 点击「高中数学」→ 应显示 44 道题目列表
5. 测试 token 过期场景：等待 2 小时后访问 → 应跳转登录页或显示空状态提示

## 修改文件

- `frontend/src/views/question/QuestionBank.vue`：第 167-170 行（loadQuestions catch）、第 214-217 行（onMounted catch）

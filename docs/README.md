# 项目文档

| 目录 | 说明 |
|---|---|
| `changelog/` | 历史 bug 报告 + 修复记录(按 ID) |
| `archive/` | 已落地的设计文档(决策溯源用,不再变更) |

## changelog/

- `BR-006-practice-infinite-loading.md` — 题库练习选中分类后无限加载

## archive/

- `2026-06-25-create-exam-teacher-scoped-design.md` — 创建考试引入"出题教师"范围(已落地)

## 写新文档放在哪?

- 当前未修复的 bug:放 `changelog/BR-XXX-短描述.md`,命名仿照 BR-006
- 设计稿/决策记录:放 `archive/`,带日期前缀
- 部署/运维:放 `../deploy/`(已存在)
- API 文档:运行后端后看 `http://localhost:8080/api/v1/doc.html`(Knife4j)
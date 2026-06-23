<template>
  <div class="practice-page">
    <div class="page-header">
      <h2>📝 题库练习</h2>
    </div>

    <div class="practice-layout" v-if="categories.length > 0">
      <!-- 左侧分类树 -->
      <div class="category-sidebar">
        <div class="sidebar-title">练习分类</div>
        <div class="category-tree">
          <div v-for="root in categories" :key="root.id" class="tree-node">
            <!-- 有子节点：点击展开/折叠 -->
            <div v-if="root.children && root.children.length > 0" class="tree-parent" @click="toggleExpand(root.id)">
              <span class="expand-icon">{{ expandedIds.has(root.id) ? '▼' : '▶' }}</span>
              <span>{{ root.categoryName }}</span>
            </div>
            <!-- 无子节点（叶子）：点击直接选中 -->
            <div v-else class="tree-child"
                 :class="{ active: selectedCategoryId === root.id }"
                 @click="selectCategory(root)">
              {{ root.categoryName }}
            </div>
            <div v-if="expandedIds.has(root.id)" class="tree-children">
              <div v-for="child in root.children" :key="child.id"
                   class="tree-child"
                   :class="{ active: selectedCategoryId === child.id }"
                   @click="selectCategory(child)">
                {{ child.categoryName }}
                <span class="q-count">({{ child._questionCount || 0 }})</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧题目列表 -->
      <div class="question-area">
        <div class="area-header" v-if="currentCategory">
          <span>{{ currentCategory.categoryName }}</span>
          <el-tag size="small">{{ questions.length }} 题</el-tag>
        </div>
        <div v-else class="area-header">
          <span>请选择左侧分类</span>
        </div>

        <el-empty v-if="!selectedCategoryId" description="请从左侧选择一个练习分类" />
        <div v-else-if="questions.length === 0 && !qLoading" class="empty-hint">
          该分类下暂无题目
        </div>

        <div v-else class="question-list" v-loading="qLoading">
          <div v-for="(q, i) in questions" :key="q.id" class="question-card">
            <div class="q-header">
              <el-tag size="small" :type="typeColor(q.questionType)">{{ typeLabel(q.questionType) }}</el-tag>
              <el-tag size="small" effect="plain">{{ diffLabel(q.difficulty) }}</el-tag>
              <span class="q-index">#{{ i + 1 }}</span>
            </div>
            <div class="q-title">{{ q.title }}</div>

            <!-- 选项 -->
            <div v-if="q.questionType === 1 || q.questionType === 2" class="q-options">
              <div v-for="opt in (q._options || [])" :key="opt.optionLabel"
                   class="q-option"
                   :class="{
                     'selected': selectedAnswers[q.id] === opt.optionLabel,
                     'correct': showAnswer && opt.isCorrect === 1,
                     'wrong': showAnswer && selectedAnswers[q.id] === opt.optionLabel && opt.isCorrect !== 1
                   }"
                   @click="!showAnswer && selectAnswer(q, opt.optionLabel)">
                <span class="opt-label">{{ opt.optionLabel }}</span>
                <span class="opt-content">{{ opt.optionContent }}</span>
              </div>
            </div>

            <!-- 判断 -->
            <div v-else-if="q.questionType === 3" class="q-options">
              <div class="q-option" :class="{ selected: selectedAnswers[q.id] === '正确', correct: showAnswer && q.answer === '正确' }"
                   @click="!showAnswer && selectAnswer(q, '正确')">✅ 正确</div>
              <div class="q-option" :class="{ selected: selectedAnswers[q.id] === '错误', correct: showAnswer && q.answer === '错误' }"
                   @click="!showAnswer && selectAnswer(q, '错误')">❌ 错误</div>
            </div>

            <!-- 填空/简答 -->
            <div v-else class="q-input">
              <el-input v-model="selectedAnswers[q.id]" :disabled="showAnswer"
                        :placeholder="q.questionType===4?'请输入答案':'请输入简要答案'" />
            </div>

            <!-- 答案反馈 -->
            <div v-if="showAnswer" class="q-feedback" :class="isAnswerCorrect(q) ? 'feedback-ok' : 'feedback-err'">
              <span v-if="isAnswerCorrect(q)">✅ 回答正确</span>
              <span v-else>❌ 正确答案：{{ q.answer }}</span>
              <div v-if="q.answerAnalysis" class="q-analysis">{{ q.answerAnalysis }}</div>
            </div>
          </div>
        </div>

        <div v-if="selectedCategoryId && questions.length > 0" class="submit-bar">
          <el-button v-if="!showAnswer" type="primary" @click="submitAnswers">提交答案</el-button>
          <el-button v-else @click="resetPractice">重新练习</el-button>
        </div>
      </div>
    </div>

    <el-empty v-else-if="!catLoading" description="暂无可练习的题库" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const categories = ref<any[]>([])
const selectedCategoryId = ref<number | null>(null)
const currentCategory = ref<any>(null)
const questions = ref<any[]>([])
const selectedAnswers = reactive<Record<number, string>>({})
const showAnswer = ref(false)
const catLoading = ref(false)
const qLoading = ref(false)
const expandedIds = ref(new Set<number>())

const typeLabels: Record<number, string> = { 1: '单选', 2: '多选', 3: '判断', 4: '填空', 5: '简答' }
const typeColor = (t: number) => (['', '', 'warning', 'success', 'info', 'danger'][t] || '')
const diffLabel = (d: number) => (['', '⭐', '⭐⭐', '⭐⭐⭐'][d] || '')

function toggleExpand(id: number) {
  if (expandedIds.value.has(id)) expandedIds.value.delete(id)
  else expandedIds.value.add(id)
}

async function selectCategory(cat: any) {
  selectedCategoryId.value = cat.id
  currentCategory.value = cat
  showAnswer.value = false
  // 清除旧的答案
  Object.keys(selectedAnswers).forEach(k => delete selectedAnswers[Number(k)])
  // 先清空旧列表，显示 loading 状态
  questions.value = []
  await loadQuestions(cat.id)
}

/**
 * 并行加载单道题目的选项详情，单题失败不影响其他题。
 */
async function loadOptions(q: any) {
  if (q.questionType === 1 || q.questionType === 2) {
    try {
      const dRes = await request.get(`/question/detail/${q.id}`)
      q._options = dRes.data?.options || []
    } catch { q._options = [] }
  }
}

async function loadQuestions(categoryId: number) {
  qLoading.value = true
  try {
    const res = await request.get('/question/practice', {
      params: { categoryId, current: 1, size: 50 }
    })
    const list = res.data?.records || []
    // 并行加载所有题目的选项，避免串行 await 导致的长时间卡死
    await Promise.all(list.map(q => loadOptions(q)))
    questions.value = list
  } catch (e: any) {
    // 请求失败时清空列表，保持选中状态以便用户重试
    questions.value = []
    // axios 拦截器已弹窗提示错误，此处仅做兜底
    if (e?.message && !e.response) {
      ElMessage.error('加载题目失败，请稍后重试')
    }
  } finally { qLoading.value = false }
}

function selectAnswer(q: any, answer: string) {
  selectedAnswers[q.id] = answer
}

function isAnswerCorrect(q: any) {
  return selectedAnswers[q.id] && selectedAnswers[q.id].trim() === (q.answer || '').trim()
}

function submitAnswers() {
  const unanswered = questions.value.filter(q => !selectedAnswers[q.id])
  if (unanswered.length > 0) {
    ElMessage.warning(`还有 ${unanswered.length} 道题未作答`)
    return
  }
  showAnswer.value = true
  const correct = questions.value.filter(q => isAnswerCorrect(q)).length
  ElMessage.success(`正确 ${correct}/${questions.value.length}`)
}

function resetPractice() {
  showAnswer.value = false
  Object.keys(selectedAnswers).forEach(k => delete selectedAnswers[Number(k)])
}

onMounted(async () => {
  catLoading.value = true
  try {
    const res = await request.get('/question/category/practice-tree')
    categories.value = res.data || []
    // 自动选中第一个分类
    if (categories.value.length > 0) {
      const first = categories.value[0]
      if (first.children && first.children.length > 0) {
        // 有子分类：展开并选中第一个子分类
        expandedIds.value.add(first.id)
        await selectCategory(first.children[0])
      } else {
        // 无子分类（叶子）：直接选中
        await selectCategory(first)
      }
    }
  } finally { catLoading.value = false }
})
</script>

<style scoped>
.practice-page { padding: 20px 24px; max-width: 1200px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; }

.practice-layout { display: flex; gap: 20px; }
.category-sidebar { width: 240px; flex-shrink: 0; background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.sidebar-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #f0f0f0; }
.tree-parent { cursor: pointer; padding: 8px 0; font-weight: 500; color: #303133; display: flex; align-items: center; gap: 4px; }
.tree-child { cursor: pointer; padding: 7px 8px 7px 20px; font-size: 13px; color: #606266; border-radius: 6px; display: flex; justify-content: space-between; align-items: center; }
.tree-child:hover { background: #f5f7fa; }
.tree-child.active { background: #ecf5ff; color: #409EFF; font-weight: 500; }
.q-count { font-size: 11px; color: #909399; }

.question-area { flex: 1; background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.area-header { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 16px; display: flex; align-items: center; gap: 10px; }

.question-card { padding: 16px; margin-bottom: 12px; background: #fafafa; border-radius: 10px; border: 1px solid #f0f0f0; }
.q-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.q-title { font-size: 15px; color: #303133; margin-bottom: 10px; }

.q-options { display: flex; flex-direction: column; gap: 6px; }
.q-option { display: flex; align-items: center; gap: 10px; padding: 8px 12px; border-radius: 6px; border: 1px solid #e8e8e8; cursor: pointer; transition: all .2s; }
.q-option:hover { border-color: #409EFF; }
.q-option.selected { border-color: #409EFF; background: #ecf5ff; }
.q-option.correct { border-color: #67c23a; background: #f0f9eb; }
.q-option.wrong { border-color: #f56c6c; background: #fef0f0; }
.opt-label { width: 22px; height: 22px; border-radius: 50%; background: #e8e8e8; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600; flex-shrink: 0; }
.opt-content { font-size: 13px; color: #606266; }

.q-input { margin-top: 4px; }

.q-feedback { margin-top: 8px; padding: 8px 12px; border-radius: 6px; font-size: 13px; }
.feedback-ok { background: #f0f9eb; color: #67c23a; }
.feedback-err { background: #fef0f0; color: #f56c6c; }
.q-analysis { margin-top: 4px; font-size: 12px; color: #909399; }

.submit-bar { margin-top: 16px; text-align: center; }

.empty-hint { text-align: center; padding: 40px 0; color: #909399; font-size: 14px; }
</style>

<template>
  <div class="practice-page">
    <div class="page-header">
      <h2>题库练习</h2>
    </div>

    <div class="practice-layout" v-if="categories.length > 0">
      <!-- 左侧分类树（保持不变） -->
      <div class="category-sidebar">
        <div class="sidebar-title">练习分类</div>
        <div class="category-tree">
          <div v-for="root in categories" :key="root.id" class="tree-node">
            <div v-if="root.children && root.children.length > 0" class="tree-parent" @click="toggleExpand(root.id)">
              <span class="expand-icon">{{ expandedIds.has(root.id) ? '▼' : '▶' }}</span>
              <span>{{ root.categoryName }}</span>
            </div>
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

      <!-- 右侧练习区域 -->
      <div class="question-area">
        <!-- 空状态：未选择分类 -->
        <el-empty v-if="!selectedCategoryId" description="请从左侧选择一个练习分类" />

        <!-- 空状态：分类下无题目 -->
        <div v-else-if="filteredQuestions.length === 0 && !qLoading" class="empty-hint">
          该分类下暂无题目
        </div>

        <!-- 练习报告面板 -->
        <div v-else-if="showReport" class="report-panel">
          <div class="report-header">
            <h3>练习报告</h3>
            <el-tag size="large" :type="accuracy >= 60 ? 'success' : 'danger'" effect="dark">
              {{ accuracy }}%
            </el-tag>
          </div>

          <div class="report-stats">
            <div class="stat-item">
              <span class="stat-num">{{ filteredQuestions.length }}</span>
              <span class="stat-label">总题数</span>
            </div>
            <div class="stat-item stat-ok">
              <span class="stat-num">{{ score }}</span>
              <span class="stat-label">正确</span>
            </div>
            <div class="stat-item stat-err">
              <span class="stat-num">{{ filteredQuestions.length - score }}</span>
              <span class="stat-label">错误</span>
            </div>
          </div>

          <!-- 错题回顾 -->
          <div v-if="wrongQuestions.length > 0" class="wrong-review">
            <h4>错题回顾（{{ wrongQuestions.length }} 题）</h4>
            <div v-for="wq in wrongQuestions" :key="wq.id" class="wrong-item" @click="jumpToQuestion(wq.id)">
              <el-tag size="small" :type="typeColor(wq.questionType)">{{ typeLabels[wq.questionType] }}</el-tag>
              <span class="wrong-title">{{ wq.title }}</span>
              <span class="wrong-answer">你的答案：{{ answerState[wq.id]?.userAnswer }}</span>
              <span class="right-answer">正确答案：{{ wq.answer }}</span>
            </div>
          </div>

          <!-- 全部正确 -->
          <div v-else class="all-correct">
            全部正确，太棒了！
          </div>

          <div class="report-actions">
            <el-button type="primary" @click="resetPractice">重新开始</el-button>
            <el-button @click="backToCategory">返回分类</el-button>
          </div>
        </div>

        <!-- 单题卡片模式 -->
        <div v-else class="single-card-mode" v-loading="qLoading">
          <!-- 顶部：分类名 + 进度 -->
          <div class="card-top-bar">
            <span class="current-cat-name">{{ currentCategory?.categoryName || '' }}</span>
            <div class="progress-wrap">
              <el-progress :percentage="Math.round((currentIndex + 1) / filteredQuestions.length * 100)" :stroke-width="6" :show-text="false" />
              <span class="progress-text">第 {{ currentIndex + 1 }} / {{ filteredQuestions.length }} 题</span>
            </div>
            <!-- 标签筛选 -->
            <el-select v-model="filterTagId" placeholder="按标签筛选" clearable size="small" style="width:150px" @change="applyTagFilter">
              <el-option v-for="t in questionTags" :key="t.id" :label="t.tagName" :value="t.id" />
            </el-select>
            <!-- 自动跳转开关 -->
            <el-switch v-model="autoNext" size="small" active-text="自动下一题" />
          </div>

          <!-- 中央：当前题目卡片 -->
          <transition name="fade-card" mode="out-in">
            <div v-if="currentQuestion" :key="currentQuestion.id" class="q-card-wrapper">
              <el-card class="question-card-main" shadow="hover">
                <!-- 题目标签行 -->
                <div class="q-header">
                  <el-tag size="small" :type="typeColor(currentQuestion.questionType)">
                    {{ typeLabels[currentQuestion.questionType] }}
                  </el-tag>
                  <el-tag size="small" effect="plain">{{ diffLabel(currentQuestion.difficulty) }}</el-tag>
                  <!-- 收藏按钮 -->
                  <span class="fav-star" @click.stop="toggleFavorite(currentQuestion)" :title="favStatus[currentQuestion.id]?.favorited ? '取消收藏' : '添加收藏'">
                    {{ favStatus[currentQuestion.id]?.favorited ? '★' : '☆' }}
                  </span>
                  <!-- 解析/详情/打标签 -->
                  <el-button size="small" link type="primary" @click.stop="openQaDialog(currentQuestion)">
                    解析/标签
                  </el-button>
                </div>

                <!-- 题目标题 -->
                <div class="q-title">{{ currentQuestion.title }}</div>

                <!-- 选项列表：单选 / 多选 -->
                <div v-if="currentQuestion.questionType === 1 || currentQuestion.questionType === 2" class="q-options">
                  <div v-for="opt in (currentQuestion._options || [])" :key="opt.optionLabel"
                       class="q-option"
                       :class="optionClass(currentQuestion, opt)"
                       @click="selectAnswer(currentQuestion, opt.optionLabel)">
                    <span class="opt-label">{{ opt.optionLabel }}</span>
                    <span class="opt-content">{{ opt.optionContent }}</span>
                    <span v-if="isAnswered(currentQuestion) && opt.isCorrect === 1" class="opt-icon">✅</span>
                    <span v-else-if="isAnswered(currentQuestion) && answerState[currentQuestion.id]?.userAnswer === opt.optionLabel && opt.isCorrect !== 1" class="opt-icon">❌</span>
                  </div>
                </div>

                <!-- 判断题 -->
                <div v-else-if="currentQuestion.questionType === 3" class="q-options">
                  <div class="q-option"
                       :class="tfOptionClass(currentQuestion, '正确')"
                       @click="selectAnswer(currentQuestion, '正确')">
                    <span class="opt-label">✓</span>
                    <span class="opt-content">正确</span>
                  </div>
                  <div class="q-option"
                       :class="tfOptionClass(currentQuestion, '错误')"
                       @click="selectAnswer(currentQuestion, '错误')">
                    <span class="opt-label">✗</span>
                    <span class="opt-content">错误</span>
                  </div>
                </div>

                <!-- 填空 / 简答 -->
                <div v-else class="q-input-area">
                  <el-input v-model="inputAnswer" :disabled="isAnswered(currentQuestion)"
                            :placeholder="currentQuestion.questionType === 4 ? '请输入答案' : '请输入简要答案'"
                            @keyup.enter="submitInputAnswer" />
                  <el-button v-if="!isAnswered(currentQuestion)" type="primary" size="small" class="input-submit-btn"
                             @click="submitInputAnswer" :disabled="!inputAnswer.trim()">
                    确认
                  </el-button>
                </div>

                <!-- 答案反馈 -->
                <transition name="fade-feedback">
                  <div v-if="isAnswered(currentQuestion)" class="q-feedback"
                       :class="answerState[currentQuestion.id]?.isCorrect ? 'feedback-ok' : 'feedback-err'">
                    <span v-if="answerState[currentQuestion.id]?.isCorrect">✅ 回答正确</span>
                    <span v-else>❌ 正确答案：{{ currentQuestion.answer }}</span>
                    <div v-if="currentQuestion.answerAnalysis" class="q-analysis">{{ currentQuestion.answerAnalysis }}</div>
                  </div>
                </transition>
              </el-card>
            </div>
          </transition>

          <!-- 底部导航栏 -->
          <div class="card-bottom-bar">
            <el-button-group>
              <el-button :disabled="currentIndex <= 0" @click="goPrev">← 上一题</el-button>
              <el-button v-if="currentIndex < filteredQuestions.length - 1" type="primary" @click="goNext">下一题 →</el-button>
              <el-button v-else type="success" @click="submitAll">提交</el-button>
            </el-button-group>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-else-if="!catLoading" description="暂无可练习的题库，请先加入班级" />

    <!-- 题目解析 / 标签 / 收藏笔记 弹层 -->
    <el-dialog v-model="qaDialogVisible" title="题目解析与标签" width="640px" @closed="onQaDialogClosed">
      <div v-if="qaDialogQuestion">
        <div class="qa-q-title">{{ qaDialogQuestion.title }}</div>

        <!-- 收藏笔记 -->
        <div class="qa-section">
          <div class="qa-section-title">收藏</div>
          <div class="qa-fav-row">
            <el-button v-if="!qaFavorited" type="primary" size="small" @click="doFavorite">添加收藏</el-button>
            <el-button v-else type="warning" size="small" @click="doUnfavorite">取消收藏</el-button>
            <el-input v-model="qaFavNote" placeholder="收藏笔记（可选）" size="small" style="width:300px" />
          </div>
        </div>

        <!-- 题目解析 -->
        <div class="qa-section">
          <div class="qa-section-title">解析</div>
          <div v-if="qaAnalysisLoading" v-loading="true" style="min-height:80px"></div>
          <template v-else>
            <div v-if="qaAnalysis">
              <div class="qa-analysis-text">{{ qaAnalysis.textAnalysis || '（无文字解析）' }}</div>
              <div v-if="qaAnalysis.videoUrl" class="qa-video">
                <a :href="qaAnalysis.videoUrl" target="_blank">🎬 视频解析</a>
              </div>
              <div v-if="qaAnalysis.knowledgePoints" class="qa-kp">
                <span class="qa-kp-label">知识点：</span>{{ qaAnalysis.knowledgePoints }}
              </div>
            </div>
            <div v-else class="qa-analysis-empty">暂无解析，可在下方编辑后保存</div>
            <!-- 编辑表单 -->
            <div class="qa-edit">
              <el-input v-model="qaEditForm.textAnalysis" type="textarea" :rows="3" placeholder="文字解析" size="small" />
              <el-input v-model="qaEditForm.videoUrl" placeholder="视频URL（可选）" size="small" style="margin-top:6px" />
              <el-input v-model="qaEditForm.knowledgePoints" placeholder="知识点（可选）" size="small" style="margin-top:6px" />
              <el-button type="primary" size="small" style="margin-top:8px" @click="doSaveAnalysis">保存解析</el-button>
            </div>
          </template>
        </div>

        <!-- 题目标签 -->
        <div class="qa-section">
          <div class="qa-section-title">🏷️ 标签</div>
          <div class="qa-tags">
            <el-tag v-for="t in qaQuestionTags" :key="t.id" size="small" closable style="margin-right:4px;margin-bottom:4px"
                    @close="doUntagQuestion(t.id)">
              {{ t.tagName }}
            </el-tag>
            <span v-if="!qaQuestionTags.length" class="qa-empty">暂无标签</span>
          </div>
          <div class="qa-tag-add">
            <el-select v-model="qaTagAddIds" multiple placeholder="选择标签" size="small" style="width:300px">
              <el-option v-for="t in questionTags" :key="t.id" :label="t.tagName" :value="t.id" />
            </el-select>
            <el-button type="primary" size="small" @click="doTagQuestion">打标签</el-button>
          </div>
        </div>
      </div>
      <div v-else v-loading="true" style="min-height:120px"></div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { addFavoriteItem, removeFavoriteItem, checkQuestionFavorite, getFavoriteFolders, createFavoriteFolder } from '@/api/favorite'
import {
  getQuestionTags,
  getQuestionAnalysis,
  saveQuestionAnalysis,
  favoriteQuestion,
  unfavoriteQuestion,
  tagQuestion,
  untagQuestion
} from '@/api/question'

// ==================== 分类相关 ====================
const categories = ref<any[]>([])
const selectedCategoryId = ref<number | null>(null)
const currentCategory = ref<any>(null)
const catLoading = ref(false)
const expandedIds = ref(new Set<number>())

// ==================== 题目相关 ====================
const questions = ref<any[]>([])
const qLoading = ref(false)
const currentIndex = ref(0)
const showReport = ref(false)
const autoNext = ref(true)
const inputAnswer = ref('')

// ==================== 收藏相关 ====================
const defaultFolderId = ref<number | null>(null)
/** 每题收藏状态：questionId → { favorited, itemId, folderId } */
const favStatus = reactive<Record<number, { favorited: boolean; itemId?: number; folderId?: number }>>({})

/** 每题作答状态：questionId → { userAnswer, isCorrect, answered } */
const answerState = reactive<Record<number, { userAnswer: string; isCorrect?: boolean; answered: boolean }>>({})

// ==================== 题目标签（筛选用） ====================
const questionTags = ref<any[]>([])
const filterTagId = ref<number | undefined>(undefined)
/** 题目→标签ID集合（前端过滤用） */
const questionTagMap = reactive<Record<number, number[]>>({})
/** 过滤后的题目（按标签） */
const filteredQuestions = computed(() => {
  if (!filterTagId.value) return questions.value
  return questions.value.filter(q => (questionTagMap[q.id] || []).includes(filterTagId.value!))
})

// ==================== 解析/标签/收藏 弹层 ====================
const qaDialogVisible = ref(false)
const qaDialogQuestion = ref<any>(null)
const qaAnalysisLoading = ref(false)
const qaAnalysis = ref<any>(null)
const qaEditForm = reactive({ textAnalysis: '', videoUrl: '', knowledgePoints: '' })
const qaFavorited = ref(false)
const qaFavNote = ref('')
const qaQuestionTags = ref<any[]>([])
const qaTagAddIds = ref<number[]>([])

// ==================== 标签映射 ====================
const typeLabels: Record<number, string> = { 1: '单选', 2: '多选', 3: '判断', 4: '填空', 5: '简答' }
const typeColor = (t: number) => (['', '', 'warning', 'success', 'info', 'danger'][t] || '')
const diffLabel = (d: number) => (['', '初', '中', '高'][d] || '')

// ==================== 计算属性 ====================
const currentQuestion = computed(() => filteredQuestions.value[currentIndex.value] || null)

const score = computed(() => Object.values(answerState).filter(s => s.isCorrect).length)

const accuracy = computed(() => {
  const total = filteredQuestions.value.length
  if (total === 0) return 0
  return Math.round(score.value / total * 100)
})

const wrongQuestions = computed(() => {
  return filteredQuestions.value.filter(q => answerState[q.id] && !answerState[q.id].isCorrect)
})

// ==================== 工具函数 ====================
function isAnswered(q: any): boolean {
  return answerState[q.id]?.answered === true
}

function optionClass(q: any, opt: any): Record<string, boolean> {
  if (!isAnswered(q)) return {}
  const state = answerState[q.id]
  return {
    'correct': opt.isCorrect === 1,
    'wrong': state?.userAnswer === opt.optionLabel && opt.isCorrect !== 1,
    'disabled-option': true
  }
}

function tfOptionClass(q: any, value: string): Record<string, boolean> {
  if (!isAnswered(q)) return {}
  const isCorrect = (q.answer || '').trim() === value
  const state = answerState[q.id]
  return {
    'correct': isCorrect,
    'wrong': state?.userAnswer === value && !isCorrect,
    'disabled-option': true
  }
}

// ==================== 题目加载 ====================
function toggleExpand(id: number) {
  if (expandedIds.value.has(id)) expandedIds.value.delete(id)
  else expandedIds.value.add(id)
}

async function selectCategory(cat: any) {
  selectedCategoryId.value = cat.id
  currentCategory.value = cat
  // 重置练习状态
  resetPracticeState()
  questions.value = []
  await loadQuestions(cat.id)
}

async function loadOptions(q: any) {
  if (q.questionType === 1 || q.questionType === 2) {
    try {
      const dRes = await request.get(`/question/detail/${q.id}`)
      let options = dRes.data?.options || []

      // question_option 表无记录时，尝试从 question.content 字段解析（Excel 导入的题目）
      if (options.length === 0 && dRes.data?.question?.content) {
        try {
          const raw = JSON.parse(dRes.data.question.content)
          options = (Array.isArray(raw) ? raw : []).map((opt: any) => ({
            optionLabel: opt.label || opt.optionLabel || '',
            optionContent: opt.content || opt.optionContent || '',
            isCorrect: q.answer && (opt.label || '').trim() === q.answer.trim() ? 1 : 0
          }))
        } catch { options = [] }
      }

      q._options = options
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
    await Promise.all(list.map((q: any) => loadOptions(q)))
    questions.value = list
    // 加载题目标签列表（用于筛选下拉）+ 各题标签
    loadQuestionTags()
    list.forEach((q: any) => loadQuestionTagMap(q.id))
  } catch (e: any) {
    questions.value = []
    console.error('[Practice] 加载题目失败:', e?.message || e)
  } finally { qLoading.value = false }
}

/** 加载全局题目标签列表 */
async function loadQuestionTags() {
  try {
    const res: any = await getQuestionTags()
    questionTags.value = res.data || []
  } catch { questionTags.value = [] }
}

/** 加载单题的标签ID集合（前端过滤用） */
async function loadQuestionTagMap(questionId: number) {
  // 复用详情接口拿标签（若无专用接口，则留空）
  try {
    const res: any = await request.get(`/question/${questionId}/tags`)
    const tags = res.data || []
    questionTagMap[questionId] = tags.map((t: any) => t.id)
  } catch { questionTagMap[questionId] = [] }
}

/** 应用标签筛选 */
function applyTagFilter() {
  currentIndex.value = 0
}

// ==================== 解析/标签/收藏 弹层 ====================
async function openQaDialog(q: any) {
  qaDialogVisible.value = true
  qaDialogQuestion.value = q
  qaAnalysis.value = null
  qaAnalysisLoading.value = true
  qaFavNote.value = ''
  qaTagAddIds.value = []
  qaQuestionTags.value = []
  // 并行加载解析、收藏状态、题目标签
  try {
    const res: any = await getQuestionAnalysis(q.id)
    qaAnalysis.value = res.data || null
    if (qaAnalysis.value) {
      qaEditForm.textAnalysis = qaAnalysis.value.textAnalysis || ''
      qaEditForm.videoUrl = qaAnalysis.value.videoUrl || ''
      qaEditForm.knowledgePoints = qaAnalysis.value.knowledgePoints || ''
    } else {
      qaEditForm.textAnalysis = ''
      qaEditForm.videoUrl = ''
      qaEditForm.knowledgePoints = ''
    }
  } catch { qaAnalysis.value = null } finally { qaAnalysisLoading.value = false }
  // 收藏状态（用本页已有 favStatus）
  const st = favStatus[q.id]
  qaFavorited.value = !!st?.favorited
  // 题目标签
  try {
    const tres: any = await request.get(`/question/${q.id}/tags`)
    qaQuestionTags.value = tres.data || []
  } catch { qaQuestionTags.value = [] }
}

function onQaDialogClosed() {
  qaDialogQuestion.value = null
  qaAnalysis.value = null
  qaTagAddIds.value = []
}

async function doFavorite() {
  const q = qaDialogQuestion.value
  if (!q) return
  try {
    await favoriteQuestion({ questionId: q.id, note: qaFavNote.value || undefined })
    ElMessage.success('已收藏')
    qaFavorited.value = true
    // 同步本页星标
    favStatus[q.id] = { favorited: true }
  } catch (e: any) { ElMessage.error(e?.response?.data?.msg || '收藏失败') }
}

async function doUnfavorite() {
  const q = qaDialogQuestion.value
  if (!q) return
  try {
    await unfavoriteQuestion(q.id)
    ElMessage.success('已取消收藏')
    qaFavorited.value = false
    favStatus[q.id] = { favorited: false }
  } catch (e: any) { ElMessage.error(e?.response?.data?.msg || '取消失败') }
}

async function doSaveAnalysis() {
  const q = qaDialogQuestion.value
  if (!q) return
  try {
    await saveQuestionAnalysis(q.id, {
      textAnalysis: qaEditForm.textAnalysis,
      videoUrl: qaEditForm.videoUrl,
      knowledgePoints: qaEditForm.knowledgePoints
    })
    ElMessage.success('解析已保存')
    // 刷新解析
    const res: any = await getQuestionAnalysis(q.id)
    qaAnalysis.value = res.data || null
  } catch (e: any) { ElMessage.error(e?.response?.data?.msg || '保存失败') }
}

async function doTagQuestion() {
  const q = qaDialogQuestion.value
  if (!q || !qaTagAddIds.value.length) {
    ElMessage.warning('请选择标签')
    return
  }
  try {
    await tagQuestion(q.id, qaTagAddIds.value)
    ElMessage.success('标签已添加')
    qaTagAddIds.value = []
    // 刷新题目标签
    const tres: any = await request.get(`/question/${q.id}/tags`)
    qaQuestionTags.value = tres.data || []
    questionTagMap[q.id] = qaQuestionTags.value.map((t: any) => t.id)
  } catch (e: any) { ElMessage.error(e?.response?.data?.msg || '打标签失败') }
}

async function doUntagQuestion(tagId: number) {
  const q = qaDialogQuestion.value
  if (!q) return
  try {
    await untagQuestion(q.id, tagId)
    ElMessage.success('已移除标签')
    const tres: any = await request.get(`/question/${q.id}/tags`)
    qaQuestionTags.value = tres.data || []
    questionTagMap[q.id] = qaQuestionTags.value.map((t: any) => t.id)
  } catch (e: any) { ElMessage.error(e?.response?.data?.msg || '移除失败') }
}

// ==================== 答题交互 ====================
function selectAnswer(q: any, answer: string) {
  if (isAnswered(q)) return // 已答不可改

  answerState[q.id] = {
    userAnswer: answer,
    isCorrect: answer.trim() === (q.answer || '').trim(),
    answered: true
  }

  if (autoNext.value && currentIndex.value < filteredQuestions.value.length - 1) {
    setTimeout(() => goNext(), 800)
  }
}

function submitInputAnswer() {
  const q = currentQuestion.value
  if (!q || isAnswered(q) || !inputAnswer.value.trim()) return

  answerState[q.id] = {
    userAnswer: inputAnswer.value.trim(),
    isCorrect: inputAnswer.value.trim() === (q.answer || '').trim(),
    answered: true
  }

  if (autoNext.value && currentIndex.value < filteredQuestions.value.length - 1) {
    setTimeout(() => goNext(), 800)
  }
}

// ==================== 收藏 ====================
/** 确保存在默认收藏夹，没有则自动创建 */
async function ensureFolder() {
  if (defaultFolderId.value != null) return
  try {
    const res = await getFavoriteFolders()
    const list = res.data || []
    if (list.length > 0) {
      defaultFolderId.value = list[0].id
    } else {
      const r2 = await createFavoriteFolder({ folderName: '默认收藏' })
      defaultFolderId.value = r2.data?.id
    }
  } catch { /* 收藏功能降级：静默失败 */ }
}

/** 加载当前题目的收藏状态 */
async function loadFavStatus(q: any) {
  if (!q?.id) return
  try {
    const res = await checkQuestionFavorite(q.id)
    const data = res.data
    favStatus[q.id] = {
      favorited: data?.isFavorite === true,
      itemId: data?.itemId,
      folderId: data?.folderId
    }
  } catch { favStatus[q.id] = { favorited: false } }
}

/** 切换收藏/取消收藏 */
async function toggleFavorite(q: any) {
  await ensureFolder()
  if (defaultFolderId.value == null) {
    ElMessage.warning('请先创建一个收藏夹')
    return
  }
  const st = favStatus[q.id]
  if (st?.favorited && st.itemId) {
    // 取消收藏
    try {
      await removeFavoriteItem(st.itemId)
      favStatus[q.id] = { favorited: false }
      ElMessage.success('已取消收藏')
    } catch { ElMessage.error('取消收藏失败') }
  } else {
    // 添加收藏
    try {
      const res = await addFavoriteItem({
        folderId: defaultFolderId.value,
        itemType: 1,
        itemId: q.id,
        itemTitle: q.title || '',
        itemUrl: `/question?categoryId=${q.categoryId}&qId=${q.id}`
      })
      favStatus[q.id] = {
        favorited: true,
        itemId: res.data?.id,
        folderId: defaultFolderId.value
      }
      ElMessage.success('收藏成功')
    } catch { ElMessage.error('收藏失败') }
  }
}

// ==================== 导航 ====================
function goPrev() {
  if (currentIndex.value > 0) {
    currentIndex.value--
    inputAnswer.value = ''
    // 如果上一题是填空/简答且已答，回显答案
    const q = filteredQuestions.value[currentIndex.value]
    if (q && answerState[q.id]?.answered) {
      inputAnswer.value = answerState[q.id].userAnswer
    }
  }
}

function goNext() {
  if (currentIndex.value < filteredQuestions.value.length - 1) {
    currentIndex.value++
    inputAnswer.value = ''
    const q = filteredQuestions.value[currentIndex.value]
    if (q && answerState[q.id]?.answered) {
      inputAnswer.value = answerState[q.id].userAnswer
    }
  } else {
    // 已经是最后一题，显示报告
    submitAll()
  }
}

function submitAll() {
  // 检查是否有未答题
  const unanswered = filteredQuestions.value.filter(q => !answerState[q.id]?.answered)
  if (unanswered.length > 0) {
    ElMessage.warning(`还有 ${unanswered.length} 道题未作答，请完成后提交`)
    return
  }
  showReport.value = true
}

function jumpToQuestion(questionId: number) {
  const idx = filteredQuestions.value.findIndex(q => q.id === questionId)
  if (idx !== -1) {
    showReport.value = false
    currentIndex.value = idx
    inputAnswer.value = ''
    const q = filteredQuestions.value[idx]
    if (q && answerState[q.id]?.answered) {
      inputAnswer.value = answerState[q.id].userAnswer
    }
  }
}

// ==================== 重置 ====================
function resetPracticeState() {
  currentIndex.value = 0
  showReport.value = false
  inputAnswer.value = ''
  filterTagId.value = undefined
  Object.keys(answerState).forEach(k => delete answerState[Number(k)])
}

function resetPractice() {
  resetPracticeState()
}

function backToCategory() {
  showReport.value = false
  selectedCategoryId.value = null
  currentCategory.value = null
  questions.value = []
  resetPracticeState()
}

// 监听 currentIndex 变化，同步 inputAnswer，检查收藏状态
watch(currentIndex, () => {
  const q = filteredQuestions.value[currentIndex.value]
  if (q && answerState[q.id]?.answered) {
    inputAnswer.value = answerState[q.id].userAnswer
  } else {
    inputAnswer.value = ''
  }
  // 切换题目时同步收藏状态
  if (q && favStatus[q.id] == null) loadFavStatus(q)
})

// ==================== 初始化 ====================
onMounted(async () => {
  catLoading.value = true
  try {
    const res = await request.get('/question/category/practice-tree')
    categories.value = res.data || []
    if (categories.value.length > 0) {
      const first = categories.value[0]
      if (first.children && first.children.length > 0) {
        expandedIds.value.add(first.id)
        await selectCategory(first.children[0])
      } else {
        await selectCategory(first)
      }
    }
  } catch (e: any) {
    categories.value = []
  } finally { catLoading.value = false }
})
</script>

<style scoped>
.practice-page { padding: 20px 24px; max-width: 1200px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; }

.practice-layout { display: flex; gap: 20px; align-items: flex-start; }
.category-sidebar { width: 240px; flex-shrink: 0; background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.sidebar-title { font-size: 14px; font-weight: 600; color: #303133; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #f0f0f0; }
.tree-parent { cursor: pointer; padding: 8px 0; font-weight: 500; color: #303133; display: flex; align-items: center; gap: 4px; }
.tree-child { cursor: pointer; padding: 7px 8px 7px 20px; font-size: 13px; color: #606266; border-radius: 6px; display: flex; justify-content: space-between; align-items: center; }
.tree-child:hover { background: #f5f7fa; }
.tree-child.active { background: #ecf5ff; color: #409EFF; font-weight: 500; }
.q-count { font-size: 11px; color: #909399; }

/* 右侧练习区域 */
.question-area { flex: 1; background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); min-height: 500px; }

/* 顶部状态栏 */
.card-top-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.current-cat-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}
.progress-wrap {
  flex: 1;
  min-width: 120px;
}
.progress-text {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
  display: inline-block;
}

/* 题目卡片 */
.q-card-wrapper { max-width: 720px; margin: 0 auto; }
.question-card-main { margin-bottom: 16px; border-radius: 12px; }

.q-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
/* 收藏星标 */
.fav-star {
  margin-left: auto;
  font-size: 22px;
  cursor: pointer;
  color: #e6a23c;
  user-select: none;
  transition: transform .15s;
  line-height: 1;
  padding: 2px;
}
.fav-star:hover { transform: scale(1.2); }
.q-title { font-size: 16px; color: #303133; margin-bottom: 16px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }

/* 选项 */
.q-options { display: flex; flex-direction: column; gap: 8px; }
.q-option {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; border-radius: 8px;
  border: 1.5px solid #e8e8e8;
  cursor: pointer; transition: all .2s;
}
.q-option:hover { border-color: #409EFF; background: #f5f9ff; }
.q-option.correct { border-color: #67c23a; background: #f0f9eb; }
.q-option.wrong { border-color: #f56c6c; background: #fef0f0; }
.q-option.disabled-option { cursor: default; pointer-events: none; }
.opt-label {
  width: 28px; height: 28px; border-radius: 50%;
  background: #e8e8e8; display: flex; align-items: center;
  justify-content: center; font-size: 13px; font-weight: 600; flex-shrink: 0;
}
.q-option.correct .opt-label { background: #67c23a; color: #fff; }
.q-option.wrong .opt-label { background: #f56c6c; color: #fff; }
.opt-content { font-size: 14px; color: #606266; flex: 1; }
.opt-icon { font-size: 16px; flex-shrink: 0; }

/* 填空/简答 */
.q-input-area {
  display: flex; gap: 10px; align-items: center;
  margin-top: 4px;
}
.q-input-area .el-input { flex: 1; }
.input-submit-btn { flex-shrink: 0; }

/* 反馈 */
.q-feedback { margin-top: 12px; padding: 10px 14px; border-radius: 8px; font-size: 14px; }
.feedback-ok { background: #f0f9eb; color: #67c23a; }
.feedback-err { background: #fef0f0; color: #f56c6c; }
.q-analysis { margin-top: 6px; font-size: 13px; color: #606266; line-height: 1.5; }

/* 底部导航 */
.card-bottom-bar {
  text-align: center;
  padding-top: 8px;
}
.card-bottom-bar .el-button-group .el-button {
  min-width: 100px;
}

/* 过渡动画 */
.fade-card-enter-active,
.fade-card-leave-active { transition: opacity .25s ease; }
.fade-card-enter-from,
.fade-card-leave-to { opacity: 0; }

.fade-feedback-enter-active { transition: all .3s ease; }
.fade-feedback-leave-active { transition: all .2s ease; }
.fade-feedback-enter-from { opacity: 0; transform: translateY(-8px); }
.fade-feedback-leave-to { opacity: 0; }

/* 报告面板 */
.report-panel {
  max-width: 640px; margin: 0 auto;
}
.report-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px;
}
.report-header h3 { margin: 0; font-size: 20px; font-weight: 600; }

.report-stats {
  display: flex; gap: 16px; margin-bottom: 24px;
}
.stat-item {
  flex: 1; text-align: center; padding: 16px 12px;
  border-radius: 10px; background: #f5f7fa;
}
.stat-ok { background: #f0f9eb; }
.stat-err { background: #fef0f0; }
.stat-num { display: block; font-size: 28px; font-weight: 700; color: #303133; }
.stat-ok .stat-num { color: #67c23a; }
.stat-err .stat-num { color: #f56c6c; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; display: block; }

.all-correct {
  text-align: center; padding: 24px;
  font-size: 18px; color: #67c23a; font-weight: 500;
}

/* 错题回顾 */
.wrong-review { margin-bottom: 20px; }
.wrong-review h4 { margin: 0 0 12px; font-size: 15px; color: #303133; }
.wrong-item {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  padding: 10px 12px; margin-bottom: 6px;
  background: #fef0f0; border-radius: 8px;
  cursor: pointer; transition: background .15s;
}
.wrong-item:hover { background: #fde2e2; }
.wrong-title {
  flex: 1; font-size: 13px; color: #303133;
  min-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.wrong-answer { font-size: 12px; color: #f56c6c; white-space: nowrap; }
.right-answer { font-size: 12px; color: #67c23a; white-space: nowrap; }

.report-actions {
  display: flex; gap: 12px; justify-content: center;
  margin-top: 24px;
}

.empty-hint { text-align: center; padding: 80px 0; color: #909399; font-size: 14px; }

/* 解析/标签/收藏 弹层 */
.qa-q-title { font-size: 15px; font-weight: 600; color: #303133; line-height: 1.6; margin-bottom: 16px; }
.qa-section { margin-bottom: 20px; padding: 12px; background: #f5f7fa; border-radius: 8px; }
.qa-section-title { font-weight: 600; margin-bottom: 10px; color: #303133; }
.qa-fav-row { display: flex; align-items: center; gap: 10px; }
.qa-analysis-text { color: #606266; line-height: 1.7; white-space: pre-wrap; margin-bottom: 8px; }
.qa-video a { color: #409eff; }
.qa-kp { font-size: 13px; color: #909399; margin-top: 4px; }
.qa-kp-label { color: #303133; font-weight: 500; }
.qa-analysis-empty { color: #909399; font-size: 13px; margin-bottom: 8px; }
.qa-edit { margin-top: 8px; border-top: 1px dashed #dcdfe6; padding-top: 8px; }
.qa-tags { margin-bottom: 8px; }
.qa-empty { color: #909399; font-size: 13px; }
.qa-tag-add { display: flex; gap: 8px; align-items: center; }
</style>

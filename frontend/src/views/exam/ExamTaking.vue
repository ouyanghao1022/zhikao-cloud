<template>
  <div class="exam-taking-page">
    <!-- 顶部考试信息栏 -->
    <div class="exam-top-bar">
      <h2 class="exam-name">{{ examInfo.title }}</h2>
      <div class="exam-info-inline">
        <span>满分：{{ examInfo.totalScore || 0 }} 分</span>
        <span>合格线：{{ examInfo.passScore || 0 }} 分</span>
        <span>共 {{ questions.length }} 题</span>
      </div>
      <div class="countdown-box" :class="{ warning: remainingSeconds <= 300 }">
        <span class="countdown-icon">⏱</span>
        <span class="countdown-time">{{ remainingTimeText }}</span>
      </div>
      <div class="cheat-info-box" :class="{ warning: cheatCount >= maxCheatCount - 1 }">
        <span class="cheat-label">切屏：{{ cheatCount }}/{{ maxCheatCount }}</span>
      </div>
    </div>

    <div class="exam-content" v-loading="loading">
      <!-- 左侧题目导航栏 -->
      <div class="question-sidebar">
        <div class="sidebar-title">答题卡</div>
        <div class="sidebar-progress">
          <span>已答：{{ answeredCount }} / {{ questions.length }}</span>
        </div>

        <!-- 按题型分组显示题号 -->
        <div v-for="(qs, typeLabel) in questionGroups" :key="typeLabel" class="question-group">
          <div class="group-label">{{ typeLabel }}</div>
          <div class="group-numbers">
            <div
              v-for="q in qs"
              :key="q.id"
              class="q-number"
              :class="{
                'q-current': q.index === currentIndex,
                'q-answered': answers[q.id],
                'q-current-answered': q.index === currentIndex && answers[q.id]
              }"
              @click="currentIndex = q.index"
            >
              {{ q.index + 1 }}
            </div>
          </div>
        </div>

        <el-button class="submit-sidebar-btn" type="danger" @click="showSummaryDialog = true">
          交卷
        </el-button>
      </div>

      <!-- 中间题目区域 -->
      <div class="question-main" v-if="questions.length > 0">
        <!-- 题目类型标签 -->
        <div class="q-header">
          <el-tag :type="questionTagColor" size="large">{{ questionTypeLabel }}</el-tag>
          <span class="q-index">第 {{ currentIndex + 1 }} 题</span>
          <span class="q-score">（{{ currentQuestion.score || 0 }} 分）</span>
          <div class="q-actions" v-if="currentQuestion.id">
            <el-button
              size="small"
              circle
              :type="favStatus[currentQuestion.id]?.favorited ? 'warning' : 'default'"
              @click="toggleFavorite"
            >{{ favStatus[currentQuestion.id]?.favorited ? '★' : '☆' }}</el-button>
            <el-button size="small" @click="openTagDialog">标签</el-button>
          </div>
        </div>

        <!-- 题目内容 -->
        <div class="q-content" v-html="currentQuestion.title"></div>

        <!-- 单选题选项 -->
        <div v-if="currentQuestion.questionType === 1" class="options-list">
          <div
            v-for="(opt, idx) in options"
            :key="idx"
            class="option-item"
            :class="{ selected: answers[currentQuestion.id] === optionLabels[Number(idx)] }"
            @click="answers[currentQuestion.id] = optionLabels[Number(idx)]"
          >
            <span class="opt-badge">{{ optionLabels[Number(idx)] }}</span>
            <span class="opt-text">{{ opt.content || opt }}</span>
          </div>
        </div>

        <!-- 多选题选项 -->
        <div v-if="currentQuestion.questionType === 2" class="options-list">
          <div
            v-for="(opt, idx) in options"
            :key="idx"
            class="option-item checkbox-option"
            :class="{ selected: isMultiSelected(Number(idx)) }"
            @click="toggleMultiOption(Number(idx))"
          >
            <span class="opt-checkbox" :class="{ checked: isMultiSelected(Number(idx)) }">
              {{ isMultiSelected(Number(idx)) ? '✓' : '' }}
            </span>
            <span class="opt-text">{{ opt.content || opt }}</span>
          </div>
        </div>

        <!-- 判断题选项 -->
        <div v-if="currentQuestion.questionType === 3" class="options-list">
          <div
            class="option-item"
            :class="{ selected: answers[currentQuestion.id] === '正确' }"
            @click="answers[currentQuestion.id] = '正确'"
          >
            <span class="opt-badge">✓</span>
            <span class="opt-text">正确</span>
          </div>
          <div
            class="option-item"
            :class="{ selected: answers[currentQuestion.id] === '错误' }"
            @click="answers[currentQuestion.id] = '错误'"
          >
            <span class="opt-badge">✗</span>
            <span class="opt-text">错误</span>
          </div>
        </div>

        <!-- 填空题 -->
        <div v-if="currentQuestion.questionType === 4" class="fill-input">
          <el-input
            v-model="answers[currentQuestion.id]"
            placeholder="请输入答案"
            size="large"
            clearable
          />
        </div>

        <!-- 简答题 -->
        <div v-if="currentQuestion.questionType === 5" class="essay-input">
          <el-input
            v-model="answers[currentQuestion.id]"
            type="textarea"
            :rows="8"
            placeholder="请输入你的答案..."
            maxlength="5000"
            show-word-limit
          />
        </div>

        <!-- 底部导航按钮 -->
        <div class="q-nav-buttons">
          <el-button
            :disabled="currentIndex === 0"
            @click="currentIndex--"
            size="large"
          >
            上一题
          </el-button>
          <el-button
            v-if="currentIndex < questions.length - 1"
            type="primary"
            @click="currentIndex++"
            size="large"
          >
            下一题
          </el-button>
          <el-button
            v-else
            type="danger"
            @click="showSummaryDialog = true"
            size="large"
          >
            交卷
          </el-button>
        </div>
      </div>
    </div>

    <!-- 考前汇总弹窗 -->
    <el-dialog
      v-model="showSummaryDialog"
      title="答题汇总"
      width="650px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div class="summary-dialog">
        <div class="summary-stats">
          <div class="summary-stat">
            <span class="stat-num">{{ answeredCount }}</span>
            <span class="stat-name">已作答</span>
          </div>
          <div class="summary-stat">
            <span class="stat-num warn">{{ unansweredCount }}</span>
            <span class="stat-name">未作答</span>
          </div>
        </div>

        <div class="summary-questions">
          <div
            v-for="q in questions"
            :key="q.id"
            class="summary-q-item"
            :class="{ 'summary-unanswered': !answers[q.id] }"
            @click="navigateFromSummary(q.index)"
          >
            <span class="summary-q-num">{{ q.index + 1 }}</span>
            <span class="summary-q-type">{{ typeShortLabel(q.questionType) }}</span>
            <span class="summary-q-status">
              {{ answers[q.id] ? answers[q.id] : '未作答' }}
            </span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showSummaryDialog = false" size="large">返回检查</el-button>
        <el-button type="danger" @click="submitExam" :loading="submitting" size="large">
          确认交卷
        </el-button>
      </template>
    </el-dialog>

    <!-- 交卷成功弹窗 -->
    <el-dialog v-model="showResultDialog" title="考试完成" width="450px" :close-on-click-modal="false">
      <div class="result-content" v-if="resultData">
        <div class="result-score">{{ resultData.score || 0 }} 分</div>
        <div class="result-pass" v-if="resultData.passed !== undefined">
          <el-tag :type="resultData.passed ? 'success' : 'danger'" size="large">
            {{ resultData.passed ? '🎉 合格' : '😞 未合格' }}
          </el-tag>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="$router.push('/exam')" size="large">返回考试列表</el-button>
        <el-button @click="$router.push('/wrongbook')" size="large">查看错题本解析</el-button>
        <el-button @click="$router.push('/report')" size="large">查看学情报告</el-button>
      </template>
    </el-dialog>

    <!-- 题目标签弹窗 -->
    <el-dialog v-model="showTagDialog" title="给本题打标签" width="420px">
      <div v-loading="tagLoading">
        <div v-if="tagAllOptions.length === 0 && !tagLoading" class="empty-tip">暂无标签可选</div>
        <el-checkbox-group v-model="tagSelectedIds">
          <el-checkbox
            v-for="t in tagAllOptions"
            :key="t.id"
            :value="t.id"
            class="tag-checkbox"
          >{{ t.tagName }}</el-checkbox>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="showTagDialog = false">取消</el-button>
        <el-button type="primary" :loading="tagApplying" @click="applyTags">应用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { takeExam, submitExam as submitApi } from '@/api/exam'
import { getQuestionTags, getTagsOfQuestion, tagQuestion } from '@/api/question'
import { checkQuestionFavorite, addFavoriteItem, removeFavoriteItem, getFavoriteFolders, createFavoriteFolder } from '@/api/favorite'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const examInfo = ref<any>({ title: '', duration: 60, totalScore: 0, passScore: 0 })
const questions = ref<any[]>([])
const answers = ref<Record<string, string>>({})
const currentIndex = ref(0)
const remainingSeconds = ref(0)
const showSummaryDialog = ref(false)
const showResultDialog = ref(false)
const resultData = ref<any>(null)

// 收藏与标签
const favStatus = ref<Record<number, { favorited: boolean; itemId?: number; folderId?: number }>>({})
const defaultFolderId = ref<number | null>(null)
const showTagDialog = ref(false)
const tagAllOptions = ref<any[]>([])
const tagSelectedIds = ref<number[]>([])
const tagLoading = ref(false)
const tagApplying = ref(false)

let timer: any = null
const cheatCount = ref(0)
const maxCheatCount = ref(3)

const currentQuestion = computed(() => questions.value[currentIndex.value] || {})
const answeredCount = computed(() => questions.value.filter(q => answers.value[q.id]).length)
const unansweredCount = computed(() => questions.value.filter(q => !answers.value[q.id]).length)

const remainingTimeText = computed(() => {
  const m = Math.floor(remainingSeconds.value / 60)
  const s = remainingSeconds.value % 60
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
})

const typeLabels: Record<number, string> = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '填空题', 5: '简答题', 6: '论述题', 7: '编程题' }
const questionTypeLabel = computed(() => typeLabels[currentQuestion.value.questionType] || '未知')
const questionTagColor = computed(() => {
  const colors: Record<number, string> = { 1: '', 2: 'success', 3: 'warning', 4: 'danger', 5: 'info' }
  return colors[currentQuestion.value.questionType] || ''
})

// ==================== 收藏与标签 ====================
async function ensureFolder() {
  if (defaultFolderId.value != null) return
  try {
    const res: any = await getFavoriteFolders()
    const list = res.data || []
    if (list.length > 0) defaultFolderId.value = list[0].id
    else {
      const r2: any = await createFavoriteFolder({ folderName: '默认收藏' })
      defaultFolderId.value = r2.data?.id ?? null
    }
  } catch { /* 收藏降级 */ }
}
async function loadFavStatus(qid: number) {
  if (!qid) return
  try {
    const res: any = await checkQuestionFavorite(qid)
    const d = res.data
    favStatus.value[qid] = { favorited: d?.isFavorite === true, itemId: d?.itemId, folderId: d?.folderId }
  } catch { favStatus.value[qid] = { favorited: false } }
}
async function toggleFavorite() {
  const q = currentQuestion.value
  if (!q?.id) return
  await ensureFolder()
  if (defaultFolderId.value == null) { ElMessage.warning('请先创建一个收藏夹'); return }
  const st = favStatus.value[q.id]
  if (st?.favorited && st.itemId) {
    try {
      await removeFavoriteItem(st.itemId)
      favStatus.value[q.id] = { favorited: false }
      ElMessage.success('已取消收藏')
    } catch { ElMessage.error('取消收藏失败') }
  } else {
    try {
      const res: any = await addFavoriteItem({
        folderId: defaultFolderId.value,
        itemType: 1,
        itemId: q.id,
        itemTitle: q.title || '',
        itemUrl: `/question?qId=${q.id}`
      })
      favStatus.value[q.id] = { favorited: true, itemId: res.data?.id, folderId: defaultFolderId.value }
      ElMessage.success('收藏成功')
    } catch { ElMessage.error('收藏失败') }
  }
}
async function openTagDialog() {
  const q = currentQuestion.value
  if (!q?.id) return
  showTagDialog.value = true
  tagLoading.value = true
  try {
    const [allRes, mineRes]: any = await Promise.all([getQuestionTags(), getTagsOfQuestion(q.id)])
    tagAllOptions.value = allRes.data || []
    tagSelectedIds.value = (mineRes.data || []).map((t: any) => t.id)
  } catch { tagAllOptions.value = []; tagSelectedIds.value = [] } finally { tagLoading.value = false }
}
async function applyTags() {
  const q = currentQuestion.value
  if (!q?.id) return
  tagApplying.value = true
  try {
    await tagQuestion(q.id, tagSelectedIds.value)
    ElMessage.success('标签已更新')
    showTagDialog.value = false
  } catch { ElMessage.error('标签更新失败') } finally { tagApplying.value = false }
}

watch(() => currentQuestion.value?.id, (qid) => {
  if (qid) loadFavStatus(qid)
}, { immediate: true })

const optionLabels = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']

const options = computed(() => {
  const q = currentQuestion.value
  if (!q) return []
  // 后端返回 options 数组（QuestionOption: {optionLabel, optionContent}）
  // Excel导入的题目选项存在 content JSON、question_option 表无记录 → 后端返回 options:[]，需走下方 content 兜底
  if (Array.isArray(q.options) && q.options.length > 0) {
    return q.options.map((o: any) => ({
      label: o.optionLabel || o.label || '',
      content: o.optionContent || o.content || ''
    }))
  }
  // 兼容 optionA/optionB 旧格式
  if (q.optionA !== undefined) {
    const opts: any[] = []
    const letters = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']
    for (const l of letters) {
      const val = q['option' + l]
      if (val !== undefined && val !== null && val !== '') {
        opts.push({ label: l, content: val })
      }
    }
    return opts
  }
  // options 是 JSON 字符串
  if (typeof q.options === 'string') {
    try { return JSON.parse(q.options) } catch { return [{ label: '', content: q.options }] }
  }
  // content 字段可能存了 options JSON
  if (typeof q.content === 'string' && q.content.trim().startsWith('[')) {
    try { return JSON.parse(q.content) } catch { return [] }
  }
  return []
})

// 按题型分组
const questionGroups = computed(() => {
  const groups: Record<string, any[]> = {}
  questions.value.forEach((q, i) => {
    q.index = i
    const label = typeLabels[q.questionType] || `题型${q.questionType}`
    if (!groups[label]) groups[label] = []
    groups[label].push(q)
  })
  return groups
})

function typeShortLabel(type: number) {
  const labels: Record<number, string> = { 1: '单选', 2: '多选', 3: '判断', 4: '填空', 5: '简答', 6: '论述', 7: '编程' }
  return labels[type] || '未知'
}

function isMultiSelected(idx: number) {
  const ans = answers.value[currentQuestion.value.id] || ''
  return ans.includes(optionLabels[idx])
}

function toggleMultiOption(idx: number) {
  const key = String(currentQuestion.value.id)
  const label = optionLabels[idx]
  const current = answers.value[key] || ''
  if (current.includes(label)) {
    answers.value[key] = current.replace(label, '')
  } else {
    answers.value[key] = (current + label).split('').sort().join('')
  }
}

function navigateFromSummary(idx: number) {
  showSummaryDialog.value = false
  currentIndex.value = idx
}

async function submitExam() {
  submitting.value = true
  try {
    const answerList = questions.value
      .filter(q => answers.value[q.id])
      .map(q => ({
        questionId: q.id,
        answer: String(answers.value[q.id])
      }))
    const res = await submitApi({
      paperId: Number(route.params.id),
      answers: answerList
    })
    // 清除考试开始时间记录
    localStorage.removeItem(`exam_start_${Number(route.params.id)}`)
    if (timer) clearInterval(timer)
    showSummaryDialog.value = false
    // 提交后允许退出全屏
    fullscreenLocked = true
    try { 
      if (document.exitFullscreen) document.exitFullscreen()
      else if ((document as any).webkitExitFullscreen) (document as any).webkitExitFullscreen()
    } catch {}
    if (res.data) {
      resultData.value = res.data
      showResultDialog.value = true
    } else {
      ElMessage.success(res.message || '交卷成功')
      router.push('/exam')
    }
  } catch {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

function startTimer() {
  timer = setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value--
    } else {
      clearInterval(timer)
      ElMessage.warning('考试时间到，系统将自动交卷')
      submitExam()
    }
  }, 1000)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await takeExam(Number(route.params.id))
    if (res.code === 200 && res.data) {
      const paperId = Number(route.params.id)
      examInfo.value = res.data.paper || res.data.examInfo || res.data
      const qs = res.data.questions || res.data.questionList || res.data.questionsList || []
      questions.value = qs.map((q: any, i: number) => ({ ...q, index: i }))

      // 用 localStorage 记录考试开始时间，刷新后恢复正确剩余时间
      const storageKey = `exam_start_${paperId}`
      let startTime = Number(localStorage.getItem(storageKey))
      if (!startTime) {
        startTime = Date.now()
        localStorage.setItem(storageKey, String(startTime))
      }
      const totalSeconds = (examInfo.value.duration || 60) * 60
      const elapsed = Math.floor((Date.now() - startTime) / 1000)
      remainingSeconds.value = Math.max(0, totalSeconds - elapsed)
      // 读取允许切屏次数
      if (examInfo.value.maxScreenSwitch !== undefined && examInfo.value.maxScreenSwitch !== null) {
        maxCheatCount.value = Number(examInfo.value.maxScreenSwitch)
      }
      startTimer()
      // 进入全屏考试模式
      tryFullScreen()
      // 防作弊：检测页面切换 + 应用切换
      document.addEventListener('visibilitychange', handleVisibilityChange)
      window.addEventListener('blur', handleWindowBlur)
      // 锁定全屏：禁止退出
      document.addEventListener('fullscreenchange', handleFullscreenChange)
      document.addEventListener('webkitfullscreenchange', handleFullscreenChange)
      // 拦截键盘：禁止 Esc/F11/Ctrl+W 等
      document.addEventListener('keydown', handleKeyDown, true)
      document.addEventListener('contextmenu', handleContextMenu)
    }
  } catch {
    ElMessage.error('加载试卷失败')
    router.push('/exam')
  } finally {
    loading.value = false
  }
})

function tryFullScreen() {
  try {
    const el = document.documentElement
    if (el.requestFullscreen) {
      el.requestFullscreen()
    }
  } catch (e) {
    // 某些浏览器可能不支持全屏
  }
}

function handleVisibilityChange() {
  if (document.hidden) {
    cheatCount.value++
    ElMessage.warning(`切屏警告 (${cheatCount.value}/${maxCheatCount.value})，超过 ${maxCheatCount.value} 次将自动交卷`)
    if (cheatCount.value >= maxCheatCount.value) {
      ElMessage.error('切屏次数过多，系统自动交卷')
      if (timer) clearInterval(timer)
      submitExam()
    }
  }
}

/** 检测应用切换（Alt+Tab/Windows键等导致窗口失焦） */
function handleWindowBlur() {
  // 窗口失焦也算切屏
  cheatCount.value++
  ElMessage.warning(`切屏警告 (${cheatCount.value}/${maxCheatCount.value})，请勿切换应用`)
  if (cheatCount.value >= maxCheatCount.value) {
    ElMessage.error('切屏次数过多，系统自动交卷')
    if (timer) clearInterval(timer)
    submitExam()
  }
}

/** 锁定全屏：提交试卷前不允许退出全屏 */
let fullscreenLocked = false
let fullscreenRecovering = false

function handleFullscreenChange() {
  const isFullscreen = !!(document.fullscreenElement || (document as any).webkitFullscreenElement)
  if (isFullscreen || fullscreenLocked) return

  // 退出全屏 → 算一次切屏
  cheatCount.value++
  ElMessage.warning(`禁止退出全屏！切屏警告 (${cheatCount.value}/${maxCheatCount.value})`)

  if (cheatCount.value >= maxCheatCount.value) {
    ElMessage.error('切屏次数过多，系统自动交卷')
    if (timer) clearInterval(timer)
    submitExam()
    return
  }

  // 尝试重新进入全屏（需用户手势，可能失败）
  if (!fullscreenRecovering) {
    fullscreenRecovering = true
    tryFullScreen()
    setTimeout(() => { fullscreenRecovering = false }, 500)
  }
}

/** 拦截键盘事件：禁止 Esc/F11 退出全屏，禁止 Alt+Tab 等组合键 */
function handleKeyDown(e: KeyboardEvent) {
  if (fullscreenLocked) return

  // 拦截 Esc — 浏览器无法完全阻止，但至少阻止其他逻辑
  if (e.key === 'Escape') {
    e.preventDefault()
    e.stopPropagation()
    ElMessage.warning('考试期间禁止退出全屏，请先交卷')
    return false
  }

  // 拦截 F11（全屏切换）
  if (e.key === 'F11') {
    e.preventDefault()
    e.stopPropagation()
    return false
  }

  // 拦截 Alt+Tab（浏览器无法完全阻止，但记录）
  if (e.altKey && e.key === 'Tab') {
    e.preventDefault()
    return false
  }

  // 拦截 Ctrl+W（关闭标签页）
  if (e.ctrlKey && e.key === 'w') {
    e.preventDefault()
    e.stopPropagation()
    ElMessage.warning('考试期间禁止关闭页面')
    return false
  }

  // 拦截 Ctrl+T（新标签页）/ Ctrl+N（新窗口）
  if (e.ctrlKey && (e.key === 't' || e.key === 'n')) {
    e.preventDefault()
    return false
  }
}

/** 禁止右键菜单 */
function handleContextMenu(e: Event) {
  if (!fullscreenLocked) {
    e.preventDefault()
    return false
  }
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('blur', handleWindowBlur)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener('webkitfullscreenchange', handleFullscreenChange)
  document.removeEventListener('keydown', handleKeyDown, true)
  document.removeEventListener('contextmenu', handleContextMenu)
  // 退出全屏
  fullscreenLocked = true
  try {
    if (document.exitFullscreen) document.exitFullscreen()
    else if ((document as any).webkitExitFullscreen) (document as any).webkitExitFullscreen()
  } catch {}
})
</script>

<style scoped>
.exam-taking-page {
  min-height: 100vh; background: var(--color-rice); display: flex; flex-direction: column;
}

/* 顶部栏 */
.exam-top-bar {
  background: var(--color-rice-card); padding: 14px 24px; border-bottom: 1px solid var(--color-rice-border);
  display: flex; align-items: center; gap: 20px; position: sticky; top: 0; z-index: 100;
}
.exam-name { font-size: 17px; color: var(--color-ink); margin: 0; flex-shrink: 0; max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.exam-info-inline { display: flex; gap: 16px; font-size: 13px; color: var(--color-ink-muted); flex: 1; }
.countdown-box {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 18px; background: #f0f9eb; border-radius: var(--radius-lg); flex-shrink: 0;
  border: 1.5px solid #b3e19d;
}
.countdown-box.warning {
  background: #fef0f0; border-color: #fab6b6;
}
.countdown-icon { font-size: 18px; }
.countdown-time { font-size: 20px; font-weight: 700; color: var(--color-success); font-family: 'Courier New', monospace; letter-spacing: 2px; }
.countdown-box.warning .countdown-time { color: var(--color-danger); }

/* 切屏计数显示 */
.cheat-info-box {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 18px; background: #fef0f0; border-radius: var(--radius-lg); flex-shrink: 0;
  border: 1.5px solid #fab6b6;
}
.cheat-info-box.warning { background: #fef0f0; border-color: var(--color-danger); }
.cheat-label { font-size: 13px; font-weight: 600; color: var(--color-danger); }

/* 内容区域 */
.exam-content {
  display: flex; flex: 1; overflow: hidden;
}

/* 左侧答题卡 */
.question-sidebar {
  width: 260px; flex-shrink: 0; background: var(--color-rice-card); border-right: 1px solid var(--color-rice-border);
  padding: 16px; overflow-y: auto;
}
.sidebar-title { font-size: 16px; font-weight: 600; color: var(--color-ink); margin-bottom: 8px; }
.sidebar-progress { font-size: 12px; color: var(--color-ink-muted); margin-bottom: 16px; }

.question-group { margin-bottom: 16px; }
.group-label {
  font-size: 13px; color: var(--color-ink-light); font-weight: 600; margin-bottom: 8px;
  padding-bottom: 4px; border-bottom: 1px solid var(--color-rice-border);
}
.group-numbers { display: flex; flex-wrap: wrap; gap: 6px; }

.q-number {
  width: 34px; height: 34px; display: flex; align-items: center; justify-content: center;
  border: 1.5px solid var(--color-rice-border); border-radius: var(--radius-sm); font-size: 13px; color: var(--color-ink-light);
  cursor: pointer; transition: all 0.2s; font-weight: 500; user-select: none;
}
.q-number:hover { border-color: var(--color-primary); color: var(--color-primary); }
.q-number.q-current { border-color: var(--color-primary); background: rgba(5,150,105,0.06); color: var(--color-primary); font-weight: 700; }
.q-number.q-answered { background: #e1f3d8; border-color: #b3e19d; color: var(--color-success); }
.q-number.q-current-answered { background: var(--color-primary); border-color: var(--color-primary); color: #fff; font-weight: 700; }

.submit-sidebar-btn { margin-top: 20px; width: 100%; }

/* 中间题目区域 */
.question-main {
  flex: 1; padding: 28px 32px; overflow-y: auto; background: var(--color-rice-card);
  margin: 8px; border-radius: var(--radius-md); box-shadow: var(--shadow-sm);
}

.q-header { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.q-actions { margin-left: auto; display: flex; gap: 8px; }
.empty-tip { color: var(--color-ink-muted); text-align: center; padding: 16px 0; }
.tag-checkbox { display: flex; margin-bottom: 8px; }
.q-index { font-size: 15px; color: var(--color-ink-light); font-weight: 600; }
.q-score { font-size: 14px; color: var(--color-ink-muted); }

.q-content {
  font-size: 16px; line-height: 1.8; color: var(--color-ink); margin-bottom: 28px;
  word-break: break-word;
}
.q-content :deep(p) { margin: 0 0 8px; }
.q-content :deep(img) { max-width: 100%; border-radius: var(--radius-sm); }

/* 选项列表 */
.options-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 24px; }
.option-item {
  display: flex; align-items: center; gap: 14px; padding: 14px 18px;
  border: 2px solid var(--color-rice-border); border-radius: var(--radius-md); cursor: pointer;
  transition: all 0.2s; min-height: 48px;
}
.option-item:hover { border-color: #c6d0e0; background: var(--color-rice-card); }
.option-item.selected { border-color: var(--color-primary); background: rgba(5,150,105,0.06); }
.opt-badge {
  width: 32px; height: 32px; border-radius: 50%; background: var(--color-rice-border);
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 14px; color: var(--color-ink-light); flex-shrink: 0;
}
.option-item.selected .opt-badge { background: var(--color-primary); color: #fff; }

/* 多选题复选框样式 */
.checkbox-option { }
.opt-checkbox {
  width: 22px; height: 22px; border: 2px solid var(--color-rice-border); border-radius: var(--radius-sm);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  font-size: 14px; color: #fff; transition: all 0.2s;
}
.opt-checkbox.checked { background: var(--color-primary); border-color: var(--color-primary); }

.opt-text { font-size: 15px; color: var(--color-ink); flex: 1; }

/* 填空/简答输入 */
.fill-input, .essay-input { margin-bottom: 24px; max-width: 600px; }

/* 底部导航 */
.q-nav-buttons {
  display: flex; justify-content: space-between; align-items: center;
  padding-top: 20px; border-top: 1px solid var(--color-rice-border); margin-top: auto;
}

/* 汇总弹窗 */
.summary-dialog { }
.summary-stats { display: flex; justify-content: center; gap: 48px; margin-bottom: 20px; }
.summary-stat { text-align: center; }
.stat-num { font-size: 36px; font-weight: 700; color: var(--color-success); }
.stat-num.warn { color: var(--color-warning); }
.stat-name { font-size: 14px; color: var(--color-ink-muted); display: block; }
.summary-questions {
  display: flex; flex-wrap: wrap; gap: 8px; max-height: 300px; overflow-y: auto; padding: 8px 0;
}
.summary-q-item {
  display: flex; align-items: center; gap: 8px; padding: 6px 12px;
  background: #f0f9eb; border-radius: var(--radius-sm); cursor: pointer; font-size: 13px;
  transition: all 0.2s;
}
.summary-q-item:hover { background: #e1f3d8; }
.summary-q-item.summary-unanswered { background: #fef0f0; }
.summary-q-num { font-weight: 600; color: var(--color-ink); min-width: 24px; text-align: center; }
.summary-q-type { color: var(--color-ink-muted); font-size: 12px; }
.summary-q-status { color: var(--color-ink-light); max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* 结果弹窗 */
.result-content { text-align: center; padding: 20px 0; }
.result-score { font-size: 56px; font-weight: 700; color: var(--color-primary); }
</style>

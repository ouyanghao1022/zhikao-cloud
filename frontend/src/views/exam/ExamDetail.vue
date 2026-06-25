<template>
  <div class="exam-detail-page" v-loading="loading">
    <div v-if="exam" class="exam-detail-card">
      <!-- 考试头部 -->
      <div class="exam-header">
        <el-tag :type="statusTagType(exam.status)" size="large">{{ statusText(exam.status) }}</el-tag>
        <h1 class="exam-title">{{ exam.title }}</h1>
        <p class="exam-subtitle" v-if="exam.description">{{ exam.description }}</p>
      </div>

      <!-- 考试信息卡片 -->
      <div class="exam-info-section">
        <h3 class="section-title">📋 考试信息</h3>
        <div class="info-grid">
          <div class="info-card">
            <div class="info-icon">⏱</div>
            <div class="info-body">
              <div class="info-card-label">考试时长</div>
              <div class="info-card-value">{{ exam.duration || 0 }} 分钟</div>
            </div>
          </div>
          <div class="info-card">
            <div class="info-icon">📊</div>
            <div class="info-body">
              <div class="info-card-label">试卷满分</div>
              <div class="info-card-value">{{ exam.totalScore || 0 }} 分</div>
            </div>
          </div>
          <div class="info-card">
            <div class="info-icon">✅</div>
            <div class="info-body">
              <div class="info-card-label">合格分数</div>
              <div class="info-card-value">{{ exam.passScore || exam.totalScore * 0.6 || 60 }} 分</div>
            </div>
          </div>
          <div class="info-card">
            <div class="info-icon">📝</div>
            <div class="info-body">
              <div class="info-card-label">题目总数</div>
              <div class="info-card-value">{{ questionTypeSummary?.total || 0 }} 题</div>
            </div>
          </div>
          <div class="info-card">
            <div class="info-icon">👤</div>
            <div class="info-body">
              <div class="info-card-label">创建人</div>
              <div class="info-card-value">{{ exam.creatorName || exam.paper?.creatorName || '—' }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 题型分布 -->
      <div class="question-type-section" v-if="questionTypeSummary">
        <h3 class="section-title">📑 题型分布</h3>
        <div class="type-grid">
          <div class="type-card" v-for="qt in questionTypeSummary.types" :key="qt.type">
            <div class="type-label">{{ qt.label }}</div>
            <div class="type-count">{{ qt.count }} 题</div>
            <div class="type-score" v-if="qt.scorePerQuestion">每题 {{ qt.scorePerQuestion }} 分</div>
          </div>
        </div>
      </div>

      <!-- 考试时间 -->
      <div class="exam-time-section" v-if="exam.startTime">
        <h3 class="section-title">📅 考试时间</h3>
        <div class="time-row">
          <div class="time-item">
            <span class="time-label">开始时间：</span>
            <span class="time-value">{{ formatTime(exam.startTime) }}</span>
          </div>
          <div v-if="exam.endTime" class="time-item">
            <span class="time-label">结束时间：</span>
            <span class="time-value">{{ formatTime(exam.endTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="exam-actions">
        <el-button size="large" @click="$router.push('/exam')">返回列表</el-button>
        <el-button
          v-if="canStart"
          type="primary"
          size="large"
          @click="startExam"
          :loading="starting"
        >
          开始考试
        </el-button>
        <el-tag v-else-if="hasTaken" type="success" size="large">已参加</el-tag>
        <el-tag v-else-if="exam.status === 2" type="warning" size="large">考试已结束</el-tag>
        <el-tag v-else-if="exam.status === 0" type="info" size="large">考试未发布</el-tag>
      </div>
    </div>

    <div v-else-if="!loading" class="error-tip">
      <el-empty description="考试不存在或已删除" />
      <el-button type="primary" @click="$router.push('/exam')">返回考试中心</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getExamDetail, getMyRecords } from '@/api/exam'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const exam = ref<any>(null)
const loading = ref(false)
const starting = ref(false)
const hasTaken = ref(false)

const typeLabels: Record<number, string> = {
  1: '单选题', 2: '多选题', 3: '填空题', 4: '简答题', 5: '判断题'
}

const canStart = computed(() => {
  if (!exam.value) return false
  return exam.value.status === 1 && !hasTaken.value
})

const questionTypeSummary = computed(() => {
  if (!exam.value) return null
  const questions = exam.value.questions || exam.value.questionList || []
  const typeMap: Record<number, { count: number; scorePerQuestion: number }> = {}
  let total = 0

  questions.forEach((q: any) => {
    const t = q.type || 1
    if (!typeMap[t]) {
      typeMap[t] = { count: 0, scorePerQuestion: q.score || 0 }
    }
    typeMap[t].count++
    total++
  })

  const types = Object.entries(typeMap).map(([type, info]) => ({
    type: Number(type),
    label: typeLabels[Number(type)] || `题型${type}`,
    count: info.count,
    scorePerQuestion: info.scorePerQuestion
  }))

  return { total, types }
})

function statusTagType(status: number) {
  return ['info', 'success', 'warning', ''][status] || 'info'
}
function statusText(status: number) {
  return ['草稿', '已发布', '已结束', '已归档'][status] || '未知'
}
function formatTime(time: string) {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '--'
}

async function startExam() {
  try {
    await ElMessageBox.confirm(
      `考试时长 ${exam.value.duration} 分钟，进入考试后将开始计时，确认开始？`,
      '开始考试',
      { type: 'info', confirmButtonText: '确认开始', cancelButtonText: '再等等' }
    )
    starting.value = true
    router.push(`/exam/take/${route.params.id}`)
  } catch { /* canceled */ }
}

onMounted(async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res = await getExamDetail(id)
    if (res.code === 200 && res.data) {
      exam.value = res.data
    }
    // 检查是否已参加过此考试
    await checkIfTaken()
  } catch {
    ElMessage.error('加载考试信息失败')
  } finally {
    loading.value = false
  }
})

async function checkIfTaken() {
  try {
    const res = await getMyRecords({ current: 1, size: 100 })
    if (res.data?.records) {
      hasTaken.value = res.data.records.some((r: any) => {
        const recordExamId = r.examId || r.paperId || r.pid
        const currentExamId = exam.value?.paperId || exam.value?.examId || Number(route.params.id)
        return recordExamId === currentExamId
      })
    }
  } catch {
    hasTaken.value = false
  }
}
</script>

<style scoped>
.exam-detail-page { padding: 24px; max-width: 900px; margin: 0 auto; }
.exam-detail-card {
  background: #fff; border-radius: 16px; padding: 36px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.06);
}
.exam-header {
  text-align: center; margin-bottom: 32px; padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}
.exam-header .el-tag { margin-bottom: 16px; }
.exam-title { font-size: 26px; color: #303133; margin: 0 0 12px; }
.exam-subtitle { font-size: 14px; color: #909399; margin: 0; line-height: 1.6; }

.section-title { font-size: 17px; color: #303133; margin: 0 0 16px; padding-left: 0; }

/* 信息卡片 */
.exam-info-section { margin-bottom: 28px; }
.info-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; }
.info-card {
  display: flex; align-items: center; gap: 12px;
  padding: 16px; background: #f5f7fa; border-radius: 10px;
  transition: all 0.3s;
}
.info-card:hover { background: #ecf5ff; transform: translateY(-2px); }
.info-icon { font-size: 28px; flex-shrink: 0; }
.info-body { }
.info-card-label { font-size: 12px; color: #909399; margin-bottom: 4px; }
.info-card-value { font-size: 18px; font-weight: 700; color: #303133; }

/* 题型分布 */
.question-type-section { margin-bottom: 28px; }
.type-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px; }
.type-card {
  padding: 16px; background: #fafafa; border-radius: 10px;
  border: 1px solid #f0f0f0; text-align: center;
}
.type-label { font-size: 14px; color: #606266; margin-bottom: 6px; }
.type-count { font-size: 24px; font-weight: 700; color: #409EFF; }
.type-score { font-size: 12px; color: #909399; margin-top: 4px; }

/* 考试时间 */
.exam-time-section { margin-bottom: 32px; }
.time-row { display: flex; gap: 32px; }
.time-item { font-size: 14px; }
.time-label { color: #909399; }
.time-value { color: #303133; font-weight: 500; }

/* 操作按钮 */
.exam-actions { display: flex; justify-content: space-between; align-items: center; padding-top: 24px; border-top: 1px solid #f0f0f0; }

.error-tip { text-align: center; padding: 80px 0; }
</style>

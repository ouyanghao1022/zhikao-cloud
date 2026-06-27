<template>
  <div class="exam-list-page">
    <div class="page-header">
      <h2>考试中心</h2>
      <el-button v-if="userStore.isTeacher" type="primary" @click="$router.push('/admin/exams')">管理考试</el-button>
    </div>

    <!-- 搜索筛选 -->
    <div class="filter-bar">
      <el-input v-model="keyword" placeholder="搜索考试名称..." clearable style="width:300px"
                @keyup.enter="loadData" :prefix-icon="Search">
      </el-input>
      <el-select v-model="statusFilter" placeholder="考试状态" clearable style="width:150px" @change="loadData">
        <el-option label="草稿" :value="0" />
        <el-option label="已发布" :value="1" />
        <el-option label="已结束" :value="2" />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
    </div>

    <!-- 考试卡片列表 -->
    <div class="exam-grid" v-loading="loading">
      <div v-for="exam in examList" :key="exam.id" class="exam-card" :class="{ taken: takenExamIds.has(exam.id) }" @click="goToExam(exam)">
        <div class="exam-card-header">
          <div class="header-tags">
            <el-tag :type="statusTagType(exam.status)">{{ statusText(exam.status) }}</el-tag>
            <el-tag v-if="takenExamIds.has(exam.id)" type="success" size="small">已参加</el-tag>
          </div>
          <span class="exam-duration">{{ exam.duration }}分钟</span>
        </div>
        <h3 class="exam-title">{{ exam.title }}</h3>
        <p class="exam-desc">{{ exam.description || '暂无描述' }}</p>
        <div class="exam-meta">
          <span>总分：{{ exam.totalScore }}分</span>
          <span>及格分：{{ exam.passScore || '无' }}</span>
          <span>已参加：{{ exam.enrolledCount || 0 }}人</span>
        </div>
        <div class="exam-footer">
          <span class="exam-time">{{ formatTime(exam.startTime) }}</span>
          <template v-if="takenRecords.has(exam.id)">
            <el-button v-if="takenRecords.get(exam.id)?.graded" type="success" size="small" @click.stop="viewScore(exam)">
              查看成绩
            </el-button>
            <el-tag v-else type="warning" size="small" @click.stop>阅卷中</el-tag>
          </template>
          <el-button v-else type="primary" size="small" @click.stop="goToExam(exam)">
            {{ exam.status === 1 ? '立即参加' : '查看详情' }}
          </el-button>
        </div>
      </div>
      <div v-if="examList.length === 0 && !loading" class="empty-tip">
        暂无考试，{{ userStore.isTeacher ? '点击右上角管理考试去创建' : '请等待老师发布考试' }}
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next, jumper"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getExamList, getMyRecords } from '@/api/exam'
import { getMyClasses } from '@/api/class'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()

const keyword = ref('')
const statusFilter = ref<number | undefined>(1)  // 默认只看已发布
const loading = ref(false)
const examList = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const takenExamIds = ref(new Set<number>())
const takenRecords = ref(new Map<number, { graded: boolean; score?: number }>())
const myClassId = ref<number|undefined>(undefined)

function statusTagType(status: number) {
  return ['info', 'success', 'warning', ''][status] || 'info'
}
function statusText(status: number) {
  return ['草稿', '已发布', '已结束', '已归档'][status] || '未知'
}
function formatTime(time: string) {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '--'
}
function goToExam(exam: any) {
  if (takenExamIds.value.has(exam.id)) {
    ElMessage.warning('你已参加过该考试')
    return
  }
  if (exam.status === 1) {
    router.push(`/exam/take/${exam.id}`)
  }
}

function viewScore(exam: any) {
  // 直接从 takenRecords 获取成绩，弹窗展示
  const record = takenRecords.value.get(exam.id)
  if (!record) {
    ElMessage.warning('暂无成绩记录')
    return
  }
  const scoreText = record.score !== undefined ? `${record.score} 分` : '阅卷中'
  const passText = exam.passScore ? `（及格分：${exam.passScore} 分）` : ''
  ElMessageBox.alert(
    `<div style="text-align:center">
      <p style="font-size:18px;margin-bottom:4px"><strong>${exam.title}</strong></p>
      <p style="font-size:32px;color:var(--color-primary);margin:16px 0">${scoreText}</p>
      <p style="color:#909399">总分：${exam.totalScore} 分 ${passText}</p>
    </div>`,
    '考试成绩',
    { dangerouslyUseHTMLString: true, confirmButtonText: '知道了' }
  )
}

async function loadData() {
  loading.value = true
  try {
    const params: any = {
      current: currentPage.value,
      size: pageSize.value,
      status: statusFilter.value
    }
    if (!userStore.isTeacher && !userStore.isAdmin && myClassId.value) {
      params.classId = myClassId.value
    }
    const res = await getExamList(params)
    examList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadMyClass() {
  if (userStore.isTeacher || userStore.isAdmin) return
  try {
    const res = await getMyClasses()
    const classes = res.data || []
    if (classes.length > 0) myClassId.value = classes[0].id
  } catch { /* ignore */ }
}

async function loadTakenIds() {
  try {
    const res = await getMyRecords({ current: 1, size: 100 })
    if (res.data?.records) {
      const ids = new Set<number>()
      const records = new Map<number, { graded: boolean; score?: number }>()
      res.data.records.forEach((r: any) => {
        // 后端返回 Map<String,Object> 使用数据库列名(下划线格式)
        const examId = r.examId || r.paperId || r.paper_id || r.pid
        if (examId) {
          ids.add(examId)
          // 已阅卷(status=3)或有总分 → 视为已批改
          const total = r.totalScore ?? r.total_score
          const graded = r.status === 3 || (total !== null && total !== undefined)
          records.set(examId, { graded, score: total })
        }
      })
      takenExamIds.value = ids
      takenRecords.value = records
    }
  } catch { /* ignore */ }
}

onMounted(async () => {
  await loadMyClass()
  await loadTakenIds()
  loadData()
})
</script>

<style scoped>
.exam-list-page { padding: 24px; max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { font-size: 22px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.exam-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.exam-card {
  background: #fff; border-radius: 10px; padding: 20px; cursor: pointer;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06); transition: all 0.3s;
}
.exam-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.12); }
.exam-card.taken { opacity: 0.65; pointer-events: auto; cursor: default; border-left: 4px solid #67c23a; }
.exam-card.taken:hover { transform: none; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.exam-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.header-tags { display: flex; gap: 6px; align-items: center; }
.exam-duration { font-size: 13px; color: #909399; }
.exam-title { font-size: 17px; margin-bottom: 8px; color: #303133; }
.exam-desc { font-size: 13px; color: #909399; margin-bottom: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.exam-meta { display: flex; gap: 16px; font-size: 12px; color: #606266; margin-bottom: 16px; flex-wrap: wrap; }
.exam-footer { display: flex; justify-content: space-between; align-items: center; }
.exam-time { font-size: 13px; color: #909399; }
.empty-tip { text-align: center; color: #909399; padding: 60px 0; grid-column: 1/-1; }
.pagination-wrapper { display: flex; justify-content: center; }
</style>

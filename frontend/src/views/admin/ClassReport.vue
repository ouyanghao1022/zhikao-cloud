<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>班级报告</h2>
    </div>

    <el-card>
      <div class="filter-bar">
        <!-- 管理员：先选教师 -->
        <el-form-item v-if="userStore.isAdmin" label="选择教师" style="margin-bottom:0">
          <el-select v-model="selectedTeacherId" placeholder="选择教师" clearable style="width:180px" @change="onTeacherChange">
            <el-option v-for="t in teacherList" :key="t.id" :label="t.name + ' (' + t.classCount + '个班级)'" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-select v-model="selectedClass" placeholder="选择班级" clearable style="width:220px" @change="onClassChange">
          <el-option v-for="c in classes" :key="c.id" :label="c.className || c.name" :value="c.id" />
        </el-select>
        <el-select v-model="selectedExam" placeholder="选择考试" clearable :disabled="!selectedClass" style="width:280px" @change="loadReport">
          <el-option v-for="e in exams" :key="e.id" :label="e.title" :value="e.id" />
        </el-select>
        <el-button type="primary" @click="loadReport" :disabled="!selectedClass || !selectedExam">查看报告</el-button>
      </div>

      <div v-if="!selectedClass" class="empty-hint">
        请先选择班级
      </div>
      <div v-else-if="!selectedExam" class="empty-hint">
        请选择考试查看班级报告
      </div>

      <div v-else-if="reportData" class="report-content" v-loading="rLoading">
        <!-- 基本统计 -->
        <el-row :gutter="20" class="stats-row">
          <el-col :span="6" v-for="s in statsList" :key="s.label">
            <div class="stat-card">
              <div class="stat-num">{{ s.value }}</div>
              <div class="stat-text">{{ s.label }}</div>
            </div>
          </el-col>
        </el-row>

        <!-- 分数分布 -->
        <h4 class="section-title">分数段分布</h4>
        <el-table :data="distributionData" style="width:100%">
          <el-table-column prop="scoreRange" label="分数段" width="180" />
          <el-table-column prop="count" label="人数" width="120" align="center">
            <template #default="{ row }">
              <el-tag>{{ row.count }} 人</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="占比" min-width="200">
            <template #default="{ row }">
              <el-progress
                :percentage="getPercent(row.count)"
                :stroke-width="16"
                :color="getPercentColor(getPercent(row.count))"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getExamList } from '@/api/exam'
import { getMyClasses } from '@/api/class'
import { getClassReport } from '@/api/report'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const rLoading = ref(false)
const classes = ref<any[]>([])
const exams = ref<any[]>([])
const selectedClass = ref<number | undefined>(undefined)
const selectedExam = ref<number | undefined>(undefined)
const reportData = ref<any>(null)

// 管理员选择教师
const selectedTeacherId = ref<number | undefined>(undefined)
const teacherList = ref<any[]>([])

const statsList = computed(() => {
  if (!reportData.value?.stats) return []
  const s = reportData.value.stats
  return [
    { label: '参加人数', value: s.student_count || s.studentCount || 0 },
    { label: '平均分', value: s.avg_score || s.avgScore || '--' },
    { label: '最高分', value: s.max_score || s.maxScore || '--' },
    { label: '最低分', value: s.min_score || s.minScore || '--' }
  ]
})

const distributionData = computed(() => {
  const raw = reportData.value?.distribution || []
  return raw.map((r: any) => ({
    scoreRange: r.score_range || r.scoreRange || r.label,
    count: r.count || 0
  }))
})

function getPercent(count: number) {
  const total = distributionData.value.reduce((s: number, d: any) => s + d.count, 0)
  return total > 0 ? Math.round((count / total) * 100) : 0
}

function getPercentColor(pct: number) {
  if (pct > 40) return '#67C23A'
  if (pct > 20) return '#E6A23C'
  return '#F56C6C'
}

async function loadClassList() {
  loading.value = true
  try {
    if (userStore.isAdmin) {
      // 管理员：先加载教师列表
      const res = await request.get('/class/teachers')
      teacherList.value = res.data || []
      classes.value = [] // 需要先选教师
    } else {
      // 教师：加载自己的班级
      const res = await getMyClasses()
      classes.value = res.data || []
    }
  } catch { /* ignore */ } finally {
    loading.value = false
  }
}

async function onTeacherChange() {
  selectedClass.value = undefined
  selectedExam.value = undefined
  reportData.value = null
  classes.value = []
  exams.value = []

  if (!selectedTeacherId.value) return

  loading.value = true
  try {
    const res = await request.get('/class/list', { params: { current: 1, size: 200, teacherId: selectedTeacherId.value } })
    classes.value = res.data?.records || []
  } catch { classes.value = [] } finally {
    loading.value = false
  }
}

async function onClassChange() {
  selectedExam.value = undefined
  reportData.value = null
  exams.value = []
  if (!selectedClass.value) return
  loading.value = true
  try {
    const res = await getExamList({ current: 1, size: 100, classId: selectedClass.value } as any)
    exams.value = res.data?.records || []
  } catch { /* ignore */ } finally {
    loading.value = false
  }
}

async function loadReport() {
  if (!selectedExam.value || !selectedClass.value) return
  rLoading.value = true
  try {
    const res = await getClassReport(selectedExam.value, selectedClass.value)
    if (res.code === 200) reportData.value = res.data
    else reportData.value = null
  } catch (e: any) {
    reportData.value = null
  } finally {
    rLoading.value = false
  }
}

onMounted(loadClassList)
</script>

<style scoped>
.empty-hint { text-align: center; padding: 60px 0; color: #909399; font-size: 15px; }
.stats-row { margin-bottom: 24px; }
.stat-card { background: #f5f7fa; border-radius: 10px; padding: 20px; text-align: center; }
.stat-num { font-size: 28px; font-weight: 700; color: #409EFF; }
.stat-text { font-size: 13px; color: #909399; margin-top: 4px; }
.section-title { font-size: 16px; color: #303133; margin: 0 0 12px; }
</style>

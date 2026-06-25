<template>
  <div class="dashboard">
    <!-- 欢迎卡片 -->
    <div class="welcome-card" :class="welcomeTheme">
      <div class="welcome-text">
        <h1>👋 欢迎回来，{{ userStore.nickname || userStore.username }}</h1>
        <p>{{ subtitle }}</p>
      </div>
      <div class="welcome-extra" v-if="!userStore.isTeacher && !userStore.isAdmin">
        <button class="action-btn" @click="$router.push('/exam')">开始考试</button>
      </div>
    </div>

    <!-- 学生视图 -->
    <template v-if="!userStore.isTeacher && !userStore.isAdmin">
      <!-- 积分签到 -->
      <div class="points-card" @click="handleCheckin">
        <div class="points-left">
          <span class="level-badge" :class="'lv-' + pointsData.level">{{ pointsData.levelName }}</span>
          <span class="points-num">{{ pointsData.totalPoints }} 积分</span>
        </div>
        <div class="points-right">
          <button class="checkin-btn" :class="{ done: pointsData.checkedToday }" :disabled="pointsData.checkedToday" @click.stop="handleCheckin">
            {{ pointsData.checkedToday ? '今日已签到' : '每日签到 +' + (5 + Math.min(pointsData.streak || 0, 30)) }}
          </button>
          <span v-if="pointsData.streak > 1" class="streak">🔥 连续 {{ pointsData.streak }} 天</span>
        </div>
      </div>

      <!-- 统计 -->
      <div class="stats-row">
        <div class="stat-card" v-for="s in studentStats" :key="s.label">
          <div class="stat-icon" :style="{ color: s.color }">{{ s.icon }}</div>
          <div class="stat-body">
            <div class="stat-value">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>
      </div>

      <!-- 最近记录 -->
      <div class="section-card">
        <div class="section-head">📝 最近考试记录</div>
        <table class="data-table" v-if="recentRecords.length">
          <thead><tr><th>试卷</th><th>得分</th><th>状态</th><th>时间</th></tr></thead>
          <tbody>
            <tr v-for="r in recentRecords" :key="r.id">
              <td>{{ r.paperTitle || r.title }}</td>
              <td><strong>{{ r.totalScore }}</strong></td>
              <td><span class="tag" :class="r.status === 3 ? 'green' : 'orange'">{{ ['进行中','已提交','超时','已批阅'][r.status] || '--' }}</span></td>
              <td class="muted">{{ formatTime(r.submitTime) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty">暂无考试记录</div>
      </div>
    </template>

    <!-- 教师/管理员视图 -->
    <template v-else>
      <div class="stats-row">
        <div class="stat-card" v-for="s in stats" :key="s.label">
          <div class="stat-icon" :style="{ color: s.color }">{{ s.icon }}</div>
          <div class="stat-body">
            <div class="stat-value">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>
      </div>
      <div v-if="userStore.isAdmin" class="section-card">
        <div class="section-head">🖥 系统信息</div>
        <div class="info-grid">
          <div class="info-item"><span class="info-key">版本</span><span>v2.0</span></div>
          <div class="info-item"><span class="info-key">数据库</span><span>MySQL 8</span></div>
          <div class="info-item"><span class="info-key">后端</span><span>Spring Boot 3</span></div>
          <div class="info-item"><span class="info-key">前端</span><span>Vue 3</span></div>
          <div class="info-item"><span class="info-key">用户数</span><span>{{ stats[0]?.value || 0 }}</span></div>
          <div class="info-item"><span class="info-key">试卷数</span><span>{{ stats[1]?.value || 0 }}</span></div>
          <div class="info-item"><span class="info-key">题库数</span><span>{{ stats[2]?.value || 0 }}</span></div>
          <div class="info-item"><span class="info-key">考试数</span><span>{{ stats[3]?.value || 0 }}</span></div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getMyRecords } from '@/api/exam'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const userStore = useUserStore()

const welcomeTheme = computed(() => userStore.isAdmin ? 'admin' : userStore.isTeacher ? 'teacher' : 'student')
const subtitle = computed(() => userStore.isAdmin ? '管理用户、试卷与题库' : userStore.isTeacher ? '管理考试与班级学情' : '今天也要加油学习哦！')

const studentStats = ref([
  { icon: '📊', label: '考试次数', value: 0, color: '#4dabf7' },
  { icon: '🎯', label: '平均分', value: '--', color: '#51cf66' },
  { icon: '🏆', label: '最高分', value: '--', color: '#ffd43b' },
  { icon: '📚', label: '错题数', value: 0, color: '#ff6b6b' }
])

const stats = ref([
  { icon: '👤', label: '用户总数', value: 0, color: '#4dabf7' },
  { icon: '📝', label: '试卷总数', value: 0, color: '#51cf66' },
  { icon: '📖', label: '题库总数', value: 0, color: '#ffd43b' },
  { icon: '📈', label: '考试次数', value: 0, color: '#ff6b6b' }
])

const recentRecords = ref<any[]>([])
const pointsData = reactive({ totalPoints: 0, level: 1, levelName: '青铜', streak: 0, checkedToday: false })

function formatTime(t: string) { return t ? dayjs(t).format('MM-DD HH:mm') : '--' }

async function loadStudentData() {
  try {
    const res = await getMyRecords({ current: 1, size: 10 })
    recentRecords.value = res.data?.records || []
    const scores = recentRecords.value.map((r: any) => Number(r.totalScore)).filter((s: number) => !isNaN(s))
    studentStats.value[0].value = recentRecords.value.length
    if (scores.length) {
      studentStats.value[1].value = Math.round(scores.reduce((a: number, b: number) => a + b, 0) / scores.length) + ''
      studentStats.value[2].value = Math.max(...scores) + ''
    }
    try {
      const r2 = await request.get('/user/my-stats')
      if (r2.data) {
        const d = r2.data
        studentStats.value[0].value = d.examCount || 0
        studentStats.value[1].value = (d.avgScore || 0) + ''
        studentStats.value[2].value = (d.maxScore || 0) + ''
        studentStats.value[3].value = d.wrongCount || 0
      }
    } catch {}
  } catch {}
}

async function loadPoints() {
  try { const r = await request.get('/points/my'); if (r.data) Object.assign(pointsData, r.data) } catch {}
}

async function handleCheckin() {
  if (pointsData.checkedToday) return
  try {
    const r = await request.post('/points/checkin')
    if (r.data?.success) { Object.assign(pointsData, r.data, { checkedToday: true }); ElMessage.success(`+${r.data.points} 积分！`) }
    else { ElMessage.info(r.data?.message || '已签到'); pointsData.checkedToday = true }
  } catch {}
}

async function loadAdminData() {
  try {
    const r = await request.get('/user/stats')
    if (r.data) { const d = r.data; stats.value[0].value = d.userCount || 0; stats.value[1].value = d.examCount || 0; stats.value[2].value = d.questionCount || 0; stats.value[3].value = d.sessionCount || 0 }
  } catch {}
}

onMounted(() => {
  if (!userStore.isTeacher && !userStore.isAdmin) { loadStudentData(); loadPoints() }
  else loadAdminData()
})
</script>

<style scoped>
.dashboard { padding: 28px; max-width: 1100px; }

.welcome-card { border-radius: 14px; padding: 32px 36px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; color: #fff; }
.welcome-card.student { background: linear-gradient(135deg, #4dabf7, #4263eb); }
.welcome-card.teacher { background: linear-gradient(135deg, #f76707, #e03131); }
.welcome-card.admin { background: linear-gradient(135deg, #343a40, #495057); }
.welcome-card h1 { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
.welcome-card p { opacity: .8; margin: 0; font-size: 14px; }
.action-btn { border: 2px solid rgba(255,255,255,.5); background: rgba(255,255,255,.15); color: #fff; padding: 10px 24px; border-radius: 8px; font-size: 14px; cursor: pointer; font-weight: 600; transition: all .2s; }
.action-btn:hover { background: rgba(255,255,255,.25); border-color: #fff; }

/* 积分卡 */
.points-card { background: linear-gradient(135deg, #ffd43b, #f59f00); border-radius: 12px; padding: 18px 24px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; color: #fff; cursor: pointer; }
.points-left { display: flex; align-items: center; gap: 12px; }
.level-badge { padding: 4px 14px; border-radius: 20px; font-weight: 700; font-size: 13px; }
.lv-1 { background: #e8590c; } .lv-2 { background: #adb5bd; } .lv-3 { background: #fab005; color: #333; }
.lv-4 { background: #51cf66; } .lv-5 { background: #4dabf7; } .lv-6 { background: #cc5de8; } .lv-7 { background: #e03131; }
.points-num { font-weight: 600; }
.checkin-btn { background: rgba(255,255,255,.25); border: none; color: #fff; padding: 8px 18px; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: 13px; }
.checkin-btn:hover:not(:disabled) { background: rgba(255,255,255,.4); }
.checkin-btn:disabled, .checkin-btn.done { opacity: .6; cursor: default; }
.streak { font-size: 13px; margin-left: 10px; }

/* 统计 */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { background: #fff; border-radius: 12px; padding: 22px 20px; display: flex; align-items: center; gap: 14px; box-shadow: 0 1px 3px rgba(0,0,0,.04); }
.stat-icon { font-size: 32px; }
.stat-value { font-size: 24px; font-weight: 700; color: #212529; }
.stat-label { font-size: 12px; color: #868e96; margin-top: 2px; }

/* 章节卡片 */
.section-card { background: #fff; border-radius: 12px; padding: 24px; box-shadow: 0 1px 3px rgba(0,0,0,.04); }
.section-head { font-size: 16px; font-weight: 600; margin-bottom: 16px; color: #212529; }

/* 表格 */
.data-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.data-table th { text-align: left; padding: 10px 12px; color: #868e96; font-weight: 600; font-size: 12px; text-transform: uppercase; border-bottom: 2px solid #f1f3f5; }
.data-table td { padding: 10px 12px; border-bottom: 1px solid #f1f3f5; color: #495057; }
.data-table tr:hover td { background: #f8f9fa; }
.tag { padding: 2px 10px; border-radius: 10px; font-size: 12px; font-weight: 600; }
.tag.green { background: #d3f9d8; color: #2b8a3e; }
.tag.orange { background: #fff3bf; color: #e67700; }
.muted { color: #adb5bd; font-size: 13px; }
.empty { text-align: center; padding: 40px 0; color: #adb5bd; font-size: 13px; }

/* 信息网格 */
.info-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.info-item { padding: 12px 16px; background: #f8f9fa; border-radius: 8px; font-size: 13px; color: #495057; }
.info-key { display: block; font-size: 11px; color: #adb5bd; margin-bottom: 2px; text-transform: uppercase; }

@media (max-width: 768px) {
  .stats-row, .info-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>

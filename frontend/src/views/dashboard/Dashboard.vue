<template>
  <div class="dashboard">
    <!-- 欢迎卡片 -->
    <div class="welcome-card" :class="welcomeTheme">
      <div class="welcome-left">
        <div class="welcome-deco"></div>
        <div class="welcome-text">
          <h1>欢迎回来，{{ userStore.nickname || userStore.username }}</h1>
          <p>{{ subtitle }}</p>
        </div>
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
          <span v-if="pointsData.streak > 1" class="streak">连续 {{ pointsData.streak }} 天</span>
        </div>
      </div>

      <!-- 统计 -->
      <div class="stats-row">
        <div class="stat-card" v-for="(s, i) in studentStats" :key="s.label">
          <div class="stat-icon-box" :style="{ background: statColors[i].bg, color: statColors[i].fg }">
            <component :is="s.icon" />
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>
      </div>

      <!-- 最近记录 -->
      <div class="section-card">
        <div class="section-head">最近考试记录</div>
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
        <div class="stat-card" v-for="(s, i) in stats" :key="s.label">
          <div class="stat-icon-box" :style="{ background: adminStatColors[i].bg, color: adminStatColors[i].fg }">
            <component :is="s.icon" />
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>
      </div>
      <div v-if="userStore.isAdmin" class="section-card">
        <div class="section-head">系统信息</div>
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
import { ref, computed, reactive, onMounted, markRaw } from 'vue'
import { useUserStore } from '@/stores/user'
import { getMyRecords } from '@/api/exam'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { DataBoard, Document, Trophy, Notebook } from '@element-plus/icons-vue'

const userStore = useUserStore()

const welcomeTheme = computed(() => userStore.isAdmin ? 'admin' : userStore.isTeacher ? 'teacher' : 'student')
const subtitle = computed(() => userStore.isAdmin ? '管理用户、试卷与题库' : userStore.isTeacher ? '管理考试与班级学情' : '今天也要加油学习哦！')

const statColors = [
  { bg: 'rgba(5,150,105,0.08)', fg: '#059669' },
  { bg: 'rgba(59,130,246,0.08)', fg: '#3b82f6' },
  { bg: 'rgba(217,119,6,0.08)', fg: '#d97706' },
  { bg: 'rgba(220,38,38,0.08)', fg: '#dc2626' },
]
const adminStatColors = [
  { bg: 'rgba(59,130,246,0.08)', fg: '#3b82f6' },
  { bg: 'rgba(5,150,105,0.08)', fg: '#059669' },
  { bg: 'rgba(217,119,6,0.08)', fg: '#d97706' },
  { bg: 'rgba(168,85,247,0.08)', fg: '#a855f7' },
]

const studentStats = ref([
  { icon: markRaw(DataBoard), label: '考试次数', value: 0 },
  { icon: markRaw(Document), label: '平均分', value: '--' },
  { icon: markRaw(Trophy), label: '最高分', value: '--' },
  { icon: markRaw(Notebook), label: '错题数', value: 0 },
])

const stats = ref([
  { icon: markRaw(DataBoard), label: '用户总数', value: 0 },
  { icon: markRaw(Document), label: '试卷总数', value: 0 },
  { icon: markRaw(Notebook), label: '题库总数', value: 0 },
  { icon: markRaw(Trophy), label: '考试次数', value: 0 },
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

/* 欢迎卡片 — 渐变底 + 装饰条 */
.welcome-card {
  border-radius: var(--radius-lg); padding: 34px 36px; display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 24px; color: #fff; position: relative; overflow: hidden;
}
.welcome-left { display: flex; align-items: center; gap: 16px; position: relative; z-index: 1; }
.welcome-deco {
  width: 4px; height: 44px; border-radius: 2px; background: rgba(255,255,255,0.4); flex-shrink: 0;
}
.welcome-card h1 { font-size: 20px; font-weight: 700; margin: 0 0 4px; letter-spacing: 1px; }
.welcome-card p { opacity: .75; margin: 0; font-size: 13px; }

.welcome-card.student { background: linear-gradient(135deg, #059669, #0d9488); }
.welcome-card.teacher { background: linear-gradient(135deg, #b45309, #92400e); }
.welcome-card.admin { background: linear-gradient(135deg, #1e293b, #334155); }

.action-btn {
  border: 1.5px solid rgba(255,255,255,0.4); background: rgba(255,255,255,0.12); color: #fff;
  padding: 10px 26px; border-radius: var(--radius-sm); font-size: 14px; cursor: pointer; font-weight: 600;
  letter-spacing: 2px; transition: all .2s;
}
.action-btn:hover { background: rgba(255,255,255,0.22); border-color: rgba(255,255,255,0.7); }

/* 积分卡 */
.points-card {
  background: linear-gradient(135deg, var(--color-accent-warm), #92400e);
  border-radius: var(--radius-lg); padding: 18px 24px; display: flex; justify-content: space-between;
  align-items: center; margin-bottom: 20px; color: #fff; cursor: pointer;
}
.points-left { display: flex; align-items: center; gap: 12px; }
.level-badge { padding: 4px 14px; border-radius: var(--radius-lg); font-weight: 700; font-size: 13px; background: rgba(255,255,255,0.2); }
.points-num { font-weight: 600; }
.checkin-btn {
  background: rgba(255,255,255,0.2); border: none; color: #fff; padding: 8px 18px;
  border-radius: var(--radius-sm); cursor: pointer; font-weight: 600; font-size: 13px;
}
.checkin-btn:hover:not(:disabled) { background: rgba(255,255,255,0.3); }
.checkin-btn:disabled, .checkin-btn.done { opacity: .6; cursor: default; }
.streak { font-size: 13px; margin-left: 10px; letter-spacing: 1px; }

/* 统计 */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card {
  background: var(--color-rice-card); border-radius: var(--radius-lg); padding: 22px 20px;
  display: flex; align-items: center; gap: 14px;
  box-shadow: var(--shadow-sm); border: 1px solid var(--color-rice-border);
}
.stat-icon-box {
  width: 44px; height: 44px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center;
  font-size: 20px; flex-shrink: 0;
}
.stat-value { font-size: 24px; font-weight: 700; color: var(--color-ink); }
.stat-label { font-size: 12px; color: var(--color-ink-muted); margin-top: 2px; }

/* 章节卡片 */
.section-card {
  background: var(--color-rice-card); border-radius: var(--radius-lg); padding: 24px;
  box-shadow: var(--shadow-sm); border: 1px solid var(--color-rice-border);
}
.section-head { font-size: 16px; font-weight: 700; margin-bottom: 16px; color: var(--color-ink); letter-spacing: 1px; }

/* 表格 */
.data-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.data-table th { text-align: left; padding: 10px 12px; color: var(--color-ink-muted); font-weight: 600; font-size: 12px; border-bottom: 2px solid var(--color-rice-border); }
.data-table td { padding: 10px 12px; border-bottom: 1px solid var(--color-rice-border); color: var(--color-ink-light); }
.data-table tr:hover td { background: rgba(0,0,0,0.02); }
.tag { padding: 2px 10px; border-radius: var(--radius-md); font-size: 12px; font-weight: 600; }
.tag.green { background: rgba(5,150,105,0.1); color: var(--color-primary); }
.tag.orange { background: rgba(217,119,6,0.1); color: var(--color-accent-warm); }
.muted { color: var(--color-ink-muted); font-size: 13px; }
.empty { text-align: center; padding: 40px 0; color: var(--color-ink-muted); font-size: 13px; }

/* 信息网格 */
.info-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.info-item { padding: 12px 16px; background: var(--color-rice); border-radius: var(--radius-md); font-size: 13px; color: var(--color-ink-light); border: 1px solid var(--color-rice-border); }
.info-key { display: block; font-size: 11px; color: var(--color-ink-muted); margin-bottom: 2px; }

@media (max-width: 768px) {
  .stats-row, .info-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>

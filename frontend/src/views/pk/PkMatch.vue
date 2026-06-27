<template>
  <div class="pk-match-page">
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft" type="default" size="small">返回大厅</el-button>
      <h2>对战详情</h2>
    </div>

    <el-row :gutter="16" v-loading="loading">
      <!-- 对战信息 -->
      <el-col :span="16">
        <el-card class="match-card">
          <div class="match-vs">
            <div class="team team-a" :class="{ winner: match.winnerTeamId === match.teamAId }">
              <div class="team-name">{{ match.teamAName || '队伍A' }}</div>
              <div class="team-score">{{ scoreA }}</div>
              <el-tag v-if="match.winnerTeamId === match.teamAId" type="warning" size="small">获胜</el-tag>
            </div>
            <div class="vs-badge">VS</div>
            <div class="team team-b" :class="{ winner: match.winnerTeamId === match.teamBId }">
              <div class="team-name">{{ match.teamBName || '队伍B' }}</div>
              <div class="team-score">{{ scoreB }}</div>
              <el-tag v-if="match.winnerTeamId === match.teamBId" type="warning" size="small">获胜</el-tag>
            </div>
          </div>

          <el-descriptions :column="4" border size="small" style="margin-top:20px">
            <el-descriptions-item label="编号">{{ match.matchNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusType" size="small">{{ statusLabel }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="轮次">{{ match.currentRound || 0 }} / {{ match.totalRounds || '--' }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ matchTypeMap[match.matchType] || '--' }}</el-descriptions-item>
          </el-descriptions>

          <!-- 操作按钮 -->
          <div v-if="match.status === 0" class="match-actions">
            <el-button type="primary" size="small" @click="showQuestionDialog = true" :disabled="!myTeamId">
              开始答题
            </el-button>
            <el-popconfirm title="确定结束对战？将根据正确数判定胜负" @confirm="handleEndMatch">
              <template #reference>
                <el-button type="danger" size="small">结束对战</el-button>
              </template>
            </el-popconfirm>
          </div>

          <div v-if="match.status !== 0 && match.winnerTeamName" class="match-result">
            <h3 class="result-text">{{ match.winnerTeamName }} 获胜！</h3>
          </div>
        </el-card>
      </el-col>

      <!-- 答题记录 -->
      <el-col :span="8">
        <el-card class="records-card">
          <template #header>
            <span style="font-weight:600">答题记录 ({{ records.length }})</span>
            <el-button link size="small" style="float:right" @click="loadRecords">刷新</el-button>
          </template>
          <div v-if="records.length === 0" class="empty-records">暂无答题记录</div>
          <div v-for="r in records" :key="r.id" class="record-item">
            <div class="record-header">
              <span class="record-user">{{ r.username || ('用户' + r.userId) }}</span>
              <span class="record-round">第{{ r.roundNum }}轮</span>
            </div>
            <div class="record-answer">
              <span>答案：{{ r.answer }}</span>
              <el-tag :type="r.isCorrect ? 'success' : 'danger'" size="small" effect="plain">
                {{ r.isCorrect ? '✓' : '✗' }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 答题对话框 -->
    <el-dialog v-model="showQuestionDialog" title="选择题目答题" width="520px">
      <div class="question-picker">
        <p style="color:#909399;font-size:13px;margin-bottom:12px">
          对战共 {{ match.totalRounds }} 轮，选择一道题目进行答题
        </p>
        <el-select v-model="selectedRound" placeholder="选择轮次" style="width:100%;margin-bottom:12px">
          <el-option
            v-for="i in match.totalRounds"
            :key="i"
            :label="`第 ${i} 轮`"
            :value="i"
          />
        </el-select>
        <el-input v-model="userAnswer" placeholder="输入你的答案..." />
      </div>
      <template #footer>
        <el-button @click="showQuestionDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!selectedRound || !userAnswer.trim()" @click="handleSubmitAnswer">
          提交答案
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMatchDetail, getMatchRecords, submitAnswer, endMatch } from '@/api/pk'
import { getMyTeam } from '@/api/pk'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const matchId = Number(route.params.id)

const loading = ref(false)
const submitting = ref(false)
const match = ref<any>({})
const records = ref<any[]>([])
const myTeamId = ref<number | null>(null)

const matchTypeMap: Record<number, string> = { 1: '随机匹配', 2: '直接挑战' }

const showQuestionDialog = ref(false)
const selectedRound = ref<number | null>(null)
const userAnswer = ref('')

const scoreA = computed(() => records.value.filter((r: any) => r.teamId === match.value.teamAId && r.isCorrect).length)
const scoreB = computed(() => records.value.filter((r: any) => r.teamId === match.value.teamBId && r.isCorrect).length)

const statusType = computed(() => {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info' }
  return map[match.value.status] || 'info'
})
const statusLabel = computed(() => {
  const map: Record<number, string> = { 0: '进行中', 1: '已结束', 2: '平局' }
  return map[match.value.status] || '未知'
})

function goBack() { router.push('/pk') }

async function loadMatch() {
  loading.value = true
  try {
    const res = await getMatchDetail(matchId)
    match.value = res.data || {}
  } finally {
    loading.value = false
  }
}

async function loadRecords() {
  try {
    const res = await getMatchRecords(matchId)
    records.value = res.data || []
  } catch { records.value = [] }
}

async function handleSubmitAnswer() {
  if (!selectedRound.value || !userAnswer.value.trim() || !myTeamId.value) return
  submitting.value = true
  try {
    await submitAnswer({
      matchId,
      teamId: myTeamId.value,
      roundNum: selectedRound.value,
      questionId: 0,  // no real question loaded yet, answer is free text
      answer: userAnswer.value.trim(),
      answerTimeMs: 0
    })
    ElMessage.success('答案已提交')
    showQuestionDialog.value = false
    userAnswer.value = ''
    selectedRound.value = null
    await loadRecords()
  } catch {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

async function handleEndMatch() {
  try {
    const res = await endMatch(matchId)
    ElMessage.success('对战已结束')
    match.value = res.data || match.value
    await loadRecords()
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(async () => {
  await loadMatch()
  await loadRecords()
  // 加载我的队伍
  try {
    const teamRes = await getMyTeam()
    if (teamRes.data) {
      myTeamId.value = teamRes.data.id
    }
  } catch { /* ignore */ }
})
</script>

<style scoped>
.pk-match-page { padding: 24px; max-width: 1200px; margin: 0 auto; }
.page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
.page-header h2 { margin: 0; font-size: 20px; }

.match-card { margin-bottom: 16px; }

.match-vs {
  display: flex; align-items: center; justify-content: center; gap: 32px;
  padding: 24px 0;
}
.team { text-align: center; }
.team-name { font-size: 18px; font-weight: 700; color: #303133; margin-bottom: 8px; }
.team-score { font-size: 36px; font-weight: 800; color: var(--color-primary); }
.winner .team-name { color: #e6a23c; }
.winner .team-score { color: #f56c6c; }
.vs-badge {
  font-size: 28px; font-weight: 900; color: #dcdfe6;
  background: #f5f7fa; border-radius: 50%; width: 64px; height: 64px;
  display: flex; align-items: center; justify-content: center;
}

.match-actions { margin-top: 16px; display: flex; gap: 8px; }
.match-result { text-align: center; margin-top: 20px; padding: 20px; background: #fdf6ec; border-radius: 10px; }
.result-text { font-size: 22px; color: #e6a23c; margin: 0; }

.records-card { }
.empty-records { text-align: center; padding: 32px 0; color: #c0c4cc; font-size: 13px; }
.record-item {
  padding: 10px 8px; border-bottom: 1px solid #f0f0f0;
}
.record-item:last-child { border: none; }
.record-header { display: flex; justify-content: space-between; margin-bottom: 4px; }
.record-user { font-size: 13px; font-weight: 600; color: #303133; }
.record-round { font-size: 12px; color: #909399; }
.record-answer { display: flex; justify-content: space-between; align-items: center; }
</style>

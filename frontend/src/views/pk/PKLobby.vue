<template>
  <div class="pk-lobby-page">
    <div class="page-header">
      <h2>组队PK大厅</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon> 创建队伍
        </el-button>
        <el-button type="success" :disabled="!myTeam" :loading="matching" @click="handleStartMatch">
          快速匹配
        </el-button>
      </div>
    </div>

    <!-- 我的队伍 -->
    <el-card v-if="myTeam" class="my-team-card accent-gold" shadow="hover">
      <template #header>
        <div class="my-team-header">
          <span class="my-team-label">我的队伍</span>
          <el-tag :type="myTeam.status === 1 ? 'success' : 'info'">
            {{ myTeam.status === 1 ? '正常' : '已解散' }}
          </el-tag>
        </div>
      </template>
      <div class="my-team-info">
        <h3>{{ myTeam.teamName }}</h3>
        <p class="team-slogan" v-if="myTeam.slogan">{{ myTeam.slogan }}</p>
        <div class="team-stats">
          <el-statistic title="积分" :value="myTeam.totalScore || 0" />
          <el-statistic title="胜场" :value="myTeam.winCount || 0" />
          <el-statistic title="败场" :value="myTeam.loseCount || 0" />
          <el-statistic title="成员" :value="`${myTeam.currentMembers || 0}/${myTeam.maxMembers || 5}`" />
        </div>
        <el-button type="danger" plain size="small" @click="handleLeaveTeam" style="margin-top:12px">
          退出队伍
        </el-button>
      </div>
    </el-card>

    <!-- 队伍列表 -->
    <el-card class="team-list-card accent-jade">
      <template #header>
        <div class="card-header">
          <span>队伍列表 ({{ teams.length }})</span>
          <el-input v-model="searchKeyword" placeholder="搜索队伍..." clearable style="width: 240px" :prefix-icon="Search" />
        </div>
      </template>
      <div class="team-grid" v-loading="loading">
        <div v-for="team in filteredTeams" :key="team.id" class="team-card">
          <div class="team-card-header">
            <h4 class="team-name">{{ team.teamName }}</h4>
            <el-tag :type="team.joinType === 1 ? 'success' : team.joinType === 2 ? 'warning' : 'info'" size="small">
              {{ ['', '公开', '审核', '邀请'][team.joinType] || '未知' }}
            </el-tag>
          </div>
          <p v-if="team.slogan" class="team-slogan">{{ team.slogan }}</p>
          <div class="team-meta">
            <span>积分: {{ team.totalScore || 0 }}</span>
            <span>胜率: {{ getWinRate(team) }}%</span>
            <span>{{ team.currentMembers || 0 }}/{{ team.maxMembers || 5 }}人</span>
          </div>
          <div class="team-actions">
            <el-button
              type="primary"
              size="small"
              :disabled="!!myTeam || (team.currentMembers >= team.maxMembers)"
              @click="handleJoinTeam(team.id)"
            >
              {{ myTeam && myTeam.id === team.id ? '已加入' : team.currentMembers >= team.maxMembers ? '已满员' : '加入队伍' }}
            </el-button>
            <el-button size="small" @click="handleChallengeTeam(team.id)" :disabled="!myTeam || myTeam.id === team.id" :loading="challenging === team.id">
              发起挑战
            </el-button>
          </div>
        </div>
        <div v-if="teams.length === 0 && !loading" class="empty-tip">
          暂无队伍，快来创建第一个队伍吧
        </div>
      </div>
    </el-card>

    <!-- 创建队伍对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建队伍" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="队伍名称" required>
          <el-input v-model="createForm.teamName" placeholder="请输入队伍名称" maxlength="20" />
        </el-form-item>
        <el-form-item label="队伍口号">
          <el-input v-model="createForm.slogan" placeholder="请输入队伍口号" maxlength="50" />
        </el-form-item>
        <el-form-item label="最大人数">
          <el-input-number v-model="createForm.maxMembers" :min="2" :max="10" />
        </el-form-item>
        <el-form-item label="加入方式">
          <el-radio-group v-model="createForm.joinType">
            <el-radio :value="1">公开加入</el-radio>
            <el-radio :value="2">审核加入</el-radio>
            <el-radio :value="3">邀请加入</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateTeam">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getTeams, createTeam, joinTeam, leaveTeam, startMatch, getMyTeam } from '@/api/pk'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()

const loading = ref(false)
const creating = ref(false)
const matching = ref(false)
const challenging = ref<number | null>(null)
const teams = ref<any[]>([])
const myTeam = ref<any>(null)
const searchKeyword = ref('')
const showCreateDialog = ref(false)

const createForm = ref({
  teamName: '',
  slogan: '',
  maxMembers: 5,
  joinType: 1
})

const filteredTeams = computed(() => {
  if (!searchKeyword.value) return teams.value
  return teams.value.filter((t: any) =>
    t.teamName?.includes(searchKeyword.value) ||
    t.slogan?.includes(searchKeyword.value)
  )
})

function getWinRate(team: any) {
  const total = (team.winCount || 0) + (team.loseCount || 0)
  return total > 0 ? Math.round((team.winCount / total) * 100) : 0
}

async function loadTeams() {
  loading.value = true
  try {
    const res = await getTeams()
    teams.value = res.data || []
    // 查找我的队伍
    await findMyTeam()
  } finally {
    loading.value = false
  }
}

async function findMyTeam() {
  try {
    const res = await getMyTeam()
    if (res.data) {
      myTeam.value = res.data
    }
  } catch { /* ignore */ }
}

async function handleCreateTeam() {
  if (!createForm.value.teamName.trim()) {
    ElMessage.warning('请输入队伍名称')
    return
  }
  creating.value = true
  try {
    await createTeam(createForm.value)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    createForm.value = { teamName: '', slogan: '', maxMembers: 5, joinType: 1 }
    await loadTeams()
  } catch { /* ignore */ } finally {
    creating.value = false
  }
}

async function handleJoinTeam(teamId: number) {
  try {
    await joinTeam(teamId)
    ElMessage.success('加入成功')
    await loadTeams()
  } catch { /* ignore */ }
}

async function handleLeaveTeam() {
  if (!myTeam.value) return
  try {
    await ElMessageBox.confirm('确定要退出队伍吗？队长退出将解散队伍。', '确认退出', { type: 'warning' })
    await leaveTeam(myTeam.value.id)
    myTeam.value = null
    ElMessage.success('已退出队伍')
    await loadTeams()
  } catch { /* cancel */ }
}

async function handleStartMatch() {
  if (matching.value) return
  matching.value = true
  try {
    const res = await startMatch({ matchType: 1 })
    ElMessage.success('匹配成功，正在进入对战...')
    if (res.data?.matchId) {
      router.push(`/pk/match/${res.data.matchId}`)
    } else if (res.data?.id) {
      router.push(`/pk/match/${res.data.id}`)
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '匹配失败，请稍后重试')
  } finally {
    matching.value = false
  }
}

async function handleChallengeTeam(teamId: number) {
  if (challenging.value === teamId) return
  challenging.value = teamId
  try {
    const res = await startMatch({ matchType: 2, teamBId: teamId })
    ElMessage.success('挑战已发起，正在进入对战...')
    if (res.data?.matchId) {
      router.push(`/pk/match/${res.data.matchId}`)
    } else if (res.data?.id) {
      router.push(`/pk/match/${res.data.id}`)
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '挑战发起失败，请稍后重试')
  } finally {
    challenging.value = null
  }
}

onMounted(loadTeams)
</script>

<style scoped>
.pk-lobby-page { padding: 24px; max-width: 1200px; margin: 0 auto; }
.header-actions { display: flex; gap: 12px; }

.my-team-card { margin-bottom: 24px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; }
.my-team-card :deep(.el-card__header) { border-color: rgba(255,255,255,0.2); }
.my-team-header { display: flex; justify-content: space-between; align-items: center; }
.my-team-label { font-weight: 600; font-size: 16px; color: #fff; }
.my-team-info h3 { color: #fff; margin-bottom: 8px; }
.team-slogan { color: rgba(255,255,255,0.8); font-size: 14px; margin-bottom: 16px; }
.team-stats { display: flex; gap: 24px; }
.my-team-card :deep(.el-statistic__head) { color: rgba(255,255,255,0.7); }
.my-team-card :deep(.el-statistic__content) { color: #fff; }

.team-list-card { }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.team-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.team-card {
  border: 1px solid var(--color-rice-border); border-radius: var(--radius-md); padding: 20px;
  transition: all 0.3s;
}
.team-card:hover { box-shadow: var(--shadow-md); border-color: var(--color-primary); }
.team-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.team-name { font-size: 16px; color: var(--color-ink); margin: 0; }
.team-meta { display: flex; gap: 16px; font-size: 13px; color: var(--color-ink-muted); margin: 12px 0; }
.team-actions { display: flex; gap: 8px; margin-top: 12px; }
.empty-tip { text-align: center; color: var(--color-ink-muted); padding: 40px 0; grid-column: 1/-1; }
</style>

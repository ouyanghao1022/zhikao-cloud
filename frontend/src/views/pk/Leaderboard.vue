<template>
  <div class="leaderboard-page">
    <div class="page-header">
      <h2>排行榜</h2>
      <div class="admin-actions" v-if="userStore.isAdmin">
        <el-button size="small" :loading="refreshing" @click="doRefresh">刷新榜单</el-button>
        <el-button size="small" type="warning" @click="doArchive">归档赛季</el-button>
      </div>
    </div>

    <!-- 类型和周期Tab切换 -->
    <el-card class="filter-card">
      <div class="tab-row">
        <div class="tab-group">
          <span class="tab-label">榜单类型：</span>
          <el-radio-group v-model="currentType" @change="loadData">
            <el-radio-button :value="1">个人积分</el-radio-button>
            <el-radio-button :value="2">个人胜率</el-radio-button>
            <el-radio-button :value="3">个人连胜</el-radio-button>
            <el-radio-button :value="4">战队积分</el-radio-button>
            <el-radio-button :value="5">战队胜率</el-radio-button>
          </el-radio-group>
        </div>
        <div class="tab-group">
          <span class="tab-label">周期：</span>
          <el-radio-group v-model="currentPeriod" @change="loadData">
            <el-radio-button :value="0">总榜</el-radio-button>
            <el-radio-button :value="1">周榜</el-radio-button>
            <el-radio-button :value="2">月榜</el-radio-button>
          </el-radio-group>
        </div>
      </div>
    </el-card>

    <!-- Top 3 展示 -->
    <div class="top-three" v-if="leaderboard.length >= 3">
      <div class="top-item rank-2" @click="scrollToList">
        <div class="rank-badge silver">2</div>
        <div class="top-avatar">{{ getAvatar(leaderboard[1]) }}</div>
        <div class="top-name">{{ getName(leaderboard[1]) }}</div>
        <div class="top-score">{{ leaderboard[1].score || 0 }}</div>
      </div>
      <div class="top-item rank-1" @click="scrollToList">
        <div class="rank-badge gold">1</div>
        <div class="top-avatar">{{ getAvatar(leaderboard[0]) }}</div>
        <div class="top-name">{{ getName(leaderboard[0]) }}</div>
        <div class="top-score">{{ leaderboard[0].score || 0 }}</div>
      </div>
      <div class="top-item rank-3" @click="scrollToList">
        <div class="rank-badge bronze">3</div>
        <div class="top-avatar">{{ getAvatar(leaderboard[2]) }}</div>
        <div class="top-name">{{ getName(leaderboard[2]) }}</div>
        <div class="top-score">{{ leaderboard[2].score || 0 }}</div>
      </div>
    </div>

    <!-- 完整排行表格 -->
    <el-card class="list-card">
      <template #header>
        <span>完整排行</span>
      </template>
      <el-table :data="leaderboard" v-loading="loading" stripe style="width: 100%">
        <el-table-column type="index" label="排名" width="80" align="center">
          <template #default="{ $index }">
            <span v-if="$index < 3" class="top-rank-icon">
              {{ ['🥇', '🥈', '🥉'][$index] }}
            </span>
            <span v-else>{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户/队伍" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="user-avatar" v-if="row.username || row.teamName">{{ getInitial(row) }}</span>
              <span>{{ getName(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="积分/胜率" width="150" align="center">
          <template #default="{ row }">
            <span class="score-value">{{ row.score || 0 }}</span>
            <span v-if="[2, 5].includes(currentType)">%</span>
          </template>
        </el-table-column>
        <el-table-column prop="rankNum" label="排名" width="100" align="center" />
        <el-table-column label="更新时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatTime(row.updatedAt) }}
          </template>
        </el-table-column>
      </el-table>
      <div v-if="leaderboard.length === 0 && !loading" class="empty-tip">
        暂无排行数据
      </div>
    </el-card>

    <!-- 赛季历史 -->
    <el-card class="season-card">
      <template #header>
        <div class="season-header">
          <span>赛季历史</span>
          <div class="season-query">
            <el-input v-model="seasonKey" placeholder="赛季标识，如 2024S01" style="width:180px" @keyup.enter="loadSeason" />
            <el-button type="primary" size="small" @click="loadSeason">查询</el-button>
          </div>
        </div>
      </template>
      <el-table :data="seasonRecords" v-loading="seasonLoading" stripe size="small">
        <el-table-column type="index" label="序号" width="80" align="center" />
        <el-table-column label="用户/队伍" min-width="180">
          <template #default="{ row }">
            {{ row.nickname || row.username || row.teamName || ('#' + (row.userId || row.teamId)) }}
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分数" width="120" align="center" />
        <el-table-column prop="rankNum" label="排名" width="100" align="center" />
        <el-table-column label="赛季" width="120" align="center">
          <template #default="{ row }">{{ row.seasonKey || seasonKey }}</template>
        </el-table-column>
      </el-table>
      <div v-if="seasonRecords.length === 0 && !seasonLoading" class="empty-tip">
        输入赛季标识查询历史排名（需管理员先归档赛季）
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getLeaderboard, getSeasonRecords, refreshLeaderboard, archiveSeason } from '@/api/pk'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

const userStore = useUserStore()

const loading = ref(false)
const currentType = ref(1)
const currentPeriod = ref(0)
const leaderboard = ref<any[]>([])

function formatTime(time: string) {
  return time ? dayjs(time).format('MM-DD HH:mm') : '--'
}

function getName(row: any) {
  return row.nickname || row.username || row.teamName || '--'
}

function getAvatar(row: any) {
  return (row.nickname || row.username || row.teamName || '?').charAt(0).toUpperCase()
}

function getInitial(row: any) {
  return getName(row).charAt(0).toUpperCase()
}

function scrollToList() {
  document.querySelector('.list-card')?.scrollIntoView({ behavior: 'smooth' })
}

async function loadData() {
  loading.value = true
  try {
    const res = await getLeaderboard({
      type: currentType.value,
      period: currentPeriod.value
    })
    leaderboard.value = res.data || []
  } catch { /* ignore */ } finally {
    loading.value = false
  }
}

// 赛季历史
const seasonKey = ref('')
const seasonRecords = ref<any[]>([])
const seasonLoading = ref(false)
async function loadSeason() {
  if (!seasonKey.value.trim()) return
  seasonLoading.value = true
  try {
    const res: any = await getSeasonRecords(seasonKey.value.trim(), currentType.value)
    seasonRecords.value = res.data || []
  } catch { seasonRecords.value = [] } finally { seasonLoading.value = false }
}

// 管理员操作
const refreshing = ref(false)
async function doRefresh() {
  refreshing.value = true
  try {
    await refreshLeaderboard({ leaderboardType: currentType.value, periodType: currentPeriod.value })
    ElMessage.success('榜单已刷新')
    await loadData()
  } catch { /* ignore */ } finally {
    refreshing.value = false
  }
}

async function doArchive() {
  try {
    const { value } = await ElMessageBox.prompt('请输入要归档的赛季标识', '归档赛季', {
      inputPlaceholder: '如 2026S01',
      inputValidator: (v: string) => !!v?.trim() || '赛季标识不能为空',
    })
    await archiveSeason(value.trim())
    ElMessage.success('赛季已归档')
  } catch { /* 取消或失败 */ }
}

onMounted(loadData)
</script>

<style scoped>
.leaderboard-page { padding: 24px; max-width: 1000px; margin: 0 auto; }
.admin-actions { display: flex; gap: 8px; }

.filter-card { margin-bottom: 24px; }
.tab-row { display: flex; flex-direction: column; gap: 16px; }
.tab-group { display: flex; align-items: center; gap: 12px; }
.tab-label { font-size: 14px; color: var(--color-ink-light); font-weight: 500; white-space: nowrap; }

.top-three { display: flex; justify-content: center; align-items: flex-end; gap: 24px; margin-bottom: 32px; min-height: 200px; }
.top-item { text-align: center; cursor: pointer; padding: 20px; border-radius: var(--radius-md); transition: transform 0.3s; }
.top-item:hover { transform: translateY(-4px); }
.rank-1 { background: linear-gradient(180deg, #fff7d6 0%, #fff 100%); box-shadow: 0 4px 20px rgba(255, 193, 7, 0.3); order: 1; }
.rank-2 { background: linear-gradient(180deg, var(--color-rice-border) 0%, #fff 100%); box-shadow: var(--shadow-md); order: 0; }
.rank-3 { background: linear-gradient(180deg, #fff0e6 0%, #fff 100%); box-shadow: 0 4px 16px rgba(205, 127, 50, 0.2); order: 2; }
.rank-badge {
  width: 40px; height: 40px; border-radius: 50%; display: flex;
  align-items: center; justify-content: center; font-size: 20px;
  font-weight: 700; color: #fff; margin: 0 auto 12px;
}
.gold { background: linear-gradient(135deg, #ffc107, #ff9800); }
.silver { background: linear-gradient(135deg, #90a4ae, #78909c); }
.bronze { background: linear-gradient(135deg, #ff8a65, #ff7043); }
.top-avatar {
  width: 56px; height: 56px; border-radius: 50%; margin: 0 auto 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; font-size: 22px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
}
.rank-1 .top-avatar { width: 64px; height: 64px; font-size: 26px; }
.top-name { font-size: 14px; color: var(--color-ink); margin-bottom: 4px; font-weight: 500; }
.top-score { font-size: 18px; color: var(--color-primary); font-weight: 700; }

.list-card { }
.top-rank-icon { font-size: 20px; }
.user-cell { display: flex; align-items: center; gap: 10px; }
.user-avatar {
  width: 36px; height: 36px; border-radius: 50%; background: #f0f2f5;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; color: var(--color-ink-light); font-weight: 500;
}
.score-value { font-weight: 700; font-size: 15px; color: var(--color-primary); }
.empty-tip { text-align: center; color: var(--color-ink-muted); padding: 40px 0; }

.season-card { margin-top: 24px; }
.season-header { display: flex; align-items: center; justify-content: space-between; }
.season-query { display: flex; gap: 8px; }
</style>

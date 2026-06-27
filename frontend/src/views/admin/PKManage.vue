<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>组队PK管理</h2>
      <span class="header-hint">管理所有PK队伍、对战记录与排行榜</span>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <div class="stat-card"><div class="stat-value">{{ teamList.length }}</div><div class="stat-label">队伍总数</div></div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card active"><div class="stat-value">{{ activeCount }}</div><div class="stat-label">活跃队伍</div></div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card warn"><div class="stat-value">{{ totalWins }}</div><div class="stat-label">总胜场</div></div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card danger"><div class="stat-value">{{ disbandedCount }}</div><div class="stat-label">已解散</div></div>
      </el-col>
    </el-row>

    <!-- 队伍列表 -->
    <el-card>
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="搜索队伍名称..." clearable style="width:240px" @keyup.enter="filterTeams" @clear="filterTeams" />
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width:120px" @change="filterTeams">
          <el-option label="正常" :value="1" />
          <el-option label="已解散" :value="0" />
        </el-select>
        <el-button @click="loadTeams">刷新</el-button>
      </div>

      <el-table :data="filteredTeams" v-loading="loading" stripe style="width:100%">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column label="队伍名称" min-width="150">
          <template #default="{ row }">
            <el-button type="primary" link @click="openTeamDetail(row)">{{ row.teamName }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="slogan" label="口号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="captainName" label="队长" width="100" />
        <el-table-column label="成员" width="80" align="center">
          <template #default="{ row }">{{ row.currentMembers || row.memberCount || 0 }}/{{ row.maxMembers || 5 }}</template>
        </el-table-column>
        <el-table-column label="战绩" width="100" align="center">
          <template #default="{ row }">
            <span style="color:#67c23a">{{ row.winCount || 0 }}胜</span>
            <span style="margin:0 4px;color:#ccc">/</span>
            <span style="color:#f56c6c">{{ row.loseCount || 0 }}负</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="积分" width="80" align="center" sortable />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status===1?'success':'info'" size="small" effect="plain">{{ row.status===1?'正常':'已解散' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="155">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openTeamDetail(row)">
              <el-icon><View /></el-icon> 详情
            </el-button>
            <el-button v-if="row.status===1" type="warning" link size="small" @click="openEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-popconfirm
              v-if="row.status===1"
              title="确定解散该队伍？"
              @confirm="handleDismiss(row)"
            >
              <template #reference>
                <el-button type="danger" link size="small">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 队伍详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="'队伍详情 - ' + detailTeam?.teamName" width="700px">
      <template v-if="detailTeam">
        <el-descriptions :column="3" border size="small" style="margin-bottom:16px">
          <el-descriptions-item label="队伍名称">{{ detailTeam.teamName }}</el-descriptions-item>
          <el-descriptions-item label="队长">{{ detailTeam.captainName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="积分">{{ detailTeam.totalScore || 0 }}</el-descriptions-item>
          <el-descriptions-item label="口号" :span="2">{{ detailTeam.slogan || '无' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detailTeam.status===1?'success':'info'" size="small">{{ detailTeam.status===1?'正常':'已解散' }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 成员列表 -->
        <h4 class="section-title">队伍成员 ({{ detailMembers.length }})</h4>
        <el-table :data="detailMembers" size="small" stripe max-height="240" v-if="detailMembers.length>0">
          <el-table-column type="index" label="#" width="40" />
          <el-table-column prop="nickname" label="昵称" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column label="角色" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.role===1?'warning':'info'" size="small" effect="plain">{{ row.role===1?'队长':'队员' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="加入时间" width="150">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ row: member }">
              <el-popconfirm
                v-if="member.role !== 1"
                title="确定踢出该成员？"
                @confirm="handleKickMember(member)"
              >
                <template #reference>
                  <el-button type="danger" link size="small">踢出</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-mini">暂无成员</div>

        <!-- 对战记录 -->
        <h4 class="section-title" style="margin-top:20px">对战记录 ({{ detailMatches.length }})</h4>
        <el-table :data="detailMatches" size="small" stripe max-height="240" v-if="detailMatches.length>0">
          <el-table-column type="index" label="#" width="40" />
          <el-table-column prop="matchNo" label="编号" width="140" />
          <el-table-column label="结果" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="matchResultTag(row,detailTeam)" size="small" effect="plain">
                {{ matchResultLabel(row,detailTeam) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="155">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-mini">暂无对战记录</div>
      </template>
      <div v-else v-loading="detailLoading" style="min-height:120px"></div>
    </el-dialog>

    <!-- 编辑队伍弹窗 -->
    <el-dialog v-model="editVisible" title="编辑队伍" width="420px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="队伍名称" required>
          <el-input v-model="editForm.teamName" placeholder="请输入队伍名称" maxlength="20" />
        </el-form-item>
        <el-form-item label="口号">
          <el-input v-model="editForm.slogan" placeholder="选填" maxlength="50" />
        </el-form-item>
        <el-form-item label="最大人数">
          <el-input-number v-model="editForm.maxMembers" :min="2" :max="50" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { View, Delete, Edit } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const loading = ref(false)
const teamList = ref<any[]>([])
const keyword = ref('')
const statusFilter = ref<number | undefined>(undefined)

// 统计
const activeCount = computed(() => teamList.value.filter(t => t.status === 1).length)
const disbandedCount = computed(() => teamList.value.filter(t => t.status !== 1).length)
const totalWins = computed(() => teamList.value.reduce((s, t) => s + (t.winCount || 0), 0))

const filteredTeams = computed(() => {
  let list = teamList.value
  if (statusFilter.value !== undefined) list = list.filter(t => t.status === statusFilter.value)
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(t => (t.teamName || '').toLowerCase().includes(kw))
  }
  return list
})

function filterTeams() { /* filteredTeams is reactive */ }

// 详情
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailTeam = ref<any>(null)
const detailMembers = ref<any[]>([])
const detailMatches = ref<any[]>([])

function formatTime(t: string) { return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '--' }

function matchResultTag(match: any, team: any): string {
  if (match.winnerId === team.id) return 'success'
  if (match.status === 2) return 'info'
  return 'danger'
}
function matchResultLabel(match: any, team: any): string {
  if (match.status === 0) return '进行中'
  if (match.winnerId === team.id) return '胜'
  if (match.status === 2) return '平局'
  return '负'
}

async function loadTeams() {
  loading.value = true
  try {
    const res = await request.get('/pk/teams')
    teamList.value = res.data || []
  } finally { loading.value = false }
}

async function openTeamDetail(row: any) {
  detailTeam.value = row
  detailVisible.value = true
  detailLoading.value = true
  detailMembers.value = []
  detailMatches.value = []
  try {
    const [membersRes, matchesRes] = await Promise.all([
      request.get(`/pk/team/${row.id}/members`),
      request.get(`/pk/team/${row.id}/matches`)
    ])
    detailMembers.value = membersRes.data || []
    detailMatches.value = matchesRes.data || []
  } catch { /* ignore */ } finally {
    detailLoading.value = false
  }
}

async function handleDismiss(row: any) {
  try {
    await request.delete(`/pk/team/${row.id}`)
    ElMessage.success('已解散')
    loadTeams()
  } catch {
    ElMessage.error('操作失败')
  }
}

// --- 编辑 ---
const editVisible = ref(false)
const saving = ref(false)
const editForm = reactive({ id: 0, teamName: '', slogan: '', maxMembers: 5 })

function openEdit(row: any) {
  editForm.id = row.id
  editForm.teamName = row.teamName || ''
  editForm.slogan = row.slogan || ''
  editForm.maxMembers = row.maxMembers || 5
  editVisible.value = true
}

async function handleSave() {
  if (!editForm.teamName.trim()) { ElMessage.warning('请输入队伍名称'); return }
  saving.value = true
  try {
    await request.put(`/pk/team/${editForm.id}`, {
      teamName: editForm.teamName,
      slogan: editForm.slogan,
      maxMembers: editForm.maxMembers
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    loadTeams()
  } finally { saving.value = false }
}

// --- 踢出成员 ---
async function handleKickMember(member: any) {
  try {
    await request.delete(`/pk/team/${detailTeam.value.id}/member/${member.userId}`)
    ElMessage.success(`已踢出 ${member.nickname || member.username}`)
    detailMembers.value = detailMembers.value.filter((m: any) => m.userId !== member.userId)
    if (detailTeam.value) detailTeam.value.currentMembers = Math.max(0, (detailTeam.value.currentMembers || 1) - 1)
  } catch {}
}

onMounted(loadTeams)
</script>

<style scoped>
.header-hint { font-size: 13px; color: var(--color-ink-muted); }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }

.stats-row { margin-bottom: 16px; }
.stat-card { background: #f5f7fa; border-radius: 10px; padding: 18px 16px; text-align: center; }
.stat-card.active { background: #f0f9eb; }
.stat-card.warn { background: #fdf6ec; }
.stat-card.danger { background: #fef0f0; }
.stat-value { font-size: 26px; font-weight: 700; color: #303133; }
.stat-card.active .stat-value { color: #67c23a; }
.stat-card.warn .stat-value { color: #e6a23c; }
.stat-card.danger .stat-value { color: #f56c6c; }
.stat-label { font-size: 12px; color: #909399; margin-top: 2px; }

.section-title { font-size: 15px; font-weight: 600; color: #303133; margin: 0 0 10px; }
.empty-mini { text-align: center; padding: 20px 0; color: #c0c4cc; font-size: 13px; }
</style>

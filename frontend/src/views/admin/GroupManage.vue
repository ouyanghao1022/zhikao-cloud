<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>👥 学习小组管理</h2>
      <el-button @click="loadAll" :loading="loading"><el-icon><Refresh /></el-icon> 刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- ========== Tab 1: 概览统计 ========== -->
      <el-tab-pane label="📊 概览统计" name="stats">
        <el-row :gutter="16" v-loading="loading">
          <el-col :span="4">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-icon" style="background:#409eff">👥</div>
              <div class="stat-info">
                <div class="stat-num">{{ stats.totalGroups || 0 }}</div>
                <div class="stat-label">小组总数</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-icon" style="background:#67c23a">✅</div>
              <div class="stat-info">
                <div class="stat-num">{{ stats.activeGroups || 0 }}</div>
                <div class="stat-label">正常小组</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-icon" style="background:#909399">🚫</div>
              <div class="stat-info">
                <div class="stat-num">{{ stats.dismissedGroups || 0 }}</div>
                <div class="stat-label">已解散</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-icon" style="background:#e6a23c">🙋</div>
              <div class="stat-info">
                <div class="stat-num">{{ stats.totalMembers || 0 }}</div>
                <div class="stat-label">活跃成员</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-icon" style="background:#f56c6c">📋</div>
              <div class="stat-info">
                <div class="stat-num">{{ stats.activeTasks || 0 }}</div>
                <div class="stat-label">进行中任务</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-icon" style="background:#9c27b0">📎</div>
              <div class="stat-info">
                <div class="stat-num">{{ stats.totalResources || 0 }}</div>
                <div class="stat-label">共享资源</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- ========== Tab 2: 小组管理 ========== -->
      <el-tab-pane label="👥 小组管理" name="groups">
        <div class="filter-bar">
          <el-input v-model="groupFilter.keyword" placeholder="搜索小组名称" clearable style="width:180px" @keyup.enter="loadGroups" />
          <el-select v-model="groupFilter.joinType" placeholder="加入方式" clearable style="width:110px" @change="loadGroups">
            <el-option label="公开" :value="1" />
            <el-option label="审核" :value="2" />
            <el-option label="邀请" :value="3" />
          </el-select>
          <el-select v-model="groupFilter.status" placeholder="状态" clearable style="width:110px" @change="loadGroups">
            <el-option label="正常" :value="1" />
            <el-option label="已解散" :value="0" />
          </el-select>
          <el-button type="primary" @click="loadGroups">搜索</el-button>
        </div>

        <el-table :data="groups" v-loading="loading" stripe>
          <el-table-column prop="groupName" label="小组名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="creatorId" label="创建人ID" width="90" align="center" />
          <el-table-column label="成员" width="90" align="center">
            <template #default="{ row }">{{ row.currentMembers }}/{{ row.maxMembers }}</template>
          </el-table-column>
          <el-table-column prop="activeScore" label="活跃度" width="70" align="center" />
          <el-table-column label="等级" width="60" align="center">
            <template #default="{ row }">Lv.{{ row.level }}</template>
          </el-table-column>
          <el-table-column label="加入方式" width="80" align="center">
            <template #default="{ row }">{{ joinTypeMap[row.joinType] }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status===1?'success':'danger'" size="small">{{ row.status===1?'正常':'已解散' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="150" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="viewMembers(row)">成员</el-button>
              <el-button type="info" link size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button v-if="row.status===1" type="warning" link size="small" @click="toggleStatus(row, 0)">解散</el-button>
              <el-button v-else type="success" link size="small" @click="toggleStatus(row, 1)">恢复</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination v-if="groupTotal>0" v-model:current-page="groupFilter.page" v-model:page-size="groupFilter.size"
          :total="groupTotal" layout="total, prev, pager, next" @current-change="loadGroups"
          style="margin-top:16px;justify-content:flex-end" />

        <!-- 编辑弹窗 -->
        <el-dialog v-model="editDialogVisible" title="编辑小组" width="480px">
          <el-form :model="editingGroup" label-width="80px">
            <el-form-item label="小组名称"><el-input v-model="editingGroup.groupName" /></el-form-item>
            <el-form-item label="简介"><el-input v-model="editingGroup.description" type="textarea" :rows="3" /></el-form-item>
            <el-form-item label="标签"><el-input v-model="editingGroup.tags" placeholder="逗号分隔" /></el-form-item>
            <el-form-item label="加入方式">
              <el-select v-model="editingGroup.joinType" style="width:100%">
                <el-option label="公开" :value="1" />
                <el-option label="审核" :value="2" />
                <el-option label="邀请" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item label="最大人数"><el-input-number v-model="editingGroup.maxMembers" :min="2" :max="999" /></el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="editDialogVisible=false">取消</el-button>
            <el-button type="primary" @click="saveGroup">保存</el-button>
          </template>
        </el-dialog>

        <!-- 成员管理弹窗 -->
        <el-dialog v-model="memberDialogVisible" :title="`成员管理 - ${currentGroup?.groupName||''}`" width="680px">
          <el-table :data="members" size="small" max-height="400">
            <el-table-column prop="userId" label="用户ID" width="80" align="center" />
            <el-table-column label="角色" width="90">
              <template #default="{ row }">
                <el-tag :type="row.role===2?'danger':row.role===1?'warning':''" size="small">
                  {{ roleMap[row.role] }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="contribution" label="贡献度" width="80" align="center" />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status===1?'success':'info'" size="small">{{ row.status===1?'正常':'已退出' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="joinTime" label="加入时间" width="150" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-select v-model="row.role" size="small" style="width:90px" @change="(v:number)=>changeRole(row, v)">
                  <el-option label="成员" :value="0" />
                  <el-option label="管理员" :value="1" />
                  <el-option label="组长" :value="2" :disabled="true" />
                </el-select>
                <el-button v-if="row.status===1 && row.role!==2" type="danger" link size="small" @click="kickMember(row)">踢出</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-dialog>
      </el-tab-pane>

      <!-- ========== Tab 3: 任务管理 ========== -->
      <el-tab-pane label="📋 任务管理" name="tasks">
        <div class="filter-bar">
          <el-input v-model="taskFilter.keyword" placeholder="搜索任务标题" clearable style="width:180px" @keyup.enter="loadTasks" />
          <el-select v-model="taskFilter.status" placeholder="状态" clearable style="width:120px" @change="loadTasks">
            <el-option label="进行中" :value="1" />
            <el-option label="已结束" :value="2" />
            <el-option label="已取消" :value="0" />
          </el-select>
          <el-button type="primary" @click="loadTasks">搜索</el-button>
        </div>

        <el-table :data="tasks" v-loading="loading" stripe>
          <el-table-column prop="taskTitle" label="任务标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="groupName" label="所属小组" width="140" show-overflow-tooltip />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status===1?'success':row.status===2?'info':'danger'" size="small">
                {{ taskStatusMap[row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="completedCount" label="完成人数" width="80" align="center" />
          <el-table-column prop="deadline" label="截止时间" width="150" />
          <el-table-column prop="createdAt" label="发布时间" width="150" />
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button type="danger" link size="small" @click="deleteTask(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination v-if="taskTotal>0" v-model:current-page="taskFilter.page" v-model:page-size="taskFilter.size"
          :total="taskTotal" layout="total, prev, pager, next" @current-change="loadTasks"
          style="margin-top:16px;justify-content:flex-end" />
      </el-tab-pane>

      <!-- ========== Tab 4: 资源管理 ========== -->
      <el-tab-pane label="📎 资源管理" name="resources">
        <div class="filter-bar">
          <el-input v-model="resFilter.keyword" placeholder="搜索文件名" clearable style="width:180px" @keyup.enter="loadResources" />
          <el-button type="primary" @click="loadResources">搜索</el-button>
        </div>

        <el-table :data="resources" v-loading="loading" stripe>
          <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
          <el-table-column prop="groupName" label="所属小组" width="140" show-overflow-tooltip />
          <el-table-column label="大小" width="90" align="center">
            <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column prop="downloadCount" label="下载次数" width="90" align="center" />
          <el-table-column prop="createdAt" label="上传时间" width="150" />
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button type="danger" link size="small" @click="deleteResource(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination v-if="resTotal>0" v-model:current-page="resFilter.page" v-model:page-size="resFilter.size"
          :total="resTotal" layout="total, prev, pager, next" @current-change="loadResources"
          style="margin-top:16px;justify-content:flex-end" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  adminGetGroupStats, adminGetGroupList, adminUpdateGroup, adminToggleGroupStatus,
  adminDismissGroup, adminGetMembers, adminKickMember, adminChangeMemberRole,
  adminGetTasks, adminDeleteTask, adminGetResources, adminDeleteResource,
} from '@/api/group'

const loading = ref(false)
const activeTab = ref('stats')

const stats = ref<any>({})

// 类型化映射，避免字面量对象索引签名报错
const joinTypeMap: Record<number, string> = { 1: '公开', 2: '审核', 3: '邀请' }
const roleMap: Record<number, string> = { 0: '成员', 1: '管理员', 2: '组长' }
const taskStatusMap: Record<number, string> = { 0: '已取消', 1: '进行中', 2: '已结束' }

// 小组
const groups = ref<any[]>([])
const groupTotal = ref(0)
const groupFilter = reactive({ keyword:'', joinType:undefined as number|undefined, status:undefined as number|undefined, page:1, size:10 })
const editDialogVisible = ref(false)
const editingGroup = reactive<any>({})

// 成员
const memberDialogVisible = ref(false)
const currentGroup = ref<any>(null)
const members = ref<any[]>([])

// 任务
const tasks = ref<any[]>([])
const taskTotal = ref(0)
const taskFilter = reactive({ keyword:'', status:undefined as number|undefined, page:1, size:10 })

// 资源
const resources = ref<any[]>([])
const resTotal = ref(0)
const resFilter = reactive({ keyword:'', page:1, size:10 })

async function loadAll() {
  loading.value = true
  try {
    await loadStats()
    if (activeTab.value === 'groups') await loadGroups()
    else if (activeTab.value === 'tasks') await loadTasks()
    else if (activeTab.value === 'resources') await loadResources()
  } finally { loading.value = false }
}

async function loadStats() {
  try { const res = await adminGetGroupStats(); stats.value = res.data || {} } catch {}
}

async function loadGroups() {
  loading.value = true
  try {
    const res = await adminGetGroupList(groupFilter)
    groups.value = res.data?.records || []
    groupTotal.value = res.data?.total || 0
  } finally { loading.value = false }
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await adminGetTasks(taskFilter)
    tasks.value = res.data?.records || []
    taskTotal.value = res.data?.total || 0
  } finally { loading.value = false }
}

async function loadResources() {
  loading.value = true
  try {
    const res = await adminGetResources(resFilter)
    resources.value = res.data?.records || []
    resTotal.value = res.data?.total || 0
  } finally { loading.value = false }
}

function onTabChange(tab: string) {
  if (tab === 'stats') loadStats()
  else if (tab === 'groups') loadGroups()
  else if (tab === 'tasks') loadTasks()
  else if (tab === 'resources') loadResources()
}

// ============ 小组操作 ============
function openEditDialog(row: any) {
  Object.assign(editingGroup, row)
  editDialogVisible.value = true
}

async function saveGroup() {
  try {
    await adminUpdateGroup(editingGroup.id, {
      groupName: editingGroup.groupName,
      description: editingGroup.description,
      tags: editingGroup.tags,
      joinType: editingGroup.joinType,
      maxMembers: editingGroup.maxMembers,
    })
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    loadGroups()
  } catch { ElMessage.error('保存失败') }
}

async function toggleStatus(row: any, newStatus: number) {
  try {
    if (newStatus === 0) {
      // 解散：调用强制解散接口（带通知）
      await ElMessageBox.confirm(
        `确定强制解散「${row.groupName}」？\n\n系统将自动通知该小组所有成员，此操作不可撤销。`,
        '强制解散确认',
        { type: 'warning', confirmButtonText: '确认解散', cancelButtonText: '取消' }
      )
      await adminDismissGroup(row.id)
      ElMessage.success('已强制解散，所有成员已收到通知')
    } else {
      // 恢复
      await ElMessageBox.confirm(`确定恢复「${row.groupName}」？`, '确认', { type: 'info' })
      await adminToggleGroupStatus(row.id, newStatus)
      ElMessage.success('已恢复')
    }
    loadGroups()
  } catch (e: any) {
    if (e?.response?.data?.message) ElMessage.error(e.response.data.message)
  }
}

// ============ 成员管理 ============
async function viewMembers(row: any) {
  currentGroup.value = row
  memberDialogVisible.value = true
  try {
    const res = await adminGetMembers(row.id)
    members.value = res.data || []
  } catch { members.value = [] }
}

async function changeRole(row: any, newRole: number) {
  try {
    await adminChangeMemberRole(row.id, newRole)
    ElMessage.success('角色已修改')
  } catch { ElMessage.error('操作失败') }
}

async function kickMember(row: any) {
  try {
    await ElMessageBox.confirm('确定踢出该成员？', '确认', { type: 'warning' })
    await adminKickMember(row.id)
    ElMessage.success('已移出')
    if (currentGroup.value) {
      const res = await adminGetMembers(currentGroup.value.id)
      members.value = res.data || []
    }
  } catch {}
}

// ============ 任务/资源操作 ============
async function deleteTask(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除任务「${row.taskTitle}」？`, '确认', { type: 'warning' })
    await adminDeleteTask(row.id)
    ElMessage.success('已删除')
    loadTasks()
  } catch {}
}

async function deleteResource(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除资源「${row.fileName}」？`, '确认', { type: 'warning' })
    await adminDeleteResource(row.id)
    ElMessage.success('已删除')
    loadResources()
  } catch {}
}

function formatFileSize(bytes: number): string {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}

onMounted(loadAll)
</script>

<style scoped>
.admin-page { padding: 24px; max-width: 1400px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; }
.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }

.stat-card { display: flex; align-items: center; padding: 16px; }
.stat-icon { width: 48px; height: 48px; border-radius: 8px; }
</style>

<template>
  <div class="group-detail-page" v-loading="loading">
    <div v-if="group" class="group-detail-container">
      <!-- 小组头部 -->
      <div class="group-header-card">
        <div class="group-header-left">
          <div class="group-cover">
            <el-icon :size="56"><UserFilled /></el-icon>
          </div>
          <div class="group-header-info">
            <h1 class="group-header-name">{{ group.groupName }}</h1>
            <p class="group-header-desc" v-if="group.description">{{ group.description }}</p>
            <div class="group-header-meta">
              <el-tag :type="group.joinType === 1 ? 'success' : group.joinType === 2 ? 'warning' : 'info'" size="small">
                {{ joinTypeMap[group.joinType] || '未知' }}
              </el-tag>
              <span>{{ group.currentMembers || 0 }}/{{ group.maxMembers || 0 }} 人</span>
              <span>Lv.{{ group.level || 1 }}</span>
              <span>活跃度 {{ group.activeScore || 0 }}</span>
            </div>
            <div class="group-tags" v-if="group.tags">
              <el-tag v-for="(tag, i) in (group.tags || '').split(',')" :key="i" size="small" type="info">{{ tag.trim() }}</el-tag>
            </div>
          </div>
        </div>
        <div class="group-header-actions">
          <el-button type="primary" @click="handleJoin" :disabled="isMember || group.currentMembers >= group.maxMembers">
            {{ isMember ? '已加入' : group.currentMembers >= group.maxMembers ? '已满员' : '加入小组' }}
          </el-button>
          <el-button v-if="isMember && !isOwner" type="warning" plain @click="handleLeave">退出小组</el-button>
          <el-button v-if="isOwner" type="danger" plain @click="handleDismiss">解散小组</el-button>
        </div>
      </div>

      <!-- Tab 内容区 -->
      <el-tabs v-model="activeTab" class="group-tabs">
        <!-- 成员列表 -->
        <el-tab-pane label="成员" name="members">
          <div class="member-grid">
            <div v-for="m in members" :key="m.id" class="member-card">
              <el-avatar :size="48" :src="m.avatar || undefined">{{ (m.nickname || m.username || '?')[0] }}</el-avatar>
              <div class="member-info">
                <div class="member-name">{{ m.nickname || m.username || ('用户' + m.userId) }}</div>
                <el-tag v-if="m.role === 2" type="danger" size="small">组长</el-tag>
                <el-tag v-else-if="m.role === 1" type="warning" size="small">管理员</el-tag>
                <el-tag v-else size="small">成员</el-tag>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 任务列表 -->
        <el-tab-pane label="任务" name="tasks">
          <div class="tasks-section">
            <div v-if="isOwner" class="task-create-bar">
              <el-button type="primary" size="small" @click="showTaskDialog = true">
                发布任务
              </el-button>
            </div>
            <div v-if="tasks.length > 0" class="task-list">
              <div v-for="t in tasks" :key="t.id" class="task-item">
                <div class="task-header">
                  <h4 class="task-title">{{ t.taskTitle }}</h4>
                  <el-tag :type="t.status === 2 ? 'success' : t.status === 1 ? '' : 'info'" size="small">
                    {{ ['', '进行中', '已完成', '已过期'][t.status] || '未知' }}
                  </el-tag>
                </div>
                <p class="task-content" v-if="t.taskContent">{{ t.taskContent }}</p>
                <div class="task-meta">
                  <span v-if="t.deadline">截止: {{ formatTime(t.deadline) }}</span>
                </div>
              </div>
            </div>
            <div v-else class="empty-tip">暂无任务</div>
          </div>
        </el-tab-pane>

        <!-- 资源列表 -->
        <el-tab-pane label="资源" name="resources">
          <div class="resources-section">
            <div v-if="resources.length > 0" class="resource-list">
              <div v-for="r in resources" :key="r.id" class="resource-item">
                <span class="resource-icon">📄</span>
                <span class="resource-name">{{ r.fileName }}</span>
                <el-button size="small" link type="primary" v-if="r.fileUrl" @click="openUrl(r.fileUrl)">
                  下载
                </el-button>
              </div>
            </div>
            <div v-else class="empty-tip">暂无共享资源</div>
          </div>
        </el-tab-pane>

        <!-- 小组聊天 -->
        <el-tab-pane label="小组聊天" name="chat">
          <div class="chat-section">
            <div class="chat-list" v-if="chatMessages.length > 0">
              <div v-for="msg in chatMessages" :key="msg.id" class="chat-item">
                <el-avatar :size="36" :src="msg.avatar || undefined">{{ (msg.nickname || msg.username || '?')[0] }}</el-avatar>
                <div class="chat-body">
                  <div class="chat-meta">
                    <span class="chat-name">{{ msg.nickname || msg.username || ('用户' + msg.userId) }}</span>
                    <span class="chat-time">{{ formatTime(msg.createdAt) }}</span>
                  </div>
                  <div class="chat-content">{{ msg.content }}</div>
                </div>
              </div>
            </div>
            <div v-else-if="!chatLoading" class="empty-tip">暂无聊天消息，发个消息打个招呼吧</div>
            <div class="chat-editor">
              <el-input
                v-model="chatContent"
                type="textarea"
                :rows="3"
                placeholder="输入消息内容..."
                maxlength="2000"
                show-word-limit
                resize="none"
              />
              <div class="chat-editor-actions">
                <el-button type="primary" :disabled="!chatContent.trim()" :loading="chatSending" @click="sendChat">
                  发送
                </el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- 发布任务弹窗 -->
      <el-dialog v-model="showTaskDialog" title="发布任务" width="480px">
        <el-form :model="taskForm" label-width="80px">
          <el-form-item label="任务标题" required>
            <el-input v-model="taskForm.taskTitle" placeholder="请输入任务标题" maxlength="50" />
          </el-form-item>
          <el-form-item label="任务内容">
            <el-input v-model="taskForm.taskContent" type="textarea" :rows="3" placeholder="请输入任务内容" />
          </el-form-item>
          <el-form-item label="截止日期">
            <el-date-picker
              v-model="taskForm.deadline"
              type="datetime"
              placeholder="选择截止日期"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width:100%"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showTaskDialog = false">取消</el-button>
          <el-button type="primary" :loading="taskSubmitting" @click="submitTask">发布</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getGroupDetail, joinGroup, leaveGroup, dismissGroup, getGroupMembers, getGroupTasks, createGroupTask, getGroupResources, getGroupChat, sendGroupMessage } from '@/api/group'
import { UserFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'

function openUrl(url?: string) { if (url) window.open(url) }

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const activeTab = ref('members')
const group = ref<any>(null)
const members = ref<any[]>([])
const tasks = ref<any[]>([])
const resources = ref<any[]>([])
const showTaskDialog = ref(false)
const taskSubmitting = ref(false)

const chatMessages = ref<any[]>([])
const chatContent = ref('')
const chatLoading = ref(false)
const chatSending = ref(false)

const joinTypeMap: Record<number, string> = { 1: '公开', 2: '审核', 3: '邀请' }

const isOwner = computed(() => {
  // 修复：后端字段是 creatorId 不是 ownerId
  return group.value && group.value.creatorId === userStore.userId
})
const isMember = computed(() => {
  return members.value.some(m => m.userId === userStore.userId)
})

const taskForm = ref({
  taskTitle: '',
  taskContent: '',
  deadline: ''
})

function formatTime(time: string) {
  return time ? dayjs(time).format('MM-DD HH:mm') : '--'
}

async function loadDetail() {
  loading.value = true
  try {
    const groupId = Number(route.params.id)
    const [groupRes, memberRes, taskRes, resourceRes] = await Promise.all([
      getGroupDetail(groupId).catch(() => ({ data: null })),
      getGroupMembers(groupId).catch(() => ({ data: [] })),
      getGroupTasks(groupId).catch(() => ({ data: [] })),
      getGroupResources(groupId).catch(() => ({ data: [] }))
    ])
    group.value = groupRes.data
    members.value = memberRes.data || []
    tasks.value = taskRes.data || []
    resources.value = resourceRes.data || []
  } finally {
    loading.value = false
  }
}

async function handleJoin() {
  try {
    await joinGroup(Number(route.params.id))
    ElMessage.success('加入成功')
    loadDetail()
  } catch { /* handled by interceptor */ }
}

async function handleLeave() {
  try {
    await ElMessageBox.confirm('确定要退出小组吗？', '确认退出', { type: 'warning' })
    await leaveGroup(Number(route.params.id))
    ElMessage.success('已退出')
    loadDetail()
  } catch { /* canceled */ }
}

async function handleDismiss() {
  try {
    await ElMessageBox.confirm(
      '解散后无法恢复，确定要解散小组吗？\n\n注意：如果小组内还有其他成员，请先移除所有成员或转移管理员权限后再解散。',
      '确认解散',
      { type: 'error', confirmButtonText: '确认解散', cancelButtonText: '取消' }
    )
    await dismissGroup(Number(route.params.id))
    ElMessage.success('已解散')
    router.push('/group')
  } catch (e: any) {
    // 后端返回"请先移除所有成员"等错误时展示给用户
    if (e?.response?.data?.message) {
      ElMessage.error(e.response.data.message)
    }
  }
}

async function loadChat(silent = false) {
  const groupId = Number(route.params.id)
  if (!silent) chatLoading.value = true
  try {
    const res: any = await getGroupChat(groupId, { page: 1, size: 50 })
    const list = res.data?.records || res.data?.list || res.data || []
    chatMessages.value = (list as any[]).slice().sort((a, b) => {
      const ta = new Date(a.createdAt).getTime()
      const tb = new Date(b.createdAt).getTime()
      return ta - tb
    })
  } catch { /* ignore */ } finally {
    if (!silent) chatLoading.value = false
  }
}

async function sendChat() {
  if (!chatContent.value.trim() || chatSending.value) return
  chatSending.value = true
  try {
    await sendGroupMessage(Number(route.params.id), {
      content: chatContent.value,
      contentType: 1
    })
    chatContent.value = ''
    ElMessage.success('发送成功')
    await loadChat()
  } catch { /* ignore */ } finally {
    chatSending.value = false
  }
}

async function submitTask() {
  if (!taskForm.value.taskTitle.trim()) {
    ElMessage.warning('请输入任务标题')
    return
  }
  taskSubmitting.value = true
  try {
    await createGroupTask(Number(route.params.id), {
      taskTitle: taskForm.value.taskTitle,
      taskContent: taskForm.value.taskContent || undefined,
      deadline: taskForm.value.deadline || undefined
    })
    ElMessage.success('任务发布成功')
    showTaskDialog.value = false
    taskForm.value = { taskTitle: '', taskContent: '', deadline: '' }
    const res = await getGroupTasks(Number(route.params.id))
    tasks.value = res.data || []
  } finally {
    taskSubmitting.value = false
  }
}

onMounted(loadDetail)

let chatTimer: ReturnType<typeof setInterval> | undefined
function startChatPolling() {
  stopChatPolling()
  chatTimer = setInterval(() => loadChat(true), 5000)
}
function stopChatPolling() {
  if (chatTimer !== undefined) { clearInterval(chatTimer); chatTimer = undefined }
}

watch(activeTab, (v) => {
  if (v === 'chat') {
    if (chatMessages.value.length === 0) loadChat()
    startChatPolling()
  } else {
    stopChatPolling()
  }
})

onUnmounted(stopChatPolling)
</script>

<style scoped>
.group-detail-page { padding: 24px; max-width: 1000px; margin: 0 auto; }

.group-header-card {
  background: var(--color-rice-card); border-radius: var(--radius-md); padding: 28px; margin-bottom: 20px;
  display: flex; justify-content: space-between; align-items: flex-start;
  box-shadow: var(--shadow-sm);
}
.group-header-left { display: flex; gap: 20px; }
.group-cover {
  width: 88px; height: 88px; border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex; align-items: center; justify-content: center; color: rgba(255,255,255,0.6);
  flex-shrink: 0;
}
.group-header-info { flex: 1; min-width: 0; }
.group-header-name { font-size: 22px; color: var(--color-ink); margin: 0 0 8px; }
.group-header-desc { font-size: 14px; color: var(--color-ink-muted); margin: 0 0 12px; }
.group-header-meta { display: flex; gap: 12px; align-items: center; font-size: 13px; color: var(--color-ink-muted); margin-bottom: 10px; }
.group-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.group-header-actions { display: flex; gap: 8px; flex-shrink: 0; }

.group-tabs { background: var(--color-rice-card); border-radius: var(--radius-md); padding: 0 20px 20px; box-shadow: var(--shadow-sm); }
.member-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 12px; }
.member-card {
  display: flex; align-items: center; gap: 10px; padding: 12px;
  border: 1px solid var(--color-rice-border); border-radius: var(--radius-sm);
}
.member-info { }
.member-name { font-size: 14px; color: var(--color-ink); margin-bottom: 4px; }

.tasks-section { }
.task-create-bar { margin-bottom: 16px; }
.task-item { padding: 16px; border: 1px solid var(--color-rice-border); border-radius: var(--radius-sm); margin-bottom: 12px; }
.task-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.task-title { font-size: 15px; color: var(--color-ink); margin: 0; }
.task-content { font-size: 13px; color: var(--color-ink-light); margin-bottom: 8px; }
.task-meta { font-size: 12px; color: var(--color-ink-muted); }

.resources-section { }
.resource-item {
  display: flex; align-items: center; gap: 10px; padding: 12px 16px;
  border: 1px solid var(--color-rice-border); border-radius: var(--radius-sm); margin-bottom: 8px;
}
.resource-icon { font-size: 20px; }
.resource-name { flex: 1; font-size: 14px; color: var(--color-ink); }

.empty-tip { text-align: center; color: var(--color-ink-muted); padding: 40px 0; }

.chat-section { display: flex; flex-direction: column; gap: 16px; padding: 8px 0; }
.chat-list { max-height: 420px; overflow-y: auto; display: flex; flex-direction: column; gap: 12px; }
.chat-item { display: flex; gap: 10px; align-items: flex-start; }
.chat-body { flex: 1; min-width: 0; }
.chat-meta { display: flex; gap: 10px; align-items: center; margin-bottom: 4px; }
.chat-name { font-size: 13px; font-weight: 600; color: var(--color-ink); }
.chat-time { font-size: 12px; color: var(--color-ink-muted); }
.chat-content { font-size: 14px; color: var(--color-ink); line-height: 1.6; word-break: break-word; }
.chat-editor { border-top: 1px solid var(--color-rice-border); padding-top: 12px; }
.chat-editor-actions { display: flex; justify-content: flex-end; margin-top: 8px; }
</style>

<template>
  <div class="layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: isCollapse }">
      <div class="sidebar-brand" @click="router.push('/dashboard')">
        <span class="brand-icon">📚</span>
        <span v-show="!isCollapse" class="brand-text">智考云</span>
      </div>

      <nav class="nav-list">
        <router-link to="/dashboard" class="nav-item" :class="{ active: route.path === '/dashboard' }">
          <span class="nav-icon"><DataBoard /></span>
          <span class="nav-label">首页</span>
        </router-link>

        <!-- 超管 -->
        <template v-if="userStore.isAdmin">
          <div v-show="!isCollapse" class="nav-group-title">管理</div>
          <router-link to="/admin/exams" class="nav-item" :class="{ active: route.path.startsWith('/admin/exams') }">
            <span class="nav-icon"><EditPen /></span><span class="nav-label">考试管理</span>
          </router-link>
          <router-link to="/admin/questions" class="nav-item" :class="{ active: route.path.startsWith('/admin/questions') }">
            <span class="nav-icon"><Notebook /></span><span class="nav-label">题库管理</span>
          </router-link>
          <router-link to="/admin/reports" class="nav-item" :class="{ active: route.path === '/admin/reports' }">
            <span class="nav-icon"><DataAnalysis /></span><span class="nav-label">班级报告</span>
          </router-link>
          <router-link to="/admin/users" class="nav-item" :class="{ active: route.path === '/admin/users' }">
            <span class="nav-icon"><User /></span><span class="nav-label">用户管理</span>
          </router-link>
          <router-link to="/admin/classes" class="nav-item" :class="{ active: route.path === '/admin/classes' }">
            <span class="nav-icon"><OfficeBuilding /></span><span class="nav-label">班级管理</span>
          </router-link>
          <router-link to="/admin/pk" class="nav-item" :class="{ active: route.path === '/admin/pk' }">
            <span class="nav-icon"><Trophy /></span><span class="nav-label">组队PK管理</span>
          </router-link>
          <router-link to="/admin/groups" class="nav-item" :class="{ active: route.path === '/admin/groups' }">
            <span class="nav-icon"><Collection /></span><span class="nav-label">学习小组管理</span>
          </router-link>
          <router-link to="/admin/discuss" class="nav-item" :class="{ active: route.path === '/admin/discuss' }">
            <span class="nav-icon"><ChatDotSquare /></span><span class="nav-label">讨论区管理</span>
          </router-link>
          <router-link to="/admin/logs" class="nav-item" :class="{ active: route.path === '/admin/logs' }">
            <span class="nav-icon"><Notebook /></span><span class="nav-label">系统日志</span>
          </router-link>
        </template>

        <!-- 教师 -->
        <template v-if="userStore.isTeacher && !userStore.isAdmin">
          <div v-show="!isCollapse" class="nav-group-title">管理</div>
          <router-link to="/admin/classes" class="nav-item" :class="{ active: route.path === '/admin/classes' }">
            <span class="nav-icon"><OfficeBuilding /></span><span class="nav-label">班级管理</span>
          </router-link>
          <router-link to="/admin/exams" class="nav-item" :class="{ active: route.path.startsWith('/admin/exams') }">
            <span class="nav-icon"><EditPen /></span><span class="nav-label">考试管理</span>
          </router-link>
          <router-link to="/admin/questions" class="nav-item" :class="{ active: route.path.startsWith('/admin/questions') }">
            <span class="nav-icon"><Notebook /></span><span class="nav-label">题库管理</span>
          </router-link>
          <router-link to="/admin/reports" class="nav-item" :class="{ active: route.path === '/admin/reports' }">
            <span class="nav-icon"><DataAnalysis /></span><span class="nav-label">班级报告</span>
          </router-link>
          <router-link to="/admin/grade" class="nav-item" :class="{ active: route.path === '/admin/grade' }">
            <span class="nav-icon"><EditPen /></span><span class="nav-label">主观题批阅</span>
          </router-link>
        </template>

        <!-- 学生 -->
        <template v-if="!userStore.isTeacher && !userStore.isAdmin">
          <div v-show="!isCollapse" class="nav-group-title">学习</div>
          <router-link to="/exam" class="nav-item" :class="{ active: route.path.startsWith('/exam') && !route.path.startsWith('/exam/take') }">
            <span class="nav-icon"><EditPen /></span><span class="nav-label">参加考试</span>
          </router-link>
          <router-link to="/question" class="nav-item" :class="{ active: route.path.startsWith('/question') }">
            <span class="nav-icon"><Notebook /></span><span class="nav-label">题库练习</span>
          </router-link>
          <router-link to="/wrongbook" class="nav-item" :class="{ active: route.path === '/wrongbook' }">
            <span class="nav-icon"><Collection /></span><span class="nav-label">错题本</span>
          </router-link>
          <router-link to="/favorite" class="nav-item" :class="{ active: route.path === '/favorite' }">
            <span class="nav-icon"><Star /></span><span class="nav-label">收藏夹</span>
          </router-link>
          <router-link to="/report" class="nav-item" :class="{ active: route.path === '/report' }">
            <span class="nav-icon"><TrendCharts /></span><span class="nav-label">学情报告</span>
          </router-link>
          <router-link to="/pk" class="nav-item" :class="{ active: route.path.startsWith('/pk') }">
            <span class="nav-icon"><Trophy /></span><span class="nav-label">组队PK</span>
          </router-link>
          <router-link to="/group" class="nav-item" :class="{ active: route.path.startsWith('/group') }">
            <span class="nav-icon"><UserFilled /></span><span class="nav-label">学习小组</span>
          </router-link>
        </template>

        <div v-show="!isCollapse" class="nav-group-title">其他</div>
        <router-link to="/discuss" class="nav-item" :class="{ active: route.path.startsWith('/discuss') }">
          <span class="nav-icon"><ChatDotSquare /></span><span class="nav-label">讨论区</span>
        </router-link>
        <router-link to="/user/profile" class="nav-item" :class="{ active: route.path === '/user/profile' }">
          <span class="nav-icon"><Setting /></span><span class="nav-label">个人中心</span>
        </router-link>
        <router-link to="/user/notification" class="nav-item" :class="{ active: route.path === '/user/notification' }">
          <span class="nav-icon"><Bell /></span><span class="nav-label">通知设置</span>
        </router-link>
      </nav>
    </aside>

    <!-- 主区域 -->
    <div class="main-area">
      <header class="topbar">
        <div class="topbar-left">
          <button class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" /><Expand v-else />
          </button>
          <span class="topbar-breadcrumb">{{ pageTitle }}</span>
        </div>
        <div class="topbar-right">
          <div class="notify-bell" @click.stop="showNotify = !showNotify">
            <Bell /><span v-if="unreadCount" class="badge">{{ unreadCount }}</span>
          </div>
          <div class="user-chip" @click.stop="showUserMenu = !showUserMenu">
            <div class="avatar">{{ userStore.nickname?.[0] || 'U' }}</div>
            <span class="user-name">{{ userStore.nickname || userStore.username }}</span>
            <span class="role-badge" :class="roleClass">{{ roleText }}</span>
          </div>
          <div v-if="showUserMenu" class="dropdown-menu" @click.stop>
            <div class="dropdown-item" @click="router.push('/user/profile')">个人中心</div>
            <div class="dropdown-item danger" @click="handleLogout">退出登录</div>
          </div>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </div>

    <!-- 通知面板 -->
    <div v-if="showNotify" class="notify-panel" @click.stop>
      <div class="notify-head">
        <strong>消息通知</strong>
        <span class="mark-read" @click="markAllRead">全部已读</span>
      </div>
      <div v-if="!notifications.length" class="notify-empty">暂无通知</div>
      <div v-for="n in notifications" :key="n.id" class="notify-row" :class="{ unread: !n.isRead }" @click="markRead(n)">
        <div class="notify-title"><span class="notify-tag">{{ n.typeText }}</span> {{ n.title }}</div>
        <div class="notify-time">{{ formatTime(n.createdAt) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import { DataBoard, EditPen, Notebook, Collection, Star, TrendCharts, DataAnalysis, ChatDotSquare, Trophy, UserFilled, Setting, User, OfficeBuilding, Fold, Expand, Bell } from '@element-plus/icons-vue'

import { getMessageList, getUnreadCount, markMessageRead, markAllMessagesRead } from '@/api/message'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const showNotify = ref(false)
const showUserMenu = ref(false)

const roleText = computed(() => userStore.isAdmin ? '管理员' : userStore.isTeacher ? '教师' : '学生')
const roleClass = computed(() => userStore.isAdmin ? 'admin' : userStore.isTeacher ? 'teacher' : 'student')

const pageTitle = computed(() => {
  const m: Record<string, string> = {
    '/dashboard': '首页', '/exam': '参加考试', '/wrongbook': '错题本', '/question': '题库练习',
    '/favorite': '收藏夹', '/report': '学情报告', '/discuss': '讨论区', '/pk': '组队PK',
    '/group': '学习小组', '/user/profile': '个人中心', '/admin/users': '用户管理',
    '/admin/classes': '班级管理', '/admin/pk': '组队PK管理', '/admin/groups': '学习小组管理',
    '/admin/exams': '考试管理', '/admin/questions': '题库管理',    '/admin/reports': '班级报告',
    '/admin/discuss': '讨论区管理', '/admin/logs': '系统日志', '/admin/grade': '主观题批阅',
    '/user/notification': '通知设置'
  }
  for (const [k, v] of Object.entries(m)) if (route.path.startsWith(k)) return v
  return ''
})

const notifications = ref<any[]>([])
const unreadCount = ref(0)

async function fetchNotifications() {
  const r = await getMessageList()
  notifications.value = r.data || []
  try {
    const u = await getUnreadCount()
    unreadCount.value = u.data?.count ?? 0
  } catch {}
}

async function markRead(n: any) {
  if (n.isRead) return
  n.isRead = 1
  unreadCount.value = Math.max(0, unreadCount.value - 1)
  try { await markMessageRead(n.id) } catch {}
}

async function markAllRead() {
  notifications.value.forEach(n => n.isRead = 1)
  unreadCount.value = 0
  try { await markAllMessagesRead() } catch {}
}

function formatTime(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = Date.now()
  const diff = now - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 172800000) return '昨天'
  return `${d.getMonth()+1}月${d.getDate()}日`
}

function closePopups(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.user-chip') && !target.closest('.dropdown-menu')) showUserMenu.value = false
  if (!target.closest('.notify-bell') && !target.closest('.notify-panel')) showNotify.value = false
}
onMounted(() => {
  document.addEventListener('click', closePopups)
  fetchNotifications()
  setInterval(fetchNotifications, 30000)
})
onUnmounted(() => document.removeEventListener('click', closePopups))

async function handleLogout() {
  try { await ElMessageBox.confirm('确定退出？', '提示', { type: 'warning' }); userStore.logout(); router.push('/auth/login') } catch {}
}
</script>

<style scoped>
.layout { display: flex; height: 100vh; background: #f5f6fa; font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif; }

/* Sidebar */
.sidebar {
  width: 220px; background: #fff; border-right: 1px solid #eef0f5; display: flex; flex-direction: column;
  transition: width .25s; flex-shrink: 0; z-index: 10;
}
.sidebar.collapsed { width: 64px; }
.sidebar-brand { display: flex; align-items: center; gap: 10px; padding: 20px 18px 16px; cursor: pointer; }
.sidebar-brand .brand-icon { font-size: 22px; }
.sidebar-brand .brand-text { font-size: 18px; font-weight: 700; color: #1a1a2e; }

.nav-list { flex: 1; overflow-y: auto; padding: 0 12px 20px; }
.nav-group-title { font-size: 11px; font-weight: 600; color: #adb5bd; text-transform: uppercase; letter-spacing: 1px; padding: 16px 12px 6px; }
.nav-item {
  display: flex; align-items: center; gap: 10px; padding: 10px 12px; border-radius: 8px;
  color: #495057; text-decoration: none; font-size: 14px; margin-bottom: 2px;
  transition: all .15s;
}
.nav-item:hover { background: #f1f3f5; color: #212529; }
.nav-item.active { background: #e7f5ff; color: #1971c2; font-weight: 600; }
.nav-icon { font-size: 18px; flex-shrink: 0; width: 20px; text-align: center; }
.nav-label { white-space: nowrap; overflow: hidden; }

/* Main */
.main-area { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.topbar {
  height: 56px; background: #fff; border-bottom: 1px solid #eef0f5;
  display: flex; align-items: center; justify-content: space-between; padding: 0 24px; flex-shrink: 0;
}
.topbar-left { display: flex; align-items: center; gap: 16px; }
.collapse-btn { border: none; background: none; cursor: pointer; padding: 4px; color: #868e96; font-size: 18px; border-radius: 6px; }
.collapse-btn:hover { background: #f1f3f5; }
.topbar-breadcrumb { font-size: 15px; font-weight: 600; color: #212529; }
.topbar-right { display: flex; align-items: center; gap: 16px; position: relative; }

.notify-bell { position: relative; cursor: pointer; color: #212529; font-size: 20px; padding: 4px; border-radius: 6px; }
.notify-bell:hover { background: #f1f3f5; }
.notify-bell .badge { position: absolute; top: 0; right: 0; width: 16px; height: 16px; background: #e03131; color: #fff; font-size: 10px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }

.user-chip { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 4px 8px; border-radius: 8px; }
.user-chip:hover { background: #f1f3f5; }
.avatar { width: 30px; height: 30px; border-radius: 50%; background: linear-gradient(135deg, #4dabf7, #1971c2); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 600; }
.user-name { font-size: 13px; color: #495057; font-weight: 500; }
.role-badge { font-size: 11px; padding: 2px 8px; border-radius: 10px; font-weight: 600; }
.role-badge.admin { background: #ffe3e3; color: #e03131; }
.role-badge.teacher { background: #fff3bf; color: #e67700; }
.role-badge.student { background: #d3f9d8; color: #2b8a3e; }

.dropdown-menu { position: absolute; top: 48px; right: 0; background: #fff; border-radius: 10px; box-shadow: 0 8px 30px rgba(0,0,0,.12); min-width: 140px; z-index: 200; overflow: hidden; }
.dropdown-item { padding: 10px 16px; cursor: pointer; font-size: 13px; color: #495057; }
.dropdown-item:hover { background: #f1f3f5; }
.dropdown-item.danger { color: #e03131; }

.content { flex: 1; overflow-y: auto; }

/* Notify panel */
.notify-panel { position: fixed; top: 56px; right: 24px; width: 340px; background: #fff; border-radius: 12px; box-shadow: 0 8px 30px rgba(0,0,0,.15); z-index: 300; max-height: 420px; overflow-y: auto; }
.notify-head { display: flex; justify-content: space-between; padding: 14px 16px 10px; border-bottom: 1px solid #f1f3f5; font-size: 14px; }
.mark-read { color: #1971c2; cursor: pointer; font-size: 12px; }
.notify-empty { text-align: center; padding: 40px 0; color: #adb5bd; font-size: 13px; }
.notify-row { padding: 12px 16px; border-bottom: 1px solid #f8f9fa; cursor: pointer; }
.notify-row.unread { background: #f1f9ff; }
.notify-row .notify-title { font-size: 13px; color: #212529; }
.notify-row .notify-tag {
  font-size: 11px; padding: 1px 6px; border-radius: 4px;
  background: #e7f5ff; color: #1971c2; margin-right: 6px; font-weight: 500;
}
.notify-row .notify-time { font-size: 11px; color: #adb5bd; margin-top: 4px; }
</style>

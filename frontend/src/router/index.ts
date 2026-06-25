import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  // 认证路由（无需登录）
  {
    path: '/auth',
    children: [
      { path: 'login', component: () => import('@/views/auth/Login.vue') }
    ]
  },
  // 主布局路由（需要登录）
  {
    path: '/',
    component: () => import('@/components/layout/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: 'dashboard', component: () => import('@/views/dashboard/Dashboard.vue') },
      // 考试中心
      { path: 'exam', component: () => import('@/views/exam/ExamList.vue') },
      { path: 'exam/:id', component: () => import('@/views/exam/ExamDetail.vue') },
      { path: 'exam/take/:id', component: () => import('@/views/exam/ExamTaking.vue') },
      // 题库练习
      { path: 'question', component: () => import('@/views/question/QuestionBank.vue') },
      // 错题本
      { path: 'wrongbook', component: () => import('@/views/wrongbook/WrongBook.vue') },
      // 个人中心
      { path: 'user/profile', component: () => import('@/views/user/Profile.vue') },
      { path: 'user/notification', component: () => import('@/views/user/NotificationSetting.vue') },
      // 收藏夹
      { path: 'favorite', component: () => import('@/views/favorite/Favorite.vue') },
      // 学情报告
      { path: 'report', component: () => import('@/views/report/PersonalReport.vue') },
      // 讨论区 — 精确路径必须放在参数路径前面
      { path: 'discuss', component: () => import('@/views/discuss/SectionList.vue') },
      { path: 'discuss/create', component: () => import('@/views/discuss/PostEditor.vue') },
      { path: 'discuss/posts', component: () => import('@/views/discuss/PostList.vue') },
      { path: 'discuss/edit/:postId', component: () => import('@/views/discuss/PostEditor.vue') },
      { path: 'discuss/post/:postId', component: () => import('@/views/discuss/PostDetail.vue') },
      { path: 'discuss/:sectionId', component: () => import('@/views/discuss/PostList.vue') },
      // 学习小组
      { path: 'group', component: () => import('@/views/group/GroupDiscovery.vue') },
      { path: 'group/:id', component: () => import('@/views/group/GroupDetail.vue') },
      // 组队PK
      { path: 'pk', component: () => import('@/views/pk/PKLobby.vue') },
      { path: 'pk/match/:id', component: () => import('@/views/pk/PkMatch.vue') },
      { path: 'pk/leaderboard', component: () => import('@/views/pk/Leaderboard.vue') },
      // 教师/管理员路由
      { path: 'admin/exams', component: () => import('@/views/admin/ExamManage.vue'), meta: { requiresTeacher: true } },
      { path: 'admin/questions', component: () => import('@/views/admin/QuestionManage.vue'), meta: { requiresTeacher: true } },
      { path: 'admin/reports', component: () => import('@/views/admin/ClassReport.vue'), meta: { requiresTeacher: true } },
      { path: 'admin/grade', component: () => import('@/views/exam/ExamGrade.vue'), meta: { requiresTeacher: true } },
      // 超管路由
      { path: 'admin/users', component: () => import('@/views/admin/UserManage.vue'), meta: { requiresAdmin: true } },
      { path: 'admin/logs', component: () => import('@/views/admin/SysLogManage.vue'), meta: { requiresAdmin: true } },
      { path: 'admin/classes', component: () => import('@/views/admin/ClassManage.vue'), meta: { requiresTeacher: true } },
      { path: 'admin/pk', component: () => import('@/views/admin/PKManage.vue'), meta: { requiresAdmin: true } },
      { path: 'admin/groups', component: () => import('@/views/admin/GroupManage.vue'), meta: { requiresAdmin: true } },
      { path: 'admin/discuss', component: () => import('@/views/admin/DiscussManage.vue'), meta: { requiresAdmin: true } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/auth/login')
  } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/dashboard')
  } else if (to.meta.requiresTeacher && !userStore.isTeacher) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, register } from '@/api/auth'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
  const userId = ref<number | null>(null)
  const username = ref('')
  const nickname = ref('')
  const avatar = ref('')
  const role = ref('')
  const accessToken = ref('')
  const refreshTokenVal = ref('')

  const isLoggedIn = computed(() => !!accessToken.value)
  const isTeacher = computed(() => ['TEACHER', 'SUPER_ADMIN'].includes(role.value))
  const isAdmin = computed(() => role.value === 'SUPER_ADMIN')

  // 从localStorage恢复登录状态
  function restoreLoginState() {
    const token = localStorage.getItem('access_token')
    const rToken = localStorage.getItem('refresh_token')
    const storedUser = localStorage.getItem('user_info')

    if (token && storedUser) {
      accessToken.value = token
      refreshTokenVal.value = rToken || ''
      const user = JSON.parse(storedUser)
      userId.value = user.userId
      username.value = user.username
      nickname.value = user.nickname
      avatar.value = user.avatar
      role.value = user.role
    }
  }

  // 登录
  async function handleLogin(account: string, password: string) {
    try {
      const res = await login({ account, password })
      if (res.code === 200) {
        setUserInfo(res.data)
        ElMessage.success('登录成功')
        return true
      }
      ElMessage.error(res.message)
      return false
    } catch (err: any) {
      ElMessage.error(err.response?.data?.message || '登录失败')
      return false
    }
  }

  // 注册
  async function handleRegister(data: any) {
    try {
      const res = await register(data)
      if (res.code === 200) {
        ElMessage.success('注册成功，请登录')
        return true
      }
      ElMessage.error(res.message)
      return false
    } catch (err: any) {
      ElMessage.error(err.response?.data?.message || '注册失败')
      return false
    }
  }

  // 登出
  function logout() {
    accessToken.value = ''
    refreshTokenVal.value = ''
    userId.value = null
    username.value = ''
    nickname.value = ''
    avatar.value = ''
    role.value = ''
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    localStorage.removeItem('user_info')
  }

  // 设置用户信息（内部方法）
  function setUserInfo(data: any) {
    accessToken.value = data.accessToken
    refreshTokenVal.value = data.refreshToken
    userId.value = data.userId
    username.value = data.username
    nickname.value = data.nickname
    avatar.value = data.avatar
    role.value = data.role

    localStorage.setItem('access_token', data.accessToken)
    localStorage.setItem('refresh_token', data.refreshToken)
    localStorage.setItem('user_info', JSON.stringify({
      userId: data.userId,
      username: data.username,
      nickname: data.nickname,
      avatar: data.avatar,
      role: data.role
    }))
  }

  return {
    userId, username, nickname, avatar, role,
    accessToken, refreshToken: refreshTokenVal,
    isLoggedIn, isTeacher, isAdmin,
    restoreLoginState, handleLogin, handleRegister, logout
  }
})

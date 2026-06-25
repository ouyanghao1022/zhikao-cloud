import axios, { type InternalAxiosRequestConfig } from 'axios'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/result'

// 开发环境通过 Vite proxy(/api) → localhost:8080/api/v1
const baseURL = '/api/v1'

const request: any = axios.create({
  baseURL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器
request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const userStore = useUserStore()
  if (userStore.accessToken) {
    config.headers = config.headers || {}
    config.headers['Authorization'] = `Bearer ${userStore.accessToken}`
  }
  return config
})

// 响应拦截器
request.interceptors.response.use(
  (response: any) => {
    const res = response.data as Result<any>
    if (res.code === 200) {
      return res as any
    } else if (res.code === 401 && !isAuthUrl(response.config.url || '')) {
      return handleTokenRefresh(response.config)
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
  },
  (error: any) => {
    if (error.response?.status === 401) {
      const msg = error.response?.data?.message || '身份过期，请重新登陆！'
      ElMessage.error(msg)
      useUserStore().logout()
      window.location.href = '/auth/login'
      return Promise.reject(error)
    }
    const msg = error.response?.data?.message || error.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

/** auth相关URL不触发token刷新 */
function isAuthUrl(url: string): boolean {
  return url.includes('/auth/')
}

// Token刷新处理
let isRefreshing = false
let refreshQueue: Array<(token: string) => void> = []

async function handleTokenRefresh(config: any) {
  if (!isRefreshing) {
    isRefreshing = true
    try {
      const userStore = useUserStore()
      const res = await axios.post(`${baseURL}/auth/refresh?refreshToken=${encodeURIComponent(userStore.refreshToken)}`)
      if (res.data.code === 200) {
        userStore.accessToken = res.data.data.accessToken
        userStore.refreshToken = res.data.data.refreshToken
        localStorage.setItem('access_token', res.data.data.accessToken)
        localStorage.setItem('refresh_token', res.data.data.refreshToken)
        refreshQueue.forEach(cb => cb(res.data.data.accessToken))
        refreshQueue = []
        config.headers['Authorization'] = `Bearer ${res.data.data.accessToken}`
        return request(config)
      }
    } catch {
      useUserStore().logout()
      window.location.href = '/auth/login'
    } finally {
      isRefreshing = false
    }
  } else {
    return new Promise(resolve => {
      refreshQueue.push((token: string) => {
        config.headers['Authorization'] = `Bearer ${token}`
        resolve(request(config))
      })
    })
  }
}

// 拦截器已解包为 Result，request 声明为 any 让调用方 r.code/r.data 合法
export default request

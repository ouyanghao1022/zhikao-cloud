import request from '@/utils/request'
import type { Result } from '@/types/result'

export interface LoginDTO {
  account: string
  password: string
  captcha?: string
}

export interface RegisterDTO {
  username: string
  password: string
  confirmPassword: string
  email: string
  phone: string
  nickname: string
}

export interface LoginVO {
  userId: number
  username: string
  nickname: string
  avatar: string
  role: string
  accessToken: string
  refreshToken: string
  accessTokenExpire: number
  refreshTokenExpire: number
}

/**
 * 用户登录
 */
export function login(data: LoginDTO): Promise<Result<LoginVO>> {
  return request.post('/auth/login', data)
}

/**
 * 用户注册
 */
export function register(data: RegisterDTO): Promise<Result<null>> {
  return request.post('/auth/register', data)
}

/**
 * 刷新Token
 */
export function refresh(data: { refreshToken: string }): Promise<Result<LoginVO>> {
  return request.post('/auth/refresh', data)
}

/**
 * 获取当前用户信息
 */
export function getCurrentUser(): Promise<Result<any>> {
  return request.get('/auth/me')
}

/**
 * 退出登录
 */
export function logout(): Promise<Result<null>> {
  return request.post('/auth/logout')
}

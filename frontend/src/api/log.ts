import request from '@/utils/request'

/** 操作日志（管理员） */
export function getOperLogs(params: { page?: number; size?: number; username?: string; status?: number }) {
  return request.get('/log/oper', { params })
}

/** 登录日志（管理员） */
export function getLoginLogs(params: { page?: number; size?: number; username?: string; status?: number }) {
  return request.get('/log/login', { params })
}

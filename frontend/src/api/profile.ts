import request from '@/utils/request'

// ==================== 用户资料/等级/积分（新增） ====================

/** 用户资料+等级+积分 */
export function getPointsProfile() {
  return request.get('/points/profile')
}

/** 等级配置 */
export function getLevelConfig() {
  return request.get('/points/level-config')
}

/** 积分明细 */
export function getIntegralLogs(params: { page?: number; size?: number }) {
  return request.get('/points/integral-logs', { params })
}

/**
 * 更新用户资料
 */
export function updateProfile(data: Record<string, any>) {
  return request.put('/user/profile', data)
}

/**
 * 修改密码
 */
export function changePassword(data: { oldPassword: string; newPassword: string; confirmPassword: string }) {
  return request.put('/user/password', data)
}

/**
 * 获取当前用户考试统计（学生）
 */
export function getMyStats() {
  return request.get('/user/my-stats')
}

/**
 * 获取个人中心概览（按角色返回不同数据）
 */
export function getMyDashboard() {
  return request.get('/user/my-dashboard')
}

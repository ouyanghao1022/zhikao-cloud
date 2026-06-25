import request from '@/utils/request'

// ==================== 小组聊天（新增） ====================

/** 获取小组聊天记录 */
export function getGroupChat(groupId: number, params?: { page?: number; size?: number }) {
  return request.get(`/group/${groupId}/chat`, { params })
}

/** 发送小组消息 */
export function sendGroupMessage(groupId: number, data: { content: string; contentType?: number; fileUrl?: string }) {
  return request.post(`/group/${groupId}/chat`, data)
}

/**
 * 获取小组列表
 */
export function getGroupList(params: {
  page?: number
  size?: number
  keyword?: string
  joinType?: number
}) {
  return request.get('/group/list', { params })
}

/**
 * 获取小组详情
 */
export function getGroupDetail(id: number) {
  return request.get(`/group/${id}`)
}

/**
 * 创建小组
 */
export function createGroup(data: {
  groupName: string
  description?: string
  tags?: string
  joinType?: number
  maxMembers?: number
}) {
  return request.post('/group', data)
}

/**
 * 加入小组
 */
export function joinGroup(id: number) {
  return request.post(`/group/${id}/join`)
}

/**
 * 退出小组
 */
export function leaveGroup(id: number) {
  return request.post(`/group/${id}/leave`)
}

/**
 * 解散小组（组长/管理员）
 */
export function dismissGroup(id: number) {
  return request.delete(`/group/${id}`)
}

/**
 * 获取成员列表
 */
export function getGroupMembers(id: number) {
  return request.get(`/group/${id}/members`)
}

/**
 * 发布任务
 */
export function createGroupTask(id: number, data: {
  taskTitle: string
  taskContent?: string
  deadline?: string
}) {
  return request.post(`/group/${id}/task`, data)
}

/**
 * 获取任务列表
 */
export function getGroupTasks(id: number, status?: number) {
  return request.get(`/group/${id}/tasks`, { params: { status } })
}

/**
 * 获取资源列表
 */
export function getGroupResources(id: number) {
  return request.get(`/group/${id}/resources`)
}

// ==================== 管理端接口 ====================

export function adminGetGroupStats() {
  return request.get('/group/admin/stats')
}

export function adminGetGroupList(params: {
  page?: number; size?: number; keyword?: string; joinType?: number; status?: number
}) {
  return request.get('/group/admin/list', { params })
}

export function adminUpdateGroup(id: number, data: Partial<{
  groupName: string; description: string; tags: string; joinType: number; maxMembers: number; coverUrl: string
}>) {
  return request.put(`/group/admin/${id}`, data)
}

export function adminToggleGroupStatus(id: number, status: number) {
  return request.put(`/group/admin/${id}/status`, { status })
}

export function adminDismissGroup(id: number) {
  return request.delete(`/group/admin/${id}`)
}

export function adminGetMembers(groupId: number, status?: number) {
  return request.get(`/group/admin/${groupId}/members`, { params: { status } })
}

export function adminKickMember(memberId: number) {
  return request.delete(`/group/admin/member/${memberId}`)
}

export function adminChangeMemberRole(memberId: number, role: number) {
  return request.put(`/group/admin/member/${memberId}/role`, { role })
}

export function adminGetTasks(params: {
  page?: number; size?: number; groupId?: number; status?: number; keyword?: string
}) {
  return request.get('/group/admin/tasks', { params })
}

export function adminDeleteTask(id: number) {
  return request.delete(`/group/admin/task/${id}`)
}

export function adminGetResources(params: {
  page?: number; size?: number; groupId?: number; keyword?: string
}) {
  return request.get('/group/admin/resources', { params })
}

export function adminDeleteResource(id: number) {
  return request.delete(`/group/admin/resource/${id}`)
}

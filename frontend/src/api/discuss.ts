import request from '@/utils/request'

// ==================== 关注（新增） ====================

/** 关注/取关用户 */
export function toggleFollow(followUserId: number) {
  return request.post('/discuss/follow', { followUserId })
}

/** 我关注的人 */
export function getFollowings() {
  return request.get('/discuss/followings')
}

/** 关注我的人 */
export function getFollowers() {
  return request.get('/discuss/followers')
}

/**
 * 获取版块列表
 */
export function getSections() {
  return request.get('/discuss/sections')
}

/**
 * 获取帖子列表
 */
export function getPosts(params: {
  sectionId?: number
  keyword?: string
  page?: number
  size?: number
}) {
  return request.get('/discuss/posts', { params })
}

/**
 * 获取帖子详情
 */
export function getPostDetail(id: number) {
  return request.get(`/discuss/post/${id}`)
}

/**
 * 发帖
 */
export function createPost(data: {
  sectionId: number
  title: string
  content: string
  tags?: string
  contentType?: number
}) {
  return request.post('/discuss/post', data)
}

/**
 * 编辑帖子
 */
export function updatePost(id: number, data: {
  title: string
  content: string
  tags?: string
}) {
  return request.put(`/discuss/post/${id}`, data)
}

/**
 * 删除帖子
 */
export function deletePost(id: number) {
  return request.delete(`/discuss/post/${id}`)
}

/**
 * 点赞/取消点赞
 */
export function toggleLike(data: {
  targetType: number
  targetId: number
}) {
  return request.post('/discuss/like', data)
}

/**
 * 获取评论列表
 */
export function getComments(postId: number, params?: {
  page?: number
  size?: number
}) {
  return request.get(`/discuss/comments/${postId}`, { params })
}

/**
 * 发表评论
 */
export function addComment(data: {
  postId: number
  content: string
  parentId?: number
  replyToUserId?: number
}) {
  return request.post('/discuss/comment', data)
}

/**
 * 置顶/取消置顶（教师/管理员）
 */
export function toggleTop(postId: number, isTop: boolean) {
  return request.put(`/discuss/post/${postId}/top`, { isTop })
}

/**
 * 加精/取消加精（教师/管理员）
 */
export function toggleEssence(postId: number, isEssence: boolean) {
  return request.put(`/discuss/post/${postId}/essence`, { isEssence })
}

// ==================== 版块管理 ====================

export function createSection(data: {
  sectionName: string
  description?: string
  sectionType?: number
  icon?: string
  sort?: number
  status?: number
}) {
  return request.post('/discuss/section', data)
}

export function updateSection(id: number, data: Partial<{
  sectionName: string
  description: string
  sectionType: number
  icon: string
  sort: number
  status: number
}>) {
  return request.put(`/discuss/section/${id}`, data)
}

export function deleteSection(id: number) {
  return request.delete(`/discuss/section/${id}`)
}

// ==================== 管理端接口 ====================

/** 管理端：讨论区统计概览 */
export function getDiscussStats() {
  return request.get('/discuss/admin/stats')
}

/** 管理端：帖子列表（含已删除） */
export function adminGetPosts(params: {
  sectionId?: number
  keyword?: string
  status?: number
  isTop?: number
  isEssence?: number
  auditStatus?: number
  page?: number
  size?: number
}) {
  return request.get('/discuss/admin/posts', { params })
}

/** 管理端：评论列表（跨帖子） */
export function adminGetComments(params: {
  keyword?: string
  status?: number
  postId?: number
  page?: number
  size?: number
}) {
  return request.get('/discuss/admin/comments', { params })
}

/** 管理端：审核帖子 */
export function auditPost(postId: number, data: { isAuditPassed: number; auditMsg?: string }) {
  return request.put(`/discuss/admin/post/${postId}/audit`, data)
}

/** 管理端：恢复已删除帖子 */
export function restorePost(postId: number) {
  return request.put(`/discuss/admin/post/${postId}/restore`)
}

/** 管理端：切换版块状态 */
export function toggleSectionStatus(id: number, status: number) {
  return request.put(`/discuss/admin/section/${id}/status`, { status })
}

/** 管理端：批量删除帖子 */
export function batchDeletePosts(ids: number[]) {
  return request.delete('/discuss/admin/posts/batch', { data: { ids } })
}

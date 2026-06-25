import request from '@/utils/request'

// ==================== 标签/解析/收藏（新增） ====================

/** 标签列表 */
export function getQuestionTags(keyword?: string) {
  return request.get('/question/tags', { params: { keyword } })
}

/** 给题目打标签 */
export function tagQuestion(id: number, tagIds: number[]) {
  return request.post(`/question/${id}/tags`, { tagIds })
}

/** 取消标签 */
export function untagQuestion(id: number, tagId: number) {
  return request.delete(`/question/${id}/tags/${tagId}`)
}

/** 获取题目已打的标签 */
export function getTagsOfQuestion(id: number) {
  return request.get(`/question/${id}/tags`)
}

/** 获取题目解析 */
export function getQuestionAnalysis(id: number) {
  return request.get(`/question/${id}/analysis`)
}

/** 保存题目解析 */
export function saveQuestionAnalysis(id: number, data: { textAnalysis?: string; videoUrl?: string; knowledgePoints?: string }) {
  return request.post(`/question/${id}/analysis`, data)
}

/** 收藏题目 */
export function favoriteQuestion(data: { questionId: number; note?: string }) {
  return request.post('/question/favorite', data)
}

/** 我的收藏题目 */
export function getMyQuestionFavorites(params: { page?: number; size?: number }) {
  return request.get('/question/favorites', { params })
}

/** 取消收藏 */
export function unfavoriteQuestion(questionId: number) {
  return request.delete(`/question/favorite/${questionId}`)
}

/**
 * 获取题目列表
 */
export function getQuestionList(params: {
  current?: number
  size?: number
  categoryId?: number
  questionType?: number
  difficulty?: number
  keyword?: string
  creatorIds?: string
}) {
  return request.get('/question/list', { params })
}

/**
 * 获取题目详情
 */
export function getQuestionDetail(id: number) {
  return request.get(`/question/detail/${id}`)
}

/**
 * 创建题目（教师）
 */
export function createQuestion(data: any) {
  return request.post('/question/create', data)
}

/**
 * 更新题目（教师）
 */
export function updateQuestion(data: any) {
  return request.put('/question/update', data)
}

/**
 * 删除题目（教师）
 */
export function deleteQuestion(id: number) {
  return request.delete(`/question/delete/${id}`)
}

/**
 * 获取题库分类树
 */
export function getCategoryTree() {
  return request.get('/question/category/tree')
}

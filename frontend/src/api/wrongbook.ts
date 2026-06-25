import request from '@/utils/request'

// ==================== 错题复习与标签（新增） ====================

/** 今日复习计划 */
export function getTodayReviewPlan() {
  return request.get('/wrong-notebook/review/today')
}

/** 标记错题复习完成 */
export function markWrongNoteReviewed(noteId: number) {
  return request.post(`/wrong-notebook/review/${noteId}/done`)
}

/** 给错题打标签 */
export function tagWrongNote(noteId: number, tags: string[]) {
  return request.post(`/wrong-notebook/${noteId}/tags`, { tags })
}

/** 查询错题标签 */
export function getWrongNoteTags(noteId: number) {
  return request.get(`/wrong-notebook/${noteId}/tags`)
}

/**
 * 获取错题本列表
 */
export function getWrongBookList(params?: any) {
  return request.get('/wrong-notebook/list', { params })
}

/**
 * 更新掌握状态
 */
export function updateMasterStatus(id: number, masterStatus: number) {
  return request.put(`/wrong-notebook/master/${id}`, { masterStatus })
}

/**
 * 删除错题
 */
export function deleteWrongNote(id: number) {
  return request.delete(`/wrong-notebook/${id}`)
}

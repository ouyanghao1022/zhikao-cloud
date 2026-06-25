import request from '@/utils/request'

export interface FavoriteFolder {
  id: number
  userId: number
  folderName: string
  description: string
  isPublic: number
  sort: number
  createdAt: string
  updatedAt: string
}

export interface FavoriteItem {
  id: number
  userId: number
  folderId: number
  itemType: number
  itemId: number | null
  itemTitle: string
  itemUrl: string
  note: string
  createdAt: string
}

/** 获取收藏夹列表 */
export function getFavoriteFolders() {
  return request.get('/favorite/folders')
}

/** 创建收藏夹 */
export function createFavoriteFolder(data: { folderName: string; description?: string }) {
  return request.post('/favorite/folder', data)
}

/** 更新收藏夹 */
export function updateFavoriteFolder(id: number, data: { folderName: string; description?: string }) {
  return request.put(`/favorite/folder/${id}`, data)
}

/** 删除收藏夹 */
export function deleteFavoriteFolder(id: number) {
  return request.delete(`/favorite/folder/${id}`)
}

/** 获取收藏项列表 */
export function getFavoriteItems(params: { folderId?: number; current?: number; size?: number }) {
  return request.get('/favorite/items', { params })
}

/** 添加收藏 */
export function addFavoriteItem(data: {
  folderId: number
  itemType: number
  itemId?: number | null
  itemTitle?: string
  itemUrl?: string
  note?: string
}) {
  return request.post('/favorite/item', data)
}

/** 取消收藏 */
export function removeFavoriteItem(id: number) {
  return request.delete(`/favorite/item/${id}`)
}

/** 更新收藏笔记 */
export function updateFavoriteNote(id: number, note: string) {
  return request.put(`/favorite/item/${id}/note`, { note })
}

/** 检查题目是否已收藏 */
export function checkQuestionFavorite(questionId: number) {
  return request.get(`/favorite/question/${questionId}`)
}

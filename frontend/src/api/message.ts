import request from '@/utils/request'

/** 消息列表 */
export function getMessageList() {
  return request.get('/messages/list')
}

/** 未读消息数 */
export function getUnreadCount() {
  return request.get('/messages/unread-count')
}

/** 标记单条已读 */
export function markMessageRead(id: number) {
  return request.put(`/messages/read/${id}`)
}

/** 标记全部已读 */
export function markAllMessagesRead() {
  return request.put('/messages/read-all')
}

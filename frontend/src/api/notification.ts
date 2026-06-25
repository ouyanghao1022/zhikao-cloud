import request from '@/utils/request'

/** 获取通知设置 */
export function getNotificationSetting() {
  return request.get('/notification/setting')
}

/** 更新通知设置 */
export function updateNotificationSetting(data: Record<string, number>) {
  return request.put('/notification/setting', data)
}

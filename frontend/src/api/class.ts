import request from '@/utils/request'

/**
 * 班级相关 API
 */

/** 创建班级 */
export function createClass(data: {
  className: string
  school?: string
  grade?: string
  description?: string
  maxStudents?: number
}) {
  return request.post('/class/create', data)
}

/** 通过口令加入班级 */
export function joinClass(classCode: string) {
  return request.post('/class/join', { classCode })
}

/** 获取我的班级列表 */
export function getMyClasses() {
  return request.get('/class/my')
}

/** 获取班级成员列表 */
export function getClassMembers(classId: number) {
  return request.get(`/class/members/${classId}`)
}

/** 退出当前班级（学生） */
export function leaveClass() {
  return request.delete('/class/leave')
}

/** 解散班级 */
export function dismissClass(classId: number) {
  return request.delete(`/class/${classId}`)
}

/** 教师移除班级学生 */
export function removeStudent(classId: number, userId: number) {
  return request.delete(`/class/members/${classId}/user/${userId}`)
}

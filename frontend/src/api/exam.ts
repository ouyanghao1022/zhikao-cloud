import request from '@/utils/request'

// ==================== 主观题批阅与考试监控（新增） ====================

/** 待批阅主观题列表（教师） */
export function getPendingGrade(paperId: number) {
  return request.get('/exam/grade/pending', { params: { paperId } })
}

/** 某次考试答题详情（教师批阅页） */
export function getGradeSessionDetail(sessionId: number) {
  return request.get(`/exam/grade/session/${sessionId}`)
}

/** 批阅单题 */
export function gradeAnswer(data: { answerId: number; score: number; feedback?: string }) {
  return request.post('/exam/grade/answer', data)
}

/** 上报防作弊事件 */
export function reportMonitorEvent(data: { sessionId: number; eventType: number; detail?: string; riskLevel?: number }) {
  return request.post('/exam/monitor/event', data)
}

/**
 * 获取试卷列表
 */
export function getExamList(params: {
  current?: number
  size?: number
  status?: number
  categoryId?: number
}) {
  return request.get('/exam/list', { params })
}

/**
 * 获取试卷详情
 */
export function getExamDetail(id: number) {
  return request.get(`/exam/detail/${id}`)
}

/**
 * 获取考试答题数据
 */
export function takeExam(id: number) {
  return request.get(`/exam/take/${id}`)
}

/**
 * 提交考试答案
 */
export function submitExam(data: {
  paperId: number
  answers: Array<{ questionId: number; answer: string }>
}) {
  return request.post('/exam/submit', data)
}

/**
 * 获取我的考试记录
 */
export function getMyRecords(params: { current?: number; size?: number }) {
  return request.get('/exam/my-records', { params })
}

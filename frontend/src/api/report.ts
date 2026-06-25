import request from '@/utils/request'

/** 获取个人学情报告（含雷达图 + 趋势 + 概览） */
export function getPersonalReport() {
  return request.get('/report/personal')
}

/** 获取考试趋势 */
export function getExamTrend() {
  return request.get('/report/exam-trend')
}

/** 获取知识点热力图 */
export function getKnowledgeHeatmap() {
  return request.get('/report/knowledge-heatmap')
}

/** 获取薄弱知识点 */
export function getWeakAreas() {
  return request.get('/report/weak-areas')
}

/** 获取班级报告（教师端，按班级过滤） */
export function getClassReport(paperId: number, classId?: number) {
  const params: any = {}
  if (classId) params.classId = classId
  return request.get(`/report/class/${paperId}`, { params })
}

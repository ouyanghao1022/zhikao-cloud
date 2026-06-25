/**
 * 统一API响应类型
 */
export interface Result<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

/**
 * 分页响应类型
 */
export interface PageResult<T = any> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 题目类型
 */
export interface Question {
  id: number
  categoryId: number
  questionType: number
  difficulty: number
  title: string
  content?: string
  answer?: string
  answerAnalysis?: string
  score: number
  options?: QuestionOption[]
}

/**
 * 题目选项类型
 */
export interface QuestionOption {
  id: number
  questionId: number
  optionLabel: string
  optionContent: string
  isCorrect: number
  sort: number
}

/**
 * 试卷类型
 */
export interface ExamPaper {
  id: number
  title: string
  description?: string
  totalScore: number
  passScore?: number
  duration?: number
  startTime?: string
  endTime?: string
  status: number
  questionCount?: number
}

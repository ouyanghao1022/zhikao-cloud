import request from '@/utils/request'

// ==================== 排行榜刷新与赛季（新增，管理员） ====================

/** 刷新排行榜（管理员） */
export function refreshLeaderboard(data: { leaderboardType: number; periodType?: number }) {
  return request.post('/leaderboard/refresh', data)
}

/** 归档赛季（管理员） */
export function archiveSeason(seasonKey: string) {
  return request.post('/leaderboard/archive', { seasonKey })
}

/** 赛季历史记录 */
export function getSeasonRecords(seasonKey: string, leaderboardType?: number) {
  return request.get(`/leaderboard/season/${seasonKey}`, { params: { leaderboardType } })
}

/**
 * 获取队伍列表
 */
export function getTeams() {
  return request.get('/pk/teams')
}

/**
 * 创建队伍
 */
export function createTeam(data: {
  teamName: string
  slogan?: string
  maxMembers?: number
  joinType?: number
}) {
  return request.post('/pk/team', data)
}

/**
 * 加入队伍
 */
export function joinTeam(teamId: number) {
  return request.post('/pk/team/join', { teamId })
}

/**
 * 退出队伍
 */
export function leaveTeam(teamId: number) {
  return request.post('/pk/team/leave', { teamId })
}

/**
 * 发起对战
 */
export function startMatch(data: {
  matchType?: number
  teamBId?: number
  paperId?: number
  totalRounds?: number
}) {
  return request.post('/pk/match/start', data)
}

/**
 * 获取对战详情
 */
export function getMatchDetail(id: number) {
  return request.get(`/pk/match/${id}`)
}

/**
 * 获取对战答题记录
 */
export function getMatchRecords(id: number) {
  return request.get(`/pk/match/${id}/records`)
}

/**
 * 结束对战
 */
export function endMatch(id: number) {
  return request.post(`/pk/match/${id}/end`)
}

/**
 * 提交对战答案
 */
export function submitAnswer(data: {
  matchId: number
  teamId?: number
  roundNum: number
  questionId: number
  answer: string
  answerTimeMs: number
}) {
  return request.post('/pk/match/answer', data)
}

/**
 * 获取当前用户的队伍
 */
export function getMyTeam() {
  return request.get('/pk/teams/my')
}

/**
 * 获取排行榜
 */
export function getLeaderboard(params: {
  type?: number
  period?: number
}) {
  return request.get('/pk/leaderboard', { params })
}

/**
 * 获取周榜Top10
 */
export function getLeaderboardTop() {
  return request.get('/pk/leaderboard/top')
}

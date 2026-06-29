import request from '@/utils/request'

/** 颁发证书 */
export function issueCertificate(sessionId: number) {
  return request.post(`/cert/issue/${sessionId}`)
}

/** 我的证书列表 */
export function getMyCertificates() {
  return request.get('/cert/my')
}

/** 查询某次考试证书 */
export function getCertBySession(sessionId: number) {
  return request.get(`/cert/${sessionId}`)
}

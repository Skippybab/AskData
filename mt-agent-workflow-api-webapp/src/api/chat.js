import request from './request.js'

export const chatApi = {
  getSessions(params) {
    return request({ url: '/api/chat/sessions', method: 'get', params })
  },
  createSession(data) {
    return request({ url: '/api/chat/sessions', method: 'post', data })
  },
  getMessages(sessionId) {
    return request({ url: `/api/chat/sessions/${sessionId}/messages`, method: 'get' })
  },

  getUserTools() {
    return request({ url: '/api/chat/user-tools', method: 'get' })
  },
  updateSessionTitle(sessionId, title) {
    return request({ url: `/api/chat/sessions/${sessionId}/title`, method: 'put', data: { title } })
  },
  deleteSession(sessionId) {
    return request({ url: `/api/chat/sessions/${sessionId}`, method: 'delete' })
  }
}



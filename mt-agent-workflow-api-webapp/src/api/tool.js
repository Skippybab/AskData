import request from './request.js'

export const toolApi = {
  getToolList(params) {
    return request({ url: '/tool/list', method: 'get', params })
  },
  addTool(data) {
    return request({ url: '/tool/add', method: 'post', data })
  },
  updateTool(data) {
    return request({ url: '/tool/update', method: 'put', data })
  },
  deleteTool(id) {
    return request({ url: `/tool/${id}`, method: 'delete' })
  },
  getToolById(id) {
    return request({ url: `/tool/${id}`, method: 'get' })
  }
}



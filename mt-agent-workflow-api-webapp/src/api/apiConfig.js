import request from './request.js'

export const apiConfigApi = {
  // 分页查询API配置
  getApiConfigPage(params) {
    return request({
      url: '/api/api-config/page',
      method: 'get',
      params
    })
  },

  // 获取用户的所有API配置
  getUserApiConfigs() {
    return request({
      url: '/api/api-config/list',
      method: 'get'
    })
  },

  // 获取API配置详情
  getApiConfig(id) {
    return request({
      url: `/api/api-config/${id}`,
      method: 'get'
    })
  },

  // 创建API配置
  createApiConfig(data) {
    return request({
      url: '/api/api-config',
      method: 'post',
      data
    })
  },

  // 更新API配置
  updateApiConfig(id, data) {
    return request({
      url: `/api/api-config/${id}`,
      method: 'put',
      data
    })
  },

  // 删除API配置
  deleteApiConfig(id) {
    return request({
      url: `/api/api-config/${id}`,
      method: 'delete'
    })
  },

  // 切换API状态
  toggleApiStatus(id) {
    return request({
      url: `/api/api-config/${id}/toggle-status`,
      method: 'put'
    })
  },

  // 重新生成API密钥
  regenerateApiKey(id) {
    return request({
      url: `/api/api-config/${id}/regenerate-key`,
      method: 'post'
    })
  }
}
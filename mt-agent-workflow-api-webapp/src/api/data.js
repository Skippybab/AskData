import request from './request.js'

export const dataApi = {
  // 业务说明
  getBusinessConfig() {
    return request({ url: '/data/business', method: 'get' })
  },
  saveBusinessConfig(data) {
    return request({ url: '/data/business', method: 'post', data })
  },

  // 数据库配置
  getDbConfig() {
    return request({ url: '/data/db/config', method: 'get' })
  },
  saveDbConfig(data) {
    return request({ url: '/data/db/config', method: 'post', data })
  },
  testDbConnection(data) {
    return request({ url: '/data/db/test', method: 'post', data })
  },
  fetchDbTables() {
    return request({ url: '/data/db/tables', method: 'get' })
  },
  fetchTableColumns(params) {
    return request({ url: '/data/db/columns', method: 'get', params })
  },

  // 数据源配置
  getDataSource() {
    return request({ url: '/data/source', method: 'get' })
  },
  saveDataSource(data) {
    return request({ url: '/data/source', method: 'post', data })
  },

  // 系统工具
  getToolOptions() {
    return request({ url: '/data/tools/options', method: 'get' })
  },
  getSelectedTools() {
    return request({ url: '/data/tools', method: 'get' })
  },
  saveSelectedTools(data) {
    return request({ url: '/data/tools', method: 'post', data })
  },

  // 我的接口
  listApis(params) {
    return request({ url: '/data/apis', method: 'get', params })
  },
  generateApi(data) {
    return request({ url: '/data/apis/generate', method: 'post', data })
  },
  toggleApi(id, enabled) {
    return request({ url: `/data/apis/${id}/toggle`, method: 'post', data: { enabled } })
  },
  deleteApi(id) {
    return request({ url: `/data/apis/${id}`, method: 'delete' })
  }
}

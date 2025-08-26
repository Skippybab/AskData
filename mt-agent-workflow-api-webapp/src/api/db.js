import request from './request.js'

export const dbApi = {
  // 保存/更新外库配置（后端读取 rawPassword 并加密存储）
  saveConfig(data) {
    // 期望 data: { id?, name, dbType:'mysql', host, port, databaseName, username, rawPassword }
    return request({ url: '/api/db/config', method: 'post', data })
  },
  // 获取单个配置
  getConfig(id) {
    return request({ url: `/api/db/config/${id}`, method: 'get' })
  },
  // 分页查询外库配置列表
  listConfigs(params) {
    return request({ url: '/api/db/configs', method: 'get', params })
  },
  // 验证连接
  verifyConfig(id) {
    return request({ url: `/api/db/config/${id}/verify`, method: 'post' })
  },
  // 触发同步（同步完成后可查询 status/tables/columns）
  startSync(dbConfigId) {
    return request({ url: `/api/db/schema/${dbConfigId}/sync`, method: 'post' })
  },
  // 查询最新版本同步状态
  getSchemaStatus(dbConfigId) {
    return request({ url: `/api/db/schema/${dbConfigId}/status`, method: 'get' })
  },
  // 查询表清单（allowed 可选 0/1）
  listTables(dbConfigId, allowed) {
    return request({ url: `/api/db/schema/${dbConfigId}/tables`, method: 'get', params: { allowed } })
  },
  // 查询列清单
  listColumns(dbConfigId, tableId) {
    return request({ url: `/api/db/schema/${dbConfigId}/tables/${tableId}/columns`, method: 'get' })
  },
  // 删除配置
  deleteConfig(id) {
    return request({ url: `/api/db/config/${id}`, method: 'delete' })
  },
  
  // 更新配置状态
  updateConfigStatus(id, data) {
    return request({ url: `/api/db/config/${id}/status`, method: 'put', data })
  },
  
  // 重新加密密码
  reEncryptPassword(id, password) {
    return request({ url: `/api/db/config/${id}/re-encrypt`, method: 'post', data: { password } })
  }
}



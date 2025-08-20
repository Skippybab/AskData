import request from './request'

// API配置管理
export const apiConfigApi = {
  // 获取API列表
  list: () => request.get('/api/api-config/list'),
  
  // 创建API
  create: (data) => request.post('/api/api-config/create', data),
  
  // 更新API状态
  updateStatus: (id, status) => request.put(`/api/api-config/status/${id}`, { status }),
  
  // 重新生成密钥
  regenerateKey: (id) => request.post(`/api/api-config/regenerate-key/${id}`),
  
  // 删除API
  delete: (id) => request.delete(`/api/api-config/${id}`)
}

// 数据库配置管理
export const dbConfigApi = {
  // 获取数据库列表
  list: (params) => request.get('/api/db/configs', { params }),
  
  // 获取启用的数据库
  getEnabled: () => request.get('/api/db/configs/enabled'),
  
  // 创建或更新数据库配置
  save: (data) => request.post('/api/db/config', data),
  
  // 删除数据库配置
  delete: (id) => request.delete(`/api/db/config/${id}`),
  
  // 更新状态
  updateStatus: (id, status) => request.put(`/api/db/config/${id}/status`, { status }),
  
  // 测试连接
  testConnection: (data) => request.post('/api/db/config/test', data),
  
  // 同步表结构
  syncSchema: (id) => request.post(`/api/db/schema/${id}/sync`)
}

// 表结构管理
export const schemaApi = {
  // 获取表列表
  getTables: (dbConfigId) => request.get('/api/db/schema/tables', { params: { dbConfigId } }),
  
  // 更新表注释
  updateTableComment: (tableId, comment) => request.put(`/api/db/schema/table/${tableId}/comment`, { comment }),
  
  // 更新表访问权限
  updateTableAccess: (tableId, enabled) => request.put(`/api/db/schema/table/${tableId}/access`, { enabled })
}

// 聊天接口
export const chatApi = {
  // 创建会话
  createSession: (data) => request.post('/api/chat/session/create', data),
  
  // 发送查询
  query: (data) => request.post('/api/chat/query', data),
  
  // 获取历史记录
  getHistory: (sessionId) => request.get(`/api/chat/history/${sessionId}`)
}

// 工具管理（预留）
export const toolApi = {
  // 获取工具列表
  list: () => request.get('/api/tools/list'),
  
  // 更新工具状态
  updateStatus: (id, enabled) => request.put(`/api/tools/${id}/status`, { enabled }),
  
  // 更新工具参数
  updateParams: (id, params) => request.put(`/api/tools/${id}/params`, params)
}

export default {
  apiConfig: apiConfigApi,
  dbConfig: dbConfigApi,
  schema: schemaApi,
  chat: chatApi,
  tool: toolApi
}
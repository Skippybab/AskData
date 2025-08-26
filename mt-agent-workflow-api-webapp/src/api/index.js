import request from './request'

// API配置管理
export const apiConfigApi = {
  // 获取API列表（改为使用分页接口）
  list: (params = { current: 1, size: 100 }) => request.get('/api/api-config/page', { params }),
  
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
  testConnection: (data) => {
    // 如果有ID，使用验证接口；否则先保存再验证
    if (data.id) {
      return request.post(`/api/db/config/${data.id}/verify`)
    } else {
      // 对于新配置，先保存再验证
      return request.post('/api/db/config', data).then(res => {
        if (res.data && res.data.id) {
          return request.post(`/api/db/config/${res.data.id}/verify`)
        }
        throw new Error('保存配置失败')
      })
    }
  },
  
  // 同步表结构
  syncSchema: (id) => request.post(`/api/db/schema/${id}/sync`)
}

// 表结构管理
export const schemaApi = {
  // 获取表列表
  getTables: (dbConfigId) => request.get(`/api/db/schema/${dbConfigId}/tables`),
  
  // 获取启用的表列表
  getEnabledTables: (dbConfigId) => request.get(`/api/db/schema/${dbConfigId}/tables/enabled`),
  
  // 更新表注释
  updateTableComment: (tableId, comment) => request.put(`/api/db/schema/table/${tableId}/comment`, { comment }),
  

  
  // 设置表启用状态
  setTableEnabled: (dbConfigId, tableId, enabled) => request.put(`/api/db/schema/${dbConfigId}/tables/${tableId}/enabled`, { enabled })
}

// 表信息管理
export const tableInfoApi = {
  // 获取表列表
  getTableList: (dbConfigId) => request.get(`/api/table-info/list`, { params: { dbConfigId } }),
  
  // 获取表详情
  getTableDetail: (dbConfigId, tableId) => request.get(`/api/table-info/detail`, { params: { dbConfigId, tableId } }),
  
  // 获取表字段信息
  getTableColumns: (dbConfigId, tableId) => request.get(`/api/table-info/columns`, { params: { dbConfigId, tableId } }),
  
  // 更新字段备注
  updateColumnComment: (dbConfigId, tableId, columnName, comment) => 
    request.put(`/api/table-info/columns/comment`, null, { 
      params: { dbConfigId, tableId, columnName, comment }
    })
}

// 聊天接口
export const chatApi = {
  // 创建会话
  createSession: (data) => request.post('/api/chat/sessions', data),
  
  // 获取会话列表
  getSessions: (params = { current: 1, size: 20 }) => request.get('/api/chat/sessions', { params }),
  
  // 获取会话消息
  getMessages: (sessionId) => request.get(`/api/chat/sessions/${sessionId}/messages`),
  
  // 重命名会话
  renameSession: (sessionId, title) => request.put(`/api/chat/sessions/${sessionId}/title`, { title }),
  
  // 删除会话
  deleteSession: (sessionId) => request.delete(`/api/chat/sessions/${sessionId}`),
  
  // 获取用户工具
  getUserTools: () => request.get('/api/chat/user-tools'),
  
  // 测试Dify连接
  testDify: () => request.get('/api/chat/test-dify')
}

// 数据问答接口
export const dataQuestionApi = {
  // 数据问答
  ask: (data) => request.post('/api/data-question/ask', data),
  
  // 获取问答历史
  getHistory: (sessionId) => request.get(`/api/data-question/history/${sessionId}`)
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
  tableInfo: tableInfoApi,
  chat: chatApi,
  dataQuestion: dataQuestionApi,
  tool: toolApi
}
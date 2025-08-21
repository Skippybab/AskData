import axios from 'axios'

// API基础URL
const BASE_URL = process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080'

// 创建axios实例
const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 240000, // 4分钟超时
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
apiClient.interceptors.request.use(
  config => {
    // 可以在这里添加token等认证信息
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
apiClient.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('响应错误:', error)
    if (error.response) {
      // 服务器返回错误
      const { status, data } = error.response
      if (status === 401) {
        // 未授权，跳转登录
        window.location.href = '/login'
      } else if (status === 500) {
        console.error('服务器错误:', data.msg || '服务器内部错误')
      }
    } else if (error.request) {
      // 请求已发送但没有收到响应
      console.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

/**
 * 数据问答API
 */
export const dataQuestionApi = {
  /**
   * 发送数据问答请求
   * @param {Object} params 请求参数
   * @param {Number} params.sessionId 会话ID
   * @param {String} params.question 用户问题
   * @param {Number} params.dbConfigId 数据库配置ID
   * @param {Number|String} params.tableId 表ID或表名（可选）
   * @returns {Promise} 返回查询结果
   */
  async ask(params) {
    try {
      const response = await apiClient.post('/api/data-question/ask', params)
      
      // 处理响应
      if (response.code === 200 && response.data) {
        return {
          success: true,
          data: response.data
        }
      } else {
        return {
          success: false,
          error: response.msg || '查询失败'
        }
      }
    } catch (error) {
      console.error('数据问答请求失败:', error)
      return {
        success: false,
        error: error.message || '请求失败'
      }
    }
  },

  /**
   * 健康检查
   * @returns {Promise} 返回服务状态
   */
  async health() {
    try {
      const response = await apiClient.get('/api/data-question/health')
      return response
    } catch (error) {
      console.error('健康检查失败:', error)
      return null
    }
  }
}

/**
 * 会话管理API
 */
export const sessionApi = {
  /**
   * 创建新会话
   * @param {Object} params 会话参数
   * @returns {Promise} 返回会话信息
   */
  async create(params) {
    return apiClient.post('/api/chat/sessions', params)
  },

  /**
   * 获取会话列表
   * @param {Number} page 页码
   * @param {Number} size 每页数量
   * @returns {Promise} 返回会话列表
   */
  async list(page = 1, size = 20) {
    return apiClient.get('/api/chat/sessions', {
      params: { current: page, size }
    })
  },

  /**
   * 获取会话消息历史
   * @param {Number} sessionId 会话ID
   * @returns {Promise} 返回消息列表
   */
  async getMessages(sessionId) {
    return apiClient.get(`/api/chat/sessions/${sessionId}/messages`)
  }
}

/**
 * 数据库配置API
 */
export const dbConfigApi = {
  /**
   * 获取数据库配置列表
   * @returns {Promise} 返回配置列表
   */
  async list() {
    return apiClient.get('/api/db-config/list')
  },

  /**
   * 获取数据库的表列表
   * @param {Number} dbConfigId 数据库配置ID
   * @returns {Promise} 返回表列表
   */
  async getTables(dbConfigId) {
    return apiClient.get(`/api/table-info/list`, {
      params: { dbConfigId }
    })
  }
}

export default {
  dataQuestionApi,
  sessionApi,
  dbConfigApi
}
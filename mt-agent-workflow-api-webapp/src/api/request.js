import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: 'http://localhost:8080', // 后端服务地址
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    // 过滤掉无效的token值
    if (token && token !== 'null' && token !== 'undefined') {
      config.headers['Authorization'] = `Bearer ${token}`
    } else {
      // 如果没有有效token，不添加Authorization头
      // 后端会使用默认用户
      delete config.headers['Authorization']
    }
    
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const { data } = response
    
    if (data.code === 200) {
      return data
    } else {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || '请求失败'))
    }
  },
  error => {
    console.error('响应错误:', error)
    if (error.code === 'NETWORK_ERROR' || error.message.includes('Network Error')) {
      ElMessage.error('网络连接失败，请检查后端服务是否启动')
    } else if (error.response?.status === 404) {
      ElMessage.error('请求的接口不存在')
    } else if (error.response?.status >= 500) {
      ElMessage.error('服务器内部错误')
    } else {
      ElMessage.error(error.response?.data?.message || '网络请求失败')
    }
    return Promise.reject(error)
  }
)

export default request
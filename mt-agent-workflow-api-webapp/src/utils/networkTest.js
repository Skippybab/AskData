/**
 * 网络连接测试工具
 */

// 测试后端服务是否可访问
export const testBackendConnection = async () => {
  try {
    const response = await fetch('/api/user/list?current=1&size=1', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    if (response.ok) {
      return { success: true, message: '后端服务连接正常' }
    } else {
      return { success: false, message: `后端服务响应异常: ${response.status}` }
    }
  } catch (error) {
    console.error('后端连接测试失败:', error)
    return { 
      success: false, 
      message: '无法连接到后端服务，请检查：\n1. 后端服务是否启动\n2. 端口8080是否被占用\n3. 防火墙设置' 
    }
  }
}

// 测试API路径是否正确
export const testApiPaths = async () => {
  const testPaths = [
    '/api/user/list',
    '/api/db/configs', 
    '/api/chat/sessions'
  ]
  
  const results = []
  
  for (const path of testPaths) {
    try {
      const response = await fetch(path, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      })
      
      results.push({
        path,
        status: response.status,
        ok: response.ok
      })
    } catch (error) {
      results.push({
        path,
        status: 'ERROR',
        ok: false,
        error: error.message
      })
    }
  }
  
  return results
}

// 检查前端配置
export const checkFrontendConfig = () => {
  const config = {
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    port: import.meta.env.VITE_PORT || 3000,
    proxy: true
  }
  
  return config
}

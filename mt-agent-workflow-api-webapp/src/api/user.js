import request from './request.js'

export const userApi = {
  // 用户登录
  login(data) {
    return request({
      url: '/api/user/login',
      method: 'post',
      data
    }).then(response => {
      // 保存token和用户信息
      if (response.data && response.data.token) {
        localStorage.setItem('token', response.data.token)
        localStorage.setItem('user', JSON.stringify(response.data.user))
      }
      return response
    })
  },

  // 用户登出
  logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  },

  // 获取当前用户信息
  getCurrentUser() {
    const userStr = localStorage.getItem('user')
    return userStr ? JSON.parse(userStr) : null
  },

  // 检查是否已登录
  isLoggedIn() {
    return !!localStorage.getItem('token')
  },

  // 获取用户列表
  getUserList(params) {
    return request({
      url: '/api/user/list',
      method: 'get',
      params
    })
  },

  // 添加用户
  addUser(data) {
    return request({
      url: '/api/user/add',
      method: 'post',
      data
    })
  },

  // 更新用户
  updateUser(data) {
    return request({
      url: '/api/user/update',
      method: 'put',
      data
    })
  },

  // 删除用户
  deleteUser(id) {
    return request({
      url: `/api/user/${id}`,
      method: 'delete'
    })
  },

  // 更新用户状态
  updateUserStatus(params) {
    return request({
      url: '/api/user/status',
      method: 'put',
      params
    })
  },

  // 根据ID获取用户
  getUserById(id) {
    return request({
      url: `/api/user/${id}`,
      method: 'get'
    })
  }
}
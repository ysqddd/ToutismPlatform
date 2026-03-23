import axios from 'axios'

const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

apiClient.interceptors.request.use(
  config => {
    const currentPath = window.location.pathname
    const isAdminPath = currentPath.startsWith('/admin')
    
    const adminToken = localStorage.getItem('adminToken')
    const userToken = localStorage.getItem('token')
    
    let token = null
    if (isAdminPath) {
      token = adminToken
    } else {
      token = userToken
    }
    
    console.log('=== Axios 请求拦截器 ===')
    console.log('请求 URL:', config.url)
    console.log('当前路径:', currentPath)
    console.log('是否管理员路径:', isAdminPath)
    console.log('使用的 Token:', token ? token.substring(0, 20) + '...' : '无')
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
      console.log('Authorization header 已设置')
    } else {
      console.warn('未找到 Token')
      if (!config.url.includes('/auth/login') && !config.url.includes('/auth/register') && !config.url.includes('/auth/admin/login')) {
        if (isAdminPath) {
          console.log('跳转到管理员登录页')
          window.location.href = '/admin/login'
        } else {
          console.log('跳转到用户登录页')
          window.location.href = '/login'
        }
        return Promise.reject(new Error('未找到 Token'))
      }
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

apiClient.interceptors.response.use(
  response => response,
  error => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      const currentPath = window.location.pathname
      const isAdminPath = currentPath.startsWith('/admin')
      
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('adminToken')
      localStorage.removeItem('adminUsername')
      localStorage.removeItem('isAdmin')
      localStorage.removeItem('userId')
      
      if (currentPath !== '/login' && currentPath !== '/admin/login') {
        if (isAdminPath) {
          window.location.href = '/admin/login'
        } else {
          window.location.href = '/login'
        }
      }
    }
    return Promise.reject(error)
  }
)

export default apiClient

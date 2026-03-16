import axios from 'axios'

// 创建 axios 实例
const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：在请求头中添加 token
apiClient.interceptors.request.use(
  config => {
    // 优先使用管理员 token，否则使用普通用户 token
    const token = localStorage.getItem('adminToken') || localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器：处理错误
apiClient.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      // 检查是否是管理员登录请求
      const isAdminLoginRequest = error.config.url.includes('/auth/admin/login')
      
      // Token 过期或无效，清除本地存储
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('adminToken')
      localStorage.removeItem('adminUsername')
      localStorage.removeItem('isAdmin')
      
      // 如果在浏览器环境中，根据请求类型跳转到对应的登录页
      if (window.location.pathname !== '/login' && window.location.pathname !== '/admin/login') {
        if (isAdminLoginRequest) {
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

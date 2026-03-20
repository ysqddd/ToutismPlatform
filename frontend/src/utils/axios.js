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
    const adminToken = localStorage.getItem('adminToken')
    const userToken = localStorage.getItem('token')
    const token = adminToken || userToken
    console.log('=== Axios 请求拦截器 ===')
    console.log('请求 URL:', config.url)
    console.log('Admin Token 存在:', !!adminToken)
    console.log('User Token 存在:', !!userToken)
    console.log('使用的 Token:', token ? token.substring(0, 20) + '...' : '无')
    console.log('当前路径:', window.location.pathname)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
      console.log('Authorization header 已设置')
    } else {
      console.warn('未找到 Token')
      // 如果是登录或注册请求，允许继续
      if (!config.url.includes('/auth/login') && !config.url.includes('/auth/register') && !config.url.includes('/auth/admin/login')) {
        // 跳转到登录页
        console.log('跳转到登录页')
        window.location.href = '/login'
        // 取消请求
        return Promise.reject(new Error('未找到 Token'))
      }
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
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      // 检查是否是管理员登录请求
      const isAdminLoginRequest = error.config.url.includes('/auth/admin/login')
      
      // Token 过期或无效，清除本地存储
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('adminToken')
      localStorage.removeItem('adminUsername')
      localStorage.removeItem('isAdmin')
      localStorage.removeItem('userId')
      
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

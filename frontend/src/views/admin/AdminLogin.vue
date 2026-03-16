<template>
  <div class="admin-login-container">
    <div class="login-box">
      <h1 class="login-title">管理员登录</h1>
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="username">用户名</label>
          <input 
            type="text" 
            id="username" 
            v-model="loginForm.username" 
            placeholder="请输入管理员用户名"
            required
          />
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input 
            type="password" 
            id="password" 
            v-model="loginForm.password" 
            placeholder="请输入密码"
            required
          />
        </div>
        <button type="submit" class="login-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
        <div v-if="error" class="error-message">{{ error }}</div>
      </form>
      <div class="back-to-home">
        <router-link to="/home">返回用户首页</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import apiClient from '../../utils/axios'

export default {
  name: 'AdminLogin',
  data() {
    return {
      loginForm: {
        username: '',
        password: ''
      },
      loading: false,
      error: ''
    }
  },
  methods: {
    async handleLogin() {
      this.loading = true
      this.error = ''
      
      try {
        const response = await apiClient.post('/api/auth/admin/login', this.loginForm)
        
        if (response.data && response.data.token) {
          localStorage.setItem('adminToken', response.data.token)
          localStorage.setItem('adminUsername', this.loginForm.username)
          localStorage.setItem('isAdmin', 'true')
          
          this.$router.push('/admin/home')
        } else {
          this.error = '登录失败，请检查用户名和密码'
        }
      } catch (error) {
        console.error('Login error:', error)
        this.error = error.response?.data?.message || '登录失败，请稍后重试'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.admin-login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-box {
  background: white;
  border-radius: 10px;
  padding: 40px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 400px;
}

.login-title {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
  font-size: 28px;
  font-weight: bold;
}

.login-form {
  display: flex;
  flex-direction: column;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #555;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.3s;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
}

.login-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 14px;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: transform 0.2s;
  margin-top: 10px;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-message {
  color: #f44336;
  text-align: center;
  margin-top: 15px;
  font-size: 14px;
}

.back-to-home {
  text-align: center;
  margin-top: 20px;
}

.back-to-home a {
  color: #667eea;
  text-decoration: none;
  font-size: 14px;
}

.back-to-home a:hover {
  text-decoration: underline;
}
</style>

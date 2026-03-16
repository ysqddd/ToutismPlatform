<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="logo-icon">🔐</div>
        <h2>欢迎登录</h2>
        <p class="subtitle">探索精彩旅游世界</p>
      </div>
      
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">
            <span class="icon">👤</span>
            用户名
          </label>
          <input 
            type="text" 
            id="username" 
            v-model="form.username" 
            required
            placeholder="请输入用户名"
          >
        </div>
        
        <div class="form-group">
          <label for="password">
            <span class="icon">🔑</span>
            密码
          </label>
          <input 
            type="password" 
            id="password" 
            v-model="form.password" 
            required
            placeholder="请输入密码"
          >
        </div>
        
        <button type="submit" class="btn" :disabled="isLoading">
          <span v-if="isLoading" class="spinner"></span>
          {{ isLoading ? '登录中...' : '登 录' }}
        </button>
        
        <p class="error" v-if="error">
          <span class="error-icon">⚠️</span>
          {{ error }}
        </p>
      </form>
      
      <div class="footer-links">
        <p class="switch">还没有账号？ 
          <router-link to="/register" class="link">立即注册</router-link>
        </p>
        <p class="admin-link">
          是管理员？ 
          <router-link to="/admin/login" class="link admin-login-link">进入管理平台</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import apiClient from '../utils/axios'
import '../assets/css/login.css'

export default {
  data() {
    return {
      form: {
        username: '',
        password: ''
      },
      error: '',
      isLoading: false
    }
  },
  methods: {
    async handleLogin() {
      this.isLoading = true;
      this.error = '';
      try {
          const response = await apiClient.post('/api/auth/login', this.form)
          localStorage.setItem('token', response.data.token)
          localStorage.setItem('username', response.data.username)
          
          // 获取用户详细信息（包括 ID）
          try {
            const userResponse = await apiClient.get('/api/auth/current-user')
            localStorage.setItem('userId', userResponse.data.id)
          } catch (err) {
            console.error('获取用户 ID 失败:', err)
          }
          
          this.$router.push('/')
        } catch (err) {
          this.error = err.response?.data || '登录失败，请检查用户名和密码'
        } finally {
          this.isLoading = false;
        }
    }
  }
}
</script>
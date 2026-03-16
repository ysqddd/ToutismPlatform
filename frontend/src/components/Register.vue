<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <div class="logo-icon">✈️</div>
        <h2>欢迎注册</h2>
        <p class="subtitle">开启您的旅游之旅</p>
      </div>
      
      <form @submit.prevent="handleRegister">
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
          <label for="email">
            <span class="icon">📧</span>
            邮箱
          </label>
          <input 
            type="email" 
            id="email" 
            v-model="form.email" 
            required
            placeholder="请输入邮箱地址"
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
            placeholder="请输入密码（至少 6 位）"
            minlength="6"
          >
        </div>
        
        <button type="submit" class="btn" :disabled="isLoading">
          <span v-if="isLoading" class="spinner"></span>
          {{ isLoading ? '注册中...' : '注 册' }}
        </button>
        
        <p class="error" v-if="error">
          <span class="error-icon">⚠️</span>
          {{ error }}
        </p>
        
        <p class="success" v-if="success">
          <span class="success-icon">✅</span>
          {{ success }}
        </p>
      </form>
      
      <div class="footer-links">
        <p class="switch">已有账号？ 
          <router-link to="/login" class="link">立即登录</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import apiClient from '../utils/axios'
import '../assets/css/register.css'

export default {
  methods: {
    async handleRegister() {
      this.isLoading = true;
      this.error = '';
      this.success = '';
      try {
        const response = await apiClient.post('/api/auth/register', this.form)
        // 根据后端返回的数据结构显示消息
        if (typeof response.data === 'string') {
          this.success = response.data
        } else if (response.data && response.data.message) {
          this.success = response.data.message
        } else {
          this.success = '注册成功！'
        }
        // 注册成功后跳转到登录页面
        setTimeout(() => {
          this.$router.push('/login')
        }, 1500)
      } catch (err) {
        // 错误信息处理
        if (err.response && err.response.data) {
          this.error = typeof err.response.data === 'string' 
            ? err.response.data 
            : '注册失败，请重试'
        } else {
          this.error = '注册失败，请检查网络连接'
        }
        this.success = ''
      } finally {
        this.isLoading = false;
      }
    }
  },
  data() {
    return {
      form: {
        username: '',
        email: '',
        password: ''
      },
      error: '',
      success: '',
      isLoading: false
    }
  }
}
</script>
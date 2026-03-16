<template>
  <div class="navbar-container">
    <div class="navbar-brand">
      <span class="logo">🌴 旅游平台</span>
    </div>
    
    <ul class="navbar-menu">
      <li class="menu-item" @click="navigateTo('/home')">
        <span class="icon">🏠</span>
        <span class="text">首页</span>
      </li>
      <li class="menu-item" @click="navigateTo('/assistant')">
        <span class="icon">🤖</span>
        <span class="text">小助手</span>
      </li>
      <li class="menu-item" @click="navigateTo('/scenic-intro')">
        <span class="icon">🏞️</span>
        <span class="text">景点介绍</span>
      </li>
      <li class="menu-item" @click="navigateTo('/subscription')">
        <span class="icon">📝</span>
        <span class="text">在线预定</span>
      </li>
      <li class="menu-item" @click="navigateTo('/shopping-cart')">
        <span class="icon">🛒</span>
        <span class="text">购物车</span>
      </li>
    </ul>
    
    <div class="navbar-user">
      <div class="user-info-wrapper">
        <span class="menu-item user-center-link" @click="navigateTo('/user-center')">
          <span class="icon">👤</span>
          <span class="text">用户中心</span>
        </span>
        <span class="welcome-text">欢迎，{{ username }}</span>
      </div>
      <button class="logout-btn" @click="handleLogout">退出</button>
    </div>
  </div>
</template>

<script>
import apiClient from '../utils/axios'

export default {
  name: 'NavBar',
  data() {
    return {
      username: ''
    }
  },
  mounted() {
    this.username = localStorage.getItem('username') || '游客'
  },
  methods: {
    navigateTo(path) {
      this.$router.push(path)
    },
    async handleLogout() {
      try {
        await apiClient.post('/api/auth/logout')
      } catch (error) {
        console.error('Logout error:', error)
      } finally {
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        this.$router.push('/login')
      }
    }
  }
}
</script>

<style scoped>
@import '@/assets/css/navbar.css';
</style>

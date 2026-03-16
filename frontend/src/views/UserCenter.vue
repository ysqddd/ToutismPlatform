<template>
  <div class="user-center-container">
    <h2>👤 用户中心</h2>
    <div class="user-info-card">
      <div class="info-section">
        <h3>个人信息</h3>
        <div class="info-item">
          <span class="label">用户名：</span>
          <span class="value">{{ userInfo.username || '加载中...' }}</span>
        </div>
        <div class="info-item">
          <span class="label">邮箱：</span>
          <span class="value">{{ userInfo.email || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="label">角色：</span>
          <span class="value">{{ userInfo.role || 'USER' }}</span>
        </div>
      </div>
      
      <div class="info-section">
        <h3>我的订单</h3>
        <p v-if="orders.length === 0" class="tip">暂无订单信息</p>
        <div v-else class="order-list">
          <div v-for="order in orders" :key="order.id" class="order-item">
            <div class="order-header">
              <span class="order-id">订单号：{{ order.id }}</span>
              <span :class="['order-status', order.status]">{{ order.statusText }}</span>
            </div>
            <div class="order-info">
              <p>商品：{{ order.productName }}</p>
              <p>金额：¥{{ order.amount }}</p>
              <p>时间：{{ order.createdAt }}</p>
            </div>
          </div>
        </div>
      </div>
      
      <div class="info-section">
        <h3>账户设置</h3>
        <button class="action-btn" @click="showModifyPassword = true">修改密码</button>
        <button class="action-btn">绑定手机</button>
        <button class="action-btn">实名认证</button>
      </div>
    </div>
  </div>
</template>

<script>
import apiClient from '@/utils/axios'

export default {
  name: 'UserCenter',
  data() {
    return {
      userInfo: {
        username: '',
        email: '',
        role: ''
      },
      orders: [],
      showModifyPassword: false
    }
  },
  mounted() {
    this.loadUserInfo()
  },
  methods: {
    async loadUserInfo() {
      try {
        const response = await apiClient.get('/api/auth/current-user')
        this.userInfo = response.data
        localStorage.setItem('userId', response.data.id)
        localStorage.setItem('username', response.data.username)
      } catch (error) {
        console.error('加载用户信息失败:', error)
        this.userInfo.username = localStorage.getItem('username') || '游客'
      }
    },
    async loadOrders() {
      try {
        // TODO: 调用订单 API
        // const response = await apiClient.get('/orders?userId=' + this.userInfo.id)
        // this.orders = response.data
        this.orders = []
      } catch (error) {
        console.error('加载订单失败:', error)
      }
    }
  }
}
</script>

<style scoped>
@import '@/assets/css/user-center.css';
</style>

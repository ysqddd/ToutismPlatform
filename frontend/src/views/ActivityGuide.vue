<template>
  <div class="activity-guide-container">
    <h2>📋 活动攻略</h2>
    
    <div class="activities-grid">
      <div 
        v-for="(activity, index) in activities" 
        :key="index" 
        class="activity-card"
      >
        <div class="activity-image">
          {{ activity.emoji }}
        </div>
        <div class="activity-info">
          <h3>{{ activity.title }}</h3>
          <p class="activity-desc">{{ activity.description }}</p>
          <div class="activity-meta">
            <span class="tag">{{ activity.tag }}</span>
            <span class="price">¥{{ activity.price }}起</span>
          </div>
          <button class="book-btn" @click="viewDetails(activity)">查看详情</button>
        </div>
      </div>
    </div>
    
    <div class="guide-section">
      <h3>💡 游玩攻略</h3>
      <div class="guide-list">
        <div 
          v-for="(guide, index) in guides" 
          :key="index" 
          class="guide-item"
        >
          <div class="guide-icon">{{ guide.icon }}</div>
          <div class="guide-content">
            <h4>{{ guide.title }}</h4>
            <p>{{ guide.content }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import apiClient from '@/utils/axios'

export default {
  name: 'ActivityGuide',
  data() {
    return {
      activities: [],
      guides: [
        {
          icon: '📅',
          title: '最佳出行时间',
          content: '春秋季节是旅游的最佳时机，天气宜人，景色优美'
        },
        {
          icon: '🎒',
          title: '行前准备',
          content: '带好身份证、充电宝、常用药品等必需品'
        },
        {
          icon: '📸',
          title: '拍照技巧',
          content: '选择光线好的时间段，利用构图技巧拍出美照'
        },
        {
          icon: '🍜',
          title: '美食推荐',
          content: '品尝当地特色美食，注意饮食卫生'
        }
      ]
    }
  },
  mounted() {
    this.loadActivities()
  },
  methods: {
    async loadActivities() {
      try {
        const response = await apiClient.get('/api/products/on-sale')
        // 将后端产品数据转换为前端活动格式
        this.activities = response.data.map(product => ({
          id: product.id,
          emoji: '🎯',
          title: product.name,
          description: product.description || '精彩活动，详情咨询',
          tag: '热门',
          price: parseFloat(product.price),
          productId: product.id
        }))
      } catch (error) {
        console.error('加载活动失败:', error)
      }
    },
    viewDetails(activity) {
      // 跳转到产品详情或添加到购物车
      alert(`即将查看 ${activity.title} 的详情`)
    }
  }
}
</script>

<style scoped>
@import '@/assets/css/activity-guide.css';
</style>

<template>
  <div class="scenic-spot-container">
    <h2>🏞️ 景点介绍</h2>
    
    <div class="spots-grid">
      <div 
        v-for="(scenic, index) in scenicSpots" 
        :key="index" 
        class="spot-card"
      >
        <div class="spot-image">
          <img v-if="scenic.imageUrl" :src="scenic.imageUrl" :alt="scenic.name">
          <div v-else class="spot-image-placeholder">{{ scenic.emoji }}</div>
        </div>
        <div class="spot-header">
          <span class="level-tag 5A">{{ scenic.tag }}</span>
        </div>
        <h3>{{ scenic.name }}</h3>
        <p class="spot-desc">{{ scenic.description }}</p>
        <div class="spot-info">
          <div class="info-row">
            <span class="label">价格</span>
            <span class="value price">¥{{ scenic.price }}起</span>
          </div>
          <div class="info-row" v-if="scenic.location">
            <span class="label">位置</span>
            <span class="value">{{ scenic.location }}</span>
          </div>
          <div class="info-row" v-if="scenic.openingHours">
            <span class="label">开放时间</span>
            <span class="value">{{ scenic.openingHours }}</span>
          </div>
        </div>
        <div class="spot-actions">
          <button class="action-btn primary" @click="viewDetails(scenic)">查看详情</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import apiClient from '@/utils/axios'

export default {
  name: 'ScenicIntro',
  data() {
    return {
      scenicSpots: []
    }
  },
  mounted() {
    this.loadScenicSpots()
  },
  methods: {
    async loadScenicSpots() {
      try {
        // 从后端API获取景区信息
        const response = await apiClient.get('/api/large-areas')
        // 将后端景区数据转换为前端景点格式
        this.scenicSpots = response.data.map(area => {
          let imageUrl = area.imageUrl
          // 处理图片URL
          if (imageUrl && imageUrl.startsWith('/')) {
            imageUrl = `http://localhost:8080${imageUrl}`
          }
          return {
            id: area.id,
            emoji: '🏞️',
            name: area.name,
            description: area.description || '精彩景点，详情咨询',
            tag: area.tags || '热门',
            price: parseFloat(area.price),
            location: area.location,
            openingHours: area.openingHours,
            imageUrl: imageUrl
          }
        })
      } catch (error) {
        console.error('加载景点失败:', error)
      }
    },
    viewDetails(scenic) {
      // 跳转到景点详情页
      this.$router.push(`/scenic/${scenic.id}`)
    }
  }
}
</script>

<style scoped>
@import '@/assets/css/scenic-spot.css';
</style>
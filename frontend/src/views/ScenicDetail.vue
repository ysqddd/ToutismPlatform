<template>
  <div class="scenic-detail">
    <div class="container">
      <h1>{{ scenic.name }}</h1>
      <div class="scenic-info">
        <div class="scenic-image">
          <img v-if="scenic.imageUrl" :src="scenic.imageUrl" :alt="scenic.name">
          <div v-else class="image-placeholder">🏞️</div>
        </div>
        <div class="scenic-details">
          <p class="ticket-price">门票: ¥{{ scenic.price }}</p>
          <p class="description">{{ scenic.description || '暂无描述' }}</p>
          <p class="address" v-if="scenic.location">地址: {{ scenic.location }}</p>
          <p class="opening-hours" v-if="scenic.openingHours">开放时间: {{ scenic.openingHours }}</p>
          <p class="tags" v-if="scenic.tags">标签: {{ scenic.tags }}</p>
          <button class="book-ticket" @click="bookTicket">预订门票</button>
        </div>
      </div>
      
      <!-- 景点列表 -->
      <div class="scenic-spots" v-if="spots.length > 0">
        <h2>景点列表</h2>
        <div class="spots-grid">
          <div class="spot-card" v-for="spot in spots" :key="spot.id">
            <div class="spot-image">
              <img v-if="spot.imageUrl" :src="spot.imageUrl" :alt="spot.name">
              <div v-else class="spot-image-placeholder">📍</div>
            </div>
            <h3>{{ spot.name }}</h3>
            <p class="spot-description">{{ spot.description || '暂无描述' }}</p>
            <p class="visiting-duration" v-if="spot.visitingDuration">建议游览时间: {{ spot.visitingDuration }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import apiClient from '@/utils/axios'

export default {
  name: 'ScenicDetail',
  data() {
    return {
      scenic: {
        id: '',
        name: '',
        price: 0,
        description: '',
        imageUrl: '',
        location: '',
        openingHours: '',
        tags: ''
      },
      spots: []
    }
  },
  created() {
    this.getScenicDetail()
  },
  methods: {
    async getScenicDetail() {
      try {
        // 从路由参数中获取景区ID
        const scenicId = this.$route.params.id
        // 调用API获取景区详情
        const response = await apiClient.get(`/api/large-areas/${scenicId}`)
        this.scenic = response.data
        
        // 处理景区图片URL
        if (this.scenic.imageUrl && this.scenic.imageUrl.startsWith('/')) {
          this.scenic.imageUrl = `http://localhost:8080${this.scenic.imageUrl}`
        }
        
        // 获取景区的景点
        if (this.scenic.smallScenicSpots) {
          this.spots = this.scenic.smallScenicSpots
          // 处理景点图片URL
          this.spots.forEach(spot => {
            if (spot.imageUrl && spot.imageUrl.startsWith('/')) {
              spot.imageUrl = `http://localhost:8080${spot.imageUrl}`
            }
          })
        }
      } catch (error) {
        console.error('加载景区详情失败:', error)
      }
    },
    bookTicket() {
      // 预订门票的逻辑
      alert('已预订门票')
    }
  }
}
</script>

<style scoped>
.scenic-detail {
  padding: 20px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

h1 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.scenic-info {
  display: flex;
  gap: 40px;
  margin-bottom: 40px;
}

.scenic-image {
  flex: 1;
  max-width: 800px;
}

.scenic-image img {
  width: 100%;
  height: 400px;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.scenic-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ticket-price {
  font-size: 28px;
  font-weight: bold;
  color: #e74c3c;
  margin: 0;
}

.description {
  font-size: 16px;
  line-height: 1.5;
  color: #666;
  margin: 0;
}

.address, .opening-hours {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.book-ticket {
  background-color: #3498db;
  color: white;
  border: none;
  padding: 12px 24px;
  font-size: 16px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
  width: fit-content;
}

.book-ticket:hover {
  background-color: #2980b9;
}

.scenic-gallery {
  margin-top: 40px;
}

h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.gallery-images {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.gallery-item img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  transition: transform 0.3s;
}

.gallery-item img:hover {
  transform: scale(1.05);
}

.scenic-spots {
  margin-top: 60px;
}

.scenic-spots h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.spots-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 30px;
}

.spot-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.spot-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.spot-image {
  width: 100%;
  height: 250px;
  overflow: hidden;
}

.spot-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.spot-image-placeholder {
  width: 100%;
  height: 250px;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
}

.spot-card h3 {
  padding: 20px 20px 10px;
  margin: 0;
  font-size: 18px;
  color: #333;
}

.spot-description {
  padding: 0 20px 15px;
  margin: 0;
  font-size: 14px;
  color: #666;
  line-height: 1.4;
}

.visiting-duration {
  padding: 0 20px 20px;
  margin: 0;
  font-size: 14px;
  color: #999;
}

.image-placeholder {
  width: 100%;
  height: 400px;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 72px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

@media (max-width: 768px) {
  .scenic-info {
    flex-direction: column;
  }
  
  .scenic-image {
    max-width: 100%;
  }
  
  .image-placeholder {
    height: 300px;
    font-size: 48px;
  }
  
  .spots-grid {
    grid-template-columns: 1fr;
  }
}
</style>
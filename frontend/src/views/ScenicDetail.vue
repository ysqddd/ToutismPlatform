<template>
  <div class="portal-page scenic-detail-page">
    <div class="portal-wrap">
      <section class="portal-grid two scenic-layout">
        <div class="portal-hero scenic-detail-hero">
          <div class="portal-pill light">SCENIC DETAIL</div>
          <h1 class="portal-title">{{ scenic.name || '景区详情' }}</h1>
          <p class="portal-subtitle">
            {{ scenic.description || '在出发前先看看这个景区的亮点、门票参考、开放时间和游览信息，帮助你判断它是否适合加入这次行程。' }}
          </p>
        </div>

        <div class="portal-surface portal-panel">
          <img v-if="scenic.imageUrl" :src="scenic.imageUrl" :alt="scenic.name" class="detail-image" />
          <div v-else class="detail-image portal-placeholder">🏞️</div>
          <div class="portal-kv detail-kv">
            <div class="portal-kv-item">
              <strong>门票</strong>
              <span class="portal-price">¥{{ scenic.price || 0 }}</span>
            </div>
            <div v-if="scenic.location" class="portal-kv-item">
              <strong>地址</strong>
              <span>{{ scenic.location }}</span>
            </div>
            <div v-if="scenic.openingHours" class="portal-kv-item">
              <strong>开放时间</strong>
              <span>{{ scenic.openingHours }}</span>
            </div>
            <div v-if="scenic.tags" class="portal-kv-item">
              <strong>标签</strong>
              <span>{{ scenic.tags }}</span>
            </div>
          </div>
          <div class="portal-actions">
            <button class="portal-btn primary" @click="bookTicket">预订门票</button>
          </div>
        </div>
      </section>

      <section class="portal-section">
        <div class="portal-section-head">
          <div>
            <div class="portal-pill soft">SPOTS</div>
            <h2>景点列表</h2>
            <p>把景点子项继续沿用首页卡片风格，方便继续深挖内容。</p>
          </div>
        </div>

        <div v-if="spots.length > 0" class="portal-grid three">
          <article class="portal-card" v-for="spot in spots" :key="spot.id">
            <img v-if="spot.imageUrl" :src="spot.imageUrl" :alt="spot.name" class="portal-media" />
            <div v-else class="portal-media portal-placeholder">📍</div>
            <div class="portal-card-body">
              <h3>{{ spot.name }}</h3>
              <p>{{ spot.description || '暂无描述' }}</p>
              <div v-if="spot.visitingDuration" class="portal-kv">
                <div class="portal-kv-item">
                  <strong>建议游览时间</strong>
                  <span>{{ spot.visitingDuration }}</span>
                </div>
              </div>
            </div>
          </article>
        </div>
      </section>
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
        const scenicId = this.$route.params.id
        const response = await apiClient.get(`/api/large-areas/${scenicId}`)
        this.scenic = response.data
        if (this.scenic.imageUrl && this.scenic.imageUrl.startsWith('/')) {
          this.scenic.imageUrl = `http://localhost:8080${this.scenic.imageUrl}`
        }
        if (this.scenic.smallScenicSpots) {
          this.spots = this.scenic.smallScenicSpots
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
      alert('已预订门票')
    }
  }
}
</script>

<style scoped>
@import '@/assets/css/portal-theme.css';

.scenic-layout {
  align-items: stretch;
}

.scenic-detail-hero {
  min-height: 100%;
}

.detail-image {
  width: 100%;
  height: 320px;
  object-fit: cover;
  border-radius: 24px;
}

.detail-kv {
  margin-top: 20px;
}
</style>

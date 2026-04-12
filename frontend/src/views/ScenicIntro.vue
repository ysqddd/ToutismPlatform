<template>
  <div class="portal-page scenic-page">
    <div class="portal-wrap">
      <section class="portal-hero scenic-hero">
        <div class="portal-pill light">SCENIC COLLECTION</div>
        <h1 class="portal-title">为你精选值得去的景区</h1>
        <p class="portal-subtitle">
          想先确定这次去哪里玩，可以从这里浏览热门景区、门票参考和开放信息，
          更轻松地选出适合你的目的地。
        </p>
      </section>

      <section class="portal-section">
        <div class="portal-section-head">
          <div>
            <div class="portal-pill soft">SCENIC GRID</div>
            <h2>热门景区推荐</h2>
            <p>每个卡片都保留价格、开放时间与位置等关键决策信息。</p>
          </div>
        </div>

        <div v-if="scenicSpots.length" class="portal-grid three scenic-grid">
          <article
            v-for="(scenic, index) in scenicSpots"
            :key="scenic.id || index"
            class="portal-card scenic-card"
          >
            <div class="scenic-media-wrap">
              <img v-if="scenic.imageUrl" :src="scenic.imageUrl" :alt="scenic.name" class="portal-media" />
              <div v-else class="portal-media portal-placeholder scenic-placeholder">{{ scenic.emoji }}</div>
              <div class="scenic-badge">{{ scenic.tag }}</div>
            </div>
            <div class="portal-card-body">
              <h3>{{ scenic.name }}</h3>
              <p class="scenic-desc">{{ scenic.description }}</p>
              <div class="portal-kv scenic-kv">
                <div class="portal-kv-item">
                  <strong>门票参考</strong>
                  <span class="portal-price">¥{{ scenic.price || 0 }} 起</span>
                </div>
                <div v-if="scenic.location" class="portal-kv-item">
                  <strong>位置</strong>
                  <span>{{ scenic.location }}</span>
                </div>
                <div v-if="scenic.openingHours" class="portal-kv-item">
                  <strong>开放时间</strong>
                  <span>{{ scenic.openingHours }}</span>
                </div>
              </div>
              <div class="portal-actions scenic-actions">
                <button class="portal-btn ghost" @click="addToCart(scenic)">加入购物车</button>
                <button class="portal-btn primary" @click="viewDetails(scenic)">查看详情</button>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="portal-surface portal-empty">
          <strong>景区数据加载中</strong>
          <p class="portal-subtitle dark">稍后这里会展示景区图片、票价和开放信息。</p>
        </div>
      </section>
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
        const response = await apiClient.get('/api/large-areas')
        this.scenicSpots = response.data.map(area => {
          let imageUrl = area.imageUrl
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
            imageUrl
          }
        })
      } catch (error) {
        console.error('加载景点失败:', error)
      }
    },
    viewDetails(scenic) {
      this.$router.push(`/scenic/${scenic.id}`)
    },
    addToCart(scenic) {
      const userId = localStorage.getItem('userId')
      if (!userId) {
        alert('请先登录')
        this.$router.push('/login')
        return
      }

      apiClient.post(`/api/cart/scenic?userId=${userId}&scenicAreaId=${scenic.id}`)
        .then(() => {
          alert('已添加到购物车')
          this.$router.push('/shopping-cart')
        })
        .catch(error => {
          console.error('添加失败:', error)
          alert('添加失败，请重试')
        })
    }
  }
}
</script>

<style scoped>
@import '@/assets/css/portal-theme.css';

.scenic-hero {
  background:
    linear-gradient(115deg, rgba(9, 39, 28, 0.92), rgba(31, 98, 68, 0.82)),
    url('http://127.0.0.1:8080/images/qingming-shangheyuan.jpg') center/cover;
}

.scenic-grid {
  align-items: stretch;
}

.scenic-card {
  height: 100%;
}

.scenic-media-wrap {
  position: relative;
}

.scenic-placeholder {
  font-size: 56px;
}

.scenic-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  color: var(--portal-primary);
  font-size: 13px;
  font-weight: 700;
}

.scenic-desc {
  min-height: 86px;
}

.scenic-kv {
  margin-top: 18px;
}

.scenic-actions {
  justify-content: space-between;
}

.scenic-actions .portal-btn {
  flex: 1;
}
</style>

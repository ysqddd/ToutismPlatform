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
          <div class="hero-detail-list">
            <div v-if="scenic.location">
              <strong>地址</strong>
              <span>{{ scenic.location }}</span>
            </div>
            <div v-if="scenic.openingHours">
              <strong>开放时间</strong>
              <span>{{ scenic.openingHours }}</span>
            </div>
            <div>
              <strong>建议游览</strong>
              <span>{{ scenic.recommendedVisitDuration || 120 }} 分钟</span>
            </div>
            <div>
              <strong>游览内容</strong>
              <span>{{ scenicSpots.length }} 个景点 · {{ gates.length }} 个入口</span>
            </div>
          </div>
        </div>

        <div class="portal-surface portal-panel">
          <img v-if="scenic.imageUrl" :src="scenic.imageUrl" :alt="scenic.name" class="detail-image" />
          <div v-else class="detail-image portal-placeholder">🏞️</div>
          <div class="portal-kv detail-kv">
            <div class="portal-kv-item">
              <strong>门票</strong>
              <span class="portal-price">¥{{ scenic.price || 0 }}</span>
            </div>
          </div>
          <div class="portal-actions">
            <button class="portal-btn primary" @click="bookTicket">预订门票</button>
          </div>
        </div>
      </section>

      <section class="portal-section">
        <div class="portal-surface portal-panel scenic-intro">
          <div>
            <div class="portal-pill soft">OVERVIEW</div>
            <h2>景区介绍</h2>
            <p>{{ scenic.description || '暂无景区描述' }}</p>
          </div>
          <div class="intro-tags" v-if="tagList.length">
            <span v-for="tag in tagList" :key="tag">{{ tag }}</span>
          </div>
        </div>
      </section>

      <section class="portal-section">
        <div class="portal-section-head">
          <div>
            <div class="portal-pill soft">SPOTS</div>
            <h2>景点列表</h2>
          </div>
        </div>

        <div v-if="gates.length > 0" class="spot-group">
          <h3>景区入口</h3>
          <div class="portal-grid three">
            <article class="portal-card" v-for="gate in gates" :key="gate.id">
              <img :src="getSpotImageUrl(gate)" :alt="gate.name" class="portal-media" />
              <div class="portal-card-body">
                <h3>{{ gate.name }}</h3>
                <p>{{ gate.description || '暂无入口描述' }}</p>
                <div v-if="gate.visitingDuration" class="portal-kv">
                  <div class="portal-kv-item">
                    <strong>建议停留时间</strong>
                    <span>{{ gate.visitingDuration }} 分钟</span>
                  </div>
                </div>
              </div>
            </article>
          </div>
        </div>

        <div v-if="scenicSpots.length > 0" class="spot-group">
          <h3>游览景点</h3>
          <div class="portal-grid three">
            <article class="portal-card" v-for="spot in scenicSpots" :key="spot.id">
              <img :src="getSpotImageUrl(spot)" :alt="spot.name" class="portal-media" />
              <div class="portal-card-body">
                <h3>{{ spot.name }}</h3>
                <p>{{ spot.description || '暂无描述' }}</p>
                <div v-if="spot.visitingDuration" class="portal-kv">
                  <div class="portal-kv-item">
                    <strong>建议游览时间</strong>
                    <span>{{ spot.visitingDuration }} 分钟</span>
                  </div>
                </div>
              </div>
            </article>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import apiClient from '@/utils/axios'

const gateImageFallbacks = {
  '2:金水门': '/images/qingming-jinshui-gate.jpg',
  '2:端门': '/images/qingming-duan-gate.jpg',
  '2:丹凤门': '/images/qingming-danfeng-gate.jpg',
  '2:通津门': '/images/qingming-tongjin-gate.jpg',
  '3:午门': '/images/longting-wumen-gate.jpg',
  '3:北门': '/images/longting-north-gate.jpg',
  '3:东便门': '/images/longting-east-gate.jpg',
  '4:府门': '/images/kaifengfu-gate.jpg',
  '5:山门': '/images/daxiangguosi-gate.jpg',
  '7:南门': '/images/wansuishan-gate.jpg',
  '7:北大门': '/images/wansuishan-gate.jpg',
  '7:东大门': '/images/wansuishan-gate.jpg',
  '11:南大门': '/images/hanyuan-gate.jpg',
  '11:北大门': '/images/hanyuan-gate.jpg',
  '13:东门': '/images/yuwangtai-gate.jpg',
  '13:西门': '/images/yuwangtai-gate.jpg'
}

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
        recommendedVisitDuration: 120,
        tags: ''
      },
      spots: []
    }
  },
  computed: {
    backendBaseUrl() {
      const baseURL = apiClient?.defaults?.baseURL || ''
      return /^https?:\/\//i.test(baseURL) ? baseURL.replace(/\/$/, '') : window.location.origin
    },
    gates() {
      return this.spots.filter(spot => Number(spot.isSpotType) === 1)
    },
    scenicSpots() {
      return this.spots.filter(spot => Number(spot.isSpotType) !== 1)
    },
    tagList() {
      return String(this.scenic.tags || '')
        .split(',')
        .map(tag => tag.trim())
        .filter(Boolean)
    }
  },
  watch: {
    '$route.params.id': 'getScenicDetail'
  },
  created() {
    this.getScenicDetail()
  },
  methods: {
    normalizeImagePath(path) {
      if (!path) {
        return ''
      }
      let normalized = String(path).trim()
      if (!normalized) {
        return ''
      }
      if (/^https?:\/\//i.test(normalized)) {
        return normalized
      }
      normalized = normalized.replace(/\\/g, '/')
      normalized = normalized.replace(/^classpath:/i, '')
      normalized = normalized.replace(/^src\/main\//i, '')
      normalized = normalized.replace(/^public\//i, '')
      normalized = normalized.replace(/^static\//i, '')
      normalized = normalized.replace(/^resources\//i, '')
      const imageIndex = normalized.lastIndexOf('images/')
      if (imageIndex >= 0) {
        normalized = normalized.substring(imageIndex)
      }
      return normalized.startsWith('/') ? normalized : `/${normalized}`
    },
    getImageUrl(path) {
      const normalized = this.normalizeImagePath(path)
      if (!normalized) {
        return ''
      }
      return /^https?:\/\//i.test(normalized) ? normalized : `${this.backendBaseUrl}${normalized}`
    },
    getSpotImageUrl(spot) {
      const fallback = gateImageFallbacks[`${spot.largeAreaId}:${spot.name}`]
      return this.getImageUrl(spot.imageUrl) || this.getImageUrl(fallback) || this.getImageUrl(this.scenic.imageUrl)
    },
    async getScenicDetail() {
      try {
        const scenicId = this.$route.params.id
        const [scenicResponse, spotsResponse] = await Promise.all([
          apiClient.get(`/api/large-areas/${scenicId}`),
          apiClient.get(`/api/small-spots/large-area/${scenicId}`)
        ])
        this.scenic = scenicResponse.data || {}
        this.scenic.imageUrl = this.getImageUrl(this.scenic.imageUrl)
        this.spots = Array.isArray(spotsResponse.data)
          ? spotsResponse.data
          : (this.scenic.smallScenicSpots || [])
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

.hero-detail-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  max-width: 760px;
  margin-top: 28px;
}

.hero-detail-list div {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.14);
}

.hero-detail-list strong {
  display: block;
  margin-bottom: 6px;
  color: #fff;
}

.hero-detail-list span {
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.6;
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

.scenic-intro {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(220px, 0.6fr);
  gap: 22px;
}

.scenic-intro h2,
.spot-group > h3 {
  margin: 12px 0 10px;
  color: var(--portal-text);
}

.scenic-intro p {
  margin: 0;
  color: var(--portal-muted);
  line-height: 1.9;
}

.intro-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  grid-column: 1 / -1;
}

.intro-tags span {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(31, 98, 68, 0.10);
  color: var(--portal-primary);
  font-size: 13px;
  font-weight: 700;
}

.spot-group + .spot-group {
  margin-top: 28px;
}

@media (max-width: 960px) {
  .scenic-intro {
    grid-template-columns: 1fr;
  }

  .hero-detail-list {
    grid-template-columns: 1fr;
  }
}
</style>

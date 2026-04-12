<template>
  <div class="home-page">
    <section
        class="hero-section"
        :style="heroBackgroundStyle"
        @mouseenter="stopAutoPlay"
        @mouseleave="resumeAutoPlay"
    >
      <div class="hero-mask"></div>

      <div class="hero-inner">
        <div class="hero-content">
          <div class="hero-tag">山水人文 · 四季可游</div>
          <h1 class="hero-title">
            {{ activeSlide.name || '遇见山水之间的旅程' }}
          </h1>
          <p class="hero-desc">
            {{ activeSlide.description || '精选景区、舒适路线与沉浸式游览体验，带你轻松开启一段更有层次的旅行。' }}
          </p>

          <div class="hero-actions">
            <button class="hero-btn primary" @click="scrollToSection('featuredSection')">热门景区</button>
            <button class="hero-btn secondary" @click="scrollToSection('guideSection')">出游指南</button>
          </div>
        </div>

        <div class="hero-side-card">
          <div class="side-card-title">精选推荐</div>
          <div
              v-for="(slide, index) in sidePreviewSlides"
              :key="slide.id || index"
              class="side-card-item"
              :class="{ active: index === currentSlide }"
              @click="goToSlide(index)"
          >
            <img :src="getImageUrl(slide.image)" :alt="slide.name" />
            <div class="side-card-text">
              <h4>{{ slide.name }}</h4>
              <p>{{ briefText(slide.description, 24) }}</p>
            </div>
          </div>
        </div>
      </div>

      <button class="hero-nav prev" @click="prevSlide">‹</button>
      <button class="hero-nav next" @click="nextSlide">›</button>

      <div class="hero-indicators">
        <span
            v-for="(slide, index) in slides"
            :key="index"
            :class="['indicator', { active: index === currentSlide }]"
            @click="goToSlide(index)"
        ></span>
      </div>
    </section>

    <section ref="featuredSection" class="section-block scenic-section">
      <div class="section-head">
        <div>
          <div class="section-tag">SCENIC RECOMMENDATION</div>
          <h2>热门景区推荐</h2>
          <p>从已接入的景区数据中提取首页推荐，保留你现有后端接口，不改动数据来源。</p>
        </div>
      </div>

      <div class="scenic-grid">
        <div
            v-for="(item, index) in featuredScenics"
            :key="item.id || index"
            class="scenic-card"
            @click="goToScenic(item)"
        >
          <div class="scenic-image-wrap">
            <img :src="getImageUrl(item.image)" :alt="item.name" class="scenic-image" />
            <div class="scenic-badge">推荐景区</div>
          </div>
          <div class="scenic-body">
            <h3>{{ item.name }}</h3>
            <p>{{ briefText(item.description, 60) }}</p>
            <button class="card-btn">查看详情</button>
          </div>
        </div>
      </div>
    </section>

    <section ref="guideSection" class="section-block guide-section single-column">
      <div class="guide-panel guide-main">
        <div class="section-tag">TRAVEL GUIDE</div>
        <h2>出游灵感</h2>
        <p>
          首页改为更偏旅游官网风格的展示方式：大图首屏、重点信息分区、推荐景区卡片和简洁的出游提示，
          更适合做景区门户首页。
        </p>
        <div class="guide-points">
          <div class="guide-point">
            <span>01</span>
            <div>
              <h4>先看景区</h4>
              <p>通过首页大图和推荐卡片快速建立目的地印象。</p>
            </div>
          </div>
          <div class="guide-point">
            <span>02</span>
            <div>
              <h4>再进详情</h4>
              <p>点击卡片即可进入景区详情页，保留你现有路由跳转逻辑。</p>
            </div>
          </div>
          <div class="guide-point">
            <span>03</span>
            <div>
              <h4>统一浏览</h4>
              <p>把首页作为景区门户入口，兼顾视觉、美观和现有业务兼容性。</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import apiClient from '@/utils/axios'

export default {
  name: 'Home',
  data() {
    return {
      currentSlide: 0,
      autoPlayInterval: null,
      slides: []
    }
  },
  computed: {
    activeSlide() {
      return this.slides[this.currentSlide] || {}
    },
    sidePreviewSlides() {
      return this.slides.slice(0, 4)
    },
    featuredScenics() {
      return this.slides.slice(0, 6)
    },
    backendBaseUrl() {
      const apiBase = apiClient?.defaults?.baseURL || ''
      if (!apiBase) return window.location.origin
      if (/^https?:\/\//i.test(apiBase)) return apiBase.replace(/\/$/, '')
      return window.location.origin
    },
    heroBackgroundStyle() {
      const image = this.getImageUrl(this.activeSlide.image)
      return image
          ? { backgroundImage: `url(${image})` }
          : { background: 'linear-gradient(135deg, #0f3d2e 0%, #2f6f52 45%, #87b89d 100%)' }
    }
  },
  mounted() {
    this.loadScenicAreas()
  },
  beforeUnmount() {
    this.stopAutoPlay()
  },
  methods: {
    normalizeImagePath(imageUrl) {
      if (!imageUrl) return ''
      let value = String(imageUrl).trim()
      if (!value) return ''
      if (/^https?:\/\//i.test(value)) return value

      value = value.replace(/\\/g, '/')
      value = value.replace(/^classpath:/i, '')
      value = value.replace(/^src\/main\//i, '')
      value = value.replace(/^public\//i, '')
      value = value.replace(/^static\//i, '')
      value = value.replace(/^resources\//i, '')

      const imagesIndex = value.lastIndexOf('images/')
      if (imagesIndex >= 0) {
        value = value.substring(imagesIndex)
      }

      return value.startsWith('/') ? value : `/${value}`
    },
    getImageUrl(imageUrl) {
      const normalized = this.normalizeImagePath(imageUrl)
      if (!normalized) return ''
      if (/^https?:\/\//i.test(normalized)) return normalized
      return `${this.backendBaseUrl}${normalized}`
    },
    async loadScenicAreas() {
      try {
        const response = await apiClient.get('/api/large-areas')
        const areas = Array.isArray(response.data) ? response.data : []
        const scenicAreas = areas.filter(area => area.isAreaType === 0)
        this.slides = scenicAreas.slice(0, 8).map(area => ({
          id: area.id,
          name: area.name,
          description: area.description,
          image: area.imageUrl
        }))
        if (this.slides.length > 0) {
          this.startAutoPlay()
        }
      } catch (error) {
        console.error('加载景区数据失败:', error)
        this.slides = [
          { id: 1, name: '清明上河园', description: '宋文化主题景区', image: '/images/qingming-shangheyuan.jpg' },
          { id: 2, name: '龙亭景区', description: '开封代表性历史景区', image: '/images/longting.jpg' },
          { id: 3, name: '开封府', description: '包公文化和府衙文化', image: '/images/kaifengfu.jpg' },
          { id: 4, name: '万岁山武侠城', description: '武侠演艺和互动体验', image: '/images/wansuishan.jpg' }
        ]
        this.startAutoPlay()
      }
    },
    goToScenic(slide) {
      if (slide && slide.id) {
        this.$router.push(`/scenic/${slide.id}`)
      }
    },
    jumpToFirstScenic() {
      if (this.featuredScenics.length > 0) {
        this.goToScenic(this.featuredScenics[0])
      }
    },
    nextSlide() {
      if (this.slides.length > 0) {
        this.currentSlide = (this.currentSlide + 1) % this.slides.length
      }
    },
    prevSlide() {
      if (this.slides.length > 0) {
        this.currentSlide = (this.currentSlide - 1 + this.slides.length) % this.slides.length
      }
    },
    goToSlide(index) {
      this.currentSlide = index
      this.resetAutoPlay()
    },
    startAutoPlay() {
      this.stopAutoPlay()
      this.autoPlayInterval = setInterval(() => {
        this.nextSlide()
      }, 5000)
    },
    stopAutoPlay() {
      if (this.autoPlayInterval) {
        clearInterval(this.autoPlayInterval)
        this.autoPlayInterval = null
      }
    },
    resumeAutoPlay() {
      if (this.slides.length > 1 && !this.autoPlayInterval) {
        this.startAutoPlay()
      }
    },
    resetAutoPlay() {
      this.stopAutoPlay()
      this.resumeAutoPlay()
    },
    scrollToSection(refName) {
      const target = this.$refs[refName]
      if (target && typeof target.scrollIntoView === 'function') {
        target.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    },
    briefText(text, maxLength = 48) {
      const value = (text || '').trim()
      if (!value) return '点击查看景区详情与更多游览信息。'
      return value.length > maxLength ? `${value.slice(0, maxLength)}...` : value
    }
  }
}
</script>

<style scoped>
.home-page {
  background: #f5f8f6;
  color: #163126;
}

.hero-section {
  position: relative;
  min-height: 720px;
  background-size: cover;
  background-position: center;
  overflow: hidden;
}

.hero-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(7, 25, 20, 0.72) 0%, rgba(11, 45, 34, 0.58) 38%, rgba(16, 42, 33, 0.38) 100%);
}

.hero-inner {
  position: relative;
  z-index: 2;
  max-width: 1280px;
  margin: 0 auto;
  min-height: 720px;
  padding: 110px 24px 80px;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) 380px;
  gap: 32px;
  align-items: center;
}

.hero-content {
  color: #fff;
  max-width: 720px;
}

.hero-tag,
.section-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  font-size: 12px;
  letter-spacing: 0.12em;
  margin-bottom: 18px;
}

.section-tag {
  background: rgba(31, 98, 68, 0.10);
  color: #1f6244;
}

.hero-title {
  margin: 0 0 20px;
  font-size: clamp(42px, 5vw, 74px);
  line-height: 1.05;
  font-weight: 700;
}

.hero-desc {
  max-width: 620px;
  margin: 0 0 32px;
  font-size: 17px;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.92);
}

.hero-actions {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.hero-btn,
.card-btn,
.quick-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  cursor: pointer;
  transition: 0.25s ease;
}

.hero-btn {
  min-width: 144px;
  height: 48px;
  padding: 0 22px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 600;
}

.hero-btn.primary {
  background: linear-gradient(135deg, #1f6244, #256f4d);
  color: #fff;
  box-shadow: 0 12px 24px rgba(31, 98, 68, 0.22);
}

.hero-btn.secondary {
  background: linear-gradient(135deg, #1f6244, #2d7a56);
  color: #fff;
  border: 1px solid rgba(31, 98, 68, 0.18);
  box-shadow: 0 12px 24px rgba(31, 98, 68, 0.18);
}

.hero-btn:hover,
.card-btn:hover,
.quick-link:hover {
  transform: translateY(-2px);
}

.hero-side-card {
  position: relative;
  z-index: 2;
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.22);
}

.side-card-title {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 12px;
}

.side-card-item {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 12px;
  padding: 10px;
  border-radius: 16px;
  margin-bottom: 10px;
  background: rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: 0.2s ease;
}

.side-card-item.active,
.side-card-item:hover {
  background: rgba(255, 255, 255, 0.18);
}

.side-card-item img {
  width: 92px;
  height: 72px;
  object-fit: cover;
  border-radius: 12px;
}

.side-card-text h4 {
  margin: 0 0 6px;
  color: #fff;
  font-size: 15px;
}

.side-card-text p {
  margin: 0;
  color: rgba(255, 255, 255, 0.82);
  font-size: 13px;
  line-height: 1.6;
}

.hero-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 3;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-size: 34px;
  cursor: pointer;
  backdrop-filter: blur(10px);
}

.hero-nav.prev { left: 20px; }
.hero-nav.next { right: 20px; }

.hero-indicators {
  position: absolute;
  left: 50%;
  bottom: 28px;
  transform: translateX(-50%);
  z-index: 3;
  display: flex;
  gap: 10px;
}

.indicator {
  width: 28px;
  height: 4px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.4);
  cursor: pointer;
}

.indicator.active {
  background: #fff;
}


.section-block {
  max-width: 1280px;
  margin: 0 auto;
  padding: 88px 24px 0;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}

.section-head h2,
.guide-panel h2 {
  margin: 0 0 10px;
  font-size: 34px;
  color: #143629;
}

.section-head p,
.guide-panel p {
  margin: 0;
  color: #5b7067;
  line-height: 1.9;
}

.scenic-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}

.scenic-card {
  background: #fff;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 18px 40px rgba(26, 61, 47, 0.08);
  cursor: pointer;
  transition: 0.25s ease;
}

.scenic-card:hover {
  transform: translateY(-6px);
}

.scenic-image-wrap {
  position: relative;
  height: 260px;
}

.scenic-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.scenic-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 88px;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  color: #17513a;
  font-size: 13px;
  font-weight: 600;
}

.scenic-body {
  padding: 22px;
}

.scenic-body h3 {
  margin: 0 0 10px;
  font-size: 22px;
  color: #173629;
}

.scenic-body p {
  margin: 0 0 18px;
  color: #5a7067;
  line-height: 1.8;
  min-height: 58px;
}

.card-btn {
  min-width: 116px;
  height: 42px;
  padding: 0 18px;
  border-radius: 999px;
  background: linear-gradient(135deg, #1f6244, #256f4d);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 12px 24px rgba(31, 98, 68, 0.18);
}

.guide-section {
  padding-bottom: 88px;
}

.guide-section.single-column {
  display: block;
}

.guide-panel {
  background: #fff;
  border-radius: 26px;
  padding: 30px;
  box-shadow: 0 18px 40px rgba(26, 61, 47, 0.08);
}

.guide-points {
  display: grid;
  gap: 16px;
  margin-top: 24px;
}

.guide-point {
  display: grid;
  grid-template-columns: 58px 1fr;
  gap: 14px;
  align-items: start;
}

.guide-point span {
  width: 58px;
  height: 58px;
  border-radius: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f6244, #3d8e66);
  color: #fff;
  font-weight: 700;
  font-size: 18px;
}

.guide-point h4 {
  margin: 0 0 6px;
  font-size: 18px;
  color: #173629;
}

.guide-point p {
  margin: 0;
}


@media (max-width: 1100px) {
  .hero-inner,
  .guide-section {
    grid-template-columns: 1fr;
  }

  .scenic-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .hero-section {
    min-height: 640px;
  }

  .hero-inner {
    min-height: 640px;
    padding-top: 88px;
  }

  .scenic-grid {
    grid-template-columns: 1fr;
  }

  .hero-nav {
    display: none;
  }

  .hero-title {
    font-size: 42px;
  }

  .section-head h2,
  .guide-panel h2 {
    font-size: 28px;
  }

  .guide-panel {
    padding: 24px;
  }
}
</style>

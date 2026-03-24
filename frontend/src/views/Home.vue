<template>
  <div class="home-container">
    <div class="carousel">
      <div class="carousel-track" :style="{ transform: `translateX(-${currentSlide * 100}%)` }">
        <div v-for="(slide, index) in slides" :key="index" class="carousel-slide" @click="goToScenic(slide)">
          <img :src="getImageUrl(slide.image)" :alt="slide.name" class="slide-image">
          <div class="slide-overlay">
            <h3>{{ slide.name }}</h3>
            <p>{{ slide.description }}</p>
          </div>
        </div>
      </div>
      <button class="carousel-btn prev" @click="prevSlide">❮</button>
      <button class="carousel-btn next" @click="nextSlide">❯</button>
      <div class="carousel-indicators">
        <span 
          v-for="(slide, index) in slides" 
          :key="index"
          :class="['indicator', { active: index === currentSlide }]"
          @click="goToSlide(index)"
        ></span>
      </div>
    </div>
    
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
  mounted() {
    this.loadScenicAreas()
  },
  beforeUnmount() {
    this.stopAutoPlay()
  },
  methods: {
    getImageUrl(imageUrl) {
      if (!imageUrl) return ''
      if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
        return imageUrl
      }
      return `http://localhost:8080${imageUrl}`
    },
    async loadScenicAreas() {
      try {
        const response = await apiClient.get('/api/large-areas')
        const areas = response.data
        const scenicAreas = areas.filter(area => area.isAreaType === 0)
        this.slides = scenicAreas.slice(0, 6).map(area => ({
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
      this.autoPlayInterval = setInterval(() => {
        this.nextSlide()
      }, 5000)
    },
    stopAutoPlay() {
      if (this.autoPlayInterval) {
        clearInterval(this.autoPlayInterval)
      }
    },
    resetAutoPlay() {
      this.stopAutoPlay()
      this.startAutoPlay()
    }
  }
}
</script>

<style scoped>
@import '@/assets/css/home.css';

.slide-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
  color: white;
  padding: 30px 20px 20px;
  text-align: left;
}

.slide-overlay h3 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
}

.slide-overlay p {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 80%;
}

.carousel-slide {
  position: relative;
  cursor: pointer;
}
</style>

<template>
  <div class="home-container">
    <!-- 轮播图 -->
    <div class="carousel">
      <div class="carousel-track" :style="{ transform: `translateX(-${currentSlide * 100}%)` }">
        <div v-for="(slide, index) in slides" :key="index" class="carousel-slide">
          <img :src="slide.image" :alt="'Slide ' + (index + 1)" class="slide-image">
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
    
    <!-- 推荐产品 -->
    <div class="recommended-products">
      <h2>🔥 热门推荐</h2>
      <div class="products-grid" v-if="products.length > 0">
        <div v-for="product in products" :key="product.id" class="product-card">
          <img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.name" class="product-image">
          <div v-else class="product-image-placeholder">🏞️</div>
          <h3>{{ product.name }}</h3>
          <p class="product-desc">{{ product.description || '暂无描述' }}</p>
          <div class="product-footer">
            <span class="price">¥{{ product.price }}</span>
            <button class="detail-btn" @click="viewProductDetail(product)">查看详情</button>
            <button class="book-btn" @click="bookProduct(product)">立即预订</button>
          </div>
        </div>
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
      slides: [
        {
          image: 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1200&h=500&fit=crop'
        },
        {
          image: 'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=1200&h=500&fit=crop'
        },
        {
          image: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=1200&h=500&fit=crop'
        },
        {
          image: 'https://images.unsplash.com/photo-1436491865332-7a61a109cc05?w=1200&h=500&fit=crop'
        }
      ],
      products: []
    }
  },
  mounted() {
    this.startAutoPlay()
    this.loadProducts()
  },
  beforeUnmount() {
    this.stopAutoPlay()
  },
  methods: {
    async loadProducts() {
      try {
        const response = await apiClient.get('/api/products/on-sale')
        this.products = response.data
      } catch (error) {
        console.error('加载产品失败:', error)
      }
    },
    bookProduct(product) {
      // 将产品添加到购物车
      const cartItem = {
        userId: localStorage.getItem('userId'),
        productId: product.id,
        productName: product.name,
        price: product.price,
        features: product.description ? product.description : ''
      }
      
      apiClient.post('/api/cart', cartItem)
        .then(() => {
          alert('已添加到购物车')
          this.$router.push('/shopping-cart')
        })
        .catch(error => {
          console.error('添加失败:', error)
          alert('添加失败，请重试')
        })
    },
    viewProductDetail(product) {
      // 跳转到产品详情页
      this.$router.push(`/product/${product.id}`)
    },
    nextSlide() {
      this.currentSlide = (this.currentSlide + 1) % this.slides.length
    },
    prevSlide() {
      this.currentSlide = (this.currentSlide - 1 + this.slides.length) % this.slides.length
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
</style>
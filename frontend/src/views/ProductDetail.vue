<template>
  <div class="portal-page product-page">
    <div class="portal-wrap portal-grid two product-layout">
      <section class="portal-hero product-hero">
        <div class="portal-pill light">PRODUCT DETAIL</div>
        <h1 class="portal-title">{{ product.name || '商品详情' }}</h1>
        <p class="portal-subtitle">
          {{ product.description || '先了解套餐包含的游玩内容、价格和出游节奏，再判断它是否适合你的时间安排与旅行偏好。' }}
        </p>
      </section>

      <section class="portal-surface portal-panel">
        <img :src="product.image" :alt="product.name" class="product-image" />
        <div class="portal-kv product-kv">
          <div class="portal-kv-item">
            <strong>商品价格</strong>
            <span class="portal-price">¥{{ product.price }}</span>
          </div>
          <div class="portal-kv-item">
            <strong>库存</strong>
            <span>{{ product.stock }}</span>
          </div>
        </div>
        <p class="product-description">{{ product.description }}</p>
        <div class="portal-actions">
          <button class="portal-btn primary" @click="addToCart">加入购物车</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ProductDetail',
  data() {
    return {
      product: {
        id: '',
        name: '',
        price: 0,
        description: '',
        image: '',
        stock: 0
      }
    }
  },
  created() {
    this.getProductDetail()
  },
  methods: {
    getProductDetail() {
      const productId = this.$route.params.id
      this.product = {
        id: productId,
        name: '特色手工艺品',
        price: 199,
        description: '这是一个精美的特色手工艺品，具有浓郁的地方特色，是送礼和收藏的绝佳选择。',
        image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=traditional%20handicraft%20souvenir&image_size=square',
        stock: 50
      }
    },
    addToCart() {
      alert('已加入购物车')
    }
  }
}
</script>

<style scoped>
@import '@/assets/css/portal-theme.css';

.product-layout {
  align-items: stretch;
}

.product-hero {
  min-height: 100%;
}

.product-image {
  width: 100%;
  height: 340px;
  object-fit: cover;
  border-radius: 24px;
}

.product-kv {
  margin-top: 20px;
}

.product-description {
  margin-top: 20px;
  color: var(--portal-muted);
  line-height: 1.9;
}
</style>

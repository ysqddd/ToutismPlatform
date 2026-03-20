<template>
  <div class="product-detail">
    <div class="container">
      <h1>{{ product.name }}</h1>
      <div class="product-info">
        <div class="product-image">
          <img :src="product.image" :alt="product.name">
        </div>
        <div class="product-details">
          <p class="price">¥{{ product.price }}</p>
          <p class="description">{{ product.description }}</p>
          <p class="stock">库存: {{ product.stock }}</p>
          <button class="add-to-cart" @click="addToCart">加入购物车</button>
        </div>
      </div>
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
      // 从路由参数中获取商品ID
      const productId = this.$route.params.id
      // 这里应该调用API获取商品详情
      // 暂时使用模拟数据
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
      // 加入购物车的逻辑
      alert('已加入购物车')
    }
  }
}
</script>

<style scoped>
.product-detail {
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

.product-info {
  display: flex;
  gap: 40px;
}

.product-image {
  flex: 1;
  max-width: 500px;
}

.product-image img {
  width: 100%;
  height: auto;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.product-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.price {
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

.stock {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.add-to-cart {
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

.add-to-cart:hover {
  background-color: #2980b9;
}

@media (max-width: 768px) {
  .product-info {
    flex-direction: column;
  }
  
  .product-image {
    max-width: 100%;
  }
}
</style>
<template>
  <div class="subscription-container">
    <h2 class="page-title">📝 在线预定</h2>
    
    <div class="subscription-plans">
      <div 
        v-for="(plan, index) in plans" 
        :key="index" 
        class="plan-card"
        @click="selectPlan(plan)"
      >
        <div class="plan-image" v-if="plan.imageUrl">
          <img :src="getImageUrl(plan.imageUrl)" :alt="plan.name">
        </div>
        <div class="plan-image-placeholder" v-else>
          <span>🏞️</span>
        </div>
        <h3>{{ plan.name }}</h3>
        <div class="plan-price">
          <span class="currency">¥</span>
          <span class="amount">{{ plan.price }}</span>
        </div>
        <ul class="plan-features">
          <li v-for="(feature, idx) in plan.features" :key="idx">
            ✓ {{ feature }}
          </li>
        </ul>
        <button 
          class="subscribe-btn"
          @click.stop="selectPlan(plan)"
        >
          立即预定
        </button>
      </div>
    </div>
    

  </div>
</template>

<script>
import apiClient from '@/utils/axios'

export default {
  name: 'Subscription',
  data() {
    return {
      plans: [],
      formData: {
        contact: '',
        phone: '',
        email: '',
        plan: '',
        remark: ''
      }
    }
  },
  mounted() {
    this.loadPlans()
  },
  methods: {
    getImageUrl(imageUrl) {
      if (!imageUrl) return ''
      if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://') || imageUrl.startsWith('data:')) {
        return imageUrl
      }
      return `http://localhost:8080${imageUrl}`
    },
    async loadPlans() {
      try {
        const response = await apiClient.get('/api/products/on-sale')
        this.plans = response.data.map(product => ({
          id: product.id,
          name: product.name,
          price: parseFloat(product.price),
          features: product.description ? [product.description] : ['详情请咨询'],
          productId: product.id,
          imageUrl: product.imageUrl
        }))
      } catch (error) {
        console.error('加载产品失败:', error)
      }
    },
    selectPlan(plan) {
      const userId = localStorage.getItem('userId')
      if (!userId) {
        alert('请先登录')
        this.$router.push('/login')
        return
      }
      
      apiClient.post(`/api/cart/product?userId=${userId}&productId=${plan.productId}`)
        .then(() => {
          alert('已添加到购物车')
          this.$router.push('/shopping-cart')
        })
        .catch(error => {
          console.error('添加失败:', error)
          alert('添加失败，请重试')
        })
    },
    handleSubmit() {
      console.log('提交订阅信息:', this.formData)
      alert('订阅申请已提交，我们会尽快与您联系！')
    }
  }
}
</script>

<style scoped>
.subscription-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-title {
  text-align: center;
  font-size: 28px;
  color: #333;
  margin-bottom: 30px;
}

.subscription-plans {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.plan-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  position: relative;
  overflow: hidden;
}

.plan-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.plan-image {
  width: 100%;
  height: 160px;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 15px;
}

.plan-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.plan-card:hover .plan-image img {
  transform: scale(1.05);
}

.plan-image-placeholder {
  width: 100%;
  height: 160px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 15px;
}

.plan-image-placeholder span {
  font-size: 48px;
}

.plan-card h3 {
  font-size: 18px;
  color: #333;
  margin: 0 0 10px 0;
}

.plan-price {
  margin-bottom: 15px;
}

.plan-price .currency {
  font-size: 16px;
  color: #ff6b6b;
}

.plan-price .amount {
  font-size: 32px;
  font-weight: bold;
  color: #ff6b6b;
}

.plan-features {
  list-style: none;
  padding: 0;
  margin: 0 0 20px 0;
}

.plan-features li {
  font-size: 14px;
  color: #666;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-features li:last-child {
  border-bottom: none;
}

.subscribe-btn {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  transition: opacity 0.3s;
}

.subscribe-btn:hover {
  opacity: 0.9;
}
</style>

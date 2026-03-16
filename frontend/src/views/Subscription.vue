<template>
  <div class="subscription-container">
    <h2 class="page-title">📝 在线预定</h2>
    
    <div class="subscription-plans">
      <div 
        v-for="(plan, index) in plans" 
        :key="index" 
        :class="['plan-card', { recommended: plan.recommended }]"
        @click="selectPlan(plan)"
      >
        <div v-if="plan.recommended" class="recommended-tag">热门推荐</div>
        <h3>{{ plan.name }}</h3>
        <div class="plan-price">
          <span class="currency">¥</span>
          <span class="amount">{{ plan.price }}</span>
          <span class="period">/年</span>
        </div>
        <ul class="plan-features">
          <li v-for="(feature, idx) in plan.features" :key="idx">
            ✓ {{ feature }}
          </li>
        </ul>
        <button 
          :class="['subscribe-btn', { recommended: plan.recommended }]"
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
    async loadPlans() {
      try {
        const response = await apiClient.get('/api/products/on-sale')
        // 将后端产品数据转换为前端套餐格式
        this.plans = response.data.map(product => ({
          id: product.id,
          name: product.name,
          price: parseFloat(product.price),
          recommended: product.price >= 300, // 价格大于 300 的设为推荐
          features: product.description ? [product.description] : ['详情请咨询'],
          productId: product.id
        }))
      } catch (error) {
        console.error('加载产品失败:', error)
      }
    },
    selectPlan(plan) {
      // 将套餐添加到购物车
      const cartItem = {
        userId: localStorage.getItem('userId'),
        productId: plan.productId,
        productName: plan.name,
        price: plan.price,
        features: plan.features
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
    handleSubmit() {
      console.log('提交订阅信息:', this.formData)
      alert('订阅申请已提交，我们会尽快与您联系！')
    }
  }
}
</script>

<style scoped>
@import '@/assets/css/subscription.css';
</style>

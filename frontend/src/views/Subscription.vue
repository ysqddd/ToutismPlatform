<template>
  <div class="portal-page subscription-page">
    <div class="portal-wrap">
      <section class="portal-hero subscription-hero">
        <div class="portal-pill light">TRAVEL PACKAGE</div>
        <h1 class="portal-title">为你准备好的旅行套餐</h1>
        <p class="portal-subtitle">
          如果你想省去自己搭配路线的时间，可以直接查看这些整理好的套餐方案，
          更快找到适合预算和出游节奏的选择。
        </p>
      </section>

      <section class="portal-section">
        <div class="portal-section-head">
          <div>
            <div class="portal-pill soft">BOOK NOW</div>
            <h2>甄选套餐</h2>
            <p>点击卡片或按钮即可直接加入购物车，保留现有业务逻辑。</p>
          </div>
        </div>

        <div v-if="plans.length" class="portal-grid three">
          <article
            v-for="(plan, index) in plans"
            :key="plan.id || index"
            class="portal-card plan-card"
            @click="selectPlan(plan)"
          >
            <img v-if="plan.imageUrl" :src="getImageUrl(plan.imageUrl)" :alt="plan.name" class="portal-media" />
            <div v-else class="portal-media portal-placeholder">🏞️</div>
            <div class="portal-card-body">
              <div class="plan-topline">
                <div class="portal-pill soft">轻松成行</div>
                <div class="plan-price">¥{{ plan.price }}</div>
              </div>
              <h3>{{ plan.name }}</h3>
              <ul class="plan-features">
                <li v-for="(feature, idx) in plan.features" :key="idx">{{ feature }}</li>
              </ul>
              <button class="portal-btn primary plan-btn" @click.stop="selectPlan(plan)">立即预定</button>
            </div>
          </article>
        </div>

        <div v-else class="portal-surface portal-empty">
          <strong>套餐加载中</strong>
          <p class="portal-subtitle dark">商品数据接入完成后，这里会展示封面、价格和核心亮点。</p>
        </div>
      </section>
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
@import '@/assets/css/portal-theme.css';

.subscription-hero {
  background:
    linear-gradient(120deg, rgba(10, 39, 28, 0.92), rgba(35, 89, 63, 0.8)),
    url('http://127.0.0.1:8080/images/package-classic-one-day.jpg') center/cover;
}

.plan-card {
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.plan-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--portal-shadow-lg);
}

.portal-card-body {
  display: flex;
  flex: 1;
  flex-direction: column;
}

.plan-topline {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
}

.plan-price {
  font-size: 30px;
  font-weight: 700;
  color: var(--portal-danger);
}

.plan-features {
  list-style: none;
  margin: 18px 0 22px;
  padding: 0;
  display: grid;
  gap: 10px;
  flex: 1;
  align-content: start;
}

.plan-features li {
  padding: 12px 14px;
  border-radius: 16px;
  background: #f7faf8;
  color: var(--portal-muted);
  line-height: 1.7;
}

.plan-btn {
  width: 100%;
  min-height: 48px;
  margin-top: auto;
}
</style>

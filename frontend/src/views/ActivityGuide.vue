<template>
  <div class="portal-page activity-page">
    <div class="portal-wrap">
      <section class="portal-hero activity-hero">
        <div class="portal-pill light">ACTIVITY GUIDE</div>
        <h1 class="portal-title">你的出游灵感库</h1>
        <p class="portal-subtitle">不知道怎么安排行程时，可以先从这里看看热门玩法、精选路线和行前建议，
          更快找到适合自己的一次旅行。</p>
      </section>

      <section class="portal-section">
        <div class="portal-grid three">
          <article v-for="(activity, index) in activities" :key="index" class="portal-card">
            <div class="portal-media portal-placeholder">{{ activity.emoji }}</div>
            <div class="portal-card-body">
              <div class="portal-pill soft">{{ activity.tag }}</div>
              <h3>{{ activity.title }}</h3>
              <p>{{ activity.description }}</p>
              <div class="portal-actions">
                <span class="portal-price">¥{{ activity.price }} 起</span>
                <button class="portal-btn primary" @click="viewDetails(activity)">查看详情</button>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section class="portal-section portal-surface portal-panel">
        <div class="portal-section-head">
          <div>
            <div class="portal-pill soft">TRAVEL TIPS</div>
            <h2>游玩攻略</h2>
          </div>
        </div>
        <div class="portal-grid two">
          <article v-for="(guide, index) in guides" :key="index" class="portal-list-item guide-card">
            <div class="guide-icon">{{ guide.icon }}</div>
            <div>
              <h3>{{ guide.title }}</h3>
              <p>{{ guide.content }}</p>
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import apiClient from '@/utils/axios'

export default {
  name: 'ActivityGuide',
  data() {
    return {
      activities: [],
      guides: [
        { icon: '📅', title: '最佳出行时间', content: '春秋季节是旅游的最佳时机，天气宜人，景色优美' },
        { icon: '🎒', title: '行前准备', content: '带好身份证、充电宝、常用药品等必需品' },
        { icon: '📸', title: '拍照技巧', content: '选择光线好的时间段，利用构图技巧拍出美照' },
        { icon: '🍜', title: '美食推荐', content: '品尝当地特色美食，注意饮食卫生' }
      ]
    }
  },
  mounted() {
    this.loadActivities()
  },
  methods: {
    async loadActivities() {
      try {
        const response = await apiClient.get('/api/products/on-sale')
        this.activities = response.data.map(product => ({
          id: product.id,
          emoji: '🎯',
          title: product.name,
          description: product.description || '精彩活动，详情咨询',
          tag: '热门',
          price: parseFloat(product.price),
          productId: product.id
        }))
      } catch (error) {
        console.error('加载活动失败:', error)
      }
    },
    viewDetails(activity) {
      alert(`即将查看 ${activity.title} 的详情`)
    }
  }
}
</script>

<style scoped>
@import '@/assets/css/portal-theme.css';

.activity-hero {
  background: linear-gradient(135deg, rgba(13, 45, 33, 0.95), rgba(31, 98, 68, 0.82));
}

.guide-card {
  grid-template-columns: 64px 1fr;
  align-items: start;
}

.guide-icon {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f6244, #3d8e66);
  color: #fff;
  font-size: 28px;
}
</style>

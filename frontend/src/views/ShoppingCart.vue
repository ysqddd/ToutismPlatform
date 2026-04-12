<template>
  <div class="portal-page cart-page">
    <div class="portal-wrap">
      <section class="portal-hero cart-hero">
        <div class="portal-pill light">SHOPPING CART</div>
        <h1 class="portal-title">确认你的旅行清单</h1>
        <p class="portal-subtitle">
          这里会汇总你已经选好的景区和套餐，方便你统一调整数量、核对金额，
          再安心进入下一步预订流程。
        </p>
      </section>

      <section class="portal-section">
        <div v-if="cartItems.length === 0" class="portal-surface portal-empty">
          <strong>购物车空空如也</strong>
          <p class="portal-subtitle dark">先去看看推荐套餐或景区门票，把心仪项目加入你的旅程。</p>
          <div class="portal-actions empty-actions">
            <button class="portal-btn primary" @click="goToSubscription">去逛逛</button>
          </div>
        </div>

        <div v-else class="portal-grid two cart-layout">
          <div class="portal-surface portal-panel">
            <div class="portal-section-head">
              <div>
                <div class="portal-pill soft">ITEMS</div>
                <h2>已选商品</h2>
              </div>
            </div>

            <div class="portal-list">
              <article
                v-for="(item, index) in cartItems"
                :key="item.id"
                class="portal-list-item cart-item"
              >
                <div class="cart-item-head">
                  <div class="portal-pill soft item-tag">
                    {{ item.itemType === 'PRODUCT' || item.itemType === 'PACKAGE' ? '套餐' : '景区门票' }}
                  </div>
                  <button class="remove-btn" @click="removeItem(index)">删除</button>
                </div>

                <div class="cart-item-main">
                  <div class="cart-item-image">
                    <img
                      v-if="item.imageUrl && !item.imageError"
                      :src="getImageUrl(item.imageUrl)"
                      :alt="item.itemName"
                      class="cart-image"
                      @error="markImageError(item.id)"
                    >
                    <div v-else class="portal-placeholder cart-image">旅行商品</div>
                  </div>

                  <div class="cart-item-body">
                    <h3>{{ item.itemName }}</h3>
                    <p v-if="item.features">{{ item.features }}</p>
                    <div class="cart-price-row">
                      <span class="portal-price">¥{{ item.price }}</span>
                      <span>小计：¥{{ (item.price * item.quantity).toFixed(2) }}</span>
                    </div>
                    <div class="quantity-control">
                      <button class="quantity-btn" @click="updateQuantity(item, -1)">-</button>
                      <span class="quantity-value">{{ item.quantity }}</span>
                      <button class="quantity-btn" @click="updateQuantity(item, 1)">+</button>
                    </div>
                  </div>
                </div>
              </article>
            </div>
          </div>

          <aside class="portal-surface portal-panel summary-panel">
            <div class="portal-pill soft">SUMMARY</div>
            <h2>结算概览</h2>
            <div class="portal-kv">
              <div class="portal-kv-item">
                <strong>商品数量</strong>
                <span>{{ totalQuantity }} 件</span>
              </div>
              <div class="portal-kv-item">
                <strong>预计总价</strong>
                <span class="portal-price">¥{{ totalPrice }}</span>
              </div>
            </div>
            <div class="portal-actions summary-actions">
              <button class="portal-btn primary" @click="checkout">去结算</button>
            </div>
          </aside>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import apiClient from '@/utils/axios'

export default {
  name: 'ShoppingCart',
  data() {
    return {
      cartItems: [],
      userId: null,
      username: ''
    }
  },
  computed: {
    totalPrice() {
      return this.cartItems
        .reduce((sum, item) => sum + (parseFloat(item.price) * item.quantity || 0), 0)
        .toFixed(2)
    },
    totalQuantity() {
      return this.cartItems.reduce((sum, item) => sum + item.quantity, 0)
    },
    backendBaseUrl() {
      const apiBase = apiClient?.defaults?.baseURL || ''
      if (!apiBase) return window.location.origin
      if (/^https?:\/\//i.test(apiBase)) return apiBase.replace(/\/$/, '')
      return window.location.origin
    }
  },
  async created() {
    await this.loadUserInfo()
  },
  mounted() {
    window.addEventListener('storage', this.handleStorageChange)
  },
  beforeUnmount() {
    window.removeEventListener('storage', this.handleStorageChange)
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
    markImageError(id) {
      this.cartItems = this.cartItems.map(item => item.id === id ? { ...item, imageError: true } : item)
    },
    async loadUserInfo() {
      const savedUserId = localStorage.getItem('userId')
      const savedUsername = localStorage.getItem('username')
      if (savedUserId) this.userId = Number(savedUserId)
      if (savedUsername) this.username = savedUsername

      try {
        const response = await apiClient.get('/api/auth/current-user')
        if (response && response.data) {
          this.userId = response.data.id != null ? Number(response.data.id) : this.userId
          this.username = response.data.username || this.username || ''
          if (this.userId != null && !Number.isNaN(this.userId)) {
            localStorage.setItem('userId', String(this.userId))
          }
          if (this.username) {
            localStorage.setItem('username', this.username)
          }
        }
      } catch (error) {
        console.error('获取用户信息失败，已尝试使用本地缓存:', error)
      }

      await this.loadCart()
    },
    async loadCart() {
      if (this.userId == null || Number.isNaN(this.userId)) return

      try {
        let response
        try {
          response = await apiClient.get('/api/cart/me')
        } catch (e) {
          response = await apiClient.get(`/api/cart?userId=${this.userId}`)
        }

        this.cartItems = (response.data || []).map(item => ({
          id: item.id,
          itemType: String(item.itemType || 'PRODUCT').toUpperCase(),
          itemId: item.itemId,
          itemName: item.itemName || item.name || item.productName || '未命名商品',
          price: Number(item.price || 0),
          imageUrl: item.imageUrl || item.image || '',
          features: item.features,
          quantity: item.quantity || 1,
          imageError: false
        }))
        localStorage.removeItem('shoppingCart')
      } catch (error) {
        console.error('加载购物车失败:', error)
      }
    },
    async updateQuantity(item, delta) {
      const newQuantity = item.quantity + delta
      if (newQuantity < 1) return

      try {
        await apiClient.put(`/api/cart/${item.id}/quantity?quantity=${newQuantity}`)
        item.quantity = newQuantity
        localStorage.setItem('cartRefreshAt', String(Date.now()))
      } catch (error) {
        console.error('更新数量失败:', error)
        alert('更新失败，请重试')
      }
    },
    async removeItem(index) {
      const item = this.cartItems[index]
      try {
        if (item.id) {
          await apiClient.delete(`/api/cart/${item.id}?userId=${this.userId}`)
        }
        this.cartItems.splice(index, 1)
        localStorage.setItem('cartRefreshAt', String(Date.now()))
      } catch (error) {
        console.error('删除商品失败:', error)
        alert('删除失败，请重试')
      }
    },
    async checkout() {
      try {
        const orderData = {
          userId: this.userId,
          items: this.cartItems,
          totalAmount: this.totalPrice
        }
        console.log('待创建订单数据:', orderData)
        alert('跳转到支付页面，总计：¥' + this.totalPrice)
      } catch (error) {
        console.error('创建订单失败:', error)
        alert('创建订单失败，请重试')
      }
    },
    async clearCart() {
      try {
        await apiClient.delete(`/api/cart?userId=${this.userId}`)
        this.cartItems = []
        localStorage.setItem('cartRefreshAt', String(Date.now()))
      } catch (error) {
        console.error('清空购物车失败:', error)
      }
    },
    goToSubscription() {
      this.$router.push('/subscription')
    },
    handleStorageChange(event) {
      if (event.key === 'cartRefreshAt') {
        this.loadCart()
      }
      if (event.key === 'userId' || event.key === 'username') {
        this.loadUserInfo()
      }
    }
  },
  watch: {
    '$route'() {
      this.loadCart()
    }
  }
}
</script>

<style scoped>
@import '@/assets/css/portal-theme.css';

.cart-hero {
  background:
    linear-gradient(120deg, rgba(10, 39, 28, 0.92), rgba(35, 89, 63, 0.82)),
    url('http://127.0.0.1:8080/images/package-two-day-deep.jpg') center/cover;
}

.cart-layout {
  align-items: start;
}

.cart-item {
  background: #fff;
}

.cart-item-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.remove-btn {
  min-height: 38px;
  padding: 0 18px;
  border-radius: 999px;
  border: none;
  background: #fff1ee;
  color: var(--portal-danger);
}

.cart-item-main {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 18px;
  align-items: center;
}

.cart-item-image,
.cart-image {
  width: 100%;
  height: 132px;
  border-radius: 20px;
  object-fit: cover;
}

.cart-item-body h3 {
  margin: 0 0 8px;
}

.cart-price-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin: 14px 0;
  color: var(--portal-muted);
}

.quantity-control {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, #dfece5, #edf5f0);
  border: 1px solid rgba(31, 98, 68, 0.14);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.75);
}

.quantity-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #1f6244, #2e7b56);
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
  box-shadow: 0 10px 18px rgba(31, 98, 68, 0.18);
}

.quantity-value {
  min-width: 44px;
  padding: 0 6px;
  text-align: center;
  font-weight: 700;
  font-size: 24px;
  color: var(--portal-text);
}

.summary-panel {
  position: sticky;
  top: 96px;
}

.summary-actions .portal-btn {
  width: 100%;
}

.empty-actions {
  justify-content: center;
}

@media (max-width: 768px) {
  .cart-item-main {
    grid-template-columns: 1fr;
  }
}
</style>

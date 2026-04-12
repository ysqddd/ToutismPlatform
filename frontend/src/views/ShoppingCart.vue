<template>
  <div class="shopping-cart-container">
    <h2 class="page-title">🛒 购物车</h2>

    <div v-if="cartItems.length === 0" class="empty-cart">
      <div class="empty-cart-icon">🛒</div>
      <p>购物车空空如也</p>
      <button class="back-btn" @click="goToSubscription">去逛逛</button>
    </div>

    <div v-else class="cart-items">
      <div
          v-for="(item, index) in cartItems"
          :key="item.id"
          class="cart-item-card"
      >
        <div class="cart-item-header">
          <div class="item-type-badge">
            {{ item.itemType === 'PRODUCT' || item.itemType === 'PACKAGE' ? '🎁 套餐' : '🎫 景区门票' }}
          </div>
          <h3>{{ item.itemName }}</h3>
          <button class="remove-btn" @click="removeItem(index)">×</button>
        </div>

        <div class="cart-item-image">
          <img
              v-if="item.imageUrl && !item.imageError"
              :src="getImageUrl(item.imageUrl)"
              :alt="item.itemName"
              @error="markImageError(item.id)"
          >
          <div v-else class="cart-image-placeholder">
            {{ item.itemType === 'PRODUCT' || item.itemType === 'PACKAGE' ? '套餐图片' : '景区图片' }}
          </div>
        </div>

        <div class="cart-item-price">
          <span class="currency">¥</span>
          <span class="amount">{{ item.price }}</span>
        </div>

        <div class="quantity-control">
          <button class="quantity-btn" @click="updateQuantity(item, -1)">-</button>
          <span class="quantity-value">{{ item.quantity }}</span>
          <button class="quantity-btn" @click="updateQuantity(item, 1)">+</button>
        </div>

        <div class="cart-item-total">
          小计：¥{{ (item.price * item.quantity).toFixed(2) }}
        </div>

        <div v-if="item.features" class="cart-item-features">
          <p>{{ item.features }}</p>
        </div>
      </div>

      <div class="cart-summary">
        <div class="summary-row">
          <span>商品数量：</span>
          <span>{{ totalQuantity }} 件</span>
        </div>
        <div class="summary-row total">
          <span>总计：</span>
          <span class="total-amount">¥{{ totalPrice }}</span>
        </div>
        <button class="checkout-btn" @click="checkout">去结算</button>
      </div>
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

      if (savedUserId) {
        this.userId = Number(savedUserId)
      }
      if (savedUsername) {
        this.username = savedUsername
      }

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
      console.log('结算商品:', this.cartItems)
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
.shopping-cart-container {
  width: 100%;
  padding: 40px 20px 20px;
  max-width: 800px;
  margin: 0 auto;
  min-height: calc(100vh - 60px);
  box-sizing: border-box;
}
.page-title {
  font-size: 28px;
  color: #667eea;
  margin-bottom: 30px;
  text-align: center;
  margin-top: 0;
  padding-top: 0;
}
.empty-cart {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}
.empty-cart-icon { font-size: 80px; margin-bottom: 20px; }
.empty-cart p { font-size: 18px; color: #666; margin-bottom: 30px; }
.back-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 25px;
  cursor: pointer;
  font-size: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1.2;
  box-sizing: border-box;
}
.cart-items { display: flex; flex-direction: column; gap: 20px; }
.cart-item-card {
  background: white; border-radius: 15px; padding: 20px; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}
.cart-item-header {
  display: flex; align-items: center; gap: 15px; margin-bottom: 15px;
}
.item-type-badge {
  background: #f0f8ff; color: #667eea; padding: 4px 8px; border-radius: 12px; font-size: 12px; font-weight: bold;
}
.cart-item-header h3 { flex: 1; margin: 0; color: #333; font-size: 20px; }
.remove-btn {
  background: #ff4757;
  color: white;
  border: none;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  padding: 0;
  box-sizing: border-box;
}
.cart-item-image {
  margin-bottom: 15px; width: 100%; height: 220px; border-radius: 10px; overflow: hidden; background: #f5f7fa;
}
.cart-item-image img {
  width: 100%; height: 100%; object-fit: cover; display: block;
}
.cart-image-placeholder {
  width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: #666; background: linear-gradient(135deg, #f5f7fa, #e4eaf3);
}
.cart-item-price { margin-bottom: 10px; text-align: center; }
.currency { font-size: 24px; color: #e74c3c; }
.amount { font-size: 36px; font-weight: bold; color: #e74c3c; }
.quantity-control {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin: 18px 0 14px;
}
.quantity-btn {
  width: 44px;
  height: 44px;
  border: 2px solid #667eea;
  background: #fff;
  color: #667eea;
  border-radius: 50%;
  cursor: pointer;
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-sizing: border-box;
}
.quantity-value {
  width: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: #333;
  line-height: 1;
  text-align: center;
}
.cart-item-total { text-align: center; font-size: 18px; font-weight: bold; color: #667eea; margin-bottom: 15px; }
.cart-item-features {
  background: #f8f9fa; padding: 15px; border-radius: 8px; border-left: 4px solid #667eea;
}
.cart-item-features p { margin: 0; color: #666; line-height: 1.5; }
.cart-summary {
  background: white; border-radius: 15px; padding: 25px; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1); position: sticky; bottom: 20px;
}
.summary-row { display: flex; justify-content: space-between; margin-bottom: 15px; font-size: 18px; }
.summary-row.total { border-top: 2px solid #eee; padding-top: 15px; font-weight: bold; font-size: 24px; }
.total-amount { color: #e74c3c; }
.checkout-btn {
  width: 100%;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  color: white;
  border: none;
  padding: 15px;
  border-radius: 25px;
  font-size: 18px;
  font-weight: bold;
  cursor: pointer;
  margin-top: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1.2;
  box-sizing: border-box;
}
</style>

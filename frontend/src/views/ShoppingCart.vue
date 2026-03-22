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
            {{ item.itemType === 'PRODUCT' ? '🎁 套餐' : '🎫 景区门票' }}
          </div>
          <h3>{{ item.itemName }}</h3>
          <button class="remove-btn" @click="removeItem(index)">×</button>
        </div>
        <div v-if="item.imageUrl" class="cart-item-image">
          <img :src="getImageUrl(item.imageUrl)" :alt="item.itemName">
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
      userId: null
    }
  },
  computed: {
    totalPrice() {
      return this.cartItems.reduce((sum, item) => sum + (parseFloat(item.price) * item.quantity || 0), 0).toFixed(2)
    },
    totalQuantity() {
      return this.cartItems.reduce((sum, item) => sum + item.quantity, 0)
    }
  },
  created() {
    // 获取当前用户信息
    this.loadUserInfo()
  },
  methods: {
    getImageUrl(imageUrl) {
      if (!imageUrl) return ''
      if (imageUrl.startsWith('http')) return imageUrl
      return `http://localhost:8080${imageUrl}`
    },
    async loadUserInfo() {
      try {
        // 尝试从后端获取用户信息
        const response = await apiClient.get('/api/auth/current-user')
        this.userId = response.data.id
        localStorage.setItem('userId', response.data.id)
        localStorage.setItem('username', response.data.username)
        this.loadCart()
      } catch (error) {
        console.error('获取用户信息失败:', error)
        // 如果获取失败，尝试从 localStorage 获取
        const savedUserId = localStorage.getItem('userId')
        if (savedUserId) {
          this.userId = parseInt(savedUserId)
          this.loadCart()
        }
      }
    },
    async loadCart() {
      if (!this.userId) return
      try {
        const response = await apiClient.get(`/api/cart?userId=${this.userId}`)
        this.cartItems = response.data.map(item => ({
          id: item.id,
          itemType: item.itemType || 'PRODUCT',
          itemId: item.itemId,
          itemName: item.itemName || item.productName,
          price: item.price,
          imageUrl: item.imageUrl,
          features: item.features,
          quantity: item.quantity || 1
        }))
        // 清除旧的 localStorage 数据
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
      } catch (error) {
        console.error('更新数量失败:', error)
        alert('更新失败，请重试')
      }
    },
    async removeItem(index) {
      const item = this.cartItems[index]
      try {
        if (item.id) {
          // 从数据库删除
          await apiClient.delete(`/api/cart/${item.id}?userId=${this.userId}`)
        }
        this.cartItems.splice(index, 1)
      } catch (error) {
        console.error('删除商品失败:', error)
        alert('删除失败，请重试')
      }
    },
    async checkout() {
      console.log('结算商品:', this.cartItems)
      try {
        // 调用后端创建订单
        const orderData = {
          userId: this.userId,
          items: this.cartItems,
          totalAmount: this.totalPrice
        }
        // TODO: 调用创建订单的 API
        // await apiClient.post('/orders', orderData)
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
      } catch (error) {
        console.error('清空购物车失败:', error)
      }
    },
    goToSubscription() {
      this.$router.push('/subscription')
    }
  },
  watch: {
    // 监听路由变化，刷新购物车数据
    '$route'(to, from) {
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

.empty-cart-icon {
  font-size: 80px;
  margin-bottom: 20px;
}

.empty-cart p {
  font-size: 18px;
  color: #999;
  margin-bottom: 30px;
}

.back-btn {
  padding: 12px 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 16px;
  transition: transform 0.3s ease;
}

.back-btn:hover {
  transform: translateY(-2px);
}

.cart-items {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 20px;
  padding-top: 10px;
}

.cart-item-card {
  background: white;
  border-radius: 10px;
  padding: 25px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 1;
}

.cart-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
  flex-wrap: wrap;
  gap: 10px;
}

.item-type-badge {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.cart-item-header h3 {
  color: #333;
  font-size: 22px;
  margin: 0;
  flex: 1;
}

.remove-btn {
  width: 30px;
  height: 30px;
  border: none;
  background: #f44336;
  color: white;
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s ease;
}

.remove-btn:hover {
  transform: scale(1.1);
}

.cart-item-image {
  width: 100%;
  max-height: 200px;
  overflow: hidden;
  border-radius: 8px;
  margin-bottom: 15px;
}

.cart-item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cart-item-price {
  text-align: center;
  margin-bottom: 15px;
}

.currency {
  font-size: 20px;
  color: #f44336;
  vertical-align: top;
}

.amount {
  font-size: 36px;
  color: #f44336;
  font-weight: bold;
}

.quantity-control {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
}

.quantity-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s ease;
}

.quantity-btn:hover {
  transform: scale(1.1);
}

.quantity-value {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  min-width: 40px;
  text-align: center;
}

.cart-item-total {
  text-align: center;
  font-size: 16px;
  font-weight: bold;
  color: #667eea;
  margin-bottom: 15px;
}

.cart-item-features {
  padding: 0;
  margin: 0;
}

.cart-item-features p {
  padding: 10px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.6;
}

.cart-summary {
  background: white;
  border-radius: 10px;
  padding: 25px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  margin-top: 20px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  font-size: 16px;
  color: #666;
  border-bottom: 1px solid #f0f0f0;
}

.summary-row.total {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  border-bottom: 2px solid #667eea;
  margin-bottom: 20px;
}

.total-amount {
  color: #f44336;
  font-size: 24px;
}

.checkout-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 18px;
  font-weight: bold;
  transition: transform 0.3s ease;
}

.checkout-btn:hover {
  transform: translateY(-2px);
}
</style>

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
        :key="index" 
        class="cart-item-card"
      >
        <div class="cart-item-header">
          <h3>{{ item.name }}</h3>
          <button class="remove-btn" @click="removeItem(index)">×</button>
        </div>
        <div class="cart-item-price">
          <span class="currency">¥</span>
          <span class="amount">{{ item.price }}</span>
          <span class="period">/年</span>
        </div>
        <ul class="cart-item-features">
          <li v-for="(feature, idx) in item.features" :key="idx">
            ✓ {{ feature }}
          </li>
        </ul>
      </div>
      
      <div class="cart-summary">
        <div class="summary-row">
          <span>商品数量：</span>
          <span>{{ cartItems.length }} 个</span>
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
      return this.cartItems.reduce((sum, item) => sum + (parseFloat(item.price) || 0), 0).toFixed(2)
    }
  },
  created() {
    // 获取当前用户信息
    this.loadUserInfo()
  },
  methods: {
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
        this.cartItems = response.data
        // 清除旧的 localStorage 数据
        localStorage.removeItem('shoppingCart')
      } catch (error) {
        console.error('加载购物车失败:', error)
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
}

.cart-item-header h3 {
  color: #333;
  font-size: 22px;
  margin: 0;
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

.cart-item-price {
  text-align: center;
  margin-bottom: 20px;
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

.period {
  font-size: 14px;
  color: #999;
}

.cart-item-features {
  list-style: none;
  padding: 0;
  margin: 0;
}

.cart-item-features li {
  padding: 8px 0;
  color: #666;
  font-size: 14px;
  border-bottom: 1px solid #f5f5f5;
}

.cart-item-features li:last-child {
  border-bottom: none;
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

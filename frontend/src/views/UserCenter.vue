<template>
  <div class="portal-page user-page">
    <div class="portal-wrap">
      <section class="portal-hero user-hero">
        <div class="portal-pill light">USER CENTER</div>
        <h1 class="portal-title">管理你的旅行账户</h1>
        <p class="portal-subtitle">
          在这里可以维护账号信息、查看购物车概览，并更方便地整理你的出游准备，
          让每一次旅行都安排得更清楚。
        </p>
      </section>

      <section class="portal-section portal-grid two user-grid">
        <div class="portal-surface portal-panel">
          <div class="portal-section-head">
            <div>
              <div class="portal-pill soft">PROFILE</div>
              <h2>个人信息</h2>
            </div>
          </div>

          <div v-if="loadingUser" class="portal-empty">
            <strong>用户信息加载中</strong>
          </div>

          <div v-else>
            <div class="portal-kv">
              <div class="portal-kv-item">
                <strong>用户 ID</strong>
                <span>{{ userInfo.id || '-' }}</span>
              </div>
              <div class="portal-kv-item">
                <strong>当前用户名</strong>
                <span>{{ userInfo.username || '-' }}</span>
              </div>
              <div class="portal-kv-item">
                <strong>当前邮箱</strong>
                <span>{{ userInfo.email || '-' }}</span>
              </div>
            </div>

            <div class="edit-stack">
              <section class="edit-card">
                <h3>修改用户名</h3>
                <div class="portal-form-grid">
                  <div class="portal-field">
                    <label>新用户名</label>
                    <input v-model.trim="usernameForm.username" type="text" class="portal-input" placeholder="请输入新用户名" />
                  </div>
                  <button class="portal-btn primary" :disabled="savingUsername" @click="saveUsername">
                    {{ savingUsername ? '保存中...' : '保存用户名' }}
                  </button>
                </div>
              </section>

              <section class="edit-card">
                <h3>修改邮箱</h3>
                <div class="portal-form-grid">
                  <div class="portal-field">
                    <label>新邮箱</label>
                    <input v-model.trim="emailForm.email" type="email" class="portal-input" placeholder="请输入新邮箱" />
                  </div>
                  <button class="portal-btn primary" :disabled="savingEmail" @click="saveEmail">
                    {{ savingEmail ? '保存中...' : '保存邮箱' }}
                  </button>
                </div>
              </section>

              <section class="edit-card">
                <h3>修改密码</h3>
                <div class="portal-form-grid two">
                  <div class="portal-field">
                    <label>新密码</label>
                    <input v-model.trim="passwordForm.password" type="password" class="portal-input" placeholder="请输入新密码" />
                  </div>
                  <div class="portal-field">
                    <label>确认密码</label>
                    <input v-model.trim="passwordForm.confirmPassword" type="password" class="portal-input" placeholder="请再次输入新密码" />
                  </div>
                </div>
                <div class="portal-actions">
                  <button class="portal-btn primary" :disabled="savingPassword" @click="savePassword">
                    {{ savingPassword ? '保存中...' : '保存密码' }}
                  </button>
                </div>
              </section>
            </div>
          </div>
        </div>

        <div class="portal-surface portal-panel">
          <div class="portal-section-head">
            <div>
              <div class="portal-pill soft">CART SNAPSHOT</div>
              <h2>我的购物车</h2>
            </div>
            <button class="portal-btn ghost" @click="loadCart" :disabled="loadingCart">
              {{ loadingCart ? '刷新中...' : '刷新购物车' }}
            </button>
          </div>

          <div v-if="loadingCart" class="portal-empty">
            <strong>购物车加载中</strong>
          </div>
          <div v-else-if="cartItems.length === 0" class="portal-empty">
            <strong>购物车暂无商品</strong>
          </div>

          <div v-else class="portal-list">
            <article class="portal-list-item cart-item" v-for="item in cartItems" :key="item.id">
              <img
                v-if="item.image && !item.imageError"
                :src="getImageUrl(item.image)"
                :alt="item.name"
                class="cart-image"
                @error="markImageError(item.id)"
              />
              <div v-else class="cart-image portal-placeholder">
                <span>{{ getPlaceholderText(item) }}</span>
              </div>

              <div class="cart-main">
                <div class="cart-top">
                  <div class="cart-name">{{ item.name }}</div>
                  <span class="portal-pill soft">{{ item.itemTypeLabel }}</span>
                </div>
                <div class="cart-meta">单价：¥{{ formatPrice(item.price) }}</div>
                <div class="cart-meta">数量：{{ item.quantity }}</div>
                <div class="cart-subtotal">¥{{ formatPrice(item.subtotal) }}</div>
              </div>
            </article>

            <div class="portal-kv-item total-row">
              <strong>合计</strong>
              <span class="portal-price">¥{{ formatPrice(cartTotal) }}</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import apiClient from '@/utils/axios'

export default {
  name: 'UserCenter',
  data() {
    return {
      loadingUser: false,
      loadingCart: false,
      savingUsername: false,
      savingEmail: false,
      savingPassword: false,
      userInfo: {
        id: null,
        username: '',
        email: ''
      },
      usernameForm: {
        username: ''
      },
      emailForm: {
        email: ''
      },
      passwordForm: {
        password: '',
        confirmPassword: ''
      },
      cartItems: []
    }
  },
  computed: {
    cartTotal() {
      return this.cartItems.reduce((sum, item) => sum + Number(item.subtotal || 0), 0)
    },
    backendBaseUrl() {
      const apiBase = apiClient?.defaults?.baseURL || ''
      if (!apiBase) return window.location.origin
      if (/^https?:\/\//i.test(apiBase)) return apiBase.replace(/\/$/, '')
      return window.location.origin
    }
  },
  mounted() {
    this.initPage()
  },
  methods: {
    async initPage() {
      await this.loadUserInfo()
      await this.loadCart()
    },
    async loadUserInfo() {
      this.loadingUser = true
      try {
        let response
        try {
          response = await apiClient.get('/api/auth/current-user')
        } catch (e) {
          response = await apiClient.get('/api/users/me')
        }
        const user = response.data || {}
        this.userInfo = {
          id: user.id || null,
          username: user.username || '',
          email: user.email || ''
        }
        this.usernameForm.username = this.userInfo.username
        this.emailForm.email = this.userInfo.email
        if (this.userInfo.id) {
          localStorage.setItem('userId', this.userInfo.id)
        }
        localStorage.setItem('username', this.userInfo.username || '')
      } catch (error) {
        console.error('加载用户信息失败:', error)
        this.$message?.error?.('加载用户信息失败')
      } finally {
        this.loadingUser = false
      }
    },
    async saveUsername() {
      if (!this.usernameForm.username) {
        this.$message?.warning?.('用户名不能为空')
        return
      }
      this.savingUsername = true
      try {
        const response = await apiClient.put('/api/users/me', { username: this.usernameForm.username })
        const user = response.data || {}
        this.userInfo.username = user.username || this.usernameForm.username
        this.usernameForm.username = this.userInfo.username
        localStorage.setItem('username', this.userInfo.username || '')
        this.$message?.success?.('用户名修改成功')
      } catch (error) {
        console.error('修改用户名失败:', error)
        this.$message?.error?.(error?.response?.data?.message || '修改用户名失败')
      } finally {
        this.savingUsername = false
      }
    },
    async saveEmail() {
      this.savingEmail = true
      try {
        const response = await apiClient.put('/api/users/me', { email: this.emailForm.email })
        const user = response.data || {}
        this.userInfo.email = user.email ?? this.emailForm.email
        this.emailForm.email = this.userInfo.email
        this.$message?.success?.('邮箱修改成功')
      } catch (error) {
        console.error('修改邮箱失败:', error)
        this.$message?.error?.(error?.response?.data?.message || '修改邮箱失败')
      } finally {
        this.savingEmail = false
      }
    },
    async savePassword() {
      if (!this.passwordForm.password) {
        this.$message?.warning?.('请输入新密码')
        return
      }
      if (this.passwordForm.password !== this.passwordForm.confirmPassword) {
        this.$message?.warning?.('两次输入的新密码不一致')
        return
      }
      this.savingPassword = true
      try {
        await apiClient.put('/api/users/me', { password: this.passwordForm.password })
        this.passwordForm.password = ''
        this.passwordForm.confirmPassword = ''
        this.$message?.success?.('密码修改成功')
      } catch (error) {
        console.error('修改密码失败:', error)
        this.$message?.error?.(error?.response?.data?.message || '修改密码失败')
      } finally {
        this.savingPassword = false
      }
    },
    async loadCart() {
      this.loadingCart = true
      try {
        const userId = this.userInfo.id || localStorage.getItem('userId')
        const requestList = []
        requestList.push(() => apiClient.get('/api/cart/me'))
        requestList.push(() => apiClient.get('/api/cart/my'))
        if (userId) {
          requestList.push(() => apiClient.get(`/api/cart/user/${userId}`))
          requestList.push(() => apiClient.get('/api/cart', { params: { userId } }))
        }
        let rawList = []
        for (const request of requestList) {
          try {
            const response = await request()
            rawList = this.extractList(response.data)
            if (Array.isArray(rawList)) break
          } catch (e) {
          }
        }
        this.cartItems = (rawList || []).map((item, index) => this.normalizeCartItem(item, index))
      } catch (error) {
        console.error('加载购物车失败:', error)
        this.cartItems = []
      } finally {
        this.loadingCart = false
      }
    },
    extractList(data) {
      if (Array.isArray(data)) return data
      if (Array.isArray(data?.data)) return data.data
      if (Array.isArray(data?.content)) return data.content
      if (Array.isArray(data?.items)) return data.items
      return []
    },
    normalizeCartItem(item, index) {
      const quantity = Number(item.quantity ?? item.count ?? item.num ?? 1)
      const price = Number(item.price ?? item.unitPrice ?? item.amount ?? 0)
      const subtotal = Number(item.subtotal ?? item.totalPrice ?? (price * quantity))
      const itemType = String(item.itemType || item.type || '').toUpperCase()
      return {
        id: item.id ?? item.cartItemId ?? item.productId ?? `cart-${index}`,
        name: item.name ?? item.itemName ?? item.productName ?? item.title ?? item.scenicName ?? '未命名商品',
        image: item.image ?? item.imageUrl ?? item.cover ?? item.photo ?? '',
        quantity,
        price,
        subtotal,
        imageError: false,
        itemType,
        itemTypeLabel: this.getItemTypeLabel(itemType),
        itemTypeClass: this.getItemTypeClass(itemType)
      }
    },
    getItemTypeLabel(itemType) {
      if (itemType === 'SCENIC_AREA' || itemType === 'SCENIC') return '景区'
      if (itemType === 'PACKAGE' || itemType === 'PRODUCT') return '套餐'
      return '商品'
    },
    getItemTypeClass(itemType) {
      if (itemType === 'SCENIC_AREA' || itemType === 'SCENIC') return 'tag-scenic'
      if (itemType === 'PACKAGE' || itemType === 'PRODUCT') return 'tag-package'
      return 'tag-default'
    },
    getPlaceholderText(item) {
      return item.itemTypeLabel || '图片'
    },
    markImageError(id) {
      this.cartItems = this.cartItems.map(item => item.id === id ? { ...item, imageError: true } : item)
    },
    normalizeImagePath(path) {
      if (!path) return ''
      let value = String(path).trim()
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
    getImageUrl(path) {
      const normalized = this.normalizeImagePath(path)
      if (!normalized) return ''
      if (/^https?:\/\//i.test(normalized)) return normalized
      return `${this.backendBaseUrl}${normalized}`
    },
    formatPrice(value) {
      const number = Number(value || 0)
      return Number.isFinite(number) ? number.toFixed(2) : '0.00'
    }
  }
}
</script>

<style scoped>
@import '@/assets/css/portal-theme.css';

.user-hero {
  background: linear-gradient(135deg, rgba(14, 57, 40, 0.96), rgba(43, 106, 76, 0.84));
}

.user-grid {
  align-items: start;
}

.edit-stack {
  display: grid;
  gap: 16px;
  margin-top: 20px;
}

.edit-card {
  padding: 20px;
  border-radius: 22px;
  background: #f8fbf9;
}

.edit-card h3 {
  margin: 0 0 14px;
}

.cart-item {
  grid-template-columns: 96px 1fr;
  align-items: center;
}

.cart-image {
  width: 96px;
  height: 96px;
  border-radius: 18px;
  object-fit: cover;
}

.cart-main {
  min-width: 0;
}

.cart-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: start;
  margin-bottom: 8px;
}

.cart-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--portal-text);
}

.cart-meta {
  color: var(--portal-muted);
  line-height: 1.8;
}

.cart-subtotal {
  margin-top: 6px;
  font-weight: 700;
  color: var(--portal-danger);
}

.total-row {
  margin-top: 4px;
}

@media (max-width: 768px) {
  .cart-item {
    grid-template-columns: 1fr;
  }

  .cart-top {
    flex-direction: column;
  }
}
</style>

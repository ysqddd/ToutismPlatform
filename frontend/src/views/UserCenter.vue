<template>
  <div class="user-center-container">
    <h2>👤 用户中心</h2>

    <div class="user-center-grid">
      <div class="panel-card">
        <div class="panel-header">
          <h3>个人信息</h3>
        </div>

        <div v-if="loadingUser" class="tip">用户信息加载中...</div>

        <div v-else>
          <div class="profile-summary">
            <div class="info-item">
              <span class="label">用户ID：</span>
              <span class="value">{{ userInfo.id || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">当前用户名：</span>
              <span class="value">{{ userInfo.username || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">当前邮箱：</span>
              <span class="value">{{ userInfo.email || '-' }}</span>
            </div>
          </div>

          <div class="edit-block">
            <h4>修改用户名</h4>
            <div class="form-item inline-form">
              <input v-model.trim="usernameForm.username" type="text" placeholder="请输入新用户名" />
              <button class="action-btn primary" :disabled="savingUsername" @click="saveUsername">
                {{ savingUsername ? '保存中...' : '保存用户名' }}
              </button>
            </div>
          </div>

          <div class="edit-block">
            <h4>修改邮箱</h4>
            <div class="form-item inline-form">
              <input v-model.trim="emailForm.email" type="email" placeholder="请输入新邮箱" />
              <button class="action-btn primary" :disabled="savingEmail" @click="saveEmail">
                {{ savingEmail ? '保存中...' : '保存邮箱' }}
              </button>
            </div>
          </div>

          <div class="edit-block">
            <h4>修改密码</h4>
            <div class="form-item password-grid">
              <input v-model.trim="passwordForm.password" type="password" placeholder="请输入新密码" />
              <input v-model.trim="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
            </div>
            <div class="btn-row">
              <button class="action-btn primary" :disabled="savingPassword" @click="savePassword">
                {{ savingPassword ? '保存中...' : '保存密码' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="panel-card">
        <div class="panel-header">
          <h3>我的购物车</h3>
          <button class="action-btn" @click="loadCart" :disabled="loadingCart">
            {{ loadingCart ? '刷新中...' : '刷新购物车' }}
          </button>
        </div>

        <div v-if="loadingCart" class="tip">购物车加载中...</div>
        <p v-else-if="cartItems.length === 0" class="tip">购物车暂无商品</p>

        <div v-else class="cart-list">
          <div class="cart-item" v-for="item in cartItems" :key="item.id">
            <img
                v-if="item.image && !item.imageError"
                :src="getImageUrl(item.image)"
                :alt="item.name"
                class="cart-image"
                @error="markImageError(item.id)"
            />
            <div v-else class="cart-image cart-image-placeholder">
              <span>{{ getPlaceholderText(item) }}</span>
            </div>

            <div class="cart-main">
              <div class="cart-name">{{ item.name }}</div>
              <div class="cart-tag-row">
                <span class="cart-tag" :class="item.itemTypeClass">{{ item.itemTypeLabel }}</span>
              </div>
              <div class="cart-meta">单价：¥{{ formatPrice(item.price) }}</div>
              <div class="cart-meta">数量：{{ item.quantity }}</div>
            </div>

            <div class="cart-right">
              <div class="cart-subtotal">¥{{ formatPrice(item.subtotal) }}</div>
            </div>
          </div>

          <div class="cart-footer">
            <span>共 {{ cartItems.length }} 件商品</span>
            <span class="cart-total">合计：¥{{ formatPrice(cartTotal) }}</span>
          </div>
        </div>
      </div>
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
        const response = await apiClient.put('/api/users/me', {
          username: this.usernameForm.username
        })
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
        const response = await apiClient.put('/api/users/me', {
          email: this.emailForm.email
        })
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
        await apiClient.put('/api/users/me', {
          password: this.passwordForm.password
        })
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
            if (Array.isArray(rawList)) {
              break
            }
          } catch (e) {
            // 尝试下一个兼容接口
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
      this.cartItems = this.cartItems.map(item => {
        if (item.id === id) {
          return { ...item, imageError: true }
        }
        return item
      })
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
.user-center-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px;
}
.user-center-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  margin-top: 20px;
}
.panel-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.profile-summary {
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f1f1f1;
}
.edit-block {
  margin-bottom: 20px;
  padding: 14px;
  border: 1px solid #f3f3f3;
  border-radius: 12px;
  background: #fafafa;
}
.edit-block h4 {
  margin: 0 0 12px;
  font-size: 15px;
  color: #333;
}
.info-item, .form-item { margin-bottom: 14px; }
.label { color: #666; margin-right: 8px; }
.value { color: #222; font-weight: 500; }
.form-item input {
  width: 100%; height: 40px; border: 1px solid #ddd; border-radius: 8px; padding: 0 12px;
  box-sizing: border-box; background: #fff;
}
.inline-form { display: flex; gap: 10px; align-items: center; margin-bottom: 0; }
.inline-form input { flex: 1; }
.password-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.btn-row { display: flex; gap: 10px; margin-top: 12px; align-items: center; flex-wrap: wrap; }
.action-btn {
  height: 36px;
  min-width: 96px;
  padding: 0 14px;
  border: none;
  border-radius: 8px;
  background: #f2f3f5;
  cursor: pointer;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  box-sizing: border-box;
}
.action-btn.primary { background: #409eff; color: #fff; }
.action-btn:disabled { opacity: 0.7; cursor: not-allowed; }
.tip { color: #888; }
.cart-list { display: flex; flex-direction: column; gap: 14px; }
.cart-item {
  display: flex; gap: 12px; align-items: center; padding: 12px; border: 1px solid #f0f0f0; border-radius: 10px;
}
.cart-image {
  width: 72px; height: 72px; object-fit: cover; border-radius: 8px; flex-shrink: 0;
}
.cart-image-placeholder {
  display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #f5f7fa, #e4eaf3);
  color: #666; font-size: 13px; text-align: center; padding: 8px; box-sizing: border-box;
}
.cart-main { flex: 1; }
.cart-name { font-size: 16px; font-weight: 600; color: #222; margin-bottom: 6px; }
.cart-tag-row { margin-bottom: 6px; }
.cart-tag { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 12px; }
.tag-scenic { background: #ecf5ff; color: #409eff; }
.tag-package { background: #fdf6ec; color: #e6a23c; }
.tag-default { background: #f4f4f5; color: #909399; }
.cart-meta { color: #666; font-size: 14px; line-height: 1.8; }
.cart-right { min-width: 100px; text-align: right; }
.cart-subtotal { font-size: 16px; font-weight: 700; color: #f56c6c; }
.cart-footer {
  display: flex; justify-content: space-between; align-items: center; padding-top: 10px;
  border-top: 1px solid #f0f0f0; color: #444;
}
.cart-total { font-size: 18px; font-weight: 700; color: #f56c6c; }
@media (max-width: 900px) {
  .user-center-grid { grid-template-columns: 1fr; }
  .password-grid, .inline-form { grid-template-columns: 1fr; display: grid; }
}
</style>

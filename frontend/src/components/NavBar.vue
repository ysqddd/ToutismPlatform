<template>
  <header class="navbar-shell">
    <div class="navbar-container">
      <div class="navbar-brand" @click="navigateTo('/home')">
        <div class="brand-badge">开封</div>
        <div class="brand-text">
          <h1>旅游平台</h1>
          <p>Scenic Travel Portal</p>
        </div>
      </div>

      <nav class="navbar-menu">
        <button
            v-for="item in navItems"
            :key="item.path"
            class="menu-item"
            :class="{ active: isActive(item.path) }"
            @click="navigateTo(item.path)"
        >
          <span class="icon">{{ item.icon }}</span>
          <span class="text">{{ item.text }}</span>
        </button>
      </nav>

      <div class="navbar-user">
        <button class="user-center-link" @click="navigateTo('/user-center')">
          <span class="user-avatar">{{ username.charAt(0) || '游' }}</span>
          <span class="user-meta">
            <strong>{{ username }}</strong>
            <em>用户中心</em>
          </span>
        </button>
        <button class="logout-btn" @click="handleLogout">退出</button>
      </div>
    </div>
  </header>
</template>

<script>
import apiClient from '../utils/axios'

export default {
  name: 'NavBar',
  data() {
    return {
      username: '',
      navItems: [
        { path: '/home', text: '首页', icon: '⌂' },
        { path: '/assistant', text: '小助手', icon: '✦' },
        { path: '/scenic-intro', text: '景点介绍', icon: '◫' },
        { path: '/subscription', text: '在线预定', icon: '▣' },
        { path: '/shopping-cart', text: '购物车', icon: '◍' }
      ]
    }
  },
  mounted() {
    this.username = localStorage.getItem('username') || '游客'
  },
  methods: {
    navigateTo(path) {
      if (this.$route.path !== path) {
        this.$router.push(path)
      }
    },
    isActive(path) {
      return this.$route.path === path
    },
    async handleLogout() {
      try {
        await apiClient.post('/api/auth/logout')
      } catch (error) {
        console.error('Logout error:', error)
      } finally {
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        this.$router.push('/login')
      }
    }
  }
}
</script>

<style scoped>
.navbar-shell {
  position: sticky;
  top: 0;
  z-index: 1000;
  padding: 16px 24px 0;
  background: linear-gradient(180deg, rgba(245, 248, 246, 0.96) 0%, rgba(245, 248, 246, 0.82) 72%, rgba(245, 248, 246, 0) 100%);
  backdrop-filter: blur(12px);
}

.navbar-container {
  max-width: 1280px;
  margin: 0 auto;
  min-height: 78px;
  padding: 14px 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(31, 98, 68, 0.10);
  box-shadow: 0 18px 40px rgba(20, 54, 41, 0.10);
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 18px;
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  min-width: 210px;
}

.brand-badge {
  width: 52px;
  height: 52px;
  border-radius: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f6244, #3e8f66);
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  box-shadow: 0 12px 24px rgba(31, 98, 68, 0.18);
  flex-shrink: 0;
}

.brand-text h1 {
  margin: 0;
  font-size: 22px;
  color: #163126;
  line-height: 1.1;
}

.brand-text p {
  margin: 4px 0 0;
  font-size: 12px;
  letter-spacing: 0.12em;
  color: #6c8179;
  text-transform: uppercase;
}

.navbar-menu {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.menu-item,
.user-center-link,
.logout-btn {
  border: none;
  outline: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  transition: 0.25s ease;
}

.menu-item {
  height: 46px;
  padding: 0 18px;
  border-radius: 999px;
  background: transparent;
  color: #26473b;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}

.menu-item .icon {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(31, 98, 68, 0.08);
  color: #1f6244;
  font-size: 12px;
  flex-shrink: 0;
}

.menu-item.active {
  background: #1f6244;
  color: #fff;
  box-shadow: 0 12px 26px rgba(31, 98, 68, 0.22);
}

.menu-item.active .icon {
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
}

.menu-item:hover,
.user-center-link:hover,
.logout-btn:hover {
  transform: translateY(-1px);
}

.navbar-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-center-link {
  min-height: 50px;
  padding: 8px 14px 8px 10px;
  border-radius: 18px;
  background: #f4f8f5;
  gap: 10px;
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f6244, #3e8f66);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.user-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  text-align: left;
  line-height: 1.15;
}

.user-meta strong {
  color: #153326;
  font-size: 14px;
  font-weight: 700;
}

.user-meta em {
  margin-top: 3px;
  color: #72857d;
  font-size: 12px;
  font-style: normal;
}

.logout-btn {
  min-width: 84px;
  height: 46px;
  padding: 0 18px;
  border-radius: 999px;
  background: #fff3f1;
  color: #c35548;
  font-size: 14px;
  font-weight: 700;
}

@media (max-width: 1180px) {
  .navbar-container {
    grid-template-columns: 1fr;
    justify-items: stretch;
  }

  .navbar-brand,
  .navbar-user {
    justify-self: center;
  }
}

@media (max-width: 768px) {
  .navbar-shell {
    padding: 12px 14px 0;
  }

  .navbar-container {
    padding: 14px;
    border-radius: 20px;
  }

  .navbar-menu {
    justify-content: flex-start;
  }

  .menu-item {
    height: 42px;
    padding: 0 14px;
    font-size: 14px;
  }

  .navbar-user {
    width: 100%;
    justify-content: space-between;
  }

  .user-center-link {
    flex: 1;
    justify-content: flex-start;
  }
}
</style>

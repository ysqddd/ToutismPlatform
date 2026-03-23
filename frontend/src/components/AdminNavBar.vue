<template>
  <div class="admin-navbar-container">
    <div class="navbar-brand">
      <span class="logo">🎯 管理平台</span>
    </div>
    
    <ul class="navbar-menu">
      <li class="menu-item" :class="{ active: isActive('/admin/home') }" @click="navigateTo('/admin/home')">
        <span class="icon">🏠</span>
        <span class="text">首页</span>
      </li>
      <li class="menu-item" :class="{ active: isActive('/admin/users') }" @click="navigateTo('/admin/users')">
        <span class="icon">👥</span>
        <span class="text">管理用户</span>
      </li>
      <li class="menu-item" :class="{ active: isActive('/admin/permissions') }" @click="navigateTo('/admin/permissions')">
        <span class="icon">🔐</span>
        <span class="text">管理权限</span>
      </li>
      <li class="menu-item" :class="{ active: isActive('/admin/employees') }" @click="navigateTo('/admin/employees')">
        <span class="icon">👨‍💼</span>
        <span class="text">管理员工</span>
      </li>
      <li class="menu-item dropdown" @mouseenter="openDropdown('comprehensive')" @mouseleave="handleDropdownLeave('comprehensive')">
        <div class="dropdown-toggle">
          <span class="icon">📋</span>
          <span class="text">综合管理</span>
          <span class="dropdown-arrow">▼</span>
        </div>
        <div class="dropdown-menu" v-if="dropdowns.comprehensive" @mouseenter="openDropdown('comprehensive')" @mouseleave="handleDropdownLeave('comprehensive')">
          <div class="dropdown-item" @click="navigateTo('/admin/scenic-management')">
            <span class="icon">🏞️</span>
            <span class="text">景区管理</span>
          </div>
          <div class="dropdown-item" @click="navigateTo('/admin/products')">
            <span class="icon">📦</span>
            <span class="text">套餐管理</span>
          </div>
          <div class="dropdown-item" @click="navigateTo('/admin/path-management')">
            <span class="icon">🗺️</span>
            <span class="text">景区间路径</span>
          </div>
          <div class="dropdown-item" @click="navigateTo('/admin/scenic-edge-management')">
            <span class="icon">🛤️</span>
            <span class="text">景区内路径</span>
          </div>
        </div>
      </li>
    </ul>
    
    <div class="navbar-user">
      <div class="user-info-wrapper">
        <span class="welcome-text">管理员：{{ adminUsername }}</span>
      </div>
      <button class="logout-btn" @click="handleLogout">退出</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdminNavBar',
  data() {
    return {
      adminUsername: '',
      dropdowns: {
        comprehensive: false
      },
      dropdownTimers: {}
    }
  },
  mounted() {
    this.adminUsername = localStorage.getItem('adminUsername') || '管理员'
  },
  methods: {
    navigateTo(path) {
      this.$router.push(path)
    },
    isActive(path) {
      return this.$route.path === path
    },
    openDropdown(dropdownName) {
      // 清除之前的定时器
      if (this.dropdownTimers && this.dropdownTimers[dropdownName]) {
        clearTimeout(this.dropdownTimers[dropdownName])
      }
      this.dropdowns[dropdownName] = true
    },
    closeDropdown(dropdownName) {
      this.dropdowns[dropdownName] = false
    },
    handleDropdownLeave(dropdownName) {
      // 设置定时器，延迟关闭下拉菜单
      if (!this.dropdownTimers) {
        this.dropdownTimers = {}
      }
      // 清除之前的定时器
      if (this.dropdownTimers[dropdownName]) {
        clearTimeout(this.dropdownTimers[dropdownName])
      }
      // 300毫秒后关闭下拉菜单
      this.dropdownTimers[dropdownName] = setTimeout(() => {
        this.dropdowns[dropdownName] = false
      }, 300)
    },
    handleLogout() {
      localStorage.removeItem('adminToken')
      localStorage.removeItem('adminUsername')
      localStorage.removeItem('isAdmin')
      this.$router.push('/admin/login')
    }
  }
}
</script>

<style scoped>
.admin-navbar-container {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0 30px;
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  position: sticky;
  top: 0;
  z-index: 100;
}

.navbar-brand {
  display: flex;
  align-items: center;
}

.logo {
  color: white;
  font-size: 24px;
  font-weight: bold;
}

.navbar-menu {
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0;
  gap: 10px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  color: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.3s;
  font-size: 15px;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.2);
}

.menu-item.active {
  background: rgba(255, 255, 255, 0.3);
  color: white;
  font-weight: 600;
}

.icon {
  font-size: 20px;
}

.text {
  white-space: nowrap;
}

.dropdown {
  position: relative;
}

.dropdown-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dropdown-arrow {
  font-size: 12px;
  transition: transform 0.3s;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  background: white;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 180px;
  margin-top: 8px;
  z-index: 1000;
  overflow: hidden;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  color: #333;
  cursor: pointer;
  transition: background-color 0.3s;
}

.dropdown-item:hover {
  background-color: #f5f5f5;
}

.dropdown-item .icon {
  color: #667eea;
}

.navbar-user {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-info-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.welcome-text {
  color: rgba(255, 255, 255, 0.95);
  font-size: 14px;
}

.logout-btn {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  padding: 8px 20px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}
</style>

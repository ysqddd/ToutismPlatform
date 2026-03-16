<template>
  <div class="admin-home-container">
    <AdminNavBar />
    
    <div class="admin-content">
      <div class="welcome-section">
        <h1>欢迎使用管理平台</h1>
        <p>管理员：{{ adminUsername }}</p>
      </div>
      
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon">🏞️</div>
          <div class="stat-info">
            <h3>景区数量</h3>
            <p class="stat-number">{{ stats.scenicAreas }}</p>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon">🎯</div>
          <div class="stat-info">
            <h3>景点数量</h3>
            <p class="stat-number">{{ stats.scenicSpots }}</p>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon">📦</div>
          <div class="stat-info">
            <h3>套餐数量</h3>
            <p class="stat-number">{{ stats.products }}</p>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon">👥</div>
          <div class="stat-info">
            <h3>用户数量</h3>
            <p class="stat-number">{{ stats.users }}</p>
          </div>
        </div>
      </div>
      

    </div>
  </div>
</template>

<script>
import AdminNavBar from '../../components/AdminNavBar.vue'
import apiClient from '../../utils/axios'

export default {
  name: 'AdminHome',
  components: {
    AdminNavBar
  },
  data() {
    return {
      adminUsername: '',
      stats: {
        scenicAreas: 0,
        scenicSpots: 0,
        products: 0,
        users: 0
      }
    }
  },
  mounted() {
    this.adminUsername = localStorage.getItem('adminUsername') || '管理员'
    this.loadStats()
  },
  methods: {
    navigateTo(path) {
      this.$router.push(path)
    },
    async loadStats() {
      try {
        // 并发加载所有统计数据
        const [scenicAreasRes, scenicSpotsRes, productsRes, usersRes] = await Promise.all([
          apiClient.get('/api/large-areas'),
          apiClient.get('/api/small-spots'),
          apiClient.get('/api/products'),
          apiClient.get('/api/users')
        ])
        
        this.stats = {
          scenicAreas: scenicAreasRes.data.length || 0,
          scenicSpots: scenicSpotsRes.data.length || 0,
          products: productsRes.data.length || 0,
          users: usersRes.data.length || 0
        }
      } catch (error) {
        console.error('加载统计数据失败:', error)
        // 如果加载失败，显示 0
        this.stats = {
          scenicAreas: 0,
          scenicSpots: 0,
          products: 0,
          users: 0
        }
      }
    }
  }
}
</script>

<style scoped>
.admin-home-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.admin-content {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-section {
  background: white;
  padding: 30px;
  border-radius: 10px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.welcome-section h1 {
  color: #333;
  margin-bottom: 10px;
  font-size: 28px;
}

.welcome-section p {
  color: #666;
  font-size: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  padding: 25px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-icon {
  font-size: 48px;
  margin-right: 20px;
}

.stat-info h3 {
  color: #666;
  font-size: 14px;
  margin-bottom: 8px;
}

.stat-number {
  color: #333;
  font-size: 32px;
  font-weight: bold;
}


</style>

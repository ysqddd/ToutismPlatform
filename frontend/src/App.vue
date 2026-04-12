<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NavBar from './components/NavBar.vue'

const route = useRoute()

// 判断是否显示导航栏（登录和注册页面不显示）
const showNavBar = computed(() => {
  return !['/login', '/register'].includes(route.path) && !route.path.startsWith('/admin')
})
</script>

<template>
  <div class="app-container">
    <NavBar v-if="showNavBar" />
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.app-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  margin-top: 60px; /* navbar height */
  background:
    radial-gradient(circle at top left, rgba(131, 184, 157, 0.18), transparent 28%),
    linear-gradient(180deg, #f7faf8 0%, #eff5f0 100%);
  min-height: calc(100vh - 60px);
  width: 100%;
  box-sizing: border-box;
}
</style>

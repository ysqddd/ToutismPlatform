import { createRouter, createWebHistory } from 'vue-router'
import Login from './components/Login.vue'
import Register from './components/Register.vue'
import AdminLogin from './views/admin/AdminLogin.vue'
import AdminHome from './views/admin/AdminHome.vue'
import AdminScenicManagement from './views/admin/AdminScenicManagement.vue'
import AdminProducts from './views/admin/AdminProducts.vue'
import AdminUsers from './views/admin/AdminUsers.vue'
import AdminPermissions from './views/admin/AdminPermissions.vue'
import AdminEmployees from './views/admin/AdminEmployees.vue'
import AdminPathManagement from './views/admin/AdminPathManagement.vue'
import AdminScenicEdgeManagement from './views/admin/AdminScenicEdgeManagement.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/register',
    name: 'Register',
    component: Register
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: AdminLogin
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('./views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/home',
    name: 'HomePage',
    component: () => import('./views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/user-center',
    name: 'UserCenter',
    component: () => import('./views/UserCenter.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/assistant',
    name: 'Assistant',
    component: () => import('./views/Assistant.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/scenic-intro',
    name: 'ScenicIntro',
    component: () => import('./views/ScenicIntro.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/subscription',
    name: 'Subscription',
    component: () => import('./views/Subscription.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/shopping-cart',
    name: 'ShoppingCart',
    component: () => import('./views/ShoppingCart.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('./views/ProductDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/scenic/:id',
    name: 'ScenicDetail',
    component: () => import('./views/ScenicDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/home',
    name: 'AdminHome',
    component: AdminHome,
    meta: { requiresAdmin: true }
  },
  {
    path: '/admin/scenic-management',
    name: 'AdminScenicManagement',
    component: AdminScenicManagement,
    meta: { requiresAdmin: true }
  },
  {
    path: '/admin/products',
    name: 'AdminProducts',
    component: AdminProducts,
    meta: { requiresAdmin: true }
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: AdminUsers,
    meta: { requiresAdmin: true }
  },
  {
    path: '/admin/permissions',
    name: 'AdminPermissions',
    component: AdminPermissions,
    meta: { requiresAdmin: true }
  },
  {
    path: '/admin/employees',
    name: 'AdminEmployees',
    component: AdminEmployees,
    meta: { requiresAdmin: true }
  },
  {
    path: '/admin/path-management',
    name: 'AdminPathManagement',
    component: AdminPathManagement,
    meta: { requiresAdmin: true }
  },
  {
    path: '/admin/scenic-edge-management',
    name: 'AdminScenicEdgeManagement',
    component: AdminScenicEdgeManagement,
    meta: { requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫，检查用户是否已登录
router.beforeEach((to, from, next) => {
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const requiresAdmin = to.matched.some(record => record.meta.requiresAdmin)
  const token = localStorage.getItem('token')
  const adminToken = localStorage.getItem('adminToken')
  
  // 检查token是否有效
  const isTokenValid = token && token.length > 0
  const isAdminTokenValid = adminToken && adminToken.length > 0
  
  if (requiresAdmin && !isAdminTokenValid) {
    // 清除无效的管理员token
    localStorage.clear()
    next('/admin/login')
  } else if (requiresAuth && !isTokenValid) {
    // 清除无效的用户token
    localStorage.clear()
    next('/login')
  } else {
    next()
  }
})

export default router
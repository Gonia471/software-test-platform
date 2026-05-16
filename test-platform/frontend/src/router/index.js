import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/RegisterView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('../views/LayoutView.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/dashboard',
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/DashboardView.vue'),
      },
      {
        path: 'organizations',
        name: 'Organizations',
        component: () => import('../views/OrganizationsView.vue'),
      },
      {
        path: 'projects',
        name: 'Projects',
        component: () => import('../views/ProjectsView.vue'),
      },
      {
        path: 'api-test',
        name: 'ApiTest',
        component: () => import('../views/ApiTestView.vue'),
      },
      {
        path: 'ui-test',
        name: 'UiTestSpace',
        component: () => import('../views/UiTestSpaceView.vue'),
      },
      {
        path: 'ui-test/cases/:id',
        name: 'UiTestCase',
        component: () => import('../views/UiTestView.vue'),
      },
      {
        path: 'reports',
        name: 'Reports',
        component: () => import('../views/ReportsView.vue'),
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/ProfileView.vue'),
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('../views/SettingsView.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

function routeRequiresAuth(to) {
  return to.matched.some(r => r.meta && r.meta.requiresAuth === true)
}

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  let token = localStorage.getItem('token')

  // 开发/演示：未登录时注入占位 token，避免本地与演示环境白屏卡死
  if (routeRequiresAuth(to) && !token) {
    localStorage.setItem('token', 'dev-token')
    localStorage.setItem('username', '开发模式用户')
    token = 'dev-token'
  }

  // 同步 Pinia 状态
  if (token && !userStore.token) {
    userStore.setAuth(
      token,
      localStorage.getItem('username') || '开发模式用户',
      localStorage.getItem('phone') || '',
      localStorage.getItem('userId') || 1,
      token === 'dev-token'
    )
  }

  // 如果已登录但访问登录页，跳转到首页
  if (to.path === '/login' && token) {
    next('/')
    return
  }

  next()
})

export default router

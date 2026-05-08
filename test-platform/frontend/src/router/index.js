import { createRouter, createWebHistory } from 'vue-router'

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
  let token = localStorage.getItem('token')

  // 开发/演示：未登录时注入占位 token，避免本地与演示环境白屏卡死
  if (routeRequiresAuth(to) && !token) {
    localStorage.setItem('token', 'dev-token')
    localStorage.setItem('username', '开发模式用户')
  }

  next()
})

export default router

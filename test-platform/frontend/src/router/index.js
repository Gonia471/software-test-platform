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

router.beforeEach((to, from, next) => {
  let token = localStorage.getItem('token')

  // 开发阶段：如果没有登录信息，自动注入一个“假登录”，避免每次手动登录
  if (to.meta.requiresAuth !== false && !token) {
    localStorage.setItem('token', 'dev-token')
    localStorage.setItem('username', '开发模式用户')
    token = 'dev-token'
  }

  next()
})

export default router

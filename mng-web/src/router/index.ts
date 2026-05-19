import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/',
      component: () => import('@/components/AppLayout.vue'),
      redirect: '/dashboard',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/Dashboard.vue'),
        },
        {
          path: 'tenants',
          name: 'TenantList',
          component: () => import('@/views/TenantList.vue'),
        },
        {
          path: 'metrics',
          name: 'MetricList',
          component: () => import('@/views/MetricList.vue'),
        },
        {
          path: 'leaderboards',
          name: 'LeaderboardList',
          component: () => import('@/views/LeaderboardList.vue'),
        },
        {
          path: 'leaderboard/create',
          name: 'LeaderboardCreate',
          component: () => import('@/views/LeaderboardCreate.vue'),
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path === '/login' && auth.isLoggedIn()) {
    return '/dashboard'
  }
  if (to.meta.requiresAuth !== false && !auth.isLoggedIn()) {
    return '/login'
  }
})

export default router

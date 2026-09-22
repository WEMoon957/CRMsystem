import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
    {
      path: '/',
      component: () => import('@/views/LayoutView.vue'),
      children: [
        { path: '', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
        { path: 'customers', name: 'customers', component: () => import('@/views/CustomerListView.vue') },
        { path: 'customers/:id', name: 'customer-detail', component: () => import('@/views/CustomerDetailView.vue') },
        { path: 'users', name: 'users', component: () => import('@/views/UserListView.vue'), meta: { adminOnly: true } }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    return auth.isLoggedIn ? true : (await auth.restore()) ? true : true
  }
  if (!auth.isLoggedIn) {
    const restored = await auth.restore()
    if (!restored) return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.adminOnly && !auth.isAdmin) {
    return { name: 'customers' }
  }
  return true
})

export default router

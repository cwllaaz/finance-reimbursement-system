import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '../layouts/AdminLayout.vue'
import { getStoredSession } from '../composables/useAuthSession'
import { canAccessMenu, defaultMenuForRole } from '../composables/usePermissions'
import { menuPathMap, routeCatalog } from '../views/routeCatalog'

const defaultPath = () => {
  const { token, user } = getStoredSession()
  if (!token || !user) return '/login'
  return menuPathMap[defaultMenuForRole(user.role)] || '/login'
}

const routes = [
  {
    path: '/login',
    name: 'login',
    component: AdminLayout,
    meta: { public: true, title: '登录' },
  },
  ...routeCatalog.map(([menu, path, title]) => ({
    path,
    name: menu,
    component: AdminLayout,
    meta: { menu, title },
  })),
  { path: '/', redirect: defaultPath },
  { path: '/:pathMatch(.*)*', redirect: defaultPath },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to) => {
  const { token, user } = getStoredSession()

  if (to.meta.public) {
    if (token && user) return menuPathMap[defaultMenuForRole(user.role)]
    return true
  }

  if (!token) return '/login'
  if (user && to.meta.menu && !canAccessMenu(user.role, to.meta.menu)) {
    return menuPathMap[defaultMenuForRole(user.role)]
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 财务报销系统` : '财务报销系统'
})

export default router

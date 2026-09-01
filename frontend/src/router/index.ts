import { createRouter, createWebHistory } from 'vue-router'

import ComponentsTest from '@/views/ComponentsTest.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),

  routes: [
    {
      path: '/components-test',
      name: 'components-test',
      component: ComponentsTest,
    },

    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/frontoffice/Login.vue'),
      meta: {
        requiresGuest: true,
      },
    },

    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('@/views/frontoffice/ForgotPassword.vue'),
      meta: {
        requiresGuest: true,
      },
    },

    {
      path: '/reset-password',
      name: 'reset-password',
      component: () => import('@/views/frontoffice/ResetPassword.vue'),
      meta: {
        requiresGuest: true,
      },
    },

    {
      path: '/',
      component: () => import('@/layouts/FrontLayout.vue'),
      meta: {
        requiresAuth: true,
      },
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/frontoffice/Dashboard.vue'),
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/frontoffice/Profile.vue'),
        }
      ],
    },
  ],
})

/**
 * Protection des routes
 */
router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (!authStore.initialized) {
    await authStore.restoreSession()
  }

  // Route nécessitant une authentification
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return {
      name: 'login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  // Empêcher un utilisateur déjà connecté
  // de retourner sur la page de connexion
  if (to.meta.requiresGuest && authStore.isAuthenticated) {
    return {
      name: 'dashboard',
    }
  }

  return true
})

export default router
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

import { frontofficeRoutes } from './frontoffice'
import { backofficeRoutes } from './backoffice'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),

    routes: [
        {
          path: '/',
          name: 'acceuil',
          component: () => import('@/views/Acceuil.vue')
        },
        ...frontofficeRoutes,
        ...backofficeRoutes,
    ],
})

router.beforeEach(async (to) => {
    const authStore = useAuthStore()

    if (!authStore.initialized) {
        await authStore.restoreSession()
    }

    // ==============================
    // BACKOFFICE
    // ==============================

    if (to.meta.requiresBackoffice) {
        if (!authStore.isAuthenticated) {
            return {
                name: 'backoffice-login',
            }
        }

        if (!authStore.isBackoffice) {
            return {
                name: 'dashboard',
            }
        }
    }

    if (to.meta.requiresBackofficeGuest) {
        if (authStore.isAuthenticated) {
            return authStore.isBackoffice
                ? { name: 'backoffice-dashboard' }
                : { name: 'dashboard' }
        }
    }

    // ==============================
    // FRONTOFFICE
    // ==============================

    if (to.meta.requiresFrontoffice) {
        if (!authStore.isAuthenticated) {
            return {
                name: 'login',
                query: {
                    redirect: to.fullPath,
                },
            }
        }
    }

    if (to.meta.requiresFrontofficeGuest) {
        if (authStore.isAuthenticated) {
            return {
                name: 'dashboard',
            }
        }
    }

    return true
})

export default router
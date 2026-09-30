import type { RouteRecordRaw } from 'vue-router'

export const backofficeRoutes: RouteRecordRaw[] = [
    {
        path: '/backoffice/login',
        name: 'backoffice-login',
        component: () => import('@/views/backoffice/Login.vue'),
        meta: {
            requiresBackofficeGuest: true,
        },
    },

    {
        path: '/backoffice',
        component: () => import('@/layouts/BackLayout.vue'),
        meta: {
            requiresBackoffice: true,
        },
        children: [
            {
                path: 'dashboard',
                name: 'backoffice-dashboard',
                component: () => import('@/views/backoffice/Dashboard.vue'),
            },
        ],
    },
]
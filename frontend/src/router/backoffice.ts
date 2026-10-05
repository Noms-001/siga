import type { RouteRecordRaw } from 'vue-router'

export const backofficeRoutes: RouteRecordRaw[] = [
    {
        path: '/backoffice/login',
        name: 'backoffice-login',
        component: () => import('@/views/backoffice/Login.vue'),
        meta: { requiresBackofficeGuest: true },
    },

    {
        path: '/backoffice',
        component: () => import('@/layouts/BackLayout.vue'),
        meta: { requiresBackoffice: true },
        children: [
            {
                path: 'dashboard',
                name: 'backoffice-dashboard',
                component: () => import('@/views/backoffice/Dashboard.vue'),
            },

            /* ------------------ DÉPARTEMENTS ------------------ */
            {
                path: 'departements',
                name: 'backoffice-departements',
                component: () => import('@/views/backoffice/departements/List.vue'),
            },
            // /nouveau doit être déclaré avant /:id pour ne pas être capturé comme un id
            {
                path: 'departements/nouveau',
                name: 'backoffice-departements-nouveau',
                component: () => import('@/views/backoffice/departements/Form.vue'),
            },
            {
                path: 'departements/:id/modifier',
                name: 'backoffice-departements-modifier',
                component: () => import('@/views/backoffice/departements/Form.vue'),
            },
            {
                path: 'departements/:id',
                name: 'backoffice-departements-detail',
                component: () => import('@/views/backoffice/departements/Show.vue'),
            },

            /* ------------------ SERVICES ------------------ */
            {
                path: 'services',
                name: 'backoffice-services',
                component: () => import('@/views/backoffice/services/List.vue'),
            },
            {
                path: 'services/nouveau',
                name: 'backoffice-services-nouveau',
                component: () => import('@/views/backoffice/services/Form.vue'),
            },
            {
                path: 'services/:id/modifier',
                name: 'backoffice-services-modifier',
                component: () => import('@/views/backoffice/services/Form.vue'),
            },
            {
                path: 'services/:id',
                name: 'backoffice-services-detail',
                component: () => import('@/views/backoffice/services/Show.vue'),
            },
            /* ------------------ PERMISSIONS ------------------ */
            {
                path: 'permissions',
                name: 'backoffice-permissions',
                component: () => import('@/views/backoffice/permissions/List.vue'),
            },
            {
                path: 'permissions/:id',
                name: 'backoffice-permissions-detail',
                component: () => import('@/views/backoffice/permissions/Show.vue'),
            },
            /* ------------------ POSTES ------------------ */
            {
                path: 'postes',
                name: 'backoffice-postes',
                component: () => import('@/views/backoffice/postes/List.vue'),
            },
            {
                path: 'postes/nouveau',
                name: 'backoffice-postes-nouveau',
                component: () => import('@/views/backoffice/postes/Form.vue'),
            },
            {
                path: 'postes/:id/modifier',
                name: 'backoffice-postes-modifier',
                component: () => import('@/views/backoffice/postes/Form.vue'),
            },
            {
                path: 'postes/:id',
                name: 'backoffice-postes-detail',
                component: () => import('@/views/backoffice/postes/Show.vue'),
            },
            {
                path: 'postes/:id/permissions',
                name: 'backoffice-postes-permissions',
                component: () => import('@/views/backoffice/postes/Permission.vue'),
            },
        ],
    },
]
import type { RouteRecordRaw } from 'vue-router'

export const frontofficeRoutes: RouteRecordRaw[] = [
    {
        path: '/login',
        name: 'login',
        component: () => import('@/views/frontoffice/Login.vue'),
        meta: {
            requiresFrontofficeGuest: true,
        },
    },

    {
        path: '/forgot-password',
        name: 'forgot-password',
        component: () => import('@/views/frontoffice/ForgotPassword.vue'),
        meta: {
            requiresFrontofficeGuest: true,
        },
    },

    {
        path: '/reset-password',
        name: 'reset-password',
        component: () => import('@/views/frontoffice/ResetPassword.vue'),
        meta: {
            requiresFrontofficeGuest: true,
        },
    },

    {
        path: '/',
        component: () => import('@/layouts/FrontLayout.vue'),
        meta: {
            requiresFrontoffice: true,
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
            },
            {
                path: 'activites',
                name: 'activites',
                component: () => import('@/views/frontoffice/activites/Liste.vue'),
            },
            /*
                Le regex (\d+) exclut /activites/nouvelle et /activites/a-valider,
                liens morts de la Sidebar : un libelle ne s'ouvrira jamais comme
                un identifiant.
            */
            {
                path: 'activites/:id(\\d+)',
                name: 'activite-detail',
                component: () => import('@/views/frontoffice/activites/Detail.vue'),
            },
            /*
                Le regex (\d+) exclut /activites/nouvelle et /activites/a-valider,
                liens morts de la Sidebar : un libelle ne s'ouvrira jamais comme
                un identifiant. Applique aux deux niveaux : la sous-activite suit
                une activite existante.
            */
            {
                path: 'activites/:id(\\d+)/sous-activites/:sousActiviteId(\\d+)',
                name: 'sous-activite-detail',
                component: () => import('@/views/frontoffice/activites/SousActivite.vue'),
            },
        ],
    },
]
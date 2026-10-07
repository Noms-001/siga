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
        path: '/activation',
        name: 'activation',
        component: () => import('@/views/frontoffice/Activation.vue'),
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
                Le meme composant sert la creation et la modification : le
                formulaire est identique, seule la source des valeurs change
                -- vide a la creation, le contenu renvoye par
                /activites/{id}/formulaire en modification. Deux composants
                dupliqueraient les regles, dont celle qui lie PTA et objectif.

                "nouvelle" est le chemin que la sidebar pointe deja ; c'est
                lui qui nomme la page, et non l'inverse.
            */
            {
                path: 'activites/nouvelle',
                name: 'activites-nouvelle',
                component: () => import('@/views/frontoffice/activites/Ajout.vue'),
            },
            {
                path: 'activites/:id(\\d+)/modifier',
                name: 'activite-modifier',
                component: () => import('@/views/frontoffice/activites/Ajout.vue'),
            },
            /*
                Declaree avant le detail, et le (\d+) de la route suivante
                suffirait a l exclure de toute facon : c'est l ordre des
                declarations qui evite qu'un libelle de sidebar soit lu
                comme un identifiant si le regex evoluait.
            */
            {
                path: 'activites/brouillons',
                name: 'activites-brouillons',
                component: () => import('@/views/frontoffice/activites/Brouillons.vue'),
            },
            /*
                Declaree dans le meme bloc que "brouillons" et pour la meme
                raison : les deux chemins sont nommes, donc aucune de ces routes
                ne peut etre lue comme un identifiant d'activite.
            */
            {
                path: 'activites/a-valider',
                name: 'activites-a-valider',
                component: () => import('@/views/frontoffice/activites/AValider.vue'),
            },
            {
                path: 'activites/:id(\\d+)',
                name: 'activite-detail',
                component: () => import('@/views/frontoffice/activites/Detail.vue'),
            },
            /*
                Applique aux deux niveaux : la sous-activite suit une activite
                existante.
            */
            {
                path: 'activites/:id(\\d+)/sous-activites/:sousActiviteId(\\d+)',
                name: 'sous-activite-detail',
                component: () => import('@/views/frontoffice/activites/SousActivite.vue'),
            },
            {
                path: 'activites/suivi',
                name: 'activites-suivi',
                component: () => import('@/views/frontoffice/activites/Suivi.vue'),
            },
        ],
    },
]
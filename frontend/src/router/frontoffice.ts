import type { RouteRecordRaw } from 'vue-router'

import {
    PermissionAction as A,
    PermissionResource as R,
    permission,
} from '@/utils/authorization'

export const frontofficeRoutes: RouteRecordRaw[] = [
    {
        path: '/validation',
        name: 'validation-directe',
        component: () => import('@/views/frontoffice/activites/AValider.vue'),
        meta: {
            requiresFrontoffice: true,
            permissions: [
                permission(R.VALIDATION_ACTIVITE, A.CONSULTER),
                permission(R.ACTIVITE, A.CONSULTER),
            ],
        },
    },
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
                meta: { permissions: [permission(R.DASHBOARD, A.CONSULTER)] },
            },
            {
                path: 'membres/suivi',
                name: 'membres-suivi',
                component: () => import('@/views/frontoffice/membres/Suivi.vue'),
                meta: { permissions: [permission(R.MEMBRE_SUIVI, A.CONSULTER)] },
            },
            {
                path: 'profile',
                name: 'profile',
                component: () => import('@/views/frontoffice/Profile.vue'),
            },
            {
                path: 'notifications',
                name: 'notifications',
                component: () => import('@/views/frontoffice/Notifications.vue'),
                meta: { permissions: [permission(R.NOTIFICATION, A.CONSULTER)] },
            },

            {
                path: 'notifications/urgentes',
                name: 'notifications-urgentes',
                component: () => import('@/views/frontoffice/NotificationsUrgentes.vue'),
                meta: { permissions: [permission(R.NOTIFICATION, A.CONSULTER)] },
            },
            {
                path: 'activites',
                name: 'activites',
                component: () => import('@/views/frontoffice/activites/Liste.vue'),
                meta: { permissions: [permission(R.ACTIVITE, A.CONSULTER)] },
            },
            {
                path: 'activites/nouvelle',
                name: 'activites-nouvelle',
                component: () => import('@/views/frontoffice/activites/Ajout.vue'),
                meta: { permissions: [permission(R.ACTIVITE, A.CREER)] },
            },
            {
                path: 'activites/importer',
                name: 'activites-importer',
                component: () => import('@/views/frontoffice/activites/Importer.vue'),
                meta: { permissions: [permission(R.ACTIVITE, A.IMPORTER)] },
            },
            {
                path: 'activites/:id(\\d+)/modifier',
                name: 'activite-modifier',
                component: () => import('@/views/frontoffice/activites/Ajout.vue'),
                meta: { permissions: [permission(R.ACTIVITE, A.MODIFIER)] },
            },
            {
                path: 'activites/traitement',
                name: 'activites-traitement',
                component: () => import('@/views/frontoffice/activites/Traitement.vue'),
                meta: {
                    permissions: [
                        permission(R.ACTIVITE, A.CONSULTER),
                        permission(R.VALIDATION_ACTIVITE, A.CONSULTER),
                        permission(R.AFFECTATION_ACTIVITE, A.CONSULTER),
                    ],
                },
            },
            {
                path: 'activites/brouillons',
                name: 'activites-brouillons',
                component: () => import('@/views/frontoffice/activites/Brouillons.vue'),
                meta: { permissions: [permission(R.ACTIVITE, A.CONSULTER)] },
            },
            {
                path: 'activites/a-valider',
                name: 'activites-a-valider',
                component: () => import('@/views/frontoffice/activites/AValider.vue'),
                meta: {
                    permissions: [
                        permission(R.VALIDATION_ACTIVITE, A.CONSULTER),
                        permission(R.ACTIVITE, A.CONSULTER),
                    ],
                },
            },
            {
                path: 'activites/:id(\\d+)',
                name: 'activite-detail',
                component: () => import('@/views/frontoffice/activites/Detail.vue'),
                meta: { permissions: [permission(R.ACTIVITE, A.CONSULTER)] },
            },
            /*
                Applique aux deux niveaux : la sous-activite suit une activite
                existante.
            */
            {
                path: 'activites/:id(\\d+)/sous-activites/:sousActiviteId(\\d+)',
                name: 'sous-activite-detail',
                component: () => import('@/views/frontoffice/activites/SousActivite.vue'),
                meta: {
                    permissions: [
                        permission(R.SOUS_ACTIVITE, A.CONSULTER),
                        permission(R.ACTIVITE, A.CONSULTER),
                    ],
                },
            },
            {
                path: 'activites/suivi',
                name: 'activites-suivi',
                component: () => import('@/views/frontoffice/activites/Suivi.vue'),
                meta: { permissions: [permission(R.ACTIVITE, A.CONSULTER)] },
            },
            {
                path: 'indicateurs',
                name: 'indicateurs',
                component: () => import('@/views/frontoffice/indicateurs/List.vue'),
                meta: { permissions: [permission(R.INDICATEUR, A.CONSULTER)] },
            },
            {
                path: 'indicateurs/:id(\\d+)',
                name: 'indicateur-detail',
                component: () => import('@/views/frontoffice/indicateurs/Show.vue'),
                meta: { permissions: [permission(R.INDICATEUR, A.CONSULTER)] },
            },

            {
                path: '/indicateurs/suivi',
                name: 'indicateurs-suivi',
                component: () => import('@/views/frontoffice/indicateurs/Suivi.vue'),
                meta: {
                    requiresAuth: true,
                    titre: 'Suivi des indicateurs',
                    permissions: [
                        permission(R.VALEUR_INDICATEUR, A.CONSULTER),
                        permission(R.INDICATEUR, A.CONSULTER),
                    ],
                },
            },

            /* ------------------ SIGNALEMENTS ------------------ */
            {
                path: 'signalements',
                name: 'signalements',
                component: () => import('@/views/frontoffice/signalements/List.vue'),
                meta: { permissions: [permission(R.SIGNALEMENT, A.CONSULTER)] },
            },
            {
                path: 'signalements/nouveau',
                name: 'signalement-nouveau',
                component: () => import('@/views/frontoffice/signalements/Form.vue'),
                meta: { permissions: [permission(R.SIGNALEMENT, A.CREER)] },
            },
            {
                path: 'signalements/:id(\\d+)',
                name: 'signalement-detail',
                component: () => import('@/views/frontoffice/signalements/Show.vue'),
                meta: { permissions: [permission(R.SIGNALEMENT, A.CONSULTER)] },
            },
            {
                path: '/plans-action',
                name: 'plans-action',
                component: () => import('@/views/frontoffice/planaction/List.vue'),
                meta: {
                    requiresAuth: true,
                    titre: 'Plans d\'action',
                    permissions: [permission(R.PLAN_ACTION, A.CONSULTER)],
                },
            },
            {
                path: '/plans-action/nouveau',
                name: 'plan-action-create',
                component: () => import('@/views/frontoffice/planaction/Create.vue'),
                meta: {
                    requiresAuth: true,
                    titre: 'Créer un plan d\'action',
                    permissions: [permission(R.PLAN_ACTION, A.CREER)],
                },
            },
            {
                path: '/plans-action/:id',
                name: 'plan-action-detail',
                component: () => import('@/views/frontoffice/planaction/Show.vue'),
                meta: {
                    requiresAuth: true,
                    titre: 'Détail du plan d\'action',
                    permissions: [permission(R.PLAN_ACTION, A.CONSULTER)],
                },
            },
            {
                path: '/demandes-reprise',
                name: 'demandes-reprise',
                component: () => import('@/views/frontoffice/demandereprise/DemandeReprise.vue'),
                meta: {
                    requiresAuth: true,
                    titre: 'Demandes de reprise',
                    permissions: [permission(R.DEMANDE_REPRISE, A.CONSULTER)],
                },
            },
            {
                path: '/utilisateurs/:id/avancement',
                name: 'utilisateur-avancement',
                component: () => import('@/views/frontoffice/utilisateurs/Avancement.vue'),
                meta: {
                    requiresAuth: true,
                    titre: 'Avancement utilisateur',
                    permissions: [
                        permission(R.MEMBRE_SUIVI, A.CONSULTER),
                        permission(R.UTILISATEUR, A.CONSULTER),
                    ],
                },
            },
            {
                path: '/activites/a-affecter',
                name: 'activites-a-affecter',
                component: () => import('@/views/frontoffice/activites/Affectation.vue'),
                meta: {
                    requiresAuth: true,
                    titre: 'Activités à affecter',
                    permissions: [
                        permission(R.AFFECTATION_ACTIVITE, A.CONSULTER),
                        permission(R.ACTIVITE, A.CONSULTER),
                    ],
                },
            },
        ],
    },
]

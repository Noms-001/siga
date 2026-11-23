import type { RouteRecordRaw } from 'vue-router'

import {
    PermissionAction as A,
    PermissionResource as R,
    permission,
} from '@/utils/authorization'

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
                meta: { permissions: [permission(R.DASHBOARD, A.CONSULTER)] },
            },
            /* ------------------ DÉPARTEMENTS ------------------ */
            {
                path: 'departements',
                name: 'backoffice-departements',
                component: () => import('@/views/backoffice/departements/List.vue'),
                meta: { permissions: [permission(R.DEPARTEMENT, A.CONSULTER)] },
            },
            // /nouveau doit être déclaré avant /:id pour ne pas être capturé comme un id
            {
                path: 'departements/nouveau',
                name: 'backoffice-departements-nouveau',
                component: () => import('@/views/backoffice/departements/Form.vue'),
                meta: { permissions: [permission(R.DEPARTEMENT, A.CREER)] },
            },
            {
                path: 'departements/:id/modifier',
                name: 'backoffice-departements-modifier',
                component: () => import('@/views/backoffice/departements/Form.vue'),
                meta: { permissions: [permission(R.DEPARTEMENT, A.MODIFIER)] },
            },
            {
                path: 'departements/:id',
                name: 'backoffice-departements-detail',
                component: () => import('@/views/backoffice/departements/Show.vue'),
                meta: { permissions: [permission(R.DEPARTEMENT, A.CONSULTER)] },
            },

            /* ------------------ SERVICES ------------------ */
            {
                path: 'services',
                name: 'backoffice-services',
                component: () => import('@/views/backoffice/services/List.vue'),
                meta: { permissions: [permission(R.SERVICE, A.CONSULTER)] },
            },
            {
                path: 'services/nouveau',
                name: 'backoffice-services-nouveau',
                component: () => import('@/views/backoffice/services/Form.vue'),
                meta: { permissions: [permission(R.SERVICE, A.CREER)] },
            },
            {
                path: 'services/:id/modifier',
                name: 'backoffice-services-modifier',
                component: () => import('@/views/backoffice/services/Form.vue'),
                meta: { permissions: [permission(R.SERVICE, A.MODIFIER)] },
            },
            {
                path: 'services/:id',
                name: 'backoffice-services-detail',
                component: () => import('@/views/backoffice/services/Show.vue'),
                meta: { permissions: [permission(R.SERVICE, A.CONSULTER)] },
            },
            /* ------------------ PERMISSIONS ------------------ */
            {
                path: 'permissions',
                name: 'backoffice-permissions',
                component: () => import('@/views/backoffice/permissions/List.vue'),
                meta: { permissions: [permission(R.PERMISSION, A.CONSULTER)] },
            },
            {
                path: 'permissions/:id',
                name: 'backoffice-permissions-detail',
                component: () => import('@/views/backoffice/permissions/Show.vue'),
                meta: { permissions: [permission(R.PERMISSION, A.CONSULTER)] },
            },
            /* ------------------ POSTES ------------------ */
            {
                path: 'postes',
                name: 'backoffice-postes',
                component: () => import('@/views/backoffice/postes/List.vue'),
                meta: { permissions: [permission(R.POSTE, A.CONSULTER)] },
            },
            {
                path: 'postes/nouveau',
                name: 'backoffice-postes-nouveau',
                component: () => import('@/views/backoffice/postes/Form.vue'),
                meta: { permissions: [permission(R.POSTE, A.CREER)] },
            },
            {
                path: 'postes/:id/modifier',
                name: 'backoffice-postes-modifier',
                component: () => import('@/views/backoffice/postes/Form.vue'),
                meta: { permissions: [permission(R.POSTE, A.MODIFIER)] },
            },
            {
                path: 'postes/:id',
                name: 'backoffice-postes-detail',
                component: () => import('@/views/backoffice/postes/Show.vue'),
                meta: { permissions: [permission(R.POSTE, A.CONSULTER)] },
            },
            {
                path: 'postes/:id/permissions',
                name: 'backoffice-postes-permissions',
                component: () => import('@/views/backoffice/postes/Permission.vue'),
                meta: {
                    permissions: [
                        permission(R.POSTE_PERMISSION, A.CONSULTER),
                        permission(R.POSTE, A.CONSULTER),
                    ],
                },
            },
            /* ------------------ RÔLES ------------------ */
            {
                path: 'roles',
                name: 'backoffice-roles',
                component: () => import('@/views/backoffice/roles/List.vue'),
                meta: { permissions: [permission(R.ROLE, A.CONSULTER)] },
            },
            {
                path: 'roles/nouveau',
                name: 'backoffice-roles-nouveau',
                component: () => import('@/views/backoffice/roles/Form.vue'),
                meta: { permissions: [permission(R.ROLE, A.CREER)] },
            },
            {
                path: 'roles/:id/modifier',
                name: 'backoffice-roles-modifier',
                component: () => import('@/views/backoffice/roles/Form.vue'),
                meta: { permissions: [permission(R.ROLE, A.MODIFIER)] },
            },
            {
                path: 'roles/:id',
                name: 'backoffice-roles-detail',
                component: () => import('@/views/backoffice/roles/Show.vue'),
                meta: { permissions: [permission(R.ROLE, A.CONSULTER)] },
            },
            /* ------------------ TYPES D'ACTIVITÉ ------------------ */
            {
                path: 'types-activite',
                name: 'backoffice-types-activite',
                component: () => import('@/views/backoffice/types-activite/List.vue'),
                meta: { permissions: [permission(R.TYPE_ACTIVITE, A.CONSULTER)] },
            },
            {
                path: 'types-activite/nouveau',
                name: 'backoffice-types-activite-nouveau',
                component: () => import('@/views/backoffice/types-activite/Form.vue'),
                meta: { permissions: [permission(R.TYPE_ACTIVITE, A.CREER)] },
            },
            {
                path: 'types-activite/:id/modifier',
                name: 'backoffice-types-activite-modifier',
                component: () => import('@/views/backoffice/types-activite/Form.vue'),
                meta: { permissions: [permission(R.TYPE_ACTIVITE, A.MODIFIER)] },
            },
            {
                path: 'types-activite/:id',
                name: 'backoffice-types-activite-detail',
                component: () => import('@/views/backoffice/types-activite/Show.vue'),
                meta: { permissions: [permission(R.TYPE_ACTIVITE, A.CONSULTER)] },
            },
            /* ------------------ SITES ------------------ */
            {
                path: 'sites',
                name: 'backoffice-sites',
                component: () => import('@/views/backoffice/sites/List.vue'),
                meta: { permissions: [permission(R.SITE, A.CONSULTER)] },
            },
            {
                path: 'sites/nouveau',
                name: 'backoffice-sites-nouveau',
                component: () => import('@/views/backoffice/sites/Form.vue'),
                meta: { permissions: [permission(R.SITE, A.CREER)] },
            },
            {
                path: 'sites/:id/modifier',
                name: 'backoffice-sites-modifier',
                component: () => import('@/views/backoffice/sites/Form.vue'),
                meta: { permissions: [permission(R.SITE, A.MODIFIER)] },
            },
            {
                path: 'sites/:id',
                name: 'backoffice-sites-detail',
                component: () => import('@/views/backoffice/sites/Show.vue'),
                meta: { permissions: [permission(R.SITE, A.CONSULTER)] },
            },

            /* ------------------ PRIORITÉS (lecture seule) ------------------ */
            {
                path: 'priorites',
                name: 'backoffice-priorites',
                component: () => import('@/views/backoffice/priorites/List.vue'),
                meta: { permissions: [permission(R.PRIORITE, A.CONSULTER)] },
            },

            /* ------------------ STATUTS (lecture seule) ------------------ */
            {
                path: 'statuts',
                name: 'backoffice-statuts',
                component: () => import('@/views/backoffice/statuts/List.vue'),
                meta: { permissions: [permission(R.STATUT, A.CONSULTER)] },
            },
            /* ------------------ PARAMÈTRES ------------------ */
            {
                path: 'parametres',
                name: 'backoffice-parametres',
                component: () => import('@/views/backoffice/parametres/List.vue'),
                meta: { permissions: [permission(R.PARAMETRE, A.CONSULTER)] },
            },
            /* ------------------ UTILISATEURS ------------------ */
            {
                path: 'utilisateurs',
                name: 'backoffice-utilisateurs',
                component: () => import('@/views/backoffice/utilisateurs/List.vue'),
                meta: { permissions: [permission(R.UTILISATEUR, A.CONSULTER)] },
            },
            {
                path: 'utilisateurs/nouveau',
                name: 'backoffice-utilisateurs-nouveau',
                component: () => import('@/views/backoffice/utilisateurs/Form.vue'),
                meta: { permissions: [permission(R.UTILISATEUR, A.CREER)] },
            },
            {
                path: 'utilisateurs/:id',
                name: 'backoffice-utilisateurs-detail',
                component: () => import('@/views/backoffice/utilisateurs/Show.vue'),
                meta: { permissions: [permission(R.UTILISATEUR, A.CONSULTER)] },
            },
            /* ------------------ PROCÉDURES ------------------ */
            {
                path: 'procedures',
                name: 'backoffice-procedures',
                component: () => import('@/views/backoffice/procedures/List.vue'),
                meta: { permissions: [permission(R.PROCEDURE, A.CONSULTER)] },
            },
            {
                path: 'procedures/nouveau',
                name: 'backoffice-procedures-nouveau',
                component: () => import('@/views/backoffice/procedures/Form.vue'),
                meta: { permissions: [permission(R.PROCEDURE, A.CREER)] },
            },
            {
                path: 'procedures/:id/modifier',
                name: 'backoffice-procedures-modifier',
                component: () => import('@/views/backoffice/procedures/Form.vue'),
                meta: { permissions: [permission(R.PROCEDURE, A.MODIFIER)] },
            },
            {
                path: 'procedures/:id',
                name: 'backoffice-procedures-detail',
                component: () => import('@/views/backoffice/procedures/Show.vue'),
                meta: { permissions: [permission(R.PROCEDURE, A.CONSULTER)] },
            },
            /* ------------------ INDICATEURS ------------------ */
            {
                path: 'indicateurs',
                name: 'backoffice-indicateurs',
                component: () => import('@/views/backoffice/indicateurs/List.vue'),
                meta: { permissions: [permission(R.INDICATEUR, A.CONSULTER)] },
            },
            {
                path: 'indicateurs/nouveau',
                name: 'backoffice-indicateurs-nouveau',
                component: () => import('@/views/backoffice/indicateurs/Form.vue'),
                meta: { permissions: [permission(R.INDICATEUR, A.CREER)] },
            },
            {
                path: 'indicateurs/:id/modifier',
                name: 'backoffice-indicateurs-modifier',
                component: () => import('@/views/backoffice/indicateurs/Form.vue'),
                meta: { permissions: [permission(R.INDICATEUR, A.MODIFIER)] },
            },
            {
                path: 'indicateurs/:id',
                name: 'backoffice-indicateurs-detail',
                component: () => import('@/views/backoffice/indicateurs/Show.vue'),
                meta: { permissions: [permission(R.INDICATEUR, A.CONSULTER)] },
            },
        ],
    },
]

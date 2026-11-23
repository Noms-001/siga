import 'vue-router'

import type { PermissionRequirement } from '@/utils/authorization'

declare module 'vue-router' {
    interface RouteMeta {
        requiresBackoffice?: boolean
        requiresBackofficeGuest?: boolean
        requiresFrontoffice?: boolean
        requiresFrontofficeGuest?: boolean
        requiresAuth?: boolean
        titre?: string

        /**
         * Permissions requises pour acceder a la route.
         *
         * Les permissions sont alternatives : il suffit d'en posseder une.
         * Une route sans permission declaree reste accessible des lors que
         * les controles d'authentification sont satisfaits.
         */
        permissions?: PermissionRequirement[]
    }
}

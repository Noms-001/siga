<template>
    <aside class="back-sidebar" :class="{ open: isOpen, collapsed: isCollapsed }">

        <nav class="back-sidebar__nav">
            <ul class="back-sidebar__list">

                <!-- PRINCIPAL -->
                <li v-if="principalItems.length" class="back-sidebar__item">
                    <span class="back-sidebar__label">Principal</span>
                </li>
                <li v-for="item in principalItems" :key="item.path" class="back-sidebar__item">
                    <router-link class="back-sidebar__link" :to="item.path" :class="{ active: isActive(item.path) }"
                        :title="isCollapsed ? item.label : ''">
                        <i :class="item.icon"></i>
                        <span class="back-sidebar__link-text">{{ item.label }}</span>
                    </router-link>
                </li>

                <!-- GESTION -->
                <li v-if="gestionItems.length" class="back-sidebar__item">
                    <span class="back-sidebar__label">Gestion</span>
                </li>
                <li v-for="item in gestionItems" :key="item.path" class="back-sidebar__item">
                    <router-link class="back-sidebar__link" :to="item.path" :class="{ active: isActive(item.path) }"
                        :title="isCollapsed ? item.label : ''">
                        <i :class="item.icon"></i>
                        <span class="back-sidebar__link-text">{{ item.label }}</span>
                    </router-link>
                </li>

                <!-- RÉFÉRENTIELS -->
                <li v-if="referentielsItems.length" class="back-sidebar__item">
                    <span class="back-sidebar__label">Référentiels</span>
                </li>
                <li v-for="item in referentielsItems" :key="item.path" class="back-sidebar__item">
                    <router-link class="back-sidebar__link" :to="item.path" :class="{ active: isActive(item.path) }"
                        :title="isCollapsed ? item.label : ''">
                        <i :class="item.icon"></i>
                        <span class="back-sidebar__link-text">{{ item.label }}</span>
                    </router-link>
                </li>

                <!-- AUTORISATIONS -->
                <li v-if="autorisationsItems.length" class="back-sidebar__item">
                    <span class="back-sidebar__label">Autorisations</span>
                </li>
                <li v-for="item in autorisationsItems" :key="item.path" class="back-sidebar__item">
                    <router-link class="back-sidebar__link" :to="item.path" :class="{ active: isActive(item.path) }"
                        :title="isCollapsed ? item.label : ''">
                        <i :class="item.icon"></i>
                        <span class="back-sidebar__link-text">{{ item.label }}</span>
                    </router-link>
                </li>

                <!-- CONFIGURATION -->
                <li v-if="configurationItems.length" class="back-sidebar__item">
                    <span class="back-sidebar__label">Configuration</span>
                </li>
                <li v-for="item in configurationItems" :key="item.path" class="back-sidebar__item">
                    <router-link class="back-sidebar__link" :to="item.path" :class="{ active: isActive(item.path) }"
                        :title="isCollapsed ? item.label : ''">
                        <i :class="item.icon"></i>
                        <span class="back-sidebar__link-text">{{ item.label }}</span>
                    </router-link>
                </li>

                <!-- SUPERVISION
                <li v-if="supervisionItems.length" class="back-sidebar__item">
                    <span class="back-sidebar__label">Supervision</span>
                </li>
                <li v-for="item in supervisionItems" :key="item.path" class="back-sidebar__item">
                    <router-link class="back-sidebar__link" :to="item.path" :class="{ active: isActive(item.path) }"
                        :title="isCollapsed ? item.label : ''">
                        <i :class="item.icon"></i>
                        <span class="back-sidebar__link-text">{{ item.label }}</span>
                    </router-link>
                </li> -->

            </ul>
        </nav>
    </aside>

    <div class="back-sidebar__overlay" :class="{ show: isOpen }" @click="closeSidebar"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// --- Types ---
interface MenuItem {
    path: string
    label: string
    icon: string
    permission: string
}

// --- Props ---
defineProps<{
    isOpen?: boolean
    isCollapsed?: boolean
}>()

// --- Emits ---
const emit = defineEmits<{
    (e: 'close'): void
}>()

// --- Router ---
const route = useRoute()
const authStore = useAuthStore()

/* ============================================================
   PRINCIPAL
   ============================================================ */
const principalMenu: MenuItem[] = [
    { path: '/backoffice/dashboard', label: 'Tableau de bord', icon: 'bi bi-grid-1x2', permission: 'dashboard:READ' },
]

/* ============================================================
   GESTION
   ============================================================ */
const gestionMenu: MenuItem[] = [
    { path: '/backoffice/utilisateurs', label: 'Utilisateurs', icon: 'bi bi-people', permission: 'utilisateur:READ' },
]

/* ============================================================
   RÉFÉRENTIELS
   ============================================================ */
const referentielsMenu: MenuItem[] = [
    { path: '/backoffice/departements', label: 'Départements', icon: 'bi bi-building', permission: 'departement:READ' },
    { path: '/backoffice/services', label: 'Services', icon: 'bi bi-diagram-3', permission: 'service:READ' },
    { path: '/backoffice/postes', label: 'Postes', icon: 'bi bi-briefcase', permission: 'poste:READ' },
    { path: '/backoffice/roles', label: 'Rôles', icon: 'bi bi-person-badge', permission: 'role:READ' },
    { path: '/backoffice/types-activite', label: "Types d'activité", icon: 'bi bi-tags', permission: 'type_activite:READ' },
    { path: '/backoffice/sites', label: 'Sites', icon: 'bi bi-geo-alt', permission: 'site:READ' },
    { path: '/backoffice/indicateurs', label: 'Indicateurs', icon: 'bi bi-graph-up-arrow', permission: 'indicateur:READ' },
    { path: '/backoffice/priorites', label: 'Priorités', icon: 'bi bi-flag', permission: 'priorite:READ' },
    { path: '/backoffice/statuts', label: 'Statuts', icon: 'bi bi-circle-half', permission: 'statut:READ' },
]

/* ============================================================
   AUTORISATIONS
   ============================================================ */
const autorisationsMenu: MenuItem[] = [
    { path: '/backoffice/permissions', label: 'Permissions', icon: 'bi bi-shield-check', permission: 'permission:READ' },
]

/* ============================================================
   CONFIGURATION
   ============================================================ */
const configurationMenu: MenuItem[] = [
    // ⚠️ Pages à créer (hors périmètre phases 1 à 5)
    { path: '/backoffice/procedures', label: 'Procédures de validation', icon: 'bi bi-file-earmark-text', permission: 'procedure:READ' },
    { path: '/backoffice/parametres', label: 'Paramètres', icon: 'bi bi-sliders', permission: 'parametre:READ' },
]

/* ============================================================
   SUPERVISION
   ============================================================ */
const supervisionMenu: MenuItem[] = [
    // ⚠️ Pages à créer (hors périmètre phases 1 à 5)
    { path: '/backoffice/activites', label: 'Activités', icon: 'bi bi-clipboard-check', permission: 'activite:READ' },
    { path: '/backoffice/plans-action', label: "Plans d'action", icon: 'bi bi-list-check', permission: 'plan_action:READ' },
    { path: '/backoffice/indicateurs', label: 'Indicateurs', icon: 'bi bi-bar-chart-line', permission: 'indicateur:READ' },
]

const visibleItems = (items: MenuItem[]) => items.filter((item) => {
    const [resource, action] = item.permission.split(':')
    return resource !== undefined && action !== undefined
        && authStore.hasPermission(resource, action)
})
const principalItems = computed(() => visibleItems(principalMenu))
const gestionItems = computed(() => visibleItems(gestionMenu))
const referentielsItems = computed(() => visibleItems(referentielsMenu))
const autorisationsItems = computed(() => visibleItems(autorisationsMenu))
const configurationItems = computed(() => visibleItems(configurationMenu))
const supervisionItems = computed(() => visibleItems(supervisionMenu))

// --- Computed ---
const isActive = (path: string): boolean => {
    return route.path === path || route.path.startsWith(path + '/')
}

// --- Methods ---
const closeSidebar = () => {
    emit('close')
}
</script>

<style scoped>
/* ============================================
   BACKOFFICE SIDEBAR — Design DTS-TSS
   ============================================ */

.back-sidebar {
    position: fixed;
    top: var(--dts-navbar-h);
    bottom: 0;
    left: 0;
    width: var(--dts-sidebar-w);
    z-index: 1045;
    display: flex;
    flex-direction: column;
    background: var(--ink);
    border-right: 1px solid var(--dts-border);
    transition: width 0.22s ease, transform 0.25s ease;
    overflow: hidden;
}

/* --- Navigation --- */
.back-sidebar__nav {
    flex: 1 1 auto;
    overflow-y: auto;
    overflow-x: hidden;
    padding: 0.8rem 0.65rem 1rem;
}

.back-sidebar__nav::-webkit-scrollbar {
    width: 9px;
}

.back-sidebar__nav::-webkit-scrollbar-thumb {
    background: rgba(26, 42, 108, 0.18);
    border-radius: 20px;
    border: 2px solid transparent;
    background-clip: content-box;
}

.back-sidebar__list {
    display: flex;
    flex-direction: column;
    list-style: none;
    padding: 0;
    margin: 0;
}

.back-sidebar__item {
    list-style: none;
}

.back-sidebar__label {
    display: block;
    margin: 0.9rem 0.6rem 0.35rem;
    font-size: 0.64rem;
    font-weight: 700;
    letter-spacing: 0.11em;
    text-transform: uppercase;
    color: #8a95a8;
    white-space: nowrap;
}

.back-sidebar__item:first-child .back-sidebar__label {
    margin-top: 0.2rem;
}

.back-sidebar__link {
    position: relative;
    display: flex;
    align-items: center;
    gap: 0.65rem;
    padding: 0.52rem 0.6rem;
    margin-bottom: 2px;
    border-radius: 10px;
    color: #6b7280;
    font-size: 0.83rem;
    font-weight: 500;
    text-decoration: none;
    white-space: nowrap;
    cursor: pointer;
    transition: background-color 0.15s ease, color 0.15s ease;
}

.back-sidebar__link i {
    width: 20px;
    flex: 0 0 20px;
    text-align: center;
    font-size: 1rem;
    color: #8a95a8;
    transition: color 0.15s ease;
}

.back-sidebar__link:hover {
    color: #f3f6fc;
    background-color: var(--navy-deep);
}

.back-sidebar__link:hover i {
    color: #e2e7fc;
}

.back-sidebar__link.active {
    background: var(--navy);
    color: white;
    font-weight: 700;
    box-shadow: none;
}

.back-sidebar__link.active i {
    color: white;
}

.back-sidebar__link.active::before {
    content: "";
    position: absolute;
    left: 0;
    top: 9px;
    width: 3px;
    height: auto;
    bottom: 9px;
    border-radius: 0 3px 3px 0;
    background: linear-gradient(180deg, #4a6cf7, #d4af37);
}

.back-sidebar__link:focus-visible {
    outline: 2px solid #7cc0f5;
    outline-offset: 1px;
}

/* --- Mode réduit (collapsed) --- */
.back-sidebar.collapsed {
    width: var(--dts-sidebar-w-sm);
}

.back-sidebar.collapsed .back-sidebar__brand {
    justify-content: center;
    padding: 10px 6px;
}

.back-sidebar.collapsed .back-sidebar__brand-wordmark { display: none; }

.back-sidebar.collapsed .back-sidebar__link-text,
.back-sidebar.collapsed .back-sidebar__label {
    display: none;
}

.back-sidebar.collapsed .back-sidebar__link {
    justify-content: center;
    padding-left: 0;
    padding-right: 0;
    gap: 0;
}

.back-sidebar.collapsed .back-sidebar__link i {
    width: auto;
    flex: 0 0 auto;
    font-size: 1.15rem;
}

.back-sidebar.collapsed .back-sidebar__nav {
    padding-left: 0.6rem;
    padding-right: 0.6rem;
}

.back-sidebar.collapsed .back-sidebar__link.active::before {
    left: 0;
}

/* --- Mobile overlay --- */
.back-sidebar__overlay {
    display: none;
    position: fixed;
    top: var(--dts-navbar-h);
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(9, 25, 45, 0.5);
    z-index: 1044;
    animation: dtsFadeIn 0.2s ease;
}

.back-sidebar__overlay.show {
    display: block;
}

@keyframes dtsFadeIn {
    from {
        opacity: 0;
    }

    to {
        opacity: 1;
    }
}

/* --- Responsive --- */
@media (max-width: 991.98px) {
    .back-sidebar {
        transform: translateX(-100%);
        width: var(--dts-sidebar-w);
        box-shadow: none;
    }

    .back-sidebar.open {
        transform: translateX(0);
        box-shadow: var(--dts-shadow-lg);
    }

    .back-sidebar.collapsed {
        width: var(--dts-sidebar-w) !important;
    }

    .back-sidebar.collapsed .back-sidebar__link-text {
        display: inline;
    }

    .back-sidebar.collapsed .back-sidebar__label {
        display: block;
    }

    .back-sidebar.collapsed .back-sidebar__link {
        justify-content: flex-start;
        padding: 0.52rem 0.6rem;
        gap: 0.65rem;
    }

    .back-sidebar.collapsed .back-sidebar__link i {
        width: 20px;
        flex: 0 0 20px;
        font-size: 1rem;
    }
}

@media (min-width: 992px) {
    .back-sidebar {
        transform: translateX(0) !important;
    }

    .back-sidebar__overlay {
        display: none !important;
    }
}
</style>
<template>
    <aside class="back-sidebar" :class="{ open: isOpen, collapsed: isCollapsed }">

        <nav class="back-sidebar__nav">
            <ul class="back-sidebar__list">
                <!-- PRINCIPAL -->
                <li class="back-sidebar__item">
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
                <li class="back-sidebar__item">
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
                <li class="back-sidebar__item">
                    <span class="back-sidebar__label">Référentiels</span>
                </li>
                <li v-for="item in referentielsItems" :key="item.path" class="back-sidebar__item">
                    <router-link class="back-sidebar__link" :to="item.path" :class="{ active: isActive(item.path) }"
                        :title="isCollapsed ? item.label : ''">
                        <i :class="item.icon"></i>
                        <span class="back-sidebar__link-text">{{ item.label }}</span>
                    </router-link>
                </li>

                <!-- ADMINISTRATION -->
                <li class="back-sidebar__item">
                    <span class="back-sidebar__label">Administration</span>
                </li>
                <li v-for="item in adminItems" :key="item.path" class="back-sidebar__item">
                    <router-link class="back-sidebar__link" :to="item.path" :class="{ active: isActive(item.path) }"
                        :title="isCollapsed ? item.label : ''">
                        <i :class="item.icon"></i>
                        <span class="back-sidebar__link-text">{{ item.label }}</span>
                    </router-link>
                </li>
            </ul>
        </nav>
    </aside>

    <div class="back-sidebar__overlay" :class="{ show: isOpen }" @click="closeSidebar"></div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

// --- Types ---
interface MenuItem {
    path: string
    label: string
    icon: string
    permission?: string // Pour une future gestion des permissions
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

// --- Menu Data ---
const principalItems: MenuItem[] = [
    { path: '/dashboard', label: 'Tableau de bord', icon: 'bi bi-house-door' },
    { path: '/activites', label: 'Activités', icon: 'bi bi-clipboard-list' },
    { path: '/notifications', label: 'Notifications', icon: 'bi bi-bell' },
]

const gestionItems: MenuItem[] = [
    { path: '/utilisateurs', label: 'Utilisateurs', icon: 'bi bi-people' },
    { path: '/plan-action', label: 'Plans d\'action', icon: 'bi bi-tasks' },
    { path: '/procedures', label: 'Procédures', icon: 'bi bi-file-text' },
    { path: '/indicateurs', label: 'Indicateurs', icon: 'bi bi-chart-bar' },
]

const referentielsItems: MenuItem[] = [
    { path: '/departements', label: 'Départements', icon: 'bi bi-building' },
    { path: '/services', label: 'Services', icon: 'bi bi-diagram-3' },
    { path: '/postes', label: 'Postes', icon: 'bi bi-briefcase' },
    { path: '/permissions', label: 'Permissions', icon: 'bi bi-shield-check' },
    { path: '/roles', label: 'Rôles', icon: 'bi bi-person-badge' },
]

const adminItems: MenuItem[] = [
    { path: '/parametres', label: 'Paramètres', icon: 'bi bi-gear' },
]

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
    background-color: var(--dts-navy);
    background-image: linear-gradient(180deg, #0d2b4e 0%, #102f57 55%, #0c2747 100%);
    border-right: 1px solid rgba(255, 255, 255, 0.07);
    transition: width 0.22s ease, transform 0.25s ease;
    overflow: hidden;
}

/* --- Navigation --- */
.back-sidebar__nav {
    flex: 1 1 auto;
    overflow-y: auto;
    overflow-x: hidden;
    padding: 0.7rem 0.55rem 1rem;
}

.back-sidebar__nav::-webkit-scrollbar {
    width: 9px;
}

.back-sidebar__nav::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.18);
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
    color: rgba(255, 255, 255, 0.38);
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
    border-radius: 7px;
    color: rgba(255, 255, 255, 0.74);
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
    color: rgba(255, 255, 255, 0.62);
    transition: color 0.15s ease;
}

.back-sidebar__link:hover {
    background-color: rgba(255, 255, 255, 0.07);
    color: #fff;
}

.back-sidebar__link:hover i {
    color: #fff;
}

.back-sidebar__link.active {
    background: linear-gradient(90deg, rgba(31, 111, 178, 0.95), rgba(31, 111, 178, 0.62));
    color: #fff;
    font-weight: 600;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.back-sidebar__link.active i {
    color: #fff;
}

.back-sidebar__link.active::before {
    content: "";
    position: absolute;
    left: -0.55rem;
    top: 50%;
    width: 3px;
    height: 22px;
    margin-top: -11px;
    border-radius: 0 3px 3px 0;
    background: #7cc0f5;
}

.back-sidebar__link:focus-visible {
    outline: 2px solid #7cc0f5;
    outline-offset: 1px;
}

/* --- Mode réduit (collapsed) --- */
.back-sidebar.collapsed {
    width: var(--dts-sidebar-w-sm);
}

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
    left: -0.6rem;
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

    /* Neutralise l'état « replié » du mode bureau */
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
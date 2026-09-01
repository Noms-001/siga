<template>
    <aside class="sidebar" :class="{ open: isOpen, collapsed: isCollapsed }">

        <nav class="sidebar-nav">
            <ul class="nav flex-column">
                <!-- PRINCIPAL -->
                <li class="nav-item">
                    <span class="nav-label">Principal</span>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/dashboard" :class="{ active: isActive('/dashboard') }">
                        <i class="bi bi-house-door"></i>
                        <span>Tableau de bord</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/activites" :class="{ active: isActive('/activites') }">
                        <i class="bi bi-clipboard-list"></i>
                        <span>Activités</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/suivi-activites"
                        :class="{ active: isActive('/suivi-activites') }">
                        <i class="bi bi-chart-line"></i>
                        <span>Suivi des activités</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/affectation-activites"
                        :class="{ active: isActive('/affectation-activites') }">
                        <i class="bi bi-user-check"></i>
                        <span>Affectation des activités</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/indicateurs" :class="{ active: isActive('/indicateurs') }">
                        <i class="bi bi-chart-bar"></i>
                        <span>Indicateurs</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/suivi-indicateurs"
                        :class="{ active: isActive('/suivi-indicateurs') }">
                        <i class="bi bi-chart-line"></i>
                        <span>Suivi des indicateurs</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/plan-action" :class="{ active: isActive('/plan-action') }">
                        <i class="bi bi-tasks"></i>
                        <span>Plans d'actions</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/notifications" :class="{ active: isActive('/notifications') }">
                        <i class="bi bi-bell position-relative">
                            <span class="badge-notif-sidebar-icon" v-if="notificationCount > 0 && isCollapsed">
                                {{ notificationCount }}
                            </span>
                        </i>
                        <span>Notifications</span>
                        <span class="badge-notif-sidebar-text ms-auto" v-if="notificationCount > 0">
                            {{ notificationCount }}
                        </span>
                    </router-link>
                </li>

                <!-- RAPPORTS -->
                <li class="nav-item">
                    <span class="nav-label">Rapports</span>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/rapports" :class="{ active: isActive('/rapports') }">
                        <i class="bi bi-file-earmark"></i>
                        <span>Rapports</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/statistiques" :class="{ active: isActive('/statistiques') }">
                        <i class="bi bi-chart-pie"></i>
                        <span>Statistiques</span>
                    </router-link>
                </li>

                <!-- ADMINISTRATION -->
                <li class="nav-item">
                    <span class="nav-label">Administration</span>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/utilisateurs" :class="{ active: isActive('/utilisateurs') }">
                        <i class="bi bi-people"></i>
                        <span>Utilisateurs</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/parametres" :class="{ active: isActive('/parametres') }">
                        <i class="bi bi-gear"></i>
                        <span>Paramètres</span>
                    </router-link>
                </li>
            </ul>
        </nav>
    </aside>

    <div class="sidebar-overlay" :class="{ show: isOpen }" @click="closeSidebar"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

// --- Props ---
const props = defineProps<{
    isOpen?: boolean
    isCollapsed?: boolean
    notificationCount?: number
}>()

// --- Emits ---
const emit = defineEmits<{
    (e: 'close'): void
    (e: 'toggle-collapse'): void
}>()

// --- Router ---
const route = useRoute()
const router = useRouter()

// --- Computed ---
const isActive = (path: string) => {
    return route.path === path || route.path.startsWith(path + '/')
}

// --- Methods ---
const closeSidebar = () => {
    emit('close')
}

const goToDashboard = () => {
    router.push('/dashboard')
    if (window.innerWidth < 992) {
        closeSidebar()
    }
}

// --- Default values ---
const notificationCount = computed(() => props.notificationCount ?? 0)
</script>

<style scoped>
/* ============================================
   SIDEBAR - Styles spécifiques au composant
   ============================================ */

.sidebar {
    position: fixed;
    top: 64px;
    left: 0;
    bottom: 0;
    width: 260px;
    background-color: var(--card-bg);
    border-right: 1px solid var(--border-color);
    z-index: 1040;
    overflow-y: auto;
    transition: width 0.3s ease, transform 0.3s ease;
    transform: translateX(0);
    padding-bottom: 1rem;
    box-shadow: var(--shadow-sm);
}

.sidebar-nav {
    padding: 0.75rem 0.5rem;
}

.sidebar-nav .nav {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    list-style: none;
    padding: 0;
    margin: 0;
}

.sidebar-nav .nav-item {
    list-style: none;
}

.sidebar-nav .nav-label {
    display: block;
    padding: 0.5rem 0.75rem 0.25rem;
    font-size: 0.7rem;
    font-weight: 700;
    letter-spacing: 0.5px;
    color: var(--text-muted-custom);
    text-transform: uppercase;
    transition: opacity 0.2s;
}

.sidebar-nav .nav-link {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.5rem 0.8rem;
    border-radius: 12px;
    color: var(--text-color);
    font-weight: 400;
    text-decoration: none;
    transition: background 0.15s, color 0.15s;
    white-space: nowrap;
    cursor: pointer;
}

.sidebar-nav .nav-link i {
    width: 1.25rem;
    text-align: center;
    color: var(--text-muted-custom);
    font-size: 1rem;
    flex-shrink: 0;
    position: relative;
}

.sidebar-nav .nav-link span {
    transition: opacity 0.2s;
}

.sidebar-nav .nav-link:hover {
    background-color: var(--bg-color);
    color: var(--primary-color);
}

.sidebar-nav .nav-link:hover i {
    color: var(--primary-color);
}

.sidebar-nav .nav-link.active {
    background-color: var(--primary-color);
    color: white;
    border-radius: 12px;
}

.sidebar-nav .nav-link.active i {
    color: white;
}

/* Badges notifications */
.badge-notif-sidebar-text {
    background-color: #dc2626;
    color: white;
    font-size: 0.6rem;
    font-weight: 700;
    padding: 2px 8px;
    border-radius: 12px;
    line-height: 1.4;
    margin-left: auto;
}

.badge-notif-sidebar-icon {
    position: absolute;
    top: -8px;
    right: -10px;
    background-color: #dc2626;
    color: white;
    font-size: 0.55rem;
    font-weight: 700;
    padding: 1px 5px;
    border-radius: 50%;
    border: 2px solid var(--card-bg);
    line-height: 1.2;
    min-width: 16px;
    text-align: center;
}

/* --- Mode réduit (collapsed) --- */
.sidebar.collapsed {
    width: 72px !important;
}

.sidebar.collapsed .nav-link span:not(.badge-notif-sidebar-icon),
.sidebar.collapsed .nav-label {
    display: none;
}

.sidebar.collapsed .sidebar-nav .nav-link {
    justify-content: center;
    padding: 0.75rem 0;
    gap: 0;
}

.sidebar.collapsed .sidebar-nav .nav-link i {
    width: auto;
    font-size: 1.2rem;
}

.sidebar.collapsed .badge-notif-sidebar-text {
    display: none;
}

.sidebar.collapsed .badge-notif-sidebar-icon {
    display: block;
}

/* --- Mobile overlay --- */
.sidebar-overlay {
    display: none;
    position: fixed;
    top: 64px;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.35);
    z-index: 1039;
}

.sidebar-overlay.show {
    display: block;
}

/* --- Responsive --- */
@media (max-width: 991.98px) {
    .sidebar {
        transform: translateX(-100%);
        width: 280px;
    }

    .sidebar.open {
        transform: translateX(0);
    }

    .sidebar-overlay.show {
        display: block;
    }

    /* Désactiver le mode réduit sur mobile */
    .sidebar.collapsed {
        width: 280px !important;
    }

    .sidebar.collapsed .nav-link span:not(.badge-notif-sidebar-icon),
    .sidebar.collapsed .nav-label {
        display: inline;
    }

    .sidebar.collapsed .badge-notif-sidebar-text {
        display: inline;
    }

    .sidebar.collapsed .badge-notif-sidebar-icon {
        display: none;
    }

    .sidebar.collapsed .sidebar-nav .nav-link {
        justify-content: flex-start;
        padding: 0.5rem 0.75rem;
        gap: 0.75rem;
    }

    .sidebar.collapsed .sidebar-nav .nav-link i {
        width: 1.25rem;
        font-size: 1rem;
    }
}

@media (min-width: 992px) {
    .sidebar {
        transform: translateX(0) !important;
    }

    .sidebar-overlay {
        display: none !important;
    }
}
</style>
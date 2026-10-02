<template>
    <aside class="sidebar" :class="{ open: isOpen, collapsed: isCollapsed }">
        <nav class="sidebar-nav">
            <ul class="nav flex-column">
                <!-- ========================================== -->
                <!-- 📊 TABLEAU DE BORD                         -->
                <!-- ========================================== -->
                <li class="nav-item">
                    <span class="nav-label">Tableau de bord</span>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/dashboard" :class="{ active: isActive('/dashboard') }">
                        <i class="bi bi-speedometer2"></i>
                        <span>Dashboard</span>
                    </router-link>
                </li>

                <!-- ========================================== -->
                <!-- 📋 ACTIVITÉS                              -->
                <!-- ========================================== -->
                <li class="nav-item">
                    <span class="nav-label">Activités</span>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/activites" :class="{ active: isActive('/activites') }">
                        <i class="bi bi-list-task"></i>
                        <span>Liste des activités</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/activites/nouvelle"
                        :class="{ active: isActive('/activites/nouvelle') }">
                        <i class="bi bi-plus-circle"></i>
                        <span>Nouvelle activité</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/affectation" :class="{ active: isActive('/affectation') }">
                        <i class="bi bi-person-check"></i>
                        <span>Affectation</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <!--
                        Pointe les activites en cours de redaction, pas la
                        file de validation : ce sont deux listes opposées,
                        l'une alimentée par l'autre.

                    -->
                    <router-link class="nav-link" to="/activites/brouillons"
                        :class="{ active: isActive('/activites/brouillons') }">
                        <i class="bi bi-pencil-square"></i>
                        <span>Activités créées</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <!--
                        L'autre bout du circuit : ce qui a été soumis et attend
                        une décision. Distinct de "Activités créées" par le
                        sens du parcours, et non par une nuance de libellé.
                    -->
                    <router-link class="nav-link" to="/activites/a-valider"
                        :class="{ active: isActive('/activites/a-valider') }">
                        <i class="bi bi-check2-square"></i>
                        <span>Activités à valider</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/suivi-activites"
                        :class="{ active: isActive('/suivi-activites') }">
                        <i class="bi bi-graph-up-arrow"></i>
                        <span>Suivi des activités</span>
                    </router-link>
                </li>

                <!-- ========================================== -->
                <!-- 📑 PROCÉDURES                             -->
                <!-- ========================================== -->
                <li class="nav-item">
                    <span class="nav-label">Procédures</span>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/procedures" :class="{ active: isActive('/procedures') }">
                        <i class="bi bi-files"></i>
                        <span>Liste des procédures</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/procedures/nouvelle"
                        :class="{ active: isActive('/procedures/nouvelle') }">
                        <i class="bi bi-file-earmark-plus"></i>
                        <span>Nouvelle procédure</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/suivi-procedures"
                        :class="{ active: isActive('/suivi-procedures') }">
                        <i class="bi bi-clock-history"></i>
                        <span>Suivi des procédures</span>
                    </router-link>
                </li>

                <!-- ========================================== -->
                <!-- 🎯 PLANS D'ACTION                         -->
                <!-- ========================================== -->
                <li class="nav-item">
                    <span class="nav-label">Plans d'action</span>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/plan-action" :class="{ active: isActive('/plan-action') }">
                        <i class="bi bi-list-ul"></i>
                        <span>Liste des plans d'action</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/plan-action/nouveau"
                        :class="{ active: isActive('/plan-action/nouveau') }">
                        <i class="bi bi-plus-square"></i>
                        <span>Nouveau plan d'action</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/suivi-plan-action"
                        :class="{ active: isActive('/suivi-plan-action') }">
                        <i class="bi bi-arrow-repeat"></i>
                        <span>Suivi des plans d'action</span>
                    </router-link>
                </li>

                <!-- ========================================== -->
                <!-- 📈 INDICATEURS                            -->
                <!-- ========================================== -->
                <li class="nav-item">
                    <span class="nav-label">Indicateurs</span>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/indicateurs" :class="{ active: isActive('/indicateurs') }">
                        <i class="bi bi-bar-chart-fill"></i>
                        <span>Liste des indicateurs</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/indicateurs/nouveau"
                        :class="{ active: isActive('/indicateurs/nouveau') }">
                        <i class="bi bi-bar-chart"></i>
                        <span>Nouvel indicateur</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/suivi-indicateurs"
                        :class="{ active: isActive('/suivi-indicateurs') }">
                        <i class="bi bi-graph-up"></i>
                        <span>Suivi des indicateurs</span>
                    </router-link>
                </li>

                <!-- ========================================== -->
                <!-- 👥 UTILISATEURS                           -->
                <!-- ========================================== -->
                <li class="nav-item">
                    <span class="nav-label">Utilisateurs</span>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/utilisateurs" :class="{ active: isActive('/utilisateurs') }">
                        <i class="bi bi-people"></i>
                        <span>Consultation des membres</span>
                    </router-link>
                </li>

                <!-- ========================================== -->
                <!-- 🔔 NOTIFICATIONS                          -->
                <!-- ========================================== -->
                <li class="nav-item">
                    <span class="nav-label">Notifications</span>
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

                <!-- ========================================== -->
                <!-- 👤 MON PROFIL                             -->
                <!-- ========================================== -->
                <li class="nav-item">
                    <span class="nav-label">Mon profil</span>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/profile" :class="{ active: isActive('/profile') }">
                        <i class="bi bi-person"></i>
                        <span>Profil</span>
                    </router-link>
                </li>
                <li class="nav-item">
                    <router-link class="nav-link" to="/profile/change-password"
                        :class="{ active: isActive('/profile/change-password') }">
                        <i class="bi bi-key"></i>
                        <span>Modifier mon mot de passe</span>
                    </router-link>
                </li>
            </ul>
        </nav>
    </aside>

    <div class="sidebar-overlay" :class="{ show: isOpen }" @click="closeSidebar"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

// --- Props ---
const props = defineProps<{
    isOpen?: boolean
    isCollapsed?: boolean
    notificationCount?: number
}>()

// --- Emits ---
const emit = defineEmits<{
    (e: 'close'): void
}>()

// --- Router ---
const route = useRoute()

// --- Computed ---
const isActive = (path: string) => {
    return route.path === path 
}

// --- Methods ---
const closeSidebar = () => {
    emit('close')
}

// --- Default values ---
const notificationCount = computed(() => props.notificationCount ?? 0)
</script>

<style scoped>
/* ============================================
   FRONTOFFICE SIDEBAR — Design DTS-TSS
   ============================================ */

.sidebar {
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
.sidebar-nav {
    flex: 1 1 auto;
    overflow-y: auto;
    overflow-x: hidden;
    padding: 0.7rem 0.55rem 1rem;
}

.sidebar-nav::-webkit-scrollbar {
    width: 9px;
}

.sidebar-nav::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.18);
    border-radius: 20px;
    border: 2px solid transparent;
    background-clip: content-box;
}

.nav {
    display: flex;
    flex-direction: column;
    list-style: none;
    padding: 0;
    margin: 0;
}

.nav-item {
    list-style: none;
}

.nav-label {
    display: block;
    margin: 0.9rem 0.6rem 0.35rem;
    font-size: 0.64rem;
    font-weight: 700;
    letter-spacing: 0.11em;
    text-transform: uppercase;
    color: rgba(255, 255, 255, 0.38);
    white-space: nowrap;
}

.nav-item:first-child .nav-label {
    margin-top: 0.2rem;
}

.nav-link {
    position: relative;
    display: flex;
    align-items: center;
    gap: 0.65rem;
    padding: 0.52rem 0.6rem;
    margin-bottom: 2px;
    border-radius: 7px;
    color: rgba(255, 255, 255, 0.74) !important;
    font-size: 0.83rem;
    font-weight: 500;
    text-decoration: none;
    white-space: nowrap;
    cursor: pointer;
    transition: background-color 0.15s ease, color 0.15s ease;
}

.nav-link i {
    width: 20px;
    flex: 0 0 20px;
    text-align: center;
    font-size: 1rem;
    color: rgba(255, 255, 255, 0.62);
    transition: color 0.15s ease;
}

.nav-link:hover {
    background-color: rgba(255, 255, 255, 0.07);
    color: #fff !important;
}

.nav-link:hover i {
    color: #fff;
}

.nav-link.active {
    background: linear-gradient(90deg, rgba(31, 111, 178, 0.95), rgba(31, 111, 178, 0.62));
    color: #fff !important;
    font-weight: 600;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.nav-link.active i {
    color: #fff;
}

.nav-link.active::before {
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

.nav-link:focus-visible {
    outline: 2px solid #7cc0f5;
    outline-offset: 1px;
}

/* --- Badge notifications --- */
.badge-notif-sidebar-icon {
    display: inline-grid;
    place-items: center;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    margin-left: auto;
    border-radius: 20px;
    background: var(--dts-danger);
    color: #fff;
    font-size: 0.64rem;
    font-weight: 700;
    line-height: 1;
}

.badge-notif-sidebar-text {
    margin-left: auto;
    color: #ffb3ab;
    font-weight: 600;
}

.nav-link.active .badge-notif-sidebar-text {
    color: #fff;
}

/* --- Mode réduit (collapsed) --- */
.sidebar.collapsed {
    width: var(--dts-sidebar-w-sm);
}

.sidebar.collapsed .nav-link span:not(.badge-notif-sidebar-text),
.sidebar.collapsed .nav-label,
.sidebar.collapsed .badge-notif-sidebar-text {
    display: none;
}

.sidebar.collapsed .nav-link {
    justify-content: center;
    padding-left: 0;
    padding-right: 0;
    gap: 0;
}

.sidebar.collapsed .nav-link i {
    width: auto;
    flex: 0 0 auto;
    font-size: 1.15rem;
}

.sidebar.collapsed .sidebar-nav {
    padding-left: 0.6rem;
    padding-right: 0.6rem;
}

.sidebar.collapsed .nav-link.active::before {
    left: -0.6rem;
}

.sidebar.collapsed .badge-notif-sidebar-icon {
    position: absolute;
    top: 2px;
    right: 2px;
    min-width: 15px;
    height: 15px;
    font-size: 0.58rem;
}

/* --- Mobile overlay --- */
.sidebar-overlay {
    display: none;
    position: fixed;
    top: var(--dts-navbar-h);
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(9, 25, 45, 0.5);
    z-index: 1044;
    animation: dtsFoFadeIn 0.2s ease;
}

.sidebar-overlay.show {
    display: block;
}

@keyframes dtsFoFadeIn {
    from {
        opacity: 0;
    }

    to {
        opacity: 1;
    }
}

/* --- Responsive --- */
@media (max-width: 991.98px) {
    .sidebar {
        transform: translateX(-100%);
        width: var(--dts-sidebar-w);
        box-shadow: none;
    }

    .sidebar.open {
        transform: translateX(0);
        box-shadow: var(--dts-shadow-lg);
    }

    /* Neutralise l'état « replié » du mode bureau */
    .sidebar.collapsed {
        width: var(--dts-sidebar-w) !important;
    }

    .sidebar.collapsed .nav-link span:not(.badge-notif-sidebar-text) {
        display: inline;
    }

    .sidebar.collapsed .nav-label {
        display: block;
    }

    .sidebar.collapsed .nav-link {
        justify-content: flex-start;
        padding: 0.52rem 0.6rem;
        gap: 0.65rem;
    }

    .sidebar.collapsed .nav-link i {
        width: 20px;
        flex: 0 0 20px;
        font-size: 1rem;
    }

    .sidebar.collapsed .badge-notif-sidebar-icon {
        position: static;
        min-width: 18px;
        height: 18px;
        font-size: 0.64rem;
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

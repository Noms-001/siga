<template>
    <nav class="top-nav">
        <div class="nav-left">
            <button class="btn-hamburger d-lg-none" @click="toggleSidebar" type="button">
                <i class="bi bi-list"></i>
            </button>
            <div class="logo-icon" @click="toggleSidebar">
                <i class="bi bi-shield-shaded"></i> DTS
            </div>
            <div class="nav-title d-none d-sm-inline-block">
                Département Etudes Sureté et Sécurité

                <template v-if="authStore.user?.service">
                    <span class="nav-separator">|</span>
                    {{ authStore.user.service }}
                </template>
            </div>
        </div>

        <div class="nav-right">
            <div class="search-bar">
                <i class="bi bi-search"></i>
                <input type="text" placeholder="Rechercher..." v-model="searchQuery" @keyup.enter="handleSearch">
            </div>

            <!-- Notifications -->
            <div class="notif-wrapper">
                <button class="notif-btn" @click="toggleNotifications" aria-label="Notifications" ref="notifBtnRef">
                    <i class="bi bi-bell"></i>
                    <span class="badge-notif-navbar" v-if="unreadCount > 0">
                        {{ unreadCount }}
                    </span>
                </button>

                <div class="notif-dropdown" :class="{ open: isNotifOpen }" ref="notifDropdownRef" role="menu">
                    <div class="notif-dropdown-header">
                        <h4>Notifications</h4>
                        <span>{{ unreadCount }} non lues</span>
                    </div>

                    <div class="notif-list">
                        <div v-for="notif in notifications" :key="notif.id" class="notif-item" :class="notif.type">
                            <div class="notif-icon">
                                <i :class="notif.icon"></i>
                            </div>
                            <div class="notif-content">
                                <div class="notif-title">
                                    {{ notif.title }}
                                    <span class="badge-urgence" :class="notif.urgence" v-if="notif.urgence">
                                        {{ notif.urgence }}
                                    </span>
                                </div>
                                <div class="notif-desc">{{ notif.description }}</div>
                                <div class="notif-meta">
                                    <span class="notif-time">
                                        <i class="bi bi-clock"></i> {{ notif.time }}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="notif-dropdown-footer">
                        <router-link to="/notifications">
                            <i class="bi bi-arrow-right"></i> Voir toutes les notifications
                        </router-link>
                    </div>
                </div>
            </div>

            <!-- User dropdown -->
            <div class="user-menu" ref="userMenuRef">

                <button type="button" class="user-menu-toggle" @click="toggleUserMenu" :aria-expanded="isUserMenuOpen"
                    aria-label="Menu utilisateur">
                    <div class="user-avatar">
                        {{ userInitials }}
                    </div>
                </button>

                <div v-if="isUserMenuOpen" class="user-menu-dropdown">
                    <div class="user-menu-header">
                        <span class="user-menu-name">
                            {{ authStore.user?.nom ?? 'Utilisateur' }}
                        </span>

                        <span class="user-menu-firstname">
                            {{ authStore.user?.prenom ?? '' }}
                        </span>

                        <span class="user-menu-role">
                            {{ userRole }}
                        </span>
                    </div>

                    <hr class="dropdown-divider">

                    <RouterLink to="/profile" class="user-menu-item" @click="isUserMenuOpen = false">
                        <i class="bi bi-person"></i>
                        <span>Mon profil</span>
                    </RouterLink>

                    <RouterLink to="/settings" class="user-menu-item" @click="isUserMenuOpen = false">
                        <i class="bi bi-gear"></i>
                        <span>Paramètres</span>
                    </RouterLink>

                    <hr class="dropdown-divider">

                    <button type="button" class="user-menu-item logout" @click="handleLogout">
                        <i class="bi bi-box-arrow-right"></i>
                        <span>Déconnexion</span>
                    </button>
                </div>

            </div>
        </div>
    </nav>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

// --- Types ---
interface Notification {
    id: number
    title: string
    description: string
    time: string
    type: 'critique' | 'normal'
    urgence?: 'critique' | 'elevee' | 'moyenne' | 'faible'
    icon: string
}

// --- Props & Emits ---
const emit = defineEmits<{
    (e: 'toggle-sidebar'): void
}>()

// --- Router ---
const router = useRouter()

// --- State ---
const searchQuery = ref('')
const isNotifOpen = ref(false)
const isUserMenuOpen = ref(false)
const notifBtnRef = ref<HTMLButtonElement | null>(null)
const notifDropdownRef = ref<HTMLDivElement | null>(null)
const userMenuRef = ref<HTMLDivElement | null>(null)

const userRole = computed(() => {
    return authStore.user?.poste ?? 'Utilisateur'
})

const userInitials = computed(() => {
    if (!authStore.user) {
        return 'U'
    }

    return `${authStore.user.prenom.charAt(0)}${authStore.user.nom.charAt(0)}`
        .toUpperCase()
})

// --- Notifications simulées ---
const notifications = ref<Notification[]>([
    {
        id: 1,
        title: 'Nouvelle activité',
        description: 'Une nouvelle activité a été créée dans le module Sécurité',
        time: 'Il y a 5 min',
        type: 'critique',
        urgence: 'elevee',
        icon: 'bi bi-exclamation-triangle'
    },
    {
        id: 2,
        title: 'Mise à jour',
        description: 'Le système a été mis à jour avec succès',
        time: 'Il y a 1 heure',
        type: 'normal',
        urgence: 'faible',
        icon: 'bi bi-info-circle'
    },
    {
        id: 3,
        title: 'Rapport disponible',
        description: 'Le rapport trimestriel est maintenant disponible',
        time: 'Il y a 3 heures',
        type: 'normal',
        icon: 'bi bi-file-earmark'
    },
    {
        id: 4,
        title: 'Alerte sécurité',
        description: 'Une tentative de connexion suspecte a été détectée',
        time: 'Il y a 5 heures',
        type: 'critique',
        urgence: 'critique',
        icon: 'bi bi-shield-halved'
    }
])

const unreadCount = computed(() => {
    return notifications.value.filter(n => n.type === 'critique').length
})

// --- Methods ---
const toggleSidebar = () => {
    emit('toggle-sidebar')
}

const toggleNotifications = () => {
    isNotifOpen.value = !isNotifOpen.value
    if (isUserMenuOpen.value) isUserMenuOpen.value = false
}

const toggleUserMenu = () => {
    isUserMenuOpen.value = !isUserMenuOpen.value
    if (isNotifOpen.value) isNotifOpen.value = false
}

const goToDashboard = () => {
    router.push('/dashboard')
}

const handleSearch = () => {
    if (searchQuery.value.trim()) {
        console.log('Recherche:', searchQuery.value)
        // Ici tu peux implémenter la logique de recherche
    }
}

const handleLogout = async () => {
    try {
        isUserMenuOpen.value = false

        await authStore.logout()

        console.log('Déconnexion réussie')

        await router.push('/login')
    } catch (error) {
        console.error('Erreur lors de la déconnexion :', error)
    }
}

// --- Click outside handlers ---
const handleClickOutside = (event: MouseEvent) => {
    const target = event.target as HTMLElement

    // Fermer les notifications si on clique en dehors
    if (isNotifOpen.value && notifBtnRef.value && notifDropdownRef.value) {
        if (!notifBtnRef.value.contains(target) && !notifDropdownRef.value.contains(target)) {
            isNotifOpen.value = false
        }
    }

    if (
        isUserMenuOpen.value &&
        userMenuRef.value &&
        !userMenuRef.value.contains(target)
    ) {
        isUserMenuOpen.value = false
    }
}

// --- Lifecycle ---
onMounted(() => {
    document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
    document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
/* ============================================
   NAVBAR - Styles spécifiques au composant
   ============================================ */

.top-nav {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    height: 64px;
    background-color: var(--primary-color);
    color: white;
    padding: 0 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    z-index: 2000;
    flex-shrink: 0;
}

.nav-left {
    display: flex;
    align-items: center;
    gap: 16px;
}

.nav-right {
    display: flex;
    align-items: center;
    gap: 20px;
}

.nav-separator {
    margin: 0 10px;
    color: rgba(255, 255, 255, 0.4);
}

.btn-hamburger {
    background: none;
    border: none;
    color: white;
    font-size: 1.2rem;
    padding: 0 4px;
    cursor: pointer;
    transition: opacity 0.2s;
}

.btn-hamburger:hover {
    opacity: 0.8;
}

.logo-icon {
    font-size: 1.2rem;
    font-weight: bold;
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    transition: opacity 0.2s;
}

.logo-icon:hover {
    opacity: 0.8;
}

.nav-title {
    font-size: 0.95rem;
    border-left: 1px solid rgba(255, 255, 255, 0.3);
    padding-left: 16px;
    font-weight: 500;
}

.search-bar {
    background-color: rgba(255, 255, 255, 0.15);
    border-radius: 4px;
    padding: 6px 12px;
    display: flex;
    align-items: center;
    gap: 8px;
}

.search-bar input {
    background: transparent;
    border: none;
    color: white;
    outline: none;
    width: 140px;
}

.search-bar input::placeholder {
    color: rgba(255, 255, 255, 0.7);
}

.user-avatar {
    width: 32px;
    height: 32px;
    background-color: rgba(255, 255, 255, 0.2);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-weight: bold;
    font-size: 0.8rem;
    text-transform: uppercase;
}

/* --- Notifications --- */
.notif-wrapper {
    position: relative;
}

.notif-btn {
    background: transparent;
    border: none;
    color: rgba(255, 255, 255, 0.8);
    font-size: 1.2rem;
    cursor: pointer;
    position: relative;
    padding: 6px 8px;
    border-radius: 8px;
    transition: background 0.2s, color 0.2s;
}

.notif-btn:hover {
    background: rgba(255, 255, 255, 0.08);
    color: #fff;
}

.badge-notif-navbar {
    position: absolute;
    top: -4px;
    right: -4px;
    background: #dc2626;
    color: #fff;
    font-size: 0.6rem;
    font-weight: 700;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 2px solid var(--primary-color);
    line-height: 1;
}

.notif-dropdown {
    position: absolute;
    right: 0;
    top: calc(100% + 12px);
    width: 380px;
    max-width: 90vw;
    background: var(--card-bg);
    border-radius: 12px;
    box-shadow: var(--shadow-md);
    opacity: 0;
    visibility: hidden;
    transform: translateY(-8px) scale(0.97);
    transition: opacity 0.25s ease, transform 0.25s ease, visibility 0.25s;
    transform-origin: top right;
    overflow: hidden;
    z-index: 1060;
}

.notif-dropdown.open {
    opacity: 1;
    visibility: visible;
    transform: translateY(0) scale(1);
}

.notif-dropdown-header {
    padding: 1rem 1.2rem 0.6rem 1.2rem;
    border-bottom: 1px solid var(--border-color);
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.notif-dropdown-header h4 {
    font-size: 0.95rem;
    font-weight: 600;
    color: var(--text-color);
    margin: 0;
}

.notif-dropdown-header span {
    font-size: 0.8rem;
    color: var(--text-muted-custom);
    background: var(--bg-color);
    padding: 0.2rem 0.7rem;
    border-radius: 20px;
}

.notif-list {
    max-height: 380px;
    overflow-y: auto;
    padding: 0.4rem 0;
}

.notif-list::-webkit-scrollbar {
    width: 4px;
}

.notif-list::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 4px;
}

.notif-item {
    display: flex;
    gap: 0.9rem;
    padding: 0.8rem 1.2rem;
    border-left: 4px solid transparent;
    transition: background 0.2s;
    align-items: flex-start;
}

.notif-item:hover {
    background: #f8fafc;
}

.notif-item.critique {
    border-left-color: #dc2626;
    background: #fef2f2;
}

.notif-item.critique:hover {
    background: #fde8e8;
}

.notif-item .notif-icon {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    font-size: 0.9rem;
    background: var(--bg-color);
    color: var(--text-muted-custom);
}

.notif-item.critique .notif-icon {
    background: #fecaca;
    color: #dc2626;
}

.notif-item .notif-content {
    flex: 1;
    min-width: 0;
}

.notif-item .notif-title {
    font-weight: 600;
    font-size: 0.9rem;
    color: var(--text-color);
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex-wrap: wrap;
}

.notif-item .notif-desc {
    font-size: 0.82rem;
    color: var(--text-muted-custom);
    margin-top: 0.1rem;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.notif-item .notif-meta {
    display: flex;
    align-items: center;
    gap: 0.8rem;
    margin-top: 0.3rem;
    flex-wrap: wrap;
}

.notif-item .notif-time {
    font-size: 0.7rem;
    color: #94a3b8;
}

.badge-urgence {
    font-size: 0.6rem;
    font-weight: 600;
    padding: 0.15rem 0.6rem;
    border-radius: 20px;
    text-transform: uppercase;
    letter-spacing: 0.4px;
}

.badge-urgence.critique {
    background: #fecaca;
    color: #b91c1c;
}

.badge-urgence.elevee {
    background: #fed7aa;
    color: #c2410c;
}

.badge-urgence.moyenne {
    background: #bfdbfe;
    color: #1e40af;
}

.badge-urgence.faible {
    background: #e2e8f0;
    color: #475569;
}

.notif-dropdown-footer {
    padding: 0.7rem 1.2rem 1rem 1.2rem;
    border-top: 1px solid var(--border-color);
    text-align: center;
}

.notif-dropdown-footer a {
    color: var(--primary-color);
    text-decoration: none;
    font-weight: 600;
    font-size: 0.9rem;
    transition: color 0.2s;
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
}

.notif-dropdown-footer a:hover {
    color: var(--secondary-color);
    text-decoration: underline;
}

/* ============================================
   USER MENU
   ============================================ */

.user-menu {
    position: relative;
}

.user-menu-toggle {
    border: none;
    background: transparent;
    padding: 0;
    margin: 0;
    cursor: pointer;
    display: flex;
    align-items: center;
}

.user-menu-toggle:focus {
    outline: none;
}

.user-menu-toggle:hover .user-avatar {
    background-color: rgba(255, 255, 255, 0.3);
}

.user-menu-dropdown {
    position: absolute;
    right: 0;
    top: calc(100% + 10px);

    width: 230px;

    background: var(--card-bg);
    border: 1px solid var(--border-color);
    border-radius: 8px;

    padding: 0.5rem 0;

    box-shadow: var(--shadow-md);

    z-index: 3000;

    animation: userMenuAppear 0.15s ease;
}

@keyframes userMenuAppear {
    from {
        opacity: 0;
        transform: translateY(-5px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.user-menu-header {
    padding: 0.6rem 1rem;
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.user-menu-name {
    color: var(--text-color);
    font-size: 0.9rem;
    font-weight: 600;
}

.user-menu-firstname {
    color: var(--text-color);
    font-size: 0.85rem;
    font-weight: 400;
}

.user-menu-role {
    color: var(--text-muted-custom);
    font-size: 0.75rem;
    margin-top: 2px;
}

.user-menu-item {
    width: 100%;

    display: flex;
    align-items: center;

    gap: 0.75rem;

    padding: 0.6rem 1rem;

    background: transparent;
    border: none;

    color: var(--text-color);

    text-decoration: none;

    font-size: 0.875rem;

    text-align: left;

    cursor: pointer;

    transition: background-color 0.15s ease;
}

.user-menu-item:hover {
    background: var(--bg-color);
}

.user-menu-item i {
    width: 1.2rem;
    text-align: center;
}

.user-menu-item.logout {
    color: #dc2626;
}

.user-menu-item.logout:hover {
    background: #fef2f2;
}

.dropdown-divider {
    border: none;
    border-top: 1px solid var(--border-color);
    margin: 0.25rem 0;
}

/* --- Responsive --- */
@media (max-width: 991.98px) {
    .nav-title {
        display: none !important;
    }

    .search-bar input {
        width: 100px;
    }
}

@media (max-width: 700px) {
    .notif-dropdown {
        width: 340px;
        right: -20px;
    }
}

@media (max-width: 480px) {
    .notif-dropdown {
        width: calc(100vw - 2rem);
        right: -1rem;
        top: calc(100% + 8px);
    }
}

@media (max-width: 575.98px) {
    .top-nav {
        padding: 0 12px;
    }

    .search-bar {
        display: none;
    }
}
</style>
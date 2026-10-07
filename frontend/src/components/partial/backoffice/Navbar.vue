<template>
    <nav class="back-nav">
        <div class="back-nav__left">
            <button class="back-nav__icon-btn d-lg-none" @click="toggleSidebar" type="button"
                aria-label="Ouvrir le menu">
                <i class="bi bi-list"></i>
            </button>

            <div class="back-nav__brand">
                <span class="back-nav__brand-mark" @click="toggleSidebar">
                    <i class="bi bi-shield-check"></i>
                </span>
                <span class="back-nav__brand-text">
                    <strong>DTS</strong>
                    <small>Administration</small>
                </span>
            </div>
        </div>

        <div class="back-nav__right">

            <!-- Notifications -->
            <div class="back-nav__notif-wrapper">
                <button class="back-nav__icon-btn" @click="toggleNotifications" aria-label="Notifications"
                    :aria-expanded="isNotifOpen" ref="notifBtnRef">
                    <i class="bi bi-bell"></i>
                    <span class="back-nav__notif-dot" v-if="unreadCount > 0">
                        {{ unreadCount }}
                    </span>
                </button>

                <div class="back-nav__notif-dropdown" :class="{ open: isNotifOpen }" ref="notifDropdownRef" role="menu">
                    <div class="back-nav__notif-header">
                        <strong>Notifications</strong>
                        <span class="back-nav__notif-count">{{ unreadCount }} non lues</span>
                    </div>

                    <div class="back-nav__notif-list">
                        <div v-for="notif in notifications" :key="notif.id" class="back-nav__notif-item"
                            :class="notif.type">
                            <div class="back-nav__notif-icon">
                                <i :class="notif.icon"></i>
                            </div>
                            <div class="back-nav__notif-content">
                                <div class="back-nav__notif-title">
                                    {{ notif.title }}
                                    <span class="back-nav__badge-urgence" :class="notif.urgence" v-if="notif.urgence">
                                        {{ notif.urgence }}
                                    </span>
                                </div>
                                <div class="back-nav__notif-desc">{{ notif.description }}</div>
                                <div class="back-nav__notif-meta">
                                    <span class="back-nav__notif-time">
                                        <i class="bi bi-clock"></i> {{ notif.time }}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="back-nav__notif-footer">
                        <router-link to="/notifications">
                            <i class="bi bi-arrow-right"></i> Voir toutes les notifications
                        </router-link>
                    </div>
                </div>
            </div>

            <span class="back-nav__sep d-none d-md-block"></span>

            <!-- Menu utilisateur -->
            <div class="back-nav__user-menu" ref="userMenuRef">
                <button type="button" class="back-nav__user-toggle" @click="toggleUserMenu"
                    :aria-expanded="isUserMenuOpen" aria-label="Menu utilisateur">
                    <span class="back-nav__user-avatar">
                        {{ userInitials }}
                    </span>
                    <span class="back-nav__user-meta d-none d-sm-flex">
                        <strong>{{ userFullName }}</strong>
                        <small>{{ userRole }}</small>
                    </span>
                    <i class="bi bi-chevron-down back-nav__user-caret"></i>
                </button>

                <div v-if="isUserMenuOpen" class="back-nav__user-dropdown">
                    <div class="back-nav__user-header">
                        <strong>{{ userFullName }}</strong>
                        <small>{{ userRole }}</small>
                    </div>

                    <hr class="back-nav__divider">

                    <RouterLink to="/profile" class="back-nav__user-item" @click="isUserMenuOpen = false">
                        <i class="bi bi-person"></i>
                        <span>Mon profil</span>
                    </RouterLink>

                    <RouterLink to="/settings" class="back-nav__user-item" @click="isUserMenuOpen = false">
                        <i class="bi bi-gear"></i>
                        <span>Paramètres</span>
                    </RouterLink>

                    <hr class="back-nav__divider">

                    <button type="button" class="back-nav__user-item back-nav__user-item--logout" @click="handleLogout">
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

// --- Store ---
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

// --- Emits ---
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

// --- Computed ---
const userRole = computed(() => {
    return authStore.user?.poste ?? 'Non défini'
})

const userFullName = computed(() => {
    const nom = authStore.user?.nom ?? 'Utilisateur'
    const prenom = authStore.user?.prenom ?? ''
    return prenom ? `${prenom} ${nom}` : nom
})

const userInitials = computed(() => {
    if (!authStore.user) return 'U'
    return `${authStore.user.prenom.charAt(0)}${authStore.user.nom.charAt(0)}`.toUpperCase()
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

const handleLogout = async () => {
    try {
        isUserMenuOpen.value = false
        await authStore.logout()
        await router.push('/login')
    } catch (error) {
        console.error('Erreur lors de la déconnexion :', error)
    }
}

// --- Click outside handlers ---
const handleClickOutside = (event: MouseEvent) => {
    const target = event.target as HTMLElement

    if (isNotifOpen.value && notifBtnRef.value && notifDropdownRef.value) {
        if (!notifBtnRef.value.contains(target) && !notifDropdownRef.value.contains(target)) {
            isNotifOpen.value = false
        }
    }

    if (isUserMenuOpen.value && userMenuRef.value && !userMenuRef.value.contains(target)) {
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
   BACKOFFICE NAVBAR — Design DTS-TSS
   ============================================ */

.back-nav {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    height: var(--dts-navbar-h);
    z-index: 1050;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    padding: 0 1rem 0 0.85rem;
    background: var(--dts-surface);
    border-bottom: 1px solid var(--dts-border);
    box-shadow: var(--dts-shadow-xs);
    color: var(--dts-text);
}

.back-nav__left,
.back-nav__right {
    display: flex;
    align-items: center;
    gap: 0.35rem;
}

.back-nav__left {
    min-width: 0;
}

.back-nav__sep {
    width: 1px;
    height: 26px;
    background: var(--dts-border);
    margin: 0 0.5rem;
}

/* --- Boutons icônes --- */
.back-nav__icon-btn {
    position: relative;
    width: 36px;
    height: 36px;
    flex: 0 0 36px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: 1px solid transparent;
    border-radius: var(--dts-radius-sm);
    background: transparent;
    color: var(--dts-text-2);
    font-size: 1.02rem;
    cursor: pointer;
    transition: background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.back-nav__icon-btn:hover {
    background: var(--dts-blue-50);
    color: var(--dts-blue);
    border-color: var(--dts-border);
}

.back-nav__icon-btn:focus-visible {
    outline: 2px solid var(--dts-blue);
    outline-offset: 1px;
}

/* --- Marque --- */
.back-nav__brand {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    margin-left: 0.3rem;
    min-width: 0;
}

.back-nav__brand-mark {
    width: 34px;
    height: 34px;
    flex: 0 0 34px;
    border-radius: 9px;
    display: grid;
    place-items: center;
    color: #fff;
    font-size: 1.05rem;
    background: linear-gradient(145deg, var(--dts-navy) 0%, var(--dts-blue) 115%);
    box-shadow: 0 2px 6px rgba(13, 43, 78, 0.28);
}

.back-nav__brand-text {
    display: flex;
    flex-direction: column;
    line-height: 1.12;
    min-width: 0;
}

.back-nav__brand-text strong {
    font-size: 1rem;
    color: var(--dts-navy);
    letter-spacing: 0.02em;
}

.back-nav__brand-text small {
    font-size: 0.69rem;
    color: var(--dts-muted);
    white-space: nowrap;
}

/* --- Recherche --- */
.back-nav__search {
    display: flex;
    align-items: center;
    gap: 0.45rem;
    min-height: 36px;
    padding: 0 0.65rem;
    background: var(--dts-bg);
    border: 1px solid var(--dts-border-2);
    border-radius: var(--dts-radius-sm);
    color: var(--dts-muted);
    transition: background-color 0.15s ease, border-color 0.15s ease;
}

.back-nav__search:focus-within {
    background: var(--dts-surface);
    border-color: #9dc2e0;
    box-shadow: 0 0 0 3px rgba(31, 111, 178, 0.12);
}

.back-nav__search i {
    font-size: 0.85rem;
    flex-shrink: 0;
}

.back-nav__search input {
    width: 160px;
    background: transparent;
    border: none;
    outline: none;
    color: var(--dts-text);
    font-size: 0.815rem;
}

.back-nav__search input::placeholder {
    color: #9fb0c2;
}

/* --- Pastille notifications --- */
.back-nav__notif-dot {
    position: absolute;
    top: 2px;
    right: 1px;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    border-radius: 20px;
    background: var(--dts-danger);
    color: #fff;
    font-size: 0.63rem;
    font-weight: 700;
    line-height: 16px;
    text-align: center;
    border: 2px solid var(--dts-surface);
    box-sizing: content-box;
}

/* --- Menu notifications --- */
.back-nav__notif-wrapper {
    position: relative;
}

.back-nav__notif-dropdown {
    position: absolute;
    right: 0;
    top: calc(100% + 12px);
    width: 340px;
    max-width: 90vw;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    box-shadow: var(--dts-shadow-md);
    overflow: hidden;
    z-index: 1060;
    opacity: 0;
    visibility: hidden;
    transform: translateY(-8px) scale(0.97);
    transform-origin: top right;
    transition: opacity 0.25s ease, transform 0.25s ease, visibility 0.25s;
}

.back-nav__notif-dropdown.open {
    opacity: 1;
    visibility: visible;
    transform: translateY(0) scale(1);
}

.back-nav__notif-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0.7rem 0.9rem;
    border-bottom: 1px solid var(--dts-border);
    font-size: 0.82rem;
}

.back-nav__notif-header strong {
    font-weight: 600;
    color: var(--dts-text);
}

.back-nav__notif-count {
    font-size: 0.74rem;
    color: var(--dts-blue-600);
    background: var(--dts-blue-100);
    border: 1px solid #cfe0ee;
    padding: 0.12rem 0.5rem;
    border-radius: 20px;
    font-weight: 600;
}

.back-nav__notif-list {
    max-height: 380px;
    overflow-y: auto;
}

.back-nav__notif-list::-webkit-scrollbar {
    width: 9px;
}

.back-nav__notif-list::-webkit-scrollbar-thumb {
    background: #c9d4e0;
    border-radius: 20px;
    border: 2px solid transparent;
    background-clip: content-box;
}

.back-nav__notif-item {
    display: flex;
    gap: 0.7rem;
    align-items: flex-start;
    padding: 0.65rem 0.9rem;
    border-bottom: 1px solid #f1f4f8;
    transition: background-color 0.15s ease;
}

.back-nav__notif-item:hover {
    background: var(--dts-blue-50);
}

.back-nav__notif-item.critique {
    background: #fdecea;
}

.back-nav__notif-icon {
    width: 30px;
    height: 30px;
    flex: 0 0 30px;
    border-radius: 8px;
    display: grid;
    place-items: center;
    font-size: 0.85rem;
    background: var(--dts-blue-100);
    color: var(--dts-blue);
}

.back-nav__notif-item.critique .back-nav__notif-icon {
    background: #fdecea;
    color: var(--dts-danger);
}

.back-nav__notif-item.normal .back-nav__notif-icon {
    background: var(--dts-blue-100);
    color: var(--dts-info);
}

.back-nav__notif-content {
    flex: 1;
    min-width: 0;
}

.back-nav__notif-title {
    display: flex;
    align-items: center;
    gap: 0.35rem;
    flex-wrap: wrap;
    font-size: 0.8rem;
    font-weight: 600;
    color: var(--dts-text);
}

.back-nav__notif-desc {
    font-size: 0.72rem;
    color: var(--dts-muted);
    margin-top: 0.1rem;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.back-nav__notif-meta {
    display: flex;
    align-items: center;
    gap: 0.8rem;
    margin-top: 0.25rem;
    flex-wrap: wrap;
}

.back-nav__notif-time {
    font-size: 0.7rem;
    color: var(--dts-muted);
}

.back-nav__badge-urgence {
    font-size: 0.6rem;
    font-weight: 600;
    padding: 0.15rem 0.6rem;
    border-radius: 20px;
    text-transform: uppercase;
    letter-spacing: 0.4px;
}

.back-nav__badge-urgence.critique {
    background: #fdecea;
    color: #a61b16;
    border: 1px solid #f6cfcc;
}

.back-nav__badge-urgence.elevee {
    background: #fdf3e2;
    color: #9a6409;
    border: 1px solid #f4e2c2;
}

.back-nav__badge-urgence.moyenne {
    background: #e4f0fb;
    color: #135d95;
    border: 1px solid #cde2f6;
}

.back-nav__badge-urgence.faible {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #e0e6ec;
}

.back-nav__notif-footer {
    padding: 0.5rem;
    border-top: 1px solid var(--dts-border);
    text-align: center;
}

.back-nav__notif-footer a {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    width: 100%;
    justify-content: center;
    padding: 0.35rem;
    border: 1px solid var(--dts-border-2);
    border-radius: var(--dts-radius-sm);
    background: var(--dts-surface);
    color: var(--dts-text-2);
    font-size: 0.78rem;
    font-weight: 600;
    text-decoration: none;
    transition: background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.back-nav__notif-footer a:hover {
    background: var(--dts-blue-50);
    color: var(--dts-blue);
    border-color: #bdd3e6;
}

/* --- Bouton utilisateur --- */
.back-nav__user-menu {
    position: relative;
}

.back-nav__user-toggle {
    display: flex;
    align-items: center;
    gap: 0.55rem;
    padding: 0.3rem 0.5rem 0.3rem 0.3rem;
    border: 1px solid transparent;
    border-radius: 30px;
    background: transparent;
    cursor: pointer;
    transition: background-color 0.15s ease, border-color 0.15s ease;
}

.back-nav__user-toggle:hover {
    background: var(--dts-blue-50);
    border-color: var(--dts-border);
}

.back-nav__user-avatar {
    width: 34px;
    height: 34px;
    flex: 0 0 34px;
    border-radius: 50%;
    display: grid;
    place-items: center;
    background: linear-gradient(145deg, #26527f, var(--dts-navy-2));
    color: #fff;
    font-size: 0.76rem;
    font-weight: 700;
    letter-spacing: 0.03em;
}

.back-nav__user-meta {
    flex-direction: column;
    line-height: 1.16;
    text-align: left;
}

.back-nav__user-meta strong {
    font-size: 0.805rem;
    color: var(--dts-text);
}

.back-nav__user-meta small {
    font-size: 0.69rem;
    color: var(--dts-muted);
}

.back-nav__user-caret {
    font-size: 0.7rem;
    color: var(--dts-muted);
}

/* --- Dropdown utilisateur --- */
.back-nav__user-dropdown {
    position: absolute;
    right: 0;
    top: calc(100% + 10px);
    width: 240px;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    padding: 0.3rem;
    box-shadow: var(--dts-shadow-md);
    z-index: 3000;
    animation: dtsUserMenuAppear 0.15s ease;
}

@keyframes dtsUserMenuAppear {
    from {
        opacity: 0;
        transform: translateY(-5px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.back-nav__user-header {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: 0.6rem 0.65rem;
}

.back-nav__user-header strong {
    font-size: 0.82rem;
    font-weight: 600;
    color: var(--dts-text);
}

.back-nav__user-header small {
    font-size: 0.75rem;
    color: var(--dts-muted);
}

.back-nav__user-item {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 0.55rem;
    padding: 0.45rem 0.6rem;
    background: transparent;
    border: none;
    border-radius: 6px;
    color: var(--dts-text);
    font-size: 0.82rem;
    text-align: left;
    text-decoration: none;
    cursor: pointer;
    transition: background-color 0.15s ease;
}

.back-nav__user-item:hover {
    background: var(--dts-blue-50);
}

.back-nav__user-item i {
    width: 15px;
    text-align: center;
    color: var(--dts-muted);
}

.back-nav__user-item--logout {
    color: var(--dts-danger);
}

.back-nav__user-item--logout i {
    color: currentColor;
}

.back-nav__user-item--logout:hover {
    background: #fdecea;
}

.back-nav__divider {
    border: none;
    border-top: 1px solid var(--dts-border);
    margin: 0.25rem 0;
}

/* --- Responsive --- */
@media (max-width: 991.98px) {
    .back-nav__search input {
        width: 120px;
    }
}

@media (max-width: 767.98px) {
    .back-nav__brand-text small {
        display: none;
    }
}

@media (max-width: 575.98px) {
    .back-nav {
        padding: 0 0.65rem;
    }

    .back-nav__search {
        display: none;
    }

    .back-nav__notif-dropdown {
        width: min(340px, calc(100vw - 2rem));
        right: -1rem;
        top: calc(100% + 8px);
    }
}
</style>

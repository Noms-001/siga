
<template>
    <nav class="top-nav" :class="{ collapsed }">
        <!-- Gauche -->
        <div class="nav-left">
            <button
                class="mobile-menu"
                @click="emit('toggle-sidebar')"
                aria-label="Ouvrir le menu"
            >
                <i class="bi bi-list"></i>
            </button>

            <nav class="breadcrumbs" aria-label="Fil d’Ariane">
                <template
                    v-for="(label, index) in breadcrumbs"
                    :key="`${label}-${index}`"
                >
                    <span
                        v-if="index > 0"
                        class="breadcrumbs__separator"
                        aria-hidden="true"
                    >/</span>

                    <strong
                        v-if="index === breadcrumbs.length - 1"
                        aria-current="page"
                    >
                        {{ label }}
                    </strong>

                    <span v-else>{{ label }}</span>
                </template>
            </nav>
        </div>

        <!-- Droite -->
        <div class="nav-right">
            <!-- Identité institutionnelle BFM -->
            <div class="bank-brand" aria-label="Banky Foiben’i Madagasikara">
                <img
                    class="bank-brand-logo"
                    src="/logo/bfm-logo.png"
                    alt="Logo de Banky Foiben’i Madagasikara"
                />

                <div class="bank-brand-copy">
                    <span class="bank-logo">
                        Banky Foiben’i Madagasikara
                    </span>
                    <span class="bank-subtitle">
                        Banque centrale de Madagascar
                    </span>
                </div>
            </div>

            <span class="separator d-none d-md-block"></span>

            <!-- Notifications -->
            <div class="notif-wrapper">
                <button
                    class="icon-button"
                    :class="{ 'has-unread': unreadCount > 0 }"
                    @click="toggleNotifications"
                    aria-label="Notifications"
                    ref="notifBtnRef"
                >
                    <i
                        class="bi bi-bell"
                        :class="{ ringing: isRinging }"
                    ></i>

                    <span
                        v-if="unreadCount > 0"
                        class="notif-badge"
                        :key="unreadCount"
                    >
                        {{ unreadCount > 99 ? '99+' : unreadCount }}
                    </span>
                </button>

                <div
                    class="notif-dropdown"
                    :class="{ open: isNotifOpen }"
                    ref="notifDropdownRef"
                    role="menu"
                >
                    <div class="notif-dropdown-header">
                        <h4>Notifications</h4>
                        <span>{{ unreadCount }} non lues</span>
                    </div>

                    <div class="notif-list">
                        <div
                            v-for="notif in notifications.slice(0, 6)"
                            :key="notif.id"
                            class="notif-item"
                            :class="{ unread: !notif.dateLecture }"
                            @click="ouvrirNotification(notif.id, notif.idActivite)"
                        >
                            <div class="notif-icon">
                                <i class="bi bi-bell"></i>
                            </div>

                            <div class="notif-content">
                                <div class="notif-title">
                                    {{ notif.titre }}

                                    <span
                                        class="badge-urgence"
                                        v-if="!notif.dateLecture"
                                    >
                                        Non lue
                                    </span>
                                </div>

                                <div class="notif-desc">
                                    {{ notif.message }}
                                </div>

                                <div
                                    class="notif-desc"
                                    v-if="notif.activite"
                                >
                                    Activité : {{ notif.activite }}
                                </div>

                                <div class="notif-meta">
                                    <span class="notif-time">
                                        <i class="bi bi-clock"></i>
                                        {{ new Date(notif.dateCreation).toLocaleString() }}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="notif-dropdown-footer">
                        <router-link to="/notifications">
                            <i class="bi bi-arrow-right"></i>
                            Voir toutes les notifications
                        </router-link>
                    </div>
                </div>
            </div>

            <!-- Menu utilisateur -->
            <div class="user-menu" ref="userMenuRef">
                <button
                    type="button"
                    class="top-avatar"
                    @click="toggleUserMenu"
                    :aria-expanded="isUserMenuOpen"
                    aria-label="Menu utilisateur"
                >
                    {{ userInitials }}
                </button>

                <div
                    v-if="isUserMenuOpen"
                    class="user-menu-dropdown"
                >
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

                    <hr class="dropdown-divider" />

                    <RouterLink
                        to="/profile"
                        class="user-menu-item"
                        @click="isUserMenuOpen = false"
                    >
                        <i class="bi bi-person"></i>
                        <span>Mon profil</span>
                    </RouterLink>

                    <RouterLink
                        to="/settings"
                        class="user-menu-item"
                        @click="isUserMenuOpen = false"
                    >
                        <i class="bi bi-gear"></i>
                        <span>Paramètres</span>
                    </RouterLink>

                    <hr class="dropdown-divider" />

                    <button
                        type="button"
                        class="user-menu-item logout"
                        @click="handleLogout"
                    >
                        <i class="bi bi-box-arrow-right"></i>
                        <span>Déconnexion</span>
                    </button>
                </div>
            </div>
        </div>
    </nav>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
    useNotifications,
    actualiserNotifications,
    marquerNotificationLue,
} from '@/services/notification'
import { getPageBreadcrumbs } from '@/utils/navigation'

const emit = defineEmits<{
    (e: 'toggle-sidebar'): void
}>()

const props = defineProps<{
    collapsed?: boolean
}>()

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isNotifOpen = ref(false)
const isUserMenuOpen = ref(false)

const notifBtnRef = ref<HTMLButtonElement | null>(null)
const notifDropdownRef = ref<HTMLDivElement | null>(null)
const userMenuRef = ref<HTMLDivElement | null>(null)

const userRole = computed(
    () => authStore.user?.poste ?? 'Utilisateur'
)

const userInitials = computed(() => {
    if (!authStore.user) return 'U'

    return `${authStore.user.prenom.charAt(0)}${authStore.user.nom.charAt(0)}`
        .toUpperCase()
})

const notificationState = useNotifications()

const notifications = computed(
    () => notificationState.notifications
)

const unreadCount = computed(
    () => notificationState.unreadCount
)

const isRinging = ref(false)

watch(unreadCount, (nv, ov) => {
    if (nv > ov) {
        isRinging.value = false

        requestAnimationFrame(() => {
            isRinging.value = true

            window.setTimeout(() => {
                isRinging.value = false
            }, 1200)
        })
    }
})

const breadcrumbs = computed(() => getPageBreadcrumbs(route))

const toggleNotifications = () => {
    isNotifOpen.value = !isNotifOpen.value

    if (isUserMenuOpen.value) {
        isUserMenuOpen.value = false
    }
}

const ouvrirNotification = async (
    id: number,
    idActivite: number | null
) => {
    await marquerNotificationLue(id)

    if (idActivite) {
        isNotifOpen.value = false
        await router.push(`/activites/${idActivite}`)
    }
}

const toggleUserMenu = () => {
    isUserMenuOpen.value = !isUserMenuOpen.value

    if (isNotifOpen.value) {
        isNotifOpen.value = false
    }
}

const handleLogout = async () => {
    try {
        isUserMenuOpen.value = false
        await authStore.logout()
        await router.push('/login')
    } catch (e) {
        console.error('Erreur lors de la déconnexion :', e)
    }
}

const handleClickOutside = (event: MouseEvent) => {
    const target = event.target as HTMLElement

    if (
        isNotifOpen.value &&
        notifBtnRef.value &&
        notifDropdownRef.value
    ) {
        if (
            !notifBtnRef.value.contains(target) &&
            !notifDropdownRef.value.contains(target)
        ) {
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

onMounted(() => {
    document.addEventListener('click', handleClickOutside)
    void actualiserNotifications()
})

onUnmounted(() => {
    document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
/* ============================================================
   TOPBAR
   ============================================================ */

.top-nav {
    position: fixed;
    top: 0;
    left: 248px;
    right: 0;
    height: 58px;
    z-index: 1040;

    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;

    padding: 0 38px;

    border-bottom: 1px solid #e6ebf3;
    backdrop-filter: blur(14px);
    -webkit-backdrop-filter: blur(14px);

    color: #15233c;
    font-family: "DM Sans", system-ui, sans-serif;

    transition: left 0.25s cubic-bezier(.4, 0, .2, 1);
}

.top-nav.collapsed {
    left: 76px;
}

.top-nav::before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: var(--brand-gradient);
}

/* ============================================================
   STRUCTURE
   ============================================================ */

.nav-left,
.nav-right {
    display: flex;
    align-items: center;
    gap: 14px;
}

.nav-left {
    min-width: 0;
    flex: 1;
}

.nav-right {
    flex-shrink: 0;
}

/* ============================================================
   MENU MOBILE
   ============================================================ */

.mobile-menu {
    display: none;
    flex-shrink: 0;

    padding: 7px 10px;
    border: 1px solid #e6ebf3;
    border-radius: 9px;

    background: #fff;
    color: #1a2a6c;
    font-size: 16px;
    cursor: pointer;
}

/* ============================================================
   FIL D'ARIANE
   ============================================================ */

.breadcrumbs {
    min-width: 0;
    font-size: 11.5px;
    color: #8a95a8;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.breadcrumbs > span:not(.breadcrumbs__separator) {
    color: #8a95a8;
}

.breadcrumbs .breadcrumbs__separator {
    padding: 0 10px;
    color: #c3cad5;
}

.breadcrumbs strong {
    font-weight: 700;
    color: var(--gold);
}

/* ============================================================
   IDENTITÉ INSTITUTIONNELLE BFM
   Remplace la barre de recherche
   ============================================================ */

.bank-brand {
    display: flex;
    align-items: center;
    gap: 10px;

    min-width: 0;
    max-width: 330px;
    padding: 0 4px;
}

.bank-brand-logo {
    display: block;
    width: 37px;
    height: 37px;
    flex: 0 0 37px;
    object-fit: contain;
}

.bank-brand-copy {
    display: flex;
    flex-direction: column;
    justify-content: center;
    min-width: 0;
    gap: 2px;
}

/* Même identité visuelle que .footer-brand-copy .bank-logo */
.bank-logo {
    color: #2577a7;
    text-transform: uppercase;
    font-weight: 900;
    font-size: 10.5px;
    line-height: 1.35;
    letter-spacing: 0.2px;
    white-space: normal;
}

.bank-subtitle {
    color: #7d899a;
    font-size: 10px;
    font-weight: 500;
    line-height: 1.3;
}

/* ============================================================
   NOTIFICATIONS
   ============================================================ */

.notif-wrapper {
    position: relative;
    flex-shrink: 0;
}

.icon-button {
    position: relative;
    width: 36px;
    height: 36px;

    display: grid;
    place-items: center;

    color: #6b7280;
    font-size: 17px;

    background: transparent;
    border: 1px solid transparent;
    border-radius: 10px;

    cursor: pointer;
    transition: background 0.18s, color 0.18s;
}

.icon-button:hover {
    background: #f3f6fc;
    color: #1a2a6c;
}

.icon-button.has-unread {
    color: #1a2a6c;
    background: rgba(74, 108, 247, 0.07);
}

.icon-button.has-unread:hover {
    background: rgba(74, 108, 247, 0.14);
}

.notif-badge {
    position: absolute;
    top: -3px;
    right: -3px;

    min-width: 20px;
    height: 20px;
    padding: 0 6px;

    border-radius: 10px;
    display: grid;
    place-items: center;

    color: #fff;
    font: 800 10.5px/1 "DM Sans", system-ui, sans-serif;
    letter-spacing: 0.2px;

    background: linear-gradient(135deg, #ff4d6a 0%, #c81e3e 100%);
    border: 2px solid #fff;

    box-shadow:
        0 2px 6px rgba(200, 30, 62, 0.45),
        0 0 0 0 rgba(255, 77, 106, 0.5);

    z-index: 1;
    pointer-events: none;

    animation: badgePop 0.4s cubic-bezier(.34, 1.56, .64, 1);
}

.notif-badge::before {
    content: "";
    position: absolute;
    inset: 0;
    border-radius: inherit;
    background: #ff4d6a;
    z-index: -1;
    animation: badgePulse 1.8s ease-out infinite;
}

@keyframes badgePop {
    0% {
        transform: scale(0) rotate(-20deg);
        opacity: 0;
    }

    60% {
        transform: scale(1.25) rotate(4deg);
        opacity: 1;
    }

    100% {
        transform: scale(1) rotate(0);
        opacity: 1;
    }
}

@keyframes badgePulse {
    0% {
        transform: scale(1);
        opacity: 0.75;
    }

    70%, 100% {
        transform: scale(2.4);
        opacity: 0;
    }
}

.bi-bell.ringing {
    display: inline-block;
    transform-origin: 50% 12%;
    animation: bellRing 1.2s cubic-bezier(.36, .07, .19, .97);
}

@keyframes bellRing {
    0%, 100% { transform: rotate(0); }
    10% { transform: rotate(18deg); }
    20% { transform: rotate(-16deg); }
    30% { transform: rotate(12deg); }
    40% { transform: rotate(-10deg); }
    50% { transform: rotate(7deg); }
    60% { transform: rotate(-5deg); }
    75% { transform: rotate(3deg); }
    90% { transform: rotate(-1deg); }
}

/* ============================================================
   LISTE DES NOTIFICATIONS
   ============================================================ */

.notif-dropdown {
    position: absolute;
    right: 0;
    top: calc(100% + 12px);

    width: 340px;
    max-width: 90vw;

    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;

    box-shadow:
        0 2px 4px rgba(26, 42, 108, 0.05),
        0 14px 34px rgba(26, 42, 108, 0.10);

    overflow: hidden;
    z-index: 1060;

    opacity: 0;
    visibility: hidden;
    transform: translateY(-8px) scale(0.97);
    transform-origin: top right;

    transition:
        opacity 0.25s ease,
        transform 0.25s ease,
        visibility 0.25s;
}

.notif-dropdown.open {
    opacity: 1;
    visibility: visible;
    transform: translateY(0) scale(1);
}

.notif-dropdown-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0.7rem 0.9rem;
    border-bottom: 1px solid #e6ebf3;
}

.notif-dropdown-header h4 {
    margin: 0;
    font: 700 0.82rem "Manrope", system-ui, sans-serif;
    color: #1a2a6c;
}

.notif-dropdown-header span {
    font-size: 0.72rem;
    color: #3b63d9;
    background: #eaf0ff;
    border: 1px solid #cfe0ee;
    padding: 0.12rem 0.5rem;
    border-radius: 20px;
    font-weight: 600;
}

.notif-list {
    max-height: 380px;
    overflow-y: auto;
}

.notif-list::-webkit-scrollbar {
    width: 9px;
}

.notif-list::-webkit-scrollbar-thumb {
    background: #c9d4e0;
    border-radius: 20px;
    border: 2px solid transparent;
    background-clip: content-box;
}

.notif-item {
    display: flex;
    gap: 0.7rem;
    align-items: flex-start;
    padding: 0.65rem 0.9rem;
    border-bottom: 1px solid #f1f4f8;
    cursor: pointer;
    transition: background-color 0.15s;
}

.notif-item:hover {
    background: #f3f6fc;
}

.notif-item.unread {
    background: #f2f8ff;
}

.notif-icon {
    width: 30px;
    height: 30px;
    flex: 0 0 30px;
    border-radius: 8px;

    display: grid;
    place-items: center;

    font-size: 0.85rem;
    background: #eaf0ff;
    color: #4a6cf7;
}

.notif-content {
    flex: 1;
    min-width: 0;
}

.notif-title {
    display: flex;
    align-items: center;
    gap: 0.35rem;
    flex-wrap: wrap;

    font-size: 0.8rem;
    font-weight: 600;
    color: #15233c;
}

.notif-desc {
    font-size: 0.72rem;
    color: #6b7280;
    margin-top: 0.1rem;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.notif-meta {
    display: flex;
    align-items: center;
    gap: 0.8rem;
    margin-top: 0.25rem;
    flex-wrap: wrap;
}

.notif-time {
    font-size: 0.7rem;
    color: #8a95a8;
}

.badge-urgence {
    font-size: 0.6rem;
    font-weight: 600;
    padding: 0.15rem 0.6rem;
    border-radius: 20px;
    background: #fff6dd;
    color: #b8860b;
    border: 1px solid #f4e2c2;
}

.notif-dropdown-footer {
    padding: 0.5rem;
    border-top: 1px solid #e6ebf3;
    text-align: center;
}

.notif-dropdown-footer a {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    width: 100%;
    justify-content: center;
    padding: 0.4rem;

    border: 1px solid #e6ebf3;
    border-radius: 10px;

    background: #fff;
    color: #6b7280;
    font-size: 0.78rem;
    font-weight: 600;
    text-decoration: none;

    transition: background 0.15s, color 0.15s, border-color 0.15s;
}

.notif-dropdown-footer a:hover {
    background: #f3f6fc;
    color: #4a6cf7;
    border-color: #bdd3e6;
}

/* ============================================================
   AVATAR
   ============================================================ */

.top-avatar {
    width: 32px;
    height: 32px;

    display: grid;
    place-items: center;

    border: 0;
    border-radius: 50%;

    color: #fff;
    font-size: 10px;
    font-weight: 700;

    background: linear-gradient(135deg, #1a2a6c, #4a6cf7);
    box-shadow:
        0 0 0 2px #fff,
        0 0 0 3.5px rgba(212, 175, 55, 0.7);

    cursor: pointer;
    transition: transform 0.15s;
}

.top-avatar:hover {
    transform: translateY(-1px);
}

/* ============================================================
   MENU UTILISATEUR
   ============================================================ */

.user-menu {
    position: relative;
    flex-shrink: 0;
}

.user-menu-dropdown {
    position: absolute;
    right: 0;
    top: calc(100% + 10px);
    width: 240px;

    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    padding: 0.3rem;

    box-shadow:
        0 2px 4px rgba(26, 42, 108, 0.05),
        0 14px 34px rgba(26, 42, 108, 0.10);

    z-index: 1060;
    animation: dtsUserAppear 0.15s ease;
}

@keyframes dtsUserAppear {
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
    display: flex;
    flex-direction: column;
    gap: 1px;
    padding: 0.6rem 0.65rem;
}

.user-menu-name {
    font-size: 0.82rem;
    font-weight: 700;
    color: #1a2a6c;
}

.user-menu-firstname {
    font-size: 0.75rem;
    color: #6b7280;
}

.user-menu-role {
    font-size: 0.72rem;
    color: #3b63d9;
    background: #eaf0ff;
    border: 1px solid #cfe0ee;
    align-self: flex-start;
    padding: 0.08rem 0.45rem;
    border-radius: 20px;
    margin-top: 0.25rem;
}

.dropdown-divider {
    border: none;
    border-top: 1px solid #e6ebf3;
    margin: 0.25rem 0;
}

.user-menu-item {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 0.55rem;

    padding: 0.5rem 0.6rem;

    background: transparent;
    border: none;
    border-radius: 8px;

    color: #15233c;
    font-size: 0.82rem;
    text-align: left;
    text-decoration: none;

    cursor: pointer;
    transition: background-color 0.15s;
}

.user-menu-item:hover {
    background: #f3f6fc;
}

.user-menu-item i {
    width: 15px;
    text-align: center;
    color: #8a95a8;
}

.user-menu-item.logout {
    color: #d84e5b;
}

.user-menu-item.logout i {
    color: currentColor;
}

.user-menu-item.logout:hover {
    background: #fff0f0;
}

.separator {
    width: 1px;
    height: 26px;
    background: var(--dts-border);
    margin: 0 0.5rem;
}

/* ============================================================
   RESPONSIVE
   ============================================================ */

@media (max-width: 991.98px) {
    .top-nav,
    .top-nav.collapsed {
        left: 0;
        padding: 0 24px;
    }

    .mobile-menu {
        display: inline-flex;
    }

    .bank-brand {
        max-width: 290px;
    }
}

@media (max-width: 600px) {
    .top-nav,
    .top-nav.collapsed {
        height: 60px;
        padding: 0 14px;
        gap: 0.5rem;
    }

    .nav-left,
    .nav-right {
        gap: 8px;
    }

    .breadcrumbs {
        font-size: 10.5px;
    }

    .breadcrumbs > span:not(.breadcrumbs__separator),
    .breadcrumbs .breadcrumbs__separator {
        display: none;
    }

    .breadcrumbs strong {
        display: inline;
    }

    .bank-brand {
        gap: 6px;
        max-width: 175px;
        padding: 0;
    }

    .bank-brand-logo {
        width: 29px;
        height: 29px;
        flex-basis: 29px;
    }

    .bank-logo {
        font-size: 8px;
        letter-spacing: 0;
        line-height: 1.2;
    }

    .bank-subtitle {
        font-size: 8px;
        line-height: 1.2;
    }

    .top-avatar {
        width: 30px;
        height: 30px;
    }

    .notif-dropdown {
        width: min(340px, calc(100vw - 2rem));
        right: -1rem;
        top: calc(100% + 8px);
    }
}

@media (max-width: 380px) {
    .bank-subtitle {
        display: none;
    }

    .bank-brand {
        max-width: 135px;
    }

    .bank-logo {
        font-size: 7.5px;
    }

    .top-nav,
    .top-nav.collapsed {
        padding: 0 10px;
    }
}
</style>

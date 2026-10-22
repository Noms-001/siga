<template>
    <aside class="sidebar" :class="{ open: isOpen, collapsed: isCollapsed }">
        <!-- Marque : clic = ferme (mobile) ou collapse (desktop) -->
        <router-link
            to="/dashboard"
            class="brand"
            aria-label="GESTIA accueil"
            :title="isCollapsed ? 'Déplier le menu' : 'Replier le menu'"
            @click.prevent="handleBrandClick"
        >
            <img src="/logo/logo.png" alt="" class="brand-icon">
            <span class="brand-word">
                <img src="/logo/titre.png" alt="GESTIA">
                <small>GESTION D’ACTIVITÉ</small>
            </span>
        </router-link>

        <!-- Zone de navigation scrollable -->
        <div class="sidebar-nav-scroll">
            <nav class="nav" aria-label="Navigation principale">

                <!-- Vue d'ensemble -->
                <router-link
                    to="/dashboard"
                    class="nav-item"
                    :class="{ active: isActive('/dashboard') }"
                    :title="isCollapsed ? 'Vue d’ensemble' : undefined"
                >
                    <span class="nav-icon">
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                            <rect x="3" y="3" width="7" height="7" rx="1.5"/>
                            <rect x="14" y="3" width="7" height="7" rx="1.5"/>
                            <rect x="14" y="14" width="7" height="7" rx="1.5"/>
                            <rect x="3" y="14" width="7" height="7" rx="1.5"/>
                        </svg>
                    </span>
                    <span class="nav-text">Vue d’ensemble</span>
                </router-link>

                <!-- ACTIVITÉS -->
                <button
                    type="button"
                    class="nav-item nav-toggle"
                    :class="{ 'has-active': isGroupActive('activites') }"
                    @click="toggle('activites')"
                    :aria-expanded="openSections.activites"
                    :title="isCollapsed ? 'Activités' : undefined"
                >
                    <span class="nav-icon">
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                            <circle cx="12" cy="12" r="9"/>
                            <polyline points="12 7 12 12 15 14"/>
                        </svg>
                    </span>
                    <span class="nav-text">Activités</span>
                    <svg class="chev" :class="{ open: openSections.activites }" viewBox="0 0 24 24" aria-hidden="true">
                        <polyline points="6 9 12 15 18 9"/>
                    </svg>
                </button>
                <div class="nav-sub" :class="{ open: openSections.activites }">
                    <div class="nav-sub-inner">
                        <router-link to="/activites" class="nav-sub-item"
                            :class="{ active: isActive('/activites') && route.path !== '/activites/nouvelle' }">
                            Liste des activités
                        </router-link>
                        <router-link to="/activites/nouvelle" class="nav-sub-item"
                            :class="{ active: isActive('/activites/nouvelle') }">
                            Nouvelle activité
                        </router-link>
                        <router-link to="/affectation" class="nav-sub-item"
                            :class="{ active: isActive('/affectation') }">
                            Affectation
                        </router-link>
                        <router-link to="/activites/brouillons" class="nav-sub-item"
                            :class="{ active: isActive('/activites/brouillons') }">
                            Activités créées
                        </router-link>
                        <router-link to="/activites/a-valider" class="nav-sub-item"
                            :class="{ active: isActive('/activites/a-valider') }">
                            Activités à valider
                        </router-link>
                        <router-link to="/activites/suivi" class="nav-sub-item"
                            :class="{ active: isActive('/activites/suivi') }">
                            Suivi des activités
                        </router-link>
                    </div>
                </div>

                <!-- PLANS D'ACTION -->
                <button
                    type="button"
                    class="nav-item nav-toggle"
                    :class="{ 'has-active': isGroupActive('plans') }"
                    @click="toggle('plans')"
                    :aria-expanded="openSections.plans"
                    :title="isCollapsed ? 'Plans d’action' : undefined"
                >
                    <span class="nav-icon">
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                        </svg>
                    </span>
                    <span class="nav-text">Plans d’action</span>
                    <svg class="chev" :class="{ open: openSections.plans }" viewBox="0 0 24 24" aria-hidden="true">
                        <polyline points="6 9 12 15 18 9"/>
                    </svg>
                </button>
                <div class="nav-sub" :class="{ open: openSections.plans }">
                    <div class="nav-sub-inner">
                        <router-link to="/plan-action" class="nav-sub-item"
                            :class="{ active: isActive('/plan-action') }">
                            Liste des plans d’action
                        </router-link>
                        <router-link to="/plan-action/nouveau" class="nav-sub-item"
                            :class="{ active: isActive('/plan-action/nouveau') }">
                            Nouveau plan d’action
                        </router-link>
                        <router-link to="/suivi-plan-action" class="nav-sub-item"
                            :class="{ active: isActive('/suivi-plan-action') }">
                            Suivi des plans d’action
                        </router-link>
                    </div>
                </div>

                <!-- SIGNALEMENTS -->
                <button
                    type="button"
                    class="nav-item nav-toggle"
                    :class="{ 'has-active': isGroupActive('signalements') }"
                    @click="toggle('signalements')"
                    :aria-expanded="openSections.signalements"
                    :title="isCollapsed ? 'Signalements' : undefined"
                >
                    <span class="nav-icon">
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                            <line x1="12" y1="9" x2="12" y2="13"/>
                            <line x1="12" y1="17" x2="12.01" y2="17"/>
                        </svg>
                    </span>
                    <span class="nav-text">Signalements</span>
                    <svg class="chev" :class="{ open: openSections.signalements }" viewBox="0 0 24 24" aria-hidden="true">
                        <polyline points="6 9 12 15 18 9"/>
                    </svg>
                </button>
                <div class="nav-sub" :class="{ open: openSections.signalements }">
                    <div class="nav-sub-inner">
                        <router-link to="/signalements" class="nav-sub-item"
                            :class="{ active: isActive('/signalements') && route.path !== '/signalements/nouveau' }">
                            Liste des signalements
                        </router-link>
                        <router-link to="/signalements/nouveau" class="nav-sub-item"
                            :class="{ active: isActive('/signalements/nouveau') }">
                            Nouveau signalement
                        </router-link>
                    </div>
                </div>

                <!-- INDICATEURS -->
                <button
                    type="button"
                    class="nav-item nav-toggle"
                    :class="{ 'has-active': isGroupActive('indicateurs') }"
                    @click="toggle('indicateurs')"
                    :aria-expanded="openSections.indicateurs"
                    :title="isCollapsed ? 'Indicateurs' : undefined"
                >
                    <span class="nav-icon">
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M18 20V10M12 20V4M6 20v-6"/>
                        </svg>
                    </span>
                    <span class="nav-text">Indicateurs</span>
                    <svg class="chev" :class="{ open: openSections.indicateurs }" viewBox="0 0 24 24" aria-hidden="true">
                        <polyline points="6 9 12 15 18 9"/>
                    </svg>
                </button>
                <div class="nav-sub" :class="{ open: openSections.indicateurs }">
                    <div class="nav-sub-inner">
                        <router-link to="/indicateurs" class="nav-sub-item"
                            :class="{ active: isActive('/indicateurs') }">
                            Liste des indicateurs
                        </router-link>
                        <router-link to="/suivi-indicateurs" class="nav-sub-item"
                            :class="{ active: isActive('/suivi-indicateurs') }">
                            Suivi des indicateurs
                        </router-link>
                    </div>
                </div>

                <!-- Utilisateurs -->
                <router-link to="/utilisateurs" class="nav-item"
                    :class="{ active: isActive('/utilisateurs') }"
                    :title="isCollapsed ? 'Utilisateurs' : undefined">
                    <span class="nav-icon">
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M17 21v-2a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v2"/>
                            <circle cx="10" cy="7" r="4"/>
                            <path d="M21 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>
                        </svg>
                    </span>
                    <span class="nav-text">Utilisateurs</span>
                </router-link>

                <!-- Notifications -->
                <router-link to="/notifications" class="nav-item"
                    :class="{ active: isActive('/notifications') }"
                    :title="isCollapsed ? 'Notifications' : undefined">
                    <span class="nav-icon">
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0"/>
                        </svg>
                    </span>
                    <span class="nav-text">Notifications</span>
                    <span class="nav-count" v-if="notificationCount > 0">{{ notificationCount }}</span>
                </router-link>

                <!-- Mon profil -->
                <button
                    type="button"
                    class="nav-item nav-toggle"
                    :class="{ 'has-active': isGroupActive('profil') }"
                    @click="toggle('profil')"
                    :aria-expanded="openSections.profil"
                    :title="isCollapsed ? 'Mon profil' : undefined"
                >
                    <span class="nav-icon">
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                            <circle cx="12" cy="7" r="4"/>
                        </svg>
                    </span>
                    <span class="nav-text">Mon profil</span>
                    <svg class="chev" :class="{ open: openSections.profil }" viewBox="0 0 24 24" aria-hidden="true">
                        <polyline points="6 9 12 15 18 9"/>
                    </svg>
                </button>
                <div class="nav-sub" :class="{ open: openSections.profil }">
                    <div class="nav-sub-inner">
                        <router-link to="/profile" class="nav-sub-item"
                            :class="{ active: isActive('/profile') && route.path !== '/profile/change-password' }">
                            Profil
                        </router-link>
                        <router-link to="/profile/change-password" class="nav-sub-item"
                            :class="{ active: isActive('/profile/change-password') }">
                            Modifier mon mot de passe
                        </router-link>
                    </div>
                </div>
            </nav>
        </div>

        <!-- Zone fixe : aide + profil -->
        <div class="sidebar-bottom">
            <div class="help-card">
                <div class="help-symbol">?</div>
                <strong>Besoin d’aide ?</strong>
                <p>Retrouvez les conseils pour mieux organiser vos activités.</p>
                <a href="#help">Centre d’aide <span>↗</span></a>
            </div>

            <router-link to="/profile" class="profile"
                :title="isCollapsed ? `${userFullName} — ${userRole}` : undefined">
                <span class="avatar">{{ userInitials }}</span>
                <span class="profile-copy">
                    <strong>{{ userFullName }}</strong>
                    <small>{{ userRole }}</small>
                </span>
                <span class="dots">•••</span>
            </router-link>
        </div>
    </aside>

    <div class="sidebar-overlay" :class="{ show: isOpen }" @click="closeSidebar"></div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
    isOpen?: boolean
    notificationCount?: number
}>()

const emit = defineEmits<{
    (e: 'close'): void
    (e: 'collapse', collapsed: boolean): void
}>()

const route = useRoute()
const authStore = useAuthStore()

const isActive = (path: string) => route.path === path
const notificationCount = computed(() => props.notificationCount ?? 0)

/* ---- État collapsed (desktop uniquement) ---- */
const isCollapsed = ref(false)

const handleBrandClick = () => {
    // Mobile : fermer la sidebar (slide-out)
    if (window.innerWidth < 992) {
        closeSidebar()
        return
    }
    // Desktop : basculer collapsed / expanded
    isCollapsed.value = !isCollapsed.value
    emit('collapse', isCollapsed.value)
}

/* ---- Accordéons ---- */
type SectionKey = 'activites' | 'plans' | 'signalements' | 'indicateurs' | 'profil'

const openSections = reactive<Record<SectionKey, boolean>>({
    activites: false,
    plans: false,
    signalements: false,
    indicateurs: false,
    profil: false,
})

const toggle = (key: SectionKey) => {
    // Si la sidebar est repliée (desktop), un clic sur un groupe l'ouvre
    // et déplie la sidebar dans la foulée : plus intuitif qu'un sous-menu
    // qui apparaîtrait sans le libellé.
    if (isCollapsed.value) {
        isCollapsed.value = false
        emit('collapse', false)
        openSections[key] = true
        return
    }
    openSections[key] = !openSections[key]
}

const getActiveGroup = (path: string): SectionKey | null => {
    if (path.startsWith('/activites') || path.startsWith('/affectation')) return 'activites'
    if (path.startsWith('/plan-action') || path.startsWith('/suivi-plan-action')) return 'plans'
    if (path.startsWith('/signalements')) return 'signalements'
    if (path.startsWith('/indicateurs') || path.startsWith('/suivi-indicateurs')) return 'indicateurs'
    if (path.startsWith('/profile')) return 'profil'
    return null
}

const isGroupActive = (key: SectionKey) => getActiveGroup(route.path) === key

watch(
    () => route.path,
    (path) => {
        const group = getActiveGroup(path)
        if (group) openSections[group] = true
    },
    { immediate: true }
)

/* ---- Profil ---- */
const userRole = computed(() => authStore.user?.poste ?? 'Utilisateur')
const userFullName = computed(() => {
    const nom = authStore.user?.nom ?? 'Utilisateur'
    const prenom = authStore.user?.prenom ?? ''
    return prenom ? `${prenom} ${nom}` : nom
})
const userInitials = computed(() => {
    if (!authStore.user) return 'U'
    return `${authStore.user.prenom.charAt(0)}${authStore.user.nom.charAt(0)}`.toUpperCase()
})

const closeSidebar = () => emit('close')
</script>

<style scoped>
/* ============================================================
   SIDEBAR — Header fixe + nav scrollable + footer fixe
   ============================================================ */
.sidebar {
    --sb-w: 248px;
    --sb-w-sm: 76px;
    position: fixed;
    inset: 0 auto 0 0;
    z-index: 1045;
    width: var(--sb-w);
    display: flex;
    flex-direction: column;
    overflow: hidden;
    background: #fff;
    border-right: 1px solid #e6ebf3;
    font-family: "DM Sans", system-ui, sans-serif;
    transition: width .25s cubic-bezier(.4, 0, .2, 1);
}

/* Liseré navy en haut */
.sidebar::before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: var(--navy);
    z-index: 1;
    width: var(--sb-w);
}

/* --- Marque (fixe) --- */
.brand {
    flex-shrink: 0;
    position: relative;
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 27px 16px 10px;
    padding: 0 8px 20px;
    text-decoration: none;
    border-radius: 10px;
    cursor: pointer;
    transition: background .15s ease, margin .25s ease, padding .25s ease;
}
.brand:hover { background: #f7f9fd; }
.brand:focus-visible {
    outline: 2px solid #4a6cf7;
    outline-offset: 2px;
}
.brand::after {
    content: "";
    position: absolute;
    left: 8px;
    right: 8px;
    bottom: 0;
    height: 1px;
    border-radius: 3px;
    background: linear-gradient(90deg, #1a2a6c, #d4af37);
    opacity: .9;
}
.brand-icon {
    width: 40px;
    height: 40px;
    object-fit: cover;
    border-radius: 11px;
    box-shadow: 0 6px 16px rgba(7, 26, 61, .25);
    flex: none;
    transition: width .25s ease, height .25s ease;
}
.brand-word {
    font: 800 22px "Manrope", system-ui, sans-serif;
    letter-spacing: -1.2px;
    line-height: 1;
    color: #1a2a6c;
    overflow: hidden;
    transition: opacity .2s ease, width .25s ease;
}
.brand-word img { width: 80%; display: block; }
.brand-word small {
    display: block;
    margin-top: 6px;
    font: 700 7.5px "DM Sans", system-ui, sans-serif;
    letter-spacing: 1.35px;
    color: #6b7280;
}

/* --- Zone de navigation scrollable --- */
.sidebar-nav-scroll {
    flex: 1 1 auto;
    min-height: 0;
    overflow-y: auto;
    overflow-x: hidden;
    overscroll-behavior: contain;
    padding: 4px 16px 8px;
    transition: padding .25s ease;
}
.sidebar-nav-scroll::-webkit-scrollbar { width: 9px; }
.sidebar-nav-scroll::-webkit-scrollbar-thumb {
    background: rgba(26, 42, 108, .18);
    border-radius: 20px;
    border: 2px solid transparent;
    background-clip: content-box;
}

/* --- Navigation --- */
.nav {
    display: grid;
    gap: 2px;
}

.nav-item {
    position: relative;
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    height: 40px;
    padding: 0 12px;
    color: #6b7280;
    font-family: inherit;
    font-size: 12.5px;
    font-weight: 500;
    text-align: left;
    text-decoration: none;
    white-space: nowrap;
    border: none;
    background: transparent;
    border-radius: 10px;
    cursor: pointer;
    transition: background .18s, color .18s, padding .25s ease, gap .25s ease;
}
.nav-item:hover { background: #f3f6fc; color: #1a2a6c; }
.nav-item.active {
    background: #edf1fd;
    color: #1a2a6c;
    font-weight: 700;
}
.nav-item.active::before {
    content: "";
    position: absolute;
    left: 0;
    top: 9px;
    bottom: 9px;
    width: 3px;
    border-radius: 0 3px 3px 0;
    background: linear-gradient(180deg, #4a6cf7, #d4af37);
}
.nav-item:focus-visible {
    outline: 2px solid #d4af37;
    outline-offset: 1px;
}

.nav-toggle.has-active {
    background: #f7f9fd;
    color: #1a2a6c;
    font-weight: 600;
}
.nav-toggle.has-active .nav-icon { color: #4a6cf7; }

/* --- Icônes SVG --- */
.nav-icon {
    width: 18px;
    height: 18px;
    display: grid;
    place-items: center;
    flex: none;
    color: #8a95a8;
    transition: color .18s;
}
.nav-icon svg {
    width: 18px;
    height: 18px;
    fill: none;
    stroke: currentColor;
    stroke-width: 1.9;
    stroke-linecap: round;
    stroke-linejoin: round;
}
.nav-item:hover .nav-icon { color: #4a6cf7; }
.nav-item.active .nav-icon { color: #4a6cf7; }

.nav-text {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    transition: opacity .2s ease;
}

/* --- Chevron (accordéon) --- */
.chev {
    width: 14px;
    height: 14px;
    flex: none;
    margin-left: auto;
    fill: none;
    stroke: currentColor;
    stroke-width: 2.4;
    stroke-linecap: round;
    stroke-linejoin: round;
    opacity: .55;
    transition: transform .22s ease, opacity .2s;
}
.nav-item:hover .chev { opacity: .85; }
.chev.open { transform: rotate(180deg); opacity: 1; }

/* --- Sous-navigation (accordéon animé) --- */
.nav-sub {
    display: grid;
    grid-template-rows: 0fr;
    transition: grid-template-rows .22s ease;
}
.nav-sub.open {
    grid-template-rows: 1fr;
}
.nav-sub-inner {
    overflow: hidden;
    min-height: 0;
    display: grid;
    gap: 1px;
    margin: 2px 0 6px;
    padding-left: 42px;
}

.nav-sub-item {
    position: relative;
    display: flex;
    align-items: center;
    height: 32px;
    padding: 0 10px 0 14px;
    color: #6b7280;
    font-size: 12px;
    font-weight: 500;
    text-decoration: none;
    white-space: nowrap;
    border-radius: 8px;
    transition: background .15s, color .15s;
}
.nav-sub-item::before {
    content: "";
    position: absolute;
    left: 0;
    top: 50%;
    width: 4px;
    height: 4px;
    margin-top: -2px;
    border-radius: 50%;
    background: #c3cad5;
    transition: background .18s, transform .18s;
}
.nav-sub-item:hover {
    background: #f3f6fc;
    color: #1a2a6c;
}
.nav-sub-item:hover::before { background: #8fa6fa; }
.nav-sub-item.active {
    background: #edf1fd;
    color: #1a2a6c;
    font-weight: 700;
}
.nav-sub-item.active::before {
    background: linear-gradient(135deg, #4a6cf7, #d4af37);
    transform: scale(1.35);
}

/* --- Badge compteur --- */
.nav-count {
    margin-left: auto;
    padding: 2px 8px;
    border-radius: 20px;
    background: #fff;
    color: #4a6cf7;
    font-size: 10.5px;
    font-weight: 700;
    border: 1px solid #e6ebf3;
    transition: all .25s ease;
}

/* --- Zone fixe en bas --- */
.sidebar-bottom {
    flex-shrink: 0;
    padding: 10px 16px 14px;
    background: #fff;
    box-shadow: 0 -10px 22px -12px rgba(26, 42, 108, .10);
    transition: padding .25s ease;
}

.help-card {
    margin-bottom: 12px;
    padding: 14px;
    border: 1px solid #e6ebf3;
    border-radius: 12px;
    background: linear-gradient(140deg, #f7f9fd, #fff);
    overflow: hidden;
    transition: opacity .2s ease, max-height .25s ease, padding .25s ease, margin .25s ease;
    max-height: 200px;
}
.help-symbol {
    width: 26px;
    height: 26px;
    display: grid;
    place-items: center;
    margin-bottom: 10px;
    border-radius: 8px;
    background: #fff6dd;
    color: #b8860b;
    font-weight: 800;
}
.help-card strong { font-size: 11.5px; color: #1a2a6c; }
.help-card p {
    margin: 6px 0 10px;
    font-size: 10.5px;
    line-height: 1.6;
    color: #6b7280;
}
.help-card a {
    font-size: 10.5px;
    font-weight: 700;
    color: #1769c2;
    text-decoration: none;
}
.help-card a span {
    margin-left: 5px;
    transition: margin .18s;
}
.help-card a:hover span { margin-left: 9px; }

.profile {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 4px 0;
    text-align: left;
    color: #15233c;
    background: transparent;
    border-top: 1px solid #e6ebf3;
    text-decoration: none;
    transition: padding .25s ease, gap .25s ease;
}
.avatar {
    display: grid;
    place-items: center;
    flex: none;
    width: 34px;
    height: 34px;
    border-radius: 50%;
    color: #fff;
    font-size: 10.5px;
    font-weight: 700;
    background: linear-gradient(135deg, #1a2a6c, #4a6cf7);
    box-shadow: 0 0 0 2px #fff, 0 0 0 3.5px rgba(212, 175, 55, .7);
}
.profile-copy {
    display: flex;
    flex-direction: column;
    line-height: 1.15;
    min-width: 0;
    transition: opacity .2s ease;
}
.profile-copy strong {
    font-size: 11.5px;
    color: #1a2a6c;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.profile-copy small { margin-top: 3px; font-size: 10.5px; color: #6b7280; }
.dots {
    margin-left: auto;
    color: #8a95a8;
    letter-spacing: 1px;
    transition: opacity .2s ease;
}

/* ============================================================
   ÉTAT COLLAPSED — Desktop uniquement (icônes seules)
   ============================================================ */
.sidebar.collapsed {
    width: var(--sb-w-sm);
}

/* Marque : logo centré */
.sidebar.collapsed .brand {
    justify-content: center;
    margin: 27px 10px 10px;
    padding: 0 0 20px;
    gap: 0;
}
.sidebar.collapsed .brand:hover {
    background: transparent;
}
.sidebar.collapsed .brand-icon {
    width: 36px;
    height: 36px;
    box-shadow: 0 4px 12px rgba(7, 26, 61, .22);
}
.sidebar.collapsed .brand-word {
    opacity: 0;
    width: 0;
    pointer-events: none;
}
.sidebar.collapsed .brand::after {
    left: 10px;
    right: 10px;
}

/* Nav scroll : padding réduit */
.sidebar.collapsed .sidebar-nav-scroll {
    padding: 4px 10px 8px;
}

/* Items : icône seule, centrée */
.sidebar.collapsed .nav-item {
    justify-content: center;
    padding: 0;
    gap: 0;
    height: 44px;
}
.sidebar.collapsed .nav-text {
    flex: 0 0 0;
    width: 0;
    opacity: 0;
    pointer-events: none;
}

.sidebar.collapsed .chev {
    display: none; 
}

/* Indicateur actif recentré */
.sidebar.collapsed .nav-item.active::before {
    top: 12px;
    bottom: 12px;
    width: 3px;
}

/* Badge notifications : pastille en position absolue */
.sidebar.collapsed .nav-count {
    position: absolute;
    top: 6px;
    right: 6px;
    margin-left: 0;
    padding: 1px 5px;
    font-size: 9px;
    min-width: 16px;
    text-align: center;
    line-height: 1.4;
}

/* Sous-menus : masqués quand replié (état préservé en mémoire) */
.sidebar.collapsed .nav-sub {
    display: none;
}

/* Carte d'aide : masquée */
.sidebar.collapsed .help-card {
    opacity: 0;
    max-height: 0;
    padding: 0;
    margin: 0;
    border: 0;
    pointer-events: none;
}

/* Zone basse : padding réduit */
.sidebar.collapsed .sidebar-bottom {
    padding: 10px 10px 14px;
}

/* Profil : avatar centré, texte masqué */
.sidebar.collapsed .profile {
    justify-content: center;
    padding: 12px 0 0;
    gap: 0;
}
.sidebar.collapsed .profile-copy {
    opacity: 0;
    width: 0;
    overflow: hidden;
    pointer-events: none;
}
.sidebar.collapsed .dots {
    opacity: 0;
    width: 0;
    pointer-events: none;
}

/* ============================================================
   OVERLAY — fade in/out
   ============================================================ */
.sidebar-overlay {
    position: fixed;
    inset: 0;
    background: rgba(6, 20, 45, .72);
    backdrop-filter: blur(4px);
    -webkit-backdrop-filter: blur(4px);
    z-index: 1044;
    opacity: 0;
    visibility: hidden;
    pointer-events: none;
    transition:
        opacity .3s cubic-bezier(.4, 0, .2, 1),
        visibility 0s linear .3s;
}
.sidebar-overlay.show {
    opacity: 1;
    visibility: visible;
    pointer-events: auto;
    transition:
        opacity .3s cubic-bezier(.4, 0, .2, 1),
        visibility 0s linear 0s;
}

/* ============================================================
   MOBILE : slide gauche ⇄ droite (le collapse est ignoré)
   ============================================================ */
@media (max-width: 991.98px) {
    .sidebar {
        width: 248px !important;
        transform: translateX(-100%);
        visibility: hidden;
        box-shadow: none;
        transition:
            transform .3s cubic-bezier(.4, 0, .2, 1),
            visibility 0s linear .3s;
        will-change: transform;
    }
    .sidebar.open {
        transform: translateX(0);
        visibility: visible;
        box-shadow: 15px 0 40px rgba(7, 26, 61, .18);
        transition:
            transform .3s cubic-bezier(.4, 0, .2, 1),
            visibility 0s linear 0s;
    }

    /* Neutralise complètement le mode collapsed en mobile */
    .sidebar.collapsed {
        width: 248px !important;
    }
    .sidebar.collapsed .brand {
        justify-content: flex-start;
        margin: 27px 16px 10px;
        padding: 0 8px 20px;
        gap: 10px;
    }
    .sidebar.collapsed .brand-icon {
        width: 40px;
        height: 40px;
    }
    .sidebar.collapsed .brand-word {
        opacity: 1;
        width: auto;
        pointer-events: auto;
    }
    .sidebar.collapsed .nav-item {
        justify-content: flex-start;
        padding: 0 12px;
        gap: 12px;
        height: 40px;
    }
    .sidebar.collapsed .nav-text,
    .sidebar.collapsed .chev {
        opacity: 1;
        width: auto;
        pointer-events: auto;
    }
    .sidebar.collapsed .nav-sub {
        display: grid;
    }
    .sidebar.collapsed .nav-count {
        position: static;
        margin-left: auto;
        padding: 2px 8px;
        font-size: 10.5px;
        min-width: 0;
        text-align: left;
    }
    .sidebar.collapsed .help-card {
        opacity: 1;
        max-height: 200px;
        padding: 14px;
        margin-bottom: 12px;
        border: 1px solid #e6ebf3;
        pointer-events: auto;
    }
    .sidebar.collapsed .profile-copy,
    .sidebar.collapsed .dots {
        opacity: 1;
        width: auto;
        pointer-events: auto;
    }
    .sidebar.collapsed .profile {
        justify-content: flex-start;
        padding: 12px 4px 0;
        gap: 10px;
    }
    .sidebar.collapsed .sidebar-nav-scroll {
        padding: 4px 16px 8px;
    }
    .sidebar.collapsed .sidebar-bottom {
        padding: 10px 16px 14px;
    }
}

/* ============================================================
   DESKTOP : toujours visible
   ============================================================ */
@media (min-width: 992px) {
    .sidebar {
        transform: none !important;
        visibility: visible !important;
        box-shadow: none !important;
    }
    .sidebar-overlay {
        display: none !important;
    }
}

/* ============================================================
   Accessibilité
   ============================================================ */
@media (prefers-reduced-motion: reduce) {
    .sidebar,
    .sidebar-overlay,
    .sidebar * {
        transition: none !important;
    }
}
</style>
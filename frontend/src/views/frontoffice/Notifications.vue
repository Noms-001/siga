<template>
    <div class="notif-page">
        <!-- ============ EN-TÊTE ============ -->
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>Notifications</h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    Suivi des notifications du dispositif
                    <template v-if="state.unreadCount > 0">
                        — <strong>{{ state.unreadCount }}</strong> non lue{{ state.unreadCount > 1 ? 's' : '' }}
                    </template>
                </p>
            </div>

            <div class="welcome-row__right">
                <BaseButton
                    v-if="state.unreadCount > 0"
                    variant="primary"
                    :loading="markingAll"
                    :disabled="markingAll"
                    @click="toutLire"
                >
                    <i class="bi bi-check2-all"></i>
                    Tout marquer comme lu
                </BaseButton>
            </div>
        </header>

        <!-- ============ SYNTHÈSE ============ -->
        <section class="summary" aria-label="Synthèse des notifications">
            <div class="summary__stats" role="group" aria-label="Statistiques">
                <article class="stat-tile stat-tile--unread" :class="{ 'stat-tile--empty': state.unreadCount === 0 }">
                    <span class="stat-tile__count">{{ state.unreadCount }}</span>
                    <span class="stat-tile__label">Non lues</span>
                    <span class="stat-tile__hint">À traiter</span>
                </article>
                <article class="stat-tile stat-tile--visible">
                    <span class="stat-tile__count">{{ nbAffichees }}</span>
                    <span class="stat-tile__label">Affichées</span>
                    <span class="stat-tile__hint">Sur cette page</span>
                </article>
                <article class="stat-tile stat-tile--pages">
                    <span class="stat-tile__count">{{ totalPages || 1 }}</span>
                    <span class="stat-tile__label">Pages</span>
                    <span class="stat-tile__hint">Page {{ page + 1 }} sur {{ totalPages || 1 }}</span>
                </article>
            </div>
        </section>

        <!-- ============ FILTRES ============ -->
        <section class="filters-card" aria-labelledby="filters-title">
            <header class="filters-card__head">
                <span class="filters-card__icon" aria-hidden="true">
                    <i class="bi bi-sliders"></i>
                </span>
                <div class="filters-card__heading">
                    <h2 id="filters-title" class="filters-card__title">Filtres</h2>
                    <span class="filters-card__sub">
                        {{ filtresActifs ? 'Filtres actifs' : 'Affiner la liste des notifications' }}
                    </span>
                </div>
                <BaseButton
                    v-if="filtresActifs"
                    variant="secondary"
                    size="sm"
                    icon="bi bi-x-lg"
                    :disabled="refreshing"
                    @click="reinitialiserFiltres"
                >
                    Réinitialiser
                </BaseButton>
            </header>

            <div class="filters-card__body">
                <!-- Recherche (avec label visible) -->
                <div class="filters__field">
                    <label for="search-input" class="filters__label">Rechercher</label>
                    <div class="filters__search">
                        <i class="bi bi-search" aria-hidden="true"></i>
                        <input
                            id="search-input"
                            v-model="search"
                            type="search"
                            placeholder="Titre, message ou activité…"
                            @input="onSearchInput"
                            @keydown.enter="appliquerFiltres"
                        />
                        <button
                            v-if="search"
                            type="button"
                            class="filters__clear"
                            aria-label="Effacer la recherche"
                            @click="effacerRecherche"
                        >
                            <i class="bi bi-x-lg"></i>
                        </button>
                    </div>
                </div>

                <div class="filters__select">
                    <label for="search-input" class="filters__label">État</label>
                    <BaseSelect
                        v-model="lecture"
                        size="sm"
                        :options="lectureOptions"
                    />
                </div>

                <div class="filters__select">
                    <label for="search-input" class="filters__label">Priorité</label>
                    <BaseSelect
                        v-model="priorite"
                        size="sm"
                        :options="prioriteOptions"
                    />
                </div>
            </div>
        </section>

        <!-- ============ ERREUR ============ -->
        <div v-if="errorMessage" class="alert alert-danger mb-0 d-flex justify-content-between align-items-center" role="alert">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="charger">Réessayer</BaseButton>
        </div>

        <!-- ============ CHARGEMENT INITIAL ============ -->
        <div v-else-if="initialLoading" class="loading-state">
            <div v-for="i in 4" :key="i" class="skeleton-card">
                <div class="skeleton-row">
                    <div class="skeleton skeleton--badge"></div>
                    <div class="skeleton skeleton--title"></div>
                </div>
                <div class="skeleton skeleton--line"></div>
                <div class="skeleton skeleton--line skeleton--medium"></div>
                <div class="skeleton skeleton--meta"></div>
            </div>
        </div>

        <!-- ============ ÉTAT VIDE ============ -->
        <div v-else-if="notifications && notifications.length === 0" class="empty-state">
            <div class="empty-state__icon">
                <i class="bi" :class="filtresActifs ? 'bi-search' : 'bi-bell-slash'"></i>
            </div>
            <h2 class="empty-state__title">
                {{ filtresActifs ? 'Aucun résultat' : 'Aucune notification' }}
            </h2>
            <p class="empty-state__text">
                {{ filtresActifs
                    ? 'Aucune notification ne correspond aux critères sélectionnés.'
                    : 'Vous n\'avez aucune notification pour le moment.' }}
            </p>
            <BaseButton
                v-if="filtresActifs"
                variant="secondary"
                size="sm"
                @click="reinitialiserFiltres"
            >
                Réinitialiser les filtres
            </BaseButton>
        </div>

        <!-- ============ LISTE ============ -->
        <div v-else class="notif-list" :class="{ 'notif-list--refreshing': refreshing }">
            <article
                v-for="item in notifications"
                :key="item.id"
                class="notif-item"
                :class="[
                    { 'notif-item--unread': !item.dateLecture },
                    classePrioriteItem(item.prioriteCode),
                ]"
            >
                <div class="notif-item__body">
                    <div class="notif-item__head">
                        <span
                            v-if="item.prioriteLibelle"
                            class="badge-priority"
                            :class="classePriorite(item.prioriteCode)"
                        >
                            {{ item.prioriteLibelle }}
                        </span>
                        <h3 class="notif-item__title">{{ item.titre }}</h3>
                    </div>

                    <p class="notif-item__message" :title="item.message">{{ item.message }}</p>

                    <div class="notif-item__footer">
                        <span class="notif-item__time" :title="formatDateAbsolue(item.dateCreation)">
                            <i class="bi bi-clock" aria-hidden="true"></i>
                            {{ formatDateRelative(item.dateCreation) }}
                        </span>

                        <RouterLink
                            v-if="item.idActivite"
                            :to="`/activites/${item.idActivite}`"
                            class="notif-item__link"
                        >
                            <i class="bi bi-box-arrow-up-right" aria-hidden="true"></i>
                            {{ item.activite || 'Voir l\'activité' }}
                        </RouterLink>
                    </div>
                </div>

                <div class="notif-item__actions">
                    <button
                        v-if="!item.dateLecture"
                        type="button"
                        class="btn-read"
                        :disabled="readingIds.has(item.id)"
                        @click="lire(item.id)"
                    >
                        <i
                            v-if="readingIds.has(item.id)"
                            class="bi bi-arrow-repeat spin"
                            aria-hidden="true"
                        ></i>
                        <i v-else class="bi bi-check2" aria-hidden="true"></i>
                        <span>{{ readingIds.has(item.id) ? 'Marquage…' : 'Marquer comme lue' }}</span>
                    </button>

                    <span v-else class="badge-read">
                        <i class="bi bi-check2-all" aria-hidden="true"></i>
                        Lue
                    </span>
                </div>
            </article>
        </div>

        <!-- ============ PAGINATION ============ -->
        <nav v-if="totalPages > 1" class="pagination" aria-label="Pagination des notifications">
            <BaseButton
                variant="secondary"
                size="sm"
                :disabled="page === 0 || refreshing"
                @click="changerPage(page - 1)"
            >
                <i class="bi bi-chevron-left"></i>
                Précédent
            </BaseButton>

            <div class="pagination__pages">
                <template v-for="(p, index) in pagesVisibles" :key="index">
                    <span v-if="p === -1" class="pagination__ellipsis">…</span>
                    <button
                        v-else
                        type="button"
                        class="pagination__page"
                        :class="{ 'pagination__page--active': p === page }"
                        :aria-current="p === page ? 'page' : undefined"
                        :disabled="refreshing"
                        @click="changerPage(p)"
                    >
                        {{ p + 1 }}
                    </button>
                </template>
            </div>

            <BaseButton
                variant="secondary"
                size="sm"
                :disabled="page + 1 >= totalPages || refreshing"
                @click="changerPage(page + 1)"
            >
                Suivant
                <i class="bi bi-chevron-right"></i>
            </BaseButton>
        </nav>
    </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { BaseButton, BaseSelect } from '@/components/base'
import {
    chargerPageNotifications,
    actualiserNotifications,
    marquerNotificationLue,
    marquerToutesNotificationsLues,
    useNotifications,
    type NotificationItem,
    type NotificationFiltres,
} from '@/services/notification'

/* -------------------- état du store -------------------- */
const state = useNotifications()

/* -------------------- état local -------------------- */
const notifications = ref<NotificationItem[]>([])
const initialLoading = ref(true)
const refreshing = ref(false)
const markingAll = ref(false)
const readingIds = ref<Set<number>>(new Set())
const errorMessage = ref('')

/* Pagination */
const page = ref(0)
const totalPages = ref(0)
const pageSize = 10

/* Filtres */
const search = ref('')
const lecture = ref<NotificationFiltres['lecture']>('TOUTES')
const priorite = ref('')

/* Debounce search */
let searchTimer: ReturnType<typeof setTimeout> | null = null

/* -------------------- en-tête -------------------- */
const dateDuJour = computed(() =>
    new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric',
    })
        .format(new Date())
        .toUpperCase()
)

/* -------------------- options des filtres -------------------- */
const lectureOptions = [
    { value: 'TOUTES', label: 'Toutes' },
    { value: 'NON_LUES', label: 'Non lues' },
    { value: 'LUES', label: 'Lues' },
]

const prioriteOptions = [
    { value: '', label: 'Toutes' },
    { value: 'CRITIQUE', label: 'Critique' },
    { value: 'HAUTE', label: 'Haute' },
    { value: 'NORMALE', label: 'Normale' },
    { value: 'FAIBLE', label: 'Faible' },
]

/* -------------------- filtres actifs -------------------- */
const filtresActifs = computed(() =>
    search.value.trim() !== ''
    || lecture.value !== 'TOUTES'
    || priorite.value !== ''
)

/* -------------------- stats -------------------- */
const nbAffichees = computed(() => notifications.value ? notifications.value.length : 0)

/* -------------------- chargement -------------------- */
function filtresCourants(): NotificationFiltres {
    return {
        search: search.value.trim(),
        lecture: lecture.value,
        priorite: priorite.value,
    }
}

async function charger(): Promise<void> {
    if (notifications.value.length === 0) {
        initialLoading.value = true
    } else {
        refreshing.value = true
    }
    errorMessage.value = ''

    const resultat = await chargerPageNotifications(page.value, pageSize, filtresCourants())

    if (!resultat) {
        errorMessage.value = 'Connexion au serveur impossible. Réessayez dans un instant.'
        initialLoading.value = false
        refreshing.value = false
        return
    }

    totalPages.value = resultat ? resultat.length : 0

    // Correction si page hors bornes après filtrage
    if (totalPages.value > 0 && page.value >= totalPages.value) {
        page.value = totalPages.value - 1
        return charger()
    }
    if (totalPages.value === 0) page.value = 0

    notifications.value = resultat
    initialLoading.value = false
    refreshing.value = false
}

/* -------------------- actions -------------------- */
async function changerPage(nouvellePage: number): Promise<void> {
    if (nouvellePage < 0 || nouvellePage >= totalPages.value) return
    page.value = nouvellePage
    await charger()
}

async function appliquerFiltres(): Promise<void> {
    page.value = 0
    await charger()
}

async function reinitialiserFiltres(): Promise<void> {
    search.value = ''
    lecture.value = 'TOUTES'
    priorite.value = ''
    await appliquerFiltres()
}

function effacerRecherche(): void {
    search.value = ''
    void appliquerFiltres()
}

function onSearchInput(): void {
    if (searchTimer) clearTimeout(searchTimer)
    searchTimer = setTimeout(() => {
        void appliquerFiltres()
    }, 350)
}

async function lire(id: number): Promise<void> {
    if (readingIds.value.has(id)) return

    const ids = new Set(readingIds.value)
    ids.add(id)
    readingIds.value = ids

    // Optimistic update
    const target = notifications.value.find(n => n.id === id)
    const wasUnread = target && !target.dateLecture
    const previous = target?.dateLecture ?? null
    if (target) target.dateLecture = new Date().toISOString()

    const ok = await marquerNotificationLue(id)

    if (!ok && wasUnread && target) {
        // Rollback
        target.dateLecture = previous
    } else if (ok) {
        await actualiserNotifications()
        // Si filtre non-lues, retirer de la liste sans attendre un refetch complet
        if (lecture.value === 'NON_LUES') {
            notifications.value = notifications.value.filter(n => n.id !== id)
        }
    }

    const ids2 = new Set(readingIds.value)
    ids2.delete(id)
    readingIds.value = ids2
}

async function toutLire(): Promise<void> {
    if (markingAll.value) return
    markingAll.value = true

    const now = new Date().toISOString()
    const snapshot = notifications.value.map(n => ({ id: n.id, prev: n.dateLecture }))
    notifications.value.forEach(n => { n.dateLecture = now })

    const ok = await marquerToutesNotificationsLues()

    if (!ok) {
        // Rollback
        for (const { id, prev } of snapshot) {
            const n = notifications.value.find(x => x.id === id)
            if (n) n.dateLecture = prev
        }
    } else {
        await Promise.all([charger(), actualiserNotifications()])
    }

    markingAll.value = false
}

/* -------------------- helpers d'affichage -------------------- */
function formatDateRelative(iso: string): string {
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return iso

    const diffMs = Date.now() - d.getTime()
    const diffMin = Math.floor(diffMs / 60000)
    const diffH = Math.floor(diffMs / 3600000)
    const diffJ = Math.floor(diffMs / 86400000)

    if (diffMin < 1) return "à l'instant"
    if (diffMin < 60) return `il y a ${diffMin} min`
    if (diffH < 24) return `il y a ${diffH} h`
    if (diffJ < 7) return `il y a ${diffJ} j`

    return d.toLocaleDateString('fr-FR', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
    })
}

function formatDateAbsolue(iso: string): string {
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return iso
    return d.toLocaleString('fr-FR')
}

function classePriorite(code: string | null | undefined): string {
    const c = (code ?? '').toUpperCase()
    if (c === 'CRITIQUE') return 'priority--critique'
    if (c === 'HAUTE') return 'priority--haute'
    if (c === 'NORMALE') return 'priority--normale'
    if (c === 'FAIBLE') return 'priority--faible'
    return 'priority--normale'
}

/** Classe posée sur la carte pour piloter les barres latérales colorées. */
function classePrioriteItem(code: string | null | undefined): string {
    const c = (code ?? '').toUpperCase()
    if (c === 'CRITIQUE') return 'notif-item--prio-critique'
    if (c === 'HAUTE') return 'notif-item--prio-haute'
    if (c === 'NORMALE') return 'notif-item--prio-normale'
    if (c === 'FAIBLE') return 'notif-item--prio-faible'
    return 'notif-item--prio-normale'
}

/* -------------------- pagination intelligente -------------------- */
const pagesVisibles = computed<number[]>(() => {
    const total = totalPages.value
    const current = page.value
    if (total <= 7) return Array.from({ length: total }, (_, i) => i)

    const pages: number[] = []
    if (current <= 3) {
        for (let i = 0; i < 5; i++) pages.push(i)
        pages.push(-1)
        pages.push(total - 1)
    } else if (current >= total - 4) {
        pages.push(0)
        pages.push(-1)
        for (let i = total - 5; i < total; i++) pages.push(i)
    } else {
        pages.push(0)
        pages.push(-1)
        for (let i = current - 1; i <= current + 1; i++) pages.push(i)
        pages.push(-1)
        pages.push(total - 1)
    }
    return pages
})

/* -------------------- lifecycle -------------------- */
onMounted(async () => {
    await actualiserNotifications()
    await charger()
})

onBeforeUnmount(() => {
    if (searchTimer) clearTimeout(searchTimer)
})
</script>

<style scoped>
/* ============================================================
   CONTENEUR
   ============================================================ */
.notif-page {
    min-height: calc(100vh - 68px);
    padding: 34px 38px 24px;
    display: flex;
    flex-direction: column;
    gap: 1.15rem;
    color: #15233c;
    font-family: "DM Sans", "Segoe UI", sans-serif;
    background:
        radial-gradient(circle at 12% 8%, rgba(26, 42, 108, .055) 0, transparent 38%),
        radial-gradient(circle at 92% 94%, rgba(212, 175, 55, .065) 0, transparent 36%),
        #f5f7fa;
}

.notif-page .welcome-row {
    margin-bottom: 0.25rem;
}

.notif-page .subheading strong {
    color: #1a2a6c;
    font-weight: 800;
}

/* ============================================================
   SYNTHÈSE — 3 tuiles
   ============================================================ */
.summary {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    padding: 1.25rem 1.5rem;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .04),
        0 8px 24px rgba(26, 42, 108, .06);
}

.summary__stats {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 0.85rem;
}

.stat-tile {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    justify-content: center;
    gap: 0.25rem;
    padding: 1rem 1.1rem;
    background: #f8fafc;
    border: 1.5px solid #e6ebf3;
    border-radius: 12px;
    position: relative;
    overflow: hidden;
}

.stat-tile::before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    bottom: 0;
    width: 4px;
    background: #c8d6fb;
}
.stat-tile--unread::before { background: #d64545; }
.stat-tile--visible::before { background: #4a6cf7; }
.stat-tile--pages::before { background: #8a95a8; }

.stat-tile--empty::before { background: #4ad8a7; }

.stat-tile__count {
    font-family: Manrope, "DM Sans", sans-serif;
    font-weight: 800;
    font-size: 1.85rem;
    line-height: 1;
    color: #1a2a6c;
    font-variant-numeric: tabular-nums;
}
.stat-tile--unread .stat-tile__count { color: #a61b16; }
.stat-tile--unread.stat-tile--empty .stat-tile__count { color: #1e7e34; }
.stat-tile--visible .stat-tile__count { color: #135d95; }
.stat-tile--pages .stat-tile__count { color: #5b6b7d; }

.stat-tile__label {
    font-size: 0.66rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: #8a95a8;
}

.stat-tile__hint {
    font-size: 0.72rem;
    color: #6b7280;
}

/* ============================================================
   FILTRES
   ============================================================ */
.filters-card {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .04),
        0 8px 24px rgba(26, 42, 108, .06);
    overflow: hidden;
}

.filters-card__head {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.85rem 1.25rem;
    background: linear-gradient(140deg, #f7f9fd, #fff);
    border-bottom: 1px solid #e6ebf3;
}

.filters-card__icon {
    display: grid;
    place-items: center;
    width: 32px;
    height: 32px;
    border-radius: 9px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.95rem;
    flex: none;
}

.filters-card__heading {
    display: flex;
    flex-direction: column;
    gap: 1px;
    flex: 1;
    min-width: 0;
}

.filters-card__title {
    margin: 0;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 0.9rem;
    font-weight: 700;
    color: #1a2a6c;
    letter-spacing: -0.005em;
    line-height: 1.2;
}

.filters-card__sub {
    font-size: 0.72rem;
    color: #6b7280;
    line-height: 1.2;
}

.filters-card__body {
    display: grid;
    grid-template-columns: minmax(240px, 1fr) minmax(160px, 200px) minmax(160px, 200px);
    gap: 0.85rem;
    align-items: end;
    padding: 1rem 1.25rem;
}

/* ============================================================
   CHAMP RECHERCHE (avec label visible)
   ============================================================ */
.filters__field {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
}

.filters__label {
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    color: #6b7280;
    line-height: 1.2;
    margin-left: 10px;
}

.filters__search {
    position: relative;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0 0.85rem;
    min-height: 38px;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 10px;
    transition: border-color .15s, box-shadow .15s;
}
.filters__search:focus-within {
    border-color: #4a6cf7;
    box-shadow: 0 0 0 3px rgba(74, 108, 247, .14);
}

.filters__search i {
    color: #8a95a8;
    font-size: 0.9rem;
}

.filters__search input {
    flex: 1;
    min-width: 0;
    border: none;
    outline: none;
    background: transparent;
    font-size: 0.85rem;
    color: #15233c;
    padding: 0.55rem 0;
    font-family: inherit;
}
.filters__search input::placeholder { color: #9aa4b8; }

.filters__clear {
    display: grid;
    place-items: center;
    width: 22px;
    height: 22px;
    padding: 0;
    border: none;
    border-radius: 6px;
    background: #eef1f5;
    color: #6b7280;
    cursor: pointer;
    transition: background .15s, color .15s;
}
.filters__clear:hover { background: #dfe5ec; color: #1a2a6c; }

.filters__select { min-width: 0; }

/* ============================================================
   ÉTAT VIDE
   ============================================================ */
.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 4rem 1.5rem;
    text-align: center;
    background: #fff;
    border: 1px dashed #dde4ec;
    border-radius: 16px;
}

.empty-state__icon {
    display: grid;
    place-items: center;
    width: 64px;
    height: 64px;
    border-radius: 20px;
    background: #eaf0ff;
    color: #4a6cf7;
    font-size: 1.7rem;
    margin-bottom: 1rem;
}

.empty-state__title {
    margin: 0 0 0.35rem;
    font-size: 1.05rem;
    font-weight: 700;
    color: #1a2a6c;
}

.empty-state__text {
    margin: 0 0 1.1rem;
    font-size: 0.88rem;
    color: #6b7280;
}

/* ============================================================
   SKELETON
   ============================================================ */
.loading-state {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.skeleton-card {
    padding: 1.15rem 1.25rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
}

.skeleton-row {
    display: flex;
    align-items: center;
    gap: 0.6rem;
}

.skeleton {
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: shimmer 1.4s infinite;
    border-radius: 6px;
}

.skeleton--badge { width: 60px; height: 18px; border-radius: 20px; }
.skeleton--title { height: 16px; flex: 1; max-width: 40%; }
.skeleton--line { height: 12px; }
.skeleton--medium { width: 60%; }
.skeleton--meta { width: 40%; height: 12px; }

@keyframes shimmer {
    from { background-position: 200% 0; }
    to { background-position: -200% 0; }
}

/* ============================================================
   LISTE DES NOTIFICATIONS
   ============================================================ */
.notif-list {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    transition: opacity .2s;
}

.notif-list--refreshing {
    opacity: 0.55;
    pointer-events: none;
    position: relative;
}

.notif-item {
    position: relative;
    display: flex;
    gap: 1rem;
    padding: 1.15rem 1.4rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 14px;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .03),
        0 4px 12px rgba(26, 42, 108, .04);
    transition: border-color .15s, box-shadow .15s, background .15s;
    overflow: hidden;
}

/* ============================================================
   BARRES LATÉRALES — symétriques, pilotées par la priorité
   ============================================================ */
.notif-item::before,
.notif-item::after {
    content: "";
    position: absolute;
    top: 0;
    bottom: 0;
    width: 4px;
    background: #c8d6fb;
    transition: background .2s, width .2s, opacity .2s;
    opacity: 0.4;
    z-index: 1;
}
.notif-item::before { left: 0; }
.notif-item::after  { right: 0; }

/* --- Priorité CRITIQUE : barres rouges épaisses + glow --- */
.notif-item--prio-critique::before,
.notif-item--prio-critique::after {
    width: 5px;
    background: #d64545;
}

/* --- Priorité HAUTE : barres orange épaisses --- */
.notif-item--prio-haute::before,
.notif-item--prio-haute::after {
    width: 5px;
    background: #f4b91f;
}

/* --- Priorité NORMALE : barres bleues --- */
.notif-item--prio-normale::before,
.notif-item--prio-normale::after {
    background: #4a6cf7;
}

/* --- Priorité FAIBLE : barres vertes fines --- */
.notif-item--prio-faible::before,
.notif-item--prio-faible::after {
    width: 3px;
    background: #4ad8a7;
}

/* ============================================================
   ÉTAT NON LUE : barres pleinement visibles + fond teinté
   ============================================================ */
.notif-item--unread {
    background: linear-gradient(90deg, #f8fafc 0%, #fff 40%);
    border-color: #d3dce8;
}

.notif-item--unread::before,
.notif-item--unread::after {
    opacity: 1;
}

/* Renfort visuel : glow coloré uniquement pour HAUTE / CRITIQUE non lues */
.notif-item--unread.notif-item--prio-critique {
    border-color: #f6cfcc;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .03),
        0 4px 16px rgba(214, 69, 69, .12);
}

.notif-item--unread.notif-item--prio-haute {
    border-color: #f4e2c2;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .03),
        0 4px 16px rgba(244, 185, 31, .12);
}

/* ============================================================
   ÉTAT LUE : barres atténuées, sauf CRITIQUE (info critique)
   ============================================================ */
.notif-item:not(.notif-item--unread)::before,
.notif-item:not(.notif-item--unread)::after {
    opacity: 0.4;
}

.notif-item:not(.notif-item--unread).notif-item--prio-critique::before,
.notif-item:not(.notif-item--unread).notif-item--prio-critique::after {
    opacity: 0.65;
}

.notif-item:not(.notif-item--unread).notif-item--prio-haute::before,
.notif-item:not(.notif-item--unread).notif-item--prio-haute::after {
    opacity: 0.55;
}

/* Hover global */
.notif-item:hover {
    border-color: #d3dce8;
    box-shadow:
        0 2px 4px rgba(26, 42, 108, .05),
        0 8px 20px rgba(26, 42, 108, .07);
}

/* --- Corps de la notification --- */
.notif-item__body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.notif-item__head {
    display: flex;
    align-items: center;
    gap: 0.55rem;
    flex-wrap: wrap;
}

.notif-item__title {
    margin: 0;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 0.95rem;
    font-weight: 700;
    color: #15233c;
    line-height: 1.35;
    letter-spacing: -0.005em;
    flex: 1;
    min-width: 0;
}

.notif-item--unread .notif-item__title {
    color: #1a2a6c;
}

/* Titres : priorité visuelle renforcée pour CRITIQUE / HAUTE */
.notif-item--prio-critique .notif-item__title,
.notif-item--prio-critique.notif-item--unread .notif-item__title {
    color: #a61b16;
}

.notif-item--prio-haute .notif-item__title,
.notif-item--prio-haute.notif-item--unread .notif-item__title {
    color: #9a6409;
}

.notif-item__message {
    margin: 0;
    font-size: 0.86rem;
    line-height: 1.55;
    color: #384a5e;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
    overflow-wrap: anywhere;
}

.notif-item__footer {
    display: flex;
    align-items: center;
    gap: 1rem;
    flex-wrap: wrap;
    margin-top: 0.15rem;
}

.notif-item__time {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    font-size: 0.75rem;
    color: #8a95a8;
}
.notif-item__time i { font-size: 0.8rem; }

.notif-item__link {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.2rem 0.55rem;
    border-radius: 6px;
    background: #eef5f8;
    color: #135d95;
    font-size: 0.78rem;
    font-weight: 600;
    text-decoration: none;
    transition: background .15s, color .15s;
}
.notif-item__link:hover {
    background: #dceefb;
    color: #0d4a75;
}
.notif-item__link i { font-size: 0.72rem; }

/* --- Actions --- */
.notif-item__actions {
    flex: none;
    display: flex;
    align-items: flex-start;
    padding-top: 0.15rem;
}

.btn-read {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.45rem 0.85rem;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    background: #fff;
    color: #384a5e;
    font-size: 0.78rem;
    font-weight: 600;
    cursor: pointer;
    transition: background .15s, color .15s, border-color .15s;
    white-space: nowrap;
}

.btn-read:hover:not(:disabled) {
    background: #eaf0ff;
    color: #1a2a6c;
    border-color: #4a6cf7;
}

.btn-read:disabled {
    cursor: wait;
    color: #8a95a8;
    background: #f8fafc;
}

.badge-read {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.3rem 0.7rem;
    border-radius: 20px;
    background: #f1f5f9;
    color: #8a95a8;
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.02em;
    white-space: nowrap;
}
.badge-read i { font-size: 0.75rem; color: #4ad8a7; }

.spin {
    animation: spin 1s linear infinite;
}

@keyframes spin {
    to { transform: rotate(360deg); }
}

/* ============================================================
   BADGES DE PRIORITÉ
   ============================================================ */
.badge-priority {
    display: inline-block;
    padding: 0.2rem 0.6rem;
    border-radius: 20px;
    font-size: 0.7rem;
    font-weight: 700;
    letter-spacing: 0.03em;
    white-space: nowrap;
    text-transform: uppercase;
}

.priority--critique {
    background: #fee2e2;
    color: #b42318;
    border: 1px solid #f6cfcc;
}

.priority--haute {
    background: #ffedd5;
    color: #c2410c;
    border: 1px solid #fbd7a9;
}

.priority--normale {
    background: #dbeafe;
    color: #1d4ed8;
    border: 1px solid #cde2f6;
}

.priority--faible {
    background: #dcfce7;
    color: #15803d;
    border: 1px solid #c8e5cf;
}

/* ============================================================
   PAGINATION
   ============================================================ */
.pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 0.75rem;
    padding: 1rem 0 0.5rem;
    flex-wrap: wrap;
}

.pagination__pages {
    display: flex;
    align-items: center;
    gap: 0.25rem;
}

.pagination__page {
    min-width: 34px;
    height: 34px;
    padding: 0 0.5rem;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    background: #fff;
    color: #384a5e;
    font-size: 0.82rem;
    font-weight: 600;
    cursor: pointer;
    transition: background .15s, color .15s, border-color .15s;
    font-variant-numeric: tabular-nums;
}

.pagination__page:hover:not(:disabled):not(.pagination__page--active) {
    background: #f3f6fc;
    border-color: #c6d2ff;
    color: #1a2a6c;
}

.pagination__page--active {
    background: linear-gradient(135deg, #1a2a6c 0%, #4a6cf7 100%);
    color: #fff;
    border-color: transparent;
    box-shadow: 0 4px 12px rgba(26, 42, 108, .22);
    cursor: default;
}

.pagination__page:disabled { cursor: not-allowed; opacity: 0.55; }

.pagination__ellipsis {
    color: #8a95a8;
    padding: 0 0.35rem;
    font-weight: 600;
    user-select: none;
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 1199.98px) {
    .notif-page { padding-left: 24px; padding-right: 24px; }
}

@media (max-width: 991.98px) {
    .summary__stats { grid-template-columns: repeat(3, minmax(0, 1fr)); }
    .filters-card__body {
        grid-template-columns: 1fr 1fr;
    }
    .filters__field {
        grid-column: 1 / -1;
    }
}

@media (max-width: 767.98px) {
    .notif-page { padding: 24px 16px 20px; }
    .notif-item {
        flex-direction: column;
        padding: 1rem 1.15rem;
        gap: 0.75rem;
    }
    .notif-item__actions {
        padding-top: 0;
        align-self: flex-end;
    }
    .notif-item__footer { gap: 0.6rem; }
    .filters-card__body { grid-template-columns: 1fr; }
    .filters__field { grid-column: auto; }
    .pagination__pages { display: none; }
}

@media (max-width: 575.98px) {
    .notif-page { padding: 20px 12px 16px; }
    .summary { padding: 1rem; }
    .summary__stats {
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 0.5rem;
    }
    .stat-tile { padding: 0.8rem 0.75rem; }
    .stat-tile__count { font-size: 1.35rem; }
    .stat-tile__label { font-size: 0.6rem; }
    .stat-tile__hint { font-size: 0.65rem; }
    .filters-card__head { padding: 0.75rem 1rem; }
    .filters-card__body { padding: 0.9rem 1rem; }
}

@media (prefers-reduced-motion: reduce) {
    .skeleton { animation: none !important; }
    .spin { animation: none !important; }
    .notif-item { transition: none !important; }
}
</style>
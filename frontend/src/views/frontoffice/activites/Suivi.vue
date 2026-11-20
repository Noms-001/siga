<script setup lang="ts">
import { useToastMessage } from '@/composables/useToast'
import { computed, onMounted, ref, watch } from 'vue'
import { BaseButton, BaseInput, BaseModal, BaseSelect } from '@/components/base'
import {
    autocompleteActivites,
    enregistrerAvancement,
    listerSuivi,
    optionsSuivi,
    statistiquesActivites,
    statistiquesSuivi,
    type AutocompleteItem,
} from '@/services/suivi'
import type {
    ActiviteOptions,
    ActiviteStatistiques,
    ActiviteSuivi,
    ActiviteSuiviStatistiques,
    AvancementRequest,
} from '@/types/suivi'
import ProgressCircle from '@/components/suivi/ProgressCircle.vue'

/* -------------------- état -------------------- */

const pta = ref<boolean>(true)

const stats = ref<ActiviteStatistiques | null>(null)
const statsSuivi = ref<ActiviteSuiviStatistiques | null>(null)
const statsLoading = ref(false)
const statsError = useToastMessage('error')

const activites = ref<ActiviteSuivi[]>([])
const loading = ref(false)
const errorMessage = useToastMessage('error')
const totalElements = ref(0)
const page = ref(0)
const size = ref(20)

const options = ref<ActiviteOptions | null>(null)

/**
 * Repli/dépli des sous-activités, par identifiant d'activité.
 * Un identifiant absent du Set signifie « déplié » — c'est le comportement
 * par défaut, on ne stocke que ce qui a été explicitement replié.
 */
const sousActivitesRepliees = ref<Set<number>>(new Set())

function toggleSousActivites(id: number): void {
    const prochain = new Set(sousActivitesRepliees.value)
    if (prochain.has(id)) prochain.delete(id)
    else prochain.add(id)
    sousActivitesRepliees.value = prochain
}

function sousActivitesOuvertes(id: number): boolean {
    return !sousActivitesRepliees.value.has(id)
}

/* -------------------- modal d'avancement (2 étapes) -------------------- */

const modalOpen = ref(false)
const modalStep = ref<'edit' | 'confirm'>('edit')
const modalLoading = ref(false)
const modalError = useToastMessage('error')

const sousActiviteSelectionnee = ref<{
    id: number
    code: string
    designation: string
} | null>(null)

const valeurActuelle = ref(0)
const modalValeur = ref(0)
const modalCommentaire = ref('')

const sens = computed<'same' | 'up' | 'down' | 'reopen'>(() => {
    const v = Math.round(modalValeur.value)
    const a = valeurActuelle.value
    if (v === a) return 'same'
    if (a >= 100 && v < 100) return 'reopen'
    return v > a ? 'up' : 'down'
})

const commentaireObligatoire = computed(() =>
    sens.value === 'down' || sens.value === 'reopen'
)

const peutPasserEnConfirmation = computed(() => {
    if (sens.value === 'same') return false
    if (commentaireObligatoire.value && !modalCommentaire.value.trim()) return false
    return true
})

function ouvrirModalAvancement(s: {
    id: number
    code: string
    designation: string
    avancement: number
}): void {
    sousActiviteSelectionnee.value = {
        id: s.id,
        code: s.code,
        designation: s.designation,
    }
    valeurActuelle.value = Math.round(s.avancement ?? 0)
    modalValeur.value = valeurActuelle.value
    modalCommentaire.value = ''
    modalError.value = ''
    modalStep.value = 'edit'
    modalOpen.value = true
}

function fermerModalAvancement(): void {
    if (modalLoading.value) return
    modalOpen.value = false
    modalStep.value = 'edit'
    sousActiviteSelectionnee.value = null
}

function passerEnConfirmation(): void {
    modalError.value = ''
    if (!peutPasserEnConfirmation.value) return
    modalStep.value = 'confirm'
}

function revenirEnEdition(): void {
    modalStep.value = 'edit'
}

async function confirmerEnregistrement(): Promise<void> {
    if (!sousActiviteSelectionnee.value) return

    modalError.value = ''
    modalLoading.value = true

    const payload: AvancementRequest = {
        valeurPourcentage: Math.round(modalValeur.value),
        commentaire: modalCommentaire.value.trim() || null,
    }

    const res = await enregistrerAvancement(
        sousActiviteSelectionnee.value.id,
        payload
    )
    modalLoading.value = false

    if (!res.success) {
        modalStep.value = 'edit'
        modalError.value = res.error
        return
    }

    const idCible = sousActiviteSelectionnee.value.id
    for (const activite of activites.value) {
        const sa = activite.sousActivites.find(x => x.id === idCible)
        if (sa) {
            sa.avancement = res.data.avancement
            activite.avancement = res.data.avancementActivite

            if (res.data.statutActiviteCode && activite.statut) {
                activite.statut.code = res.data.statutActiviteCode
                if (res.data.statutActiviteLibelle) {
                    activite.statut.libelle = res.data.statutActiviteLibelle
                }
            }
            break
        }
    }

    modalOpen.value = false
    modalStep.value = 'edit'
    sousActiviteSelectionnee.value = null

    await chargerStats()
    await chargerListe()
}

/* Filtres actifs */
const carteActive = ref<'all' | 'NON_COMMENCEE' | 'EN_COURS' | 'TERMINEE'>('all')
const recherche = ref('')
const idService = ref<number | 'all'>('all')
const idPriorite = ref<number | 'all'>('all')
const filtreDateDebut = ref('')
const filtreDateFin = ref('')

/* Autocomplete BaseSelect */
const suggestions = ref<AutocompleteItem[]>([])
const suggestionSelection = ref<number | string | null>(null)
let searchTimer: ReturnType<typeof setTimeout> | null = null
let suggestionRequestId = 0

function formatSuggestion(option: AutocompleteItem): string {
    const secondaire = option.libelleSecondaire ? ` (${option.libelleSecondaire})` : ''
    return `${option.code} — ${option.libelle}${secondaire}`
}

/* -------------------- computed -------------------- */

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

const erreurPeriode = computed(() =>
    filtreDateDebut.value && filtreDateFin.value && filtreDateDebut.value > filtreDateFin.value
        ? 'La date de début doit être antérieure ou égale à la date de fin.'
        : ''
)

const serviceOptions = computed(() => {
    const list = options.value?.services ?? []
    return [
        { value: 'all', label: 'Tous les services' },
        ...list.map(o => ({ value: o.id, label: o.libelle })),
    ]
})

const prioriteOptions = computed(() => {
    const list = options.value?.priorites ?? []
    return [
        { value: 'all', label: 'Toutes les priorités' },
        ...list.map(o => ({ value: o.id, label: `${o.code ?? ''} — ${o.libelle}` })),
    ]
})

const statutBackend = computed(() => {
    return carteActive.value === 'all' ? undefined : carteActive.value
})

const avancementGlobal = computed(() => {
    return Math.round(statsSuivi.value?.avancementMoyen ?? 0)
})

const totalSousActivites = computed(() =>
    statsSuivi.value?.totalSousActivites ?? 0
)

const filtresActifs = computed(() =>
    carteActive.value !== 'all'
    || recherche.value.trim() !== ''
    || idService.value !== 'all'
    || idPriorite.value !== 'all'
    || filtreDateDebut.value !== ''
    || filtreDateFin.value !== ''
)

/** Y a-t-il au moins une activité qui porte des sous-activités ? */
const aDesSousActivites = computed(() =>
    activites.value.some(a => a.sousActivites.length > 0)
)

/** Toutes les activités sont-elles actuellement repliées ? */
const toutesRepliees = computed(() => {
    const avecSous = activites.value.filter(a => a.sousActivites.length > 0)
    if (avecSous.length === 0) return false
    return avecSous.every(a => sousActivitesRepliees.value.has(a.id))
})

function toutReplier(): void {
    const prochain = new Set(sousActivitesRepliees.value)
    for (const a of activites.value) {
        if (a.sousActivites.length > 0) prochain.add(a.id)
    }
    sousActivitesRepliees.value = prochain
}

function toutDeplier(): void {
    const prochain = new Set(sousActivitesRepliees.value)
    for (const a of activites.value) {
        prochain.delete(a.id)
    }
    sousActivitesRepliees.value = prochain
}

function basculerTout(): void {
    if (toutesRepliees.value) toutDeplier()
    else toutReplier()
}

/* -------------------- chargements -------------------- */

function buildParams() {
    return {
        pta: pta.value,
        search: recherche.value.trim() || undefined,
        serviceId: idService.value === 'all' ? undefined : Number(idService.value),
        prioriteId: idPriorite.value === 'all' ? undefined : Number(idPriorite.value),
        statut: statutBackend.value,
        dateDebut: filtreDateDebut.value || undefined,
        dateFin: filtreDateFin.value || undefined,
    }
}

let statsRequestId = 0
let listeRequestId = 0

async function chargerStats(): Promise<void> {
    const requestId = ++statsRequestId
    statsLoading.value = true
    statsError.value = ''
    try {
        const params = buildParams()
        const [compteurs, agregats] = await Promise.all([
            statistiquesActivites(params),
            statistiquesSuivi(params),
        ])
        if (requestId !== statsRequestId) return
        statsLoading.value = false
        if (!compteurs.success) statsError.value = compteurs.error
        else stats.value = compteurs.data
        if (!agregats.success) statsError.value = agregats.error
        else statsSuivi.value = agregats.data
    } catch {
        if (requestId !== statsRequestId) return
        statsLoading.value = false
        statsError.value = 'Impossible de charger les statistiques du suivi.'
    }
}

async function chargerListe(): Promise<void> {
    const requestId = ++listeRequestId
    loading.value = true
    errorMessage.value = ''
    try {
        const res = await listerSuivi({
            ...buildParams(),
            page: page.value,
            size: size.value,
        })
        if (requestId !== listeRequestId) return
        loading.value = false
        if (!res.success) {
            errorMessage.value = res.error
            return
        }
        activites.value = res.data.content
        totalElements.value = res.data.totalElements
    } catch {
        if (requestId !== listeRequestId) return
        loading.value = false
        errorMessage.value = 'Impossible de charger la liste des activités.'
    }
}

async function chargerOptions(): Promise<void> {
    const res = await optionsSuivi()
    if (res.success) options.value = res.data
}

/* -------------------- comportements -------------------- */

function basculerPta(valeur: boolean): void {
    if (pta.value === valeur) return
    pta.value = valeur
    if (recherche.value.trim()) onRechercheInput(recherche.value)
    else {
        suggestionRequestId++
        suggestions.value = []
    }
    page.value = 0
    recharger()
}

function choisirCarte(carte: typeof carteActive.value): void {
    carteActive.value = carte
    page.value = 0
    recharger()
}

function recharger(): void {
    if (erreurPeriode.value) {
        statsRequestId++
        listeRequestId++
        statsLoading.value = false
        loading.value = false
        stats.value = null
        statsSuivi.value = null
        activites.value = []
        totalElements.value = 0
        statsError.value = erreurPeriode.value
        errorMessage.value = erreurPeriode.value
        return
    }
    chargerStats()
    chargerListe()
}

function reinitialiserFiltres(): void {
    carteActive.value = 'all'
    recherche.value = ''
    suggestionSelection.value = null
    suggestions.value = []
    suggestionRequestId++
    if (searchTimer) clearTimeout(searchTimer)
    idService.value = 'all'
    idPriorite.value = 'all'
    filtreDateDebut.value = ''
    filtreDateFin.value = ''
    page.value = 0
    recharger()
}

function onRechercheInput(terme: string): void {
    if (searchTimer) clearTimeout(searchTimer)
    const requestId = ++suggestionRequestId
    recherche.value = terme
    const q = terme.trim()
    if (!q) {
        suggestions.value = []
        page.value = 0
        recharger()
        return
    }
    searchTimer = setTimeout(async () => {
        const res = await autocompleteActivites(q, pta.value)
        if (requestId !== suggestionRequestId) return
        if (res.success) {
            suggestions.value = res.data
        } else {
            suggestions.value = []
        }
    }, 250)
}

function selectionnerActivite(valeur: number | string | null): void {
    suggestionSelection.value = valeur
    if (valeur === null) return
    suggestionRequestId++
    if (searchTimer) clearTimeout(searchTimer)
    const item = typeof valeur === 'number'
        ? suggestions.value.find(suggestion => suggestion.id === valeur)
        : null
    recherche.value = item?.code ?? String(valeur)
    page.value = 0
    recharger()
}

function validerRechercheLibre(): void {
    if (suggestions.value.length === 0 && recherche.value.trim()) {
        page.value = 0
        recharger()
    }
}

/* -------------------- formats -------------------- */

function dateDebut(a: ActiviteSuivi): string {
    return formaterDate(a.dateDebutReelle ?? a.dateDebutPrevue)
}
function dateFin(a: ActiviteSuivi): string {
    return formaterDate(a.dateFinReelle ?? a.dateFinPrevue)
}

function formaterDate(iso?: string | null): string {
    if (!iso) return '—'
    const d = new Date(iso)
    return Number.isNaN(d.getTime()) ? iso : d.toLocaleDateString('fr-FR')
}

function classeStatut(a: ActiviteSuivi): string {
    const code = a.statut?.code ?? ''
    if (code === 'TERMINEE') return 'statut--terminee'
    if (code === 'EN_COURS') return 'statut--en-cours'
    if (code === 'NON_COMMENCEE') return 'statut--non-commencee'
    return 'statut--autre'
}

function classePriorite(a: ActiviteSuivi): string {
    const code = (a.priorite?.code ?? '').toUpperCase()
    if (code === 'CRITIQUE') return 'prio--critique'
    if (code === 'HAUTE') return 'prio--haute'
    if (code === 'NORMALE') return 'prio--normale'
    if (code === 'FAIBLE') return 'prio--faible'
    return 'prio--autre'
}

function sousStatutClass(avancement: number): string {
    if (avancement >= 100) return 'sous-item--done'
    if (avancement > 0) return 'sous-item--wip'
    return 'sous-item--idle'
}

/* -------------------- lifecycle -------------------- */

onMounted(async () => {
    await chargerOptions()
    recharger()
})

watch([idService, idPriorite, filtreDateDebut, filtreDateFin], () => {
    page.value = 0
    recharger()
})
</script>

<template>
    <div class="suivi-page">
        <!-- ============ EN-TÊTE ============ -->
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>Suivi des activités</h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    Suivi et avancement des activités du département TSS
                </p>
            </div>

            <div class="welcome-row__right">
                <div class="scope-switch" role="tablist" aria-label="Périmètre">
                    <button type="button" role="tab" class="scope-switch__btn"
                        :class="{ 'scope-switch__btn--active': pta }" :aria-selected="pta" @click="basculerPta(true)">
                        <i class="bi bi-clipboard-check"></i>
                        PTA
                    </button>
                    <button type="button" role="tab" class="scope-switch__btn"
                        :class="{ 'scope-switch__btn--active': !pta }" :aria-selected="!pta"
                        @click="basculerPta(false)">
                        <i class="bi bi-clipboard"></i>
                        Courante
                    </button>
                </div>
            </div>
        </header>

        <!-- ============ SYNTHÈSE ============ -->
        <section class="summary" aria-label="Synthèse du suivi">
            <div class="summary__stats" role="group" aria-label="Filtres par statut">
                <button type="button" class="stat-tile stat-tile--total"
                    :class="{ 'stat-tile--active': carteActive === 'all' }" :aria-pressed="carteActive === 'all'"
                    :disabled="statsLoading" @click="choisirCarte('all')">
                    <span class="stat-tile__count">{{ stats?.total ?? 0 }}</span>
                    <span class="stat-tile__label">Total</span>
                </button>

                <button type="button" class="stat-tile stat-tile--nc"
                    :class="{ 'stat-tile--active': carteActive === 'NON_COMMENCEE' }"
                    :aria-pressed="carteActive === 'NON_COMMENCEE'" :disabled="statsLoading"
                    @click="choisirCarte('NON_COMMENCEE')">
                    <span class="stat-tile__count">{{ stats?.nonCommencees ?? 0 }}</span>
                    <span class="stat-tile__label">Non commencées</span>
                </button>

                <button type="button" class="stat-tile stat-tile--ec"
                    :class="{ 'stat-tile--active': carteActive === 'EN_COURS' }"
                    :aria-pressed="carteActive === 'EN_COURS'" :disabled="statsLoading"
                    @click="choisirCarte('EN_COURS')">
                    <span class="stat-tile__count">{{ stats?.enCours ?? 0 }}</span>
                    <span class="stat-tile__label">En cours</span>
                </button>

                <button type="button" class="stat-tile stat-tile--te"
                    :class="{ 'stat-tile--active': carteActive === 'TERMINEE' }"
                    :aria-pressed="carteActive === 'TERMINEE'" :disabled="statsLoading"
                    @click="choisirCarte('TERMINEE')">
                    <span class="stat-tile__count">{{ stats?.terminees ?? 0 }}</span>
                    <span class="stat-tile__label">Terminées</span>
                </button>
            </div>

            <div class="summary__progress">
                <div class="summary__progress-head">
                    <span class="summary__progress-label">Avancement moyen des activités filtrées</span>
                    <span class="summary__progress-value">
                        {{ avancementGlobal }}<small>%</small>
                    </span>
                </div>
                <div class="summary__bar" role="progressbar" :aria-valuenow="avancementGlobal" aria-valuemin="0"
                    aria-valuemax="100">
                    <div class="summary__bar-fill" :style="{ width: avancementGlobal + '%' }"></div>
                </div>
                <div class="summary__progress-foot">
                    {{ statsSuivi?.totalActivites ?? 0 }} activité{{ (statsSuivi?.totalActivites ?? 0) > 1 ? 's' : '' }}
                    correspondant{{ (statsSuivi?.totalActivites ?? 0) > 1 ? 'es' : 'e' }} aux filtres
                    <span class="summary__dot">•</span>
                    {{ totalSousActivites }} sous-activité{{ totalSousActivites > 1 ? 's' : '' }}
                </div>
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
                        {{ filtresActifs ? 'Filtres actifs' : 'Affiner la liste des activités' }}
                    </span>
                </div>
                <BaseButton v-if="filtresActifs" variant="secondary" size="sm" icon="bi bi-x-lg"
                    @click="reinitialiserFiltres">
                    Réinitialiser
                </BaseButton>
            </header>

            <div class="filters-card__body">
                <BaseSelect :model-value="suggestionSelection ?? undefined" class="filters__search"
                    label="Rechercher une activité" icon="bi bi-search" size="sm"
                    placeholder="Code, référence ou désignation…" :options="suggestions"
                    option-label="code" option-value="id" :option-formatter="formatSuggestion"
                    autocomplete remote clear-on-input clearable @search="onRechercheInput"
                    @update:model-value="selectionnerActivite" @keydown.enter="validerRechercheLibre" />

                <div v-if="(options?.services?.length ?? 0) > 0" class="filters__select">
                    <BaseSelect v-model="idService" size="sm" placeholder="Tous les services"
                        :options="serviceOptions" />
                </div>

                <div class="filters__select">
                    <BaseSelect v-model="idPriorite" size="sm" placeholder="Toutes les priorités"
                        :options="prioriteOptions" />
                </div>

                <div class="filters__dates">
                    <BaseInput v-model="filtreDateDebut" type="date" label="Date de début" />
                    <BaseInput v-model="filtreDateFin" type="date" label="Date de fin" />
                </div>
                <p v-if="erreurPeriode" class="filters__date-error" role="alert">{{ erreurPeriode }}</p>
            </div>
        </section>

        <!-- ============ CHARGEMENT ============ -->
        <div v-if="loading" class="loading-state">
            <div v-for="i in 3" :key="i" class="skeleton-card">
                <div class="skeleton skeleton--line skeleton--short"></div>
                <div class="skeleton skeleton--line"></div>
                <div class="skeleton skeleton--line skeleton--medium"></div>
                <div class="skeleton skeleton--bar"></div>
            </div>
        </div>

        <!-- ============ ÉTAT VIDE ============ -->
        <div v-else-if="activites.length === 0" class="empty-state">
            <div class="empty-state__icon">
                <i class="bi bi-inbox"></i>
            </div>
            <h2 class="empty-state__title">Aucune activité à afficher</h2>
            <p class="empty-state__text">
                Aucune activité ne correspond aux critères sélectionnés.
            </p>
            <BaseButton v-if="filtresActifs" variant="secondary" size="sm" @click="reinitialiserFiltres">
                Réinitialiser les filtres
            </BaseButton>
        </div>

        <!-- ============ LISTE DES ACTIVITÉS ============ -->
        <div v-else class="activities">

            <!-- Barre d'actions globale : tout replier / tout déplier -->
            <div v-if="aDesSousActivites" class="activities__toolbar">
                <span class="activities__toolbar-label">
                    <i class="bi bi-diagram-3" aria-hidden="true"></i>
                    {{ activites.length }} activité{{ activites.length > 1 ? 's' : '' }}
                    sur cette page
                </span>
                <BaseButton size="sm" variant="secondary" @click="basculerTout">
                    <i class="bi" :class="toutesRepliees ? 'bi-arrows-expand' : 'bi-arrows-collapse'"></i>
                    {{ toutesRepliees ? 'Tout déplier' : 'Tout replier' }}
                </BaseButton>
            </div>

            <article v-for="a in activites" :key="a.id" class="activity-card" :class="classeStatut(a)">
                <div class="activity-card__head">
                    <div class="activity-card__ids">
                        <RouterLink :to="{ name: 'activite-detail', params: { id: a.id } }" class="activity-card__code"
                            :aria-label="`Voir la fiche de l'activité ${a.code}`">
                            {{ a.code }}
                        </RouterLink>
                        <RouterLink v-if="a.reference" :to="{ name: 'activite-detail', params: { id: a.id } }"
                            class="activity-card__ref" :aria-label="`Voir la fiche de l'activité ${a.reference}`">
                            {{ a.reference }}
                        </RouterLink>
                    </div>
                    <div class="activity-card__badges">
                        <span v-if="a.statut" class="badge-statut" :class="classeStatut(a)">
                            {{ a.statut.libelle }}
                        </span>
                        <span v-if="a.priorite" class="badge-prio" :class="classePriorite(a)">
                            {{ a.priorite.libelle }}
                        </span>
                    </div>
                </div>

                <h3 class="activity-card__title">
                    <RouterLink :to="{ name: 'activite-detail', params: { id: a.id } }"
                        :aria-label="`Voir la fiche de l'activité ${a.designation}`">
                        {{ a.designation }}
                    </RouterLink>
                </h3>

                <div class="activity-card__meta">
                    <span v-if="a.responsable" class="activity-card__meta-item">
                        <i class="bi bi-person" aria-hidden="true"></i>
                        {{ a.responsable.prenom }} {{ a.responsable.nom }}
                    </span>
                    <span v-if="a.service" class="activity-card__meta-item">
                        <i class="bi bi-building" aria-hidden="true"></i>
                        {{ a.service.libelle }}
                    </span>
                    <span class="activity-card__meta-item">
                        <i class="bi bi-calendar3" aria-hidden="true"></i>
                        {{ dateDebut(a) }} → {{ dateFin(a) }}
                    </span>
                </div>

                <div class="activity-card__progress">
                    <div class="activity-card__progress-head">
                        <span class="activity-card__progress-label">Avancement</span>
                        <strong class="activity-card__progress-value">{{ a.avancement }}&nbsp;%</strong>
                    </div>
                    <div class="activity-card__progress-bar" role="progressbar" :aria-valuenow="a.avancement"
                        aria-valuemin="0" aria-valuemax="100">
                        <div class="activity-card__progress-fill" :style="{ width: a.avancement + '%' }"></div>
                    </div>
                </div>

                <div v-if="a.sousActivites.length" class="sous-activites">
                    <button type="button" class="sous-activites__head" :aria-expanded="sousActivitesOuvertes(a.id)"
                        :aria-controls="`sous-activites-${a.id}`" @click="toggleSousActivites(a.id)">
                        <i class="bi bi-diagram-3" aria-hidden="true"></i>
                        <span class="sous-activites__title">Sous-activités</span>
                        <span class="sous-activites__count">{{ a.sousActivites.length }}</span>
                        <i class="bi sous-activites__chevron"
                            :class="sousActivitesOuvertes(a.id) ? 'bi-chevron-up' : 'bi-chevron-down'"
                            aria-hidden="true"></i>
                    </button>

                    <ul v-show="sousActivitesOuvertes(a.id)" :id="`sous-activites-${a.id}`"
                        class="sous-activites__list">
                        <li v-for="s in a.sousActivites" :key="s.id" class="sous-item"
                            :class="sousStatutClass(s.avancement ?? 0)">
                            <span class="sous-item__dot" aria-hidden="true"></span>
                            <div class="sous-item__infos">
                                <RouterLink
                                    :to="{ name: 'sous-activite-detail', params: { id: a.id, sousActiviteId: s.id } }"
                                    class="sous-item__designation"
                                    :aria-label="`Voir la fiche de la sous-activité ${s.designation}`">
                                    {{ s.designation }}
                                </RouterLink>
                                <RouterLink
                                    :to="{ name: 'sous-activite-detail', params: { id: a.id, sousActiviteId: s.id } }"
                                    class="sous-item__code" :aria-label="`Voir la fiche de la sous-activité ${s.code}`">
                                    {{ s.code }}
                                </RouterLink>
                            </div>
                            <ProgressCircle :value="s.avancement ?? 0" label="Modifier l'avancement" @click="ouvrirModalAvancement({
                                id: s.id,
                                code: s.code,
                                designation: s.designation,
                                avancement: s.avancement ?? 0,
                            })" />
                        </li>
                    </ul>
                </div>
            </article>
        </div>

        <!-- ============ PAGINATION ============ -->
        <nav v-if="totalElements > size" class="pagination" aria-label="Pagination">
            <BaseButton size="sm" variant="secondary" :disabled="page === 0" @click="page--; chargerListe()">
                <i class="bi bi-chevron-left"></i>
                Précédent
            </BaseButton>
            <span class="pagination__info">
                Page <strong>{{ page + 1 }}</strong> sur {{ Math.ceil(totalElements / size) }}
                <span class="pagination__sep">•</span>
                {{ totalElements }} résultat{{ totalElements > 1 ? 's' : '' }}
            </span>
            <BaseButton size="sm" variant="secondary" :disabled="(page + 1) * size >= totalElements"
                @click="page++; chargerListe()">
                Suivant
                <i class="bi bi-chevron-right"></i>
            </BaseButton>
        </nav>
    </div>

    <!-- ============ MODAL AVANCEMENT ============ -->
    <BaseModal v-model="modalOpen"
        :title="modalStep === 'edit' ? 'Modifier l\'avancement' : 'Confirmer la modification'" size="md"
        :loading="modalLoading" :closeOnOutside="!modalLoading"
        @update:modelValue="(v: boolean) => !v && fermerModalAvancement()">
        <div v-if="modalStep === 'edit' && sousActiviteSelectionnee" class="modal-avancement">
            <div class="modal-avancement__sous">
                <strong>{{ sousActiviteSelectionnee.designation }}</strong>
                <span class="modal-avancement__code">{{ sousActiviteSelectionnee.code }}</span>
            </div>

            <div v-if="sens === 'reopen'" class="modal-warning modal-warning--danger" role="note">
                <i class="bi bi-exclamation-triangle-fill"></i>
                <div>
                    <strong>Réouverture de la sous-activité</strong>
                    <p>Cette sous-activité est actuellement terminée (100 %).<br>
                        Vous allez ramener son avancement à {{ Math.round(modalValeur) }} %.<br>
                        Une justification est obligatoire.</p>
                </div>
            </div>

            <div v-else-if="sens === 'down'" class="modal-warning modal-warning--danger" role="note">
                <i class="bi bi-exclamation-triangle-fill"></i>
                <div>
                    <strong>Diminution de l'avancement</strong>
                    <p>L'avancement actuel est de {{ valeurActuelle }} %.<br>
                        La nouvelle valeur est de {{ Math.round(modalValeur) }} %.<br>
                        Une justification est obligatoire.</p>
                </div>
            </div>

            <div v-else-if="sens === 'up' && Math.round(modalValeur) >= 100"
                class="modal-warning modal-warning--success" role="note">
                <i class="bi bi-check-circle-fill"></i>
                <div>
                    <strong>Sous-activité terminée</strong>
                    <p>En passant à 100 %, la sous-activité sera considérée comme terminée.</p>
                </div>
            </div>

            <div v-else-if="sens === 'same'" class="modal-warning modal-warning--neutral" role="note">
                <i class="bi bi-info-circle-fill"></i>
                <div>
                    <p>Aucune modification de l'avancement.</p>
                </div>
            </div>

            <div class="modal-avancement__current">
                Avancement actuel&nbsp;: <strong>{{ valeurActuelle }} %</strong>
            </div>

            <div class="modal-avancement__field">
                <label class="modal-avancement__label">Nouvel avancement</label>
                <div class="modal-avancement__value">{{ Math.round(modalValeur) }} %</div>
                <input type="range" min="0" max="100" step="1" v-model.number="modalValeur"
                    class="modal-avancement__range" :disabled="modalLoading" />
                <div class="modal-avancement__range-bounds">
                    <span>0 %</span><span>100 %</span>
                </div>
            </div>

            <div class="modal-avancement__field">
                <label class="modal-avancement__label">
                    Commentaire
                    <span v-if="commentaireObligatoire" class="required">*</span>
                    <span v-else class="text-muted-custom">(facultatif)</span>
                </label>
                <textarea v-model="modalCommentaire" class="form-control" rows="3" maxlength="2000"
                    :placeholder="commentaireObligatoire ? 'Justifiez la diminution…' : 'Précisez l\'état d\'avancement…'"
                    :disabled="modalLoading"></textarea>
            </div>
        </div>

        <div v-else-if="modalStep === 'confirm' && sousActiviteSelectionnee"
            class="modal-avancement modal-avancement--confirm">
            <div v-if="sens === 'reopen'" class="modal-warning modal-warning--danger" role="note">
                <i class="bi bi-exclamation-triangle-fill"></i>
                <div>
                    <strong>Réouverture de la sous-activité</strong>
                    <p>L'avancement va diminuer et la sous-activité repassera d'un état terminé vers un état en cours.
                    </p>
                </div>
            </div>

            <div v-else-if="sens === 'down'" class="modal-warning modal-warning--danger" role="note">
                <i class="bi bi-exclamation-triangle-fill"></i>
                <div>
                    <strong>Confirmer la diminution</strong>
                    <p>L'avancement va diminuer.</p>
                </div>
            </div>

            <p class="modal-avancement__question">Voulez-vous enregistrer cet avancement&nbsp;?</p>

            <dl class="modal-avancement__recap">
                <dt>Sous-activité</dt>
                <dd>{{ sousActiviteSelectionnee.designation }}</dd>
                <dt>Avancement</dt>
                <dd><strong>{{ valeurActuelle }} % → {{ Math.round(modalValeur) }} %</strong></dd>
                <template v-if="modalCommentaire.trim()">
                    <dt>Commentaire</dt>
                    <dd>{{ modalCommentaire.trim() }}</dd>
                </template>
            </dl>

            <p class="modal-avancement__note">
                Cette modification sera enregistrée dans l'historique.
                Les anciennes valeurs sont conservées.
            </p>
        </div>

        <template #footer>
            <div class="d-flex justify-content-end gap-2">
                <template v-if="modalStep === 'edit'">
                    <BaseButton variant="secondary" :disabled="modalLoading" @click="fermerModalAvancement">
                        Annuler
                    </BaseButton>
                    <BaseButton :disabled="!peutPasserEnConfirmation" @click="passerEnConfirmation">
                        Enregistrer
                    </BaseButton>
                </template>
                <template v-else>
                    <BaseButton variant="secondary" :disabled="modalLoading" @click="revenirEnEdition">
                        Annuler
                    </BaseButton>
                    <BaseButton :loading="modalLoading" :variant="commentaireObligatoire ? 'danger' : 'primary'"
                        @click="confirmerEnregistrement">
                        Confirmer
                    </BaseButton>
                </template>
            </div>
        </template>
    </BaseModal>
</template>

<style scoped>
/* ============================================================
   CONTENEUR
   ============================================================ */
.suivi-page {
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

/* Override du welcome-row global pour un espacement page-racine */
.suivi-page .welcome-row {
    margin-bottom: 0.25rem;
}

/* ============================================================
   SCOPE SWITCH (PTA / NON PTA)
   ============================================================ */
.scope-switch {
    display: inline-flex;
    padding: 4px;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 12px;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04);
}

.scope-switch__btn {
    display: inline-flex;
    align-items: center;
    gap: 0.45rem;
    padding: 0.5rem 1.1rem;
    border: none;
    border-radius: 9px;
    background: transparent;
    color: #6b7280;
    font-size: 0.82rem;
    font-weight: 600;
    cursor: pointer;
    transition: background .15s, color .15s, box-shadow .15s;
}

.scope-switch__btn i {
    font-size: 0.9rem;
    color: #8a95a8;
    transition: color .15s;
}

.scope-switch__btn:hover:not(.scope-switch__btn--active) {
    background: #f7f9fd;
    color: #1a2a6c;
}

.scope-switch__btn:hover:not(.scope-switch__btn--active) i {
    color: #4a6cf7;
}

.scope-switch__btn--active {
    background: linear-gradient(135deg, #1a2a6c 0%, #4a6cf7 100%);
    color: #fff;
    box-shadow: 0 4px 12px rgba(26, 42, 108, .25);
}

.scope-switch__btn--active i {
    color: #ffd04a;
}

/* ============================================================
   SYNTHÈSE
   ============================================================ */
.summary {
    display: flex;
    flex-direction: column;
    gap: 1.15rem;
    padding: 1.25rem 1.5rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .04),
        0 8px 24px rgba(26, 42, 108, .06);
}

.summary__stats {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 0.75rem;
}

.stat-tile {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 0.85rem 1rem;
    background: #f8fafc;
    border: 1.5px solid #e6ebf3;
    border-radius: 12px;
    cursor: pointer;
    transition: border-color .15s, background .15s, transform .15s, box-shadow .15s;
}

.stat-tile:hover:not(:disabled) {
    border-color: #c6d2ff;
    background: #f3f6fc;
    transform: translateY(-1px);
}

.stat-tile:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.stat-tile--active {
    background: linear-gradient(135deg, #eaf0ff 0%, #f3f6fc 100%);
    border-color: #4a6cf7;
    box-shadow: 0 0 0 3px rgba(74, 108, 247, .12);
}

.stat-tile__count {
    font-family: Manrope, "DM Sans", sans-serif;
    font-weight: 800;
    font-size: 1.6rem;
    line-height: 1;
    color: #1a2a6c;
    font-variant-numeric: tabular-nums;
}

.stat-tile__label {
    margin-top: 0.35rem;
    font-size: 0.66rem;
    font-weight: 700;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    color: #8a95a8;
    text-align: center;
    white-space: nowrap;
}

.stat-tile--nc .stat-tile__count {
    color: #5b6b7d;
}

.stat-tile--ec .stat-tile__count {
    color: #b8860b;
}

.stat-tile--te .stat-tile__count {
    color: #1e7e34;
}

.summary__progress {
    min-width: 0;
}

.summary__progress-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 0.75rem;
}

.summary__progress-label {
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: #8a95a8;
}

.summary__progress-value {
    font-family: Manrope, "DM Sans", sans-serif;
    font-weight: 800;
    font-size: 2rem;
    line-height: 1;
    color: #1a2a6c;
    letter-spacing: -0.02em;
    font-variant-numeric: tabular-nums;
}

.summary__progress-value small {
    font-size: 0.9rem;
    font-weight: 700;
    color: #8a95a8;
    margin-left: 0.1rem;
}

.summary__bar {
    margin-top: 0.6rem;
    height: 12px;
    border-radius: 20px;
    background: #eef1f5;
    overflow: hidden;
}

.summary__bar-fill {
    height: 100%;
    border-radius: 20px;
    background: linear-gradient(90deg, #1a2a6c 0%, #4a6cf7 50%, #d4af37 100%);
    transition: width 0.5s cubic-bezier(.4, 0, .2, 1);
}

.summary__progress-foot {
    margin-top: 0.55rem;
    font-size: 0.75rem;
    color: #6b7280;
}

.summary__dot {
    color: #c3cad5;
    margin: 0 0.35rem;
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
    grid-template-columns: minmax(240px, 1fr) auto auto;
    gap: 0.75rem;
    align-items: center;
    padding: 1rem 1.25rem;
}

.filters__search {
    min-width: 0;
}

.filters__select {
    min-width: 190px;
}

.filters__dates {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0.75rem;
}

.filters__date-error {
    grid-column: 1 / -1;
    margin: -0.35rem 0 0;
    color: #a61b16;
    font-size: 0.78rem;
}

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
    gap: 0.9rem;
}

.skeleton-card {
    padding: 1.1rem 1.25rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    display: flex;
    flex-direction: column;
    gap: 0.65rem;
}

.skeleton {
    position: relative;
    overflow: hidden;
    background: #eef1f5;
    border-radius: 6px;
}


.skeleton::after {
    content: "";
    position: absolute;
    inset: 0;
    background: linear-gradient(90deg,
            transparent 0%,
            rgba(255, 255, 255, 0.65) 50%,
            transparent 100%);
    transform: translateX(-100%);
    animation: shimmer 2s ease-in-out infinite;
}

.skeleton--line {
    height: 12px;
}

.skeleton--short {
    width: 30%;
}

.skeleton--medium {
    width: 60%;
}

.skeleton--bar {
    height: 8px;
    border-radius: 4px;
}

@keyframes shimmer {
    from {
        transform: translateX(-100%);
    }

    to {
        transform: translateX(100%);
    }
}

/* ============================================================
   LISTE DES ACTIVITÉS + BARRE TOUT REPLIER / TOUT DÉPLIER
   ============================================================ */
.activities {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.activities__toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.75rem;
    padding: 0.6rem 0.9rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 12px;
    font-size: 0.8rem;
    color: #6b7280;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04);
}

.activities__toolbar-label {
    display: inline-flex;
    align-items: center;
    gap: 0.45rem;
}

.activities__toolbar-label i {
    color: #8a95a8;
    font-size: 0.9rem;
}

.activity-card {
    position: relative;
    padding: 1.15rem 1.35rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .04),
        0 8px 24px rgba(26, 42, 108, .06);
    display: flex;
    flex-direction: column;
    gap: 0.85rem;
    overflow: hidden;
}

.activity-card::before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    bottom: 0;
    width: 4px;
    background: #c8d6fb;
}

.activity-card.statut--non-commencee::before {
    background: #8a95a8;
}

.activity-card.statut--en-cours::before {
    background: #f4b91f;
}

.activity-card.statut--terminee::before {
    background: #4ad8a7;
}

.activity-card.statut--autre::before {
    background: #c8d6fb;
}

.activity-card__head {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 1rem;
    flex-wrap: wrap;
}

.activity-card__ids {
    display: flex;
    align-items: baseline;
    gap: 0.65rem;
    flex-wrap: wrap;
    min-width: 0;
}

.activity-card__code {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.82rem;
    font-weight: 700;
    color: #1a2a6c;
    letter-spacing: 0.02em;
    text-decoration: none;
    transition: color .12s;
}

.activity-card__code:hover {
    color: #4a6cf7;
}

.activity-card__ref {
    font-size: 0.76rem;
    color: #8a95a8;
    text-decoration: none;
    transition: color .12s;
}

.activity-card__ref:hover {
    color: #4a6cf7;
}

.activity-card__badges {
    display: flex;
    gap: 0.4rem;
    flex-wrap: wrap;
}

.badge-statut,
.badge-prio {
    display: inline-block;
    padding: 0.25rem 0.65rem;
    border-radius: 20px;
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.02em;
    white-space: nowrap;
}

.badge-statut.statut--non-commencee {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
}

.badge-statut.statut--en-cours {
    background: #fdf3e2;
    color: #9a6409;
    border: 1px solid #f4e2c2;
}

.badge-statut.statut--terminee {
    background: #e6f4ea;
    color: #1e7e34;
    border: 1px solid #c8e5cf;
}

.badge-statut.statut--autre {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
}

.badge-prio.prio--critique {
    background: #fdecea;
    color: #a61b16;
    border: 1px solid #f6cfcc;
}

.badge-prio.prio--haute {
    background: #fdf3e2;
    color: #9a6409;
    border: 1px solid #f4e2c2;
}

.badge-prio.prio--normale {
    background: #e4f0fb;
    color: #135d95;
    border: 1px solid #cde2f6;
}

.badge-prio.prio--faible,
.badge-prio.prio--autre {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
}

.activity-card__title {
    margin: 0;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 1.05rem;
    font-weight: 700;
    color: #15233c;
    line-height: 1.35;
    letter-spacing: -0.01em;
}

.activity-card__title a {
    text-decoration: none;
    color: inherit;
    transition: color .12s;
}

.activity-card__title a:hover {
    color: #4a6cf7;
}

.activity-card__title a:focus-visible,
.activity-card__code:focus-visible,
.activity-card__ref:focus-visible,
.sous-item__designation:focus-visible,
.sous-item__code:focus-visible {
    outline: 2px solid #4a6cf7;
    outline-offset: 2px;
    border-radius: 3px;
}

.activity-card__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 0.35rem 1.25rem;
    font-size: 0.82rem;
    color: #6b7280;
}

.activity-card__meta-item {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
}

.activity-card__meta-item i {
    color: #8a95a8;
    font-size: 0.85rem;
}

.activity-card__progress {
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
}

.activity-card__progress-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.activity-card__progress-label {
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    color: #8a95a8;
}

.activity-card__progress-value {
    font-size: 0.95rem;
    font-weight: 800;
    color: #1a2a6c;
    font-variant-numeric: tabular-nums;
}

.activity-card__progress-bar {
    height: 8px;
    border-radius: 6px;
    background: #eef1f5;
    overflow: hidden;
}

.activity-card__progress-fill {
    height: 100%;
    background: linear-gradient(90deg, #4a6cf7 0%, #6d8cfb 100%);
    border-radius: 6px;
    transition: width 0.4s ease;
}

.activity-card.statut--terminee .activity-card__progress-fill {
    background: linear-gradient(90deg, #138a68 0%, #4ad8a7 100%);
}

.activity-card.statut--en-cours .activity-card__progress-fill {
    background: linear-gradient(90deg, #b8860b 0%, #f4b91f 100%);
}

/* ============================================================
   SOUS-ACTIVITÉS — repli / dépli
   ============================================================ */
.sous-activites {
    margin-top: 0.25rem;
    padding: 0.85rem 1rem;
    background: #f8fafc;
    border: 1px solid #eef1f5;
    border-radius: 12px;
    display: flex;
    flex-direction: column;
    gap: 0.55rem;
}

/* Header transformé en bouton : reset des styles natifs puis
   ré-application des styles de l'ancien <div> de titre. */
.sous-activites__head {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    width: 100%;
    padding: 0.35rem 0.5rem;
    border: none;
    border-radius: 8px;
    background: transparent;
    font-family: inherit;
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: #6b7280;
    cursor: pointer;
    transition: background .15s, color .15s;
    text-align: left;
}

.sous-activites__head:hover {
    background: #eef2f7;
    color: #1a2a6c;
}

.sous-activites__head:focus-visible {
    outline: 2px solid #4a6cf7;
    outline-offset: 2px;
}

.sous-activites__head>i.bi-diagram-3 {
    color: #8a95a8;
    font-size: 0.85rem;
    transition: color .15s;
}

.sous-activites__head:hover>i.bi-diagram-3 {
    color: #4a6cf7;
}

.sous-activites__title {
    flex: 1;
}

.sous-activites__count {
    padding: 0.1rem 0.5rem;
    border-radius: 999px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.68rem;
    font-weight: 700;
    letter-spacing: 0;
}

.sous-activites__chevron {
    color: #8a95a8;
    font-size: 0.75rem;
    transition: color .15s, transform .15s;
}

.sous-activites__head:hover .sous-activites__chevron {
    color: #4a6cf7;
}

.sous-activites__list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
}

.sous-item {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.6rem 0.75rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 10px;
    transition: border-color .15s, box-shadow .15s;
}

.sous-item:hover {
    border-color: #c6d2ff;
    box-shadow: 0 2px 8px rgba(26, 42, 108, .06);
}

.sous-item__dot {
    flex: none;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #c8d6fb;
}

.sous-item--idle .sous-item__dot {
    background: #c8d6fb;
}

.sous-item--wip .sous-item__dot {
    background: #f4b91f;
    box-shadow: 0 0 0 3px rgba(244, 185, 31, .18);
}

.sous-item--done .sous-item__dot {
    background: #4ad8a7;
    box-shadow: 0 0 0 3px rgba(74, 216, 167, .18);
}

.sous-item__infos {
    display: flex;
    flex-direction: column;
    gap: 0.1rem;
    min-width: 0;
    flex: 1;
}

.sous-item__designation {
    display: block;
    font-size: 0.85rem;
    font-weight: 500;
    color: #15233c;
    text-decoration: none;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    transition: color .12s;
}

.sous-item__designation:hover {
    color: #4a6cf7;
}

.sous-item__code {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.72rem;
    color: #8a95a8;
    text-decoration: none;
    transition: color .12s;
}

.sous-item__code:hover {
    color: #4a6cf7;
}

/* ============================================================
   PAGINATION
   ============================================================ */
.pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 1rem;
    padding: 1rem 0 0.5rem;
    flex-wrap: wrap;
}

.pagination__info {
    font-size: 0.82rem;
    color: #6b7280;
}

.pagination__info strong {
    color: #1a2a6c;
}

.pagination__sep {
    color: #c3cad5;
    margin: 0 0.35rem;
}

/* ============================================================
   MODAL AVANCEMENT
   ============================================================ */
.modal-avancement {
    display: flex;
    flex-direction: column;
    gap: 1.1rem;
}

.modal-avancement__sous {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
    padding: 0.75rem 0.9rem;
    background: #f8fafc;
    border-left: 3px solid #4a6cf7;
    border-radius: 6px;
}

.modal-avancement__code {
    font-family: ui-monospace, Menlo, monospace;
    color: #8a95a8;
    font-size: 0.78rem;
}

.modal-avancement__current {
    font-size: 0.85rem;
    color: #6b7280;
}

.modal-avancement__current strong {
    color: #1a2a6c;
    margin-left: 0.25rem;
}

.modal-avancement__field {
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
}

.modal-avancement__label {
    font-size: 0.82rem;
    font-weight: 600;
    color: #15233c;
    display: flex;
    align-items: center;
    gap: 0.35rem;
}

.modal-avancement__value {
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 2rem;
    font-weight: 800;
    color: #4a6cf7;
    text-align: center;
    line-height: 1;
    font-variant-numeric: tabular-nums;
    padding: 0.35rem 0;
}

.modal-avancement__range {
    width: 100%;
    accent-color: #4a6cf7;
}

.modal-avancement__range-bounds {
    display: flex;
    justify-content: space-between;
    font-size: 0.72rem;
    color: #8a95a8;
}

.modal-warning {
    display: flex;
    gap: 0.7rem;
    padding: 0.75rem 0.9rem;
    border-radius: 8px;
    font-size: 0.85rem;
    line-height: 1.5;
}

.modal-warning i {
    flex-shrink: 0;
    font-size: 1rem;
    margin-top: 0.1rem;
}

.modal-warning strong {
    display: block;
    margin-bottom: 0.2rem;
    font-weight: 700;
}

.modal-warning p {
    margin: 0;
}

.modal-warning--danger {
    background: #fdecea;
    border-left: 3px solid #d64545;
    color: #5c1a17;
}

.modal-warning--danger i {
    color: #d64545;
}

.modal-warning--success {
    background: #e6f4ea;
    border-left: 3px solid #1e7e34;
    color: #14532d;
}

.modal-warning--success i {
    color: #1e7e34;
}

.modal-warning--neutral {
    background: #eef1f5;
    border-left: 3px solid #9fb0c2;
    color: #384a5e;
}

.modal-warning--neutral i {
    color: #5b6b7d;
}

.required {
    color: #d64545;
}

.text-muted-custom {
    color: #8a95a8;
    font-weight: 400;
}

.modal-avancement__question {
    font-size: 1rem;
    margin: 0;
    color: #15233c;
    font-weight: 500;
}

.modal-avancement__recap {
    display: grid;
    grid-template-columns: 140px 1fr;
    gap: 0.5rem 1rem;
    margin: 0;
    padding: 0.9rem 1rem;
    background: #f8fafc;
    border-radius: 8px;
    font-size: 0.88rem;
}

.modal-avancement__recap dt {
    color: #8a95a8;
    font-weight: 600;
    font-size: 0.72rem;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    align-self: center;
}

.modal-avancement__recap dd {
    margin: 0;
    color: #15233c;
    word-break: break-word;
}

.modal-avancement__note {
    font-size: 0.78rem;
    color: #8a95a8;
    margin: 0;
    text-align: center;
    font-style: italic;
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 1199.98px) {
    .suivi-page {
        padding-left: 24px;
        padding-right: 24px;
    }
}

@media (max-width: 991.98px) {
    .summary__stats {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .filters-card__body {
        grid-template-columns: 1fr 1fr;
    }

    .filters__search {
        grid-column: 1 / -1;
    }

    .filters__dates,
    .filters__date-error {
        grid-column: 1 / -1;
    }
}

@media (max-width: 767.98px) {
    .suivi-page {
        padding: 24px 16px 20px;
    }

    .filters-card__body {
        grid-template-columns: 1fr;
    }

    .filters__select {
        min-width: 0;
    }

    .filters__dates {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 575.98px) {
    .suivi-page {
        padding: 20px 12px 16px;
    }

    .summary {
        padding: 1rem;
    }

    .summary__stats {
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 0.5rem;
    }

    .stat-tile {
        padding: 0.65rem 0.5rem;
    }

    .stat-tile__count {
        font-size: 1.35rem;
    }

    .stat-tile__label {
        font-size: 0.6rem;
    }

    .summary__progress-value {
        font-size: 1.6rem;
    }

    .filters-card__head {
        padding: 0.75rem 1rem;
    }

    .filters-card__body {
        padding: 0.9rem 1rem;
    }

    .activity-card {
        padding: 1rem 1rem 1rem 1.15rem;
    }

    .activity-card__title {
        font-size: 0.95rem;
    }

    .sous-item__designation {
        font-size: 0.8rem;
    }

    .modal-avancement__recap {
        grid-template-columns: 1fr;
    }

    .modal-avancement__recap dt {
        margin-top: 0.4rem;
    }
}

@media (prefers-reduced-motion: reduce) {

    .summary__bar-fill,
    .activity-card__progress-fill,
    .stat-tile,
    .sous-item,
    .sous-activites__chevron {
        transition: none !important;
    }

    .skeleton {
        animation: none !important;
    }
}
</style>
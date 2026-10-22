<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { BaseButton, BaseModal, BaseSelect } from '@/components/base'
import {
    autocompleteActivites,
    enregistrerAvancement,
    listerSuivi,
    optionsSuivi,
    statistiquesSuivi,
    type AutocompleteItem,
} from '@/services/suivi'
import type {
    ActiviteOptions,
    ActiviteStatistiques,
    ActiviteSuivi,
    AvancementRequest,
} from '@/types/suivi'
import ProgressCircle from '@/components/suivi/ProgressCircle.vue'

/* -------------------- état -------------------- */

const pta = ref<boolean>(true)

const stats = ref<ActiviteStatistiques | null>(null)
const statsLoading = ref(false)
const statsError = ref('')

const activites = ref<ActiviteSuivi[]>([])
const loading = ref(false)
const errorMessage = ref('')
const totalElements = ref(0)
const page = ref(0)
const size = ref(20)

const options = ref<ActiviteOptions | null>(null)

/* -------------------- modal d'avancement (2 étapes) -------------------- */

const modalOpen = ref(false)
const modalStep = ref<'edit' | 'confirm'>('edit')
const modalLoading = ref(false)
const modalError = ref('')

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
            break
        }
    }

    modalOpen.value = false
    modalStep.value = 'edit'
    sousActiviteSelectionnee.value = null
}

/* Filtres actifs */
const carteActive = ref<'all' | 'NON_COMMENCEE' | 'EN_COURS' | 'TERMINEE'>('all')
const recherche = ref('')
const idService = ref<number | 'all'>('all')
const idPriorite = ref<number | 'all'>('all')

/* Autocomplete */
const suggestions = ref<AutocompleteItem[]>([])
const suggestionsOpen = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null

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
    if (activites.value.length === 0) return 0
    const total = activites.value.reduce((sum, a) => sum + (a.avancement ?? 0), 0)
    return Math.round(total / activites.value.length)
})

const totalSousActivites = computed(() =>
    activites.value.reduce((sum, a) => sum + a.sousActivites.length, 0)
)

const filtresActifs = computed(() =>
    carteActive.value !== 'all'
    || recherche.value.trim() !== ''
    || idService.value !== 'all'
    || idPriorite.value !== 'all'
)

/* -------------------- chargements -------------------- */

function buildParams() {
    return {
        pta: pta.value,
        search: recherche.value.trim() || undefined,
        serviceId: idService.value === 'all' ? undefined : Number(idService.value),
        prioriteId: idPriorite.value === 'all' ? undefined : Number(idPriorite.value),
        statut: statutBackend.value,
    }
}

async function chargerStats(): Promise<void> {
    statsLoading.value = true
    statsError.value = ''
    const res = await statistiquesSuivi(buildParams())
    statsLoading.value = false
    if (!res.success) {
        statsError.value = res.error
        return
    }
    stats.value = res.data
}

async function chargerListe(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await listerSuivi({
        ...buildParams(),
        page: page.value,
        size: size.value,
    })
    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    activites.value = res.data.content
    totalElements.value = res.data.totalElements
}

async function chargerOptions(): Promise<void> {
    const res = await optionsSuivi()
    if (res.success) options.value = res.data
}

/* -------------------- comportements -------------------- */

function basculerPta(valeur: boolean): void {
    if (pta.value === valeur) return
    pta.value = valeur
    page.value = 0
    recharger()
}

function choisirCarte(carte: typeof carteActive.value): void {
    carteActive.value = carte
    page.value = 0
    recharger()
}

function recharger(): void {
    chargerStats()
    chargerListe()
}

function reinitialiserFiltres(): void {
    carteActive.value = 'all'
    recherche.value = ''
    idService.value = 'all'
    idPriorite.value = 'all'
    page.value = 0
    recharger()
}

function onRechercheInput(): void {
    if (searchTimer) clearTimeout(searchTimer)
    const q = recherche.value.trim()
    if (!q) {
        suggestions.value = []
        suggestionsOpen.value = false
        return
    }
    searchTimer = setTimeout(async () => {
        const res = await autocompleteActivites(q)
        if (res.success) {
            suggestions.value = res.data
            suggestionsOpen.value = true
        }
    }, 250)
}

function choisirSuggestion(item: AutocompleteItem): void {
    recherche.value = item.code
    suggestionsOpen.value = false
    page.value = 0
    recharger()
}

function fermerSuggestions(): void {
    setTimeout(() => (suggestionsOpen.value = false), 150)
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

watch([idService, idPriorite], () => {
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
                    <button
                        type="button"
                        role="tab"
                        class="scope-switch__btn"
                        :class="{ 'scope-switch__btn--active': pta }"
                        :aria-selected="pta"
                        @click="basculerPta(true)"
                    >
                        <i class="bi bi-clipboard-check"></i>
                        PTA
                    </button>
                    <button
                        type="button"
                        role="tab"
                        class="scope-switch__btn"
                        :class="{ 'scope-switch__btn--active': !pta }"
                        :aria-selected="!pta"
                        @click="basculerPta(false)"
                    >
                        <i class="bi bi-clipboard"></i>
                        NON PTA
                    </button>
                </div>
            </div>
        </header>

        <!-- ============ SYNTHÈSE ============ -->
        <section class="summary" aria-label="Synthèse du suivi">
            <!-- Tuiles statistiques — au-dessus de la progression -->
            <div class="summary__stats" role="group" aria-label="Filtres par statut">
                <button
                    type="button"
                    class="stat-tile stat-tile--total"
                    :class="{ 'stat-tile--active': carteActive === 'all' }"
                    :aria-pressed="carteActive === 'all'"
                    :disabled="statsLoading"
                    @click="choisirCarte('all')"
                >
                    <span class="stat-tile__count">{{ stats?.total ?? 0 }}</span>
                    <span class="stat-tile__label">Total</span>
                </button>

                <button
                    type="button"
                    class="stat-tile stat-tile--nc"
                    :class="{ 'stat-tile--active': carteActive === 'NON_COMMENCEE' }"
                    :aria-pressed="carteActive === 'NON_COMMENCEE'"
                    :disabled="statsLoading"
                    @click="choisirCarte('NON_COMMENCEE')"
                >
                    <span class="stat-tile__count">{{ stats?.nonCommencees ?? 0 }}</span>
                    <span class="stat-tile__label">Non commencées</span>
                </button>

                <button
                    type="button"
                    class="stat-tile stat-tile--ec"
                    :class="{ 'stat-tile--active': carteActive === 'EN_COURS' }"
                    :aria-pressed="carteActive === 'EN_COURS'"
                    :disabled="statsLoading"
                    @click="choisirCarte('EN_COURS')"
                >
                    <span class="stat-tile__count">{{ stats?.enCours ?? 0 }}</span>
                    <span class="stat-tile__label">En cours</span>
                </button>

                <button
                    type="button"
                    class="stat-tile stat-tile--te"
                    :class="{ 'stat-tile--active': carteActive === 'TERMINEE' }"
                    :aria-pressed="carteActive === 'TERMINEE'"
                    :disabled="statsLoading"
                    @click="choisirCarte('TERMINEE')"
                >
                    <span class="stat-tile__count">{{ stats?.terminees ?? 0 }}</span>
                    <span class="stat-tile__label">Terminées</span>
                </button>
            </div>

            <!-- Progression globale — sous les tuiles -->
            <div class="summary__progress">
                <div class="summary__progress-head">
                    <span class="summary__progress-label">Avancement global</span>
                    <span class="summary__progress-value">
                        {{ avancementGlobal }}<small>%</small>
                    </span>
                </div>
                <div
                    class="summary__bar"
                    role="progressbar"
                    :aria-valuenow="avancementGlobal"
                    aria-valuemin="0"
                    aria-valuemax="100"
                >
                    <div
                        class="summary__bar-fill"
                        :style="{ width: avancementGlobal + '%' }"
                    ></div>
                </div>
                <div class="summary__progress-foot">
                    {{ activites.length }} activité{{ activites.length > 1 ? 's' : '' }} affichée{{ activites.length > 1 ? 's' : '' }}
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
                <BaseButton
                    v-if="filtresActifs"
                    variant="secondary"
                    size="sm"
                    icon="bi bi-x-lg"
                    @click="reinitialiserFiltres"
                >
                    Réinitialiser
                </BaseButton>
            </header>

            <div class="filters-card__body">
                <div class="filters__search" @focusout="fermerSuggestions">
                    <i class="bi bi-search" aria-hidden="true"></i>
                    <input
                        type="text"
                        placeholder="Rechercher une activité (code, référence, désignation)…"
                        v-model="recherche"
                        aria-label="Rechercher une activité"
                        @input="onRechercheInput"
                        @focus="suggestions.length && (suggestionsOpen = true)"
                        @keydown.enter="page = 0; recharger()"
                    />
                    <ul v-if="suggestionsOpen && suggestions.length" class="filters__suggestions" role="listbox">
                        <li
                            v-for="s in suggestions"
                            :key="s.id"
                            role="option"
                            class="filters__suggestion"
                            @mousedown.prevent="choisirSuggestion(s)"
                        >
                            <span class="filters__suggestion-code">{{ s.code }}</span>
                            <span class="filters__suggestion-label">{{ s.libelle }}</span>
                            <span v-if="s.libelleSecondaire" class="filters__suggestion-secondary">
                                {{ s.libelleSecondaire }}
                            </span>
                        </li>
                    </ul>
                </div>

                <div v-if="(options?.services?.length ?? 0) > 0" class="filters__select">
                    <BaseSelect
                        v-model="idService"
                        size="sm"
                        placeholder="Tous les services"
                        :options="serviceOptions"
                    />
                </div>

                <div class="filters__select">
                    <BaseSelect
                        v-model="idPriorite"
                        size="sm"
                        placeholder="Toutes les priorités"
                        :options="prioriteOptions"
                    />
                </div>
            </div>
        </section>

        <!-- ============ MESSAGES ============ -->
        <div v-if="statsError" class="alert alert-danger mb-0" role="alert">
            {{ statsError }}
        </div>

        <div v-if="errorMessage" class="alert alert-danger mb-0 d-flex justify-content-between align-items-center" role="alert">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="recharger">Réessayer</BaseButton>
        </div>

        <!-- ============ CHARGEMENT ============ -->
        <div v-else-if="loading" class="loading-state">
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
            <BaseButton
                v-if="filtresActifs"
                variant="secondary"
                size="sm"
                @click="reinitialiserFiltres"
            >
                Réinitialiser les filtres
            </BaseButton>
        </div>

        <!-- ============ LISTE DES ACTIVITÉS ============ -->
        <div v-else class="activities">
            <article
                v-for="a in activites"
                :key="a.id"
                class="activity-card"
                :class="classeStatut(a)"
            >
                <div class="activity-card__head">
                    <div class="activity-card__ids">
                        <span class="activity-card__code">{{ a.code }}</span>
                        <span v-if="a.reference" class="activity-card__ref">{{ a.reference }}</span>
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

                <h3 class="activity-card__title">{{ a.designation }}</h3>

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
                    <div
                        class="activity-card__progress-bar"
                        role="progressbar"
                        :aria-valuenow="a.avancement"
                        aria-valuemin="0"
                        aria-valuemax="100"
                    >
                        <div
                            class="activity-card__progress-fill"
                            :style="{ width: a.avancement + '%' }"
                        ></div>
                    </div>
                </div>

                <div v-if="a.sousActivites.length" class="sous-activites">
                    <div class="sous-activites__head">
                        <i class="bi bi-diagram-3" aria-hidden="true"></i>
                        <span class="sous-activites__title">Sous-activités</span>
                        <span class="sous-activites__count">{{ a.sousActivites.length }}</span>
                    </div>
                    <ul class="sous-activites__list">
                        <li
                            v-for="s in a.sousActivites"
                            :key="s.id"
                            class="sous-item"
                            :class="sousStatutClass(s.avancement ?? 0)"
                        >
                            <span class="sous-item__dot" aria-hidden="true"></span>
                            <div class="sous-item__infos">
                                <span class="sous-item__designation">{{ s.designation }}</span>
                                <span class="sous-item__code">{{ s.code }}</span>
                            </div>
                            <ProgressCircle
                                :value="s.avancement ?? 0"
                                label="Modifier l'avancement"
                                @click="ouvrirModalAvancement({
                                    id: s.id,
                                    code: s.code,
                                    designation: s.designation,
                                    avancement: s.avancement ?? 0,
                                })"
                            />
                        </li>
                    </ul>
                </div>
            </article>
        </div>

        <!-- ============ PAGINATION ============ -->
        <nav v-if="totalElements > size" class="pagination" aria-label="Pagination">
            <BaseButton
                size="sm"
                variant="secondary"
                :disabled="page === 0"
                @click="page--; chargerListe()"
            >
                <i class="bi bi-chevron-left"></i>
                Précédent
            </BaseButton>
            <span class="pagination__info">
                Page <strong>{{ page + 1 }}</strong> sur {{ Math.ceil(totalElements / size) }}
                <span class="pagination__sep">•</span>
                {{ totalElements }} résultat{{ totalElements > 1 ? 's' : '' }}
            </span>
            <BaseButton
                size="sm"
                variant="secondary"
                :disabled="(page + 1) * size >= totalElements"
                @click="page++; chargerListe()"
            >
                Suivant
                <i class="bi bi-chevron-right"></i>
            </BaseButton>
        </nav>
    </div>

    <!-- ============ MODAL AVANCEMENT ============ -->
    <BaseModal
        v-model="modalOpen"
        :title="modalStep === 'edit' ? 'Modifier l\'avancement' : 'Confirmer la modification'"
        size="md"
        :loading="modalLoading"
        :closeOnOutside="!modalLoading"
        @update:modelValue="(v: boolean) => !v && fermerModalAvancement()"
    >
        <div v-if="modalStep === 'edit' && sousActiviteSelectionnee" class="modal-avancement">
            <div class="modal-avancement__sous">
                <strong>{{ sousActiviteSelectionnee.designation }}</strong>
                <span class="modal-avancement__code">{{ sousActiviteSelectionnee.code }}</span>
            </div>

            <div v-if="modalError" class="alert alert-danger mb-0" role="alert">
                {{ modalError }}
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

            <div v-else-if="sens === 'up' && Math.round(modalValeur) >= 100" class="modal-warning modal-warning--success" role="note">
                <i class="bi bi-check-circle-fill"></i>
                <div>
                    <strong>Sous-activité terminée</strong>
                    <p>En passant à 100 %, la sous-activité sera considérée comme terminée.</p>
                </div>
            </div>

            <div v-else-if="sens === 'same'" class="modal-warning modal-warning--neutral" role="note">
                <i class="bi bi-info-circle-fill"></i>
                <div><p>Aucune modification de l'avancement.</p></div>
            </div>

            <div class="modal-avancement__current">
                Avancement actuel&nbsp;: <strong>{{ valeurActuelle }} %</strong>
            </div>

            <div class="modal-avancement__field">
                <label class="modal-avancement__label">Nouvel avancement</label>
                <div class="modal-avancement__value">{{ Math.round(modalValeur) }} %</div>
                <input
                    type="range"
                    min="0"
                    max="100"
                    step="1"
                    v-model.number="modalValeur"
                    class="modal-avancement__range"
                    :disabled="modalLoading"
                />
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
                <textarea
                    v-model="modalCommentaire"
                    class="form-control"
                    rows="3"
                    maxlength="2000"
                    :placeholder="commentaireObligatoire ? 'Justifiez la diminution…' : 'Précisez l\'état d\'avancement…'"
                    :disabled="modalLoading"
                ></textarea>
            </div>
        </div>

        <div v-else-if="modalStep === 'confirm' && sousActiviteSelectionnee" class="modal-avancement modal-avancement--confirm">
            <div v-if="sens === 'reopen'" class="modal-warning modal-warning--danger" role="note">
                <i class="bi bi-exclamation-triangle-fill"></i>
                <div>
                    <strong>Réouverture de la sous-activité</strong>
                    <p>L'avancement va diminuer et la sous-activité repassera d'un état terminé vers un état en cours.</p>
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
                    <BaseButton
                        :loading="modalLoading"
                        :variant="commentaireObligatoire ? 'danger' : 'primary'"
                        @click="confirmerEnregistrement"
                    >
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
.scope-switch__btn:hover:not(.scope-switch__btn--active) i { color: #4a6cf7; }

.scope-switch__btn--active {
    background: linear-gradient(135deg, #1a2a6c 0%, #4a6cf7 100%);
    color: #fff;
    box-shadow: 0 4px 12px rgba(26, 42, 108, .25);
}
.scope-switch__btn--active i { color: #ffd04a; }

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

/* Tuiles alignées en 4 colonnes sur toute la largeur */
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

.stat-tile--nc .stat-tile__count { color: #5b6b7d; }
.stat-tile--ec .stat-tile__count { color: #b8860b; }
.stat-tile--te .stat-tile__count { color: #1e7e34; }

/* Progression globale — sous les tuiles */
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
   FILTRES — carte avec titre
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

/* --- Recherche --- */
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
.filters__search input::placeholder {
    color: #9aa4b8;
}

.filters__suggestions {
    position: absolute;
    top: calc(100% + 4px);
    left: 0;
    right: 0;
    z-index: 20;
    list-style: none;
    margin: 0;
    padding: 0.3rem 0;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 10px;
    box-shadow: 0 12px 28px rgba(26, 42, 108, .12);
    max-height: 260px;
    overflow-y: auto;
}

.filters__suggestion {
    display: grid;
    grid-template-columns: minmax(90px, auto) 1fr auto;
    gap: 0.55rem;
    padding: 0.5rem 0.85rem;
    cursor: pointer;
    font-size: 0.83rem;
    align-items: baseline;
}
.filters__suggestion:hover {
    background: #eef5f8;
}

.filters__suggestion-code {
    font-family: ui-monospace, Menlo, monospace;
    color: #3b63d9;
    font-size: 0.78rem;
    font-weight: 600;
}
.filters__suggestion-label {
    color: #15233c;
}
.filters__suggestion-secondary {
    color: #8a95a8;
    font-size: 0.75rem;
}

.filters__select {
    min-width: 190px;
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
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: shimmer 1.4s infinite;
    border-radius: 6px;
}

.skeleton--line { height: 12px; }
.skeleton--short { width: 30%; }
.skeleton--medium { width: 60%; }
.skeleton--bar { height: 8px; border-radius: 4px; }

@keyframes shimmer {
    from { background-position: 200% 0; }
    to { background-position: -200% 0; }
}

/* ============================================================
   CARTES ACTIVITÉS
   ============================================================ */
.activities {
    display: flex;
    flex-direction: column;
    gap: 1rem;
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

/* Bandeau de statut à gauche */
.activity-card::before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    bottom: 0;
    width: 4px;
    background: #c8d6fb;
}
.activity-card.statut--non-commencee::before { background: #8a95a8; }
.activity-card.statut--en-cours::before { background: #f4b91f; }
.activity-card.statut--terminee::before { background: #4ad8a7; }
.activity-card.statut--autre::before { background: #c8d6fb; }

/* --- En-tête --- */
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
}

.activity-card__ref {
    font-size: 0.76rem;
    color: #8a95a8;
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
    background: #eef1f5; color: #5b6b7d; border: 1px solid #dfe5ec;
}
.badge-statut.statut--en-cours {
    background: #fdf3e2; color: #9a6409; border: 1px solid #f4e2c2;
}
.badge-statut.statut--terminee {
    background: #e6f4ea; color: #1e7e34; border: 1px solid #c8e5cf;
}
.badge-statut.statut--autre {
    background: #eef1f5; color: #5b6b7d; border: 1px solid #dfe5ec;
}

.badge-prio.prio--critique {
    background: #fdecea; color: #a61b16; border: 1px solid #f6cfcc;
}
.badge-prio.prio--haute {
    background: #fdf3e2; color: #9a6409; border: 1px solid #f4e2c2;
}
.badge-prio.prio--normale {
    background: #e4f0fb; color: #135d95; border: 1px solid #cde2f6;
}
.badge-prio.prio--faible,
.badge-prio.prio--autre {
    background: #eef1f5; color: #5b6b7d; border: 1px solid #dfe5ec;
}

/* --- Titre --- */
.activity-card__title {
    margin: 0;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 1.05rem;
    font-weight: 700;
    color: #15233c;
    line-height: 1.35;
    letter-spacing: -0.01em;
}

/* --- Meta --- */
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

/* --- Barre d'avancement --- */
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
   SOUS-ACTIVITÉS
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

.sous-activites__head {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: #6b7280;
}

.sous-activites__head i {
    color: #8a95a8;
    font-size: 0.85rem;
}

.sous-activites__title { flex: 1; }

.sous-activites__count {
    padding: 0.1rem 0.5rem;
    border-radius: 999px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.68rem;
    font-weight: 700;
    letter-spacing: 0;
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
.sous-item--idle .sous-item__dot { background: #c8d6fb; }
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
    font-size: 0.85rem;
    font-weight: 500;
    color: #15233c;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.sous-item__code {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.72rem;
    color: #8a95a8;
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
.pagination__info strong { color: #1a2a6c; }

.pagination__sep {
    color: #c3cad5;
    margin: 0 0.35rem;
}

/* ============================================================
   MODAL — améliorations visuelles
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
.modal-warning p { margin: 0; }

.modal-warning--danger {
    background: #fdecea; border-left: 3px solid #d64545; color: #5c1a17;
}
.modal-warning--danger i { color: #d64545; }

.modal-warning--success {
    background: #e6f4ea; border-left: 3px solid #1e7e34; color: #14532d;
}
.modal-warning--success i { color: #1e7e34; }

.modal-warning--neutral {
    background: #eef1f5; border-left: 3px solid #9fb0c2; color: #384a5e;
}
.modal-warning--neutral i { color: #5b6b7d; }

.required { color: #d64545; }
.text-muted-custom { color: #8a95a8; font-weight: 400; }

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
    .suivi-page { padding-left: 24px; padding-right: 24px; }
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
}

@media (max-width: 767.98px) {
    .suivi-page { padding: 24px 16px 20px; }
    .filters-card__body {
        grid-template-columns: 1fr;
    }
    .filters__select { min-width: 0; }
}

@media (max-width: 575.98px) {
    .suivi-page { padding: 20px 12px 16px; }
    .summary { padding: 1rem; }
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
    .summary__progress-value { font-size: 1.6rem; }
    .filters-card__head { padding: 0.75rem 1rem; }
    .filters-card__body { padding: 0.9rem 1rem; }
    .activity-card { padding: 1rem 1rem 1rem 1.15rem; }
    .activity-card__title { font-size: 0.95rem; }
    .sous-item__designation { font-size: 0.8rem; }
    .modal-avancement__recap { grid-template-columns: 1fr; }
    .modal-avancement__recap dt { margin-top: 0.4rem; }
}

@media (prefers-reduced-motion: reduce) {
    .summary__bar-fill,
    .activity-card__progress-fill,
    .stat-tile,
    .sous-item {
        transition: none !important;
    }
    .skeleton {
        animation: none !important;
    }
}
</style>
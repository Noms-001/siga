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

/** Valeur au moment de l'ouverture — immuable tant que le modal est ouvert. */
const valeurActuelle = ref(0)
/** Valeur en cours d'édition dans le modal. */
const modalValeur = ref(0)
const modalCommentaire = ref('')

/**
 * Sens de la modification. Pilote :
 *   - l'affichage des avertissements dans le modal ;
 *   - le caractère obligatoire du commentaire ;
 *   - le libellé du bouton Enregistrer.
 *
 * `reopen` est un sous-cas de `down` : sémantiquement c'est une diminution,
 * mais la communication à l'utilisateur doit être plus explicite car elle
 * change le statut d'une sous-activité terminée vers "en cours".
 */
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

/**
 * Le bouton Enregistrer du modal d'édition est actif uniquement si :
 *   - la valeur a changé ;
 *   - si diminution ou réouverture : le commentaire est rempli.
 *
 * L'utilisateur voit donc immédiatement pourquoi il ne peut pas continuer,
 * plutôt que de cliquer et recevoir une erreur serveur.
 */
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

/**
 * Passe à l'étape de confirmation. N'appelle PAS le backend : c'est un
 * changement d'écran, rien d'autre. Le POST n'a lieu qu'au clic sur
 * Confirmer — c'est la règle explicite : "Le backend ne doit être appelé
 * qu'après le clic sur Confirmer".
 */
function passerEnConfirmation(): void {
    modalError.value = ''
    if (!peutPasserEnConfirmation.value) return
    modalStep.value = 'confirm'
}

function revenirEnEdition(): void {
    modalStep.value = 'edit'
}

/**
 * Seule fonction qui déclenche la requête. Séparée de la validation et
 * de l'étape de confirmation pour qu'aucun autre chemin ne puisse appeler
 * le backend par accident.
 */
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
        // Échec : on revient à l'édition pour que l'utilisateur corrige.
        // Le message reste dans `modalError` et s'affiche dans l'étape
        // d'édition, là où les champs concernés sont visibles.
        modalStep.value = 'edit'
        modalError.value = res.error
        return
    }

    // Mise à jour locale de la liste plate — pas de rechargement complet.
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

/** Statut à passer au backend selon la carte active. */
const statutBackend = computed(() => {
    return carteActive.value === 'all' ? undefined : carteActive.value
})

/** Avancement global : moyenne des avancements des activités affichées. */
const avancementGlobal = computed(() => {
    if (activites.value.length === 0) return 0
    const total = activites.value.reduce((sum, a) => sum + (a.avancement ?? 0), 0)
    return Math.round(total / activites.value.length)
})

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

/** Autocomplete : debounce de 250ms pour ne pas spammer le backend. */
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
    // Léger délai pour laisser le clic sur une suggestion s'exécuter.
    setTimeout(() => (suggestionsOpen.value = false), 150)
}

/* -------------------- formats -------------------- */

/** Date affichée : réelle si présente, sinon prévue. */
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
    if (code === 'TERMINEE') return 'badge-statut--terminee'
    if (code === 'EN_COURS') return 'badge-statut--en-cours'
    if (code === 'NON_COMMENCEE') return 'badge-statut--non-commencee'
    return 'badge-statut--autre'
}

function classePriorite(a: ActiviteSuivi): string {
    const code = (a.priorite?.code ?? '').toUpperCase()
    if (code === 'CRITIQUE') return 'badge-prio--critique'
    if (code === 'HAUTE') return 'badge-prio--haute'
    if (code === 'NORMALE') return 'badge-prio--normale'
    if (code === 'FAIBLE') return 'badge-prio--faible'
    return 'badge-prio--autre'
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
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">Suivi des activités</h1>
                <p class="bo-page__subtitle">
                    Suivi et avancement des activités du département TSS
                </p>
            </div>
        </header>

        <!-- Bascule PTA / NON PTA -->
        <div class="suivi-switch" role="tablist">
            <button type="button" role="tab" class="suivi-switch__btn" :class="{ 'suivi-switch__btn--active': pta }"
                :aria-selected="pta" @click="basculerPta(true)">
                PTA
            </button>
            <button type="button" role="tab" class="suivi-switch__btn" :class="{ 'suivi-switch__btn--active': !pta }"
                :aria-selected="!pta" @click="basculerPta(false)">
                NON PTA
            </button>
        </div>

        <!-- Avancement global -->
        <section class="bo-section">
            <header class="bo-section__head">
                <h2 class="bo-section__title">Avancement global</h2>
                <span class="bo-section__count">{{ avancementGlobal }} %</span>
            </header>
            <div class="progress suivi-progress">
                <div class="progress-bar" role="progressbar" :style="{ width: avancementGlobal + '%' }"
                    :aria-valuenow="avancementGlobal" aria-valuemin="0" aria-valuemax="100"></div>
            </div>
        </section>

        <!-- Cartes statistiques -->
        <div v-if="statsError" class="alert alert-danger mb-0" role="alert">
            {{ statsError }}
        </div>

        <div v-else class="suivi-cards">
            <button type="button" class="suivi-card" :class="{ 'suivi-card--active': carteActive === 'all' }"
                :disabled="statsLoading" @click="choisirCarte('all')">
                <span class="suivi-card__value">{{ stats?.total ?? 0 }}</span>
                <span class="suivi-card__label">TOTAL</span>
                <span class="suivi-card__hint">Toutes les activités</span>
            </button>

            <button type="button" class="suivi-card suivi-card--non-commencee"
                :class="{ 'suivi-card--active': carteActive === 'NON_COMMENCEE' }" :disabled="statsLoading"
                @click="choisirCarte('NON_COMMENCEE')">
                <span class="suivi-card__value">{{ stats?.nonCommencees ?? 0 }}</span>
                <span class="suivi-card__label">NON COMMENCÉES</span>
            </button>

            <button type="button" class="suivi-card suivi-card--en-cours"
                :class="{ 'suivi-card--active': carteActive === 'EN_COURS' }" :disabled="statsLoading"
                @click="choisirCarte('EN_COURS')">
                <span class="suivi-card__value">{{ stats?.enCours ?? 0 }}</span>
                <span class="suivi-card__label">EN COURS</span>
            </button>

            <button type="button" class="suivi-card suivi-card--terminee"
                :class="{ 'suivi-card--active': carteActive === 'TERMINEE' }" :disabled="statsLoading"
                @click="choisirCarte('TERMINEE')">
                <span class="suivi-card__value">{{ stats?.terminees ?? 0 }}</span>
                <span class="suivi-card__label">TERMINÉES</span>
            </button>
        </div>

        <!-- Filtres -->
        <div class="suivi-filtres">
            <div class="suivi-search" @focusout="fermerSuggestions">
                <i class="bi bi-search"></i>
                <input type="text" placeholder="Rechercher une activité (code, référence, désignation)…"
                    v-model="recherche" @input="onRechercheInput"
                    @focus="suggestions.length && (suggestionsOpen = true)" @keydown.enter="page = 0; recharger()" />

                <ul v-if="suggestionsOpen && suggestions.length" class="suivi-search__list">
                    <li v-for="s in suggestions" :key="s.id" class="suivi-search__item"
                        @mousedown.prevent="choisirSuggestion(s)">
                        <span class="suivi-search__code">{{ s.code }}</span>
                        <span class="suivi-search__label">{{ s.libelle }}</span>
                        <span v-if="s.libelleSecondaire" class="suivi-search__secondary">
                            {{ s.libelleSecondaire }}
                        </span>
                    </li>
                </ul>
            </div>

            <BaseSelect v-if="(options?.services?.length ?? 0) > 0" v-model="idService" label="Service" size="sm"
                placeholder="Tous les services" :options="serviceOptions" />

            <BaseSelect v-model="idPriorite" label="Priorité" size="sm" placeholder="Toutes les priorités"
                :options="prioriteOptions" />

            <BaseButton variant="secondary" size="sm" icon="bi bi-arrow-counterclockwise" :disabled="carteActive === 'all' && !recherche
                && idService === 'all' && idPriorite === 'all'" @click="() => {
                    carteActive = 'all'
                    recherche = ''
                    idService = 'all'
                    idPriorite = 'all'
                    page = 0
                    recharger()
                }">
                Réinitialiser
            </BaseButton>
        </div>

        <!-- Liste -->
        <div v-if="errorMessage" class="alert alert-danger mb-0 d-flex justify-content-between align-items-center"
            role="alert">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="recharger">Réessayer</BaseButton>
        </div>

        <div v-else-if="loading" class="suivi-loading">
            <div v-for="i in 4" :key="i" class="skeleton suivi-skeleton"></div>
        </div>

        <div v-else-if="activites.length === 0" class="bo-empty">
            Aucune activité ne correspond aux filtres.
        </div>

        <div v-else class="suivi-liste">
            <article v-for="a in activites" :key="a.id" class="activite-card" :class="classeStatut(a)">
                <header class="activite-card__head">
                    <div class="activite-card__ids">
                        <span class="activite-card__code">{{ a.code }}</span>
                        <span v-if="a.reference" class="activite-card__ref">
                            {{ a.reference }}
                        </span>
                    </div>
                    <div class="activite-card__badges">
                        <span v-if="a.statut" class="badge-statut" :class="classeStatut(a)">
                            {{ a.statut.libelle }}
                        </span>
                        <span v-if="a.priorite" class="badge-prio" :class="classePriorite(a)">
                            {{ a.priorite.libelle }}
                        </span>
                    </div>
                </header>

                <h3 class="activite-card__titre">{{ a.designation }}</h3>

                <div class="activite-card__meta">
                    <div class="activite-card__meta-item">
                        <span class="activite-card__meta-label">Responsable</span>
                        <span class="activite-card__meta-value">
                            <template v-if="a.responsable">
                                {{ a.responsable.prenom }} {{ a.responsable.nom }}
                            </template>
                            <template v-else>—</template>
                        </span>
                    </div>
                    <div class="activite-card__meta-item">
                        <span class="activite-card__meta-label">Début</span>
                        <span class="activite-card__meta-value">{{ dateDebut(a) }}</span>
                    </div>
                    <div class="activite-card__meta-item">
                        <span class="activite-card__meta-label">Fin</span>
                        <span class="activite-card__meta-value">{{ dateFin(a) }}</span>
                    </div>
                    <div v-if="a.service" class="activite-card__meta-item">
                        <span class="activite-card__meta-label">Service</span>
                        <span class="activite-card__meta-value">{{ a.service.libelle }}</span>
                    </div>
                </div>

                <div class="activite-card__avancement">
                    <div class="activite-card__avancement-head">
                        <span>Avancement</span>
                        <strong>{{ a.avancement }} %</strong>
                    </div>
                    <div class="progress">
                        <div class="progress-bar" role="progressbar" :style="{ width: a.avancement + '%' }"
                            :aria-valuenow="a.avancement" aria-valuemin="0" aria-valuemax="100"></div>
                    </div>
                </div>

                <div v-if="a.sousActivites.length" class="sous-liste">
                    <div class="sous-liste__titre">Sous-activités</div>
                    <ul class="sous-liste__items">
                        <li v-for="s in a.sousActivites" :key="s.id" class="sous-liste__item">
                            <div class="sous-liste__infos">
                                <span class="sous-liste__designation">{{ s.designation }}</span>
                                <span class="sous-liste__code">{{ s.code }}</span>
                            </div>
                            <div>
                                <ProgressCircle :value="s.avancement ?? 0" label="Modifier l'avancement" @click="ouvrirModalAvancement({
                                    id: s.id,
                                    code: s.code,
                                    designation: s.designation,
                                    avancement: s.avancement ?? 0,
                                })" />
                            </div>
                        </li>
                    </ul>
                </div>
            </article>
        </div>

        <!-- Pagination simple -->
        <div v-if="totalElements > size" class="suivi-pagination">
            <BaseButton size="sm" variant="secondary" :disabled="page === 0" @click="page--; chargerListe()">
                Précédent
            </BaseButton>
            <span class="suivi-pagination__info">
                Page {{ page + 1 }} — {{ totalElements }} résultat(s)
            </span>
            <BaseButton size="sm" variant="secondary" :disabled="(page + 1) * size >= totalElements"
                @click="page++; chargerListe()">
                Suivant
            </BaseButton>
        </div>
    </div>

    <BaseModal v-model="modalOpen"
        :title="modalStep === 'edit' ? 'Modifier l\'avancement' : 'Confirmer la modification'" size="md"
        :loading="modalLoading" :closeOnOutside="!modalLoading"
        @update:modelValue="(v: boolean) => !v && fermerModalAvancement()">

        <!-- ÉTAPE 1 : édition -->
        <div v-if="modalStep === 'edit' && sousActiviteSelectionnee" class="modal-avancement">
            <div class="modal-avancement__sous">
                <strong>{{ sousActiviteSelectionnee.designation }}</strong>
                <span class="modal-avancement__code">{{ sousActiviteSelectionnee.code }}</span>
            </div>

            <div v-if="modalError" class="alert alert-danger mb-0" role="alert">
                {{ modalError }}
            </div>

            <!-- Bandeau directionnel -->
            <div v-if="sens === 'down'" class="modal-warning modal-warning--danger" role="note">
                <i class="bi bi-exclamation-triangle-fill"></i>
                <div>
                    <strong>Diminution de l'avancement</strong>
                    <p>
                        L'avancement actuel est de {{ valeurActuelle }} %.<br>
                        La nouvelle valeur est de {{ Math.round(modalValeur) }} %.<br>
                        Une justification est obligatoire.
                    </p>
                </div>
            </div>

            <div v-else-if="sens === 'reopen'" class="modal-warning modal-warning--danger" role="note">
                <i class="bi bi-exclamation-triangle-fill"></i>
                <div>
                    <strong>Réouverture de la sous-activité</strong>
                    <p>
                        Cette sous-activité est actuellement terminée (100 %).<br>
                        Vous allez ramener son avancement à {{ Math.round(modalValeur) }} %.<br>
                        Une justification est obligatoire.
                    </p>
                </div>
            </div>

            <div v-else-if="sens === 'up' && Math.round(modalValeur) >= 100"
                class="modal-warning modal-warning--success" role="note">
                <i class="bi bi-check-circle-fill"></i>
                <div>
                    <strong>Sous-activité terminée</strong>
                    <p>
                        En passant à 100 %, la sous-activité sera considérée
                        comme terminée.
                    </p>
                </div>
            </div>

            <div v-else-if="sens === 'same'" class="modal-warning modal-warning--neutral" role="note">
                <i class="bi bi-info-circle-fill"></i>
                <div>
                    <p>Aucune modification de l'avancement.</p>
                </div>
            </div>

            <!-- Valeur actuelle -->
            <div class="modal-avancement__current">
                Avancement actuel :
                <strong>{{ valeurActuelle }} %</strong>
            </div>

            <!-- Nouvelle valeur -->
            <div class="modal-avancement__field">
                <label class="modal-avancement__label">Nouvel avancement</label>
                <div class="modal-avancement__value">{{ Math.round(modalValeur) }} %</div>
                <input type="range" min="0" max="100" step="1" v-model.number="modalValeur"
                    class="modal-avancement__range" :disabled="modalLoading" />
                <div class="modal-avancement__range-bounds">
                    <span>0</span><span>100</span>
                </div>
            </div>

            <!-- Commentaire -->
            <div class="modal-avancement__field">
                <label class="modal-avancement__label">
                    Commentaire
                    <span v-if="commentaireObligatoire" class="required">*</span>
                    <span v-else class="text-muted-custom">(facultatif)</span>
                </label>
                <textarea v-model="modalCommentaire" class="form-control" rows="3" maxlength="2000" :placeholder="commentaireObligatoire
                    ? 'Justifiez la diminution…'
                    : 'Précisez l\'état d\'avancement…'" :disabled="modalLoading"></textarea>
            </div>
        </div>

        <!-- ÉTAPE 2 : confirmation -->
        <div v-else-if="modalStep === 'confirm' && sousActiviteSelectionnee"
            class="modal-avancement modal-avancement--confirm">

            <div v-if="sens === 'reopen'" class="modal-warning modal-warning--danger" role="note">
                <i class="bi bi-exclamation-triangle-fill"></i>
                <div>
                    <strong>Réouverture de la sous-activité</strong>
                    <p>
                        L'avancement va diminuer et la sous-activité repassera
                        d'un état terminé vers un état en cours.
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

            <p class="modal-avancement__question">
                Voulez-vous enregistrer cet avancement ?
            </p>

            <dl class="modal-avancement__recap">
                <dt>Sous-activité</dt>
                <dd>{{ sousActiviteSelectionnee.designation }}</dd>

                <dt>Avancement</dt>
                <dd>
                    <strong>{{ valeurActuelle }} % → {{ Math.round(modalValeur) }} %</strong>
                </dd>

                <dt v-if="modalCommentaire.trim()">Commentaire</dt>
                <dd v-if="modalCommentaire.trim()">{{ modalCommentaire.trim() }}</dd>
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
.bo-page {
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.bo-page__header {
    display: flex;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 1rem;
}

.bo-page__title {
    font-size: 1.4rem;
    font-weight: 700;
    color: var(--dts-navy);
    margin: 0;
}

.bo-page__subtitle {
    color: var(--dts-muted);
    margin: 0;
    font-size: 0.85rem;
}

/* --- bascule PTA --- */

.suivi-switch {
    display: inline-flex;
    gap: 0.25rem;
    padding: 0.25rem;
    background: var(--dts-bg);
    border: 1px solid var(--dts-border);
    border-radius: 20px;
    align-self: flex-start;
}

.suivi-switch__btn {
    padding: 0.35rem 1.2rem;
    border: none;
    background: transparent;
    border-radius: 16px;
    font-size: 0.82rem;
    font-weight: 600;
    color: var(--dts-muted);
    cursor: pointer;
    transition: background-color 0.15s ease, color 0.15s ease;
}

.suivi-switch__btn--active {
    background: var(--dts-navy);
    color: #fff;
}

/* --- avancement global --- */

.bo-section {
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    padding: 1rem 1.25rem;
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
}

.bo-section__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.bo-section__title {
    margin: 0;
    font-size: 0.95rem;
    font-weight: 700;
    color: var(--dts-navy);
}

.bo-section__count {
    font-size: 0.9rem;
    font-weight: 700;
    color: var(--dts-blue);
}

.suivi-progress {
    height: 12px;
    border-radius: 20px;
    background: var(--dts-bg);
}

.suivi-progress .progress-bar {
    background: var(--dts-blue);
    border-radius: 20px;
}

/* --- 4 cartes --- */

.suivi-cards {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 1rem;
}

.suivi-card {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
    padding: 1rem 1.15rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    text-align: left;
    cursor: pointer;
    transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.suivi-card:hover:not(:disabled) {
    border-color: #9dc2e0;
    transform: translateY(-1px);
}

.suivi-card--active {
    border-color: var(--dts-blue);
    box-shadow: 0 0 0 3px rgba(31, 111, 178, 0.15);
}

.suivi-card:disabled {
    cursor: not-allowed;
    opacity: 0.7;
}

.suivi-card__value {
    font-size: 1.75rem;
    font-weight: 700;
    color: var(--dts-navy);
    line-height: 1;
}

.suivi-card__label {
    font-size: 0.7rem;
    font-weight: 700;
    letter-spacing: 0.05em;
    color: var(--dts-muted);
    text-transform: uppercase;
}

.suivi-card__hint {
    font-size: 0.72rem;
    color: var(--dts-muted);
}

.suivi-card--non-commencee .suivi-card__value {
    color: #5b6b7d;
}

.suivi-card--en-cours .suivi-card__value {
    color: #9a6409;
}

.suivi-card--terminee .suivi-card__value {
    color: #1e7e34;
}

/* --- filtres --- */

.suivi-filtres {
    display: grid;
    grid-template-columns: minmax(280px, 1fr) minmax(180px, 220px) minmax(180px, 220px) auto;
    gap: 1rem;
    align-items: end;
}

.suivi-search {
    position: relative;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0 0.75rem;
    min-height: 42px;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius-sm);
}

.suivi-search i {
    color: var(--dts-muted);
    font-size: 0.85rem;
}

.suivi-search input {
    flex: 1;
    border: none;
    outline: none;
    background: transparent;
    font-size: 0.85rem;
    color: var(--dts-text);
    padding: 0.6rem 0;
}

.suivi-search__list {
    position: absolute;
    top: calc(100% + 4px);
    left: 0;
    right: 0;
    z-index: 20;
    list-style: none;
    margin: 0;
    padding: 0.25rem 0;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius-sm);
    box-shadow: 0 8px 24px rgba(13, 43, 78, 0.12);
    max-height: 240px;
    overflow-y: auto;
}

.suivi-search__item {
    display: grid;
    grid-template-columns: minmax(90px, auto) 1fr auto;
    gap: 0.5rem;
    padding: 0.45rem 0.75rem;
    cursor: pointer;
    font-size: 0.82rem;
    align-items: baseline;
}

.suivi-search__item:hover {
    background: var(--dts-blue-50);
}

.suivi-search__code {
    font-family: ui-monospace, Menlo, monospace;
    color: var(--dts-blue);
    font-size: 0.78rem;
}

.suivi-search__label {
    color: var(--dts-text);
}

.suivi-search__secondary {
    color: var(--dts-muted);
    font-size: 0.75rem;
}

/* --- liste activités --- */

.suivi-liste {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.activite-card {
    position: relative;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-left: 4px solid var(--dts-border-2);
    border-radius: var(--dts-radius);
    padding: 1.1rem 1.25rem;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.activite-card.badge-statut--non-commencee {
    border-left-color: #9fb0c2;
}

.activite-card.badge-statut--en-cours {
    border-left-color: #d98324;
}

.activite-card.badge-statut--terminee {
    border-left-color: #1e7e34;
}

.activite-card__head {
    display: flex;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
}

.activite-card__ids {
    display: flex;
    align-items: baseline;
    gap: 0.6rem;
    flex-wrap: wrap;
}

.activite-card__code {
    font-family: ui-monospace, Menlo, monospace;
    font-weight: 700;
    color: var(--dts-navy);
}

.activite-card__ref {
    font-size: 0.78rem;
    color: var(--dts-muted);
}

.activite-card__badges {
    display: flex;
    gap: 0.4rem;
    flex-wrap: wrap;
}

.badge-statut,
.badge-prio {
    display: inline-block;
    padding: 0.2rem 0.65rem;
    border-radius: 20px;
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.03em;
}

.badge-statut--non-commencee {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
}

.badge-statut--en-cours {
    background: #fdf3e2;
    color: #9a6409;
    border: 1px solid #f4e2c2;
}

.badge-statut--terminee {
    background: #e6f4ea;
    color: #1e7e34;
    border: 1px solid #c8e5cf;
}

.badge-statut--autre {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
}

.badge-prio--critique {
    background: #fdecea;
    color: #a61b16;
    border: 1px solid #f6cfcc;
}

.badge-prio--haute {
    background: #fdf3e2;
    color: #9a6409;
    border: 1px solid #f4e2c2;
}

.badge-prio--normale {
    background: #e4f0fb;
    color: #135d95;
    border: 1px solid #cde2f6;
}

.badge-prio--faible {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
}

.badge-prio--autre {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
}

.activite-card__titre {
    margin: 0;
    font-size: 1.05rem;
    font-weight: 600;
    color: var(--dts-text);
}

.activite-card__meta {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    gap: 0.5rem 1.25rem;
    font-size: 0.82rem;
}

.activite-card__meta-item {
    display: flex;
    flex-direction: column;
    gap: 0.1rem;
}

.activite-card__meta-label {
    font-size: 0.68rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--dts-muted);
    font-weight: 700;
}

.activite-card__meta-value {
    color: var(--dts-text);
}

.activite-card__avancement {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
}

.activite-card__avancement-head {
    display: flex;
    justify-content: space-between;
    font-size: 0.8rem;
    color: var(--dts-muted);
}

.activite-card__avancement-head strong {
    color: var(--dts-navy);
}

.activite-card__avancement .progress {
    height: 8px;
    border-radius: 20px;
    background: var(--dts-bg);
}

.activite-card__avancement .progress-bar {
    background: var(--dts-blue);
    border-radius: 20px;
}

/* --- sous-activités --- */

.sous-liste {
    background: var(--dts-bg);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius-sm);
    padding: 0.75rem 0.9rem;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.sous-liste__titre {
    font-size: 0.72rem;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    font-weight: 700;
    color: var(--dts-muted);
}

.sous-liste__items {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.sous-liste__designation {
    color: var(--dts-text);
    font-size: 0.85rem;
}

.sous-liste__code {
    color: var(--dts-muted);
    font-size: 0.75rem;
    font-family: ui-monospace, Menlo, monospace;
}

/* --- états --- */

.bo-empty {
    padding: 2rem 1rem;
    text-align: center;
    font-size: 0.9rem;
    color: var(--dts-muted);
    background: var(--dts-surface);
    border: 1px dashed var(--dts-border-2);
    border-radius: var(--dts-radius-sm);
}

.suivi-loading {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.suivi-skeleton {
    height: 120px;
    border-radius: var(--dts-radius);
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: sk 1.4s infinite;
}

@keyframes sk {
    from {
        background-position: 200% 0;
    }

    to {
        background-position: -200% 0;
    }
}

.suivi-pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 1rem;
}

.suivi-pagination__info {
    font-size: 0.82rem;
    color: var(--dts-muted);
}

/* --- sous-activités : nouvelle structure --- */

.sous-liste__item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    padding: 0.15rem 0;
    flex-direction: row;
}

.sous-liste__infos {
    display: flex;
    flex-direction: column;
    min-width: 0;
}

/* --- modale --- */

.modal-avancement {
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
}

.modal-avancement__sous {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
    padding: 0.75rem 0.9rem;
    background: var(--dts-bg);
    border-left: 3px solid var(--dts-blue);
    border-radius: 6px;
}

.modal-avancement__code {
    font-family: ui-monospace, Menlo, monospace;
    color: var(--dts-muted);
    font-size: 0.78rem;
}

.modal-avancement__field {
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
}

.modal-avancement__label {
    font-size: 0.82rem;
    font-weight: 600;
    color: var(--dts-text);
}

.modal-avancement__value {
    font-size: 1.8rem;
    font-weight: 700;
    color: var(--dts-blue);
    text-align: center;
    line-height: 1;
}

.modal-avancement__range {
    width: 100%;
    accent-color: var(--dts-blue);
}

.modal-avancement__range-bounds {
    display: flex;
    justify-content: space-between;
    font-size: 0.72rem;
    color: var(--dts-muted);
}

.modal-avancement {
    display: flex;
    flex-direction: column;
    gap: 1.1rem;
}

.modal-avancement--confirm {
    gap: 1.25rem;
}

.modal-avancement__sous {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
    padding: 0.75rem 0.9rem;
    background: var(--dts-bg);
    border-left: 3px solid var(--dts-blue);
    border-radius: 6px;
}

.modal-avancement__code {
    font-family: ui-monospace, Menlo, monospace;
    color: var(--dts-muted);
    font-size: 0.78rem;
}

.modal-avancement__current {
    font-size: 0.85rem;
    color: var(--dts-muted);
}
.modal-avancement__current strong {
    color: var(--dts-navy);
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
    color: var(--dts-text);
    display: flex;
    align-items: center;
    gap: 0.35rem;
}

.modal-avancement__value {
    font-size: 1.8rem;
    font-weight: 700;
    color: var(--dts-blue);
    text-align: center;
    line-height: 1;
}

.modal-avancement__range {
    width: 100%;
    accent-color: var(--dts-blue);
}

.modal-avancement__range-bounds {
    display: flex;
    justify-content: space-between;
    font-size: 0.72rem;
    color: var(--dts-muted);
}

/* --- bandeaux directionnels --- */

.modal-warning {
    display: flex;
    gap: 0.7rem;
    padding: 0.75rem 0.9rem;
    border-radius: 6px;
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
.modal-warning--danger i { color: #d64545; }

.modal-warning--success {
    background: #e6f4ea;
    border-left: 3px solid #1e7e34;
    color: #14532d;
}
.modal-warning--success i { color: #1e7e34; }

.modal-warning--neutral {
    background: #eef1f5;
    border-left: 3px solid #9fb0c2;
    color: #384a5e;
}
.modal-warning--neutral i { color: #5b6b7d; }

.required { color: var(--dts-danger); }
.text-muted-custom { color: var(--dts-muted); font-weight: 400; }

/* --- récap de confirmation --- */

.modal-avancement__question {
    font-size: 1rem;
    margin: 0;
    color: var(--dts-text);
    font-weight: 500;
}

.modal-avancement__recap {
    display: grid;
    grid-template-columns: 140px 1fr;
    gap: 0.5rem 1rem;
    margin: 0;
    padding: 0.9rem 1rem;
    background: var(--dts-bg);
    border-radius: 6px;
    font-size: 0.88rem;
}

.modal-avancement__recap dt {
    color: var(--dts-muted);
    font-weight: 600;
    font-size: 0.78rem;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    align-self: center;
}

.modal-avancement__recap dd {
    margin: 0;
    color: var(--dts-text);
    word-break: break-word;
}

.modal-avancement__note {
    font-size: 0.78rem;
    color: var(--dts-muted);
    margin: 0;
    text-align: center;
    font-style: italic;
}

@media (max-width: 992px) {
    .suivi-cards {
        grid-template-columns: repeat(2, 1fr);
    }

    .suivi-filtres {
        grid-template-columns: 1fr 1fr;
    }
}

@media (max-width: 576px) {
    .bo-page {
        padding: 1rem;
    }

    .suivi-cards {
        grid-template-columns: 1fr;
    }

    .suivi-filtres {
        grid-template-columns: 1fr;
    }
}
</style>
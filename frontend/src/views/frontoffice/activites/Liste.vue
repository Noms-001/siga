<script setup lang="ts">
/**
 * Liste des activites DTS-TSS.
 *
 * Repartition des responsabilites :
 * - le backend filtre, trie, pagine et calcule (statut, avancement) ;
 * - cette page ne fait aucun slice() sur les donnees, aucun calcul
 *   d'avancement et aucun tri local.
 *
 * Le tableau n'utilise pas BaseTable : ce composant pagine et recherche
 * cote client, ce qui est explicitement exclu ici. Le tableau est donc
 * ecrit directement, avec BasePagination pour la pagination serveur.
 */
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import {
    BaseButton,
    BaseCard,
    BaseInput,
    BasePagination,
    BaseSelect,
} from '@/components/base'
import * as activiteService from '@/services/activite'
import { ANNEE_DEFAUT } from '@/services/activite'
import { useAutocomplete, type Suggestion } from '@/composables/useAutocomplete'
import {
    EN_RETARD,
    type ActiviteFiltres,
    type ActiviteListItem,
    type ActiviteOption,
    type ActiviteOptions,
    type ActiviteStatistiques,
    type CarteStatut,
    type Trimestre,
    type VueActivite,
} from '@/types/activite'
import { formatDateSeule } from '@/utils/date'

// ------------------------------------------------------------------
// Constantes de presentation
// ------------------------------------------------------------------

const TAILLE_PAGE = 10
const DELAI_RECHERCHE = 350

const TRIMESTRES: Array<{ value: Trimestre; label: string }> = [
    { value: 1, label: 'T1' },
    { value: 2, label: 'T2' },
    { value: 3, label: 'T3' },
    { value: 4, label: 'T4' },
]

const VUES: Array<{ value: VueActivite; label: string }> = [
    { value: 'PTA', label: 'Activités PTA' },
    { value: 'NON_PTA', label: 'Activités NON PTA' },
]

const ANNEES = Array.from({ length: 6 }, (_, i) => ANNEE_DEFAUT - 1 + i)

// ------------------------------------------------------------------
// Etat
// ------------------------------------------------------------------

const vue = ref<VueActivite>('PTA')

/**
 * Statut applique. Volontairement sans champ dans la zone de filtres :
 * seul le clic sur une card peut le poser ou l'enlever.
 */
const statutActif = ref<string | null>(null)

const filtres = reactive<ActiviteFiltres>({
    search: '',
    objectifSearch: '',
    objectifSpecifiqueId: null,
    serviceId: null,
    prioriteId: null,
    typeActiviteId: null,
    siteId: null,
    annee: ANNEE_DEFAUT,
    trimestre: null,
    dateDebut: '',
    dateFin: '',
})

const activites = ref<ActiviteListItem[]>([])
const statistiques = ref<ActiviteStatistiques>({
    total: 0,
    nonCommencees: 0,
    enCours: 0,
    terminees: 0,
    enRetard: 0,
    annulees: 0,
    reportees: 0,
    suspendues: 0,
})

const options = ref<ActiviteOptions>({
    services: [],
    priorites: [],
    typesActivite: [],
    sites: [],
    statuts: [],
})

const page = ref(1)
const totalPages = ref(0)
const totalElements = ref(0)

const chargement = ref(false)
const chargementStats = ref(false)
const erreur = ref<string | null>(null)

const router = useRouter()

// ------------------------------------------------------------------
// Computed
// ------------------------------------------------------------------

/**
 * Le filtre Service n'est propose que si le backend a envoye des services.
 * Il ne les envoie pas a un utilisateur rattache a un service : le filtre
 * n'est donc pas masque, il n'a simplyment rien a offrir.
 */
const filtreServiceDisponible = computed(() => options.value.services.length > 0)

const estVuePta = computed(() => vue.value === 'PTA')

/** Libelles de priorite : badges derives du code, jamais du libelle. */
const COULEURS_PRIORITE: Record<string, string> = {
    CRITIQUE: 'danger',
    HAUTE: 'warning',
    NORMALE: 'primary',
    FAIBLE: 'secondary',
}

const COULEURS_STATUT: Record<string, string> = {
    NON_COMMENCEE: 'secondary',
    EN_COURS: 'primary',
    TERMINEE: 'success',
    ANNULEE: 'danger',
    REPORTEE: 'warning',
    SUSPENDUE: 'purple',
    EN_RETARD: 'danger',
}

/**
 * Code de statut pose par chaque card.
 *
 * Une seule table pour l'affichage et pour le clic : si les deux avaient
 * leurs propres listes, une carte finirait par filtrer sur un code absent du
 * badge correspondant.
 */
const CODES_CARTES: Record<CarteStatut, string> = {
    nonCommencees: 'NON_COMMENCEE',
    enCours: 'EN_COURS',
    terminees: 'TERMINEE',
    enRetard: EN_RETARD,
    annulees: 'ANNULEE',
    reportees: 'REPORTEE',
    suspendues: 'SUSPENDUE',
}

const cartes = computed(() => [
    {
        cle: null as CarteStatut | null,
        label: 'Total',
        valeur: statistiques.value.total,
        icone: 'bi-collection',
        variante: 'primary',
    },
    {
        cle: 'nonCommencees' as CarteStatut,
        label: 'Non commencées',
        valeur: statistiques.value.nonCommencees,
        icone: 'bi-circle',
        variante: 'secondary',
    },
    {
        cle: 'enCours' as CarteStatut,
        label: 'En cours',
        valeur: statistiques.value.enCours,
        icone: 'bi-hourglass-split',
        variante: 'info',
    },
    {
        cle: 'suspendues' as CarteStatut,
        label: 'Suspendues',
        valeur: statistiques.value.suspendues,
        icone: 'bi-pause-circle',
        variante: 'purple',
    },
    {
        cle: 'terminees' as CarteStatut,
        label: 'Terminées',
        valeur: statistiques.value.terminees,
        icone: 'bi-check-circle',
        variante: 'success',
    },
    {
        cle: 'annulees' as CarteStatut,
        label: 'Annulées',
        valeur: statistiques.value.annulees,
        icone: 'bi-x-circle',
        variante: 'secondary',
    },
    {
        cle: 'reportees' as CarteStatut,
        label: 'Reportées',
        valeur: statistiques.value.reportees,
        icone: 'bi-calendar-event',
        variante: 'warning',
    },
    /*
        En retard est place en dernier, et c est delibere : c est le seul
        compteur qui ne soit pas exclusif. Il recompte une partie des cartes
        qui le precedent, donc le mettre au milieu ferait croire qu il
        s additionne a elles. Le groupement "en cours / suspendues / terminees
        / annulees / reportees" correspond, lui, a des statuts courants
        mutuellement exclusifs.
    */
    {
        cle: 'enRetard' as CarteStatut,
        label: 'En retard',
        valeur: statistiques.value.enRetard,
        icone: 'bi-exclamation-triangle',
        variante: 'danger',
    },
])

/**
 * Options des selects, construites depuis les referentiels du backend.
 * Si le referentiel est vide, aucun libelle n'est invente.
 */
/**
 * Conversion vers le format attendu par BaseSelect.
 * Le code est conserve quand il existe : priorite et statut en ont un.
 */
function versOptions(list: ActiviteOption[]): Array<Record<string, unknown>> {
    return list.map(o => ({
        id: o.id,
        libelle: o.libelle ?? o.code ?? '',
        ...(o.code ? { code: o.code } : {}),
    }))
}

const optionsService = computed(() => versOptions(options.value.services))
const optionsPriorite = computed(() => versOptions(options.value.priorites))
const optionsType = computed(() => versOptions(options.value.typesActivite))
const optionsSite = computed(() => versOptions(options.value.sites))

const anneesOptions = ANNEES.map(a => ({ id: a, libelle: String(a) }))

const trimestresOptions = TRIMESTRES.map(t => ({
    id: t.value,
    libelle: t.label,
}))

// ------------------------------------------------------------------
// Autocompletes
// ------------------------------------------------------------------

/**
 * Les deux champs texte partagent le meme composable.
 *
 * Filtrage a la frappe ET suggestions sont deux choses distinctes :
 * - la frappe alimente filtres.search / filtres.objectifSearch, qui
 *   rechargent la liste ;
 * - les suggestions ne font que proposer un choix parmi ce que le backend
 *   autorise. Choisir une suggestion fige la recherche, sans jamais
 *   inventer de critere que le backend ne comprendrait pas.
 */

/** Liste de suggestions, au format attendu par BaseInput (role option). */
function formatSuggestion(s: Suggestion): string {
    return s.libelleSecondaire
        ? `${s.code} — ${s.libelle} (${s.libelleSecondaire})`
        : `${s.code} — ${s.libelle}`
}

const autocompleteActivite = useAutocomplete({
    rechercher: async (terme: string) => {
        const reponse = await activiteService.autocompleterActivites(terme)
        return reponse.success ? reponse.data : []
    },
    onSelectionner: (s) => {
        // Le filtre porte sur le CODE, pas sur le libelle affiche : le code est
        // le seul identifiant stable, le libelle peut contenir des espaces,
        // des accents et evoluer.
        filtres.search = s.code ?? ''
        appliquerFiltres()
    },
    onEffacer: () => {
        if (filtres.search !== '') {
            filtres.search = ''
            appliquerFiltres()
        }
    },
    delaiMs: DELAI_RECHERCHE,
})

const autocompleteObjectif = useAutocomplete({
    rechercher: async (terme: string) => {
        const reponse = await activiteService.autocompleterObjectifs(terme)
        return reponse.success ? reponse.data : []
    },
    onSelectionner: (s) => {
        // Un objectif a un identifiant : on filtre dessus, ce qui est exact.
        filtres.objectifSpecifiqueId = s.id
        // Le texte est vide pour ne pas resserrer le filtre avec un libelle
        // que le backend ne rejouerait pas comme une eg stricte.
        filtres.objectifSearch = ''
        appliquerFiltres()
    },
    onEffacer: () => {
        if (filtres.objectifSpecifiqueId !== null || filtres.objectifSearch !== '') {
            filtres.objectifSpecifiqueId = null
            filtres.objectifSearch = ''
            appliquerFiltres()
        }
    },
    delaiMs: DELAI_RECHERCHE,
})

// ------------------------------------------------------------------
// Chargement
// ------------------------------------------------------------------

let minuteurFiltre: ReturnType<typeof setTimeout> | null = null
let compteurRequete = 0

/**
 * Deux requetes en parallele : la page et les compteurs.
 *
 * Le compteur d'identifiant evite qu'une reponse lente n'ecrase une reponse
 * plus recente (frappe rapide dans un filtre).
 */
async function charger() {
    const jeton = ++compteurRequete

    chargement.value = true
    erreur.value = null

    let reponsePage: Awaited<ReturnType<typeof activiteService.listerActivites>>
    let reponseStats: Awaited<ReturnType<typeof activiteService.statistiquesActivites>>

    /**
     * get() convertit une reponse HTTP en echec metier, mais laisse remonter
     * une panne reseau : fetch rejette sur un abort ou un serveur injoignable.
     * Sans ce rattrapage, le rejet traversait Promise.all puis sortait de
     * charger(), que les appelants appellent avec void : le navigateur
     * signalait alors "Uncaught (in promise) TypeError: NetworkError".
     */
    try {
        ;[reponsePage, reponseStats] = await Promise.all([
            activiteService.listerActivites(
                { ...filtres },
                vue.value,
                statutActif.value,
                page.value,
                TAILLE_PAGE
            ),
            activiteService.statistiquesActivites(
                { ...filtres },
                vue.value,
                statutActif.value
            ),
        ])
    } catch {
        if (jeton !== compteurRequete) return
        erreur.value = 'Connexion au serveur impossible. Réessayez dans un instant.'
        activites.value = []
        chargement.value = false
        return
    }

    if (jeton !== compteurRequete) return

    chargementStats.value = false

    if (!reponsePage.success) {
        erreur.value = reponsePage.error
        activites.value = []
        chargement.value = false
        return
    }

    activites.value = reponsePage.data.content
    totalElements.value = reponsePage.data.totalElements
    totalPages.value = reponsePage.data.totalPages

    /**
     * Filtre devenu trop restrictif : le backend peut renvoyer une page vide
     * alors qu on en demande une qui existe encore.
     *
     * Le garde `derniere === page.value` est ce qui empeche la boucle
     * infinie. Clamper sur la derniere page ne suffit pas : si cette page est
     * elle-meme vide, le clamp redonne le meme numero, la meme requete part,
     * et la meme reponse vide revient, indefiniment. La seule garantie de
     * terminaison est de constater que la page n a pas bouge.
     */
    if (reponsePage.data.content.length === 0 && page.value > 1) {
        const derniere = Math.max(1, totalPages.value)

        if (derniere === page.value) {
            activites.value = []
        } else {
            page.value = derniere
            chargement.value = false
            return charger()
        }
    }

    if (reponseStats.success) {
        statistiques.value = reponseStats.data
    }

    chargement.value = false
}

async function chargerOptions() {
    try {
        const reponse = await activiteService.chargerOptions()

        if (reponse.success) {
            options.value = reponse.data
        }
    } catch {
        // Les referentiels manquants ne doivent pas empecher l affichage de
        // la liste : les filtres restent vides, la page reste utilisable.
    }
}

/** Appele API groupe pour une saisie : pas de requete a chaque frappe. */
function surRechercheActivite() {
    filtres.search = autocompleteActivite.saisie.value
    autocompleteActivite.charger()
}

function surRechercheObjectif() {
    filtres.objectifSearch = autocompleteObjectif.saisie.value
    autocompleteObjectif.charger()
}

// ------------------------------------------------------------------
// Actions de l'interface
// ------------------------------------------------------------------

/** Toute modification de filtre repart de la page 1. */
function appliquerFiltres() {
    // Annule un rechargement encore en attente : une selection de suggestion
    // appelle cette fonction alors qu un minuteur de frappe peut rester actif,
    // et on ne veut pas deux requetes pour le meme etat de filtres.
    if (minuteurFiltre) {
        clearTimeout(minuteurFiltre)
        minuteurFiltre = null
    }

    page.value = 1
    void charger()
}

function changerPage(nouvelle: number) {
    page.value = nouvelle
    void charger()
}

/** Ouverture du detail : la page existe depuis la creation de /activites/:id. */
function voirDetail(id: number) {
    router.push({ name: 'activite-detail', params: { id } })
}

/** Reclic sur une card active : le filtre de statut est retire. */
function basculerStatut(cle: CarteStatut) {
    const code = CODES_CARTES[cle]
    statutActif.value = statutActif.value === code ? null : code
    page.value = 1
    void charger()
}

const estCarteActive = (cle: CarteStatut | null): boolean => {
    if (!cle) return false
    return statutActif.value === CODES_CARTES[cle]
}

/**
 * Libelle du statut actif dans le badge.
 *
 * Le libelle vient du referentiel du backend quand il est connu, et EN_RETARD
 * est traite a part car il n existe pas en table : c est un pseudo-statut
 * calcule, donc il n aura jamais de libelle dans options.statuts.
 */
const libelleStatut = (code: string): string => {
    if (code === EN_RETARD) return 'En retard'

    const option = options.value.statuts.find(o => o.code === code)
    return option?.libelle ?? code
}

/** Retrait du filtre de statut, quel que soit le statut pose. */
function retirerStatut() {
    statutActif.value = null
    page.value = 1
    void charger()
}

function changerVue(nouvelle: VueActivite) {
    if (vue.value === nouvelle) return

    vue.value = nouvelle

    // Un objectif specifique n'a pas de sens hors PTA.
    filtres.objectifSearch = ''
    filtres.objectifSpecifiqueId = null
    autocompleteObjectif.saisie.value = ''
    autocompleteObjectif.fermer()

    page.value = 1
    void charger()
}

function reinitialiser() {
    filtres.search = ''
    filtres.objectifSearch = ''
    filtres.objectifSpecifiqueId = null
    filtres.serviceId = null
    filtres.prioriteId = null
    filtres.typeActiviteId = null
    filtres.siteId = null
    filtres.annee = ANNEE_DEFAUT
    filtres.trimestre = null
    filtres.dateDebut = ''
    filtres.dateFin = ''

    statutActif.value = null
    autocompleteActivite.saisie.value = ''
    autocompleteObjectif.saisie.value = ''
    autocompleteActivite.fermer()
    autocompleteObjectif.fermer()

    page.value = 1
    void charger()
}

// ------------------------------------------------------------------
// Rendu
// ------------------------------------------------------------------

function variantePriorite(item: ActiviteListItem): string {
    const code = item.priorite?.code
    return (code && COULEURS_PRIORITE[code.toUpperCase()]) || 'secondary'
}

function varianteStatut(code: string | null | undefined): string {
    if (!code) return 'secondary'
    return COULEURS_STATUT[code.toUpperCase()] || 'secondary'
}

/** Arrondi pour l'affichage, la valeur brute vient du backend. */
function avancementAffiche(item: ActiviteListItem): number {
    return Math.round(item.avancement ?? 0)
}

// ------------------------------------------------------------------
// Cycle de vie
// ------------------------------------------------------------------

onMounted(async () => {
    await Promise.all([chargerOptions(), charger()])
})

onBeforeUnmount(() => {
    if (minuteurFiltre) clearTimeout(minuteurFiltre)
})

/**
 * Les deux champs texte filtrent la liste, avec le meme delai que
 * l'autocomplete.
 *
 * Sans ce delai, chaque frappe declencherait un rechargement de la page ET
 * des compteurs : trois requetes par caractere, et les reponses pourraient
 * arriver dans le desordre. appliquerFiltres annule ce minuteur quand elle est
 * appelee directement (selection d'une suggestion), donc une seule requete
 * dans ce cas la.
 */
watch(
    () => [filtres.search, filtres.objectifSearch] as const,
    () => {
        if (minuteurFiltre) clearTimeout(minuteurFiltre)
        minuteurFiltre = setTimeout(appliquerFiltres, DELAI_RECHERCHE)
    }
)
</script>

<template>
    <div class="liste-activites">
        <!-- ============ EN-TETE ============ -->
        <div class="page-header d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
                <h1 class="page-title">Activités</h1>
                <p class="page-subtitle">Gestion et suivi des activités DTS-TSS</p>
            </div>

            <!--
                La page de creation n'existe pas encore dans le projet :
                /activites/nouvelle est un lien mort de la sidebar.
                Bouton desactive plutot que route fictive.
            -->
            <BaseButton variant="primary" disabled title="Page de création non encore disponible">
                <i class="bi bi-plus-lg"></i>
                Ajouter une activité
            </BaseButton>
        </div>

        <!-- ============ ERREUR ============ -->
        <div v-if="erreur" class="alert alert-danger d-flex align-items-center gap-2" role="alert">
            <i class="bi bi-exclamation-triangle"></i>
            <span>{{ erreur }}</span>
            <button type="button" class="btn-close ms-auto" @click="erreur = null"></button>
        </div>

        <!-- ============ CARDS STATISTIQUES ============ -->
        <!--
            col-lg sans largeur fixe : avec 8 cards, une largeur en colonne 2
            (col-lg-2) deborderait la ligne sur les ecrans moyens.
        -->
        <div class="row g-3 mb-4">
            <div v-for="carte in cartes" :key="carte.label" class="col-12 col-sm-6 col-lg-3">
                <BaseCard class="stat-card h-100" :class="{
                    'stat-card--active': estCarteActive(carte.cle),
                    'stat-card--static': carte.cle === null,
                }" :title="carte.label">
                    <div class="stat-card__body">
                        <i class="stat-card__icon" :class="`bi ${carte.icone} text-${carte.variante}`"></i>
                        <span class="stat-card__value">{{ carte.valeur }}</span>
                    </div>

                    <template v-if="carte.cle">
                        <button type="button" class="stat-card__overlay" :aria-pressed="estCarteActive(carte.cle)"
                            @click="basculerStatut(carte.cle)">
                            <span class="visually-hidden">Filtrer sur {{ carte.label }}</span>
                        </button>
                    </template>
                </BaseCard>
            </div>
        </div>

        <!-- ============ PTA / NON PTA ============ -->
        <div class="vue-switch mb-3" role="tablist" aria-label="Type d'activité">
            <button v-for="v in VUES" :key="v.value" type="button" role="tab" class="vue-switch__btn"
                :class="{ 'vue-switch__btn--active': vue === v.value }" :aria-selected="vue === v.value"
                @click="changerVue(v.value)">
                {{ v.label }}
            </button>
        </div>

        <!-- ============ FILTRES ============ -->
        <BaseCard class="filtres mb-4" title="Filtres" noPadding>
            <div class="filtres__grid">
                <!--
                    La div englobe le champ ET la liste : c'est elle qui porte
                    la ref racine du composable, pour que le clic dans la liste
                    ne la ferme pas avant d'etre traite.
                -->
                <div class="row">
                    <div ref="autocompleteActivite.racine" class="filtres__col filtres__col--large col-md-6"
                        @keydown="autocompleteActivite.surTouche">
                        <BaseInput v-model="autocompleteActivite.saisie.value" label="Recherche activité" type="search"
                            icon="bi bi-search" placeholder="Code, référence ou désignation" autocomplete="off"
                            :aria-expanded="autocompleteActivite.ouvert.value" @input="surRechercheActivite" />
                        <ul v-if="autocompleteActivite.ouvert.value && autocompleteActivite.suggestions.value.length"
                            class="autocomplete" role="listbox" aria-label="Suggestions d'activité">
                            <li v-for="(s, i) in autocompleteActivite.suggestions.value" :key="s.id" role="option"
                                :aria-selected="i === autocompleteActivite.indexActif.value"
                                :class="{ 'autocomplete__item--actif': i === autocompleteActivite.indexActif.value }"
                                @mouseenter="autocompleteActivite.survoler(i)">
                                <button type="button" tabindex="-1" @click="autocompleteActivite.selectionner(s)">
                                    {{ formatSuggestion(s) }}
                                </button>
                            </li>
                        </ul>
                    </div>

                    <!-- Recherche objectif : PTA uniquement -->
                    <div v-if="estVuePta" ref="autocompleteObjectif.racine"
                        class="filtres__col filtres__col--large col-md-6" @keydown="autocompleteObjectif.surTouche">
                        <BaseInput v-model="autocompleteObjectif.saisie.value" label="Recherche objectif spécifique"
                            type="search" icon="bi bi-bullseye" placeholder="Code ou désignation" autocomplete="off"
                            :aria-expanded="autocompleteObjectif.ouvert.value" @input="surRechercheObjectif" />
                        <ul v-if="autocompleteObjectif.ouvert.value && autocompleteObjectif.suggestions.value.length"
                            class="autocomplete" role="listbox" aria-label="Suggestions d'objectif">
                            <li v-for="(s, i) in autocompleteObjectif.suggestions.value" :key="s.id" role="option"
                                :aria-selected="i === autocompleteObjectif.indexActif.value"
                                :class="{ 'autocomplete__item--actif': i === autocompleteObjectif.indexActif.value }"
                                @mouseenter="autocompleteObjectif.survoler(i)">
                                <button type="button" tabindex="-1" @click="autocompleteObjectif.selectionner(s)">
                                    {{ formatSuggestion(s) }}
                                </button>
                            </li>
                        </ul>
                    </div>
                </div>

                <!-- Service : uniquement si le backend en fournit -->
                <div v-if="filtreServiceDisponible" class="filtres__col col-md-6">
                    <BaseSelect :model-value="filtres.serviceId ?? undefined" label="Service"
                        placeholder="Tous les services" :options="optionsService" option-label="libelle"
                        option-value="id"
                        @update:model-value="filtres.serviceId = $event as number | null; appliquerFiltres()" />
                </div>

                <div class="row">
                    <div class="filtres__col col-md-4">
                        <BaseSelect :model-value="filtres.prioriteId ?? undefined" label="Priorité" placeholder="Toutes"
                            :options="optionsPriorite" option-label="libelle" option-value="id"
                            @update:model-value="filtres.prioriteId = $event as number | null; appliquerFiltres()" />
                    </div>

                    <div class="filtres__col col-md-4">
                        <BaseSelect :model-value="filtres.typeActiviteId ?? undefined" label="Type d'activité"
                            placeholder="Tous" :options="optionsType" option-label="libelle" option-value="id"
                            @update:model-value="filtres.typeActiviteId = $event as number | null; appliquerFiltres()" />
                    </div>

                    <div class="filtres__col col-md-4">
                        <BaseSelect :model-value="filtres.siteId ?? undefined" label="Site" placeholder="Tous"
                            :options="optionsSite" option-label="libelle" option-value="id"
                            @update:model-value="filtres.siteId = $event as null | number; appliquerFiltres()" />
                    </div>
                </div>

                <div class="row">
                    <div class="filtres__col col-md-3">
                        <BaseSelect :model-value="filtres.annee" label="Année" :options="anneesOptions"
                            option-label="libelle" option-value="id"
                            @update:model-value="filtres.annee = $event as number; appliquerFiltres()" />
                    </div>

                    <div class="filtres__col col-md-3">
                        <BaseSelect :model-value="filtres.trimestre ?? undefined" label="Trimestre" placeholder="Tous"
                            :options="trimestresOptions" option-label="libelle" option-value="id"
                            @update:model-value="filtres.trimestre = ($event as Trimestre) ?? null; appliquerFiltres()" />
                    </div>

                    <div class="filtres__col col-md-3">
                        <BaseInput v-model="filtres.dateDebut" label="Date début" type="date"
                            @change="appliquerFiltres()" />
                    </div>

                    <div class="filtres__col col-md-3">
                        <BaseInput v-model="filtres.dateFin" label="Date fin" type="date"
                            @change="appliquerFiltres()" />
                    </div>
                </div>
            </div>

            <div class="filtres__actions">
                <span v-if="statutActif" class="filtres__badge">
                    <i class="bi bi-funnel"></i>
                    {{ libelleStatut(statutActif) }}
                    <button type="button" @click="retirerStatut" title="Retirer le filtre de statut">
                        <i class="bi bi-x"></i>
                    </button>
                </span>

                <BaseButton variant="secondary" @click="reinitialiser">
                    <i class="bi bi-arrow-counterclockwise"></i>
                    Réinitialiser les filtres
                </BaseButton>
            </div>
        </BaseCard>

        <!-- ============ TABLEAU ============ -->
        <BaseCard noPadding>
            <div v-if="chargement" class="table-state">
                <div class="spinner-border text-primary" role="status"></div>
                <p class="mt-3 mb-0">Chargement des activités…</p>
            </div>

            <div v-else-if="!erreur && activites.length === 0" class="table-state">
                <i class="bi bi-inbox empty-icon"></i>
                <h3 class="empty-title">Aucune activité trouvée</h3>
                <p class="empty-text">
                    Aucune activité ne correspond aux critères sélectionnés.
                </p>
                <BaseButton variant="secondary" @click="reinitialiser">Réinitialiser les filtres</BaseButton>
            </div>

            <div v-else class="table-wrapper">
                <table class="table-activites">
                    <thead>
                        <tr>
                            <th class="col-code">Code / Réf.</th>
                            <th class="col-designation">Désignation</th>
                            <th v-if="estVuePta" class="col-objectif">Objectif spécifique</th>
                            <th class="col-service">Service</th>
                            <th class="col-type-site">Type / Site</th>
                            <th class="col-priorite">Priorité</th>
                            <th class="col-statut">Statut</th>
                            <th class="col-periode">Période</th>
                            <th class="col-avancement">Avancement</th>
                            <th class="col-actions">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="item in activites" :key="item.id">
                            <!-- Code + référence empilés -->
                            <td class="col-code">
                                <span class="cell-code">{{ item.code }}</span>
                                <span class="cell-reference">{{ item.reference }}</span>
                            </td>

                            <td class="col-designation">
                                <span class="cell-designation">{{ item.designation }}</span>
                            </td>

                            <td v-if="estVuePta" class="col-objectif">
                                <span v-if="item.objectifSpecifique" class="objectif">
                                    <span class="objectif__code">{{ item.objectifSpecifique.code }}</span>
                                    <span class="objectif__libelle">{{ item.objectifSpecifique.libelle }}</span>
                                </span>
                                <span v-else class="text-muted">—</span>
                            </td>

                            <td class="col-service">
                                <span class="cell-truncate">{{ item.service?.libelle ?? '—' }}</span>
                            </td>

                            <!-- Type + Site empilés -->
                            <td class="col-type-site">
                                <span class="cell-type">{{ item.typeActivite?.libelle ?? '—' }}</span>
                                <span class="cell-site">
                                    <i class="bi bi-geo-alt"></i>
                                    {{ item.site?.libelle ?? '—' }}
                                </span>
                            </td>

                            <td class="col-priorite">
                                <span class="badge-soft" :class="`badge-soft--${variantePriorite(item)}`">
                                    {{ item.priorite?.libelle ?? '—' }}
                                </span>
                            </td>

                            <td class="col-statut">
                                <span v-if="item.statut" class="badge-soft"
                                    :class="`badge-soft--${varianteStatut(item.statut.code)}`">
                                    {{ item.statut.libelle }}
                                </span>
                                <span v-else class="text-muted">—</span>
                            </td>

                            <td class="col-periode">
                                <span class="cell-periode">
                                    <span>{{ formatDateSeule(item.dateDebutPrevue) }}</span>
                                    <span class="cell-periode__arrow">→</span>
                                    <span>{{ formatDateSeule(item.dateFinPrevue) }}</span>
                                </span>
                            </td>

                            <td class="col-avancement">
                                <div class="avancement">
                                    <span class="avancement__valeur">
                                        {{ avancementAffiche(item) }}%
                                    </span>
                                    <div class="avancement__bar" role="progressbar"
                                        :aria-valuenow="avancementAffiche(item)" aria-valuemin="0" aria-valuemax="100">
                                        <div class="avancement__fill"
                                            :style="{ width: avancementAffiche(item) + '%' }"></div>
                                    </div>
                                </div>
                            </td>

                            <td class="col-actions">
                                <div class="actions">
                                    <button type="button" class="btn-icone" title="Voir le détail"
                                        @click="voirDetail(item.id)">
                                        <i class="bi bi-eye"></i>
                                    </button>
                                    <button type="button" class="btn-icone" disabled
                                        title="Page d'édition non encore disponible">
                                        <i class="bi bi-pencil"></i>
                                    </button>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div v-if="!chargement && activites.length" class="px-3 pb-3">
                <BasePagination :page="page" :total-pages="totalPages" :total-elements="totalElements"
                    :page-size="TAILLE_PAGE" @change="changerPage" />
            </div>
        </BaseCard>
    </div>
</template>

<style scoped>
.liste-activites {
    padding: 1.5rem 0 2rem;
}

/* --- En-tete --- */
.page-header {
    margin-bottom: 1.5rem;
}

.page-title {
    font-size: 1.6rem;
    font-weight: 700;
    color: #1a2b3c;
    margin: 0;
}

.page-subtitle {
    font-size: 0.9rem;
    color: #74879b;
    margin: 0.2rem 0 0;
}

/* --- Cards --- */
.stat-card {
    position: relative;
    transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.stat-card--static {
    opacity: 0.92;
}

.stat-card__body {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.5rem;
}

.stat-card__icon {
    font-size: 1.35rem;
    opacity: 0.85;
}

.stat-card__value {
    font-size: 1.75rem;
    font-weight: 700;
    color: #1a2b3c;
    line-height: 1;
}

.stat-card__overlay {
    position: absolute;
    inset: 0;
    border: none;
    border-radius: inherit;
    background: transparent;
    cursor: pointer;
}

.stat-card__overlay:focus-visible {
    outline: 2px solid #1e88a8;
    outline-offset: 2px;
}

.stat-card--active {
    border-color: #1e88a8;
    box-shadow: 0 0 0 2px rgba(30, 136, 168, 0.18);
    transform: translateY(-2px);
}

/* --- Bascule PTA / NON PTA --- */
.vue-switch {
    display: inline-flex;
    padding: 3px;
    background: #eef2f6;
    border-radius: 8px;
}

.vue-switch__btn {
    padding: 0.5rem 1.1rem;
    border: none;
    border-radius: 6px;
    background: transparent;
    color: #5a6b7f;
    font-size: 0.85rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.15s ease;
}

.vue-switch__btn--active {
    background: #fff;
    color: #1e88a8;
    box-shadow: 0 1px 3px rgba(26, 43, 60, 0.12);
}

/* --- Filtres --- */
.filtres__grid {
    display: grid;
    gap: 1rem;
    padding: 1.25rem;
}

.filtres__col {
    position: relative;
}

.filtres__actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
    padding: 0 1.25rem 1.25rem;
}

.filtres__badge {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.3rem 0.6rem;
    background: #e7f2f7;
    border: 1px solid #bcdcea;
    border-radius: 20px;
    font-size: 0.78rem;
    color: #14657f;
}

.filtres__badge button {
    border: none;
    background: transparent;
    color: inherit;
    cursor: pointer;
    padding: 0;
    line-height: 1;
}

/* --- Autocomplete --- */
.autocomplete {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    z-index: 20;
    margin: 0.15rem 0 0;
    padding: 0.25rem;
    list-style: none;
    background: #fff;
    border: 1px solid #dde4ec;
    border-radius: 7px;
    box-shadow: 0 6px 18px rgba(26, 43, 60, 0.12);
    max-height: 240px;
    overflow-y: auto;
}

.autocomplete button {
    display: block;
    width: 100%;
    padding: 0.45rem 0.6rem;
    border: none;
    border-radius: 5px;
    background: transparent;
    text-align: left;
    font-size: 0.82rem;
    color: #33475c;
    cursor: pointer;
}

.autocomplete button:hover {
    background: #eef5f8;
}

/*
    Mise en avant de la suggestion selectionnee au clavier.
    Placee apres :hover pour que l emphasizing reste le meme que celui du survol,
    une seule couleur pour les deux affordances.
*/
.autocomplete__item--actif button {
    background: #eef5f8;
    font-weight: 600;
}

/* --- Tableau --- */
.table-wrapper {
    width: 100%;
    overflow: visible;
}

.table-activites {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.82rem;
    table-layout: fixed;
    /* clé : respecte les largeurs de colonnes */
}

.table-activites th,
.table-activites td {
    padding: 0.65rem 0.6rem;
    border-bottom: 1px solid #eef2f6;
    vertical-align: middle;
    overflow: hidden;
}

.table-activites thead th {
    background: #f6f8fa;
    color: #5a6b7f;
    font-size: 0.7rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.03em;
    white-space: nowrap;
}

.table-activites tbody tr:hover {
    background: #f9fbfc;
}

/* --- Largeurs de colonnes --- */
.col-code {
    width: 9%;
}

.col-designation {
    width: 15%;
}

.col-objectif {
    width: 14%;
}

.col-service {
    width: 9%;
}

.col-type-site {
    width: 10%;
}

.col-priorite {
    width: 7%;
}

.col-statut {
    width: 9%;
}

.col-periode {
    width: 12%;
}

.col-avancement {
    width: 11%;
}

.col-actions {
    width: 6%;
    text-align: center;
}

/* --- Cellules --- */
.cell-code {
    display: block;
    font-weight: 700;
    color: #1a2b3c;
    font-size: 0.82rem;
}

.cell-reference {
    display: block;
    color: #8a9aac;
    font-size: 0.72rem;
    margin-top: 1px;
}

.cell-designation {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    /* max 2 lignes */
    -webkit-box-orient: vertical;
    overflow: hidden;
    text-overflow: ellipsis;
    line-height: 1.3;
    color: #33475c;
}

.cell-truncate {
    display: block;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.col-type-site .cell-type {
    display: block;
    color: #33475c;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.col-type-site .cell-site {
    display: block;
    color: #8a9aac;
    font-size: 0.72rem;
    margin-top: 1px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.cell-periode {
    display: flex;
    flex-direction: column;
    gap: 1px;
    font-size: 0.74rem;
    color: #5a6b7f;
    line-height: 1.25;
}

.cell-periode__arrow {
    color: #b6c2ce;
    font-size: 0.68rem;
    line-height: 1;
}

/* --- Objectif --- */
.objectif {
    display: flex;
    flex-direction: column;
    line-height: 1.3;
}

.objectif__code {
    font-weight: 600;
    color: #14657f;
    font-size: 0.78rem;
}

.objectif__libelle {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    color: #5a6b7f;
    font-size: 0.74rem;
}

/* --- Badges --- */
.badge-soft {
    display: inline-block;
    padding: 0.15rem 0.45rem;
    border-radius: 20px;
    font-size: 0.7rem;
    font-weight: 600;
    white-space: nowrap;
}

.badge-soft--primary {
    background: #e3edf6;
    color: #14507a;
}

.badge-soft--info {
    background: #e0f0f4;
    color: #14657f;
}

.badge-soft--success {
    background: #e4f4ea;
    color: #1c6b40;
}

.badge-soft--warning {
    background: #fdf0dc;
    color: #8a5a10;
}

.badge-soft--danger {
    background: #fbe6e4;
    color: #9c2a20;
}

.badge-soft--secondary {
    background: #eceff2;
    color: #55636f;
}

/*
    "Suspendu" n'a pas de couleur Bootstrap et ne peut pas emprunter celle
    d'un autre statut : la partagere avec "Reportee" ou "Non commencee"
    rendrait les deux badges indistinguables dans le tableau.
*/
.badge-soft--purple {
    background: #ebe6f7;
    color: #523a91;
}

/* Couleur de l icone des cards : meme role que text-* de Bootstrap. */
.text-purple {
    color: #6b4fa0;
}

/* --- Avancement --- */
.avancement {
    display: flex;
    flex-direction: column;
    gap: 3px;
}

.avancement__valeur {
    font-size: 0.72rem;
    font-weight: 600;
    color: #33475c;
    text-align: center;
}

.avancement__bar {
    width: 100%;
    height: 6px;
    background: #eef2f6;
    border-radius: 3px;
    overflow: hidden;
}

.avancement__fill {
    height: 100%;
    background: #1e88a8;
    border-radius: 3px;
    transition: width 0.25s ease;
}

/* --- Actions --- */
.actions {
    display: inline-flex;
    gap: 0.3rem;
}

.btn-icone {
    width: 1.7rem;
    height: 1.7rem;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #dde4ec;
    border-radius: 6px;
    background: #fff;
    color: #42556b;
    font-size: 0.8rem;
    cursor: pointer;
    transition: all 0.15s ease;
}

.btn-icone:hover:not(:disabled) {
    border-color: #1e88a8;
    color: #1e88a8;
}

.btn-icone:disabled {
    opacity: 0.4;
    cursor: not-allowed;
}

/* --- Etats --- */
.table-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 3.5rem 1.5rem;
    text-align: center;
}

.empty-icon {
    font-size: 2.6rem;
    color: #c3ced9;
    margin-bottom: 0.75rem;
}

.empty-title {
    font-size: 1.05rem;
    font-weight: 600;
    color: #33475c;
    margin: 0 0 0.35rem;
}

.empty-text {
    font-size: 0.85rem;
    color: #74879b;
    margin: 0 0 1.1rem;
}

/* --- Responsive --- */
@media (max-width: 1199.98px) {
    .table-activites {
        font-size: 0.78rem;
    }

    .table-activites th,
    .table-activites td {
        padding: 0.55rem 0.4rem;
    }
}

@media (max-width: 991.98px) {

    /* En dessous de 992px, on masque les colonnes secondaires */
    .col-objectif,
    .col-type-site,
    .col-service {
        display: none;
    }

    .col-code {
        width: 12%;
    }

    .col-designation {
        width: 22%;
    }

    .col-priorite {
        width: 10%;
    }

    .col-statut {
        width: 12%;
    }

    .col-periode {
        width: 16%;
    }

    .col-avancement {
        width: 18%;
    }

    .col-actions {
        width: 10%;
    }
}

@media (max-width: 767.98px) {
    .filtres__actions {
        flex-direction: column;
        align-items: stretch;
    }

    .stat-card__value {
        font-size: 1.5rem;
    }
}

@media (max-width: 575.98px) {

    /* Sur mobile : on cache encore plus */
    .col-priorite,
    .col-periode {
        display: none;
    }

    .col-code {
        width: 18%;
    }

    .col-designation {
        width: 34%;
    }

    .col-statut {
        width: 18%;
    }

    .col-avancement {
        width: 20%;
    }

    .col-actions {
        width: 10%;
    }
}
</style>
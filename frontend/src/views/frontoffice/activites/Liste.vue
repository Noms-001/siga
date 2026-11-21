<script setup lang="ts">
import { useToastMessage } from '@/composables/useToast'

import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import {
    BaseButton,
    BaseCard,
    BaseConfirm,
    BaseInput,
    BaseModal,
    BasePagination,
    BaseSelect,
} from '@/components/base'
import * as activiteService from '@/services/activite'
import { ANNEE_DEFAUT } from '@/services/activite'
import { useConfirmation } from '@/composables/useConfirmation'
import {
    EN_RETARD,
    type ActiviteAutocomplete,
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
import type { ActiviteExportResponse } from '@/types/exportActivite'

// ------------------------------------------------------------------
// Constantes de presentation
// ------------------------------------------------------------------

const dateDuJour = computed(() =>
    new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    }).format(new Date()).toUpperCase()
)

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
    { value: 'NON_PTA', label: 'Activités courantes' },
]

const ANNEES = Array.from({ length: 6 }, (_, i) => ANNEE_DEFAUT - 1 + i)

// ------------------------------------------------------------------
// Etat
// ------------------------------------------------------------------

const vue = ref<VueActivite>('PTA')
const statutActif = ref<string | null>(null)
const filtresOuverts = ref(false)

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
    total: 0, nonCommencees: 0, enCours: 0, terminees: 0,
    enRetard: 0, annulees: 0, reportees: 0, suspendues: 0,
})

const options = ref<ActiviteOptions>({
    services: [], priorites: [], typesActivite: [], sites: [], statuts: [],
})

const page = ref(1)
const totalPages = ref(0)
const totalElements = ref(0)

const chargement = ref(false)
const chargementStats = ref(false)
const erreur = useToastMessage('error')
const succes = useToastMessage('success')
const avertissement = useToastMessage('warning')

const exportEnCours = ref(false)
const exportMenuOuvert = ref(false)

const router = useRouter()
const confirmation = useConfirmation()

// --- Priorité : édition inline ---
const prioriteEnEdition = ref<ActiviteListItem | null>(null)
const positionDropdown = ref({ top: 0, left: 0 })

// --- Reprise : modale avec motif ---
const modalReprise = ref(false)
const activitePourReprise = ref<ActiviteListItem | null>(null)
const motifReprise = ref('')
const envoiReprise = ref(false)

// ------------------------------------------------------------------
// Computed
// ------------------------------------------------------------------

const filtreServiceDisponible = computed(() => options.value.services.length > 0)
const estVuePta = computed(() => vue.value === 'PTA')

const nombreFiltresActifs = computed(() => {
    let n = 0
    if (filtres.search) n++
    if (filtres.objectifSearch) n++
    if (filtres.objectifSpecifiqueId !== null) n++
    if (filtres.serviceId !== null) n++
    if (filtres.prioriteId !== null) n++
    if (filtres.typeActiviteId !== null) n++
    if (filtres.siteId !== null) n++
    if (filtres.trimestre !== null) n++
    if (filtres.dateDebut) n++
    if (filtres.dateFin) n++
    if (statutActif.value) n++
    return n
})

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
    { cle: null as CarteStatut | null, label: 'Total', valeur: statistiques.value.total, icone: 'bi-collection', variante: 'primary' },
    { cle: 'nonCommencees' as CarteStatut, label: 'Non commencées', valeur: statistiques.value.nonCommencees, icone: 'bi-circle', variante: 'secondary' },
    { cle: 'enCours' as CarteStatut, label: 'En cours', valeur: statistiques.value.enCours, icone: 'bi-hourglass-split', variante: 'info' },
    { cle: 'suspendues' as CarteStatut, label: 'Suspendues', valeur: statistiques.value.suspendues, icone: 'bi-pause-circle', variante: 'purple' },
    { cle: 'terminees' as CarteStatut, label: 'Terminées', valeur: statistiques.value.terminees, icone: 'bi-check-circle', variante: 'success' },
    { cle: 'annulees' as CarteStatut, label: 'Annulées', valeur: statistiques.value.annulees, icone: 'bi-x-circle', variante: 'secondary' },
    { cle: 'reportees' as CarteStatut, label: 'Reportées', valeur: statistiques.value.reportees, icone: 'bi-calendar-event', variante: 'warning' },
    { cle: 'enRetard' as CarteStatut, label: 'En retard', valeur: statistiques.value.enRetard, icone: 'bi-exclamation-triangle', variante: 'danger' },
])

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
const trimestresOptions = TRIMESTRES.map(t => ({ id: t.value, libelle: t.label }))

// ------------------------------------------------------------------
// Autocompletes distants BaseSelect
// ------------------------------------------------------------------

const activiteSuggestions = ref<ActiviteAutocomplete[]>([])
const objectifSuggestions = ref<ActiviteAutocomplete[]>([])
const activiteSelection = ref<number | null>(null)
const objectifSelection = ref<number | null>(null)
const activiteQuery = ref('')
const objectifQuery = ref('')

const activiteSuggestionOptions = computed(() => activiteSuggestions.value)
const objectifSuggestionOptions = computed(() => objectifSuggestions.value)

function formatAutocomplete(option: ActiviteAutocomplete): string {
    const code = option.code ?? ''
    const libelle = option.libelle ?? ''
    return option.libelleSecondaire
        ? `${code} — ${libelle} (${option.libelleSecondaire})`
        : `${code} — ${libelle}`
}

let minuteurAutocompleteActivite: ReturnType<typeof setTimeout> | null = null
let minuteurAutocompleteObjectif: ReturnType<typeof setTimeout> | null = null
let requeteAutocompleteActivite = 0
let requeteAutocompleteObjectif = 0

function rechercherActivites(terme: string): void {
    activiteQuery.value = terme
    filtres.search = terme
    activiteSuggestions.value = []
    if (minuteurAutocompleteActivite) clearTimeout(minuteurAutocompleteActivite)
    const jeton = ++requeteAutocompleteActivite
    if (!terme.trim()) return

    minuteurAutocompleteActivite = setTimeout(async () => {
        const reponse = await activiteService.autocompleterActivites(terme, vue.value === 'PTA')
        if (jeton === requeteAutocompleteActivite) {
            activiteSuggestions.value = reponse.success ? reponse.data : []
        }
    }, DELAI_RECHERCHE)
}

function rechercherObjectifs(terme: string): void {
    objectifQuery.value = terme
    filtres.objectifSearch = terme
    objectifSuggestions.value = []
    if (minuteurAutocompleteObjectif) clearTimeout(minuteurAutocompleteObjectif)
    const jeton = ++requeteAutocompleteObjectif
    if (!terme.trim()) return

    minuteurAutocompleteObjectif = setTimeout(async () => {
        const reponse = await activiteService.autocompleterObjectifs(terme)
        if (jeton === requeteAutocompleteObjectif) {
            objectifSuggestions.value = reponse.success ? reponse.data : []
        }
    }, DELAI_RECHERCHE)
}

function selectionnerActivite(valeur: number | null): void {
    activiteSelection.value = valeur
    if (valeur === null) return
    const suggestion = activiteSuggestions.value.find(item => item.id === valeur)
    if (!suggestion) return
    filtres.search = suggestion.code ?? ''
    activiteQuery.value = filtres.search
    appliquerFiltres()
}

function selectionnerObjectif(valeur: number | null): void {
    objectifSelection.value = valeur
    if (valeur === null) {
        filtres.objectifSpecifiqueId = null
        return
    }
    filtres.objectifSpecifiqueId = valeur
    filtres.objectifSearch = ''
    objectifQuery.value = ''
    appliquerFiltres()
}

// ------------------------------------------------------------------
// Chargement
// ------------------------------------------------------------------

let minuteurFiltre: ReturnType<typeof setTimeout> | null = null
let compteurRequete = 0

async function charger() {
    prioriteEnEdition.value = null
    const jeton = ++compteurRequete
    chargement.value = true
    erreur.value = null

    let reponsePage: Awaited<ReturnType<typeof activiteService.listerActivites>>
    let reponseStats: Awaited<ReturnType<typeof activiteService.statistiquesActivites>>

    try {
        ;[reponsePage, reponseStats] = await Promise.all([
            activiteService.listerActivites(
                { ...filtres }, vue.value, statutActif.value, page.value, TAILLE_PAGE,
            ),
            activiteService.statistiquesActivites({ ...filtres }, vue.value, statutActif.value),
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
        if (reponse.success) options.value = reponse.data
    } catch {
        // silencieux : la liste reste utilisable sans les référentiels.
    }
}

// ------------------------------------------------------------------
// Actions
// ------------------------------------------------------------------

function basculerPanneauFiltres() {
    filtresOuverts.value = !filtresOuverts.value
}

function appliquerFiltres() {
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

function voirDetail(id: number) {
    router.push({ name: 'activite-detail', params: { id } })
}

/**
 * Bouton principal : ouvre le formulaire de création.
 */
function ouvrirCreation() {
    router.push({ name: 'activites-nouvelle' })
}

function basculerMenuExport() {
    if (exportEnCours.value) return
    exportMenuOuvert.value = !exportMenuOuvert.value
}

async function lancerExport(format: 'excel' | 'pdf') {
    exportMenuOuvert.value = false

    if (exportEnCours.value) return

    exportEnCours.value = true

    try {
        const reponse = await activiteService.exporterActivites(
            { ...filtres }, vue.value, statutActif.value,
        )

        if (!reponse.success) {
            erreur.value = reponse.error
            return
        }

        const donnees: ActiviteExportResponse = reponse.data

        if (donnees.activites.length === 0) {
            avertissement.value = 'Aucune activité ne correspond aux filtres sélectionnés.'
            return
        }

        const { exporterExcel, exporterPdf } = await import('@/utils/exportActivites')

        if (format === 'excel') {
            exporterExcel(donnees)
            succes.value = 'Le fichier Excel a été généré.'
        } else {
            exporterPdf(donnees)
            succes.value = 'Le ou les fichiers PDF ont été générés.'
        }
    } catch {
        erreur.value = 'Export impossible. Réessayez dans un instant.'
    } finally {
        exportEnCours.value = false
    }
}

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

const libelleStatut = (code: string): string => {
    if (code === EN_RETARD) return 'En retard'
    const option = options.value.statuts.find(o => o.code === code)
    return option?.libelle ?? code
}

function retirerStatut() {
    statutActif.value = null
    page.value = 1
    void charger()
}

function changerVue(nouvelle: VueActivite) {
    if (vue.value === nouvelle) return
    vue.value = nouvelle
    activiteSuggestions.value = []
    if (activiteQuery.value.trim()) rechercherActivites(activiteQuery.value)
    filtres.objectifSearch = ''
    filtres.objectifSpecifiqueId = null
    objectifQuery.value = ''
    objectifSelection.value = null
    objectifSuggestions.value = []
    requeteAutocompleteObjectif++
    if (minuteurAutocompleteObjectif) clearTimeout(minuteurAutocompleteObjectif)
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
    activiteQuery.value = ''
    objectifQuery.value = ''
    activiteSelection.value = null
    objectifSelection.value = null
    activiteSuggestions.value = []
    objectifSuggestions.value = []
    requeteAutocompleteActivite++
    requeteAutocompleteObjectif++
    if (minuteurAutocompleteActivite) clearTimeout(minuteurAutocompleteActivite)
    if (minuteurAutocompleteObjectif) clearTimeout(minuteurAutocompleteObjectif)
    page.value = 1
    void charger()
}

// ------------------------------------------------------------------
// Priorité : édition inline + confirmation
// ------------------------------------------------------------------

function ouvrirEditionPriorite(item: ActiviteListItem, event: MouseEvent) {
    if (prioriteEnEdition.value?.id === item.id) {
        prioriteEnEdition.value = null
        return
    }

    prioriteEnEdition.value = item

    const cible = event.currentTarget as HTMLElement
    const rect = cible.getBoundingClientRect()
    const largeur = 200
    const nbItems = options.value.priorites.length + 1 // + "Aucune"
    const hauteurEstimee = Math.min(nbItems * 34 + 12, 320)

    let top = rect.bottom + 4
    if (top + hauteurEstimee > window.innerHeight - 8) {
        top = Math.max(8, rect.top - hauteurEstimee - 4)
    }
    const left = Math.min(rect.left, window.innerWidth - largeur - 8)

    positionDropdown.value = { top, left }
}

function fermerEditionPriorite() {
    prioriteEnEdition.value = null
}

function choisirPriorite(item: ActiviteListItem, nouvelle: ActiviteOption | null) {
    prioriteEnEdition.value = null

    const idActuelle = item.priorite?.id ?? null
    const idNouvelle = nouvelle?.id ?? null
    if (idActuelle === idNouvelle) return

    const nomActuelle = item.priorite?.libelle ?? 'Aucune'
    const nomNouvelle = nouvelle?.libelle ?? 'Aucune'

    confirmation.demander({
        titre: 'Changer la priorité',
        message: `La priorité de l'activité « ${item.code} » passera de « ${nomActuelle} » à « ${nomNouvelle} ».`,
        consequence: 'Le changement est immédiat et visible de tous les utilisateurs.',
        libelleConfirmer: 'Changer',
        ton: 'primary',
        icone: 'bi bi-arrow-repeat',
        action: () => confirmerChangementPriorite(item, idNouvelle),
    })
}

async function confirmerChangementPriorite(item: ActiviteListItem, idPriorite: number | null) {
    const reponse = await activiteService.changerPrioriteActivite(item.id, idPriorite)

    if (!reponse.success) {
        erreur.value = reponse.error
        return
    }

    // Recharge : source de vérité unique, cohérence garantie avec les compteurs.
    void charger()
}

// ------------------------------------------------------------------
// Demande de reprise
// ------------------------------------------------------------------

function ouvrirDemandeReprise(item: ActiviteListItem) {
    activitePourReprise.value = item
    motifReprise.value = ''
    modalReprise.value = true
}

function fermerModalReprise() {
    if (envoiReprise.value) return
    modalReprise.value = false
    activitePourReprise.value = null
    motifReprise.value = ''
}

async function confirmerDemandeReprise() {
    if (!activitePourReprise.value) return
    envoiReprise.value = true

    const reponse = await activiteService.demanderReprise(
        activitePourReprise.value.id,
        motifReprise.value.trim() || null,
    )

    envoiReprise.value = false

    if (!reponse.success) {
        erreur.value = reponse.error
        return
    }

    fermerModalReprise()
    void charger()
}

/** La demande de reprise n'a de sens que sur une activité non commencée. */
function peutDemanderReprise(item: ActiviteListItem): boolean {
    return item.statut?.code === 'NON_COMMENCEE'
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

function avancementAffiche(item: ActiviteListItem): number {
    return Math.round(item.avancement ?? 0)
}

/** Liste complète : "Aucune" + toutes les priorités actives. */
const prioritesDisponibles = computed<ActiviteOption[]>(() => options.value.priorites)

// ------------------------------------------------------------------
// Fermer le dropdown priorité au clic extérieur / scroll / resize
// ------------------------------------------------------------------

function onDocumentClick(e: MouseEvent) {
    const cible = e.target as HTMLElement | null
    if (!cible?.closest('.priorite-cell') && !cible?.closest('.priorite-dropdown')) {
        prioriteEnEdition.value = null
    }

    if (!cible?.closest('.export-menu')) {
        exportMenuOuvert.value = false
    }
}

onMounted(async () => {
    document.addEventListener('click', onDocumentClick)
    window.addEventListener('scroll', fermerEditionPriorite, true)
    window.addEventListener('resize', fermerEditionPriorite)
    await Promise.all([chargerOptions(), charger()])
})

onBeforeUnmount(() => {
    if (minuteurFiltre) clearTimeout(minuteurFiltre)
    if (minuteurAutocompleteActivite) clearTimeout(minuteurAutocompleteActivite)
    if (minuteurAutocompleteObjectif) clearTimeout(minuteurAutocompleteObjectif)
    document.removeEventListener('click', onDocumentClick)
    window.removeEventListener('scroll', fermerEditionPriorite, true)
    window.removeEventListener('resize', fermerEditionPriorite)
})

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
        <!-- Confirmation priorité -->
        <BaseConfirm v-model="confirmation.ouvert.value" :demande="confirmation.demande.value"
            :loading="confirmation.enCours.value" @confirme="confirmation.confirmer" @annule="confirmation.annuler" />

        <!-- ============ EN-TETE ============ -->
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>
                    Activités
                    <span class="wave" aria-hidden="true">✳</span>
                </h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    Gestion et suivi des activités DTS-TSS
                </p>
            </div>

            <div class="welcome-row__right">
                <div class="export-menu">
                    <BaseButton variant="secondary" :loading="exportEnCours" :disabled="chargement"
                        @click="basculerMenuExport">
                        <i class="bi bi-download"></i>
                        Exporter
                        <i class="bi bi-chevron-down export-menu__chev"
                            :class="{ 'export-menu__chev--open': exportMenuOuvert }"></i>
                    </BaseButton>

                    <ul v-if="exportMenuOuvert" class="export-menu__liste">
                        <li>
                            <button type="button" class="export-menu__item" @click="lancerExport('excel')">
                                <i class="bi bi-file-earmark-excel"></i>
                                <span>
                                    <strong>Excel</strong>
                                    <small>Classeur .xlsx (3 feuilles)</small>
                                </span>
                            </button>
                        </li>
                        <li>
                            <button type="button" class="export-menu__item" @click="lancerExport('pdf')">
                                <i class="bi bi-file-earmark-pdf"></i>
                                <span>
                                    <strong>PDF</strong>
                                    <small>Rapport .pdf</small>
                                </span>
                            </button>
                        </li>
                    </ul>
                </div>

                <BaseButton variant="primary" @click="ouvrirCreation">
                    <i class="bi bi-plus-lg"></i>
                    Ajouter une activité
                </BaseButton>
            </div>
        </header>

        <!-- ============ ERREUR ============ -->
        <!-- ============ BASCULE PTA / NON PTA ============ -->
        <div class="vue-switch mb-3" role="tablist" aria-label="Type d'activité">
            <button v-for="v in VUES" :key="v.value" type="button" role="tab" class="vue-switch__btn"
                :class="{ 'vue-switch__btn--active': vue === v.value }" :aria-selected="vue === v.value"
                @click="changerVue(v.value)">
                <i class="bi" :class="v.value === 'PTA' ? 'bi-clipboard-check' : 'bi-clipboard'"></i>
                {{ v.label }}
            </button>
        </div>

        <!-- ============ CARDS STATISTIQUES ============ -->
        <div class="row g-3 mb-4">
            <div v-for="carte in cartes" :key="carte.label" class="col-12 col-sm-6 col-lg-3">
                <article class="stat-card h-100" :class="[
                    `stat-card--${carte.variante}`,
                    {
                        'stat-card--active': estCarteActive(carte.cle),
                        'stat-card--static': carte.cle === null,
                    },
                ]">
                    <header class="stat-card__head">
                        <span class="stat-card__label">{{ carte.label }}</span>
                        <span class="stat-card__icon-wrap" aria-hidden="true">
                            <i class="stat-card__icon" :class="`bi ${carte.icone}`"></i>
                        </span>
                    </header>
                    <div class="stat-card__value">{{ carte.valeur }}</div>
                    <footer class="stat-card__hint">
                        {{ carte.cle ? 'Cliquer pour filtrer' : 'Toutes les activités' }}
                    </footer>

                    <template v-if="carte.cle">
                        <button type="button" class="stat-card__overlay" :aria-pressed="estCarteActive(carte.cle)"
                            @click="basculerStatut(carte.cle)">
                            <span class="visually-hidden">Filtrer sur {{ carte.label }}</span>
                        </button>
                    </template>
                </article>
            </div>
        </div>

        <!-- ============ FILTRES ============ -->
        <BaseCard class="filtres mb-4" noPadding>
            <header class="filtres__header">
                <button type="button" class="filtres__toggle" :aria-expanded="filtresOuverts"
                    aria-controls="filtres-body" @click="basculerPanneauFiltres">
                    <span class="filtres__header-icon" aria-hidden="true">
                        <i class="bi bi-sliders"></i>
                    </span>
                    <span class="filtres__header-text">
                        <span class="filtres__header-title">Filtres</span>
                        <span class="filtres__header-sub">
                            {{ filtresOuverts ? 'Cliquez pour replier' : 'Cliquez pour déplier' }}
                        </span>
                    </span>
                    <span v-if="nombreFiltresActifs > 0" class="filtres__count">
                        {{ nombreFiltresActifs }} {{ nombreFiltresActifs > 1 ? 'actifs' : 'actif' }}
                    </span>
                    <i class="bi bi-chevron-down filtres__chev" :class="{ 'filtres__chev--open': filtresOuverts }"
                        aria-hidden="true"></i>
                </button>

                <button v-if="nombreFiltresActifs > 0" type="button" class="filtres__clear" @click="reinitialiser">
                    <i class="bi bi-x-lg"></i>
                    Tout effacer
                </button>
            </header>

            <div id="filtres-body" class="filtres__collapse" :class="{ 'filtres__collapse--open': filtresOuverts }">
                <div class="filtres__collapse-inner">
                    <div class="filtres__body">
                        <div class="filtres__section">
                            <span class="filtres__section-label">Recherche</span>
                            <div class="filtres__section-grid">
                                <BaseSelect :model-value="activiteSelection ?? undefined" class="filtres__col"
                                    label="Recherche activité" placeholder="Code, référence ou désignation"
                                    icon="bi bi-search"
                                    :options="activiteSuggestionOptions" option-label="code" option-value="id"
                                    :option-formatter="formatAutocomplete" autocomplete remote clear-on-input clearable
                                    @search="rechercherActivites" @update:model-value="selectionnerActivite" />

                                <BaseSelect v-if="estVuePta" :model-value="objectifSelection ?? undefined" class="filtres__col"
                                    label="Recherche objectif spécifique" placeholder="Code ou désignation"
                                    icon="bi bi-search"
                                    :options="objectifSuggestionOptions" option-label="code" option-value="id"
                                    :option-formatter="formatAutocomplete" autocomplete remote clear-on-input clearable
                                    @search="rechercherObjectifs" @update:model-value="selectionnerObjectif" />
                            </div>
                        </div>

                        <div class="filtres__section">
                            <span class="filtres__section-label">Critères</span>
                            <div class="filtres__section-grid">
                                <div v-if="filtreServiceDisponible" class="filtres__col">
                                    <BaseSelect :model-value="filtres.serviceId ?? undefined" label="Service"
                                        placeholder="Tous les services" :options="optionsService" option-label="libelle"
                                        option-value="id"
                                        @update:model-value="filtres.serviceId = $event as number | null; appliquerFiltres()" />
                                </div>

                                <div class="filtres__col">
                                    <BaseSelect :model-value="filtres.prioriteId ?? undefined" label="Priorité"
                                        placeholder="Toutes" :options="optionsPriorite" option-label="libelle"
                                        option-value="id"
                                        @update:model-value="filtres.prioriteId = $event as number | null; appliquerFiltres()" />
                                </div>

                                <div class="filtres__col">
                                    <BaseSelect :model-value="filtres.typeActiviteId ?? undefined"
                                        label="Type d'activité" placeholder="Tous" :options="optionsType"
                                        option-label="libelle" option-value="id"
                                        @update:model-value="filtres.typeActiviteId = $event as number | null; appliquerFiltres()" />
                                </div>

                                <div class="filtres__col">
                                    <BaseSelect :model-value="filtres.siteId ?? undefined" label="Site"
                                        placeholder="Tous" :options="optionsSite" option-label="libelle"
                                        option-value="id"
                                        @update:model-value="filtres.siteId = $event as null | number; appliquerFiltres()" />
                                </div>
                            </div>
                        </div>

                        <div class="filtres__section">
                            <span class="filtres__section-label">Période</span>
                            <div class="filtres__section-grid filtres__section-grid--4">
                                <div class="filtres__col">
                                    <BaseSelect :model-value="filtres.annee" label="Année" :options="anneesOptions"
                                        option-label="libelle" option-value="id"
                                        @update:model-value="filtres.annee = $event as number; appliquerFiltres()" />
                                </div>

                                <div class="filtres__col">
                                    <BaseSelect :model-value="filtres.trimestre ?? undefined" label="Trimestre"
                                        placeholder="Tous" :options="trimestresOptions" option-label="libelle"
                                        option-value="id"
                                        @update:model-value="filtres.trimestre = ($event as Trimestre) ?? null; appliquerFiltres()" />
                                </div>

                                <div class="filtres__col">
                                    <BaseInput v-model="filtres.dateDebut" label="Date début" type="date"
                                        @change="appliquerFiltres()" />
                                </div>

                                <div class="filtres__col">
                                    <BaseInput v-model="filtres.dateFin" label="Date fin" type="date"
                                        @change="appliquerFiltres()" />
                                </div>
                            </div>
                        </div>
                    </div>

                    <footer class="filtres__footer">
                        <span v-if="statutActif" class="filtres__badge">
                            <i class="bi bi-funnel"></i>
                            {{ libelleStatut(statutActif) }}
                            <button type="button" @click="retirerStatut" title="Retirer le filtre de statut">
                                <i class="bi bi-x"></i>
                            </button>
                        </span>
                        <span v-else class="filtres__hint">
                            <i class="bi bi-info-circle"></i>
                            Les résultats se mettent à jour automatiquement
                        </span>

                        <div class="filtres__footer-actions">
                            <button type="button" class="filtres__reset" @click="reinitialiser">
                                <i class="bi bi-arrow-counterclockwise"></i>
                                Réinitialiser
                            </button>
                            <button type="button" class="filtres__close" @click="basculerPanneauFiltres">
                                <i class="bi bi-chevron-up"></i>
                                Replier
                            </button>
                        </div>
                    </footer>
                </div>
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
                <p class="empty-text">Aucune activité ne correspond aux critères sélectionnés.</p>
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

                            <td class="col-type-site">
                                <span class="cell-type">{{ item.typeActivite?.libelle ?? '—' }}</span>
                                <span class="cell-site">
                                    <i class="bi bi-geo-alt"></i>
                                    {{ item.site?.libelle ?? '—' }}
                                </span>
                            </td>

                            <!-- Priorité : badge cliquable -->
                            <td class="col-priorite">
                                <div class="priorite-cell">
                                    <button type="button" class="badge-soft badge-soft--clickable"
                                        :class="`badge-soft--${variantePriorite(item)}`"
                                        :aria-expanded="prioriteEnEdition?.id === item.id" aria-haspopup="listbox"
                                        @click.stop="ouvrirEditionPriorite(item, $event)">
                                        {{ item.priorite?.libelle ?? '—' }}
                                        <i class="bi bi-chevron-down priorite-chev"></i>
                                    </button>
                                </div>
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
                                    <span class="avancement__valeur">{{ avancementAffiche(item) }}%</span>
                                    <div class="avancement__bar" role="progressbar"
                                        :aria-valuenow="avancementAffiche(item)" aria-valuemin="0" aria-valuemax="100">
                                        <div class="avancement__fill" :style="{ width: avancementAffiche(item) + '%' }">
                                        </div>
                                    </div>
                                </div>
                            </td>

                            <td class="col-actions">
                                <div class="actions">
                                    <button type="button" class="btn-icone" title="Voir le détail"
                                        @click="voirDetail(item.id)">
                                        <i class="bi bi-eye"></i>
                                    </button>
                                    <button v-if="peutDemanderReprise(item)" type="button"
                                        class="btn-icone btn-icone--reprise" title="Demander une reprise"
                                        @click="ouvrirDemandeReprise(item)">
                                        <i class="bi bi-arrow-repeat"></i>
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

        <!-- ============ MODAL : DEMANDE DE REPRISE ============ -->
        <BaseModal v-model="modalReprise" title="Demander une reprise" size="md" :closeOnOutside="false">
            <div class="modal-reprise">
                <p class="modal-reprise__intro">
                    Vous demandez à devenir responsable de l'activité
                    <strong>{{ activitePourReprise?.code }}</strong>.
                </p>
                <p class="modal-reprise__subject">
                    {{ activitePourReprise?.designation }}
                </p>
                <BaseInput v-model="motifReprise" label="Motif (facultatif)" type="textarea" :rows="4"
                    placeholder="Expliquez brièvement pourquoi vous souhaitez reprendre cette activité…" />
                <p class="modal-reprise__hint">
                    Le responsable actuel recevra une notification. La demande peut être acceptée ou refusée.
                </p>
            </div>

            <template #footer>
                <BaseButton variant="secondary" :disabled="envoiReprise" @click="fermerModalReprise">
                    Annuler
                </BaseButton>
                <BaseButton variant="primary" :loading="envoiReprise" @click="confirmerDemandeReprise">
                    <i class="bi bi-send"></i>
                    Envoyer la demande
                </BaseButton>
            </template>
        </BaseModal>

        <!-- ============ DROPDOWN PRIORITÉ (téléporté dans body) ============ -->
        <Teleport to="body">
            <ul v-if="prioriteEnEdition" class="priorite-dropdown"
                :style="{ top: `${positionDropdown.top}px`, left: `${positionDropdown.left}px` }" role="listbox"
                aria-label="Changer la priorité" @click.stop>
                <li>
                    <button type="button" class="priorite-dropdown__item"
                        :class="{ 'priorite-dropdown__item--current': !prioriteEnEdition.priorite }"
                        @click="choisirPriorite(prioriteEnEdition, null)">
                        <span class="priorite-dropdown__dot priorite-dropdown__dot--muted"></span>
                        Aucune
                    </button>
                </li>
                <li v-for="p in prioritesDisponibles" :key="p.id">
                    <button type="button" class="priorite-dropdown__item"
                        :class="{ 'priorite-dropdown__item--current': prioriteEnEdition.priorite?.id === p.id }"
                        @click="choisirPriorite(prioriteEnEdition, p)">
                        <span class="priorite-dropdown__dot"
                            :class="`priorite-dropdown__dot--${COULEURS_PRIORITE[(p.code ?? '').toUpperCase()] ?? 'secondary'}`"></span>
                        {{ p.libelle }}
                        <i v-if="prioriteEnEdition.priorite?.id === p.id"
                            class="bi bi-check-lg priorite-dropdown__check"></i>
                    </button>
                </li>
            </ul>
        </Teleport>
    </div>
</template>

<style scoped>
.liste-activites {
    --primary-color: #1a2a6c;
    --secondary-color: #2a3f94;
    --bg-color: #f5f7fa;
    --surface-color: #fff;
    --card-bg: #fff;
    --text-color: #15233c;
    --text-muted-custom: #6b7280;
    --border-color: #e6ebf3;
    --radius-xl: 16px;
    min-height: calc(100vh - 68px);
    padding: 34px 38px 24px;
    color: #15233c;
    font-family: "DM Sans", "Segoe UI", sans-serif;
    background:
        radial-gradient(circle at 12% 8%, rgba(26, 42, 108, .055) 0, transparent 38%),
        radial-gradient(circle at 92% 94%, rgba(212, 175, 55, .065) 0, transparent 36%),
        #f5f7fa;
}

.export-menu {
    position: relative;
}

.export-menu__chev {
    font-size: 0.72rem;
    transition: transform .2s ease;
}

.export-menu__chev--open {
    transform: rotate(180deg);
}

.export-menu__liste {
    position: absolute;
    top: calc(100% + 8px);
    right: 0;
    z-index: 1100;
    min-width: 240px;
    margin: 0;
    padding: 0.4rem;
    list-style: none;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 12px;
    box-shadow: 0 12px 32px rgba(26, 42, 108, .16);
}

.export-menu__item {
    display: flex;
    align-items: center;
    gap: 0.7rem;
    width: 100%;
    padding: 0.6rem 0.7rem;
    border: none;
    border-radius: 8px;
    background: transparent;
    color: #15233c;
    text-align: left;
    cursor: pointer;
    transition: background .15s ease;
}

.export-menu__item:hover {
    background: #f4f7ff;
}

.export-menu__item i {
    font-size: 1.2rem;
    color: #1a2a6c;
}

.export-menu__item span {
    display: flex;
    flex-direction: column;
    line-height: 1.2;
}

.export-menu__item strong {
    font-size: 0.85rem;
}

.export-menu__item small {
    color: #6b7280;
    font-size: 0.72rem;
}

.vue-switch {
    display: inline-flex;
    gap: 6px;
    padding: 5px;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 12px;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04);
}

.vue-switch__btn {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.55rem 1.15rem;
    border: none;
    border-radius: 8px;
    background: transparent;
    color: #6b7280;
    font-size: 0.85rem;
    font-weight: 600;
    cursor: pointer;
    transition: background .15s ease, color .15s ease, box-shadow .15s ease;
}

.vue-switch__btn i {
    font-size: 1rem;
    color: #8a95a8;
    transition: color .15s ease;
}

.vue-switch__btn:hover:not(.vue-switch__btn--active) {
    background: #f7f9fd;
    color: #1a2a6c;
}

.vue-switch__btn:hover:not(.vue-switch__btn--active) i {
    color: #4a6cf7;
}

.vue-switch__btn--active {
    background: linear-gradient(135deg, #1a2a6c 0%, #2a3f94 100%);
    color: #fff;
    box-shadow: 0 4px 12px rgba(26, 42, 108, .25);
}

.vue-switch__btn--active i {
    color: #ffd04a;
}

/* --- Cards KPI --- */
.stat-card {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    overflow: hidden;
    min-height: 110px;
    padding: 1.15rem 1.2rem 1rem;
    background: #fff;
    border: 1px solid #e6ebf3 !important;
    border-top: 3px solid #c8d6fb !important;
    border-radius: 16px !important;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04), 0 8px 24px rgba(26, 42, 108, .06);
    transition: transform .2s ease, box-shadow .2s ease;
}

.stat-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 2px 4px rgba(26, 42, 108, .05), 0 14px 34px rgba(26, 42, 108, .10);
}

.stat-card__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 0.5rem;
}

.stat-card__label {
    color: #7b8799;
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.05em;
    line-height: 1.4;
    text-transform: uppercase;
}

.stat-card__icon-wrap {
    display: grid;
    width: 30px;
    height: 30px;
    flex: 0 0 30px;
    place-items: center;
    border-radius: 9px;
    background: #eaf0ff;
    color: #3b63d9;
}

.stat-card__icon {
    font-size: 1.05rem;
    line-height: 1;
}

.stat-card__value {
    font-size: 1.75rem;
    font-weight: 700;
    color: #1a2a6c;
    line-height: 1;
}

.stat-card__hint {
    margin-top: auto;
    color: #7b8799;
    font-size: 0.72rem;
}

.stat-card--primary {
    border-top-color: #4a6cf7 !important;
}

.stat-card--info {
    border-top-color: #72a8ed !important;
}

.stat-card--success {
    border-top-color: #55d6ad !important;
}

.stat-card--warning {
    border-top-color: #f4b91f !important;
}

.stat-card--danger {
    border-top-color: #ff8790 !important;
}

.stat-card--secondary {
    border-top-color: #c8d6fb !important;
}

.stat-card--purple {
    border-top-color: #a99aef !important;
}

.stat-card--info .stat-card__icon-wrap {
    background: #eaf0ff;
    color: #3b63d9;
}

.stat-card--success .stat-card__icon-wrap {
    background: #e4f7f0;
    color: #138a68;
}

.stat-card--warning .stat-card__icon-wrap {
    background: #fff6dd;
    color: #b8860b;
}

.stat-card--danger .stat-card__icon-wrap {
    background: #fff0f0;
    color: #d84e5b;
}

.stat-card--purple .stat-card__icon-wrap {
    background: #f1edff;
    color: #7a68d6;
}

.stat-card--secondary .stat-card__icon-wrap {
    background: #f1edff;
    color: #7a68d6;
}

.stat-card--success .stat-card__value {
    color: #138a68;
}

.stat-card--warning .stat-card__value {
    color: #b8860b;
}

.stat-card--danger .stat-card__value {
    color: #d84e5b;
}

.stat-card--info .stat-card__value {
    color: #3b63d9;
}

.stat-card--purple .stat-card__value {
    color: #7a68d6;
}

.stat-card--secondary .stat-card__value {
    color: #6b7280;
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
    outline: 2px solid #d4af37;
    outline-offset: 2px;
}

.stat-card--active {
    border-color: #4a6cf7 !important;
    border-top-color: #4a6cf7 !important;
    box-shadow: 0 0 0 3px rgba(74, 108, 247, .14), 0 8px 24px rgba(26, 42, 108, .08);
    transform: translateY(-2px);
}

/* --- Filtres --- */
.filtres {
    overflow: visible;
}

.filtres__header {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.9rem 1.25rem;
    background: linear-gradient(140deg, #f7f9fd, #fff);
    border-bottom: 1px solid #e6ebf3;
}

.filtres__toggle {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex: 1;
    min-width: 0;
    padding: 0.25rem;
    border: none;
    background: transparent;
    text-align: left;
    cursor: pointer;
    border-radius: 8px;
    transition: background .15s ease;
}

.filtres__toggle:hover {
    background: rgba(74, 108, 247, .04);
}

.filtres__toggle:focus-visible {
    outline: 2px solid #4a6cf7;
    outline-offset: 2px;
}

.filtres__header-icon {
    display: grid;
    place-items: center;
    width: 34px;
    height: 34px;
    border-radius: 9px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 1rem;
    flex: 0 0 34px;
}

.filtres__header-text {
    display: flex;
    flex-direction: column;
    min-width: 0;
    flex: 1;
}

.filtres__header-title {
    font-size: 0.95rem;
    font-weight: 700;
    color: #1a2a6c;
    font-family: Manrope, "DM Sans", sans-serif;
    line-height: 1.2;
}

.filtres__header-sub {
    font-size: 0.75rem;
    color: #6b7280;
    line-height: 1.2;
    margin-top: 2px;
}

.filtres__count {
    display: inline-flex;
    align-items: center;
    padding: 0.2rem 0.65rem;
    background: #eaf0ff;
    border: 1px solid #d9e2ff;
    border-radius: 20px;
    font-size: 0.72rem;
    font-weight: 600;
    color: #3b63d9;
    flex: none;
}

.filtres__chev {
    font-size: 0.9rem;
    color: #8a95a8;
    flex: none;
    transition: transform .25s ease, color .15s ease;
}

.filtres__chev--open {
    transform: rotate(180deg);
    color: #4a6cf7;
}

.filtres__clear {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.32rem 0.65rem;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    background: #fff;
    color: #6b7280;
    font-size: 0.75rem;
    font-weight: 600;
    cursor: pointer;
    flex: none;
    transition: background .15s, color .15s, border-color .15s;
}

.filtres__clear:hover {
    border-color: #d84e5b;
    color: #d84e5b;
    background: #fff0f0;
}

.filtres__collapse {
    display: grid;
    grid-template-rows: 0fr;
    transition: grid-template-rows .3s ease;
}

.filtres__collapse--open {
    grid-template-rows: 1fr;
}

.filtres__collapse-inner {
    overflow: hidden;
    min-height: 0;
}

.filtres__body {
    display: grid;
    gap: 1.25rem;
    padding: 1.25rem;
}

.filtres__section {
    display: grid;
    gap: 0.6rem;
}

.filtres__section-label {
    display: block;
    font-size: 0.68rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: #8a95a8;
}

.filtres__section-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1rem;
}

.filtres__section-grid--4 {
    grid-template-columns: repeat(4, minmax(0, 1fr));
}

.filtres__col {
    position: relative;
    min-width: 0;
}

.filtres__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
    padding: 0.85rem 1.25rem;
    background: #f7f9fd;
    border-top: 1px solid #e6ebf3;
}

.filtres__hint {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    font-size: 0.75rem;
    color: #8a95a8;
}

.filtres__footer-actions {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-left: auto;
}

.filtres__reset,
.filtres__close {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.45rem 0.9rem;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    background: #fff;
    color: #1a2a6c;
    font-size: 0.8rem;
    font-weight: 600;
    cursor: pointer;
    transition: background .15s, color .15s, border-color .15s;
}

.filtres__reset:hover {
    border-color: #4a6cf7;
    color: #4a6cf7;
    background: #f3f6fc;
}

.filtres__close {
    background: linear-gradient(135deg, #1a2a6c 0%, #2a3f94 100%);
    color: #fff;
    border-color: transparent;
}

.filtres__close:hover {
    background: linear-gradient(135deg, #22358a 0%, #3350b3 100%);
    color: #fff;
}

.filtres__badge {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.32rem 0.65rem;
    background: #eaf0ff;
    border: 1px solid #d9e2ff;
    border-radius: 20px;
    font-size: 0.78rem;
    font-weight: 600;
    color: #3b63d9;
}

.filtres__badge button {
    display: grid;
    place-items: center;
    width: 16px;
    height: 16px;
    padding: 0;
    border: none;
    border-radius: 50%;
    background: transparent;
    color: inherit;
    cursor: pointer;
    transition: background .15s;
}

.filtres__badge button:hover {
    background: rgba(59, 99, 217, .14);
}

/* --- Tableau --- */
.table-wrapper {
    width: 100%;
    overflow-x: auto;
    border-radius: 8px 8px 0 0;
}

.table-activites {
    width: 100%;
    border-collapse: collapse;
    min-width: 1050px;
    font-size: 0.82rem;
    table-layout: fixed;
}

.table-activites th,
.table-activites td {
    padding: 14px 8px;
    border-bottom: 1px solid #e6ebf3;
    vertical-align: middle;
    overflow: hidden;
    background: #fff;
}

.table-activites thead th {
    background: #f7f9fd;
    color: #7b8799;
    font-size: 9.5px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: .9px;
    white-space: nowrap;
}

.table-activites tbody tr:hover,
.table-activites tbody tr:hover td {
    background: #f7f9fd;
}

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
    width: 10%;
}

.col-statut {
    width: 9%;
}

.col-periode {
    width: 11%;
}

.col-avancement {
    width: 10%;
}

.col-actions {
    width: 7%;
    text-align: center;
}

.cell-code {
    display: block;
    font-weight: 700;
    color: #1a2a6c;
    font-size: 0.82rem;
}

.cell-reference {
    display: block;
    color: #8a95a8;
    font-size: 0.72rem;
    margin-top: 1px;
}

.cell-designation {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    text-overflow: ellipsis;
    line-height: 1.3;
    color: #40516b;
}

.cell-truncate {
    display: block;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.col-type-site .cell-type {
    display: block;
    color: #40516b;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.col-type-site .cell-site {
    display: block;
    color: #8a95a8;
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
    color: #6b7280;
    line-height: 1.25;
}

.cell-periode__arrow {
    color: #aeb8c6;
    font-size: 0.68rem;
    line-height: 1;
}

.objectif {
    display: flex;
    flex-direction: column;
    line-height: 1.3;
}

.objectif__code {
    font-weight: 600;
    color: #3b63d9;
    font-size: 0.78rem;
}

.objectif__libelle {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    color: #6b7280;
    font-size: 0.74rem;
}

.badge-soft {
    display: inline-block;
    padding: 0.15rem 0.45rem;
    border-radius: 20px;
    font-size: 0.7rem;
    font-weight: 600;
    white-space: nowrap;
}

.badge-soft--primary,
.badge-soft--info {
    background: #eaf0ff;
    color: #3b63d9;
}

.badge-soft--success {
    background: #e4f7f0;
    color: #138a68;
}

.badge-soft--warning {
    background: #fff6dd;
    color: #b8860b;
}

.badge-soft--danger {
    background: #fff0f0;
    color: #d84e5b;
}

.badge-soft--secondary,
.badge-soft--purple {
    background: #f1edff;
    color: #7a68d6;
}

/* --- Priorité : badge cliquable + dropdown (téléporté) --- */
.priorite-cell {
    position: relative;
    display: inline-block;
}

.badge-soft--clickable {
    display: inline-flex;
    align-items: center;
    gap: 0.25rem;
    border: none;
    cursor: pointer;
    font-family: inherit;
    transition: filter .15s ease, transform .1s ease;
}

.badge-soft--clickable:hover {
    filter: brightness(0.96);
    transform: translateY(-1px);
}

.badge-soft--clickable:focus-visible {
    outline: 2px solid #4a6cf7;
    outline-offset: 2px;
}

.priorite-chev {
    font-size: 0.6rem;
    opacity: 0.7;
}

.priorite-dropdown {
    position: fixed;
    z-index: 1080;
    min-width: 200px;
    max-height: 320px;
    overflow-y: auto;
    margin: 0;
    padding: 0.3rem;
    list-style: none;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 10px;
    box-shadow: 0 12px 28px rgba(26, 42, 108, .18);
    animation: fadeIn .12s ease-out;
}

@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(-4px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.priorite-dropdown__item {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    width: 100%;
    padding: 0.45rem 0.6rem;
    border: none;
    border-radius: 6px;
    background: transparent;
    text-align: left;
    font-size: 0.8rem;
    color: #40516b;
    cursor: pointer;
    transition: background .12s ease;
}

.priorite-dropdown__item:hover {
    background: #f3f6fc;
}

.priorite-dropdown__item--current {
    background: #edf1fd;
    color: #1a2a6c;
    font-weight: 600;
}

.priorite-dropdown__dot {
    flex: 0 0 8px;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #c8d6fb;
}

.priorite-dropdown__dot--danger {
    background: #d84e5b;
}

.priorite-dropdown__dot--warning {
    background: #f4b91f;
}

.priorite-dropdown__dot--primary {
    background: #4a6cf7;
}

.priorite-dropdown__dot--secondary {
    background: #a99aef;
}

.priorite-dropdown__dot--muted {
    background: #d1d5db;
}

.priorite-dropdown__check {
    margin-left: auto;
    color: #1a6fb0;
    font-size: 0.75rem;
}

.avancement {
    display: flex;
    flex-direction: column;
    gap: 3px;
}

.avancement__valeur {
    font-size: 0.72rem;
    font-weight: 600;
    color: #40516b;
    text-align: center;
}

.avancement__bar {
    width: 100%;
    height: 6px;
    background: #e6ebf3;
    border-radius: 5px;
    overflow: hidden;
}

.avancement__fill {
    height: 100%;
    background: linear-gradient(90deg, #4a6cf7, #d4af37);
    border-radius: 5px;
    transition: width 0.25s ease;
}

.actions {
    display: inline-flex;
    gap: 0.3rem;
    justify-content: center;
}

.btn-icone {
    width: 1.7rem;
    height: 1.7rem;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    background: #fff;
    color: #6b7280;
    font-size: 0.8rem;
    cursor: pointer;
    transition: all 0.15s ease;
}

.btn-icone:hover:not(:disabled) {
    border-color: #4a6cf7;
    color: #4a6cf7;
}

.btn-icone:disabled {
    opacity: 0.4;
    cursor: not-allowed;
}

.btn-icone--reprise:hover:not(:disabled) {
    border-color: #d4af37;
    color: #b8860b;
}

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
    color: #c8d6fb;
    margin-bottom: 0.75rem;
}

.empty-title {
    font-size: 1.05rem;
    font-weight: 600;
    color: #1a2a6c;
    margin: 0 0 0.35rem;
}

.empty-text {
    font-size: 0.85rem;
    color: #6b7280;
    margin: 0 0 1.1rem;
}

/* --- Modal reprise --- */
.modal-reprise {
    display: flex;
    flex-direction: column;
    gap: 0.9rem;
}

.modal-reprise__intro {
    margin: 0;
    font-size: 0.85rem;
    color: #40516b;
    line-height: 1.5;
}

.modal-reprise__intro strong {
    color: #1a2a6c;
}

.modal-reprise__subject {
    margin: 0;
    padding: 0.7rem 0.9rem;
    background: #f7f9fd;
    border-left: 3px solid #d4af37;
    border-radius: 6px;
    font-size: 0.82rem;
    color: #1a2a6c;
    font-weight: 600;
}

.modal-reprise__hint {
    margin: 0;
    font-size: 0.75rem;
    color: #8a95a8;
    line-height: 1.5;
}

/* --- Responsive --- */
@media (max-width: 1199.98px) {
    .liste-activites {
        padding-left: 24px;
        padding-right: 24px;
    }

    .table-activites {
        font-size: 0.78rem;
    }

    .table-activites th,
    .table-activites td {
        padding: 0.55rem 0.4rem;
    }
}

@media (max-width: 991.98px) {

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

    .table-activites {
        min-width: 760px;
    }

    .filtres__section-grid,
    .filtres__section-grid--4 {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 767.98px) {
    .liste-activites {
        padding: 24px 16px 20px;
    }

    .stat-card__value {
        font-size: 1.5rem;
    }

    .filtres__header {
        flex-wrap: wrap;
    }

    .filtres__toggle {
        flex-basis: 100%;
    }

    .filtres__clear {
        margin-left: auto;
    }

    .filtres__footer {
        flex-direction: column;
        align-items: stretch;
    }

    .filtres__footer-actions {
        margin-left: 0;
        flex-direction: column-reverse;
    }

    .filtres__reset,
    .filtres__close {
        justify-content: center;
    }
}

@media (max-width: 575.98px) {
    .liste-activites {
        padding: 20px 12px 16px;
    }

    .page-title {
        font-size: 1.35rem;
    }

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

    .table-activites {
        min-width: 620px;
    }

    .filtres__section-grid,
    .filtres__section-grid--4 {
        grid-template-columns: 1fr;
    }

    .filtres__header,
    .filtres__body,
    .filtres__footer {
        padding-left: 1rem;
        padding-right: 1rem;
    }

    .vue-switch {
        width: 100%;
    }

    .vue-switch__btn {
        flex: 1;
        justify-content: center;
        padding: 0.55rem 0.65rem;
        font-size: 0.8rem;
    }

    .filtres__header-title {
        font-size: 0.88rem;
    }

    .filtres__header-sub {
        font-size: 0.7rem;
    }

    .filtres__count {
        font-size: 0.68rem;
        padding: 0.15rem 0.55rem;
    }
}

@media (prefers-reduced-motion: reduce) {

    .filtres__collapse,
    .filtres__chev,
    .stat-card,
    .avancement__fill,
    .priorite-dropdown {
        transition: none !important;
        animation: none !important;
    }
}
</style>
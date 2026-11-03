<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { BaseButton, BaseConfirm, BaseTable } from '@/components/base'
import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'
import type { TableColumn } from '@/components/base/BaseTable/BaseTable.types'
import ModalAffecter from '@/components/ModalAffecter.vue'
import * as activiteService from '@/services/activite'
import { listerActivitesNonAffectees } from '@/services/affectation'
import type { ActiviteListItem } from '@/types/activite'
import { formatDateSeule } from '@/utils/date'

// ------------------------------------------------------------------
// Onglets
// ------------------------------------------------------------------

type OngletKey = 'crees' | 'a-valider' | 'a-affecter' | 'a-demarrer'

interface OngletDef {
    key: OngletKey
    label: string
    icone: string
    description: string
}

const ONGLETS: OngletDef[] = [
    { key: 'crees', label: 'Créées', icone: 'bi-file-earmark', description: 'Brouillons en attente de soumission' },
    { key: 'a-valider', label: 'À valider', icone: 'bi-shield-check', description: 'Activités en attente de votre décision' },
    { key: 'a-affecter', label: 'À affecter', icone: 'bi-person-plus', description: 'Activités validées sans responsable actif sur l\'activité' },
    { key: 'a-demarrer', label: 'À démarrer', icone: 'bi-play-circle', description: 'Activités validées avec un responsable prêt à démarrer' },
]

const route = useRoute()
const router = useRouter()

function resoudreOngletInitial(): OngletKey {
    const fromQuery = String(route.query.onglet ?? '')
    return ONGLETS.find(o => o.key === fromQuery)?.key ?? 'crees'
}

const ongletActif = ref<OngletKey>(resoudreOngletInitial())
const ongletCourant = computed(() => ONGLETS.find(o => o.key === ongletActif.value)!)

// ------------------------------------------------------------------
// Données
// ------------------------------------------------------------------

type LigneWorkflow = ActiviteListItem

const lignes = ref<LigneWorkflow[]>([])
const chargement = ref(false)
const erreur = ref<string | null>(null)
const succes = ref<string | null>(null)

const page = ref(1)
const taillePage = ref(20)
const totalElements = ref(0)
const totalPages = ref(0)

// ------------------------------------------------------------------
// Colonnes par onglet
// ------------------------------------------------------------------

const COLONNES_PAR_ONGLET: Record<OngletKey, TableColumn<LigneWorkflow>[]> = {
    'crees': [
        { key: 'activite', label: 'Activité', align: 'start' },
        { key: 'etapeSuivante', label: 'Étape suivante' },
        { key: 'etapePrecedente', label: 'Étape précédente' },
        { key: 'periode', label: 'Période prévue' },
        { key: 'actions', label: 'Actions', align: 'center' },
    ],
    'a-valider': [
        { key: 'activite', label: 'Activité', align: 'start' },
        { key: 'etapeCourante', label: 'Étape courante' },
        { key: 'etapeSuivante', label: 'Étape suivante' },
        { key: 'periode', label: 'Période prévue' },
        { key: 'commentaire', label: 'Commentaire' },
        { key: 'actions', label: 'Actions', align: 'center' },
    ],
    'a-affecter': [
        { key: 'activite', label: 'Activité', align: 'start' },
        { key: 'periode', label: 'Période prévue' },
        { key: 'actions', label: 'Actions', align: 'center' },
    ],
    'a-demarrer': [
        { key: 'activite', label: 'Activité', align: 'start' },
        { key: 'periode', label: 'Période prévue' },
        { key: 'avancement', label: 'Avancement' },
        { key: 'actions', label: 'Actions', align: 'center' },
    ],
}

const colonnesCourantes = computed(() => COLONNES_PAR_ONGLET[ongletActif.value])

// ------------------------------------------------------------------
// Sélection (Lot 1)
// ------------------------------------------------------------------

const selectionActive = computed(
    () => ongletActif.value === 'crees' || ongletActif.value === 'a-valider'
)

const selection = ref<Set<number>>(new Set())

const itemsSelectionnes = computed<LigneWorkflow[]>({
    get: () => lignes.value.filter(l => selection.value.has(l.id)),
    set: (items) => {
        selection.value = new Set(items.map(l => l.id))
    },
})

function toutSelectionner(): void {
    selection.value = new Set(lignes.value.map(l => l.id))
}

function toutDeselectionner(): void {
    selection.value = new Set()
}

// ------------------------------------------------------------------
// Éligibilité par action
// ------------------------------------------------------------------

function decisionContientRetour(item: LigneWorkflow): boolean {
    const decision = item.derniereValidation?.derniereDecision
    return typeof decision === 'string' && decision.toLowerCase().includes('retour')
}

const itemsSelectionnesPourValider = computed<LigneWorkflow[]>(() =>
    lignes.value.filter(l => selection.value.has(l.id))
)

const itemsSelectionnesPourSoumettre = itemsSelectionnesPourValider

const itemsSelectionnesPourRejeter = computed<LigneWorkflow[]>(() =>
    lignes.value.filter(l => selection.value.has(l.id) && !decisionContientRetour(l))
)

const itemsSelectionnesPourRenvoyer = computed<LigneWorkflow[]>(() =>
    lignes.value.filter(l => selection.value.has(l.id) && l.etapePrecedente != null)
)

const etapeSuivanteCommune = computed<string | null>(() => {
    const libelles = new Set<string>()
    for (const id of selection.value) {
        const libelle = lignes.value.find(l => l.id === id)?.etapeSuivante?.libelle
        if (libelle) libelles.add(libelle)
    }
    return libelles.size === 1 ? [...libelles][0]! : null
})

const etapePrecedenteCommune = computed<string | null>(() => {
    const libelles = new Set<string>()
    for (const id of selection.value) {
        const libelle = lignes.value.find(l => l.id === id)?.etapePrecedente?.libelle
        if (libelle) libelles.add(libelle)
    }
    return libelles.size === 1 ? [...libelles][0]! : null
})

// ------------------------------------------------------------------
// Confirmation d'action (unitaire + batch)
// ------------------------------------------------------------------

type SensAction = 'SOUMETTRE' | 'VALIDER' | 'REJETER' | 'RENVOYER' | 'DEMARRER' | 'SUPPRIMER'

interface ActionAConfirmer {
    sens: SensAction
    items: LigneWorkflow[]
}

const actionEnCours = ref<ActionAConfirmer | null>(null)
const confirmOpen = ref(false)
const confirmDemande = ref<ConfirmDemande | null>(null)
const traitementEnCours = ref(false)
const enCours = ref<Set<number>>(new Set())

function demanderAction(sens: SensAction, items: LigneWorkflow[]): void {
    if (!items.length) return
    actionEnCours.value = { sens, items }
    confirmDemande.value = construireDemande(sens, items)
    confirmOpen.value = true
}

function construireDemande(sens: SensAction, items: LigneWorkflow[]): ConfirmDemande {
    if (items.length === 1) return construireDemandeUnitaire(sens, items[0]!)
    return construireDemandeBatch(sens, items)
}

function construireDemandeUnitaire(sens: SensAction, item: LigneWorkflow): ConfirmDemande {
    switch (sens) {
        case 'SOUMETTRE':
            return {
                titre: 'Soumettre l\'activité',
                message: `L'activité « ${item.code} » sera soumise au circuit de validation.`,
                libelleConfirmer: 'Soumettre',
                ton: 'primary',
                icone: 'bi-send',
                consequence: item.etapeSuivante
                    ? `Elle passera à l'étape : ${item.etapeSuivante.libelle}. Vous ne pourrez plus la modifier tant qu'elle est dans le circuit.`
                    : 'Elle passera en validation finale et ne sera plus modifiable.',
            }
        case 'VALIDER':
            return {
                titre: 'Valider l\'activité',
                message: `Vous allez valider l'activité « ${item.code} »${item.etapeCourante ? ` à l'étape « ${item.etapeCourante.libelle} »` : ''}.`,
                libelleConfirmer: 'Valider',
                ton: 'primary',
                icone: 'bi-check-circle',
                consequence: item.etapeSuivante
                    ? `Elle passera à l'étape suivante : ${item.etapeSuivante.libelle}.`
                    : 'La validation finale la rendra NON_COMMENCEE, prête à être affectée.',
            }
        case 'REJETER':
            return {
                titre: 'Rejeter l\'activité',
                message: `Vous allez rejeter l'activité « ${item.code} ».`,
                libelleConfirmer: 'Rejeter',
                ton: 'danger',
                icone: 'bi-x-circle',
                consequence: 'Elle ne pourra pas être validée en l\'état. L\'auteur devra la reprendre.',
            }
        case 'RENVOYER':
            return {
                titre: 'Renvoyer l\'activité',
                message: `L'activité « ${item.code} » sera renvoyée à l'étape précédente${item.etapePrecedente ? ` (${item.etapePrecedente.libelle})` : ''}.`,
                libelleConfirmer: 'Renvoyer',
                ton: 'warning',
                icone: 'bi-arrow-counterclockwise',
                consequence: 'L\'auteur devra la corriger avant de la resoumettre.',
            }
        case 'DEMARRER':
            return {
                titre: 'Démarrer l\'activité',
                message: `L'activité « ${item.code} » passera au statut EN_COURS. Sa date de démarrage effective sera enregistrée.`,
                libelleConfirmer: 'Démarrer',
                ton: 'primary',
                icone: 'bi-play-circle',
                consequence: 'Les sous-activités ne seront pas démarrées automatiquement : chacune devra être démarrée par son responsable.',
            }
        case 'SUPPRIMER':
            return {
                titre: 'Supprimer définitivement',
                message: `L'activité « ${item.code} » — ${item.designation} sera supprimée définitivement.`,
                libelleConfirmer: 'Supprimer',
                ton: 'danger',
                icone: 'bi-trash',
                consequence: 'Cette action est irréversible. La sous-activité et toutes ses données seront perdues.',
            }
    }
}

function construireDemandeBatch(sens: SensAction, items: LigneWorkflow[]): ConfirmDemande {
    const n = items.length
    const pluriel = n > 1 ? 's' : ''
    switch (sens) {
        case 'SOUMETTRE':
            return {
                titre: `Soumettre ${n} activités`,
                message: `${n} activité${pluriel} ser${n > 1 ? 'ont' : 'a'} soumise${pluriel} au circuit de validation.`,
                libelleConfirmer: `Soumettre (${n})`,
                ton: 'primary',
                icone: 'bi-send',
                consequence: 'Elles ne pourront plus être modifiées tant qu\'elles sont dans le circuit.',
            }
        case 'VALIDER':
            return {
                titre: `Valider ${n} activités`,
                message: `Vous allez valider ${n} activité${pluriel}.`,
                libelleConfirmer: `Valider (${n})`,
                ton: 'primary',
                icone: 'bi-check-circle',
                consequence: 'Elles passeront à l\'étape suivante ou seront rendues NON_COMMENCEE.',
            }
        case 'REJETER':
            return {
                titre: `Rejeter ${n} activités`,
                message: `Vous allez rejeter ${n} activité${pluriel}.`,
                libelleConfirmer: `Rejeter (${n})`,
                ton: 'danger',
                icone: 'bi-x-circle',
                consequence: 'Elles ne pourront pas être validées en l\'état. Les auteurs devront les reprendre.',
            }
        case 'RENVOYER':
            return {
                titre: `Renvoyer ${n} activités`,
                message: `${n} activité${pluriel} ser${n > 1 ? 'ont' : 'a'} renvoyée${pluriel} à l'étape précédente.`,
                libelleConfirmer: `Renvoyer (${n})`,
                ton: 'warning',
                icone: 'bi-arrow-counterclockwise',
                consequence: 'Les auteurs devront les corriger avant de les resoumettre.',
            }
        case 'DEMARRER':
        case 'SUPPRIMER':
            return construireDemandeUnitaire(sens, items[0]!)
    }
}

// ------------------------------------------------------------------
// Exécution
// ------------------------------------------------------------------

async function confirmerAction(): Promise<void> {
    if (!actionEnCours.value) return
    const { sens, items } = actionEnCours.value
    traitementEnCours.value = true
    erreur.value = null
    succes.value = null

    let reussies = 0
    const refus: string[] = []
    let panne = false

    for (const item of items) {
        if (enCours.value.has(item.id)) continue
        enCours.value = new Set([...enCours.value, item.id])

        try {
            const rep = await executerAction(item.id, sens)
            if (rep.success) {
                reussies++
                retirer(item.id)
            } else {
                refus.push(`${item.code} : ${rep.error ?? 'refusée'}`)
                if (rep.status === 409) retirer(item.id)
            }
        } catch {
            refus.push(`${item.code} : connexion impossible`)
            panne = true
            break
        } finally {
            enCours.value = new Set([...enCours.value].filter(x => x !== item.id))
        }
    }

    traitementEnCours.value = false
    confirmOpen.value = false
    actionEnCours.value = null
    confirmDemande.value = null

    const verbe = verbeDeAction(sens)
    if (refus.length) {
        succes.value = reussies > 0
            ? `${reussies} activité(s) ${verbe}, ${refus.length} refusée(s).`
            : null
        erreur.value = refus.join(' · ')
    } else if (reussies > 0) {
        succes.value = `${reussies} activité(s) ${verbe}.`
        setTimeout(() => { succes.value = null }, 4000)
    }

    if (panne) {
        erreur.value = 'Connexion au serveur impossible. Réessayez dans un instant.'
    }

    if (!panne) void charger()
}

function verbeDeAction(sens: SensAction): string {
    switch (sens) {
        case 'SOUMETTRE': return 'soumise(s)'
        case 'VALIDER': return 'validée(s)'
        case 'REJETER': return 'rejetée(s)'
        case 'RENVOYER': return 'renvoyée(s)'
        case 'DEMARRER': return 'démarrée(s)'
        case 'SUPPRIMER': return 'supprimée(s)'
    }
}

async function executerAction(id: number, sens: SensAction) {
    switch (sens) {
        case 'SOUMETTRE': return activiteService.soumettreValidation(id, null)
        case 'VALIDER': return activiteService.validerActivite(id, false, null)
        case 'REJETER': return activiteService.validerActivite(id, true, null)
        case 'RENVOYER': return activiteService.soumettreValidationRetour(id, null)
        case 'DEMARRER': return activiteService.demarrerActivite(id)
        case 'SUPPRIMER': return activiteService.supprimerActivite(id)
    }
}

function retirer(id: number): void {
    lignes.value = lignes.value.filter(l => l.id !== id)
    selection.value = new Set([...selection.value].filter(x => x !== id))
}

function fermerConfirm(): void {
    actionEnCours.value = null
    confirmDemande.value = null
    confirmOpen.value = false
}

// ------------------------------------------------------------------
// Affecter (formulaire)
// ------------------------------------------------------------------

const modalAffectation = ref(false)
const activiteAAffecter = ref<LigneWorkflow | null>(null)

function ouvrirAffectation(item: LigneWorkflow): void {
    activiteAAffecter.value = item
    modalAffectation.value = true
}

function surAffectationReussie(): void {
    modalAffectation.value = false
    activiteAAffecter.value = null
    void charger()
}

// ------------------------------------------------------------------
// Row click → navigation
// ------------------------------------------------------------------

function onTableClick(event: MouseEvent): void {
    const target = event.target as HTMLElement
    if (target.closest('button, a, input, select, textarea')) return
    const tr = target.closest('tbody tr')
    if (!tr) return
    const tbody = tr.parentElement
    if (!tbody) return
    const index = Array.from(tbody.children).indexOf(tr)
    if (index < 0 || index >= lignes.value.length) return
    const item = lignes.value[index]
    if (item) voirDetail(item)
}

function voirDetail(item: LigneWorkflow): void {
    router.push({ name: 'activite-detail', params: { id: item.id } })
}

// ------------------------------------------------------------------
// Chargement
// ------------------------------------------------------------------

async function charger(): Promise<void> {
    chargement.value = true
    erreur.value = null
    selection.value = new Set()
    lignes.value = []

    try {
        switch (ongletActif.value) {
            case 'crees': await chargerCrees(); break
            case 'a-valider': await chargerAValider(); break
            case 'a-affecter': await chargerAAffecter(); break
            case 'a-demarrer': await chargerADemarrer(); break
        }
    } catch {
        erreur.value = 'Connexion au serveur impossible. Réessayez dans un instant.'
    } finally {
        chargement.value = false
    }
}

async function chargerCrees(): Promise<void> {
    const rep = await activiteService.listerActivitesASoumettre()
    if (!rep.success) { erreur.value = rep.error; return }
    lignes.value = rep.data.content.map((item: any) => ({
        ...item,
        etapeSuivante: item.etapeSuivante ?? null,
        etapePrecedente: item.etapePrecedente ?? null,
    }))
    totalElements.value = rep.data.totalElements
    totalPages.value = rep.data.totalPages
}

async function chargerAValider(): Promise<void> {
    const rep = await activiteService.listerActivitesAValider()
    if (!rep.success) { erreur.value = rep.error; return }
    const aplaties: LigneWorkflow[] = []
    for (const entree of rep.data.content) {
        for (const [designationEtape, item] of Object.entries(entree)) {
            const it = item as any
            aplaties.push({
                ...it,
                etapeCourante: it.etapeCourante ?? { id: 0, libelle: designationEtape },
                etapeSuivante: it.etapeSuivante ?? null,
                etapePrecedente: it.etapePrecedente ?? null,
            })
        }
    }
    lignes.value = aplaties
    totalElements.value = aplaties.length
    totalPages.value = 1
}

async function chargerAAffecter(): Promise<void> {
    const rep = await listerActivitesNonAffectees({
        pta: true,
        page: page.value - 1,
        size: taillePage.value,
    })
    if (!rep.success) { erreur.value = rep.error; return }
    lignes.value = rep.data.content
    totalElements.value = rep.data.totalElements
    totalPages.value = rep.data.totalPages
}

async function chargerADemarrer(): Promise<void> {
    const rep = await activiteService.listerActivitesADemarrer(
        filtresVides(), page.value, taillePage.value)
    if (!rep.success) { erreur.value = rep.error; return }
    lignes.value = rep.data.content
    totalElements.value = rep.data.totalElements
    totalPages.value = rep.data.totalPages
}

function filtresVides() {
    return {
        search: '', objectifSearch: '', objectifSpecifiqueId: null,
        serviceId: null, prioriteId: null, typeActiviteId: null, siteId: null,
        annee: 0, trimestre: null, dateDebut: '', dateFin: '',
    }
}

function changerOnglet(cle: OngletKey): void {
    if (ongletActif.value === cle) return
    ongletActif.value = cle
    selection.value = new Set()
    succes.value = null
    page.value = 1
    router.replace({ query: { ...route.query, onglet: cle } })
    void charger()
}

// ------------------------------------------------------------------
// Affichage
// ------------------------------------------------------------------

const dateDuJour = computed(() =>
    new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    }).format(new Date()).toUpperCase()
)

function formaterDate(valeur?: string | null): string {
    return formatDateSeule(valeur)
}

function pct(valeur?: number | null): number {
    return Math.round(valeur ?? 0)
}

onMounted(charger)
</script>

<template>
    <div class="traitement-activites">
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>
                    Traitement des activités
                    <span class="wave" aria-hidden="true">✳</span>
                </h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">{{ ongletCourant.description }}</p>
            </div>
            <div class="welcome-row__right">
                <BaseButton variant="secondary" :loading="chargement" :disabled="chargement" @click="charger">
                    <i class="bi bi-arrow-clockwise"></i>
                    Actualiser
                </BaseButton>
            </div>
        </header>

        <nav class="onglets" role="tablist">
            <button v-for="o in ONGLETS" :key="o.key" type="button" role="tab" class="onglet"
                :class="{ 'onglet--actif': ongletActif === o.key }" :aria-selected="ongletActif === o.key"
                @click="changerOnglet(o.key)">
                <i class="bi" :class="o.icone"></i>
                {{ o.label }}
            </button>
        </nav>

        <div v-if="succes" class="alert alert-success d-flex align-items-center gap-2" role="status">
            <i class="bi bi-check-circle"></i>
            <span>{{ succes }}</span>
            <button type="button" class="btn-close ms-auto" @click="succes = null"></button>
        </div>

        <div v-if="erreur" class="alert alert-danger d-flex align-items-center gap-2" role="alert">
            <i class="bi bi-exclamation-triangle"></i>
            <span>{{ erreur }}</span>
            <button type="button" class="btn-close ms-auto" @click="erreur = null"></button>
        </div>

        <div v-if="chargement && !lignes.length" class="detail-state">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="mt-3 mb-0">Chargement…</p>
        </div>

        <div v-else-if="!lignes.length && !erreur" class="vide">
            <i class="bi bi-inbox"></i>
            <p class="vide__titre">Aucune activité dans cet onglet</p>
            <p class="vide__detail">Les activités concernées apparaîtront ici.</p>
        </div>

        <template v-else>

            <!-- Barre de lot -->
            <div v-if="selectionActive && selection.size > 0" class="lot" role="toolbar"
                aria-label="Actions sur la sélection">
                <span class="lot__compte" role="status">
                    {{ selection.size }} / {{ lignes.length }} sélectionnée(s)
                </span>

                <div class="lot__actions">
                    <BaseButton variant="secondary" size="sm" @click="toutSelectionner">
                        <i class="bi bi-check2-square"></i>
                        Tout sélectionner
                    </BaseButton>
                    <BaseButton variant="secondary" size="sm" @click="toutDeselectionner">
                        <i class="bi bi-x-square"></i>
                        Tout désélectionner
                    </BaseButton>

                    <span class="lot__separateur" aria-hidden="true"></span>

                    <template v-if="ongletActif === 'crees'">
                        <BaseButton variant="primary" size="sm" :loading="traitementEnCours"
                            :disabled="traitementEnCours || itemsSelectionnesPourSoumettre.length === 0"
                            @click="demanderAction('SOUMETTRE', itemsSelectionnesPourSoumettre)">
                            <i class="bi bi-send"></i>
                            Soumettre{{ etapeSuivanteCommune ? ` (${etapeSuivanteCommune})` : '' }}
                        </BaseButton>
                    </template>

                    <template v-else-if="ongletActif === 'a-valider'">
                        <BaseButton variant="success" size="sm" :loading="traitementEnCours"
                            :disabled="traitementEnCours || itemsSelectionnesPourValider.length === 0"
                            @click="demanderAction('VALIDER', itemsSelectionnesPourValider)">
                            <i class="bi bi-check-circle"></i>
                            Valider
                        </BaseButton>
                        <BaseButton v-if="itemsSelectionnesPourRejeter.length > 0" variant="danger" size="sm"
                            :disabled="traitementEnCours"
                            @click="demanderAction('REJETER', itemsSelectionnesPourRejeter)">
                            <i class="bi bi-x-circle"></i>
                            Rejeter
                        </BaseButton>
                        <BaseButton v-if="itemsSelectionnesPourRenvoyer.length > 0" variant="warning" size="sm"
                            :disabled="traitementEnCours"
                            @click="demanderAction('RENVOYER', itemsSelectionnesPourRenvoyer)">
                            <i class="bi bi-arrow-counterclockwise"></i>
                            Renvoyer{{ etapePrecedenteCommune ? ` (${etapePrecedenteCommune})` : '' }}
                        </BaseButton>
                    </template>
                </div>
            </div>

            <!-- Tableau -->
            <div class="table-wrapper" @click="onTableClick">
                <BaseTable :items="lignes" :columns="colonnesCourantes" :loading="chargement" :page-size="taillePage"
                    :sortable="false" :multi-select="selectionActive" v-model:selectedItems="itemsSelectionnes"
                    custom-class="traitement-table">
                    <template #cell-activite="{ item }">
                        <div class="activite__lien">
                            <span class="activite__entete">
                                <span class="activite__code">{{ item.code }}</span>
                                <span v-if="item.reference" class="activite__ref">{{ item.reference }}</span>
                            </span>
                            <span class="activite__designation">{{ item.designation }}</span>
                        </div>
                    </template>

                    <template #cell-etapeCourante="{ item }">
                        <span v-if="item.etapeCourante" class="badge-soft badge-soft--primary">
                            {{ item.etapeCourante.libelle }}
                        </span>
                        <span v-else class="text-muted">—</span>
                    </template>

                    <template #cell-etapeSuivante="{ item }">
                        <span v-if="item.etapeSuivante" class="badge-soft badge-soft--primary">
                            {{ item.etapeSuivante.libelle }}
                        </span>
                        <span v-else class="badge-soft badge-soft--success">Validation finale</span>
                    </template>

                    <template #cell-etapePrecedente="{ item }">
                        <span v-if="item.etapePrecedente" class="badge-soft badge-soft--warning">
                            {{ item.etapePrecedente.libelle }}
                        </span>
                        <span v-else class="text-muted">—</span>
                    </template>

                    <template #cell-periode="{ item }">
                        <template v-if="item.dateDebutPrevue">
                            <span class="periode">{{ formaterDate(item.dateDebutPrevue) }}</span>
                            <span class="periode__arrow">→</span>
                            <span class="periode">{{ formaterDate(item.dateFinPrevue) }}</span>
                        </template>
                        <span v-else class="text-muted">—</span>
                    </template>

                    <template #cell-commentaire="{ item }">
                        <span v-if="item.derniereValidation?.commentaire" class="commentaire"
                            :title="item.derniereValidation.commentaire">
                            {{ item.derniereValidation.commentaire }}
                        </span>
                        <span v-else class="text-muted">—</span>
                    </template>

                    <template #cell-avancement="{ item }">
                        <div class="avancement">
                            <span class="avancement__valeur">{{ pct(item.avancement) }}%</span>
                            <div class="avancement__bar">
                                <div class="avancement__fill" :style="{ width: pct(item.avancement) + '%' }"></div>
                            </div>
                        </div>
                    </template>

                    <template #cell-actions="{ item }">
                        <div class="actions" @click.stop>
                            <template v-if="ongletActif === 'crees'">
                                <button type="button" class="btn-action btn-action--primary"
                                    :disabled="enCours.has(item.id)" @click="demanderAction('SOUMETTRE', [item])">
                                    <i class="bi bi-send"></i>
                                    Soumettre
                                </button>
                                <button type="button" class="btn-action btn-action--danger"
                                    :disabled="enCours.has(item.id)" @click="demanderAction('SUPPRIMER', [item])">
                                    <i class="bi bi-trash"></i>
                                    Supprimer
                                </button>
                            </template>

                            <template v-else-if="ongletActif === 'a-valider'">
                                <button type="button" class="btn-action btn-action--success"
                                    :disabled="enCours.has(item.id)" @click="demanderAction('VALIDER', [item])">
                                    <i class="bi bi-check-circle"></i>
                                    Valider
                                </button>
                                <button type="button" class="btn-action btn-action--danger"
                                    :disabled="enCours.has(item.id)" @click="demanderAction('REJETER', [item])">
                                    <i class="bi bi-x-circle"></i>
                                    Rejeter
                                </button>
                                <button v-if="item.etapePrecedente" type="button" class="btn-action btn-action--warning"
                                    :disabled="enCours.has(item.id)" @click="demanderAction('RENVOYER', [item])">
                                    <i class="bi bi-arrow-counterclockwise"></i>
                                    Renvoyer
                                </button>
                            </template>

                            <template v-else-if="ongletActif === 'a-affecter'">
                                <button type="button" class="btn-action btn-action--primary"
                                    @click="ouvrirAffectation(item)">
                                    <i class="bi bi-person-plus"></i>
                                    Affecter
                                </button>
                            </template>

                            <template v-else-if="ongletActif === 'a-demarrer'">
                                <button type="button" class="btn-action btn-action--success"
                                    :disabled="enCours.has(item.id)" @click="demanderAction('DEMARRER', [item])">
                                    <i class="bi bi-play-circle"></i>
                                    Démarrer
                                </button>
                            </template>
                        </div>
                    </template>
                </BaseTable>
            </div>
        </template>

        <BaseConfirm v-model="confirmOpen" :demande="confirmDemande" :loading="traitementEnCours"
            @confirme="confirmerAction" @annule="fermerConfirm" />

        <ModalAffecter v-if="activiteAAffecter" v-model="modalAffectation" :id-activite="activiteAAffecter.id"
            :code-activite="activiteAAffecter.code" :designation-activite="activiteAAffecter.designation"
            @affecte="surAffectationReussie" />
    </div>
</template>

<style scoped>
.traitement-activites {
    min-height: calc(100vh - 68px);
    padding: 34px 38px 24px;
    color: #15233c;
    font-family: "DM Sans", "Segoe UI", sans-serif;
    background:
        radial-gradient(circle at 12% 8%, rgba(26, 42, 108, .055) 0, transparent 38%),
        radial-gradient(circle at 92% 94%, rgba(212, 175, 55, .065) 0, transparent 36%),
        #f5f7fa;
}

.onglets {
    display: flex;
    gap: 0.25rem;
    padding: 0.35rem;
    margin-bottom: 1.25rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 12px;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04);
    overflow-x: auto;
    width: fit-content;
}

.onglet {
    display: inline-flex;
    align-items: center;
    gap: 0.45rem;
    padding: 0.55rem 1.1rem;
    background: transparent;
    border: none;
    border-radius: 8px;
    color: #6b7280;
    font-size: 0.85rem;
    font-weight: 600;
    cursor: pointer;
    font-family: inherit;
    transition: background .15s ease, color .15s ease;
    white-space: nowrap;
}

.onglet:hover:not(.onglet--actif) {
    background: #f7f9fd;
    color: #1a2a6c;
}

.onglet--actif {
    background: linear-gradient(135deg, #1a2a6c 0%, #2a3f94 100%);
    color: #fff;
    box-shadow: 0 4px 12px rgba(26, 42, 108, .25);
}

.onglet--actif i {
    color: #ffd04a;
}

.lot {
    display: flex;
    align-items: center;
    gap: 1rem;
    flex-wrap: wrap;
    padding: 0.7rem 1rem;
    margin-bottom: 1rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .04),
        0 8px 24px rgba(26, 42, 108, .06);
}

.lot__compte {
    font-size: 0.9rem;
    font-weight: 600;
    color: #1a2a6c;
    white-space: nowrap;
}

.lot__actions {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
    margin-left: auto;
}

.lot__separateur {
    width: 1px;
    align-self: stretch;
    background: #e6ebf3;
    margin: 0 0.25rem;
}

.table-wrapper :deep(.traitement-table) tbody tr {
    cursor: pointer;
    transition: background .12s ease;
}

.table-wrapper :deep(.traitement-table) tbody tr:hover {
    background: #f7f9fd !important;
}

.detail-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 3.5rem 1.5rem;
    text-align: center;
    color: #7b8799;
}

.vide {
    text-align: center;
    padding: 3rem 1rem;
    color: #7b8799;
}

.vide i {
    font-size: 2.5rem;
    opacity: 0.4;
}

.vide__titre {
    margin: 0.75rem 0 0;
    font-size: 1rem;
    font-weight: 600;
    color: #1a2a6c;
}

.vide__detail {
    margin: 0.25rem 0 0;
    font-size: 0.9rem;
}

.activite__lien {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
}

.activite__entete {
    display: flex;
    align-items: baseline;
    gap: 0.5rem;
    flex-wrap: wrap;
}

.activite__code {
    font-size: 0.78rem;
    font-weight: 700;
    color: #3b63d9;
    letter-spacing: 0.02em;
}

.activite__ref {
    font-size: 0.72rem;
    color: #8a95a8;
}

.activite__designation {
    font-size: 0.9rem;
    font-weight: 500;
    line-height: 1.3;
    overflow-wrap: anywhere;
}

.badge-soft {
    display: inline-flex;
    align-items: center;
    padding: 0.18rem 0.55rem;
    border-radius: 20px;
    font-size: 0.72rem;
    font-weight: 600;
    white-space: nowrap;
}

.badge-soft--primary {
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

.periode__arrow {
    margin: 0 0.35rem;
    color: #8a95a8;
}

.commentaire {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    font-size: 0.82rem;
    line-height: 1.35;
    color: #40516b;
    overflow-wrap: anywhere;
}

.avancement {
    display: flex;
    flex-direction: column;
    gap: 3px;
    min-width: 80px;
}

.avancement__valeur {
    font-size: 0.72rem;
    font-weight: 600;
    color: #40516b;
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
    gap: 0.35rem;
    align-items: center;
    flex-wrap: wrap;
    justify-content: center;
}

.btn-action {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
    padding: 0.32rem 0.65rem;
    border-radius: 7px;
    border: 1px solid transparent;
    font-size: 0.75rem;
    font-weight: 600;
    font-family: inherit;
    cursor: pointer;
    transition: background .15s, border-color .15s, color .15s;
    white-space: nowrap;
}

.btn-action:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.btn-action--primary {
    background: #edf1fd;
    color: #1a2a6c;
    border-color: #c8d6fb;
}

.btn-action--primary:hover:not(:disabled) {
    background: #dde4ff;
    border-color: #4a6cf7;
}

.btn-action--success {
    background: #e4f7f0;
    color: #138a68;
    border-color: #b7dfc1;
}

.btn-action--success:hover:not(:disabled) {
    background: #cfeee1;
    border-color: #55d6ad;
}

.btn-action--danger {
    background: #fff0f0;
    color: #d84e5b;
    border-color: #f6cfcc;
}

.btn-action--danger:hover:not(:disabled) {
    background: #fdd;
    border-color: #f59aa0;
}

.btn-action--warning {
    background: #fff6dd;
    color: #b8860b;
    border-color: #f4e2c2;
}

.btn-action--warning:hover:not(:disabled) {
    background: #fdedb8;
    border-color: #e0b030;
}

@media (max-width: 767.98px) {
    .traitement-activites {
        padding: 24px 16px 20px;
    }

    .onglet {
        flex: 1;
        justify-content: center;
        padding: 0.5rem 0.6rem;
        font-size: 0.75rem;
    }

    .onglet i {
        display: none;
    }

    .actions {
        flex-direction: column;
        gap: 0.25rem;
    }

    .btn-action {
        width: 100%;
        justify-content: center;
    }

    .lot {
        flex-direction: column;
        align-items: stretch;
    }

    .lot__actions {
        margin-left: 0;
    }

    .lot__separateur {
        display: none;
    }
}
</style>
<script setup lang="ts">
import { useToastMessage } from '@/composables/useToast'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { BaseButton, BaseCard, BaseConfirm } from '@/components/base'
import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'
import ModalAffecter from '@/components/ModalAffecter.vue'
import ModalAffecterSousActivite from '@/components/ModalAffecterSousActivite.vue'
import * as activiteService from '@/services/activite'
import * as affectationService from '@/services/affectation'
import type {
    AffectationDetail,
    ActiviteDetail,
    DecisionValidation,
    SousActiviteDetail,
    UtilisateurResume,
} from '@/types/activite'
import type { AffectationActivite } from '@/types/affectation'
import { BROUILLON } from '@/types/activite'
import { formatDateSeule } from '@/utils/date'
import DemandeRepriseButton from '../demandereprise/DemandeRepriseButton.vue'

// ------------------------------------------------------------------
// Constantes de présentation
// ------------------------------------------------------------------

const LIBELLES_DECISION: Record<DecisionValidation, string> = {
    EN_ATTENTE_VALIDATION: 'En attente de validation',
    VALIDE: 'Validé',
    REJETE: 'Rejeté',
    RETOUR_MODIFICATION: 'Retour de modification',
}

const COULEURS_DECISION: Record<DecisionValidation, string> = {
    EN_ATTENTE_VALIDATION: 'warning',
    VALIDE: 'success',
    REJETE: 'danger',
    RETOUR_MODIFICATION: 'warning',
}

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

type Onglet = 'details' | 'sous-activites' | 'indicateurs' | 'historique' | 'affectations'

// ------------------------------------------------------------------
// État
// ------------------------------------------------------------------

const route = useRoute()
const router = useRouter()

const idActivite = computed(() => Number(route.params.id))

const detail = ref<ActiviteDetail | null>(null)
const chargement = ref(false)
const erreur = useToastMessage('error')

/** Onglet actif — les données sont déjà chargées, le switch est instantané. */
const ongletActif = ref<Onglet>('details')

/** Modal d'affectation d'une sous-activité. */
const modalAffectSousActOpen = ref(false)
const sousActiviteCible = ref<SousActiviteDetail | null>(null)

/** Affectations de l'activité (onglet Affectations). */
const affectationsActivite = ref<AffectationActivite[]>([])
const chargementAffectations = ref(false)
const modalAffectActiviteOpen = ref(false)
const confirmDesaffectOpen = ref(false)
const confirmDesaffectDemande = ref<ConfirmDemande | null>(null)
const affectationADesaffecter = ref<AffectationActivite | null>(null)
const desaffectationEnCours = ref(false)

// ------------------------------------------------------------------
// Présentation
// ------------------------------------------------------------------

const avancementAffiche = computed(() =>
    Math.round(detail.value?.avancement ?? 0)
)

function varianteStatut(code?: string | null): string {
    if (!code) return 'secondary'
    return COULEURS_STATUT[code.toUpperCase()] || 'secondary'
}

function variantePriorite(code?: string | null): string {
    if (!code) return 'secondary'
    return COULEURS_PRIORITE[code.toUpperCase()] || 'secondary'
}

function nomComplet(u?: UtilisateurResume | null): string {
    if (!u) return '—'
    return [u.prenom, u.nom].filter(Boolean).join(' ') || '—'
}

function formaterDateHeure(valeur?: string | null): string {
    if (!valeur) return '—'

    const d = new Date(valeur)

    if (isNaN(d.getTime())) return '—'

    return new Intl.DateTimeFormat('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    }).format(d)
}

function periode(debut?: string | null, fin?: string | null): string {
    const d = formatDateSeule(debut)
    const f = formatDateSeule(fin)

    if (d === '—' && f === '—') return '—'
    return `${d} → ${f}`
}

function pct(valeur?: number | null): number {
    return Math.round(valeur ?? 0)
}

function responsablesActifs(sa: SousActiviteDetail): AffectationDetail[] {
    return sa.affectations.filter(a => a.dateDesaffectation == null)
}

function voirSousActivite(idSousActivite: number) {
    router.push({
        name: 'sous-activite-detail',
        params: { id: idActivite.value, sousActiviteId: idSousActivite },
    })
}

function ouvrirAffectationSousActivite(sa: SousActiviteDetail): void {
    sousActiviteCible.value = sa
    modalAffectSousActOpen.value = true
}

function surAffectationSousActivite(): void {
    charger()
}

const peutModifier = computed(() => detail.value?.statut?.code === BROUILLON)

function ouvrirFormulaire(): void {
    router.push({
        name: 'activite-modifier',
        params: { id: idActivite.value },
    })
}

// ------------------------------------------------------------------
// ------------------------------------------------------------------
// Affectations — présentation
// ------------------------------------------------------------------

const affectationsActuelles = computed(() =>
    affectationsActivite.value.filter(a => !a.dateDesaffectation)
)

function nomRole(a: AffectationActivite): string {
    return a.role?.libelle ?? a.role?.code ?? '—'
}

function nomUtilisateur(a: AffectationActivite): string {
    return a.utilisateur?.libelle ?? '—'
}

interface EvenementAffectation {
    id: string
    type: 'affectation' | 'desaffectation'
    date: string
    utilisateur: string
    role: string
}

const historiqueAffectations = computed<EvenementAffectation[]>(() => {
    const events: EvenementAffectation[] = []
    for (const a of affectationsActivite.value) {
        if (a.dateAffectation) {
            events.push({
                id: `a-${a.id}`,
                type: 'affectation',
                date: a.dateAffectation,
                utilisateur: nomUtilisateur(a),
                role: nomRole(a),
            })
        }
        if (a.dateDesaffectation) {
            events.push({
                id: `d-${a.id}`,
                type: 'desaffectation',
                date: a.dateDesaffectation,
                utilisateur: nomUtilisateur(a),
                role: nomRole(a),
            })
        }
    }
    return events.sort((x, y) => (y.date > x.date ? 1 : -1))
})

function formaterDateHeureAff(valeur?: string | null): string {
    if (!valeur) return '—'
    const d = new Date(valeur)
    if (isNaN(d.getTime())) return '—'
    return new Intl.DateTimeFormat('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    }).format(d)
}

function demanderDesaffectation(a: AffectationActivite) {
    affectationADesaffecter.value = a
    confirmDesaffectDemande.value = {
        titre: 'Désaffecter cette personne',
        message: `Retirer ${nomUtilisateur(a)} (${nomRole(a)}) de cette activité ?`,
        libelleConfirmer: 'Désaffecter',
        ton: 'danger',
        consequence: 'La personne ne sera plus affectée à ce rôle. L’historique est conservé.',
        icone: 'bi-person-dash',
    }
    confirmDesaffectOpen.value = true
}

async function confirmerDesaffectation() {
    const a = affectationADesaffecter.value
    if (!a) return
    desaffectationEnCours.value = true
    try {
        const reponse = await affectationService.desaffecterActivite(idActivite.value, a.id)
        if (reponse.success) {
            await chargerAffectations()
        }
    } finally {
        desaffectationEnCours.value = false
        confirmDesaffectOpen.value = false
        affectationADesaffecter.value = null
    }
}

function surAffectationActivite() {
    modalAffectActiviteOpen.value = false
    chargerAffectations()
}
// ------------------------------------------------------------------

async function charger() {
    chargement.value = true
    erreur.value = null
    detail.value = null

    const id = idActivite.value

    if (!Number.isInteger(id) || id <= 0) {
        erreur.value = "Identifiant d'activité invalide."
        chargement.value = false
        return
    }

    let reponse
    try {
        reponse = await activiteService.detailActivite(id)
    } catch {
        erreur.value = 'Connexion au serveur impossible. Réessayez dans un instant.'
        chargement.value = false
        return
    }

    if (!reponse.success) {
        erreur.value = reponse.error
        chargement.value = false
        return
    }

    detail.value = reponse.data
    chargement.value = false
}

async function chargerAffectations() {
    chargementAffectations.value = true
    try {
        const reponse = await affectationService.listerAffectationsActivite(idActivite.value)
        if (reponse.success) {
            affectationsActivite.value = reponse.data ?? []
        }
    } catch {
        affectationsActivite.value = []
    }
    chargementAffectations.value = false
}

const demarrageEnCours = ref(false)
const clotureEnCours = ref(false)

async function demarrerActiviteCourante(): Promise<void> {
    if (!detail.value) return
    demarrageEnCours.value = true
    const rep = await activiteService.demarrerActivite(detail.value.id)
    demarrageEnCours.value = false
    if (!rep.success) { erreur.value = rep.error; return }
    await charger()
}

async function cloturerActiviteCourante(): Promise<void> {
    if (!detail.value) return
    clotureEnCours.value = true
    const rep = await activiteService.cloturerActivite(detail.value.id)
    clotureEnCours.value = false
    if (!rep.success) { erreur.value = rep.error; return }
    await charger()
}

onMounted(() => {
    charger()
    chargerAffectations()
})
</script>

<template>
    <div class="detail-activite">
        <!-- ============ EN-TÊTE ============ -->
        <header class="detail-header">
            <div class="detail-header__main">
                <button type="button" class="btn-retour" @click="router.push({ name: 'activites' })">
                    <i class="bi bi-arrow-left"></i>
                    Retour à la liste
                </button>

                <div class="eyebrow">
                    <span class="live-dot"></span>
                    SUIVI DES ACTIVITÉS DTS-TSS
                </div>

                <h1 class="page-title">
                    Détail de l'activité
                    <span class="page-title__accent" aria-hidden="true">✳</span>
                </h1>

                <span class="title-divider" aria-hidden="true"></span>

                <p class="page-subtitle">
                    {{ detail
                        ? `${detail.code} · ${detail.reference}`
                        : chargement
                            ? 'Chargement…'
                            : 'Consultez les informations et le suivi de l’activité' }}
                </p>
            </div>

            <div class="detail-header__actions">
                <DemandeRepriseButton v-if="detail?.statut?.code?.toUpperCase() === 'NON_COMMENCEE'" kind="activite"
                    :id="idActivite" :deja-responsable="false" @cree="charger" />

                <BaseButton v-if="peutModifier" variant="secondary" @click="ouvrirFormulaire">
                    <i class="bi bi-pencil"></i>
                    Modifier
                </BaseButton>

                <BaseButton v-if="detail && detail.statut?.code === 'VALIDEE'" variant="primary"
                    :loading="demarrageEnCours" :disabled="demarrageEnCours" @click="demarrerActiviteCourante">
                    <i class="bi bi-play-circle"></i>
                    Démarrer l'activité
                </BaseButton>

                <BaseButton v-if="detail && detail.statut?.code === 'EN_COURS'" variant="success"
                    :loading="clotureEnCours" :disabled="clotureEnCours" @click="cloturerActiviteCourante">
                    <i class="bi bi-check-circle"></i>
                    Clôturer l'activité
                </BaseButton>

                <div v-if="detail" class="badges">
                    <span v-if="detail.statut" class="badge-soft"
                        :class="`badge-soft--${varianteStatut(detail.statut.code)}`">
                        {{ detail.statut.libelle }}
                    </span>

                    <span v-if="detail.priorite" class="badge-soft"
                        :class="`badge-soft--${variantePriorite(detail.priorite.code)}`">
                        {{ detail.priorite.libelle }}
                    </span>

                    <span class="badge-soft badge-soft--secondary">
                        {{ detail.pta ? 'PTA' : 'Non PTA' }}
                    </span>

                    <span v-if="detail.enRetard" class="badge-soft badge-soft--danger">
                        <i class="bi bi-exclamation-triangle-fill"></i>
                        En retard
                    </span>
                </div>
            </div>
        </header>

        <!-- ============ ERREUR ============ -->
        <!-- ============ CHARGEMENT ============ -->
        <div v-if="chargement" class="detail-state">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="mt-3 mb-0">Chargement des détails…</p>
        </div>

        <!-- ============ ERREUR FATALE ============ -->
        <div v-else-if="!detail && erreur" class="detail-state">
            <i class="bi bi-exclamation-circle empty-icon"></i>
            <h3 class="empty-title">Impossible d'afficher l'activité</h3>
            <p class="empty-text">{{ erreur }}</p>
            <BaseButton variant="primary" @click="charger">
                <i class="bi bi-arrow-repeat"></i>
                Réessayer
            </BaseButton>
        </div>

        <!-- ============ CONTENU ============ -->
        <template v-else-if="detail">

            <!-- Barre d'onglets -->
            <nav class="tabs-nav" role="tablist">
                <button type="button" :class="['tab-btn', { 'tab-btn--active': ongletActif === 'details' }]" role="tab"
                    :aria-selected="ongletActif === 'details'" @click="ongletActif = 'details'">
                    <i class="bi bi-info-circle"></i>
                    Détails
                </button>
                <button type="button" :class="['tab-btn', { 'tab-btn--active': ongletActif === 'sous-activites' }]"
                    role="tab" :aria-selected="ongletActif === 'sous-activites'"
                    @click="ongletActif = 'sous-activites'">
                    <i class="bi bi-list-check"></i>
                    Sous-activités
                    <span class="tab-btn__count">{{ detail.sousActivites.length }}</span>
                </button>
                <button type="button" :class="['tab-btn', { 'tab-btn--active': ongletActif === 'indicateurs' }]"
                    role="tab" :aria-selected="ongletActif === 'indicateurs'" @click="ongletActif = 'indicateurs'">
                    <i class="bi bi-bullseye"></i>
                    Indicateurs
                    <span class="tab-btn__count">{{ detail.indicateurs.length }}</span>
                </button>
                <button type="button" :class="['tab-btn', { 'tab-btn--active': ongletActif === 'affectations' }]"
                    role="tab" :aria-selected="ongletActif === 'affectations'" @click="ongletActif = 'affectations'">
                    <i class="bi bi-people"></i>
                    Affectations
                    <span class="tab-btn__count">{{ affectationsActuelles.length }}</span>
                </button>
                <button type="button" :class="['tab-btn', { 'tab-btn--active': ongletActif === 'historique' }]"
                    role="tab" :aria-selected="ongletActif === 'historique'" @click="ongletActif = 'historique'">
                    <i class="bi bi-clock-history"></i>
                    Historique
                </button>
            </nav>

            <!-- ============ ONGLET DÉTAILS ============ -->
            <div v-show="ongletActif === 'details'" class="tab-content">
                <div class="row g-4">
                    <!-- Informations générales -->
                    <div class="col-lg-12">
                        <BaseCard class="h-100">
                            <template #title>
                                <div class="section-head">
                                    <i class="bi bi-info-circle"></i>
                                    <h3 class="card-title h6 mb-0">Informations générales</h3>
                                </div>
                            </template>

                            <div class="info-grid">
                                <div class="info-item info-item--large">
                                    <span class="info-label">Désignation</span>
                                    <span class="info-value">{{ detail.designation }}</span>
                                </div>

                                <div v-if="detail.pta" class="info-item">
                                    <span class="info-label">Objectif spécifique</span>
                                    <span v-if="detail.objectifSpecifique" class="info-value">
                                        <span class="objectif-code">{{ detail.objectifSpecifique.code }}</span>
                                        {{ detail.objectifSpecifique.libelle }}
                                    </span>
                                    <span v-else class="info-value">—</span>
                                </div>

                                <div class="info-item">
                                    <span class="info-label">Service</span>
                                    <span class="info-value">{{ detail.service?.libelle ?? '—' }}</span>
                                </div>

                                <div class="info-item">
                                    <span class="info-label">Type d'activité</span>
                                    <span class="info-value">{{ detail.typeActivite?.libelle ?? '—' }}</span>
                                </div>

                                <div class="info-item">
                                    <span class="info-label">Site</span>
                                    <span class="info-value">{{ detail.site?.libelle ?? '—' }}</span>
                                </div>

                                <div class="info-item">
                                    <span class="info-label">Priorité</span>
                                    <span class="info-value">{{ detail.priorite?.libelle ?? '—' }}</span>
                                </div>

                                <div class="info-item">
                                    <span class="info-label">Période prévue</span>
                                    <span class="info-value">
                                        {{ periode(detail.dateDebutPrevue, detail.dateFinPrevue) }}
                                    </span>
                                </div>

                                <div class="info-item">
                                    <span class="info-label">Période réelle</span>
                                    <span class="info-value">
                                        {{ periode(detail.dateDebutReelle, detail.dateFinReelle) }}
                                    </span>
                                </div>

                                <div class="info-item">
                                    <span class="info-label">Date de création</span>
                                    <span class="info-value">{{ formaterDateHeure(detail.dateCreation) }}</span>
                                </div>
                            </div>
                        </BaseCard>
                    </div>

                    <!-- Avancement + Validations -->
                    <div class="col-lg-7 gap-4">
                        <BaseCard>
                            <template #title>
                                <div class="section-head">
                                    <i class="bi bi-graph-up"></i>
                                    <h3 class="card-title h6 mb-0">Avancement global</h3>
                                </div>
                            </template>

                            <div class="avancement">
                                <div class="avancement__row">
                                    <span class="avancement__label">
                                        Moyenne des derniers avancements des sous-activités
                                    </span>
                                    <span class="avancement__valeur">{{ avancementAffiche }}%</span>
                                </div>

                                <div class="avancement__bar" role="progressbar" :aria-valuenow="avancementAffiche"
                                    aria-valuemin="0" aria-valuemax="100">
                                    <div class="avancement__fill" :style="{ width: avancementAffiche + '%' }"></div>
                                </div>

                                <p v-if="detail.enRetard" class="retard-alert">
                                    <i class="bi bi-exclamation-triangle-fill"></i>
                                    Échéance dépassée sans statut terminal.
                                </p>
                            </div>
                        </BaseCard>

                    </div>
                    <div class="col-lg-5 gap-4">
                        <BaseCard class="h-100">
                            <template #title>
                                <div class="section-head">
                                    <i class="bi bi-flag"></i>
                                    <h3 class="card-title h6 mb-0">
                                        Résultats intermédiaires ({{ detail.resultatsIntermediaires.length }})
                                    </h3>
                                </div>
                            </template>

                            <div v-if="detail.resultatsIntermediaires.length" class="resultats">
                                <div v-for="r in detail.resultatsIntermediaires" :key="r.id" class="resultat">
                                    <i class="bi bi-check2-circle"></i>
                                    <span>{{ r.designation }}</span>
                                </div>
                            </div>

                            <p v-else class="vide">Aucun résultat intermédiaire pour cette activité.</p>
                        </BaseCard>
                    </div>
                </div>
            </div>

            <!-- ============ ONGLET SOUS-ACTIVITÉS ============ -->
            <div v-show="ongletActif === 'sous-activites'" class="tab-content">
                <BaseCard>
                    <template #title>
                        <div class="section-head">
                            <i class="bi bi-list-check"></i>
                            <h3 class="card-title h6 mb-0">
                                Sous-activités ({{ detail.sousActivites.length }})
                            </h3>
                        </div>
                    </template>

                    <div v-if="detail.sousActivites.length" class="table-wrapper">
                        <table class="table-sous-activites">
                            <thead>
                                <tr>
                                    <th class="col-code">Code</th>
                                    <th class="col-designation">Désignation</th>
                                    <th class="col-periode">Période prévue</th>
                                    <th class="col-responsables">Responsables</th>
                                    <th class="col-livrables">Livrables</th>
                                    <th class="col-avancement">Avancement</th>
                                    <th class="col-actions">Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="sa in detail.sousActivites" :key="sa.id">
                                    <td class="col-code">
                                        <span class="cell-code">{{ sa.code }}</span>
                                    </td>

                                    <td class="col-designation">
                                        <span class="cell-designation">{{ sa.designation }}</span>
                                    </td>

                                    <td class="col-periode">
                                        <span class="cell-periode">
                                            {{ periode(sa.dateDebutPrevue, sa.dateFinPrevue) }}
                                        </span>
                                    </td>

                                    <td class="col-responsables">
                                        <div v-if="responsablesActifs(sa).length" class="cell-responsables">
                                            <span v-for="resp in responsablesActifs(sa)" :key="resp.id"
                                                class="badge-soft badge-soft--info"
                                                :title="resp.role?.libelle ?? undefined">
                                                <i class="bi bi-person"></i>
                                                {{ nomComplet(resp.utilisateur) }}
                                            </span>
                                        </div>
                                        <span v-else class="text-muted">Aucun responsable</span>
                                    </td>

                                    <td class="col-livrables">
                                        <span v-if="sa.livrables.length">{{ sa.livrables.length }}</span>
                                        <span v-else class="text-muted">—</span>
                                    </td>

                                    <td class="col-avancement">
                                        <template v-if="sa.avancementCourant">
                                            <span class="cell-avancement__valeur">
                                                {{ pct(sa.avancementCourant.valeurPourcentage) }}%
                                            </span>
                                            <div class="cell-avancement__bar" role="progressbar"
                                                :aria-valuenow="pct(sa.avancementCourant.valeurPourcentage)"
                                                aria-valuemin="0" aria-valuemax="100">
                                                <div class="avancement__fill"
                                                    :style="{ width: pct(sa.avancementCourant.valeurPourcentage) + '%' }">
                                                </div>
                                            </div>
                                        </template>
                                        <span v-else class="text-muted">Jamais évalué</span>
                                    </td>

                                    <td class="col-actions">
                                        <div class="actions-cell">
                                            <button type="button" class="btn-icone"
                                                title="Voir le détail de la sous-activité"
                                                @click="voirSousActivite(sa.id)">
                                                <i class="bi bi-eye"></i>
                                            </button>
                                            <button type="button" class="btn-icone btn-icone--primary"
                                                title="Gérer les affectations"
                                                @click="ouvrirAffectationSousActivite(sa)">
                                                <i class="bi bi-people"></i>
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>

                    <p v-else class="vide">Aucune sous-activité pour cette activité.</p>
                </BaseCard>
            </div>

            <!-- ============ ONGLET INDICATEURS ============ -->
            <div v-show="ongletActif === 'indicateurs'" class="tab-content">
                <BaseCard>
                    <template #title>
                        <div class="section-head">
                            <i class="bi bi-bullseye"></i>
                            <h3 class="card-title h6 mb-0">Indicateurs ({{ detail.indicateurs.length }})</h3>
                        </div>
                    </template>

                    <div v-if="detail.indicateurs.length" class="indicateurs">
                        <div v-for="ind in detail.indicateurs" :key="ind.id" class="indicateur">
                            <div class="indicateur__head">
                                <span class="indicateur__code">{{ ind.code }}</span>
                                <span class="badge-soft"
                                    :class="ind.actif ? 'badge-soft--success' : 'badge-soft--secondary'">
                                    {{ ind.actif ? 'Actif' : 'Inactif' }}
                                </span>
                            </div>

                            <p class="indicateur__designation">{{ ind.designation }}</p>

                            <ul class="indicateur__meta">
                                <li v-if="ind.typeIndicateur">Type : {{ ind.typeIndicateur }}</li>
                                <li v-if="ind.uniteMesure">Unité : {{ ind.uniteMesure }}</li>
                                <li v-if="ind.frequenceVerification">Fréquence : {{ ind.frequenceVerification }}</li>
                                <li v-if="ind.valeurCible != null">Cible : {{ ind.valeurCible }}</li>
                                <li v-if="ind.seuilMin != null || ind.seuilMax != null">
                                    Seuils : {{ ind.seuilMin ?? '—' }} – {{ ind.seuilMax ?? '—' }}
                                </li>
                            </ul>

                            <div class="indicateur__courante">
                                <span class="indicateur__titre">Valeur courante</span>
                                <template v-if="ind.valeurCourante">
                                    <span class="indicateur__valeur">
                                        {{ ind.valeurCourante.valeur }}
                                        <template v-if="ind.uniteMesure">{{ ind.uniteMesure }}</template>
                                    </span>
                                    <span class="indicateur__periode">
                                        {{ periode(ind.valeurCourante.periodeDebut, ind.valeurCourante.periodeFin) }} ·
                                        {{ formaterDateHeure(ind.valeurCourante.dateSaisie) }}
                                    </span>
                                </template>
                                <span v-else class="indicateur__periode">Aucune mesure</span>

                                <RouterLink :to="{ name: 'indicateur-detail', params: { id: ind.id } }"
                                    class="indicateur__lien">
                                    Voir l'historique complet
                                    <i class="bi bi-arrow-right"></i>
                                </RouterLink>
                            </div>
                        </div>
                    </div>

                    <p v-else class="vide">Aucun indicateur rattaché à cette activité.</p>
                </BaseCard>
            </div>

            <!-- ============ ONGLET AFFECTATIONS ============ -->
            <div v-show="ongletActif === 'affectations'" class="tab-content">
                <BaseCard>
                    <template #title>
                        <div class="section-head">
                            <i class="bi bi-people"></i>
                            Affectations de l’activité
                        </div>
                    </template>
                    <div class="affectations-toolbar">
                        <BaseButton variant="primary" size="sm" @click="modalAffectActiviteOpen = true">
                            <i class="bi bi-person-plus"></i>
                            Gérer l’équipe
                        </BaseButton>
                        <span class="helper-text">
                            {{ affectationsActuelles.length }} personne(s) affectée(s) actuellement
                        </span>
                    </div>

                    <div v-if="chargementAffectations" class="loading">
                        <span class="spinner"></span> Chargement des affectations…
                    </div>

                    <div v-else class="affectations-roles">
                        <div class="role-block">
                            <div class="role-block__head">
                                <i class="bi bi-person-check-fill"></i>
                                <span class="role-block__title">Responsable</span>
                                <span class="badge-soft badge-soft--primary">Obligatoire · 1 personne</span>
                            </div>
                            <ul class="role-list">
                                <li v-for="a in affectationsActuelles.filter(x => x.role?.code === 'RESPONSABLE')"
                                    :key="a.id" class="role-item">
                                    <span class="role-item__name">{{ nomUtilisateur(a) }}</span>
                                    <span class="role-item__date">depuis le {{ formaterDateHeureAff(a.dateAffectation)
                                        }}</span>
                                    <BaseButton variant="danger" size="sm"
                                        :disabled="affectationsActuelles.filter(x => x.role?.code === 'RESPONSABLE').length <= 1"
                                        @click="demanderDesaffectation(a)">
                                        <i class="bi bi-person-dash"></i>
                                    </BaseButton>
                                </li>
                                <li v-if="!affectationsActuelles.some(x => x.role?.code === 'RESPONSABLE')"
                                    class="role-empty">
                                    Aucun responsable affecté.
                                </li>
                            </ul>
                        </div>

                        <div class="role-block">
                            <div class="role-block__head">
                                <i class="bi bi-person-plus"></i>
                                <span class="role-block__title">Suppléant</span>
                                <span class="badge-soft badge-soft--warning">Facultatif · 1 personne max</span>
                            </div>
                            <ul class="role-list">
                                <li v-for="a in affectationsActuelles.filter(x => x.role?.code === 'SUPPLEANT')"
                                    :key="a.id" class="role-item">
                                    <span class="role-item__name">{{ nomUtilisateur(a) }}</span>
                                    <span class="role-item__date">depuis le {{ formaterDateHeureAff(a.dateAffectation)
                                        }}</span>
                                    <BaseButton variant="danger" size="sm" @click="demanderDesaffectation(a)">
                                        <i class="bi bi-person-dash"></i>
                                    </BaseButton>
                                </li>
                                <li v-if="!affectationsActuelles.some(x => x.role?.code === 'SUPPLEANT')"
                                    class="role-empty">
                                    Aucun suppléant affecté.
                                </li>
                            </ul>
                        </div>

                        <div class="role-block">
                            <div class="role-block__head">
                                <i class="bi bi-people"></i>
                                <span class="role-block__title">Participants</span>
                                <span class="badge-soft badge-soft--secondary">Facultatif · plusieurs</span>
                            </div>
                            <ul class="role-list">
                                <li v-for="a in affectationsActuelles.filter(x => x.role?.code === 'PARTICIPANT')"
                                    :key="a.id" class="role-item">
                                    <span class="role-item__name">{{ nomUtilisateur(a) }}</span>
                                    <span class="role-item__date">depuis le {{ formaterDateHeureAff(a.dateAffectation)
                                        }}</span>
                                    <BaseButton variant="danger" size="sm" @click="demanderDesaffectation(a)">
                                        <i class="bi bi-person-dash"></i>
                                    </BaseButton>
                                </li>
                                <li v-if="!affectationsActuelles.some(x => x.role?.code === 'PARTICIPANT')"
                                    class="role-empty">
                                    Aucun participant affecté.
                                </li>
                            </ul>
                        </div>
                    </div>
                    <!-- Historique des affectations -->
                    <div class="affectations-historique">
                        <h4 class="affectations-historique__title">
                            <i class="bi bi-clock-history"></i>
                            Historique des affectations
                        </h4>
                        <div v-if="historiqueAffectations.length === 0" class="role-empty">
                            Aucun événement enregistré.
                        </div>
                        <ul v-else class="timeline">
                            <li v-for="e in historiqueAffectations" :key="e.id" class="timeline__item"
                                :class="`timeline__item--${e.type}`">
                                <span class="timeline__dot"></span>
                                <div class="timeline__content">
                                    <div class="timeline__head">
                                        <span class="timeline__user">{{ e.utilisateur }}</span>
                                        <span class="badge-soft"
                                            :class="e.type === 'affectation' ? 'badge-soft--success' : 'badge-soft--danger'">
                                            {{ e.type === 'affectation' ? 'Affectation' : 'Désaffectation' }}
                                        </span>
                                        <span class="badge-soft badge-soft--secondary">{{ e.role }}</span>
                                    </div>
                                    <div class="timeline__date">{{ formaterDateHeureAff(e.date) }}</div>
                                </div>
                            </li>
                        </ul>
                    </div>
                </BaseCard>
            </div>

            <!-- ============ ONGLET HISTORIQUE ============ -->
            <div v-show="ongletActif === 'historique'" class="tab-content">
                <div class="row g-4">
                    <div class="col-lg-5">
                        <BaseCard>
                            <template #title>
                                <div class="section-head">
                                    <i class="bi bi-shield-check"></i>
                                    <h3 class="card-title h6 mb-0">Circuit de validation</h3>
                                </div>
                            </template>

                            <div v-if="(detail.validations ?? []).length > 0" class="validations">
                                <div v-for="v in (detail.validations ?? [])" :key="v.id" class="validation">
                                    <div class="validation__head">
                                        <span class="validation__niveau">Étape {{ v.etape.niveau }}</span>
                                        <span class="badge-soft"
                                            :class="`badge-soft--${COULEURS_DECISION[v.decision]}`">
                                            {{ LIBELLES_DECISION[v.decision] }}
                                        </span>
                                    </div>

                                    <p class="validation__designation">{{ v.etape.designation }}</p>
                                    <p v-if="v.etape.procedureLibelle" class="validation__procedure">
                                        {{ v.etape.procedureLibelle }}
                                    </p>

                                    <ul class="validation__meta">
                                        <li>
                                            <i class="bi bi-send"></i>
                                            Demandé le {{ formaterDateHeure(v.dateDemande) }} par
                                            {{ nomComplet(v.demandeur) }}
                                        </li>
                                        <li>
                                            <i class="bi bi-check2-circle"></i>
                                            Décidé le {{ formaterDateHeure(v.dateDecision) }} par
                                            {{ nomComplet(v.decideur) }}
                                        </li>
                                    </ul>

                                    <p v-if="v.commentaire" class="validation__commentaire">{{ v.commentaire }}</p>
                                </div>
                            </div>

                            <p v-else class="vide">Aucune validation enregistrée pour cette activité.</p>
                        </BaseCard>
                    </div>

                    <div class="col-lg-7">
                        <BaseCard class="h-100">
                            <template #title>
                                <div class="section-head">
                                    <i class="bi bi-clock-history"></i>
                                    <h3 class="card-title h6 mb-0">
                                        Historique des statuts ({{ detail.historique.length }})
                                    </h3>
                                </div>
                            </template>

                            <div v-if="detail.historique.length" class="historique">
                                <div v-for="h in detail.historique" :key="h.id" class="historique__item"
                                    :class="{ 'historique__item--courant': h.etatActuel }">
                                    <div class="historique__ligne">
                                        <span v-if="h.statut" class="badge-soft"
                                            :class="`badge-soft--${varianteStatut(h.statut.code)}`">
                                            {{ h.statut.libelle }}
                                        </span>
                                        <span v-else class="badge-soft badge-soft--secondary">—</span>

                                        <span v-if="h.etatActuel" class="historique__courant">Statut actuel</span>
                                    </div>

                                    <div class="historique__meta">
                                        {{ formaterDateHeure(h.dateChangement) }} ·
                                        {{ nomComplet(h.utilisateur) }}
                                    </div>

                                    <p v-if="h.commentaire" class="historique__commentaire">{{ h.commentaire }}</p>
                                </div>
                            </div>

                            <p v-else class="vide">Aucun historique pour cette activité.</p>
                        </BaseCard>
                    </div>
                </div>
            </div>
        </template>

        <!-- Modal d'affectation sous-activité -->
        <ModalAffecterSousActivite v-if="sousActiviteCible" v-model="modalAffectSousActOpen" :id-activite="idActivite"
            :id-sous-activite="sousActiviteCible.id" :code-sous-activite="sousActiviteCible.code"
            :designation-sous-activite="sousActiviteCible.designation" @affecte="surAffectationSousActivite" />

        <!-- Modal d'affectation de l'activité -->
        <ModalAffecter v-if="true" v-model="modalAffectActiviteOpen" :id-activite="idActivite"
            :code-activite="detail?.code ?? ''" :designation-activite="detail?.designation ?? ''"
            @affecte="surAffectationActivite" />

        <!-- Confirmation de désaffectation -->
        <BaseConfirm v-model="confirmDesaffectOpen" :demande="confirmDesaffectDemande" :loading="desaffectationEnCours"
            @confirme="confirmerDesaffectation" @annule="confirmDesaffectOpen = false" />
    </div>
</template>

<style scoped>
.detail-activite {
    --primary-color: #1a2a6c;
    --accent-color: #d4af37;
    min-height: calc(100vh - 68px);
    padding: 34px 38px 24px;
    color: #15233c;
    font-family: "DM Sans", "Segoe UI", sans-serif;
    background:
        radial-gradient(circle at 12% 8%, rgba(26, 42, 108, .055) 0, transparent 38%),
        radial-gradient(circle at 92% 94%, rgba(212, 175, 55, .065) 0, transparent 36%),
        #f5f7fa;
}

/* ============================================================
   EN-TÊTE
   ============================================================ */
.detail-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 1.5rem;
    margin-bottom: 1.75rem;
}

.detail-header__main {
    min-width: 0;
}

.detail-header__actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: flex-end;
    gap: 0.75rem;
}

.btn-retour {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    margin: 0 0 1rem;
    padding: 0;
    border: none;
    background: transparent;
    color: #3b63d9;
    font-size: 0.82rem;
    font-weight: 600;
    cursor: pointer;
}

.btn-retour:hover {
    color: #1a2a6c;
}

.eyebrow {
    display: flex;
    align-items: center;
    gap: 0.55rem;
    margin-bottom: 0.45rem;
    color: #7b8799;
    font-size: 0.68rem;
    font-weight: 700;
    letter-spacing: 0.08em;
}

.live-dot {
    width: 8px;
    height: 8px;
    flex: 0 0 8px;
    border-radius: 50%;
    background: #d4af37;
    box-shadow: 0 0 0 4px rgba(212, 175, 55, .14);
}

.page-title {
    margin: 0;
    color: #1a2a6c;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: clamp(1.5rem, 3vw, 2rem);
    font-weight: 700;
    letter-spacing: -0.035em;
}

.page-title__accent {
    color: #d4af37;
    font-size: 1.15rem;
}

.title-divider {
    display: block;
    width: 42px;
    height: 3px;
    margin: 0.7rem 0;
    border-radius: 3px;
    background: linear-gradient(90deg, #1a2a6c, #d4af37);
}

.page-subtitle {
    margin: 0;
    color: #6b7280;
    font-size: 0.88rem;
}

.badges {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.5rem;
}

/* ============================================================
   ONGLETS
   ============================================================ */
.tabs-nav {
    display: flex;
    gap: 0.25rem;
    margin-bottom: 1.25rem;
    padding: 0.35rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 12px;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04);
    overflow-x: auto;
    width: fit-content;
}

.tab-btn {
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

.tab-btn:hover:not(.tab-btn--active) {
    background: #f7f9fd;
    color: #1a2a6c;
}

.tab-btn--active {
    background: linear-gradient(135deg, #1a2a6c 0%, #2a3f94 100%);
    color: #fff;
    box-shadow: 0 4px 12px rgba(26, 42, 108, .25);
}

.tab-btn i {
    font-size: 0.95rem;
}

.tab-btn--active i {
    color: #ffd04a;
}

.tab-btn__count {
    padding: 0.1rem 0.45rem;
    border-radius: 20px;
    background: rgba(0, 0, 0, .06);
    font-size: 0.7rem;
    font-variant-numeric: tabular-nums;
}

.tab-btn--active .tab-btn__count {
    background: rgba(255, 255, 255, .2);
    color: #fff;
}

.tab-content {
    animation: fadeIn .18s ease;
}

@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(3px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

/* ============================================================
   TITRES DE CARTES
   ============================================================ */
.section-head {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    border-bottom: 1px solid #eef2f6;
    padding-bottom: 0.7rem;
    margin-bottom: 0.15rem;
}

.section-head i {
    color: #1e88a8;
    font-size: 1rem;
}

.card-title {
    font-weight: 700;
    color: #1a2b3c;
}

/* ============================================================
   GRILLE D'INFORMATIONS
   ============================================================ */
.info-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.9rem 1.5rem;
}

.info-item--large {
    grid-column: 1 / -1;
}

.info-label {
    display: block;
    font-size: 0.68rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: #8a9aac;
    margin-bottom: 0.2rem;
}

.info-value {
    font-size: 0.85rem;
    color: #33475c;
    line-height: 1.45;
}

.objectif-code {
    font-weight: 600;
    color: #14657f;
}

/* ============================================================
   AVANCEMENT
   ============================================================ */
.avancement {
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
}

.avancement__row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;
}

.avancement__label {
    font-size: 0.8rem;
    color: #5a6b7f;
}

.avancement__valeur {
    font-size: 1.2rem;
    font-weight: 700;
    color: #1a2b3c;
}

.avancement__bar {
    width: 100%;
    height: 12px;
    background: #eef2f6;
    border-radius: 7px;
    overflow: hidden;
}

.avancement__fill {
    height: 100%;
    background: linear-gradient(90deg, #1e88a8, #21a0c4);
    border-radius: 7px;
    transition: width 0.25s ease;
}

.retard-alert {
    display: flex;
    align-items: center;
    gap: 0.45rem;
    margin: 0;
    padding: 0.55rem 0.75rem;
    border-radius: 7px;
    background: #fbe6e4;
    border: 1px solid #f6cfcc;
    color: #9c2a20;
    font-size: 0.8rem;
}

/* ============================================================
   BADGES
   ============================================================ */
.badge-soft {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
    padding: 0.18rem 0.5rem;
    border-radius: 20px;
    font-size: 0.72rem;
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

.badge-soft--purple {
    background: #ebe6f7;
    color: #523a91;
}

/* ============================================================
   VALIDATIONS
   ============================================================ */
.validations {
    display: flex;
    flex-direction: column;
    gap: 0.9rem;
}

.validation {
    border: 1px solid #e6ecf1;
    border-radius: 10px;
    padding: 0.9rem 1rem;
}

.validation__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;
}

.validation__niveau {
    font-size: 0.72rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.03em;
    color: #8a9aac;
}

.validation__designation {
    font-size: 0.9rem;
    font-weight: 600;
    color: #1a2b3c;
    margin: 0.4rem 0 0;
}

.validation__procedure {
    font-size: 0.78rem;
    color: #8a9aac;
    margin: 0.1rem 0 0;
}

.validation__meta {
    list-style: none;
    margin: 0.6rem 0 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
}

.validation__meta li {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    font-size: 0.78rem;
    color: #5a6b7f;
}

.validation__meta i {
    color: #b6c2ce;
}

.validation__commentaire {
    margin: 0.6rem 0 0;
    padding: 0.55rem 0.7rem;
    background: #f6f8fa;
    border-radius: 7px;
    font-size: 0.8rem;
    color: #5a6b7f;
}

/* ============================================================
   SOUS-ACTIVITÉS
   ============================================================ */
.table-wrapper {
    width: 100%;
    overflow-x: auto;
}

.table-sous-activites {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.82rem;
    table-layout: fixed;
}

.table-sous-activites th,
.table-sous-activites td {
    padding: 0.6rem 0.6rem;
    border-bottom: 1px solid #eef2f6;
    vertical-align: middle;
    overflow: hidden;
}

.table-sous-activites thead th {
    background: #f6f8fa;
    color: #5a6b7f;
    font-size: 0.7rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.03em;
    white-space: nowrap;
}

.table-sous-activites tbody tr:hover {
    background: #f9fbfc;
}

.col-code {
    width: 9%;
}

.col-designation {
    width: 24%;
}

.col-periode {
    width: 15%;
}

.col-responsables {
    width: 20%;
}

.col-livrables {
    width: 7%;
}

.col-avancement {
    width: 13%;
}

.col-actions {
    width: 12%;
}

.cell-code {
    font-weight: 700;
    color: #14657f;
    font-size: 0.78rem;
}

.cell-designation {
    display: block;
    font-weight: 600;
    color: #33475c;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.cell-periode {
    color: #5a6b7f;
    font-size: 0.76rem;
    white-space: nowrap;
}

.cell-responsables {
    display: flex;
    flex-wrap: wrap;
    gap: 0.35rem;
}

.cell-avancement__valeur {
    display: block;
    font-size: 0.78rem;
    font-weight: 600;
    color: #33475c;
    margin-bottom: 0.2rem;
}

.cell-avancement__bar {
    height: 6px;
    background: #eef2f6;
    border-radius: 3px;
    overflow: hidden;
}

/* ============================================================
   BOUTONS D'ACTION
   ============================================================ */
.actions-cell {
    display: flex;
    gap: 0.35rem;
    justify-content: flex-end;
}

.btn-icone {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 30px;
    height: 30px;
    border: 1px solid #e6ecf1;
    border-radius: 8px;
    background: #fff;
    color: #5a6b7f;
    cursor: pointer;
    transition: all 0.15s ease;
}

.btn-icone:hover {
    color: #14657f;
    border-color: #bcdcea;
    background: #f4fafc;
}

.btn-icone--primary {
    color: #3b63d9;
    border-color: #cde2f6;
    background: #f0f7ff;
}

.btn-icone--primary:hover {
    background: #e0efff;
    border-color: #4a6cf7;
    color: #1a2a6c;
}

/* ============================================================
   INDICATEURS
   ============================================================ */
.indicateur__titre {
    display: block;
    font-size: 0.68rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: #8a9aac;
    margin-bottom: 0.4rem;
}

.indicateurs {
    display: flex;
    flex-direction: column;
    gap: 0.9rem;
}

.indicateur {
    border: 1px solid #e6ecf1;
    border-radius: 10px;
    padding: 0.95rem 1.1rem;
}

.indicateur__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;
}

.indicateur__code {
    font-weight: 700;
    color: #14657f;
    font-size: 0.8rem;
}

.indicateur__designation {
    font-size: 0.92rem;
    font-weight: 600;
    color: #1a2b3c;
    margin: 0.4rem 0 0;
}

.indicateur__meta {
    list-style: none;
    margin: 0.5rem 0 0;
    padding: 0;
    display: flex;
    flex-wrap: wrap;
    gap: 0.25rem 1.2rem;
}

.indicateur__meta li {
    font-size: 0.78rem;
    color: #5a6b7f;
}

.indicateur__courante {
    margin-top: 0.7rem;
    padding: 0.65rem 0.75rem;
    background: #f6f8fa;
    border-radius: 8px;
}

.indicateur__valeur {
    font-size: 1.1rem;
    font-weight: 700;
    color: #1e88a8;
}

.indicateur__periode {
    font-size: 0.76rem;
    color: #8a9aac;
    margin-left: 0.5rem;
}

.indicateur__lien {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
    margin-top: 0.4rem;
    font-size: 0.76rem;
    color: #14657f;
    text-decoration: none;
    font-weight: 600;
}

.indicateur__lien:hover {
    color: #1e88a8;
    text-decoration: underline;
}

/* ============================================================
   RÉSULTATS
   ============================================================ */
.resultats {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.resultat {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.85rem;
    color: #33475c;
}

.resultat i {
    color: #1e88a8;
}

/* ============================================================
   HISTORIQUE
   ============================================================ */
.historique {
    display: flex;
    flex-direction: column;
    gap: 0.9rem;
}

.historique__item {
    border: 1px solid #e6ecf1;
    border-radius: 10px;
    padding: 0.85rem 1rem;
}

.historique__item--courant {
    border-color: #bcdcea;
    background: #f4fafc;
}

.historique__ligne {
    display: flex;
    align-items: center;
    gap: 0.6rem;
}

.historique__courant {
    font-size: 0.72rem;
    font-weight: 600;
    color: #14657f;
}

.historique__meta {
    font-size: 0.76rem;
    color: #8a9aac;
    margin-top: 0.35rem;
}

.historique__commentaire {
    margin: 0.45rem 0 0;
    font-size: 0.8rem;
    color: #5a6b7f;
}

/* ============================================================
   ÉTATS
   ============================================================ */
.detail-state {
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

.vide {
    font-size: 0.85rem;
    color: #8a9aac;
    margin: 0;
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 1199.98px) {
    .detail-activite {
        padding-right: 24px;
        padding-left: 24px;
    }
}

@media (max-width: 767.98px) {
    .detail-activite {
        padding: 24px 16px 20px;
    }

    .detail-header {
        flex-direction: column;
    }

    .detail-header__actions {
        justify-content: flex-start;
    }

    .tabs-nav {
        width: 100%;
    }

    .tab-btn {
        flex: 1;
        justify-content: center;
        padding: 0.5rem 0.7rem;
        font-size: 0.78rem;
    }

    .tab-btn__count {
        display: none;
    }

    .table-sous-activites thead th:nth-child(5),
    .table-sous-activites tbody td:nth-child(5) {
        display: none;
    }
}

@media (max-width: 575.98px) {
    .detail-activite {
        padding: 20px 12px 16px;
    }

    .info-grid {
        grid-template-columns: 1fr;
    }

    .info-item--large {
        grid-column: auto;
    }

    .validation__head {
        flex-direction: column;
        align-items: flex-start;
    }

    .table-sous-activites thead th:nth-child(3),
    .table-sous-activites tbody td:nth-child(3) {
        display: none;
    }
}

/* ============================================================
   AFFECTATIONS
   ============================================================ */
.affectations-toolbar {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    margin-bottom: 1rem;
    flex-wrap: wrap;
}

.affectations-roles {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.role-block {
    border: 1px solid #e6ecf1;
    border-radius: 10px;
    padding: 0.85rem 1rem;
}

.role-block__head {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 0.6rem;
}

.role-block__title {
    font-weight: 600;
    color: #1a2b3c;
}

.role-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
}

.role-item {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.45rem 0.6rem;
    background: #f8fafc;
    border-radius: 8px;
}

.role-item__name {
    font-weight: 600;
    color: #33475c;
    flex: 1;
}

.role-item__date {
    font-size: 0.75rem;
    color: #8a9aac;
}

.role-empty {
    font-size: 0.85rem;
    color: #8a9aac;
    font-style: italic;
    margin: 0;
}

.affectations-historique {
    margin-top: 1.5rem;
    padding-top: 1rem;
    border-top: 1px solid #e6ecf1;
}

.affectations-historique__title {
    font-size: 0.95rem;
    font-weight: 600;
    color: #1a2b3c;
    margin: 0 0 0.75rem;
    display: flex;
    align-items: center;
    gap: 0.4rem;
}

.timeline {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
}

.timeline__item {
    display: flex;
    align-items: flex-start;
    gap: 0.6rem;
}

.timeline__dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    margin-top: 0.35rem;
    flex: none;
}

.timeline__item--affectation .timeline__dot {
    background: #2a9d5a;
}

.timeline__item--desaffectation .timeline__dot {
    background: #d84e5b;
}

.timeline__content {
    flex: 1;
}

.timeline__head {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    flex-wrap: wrap;
}

.timeline__user {
    font-weight: 600;
    color: #33475c;
}

.timeline__date {
    font-size: 0.75rem;
    color: #8a9aac;
    margin-top: 0.15rem;
}
</style>
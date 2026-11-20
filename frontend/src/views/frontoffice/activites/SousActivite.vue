<script setup lang="ts">
import { useToastMessage } from '@/composables/useToast'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { BaseButton, BaseCard, BaseConfirm } from '@/components/base'
import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'
import { FichiersModal, LivrableModal } from '@/components/activite'
import ModalAffecterSousActivite from '@/components/ModalAffecterSousActivite.vue'
import * as activiteService from '@/services/activite'
import * as affectationService from '@/services/affectation'
import type {
    FichierDetail,
    LivrableDetail,
    SousActiviteDetail,
    UtilisateurResume,
} from '@/types/activite'
import type { AffectationActivite as AffectationSous } from '@/types/affectation'
import { formatDateSeule } from '@/utils/date'

// ------------------------------------------------------------------
// Constantes de presentation
// ------------------------------------------------------------------

const COULEURS_STATUT: Record<string, string> = {
    NON_COMMENCEE: 'secondary',
    EN_COURS: 'primary',
    TERMINEE: 'success',
    ANNULEE: 'danger',
    REPORTEE: 'warning',
    SUSPENDUE: 'purple',
    EN_RETARD: 'danger',
}

// ------------------------------------------------------------------
// Etat
// ------------------------------------------------------------------

const route = useRoute()
const router = useRouter()

const idActivite = computed(() => Number(route.params.id))
const idSousActivite = computed(() => Number(route.params.sousActiviteId))

const detail = ref<SousActiviteDetail | null>(null)
const chargement = ref(false)
const erreur = useToastMessage('error')

type Onglet = 'details' | 'livrables' | 'affectations' | 'historique'
const ongletActif = ref<Onglet>('details')

// --- Telechargement des fichiers ---

/** Id du fichier en cours de telechargement, pour desactiver les autres. */
const fichierEnTelechargement = ref<number | null>(null)

/** Affectations de la sous-activité (onglet Affectations). */
const affectationsSous = ref<AffectationSous[]>([])
const chargementAffectations = ref(false)
const modalAffectSousOpen = ref(false)
const confirmDesaffectOpen = ref(false)
const confirmDesaffectDemande = ref<ConfirmDemande | null>(null)
const affectationADesaffecter = ref<AffectationSous | null>(null)
const desaffectationEnCours = ref(false)

// ------------------------------------------------------------------
// Presentation
// ------------------------------------------------------------------

/** Libelle des codes de statut, les couleurs seules importent. */
function varianteStatut(code?: string | null): string {
    if (!code) return 'secondary'
    return COULEURS_STATUT[code.toUpperCase()] || 'secondary'
}

/** Identite complete : prenom nom, ou tiret si inconnue. */
function nomComplet(u?: UtilisateurResume | null): string {
    if (!u) return '—'
    return [u.prenom, u.nom].filter(Boolean).join(' ') || '—'
}

/** Horodatage lisible. formaterDate() afficherait "Jamais connecte" sur null. */
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

/** Periode "debut → fin", compacte quand l'une des bornes manque. */
function periode(debut?: string | null, fin?: string | null): string {
    const d = formatDateSeule(debut)
    const f = formatDateSeule(fin)

    if (d === '—' && f === '—') return '—'
    return `${d} → ${f}`
}

function formaterTaille(octets: number | null | undefined): string {
    if (octets == null) return '—'
    if (octets < 1024) return `${octets} o`
    if (octets < 1024 * 1024) return `${(octets / 1024).toFixed(1)} Ko`
    return `${(octets / (1024 * 1024)).toFixed(1)} Mo`
}

/** Pourcentage arrondi pour l'affichage, la valeur brute vient du backend. */
function pct(valeur?: number | null): number {
    return Math.round(valeur ?? 0)
}

/** Responsables actuellement affectes : dateDesaffectation nulle. */
function responsablesActifs(): UtilisateurResume[] {
    if (!detail.value) return []
    return detail.value.affectations
        .filter(a => a.dateDesaffectation == null)
        .map(a => a.utilisateur)
        .filter((u): u is UtilisateurResume => u != null)
}

// ------------------------------------------------------------------
// Depot d un livrable
// ------------------------------------------------------------------

const modalLivrable = ref(false)

/**
 * Les deux statuts sur lesquels le serveur accepte le depot.
 */
const STATUTS_DEPOSIT_OUVERTS = new Set(['BROUILLON', 'EN_COURS'])

const depotOuvert = computed(
    () => STATUTS_DEPOSIT_OUVERTS.has(detail.value?.statutActivite ?? '')
)

/** Motif du refus, porte par le bouton desactive. */
const motifDepotRefuse = computed(() => {
    const statut = detail.value?.statutActivite

    return statut
        ? `Un livrable ne peut être ajouté qu’à une activité en brouillon ou en cours (activité ${statut}).`
        : 'Un livrable ne peut être ajouté qu’à une activité en brouillon ou en cours.'
})

/**
 * Le livrable cree rejoint la liste affichee, sans recharger la page.
 */
function ajouterALivrables(livrable: LivrableDetail): void {
    if (!detail.value) return

    detail.value.livrables = [...detail.value.livrables, livrable]
}

/**
 * Le livrable dont on ajoute des fichiers, et l'ouverture du modal.
 */
const livrableChoisi = ref<LivrableDetail | null>(null)
const modalFichiers = ref(false)

function ouvrirFichiers(livrable: LivrableDetail): void {
    livrableChoisi.value = livrable
    modalFichiers.value = true
}

/**
 * Le livrable relu remplace l'ancien, sans rechargement.
 */
function fichiersAjoutes(livrable: LivrableDetail): void {
    if (!detail.value) return

    detail.value.livrables = detail.value.livrables.map((existant) =>
        existant.id === livrable.id ? livrable : existant
    )
}

// ------------------------------------------------------------------
// Telechargement des fichiers
// ------------------------------------------------------------------

async function telecharger(f: FichierDetail) {
    if (fichierEnTelechargement.value !== null) return
    fichierEnTelechargement.value = f.id

    let reponse

    try {
        reponse = await activiteService.telechargerFichier(
            idActivite.value, idSousActivite.value, f.id
        )
    } catch {
        erreur.value = 'Connexion au serveur impossible. Réessayez dans un instant.'
        fichierEnTelechargement.value = null
        return
    }

    fichierEnTelechargement.value = null

    if (!reponse.success) {
        erreur.value = reponse.error
        return
    }

    const url = URL.createObjectURL(reponse.data)
    const lien = document.createElement('a')
    lien.href = url
    lien.download = f.nomOriginal
    document.body.appendChild(lien)
    lien.click()
    lien.remove()
    URL.revokeObjectURL(url)
}

// ------------------------------------------------------------------
// Affectations — présentation
// ------------------------------------------------------------------

const affectationsActuelles = computed(() =>
    affectationsSous.value.filter(a => !a.dateDesaffectation)
)

function nomRole(a: AffectationSous): string {
    return a.role?.libelle ?? a.role?.code ?? '—'
}

function nomUtilisateur(a: AffectationSous): string {
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
    for (const a of affectationsSous.value) {
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

function demanderDesaffectation(a: AffectationSous) {
    affectationADesaffecter.value = a
    confirmDesaffectDemande.value = {
        titre: 'Désaffecter cette personne',
        message: `Retirer ${nomUtilisateur(a)} (${nomRole(a)}) de cette sous-activité ?`,
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
        const reponse = await affectationService.desaffecterSousActivite(
            idActivite.value,
            idSousActivite.value,
            a.id
        )
        if (reponse.success) {
            await chargerAffectations()
        }
    } finally {
        desaffectationEnCours.value = false
        confirmDesaffectOpen.value = false
        affectationADesaffecter.value = null
    }
}

function surAffectationSous() {
    modalAffectSousOpen.value = false
    chargerAffectations()
}

// ------------------------------------------------------------------
// Chargement
// ------------------------------------------------------------------

async function charger() {
    chargement.value = true
    erreur.value = null
    detail.value = null

    const id = idActivite.value
    const saId = idSousActivite.value

    if (!Number.isInteger(id) || id <= 0 || !Number.isInteger(saId) || saId <= 0) {
        erreur.value = "Identifiant d'activité ou de sous-activité invalide."
        chargement.value = false
        return
    }

    let reponse
    try {
        reponse = await activiteService.detailSousActivite(id, saId)
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
        const reponse = await affectationService.listerAffectationsSousActivite(idActivite.value, idSousActivite.value)
        if (reponse.success) {
            affectationsSous.value = reponse.data ?? []
        }
    } catch {
        affectationsSous.value = []
    }
    chargementAffectations.value = false
}

onMounted(() => {
    charger()
    chargerAffectations()
})
</script>

<template>
    <div class="detail-sous-activite">
        <!-- ============ EN-TÊTE ============ -->
        <header class="detail-header">
            <div class="detail-header__main">
                <button type="button" class="btn-retour"
                    @click="router.push({ name: 'activite-detail', params: { id: idActivite } })">
                    <i class="bi bi-arrow-left"></i>
                    Retour à l'activité
                </button>

                <div class="eyebrow">
                    <span class="live-dot"></span>
                    SUIVI DES ACTIVITÉS DTS-TSS
                </div>

                <h1 class="page-title">
                    Détail de la sous-activité
                    <span class="page-title__accent" aria-hidden="true">✳</span>
                </h1>

                <span class="title-divider" aria-hidden="true"></span>

                <p class="page-subtitle">
                    {{ detail
                        ? `${detail.code} · ${detail.designation}`
                        : chargement
                            ? 'Chargement…'
                            : 'Consultez les informations et le suivi de la sous-activité' }}
                </p>
            </div>

            <div class="detail-header__actions">
                <div v-if="detail" class="badges">
                    <span v-if="detail.avancementCourant" class="badge-soft badge-soft--primary">
                        {{ pct(detail.avancementCourant.valeurPourcentage) }}% d'avancement
                    </span>
                    <span v-else class="badge-soft badge-soft--secondary">Jamais évalué</span>
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
            <h3 class="empty-title">Impossible d'afficher la sous-activité</h3>
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
                <button type="button" :class="['tab-btn', { 'tab-btn--active': ongletActif === 'livrables' }]"
                    role="tab" :aria-selected="ongletActif === 'livrables'" @click="ongletActif = 'livrables'">
                    <i class="bi bi-file-earmark-text"></i>
                    Livrables
                    <span class="tab-btn__count">{{ detail.livrables.length }}</span>
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
                    <span class="tab-btn__count">{{ detail.historiqueAvancement.length }}</span>
                </button>
            </nav>

            <!-- ============ ONGLET DÉTAILS ============ -->
            <div v-show="ongletActif === 'details'" class="tab-content">
                <div class="row g-4">
                    <!-- Informations -->
                    <div class="col-lg-5">
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

                                <div class="info-item">
                                    <span class="info-label">Code</span>
                                    <span class="info-value code-valeur">{{ detail.code }}</span>
                                </div>

                                <div class="info-item">
                                    <span class="info-label">Période prévue</span>
                                    <span class="info-value">{{ periode(detail.dateDebutPrevue, detail.dateFinPrevue)
                                        }}</span>
                                </div>

                                <div class="info-item">
                                    <span class="info-label">Période réelle</span>
                                    <span class="info-value">{{ periode(detail.dateDebutReelle, detail.dateFinReelle)
                                        }}</span>
                                </div>

                                <div class="info-item">
                                    <span class="info-label">Responsables actifs</span>
                                    <span class="info-value">
                                        <template v-if="responsablesActifs().length">
                                            {{ responsablesActifs().map(nomComplet).join(' · ') }}
                                        </template>
                                        <template v-else>—</template>
                                    </span>
                                </div>
                            </div>
                        </BaseCard>
                    </div>

                    <!-- Avancement -->
                    <div class="col-lg-7">
                        <BaseCard class="h-100">
                            <template #title>
                                <div class="section-head">
                                    <i class="bi bi-graph-up"></i>
                                    <h3 class="card-title h6 mb-0">Avancement courant</h3>
                                </div>
                            </template>

                            <template v-if="detail.avancementCourant">
                                <div class="avancement">
                                    <div class="avancement__row">
                                        <span class="avancement__label">
                                            Dernier relevé du
                                            {{ formaterDateHeure(detail.avancementCourant.dateChangement) }}
                                            par {{ nomComplet(detail.avancementCourant.utilisateur) }}
                                        </span>
                                        <span class="avancement__valeur">
                                            {{ pct(detail.avancementCourant.valeurPourcentage) }}%
                                        </span>
                                    </div>

                                    <div class="avancement__bar" role="progressbar"
                                        :aria-valuenow="pct(detail.avancementCourant.valeurPourcentage)"
                                        aria-valuemin="0" aria-valuemax="100">
                                        <div class="avancement__fill"
                                            :style="{ width: pct(detail.avancementCourant.valeurPourcentage) + '%' }">
                                        </div>
                                    </div>

                                    <p v-if="detail.avancementCourant.commentaire" class="avancement__commentaire">
                                        {{ detail.avancementCourant.commentaire }}
                                    </p>
                                </div>
                            </template>

                            <template v-else>
                                <p class="vide">
                                    Aucun relevé d'avancement n'a été enregistré pour cette sous-activité.
                                </p>
                            </template>
                        </BaseCard>
                    </div>
                </div>
            </div>

            <!-- ============ ONGLET LIVRABLES ============ -->
            <div v-show="ongletActif === 'livrables'" class="tab-content">
                <BaseCard>
                    <template #title>
                        <div class="section-head">
                            <i class="bi bi-file-earmark-text"></i>
                            <h3 class="card-title h6 mb-0">
                                Livrables ({{ detail.livrables.length }})
                            </h3>
                        </div>
                    </template>

                    <template #actions>
                        <BaseButton size="sm" variant="primary" :disabled="!depotOuvert" :title="depotOuvert
                            ? 'Ajouter un livrable à cette sous-activité'
                            : motifDepotRefuse" @click="modalLivrable = true">
                            <i class="bi bi-plus-lg"></i>
                            Ajouter un livrable
                        </BaseButton>
                    </template>

                    <div v-if="detail.livrables.length" class="livrables">
                        <div v-for="liv in detail.livrables" :key="liv.id" class="livrable">
                            <div class="livrable__head">
                                <i class="bi bi-file-earmark-text"></i>
                                <span class="livrable__designation">{{ liv.designation }}</span>

                                <BaseButton size="sm" variant="secondary" :disabled="!depotOuvert" :title="depotOuvert
                                    ? 'Ajouter des fichiers à ce livrable'
                                    : motifDepotRefuse" :aria-label="`Ajouter des fichiers à ${liv.designation}`"
                                    @click="ouvrirFichiers(liv)">
                                    <i class="bi bi-paperclip"></i>
                                </BaseButton>
                            </div>

                            <p v-if="liv.description" class="livrable__description">{{ liv.description }}</p>

                            <div v-if="liv.fichiers.length" class="fichiers">
                                <span class="fichier__titre">Fichiers déposés</span>
                                <div v-for="f in liv.fichiers" :key="f.id" class="fichier">
                                    <i class="bi bi-paperclip"></i>
                                    <span class="fichier__nom">{{ f.nomOriginal }}</span>
                                    <span class="fichier__meta">
                                        v{{ f.version }} · {{ formaterTaille(f.taille) }} ·
                                        {{ formaterDateHeure(f.dateDepot) }} ·
                                        {{ nomComplet(f.utilisateur) }}
                                    </span>

                                    <span class="fichier__actions">
                                        <BaseButton size="sm" variant="secondary" title="Télécharger le fichier"
                                            :aria-label="`Télécharger ${f.nomOriginal}`"
                                            :loading="fichierEnTelechargement === f.id"
                                            :disabled="fichierEnTelechargement !== null && fichierEnTelechargement !== f.id"
                                            @click.stop="telecharger(f)">
                                            <i class="bi bi-download"></i>
                                        </BaseButton>
                                    </span>
                                </div>
                            </div>

                            <span v-else class="livrable__aucun">Aucun fichier déposé</span>
                        </div>
                    </div>

                    <p v-else class="vide">Aucun livrable pour cette sous-activité.</p>
                </BaseCard>
            </div>

            <!-- ============ ONGLET AFFECTATIONS ============ -->
            <div v-show="ongletActif === 'affectations'" class="tab-content">
                <BaseCard>
                    <template #title>
                        <div class="section-head">
                            <i class="bi bi-people"></i>
                            Affectations de la sous-activité
                        </div>
                    </template>
                    <div class="affectations-toolbar">
                        <BaseButton variant="primary" size="sm" @click="modalAffectSousOpen = true">
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
                <BaseCard>
                    <template #title>
                        <div class="section-head">
                            <i class="bi bi-clock-history"></i>
                            <h3 class="card-title h6 mb-0">
                                Historique des avancements ({{ detail.historiqueAvancement.length }})
                            </h3>
                        </div>
                    </template>

                    <div v-if="detail.historiqueAvancement.length" class="releves">
                        <div v-for="r in detail.historiqueAvancement" :key="r.id" class="releve"
                            :class="{ 'releve--courant': r.etatActuel }">
                            <div class="releve__ligne">
                                <span class="releve__valeur">{{ pct(r.valeurPourcentage) }}%</span>
                                <span v-if="r.statut" class="badge-soft"
                                    :class="`badge-soft--${varianteStatut(r.statut.code)}`">
                                    {{ r.statut.libelle }}
                                </span>
                                <span v-if="r.etatActuel" class="releve__courant">Courant</span>
                            </div>

                            <div class="releve__meta">
                                {{ formaterDateHeure(r.dateChangement) }} ·
                                {{ nomComplet(r.utilisateur) }}
                            </div>

                            <p v-if="r.commentaire" class="releve__commentaire">{{ r.commentaire }}</p>
                        </div>
                    </div>

                    <p v-else class="vide">Aucun relevé pour cette sous-activité.</p>
                </BaseCard>
            </div>

            <!-- ============ AJOUT D'UN LIVRABLE ============ -->
            <LivrableModal v-model="modalLivrable" :idActivite="idActivite" :idSousActivite="idSousActivite"
                :designationSousActivite="detail.designation" @ajoute="ajouterALivrables" />

            <!-- ============ AJOUT DE FICHIERS A UN LIVRABLE ============ -->
            <FichiersModal v-if="livrableChoisi" v-model="modalFichiers" :idActivite="idActivite"
                :idSousActivite="idSousActivite" :idLivrable="livrableChoisi.id"
                :designationLivrable="livrableChoisi.designation" :fichiersExistants="livrableChoisi.fichiers.length"
                @ajoute="fichiersAjoutes" />

            <!-- Modal d'affectation sous-activité -->
            <ModalAffecterSousActivite v-model="modalAffectSousOpen" :id-activite="idActivite"
                :id-sous-activite="idSousActivite" :code-sous-activite="detail.code"
                :designation-sous-activite="detail.designation" @affecte="surAffectationSous" />
        </template>

        <!-- Confirmation de désaffectation -->
        <BaseConfirm v-model="confirmDesaffectOpen" :demande="confirmDesaffectDemande" :loading="desaffectationEnCours"
            @confirme="confirmerDesaffectation" @annule="confirmDesaffectOpen = false" />
    </div>
</template>

<style scoped>
/* ============================================================
   CONTENEUR
   ============================================================ */
.detail-sous-activite {
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

.code-valeur {
    font-weight: 700;
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

.avancement__commentaire {
    margin: 0;
    padding: 0.55rem 0.7rem;
    background: #f6f8fa;
    border-radius: 7px;
    font-size: 0.8rem;
    color: #5a6b7f;
}

/* ============================================================
   RELEVÉS (Historique)
   ============================================================ */
.releves {
    display: flex;
    flex-direction: column;
    gap: 0.9rem;
}

.releve {
    border: 1px solid #e6ecf1;
    border-radius: 10px;
    padding: 0.85rem 1rem;
}

.releve--courant {
    border-color: #bcdcea;
    background: #f4fafc;
}

.releve__ligne {
    display: flex;
    align-items: center;
    gap: 0.6rem;
}

.releve__valeur {
    font-size: 1rem;
    font-weight: 700;
    color: #1e88a8;
    min-width: 2.6rem;
}

.releve__courant {
    font-size: 0.72rem;
    font-weight: 600;
    color: #14657f;
}

.releve__meta {
    font-size: 0.76rem;
    color: #8a9aac;
    margin-top: 0.35rem;
}

.releve__commentaire {
    margin: 0.45rem 0 0;
    font-size: 0.8rem;
    color: #5a6b7f;
}

/* ============================================================
   LIVRABLES
   ============================================================ */
.livrables {
    display: flex;
    flex-direction: column;
    gap: 0.9rem;
}

.livrable {
    border: 1px solid #e6ecf1;
    border-left: 3px solid #1e88a8;
    border-radius: 10px;
    padding: 0.9rem 1rem;
}

.livrable__head {
    display: flex;
    align-items: center;
    gap: 0.45rem;
}

.livrable__head i {
    color: #1e88a8;
}

.livrable__designation {
    font-size: 0.88rem;
    font-weight: 600;
    color: #33475c;
    flex: 1;
}

.livrable__description {
    font-size: 0.8rem;
    color: #8a9aac;
    margin: 0.25rem 0 0 1.35rem;
}

.fichiers {
    margin: 0.6rem 0 0 1.35rem;
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
}

.fichier__titre {
    font-size: 0.68rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: #8a9aac;
    margin-bottom: 0.15rem;
}

.fichier {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.35rem 0.5rem;
    font-size: 0.76rem;
}

.fichier i {
    color: #b6c2ce;
}

.fichier__nom {
    font-weight: 600;
    color: #33475c;
}

.fichier__meta {
    color: #8a9aac;
}

.fichier__actions {
    display: flex;
    gap: 0.35rem;
    margin-left: auto;
}

.fichier__actions .base-btn {
    padding: 0.28rem 0.55rem;
}

.livrable__aucun {
    display: block;
    margin: 0.35rem 0 0 1.35rem;
    font-size: 0.76rem;
    color: #b6c2ce;
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

/* ============================================================
   LOADING / SPINNER
   ============================================================ */
.loading {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    padding: 1.5rem;
    justify-content: center;
    color: #6b7280;
    font-size: 0.86rem;
}

.spinner {
    width: 16px;
    height: 16px;
    border: 2px solid #e6ebf3;
    border-top-color: #4a6cf7;
    border-radius: 50%;
    animation: spin 0.7s linear infinite;
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
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
    .detail-sous-activite {
        padding-right: 24px;
        padding-left: 24px;
    }
}

@media (max-width: 767.98px) {
    .detail-sous-activite {
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
}

@media (max-width: 575.98px) {
    .detail-sous-activite {
        padding: 20px 12px 16px;
    }

    .info-grid {
        grid-template-columns: 1fr;
    }

    .info-item--large {
        grid-column: auto;
    }
}
</style>
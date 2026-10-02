<script setup lang="ts">
/**
 * Detail d'une activite DTS-TSS.
 *
 * Repartition des responsabilites :
 * - le backend assemble TOUT (sous-activites, avancements, livrables,
 *   fichiers, validations, indicateurs, historique) en une seule reponse ;
 * - cette page ne recalcule rien : ni avancement global, ni statut courant,
 *   ni "en retard". Elle affiche ce que le backend a deja determine.
 *
 * Une collection vide signifie "rien a afficher" : le backend ne renvoie
 * jamais null pour une liste.
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { BaseButton, BaseCard } from '@/components/base'
import * as activiteService from '@/services/activite'
import type {
    AffectationDetail,
    ActiviteDetail,
    DecisionValidation,
    SousActiviteDetail,
    UtilisateurResume,
} from '@/types/activite'
import { BROUILLON } from '@/types/activite'
import { formatDateSeule } from '@/utils/date'

// ------------------------------------------------------------------
// Constantes de presentation
// ------------------------------------------------------------------

/** Les libelles des decisions ne viennent pas du backend : ils sont ici. */
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

// ------------------------------------------------------------------
// Etat
// ------------------------------------------------------------------

const route = useRoute()
const router = useRouter()

const idActivite = computed(() => Number(route.params.id))

const detail = ref<ActiviteDetail | null>(null)
const chargement = ref(false)
const erreur = ref<string | null>(null)

// ------------------------------------------------------------------
// Presentation
// ------------------------------------------------------------------

const avancementAffiche = computed(() =>
    Math.round(detail.value?.avancement ?? 0)
)

/** Libelle des codes de statut / priorite, les couleurs seules importent. */
function varianteStatut(code?: string | null): string {
    if (!code) return 'secondary'
    return COULEURS_STATUT[code.toUpperCase()] || 'secondary'
}

function variantePriorite(code?: string | null): string {
    if (!code) return 'secondary'
    return COULEURS_PRIORITE[code.toUpperCase()] || 'secondary'
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

/** Pourcentage arrondi pour l'affichage, la valeur brute vient du backend. */
function pct(valeur?: number | null): number {
    return Math.round(valeur ?? 0)
}

/** Responsables actuellement affectes : dateDesaffectation nulle. */
function responsablesActifs(sa: SousActiviteDetail): AffectationDetail[] {
    return sa.affectations.filter(a => a.dateDesaffectation == null)
}

function voirSousActivite(idSousActivite: number) {
    router.push({
        name: 'sous-activite-detail',
        params: { id: idActivite.value, sousActiviteId: idSousActivite },
    })
}

/**
 * Le formulaire de modification n'a de sens que sur un brouillon.
 *
 * Le statut vient du detail, donc de la meme source que le reste de la page,
 * et non d'une requete supplementaire. L'unicite du statut garantit qu'il
 * ne peut pas y avoir de lien ajoute et retire entre deux chargements --
 * c'est ce que produit la derniere entree d'historique.
 */
const peutModifier = computed(() => detail.value?.statut?.code === BROUILLON)

function ouvrirFormulaire(): void {
    router.push({
        name: 'activite-modifier',
        params: { id: idActivite.value },
    })
}

// ------------------------------------------------------------------
// Chargement
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

    // get() convertit une reponse HTTP en echec metier, mais laisse remonter
    // une panne reseau : sans ce rattrapage, le rejet sortirait de charger().
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

onMounted(charger)
</script>

<template>
    <div class="detail-activite">
        <!-- ============ EN-TETE ============ -->
        <div class="page-header d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
                <button type="button" class="btn-retour" @click="router.push({ name: 'activites' })">
                    <i class="bi bi-arrow-left"></i>
                    Retour à la liste
                </button>

                <h1 class="page-title">Détail de l'activité</h1>

                <p class="page-subtitle">
                    {{ detail ? `${detail.code} · ${detail.reference}` : chargement ? 'Chargement…' : '' }}
                </p>
            </div>

            <div v-if="detail" class="d-flex flex-wrap align-items-center gap-3">
                <div class="badges">
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

                <!--
                    Reserve au brouillon : une activite soumise n'a plus de
                    redaction a faire, son contenu appartient au suivi. Ce
                    n'est qu'un affichage -- un bouton absent ne protege
                    rien, et le refus est aussi verifie a l'enregistrement.
                -->
                <BaseButton v-if="peutModifier" variant="secondary" @click="ouvrirFormulaire">
                    <i class="bi bi-pencil"></i>
                    Modifier
                </BaseButton>
            </div>
        </div>

        <!-- ============ ERREUR ============ -->
        <div v-if="erreur" class="alert alert-danger d-flex align-items-center gap-2" role="alert">
            <i class="bi bi-exclamation-triangle"></i>
            <span>{{ erreur }}</span>
            <button type="button" class="btn-close ms-auto" @click="erreur = null"></button>
        </div>

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
            <div class="row g-4 mb-4">
                <!-- Informations generales -->
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
                                <span class="info-value">{{ periode(detail.dateDebutPrevue, detail.dateFinPrevue) }}</span>
                            </div>

                            <div class="info-item">
                                <span class="info-label">Période réelle</span>
                                <span class="info-value">{{ periode(detail.dateDebutReelle, detail.dateFinReelle) }}</span>
                            </div>

                            <div class="info-item">
                                <span class="info-label">Date de création</span>
                                <span class="info-value">{{ formaterDateHeure(detail.dateCreation) }}</span>
                            </div>
                        </div>
                    </BaseCard>
                </div>

                <!-- Avancement + Validations -->
                <div class="col-lg-7 d-flex flex-column gap-4">
                    <BaseCard>
                        <template #title>
                            <div class="section-head">
                                <i class="bi bi-graph-up"></i>
                                <h3 class="card-title h6 mb-0">Avancement global</h3>
                            </div>
                        </template>

                        <div class="avancement">
                            <div class="avancement__row">
                                <span class="avancement__label">Moyenne des derniers avancements des sous-activités</span>
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

                    <BaseCard>
                        <template #title>
                            <div class="section-head">
                                <i class="bi bi-shield-check"></i>
                                <h3 class="card-title h6 mb-0">Circuit de validation</h3>
                            </div>
                        </template>

                        <div v-if="detail.validations.length" class="validations">
                            <div v-for="v in detail.validations" :key="v.id" class="validation">
                                <div class="validation__head">
                                    <span class="validation__niveau">Étape {{ v.etape.niveau }}</span>
                                    <span class="badge-soft" :class="`badge-soft--${COULEURS_DECISION[v.decision]}`">
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
            </div>

            <!-- ============ SOUS-ACTIVITES ============ -->
                <BaseCard class="mb-4">
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
                                        <span class="cell-periode">{{ periode(sa.dateDebutPrevue, sa.dateFinPrevue) }}</span>
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
                                        <span v-else class="text-muted">—</span>
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
                                                    :style="{ width: pct(sa.avancementCourant.valeurPourcentage) + '%' }"></div>
                                            </div>
                                        </template>
                                        <span v-else class="text-muted">Jamais évalué</span>
                                    </td>

                                    <td class="col-actions">
                                        <button type="button" class="btn-icone" title="Voir le détail de la sous-activité"
                                            @click="voirSousActivite(sa.id)">
                                            <i class="bi bi-eye"></i>
                                        </button>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>

                    <p v-else class="vide">Aucune sous-activité pour cette activité.</p>
                </BaseCard>

            <!-- ============ INDICATEURS ============ -->
            <BaseCard class="mb-4">
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
                        </div>

                        <div v-if="ind.valeurs.length" class="indicateur__mesures">
                            <span class="indicateur__titre">Mesures</span>
                            <div v-for="v in ind.valeurs" :key="v.id" class="mesure">
                                <span class="mesure__valeur">{{ v.valeur }}</span>
                                <span class="mesure__periode">{{ periode(v.periodeDebut, v.periodeFin) }}</span>
                                <span class="mesure__saisie">{{ formaterDateHeure(v.dateSaisie) }}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <p v-else class="vide">Aucun indicateur rattaché à cette activité.</p>
            </BaseCard>

            <!-- ============ RESULTATS + HISTORIQUE ============ -->
            <div class="row g-4">
                <div class="col-lg-5">
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
        </template>
    </div>
</template>

<style scoped>
.detail-activite {
    padding: 1.5rem 0 2rem;
}

/* --- En-tete --- */
.btn-retour {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    border: none;
    background: transparent;
    color: #14657f;
    font-size: 0.82rem;
    font-weight: 600;
    padding: 0;
    cursor: pointer;
    margin-bottom: 0.75rem;
}

.btn-retour:hover {
    color: #1e88a8;
}

.badges {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    align-items: center;
}

/* --- Titre des cartes --- */
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

/* --- Grille d'informations --- */
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

/* --- Avancement --- */
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

/* --- Badges --- */
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

/* --- Validations --- */
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

/* --- Sous-activites : tableau --- */
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
    width: 26%;
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
    width: 10%;
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

.indicateur__titre {
    display: block;
    font-size: 0.68rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: #8a9aac;
    margin-bottom: 0.4rem;
}

/* --- Indicateurs --- */
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

.indicateur__mesures {
    margin-top: 0.7rem;
}

.mesure {
    display: flex;
    align-items: baseline;
    gap: 0.75rem;
    padding: 0.3rem 0;
    border-top: 1px solid #f2f5f7;
    font-size: 0.78rem;
}

.mesure__valeur {
    font-weight: 600;
    color: #33475c;
    min-width: 4.5rem;
}

.mesure__periode {
    color: #5a6b7f;
}

.mesure__saisie {
    color: #8a9aac;
    margin-left: auto;
}

/* --- Resultats --- */
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

/* --- Historique --- */
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

/* --- Etats --- */
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

/* --- Responsive --- */
@media (max-width: 575.98px) {
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
}
</style>
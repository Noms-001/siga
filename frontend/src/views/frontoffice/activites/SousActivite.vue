<script setup lang="ts">
/**
 * Detail d'une sous-activite.
 *
 * Le backend expose ce detail sur un endpoint dedie :
 * GET /api/activites/{id}/sous-activites/{saId}. La sous-activite doit
 * appartenir a l'activite demandee, elle-meme dans le perimetre de
 * l'appelant (404 si non rattachee, 403 si activite hors perimetre).
 *
 * Comme pour le detail d'activite, le front n'assemble rien : l'avancement
 * courant et son etat sont deja determines par le backend.
 */
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { BaseButton, BaseCard, BaseModal } from '@/components/base'
import * as activiteService from '@/services/activite'
import type { FichierDetail, SousActiviteDetail, UtilisateurResume } from '@/types/activite'
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
const erreur = ref<string | null>(null)

// --- Apercu et telechargement de fichiers ---

/** Id du fichier en cours de telechargement, pour desactiver les autres. */
const fichierEnTelechargement = ref<number | null>(null)

const apercuOuvert = ref(false)
const apercu = ref<{ nom: string; typeMime: string; url: string } | null>(null)

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
// Apercu et telechargement de fichiers
// ------------------------------------------------------------------

/** Les images et les PDF sont affichables dans le navigateur. */
function apercevable(f: FichierDetail): boolean {
    return f.typeMime.startsWith('image/') || f.typeMime === 'application/pdf'
}

async function ouvrirApercu(f: FichierDetail) {
    let reponse

    try {
        reponse = await activiteService.telechargerFichier(
            idActivite.value, idSousActivite.value, f.id
        )
    } catch {
        erreur.value = 'Connexion au serveur impossible. Réessayez dans un instant.'
        return
    }

    if (!reponse.success) {
        erreur.value = reponse.error
        return
    }

    if (apercu.value) {
        URL.revokeObjectURL(apercu.value.url)
    }

    apercu.value = {
        nom: f.nomOriginal,
        typeMime: reponse.data.type,
        url: URL.createObjectURL(reponse.data),
    }
    apercuOuvert.value = true
}

function fermerApercu() {
    apercuOuvert.value = false

    if (apercu.value) {
        URL.revokeObjectURL(apercu.value.url)
        apercu.value = null
    }
}

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

onUnmounted(fermerApercu)

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

onMounted(charger)
</script>

<template>
    <div class="detail-sous-activite">
        <!-- ============ EN-TETE ============ -->
        <div class="page-header d-flex flex-wrap justify-content-between align-items-start gap-3">
            <div>
                <button type="button" class="btn-retour"
                    @click="router.push({ name: 'activite-detail', params: { id: idActivite } })">
                    <i class="bi bi-arrow-left"></i>
                    Retour à l'activité
                </button>

                <h1 class="page-title">Détail de la sous-activité</h1>

                <p class="page-subtitle">
                    {{ detail ? `${detail.code} · ${detail.designation}` : chargement ? 'Chargement…' : '' }}
                </p>
            </div>

            <div v-if="detail" class="badges">
                <span v-if="detail.avancementCourant" class="badge-soft badge-soft--primary">
                    {{ pct(detail.avancementCourant.valeurPourcentage) }}% d'avancement
                </span>
                <span v-else class="badge-soft badge-soft--secondary">Jamais évalué</span>
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
            <h3 class="empty-title">Impossible d'afficher la sous-activité</h3>
            <p class="empty-text">{{ erreur }}</p>
            <BaseButton variant="primary" @click="charger">
                <i class="bi bi-arrow-repeat"></i>
                Réessayer
            </BaseButton>
        </div>

        <!-- ============ CONTENU ============ -->
        <template v-else-if="detail">
            <div class="row g-4 mb-4">
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
                                <span class="info-value">{{ periode(detail.dateDebutPrevue, detail.dateFinPrevue) }}</span>
                            </div>

                            <div class="info-item">
                                <span class="info-label">Période réelle</span>
                                <span class="info-value">{{ periode(detail.dateDebutReelle, detail.dateFinReelle) }}</span>
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
                <div class="col-lg-7 d-flex flex-column gap-4">
                    <BaseCard>
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
                                        :style="{ width: pct(detail.avancementCourant.valeurPourcentage) + '%' }"></div>
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
            </div>

            <!-- ============ RESPONSABLES ============ -->
            <BaseCard class="mb-4">
                <template #title>
                    <div class="section-head">
                        <i class="bi bi-people"></i>
                        <h3 class="card-title h6 mb-0">
                            Responsables ({{ detail.affectations.length }})
                        </h3>
                    </div>
                </template>

                <div v-if="detail.affectations.length" class="affectations">
                    <div v-for="a in detail.affectations" :key="a.id" class="affectation"
                        :class="{ 'affectation--passee': a.dateDesaffectation != null }">
                        <div class="affectation__ligne">
                            <span class="affectation__nom">
                                <i class="bi bi-person"></i>
                                {{ nomComplet(a.utilisateur) }}
                            </span>

                            <span v-if="a.role" class="affectation__role">
                                <i class="bi bi-award"></i>
                                {{ a.role.libelle }}
                            </span>

                            <span v-if="a.dateDesaffectation == null" class="badge-soft badge-soft--success">
                                Affecté
                            </span>
                            <span v-else class="badge-soft badge-soft--secondary">Désaffecté</span>
                        </div>

                        <div class="affectation__meta">
                            Affecté le {{ formaterDateHeure(a.dateAffectation) }}
                            <template v-if="a.dateDesaffectation">
                                · Désaffecté le {{ formaterDateHeure(a.dateDesaffectation) }}
                            </template>
                        </div>
                    </div>
                </div>

                <p v-else class="vide">Aucune affectation pour cette sous-activité.</p>
            </BaseCard>

            <!-- ============ LIVRABLES ============ -->
            <BaseCard>
                <template #title>
                    <div class="section-head">
                        <i class="bi bi-file-earmark-text"></i>
                        <h3 class="card-title h6 mb-0">
                            Livrables ({{ detail.livrables.length }})
                        </h3>
                    </div>
                </template>

                <div v-if="detail.livrables.length" class="livrables">
                    <div v-for="liv in detail.livrables" :key="liv.id" class="livrable">
                        <div class="livrable__head">
                            <i class="bi bi-file-earmark-text"></i>
                            <span class="livrable__designation">{{ liv.designation }}</span>
                        </div>

                        <p v-if="liv.description" class="livrable__description">{{ liv.description }}</p>

                        <div v-if="liv.fichiers.length" class="fichiers">
                            <span class="fichier__titre">Fichiers déposés</span>
                            <div v-for="f in liv.fichiers" :key="f.id" class="fichier"
                                    :class="{ 'fichier--apercevable': apercevable(f) }"
                                    :title="apercevable(f) ? 'Cliquer pour afficher l\'aperçu' : ''"
                                    role="button" tabindex="0"
                                    @click="apercevable(f) && ouvrirApercu(f)"
                                    @keydown.enter="apercevable(f) && ouvrirApercu(f)">
                                <i class="bi bi-paperclip"></i>
                                <span class="fichier__nom">{{ f.nomOriginal }}</span>
                                <span class="fichier__meta">
                                    v{{ f.version }} · {{ formaterTaille(f.taille) }} ·
                                    {{ formaterDateHeure(f.dateDepot) }} ·
                                    {{ nomComplet(f.utilisateur) }}
                                </span>

                                <span class="fichier__actions">
                                    <BaseButton
                                        size="sm"
                                        variant="secondary"
                                        title="Télécharger le fichier"
                                        :loading="fichierEnTelechargement === f.id"
                                        :disabled="fichierEnTelechargement !== null && fichierEnTelechargement !== f.id"
                                        @click.stop="telecharger(f)"
                                    >
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
        </template>

        <!-- ============ APERÇU FICHIER ============ -->
        <BaseModal
            v-model="apercuOuvert"
            size="lg"
            :title="apercu ? apercu.nom : 'Aperçu du fichier'"
            @close="fermerApercu"
        >
            <template v-if="apercu">
                <div v-if="apercu.typeMime.startsWith('image/')" class="apercu">
                    <img :src="apercu.url" :alt="apercu.nom" class="apercu__image" />
                </div>

                <template v-else-if="apercu.typeMime === 'application/pdf'">
                    <iframe class="apercu__pdf" :src="apercu.url" :title="apercu.nom"></iframe>
                </template>

                <p v-else class="vide">
                    Aucun aperçu n'est disponible pour ce type de fichier.
                </p>
            </template>

            <template v-else>
                <p class="vide">Chargement du fichier…</p>
            </template>
        </BaseModal>
    </div>
</template>

<style scoped>
.detail-sous-activite {
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

.code-valeur {
    font-weight: 700;
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

.avancement__commentaire {
    margin: 0;
    padding: 0.55rem 0.7rem;
    background: #f6f8fa;
    border-radius: 7px;
    font-size: 0.8rem;
    color: #5a6b7f;
}

/* --- Releves --- */
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

/* --- Affectations --- */
.affectations {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.affectation {
    border: 1px solid #e6ecf1;
    border-radius: 10px;
    padding: 0.85rem 1rem;
}

.affectation--passee {
    opacity: 0.7;
    background: #fafbfc;
}

.affectation__ligne {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.5rem 1rem;
}

.affectation__nom {
    font-size: 0.9rem;
    font-weight: 600;
    color: #1a2b3c;
}

.affectation__nom i {
    color: #1e88a8;
}

.affectation__role {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
    font-size: 0.78rem;
    color: #5a6b7f;
}

.affectation__role i {
    color: #b6c2ce;
}

.affectation__meta {
    font-size: 0.76rem;
    color: #8a9aac;
    margin-top: 0.35rem;
}

/* --- Livrables --- */
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

.fichier--apercevable {
    cursor: pointer;
    border-radius: 7px;
    padding: 0.25rem 0.4rem;
    margin: -0.25rem 0 0 -0.4rem;
    transition: background var(--transition-fast);
}

.fichier--apercevable:hover {
    background: #f2f7fa;
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

/* --- Apercu --- */
.apercu {
    display: flex;
    justify-content: center;
}

.apercu__image {
    max-width: 100%;
    max-height: 72vh;
    border-radius: 8px;
    object-fit: contain;
}

.apercu__pdf {
    width: 100%;
    height: 72vh;
    border: none;
    border-radius: 8px;
}

.livrable__aucun {
    display: block;
    margin: 0.35rem 0 0 1.35rem;
    font-size: 0.76rem;
    color: #b6c2ce;
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
}
</style>
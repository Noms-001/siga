<script setup lang="ts">
import { useToastMessage } from '@/composables/useToast'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BaseButton, BaseInput } from '@/components/base'
import {
    mesDemandesActivites, demandesActivitesATraiter,
    annulerDemandeActivite,
    accepterDemandeActivite, refuserDemandeActivite,
    mesDemandesPlans, demandesPlansATraiter,
    annulerDemandePlan, accepterDemandePlan, refuserDemandePlan,
} from '@/services/demandeReprise'
import { STATUT_DEMANDE } from '@/types/demandeReprise'
import type {
    DemandeRepriseActivite,
    DemandeReprisePlan,
} from '@/types/demandeReprise'

const router = useRouter()

type VuePrincipale = 'mes-demandes' | 'a-traiter'
type SousVue = 'activites' | 'plans'

const vue = ref<VuePrincipale>('mes-demandes')
const sousVue = ref<SousVue>('activites')

const loading = ref(false)
const errorMessage = useToastMessage('error')
const successMessage = useToastMessage('success')

const demandesActivites = ref<DemandeRepriseActivite[]>([])
const demandesPlans = ref<DemandeReprisePlan[]>([])

/* Modal décision */
const modalDecision = ref(false)
const decisionType = ref<'accepter' | 'refuser'>('accepter')
const decisionCible = ref<
    | { kind: 'activite'; demande: DemandeRepriseActivite }
    | { kind: 'plan'; demande: DemandeReprisePlan }
    | null
>(null)
const decisionCommentaire = ref('')
const decisionLoading = ref(false)

const dateDuJour = computed(() =>
    new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    }).format(new Date()).toUpperCase()
)

/* -------------------- chargement -------------------- */

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    successMessage.value = ''

    try {
        if (vue.value === 'mes-demandes') {
            if (sousVue.value === 'activites') {
                const res = await mesDemandesActivites()
                if (res.success) demandesActivites.value = res.data
                else errorMessage.value = res.error
            } else {
                const res = await mesDemandesPlans()
                if (res.success) demandesPlans.value = res.data
                else errorMessage.value = res.error
            }
        } else {
            if (sousVue.value === 'activites') {
                const res = await demandesActivitesATraiter()
                if (res.success) demandesActivites.value = res.data
                else errorMessage.value = res.error
            } else {
                const res = await demandesPlansATraiter()
                if (res.success) demandesPlans.value = res.data
                else errorMessage.value = res.error
            }
        }
    } finally {
        loading.value = false
    }
}

function changerVue(v: VuePrincipale): void {
    vue.value = v
    charger()
}

function changerSousVue(v: SousVue): void {
    sousVue.value = v
    charger()
}

/* -------------------- actions -------------------- */

async function annuler(id: number): Promise<void> {
    if (!confirm('Annuler cette demande ?')) return

    const res = sousVue.value === 'activites'
        ? await annulerDemandeActivite(id)
        : await annulerDemandePlan(id)

    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    successMessage.value = 'Demande annulée.'
    await charger()
    setTimeout(() => (successMessage.value = ''), 3000)
}

function ouvrirDecision(
    type: 'accepter' | 'refuser',
    kind: 'activite' | 'plan',
    demande: DemandeRepriseActivite | DemandeReprisePlan
): void {
    decisionType.value = type
    decisionCommentaire.value = ''
    decisionCible.value = kind === 'activite'
        ? { kind: 'activite', demande: demande as DemandeRepriseActivite }
        : { kind: 'plan', demande: demande as DemandeReprisePlan }
    modalDecision.value = true
}

async function validerDecision(): Promise<void> {
    if (!decisionCible.value) return

    decisionLoading.value = true
    const payload = { commentaire: decisionCommentaire.value.trim() || null }

    const res = decisionCible.value.kind === 'activite'
        ? (decisionType.value === 'accepter'
            ? await accepterDemandeActivite(decisionCible.value.demande.id, payload)
            : await refuserDemandeActivite(decisionCible.value.demande.id, payload))
        : (decisionType.value === 'accepter'
            ? await accepterDemandePlan(decisionCible.value.demande.id, payload)
            : await refuserDemandePlan(decisionCible.value.demande.id, payload))

    decisionLoading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        modalDecision.value = false
        return
    }

    modalDecision.value = false
    successMessage.value = decisionType.value === 'accepter'
        ? 'Demande acceptée.'
        : 'Demande refusée.'
    await charger()
    setTimeout(() => (successMessage.value = ''), 3000)
}

function voirCible(kind: 'activite' | 'plan', id: number): void {
    if (kind === 'activite') {
        router.push({ name: 'activite-detail', params: { id } })
    } else {
        router.push({ name: 'plan-action-detail', params: { id } })
    }
}

/* -------------------- helpers -------------------- */

function formaterDateHeure(iso: string | null): string {
    if (!iso) return '—'
    return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
}

function libelleStatut(statut: string): string {
    return STATUT_DEMANDE[statut as keyof typeof STATUT_DEMANDE]?.libelle ?? statut
}

function classeStatut(statut: string): string {
    return STATUT_DEMANDE[statut as keyof typeof STATUT_DEMANDE]?.classe ?? ''
}

const nombreActivites = computed(() => demandesActivites.value.length)
const nombrePlans = computed(() => demandesPlans.value.length)

function fermerModal(): void {
    modalDecision.value = false
}

onMounted(charger)
</script>

<template>
    <div class="dr-page">
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>Demandes de reprise</h1>
                <span class="title-divider"></span>
                <p class="subheading">
                    Demandes de reprise d'activités et de plans d'action non commencés
                </p>
            </div>
            <div class="welcome-row__right">
                <BaseButton variant="secondary" icon="bi bi-arrow-clockwise" @click="charger">
                    Actualiser
                </BaseButton>
            </div>
        </header>

        <!-- Message succès -->
        <!-- Erreur -->
        <!-- Onglets principaux -->
        <div class="main-tabs">
            <button :class="['main-tab', { 'main-tab--active': vue === 'mes-demandes' }]"
                @click="changerVue('mes-demandes')">
                <i class="bi bi-send"></i>
                Mes demandes
            </button>
            <button :class="['main-tab', { 'main-tab--active': vue === 'a-traiter' }]" @click="changerVue('a-traiter')">
                <i class="bi bi-inbox"></i>
                À traiter
            </button>
        </div>

        <!-- Sous-onglets -->
        <div class="sub-tabs">
            <button :class="['sub-tab', { 'sub-tab--active': sousVue === 'activites' }]"
                @click="changerSousVue('activites')">
                Activités
                <span v-if="sousVue === 'activites' && nombreActivites > 0" class="sub-tab__count">
                    {{ nombreActivites }}
                </span>
            </button>
            <button :class="['sub-tab', { 'sub-tab--active': sousVue === 'plans' }]" @click="changerSousVue('plans')">
                Plans d'action
                <span v-if="sousVue === 'plans' && nombrePlans > 0" class="sub-tab__count">
                    {{ nombrePlans }}
                </span>
            </button>
        </div>

        <!-- Chargement -->
        <div v-if="loading" class="loading-state">
            <div v-for="i in 3" :key="i" class="skeleton-card"></div>
        </div>

        <!-- Vide -->
        <section
            v-else-if="(sousVue === 'activites' && nombreActivites === 0) || (sousVue === 'plans' && nombrePlans === 0)"
            class="empty-state">
            <i class="bi bi-inbox"></i>
            <h2>
                {{ vue === 'mes-demandes' ? 'Aucune demande envoyée' : 'Aucune demande à traiter' }}
            </h2>
            <p v-if="vue === 'mes-demandes'">
                Vous n'avez encore demandé la reprise d'aucune {{ sousVue === 'activites' ? 'activité' : 'pland\'action' }}.
            </p>
            <p v-else>
                Aucune demande en attente de votre décision pour le moment.
            </p>
        </section>

        <!-- Liste activités -->
        <section v-else-if="sousVue === 'activites'" class="demandes-list">
            <article v-for="d in demandesActivites" :key="d.id" class="demande-card">
                <header class="demande-card__head">
                    <div class="demande-card__left">
                        <span :class="['badge-statut', classeStatut(d.statut)]">
                            {{ libelleStatut(d.statut) }}
                        </span>
                        <code class="demande-card__code">{{ d.activite.code }}</code>
                    </div>
                    <span class="demande-card__date">
                        {{ formaterDateHeure(d.dateDemande) }}
                    </span>
                </header>

                <div class="demande-card__body">
                    <h3 class="demande-card__title" @click="voirCible('activite', d.activite.id!)">
                        {{ d.activite.libelle }}
                    </h3>

                    <div class="demande-card__info">
                        <div class="info-pair">
                            <span class="info-pair__label">Demandeur</span>
                            <span class="info-pair__value">{{ d.demandeur.libelle }}</span>
                        </div>
                        <div class="info-pair">
                            <span class="info-pair__label">Responsable actuel</span>
                            <span class="info-pair__value">{{ d.responsableActuel.libelle }}</span>
                        </div>
                    </div>

                    <blockquote v-if="d.motif" class="demande-card__motif">
                        <i class="bi bi-chat-quote"></i>
                        {{ d.motif }}
                    </blockquote>

                    <div v-if="d.statut !== 'EN_ATTENTE' && d.decideur" class="demande-card__decision">
                        <span>Décision de {{ d.decideur.libelle }} le {{ formaterDateHeure(d.dateDecision) }}</span>
                        <p v-if="d.commentaireDecision">{{ d.commentaireDecision }}</p>
                    </div>
                </div>

                <footer class="demande-card__actions">
                    <template v-if="d.statut === 'EN_ATTENTE' && vue === 'mes-demandes'">
                        <BaseButton variant="secondary" size="sm" icon="bi bi-x-lg" @click="annuler(d.id)">
                            Annuler
                        </BaseButton>
                    </template>
                    <template v-else-if="d.statut === 'EN_ATTENTE' && vue === 'a-traiter'">
                        <BaseButton variant="secondary" size="sm" icon="bi bi-x-circle"
                            @click="ouvrirDecision('refuser', 'activite', d)">
                            Refuser
                        </BaseButton>
                        <BaseButton variant="primary" size="sm" icon="bi bi-check-lg"
                            @click="ouvrirDecision('accepter', 'activite', d)">
                            Accepter
                        </BaseButton>
                    </template>
                </footer>
            </article>
        </section>

        <!-- Liste plans -->
        <section v-else class="demandes-list">
            <article v-for="d in demandesPlans" :key="d.id" class="demande-card">
                <header class="demande-card__head">
                    <div class="demande-card__left">
                        <span :class="['badge-statut', classeStatut(d.statut)]">
                            {{ libelleStatut(d.statut) }}
                        </span>
                        <code class="demande-card__code">{{ d.planAction.code }}</code>
                    </div>
                    <span class="demande-card__date">
                        {{ formaterDateHeure(d.dateDemande) }}
                    </span>
                </header>

                <div class="demande-card__body">
                    <h3 class="demande-card__title" @click="voirCible('plan', d.planAction.id!)">
                        {{ d.planAction.libelle }}
                    </h3>

                    <div class="demande-card__info">
                        <div class="info-pair">
                            <span class="info-pair__label">Demandeur</span>
                            <span class="info-pair__value">{{ d.demandeur.libelle }}</span>
                        </div>
                        <div class="info-pair">
                            <span class="info-pair__label">Responsable actuel</span>
                            <span class="info-pair__value">{{ d.responsableActuel.libelle }}</span>
                        </div>
                    </div>

                    <blockquote v-if="d.motif" class="demande-card__motif">
                        <i class="bi bi-chat-quote"></i>
                        {{ d.motif }}
                    </blockquote>

                    <div v-if="d.statut !== 'EN_ATTENTE' && d.decideur" class="demande-card__decision">
                        <span>Décision de {{ d.decideur.libelle }} le {{ formaterDateHeure(d.dateDecision) }}</span>
                        <p v-if="d.commentaireDecision">{{ d.commentaireDecision }}</p>
                    </div>
                </div>

                <footer class="demande-card__actions">
                    <template v-if="d.statut === 'EN_ATTENTE' && vue === 'mes-demandes'">
                        <BaseButton variant="secondary" size="sm" icon="bi bi-x-lg" @click="annuler(d.id)">
                            Annuler
                        </BaseButton>
                    </template>
                    <template v-else-if="d.statut === 'EN_ATTENTE' && vue === 'a-traiter'">
                        <BaseButton variant="secondary" size="sm" icon="bi bi-x-circle"
                            @click="ouvrirDecision('refuser', 'plan', d)">
                            Refuser
                        </BaseButton>
                        <BaseButton variant="primary" size="sm" icon="bi bi-check-lg"
                            @click="ouvrirDecision('accepter', 'plan', d)">
                            Accepter
                        </BaseButton>
                    </template>
                </footer>
            </article>
        </section>
    </div>

    <!-- Modal décision -->
    <Teleport to="body">
        <div v-if="modalDecision" class="modal-backdrop" @click.self="fermerModal">
            <div class="modal">
                <header class="modal__header">
                    <h2>{{ decisionType === 'accepter' ? 'Accepter' : 'Refuser' }} la demande</h2>
                    <button class="modal__close" @click="fermerModal" aria-label="Fermer">
                        <i class="bi bi-x-lg"></i>
                    </button>
                </header>
                <div class="modal__body">
                    <p v-if="decisionType === 'accepter'" class="modal__text">
                        En acceptant, vous transférez la responsabilité au demandeur.
                        Le suppléant actuel sera désaffecté.
                    </p>
                    <p v-else class="modal__text">
                        En refusant, le demandeur en sera informé. Le plan/activité reste inchangé.
                    </p>

                    <div class="modal__field">
                        <label class="modal__label">Commentaire (optionnel)</label>
                        <textarea v-model="decisionCommentaire" rows="3" class="modal__textarea"
                            placeholder="Motif de votre décision…" />
                    </div>
                </div>
                <footer class="modal__footer">
                    <BaseButton variant="secondary" @click="fermerModal">Annuler</BaseButton>
                    <BaseButton :variant="decisionType === 'accepter' ? 'primary' : 'secondary'"
                        :loading="decisionLoading" :disabled="decisionLoading" @click="validerDecision">
                        {{ decisionType === 'accepter' ? 'Confirmer l\'acceptation' : 'Confirmer le refus' }}
                    </BaseButton>
                </footer>
            </div>
        </div>
    </Teleport>
</template>

<style scoped>
.dr-page {
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

/* Onglets principaux */
.main-tabs {
    display: flex;
    gap: 0.5rem;
    border-bottom: 1px solid #e6ebf3;
}

.main-tab {
    display: inline-flex;
    align-items: center;
    gap: 0.45rem;
    padding: 0.65rem 1.15rem;
    background: none;
    border: none;
    border-bottom: 2px solid transparent;
    color: #6b7280;
    font-size: 0.9rem;
    font-weight: 600;
    cursor: pointer;
    transition: color .15s, border-color .15s;
}

.main-tab:hover {
    color: #1a2a6c;
}

.main-tab--active {
    color: #1a2a6c;
    border-bottom-color: #4a6cf7;
}

/* Sous-onglets */
.sub-tabs {
    display: flex;
    gap: 0.35rem;
}

.sub-tab {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.45rem 1rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 20px;
    color: #6b7280;
    font-size: 0.82rem;
    font-weight: 600;
    cursor: pointer;
    transition: background .12s, border-color .12s, color .12s;
}

.sub-tab:hover {
    background: #f8fafc;
}

.sub-tab--active {
    background: #eaf0ff;
    border-color: #cde2f6;
    color: #3b63d9;
}

.sub-tab__count {
    padding: 0.1rem 0.45rem;
    border-radius: 20px;
    background: rgba(255, 255, 255, .6);
    font-size: 0.7rem;
    font-variant-numeric: tabular-nums;
}

.sub-tab--active .sub-tab__count {
    background: #fff;
    color: #3b63d9;
}

/* Liste */
.demandes-list {
    display: flex;
    flex-direction: column;
    gap: 0.85rem;
}

.demande-card {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-left: 4px solid #c3cad5;
    border-radius: 14px;
    padding: 1rem 1.25rem;
    display: flex;
    flex-direction: column;
    gap: 0.85rem;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04);
}

.demande-card__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 0.5rem;
}

.demande-card__left {
    display: flex;
    align-items: center;
    gap: 0.6rem;
}

.demande-card__code {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.72rem;
    font-weight: 600;
    color: #1a2a6c;
    background: #eef1f5;
    padding: 0.1rem 0.4rem;
    border-radius: 3px;
}

.demande-card__date {
    font-size: 0.72rem;
    color: #8a95a8;
}

.demande-card__body {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.demande-card__title {
    margin: 0;
    font: 700 1rem Manrope, sans-serif;
    color: #1a2a6c;
    cursor: pointer;
}

.demande-card__title:hover {
    text-decoration: underline;
}

.demande-card__info {
    display: flex;
    gap: 1.5rem;
    flex-wrap: wrap;
    font-size: 0.8rem;
}

.info-pair {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
}

.info-pair__label {
    font-size: 0.68rem;
    font-weight: 700;
    text-transform: uppercase;
    color: #8a95a8;
    letter-spacing: 0.04em;
}

.info-pair__value {
    color: #15233c;
    font-weight: 600;
}

.demande-card__motif {
    margin: 0;
    padding: 0.55rem 0.85rem;
    background: #f8fafc;
    border-left: 3px solid #d4af37;
    border-radius: 6px;
    font-style: italic;
    font-size: 0.82rem;
    color: #4a5568;
    display: flex;
    gap: 0.5rem;
    align-items: flex-start;
}

.demande-card__motif i {
    color: #d4af37;
    margin-top: 0.1rem;
}

.demande-card__decision {
    padding: 0.5rem 0.85rem;
    background: #f8fafc;
    border-radius: 6px;
    font-size: 0.78rem;
    color: #6b7280;
}

.demande-card__decision p {
    margin: 0.25rem 0 0;
    color: #15233c;
    font-style: italic;
}

.demande-card__actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.5rem;
    padding-top: 0.5rem;
    border-top: 1px solid #eef1f5;
}

/* Badges */
.badge-statut {
    display: inline-block;
    font-size: 0.7rem;
    font-weight: 700;
    padding: 0.25rem 0.65rem;
    border-radius: 20px;
}

.badge-statut--attente {
    background: #fdf3e2;
    color: #9a6409;
}

.badge-statut--ok {
    background: #e6f4ea;
    color: #1e7e34;
}

.badge-statut--ko {
    background: #fde9e9;
    color: #b03030;
}

.badge-statut--annulee {
    background: #eef1f5;
    color: #6b7280;
}

/* Vide */
.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0.85rem;
    padding: 4rem 2rem;
    background: #fff;
    border: 1px dashed #dfe5ec;
    border-radius: 14px;
    text-align: center;
}

.empty-state i {
    font-size: 2.5rem;
    color: #c3cad5;
}

.empty-state h2 {
    margin: 0;
    font: 700 1rem Manrope, sans-serif;
    color: #1a2a6c;
}

.empty-state p {
    margin: 0;
    color: #6b7280;
    font-size: 0.85rem;
    max-width: 400px;
}

/* Skeleton */
.loading-state {
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
}

.skeleton-card {
    height: 140px;
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: shimmer 1.4s infinite;
    border-radius: 14px;
}

@keyframes shimmer {
    from {
        background-position: 200% 0;
    }

    to {
        background-position: -200% 0;
    }
}

/* Modal */
.modal-backdrop {
    position: fixed;
    inset: 0;
    background: rgba(16, 24, 40, .55);
    backdrop-filter: blur(4px);
    display: grid;
    place-items: center;
    z-index: 1000;
    padding: 1rem;
}

.modal {
    background: #fff;
    border-radius: 16px;
    width: 100%;
    max-width: 480px;
    box-shadow: 0 20px 60px rgba(16, 24, 40, .3);
    display: flex;
    flex-direction: column;
}

.modal__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 1rem 1.25rem;
    border-bottom: 1px solid #e6ebf3;
}

.modal__header h2 {
    margin: 0;
    font: 700 1rem Manrope, sans-serif;
    color: #1a2a6c;
}

.modal__close {
    background: none;
    border: none;
    color: #8a95a8;
    cursor: pointer;
    font-size: 1rem;
    padding: 0.25rem;
    border-radius: 4px;
}

.modal__body {
    padding: 1.25rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.modal__text {
    margin: 0;
    font-size: 0.85rem;
    color: #4a5568;
    line-height: 1.5;
}

.modal__field {
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
}

.modal__label {
    font-size: 0.78rem;
    font-weight: 600;
    color: #15233c;
}

.modal__textarea {
    width: 100%;
    padding: 0.55rem 0.75rem;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    font-size: 0.85rem;
    font-family: inherit;
    resize: vertical;
}

.modal__textarea:focus {
    outline: none;
    border-color: #4a6cf7;
    box-shadow: 0 0 0 3px rgba(74, 108, 247, .1);
}

.modal__footer {
    display: flex;
    justify-content: flex-end;
    gap: 0.5rem;
    padding: 0.85rem 1.25rem;
    border-top: 1px solid #e6ebf3;
    background: #fafbfc;
}

/* Responsive */
@media (max-width: 991.98px) {
    .dr-page {
        padding-left: 24px;
        padding-right: 24px;
    }
}

@media (max-width: 767.98px) {
    .dr-page {
        padding: 24px 16px 20px;
    }

    .demande-card__info {
        flex-direction: column;
        gap: 0.5rem;
    }

    .demande-card__actions {
        flex-direction: column-reverse;
    }
}
</style>
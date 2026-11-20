<script setup lang="ts">
import { useToastMessage } from '@/composables/useToast'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseButton, BaseModal } from '@/components/base'
import { detailPlan, saisirAvancement } from '@/services/planaction'
import type {
    AvancementPlanAction,
    PlanActionDetail,
} from '@/types/planaction'
import DemandeRepriseButton from '../demandereprise/DemandeRepriseButton.vue'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)

/* -------------------- état -------------------- */

const plan = ref<PlanActionDetail | null>(null)
const loading = ref(false)
const errorMessage = useToastMessage('error')
const successMessage = useToastMessage('success')

/* -------------------- état modal saisie -------------------- */

const modalOpen = ref(false)
const saisieValeur = ref(0)
const saisieCommentaire = ref('')
const saisieError = ref('')
const saisieEnCours = ref(false)

/* -------------------- en-tête -------------------- */

const dateDuJour = computed(() =>
    new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    }).format(new Date()).toUpperCase()
)

/* -------------------- chargement -------------------- */

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await detailPlan(id)
    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    plan.value = res.data
}

/* -------------------- actions -------------------- */

const avancementActuel = computed(() => plan.value?.avancement ?? 0)
const avancementActuelArrondi = computed(() => Math.round(avancementActuel.value))

function ouvrirSaisie(): void {
    saisieValeur.value = avancementActuelArrondi.value
    saisieCommentaire.value = ''
    saisieError.value = ''
    saisieEnCours.value = false
    modalOpen.value = true
}

// Bloque la fermeture pendant l'enregistrement
const saisieOuverte = computed({
    get: () => modalOpen.value,
    set: (v: boolean) => {
        if (saisieEnCours.value && !v) return
        modalOpen.value = v
    },
})

const peutValiderSaisie = computed(() => {
    const v = saisieValeur.value
    return (
        Number.isFinite(v) &&
        v >= avancementActuelArrondi.value &&
        v <= 100
    )
})

async function validerSaisie(): Promise<void> {
    saisieError.value = ''

    const v = saisieValeur.value
    if (!Number.isFinite(v)) {
        saisieError.value = 'Veuillez saisir une valeur valide.'
        return
    }
    if (v < avancementActuelArrondi.value) {
        saisieError.value = `La valeur ne peut pas être inférieure à l'avancement actuel (${avancementActuelArrondi.value} %).`
        return
    }
    if (v > 100) {
        saisieError.value = 'La valeur ne peut pas dépasser 100 %.'
        return
    }

    saisieEnCours.value = true
    const payload = {
        valeurPourcentage: v,
        commentaire: saisieCommentaire.value.trim() || null,
    }
    const res = await saisirAvancement(id, payload)
    saisieEnCours.value = false

    if (!res.success) {
        saisieError.value = res.error
        return
    }

    modalOpen.value = false
    successMessage.value = `Avancement enregistré : ${payload.valeurPourcentage} %`
    await charger()
    setTimeout(() => (successMessage.value = ''), 4000)
}

function retour(): void {
    router.push({ name: 'plans-action' })
}

/* -------------------- calculs -------------------- */

const estEnRetard = computed(() => {
    if (!plan.value) return false
    if (!plan.value.dateFinPrevue) return false
    if (plan.value.statut?.code === 'TERMINEE' || plan.value.statut?.code === 'ANNULEE') return false
    return new Date(plan.value.dateFinPrevue) < new Date()
})

const joursDeRetard = computed(() => {
    if (!plan.value?.dateFinPrevue || !estEnRetard.value) return 0
    const diff = Date.now() - new Date(plan.value.dateFinPrevue).getTime()
    return Math.floor(diff / (1000 * 60 * 60 * 24))
})

const peutSaisir = computed(() => {
    if (!plan.value) return false
    const code = plan.value.statut?.code
    return code !== 'BROUILLON' && code !== 'ANNULEE' && code !== null
})

const avancementsTries = computed<AvancementPlanAction[]>(() => {
    return plan.value?.historique ?? []
})

/* -------------------- helpers -------------------- */

function formaterDate(iso: string | null | undefined): string {
    if (!iso) return '—'
    const d = new Date(iso)
    return Number.isNaN(d.getTime()) ? iso : d.toLocaleDateString('fr-FR')
}

function formaterDateHeure(iso: string | null | undefined): string {
    if (!iso) return '—'
    const d = new Date(iso)
    return Number.isNaN(d.getTime()) ? iso : d.toLocaleString('fr-FR')
}

function classeStatut(code: string | undefined | null): string {
    if (code === 'NON_COMMENCEE') return 'badge-statut--non-commence'
    if (code === 'EN_COURS') return 'badge-statut--en-cours'
    if (code === 'EN_RETARD') return 'badge-statut--en-retard'
    if (code === 'TERMINEE') return 'badge-statut--termine'
    if (code === 'BROUILLON') return 'badge-statut--brouillon'
    return ''
}

function classeOrigine(type: string): string {
    const t = type.toUpperCase()
    if (t === 'INCIDENT') return 'origine-tag--incident'
    if (t === 'RISQUE') return 'origine-tag--risque'
    return 'origine-tag--autre'
}

onMounted(charger)
</script>

<template>
    <div class="pa-detail-page">
        <!-- EN-TÊTE -->
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>{{ plan?.designation || "Détail du plan d'action" }}</h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    <template v-if="plan">
                        <code class="text-code">{{ plan.code }}</code>
                        <span v-if="plan.statut" class="subheading__sep">—</span>
                        <span v-if="plan.statut" :class="['badge-statut', classeStatut(plan.statut.code)]">
                            {{ plan.statut.libelle }}
                        </span>
                    </template>
                    <template v-else>Chargement…</template>
                </p>
            </div>

            <div class="welcome-row__right">
                <BaseButton variant="secondary" icon="bi bi-arrow-left" @click="retour">
                    Retour à la liste
                </BaseButton>
                <BaseButton v-if="plan && peutSaisir" variant="primary" icon="bi bi-plus-lg" @click="ouvrirSaisie">
                    Saisir un avancement
                </BaseButton>
                <DemandeRepriseButton kind="plan" :id="id" :deja-responsable="false" @cree="charger" />
            </div>
        </header>

        <!-- SUCCÈS -->
        <!-- ERREUR -->
        <!-- CHARGEMENT -->
        <div v-if="loading" class="loading-state">
            <div class="skeleton skeleton-hero"></div>
            <div class="skeleton skeleton-card"></div>
        </div>

        <!-- CONTENU -->
        <template v-else-if="plan">

            <!-- HERO : AVANCEMENT -->
            <section class="hero" aria-labelledby="hero-label">
                <div class="hero__badges">
                    <span v-if="plan.statut" :class="['badge-statut', classeStatut(plan.statut.code)]">
                        {{ plan.statut.libelle }}
                    </span>
                    <span v-if="estEnRetard" class="badge-retard">
                        <i class="bi bi-exclamation-triangle-fill"></i>
                        En retard de {{ joursDeRetard }} j
                    </span>
                </div>

                <div class="hero__body">
                    <span id="hero-label" class="hero__label">Avancement</span>
                    <div class="hero__value">{{ plan.avancement.toFixed(0) }} %</div>

                    <div class="hero__progress">
                        <div class="hero__progress-bar" :style="{ width: plan.avancement + '%' }" :class="{
                            'hero__progress-bar--done': plan.avancement >= 100,
                            'hero__progress-bar--late': estEnRetard,
                        }" />
                    </div>

                    <div class="hero__meta">
                        <span class="hero__meta-item">
                            <i class="bi bi-calendar3"></i>
                            {{ formaterDate(plan.dateDebutPrevue) }} → {{ formaterDate(plan.dateFinPrevue) }}
                        </span>
                        <span class="hero__meta-item">
                            <i class="bi bi-person"></i>
                            {{ plan.responsable?.libelle ?? 'Aucun responsable' }}
                        </span>
                    </div>
                </div>
            </section>

            <!-- GRILLE INFO -->
            <div class="info-grid">
                <!-- Informations générales -->
                <article class="info-card">
                    <header class="info-card__head">
                        <span class="info-card__icon"><i class="bi bi-info-circle"></i></span>
                        <h2 class="info-card__title">Informations</h2>
                    </header>
                    <div class="info-card__body">
                        <div class="info-row">
                            <span class="info-row__label">Code</span>
                            <span class="info-row__value">
                                <code class="text-code">{{ plan.code }}</code>
                            </span>
                        </div>
                        <div class="info-row">
                            <span class="info-row__label">Désignation</span>
                            <span class="info-row__value">{{ plan.designation }}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-row__label">Statut</span>
                            <span class="info-row__value">
                                <span v-if="plan.statut" :class="['badge-statut', classeStatut(plan.statut.code)]">
                                    {{ plan.statut.libelle }}
                                </span>
                                <span v-else class="text-muted">—</span>
                            </span>
                        </div>
                    </div>
                </article>

                <!-- Dates -->
                <article class="info-card">
                    <header class="info-card__head">
                        <span class="info-card__icon"><i class="bi bi-calendar-range"></i></span>
                        <h2 class="info-card__title">Dates</h2>
                    </header>
                    <div class="info-card__body info-card__body--tiles">
                        <div class="date-tile">
                            <span class="date-tile__label">Début prévu</span>
                            <span class="date-tile__value">{{ formaterDate(plan.dateDebutPrevue) }}</span>
                        </div>
                        <div class="date-tile">
                            <span class="date-tile__label">Fin prévue</span>
                            <span class="date-tile__value" :class="{ 'text-danger': estEnRetard }">
                                {{ formaterDate(plan.dateFinPrevue) }}
                            </span>
                        </div>
                        <div class="date-tile">
                            <span class="date-tile__label">Début réel</span>
                            <span class="date-tile__value">{{ formaterDate(plan.dateDebutReelle) }}</span>
                        </div>
                        <div class="date-tile">
                            <span class="date-tile__label">Fin réelle</span>
                            <span class="date-tile__value">{{ formaterDate(plan.dateFinReelle) }}</span>
                        </div>
                    </div>
                </article>

                <!-- Origines -->
                <article v-if="plan.origines.length > 0" class="info-card grid-full">
                    <header class="info-card__head">
                        <span class="info-card__icon"><i class="bi bi-tag"></i></span>
                        <h2 class="info-card__title">Origines ({{ plan.origines.length }})</h2>
                    </header>
                    <div class="info-card__body">
                        <div class="origines-list">
                            <div v-for="o in plan.origines" :key="o.id" class="origine-item">
                                <span :class="['origine-tag', classeOrigine(o.typeOrigine)]">
                                    {{ o.typeOrigine }}
                                </span>
                                <div class="origine-info">
                                    <code class="origine-code">{{ o.code }}</code>
                                    <span class="origine-designation">{{ o.designation }}</span>
                                </div>
                                <span v-if="o.estPrincipale" class="origine-principale">
                                    <i class="bi bi-star-fill"></i>
                                    Principale
                                </span>
                            </div>
                        </div>
                    </div>
                </article>

                <!-- Affectations -->
                <article v-if="plan.affectations.length > 0" class="info-card grid-full">
                    <header class="info-card__head">
                        <span class="info-card__icon"><i class="bi bi-people"></i></span>
                        <h2 class="info-card__title">Affectations</h2>
                    </header>
                    <div class="info-card__body">
                        <table class="affectations-table">
                            <thead>
                                <tr>
                                    <th>Utilisateur</th>
                                    <th>Rôle</th>
                                    <th>Depuis</th>
                                    <th>Jusqu'à</th>
                                    <th>Statut</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="a in plan.affectations" :key="a.id">
                                    <td>{{ a.utilisateur?.libelle ?? '—' }}</td>
                                    <td>
                                        <span class="role-badge">{{ a.role?.code ?? '—' }}</span>
                                    </td>
                                    <td>{{ formaterDateHeure(a.dateAffectation) }}</td>
                                    <td>{{ formaterDateHeure(a.dateDesaffectation) }}</td>
                                    <td>
                                        <span
                                            :class="['badge-actif', a.active ? 'badge-actif--on' : 'badge-actif--off']">
                                            {{ a.active ? 'Actif' : 'Terminé' }}
                                        </span>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </article>
            </div>

            <!-- HISTORIQUE -->
            <section class="content-card" aria-labelledby="history-title">
                <header class="content-card__head">
                    <span class="content-card__icon"><i class="bi bi-clock-history"></i></span>
                    <h2 id="history-title" class="content-card__title">Historique d'avancement</h2>
                    <span class="content-card__count">{{ avancementsTries.length }}</span>
                </header>

                <div v-if="avancementsTries.length === 0" class="content-card__empty">
                    <i class="bi bi-clock-history"></i>
                    <p>Aucun avancement enregistré.</p>
                </div>

                <div v-else class="timeline">
                    <div v-for="(av, i) in avancementsTries" :key="av.id"
                        :class="['timeline-item', { 'timeline-item--current': i === 0 }]">
                        <div class="timeline-dot">
                            <i :class="i === 0 ? 'bi bi-circle-fill' : 'bi bi-circle'"></i>
                        </div>
                        <div class="timeline-content">
                            <div class="timeline-header">
                                <span class="timeline-value">{{ av.valeurPourcentage.toFixed(0) }} %</span>
                                <span v-if="av.statut" :class="['badge-statut', classeStatut(av.statut.code)]">
                                    {{ av.statut.libelle }}
                                </span>
                                <span v-if="i === 0" class="timeline-current-badge">Courant</span>
                            </div>
                            <p v-if="av.commentaire" class="timeline-comment">
                                {{ av.commentaire }}
                            </p>
                            <div class="timeline-meta">
                                <span>
                                    <i class="bi bi-person"></i>
                                    {{ av.utilisateur?.libelle ?? '—' }}
                                </span>
                                <span>
                                    <i class="bi bi-clock"></i>
                                    {{ formaterDateHeure(av.dateChangement) }}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
            </section>
        </template>
    </div>

    <!-- ================================================================
         MODAL SAISIE (BaseModal) -->
    <BaseModal v-model="saisieOuverte" title="Saisir un avancement" size="md" :loading="saisieEnCours"
        :close-on-outside="!saisieEnCours">
        <form class="saisie-form" @submit.prevent="validerSaisie">
            <!-- Avancement actuel -->
            <div class="saisie-current">
                <span class="saisie-current__label">Avancement actuel</span>
                <span class="saisie-current__value">{{ avancementActuelArrondi }} %</span>
            </div>

            <!-- Nouveau pourcentage -->
            <div class="form-group">
                <label for="saisie-valeur" class="form-label">Nouveau pourcentage</label>

                <div class="slider-row">
                    <input id="saisie-valeur" v-model.number="saisieValeur" type="range" :min="avancementActuelArrondi"
                        max="100" step="1" class="slider" />

                    <div class="number-wrapper">
                        <input v-model.number="saisieValeur" type="number" :min="avancementActuelArrondi" max="100"
                            step="1" class="number-input" aria-label="Pourcentage d'avancement" />
                        <span class="number-suffix">%</span>
                    </div>
                </div>

                <div class="preview-progress" aria-hidden="true">
                    <div class="preview-progress__bar"
                        :style="{ width: `${Math.min(100, Math.max(0, saisieValeur))}%` }" />
                </div>
            </div>

            <!-- Commentaire -->
            <div class="form-group">
                <label for="saisie-commentaire" class="form-label">
                    Commentaire
                    <span class="form-label__hint">(optionnel)</span>
                </label>
                <textarea id="saisie-commentaire" v-model="saisieCommentaire" class="form-textarea" rows="3"
                    placeholder="Préciser l'état d'avancement…" />
            </div>

            <!-- Erreur locale -->
            <div v-if="saisieError" class="form-error" role="alert">
                <i class="bi bi-exclamation-triangle-fill"></i>
                {{ saisieError }}
            </div>
        </form>

        <template #footer>
            <BaseButton variant="secondary" :disabled="saisieEnCours" @click="saisieOuverte = false">
                Annuler
            </BaseButton>
            <BaseButton variant="primary" icon="bi bi-check-lg" :disabled="!peutValiderSaisie || saisieEnCours"
                @click="validerSaisie">
                Enregistrer
            </BaseButton>
        </template>
    </BaseModal>
</template>

<style scoped>
/* ───────────────────────────────────────────────────────────
   Page (inchangé)
   ─────────────────────────────────────────────────────────── */
.pa-detail-page {
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

.subheading__sep {
    color: #c3cad5;
    margin: 0 0.4rem;
}

/* HERO */
.hero {
    position: relative;
    padding: 1.5rem 1.75rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-left: 4px solid #4a6cf7;
    border-radius: 16px;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04), 0 8px 24px rgba(26, 42, 108, .06);
}

.hero__badges {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
    justify-content: flex-end;
    position: absolute;
    top: 1.15rem;
    right: 1.35rem;
}

.hero__body {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
}

.hero__label {
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: #8a95a8;
}

.hero__value {
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 3rem;
    font-weight: 800;
    line-height: 1;
    color: #1a2a6c;
    letter-spacing: -0.02em;
    font-variant-numeric: tabular-nums;
    margin: 0.15rem 0 0.6rem;
}

.hero__progress {
    height: 10px;
    background: #eef1f5;
    border-radius: 5px;
    overflow: hidden;
    margin-bottom: 1rem;
    max-width: 600px;
}

.hero__progress-bar {
    height: 100%;
    background: linear-gradient(90deg, #4a6cf7, #6b8dff);
    border-radius: 5px;
    transition: width .4s ease;
}

.hero__progress-bar--done {
    background: linear-gradient(90deg, #1e7e34, #28a745);
}

.hero__progress-bar--late {
    background: linear-gradient(90deg, #d64545, #e26e6e);
}

.hero__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 0.35rem 1.25rem;
    font-size: 0.82rem;
    color: #6b7280;
}

.hero__meta-item {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
}

.hero__meta-item i {
    color: #8a95a8;
}

/* INFO GRID */
.info-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1rem;
}

.info-card {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04), 0 8px 24px rgba(26, 42, 108, .06);
}

.grid-full {
    grid-column: 1 / -1;
}

.info-card__head {
    display: flex;
    align-items: center;
    gap: 0.65rem;
    padding: 0.85rem 1.25rem;
    background: linear-gradient(140deg, #f7f9fd, #fff);
    border-bottom: 1px solid #e6ebf3;
}

.info-card__icon {
    display: grid;
    place-items: center;
    width: 30px;
    height: 30px;
    border-radius: 9px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.9rem;
}

.info-card__title {
    margin: 0;
    font-family: Manrope, sans-serif;
    font-size: 0.88rem;
    font-weight: 700;
    color: #1a2a6c;
}

.info-card__body {
    padding: 0.4rem 1.25rem 0.75rem;
}

.info-card__body--tiles {
    padding: 1rem 1.25rem;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0.6rem;
}

/* Info rows */
.info-row {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 1rem;
    padding: 0.55rem 0;
    border-bottom: 1px dashed #eef1f5;
}

.info-row:last-child {
    border-bottom: none;
}

.info-row__label {
    font-size: 0.72rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: #8a95a8;
}

.info-row__value {
    color: #15233c;
    font-size: 0.86rem;
    text-align: right;
}

/* Date tiles */
.date-tile {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    padding: 0.75rem 0.85rem;
    background: #f8fafc;
    border: 1px solid #eef1f5;
    border-radius: 10px;
}

.date-tile__label {
    font-size: 0.66rem;
    font-weight: 700;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    color: #8a95a8;
}

.date-tile__value {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.92rem;
    font-weight: 600;
    color: #1a2a6c;
}

/* Origines */
.origines-list {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.origine-item {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.65rem 0.85rem;
    background: #f8fafc;
    border-radius: 8px;
    border-left: 3px solid #dfe5ec;
}

.origine-tag {
    display: inline-block;
    padding: 0.15rem 0.55rem;
    border-radius: 20px;
    font-size: 0.65rem;
    font-weight: 800;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    flex: none;
}

.origine-tag--incident {
    background: #fde9e9;
    color: #b03030;
}

.origine-tag--risque {
    background: #fdf3e2;
    color: #9a6409;
}

.origine-tag--autre {
    background: #eef1f5;
    color: #5b6b7d;
}

.origine-info {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
    flex: 1;
}

.origine-code {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.75rem;
    font-weight: 600;
    color: #1a2a6c;
    background: #eef1f5;
    padding: 0.1rem 0.4rem;
    border-radius: 3px;
    align-self: flex-start;
}

.origine-designation {
    font-size: 0.82rem;
    color: #15233c;
}

.origine-principale {
    font-size: 0.7rem;
    color: #b8860b;
    font-weight: 700;
    display: inline-flex;
    align-items: center;
    gap: 0.25rem;
    flex: none;
}

/* Affectations */
.affectations-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.82rem;
}

.affectations-table th {
    text-align: left;
    padding: 0.5rem 0.75rem 0.5rem 0;
    font-size: 0.68rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: #8a95a8;
    border-bottom: 1px solid #eef1f5;
}

.affectations-table td {
    padding: 0.65rem 0.75rem 0.65rem 0;
    border-bottom: 1px solid #f5f7fa;
    color: #15233c;
}

.affectations-table tr:last-child td {
    border-bottom: none;
}

.role-badge {
    display: inline-block;
    padding: 0.15rem 0.5rem;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.7rem;
    font-weight: 700;
    border-radius: 20px;
}

.badge-actif {
    display: inline-block;
    padding: 0.15rem 0.55rem;
    border-radius: 20px;
    font-size: 0.68rem;
    font-weight: 700;
}

.badge-actif--on {
    background: #e6f4ea;
    color: #1e7e34;
}

.badge-actif--off {
    background: #eef1f5;
    color: #6b7280;
}

/* Timeline */
.content-card {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04), 0 8px 24px rgba(26, 42, 108, .06);
}

.content-card__head {
    display: flex;
    align-items: center;
    gap: 0.65rem;
    padding: 0.85rem 1.25rem;
    background: linear-gradient(140deg, #f7f9fd, #fff);
    border-bottom: 1px solid #e6ebf3;
}

.content-card__icon {
    display: grid;
    place-items: center;
    width: 30px;
    height: 30px;
    border-radius: 9px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.9rem;
}

.content-card__title {
    margin: 0;
    font-family: Manrope, sans-serif;
    font-size: 0.92rem;
    font-weight: 700;
    color: #1a2a6c;
    flex: 1;
}

.content-card__count {
    padding: 0.15rem 0.55rem;
    border-radius: 20px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.7rem;
    font-weight: 700;
}

.content-card__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    padding: 2.5rem 1.5rem;
    color: #8a95a8;
}

.content-card__empty i {
    font-size: 2rem;
    opacity: 0.5;
}

.content-card__empty p {
    margin: 0;
    font-size: 0.88rem;
}

.timeline {
    padding: 1.25rem;
    display: flex;
    flex-direction: column;
    gap: 0;
    position: relative;
}

.timeline-item {
    display: flex;
    gap: 0.85rem;
    padding: 0.75rem 0;
    position: relative;
}

.timeline-item:not(:last-child)::before {
    content: '';
    position: absolute;
    left: 10px;
    top: 32px;
    bottom: -8px;
    width: 2px;
    background: #eef1f5;
}

.timeline-dot {
    flex: none;
    width: 22px;
    height: 22px;
    display: grid;
    place-items: center;
    color: #c3cad5;
    font-size: 0.6rem;
    background: #fff;
    z-index: 1;
}

.timeline-item--current .timeline-dot {
    color: #4a6cf7;
}

.timeline-content {
    flex: 1;
    min-width: 0;
}

.timeline-header {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex-wrap: wrap;
    margin-bottom: 0.35rem;
}

.timeline-value {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.95rem;
    font-weight: 700;
    color: #1a2a6c;
}

.timeline-current-badge {
    padding: 0.1rem 0.5rem;
    background: #eaf0ff;
    color: #3b63d9;
    border-radius: 20px;
    font-size: 0.65rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
}

.timeline-comment {
    margin: 0.25rem 0 0.35rem;
    font-size: 0.85rem;
    color: #4a5568;
    font-style: italic;
    padding: 0.4rem 0.7rem;
    background: #f8fafc;
    border-left: 2px solid #d4af37;
    border-radius: 4px;
}

.timeline-meta {
    display: flex;
    gap: 1rem;
    font-size: 0.75rem;
    color: #8a95a8;
    flex-wrap: wrap;
}

.timeline-meta span {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
}

/* Badges statut */
.badge-statut {
    display: inline-block;
    font-size: 0.7rem;
    font-weight: 700;
    padding: 0.25rem 0.65rem;
    border-radius: 20px;
}

.badge-statut--non-commence {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
}

.badge-statut--en-cours {
    background: #e4f0fb;
    color: #135d95;
    border: 1px solid #cde2f6;
}

.badge-statut--en-retard {
    background: #fde9e9;
    color: #b03030;
    border: 1px solid #f4c7c7;
}

.badge-statut--termine {
    background: #e6f4ea;
    color: #1e7e34;
    border: 1px solid #c8e5cf;
}

.badge-statut--brouillon {
    background: #fdf3e2;
    color: #9a6409;
    border: 1px solid #f4e2c2;
}

.badge-retard {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
    padding: 0.25rem 0.65rem;
    background: #fde9e9;
    color: #b03030;
    border: 1px solid #f4c7c7;
    border-radius: 20px;
    font-size: 0.7rem;
    font-weight: 700;
}

.text-code {
    display: inline-block;
    font-size: 0.78rem;
    padding: 0.15rem 0.5rem;
    background: #eef1f5;
    color: #384a5e;
    border-radius: 4px;
    font-family: ui-monospace, Menlo, monospace;
    font-weight: 600;
}

.text-danger {
    color: #b03030;
    font-weight: 600;
}

.text-muted {
    color: #8a95a8;
}

/* Skeleton */
.loading-state {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.skeleton {
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: shimmer 1.4s infinite;
    border-radius: 12px;
}

.skeleton-hero {
    height: 180px;
}

.skeleton-card {
    height: 240px;
}

@keyframes shimmer {
    from {
        background-position: 200% 0;
    }

    to {
        background-position: -200% 0;
    }
}

/* ───────────────────────────────────────────────────────────
   Formulaire saisie (dans le modal)
   ─────────────────────────────────────────────────────────── */
.saisie-form {
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
}

.saisie-current {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0.75rem 1rem;
    background: #f5f7fa;
    border: 1px solid #e6ebf3;
    border-radius: 10px;
}

.saisie-current__label {
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    color: #8a95a8;
}

.saisie-current__value {
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 1.35rem;
    font-weight: 800;
    color: #1a2a6c;
    font-variant-numeric: tabular-nums;
}

.form-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.form-label {
    font-size: 0.82rem;
    font-weight: 600;
    color: #15233c;
}

.form-label__hint {
    color: #8a95a8;
    font-weight: 400;
}

.slider-row {
    display: flex;
    align-items: center;
    gap: 1rem;
}

.slider {
    flex: 1;
    appearance: none;
    height: 8px;
    border-radius: 4px;
    background: #eef1f5;
    outline: none;
    cursor: pointer;
}

.slider::-webkit-slider-thumb {
    appearance: none;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: #4a6cf7;
    border: 3px solid #fff;
    box-shadow: 0 2px 6px rgba(74, 108, 247, 0.4);
    cursor: pointer;
}

.slider::-moz-range-thumb {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: #4a6cf7;
    border: 3px solid #fff;
    box-shadow: 0 2px 6px rgba(74, 108, 247, 0.4);
    cursor: pointer;
}

.number-wrapper {
    position: relative;
    width: 92px;
    flex: none;
}

.number-input {
    width: 100%;
    padding: 0.55rem 1.75rem 0.55rem 0.75rem;
    border: 1px solid #d8dee7;
    border-radius: 10px;
    font-size: 0.92rem;
    font-family: ui-monospace, Menlo, monospace;
    font-weight: 600;
    color: #1a2a6c;
    background: #fff;
    outline: none;
    transition: border-color 0.15s, box-shadow 0.15s;
}

.number-input:focus {
    border-color: #4a6cf7;
    box-shadow: 0 0 0 3px rgba(74, 108, 247, 0.15);
}

.number-suffix {
    position: absolute;
    right: 0.65rem;
    top: 50%;
    transform: translateY(-50%);
    font-size: 0.8rem;
    font-weight: 700;
    color: #8a95a8;
    pointer-events: none;
}

.preview-progress {
    height: 6px;
    background: #eef1f5;
    border-radius: 3px;
    overflow: hidden;
    margin-top: 0.35rem;
}

.preview-progress__bar {
    height: 100%;
    background: linear-gradient(90deg, #4a6cf7, #6b8dff);
    border-radius: 3px;
    transition: width 0.25s ease;
}

.form-textarea {
    width: 100%;
    padding: 0.7rem 0.85rem;
    border: 1px solid #d8dee7;
    border-radius: 10px;
    font-size: 0.88rem;
    font-family: inherit;
    color: #15233c;
    background: #fff;
    resize: vertical;
    min-height: 80px;
    outline: none;
    transition: border-color 0.15s, box-shadow 0.15s;
}

.form-textarea:focus {
    border-color: #4a6cf7;
    box-shadow: 0 0 0 3px rgba(74, 108, 247, 0.15);
}

.form-error {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.65rem 0.85rem;
    background: #fde9e9;
    color: #b03030;
    border: 1px solid #f4c7c7;
    border-radius: 10px;
    font-size: 0.82rem;
    font-weight: 500;
}

/* Responsive */
@media (max-width: 991.98px) {
    .pa-detail-page {
        padding-left: 24px;
        padding-right: 24px;
    }

    .info-grid {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 767.98px) {
    .pa-detail-page {
        padding: 24px 16px 20px;
    }

    .hero__value {
        font-size: 2.4rem;
    }

    .hero__badges {
        position: static;
        justify-content: flex-start;
        margin-bottom: 0.75rem;
    }

    .info-card__body--tiles {
        grid-template-columns: 1fr;
    }

    .affectations-table th:nth-child(4),
    .affectations-table td:nth-child(4) {
        display: none;
    }
}
</style>
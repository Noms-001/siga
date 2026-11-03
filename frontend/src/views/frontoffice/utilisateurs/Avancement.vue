<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseButton } from '@/components/base'
import { getAvancementUtilisateur } from '@/services/utilisateurAvancement'
import type {
    ActiviteAvancement,
    PlanActionAvancement,
    UtilisateurAvancement,
} from '@/types/utilisateurAvancement'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)

const data = ref<UtilisateurAvancement | null>(null)
const loading = ref(false)
const errorMessage = ref('')

const annee = ref(new Date().getFullYear())
const ongletActif = ref<'activites' | 'plans'>('activites')

const dateDuJour = computed(() =>
    new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    }).format(new Date()).toUpperCase()
)

/* -------------------- chargement -------------------- */

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await getAvancementUtilisateur(id, annee.value)
    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    data.value = res.data
}

function anneePrecedente(): void {
    annee.value--
    charger()
}

function anneeSuivante(): void {
    annee.value++
    charger()
}

/* -------------------- helpers -------------------- */

function formaterDate(iso: string | null | undefined): string {
    if (!iso) return '—'
    const d = new Date(iso)
    return Number.isNaN(d.getTime()) ? iso : d.toLocaleDateString('fr-FR')
}

function classeStatut(code: string | null | undefined): string {
    if (code === 'NON_COMMENCEE') return 'badge-statut--non-commence'
    if (code === 'EN_COURS') return 'badge-statut--en-cours'
    if (code === 'EN_RETARD') return 'badge-statut--en-retard'
    if (code === 'TERMINEE') return 'badge-statut--termine'
    return ''
}

function classeRole(code: string | null | undefined): string {
    if (code === 'RESPONSABLE') return 'role--responsable'
    if (code === 'SUPPLEANT') return 'role--suppleant'
    if (code === 'PARTICIPANT') return 'role--participant'
    return ''
}

function voirActivite(a: ActiviteAvancement): void {
    router.push({ name: 'activite-detail', params: { id: a.id } })
}

function voirPlan(p: PlanActionAvancement): void {
    router.push({ name: 'plan-action-detail', params: { id: p.id } })
}

function compterActivites(liste: ActiviteAvancement[]): number {
    return liste.length
}

onMounted(charger)
</script>

<template>
    <div class="ua-page">
        <!-- En-tête -->
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>
                    Avancement de
                    <span v-if="data">{{ data.utilisateur.libelle }}</span>
                    <span v-else>l'utilisateur</span>
                </h1>
                <span class="title-divider"></span>
                <p class="subheading">
                    Activités et plans d'action sur l'année {{ annee }}
                </p>
            </div>

            <div class="welcome-row__right">
                <div class="year-nav">
                    <button class="nav-btn" @click="anneePrecedente">
                        <i class="bi bi-chevron-left"></i>
                    </button>
                    <span class="nav-year">{{ annee }}</span>
                    <button class="nav-btn" @click="anneeSuivante">
                        <i class="bi bi-chevron-right"></i>
                    </button>
                </div>
                <BaseButton variant="secondary" icon="bi bi-arrow-clockwise" @click="charger">
                    Actualiser
                </BaseButton>
            </div>
        </header>

        <!-- Erreur -->
        <div v-if="errorMessage" class="alert alert-danger d-flex justify-content-between">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="charger">Réessayer</BaseButton>
        </div>

        <!-- Chargement -->
        <div v-else-if="loading" class="loading-state">
            <div class="skeleton skeleton-cards"></div>
            <div class="skeleton skeleton-table"></div>
        </div>

        <!-- Contenu -->
        <template v-else-if="data">

            <!-- Cards de synthèse : activités -->
            <section v-if="data.activites.total > 0" class="section">
                <header class="section__head">
                    <span class="section__icon"><i class="bi bi-list-task"></i></span>
                    <h2 class="section__title">Activités</h2>
                    <span class="section__count">{{ data.activites.total }}</span>
                </header>

                <div class="cards">
                    <div class="card card--neutral">
                        <span class="card__label">Total</span>
                        <span class="card__value">{{ data.activites.total }}</span>
                    </div>
                    <div class="card card--ind">
                        <span class="card__label">Non commencées</span>
                        <span class="card__value">{{ data.activites.nonCommencees }}</span>
                    </div>
                    <div class="card card--info">
                        <span class="card__label">En cours</span>
                        <span class="card__value">{{ data.activites.enCours }}</span>
                    </div>
                    <div class="card card--ko">
                        <span class="card__label">En retard</span>
                        <span class="card__value">{{ data.activites.enRetard }}</span>
                    </div>
                    <div class="card card--ok">
                        <span class="card__label">Terminées</span>
                        <span class="card__value">{{ data.activites.terminees }}</span>
                    </div>
                    <div class="card card--avancement">
                        <span class="card__label">Avancement moyen</span>
                        <span class="card__value">{{ data.activites.avancementMoyenGlobal.toFixed(0) }} %</span>
                        <div class="card__progress">
                            <div class="card__progress-bar"
                                :style="{ width: data.activites.avancementMoyenGlobal + '%' }" />
                        </div>
                    </div>
                </div>

                <div class="panel">
                    <div class="table-wrap">
                        <table class="detail-table">
                            <thead>
                                <tr>
                                    <th>Code</th>
                                    <th>Désignation</th>
                                    <th>Rôle</th>
                                    <th>Statut</th>
                                    <th>Avancement</th>
                                    <th>Échéance</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="a in data.activites.details" :key="a.id" class="detail-row"
                                    @click="voirActivite(a)">
                                    <td><code class="code">{{ a.code }}</code></td>
                                    <td class="col-design">{{ a.designation }}</td>
                                    <td>
                                        <span v-if="a.role" :class="['role-badge', classeRole(a.role.code)]">
                                            {{ a.role.code }}
                                        </span>
                                        <span v-else class="text-muted">—</span>
                                    </td>
                                    <td>
                                        <span v-if="a.statut" :class="['badge-statut', classeStatut(a.statut.code)]">
                                            {{ a.statut.libelle }}
                                        </span>
                                        <span v-else class="text-muted">—</span>
                                    </td>
                                    <td class="col-progress">
                                        <div class="progress">
                                            <div class="progress__bar" :style="{ width: a.avancement + '%' }" />
                                        </div>
                                        <span class="progress__label">{{ a.avancement.toFixed(0) }} %</span>
                                    </td>
                                    <td class="col-date">
                                        <span :class="{ 'text-ko': a.enRetard }">
                                            {{ formaterDate(a.dateFinPrevue) }}
                                        </span>
                                        <span v-if="a.joursDeRetard > 0" class="retard-badge">
                                            +{{ a.joursDeRetard }} j
                                        </span>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </section>

            <!-- Cards de synthèse : plans -->
            <section v-if="data.plansAction.total > 0" class="section">
                <header class="section__head">
                    <span class="section__icon"><i class="bi bi-kanban"></i></span>
                    <h2 class="section__title">Plans d'action</h2>
                    <span class="section__count">{{ data.plansAction.total }}</span>
                </header>

                <div class="cards">
                    <div class="card card--neutral">
                        <span class="card__label">Total</span>
                        <span class="card__value">{{ data.plansAction.total }}</span>
                    </div>
                    <div class="card card--ind">
                        <span class="card__label">Non commencés</span>
                        <span class="card__value">{{ data.plansAction.nonCommences }}</span>
                    </div>
                    <div class="card card--info">
                        <span class="card__label">En cours</span>
                        <span class="card__value">{{ data.plansAction.enCours }}</span>
                    </div>
                    <div class="card card--ko">
                        <span class="card__label">En retard</span>
                        <span class="card__value">{{ data.plansAction.enRetard }}</span>
                    </div>
                    <div class="card card--ok">
                        <span class="card__label">Terminés</span>
                        <span class="card__value">{{ data.plansAction.termines }}</span>
                    </div>
                    <div class="card card--avancement">
                        <span class="card__label">Avancement moyen</span>
                        <span class="card__value">{{ data.plansAction.avancementMoyenGlobal.toFixed(0) }} %</span>
                        <div class="card__progress">
                            <div class="card__progress-bar"
                                :style="{ width: data.plansAction.avancementMoyenGlobal + '%' }" />
                        </div>
                    </div>
                </div>

                <div class="panel">
                    <div class="table-wrap">
                        <table class="detail-table">
                            <thead>
                                <tr>
                                    <th>Code</th>
                                    <th>Désignation</th>
                                    <th>Rôle</th>
                                    <th>Statut</th>
                                    <th>Avancement</th>
                                    <th>Échéance</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="p in data.plansAction.details" :key="p.id" class="detail-row"
                                    @click="voirPlan(p)">
                                    <td><code class="code">{{ p.code }}</code></td>
                                    <td class="col-design">{{ p.designation }}</td>
                                    <td>
                                        <span v-if="p.role" :class="['role-badge', classeRole(p.role.code)]">
                                            {{ p.role.code }}
                                        </span>
                                        <span v-else class="text-muted">—</span>
                                    </td>
                                    <td>
                                        <span v-if="p.statut" :class="['badge-statut', classeStatut(p.statut.code)]">
                                            {{ p.statut.libelle }}
                                        </span>
                                        <span v-else class="text-muted">—</span>
                                    </td>
                                    <td class="col-progress">
                                        <div class="progress">
                                            <div class="progress__bar" :style="{ width: p.avancement + '%' }" />
                                        </div>
                                        <span class="progress__label">{{ p.avancement.toFixed(0) }} %</span>
                                    </td>
                                    <td class="col-date">
                                        <span :class="{ 'text-ko': p.enRetard }">
                                            {{ formaterDate(p.dateFinPrevue) }}
                                        </span>
                                        <span v-if="p.joursDeRetard > 0" class="retard-badge">
                                            +{{ p.joursDeRetard }} j
                                        </span>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </section>

            <!-- Aucune affectation -->
            <section v-if="data.activites.total === 0 && data.plansAction.total === 0" class="empty-state">
                <i class="bi bi-inbox"></i>
                <h2>Aucune affectation sur {{ data.annee }}</h2>
                <p>Cet utilisateur n'a été affecté à aucune activité ni aucun plan d'action en {{ data.annee }}.</p>
            </section>
        </template>
    </div>
</template>

<style scoped>
.ua-page {
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

/* Year nav */
.year-nav {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0 0.5rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 10px;
    height: 38px;
}

.nav-btn {
    display: grid;
    place-items: center;
    width: 26px;
    height: 26px;
    border: none;
    background: none;
    color: #6b7280;
    border-radius: 6px;
    cursor: pointer;
}

.nav-btn:hover {
    background: #f5f7fa;
    color: #1a2a6c;
}

.nav-year {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.9rem;
    font-weight: 700;
    color: #1a2a6c;
    min-width: 50px;
    text-align: center;
}

/* Sections */
.section {
    display: flex;
    flex-direction: column;
    gap: 0.85rem;
}

.section__head {
    display: flex;
    align-items: center;
    gap: 0.65rem;
}

.section__icon {
    display: grid;
    place-items: center;
    width: 32px;
    height: 32px;
    border-radius: 9px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.95rem;
}

.section__title {
    margin: 0;
    font-family: Manrope, sans-serif;
    font-size: 1.05rem;
    font-weight: 700;
    color: #1a2a6c;
    flex: 1;
}

.section__count {
    padding: 0.15rem 0.6rem;
    border-radius: 20px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.72rem;
    font-weight: 700;
}

/* Cards */
.cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    gap: 0.75rem;
}

.card {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 12px;
    padding: 0.85rem 1rem;
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04);
}

.card__label {
    font-size: 0.66rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: #8a95a8;
}

.card__value {
    font-family: Manrope, sans-serif;
    font-size: 1.5rem;
    font-weight: 800;
    line-height: 1;
    color: #1a2a6c;
    font-variant-numeric: tabular-nums;
}

.card--ok .card__value {
    color: #1e7e34;
}

.card--ko .card__value {
    color: #b03030;
}

.card--info .card__value {
    color: #135d95;
}

.card--ind .card__value {
    color: #9a6409;
}

.card--avancement {
    grid-column: span 2;
}

.card__progress {
    height: 6px;
    background: #eef1f5;
    border-radius: 3px;
    overflow: hidden;
    margin-top: 0.25rem;
}

.card__progress-bar {
    height: 100%;
    background: linear-gradient(90deg, #4a6cf7, #6b8dff);
    border-radius: 3px;
}

/* Panels + table */
.panel {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04);
}

.table-wrap {
    overflow-x: auto;
}

.detail-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.83rem;
}

.detail-table thead th {
    padding: 0.65rem 1rem;
    text-align: left;
    font-size: 0.68rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: #8a95a8;
    background: #f8fafc;
    border-bottom: 1px solid #e6ebf3;
    white-space: nowrap;
}

.detail-table tbody td {
    padding: 0.7rem 1rem;
    border-bottom: 1px solid #f5f7fa;
    color: #15233c;
    vertical-align: middle;
}

.detail-row {
    cursor: pointer;
    transition: background .12s;
}

.detail-row:hover {
    background: #f8fafc;
}

.detail-row:last-child td {
    border-bottom: none;
}

.code {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.75rem;
    font-weight: 600;
    color: #1a2a6c;
    background: #eef1f5;
    padding: 0.1rem 0.4rem;
    border-radius: 3px;
}

.col-design {
    max-width: 280px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.col-progress {
    width: 160px;
}

.col-date {
    white-space: nowrap;
}

.progress {
    display: inline-block;
    width: 80px;
    height: 6px;
    background: #eef1f5;
    border-radius: 3px;
    overflow: hidden;
    vertical-align: middle;
    margin-right: 0.45rem;
}

.progress__bar {
    height: 100%;
    background: linear-gradient(90deg, #4a6cf7, #6b8dff);
    border-radius: 3px;
}

.progress__label {
    font-size: 0.75rem;
    color: #6b7280;
    font-variant-numeric: tabular-nums;
}

.retard-badge {
    display: inline-block;
    margin-left: 0.4rem;
    padding: 0.1rem 0.4rem;
    background: #fde9e9;
    color: #b03030;
    border-radius: 20px;
    font-size: 0.65rem;
    font-weight: 700;
}

/* Badges */
.badge-statut {
    display: inline-block;
    font-size: 0.68rem;
    font-weight: 700;
    padding: 0.2rem 0.55rem;
    border-radius: 20px;
    white-space: nowrap;
}

.badge-statut--non-commence {
    background: #eef1f5;
    color: #5b6b7d;
}

.badge-statut--en-cours {
    background: #e4f0fb;
    color: #135d95;
}

.badge-statut--en-retard {
    background: #fde9e9;
    color: #b03030;
}

.badge-statut--termine {
    background: #e6f4ea;
    color: #1e7e34;
}

.role-badge {
    display: inline-block;
    font-size: 0.65rem;
    font-weight: 700;
    padding: 0.15rem 0.5rem;
    border-radius: 20px;
    text-transform: uppercase;
    letter-spacing: 0.03em;
    white-space: nowrap;
}

.role--responsable {
    background: #eaf0ff;
    color: #3b63d9;
}

.role--suppleant {
    background: #fdf3e2;
    color: #9a6409;
}

.role--participant {
    background: #f0f0f0;
    color: #6b7280;
}

.text-ko {
    color: #b03030;
    font-weight: 600;
}

.text-muted {
    color: #8a95a8;
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
    max-width: 450px;
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

.skeleton-cards {
    height: 100px;
}

.skeleton-table {
    height: 300px;
}

@keyframes shimmer {
    from {
        background-position: 200% 0;
    }

    to {
        background-position: -200% 0;
    }
}

/* Responsive */
@media (max-width: 991.98px) {
    .ua-page {
        padding-left: 24px;
        padding-right: 24px;
    }
}

@media (max-width: 767.98px) {
    .ua-page {
        padding: 24px 16px 20px;
    }

    .card--avancement {
        grid-column: span 1;
    }

    .detail-table thead th:nth-child(3),
    .detail-table tbody td:nth-child(3) {
        display: none;
    }
}
</style>
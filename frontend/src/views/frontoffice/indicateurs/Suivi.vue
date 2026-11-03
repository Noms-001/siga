<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BaseButton } from '@/components/base'
import { getDashboardIndicateurs } from '@/services/indicateurFront'
import type { DashboardIndicateur } from '@/types/backoffice/indicateur'

const router = useRouter()
const data = ref<DashboardIndicateur | null>(null)
const loading = ref(false)
const errorMessage = ref('')

const dateDuJour = computed(() =>
    new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    }).format(new Date()).toUpperCase()
)

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await getDashboardIndicateurs()
    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    data.value = res.data
}

function voirIndicateur(id: number): void {
    router.push({ name: 'indicateur-detail', params: { id } })
}

function formaterValeur(v: number | null, unite: string | null | undefined): string {
    if (v === null || v === undefined) return '—'
    const n = Number(v).toLocaleString('fr-FR', { maximumFractionDigits: 2 })
    return unite ? `${n} ${unite}` : n
}

function formaterDate(iso: string | null): string {
    if (!iso) return '—'
    return new Date(iso).toLocaleDateString('fr-FR')
}

function classeStatut(statut: string): string {
    return {
        ATTEINT: 'badge-statut--ok',
        NON_ATTEINT: 'badge-statut--ko',
        INDETERMINE: 'badge-statut--ind',
        JAMAIS_SAISI: 'badge-statut--vide',
    }[statut] ?? ''
}

function libelleStatut(statut: string): string {
    return {
        ATTEINT: 'Atteint',
        NON_ATTEINT: 'Non atteint',
        INDETERMINE: 'Indéterminé',
        JAMAIS_SAISI: 'Jamais saisi',
    }[statut] ?? statut
}

function classeType(type: string | null): string {
    if (type === 'KPI') return 'badge-type--kpi'
    if (type === 'KRI') return 'badge-type--kri'
    return 'badge-type--autre'
}

onMounted(charger)
</script>

<template>
    <div class="ind-suivi-page">
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>Suivi des indicateurs</h1>
                <span class="title-divider"></span>
                <p class="subheading">
                    Vue d'ensemble du portefeuille d'indicateurs du périmètre
                </p>
            </div>
            <div class="welcome-row__right">
                <BaseButton variant="secondary" icon="bi bi-arrow-clockwise" @click="charger">
                    Actualiser
                </BaseButton>
            </div>
        </header>

        <div v-if="errorMessage" class="alert alert-danger d-flex justify-content-between align-items-center">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="charger">Réessayer</BaseButton>
        </div>

        <div v-else-if="loading" class="loading-state">
            <div class="skeleton skeleton-cards"></div>
            <div class="skeleton skeleton-cards"></div>
        </div>

        <template v-else-if="data">

            <!-- Cards de synthèse -->
            <section class="cards">
                <div class="card card--total">
                    <span class="card__label">Total indicateurs</span>
                    <span class="card__value">{{ data.resume.total }}</span>
                </div>
                <div class="card card--ok">
                    <span class="card__label">Atteints</span>
                    <span class="card__value">{{ data.resume.atteints }}</span>
                </div>
                <div class="card card--ko">
                    <span class="card__label">Non atteints</span>
                    <span class="card__value">{{ data.resume.nonAtteints }}</span>
                </div>
                <div class="card card--ind">
                    <span class="card__label">Indéterminés</span>
                    <span class="card__value">{{ data.resume.indetermines }}</span>
                </div>
                <div class="card card--retard">
                    <span class="card__label">En retard de saisie</span>
                    <span class="card__value">{{ data.resume.enRetard }}</span>
                </div>
                <div class="card card--vide">
                    <span class="card__label">Jamais saisis</span>
                    <span class="card__value">{{ data.resume.jamaisSaisis }}</span>
                </div>
            </section>

            <!-- Indicateurs en retard -->
            <section class="panel">
                <header class="panel__head">
                    <span class="panel__icon panel__icon--danger">
                        <i class="bi bi-exclamation-triangle"></i>
                    </span>
                    <h2 class="panel__title">Indicateurs en retard de saisie</h2>
                    <span class="panel__count">{{ data.enRetard.length }}</span>
                </header>

                <div v-if="data.enRetard.length === 0" class="empty">
                    <i class="bi bi-check-circle"></i>
                    Aucun indicateur en retard.
                </div>

                <ul v-else class="retard-list">
                    <li v-for="r in data.enRetard" :key="r.idIndicateur" class="retard-item"
                        @click="voirIndicateur(r.idIndicateur)">
                        <div class="retard-item__head">
                            <code class="retard-item__code">{{ r.code }}</code>
                            <span v-if="r.statut === 'JAMAIS_SAISI'" class="retard-badge retard-badge--vide">
                                Jamais saisi
                            </span>
                            <span v-else class="retard-badge">
                                {{ r.joursDeRetard }} j de retard
                            </span>
                        </div>
                        <span class="retard-item__designation">{{ r.designation }}</span>
                        <div class="retard-item__meta">
                            <span>{{ r.frequenceVerification }}</span>
                            <span v-if="r.dernierePeriodeFin">
                                Dernière : {{ formaterDate(r.dernierePeriodeFin) }}
                            </span>
                        </div>
                    </li>
                </ul>
            </section>

            <!-- Tous les indicateurs -->
            <section class="panel">
                <header class="panel__head">
                    <span class="panel__icon">
                        <i class="bi bi-list-check"></i>
                    </span>
                    <h2 class="panel__title">Tous les indicateurs</h2>
                    <span class="panel__count">{{ data.indicateurs.length }}</span>
                </header>

                <div v-if="data.indicateurs.length === 0" class="empty">
                    <i class="bi bi-inbox"></i>
                    Aucun indicateur.
                </div>

                <div v-else class="table-wrap">
                    <table class="indic-table">
                        <thead>
                            <tr>
                                <th>Code</th>
                                <th>Désignation</th>
                                <th>Type</th>
                                <th>Cible</th>
                                <th>Valeur</th>
                                <th>Écart</th>
                                <th>Statut</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="ind in data.indicateurs" :key="ind.id" class="indic-row"
                                @click="voirIndicateur(ind.id)">
                                <td class="col-code">
                                    <code>{{ ind.code }}</code>
                                </td>
                                <td class="col-designation">{{ ind.designation }}</td>
                                <td>
                                    <span v-if="ind.typeIndicateur"
                                        :class="['badge-type', classeType(ind.typeIndicateur)]">
                                        {{ ind.typeIndicateur }}
                                    </span>
                                    <span v-else class="text-muted">—</span>
                                </td>
                                <td class="col-num">{{ formaterValeur(ind.valeurCible, ind.uniteMesure) }}</td>
                                <td class="col-num col-val">
                                    {{ formaterValeur(ind.valeurCourante, ind.uniteMesure) }}
                                </td>
                                <td class="col-num">
                                    <span v-if="ind.ecart !== null" :class="ind.ecart >= 0 ? 'text-ok' : 'text-ko'">
                                        {{ ind.ecart > 0 ? '+' : '' }}{{ ind.ecart.toFixed(2) }}
                                    </span>
                                    <span v-else class="text-muted">—</span>
                                </td>
                                <td>
                                    <span :class="['badge-statut', classeStatut(ind.statut)]">
                                        {{ libelleStatut(ind.statut) }}
                                    </span>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>
        </template>
    </div>
</template>

<style scoped>
.ind-suivi-page {
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

/* Cards */
.cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    gap: 0.85rem;
}

.card {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 14px;
    padding: 1.1rem 1.25rem;
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04);
}

.card__label {
    font-size: 0.72rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: #8a95a8;
}

.card__value {
    font-family: Manrope, sans-serif;
    font-size: 2rem;
    font-weight: 800;
    line-height: 1;
    color: #1a2a6c;
    font-variant-numeric: tabular-nums;
}

.card--total .card__value {
    color: #1a2a6c;
}

.card--ok .card__value {
    color: #1e7e34;
}

.card--ko .card__value {
    color: #b03030;
}

.card--ind .card__value {
    color: #9a6409;
}

.card--retard .card__value {
    color: #d64545;
}

.card--vide .card__value {
    color: #8a95a8;
}

/* Panels */
.panel {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 14px;
    overflow: hidden;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04), 0 8px 24px rgba(26, 42, 108, .06);
}

.panel__head {
    display: flex;
    align-items: center;
    gap: 0.65rem;
    padding: 0.85rem 1.25rem;
    background: linear-gradient(140deg, #f7f9fd, #fff);
    border-bottom: 1px solid #e6ebf3;
}

.panel__icon {
    display: grid;
    place-items: center;
    width: 30px;
    height: 30px;
    border-radius: 9px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.9rem;
}

.panel__icon--danger {
    background: #fde9e9;
    color: #b03030;
}

.panel__title {
    margin: 0;
    flex: 1;
    font-family: Manrope, sans-serif;
    font-size: 0.92rem;
    font-weight: 700;
    color: #1a2a6c;
}

.panel__count {
    padding: 0.15rem 0.55rem;
    border-radius: 20px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.7rem;
    font-weight: 700;
}

/* Liste retard */
.retard-list {
    list-style: none;
    margin: 0;
    padding: 0;
    max-height: 320px;
    overflow-y: auto;
}

.retard-item {
    padding: 0.75rem 1.25rem;
    border-bottom: 1px solid #f5f7fa;
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
    cursor: pointer;
    transition: background .12s;
}

.retard-item:hover {
    background: #f8fafc;
}

.retard-item:last-child {
    border-bottom: none;
}

.retard-item__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 0.5rem;
}

.retard-item__code {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.72rem;
    font-weight: 600;
    color: #1a2a6c;
    background: #eef1f5;
    padding: 0.1rem 0.4rem;
    border-radius: 3px;
}

.retard-item__designation {
    font-size: 0.85rem;
    font-weight: 600;
    color: #15233c;
}

.retard-item__meta {
    display: flex;
    justify-content: space-between;
    font-size: 0.75rem;
    color: #8a95a8;
}

.retard-badge {
    background: #fde9e9;
    color: #b03030;
    padding: 0.15rem 0.5rem;
    border-radius: 20px;
    font-size: 0.65rem;
    font-weight: 700;
}

.retard-badge--vide {
    background: #f0f0f0;
    color: #6b7280;
}

/* Tableau */
.table-wrap {
    overflow-x: auto;
}

.indic-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.84rem;
}

.indic-table thead th {
    padding: 0.7rem 1rem;
    text-align: left;
    font-size: 0.7rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: #8a95a8;
    background: #f8fafc;
    border-bottom: 1px solid #e6ebf3;
    white-space: nowrap;
}

.indic-table tbody td {
    padding: 0.75rem 1rem;
    border-bottom: 1px solid #f5f7fa;
    color: #15233c;
    vertical-align: middle;
}

.indic-row {
    cursor: pointer;
    transition: background .12s;
}

.indic-row:hover {
    background: #f8fafc;
}

.indic-row:last-child td {
    border-bottom: none;
}

.col-code code {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.78rem;
    font-weight: 600;
    color: #1a2a6c;
    background: #eef1f5;
    padding: 0.15rem 0.45rem;
    border-radius: 3px;
}

.col-designation {
    max-width: 280px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.col-num {
    text-align: right;
    font-variant-numeric: tabular-nums;
}

.col-val {
    font-weight: 700;
    color: #1a2a6c;
}

.text-ok {
    color: #1e7e34;
    font-weight: 600;
}

.text-ko {
    color: #b03030;
    font-weight: 600;
}

.text-muted {
    color: #8a95a8;
}

/* Badges type */
.badge-type {
    display: inline-block;
    font-size: 0.7rem;
    font-weight: 700;
    padding: 0.2rem 0.6rem;
    border-radius: 20px;
    letter-spacing: 0.04em;
}

.badge-type--kpi {
    background: #e4f0fb;
    color: #135d95;
}

.badge-type--kri {
    background: #fdf3e2;
    color: #9a6409;
}

.badge-type--autre {
    background: #eef1f5;
    color: #5b6b7d;
}

/* Badges statut */
.badge-statut {
    display: inline-block;
    font-size: 0.7rem;
    font-weight: 700;
    padding: 0.25rem 0.65rem;
    border-radius: 20px;
}

.badge-statut--ok {
    background: #e6f4ea;
    color: #1e7e34;
}

.badge-statut--ko {
    background: #fde9e9;
    color: #b03030;
}

.badge-statut--ind {
    background: #fdf3e2;
    color: #9a6409;
}

.badge-statut--vide {
    background: #eef1f5;
    color: #6b7280;
}

/* Vide */
.empty {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    padding: 2rem 1.25rem;
    color: #8a95a8;
    font-size: 0.85rem;
}

.empty i {
    font-size: 1.2rem;
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
    height: 120px;
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
    .ind-suivi-page {
        padding-left: 24px;
        padding-right: 24px;
    }
}

@media (max-width: 767.98px) {
    .ind-suivi-page {
        padding: 24px 16px 20px;
    }

    .cards {
        grid-template-columns: repeat(2, 1fr);
    }

    .card__value {
        font-size: 1.6rem;
    }

    .indic-table thead th:nth-child(4),
    .indic-table tbody td:nth-child(4),
    .indic-table thead th:nth-child(6),
    .indic-table tbody td:nth-child(6) {
        display: none;
    }
}
</style>
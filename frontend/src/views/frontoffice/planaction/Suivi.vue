<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BaseButton } from '@/components/base'
import { getSuiviGlobalPlanAction } from '@/services/planaction'
import type { SuiviGlobalPlanAction } from '@/types/planaction'

const router = useRouter()
const suivi = ref<SuiviGlobalPlanAction | null>(null)
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
    const res = await getSuiviGlobalPlanAction()
    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    suivi.value = res.data
}

function voirPlan(id: number): void {
    router.push({ name: 'plan-action-detail', params: { id } })
}

function versListe(statut?: string): void {
    router.push({ name: 'plans-action', query: statut ? { statut } : {} })
}

function formaterDate(iso: string | null | undefined): string {
    if (!iso) return '—'
    const d = new Date(iso)
    return Number.isNaN(d.getTime()) ? iso : d.toLocaleDateString('fr-FR')
}

function classeOrigine(type: string): string {
    const t = type.toUpperCase()
    if (t === 'INCIDENT') return 'orig-tag--incident'
    if (t === 'RISQUE') return 'orig-tag--risque'
    return 'orig-tag--autre'
}

onMounted(charger)
</script>

<template>
    <div class="suivi-page">
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>Suivi des plans d'action</h1>
                <span class="title-divider"></span>
                <p class="subheading">
                    Vue d'ensemble du portefeuille de plans d'action du périmètre
                </p>
            </div>
            <div class="welcome-row__right">
                <BaseButton variant="secondary" icon="bi bi-arrow-clockwise" @click="charger">
                    Actualiser
                </BaseButton>
                <BaseButton variant="primary" icon="bi bi-list-check" @click="versListe()">
                    Voir tous les plans
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

        <template v-else-if="suivi">

            <!-- Cards de synthèse -->
            <section class="cards">
                <div class="card card--total" @click="versListe()">
                    <span class="card__label">Total plans</span>
                    <span class="card__value">{{ suivi.resume.total }}</span>
                </div>
                <div class="card card--non-commence" @click="versListe('NON_COMMENCEE')">
                    <span class="card__label">Non commencés</span>
                    <span class="card__value">{{ suivi.resume.nonCommences }}</span>
                </div>
                <div class="card card--en-cours" @click="versListe('EN_COURS')">
                    <span class="card__label">En cours</span>
                    <span class="card__value">{{ suivi.resume.enCours }}</span>
                </div>
                <div class="card card--en-retard" @click="versListe('EN_COURS')">
                    <span class="card__label">En retard</span>
                    <span class="card__value">{{ suivi.resume.enRetard }}</span>
                </div>
                <div class="card card--termine" @click="versListe('TERMINEE')">
                    <span class="card__label">Terminés</span>
                    <span class="card__value">{{ suivi.resume.termines }}</span>
                </div>
                <div class="card card--avancement">
                    <span class="card__label">Avancement moyen</span>
                    <span class="card__value">{{ suivi.resume.avancementMoyen.toFixed(0) }} %</span>
                    <div class="card__progress">
                        <div class="card__progress-bar" :style="{ width: suivi.resume.avancementMoyen + '%' }" />
                    </div>
                </div>
            </section>

            <!-- Grille 2 colonnes -->
            <div class="grid-2">

                <!-- En retard -->
                <section class="panel">
                    <header class="panel__head">
                        <span class="panel__icon panel__icon--danger">
                            <i class="bi bi-exclamation-triangle"></i>
                        </span>
                        <h2 class="panel__title">Plans en retard</h2>
                        <span class="panel__count">{{ suivi.enRetard.length }}</span>
                    </header>
                    <div v-if="suivi.enRetard.length === 0" class="empty">
                        <i class="bi bi-check-circle"></i>
                        Aucun plan en retard.
                    </div>
                    <ul v-else class="liste">
                        <li v-for="p in suivi.enRetard" :key="p.id" class="liste-item liste-item--clickable"
                            @click="voirPlan(p.id)">
                            <div class="liste-item__head">
                                <code class="liste-item__code">{{ p.code }}</code>
                                <span class="retard-badge">
                                    {{ p.joursDeRetard }} j de retard
                                </span>
                            </div>
                            <span class="liste-item__designation">{{ p.designation }}</span>
                            <div class="liste-item__meta">
                                <span>{{ p.responsableLibelle ?? '—' }}</span>
                                <span>{{ p.avancement.toFixed(0) }} %</span>
                            </div>
                        </li>
                    </ul>
                </section>

                <!-- Échéances prochaines -->
                <section class="panel">
                    <header class="panel__head">
                        <span class="panel__icon panel__icon--warning">
                            <i class="bi bi-calendar-event"></i>
                        </span>
                        <h2 class="panel__title">Échéances prochaines</h2>
                        <span class="panel__count">{{ suivi.echeancesProchaines.length }}</span>
                    </header>
                    <div v-if="suivi.echeancesProchaines.length === 0" class="empty">
                        <i class="bi bi-calendar-check"></i>
                        Aucune échéance dans les 30 prochains jours.
                    </div>
                    <ul v-else class="liste">
                        <li v-for="p in suivi.echeancesProchaines" :key="p.id" class="liste-item liste-item--clickable"
                            @click="voirPlan(p.id)">
                            <div class="liste-item__head">
                                <code class="liste-item__code">{{ p.code }}</code>
                                <span class="echeance-badge">
                                    {{ p.joursRestants }} j
                                </span>
                            </div>
                            <span class="liste-item__designation">{{ p.designation }}</span>
                            <div class="liste-item__meta">
                                <span>{{ formaterDate(p.dateFinPrevue) }}</span>
                                <span>{{ p.avancement.toFixed(0) }} %</span>
                            </div>
                        </li>
                    </ul>
                </section>

                <!-- Répartition par type d'origine -->
                <section class="panel">
                    <header class="panel__head">
                        <span class="panel__icon">
                            <i class="bi bi-tag"></i>
                        </span>
                        <h2 class="panel__title">Par type d'origine</h2>
                    </header>
                    <div v-if="suivi.parTypeOrigine.length === 0" class="empty">
                        Aucune donnée.
                    </div>
                    <ul v-else class="bar-list">
                        <li v-for="t in suivi.parTypeOrigine" :key="t.typeOrigine" class="bar-item">
                            <div class="bar-item__head">
                                <span :class="['orig-tag', classeOrigine(t.typeOrigine)]">
                                    {{ t.typeOrigine }}
                                </span>
                                <span class="bar-item__count">{{ t.nombre }}</span>
                            </div>
                            <div class="bar-track">
                                <div class="bar-fill" :style="{ width: (t.nombre / suivi.resume.total * 100) + '%' }" />
                            </div>
                            <span class="bar-item__meta">
                                avancement moyen : {{ t.avancementMoyen.toFixed(0) }} %
                            </span>
                        </li>
                    </ul>
                </section>

                <!-- Répartition par responsable -->
                <section class="panel">
                    <header class="panel__head">
                        <span class="panel__icon">
                            <i class="bi bi-people"></i>
                        </span>
                        <h2 class="panel__title">Par responsable</h2>
                        <span class="panel__sub">Top {{ suivi.parResponsable.length }}</span>
                    </header>
                    <div v-if="suivi.parResponsable.length === 0" class="empty">
                        Aucun responsable.
                    </div>
                    <table v-else class="resp-table">
                        <thead>
                            <tr>
                                <th>Responsable</th>
                                <th>Plans</th>
                                <th>En retard</th>
                                <th>Avanc.</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="r in suivi.parResponsable" :key="r.idUtilisateur">
                                <td>{{ r.libelle }}</td>
                                <td class="col-num">{{ r.total }}</td>
                                <td class="col-num">
                                    <span v-if="r.enRetard > 0" class="text-danger">
                                        {{ r.enRetard }}
                                    </span>
                                    <span v-else class="text-muted">0</span>
                                </td>
                                <td class="col-num">{{ r.avancementMoyen.toFixed(0) }} %</td>
                            </tr>
                        </tbody>
                    </table>
                </section>

            </div>
        </template>
    </div>
</template>

<style scoped>
.suivi-page {
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
    cursor: pointer;
    transition: transform .12s, box-shadow .12s;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04);
}

.card:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(26, 42, 108, .08);
}

.card--avancement {
    cursor: default;
}

.card--avancement:hover {
    transform: none;
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

.card--non-commence .card__value {
    color: #5b6b7d;
}

.card--en-cours .card__value {
    color: #135d95;
}

.card--en-retard .card__value {
    color: #b03030;
}

.card--termine .card__value {
    color: #1e7e34;
}

.card__progress {
    height: 6px;
    background: #eef1f5;
    border-radius: 3px;
    overflow: hidden;
    margin-top: 0.35rem;
}

.card__progress-bar {
    height: 100%;
    background: linear-gradient(90deg, #4a6cf7, #6b8dff);
    border-radius: 3px;
}

/* Grid */
.grid-2 {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1rem;
}

.panel {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 14px;
    overflow: hidden;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04), 0 8px 24px rgba(26, 42, 108, .06);
    display: flex;
    flex-direction: column;
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

.panel__icon--warning {
    background: #fdf3e2;
    color: #9a6409;
}

.panel__title {
    margin: 0;
    flex: 1;
    font-family: Manrope, sans-serif;
    font-size: 0.88rem;
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
    font-variant-numeric: tabular-nums;
}

.panel__sub {
    font-size: 0.7rem;
    color: #8a95a8;
}

/* Liste */
.liste {
    list-style: none;
    margin: 0;
    padding: 0;
    max-height: 360px;
    overflow-y: auto;
}

.liste-item {
    padding: 0.75rem 1.25rem;
    border-bottom: 1px solid #f5f7fa;
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
}

.liste-item:last-child {
    border-bottom: none;
}

.liste-item--clickable {
    cursor: pointer;
    transition: background .12s;
}

.liste-item--clickable:hover {
    background: #f8fafc;
}

.liste-item__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 0.5rem;
}

.liste-item__code {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.72rem;
    font-weight: 600;
    color: #1a2a6c;
    background: #eef1f5;
    padding: 0.1rem 0.4rem;
    border-radius: 3px;
}

.liste-item__designation {
    font-size: 0.85rem;
    font-weight: 600;
    color: #15233c;
}

.liste-item__meta {
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

.echeance-badge {
    background: #fdf3e2;
    color: #9a6409;
    padding: 0.15rem 0.5rem;
    border-radius: 20px;
    font-size: 0.65rem;
    font-weight: 700;
}

/* Bar list */
.bar-list {
    list-style: none;
    margin: 0;
    padding: 1rem 1.25rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.bar-item {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
}

.bar-item__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.bar-item__count {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.85rem;
    font-weight: 700;
    color: #1a2a6c;
}

.bar-track {
    height: 8px;
    background: #eef1f5;
    border-radius: 4px;
    overflow: hidden;
}

.bar-fill {
    height: 100%;
    background: linear-gradient(90deg, #4a6cf7, #6b8dff);
    border-radius: 4px;
    transition: width .3s ease;
}

.bar-item__meta {
    font-size: 0.72rem;
    color: #8a95a8;
}

/* Orig tags */
.orig-tag {
    display: inline-block;
    padding: 0.15rem 0.55rem;
    border-radius: 20px;
    font-size: 0.65rem;
    font-weight: 800;
    letter-spacing: 0.04em;
    text-transform: uppercase;
}

.orig-tag--incident {
    background: #fde9e9;
    color: #b03030;
}

.orig-tag--risque {
    background: #fdf3e2;
    color: #9a6409;
}

.orig-tag--autre {
    background: #eef1f5;
    color: #5b6b7d;
}

/* Table responsables */
.resp-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.82rem;
}

.resp-table th {
    text-align: left;
    padding: 0.6rem 1.25rem 0.5rem;
    font-size: 0.68rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: #8a95a8;
    border-bottom: 1px solid #eef1f5;
}

.resp-table td {
    padding: 0.6rem 1.25rem;
    border-bottom: 1px solid #f5f7fa;
    color: #15233c;
}

.resp-table tr:last-child td {
    border-bottom: none;
}

.col-num {
    text-align: right;
    font-variant-numeric: tabular-nums;
}

/* Empty */
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

.text-danger {
    color: #b03030;
    font-weight: 700;
}

.text-muted {
    color: #c3cad5;
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
    .suivi-page {
        padding-left: 24px;
        padding-right: 24px;
    }

    .grid-2 {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 767.98px) {
    .suivi-page {
        padding: 24px 16px 20px;
    }

    .cards {
        grid-template-columns: repeat(2, 1fr);
    }

    .card__value {
        font-size: 1.6rem;
    }
}
</style>
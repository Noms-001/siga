<template>
    <div class="suivi-membres">
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>
                    Suivi des membres
                    <span class="wave" aria-hidden="true">✳</span>
                </h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    Participation et avancement des activités affectées et plans d'actions de votre périmètre.
                </p>
            </div>

            <div class="welcome-row__right">
                <span class="scope-pill">{{ scopeLabel }}</span>
                <BaseButton variant="primary" :loading="loading" :disabled="loading" @click="charger">
                    <i class="bi bi-arrow-clockwise"></i>
                    Actualiser
                </BaseButton>
            </div>
        </header>

        <!-- Filtres -->
        <div class="filtres section mb-4" noPadding>
            <header class="filtres__header">
                <button type="button" class="filtres__toggle" :aria-expanded="filtresOuverts"
                    @click="filtresOuverts = !filtresOuverts">
                    <span class="filtres__header-icon"><i class="bi bi-sliders"></i></span>
                    <span class="filtres__header-text">
                        <span class="filtres__header-title">Filtres</span>
                        <span class="filtres__header-sub">
                            {{ filtresOuverts ? 'Cliquez pour replier' : 'Cliquez pour déplier' }}
                        </span>
                    </span>
                    <i class="bi bi-chevron-down filtres__chev" :class="{ 'filtres__chev--open': filtresOuverts }"></i>
                </button>
            </header>

            <div class="filtres__collapse" :class="{ 'filtres__collapse--open': filtresOuverts }">
                <div class="filtres__collapse-inner">
                    <div class="filtres__body">
                        <!-- LIGNE 1 : autocomplete membre (BaseSelect) + service -->
                        <div class="filtres__section">
                            <span class="filtres__section-label">Recherche et périmètre</span>
                            <div class="filtres__row">
                                <div class="filtres__col">
                                    <BaseSelect v-model="selectedMemberId" label="Rechercher un membre"
                                        placeholder="Tapez un nom ou prénom…" :options="memberOptions"
                                        option-label="nomComplet" option-value="id" autocomplete clearable
                                        icon="bi bi-search" />
                                </div>

                                <div v-if="data?.filtreServiceVisible" class="filtres__col">
                                    <BaseSelect v-model="selectedService" label="Service" :options="serviceOptions"
                                        option-label="nom" option-value="id" placeholder="Tous les services"
                                        clearable />
                                </div>
                            </div>
                        </div>

                        <!-- LIGNE 2 : période (dates en bas) -->
                        <div class="filtres__section">
                            <span class="filtres__section-label">Période</span>
                            <div class="filtres__section-grid filtres__section-grid--4">
                                <div class="filtres__col">
                                    <BaseSelect v-model="periodMode" label="Mode" :options="periodOptions"
                                        option-label="label" option-value="value" />
                                </div>
                                <div v-if="periodMode !== 'custom'" class="filtres__col">
                                    <BaseSelect v-model="selectedYear" label="Année" :options="yearOptions"
                                        option-label="label" option-value="value" />
                                </div>
                                <div v-if="periodMode === 'quarter'" class="filtres__col">
                                    <BaseSelect v-model="selectedQuarter" label="Trimestre" :options="quarterOptions"
                                        option-label="label" option-value="value" />
                                </div>
                                <div v-if="periodMode === 'custom'" class="filtres__col">
                                    <BaseInput v-model="customStart" label="Du" type="date" />
                                </div>
                                <div v-if="periodMode === 'custom'" class="filtres__col">
                                    <BaseInput v-model="customEnd" label="Au" type="date" />
                                </div>
                            </div>
                        </div>
                    </div>
                    <footer class="filtres__footer">
                        <span v-if="data" class="filtres__hint">
                            <i class="bi bi-calendar-range"></i>
                            Du {{ formatDate(data.dateDebut) }} au {{ formatDate(data.dateFin) }}
                        </span>
                        <div class="filtres__footer-actions">
                            <button type="button" class="filtres__reset" @click="reinitialiser">
                                <i class="bi bi-arrow-counterclockwise"></i>
                                Réinitialiser
                            </button>
                            <button type="button" class="filtres__close" @click="filtresOuverts = false">
                                <i class="bi bi-chevron-up"></i>
                                Replier
                            </button>
                        </div>
                    </footer>
                </div>
            </div>
        </div>

        <div v-if="loading && !data" class="loading-state" role="status">
            <span class="spinner"></span>Chargement des données membres…
        </div>

        <template v-if="data">
            <div v-if="loading" class="refresh-line" role="status">Actualisation…</div>

            <!-- KPI cards -->
            <div class="kpi-grid">
                <article v-for="kpi in kpis" :key="kpi.label" class="stat-card" :class="`stat-card--${kpi.color}`">
                    <header class="stat-card__head">
                        <span class="stat-card__label">{{ kpi.label }}</span>
                        <span class="stat-card__icon-wrap" aria-hidden="true">
                            <i class="stat-card__icon" :class="kpi.icon"></i>
                        </span>
                    </header>
                    <div class="stat-card__value">{{ kpi.value }}</div>
                    <footer class="stat-card__hint">{{ kpi.hint }}</footer>
                </article>
            </div>

            <!-- Charts grid -->
            <section class="charts-grid" aria-label="Comparaison des membres">
                <article class="panel chart-panel">
                    <div class="panel-heading">
                        <div>
                            <p class="eyebrow">PARTICIPATION</p>
                            <h2>Éléments par membre</h2>
                        </div>
                        <span class="table-hint">{{ chartMembers.length }} membres affichés</span>
                    </div>
                    <div v-if="!filteredMembers.length" class="empty-chart">
                        Aucun membre ne correspond aux filtres.
                    </div>
                    <div v-else class="member-chart">
                        <div v-for="member in chartMembers" :key="member.idUtilisateur" class="chart-member">
                            <strong class="chart-member__name" :title="member.nomComplet">
                                {{ member.nomComplet }}
                            </strong>
                            <div class="chart-counts">
                                <span class="chart-count">
                                    <i class="dot dot-pta"></i>PTA
                                    <b>{{ member.totalPta }}</b>
                                </span>
                                <span class="chart-count">
                                    <i class="dot dot-non-pta"></i>Courantes
                                    <b>{{ member.totalNonPta }}</b>
                                </span>
                                <span class="chart-count">
                                    <i class="dot dot-plans"></i>Plans
                                    <b>{{ member.totalPlansActions }}</b>
                                </span>
                            </div>
                        </div>
                    </div>
                    <div class="chart-legend">
                        <span><i class="legend-pta"></i>Activités PTA</span>
                        <span><i class="legend-non-pta"></i>Activités courantes</span>
                        <span><i class="legend-plans"></i>Plans d'actions</span>
                    </div>
                </article>

                <article class="panel chart-panel">
                    <div class="panel-heading">
                        <div>
                            <p class="eyebrow">AVANCEMENT</p>
                            <h2>Progression par membre</h2>
                        </div>
                        <span class="table-hint">{{ chartMembers.length }} membres affichés</span>
                    </div>
                    <div v-if="!filteredMembers.length" class="empty-chart">
                        Aucun membre ne correspond aux filtres.
                    </div>
                    <div v-else class="member-chart">
                        <div v-for="member in chartMembers" :key="member.idUtilisateur" class="chart-member">
                            <strong class="chart-member__name" :title="member.nomComplet">
                                {{ member.nomComplet }}
                            </strong>
                            <div class="chart-bars">
                                <div class="chart-bar-row">
                                    <span>PTA</span>
                                    <div class="bar-track">
                                        <i class="fill-pta" :style="{ width: percentWidth(member.avancementPta) }"></i>
                                    </div>
                                    <b>{{ percent(member.avancementPta) }}</b>
                                </div>
                                <div class="chart-bar-row">
                                    <span>Courantes</span>
                                    <div class="bar-track">
                                        <i class="fill-non-pta"
                                            :style="{ width: percentWidth(member.avancementNonPta) }"></i>
                                    </div>
                                    <b>{{ percent(member.avancementNonPta) }}</b>
                                </div>
                                <div class="chart-bar-row">
                                    <span>Plans</span>
                                    <div class="bar-track">
                                        <i class="fill-plans"
                                            :style="{ width: percentWidth(member.avancementPlansActions) }"></i>
                                    </div>
                                    <b>{{ percent(member.avancementPlansActions) }}</b>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="chart-legend">
                        <span><i class="legend-pta"></i>Avancement PTA</span>
                        <span><i class="legend-non-pta"></i>Avancement courantes</span>
                        <span><i class="legend-plans"></i>Avancement plans</span>
                    </div>
                </article>
            </section>

            <!-- Tableau récapitulatif -->
            <BaseCard noPadding class="table-panel-card">
                <div class="panel-heading table-heading">
                    <div>
                        <p class="eyebrow">DÉTAIL COLLECTIF</p>
                        <h2>Membres du périmètre</h2>
                        <p class="table-hint">
                            Les activités PTA et courantes sont dédupliquées par membre et activité.
                        </p>
                    </div>
                    <div class="sort-controls">
                        <label><span>Trier par</span>
                            <BaseSelect v-model="sortBy" :options="sortOptions" option-label="label"
                                option-value="value" size="sm" />
                        </label>
                        <BaseButton variant="secondary" size="sm"
                            :icon="sortDirection === 'asc' ? 'bi bi-sort-up' : 'bi bi-sort-down'"
                            :title="sortDirection === 'asc' ? 'Ordre croissant' : 'Ordre décroissant'"
                            @click="toggleSort">
                            {{ sortDirection === 'asc' ? 'Croissant' : 'Décroissant' }}
                        </BaseButton>
                    </div>
                </div>
                <p v-if="!filteredMembers.length" class="empty-table">
                    Aucun membre ne correspond aux filtres.
                </p>
                <BaseTable v-else :items="sortedMembers" :columns="columns" :page-size="10" :searchable="false"
                    :sortable="false" custom-class="members-table">
                    <template #cell-nomComplet="{ item }">
                        <div class="member-cell">
                            <strong>{{ item.nomComplet }}</strong>
                            <span v-if="item.elementsEnRetard" class="late-badge"
                                :title="`${item.elementsEnRetard} élément(s) en retard au total`">
                                <i class="bi bi-exclamation-circle-fill"></i>
                                {{ item.elementsEnRetard }}
                            </span>
                        </div>
                    </template>

                    <template #cell-activites="{ item }">
                        <div class="split-cell">
                            <div class="split-row">
                                <span class="split-label"><i class="dot dot-pta"></i>PTA</span>
                                <b class="split-count">{{ item.totalPta }}</b>
                                <span v-if="item.activitesPtaEnRetard > 0" class="late-mini"
                                    :title="`${item.activitesPtaEnRetard} PTA en retard`">
                                    <i class="bi bi-exclamation-circle-fill"></i>{{ item.activitesPtaEnRetard }}
                                </span>
                            </div>
                            <div class="split-row">
                                <span class="split-label"><i class="dot dot-non-pta"></i>Courantes</span>
                                <b class="split-count">{{ item.totalNonPta }}</b>
                                <span v-if="item.activitesNonPtaEnRetard > 0" class="late-mini"
                                    :title="`${item.activitesNonPtaEnRetard} courantes en retard`">
                                    <i class="bi bi-exclamation-circle-fill"></i>{{ item.activitesNonPtaEnRetard }}
                                </span>
                            </div>
                        </div>
                    </template>

                    <template #cell-avancementActivites="{ item }">
                        <div class="split-avancement">
                            <div v-if="item.avancementPta != null" class="mini-donut">
                                <svg viewBox="0 0 32 32" class="donut donut--sm" aria-hidden="true">
                                    <circle cx="16" cy="16" r="12" fill="none" stroke="#eef2f7" stroke-width="4" />
                                    <circle cx="16" cy="16" r="12" fill="none"
                                        :stroke="donutColor(item.avancementPta, 'activity')" stroke-width="4"
                                        :stroke-dasharray="`${clamp(item.avancementPta)} 100`" stroke-linecap="round"
                                        transform="rotate(-90 16 16)" pathLength="100" />
                                </svg>
                                <span class="mini-donut__label">PTA</span>
                                <b class="mini-donut__value">{{ percent(item.avancementPta) }}</b>
                            </div>

                            <div v-if="item.avancementNonPta != null" class="mini-donut">
                                <svg viewBox="0 0 32 32" class="donut donut--sm" aria-hidden="true">
                                    <circle cx="16" cy="16" r="12" fill="none" stroke="#eef2f7" stroke-width="4" />
                                    <circle cx="16" cy="16" r="12" fill="none"
                                        :stroke="donutColor(item.avancementNonPta, 'activity')" stroke-width="4"
                                        :stroke-dasharray="`${clamp(item.avancementNonPta)} 100`" stroke-linecap="round"
                                        transform="rotate(-90 16 16)" pathLength="100" />
                                </svg>
                                <span class="mini-donut__label">Cour.</span>
                                <b class="mini-donut__value">{{ percent(item.avancementNonPta) }}</b>
                            </div>

                            <span v-if="item.avancementPta == null && item.avancementNonPta == null"
                                class="no-value">—</span>
                        </div>
                    </template>

                    <template #cell-plans="{ item }">
                        <div class="split-row">
                            <b class="split-count">{{ item.totalPlansActions }}</b>
                            <span v-if="item.plansActionsEnRetard > 0" class="late-mini"
                                :title="`${item.plansActionsEnRetard} plan(s) en retard`">
                                <i class="bi bi-exclamation-circle-fill"></i>{{ item.plansActionsEnRetard }}
                            </span>
                        </div>
                    </template>

                    <template #cell-avancementPlansActions="{ item }">
                        <div v-if="item.avancementPlansActions != null" class="donut-cell">
                            <svg viewBox="0 0 40 40" class="donut" aria-hidden="true">
                                <circle cx="20" cy="20" r="15" fill="none" stroke="#eef2f7" stroke-width="5" />
                                <circle cx="20" cy="20" r="15" fill="none"
                                    :stroke="donutColor(item.avancementPlansActions, 'plan')" stroke-width="5"
                                    :stroke-dasharray="`${clamp(item.avancementPlansActions)} 100`"
                                    stroke-linecap="round" transform="rotate(-90 20 20)" pathLength="100" />
                            </svg>
                            <span class="donut-value">{{ percent(item.avancementPlansActions) }}</span>
                        </div>
                        <span v-else class="no-value">—</span>
                    </template>
                </BaseTable>
            </BaseCard>
        </template>
    </div>
</template>

<script setup lang="ts">
import { useToastMessage } from '@/composables/useToast'
import { computed, ref, watch } from 'vue'
import { BaseButton, BaseCard, BaseInput, BaseSelect, BaseTable } from '@/components/base'
import { getMembreSuiviMembres } from '@/services/membreSuivi'
import type { MembreSuiviFiltres, MembreSuiviMembre, MembreSuiviMembres } from '@/types/membreSuivi'

const currentYear = new Date().getFullYear()
const years = Array.from({ length: 8 }, (_, index) => currentYear - index)
const periodOptions = [
    { label: 'Année', value: 'year' },
    { label: 'Trimestre', value: 'quarter' },
    { label: 'Période personnalisée', value: 'custom' },
]
const yearOptions = years.map((year) => ({ label: String(year), value: year }))
const quarterOptions = [1, 2, 3, 4].map((value) => ({ label: `T${value}`, value }))
const sortOptions = [
    { label: 'Nom', value: 'nomComplet' },
    { label: 'Nombre d’activités', value: 'totalActivites' },
    { label: 'Nombre de plans d’actions', value: 'totalPlansActions' },
    { label: 'Avancement global', value: 'avancementGlobal' },
]

const columns = [
    { key: 'nomComplet', label: 'Membre' },
    { key: 'service', label: 'Service' },
    { key: 'activites', label: 'Activités' },
    { key: 'avancementActivites', label: 'Avancement activités' },
    { key: 'plans', label: 'Plans d’actions' },
    { key: 'avancementPlansActions', label: 'Avancement plans' },
]

const dateDuJour = computed(() =>
    new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    }).format(new Date()).toUpperCase()
)

const periodMode = ref<'year' | 'quarter' | 'custom'>('year')
const selectedYear = ref(currentYear)
const selectedQuarter = ref(Math.floor(new Date().getMonth() / 3) + 1)
const customStart = ref(`${currentYear}-01-01`)
const customEnd = ref(new Date().toISOString().slice(0, 10))
const selectedService = ref<number | undefined>()
// ✅ undefined au lieu de null pour compatibilité avec BaseSelect
const selectedMemberId = ref<number | undefined>(undefined)
const sortBy = ref('nomComplet')
const sortDirection = ref<'asc' | 'desc'>('asc')
const filtresOuverts = ref(false)
const data = ref<MembreSuiviMembres | null>(null)
const loading = ref(false)
const errorMessage = useToastMessage('error')
let requestId = 0

// ✅ Options = membres réels uniquement (pas de sentinelle)
const memberOptions = computed<Record<string, unknown>[]>(() =>
    (data.value?.membres ?? []).map((m) => ({
        id: m.idUtilisateur,
        nomComplet: m.nomComplet,
    }))
)

const filters = computed<MembreSuiviFiltres>(() => ({
    ...(periodMode.value === 'custom'
        ? { dateDebut: customStart.value || undefined, dateFin: customEnd.value || undefined }
        : {
            annee: selectedYear.value,
            ...(periodMode.value === 'quarter' ? { trimestre: selectedQuarter.value } : {}),
        }),
    idService: selectedService.value,
}))
const scopeLabel = computed(() => data.value?.filtreServiceVisible
    ? 'Périmètre départemental'
    : 'Périmètre de votre service')
const serviceOptions = computed(() => data.value?.servicesDisponibles ?? [])
const filteredMembers = computed(() => {
    if (selectedMemberId.value != null) {
        return (data.value?.membres ?? [])
            .filter((m) => m.idUtilisateur === selectedMemberId.value)
    }
    return data.value?.membres ?? []
})
const sortedMembers = computed(() => [...filteredMembers.value].sort((a, b) => {
    const left = a[sortBy.value as keyof MembreSuiviMembre]
    const right = b[sortBy.value as keyof MembreSuiviMembre]
    let comparison = 0
    if (typeof left === 'string' && typeof right === 'string') comparison = left.localeCompare(right, 'fr')
    else comparison = Number(left ?? -1) - Number(right ?? -1)
    return comparison * (sortDirection.value === 'asc' ? 1 : -1)
}))
const chartMembers = computed(() => [...filteredMembers.value]
    .sort((a, b) => {
        const totalA = a.totalPta + a.totalNonPta + a.totalPlansActions
        const totalB = b.totalPta + b.totalNonPta + b.totalPlansActions
        return totalB - totalA || a.nomComplet.localeCompare(b.nomComplet, 'fr')
    })
    .slice(0, 15))
const kpis = computed(() => [
    {
        label: 'Membres suivis',
        value: formatNumber(filteredMembers.value.length),
        hint: 'Membres visibles dans ce périmètre',
        icon: 'bi bi-people',
        color: 'primary',
    },
    {
        label: 'Avec activités',
        value: formatNumber(filteredMembers.value.filter((m) => (m.totalPta + m.totalNonPta) > 0).length),
        hint: 'PTA + courantes affectées',
        icon: 'bi bi-list-check',
        color: 'info',
    },
    {
        label: 'Avec plans d’actions',
        value: formatNumber(filteredMembers.value.filter((m) => m.totalPlansActions > 0).length),
        hint: 'Plans affectés sans doublons',
        icon: 'bi bi-clipboard-data',
        color: 'warning',
    },
    {
        label: 'Avec retards',
        value: formatNumber(filteredMembers.value.filter((m) => m.elementsEnRetard > 0).length),
        hint: 'Au moins un élément en retard',
        icon: 'bi bi-clock-history',
        color: 'danger',
    },
])

async function charger(): Promise<void> {
    if (periodMode.value === 'custom' && (!customStart.value || !customEnd.value)) return
    const current = ++requestId
    loading.value = true
    errorMessage.value = ''
    const response = await getMembreSuiviMembres(filters.value)
    if (current !== requestId) return
    loading.value = false
    if (!response.success) {
        errorMessage.value = response.error || 'Impossible de charger le suivi des membres.'
        return
    }
    data.value = response.data
    // Sélection obsolète si le membre n'existe plus dans la nouvelle réponse
    if (selectedMemberId.value != null
        && !response.data.membres.some((m) => m.idUtilisateur === selectedMemberId.value)) {
        selectedMemberId.value = undefined
    }
}

function reinitialiser(): void {
    periodMode.value = 'year'
    selectedYear.value = currentYear
    selectedQuarter.value = Math.floor(new Date().getMonth() / 3) + 1
    customStart.value = `${currentYear}-01-01`
    customEnd.value = new Date().toISOString().slice(0, 10)
    selectedService.value = undefined
    selectedMemberId.value = undefined
    sortBy.value = 'nomComplet'
    sortDirection.value = 'asc'
}

watch([periodMode, selectedYear, selectedQuarter, customStart, customEnd, selectedService],
    () => { void charger() }, { immediate: true })

function toggleSort(): void {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
}
function formatNumber(value: number): string {
    return new Intl.NumberFormat('fr-FR').format(value)
}
function percent(value: number | null): string {
    return value == null
        ? '—'
        : `${new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 }).format(value)} %`
}
function clamp(value: number): number {
    return Math.max(0, Math.min(100, Number.isFinite(value) ? value : 0))
}
function percentWidth(value: number | null): string {
    return value == null ? '0%' : `${clamp(value)}%`
}
function donutColor(value: number | null, kind: 'activity' | 'plan'): string {
    if (value == null) return '#eef2f7'
    if (value >= 100) return '#55d6ad'
    return kind === 'activity' ? '#4a6cf7' : '#e5a944'
}
function formatDate(value: string): string {
    const date = new Date(`${value.slice(0, 10)}T00:00:00`)
    return new Intl.DateTimeFormat('fr-FR', {
        day: '2-digit', month: 'short', year: 'numeric',
    }).format(date)
}
</script>

<style scoped>
.suivi-membres {
    min-height: calc(100vh - 68px);
    padding: 34px 38px 24px;
    color: #15233c;
    font-family: "DM Sans", "Segoe UI", sans-serif;
    background:
        radial-gradient(circle at 12% 8%, rgba(26, 42, 108, .055) 0, transparent 38%),
        radial-gradient(circle at 92% 94%, rgba(212, 175, 55, .065) 0, transparent 36%),
        #f5f7fa;
}

@keyframes pulse {

    0%,
    100% {
        opacity: 1;
    }

    50% {
        opacity: .35;
    }
}

.wave {
    color: #d4af37;
    font-size: 1.4rem;
    line-height: 1;
}

.title-divider {
    display: block;
    width: 48px;
    height: 3px;
    margin: .6rem 0 .55rem;
    border-radius: 3px;
    background: linear-gradient(90deg, #1a2a6c, #d4af37);
}

.subheading {
    margin: 0;
    color: #6b7280;
    max-width: 60ch;
}

.scope-pill {
    white-space: nowrap;
    padding: .55rem .85rem;
    border: 1px solid #dfe7f1;
    border-radius: 999px;
    color: #365478;
    background: #f7f9fc;
    font-size: .78rem;
    font-weight: 700;
}

/* ============ FILTRES ============ */
.filtres {
    overflow: visible;
}

.filtres__header {
    display: flex;
    align-items: center;
    gap: .5rem;
    padding: .9rem 1.25rem;
    background: linear-gradient(140deg, #f7f9fd, #fff);
    border-bottom: 1px solid #e6ebf3;
}

.filtres__toggle {
    display: flex;
    align-items: center;
    gap: .75rem;
    flex: 1;
    min-width: 0;
    padding: .25rem;
    border: none;
    background: transparent;
    text-align: left;
    cursor: pointer;
    border-radius: 8px;
}

.filtres__header-icon {
    display: grid;
    place-items: center;
    width: 34px;
    height: 34px;
    border-radius: 9px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 1rem;
    flex: 0 0 34px;
}

.filtres__header-text {
    display: flex;
    flex-direction: column;
    min-width: 0;
    flex: 1;
}

.filtres__header-title {
    font-size: .95rem;
    font-weight: 700;
    color: #1a2a6c;
    line-height: 1.2;
}

.filtres__header-sub {
    font-size: .75rem;
    color: #6b7280;
    margin-top: 2px;
}

.filtres__chev {
    font-size: .9rem;
    color: #8a95a8;
    transition: transform .25s ease, color .15s ease;
}

.filtres__chev--open {
    transform: rotate(180deg);
    color: #4a6cf7;
}

.filtres__collapse {
    display: grid;
    grid-template-rows: 0fr;
    transition: grid-template-rows .3s ease;
}

.filtres__collapse--open {
    grid-template-rows: 1fr;
}

.filtres__collapse-inner {
    overflow: hidden;
    min-height: 0;
}

.filtres__body {
    display: grid;
    gap: 1.25rem;
    padding: 1.25rem;
}

.filtres__section {
    display: grid;
    gap: .6rem;
}

.filtres__section-label {
    font-size: .68rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: .08em;
    color: #8a95a8;
}

.filtres__row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
    gap: 1rem;
}

.filtres__section-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1rem;
}

.filtres__section-grid--4 {
    grid-template-columns: repeat(4, minmax(0, 1fr));
}

.filtres__col {
    position: relative;
    min-width: 0;
}

.filtres__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
    padding: .85rem 1.25rem;
    background: #f7f9fd;
    border-top: 1px solid #e6ebf3;
}

.filtres__hint {
    display: inline-flex;
    align-items: center;
    gap: .4rem;
    font-size: .75rem;
    color: #8a95a8;
}

.filtres__footer-actions {
    display: flex;
    align-items: center;
    gap: .5rem;
    margin-left: auto;
}

.filtres__reset,
.filtres__close {
    display: inline-flex;
    align-items: center;
    gap: .4rem;
    padding: .45rem .9rem;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    background: #fff;
    color: #1a2a6c;
    font-size: .8rem;
    font-weight: 600;
    cursor: pointer;
}

.filtres__reset:hover {
    border-color: #4a6cf7;
    color: #4a6cf7;
    background: #f3f6fc;
}

.filtres__close {
    background: linear-gradient(135deg, #1a2a6c 0%, #2a3f94 100%);
    color: #fff;
    border-color: transparent;
}

/* ============ KPI STAT-CARDS ============ */
.kpi-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: .9rem;
    margin: 1rem 0;
}

.stat-card {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: .5rem;
    overflow: hidden;
    min-height: 110px;
    padding: 1.15rem 1.2rem 1rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-top: 3px solid #c8d6fb;
    border-radius: 16px;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04), 0 8px 24px rgba(26, 42, 108, .06);
    transition: transform .2s ease, box-shadow .2s ease;
}

.stat-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 2px 4px rgba(26, 42, 108, .05), 0 14px 34px rgba(26, 42, 108, .10);
}

.stat-card__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: .5rem;
}

.stat-card__label {
    color: #7b8799;
    font-size: .72rem;
    font-weight: 700;
    letter-spacing: .05em;
    text-transform: uppercase;
    line-height: 1.4;
}

.stat-card__icon-wrap {
    display: grid;
    width: 30px;
    height: 30px;
    flex: 0 0 30px;
    place-items: center;
    border-radius: 9px;
    background: #eaf0ff;
    color: #3b63d9;
}

.stat-card__icon {
    font-size: 1.05rem;
    line-height: 1;
}

.stat-card__value {
    font-size: 1.75rem;
    font-weight: 700;
    color: #1a2a6c;
    line-height: 1;
}

.stat-card__hint {
    margin-top: auto;
    color: #7b8799;
    font-size: .72rem;
}

.stat-card--primary {
    border-top-color: #4a6cf7;
}

.stat-card--info {
    border-top-color: #72a8ed;
}

.stat-card--warning {
    border-top-color: #f4b91f;
}

.stat-card--danger {
    border-top-color: #ff8790;
}

.stat-card--info .stat-card__icon-wrap {
    background: #eaf0ff;
    color: #3b63d9;
}

.stat-card--warning .stat-card__icon-wrap {
    background: #fff6dd;
    color: #b8860b;
}

.stat-card--danger .stat-card__icon-wrap {
    background: #fff0f0;
    color: #d84e5b;
}

.stat-card--info .stat-card__value {
    color: #3b63d9;
}

.stat-card--warning .stat-card__value {
    color: #b8860b;
}

.stat-card--danger .stat-card__value {
    color: #d84e5b;
}

/* ============ CHARTS ============ */
.charts-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1rem;
    margin: 1rem 0;
}

.panel {
    border: 1px solid #e5eaf1;
    border-radius: 16px;
    background: #fff;
    box-shadow: 0 8px 26px #1d355708;
}

.chart-panel {
    padding: 1.2rem;
    min-height: 300px;
}

.panel-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    margin-bottom: 1rem;
}

.panel-heading h2 {
    margin: 0;
    font-size: 1.05rem;
    letter-spacing: -.02em;
    color: #1a2a6c;
}

.panel-heading .eyebrow {
    margin: 0 0 .4rem;
    color: #71809a;
    font-size: .68rem;
    font-weight: 800;
    letter-spacing: .13em;
}

.table-hint {
    margin: .45rem 0 0;
    color: #8490a2;
    font-size: .72rem;
}

.member-chart {
    display: grid;
    gap: .7rem;
    max-height: 430px;
    overflow: auto;
    padding-right: .3rem;
}

.chart-member {
    display: grid;
    grid-template-columns: minmax(115px, 30%) 1fr;
    align-items: center;
    gap: .8rem;
    padding: .65rem .7rem;
    border: 1px solid #edf1f6;
    border-radius: 10px;
}

.chart-member__name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: .76rem;
    color: #2d3b53;
}

.chart-counts {
    display: flex;
    gap: .5rem;
    flex-wrap: wrap;
}

.chart-count {
    display: inline-flex;
    align-items: center;
    gap: .4rem;
    padding: .35rem .7rem;
    border-radius: 8px;
    background: #f7f9fd;
    border: 1px solid #eef2f7;
    font-size: .72rem;
    color: #738096;
}

.chart-count b {
    color: #2d3b53;
    font-variant-numeric: tabular-nums;
    margin-left: .15rem;
    font-size: .78rem;
}

.dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    display: inline-block;
}

.dot-pta {
    background: #4a6cf7;
}

.dot-non-pta {
    background: #55d6ad;
}

.dot-plans {
    background: #e5a944;
}

.chart-bars {
    display: grid;
    gap: .45rem;
}

.chart-bar-row {
    display: grid;
    grid-template-columns: 92px minmax(40px, 1fr) 58px;
    align-items: center;
    gap: .5rem;
    color: #738096;
    font-size: .66rem;
}

.chart-bar-row b {
    text-align: right;
    color: #37445a;
    font-size: .7rem;
    font-variant-numeric: tabular-nums;
}

.bar-track {
    height: 9px;
    border-radius: 99px;
    background: #edf1f6;
    overflow: hidden;
}

.bar-track i {
    display: block;
    height: 100%;
    border-radius: inherit;
    transition: width .25s ease;
}

.fill-pta,
.legend-pta {
    background: #4a6cf7;
}

.fill-non-pta,
.legend-non-pta {
    background: #55d6ad;
}

.fill-plans,
.legend-plans {
    background: #e5a944;
}

.chart-legend {
    display: flex;
    justify-content: center;
    flex-wrap: wrap;
    gap: 1rem;
    margin-top: 1rem;
    color: #65738a;
    font-size: .7rem;
}

.chart-legend span {
    display: flex;
    align-items: center;
    gap: .4rem;
}

.chart-legend i {
    width: 9px;
    height: 9px;
    border-radius: 50%;
}

.empty-chart,
.empty-table {
    min-height: 180px;
    display: grid;
    place-items: center;
    padding: 1rem;
    text-align: center;
    color: #8b97a9;
    font-size: .83rem;
}

/* ============ TABLEAU ============ */
.table-panel-card {
    margin: 1rem 0 2rem;
}

.table-heading {
    padding: 1.1rem 1.2rem;
    align-items: flex-end;
    border-bottom: 1px solid #eef2f7;
}

.sort-controls {
    display: flex;
    align-items: flex-end;
    gap: .6rem;
}

.sort-controls label {
    display: grid;
    gap: .3rem;
    min-width: 190px;
}

.sort-controls label span {
    font-size: .72rem;
    font-weight: 700;
    color: #66758d;
}

.table-panel-card :deep(.base-table-container) {
    border-radius: 10px;
}

.table-panel-card :deep(.table) {
    min-width: 900px;
}

.member-cell {
    display: grid;
    gap: .35rem;
}

.member-cell strong {
    color: #24324a;
}

.late-badge {
    display: inline-flex;
    align-items: center;
    gap: .3rem;
    width: max-content;
    padding: .2rem .45rem;
    border-radius: 999px;
    background: #fff0ee;
    color: #b43e39;
    font-size: .63rem;
    font-weight: 700;
}

.split-cell {
    display: grid;
    gap: .3rem;
}

.split-row {
    display: inline-flex;
    align-items: center;
    gap: .4rem;
    font-size: .78rem;
    color: #40516b;
}

.split-label {
    display: inline-flex;
    align-items: center;
    gap: .3rem;
    color: #738096;
    font-size: .72rem;
    min-width: 78px;
}

.split-count {
    color: #24324a;
    font-variant-numeric: tabular-nums;
    min-width: 18px;
    text-align: right;
    font-size: .8rem;
}

.late-mini {
    display: inline-flex;
    align-items: center;
    gap: .2rem;
    padding: .1rem .35rem;
    border-radius: 999px;
    background: #fff0ee;
    color: #b43e39;
    font-size: .62rem;
    font-weight: 700;
}

.split-avancement {
    display: flex;
    align-items: center;
    gap: .9rem;
    flex-wrap: wrap;
}

.mini-donut {
    display: inline-flex;
    align-items: center;
    gap: .3rem;
}

.mini-donut__label {
    font-size: .65rem;
    color: #738096;
}

.mini-donut__value {
    font-size: .7rem;
    color: #34425a;
    font-variant-numeric: tabular-nums;
}

.donut-cell {
    display: inline-flex;
    align-items: center;
    gap: .45rem;
    min-width: 74px;
}

.donut {
    width: 32px;
    height: 32px;
    flex-shrink: 0;
}

.donut--sm {
    width: 26px;
    height: 26px;
}

.donut-value {
    font-size: .7rem;
    color: #34425a;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
}

.no-value {
    color: #8b97a9;
}

.table-panel-card :deep(.table-search) {
    display: none;
}

/* ============ ÉTATS ============ */
.loading-state {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: .75rem;
    min-height: 180px;
    color: #64748b;
}

.spinner {
    width: 20px;
    height: 20px;
    border: 2px solid #dbe3ee;
    border-top-color: #4778e8;
    border-radius: 50%;
    animation: spin .8s linear infinite;
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
}

.refresh-line {
    text-align: right;
    color: #738198;
    font-size: .75rem;
}

/* ============ RESPONSIVE ============ */
@media (max-width: 1050px) {
    .kpi-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .charts-grid {
        grid-template-columns: 1fr;
    }

    .filtres__section-grid,
    .filtres__section-grid--4 {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 680px) {
    .suivi-membres {
        padding: 1rem;
    }

    .welcome-row {
        flex-direction: column;
        align-items: flex-start;
    }

    .welcome-row__right {
        width: 100%;
        justify-content: space-between;
    }

    .kpi-grid {
        gap: .6rem;
    }

    .stat-card {
        min-height: 105px;
        padding: .8rem;
    }

    .stat-card__value {
        font-size: 1.45rem;
    }

    .chart-panel {
        padding: .9rem;
    }

    .panel-heading,
    .table-heading {
        align-items: flex-start;
        flex-direction: column;
    }

    .sort-controls {
        width: 100%;
    }

    .sort-controls label {
        flex: 1;
        min-width: 0;
    }

    .chart-member {
        grid-template-columns: 1fr;
        gap: .45rem;
    }

    .chart-bar-row {
        grid-template-columns: 83px minmax(30px, 1fr) 52px;
    }

    .filtres__footer {
        flex-direction: column;
        align-items: stretch;
    }

    .filtres__footer-actions {
        margin-left: 0;
    }

    .split-avancement {
        gap: .5rem;
    }
}

@media (max-width: 420px) {

    .filtres__section-grid,
    .filtres__section-grid--4,
    .filtres__row {
        grid-template-columns: 1fr;
    }

    .kpi-grid {
        grid-template-columns: 1fr 1fr;
    }
}
</style>
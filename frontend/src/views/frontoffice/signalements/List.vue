<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { BaseTable, BaseButton, BaseSelect } from '@/components/base'
import type { TableColumn } from '@/components/base/BaseTable/BaseTable.types'
import {
    getSignalementFiltres,
    listerSignalements,
} from '@/services/signalement'
import type { Signalement } from '@/types/backoffice/signalement'
import { getCurrentYear } from '@/utils/date'

const router = useRouter()

/* -------------------- état -------------------- */

const items = ref<Signalement[]>([])
const typesDisponibles = ref<string[]>([])
const loading = ref(false)
const errorMessage = ref('')

const type = ref<string>('all')
const planAction = ref<'all' | 'with' | 'without'>('all')
const annee = ref<number>(getCurrentYear())

/* -------------------- en-tête -------------------- */

const dateDuJour = computed(() =>
    new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric',
    })
        .format(new Date())
        .toUpperCase()
)

/* -------------------- options -------------------- */

const typeOptions = computed(() => [
    { value: 'all', label: 'Tous les types' },
    ...typesDisponibles.value.map(t => ({ value: t, label: t })),
])

const planActionOptions = [
    { value: 'all', label: 'Tous' },
    { value: 'with', label: "Avec plan d'action" },
    { value: 'without', label: "Sans plan d'action" },
]

const anneesDisponibles = computed(() => {
    const base = getCurrentYear()
    return [base - 2, base - 1, base, base + 1, base + 2]
        .map(y => ({ value: y, label: String(y) }))
})

/* -------------------- filtres actifs -------------------- */

const aDesFiltres = computed(() =>
    type.value !== 'all'
    || planAction.value !== 'all'
    || annee.value !== getCurrentYear()
)

function reinitialiserFiltres(): void {
    type.value = 'all'
    planAction.value = 'all'
    annee.value = getCurrentYear()
}

/* -------------------- statistiques -------------------- */

const nbTotal = computed(() => items.value.length)

const nbIncidents = computed(() =>
    items.value.filter(s => (s.typeOrigine ?? '').toUpperCase() === 'INCIDENT').length
)

const nbRisques = computed(() =>
    items.value.filter(s => (s.typeOrigine ?? '').toUpperCase() === 'RISQUE').length
)

const nbAutres = computed(() =>
    nbTotal.value - nbIncidents.value - nbRisques.value
)

/* -------------------- tableau -------------------- */

const columns: TableColumn<Signalement>[] = [
    { key: 'code', label: 'Code' },
    { key: 'designation', label: 'Désignation' },
    { key: 'typeOrigine', label: 'Type', align: 'center' },
    { key: 'declarant', label: 'Déclaré par' },
    { key: 'dateCreation', label: 'Date de création', align: 'center' },
    { key: 'actions', label: 'Actions', align: 'text-end' },
]

/* -------------------- chargement -------------------- */

async function chargerFiltres(): Promise<void> {
    const res = await getSignalementFiltres()
    if (res.success) typesDisponibles.value = res.data.types
}

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await listerSignalements({
        types: type.value === 'all' ? undefined : type.value,
        avecPlan: planAction.value === 'all'
            ? undefined
            : planAction.value === 'with',
        annee: annee.value,
    })
    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    items.value = res.data
}

watch([type, planAction, annee], charger)

onMounted(() => {
    chargerFiltres()
    charger()
})

/* -------------------- helpers -------------------- */

function versDetail(row: Signalement): void {
    router.push({ name: 'signalement-detail', params: { id: row.id } })
}

function versNouveau(): void {
    router.push({ name: 'signalement-nouveau' })
}

function classeType(t: string): string {
    const code = (t ?? '').toUpperCase()
    if (code === 'INCIDENT') return 'badge-type--incident'
    if (code === 'RISQUE') return 'badge-type--risque'
    return 'badge-type--autre'
}

function formaterDate(iso: string | null | undefined): string {
    if (!iso) return '—'
    const d = new Date(iso)
    return Number.isNaN(d.getTime()) ? iso : d.toLocaleDateString('fr-FR')
}

function nomComplet(s: Signalement): string {
    const parts = [s.prenomUtilisateur, s.nomUtilisateur].filter(Boolean)
    return parts.length ? parts.join(' ') : '—'
}
</script>

<template>
    <div class="sig-page">
        <!-- ============ EN-TÊTE ============ -->
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>Signalements</h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    Incidents et risques déclarés dans le dispositif
                </p>
            </div>

            <div class="welcome-row__right">
                <BaseButton
                    variant="secondary"
                    :loading="loading"
                    :disabled="loading"
                    @click="charger"
                >
                    <i class="bi bi-arrow-clockwise"></i>
                    Actualiser
                </BaseButton>
                <BaseButton variant="primary" @click="versNouveau">
                    <i class="bi bi-plus-lg"></i>
                    Déclarer un signalement
                </BaseButton>
            </div>
        </header>

        <!-- ============ SYNTHÈSE ============ -->
        <section class="summary" aria-label="Répartition des signalements">
            <div class="summary__stats" role="group" aria-label="Répartition par type">
                <article class="stat-tile stat-tile--total">
                    <span class="stat-tile__count">{{ nbTotal }}</span>
                    <span class="stat-tile__label">Total</span>
                    <span class="stat-tile__hint">Signalements affichés</span>
                </article>
                <article class="stat-tile stat-tile--incident">
                    <span class="stat-tile__count">{{ nbIncidents }}</span>
                    <span class="stat-tile__label">Incidents</span>
                    <span class="stat-tile__hint">Événements constatés</span>
                </article>
                <article class="stat-tile stat-tile--risque">
                    <span class="stat-tile__count">{{ nbRisques }}</span>
                    <span class="stat-tile__label">Risques</span>
                    <span class="stat-tile__hint">Risques identifiés</span>
                </article>
                <article class="stat-tile stat-tile--autre">
                    <span class="stat-tile__count">{{ nbAutres }}</span>
                    <span class="stat-tile__label">Autres</span>
                    <span class="stat-tile__hint">Autres signalements</span>
                </article>
            </div>
        </section>

        <!-- ============ MESSAGES ============ -->
        <div v-if="errorMessage" class="alert alert-danger mb-0 d-flex justify-content-between align-items-center" role="alert">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="charger">Réessayer</BaseButton>
        </div>

        <!-- ============ FILTRES ============ -->
        <section class="filters-card" aria-labelledby="filters-title">
            <header class="filters-card__head">
                <span class="filters-card__icon" aria-hidden="true">
                    <i class="bi bi-sliders"></i>
                </span>
                <div class="filters-card__heading">
                    <h2 id="filters-title" class="filters-card__title">Filtres</h2>
                    <span class="filters-card__sub">
                        {{ aDesFiltres ? 'Filtres actifs' : 'Affiner la liste des signalements' }}
                    </span>
                </div>
                <BaseButton
                    v-if="aDesFiltres"
                    variant="secondary"
                    size="sm"
                    icon="bi bi-x-lg"
                    @click="reinitialiserFiltres"
                >
                    Réinitialiser
                </BaseButton>
            </header>

            <div class="filters-card__body">
                <div class="filters__select">
                    <BaseSelect
                        v-model="annee"
                        label="Année"
                        size="sm"
                        placeholder="Année"
                        :options="anneesDisponibles"
                    />
                </div>

                <div class="filters__select">
                    <BaseSelect
                        v-model="type"
                        label="Type"
                        size="sm"
                        placeholder="Tous les types"
                        :options="typeOptions"
                    />
                </div>

                <div class="filters__select">
                    <BaseSelect
                        v-model="planAction"
                        label="Plan d'action"
                        size="sm"
                        placeholder="Tous"
                        :options="planActionOptions"
                    />
                </div>
            </div>
        </section>

        <!-- ============ TABLEAU ============ -->
        <section class="table-card" aria-label="Liste des signalements">
            <BaseTable
                :items="items"
                :columns="columns"
                :loading="loading"
                searchable
            >
                <template #cell-code="{ item }">
                    <code class="text-code">{{ item.code }}</code>
                </template>

                <template #cell-designation="{ item }">
                    <span class="sig-designation">{{ item.designation }}</span>
                </template>

                <template #cell-typeOrigine="{ item }">
                    <span class="badge-type" :class="classeType(item.typeOrigine)">
                        {{ item.typeOrigine }}
                    </span>
                </template>

                <template #cell-declarant="{ item }">
                    <span class="sig-declarant">{{ nomComplet(item) }}</span>
                </template>

                <template #cell-dateCreation="{ item }">
                    <span class="sig-date">{{ formaterDate(item.dateCreation) }}</span>
                </template>

                <template #cell-actions="{ item }">
                    <div class="d-flex justify-content-end gap-2 no-row-click">
                        <button
                            type="button"
                            class="table-action"
                            title="Consulter le détail"
                            aria-label="Consulter le détail"
                            @click="versDetail(item)"
                        >
                            <i class="bi bi-eye"></i>
                        </button>
                    </div>
                </template>
            </BaseTable>
        </section>
    </div>
</template>

<style scoped>
/* ============================================================
   CONTENEUR PAGE
   ============================================================ */
.sig-page {
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

.sig-page .welcome-row {
    margin-bottom: 0.25rem;
}

/* ============================================================
   SYNTHÈSE — tuiles statistiques
   ============================================================ */
.summary {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    padding: 1.25rem 1.5rem;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .04),
        0 8px 24px rgba(26, 42, 108, .06);
}

.summary__stats {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 0.85rem;
}

.stat-tile {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    justify-content: center;
    gap: 0.25rem;
    padding: 1rem 1.1rem;
    background: #f8fafc;
    border: 1.5px solid #e6ebf3;
    border-radius: 12px;
    position: relative;
    overflow: hidden;
}

.stat-tile::before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    bottom: 0;
    width: 4px;
    background: #c8d6fb;
}
.stat-tile--total::before { background: #1a2a6c; }
.stat-tile--incident::before { background: #f4b91f; }
.stat-tile--risque::before { background: #d64545; }
.stat-tile--autre::before { background: #8a95a8; }

.stat-tile__count {
    font-family: Manrope, "DM Sans", sans-serif;
    font-weight: 800;
    font-size: 1.85rem;
    line-height: 1;
    color: #1a2a6c;
    font-variant-numeric: tabular-nums;
}
.stat-tile--incident .stat-tile__count { color: #9a6409; }
.stat-tile--risque .stat-tile__count { color: #a61b16; }
.stat-tile--autre .stat-tile__count { color: #5b6b7d; }

.stat-tile__label {
    font-size: 0.66rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: #8a95a8;
}

.stat-tile__hint {
    font-size: 0.72rem;
    color: #6b7280;
}

/* ============================================================
   FILTRES — carte avec titre
   ============================================================ */
.filters-card {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .04),
        0 8px 24px rgba(26, 42, 108, .06);
    overflow: hidden;
}

.filters-card__head {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.85rem 1.25rem;
    background: linear-gradient(140deg, #f7f9fd, #fff);
    border-bottom: 1px solid #e6ebf3;
}

.filters-card__icon {
    display: grid;
    place-items: center;
    width: 32px;
    height: 32px;
    border-radius: 9px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.95rem;
    flex: none;
}

.filters-card__heading {
    display: flex;
    flex-direction: column;
    gap: 1px;
    flex: 1;
    min-width: 0;
}

.filters-card__title {
    margin: 0;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 0.9rem;
    font-weight: 700;
    color: #1a2a6c;
    letter-spacing: -0.005em;
    line-height: 1.2;
}

.filters-card__sub {
    font-size: 0.72rem;
    color: #6b7280;
    line-height: 1.2;
}

.filters-card__body {
    display: grid;
    grid-template-columns: minmax(120px, 160px) minmax(200px, 1fr) minmax(200px, 1fr);
    gap: 0.85rem;
    align-items: end;
    padding: 1rem 1.25rem;
}

.filters__select {
    min-width: 0;
}

/* ============================================================
   TABLEAU
   ============================================================ */
.table-card {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    overflow: hidden;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .04),
        0 8px 24px rgba(26, 42, 108, .06);
}

.table-card :deep(.base-table-container) {
    border: none;
    box-shadow: none;
    border-radius: 0;
}

/* --- Cellule code --- */
.text-code {
    font-size: 0.78rem;
    padding: 0.15rem 0.5rem;
    background: #eef1f5;
    color: #384a5e;
    border-radius: 4px;
    font-family: ui-monospace, Menlo, monospace;
    font-weight: 600;
}

/* --- Cellule désignation --- */
.sig-designation {
    font-weight: 500;
    color: #15233c;
}

/* --- Cellule déclarant --- */
.sig-declarant {
    color: #384a5e;
    font-size: 0.85rem;
}

/* --- Cellule date --- */
.sig-date {
    font-size: 0.82rem;
    color: #6b7280;
    white-space: nowrap;
}

/* --- Badge type --- */
.badge-type {
    display: inline-block;
    font-size: 0.72rem;
    font-weight: 700;
    padding: 0.2rem 0.65rem;
    border-radius: 20px;
    letter-spacing: 0.04em;
}

.badge-type--incident {
    background: #fdf3e2;
    color: #9a6409;
    border: 1px solid #f4e2c2;
}

.badge-type--risque {
    background: #fdecea;
    color: #a61b16;
    border: 1px solid #f6cfcc;
}

.badge-type--autre {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
}

/* --- Bouton d'action --- */
.table-action {
    width: 32px;
    height: 32px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    border: 1px solid #e6ebf3;
    background: #fff;
    color: #6b7280;
    cursor: pointer;
    transition: background-color .15s, color .15s, border-color .15s;
}

.table-action:hover {
    background: #eef5f8;
    color: #4a6cf7;
    border-color: #bdd3e6;
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 1199.98px) {
    .sig-page { padding-left: 24px; padding-right: 24px; }
}

@media (max-width: 991.98px) {
    .summary__stats {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .filters-card__body {
        grid-template-columns: 1fr 1fr;
    }
    .filters__select:first-child {
        grid-column: 1 / -1;
    }
}

@media (max-width: 767.98px) {
    .sig-page { padding: 24px 16px 20px; }
    .filters-card__body {
        grid-template-columns: 1fr;
    }
    .filters__select:first-child {
        grid-column: auto;
    }
}

@media (max-width: 575.98px) {
    .sig-page { padding: 20px 12px 16px; }
    .summary { padding: 1rem; }
    .summary__stats {
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 0.5rem;
    }
    .stat-tile {
        padding: 0.8rem 0.85rem;
    }
    .stat-tile__count { font-size: 1.5rem; }
    .stat-tile__label { font-size: 0.6rem; }
    .stat-tile__hint { font-size: 0.68rem; }
    .filters-card__head { padding: 0.75rem 1rem; }
    .filters-card__body { padding: 0.9rem 1rem; }
}

@media (prefers-reduced-motion: reduce) {
    .table-action {
        transition: none !important;
    }
}
</style>
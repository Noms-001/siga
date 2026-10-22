<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { BaseTable, BaseButton, BaseSelect } from '@/components/base'
import type { TableColumn } from '@/components/base/BaseTable/BaseTable.types'
import { listerIndicateursFront } from '@/services/indicateurFront'
import type { Indicateur } from '@/types/backoffice/indicateur'

const router = useRouter()

/* -------------------- état -------------------- */

const items = ref<Indicateur[]>([])
const loading = ref(false)
const errorMessage = ref('')

/* Filtres */
const type = ref<string>('all')
const frequenceVerification = ref<string>('all')
const frequenceAggregation = ref<string>('all')
const etat = ref<'all' | 'true' | 'false'>('true')

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

/* -------------------- options de filtres -------------------- */

const typeOptions = computed(() => {
    const set = new Set(items.value.map(i => i.typeIndicateur).filter((x): x is string => !!x))
    return [
        { value: 'all', label: 'Tous les types' },
        ...Array.from(set).sort().map(t => ({ value: t, label: t })),
    ]
})

const frequenceVerificationOptions = computed(() => {
    const set = new Set(items.value.map(i => i.frequenceVerification).filter((x): x is string => !!x))
    return [
        { value: 'all', label: 'Toutes' },
        ...Array.from(set).sort().map(f => ({ value: f, label: f })),
    ]
})

const frequenceAggregationOptions = computed(() => {
    const set = new Set(items.value.map(i => i.frequenceAggregation).filter((x): x is string => !!x))
    return [
        { value: 'all', label: 'Toutes' },
        ...Array.from(set).sort().map(f => ({ value: f, label: f })),
    ]
})

const etatOptions = [
    { value: 'all', label: 'Tous' },
    { value: 'true', label: 'Actifs' },
    { value: 'false', label: 'Inactifs' },
]

/* -------------------- filtrage local -------------------- */

const itemsFiltres = computed(() => {
    return items.value.filter(i => {
        if (frequenceVerification.value !== 'all'
            && i.frequenceVerification !== frequenceVerification.value) return false
        if (frequenceAggregation.value !== 'all'
            && i.frequenceAggregation !== frequenceAggregation.value) return false
        return true
    })
})

/* -------------------- KPI -------------------- */

const nbTotal = computed(() => items.value.length)
const nbKpi = computed(() =>
    items.value.filter(i => (i.typeIndicateur ?? '').toUpperCase() === 'KPI').length
)
const nbKri = computed(() =>
    items.value.filter(i => (i.typeIndicateur ?? '').toUpperCase() === 'KRI').length
)
const nbAutres = computed(() => nbTotal.value - nbKpi.value - nbKri.value)

/* -------------------- filtres actifs -------------------- */

const aDesFiltres = computed(() =>
    type.value !== 'all'
    || frequenceVerification.value !== 'all'
    || frequenceAggregation.value !== 'all'
    || etat.value !== 'true'
)

const nbFiltresLocaux = computed(() =>
    (frequenceVerification.value !== 'all' ? 1 : 0)
    + (frequenceAggregation.value !== 'all' ? 1 : 0)
)

function reinitialiserFiltres(): void {
    type.value = 'all'
    frequenceVerification.value = 'all'
    frequenceAggregation.value = 'all'
    etat.value = 'true'
}

/* -------------------- chargement -------------------- */

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await listerIndicateursFront({
        actif: etat.value === 'all' ? undefined : etat.value === 'true',
        type: type.value === 'all' ? undefined : type.value,
    })
    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    items.value = res.data
}

watch([type, etat], charger)

onMounted(charger)

/* -------------------- tableau -------------------- */

const columns: TableColumn<Indicateur>[] = [
    { key: 'typeIndicateur', label: 'Type', align: 'center' },
    { key: 'code', label: 'Code' },
    { key: 'designation', label: 'Indicateur' },
    { key: 'frequenceVerification', label: 'Fréq. vérif.' },
    { key: 'frequenceAggregation', label: 'Fréq. agrég.' },
    { key: 'valeurCible', label: 'Cible', align: 'text-end' },
    { key: 'seuils', label: 'Seuils', align: 'center' },
    { key: 'actions', label: 'Actions', align: 'text-end' },
]

function versDetail(row: Indicateur): void {
    router.push({ name: 'indicateur-detail', params: { id: row.id } })
}

function classeType(t: string | null | undefined): string {
    const code = (t ?? '').toUpperCase()
    if (code === 'KPI') return 'badge-type--kpi'
    if (code === 'KRI') return 'badge-type--kri'
    return 'badge-type--autre'
}
</script>

<template>
    <div class="ind-page">
        <!-- ============ EN-TÊTE ============ -->
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>Indicateurs KPI / KRI</h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    Indicateurs de performance et de risque du dispositif
                </p>
            </div>

            <div class="welcome-row__right">
                <BaseButton variant="secondary" :loading="loading" :disabled="loading" @click="charger">
                    <i class="bi bi-arrow-clockwise"></i>
                    Actualiser
                </BaseButton>
            </div>
        </header>

        <!-- ============ SYNTHÈSE ============ -->
        <section class="summary" aria-label="Répartition des indicateurs">
            <div class="summary__stats" role="group" aria-label="Répartition par type">
                <article class="stat-tile stat-tile--total">
                    <span class="stat-tile__count">{{ nbTotal }}</span>
                    <span class="stat-tile__label">Total</span>
                    <span class="stat-tile__hint">Indicateurs référencés</span>
                </article>
                <article class="stat-tile stat-tile--kpi">
                    <span class="stat-tile__count">{{ nbKpi }}</span>
                    <span class="stat-tile__label">KPI</span>
                    <span class="stat-tile__hint">Performance</span>
                </article>
                <article class="stat-tile stat-tile--kri">
                    <span class="stat-tile__count">{{ nbKri }}</span>
                    <span class="stat-tile__label">KRI</span>
                    <span class="stat-tile__hint">Risque</span>
                </article>
                <article class="stat-tile stat-tile--autre">
                    <span class="stat-tile__count">{{ nbAutres }}</span>
                    <span class="stat-tile__label">Autres</span>
                    <span class="stat-tile__hint">Non typés</span>
                </article>
            </div>
        </section>

        <!-- ============ MESSAGES ============ -->
        <div v-if="errorMessage" class="alert alert-danger mb-0 d-flex justify-content-between align-items-center"
            role="alert">
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
                        {{ aDesFiltres ? 'Filtres actifs' : 'Affiner la liste des indicateurs' }}
                    </span>
                </div>
                <BaseButton v-if="aDesFiltres" variant="secondary" size="sm" icon="bi bi-x-lg"
                    @click="reinitialiserFiltres">
                    Réinitialiser
                </BaseButton>
            </header>

            <div class="filters-card__body">
                <div class="filters__select">
                    <BaseSelect v-model="type" label="Type" size="sm"
                        :options="typeOptions" />
                </div>

                <div class="filters__select">
                    <BaseSelect v-model="frequenceVerification" label="Fréquence de vérification" size="sm"
                        :options="frequenceVerificationOptions" />
                </div>

                <div class="filters__select">
                    <BaseSelect v-model="frequenceAggregation" label="Fréquence d'agrégation" size="sm"
                        :options="frequenceAggregationOptions" />
                </div>

                <div class="filters__select">
                    <BaseSelect v-model="etat" label="État" size="sm" :options="etatOptions" />
                </div>
            </div>

            <footer v-if="nbFiltresLocaux > 0" class="filters-card__foot">
                <i class="bi bi-info-circle" aria-hidden="true"></i>
                <span>
                    {{ nbFiltresLocaux }} filtre{{ nbFiltresLocaux > 1 ? 's' : '' }} appliqué{{ nbFiltresLocaux > 1 ?
                    's' : '' }} en local sur {{ itemsFiltres.length }} indicateur{{ itemsFiltres.length > 1 ? 's' : ''
                    }} affiché{{ itemsFiltres.length > 1 ? 's' : '' }}.
                </span>
            </footer>
        </section>

        <!-- ============ TABLEAU ============ -->
        <section class="table-card" aria-label="Liste des indicateurs">
            <BaseTable :items="itemsFiltres" :columns="columns" :loading="loading" searchable>
                <template #cell-typeIndicateur="{ item }">
                    <span v-if="item.typeIndicateur" class="badge-type" :class="classeType(item.typeIndicateur)">
                        {{ item.typeIndicateur }}
                    </span>
                    <span v-else class="text-muted-custom">—</span>
                </template>

                <template #cell-code="{ item }">
                    <code class="text-code">{{ item.code }}</code>
                </template>

                <template #cell-designation="{ item }">
                    <span class="ind-designation">{{ item.designation }}</span>
                </template>

                <template #cell-frequenceVerification="{ value }">
                    <span class="text-muted-custom">{{ value || '—' }}</span>
                </template>

                <template #cell-frequenceAggregation="{ value }">
                    <span class="text-muted-custom">{{ value || '—' }}</span>
                </template>

                <template #cell-valeurCible="{ value }">
                    <span class="cell-cible">{{ value != null ? value : '—' }}</span>
                </template>

                <template #cell-seuils="{ item }">
                    <span v-if="item.seuilMin != null || item.seuilMax != null" class="ind-seuils">
                        <span>{{ item.seuilMin ?? '—' }}</span>
                        <span class="ind-seuils__sep">→</span>
                        <span>{{ item.seuilMax ?? '—' }}</span>
                    </span>
                    <span v-else class="text-muted-custom">—</span>
                </template>

                <template #cell-actions="{ item }">
                    <div class="d-flex justify-content-end gap-2 no-row-click">
                        <button type="button" class="table-action" title="Consulter le détail"
                            aria-label="Consulter le détail" @click="versDetail(item)">
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
   CONTENEUR
   ============================================================ */
.ind-page {
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

.ind-page .welcome-row {
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

.stat-tile--total::before {
    background: #1a2a6c;
}

.stat-tile--kpi::before {
    background: #4a6cf7;
}

.stat-tile--kri::before {
    background: #f4b91f;
}

.stat-tile--autre::before {
    background: #8a95a8;
}

.stat-tile__count {
    font-family: Manrope, "DM Sans", sans-serif;
    font-weight: 800;
    font-size: 1.85rem;
    line-height: 1;
    color: #1a2a6c;
    font-variant-numeric: tabular-nums;
}

.stat-tile--kpi .stat-tile__count {
    color: #135d95;
}

.stat-tile--kri .stat-tile__count {
    color: #9a6409;
}

.stat-tile--autre .stat-tile__count {
    color: #5b6b7d;
}

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
    grid-template-columns: repeat(4, minmax(160px, 1fr));
    gap: 0.85rem;
    align-items: center;
    padding: 1rem 1.25rem;
}

.filters-card__foot {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.65rem 1.25rem;
    background: #f8fafc;
    border-top: 1px solid #e6ebf3;
    font-size: 0.76rem;
    color: #6b7280;
}

.filters-card__foot i {
    color: #4a6cf7;
    font-size: 0.9rem;
    flex: none;
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
.ind-designation {
    font-weight: 500;
    color: #15233c;
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

.badge-type--kpi {
    background: #e4f0fb;
    color: #135d95;
    border: 1px solid #cde2f6;
}

.badge-type--kri {
    background: #fdf3e2;
    color: #9a6409;
    border: 1px solid #f4e2c2;
}

.badge-type--autre {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
}

/* --- Cellule seuils --- */
.ind-seuils {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.78rem;
    color: #15233c;
}

.ind-seuils__sep {
    color: #8a95a8;
}

/* --- Cellule cible --- */
.cell-cible {
    font-weight: 600;
    color: #1a2a6c;
    font-variant-numeric: tabular-nums;
}

/* --- Cellule vide --- */
.text-muted-custom {
    color: #8a95a8;
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
    .ind-page {
        padding-left: 24px;
        padding-right: 24px;
    }
}

@media (max-width: 991.98px) {
    .summary__stats {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .filters-card__body {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 767.98px) {
    .ind-page {
        padding: 24px 16px 20px;
    }

    .filters-card__body {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 575.98px) {
    .ind-page {
        padding: 20px 12px 16px;
    }

    .summary {
        padding: 1rem;
    }

    .summary__stats {
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 0.5rem;
    }

    .stat-tile {
        padding: 0.8rem 0.85rem;
    }

    .stat-tile__count {
        font-size: 1.5rem;
    }

    .stat-tile__label {
        font-size: 0.6rem;
    }

    .stat-tile__hint {
        font-size: 0.68rem;
    }

    .filters-card__head {
        padding: 0.75rem 1rem;
    }

    .filters-card__body {
        padding: 0.9rem 1rem;
    }

    .filters-card__foot {
        padding: 0.6rem 1rem;
        font-size: 0.72rem;
    }
}

@media (prefers-reduced-motion: reduce) {

    .stat-tile,
    .table-action {
        transition: none !important;
    }
}
</style>
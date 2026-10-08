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

/* -------------------- options de filtres -------------------- */

/**
 * Types distincts calculés sur la liste courante.
 *
 * La liste backend ne fournit pas d'endpoint /filtres côté frontoffice :
 * ajouter un endpoint dédié pour trois listes de valeurs distinctes
 * serait disproportionné. Les valeurs sont extraites de ce qui est déjà
 * chargé — filtrer sur ce qu'on voit est exactement ce qu'attend
 * l'utilisateur.
 */
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
/*
 * Le backend fournit déjà `search`, `type`, `actif`. Les deux fréquences
 * ne sont pas exposées par l'API — on filtre dessus en local. La liste
 * d'indicateurs reste petite (référentiel), et le filtrage client évite
 * trois paramètres supplémentaires côté backend pour un gain nul.
 */
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
/*
 * Trois cartes seulement, parce que les trois seules qui peuvent être
 * calculées à partir de la définition d'un indicateur. Conformité /
 * proche du seuil / hors seuil dépendent de la dernière valeur mesurée
 * (valeur_indicateur), hors périmètre de cette page. Afficher ces trois
 * cartes avec des zéros serait trompeur.
 */
const nbTotal = computed(() => items.value.length)
const nbKpi = computed(() =>
    items.value.filter(i => (i.typeIndicateur ?? '').toUpperCase() === 'KPI').length
)
const nbKri = computed(() =>
    items.value.filter(i => (i.typeIndicateur ?? '').toUpperCase() === 'KRI').length
)

/* -------------------- filtres actifs -------------------- */

const aDesFiltres = computed(() =>
    type.value !== 'all'
    || frequenceVerification.value !== 'all'
    || frequenceAggregation.value !== 'all'
    || etat.value !== 'true'
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
    { key: 'frequenceVerification', label: 'Fréq. vérification' },
    { key: 'frequenceAggregation', label: 'Fréq. agrégation' },
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
        <!-- En-tête -->
        <header class="ind-header">
            <div>
                <h1 class="ind-header__title">Indicateurs KPI / KRI</h1>
                <p class="ind-header__subtitle">
                    Indicateurs de performance et de risque du dispositif
                </p>
            </div>
        </header>

        <!-- KPI -->
        <section class="ind-cards">
            <article class="ind-card">
                <span class="ind-card__label">Total</span>
                <span class="ind-card__value">{{ nbTotal }}</span>
                <span class="ind-card__hint">Indicateurs référencés</span>
            </article>
            <article class="ind-card ind-card--kpi">
                <span class="ind-card__label">KPI</span>
                <span class="ind-card__value">{{ nbKpi }}</span>
                <span class="ind-card__hint">Performance</span>
            </article>
            <article class="ind-card ind-card--kri">
                <span class="ind-card__label">KRI</span>
                <span class="ind-card__value">{{ nbKri }}</span>
                <span class="ind-card__hint">Risque</span>
            </article>
        </section>

        <!-- Message d'erreur -->
        <div v-if="errorMessage" class="alert alert-danger mb-0 d-flex justify-content-between align-items-center"
            role="alert">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="charger">Réessayer</BaseButton>
        </div>

        <!-- Filtres -->
        <div class="ind-filters">
            <BaseSelect v-model="type" label="Type" size="sm" placeholder="Tous les types" :options="typeOptions" />
            <BaseSelect v-model="frequenceVerification" label="Fréq. vérification" size="sm" placeholder="Toutes"
                :options="frequenceVerificationOptions" />
            <BaseSelect v-model="frequenceAggregation" label="Fréq. agrégation" size="sm" placeholder="Toutes"
                :options="frequenceAggregationOptions" />
            <BaseSelect v-model="etat" label="État" size="sm" placeholder="Tous" :options="etatOptions" />

            <div class="ind-filters__reset">
                <BaseButton variant="secondary" size="sm" icon="bi bi-arrow-counterclockwise" :disabled="!aDesFiltres"
                    @click="reinitialiserFiltres">
                    Réinitialiser
                </BaseButton>
            </div>
        </div>

        <!-- Tableau -->
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
                {{ value != null ? value : '—' }}
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
                    <button type="button" class="table-action" title="Consulter" @click="versDetail(item)">
                        <i class="bi bi-eye"></i>
                    </button>
                </div>
            </template>
        </BaseTable>
    </div>
</template>

<style scoped>
.ind-page {
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
}

/* --- en-tête --- */

.ind-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 1rem;
}

.ind-header__title {
    font-size: 1.4rem;
    font-weight: 700;
    color: var(--dts-navy);
    margin: 0;
}

.ind-header__subtitle {
    color: var(--dts-muted);
    margin: 0.15rem 0 0;
    font-size: 0.85rem;
}

/* --- cartes KPI --- */

.ind-cards {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 1rem;
}

.ind-card {
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
    padding: 1rem 1.15rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-left: 4px solid var(--dts-navy);
    border-radius: var(--dts-radius);
}

.ind-card--kpi {
    border-left-color: var(--dts-blue);
}

.ind-card--kri {
    border-left-color: #9a6409;
}

.ind-card__label {
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    color: var(--dts-muted);
}

.ind-card__value {
    font-size: 1.75rem;
    font-weight: 700;
    color: var(--dts-navy);
    line-height: 1;
}

.ind-card--kpi .ind-card__value {
    color: var(--dts-blue);
}

.ind-card--kri .ind-card__value {
    color: #9a6409;
}

.ind-card__hint {
    font-size: 0.72rem;
    color: var(--dts-muted);
}

/* --- filtres --- */

.ind-filters {
    display: grid;
    grid-template-columns: repeat(4, minmax(160px, 1fr)) auto;
    gap: 1rem;
    align-items: end;
}

.ind-filters :deep(.base-select-container) {
    gap: 0.35rem;
}

.ind-filters__reset {
    display: flex;
    align-items: end;
}

/* --- tableau --- */

.text-code {
    font-size: 0.78rem;
    padding: 0.15rem 0.5rem;
    background: #eef1f5;
    color: #384a5e;
    border-radius: 4px;
    font-family: ui-monospace, Menlo, monospace;
    font-weight: 600;
}

.ind-designation {
    font-weight: 500;
    color: var(--dts-text);
}

.badge-type {
    display: inline-block;
    font-size: 0.72rem;
    font-weight: 700;
    padding: 0.15rem 0.6rem;
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

.ind-seuils {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.78rem;
    color: var(--dts-text);
}

.ind-seuils__sep {
    color: var(--dts-muted);
}

.text-muted-custom {
    color: var(--dts-muted);
}

.table-action {
    width: 32px;
    height: 32px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    border: 1px solid var(--dts-border);
    background: transparent;
    color: var(--dts-text-2);
    cursor: pointer;
    transition: background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.table-action:hover {
    background: var(--dts-blue-50);
    color: var(--dts-blue);
    border-color: #bdd3e6;
}

/* --- responsive --- */

@media (max-width: 992px) {
    .ind-filters {
        grid-template-columns: repeat(2, 1fr);
    }

    .ind-filters__reset {
        grid-column: 1 / -1;
        justify-content: flex-end;
    }
}

@media (max-width: 768px) {
    .ind-cards {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 576px) {
    .ind-page {
        padding: 1rem;
    }

    .ind-filters {
        grid-template-columns: 1fr;
    }

    .ind-filters__reset {
        justify-content: stretch;
    }

    .ind-filters__reset :deep(.base-btn) {
        width: 100%;
    }
}
</style>
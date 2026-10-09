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

const items = ref<Signalement[]>([])
const typesDisponibles = ref<string[]>([])
const loading = ref(false)
const errorMessage = ref('')

const type = ref<string>('all')

const typeOptions = computed(() => [
    { value: 'all', label: 'Tous les types' },
    ...typesDisponibles.value.map(t => ({ value: t, label: t })),
])

const columns: TableColumn<Signalement>[] = [
    { key: 'code', label: 'Code' },
    { key: 'designation', label: 'Désignation' },
    { key: 'typeOrigine', label: 'Type', align: 'center' },
    { key: 'declarant', label: 'Déclaré par' },
    { key: 'dateCreation', label: 'Date de création', align: 'center' },
    { key: 'actions', label: 'Actions', align: 'text-end' },
]

const planAction = ref<'all' | 'with' | 'without'>('all')

const planActionOptions = [
    { value: 'all', label: 'Tous' },
    { value: 'with', label: 'Avec plan d\'action' },
    { value: 'without', label: 'Sans plan d\'action' },
]

const annee = ref<number>(getCurrentYear())

const anneesDisponibles = computed(() => {
    const base = getCurrentYear()
    return [base - 2, base - 1, base, base + 1, base + 2]
        .map(y => ({ value: y, label: String(y) }))
})

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
    if (!res.success) { errorMessage.value = res.error; return }
    items.value = res.data
}

function versDetail(row: Signalement): void {
    router.push({ name: 'signalement-detail', params: { id: row.id } })
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

watch([type, planAction, annee], charger)
onMounted(() => {
    chargerFiltres()
    charger()
})
</script>

<template>
    <div class="sig-page">
        <header class="sig-header">
            <div>
                <h1 class="sig-header__title">Signalements</h1>
                <p class="sig-header__subtitle">
                    Incidents et risques déclarés dans le dispositif
                </p>
            </div>
            <BaseButton icon="bi bi-plus-lg" @click="router.push({ name: 'signalement-nouveau' })">
                Déclarer un signalement
            </BaseButton>
        </header>

        <div v-if="errorMessage" class="alert alert-danger mb-0 d-flex justify-content-between align-items-center"
            role="alert">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="charger">Réessayer</BaseButton>
        </div>

        <div class="sig-filters">
            <BaseSelect v-model="annee" label="Année" size="sm" placeholder="Année" :options="anneesDisponibles" />
            <BaseSelect v-model="type" label="Type" size="sm" placeholder="Tous les types" :options="typeOptions" />
            <BaseSelect v-model="planAction" label="Plan d'action" size="sm" placeholder="Tous"
                :options="planActionOptions" />
        </div>

        <BaseTable :items="items" :columns="columns" :loading="loading" searchable>
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
                {{ nomComplet(item) }}
            </template>

            <template #cell-dateCreation="{ item }">
                {{ formaterDate(item.dateCreation) }}
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
.sig-page {
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
}

.sig-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 1rem;
}

.sig-header__title {
    font-size: 1.4rem;
    font-weight: 700;
    color: var(--dts-navy);
    margin: 0;
}

.sig-header__subtitle {
    color: var(--dts-muted);
    margin: 0.15rem 0 0;
    font-size: 0.85rem;
}

.sig-filters {
    display: grid;
    grid-template-columns: minmax(120px, 140px) minmax(180px, 240px) minmax(180px, 240px);
    gap: 1rem;
    align-items: end;
}

.sig-filters :deep(.base-select-container) {
    gap: 0.35rem;
}

.text-code {
    font-size: 0.78rem;
    padding: 0.15rem 0.5rem;
    background: #eef1f5;
    color: #384a5e;
    border-radius: 4px;
    font-family: ui-monospace, Menlo, monospace;
    font-weight: 600;
}

.sig-designation {
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

@media (max-width: 576px) {
    .sig-page {
        padding: 1rem;
    }

    .sig-filters {
        grid-template-columns: 1fr;
    }

    .sig-filters> :first-child {
        grid-column: auto;
    }
}

@media (max-width: 768px) {
    .sig-filters {
        grid-template-columns: 1fr 1fr;
    }

    .sig-filters> :first-child {
        grid-column: 1 / -1;
    }
}
</style>
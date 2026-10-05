<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { BaseTable, BaseButton, BaseSelect } from '@/components/base'
import type { TableColumn } from '@/components/base/BaseTable/BaseTable.types'
import {
    getPermissionFiltres,
    listerPermissions,
} from '@/services/permission'
import type { Permission, PermissionFiltres } from '@/types/backoffice/permission'

const router = useRouter()

const items = ref<Permission[]>([])
const loading = ref(false)
const errorMessage = ref('')

const filtresDisponibles = ref<PermissionFiltres>({ ressources: [], actions: [] })

/** 'all' = pas de filtre ; sinon true / false. Le select manipule du string. */
const statut = ref<'all' | 'true' | 'false'>('all')
const ressource = ref<string>('all')
const action = ref<string>('all')

const statutOptions = [
    { value: 'all', label: 'Toutes' },
    { value: 'true', label: 'Actives' },
    { value: 'false', label: 'Inactives' },
]

const ressourceOptions = computed(() => [
    { value: 'all', label: 'Toutes' },
    ...filtresDisponibles.value.ressources.map(r => ({ value: r, label: r })),
])

const actionOptions = computed(() => [
    { value: 'all', label: 'Toutes' },
    ...filtresDisponibles.value.actions.map(a => ({ value: a, label: a })),
])

const columns: TableColumn<Permission>[] = [
    { key: 'designation', label: 'Désignation' },
    { key: 'ressource', label: 'Ressource' },
    { key: 'action', label: 'Action' },
    { key: 'actif', label: 'Statut', align: 'center' },
    { key: 'actions', label: 'Actions', align: 'text-end' },
]

async function chargerFiltres(): Promise<void> {
    const res = await getPermissionFiltres()
    if (res.success) filtresDisponibles.value = res.data
}

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await listerPermissions({
        actif: statut.value === 'all' ? null : statut.value === 'true',
        ressource: ressource.value === 'all' ? undefined : ressource.value,
        action: action.value === 'all' ? undefined : action.value,
    })
    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    items.value = res.data
}

function versDetail(row: Permission): void {
    router.push({ name: 'backoffice-permissions-detail', params: { id: row.id } })
}

const aDesFiltresActifs = computed(
    () => statut.value !== 'all'
        || ressource.value !== 'all'
        || action.value !== 'all'
)

function reinitialiserFiltres(): void {
    if (!aDesFiltresActifs.value) return
    statut.value = 'all'
    ressource.value = 'all'
    action.value = 'all'
    // Le watch([statut, ressource, action]) déclenche charger() automatiquement.
}

watch([statut, ressource, action], charger)

onMounted(() => {
    chargerFiltres()
    charger()
})
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">Permissions</h1>
                <p class="bo-page__subtitle">
                    Référentiel des permissions du système (consultation uniquement)
                </p>
            </div>
        </header>

        <div v-if="errorMessage" class="alert alert-danger mb-0 d-flex justify-content-between align-items-center"
            role="alert">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="charger">Réessayer</BaseButton>
        </div>

        <div class="bo-filters">
            <BaseSelect v-model="ressource" label="Ressource" placeholder="Toutes" size="sm"
                :options="ressourceOptions" />
            <BaseSelect v-model="action" label="Action" placeholder="Toutes" size="sm" :options="actionOptions" />
            <BaseSelect v-model="statut" label="Statut" placeholder="Toutes" size="sm" :options="statutOptions" />

            <div class="bo-filters__reset">
                <BaseButton variant="secondary" size="sm" icon="bi bi-arrow-counterclockwise"
                    :disabled="!aDesFiltresActifs" @click="reinitialiserFiltres">
                    Réinitialiser
                </BaseButton>
            </div>
        </div>

        <BaseTable :items="items" :columns="columns" :loading="loading" searchable>
            <template #cell-ressource="{ item }">
                <span class="badge-ressource">{{ item.ressource }}</span>
            </template>

            <template #cell-action="{ item }">
                <code class="text-action">{{ item.action }}</code>
            </template>

            <template #cell-actif="{ item }">
                <span class="badge" :class="item.actif ? 'badge-actif' : 'badge-inactif'">
                    {{ item.actif ? 'Active' : 'Inactive' }}
                </span>
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
.bo-page {
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.bo-page__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 1rem;
}

.bo-page__title {
    font-size: 1.4rem;
    font-weight: 700;
    color: var(--dts-navy);
    margin: 0;
}

.bo-page__subtitle {
    color: var(--dts-muted);
    margin: 0;
    font-size: 0.85rem;
}

.bo-filters {
    display: grid;
    grid-template-columns: repeat(3, minmax(180px, 220px)) auto;
    gap: 1rem;
    align-items: end;
}

.bo-filters :deep(.base-select-container) {
    gap: 0.35rem;
}

.bo-filters__reset {
    display: flex;
    align-items: end;
    /* Le label des selects occupe une ligne : on aligne le bouton sur la
       même ligne que les champs, pas sur celle des labels. */
    padding-bottom: 0;
}

.badge-ressource {
    display: inline-block;
    padding: 0.15rem 0.55rem;
    border-radius: 4px;
    background: #e4f0fb;
    color: #135d95;
    border: 1px solid #cde2f6;
    font-size: 0.72rem;
    font-weight: 600;
    letter-spacing: 0.02em;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.text-action {
    font-size: 0.78rem;
    padding: 0.15rem 0.5rem;
    background: #eef1f5;
    color: #384a5e;
    border-radius: 4px;
}

.badge-actif {
    background: #e6f4ea;
    color: #1e7e34;
    border: 1px solid #c8e5cf;
    font-weight: 600;
    padding: 0.25rem 0.7rem;
    border-radius: 20px;
    font-size: 0.7rem;
}

.badge-inactif {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
    font-weight: 600;
    padding: 0.25rem 0.7rem;
    border-radius: 20px;
    font-size: 0.7rem;
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

@media (max-width: 768px) {
    .bo-filters {
        grid-template-columns: 1fr 1fr;
    }
}

@media (max-width: 992px) {
    .bo-filters {
        grid-template-columns: 1fr 1fr;
    }

    .bo-filters__reset {
        grid-column: 1 / -1;
        justify-content: flex-end;
    }
}

@media (max-width: 576px) {
    .bo-page {
        padding: 1rem;
    }

    .bo-filters {
        grid-template-columns: 1fr;
    }

    .bo-filters__reset {
        justify-content: stretch;
    }

    .bo-filters__reset :deep(.base-btn) {
        width: 100%;
    }
}
</style>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseTable, BaseButton } from '@/components/base'
import type { TableColumn } from '@/components/base/BaseTable/BaseTable.types'
import { listerDepartements } from '@/services/departement'
import type { Departement } from '@/types/backoffice/departement'

const router = useRouter()
const route = useRoute()

const items = ref<Departement[]>([])
const loading = ref(false)
const errorMessage = ref('')
const flashMessage = ref('')

const columns: TableColumn<Departement>[] = [
    { key: 'code', label: 'Code' },
    { key: 'nom', label: 'Nom' },
    { key: 'actions', label: 'Actions', align: 'text-end' },
]

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await listerDepartements()
    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    items.value = res.data
}

function versDetail(row: Departement): void {
    router.push({ name: 'backoffice-departements-detail', params: { id: row.id } })
}

function versEdition(row: Departement): void {
    router.push({ name: 'backoffice-departements-modifier', params: { id: row.id } })
}

onMounted(() => {
    if (route.query.created) flashMessage.value = 'Département créé avec succès.'
    else if (route.query.updated) flashMessage.value = 'Département modifié avec succès.'
    if (route.query.created || route.query.updated) router.replace({ query: {} })
    charger()
})
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">Départements</h1>
                <p class="bo-page__subtitle">Gestion des départements du système</p>
            </div>
            <BaseButton icon="bi bi-plus-lg"
                @click="router.push({ name: 'backoffice-departements-nouveau' })">
                Ajouter un département
            </BaseButton>
        </header>

        <div v-if="flashMessage" class="alert alert-success mb-0" role="status">
            {{ flashMessage }}
        </div>

        <div v-if="errorMessage" class="alert alert-danger mb-0 d-flex justify-content-between align-items-center" role="alert">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="charger">Réessayer</BaseButton>
        </div>

        <BaseTable :items="items" :columns="columns" :loading="loading" searchable>

            <template #cell-actions="{ item }">
                <div class="d-flex justify-content-end gap-2 no-row-click">
                    <button type="button" class="table-action" title="Consulter" @click="versDetail(item)">
                        <i class="bi bi-eye"></i>
                    </button>
                    <button type="button" class="table-action" title="Modifier" @click="versEdition(item)">
                        <i class="bi bi-pencil"></i>
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
    gap: 1rem;
    flex-wrap: wrap;
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
    .bo-page { padding: 1rem; }
}
</style>
<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { BaseTable, BaseButton } from '@/components/base'
import type { TableColumn } from '@/components/base/BaseTable/BaseTable.types'
import { listerStatuts } from '@/services/statut'
import type { Statut } from '@/types/backoffice/statut'

const items = ref<Statut[]>([])
const loading = ref(false)
const errorMessage = ref('')
const inclureInactifs = ref(false)

const columns: TableColumn<Statut>[] = [
    { key: 'code', label: 'Code' },
    { key: 'libelle', label: 'Libellé' },
    { key: 'description', label: 'Description' },
    { key: 'actif', label: 'Statut', align: 'center' },
]

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await listerStatuts(inclureInactifs.value)
    loading.value = false
    if (!res.success) { errorMessage.value = res.error; return }
    items.value = res.data
}

watch(inclureInactifs, charger)
onMounted(charger)
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">Statuts</h1>
                <p class="bo-page__subtitle">
                    Référentiel des statuts (consultation uniquement)
                </p>
            </div>
        </header>

        <div v-if="errorMessage" class="alert alert-danger mb-0 d-flex justify-content-between align-items-center"
            role="alert">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="charger">Réessayer</BaseButton>
        </div>

        <div class="bo-filters">
            <label class="bo-filters__check">
                <input type="checkbox" v-model="inclureInactifs">
                <span>Afficher les statuts désactivés</span>
            </label>
        </div>

        <BaseTable :items="items" :columns="columns" :loading="loading" searchable>
            <template #cell-code="{ item }">
                <code class="text-code">{{ item.code }}</code>
            </template>
            <template #cell-description="{ value }">
                <span class="text-muted-custom">{{ value || '—' }}</span>
            </template>
            <template #cell-actif="{ item }">
                <span class="badge" :class="item.actif ? 'badge-actif' : 'badge-inactif'">
                    {{ item.actif ? 'Actif' : 'Inactif' }}
                </span>
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
    display: flex;
    flex-wrap: wrap;
    gap: 1rem;
    align-items: center;
}

.bo-filters__check {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.85rem;
    color: var(--dts-text-2);
    cursor: pointer;
    user-select: none;
}

.bo-filters__check input {
    width: 16px;
    height: 16px;
    accent-color: var(--dts-blue);
}

.text-muted-custom {
    color: var(--dts-muted);
}

.text-code {
    font-size: 0.78rem;
    padding: 0.15rem 0.5rem;
    background: #eef1f5;
    color: #384a5e;
    border-radius: 4px;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-weight: 600;
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

@media (max-width: 576px) {
    .bo-page {
        padding: 1rem;
    }
}
</style>
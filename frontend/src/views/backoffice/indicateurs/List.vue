<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseTable, BaseButton, BaseSelect, BaseConfirm } from '@/components/base'
import type { TableColumn } from '@/components/base/BaseTable/BaseTable.types'
import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'
import {
    activerIndicateur,
    desactiverIndicateur,
    getIndicateurFiltres,
    listerIndicateurs,
} from '@/services/indicateur'
import type { Indicateur } from '@/types/backoffice/indicateur'

const router = useRouter()
const route = useRoute()

const items = ref<Indicateur[]>([])
const typesDisponibles = ref<string[]>([])
const loading = ref(false)
const errorMessage = ref('')
const flashMessage = ref('')

const statut = ref<'all' | 'true' | 'false'>('all')
const type = ref<string>('all')

const confirmOpen = ref(false)
const confirmLoading = ref(false)
const confirmDemande = ref<ConfirmDemande | null>(null)
const indicateurCible = ref<Indicateur | null>(null)

const statutOptions = [
    { value: 'all', label: 'Tous' },
    { value: 'true', label: 'Actifs' },
    { value: 'false', label: 'Inactifs' },
]

const typeOptions = computed(() => [
    { value: 'all', label: 'Tous les types' },
    ...typesDisponibles.value.map(t => ({ value: t, label: t })),
])

const columns: TableColumn<Indicateur>[] = [
    { key: 'code', label: 'Code' },
    { key: 'designation', label: 'Désignation' },
    { key: 'typeIndicateur', label: 'Type', align: 'center' },
    { key: 'uniteMesure', label: 'Unité', align: 'center' },
    { key: 'valeurCible', label: 'Cible', align: 'text-end' },
    { key: 'seuilMin', label: 'Seuil min', align: 'text-end' },
    { key: 'seuilMax', label: 'Seuil max', align: 'text-end' },
    { key: 'actif', label: 'Statut', align: 'center' },
    { key: 'actions', label: 'Actions', align: 'text-end' },
]

async function chargerTypes(): Promise<void> {
    const res = await getIndicateurFiltres()
    if (res.success) typesDisponibles.value = res.data.types
}

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await listerIndicateurs({
        actif: statut.value === 'all' ? null : statut.value === 'true',
        type: type.value === 'all' ? undefined : type.value,
    })
    loading.value = false
    if (!res.success) { errorMessage.value = res.error; return }
    items.value = res.data
}

function versDetail(row: Indicateur): void {
    router.push({ name: 'backoffice-indicateurs-detail', params: { id: row.id } })
}

function versEdition(row: Indicateur): void {
    router.push({ name: 'backoffice-indicateurs-modifier', params: { id: row.id } })
}

function demanderBascule(row: Indicateur): void {
    indicateurCible.value = row
    confirmDemande.value = row.actif
        ? {
            titre: "Désactiver l'indicateur ?",
            message: `L'indicateur « ${row.code} » sera désactivé.`,
            consequence: "Il ne pourra plus être associé à de nouvelles activités.",
            libelleConfirmer: 'Désactiver',
            ton: 'warning',
            icone: 'bi bi-slash-circle',
        }
        : {
            titre: "Réactiver l'indicateur ?",
            message: `L'indicateur « ${row.code} » sera réactivé.`,
            consequence: 'Il redeviendra disponible.',
            libelleConfirmer: 'Réactiver',
            ton: 'primary',
            icone: 'bi bi-arrow-clockwise',
        }
    confirmOpen.value = true
}

async function confirmerBascule(): Promise<void> {
    if (!indicateurCible.value) return
    const cible = indicateurCible.value
    const etaitActif = cible.actif

    confirmLoading.value = true
    const res = etaitActif
        ? await desactiverIndicateur(cible.id)
        : await activerIndicateur(cible.id)
    confirmLoading.value = false

    if (!res.success) { confirmOpen.value = false; errorMessage.value = res.error; return }

    flashMessage.value = etaitActif
        ? `L'indicateur « ${cible.code} » a été désactivé.`
        : `L'indicateur « ${cible.code} » a été réactivé.`

    confirmOpen.value = false
    indicateurCible.value = null
    await charger()
}

watch([statut, type], charger)

onMounted(() => {
    if (route.query.created) flashMessage.value = 'Indicateur créé avec succès.'
    else if (route.query.updated) flashMessage.value = 'Indicateur modifié avec succès.'
    if (route.query.created || route.query.updated) router.replace({ query: {} })
    chargerTypes()
    charger()
})
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">Indicateurs</h1>
                <p class="bo-page__subtitle">Gestion des indicateurs de performance</p>
            </div>
            <BaseButton icon="bi bi-plus-lg" @click="router.push({ name: 'backoffice-indicateurs-nouveau' })">
                Ajouter un indicateur
            </BaseButton>
        </header>

        <div v-if="flashMessage" class="alert alert-success mb-0" role="status">
            {{ flashMessage }}
        </div>

        <div v-if="errorMessage" class="alert alert-danger mb-0 d-flex justify-content-between align-items-center"
            role="alert">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="charger">Réessayer</BaseButton>
        </div>

        <div class="bo-filters">
            <BaseSelect v-model="type" label="Type" size="sm" placeholder="Tous les types" :options="typeOptions" />
            <BaseSelect v-model="statut" label="Statut" size="sm" placeholder="Tous" :options="statutOptions" />
        </div>

        <BaseTable :items="items" :columns="columns" :loading="loading" searchable>
            <template #cell-code="{ item }">
                <code class="text-code">{{ item.code }}</code>
            </template>

            <template #cell-typeIndicateur="{ value }">
                <span v-if="value" class="badge-type">{{ value }}</span>
                <span v-else class="text-muted-custom">—</span>
            </template>

            <template #cell-uniteMesure="{ value }">
                <span class="text-muted-custom">{{ value || '—' }}</span>
            </template>

            <template #cell-valeurCible="{ value }">
                {{ value != null ? value : '—' }}
            </template>

            <template #cell-seuilMin="{ value }">
                {{ value != null ? value : '—' }}
            </template>

            <template #cell-seuilMax="{ value }">
                {{ value != null ? value : '—' }}
            </template>

            <template #cell-actif="{ item }">
                <span class="badge" :class="item.actif ? 'badge-actif' : 'badge-inactif'">
                    {{ item.actif ? 'Actif' : 'Inactif' }}
                </span>
            </template>

            <template #cell-actions="{ item }">
                <div class="d-flex justify-content-end gap-2 no-row-click">
                    <button type="button" class="table-action" title="Consulter" @click="versDetail(item)">
                        <i class="bi bi-eye"></i>
                    </button>

                    <button type="button" class="table-action" title="Modifier" :disabled="!item.actif"
                        @click="versEdition(item)">
                        <i class="bi bi-pencil"></i>
                    </button>

                    <button v-if="item.actif" type="button" class="table-action table-action--warning"
                        :title="`Désactiver ${item.code}`" @click="demanderBascule(item)">
                        <i class="bi bi-slash-circle"></i>
                    </button>

                    <button v-else type="button" class="table-action table-action--success"
                        :title="`Réactiver ${item.code}`" @click="demanderBascule(item)">
                        <i class="bi bi-arrow-clockwise"></i>
                    </button>
                </div>
            </template>
        </BaseTable>

        <BaseConfirm v-model="confirmOpen" :demande="confirmDemande" :loading="confirmLoading"
            @confirme="confirmerBascule" />
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
    grid-template-columns: minmax(180px, 220px) minmax(140px, 180px);
    gap: 1rem;
    align-items: end;
}

.bo-filters :deep(.base-select-container) {
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

.badge-type {
    display: inline-block;
    font-size: 0.72rem;
    font-weight: 600;
    padding: 0.15rem 0.6rem;
    border-radius: 20px;
    background: #e9e6fb;
    color: #4338a1;
    border: 1px solid #d5d0f3;
}

.text-muted-custom {
    color: var(--dts-muted);
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

.table-action:hover:not(:disabled) {
    background: var(--dts-blue-50);
    color: var(--dts-blue);
    border-color: #bdd3e6;
}

.table-action:disabled {
    opacity: 0.4;
    cursor: not-allowed;
}

.table-action--warning:hover:not(:disabled) {
    background: #fdf3e2;
    color: #9a6409;
    border-color: #f4e2c2;
}

.table-action--success:hover:not(:disabled) {
    background: #e6f4ea;
    color: #1e7e34;
    border-color: #c8e5cf;
}

@media (max-width: 576px) {
    .bo-page {
        padding: 1rem;
    }

    .bo-filters {
        grid-template-columns: 1fr;
    }
}
</style>
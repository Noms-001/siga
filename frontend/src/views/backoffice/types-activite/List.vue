<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseTable, BaseButton, BaseSelect, BaseConfirm }  from '@/components/base'
import type { TableColumn } from '@/components/base/BaseTable/BaseTable.types'
import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'
import {
    activerTypeActivite,
    desactiverTypeActivite,
    listerTypesActivite,
} from '@/services/typeActivite'
import { listerServices } from '@/services/service'
import type { TypeActivite } from '@/types/backoffice/typeActivite'
import type { Service } from '@/types/backoffice/service'

const router = useRouter()
const route = useRoute()

const items = ref<TypeActivite[]>([])
const services = ref<Service[]>([])
const loading = ref(false)
const errorMessage = ref('')
const flashMessage = ref('')

const statut = ref<'all' | 'true' | 'false'>('all')
const idService = ref<number | 'all'>('all')

const confirmOpen = ref(false)
const confirmLoading = ref(false)
const confirmDemande = ref<ConfirmDemande | null>(null)
const typeCible = ref<TypeActivite | null>(null)

const statutOptions = [
    { value: 'all', label: 'Tous' },
    { value: 'true', label: 'Actifs' },
    { value: 'false', label: 'Inactifs' },
]

const serviceOptions = computed(() => [
    { value: 'all', label: 'Tous les services' },
    ...services.value.map(s => ({
        value: s.id,
        label: `${s.departement.code} — ${s.nom}`,
    })),
])

const columns: TableColumn<TypeActivite>[] = [
    { key: 'designation', label: 'Désignation' },
    { key: 'description', label: 'Description' },
    { key: 'services', label: 'Services', align: 'center' },
    { key: 'actif', label: 'Statut', align: 'center' },
    { key: 'actions', label: 'Actions', align: 'text-end' },
]

async function chargerServices(): Promise<void> {
    const res = await listerServices({ inclureInactifs: true })
    if (res.success) services.value = res.data
}

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await listerTypesActivite({
        actif: statut.value === 'all' ? null : statut.value === 'true',
        idService: idService.value === 'all' ? undefined : Number(idService.value),
    })
    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    items.value = res.data
}

function versDetail(row: TypeActivite): void {
    router.push({ name: 'backoffice-types-activite-detail', params: { id: row.id } })
}

function versEdition(row: TypeActivite): void {
    router.push({ name: 'backoffice-types-activite-modifier', params: { id: row.id } })
}

function demanderBascule(row: TypeActivite): void {
    typeCible.value = row

    if (row.actif) {
        confirmDemande.value = {
            titre: "Désactiver le type d'activité ?",
            message: `Le type « ${row.designation} » sera désactivé.`,
            consequence: 'Il ne pourra plus être utilisé pour de nouvelles activités.',
            libelleConfirmer: 'Désactiver',
            ton: 'warning',
            icone: 'bi bi-slash-circle',
        }
    } else {
        confirmDemande.value = {
            titre: "Réactiver le type d'activité ?",
            message: `Le type « ${row.designation} » sera réactivé.`,
            consequence: 'Il redeviendra disponible.',
            libelleConfirmer: 'Réactiver',
            ton: 'primary',
            icone: 'bi bi-arrow-clockwise',
        }
    }

    confirmOpen.value = true
}

async function confirmerBascule(): Promise<void> {
    if (!typeCible.value) return

    const cible = typeCible.value
    const etaitActif = cible.actif

    confirmLoading.value = true
    const res = etaitActif
        ? await desactiverTypeActivite(cible.id)
        : await activerTypeActivite(cible.id)
    confirmLoading.value = false

    if (!res.success) {
        confirmOpen.value = false
        errorMessage.value = res.error
        return
    }

    flashMessage.value = etaitActif
        ? `Le type d'activité « ${cible.designation} » a été désactivé.`
        : `Le type d'activité « ${cible.designation} » a été réactivé.`

    confirmOpen.value = false
    typeCible.value = null
    await charger()
}

watch([statut, idService], charger)

onMounted(() => {
    if (route.query.created) flashMessage.value = "Type d'activité créé avec succès."
    else if (route.query.updated) flashMessage.value = "Type d'activité modifié avec succès."
    if (route.query.created || route.query.updated) router.replace({ query: {} })
    chargerServices()
    charger()
})
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">Types d'activité</h1>
                <p class="bo-page__subtitle">
                    Gestion des types d'activité et de leurs services
                </p>
            </div>
            <BaseButton icon="bi bi-plus-lg" @click="router.push({ name: 'backoffice-types-activite-nouveau' })">
                Ajouter un type d'activité
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
            <BaseSelect v-model="idService" label="Service" placeholder="Tous les services" size="sm"
                :options="serviceOptions" />
            <BaseSelect v-model="statut" label="Statut" placeholder="Tous" size="sm" :options="statutOptions" />
        </div>

        <BaseTable :items="items" :columns="columns" :loading="loading" searchable>
            <template #cell-description="{ value }">
                <span class="text-muted-custom">{{ value || '—' }}</span>
            </template>

            <template #cell-services="{ item }">
                <span class="badge-count" :class="{ 'badge-count--empty': item.services.length === 0 }">
                    {{ item.services.length }}
                </span>
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
                        :title="`Désactiver « ${item.designation} »`" @click="demanderBascule(item)">
                        <i class="bi bi-slash-circle"></i>
                    </button>

                    <button v-else type="button" class="table-action table-action--success"
                        :title="`Réactiver « ${item.designation} »`" @click="demanderBascule(item)">
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
    grid-template-columns: minmax(220px, 320px) minmax(160px, 200px);
    gap: 1rem;
    align-items: end;
}

.bo-filters :deep(.base-select-container) {
    gap: 0.35rem;
}

.text-muted-custom {
    color: var(--dts-muted);
}

.badge-count {
    display: inline-block;
    min-width: 28px;
    padding: 0.15rem 0.55rem;
    border-radius: 20px;
    background: #e4f0fb;
    color: #135d95;
    border: 1px solid #cde2f6;
    font-size: 0.72rem;
    font-weight: 700;
}

.badge-count--empty {
    background: #eef1f5;
    color: #5b6b7d;
    border-color: #dfe5ec;
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
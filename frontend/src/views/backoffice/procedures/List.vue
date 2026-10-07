<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseTable, BaseButton, BaseConfirm }  from '@/components/base' 
import type { TableColumn } from '@/components/base/BaseTable/BaseTable.types'
import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'
import {
    activerProcedure,
    desactiverProcedure,
    listerProcedures,
} from '@/services/procedure'
import type { Procedure } from '@/types/backoffice/procedure'

const router = useRouter()
const route = useRoute()

const items = ref<Procedure[]>([])
const loading = ref(false)
const errorMessage = ref('')
const flashMessage = ref('')
const inclureInactifs = ref(false)

const confirmOpen = ref(false)
const confirmLoading = ref(false)
const confirmDemande = ref<ConfirmDemande | null>(null)
const procedureCible = ref<Procedure | null>(null)

const columns: TableColumn<Procedure>[] = [
    { key: 'designation', label: 'Désignation' },
    { key: 'actif', label: 'Statut', align: 'center' },
    { key: 'actions', label: 'Actions', align: 'text-end' },
]

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await listerProcedures({ inclureInactifs: inclureInactifs.value })
    loading.value = false
    if (!res.success) { errorMessage.value = res.error; return }
    items.value = res.data
}

function versDetail(row: Procedure): void {
    router.push({ name: 'backoffice-procedures-detail', params: { id: row.id } })
}
function versEdition(row: Procedure): void {
    router.push({ name: 'backoffice-procedures-modifier', params: { id: row.id } })
}

function demanderBascule(row: Procedure): void {
    procedureCible.value = row

    if (row.actif) {
        confirmDemande.value = {
            titre: 'Désactiver la procédure ?',
            message: `La procédure « ${row.designation} » sera désactivée.`,
            consequence:
                "Elle ne pourra plus être utilisée comme procédure active pour de nouvelles activités.",
            libelleConfirmer: 'Désactiver',
            ton: 'warning',
            icone: 'bi bi-slash-circle',
        }
    } else {
        confirmDemande.value = {
            titre: 'Réactiver la procédure ?',
            message: `La procédure « ${row.designation} » sera réactivée.`,
            consequence: 'Elle redeviendra disponible.',
            libelleConfirmer: 'Réactiver',
            ton: 'primary',
            icone: 'bi bi-arrow-clockwise',
        }
    }

    confirmOpen.value = true
}

async function confirmerBascule(): Promise<void> {
    if (!procedureCible.value) return

    const cible = procedureCible.value
    const etaitActif = cible.actif

    confirmLoading.value = true
    const res = etaitActif
        ? await desactiverProcedure(cible.id)
        : await activerProcedure(cible.id)
    confirmLoading.value = false

    if (!res.success) {
        confirmOpen.value = false
        errorMessage.value = res.error
        return
    }

    flashMessage.value = etaitActif
        ? `La procédure « ${cible.designation} » a été désactivée.`
        : `La procédure « ${cible.designation} » a été réactivée.`

    confirmOpen.value = false
    procedureCible.value = null
    await charger()
}

watch(inclureInactifs, charger)

onMounted(() => {
    if (route.query.created) flashMessage.value = 'Procédure créée avec succès.'
    else if (route.query.updated) flashMessage.value = 'Procédure modifiée avec succès.'
    if (route.query.created || route.query.updated) router.replace({ query: {} })
    charger()
})
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">Procédures</h1>
                <p class="bo-page__subtitle">Gestion des procédures du système</p>
            </div>
            <BaseButton icon="bi bi-plus-lg" @click="router.push({ name: 'backoffice-procedures-nouveau' })">
                Ajouter une procédure
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
            <label class="bo-filters__check">
                <input type="checkbox" v-model="inclureInactifs">
                <span>Afficher les procédures désactivées</span>
            </label>
        </div>

        <BaseTable :items="items" :columns="columns" :loading="loading" searchable>

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
}
</style>
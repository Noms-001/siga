<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseTable, BaseButton, BaseConfirm } from '@/components/base'
import type { TableColumn } from '@/components/base/BaseTable/BaseTable.types'
import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'
import {
    activerRole,
    desactiverRole,
    listerRoles,
} from '@/services/role'
import type { Role } from '@/types/backoffice/role'

const router = useRouter()
const route = useRoute()

const items = ref<Role[]>([])
const loading = ref(false)
const errorMessage = ref('')
const flashMessage = ref('')

const inclureInactifs = ref(false)

const confirmOpen = ref(false)
const confirmLoading = ref(false)
const confirmDemande = ref<ConfirmDemande | null>(null)
const roleCible = ref<Role | null>(null)

const columns: TableColumn<Role>[] = [
    { key: 'code', label: 'Code' },
    { key: 'designation', label: 'Désignation' },
    { key: 'description', label: 'Description' },
    { key: 'actif', label: 'Statut', align: 'center' },
    { key: 'actions', label: 'Actions', align: 'text-end' },
]

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await listerRoles({ inclureInactifs: inclureInactifs.value })
    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    items.value = res.data
}

function versDetail(row: Role): void {
    router.push({ name: 'backoffice-roles-detail', params: { id: row.id } })
}

function versEdition(row: Role): void {
    router.push({ name: 'backoffice-roles-modifier', params: { id: row.id } })
}

function demanderBascule(row: Role): void {
    roleCible.value = row

    if (row.actif) {
        confirmDemande.value = {
            titre: 'Désactiver le rôle ?',
            message: `Le rôle « ${row.designation} » sera désactivé.`,
            consequence: 'Il ne pourra plus être affecté à de nouvelles activités ou plans d\'action.',
            libelleConfirmer: 'Désactiver',
            ton: 'warning',
            icone: 'bi bi-slash-circle',
        }
    } else {
        confirmDemande.value = {
            titre: 'Réactiver le rôle ?',
            message: `Le rôle « ${row.designation} » sera réactivé.`,
            consequence: 'Il redeviendra disponible pour les affectations.',
            libelleConfirmer: 'Réactiver',
            ton: 'primary',
            icone: 'bi bi-arrow-clockwise',
        }
    }

    confirmOpen.value = true
}

async function confirmerBascule(): Promise<void> {
    if (!roleCible.value) return

    const cible = roleCible.value
    const etaitActif = cible.actif

    confirmLoading.value = true
    const res = etaitActif
        ? await desactiverRole(cible.id)
        : await activerRole(cible.id)
    confirmLoading.value = false

    if (!res.success) {
        confirmOpen.value = false
        errorMessage.value = res.error
        return
    }

    flashMessage.value = etaitActif
        ? `Le rôle « ${cible.designation} » a été désactivé.`
        : `Le rôle « ${cible.designation} » a été réactivé.`

    confirmOpen.value = false
    roleCible.value = null
    await charger()
}

watch(inclureInactifs, charger)

onMounted(() => {
    if (route.query.created) flashMessage.value = 'Rôle créé avec succès.'
    else if (route.query.updated) flashMessage.value = 'Rôle modifié avec succès.'
    if (route.query.created || route.query.updated) router.replace({ query: {} })
    charger()
})
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">Rôles</h1>
                <p class="bo-page__subtitle">Gestion des rôles du système</p>
            </div>
            <BaseButton icon="bi bi-plus-lg" @click="router.push({ name: 'backoffice-roles-nouveau' })">
                Ajouter un rôle
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
                <span>Afficher les rôles désactivés</span>
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
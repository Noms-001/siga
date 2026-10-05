<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { 
    BaseTable, 
    BaseButton,  
    BaseSelect, 
    BaseConfirm 
} from '@/components/base'
import type { TableColumn } from '@/components/base/BaseTable/BaseTable.types'
import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'
import {
    activerService,
    desactiverService,
    listerServices,
} from '@/services/service'
import { listerDepartements } from '@/services/departement'
import type { Service } from '@/types/backoffice/service'
import type { Departement } from '@/types/backoffice/departement'

const router = useRouter()
const route = useRoute()

const items = ref<Service[]>([])
const departements = ref<Departement[]>([])
const loading = ref(false)
const errorMessage = ref('')
const flashMessage = ref('')

const inclureInactifs = ref(false)
const idDepartement = ref<number | ''>('')

const confirmOpen = ref(false)
const confirmLoading = ref(false)
const confirmDemande = ref<ConfirmDemande | null>(null)
const serviceCible = ref<Service | null>(null)

const departementOptions = computed(() =>
    departements.value.map(d => ({
        value: d.id,
        label: `${d.code} — ${d.nom}`,
    }))
)

const columns: TableColumn<Service>[] = [
    { key: 'nom', label: 'Nom' },
    { key: 'departement', label: 'Département' },
    { key: 'actif', label: 'Statut', align: 'center' },
    { key: 'actions', label: 'Actions', align: 'text-end' },
]

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await listerServices({
        inclureInactifs: inclureInactifs.value,
        idDepartement: idDepartement.value === '' ? undefined : Number(idDepartement.value),
    })
    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    items.value = res.data
}

async function chargerDepartements(): Promise<void> {
    const res = await listerDepartements()
    if (res.success) departements.value = res.data
}

function versDetail(row: Service): void {
    router.push({ name: 'backoffice-services-detail', params: { id: row.id } })
}

function versEdition(row: Service): void {
    router.push({ name: 'backoffice-services-modifier', params: { id: row.id } })
}

/**
 * Ouvre la confirmation adaptée au statut courant.
 *
 * Le bouton est le même dans la liste, mais la décision à confirmer est
 * opposée selon `actif`. Le texte doit donc suivre l'état, sans quoi
 * l'utilisateur cliquerait "Désactiver" sur un service déjà inactif.
 */
function demanderBascule(row: Service): void {
    serviceCible.value = row

    if (row.actif) {
        confirmDemande.value = {
            titre: 'Désactiver le service ?',
            message: `Le service « ${row.nom} » sera désactivé.`,
            consequence: 'Il ne pourra plus être utilisé comme service actif.',
            libelleConfirmer: 'Désactiver',
            ton: 'warning',
            icone: 'bi bi-slash-circle',
        }
    } else {
        confirmDemande.value = {
            titre: 'Réactiver le service ?',
            message: `Le service « ${row.nom} » sera réactivé.`,
            consequence: 'Il redeviendra disponible comme service actif.',
            libelleConfirmer: 'Réactiver',
            ton: 'primary',
            icone: 'bi bi-arrow-clockwise',
        }
    }

    confirmOpen.value = true
}

async function confirmerBascule(): Promise<void> {
    if (!serviceCible.value) return

    const cible = serviceCible.value
    const etaitActif = cible.actif

    confirmLoading.value = true
    const res = etaitActif
        ? await desactiverService(cible.id)
        : await activerService(cible.id)
    confirmLoading.value = false

    if (!res.success) {
        confirmOpen.value = false
        errorMessage.value = res.error
        return
    }

    flashMessage.value = etaitActif
        ? `Le service « ${cible.nom} » a été désactivé.`
        : `Le service « ${cible.nom} » a été réactivé.`

    confirmOpen.value = false
    serviceCible.value = null
    await charger()
}

watch([inclureInactifs, idDepartement], charger)

onMounted(() => {
    if (route.query.created) flashMessage.value = 'Service créé avec succès.'
    else if (route.query.updated) flashMessage.value = 'Service modifié avec succès.'
    if (route.query.created || route.query.updated) router.replace({ query: {} })
    chargerDepartements()
    charger()
})
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">Services</h1>
                <p class="bo-page__subtitle">Gestion des services du système</p>
            </div>
            <BaseButton icon="bi bi-plus-lg"
                @click="router.push({ name: 'backoffice-services-nouveau' })">
                Ajouter un service
            </BaseButton>
        </header>

        <div v-if="flashMessage" class="alert alert-success mb-0" role="status">
            {{ flashMessage }}
        </div>

        <div v-if="errorMessage" class="alert alert-danger mb-0 d-flex justify-content-between align-items-center" role="alert">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="charger">Réessayer</BaseButton>
        </div>

        <div class="bo-filters">
            <BaseSelect v-model="idDepartement" size="sm" placeholder="Tous les départements"
                :options="departementOptions" />
            <label class="bo-filters__check">
                <input type="checkbox" v-model="inclureInactifs">
                <span>Afficher les services désactivés</span>
            </label>
        </div>

        <BaseTable :items="items" :columns="columns" :loading="loading" searchable>
            <template #cell-departement="{ item }">
                <span class="text-nowrap">{{ item.departement.code }} — {{ item.departement.nom }}</span>
            </template>

            <template #cell-actif="{ item }">
                <span class="badge" :class="item.actif ? 'badge-actif' : 'badge-inactif'">
                    {{ item.actif ? 'Actif' : 'Inactif' }}
                </span>
            </template>

            <template #cell-actions="{ item }">
                <div class="d-flex justify-content-end gap-2 no-row-click">
                    <button type="button" class="table-action" title="Consulter"
                        @click="versDetail(item)">
                        <i class="bi bi-eye"></i>
                    </button>

                    <button type="button" class="table-action" title="Modifier"
                        :disabled="!item.actif" @click="versEdition(item)">
                        <i class="bi bi-pencil"></i>
                    </button>

                    <!-- Bascule unique : libellé, icône et ton suivent l'état -->
                    <button v-if="item.actif" type="button"
                        class="table-action table-action--warning"
                        :title="`Désactiver « ${item.nom} »`"
                        @click="demanderBascule(item)">
                        <i class="bi bi-slash-circle"></i>
                    </button>

                    <button v-else type="button"
                        class="table-action table-action--success"
                        :title="`Réactiver « ${item.nom} »`"
                        @click="demanderBascule(item)">
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
.bo-page { padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; }

.bo-page__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 1rem;
}

.bo-page__title { font-size: 1.4rem; font-weight: 700; color: var(--dts-navy); margin: 0; }
.bo-page__subtitle { color: var(--dts-muted); margin: 0; font-size: 0.85rem; }

.bo-filters {
    display: flex;
    flex-wrap: wrap;
    gap: 1rem;
    align-items: center;
}

.bo-filters :deep(.base-select-container) { width: 280px; gap: 0; }

.bo-filters__check {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.85rem;
    color: var(--dts-text-2);
    cursor: pointer;
    user-select: none;
}

.bo-filters__check input { width: 16px; height: 16px; accent-color: var(--dts-blue); }

.badge-actif {
    background: #e6f4ea; color: #1e7e34; border: 1px solid #c8e5cf;
    font-weight: 600; padding: 0.25rem 0.7rem; border-radius: 20px; font-size: 0.7rem;
}
.badge-inactif {
    background: #eef1f5; color: #5b6b7d; border: 1px solid #dfe5ec;
    font-weight: 600; padding: 0.25rem 0.7rem; border-radius: 20px; font-size: 0.7rem;
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

.table-action:disabled { opacity: 0.4; cursor: not-allowed; }

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
    .bo-page { padding: 1rem; }
    .bo-filters :deep(.base-select-container) { width: 100%; }
}
</style>
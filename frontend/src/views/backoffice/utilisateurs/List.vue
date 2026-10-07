<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseTable, BaseButton, BaseSelect, BaseConfirm } from '@/components/base'
import type { TableColumn } from '@/components/base/BaseTable/BaseTable.types'
import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'
import {
    activerUtilisateur,
    desactiverUtilisateur,
    listerUtilisateurs,
} from '@/services/utilisateur'
import { listerDepartements } from '@/services/departement'
import { listerServices } from '@/services/service'
import type { Utilisateur } from '@/types/backoffice/utilisateur'
import type { Departement } from '@/types/backoffice/departement'
import type { Service } from '@/types/backoffice/service'

const router = useRouter()
const route = useRoute()

const items = ref<Utilisateur[]>([])
const departements = ref<Departement[]>([])
const services = ref<Service[]>([])
const loading = ref(false)
const errorMessage = ref('')
const flashMessage = ref('')

const statut = ref<'all' | 'true' | 'false'>('all')
const idDepartement = ref<number | 'all'>('all')
const idService = ref<number | 'all'>('all')

const confirmOpen = ref(false)
const confirmLoading = ref(false)
const confirmDemande = ref<ConfirmDemande | null>(null)
const utilisateurCible = ref<Utilisateur | null>(null)

const statutOptions = [
    { value: 'all', label: 'Tous' },
    { value: 'true', label: 'Actifs' },
    { value: 'false', label: 'Inactifs' },
]

const departementOptions = computed(() => [
    { value: 'all', label: 'Tous les départements' },
    ...departements.value.map(d => ({ value: d.id, label: `${d.code} — ${d.nom}` })),
])

const serviceOptions = computed(() => {
    const filtered = idDepartement.value === 'all'
        ? services.value
        : services.value.filter(s => s.departement.id === Number(idDepartement.value))

    return [
        { value: 'all', label: 'Tous les services' },
        ...filtered.map(s => ({
            value: s.id,
            label: `${s.departement.code} — ${s.nom}`,
        })),
    ]
})

const columns: TableColumn<Utilisateur>[] = [
    { key: 'nom', label: 'Nom' },
    { key: 'prenom', label: 'Prénom' },
    { key: 'email', label: 'Email' },
    { key: 'departement', label: 'Département' },
    { key: 'service', label: 'Service' },
    { key: 'poste', label: 'Poste' },
    { key: 'actif', label: 'Statut', align: 'center' },
    { key: 'actions', label: 'Actions', align: 'text-end' },
]

async function chargerReferentiels(): Promise<void> {
    const [depsRes, servsRes] = await Promise.all([
        listerDepartements(),
        listerServices({ inclureInactifs: true }),
    ])
    if (depsRes.success) departements.value = depsRes.data
    if (servsRes.success) services.value = servsRes.data
}

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await listerUtilisateurs({
        actif: statut.value === 'all' ? null : statut.value === 'true',
        idDepartement: idDepartement.value === 'all' ? undefined : Number(idDepartement.value),
        idService: idService.value === 'all' ? undefined : Number(idService.value),
    })
    loading.value = false
    if (!res.success) { errorMessage.value = res.error; return }
    items.value = res.data
}

function versDetail(row: Utilisateur): void {
    router.push({ name: 'backoffice-utilisateurs-detail', params: { id: row.id } })
}

function demanderBascule(row: Utilisateur): void {
    utilisateurCible.value = row

    if (row.enAttenteActivation) {
        errorMessage.value =
            "Cet utilisateur doit d'abord activer son compte via le lien reçu par email."
        return
    }

    if (row.actif) {
        confirmDemande.value = {
            titre: "Désactiver l'utilisateur ?",
            message: `L'utilisateur « ${row.prenom} ${row.nom} » sera désactivé.`,
            consequence:
                "Il ne pourra plus se connecter. Ses données restent conservées.",
            libelleConfirmer: 'Désactiver',
            ton: 'warning',
            icone: 'bi bi-slash-circle',
        }
    } else {
        confirmDemande.value = {
            titre: "Réactiver l'utilisateur ?",
            message: `L'utilisateur « ${row.prenom} ${row.nom} » sera réactivé.`,
            consequence: 'Il pourra de nouveau se connecter.',
            libelleConfirmer: 'Réactiver',
            ton: 'primary',
            icone: 'bi bi-arrow-clockwise',
        }
    }

    confirmOpen.value = true
}

async function confirmerBascule(): Promise<void> {
    if (!utilisateurCible.value) return

    const cible = utilisateurCible.value
    const etaitActif = cible.actif

    confirmLoading.value = true
    const res = etaitActif
        ? await desactiverUtilisateur(cible.id)
        : await activerUtilisateur(cible.id)
    confirmLoading.value = false

    if (!res.success) {
        confirmOpen.value = false
        errorMessage.value = res.error
        return
    }

    flashMessage.value = etaitActif
        ? `L'utilisateur « ${cible.prenom} ${cible.nom} » a été désactivé.`
        : `L'utilisateur « ${cible.prenom} ${cible.nom} » a été réactivé.`

    confirmOpen.value = false
    utilisateurCible.value = null
    await charger()
}

/** Réinitialise les services quand on change de département. */
watch(idDepartement, () => {
    if (idService.value !== 'all') {
        const s = services.value.find(x => x.id === idService.value)
        if (!s || (idDepartement.value !== 'all' && s.departement.id !== Number(idDepartement.value))) {
            idService.value = 'all'
        }
    }
})

watch([statut, idDepartement, idService], charger)

onMounted(() => {
    if (route.query.created) {
        flashMessage.value =
            "Utilisateur créé. Un email d'activation lui a été envoyé."
    }
    if (route.query.created) router.replace({ query: {} })
    chargerReferentiels()
    charger()
})
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">Utilisateurs</h1>
                <p class="bo-page__subtitle">Gestion des comptes utilisateurs</p>
            </div>
            <BaseButton icon="bi bi-plus-lg" @click="router.push({ name: 'backoffice-utilisateurs-nouveau' })">
                Ajouter un utilisateur
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
            <BaseSelect v-model="idDepartement" label="Département" size="sm" placeholder="Tous"
                :options="departementOptions" />
            <BaseSelect v-model="idService" label="Service" size="sm" placeholder="Tous" :options="serviceOptions" />
            <BaseSelect v-model="statut" label="Statut" size="sm" placeholder="Tous" :options="statutOptions" />
        </div>

        <BaseTable :items="items" :columns="columns" :loading="loading" searchable>
            <template #cell-nom="{ item }">
                <strong>{{ item.nom }}</strong>
            </template>

            <template #cell-email="{ item }">
                <span class="text-code">{{ item.email }}</span>
            </template>

            <template #cell-departement="{ item }">
                <span v-if="item.departement" class="badge-dept">{{ item.departement.code }}</span>
                <span v-else class="text-muted-custom">—</span>
            </template>

            <template #cell-service="{ item }">
                <span v-if="item.service" class="text-nowrap">{{ item.service.nom }}</span>
                <span v-else class="text-muted-custom">—</span>
            </template>

            <template #cell-poste="{ item }">
                <span v-if="!item.poste" class="text-muted-custom">—</span>
                <span v-else>{{ item.poste.nom }}</span>
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

                    <button v-if="item.actif" type="button" class="table-action table-action--warning"
                        :title="`Désactiver ${item.prenom} ${item.nom}`" @click="demanderBascule(item)">
                        <i class="bi bi-slash-circle"></i>
                    </button>

                    <button v-else type="button" class="table-action table-action--success"
                        :title="`Réactiver ${item.prenom} ${item.nom}`" @click="demanderBascule(item)">
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
    grid-template-columns: repeat(3, minmax(200px, 1fr));
    gap: 1rem;
    align-items: end;
}

.bo-filters :deep(.base-select-container) {
    gap: 0.35rem;
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
}

.badge-dept {
    display: inline-block;
    font-size: 0.7rem;
    font-weight: 700;
    padding: 0.15rem 0.5rem;
    background: var(--dts-navy);
    color: #fff;
    border-radius: 4px;
    letter-spacing: 0.04em;
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

@media (max-width: 768px) {
    .bo-filters {
        grid-template-columns: 1fr 1fr;
    }
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
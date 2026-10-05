<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseButton, BaseInput, BaseSelect, BaseModal, BaseConfirm } from '@/components/base'
import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'
import { getPoste } from '@/services/poste'
import {
    affecterPermission,
    listerPermissionsDuPoste,
    modifierPorteePermission,
    retirerPermission,
} from '@/services/postePermission'
import { listerPermissions } from '@/services/permission'
import type { Poste, PermissionPourPoste } from '@/types/backoffice/poste'
import type { Permission, PorteePermission } from '@/types/backoffice/permission'
import { PORTEE_LABELS } from '@/types/backoffice/permission'

const route = useRoute()
const router = useRouter()
const idPoste = Number(route.params.id)

/* -------------------- état principal -------------------- */

const poste = ref<Poste | null>(null)
const permissions = ref<PermissionPourPoste[]>([])
const allPermissions = ref<Permission[]>([])
const loading = ref(false)
const errorMessage = ref('')
const flashMessage = ref('')

/* -------------------- modal affecter / modifier -------------------- */

const modalOpen = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const modalForm = ref<{
    idPermission: number | ''
    portee: PorteePermission | ''
}>({ idPermission: '', portee: '' })
const modalErrors = ref<Record<string, string>>({})
const modalGlobalError = ref('')
const modalLoading = ref(false)

/* -------------------- confirm retrait -------------------- */

const confirmOpen = ref(false)
const confirmLoading = ref(false)
const confirmDemande = ref<ConfirmDemande | null>(null)
const permissionCible = ref<PermissionPourPoste | null>(null)

/* -------------------- computed -------------------- */

const porteeOptions = [
    { value: 'UTILISATEUR', label: PORTEE_LABELS.UTILISATEUR },
    { value: 'SERVICE', label: PORTEE_LABELS.SERVICE },
    { value: 'DEPARTEMENT', label: PORTEE_LABELS.DEPARTEMENT },
    { value: 'TOUS', label: PORTEE_LABELS.TOUS },
]

/**
 * Ids déjà affectés — pour exclure ces permissions du sélecteur
 * en mode « affecter ». Évite un aller-retour backend pour un conflit
 * qu'on connaît déjà côté client.
 */
const idsDejaAffectes = computed(() =>
    new Set(permissions.value.map(p => p.idPermission))
)

const permissionsDisponibles = computed(() =>
    allPermissions.value
        .filter(p => p.actif && !idsDejaAffectes.value.has(p.id))
        .map(p => ({
            value: p.id,
            label: `${p.ressource} · ${p.designation}`,
        }))
)

const porteeHint = computed(() => {
    switch (modalForm.value.portee) {
        case 'UTILISATEUR':
            return "La permission s'applique aux entités dont l'utilisateur est propriétaire."
        case 'SERVICE':
            return "La permission s'applique aux activités du service auquel appartient l'utilisateur."
        case 'DEPARTEMENT':
            return "La permission s'applique aux activités du département auquel appartient l'utilisateur."
        case 'TOUS':
            return "La permission s'applique globalement, sans restriction de périmètre."
        default:
            return ''
    }
})

/* -------------------- chargement -------------------- */

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''

    const [posteRes, permsRes, allRes] = await Promise.all([
        getPoste(idPoste),
        listerPermissionsDuPoste(idPoste),
        listerPermissions({ actif: true }),
    ])

    loading.value = false

    if (!posteRes.success) {
        errorMessage.value = posteRes.error
        return
    }
    poste.value = posteRes.data

    if (!permsRes.success) {
        errorMessage.value = permsRes.error
        return
    }
    permissions.value = permsRes.data

    if (allRes.success) allPermissions.value = allRes.data
}

/* -------------------- modal : ouvrir -------------------- */

function ouvrirAffectation(): void {
    modalMode.value = 'create'
    modalForm.value = { idPermission: '', portee: 'SERVICE' }
    modalErrors.value = {}
    modalGlobalError.value = ''
    modalOpen.value = true
}

function ouvrirModification(row: PermissionPourPoste): void {
    modalMode.value = 'edit'
    modalForm.value = { idPermission: row.idPermission, portee: row.portee }
    modalErrors.value = {}
    modalGlobalError.value = ''
    modalOpen.value = true
}

/* -------------------- modal : soumettre -------------------- */

function validerModal(): boolean {
    modalErrors.value = {}

    if (modalMode.value === 'create') {
        if (modalForm.value.idPermission === '' || modalForm.value.idPermission === null) {
            modalErrors.value.idPermission = 'La permission est obligatoire'
        }
    }

    if (!modalForm.value.portee) {
        modalErrors.value.portee = 'La portée est obligatoire'
    }

    return Object.keys(modalErrors.value).length === 0
}

async function soumettreModal(): Promise<void> {
    modalGlobalError.value = ''
    if (!validerModal()) return

    modalLoading.value = true

    let res
    if (modalMode.value === 'create') {
        res = await affecterPermission(idPoste, {
            idPermission: Number(modalForm.value.idPermission),
            portee: modalForm.value.portee as PorteePermission,
        })
    } else {
        res = await modifierPorteePermission(
            idPoste,
            Number(modalForm.value.idPermission),
            { portee: modalForm.value.portee as PorteePermission }
        )
    }

    modalLoading.value = false

    if (!res.success) {
        // La règle métier est côté backend : on l'affiche ici si refusée.
        modalGlobalError.value = res.error
        return
    }

    flashMessage.value = modalMode.value === 'create'
        ? 'Permission affectée avec succès.'
        : 'Portée modifiée avec succès.'

    modalOpen.value = false
    await charger()
}

/* -------------------- retrait -------------------- */

function demanderRetrait(row: PermissionPourPoste): void {
    if (!poste.value) return
    permissionCible.value = row

    confirmDemande.value = {
        titre: 'Retirer la permission ?',
        message: `Voulez-vous retirer la permission « ${row.designation} » du poste « ${poste.value.nom} » ?`,
        consequence: 'Cette affectation sera définitivement supprimée.',
        libelleConfirmer: 'Retirer',
        ton: 'danger',
        icone: 'bi bi-trash',
    }

    confirmOpen.value = true
}

async function confirmerRetrait(): Promise<void> {
    if (!permissionCible.value) return

    confirmLoading.value = true
    const res = await retirerPermission(idPoste, permissionCible.value.idPermission)
    confirmLoading.value = false

    if (!res.success) {
        confirmOpen.value = false
        errorMessage.value = res.error
        return
    }

    flashMessage.value = `La permission « ${permissionCible.value.designation} » a été retirée.`
    confirmOpen.value = false
    permissionCible.value = null
    await charger()
}

onMounted(charger)
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <RouterLink :to="{ name: 'backoffice-postes' }" class="bo-page__back">
                    <i class="bi bi-arrow-left"></i>
                    Retour aux postes
                </RouterLink>

                <h1 class="bo-page__title">
                    {{ poste ? `${poste.nom}` : 'Poste' }} — Gestion des permissions
                </h1>
                <p class="bo-page__subtitle">
                    Permissions attribuées à ce poste
                </p>
            </div>
            <BaseButton icon="bi bi-plus-lg" :disabled="loading || !poste" @click="ouvrirAffectation">
                Affecter une permission
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

        <!-- Tableau -->
        <div v-if="loading" class="bo-table-wrapper">
            <div v-for="i in 4" :key="i" class="skeleton skeleton-row"></div>
        </div>

        <div v-else-if="permissions.length === 0" class="bo-empty">
            Aucune permission affectée à ce poste pour le moment.
        </div>

        <div v-else class="bo-table-wrapper">
            <table class="bo-table">
                <thead>
                    <tr>
                        <th>Permission</th>
                        <th>Ressource</th>
                        <th>Action</th>
                        <th>Portée</th>
                        <th class="text-end">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="p in permissions" :key="p.idPermission">
                        <td>{{ p.designation }}</td>
                        <td><span class="badge-ressource">{{ p.ressource }}</span></td>
                        <td><code class="text-action">{{ p.action }}</code></td>
                        <td>
                            <span class="badge-portee" :class="`portee-${p.portee.toLowerCase()}`">
                                {{ PORTEE_LABELS[p.portee] }}
                            </span>
                        </td>
                        <td class="text-end">
                            <div class="d-flex justify-content-end gap-2">
                                <button type="button" class="table-action" title="Modifier la portée"
                                    @click="ouvrirModification(p)">
                                    <i class="bi bi-pencil"></i>
                                </button>
                                <button type="button" class="table-action table-action--danger" title="Retirer"
                                    @click="demanderRetrait(p)">
                                    <i class="bi bi-trash"></i>
                                </button>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <!-- Modal affecter / modifier -->
        <BaseModal v-model="modalOpen"
            :title="modalMode === 'create' ? 'Affecter une permission' : 'Modifier la permission'" size="md">
            <div class="modal-form">
                <div v-if="modalGlobalError" class="alert alert-danger mb-0" role="alert">
                    {{ modalGlobalError }}
                </div>

                <BaseInput :model-value="poste?.nom ?? ''" label="Poste" disabled />

                <template v-if="modalMode === 'create'">
                    <BaseSelect v-model="modalForm.idPermission" label="Permission" required
                        placeholder="Sélectionner une permission" autocomplete :options="permissionsDisponibles"
                        :error="modalErrors.idPermission" :disabled="modalLoading" />
                </template>

                <template v-else>
                    <BaseInput :model-value="permissions.find(p => p.idPermission === modalForm.idPermission)?.designation ?? ''
                        " label="Permission" disabled />
                </template>

                <BaseSelect v-model="modalForm.portee" label="Portée" required placeholder="Sélectionner une portée"
                    :options="porteeOptions" :error="modalErrors.portee" :disabled="modalLoading" />

                <div v-if="porteeHint" class="modal-hint" role="note">
                    <strong>Description de la portée</strong>
                    <p>{{ porteeHint }}</p>
                </div>
            </div>

            <template #footer>
                <div class="d-flex justify-content-end gap-2">
                    <BaseButton variant="secondary" :disabled="modalLoading" @click="modalOpen = false">
                        Annuler
                    </BaseButton>
                    <BaseButton :loading="modalLoading" @click="soumettreModal">
                        {{ modalMode === 'create' ? 'Affecter' : 'Enregistrer' }}
                    </BaseButton>
                </div>
            </template>
        </BaseModal>

        <!-- Confirmation retrait -->
        <BaseConfirm v-model="confirmOpen" :demande="confirmDemande" :loading="confirmLoading"
            @confirme="confirmerRetrait" />
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

.bo-page__back {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    font-size: 0.82rem;
    color: var(--dts-muted);
    text-decoration: none;
    margin-bottom: 0.3rem;
}

.bo-page__back:hover {
    color: var(--dts-blue);
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

/* --- tableau --- */

.bo-table-wrapper {
    overflow-x: auto;
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius-sm);
    background: var(--dts-surface);
}

.bo-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.85rem;
}

.bo-table thead th {
    background: var(--dts-bg);
    padding: 0.65rem 0.9rem;
    text-align: left;
    font-size: 0.7rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--dts-muted);
    border-bottom: 1px solid var(--dts-border);
}

.bo-table tbody td {
    padding: 0.7rem 0.9rem;
    color: var(--dts-text);
    border-bottom: 1px solid var(--dts-border);
}

.bo-table tbody tr:last-child td {
    border-bottom: none;
}

.bo-table tbody tr:hover {
    background: var(--dts-blue-50);
}

.bo-empty {
    padding: 2rem 1rem;
    text-align: center;
    font-size: 0.9rem;
    color: var(--dts-muted);
    background: var(--dts-surface);
    border: 1px dashed var(--dts-border-2);
    border-radius: var(--dts-radius-sm);
}

/* --- badges --- */

.badge-ressource {
    display: inline-block;
    padding: 0.15rem 0.55rem;
    border-radius: 4px;
    background: #e4f0fb;
    color: #135d95;
    border: 1px solid #cde2f6;
    font-size: 0.72rem;
    font-weight: 600;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.text-action {
    font-size: 0.78rem;
    padding: 0.15rem 0.5rem;
    background: #eef1f5;
    color: #384a5e;
    border-radius: 4px;
}

.badge-portee {
    display: inline-block;
    font-size: 0.7rem;
    font-weight: 600;
    padding: 0.2rem 0.65rem;
    border-radius: 20px;
}

.portee-utilisateur {
    background: #e4f0fb;
    color: #135d95;
    border: 1px solid #cde2f6;
}

.portee-service {
    background: #e6f4ea;
    color: #1e7e34;
    border: 1px solid #c8e5cf;
}

.portee-departement {
    background: #fdf3e2;
    color: #9a6409;
    border: 1px solid #f4e2c2;
}

.portee-tous {
    background: #e9e6fb;
    color: #4338a1;
    border: 1px solid #d5d0f3;
}

/* --- actions --- */

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

.table-action--danger:hover {
    background: #fdecea;
    color: var(--dts-danger);
    border-color: #f6cfcc;
}

/* --- modal --- */

.modal-form {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.modal-hint {
    padding: 0.75rem 0.9rem;
    background: var(--dts-bg);
    border-left: 3px solid var(--dts-blue);
    border-radius: 6px;
    font-size: 0.8rem;
}

.modal-hint strong {
    display: block;
    font-size: 0.72rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--dts-muted);
    margin-bottom: 0.25rem;
}

.modal-hint p {
    margin: 0;
    color: var(--dts-text-2);
    line-height: 1.5;
}

/* --- skeleton --- */

.skeleton-row {
    height: 48px;
    margin: 0.4rem 0.9rem;
    border-radius: 6px;
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: sk 1.4s infinite;
}

@keyframes sk {
    from {
        background-position: 200% 0;
    }

    to {
        background-position: -200% 0;
    }
}

@media (max-width: 576px) {
    .bo-page {
        padding: 1rem;
    }
}
</style>
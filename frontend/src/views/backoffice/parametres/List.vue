<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { BaseTable, BaseButton, BaseSelect, BaseInput, BaseModal, BaseConfirm } from '@/components/base'
import type { TableColumn } from '@/components/base/BaseTable/BaseTable.types'
import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'
import {
    activerParametre,
    desactiverParametre,
    getParametreFiltres,
    listerParametres,
    modifierParametre,
} from '@/services/parametre'
import {
    formatTypeValeur,
    type Parametre,
} from '@/types/backoffice/parametre'

/* -------------------- état principal -------------------- */

const items = ref<Parametre[]>([])
const loading = ref(false)
const errorMessage = ref('')
const flashMessage = ref('')

const categoriesDisponibles = ref<string[]>([])

/** Filtres. Le select manipule des string pour rester homogène. */
const statut = ref<'all' | 'true' | 'false'>('all')
const categorie = ref<string>('all')

/* -------------------- modal modifier -------------------- */

const editModalOpen = ref(false)
const editLoading = ref(false)
const editGlobalError = ref('')
const editErrors = ref<Record<string, string>>({})
const parametreEnEdition = ref<Parametre | null>(null)

/** Valeur courante saisie dans le modal (string, quel que soit le type). */
const valeurSaisie = ref<string>('')
/** Cas particulier du booléen : stocké séparément pour piloter la checkbox. */
const valeurBooleenne = ref<boolean>(false)

/* -------------------- confirm bascule -------------------- */

const confirmOpen = ref(false)
const confirmLoading = ref(false)
const confirmDemande = ref<ConfirmDemande | null>(null)
const parametreCible = ref<Parametre | null>(null)

/* -------------------- computed -------------------- */

const statutOptions = [
    { value: 'all', label: 'Tous' },
    { value: 'true', label: 'Actifs' },
    { value: 'false', label: 'Inactifs' },
]

const categorieOptions = computed(() => [
    { value: 'all', label: 'Toutes les catégories' },
    ...categoriesDisponibles.value.map(c => ({ value: c, label: c })),
])

const columns: TableColumn<Parametre>[] = [
    { key: 'code', label: 'Code' },
    { key: 'designation', label: 'Désignation' },
    { key: 'valeur', label: 'Valeur' },
    { key: 'typeValeur', label: 'Type', align: 'center' },
    { key: 'categorie', label: 'Catégorie' },
    { key: 'modifiable', label: 'Modifiable', align: 'center' },
    { key: 'actif', label: 'Statut', align: 'center' },
    { key: 'actions', label: 'Actions', align: 'text-end' },
]

/**
 * Type d'input à utiliser selon le `type_valeur` du paramètre.
 *
 * Le type n'est pas deviné depuis la valeur : le backend a déjà fixé la
 * règle au niveau du schéma, c'est elle qui pilote l'interface.
 */
const inputTypeFor = (t: Parametre['typeValeur']) => {
    switch (t) {
        case 'INTEGER': return 'number'
        case 'DECIMAL': return 'number'
        case 'DATE': return 'date'
        case 'DATETIME': return 'datetime-local'
        case 'BOOLEAN': return 'checkbox'
        default: return 'text'
    }
}

/** Pour l'affichage du tableau : rend un booléen lisible. */
function afficherValeur(p: Parametre): string {
    if (p.typeValeur === 'BOOLEAN') {
        return p.valeur === 'true' ? 'Oui' : 'Non'
    }
    if (p.typeValeur === 'DATETIME' && p.valeur) {
        // Rendre la valeur lisible pour un humain sans changer la donnée
        const d = new Date(p.valeur)
        return Number.isNaN(d.getTime()) ? p.valeur : d.toLocaleString('fr-FR')
    }
    return p.valeur
}

/* -------------------- chargements -------------------- */

async function chargerCategories(): Promise<void> {
    const res = await getParametreFiltres()
    if (res.success) categoriesDisponibles.value = res.data.categories
}

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await listerParametres({
        actif: statut.value === 'all' ? null : statut.value === 'true',
        categorie: categorie.value === 'all' ? undefined : categorie.value,
    })
    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    items.value = res.data
}

/* -------------------- actions liste -------------------- */

/**
 * Ouvre le modal de modification.
 *
 * Refusé si le paramètre n'est pas modifiable : c'est une règle système,
 * autant l'annoncer tout de suite plutôt que d'attendre le refus backend.
 * Le backend reste la source de vérité, mais l'interface évite un
 * aller-retour inutile.
 */
function ouvrirEdition(p: Parametre): void {
    if (!p.modifiable) {
        errorMessage.value = "Ce paramètre n'est pas modifiable."
        return
    }
    if (!p.actif) {
        errorMessage.value = "Impossible de modifier un paramètre désactivé."
        return
    }

    parametreEnEdition.value = p
    editErrors.value = {}
    editGlobalError.value = ''

    if (p.typeValeur === 'BOOLEAN') {
        valeurBooleenne.value = p.valeur === 'true'
        valeurSaisie.value = ''
    } else if (p.typeValeur === 'DATETIME') {
        // L'input datetime-local refuse les secondes : on tronque à la minute.
        // La valeur backend reste intacte tant que l'utilisateur ne soumet pas.
        valeurSaisie.value = (p.valeur ?? '').slice(0, 16)
        valeurBooleenne.value = false
    } else {
        valeurSaisie.value = p.valeur ?? ''
        valeurBooleenne.value = false
    }

    editModalOpen.value = true
}

function fermerEdition(): void {
    editModalOpen.value = false
    parametreEnEdition.value = null
}

/** Validation locale cohérente avec celle du backend. */
function validerValeur(): boolean {
    editErrors.value = {}

    const p = parametreEnEdition.value
    if (!p) return false

    if (p.typeValeur === 'BOOLEAN') {
        return true
    }

    const v = valeurSaisie.value.trim()
    if (v === '') {
        editErrors.value.valeur = 'La valeur est obligatoire'
        return false
    }

    switch (p.typeValeur) {
        case 'INTEGER':
            if (!/^-?\d+$/.test(v)) {
                editErrors.value.valeur = 'Doit être un entier (sans décimales)'
                return false
            }
            break
        case 'DECIMAL':
            if (!/^-?\d+(\.\d+)?$/.test(v)) {
                editErrors.value.valeur = 'Doit être un nombre décimal'
                return false
            }
            break
        case 'DATE':
            if (!/^\d{4}-\d{2}-\d{2}$/.test(v)) {
                editErrors.value.valeur = 'Format attendu : AAAA-MM-JJ'
                return false
            }
            break
        case 'DATETIME':
            if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(v)) {
                editErrors.value.valeur = 'Format attendu : AAAA-MM-JJTHH:MM'
                return false
            }
            break
    }

    return true
}

async function enregistrerModification(): Promise<void> {
    if (!parametreEnEdition.value) return
    editGlobalError.value = ''
    if (!validerValeur()) return

    const p = parametreEnEdition.value
    const payload = {
        valeur: p.typeValeur === 'BOOLEAN'
            ? String(valeurBooleenne.value)
            : valeurSaisie.value.trim(),
    }

    editLoading.value = true
    const res = await modifierParametre(p.id, payload)
    editLoading.value = false

    if (!res.success) {
        editGlobalError.value = res.error
        return
    }

    flashMessage.value = `Le paramètre « ${p.code} » a été mis à jour.`
    editModalOpen.value = false
    parametreEnEdition.value = null
    await charger()
}

function demanderBascule(p: Parametre): void {
    parametreCible.value = p

    if (p.actif) {
        confirmDemande.value = {
            titre: 'Désactiver le paramètre ?',
            message: `Le paramètre « ${p.code} » sera désactivé.`,
            consequence:
                "Il ne sera plus pris en compte dans les traitements qui l'utilisent.",
            libelleConfirmer: 'Désactiver',
            ton: 'warning',
            icone: 'bi bi-slash-circle',
        }
    } else {
        confirmDemande.value = {
            titre: 'Réactiver le paramètre ?',
            message: `Le paramètre « ${p.code} » sera réactivé.`,
            consequence: 'Il redeviendra disponible pour les traitements.',
            libelleConfirmer: 'Réactiver',
            ton: 'primary',
            icone: 'bi bi-arrow-clockwise',
        }
    }

    confirmOpen.value = true
}

async function confirmerBascule(): Promise<void> {
    if (!parametreCible.value) return

    const cible = parametreCible.value
    const etaitActif = cible.actif

    confirmLoading.value = true
    const res = etaitActif
        ? await desactiverParametre(cible.id)
        : await activerParametre(cible.id)
    confirmLoading.value = false

    if (!res.success) {
        confirmOpen.value = false
        errorMessage.value = res.error
        return
    }

    flashMessage.value = etaitActif
        ? `Le paramètre « ${cible.code} » a été désactivé.`
        : `Le paramètre « ${cible.code} » a été réactivé.`

    confirmOpen.value = false
    parametreCible.value = null
    await charger()
}

/* -------------------- watch / lifecycle -------------------- */

watch([statut, categorie], charger)

onMounted(() => {
    chargerCategories()
    charger()
})
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">Paramètres</h1>
                <p class="bo-page__subtitle">
                    Consultation, modification et désactivation des paramètres système
                </p>
            </div>
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
            <BaseSelect v-model="categorie" label="Catégorie" placeholder="Toutes" size="sm"
                :options="categorieOptions" />
            <BaseSelect v-model="statut" label="Statut" placeholder="Tous" size="sm" :options="statutOptions" />
        </div>

        <BaseTable :items="items" :columns="columns" :loading="loading" searchable>
            <template #cell-code="{ item }">
                <code class="text-code">{{ item.code }}</code>
            </template>

            <template #cell-valeur="{ item }">
                <span class="text-valeur">{{ afficherValeur(item) }}</span>
            </template>

            <template #cell-typeValeur="{ item }">
                <span class="badge-type">{{ formatTypeValeur(item.typeValeur) }}</span>
            </template>

            <template #cell-categorie="{ value }">
                <span class="text-muted-custom">{{ value || '—' }}</span>
            </template>

            <template #cell-modifiable="{ item }">
                <span class="badge" :class="item.modifiable ? 'badge-oui' : 'badge-non'">
                    {{ item.modifiable ? 'Oui' : 'Non' }}
                </span>
            </template>

            <template #cell-actif="{ item }">
                <span class="badge" :class="item.actif ? 'badge-actif' : 'badge-inactif'">
                    {{ item.actif ? 'Actif' : 'Inactif' }}
                </span>
            </template>

            <template #cell-actions="{ item }">
                <div class="d-flex justify-content-end gap-2 no-row-click">
                    <button type="button" class="table-action" :title="item.modifiable && item.actif
                        ? 'Modifier la valeur'
                        : 'Paramètre non modifiable'" :disabled="!item.modifiable || !item.actif"
                        @click="ouvrirEdition(item)">
                        <i class="bi bi-pencil"></i>
                    </button>

                    <button v-if="item.actif" type="button" class="table-action table-action--warning"
                        :title="`Désactiver « ${item.code} »`" @click="demanderBascule(item)">
                        <i class="bi bi-slash-circle"></i>
                    </button>

                    <button v-else type="button" class="table-action table-action--success"
                        :title="`Réactiver « ${item.code} »`" @click="demanderBascule(item)">
                        <i class="bi bi-arrow-clockwise"></i>
                    </button>
                </div>
            </template>
        </BaseTable>

        <!-- Modal modifier la valeur -->
        <BaseModal v-model="editModalOpen" title="Modifier le paramètre" size="md" :loading="editLoading">
            <div v-if="parametreEnEdition" class="modal-form">
                <div v-if="editGlobalError" class="alert alert-danger mb-0" role="alert">
                    {{ editGlobalError }}
                </div>

                <BaseInput :model-value="parametreEnEdition.code" label="Code" disabled />

                <BaseInput :model-value="parametreEnEdition.designation" label="Désignation" disabled />

                <BaseInput :model-value="formatTypeValeur(parametreEnEdition.typeValeur)" label="Type de valeur"
                    disabled />

                <!-- Input adapté au type -->
                <BaseInput v-if="parametreEnEdition.typeValeur === 'BOOLEAN'" v-model="valeurBooleenne" type="checkbox"
                    label="Valeur" :disabled="editLoading" />

                <BaseInput v-else v-model="valeurSaisie" :type="inputTypeFor(parametreEnEdition.typeValeur)"
                    label="Valeur" required :error="editErrors.valeur" :disabled="editLoading" />

                <p v-if="parametreEnEdition.description" class="modal-note">
                    {{ parametreEnEdition.description }}
                </p>
            </div>

            <template #footer>
                <div class="d-flex justify-content-end gap-2">
                    <BaseButton variant="secondary" :disabled="editLoading" @click="fermerEdition">Annuler</BaseButton>
                    <BaseButton :loading="editLoading" @click="enregistrerModification">
                        Enregistrer
                    </BaseButton>
                </div>
            </template>
        </BaseModal>

        <!-- Confirmation bascule -->
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

.text-code {
    font-size: 0.78rem;
    padding: 0.15rem 0.5rem;
    background: #eef1f5;
    color: #384a5e;
    border-radius: 4px;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-weight: 600;
}

.text-valeur {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 0.8rem;
    color: var(--dts-text);
    background: #f6f8fa;
    border: 1px solid #e5eaef;
    padding: 0.15rem 0.5rem;
    border-radius: 4px;
    display: inline-block;
    max-width: 320px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.badge-type {
    display: inline-block;
    font-size: 0.72rem;
    font-weight: 600;
    padding: 0.15rem 0.6rem;
    background: #e9e6fb;
    color: #4338a1;
    border: 1px solid #d5d0f3;
    border-radius: 20px;
}

.badge-actif,
.badge-inactif,
.badge-oui,
.badge-non {
    font-weight: 600;
    padding: 0.25rem 0.7rem;
    border-radius: 20px;
    font-size: 0.7rem;
}

.badge-actif {
    background: #e6f4ea;
    color: #1e7e34;
    border: 1px solid #c8e5cf;
}

.badge-inactif {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
}

.badge-oui {
    background: #e4f0fb;
    color: #135d95;
    border: 1px solid #cde2f6;
}

.badge-non {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
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

/* --- modal --- */

.modal-form {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.modal-note {
    margin: 0;
    font-size: 0.82rem;
    color: var(--dts-muted);
    line-height: 1.5;
    padding: 0.6rem 0.75rem;
    background: var(--dts-bg);
    border-left: 3px solid var(--dts-blue);
    border-radius: 4px;
}

@media (max-width: 768px) {
    .bo-filters {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 576px) {
    .bo-page {
        padding: 1rem;
    }
}
</style>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseButton, BaseConfirm } from '@/components/base'
import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'
import {
    activerProcedure,
    desactiverProcedure,
    getProcedure,
} from '@/services/procedure'
import type { Procedure } from '@/types/backoffice/procedure'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)

const item = ref<Procedure | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const flashMessage = ref('')

const confirmOpen = ref(false)
const confirmLoading = ref(false)
const confirmDemande = ref<ConfirmDemande | null>(null)

const basculeLabel = computed(() => (item.value?.actif ? 'Désactiver' : 'Réactiver'))
const basculeIcon = computed(() => item.value?.actif ? 'bi bi-slash-circle' : 'bi bi-arrow-clockwise')
const basculeVariant = computed(() => item.value?.actif ? 'warning' : 'primary')

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await getProcedure(id)
    loading.value = false
    if (!res.success) { errorMessage.value = res.error; return }
    item.value = res.data
}

function demanderBascule(): void {
    if (!item.value) return
    confirmDemande.value = item.value.actif
        ? {
            titre: 'Désactiver la procédure ?',
            message: `La procédure « ${item.value.designation} » sera désactivée.`,
            consequence:
                'Elle ne pourra plus être utilisée pour de nouvelles activités.',
            libelleConfirmer: 'Désactiver',
            ton: 'warning',
            icone: 'bi bi-slash-circle',
        }
        : {
            titre: 'Réactiver la procédure ?',
            message: `La procédure « ${item.value.designation} » sera réactivée.`,
            consequence: 'Elle redeviendra disponible.',
            libelleConfirmer: 'Réactiver',
            ton: 'primary',
            icone: 'bi bi-arrow-clockwise',
        }
    confirmOpen.value = true
}

async function confirmerBascule(): Promise<void> {
    if (!item.value) return
    const etaitActif = item.value.actif
    confirmLoading.value = true
    const res = etaitActif
        ? await desactiverProcedure(item.value.id)
        : await activerProcedure(item.value.id)
    confirmLoading.value = false
    if (!res.success) { confirmOpen.value = false; errorMessage.value = res.error; return }
    flashMessage.value = etaitActif
        ? 'La procédure a été désactivée.'
        : 'La procédure a été réactivée.'
    confirmOpen.value = false
    await charger()
}



onMounted(charger)
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">Détail de la procédure</h1>
                <p class="bo-page__subtitle">Consultation des informations</p>
            </div>
            <div class="bo-page__actions">
                <BaseButton variant="secondary" icon="bi bi-arrow-left"
                    @click="router.push({ name: 'backoffice-procedures' })">
                    Retour
                </BaseButton>
                <BaseButton icon="bi bi-pencil" :disabled="!item || !item.actif"
                    @click="router.push({ name: 'backoffice-procedures-modifier', params: { id } })">
                    Modifier
                </BaseButton>
                <BaseButton :variant="basculeVariant" :icon="basculeIcon" :disabled="!item" @click="demanderBascule">
                    {{ basculeLabel }}
                </BaseButton>
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

        <div v-else-if="loading" class="bo-detail">
            <div class="skeleton"></div>
            <div class="skeleton short"></div>
            <div class="skeleton"></div>
        </div>

        <div v-else-if="item" class="bo-detail">
            <div class="bo-detail__row">
                <span class="bo-detail__label">Désignation</span>
                <span class="bo-detail__value">{{ item.designation }}</span>
            </div>
            <div class="bo-detail__row">
                <span class="bo-detail__label">Statut</span>
                <span class="bo-detail__value">
                    <span class="badge" :class="item.actif ? 'badge-actif' : 'badge-inactif'">
                        {{ item.actif ? 'Active' : 'Inactive' }}
                    </span>
                </span>
            </div>
            <div class="bo-detail__row">
                <span class="bo-detail__label">Description</span>
                <span class="bo-detail__value">{{ item.description || '—' }}</span>
            </div>
            <div v-if="item.dateCreation" class="bo-detail__row">
                <span class="bo-detail__label">Créée le</span>
                <span class="bo-detail__value">
                    {{ new Date(item.dateCreation).toLocaleString('fr-FR') }}
                </span>
            </div>
            <div v-if="item.dateModification" class="bo-detail__row">
                <span class="bo-detail__label">Modifiée le</span>
                <span class="bo-detail__value">
                    {{ new Date(item.dateModification).toLocaleString('fr-FR') }}
                </span>
            </div>
            <div v-if="item.dateDesactivation" class="bo-detail__row">
                <span class="bo-detail__label">Désactivée le</span>
                <span class="bo-detail__value">
                    {{ new Date(item.dateDesactivation).toLocaleString('fr-FR') }}
                </span>
            </div>
        </div>

        <section v-if="item" class="bo-section">
            <header class="bo-section__head">
                <h2 class="bo-section__title">Étapes de validation</h2>
                <span class="bo-section__count">
                    {{ item.etapes.length }} étape{{ item.etapes.length > 1 ? 's' : '' }}
                </span>
            </header>

            <div v-if="item.etapes.length === 0" class="bo-empty">
                Aucune étape de validation définie pour cette procédure.
            </div>

            <div v-else class="bo-table-wrapper">
                <table class="bo-table">
                    <thead>
                        <tr>
                            <th class="text-center">Niveau</th>
                            <th>Désignation</th>
                            <th>Décideurs</th>
                            <th class="text-center">Obligatoire</th>
                            <th class="text-center">Retour</th>
                            <th class="text-center">Statut</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="e in item.etapes" :key="e.id">
                            <td class="text-center">
                                <span class="badge-niveau">{{ e.niveau }}</span>
                            </td>
                            <td>
                                <strong>{{ e.designation }}</strong>
                                <p v-if="e.description" class="text-muted-custom mb-0 small">
                                    {{ e.description }}
                                </p>
                            </td>
                            <td>
                                <span v-if="e.decideurs.length === 0" class="text-muted-custom">
                                    Aucun décideur
                                </span>
                                <div v-else class="chips">
                                    <span v-for="d in e.decideurs" :key="d.id" class="chip">
                                        <i :class="d.isMetier ? 'bi bi-briefcase' : 'bi bi-tools'"></i>
                                        {{ d.nom }}
                                    </span>
                                </div>
                            </td>
                            <td class="text-center">
                                <span class="badge" :class="e.obligatoire ? 'badge-oui' : 'badge-non'">
                                    {{ e.obligatoire ? 'Oui' : 'Non' }}
                                </span>
                            </td>
                            <td class="text-center">
                                <span class="badge" :class="e.retour ? 'badge-oui' : 'badge-non'">
                                    {{ e.retour ? 'Oui' : 'Non' }}
                                </span>
                            </td>
                            <td class="text-center">
                                <span class="badge" :class="e.actif ? 'badge-actif' : 'badge-inactif'">
                                    {{ e.actif ? 'Active' : 'Inactive' }}
                                </span>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </section>

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

.bo-page__actions {
    display: flex;
    gap: 0.5rem;
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

.bo-detail {
    max-width: 720px;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    padding: 1.25rem 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.bo-detail__row {
    display: grid;
    grid-template-columns: 160px 1fr;
    gap: 1rem;
    padding-bottom: 0.65rem;
    border-bottom: 1px solid var(--dts-border);
}

.bo-detail__row:last-child {
    border-bottom: none;
    padding-bottom: 0;
}

.bo-detail__label {
    font-size: 0.78rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--dts-muted);
    padding-top: 0.15rem;
}

.bo-detail__value {
    color: var(--dts-text);
    word-break: break-word;
}

.bo-section {
    max-width: 1000px;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    padding: 1.25rem 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.bo-section__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;
    flex-wrap: wrap;
}

.bo-section__title {
    margin: 0;
    font-size: 1rem;
    font-weight: 700;
    color: var(--dts-navy);
}

.bo-section__count {
    font-size: 0.75rem;
    font-weight: 600;
    background: var(--dts-blue-50);
    color: var(--dts-blue);
    padding: 0.15rem 0.6rem;
    border-radius: 20px;
    border: 1px solid var(--dts-border);
}

.bo-empty {
    padding: 1rem;
    text-align: center;
    font-size: 0.85rem;
    color: var(--dts-muted);
    background: var(--dts-bg);
    border: 1px dashed var(--dts-border-2);
    border-radius: var(--dts-radius-sm);
}

.bo-table-wrapper {
    overflow-x: auto;
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius-sm);
}

.bo-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.85rem;
}

.bo-table thead th {
    background: var(--dts-bg);
    padding: 0.6rem 0.9rem;
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
    vertical-align: top;
}

.bo-table tbody tr:last-child td {
    border-bottom: none;
}

.bo-table tbody tr:hover {
    background: var(--dts-blue-50);
}

.badge-niveau {
    display: inline-block;
    min-width: 30px;
    padding: 0.2rem 0.55rem;
    background: var(--dts-navy);
    color: #fff;
    border-radius: 20px;
    font-size: 0.72rem;
    font-weight: 700;
    text-align: center;
}

.chips {
    display: flex;
    flex-wrap: wrap;
    gap: 0.35rem;
}

.chip {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.2rem 0.6rem;
    background: var(--dts-bg);
    color: var(--dts-text);
    border: 1px solid var(--dts-border);
    border-radius: 20px;
    font-size: 0.75rem;
}

.chip i {
    color: var(--dts-blue);
    font-size: 0.75rem;
}

.badge-oui {
    background: #e4f0fb;
    color: #135d95;
    border: 1px solid #cde2f6;
    font-weight: 600;
    padding: 0.2rem 0.6rem;
    border-radius: 20px;
    font-size: 0.7rem;
}

.badge-non {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
    font-weight: 600;
    padding: 0.2rem 0.6rem;
    border-radius: 20px;
    font-size: 0.7rem;
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

.skeleton {
    height: 16px;
    border-radius: 6px;
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: sk 1.4s infinite;
}

.skeleton.short {
    width: 40%;
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

    .bo-detail__row {
        grid-template-columns: 1fr;
        gap: 0.25rem;
    }
}
</style>
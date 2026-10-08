<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseButton, BaseConfirm } from '@/components/base'
import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'
import {
    activerIndicateur,
    desactiverIndicateur,
    getIndicateur,
} from '@/services/indicateur'
import type { Indicateur } from '@/types/backoffice/indicateur'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)

const item = ref<Indicateur | null>(null)
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
    const res = await getIndicateur(id)
    loading.value = false
    if (!res.success) { errorMessage.value = res.error; return }
    item.value = res.data
}

function demanderBascule(): void {
    if (!item.value) return
    confirmDemande.value = item.value.actif
        ? {
            titre: "Désactiver l'indicateur ?",
            message: `L'indicateur « ${item.value.code} » sera désactivé.`,
            consequence: "Il ne pourra plus être associé à de nouvelles activités.",
            libelleConfirmer: 'Désactiver',
            ton: 'warning',
            icone: 'bi bi-slash-circle',
        }
        : {
            titre: "Réactiver l'indicateur ?",
            message: `L'indicateur « ${item.value.code} » sera réactivé.`,
            consequence: 'Il redeviendra disponible.',
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
        ? await desactiverIndicateur(item.value.id)
        : await activerIndicateur(item.value.id)
    confirmLoading.value = false
    if (!res.success) { confirmOpen.value = false; errorMessage.value = res.error; return }
    flashMessage.value = etaitActif
        ? "L'indicateur a été désactivé."
        : "L'indicateur a été réactivé."
    confirmOpen.value = false
    await charger()
}

function v(value: unknown): string {
    return value == null || value === '' ? '—' : String(value)
}

onMounted(charger)
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">Détail de l'indicateur</h1>
                <p class="bo-page__subtitle">Consultation des informations</p>
            </div>
            <div class="bo-page__actions">
                <BaseButton variant="secondary" icon="bi bi-arrow-left"
                    @click="router.push({ name: 'backoffice-indicateurs' })">
                    Retour
                </BaseButton>
                <BaseButton icon="bi bi-pencil" :disabled="!item || !item.actif"
                    @click="router.push({ name: 'backoffice-indicateurs-modifier', params: { id } })">
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

        <template v-else-if="item">
            <section class="bo-detail">
                <header class="bo-detail__section-head">Informations générales</header>
                <div class="bo-detail__row">
                    <span class="bo-detail__label">Code</span>
                    <span class="bo-detail__value"><code class="text-code">{{ item.code }}</code></span>
                </div>
                <div class="bo-detail__row">
                    <span class="bo-detail__label">Désignation</span>
                    <span class="bo-detail__value">{{ item.designation }}</span>
                </div>
                <div class="bo-detail__row">
                    <span class="bo-detail__label">Type</span>
                    <span class="bo-detail__value">
                        <span v-if="item.typeIndicateur" class="badge-type">{{ item.typeIndicateur }}</span>
                        <span v-else class="text-muted-custom">—</span>
                    </span>
                </div>
                <div class="bo-detail__row">
                    <span class="bo-detail__label">Statut</span>
                    <span class="bo-detail__value">
                        <span class="badge" :class="item.actif ? 'badge-actif' : 'badge-inactif'">
                            {{ item.actif ? 'Actif' : 'Inactif' }}
                        </span>
                    </span>
                </div>
            </section>

            <section class="bo-detail">
                <header class="bo-detail__section-head">Référentiel HOPEx</header>
                <div class="bo-detail__row">
                    <span class="bo-detail__label">Code HOPEx</span>
                    <span class="bo-detail__value">{{ v(item.codeHopex) }}</span>
                </div>
                <div class="bo-detail__row">
                    <span class="bo-detail__label">Indicateur HOPEx</span>
                    <span class="bo-detail__value">{{ v(item.indicateurHopex) }}</span>
                </div>
            </section>

            <section class="bo-detail">
                <header class="bo-detail__section-head">Mesure</header>
                <div class="bo-detail__row">
                    <span class="bo-detail__label">Unité</span>
                    <span class="bo-detail__value">{{ v(item.uniteMesure) }}</span>
                </div>
                <div class="bo-detail__row">
                    <span class="bo-detail__label">Valeur cible</span>
                    <span class="bo-detail__value">{{ v(item.valeurCible) }}</span>
                </div>
                <div class="bo-detail__row">
                    <span class="bo-detail__label">Seuil minimum</span>
                    <span class="bo-detail__value">{{ v(item.seuilMin) }}</span>
                </div>
                <div class="bo-detail__row">
                    <span class="bo-detail__label">Seuil maximum</span>
                    <span class="bo-detail__value">{{ v(item.seuilMax) }}</span>
                </div>
            </section>

            <section class="bo-detail">
                <header class="bo-detail__section-head">Fréquences</header>
                <div class="bo-detail__row">
                    <span class="bo-detail__label">Vérification</span>
                    <span class="bo-detail__value">{{ v(item.frequenceVerification) }}</span>
                </div>
                <div class="bo-detail__row">
                    <span class="bo-detail__label">Agrégation</span>
                    <span class="bo-detail__value">{{ v(item.frequenceAggregation) }}</span>
                </div>
            </section>

            <section class="bo-detail">
                <header class="bo-detail__section-head">Définition</header>
                <div class="bo-detail__row bo-detail__row--block">
                    <span class="bo-detail__label">Définition</span>
                    <span class="bo-detail__value">{{ v(item.definition) }}</span>
                </div>
                <div class="bo-detail__row bo-detail__row--block">
                    <span class="bo-detail__label">Méthode de détermination</span>
                    <span class="bo-detail__value">{{ v(item.methodeDetermination) }}</span>
                </div>
                <div class="bo-detail__row bo-detail__row--block">
                    <span class="bo-detail__label">Objectif</span>
                    <span class="bo-detail__value">{{ v(item.objectif) }}</span>
                </div>
            </section>
        </template>

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
    max-width: 860px;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    padding: 1rem 1.5rem 1.25rem;
    display: flex;
    flex-direction: column;
    gap: 0.65rem;
}

.bo-detail__section-head {
    font-size: 0.72rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: var(--dts-muted);
    padding-bottom: 0.4rem;
    border-bottom: 1px solid var(--dts-border);
}

.bo-detail__row {
    display: grid;
    grid-template-columns: 200px 1fr;
    gap: 1rem;
    padding-bottom: 0.5rem;
    border-bottom: 1px dashed var(--dts-border);
}

.bo-detail__row:last-child {
    border-bottom: none;
    padding-bottom: 0;
}

.bo-detail__row--block {
    grid-template-columns: 200px 1fr;
}

.bo-detail__label {
    font-size: 0.78rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--dts-muted);
    padding-top: 0.15rem;
}

.bo-detail__value {
    color: var(--dts-text);
    word-break: break-word;
    white-space: pre-wrap;
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

.text-muted-custom {
    color: var(--dts-muted);
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
        gap: 0.15rem;
    }

    .bo-detail {
        padding: 1rem;
    }
}
</style>
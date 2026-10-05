<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseButton } from '@/components/base'
import { getDepartement } from '@/services/departement'
import type { Departement } from '@/types/backoffice/departement'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)

const item = ref<Departement | null>(null)
const loading = ref(false)
const errorMessage = ref('')

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await getDepartement(id)
    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    item.value = res.data
}

onMounted(charger)
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">Détail du département</h1>
                <p class="bo-page__subtitle">Consultation des informations</p>
            </div>
            <div class="bo-page__actions">
                <BaseButton variant="secondary" icon="bi bi-arrow-left"
                    @click="router.push({ name: 'backoffice-departements' })">
                    Retour
                </BaseButton>
                <BaseButton icon="bi bi-pencil"
                    :disabled="!item"
                    @click="router.push({ name: 'backoffice-departements-modifier', params: { id } })">
                    Modifier
                </BaseButton>
            </div>
        </header>

        <div v-if="errorMessage" class="alert alert-danger mb-0 d-flex justify-content-between align-items-center" role="alert">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="charger">Réessayer</BaseButton>
        </div>

        <div v-else-if="loading" class="bo-detail bo-detail--loading">
            <div class="skeleton skeleton-line"></div>
            <div class="skeleton skeleton-line short"></div>
            <div class="skeleton skeleton-line"></div>
        </div>

        <div v-else-if="item" class="bo-detail">
            <div class="bo-detail__row">
                <span class="bo-detail__label">Code</span>
                <span class="bo-detail__value">{{ item.code }}</span>
            </div>
            <div class="bo-detail__row">
                <span class="bo-detail__label">Nom</span>
                <span class="bo-detail__value">{{ item.nom }}</span>
            </div>
            <div class="bo-detail__row">
                <span class="bo-detail__label">Description</span>
                <span class="bo-detail__value">{{ item.description || '—' }}</span>
            </div>
        </div>
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

.bo-page__actions { display: flex; gap: 0.5rem; }

.bo-page__title { font-size: 1.4rem; font-weight: 700; color: var(--dts-navy); margin: 0; }
.bo-page__subtitle { color: var(--dts-muted); margin: 0; font-size: 0.85rem; }

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
    grid-template-columns: 140px 1fr;
    gap: 1rem;
    padding-bottom: 0.65rem;
    border-bottom: 1px solid var(--dts-border);
}

.bo-detail__row:last-child { border-bottom: none; padding-bottom: 0; }

.bo-detail__label {
    font-size: 0.78rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--dts-muted);
    padding-top: 0.15rem;
}

.bo-detail__value { color: var(--dts-text); word-break: break-word; }

.bo-detail--loading .skeleton-line {
    height: 16px;
    border-radius: 6px;
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: sk 1.4s infinite;
}

.bo-detail--loading .short { width: 40%; }

@keyframes sk { from { background-position: 200% 0; } to { background-position: -200% 0; } }

@media (max-width: 576px) {
    .bo-page { padding: 1rem; }
    .bo-detail__row { grid-template-columns: 1fr; gap: 0.25rem; }
}
</style>
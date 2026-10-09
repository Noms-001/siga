<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseButton from '@/components/base/BaseButton/BaseButton.vue'
import { getSignalement } from '@/services/signalement'
import type { Signalement, SignalementDetail } from '@/types/backoffice/signalement'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)

const loading = ref(false)
const errorMessage = ref('')

const item = ref<SignalementDetail | null>(null)

console.log(item)

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await getSignalement(id)
    loading.value = false
    if (!res.success) { errorMessage.value = res.error; return }
    item.value = res.data
}

const classeType = computed(() => {
    const t = (item.value?.typeOrigine ?? '').toUpperCase()
    if (t === 'INCIDENT') return 'badge-type--incident'
    if (t === 'RISQUE') return 'badge-type--risque'
    return 'badge-type--autre'
})

const declarant = computed(() => {
    const i = item.value
    if (!i) return '—'
    const parts = [i.prenomUtilisateur, i.nomUtilisateur].filter(Boolean)
    return parts.length ? parts.join(' ') : '—'
})

function formaterDateHeure(iso: string | null | undefined): string {
    if (!iso) return '—'
    const d = new Date(iso)
    return Number.isNaN(d.getTime()) ? iso : d.toLocaleString('fr-FR')
}

function v(value: unknown): string {
    if (value === null || value === undefined) return '—'
    const s = String(value).trim()
    return s === '' ? '—' : s
}

onMounted(charger)
</script>

<template>
    <div class="sig-page">
        <header class="sig-header">
            <div>
                <h1 class="sig-header__title">Détail du signalement</h1>
                <p class="sig-header__subtitle">Consultation</p>
            </div>
            <div class="sig-header__actions">
                <BaseButton variant="secondary" icon="bi bi-arrow-left" @click="router.push({ name: 'signalements' })">
                    Retour
                </BaseButton>
            </div>
        </header>

        <div v-if="errorMessage" class="alert alert-danger mb-0 d-flex justify-content-between align-items-center"
            role="alert">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="charger">Réessayer</BaseButton>
        </div>

        <div v-else-if="loading" class="sig-loading">
            <div class="sig-skeleton"></div>
            <div class="sig-skeleton sig-skeleton--short"></div>
            <div class="sig-skeleton"></div>
        </div>

        <template v-else-if="item">
            <section class="sig-section">
                <header class="sig-section__head">Identification</header>
                <div class="sig-section__body">
                    <div class="sig-row">
                        <span class="sig-row__label">Code</span>
                        <span class="sig-row__value">
                            <code class="text-code">{{ item.code }}</code>
                        </span>
                    </div>
                    <div class="sig-row">
                        <span class="sig-row__label">Désignation</span>
                        <span class="sig-row__value">{{ item.designation }}</span>
                    </div>
                    <div class="sig-row">
                        <span class="sig-row__label">Type</span>
                        <span class="sig-row__value">
                            <span class="badge-type" :class="classeType">
                                {{ item.typeOrigine }}
                            </span>
                        </span>
                    </div>
                    <div class="sig-row">
                        <span class="sig-row__label">Déclaré par</span>
                        <span class="sig-row__value">{{ declarant }}</span>
                    </div>
                    <div class="sig-row">
                        <span class="sig-row__label">Date de création</span>
                        <span class="sig-row__value">{{ formaterDateHeure(item.dateCreation) }}</span>
                    </div>
                </div>
            </section>

            <section class="sig-section">
                <header class="sig-section__head">Description</header>
                <div class="sig-section__body">
                    <div class="sig-row sig-row--block">
                        <span class="sig-row__value sig-row__value--pre">
                            {{ v(item.description) }}
                        </span>
                    </div>
                </div>
            </section>
            <section class="sig-section">
                <header class="sig-section__head">
                    Plan d'action rattaché
                    <span v-if="item.plansAction.length > 0" class="sig-section__count">
                        {{ item.plansAction.length }}
                    </span>
                </header>

                <div class="sig-section__body">
                    <div v-if="item.plansAction.length === 0" class="sig-empty">
                        <i class="bi bi-info-circle"></i>
                        Aucun plan d'action rattaché
                    </div>

                    <div v-else class="sig-plans">
                        <div v-for="p in item.plansAction" :key="p.id" class="sig-plan">
                            <div class="sig-plan__main">
                                <code class="sig-plan__code">{{ p.code }}</code>
                                <span class="sig-plan__designation">{{ p.designation }}</span>
                            </div>
                            <span v-if="p.estPrincipale" class="sig-plan__badge">
                                Principale
                            </span>
                        </div>
                    </div>
                </div>
            </section>
        </template>
    </div>
</template>

<style scoped>
.sig-page {
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
}

.sig-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 1rem;
}

.sig-header__title {
    font-size: 1.4rem;
    font-weight: 700;
    color: var(--dts-navy);
    margin: 0;
}

.sig-header__subtitle {
    color: var(--dts-muted);
    margin: 0.15rem 0 0;
    font-size: 0.85rem;
}

.sig-header__actions {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
}

.sig-section {
    max-width: 820px;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    overflow: hidden;
}

.sig-section__head {
    padding: 0.7rem 1.25rem;
    background: var(--dts-bg);
    border-bottom: 1px solid var(--dts-border);
    font-size: 0.72rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: var(--dts-muted);
}

.sig-section__body {
    padding: 0.75rem 1.25rem 1rem;
    display: flex;
    flex-direction: column;
}

.sig-row {
    display: grid;
    grid-template-columns: 200px 1fr;
    gap: 1rem;
    padding: 0.6rem 0;
    border-bottom: 1px dashed var(--dts-border);
    align-items: baseline;
}

.sig-row:last-child {
    border-bottom: none;
    padding-bottom: 0;
}

.sig-row--block {
    grid-template-columns: 1fr;
}

.sig-row__label {
    font-size: 0.78rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--dts-muted);
}

.sig-row__value {
    color: var(--dts-text);
    font-size: 0.9rem;
    word-break: break-word;
}

.sig-row__value--pre {
    white-space: pre-wrap;
    line-height: 1.55;
    font-size: 0.88rem;
}

.text-code {
    display: inline-block;
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
    font-weight: 700;
    padding: 0.15rem 0.6rem;
    border-radius: 20px;
    letter-spacing: 0.04em;
}

.badge-type--incident {
    background: #fdf3e2;
    color: #9a6409;
    border: 1px solid #f4e2c2;
}

.badge-type--risque {
    background: #fdecea;
    color: #a61b16;
    border: 1px solid #f6cfcc;
}

.badge-type--autre {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
}

.sig-loading {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    max-width: 820px;
}

.sig-skeleton {
    height: 120px;
    border-radius: var(--dts-radius);
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: sigSk 1.4s infinite;
}

.sig-skeleton--short {
    height: 60px;
}

/* --- section compteur --- */

.sig-section__count {
    display: inline-block;
    margin-left: 0.5rem;
    padding: 0.1rem 0.5rem;
    border-radius: 20px;
    background: var(--dts-blue-50);
    color: var(--dts-blue);
    font-size: 0.7rem;
    font-weight: 700;
    text-transform: none;
    letter-spacing: 0;
}

/* --- état vide --- */

.sig-empty {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.9rem 1rem;
    background: var(--dts-bg);
    border: 1px dashed var(--dts-border-2);
    border-radius: var(--dts-radius-sm);
    font-size: 0.85rem;
    color: var(--dts-muted);
    font-style: italic;
}

.sig-empty i {
    color: var(--dts-muted);
    font-size: 1rem;
    font-style: normal;
}

/* --- liste des plans --- */

.sig-plans {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.sig-plan {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.7rem 0.95rem;
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius-sm);
    background: var(--dts-surface);
    text-decoration: none;
    color: inherit;
    transition: border-color 0.15s ease, background-color 0.15s ease;
}

.sig-plan:hover {
    border-color: #bdd3e6;
    background: var(--dts-blue-50);
}

.sig-plan__main {
    display: flex;
    align-items: baseline;
    gap: 0.6rem;
    min-width: 0;
    flex: 1;
}

.sig-plan__code {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.78rem;
    font-weight: 700;
    color: var(--dts-blue);
    white-space: nowrap;
}

.sig-plan__designation {
    font-size: 0.88rem;
    font-weight: 500;
    color: var(--dts-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.sig-plan__badge {
    display: inline-block;
    padding: 0.15rem 0.55rem;
    border-radius: 20px;
    background: #e4f0fb;
    color: #135d95;
    border: 1px solid #cde2f6;
    font-size: 0.68rem;
    font-weight: 700;
    letter-spacing: 0.03em;
    text-transform: uppercase;
    white-space: nowrap;
}

.sig-plan__arrow {
    color: var(--dts-muted);
    font-size: 0.85rem;
    flex-shrink: 0;
}

.sig-plan:hover .sig-plan__arrow {
    color: var(--dts-blue);
}

@keyframes sigSk {
    from {
        background-position: 200% 0;
    }

    to {
        background-position: -200% 0;
    }
}

@media (max-width: 576px) {
    .sig-page {
        padding: 1rem;
    }

    .sig-row {
        grid-template-columns: 1fr;
        gap: 0.15rem;
    }
}
</style>
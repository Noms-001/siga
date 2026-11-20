<script setup lang="ts">
import { computed } from 'vue'
import BaseSelect from '@/components/base/BaseSelect/BaseSelect.vue'
import type { DashboardOption, DashboardTypeActivite } from '@/types/dashboard'

const props = defineProps<{
    types: DashboardTypeActivite[]
    loading?: boolean
    services: DashboardOption[]
    serviceId: number
    filtreServiceVisible?: boolean
}>()

const emit = defineEmits<{
    (e: 'update:serviceId', value: number): void
}>()

const optionsService = computed(() => [
    { value: 0, label: 'Tous les services' },
    ...props.services.map(s => ({ value: s.id, label: s.libelle })),
])

const serviceSelectionne = computed({
    get: () => props.serviceId,
    set: (valeur: number | string | null) => {
        const normalise = valeur === '' || valeur === null ? 0 : Number(valeur)
        emit('update:serviceId', Number.isNaN(normalise) ? 0 : normalise)
    },
})

const HEIGHT = 320
const PAD = { top: 28, right: 24, bottom: 66, left: 56 }
const INNER_H = HEIGHT - PAD.top - PAD.bottom
const Y_TICKS = [0, 25, 50, 75, 100]

const largeurTotale = computed(() =>
    Math.max(520, props.types.length * 96 + PAD.left + PAD.right)
)

const largeurInterne = computed(
    () => largeurTotale.value - PAD.left - PAD.right
)

const barres = computed(() =>
    props.types.map((t, index) => {
        const avancement = Math.max(0, Math.min(100, t.avancement))
        const slot = props.types.length === 0
            ? largeurInterne.value
            : largeurInterne.value / props.types.length
        const largeur = Math.min(56, slot * 0.5)
        const centre = PAD.left + slot * (index + 0.5)
        const hauteur = (INNER_H * avancement) / 100
        return {
            ...t,
            avancement,
            x: centre - largeur / 2,
            y: PAD.top + INNER_H - hauteur,
            largeur,
            hauteur,
            centre,
            libelleCourt: tronquer(t.typeActivite),
        }
    })
)

function tronquer(valeur: string, max = 16): string {
    if (!valeur) return '—'
    return valeur.length > max ? `${valeur.slice(0, max - 1)}…` : valeur
}

function py(valeur: number): number {
    return PAD.top + INNER_H * (1 - valeur / 100)
}
</script>

<template>
    <section class="types">
        <header class="types__head">
            <div>
                <h2 class="types__title">Avancement par type d'activité</h2>
                <p class="types__sub">
                    Taux moyen d'avancement sur la période sélectionnée.
                </p>
            </div>

            <div v-if="filtreServiceVisible" class="types__filter">
                <label class="types__filter-label">Service</label>
                <BaseSelect v-model="serviceSelectionne" size="sm" :options="optionsService" />
            </div>
        </header>

        <div v-if="loading" class="types__loading">
            <div class="skeleton"></div>
        </div>

        <div v-else-if="types.length === 0" class="types__empty">
            Aucun type d'activité renseigné sur la période sélectionnée.
        </div>

        <div v-else class="types__chart-wrap">
            <div class="types__chart-inner" :style="{ minWidth: largeurTotale + 'px' }">
                <svg class="types__chart" :viewBox="`0 0 ${largeurTotale} ${HEIGHT}`" role="img"
                    aria-label="Avancement moyen par type d'activité">
                    <g v-for="t in Y_TICKS" :key="t">
                        <line :x1="PAD.left" :x2="largeurTotale - PAD.right" :y1="py(t)" :y2="py(t)"
                            class="types__grid" />
                        <text :x="PAD.left - 10" :y="py(t) + 4" text-anchor="end" class="types__axis">
                            {{ t }} %
                        </text>
                    </g>

                    <line :x1="PAD.left" :x2="largeurTotale - PAD.right" :y1="PAD.top + INNER_H"
                        :y2="PAD.top + INNER_H" class="types__axis-line" />

                    <g v-for="b in barres" :key="b.idTypeActivite">
                        <rect :x="b.x" :y="b.y" :width="b.largeur" :height="b.hauteur"
                            fill="#4a6cf7" rx="6" class="types__bar">
                            <title>
                                {{ b.typeActivite }} : {{ b.avancement.toFixed(1) }} %
                                ({{ b.totalActivites }} activité{{ b.totalActivites > 1 ? 's' : '' }})
                            </title>
                        </rect>
                        <text :x="b.centre" :y="b.y - 8" text-anchor="middle" class="types__value">
                            {{ b.avancement.toFixed(1) }} %
                        </text>
                        <text :x="b.centre" :y="PAD.top + INNER_H + 20" text-anchor="middle"
                            class="types__label">
                            {{ b.libelleCourt }}
                        </text>
                        <text :x="b.centre" :y="PAD.top + INNER_H + 38" text-anchor="middle"
                            class="types__meta">
                            {{ b.totalActivites }} activité{{ b.totalActivites > 1 ? 's' : '' }}
                        </text>
                    </g>
                </svg>
            </div>
        </div>
    </section>
</template>

<style scoped>
.types {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    padding: 1.25rem 1.5rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
}

.types__head {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    gap: 1rem;
    flex-wrap: wrap;
}

.types__title {
    font-size: 1rem;
    font-weight: 700;
    color: var(--dts-navy);
    margin: 0;
}

.types__sub {
    margin: 0.2rem 0 0;
    font-size: 0.76rem;
    color: var(--dts-muted);
}

.types__filter {
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
    min-width: 180px;
}

.types__filter-label {
    font-size: 0.68rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--dts-muted);
}

.types__chart-wrap {
    width: 100%;
    overflow-x: auto;
}

.types__chart-inner {
    position: relative;
}

.types__chart {
    display: block;
    width: 100%;
    height: auto;
}

.types__grid {
    stroke: var(--dts-border);
    stroke-width: 1;
    stroke-dasharray: 3 3;
}

.types__axis-line {
    stroke: var(--dts-border);
    stroke-width: 1;
}

.types__axis {
    fill: var(--dts-muted);
    font-size: 10px;
}

.types__bar {
    transition: opacity .15s ease;
}

.types__bar:hover {
    opacity: 0.85;
}

.types__value {
    fill: var(--dts-navy, #1a2a6c);
    font-size: 11px;
    font-weight: 700;
}

.types__label {
    fill: var(--dts-text, #334155);
    font-size: 11px;
    font-weight: 600;
}

.types__meta {
    fill: var(--dts-muted, #6b7280);
    font-size: 10px;
}

.types__loading .skeleton {
    height: 260px;
    border-radius: var(--dts-radius-sm);
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: typesSk 1.4s infinite;
}

.types__empty {
    padding: 2rem 1rem;
    text-align: center;
    font-size: 0.85rem;
    color: var(--dts-muted);
}

@keyframes typesSk {
    from {
        background-position: 200% 0;
    }

    to {
        background-position: -200% 0;
    }
}

@media (max-width: 576px) {
    .types__head {
        align-items: flex-start;
    }

    .types__filter {
        width: 100%;
    }
}
</style>

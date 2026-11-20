<script setup lang="ts">
import { computed } from 'vue'
import type { DashboardPlanActionOrigine } from '@/types/dashboard'

const props = defineProps<{
    data: DashboardPlanActionOrigine[]
    loading?: boolean
}>()

const COULEURS: Record<string, string> = {
    INCIDENT: '#d64545',
    RISQUE: '#d98324',
    AUTRE: '#4a6cf7',
}

const WIDTH = 920
const HEIGHT = 320
const PAD = { top: 28, right: 24, bottom: 62, left: 56 }
const INNER_W = WIDTH - PAD.left - PAD.right
const INNER_H = HEIGHT - PAD.top - PAD.bottom
const Y_TICKS = [0, 25, 50, 75, 100]

const barres = computed(() =>
    props.data.map((d, index) => {
        const avancement = Math.max(0, Math.min(100, d.avancement))
        const slot = props.data.length === 0 ? INNER_W : INNER_W / props.data.length
        const largeur = Math.min(72, slot * 0.5)
        const centre = PAD.left + slot * (index + 0.5)
        const hauteur = (INNER_H * avancement) / 100
        return {
            ...d,
            couleur: COULEURS[d.origine] ?? '#6b7b8d',
            avancement,
            x: centre - largeur / 2,
            y: PAD.top + INNER_H - hauteur,
            largeur,
            hauteur,
            centre,
        }
    })
)

const total = computed(() =>
    props.data.reduce((somme, d) => somme + d.total, 0)
)

const aDesDonnees = computed(() => total.value > 0)

function py(valeur: number): number {
    return PAD.top + INNER_H * (1 - valeur / 100)
}
</script>

<template>
    <section class="pao">
        <header class="pao__head">
            <div>
                <h2 class="pao__title">Avancement des plans d'action par origine</h2>
                <p class="pao__sub">
                    Taux d'avancement moyen des plans sur la période sélectionnée.
                </p>
            </div>
        </header>

        <div v-if="loading" class="pao__loading">
            <div class="skeleton"></div>
        </div>

        <div v-else-if="!aDesDonnees" class="pao__empty">
            <i class="bi bi-bar-chart"></i>
            <span>
                Aucun plan d'action sur la période sélectionnée.
            </span>
        </div>

        <div v-else class="pao__chart-wrap">
            <div class="pao__chart-inner">
                <svg class="pao__chart" :viewBox="`0 0 ${WIDTH} ${HEIGHT}`" role="img"
                    aria-label="Avancement moyen des plans d'action par origine">
                    <!-- Grille et axe Y -->
                    <g v-for="t in Y_TICKS" :key="t">
                        <line :x1="PAD.left" :x2="WIDTH - PAD.right" :y1="py(t)" :y2="py(t)"
                            class="pao__grid" />
                        <text :x="PAD.left - 10" :y="py(t) + 4" text-anchor="end" class="pao__axis">
                            {{ t }} %
                        </text>
                    </g>

                    <!-- Axe X -->
                    <line :x1="PAD.left" :x2="WIDTH - PAD.right" :y1="PAD.top + INNER_H"
                        :y2="PAD.top + INNER_H" class="pao__axis-line" />

                    <!-- Barres verticales -->
                    <g v-for="b in barres" :key="b.origine">
                        <rect :x="b.x" :y="b.y" :width="b.largeur" :height="b.hauteur"
                            :fill="b.couleur" rx="6" class="pao__bar">
                            <title>
                                {{ b.libelle }} : {{ b.avancement.toFixed(1) }} %
                                ({{ b.total }} plan{{ b.total > 1 ? 's' : '' }})
                            </title>
                        </rect>
                        <text :x="b.centre" :y="b.y - 8" text-anchor="middle" class="pao__value">
                            {{ b.avancement.toFixed(1) }} %
                        </text>
                        <text :x="b.centre" :y="PAD.top + INNER_H + 20" text-anchor="middle"
                            class="pao__label">
                            {{ b.libelle }}
                        </text>
                        <text :x="b.centre" :y="PAD.top + INNER_H + 38" text-anchor="middle"
                            class="pao__meta">
                            {{ b.total }} plan{{ b.total > 1 ? 's' : '' }}
                        </text>
                    </g>
                </svg>
            </div>
        </div>
    </section>
</template>

<style scoped>
.pao {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    padding: 1.25rem 1.5rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
}

.pao__head {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    gap: 1rem;
    flex-wrap: wrap;
}

.pao__title {
    font-size: 1rem;
    font-weight: 700;
    color: var(--dts-navy);
    margin: 0;
}

.pao__sub {
    margin: 0.25rem 0 0;
    font-size: 0.78rem;
    color: var(--dts-muted);
}

.pao__chart-wrap {
    width: 100%;
    overflow-x: auto;
}

.pao__chart-inner {
    position: relative;
    min-width: 520px;
}

.pao__chart {
    display: block;
    width: 100%;
    height: auto;
}

.pao__grid {
    stroke: var(--dts-border);
    stroke-width: 1;
    stroke-dasharray: 3 3;
}

.pao__axis-line {
    stroke: var(--dts-border);
    stroke-width: 1;
}

.pao__axis {
    fill: var(--dts-muted);
    font-size: 10px;
}

.pao__bar {
    transition: opacity .15s ease;
}

.pao__bar:hover {
    opacity: 0.85;
}

.pao__value {
    fill: var(--dts-navy, #1a2a6c);
    font-size: 11px;
    font-weight: 700;
}

.pao__label {
    fill: var(--dts-text, #334155);
    font-size: 11px;
    font-weight: 600;
}

.pao__meta {
    fill: var(--dts-muted, #6b7280);
    font-size: 10px;
}

.pao__loading .skeleton {
    height: 260px;
    border-radius: var(--dts-radius-sm);
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: paoSk 1.4s infinite;
}

@keyframes paoSk {
    from {
        background-position: 200% 0;
    }

    to {
        background-position: -200% 0;
    }
}

.pao__empty {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 2rem 1rem;
    border-radius: var(--dts-radius-sm);
    background: var(--dts-bg);
    color: var(--dts-muted);
    font-size: 0.85rem;
}
</style>

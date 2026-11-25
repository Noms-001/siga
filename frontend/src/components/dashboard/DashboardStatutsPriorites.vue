<script setup lang="ts">
import { computed } from 'vue'
import type { DashboardStatutPriorite } from '@/types/dashboard'

const props = defineProps<{
    data: DashboardStatutPriorite[]
    loading?: boolean
}>()

/* ---------------------------------------------------------------- */
/*  Priorités : ordre d'empilement et palette de la charte            */
/* ---------------------------------------------------------------- */

const ORDRE_PRIORITES = ['CRITIQUE', 'HAUTE', 'NORMALE', 'FAIBLE', 'SANS_PRIORITE']

const COULEURS_PRIORITE: Record<string, string> = {
    CRITIQUE: '#d64545',
    HAUTE: '#d98324',
    NORMALE: '#4a6cf7',
    FAIBLE: '#9fb0c2',
    SANS_PRIORITE: '#cbd5e1',
}

const LIBELLES_PRIORITE: Record<string, string> = {
    CRITIQUE: 'Critique',
    HAUTE: 'Haute',
    NORMALE: 'Normale',
    FAIBLE: 'Faible',
    SANS_PRIORITE: 'Sans priorité',
}

interface Segment {
    code: string
    libelle: string
    nombre: number
    couleur: string
}

interface Barre {
    code: string
    libelle: string
    total: number
    segments: Segment[]
}

function normaliserPriorite(code: string | null): string {
    return (code || 'SANS_PRIORITE').toUpperCase()
}

function ordrePriorite(code: string): number {
    const index = ORDRE_PRIORITES.indexOf(code)
    return index === -1 ? ORDRE_PRIORITES.length : index
}

function couleurPriorite(code: string): string {
    return COULEURS_PRIORITE[code] ?? '#cbd5e1'
}

function libellePriorite(code: string, libelle: string | null): string {
    return libelle || LIBELLES_PRIORITE[code] || code
}

/* ---------------------------------------------------------------- */
/*  Regroupement des lignes (statut, priorité) en barres groupées     */
/* ---------------------------------------------------------------- */

const barres = computed<Barre[]>(() => {
    const index = new Map<string, Barre>()

    for (const row of props.data) {
        const statut = row.statut || 'SANS_STATUT'
        let barre = index.get(statut)
        if (!barre) {
            barre = {
                code: statut,
                libelle: row.statutLibelle || statut,
                total: 0,
                segments: [],
            }
            index.set(statut, barre)
        }

        const code = normaliserPriorite(row.priorite)
        barre.segments.push({
            code,
            libelle: libellePriorite(code, row.prioriteLibelle),
            nombre: row.nombre,
            couleur: couleurPriorite(code),
        })
        barre.total += row.nombre
    }

    for (const barre of index.values()) {
        barre.segments.sort((a, b) => ordrePriorite(a.code) - ordrePriorite(b.code))
    }

    return Array.from(index.values())
})

const totalActivites = computed(() =>
    barres.value.reduce((somme, b) => somme + b.total, 0)
)

const legende = computed(() => {
    const codes = new Set(
        props.data.map(r => normaliserPriorite(r.priorite))
    )
    return Array.from(codes)
        .sort((a, b) => ordrePriorite(a) - ordrePriorite(b))
        .map(code => ({
            code,
            libelle: LIBELLES_PRIORITE[code] ?? code,
            couleur: couleurPriorite(code),
        }))
})

/* ---------------------------------------------------------------- */
/*  Géométrie du graphique (SVG pur, sans dépendance)                 */
/* ---------------------------------------------------------------- */

const HEIGHT = 320
const PAD = { top: 32, right: 24, bottom: 78, left: 56 }
const INNER_H = HEIGHT - PAD.top - PAD.bottom

// Largeur d'un groupe de statut (plus large qu'avant pour loger
// plusieurs barres verticales côte à côte).
const LARGEUR_GROUPE = 130

const largeurTotale = computed(() =>
    Math.max(520, barres.value.length * LARGEUR_GROUPE + PAD.left + PAD.right)
)

const largeurInterne = computed(
    () => largeurTotale.value - PAD.left - PAD.right
)

const yStep = computed(() => {
    const max = Math.max(1, ...barres.value.map(b => b.total))
    return Math.max(1, Math.ceil(max / 4))
})

const yMax = computed(() => yStep.value * 4)

const yTicks = computed(() =>
    Array.from({ length: 5 }, (_, i) => i * yStep.value)
)

const barresGeo = computed(() => {
    const nombre = barres.value.length
    const slot = nombre === 0
        ? largeurInterne.value
        : largeurInterne.value / nombre

    return barres.value.map((barre, index) => {
        // Uniquement les priorités réellement présentes
        const actifs = barre.segments.filter(s => s.nombre > 0)
        const nbSegments = actifs.length

        const centre = PAD.left + slot * (index + 0.5)

        // Largeur d'une barre individuelle (cappée pour rester lisible
        // même avec beaucoup de statuts ou de priorités).
        const barWidth = nbSegments > 0
            ? Math.min((slot * 0.65) / nbSegments, 26)
            : 0
        const gap = barWidth > 4 ? Math.min(2, barWidth * 0.18) : 0
        const rectWidth = Math.max(0.5, barWidth - gap)
        const groupWidth = barWidth * nbSegments
        const groupLeft = centre - groupWidth / 2

        // Disposition « de droite à gauche » :
        //   CRITIQUE (i=0)      → position la plus à droite
        //   SANS_PRIORITE (i=n) → position la plus à gauche
        const segments = actifs.map((s, i) => {
            const hauteur = (INNER_H * s.nombre) / yMax.value
            const y = PAD.top + INNER_H - hauteur
            const posFromLeft = nbSegments - 1 - i
            const x = groupLeft + posFromLeft * barWidth + gap / 2
            return {
                ...s,
                x,
                y,
                largeur: rectWidth,
                hauteur,
                // On affiche la valeur au-dessus de la barre si assez large
                showLabel: rectWidth >= 12,
            }
        })

        const maxHauteur = actifs.reduce(
            (max, s) => Math.max(max, (INNER_H * s.nombre) / yMax.value),
            0
        )

        return {
            ...barre,
            centre,
            segments,
            sommet: PAD.top + INNER_H - maxHauteur,
            libelleCourt: tronquer(barre.libelle),
        }
    })
})

function tronquer(valeur: string, max = 14): string {
    if (!valeur) return '—'
    return valeur.length > max ? `${valeur.slice(0, max - 1)}…` : valeur
}

function py(valeur: number): number {
    return PAD.top + INNER_H * (1 - valeur / yMax.value)
}
</script>

<template>
    <section class="sp">
        <header class="sp__head">
            <div>
                <h2 class="sp__title">Répartition des activités par statut et priorité</h2>
                <p class="sp__sub">
                    Nombre d'activités par statut, ventilé par niveau de priorité.
                </p>
            </div>
            <span class="sp__total">
                {{ totalActivites }} activité{{ totalActivites > 1 ? 's' : '' }}
            </span>
        </header>

        <div v-if="loading" class="sp__loading">
            <div class="skeleton"></div>
        </div>

        <div v-else-if="barres.length === 0" class="sp__empty">
            Aucune donnée de statut et de priorité sur la période sélectionnée.
        </div>

        <template v-else>
            <ul class="sp__legend" aria-label="Légende des priorités">
                <li v-for="p in legende" :key="p.code" class="sp__legend-item">
                    <span class="sp__legend-dot" :style="{ background: p.couleur }" />
                    {{ p.libelle }}
                </li>
            </ul>

            <div class="sp__chart-wrap">
                <div class="sp__chart-inner" :style="{ minWidth: largeurTotale + 'px' }">
                    <svg class="sp__chart" :viewBox="`0 0 ${largeurTotale} ${HEIGHT}`" role="img"
                        aria-label="Répartition des activités par statut et priorité">
                        <g v-for="(tick, i) in yTicks" :key="i">
                            <line :x1="PAD.left" :x2="largeurTotale - PAD.right" :y1="py(tick)"
                                :y2="py(tick)" class="sp__grid" />
                            <text :x="PAD.left - 10" :y="py(tick) + 4" text-anchor="end" class="sp__axis">
                                {{ tick }}
                            </text>
                        </g>

                        <line :x1="PAD.left" :x2="largeurTotale - PAD.right" :y1="PAD.top + INNER_H"
                            :y2="PAD.top + INNER_H" class="sp__axis-line" />

                        <g v-for="b in barresGeo" :key="b.code">
                            <!-- Barres verticales individuelles, côte à côte -->
                            <rect v-for="s in b.segments" :key="s.code" :x="s.x" :y="s.y"
                                :width="s.largeur" :height="s.hauteur" :fill="s.couleur" rx="1.5"
                                class="sp__seg">
                                <title>{{ b.libelle }} — {{ s.libelle }} : {{ s.nombre }}</title>
                            </rect>

                            <!-- Valeur au-dessus de chaque barre -->
                            <text v-for="s in b.segments" :key="'v-' + s.code"
                                v-show="s.showLabel"
                                :x="s.x + s.largeur / 2" :y="s.y - 3"
                                text-anchor="middle" class="sp__seg-value">
                                {{ s.nombre }}
                            </text>

                            <!-- Total au-dessus du groupe -->
                            <text :x="b.centre" :y="b.sommet - 16" text-anchor="middle" class="sp__value">
                                {{ b.total }}
                            </text>
                            <text :x="b.centre" :y="PAD.top + INNER_H + 20" text-anchor="middle"
                                class="sp__label">
                                {{ b.libelleCourt }}
                            </text>
                            <text :x="b.centre" :y="PAD.top + INNER_H + 38" text-anchor="middle"
                                class="sp__meta">
                                {{ b.total }} activité{{ b.total > 1 ? 's' : '' }}
                            </text>
                        </g>
                    </svg>
                </div>
            </div>
        </template>
    </section>
</template>

<style scoped>
.sp {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    padding: 1.25rem 1.5rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
}

.sp__head {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    gap: 1rem;
    flex-wrap: wrap;
}

.sp__title {
    margin: 0;
    color: var(--dts-navy);
    font-size: 1rem;
    font-weight: 700;
}

.sp__sub {
    margin: 0.2rem 0 0;
    color: var(--dts-muted);
    font-size: 0.76rem;
}

.sp__total {
    color: var(--dts-muted);
    font-size: 0.8rem;
}

.sp__legend {
    display: flex;
    flex-wrap: wrap;
    gap: 0.4rem 1rem;
    margin: 0;
    padding: 0;
    list-style: none;
}

.sp__legend-item {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    color: var(--dts-text, #334155);
    font-size: 0.78rem;
    font-weight: 500;
}

.sp__legend-dot {
    width: 10px;
    height: 10px;
    border-radius: 3px;
}

.sp__chart-wrap {
    width: 100%;
    overflow-x: auto;
}

.sp__chart-inner {
    position: relative;
}

.sp__chart {
    display: block;
    width: 100%;
    height: auto;
}

.sp__grid {
    stroke: var(--dts-border);
    stroke-width: 1;
    stroke-dasharray: 3 3;
}

.sp__axis-line {
    stroke: var(--dts-border);
    stroke-width: 1;
}

.sp__axis {
    fill: var(--dts-muted);
    font-size: 10px;
}

.sp__seg {
    transition: opacity .15s ease;
}

.sp__seg:hover {
    opacity: 0.82;
}

.sp__seg-value {
    fill: var(--dts-muted, #6b7280);
    font-size: 9px;
    font-weight: 600;
}

.sp__value {
    fill: var(--dts-navy, #1a2a6c);
    font-size: 11px;
    font-weight: 700;
}

.sp__label {
    fill: var(--dts-text, #334155);
    font-size: 11px;
    font-weight: 600;
}

.sp__meta {
    fill: var(--dts-muted, #6b7280);
    font-size: 10px;
}

.sp__loading .skeleton {
    height: 260px;
    border-radius: var(--dts-radius-sm);
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: spSk 1.4s infinite;
}

.sp__empty {
    padding: 2rem 1rem;
    text-align: center;
    font-size: 0.85rem;
    color: var(--dts-muted);
}

@keyframes spSk {
    from {
        background-position: 200% 0;
    }

    to {
        background-position: -200% 0;
    }
}

@media (max-width: 576px) {
    .sp__head {
        align-items: flex-start;
    }
}
</style>
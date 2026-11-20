<script setup lang="ts">
import { computed } from 'vue'
import type { DashboardStatut } from '@/types/dashboard'

const props = defineProps<{
    statuts: DashboardStatut[]
    loading?: boolean
}>()

/* ---------------------------------------------------------------- */
/*  Palette alignée sur les tons existants (tone--success, etc.)     */
/* ---------------------------------------------------------------- */

const COULEURS: Record<string, string> = {
    BROUILLON: '#9fb0c2',
    SANS_STATUT: '#9fb0c2',
    TERMINEE: '#1e7e34',
    VALIDEE: '#1e7e34',
    EN_COURS: '#4a6cf7',
    NON_COMMENCEE: '#9fb0c2',
    EN_RETARD: '#d64545',
    REJETE: '#d64545',
    ANNULEE: '#d64545',
    EN_ATTENTE_VALIDATION: '#d98324',
    REPORTEE: '#d98324',
    SUSPENDUE: '#6b7b8d',
}

function couleur(code: string): string {
    return COULEURS[code] ?? '#6b7b8d'
}

/* ---------------------------------------------------------------- */
/*  Total & parts                                                     */
/* ---------------------------------------------------------------- */

const total = computed(() =>
    props.statuts.reduce((s, x) => s + x.nombre, 0)
)

const RAYON = 60
const CENTRE = 80
const EPAISSEUR = 26
const CIRCONFERENCE = 2 * Math.PI * RAYON

/**
 * Chaque part est un arc de cercle tracé via stroke-dasharray sur un
 * <circle>. L'offset décale le départ de l'arc pour empiler les parts
 * dans l'ordre. Pas de dépendance : SVG pur.
 */
const parts = computed(() => {
    if (total.value === 0) return []
    let cumul = 0
    return props.statuts
        .filter(s => s.nombre > 0)
        .map(s => {
            const fraction = s.nombre / total.value
            const longueur = fraction * CIRCONFERENCE
            const p = {
                code: s.statut,
                libelle: s.libelle || s.statut,
                nombre: s.nombre,
                pourcentage: fraction * 100,
                couleur: couleur(s.statut),
                dashArray: `${longueur} ${CIRCONFERENCE - longueur}`,
                dashOffset: -cumul,
            }
            cumul += longueur
            return p
        })
})
</script>

<template>
    <section class="statuts">
        <header class="statuts__head">
            <h2 class="statuts__title">Répartition des activités suivies par statut</h2>
            <span class="statuts__total">
                {{ total }} activité{{ total > 1 ? 's' : '' }}
            </span>
        </header>

        <!-- Skeleton -->
        <div v-if="loading" class="statuts__loading">
            <div class="statuts__skeleton-donut"></div>
            <div class="statuts__skeleton-legend">
                <div v-for="i in 6" :key="i" class="statuts__skeleton-row"></div>
            </div>
        </div>

        <!-- Vide -->
        <div v-else-if="total === 0" class="statuts__empty">
            Aucune donnée de statut disponible.
        </div>

        <!-- Donut + légende -->
        <div v-else class="statuts__body">
            <svg class="statuts__donut" viewBox="0 0 160 160" role="img"
                :aria-label="`Répartition de ${total} activités par statut`">
                <!-- fond -->
                <circle :cx="CENTRE" :cy="CENTRE" :r="RAYON" fill="none" stroke="var(--dts-bg, #eef1f5)"
                    :stroke-width="EPAISSEUR" />
                <!-- parts -->
                <circle v-for="p in parts" :key="p.code" :cx="CENTRE" :cy="CENTRE" :r="RAYON" fill="none"
                    :stroke="p.couleur" :stroke-width="EPAISSEUR" :stroke-dasharray="p.dashArray"
                    :stroke-dashoffset="p.dashOffset" transform="rotate(-90 80 80)" stroke-linecap="butt">
                    <title>
                        {{ p.libelle }} : {{ p.nombre }}
                        ({{ p.pourcentage.toFixed(1) }} %)
                    </title>
                </circle>
                <!-- total au centre -->
                <text :x="CENTRE" :y="CENTRE - 2" text-anchor="middle" class="statuts__donut-total">{{ total }}</text>
                <text :x="CENTRE" :y="CENTRE + 15" text-anchor="middle" class="statuts__donut-lib">activité{{ total > 1
                    ? 's' : '' }}</text>
            </svg>

            <ul class="statuts__legend">
                <li v-for="p in parts" :key="p.code" class="statuts__legend-item">
                    <span class="statuts__dot" :style="{ background: p.couleur }" />
                    <span class="statuts__legend-lib">{{ p.libelle }}</span>
                    <span class="statuts__legend-nb">{{ p.nombre }}</span>
                    <span class="statuts__legend-pct">
                        {{ p.pourcentage.toFixed(1) }} %
                    </span>
                </li>
            </ul>
        </div>
    </section>
</template>

<style scoped>
.statuts {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    padding: 1.25rem 1.5rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
}

.statuts__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;
}

.statuts__title {
    font-size: 1rem;
    font-weight: 700;
    color: var(--dts-navy);
    margin: 0;
}

.statuts__total {
    font-size: 0.8rem;
    color: var(--dts-muted);
}

.statuts__body {
    display: flex;
    align-items: center;
    gap: 1.5rem;
    flex-wrap: wrap;
}

.statuts__donut {
    width: 180px;
    height: 180px;
    flex: 0 0 auto;
}

.statuts__donut-total {
    font: 700 22px "DM Sans", system-ui, sans-serif;
    fill: var(--dts-navy, #1a2a6c);
}

.statuts__donut-lib {
    font: 500 9px "DM Sans", system-ui, sans-serif;
    fill: var(--dts-muted, #6b7280);
    letter-spacing: .05em;
    text-transform: uppercase;
}

.statuts__legend {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 0.55rem;
    flex: 1 1 180px;
    min-width: 180px;
}

.statuts__legend-item {
    display: grid;
    grid-template-columns: 12px 1fr auto auto;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.82rem;
    color: var(--dts-text, #334155);
}

.statuts__dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    display: inline-block;
}

.statuts__legend-lib {
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.statuts__legend-nb {
    color: var(--dts-navy, #1a2a6c);
    font-weight: 700;
    font-variant-numeric: tabular-nums;
}

.statuts__legend-pct {
    color: var(--dts-muted, #6b7280);
    font-size: 0.74rem;
    font-variant-numeric: tabular-nums;
    min-width: 3.2em;
    text-align: right;
}

/* Skeleton */
.statuts__loading {
    display: flex;
    align-items: center;
    gap: 1.5rem;
    flex-wrap: wrap;
}

.statuts__skeleton-donut {
    width: 180px;
    height: 180px;
    border-radius: 50%;
    background: conic-gradient(from 0deg,
            #eef1f5 0deg 90deg,
            #f6f8fa 90deg 180deg,
            #eef1f5 180deg 270deg,
            #f6f8fa 270deg 360deg);
    animation: statSpin 1.4s linear infinite;
}

.statuts__skeleton-legend {
    flex: 1 1 180px;
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
}

.statuts__skeleton-row {
    height: 14px;
    border-radius: 6px;
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: statSk 1.4s infinite;
}

.statuts__empty {
    padding: 2rem 1rem;
    text-align: center;
    font-size: 0.85rem;
    color: var(--dts-muted);
}

@keyframes statSk {
    from {
        background-position: 200% 0;
    }

    to {
        background-position: -200% 0;
    }
}

@keyframes statSpin {
    from {
        transform: rotate(0deg);
    }

    to {
        transform: rotate(360deg);
    }
}

@media (max-width: 576px) {
    .statuts__body {
        justify-content: center;
    }

    .statuts__legend {
        min-width: 100%;
    }
}
</style>
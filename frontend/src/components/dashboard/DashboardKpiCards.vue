<script setup lang="ts">
import { computed } from 'vue'
import type { DashboardKpi } from '@/types/dashboard'

const props = defineProps<{
    kpi: DashboardKpi | null
    loading?: boolean
}>()

interface KpiCard {
    key: keyof DashboardKpi
    label: string
    hint: string
    icon: string
    tone: 'neutral' | 'info' | 'success' | 'warning' | 'danger' | 'muted'
    suffix?: string
}

/**
 * Deux lignes de cinq cartes. Chaque carte porte le nom du champ qu'elle
 * affiche : le rendu lit `kpi[card.key]` directement, ce qui évite toute
 * divergence entre le contrat backend et l'affichage.
 */
const cards: KpiCard[] = [
    { key: 'totalActivites',      label: 'Total activités',  hint: 'Activités enregistrées',  icon: 'bi bi-collection',         tone: 'neutral' },
    { key: 'enCours',             label: 'En cours',         hint: 'Activités démarrées',     icon: 'bi bi-play-circle',        tone: 'info' },
    { key: 'terminees',           label: 'Terminées',        hint: 'Activités clôturées',     icon: 'bi bi-check2-circle',      tone: 'success' },
    { key: 'enRetard',            label: 'En retard',        hint: 'Échéance dépassée',       icon: 'bi bi-exclamation-triangle', tone: 'danger' },
    { key: 'reportees',           label: 'Reportées',        hint: 'Échéance repoussée',      icon: 'bi bi-calendar-x',         tone: 'warning' },

    { key: 'nonCommencees',       label: 'Non commencées',   hint: 'Aucune action engagée',   icon: 'bi bi-hourglass',          tone: 'muted' },
    { key: 'suspendues',          label: 'Suspendues',       hint: 'En pause temporaire',     icon: 'bi bi-pause-circle',       tone: 'muted' },
    { key: 'annulees',            label: 'Annulées',         hint: 'Activités abandonnées',   icon: 'bi bi-x-circle',           tone: 'muted' },
    { key: 'enAttenteValidation', label: 'En attente',       hint: 'Validation requise',      icon: 'bi bi-shield-exclamation', tone: 'warning' },
    { key: 'avancementGlobal',    label: 'Avancement global', hint: 'Moyenne pondérée',       icon: 'bi bi-speedometer2',       tone: 'info', suffix: '%' },
]

const rows = computed(() => {
    // Découpe fixe en deux groupes de 5 pour la grille visuelle.
    return [cards.slice(0, 5), cards.slice(5)]
})

function valeur(key: keyof DashboardKpi, suffix?: string): string {
    if (!props.kpi) return '—'
    const v = props.kpi[key]
    if (v == null) return '—'
    return suffix ? `${Math.round(Number(v))}${suffix}` : String(v)
}
</script>

<template>
    <div class="kpi-rows">
        <div v-for="(row, i) in rows" :key="i" class="kpi-row">
            <article v-for="c in row" :key="c.key"
                class="kpi-card"
                :class="`kpi-card--${c.tone}`">
                <header class="kpi-card__head">
                    <span class="kpi-card__label">{{ c.label }}</span>
                    <i :class="[c.icon, 'kpi-card__icon']" aria-hidden="true"></i>
                </header>
                <div class="kpi-card__value">
                    <template v-if="loading">
                        <span class="kpi-card__skeleton"></span>
                    </template>
                    <template v-else>
                        {{ valeur(c.key, c.suffix) }}
                    </template>
                </div>
                <footer class="kpi-card__hint">{{ c.hint }}</footer>
            </article>
        </div>
    </div>
</template>

<style scoped>
.kpi-rows {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.kpi-row {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 1rem;
}

.kpi-card {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    padding: 1rem 1.15rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-left: 4px solid var(--dts-border-2);
    border-radius: var(--dts-radius);
    min-height: 110px;
}

.kpi-card__head {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 0.5rem;
}

.kpi-card__label {
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    color: var(--dts-muted);
}

.kpi-card__icon {
    font-size: 1.1rem;
    color: var(--dts-muted);
    line-height: 1;
}

.kpi-card__value {
    font-size: 1.75rem;
    font-weight: 700;
    line-height: 1;
    color: var(--dts-navy);
}

.kpi-card__skeleton {
    display: inline-block;
    width: 60%;
    height: 1.75rem;
    border-radius: 6px;
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: kpiSk 1.4s infinite;
}

.kpi-card__hint {
    font-size: 0.72rem;
    color: var(--dts-muted);
    margin-top: auto;
}

/* --- tons --- */

.kpi-card--neutral { border-left-color: var(--dts-navy); }
.kpi-card--info    { border-left-color: var(--dts-blue); }
.kpi-card--success { border-left-color: #1e7e34; }
.kpi-card--warning { border-left-color: #d98324; }
.kpi-card--danger  { border-left-color: #d64545; }
.kpi-card--muted   { border-left-color: #9fb0c2; }

.kpi-card--success .kpi-card__value { color: #1e7e34; }
.kpi-card--warning .kpi-card__value { color: #9a6409; }
.kpi-card--danger  .kpi-card__value { color: #a61b16; }
.kpi-card--info    .kpi-card__value { color: var(--dts-blue); }

@keyframes kpiSk {
    from { background-position: 200% 0; }
    to   { background-position: -200% 0; }
}

@media (max-width: 1200px) {
    .kpi-row { grid-template-columns: repeat(4, minmax(0, 1fr)); }
}

@media (max-width: 992px) {
    .kpi-row { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}

@media (max-width: 768px) {
    .kpi-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 480px) {
    .kpi-row { grid-template-columns: 1fr; }
}
</style>
<script setup lang="ts">
import { computed } from 'vue'
import type { DashboardStatut } from '@/types/dashboard'

const props = defineProps<{
    statuts: DashboardStatut[]
    loading?: boolean
}>()

/**
 * Total courant pour calculer les pourcentages — et non le total des
 * activités de l'année : les activités sans historique de statut ne
 * figurent pas dans cette liste et ne doivent pas tirer les pourcentages
 * vers le bas.
 */
const total = computed(() =>
    props.statuts.reduce((s, x) => s + x.nombre, 0)
)

/**
 * Couleur d'une barre selon le code de statut.
 *
 * Le mapping reste local au composant : c'est une décision de présentation,
 * pas une règle métier. Un code inconnu tombe sur gris neutre, jamais
 * sur une couleur qui prêterait à confusion.
 */
function tone(code: string): string {
    switch (code) {
        case 'TERMINEE':
        case 'VALIDEE':
            return 'tone--success'
        case 'EN_COURS':
            return 'tone--info'
        case 'NON_COMMENCEE':
            return 'tone--muted'
        case 'EN_ATTENTE_VALIDATION':
        case 'REPORTEE':
            return 'tone--warning'
        case 'EN_RETARD':
        case 'REJETE':
        case 'ANNULEE':
            return 'tone--danger'
        case 'SUSPENDUE':
            return 'tone--neutral'
        default:
            return 'tone--neutral'
    }
}

function part(n: number): number {
    if (total.value === 0) return 0
    return Math.round((n / total.value) * 100)
}
</script>

<template>
    <section class="statuts">
        <header class="statuts__head">
            <h2 class="statuts__title">Répartition par statut</h2>
            <span class="statuts__total">
                {{ total }} activité{{ total > 1 ? 's' : '' }}
            </span>
        </header>

        <div v-if="loading" class="statuts__loading">
            <div v-for="i in 4" :key="i" class="skeleton"></div>
        </div>

        <div v-else-if="statuts.length === 0" class="statuts__empty">
            Aucune donnée de statut disponible.
        </div>

        <ul v-else class="statuts__list">
            <li v-for="s in statuts" :key="s.statut" class="statuts__item">
                <div class="statuts__line">
                    <span class="statuts__label">{{ s.libelle || s.statut }}</span>
                    <span class="statuts__value">
                        <strong>{{ s.nombre }}</strong>
                        <span class="statuts__pct">{{ part(s.nombre) }} %</span>
                    </span>
                </div>
                <div class="progress statuts__bar">
                    <div class="progress-bar" :class="tone(s.statut)"
                        role="progressbar"
                        :style="{ width: part(s.nombre) + '%' }"
                        :aria-valuenow="part(s.nombre)"
                        aria-valuemin="0" aria-valuemax="100"></div>
                </div>
            </li>
        </ul>
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

.statuts__list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
}

.statuts__item { display: flex; flex-direction: column; gap: 0.25rem; }

.statuts__line {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    gap: 1rem;
    font-size: 0.82rem;
}

.statuts__label { color: var(--dts-text); }

.statuts__value {
    display: inline-flex;
    align-items: baseline;
    gap: 0.5rem;
}

.statuts__value strong {
    color: var(--dts-navy);
    font-weight: 700;
}

.statuts__pct {
    font-size: 0.72rem;
    color: var(--dts-muted);
    min-width: 42px;
    text-align: right;
}

.statuts__bar {
    height: 8px;
    border-radius: 20px;
    background: var(--dts-bg);
}

.statuts__bar .progress-bar { border-radius: 20px; }

/* Tons — purement visuels, pas de sémantique métier */

.tone--success { background: #1e7e34; }
.tone--info    { background: var(--dts-blue); }
.tone--warning { background: #d98324; }
.tone--danger  { background: #d64545; }
.tone--muted   { background: #9fb0c2; }
.tone--neutral { background: #6b7b8d; }

.statuts__loading {
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
}

.statuts__loading .skeleton {
    height: 20px;
    border-radius: 6px;
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: statSk 1.4s infinite;
}

.statuts__empty {
    padding: 1rem;
    text-align: center;
    font-size: 0.85rem;
    color: var(--dts-muted);
}

@keyframes statSk {
    from { background-position: 200% 0; }
    to   { background-position: -200% 0; }
}
</style>
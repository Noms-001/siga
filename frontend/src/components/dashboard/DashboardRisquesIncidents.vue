<script setup lang="ts">
import { computed } from 'vue'
import type { DashboardRisquesIncidents } from '@/types/dashboard'

const props = defineProps<{
    data: DashboardRisquesIncidents | null
    loading?: boolean
}>()

/**
 * Le backend n'expose aujourd'hui que `plansAction` — les risques et
 * incidents n'ont pas de table dans le modèle SAGA. Masquer la section
 * entière plutôt que d'afficher des zéros ambigus : un utilisateur qui
 * voit "0 risque" ne doit pas en déduire une absence de risque, mais
 * une absence de module.
 */
const afficher = computed(() => {
    if (props.loading) return true
    const p = props.data?.plansAction
    return p != null && p.total > 0
})
</script>

<template>
    <section v-if="afficher" class="ri">
        <header class="ri__head">
            <h2 class="ri__title">Suivi des plans d'action</h2>
        </header>

        <div v-if="loading" class="ri__loading">
            <div v-for="i in 4" :key="i" class="skeleton"></div>
        </div>

        <div v-else-if="data?.plansAction" class="ri__grid">
            <article class="ri-card">
                <span class="ri-card__label">Total</span>
                <span class="ri-card__value">{{ data.plansAction.total }}</span>
            </article>
            <article class="ri-card ri-card--info">
                <span class="ri-card__label">En cours</span>
                <span class="ri-card__value">{{ data.plansAction.enCours }}</span>
            </article>
            <article class="ri-card ri-card--success">
                <span class="ri-card__label">Terminés</span>
                <span class="ri-card__value">{{ data.plansAction.termines }}</span>
            </article>
            <article class="ri-card ri-card--danger">
                <span class="ri-card__label">En retard</span>
                <span class="ri-card__value">{{ data.plansAction.enRetard }}</span>
            </article>
        </div>
    </section>
</template>

<style scoped>
.ri {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    padding: 1.25rem 1.5rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
}

.ri__title { font-size: 1rem; font-weight: 700; color: var(--dts-navy); margin: 0; }

.ri__grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 0.75rem;
}

.ri-card {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
    padding: 0.75rem 1rem;
    border-radius: var(--dts-radius-sm);
    background: var(--dts-bg);
    border: 1px solid var(--dts-border);
}

.ri-card__label {
    font-size: 0.7rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--dts-muted);
    font-weight: 700;
}

.ri-card__value {
    font-size: 1.5rem;
    font-weight: 700;
    color: var(--dts-navy);
    line-height: 1;
}

.ri-card--info .ri-card__value    { color: var(--dts-blue); }
.ri-card--success .ri-card__value { color: #1e7e34; }
.ri-card--danger .ri-card__value  { color: #a61b16; }

.ri__loading {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 0.75rem;
}
.ri__loading .skeleton {
    height: 70px;
    border-radius: var(--dts-radius-sm);
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: riSk 1.4s infinite;
}
@keyframes riSk {
    from { background-position: 200% 0; }
    to   { background-position: -200% 0; }
}

@media (max-width: 768px) {
    .ri__grid, .ri__loading { grid-template-columns: repeat(2, 1fr); }
}
</style>
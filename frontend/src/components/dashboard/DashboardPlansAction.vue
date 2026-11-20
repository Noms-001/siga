<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { SuiviGlobalPlanAction } from '@/types/planaction'

const props = defineProps<{
    suivi: SuiviGlobalPlanAction | null
    loading?: boolean
}>()

const router = useRouter()

const afficher = computed(() => {
    if (props.loading) return true
    return (props.suivi?.resume.total ?? 0) > 0
})

const avancement = computed(() => props.suivi?.resume.avancementMoyen ?? 0)

function formaterDate(date: string): string {
    return new Date(date).toLocaleDateString('fr-FR')
}

function voirPlan(id: number): void {
    router.push({ name: 'plan-action-detail', params: { id } })
}
</script>

<template>
    <section v-if="afficher" class="pa">
        <header class="pa__head">
            <h2 class="pa__title">Suivi des plans d'action</h2>
        </header>

        <div v-if="loading" class="pa__loading">
            <div v-for="i in 5" :key="i" class="skeleton"></div>
        </div>

        <template v-else-if="suivi">
            <div class="pa__grid">
                <article class="pa-card">
                    <span class="pa-card__label">Total</span>
                    <span class="pa-card__value">{{ suivi.resume.total }}</span>
                </article>
                <article class="pa-card pa-card--info">
                    <span class="pa-card__label">En cours</span>
                    <span class="pa-card__value">{{ suivi.resume.enCours }}</span>
                </article>
                <article class="pa-card pa-card--success">
                    <span class="pa-card__label">Terminés</span>
                    <span class="pa-card__value">{{ suivi.resume.termines }}</span>
                </article>
                <article class="pa-card pa-card--danger">
                    <span class="pa-card__label">En retard</span>
                    <span class="pa-card__value">{{ suivi.resume.enRetard }}</span>
                </article>
                <article class="pa-card pa-card--progress">
                    <span class="pa-card__label">Avancement global</span>
                    <span class="pa-card__value">{{ avancement.toFixed(0) }} %</span>
                    <div class="pa-card__progress">
                        <div class="pa-card__progress-bar" :style="{ width: avancement + '%' }" />
                    </div>
                </article>
            </div>

            <div class="pa__panels">
                <section class="pa-panel">
                    <header class="pa-panel__head">
                        <span class="pa-panel__icon pa-panel__icon--danger">
                            <i class="bi bi-exclamation-triangle"></i>
                        </span>
                        <h3 class="pa-panel__title">Plans en retard</h3>
                        <span class="pa-panel__count">{{ suivi.enRetard.length }}</span>
                    </header>
                    <div v-if="suivi.enRetard.length === 0" class="pa-empty">
                        <i class="bi bi-check-circle"></i>
                        Aucun plan en retard.
                    </div>
                    <ul v-else class="pa-list">
                        <li v-for="p in suivi.enRetard" :key="p.id" class="pa-item"
                            @click="voirPlan(p.id)">
                            <div class="pa-item__head">
                                <code class="pa-item__code">{{ p.code }}</code>
                                <span class="pa-badge pa-badge--danger">
                                    {{ p.joursDeRetard }} j de retard
                                </span>
                            </div>
                            <span class="pa-item__designation">{{ p.designation }}</span>
                            <div class="pa-item__meta">
                                <span>{{ formaterDate(p.dateFinPrevue) }}</span>
                                <span>{{ p.avancement.toFixed(0) }} %</span>
                            </div>
                        </li>
                    </ul>
                </section>

                <section class="pa-panel">
                    <header class="pa-panel__head">
                        <span class="pa-panel__icon pa-panel__icon--warning">
                            <i class="bi bi-calendar-event"></i>
                        </span>
                        <h3 class="pa-panel__title">Échéances prochaines</h3>
                        <span class="pa-panel__count">{{ suivi.echeancesProchaines.length }}</span>
                    </header>
                    <div v-if="suivi.echeancesProchaines.length === 0" class="pa-empty">
                        <i class="bi bi-calendar-check"></i>
                        Aucune échéance dans les 30 prochains jours.
                    </div>
                    <ul v-else class="pa-list">
                        <li v-for="p in suivi.echeancesProchaines" :key="p.id" class="pa-item"
                            @click="voirPlan(p.id)">
                            <div class="pa-item__head">
                                <code class="pa-item__code">{{ p.code }}</code>
                                <span class="pa-badge pa-badge--warning">
                                    {{ p.joursRestants }} j
                                </span>
                            </div>
                            <span class="pa-item__designation">{{ p.designation }}</span>
                            <div class="pa-item__meta">
                                <span>{{ formaterDate(p.dateFinPrevue) }}</span>
                                <span>{{ p.avancement.toFixed(0) }} %</span>
                            </div>
                        </li>
                    </ul>
                </section>
            </div>
        </template>
    </section>
</template>

<style scoped>
.pa {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    padding: 1.25rem 1.5rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
}

.pa__title { font-size: 1rem; font-weight: 700; color: var(--dts-navy); margin: 0; }

.pa__grid {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 0.75rem;
}

.pa-card {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
    padding: 0.75rem 1rem;
    border-radius: var(--dts-radius-sm);
    background: var(--dts-bg);
    border: 1px solid var(--dts-border);
}

.pa-card__label {
    font-size: 0.7rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--dts-muted);
    font-weight: 700;
}

.pa-card__value {
    font-size: 1.5rem;
    font-weight: 700;
    color: var(--dts-navy);
    line-height: 1;
}

.pa-card--info .pa-card__value    { color: var(--dts-blue); }
.pa-card--success .pa-card__value { color: #1e7e34; }
.pa-card--danger .pa-card__value  { color: #a61b16; }

.pa-card__progress {
    height: 6px;
    margin-top: 0.35rem;
    border-radius: 999px;
    background: #e6eaf2;
    overflow: hidden;
}

.pa-card__progress-bar {
    height: 100%;
    border-radius: 999px;
    background: linear-gradient(135deg, #1a2a6c 0%, #4a6cf7 100%);
    transition: width .3s ease;
}

.pa__panels {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
}

.pa-panel {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    padding: 1rem;
    border-radius: var(--dts-radius-sm);
    border: 1px solid var(--dts-border);
    background: var(--dts-bg);
}

.pa-panel__head {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.pa-panel__title { font-size: 0.85rem; font-weight: 700; color: var(--dts-navy); margin: 0; }

.pa-panel__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 26px;
    height: 26px;
    border-radius: 8px;
    background: #eef1f7;
    color: var(--dts-navy);
    font-size: 0.8rem;
}

.pa-panel__icon--danger  { background: #fdecea; color: #a61b16; }
.pa-panel__icon--warning { background: #fff4e5; color: #b26a00; }

.pa-panel__count {
    margin-left: auto;
    font-size: 0.72rem;
    font-weight: 700;
    color: var(--dts-muted);
}

.pa-empty {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.8rem;
    color: var(--dts-muted);
    padding: 0.5rem 0;
}

.pa-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    max-height: 260px;
    overflow-y: auto;
}

.pa-item {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    padding: 0.6rem 0.75rem;
    border-radius: 8px;
    border: 1px solid var(--dts-border);
    background: var(--dts-surface);
    cursor: pointer;
    transition: border-color .15s ease, box-shadow .15s ease;
}

.pa-item:hover {
    border-color: var(--dts-blue);
    box-shadow: 0 2px 8px rgba(26, 42, 108, .08);
}

.pa-item__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.5rem;
}

.pa-item__code {
    font-size: 0.72rem;
    font-weight: 700;
    color: var(--dts-navy);
}

.pa-item__designation {
    font-size: 0.8rem;
    color: var(--dts-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.pa-item__meta {
    display: flex;
    justify-content: space-between;
    font-size: 0.7rem;
    color: var(--dts-muted);
}

.pa-badge {
    padding: 0.1rem 0.45rem;
    border-radius: 999px;
    font-size: 0.65rem;
    font-weight: 700;
}

.pa-badge--danger  { background: #fdecea; color: #a61b16; }
.pa-badge--warning { background: #fff4e5; color: #b26a00; }

.pa__loading {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 0.75rem;
}

.pa__loading .skeleton {
    height: 70px;
    border-radius: var(--dts-radius-sm);
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: paSk 1.4s infinite;
}

@keyframes paSk {
    from { background-position: 200% 0; }
    to   { background-position: -200% 0; }
}

@media (max-width: 992px) {
    .pa__grid, .pa__loading { grid-template-columns: repeat(3, 1fr); }
    .pa__panels { grid-template-columns: 1fr; }
}

@media (max-width: 576px) {
    .pa__grid, .pa__loading { grid-template-columns: repeat(2, 1fr); }
}
</style>

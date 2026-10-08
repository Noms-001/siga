<script setup lang="ts">
import type { DashboardPtaComparaison, DashboardPtaSection } from '@/types/dashboard'

defineProps<{
    comparaison: DashboardPtaComparaison | null
    loading?: boolean
}>()

function pct(s: DashboardPtaSection | undefined | null): number {
    if (!s) return 0
    return Math.round(s.avancement)
}
</script>

<template>
    <section class="pta">
        <header class="pta__head">
            <h2 class="pta__title">PTA / NON-PTA</h2>
        </header>

        <div v-if="loading" class="pta__loading">
            <div v-for="i in 2" :key="i" class="skeleton"></div>
        </div>

        <div v-else class="pta__grid">
            <article class="pta-card pta-card--pta">
                <header class="pta-card__head">
                    <span class="pta-card__tag">PTA</span>
                    <span class="pta-card__avancement">{{ pct(comparaison?.pta) }} %</span>
                </header>
                <div class="pta-card__total">
                    <strong>{{ comparaison?.pta?.total ?? 0 }}</strong>
                    <span>activité{{ (comparaison?.pta?.total ?? 0) > 1 ? 's' : '' }}</span>
                </div>
                <div class="progress pta-card__bar">
                    <div class="progress-bar"
                        :style="{ width: pct(comparaison?.pta) + '%' }"
                        role="progressbar"
                        :aria-valuenow="pct(comparaison?.pta)"
                        aria-valuemin="0" aria-valuemax="100"></div>
                </div>
                <dl class="pta-card__stats">
                    <div><dt>En cours</dt><dd>{{ comparaison?.pta?.enCours ?? 0 }}</dd></div>
                    <div><dt>Terminées</dt><dd>{{ comparaison?.pta?.terminees ?? 0 }}</dd></div>
                    <div><dt>En retard</dt><dd>{{ comparaison?.pta?.enRetard ?? 0 }}</dd></div>
                    <div><dt>Non comm.</dt><dd>{{ comparaison?.pta?.nonCommencees ?? 0 }}</dd></div>
                </dl>
            </article>

            <article class="pta-card pta-card--non-pta">
                <header class="pta-card__head">
                    <span class="pta-card__tag">NON-PTA</span>
                    <span class="pta-card__avancement">{{ pct(comparaison?.nonPta) }} %</span>
                </header>
                <div class="pta-card__total">
                    <strong>{{ comparaison?.nonPta?.total ?? 0 }}</strong>
                    <span>activité{{ (comparaison?.nonPta?.total ?? 0) > 1 ? 's' : '' }}</span>
                </div>
                <div class="progress pta-card__bar">
                    <div class="progress-bar"
                        :style="{ width: pct(comparaison?.nonPta) + '%' }"
                        role="progressbar"
                        :aria-valuenow="pct(comparaison?.nonPta)"
                        aria-valuemin="0" aria-valuemax="100"></div>
                </div>
                <dl class="pta-card__stats">
                    <div><dt>En cours</dt><dd>{{ comparaison?.nonPta?.enCours ?? 0 }}</dd></div>
                    <div><dt>Terminées</dt><dd>{{ comparaison?.nonPta?.terminees ?? 0 }}</dd></div>
                    <div><dt>En retard</dt><dd>{{ comparaison?.nonPta?.enRetard ?? 0 }}</dd></div>
                    <div><dt>Non comm.</dt><dd>{{ comparaison?.nonPta?.nonCommencees ?? 0 }}</dd></div>
                </dl>
            </article>
        </div>
    </section>
</template>

<style scoped>
.pta {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    padding: 1.25rem 1.5rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
}

.pta__title { font-size: 1rem; font-weight: 700; color: var(--dts-navy); margin: 0; }

.pta__grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
}

.pta-card {
    display: flex;
    flex-direction: column;
    gap: 0.65rem;
    padding: 1rem 1.15rem;
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius-sm);
    background: var(--dts-bg);
}

.pta-card--pta { border-left: 4px solid var(--dts-blue); }
.pta-card--non-pta { border-left: 4px solid #6b7b8d; }

.pta-card__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.pta-card__tag {
    font-size: 0.7rem;
    font-weight: 700;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    color: var(--dts-muted);
}

.pta-card__avancement {
    font-size: 1.15rem;
    font-weight: 700;
    color: var(--dts-navy);
}

.pta-card__total {
    display: flex;
    align-items: baseline;
    gap: 0.35rem;
}

.pta-card__total strong {
    font-size: 1.6rem;
    font-weight: 700;
    color: var(--dts-navy);
    line-height: 1;
}

.pta-card__total span {
    font-size: 0.82rem;
    color: var(--dts-muted);
}

.pta-card__bar { height: 8px; border-radius: 20px; background: var(--dts-surface); }
.pta-card--pta .progress-bar { background: var(--dts-blue); border-radius: 20px; }
.pta-card--non-pta .progress-bar { background: #6b7b8d; border-radius: 20px; }

.pta-card__stats {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.35rem 0.75rem;
    margin: 0;
    font-size: 0.78rem;
}

.pta-card__stats div { display: flex; justify-content: space-between; gap: 0.5rem; }
.pta-card__stats dt { color: var(--dts-muted); font-weight: 500; }
.pta-card__stats dd { margin: 0; color: var(--dts-navy); font-weight: 700; }

.pta__loading {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
}

.pta__loading .skeleton {
    height: 160px;
    border-radius: var(--dts-radius-sm);
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: ptaSk 1.4s infinite;
}

@keyframes ptaSk {
    from { background-position: 200% 0; }
    to   { background-position: -200% 0; }
}

@media (max-width: 768px) {
    .pta__grid, .pta__loading { grid-template-columns: 1fr; }
}
</style>
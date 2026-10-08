<script setup lang="ts">
import type { DashboardAvancement } from '@/types/dashboard'

defineProps<{
    avancement: DashboardAvancement | null
    loading?: boolean
}>()
</script>

<template>
    <section class="av-global">
        <header class="av-global__head">
            <h2 class="av-global__title">Avancement global</h2>
            <span class="av-global__percent">
                <template v-if="loading">…</template>
                <template v-else-if="avancement">
                    {{ Math.round(avancement.avancementGlobal) }} %
                </template>
                <template v-else>—</template>
            </span>
        </header>

        <div class="progress av-global__bar">
            <div class="progress-bar" role="progressbar"
                :style="{ width: (avancement?.avancementGlobal ?? 0) + '%' }"
                :aria-valuenow="avancement?.avancementGlobal ?? 0"
                aria-valuemin="0" aria-valuemax="100"></div>
        </div>

        <footer class="av-global__footer">
            <span>
                <strong>{{ avancement?.totalActivites ?? 0 }}</strong>
                activité{{ (avancement?.totalActivites ?? 0) > 1 ? 's' : '' }}
            </span>
            <span>
                <strong>{{ avancement?.totalSousActivites ?? 0 }}</strong>
                sous-activité{{ (avancement?.totalSousActivites ?? 0) > 1 ? 's' : '' }}
            </span>
            <span>
                <strong>{{ avancement?.sousActivitesTerminees ?? 0 }}</strong>
                terminée{{ (avancement?.sousActivitesTerminees ?? 0) > 1 ? 's' : '' }}
            </span>
        </footer>
    </section>
</template>

<style scoped>
.av-global {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    padding: 1.25rem 1.5rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    height: 100%;
}

.av-global__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;
}

.av-global__title {
    font-size: 1rem;
    font-weight: 700;
    color: var(--dts-navy);
    margin: 0;
}

.av-global__percent {
    font-size: 1.5rem;
    font-weight: 700;
    color: var(--dts-blue);
}

.av-global__bar {
    height: 14px;
    border-radius: 20px;
    background: var(--dts-bg);
}

.av-global__bar .progress-bar {
    background: var(--dts-blue);
    border-radius: 20px;
    transition: width 0.4s ease;
}

.av-global__footer {
    display: flex;
    gap: 1.5rem;
    flex-wrap: wrap;
    font-size: 0.82rem;
    color: var(--dts-muted);
}

.av-global__footer strong {
    color: var(--dts-navy);
    font-weight: 700;
    margin-right: 0.2rem;
}
</style>
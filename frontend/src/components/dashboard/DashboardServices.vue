<script setup lang="ts">
import type { DashboardService } from '@/types/dashboard'

defineProps<{
    services: DashboardService[]
    loading?: boolean
}>()
</script>

<template>
    <section class="services">
        <header class="services__head">
            <h2 class="services__title">Activités par service</h2>
        </header>

        <div v-if="loading" class="services__loading">
            <div v-for="i in 3" :key="i" class="skeleton"></div>
        </div>

        <div v-else-if="services.length === 0" class="services__empty">
            Aucun service à afficher.
        </div>

        <div v-else class="table-responsive">
            <table class="services__table">
                <thead>
                    <tr>
                        <th>Service</th>
                        <th class="text-center">Total</th>
                        <th class="text-center">En cours</th>
                        <th class="text-center">Terminées</th>
                        <th class="text-center">En retard</th>
                        <th class="text-center">Non comm.</th>
                        <th class="text-center">Avancement</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="s in services" :key="s.idService">
                        <td class="services__nom">{{ s.service }}</td>
                        <td class="text-center">{{ s.totalActivites }}</td>
                        <td class="text-center">{{ s.enCours }}</td>
                        <td class="text-center">{{ s.terminees }}</td>
                        <td class="text-center">
                            <span v-if="s.enRetard > 0" class="badge-danger">
                                {{ s.enRetard }}
                            </span>
                            <span v-else class="text-muted-custom">0</span>
                        </td>
                        <td class="text-center">{{ s.nonCommencees }}</td>
                        <td class="text-center">
                            <div class="services__av">
                                <span class="services__av-value">
                                    {{ Math.round(s.avancement) }} %
                                </span>
                                <div class="progress services__bar">
                                    <div class="progress-bar"
                                        :style="{ width: s.avancement + '%' }"
                                        role="progressbar"
                                        :aria-valuenow="s.avancement"
                                        aria-valuemin="0" aria-valuemax="100"></div>
                                </div>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </section>
</template>

<style scoped>
.services {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    padding: 1.25rem 1.5rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
}

.services__title {
    font-size: 1rem;
    font-weight: 700;
    color: var(--dts-navy);
    margin: 0;
}

.services__table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.85rem;
}

.services__table thead th {
    background: var(--dts-bg);
    padding: 0.6rem 0.75rem;
    text-align: left;
    font-size: 0.7rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--dts-muted);
    border-bottom: 1px solid var(--dts-border);
    white-space: nowrap;
}

.services__table tbody td {
    padding: 0.6rem 0.75rem;
    border-bottom: 1px solid var(--dts-border);
    color: var(--dts-text);
    vertical-align: middle;
}

.services__table tbody tr:last-child td { border-bottom: none; }
.services__table tbody tr:hover { background: var(--dts-blue-50); }

.services__nom { font-weight: 600; color: var(--dts-navy); }

.services__av {
    display: flex;
    flex-direction: column;
    gap: 0.2rem;
    align-items: center;
    min-width: 90px;
}

.services__av-value {
    font-size: 0.78rem;
    font-weight: 700;
    color: var(--dts-blue);
}

.services__bar {
    width: 100%;
    height: 6px;
    border-radius: 20px;
    background: var(--dts-bg);
}
.services__bar .progress-bar {
    background: var(--dts-blue);
    border-radius: 20px;
}

.badge-danger {
    display: inline-block;
    padding: 0.15rem 0.55rem;
    border-radius: 20px;
    background: #fdecea;
    color: #a61b16;
    border: 1px solid #f6cfcc;
    font-weight: 700;
    font-size: 0.72rem;
}

.text-muted-custom { color: var(--dts-muted); }

.services__empty {
    padding: 1rem;
    text-align: center;
    font-size: 0.85rem;
    color: var(--dts-muted);
}

.services__loading {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.services__loading .skeleton {
    height: 32px;
    border-radius: 6px;
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: svcSk 1.4s infinite;
}

@keyframes svcSk {
    from { background-position: 200% 0; }
    to   { background-position: -200% 0; }
}
</style>
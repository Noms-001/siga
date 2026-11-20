<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { DashboardEcheance, DashboardEcheances } from '@/types/dashboard'

defineProps<{
    echeances: DashboardEcheances | null
    loading?: boolean
}>()

const router = useRouter()

function versDetail(id: number): void {
    router.push({ name: 'activite-detail', params: { id } })
}

function formaterDate(iso: string): string {
    const d = new Date(iso)
    return Number.isNaN(d.getTime()) ? iso : d.toLocaleDateString('fr-FR')
}

function libelleJours(jours: number): string {
    if (jours > 0) return `${jours} j de retard`
    if (jours === 0) return "Aujourd'hui"
    return `dans ${Math.abs(jours)} j`
}

function classeEcheance(e: DashboardEcheance): string {
    if (e.jours >= 0) return 'echeance--retard'
    if (e.jours >= -3) return 'echeance--urgent'
    return 'echeance--proche'
}

function voirActivite(echeance: DashboardEcheance): void {
    versDetail(echeance.id)
}
</script>

<template>
    <section class="ech">
        <header class="ech__head">
            <h2 class="ech__title">Échéances</h2>
        </header>

        <div v-if="loading" class="ech__panels ech__loading">
            <section v-for="i in 2" :key="i" class="ech__panel">
                <div class="skeleton"></div>
            </section>
        </div>

        <div v-else class="ech__panels">
            <section class="ech__panel">
                <header class="ech__panel-head">
                    <span class="ech__panel-icon ech__panel-icon--danger">
                        <i class="bi bi-exclamation-triangle"></i>
                    </span>
                    <h3 class="ech__panel-title">Activités en retard</h3>
                    <span class="ech__panel-count">{{ (echeances?.enRetard ?? []).length }}</span>
                </header>

                <div v-if="(echeances?.enRetard ?? []).length === 0" class="ech__empty">
                    <i class="bi bi-check-circle"></i>
                    Aucune activité en retard.
                </div>

                <ul v-else class="ech__list" aria-label="Activités en retard">
                    <li v-for="e in echeances!.enRetard" :key="e.id"
                        class="ech__item">
                        <button type="button" class="ech__item-button" @click="voirActivite(e)"
                            :aria-label="`Ouvrir l'activité ${e.code}`">
                            <span class="ech__item-head">
                                <code class="ech__item-code">{{ e.code }}</code>
                                <span class="ech__badge" :class="classeEcheance(e)">{{ libelleJours(e.jours) }}</span>
                            </span>
                            <span class="ech__item-designation">{{ e.designation }}</span>
                            <span class="ech__item-meta">
                                <span>{{ formaterDate(e.dateEcheance) }}</span>
                                <span>{{ e.service || '—' }}</span>
                            </span>
                            <i class="bi bi-arrow-right ech__item-arrow" aria-hidden="true"></i>
                        </button>
                    </li>
                </ul>
            </section>

            <section class="ech__panel">
                <header class="ech__panel-head">
                    <span class="ech__panel-icon ech__panel-icon--warning">
                        <i class="bi bi-calendar-event"></i>
                    </span>
                    <h3 class="ech__panel-title">Échéances prochaines</h3>
                    <span class="ech__panel-count">{{ (echeances?.echeanceProche ?? []).length }}</span>
                </header>

                <div v-if="(echeances?.echeanceProche ?? []).length === 0" class="ech__empty">
                    <i class="bi bi-calendar-check"></i>
                    Aucune échéance prochaine.
                </div>

                <ul v-else class="ech__list" aria-label="Échéances prochaines">
                    <li v-for="e in echeances!.echeanceProche" :key="e.id"
                        class="ech__item">
                        <button type="button" class="ech__item-button" @click="voirActivite(e)"
                            :aria-label="`Ouvrir l'activité ${e.code}`">
                            <span class="ech__item-head">
                                <code class="ech__item-code">{{ e.code }}</code>
                                <span class="ech__badge" :class="classeEcheance(e)">{{ libelleJours(e.jours) }}</span>
                            </span>
                            <span class="ech__item-designation">{{ e.designation }}</span>
                            <span class="ech__item-meta">
                                <span>{{ formaterDate(e.dateEcheance) }}</span>
                                <span>{{ e.service || '—' }}</span>
                            </span>
                            <i class="bi bi-arrow-right ech__item-arrow" aria-hidden="true"></i>
                        </button>
                    </li>
                </ul>
            </section>
        </div>
    </section>
</template>

<style scoped>
.ech {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    padding: 1.25rem 1.5rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
}

.ech__title {
    margin: 0;
    color: var(--dts-navy);
    font-size: 1rem;
    font-weight: 700;
}

.ech__panels {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1rem;
}

.ech__panel {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    min-width: 0;
    padding: 1rem;
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius-sm);
    background: var(--dts-bg);
}

.ech__panel-head {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.ech__panel-title {
    margin: 0;
    color: var(--dts-navy);
    font-size: 0.85rem;
    font-weight: 700;
}

.ech__panel-icon {
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

.ech__panel-icon--danger { background: #fdecea; color: #a61b16; }
.ech__panel-icon--warning { background: #fff4e5; color: #b26a00; }

.ech__panel-count {
    margin-left: auto;
    color: var(--dts-muted);
    font-size: 0.72rem;
    font-weight: 700;
}

.ech__list {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    max-height: 260px;
    margin: 0;
    padding: 0 0.25rem 0 0;
    overflow-y: auto;
    list-style: none;
    scrollbar-gutter: stable;
}

.ech__item { min-width: 0; }

.ech__item-button {
    position: relative;
    display: flex;
    flex-direction: column;
    width: 100%;
    gap: 0.25rem;
    padding: 0.6rem 2rem 0.6rem 0.75rem;
    border: 1px solid var(--dts-border);
    border-radius: 8px;
    background: var(--dts-surface);
    text-align: left;
    cursor: pointer;
    transition: border-color .15s ease, box-shadow .15s ease;
}

.ech__item-button:hover,
.ech__item-button:focus-visible {
    border-color: var(--dts-blue);
    box-shadow: 0 2px 8px rgba(26, 42, 108, .08);
    outline: none;
}

.ech__item-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.5rem;
}

.ech__item-code {
    min-width: 0;
    color: var(--dts-navy);
    font-size: 0.72rem;
    font-weight: 700;
}

.ech__item-designation {
    overflow: hidden;
    color: var(--dts-text);
    font-size: 0.8rem;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.ech__item-meta {
    display: flex;
    justify-content: space-between;
    gap: 0.75rem;
    color: var(--dts-muted);
    font-size: 0.7rem;
}

.ech__item-meta span:last-child {
    overflow: hidden;
    text-align: right;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.ech__badge {
    flex: 0 0 auto;
    padding: 0.1rem 0.45rem;
    border-radius: 999px;
    font-size: 0.65rem;
    font-weight: 700;
}

.echeance--retard { background: #fdecea; color: #a61b16; }
.echeance--urgent { background: #fff4e5; color: #b26a00; }
.echeance--proche { background: #e8f1fb; color: var(--dts-blue); }

.ech__item-arrow {
    position: absolute;
    top: 50%;
    right: 0.7rem;
    transform: translateY(-50%);
    color: var(--dts-blue);
    font-size: 0.85rem;
}

.ech__empty {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 0;
    color: var(--dts-muted);
    font-size: 0.8rem;
}

.ech__loading { min-height: 180px; }

.ech__loading .skeleton {
    flex: 1;
    min-height: 150px;
    border-radius: 8px;
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: eSk 1.4s infinite;
}

@keyframes eSk {
    from { background-position: 200% 0; }
    to { background-position: -200% 0; }
}

@media (max-width: 768px) {
    .ech__panels { grid-template-columns: 1fr; }
}
</style>
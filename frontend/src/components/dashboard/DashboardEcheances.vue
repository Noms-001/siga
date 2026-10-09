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
</script>

<template>
    <section class="ech">
        <header class="ech__head">
            <h2 class="ech__title">Échéances</h2>
        </header>

        <div v-if="loading" class="ech__loading">
            <div v-for="i in 2" :key="i" class="skeleton"></div>
        </div>

        <div v-else class="ech__grid">
            <div class="ech__block">
                <header class="ech__block-head">
                    <span class="ech__block-title ech__block-title--danger">
                        En retard
                    </span>
                    <span class="ech__block-count">
                        {{ (echeances?.enRetard ?? []).length }}
                    </span>
                </header>

                <div v-if="(echeances?.enRetard ?? []).length === 0"
                    class="ech__empty">Aucun retard.</div>

                <ul v-else class="ech__list">
                    <li v-for="e in echeances!.enRetard" :key="e.id"
                        class="ech__item" :class="classeEcheance(e)">
                        <div class="ech__item-main">
                            <span class="ech__item-code">{{ e.code }}</span>
                            <span class="ech__item-desig">{{ e.designation }}</span>
                            <span class="ech__item-service">{{ e.service || '—' }}</span>
                        </div>
                        <div class="ech__item-side">
                            <span class="ech__item-date">{{ formaterDate(e.dateEcheance) }}</span>
                            <span class="ech__item-delai">{{ libelleJours(e.jours) }}</span>
                        </div>
                        <button type="button" class="ech__btn"
                            @click="versDetail(e.id)" title="Voir l'activité">
                            <i class="bi bi-arrow-right"></i>
                        </button>
                    </li>
                </ul>
            </div>

            <div class="ech__block">
                <header class="ech__block-head">
                    <span class="ech__block-title ech__block-title--warn">
                        Échéances proches
                    </span>
                    <span class="ech__block-count">
                        {{ (echeances?.echeanceProche ?? []).length }}
                    </span>
                </header>

                <div v-if="(echeances?.echeanceProche ?? []).length === 0"
                    class="ech__empty">Aucune échéance proche.</div>

                <ul v-else class="ech__list">
                    <li v-for="e in echeances!.echeanceProche" :key="e.id"
                        class="ech__item" :class="classeEcheance(e)">
                        <div class="ech__item-main">
                            <span class="ech__item-code">{{ e.code }}</span>
                            <span class="ech__item-desig">{{ e.designation }}</span>
                            <span class="ech__item-service">{{ e.service || '—' }}</span>
                        </div>
                        <div class="ech__item-side">
                            <span class="ech__item-date">{{ formaterDate(e.dateEcheance) }}</span>
                            <span class="ech__item-delai">{{ libelleJours(e.jours) }}</span>
                        </div>
                        <button type="button" class="ech__btn"
                            @click="versDetail(e.id)" title="Voir l'activité">
                            <i class="bi bi-arrow-right"></i>
                        </button>
                    </li>
                </ul>
            </div>
        </div>
    </section>
</template>

<style scoped>
.ech {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    padding: 1.25rem 1.5rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
}

.ech__title { font-size: 1rem; font-weight: 700; color: var(--dts-navy); margin: 0; }

.ech__grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1.25rem;
}

.ech__block {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.ech__block-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 0.35rem;
    border-bottom: 1px solid var(--dts-border);
}

.ech__block-title {
    font-size: 0.78rem;
    font-weight: 700;
    letter-spacing: 0.04em;
    text-transform: uppercase;
}
.ech__block-title--danger { color: #a61b16; }
.ech__block-title--warn   { color: #9a6409; }

.ech__block-count {
    font-size: 0.75rem;
    font-weight: 700;
    padding: 0.1rem 0.55rem;
    border-radius: 20px;
    background: var(--dts-bg);
    color: var(--dts-muted);
}

.ech__list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
}

.ech__item {
    display: grid;
    grid-template-columns: 1fr auto auto;
    gap: 0.75rem;
    align-items: center;
    padding: 0.55rem 0.6rem;
    border-radius: 6px;
    border-left: 3px solid var(--dts-border-2);
    transition: background-color 0.15s ease;
}
.ech__item:hover { background: var(--dts-bg); }

.ech__item.echeance--retard   { border-left-color: #d64545; }
.ech__item.echeance--urgent   { border-left-color: #d98324; }
.ech__item.echeance--proche   { border-left-color: var(--dts-blue); }

.ech__item-main {
    display: flex;
    flex-direction: column;
    min-width: 0;
}

.ech__item-code {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.75rem;
    color: var(--dts-blue);
    font-weight: 700;
}

.ech__item-desig {
    font-size: 0.85rem;
    color: var(--dts-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.ech__item-service {
    font-size: 0.72rem;
    color: var(--dts-muted);
}

.ech__item-side {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 0.1rem;
    font-size: 0.75rem;
}

.ech__item-date { color: var(--dts-text); }
.ech__item-delai { font-weight: 700; color: var(--dts-muted); }

.ech__item.echeance--retard .ech__item-delai   { color: #a61b16; }
.ech__item.echeance--urgent .ech__item-delai   { color: #9a6409; }

.ech__btn {
    width: 26px; height: 26px;
    display: inline-flex; align-items: center; justify-content: center;
    border-radius: 6px;
    border: 1px solid var(--dts-border);
    background: transparent;
    color: var(--dts-blue);
    cursor: pointer;
}
.ech__btn:hover { background: var(--dts-blue-50); border-color: #bdd3e6; }

.ech__empty {
    padding: 0.75rem;
    font-size: 0.82rem;
    color: var(--dts-muted);
    text-align: center;
}

.ech__loading {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
}
.ech__loading .skeleton {
    height: 120px;
    border-radius: var(--dts-radius-sm);
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: eSk 1.4s infinite;
}
@keyframes eSk {
    from { background-position: 200% 0; }
    to   { background-position: -200% 0; }
}

@media (max-width: 768px) {
    .ech__grid, .ech__loading { grid-template-columns: 1fr; }
}
</style>
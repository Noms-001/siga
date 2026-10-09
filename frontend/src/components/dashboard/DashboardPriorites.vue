<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { DashboardPriorite } from '@/types/dashboard'

defineProps<{
    priorites: DashboardPriorite[]
    loading?: boolean
}>()

const router = useRouter()

function versDetail(id: number): void {
    // Réutilise la route existante du détail d'activité.
    router.push({ name: 'activite-detail', params: { id } })
}

function classePriorite(code: string | null): string {
    switch ((code ?? '').toUpperCase()) {
        case 'CRITIQUE': return 'prio--critique'
        case 'HAUTE': return 'prio--haute'
        case 'NORMALE': return 'prio--normale'
        case 'FAIBLE': return 'prio--faible'
        default: return 'prio--autre'
    }
}

function classeStatut(code: string | null): string {
    switch ((code ?? '').toUpperCase()) {
        case 'TERMINEE': return 'st--terminee'
        case 'EN_COURS': return 'st--en-cours'
        case 'NON_COMMENCEE': return 'st--non-commencee'
        case 'EN_RETARD': return 'st--retard'
        case 'REPORTEE': return 'st--reportee'
        case 'SUSPENDUE': return 'st--suspendue'
        default: return 'st--autre'
    }
}

function formaterDate(iso: string | null): string {
    if (!iso) return '—'
    const d = new Date(iso)
    return Number.isNaN(d.getTime()) ? iso : d.toLocaleDateString('fr-FR')
}
</script>

<template>
    <section class="prio">
        <header class="prio__head">
            <h2 class="prio__title">Activités prioritaires</h2>
        </header>

        <div v-if="loading" class="prio__loading">
            <div v-for="i in 4" :key="i" class="skeleton"></div>
        </div>

        <div v-else-if="priorites.length === 0" class="prio__empty">
            Aucune activité à traiter en priorité.
        </div>

        <div v-else class="table-responsive">
            <table class="prio__table">
                <thead>
                    <tr>
                        <th>Code</th>
                        <th>Désignation</th>
                        <th>Service</th>
                        <th class="text-center">Priorité</th>
                        <th class="text-center">Statut</th>
                        <th class="text-center">Échéance</th>
                        <th class="text-center">Avancement</th>
                        <th></th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="p in priorites" :key="p.id">
                        <td class="prio__code">{{ p.code }}</td>
                        <td class="prio__desig">
                            {{ p.designation }}
                            <span v-if="p.reference" class="prio__ref">{{ p.reference }}</span>
                        </td>
                        <td class="prio__service">{{ p.service || '—' }}</td>
                        <td class="text-center">
                            <span v-if="p.prioriteCode"
                                class="badge-prio" :class="classePriorite(p.prioriteCode)">
                                {{ p.priorite || p.prioriteCode }}
                            </span>
                            <span v-else class="text-muted-custom">—</span>
                        </td>
                        <td class="text-center">
                            <span v-if="p.statut"
                                class="badge-st" :class="classeStatut(p.statut)">
                                {{ p.statutLibelle || p.statut }}
                            </span>
                            <span v-else class="text-muted-custom">—</span>
                        </td>
                        <td class="text-center prio__date">
                            {{ formaterDate(p.dateEcheance) }}
                            <span v-if="p.joursRetard > 0" class="prio__retard">
                                {{ p.joursRetard }} j de retard
                            </span>
                        </td>
                        <td class="text-center prio__av">
                            {{ Math.round(p.avancement) }} %
                        </td>
                        <td class="text-end">
                            <button type="button" class="prio__action"
                                title="Voir l'activité" @click="versDetail(p.id)">
                                <i class="bi bi-arrow-right"></i>
                            </button>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </section>
</template>

<style scoped>
.prio {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    padding: 1.25rem 1.5rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
}

.prio__title { font-size: 1rem; font-weight: 700; color: var(--dts-navy); margin: 0; }

.prio__table { width: 100%; border-collapse: collapse; font-size: 0.85rem; }

.prio__table thead th {
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

.prio__table tbody td {
    padding: 0.6rem 0.75rem;
    border-bottom: 1px solid var(--dts-border);
    color: var(--dts-text);
    vertical-align: middle;
}

.prio__table tbody tr:last-child td { border-bottom: none; }
.prio__table tbody tr:hover { background: var(--dts-blue-50); }

.prio__code {
    font-family: ui-monospace, Menlo, monospace;
    font-weight: 700;
    color: var(--dts-navy);
    white-space: nowrap;
}

.prio__desig { font-weight: 600; color: var(--dts-text); }
.prio__ref { display: block; font-size: 0.72rem; color: var(--dts-muted); font-weight: 400; }

.prio__service { color: var(--dts-text); font-size: 0.82rem; }
.prio__date { white-space: nowrap; font-size: 0.82rem; }
.prio__retard {
    display: block;
    font-size: 0.7rem;
    color: #a61b16;
    font-weight: 700;
}
.prio__av { font-weight: 700; color: var(--dts-blue); }

.badge-prio, .badge-st {
    display: inline-block;
    padding: 0.2rem 0.6rem;
    border-radius: 20px;
    font-size: 0.7rem;
    font-weight: 700;
}

.prio--critique { background: #fdecea; color: #a61b16; border: 1px solid #f6cfcc; }
.prio--haute    { background: #fdf3e2; color: #9a6409; border: 1px solid #f4e2c2; }
.prio--normale  { background: #e4f0fb; color: #135d95; border: 1px solid #cde2f6; }
.prio--faible   { background: #eef1f5; color: #5b6b7d; border: 1px solid #dfe5ec; }
.prio--autre    { background: #eef1f5; color: #5b6b7d; border: 1px solid #dfe5ec; }

.st--terminee      { background: #e6f4ea; color: #1e7e34; border: 1px solid #c8e5cf; }
.st--en-cours      { background: #e4f0fb; color: #135d95; border: 1px solid #cde2f6; }
.st--non-commencee { background: #eef1f5; color: #5b6b7d; border: 1px solid #dfe5ec; }
.st--retard        { background: #fdecea; color: #a61b16; border: 1px solid #f6cfcc; }
.st--reportee      { background: #fdf3e2; color: #9a6409; border: 1px solid #f4e2c2; }
.st--suspendue     { background: #eef1f5; color: #5b6b7d; border: 1px solid #dfe5ec; }
.st--autre         { background: #eef1f5; color: #5b6b7d; border: 1px solid #dfe5ec; }

.prio__action {
    width: 28px; height: 28px;
    display: inline-flex; align-items: center; justify-content: center;
    border-radius: 6px;
    border: 1px solid var(--dts-border);
    background: transparent;
    color: var(--dts-blue);
    cursor: pointer;
    transition: background-color 0.15s ease, border-color 0.15s ease;
}
.prio__action:hover { background: var(--dts-blue-50); border-color: #bdd3e6; }

.text-muted-custom { color: var(--dts-muted); }

.prio__empty {
    padding: 1.5rem 1rem;
    text-align: center;
    font-size: 0.85rem;
    color: var(--dts-muted);
}

.prio__loading {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}
.prio__loading .skeleton {
    height: 32px;
    border-radius: 6px;
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: pSk 1.4s infinite;
}
@keyframes pSk {
    from { background-position: 200% 0; }
    to   { background-position: -200% 0; }
}
</style>
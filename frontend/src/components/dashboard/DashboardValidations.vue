<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { DashboardValidation } from '@/types/dashboard'

defineProps<{
    validations: DashboardValidation[]
    loading?: boolean
}>()

const router = useRouter()

function versDetail(idActivite: number): void {
    router.push({ name: 'activite-detail', params: { id: idActivite } })
}

function formaterDate(iso: string): string {
    const d = new Date(iso)
    return Number.isNaN(d.getTime()) ? iso : d.toLocaleString('fr-FR')
}
</script>

<template>
    <section class="valid">
        <header class="valid__head">
            <h2 class="valid__title">Validations en attente</h2>
            <span class="valid__count" v-if="!loading">
                {{ validations.length }} demande{{ validations.length > 1 ? 's' : '' }}
            </span>
        </header>

        <div v-if="loading" class="valid__loading">
            <div v-for="i in 3" :key="i" class="skeleton"></div>
        </div>

        <div v-else-if="validations.length === 0" class="valid__empty">
            Aucune validation en attente.
        </div>

        <ul v-else class="valid__list">
            <li v-for="v in validations" :key="v.idValidation" class="valid__item">
                <div class="valid__main">
                    <div class="valid__code">{{ v.codeActivite }}</div>
                    <div class="valid__designation">{{ v.designationActivite }}</div>
                </div>

                <div class="valid__meta">
                    <div class="valid__field">
                        <span class="valid__label">Étape</span>
                        <span class="valid__value">
                            <span v-if="v.niveau != null" class="valid__niveau">
                                N{{ v.niveau }}
                            </span>
                            {{ v.etape || '—' }}
                        </span>
                    </div>
                    <div class="valid__field">
                        <span class="valid__label">Demandeur</span>
                        <span class="valid__value">{{ v.demandeur || '—' }}</span>
                    </div>
                    <div class="valid__field">
                        <span class="valid__label">Date de demande</span>
                        <span class="valid__value">{{ formaterDate(v.dateDemande) }}</span>
                    </div>
                </div>

                <div class="valid__actions">
                    <span class="valid__badge">En attente</span>
                    <button type="button" class="valid__btn"
                        @click="versDetail(v.idActivite)"
                        title="Voir l'activité">
                        <i class="bi bi-arrow-right"></i>
                    </button>
                </div>
            </li>
        </ul>
    </section>
</template>

<style scoped>
.valid {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    padding: 1.25rem 1.5rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
}

.valid__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;
}

.valid__title { font-size: 1rem; font-weight: 700; color: var(--dts-navy); margin: 0; }
.valid__count { font-size: 0.78rem; color: var(--dts-muted); }

.valid__list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
}

.valid__item {
    display: grid;
    grid-template-columns: minmax(180px, 1.2fr) 2fr auto;
    gap: 1rem;
    align-items: center;
    padding: 0.75rem 0;
    border-bottom: 1px solid var(--dts-border);
}

.valid__item:last-child { border-bottom: none; }

.valid__code {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.78rem;
    color: var(--dts-blue);
    font-weight: 700;
}

.valid__designation {
    font-size: 0.88rem;
    font-weight: 600;
    color: var(--dts-text);
    margin-top: 0.1rem;
}

.valid__meta {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 0.5rem 1rem;
}

.valid__field { display: flex; flex-direction: column; gap: 0.1rem; }

.valid__label {
    font-size: 0.68rem;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--dts-muted);
    font-weight: 700;
}

.valid__value {
    font-size: 0.82rem;
    color: var(--dts-text);
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
}

.valid__niveau {
    display: inline-block;
    font-size: 0.68rem;
    font-weight: 700;
    background: var(--dts-navy);
    color: #fff;
    padding: 0.05rem 0.45rem;
    border-radius: 20px;
}

.valid__actions {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    justify-content: flex-end;
}

.valid__badge {
    display: inline-block;
    padding: 0.2rem 0.6rem;
    border-radius: 20px;
    background: #fdf3e2;
    color: #9a6409;
    border: 1px solid #f4e2c2;
    font-size: 0.72rem;
    font-weight: 700;
}

.valid__btn {
    width: 28px; height: 28px;
    display: inline-flex; align-items: center; justify-content: center;
    border-radius: 6px;
    border: 1px solid var(--dts-border);
    background: transparent;
    color: var(--dts-blue);
    cursor: pointer;
    transition: background-color 0.15s ease;
}
.valid__btn:hover { background: var(--dts-blue-50); border-color: #bdd3e6; }

.valid__empty {
    padding: 1.5rem 1rem;
    text-align: center;
    font-size: 0.85rem;
    color: var(--dts-muted);
}

.valid__loading {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}
.valid__loading .skeleton {
    height: 44px;
    border-radius: 6px;
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: vSk 1.4s infinite;
}
@keyframes vSk {
    from { background-position: 200% 0; }
    to   { background-position: -200% 0; }
}

@media (max-width: 768px) {
    .valid__item {
        grid-template-columns: 1fr;
        gap: 0.5rem;
    }
    .valid__meta { grid-template-columns: 1fr 1fr; }
    .valid__actions { justify-content: flex-start; }
}
</style>
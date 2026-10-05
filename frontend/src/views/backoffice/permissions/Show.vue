<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseButton from '@/components/base/BaseButton/BaseButton.vue'
import { getPermission } from '@/services/permission'
import {
    formatPortee,
    type PermissionDetail,
} from '@/types/backoffice/permission'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)

const item = ref<PermissionDetail | null>(null)
const loading = ref(false)
const errorMessage = ref('')

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await getPermission(id)
    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    item.value = res.data
}

onMounted(charger)
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">Détail de la permission</h1>
                <p class="bo-page__subtitle">Consultation des informations</p>
            </div>
            <div class="bo-page__actions">
                <BaseButton variant="secondary" icon="bi bi-arrow-left"
                    @click="router.push({ name: 'backoffice-permissions' })">
                    Retour
                </BaseButton>
            </div>
        </header>

        <div v-if="errorMessage"
            class="alert alert-danger mb-0 d-flex justify-content-between align-items-center"
            role="alert">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="charger">Réessayer</BaseButton>
        </div>

        <div v-else-if="loading" class="bo-detail">
            <div class="skeleton"></div>
            <div class="skeleton short"></div>
            <div class="skeleton"></div>
        </div>

        <template v-else-if="item">
            <!-- Fiche permission -->
            <div class="bo-detail">
                <div class="bo-detail__row">
                    <span class="bo-detail__label">Désignation</span>
                    <span class="bo-detail__value">{{ item.designation }}</span>
                </div>
                <div class="bo-detail__row">
                    <span class="bo-detail__label">Ressource</span>
                    <span class="bo-detail__value">
                        <span class="badge-ressource">{{ item.ressource }}</span>
                    </span>
                </div>
                <div class="bo-detail__row">
                    <span class="bo-detail__label">Action</span>
                    <span class="bo-detail__value">
                        <code class="text-action">{{ item.action }}</code>
                    </span>
                </div>
                <div class="bo-detail__row">
                    <span class="bo-detail__label">Statut</span>
                    <span class="bo-detail__value">
                        <span class="badge" :class="item.actif ? 'badge-actif' : 'badge-inactif'">
                            {{ item.actif ? 'Active' : 'Inactive' }}
                        </span>
                    </span>
                </div>
                <div class="bo-detail__row">
                    <span class="bo-detail__label">Description</span>
                    <span class="bo-detail__value">{{ item.description || '—' }}</span>
                </div>
                <div v-if="item.dateDesactivation" class="bo-detail__row">
                    <span class="bo-detail__label">Désactivée le</span>
                    <span class="bo-detail__value">
                        {{ new Date(item.dateDesactivation).toLocaleString('fr-FR') }}
                    </span>
                </div>
            </div>

            <!-- Postes utilisant cette permission -->
            <section class="bo-section">
                <header class="bo-section__head">
                    <h2 class="bo-section__title">Postes utilisant cette permission</h2>
                    <span class="bo-section__count">
                        {{ item.postes.length }} poste{{ item.postes.length > 1 ? 's' : '' }}
                    </span>
                </header>

                <div v-if="item.postes.length === 0" class="bo-empty">
                    Aucun poste n'utilise cette permission.
                </div>

                <div v-else class="bo-table-wrapper">
                    <table class="bo-table">
                        <thead>
                            <tr>
                                <th>Poste</th>
                                <th>Portée</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="p in item.postes" :key="p.idPoste">
                                <td>{{ p.nom }}</td>
                                <td>
                                    <span class="badge-portee" :class="p.portee ? `portee-${p.portee.toLowerCase()}` : 'portee-none'">
                                        {{ formatPortee(p.portee) }}
                                    </span>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>
        </template>
    </div>
</template>

<style scoped>
.bo-page { padding: 1.5rem; display: flex; flex-direction: column; gap: 1.25rem; }

.bo-page__header { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem; }
.bo-page__actions { display: flex; gap: 0.5rem; }
.bo-page__title { font-size: 1.4rem; font-weight: 700; color: var(--dts-navy); margin: 0; }
.bo-page__subtitle { color: var(--dts-muted); margin: 0; font-size: 0.85rem; }

.bo-detail {
    max-width: 720px;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    padding: 1.25rem 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.bo-detail__row {
    display: grid;
    grid-template-columns: 160px 1fr;
    gap: 1rem;
    padding-bottom: 0.65rem;
    border-bottom: 1px solid var(--dts-border);
}
.bo-detail__row:last-child { border-bottom: none; padding-bottom: 0; }

.bo-detail__label {
    font-size: 0.78rem; font-weight: 600; text-transform: uppercase;
    letter-spacing: 0.05em; color: var(--dts-muted); padding-top: 0.15rem;
}
.bo-detail__value { color: var(--dts-text); word-break: break-word; }

/* --- Section "Postes utilisant cette permission" --- */

.bo-section {
    max-width: 720px;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    padding: 1.25rem 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.bo-section__head {
    display: flex; justify-content: space-between; align-items: center;
    gap: 1rem; flex-wrap: wrap;
}

.bo-section__title {
    margin: 0; font-size: 1rem; font-weight: 700; color: var(--dts-navy);
}

.bo-section__count {
    font-size: 0.75rem; font-weight: 600;
    background: var(--dts-blue-50); color: var(--dts-blue);
    padding: 0.15rem 0.6rem; border-radius: 20px;
    border: 1px solid var(--dts-border);
}

.bo-empty {
    padding: 1rem;
    text-align: center;
    font-size: 0.85rem;
    color: var(--dts-muted);
    background: var(--dts-bg);
    border: 1px dashed var(--dts-border-2);
    border-radius: var(--dts-radius-sm);
}

.bo-table-wrapper {
    overflow-x: auto;
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius-sm);
}

.bo-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.85rem;
}

.bo-table thead th {
    background: var(--dts-bg);
    padding: 0.6rem 0.9rem;
    text-align: left;
    font-size: 0.7rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--dts-muted);
    border-bottom: 1px solid var(--dts-border);
}

.bo-table tbody td {
    padding: 0.65rem 0.9rem;
    color: var(--dts-text);
    border-bottom: 1px solid var(--dts-border);
}

.bo-table tbody tr:last-child td { border-bottom: none; }
.bo-table tbody tr:hover { background: var(--dts-blue-50); }

/* --- Badges --- */

.badge-ressource {
    display: inline-block; padding: 0.15rem 0.55rem;
    border-radius: 4px; background: #e4f0fb; color: #135d95; border: 1px solid #cde2f6;
    font-size: 0.72rem; font-weight: 600;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.text-action {
    font-size: 0.78rem; padding: 0.15rem 0.5rem;
    background: #eef1f5; color: #384a5e; border-radius: 4px;
}

.badge-actif, .badge-inactif {
    font-weight: 600; padding: 0.25rem 0.7rem; border-radius: 20px; font-size: 0.7rem;
}
.badge-actif { background: #e6f4ea; color: #1e7e34; border: 1px solid #c8e5cf; }
.badge-inactif { background: #eef1f5; color: #5b6b7d; border: 1px solid #dfe5ec; }

.badge-portee {
    display: inline-block;
    font-size: 0.7rem; font-weight: 600;
    padding: 0.2rem 0.65rem; border-radius: 20px;
}

.portee-utilisateur  { background: #e4f0fb; color: #135d95; border: 1px solid #cde2f6; }
.portee-service      { background: #e6f4ea; color: #1e7e34; border: 1px solid #c8e5cf; }
.portee-departement  { background: #fdf3e2; color: #9a6409; border: 1px solid #f4e2c2; }
.portee-tous         { background: #e9e6fb; color: #4338a1; border: 1px solid #d5d0f3; }
.portee-none         { background: #eef1f5; color: #5b6b7d; border: 1px solid #dfe5ec; }

/* --- Skeleton --- */

.skeleton {
    height: 16px; border-radius: 6px;
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%; animation: sk 1.4s infinite;
}
.skeleton.short { width: 40%; }
@keyframes sk { from { background-position: 200% 0; } to { background-position: -200% 0; } }

@media (max-width: 576px) {
    .bo-page { padding: 1rem; }
    .bo-detail__row { grid-template-columns: 1fr; gap: 0.25rem; }
    .bo-detail, .bo-section { padding: 1rem; }
}
</style>
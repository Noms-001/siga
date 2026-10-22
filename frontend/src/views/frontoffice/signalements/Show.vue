<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseButton from '@/components/base/BaseButton/BaseButton.vue'
import { getSignalement } from '@/services/signalement'
import type { Signalement, SignalementDetail } from '@/types/backoffice/signalement'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)

const loading = ref(false)
const errorMessage = ref('')

const item = ref<SignalementDetail | null>(null)

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await getSignalement(id)
    loading.value = false
    if (!res.success) { errorMessage.value = res.error; return }
    item.value = res.data
}

/* -------------------- en-tête -------------------- */

const dateDuJour = computed(() =>
    new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric',
    })
        .format(new Date())
        .toUpperCase()
)

const classeType = computed(() => {
    const t = (item.value?.typeOrigine ?? '').toUpperCase()
    if (t === 'INCIDENT') return 'badge-type--incident'
    if (t === 'RISQUE') return 'badge-type--risque'
    return 'badge-type--autre'
})

const declarant = computed(() => {
    const i = item.value
    if (!i) return '—'
    const parts = [i.prenomUtilisateur, i.nomUtilisateur].filter(Boolean)
    return parts.length ? parts.join(' ') : '—'
})

const nbPlans = computed(() => item.value?.plansAction.length ?? 0)

function formaterDateHeure(iso: string | null | undefined): string {
    if (!iso) return '—'
    const d = new Date(iso)
    return Number.isNaN(d.getTime()) ? iso : d.toLocaleString('fr-FR')
}

function formaterDate(iso: string | null | undefined): string {
    if (!iso) return '—'
    const d = new Date(iso)
    return Number.isNaN(d.getTime()) ? iso : d.toLocaleDateString('fr-FR')
}

function v(value: unknown): string {
    if (value === null || value === undefined) return '—'
    const s = String(value).trim()
    return s === '' ? '—' : s
}

onMounted(charger)
</script>

<template>
    <div class="sig-page">
        <!-- ============ EN-TÊTE ============ -->
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>{{ item?.designation || 'Détail du signalement' }}</h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    <template v-if="item">
                        <code class="text-code text-code--inline">{{ item.code }}</code>
                        <span class="subheading__sep">—</span>
                        <span>{{ item.typeOrigine }}</span>
                    </template>
                    <template v-else>Consultation d'un signalement</template>
                </p>
            </div>

            <div class="welcome-row__right">
                <BaseButton variant="secondary" icon="bi bi-arrow-left" @click="router.push({ name: 'signalements' })">
                    Retour à la liste
                </BaseButton>
                <BaseButton variant="secondary" :loading="loading" :disabled="loading" @click="charger">
                    <i class="bi bi-arrow-clockwise"></i>
                    Actualiser
                </BaseButton>
            </div>
        </header>

        <!-- ============ ERREUR ============ -->
        <div v-if="errorMessage" class="alert alert-danger mb-0 d-flex justify-content-between align-items-center" role="alert">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="charger">Réessayer</BaseButton>
        </div>

        <!-- ============ CHARGEMENT ============ -->
        <div v-else-if="loading" class="loading-state">
            <div class="hero-skeleton">
                <div class="skeleton skeleton--badge"></div>
                <div class="skeleton skeleton--title"></div>
                <div class="skeleton skeleton--meta"></div>
            </div>
            <div class="content-skeleton">
                <div class="skeleton skeleton--line skeleton--short"></div>
                <div class="skeleton skeleton--line"></div>
                <div class="skeleton skeleton--line skeleton--medium"></div>
            </div>
        </div>

        <!-- ============ CONTENU ============ -->
        <template v-else-if="item">

            <!-- ============ HERO : identification principale ============ -->
            <section class="hero" aria-labelledby="hero-designation">
                <div class="hero__badges">
                    <span class="badge-type" :class="classeType">
                        {{ item.typeOrigine }}
                    </span>
                </div>

                <div class="hero__body">
                    <span class="hero__label">Désignation</span>
                    <h2 id="hero-designation" class="hero__designation">
                        {{ item.designation }}
                    </h2>

                    <div class="hero__code-row">
                        <code class="text-code">{{ item.code }}</code>
                    </div>

                    <div class="hero__meta">
                        <span class="hero__meta-item">
                            <i class="bi bi-person" aria-hidden="true"></i>
                            {{ declarant }}
                        </span>
                        <span class="hero__meta-item">
                            <i class="bi bi-calendar3" aria-hidden="true"></i>
                            {{ formaterDateHeure(item.dateCreation) }}
                        </span>
                    </div>
                </div>
            </section>

            <!-- ============ DESCRIPTION ============ -->
            <section class="content-card" aria-labelledby="description-title">
                <header class="content-card__head">
                    <span class="content-card__icon" aria-hidden="true">
                        <i class="bi bi-file-text"></i>
                    </span>
                    <h2 id="description-title" class="content-card__title">Description</h2>
                </header>
                <div class="content-card__body">
                    <div v-if="item.description" class="text-block">
                        <p class="text-block__content">{{ item.description }}</p>
                    </div>
                    <div v-else class="content-card__empty">
                        <i class="bi bi-file-earmark-x" aria-hidden="true"></i>
                        <p>Aucune description n'a été renseignée pour ce signalement.</p>
                    </div>
                </div>
            </section>

            <!-- ============ PLANS D'ACTION ============ -->
            <section class="content-card" aria-labelledby="plans-title">
                <header class="content-card__head">
                    <span class="content-card__icon" aria-hidden="true">
                        <i class="bi bi-diagram-3"></i>
                    </span>
                    <h2 id="plans-title" class="content-card__title">Plans d'action rattachés</h2>
                    <span class="content-card__count">{{ nbPlans }}</span>
                </header>

                <div class="content-card__body">
                    <div v-if="nbPlans === 0" class="content-card__empty">
                        <i class="bi bi-info-circle" aria-hidden="true"></i>
                        <p>Aucun plan d'action n'est rattaché à ce signalement.</p>
                    </div>

                    <div v-else class="plans-list">
                        <article
                            v-for="p in item.plansAction"
                            :key="p.id"
                            class="plan-item"
                            :class="{ 'plan-item--principale': p.estPrincipale }"
                        >
                            <div class="plan-item__marker" aria-hidden="true">
                                <i class="bi bi-clipboard-check"></i>
                            </div>

                            <div class="plan-item__body">
                                <div class="plan-item__head">
                                    <code class="plan-item__code">{{ p.code }}</code>
                                    <span v-if="p.estPrincipale" class="plan-item__badge">
                                        <i class="bi bi-star-fill" aria-hidden="true"></i>
                                        Principale
                                    </span>
                                </div>
                                <p class="plan-item__designation">{{ p.designation }}</p>
                            </div>
                        </article>
                    </div>
                </div>
            </section>

        </template>
    </div>
</template>

<style scoped>
/* ============================================================
   CONTENEUR
   ============================================================ */
.sig-page {
    min-height: calc(100vh - 68px);
    padding: 34px 38px 24px;
    display: flex;
    flex-direction: column;
    gap: 1.15rem;
    color: #15233c;
    font-family: "DM Sans", "Segoe UI", sans-serif;
    background:
        radial-gradient(circle at 12% 8%, rgba(26, 42, 108, .055) 0, transparent 38%),
        radial-gradient(circle at 92% 94%, rgba(212, 175, 55, .065) 0, transparent 36%),
        #f5f7fa;
}

.sig-page .welcome-row {
    margin-bottom: 0.25rem;
}

.subheading__sep {
    color: #c3cad5;
    margin: 0 0.4rem;
}

.text-code--inline {
    display: inline-block;
    vertical-align: middle;
    margin-right: 0.15rem;
}

/* ============================================================
   HERO : identification principale
   ============================================================ */
.hero {
    position: relative;
    padding: 1.5rem 1.75rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-left: 4px solid #4a6cf7;
    border-radius: 16px;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .04),
        0 8px 24px rgba(26, 42, 108, .06);
    overflow: hidden;
}

.hero::after {
    content: "";
    position: absolute;
    top: -60px;
    right: -60px;
    width: 200px;
    height: 200px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(74, 108, 247, .08), transparent 70%);
    pointer-events: none;
}

.hero__badges {
    position: absolute;
    top: 1.15rem;
    right: 1.35rem;
    z-index: 1;
}

.hero__body {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
    position: relative;
    z-index: 1;
    max-width: calc(100% - 140px);
}

.hero__label {
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: #8a95a8;
}

.hero__designation {
    margin: 0.15rem 0 0.5rem;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 1.4rem;
    font-weight: 800;
    line-height: 1.25;
    letter-spacing: -0.015em;
    color: #1a2a6c;
}

.hero__code-row {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 0.75rem;
}

.hero__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 0.35rem 1.25rem;
    font-size: 0.82rem;
    color: #6b7280;
}

.hero__meta-item {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
}

.hero__meta-item i {
    color: #8a95a8;
    font-size: 0.85rem;
}

/* ============================================================
   CONTENT CARDS
   ============================================================ */
.content-card {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .04),
        0 8px 24px rgba(26, 42, 108, .06);
    overflow: hidden;
}

.content-card__head {
    display: flex;
    align-items: center;
    gap: 0.65rem;
    padding: 0.85rem 1.25rem;
    background: linear-gradient(140deg, #f7f9fd, #fff);
    border-bottom: 1px solid #e6ebf3;
}

.content-card__icon {
    display: grid;
    place-items: center;
    width: 30px;
    height: 30px;
    border-radius: 9px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.9rem;
    flex: none;
}

.content-card__title {
    margin: 0;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 0.92rem;
    font-weight: 700;
    color: #1a2a6c;
    letter-spacing: -0.005em;
    flex: 1;
}

.content-card__count {
    padding: 0.15rem 0.55rem;
    border-radius: 20px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.7rem;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
}

.content-card__body {
    padding: 1rem 1.25rem 1.15rem;
}

/* --- Text block (description) --- */
.text-block {
    padding: 0.9rem 1.1rem;
    background: #f8fafc;
    border-left: 3px solid #4a6cf7;
    border-radius: 6px;
}

.text-block__content {
    margin: 0;
    font-size: 0.9rem;
    line-height: 1.65;
    color: #15233c;
    white-space: pre-wrap;
    word-break: break-word;
}

/* --- Empty --- */
.content-card__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    padding: 2.5rem 1.5rem;
    text-align: center;
    color: #8a95a8;
}
.content-card__empty i {
    font-size: 2rem;
    opacity: 0.5;
}
.content-card__empty p {
    margin: 0;
    font-size: 0.88rem;
    color: #6b7280;
}

/* ============================================================
   LISTE DES PLANS D'ACTION
   ============================================================ */
.plans-list {
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
}

.plan-item {
    display: flex;
    gap: 0.85rem;
    align-items: flex-start;
    padding: 0.85rem 1rem;
    background: #f8fafc;
    border: 1px solid #eef1f5;
    border-radius: 12px;
    transition: border-color .15s, background .15s, transform .15s, box-shadow .15s;
}

.plan-item:hover {
    border-color: #c6d2ff;
    background: #fff;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(26, 42, 108, .06);
}

.plan-item--principale {
    border-left: 3px solid #d4af37;
    background: linear-gradient(90deg, #fffdf5 0%, #f8fafc 100%);
}

.plan-item__marker {
    display: grid;
    place-items: center;
    width: 36px;
    height: 36px;
    border-radius: 10px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 1rem;
    flex: none;
}

.plan-item--principale .plan-item__marker {
    background: #fff6dd;
    color: #b8860b;
}

.plan-item__body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
}

.plan-item__head {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex-wrap: wrap;
}

.plan-item__code {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.76rem;
    font-weight: 700;
    color: #3b63d9;
    padding: 0.12rem 0.45rem;
    background: #eef1f5;
    border-radius: 4px;
}

.plan-item__badge {
    display: inline-flex;
    align-items: center;
    gap: 0.25rem;
    padding: 0.15rem 0.55rem;
    border-radius: 20px;
    background: #fff6dd;
    color: #b8860b;
    border: 1px solid #f4e2c2;
    font-size: 0.68rem;
    font-weight: 700;
    letter-spacing: 0.03em;
    text-transform: uppercase;
}
.plan-item__badge i {
    font-size: 0.6rem;
}

.plan-item__designation {
    margin: 0;
    font-size: 0.88rem;
    font-weight: 500;
    line-height: 1.4;
    color: #15233c;
    overflow-wrap: anywhere;
}

/* ============================================================
   BADGES
   ============================================================ */
.badge-type {
    display: inline-block;
    font-size: 0.72rem;
    font-weight: 700;
    padding: 0.25rem 0.7rem;
    border-radius: 20px;
    letter-spacing: 0.04em;
}

.badge-type--incident {
    background: #fdf3e2;
    color: #9a6409;
    border: 1px solid #f4e2c2;
}

.badge-type--risque {
    background: #fdecea;
    color: #a61b16;
    border: 1px solid #f6cfcc;
}

.badge-type--autre {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
}

.text-code {
    display: inline-block;
    font-size: 0.78rem;
    padding: 0.2rem 0.55rem;
    background: #eef1f5;
    color: #384a5e;
    border-radius: 4px;
    font-family: ui-monospace, Menlo, monospace;
    font-weight: 600;
}

/* ============================================================
   SKELETONS
   ============================================================ */
.loading-state {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.skeleton {
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: shimmer 1.4s infinite;
    border-radius: 6px;
}

.skeleton--badge { width: 25%; height: 22px; border-radius: 20px; }
.skeleton--title { width: 60%; height: 28px; border-radius: 8px; }
.skeleton--meta { width: 45%; height: 14px; }
.skeleton--line { height: 12px; }
.skeleton--short { width: 30%; }
.skeleton--medium { width: 60%; }

@keyframes shimmer {
    from { background-position: 200% 0; }
    to { background-position: -200% 0; }
}

.hero-skeleton {
    padding: 1.5rem 1.75rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-left: 4px solid #4a6cf7;
    border-radius: 16px;
    display: flex;
    flex-direction: column;
    gap: 0.85rem;
}

.content-skeleton {
    padding: 1rem 1.25rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 1199.98px) {
    .sig-page { padding-left: 24px; padding-right: 24px; }
}

@media (max-width: 767.98px) {
    .sig-page { padding: 24px 16px 20px; }
    .hero__body {
        max-width: 100%;
    }
    .hero__badges {
        position: static;
        justify-content: flex-start;
        margin-bottom: 0.75rem;
    }
    .hero__designation {
        font-size: 1.15rem;
    }
    .plan-item {
        flex-direction: column;
        align-items: flex-start;
        gap: 0.5rem;
    }
}

@media (max-width: 575.98px) {
    .sig-page { padding: 20px 12px 16px; }
    .hero { padding: 1.15rem 1.15rem; }
    .hero__meta { gap: 0.3rem 0.85rem; font-size: 0.78rem; }
    .text-block { padding: 0.75rem 0.9rem; }
    .text-block__content { font-size: 0.85rem; }
}

@media (prefers-reduced-motion: reduce) {
    .skeleton { animation: none !important; }
    .plan-item { transition: none !important; }
}
</style>
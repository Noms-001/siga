<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseButton } from '@/components/base'
import { getIndicateurFront, listerValeursIndicateur } from '@/services/indicateurFront'
import type { Indicateur, ValeurIndicateur } from '@/types/backoffice/indicateur'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)

const item = ref<Indicateur | null>(null)
const loading = ref(false)
const errorMessage = ref('')

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await getIndicateurFront(id)
    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }
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

/** Affiche un tiret cadratin pour toute valeur absente ou vide. */
function v(value: unknown): string {
    if (value === null || value === undefined) return '—'
    const s = String(value).trim()
    return s === '' ? '—' : s
}

/** Classe de badge selon le type — cohérent avec la liste. */
const classeType = computed(() => {
    const t = (item.value?.typeIndicateur ?? '').toUpperCase()
    if (t === 'KPI') return 'badge-type--kpi'
    if (t === 'KRI') return 'badge-type--kri'
    return 'badge-type--autre'
})

const aDesBornes = computed(() => {
    const i = item.value
    if (!i) return false
    return i.valeurCible != null || i.seuilMin != null || i.seuilMax != null
})

const aDesTextes = computed(() => {
    const i = item.value
    if (!i) return false
    return !!(i.definition || i.methodeDetermination || i.objectif)
})

const valeurs = ref<ValeurIndicateur[]>([])
const valeursLoading = ref(false)
const valeursError = ref('')

const valeurCourante = computed<ValeurIndicateur | null>(() => {
    return valeurs.value[0] ?? null
})

const historique = computed(() => valeurs.value.slice(1))

async function chargerValeurs(): Promise<void> {
    valeursLoading.value = true
    valeursError.value = ''
    const res = await listerValeursIndicateur(id)
    valeursLoading.value = false
    if (!res.success) {
        valeursError.value = res.error
        return
    }
    valeurs.value = res.data
}

onMounted(() => {
    charger()
    chargerValeurs()
})

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
</script>

<template>
    <div class="ind-page">
        <!-- ============ EN-TÊTE ============ -->
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>{{ item?.designation || "Détail de l'indicateur" }}</h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    <template v-if="item">
                        <code class="text-code text-code--inline">{{ item.code }}</code>
                        <span v-if="item.typeIndicateur" class="subheading__sep">—</span>
                        <span v-if="item.typeIndicateur">{{ item.typeIndicateur }}</span>
                    </template>
                    <template v-else>Consultation des informations de l'indicateur</template>
                </p>
            </div>

            <div class="welcome-row__right">
                <BaseButton variant="secondary" icon="bi bi-arrow-left" @click="router.push({ name: 'indicateurs' })">
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
                <div class="skeleton skeleton--label"></div>
                <div class="skeleton skeleton--value"></div>
                <div class="skeleton skeleton--meta"></div>
            </div>
            <div class="info-grid">
                <div v-for="i in 2" :key="i" class="info-skeleton">
                    <div class="skeleton skeleton--line skeleton--short"></div>
                    <div class="skeleton skeleton--line"></div>
                    <div class="skeleton skeleton--line skeleton--medium"></div>
                </div>
            </div>
        </div>

        <!-- ============ CONTENU ============ -->
        <template v-else-if="item">

            <!-- ============ HERO : VALEUR ACTUELLE ============ -->
            <section
                v-if="valeurCourante && !valeursLoading"
                class="hero"
                aria-labelledby="hero-label"
            >
                <div class="hero__badges">
                    <span v-if="item.typeIndicateur" class="badge-type" :class="classeType">
                        {{ item.typeIndicateur }}
                    </span>
                    <span
                        class="badge-statut"
                        :class="item.actif ? 'badge-statut--actif' : 'badge-statut--inactif'"
                    >
                        {{ item.actif ? 'Actif' : 'Inactif' }}
                    </span>
                </div>

                <div class="hero__body">
                    <span id="hero-label" class="hero__label">Valeur actuelle</span>
                    <div class="hero__value">
                        {{ valeurCourante.valeur }}
                        <span v-if="item.uniteMesure" class="hero__unit">
                            {{ item.uniteMesure }}
                        </span>
                    </div>

                    <div class="hero__meta">
                        <span class="hero__meta-item">
                            <i class="bi bi-calendar3" aria-hidden="true"></i>
                            {{ formaterDate(valeurCourante.periodeDebut) }} → {{ formaterDate(valeurCourante.periodeFin) }}
                        </span>
                        <span class="hero__meta-item">
                            <i class="bi bi-person" aria-hidden="true"></i>
                            {{ valeurCourante.prenomUtilisateur }} {{ valeurCourante.nomUtilisateur }}
                        </span>
                        <span class="hero__meta-item">
                            <i class="bi bi-clock" aria-hidden="true"></i>
                            {{ formaterDateHeure(valeurCourante.dateSaisie) }}
                        </span>
                    </div>

                    <blockquote v-if="valeurCourante.commentaire" class="hero__comment">
                        <i class="bi bi-chat-quote" aria-hidden="true"></i>
                        <span>{{ valeurCourante.commentaire }}</span>
                    </blockquote>
                </div>
            </section>

            <section v-else-if="valeursLoading" class="hero hero--loading" aria-hidden="true">
                <div class="hero-skeleton hero-skeleton--inline">
                    <div class="skeleton skeleton--label"></div>
                    <div class="skeleton skeleton--value"></div>
                    <div class="skeleton skeleton--meta"></div>
                </div>
            </section>

            <section v-else class="hero hero--empty">
                <span class="hero__empty-icon" aria-hidden="true">
                    <i class="bi bi-dash-circle"></i>
                </span>
                <div class="hero__empty-body">
                    <span class="hero__label">Valeur actuelle</span>
                    <p class="hero__empty-text">
                        Aucune valeur n'a encore été saisie pour cet indicateur.
                    </p>
                </div>
            </section>

            <!-- ============ INFO GRID ============ -->
            <div class="info-grid">
                <!-- Général -->
                <article class="info-card">
                    <header class="info-card__head">
                        <span class="info-card__icon" aria-hidden="true">
                            <i class="bi bi-info-circle"></i>
                        </span>
                        <h2 class="info-card__title">Informations générales</h2>
                    </header>
                    <div class="info-card__body">
                        <div class="info-row">
                            <span class="info-row__label">Code</span>
                            <span class="info-row__value">
                                <code class="text-code">{{ item.code }}</code>
                            </span>
                        </div>
                        <div class="info-row">
                            <span class="info-row__label">Désignation</span>
                            <span class="info-row__value">{{ item.designation }}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-row__label">Type</span>
                            <span class="info-row__value">
                                <span v-if="item.typeIndicateur" class="badge-type" :class="classeType">
                                    {{ item.typeIndicateur }}
                                </span>
                                <span v-else class="text-muted-custom">—</span>
                            </span>
                        </div>
                        <div class="info-row">
                            <span class="info-row__label">Statut</span>
                            <span class="info-row__value">
                                <span
                                    class="badge-statut"
                                    :class="item.actif ? 'badge-statut--actif' : 'badge-statut--inactif'"
                                >
                                    {{ item.actif ? 'Actif' : 'Inactif' }}
                                </span>
                            </span>
                        </div>
                    </div>
                </article>

                <!-- Mesure -->
                <article v-if="aDesBornes" class="info-card">
                    <header class="info-card__head">
                        <span class="info-card__icon" aria-hidden="true">
                            <i class="bi bi-speedometer2"></i>
                        </span>
                        <h2 class="info-card__title">Mesure</h2>
                        <span v-if="item.uniteMesure" class="info-card__pill">
                            {{ item.uniteMesure }}
                        </span>
                    </header>
                    <div class="info-card__body info-card__body--tiles">
                        <div class="measure-tile">
                            <span class="measure-tile__label">Valeur cible</span>
                            <span class="measure-tile__value">{{ v(item.valeurCible) }}</span>
                        </div>
                        <div class="measure-tile measure-tile--min">
                            <span class="measure-tile__label">Seuil minimum</span>
                            <span class="measure-tile__value">{{ v(item.seuilMin) }}</span>
                        </div>
                        <div class="measure-tile measure-tile--max">
                            <span class="measure-tile__label">Seuil maximum</span>
                            <span class="measure-tile__value">{{ v(item.seuilMax) }}</span>
                        </div>
                    </div>
                </article>

                <!-- Référentiel HOPEx -->
                <article v-if="item.codeHopex || item.indicateurHopex" class="info-card">
                    <header class="info-card__head">
                        <span class="info-card__icon" aria-hidden="true">
                            <i class="bi bi-diagram-2"></i>
                        </span>
                        <h2 class="info-card__title">Référentiel HOPEx</h2>
                    </header>
                    <div class="info-card__body">
                        <div class="info-row">
                            <span class="info-row__label">Code HOPEx</span>
                            <span class="info-row__value info-row__value--mono">{{ v(item.codeHopex) }}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-row__label">Indicateur HOPEx</span>
                            <span class="info-row__value">{{ v(item.indicateurHopex) }}</span>
                        </div>
                    </div>
                </article>

                <!-- Fréquences -->
                <article
                    v-if="item.frequenceVerification || item.frequenceAggregation"
                    class="info-card grid-column-full"
                >
                    <header class="info-card__head">
                        <span class="info-card__icon" aria-hidden="true">
                            <i class="bi bi-arrow-repeat"></i>
                        </span>
                        <h2 class="info-card__title">Fréquences</h2>
                    </header>
                    <div class="info-card__body">
                        <div class="info-row">
                            <span class="info-row__label">Vérification</span>
                            <span class="info-row__value">{{ v(item.frequenceVerification) }}</span>
                        </div>
                        <div class="info-row">
                            <span class="info-row__label">Agrégation</span>
                            <span class="info-row__value">{{ v(item.frequenceAggregation) }}</span>
                        </div>
                    </div>
                </article>
            </div>

            <!-- ============ DÉFINITION ============ -->
            <section v-if="aDesTextes" class="content-card" aria-labelledby="definition-title">
                <header class="content-card__head">
                    <span class="content-card__icon" aria-hidden="true">
                        <i class="bi bi-journal-text"></i>
                    </span>
                    <h2 id="definition-title" class="content-card__title">Définition</h2>
                </header>
                <div class="content-card__body">
                    <div v-if="item.definition" class="text-block">
                        <span class="text-block__label">Définition</span>
                        <p class="text-block__content">{{ item.definition }}</p>
                    </div>
                    <div v-if="item.methodeDetermination" class="text-block">
                        <span class="text-block__label">Méthode de détermination</span>
                        <p class="text-block__content">{{ item.methodeDetermination }}</p>
                    </div>
                    <div v-if="item.objectif" class="text-block">
                        <span class="text-block__label">Objectif</span>
                        <p class="text-block__content">{{ item.objectif }}</p>
                    </div>
                </div>
            </section>

            <!-- ============ HISTORIQUE ============ -->
            <section v-if="valeurCourante" class="content-card" aria-labelledby="history-title">
                <header class="content-card__head">
                    <span class="content-card__icon" aria-hidden="true">
                        <i class="bi bi-clock-history"></i>
                    </span>
                    <h2 id="history-title" class="content-card__title">Historique des valeurs</h2>
                    <span class="content-card__count">{{ valeurs.length }}</span>
                </header>

                <div v-if="valeursError" class="alert alert-danger m-3 d-flex justify-content-between align-items-center" role="alert">
                    <span>{{ valeursError }}</span>
                    <BaseButton size="sm" variant="secondary" @click="chargerValeurs">Réessayer</BaseButton>
                </div>

                <div v-else-if="historique.length === 0" class="content-card__empty">
                    <i class="bi bi-clock-history" aria-hidden="true"></i>
                    <p>Aucun historique antérieur — la valeur courante est le premier relevé.</p>
                </div>

                <div v-else class="history-table-wrap">
                    <table class="history-table">
                        <thead>
                            <tr>
                                <th>Valeur</th>
                                <th>Période</th>
                                <th>Commentaire</th>
                                <th>Date de saisie</th>
                                <th>Par</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="val in historique" :key="val.id">
                                <td class="history-table__value">
                                    {{ val.valeur }}
                                    <span v-if="item.uniteMesure" class="history-table__unit">
                                        {{ item.uniteMesure }}
                                    </span>
                                </td>
                                <td class="history-table__period">
                                    {{ formaterDate(val.periodeDebut) }} → {{ formaterDate(val.periodeFin) }}
                                </td>
                                <td class="history-table__comment">
                                    <span v-if="val.commentaire">{{ val.commentaire }}</span>
                                    <span v-else class="text-muted-custom">—</span>
                                </td>
                                <td class="history-table__date">{{ formaterDateHeure(val.dateSaisie) }}</td>
                                <td class="history-table__user">
                                    {{ val.prenomUtilisateur }} {{ val.nomUtilisateur }}
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
/* ============================================================
   CONTENEUR
   ============================================================ */
.ind-page {
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

.ind-page .welcome-row {
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
   HERO : VALEUR ACTUELLE
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
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
    justify-content: flex-end;
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
}

.hero__label {
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: #8a95a8;
}

.hero__value {
    display: flex;
    align-items: baseline;
    gap: 0.5rem;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 3rem;
    font-weight: 800;
    line-height: 1;
    color: #1a2a6c;
    letter-spacing: -0.02em;
    font-variant-numeric: tabular-nums;
    margin: 0.15rem 0 0.6rem;
    max-width: calc(100% - 180px);
}

.hero__unit {
    font-size: 1.1rem;
    font-weight: 600;
    color: #8a95a8;
    letter-spacing: 0;
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
    gap: 0.35rem;
}

.hero__meta-item i {
    color: #8a95a8;
    font-size: 0.85rem;
}

.hero__comment {
    margin: 0.85rem 0 0;
    padding: 0.7rem 0.9rem;
    background: #f8fafc;
    border-left: 3px solid #d4af37;
    border-radius: 6px;
    font-style: italic;
    font-size: 0.88rem;
    color: #384a5e;
    display: flex;
    gap: 0.6rem;
    align-items: flex-start;
    max-width: 720px;
}
.hero__comment i {
    color: #d4af37;
    font-size: 0.95rem;
    margin-top: 0.1rem;
    flex: none;
}

/* --- Hero : états --- */
.hero--loading,
.hero--empty {
    display: flex;
    align-items: center;
    gap: 1.1rem;
    padding: 1.5rem 1.75rem;
}

.hero__empty-icon {
    display: grid;
    place-items: center;
    width: 52px;
    height: 52px;
    border-radius: 14px;
    background: #f1f5f9;
    color: #8a95a8;
    font-size: 1.5rem;
    flex: none;
}

.hero__empty-body {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
}

.hero__empty-text {
    margin: 0;
    font-size: 0.88rem;
    color: #6b7280;
    font-style: italic;
}

/* ============================================================
   INFO GRID (cartes courtes)
   ============================================================ */
.info-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1rem;
}

.info-card {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .04),
        0 8px 24px rgba(26, 42, 108, .06);
    overflow: hidden;
    display: flex;
    flex-direction: column;
}

.grid-column-full {
    grid-column: 1 / -1;
}

.info-card__head {
    display: flex;
    align-items: center;
    gap: 0.65rem;
    padding: 0.85rem 1.25rem;
    background: linear-gradient(140deg, #f7f9fd, #fff);
    border-bottom: 1px solid #e6ebf3;
}

.info-card__icon {
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

.info-card__title {
    margin: 0;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 0.88rem;
    font-weight: 700;
    color: #1a2a6c;
    letter-spacing: -0.005em;
}

.info-card__pill {
    margin-left: auto;
    padding: 0.15rem 0.6rem;
    border-radius: 20px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.7rem;
    font-weight: 700;
    letter-spacing: 0.02em;
}

.info-card__body {
    padding: 0.4rem 1.25rem 0.75rem;
    display: flex;
    flex-direction: column;
    flex: 1;
}

.info-card__body--tiles {
    padding: 1rem 1.25rem 1.1rem;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 0.6rem;
    align-content: start;
}

/* --- Lignes d'info --- */
.info-row {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 1rem;
    padding: 0.55rem 0;
    border-bottom: 1px dashed #eef1f5;
}

.info-row:last-child {
    border-bottom: none;
    padding-bottom: 0;
}

.info-row__label {
    font-size: 0.72rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: #8a95a8;
    flex: none;
}

.info-row__value {
    color: #15233c;
    font-size: 0.86rem;
    text-align: right;
    word-break: break-word;
    min-width: 0;
}

.info-row__value--mono {
    font-family: ui-monospace, Menlo, monospace;
    font-weight: 600;
    color: #1a2a6c;
}

/* --- Tuiles mesure --- */
.measure-tile {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    padding: 0.75rem 0.85rem;
    background: #f8fafc;
    border: 1px solid #eef1f5;
    border-radius: 10px;
}

.measure-tile--min {
    border-left: 3px solid #f4b91f;
}

.measure-tile--max {
    border-left: 3px solid #d64545;
}

.measure-tile__label {
    font-size: 0.66rem;
    font-weight: 700;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    color: #8a95a8;
}

.measure-tile__value {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 1.15rem;
    font-weight: 700;
    color: #1a2a6c;
    font-variant-numeric: tabular-nums;
    line-height: 1.2;
}

/* ============================================================
   CONTENT CARDS (Définition, Historique)
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
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

/* --- Textes (Définition) --- */
.text-block {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
    padding: 0.75rem 1rem;
    background: #f8fafc;
    border-left: 3px solid #4a6cf7;
    border-radius: 6px;
}

.text-block__label {
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    color: #6b7280;
}

.text-block__content {
    margin: 0;
    font-size: 0.88rem;
    line-height: 1.6;
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
   HISTORIQUE — table
   ============================================================ */
.history-table-wrap {
    overflow-x: auto;
}

.history-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.85rem;
}

.history-table thead th {
    padding: 0.7rem 1.25rem;
    text-align: left;
    font-size: 0.7rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: #8a95a8;
    background: #f8fafc;
    border-bottom: 1px solid #e6ebf3;
    white-space: nowrap;
}

.history-table tbody td {
    padding: 0.75rem 1.25rem;
    border-bottom: 1px solid #eef1f5;
    color: #15233c;
    vertical-align: middle;
}

.history-table tbody tr:last-child td {
    border-bottom: none;
}

.history-table tbody tr:hover {
    background: #f8fafc;
}

.history-table__value {
    font-family: ui-monospace, Menlo, monospace;
    font-weight: 700;
    color: #1a2a6c;
    white-space: nowrap;
    font-variant-numeric: tabular-nums;
}

.history-table__unit {
    font-weight: 400;
    color: #8a95a8;
    margin-left: 0.25rem;
    font-family: inherit;
    font-size: 0.8rem;
}

.history-table__period,
.history-table__date {
    white-space: nowrap;
    font-size: 0.8rem;
    color: #6b7280;
}

.history-table__comment {
    max-width: 320px;
    font-style: italic;
    color: #6b7280;
    overflow-wrap: anywhere;
}

.history-table__user {
    white-space: nowrap;
    font-size: 0.82rem;
}

/* ============================================================
   BADGES
   ============================================================ */
.badge-type {
    display: inline-block;
    font-size: 0.72rem;
    font-weight: 700;
    padding: 0.2rem 0.65rem;
    border-radius: 20px;
    letter-spacing: 0.04em;
}
.badge-type--kpi { background: #e4f0fb; color: #135d95; border: 1px solid #cde2f6; }
.badge-type--kri { background: #fdf3e2; color: #9a6409; border: 1px solid #f4e2c2; }
.badge-type--autre { background: #eef1f5; color: #5b6b7d; border: 1px solid #dfe5ec; }

.badge-statut {
    display: inline-block;
    font-size: 0.7rem;
    font-weight: 700;
    padding: 0.25rem 0.7rem;
    border-radius: 20px;
}
.badge-statut--actif { background: #e6f4ea; color: #1e7e34; border: 1px solid #c8e5cf; }
.badge-statut--inactif { background: #eef1f5; color: #5b6b7d; border: 1px solid #dfe5ec; }

.text-code {
    display: inline-block;
    font-size: 0.78rem;
    padding: 0.15rem 0.5rem;
    background: #eef1f5;
    color: #384a5e;
    border-radius: 4px;
    font-family: ui-monospace, Menlo, monospace;
    font-weight: 600;
}

.text-muted-custom {
    color: #8a95a8;
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

.skeleton--label { width: 25%; height: 12px; }
.skeleton--value { width: 40%; height: 36px; border-radius: 8px; }
.skeleton--meta { width: 65%; height: 14px; }
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
    gap: 0.65rem;
}

.hero-skeleton--inline {
    border: none;
    border-left: none;
    padding: 0;
}

.info-skeleton {
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
    .ind-page { padding-left: 24px; padding-right: 24px; }
}

@media (max-width: 991.98px) {
    .hero__value { font-size: 2.4rem; max-width: 100%; }
}

@media (max-width: 767.98px) {
    .ind-page { padding: 24px 16px 20px; }
    .info-grid { grid-template-columns: 1fr; }
    .info-card__body--tiles { grid-template-columns: repeat(3, minmax(0, 1fr)); }
    .hero__badges {
        position: static;
        justify-content: flex-start;
        margin-bottom: 0.75rem;
    }
    .hero__value { max-width: 100%; }
}

@media (max-width: 575.98px) {
    .ind-page { padding: 20px 12px 16px; }
    .hero { padding: 1.15rem 1.15rem; }
    .hero__value { font-size: 2rem; }
    .hero__meta { gap: 0.3rem 0.85rem; font-size: 0.78rem; }
    .info-card__body--tiles { grid-template-columns: 1fr; }
    .info-row {
        flex-direction: column;
        gap: 0.15rem;
        align-items: flex-start;
    }
    .info-row__value { text-align: left; }
    .history-table thead th,
    .history-table tbody td { padding-left: 0.85rem; padding-right: 0.85rem; }
    .history-table thead th:nth-child(3),
    .history-table tbody td:nth-child(3) { display: none; }
}

@media (prefers-reduced-motion: reduce) {
    .skeleton { animation: none !important; }
}
</style>
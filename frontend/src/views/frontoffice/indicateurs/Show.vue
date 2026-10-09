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

/**
 * Vrai si au moins une des trois bornes est renseignée.
 * Sert à masquer la section "Mesure" quand l'indicateur n'est encore
 * qu'une ébauche de paramétrage — un bloc vide n'apporte rien.
 */
const aDesBornes = computed(() => {
    const i = item.value
    if (!i) return false
    return i.valeurCible != null || i.seuilMin != null || i.seuilMax != null
})

/** Vrai si l'indicateur porte au moins un texte de définition. */
const aDesTextes = computed(() => {
    const i = item.value
    if (!i) return false
    return !!(i.definition || i.methodeDetermination || i.objectif)
})

const valeurs = ref<ValeurIndicateur[]>([])
const valeursLoading = ref(false)
const valeursError = ref('')

/** Valeur courante = le premier élément (tri desc par période de fin). */
const valeurCourante = computed<ValeurIndicateur | null>(() => {
    return valeurs.value[0] ?? null
})

/** Historique sans la valeur courante, pour le tableau. */
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
        <header class="ind-header">
            <div>
                <h1 class="ind-header__title">Détail de l'indicateur</h1>
                <p class="ind-header__subtitle">
                    Consultation des informations de l'indicateur
                </p>
            </div>
            <div class="ind-header__actions">
                <BaseButton variant="secondary" icon="bi bi-arrow-left" @click="router.push({ name: 'indicateurs' })">
                    Retour à la liste
                </BaseButton>
            </div>
        </header>

        <!-- Erreur -->
        <div v-if="errorMessage" class="alert alert-danger mb-0 d-flex justify-content-between align-items-center"
            role="alert">
            <span>{{ errorMessage }}</span>
            <BaseButton size="sm" variant="secondary" @click="charger">Réessayer</BaseButton>
        </div>

        <!-- Chargement -->
        <div v-else-if="loading" class="ind-loading">
            <div v-for="i in 3" :key="i" class="ind-skeleton"></div>
        </div>

        <template v-else-if="item">

            <!-- Informations générales -->
            <section class="ind-section">
                <header class="ind-section__head">Informations générales</header>

                <div class="ind-section__body">
                    <div class="ind-row">
                        <span class="ind-row__label">Code</span>
                        <span class="ind-row__value">
                            <code class="text-code">{{ item.code }}</code>
                        </span>
                    </div>
                    <div class="ind-row">
                        <span class="ind-row__label">Désignation</span>
                        <span class="ind-row__value">{{ item.designation }}</span>
                    </div>
                    <div class="ind-row">
                        <span class="ind-row__label">Type</span>
                        <span class="ind-row__value">
                            <span v-if="item.typeIndicateur" class="badge-type" :class="classeType">
                                {{ item.typeIndicateur }}
                            </span>
                            <span v-else class="text-muted-custom">—</span>
                        </span>
                    </div>
                    <div class="ind-row">
                        <span class="ind-row__label">Statut</span>
                        <span class="ind-row__value">
                            <span class="badge-statut"
                                :class="item.actif ? 'badge-statut--actif' : 'badge-statut--inactif'">
                                {{ item.actif ? 'Actif' : 'Inactif' }}
                            </span>
                        </span>
                    </div>
                </div>
            </section>

            <section v-if="item" class="ind-section ind-section--current">
                <header class="ind-section__head">Valeur actuelle</header>
                <div class="ind-current">
                    <div v-if="valeursLoading" class="ind-current__loading">
                        <div class="ind-skeleton" style="height: 42px"></div>
                    </div>

                    <div v-else-if="valeurCourante" class="ind-current__value-wrap">
                        <div class="ind-current__value">
                            {{ valeurCourante.valeur }}
                            <span v-if="item.uniteMesure" class="ind-current__unite">
                                {{ item.uniteMesure }}
                            </span>
                        </div>
                        <div class="ind-current__meta">
                            <span>
                                Période : {{ formaterDate(valeurCourante.periodeDebut) }}
                                → {{ formaterDate(valeurCourante.periodeFin) }}
                            </span>
                            <span>
                                Saisie le {{ formaterDateHeure(valeurCourante.dateSaisie) }}
                                par {{ valeurCourante.prenomUtilisateur }} {{ valeurCourante.nomUtilisateur }}
                            </span>
                            <span v-if="valeurCourante.commentaire" class="ind-current__comment">
                                « {{ valeurCourante.commentaire }} »
                            </span>
                        </div>
                    </div>

                    <div v-else class="ind-current__empty">
                        Aucune valeur saisie pour cet indicateur.
                    </div>
                </div>
            </section>

            <!-- Référentiel HOPEx — affiché uniquement si au moins un des
                 deux champs est renseigné. -->
            <section v-if="item.codeHopex || item.indicateurHopex" class="ind-section">
                <header class="ind-section__head">Référentiel HOPEx</header>
                <div class="ind-section__body">
                    <div class="ind-row">
                        <span class="ind-row__label">Code HOPEx</span>
                        <span class="ind-row__value">{{ v(item.codeHopex) }}</span>
                    </div>
                    <div class="ind-row">
                        <span class="ind-row__label">Indicateur HOPEx</span>
                        <span class="ind-row__value">{{ v(item.indicateurHopex) }}</span>
                    </div>
                </div>
            </section>

            <!-- Mesure — affichée uniquement si au moins une borne est
                 renseignée. Un bloc à trois tirets n'apprend rien. -->
            <section v-if="aDesBornes" class="ind-section">
                <header class="ind-section__head">Mesure</header>
                <div class="ind-section__body">
                    <div class="ind-row">
                        <span class="ind-row__label">Unité de mesure</span>
                        <span class="ind-row__value">{{ v(item.uniteMesure) }}</span>
                    </div>
                    <div class="ind-row">
                        <span class="ind-row__label">Valeur cible</span>
                        <span class="ind-row__value ind-row__value--mono">
                            {{ v(item.valeurCible) }}
                        </span>
                    </div>
                    <div class="ind-row">
                        <span class="ind-row__label">Seuil minimum</span>
                        <span class="ind-row__value ind-row__value--mono">
                            {{ v(item.seuilMin) }}
                        </span>
                    </div>
                    <div class="ind-row">
                        <span class="ind-row__label">Seuil maximum</span>
                        <span class="ind-row__value ind-row__value--mono">
                            {{ v(item.seuilMax) }}
                        </span>
                    </div>
                </div>
            </section>

            <!-- Fréquences -->
            <section v-if="item.frequenceVerification || item.frequenceAggregation" class="ind-section">
                <header class="ind-section__head">Fréquences</header>
                <div class="ind-section__body">
                    <div class="ind-row">
                        <span class="ind-row__label">Fréquence de vérification</span>
                        <span class="ind-row__value">{{ v(item.frequenceVerification) }}</span>
                    </div>
                    <div class="ind-row">
                        <span class="ind-row__label">Fréquence d'agrégation</span>
                        <span class="ind-row__value">{{ v(item.frequenceAggregation) }}</span>
                    </div>
                </div>
            </section>

            <!-- Définition -->
            <section v-if="aDesTextes" class="ind-section">
                <header class="ind-section__head">Définition</header>
                <div class="ind-section__body">
                    <div v-if="item.definition" class="ind-row ind-row--block">
                        <span class="ind-row__label">Définition</span>
                        <span class="ind-row__value ind-row__value--pre">{{ item.definition }}</span>
                    </div>
                    <div v-if="item.methodeDetermination" class="ind-row ind-row--block">
                        <span class="ind-row__label">Méthode de détermination</span>
                        <span class="ind-row__value ind-row__value--pre">{{ item.methodeDetermination }}</span>
                    </div>
                    <div v-if="item.objectif" class="ind-row ind-row--block">
                        <span class="ind-row__label">Objectif</span>
                        <span class="ind-row__value ind-row__value--pre">{{ item.objectif }}</span>
                    </div>
                </div>
            </section>

            <section v-if="valeurCourante" class="ind-section">
                <header class="ind-section__head">
                    Historique des valeurs
                    <span class="ind-section__count">{{ valeurs.length }}</span>
                </header>

                <div v-if="valeursError"
                    class="alert alert-danger mb-0 d-flex justify-content-between align-items-center" role="alert">
                    <span>{{ valeursError }}</span>
                    <BaseButton size="sm" variant="secondary" @click="chargerValeurs">Réessayer</BaseButton>
                </div>

                <div v-else-if="historique.length === 0" class="ind-empty">
                    Aucun historique antérieur — la valeur courante est le premier relevé.
                </div>

                <div v-else class="ind-history">
                    <table class="ind-history__table">
                        <thead>
                            <tr>
                                <th>Valeur</th>
                                <th>Période</th>
                                <th>Commentaire</th>
                                <th>Saisie</th>
                                <th>Par</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="v in historique" :key="v.id">
                                <td class="ind-history__valeur">
                                    {{ v.valeur }}
                                    <span v-if="item.uniteMesure" class="ind-history__unite">
                                        {{ item.uniteMesure }}
                                    </span>
                                </td>
                                <td class="ind-history__periode">
                                    {{ formaterDate(v.periodeDebut) }} →
                                    {{ formaterDate(v.periodeFin) }}
                                </td>
                                <td class="ind-history__comment">
                                    {{ v.commentaire || '—' }}
                                </td>
                                <td class="ind-history__date">
                                    {{ formaterDateHeure(v.dateSaisie) }}
                                </td>
                                <td class="ind-history__user">
                                    {{ v.prenomUtilisateur }} {{ v.nomUtilisateur }}
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
.ind-page {
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
}

/* --- en-tête --- */

.ind-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 1rem;
}

.ind-header__title {
    font-size: 1.4rem;
    font-weight: 700;
    color: var(--dts-navy);
    margin: 0;
}

.ind-header__subtitle {
    color: var(--dts-muted);
    margin: 0.15rem 0 0;
    font-size: 0.85rem;
}

.ind-header__actions {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
}

/* --- sections --- */

.ind-section {
    max-width: 860px;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    overflow: hidden;
}

.ind-section__head {
    padding: 0.7rem 1.25rem;
    background: var(--dts-bg);
    border-bottom: 1px solid var(--dts-border);
    font-size: 0.72rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: var(--dts-muted);
}

.ind-section__body {
    padding: 0.75rem 1.25rem 1rem;
    display: flex;
    flex-direction: column;
}

/* --- lignes --- */

.ind-row {
    display: grid;
    grid-template-columns: 220px 1fr;
    gap: 1rem;
    padding: 0.6rem 0;
    border-bottom: 1px dashed var(--dts-border);
    align-items: baseline;
}

.ind-row:last-child {
    border-bottom: none;
    padding-bottom: 0;
}

.ind-row--block {
    align-items: start;
}

.ind-row__label {
    font-size: 0.78rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: var(--dts-muted);
}

.ind-row__value {
    color: var(--dts-text);
    font-size: 0.9rem;
    word-break: break-word;
}

.ind-row__value--mono {
    font-family: ui-monospace, Menlo, monospace;
    font-weight: 600;
    color: var(--dts-navy);
}

.ind-row__value--pre {
    white-space: pre-wrap;
    line-height: 1.55;
    font-size: 0.88rem;
}

/* --- badges --- */

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

.badge-type {
    display: inline-block;
    font-size: 0.72rem;
    font-weight: 700;
    padding: 0.15rem 0.6rem;
    border-radius: 20px;
    letter-spacing: 0.04em;
}

.badge-type--kpi {
    background: #e4f0fb;
    color: #135d95;
    border: 1px solid #cde2f6;
}

.badge-type--kri {
    background: #fdf3e2;
    color: #9a6409;
    border: 1px solid #f4e2c2;
}

.badge-type--autre {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
}

.badge-statut {
    display: inline-block;
    font-size: 0.7rem;
    font-weight: 700;
    padding: 0.25rem 0.7rem;
    border-radius: 20px;
}

.badge-statut--actif {
    background: #e6f4ea;
    color: #1e7e34;
    border: 1px solid #c8e5cf;
}

.badge-statut--inactif {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
}

.text-muted-custom {
    color: var(--dts-muted);
}

/* --- états --- */

.ind-loading {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    max-width: 860px;
}

.ind-skeleton {
    height: 120px;
    border-radius: var(--dts-radius);
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: indSk 1.4s infinite;
}

/* --- valeur courante --- */

.ind-section--current {
    border-left: 4px solid var(--dts-blue);
}

.ind-current {
    padding: 1.25rem 1.5rem;
}

.ind-current__value-wrap {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.ind-current__value {
    display: flex;
    align-items: baseline;
    gap: 0.5rem;
    font-size: 2.25rem;
    font-weight: 700;
    color: var(--dts-navy);
    line-height: 1;
    font-family: ui-monospace, Menlo, monospace;
}

.ind-current__unite {
    font-size: 1rem;
    font-weight: 500;
    color: var(--dts-muted);
    font-family: inherit;
}

.ind-current__meta {
    display: flex;
    flex-direction: column;
    gap: 0.2rem;
    font-size: 0.82rem;
    color: var(--dts-muted);
}

.ind-current__comment {
    font-style: italic;
    color: var(--dts-text);
    margin-top: 0.35rem;
}

.ind-current__empty {
    font-size: 0.85rem;
    color: var(--dts-muted);
    font-style: italic;
}

.ind-current__loading .ind-skeleton {
    border-radius: 6px;
}

/* --- historique --- */

.ind-section__count {
    display: inline-block;
    margin-left: 0.5rem;
    padding: 0.1rem 0.5rem;
    border-radius: 20px;
    background: var(--dts-blue-50);
    color: var(--dts-blue);
    font-size: 0.7rem;
    font-weight: 700;
    text-transform: none;
    letter-spacing: 0;
}

.ind-empty {
    padding: 1.25rem;
    text-align: center;
    font-size: 0.85rem;
    color: var(--dts-muted);
}

.ind-history {
    overflow-x: auto;
}

.ind-history__table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.85rem;
}

.ind-history__table thead th {
    padding: 0.6rem 1rem;
    text-align: left;
    font-size: 0.7rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--dts-muted);
    background: var(--dts-bg);
    border-bottom: 1px solid var(--dts-border);
    white-space: nowrap;
}

.ind-history__table tbody td {
    padding: 0.7rem 1rem;
    border-bottom: 1px solid var(--dts-border);
    color: var(--dts-text);
    vertical-align: top;
}

.ind-history__table tbody tr:last-child td {
    border-bottom: none;
}

.ind-history__table tbody tr:hover {
    background: var(--dts-blue-50);
}

.ind-history__valeur {
    font-family: ui-monospace, Menlo, monospace;
    font-weight: 700;
    color: var(--dts-navy);
    white-space: nowrap;
}

.ind-history__unite {
    font-weight: 400;
    color: var(--dts-muted);
    margin-left: 0.2rem;
    font-family: inherit;
}

.ind-history__periode,
.ind-history__date {
    white-space: nowrap;
    font-size: 0.8rem;
}

.ind-history__comment {
    max-width: 320px;
    font-style: italic;
    color: var(--dts-muted);
}

.ind-history__user {
    white-space: nowrap;
    font-size: 0.82rem;
}

@keyframes indSk {
    from {
        background-position: 200% 0;
    }

    to {
        background-position: -200% 0;
    }
}

/* --- responsive --- */

@media (max-width: 768px) {
    .ind-row {
        grid-template-columns: 1fr;
        gap: 0.2rem;
        padding: 0.5rem 0;
    }

    .ind-row__label {
        font-size: 0.72rem;
    }
}

@media (max-width: 576px) {
    .ind-page {
        padding: 1rem;
    }

    .ind-section__head {
        padding: 0.6rem 1rem;
    }

    .ind-section__body {
        padding: 0.6rem 1rem 0.8rem;
    }
}
</style>
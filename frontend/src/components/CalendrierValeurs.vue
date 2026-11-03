<script setup lang="ts">
import { computed, ref } from 'vue'
import { getCalendrierIndicateur } from '@/services/indicateurFront'
import type {
    CalendrierIndicateur,
    PeriodeCalendrier,
} from '@/types/backoffice/indicateur'

const props = defineProps<{
    idIndicateur: number
    anneeInitiale?: number
}>()

const emit = defineEmits<{
    (e: 'saisir', periode: PeriodeCalendrier): void
    (e: 'voir', periode: PeriodeCalendrier): void
}>()

const annee = ref(props.anneeInitiale ?? new Date().getFullYear())
const calendrier = ref<CalendrierIndicateur | null>(null)
const loading = ref(false)
const errorMessage = ref('')

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''
    const res = await getCalendrierIndicateur(props.idIndicateur, annee.value)
    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    calendrier.value = res.data
}

function anneePrecedente(): void {
    annee.value--
    charger()
}

function anneeSuivante(): void {
    annee.value++
    charger()
}

function cliquerCellule(p: PeriodeCalendrier): void {
    if (p.valeurId === null) {
        emit('saisir', p)
    } else {
        emit('voir', p)
    }
}

/* Classe de la grille selon la fréquence */
const classeGrille = computed(() => {
    const f = calendrier.value?.frequenceVerification ?? 'MENSUEL'
    if (f === 'MENSUEL') return 'grid--mensuel'
    if (f === 'TRIMESTRIEL') return 'grid--trimestriel'
    if (f === 'SEMESTRIEL') return 'grid--semestriel'
    return 'grid--annuel'
})

const libelleFrequence = computed(() => {
    const f = calendrier.value?.frequenceVerification ?? ''
    return {
        MENSUEL: 'Mensuel',
        TRIMESTRIEL: 'Trimestriel',
        SEMESTRIEL: 'Semestriel',
        ANNUEL: 'Annuel',
    }[f] ?? f
})

/* Helpers d'affichage */
function formaterValeur(v: number | null, unite: string | null): string {
    if (v === null) return '—'
    const formatted = v.toLocaleString('fr-FR', { maximumFractionDigits: 2 })
    return unite ? `${formatted} ${unite}` : formatted
}

function formaterDate(iso: string): string {
    const d = new Date(iso)
    return d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' })
}

function classeStatut(statut: string): string {
    return {
        ATTEINT: 'cell--ok',
        NON_ATTEINT: 'cell--ko',
        INDETERMINE: 'cell--indetermine',
        NON_SAISI: 'cell--vide',
        EN_COURS: 'cell--encours',
        MANQUANT: 'cell--manquant',
    }[statut] ?? ''
}

function titreCellule(p: PeriodeCalendrier): string {
    if (p.valeurId !== null) {
        return `${p.libelle} — ${formaterValeur(p.valeur, calendrier.value?.uniteMesure ?? null)}\n`
            + `Saisi le ${p.dateSaisie ? new Date(p.dateSaisie).toLocaleDateString('fr-FR') : '—'}\n`
            + `Par ${p.saisieParLibelle ?? '—'}\n`
            + (p.commentaire ?? '')
    }
    if (p.estFuture) return `${p.libelle} — À venir`
    if (p.estEnCours) return `${p.libelle} — En cours, cliquez pour saisir`
    return `${p.libelle} — Non saisi (en retard)`
}

// Chargement initial
charger()
</script>

<template>
    <section class="calendrier-panel">
        <!-- En-tête -->
        <header class="calendrier-header">
            <div class="calendrier-header__left">
                <h2>Calendrier {{ annee }}</h2>
                <span class="calendrier-header__freq">{{ libelleFrequence }}</span>
            </div>

            <div class="calendrier-header__nav">
                <button class="nav-btn" @click="anneePrecedente" aria-label="Année précédente">
                    <i class="bi bi-chevron-left"></i>
                </button>
                <span class="nav-year">{{ annee }}</span>
                <button class="nav-btn" @click="anneeSuivante" aria-label="Année suivante">
                    <i class="bi bi-chevron-right"></i>
                </button>
            </div>

            <div v-if="calendrier" class="calendrier-header__stats">
                <span class="stat">
                    <strong>{{ calendrier.nombreSaisies }}</strong>
                    / {{ calendrier.nombrePeriodes }} saisies
                </span>
                <span class="stat">
                    Moy. <strong>{{ calendrier.valeurMoyenne.toFixed(1) }}</strong>
                    <span v-if="calendrier.uniteMesure"> {{ calendrier.uniteMesure }}</span>
                </span>
            </div>
        </header>

        <!-- Erreur -->
        <div v-if="errorMessage" class="calendrier-error">
            <i class="bi bi-exclamation-triangle-fill"></i>
            {{ errorMessage }}
        </div>

        <!-- Loading -->
        <div v-else-if="loading" class="calendrier-loading">
            <div v-for="i in 12" :key="i" class="skeleton-cell" />
        </div>

        <!-- Grille -->
        <div v-else-if="calendrier" :class="['calendrier-grid', classeGrille]">
            <button v-for="p in calendrier.periodes" :key="p.libelle" :class="['cell', classeStatut(p.statut), {
                'cell--en-cours': p.estEnCours,
                'cell--future': p.estFuture,
                'cell--clickable': true,
            }]" :title="titreCellule(p)" @click="cliquerCellule(p)">
                <span class="cell__label">{{ p.libelle }}</span>

                <span class="cell__value">
                    {{ formaterValeur(p.valeur, calendrier.uniteMesure) }}
                </span>

                <span v-if="p.valeurId === null" class="cell__hint">
                    <i
                        :class="p.estFuture ? 'bi bi-calendar-plus' : p.estEnCours ? 'bi bi-plus-circle' : 'bi bi-exclamation-circle'"></i>
                    {{ p.estFuture ? 'À venir' : p.estEnCours ? 'Saisir' : 'Manquant' }}
                </span>

                <span v-else class="cell__meta">
                    {{ p.dateSaisie ? formaterDate(p.dateSaisie) : '' }}
                </span>

                <!-- Icône de statut -->
                <span v-if="p.statut === 'ATTEINT'" class="cell__badge cell__badge--ok">
                    <i class="bi bi-check-lg"></i>
                </span>
                <span v-else-if="p.statut === 'NON_ATTEINT'" class="cell__badge cell__badge--ko">
                    <i class="bi bi-x-lg"></i>
                </span>
            </button>
        </div>

        <!-- Légende -->
        <footer class="calendrier-legend">
            <span class="legend-item"><span class="dot dot--ok"></span> Atteint</span>
            <span class="legend-item"><span class="dot dot--ko"></span> Non atteint</span>
            <span class="legend-item"><span class="dot dot--encours"></span> En cours</span>
            <span class="legend-item"><span class="dot dot--manquant"></span> Manquant</span>
            <span class="legend-item"><span class="dot dot--vide"></span> À venir</span>
        </footer>
    </section>
</template>

<style scoped>
.calendrier-panel {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    padding: 1.25rem;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04), 0 8px 24px rgba(26, 42, 108, .06);
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

/* En-tête */
.calendrier-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
}

.calendrier-header__left {
    display: flex;
    align-items: baseline;
    gap: 0.6rem;
}

.calendrier-header__left h2 {
    margin: 0;
    font: 700 1.05rem Manrope, sans-serif;
    color: #1a2a6c;
}

.calendrier-header__freq {
    font-size: 0.72rem;
    font-weight: 700;
    color: #3b63d9;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    padding: 0.15rem 0.55rem;
    background: #eaf0ff;
    border-radius: 20px;
}

.calendrier-header__nav {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.nav-btn {
    display: grid;
    place-items: center;
    width: 32px;
    height: 32px;
    border: 1px solid #e6ebf3;
    background: #fff;
    border-radius: 8px;
    color: #6b7280;
    cursor: pointer;
    transition: background .12s, border-color .12s;
}

.nav-btn:hover {
    background: #f8fafc;
    border-color: #c3cad5;
}

.nav-year {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.95rem;
    font-weight: 700;
    color: #1a2a6c;
    min-width: 60px;
    text-align: center;
}

.calendrier-header__stats {
    display: flex;
    gap: 1.25rem;
    font-size: 0.8rem;
    color: #6b7280;
}

.calendrier-header__stats strong {
    color: #1a2a6c;
    font-variant-numeric: tabular-nums;
}

/* Grille */
.calendrier-grid {
    display: grid;
    gap: 0.5rem;
}

.grid--mensuel {
    grid-template-columns: repeat(4, 1fr);
}

.grid--trimestriel {
    grid-template-columns: repeat(4, 1fr);
}

.grid--semestriel {
    grid-template-columns: repeat(2, 1fr);
}

.grid--annuel {
    grid-template-columns: 1fr;
}

/* Cellule */
.cell {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    padding: 0.75rem 0.85rem;
    background: #f8fafc;
    border: 1px solid #eef1f5;
    border-radius: 10px;
    cursor: pointer;
    text-align: left;
    transition: transform .12s, box-shadow .12s, border-color .12s;
    min-height: 100px;
    font-family: inherit;
}

.cell:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(26, 42, 108, .08);
    border-color: #c3cad5;
}

.cell__label {
    font-size: 0.72rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: #6b7280;
}

.cell__value {
    font-family: Manrope, sans-serif;
    font-size: 1.1rem;
    font-weight: 800;
    color: #1a2a6c;
    font-variant-numeric: tabular-nums;
    line-height: 1.2;
}

.cell__hint {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
    font-size: 0.7rem;
    color: #8a95a8;
    font-style: italic;
    margin-top: auto;
}

.cell__hint i {
    font-size: 0.75rem;
}

.cell__meta {
    font-size: 0.68rem;
    color: #8a95a8;
    margin-top: auto;
}

.cell__badge {
    position: absolute;
    top: 0.5rem;
    right: 0.5rem;
    display: grid;
    place-items: center;
    width: 22px;
    height: 22px;
    border-radius: 50%;
    font-size: 0.7rem;
    color: #fff;
}

.cell__badge--ok {
    background: #1e7e34;
}

.cell__badge--ko {
    background: #d64545;
}

/* Statuts */
.cell--ok {
    background: #f0faf3;
    border-color: #c8e5cf;
}

.cell--ko {
    background: #fef5f5;
    border-color: #f4c7c7;
}

.cell--indetermine {
    background: #f8fafc;
    border-color: #e6ebf3;
}

.cell--vide {
    background: #fafbfc;
    border-color: #eef1f5;
}

.cell--encours {
    background: #eef4ff;
    border-color: #cde2f6;
    border-style: dashed;
}

.cell--manquant {
    background: #fff8e6;
    border-color: #f4e2c2;
    border-left: 3px solid #d4af37;
}

/* Légende */
.calendrier-legend {
    display: flex;
    gap: 1.25rem;
    flex-wrap: wrap;
    padding-top: 0.75rem;
    border-top: 1px dashed #eef1f5;
    font-size: 0.72rem;
    color: #6b7280;
}

.legend-item {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
}

.dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
}

.dot--ok {
    background: #1e7e34;
}

.dot--ko {
    background: #d64545;
}

.dot--encours {
    background: #4a6cf7;
}

.dot--manquant {
    background: #d4af37;
}

.dot--vide {
    background: #c3cad5;
}

/* Erreur / loading */
.calendrier-error {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.75rem 1rem;
    background: #fde9e9;
    color: #b03030;
    border-radius: 8px;
    font-size: 0.85rem;
}

.calendrier-loading {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 0.5rem;
}

.skeleton-cell {
    height: 100px;
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: shimmer 1.4s infinite;
    border-radius: 10px;
}

@keyframes shimmer {
    from {
        background-position: 200% 0;
    }

    to {
        background-position: -200% 0;
    }
}

/* Responsive */
@media (max-width: 767.98px) {
    .grid--mensuel {
        grid-template-columns: repeat(2, 1fr);
    }

    .grid--trimestriel {
        grid-template-columns: repeat(2, 1fr);
    }

    .calendrier-header {
        flex-direction: column;
        align-items: stretch;
    }

    .calendrier-header__nav {
        justify-content: center;
    }
}

@media (max-width: 500px) {
    .grid--mensuel {
        grid-template-columns: 1fr;
    }

    .cell {
        min-height: 80px;
    }
}
</style>
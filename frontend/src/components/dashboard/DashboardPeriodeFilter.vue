<script setup lang="ts">
import { computed, ref } from 'vue'
import { BaseCard, BaseInput, BaseSelect } from '@/components/base'
import type { ModePeriode, OptionPeriode } from '@/composables/useDashboardPeriode'

const props = defineProps<{
    annees: OptionPeriode[]
    moisOptions: OptionPeriode[]
    trimestres: OptionPeriode[]
    erreur?: string
}>()

const mode = defineModel<ModePeriode>('mode', { required: true })
const annee = defineModel<number>('annee', { required: true })
const mois = defineModel<number>('mois', { required: true })
const trimestre = defineModel<number>('trimestre', { required: true })
const dateDebut = defineModel<string>('dateDebut', { required: true })
const dateFin = defineModel<string>('dateFin', { required: true })

const filtresOuverts = ref(true)

const maintenant = new Date()
const anneeCourante = maintenant.getFullYear()
const moisCourant = maintenant.getMonth() + 1
const trimestreCourant = Math.floor(maintenant.getMonth() / 3) + 1

/** Ignore la sélection de l'option vide du select natif. */
function nombreValide(valeur: unknown, min: number, max: number): number | null {
    const n = Number(valeur)
    if (Number.isNaN(n) || n < min || n > max) return null
    return n
}

const anneeModel = computed({
    get: () => annee.value,
    set: (valeur: unknown) => {
        const n = nombreValide(valeur, 1900, 2100)
        if (n !== null) annee.value = n
    },
})

const moisModel = computed({
    get: () => mois.value,
    set: (valeur: unknown) => {
        const n = nombreValide(valeur, 1, 12)
        if (n !== null) mois.value = n
    },
})

const trimestreModel = computed({
    get: () => trimestre.value,
    set: (valeur: unknown) => {
        const n = nombreValide(valeur, 1, 4)
        if (n !== null) trimestre.value = n
    },
})

const modes: Array<{ value: ModePeriode; label: string }> = [
    { value: 'annee', label: 'Année' },
    { value: 'mois', label: 'Mois' },
    { value: 'trimestre', label: 'Trimestre' },
    { value: 'personnalise', label: 'Personnalisé' },
]

const periodeModifiee = computed(
    () => mode.value !== 'annee' || annee.value !== anneeCourante
)

function formatCourt(iso: string): string {
    const [a, m, j] = iso.split('-')
    if (!a || !m || !j) return iso
    return `${j}/${m}/${a}`
}

const libellePeriode = computed(() => {
    if (mode.value === 'annee') return String(annee.value)

    if (mode.value === 'mois') {
        const option = props.moisOptions.find(o => o.value === mois.value)
        return `${option?.label ?? ''} ${annee.value}`.trim()
    }

    if (mode.value === 'trimestre') {
        const option = props.trimestres.find(o => o.value === trimestre.value)
        return `${option?.label ?? ''} ${annee.value}`.trim()
    }

    if (!dateDebut.value || !dateFin.value) return 'Période personnalisée'
    return `${formatCourt(dateDebut.value)} – ${formatCourt(dateFin.value)}`
})

function basculerPanneau(): void {
    filtresOuverts.value = !filtresOuverts.value
}

function reinitialiser(): void {
    mode.value = 'annee'
    annee.value = anneeCourante
    mois.value = moisCourant
    trimestre.value = trimestreCourant
    dateDebut.value = `${anneeCourante}-01-01`
    dateFin.value = `${anneeCourante}-12-31`
}
</script>

<template>
    <BaseCard class="filtres" noPadding>
        <header class="filtres__header">
            <button type="button" class="filtres__toggle" :aria-expanded="filtresOuverts"
                aria-controls="periode-filtres-body" @click="basculerPanneau">
                <span class="filtres__header-icon" aria-hidden="true">
                    <i class="bi bi-sliders"></i>
                </span>
                <span class="filtres__header-text">
                    <span class="filtres__header-title">Période</span>
                    <span class="filtres__header-sub">
                        {{ filtresOuverts ? 'Cliquez pour replier' : 'Cliquez pour déplier' }}
                    </span>
                </span>
                <span class="filtres__count">{{ libellePeriode }}</span>
                <i class="bi bi-chevron-down filtres__chev" :class="{ 'filtres__chev--open': filtresOuverts }"
                    aria-hidden="true"></i>
            </button>

            <button v-if="periodeModifiee" type="button" class="filtres__clear" @click="reinitialiser">
                <i class="bi bi-x-lg"></i>
                Réinitialiser
            </button>
        </header>

        <div id="periode-filtres-body" class="filtres__collapse"
            :class="{ 'filtres__collapse--open': filtresOuverts }">
            <div class="filtres__collapse-inner">
                <div class="filtres__body">
                    <div class="filtres__section">
                        <span class="filtres__section-label">Mode de période</span>
                        <div class="filtres__modes" role="group" aria-label="Mode de période">
                            <button v-for="m in modes" :key="m.value" type="button"
                                :class="['filtres__mode', { 'filtres__mode--active': mode === m.value }]"
                                :aria-pressed="mode === m.value" @click="mode = m.value">
                                {{ m.label }}
                            </button>
                        </div>
                    </div>

                    <div class="filtres__section">
                        <span class="filtres__section-label">Période</span>
                        <div class="filtres__section-grid">
                            <div v-if="mode !== 'personnalise'" class="filtres__col">
                                <BaseSelect v-model="anneeModel" label="Année" :options="annees" />
                            </div>

                            <div v-if="mode === 'mois'" class="filtres__col">
                                <BaseSelect v-model="moisModel" label="Mois" :options="moisOptions" />
                            </div>

                            <div v-if="mode === 'trimestre'" class="filtres__col">
                                <BaseSelect v-model="trimestreModel" label="Trimestre" :options="trimestres" />
                            </div>

                            <template v-if="mode === 'personnalise'">
                                <div class="filtres__col">
                                    <BaseInput v-model="dateDebut" label="Date début" type="date" />
                                </div>
                                <div class="filtres__col">
                                    <BaseInput v-model="dateFin" label="Date fin" type="date" />
                                </div>
                            </template>
                        </div>
                    </div>

                    <p v-if="erreur" class="filtres__error" role="alert">
                        <i class="bi bi-exclamation-triangle"></i>
                        {{ erreur }}
                    </p>
                </div>

                <footer class="filtres__footer">
                    <span class="filtres__hint">
                        <i class="bi bi-info-circle"></i>
                        Les indicateurs se mettent à jour automatiquement
                    </span>

                    <div class="filtres__footer-actions">
                        <button type="button" class="filtres__reset" @click="reinitialiser">
                            <i class="bi bi-arrow-counterclockwise"></i>
                            Réinitialiser
                        </button>
                        <button type="button" class="filtres__close" @click="basculerPanneau">
                            <i class="bi bi-chevron-up"></i>
                            Replier
                        </button>
                    </div>
                </footer>
            </div>
        </div>
    </BaseCard>
</template>

<style scoped>
.filtres {
    overflow: visible;
}

.filtres__header {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.9rem 1.25rem;
    background: linear-gradient(140deg, #f7f9fd, #fff);
    border-bottom: 1px solid #e6ebf3;
}

.filtres__toggle {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex: 1;
    min-width: 0;
    padding: 0.25rem;
    border: none;
    background: transparent;
    text-align: left;
    cursor: pointer;
    border-radius: 8px;
    transition: background .15s ease;
}

.filtres__toggle:hover {
    background: rgba(74, 108, 247, .04);
}

.filtres__toggle:focus-visible {
    outline: 2px solid #4a6cf7;
    outline-offset: 2px;
}

.filtres__header-icon {
    display: grid;
    place-items: center;
    width: 34px;
    height: 34px;
    border-radius: 9px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 1rem;
    flex: 0 0 34px;
}

.filtres__header-text {
    display: flex;
    flex-direction: column;
    min-width: 0;
    flex: 1;
}

.filtres__header-title {
    font-size: 0.95rem;
    font-weight: 700;
    color: #1a2a6c;
    font-family: Manrope, "DM Sans", sans-serif;
    line-height: 1.2;
}

.filtres__header-sub {
    font-size: 0.75rem;
    color: #6b7280;
    line-height: 1.2;
    margin-top: 2px;
}

.filtres__count {
    display: inline-flex;
    align-items: center;
    padding: 0.2rem 0.65rem;
    background: #eaf0ff;
    border: 1px solid #d9e2ff;
    border-radius: 20px;
    font-size: 0.72rem;
    font-weight: 600;
    color: #3b63d9;
    flex: none;
    white-space: nowrap;
}

.filtres__chev {
    font-size: 0.9rem;
    color: #8a95a8;
    flex: none;
    transition: transform .25s ease, color .15s ease;
}

.filtres__chev--open {
    transform: rotate(180deg);
    color: #4a6cf7;
}

.filtres__clear {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.32rem 0.65rem;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    background: #fff;
    color: #6b7280;
    font-size: 0.75rem;
    font-weight: 600;
    cursor: pointer;
    flex: none;
    transition: background .15s, color .15s, border-color .15s;
}

.filtres__clear:hover {
    border-color: #d84e5b;
    color: #d84e5b;
    background: #fff0f0;
}

.filtres__collapse {
    display: grid;
    grid-template-rows: 0fr;
    transition: grid-template-rows .3s ease;
}

.filtres__collapse--open {
    grid-template-rows: 1fr;
}

.filtres__collapse-inner {
    overflow: hidden;
    min-height: 0;
}

.filtres__body {
    display: grid;
    gap: 1.25rem;
    padding: 1.25rem;
}

.filtres__section {
    display: grid;
    gap: 0.6rem;
}

.filtres__section-label {
    display: block;
    font-size: 0.68rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: #8a95a8;
}

.filtres__modes {
    display: inline-flex;
    flex-wrap: wrap;
    align-self: flex-start;
    gap: 0.25rem;
    padding: 0.25rem;
    border-radius: 10px;
    background: #eef2f8;
}

.filtres__mode {
    border: none;
    background: transparent;
    padding: 0.4rem 0.9rem;
    border-radius: 8px;
    font: 600 0.78rem "DM Sans", system-ui, sans-serif;
    color: #6b7280;
    cursor: pointer;
    transition: background .15s ease, color .15s ease, box-shadow .15s ease;
}

.filtres__mode:hover {
    color: #1a2a6c;
}

.filtres__mode--active {
    background: #fff;
    color: #1a2a6c;
    box-shadow: 0 1px 4px rgba(15, 23, 42, .12);
}

.filtres__section-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1rem;
}

.filtres__col {
    position: relative;
    min-width: 0;
}

.filtres__error {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    margin: 0;
    padding: 0.55rem 0.75rem;
    border: 1px solid #f5c2c0;
    border-radius: 8px;
    background: #fff4f4;
    color: #a61b16;
    font-size: 0.78rem;
}

.filtres__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
    padding: 0.85rem 1.25rem;
    background: #f7f9fd;
    border-top: 1px solid #e6ebf3;
}

.filtres__hint {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    font-size: 0.75rem;
    color: #8a95a8;
}

.filtres__footer-actions {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-left: auto;
}

.filtres__reset,
.filtres__close {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.45rem 0.9rem;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    background: #fff;
    color: #1a2a6c;
    font-size: 0.8rem;
    font-weight: 600;
    cursor: pointer;
    transition: background .15s, color .15s, border-color .15s;
}

.filtres__reset:hover {
    border-color: #4a6cf7;
    color: #4a6cf7;
    background: #f3f6fc;
}

.filtres__close {
    background: linear-gradient(135deg, #1a2a6c 0%, #2a3f94 100%);
    color: #fff;
    border-color: transparent;
}

.filtres__close:hover {
    background: linear-gradient(135deg, #22358a 0%, #3350b3 100%);
    color: #fff;
}

@media (max-width: 640px) {
    .filtres__section-grid {
        grid-template-columns: 1fr;
    }

    .filtres__modes {
        align-self: stretch;
    }

    .filtres__mode {
        flex: 1 1 auto;
    }
}
</style>

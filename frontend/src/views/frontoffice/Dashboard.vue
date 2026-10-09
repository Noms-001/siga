<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import BaseSelect from '@/components/base/BaseSelect/BaseSelect.vue'
import DashboardKpiCards from '@/components/dashboard/DashboardKpiCards.vue'
import DashboardAvancement from '@/components/dashboard/DashboardAvancement.vue'
import DashboardStatuts from '@/components/dashboard/DashboardStatuts.vue'
import DashboardServices from '@/components/dashboard/DashboardServices.vue'
import DashboardPta from '@/components/dashboard/DashboardPta.vue'
import DashboardPriorites from '@/components/dashboard/DashboardPriorites.vue'
import DashboardValidations from '@/components/dashboard/DashboardValidations.vue'
import DashboardEcheances from '@/components/dashboard/DashboardEcheances.vue'
import DashboardRisquesIncidents from '@/components/dashboard/DashboardRisquesIncidents.vue'
import {
    getDashboardAvancement,
    getDashboardEcheances,
    getDashboardKpi,
    getDashboardPriorites,
    getDashboardPta,
    getDashboardRisquesIncidents,
    getDashboardServices,
    getDashboardStatuts,
    getDashboardValidations,
} from '@/services/dashboard'
import type {
    DashboardAvancement as Avancement,
    DashboardEcheances as EcheancesData,
    DashboardKpi,
    DashboardPriorite,
    DashboardPtaComparaison,
    DashboardRisquesIncidents as RisquesIncidentsData,
    DashboardService,
    DashboardStatut,
    DashboardValidation,
} from '@/types/dashboard'
import { getCurrentYear } from '@/utils/date'

/* -------------------- année -------------------- */

/**
 * Année par défaut : 2027.
 *
 * Le projet travaille actuellement principalement sur cette année. La
 * valeur n'est PAS une constante figée : l'utilisateur peut la changer,
 * et le sélecteur propose une plage autour.
 */
const annee = ref<number>(getCurrentYear())

const anneesDisponibles = computed(() => {
    const base = getCurrentYear()
    const plage = [base - 2, base - 1, base, base + 1, base + 2]
    return plage.map(y => ({ value: y, label: String(y) }))
})

/* -------------------- état par section -------------------- */
/* Chaque section a son propre loading/error : une API en échec ne casse
   pas les autres blocs. */

const kpi = ref<DashboardKpi | null>(null)
const kpiLoading = ref(false)
const kpiError = ref('')

const avancement = ref<Avancement | null>(null)
const avancementLoading = ref(false)
const avancementError = ref('')

const statuts = ref<DashboardStatut[]>([])
const statutsLoading = ref(false)
const statutsError = ref('')

const services = ref<DashboardService[]>([])
const servicesLoading = ref(false)
const servicesError = ref('')

const comparaisonPta = ref<DashboardPtaComparaison | null>(null)
const ptaLoading = ref(false)
const ptaError = ref('')

const priorites = ref<DashboardPriorite[]>([])
const prioritesLoading = ref(false)
const prioritesError = ref('')

const validations = ref<DashboardValidation[]>([])
const validationsLoading = ref(false)
const validationsError = ref('')

const echeances = ref<EcheancesData | null>(null)
const echeancesLoading = ref(false)
const echeancesError = ref('')

// …

const risques = ref<RisquesIncidentsData | null>(null)
const risquesLoading = ref(false)
const risquesError = ref('')

/* -------------------- chargements -------------------- */

async function chargerKpi(): Promise<void> {
    kpiLoading.value = true
    kpiError.value = ''
    const res = await getDashboardKpi(annee.value)
    kpiLoading.value = false
    if (!res.success) { kpiError.value = res.error; return }
    kpi.value = res.data
}

async function chargerAvancement(): Promise<void> {
    avancementLoading.value = true
    avancementError.value = ''
    const res = await getDashboardAvancement(annee.value)
    avancementLoading.value = false
    if (!res.success) { avancementError.value = res.error; return }
    avancement.value = res.data
}

async function chargerStatuts(): Promise<void> {
    statutsLoading.value = true
    statutsError.value = ''
    const res = await getDashboardStatuts(annee.value)
    statutsLoading.value = false
    if (!res.success) { statutsError.value = res.error; return }
    statuts.value = res.data
}

async function chargerServices(): Promise<void> {
    servicesLoading.value = true
    servicesError.value = ''
    const res = await getDashboardServices(annee.value)
    servicesLoading.value = false
    if (!res.success) { servicesError.value = res.error; return }
    services.value = res.data
}

async function chargerPta(): Promise<void> {
    ptaLoading.value = true
    ptaError.value = ''
    const res = await getDashboardPta(annee.value)
    ptaLoading.value = false
    if (!res.success) { ptaError.value = res.error; return }
    comparaisonPta.value = res.data
}

async function chargerPriorites(): Promise<void> {
    prioritesLoading.value = true
    prioritesError.value = ''
    const res = await getDashboardPriorites(annee.value, 10)
    prioritesLoading.value = false
    if (!res.success) { prioritesError.value = res.error; return }
    priorites.value = res.data
}

async function chargerValidations(): Promise<void> {
    validationsLoading.value = true
    validationsError.value = ''
    const res = await getDashboardValidations(annee.value)
    validationsLoading.value = false
    if (!res.success) { validationsError.value = res.error; return }
    validations.value = res.data
}

async function chargerEcheances(): Promise<void> {
    echeancesLoading.value = true
    echeancesError.value = ''
    const res = await getDashboardEcheances(annee.value, 7)
    echeancesLoading.value = false
    if (!res.success) { echeancesError.value = res.error; return }
    echeances.value = res.data
}

async function chargerRisques(): Promise<void> {
    risquesLoading.value = true
    risquesError.value = ''
    const res = await getDashboardRisquesIncidents(annee.value)
    risquesLoading.value = false
    if (!res.success) { risquesError.value = res.error; return }
    risques.value = res.data
}

/**
 * Recharge toutes les sections en parallèle.
 *
 * Aucun await global : chaque section gère son propre état, et le
 * rechargement d'une année n'attend pas qu'une API lente bloque les
 * autres. Les appels partent tous en même temps.
 */
function chargerTout(): void {
    void chargerKpi()
    void chargerAvancement()
    void chargerStatuts()
    void chargerServices()
    void chargerPta()
    void chargerPriorites()
    void chargerValidations()
    void chargerEcheances()
    void chargerRisques()
}

/* -------------------- cycle de vie -------------------- */

onMounted(chargerTout)

watch(annee, () => {
    chargerTout()
})
</script>

<template>
    <div class="dash">
        <!-- En-tête -->
        <header class="dash__header">
            <div>
                <h1 class="dash__title">Dashboard TSS</h1>
                <p class="dash__subtitle">
                    Vue d'ensemble de la gestion des activités
                </p>
            </div>

            <div class="dash__annee">
                <label class="dash__annee-label">Année</label>
                <BaseSelect v-model="annee" size="sm" :options="anneesDisponibles" />
            </div>
        </header>

        <!-- KPI -->
        <section class="dash__section">
            <div v-if="kpiError" class="dash__error" role="alert">
                <span>{{ kpiError }}</span>
                <button type="button" @click="chargerKpi">Réessayer</button>
            </div>
            <DashboardKpiCards v-else :kpi="kpi" :loading="kpiLoading" />
        </section>

        <!-- Avancement + statuts -->
        <section class="dash__grid-2">
            <div>
                <div v-if="avancementError" class="dash__error" role="alert">
                    <span>{{ avancementError }}</span>
                    <button type="button" @click="chargerAvancement">Réessayer</button>
                </div>
                <DashboardAvancement v-else
                    :avancement="avancement" :loading="avancementLoading" />
            </div>

            <div>
                <div v-if="statutsError" class="dash__error" role="alert">
                    <span>{{ statutsError }}</span>
                    <button type="button" @click="chargerStatuts">Réessayer</button>
                </div>
                <DashboardStatuts v-else
                    :statuts="statuts" :loading="statutsLoading" />
            </div>
        </section>

        <!-- Services -->
        <section class="dash__section">
            <div v-if="servicesError" class="dash__error" role="alert">
                <span>{{ servicesError }}</span>
                <button type="button" @click="chargerServices">Réessayer</button>
            </div>
            <DashboardServices v-else
                :services="services" :loading="servicesLoading" />
        </section>

        <!-- PTA / NON-PTA -->
        <section class="dash__section">
            <div v-if="ptaError" class="dash__error" role="alert">
                <span>{{ ptaError }}</span>
                <button type="button" @click="chargerPta">Réessayer</button>
            </div>
            <DashboardPta v-else
                :comparaison="comparaisonPta" :loading="ptaLoading" />
        </section>

        <!-- Priorités + échéances -->
        <section class="dash__grid-2 d-flex flex-column">
            <div>
                <div v-if="prioritesError" class="dash__error" role="alert">
                    <span>{{ prioritesError }}</span>
                    <button type="button" @click="chargerPriorites">Réessayer</button>
                </div>
                <DashboardPriorites v-else
                    :priorites="priorites" :loading="prioritesLoading" />
            </div>
            <div>
                <div v-if="validationsError" class="dash__error" role="alert">
                    <span>{{ validationsError }}</span>
                    <button type="button" @click="chargerValidations">Réessayer</button>
                </div>
                <DashboardValidations v-else
                    :validations="validations" :loading="validationsLoading" />
            </div>
        </section>

        <!-- Échéances -->
        <section class="dash__section">
            <div v-if="echeancesError" class="dash__error" role="alert">
                <span>{{ echeancesError }}</span>
                <button type="button" @click="chargerEcheances">Réessayer</button>
            </div>
            <DashboardEcheances v-else
                :echeances="echeances" :loading="echeancesLoading" />
        </section>

        <!-- Risques / incidents -->
        <section v-if="risquesError || (risques?.plansAction?.total ?? 0) > 0 || risquesLoading"
            class="dash__section">
            <div v-if="risquesError" class="dash__error" role="alert">
                <span>{{ risquesError }}</span>
                <button type="button" @click="chargerRisques">Réessayer</button>
            </div>
            <DashboardRisquesIncidents v-else
                :data="risques" :loading="risquesLoading" />
        </section>
    </div>
</template>

<style scoped>
.dash {
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
}

.dash__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    gap: 1rem;
    flex-wrap: wrap;
}

.dash__title {
    font-size: 1.5rem;
    font-weight: 700;
    color: var(--dts-navy);
    margin: 0;
}

.dash__subtitle {
    color: var(--dts-muted);
    margin: 0.15rem 0 0;
    font-size: 0.88rem;
}

.dash__annee {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    min-width: 160px;
}

.dash__annee-label {
    font-size: 0.78rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--dts-muted);
}

.dash__section { display: flex; flex-direction: column; gap: 0.75rem; }

.dash__grid-2 {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1.5rem;
}

.dash__error {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;
    padding: 0.75rem 1rem;
    border-radius: var(--dts-radius-sm);
    background: #fdecea;
    border: 1px solid #f6cfcc;
    color: #5c1a17;
    font-size: 0.85rem;
}

.dash__error button {
    padding: 0.25rem 0.75rem;
    border-radius: 6px;
    border: 1px solid #f6cfcc;
    background: #fff;
    color: #a61b16;
    font-size: 0.78rem;
    font-weight: 600;
    cursor: pointer;
}

.dash__error button:hover {
    background: #fdf3f2;
}

@media (max-width: 992px) {
    .dash__grid-2,
    .dash__grid-2--large-left {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 576px) {
    .dash { padding: 1rem; }
    .dash__title { font-size: 1.25rem; }
}
</style>
<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import DashboardKpiCards from '@/components/dashboard/DashboardKpiCards.vue'
import DashboardAvancement from '@/components/dashboard/DashboardAvancement.vue'
import DashboardStatuts from '@/components/dashboard/DashboardStatuts.vue'
import DashboardStatutsPriorites from '@/components/dashboard/DashboardStatutsPriorites.vue'
import DashboardServices from '@/components/dashboard/DashboardServices.vue'
import DashboardPta from '@/components/dashboard/DashboardPta.vue'
import DashboardEcheances from '@/components/dashboard/DashboardEcheances.vue'
import DashboardPlansAction from '@/components/dashboard/DashboardPlansAction.vue'
import DashboardPlansActionOrigine from '@/components/dashboard/DashboardPlansActionOrigine.vue'
import DashboardTypesActivite from '@/components/dashboard/DashboardTypesActivite.vue'
import DashboardPeriodeFilter from '@/components/dashboard/DashboardPeriodeFilter.vue'
import { useDashboardPeriode } from '@/composables/useDashboardPeriode'
import {
    getDashboardAvancement,
    getDashboardEcheances,
    getDashboardKpi,
    getDashboardPerimetre,
    getDashboardPlanActionOrigine,
    getDashboardPta,
    getDashboardServices,
    getDashboardStatuts,
    getDashboardStatutsPriorites,
    getDashboardTypesActivite,
} from '@/services/dashboard'
import type {
    DashboardAvancement as Avancement,
    DashboardEcheances as EcheancesData,
    DashboardKpi,
    DashboardPerimetre,
    DashboardPlanActionOrigine as PlanActionOrigine,
    DashboardPtaComparaison,
    DashboardService,
    DashboardStatut,
    DashboardStatutPriorite,
    DashboardTypeActivite,
} from '@/types/dashboard'
import { getSuiviGlobalPlanAction } from '@/services/planaction'
import type { SuiviGlobalPlanAction } from '@/types/planaction'
import { useRouter } from 'vue-router'

/* -------------------- filtre temporel global -------------------- */

const router = useRouter()

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

const nouvelleActivite = () => {
    router.push('/activites/nouvelle')
}

const {
    mode,
    annee,
    mois,
    trimestre,
    dateDebut,
    dateFin,
    annees,
    moisOptions,
    trimestres,
    erreur: periodeErreur,
    periode,
} = useDashboardPeriode()

/* -------------------- périmètre et filtre service -------------------- */

const perimetre = ref<DashboardPerimetre | null>(null)
const serviceId = ref(0)
const pret = ref(false)

const filtreServiceVisible = computed(
    () => perimetre.value?.niveau === 'DEPARTEMENT'
)
const servicesPerimetre = computed(() => perimetre.value?.services ?? [])
const serviceIdParam = computed(() =>
    serviceId.value > 0 ? serviceId.value : undefined
)

async function chargerPerimetre(): Promise<void> {
    const res = await getDashboardPerimetre()
    if (!res.success) return
    perimetre.value = res.data
    serviceId.value = res.data.niveau === 'SERVICE'
        ? (res.data.idService ?? 0)
        : 0
}

/* -------------------- état par section -------------------- */

const kpi = ref<DashboardKpi | null>(null)
const kpiLoading = ref(false)
const kpiError = ref('')

const avancement = ref<Avancement | null>(null)
const avancementLoading = ref(false)
const avancementError = ref('')

const statuts = ref<DashboardStatut[]>([])
const statutsLoading = ref(false)
const statutsError = ref('')

const statutsPriorites = ref<DashboardStatutPriorite[]>([])
const statutsPrioritesLoading = ref(false)
const statutsPrioritesError = ref('')

const services = ref<DashboardService[]>([])
const servicesLoading = ref(false)
const servicesError = ref('')

const comparaisonPta = ref<DashboardPtaComparaison | null>(null)
const ptaLoading = ref(false)
const ptaError = ref('')

const echeances = ref<EcheancesData | null>(null)
const echeancesLoading = ref(false)
const echeancesError = ref('')

const suiviPlans = ref<SuiviGlobalPlanAction | null>(null)
const suiviPlansLoading = ref(false)
const suiviPlansError = ref('')

const typesActivite = ref<DashboardTypeActivite[]>([])
const typesActiviteLoading = ref(false)
const typesActiviteError = ref('')

const plansOrigine = ref<PlanActionOrigine[]>([])
const plansOrigineLoading = ref(false)
const plansOrigineError = ref('')

/**
 * Jeton de génération : une réponse arrivée après un changement de
 * période est ignorée, pour ne jamais afficher des données obsolètes.
 */
let generation = 0

/** Jeton propre au graphique « types » : le filtre service peut changer
 *  plus vite que la période, il faut ignorer les réponses obsolètes. */
let typesRequeteId = 0

/** Jeton propre au graphique statut × priorité (filtre service). */
let statutsPrioritesRequeteId = 0

/* -------------------- chargements -------------------- */

async function chargerKpi(): Promise<void> {
    const p = periode.value
    if (!p) return
    const gen = generation
    kpiLoading.value = true
    kpiError.value = ''
    const res = await getDashboardKpi(p)
    if (gen !== generation) return
    kpiLoading.value = false
    if (!res.success) { kpiError.value = res.error; return }
    kpi.value = res.data
}

async function chargerAvancement(): Promise<void> {
    const p = periode.value
    if (!p) return
    const gen = generation
    avancementLoading.value = true
    avancementError.value = ''
    const res = await getDashboardAvancement(p)
    if (gen !== generation) return
    avancementLoading.value = false
    if (!res.success) { avancementError.value = res.error; return }
    avancement.value = res.data
}

async function chargerStatuts(): Promise<void> {
    const p = periode.value
    if (!p) return
    const gen = generation
    statutsLoading.value = true
    statutsError.value = ''
    const res = await getDashboardStatuts(p)
    if (gen !== generation) return
    statutsLoading.value = false
    if (!res.success) { statutsError.value = res.error; return }
    statuts.value = res.data
}

async function chargerStatutsPriorites(): Promise<void> {
    const p = periode.value
    if (!p) return
    const gen = generation
    const id = ++statutsPrioritesRequeteId
    statutsPrioritesLoading.value = true
    statutsPrioritesError.value = ''
    const res = await getDashboardStatutsPriorites(p, serviceIdParam.value)
    if (gen !== generation || id !== statutsPrioritesRequeteId) return
    statutsPrioritesLoading.value = false
    if (!res.success) { statutsPrioritesError.value = res.error; return }
    statutsPriorites.value = res.data
}

async function chargerServices(): Promise<void> {
    const p = periode.value
    if (!p) return
    const gen = generation
    servicesLoading.value = true
    servicesError.value = ''
    const res = await getDashboardServices(p)
    if (gen !== generation) return
    servicesLoading.value = false
    if (!res.success) { servicesError.value = res.error; return }
    services.value = res.data
}

async function chargerPta(): Promise<void> {
    const p = periode.value
    if (!p) return
    const gen = generation
    ptaLoading.value = true
    ptaError.value = ''
    const res = await getDashboardPta(p)
    if (gen !== generation) return
    ptaLoading.value = false
    if (!res.success) { ptaError.value = res.error; return }
    comparaisonPta.value = res.data
}

async function chargerEcheances(): Promise<void> {
    const p = periode.value
    if (!p) return
    const gen = generation
    echeancesLoading.value = true
    echeancesError.value = ''
    const res = await getDashboardEcheances(p, 7)
    if (gen !== generation) return
    echeancesLoading.value = false
    if (!res.success) { echeancesError.value = res.error; return }
    echeances.value = res.data
}

async function chargerSuiviPlans(): Promise<void> {
    const p = periode.value
    if (!p) return
    const gen = generation
    suiviPlansLoading.value = true
    suiviPlansError.value = ''
    const res = await getSuiviGlobalPlanAction(p)
    if (gen !== generation) return
    suiviPlansLoading.value = false
    if (!res.success) { suiviPlansError.value = res.error; return }
    suiviPlans.value = res.data
}

async function chargerTypesActivite(): Promise<void> {
    const p = periode.value
    if (!p) return
    const gen = generation
    const id = ++typesRequeteId
    typesActiviteLoading.value = true
    typesActiviteError.value = ''
    const res = await getDashboardTypesActivite(p, serviceIdParam.value)
    if (gen !== generation || id !== typesRequeteId) return
    typesActiviteLoading.value = false
    if (!res.success) { typesActiviteError.value = res.error; return }
    typesActivite.value = res.data
}

async function chargerPlansOrigine(): Promise<void> {
    const p = periode.value
    if (!p) return
    const gen = generation
    plansOrigineLoading.value = true
    plansOrigineError.value = ''
    const res = await getDashboardPlanActionOrigine(p)
    if (gen !== generation) return
    plansOrigineLoading.value = false
    if (!res.success) { plansOrigineError.value = res.error; return }
    plansOrigine.value = res.data
}

/**
 * Recharge toutes les sections en parallèle — aucun await global,
 * chaque section gère son propre état.
 */
function chargerTout(): void {
    generation++
    if (!periode.value) return
    void chargerKpi()
    void chargerAvancement()
    void chargerStatuts()
    void chargerStatutsPriorites()
    void chargerServices()
    void chargerPta()
    void chargerEcheances()
    void chargerSuiviPlans()
    void chargerTypesActivite()
    void chargerPlansOrigine()
}

/* -------------------- cycle de vie -------------------- */

onMounted(async () => {
    await chargerPerimetre()
    chargerTout()
    pret.value = true
})

watch(periode, () => {
    chargerTout()
})

watch(serviceId, () => {
    if (pret.value) {
        void chargerTypesActivite()
        void chargerStatutsPriorites()
    }
})
</script>

<template>
    <div class="dash">
        <!-- En-tête -->
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>
                    Dashboard
                    <span class="wave" aria-hidden="true">✳</span>
                </h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    Voici ce qui se passe dans votre espace aujourd’hui.
                </p>
            </div>

            <div class="welcome-row__right">
                <button type="button" class="button primary" @click="nouvelleActivite">
                    <span class="button-plus" aria-hidden="true">＋</span>
                    <span>Nouvelle activité</span>
                </button>
            </div>
        </header>

        <!-- Filtre temporel global -->
        <section class="dash__section">
            <DashboardPeriodeFilter v-model:mode="mode" v-model:annee="annee" v-model:mois="mois"
                v-model:trimestre="trimestre" v-model:date-debut="dateDebut" v-model:date-fin="dateFin"
                :annees="annees" :mois-options="moisOptions" :trimestres="trimestres"
                :erreur="periodeErreur" />
        </section>

        <!-- KPI -->
        <section class="dash__section">
            <div v-if="kpiError" class="dash__error" role="alert">
                <span>{{ kpiError }}</span>
                <button type="button" @click="chargerKpi">Réessayer</button>
            </div>
            <DashboardKpiCards v-else :kpi="kpi" :loading="kpiLoading" />
        </section>

        <!-- Avancement + statuts (camembert) -->
        <section class="dash__grid-2">
            <div>
                <div v-if="avancementError" class="dash__error" role="alert">
                    <span>{{ avancementError }}</span>
                    <button type="button" @click="chargerAvancement">Réessayer</button>
                </div>
                <DashboardAvancement v-else :avancement="avancement" :loading="avancementLoading" />
            </div>

            <div>
                <div v-if="statutsError" class="dash__error" role="alert">
                    <span>{{ statutsError }}</span>
                    <button type="button" @click="chargerStatuts">Réessayer</button>
                </div>
                <DashboardStatuts v-else :statuts="statuts" :loading="statutsLoading" />
            </div>
        </section>

        <!-- Répartition des activités par statut et priorité -->
        <section class="dash__section">
            <div v-if="statutsPrioritesError" class="dash__error" role="alert">
                <span>{{ statutsPrioritesError }}</span>
                <button type="button" @click="chargerStatutsPriorites">Réessayer</button>
            </div>
            <DashboardStatutsPriorites v-else :data="statutsPriorites"
                :loading="statutsPrioritesLoading" />
        </section>

        <!-- Avancement par type d'activité -->
        <section class="dash__section">
            <div v-if="typesActiviteError" class="dash__error" role="alert">
                <span>{{ typesActiviteError }}</span>
                <button type="button" @click="chargerTypesActivite">Réessayer</button>
            </div>
            <DashboardTypesActivite v-else :types="typesActivite" :loading="typesActiviteLoading"
                :services="servicesPerimetre" :service-id="serviceId"
                :filtre-service-visible="filtreServiceVisible"
                @update:service-id="serviceId = $event" />
        </section>

        <!-- Services -->
        <section class="dash__section">
            <div v-if="servicesError" class="dash__error" role="alert">
                <span>{{ servicesError }}</span>
                <button type="button" @click="chargerServices">Réessayer</button>
            </div>
            <DashboardServices v-else :services="services" :loading="servicesLoading" />
        </section>

        <!-- PTA / NON-PTA -->
        <section class="dash__section">
            <div v-if="ptaError" class="dash__error" role="alert">
                <span>{{ ptaError }}</span>
                <button type="button" @click="chargerPta">Réessayer</button>
            </div>
            <DashboardPta v-else :comparaison="comparaisonPta" :loading="ptaLoading" />
        </section>

        <!-- Échéances -->
        <section class="dash__section">
            <div v-if="echeancesError" class="dash__error" role="alert">
                <span>{{ echeancesError }}</span>
                <button type="button" @click="chargerEcheances">Réessayer</button>
            </div>
            <DashboardEcheances v-else :echeances="echeances" :loading="echeancesLoading" />
        </section>

        <!-- Suivi des plans d'action -->
        <section v-if="suiviPlansError || (suiviPlans?.resume.total ?? 0) > 0 || suiviPlansLoading"
            class="dash__section">
            <div v-if="suiviPlansError" class="dash__error" role="alert">
                <span>{{ suiviPlansError }}</span>
                <button type="button" @click="chargerSuiviPlans">Réessayer</button>
            </div>
            <DashboardPlansAction v-else :suivi="suiviPlans" :loading="suiviPlansLoading" />
        </section>

        <!-- Avancement des plans d'action par origine -->
        <section class="dash__section">
            <div v-if="plansOrigineError" class="dash__error" role="alert">
                <span>{{ plansOrigineError }}</span>
                <button type="button" @click="chargerPlansOrigine">Réessayer</button>
            </div>
            <DashboardPlansActionOrigine v-else :data="plansOrigine" :loading="plansOrigineLoading" />
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

/* Bouton primaire */
.button.primary {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    height: 42px;
    padding: 0 18px;
    border: 0;
    border-radius: 10px;
    background: linear-gradient(135deg, #1a2a6c 0%, #4a6cf7 100%);
    color: #fff;
    font: 600 12.5px "DM Sans", system-ui, sans-serif;
    letter-spacing: .2px;
    cursor: pointer;
    box-shadow: 0 6px 16px rgba(26, 42, 108, .25);
    transition: transform .15s ease, box-shadow .15s ease, filter .15s ease;
}

.button.primary:hover {
    transform: translateY(-1px);
    box-shadow: 0 10px 22px rgba(26, 42, 108, .30);
    filter: brightness(1.05);
}

.button.primary:active {
    transform: translateY(0);
    box-shadow: 0 4px 10px rgba(26, 42, 108, .25);
}

.button.primary .button-plus {
    font-size: 16px;
    line-height: 1;
    color: #f0c75e;
}

@media (max-width: 576px) {
    .welcome-row {
        align-items: flex-start;
    }

    .welcome-row__right {
        width: 100%;
        justify-content: space-between;
    }

    .welcome-row h1 {
        font-size: 22px;
        letter-spacing: -0.6px;
    }

    .wave {
        font-size: 17px;
        vertical-align: 1px;
    }

    .title-divider {
        width: 180px;
    }

    .button.primary {
        flex: 1;
        justify-content: center;
    }
}

.dash__section {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.dash__grid-2 {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1.5rem;
    align-items: stretch;
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
    .dash__grid-2 {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 576px) {
    .dash {
        padding: 1rem;
    }
}
</style>

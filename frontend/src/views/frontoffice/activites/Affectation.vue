<script setup lang="ts">
import { useToastMessage } from '@/composables/useToast'
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { BaseButton, BaseInput, BaseSelect } from '@/components/base'
import { listerActivitesNonAffectees } from '@/services/affectation'
import type { ActiviteListItem, ActiviteOption } from '@/types/activite'
import * as activiteService from '@/services/activite'
import ModalAffecter from '@/components/ModalAffecter.vue'
import ModalAffecterMultiple from '@/components/ModalAffecterMultiple.vue'

const router = useRouter()

const activites = ref<ActiviteListItem[]>([])
const loading = ref(false)
const errorMessage = useToastMessage('error')
const successMessage = useToastMessage('success')

const page = ref(0)
const size = ref(20)
const totalElements = ref(0)
const totalPages = ref(0)

const vue = ref<'PTA' | 'NON_PTA'>('PTA')
const services = ref<ActiviteOption[]>([])
const serviceId = ref<number | undefined>(undefined)

const filtreServiceDisponible = computed(() => services.value.length > 0)
const servicesOptions = computed(() =>
    services.value.map(service => ({
        id: service.id,
        libelle: service.libelle ?? service.code ?? '',
    }))
)

const filtres = ref({
    search: '',
    annee: undefined as number | undefined,
})

/* ==========================================================================
   SÉLECTION MULTIPLE
   ========================================================================== */
const selection = ref<Set<number>>(new Set())

/** Les activités actuellement visibles dont l'ID est dans la sélection. */
const selectionVisibles = computed(() =>
    activites.value.filter(a => selection.value.has(a.id))
)

const nombreSelectionnes = computed(() => selectionVisibles.value.length)
const nombreTotalVisibles = computed(() => activites.value.length)

const tousSelectionnes = computed(() =>
    nombreTotalVisibles.value > 0 && nombreSelectionnes.value === nombreTotalVisibles.value
)

const partiellementSelectionnes = computed(() =>
    nombreSelectionnes.value > 0 && !tousSelectionnes.value
)

function estSelectionnee(id: number): boolean {
    return selection.value.has(id)
}

function basculerSelection(id: number): void {
    const next = new Set(selection.value)
    if (next.has(id)) {
        next.delete(id)
    } else {
        next.add(id)
    }
    selection.value = next
}

function toutSelectionner(): void {
    selection.value = new Set(activites.value.map(a => a.id))
}

function toutDeselectionner(): void {
    selection.value = new Set()
}

function basculerSelectionGlobale(): void {
    if (tousSelectionnes.value) {
        toutDeselectionner()
    } else {
        toutSelectionner()
    }
}

/* ==========================================================================
   MODALES
   ========================================================================== */
const modalOpen = ref(false)
const activiteCible = ref<ActiviteListItem | null>(null)

const modalMultipleOpen = ref(false)

const dateDuJour = computed(() =>
    new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    }).format(new Date()).toUpperCase()
)

const anneeOptions = computed(() => {
    const current = new Date().getFullYear()
    return [
        { value: '', label: 'Toutes les années' },
        { value: String(current), label: String(current) },
        { value: String(current - 1), label: String(current - 1) },
        { value: String(current + 1), label: String(current + 1) },
    ]
})

const anneeSelectionnee = computed({
    get: () => filtres.value.annee ? String(filtres.value.annee) : '',
    set: (value: string | number) => {
        filtres.value.annee = value ? Number(value) : undefined
    },
})

/* ==========================================================================
   CHARGEMENT
   ========================================================================== */

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''

    const res = await listerActivitesNonAffectees({
        search: filtres.value.search || undefined,
        annee: filtres.value.annee,
        pta: vue.value === 'PTA',
        serviceId: filtreServiceDisponible.value ? serviceId.value ?? undefined : undefined,
        page: page.value,
        size: size.value,
    })

    loading.value = false

    if (!res.success) {
        errorMessage.value = res.error
        return
    }

    activites.value = res.data.content
    totalElements.value = res.data.totalElements
    totalPages.value = res.data.totalPages

    // Nettoyer la sélection : retirer les IDs qui ne sont plus visibles
    const idsVisibles = new Set(res.data.content.map(a => a.id))
    selection.value = new Set(
        [...selection.value].filter(id => idsVisibles.has(id))
    )
}

async function chargerOptions(): Promise<void> {
    const res = await activiteService.chargerOptions()
    if (res.success) {
        services.value = res.data.services
    }
}

function changerVue(nouvelleVue: 'PTA' | 'NON_PTA'): void {
    if (vue.value === nouvelleVue) return
    vue.value = nouvelleVue
    page.value = 0
    selection.value = new Set()
    void charger()
}

/* ==========================================================================
   ACTIONS
   ========================================================================== */

function ouvrirModale(a: ActiviteListItem): void {
    activiteCible.value = a
    modalOpen.value = true
}

function ouvrirModaleMultiple(): void {
    if (nombreSelectionnes.value === 0) return
    modalMultipleOpen.value = true
}

function surAffectation(): void {
    successMessage.value = `Utilisateur affecté à l'activité ${activiteCible.value?.code}.`
    modalOpen.value = false
    void charger()
    setTimeout(() => (successMessage.value = ''), 4000)
}

function surAffectationMultiple(resume: { succes: number; echecs: number; total: number }): void {
    if (resume.echecs === 0) {
        successMessage.value = `${resume.succes} activité${resume.succes > 1 ? 's' : ''} affectée${resume.succes > 1 ? 's' : ''} avec succès.`
    } else if (resume.succes === 0) {
        errorMessage.value = `Aucune activité n'a pu être affectée sur ${resume.total}.`
    } else {
        successMessage.value = `${resume.succes} / ${resume.total} affectée${resume.succes > 1 ? 's' : ''} — ${resume.echecs} échec${resume.echecs > 1 ? 's' : ''}.`
    }
    modalMultipleOpen.value = false
    toutDeselectionner()
    void charger()
    setTimeout(() => {
        successMessage.value = ''
        errorMessage.value = ''
    }, 6000)
}

function voirDetail(a: ActiviteListItem): void {
    router.push({ name: 'activite-detail', params: { id: a.id } })
}

function reinitialiser(): void {
    filtres.value = { search: '', annee: undefined }
    serviceId.value = undefined
    page.value = 0
    selection.value = new Set()
    void charger()
}

function changerPage(n: number): void {
    if (n < 0 || n >= totalPages.value) return
    page.value = n
    selection.value = new Set()
    void charger()
}

/* ==========================================================================
   HELPERS
   ========================================================================== */

function formaterDate(iso: string | null | undefined): string {
    if (!iso) return '—'
    const d = new Date(iso)
    return Number.isNaN(d.getTime()) ? iso : d.toLocaleDateString('fr-FR')
}

const debutAffichage = computed(() =>
    totalElements.value === 0 ? 0 : page.value * size.value + 1)
const finAffichage = computed(() =>
    Math.min((page.value + 1) * size.value, totalElements.value))

const nombreFiltresActifs = computed(() => {
    let n = 0
    if (filtres.value.search) n++
    if (filtres.value.annee) n++
    if (serviceId.value) n++
    return n
})

watch([filtres, serviceId], () => {
    page.value = 0
    selection.value = new Set()
    void charger()
}, { deep: true })

onMounted(async () => {
    await Promise.all([chargerOptions(), charger()])
})
</script>

<template>
    <div class="affectation-page">

        <!-- ============ HERO ============ -->
        <header class="hero">
            <div class="hero__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>
                    Activités à affecter
                    <span class="wave" aria-hidden="true">✳</span>
                </h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    Attribuez un responsable aux activités non commencées de votre périmètre.
                </p>
            </div>

            <div class="hero__kpi">
                <div class="kpi-tile">
                    <span class="kpi-tile__icon">
                        <i class="bi bi-inbox"></i>
                    </span>
                    <div class="kpi-tile__body">
                        <span class="kpi-tile__value">{{ totalElements }}</span>
                        <span class="kpi-tile__label">à affecter</span>
                    </div>
                </div>
            </div>
        </header>

        <!-- ============ MESSAGES ============ -->
        <!-- ============ VUE SWITCH ============ -->
        <div class="vue-switch" role="tablist" aria-label="Type d'activité">
            <button type="button" role="tab" :aria-selected="vue === 'PTA'"
                :class="{ 'vue-switch__btn--active': vue === 'PTA' }" class="vue-switch__btn"
                @click="changerVue('PTA')">
                <i class="bi bi-clipboard-check"></i>
                Activités PTA
            </button>
            <button type="button" role="tab" :aria-selected="vue === 'NON_PTA'"
                :class="{ 'vue-switch__btn--active': vue === 'NON_PTA' }" class="vue-switch__btn"
                @click="changerVue('NON_PTA')">
                <i class="bi bi-clipboard"></i>
                Activités courantes
            </button>
        </div>

        <!-- ============ FILTRES ============ -->
        <section class="filters">
            <div class="filters__group filters__group--grow">
                <BaseInput v-model="filtres.search" placeholder="Rechercher par code, référence ou désignation…"
                    icon="bi bi-search" />
            </div>

            <div class="filters__group">
                <BaseSelect v-model="anneeSelectionnee" :options="anneeOptions" placeholder="Année" />
            </div>

            <div v-if="filtreServiceDisponible" class="filters__group">
                <BaseSelect v-model="serviceId" :options="servicesOptions" option-label="libelle" option-value="id"
                    placeholder="Tous les services" />
            </div>

            <div class="filters__actions">
                <button type="button" class="filters__reset" :disabled="nombreFiltresActifs === 0"
                    @click="reinitialiser">
                    <i class="bi bi-arrow-counterclockwise"></i>
                    Réinitialiser
                </button>
            </div>
        </section>

        <!-- ============ CONTENU ============ -->
        <section class="content">

            <!-- Chargement -->
            <div v-if="loading" class="skeleton-wrap">
                <div class="skeleton skeleton-header"></div>
                <div v-for="i in 5" :key="i" class="skeleton skeleton-row"></div>
            </div>

            <!-- Vide -->
            <div v-else-if="activites.length === 0 && !errorMessage" class="empty-state">
                <div class="empty-state__icon">
                    <i class="bi bi-check2-all"></i>
                </div>
                <h2 class="empty-state__title">Tout est affecté</h2>
                <p class="empty-state__text">
                    Aucune activité <strong>{{ vue === 'PTA' ? 'PTA' : 'courante' }}</strong>
                    non commencée sans responsable dans votre périmètre.
                </p>
                <button type="button" class="empty-state__cta" @click="changerVue(vue === 'PTA' ? 'NON_PTA' : 'PTA')">
                    <i class="bi bi-arrow-left-right"></i>
                    Voir les activités {{ vue === 'PTA' ? 'courantes' : 'PTA' }}
                </button>
            </div>

            <!-- Tableau -->
            <div v-else class="table-card">

                <!-- Barre de sélection -->
                <div class="table-toolbar">
                    <div class="table-toolbar__info">
                        <i class="bi" :class="nombreSelectionnes > 0 ? 'bi-check2-square' : 'bi-square'"></i>
                        <span v-if="nombreSelectionnes === 0">
                            Aucune activité sélectionnée
                        </span>
                        <span v-else>
                            <strong>{{ nombreSelectionnes }}</strong>
                            activité{{ nombreSelectionnes > 1 ? 's' : '' }} sélectionnée{{ nombreSelectionnes > 1 ? 's'
                            : '' }}
                        </span>
                    </div>

                    <button type="button" class="table-toolbar__toggle" @click="basculerSelectionGlobale">
                        <i class="bi" :class="tousSelectionnes ? 'bi-dash-square' : 'bi-check2-all'"></i>
                        {{ tousSelectionnes ? 'Tout désélectionner' : 'Tout sélectionner' }}
                    </button>
                </div>

                <table class="aa-table">
                    <thead>
                        <tr>
                            <th class="col-check">
                                <span class="checkbox-wrapper">
                                    <input type="checkbox" :checked="tousSelectionnes"
                                        :indeterminate.prop="partiellementSelectionnes"
                                        :disabled="activites.length === 0" @change="basculerSelectionGlobale" />
                                </span>
                            </th>
                            <th class="col-code">Code / Réf.</th>
                            <th class="col-design">Désignation</th>
                            <th class="col-periode">Période</th>
                            <th class="col-actions"></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="a in activites" :key="a.id" class="aa-row"
                            :class="{ 'aa-row--selected': estSelectionnee(a.id) }">
                            <td class="col-check">
                                <span class="checkbox-wrapper">
                                    <input type="checkbox" :checked="estSelectionnee(a.id)"
                                        @change="basculerSelection(a.id)" />
                                </span>
                            </td>

                            <td class="col-code">
                                <span class="cell-code">{{ a.code }}</span>
                                <span v-if="a.reference" class="cell-reference">{{ a.reference }}</span>
                            </td>

                            <td class="col-design">
                                <span class="cell-design" @click="voirDetail(a)">{{ a.designation }}</span>
                                <span v-if="a.objectifSpecifique" class="cell-objectif">
                                    <i class="bi bi-bullseye"></i>
                                    {{ a.objectifSpecifique.code }}
                                </span>
                            </td>

                            <td class="col-periode">
                                <span class="cell-periode">
                                    <span class="cell-periode__date">{{ formaterDate(a.dateDebutPrevue) }}</span>
                                    <span class="cell-periode__arrow">→</span>
                                    <span class="cell-periode__date">{{ formaterDate(a.dateFinPrevue) }}</span>
                                </span>
                            </td>

                            <td class="col-actions">
                                <button type="button" class="btn-affecter" @click="ouvrirModale(a)">
                                    <i class="bi bi-person-plus"></i>
                                    <span>Affecter</span>
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- Pagination -->
            <footer v-if="!loading && totalPages > 1" class="pagination">
                <span class="pagination__info">
                    <strong>{{ debutAffichage }}–{{ finAffichage }}</strong>
                    sur <strong>{{ totalElements }}</strong>
                    activité{{ totalElements > 1 ? 's' : '' }}
                </span>
                <div class="pagination__controls">
                    <button class="page-btn" :disabled="page === 0" @click="changerPage(page - 1)">
                        <i class="bi bi-chevron-left"></i>
                    </button>
                    <span class="page-current">{{ page + 1 }} / {{ totalPages }}</span>
                    <button class="page-btn" :disabled="page >= totalPages - 1" @click="changerPage(page + 1)">
                        <i class="bi bi-chevron-right"></i>
                    </button>
                </div>
            </footer>
        </section>

        <!-- ============ BARRE FLOTTANTE ============ -->
        <Transition name="slide-up">
            <div v-if="nombreSelectionnes > 0" class="floating-bar">
                <div class="floating-bar__left">
                    <span class="floating-bar__count">{{ nombreSelectionnes }}</span>
                    <span class="floating-bar__label">
                        activité{{ nombreSelectionnes > 1 ? 's' : '' }} sélectionnée{{ nombreSelectionnes > 1 ? 's' : ''
                        }}
                    </span>
                </div>
                <div class="floating-bar__actions">
                    <button type="button" class="floating-bar__btn floating-bar__btn--ghost"
                        @click="toutDeselectionner">
                        <i class="bi bi-x-lg"></i>
                        Annuler
                    </button>
                    <button type="button" class="floating-bar__btn floating-bar__btn--primary"
                        @click="ouvrirModaleMultiple">
                        <i class="bi bi-people-fill"></i>
                        Affecter les {{ nombreSelectionnes }}
                    </button>
                </div>
            </div>
        </Transition>
    </div>

    <!-- Modal individuelle -->
    <ModalAffecter v-if="activiteCible" v-model="modalOpen" :id-activite="activiteCible.id"
        :code-activite="activiteCible.code" :designation-activite="activiteCible.designation"
        @affecte="surAffectation" />

    <!-- Modal multi-activités -->
    <ModalAffecterMultiple v-if="modalMultipleOpen" v-model="modalMultipleOpen" :activites="selectionVisibles"
        @affecte="surAffectationMultiple" />
</template>

<style scoped>
.affectation-page {
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

/* HERO */
.hero {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 2rem;
    flex-wrap: wrap;
    margin-bottom: 0.25rem;
}

.hero__left {
    min-width: 0;
    flex: 1;
}

.hero .eyebrow {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.7rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: #8a95a8;
}

.hero .live-dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #55d6ad;
    box-shadow: 0 0 0 3px rgba(85, 214, 173, .22);
    animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {

    0%,
    100% {
        opacity: 1;
        transform: scale(1);
    }

    50% {
        opacity: .7;
        transform: scale(.9);
    }
}

.hero h1 {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    margin: 0.5rem 0 0.35rem;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 1.75rem;
    font-weight: 800;
    color: #1a2a6c;
    letter-spacing: -0.5px;
    line-height: 1.15;
}

.hero .wave {
    font-size: 1.3rem;
    color: #d4af37;
    animation: waveAnim 3s ease-in-out infinite;
}

@keyframes waveAnim {

    0%,
    100% {
        transform: rotate(0);
    }

    50% {
        transform: rotate(15deg);
    }
}

.hero .title-divider {
    display: block;
    width: 48px;
    height: 3px;
    background: linear-gradient(90deg, #d4af37 0%, rgba(212, 175, 55, 0) 100%);
    border-radius: 3px;
    margin: 0.7rem 0;
}

.hero .subheading {
    margin: 0;
    font-size: 0.88rem;
    color: #6b7280;
    max-width: 580px;
    line-height: 1.5;
}

.hero__kpi {
    flex: none;
}

.kpi-tile {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 1rem 1.5rem;
    background: linear-gradient(135deg, #1a2a6c 0%, #2a3f94 100%);
    border-radius: 16px;
    box-shadow: 0 8px 24px rgba(26, 42, 108, .22);
    color: #fff;
    min-width: 190px;
}

.kpi-tile__icon {
    display: grid;
    place-items: center;
    width: 44px;
    height: 44px;
    border-radius: 12px;
    background: rgba(255, 255, 255, .16);
    color: #ffd04a;
    font-size: 1.25rem;
    flex: none;
}

.kpi-tile__body {
    display: flex;
    flex-direction: column;
    gap: 0.1rem;
    min-width: 0;
}

.kpi-tile__value {
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 1.75rem;
    font-weight: 800;
    line-height: 1;
    font-variant-numeric: tabular-nums;
}

.kpi-tile__label {
    font-size: 0.72rem;
    font-weight: 600;
    color: rgba(255, 255, 255, .78);
    text-transform: uppercase;
    letter-spacing: 0.06em;
}

/* VUE SWITCH */
.vue-switch {
    display: inline-flex;
    gap: 6px;
    padding: 5px;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 12px;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04);
    width: fit-content;
}

.vue-switch__btn {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.55rem 1.15rem;
    border: none;
    border-radius: 8px;
    background: transparent;
    color: #6b7280;
    font-size: 0.85rem;
    font-weight: 600;
    cursor: pointer;
    transition: background .15s ease, color .15s ease, box-shadow .15s ease;
}

.vue-switch__btn i {
    font-size: 1rem;
    color: #8a95a8;
    transition: color .15s ease;
}

.vue-switch__btn:hover:not(.vue-switch__btn--active) {
    background: #f7f9fd;
    color: #1a2a6c;
}

.vue-switch__btn:hover:not(.vue-switch__btn--active) i {
    color: #4a6cf7;
}

.vue-switch__btn--active {
    background: linear-gradient(135deg, #1a2a6c 0%, #2a3f94 100%);
    color: #fff;
    box-shadow: 0 4px 12px rgba(26, 42, 108, .25);
}

.vue-switch__btn--active i {
    color: #ffd04a;
}

/* FILTRES */
.filters {
    display: flex;
    flex-wrap: wrap;
    gap: 0.75rem;
    align-items: center;
    padding: 1rem 1.25rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 14px;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04);
}

.filters__group {
    min-width: 0;
}

.filters__group--grow {
    flex: 1 1 260px;
}

.filters__group:not(.filters__group--grow) {
    flex: 0 1 200px;
}

.filters__actions {
    margin-left: auto;
}

.filters__reset {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.5rem 0.9rem;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    background: #fff;
    color: #6b7280;
    font-size: 0.8rem;
    font-weight: 600;
    cursor: pointer;
    transition: background .15s, border-color .15s, color .15s;
}

.filters__reset:hover:not(:disabled) {
    background: #f7f9fd;
    border-color: #4a6cf7;
    color: #1a2a6c;
}

.filters__reset:disabled {
    opacity: .4;
    cursor: not-allowed;
}

/* CONTENU */
.content {
    display: flex;
    flex-direction: column;
    gap: 0.85rem;
}

/* Empty */
.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0.85rem;
    padding: 4.5rem 2rem;
    background: #fff;
    border: 1px dashed #dfe5ec;
    border-radius: 16px;
    text-align: center;
}

.empty-state__icon {
    display: grid;
    place-items: center;
    width: 72px;
    height: 72px;
    border-radius: 50%;
    background: linear-gradient(135deg, #e4f7f0 0%, #d1f0e2 100%);
    color: #138a68;
    font-size: 2.2rem;
    margin-bottom: 0.5rem;
}

.empty-state__title {
    margin: 0;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 1.2rem;
    font-weight: 800;
    color: #1a2a6c;
}

.empty-state__text {
    margin: 0;
    font-size: 0.88rem;
    color: #6b7280;
    max-width: 480px;
    line-height: 1.5;
}

.empty-state__text strong {
    color: #1a2a6c;
}

.empty-state__cta {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    margin-top: 0.5rem;
    padding: 0.55rem 1rem;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    background: #fff;
    color: #1a2a6c;
    font-size: 0.82rem;
    font-weight: 600;
    cursor: pointer;
    transition: background .15s, border-color .15s;
}

.empty-state__cta:hover {
    background: #f3f6fc;
    border-color: #4a6cf7;
}

/* Table card + toolbar */
.table-card {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 14px;
    overflow: hidden;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04), 0 8px 24px rgba(26, 42, 108, .06);
}

.table-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.75rem;
    padding: 0.7rem 1rem;
    background: #f8fafc;
    border-bottom: 1px solid #e6ebf3;
    flex-wrap: wrap;
}

.table-toolbar__info {
    display: inline-flex;
    align-items: center;
    gap: 0.45rem;
    font-size: 0.8rem;
    color: #6b7280;
}

.table-toolbar__info i {
    color: #8a95a8;
}

.table-toolbar__info strong {
    color: #1a2a6c;
    font-weight: 700;
}

.table-toolbar__toggle {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.35rem 0.7rem;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    background: #fff;
    color: #1a2a6c;
    font-size: 0.78rem;
    font-weight: 600;
    cursor: pointer;
    transition: background .15s, border-color .15s;
}

.table-toolbar__toggle:hover {
    background: #eaf0ff;
    border-color: #4a6cf7;
}

/* Tableau */
.aa-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.85rem;
}

.aa-table thead th {
    padding: 0.75rem 1rem;
    text-align: left;
    font-size: 0.68rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    color: #8a95a8;
    background: #f8fafc;
    border-bottom: 1px solid #e6ebf3;
    white-space: nowrap;
}

.aa-table tbody td {
    padding: 0.9rem 1rem;
    border-bottom: 1px solid #f0f3f7;
    vertical-align: middle;
    color: #15233c;
}

.aa-row {
    transition: background .12s;
}

.aa-row:hover {
    background: #f8fafc;
}

.aa-row--selected {
    background: #eef5ff !important;
}

.aa-row--selected:hover {
    background: #e3efff !important;
}

.aa-row:last-child td {
    border-bottom: none;
}

/* Checkbox */
.col-check {
    width: 48px;
    text-align: center;
    padding-left: 1rem;
    padding-right: 0.5rem;
}

.checkbox-wrapper {
    display: inline-flex;
    align-items: center;
    justify-content: center;
}

.checkbox-wrapper input[type="checkbox"] {
    width: 18px;
    height: 18px;
    cursor: pointer;
    accent-color: #4a6cf7;
    margin: 0;
}

.checkbox-wrapper input[type="checkbox"]:disabled {
    cursor: not-allowed;
    opacity: .5;
}

/* Colonnes */
.col-code {
    width: 13%;
}

.col-design {
    width: 47%;
}

.col-periode {
    width: 20%;
}

.col-actions {
    width: 20%;
    text-align: right;
}

/* Cellules */
.cell-code {
    display: block;
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.78rem;
    font-weight: 700;
    color: #1a2a6c;
}

.cell-reference {
    display: block;
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.72rem;
    color: #8a95a8;
    margin-top: 2px;
}

.cell-design {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    font-weight: 600;
    color: #1a2a6c;
    line-height: 1.35;
    cursor: pointer;
    transition: color .15s;
}

.cell-design:hover {
    color: #4a6cf7;
    text-decoration: underline;
}

.cell-objectif {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
    margin-top: 4px;
    font-size: 0.72rem;
    font-weight: 600;
    color: #3b63d9;
}

.cell-objectif i {
    font-size: 0.68rem;
    opacity: .75;
}

.cell-periode {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    font-size: 0.78rem;
    color: #6b7280;
    white-space: nowrap;
}

.cell-periode__date {
    font-variant-numeric: tabular-nums;
}

.cell-periode__arrow {
    color: #cbd5e1;
    font-size: 0.72rem;
}

.btn-affecter {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.45rem 0.9rem;
    border: 1px solid #2a3f94;
    border-radius: 8px;
    background: #2a3f94;
    color: #fff;
    font-size: 0.78rem;
    font-weight: 600;
    cursor: pointer;
    transition: background .15s, transform .1s;
    box-shadow: 0 1px 3px rgba(74, 108, 247, .22);
}

.btn-affecter:hover {
    background: #3a5ce6;
    border-color: #3a5ce6;
    transform: translateY(-1px);
}

.btn-affecter i {
    font-size: 0.85rem;
}

/* PAGINATION */
.pagination {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;
    flex-wrap: wrap;
    padding: 0.4rem 0.25rem;
}

.pagination__info {
    font-size: 0.8rem;
    color: #6b7280;
}

.pagination__info strong {
    color: #1a2a6c;
    font-weight: 700;
}

.pagination__controls {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
}

.page-btn {
    min-width: 34px;
    height: 34px;
    display: grid;
    place-items: center;
    border: 1px solid #e6ebf3;
    background: #fff;
    color: #15233c;
    border-radius: 8px;
    cursor: pointer;
    transition: background .15s, border-color .15s;
}

.page-btn:hover:not(:disabled) {
    background: #f8fafc;
    border-color: #4a6cf7;
    color: #4a6cf7;
}

.page-btn:disabled {
    opacity: .4;
    cursor: not-allowed;
}

.page-current {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.82rem;
    color: #1a2a6c;
    font-weight: 700;
    padding: 0 0.5rem;
}

/* SKELETON */
.skeleton-wrap {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.skeleton {
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: shimmer 1.4s infinite;
    border-radius: 10px;
}

.skeleton-header {
    height: 44px;
    border-radius: 10px 10px 0 0;
}

.skeleton-row {
    height: 64px;
}

@keyframes shimmer {
    from {
        background-position: 200% 0;
    }

    to {
        background-position: -200% 0;
    }
}

/* BARRE FLOTTANTE */
.floating-bar {
    position: fixed;
    left: 50%;
    bottom: 24px;
    transform: translateX(-50%);
    z-index: 40;
    display: flex;
    align-items: center;
    gap: 1.25rem;
    padding: 0.75rem 1rem 0.75rem 1.25rem;
    background: linear-gradient(135deg, #1a2a6c 0%, #2a3f94 100%);
    border-radius: 14px;
    box-shadow: 0 12px 32px rgba(26, 42, 108, .35), 0 2px 6px rgba(0, 0, 0, .1);
    color: #fff;
    min-width: 380px;
    max-width: calc(100vw - 32px);
    flex-wrap: wrap;
}

.floating-bar__left {
    display: inline-flex;
    align-items: center;
    gap: 0.65rem;
    flex: 1;
    min-width: 0;
}

.floating-bar__count {
    display: grid;
    place-items: center;
    min-width: 32px;
    height: 32px;
    padding: 0 0.5rem;
    border-radius: 8px;
    background: rgba(255, 255, 255, .16);
    color: #ffd04a;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 1rem;
    font-weight: 800;
    font-variant-numeric: tabular-nums;
}

.floating-bar__label {
    font-size: 0.85rem;
    font-weight: 600;
    color: rgba(255, 255, 255, .92);
    white-space: nowrap;
}

.floating-bar__actions {
    display: inline-flex;
    gap: 0.5rem;
}

.floating-bar__btn {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.5rem 0.9rem;
    border-radius: 8px;
    font-size: 0.82rem;
    font-weight: 600;
    cursor: pointer;
    transition: background .15s, transform .1s;
    border: 1px solid transparent;
    font-family: inherit;
}

.floating-bar__btn--ghost {
    background: rgba(255, 255, 255, .12);
    color: #fff;
    border-color: rgba(255, 255, 255, .2);
}

.floating-bar__btn--ghost:hover {
    background: rgba(255, 255, 255, .2);
}

.floating-bar__btn--primary {
    background: #ffd04a;
    color: #1a2a6c;
    box-shadow: 0 2px 6px rgba(0, 0, 0, .15);
}

.floating-bar__btn--primary:hover {
    background: #ffdf6f;
    transform: translateY(-1px);
}

/* Transition barre flottante */
.slide-up-enter-active,
.slide-up-leave-active {
    transition: all .25s cubic-bezier(.2, .9, .3, 1.1);
}

.slide-up-enter-from,
.slide-up-leave-to {
    opacity: 0;
    transform: translate(-50%, 20px);
}

/* UTILITAIRES */
.text-muted {
    color: #aeb8c6;
    font-size: 0.82rem;
}

/* RESPONSIVE */
@media (max-width: 1199.98px) {
    .affectation-page {
        padding-left: 24px;
        padding-right: 24px;
    }
}

@media (max-width: 991.98px) {
    .hero h1 {
        font-size: 1.5rem;
    }

    .kpi-tile {
        min-width: unset;
        padding: 0.85rem 1.15rem;
    }

    .kpi-tile__value {
        font-size: 1.4rem;
    }

    .col-periode {
        display: none;
    }

    .col-code {
        width: 20%;
    }

    .col-design {
        width: 55%;
    }

    .col-actions {
        width: 25%;
    }
}

@media (max-width: 767.98px) {
    .affectation-page {
        padding: 24px 16px 20px;
    }

    .hero {
        flex-direction: column;
        align-items: stretch;
    }

    .hero__kpi {
        align-self: flex-start;
    }

    .vue-switch {
        width: 100%;
    }

    .vue-switch__btn {
        flex: 1;
        justify-content: center;
        padding: 0.55rem 0.65rem;
        font-size: 0.8rem;
    }

    .filters {
        flex-direction: column;
        align-items: stretch;
    }

    .filters__group,
    .filters__group:not(.filters__group--grow) {
        width: 100%;
        flex: 1 1 auto;
    }

    .filters__actions {
        margin-left: 0;
    }

    .filters__reset {
        width: 100%;
        justify-content: center;
    }

    .table-toolbar {
        flex-direction: column;
        align-items: stretch;
    }

    .table-toolbar__toggle {
        justify-content: center;
    }

    .aa-table thead {
        display: none;
    }

    .aa-table tbody tr {
        display: block;
        padding: 1rem;
        border-bottom: 1px solid #e6ebf3;
    }

    .aa-table tbody tr:last-child {
        border-bottom: none;
    }

    .aa-table tbody td {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0.35rem 0;
        border-bottom: none;
    }

    .aa-table tbody td::before {
        content: attr(data-label);
        font-size: 0.7rem;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: #8a95a8;
        flex: none;
        margin-right: 1rem;
    }

    .col-check {
        padding-left: 0;
        padding-right: 0;
    }

    .col-check::before {
        content: none;
    }

    .col-actions {
        margin-top: 0.5rem;
        text-align: left;
        justify-content: flex-start;
    }

    .col-actions::before {
        display: none;
    }

    .btn-affecter {
        width: 100%;
        justify-content: center;
    }

    .pagination {
        flex-direction: column;
        align-items: stretch;
    }

    .pagination__controls {
        justify-content: space-between;
    }

    .floating-bar {
        left: 16px;
        right: 16px;
        bottom: 16px;
        transform: none;
        min-width: unset;
        flex-direction: column;
        align-items: stretch;
        gap: 0.75rem;
    }

    .floating-bar__actions {
        width: 100%;
    }

    .floating-bar__btn {
        flex: 1;
        justify-content: center;
    }

    .slide-up-enter-from,
    .slide-up-leave-to {
        opacity: 0;
        transform: translateY(20px);
    }
}

@media (prefers-reduced-motion: reduce) {

    .hero .live-dot,
    .hero .wave,
    .skeleton,
    .btn-affecter,
    .aa-row,
    .floating-bar {
        animation: none !important;
        transition: none !important;
    }
}
</style>
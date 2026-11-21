<script setup lang="ts">
import { useToastMessage } from '@/composables/useToast'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { BaseButton, BaseInput, BaseSelect } from '@/components/base'
import { exporterPlansAction, listerPlans, listerMesBrouillons, listerAValider, listerADemarrer, soumettreValidationPlan, validerPlan } from '@/services/planaction'
import { STATUTS_PLAN, TYPES_ORIGINE } from '@/types/planaction'
import type { PlanActionFiltres, PlanActionListItem } from '@/types/planaction'

defineOptions({ name: 'PlanActionList' })

const router = useRouter()

/* -------------------- état -------------------- */

const plans = ref<PlanActionListItem[]>([])
const loading = ref(false)
const errorMessage = useToastMessage('error')
const successMessage = useToastMessage('success')
const warningMessage = useToastMessage('warning')

const exportEnCours = ref(false)
const exportMenuOuvert = ref(false)

const page = ref(0)
const size = ref(20)
const totalElements = ref(0)
const totalPages = ref(0)

const vue = ref<'tous' | 'brouillons' | 'a-valider' | 'a-demarrer'>('tous')

/* -------------------- filtres -------------------- */

const filtres = ref<PlanActionFiltres>({
    statut: '',
    search: '',
    annee: undefined,
    dateDebut: undefined,
    dateFin: undefined,
    typeOrigine: '',
})

const anneeOptions = computed(() => {
    const current = new Date().getFullYear()
    return [
        { value: '', label: 'Toutes les années' },
        { value: String(current), label: String(current) },
        { value: String(current - 1), label: String(current - 1) },
        { value: String(current + 1), label: String(current + 1) },
    ]
})

/* -------------------- en-tête -------------------- */

const dateDuJour = computed(() =>
    new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    }).format(new Date()).toUpperCase()
)

/* -------------------- chargement -------------------- */

async function charger(): Promise<void> {
    loading.value = true
    errorMessage.value = ''

    const params = {
        ...filtres.value,
        page: page.value,
        size: size.value,
    }

    // Nettoie les valeurs vides pour ne pas envoyer des chaînes vides
    Object.keys(params).forEach(k => {
        if (params[k as keyof typeof params] === '' || params[k as keyof typeof params] === null) {
            delete params[k as keyof typeof params]
        }
    })

    const res = vue.value === 'brouillons'
        ? await listerMesBrouillons(page.value, size.value)
        : vue.value === 'a-valider'
            ? await listerAValider(page.value, size.value)
            : vue.value === 'a-demarrer'
                ? await listerADemarrer(page.value, size.value)
                : await listerPlans(params)

    loading.value = false
    if (!res.success) {
        errorMessage.value = res.error
        return
    }

    plans.value = res.data.content
    totalElements.value = res.data.totalElements
    totalPages.value = res.data.totalPages
}

/* -------------------- actions -------------------- */

function voirDetail(id: number): void {
    router.push({ name: 'plan-action-detail', params: { id } })
}

async function soumettrePlan(id: number, event?: Event): Promise<void> {
    if (event) event.stopPropagation()
    try {
        const res = await soumettreValidationPlan(id)
        if (!res.success) {
            errorMessage.value = res.error
            return
        }
        await charger()
    } catch {
        errorMessage.value = 'Erreur lors de la soumission.'
    }
}

function creer(): void {
    router.push({ name: 'plan-action-create' })
}

function basculerMenuExport(): void {
    if (exportEnCours.value) return
    exportMenuOuvert.value = !exportMenuOuvert.value
}

function onDocumentClick(event: MouseEvent): void {
    const cible = event.target as HTMLElement | null
    if (!cible?.closest('.export-menu')) {
        exportMenuOuvert.value = false
    }
}

function parametresExport(): PlanActionFiltres {
    const filtresExport: PlanActionFiltres = { ...filtres.value }
    if (vue.value === 'brouillons') filtresExport.statut = 'BROUILLON'
    if (vue.value === 'a-valider') filtresExport.statut = 'EN_ATTENTE_VALIDATION'
    if (vue.value === 'a-demarrer') filtresExport.statut = 'NON_COMMENCEE'
    return filtresExport
}

async function lancerExport(format: 'excel' | 'pdf'): Promise<void> {
    exportMenuOuvert.value = false
    if (exportEnCours.value) return
    exportEnCours.value = true

    try {
        const reponse = await exporterPlansAction(parametresExport())
        if (!reponse.success) {
            errorMessage.value = reponse.error
            return
        }

        const donnees = reponse.data
        if (donnees.plans.length === 0) {
            warningMessage.value = 'Aucun plan d’action ne correspond aux filtres sélectionnés.'
            return
        }

        const { exporterExcelPlans, exporterPdfPlans } = await import('@/utils/exportPlansAction')
        if (format === 'excel') {
            exporterExcelPlans(donnees)
            successMessage.value = 'Le fichier Excel a été généré.'
        } else {
            exporterPdfPlans(donnees)
            successMessage.value = 'Le ou les fichiers PDF ont été générés.'
        }
    } catch {
        errorMessage.value = 'Export impossible. Réessayez dans un instant.'
    } finally {
        exportEnCours.value = false
    }
}

async function validerPlanAction(id: number, rejeter: boolean, event?: Event): Promise<void> {
    if (event) event.stopPropagation()
    try {
        const res = await validerPlan(id, rejeter)
        if (!res.success) {
            errorMessage.value = res.error
            return
        }
        await charger()
    } catch {
        errorMessage.value = 'Erreur lors de la validation.'
    }
}




function reinitialiser(): void {
    filtres.value = { statut: '', search: '', annee: undefined, dateDebut: undefined, dateFin: undefined, typeOrigine: '', }
    page.value = 0
    charger()
}

function changerPage(nouvellePage: number): void {
    if (nouvellePage < 0 || nouvellePage >= totalPages.value) return
    page.value = nouvellePage
    charger()
}

/* -------------------- helpers d'affichage -------------------- */

function formaterDate(iso: string | null | undefined): string {
    if (!iso) return '—'
    const d = new Date(iso)
    return Number.isNaN(d.getTime()) ? iso : d.toLocaleDateString('fr-FR')
}

function classeStatut(statutCode: string | null): string {
    if (statutCode === 'NON_COMMENCEE') return 'badge-statut--non-commence'
    if (statutCode === 'EN_COURS') return 'badge-statut--en-cours'
    if (statutCode === 'EN_RETARD') return 'badge-statut--en-retard'
    if (statutCode === 'TERMINEE') return 'badge-statut--termine'
    if (statutCode === 'BROUILLON') return 'badge-statut--brouillon'
    if (statutCode === 'EN_ATTENTE_VALIDATION') return 'badge-statut--brouillon'
    if (statutCode === 'VALIDEE') return 'badge-statut--termine'
    return ''
}

function estEnRetard(p: PlanActionListItem): boolean {
    if (!p.dateFinPrevue) return false
    if (p.statut?.code === 'TERMINEE' || p.statut?.code === 'ANNULEE') return false
    return new Date(p.dateFinPrevue) < new Date()
}

const debutAffichage = computed(() => totalElements.value === 0 ? 0 : page.value * size.value + 1)
const finAffichage = computed(() => Math.min((page.value + 1) * size.value, totalElements.value))

/* -------------------- watchers -------------------- */

watch(filtres, () => {
    page.value = 0
    charger()
}, { deep: true })

watch(vue, () => {
    page.value = 0
    charger()
})

onMounted(() => {
    charger()
    document.addEventListener('click', onDocumentClick)
})

onBeforeUnmount(() => {
    document.removeEventListener('click', onDocumentClick)
})
</script>

<template>
    <div class="pa-page">
        <!-- EN-TÊTE -->
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>Plans d'action</h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    Suivi et pilotage des plans d'action du périmètre
                </p>
            </div>
            <div class="welcome-row__right">
                <div class="export-menu">
                    <BaseButton variant="secondary" :loading="exportEnCours" @click="basculerMenuExport">
                        <i class="bi bi-download"></i>
                        Exporter
                        <i class="bi bi-chevron-down export-menu__chev"
                            :class="{ 'export-menu__chev--open': exportMenuOuvert }"></i>
                    </BaseButton>

                    <ul v-if="exportMenuOuvert" class="export-menu__liste">
                        <li>
                            <button type="button" class="export-menu__item" @click="lancerExport('excel')">
                                <i class="bi bi-file-earmark-excel"></i>
                                <span><strong>Excel</strong><small>Classeur .xlsx (3 feuilles)</small></span>
                            </button>
                        </li>
                        <li>
                            <button type="button" class="export-menu__item" @click="lancerExport('pdf')">
                                <i class="bi bi-file-earmark-pdf"></i>
                                <span><strong>PDF</strong><small>Rapport .pdf</small></span>
                            </button>
                        </li>
                    </ul>
                </div>

                <BaseButton variant="primary" icon="bi bi-plus-lg" @click="creer">
                    Nouveau plan
                </BaseButton>
            </div>
        </header>

        <!-- ONGLETS -->
        <div class="vue-tabs" role="tablist">
            <button :class="['vue-tab', { 'vue-tab--active': vue === 'tous' }]" role="tab" @click="vue = 'tous'">
                <i class="bi bi-list-check"></i>
                Tous les plans
            </button>
            <button :class="['vue-tab', { 'vue-tab--active': vue === 'brouillons' }]" role="tab"
                @click="vue = 'brouillons'">
                <i class="bi bi-pencil-square"></i>
                Mes brouillons
            </button>
            <button :class="['vue-tab', { 'vue-tab--active': vue === 'a-valider' }]" role="tab"
                @click="vue = 'a-valider'">
                <i class="bi bi-shield-check"></i>
                À valider
            </button>
            <button :class="['vue-tab', { 'vue-tab--active': vue === 'a-demarrer' }]" role="tab"
                @click="vue = 'a-demarrer'">
                <i class="bi bi-play-circle"></i>
                À démarrer
            </button>
        </div>

        <!-- FILTRES -->
        <section v-if="vue === 'tous'" class="filters">
            <BaseInput v-model="filtres.search" placeholder="Rechercher par code ou désignation…" icon="bi bi-search"
                class="filters__search" />
            <BaseSelect v-model="filtres.statut" :options="STATUTS_PLAN" placeholder="Statut" class="filters__select" />
            <BaseSelect v-model="filtres.typeOrigine as any" :options="TYPES_ORIGINE" class="filters__select" />
            <BaseSelect v-model="filtres.annee as any" :options="anneeOptions" placeholder="Année"
                class="filters__select" />
            <BaseButton variant="secondary" icon="bi bi-arrow-clockwise" @click="reinitialiser">
                Réinitialiser
            </BaseButton>
        </section>

        <!-- CHARGEMENT -->
        <div v-if="loading" class="loading-state">
            <div v-for="i in 5" :key="i" class="skeleton skeleton-row"></div>
        </div>

        <!-- VIDE -->
        <section v-else-if="plans.length === 0" class="empty-state">
            <i class="bi bi-clipboard-x"></i>
            <h2>Aucun plan d'action</h2>
            <p v-if="vue === 'brouillons'">
                Vous n'avez aucun brouillon en cours.
            </p>
            <p v-else-if="vue === 'a-valider'">
                Aucun plan en attente de validation.
            </p>
            <p v-else-if="vue === 'a-demarrer'">
                Aucun plan prêt à démarrer.
            </p>
            <p v-else>
                Aucun plan ne correspond aux filtres sélectionnés.
            </p>
            <BaseButton variant="primary" icon="bi bi-plus-lg" @click="creer">
                Créer un plan
            </BaseButton>
        </section>

        <!-- TABLEAU -->
        <section v-else class="panel">
            <table class="plans-table">
                <thead>
                    <tr>
                        <th>Code</th>
                        <th>Désignation</th>
                        <th>Statut</th>
                        <th>Avancement</th>
                        <th>Responsable</th>
                        <th>Échéance</th>
                        <th></th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="plan in plans" :key="plan.id" class="plans-row" @click="voirDetail(plan.id)">
                        <td class="plans-table__code">
                            <code>{{ plan.code }}</code>
                        </td>
                        <td class="plans-table__designation">
                            {{ plan.designation }}
                        </td>
                        <td>
                            <span v-if="plan.statut" :class="['badge-statut', classeStatut(plan.statut.code)]">
                                {{ plan.statut.libelle }}
                            </span>
                            <span v-else class="text-muted">—</span>
                        </td>
                        <td class="plans-table__avancement">
                            <div class="progress">
                                <div class="progress__bar" :style="{ width: plan.avancement + '%' }" />
                            </div>
                            <span class="progress__label">{{ plan.avancement.toFixed(0) }} %</span>
                        </td>
                        <td class="plans-table__responsable">
                            {{ plan.responsable?.libelle ?? '—' }}
                        </td>
                        <td class="plans-table__date">
                            <span :class="{ 'text-danger': estEnRetard(plan) }">
                                {{ formaterDate(plan.dateFinPrevue) }}
                            </span>
                        </td>
                        <td class="plans-table__actions" @click.stop>
                            <button v-if="vue === 'brouillons'" type="button" class="btn-action btn-action--success"
                                @click="soumettrePlan(plan.id, $event)">
                                Soumettre
                            </button>
                            <template v-else-if="vue === 'a-valider'">
                                <button type="button" class="btn-action btn-action--success"
                                    @click="validerPlanAction(plan.id, false, $event)">
                                    Valider
                                </button>
                                <button type="button" class="btn-action btn-action--danger"
                                    @click="validerPlanAction(plan.id, true, $event)">
                                    Rejeter
                                </button>
                            </template>
                            <i v-else class="bi bi-chevron-right"></i>
                        </td>
                    </tr>
                </tbody>
            </table>
        </section>

        <!-- PAGINATION -->
        <footer v-if="totalPages > 1" class="pagination">
            <span class="pagination__info">
                {{ debutAffichage }}–{{ finAffichage }} sur {{ totalElements }} plan{{ totalElements > 1 ? 's' : '' }}
            </span>
            <div class="pagination__controls">
                <BaseButton variant="secondary" size="sm" :disabled="page === 0" @click="changerPage(page - 1)">
                    <i class="bi bi-chevron-left"></i>
                    Précédent
                </BaseButton>
                <BaseButton variant="secondary" size="sm" :disabled="page >= totalPages - 1"
                    @click="changerPage(page + 1)">
                    Suivant
                    <i class="bi bi-chevron-right"></i>
                </BaseButton>
            </div>
        </footer>
    </div>
</template>

<style scoped>
.pa-page {
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

/* Onglets */
.vue-tabs {
    display: flex;
    gap: 0.5rem;
    border-bottom: 1px solid #e6ebf3;
    margin-bottom: 0.5rem;
}

.vue-tab {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.6rem 1rem;
    background: none;
    border: none;
    border-bottom: 2px solid transparent;
    color: #6b7280;
    font-size: 0.86rem;
    font-weight: 600;
    cursor: pointer;
    transition: color .15s, border-color .15s;
}

.vue-tab:hover {
    color: #1a2a6c;
}

.vue-tab--active {
    color: #1a2a6c;
    border-bottom-color: #4a6cf7;
}

/* Filtres */
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

.filters__search {
    flex: 1;
    min-width: 260px;
}

.filters__select {
    min-width: 180px;
}

/* Tableau */
.panel {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 14px;
    overflow: hidden;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04), 0 8px 24px rgba(26, 42, 108, .06);
}

.plans-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.86rem;
}

.plans-table thead th {
    padding: 0.75rem 1.25rem;
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

.btn-action {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
    padding: 0.32rem 0.65rem;
    border-radius: 7px;
    border: 1px solid transparent;
    font-size: 0.75rem;
    font-weight: 600;
    font-family: inherit;
    cursor: pointer;
    transition: background .15s, border-color .15s, color .15s;
    white-space: nowrap;
    background: #edf1fd;
    color: #1a2a6c;
    border-color: #c8d6fb;
    margin: 0 0.15rem;
}
.btn-action:hover { background: #dde4ff; border-color: #4a6cf7; }
.btn-action--success { background: #e4f7f0; color: #138a68; border-color: #b7dfc1; }
.btn-action--success:hover { background: #cfeee1; border-color: #55d6ad; }
.btn-action--danger { background: #fff0f0; color: #d84e5b; border-color: #f6cfcc; }
.btn-action--danger:hover { background: #fdd; border-color: #f59aa0; }

.plans-table tbody td {
    padding: 0.85rem 1.25rem;
    border-bottom: 1px solid #eef1f5;
    color: #15233c;
    vertical-align: middle;
}

.plans-row {
    cursor: pointer;
    transition: background .12s;
}

.plans-row:hover {
    background: #f8fafc;
}

.plans-row:last-child td {
    border-bottom: none;
}

.plans-table__code code {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.8rem;
    font-weight: 600;
    color: #1a2a6c;
    background: #eef1f5;
    padding: 0.15rem 0.5rem;
    border-radius: 4px;
}

.plans-table__designation {
    max-width: 400px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.plans-table__avancement {
    width: 160px;
}

.progress {
    display: inline-block;
    width: 90px;
    height: 6px;
    background: #eef1f5;
    border-radius: 3px;
    overflow: hidden;
    vertical-align: middle;
    margin-right: 0.5rem;
}

.progress__bar {
    height: 100%;
    background: linear-gradient(90deg, #4a6cf7, #6b8dff);
    border-radius: 3px;
    transition: width .25s ease;
}

.progress__label {
    font-size: 0.78rem;
    color: #6b7280;
    font-variant-numeric: tabular-nums;
}

.plans-table__responsable {
    color: #4a5568;
}

.plans-table__date {
    font-variant-numeric: tabular-nums;
}

.plans-table__actions {
    width: 30px;
    color: #c3cad5;
    text-align: right;
}

.plans-row:hover .plans-table__actions {
    color: #4a6cf7;
}

/* Statuts */
.badge-statut {
    display: inline-block;
    font-size: 0.7rem;
    font-weight: 700;
    padding: 0.25rem 0.65rem;
    border-radius: 20px;
    letter-spacing: 0.02em;
}

.badge-statut--non-commence {
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
}

.badge-statut--en-cours {
    background: #e4f0fb;
    color: #135d95;
    border: 1px solid #cde2f6;
}

.badge-statut--en-retard {
    background: #fde9e9;
    color: #b03030;
    border: 1px solid #f4c7c7;
}

.badge-statut--termine {
    background: #e6f4ea;
    color: #1e7e34;
    border: 1px solid #c8e5cf;
}

.badge-statut--brouillon {
    background: #fdf3e2;
    color: #9a6409;
    border: 1px solid #f4e2c2;
}

.text-danger {
    color: #b03030;
    font-weight: 600;
}

.text-muted {
    color: #8a95a8;
}

/* Vide */
.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0.85rem;
    padding: 4rem 2rem;
    background: #fff;
    border: 1px dashed #dfe5ec;
    border-radius: 14px;
    text-align: center;
}

.empty-state i {
    font-size: 2.5rem;
    color: #c3cad5;
}

.empty-state h2 {
    margin: 0;
    font: 700 1rem Manrope, sans-serif;
    color: #1a2a6c;
}

.empty-state p {
    margin: 0;
    color: #6b7280;
    font-size: 0.88rem;
}

/* Skeleton */
.loading-state {
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
}

.skeleton-row {
    height: 58px;
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

/* Pagination */
.pagination {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.5rem 0;
}

.pagination__info {
    font-size: 0.82rem;
    color: #6b7280;
}

.pagination__controls {
    display: flex;
    gap: 0.5rem;
}

/* Responsive */
.export-menu {
    position: relative;
}

.export-menu__chev {
    margin-left: 4px;
    transition: transform 0.15s ease;
}

.export-menu__chev--open {
    transform: rotate(180deg);
}

.export-menu__liste {
    position: absolute;
    top: calc(100% + 8px);
    right: 0;
    z-index: 30;
    width: 250px;
    margin: 0;
    padding: 6px;
    list-style: none;
    background: #fff;
    border: 1px solid #e4e9f2;
    border-radius: 10px;
    box-shadow: 0 12px 32px rgba(15, 35, 70, 0.14);
}

.export-menu__item {
    display: flex;
    align-items: center;
    gap: 10px;
    width: 100%;
    padding: 9px 10px;
    color: #1f2937;
    text-align: left;
    background: transparent;
    border: 0;
    border-radius: 7px;
    cursor: pointer;
}

.export-menu__item:hover {
    background: #f3f6fa;
}

.export-menu__item i {
    font-size: 18px;
    color: #2b6cb0;
}

.export-menu__item span {
    display: flex;
    flex-direction: column;
}

.export-menu__item small {
    color: #7b8798;
    font-size: 11px;
}

@media (max-width: 991.98px) {
    .pa-page {
        padding-left: 24px;
        padding-right: 24px;
    }

    .plans-table__designation {
        max-width: 240px;
    }
}

@media (max-width: 767.98px) {
    .pa-page {
        padding: 24px 16px 20px;
    }

    .filters {
        flex-direction: column;
        align-items: stretch;
    }

    .filters__search,
    .filters__select {
        width: 100%;
        min-width: 0;
    }

    .plans-table thead th:nth-child(5),
    .plans-table tbody td:nth-child(5),
    .plans-table thead th:nth-child(6),
    .plans-table tbody td:nth-child(6) {
        display: none;
    }
}
</style>
<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BaseButton, BaseInput, BaseConfirm } from '@/components/base'
import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'
import { creerPlan } from '@/services/planaction'
import type { PlanActionRequest } from '@/types/planaction'
import OriginesSelect from '@/components/OriginesSelect.vue'
import UtilisateurSelect from '@/components/UtilisateurSelect.vue'

const router = useRouter()

/* -------------------- formulaire -------------------- */

const form = ref<{
    designation: string
    dateDebutPrevue: string
    dateFinPrevue: string
    origineIds: number[]
    responsableId: number | null
}>({
    designation: '',
    dateDebutPrevue: '',
    dateFinPrevue: '',
    origineIds: [],
    responsableId: null,
})

const errors = ref<Record<string, string>>({})
const globalError = ref('')
const submitting = ref(false)

/* -------------------- confirmation -------------------- */

const confirmOpen = ref(false)
const confirmLoading = ref(false)
const confirmDemande = ref<ConfirmDemande | null>(null)

/* -------------------- en-tête -------------------- */

const dateDuJour = computed(() =>
    new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    }).format(new Date()).toUpperCase()
)

/* -------------------- validation -------------------- */

function valider(): boolean {
    errors.value = {}

    if (!form.value.designation.trim()) {
        errors.value.designation = 'La désignation est obligatoire'
    } else if (form.value.designation.trim().length > 255) {
        errors.value.designation = 'Maximum 255 caractères'
    }

    if (!form.value.dateDebutPrevue) {
        errors.value.dateDebutPrevue = 'La date de début est obligatoire'
    }
    if (!form.value.dateFinPrevue) {
        errors.value.dateFinPrevue = 'La date de fin est obligatoire'
    }
    if (form.value.dateDebutPrevue && form.value.dateFinPrevue) {
        if (new Date(form.value.dateFinPrevue) < new Date(form.value.dateDebutPrevue)) {
            errors.value.dateFinPrevue = 'La date de fin doit être postérieure ou égale à la date de début'
        }
    }

    if (form.value.origineIds.length === 0) {
        errors.value.origineIds = 'Au moins une origine est obligatoire'
    }

    if (form.value.responsableId === null) {
        errors.value.responsableId = 'Le responsable est obligatoire'
    }

    return Object.keys(errors.value).length === 0
}

/* -------------------- actions -------------------- */

function demanderConfirmation(): void {
    globalError.value = ''
    if (!valider()) return

    confirmDemande.value = {
        titre: 'Créer le plan d\'action',
        message: `Vous allez créer le plan « ${form.value.designation.trim()} ».`,
        consequence:
            'Le code sera généré automatiquement (PA-année-numéro). '
            + 'Le plan sera créé en statut BROUILLON : il devra être soumis puis validé '
            + 'avant de pouvoir démarrer son exécution.',
        libelleConfirmer: 'Créer',
        libelleAnnuler: 'Revenir au formulaire',
        ton: 'primary',
        icone: 'bi bi-plus-circle',
    }

    confirmOpen.value = true
}

async function confirmerCreation(): Promise<void> {
    globalError.value = ''
    confirmLoading.value = true

    const payload: PlanActionRequest = {
        designation: form.value.designation.trim(),
        dateDebutPrevue: form.value.dateDebutPrevue,
        dateFinPrevue: form.value.dateFinPrevue,
        origineIds: [...form.value.origineIds],
        responsableId: form.value.responsableId!,
    }

    const res = await creerPlan(payload)
    confirmLoading.value = false

    if (!res.success) {
        confirmOpen.value = false
        globalError.value = res.error
        return
    }

    router.push({ name: 'plans-action', query: { created: res.data.code } })
}

function annulerConfirmation(): void {
    confirmOpen.value = false
}

function annuler(): void {
    router.push({ name: 'plans-action' })
}

/* -------------------- récap -------------------- */

const resumeDesignation = computed(() => form.value.designation.trim() || '—')
const resumeDebut = computed(() => form.value.dateDebutPrevue || '—')
const resumeFin = computed(() => form.value.dateFinPrevue || '—')
const resumeNbOrigines = computed(() => form.value.origineIds.length)
const resumeResponsable = computed(() =>
    form.value.responsableId !== null ? 'Sélectionné' : '—'
)

const formulairePret = computed(() =>
    form.value.designation.trim() !== ''
    && form.value.dateDebutPrevue !== ''
    && form.value.dateFinPrevue !== ''
    && form.value.origineIds.length > 0
    && form.value.responsableId !== null
)
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
                <h1>Créer un plan d'action</h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    Plan indépendant, à soumettre pour validation après création
                </p>
            </div>
            <div class="welcome-row__right">
                <BaseButton variant="secondary" :disabled="submitting" @click="annuler">
                    <i class="bi bi-arrow-left"></i>
                    Retour à la liste
                </BaseButton>
            </div>
        </header>

        <!-- ERREUR GLOBALE -->
        <div v-if="globalError" class="alert alert-danger mb-0" role="alert">
            <i class="bi bi-exclamation-triangle-fill"></i>
            {{ globalError }}
        </div>

        <!-- FORMULAIRE -->
        <form class="form-layout" novalidate @submit.prevent="demanderConfirmation">
            <div class="form-main">
                <!-- 01 — Identification -->
                <section class="panel form-section">
                    <div class="section-head">
                        <span class="section-num">01</span>
                        <div>
                            <h2>Identification</h2>
                            <p>Code généré automatiquement (PA-année-numéro).</p>
                        </div>
                    </div>

                    <div class="field-grid">
                        <div class="field full">
                            <BaseInput v-model="form.designation" label="Désignation" required
                                placeholder="Résumé du plan d'action" :error="errors.designation" :disabled="submitting"
                                helper="Maximum 255 caractères." />
                        </div>
                    </div>
                </section>

                <!-- 02 — Dates -->
                <section class="panel form-section">
                    <div class="section-head">
                        <span class="section-num">02</span>
                        <div>
                            <h2>Période prévue</h2>
                            <p>Dates planifiées pour la réalisation du plan.</p>
                        </div>
                    </div>

                    <div class="field-grid">
                        <div class="field">
                            <BaseInput v-model="form.dateDebutPrevue" type="date" label="Date de début prévue" required
                                :error="errors.dateDebutPrevue" :disabled="submitting" />
                        </div>
                        <div class="field">
                            <BaseInput v-model="form.dateFinPrevue" type="date" label="Date de fin prévue" required
                                :error="errors.dateFinPrevue" :disabled="submitting" />
                        </div>
                    </div>
                </section>

                <!-- 03 — Origines -->
                <section class="panel form-section">
                    <div class="section-head">
                        <span class="section-num">03</span>
                        <div>
                            <h2>Origines</h2>
                            <p>Incidents, risques ou autres événements à l'origine du plan.</p>
                        </div>
                    </div>

                    <div class="field-grid">
                        <div class="field full">
                            <OriginesSelect v-model="form.origineIds" :disabled="submitting" />
                            <span v-if="errors.origineIds" class="field-error">
                                {{ errors.origineIds }}
                            </span>
                        </div>
                    </div>
                </section>

                <!-- 04 — Responsable -->
                <section class="panel form-section">
                    <div class="section-head">
                        <span class="section-num">04</span>
                        <div>
                            <h2>Responsable</h2>
                            <p>Utilisateur affecté à la responsabilité du plan.</p>
                        </div>
                    </div>

                    <div class="field-grid">
                        <div class="field full">
                            <UtilisateurSelect v-model="form.responsableId" :disabled="submitting" />
                            <span v-if="errors.responsableId" class="field-error">
                                {{ errors.responsableId }}
                            </span>
                        </div>
                    </div>
                </section>

                <!-- Actions -->
                <div class="form-actions">
                    <span class="form-note">
                        <i class="bi bi-info-circle"></i>
                        Le plan sera créé en statut BROUILLON et devra être soumis pour validation.
                    </span>

                    <div class="form-actions-buttons">
                        <BaseButton variant="secondary" :disabled="submitting" @click="annuler">
                            Annuler
                        </BaseButton>
                        <BaseButton variant="primary" type="submit" :disabled="submitting || !formulairePret">
                            <i class="bi bi-check-lg"></i>
                            Créer le plan
                        </BaseButton>
                    </div>
                </div>
            </div>

            <!-- Aside -->
            <aside class="form-side">
                <section class="panel side-card">
                    <h2 class="side-title">Récapitulatif</h2>
                    <p class="side-sub">Aperçu avant création.</p>

                    <div class="recap">
                        <div class="recap-row">
                            <span>Désignation</span>
                            <strong :class="{ 'recap-empty': !form.designation.trim() }">
                                {{ resumeDesignation }}
                            </strong>
                        </div>
                        <div class="recap-row">
                            <span>Début prévu</span>
                            <strong :class="{ 'recap-empty': !form.dateDebutPrevue }">
                                {{ resumeDebut }}
                            </strong>
                        </div>
                        <div class="recap-row">
                            <span>Fin prévue</span>
                            <strong :class="{ 'recap-empty': !form.dateFinPrevue }">
                                {{ resumeFin }}
                            </strong>
                        </div>
                        <div class="recap-row">
                            <span>Origines</span>
                            <strong :class="{ 'recap-empty': resumeNbOrigines === 0 }">
                                {{ resumeNbOrigines > 0 ? resumeNbOrigines : '—' }}
                            </strong>
                        </div>
                        <div class="recap-row">
                            <span>Responsable</span>
                            <strong :class="{ 'recap-empty': form.responsableId === null }">
                                {{ resumeResponsable }}
                            </strong>
                        </div>
                    </div>
                </section>

                <section class="panel side-card side-card--warning">
                    <div class="warning-icon">
                        <i class="bi bi-info-circle-fill"></i>
                    </div>
                    <div>
                        <h3 class="warning-title">Statut BROUILLON</h3>
                        <p class="warning-text">
                            Le plan n'apparaîtra pas dans la liste principale tant qu'il
                            n'aura pas été soumis puis validé. Retrouvez-le dans
                            <strong>Mes brouillons</strong>.
                        </p>
                    </div>
                </section>
            </aside>
        </form>
    </div>

    <!-- Modal confirmation -->
    <BaseConfirm v-model="confirmOpen" :demande="confirmDemande" :loading="confirmLoading" @confirme="confirmerCreation"
        @annule="annulerConfirmation" />
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

.panel {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 14px;
    box-shadow: 0 1px 2px rgba(26, 42, 108, .04), 0 8px 24px rgba(26, 42, 108, .06);
}

.form-layout {
    display: grid;
    grid-template-columns: minmax(0, 1.9fr) minmax(280px, 1fr);
    align-items: start;
    gap: 20px;
}

.form-main {
    display: grid;
    gap: 20px;
    min-width: 0;
}

.form-side {
    position: sticky;
    top: 88px;
    display: grid;
    gap: 18px;
    align-self: start;
}

.form-section {
    padding: 24px 26px 26px;
}

.section-head {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    margin-bottom: 22px;
}

.section-num {
    flex: none;
    display: grid;
    place-items: center;
    width: 34px;
    height: 34px;
    border-radius: 10px;
    color: #fff;
    font: 800 12px Manrope, sans-serif;
    background: linear-gradient(135deg, #1a2a6c 0%, #4a6cf7 100%);
    box-shadow: 0 6px 14px rgba(26, 42, 108, .22), inset 0 -2px 0 #d4af37;
}

.section-head h2 {
    margin: 0 0 4px;
    font: 800 16px Manrope, sans-serif;
    letter-spacing: -.3px;
    color: #1a2a6c;
}

.section-head p {
    margin: 0;
    font-size: 11px;
    color: #8a95a8;
}

.field-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px;
}

.field {
    display: flex;
    flex-direction: column;
    gap: 7px;
    min-width: 0;
}

.field.full {
    grid-column: 1 / -1;
}

.field-error {
    font-size: 0.75rem;
    color: #d64545;
    margin-top: -0.15rem;
}

/* Actions */
.form-actions {
    position: sticky;
    bottom: 14px;
    z-index: 3;
    grid-column: 1 / -1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
    padding: 14px 20px;
    background: rgba(255, 255, 255, .92);
    border: 1px solid #e6ebf3;
    border-radius: 14px;
    box-shadow: 0 10px 30px rgba(26, 42, 108, .10);
    backdrop-filter: blur(12px);
}

.form-note {
    font-size: 10.5px;
    color: #8a95a8;
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
}

.form-note i {
    color: #4a6cf7;
    font-size: 0.85rem;
}

.form-actions-buttons {
    display: flex;
    gap: 10px;
}

/* Aside */
.side-card {
    padding: 20px 20px 18px;
}

.side-title {
    margin: 0 0 4px;
    font: 800 14px Manrope, sans-serif;
    color: #1a2a6c;
}

.side-sub {
    margin: 0 0 14px;
    font-size: 10.5px;
    color: #8a95a8;
}

.recap {
    display: grid;
}

.recap-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 11px 0;
    font-size: 11.5px;
    color: #6b7280;
    border-bottom: 1px dashed #e6ebf3;
}

.recap-row:last-child {
    border-bottom: 0;
    padding-bottom: 0;
}

.recap-row strong {
    max-width: 62%;
    font-size: 11.5px;
    font-weight: 600;
    color: #1a2a6c;
    text-align: right;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.recap-empty {
    color: #c3cad5;
    font-weight: 400;
}

.side-card--warning {
    display: flex;
    gap: 0.85rem;
    padding: 1.1rem 1.25rem;
    background: linear-gradient(135deg, #eef4ff, #fff);
    border: 1px solid #cde2f6;
    border-left: 3px solid #4a6cf7;
}

.warning-icon {
    flex: none;
    display: grid;
    place-items: center;
    width: 34px;
    height: 34px;
    border-radius: 10px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 1.05rem;
}

.warning-title {
    margin: 0 0 0.35rem;
    font: 800 13px Manrope, sans-serif;
    color: #1a2a6c;
}

.warning-text {
    margin: 0;
    font-size: 11.5px;
    line-height: 1.55;
    color: #6b7280;
}

.warning-text strong {
    color: #1a2a6c;
}

/* Responsive */
@media (max-width: 1100px) {
    .form-layout {
        grid-template-columns: 1fr;
    }

    .form-side {
        position: static;
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 991.98px) {
    .pa-page {
        padding-left: 24px;
        padding-right: 24px;
    }
}

@media (max-width: 767.98px) {
    .pa-page {
        padding: 24px 16px 20px;
    }

    .field-grid {
        grid-template-columns: 1fr;
        gap: 16px;
    }

    .form-side {
        grid-template-columns: 1fr;
    }

    .form-actions {
        flex-direction: column;
        align-items: stretch;
        gap: 10px;
        text-align: center;
    }

    .form-actions-buttons {
        flex-direction: column-reverse;
        gap: 8px;
    }

    .form-actions-buttons :deep(.base-btn),
    .form-actions-buttons :deep(.btn) {
        width: 100%;
    }
}

@media (max-width: 600px) {
    .pa-page {
        padding: 20px 12px 16px;
    }

    .form-section {
        padding: 18px 14px 20px;
    }

    .section-num {
        width: 30px;
        height: 30px;
        font-size: 11px;
        border-radius: 9px;
    }

    .section-head h2 {
        font-size: 15px;
    }

    .side-card {
        padding: 17px 15px 15px;
    }
}
</style>
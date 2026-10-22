<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { BaseInput, BaseSelect, BaseButton, BaseConfirm } from '@/components/base'
import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'
import {
    creerSignalement,
    getProchainCode,
} from '@/services/signalement'
import type { SignalementRequest } from '@/types/backoffice/signalement'

const router = useRouter()

/* -------------------- formulaire -------------------- */

const form = ref<SignalementRequest>({
    designation: '',
    typeOrigine: '',
    description: '',
})

const errors = ref<Record<string, string>>({})
const globalError = ref('')
const loading = ref(false)
const submitting = ref(false)

/* -------------------- aperçu du code -------------------- */

const codePropose = ref('')
const codeLoading = ref(false)

/* -------------------- confirmation -------------------- */

const confirmOpen = ref(false)
const confirmLoading = ref(false)
const confirmDemande = ref<ConfirmDemande | null>(null)

const TYPES_SIGNALEMENT = ['INCIDENT', 'RISQUE', 'AUTRE'] as const
const typeOptions = TYPES_SIGNALEMENT.map(t => ({ value: t, label: t }))

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

/* -------------------- aide contextuelle -------------------- */

const descriptionExemples = computed(() => {
    const type = form.value.typeOrigine
    if (type === 'INCIDENT') {
        return {
            titre: 'Exemples pour un incident',
            items: [
                'Une panne du système de contrôle d\'accès au bâtiment A.',
                'Une intrusion constatée dans la salle serveur le 12/03.',
                'Une indisponibilité de la ligne réseau principale pendant 2 h.',
            ],
        }
    }
    if (type === 'RISQUE') {
        return {
            titre: 'Exemples pour un risque',
            items: [
                'Absence de plan de secours pour les serveurs critiques.',
                'Dépendance à un fournisseur unique pour la maintenance.',
                'Données sensibles accessibles sans restriction réseau.',
            ],
        }
    }
    return {
        titre: 'Exemples de signalement',
        items: [
            'Sélectionnez d\'abord un type pour voir les exemples adaptés.',
        ],
    }
})

/* -------------------- génération du code -------------------- */

watch(() => form.value.typeOrigine, async (nouveau) => {
    if (!nouveau) {
        codePropose.value = ''
        return
    }
    codeLoading.value = true
    const res = await getProchainCode(nouveau)
    codeLoading.value = false
    codePropose.value = res.success ? res.data.code : ''
})

/* -------------------- validation -------------------- */

function valider(): boolean {
    errors.value = {}

    if (!form.value.designation.trim()) {
        errors.value.designation = 'La désignation est obligatoire'
    } else if (form.value.designation.trim().length > 255) {
        errors.value.designation = 'Maximum 255 caractères'
    }

    if (!form.value.typeOrigine.trim()) {
        errors.value.typeOrigine = 'Le type est obligatoire'
    }

    return Object.keys(errors.value).length === 0
}

/* -------------------- confirmation -------------------- */

function demanderConfirmation(): void {
    globalError.value = ''
    if (!valider()) return

    confirmDemande.value = {
        titre: 'Confirmer la déclaration',
        message: `Vous allez déclarer le signalement « ${form.value.designation.trim()} ».`,
        consequence:
            'Le code sera attribué automatiquement par le système. '
            + 'Un signalement est un fait historique : une fois créé, il ne peut '
            + 'plus être modifié ni supprimé.',
        libelleConfirmer: 'Déclarer',
        libelleAnnuler: 'Revenir au formulaire',
        ton: 'primary',
        icone: 'bi bi-exclamation-circle',
    }

    confirmOpen.value = true
}

async function confirmerCreation(): Promise<void> {
    globalError.value = ''
    confirmLoading.value = true

    const payload: SignalementRequest = {
        designation: form.value.designation.trim(),
        typeOrigine: form.value.typeOrigine.trim(),
        description: form.value.description?.trim() || null,
    }

    const res = await creerSignalement(payload)
    confirmLoading.value = false

    if (!res.success) {
        confirmOpen.value = false
        globalError.value = res.error
        return
    }

    router.push({ name: 'signalements', query: { created: '1' } })
}

function annulerConfirmation(): void {
    confirmOpen.value = false
}

function annuler(): void {
    router.push({ name: 'signalements' })
}

/* -------------------- résumé -------------------- */

const resumeType = computed(() => form.value.typeOrigine || '—')
const resumeDesignation = computed(() => form.value.designation.trim() || '—')
const resumeCode = computed(() => codePropose.value || '—')

const formulairePret = computed(() =>
    form.value.typeOrigine.trim() !== ''
    && form.value.designation.trim() !== ''
)

onMounted(() => {
    // placeholder pour cohérence : rien à charger ici
})
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
                <h1>Déclarer un signalement</h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    Incident, risque ou autre événement à documenter
                </p>
            </div>

            <div class="welcome-row__right">
                <BaseButton variant="secondary" :disabled="submitting" @click="annuler">
                    <i class="bi bi-arrow-left"></i>
                    Retour à la liste
                </BaseButton>
            </div>
        </header>

        <!-- ============ MESSAGE GLOBAL ============ -->
        <div v-if="globalError" class="alert alert-danger mb-0" role="alert">
            <i class="bi bi-exclamation-triangle-fill"></i>
            {{ globalError }}
        </div>

        <!-- ============ FORMULAIRE ============ -->
        <form class="form-layout" novalidate @submit.prevent="demanderConfirmation">
            <!-- ============ COLONNE PRINCIPALE ============ -->
            <div class="form-main">
                <!-- 01 — Identification -->
                <section class="panel form-section">
                    <div class="section-head">
                        <span class="section-num">01</span>
                        <div>
                            <h2>Identification</h2>
                            <p>Nature de l'événement et code attribué automatiquement.</p>
                        </div>
                    </div>

                    <div class="field-grid">
                        <div class="field full">
                            <BaseSelect
                                v-model="form.typeOrigine"
                                label="Type de signalement"
                                required
                                placeholder="Sélectionner un type"
                                :options="typeOptions"
                                :error="errors.typeOrigine"
                                :disabled="submitting"
                            />
                        </div>

                        <div class="field full">
                            <BaseInput
                                :model-value="codePropose"
                                label="Code attribué"
                                readonly
                                placeholder="Sélectionnez un type pour obtenir le code"
                                :loading="codeLoading"
                                helper="Le code est généré automatiquement selon le type et l'année. Il sera définitif après création."
                            />
                        </div>

                        <div class="field full">
                            <BaseInput
                                v-model="form.designation"
                                label="Désignation"
                                required
                                placeholder="Résumé court du signalement"
                                :error="errors.designation"
                                :disabled="submitting"
                                helper="Maximum 255 caractères. Un titre explicite facilite la recherche."
                            />
                        </div>
                    </div>
                </section>

                <!-- 02 — Description -->
                <section class="panel form-section">
                    <div class="section-head">
                        <span class="section-num">02</span>
                        <div>
                            <h2>Description détaillée</h2>
                            <p>Circonstances, impacts et mesures déjà prises.</p>
                        </div>
                    </div>

                    <div class="field-grid">
                        <div class="field full">
                            <BaseInput
                                v-model="form.description"
                                type="textarea"
                                label="Description"
                                placeholder="Décrivez les circonstances, les impacts, les mesures déjà prises…"
                                :disabled="submitting"
                                helper="Facultatif, mais fortement recommandé pour le suivi."
                            />
                        </div>
                    </div>
                </section>

                <!-- ============ ACTIONS ============ -->
                <div class="form-actions">
                    <span class="form-note">
                        <i class="bi bi-info-circle"></i>
                        Un signalement ne peut pas être modifié après création.
                    </span>

                    <div class="form-actions-buttons">
                        <BaseButton
                            variant="secondary"
                            :disabled="submitting"
                            @click="annuler"
                        >
                            Annuler
                        </BaseButton>
                        <BaseButton
                            variant="primary"
                            type="submit"
                            :disabled="submitting || !formulairePret"
                        >
                            <i class="bi bi-check-lg"></i>
                            Déclarer le signalement
                        </BaseButton>
                    </div>
                </div>
            </div>

            <!-- ============ COLONNE LATÉRALE ============ -->
            <aside class="form-side">
                <!-- Récapitulatif -->
                <section class="panel side-card">
                    <h2 class="side-title">Récapitulatif</h2>
                    <p class="side-sub">Aperçu de la fiche avant enregistrement.</p>

                    <div class="recap">
                        <div class="recap-row">
                            <span>Type</span>
                            <strong>
                                <span v-if="form.typeOrigine" class="recap-badge">
                                    {{ resumeType }}
                                </span>
                                <span v-else class="recap-empty">—</span>
                            </strong>
                        </div>
                        <div class="recap-row">
                            <span>Code</span>
                            <strong>
                                <code v-if="codePropose" class="recap-code">{{ resumeCode }}</code>
                                <span v-else class="recap-empty">—</span>
                            </strong>
                        </div>
                        <div class="recap-row">
                            <span>Désignation</span>
                            <strong :class="{ 'recap-empty': !form.designation.trim() }">
                                {{ resumeDesignation }}
                            </strong>
                        </div>
                        <div class="recap-row">
                            <span>Description</span>
                            <strong :class="{ 'recap-empty': !form.description?.trim() }">
                                {{ form.description?.trim() ? 'Renseignée' : '—' }}
                            </strong>
                        </div>
                    </div>
                </section>

                <!-- Avertissement irréversibilité -->
                <section class="panel side-card side-card--warning">
                    <div class="warning-icon">
                        <i class="bi bi-exclamation-triangle-fill"></i>
                    </div>
                    <div>
                        <h3 class="warning-title">Signalement irréversible</h3>
                        <p class="warning-text">
                            Un signalement est un fait historique. Une fois créé,
                            il ne peut <strong>ni être modifié ni supprimé</strong>.
                            Vérifiez les informations avant de déclarer.
                        </p>
                    </div>
                </section>

                <!-- Conseils -->
                <section class="panel side-card">
                    <h2 class="side-title">{{ descriptionExemples.titre }}</h2>
                    <ul class="tip-list">
                        <li v-for="(ex, i) in descriptionExemples.items" :key="i">
                            <span>{{ i + 1 }}</span>
                            <em>{{ ex }}</em>
                        </li>
                    </ul>
                </section>
            </aside>
        </form>
    </div>

    <!-- ============ MODAL DE CONFIRMATION ============ -->
    <BaseConfirm
        v-model="confirmOpen"
        :demande="confirmDemande"
        :loading="confirmLoading"
        @confirme="confirmerCreation"
        @annule="annulerConfirmation"
    />
</template>

<style scoped>
/* ============================================================
   CONTENEUR PAGE
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

/* ============================================================
   FALLBACK PANEL (si absent globalement)
   ============================================================ */
.panel {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 14px;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .04),
        0 8px 24px rgba(26, 42, 108, .06);
}

/* ============================================================
   FORM LAYOUT (2 colonnes, sidebar sticky)
   ============================================================ */
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

/* ============================================================
   SECTIONS NUMÉROTÉES
   ============================================================ */
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
    letter-spacing: .3px;
    background: linear-gradient(135deg, #1a2a6c 0%, #4a6cf7 100%);
    box-shadow:
        0 6px 14px rgba(26, 42, 108, .22),
        inset 0 -2px 0 #d4af37;
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

/* ============================================================
   GRILLE DE CHAMPS
   ============================================================ */
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

/* ============================================================
   BARRE D'ACTIONS COLLANTE
   ============================================================ */
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
    -webkit-backdrop-filter: blur(12px);
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

/* ============================================================
   COLONNE LATÉRALE
   ============================================================ */
.side-card {
    padding: 20px 20px 18px;
}

.side-title {
    margin: 0 0 4px;
    font: 800 14px Manrope, sans-serif;
    letter-spacing: -.2px;
    color: #1a2a6c;
}

.side-sub {
    margin: 0 0 14px;
    font-size: 10.5px;
    color: #8a95a8;
}

/* --- Récapitulatif --- */
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
    padding-bottom: 0;
    border-bottom: 0;
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

.recap-badge {
    display: inline-block;
    padding: 0.2rem 0.6rem;
    border-radius: 20px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.7rem;
    font-weight: 700;
    letter-spacing: 0.03em;
}

.recap-code {
    display: inline-block;
    padding: 0.2rem 0.55rem;
    background: #eef1f5;
    color: #384a5e;
    border-radius: 4px;
    font-family: ui-monospace, Menlo, monospace;
    font-weight: 600;
    font-size: 0.75rem;
}

.recap-empty {
    color: #c3cad5;
    font-weight: 400;
}

/* --- Carte d'avertissement --- */
.side-card--warning {
    display: flex;
    gap: 0.85rem;
    padding: 1.1rem 1.25rem;
    background: linear-gradient(135deg, #fff8e6, #fff);
    border: 1px solid #f4e2c2;
    border-left: 3px solid #d4af37;
}

.warning-icon {
    flex: none;
    display: grid;
    place-items: center;
    width: 34px;
    height: 34px;
    border-radius: 10px;
    background: #fff6dd;
    color: #b8860b;
    font-size: 1.05rem;
}

.warning-title {
    margin: 0 0 0.35rem;
    font: 800 13px Manrope, sans-serif;
    color: #5c1a17;
    letter-spacing: -0.01em;
}

.warning-text {
    margin: 0;
    font-size: 11.5px;
    line-height: 1.55;
    color: #6b7280;
}

.warning-text strong {
    color: #5c1a17;
}

/* --- Conseils --- */
.tip-list {
    list-style: none;
    display: grid;
    gap: 12px;
    margin: 14px 0 0;
    padding: 0;
}

.tip-list li {
    display: flex;
    gap: 10px;
    font-size: 11px;
    line-height: 1.55;
    color: #6b7280;
    align-items: flex-start;
}

.tip-list li span {
    flex: none;
    display: grid;
    place-items: center;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: #fff6dd;
    color: #b8860b;
    font-size: 9.5px;
    font-weight: 800;
    margin-top: 0.1rem;
}

.tip-list li em {
    font-style: italic;
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 1100px) {
    .form-layout {
        grid-template-columns: 1fr;
    }

    .form-side {
        position: static;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 16px;
    }
}

@media (max-width: 1199.98px) {
    .sig-page {
        padding-left: 24px;
        padding-right: 24px;
    }
}

@media (max-width: 767.98px) {
    .sig-page {
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
    .sig-page {
        padding: 20px 12px 16px;
    }

    .form-section {
        padding: 18px 14px 20px;
    }

    .section-head {
        gap: 11px;
        margin-bottom: 18px;
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

    .form-actions {
        bottom: 0;
        padding: 12px 14px;
        border-radius: 12px;
    }
}

@media (prefers-reduced-motion: reduce) {
    .form-layout *,
    .form-layout *::before,
    .form-layout *::after {
        transition: none !important;
    }
}
</style>
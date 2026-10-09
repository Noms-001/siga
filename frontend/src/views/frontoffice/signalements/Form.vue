<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BaseInput, BaseSelect, BaseButton, BaseConfirm } from '@/components/base'
import {
    creerSignalement,
    getSignalementFiltres,
} from '@/services/signalement'
import type { SignalementRequest } from '@/types/backoffice/signalement'

import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'

/* -------------------- confirmation -------------------- */

const confirmOpen = ref(false)
const confirmLoading = ref(false)
const confirmDemande = ref<ConfirmDemande | null>(null)

/**
 * Étape 1 — l'utilisateur clique "Déclarer le signalement".
 *
 * On valide le formulaire AVANT d'ouvrir la confirmation : demander
 * "confirmez-vous ?" sur une saisie incomplète reviendrait à faire
 * confirmer une erreur. La validation reste ici, pas dans le bouton
 * de confirmation.
 */
function demanderConfirmation(): void {
    globalError.value = ''
    if (!valider()) return

    confirmDemande.value = {
        titre: 'Confirmer la déclaration',
        message:
            `Vous allez déclarer le signalement « ${form.value.designation.trim()} ».`,
        consequence:
            'Un signalement est un fait historique : une fois créé, il ne peut '
            + 'plus être modifié ni supprimé.',
        libelleConfirmer: 'Déclarer',
        libelleAnnuler: 'Revenir au formulaire',
        ton: 'primary',
        icone: 'bi bi-exclamation-circle',
    }

    confirmOpen.value = true
}

/** Étape 2 — l'utilisateur confirme. Seul point qui appelle le backend. */
async function confirmerCreation(): Promise<void> {
    globalError.value = ''
    confirmLoading.value = true

    const payload: SignalementRequest = {
        code: form.value.code.trim(),
        designation: form.value.designation.trim(),
        typeOrigine: form.value.typeOrigine.trim(),
        description: form.value.description?.trim() || null,
    }

    const res = await creerSignalement(payload)
    confirmLoading.value = false

    if (!res.success) {
        // Échec serveur : on referme la confirmation et on rend la main
        // au formulaire, où l'utilisateur peut corriger. La saisie n'est
        // pas perdue — le formulaire n'a jamais été démonté.
        confirmOpen.value = false
        globalError.value = res.error
        return
    }

    router.push({ name: 'signalements', query: { created: '1' } })
}

/** Fermeture sans confirmer : retour au formulaire, saisie préservée. */
function annulerConfirmation(): void {
    confirmOpen.value = false
}

const router = useRouter()

const form = ref<SignalementRequest>({
    code: '',
    designation: '',
    typeOrigine: '',
    description: '',
})

const typesDisponibles = ref<string[]>([])
const errors = ref<Record<string, string>>({})
const globalError = ref('')
const loading = ref(false)
const submitting = ref(false)

/**
 * Types disponibles chargés depuis la base.
 *
 * Ne pas figer la liste : elle dépend du référentiel réel. Les valeurs
 * les plus fréquentes (INCIDENT, RISQUE) n'ont pas à être connues du code.
 */
const typeOptions = computed(() =>
    typesDisponibles.value.map(t => ({ value: t, label: t }))
)

async function charger(): Promise<void> {
    loading.value = true
    const res = await getSignalementFiltres()
    loading.value = false
    if (res.success) typesDisponibles.value = res.data.types
}

function valider(): boolean {
    errors.value = {}

    if (!form.value.code.trim()) errors.value.code = 'Le code est obligatoire'
    else if (form.value.code.trim().length > 50) errors.value.code = 'Maximum 50 caractères'

    if (!form.value.designation.trim()) errors.value.designation = 'La désignation est obligatoire'
    else if (form.value.designation.trim().length > 255) errors.value.designation = 'Maximum 255 caractères'

    if (!form.value.typeOrigine.trim()) errors.value.typeOrigine = 'Le type est obligatoire'

    return Object.keys(errors.value).length === 0
}

async function soumettre(): Promise<void> {
    globalError.value = ''
    if (!valider()) return
    submitting.value = true

    const payload: SignalementRequest = {
        code: form.value.code.trim(),
        designation: form.value.designation.trim(),
        typeOrigine: form.value.typeOrigine.trim(),
        description: form.value.description?.trim() || null,
    }

    const res = await creerSignalement(payload)
    submitting.value = false

    if (!res.success) { globalError.value = res.error; return }
    router.push({
        name: 'signalements',
        query: { created: '1' },
    })
}

function annuler(): void {
    router.push({ name: 'signalements' })
}

onMounted(charger)
</script>

<template>
    <div class="sig-page">
        <header class="sig-header">
            <div>
                <h1 class="sig-header__title">Déclarer un signalement</h1>
                <p class="sig-header__subtitle">
                    Incident, risque ou autre événement à documenter
                </p>
            </div>
        </header>

        <div v-if="globalError" class="alert alert-danger mb-0" role="alert">
            {{ globalError }}
        </div>

        <div class="sig-form">
            <section class="sig-form__section">
                <h2 class="sig-form__title">Identification</h2>
                <div class="sig-form__row">
                    <BaseInput v-model="form.code" label="Code" required placeholder="Ex : INC-2026-001"
                        :error="errors.code" :disabled="loading || submitting" />
                    <BaseInput v-model="form.designation" label="Désignation" required
                        placeholder="Résumé court du signalement" :error="errors.designation"
                        :disabled="loading || submitting" />
                </div>

                <BaseSelect v-model="form.typeOrigine" label="Type" required placeholder="Sélectionner un type"
                    :options="typeOptions" :error="errors.typeOrigine" :disabled="loading || submitting" />
            </section>

            <section class="sig-form__section">
                <h2 class="sig-form__title">Description</h2>
                <BaseInput v-model="form.description" type="textarea" label="Description détaillée"
                    placeholder="Décrivez les circonstances, les impacts, les mesures déjà prises…"
                    :disabled="loading || submitting" />
            </section>

            <div class="sig-form__actions">
                <BaseButton variant="secondary" :disabled="submitting" @click="annuler">
                    Annuler
                </BaseButton>
                <BaseButton @click="demanderConfirmation">
                    Déclarer le signalement
                </BaseButton>
            </div>
        </div>
    </div>
    <!-- Confirmation avant création -->
    <BaseConfirm v-model="confirmOpen" :demande="confirmDemande" :loading="confirmLoading" @confirme="confirmerCreation"
        @annule="annulerConfirmation" />
</template>


<style scoped>
.sig-page {
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
}

.sig-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 1rem;
}

.sig-header__title {
    font-size: 1.4rem;
    font-weight: 700;
    color: var(--dts-navy);
    margin: 0;
}

.sig-header__subtitle {
    color: var(--dts-muted);
    margin: 0.15rem 0 0;
    font-size: 0.85rem;
}

.sig-form {
    max-width: 820px;
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
}

.sig-form__section {
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    padding: 1.25rem 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.sig-form__title {
    font-size: 0.78rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--dts-muted);
    margin: 0;
    padding-bottom: 0.5rem;
    border-bottom: 1px solid var(--dts-border);
}

.sig-form__row {
    display: grid;
    grid-template-columns: 1fr 2fr;
    gap: 1rem;
}

.sig-form__actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
}

@media (max-width: 576px) {
    .sig-page {
        padding: 1rem;
    }

    .sig-form__row {
        grid-template-columns: 1fr;
    }

    .sig-form__section {
        padding: 1rem;
    }
}
</style>
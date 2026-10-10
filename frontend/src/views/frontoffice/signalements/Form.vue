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

/**
 * Code affiché dans le champ en lecture seule.
 *
 * Rempli à chaque changement de type par un appel à /prochain-code.
 * C'est un APERÇU : le serveur régénère le code au moment de la
 * création. Deux formulaires ouverts en parallèle sur le même type
 * afficheront le même aperçu — le second enregistrement sera refusé
 * par la contrainte UNIQUE et l'utilisateur rechargera.
 */
const codePropose = ref('')
const codeLoading = ref(false)

/* -------------------- confirmation -------------------- */

const confirmOpen = ref(false)
const confirmLoading = ref(false)
const confirmDemande = ref<ConfirmDemande | null>(null)

const TYPES_SIGNALEMENT = ['INCIDENT', 'RISQUE', 'AUTRE'] as const
const typeOptions = TYPES_SIGNALEMENT.map(t => ({ value: t, label: t }))


/* -------------------- génération du code -------------------- */

/**
 * Regénère l'aperçu du code à chaque changement de type.
 *
 * Le watch couvre aussi le retour à vide (l'utilisateur désélectionne) :
 * dans ce cas, on efface l'aperçu plutôt que de le laisser sur un code
 * devenu obsolète.
 */
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

                <BaseSelect v-model="form.typeOrigine" label="Type" required placeholder="Sélectionner un type"
                    :options="typeOptions" :error="errors.typeOrigine" :disabled="loading || submitting" />

                <BaseInput :model-value="codePropose" label="Code attribué" readonly
                    placeholder="Sélectionnez un type pour obtenir le code" :loading="codeLoading"
                    helper="Le code est généré automatiquement selon le type et l'année." />

                <BaseInput v-model="form.designation" label="Désignation" required
                    placeholder="Résumé court du signalement" :error="errors.designation"
                    :disabled="loading || submitting" />
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
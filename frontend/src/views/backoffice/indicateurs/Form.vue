<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseInput, BaseButton } from '@/components/base'
import {
    creerIndicateur,
    getIndicateur,
    modifierIndicateur,
} from '@/services/indicateur'
import type { IndicateurRequest } from '@/types/backoffice/indicateur'

const route = useRoute()
const router = useRouter()

const isEdition = computed(() => route.name === 'backoffice-indicateurs-modifier')
const id = computed<number | null>(() =>
    isEdition.value ? Number(route.params.id) : null
)

/**
 * Les champs numériques restent typés `string` dans le formulaire pour
 * accepter les états intermédiaires de saisie (vide, "12.", etc.).
 * La conversion en number se fait à la soumission, après validation.
 */
const form = ref<{
    code: string
    designation: string
    codeHopex: string
    indicateurHopex: string
    typeIndicateur: string
    uniteMesure: string
    frequenceVerification: string
    frequenceAggregation: string
    definition: string
    methodeDetermination: string
    objectif: string
    valeurCible: string
    seuilMin: string
    seuilMax: string
    actif: boolean
}>({
    code: '',
    designation: '',
    codeHopex: '',
    indicateurHopex: '',
    typeIndicateur: '',
    uniteMesure: '',
    frequenceVerification: '',
    frequenceAggregation: '',
    definition: '',
    methodeDetermination: '',
    objectif: '',
    valeurCible: '',
    seuilMin: '',
    seuilMax: '',
    actif: true,
})

const errors = ref<Record<string, string>>({})
const globalError = ref('')
const loading = ref(false)
const submitting = ref(false)

async function charger(): Promise<void> {
    if (!isEdition.value || id.value == null) return
    loading.value = true
    const res = await getIndicateur(id.value)
    loading.value = false
    if (!res.success) { globalError.value = res.error; return }
    const d = res.data
    form.value = {
        code: d.code,
        designation: d.designation,
        codeHopex: d.codeHopex ?? '',
        indicateurHopex: d.indicateurHopex ?? '',
        typeIndicateur: d.typeIndicateur ?? '',
        uniteMesure: d.uniteMesure ?? '',
        frequenceVerification: d.frequenceVerification ?? '',
        frequenceAggregation: d.frequenceAggregation ?? '',
        definition: d.definition ?? '',
        methodeDetermination: d.methodeDetermination ?? '',
        objectif: d.objectif ?? '',
        valeurCible: d.valeurCible != null ? String(d.valeurCible) : '',
        seuilMin: d.seuilMin != null ? String(d.seuilMin) : '',
        seuilMax: d.seuilMax != null ? String(d.seuilMax) : '',
        actif: d.actif,
    }
}

function nombreOuNull(v: string): number | null {
    const t = v.trim()
    if (t === '') return null
    const n = Number(t)
    return Number.isFinite(n) ? n : null
}

function valider(): boolean {
    errors.value = {}

    if (!form.value.code.trim()) errors.value.code = 'Le code est obligatoire'
    else if (form.value.code.trim().length > 50) errors.value.code = 'Maximum 50 caractères'

    if (!form.value.designation.trim()) errors.value.designation = 'La désignation est obligatoire'
    else if (form.value.designation.trim().length > 250) errors.value.designation = 'Maximum 250 caractères'

    if (form.value.codeHopex.trim().length > 150) errors.value.codeHopex = 'Maximum 150 caractères'
    if (form.value.indicateurHopex.trim().length > 250) errors.value.indicateurHopex = 'Maximum 250 caractères'

    // Validation numérique : "12.3.4" ou "abc" doivent être refusés avant
    // d'atteindre le backend.
    for (const [key, label] of [
        ['valeurCible', 'La valeur cible'],
        ['seuilMin', 'Le seuil minimum'],
        ['seuilMax', 'Le seuil maximum'],
    ] as const) {
        const v = form.value[key].trim()
        if (v === '') continue
        const n = Number(v)
        if (!Number.isFinite(n)) {
            errors.value[key] = `${label} doit être un nombre`
        }
    }

    // Cohérence des seuils, uniquement si les trois valeurs sont fournies.
    const cible = nombreOuNull(form.value.valeurCible)
    const min = nombreOuNull(form.value.seuilMin)
    const max = nombreOuNull(form.value.seuilMax)

    if (cible != null && min != null && max != null) {
        if (min > cible) {
            errors.value.seuilMin = 'Le seuil minimum ne peut pas dépasser la valeur cible'
        }
        if (cible > max) {
            errors.value.seuilMax = 'Le seuil maximum ne peut pas être inférieur à la valeur cible'
        }
    }

    return Object.keys(errors.value).length === 0
}

async function soumettre(): Promise<void> {
    globalError.value = ''
    if (!valider()) return
    submitting.value = true

    const payload: IndicateurRequest = {
        code: form.value.code.trim(),
        designation: form.value.designation.trim(),
        codeHopex: form.value.codeHopex.trim() || null,
        indicateurHopex: form.value.indicateurHopex.trim() || null,
        typeIndicateur: form.value.typeIndicateur.trim() || null,
        uniteMesure: form.value.uniteMesure.trim() || null,
        frequenceVerification: form.value.frequenceVerification.trim() || null,
        frequenceAggregation: form.value.frequenceAggregation.trim() || null,
        definition: form.value.definition.trim() || null,
        methodeDetermination: form.value.methodeDetermination.trim() || null,
        objectif: form.value.objectif.trim() || null,
        valeurCible: nombreOuNull(form.value.valeurCible),
        seuilMin: nombreOuNull(form.value.seuilMin),
        seuilMax: nombreOuNull(form.value.seuilMax),
        actif: form.value.actif,
    }

    const res = isEdition.value && id.value != null
        ? await modifierIndicateur(id.value, payload)
        : await creerIndicateur(payload)
    submitting.value = false

    if (!res.success) { globalError.value = res.error; return }
    router.push({
        name: 'backoffice-indicateurs',
        query: isEdition.value ? { updated: '1' } : { created: '1' },
    })
}

function annuler(): void {
    router.push({ name: 'backoffice-indicateurs' })
}

onMounted(charger)
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">
                    {{ isEdition ? 'Modifier un indicateur' : 'Nouvel indicateur' }}
                </h1>
                <p class="bo-page__subtitle">
                    {{ isEdition
                        ? "Mettre à jour les informations de l'indicateur"
                        : 'Créer un nouvel indicateur de performance' }}
                </p>
            </div>
        </header>

        <div v-if="globalError" class="alert alert-danger mb-0" role="alert">
            {{ globalError }}
        </div>

        <div class="bo-form">
            <!-- Identification -->
            <section class="bo-form__section">
                <h2 class="bo-form__section-title">Identification</h2>
                <div class="bo-form__row">
                    <BaseInput v-model="form.code" label="Code" required :error="errors.code"
                        :disabled="loading || submitting" />
                    <BaseInput v-model="form.designation" label="Désignation" required :error="errors.designation"
                        :disabled="loading || submitting" />
                </div>
                <BaseInput v-model="form.typeIndicateur" label="Type d'indicateur"
                    placeholder="Ex : PERFORMANCE, RISQUE…" :disabled="loading || submitting" />
            </section>

            <!-- Référentiel HOPEx -->
            <section class="bo-form__section">
                <h2 class="bo-form__section-title">Référentiel HOPEx</h2>
                <div class="bo-form__row">
                    <BaseInput v-model="form.codeHopex" label="Code HOPEx" :error="errors.codeHopex"
                        :disabled="loading || submitting" />
                    <BaseInput v-model="form.indicateurHopex" label="Indicateur HOPEx" :error="errors.indicateurHopex"
                        :disabled="loading || submitting" />
                </div>
            </section>

            <!-- Mesure -->
            <section class="bo-form__section">
                <h2 class="bo-form__section-title">Mesure</h2>
                <BaseInput v-model="form.uniteMesure" label="Unité de mesure" :disabled="loading || submitting" />
                <div class="bo-form__row-3">
                    <BaseInput v-model="form.seuilMin" type="number" label="Seuil minimum" :error="errors.seuilMin"
                        :disabled="loading || submitting" />
                    <BaseInput v-model="form.valeurCible" type="number" label="Valeur cible" :error="errors.valeurCible"
                        :disabled="loading || submitting" />
                    <BaseInput v-model="form.seuilMax" type="number" label="Seuil maximum" :error="errors.seuilMax"
                        :disabled="loading || submitting" />
                </div>
            </section>

            <!-- Fréquence -->
            <section class="bo-form__section">
                <h2 class="bo-form__section-title">Fréquence</h2>
                <div class="bo-form__row">
                    <BaseInput v-model="form.frequenceVerification" label="Fréquence de vérification"
                        :disabled="loading || submitting" />
                    <BaseInput v-model="form.frequenceAggregation" label="Fréquence d'agrégation"
                        :disabled="loading || submitting" />
                </div>
            </section>

            <!-- Définition -->
            <section class="bo-form__section">
                <h2 class="bo-form__section-title">Définition</h2>
                <BaseInput v-model="form.definition" type="textarea" label="Définition"
                    :disabled="loading || submitting" />
                <BaseInput v-model="form.methodeDetermination" type="textarea" label="Méthode de détermination"
                    :disabled="loading || submitting" />
                <BaseInput v-model="form.objectif" type="textarea" label="Objectif" :disabled="loading || submitting" />
            </section>

            <!-- Statut -->
            <section class="bo-form__section">
                <h2 class="bo-form__section-title">Statut</h2>
                <BaseInput v-model="form.actif" type="checkbox" label="Indicateur actif"
                    :disabled="loading || submitting" />
            </section>

            <div class="bo-form__actions">
                <BaseButton variant="secondary" :disabled="submitting" @click="annuler">
                    Annuler
                </BaseButton>
                <BaseButton :loading="submitting" @click="soumettre">
                    {{ isEdition ? 'Enregistrer' : 'Créer' }}
                </BaseButton>
            </div>
        </div>
    </div>
</template>

<style scoped>
.bo-page {
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.bo-page__header {
    display: flex;
    flex-wrap: wrap;
    gap: 1rem;
    justify-content: space-between;
}

.bo-page__title {
    font-size: 1.4rem;
    font-weight: 700;
    color: var(--dts-navy);
    margin: 0;
}

.bo-page__subtitle {
    color: var(--dts-muted);
    margin: 0;
    font-size: 0.85rem;
}

.bo-form {
    max-width: 860px;
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
}

.bo-form__section {
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    padding: 1.25rem 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.bo-form__section-title {
    font-size: 0.78rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--dts-muted);
    margin: 0;
    padding-bottom: 0.5rem;
    border-bottom: 1px solid var(--dts-border);
}

.bo-form__row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
}

.bo-form__row-3 {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 1rem;
}

.bo-form__actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
}

@media (max-width: 768px) {

    .bo-form__row,
    .bo-form__row-3 {
        grid-template-columns: 1fr;
    }
}

@media (max-width: 576px) {
    .bo-page {
        padding: 1rem;
    }

    .bo-form__section {
        padding: 1rem;
    }
}
</style>
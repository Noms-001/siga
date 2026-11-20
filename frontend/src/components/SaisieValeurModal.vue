<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { BaseButton, BaseModal } from '@/components/base'

const props = defineProps<{
    modelValue: boolean
    uniteMesure?: string | null
    valeurCible?: number | null
    typeIndicateur?: string | null
    prefillDebut?: string | null
    prefillFin?: string | null
}>()

const emit = defineEmits<{
    (e: 'update:modelValue', value: boolean): void
    (e: 'valide', payload: {
        valeur: number
        periodeDebut: string
        periodeFin: string
        commentaire: string | null
    }): void
}>()

const ouvert = computed({
    get: () => props.modelValue,
    set: (value: boolean) => emit('update:modelValue', value),
})

const valeur = ref<number | ''>('')
const periodeDebut = ref<string>('')
const periodeFin = ref<string>('')
const commentaire = ref('')
const erreur = ref('')

/* pré-remplit la période sur le mois en cours à l'ouverture */
watch(() => props.modelValue, (ouvert) => {
    if (ouvert) {
        erreur.value = ''
        valeur.value = ''
        commentaire.value = ''

        if (props.prefillDebut && props.prefillFin) {
            periodeDebut.value = props.prefillDebut.slice(0, 10)
            periodeFin.value = props.prefillFin.slice(0, 10)
        } else {
            // défaut : mois en cours
            const now = new Date()
            const debut = new Date(now.getFullYear(), now.getMonth(), 1)
            const fin = new Date(now.getFullYear(), now.getMonth() + 1, 0)
            periodeDebut.value = formatDateInput(debut)
            periodeFin.value = formatDateInput(fin)
        }
    }
})

function formatDateInput(d: Date): string {
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${day}`
}

/* aperçu du statut attendu */
const apercu = computed(() => {
    if (valeur.value === '' || props.valeurCible == null || !props.typeIndicateur) {
        return null
    }
    const v = Number(valeur.value)
    const cible = props.valeurCible

    if (props.typeIndicateur === 'KPI') {
        return v >= cible
            ? { libelle: 'Cible atteinte', classe: 'preview--ok' }
            : { libelle: `Sous la cible (${cible})`, classe: 'preview--ko' }
    }
    if (props.typeIndicateur === 'KRI') {
        return v <= cible
            ? { libelle: 'Cible respectée', classe: 'preview--ok' }
            : { libelle: `Au-dessus de la cible (${cible})`, classe: 'preview--ko' }
    }
    return null
})

function fermer(): void {
    ouvert.value = false
}

function valider(): void {
    erreur.value = ''

    if (valeur.value === '' || isNaN(Number(valeur.value))) {
        erreur.value = 'La valeur est obligatoire.'
        return
    }
    if (!periodeDebut.value || !periodeFin.value) {
        erreur.value = 'Les dates de période sont obligatoires.'
        return
    }
    if (new Date(periodeFin.value) <= new Date(periodeDebut.value)) {
        erreur.value = 'La date de fin doit être strictement postérieure à la date de début.'
        return
    }

    // LocalDateTime au format ISO : "YYYY-MM-DDT00:00:00" et "YYYY-MM-DDT23:59:59"
    emit('valide', {
        valeur: Number(valeur.value),
        periodeDebut: `${periodeDebut.value}T00:00:00`,
        periodeFin: `${periodeFin.value}T23:59:59`,
        commentaire: commentaire.value.trim() || null,
    })
}
</script>

<template>
    <BaseModal v-model="ouvert" title="Saisir une valeur" size="md">
        <div class="saisie-form">
            <div class="field">
                <label class="field__label">
                    Valeur <span class="required">*</span>
                    <span v-if="uniteMesure" class="field__unit">({{ uniteMesure }})</span>
                </label>
                <input v-model.number="valeur" type="number" step="0.01" class="field__input"
                    placeholder="Ex. 85.50" />
                <div v-if="apercu" :class="['preview', apercu.classe]">
                    <i class="bi bi-info-circle"></i>
                    {{ apercu.libelle }}
                </div>
            </div>

            <div class="field-grid">
                <div class="field">
                    <label class="field__label">
                        Période — début <span class="required">*</span>
                    </label>
                    <input v-model="periodeDebut" type="date" class="field__input" />
                </div>
                <div class="field">
                    <label class="field__label">
                        Période — fin <span class="required">*</span>
                    </label>
                    <input v-model="periodeFin" type="date" class="field__input" />
                </div>
            </div>

            <div class="field">
                <label class="field__label">Commentaire</label>
                <textarea v-model="commentaire" rows="3" class="field__input"
                    placeholder="Précisions sur la mesure…" />
            </div>

            <div v-if="erreur" class="error">
                <i class="bi bi-exclamation-triangle-fill"></i>
                {{ erreur }}
            </div>
        </div>

        <template #footer>
            <BaseButton variant="secondary" @click="fermer">Annuler</BaseButton>
            <BaseButton variant="primary" icon="bi bi-check-lg" @click="valider">
                Enregistrer
            </BaseButton>
        </template>
    </BaseModal>
</template>

<style scoped>
.saisie-form {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.field {
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
}

.field__label {
    font-size: 0.78rem;
    font-weight: 600;
    color: #15233c;
}

.field__unit {
    color: #8a95a8;
    font-weight: 500;
}

.required {
    color: #d64545;
}

.field__input {
    width: 100%;
    padding: 0.55rem 0.75rem;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    font-size: 0.88rem;
    color: #15233c;
    font-family: inherit;
}

.field__input:focus {
    outline: none;
    border-color: #4a6cf7;
    box-shadow: 0 0 0 3px rgba(74, 108, 247, .1);
}

.field__input[type="date"] {
    font-variant-numeric: tabular-nums;
}

.field-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.75rem;
}

.preview {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.45rem 0.7rem;
    border-radius: 6px;
    font-size: 0.78rem;
    font-weight: 600;
}

.preview--ok {
    background: #e6f4ea;
    color: #1e7e34;
}

.preview--ko {
    background: #fdf3e2;
    color: #9a6409;
}

.error {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.6rem 0.85rem;
    background: #fde9e9;
    color: #b03030;
    border-radius: 8px;
    font-size: 0.82rem;
}
</style>

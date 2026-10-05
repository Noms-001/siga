<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseInput, BaseButton } from '@/components/base'
import { creerPoste, getPoste, modifierPoste } from '@/services/poste'
import type { PosteRequest } from '@/types/backoffice/poste'

const route = useRoute()
const router = useRouter()

const isEdition = computed(() => route.name === 'backoffice-postes-modifier')
const id = computed<number | null>(() =>
    isEdition.value ? Number(route.params.id) : null
)

/**
 * Le formulaire manipule les nombres comme des chaînes le temps de la saisie
 * (BaseInput type="number" émet une string). La conversion se fait dans
 * `soumettre()`, une fois la validation passée.
 */
const form = ref<{
    nom: string
    effectifPrevu: string
    effectifReel: string
    isMetier: boolean
}>({
    nom: '',
    effectifPrevu: '0',
    effectifReel: '',
    isMetier: true,
})

const errors = ref<Record<string, string>>({})
const globalError = ref('')
const loading = ref(false)
const submitting = ref(false)

async function charger(): Promise<void> {
    if (!isEdition.value || id.value == null) return
    loading.value = true
    const res = await getPoste(id.value)
    loading.value = false
    if (!res.success) {
        globalError.value = res.error
        return
    }
    form.value = {
        nom: res.data.nom,
        effectifPrevu: String(res.data.effectifPrevu ?? 0),
        effectifReel: res.data.effectifReel == null ? '' : String(res.data.effectifReel),
        isMetier: res.data.isMetier,
    }
}

function valider(): boolean {
    errors.value = {}

    if (!form.value.nom.trim()) {
        errors.value.nom = 'Le nom est obligatoire'
    }

    if (form.value.effectifPrevu.trim() === '') {
        errors.value.effectifPrevu = "L'effectif prévu est obligatoire"
    } else {
        const n = Number(form.value.effectifPrevu)
        if (!Number.isFinite(n) || !Number.isInteger(n) || n < 0) {
            errors.value.effectifPrevu = 'Doit être un entier supérieur ou égal à 0'
        }
    }

    if (form.value.effectifReel.trim() !== '') {
        const n = Number(form.value.effectifReel)
        if (!Number.isFinite(n) || !Number.isInteger(n) || n < 0) {
            errors.value.effectifReel = 'Doit être un entier supérieur ou égal à 0'
        }
    }

    return Object.keys(errors.value).length === 0
}

async function soumettre(): Promise<void> {
    globalError.value = ''
    if (!valider()) return
    submitting.value = true

    const payload: PosteRequest = {
        nom: form.value.nom.trim(),
        effectifPrevu: Number(form.value.effectifPrevu),
        effectifReel: form.value.effectifReel.trim() === ''
            ? null
            : Number(form.value.effectifReel),
        isMetier: form.value.isMetier,
    }

    const res = isEdition.value && id.value != null
        ? await modifierPoste(id.value, payload)
        : await creerPoste(payload)
    submitting.value = false

    if (!res.success) {
        globalError.value = res.error
        return
    }
    router.push({
        name: 'backoffice-postes',
        query: isEdition.value ? { updated: '1' } : { created: '1' },
    })
}

function annuler(): void {
    router.push({ name: 'backoffice-postes' })
}

onMounted(charger)
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">
                    {{ isEdition ? 'Modifier un poste' : 'Nouveau poste' }}
                </h1>
                <p class="bo-page__subtitle">
                    {{ isEdition ? 'Mettre à jour les informations du poste' : 'Créer un nouveau poste' }}
                </p>
            </div>
        </header>

        <div v-if="globalError" class="alert alert-danger mb-0" role="alert">
            {{ globalError }}
        </div>

        <div class="bo-form">
            <BaseInput v-model="form.nom" label="Nom" placeholder="Ex : Chef de service" required :error="errors.nom"
                :disabled="loading || submitting" />

            <div class="bo-form__row">
                <BaseInput v-model="form.effectifPrevu" type="number" label="Effectif prévu" placeholder="0" required
                    :error="errors.effectifPrevu" :disabled="loading || submitting" />

                <BaseInput v-model="form.effectifReel" type="number" label="Effectif réel" placeholder="Optionnel"
                    :error="errors.effectifReel" :disabled="loading || submitting" />
            </div>

            <div class="bo-form__switch">
                <BaseInput v-model="form.isMetier" type="checkbox" label="Poste métier"
                    :disabled="loading || submitting" />
                <small class="bo-form__hint">
                    Un poste support est rattaché à une fonction transversale (RH, informatique, etc.).
                </small>
            </div>

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
    max-width: 720px;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.bo-form__row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
}

.bo-form__switch {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
    padding: 0.75rem 1rem;
    background: var(--dts-bg);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius-sm);
}

.bo-form__hint {
    font-size: 0.78rem;
    color: var(--dts-muted);
}

.bo-form__actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
    margin-top: 0.5rem;
}

@media (max-width: 576px) {
    .bo-page {
        padding: 1rem;
    }

    .bo-form {
        padding: 1rem;
    }

    .bo-form__row {
        grid-template-columns: 1fr;
    }
}
</style>
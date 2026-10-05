<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseInput, BaseButton } from '@/components/base'
import { creerRole, getRole, modifierRole } from '@/services/role'
import type { RoleRequest } from '@/types/backoffice/role'

const route = useRoute()
const router = useRouter()

const isEdition = computed(() => route.name === 'backoffice-roles-modifier')
const id = computed<number | null>(() =>
    isEdition.value ? Number(route.params.id) : null
)

const form = ref<RoleRequest>({
    code: '',
    designation: '',
    description: '',
})

const errors = ref<Record<string, string>>({})
const globalError = ref('')
const loading = ref(false)
const submitting = ref(false)

async function charger(): Promise<void> {
    if (!isEdition.value || id.value == null) return
    loading.value = true
    const res = await getRole(id.value)
    loading.value = false
    if (!res.success) {
        globalError.value = res.error
        return
    }
    form.value = {
        code: res.data.code,
        designation: res.data.designation,
        description: res.data.description ?? '',
    }
}

function valider(): boolean {
    errors.value = {}

    if (!form.value.code.trim()) {
        errors.value.code = 'Le code est obligatoire'
    } else if (!/^[A-Z][A-Z0-9_]*$/i.test(form.value.code.trim())) {
        errors.value.code = 'Le code ne peut contenir que des lettres, chiffres et underscores'
    }

    if (!form.value.designation.trim()) {
        errors.value.designation = 'La désignation est obligatoire'
    }

    return Object.keys(errors.value).length === 0
}

async function soumettre(): Promise<void> {
    globalError.value = ''
    if (!valider()) return
    submitting.value = true

    const payload: RoleRequest = {
        code: form.value.code.trim().toUpperCase(),
        designation: form.value.designation.trim(),
        description: form.value.description?.trim() || null,
    }

    const res = isEdition.value && id.value != null
        ? await modifierRole(id.value, payload)
        : await creerRole(payload)
    submitting.value = false

    if (!res.success) {
        globalError.value = res.error
        return
    }
    router.push({
        name: 'backoffice-roles',
        query: isEdition.value ? { updated: '1' } : { created: '1' },
    })
}

function annuler(): void {
    router.push({ name: 'backoffice-roles' })
}

onMounted(charger)
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">
                    {{ isEdition ? 'Modifier un rôle' : 'Nouveau rôle' }}
                </h1>
                <p class="bo-page__subtitle">
                    {{ isEdition ? 'Mettre à jour les informations du rôle' : 'Créer un nouveau rôle' }}
                </p>
            </div>
        </header>

        <div v-if="globalError" class="alert alert-danger mb-0" role="alert">
            {{ globalError }}
        </div>

        <div class="bo-form">
            <BaseInput v-model="form.code" label="Code"
                placeholder="Ex : CHEF_SERVICE" required
                :error="errors.code" :disabled="loading || submitting" />
            <small class="bo-form__hint">
                Le code est unique et stocké en majuscules. Exemples : ADMIN, CHEF_SERVICE, VALIDATEUR.
            </small>

            <BaseInput v-model="form.designation" label="Désignation"
                placeholder="Ex : Chef de service" required
                :error="errors.designation" :disabled="loading || submitting" />

            <BaseInput v-model="form.description" type="textarea" label="Description"
                placeholder="Description du rôle" :disabled="loading || submitting" />

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
.bo-page { padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; }

.bo-page__header { display: flex; flex-wrap: wrap; gap: 1rem; justify-content: space-between; }
.bo-page__title { font-size: 1.4rem; font-weight: 700; color: var(--dts-navy); margin: 0; }
.bo-page__subtitle { color: var(--dts-muted); margin: 0; font-size: 0.85rem; }

.bo-form {
    max-width: 720px;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.bo-form__hint {
    font-size: 0.78rem;
    color: var(--dts-muted);
    margin-top: -0.25rem;
}

.bo-form__actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
    margin-top: 0.75rem;
}

@media (max-width: 576px) {
    .bo-page { padding: 1rem; }
    .bo-form { padding: 1rem; }
}
</style>
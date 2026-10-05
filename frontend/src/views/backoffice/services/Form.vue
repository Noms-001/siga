<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseInput, BaseSelect, BaseButton } from '@/components/base'
import { creerService, getService, modifierService } from '@/services/service'
import { listerDepartements } from '@/services/departement'
import type { Departement } from '@/types/backoffice/departement'
import type { ServiceRequest } from '@/types/backoffice/service'

const route = useRoute()
const router = useRouter()

const isEdition = computed(() => route.name === 'backoffice-services-modifier')
const id = computed<number | null>(() =>
    isEdition.value ? Number(route.params.id) : null
)

const form = ref<ServiceRequest>({
    nom: '',
    description: '',
    idDepartement: 0,
})

const departements = ref<Departement[]>([])
const errors = ref<Record<string, string>>({})
const globalError = ref('')
const loading = ref(false)
const submitting = ref(false)

const departementOptions = computed(() =>
    departements.value.map(d => ({
        value: d.id,
        label: `${d.code} — ${d.nom}`,
    }))
)

async function charger(): Promise<void> {
    loading.value = true
    const depsRes = await listerDepartements()
    if (depsRes.success) departements.value = depsRes.data

    if (isEdition.value && id.value != null) {
        const res = await getService(id.value)
        if (!res.success) {
            loading.value = false
            globalError.value = res.error
            return
        }
        form.value = {
            nom: res.data.nom,
            description: res.data.description ?? '',
            idDepartement: res.data.departement.id,
        }
    }
    loading.value = false
}

function valider(): boolean {
    errors.value = {}
    if (!form.value.nom.trim()) errors.value.nom = 'Le nom est obligatoire'
    if (!form.value.idDepartement || form.value.idDepartement <= 0) {
        errors.value.idDepartement = 'Le département est obligatoire'
    }
    return Object.keys(errors.value).length === 0
}

async function soumettre(): Promise<void> {
    globalError.value = ''
    if (!valider()) return
    submitting.value = true
    const payload: ServiceRequest = {
        nom: form.value.nom.trim(),
        description: form.value.description?.trim() || null,
        idDepartement: Number(form.value.idDepartement),
    }
    const res = isEdition.value && id.value != null
        ? await modifierService(id.value, payload)
        : await creerService(payload)
    submitting.value = false

    if (!res.success) {
        globalError.value = res.error
        return
    }
    router.push({
        name: 'backoffice-services',
        query: isEdition.value ? { updated: '1' } : { created: '1' },
    })
}

function annuler(): void {
    router.push({ name: 'backoffice-services' })
}

onMounted(charger)
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">
                    {{ isEdition ? 'Modifier un service' : 'Nouveau service' }}
                </h1>
                <p class="bo-page__subtitle">
                    {{ isEdition ? 'Mettre à jour les informations du service' : 'Créer un nouveau service' }}
                </p>
            </div>
        </header>

        <div v-if="globalError" class="alert alert-danger mb-0" role="alert">
            {{ globalError }}
        </div>

        <div class="bo-form">
            <BaseInput v-model="form.nom" label="Nom"
                placeholder="Ex : Service de Gestion des Risques et Procédures" required
                :error="errors.nom" :disabled="loading || submitting" />

            <BaseSelect v-model="form.idDepartement" label="Département" required
                placeholder="Sélectionner un département"
                :options="departementOptions" :error="errors.idDepartement"
                :disabled="loading || submitting" />

            <BaseInput v-model="form.description" type="textarea" label="Description"
                placeholder="Description du service" :disabled="loading || submitting" />

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
    gap: 1rem;
}

.bo-form__actions { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 0.5rem; }

@media (max-width: 576px) {
    .bo-page { padding: 1rem; }
    .bo-form { padding: 1rem; }
}
</style>
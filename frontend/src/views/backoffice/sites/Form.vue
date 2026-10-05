<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseInput, BaseButton } from '@/components/base'
import { creerSite, getSite, modifierSite } from '@/services/site'
import type { SiteRequest } from '@/types/backoffice/site'

const route = useRoute()
const router = useRouter()

const isEdition = computed(() => route.name === 'backoffice-sites-modifier')
const id = computed<number | null>(() =>
    isEdition.value ? Number(route.params.id) : null
)

const form = ref<SiteRequest>({ nom: '' })
const errors = ref<Record<string, string>>({})
const globalError = ref('')
const loading = ref(false)
const submitting = ref(false)

async function charger(): Promise<void> {
    if (!isEdition.value || id.value == null) return
    loading.value = true
    const res = await getSite(id.value)
    loading.value = false
    if (!res.success) { globalError.value = res.error; return }
    form.value = { nom: res.data.nom }
}

function valider(): boolean {
    errors.value = {}
    if (!form.value.nom.trim()) errors.value.nom = 'Le nom est obligatoire'
    return Object.keys(errors.value).length === 0
}

async function soumettre(): Promise<void> {
    globalError.value = ''
    if (!valider()) return
    submitting.value = true
    const payload: SiteRequest = { nom: form.value.nom.trim() }
    const res = isEdition.value && id.value != null
        ? await modifierSite(id.value, payload)
        : await creerSite(payload)
    submitting.value = false
    if (!res.success) { globalError.value = res.error; return }
    router.push({
        name: 'backoffice-sites',
        query: isEdition.value ? { updated: '1' } : { created: '1' },
    })
}

function annuler(): void {
    router.push({ name: 'backoffice-sites' })
}

onMounted(charger)
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">
                    {{ isEdition ? 'Modifier un site' : 'Nouveau site' }}
                </h1>
                <p class="bo-page__subtitle">
                    {{ isEdition ? 'Mettre à jour les informations du site' : 'Créer un nouveau site' }}
                </p>
            </div>
        </header>

        <div v-if="globalError" class="alert alert-danger mb-0" role="alert">
            {{ globalError }}
        </div>

        <div class="bo-form">
            <BaseInput v-model="form.nom" label="Nom" placeholder="Ex : Siège Antananarivo" required
                :error="errors.nom" :disabled="loading || submitting" />

            <div class="bo-form__actions">
                <BaseButton variant="secondary" :disabled="submitting" @click="annuler">Annuler</BaseButton>
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
.bo-form { max-width: 720px; background: var(--dts-surface); border: 1px solid var(--dts-border); border-radius: var(--dts-radius); padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; }
.bo-form__actions { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 0.5rem; }
@media (max-width: 576px) { .bo-page { padding: 1rem; } .bo-form { padding: 1rem; } }
</style>
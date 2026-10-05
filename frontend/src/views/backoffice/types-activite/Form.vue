<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseInput, BaseButton } from '@/components/base'
import {
    creerTypeActivite,
    getTypeActivite,
    modifierTypeActivite,
} from '@/services/typeActivite'
import { listerServices } from '@/services/service'
import type { TypeActiviteRequest } from '@/types/backoffice/typeActivite'
import type { Service } from '@/types/backoffice/service'

const route = useRoute()
const router = useRouter()

const isEdition = computed(() => route.name === 'backoffice-types-activite-modifier')
const id = computed<number | null>(() =>
    isEdition.value ? Number(route.params.id) : null
)

const form = ref<{
    designation: string
    description: string
    idServices: number[]
}>({
    designation: '',
    description: '',
    idServices: [],
})

const services = ref<Service[]>([])
const errors = ref<Record<string, string>>({})
const globalError = ref('')
const loading = ref(false)
const submitting = ref(false)

/**
 * Regroupe les services par département pour l'affichage.
 *
 * Une liste plate de 50 services est illisible ; le regroupement par
 * département fait apparaître la structure et rend la recherche visuelle
 * immédiate.
 */
const servicesParDepartement = computed(() => {
    const groupes = new Map<number, {
        idDepartement: number
        code: string
        nom: string
        services: Service[]
    }>()

    for (const s of services.value) {
        const key = s.departement.id
        if (!groupes.has(key)) {
            groupes.set(key, {
                idDepartement: s.departement.id,
                code: s.departement.code,
                nom: s.departement.nom,
                services: [],
            })
        }
        groupes.get(key)!.services.push(s)
    }

    return Array.from(groupes.values())
        .sort((a, b) => a.code.localeCompare(b.code))
})

async function charger(): Promise<void> {
    loading.value = true

    // On charge tous les services (y compris inactifs) pour ne pas perdre
    // une association existante avec un service désactivé depuis.
    const servicesRes = await listerServices({ inclureInactifs: true })
    if (servicesRes.success) services.value = servicesRes.data

    if (isEdition.value && id.value != null) {
        const res = await getTypeActivite(id.value)
        if (!res.success) {
            loading.value = false
            globalError.value = res.error
            return
        }
        form.value = {
            designation: res.data.designation,
            description: res.data.description ?? '',
            idServices: res.data.services.map(s => s.id),
        }
    }

    loading.value = false
}

function valider(): boolean {
    errors.value = {}
    if (!form.value.designation.trim()) {
        errors.value.designation = 'La désignation est obligatoire'
    }
    return Object.keys(errors.value).length === 0
}

async function soumettre(): Promise<void> {
    globalError.value = ''
    if (!valider()) return
    submitting.value = true

    const payload: TypeActiviteRequest = {
        designation: form.value.designation.trim(),
        description: form.value.description?.trim() || null,
        idServices: [...form.value.idServices],
    }

    const res = isEdition.value && id.value != null
        ? await modifierTypeActivite(id.value, payload)
        : await creerTypeActivite(payload)
    submitting.value = false

    if (!res.success) {
        globalError.value = res.error
        return
    }
    router.push({
        name: 'backoffice-types-activite',
        query: isEdition.value ? { updated: '1' } : { created: '1' },
    })
}

function annuler(): void {
    router.push({ name: 'backoffice-types-activite' })
}

onMounted(charger)
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">
                    {{ isEdition ? "Modifier un type d'activité" : "Nouveau type d'activité" }}
                </h1>
                <p class="bo-page__subtitle">
                    {{ isEdition
                        ? "Mettre à jour les informations et les services associés"
                        : "Créer un nouveau type d'activité" }}
                </p>
            </div>
        </header>

        <div v-if="globalError" class="alert alert-danger mb-0" role="alert">
            {{ globalError }}
        </div>

        <div class="bo-form">
            <BaseInput v-model="form.designation" label="Désignation"
                placeholder="Ex : Installation" required
                :error="errors.designation" :disabled="loading || submitting" />

            <BaseInput v-model="form.description" type="textarea" label="Description"
                placeholder="Description du type d'activité"
                :disabled="loading || submitting" />

            <div class="bo-form__services">
                <div class="bo-form__services-head">
                    <label class="bo-form__services-label">Services associés</label>
                    <span class="bo-form__services-count">
                        {{ form.idServices.length }} sélectionné{{ form.idServices.length > 1 ? 's' : '' }}
                    </span>
                </div>

                <p class="bo-form__services-hint">
                    Un type d'activité peut être rattaché à un ou plusieurs services.
                    Laissez vide pour ne rattacher à aucun service.
                </p>

                <div v-if="loading" class="bo-form__services-loading">
                    <div v-for="i in 3" :key="i" class="skeleton skeleton-line"></div>
                </div>

                <div v-else-if="services.length === 0" class="bo-form__services-empty">
                    Aucun service disponible. Créez d'abord un service.
                </div>

                <div v-else class="bo-form__services-list">
                    <div v-for="grp in servicesParDepartement" :key="grp.idDepartement"
                        class="bo-form__group">
                        <div class="bo-form__group-head">
                            <span class="badge-code">{{ grp.code }}</span>
                            <span class="bo-form__group-nom">{{ grp.nom }}</span>
                        </div>

                        <div class="bo-form__group-items">
                            <label v-for="s in grp.services" :key="s.id"
                                class="bo-form__check">
                                <input type="checkbox" :value="s.id" v-model="form.idServices"
                                    :disabled="loading || submitting">
                                <span class="bo-form__check-label">
                                    {{ s.nom }}
                                    <span v-if="!s.actif" class="badge-inactif-inline">inactif</span>
                                </span>
                            </label>
                        </div>
                    </div>
                </div>
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
.bo-page { padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; }

.bo-page__header { display: flex; flex-wrap: wrap; gap: 1rem; justify-content: space-between; }
.bo-page__title { font-size: 1.4rem; font-weight: 700; color: var(--dts-navy); margin: 0; }
.bo-page__subtitle { color: var(--dts-muted); margin: 0; font-size: 0.85rem; }

.bo-form {
    max-width: 820px;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

/* --- Bloc services --- */

.bo-form__services {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    padding: 1rem;
    background: var(--dts-bg);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius-sm);
}

.bo-form__services-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;
    flex-wrap: wrap;
}

.bo-form__services-label {
    font-weight: 600;
    color: var(--text-color);
}

.bo-form__services-count {
    font-size: 0.75rem;
    font-weight: 600;
    background: var(--dts-blue-50);
    color: var(--dts-blue);
    padding: 0.15rem 0.6rem;
    border-radius: 20px;
    border: 1px solid var(--dts-border);
}

.bo-form__services-hint {
    font-size: 0.78rem;
    color: var(--dts-muted);
    margin: 0;
    line-height: 1.5;
}

.bo-form__services-loading {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    padding: 0.5rem 0;
}

.bo-form__services-empty {
    padding: 1rem;
    text-align: center;
    font-size: 0.85rem;
    color: var(--dts-muted);
    background: var(--dts-surface);
    border: 1px dashed var(--dts-border-2);
    border-radius: var(--dts-radius-sm);
}

.bo-form__services-list {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    max-height: 380px;
    overflow-y: auto;
    padding: 0.25rem;
}

.bo-form__group {
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius-sm);
    padding: 0.65rem 0.85rem;
}

.bo-form__group-head {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 0.4rem;
}

.bo-form__group-nom {
    font-size: 0.78rem;
    color: var(--dts-muted);
}

.badge-code {
    font-size: 0.7rem;
    font-weight: 700;
    padding: 0.15rem 0.5rem;
    background: var(--dts-navy);
    color: #fff;
    border-radius: 4px;
    letter-spacing: 0.04em;
}

.bo-form__group-items {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 0.35rem 1rem;
}

.bo-form__check {
    display: flex;
    align-items: center;
    gap: 0.55rem;
    font-size: 0.85rem;
    color: var(--dts-text);
    cursor: pointer;
    padding: 0.2rem 0;
    user-select: none;
}

.bo-form__check input {
    width: 16px;
    height: 16px;
    accent-color: var(--dts-blue);
    cursor: pointer;
}

.bo-form__check-label { display: inline-flex; align-items: center; gap: 0.4rem; }

.badge-inactif-inline {
    font-size: 0.65rem;
    font-weight: 600;
    padding: 0.05rem 0.4rem;
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
    border-radius: 20px;
}

.bo-form__actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
    margin-top: 0.5rem;
}

.skeleton-line {
    height: 14px;
    border-radius: 6px;
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: sk 1.4s infinite;
}
@keyframes sk { from { background-position: 200% 0; } to { background-position: -200% 0; } }

@media (max-width: 576px) {
    .bo-page { padding: 1rem; }
    .bo-form { padding: 1rem; }
    .bo-form__group-items { grid-template-columns: 1fr; }
}
</style>
<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { BaseInput, BaseSelect, BaseButton } from '@/components/base'
import { creerUtilisateur } from '@/services/utilisateur'
import { listerDepartements } from '@/services/departement'
import { listerServices } from '@/services/service'
import { listerPostes } from '@/services/poste'
import type { Departement } from '@/types/backoffice/departement'
import type { Service } from '@/types/backoffice/service'
import type { Poste } from '@/types/backoffice/poste'
import type { UtilisateurRequest } from '@/types/backoffice/utilisateur'

const router = useRouter()

const form = ref<{
    nom: string
    prenom: string
    email: string
    telephone: string
    idDepartement: number | ''
    idService: number | ''       // '' = aucun service (au lieu de null)
    idPoste: number | ''
}>({
    nom: '',
    prenom: '',
    email: '',
    telephone: '',
    idDepartement: '',
    idService: '',               // '' et non null
    idPoste: '',
})

const departements = ref<Departement[]>([])
const services = ref<Service[]>([])
const postes = ref<Poste[]>([])

const errors = ref<Record<string, string>>({})
const globalError = ref('')
const loading = ref(false)
const submitting = ref(false)

const departementOptions = computed(() =>
    departements.value.map(d => ({ value: d.id, label: `${d.code} — ${d.nom}` }))
)

/** Services filtrés par département sélectionné. */
const serviceOptions = computed(() => {
    if (form.value.idDepartement === '') return []
    return services.value
        .filter(s => s.departement.id === Number(form.value.idDepartement))
        .map(s => ({ value: s.id, label: s.nom }))
})

/** Postes actifs uniquement — un utilisateur ne peut pas être affecté à un poste désactivé. */
const posteOptions = computed(() =>
    postes.value
        .filter(p => p.actif)
        .map(p => ({
            value: p.id,
            label: `${p.nom}${p.isMetier ? '' : ' (support)'}`,
        }))
)

async function charger(): Promise<void> {
    loading.value = true
    const [depsRes, servsRes, postesRes] = await Promise.all([
        listerDepartements(),
        listerServices({ inclureInactifs: true }),
        listerPostes(),
    ])
    loading.value = false
    if (depsRes.success) departements.value = depsRes.data
    if (servsRes.success) services.value = servsRes.data
    if (postesRes.success) postes.value = postesRes.data
}

/** Réinitialise le service quand le département change. */
watch(() => form.value.idDepartement, () => {
    form.value.idService = ''
})

function valider(): boolean {
    errors.value = {}

    if (!form.value.nom.trim()) errors.value.nom = 'Le nom est obligatoire'
    if (!form.value.prenom.trim()) errors.value.prenom = 'Le prénom est obligatoire'

    const email = form.value.email.trim()
    if (!email) {
        errors.value.email = "L'email est obligatoire"
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        errors.value.email = "Format d'email invalide"
    }

    if (form.value.idDepartement === '') {
        errors.value.idDepartement = 'Le département est obligatoire'
    }

    if (form.value.idPoste === '') {
        errors.value.idPoste = 'Le poste est obligatoire'
    }

    return Object.keys(errors.value).length === 0
}

async function soumettre(): Promise<void> {
    globalError.value = ''
    if (!valider()) return
    submitting.value = true

    // Construction explicite du payload aligné sur UtilisateurRequest du backend.
    // idService est optionnel : on envoie null plutôt que 0 ou ''.
    const payload: UtilisateurRequest = {
        nom: form.value.nom.trim(),
        prenom: form.value.prenom.trim(),
        email: form.value.email.trim().toLowerCase(),
        telephone: form.value.telephone.trim() || null,
        idDepartement: Number(form.value.idDepartement),
        idService: form.value.idService === '' ? null : Number(form.value.idService),
        idPoste: Number(form.value.idPoste),
    }

    const res = await creerUtilisateur(payload)
    submitting.value = false

    if (!res.success) {
        globalError.value = res.error
        return
    }

    router.push({
        name: 'backoffice-utilisateurs',
        query: { created: '1' },
    })
}

function annuler(): void {
    router.push({ name: 'backoffice-utilisateurs' })
}

onMounted(charger)
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">Nouvel utilisateur</h1>
                <p class="bo-page__subtitle">
                    Créer un compte utilisateur. Un email d'activation sera envoyé
                    automatiquement.
                </p>
            </div>
        </header>

        <div v-if="globalError" class="alert alert-danger mb-0" role="alert">
            {{ globalError }}
        </div>

        <div class="bo-form">
            <div class="bo-form__row">
                <BaseInput v-model="form.nom" label="Nom" required :error="errors.nom"
                    :disabled="loading || submitting" />
                <BaseInput v-model="form.prenom" label="Prénom" required :error="errors.prenom"
                    :disabled="loading || submitting" />
            </div>

            <BaseInput v-model="form.email" type="email" label="Email" required placeholder="utilisateur@exemple.mg"
                :error="errors.email" :disabled="loading || submitting" />

            <BaseInput v-model="form.telephone" type="tel" label="Téléphone" placeholder="Optionnel"
                :disabled="loading || submitting" />

            <hr class="bo-form__sep">

            <BaseSelect v-model="form.idDepartement" label="Département" required
                placeholder="Sélectionner un département" :options="departementOptions" :error="errors.idDepartement"
                :disabled="loading || submitting" />

            <BaseSelect v-model="form.idService" label="Service" placeholder="Aucun (optionnel)"
                :options="serviceOptions" :disabled="loading || submitting || form.idDepartement === ''" />

            <BaseSelect v-model="form.idPoste" label="Poste" required placeholder="Sélectionner un poste"
                :options="posteOptions" :error="errors.idPoste" :disabled="loading || submitting" />

            <div class="bo-form__info" role="note">
                <i class="bi bi-info-circle"></i>
                <span>
                    À la création, le compte est inactif. L'utilisateur reçoit un
                    email d'activation pour définir son mot de passe et activer
                    son compte.
                </span>
            </div>

            <div class="bo-form__actions">
                <BaseButton variant="secondary" :disabled="submitting" @click="annuler">
                    Annuler
                </BaseButton>
                <BaseButton :loading="submitting" @click="soumettre">
                    Créer
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

.bo-form__sep {
    border: none;
    border-top: 1px solid var(--dts-border);
    margin: 0.25rem 0;
}

.bo-form__info {
    display: flex;
    gap: 0.6rem;
    padding: 0.75rem 1rem;
    background: var(--dts-blue-50);
    border-left: 3px solid var(--dts-blue);
    border-radius: 6px;
    font-size: 0.82rem;
    color: var(--dts-text-2);
    line-height: 1.5;
}

.bo-form__info i {
    color: var(--dts-blue);
    flex-shrink: 0;
    margin-top: 0.1rem;
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
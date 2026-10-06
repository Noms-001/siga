<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseInput, BaseButton } from '@/components/base'
import { activateAccount, verifyActivationToken } from '@/services/auth'

const route = useRoute()
const router = useRouter()

/*
    Le jeton d'activation voyage en query string, parce qu'un clic sur un
    lien HTML déclenche un GET — la page lit le jeton, puis c'est le
    formulaire qui le renvoie en POST.
*/
const token = ref('')

const password = ref('')
const confirmPassword = ref('')

const errors = ref<Record<string, string>>({})
const globalError = ref('')
const submitting = ref(false)
const success = ref(false)
const tokenInvalide = ref(false)

/**
 * Longueur minimale du mot de passe.
 *
 * Doit rester alignée sur la contrainte portée par ActivationRequest côté
 * backend. Une divergence produirait un refus que l'utilisateur ne
 * comprendrait pas (le front aurait laissé passer ce que le serveur
 * rejette, ou l'inverse).
 */
const PASSWORD_MIN_LENGTH = 8

const tokenManquant = computed(() => token.value === '')

onMounted(async () => {
    const q = route.query.token

    if (typeof q === 'string' && q.trim() !== '') {
        token.value = q.trim()
    }

    if (tokenManquant.value) {
        tokenInvalide.value = true
        globalError.value =
            "Lien d'activation invalide : le jeton est manquant. " +
            "Utilisez le lien contenu dans l'email d'invitation."
        return
    }

    // Vérifie le jeton avant d'afficher le formulaire : l'utilisateur voit
    // immédiatement si son lien est expiré ou déjà consommé, sans avoir à
    // saisir un mot de passe pour découvrir le problème.
    const res = await verifyActivationToken(token.value)

    if (!res.success) {
        tokenInvalide.value = true
        globalError.value = res.error
    }
})

function valider(): boolean {
    errors.value = {}

    if (!password.value) {
        errors.value.password = 'Le mot de passe est obligatoire'
    } else if (password.value.length < PASSWORD_MIN_LENGTH) {
        errors.value.password =
            `Le mot de passe doit contenir au moins ${PASSWORD_MIN_LENGTH} caractères`
    }

    if (!confirmPassword.value) {
        errors.value.confirmPassword = 'La confirmation est obligatoire'
    } else if (confirmPassword.value !== password.value) {
        errors.value.confirmPassword = 'Les mots de passe ne correspondent pas'
    }

    return Object.keys(errors.value).length === 0
}

async function soumettre(): Promise<void> {
    globalError.value = ''

    if (!valider()) return

    submitting.value = true

    const res = await activateAccount(
        token.value,
        password.value,
        confirmPassword.value,
    )

    submitting.value = false

    if (!res.success) {
        globalError.value = res.error
        return
    }

    success.value = true

    // Court délai pour que l'utilisateur voie la confirmation avant la
    // redirection : sans cela, le formulaire disparaîtrait d'un coup.
    setTimeout(() => {
        router.push({ name: 'login', query: { activated: '1' } })
    }, 1500)
}
</script>

<template>
    <div class="activation-page">
        <div class="activation-card">
            <header class="activation-card__head">
                <div class="activation-card__mark">
                    <i class="bi bi-shield-check"></i>
                </div>
                <h1 class="activation-card__title">Activation de votre compte</h1>
                <p class="activation-card__subtitle">
                    Choisissez un mot de passe pour activer votre compte SAGA.
                </p>
            </header>

            <!-- Succès -->
            <div v-if="success" class="activation-success" role="status">
                <i class="bi bi-check-circle-fill"></i>
                <div>
                    <strong>Compte activé</strong>
                    <p>Vous allez être redirigé vers la page de connexion…</p>
                </div>
            </div>

            <!-- Erreur globale : jeton manquant, expiré, ou refus serveur -->
            <div v-else-if="globalError" class="alert alert-danger" role="alert">
                {{ globalError }}
            </div>

            <!-- Formulaire -->
            <form v-else class="activation-form" @submit.prevent="soumettre">
                <BaseInput v-model="password" type="password" label="Mot de passe" required
                    placeholder="Choisissez un mot de passe" :error="errors.password" :disabled="submitting" />

                <BaseInput v-model="confirmPassword" type="password" label="Confirmation du mot de passe" required
                    placeholder="Retapez le mot de passe" :error="errors.confirmPassword" :disabled="submitting" />

                <p class="activation-form__hint">
                    Le mot de passe doit contenir au moins
                    {{ PASSWORD_MIN_LENGTH }} caractères.
                </p>

                <BaseButton type="submit" :loading="submitting" class="activation-form__submit">
                    Activer mon compte
                </BaseButton>
            </form>
        </div>
    </div>
</template>

<style scoped>
body {
    padding-top: 0 !important;
    /* Supprime le padding-top global pour la page de connexion */
}

.activation-page {
    position: fixed;
    inset: 0;
    overflow-y: auto;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 2rem 1rem;
    background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
}

.activation-card {
    width: 100%;
    max-width: 440px;
    background: #fff;
    border-radius: 16px;
    padding: 2rem;
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.25);
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
}

.activation-card__head {
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.75rem;
}

.activation-card__mark {
    width: 56px;
    height: 56px;
    border-radius: 16px;
    display: grid;
    place-items: center;
    color: #fff;
    font-size: 1.6rem;
    background: linear-gradient(145deg, #0d2b4e 0%, #1f6fb2 115%);
    box-shadow: 0 4px 12px rgba(13, 43, 78, 0.28);
}

.activation-card__title {
    font-size: 1.2rem;
    font-weight: 700;
    color: #0d2b4e;
    margin: 0;
}

.activation-card__subtitle {
    font-size: 0.85rem;
    color: #5b6b7d;
    margin: 0;
    line-height: 1.5;
}

.activation-form {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.activation-form__hint {
    font-size: 0.78rem;
    color: #5b6b7d;
    margin: 0;
    line-height: 1.5;
}

.activation-form__submit {
    width: 100%;
    margin-top: 0.5rem;
}

.activation-success {
    display: flex;
    align-items: flex-start;
    gap: 0.85rem;
    padding: 1rem 1.15rem;
    background: #e6f4ea;
    border: 1px solid #c8e5cf;
    border-radius: 12px;
    color: #14532d;
}

.activation-success i {
    font-size: 1.4rem;
    color: #1e7e34;
    flex-shrink: 0;
    margin-top: 0.1rem;
}

.activation-success strong {
    display: block;
    font-size: 0.9rem;
    margin-bottom: 0.15rem;
}

.activation-success p {
    font-size: 0.82rem;
    margin: 0;
    line-height: 1.5;
}

@media (max-width: 480px) {
    .activation-card {
        padding: 1.5rem;
        border-radius: 14px;
    }
}
</style>
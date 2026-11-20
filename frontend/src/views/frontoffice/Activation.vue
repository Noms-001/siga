<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseCard, BaseInput, BaseButton } from '@/components/base'
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
    <div class="auth-wrapper">
        <BaseCard class="auth-card" :custom-class="['border-0', 'shadow-lg']">
            <template #default>

                <!-- En-tête : logo + titre, identique au login -->
                <header class="auth-header">
                    <div class="auth-logo-ring">
                        <img src="/logo/logo.png" alt="Logo" class="brand-logo">
                    </div>

                    <img src="/logo/titre.png" alt="GESTIA" class="brand-title-img">

                    <span class="auth-divider" aria-hidden="true"></span>
                </header>

                <h2 class="auth-title text-center">Activation de votre compte</h2>
                <p class="auth-subtitle text-center">
                    Choisissez un mot de passe pour activer votre compte SAGA.
                </p>

                <!-- Succès -->
                <div v-if="success" class="alert alert-success" role="status">
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
                <form v-else @submit.prevent="soumettre">
                    <BaseInput v-model="password" type="password" label="Mot de passe" required
                        placeholder="Choisissez un mot de passe" :error="errors.password" :disabled="submitting"
                        custom-class="mb-3" />

                    <BaseInput v-model="confirmPassword" type="password" label="Confirmation du mot de passe" required
                        placeholder="Retapez le mot de passe" :error="errors.confirmPassword" :disabled="submitting"
                        custom-class="mb-3" />

                    <p class="auth-hint">
                        Le mot de passe doit contenir au moins
                        {{ PASSWORD_MIN_LENGTH }} caractères.
                    </p>

                    <BaseButton type="submit" variant="primary" size="md" :loading="submitting" :disabled="submitting"
                        custom-class="w-100">
                        {{ submitting ? 'Activation en cours...' : 'Activer mon compte' }}
                    </BaseButton>
                </form>

                <!-- Pied de page identique au login -->
                <div class="auth-footer text-center border-top mt-4 pt-3">
                    &copy; 2026 &middot; Département Études, Sûreté et Sécurité
                </div>

            </template>
        </BaseCard>
    </div>
</template>

<style scoped>
/* ============ Page ============ */
.auth-wrapper {
    position: fixed;
    inset: 0;
    width: 100%;
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 1rem;
    overflow-y: auto;

    background:
        radial-gradient(circle at 12% 10%, rgba(26, 42, 108, 0.10) 0, transparent 38%),
        radial-gradient(circle at 90% 92%, rgba(212, 175, 55, 0.14) 0, transparent 36%),
        linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
}

/* ============ Carte ============ */
.auth-card {
    position: relative;
    overflow: auto;
    width: 100%;
    max-width: 490px;
    max-height: 97%;
    padding: 0 2rem 1.5rem;
    border-radius: 22px !important;
    background: #fff;
    box-shadow: 0 20px 60px rgba(26, 42, 108, 0.12);
    flex-shrink: 0;
}

/* Liseré décoratif bleu → doré en haut de la carte */
.auth-card::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 5px;
    background: linear-gradient(90deg, #1a2a6c 0%, #4a6cf7 55%, #d4af37 100%);
}

/* ============ En-tête (logo + titre) ============ */
.auth-header {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-bottom: 0.5rem;
}

/* Logo dans un médaillon : le mettre en valeur et le détacher du fond */
.auth-logo-ring {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 120px;
    height: 120px;
    padding: 8px;
}

.brand-logo {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: contain;
}

/* Titre : largeur maîtrisée, fond blanc du JPEG fondu dans la carte */
.brand-title-img {
    display: block;
    width: 100%;
    max-width: 260px;
    max-height: 58px;
    height: auto;
    object-fit: contain;
    mix-blend-mode: multiply;
}

/* Séparateur doré sous l'en-tête */
.auth-divider {
    display: block;
    width: 200px;
    height: 3px;
    border-radius: 3px;
    background: linear-gradient(90deg, #1a2a6c, #d4af37);
}

/* ============ Titres ============ */
.auth-title {
    font-size: 1.4rem;
    font-weight: 700;
    color: #1a2a6c;
    margin-bottom: 0.25rem;
}

.auth-subtitle {
    font-size: 0.875rem;
    color: #6b7280;
    margin-bottom: 1.25rem;
}

/* Indice sur le mot de passe */
.auth-hint {
    font-size: 0.78rem;
    color: #6b7280;
    margin: 0 0 0.75rem;
    line-height: 1.5;
}

/* Pied de page */
.auth-footer {
    font-size: 0.75rem;
    color: #9ca3af;
}

/* ============ Messages ============ */
.alert {
    border-radius: 12px;
    font-size: 0.9rem;
    padding: 0.75rem 1rem;
}

.alert-danger {
    background-color: #fef2f2;
    border-color: #fecaca;
    color: #dc2626;
}

.alert-success {
    display: flex;
    align-items: flex-start;
    gap: 0.85rem;
    background-color: #e6f4ea;
    border: 1px solid #c8e5cf;
    color: #14532d;
}

.alert-success i {
    font-size: 1.4rem;
    color: #1e7e34;
    flex-shrink: 0;
    margin-top: 0.1rem;
}

.alert-success strong {
    display: block;
    font-size: 0.9rem;
    margin-bottom: 0.15rem;
}

.alert-success p {
    font-size: 0.82rem;
    margin: 0;
    line-height: 1.5;
}

/* ============ Responsive ============ */
@media (max-width: 480px) {
    .auth-wrapper {
        padding: 0.75rem;
    }

    .auth-card {
        padding: 1.75rem 1.25rem 1.25rem;
    }

    .auth-logo-ring {
        width: 76px;
        height: 76px;
        padding: 7px;
    }

    .brand-title-img {
        max-width: 100%;
        max-height: 48px;
    }

    .auth-title {
        font-size: 1.4rem;
    }
}
</style>
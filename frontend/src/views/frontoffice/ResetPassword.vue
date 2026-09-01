<template>
    <div class="auth-page">
        <div class="auth-container">
            <BaseCard class="auth-card">
                <!-- En-tête -->
                <div class="auth-header">
                    <div class="auth-icon">
                        <i class="bi bi-key"></i>
                    </div>
                    <h1 class="auth-title">Réinitialisation du mot de passe</h1>
                    <p class="auth-subtitle text-muted">
                        {{ isTokenValid ? 'Définissez votre nouveau mot de passe' : 'Lien invalide ou expiré' }}
                    </p>
                </div>

                <!-- 1. Vérification du token -->
                <div v-if="verifying" class="auth-loading">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Vérification du token...</span>
                    </div>
                    <p class="mt-2 text-muted">Vérification du lien en cours...</p>
                </div>

                <!-- 2. Token invalide -->
                <div v-else-if="!isTokenValid" class="auth-error">
                    <i class="bi bi-exclamation-circle-fill"></i>
                    <div>
                        <p class="fw-semibold mb-1">Lien invalide ou expiré</p>
                        <p class="mb-0">
                            Le lien de réinitialisation est invalide ou a expiré.
                            Veuillez faire une nouvelle demande.
                        </p>
                    </div>

                    <div class="auth-footer mt-3">
                        <router-link to="/forgot-password" class="auth-link">
                            <i class="bi bi-arrow-left"></i>
                            Faire une nouvelle demande
                        </router-link>
                    </div>
                </div>

                <!-- 3. Réinitialisation réussie -->
                <div v-else-if="success" class="auth-success-container">
                    <div class="auth-success-icon">
                        <i class="bi bi-check-circle-fill"></i>
                    </div>

                    <h2 class="success-title">
                        Mot de passe réinitialisé avec succès !
                    </h2>

                    <p class="success-message text-muted">
                        Votre mot de passe a été modifié avec succès.
                        Vous pouvez maintenant vous connecter avec votre nouveau mot de passe.
                    </p>

                    <BaseButton variant="primary" size="lg" custom-class="w-100" @click="goToLogin">
                        <i class="bi bi-box-arrow-in-right"></i>
                        Se connecter
                    </BaseButton>
                </div>

                <!-- 4. Token valide → formulaire -->
                <form v-else @submit.prevent="handleSubmit" class="auth-form">
                    <BaseInput v-model="password" label="Nouveau mot de passe" type="password" name="password"
                        placeholder="••••••••" :required="true" :disabled="submitting" icon="bi bi-lock" size="lg" />

                    <BaseInput v-model="confirmPassword" label="Confirmer le mot de passe" type="password"
                        name="confirmPassword" placeholder="••••••••" :required="true" :disabled="submitting"
                        :error="confirmError" icon="bi bi-lock" size="lg" />

                    <!-- Erreur renvoyée par le backend -->
                    <div v-if="errorMessage" class="auth-error">
                        <i class="bi bi-exclamation-circle-fill"></i>
                        <p>{{ errorMessage }}</p>
                    </div>

                    <BaseButton type="submit" variant="primary" size="lg" :loading="submitting" :disabled="submitting"
                        custom-class="w-100">
                        <i class="bi bi-check-circle"></i>
                        Réinitialiser le mot de passe
                    </BaseButton>
                </form>
            </BaseCard>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseCard, BaseInput, BaseButton } from '@/components/base'
import { useAuthStore } from '@/stores/auth'

// --- Router ---
const route = useRoute()
const router = useRouter()
const token = computed(() => route.query.token as string || '')

// --- State ---
const password = ref('')
const confirmPassword = ref('')
const verifying = ref(true)
const isTokenValid = ref(false)
const submitting = ref(false)
const success = ref(false)
const errorMessage = ref('')

const authStore = useAuthStore()

const goToLogin = () => {
    router.push({ path: '/login' })
}


// --- Validation ---
const confirmError = computed(() => {
    if (!confirmPassword.value) return ''
    if (password.value !== confirmPassword.value) {
        return 'Les mots de passe ne correspondent pas'
    }
    return ''
})

const isFormValid = computed(() => {
    return (
        password.value.length > 0 &&
        confirmPassword.value.length > 0 &&
        !confirmError.value
    )
})

// --- Methods ---
const verifyToken = async () => {
    if (!token.value) {
        verifying.value = false
        isTokenValid.value = false
        return
    }

    try {
        await authStore.verifyResetPasswordToken(token.value)
        isTokenValid.value = true
    } catch (err: any) {
        isTokenValid.value = false
        console.error('Token verification error:', err)
    } finally {
        verifying.value = false
    }
}

const handleSubmit = async () => {
    if (!isFormValid.value || submitting.value || !isTokenValid.value) return

    submitting.value = true
    errorMessage.value = ''

    try {
        await authStore.resetPassword({
            token: token.value,
            password: password.value,
            confirmPassword: confirmPassword.value
        })

        success.value = true
        password.value = ''
        confirmPassword.value = ''
    } catch (err: any) {
        errorMessage.value = err.message 
        console.error('Reset password error:', err)
    } finally {
        submitting.value = false
    }
}

// --- Lifecycle ---
onMounted(() => {
    verifyToken()
})
</script>

<style scoped>
/* ============================================
   AUTH PAGES - Styles communs
   ============================================ */

.auth-page {
    position: fixed;
    inset: 0;
    width: 100%;
    height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: radial-gradient(circle at top, rgba(8, 56, 105, 0.08), transparent 28%),
        var(--bg-color, #eef2f8f4);
    padding: 2rem 1rem;
}

.auth-container {
    width: 100%;
    max-width: 440px;
}

.auth-card {
    padding: 2.5rem 2rem;
    border: 1px solid var(--border-color, #dee2e6);
    border-radius: var(--radius-xl, 1rem);
    box-shadow: var(--shadow-md, 0 20px 50px rgba(15, 23, 42, 0.1));
}

/* --- En-tête --- */
.auth-header {
    text-align: center;
    margin-bottom: 2rem;
}

.auth-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--primary-color, #083869), var(--secondary-color, #164493));
    color: white;
    font-size: 2rem;
    margin-bottom: 1rem;
    box-shadow: 0 4px 12px rgba(8, 56, 105, 0.2);
}

.auth-title {
    font-size: 1.5rem;
    font-weight: 700;
    color: var(--text-color, #0f172a);
    margin: 0 0 0.5rem;
}

.auth-subtitle {
    font-size: 0.9rem;
    margin: 0;
    color: var(--text-muted-custom, #64748b);
}

/* --- Loading --- */
.auth-loading {
    text-align: center;
    padding: 2rem 0;
}

/* --- Formulaire --- */
.auth-form {
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
}

.auth-form .base-input-container {
    margin-bottom: 0.25rem;
}

/* --- Messages --- */
.auth-success {
    display: flex;
    align-items: flex-start;
    gap: 0.75rem;
    padding: 0.75rem 1rem;
    background: rgba(16, 185, 129, 0.08);
    border-left: 4px solid var(--success-color, #10b981);
    border-radius: var(--radius-md, 0.25rem);
    color: var(--success-color, #10b981);
}

.auth-success i {
    font-size: 1.25rem;
    flex-shrink: 0;
    margin-top: 0.1rem;
}

.auth-success p {
    margin: 0;
    font-size: 0.9rem;
}

.auth-error {
    display: flex;
    align-items: flex-start;
    flex-direction: column;
    gap: 0.75rem;
    padding: 0.75rem 1rem;
    background: rgba(220, 38, 38, 0.08);
    border-left: 4px solid var(--danger-color, #dc2626);
    border-radius: var(--radius-md, 0.25rem);
    color: var(--danger-color, #dc2626);
}

.auth-error i {
    font-size: 1.25rem;
    flex-shrink: 0;
    margin-top: 0.1rem;
}

.auth-error p {
    margin: 0;
    font-size: 0.9rem;
}

/* --- Footer --- */
.auth-footer {
    text-align: center;
    padding-top: 0.5rem;
}

.auth-link {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    color: var(--text-muted-custom, #64748b);
    text-decoration: none;
    font-size: 0.9rem;
    font-weight: 500;
    transition: color 0.2s ease;
}

.auth-link:hover {
    color: var(--primary-color, #083869);
}

.auth-link-primary {
    color: var(--primary-color, #083869);
    font-weight: 600;
}

.auth-link-primary:hover {
    color: var(--secondary-color, #164493);
}

/* --- Spinner --- */
.spinner-border {
    width: 2rem;
    height: 2rem;
}

/* --- Responsive --- */
@media (max-width: 575.98px) {
    .auth-card {
        padding: 1.5rem 1.25rem;
    }

    .auth-title {
        font-size: 1.25rem;
    }

    .auth-icon {
        width: 52px;
        height: 52px;
        font-size: 1.5rem;
    }
}
</style>
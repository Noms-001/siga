<template>
    <div class="auth-page">
        <div class="auth-container">
            <BaseCard class="auth-card">
                <!-- En-tête -->
                <div class="auth-header">
                    <div class="auth-icon">
                        <i class="bi bi-shield-lock"></i>
                    </div>
                    <h1 class="auth-title">Mot de passe oublié</h1>
                    <p class="auth-subtitle text-muted">
                        Saisissez votre adresse email pour recevoir un lien de réinitialisation
                    </p>
                </div>

                <!-- Formulaire -->
                <form @submit.prevent="handleSubmit" class="auth-form">
                    <BaseInput v-model="email" label="Adresse email" type="email" name="email"
                        placeholder="exemple@dts.mg" :required="true" :disabled="loading" :error="emailError"
                        icon="bi bi-envelope" size="lg" />

                    <BaseButton type="submit" variant="primary" size="lg" :loading="loading"
                        :disabled="loading || !isFormValid" custom-class="w-100">
                        <i class="bi bi-send"></i>
                        Envoyer le lien de réinitialisation
                    </BaseButton>

                    <!-- Message de succès -->
                    <div v-if="success" class="auth-success">
                        <i class="bi bi-check-circle-fill"></i>
                        <p>
                            Si cette adresse email correspond à un compte,
                            un lien de réinitialisation vous a été envoyé.
                        </p>
                    </div>

                    <!-- Message d'erreur général -->
                    <div v-if="errorMessage" class="auth-error">
                        <i class="bi bi-exclamation-circle-fill"></i>
                        <p>{{ errorMessage }}</p>
                    </div>

                    <!-- Lien retour -->
                    <div class="auth-footer">
                        <router-link to="/login" class="auth-link">
                            <i class="bi bi-arrow-left"></i>
                            Retour à la connexion
                        </router-link>
                    </div>
                </form>
            </BaseCard>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { BaseCard, BaseInput, BaseButton } from '@/components/base'
import { forgotPassword } from '@/services/auth'

// --- State ---
const email = ref('')
const loading = ref(false)
const success = ref(false)
const errorMessage = ref('')

// --- Validation ---
const emailError = computed(() => {
    if (!email.value) return ''
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
    if (!emailRegex.test(email.value)) {
        return 'Veuillez saisir une adresse email valide'
    }
    return ''
})

const isFormValid = computed(() => {
    return email.value.trim() !== '' && !emailError.value
})

// --- Methods ---
const handleSubmit = async () => {
    if (!isFormValid.value || loading.value) return

    loading.value = true
    success.value = false
    errorMessage.value = ''

    try {
        await forgotPassword({ email: email.value.trim() })

        // Succès - ne pas révéler si le compte existe
        success.value = true
        email.value = '' // Réinitialiser le champ pour sécurité
    } catch (err: any) {
        // En cas d'erreur, afficher un message générique
        errorMessage.value = 'Une erreur est survenue. Veuillez réessayer ultérieurement.'
        console.error('Forgot password error:', err)
    } finally {
        loading.value = false
    }
}
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
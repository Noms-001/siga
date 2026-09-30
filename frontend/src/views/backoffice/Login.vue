<template>
    <div class="backoffice-auth-wrapper">
        <BaseCard class="backoffice-auth-card" custom-class="border-0 shadow-lg">
            <!-- En-tête -->
            <div class="backoffice-auth-header text-center">
                <div class="backoffice-auth-brand">
                    <span class="backoffice-brand-name"><i class="bi bi-shield-shaded"></i>DTS</span>
                    <span class="backoffice-brand-sub d-block">
                        Backoffice · Administration
                    </span>
                </div>
            </div>

            <div class="backoffice-auth-body">
                <h2 class="backoffice-auth-title text-center">Connexion au Backoffice</h2>
                <p class="backoffice-auth-subtitle text-center">
                    Accédez à l'interface d'administration
                </p>

                <form @submit.prevent="handleLogin" class="backoffice-auth-form">
                    <!-- Email -->
                    <BaseInput v-model="email" label="Adresse email" placeholder="admin@dts.mg" type="email"
                        name="email" :required="true" :error="emailError" size="lg" icon="bi bi-envelope"
                        custom-class="mb-3" />

                    <!-- Mot de passe -->
                    <BaseInput v-model="password" label="Mot de passe" placeholder="••••••••" type="password"
                        name="password" :required="true" :error="passwordError" size="lg" icon="bi bi-lock"
                        custom-class="mb-4" />

                    <!-- Session fermée pour inactivité -->
                    <div v-if="sessionExpiree" class="backoffice-alert backoffice-alert-warning" role="alert">
                        <i class="bi bi-clock-history"></i>
                        <span>Votre session a été fermée après une période d’inactivité. Veuillez vous reconnecter.</span>
                    </div>

                    <!-- Message d'erreur général -->
                    <div v-if="loginError" class="backoffice-alert backoffice-alert-danger" role="alert">
                        <i class="bi bi-exclamation-circle-fill"></i>
                        <span>{{ loginError }}</span>
                    </div>

                    <!-- Bouton de connexion -->
                    <BaseButton type="submit" variant="primary" size="lg" :loading="loading" :disabled="loading"
                        custom-class="w-100 backoffice-login-btn">
                        <i class="bi bi-box-arrow-in-right"></i>
                        {{ loading ? 'Connexion en cours...' : 'Se connecter' }}
                    </BaseButton>
                </form>

                <!-- Lien mot de passe oublié -->
                <div class="backoffice-auth-links text-center mt-3">
                    <RouterLink to="/forgot-password" class="backoffice-forgot-link">
                        Mot de passe oublié ?
                    </RouterLink>
                </div>
            </div>

            <!-- Pied de page -->
            <div class="backoffice-auth-footer text-center mt-4 pt-3 border-top">
                &copy; {{ currentYear }} &middot; DTS — Département Études, Sûreté et Sécurité
                <span class="backoffice-footer-badge">Backoffice</span>
            </div>
        </BaseCard>
    </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseCard, BaseInput, BaseButton } from '@/components/base'
import { useAuthStore } from '@/stores/auth'

// --- Router ---
const router = useRouter()
const route = useRoute()

// --- Store ---
const authStore = useAuthStore()

// --- State ---
const email = ref('')
const password = ref('')
const emailError = ref('')
const passwordError = ref('')
const loginError = ref('')
const loading = ref(false)

// --- Computed ---
const currentYear = computed(() => new Date().getFullYear())

/**
 * Session fermée pour inactivité : le watchdog redirige ici avec
 * `?session=expiree`. Sans ce message, l'utilisateur landing sur le login
 * sans avoir cliqué sur "Déconnexion" croirait à une erreur de sa part.
 */
const sessionExpiree = computed(() => route.query.session === 'expiree')

// --- Methods ---
const validateEmail = (value: string): boolean => {
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
    return emailRegex.test(value)
}

const handleLogin = async () => {
    // Réinitialiser les erreurs
    emailError.value = ''
    passwordError.value = ''
    loginError.value = ''

    // --- Validation frontend ---
    // Email
    if (!email.value.trim()) {
        emailError.value = 'L\'adresse email est obligatoire'
        return
    }
    if (!validateEmail(email.value)) {
        emailError.value = 'Veuillez saisir une adresse email valide'
        return
    }

    // Mot de passe
    if (!password.value) {
        passwordError.value = 'Le mot de passe est obligatoire'
        return
    }

    try {
        loading.value = true

        // Appel au store
        await authStore.login({
            email: email.value.trim(),
            password: password.value
        }, true) // true pour indiquer que c'est une connexion backoffice

        // Vérifier que l'utilisateur est bien connecté
        if (authStore.isAuthenticated) {
            // Redirection vers le dashboard
            await router.push('/backoffice/dashboard')
        } else {
            loginError.value = 'Une erreur est survenue lors de la connexion'
        }
    } catch (error: unknown) {
        console.error('Erreur de connexion:', error)

        // Récupérer le message d'erreur du backend
        const err = error as any
        loginError.value =
            err?.response?.data?.error ??
            err?.response?.data?.message ??
            err?.message ??
            'Une erreur est survenue. Veuillez réessayer.'
    } finally {
        loading.value = false
    }
}
</script>

<style scoped>
/* ============================================
   BACKOFFICE LOGIN - Styles
   ============================================ */

.backoffice-auth-wrapper {
    position: fixed;
    inset: 0;
    width: 100%;
    height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-color, #eef2f8f4);
    padding: 1.5rem;
    overflow-y: auto;
}

/* --- Carte --- */
.backoffice-auth-card {
    max-width: 580px;
    width: 100%;
    padding: 2.5rem 2rem 2rem;
    border-radius: 24px !important;
    background: var(--card-bg, #ffffff);
    box-shadow: var(--shadow-lg, 0 30px 80px rgba(15, 23, 42, 0.15));
    transition: transform 0.2s ease;
    border: 1px solid var(--border-color, #dee2e6) !important;
}

/* --- En-tête --- */
.backoffice-auth-header {
    margin-bottom: 1.5rem;
}

.backoffice-auth-brand {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
}

.backoffice-brand-name {
    font-size: 2rem;
    font-weight: 800;
    color: var(--text-color, #0f172a);
    letter-spacing: -0.02em;
}

.backoffice-brand-name i {
    margin-right: 0.65rem;
}

.backoffice-brand-sub {
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--text-muted-custom, #64748b);
    margin-top: 0.1rem;
}

/* --- Corps --- */
.backoffice-auth-body {
    margin-top: 0.5rem;
}

.backoffice-auth-title {
    font-size: 1.5rem;
    font-weight: 700;
    color: var(--text-color, #0f172a);
    margin-bottom: 0.25rem;
}

.backoffice-auth-subtitle {
    font-size: 0.9rem;
    color: var(--text-muted-custom, #64748b);
    margin-bottom: 1.75rem;
}

/* --- Formulaire --- */
.backoffice-auth-form {
    display: flex;
    flex-direction: column;
}

/* --- Alertes --- */
.backoffice-alert {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.75rem 1rem;
    border-radius: 12px;
    font-size: 0.9rem;
    margin-bottom: 1rem;
}

.backoffice-alert i {
    font-size: 1.1rem;
    flex-shrink: 0;
}

.backoffice-alert-danger {
    background-color: #fef2f2;
    border: 1px solid #fecaca;
    color: #dc2626;
}

.backoffice-alert-warning {
    background-color: #fffbeb;
    border: 1px solid #fde68a;
    color: #b45309;
}

/* --- Bouton --- */
.backoffice-login-btn {
    font-weight: 600;
    padding: 0.75rem 1.5rem;
    border-radius: 12px !important;
    transition: all 0.2s ease;
}

.backoffice-login-btn i {
    font-size: 1.1rem;
}

.backoffice-login-btn:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(8, 56, 105, 0.25);
}

/* --- Liens --- */
.backoffice-auth-links {
    margin-top: 0.5rem;
}

.backoffice-forgot-link {
    color: var(--text-muted-custom, #64748b);
    font-size: 0.85rem;
    font-weight: 500;
    text-decoration: none;
    transition: color 0.2s ease;
}

.backoffice-forgot-link:hover {
    color: var(--primary-color, #083869);
    text-decoration: underline;
}

/* --- Footer --- */
.backoffice-auth-footer {
    font-size: 0.75rem;
    color: var(--text-muted-custom, #64748b);
    border-top-color: var(--border-color, #dee2e6) !important;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    flex-wrap: wrap;
}

.backoffice-footer-badge {
    font-size: 0.55rem;
    font-weight: 700;
    background: var(--primary-color, #083869);
    color: white;
    padding: 2px 10px;
    border-radius: 12px;
    letter-spacing: 0.3px;
    text-transform: uppercase;
}

/* ============================================
   RESPONSIVE
   ============================================ */
@media (max-width: 575.98px) {
    .backoffice-auth-card {
        padding: 2rem 1.25rem 1.5rem;
        border-radius: 16px !important;
    }

    .backoffice-auth-title {
        font-size: 1.25rem;
    }

    .backoffice-brand-name {
        font-size: 1.5rem;
    }

    .backoffice-brand-icon {
        width: 52px;
        height: 52px;
        font-size: 1.5rem;
    }

    .backoffice-auth-wrapper {
        padding: 1rem;
    }
}

@media (min-width: 576px) and (max-width: 991.98px) {
    .backoffice-auth-card {
        padding: 2.5rem 2rem;
    }
}

</style>
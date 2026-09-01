<script setup lang="ts">
import { ref } from 'vue'
import { BaseCard, BaseInput, BaseButton } from '@/components/base'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'

// État du formulaire
const email = ref('')
const password = ref('')

// Erreurs des champs
const emailError = ref('')
const passwordError = ref('')

// Erreur générale de connexion
const loginError = ref('')

// État de chargement
const loading = ref(false)

const authStore = useAuthStore()

/**
 * Connexion de l'utilisateur
 */
const handleLogin = async () => {
    emailError.value = ''
    passwordError.value = ''
    loginError.value = ''

    // Validation de l'email
    if (!email.value.trim()) {
        emailError.value = 'L’adresse email est obligatoire'
        return
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
        emailError.value = 'Veuillez saisir une adresse email valide'
        return
    }

    // Validation du mot de passe
    if (!password.value) {
        passwordError.value = 'Le mot de passe est obligatoire'
        return
    }

    try {
        loading.value = true

        // Le composant appelle le STORE
        await authStore.login({
            email: email.value.trim(),
            password: password.value
        })

        console.log('Connexion réussie :', authStore.user)

        // Redirection
        await router.push('/dashboard')

    } catch (error: unknown) {
        console.error('Erreur complète de connexion :', error)
        console.error('Response :', (error as any)?.response)
        console.error('Response data :', (error as any)?.response?.data)

        loginError.value =
            (error as any)?.response?.data?.error ??
            (error as any)?.response?.data?.message ??
            (error instanceof Error ? error.message : 'Erreur de connexion')
    } finally {
        loading.value = false
    }
}
</script>

<template>
    <div class="auth-wrapper">
        <BaseCard class="auth-card" :custom-class="['border-0', 'shadow-lg']">
            <template #default>
                <!-- En-tête -->
                <div class="auth-header text-center">
                    <div class="auth-brand">
                        <span class="brand-name"><i class="bi bi-shield-shaded"></i>DTS</span>
                        <span class="brand-sub d-block">
                            Département Études, Sûreté &amp; Sécurité - TSS
                        </span>
                    </div>
                </div>

                <div id="loginSection" class="mt-4">
                    <h2 class="auth-title text-center">Connexion</h2>

                    <p class="auth-subtitle text-center">
                        Accédez à votre espace de travail
                    </p>

                    <form @submit.prevent="handleLogin">
                        <!-- Champ Email -->
                        <BaseInput v-model="email" label="Adresse email" placeholder="nom.prenom@banky-foibe.mg"
                            type="email" name="email" :required="true" :error="emailError" size="lg"
                            custom-class="mb-3" />

                        <!-- Champ Mot de passe -->
                        <BaseInput v-model="password" label="Mot de passe" placeholder="••••••••" type="password"
                            name="password" :required="true" :error="passwordError" size="lg" custom-class="mb-4" />

                        <!-- Message d'erreur général -->
                        <div v-if="loginError" class="alert alert-danger" role="alert">
                            {{ loginError }}
                        </div>

                        <!-- Bouton de connexion -->
                        <BaseButton type="submit" variant="primary" size="lg" :loading="loading" :disabled="loading"
                            custom-class="w-100">
                            {{ loading ? 'Connexion en cours...' : 'Se connecter' }}
                        </BaseButton>
                    </form>

                    <!-- Lien mot de passe oublié -->
                    <div class="auth-links text-center mt-3">
                        <RouterLink to="/forgot-password" class="forgot-link">
                            Mot de passe oublié ?
                        </RouterLink>
                    </div>
                </div>

                <!-- Pied de page -->
                <div class="auth-footer text-center mt-4 pt-3 border-top">
                    &copy; 2026 &middot; Département Études, Sûreté et Sécurité
                </div>
            </template>
        </BaseCard>
    </div>
</template>



<style scoped>
body {
    padding-top: 0 !important;
    /* Supprime le padding-top global pour la page de connexion */
}

/* Style global de la page d'authentification */
.auth-wrapper {
    position: fixed;
    inset: 0;
    width: 100%;
    height: 100vh;

    display: flex;
    align-items: center;
    justify-content: center;

    background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);

    padding: 1.5rem;
    overflow-y: auto;
}

/* Carte de connexion */
.auth-card {
    max-width: 580px;
    width: 100%;
    padding: 2.5rem 2rem 2rem;
    border-radius: 24px !important;
    background: #ffffff;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
    transition: transform 0.2s ease;
}

.auth-brand {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    margin-bottom: 0.5rem;
}

.brand-name {
    font-size: 2rem;
    font-weight: 800;
    color: #1a2a6c;
    letter-spacing: -0.02em;
}

.brand-name i {
    margin-right: 0.65rem;
}

.brand-sub {
    font-size: 0.75rem;
    font-weight: 500;
    color: #6b7280;
    margin-top: 0.1rem;
    line-height: 1.3;
}

/* Titres de la section connexion */
.auth-title {
    font-size: 1.75rem;
    font-weight: 700;
    color: #1a2a6c;
    margin-bottom: 0.25rem;
}

.auth-subtitle {
    font-size: 0.95rem;
    color: #6b7280;
    margin-bottom: 2rem;
}

/* Lien mot de passe oublié */
.forgot-link {
    color: #1a2a6c;
    font-size: 0.9rem;
    font-weight: 500;
    text-decoration: none;
    transition: color 0.2s ease;
}

.forgot-link:hover {
    color: #4a6cf7;
    text-decoration: underline;
}

/* Pied de page */
.auth-footer {
    font-size: 0.8rem;
    color: #9ca3af;
}

/* Messages d'erreur */
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

/* Responsive */
@media (max-width: 480px) {
    .auth-card {
        padding: 2rem 1.25rem 1.5rem;
    }

    .auth-title {
        font-size: 1.5rem;
    }

    .brand-name {
        font-size: 1.75rem;
    }

    .shield-icon-wrapper {
        width: 44px;
        height: 44px;
        font-size: 1.4rem;
    }
}

/* Support Dark mode si nécessaire */
@media (prefers-color-scheme: dark) {
    .auth-wrapper {
        background: linear-gradient(135deg, #111827 0%, #1f2937 100%);
    }

    .auth-card {
        background: #1f2937;
        border: 1px solid #374151 !important;
    }

    .brand-name {
        color: #f0f2f5;
    }

    .brand-sub,
    .brand-tagline,
    .auth-subtitle {
        color: #9ca3af;
    }

    .auth-title {
        color: #f0f2f5;
    }

    .forgot-link {
        color: #9ca3af;
    }

    .forgot-link:hover {
        color: #4a6cf7;
    }

    .auth-footer {
        color: #6b7280;
        border-top-color: #374151 !important;
    }

    .alert-danger {
        background-color: #2d1b1b;
        border-color: #5c2626;
        color: #fca5a5;
    }
}
</style>
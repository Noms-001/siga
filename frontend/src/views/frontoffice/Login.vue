<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
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

const route = useRoute()

/**
 * Session fermée pour inactivité : le watchdog redirige ici avec
 * `?session=expiree`. Sans ce message, l'utilisateur landing sur le login
 * sans avoir cliqué sur "Déconnexion" croirait à une erreur de sa part.
 */
const sessionExpiree = computed(() => route.query.session === 'expiree')

/**
 * Retour à la page précédente, ou à l'accueil si pas d'historique.
 */
const goBack = () => {
    router.push('/').catch(() => {
        // Navigation annulée par un guard — on ne fait rien
    })
}

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

        // Reprendre la page demandée avant la connexion, notamment le lien
        // direct vers la file de validation reçu par e-mail.
        const redirect = route.query.redirect
        const destination = typeof redirect === 'string'
            && redirect.startsWith('/')
            && !redirect.startsWith('//')
            ? redirect
            : '/dashboard'
        await router.replace(destination)

    } catch (error: unknown) {

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
        <button type="button" class="auth-back-btn" @click="goBack" title="Retour" aria-label="Retour">
            <i class="bi bi-arrow-left"></i>
        </button>

        <BaseCard class="auth-card" :custom-class="['border-0', 'shadow-lg']">
            <template #default>

                <!-- En-tête : logo + titre -->
                <header class="auth-header">
                    <div class="auth-logo-ring">
                        <img src="/logo/logo.png" alt="Logo" class="brand-logo">
                    </div>

                    <img src="/logo/titre.png" alt="GESTIA" class="brand-title-img">

                    <span class="auth-divider" aria-hidden="true"></span>
                </header>

                <div id="loginSection">
                    <h2 class="auth-title text-center">Connexion</h2>

                    <p class="auth-subtitle text-center">
                        Accédez à votre espace de travail
                    </p>

                    <!-- Session fermée pour inactivité -->
                    <div v-if="sessionExpiree" class="alert alert-warning" role="alert">
                        Votre session a été fermée après une période d’inactivité.
                        Veuillez vous reconnecter.
                    </div>

                    <form @submit.prevent="handleLogin">
                        <!-- Champ Email -->
                        <BaseInput v-model="email" label="Adresse email" placeholder="nom.prenom@banky-foibe.mg"
                            type="email" name="email" :required="true" :error="emailError" size="sm"
                            custom-class="mb-3" />

                        <!-- Champ Mot de passe -->
                        <BaseInput v-model="password" label="Mot de passe" placeholder="••••••••" type="password"
                            name="password" :required="true" :error="passwordError" size="md" custom-class="mb-4" />

                        <!-- Message d'erreur général -->
                        <div v-if="loginError" class="alert alert-danger" role="alert">
                            {{ loginError }}
                        </div>

                        <!-- Bouton de connexion -->
                        <BaseButton type="submit" variant="primary" size="md" :loading="loading" :disabled="loading"
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
    overflow: scroll;
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

/* ============ Section connexion ============ */
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
    font-size: 0.75rem;
    color: #9ca3af;
}

/* Messages */
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

.alert-warning {
    background-color: #fffbeb;
    border-color: #fde68a;
    color: #b45309;
}

/* ============ Bouton retour ============ */
.auth-back-btn {
    position: fixed;
    top: 1.5rem;
    left: 1.5rem;
    z-index: 10;

    display: inline-flex;
    align-items: center;
    justify-content: center;

    width: 42px;
    height: 42px;

    background-color: #1a2a6c;
    color: #ffffff;

    border: 1px solid #1a2a6c;
    border-radius: 50%;

    font-size: 1.15rem;
    line-height: 1;
    cursor: pointer;

    transition:
        color 0.2s ease,
        border-color 0.2s ease,
        box-shadow 0.2s ease,
        transform 0.2s ease,
        background 0.2s ease;
}

.auth-back-btn:hover {
    color: white;
    border-color: white;
    box-shadow:
        0 0 5px 3px rgba(212, 175, 55, 0.25),
        0 6px 16px rgba(212, 175, 55, 0.35);
    transform: translateY(-1px);
    background: #d4af37;
}

.auth-back-btn:active {
    transform: translateY(0);
}

.auth-back-btn:focus-visible {
    outline: 2px solid #d4af37;
    outline-offset: 2px;
}

/* ============ Responsive ============ */
@media (max-width: 480px) {
    .auth-wrapper {
        padding: 0.75rem;
    }

    .auth-back-btn {
        top: 1rem;
        left: 1rem;
        width: 38px;
        height: 38px;
        font-size: 1rem;
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
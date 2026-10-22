<template>
    <div class="auth-wrapper">
        <button type="button" class="auth-back-btn" @click="goBack" title="Retour" aria-label="Retour">
            <i class="bi bi-arrow-left"></i>
        </button>

        <div class="auth-card">
            <!-- ============================================================
                 HERO — bloc marque avec gradient navy → bleu
                 ============================================================ -->
            <header class="auth-hero">
                <span class="auth-hero__circle auth-hero__circle--1" aria-hidden="true"></span>
                <span class="auth-hero__circle auth-hero__circle--2" aria-hidden="true"></span>

                <div class="auth-logo-ring">
                    <img src="/favicon.png" alt="Logo GESTIA" class="brand-logo">
                </div>

                <img src="/logo/titre.png" alt="GESTIA" class="brand-title-img">

                <span class="auth-divider" aria-hidden="true"></span>

                <span class="auth-hero__eyebrow">Backoffice · Administration</span>
            </header>

            <!-- ============================================================
                 PANNEAU FORMULAIRE
                 ============================================================ -->
            <section class="auth-panel">
                <h2 class="auth-title">Connexion</h2>

                <p class="auth-subtitle">
                    Accédez à l'interface d'administration
                </p>

                <!-- Session fermée pour inactivité -->
                <div v-if="sessionExpiree" class="alert alert--warning" role="alert">
                    <i class="bi bi-clock-history"></i>
                    <span>
                        Votre session a été fermée après une période d'inactivité.
                        Veuillez vous reconnecter.
                    </span>
                </div>

                <form @submit.prevent="handleLogin">
                    <!-- Champ Email -->
                    <BaseInput
                        v-model="email"
                        label="Adresse email"
                        placeholder="admin@dts.mg"
                        type="email"
                        name="email"
                        :required="true"
                        :error="emailError"
                        size="md"
                        custom-class="mb-3"
                    />

                    <!-- Champ Mot de passe -->
                    <BaseInput
                        v-model="password"
                        label="Mot de passe"
                        placeholder="••••••••"
                        type="password"
                        name="password"
                        :required="true"
                        :error="passwordError"
                        size="md"
                        custom-class="mb-3"
                    />

                    <!-- Lien mot de passe oublié -->
                    <div class="auth-links">
                        <RouterLink to="/forgot-password" class="forgot-link">
                            Mot de passe oublié ?
                        </RouterLink>
                    </div>

                    <!-- Message d'erreur général -->
                    <div v-if="loginError" class="alert alert--danger" role="alert">
                        <i class="bi bi-exclamation-triangle-fill"></i>
                        <span>{{ loginError }}</span>
                    </div>

                    <!-- Bouton de connexion -->
                    <button
                        type="submit"
                        class="auth-submit"
                        :disabled="loading"
                    >
                        <i v-if="!loading" class="bi bi-box-arrow-in-right"></i>
                        <i v-else class="bi bi-arrow-repeat spin"></i>
                        {{ loading ? 'Connexion en cours…' : 'Se connecter' }}
                    </button>
                </form>

                <!-- Pied de page -->
                <footer class="auth-footer">
                    <span class="auth-footer__copy">
                        &copy; {{ currentYear }} · Département Études, Sûreté et Sécurité
                    </span>
                </footer>
            </section>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseInput } from '@/components/base'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const emailError = ref('')
const passwordError = ref('')
const loginError = ref('')
const loading = ref(false)

const currentYear = computed(() => new Date().getFullYear())

const sessionExpiree = computed(() => route.query.session === 'expiree')

const goBack = () => {
    router.push('/').catch(() => {})
}

const handleLogin = async () => {
    emailError.value = ''
    passwordError.value = ''
    loginError.value = ''

    if (!email.value.trim()) {
        emailError.value = 'L’adresse email est obligatoire'
        return
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
        emailError.value = 'Veuillez saisir une adresse email valide'
        return
    }

    if (!password.value) {
        passwordError.value = 'Le mot de passe est obligatoire'
        return
    }

    try {
        loading.value = true

        await authStore.login({
            email: email.value.trim(),
            password: password.value,
        }, true)

        if (authStore.isAuthenticated) {
            await router.replace('/backoffice/dashboard')
        } else {
            loginError.value = 'Une erreur est survenue lors de la connexion'
        }
    } catch (error: unknown) {
        const err = error as any
        loginError.value =
            err?.response?.data?.error ??
            err?.response?.data?.message ??
            (error instanceof Error ? error.message : 'Erreur de connexion')
    } finally {
        loading.value = false
    }
}
</script>

<style scoped>
/* ============================================================
   TOKENS
   ============================================================ */
.auth-wrapper {
    --ink: #15233c;
    --navy: #1a2a6c;
    --navy-2: #22358a;
    --blue: #4a6cf7;
    --blue-soft: #eaf0ff;
    --gold: #d4af37;
    --line: #e6ebf3;
    --soft: #8a95a8;
    --muted: #6b7280;

    position: fixed;
    inset: 0;
    width: 100%;
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 1.5rem;
    overflow-y: auto;

    background:
        radial-gradient(circle at 12% 10%, rgba(74, 108, 247, 0.09) 0, transparent 42%),
        radial-gradient(circle at 90% 92%, rgba(212, 175, 55, 0.10) 0, transparent 40%),
        linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);

    font-family: "DM Sans", "Segoe UI", sans-serif;
    color: var(--ink);
}

/* ============================================================
   CARTE
   ============================================================ */
.auth-card {
    position: relative;
    width: 100%;
    max-width: 460px;
    background: #fff;
    border-radius: 22px;
    overflow: hidden;
    box-shadow:
        0 4px 12px rgba(26, 42, 108, .06),
        0 24px 60px rgba(26, 42, 108, .14);
    flex-shrink: 0;
}

/* ============================================================
   HERO — gradient navy → bleu (bleu en bas)
   ============================================================ */
.auth-hero {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 1rem 2rem 2.75rem;
    color: #fff;
    isolation: isolate;
    overflow: hidden;

    /* Gradient : dark navy en haut → navy → bleu vif en bas */
    background: var(--ink);
}

/* Liseré doré à la limite hero/panel */
.auth-hero::after {
    content: "";
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 3px;
    background: linear-gradient(90deg, #1a2a6c 0%, #4a6cf7 50%, #d4af37 100%);
}

/* Décorations circulaires */
.auth-hero__circle {
    position: absolute;
    border-radius: 50%;
    border: 1px solid rgba(255, 255, 255, .1);
    pointer-events: none;
    z-index: 0;
}
.auth-hero__circle--1 {
    width: 260px;
    height: 260px;
    top: -110px;
    right: -90px;
    background: radial-gradient(circle, rgba(74, 108, 247, .35), transparent 70%);
}
.auth-hero__circle--2 {
    width: 140px;
    height: 140px;
    bottom: -60px;
    left: -50px;
    background: radial-gradient(circle, rgba(212, 175, 55, .22), transparent 70%);
}

/* Logo dans un médaillon verre dépoli */
.auth-logo-ring {
    display: grid;
    place-items: center;
    width: 88px;
    height: 88px;
    padding: 8px;
    margin-bottom: 0.85rem;
    border-radius: 50%;
    background: rgba(255, 255, 255, .1);
    border: 1px solid rgba(255, 255, 255, .22);
    backdrop-filter: blur(6px);
    -webkit-backdrop-filter: blur(6px);
    box-shadow:
        0 8px 24px rgba(0, 0, 0, .22),
        inset 0 0 16px rgba(74, 108, 247, .15);
    position: relative;
    z-index: 1;
}

.brand-logo {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: contain;
}

.brand-title-img {
    display: block;
    width: 100%;
    max-width: 200px;
    max-height: 46px;
    height: auto;
    object-fit: contain;
    filter: brightness(0) invert(1);
    margin-bottom: 0.85rem;
    position: relative;
    z-index: 1;
}

/* Divider doré */
.auth-divider {
    display: block;
    width: 140px;
    height: 2px;
    border-radius: 2px;
    background: linear-gradient(90deg, transparent, var(--gold), transparent);
    margin-bottom: 0.85rem;
    position: relative;
    z-index: 1;
}

/* Eyebrow */
.auth-hero__eyebrow {
    display: block;
    font-size: 0.68rem;
    font-weight: 700;
    letter-spacing: 0.14em;
    text-transform: uppercase;
    color: rgba(255, 255, 255, .78);
    position: relative;
    z-index: 1;
}

/* ============================================================
   PANNEAU FORMULAIRE
   ============================================================ */
.auth-panel {
    padding: 2rem 2.25rem 1.5rem;
    background: #fff;
}

/* ============================================================
   TITRES
   ============================================================ */
.auth-title {
    margin: 0 0 0.35rem;
    font-family: "Manrope", "DM Sans", sans-serif;
    font-size: 1.6rem;
    font-weight: 800;
    letter-spacing: -0.02em;
    line-height: 1.15;
    color: var(--navy);
    text-align: center;
}

.auth-subtitle {
    margin: 0 0 1.5rem;
    font-size: 0.85rem;
    color: var(--muted);
    line-height: 1.5;
    text-align: center;
}

/* ============================================================
   LIEN MOT DE PASSE OUBLIÉ
   ============================================================ */
.auth-links {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 1rem;
}

.forgot-link {
    color: var(--blue);
    font-size: 0.82rem;
    font-weight: 600;
    text-decoration: none;
    transition: color .15s ease;
}

.forgot-link:hover {
    color: var(--navy);
    text-decoration: underline;
}

/* ============================================================
   BOUTON SUBMIT
   ============================================================ */
.auth-submit {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.55rem;
    width: 100%;
    height: 46px;
    padding: 0 1.25rem;

    background: linear-gradient(135deg, var(--navy) 0%, var(--blue) 100%);
    color: #fff;

    border: none;
    border-radius: 12px;

    font-family: inherit;
    font-size: 0.9rem;
    font-weight: 700;
    letter-spacing: 0.01em;

    cursor: pointer;
    box-shadow: 0 6px 18px rgba(26, 42, 108, .25);
    transition: transform .15s ease, box-shadow .15s ease, filter .15s ease;
}

.auth-submit:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 10px 26px rgba(26, 42, 108, .32);
    filter: brightness(1.04);
}

.auth-submit:active:not(:disabled) {
    transform: translateY(0);
}

.auth-submit:disabled {
    cursor: wait;
    opacity: 0.75;
}

.auth-submit i {
    font-size: 1rem;
}

/* ============================================================
   ALERTES
   ============================================================ */
.alert {
    display: flex;
    align-items: flex-start;
    gap: 0.55rem;
    padding: 0.75rem 0.9rem;
    border-radius: 10px;
    font-size: 0.82rem;
    line-height: 1.45;
    margin-bottom: 1rem;
    border: 1px solid transparent;
}

.alert i {
    flex: none;
    font-size: 0.95rem;
    margin-top: 0.05rem;
}

.alert--warning {
    background: #fffbeb;
    border-color: #fde68a;
    color: #b45309;
}
.alert--warning i { color: #b45309; }

.alert--danger {
    background: #fef2f2;
    border-color: #fecaca;
    color: #b91c1c;
}
.alert--danger i { color: #b91c1c; }

/* ============================================================
   PIED DE PAGE
   ============================================================ */
.auth-footer {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.35rem;
    margin-top: 1.5rem;
    padding-top: 1rem;
    border-top: 1px solid var(--line);
    text-align: center;
}

.auth-footer__secure {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    font-size: 0.68rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: var(--blue);
}
.auth-footer__secure i {
    font-size: 0.62rem;
    color: var(--gold);
}

.auth-footer__copy {
    font-size: 0.7rem;
    color: var(--soft);
    letter-spacing: 0.01em;
}

/* ============================================================
   BOUTON RETOUR
   ============================================================ */
.auth-back-btn {
    position: fixed;
    top: 1.5rem;
    left: 1.5rem;
    z-index: 20;

    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 42px;
    height: 42px;

    background: #fff;
    color: var(--navy);
    border: 1px solid var(--line);
    border-radius: 50%;
    font-size: 1.1rem;
    cursor: pointer;

    box-shadow: 0 4px 12px rgba(26, 42, 108, .08);
    transition:
        background .2s ease,
        color .2s ease,
        border-color .2s ease,
        box-shadow .2s ease,
        transform .2s ease;
}

.auth-back-btn:hover {
    background: var(--navy);
    color: #fff;
    border-color: var(--navy);
    transform: translateY(-1px);
    box-shadow: 0 8px 20px rgba(26, 42, 108, .22);
}

.auth-back-btn:active { transform: translateY(0); }

.auth-back-btn:focus-visible {
    outline: 2px solid var(--gold);
    outline-offset: 2px;
}

/* ============================================================
   ANIMATIONS
   ============================================================ */
.spin {
    animation: spin 1s linear infinite;
}
@keyframes spin {
    to { transform: rotate(360deg); }
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 480px) {
    .auth-wrapper {
        padding: 0.75rem;
    }

    .auth-back-btn {
        top: 0.9rem;
        left: 0.9rem;
        width: 36px;
        height: 36px;
        font-size: 0.95rem;
    }

    .auth-card {
        border-radius: 18px;
    }

    .auth-hero {
        padding: 1.75rem 1.35rem 2rem;
    }

    .auth-logo-ring {
        width: 72px;
        height: 72px;
        padding: 7px;
    }

    .brand-title-img {
        max-width: 170px;
        max-height: 40px;
    }

    .auth-hero__circle--1 {
        width: 180px;
        height: 180px;
        top: -80px;
        right: -70px;
    }

    .auth-panel {
        padding: 1.5rem 1.35rem 1.25rem;
    }

    .auth-title {
        font-size: 1.4rem;
    }

    .auth-subtitle {
        font-size: 0.8rem;
        margin-bottom: 1.25rem;
    }

    .auth-submit {
        height: 44px;
        font-size: 0.85rem;
    }
}

@media (prefers-reduced-motion: reduce) {
    .auth-back-btn,
    .auth-submit,
    .forgot-link,
    .spin {
        transition: none !important;
        animation: none !important;
    }
}
</style>
<template>
    <div class="accueil-page">
        <div class="accueil-container">
            <!-- En-tête -->
            <div class="accueil-header section text-center">
                <div class="accueil-brand">
                    <div class="auth-logo-ring">
                        <img src="/logo/logo.png" alt="Logo" class="brand-logo">
                    </div>

                    <img src="/logo/titre.png" alt="GESTIA" class="brand-title-img">

                    <span class="auth-divider" aria-hidden="true"></span>

                    <h1 class="accueil-title">Gestion Intelligente et Automatisée des Activités</h1>
                    <p class="accueil-subtitle">
                        Bienvenue sur la plateforme de gestion. Veuillez choisir votre espace d'accès.
                    </p>
                </div>
            </div>

            <!-- Cartes de choix -->
            <div class="accueil-grid">
                <!-- Espace utilisateur -->
                <BaseCard class="accueil-card">
                    <div class="accueil-card-content">
                        <div class="accueil-card-icon user-icon">
                            <i class="bi bi-person"></i>
                        </div>
                        <h2 class="accueil-card-title">Espace utilisateur</h2>
                        <p class="accueil-card-description">
                            Accédez à votre espace personnel pour consulter vos activités,
                            vos indicateurs et vos notifications.
                        </p>
                        <router-link to="/login" class="accueil-card-link">
                            <BaseButton variant="primary" size="lg" custom-class="accueil-btn w-100">
                                <i class="bi bi-box-arrow-in-right"></i>
                                Accéder à l'espace utilisateur
                            </BaseButton>
                        </router-link>
                    </div>
                </BaseCard>

                <!-- Espace administrateur -->
                <BaseCard class="accueil-card admin-card">
                    <div class="accueil-card-content">
                        <div class="accueil-card-icon admin-icon">
                            <i class="bi bi-shield-lock"></i>
                        </div>
                        <h2 class="accueil-card-title">Espace administrateur</h2>
                        <p class="accueil-card-description">
                            Gérez les utilisateurs, les permissions, les référentiels et
                            administrez l'ensemble de la plateforme.
                        </p>
                        <router-link to="/backoffice/login" class="accueil-card-link">
                            <BaseButton variant="secondary" size="lg" custom-class="accueil-btn w-100">
                                <i class="bi bi-shield-lock"></i>
                                Accéder au backoffice
                            </BaseButton>
                        </router-link>
                    </div>
                </BaseCard>
            </div>

            <!-- Footer -->
            <div class="accueil-footer text-center">
                <span class="accueil-footer-text">
                    &copy; {{ currentYear }} DTS — Département Études, Sûreté et Sécurité
                </span>
                <span class="accueil-footer-badge">v{{ appVersion }}</span>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { BaseCard, BaseButton } from '@/components/base'

// --- Props ---
const props = defineProps<{
    appVersion?: string
}>()

// --- Computed ---
const currentYear = computed(() => new Date().getFullYear())
const appVersion = computed(() => props.appVersion || '1.0.0')
</script>

<style scoped>
/* ============================================
   ACCUEIL - Styles (alignés sur les pages auth)
   ============================================ */

.accueil-page {
    position: fixed;
    inset: 0;
    width: 100%;
    height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 2rem 1.5rem;
    overflow-y: auto;

    background:
        radial-gradient(circle at 12% 10%, rgba(26, 42, 108, 0.10) 0, transparent 38%),
        radial-gradient(circle at 90% 92%, rgba(212, 175, 55, 0.14) 0, transparent 36%),
        linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
}

.accueil-container {
    width: 100%;
    max-width: 1100px;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

/* --- En-tête --- */
.accueil-header {
    margin-bottom: 0.5rem;
}

.accueil-brand {
    display: flex;
    flex-direction: column;
    align-items: center;
}

/* Logo dans un médaillon (identique au login) */
.auth-logo-ring {
    display: flex;
    align-items: center;
    justify-content: center;

    width: 90px;
    height: 90px;
    padding: 8px;
}

.brand-logo {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: contain;
}

/* Titre : largeur maîtrisée, fond blanc du JPEG fondu dans la page */
.brand-title-img {
    display: block;
    width: 100%;
    max-width: 260px;
    max-height: 58px;
    height: auto;
    object-fit: contain;
    mix-blend-mode: multiply;
}

/* Séparateur doré sous le titre */
.auth-divider {
    display: block;
    width: 200px;
    height: 3px;
    border-radius: 3px;
    margin: 0.5rem 0 0.75rem;
    background: linear-gradient(90deg, #1a2a6c, #d4af37);
}

.accueil-title {
    font-size: 1.5rem;
    font-weight: 700;
    color: #1a2a6c;
    margin: 0;
    letter-spacing: -0.02em;
    max-width: 720px;
}

.accueil-subtitle {
    font-size: 0.85rem;
    color: #6b7280;
    margin: 0.35rem 0 0;
}

/* --- Grille des cartes --- */
.accueil-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1.5rem;
}

/* --- Cartes --- */
.accueil-card {
    padding: 1rem 1.75rem;
    border-radius: 22px !important;
    background: #fff;
    border: 1px solid #e5e7eb !important;
    box-shadow: 0 20px 60px rgba(26, 42, 108, 0.08);
    transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.accueil-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 20px 60px rgba(26, 42, 108, 0.14);
}

.accueil-card.admin-card:hover {
    box-shadow: 0 20px 60px rgba(212, 175, 55, 0.18);
}

.accueil-card-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 1rem;
}

.accueil-card-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 64px;
    height: 64px;
    border-radius: 16px;
    font-size: 2rem;
    flex-shrink: 0;
}

.user-icon {
    background: rgba(26, 42, 108, 0.08);
    color: #1a2a6c;
}

.admin-icon {
    background: rgba(212, 175, 55, 0.15);
    color: #b45309;
}

.accueil-card-title {
    font-size: 1.25rem;
    font-weight: 700;
    color: #1a2a6c;
    margin: 0;
}

.accueil-card-description {
    font-size: 0.9rem;
    color: #6b7280;
    margin: 0;
    line-height: 1.5;
    max-width: 320px;
}

.accueil-card-link {
    width: 100%;
    text-decoration: none;
    margin-top: 0.5rem;
}

.accueil-btn {
    font-weight: 600;
    padding: 0.75rem 1.5rem;
    border-radius: 12px !important;
    transition: all 0.2s ease;
    gap: 0.5rem;
}

.accueil-btn i {
    font-size: 1.1rem;
}

.accueil-btn:hover:not(:disabled) {
    transform: translateY(-2px);
}

/* Variante secondaire (backoffice) */
.accueil-card .base-btn--secondary {
    background: #1a2a6c;
    color: white;
    border: none;
}

.accueil-card .base-btn--secondary:hover:not(:disabled) {
    background: #4a6cf7;
    box-shadow: 0 8px 24px rgba(26, 42, 108, 0.25);
}

/* --- Footer --- */
.accueil-footer {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.75rem;
    flex-wrap: wrap;
    padding-top: 0.75rem;
    border-top: 1px solid #e5e7eb;
}

.accueil-footer-text {
    font-size: 0.8rem;
    color: #9ca3af;
}

.accueil-footer-badge {
    font-size: 0.6rem;
    font-weight: 600;
    background: #e5e7eb;
    color: #6b7280;
    padding: 2px 10px;
    border-radius: 12px;
    letter-spacing: 0.3px;
}

/* ============================================
   RESPONSIVE
   ============================================ */
@media (max-width: 768px) {
    .accueil-grid {
        grid-template-columns: 1fr;
        gap: 1rem;
    }

    .accueil-title {
        font-size: 1.25rem;
    }

    .auth-logo-ring {
        width: 96px;
        height: 96px;
        padding: 7px;
    }

    .brand-title-img {
        max-width: 220px;
        max-height: 50px;
    }

    .auth-divider {
        width: 160px;
    }

    .accueil-card {
        padding: 1.5rem 1.25rem;
    }

    .accueil-card-icon {
        width: 52px;
        height: 52px;
        font-size: 1.5rem;
    }

    .accueil-card-title {
        font-size: 1.1rem;
    }
}

@media (max-width: 480px) {
    .accueil-page {
        padding: 1rem 0.75rem;
    }

    .accueil-container {
        gap: 1.5rem;
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

    .auth-divider {
        width: 140px;
    }

    .accueil-title {
        font-size: 1.15rem;
    }

    .accueil-subtitle {
        font-size: 0.85rem;
    }

    .accueil-card {
        padding: 1.25rem 1rem;
    }

    .accueil-btn {
        font-size: 0.85rem !important;
        padding: 0.6rem 1rem !important;
    }
}
</style>
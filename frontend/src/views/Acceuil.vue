<template>
    <div class="accueil-page">
        <div class="accueil-container">
            <!-- En-tête -->
            <div class="accueil-header text-center">
                <div class="accueil-brand">
                    <div class="accueil-brand-icon">
                        <i class="bi bi-shield-fill-check"></i>
                    </div>
                    <h1 class="accueil-title">Système d'Alerte et de Gestion des Activités</h1>
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
   ACCUEIL - Styles
   ============================================ */

.accueil-page {
    position: fixed;
    inset: 0;
    width: 100%;
    height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: radial-gradient(circle at top, rgba(8, 56, 105, 0.06), transparent 28%),
        var(--bg-color, #eef2f8f4);
    padding: 2rem 1.5rem;
    overflow-y: auto;
}

.accueil-container {
    width: 100%;
    max-width: 1100px;
    display: flex;
    flex-direction: column;
    gap: 2.5rem;
}

/* --- En-tête --- */
.accueil-header {
    margin-bottom: 0.5rem;
}

.accueil-brand {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.5rem;
}

.accueil-brand-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 72px;
    height: 72px;
    border-radius: 20px;
    background: linear-gradient(135deg, var(--primary-color, #083869), var(--secondary-color, #164493));
    color: white;
    font-size: 2.5rem;
    box-shadow: 0 4px 20px rgba(8, 56, 105, 0.3);
    margin-bottom: 0.5rem;
}

.accueil-title {
    font-size: 2rem;
    font-weight: 700;
    color: var(--text-color, #0f172a);
    margin: 0;
    letter-spacing: -0.02em;
}

.accueil-subtitle {
    font-size: 1rem;
    color: var(--text-muted-custom, #64748b);
    margin: 0.25rem 0 0;
}

/* --- Grille des cartes --- */
.accueil-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1.5rem;
}

/* --- Cartes --- */
.accueil-card {
    padding: 2rem 1.75rem;
    border-radius: 20px !important;
    background: var(--card-bg, #ffffff);
    border: 1px solid var(--border-color, #dee2e6) !important;
    box-shadow: var(--shadow-sm, 0 2px 4px rgba(0, 0, 0, 0.05));
    transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.accueil-card:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-lg, 0 20px 60px rgba(15, 23, 42, 0.12));
}

.accueil-card.admin-card:hover {
    box-shadow: 0 20px 60px rgba(8, 56, 105, 0.18);
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
    background: rgba(8, 56, 105, 0.08);
    color: var(--primary-color, #083869);
}

.admin-icon {
    background: rgba(220, 38, 38, 0.08);
    color: #dc2626;
}

.accueil-card-title {
    font-size: 1.25rem;
    font-weight: 700;
    color: var(--text-color, #0f172a);
    margin: 0;
}

.accueil-card-description {
    font-size: 0.9rem;
    color: var(--text-muted-custom, #64748b);
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
    background: var(--primary-color, #083869);
    color: white;
    border: none;
}

.accueil-card .base-btn--secondary:hover:not(:disabled) {
    background: var(--secondary-color, #164493);
    box-shadow: 0 8px 24px rgba(8, 56, 105, 0.25);
}

/* --- Footer --- */
.accueil-footer {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.75rem;
    flex-wrap: wrap;
    padding-top: 0.5rem;
    border-top: 1px solid var(--border-color, #dee2e6);
    margin-top: 0.5rem;
}

.accueil-footer-text {
    font-size: 0.8rem;
    color: var(--text-muted-custom, #64748b);
}

.accueil-footer-badge {
    font-size: 0.6rem;
    font-weight: 600;
    background: var(--border-color, #dee2e6);
    color: var(--text-muted-custom, #64748b);
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
        font-size: 1.5rem;
    }

    .accueil-brand-icon {
        width: 56px;
        height: 56px;
        font-size: 2rem;
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
        padding: 1rem;
    }

    .accueil-container {
        gap: 1.5rem;
    }

    .accueil-title {
        font-size: 1.25rem;
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
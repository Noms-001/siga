<template>
    <div class="profile-page">
        <!-- En-tête -->
        <div class="profile-header mb-4">
            <div class="d-flex flex-wrap justify-content-between align-items-start gap-3">
                <div>
                    <h1 class="page-title">Mon profil</h1>
                    <p class="page-subtitle text-muted">
                        Consultez et gérez vos informations personnelles
                    </p>
                </div>
                <div class="d-flex gap-2 flex-wrap">
                    <BaseButton variant="secondary" @click="handleEditProfile">
                        <i class="bi bi-pencil"></i>
                        Modifier mes informations
                    </BaseButton>
                    <BaseButton variant="primary" @click="handleChangePassword">
                        <i class="bi bi-key"></i>
                        Modifier mon mot de passe
                    </BaseButton>
                </div>
            </div>
        </div>

        <!-- Loading state -->
        <div v-if="loading" class="profile-loading">
            <div class="skeleton skeleton-title w-50"></div>
            <div class="skeleton skeleton-text w-75"></div>
            <div class="skeleton skeleton-text w-50"></div>
        </div>

        <!-- Contenu -->
        <div v-else-if="user" class="row g-4">
            <!-- Carte Avatar -->
            <div class="col-lg-4">
                <BaseCard class="profile-avatar-card" :full-height="true">
                    <div class="text-center p-3">
                        <div class="avatar-circle mx-auto mb-3">
                            <span class="avatar-initials">{{ userInitials }}</span>
                        </div>
                        <h2 class="avatar-name h5 mb-1">{{ userFullName }}</h2>
                        <p class="avatar-role text-muted mb-3">
                            {{ user.poste || 'Poste non défini' }}
                        </p>
                        <div class="avatar-status d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill">
                            <span class="status-dot active"></span>
                            <span class="status-text small fw-medium">Compte actif</span>
                        </div>
                    </div>
                </BaseCard>
            </div>

            <!-- Informations personnelles -->
            <div class="col-lg-8">
                <BaseCard class="profile-info-card">
                    <template #title>
                        <div class="d-flex align-items-center gap-2 border-bottom pb-2 mb-3">
                            <i class="bi bi-person text-primary"></i>
                            <h3 class="card-title h6 mb-0">Informations personnelles</h3>
                        </div>
                    </template>

                    <div class="row g-3">
                        <div class="col-md-6">
                            <div class="info-item">
                                <label class="info-label text-uppercase text-muted small fw-semibold">Nom</label>
                                <p class="info-value mb-0">{{ user.nom || 'Non renseigné' }}</p>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="info-item">
                                <label class="info-label text-uppercase text-muted small fw-semibold">Prénom</label>
                                <p class="info-value mb-0">{{ user.prenom || 'Non renseigné' }}</p>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="info-item">
                                <label class="info-label text-uppercase text-muted small fw-semibold">Email</label>
                                <p class="info-value mb-0">{{ user.email || 'Non renseigné' }}</p>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="info-item">
                                <label class="info-label text-uppercase text-muted small fw-semibold">Téléphone</label>
                                <p class="info-value mb-0">{{ user.telephone || 'Non renseigné' }}</p>
                            </div>
                        </div>
                    </div>

                    <div class="profile-actions mt-3 pt-3 border-top">
                        <BaseButton variant="secondary" class="w-100" @click="handleEditProfile">
                            <i class="bi bi-pencil"></i>
                            Modifier mes informations
                        </BaseButton>
                    </div>
                </BaseCard>

                <!-- Informations professionnelles -->
                <BaseCard class="profile-info-card mt-4">
                    <template #title>
                        <div class="d-flex align-items-center gap-2 border-bottom pb-2 mb-3">
                            <i class="bi bi-briefcase text-primary"></i>
                            <h3 class="card-title h6 mb-0">Informations professionnelles</h3>
                        </div>
                    </template>

                    <div class="row g-3">
                        <div class="col-md-6">
                            <div class="info-item">
                                <label class="info-label text-uppercase text-muted small fw-semibold">Poste</label>
                                <p class="info-value mb-0">{{ user.poste || 'Non défini' }}</p>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="info-item">
                                <label class="info-label text-uppercase text-muted small fw-semibold">Service</label>
                                <p class="info-value mb-0">{{ user.service || 'Non défini' }}</p>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="info-item">
                                <label class="info-label text-uppercase text-muted small fw-semibold">Statut du
                                    compte</label>
                                <div class="info-status d-flex align-items-center gap-2">
                                    <span class="status-dot active"></span>
                                    <span class="status-text">Actif</span>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="info-item">
                                <label class="info-label text-uppercase text-muted small fw-semibold">Dernière
                                    connexion</label>
                                <p class="info-value mb-0">{{ formatDate(user.dateDerniereConnexion) }}</p>
                            </div>
                        </div>
                    </div>

                    <div class="profile-actions mt-3 pt-3 border-top">
                        <BaseButton variant="primary" class="w-100" @click="handleChangePassword">
                            <i class="bi bi-key"></i>
                            Modifier mon mot de passe
                        </BaseButton>
                    </div>
                </BaseCard>
            </div>
        </div>

        <!-- Erreur -->
        <div v-else-if="error" class="profile-error d-flex justify-content-center align-items-center"
            style="min-height: 300px;">
            <div class="text-center">
                <i class="bi bi-exclamation-circle text-danger" style="font-size: 3rem;"></i>
                <h3 class="mt-3">Impossible de charger votre profil</h3>
                <p class="text-muted">{{ error }}</p>
                <BaseButton variant="primary" @click="loadProfile">
                    <i class="bi bi-arrow-repeat"></i>
                    Réessayer
                </BaseButton>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { BaseCard, BaseButton } from '@/components/base'
import { formatDate } from '@/utils/date'

// --- Store ---
const authStore = useAuthStore()

// --- State ---
const loading = ref(false)
const error = ref<string | null>(null)

// --- Computed ---
const user = computed(() => authStore.user)

const userInitials = computed(() => {
    if (!user.value) return ''

    const firstName = user.value.prenom || ''
    const lastName = user.value.nom || ''

    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase()
})

const userFullName = computed(() => {
    if (!user.value) return ''

    const firstName = user.value.prenom || ''
    const lastName = user.value.nom || ''

    return `${firstName} ${lastName}`.trim()
})

// --- Methods ---
const loadProfile = async () => {
    if (!authStore.isAuthenticated) {
        error.value = 'Vous devez être connecté pour accéder à votre profil.'
        return
    }

    loading.value = true
    error.value = null

    try {
        // Charger les données depuis /auth/me
        await authStore.loadProfile()
    } catch (err: any) {
        error.value = err.message || 'Une erreur est survenue lors du chargement du profil.'
        console.error('Erreur lors du chargement du profil:', err)
    } finally {
        loading.value = false
    }
}

const handleEditProfile = () => {
    // Préparer la navigation vers la page d'édition du profil
    // Exemple: router.push('/profile/edit')
    console.log('Modifier mes informations')
}

const handleChangePassword = () => {
    // Préparer la navigation vers la page de changement de mot de passe
    // Exemple: router.push('/profile/change-password')
    console.log('Modifier mon mot de passe')
}

// --- Lifecycle ---
onMounted(() => {
    if (!authStore.isAuthenticated) {
        error.value = 'Vous devez être connecté pour accéder à votre profil.'
        return
    }

    // Si l'utilisateur existe déjà mais manque des données, on recharge
    if (authStore.user && !authStore.user.email) {
        loadProfile()
    } else if (!authStore.user) {
        loadProfile()
    }
})
</script>

<style scoped>
/* ============================================
   PROFILE PAGE - Styles personnalisés
   ============================================ */

.profile-page {
    padding: 0.5rem 0;
}

/* --- Loading Skeleton --- */
.profile-loading {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    max-width: 600px;
}

.skeleton {
    position: relative;
    overflow: hidden;
    background: var(--skeleton-bg, #e9e9ea) !important;
    border-radius: 4px;
    height: 1.2rem;
}

.skeleton::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(90deg,
            transparent,
            var(--skeleton-highlight, rgba(221, 224, 224, 0.726)),
            transparent);
    transform: translateX(-100%);
    animation: skeleton-loading 1.4s infinite;
}

.skeleton-title {
    height: 1.8rem;
}

.skeleton-text {
    height: 0.9rem;
}

@keyframes skeleton-loading {
    0% {
        transform: translateX(-100%);
    }

    100% {
        transform: translateX(100%);
    }
}

/* --- Avatar --- */
.profile-avatar-card {
    border: 1px solid var(--border-color, #dee2e6);
    border-radius: var(--radius-lg, 0.5rem);
    box-shadow: var(--shadow-sm, 0 2px 4px rgba(0, 0, 0, 0.05));
}

.avatar-circle {
    width: 100px;
    height: 100px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--primary-color, #083869), var(--secondary-color, #164493));
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 12px rgba(8, 56, 105, 0.2);
}

.avatar-initials {
    font-size: 2.5rem;
    font-weight: 700;
    color: white;
    letter-spacing: 1px;
}

.avatar-name {
    font-weight: 600;
    color: var(--text-color, #0f172a);
}

.avatar-role {
    font-size: 0.9rem;
}

.avatar-status {
    background: rgba(16, 185, 129, 0.1);
}

.status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    display: inline-block;
}

.status-dot.active {
    background: var(--success-color, #10b981);
}

.status-text {
    font-size: 0.8rem;
    font-weight: 500;
    color: var(--success-color, #10b981);
}

/* --- Cartes d'informations --- */
.profile-info-card {
    border: 1px solid var(--border-color, #dee2e6);
    border-radius: var(--radius-lg, 0.5rem);
    box-shadow: var(--shadow-sm, 0 2px 4px rgba(0, 0, 0, 0.05));
}

.card-title {
    font-weight: 600;
    color: var(--text-color, #0f172a);
}

.info-label {
    font-size: 0.7rem;
    letter-spacing: 0.3px;
    color: var(--text-muted-custom, #6c757d);
}

.info-value {
    font-size: 0.95rem;
    color: var(--text-color, #0f172a);
    padding: 0.25rem 0;
    border-bottom: 1px solid transparent;
    transition: border-color 0.2s;
}

.info-value:hover {
    border-bottom-color: var(--border-color, #dee2e6);
}

.info-status .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
}

.info-status .status-dot.active {
    background: var(--success-color, #10b981);
}

.info-status .status-text {
    font-size: 0.9rem;
    font-weight: 400;
    color: var(--text-color, #0f172a);
}

/* --- Actions --- */
.profile-actions {
    padding-top: 1rem;
}

/* --- Responsive --- */
@media (max-width: 991.98px) {
    .profile-header {
        flex-direction: column;
    }
}

@media (max-width: 575.98px) {
    .profile-page {
        padding: 0.25rem 0;
    }

    .profile-header .d-flex {
        flex-direction: column;
        align-items: stretch !important;
    }

    .profile-header .d-flex .btn {
        width: 100%;
        justify-content: center;
    }

    .avatar-circle {
        width: 80px;
        height: 80px;
    }

    .avatar-initials {
        font-size: 2rem;
    }
}
</style>
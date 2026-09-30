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
                            {{ userPostes || 'Poste non défini' }}
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

                    <!-- Lecture -->
                    <div v-if="!isEditingProfile" class="row g-3">
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

                    <!-- Édition en place -->
                    <div v-else class="row g-3">
                        <div class="col-md-6">
                            <BaseInput v-model="form.nom" label="Nom" icon="bi bi-person" :error="errors.nom"
                                :disabled="isSavingProfile" />
                        </div>
                        <div class="col-md-6">
                            <BaseInput v-model="form.prenom" label="Prénom" icon="bi bi-person" :error="errors.prenom"
                                :disabled="isSavingProfile" />
                        </div>
                        <div class="col-md-6">
                            <BaseInput v-model="form.email" label="Email" type="email" icon="bi bi-envelope"
                                :error="errors.email" :disabled="isSavingProfile" />
                        </div>
                        <div class="col-md-6">
                            <BaseInput v-model="form.telephone" label="Téléphone" type="tel" icon="bi bi-telephone"
                                :error="errors.telephone" :disabled="isSavingProfile" />
                        </div>

                        <div v-if="formAlert" class="col-12">
                            <div class="profile-alert" :class="`profile-alert--${formAlert.type}`" role="alert">
                                <i :class="formAlert.type === 'success' ? 'bi bi-check-circle' : 'bi bi-exclamation-circle'"></i>
                                <span>{{ formAlert.message }}</span>
                            </div>
                        </div>

                        <div class="col-12">
                            <p class="profile-hint mb-0">
                                <i class="bi bi-info-circle"></i>
                                L'email sert à vous connecter et à réinitialiser votre mot de passe.
                                Le modifier vous déconnectera sur vos autres appareils.
                            </p>
                        </div>
                    </div>

                    <div class="profile-actions mt-3 pt-3 border-top">
                        <!-- Bouton lecture -> édition -->
                        <BaseButton v-if="!isEditingProfile" variant="secondary" class="w-100" @click="startEditProfile">
                            <i class="bi bi-pencil"></i>
                            Modifier mes informations
                        </BaseButton>

                        <!-- Boutons édition -->
                        <div v-else class="d-flex gap-2">
                            <BaseButton variant="secondary" class="flex-fill" :disabled="isSavingProfile"
                                @click="cancelEditProfile">
                                Annuler
                            </BaseButton>
                            <BaseButton variant="primary" class="flex-fill" :loading="isSavingProfile"
                                @click="saveProfile">
                                <i class="bi bi-check-lg"></i>
                                Enregistrer
                            </BaseButton>
                        </div>
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
                                <p class="info-value mb-0">{{ userPostes || 'Non défini' }}</p>
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
                        <!-- Bouton lecture -> formulaire -->
                        <BaseButton v-if="!isEditingPassword" variant="primary" class="w-100" @click="startChangePassword">
                            <i class="bi bi-key"></i>
                            Modifier mon mot de passe
                        </BaseButton>

                        <!-- Formulaire de changement en place -->
                        <div v-else class="profile-form">
                            <BaseInput v-model="passwordForm.ancienMotDePasse" label="Mot de passe actuel" type="password"
                                icon="bi bi-lock" autocomplete="current-password" :error="passwordErrors.ancienMotDePasse"
                                :disabled="isSavingPassword" />

                            <BaseInput v-model="passwordForm.password" label="Nouveau mot de passe" type="password"
                                icon="bi bi-lock" autocomplete="new-password" :error="passwordErrors.password"
                                :disabled="isSavingPassword" />

                            <BaseInput v-model="passwordForm.confirmPassword" label="Confirmer le nouveau mot de passe"
                                type="password" icon="bi bi-lock" autocomplete="new-password"
                                :error="passwordErrors.confirmPassword" :disabled="isSavingPassword" />

                            <p class="profile-hint mb-0">
                                <i class="bi bi-info-circle"></i>
                                Au moins 8 caractères, avec une majuscule, une minuscule, un chiffre et un caractère
                                spécial.
                            </p>

                            <div v-if="passwordAlert" class="profile-alert"
                                :class="`profile-alert--${passwordAlert.type}`" role="alert">
                                <i :class="passwordAlert.type === 'success' ? 'bi bi-check-circle' : 'bi bi-exclamation-circle'"></i>
                                <span>{{ passwordAlert.message }}</span>
                            </div>

                            <div class="d-flex gap-2">
                                <BaseButton variant="secondary" class="flex-fill" :disabled="isSavingPassword"
                                    @click="cancelChangePassword">
                                    Annuler
                                </BaseButton>
                                <BaseButton variant="primary" class="flex-fill" :loading="isSavingPassword"
                                    @click="savePassword">
                                    <i class="bi bi-check-lg"></i>
                                    Modifier
                                </BaseButton>
                            </div>
                        </div>
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
import { BaseCard, BaseButton, BaseInput } from '@/components/base'
import { formatDate } from '@/utils/date'
import * as authService from '@/services/auth'

// --- Store ---
const authStore = useAuthStore()

// --- State ---
const loading = ref(false)
const error = ref<string | null>(null)

// --- Édition des informations personnelles ---
const isEditingProfile = ref(false)
const isSavingProfile = ref(false)
const form = ref({ nom: '', prenom: '', email: '', telephone: '' })
const errors = ref<Record<string, string>>({})
const formAlert = ref<{ type: 'success' | 'danger'; message: string } | null>(null)

// --- Changement de mot de passe ---
const isEditingPassword = ref(false)
const isSavingPassword = ref(false)
const passwordForm = ref({ ancienMotDePasse: '', password: '', confirmPassword: '' })
const passwordErrors = ref<Record<string, string>>({})
const passwordAlert = ref<{ type: 'success' | 'danger'; message: string } | null>(null)

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

/**
 * Poste(s) de l'utilisateur.
 *
 * Delegue au store, qui est le seul endroit qui connait la forme de la
 * liste renvoyee par l'API. C est ce point la qui affichait "Poste non
 * defini" : le champ lu ici s appelait `poste`, alors que l'API renvoie
 * `postes`, un tableau.
 */
const userPostes = computed(() => authStore.posteLibelle)

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

const startEditProfile = () => {
    if (!user.value) return

    isEditingProfile.value = true
    formAlert.value = null
    errors.value = {}

    form.value = {
        nom: user.value.nom ?? '',
        prenom: user.value.prenom ?? '',
        email: user.value.email ?? '',
        telephone: user.value.telephone ?? '',
    }
}

const cancelEditProfile = () => {
    isEditingProfile.value = false
    isSavingProfile.value = false
    formAlert.value = null
    errors.value = {}
}

const saveProfile = async () => {
    errors.value = {}
    formAlert.value = null

    if (!form.value.nom.trim()) {
        errors.value.nom = 'Le nom est obligatoire'
    }

    if (!form.value.prenom.trim()) {
        errors.value.prenom = 'Le prénom est obligatoire'
    }

    if (!form.value.email.trim()) {
        errors.value.email = "L'email est obligatoire"
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.email.trim())) {
        errors.value.email = "L'email est invalide"
    }

    if (Object.keys(errors.value).length > 0) {
        return
    }

    isSavingProfile.value = true

    try {
        const response = await authService.updateProfile({
            nom: form.value.nom.trim(),
            prenom: form.value.prenom.trim(),
            email: form.value.email.trim(),
            telephone: form.value.telephone.trim() || null,
        })

        if (!response.success) {
            formAlert.value = { type: 'danger', message: response.error }
            return
        }

        // L'email est le sujet du JWT : s'il a changé, on stocke les
        // nouveaux jetons, sinon la session serait invalidée.
        if (response.data.jetonRenouvele) {
            if (response.data.accessToken) {
                localStorage.setItem('accessToken', response.data.accessToken)
            }

            if (response.data.refreshToken) {
                localStorage.setItem('refreshToken', response.data.refreshToken)
            }
        }

        authStore.setUser(response.data.profil)

        isEditingProfile.value = false
        formAlert.value = { type: 'success', message: 'Vos informations ont été mises à jour.' }
    } catch (err) {
        formAlert.value = {
            type: 'danger',
            message: err instanceof Error ? err.message : 'Une erreur est survenue.',
        }
    } finally {
        isSavingProfile.value = false
    }
}

const startChangePassword = () => {
    isEditingPassword.value = true
    passwordAlert.value = null
    passwordErrors.value = {}
    passwordForm.value = { ancienMotDePasse: '', password: '', confirmPassword: '' }
}

const cancelChangePassword = () => {
    isEditingPassword.value = false
    isSavingPassword.value = false
    passwordAlert.value = null
    passwordErrors.value = {}
    passwordForm.value = { ancienMotDePasse: '', password: '', confirmPassword: '' }
}

const savePassword = async () => {
    passwordErrors.value = {}
    passwordAlert.value = null

    if (!passwordForm.value.ancienMotDePasse) {
        passwordErrors.value.ancienMotDePasse = 'Le mot de passe actuel est obligatoire'
    }

    if (passwordForm.value.password.length < 8) {
        passwordErrors.value.password = 'Au moins 8 caractères'
    } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).+$/.test(passwordForm.value.password)) {
        passwordErrors.value.password =
            'Il faut une majuscule, une minuscule, un chiffre et un caractère spécial'
    }

    if (passwordForm.value.password !== passwordForm.value.confirmPassword) {
        passwordErrors.value.confirmPassword = 'Les mots de passe ne correspondent pas'
    }

    if (Object.keys(passwordErrors.value).length > 0) {
        return
    }

    isSavingPassword.value = true

    try {
        const response = await authService.changePassword({ ...passwordForm.value })

        if (!response.success) {
            passwordAlert.value = { type: 'danger', message: response.error }
            return
        }

        cancelChangePassword()

        passwordAlert.value = { type: 'success', message: 'Votre mot de passe a été modifié.' }
        passwordErrors.value = {}
    } catch (err) {
        passwordAlert.value = {
            type: 'danger',
            message: err instanceof Error ? err.message : 'Une erreur est survenue.',
        }
    } finally {
        isSavingPassword.value = false
    }
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

/* --- Formulaire de changement de mot de passe --- */
.profile-form {
    display: flex;
    flex-direction: column;
    gap: 0.9rem;
}

/* --- Message d'information --- */
.profile-hint {
    font-size: 0.78rem;
    color: var(--text-muted-custom, #74879b);
    line-height: 1.5;
    display: flex;
    align-items: flex-start;
    gap: 0.4rem;
}

.profile-hint i {
    flex-shrink: 0;
    margin-top: 0.1rem;
}

/* --- Alertes de retour --- */
.profile-alert {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.6rem 0.75rem;
    border-radius: 7px;
    font-size: 0.82rem;
    line-height: 1.4;
}

.profile-alert--success {
    background: #e7f5ee;
    border: 1px solid #c2e3d2;
    color: #1f7a53;
}

.profile-alert--danger {
    background: #fdecea;
    border: 1px solid #f6cfcc;
    color: #a61b16;
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
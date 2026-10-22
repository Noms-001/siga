<template>
    <BaseConfirm
        v-model="confirmation.ouvert.value"
        :demande="confirmation.demande.value"
        :loading="confirmation.enCours.value || isSavingProfile || isSavingPassword"
        @confirme="confirmation.confirmer"
        @annule="confirmation.annuler"
    />

    <div class="profile-page">
        <!-- ============ EN-TÊTE ============ -->
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>Mon profil</h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    Consultez et gérez vos informations personnelles
                </p>
            </div>

            <div class="welcome-row__right">
                <BaseButton
                    v-if="user"
                    variant="secondary"
                    :loading="loading"
                    :disabled="loading"
                    @click="loadProfile"
                >
                    <i class="bi bi-arrow-clockwise"></i>
                    Actualiser
                </BaseButton>
            </div>
        </header>

        <!-- ============ CHARGEMENT ============ -->
        <div v-if="loading" class="loading-state">
            <div class="profile-skeleton">
                <div class="profile-skeleton__avatar"></div>
                <div class="skeleton skeleton--line skeleton--short"></div>
                <div class="skeleton skeleton--line skeleton--medium"></div>
            </div>
            <div class="info-skeleton">
                <div class="skeleton skeleton--line skeleton--short"></div>
                <div class="skeleton skeleton--line"></div>
                <div class="skeleton skeleton--line skeleton--medium"></div>
            </div>
        </div>

        <!-- ============ ERREUR ============ -->
        <div v-else-if="error" class="error-state">
            <div class="error-state__icon">
                <i class="bi bi-exclamation-circle"></i>
            </div>
            <h2 class="error-state__title">Impossible de charger votre profil</h2>
            <p class="error-state__text">{{ error }}</p>
            <BaseButton variant="primary" @click="loadProfile">
                <i class="bi bi-arrow-repeat"></i>
                Réessayer
            </BaseButton>
        </div>

        <!-- ============ CONTENU ============ -->
        <div v-else-if="user" class="profile-layout">
            <!-- ============ SIDEBAR : AVATAR + STATUT ============ -->
            <aside class="profile-side">
                <section class="avatar-card">
                    <div class="avatar-card__circle">
                        <span class="avatar-card__initials">{{ userInitials }}</span>
                    </div>
                    <h2 class="avatar-card__name">{{ userFullName }}</h2>
                    <p class="avatar-card__role">
                        {{ userPoste || 'Poste non défini' }}
                    </p>
                    <span class="avatar-card__status">
                        <span class="status-dot"></span>
                        Compte actif
                    </span>
                </section>
            </aside>

            <!-- ============ COLONNE PRINCIPALE ============ -->
            <div class="profile-main">
                <!-- ===== INFORMATIONS PERSONNELLES ===== -->
                <section class="content-card">
                    <header class="content-card__head">
                        <span class="content-card__icon" aria-hidden="true">
                            <i class="bi bi-person"></i>
                        </span>
                        <h2 class="content-card__title">Informations personnelles</h2>
                        <span
                            v-if="isEditingProfile"
                            class="content-card__badge content-card__badge--edit"
                        >
                            Édition
                        </span>
                    </header>

                    <div class="content-card__body">
                        <!-- Lecture -->
                        <div v-if="!isEditingProfile" class="info-grid">
                            <div class="info-row">
                                <span class="info-row__label">Nom</span>
                                <span class="info-row__value">
                                    {{ user.nom || 'Non renseigné' }}
                                </span>
                            </div>
                            <div class="info-row">
                                <span class="info-row__label">Prénom</span>
                                <span class="info-row__value">
                                    {{ user.prenom || 'Non renseigné' }}
                                </span>
                            </div>
                            <div class="info-row">
                                <span class="info-row__label">Email</span>
                                <span class="info-row__value">{{ user.email || 'Non renseigné' }}</span>
                            </div>
                            <div class="info-row">
                                <span class="info-row__label">Téléphone</span>
                                <span class="info-row__value">
                                    {{ user.telephone || 'Non renseigné' }}
                                </span>
                            </div>
                        </div>

                        <!-- Édition -->
                        <div v-else class="edit-form">
                            <div class="edit-grid">
                                <div class="edit-field">
                                    <BaseInput
                                        v-model="form.nom"
                                        label="Nom"
                                        icon="bi bi-person"
                                        :error="errors.nom"
                                        :disabled="isSavingProfile"
                                    />
                                </div>
                                <div class="edit-field">
                                    <BaseInput
                                        v-model="form.prenom"
                                        label="Prénom"
                                        icon="bi bi-person"
                                        :error="errors.prenom"
                                        :disabled="isSavingProfile"
                                    />
                                </div>
                                <div class="edit-field">
                                    <BaseInput
                                        v-model="form.email"
                                        label="Email"
                                        type="email"
                                        icon="bi bi-envelope"
                                        :error="errors.email"
                                        :disabled="isSavingProfile"
                                    />
                                </div>
                                <div class="edit-field">
                                    <BaseInput
                                        v-model="form.telephone"
                                        label="Téléphone"
                                        type="tel"
                                        icon="bi bi-telephone"
                                        :error="errors.telephone"
                                        :disabled="isSavingProfile"
                                    />
                                </div>
                            </div>

                            <div v-if="formAlert" class="alert" :class="`alert--${formAlert.type}`" role="alert">
                                <i :class="formAlert.type === 'success' ? 'bi bi-check-circle-fill' : 'bi bi-exclamation-circle-fill'"></i>
                                <span>{{ formAlert.message }}</span>
                            </div>
                        </div>
                    </div>

                    <footer class="content-card__foot">
                        <BaseButton v-if="!isEditingProfile" variant="secondary" @click="startEditProfile">
                            <i class="bi bi-pencil"></i>
                            Modifier mes informations
                        </BaseButton>

                        <div v-else class="content-card__actions">
                            <BaseButton
                                variant="secondary"
                                :disabled="isSavingProfile"
                                @click="cancelEditProfile"
                            >
                                Annuler
                            </BaseButton>
                            <BaseButton
                                variant="primary"
                                :loading="isSavingProfile"
                                @click="saveProfile"
                            >
                                <i class="bi bi-check-lg"></i>
                                Enregistrer
                            </BaseButton>
                        </div>
                    </footer>
                </section>

                <!-- ===== INFORMATIONS PROFESSIONNELLES ===== -->
                <section class="content-card">
                    <header class="content-card__head">
                        <span class="content-card__icon" aria-hidden="true">
                            <i class="bi bi-briefcase"></i>
                        </span>
                        <h2 class="content-card__title">Informations professionnelles</h2>
                    </header>

                    <div class="content-card__body">
                        <div class="info-grid info-grid--2">
                            <div class="info-row">
                                <span class="info-row__label">Poste</span>
                                <span class="info-row__value">
                                    {{ userPoste || 'Non défini' }}
                                </span>
                            </div>
                            <div class="info-row">
                                <span class="info-row__label">Service</span>
                                <span class="info-row__value">
                                    {{ user.service || 'Non défini' }}
                                </span>
                            </div>
                            <div class="info-row">
                                <span class="info-row__label">Statut du compte</span>
                                <span class="info-row__value info-row__value--status">
                                    <span class="status-dot"></span>
                                    Actif
                                </span>
                            </div>
                            <div class="info-row">
                                <span class="info-row__label">Dernière connexion</span>
                                <span class="info-row__value">
                                    {{ formatDate(user.dateDerniereConnexion) }}
                                </span>
                            </div>
                        </div>
                    </div>

                    <footer class="content-card__foot">
                        <BaseButton v-if="!isEditingPassword" variant="primary" @click="startChangePassword">
                            <i class="bi bi-key"></i>
                            Modifier mon mot de passe
                        </BaseButton>

                        <div v-else class="edit-form">
                            <div class="edit-grid edit-grid--1">
                                <div class="edit-field">
                                    <BaseInput
                                        v-model="passwordForm.ancienMotDePasse"
                                        label="Mot de passe actuel"
                                        type="password"
                                        icon="bi bi-lock"
                                        autocomplete="current-password"
                                        :error="passwordErrors.ancienMotDePasse"
                                        :disabled="isSavingPassword"
                                    />
                                </div>
                                <div class="edit-field">
                                    <BaseInput
                                        v-model="passwordForm.password"
                                        label="Nouveau mot de passe"
                                        type="password"
                                        icon="bi bi-lock"
                                        autocomplete="new-password"
                                        :error="passwordErrors.password"
                                        :disabled="isSavingPassword"
                                    />
                                </div>
                                <div class="edit-field">
                                    <BaseInput
                                        v-model="passwordForm.confirmPassword"
                                        label="Confirmer le nouveau mot de passe"
                                        type="password"
                                        icon="bi bi-lock"
                                        autocomplete="new-password"
                                        :error="passwordErrors.confirmPassword"
                                        :disabled="isSavingPassword"
                                    />
                                </div>
                            </div>

                            <p class="edit-hint">
                                <i class="bi bi-info-circle"></i>
                                Au moins 8 caractères, avec une majuscule, une
                                minuscule, un chiffre et un caractère spécial.
                            </p>

                            <div v-if="passwordAlert" class="alert" :class="`alert--${passwordAlert.type}`" role="alert">
                                <i :class="passwordAlert.type === 'success' ? 'bi bi-check-circle-fill' : 'bi bi-exclamation-circle-fill'"></i>
                                <span>{{ passwordAlert.message }}</span>
                            </div>

                            <div class="content-card__actions">
                                <BaseButton
                                    variant="secondary"
                                    :disabled="isSavingPassword"
                                    @click="cancelChangePassword"
                                >
                                    Annuler
                                </BaseButton>
                                <BaseButton
                                    variant="primary"
                                    :loading="isSavingPassword"
                                    @click="savePassword"
                                >
                                    <i class="bi bi-check-lg"></i>
                                    Modifier
                                </BaseButton>
                            </div>
                        </div>
                    </footer>
                </section>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { BaseCard, BaseButton, BaseConfirm, BaseInput } from '@/components/base'
import { useConfirmation } from '@/composables/useConfirmation'
import { formatDate } from '@/utils/date'
import * as authService from '@/services/auth'

const authStore = useAuthStore()

/* -------------------- en-tête -------------------- */
const dateDuJour = computed(() =>
    new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric',
    })
        .format(new Date())
        .toUpperCase()
)

/* -------------------- state -------------------- */
const loading = ref(false)
const error = ref<string | null>(null)

/* Édition des informations personnelles */
const isEditingProfile = ref(false)
const isSavingProfile = ref(false)
const form = ref({ nom: '', prenom: '', email: '', telephone: '' })
const errors = ref<Record<string, string>>({})
const formAlert = ref<{ type: 'success' | 'danger'; message: string } | null>(null)

/* Changement de mot de passe */
const isEditingPassword = ref(false)
const isSavingPassword = ref(false)
const passwordForm = ref({ ancienMotDePasse: '', password: '', confirmPassword: '' })
const passwordErrors = ref<Record<string, string>>({})
const passwordAlert = ref<{ type: 'success' | 'danger'; message: string } | null>(null)

/* -------------------- computed -------------------- */
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

const userPoste = computed(() => authStore.user?.poste ?? null)

/* -------------------- méthodes -------------------- */
const loadProfile = async () => {
    if (!authStore.isAuthenticated) {
        error.value = 'Vous devez être connecté pour accéder à votre profil.'
        return
    }

    loading.value = true
    error.value = null

    try {
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

const confirmation = useConfirmation()

const cancelEditProfile = () => {
    isEditingProfile.value = false
    isSavingProfile.value = false
    formAlert.value = null
    errors.value = {}
}

const saveProfile = () => {
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

    const emailChange = form.value.email.trim() !== (authStore.user?.email ?? '')

    confirmation.demander({
        titre: 'Enregistrer le profil',
        message: emailChange
            ? `Votre email passera à ${form.value.email.trim()}.`
            : 'Vos informations personnelles seront enregistrées.',
        consequence: emailChange
            ? "Changer d'email déconnecte vos autres sessions, car il est le sujet du jeton."
            : undefined,
        libelleConfirmer: 'Enregistrer',
        ton: emailChange ? 'warning' : 'primary',
        icone: 'bi bi-person-check',
        action: enregistrerProfil,
    })
}

const enregistrerProfil = async () => {
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

const savePassword = () => {
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

    confirmation.demander({
        titre: 'Changer le mot de passe',
        message: 'Votre mot de passe actuel sera remplacé.',
        consequence: 'Les autres sessions ouvertes avec le mot de passe précédent devront vous reconnecter.',
        libelleConfirmer: 'Changer',
        libelleAnnuler: 'Revenir',
        ton: 'warning',
        icone: 'bi bi-key',
        action: enregistrerMotDePasse,
    })
}

const enregistrerMotDePasse = async () => {
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

/* -------------------- lifecycle -------------------- */
onMounted(() => {
    if (!authStore.isAuthenticated) {
        error.value = 'Vous devez être connecté pour accéder à votre profil.'
        return
    }

    if (authStore.user && !authStore.user.email) {
        loadProfile()
    } else if (!authStore.user) {
        loadProfile()
    }
})
</script>

<style scoped>
/* ============================================================
   CONTENEUR
   ============================================================ */
.profile-page {
    min-height: calc(100vh - 68px);
    padding: 34px 38px 24px;
    display: flex;
    flex-direction: column;
    gap: 1.15rem;
    color: #15233c;
    font-family: "DM Sans", "Segoe UI", sans-serif;
    background:
        radial-gradient(circle at 12% 8%, rgba(26, 42, 108, .055) 0, transparent 38%),
        radial-gradient(circle at 92% 94%, rgba(212, 175, 55, .065) 0, transparent 36%),
        #f5f7fa;
}

.profile-page .welcome-row {
    margin-bottom: 0.25rem;
}

/* ============================================================
   LAYOUT — sidebar + main
   ============================================================ */
.profile-layout {
    display: grid;
    grid-template-columns: minmax(240px, 280px) minmax(0, 1fr);
    gap: 1.15rem;
    align-items: start;
}

.profile-side {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    position: sticky;
    top: 88px;
}

.profile-main {
    display: flex;
    flex-direction: column;
    gap: 1.15rem;
    min-width: 0;
}

/* ============================================================
   CARTE AVATAR
   ============================================================ */
.avatar-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 0.5rem;
    padding: 2rem 1.5rem 1.75rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .04),
        0 8px 24px rgba(26, 42, 108, .06);
    position: relative;
    overflow: hidden;
    min-height: 620px;
}

.avatar-card::before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, #1a2a6c 0%, #4a6cf7 50%, #d4af37 100%);
}

.avatar-card__circle {
    display: grid;
    place-items: center;
    width: 96px;
    height: 96px;
    border-radius: 50%;
    background: linear-gradient(135deg, #1a2a6c 0%, #4a6cf7 100%);
    box-shadow:
        0 8px 24px rgba(26, 42, 108, .25),
        0 0 0 4px #fff,
        0 0 0 6px rgba(212, 175, 55, .5);
    margin-bottom: 0.75rem;
}

.avatar-card__initials {
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 2.25rem;
    font-weight: 800;
    color: #fff;
    letter-spacing: 0.02em;
}

.avatar-card__name {
    margin: 0;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 1.1rem;
    font-weight: 700;
    color: #1a2a6c;
    letter-spacing: -0.01em;
    line-height: 1.3;
}

.avatar-card__role {
    margin: 0;
    font-size: 0.85rem;
    color: #6b7280;
}

.avatar-card__status {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.3rem 0.7rem;
    border-radius: 20px;
    background: rgba(74, 216, 167, .12);
    color: #1e7e34;
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.02em;
    margin-top: 0.5rem;
}

.status-dot {
    display: inline-block;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #4ad8a7;
    box-shadow: 0 0 0 3px rgba(74, 216, 167, .25);
    flex: none;
}

/* ============================================================
   SIDE INFO (carte d'aide)
   ============================================================ */
.side-info {
    display: flex;
    gap: 0.85rem;
    padding: 1.1rem 1.25rem;
    background: linear-gradient(135deg, #f0f6ff 0%, #fff 100%);
    border: 1px solid #cfe0ee;
    border-left: 3px solid #4a6cf7;
    border-radius: 14px;
}

.side-info__icon {
    display: grid;
    place-items: center;
    width: 34px;
    height: 34px;
    border-radius: 10px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 1rem;
    flex: none;
}

.side-info__title {
    margin: 0 0 0.25rem;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 0.85rem;
    font-weight: 700;
    color: #1a2a6c;
}

.side-info__text {
    margin: 0;
    font-size: 0.78rem;
    line-height: 1.55;
    color: #6b7280;
}

/* ============================================================
   CONTENT CARDS
   ============================================================ */
.content-card {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .04),
        0 8px 24px rgba(26, 42, 108, .06);
    overflow: hidden;
    display: flex;
    flex-direction: column;
}

.content-card__head {
    display: flex;
    align-items: center;
    gap: 0.65rem;
    padding: 0.85rem 1.25rem;
    background: linear-gradient(140deg, #f7f9fd, #fff);
    border-bottom: 1px solid #e6ebf3;
}

.content-card__icon {
    display: grid;
    place-items: center;
    width: 30px;
    height: 30px;
    border-radius: 9px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.9rem;
    flex: none;
}

.content-card__title {
    margin: 0;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 0.92rem;
    font-weight: 700;
    color: #1a2a6c;
    letter-spacing: -0.005em;
    flex: 1;
}

.content-card__badge {
    padding: 0.15rem 0.55rem;
    border-radius: 20px;
    font-size: 0.68rem;
    font-weight: 700;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    white-space: nowrap;
}

.content-card__badge--edit {
    background: #fff6dd;
    color: #b8860b;
    border: 1px solid #f4e2c2;
}

.content-card__body {
    padding: 1rem 1.25rem 1.15rem;
}

.content-card__foot {
    padding: 0.85rem 1.25rem;
    background: #f8fafc;
    border-top: 1px solid #eef1f5;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 0.6rem;
    flex-wrap: wrap;
}

.content-card__actions {
    display: flex;
    gap: 0.6rem;
    justify-content: flex-end;
    flex-wrap: wrap;
    width: 100%;
}

/* ============================================================
   INFO GRID (lecture)
   ============================================================ */
.info-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0.85rem 1.5rem;
}

.info-grid--2 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
}

.info-row {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    padding: 0.55rem 0;
    border-bottom: 1px dashed #eef1f5;
}

.info-row:last-child {
    border-bottom: none;
    padding-bottom: 0;
}

.info-row__label {
    font-size: 0.7rem;
    font-weight: 700;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    color: #8a95a8;
}

.info-row__value {
    font-size: 0.9rem;
    font-weight: 500;
    color: #15233c;
    word-break: break-word;
    min-height: 1.3em;
}

.info-row__value--status {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    font-weight: 600;
    color: #1e7e34;
}

/* ============================================================
   FORMULAIRE D'ÉDITION
   ============================================================ */
.edit-form {
    display: flex;
    flex-direction: column;
    gap: 0.9rem;
}

.edit-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0.85rem;
}

.edit-grid--1 {
    grid-template-columns: 1fr;
}

.edit-field {
    min-width: 0;
}

.edit-hint {
    display: flex;
    align-items: flex-start;
    gap: 0.45rem;
    margin: 0;
    padding: 0.6rem 0.75rem;
    background: #f0f6ff;
    border-left: 3px solid #4a6cf7;
    border-radius: 6px;
    font-size: 0.78rem;
    line-height: 1.5;
    color: #6b7280;
}

.edit-hint i {
    flex: none;
    margin-top: 0.1rem;
    color: #4a6cf7;
    font-size: 0.85rem;
}

/* ============================================================
   ALERTES
   ============================================================ */
.alert {
    display: flex;
    align-items: flex-start;
    gap: 0.5rem;
    padding: 0.65rem 0.85rem;
    border-radius: 8px;
    font-size: 0.82rem;
    line-height: 1.45;
}

.alert i {
    flex: none;
    font-size: 0.9rem;
    margin-top: 0.05rem;
}

.alert--success {
    background: #e6f4ea;
    border: 1px solid #c8e5cf;
    color: #1e7e34;
}
.alert--success i { color: #1e7e34; }

.alert--danger {
    background: #fdecea;
    border: 1px solid #f6cfcc;
    color: #a61b16;
}
.alert--danger i { color: #a61b16; }

/* ============================================================
   ÉTAT D'ERREUR
   ============================================================ */
.error-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 4rem 1.5rem;
    text-align: center;
    background: #fff;
    border: 1px dashed #dde4ec;
    border-radius: 16px;
}

.error-state__icon {
    display: grid;
    place-items: center;
    width: 64px;
    height: 64px;
    border-radius: 20px;
    background: #fdecea;
    color: #a61b16;
    font-size: 1.8rem;
    margin-bottom: 1rem;
}

.error-state__title {
    margin: 0 0 0.35rem;
    font-size: 1.05rem;
    font-weight: 700;
    color: #1a2a6c;
}

.error-state__text {
    margin: 0 0 1.1rem;
    font-size: 0.88rem;
    color: #6b7280;
    max-width: 480px;
}

/* ============================================================
   SKELETONS
   ============================================================ */
.loading-state {
    display: grid;
    grid-template-columns: minmax(240px, 280px) minmax(0, 1fr);
    gap: 1.15rem;
    align-items: start;
}

.skeleton {
    background: linear-gradient(90deg, #eef1f5 25%, #f6f8fa 50%, #eef1f5 75%);
    background-size: 200% 100%;
    animation: shimmer 1.4s infinite;
    border-radius: 6px;
}

.skeleton--line { height: 12px; }
.skeleton--short { width: 40%; }
.skeleton--medium { width: 65%; }

@keyframes shimmer {
    from { background-position: 200% 0; }
    to { background-position: -200% 0; }
}

.profile-skeleton {
    padding: 2rem 1.5rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.75rem;
}

.profile-skeleton__avatar {
    width: 96px;
    height: 96px;
    border-radius: 50%;
    background: #eef1f5;
    margin-bottom: 0.5rem;
}

.info-skeleton {
    padding: 1.25rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 1199.98px) {
    .profile-page { padding-left: 24px; padding-right: 24px; }
}

@media (max-width: 991.98px) {
    .profile-layout,
    .loading-state {
        grid-template-columns: 1fr;
    }
    .profile-side {
        position: static;
        display: grid;
        grid-template-columns: 1fr 1fr;
    }
    .avatar-card {
        padding: 1.5rem 1rem;
    }
}

@media (max-width: 767.98px) {
    .profile-page { padding: 24px 16px 20px; }
    .profile-side {
        grid-template-columns: 1fr;
    }
    .info-grid,
    .info-grid--2 {
        grid-template-columns: 1fr;
    }
    .edit-grid {
        grid-template-columns: 1fr;
    }
    .content-card__foot {
        justify-content: stretch;
    }
    .content-card__foot :deep(.base-btn),
    .content-card__foot :deep(.btn) {
        flex: 1;
    }
    .content-card__actions {
        flex-direction: column-reverse;
    }
}

@media (max-width: 575.98px) {
    .profile-page { padding: 20px 12px 16px; }
    .avatar-card__circle {
        width: 80px;
        height: 80px;
    }
    .avatar-card__initials {
        font-size: 1.9rem;
    }
    .content-card__body {
        padding: 0.9rem 1rem;
    }
    .content-card__head {
        padding: 0.75rem 1rem;
    }
    .content-card__foot {
        padding: 0.75rem 1rem;
    }
}

@media (prefers-reduced-motion: reduce) {
    .skeleton { animation: none !important; }
    .avatar-card__circle { transition: none !important; }
}
</style>
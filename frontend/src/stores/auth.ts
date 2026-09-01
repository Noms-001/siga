import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

import type {
    ForgotPasswordRequest,
    LoginRequest,
    LoginResponse,
    ProfileResponse,
    ResetPasswordRequest
} from '@/types/auth'

import * as authService from '@/services/auth'

export const useAuthStore = defineStore('auth', () => {

    // Le store conserve toujours le profil complet
    const user = ref<ProfileResponse | null>(null)

    const isAuthenticated = computed(() => user.value !== null)

    const initialized = ref(false)

    /**
     * Connexion
     */
    const login = async (credentials: LoginRequest) => {

        const response = await authService.login(credentials)

        if (!response.success || !response.data) {
            throw new Error(response.error ?? 'Échec de la connexion')
        }

        const loginData: LoginResponse = response.data

        // Après le login, on récupère le profil complet
        const profileResponse = await authService.getProfile()

        if (!profileResponse.success || !profileResponse.data) {
            throw new Error(
                profileResponse.error ?? 'Impossible de récupérer le profil'
            )
        }

        user.value = profileResponse.data

        return {
            ...loginData,
            ...profileResponse.data
        }
    }

    /**
     * Récupérer le profil complet
     */
    const loadProfile = async () => {

        const response = await authService.getProfile()

        if (!response.success || !response.data) {
            clearUser()
            return null
        }

        user.value = response.data

        return user.value
    }

    /**
     * Restaurer la session
     */
    const restoreSession = async () => {

        try {

            const response = await authService.getProfile()

            if (response.success && response.data) {
                user.value = response.data
            } else {
                user.value = null
            }

        } catch {

            user.value = null

        } finally {

            initialized.value = true

        }
    }

    /**
     * Déconnexion
     */
    const logout = async () => {

        try {

            await authService.logout()

        } finally {

            clearUser()

        }
    }

    /**
 * Demander la réinitialisation du mot de passe
 */
    const forgotPassword = async (data: ForgotPasswordRequest) => {

        const response = await authService.forgotPassword(data)

        if (!response.success) {
            throw new Error(
                response.error ?? 'Impossible d’envoyer la demande de réinitialisation'
            )
        }

        return response
    }

    /**
     * Vérifier le token de réinitialisation
     */
    const verifyResetPasswordToken = async (token: string) => {

        const response = await authService.verifyResetPasswordToken(token)

        if (!response.success) {
            throw new Error(
                response.error ?? 'Le lien de réinitialisation est invalide ou expiré'
            )
        }

        return response
    }

    /**
     * Réinitialiser le mot de passe
     */
    const resetPassword = async (data: ResetPasswordRequest) => {

        const response = await authService.resetPassword(data)

        if (!response.success) {
            throw new Error(
                response.error ?? 'Impossible de réinitialiser le mot de passe'
            )
        }

        return response
    }

    /**
     * Définir l'utilisateur
     */
    const setUser = (userData: ProfileResponse) => {
        user.value = userData
    }

    /**
     * Supprimer l'utilisateur
     */
    const clearUser = () => {
        user.value = null
    }

    return {
        user,
        isAuthenticated,
        initialized,
        restoreSession,
        login,
        loadProfile,
        logout,
        forgotPassword,
        verifyResetPasswordToken,
        resetPassword,
        setUser,
        clearUser
    }
})

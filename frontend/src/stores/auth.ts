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
import type { ApiResponse } from '@/services/api-client'

export const useAuthStore = defineStore('auth', () => {

    // Le store conserve toujours le profil complet
    const user = ref<ProfileResponse | null>(null)

    const isAuthenticated = computed(() => user.value !== null)

    const initialized = ref(false)

    /**
     * Accès backoffice.
     *
     * `includes` et non `===` : postes est une liste, donc la comparaison
     * scalaire renvoyait toujours false et l'acces backoffice etait refuse a
     * tout le monde, y compris a un vrai administrateur. Le cas passa
     * inaperçu parce qu'un utilisateur non administrateur voit exactement le
     * meme resultat, a savoir aucun acces.
     *
     * Le libelle est compare sans casse, comme le fait AuthService cote
     * backend pour la meme verification.
     */
    const isBackoffice = computed(() => {
        return (user.value?.postes ?? []).some(
            poste => poste.toLowerCase() === 'administrateur'
        )
    })

    /** Postes de l'utilisateur, jamais undefined. */
    const postes = computed<string[]>(() => user.value?.postes ?? [])

    /**
     * Postes réduits à un libellé affichable, ou null si l'utilisateur n'en
     * a aucun.
     *
     * null et non "" : les appelants utilisent `|| 'Non défini'`, et une
     * chaine vide passerait pour un poste renseigné mais vide.
     */
    const posteLibelle = computed<string | null>(() => {
        const liste = postes.value
        return liste.length > 0 ? liste.join(' / ') : null
    })

    /**
     * Connexion
     */
    /**
 * Connexion
 */
    const login = async (
        credentials: LoginRequest,
        isBackoffice: boolean = false
    ) => {

        let response: ApiResponse<LoginResponse>

        if (isBackoffice) {
            response = await authService.loginBackoffice(credentials)
        } else {
            response = await authService.login(credentials)
        }

        if (!response.success || !response.data) {
            throw new Error(
                response.error ?? 'Échec de la connexion'
            )
        }

        const loginData: LoginResponse = response.data

        // Stocker les tokens AVANT d'appeler /auth/me
        localStorage.setItem(
            'accessToken',
            loginData.accessToken
        )

        localStorage.setItem(
            'refreshToken',
            loginData.refreshToken
        )

        // Maintenant le JWT sera automatiquement envoyé
        // par api-client.ts
        const profileResponse = await authService.getProfile()

        if (!profileResponse.success || !profileResponse.data) {

            // Si le profil échoue, on nettoie les tokens
            localStorage.removeItem('accessToken')
            localStorage.removeItem('refreshToken')

            throw new Error(
                profileResponse.error ??
                'Impossible de récupérer le profil'
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
     * Fermeture de session purement locale, sans appel au backend.
     *
     * Réservée aux cas où le serveur considère déjà la session morte (token
     * expiré, 401 reçu, inactivité) : un POST /auth/logout répondrait à son
     * tour 401 et ne donnerait rien, sans compter une requête inutile.
     */
    const logoutLocal = () => {

        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')

        clearUser()
    }

    /**
     * Déconnexion
     */
    const logout = async () => {

        try {

            await authService.logout()

        } finally {

            logoutLocal()
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
        postes,
        posteLibelle,
        isBackoffice,
        isAuthenticated,
        initialized,
        restoreSession,
        login,
        loadProfile,
        logout,
        logoutLocal,
        forgotPassword,
        verifyResetPasswordToken,
        resetPassword,
        setUser,
        clearUser
    }
})

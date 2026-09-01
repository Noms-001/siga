/**
 * Données envoyées lors de la connexion
 */
export interface LoginRequest {
    email: string
    password: string
}

/**
 * Données du profil complet de l'utilisateur
 */
export interface ProfileResponse {
    idUtilisateur: number
    nom: string
    prenom: string
    email: string
    telephone: string | null
    service: string | null
    poste: string | null
    actif: boolean
    dateCreation: string
    dateDerniereConnexion: string | null
    dateDesactivation: string | null
}

/**
 * Données retournées après une connexion réussie
 */
export interface LoginResponse {
    idUtilisateur: number
    nom: string
    prenom: string
    poste: string | null
    service: string | null
}

export interface ForgotPasswordRequest {
    email: string
}

export interface ResetPasswordRequest {
    token: string
    password: string
    confirmPassword: string
}
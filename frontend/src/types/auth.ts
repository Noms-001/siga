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
    /**
     * Un utilisateur peut cumuler plusieurs postes (poste_utilisateur est une
     * table de jointure, pas une colonne).
     *
     * L'API renvoie donc un TABLEAU. Ce contrat est déclare ici une seule fois :
     * le typer en `poste: string` le faisait passer pour absent partout, et
     * aucun appel n'aurait signalé l'erreur, puisque le JSON est valide dans
     * les deux cas.
     */
    postes: string[]
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
    /** Tableau pour la même raison que ProfileResponse.postes. */
    postes: string[]
    service: string | null

    accessToken: string
    refreshToken: string
}

export interface ForgotPasswordRequest {
    email: string
}

export interface ResetPasswordRequest {
    token: string
    password: string
    confirmPassword: string
}
/**
 * Données envoyées pour modifier les informations personnelles
 */
export interface ModifierProfilRequest {
    nom: string
    prenom: string
    email: string
    telephone: string | null
}

/**
 * Réponse de la modification du profil.
 *
 * Les jetons ne sont présents que si l'email a changé, car le sujet du JWT
 * est l'email : sans renouvellement, la session en cours serait invalidée.
 */
export interface ModifierProfilResponse {
    profil: ProfileResponse
    accessToken: string | null
    refreshToken: string | null
    jetonRenouvele: boolean
}

/**
 * Données envoyées pour changer le mot de passe en étant connecté
 */
export interface ChangerMotDePasseRequest {
    ancienMotDePasse: string
    password: string
    confirmPassword: string
}

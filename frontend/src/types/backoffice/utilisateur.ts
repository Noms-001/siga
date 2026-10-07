export interface Utilisateur {
    id: number
    nom: string
    prenom: string
    email: string
    telephone?: string | null
    actif: boolean
    enAttenteActivation: boolean
    dateCreation?: string | null
    dateModification?: string | null
    dateDesactivation?: string | null
    dateDerniereConnexion?: string | null
    departement: { id: number; code: string; nom: string } | null
    service: {
        id: number
        nom: string
        actif: boolean
        departement: { id: number; code: string; nom: string }
    } | null
    poste: { id: number; nom: string; isMetier: boolean }
}

/**
 * Reprend la forme exacte du DTO backend UtilisateurRequest.
 *
 * Ne pas renommer les champs : le backend les attend tels quels. Les noms
 * `idDepartement`, `idService`, `idPoste` viennent du DTO Java, pas d'une
 * convention frontend.
 */
export interface UtilisateurRequest {
    nom: string
    prenom: string
    email: string
    telephone?: string | null
    idDepartement: number
    idService?: number | null
    idPoste: number
}
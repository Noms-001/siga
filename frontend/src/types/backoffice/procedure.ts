export interface PosteDecideur {
    id: number
    nom: string
    isMetier: boolean
}

export interface EtapeValidation {
    id: number
    designation: string
    description?: string | null
    niveau: number
    obligatoire: boolean
    actif: boolean
    retour: boolean
    dateDesactivation?: string | null
    decideurs: PosteDecideur[]
}

/**
 * Étape envoyée au backend. `id` est null pour une nouvelle étape, ou
 * renseigné pour une étape existante à mettre à jour. Le champ `niveau`
 * n'apparaît pas : le backend le déduit de la position dans la liste.
 */
export interface EtapeValidationRequest {
    id: number | null
    designation: string
    description?: string | null
    obligatoire: boolean
    retour: boolean
    idPostesDecideurs: number[]
}

export interface Procedure {
    id: number
    designation: string
    description?: string | null
    actif: boolean
    dateCreation?: string | null
    dateModification?: string | null
    dateDesactivation?: string | null
    etapes: EtapeValidation[]
}

export interface ProcedureRequest {
    designation: string
    description?: string | null
    etapes: EtapeValidationRequest[]
}
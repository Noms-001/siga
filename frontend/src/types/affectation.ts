import type { ReferenceDTO } from '@/types/common'

export interface AffectationActivite {
    id: number
    dateAffectation: string
    dateDesaffectation: string | null
    role: ReferenceDTO | null
    utilisateur: ReferenceDTO | null
    active: boolean
}

export interface UtilisateurAffectable {
    id: number
    nom: string
    prenom: string
}

export interface AffectationRequest {
    utilisateurId: number
    roleId: number
}
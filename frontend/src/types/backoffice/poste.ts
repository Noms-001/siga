import type { PorteePermission } from './permission'

export interface Poste {
    id: number
    nom: string
    effectifPrevu: number
    effectifReel: number | null
    isMetier: boolean
    actif: boolean
    dateCreation?: string | null
    dateModification?: string | null
    dateDesactivation?: string | null
}

export interface PosteRequest {
    nom: string
    effectifPrevu: number
    effectifReel: number | null
    isMetier: boolean
}

export interface PermissionPourPoste {
    idPermission: number
    designation: string
    ressource: string
    action: string
    actif: boolean
    portee: PorteePermission
}

export interface AffecterPermissionRequest {
    idPermission: number
    portee: PorteePermission
}

export interface ModifierPorteeRequest {
    portee: PorteePermission
}
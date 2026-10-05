export interface Role {
    id: number
    code: string
    designation: string
    description?: string | null
    actif: boolean
    dateCreation?: string | null
    dateModification?: string | null
    dateDesactivation?: string | null
}

export interface RoleRequest {
    code: string
    designation: string
    description?: string | null
}
export interface Departement {
    id: number
    code: string
    nom: string
    description?: string | null
}

export interface DepartementRequest {
    code: string
    nom: string
    description?: string | null
}
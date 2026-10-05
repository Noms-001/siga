export interface DepartementSummary {
    id: number
    code: string
    nom: string
}

export interface Service {
    id: number
    nom: string
    description?: string | null
    actif: boolean
    dateDesactivation?: string | null
    departement: DepartementSummary
}

export interface ServiceRequest {
    nom: string
    description?: string | null
    idDepartement: number
}
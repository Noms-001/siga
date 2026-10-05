export interface ServiceSummary {
    id: number
    nom: string
    actif: boolean
    departement: {
        id: number
        code: string
        nom: string
    }
}

export interface TypeActivite {
    id: number
    designation: string
    description?: string | null
    actif: boolean
    dateCreation?: string | null
    dateDesactivation?: string | null
    services: ServiceSummary[]
}

export interface TypeActiviteRequest {
    designation: string
    description?: string | null
    idServices: number[]
}
export interface Site {
    id: number
    nom: string
    actif: boolean
    dateCreation?: string | null
    dateDesactivation?: string | null
}

export interface SiteRequest {
    nom: string
}
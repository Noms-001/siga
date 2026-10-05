export interface Priorite {
    id: number
    code: string
    libelle: string
    description?: string | null
    actif: boolean
    dateCreation?: string | null
    dateDesactivation?: string | null
}
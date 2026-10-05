export interface Statut {
    id: number
    code: string
    libelle: string
    description?: string | null
    actif: boolean
    dateCreation?: string | null
    dateModification?: string | null
    dateDesactivation?: string | null
}
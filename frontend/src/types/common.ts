/**
 * Types partagés dans toute l'application.
 */

export interface ReferenceDTO {
    id: number | null
    code: string | null
    libelle: string | null
    annee: number | null
}

export interface PageResponse<T> {
    content: T[]
    page: number
    size: number
    totalElements: number
    totalPages: number
}
import { get, type ApiResponse } from '@/services/api-client'
import type { Indicateur, ValeurIndicateur } from '@/types/backoffice/indicateur'

const BASE = '/indicateurs'

export interface PageResponse<T> {
    content: T[]
    page: number
    size: number
    totalElements: number
    totalPages: number
}

export interface ListeIndicateursFrontParams {
    actif?: boolean
    type?: string
    search?: string
}

function query(params: object): string {
    const qs = new URLSearchParams()
    for (const [k, v] of Object.entries(params)) {
        if (v === undefined || v === null || v === '') continue
        qs.set(k, String(v))
    }
    const q = qs.toString()
    return q ? `?${q}` : ''
}

/**
 * Liste des indicateurs pour le frontoffice.
 *
 * Distinct du service backoffice : chemins différents, périmètre
 * différent (pas d'écriture). Les types TS sont partagés — le DTO backend
 * est identique — mais les deux services restent séparés pour que
 * l'évolution de l'un ne tire pas l'autre.
 */
export function listerIndicateursFront(
    params: ListeIndicateursFrontParams = {}
): Promise<ApiResponse<Indicateur[]>> {
    return get<Indicateur[]>(`${BASE}${query(params)}`)
}

export function getIndicateurFront(id: number): Promise<ApiResponse<Indicateur>> {
    return get<Indicateur>(`${BASE}/${id}`)
}

export async function listerValeursIndicateur(
    id: number
): Promise<ApiResponse<ValeurIndicateur[]>> {
    const res = await get<PageResponse<ValeurIndicateur>>(
        `${BASE}/${id}/valeurs?page=0&size=100`
    )
    if (!res.success) return res
    return { success: true, data: res.data.content }
}
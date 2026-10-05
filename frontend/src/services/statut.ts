import { get, type ApiResponse } from '@/services/api-client'
import type { Statut } from '@/types/backoffice/statut'

const BASE = '/backoffice/statuts'

export function listerStatuts(inclureInactifs = false): Promise<ApiResponse<Statut[]>> {
    const query = inclureInactifs ? '?inclureInactifs=true' : ''
    return get<Statut[]>(`${BASE}${query}`)
}

export function getStatut(id: number): Promise<ApiResponse<Statut>> {
    return get<Statut>(`${BASE}/${id}`)
}
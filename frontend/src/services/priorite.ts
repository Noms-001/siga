import { get, type ApiResponse } from '@/services/api-client'
import type { Priorite } from '@/types/backoffice/priorite'

const BASE = '/backoffice/priorites'

export function listerPriorites(inclureInactifs = false): Promise<ApiResponse<Priorite[]>> {
    const query = inclureInactifs ? '?inclureInactifs=true' : ''
    return get<Priorite[]>(`${BASE}${query}`)
}

export function getPriorite(id: number): Promise<ApiResponse<Priorite>> {
    return get<Priorite>(`${BASE}/${id}`)
}
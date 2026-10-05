import { get, post, put, patch, type ApiResponse } from '@/services/api-client'
import type { Poste, PosteRequest } from '@/types/backoffice/poste'

const BASE = '/backoffice/postes'

export interface ListePostesParams {
    inclureInactifs?: boolean
}

export function listerPostes(params: ListePostesParams = {}): Promise<ApiResponse<Poste[]>> {
    const qs = new URLSearchParams()
    if (params.inclureInactifs) qs.set('inclureInactifs', 'true')
    const query = qs.toString()
    return get<Poste[]>(query ? `${BASE}?${query}` : BASE)
}

export function getPoste(id: number): Promise<ApiResponse<Poste>> {
    return get<Poste>(`${BASE}/${id}`)
}

export function creerPoste(payload: PosteRequest): Promise<ApiResponse<Poste>> {
    return post<Poste>(BASE, payload)
}

export function modifierPoste(id: number, payload: PosteRequest): Promise<ApiResponse<Poste>> {
    return put<Poste>(`${BASE}/${id}`, payload)
}

export function desactiverPoste(id: number): Promise<ApiResponse<Poste>> {
    return patch<Poste>(`${BASE}/${id}/desactiver`)
}

export function activerPoste(id: number): Promise<ApiResponse<Poste>> {
    return patch<Poste>(`${BASE}/${id}/activer`)
}
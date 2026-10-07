import { get, post, put, patch, type ApiResponse } from '@/services/api-client'
import type { Procedure, ProcedureRequest, EtapeValidation } from '@/types/backoffice/procedure'

const BASE = '/backoffice/procedures'

export interface ListeProceduresParams {
    inclureInactifs?: boolean
}

export function listerProcedures(
    params: ListeProceduresParams = {}
): Promise<ApiResponse<Procedure[]>> {
    const qs = new URLSearchParams()
    if (params.inclureInactifs) qs.set('inclureInactifs', 'true')
    const query = qs.toString()
    return get<Procedure[]>(query ? `${BASE}?${query}` : BASE)
}

export function getProcedure(id: number): Promise<ApiResponse<Procedure>> {
    return get<Procedure>(`${BASE}/${id}`)
}

export function creerProcedure(payload: ProcedureRequest): Promise<ApiResponse<Procedure>> {
    return post<Procedure>(BASE, payload)
}

export function modifierProcedure(
    id: number,
    payload: ProcedureRequest
): Promise<ApiResponse<Procedure>> {
    return put<Procedure>(`${BASE}/${id}`, payload)
}

export function desactiverProcedure(id: number): Promise<ApiResponse<Procedure>> {
    return patch<Procedure>(`${BASE}/${id}/desactiver`)
}

export function activerProcedure(id: number): Promise<ApiResponse<Procedure>> {
    return patch<Procedure>(`${BASE}/${id}/activer`)
}
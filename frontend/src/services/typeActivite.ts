import { get, post, put, patch, type ApiResponse } from '@/services/api-client'
import type { TypeActivite, TypeActiviteRequest } from '@/types/backoffice/typeActivite'

const BASE = '/backoffice/types-activite'

export interface ListeTypesActiviteParams {
    /** null/undefined → toutes ; true → actifs ; false → inactifs. */
    actif?: boolean | null
    idService?: number
}

export function listerTypesActivite(
    params: ListeTypesActiviteParams = {}
): Promise<ApiResponse<TypeActivite[]>> {
    const qs = new URLSearchParams()
    if (params.actif === true || params.actif === false) {
        qs.set('actif', String(params.actif))
    }
    if (params.idService != null) qs.set('idService', String(params.idService))
    const query = qs.toString()
    return get<TypeActivite[]>(query ? `${BASE}?${query}` : BASE)
}

export function getTypeActivite(id: number): Promise<ApiResponse<TypeActivite>> {
    return get<TypeActivite>(`${BASE}/${id}`)
}

export function creerTypeActivite(payload: TypeActiviteRequest): Promise<ApiResponse<TypeActivite>> {
    return post<TypeActivite>(BASE, payload)
}

export function modifierTypeActivite(
    id: number,
    payload: TypeActiviteRequest
): Promise<ApiResponse<TypeActivite>> {
    return put<TypeActivite>(`${BASE}/${id}`, payload)
}

export function desactiverTypeActivite(id: number): Promise<ApiResponse<TypeActivite>> {
    return patch<TypeActivite>(`${BASE}/${id}/desactiver`)
}

export function activerTypeActivite(id: number): Promise<ApiResponse<TypeActivite>> {
    return patch<TypeActivite>(`${BASE}/${id}/activer`)
}
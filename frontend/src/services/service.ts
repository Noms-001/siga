import { get, post, put, patch, type ApiResponse } from '@/services/api-client'
import type { Service, ServiceRequest } from '@/types/backoffice/service'

const BASE = '/backoffice/services'

export interface ListeServicesParams {
    inclureInactifs?: boolean
    idDepartement?: number
}

export function listerServices(params: ListeServicesParams = {}): Promise<ApiResponse<Service[]>> {
    const qs = new URLSearchParams()
    if (params.inclureInactifs) qs.set('inclureInactifs', 'true')
    if (params.idDepartement != null) qs.set('idDepartement', String(params.idDepartement))
    const query = qs.toString()
    return get<Service[]>(query ? `${BASE}?${query}` : BASE)
}

export function getService(id: number): Promise<ApiResponse<Service>> {
    return get<Service>(`${BASE}/${id}`)
}

export function creerService(payload: ServiceRequest): Promise<ApiResponse<Service>> {
    return post<Service>(BASE, payload)
}

export function modifierService(id: number, payload: ServiceRequest): Promise<ApiResponse<Service>> {
    return put<Service>(`${BASE}/${id}`, payload)
}

export function desactiverService(id: number): Promise<ApiResponse<Service>> {
    return patch<Service>(`${BASE}/${id}/desactiver`)
}

export function activerService(id: number): Promise<ApiResponse<Service>> {
    return patch<Service>(`${BASE}/${id}/activer`)
}
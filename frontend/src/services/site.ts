import { get, post, put, patch, type ApiResponse } from '@/services/api-client'
import type { Site, SiteRequest } from '@/types/backoffice/site'

const BASE = '/backoffice/sites'

export interface ListeSitesParams { inclureInactifs?: boolean }

export function listerSites(params: ListeSitesParams = {}): Promise<ApiResponse<Site[]>> {
    const qs = new URLSearchParams()
    if (params.inclureInactifs) qs.set('inclureInactifs', 'true')
    const query = qs.toString()
    return get<Site[]>(query ? `${BASE}?${query}` : BASE)
}

export function getSite(id: number): Promise<ApiResponse<Site>> {
    return get<Site>(`${BASE}/${id}`)
}

export function creerSite(payload: SiteRequest): Promise<ApiResponse<Site>> {
    return post<Site>(BASE, payload)
}

export function modifierSite(id: number, payload: SiteRequest): Promise<ApiResponse<Site>> {
    return put<Site>(`${BASE}/${id}`, payload)
}

export function desactiverSite(id: number): Promise<ApiResponse<Site>> {
    return patch<Site>(`${BASE}/${id}/desactiver`)
}

export function activerSite(id: number): Promise<ApiResponse<Site>> {
    return patch<Site>(`${BASE}/${id}/activer`)
}
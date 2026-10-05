import { get, post, put, patch, type ApiResponse } from '@/services/api-client'
import type { Role, RoleRequest } from '@/types/backoffice/role'

const BASE = '/backoffice/roles'

export interface ListeRolesParams {
    inclureInactifs?: boolean
}

export function listerRoles(params: ListeRolesParams = {}): Promise<ApiResponse<Role[]>> {
    const qs = new URLSearchParams()
    if (params.inclureInactifs) qs.set('inclureInactifs', 'true')
    const query = qs.toString()
    return get<Role[]>(query ? `${BASE}?${query}` : BASE)
}

export function getRole(id: number): Promise<ApiResponse<Role>> {
    return get<Role>(`${BASE}/${id}`)
}

export function creerRole(payload: RoleRequest): Promise<ApiResponse<Role>> {
    return post<Role>(BASE, payload)
}

export function modifierRole(id: number, payload: RoleRequest): Promise<ApiResponse<Role>> {
    return put<Role>(`${BASE}/${id}`, payload)
}

export function desactiverRole(id: number): Promise<ApiResponse<Role>> {
    return patch<Role>(`${BASE}/${id}/desactiver`)
}

export function activerRole(id: number): Promise<ApiResponse<Role>> {
    return patch<Role>(`${BASE}/${id}/activer`)
}
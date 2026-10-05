import { get, type ApiResponse } from '@/services/api-client'
import type {
    Permission,
    PermissionDetail,
    PermissionFiltres,
} from '@/types/backoffice/permission'

const BASE = '/backoffice/permissions'

export interface ListePermissionsParams {
    /** null / undefined → toutes ; true → actives ; false → inactives. */
    actif?: boolean | null
    ressource?: string
    action?: string
}

export function listerPermissions(
    params: ListePermissionsParams = {}
): Promise<ApiResponse<Permission[]>> {
    const qs = new URLSearchParams()
    if (params.actif === true || params.actif === false) {
        qs.set('actif', String(params.actif))
    }
    if (params.ressource) qs.set('ressource', params.ressource)
    if (params.action) qs.set('action', params.action)
    const query = qs.toString()
    return get<Permission[]>(query ? `${BASE}?${query}` : BASE)
}

export function getPermission(id: number): Promise<ApiResponse<PermissionDetail>> {
    return get<PermissionDetail>(`${BASE}/${id}`)
}

export function getPermissionFiltres(): Promise<ApiResponse<PermissionFiltres>> {
    return get<PermissionFiltres>(`${BASE}/filtres`)
}
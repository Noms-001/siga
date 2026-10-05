import { del, get, post, put, type ApiResponse } from '@/services/api-client'
import type {
    AffecterPermissionRequest,
    ModifierPorteeRequest,
    PermissionPourPoste,
} from '@/types/backoffice/poste'

function base(idPoste: number): string {
    return `/backoffice/postes/${idPoste}/permissions`
}

export function listerPermissionsDuPoste(
    idPoste: number
): Promise<ApiResponse<PermissionPourPoste[]>> {
    return get<PermissionPourPoste[]>(base(idPoste))
}

export function affecterPermission(
    idPoste: number,
    payload: AffecterPermissionRequest
): Promise<ApiResponse<PermissionPourPoste>> {
    return post<PermissionPourPoste>(base(idPoste), payload)
}

export function modifierPorteePermission(
    idPoste: number,
    idPermission: number,
    payload: ModifierPorteeRequest
): Promise<ApiResponse<PermissionPourPoste>> {
    return put<PermissionPourPoste>(`${base(idPoste)}/${idPermission}`, payload)
}

export function retirerPermission(
    idPoste: number,
    idPermission: number
): Promise<ApiResponse<void>> {
    return del<void>(`${base(idPoste)}/${idPermission}`)
}
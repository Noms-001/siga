import { get, post, del, type ApiResponse } from '@/services/api-client'
import type { PageResponse } from '@/types/common'
import type { ActiviteListItem } from '@/types/activite' // ou le type réel
import type {
    AffectationActivite,
    AffectationRequest,
    UtilisateurAffectable,
} from '@/types/affectation'

const BASE = '/activites/affectations'

// ✅ Après — accepte n'importe quel objet sans exiger d'index signature
function query(params: object | undefined): string {
    if (!params) return ''
    const qs = new URLSearchParams()
    for (const [k, v] of Object.entries(params)) {
        if (v === undefined || v === null || v === '') continue
        qs.set(k, String(v))
    }
    const q = qs.toString()
    return q ? `?${q}` : ''
}

export interface ListeNonAffecteesParams {
    pta?: boolean
    search?: string
    serviceId?: number
    annee?: number
    trimestre?: number
    page?: number
    size?: number
}

export function listerActivitesNonAffectees(
    params: ListeNonAffecteesParams = {}
): Promise<ApiResponse<PageResponse<ActiviteListItem>>> {
    return get<PageResponse<ActiviteListItem>>(`${BASE}/non-affectees${query(params)}`)
}

export function listerAffectationsActivite(
    idActivite: number
): Promise<ApiResponse<AffectationActivite[]>> {
    return get<AffectationActivite[]>(`${BASE}/${idActivite}`)
}

export function listerUtilisateursDisponibles(
    idActivite: number
): Promise<ApiResponse<UtilisateurAffectable[]>> {
    return get<UtilisateurAffectable[]>(`${BASE}/${idActivite}/utilisateurs-disponibles`)
}

export function affecterActivite(
    idActivite: number,
    payload: AffectationRequest
): Promise<ApiResponse<AffectationActivite>> {
    return post<AffectationActivite>(`${BASE}/${idActivite}`, payload)
}

export function desaffecterActivite(
    idActivite: number,
    idAffectation: number
): Promise<ApiResponse<void>> {
    return del<void>(`${BASE}/${idActivite}/${idAffectation}`)
}

export interface AffectationBatchRequest {
    affectations: Array<{ utilisateurId: number; roleId: number }>
}

export function affecterActivitesBatch(
    idActivite: number,
    payload: AffectationBatchRequest
): Promise<ApiResponse<AffectationActivite[]>> {
    return post<AffectationActivite[]>(`${BASE}/${idActivite}/batch`, payload)
}

export function listerAffectationsSousActivite(
    idActivite: number,
    idSousActivite: number
): Promise<ApiResponse<AffectationActivite[]>> {
    return get<AffectationActivite[]>(
        `${BASE}/${idActivite}/sous-activites/${idSousActivite}`)
}

export function affecterSousActivitesBatch(
    idActivite: number,
    idSousActivite: number,
    payload: AffectationBatchRequest
): Promise<ApiResponse<AffectationActivite[]>> {
    return post<AffectationActivite[]>(
        `${BASE}/${idActivite}/sous-activites/${idSousActivite}/batch`, payload)
}

export function desaffecterSousActivite(
    idActivite: number,
    idSousActivite: number,
    idAffectation: number
): Promise<ApiResponse<void>> {
    return del<void>(
        `${BASE}/${idActivite}/sous-activites/${idSousActivite}/${idAffectation}`)
}
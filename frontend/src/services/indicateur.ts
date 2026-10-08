import { get, post, put, patch, type ApiResponse } from '@/services/api-client'
import type {
    Indicateur,
    IndicateurFiltres,
    IndicateurRequest,
} from '@/types/backoffice/indicateur'

const BASE = '/backoffice/indicateurs'

export interface ListeIndicateursParams {
    actif?: boolean | null
    type?: string
    search?: string
}

function query(params: object): string {
    const qs = new URLSearchParams()
    for (const [k, v] of Object.entries(params)) {
        if (v === undefined || v === null || v === '') continue
        qs.set(k, String(v))
    }
    const q = qs.toString()
    return q ? `?${q}` : ''
}

export function listerIndicateurs(
    params: ListeIndicateursParams = {}
): Promise<ApiResponse<Indicateur[]>> {
    return get<Indicateur[]>(`${BASE}${query(params)}`)
}

export function getIndicateur(id: number): Promise<ApiResponse<Indicateur>> {
    return get<Indicateur>(`${BASE}/${id}`)
}

export function getIndicateurFiltres(): Promise<ApiResponse<IndicateurFiltres>> {
    return get<IndicateurFiltres>(`${BASE}/filtres`)
}

export function creerIndicateur(
    payload: IndicateurRequest
): Promise<ApiResponse<Indicateur>> {
    return post<Indicateur>(BASE, payload)
}

export function modifierIndicateur(
    id: number,
    payload: IndicateurRequest
): Promise<ApiResponse<Indicateur>> {
    return put<Indicateur>(`${BASE}/${id}`, payload)
}

export function desactiverIndicateur(id: number): Promise<ApiResponse<Indicateur>> {
    return patch<Indicateur>(`${BASE}/${id}/desactiver`)
}

export function activerIndicateur(id: number): Promise<ApiResponse<Indicateur>> {
    return patch<Indicateur>(`${BASE}/${id}/activer`)
}
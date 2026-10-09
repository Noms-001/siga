import { get, post, type ApiResponse } from '@/services/api-client'
import type {
    Signalement,
    SignalementDetail,
    SignalementFiltres,
    SignalementRequest,
} from '@/types/backoffice/signalement'

const BASE = '/signalements'

function query(params: object): string {
    const qs = new URLSearchParams()
    for (const [k, v] of Object.entries(params)) {
        if (v === undefined || v === null || v === '') continue
        qs.set(k, String(v))
    }
    const q = qs.toString()
    return q ? `?${q}` : ''
}

export interface ListeSignalementsParams {
    /** Liste en CSV : "INCIDENT,RISQUE" */
    types?: string
    search?: string
    avecPlan?: boolean
    annee?: number
}

export function listerSignalements(
    params: ListeSignalementsParams = {}
): Promise<ApiResponse<Signalement[]>> {
    return get<Signalement[]>(`${BASE}${query(params)}`)
}

export function getSignalement(id: number): Promise<ApiResponse<SignalementDetail>> {
    return get<SignalementDetail>(`${BASE}/${id}`)
}

export function getSignalementFiltres(): Promise<ApiResponse<SignalementFiltres>> {
    return get<SignalementFiltres>(`${BASE}/filtres`)
}

export function creerSignalement(
    payload: SignalementRequest
): Promise<ApiResponse<Signalement>> {
    return post<Signalement>(BASE, payload)
}
import { get, post, type ApiResponse } from '@/services/api-client'
import type {
    PageResponse,
    PlanActionFiltres,
    PlanActionListItem,
    AvancementPlanAction,
    AvancementSaisieRequest,
    PlanActionDetail,
    PlanActionEcritureResponse,
    PlanActionRequest,
    SuiviGlobalPlanAction,
} from '@/types/planaction'

const BASE = '/plans-action'

function query(params: PlanActionFiltres & { page?: number; size?: number }): string {
    const qs = new URLSearchParams()
    for (const [k, v] of Object.entries(params)) {
        if (v === undefined || v === null || v === '') continue
        qs.set(k, String(v))
    }
    const q = qs.toString()
    return q ? `?${q}` : ''
}

export interface ListePlansParams extends PlanActionFiltres {
    page?: number
    size?: number
}

export function listerPlans(
    params: ListePlansParams = {}
): Promise<ApiResponse<PageResponse<PlanActionListItem>>> {
    return get<PageResponse<PlanActionListItem>>(`${BASE}${query(params)}`)
}

export function listerMesBrouillons(
    page = 0,
    size = 20
): Promise<ApiResponse<PageResponse<PlanActionListItem>>> {
    return get<PageResponse<PlanActionListItem>>(
        `${BASE}/mes-brouillons${query({ page, size }) as string}`
    )
}

export function detailPlan(id: number): Promise<ApiResponse<PlanActionDetail>> {
    return get<PlanActionDetail>(`${BASE}/${id}`)
}

export function listerAvancements(
    id: number
): Promise<ApiResponse<AvancementPlanAction[]>> {
    return get<AvancementPlanAction[]>(`${BASE}/${id}/avancements`)
}

export function avancementCourant(
    id: number
): Promise<ApiResponse<AvancementPlanAction>> {
    return get<AvancementPlanAction>(`${BASE}/${id}/avancement-courant`)
}

export function saisirAvancement(
    id: number,
    payload: AvancementSaisieRequest
): Promise<ApiResponse<AvancementPlanAction>> {
    return post<AvancementPlanAction>(`${BASE}/${id}/avancements`, payload)
}

export function creerPlan(
    payload: PlanActionRequest
): Promise<ApiResponse<PlanActionEcritureResponse>> {
    return post<PlanActionEcritureResponse>(BASE, payload)
}

export function getSuiviGlobalPlanAction(): Promise<ApiResponse<SuiviGlobalPlanAction>> {
    return get<SuiviGlobalPlanAction>(`${BASE}/suivi-global`)
}
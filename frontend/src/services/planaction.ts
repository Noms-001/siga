import { get, post, type ApiResponse } from '@/services/api-client'
import type { PeriodeDashboard } from '@/types/dashboard'
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

export function listerAValider(
    page = 0,
    size = 20
): Promise<ApiResponse<PageResponse<PlanActionListItem>>> {
    return get<PageResponse<PlanActionListItem>>(
        `${BASE}/a-valider${query({ page, size }) as string}`
    )
}

export function listerADemarrer(
    page = 0,
    size = 20
): Promise<ApiResponse<PageResponse<PlanActionListItem>>> {
    return get<PageResponse<PlanActionListItem>>(
        `${BASE}/a-demarrer${query({ page, size }) as string}`
    )
}

export function soumettreValidationPlan(
    id: number,
    commentaire?: string | null
): Promise<ApiResponse<PlanActionDetail>> {
    const qs = commentaire ? `?commentaire=${encodeURIComponent(commentaire)}` : ''
    return post<PlanActionDetail>(`${BASE}/${id}/soumettre-validation${qs}`, {})
}

export function validerPlan(
    id: number,
    rejeter: boolean,
    commentaire?: string | null
): Promise<ApiResponse<PlanActionDetail>> {
    const params = new URLSearchParams()
    params.set('rejeter', String(rejeter))
    if (commentaire) params.set('commentaire', commentaire)
    return post<PlanActionDetail>(`${BASE}/${id}/valider?${params.toString()}`, {})
}

export function getSuiviGlobalPlanAction(
    periode: PeriodeDashboard
): Promise<ApiResponse<SuiviGlobalPlanAction>> {
    const qs = new URLSearchParams()
    qs.set('dateDebut', periode.debut)
    qs.set('dateFin', periode.fin)
    return get<SuiviGlobalPlanAction>(`${BASE}/suivi-global?${qs.toString()}`)
}

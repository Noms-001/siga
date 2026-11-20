import { get, type ApiResponse } from '@/services/api-client'
import type {
    DashboardAvancement,
    DashboardEcheances,
    DashboardKpi,
    DashboardPerimetre,
    DashboardPlanActionOrigine,
    DashboardPtaComparaison,
    DashboardService,
    DashboardStatut,
    DashboardStatutPriorite,
    DashboardTypeActivite,
    PeriodeDashboard,
} from '@/types/dashboard'

/**
 * Construit une query string à partir d'un objet de paramètres.
 *
 * Typé sur `object` et non `Record<string, unknown>` : une interface
 * nommée n'est pas assignable à un Record sans index signature, alors
 * qu'elle l'est à un paramètre de type `object`. Le comportement runtime
 * est identique.
 */
function query(params: object): string {
    const qs = new URLSearchParams()
    for (const [k, v] of Object.entries(params)) {
        if (v === undefined || v === null || v === '') continue
        qs.set(k, String(v))
    }
    const q = qs.toString()
    return q ? `?${q}` : ''
}

/**
 * Paramètres temporels communs à tous les endpoints du dashboard.
 * Le backend applique la même période à chaque bloc.
 */
function periodeQuery(periode: PeriodeDashboard, serviceId?: number): string {
    return query({
        dateDebut: periode.debut,
        dateFin: periode.fin,
        serviceId,
    })
}

/* ------------------------------------------------------------------ */
/*  Chaque endpoint a sa fonction, pour que le dashboard puisse les   */
/*  appeler en parallèle et gérer les erreurs indépendamment.         */
/* ------------------------------------------------------------------ */

export function getDashboardPerimetre(): Promise<ApiResponse<DashboardPerimetre>> {
    return get<DashboardPerimetre>('/dashboard/perimetre')
}

export function getDashboardKpi(
    periode: PeriodeDashboard
): Promise<ApiResponse<DashboardKpi>> {
    return get<DashboardKpi>(`/dashboard/kpi${periodeQuery(periode)}`)
}

export function getDashboardAvancement(
    periode: PeriodeDashboard
): Promise<ApiResponse<DashboardAvancement>> {
    return get<DashboardAvancement>(`/dashboard/avancement${periodeQuery(periode)}`)
}

export function getDashboardStatuts(
    periode: PeriodeDashboard
): Promise<ApiResponse<DashboardStatut[]>> {
    return get<DashboardStatut[]>(`/dashboard/statuts${periodeQuery(periode)}`)
}

export function getDashboardStatutsPriorites(
    periode: PeriodeDashboard,
    serviceId?: number
): Promise<ApiResponse<DashboardStatutPriorite[]>> {
    return get<DashboardStatutPriorite[]>(
        `/dashboard/statuts-priorites${periodeQuery(periode, serviceId)}`
    )
}

export function getDashboardServices(
    periode: PeriodeDashboard
): Promise<ApiResponse<DashboardService[]>> {
    return get<DashboardService[]>(`/dashboard/services${periodeQuery(periode)}`)
}

export function getDashboardEcheances(
    periode: PeriodeDashboard,
    jours?: number
): Promise<ApiResponse<DashboardEcheances>> {
    return get<DashboardEcheances>(
        `/dashboard/echeances${query({
            dateDebut: periode.debut,
            dateFin: periode.fin,
            jours,
        })}`
    )
}

export function getDashboardPta(
    periode: PeriodeDashboard
): Promise<ApiResponse<DashboardPtaComparaison>> {
    return get<DashboardPtaComparaison>(`/dashboard/pta${periodeQuery(periode)}`)
}

export function getDashboardTypesActivite(
    periode: PeriodeDashboard,
    serviceId?: number
): Promise<ApiResponse<DashboardTypeActivite[]>> {
    return get<DashboardTypeActivite[]>(
        `/dashboard/types-activite${periodeQuery(periode, serviceId)}`
    )
}

export function getDashboardPlanActionOrigine(
    periode: PeriodeDashboard
): Promise<ApiResponse<DashboardPlanActionOrigine[]>> {
    return get<DashboardPlanActionOrigine[]>(
        `/dashboard/plans-action/avancement-origine${periodeQuery(periode)}`
    )
}

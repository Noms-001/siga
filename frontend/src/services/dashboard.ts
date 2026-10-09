import { get, type ApiResponse } from '@/services/api-client'
import type {
    DashboardAvancement,
    DashboardEcheances,
    DashboardKpi,
    DashboardPriorite,
    DashboardPtaComparaison,
    DashboardRisquesIncidents,
    DashboardService,
    DashboardStatut,
    DashboardValidation,
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

/* ------------------------------------------------------------------ */
/*  Chaque endpoint a sa fonction, pour que le dashboard puisse les   */
/*  appeler en parallèle et gérer les erreurs indépendamment.         */
/* ------------------------------------------------------------------ */

export function getDashboardKpi(
    annee?: number
): Promise<ApiResponse<DashboardKpi>> {
    return get<DashboardKpi>(`/dashboard/kpi${query({ annee })}`)
}

export function getDashboardAvancement(
    annee?: number
): Promise<ApiResponse<DashboardAvancement>> {
    return get<DashboardAvancement>(`/dashboard/avancement${query({ annee })}`)
}

export function getDashboardStatuts(
    annee?: number
): Promise<ApiResponse<DashboardStatut[]>> {
    return get<DashboardStatut[]>(`/dashboard/statuts${query({ annee })}`)
}

export function getDashboardServices(
    annee?: number
): Promise<ApiResponse<DashboardService[]>> {
    return get<DashboardService[]>(`/dashboard/services${query({ annee })}`)
}

export function getDashboardPriorites(
    annee?: number,
    limit?: number
): Promise<ApiResponse<DashboardPriorite[]>> {
    return get<DashboardPriorite[]>(
        `/dashboard/priorites${query({ annee, limit })}`
    )
}

export function getDashboardValidations(
    annee?: number
): Promise<ApiResponse<DashboardValidation[]>> {
    return get<DashboardValidation[]>(`/dashboard/validations${query({ annee })}`)
}

export function getDashboardEcheances(
    annee?: number,
    jours?: number
): Promise<ApiResponse<DashboardEcheances>> {
    return get<DashboardEcheances>(
        `/dashboard/echeances${query({ annee, jours })}`
    )
}

export function getDashboardPta(
    annee?: number
): Promise<ApiResponse<DashboardPtaComparaison>> {
    return get<DashboardPtaComparaison>(`/dashboard/pta${query({ annee })}`)
}

export function getDashboardRisquesIncidents(
    annee?: number
): Promise<ApiResponse<DashboardRisquesIncidents>> {
    return get<DashboardRisquesIncidents>(
        `/dashboard/risques-incidents${query({ annee })}`
    )
}
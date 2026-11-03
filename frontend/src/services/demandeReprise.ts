import { get, post, type ApiResponse } from '@/services/api-client'
import type {
    CreerDemandeRequest,
    DecisionDemandeRequest,
    DemandeRepriseActivite,
    DemandeReprisePlan,
} from '@/types/demandeReprise'

const BASE_ACTIVITE = '/demandes-reprise'
const BASE_PLAN = '/demandes-reprise-plans'

/* -------------------- Demandes d'ACTIVITÉ -------------------- */

export function creerDemandeRepriseActivite(
    idActivite: number,
    payload: CreerDemandeRequest = {}
): Promise<ApiResponse<DemandeRepriseActivite>> {
    return post<DemandeRepriseActivite>(
        `${BASE_ACTIVITE}/activites/${idActivite}`,
        payload
    )
}

export function listerHistoriqueActivite(
    idActivite: number
): Promise<ApiResponse<DemandeRepriseActivite[]>> {
    return get<DemandeRepriseActivite[]>(`${BASE_ACTIVITE}/activites/${idActivite}`)
}

export function mesDemandesActivites(): Promise<ApiResponse<DemandeRepriseActivite[]>> {
    return get<DemandeRepriseActivite[]>(`${BASE_ACTIVITE}/mes-demandes`)
}

export function demandesActivitesATraiter(): Promise<ApiResponse<DemandeRepriseActivite[]>> {
    return get<DemandeRepriseActivite[]>(`${BASE_ACTIVITE}/a-traiter`)
}

export function accepterDemandeActivite(
    idDemande: number,
    payload: DecisionDemandeRequest = {}
): Promise<ApiResponse<DemandeRepriseActivite>> {
    return post<DemandeRepriseActivite>(`${BASE_ACTIVITE}/${idDemande}/accepter`, payload)
}

export function refuserDemandeActivite(
    idDemande: number,
    payload: DecisionDemandeRequest = {}
): Promise<ApiResponse<DemandeRepriseActivite>> {
    return post<DemandeRepriseActivite>(`${BASE_ACTIVITE}/${idDemande}/refuser`, payload)
}

export function annulerDemandeActivite(
    idDemande: number
): Promise<ApiResponse<DemandeRepriseActivite>> {
    return post<DemandeRepriseActivite>(`${BASE_ACTIVITE}/${idDemande}/annuler`, {})
}

/* -------------------- Demandes de PLAN -------------------- */

export function creerDemandeReprisePlan(
    idPlanAction: number,
    payload: CreerDemandeRequest = {}
): Promise<ApiResponse<DemandeReprisePlan>> {
    return post<DemandeReprisePlan>(
        `${BASE_PLAN}/plans/${idPlanAction}`,
        payload
    )
}

export function listerHistoriquePlan(
    idPlanAction: number
): Promise<ApiResponse<DemandeReprisePlan[]>> {
    return get<DemandeReprisePlan[]>(`${BASE_PLAN}/plans/${idPlanAction}`)
}

export function mesDemandesPlans(): Promise<ApiResponse<DemandeReprisePlan[]>> {
    return get<DemandeReprisePlan[]>(`${BASE_PLAN}/mes-demandes`)
}

export function demandesPlansATraiter(): Promise<ApiResponse<DemandeReprisePlan[]>> {
    return get<DemandeReprisePlan[]>(`${BASE_PLAN}/a-traiter`)
}

export function accepterDemandePlan(
    idDemande: number,
    payload: DecisionDemandeRequest = {}
): Promise<ApiResponse<DemandeReprisePlan>> {
    return post<DemandeReprisePlan>(`${BASE_PLAN}/${idDemande}/accepter`, payload)
}

export function refuserDemandePlan(
    idDemande: number,
    payload: DecisionDemandeRequest = {}
): Promise<ApiResponse<DemandeReprisePlan>> {
    return post<DemandeReprisePlan>(`${BASE_PLAN}/${idDemande}/refuser`, payload)
}

export function annulerDemandePlan(
    idDemande: number
): Promise<ApiResponse<DemandeReprisePlan>> {
    return post<DemandeReprisePlan>(`${BASE_PLAN}/${idDemande}/annuler`, {})
}
import { get, post, patch, type ApiResponse } from '@/services/api-client'
import type { Utilisateur, UtilisateurRequest } from '@/types/backoffice/utilisateur'

const BASE = '/backoffice/utilisateurs'

export interface ListeUtilisateursParams {
    actif?: boolean | null
    idDepartement?: number
    idService?: number
}

export function listerUtilisateurs(
    params: ListeUtilisateursParams = {}
): Promise<ApiResponse<Utilisateur[]>> {
    const qs = new URLSearchParams()
    if (params.actif === true || params.actif === false) {
        qs.set('actif', String(params.actif))
    }
    if (params.idDepartement != null) qs.set('idDepartement', String(params.idDepartement))
    if (params.idService != null) qs.set('idService', String(params.idService))
    const query = qs.toString()
    return get<Utilisateur[]>(query ? `${BASE}?${query}` : BASE)
}

export function getUtilisateur(id: number): Promise<ApiResponse<Utilisateur>> {
    return get<Utilisateur>(`${BASE}/${id}`)
}

export function creerUtilisateur(payload: UtilisateurRequest): Promise<ApiResponse<Utilisateur>> {
    return post<Utilisateur>(BASE, payload)
}

export function desactiverUtilisateur(id: number): Promise<ApiResponse<Utilisateur>> {
    return patch<Utilisateur>(`${BASE}/${id}/desactiver`)
}

export function activerUtilisateur(id: number): Promise<ApiResponse<Utilisateur>> {
    return patch<Utilisateur>(`${BASE}/${id}/activer`)
}
import { get, put, patch, type ApiResponse } from '@/services/api-client'
import type {
    Parametre,
    ParametreFiltres,
    ParametreUpdateRequest,
} from '@/types/backoffice/parametre'

const BASE = '/backoffice/parametres'

export interface ListeParametresParams {
    /** null/undefined → tous ; true → actifs ; false → inactifs. */
    actif?: boolean | null
    categorie?: string
}

export function listerParametres(
    params: ListeParametresParams = {}
): Promise<ApiResponse<Parametre[]>> {
    const qs = new URLSearchParams()
    if (params.actif === true || params.actif === false) {
        qs.set('actif', String(params.actif))
    }
    if (params.categorie) qs.set('categorie', params.categorie)
    const query = qs.toString()
    return get<Parametre[]>(query ? `${BASE}?${query}` : BASE)
}

export function getParametre(id: number): Promise<ApiResponse<Parametre>> {
    return get<Parametre>(`${BASE}/${id}`)
}

export function getParametreFiltres(): Promise<ApiResponse<ParametreFiltres>> {
    return get<ParametreFiltres>(`${BASE}/filtres`)
}

export function modifierParametre(
    id: number,
    payload: ParametreUpdateRequest
): Promise<ApiResponse<Parametre>> {
    return put<Parametre>(`${BASE}/${id}`, payload)
}

export function desactiverParametre(id: number): Promise<ApiResponse<Parametre>> {
    return patch<Parametre>(`${BASE}/${id}/desactiver`)
}

export function activerParametre(id: number): Promise<ApiResponse<Parametre>> {
    return patch<Parametre>(`${BASE}/${id}/activer`)
}
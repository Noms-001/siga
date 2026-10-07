import { get, post, type ApiResponse } from '@/services/api-client'
import type {
    ActiviteOptions,
    ActiviteStatistiques,
    ActiviteSuivi,
    AvancementRequest,
    AvancementResponse,
    PageResponse,
} from '@/types/suivi'

export interface ListeSuiviParams {
    pta?: boolean
    search?: string
    serviceId?: number
    prioriteId?: number
    statut?: string
    annee?: number
    trimestre?: number
    dateDebut?: string
    dateFin?: string
    page?: number
    size?: number
}

function buildQuery(params: object): string {
    const qs = new URLSearchParams()
    for (const [k, v] of Object.entries(params)) {
        if (v === undefined || v === null || v === '') continue
        qs.set(k, String(v))
    }
    const q = qs.toString()
    return q ? `?${q}` : ''
}

export function listerSuivi(
    params: ListeSuiviParams = {}
): Promise<ApiResponse<PageResponse<ActiviteSuivi>>> {
    return get<PageResponse<ActiviteSuivi>>(
        `/activites/suivi${buildQuery(params)}`
    )
}

export function statistiquesSuivi(
    params: Omit<ListeSuiviParams, 'page' | 'size'> = {}
): Promise<ApiResponse<ActiviteStatistiques>> {
    return get<ActiviteStatistiques>(
        `/activites/statistiques${buildQuery(params)}`
    )
}

export function optionsSuivi(): Promise<ApiResponse<ActiviteOptions>> {
    return get<ActiviteOptions>('/activites/options')
}

export interface AutocompleteItem {
    id: number
    code: string
    libelle: string
    libelleSecondaire?: string | null
}

export function autocompleteActivites(
    q: string
): Promise<ApiResponse<AutocompleteItem[]>> {
    return get<AutocompleteItem[]>(
        `/activites/autocomplete${buildQuery({ q })}`
    )
}

export function enregistrerAvancement(
    idSousActivite: number,
    payload: AvancementRequest
): Promise<ApiResponse<AvancementResponse>> {
    return post<AvancementResponse>(
        `/sous-activites/${idSousActivite}/avancement`,
        payload
    )
}
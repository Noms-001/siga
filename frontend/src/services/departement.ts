import { get, post, put, type ApiResponse } from '@/services/api-client'
import type { Departement, DepartementRequest } from '@/types/backoffice/departement'

const BASE = '/backoffice/departements'

export function listerDepartements(): Promise<ApiResponse<Departement[]>> {
    return get<Departement[]>(BASE)
}

export function getDepartement(id: number): Promise<ApiResponse<Departement>> {
    return get<Departement>(`${BASE}/${id}`)
}

export function creerDepartement(payload: DepartementRequest): Promise<ApiResponse<Departement>> {
    return post<Departement>(BASE, payload)
}

export function modifierDepartement(id: number, payload: DepartementRequest): Promise<ApiResponse<Departement>> {
    return put<Departement>(`${BASE}/${id}`, payload)
}
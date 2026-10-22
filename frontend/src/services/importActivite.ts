import { post, type ApiResponse } from '@/services/api-client'
import type { ActiviteImportRequest, ImportResult, ImportValidationReport } from '@/types/importActivite'

const BASE = '/activites/import'

export function validerImportActivites(
    payload: ActiviteImportRequest
): Promise<ApiResponse<ImportValidationReport>> {
    return post<ImportValidationReport>(`${BASE}/valider`, payload)
}

export function confirmerImportActivites(
    payload: ActiviteImportRequest
): Promise<ApiResponse<ImportResult>> {
    return post<ImportResult>(`${BASE}/confirmer`, payload)
}
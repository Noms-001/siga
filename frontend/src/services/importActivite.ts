import { post, type ApiResponse } from '@/services/api-client'
import type {
    ImportCible,
    ImportResult,
    ImportValidationReport,
} from '@/types/importActivite'

/** Base URL de chaque cible (le backend expose 5 paires d'endpoints). */
export const IMPORT_ENDPOINTS: Record<ImportCible, string> = {
    ACTIVITES: '/activites/import',
    SOUS_ACTIVITES: '/activites/import/sous-activites',
    RESULTATS_INTERMEDIAIRES: '/activites/import/resultats',
    INDICATEURS: '/activites/import/indicateurs',
    ORIGINES: '/activites/import/origines',
}

export function validerImport(
    payload: unknown,
    cible: ImportCible
): Promise<ApiResponse<ImportValidationReport>> {
    return post<ImportValidationReport>(`${IMPORT_ENDPOINTS[cible]}/valider`, payload)
}

export function confirmerImport(
    payload: unknown,
    cible: ImportCible
): Promise<ApiResponse<ImportResult>> {
    return post<ImportResult>(`${IMPORT_ENDPOINTS[cible]}/confirmer`, payload)
}
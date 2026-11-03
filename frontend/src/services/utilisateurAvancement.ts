import { get, type ApiResponse } from '@/services/api-client'
import type { UtilisateurAvancement } from '@/types/utilisateurAvancement'

const BASE = '/utilisateurs'

export function getAvancementUtilisateur(
    id: number,
    annee?: number
): Promise<ApiResponse<UtilisateurAvancement>> {
    const q = annee ? `?annee=${annee}` : ''
    return get<UtilisateurAvancement>(`${BASE}/${id}/avancement-annee${q}`)
}
import { get, type ApiResponse } from '@/services/api-client'
import type {
  MembreSuiviElement,
  MembreSuiviFiltres,
  MembreSuiviGlobal,
  MembreSuiviCategorie,
  MembreSuiviMembres,
} from '@/types/membreSuivi'

function query(filtres: MembreSuiviFiltres, categorie?: MembreSuiviCategorie): string {
  const params = new URLSearchParams()
  if (filtres.annee != null) params.set('annee', String(filtres.annee))
  if (filtres.trimestre != null) params.set('trimestre', String(filtres.trimestre))
  if (filtres.dateDebut) params.set('dateDebut', filtres.dateDebut)
  if (filtres.dateFin) params.set('dateFin', filtres.dateFin)
  if (filtres.idService != null) params.set('idService', String(filtres.idService))
  if (categorie) params.set('categorie', categorie)
  const serialized = params.toString()
  return serialized ? `?${serialized}` : ''
}

export function getMembreSuiviGlobal(
  filtres: MembreSuiviFiltres,
): Promise<ApiResponse<MembreSuiviGlobal>> {
  return get<MembreSuiviGlobal>(`/utilisateurs/suivi/global${query(filtres)}`)
}

export function getMembreSuiviMembres(
  filtres: MembreSuiviFiltres,
  recherche?: string,
): Promise<ApiResponse<MembreSuiviMembres>> {
  const params = new URLSearchParams(query(filtres).replace(/^\?/, ''))
  if (recherche?.trim()) params.set('recherche', recherche.trim())
  const serialized = params.toString()
  return get<MembreSuiviMembres>(`/utilisateurs/suivi/membres${serialized ? `?${serialized}` : ''}`)
}

export function getMembreSuiviElements(
  filtres: MembreSuiviFiltres,
  categorie: MembreSuiviCategorie,
): Promise<ApiResponse<MembreSuiviElement[]>> {
  return get<MembreSuiviElement[]>(`/utilisateurs/suivi/elements${query(filtres, categorie)}`)
}

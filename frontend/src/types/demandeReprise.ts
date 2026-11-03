import type { ReferenceDTO } from '@/types/common'

export type StatutDemande = 'EN_ATTENTE' | 'ACCEPTEE' | 'REFUSEE' | 'ANNULEE'

export interface DemandeRepriseActivite {
    id: number
    dateDemande: string
    dateDecision: string | null
    statut: StatutDemande
    motif: string | null
    commentaireDecision: string | null
    activite: ReferenceDTO
    demandeur: ReferenceDTO
    responsableActuel: ReferenceDTO
    decideur: ReferenceDTO | null
}

export interface DemandeReprisePlan {
    id: number
    dateDemande: string
    dateDecision: string | null
    statut: StatutDemande
    motif: string | null
    commentaireDecision: string | null
    planAction: ReferenceDTO
    demandeur: ReferenceDTO
    responsableActuel: ReferenceDTO
    decideur: ReferenceDTO | null
}

export interface CreerDemandeRequest {
    motif?: string | null
}

export interface DecisionDemandeRequest {
    commentaire?: string | null
}

export const STATUT_DEMANDE: Record<StatutDemande, { libelle: string; classe: string }> = {
    EN_ATTENTE: { libelle: 'En attente', classe: 'badge-statut--attente' },
    ACCEPTEE:   { libelle: 'Acceptée',   classe: 'badge-statut--ok' },
    REFUSEE:    { libelle: 'Refusée',    classe: 'badge-statut--ko' },
    ANNULEE:    { libelle: 'Annulée',    classe: 'badge-statut--annulee' },
}
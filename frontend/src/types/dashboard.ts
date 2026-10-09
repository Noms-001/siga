/* ============================================================
   Types du dashboard SAGA — miroir des DTO backend
   ============================================================
   Chaque interface correspond à un DTO Java réel. Les noms de
   champs sont ceux de la sérialisation JSON, pas des entités.
   Une évolution côté backend doit se refléter ici.
   ============================================================ */

export interface DashboardKpi {
    totalActivites: number
    enCours: number
    terminees: number
    enRetard: number
    nonCommencees: number
    suspendues: number
    annulees: number
    reportees: number
    enAttenteValidation: number
    avancementGlobal: number
}

export interface DashboardAvancement {
    avancementGlobal: number
    totalActivites: number
    totalSousActivites: number
    sousActivitesTerminees: number
}

export interface DashboardStatut {
    statut: string
    libelle: string
    nombre: number
}

export interface DashboardService {
    idService: number
    service: string
    totalActivites: number
    terminees: number
    enCours: number
    enRetard: number
    nonCommencees: number
    avancement: number
}

export interface DashboardPriorite {
    id: number
    code: string
    reference: string | null
    designation: string
    service: string | null
    priorite: string | null
    prioriteCode: string | null
    statut: string | null
    statutLibelle: string | null
    dateDebut: string | null
    dateEcheance: string | null
    avancement: number
    /** 0 si non en retard ; > 0 = nombre de jours de retard. */
    joursRetard: number
}

export interface DashboardValidation {
    idValidation: number
    idActivite: number
    codeActivite: string
    designationActivite: string
    demandeur: string | null
    etape: string | null
    niveau: number | null
    dateDemande: string
    decision: string | null
}

export interface DashboardEcheance {
    id: number
    code: string
    designation: string
    service: string | null
    dateEcheance: string
    /** >= 0 → en retard de N jours ; < 0 → échéance dans N jours. */
    jours: number
    priorite: string | null
    prioriteCode: string | null
    statut: string | null
    statutLibelle: string | null
    avancement: number
}

export interface DashboardEcheances {
    enRetard: DashboardEcheance[]
    echeanceProche: DashboardEcheance[]
}

export interface DashboardPtaSection {
    total: number
    terminees: number
    enCours: number
    enRetard: number
    nonCommencees: number
    avancement: number
}

export interface DashboardPtaComparaison {
    pta: DashboardPtaSection
    nonPta: DashboardPtaSection
}

export interface DashboardPlansAction {
    total: number
    enCours: number
    termines: number
    enRetard: number
}

/**
 * Le backend n'expose aujourd'hui QUE `plansAction`. Les risques et
 * incidents n'ont pas de table en base — le DTO ne les porte pas. Ne pas
 * les inventer côté front, même pour remplir un vide visuel.
 */
export interface DashboardRisquesIncidents {
    plansAction: DashboardPlansAction
}
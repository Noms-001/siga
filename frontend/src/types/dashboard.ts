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

export interface DashboardStatutPriorite {
    statut: string
    statutLibelle: string
    priorite: string
    prioriteLibelle: string
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

export interface DashboardTypeActivite {
    idTypeActivite: number
    typeActivite: string
    totalActivites: number
    avancement: number
}

/** Période globale appliquée à tous les blocs du dashboard. */
export interface PeriodeDashboard {
    debut: string
    fin: string
}

export type DashboardPerimetreNiveau = 'DEPARTEMENT' | 'SERVICE' | 'GLOBAL'

export interface DashboardOption {
    id: number
    code: string | null
    libelle: string
}

export interface DashboardPerimetre {
    niveau: DashboardPerimetreNiveau
    idService: number | null
    idDepartement: number | null
    service: string | null
    services: DashboardOption[]
}

/** Avancement moyen des plans d'action pour une origine. */
export interface DashboardPlanActionOrigine {
    origine: string
    libelle: string
    total: number
    avancement: number
}
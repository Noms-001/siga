export type MembreSuiviCategorie = 'PTA' | 'NON_PTA' | 'PLANS_ACTIONS'

export interface MembreSuiviCategorieStat {
    categorie: MembreSuiviCategorie
    libelle: string
    total: number
    termines: number
    enCours: number
    enRetard: number
    avancement: number
}

export interface MembreSuiviStatut {
    statut: string
    libelle: string
    nombre: number
}

export interface MembreSuiviEvolutionMensuelle {
    mois: string
    pta: number | null
    nonPta: number | null
    plansActions: number | null
}

export interface MembreSuiviServiceStat {
    idService: number
    service: string
    avancementPta: number
    avancementNonPta: number
    avancementPlansActions: number
}

export interface MembreSuiviServiceOption {
    id: number
    nom: string
}

export interface MembreSuiviElement {
    id: number
    categorie: MembreSuiviCategorie
    code: string
    designation: string
    service: string | null
    statut: string | null
    dateEcheance: string | null
    avancement: number
}

export interface MembreSuiviGlobal {
    dateDebut: string
    dateFin: string
    categorieSelectionnee: MembreSuiviCategorie | null
    comparaisonServicesVisible: boolean
    nombreMembres: number
    totalActivitesPta: number
    totalActivitesNonPta: number
    totalPlansActions: number
    avancementGlobal: number
    elementsTermines: number
    elementsEnRetard: number
    categories: MembreSuiviCategorieStat[]
    statutsActivites: MembreSuiviStatut[]
    evolutionMensuelle: MembreSuiviEvolutionMensuelle[]
    services: MembreSuiviServiceStat[]
    servicesDisponibles: MembreSuiviServiceOption[]
    plansMultiServicesNonRepartis: number
}

export interface MembreSuiviMembre {
    idUtilisateur: number
    nom: string
    prenom: string
    nomComplet: string
    idService: number | null
    service: string
    totalActivites: number
    totalPta: number
    totalNonPta: number
    avancementPta: number | null
    avancementNonPta: number | null
    avancementActivites: number | null
    activitesPtaEnRetard: number       // NEW
    activitesNonPtaEnRetard: number    // NEW
    totalPlansActions: number
    avancementPlansActions: number | null
    plansActionsEnRetard: number       // NEW
    avancementGlobal: number | null
    elementsEnRetard: number
}

export interface MembreSuiviMembres {
    dateDebut: string
    dateFin: string
    filtreServiceVisible: boolean
    servicesDisponibles: MembreSuiviServiceOption[]
    membresSuivis: number
    membresAvecActivites: number
    membresAvecPlansActions: number
    membresAvecRetards: number
    membres: MembreSuiviMembre[]
}

export interface MembreSuiviFiltres {
    annee?: number
    trimestre?: number
    dateDebut?: string
    dateFin?: string
    idService?: number
    categorie?: MembreSuiviCategorie | null
}

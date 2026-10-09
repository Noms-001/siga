export interface Signalement {
    id: number
    code: string
    designation: string
    typeOrigine: string
    description?: string | null
    dateCreation: string
    idUtilisateur: number
    nomUtilisateur: string
    prenomUtilisateur: string
}

export interface SignalementRequest {
    code: string
    designation: string
    typeOrigine: string
    description?: string | null
}

export interface SignalementFiltres {
    types: string[]
}

export interface PlanActionResume {
    id: number
    code: string
    designation: string
    estPrincipale: boolean | null
}

/**
 * Vue détaillée : mêmes champs que Signalement, plus les plans d'action
 * rattachés. Le type est séparé pour que la liste ne s'attende pas à
 * recevoir `plansAction` — elle ne les a pas, et TypeScript doit le dire.
 */
export interface SignalementDetail extends Signalement {
    plansAction: PlanActionResume[]
}
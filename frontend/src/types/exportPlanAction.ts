export interface ServiceExportPlan {
    id: number
    nom: string
}

export interface OrigineExportPlan {
    code: string | null
    designation: string | null
    typeOrigine: string | null
    estPrincipale: boolean
}

export interface ActiviteLieeExportPlan {
    id: number
    code: string | null
    designation: string | null
    serviceId: number | null
    serviceNom: string | null
    origineCode: string | null
    typeOrigine: string | null
}

export interface SuiviPlanActionExport {
    dateChangement: string | null
    utilisateur: string | null
    valeurPourcentage: number
    commentaire: string | null
    motif: string | null
    statutCode: string | null
    statutLibelle: string | null
}

export interface PlanActionExport {
    id: number
    code: string
    designation: string
    dateDebutPrevue: string | null
    dateFinPrevue: string | null
    dateDebutReelle: string | null
    dateFinReelle: string | null
    statutCode: string | null
    statutLibelle: string | null
    avancement: number
    serviceId: number | null
    serviceNom: string | null
    servicesLies: string | null
    responsables: string[]
    motifReprise: string | null
    origines: OrigineExportPlan[]
    activites: ActiviteLieeExportPlan[]
    suivis: SuiviPlanActionExport[]
}

export interface PlanActionExportResponse {
    direction: string
    departement: string | null
    departementCode: string | null
    service: string | null
    serviceId: number | null
    annee: number
    dateGeneration: string
    services: ServiceExportPlan[]
    plans: PlanActionExport[]
}

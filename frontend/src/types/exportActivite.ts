export interface SuiviExport {
    dateChangement: string | null
    utilisateur: string | null
    valeurPourcentage: number | null
    commentaire: string | null
    statutCode: string | null
    statutLibelle: string | null
}

export interface SousActiviteExport {
    id: number
    code: string
    designation: string
    dateDebutPrevue: string | null
    dateFinPrevue: string | null
    statutCode: string | null
    statutLibelle: string | null
    responsables: string[]
    avancement: number
    suivis: SuiviExport[]
}

export interface ActiviteExport {
    id: number
    code: string
    reference: string | null
    designation: string
    pta: boolean
    objectifSpecifiqueCode: string | null
    objectifSpecifiqueDesignation: string | null
    procedure: string | null
    dateDebutPrevue: string | null
    dateFinPrevue: string | null
    statutCode: string | null
    statutLibelle: string | null
    serviceId: number | null
    serviceNom: string | null
    priorite: string | null
    responsables: string[]
    avancement: number
    sousActivites: SousActiviteExport[]
}

export interface ServiceExport {
    id: number
    nom: string
}

export interface ActiviteExportResponse {
    direction: string
    departement: string | null
    departementCode: string | null
    service: string | null
    serviceId: number | null
    annee: number
    dateGeneration: string
    services: ServiceExport[]
    activites: ActiviteExport[]
}

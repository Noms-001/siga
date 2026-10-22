export interface ActiviteImportRequest {
    version: string
    service: number
    procedure: number
    annee: number
    typeImport: 'PTA' | 'NON_PTA'
    activitesDejaValidees: boolean
    activites: ActiviteImport[]
}

export interface ActiviteImport {
    code?: string
    reference?: string
    designation: string
    dateDebut?: string
    dateFin?: string
    dateDebutReelle?: string
    dateFinReelle?: string
    priorite?: string
    typeActivite?: string
    site?: string
    objectifSpecifique?: { code?: string; designation?: string }
    sousActivites?: SousActiviteImport[]
    resultatsIntermediaires?: { designation: string }[]
    indicateurs?: { designation: string }[]
    origines?: { designation: string }[]
}

export interface SousActiviteImport {
    code?: string
    designation?: string
    dateDebut?: string
    dateFin?: string
    dateDebutReelle?: string
    dateFinReelle?: string
    livrables?: { designation?: string; description?: string }[]
}

export interface ImportValidationIssue {
    activite: string | null
    champ: string | null
    message: string
}

export interface ImportValidationReport {
    valide: boolean
    message: string
    nombreActivites: number
    nombreErreurs: number
    nombreAvertissements: number
    erreurs: ImportValidationIssue[]
    avertissements: ImportValidationIssue[]
    apercu: {
        code: string | null
        reference: string | null
        designation: string
        dateDebut: string | null
        dateFin: string | null
        nombreSousActivites: number
        nombreLivrables: number
        statutInitial: string
    }[]
}

export interface ImportResult {
    message: string
    nombreActivitesImportees: number
    nombreSousActivitesImportees: number
    nombreLivrablesImportes: number
    nombreResultatsIntermediairesImportes: number
    codesGeneres: string[]
    indicateursCrees: string[]
    originesCreees: string[]
    statutInitial: string
}
/* ==========================================================================
   Requêtes — une par cible
   ========================================================================== */

export type ImportCible =
    | 'ACTIVITES'
    | 'SOUS_ACTIVITES'
    | 'RESULTATS_INTERMEDIAIRES'
    | 'INDICATEURS'

/* --- Cible ACTIVITES (inchangée) --------------------------------------- */

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
    /** Facultatif : généré par le serveur ou validé s'il est fourni. */
    code?: string
    designation: string
    priorite?: string
    typeActivite?: string
    site?: string
    objectifSpecifique?: { code?: string; designation?: string }
    /** Facultatif. Si absent, l'activité est créée sans sous-activités ni dates. */
    sousActivites?: SousActiviteImport[]
    resultatsIntermediaires?: { designation: string }[]
    indicateurs?: { designation: string }[]
}

/* --- Cible SOUS_ACTIVITES ---------------------------------------------- */

export interface SousActiviteImportRequest {
    version: string
    annee: number
    activites: ActiviteSousActivitesGroup[]
}

export interface ActiviteSousActivitesGroup {
    reference: string
    sousActivites: SousActiviteImport[]
}

/* --- Cible RESULTATS_INTERMEDIAIRES ------------------------------------ */

export interface ResultatIntermediaireImportRequest {
    version: string
    activites: ActiviteResultatsGroup[]
}

export interface ActiviteResultatsGroup {
    reference: string
    resultatsIntermediaires: ResultatIntermediaireImport[]
}

export interface ResultatIntermediaireImport {
    designation: string
}

/* --- Cible INDICATEURS -------------------------------------------------- */

export interface IndicateurImportRequest {
    version: string
    activites: ActiviteIndicateursGroup[]
}

export interface ActiviteIndicateursGroup {
    reference: string
    indicateurs: IndicateurImport[]
}

export interface IndicateurImport {
    designation: string
    typeIndicateur?: string
    uniteMesure?: string
    frequenceVerification?: string
    frequenceAgregation?: string
    definition?: string
    methodeDetermination?: string
    objectif?: string
    valeurCible?: number | null
    seuilMin?: number | null
    seuilMax?: number | null
}

/* --- Éléments communs -------------------------------------------------- */

export interface SousActiviteImport {
    /** Facultatif : généré depuis les références de l'activité parente. */
    code?: string
    designation?: string
    dateDebut?: string
    dateFin?: string
    livrables?: { designation?: string; description?: string }[]
}

/* ==========================================================================
   Rapports
   ========================================================================== */

export interface ImportValidationIssue {
    activite: string | null
    champ: string | null
    message: string
}

export interface ApercuActivite {
    code: string | null
    reference: string | null
    designation: string
    dateDebut: string | null
    dateFin: string | null
    nombreSousActivites: number
    nombreLivrables: number
    statutInitial: string
}

export interface ImportValidationReport {
    valide: boolean
    message: string
    nombreActivites: number
    nombreErreurs: number
    nombreAvertissements: number
    erreurs: ImportValidationIssue[]
    avertissements: ImportValidationIssue[]
    apercu: ApercuActivite[] | null
}

export interface ImportResult {
    message: string
    nombreActivitesImportees: number
    nombreSousActivitesImportees: number
    nombreLivrablesImportes: number
    nombreResultatsIntermediairesImportes: number
    codesGeneres: string[]
    indicateursCrees: string[]
    statutInitial: string | null
}
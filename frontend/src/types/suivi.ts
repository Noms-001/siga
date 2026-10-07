export interface Reference {
    id: number
    code?: string | null
    libelle: string
}

export interface SousActiviteSuivi {
    id: number
    code: string
    designation: string
    /** Dernière valeur connue, ou 0 si jamais évaluée. */
    avancement: number
}

export interface AvancementRequest {
    valeurPourcentage: number
    commentaire?: string | null
}

export interface AvancementResponse {
    idSousActivite: number
    /** Null si premier relevé (jamais évaluée). */
    ancienneValeur: number | null
    avancement: number
    commentaire?: string | null
    dateChangement: string
    statutCode: string
    statutLibelle: string
    avancementActivite: number
}

export interface ResponsableSuivi {
    idUtilisateur: number
    nom: string
    prenom: string
}

export interface ActiviteSuivi {
    id: number
    code: string
    reference?: string | null
    designation: string
    dateDebutPrevue: string
    dateFinPrevue?: string | null
    dateDebutReelle?: string | null
    dateFinReelle?: string | null
    service: Reference | null
    priorite: Reference | null
    statut: Reference | null
    avancement: number
    responsable: ResponsableSuivi | null
    sousActivites: SousActiviteSuivi[]
}

export interface ActiviteStatistiques {
    total: number
    nonCommencees: number
    enCours: number
    terminees: number
    enRetard: number
    annulees: number
    reportees: number
    suspendues: number
}

export interface ReferenceOption {
    id: number
    code?: string | null
    libelle: string
}

export interface ActiviteOptions {
    services: ReferenceOption[]
    priorites: ReferenceOption[]
    typesActivite: ReferenceOption[]
    sites: ReferenceOption[]
    statuts: ReferenceOption[]
}

export interface PageResponse<T> {
    content: T[]
    page: number
    size: number
    totalElements: number
    totalPages: number
    first: boolean
    last: boolean
}
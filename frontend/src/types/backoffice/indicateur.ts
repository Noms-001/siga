export interface Indicateur {
    id: number
    code: string
    designation: string
    codeHopex?: string | null
    indicateurHopex?: string | null
    typeIndicateur?: string | null
    uniteMesure?: string | null
    frequenceVerification?: string | null
    frequenceAggregation?: string | null
    definition?: string | null
    methodeDetermination?: string | null
    objectif?: string | null
    valeurCible?: number | null
    seuilMin?: number | null
    seuilMax?: number | null
    actif: boolean
}

export interface IndicateurRequest {
    code: string
    designation: string
    codeHopex?: string | null
    indicateurHopex?: string | null
    typeIndicateur?: string | null
    uniteMesure?: string | null
    frequenceVerification?: string | null
    frequenceAggregation?: string | null
    definition?: string | null
    methodeDetermination?: string | null
    objectif?: string | null
    valeurCible?: number | null
    seuilMin?: number | null
    seuilMax?: number | null
    actif?: boolean | null
}

export interface IndicateurFiltres {
    types: string[]
}

export interface ValeurIndicateur {
    id: number
    valeur: number
    periodeDebut: string
    periodeFin: string
    commentaire?: string | null
    dateSaisie: string
    idUtilisateur: number
    nomUtilisateur: string
    prenomUtilisateur: string
}
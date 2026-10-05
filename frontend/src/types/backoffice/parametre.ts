export type ParametreTypeValeur =
    | 'STRING'
    | 'INTEGER'
    | 'DECIMAL'
    | 'BOOLEAN'
    | 'DATE'
    | 'DATETIME'

export interface Parametre {
    id: number
    code: string
    designation: string
    valeur: string
    typeValeur: ParametreTypeValeur
    description?: string | null
    categorie?: string | null
    modifiable: boolean
    actif: boolean
    dateCreation?: string | null
    dateModification?: string | null
    dateDesactivation?: string | null
    idUtilisateurModification?: number | null
    nomUtilisateurModification?: string | null
}

export interface ParametreUpdateRequest {
    valeur: string
}

export interface ParametreFiltres {
    categories: string[]
}

/**
 * Libellés d'affichage pour les types de valeur.
 *
 * Les noms techniques (STRING, DATETIME…) sont destinés au backend ; à
 * l'écran, l'utilisateur backoffice lit un type par son nom usuel.
 */
export const TYPE_VALEUR_LABELS: Record<ParametreTypeValeur, string> = {
    STRING: 'Texte',
    INTEGER: 'Entier',
    DECIMAL: 'Décimal',
    BOOLEAN: 'Booléen',
    DATE: 'Date',
    DATETIME: 'Date et heure',
}

export function formatTypeValeur(t: ParametreTypeValeur): string {
    return TYPE_VALEUR_LABELS[t] ?? t
}
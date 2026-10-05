export type PorteePermission =
    | 'UTILISATEUR'
    | 'SERVICE'
    | 'DEPARTEMENT'
    | 'TOUS'

export interface Permission {
    id: number
    designation: string
    description?: string | null
    ressource: string
    action: string
    actif: boolean
    dateDesactivation?: string | null
}

export interface PostePermissionInfo {
    idPoste: number
    nom: string
    portee: PorteePermission | null
}

export interface PermissionDetail extends Permission {
    postes: PostePermissionInfo[]
}

export interface PermissionFiltres {
    ressources: string[]
    actions: string[]
}

/**
 * Libellés d'affichage pour les portées.
 *
 * Le backend renvoie les noms techniques (UTILISATEUR, SERVICE…). Le backoffice
 * parle à des humains : on traduit ici, à un seul endroit.
 */
export const PORTEE_LABELS: Record<PorteePermission, string> = {
    UTILISATEUR: 'Utilisateur',
    SERVICE: 'Service',
    DEPARTEMENT: 'Département',
    TOUS: 'Global',
}

export function formatPortee(portee: PorteePermission | null): string {
    if (!portee) return 'Non définie'
    return PORTEE_LABELS[portee] ?? portee
}
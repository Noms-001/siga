/**
 * Systeme d'autorisation frontend.
 *
 * Il reprend les principes du backend :
 * - les ressources et actions sont declarees une seule fois ;
 * - une route ou un element de navigation declare ses permissions requises ;
 * - plusieurs permissions sont alternatives (OU logique), ce qui permet
 *   d'exprimer l'heritage d'une ressource (consulter une activite donne
 *   acces a ses sous-activites).
 *
 * Les valeurs sont stockees en minuscules cote backend ; la comparaison est
 * donc insensible a la casse.
 */

export const PermissionResource = {
    ACTIVITE: 'activite',
    SOUS_ACTIVITE: 'sous_activite',
    LIVRABLE: 'livrable',
    FICHIER: 'fichier',
    AVANCEMENT_SOUS_ACTIVITE: 'avancement_sous_activite',
    OBJECTIF_SPECIFIQUE: 'objectif_specifique',
    VALIDATION_ACTIVITE: 'validation_activite',
    VALIDATION_PLAN_ACTION: 'validation_plan_action',
    AFFECTATION_ACTIVITE: 'affectation_activite',
    AFFECTATION_SOUS_ACTIVITE: 'affectation_sous_activite',
    DEMANDE_REPRISE: 'demande_reprise',
    DEMANDE_REPRISE_PLAN: 'demande_reprise_plan',
    PLAN_ACTION: 'plan_action',
    AVANCEMENT_PLAN_ACTION: 'avancement_plan_action',
    INDICATEUR: 'indicateur',
    VALEUR_INDICATEUR: 'valeur_indicateur',
    SIGNALEMENT: 'signalement',
    NOTIFICATION: 'notification',
    DASHBOARD: 'dashboard',
    MEMBRE_SUIVI: 'membre_suivi',
    UTILISATEUR: 'utilisateur',
    DEPARTEMENT: 'departement',
    SERVICE: 'service',
    POSTE: 'poste',
    ROLE: 'role',
    PERMISSION: 'permission',
    POSTE_PERMISSION: 'poste_permission',
    TYPE_ACTIVITE: 'type_activite',
    SITE: 'site',
    PRIORITE: 'priorite',
    STATUT: 'statut',
    PROCEDURE: 'procedure',
    PARAMETRE: 'parametre',
    RESULTAT_INTERMEDIAIRE: 'resultat_intermediaire',
} as const

export const PermissionAction = {
    AFFECTER: 'AFFECTER',
    CREER: 'CREER',
    MODIFIER: 'MODIFIER',
    DESACTIVER: 'DESACTIVER',
    ACTIVER: 'ACTIVER',
    VALIDER: 'VALIDER',
    IMPORTER: 'IMPORTER',
    EXPORTER: 'EXPORTER',
    CONSULTER: 'CONSULTER',
} as const

export type PermissionResource =
    (typeof PermissionResource)[keyof typeof PermissionResource]

export type PermissionAction =
    (typeof PermissionAction)[keyof typeof PermissionAction]

export interface PermissionRequirement {
    resource: PermissionResource
    action: PermissionAction
}

/** Construit une permission requise. */
export function permission(
    resource: PermissionResource,
    action: PermissionAction,
): PermissionRequirement {
    return { resource, action }
}

function permissionKey(
    resource: PermissionResource,
    action: PermissionAction,
): string {
    return `${resource}:${action}`.toLowerCase()
}

/**
 * Verifie qu'au moins une des permissions requises est accordee.
 *
 * Sans permission requise, l'acces est libre : une route non annotee n'est
 * pas soumise au controle applicatif (l'authentification reste assuree par
 * le routeur).
 */
export function hasAnyPermission(
    granted: readonly string[] | undefined,
    required: readonly PermissionRequirement[] | undefined,
): boolean {
    if (!required || required.length === 0) return true

    const grantedKeys = new Set(
        (granted ?? []).map((value) => value.toLowerCase()),
    )

    return required.some(({ resource, action }) =>
        grantedKeys.has(permissionKey(resource, action)),
    )
}

/** Verifie une permission precise. */
export function hasPermission(
    granted: readonly string[] | undefined,
    resource: PermissionResource,
    action: PermissionAction,
): boolean {
    return hasAnyPermission(granted, [{ resource, action }])
}

export interface PermissionRequirement {
    resource: string
    action: string
}

const backofficeResources: Record<string, string> = {
    departements: 'departement',
    services: 'service',
    postes: 'poste',
    roles: 'role',
    'types-activite': 'type_activite',
    sites: 'site',
    indicateurs: 'indicateur',
    priorites: 'priorite',
    statuts: 'statut',
    procedures: 'procedure',
    parametres: 'parametre',
    permissions: 'permission',
    utilisateurs: 'utilisateur',
    activites: 'activite',
    'plans-action': 'plan_action',
}

export function permissionForRoute(path: string): PermissionRequirement | null {
    const normalized = path.replace(/\/$/, '') || '/'

    if (normalized === '/backoffice/dashboard') {
        return { resource: 'dashboard', action: 'READ' }
    }
    if (!normalized.startsWith('/backoffice/')) return null

    const segments = normalized.slice('/backoffice/'.length).split('/')
    const firstSegment = segments[0]
    const resource = firstSegment === 'postes' && segments.includes('permissions')
        ? 'poste_permission'
        : firstSegment ? backofficeResources[firstSegment] : undefined
    if (!resource) return null
    if (segments.includes('nouveau')) return { resource, action: 'CREATE' }
    if (segments.includes('modifier')) return { resource, action: 'UPDATE' }
    return { resource, action: 'READ' }

}

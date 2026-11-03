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

    if (normalized === '/profile' || normalized.startsWith('/profile/')) return null
    if (normalized === '/backoffice/dashboard' || normalized === '/dashboard') {
        return { resource: 'dashboard', action: 'READ' }
    }
    if (normalized === '/activites/importer') {
        return { resource: 'activite', action: 'CREATE' }
    }

    if (normalized.startsWith('/backoffice/')) {
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

    if (normalized === '/validation' || normalized.endsWith('/a-valider')) {
        return { resource: 'validation_activite', action: 'READ' }
    }
    if (normalized.startsWith('/activites/a-affecter')) {
        return { resource: 'affectation_activite', action: 'READ' }
    }
    if (normalized === '/activites/brouillons') {
        return { resource: 'validation_activite', action: 'READ' }
    }
    if (normalized.startsWith('/activites')) {
        if (normalized === '/activites/nouvelle') return { resource: 'activite', action: 'CREATE' }
        if (/^\/activites\/\d+\/modifier$/.test(normalized)) return { resource: 'activite', action: 'UPDATE' }
        if (/^\/activites\/\d+\/sous-activites\//.test(normalized)) {
            return { resource: 'sous_activite', action: 'READ' }
        }
        return { resource: 'activite', action: 'READ' }
    }
    if (normalized.startsWith('/plans-action')) {
        return {
            resource: 'plan_action',
            action: normalized === '/plans-action/nouveau' ? 'CREATE' : 'READ',
        }
    }
    if (normalized.startsWith('/signalements')) {
        return {
            resource: 'signalement',
            action: normalized === '/signalements/nouveau' ? 'CREATE' : 'READ',
        }
    }
    if (normalized.startsWith('/indicateurs')) return { resource: 'indicateur', action: 'READ' }
    if (normalized.startsWith('/notifications')) return { resource: 'notification', action: 'READ' }
    if (normalized.startsWith('/demandes-reprise')) return { resource: 'demande_reprise', action: 'READ' }
    if (/^\/utilisateurs\/\d+\/avancement/.test(normalized)) return { resource: 'utilisateur', action: 'READ' }

    return null
}

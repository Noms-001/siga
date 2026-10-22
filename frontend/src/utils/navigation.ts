import type { RouteLocationNormalizedLoaded } from 'vue-router'

const pageBreadcrumbs: Record<string, string[]> = {
    acceuil: ['Accueil'],
    login: ['Connexion'],
    'backoffice-login': ['Connexion à l’administration'],
    'forgot-password': ['Mot de passe oublié'],
    'reset-password': ['Réinitialisation du mot de passe'],
    activation: ['Activation du compte'],
    dashboard: ['Espace de travail', 'Tableau de bord'],
    profile: ['Espace personnel', 'Mon profil'],
    notifications: ['Espace de travail', 'Notifications'],
    activites: ['Espace de travail', 'Activités'],
    'activites-nouvelle': ['Espace de travail', 'Activités', 'Nouvelle activité'],
    'activite-modifier': ['Espace de travail', 'Activités', 'Modifier une activité'],
    'activites-brouillons': ['Espace de travail', 'Activités', 'Activités créées'],
    'activites-a-valider': ['Espace de travail', 'Activités', 'Activités à valider'],
    'validation-directe': ['Espace de travail', 'Activités', 'Activités à valider'],
    'activite-detail': ['Espace de travail', 'Activités', 'Détail de l’activité'],
    'sous-activite-detail': ['Espace de travail', 'Activités', 'Détail de la sous-activité'],
    'activites-suivi': ['Espace de travail', 'Activités', 'Suivi des activités'],
    indicateurs: ['Espace de travail', 'Indicateurs'],
    'indicateur-detail': ['Espace de travail', 'Indicateurs', 'Détail de l’indicateur'],
    signalements: ['Espace de travail', 'Signalements'],
    'signalement-nouveau': ['Espace de travail', 'Signalements', 'Nouveau signalement'],
    'signalement-detail': ['Espace de travail', 'Signalements', 'Détail du signalement'],
    'backoffice-dashboard': ['Administration', 'Tableau de bord'],
    'backoffice-departements': ['Administration', 'Référentiels', 'Départements'],
    'backoffice-departements-nouveau': ['Administration', 'Référentiels', 'Nouveau département'],
    'backoffice-departements-modifier': ['Administration', 'Référentiels', 'Modifier un département'],
    'backoffice-departements-detail': ['Administration', 'Référentiels', 'Détail du département'],
    'backoffice-services': ['Administration', 'Référentiels', 'Services'],
    'backoffice-services-nouveau': ['Administration', 'Référentiels', 'Nouveau service'],
    'backoffice-services-modifier': ['Administration', 'Référentiels', 'Modifier un service'],
    'backoffice-services-detail': ['Administration', 'Référentiels', 'Détail du service'],
    'backoffice-permissions': ['Administration', 'Autorisations', 'Permissions'],
    'backoffice-permissions-detail': ['Administration', 'Autorisations', 'Détail de la permission'],
    'backoffice-postes': ['Administration', 'Référentiels', 'Postes'],
    'backoffice-postes-nouveau': ['Administration', 'Référentiels', 'Nouveau poste'],
    'backoffice-postes-modifier': ['Administration', 'Référentiels', 'Modifier un poste'],
    'backoffice-postes-detail': ['Administration', 'Référentiels', 'Détail du poste'],
    'backoffice-postes-permissions': ['Administration', 'Autorisations', 'Permissions du poste'],
    'backoffice-roles': ['Administration', 'Autorisations', 'Rôles'],
    'backoffice-roles-nouveau': ['Administration', 'Autorisations', 'Nouveau rôle'],
    'backoffice-roles-modifier': ['Administration', 'Autorisations', 'Modifier un rôle'],
    'backoffice-roles-detail': ['Administration', 'Autorisations', 'Détail du rôle'],
    'backoffice-types-activite': ['Administration', 'Référentiels', 'Types d’activité'],
    'backoffice-types-activite-nouveau': ['Administration', 'Référentiels', 'Nouveau type d’activité'],
    'backoffice-types-activite-modifier': ['Administration', 'Référentiels', 'Modifier un type d’activité'],
    'backoffice-types-activite-detail': ['Administration', 'Référentiels', 'Détail du type d’activité'],
    'backoffice-sites': ['Administration', 'Référentiels', 'Sites'],
    'backoffice-sites-nouveau': ['Administration', 'Référentiels', 'Nouveau site'],
    'backoffice-sites-modifier': ['Administration', 'Référentiels', 'Modifier un site'],
    'backoffice-sites-detail': ['Administration', 'Référentiels', 'Détail du site'],
    'backoffice-priorites': ['Administration', 'Référentiels', 'Priorités'],
    'backoffice-statuts': ['Administration', 'Référentiels', 'Statuts'],
    'backoffice-parametres': ['Administration', 'Paramètres'],
    'backoffice-utilisateurs': ['Administration', 'Gestion', 'Utilisateurs'],
    'backoffice-utilisateurs-nouveau': ['Administration', 'Gestion', 'Nouvel utilisateur'],
    'backoffice-utilisateurs-detail': ['Administration', 'Gestion', 'Détail de l’utilisateur'],
    'backoffice-procedures': ['Administration', 'Configuration', 'Procédures de validation'],
    'backoffice-procedures-nouveau': ['Administration', 'Configuration', 'Nouvelle procédure'],
    'backoffice-procedures-modifier': ['Administration', 'Configuration', 'Modifier une procédure'],
    'backoffice-procedures-detail': ['Administration', 'Configuration', 'Détail de la procédure'],
    'backoffice-indicateurs': ['Administration', 'Référentiels', 'Indicateurs'],
    'backoffice-indicateurs-nouveau': ['Administration', 'Référentiels', 'Nouvel indicateur'],
    'backoffice-indicateurs-modifier': ['Administration', 'Référentiels', 'Modifier un indicateur'],
    'backoffice-indicateurs-detail': ['Administration', 'Référentiels', 'Détail de l’indicateur'],
}

function routeKey(route: Pick<RouteLocationNormalizedLoaded, 'name'>): string {
    return typeof route.name === 'string' ? route.name : ''
}

export function getPageBreadcrumbs(route: Pick<RouteLocationNormalizedLoaded, 'name' | 'path'>): string[] {
    const key = routeKey(route)
    const labels = pageBreadcrumbs[key]
    if (labels) return labels

    const segment = route.path.split('/').filter(Boolean).pop()
    if (!segment) return ['Accueil']

    const fallback = segment
        .replace(/-/g, ' ')
        .replace(/^./u, first => first.toLocaleUpperCase('fr'))
    return [fallback]
}

export function getPageTitle(route: Pick<RouteLocationNormalizedLoaded, 'name' | 'path'>): string {
    const labels = getPageBreadcrumbs(route)
    return labels[labels.length - 1] ?? 'Accueil'
}

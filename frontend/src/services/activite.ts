/**
 * Service API de la liste des activites.
 *
 * Le statut n'a pas de champ dedie dans la page : il est porte par les cards
 * et envoye ici comme parametre. C'est volontaire, pour qu'il n'existe qu'un
 * seul endroit ou appliquer un filtre de statut.
 *
 * Aucune pagination n'est faite ici ni dans les composants : la pagination
 * est appliquee par le backend apres les filtres.
 */
import { get, getBlob } from './api-client'
import type { ApiResponse } from './api-client'
import type {
    ActiviteAutocomplete,
    ActiviteDetail,
    ActiviteFiltres,
    ActiviteListItem,
    ActiviteOptions,
    ActiviteStatistiques,
    PageResult,
    SousActiviteDetail,
    Trimestre,
    VueActivite,
} from '@/types/activite'

/** Annee proposee par defaut a l'ouverture de la page. */
export const ANNEE_DEFAUT = 2027

/**
 * Construit la query string des filtres en ignorant les valeurs vides.
 *
 * Un filtre absent n'est pas envoye : le backend le traite comme "pas de
 * contrainte", ce qui evite de restreindre la requete par accident.
 *
 * Page et size sont ajoutes separement, uniquement pour la liste.
 */
function construireParamsFiltres(
    filtres: ActiviteFiltres,
    vue: VueActivite,
    statut: string | null
): URLSearchParams {
    const params = new URLSearchParams()

    // PTA est la vue par defaut : le backend la sous-entend quand pta est absent.
    if (vue === 'NON_PTA') {
        params.set('pta', 'false')
    }

    if (filtres.search.trim()) {
        params.set('search', filtres.search.trim())
    }

    // Objectif specifique : sans objet hors vue PTA, on ne l'envoie pas.
    if (vue === 'PTA') {
        if (filtres.objectifSpecifiqueId !== null) {
            params.set('objectifSpecifiqueId', String(filtres.objectifSpecifiqueId))
        }

        if (filtres.objectifSearch.trim()) {
            params.set('objectifSearch', filtres.objectifSearch.trim())
        }
    }

    if (filtres.serviceId !== null) {
        params.set('serviceId', String(filtres.serviceId))
    }

    if (filtres.prioriteId !== null) {
        params.set('prioriteId', String(filtres.prioriteId))
    }

    if (filtres.typeActiviteId !== null) {
        params.set('typeActiviteId', String(filtres.typeActiviteId))
    }

    if (filtres.siteId !== null) {
        params.set('siteId', String(filtres.siteId))
    }

    if (statut) {
        params.set('statut', statut)
    }

    if (filtres.annee) {
        params.set('annee', String(filtres.annee))
    }

    if (filtres.trimestre) {
        params.set('trimestre', String(filtres.trimestre))
    }

    if (filtres.dateDebut) {
        params.set('dateDebut', filtres.dateDebut)
    }

    if (filtres.dateFin) {
        params.set('dateFin', filtres.dateFin)
    }

    return params
}

/**
 * Liste paginee et filtree.
 *
 * `page` est en base 1, comme partout dans l'interface : BasePagination
 * affiche 1..N, et la premiere page porte le libelle "1-N sur total".
 * L'API, elle, indexe depuis 0 comme Pageable de Spring. La conversion est
 * faite ici, a la frontiere, et nowhere else.
 *
 * Cette conversion n'est pas cosmetique. Sans elle, la derniere page affichee
 * (N) est envoyee comme N, tombe hors bornes, et revient vide. Le front
 * reessayait alors sur cette meme page, ce qui bouclait. */
export async function listerActivites(
    filtres: ActiviteFiltres,
    vue: VueActivite,
    statut: string | null,
    page: number,
    size: number
) {
    const params = construireParamsFiltres(filtres, vue, statut)
    params.set('page', String(Math.max(0, page - 1)))
    params.set('size', String(size))

    return get<PageResult<ActiviteListItem>>(`/activites?${params.toString()}`)
}

/**
 * Statistiques sur le meme perimetre et les memes filtres que la liste.
 *
 * Les compteurs des cards restent coherents avec le tableau affiche, et
 * reflettent la page courante sans avoir a charger toutes les activites.
 */
export async function statistiquesActivites(
    filtres: ActiviteFiltres,
    vue: VueActivite,
    statut: string | null
) {
    const params = construireParamsFiltres(filtres, vue, statut)

    return get<ActiviteStatistiques>(`/activites/statistiques?${params.toString()}`)
}

/**
 * Referentiels des filtres, groupes par type.
 *
 * services n'est fourni que pour un utilisateur de niveau departement.
 * Son tableau vide signale un utilisateur rattache a un service : le filtre
 * Service ne sera pas propose, sans avoir a le deviner cote front.
 */
export async function chargerOptions() {
    return get<ActiviteOptions>('/activites/options')
}

/** Autocomplete activite : code, reference, designation. */
export async function autocompleterActivites(q: string) {
    const params = new URLSearchParams()
    if (q.trim()) params.set('q', q.trim())

    return get<ActiviteAutocomplete[]>(`/activites/autocomplete?${params.toString()}`)
}

/** Autocomplete objectif specifique : reserve a la vue PTA. */
export async function autocompleterObjectifs(q: string) {
    const params = new URLSearchParams()
    if (q.trim()) params.set('q', q.trim())

    return get<ActiviteAutocomplete[]>(
        `/activites/objectifs/autocomplete?${params.toString()}`
    )
}

/**
 * Detail complet d'une activite.
 *
 * Une seule requete : toutes les collections (sous-activites, livrables,
 * validations, indicateurs, historique...) arrivent imbriquees. Le backend
 * applique le perimetre de la liste : 404 si absent, 403 si hors perimetre.
 */
export async function detailActivite(id: number) {
    return get<ActiviteDetail>(`/activites/${id}`)
}

/**
 * Detail d'une sous-activite du detail d'une activite.
 *
 * La sous-activite doit appartenir a l'activite demandee, elle-meme dans le
 * perimetre de l'appelant : 404 sinon, 403 si l'activite sort du perimetre.
 */
export async function detailSousActivite(idActivite: number, idSousActivite: number) {
    return get<SousActiviteDetail>(
        `/activites/${idActivite}/sous-activites/${idSousActivite}`
    )
}

/**
 * Contenu binaire d'un fichier d'une sous-activité.
 *
 * Même périmètre et mêmes erreurs que detailSousActivite : 404 si le fichier
 * n'appartient pas au chemin, 403 si l'activité sort du périmètre. Le chemin
 * de stockage n'existe pas côté client : seul le blob est renvoyé.
 */
export async function telechargerFichier(
    idActivite: number,
    idSousActivite: number,
    idFichier: number
): Promise<ApiResponse<Blob>> {
    return getBlob(
        `/activites/${idActivite}/sous-activites/${idSousActivite}/fichiers/${idFichier}`
    )
}

export type { Trimestre }

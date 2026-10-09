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
import { getCurrentYear } from '@/utils/date'
import { get, getBlob, post, postForm, put } from './api-client'
import type { ApiResponse } from './api-client'
import {
    BROUILLON,
    EN_ATTENTE_VALIDATION,
    type ActiviteAutocomplete,
    type ActiviteDetail,
    type ActiviteEcriture,
    type ActiviteEcritureResultat,
    type ActiviteFiltres,
    type ActiviteFormulaire,
    type CodePropose,
    type DecisionResultat,
    type LivrableAjout,
    type LivrableDetail,
    type ObjectifSpecifiqueEcriture,
    type ActiviteListItem,
    type ActiviteOptions,
    type ActiviteStatistiques,
    type PageResult,
    type SoumissionResultat,
    type SousActiviteDetail,
    type Trimestre,
    type VueActivite,
    type ValidationActiviteResponse,
} from '@/types/activite'

/** Annee proposee par defaut a l'ouverture de la page. */
export const ANNEE_DEFAUT = getCurrentYear()

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
 * Liste des activites en brouillon, hors circuit de validation.
 *
 * Meme endpoint que la liste, avec le statut BROUILLON impose et SANS
 * annee : un brouillon n'appartient pas encore a un exercice de suivi, donc
 * le restreindre a ANNEE_DEFAUT le ferait disparaitre de la page meme
 * depuis laquelle on le soumet. D'ou un filtre d'annee a 0, que
 * construireParamsFiltres traite comme "aucune contrainte".
 *
 * Aucun filtre de periode non plus (trimestre, dates) : ces bornes servent
 * au suivi d'un exercice, pas a une liste de brouillons a vider.
 */
export async function listerActivitesBrouillon(
    filtres: ActiviteFiltres,
    vue: VueActivite,
    page: number,
    size: number
) {
    const aVider: ActiviteFiltres = {
        ...filtres,
        annee: 0,
        trimestre: null,
        dateDebut: '',
        dateFin: '',
    }

    return listerActivites(aVider, vue, BROUILLON, page, size)
}

/**
 * Decider du sort d'une activite soumise (POST /api/activites/{id}/decision).
 *
 * Le statut resultant n'est PAS envoye par le client : il se deduit de la
 * decision. VALIDE publie, REJETE sort du circuit, RETOUR_MODIFICATION rend la
 * main a l'auteur. Un client qui choisirait lui-meme le statut resultant
 * pourrait faire valider une activite sans passer par VALIDE.
 *
 * Le commentaire accompagne REJETE et RETOUR_MODIFICATION, et le backend le
 * reclame : sans motif, un refus n'est pas opposable a l'auteur. VALIDE s'en
 * passe, d'ou un champ facultatif ici et une verification la-bas.
 *
 * 404 activite inexistante, 403 hors perimetre, 400 si la decision est inconnue
 * ou le motif manquant, 409 si l'activite n'attend plus de decision. Le 409 est
 * le cas de deux validateurs ouverts sur la meme activite : la page doit alors
 * retirer la carte plutot que d'insister.
 */
export async function deciderValidation(
    id: number,
    decision: string,
    commentaire: string | null
): Promise<ApiResponse<DecisionResultat>> {
    return post<DecisionResultat>(`/activites/${id}/decision`, {
        decision,
        commentaire,
    })
}

/**
 * Soumettre une activite a la validation.
 *
 * POST sans corps : le statut et l'identifiant viennent du chemin, jamais du
 * client. Un corps aurait permit de soumettre au statut de son choix.
 *
 * 404 activite inexistante, 403 hors perimetre, 409 si elle n'est plus un
 * brouillon -- le cas de deux onglets ouverts sur la meme activite. Le 409
 * n'est pas une panne : la page doit le dire et retirer la carte.
 */
export async function soumettreValidation(
    id: number,
    commentaire?: string | null
): Promise<ApiResponse<SoumissionResultat>> {
    const params = new URLSearchParams()
    if (commentaire?.trim()) {
        params.set('commentaire', commentaire.trim())
    }
    const qs = params.toString()

    return post<SoumissionResultat>(
        `/activites/${id}/soumettre-validation${qs ? `?${qs}` : ''}`,
        null
    )
}

/**
 * Creer une activite en brouillon.
 *
 * Le statut n'est pas dans le corps : il est pose a BROUILLON par le
 * backend. L'envoyer aurait permis d'introduire une activite directement
 * dans le suivi.
 */
export async function creerActivite(
    corps: ActiviteEcriture
): Promise<ApiResponse<ActiviteEcritureResultat>> {
    return post<ActiviteEcritureResultat>('/activites', corps)
}

/**
 * Modifier une activite.
 *
 * PUT et non PATCH : le corps est l'etat complet de ce que le formulaire
 * edite. Une ecriture partielle donnerait au client le pouvoir d'effacer un
 * champ en ne l'envoyant pas.
 *
 * 404 activite inexistante, 403 hors perimetre ou sans permission, 409 si
 * elle n'est plus un brouillon.
 */
export async function modifierActivite(
    id: number,
    corps: ActiviteEcriture
): Promise<ApiResponse<ActiviteEcritureResultat>> {
    return put<ActiviteEcritureResultat>(`/activites/${id}`, corps)
}

/**
 * Ce que le formulaire de modification doit pre-remplir.
 *
 * Le statut est renvoye pour que la page sache s'il a le droit d'afficher
 * le bouton Modifier. Ce n'est qu'un affichage : l'enregistrement reverifie.
 */
export async function formulaireActivite(
    id: number
): Promise<ApiResponse<ActiviteFormulaire>> {
    return get<ActiviteFormulaire>(`/activites/${id}/formulaire`)
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
 * Code propose pour une activite NON PTA.
 *
 * La regle du jeu de donnees est une sequence par annee -- A-2027-01 a
 * A-2027-50 -- et le backend renvoie le code complet plutot que le numero :
 * le format lui appartient, et le formulaire n'a pas a le recomposer.
 *
 * L'annee est passee en parametre parce qu'une NON PTA n'a pas d'objectif :
 * rien dans l'activite ne dit sur quelle annee on travaille, tant que
 * l'utilisateur n'a pas renseigne sa date de debut.
 */
export async function prochainCodeNonPta(annee: number) {
    return get<CodePropose>(`/activites/non-pta/prochain-code?annee=${annee}`)
}

/**
 * Creer un objectif specifique, depuis le bouton place a cote du champ.
 *
 * La reponse est l'objectif cree, au format de l'autocomplete : le
 * formulaire le selectionne directement, sans relire la liste, et le code et la
 * reference de l'activite sont proposes dans la foulée. `prochainNumero` vaut
 * 1, un objectif ne venant que d'etre cree.
 */
export async function creerObjectifSpecifique(objectif: ObjectifSpecifiqueEcriture) {
    return post<ActiviteAutocomplete>('/activites/objectifs', objectif)
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
/**
 * Ajouter un livrable a une sous-activite, avec les fichiers a deposer.
 *
 * LE JSON ET LES OCTETS VOYAGENT SEPAREMENT
 *
 * Le livrable part dans la partie `livrable`, en JSON : c'est ce qui permet au
 * serveur de le valider avec les memes regles que les autres corps de requete,
 * et de nommer le champ fautif dans l'erreur. Les fichiers partaient dans
 * `fichiers`. Les assembler dans un seul JSON obligerait a encoder les octets
 * en base64 -- un tiers de plus a transferer, et une copie en memoire de plus
 * que le fichier lui-meme.
 *
 * `Content-Type` n'est PAS pose sur le FormData : c'est le navigateur qui
 * doit ajouter la frontiere, et la fixer a la main est le moyen classique de
 * faire rejeter la requete. postForm s en charge.
 *
 * Les fichiers sont facultatifs : un FormData sans entree `fichiers` est
 * envoye tel quel, et le serveur lit un lot vide.
 *
 * Le depot est refuse par le serveur si l'activite n'est plus en cours (409).
 * Ce controle n'est pas repris ici : il est fait sous verrou, sur l'historique,
 * et une regle reecrite dans le formulaire pourrait diverger de la seule qui
 * fait autorite.
 */
export async function creerLivrable(
    idActivite: number,
    idSousActivite: number,
    livrable: LivrableAjout,
    fichiers: File[]
): Promise<ApiResponse<LivrableDetail>> {

    const corps = new FormData()

    corps.append(
        'livrable',
        new Blob([JSON.stringify(livrable)], { type: 'application/json' })
    )

    for (const fichier of fichiers) {
        corps.append('fichiers', fichier, fichier.name)
    }

    return postForm(
        `/activites/${idActivite}/sous-activites/${idSousActivite}/livrables`,
        corps
    )
}

/**
 * Joindre des fichiers a un livrable deja declare.
 *
 * Chemin distinct de creerLivrable, et sans partie JSON : il n'y a aucun champ
 * a saisir ici, le livrable existe deja et son identifiant est dans l'URL.
 * Seuls les octets partent, d'ou un multipart sans "livrable".
 *
 * La liste ne peut pas etre vide : le serveur la refuse, et l'appeler pour
 * rien produirait un succes qui ne change rien a l'affichage. Le refus est donc
 * rendu ici, avant l'envoi.
 */
export async function ajouterFichiersLivrable(
    idActivite: number,
    idSousActivite: number,
    idLivrable: number,
    fichiers: File[]
): Promise<ApiResponse<LivrableDetail>> {

    if (fichiers.length === 0) {
        return {
            success: false,
            data: null,
            error: 'Sélectionnez au moins un fichier à joindre.',
            // Refus rendu ici : aucun appel n'est parti, donc pas de code
            // HTTP. 0 dit "la requete n'a pas eu lieu", ce que 400 ferait
            // croire a un rejet du serveur.
            status: 0,
        }
    }

    const corps = new FormData()

    for (const fichier of fichiers) {
        corps.append('fichiers', fichier, fichier.name)
    }

    return postForm(
        `/activites/${idActivite}/sous-activites/${idSousActivite}`
        + `/livrables/${idLivrable}/fichiers`,
        corps
    )
}

export async function telechargerFichier(
    idActivite: number,
    idSousActivite: number,
    idFichier: number
): Promise<ApiResponse<Blob>> {
    return getBlob(
        `/activites/${idActivite}/sous-activites/${idSousActivite}/fichiers/${idFichier}`
    )
}

/**
 * Liste des activités en attente de validation, groupées par étape.
 *
 * Endpoint dédié (POST /activites/a-valider) et non la liste standard : la
 * structure de réponse diffère. Chaque activité porte son étape courante, sa
 * précédente et sa suivante — ce qui permet à l'écran de proposer "soumettre
 * à l'étape précédente" sans relire le circuit.
 *
 * Pas de pagination : le backend renvoie tout le circuit en une fois, et la
 * file est par nature courte (les décisions sont prises au fil de l'eau).
 *
 * La réponse est une liste de Maps à UNE clé : la désignation de l'étape de
 * validation. La page aplatit cette structure.
 */
export async function listerActivitesAValider(): Promise<
    ApiResponse<PageResult<Record<string, ActiviteListItem>>>
> {
    return get<PageResult<Record<string, ActiviteListItem>>>('/activites/a-valider')
}

/**
 * Valider ou rejeter une activité soumise.
 *
 * UN SEUL ENDPOINT, DEUX ISSUES : `rejeter` à false publie l'activité au
 * suivi, à true la sort définitivement du circuit. Un endpoint dédié par
 * issue aurait dupliqué toute la mécanique de verrou et de périmètre côté
 * backend, pour un booléen qui suffit.
 *
 * Le motif n'est requis que pour le rejet — valider n'a rien à justifier.
 *
 * 404 si l'activité n'existe pas, 403 hors périmètre, 409 si elle n'attend
 * plus de décision (deux validateurs ouverts sur la même file).
 */
export async function validerActivite(
    id: number,
    rejeter: boolean,
    commentaire?: string | null
): Promise<ApiResponse<ValidationActiviteResponse>> {
    const params = new URLSearchParams()
    params.set('rejeter', String(rejeter))
    if (commentaire?.trim()) {
        params.set('commentaire', commentaire.trim())
    }

    return post<ValidationActiviteResponse>(
        `/activites/${id}/valider?${params.toString()}`,
        null
    )
}

/**
 * Renvoyer une activité à l'étape précédente (retour pour modification).
 *
 * `retour=true` et non `false` : c'est l'option qui remet l'activité en
 * rédaction, plutôt que de la faire avancer. Distinct de `/valider` parce
 * que le sens de l'écriture est inverse — un endpoint par direction reste
 * plus lisible qu'un paramètre sur la même route.
 *
 * Un motif est attendu — le backend le rend facultatif, mais renvoyer sans
 * dire pourquoi laisserait l'auteur sans rien à corriger.
 */
export async function soumettreValidationRetour(
    id: number,
    commentaire?: string | null
): Promise<ApiResponse<ValidationActiviteResponse>> {
    const params = new URLSearchParams()
    params.set('retour', 'true')
    if (commentaire?.trim()) {
        params.set('commentaire', commentaire.trim())
    }

    return post<ValidationActiviteResponse>(
        `/activites/${id}/soumettre-validation?${params.toString()}`,
        null
    )
}

export async function listerActivitesASoumettre(): Promise<
    ApiResponse<PageResult<ActiviteListItem>>
> {
    return get<PageResult<ActiviteListItem>>('/activites/a-soumettre')
}

export type { Trimestre }

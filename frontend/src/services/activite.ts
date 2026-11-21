import { getCurrentYear } from '@/utils/date'
import { del, get, getBlob, patch, post, postForm, put } from './api-client'
import type { ApiResponse } from './api-client'
import type { ActiviteExportResponse } from '@/types/exportActivite'
import {
    BROUILLON,
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
    type ActiviteTransitionResultat
} from '@/types/activite'

/** Annee proposee par defaut a l'ouverture de la page. */
export const ANNEE_DEFAUT = getCurrentYear()


export function construireParamsFiltres(
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

export async function exporterActivites(
    filtres: ActiviteFiltres,
    vue: VueActivite,
    statut: string | null
) {
    const params = construireParamsFiltres(filtres, vue, statut)

    return get<ActiviteExportResponse>(`/activites/export?${params.toString()}`)
}

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

export async function creerActivite(
    corps: ActiviteEcriture
): Promise<ApiResponse<ActiviteEcritureResultat>> {
    return post<ActiviteEcritureResultat>('/activites', corps)
}

export async function modifierActivite(
    id: number,
    corps: ActiviteEcriture
): Promise<ApiResponse<ActiviteEcritureResultat>> {
    return put<ActiviteEcritureResultat>(`/activites/${id}`, corps)
}

export async function formulaireActivite(
    id: number
): Promise<ApiResponse<ActiviteFormulaire>> {
    return get<ActiviteFormulaire>(`/activites/${id}/formulaire`)
}

export async function statistiquesActivites(
    filtres: ActiviteFiltres,
    vue: VueActivite,
    statut: string | null
) {
    const params = construireParamsFiltres(filtres, vue, statut)

    return get<ActiviteStatistiques>(`/activites/statistiques?${params.toString()}`)
}

export async function chargerOptions() {
    return get<ActiviteOptions>('/activites/options')
}

/** Autocomplete activite : code, reference, designation. */
export async function autocompleterActivites(q: string, pta: boolean) {
    const params = new URLSearchParams()
    if (q.trim()) params.set('q', q.trim())
    params.set('pta', String(pta))

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

export async function supprimerActivite(id: number): Promise<ApiResponse<void>> {
    return del<void>(`/activites/${id}`)
}

export async function prochainCodeNonPta(annee: number) {
    return get<CodePropose>(`/activites/non-pta/prochain-code?annee=${annee}`)
}

export async function creerObjectifSpecifique(objectif: ObjectifSpecifiqueEcriture) {
    return post<ActiviteAutocomplete>('/activites/objectifs', objectif)
}


export async function detailActivite(id: number) {
    return get<ActiviteDetail>(`/activites/${id}`)
}

export async function detailSousActivite(idActivite: number, idSousActivite: number) {
    return get<SousActiviteDetail>(
        `/activites/${idActivite}/sous-activites/${idSousActivite}`
    )
}

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

export async function listerActivitesAValider(): Promise<
    ApiResponse<PageResult<Record<string, ActiviteListItem>>>
> {
    return get<PageResult<Record<string, ActiviteListItem>>>('/activites/a-valider')
}

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

export async function changerPrioriteActivite(
    idActivite: number,
    idPriorite: number | null
): Promise<ApiResponse<void>> {

    return patch<void>(
        `/activites/${idActivite}/priorite`,
        { idPriorite }
    )
}

export async function demanderReprise(
    idActivite: number,
    motif: string | null
): Promise<ApiResponse<unknown>> {
    // Idem : post<T> retourne ApiResponse<T>.
    return post<unknown>(
        `/demandes-reprise/activites/${idActivite}`,
        { motif }
    )
}

export async function listerActivitesADemarrer(
    filtres: ActiviteFiltres,
    page: number,
    size: number
): Promise<ApiResponse<PageResult<ActiviteListItem>>> {

    const params = construireParamsFiltres(filtres, 'PTA', null)
    params.set('page', String(Math.max(0, page - 1)))
    params.set('size', String(size))
    return get<PageResult<ActiviteListItem>>(`/activites/a-demarrer?${params.toString()}`)
}

export async function demarrerActivite(
    id: number, commentaire?: string | null
): Promise<ApiResponse<ActiviteTransitionResultat>> {
    const params = new URLSearchParams()
    if (commentaire?.trim()) params.set('commentaire', commentaire.trim())
    const qs = params.toString()
    return post<ActiviteTransitionResultat>(
        `/activites/${id}/demarrer${qs ? `?${qs}` : ''}`, null)
}

export async function cloturerActivite(
    id: number, commentaire?: string | null
): Promise<ApiResponse<ActiviteTransitionResultat>> {
    const params = new URLSearchParams()
    if (commentaire?.trim()) params.set('commentaire', commentaire.trim())
    const qs = params.toString()
    return post<ActiviteTransitionResultat>(
        `/activites/${id}/cloturer${qs ? `?${qs}` : ''}`, null)
}

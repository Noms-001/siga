/**
 * Types de la liste des activites.
 *
 * Ils decrivent exactement ce que le backend renvoie (voir
 * ActiviteListItemDTO / ActiviteStatistiquesDTO), et rien de plus.
 *
 * Points importants :
 * - avancement, statut et date de creation sont CALCULES par le backend.
 *   Le front ne les recalcule pas.
 * - objectifSpecifique est null pour une activite NON PTA.
 * - les libelles priorite / statut viennent de la table de reference, ils ne
 *   sont pas codes en dur ici.
 */

/** Reference aplatie d'un objet cible. */
export interface ActiviteReference {
    id: number
    code?: string | null
    libelle?: string | null
    annee?: number | null
}

/** Ligne de la liste. */
export interface ActiviteListItem {
    id: number
    code: string
    reference: string
    designation: string

    dateDebutPrevue: string | null
    dateFinPrevue: string | null
    dateDebutReelle: string | null
    dateFinReelle: string | null

    /** Premiere entree d'historique : sert au tri par recurrence. */
    dateCreation: string | null

    /** null si activite NON PTA. */
    objectifSpecifique: ActiviteReference | null

    service: ActiviteReference | null
    typeActivite: ActiviteReference | null
    site: ActiviteReference | null
    priorite: ActiviteReference | null

    /** Statut courant, issu du dernier historique. null si aucun historique. */
    statut: ActiviteReference | null

    /** 0 a 100, calcule en base. */
    avancement: number
}

/** Compteurs des cards. */
export interface ActiviteStatistiques {
    total: number
    nonCommencees: number
    enCours: number
    terminees: number
    enRetard: number
    annulees: number
    reportees: number
    suspendues: number
}

/** Reponse paginee renvoyee par l'API. */
export interface PageResult<T> {
    content: T[]
    page: number
    size: number
    totalElements: number
    totalPages: number
    first: boolean
    last: boolean
}

/** Option d'un filtre deroulant, depuis les referentiels. */
export interface ActiviteOption {
    id: number
    code?: string | null
    libelle?: string | null
}

/** Suggestion d'autocomplete. */
export interface ActiviteAutocomplete {
    id: number
    code?: string | null
    libelle?: string | null
    libelleSecondaire?: string | null
}

/**
 * Referentiels des filtres, groupes par type.
 *
 * services n'est RENVoye QUE si l'utilisateur est au niveau departement.
 * Son tableau vide signifie qu'il est rattache a un service : le filtre
 * Service n'a donc pas a etre masque cote front, il n'est pas fourni.
 */
export interface ActiviteOptions {
    services: ActiviteOption[]
    priorites: ActiviteOption[]
    typesActivite: ActiviteOption[]
    sites: ActiviteOption[]
    statuts: ActiviteOption[]
}

/** Vue PTA / NON PTA. PTA par defaut. */
export type VueActivite = 'PTA' | 'NON_PTA'

/**
 * "En retard" n'est pas un statut en base : c'est une regle metier.
 * Le backend reconnait ce pseudo-code et applique echeance depassee +
 * statut non terminal.
 */
export const EN_RETARD = 'EN_RETARD'

/** Cartes cliquables, hors total. */
export type CarteStatut =
    | 'nonCommencees'
    | 'enCours'
    | 'terminees'
    | 'enRetard'
    | 'annulees'
    | 'reportees'
    | 'suspendues'

/** Trimestres. */
export type Trimestre = 1 | 2 | 3 | 4

/** Filtres de la page. */
export interface ActiviteFiltres {
    search: string
    objectifSearch: string
    objectifSpecifiqueId: number | null
    serviceId: number | null
    prioriteId: number | null
    typeActiviteId: number | null
    siteId: number | null
    annee: number
    trimestre: Trimestre | null
    dateDebut: string
    dateFin: string
}

// ------------------------------------------------------------------
// Detail d'une activite (GET /api/activites/{id})
//
// Miroir exact des DTO de ActiviteDetailDTO. Regle du backend :
// aucune collection n'est null, une activite sans donnee expose [].
// Les types optionnels ci-dessous ne portent que sur les valeurs
// scalaires que le backend renvoie null (avancementCourant, statut,
// libelles de reference...), jamais sur les listes.
// ------------------------------------------------------------------

/** Identite courte d'un utilisateur, repetee dans le detail. */
export interface UtilisateurResume {
    id: number
    nom: string
    prenom: string
}

/** Releve d'avancement d'une sous-activite. */
export interface AvancementDetail {
    id: number
    /** Pourcentage, de 0 a 100. */
    valeurPourcentage: number | null
    commentaire: string | null
    dateChangement: string | null
    statut: ActiviteReference | null
    utilisateur: UtilisateurResume | null
    /** Vrai pour le seul releve retenu comme avancement courant. */
    etatActuel: boolean
}

/** Affectation d'un utilisateur a une sous-activite, dans un role. */
export interface AffectationDetail {
    id: number
    dateAffectation: string | null
    /** Nulle tant que l'utilisateur est affecte. */
    dateDesaffectation: string | null
    role: ActiviteReference | null
    utilisateur: UtilisateurResume | null
}

/** Fichier depose sur un livrable. cheminFichier n'est jamais expose. */
export interface FichierDetail {
    id: number
    /** Nom de stockage, different de nomOriginal apres un renommage. */
    nomFichier: string
    /** Nom d'origine, tel que fourni par l'utilisateur. */
    nomOriginal: string
    extension: string
    typeMime: string
    taille: number | null
    /** Rang du depot : deux versions d'un meme nomOriginal coexistent. */
    version: number
    dateDepot: string
    utilisateur: UtilisateurResume | null
}

/** Livrable attendu d'une sous-activite, avec ses fichiers. */
export interface LivrableDetail {
    id: number
    designation: string
    description: string | null
    /** Jamais null : une liste vide signifie aucun depot. */
    fichiers: FichierDetail[]
}

/** Sous-activite d'une activite, avec son suivi. */
export interface SousActiviteDetail {
    id: number
    code: string
    designation: string
    dateDebutPrevue: string | null
    dateFinPrevue: string | null
    dateDebutReelle: string | null
    dateFinReelle: string | null
    /** Dernier releve, ou null si jamais evalue (distinct de 0). */
    avancementCourant: AvancementDetail | null
    /** Tous les releves, du plus recent au plus ancien. */
    historiqueAvancement: AvancementDetail[]
    affectations: AffectationDetail[]
    livrables: LivrableDetail[]
}

/**
 * Decision d'une etape de validation (enum PostgreSQL decision_validation).
 * Distinct du statut d'une activite, qui s'ecrit VALIDEE avec un E.
 */
export type DecisionValidation =
    | 'EN_ATTENTE_VALIDATION'
    | 'VALIDE'
    | 'REJETE'
    | 'RETOUR_MODIFICATION'

/** Etape du circuit de validation atteinte par une activite. */
export interface EtapeValidation {
    id: number
    designation: string
    description: string | null
    /** Rang de l'etape dans la procedure : 1 est la premiere. */
    niveau: number
    obligatoire: boolean | null
    procedureLibelle: string | null
}

/** Passage d'une activite par une etape de validation. */
export interface ValidationActivite {
    id: number
    decision: DecisionValidation
    commentaire: string | null
    dateDemande: string | null
    /** Nulle tant que la decision est EN_ATTENTE_VALIDATION. */
    dateDecision: string | null
    etape: EtapeValidation
    demandeur: UtilisateurResume | null
    decideur: UtilisateurResume | null
}

/** Mesure d'un indicateur sur une periode. */
export interface ValeurIndicateur {
    id: number
    valeur: number | null
    periodeDebut: string | null
    periodeFin: string | null
    commentaire: string | null
    dateSaisie: string | null
    utilisateur: UtilisateurResume | null
    /** Vrai pour la seule valeur retenue comme valeur courante. */
    valeurCourante: boolean
}

/** Indicateur rattache a une activite, avec son historique de mesures. */
export interface IndicateurDetail {
    id: number
    code: string | null
    codeHopex: string | null
    indicateurHopex: string | null
    designation: string | null
    typeIndicateur: string | null
    uniteMesure: string | null
    frequenceVerification: string | null
    frequenceAggregation: string | null
    definition: string | null
    methodeDetermination: string | null
    objectif: string | null
    valeurCible: number | null
    seuilMin: number | null
    seuilMax: number | null
    actif: boolean | null
    /** Mesure la plus recente, ou null si aucune. */
    valeurCourante: ValeurIndicateur | null
    /** Toutes les mesures, de la plus recente a la plus ancienne. */
    valeurs: ValeurIndicateur[]
}

/** Resultat intermediaire rattache a une activite. */
export interface ResultatIntermediaire {
    id: number
    designation: string
}

/** Entree de l'historique de statut d'une activite. */
export interface HistoriqueActivite {
    id: number
    statut: ActiviteReference | null
    dateChangement: string | null
    commentaire: string | null
    utilisateur: UtilisateurResume | null
    /** Vrai pour la seule ligne portant le statut courant. */
    etatActuel: boolean
}

/** Detail complet d'une activite, renvoye en une seule reponse. */
export interface ActiviteDetail {
    id: number
    code: string
    reference: string
    designation: string
    dateDebutPrevue: string | null
    dateFinPrevue: string | null
    dateDebutReelle: string | null
    dateFinReelle: string | null
    /** Premiere entree d'historique : l'activite ne porte pas sa date de creation. */
    dateCreation: string | null
    /** Vrai si l'activite est rattachee a un objectif specifique. */
    pta: boolean
    objectifSpecifique: ActiviteReference | null
    service: ActiviteReference | null
    typeActivite: ActiviteReference | null
    site: ActiviteReference | null
    priorite: ActiviteReference | null
    /** Statut courant, reconstruit depuis l'historique. Null si l'historique est vide. */
    statut: ActiviteReference | null
    /** Moyenne des derniers avancements des sous-activites, 0 si aucune ne suit. */
    avancement: number | null
    /** Echeance depassee sans statut terminal. */
    enRetard: boolean
    sousActivites: SousActiviteDetail[]
    validations: ValidationActivite[]
    indicateurs: IndicateurDetail[]
    resultatsIntermediaires: ResultatIntermediaire[]
    /** Du plus recent au plus ancien. */
    historique: HistoriqueActivite[]
}

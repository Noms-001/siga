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
    reference: string | null
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

    /**
     * Etape du circuit de validation atteinte par l'activite.
     *
     * Renseignees uniquement par /activites/a-valider : sur la liste de
     * suivi standard, ces trois champs restent absents du DTO. Le typage
     * les declare optionnels pour que la meme interface serve les deux
     * endpoints -- ce qui evite de dupliquer ActiviteListItem en
     * ActiviteListItemAValider, et de faire diverger les deux au premier
     * champ ajoute.
     *
     * Cote a-valider, `etapeCourante` est TOUJOURS renseignee : le backend
     * indexe sa reponse par la designation de l'etape, donc une activite
     * sans etape n'y figurerait pas. Les deux autres peuvent etre null --
     * premiere etape du circuit, ou derniere.
     */
    etapeCourante?: ActiviteReference | null
    etapeSuivante?: ActiviteReference | null
    etapePrecedente?: ActiviteReference | null
    derniereValidation?: ValidationActiviteResponse | null
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
    /**
     * Objectif specifique uniquement : annee de l'objectif et prochain
     * numero d'activite libre dans cet objectif. Le formulaire s'en sert pour
     * proposer un code et une reference, qui restent modifiables.
     */
    annee?: number | null
    prochainNumero?: number | null
}

/**
 * Code propose pour une activite NON PTA.
 *
 * `annee` accompagne le code parce que la proposition ne vaut que pour elle :
 * changer d'annee dans le formulaire doit rendre la valeur affichee
 * caduque, et non la laisser paraitre toujours valable.
 */
export interface CodePropose {
    code: string
    annee: number
}

/**
 * Objectif specifique a creer depuis le formulaire d'activite.
 *
 * `annee` est demandee et non deduite : la colonne est NOT NULL, contrainte
 * entre 1900 et 2100, et le code ne la porte pas (`BCT/1`). Le backend ne
 * peut donc pas la retrouver, et une valeur par defaut rangerait l'utilisateur
 * dans l'annee du jour sans qu'il l'ait demandee.
 */
export interface ObjectifSpecifiqueEcriture {
    code: string
    designation: string
    annee: number
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
 * "Brouillon" est un statut d'ecriture, pas de suivi : il n'apparait dans
 * aucune carte de la liste, qui ne compte que les activites publiees. Il est
 * donc exporte a part, pour la seule page qui liste les activites en cours
 * de redaction.
 */
export const BROUILLON = 'BROUILLON'

/**
 * "En attente de validation" est exporte pour la seule page qui vide le
 * circuit, comme BROUILLON pour celle qui l'emplit. Ces deux statuts-la sont
 * des etats de travail : ni l'un ni l'autre n'apparait dans les cartes de la
 * liste de suivi.
 */
export const EN_ATTENTE_VALIDATION = 'EN_ATTENTE_VALIDATION'

/**
 * Accuse de soumission (POST /api/activites/{id}/soumettre-validation).
 *
 * statut est toujours EN_ATTENTE_VALIDATION : l'activite quitte la liste des
 * brouillons, elle n'entre pas encore dans le suivi. C'est ce qui permet a la
 * page de retirer la carte sans relire l'activite.
 */
export interface SoumissionResultat {
    idActivite: number
    code: string
    statut: string
    dateSoumission: string | null
}

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
    /**
     * Statut courant de l'activite mere : une sous-activite n'en a pas.
     *
     * C'est lui qui autorise le depot d'un livrable, et le backend le
     * verifie de nouveau a l'ecriture -- cette valeur sert a afficher le
     * bouton, pas a decider. `null` quand l'activite n'a pas d'historique,
     * ce qui equivaut a un refus.
     */
    statutActivite: string | null
}

/**
 * Accuse de decision (POST /api/activites/{id}/decision).
 *
 * statut est le statut APRES decision, et il est renvoye pour que la page
 * n'ait pas a le deduire : c'est lui qui permet de retirer la carte sans
 * relire l'activite. dateDecision peut etre nulle si le backend a rejoue une
 * ecriture sans horodatage, d'ou le type large plutot que Date.
 */
export interface DecisionResultat {
    idActivite: number
    code: string
    /** Ce qui a ete decide : VALIDE, REJETE ou RETOUR_MODIFICATION. */
    decision: string
    /** Statut obtenu : VALIDEE, REJETE ou BROUILLON. */
    statut: string
    dateDecision: string | null
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

/**
 * Resultat intermediaire ecrit par le formulaire.
 *
 * Un seul champ, contre deux cote lecture : l'identifiant n'est pas renvoye
 * parce que le backend remplace l'ensemble des resultats a l'enregistrement.
 * Aucune table ne depend d'un resultat intermediaire, donc le supprimer
 * n'entraine rien -- contrairement a une sous-activite, dont les
 * affectations et livrables interdit de la reecrire ainsi.
 */
export interface ResultatIntermediaireEcriture {
    designation: string
}

/** Ligne de resultat intermediaire pre-remplie par le backend. */
export interface ResultatIntermediaireFormulaire {
    idResultatIntermediaire: number
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
    reference: string | null
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

/**
 * Corps d'ecriture d'une activite, partage par la creation et la
 * modification.
 *
 * `pta` est envoye explicitement plutot que deduit de la presence d'un
 * objectif : c'est ce qui permet au backend de refuser un corps qui se
 * contredit (pta vrai sans objectif, ou pta faux avec un objectif) au lieu
 * de le corriger en silence.
 *
 * `idService` est facultatif : un utilisateur rattache a un service n'a pas
 * d'element a choisir, et le backend impose le sien. Un utilisateur de
 * niveau departement doit, lui, en designer un.
 */
export interface ActiviteEcriture {
    code: string
    reference: string | null
    designation: string
    dateDebutPrevue: string
    dateFinPrevue: string | null
    pta: boolean
    idObjectifSpecifique: number | null
    idTypeActivite: number | null
    idSite: number | null
    idPriorite: number
    idService: number | null
    sousActivites: SousActiviteEcriture[]
    resultatsIntermediaires: ResultatIntermediaireEcriture[]
}

/**
 * Ligne de sous-activite du formulaire.
 *
 * `idSousActivite` a null = creation, renseignee = modification de cette
 * ligne. C'est ce qui permet d'ajouter ou de retirer une ligne sans
 * supprimer puis recreer toutes les autres.
 *
 * `code` vide = le backend en genere un, car il est unique dans toute la
 * base et non seulement par activite.
 *
 * `livrables` suit la meme logique, mais sans identifiant : le backend
 * reconnait un livrable a sa designation et le remplace tant qu'il ne porte
 * pas de fichier. Un livrable ne designe personne d'autre, contrairement a la
 * sous-activite, donc il n'a pas besoin d'etre lu par affectation.
 */
export interface SousActiviteEcriture {
    idSousActivite: number | null
    code: string
    designation: string
    dateDebutPrevue: string
    dateFinPrevue: string
    livrables: LivrableEcriture[]
}

/**
 * Livrable a ajouter depuis le detail d'une sous-activite.
 *
 * Deux champs seulement, parce que la table n'en porte que deux. Les
 * FICHIERS ne voyagent pas dans ce corps : ils sont joints en multipart, a
 * cote, via creerLivrable. Ils restent donc facultatifs des deux cotes, et le
 * serveur ne refuse pas un livrable sans depot.
 */
export interface LivrableAjout {
    designation: string
    description?: string | null
}

/**
 * Livrable ecrit par le formulaire.
 *
 * Aucun identifiant n'est renvoye, et c'est volontaire : la designation est
 * le nom que l'utilisateur donne au livrable, et c'est elle qui sert a
 * reconnaitre une ligne deja enregistree. Renvoyer un identifiant que le
 * formulaire ne pourrait pas resoudre -- aucun livrable ne porte
 * d'affectation -- n'ajouterait qu'un ecran a gerer.
 */
export interface LivrableEcriture {
    designation: string
    description: string
}

/**
 * Livrable pre-rempli par le backend.
 *
 * `nombreFichiers` n'est pas qu'un libelle a afficher : il interdit au
 * formulaire de proposer la suppression d'un livrable que le backend
 * refuserait. Il est donc charge avec la ligne, et non relu a
 * l'enregistrement, ou un depot peut avoir ete ajoute depuis.
 */
export interface LivrableFormulaire {
    idLivrable: number
    designation: string
    description: string | null
    nombreFichiers: number
}

/** Accuse de creation ou de modification. */
export interface ActiviteEcritureResultat {
    idActivite: number
    code: string
    reference: string | null
    designation: string
    statut: string
    dateDebutPrevue: string
    dateFinPrevue: string | null
    dateEnregistrement: string
    nombreSousActivites: number
    nombreResultatsIntermediaires: number
}

/** Ligne de sous-activite pre-remplie par le backend. */
export interface SousActiviteFormulaire {
    idSousActivite: number
    code: string
    designation: string
    dateDebutPrevue: string
    dateFinPrevue: string
    dateDebutReelle: string | null
    dateFinReelle: string | null
    livrables: LivrableFormulaire[]
}

/**
 * Ce que le formulaire doit pre-remplir en mode modification.
 *
 * Sous-ensemble du detail d'une activite : celui-ci porte en plus avancement,
 * livrables, fichiers, validations, indicateurs et historique, qu'un
 * formulaire de redaction n'edite pas.
 *
 * `statut` sert uniquement a afficher ou masquer le bouton Modifier ; c'est
 * le backend qui refuse la modification si l'activite n'est plus un
 * brouillon.
 */
export interface ActiviteFormulaire {
    idActivite: number
    code: string
    reference: string | null
    designation: string
    dateDebutPrevue: string
    dateFinPrevue: string | null
    idObjectifSpecifique: number | null
    /**
     * Objectif retenu, en clair.
     *
     * Un identifiant seul ne suffirait pas a pre-remplir le champ : le
     * formulaire doit afficher un libelle, pas un nombre. Les trois valeurs
     * sont nulles ensemble, quand aucun objectif n'est rattache.
     */
    objectifCode: string | null
    objectifDesignation: string | null
    objectifAnnee: number | null
    idTypeActivite: number | null
    idSite: number | null
    idPriorite: number
    idService: number
    statut: string
    sousActivites: SousActiviteFormulaire[]
    resultatsIntermediaires: ResultatIntermediaireFormulaire[]
}

/**
 * Accusé d'une décision de validation (POST /{id}/valider ou
 * POST /{id}/soumettre-validation?retour=true).
 *
 * Ce que le backend a réellement enregistré : décision, statut obtenu,
 * étape franchie. La page ne relit pas l'activité pour vérifier — elle
 * retire la ligne et fait confiance au verdict.
 */
export interface ValidationActiviteResponse {
    idValidationActivite: number
    decision: DecisionValidation
    /** Décision de la validation qui précède celle-ci, en clair. Null si aucune. */
    derniereDecision: string | null
    commentaire: string | null
    dateDemande: string | null
    dateDecision: string | null
    idActivite: number
    idEtapeValidation: number
    designationEtape: string
    niveau: number
    obligatoire: boolean | null
    idDemandeur: number
    idDecideur: number | null
}
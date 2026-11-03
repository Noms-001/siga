export interface ReferenceDTO {
    id: number | null
    code: string | null
    libelle: string | null
    annee: number | null
}

export interface PlanActionListItem {
    id: number
    code: string
    designation: string
    dateDebutPrevue: string
    dateFinPrevue: string
    dateDebutReelle: string | null
    dateFinReelle: string | null
    statut: ReferenceDTO | null
    avancement: number
    responsable: ReferenceDTO | null
}

export interface PageResponse<T> {
    content: T[]
    page: number
    size: number
    totalElements: number
    totalPages: number
}

export interface PlanActionFiltres {
    statut?: string
    search?: string
    annee?: number
    dateDebut?: string
    dateFin?: string
    origineId?: number
    typeOrigine?: string
    responsableId?: number
}

export const STATUTS_PLAN: Array<{ value: string; label: string }> = [
    { value: '', label: 'Tous (actifs)' },
    { value: 'NON_COMMENCEE', label: 'Non commencés' },
    { value: 'EN_COURS', label: 'En cours' },
    { value: 'TERMINEE', label: 'Terminés' },
]

export const TYPES_ORIGINE: Array<{ value: string; label: string }> = [
    { value: '',         label: 'Tous les types' },
    { value: 'INCIDENT', label: 'Incident' },
    { value: 'RISQUE',   label: 'Risque' },
    { value: 'AUTRE',    label: 'Autre' },
]

export interface OrigineLien {
    id: number
    code: string
    designation: string
    typeOrigine: string
    estPrincipale: boolean | null
}

export interface AffectationPlanAction {
    id: number
    dateAffectation: string
    dateDesaffectation: string | null
    role: ReferenceDTO | null
    utilisateur: ReferenceDTO | null
    active: boolean
}

export interface AvancementPlanAction {
    id: number
    valeurPourcentage: number
    commentaire: string | null
    dateChangement: string
    statut: ReferenceDTO | null
    utilisateur: ReferenceDTO | null
    courant?: boolean
}

export interface PlanActionDetail {
    id: number
    code: string
    designation: string
    dateDebutPrevue: string
    dateFinPrevue: string
    dateDebutReelle: string | null
    dateFinReelle: string | null
    statut: ReferenceDTO | null
    avancement: number
    responsable: ReferenceDTO | null
    origines: OrigineLien[]
    affectations: AffectationPlanAction[]
    historique: AvancementPlanAction[]
}

export interface AvancementSaisieRequest {
    valeurPourcentage: number
    commentaire?: string | null
}

export interface PlanActionRequest {
    code?: string | null
    designation: string
    dateDebutPrevue: string
    dateFinPrevue: string
    origineIds: number[]
    responsableId: number
}

export interface PlanActionEcritureResponse {
    id: number
    code: string
    designation: string
    statutInitial: string
}

export interface OrigineListeItem {
    id: number
    code: string
    designation: string
    typeOrigine: string
}

export interface UtilisateurAffectableItem {
    id: number
    nom: string
    prenom: string
}

export interface SuiviGlobalPlanAction {
    resume: {
        total: number
        nonCommences: number
        enCours: number
        enRetard: number
        termines: number
        avancementMoyen: number
    }
    parTypeOrigine: Array<{
        typeOrigine: string
        nombre: number
        avancementMoyen: number
    }>
    parResponsable: Array<{
        idUtilisateur: number
        libelle: string
        total: number
        enRetard: number
        avancementMoyen: number
    }>
    enRetard: Array<{
        id: number
        code: string
        designation: string
        dateFinPrevue: string
        joursDeRetard: number
        avancement: number
        responsableLibelle: string | null
    }>
    echeancesProchaines: Array<{
        id: number
        code: string
        designation: string
        dateFinPrevue: string
        joursRestants: number
        avancement: number
        responsableLibelle: string | null
    }>
}
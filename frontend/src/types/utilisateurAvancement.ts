import type { ReferenceDTO } from '@/types/common'

export interface ActiviteAvancement {
    id: number
    code: string
    designation: string
    reference: string | null
    role: ReferenceDTO | null
    statut: ReferenceDTO | null
    avancement: number
    dateDebutPrevue: string
    dateFinPrevue: string
    dateDebutReelle: string | null
    dateFinReelle: string | null
    joursDeRetard: number
    enRetard: boolean
    dateAffectation: string
}

export interface PlanActionAvancement {
    id: number
    code: string
    designation: string
    role: ReferenceDTO | null
    statut: ReferenceDTO | null
    avancement: number
    dateDebutPrevue: string
    dateFinPrevue: string
    dateDebutReelle: string | null
    dateFinReelle: string | null
    joursDeRetard: number
    enRetard: boolean
    dateAffectation: string
}

export interface ResumeActivites {
    total: number
    nonCommencees: number
    enCours: number
    enRetard: number
    terminees: number
    avancementMoyenGlobal: number
    details: ActiviteAvancement[]
}

export interface ResumePlansAction {
    total: number
    nonCommences: number
    enCours: number
    enRetard: number
    termines: number
    avancementMoyenGlobal: number
    details: PlanActionAvancement[]
}

export interface UtilisateurAvancement {
    utilisateur: ReferenceDTO
    annee: number
    activites: ResumeActivites
    plansAction: ResumePlansAction
}
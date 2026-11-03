export interface Indicateur {
  id: number
  code: string
  designation: string
  typeIndicateur?: string | null
  uniteMesure?: string | null
  frequenceVerification?: string | null
  frequenceAggregation?: string | null
  definition?: string | null
  methodeDetermination?: string | null
  objectif?: string | null
  valeurCible?: number | null
  seuilMin?: number | null
  seuilMax?: number | null
  actif: boolean
}

export interface IndicateurRequest {
  code: string
  designation: string
  typeIndicateur?: string | null
  uniteMesure?: string | null
  frequenceVerification?: string | null
  frequenceAggregation?: string | null
  definition?: string | null
  methodeDetermination?: string | null
  objectif?: string | null
  valeurCible?: number | null
  seuilMin?: number | null
  seuilMax?: number | null
  actif?: boolean | null
}

export interface IndicateurFiltres {
  types: string[]
}

export interface SaisiePar {
  id: number
  nom: string
  prenom: string
}

export interface ValeurIndicateur {
  id: number
  valeur: number
  periodeDebut: string
  periodeFin: string
  commentaire?: string | null
  dateSaisie: string
  saisiePar: SaisiePar | null
}

export interface ValeurIndicateurRequest {
  valeur: number
  periodeDebut: string   // ISO LocalDateTime, ex. "2026-01-01T00:00:00"
  periodeFin: string     // ISO LocalDateTime, ex. "2026-03-31T23:59:59"
  commentaire?: string | null
}

export type StatutPeriodeCalendrier =
  | 'ATTEINT'
  | 'NON_ATTEINT'
  | 'INDETERMINE'
  | 'NON_SAISI'
  | 'EN_COURS'
  | 'MANQUANT'

export interface PeriodeCalendrier {
  libelle: string
  debut: string
  fin: string
  valeurId: number | null
  valeur: number | null
  commentaire: string | null
  dateSaisie: string | null
  saisieParLibelle: string | null
  statut: StatutPeriodeCalendrier
  estEnCours: boolean
  estFuture: boolean
  estPassee: boolean
}

export interface CalendrierIndicateur {
  idIndicateur: number
  code: string
  designation: string
  annee: number
  frequenceVerification: string
  typeIndicateur: string | null
  uniteMesure: string | null
  valeurCible: number | null
  seuilMin: number | null
  seuilMax: number | null
  nombrePeriodes: number
  nombreSaisies: number
  valeurMoyenne: number
  periodes: PeriodeCalendrier[]
}

export interface DashboardIndicateurResume {
  total: number
  atteints: number
  nonAtteints: number
  indetermines: number
  enRetard: number
  jamaisSaisis: number
}

export interface IndicateurAvecAtteinte {
  id: number
  code: string
  designation: string
  typeIndicateur: string | null
  uniteMesure: string | null
  valeurCible: number | null
  valeurCourante: number | null
  ecart: number | null
  statut: string
  frequenceVerification: string | null
}

export interface DashboardIndicateur {
  resume: DashboardIndicateurResume
  indicateurs: IndicateurAvecAtteinte[]
  enRetard: Array<{
    idIndicateur: number
    code: string
    designation: string
    typeIndicateur: string | null
    frequenceVerification: string
    dernierePeriodeFin: string | null
    prochaineEcheance: string | null
    joursDeRetard: number | null
    statut: string
  }>
}
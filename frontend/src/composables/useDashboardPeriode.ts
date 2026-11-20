import { computed, ref } from 'vue'
import type { PeriodeDashboard } from '@/types/dashboard'
import { getCurrentYear } from '@/utils/date'

export type ModePeriode = 'annee' | 'mois' | 'trimestre' | 'personnalise'

export interface OptionPeriode {
    /** Index signature requise par le type SelectOption de BaseSelect. */
    [key: string]: unknown
    value: number
    label: string
}

const MOIS: OptionPeriode[] = [
    { value: 1, label: 'Janvier' },
    { value: 2, label: 'Février' },
    { value: 3, label: 'Mars' },
    { value: 4, label: 'Avril' },
    { value: 5, label: 'Mai' },
    { value: 6, label: 'Juin' },
    { value: 7, label: 'Juillet' },
    { value: 8, label: 'Août' },
    { value: 9, label: 'Septembre' },
    { value: 10, label: 'Octobre' },
    { value: 11, label: 'Novembre' },
    { value: 12, label: 'Décembre' },
]

const TRIMESTRES: OptionPeriode[] = [
    { value: 1, label: 'T1' },
    { value: 2, label: 'T2' },
    { value: 3, label: 'T3' },
    { value: 4, label: 'T4' },
]

function deuxChiffres(valeur: number): string {
    return String(valeur).padStart(2, '0')
}

/** Dernier jour d'un mois (1-12) pour une année donnée. */
function dernierJour(annee: number, mois: number): string {
    const d = new Date(annee, mois, 0)
    return `${d.getFullYear()}-${deuxChiffres(d.getMonth() + 1)}-${deuxChiffres(d.getDate())}`
}

/**
 * État du filtre temporel global du dashboard.
 *
 * Le mode détermine la période envoyée à tous les endpoints. En mode
 * personnalisé, `periode` vaut null tant que la saisie est invalide :
 * le dashboard suspend alors ses chargements et affiche `erreur`.
 */
export function useDashboardPeriode() {
    const maintenant = new Date()
    const anneeCourante = getCurrentYear()
    const moisCourant = maintenant.getMonth() + 1
    const trimestreCourant = Math.floor(maintenant.getMonth() / 3) + 1

    const mode = ref<ModePeriode>('annee')
    const annee = ref(anneeCourante)
    const mois = ref(moisCourant)
    const trimestre = ref(trimestreCourant)
    const dateDebut = ref(`${anneeCourante}-01-01`)
    const dateFin = ref(`${anneeCourante}-12-31`)

    const annees = computed<OptionPeriode[]>(() => {
        const base = getCurrentYear()
        return [base - 3, base - 2, base - 1, base, base + 1].map(y => ({
            value: y,
            label: String(y),
        }))
    })

    const erreur = computed(() => {
        if (mode.value !== 'personnalise') return ''
        if (!dateDebut.value || !dateFin.value) {
            return 'Renseignez la date de début et la date de fin.'
        }
        if (dateDebut.value > dateFin.value) {
            return 'La date de début doit être antérieure ou égale à la date de fin.'
        }
        return ''
    })

    const periode = computed<PeriodeDashboard | null>(() => {
        if (mode.value === 'annee') {
            return {
                debut: `${annee.value}-01-01`,
                fin: `${annee.value}-12-31`,
            }
        }

        if (mode.value === 'mois') {
            return {
                debut: `${annee.value}-${deuxChiffres(mois.value)}-01`,
                fin: dernierJour(annee.value, mois.value),
            }
        }

        if (mode.value === 'trimestre') {
            const premierMois = (trimestre.value - 1) * 3 + 1
            return {
                debut: `${annee.value}-${deuxChiffres(premierMois)}-01`,
                fin: dernierJour(annee.value, premierMois + 2),
            }
        }

        if (erreur.value) return null
        return { debut: dateDebut.value, fin: dateFin.value }
    })

    return {
        mode,
        annee,
        mois,
        trimestre,
        dateDebut,
        dateFin,
        annees,
        moisOptions: MOIS,
        trimestres: TRIMESTRES,
        erreur,
        periode,
    }
}

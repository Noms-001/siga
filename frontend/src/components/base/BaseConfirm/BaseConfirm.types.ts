import type { PropType } from 'vue'
import type { ButtonVariant } from '../BaseButton/BaseButton.types'

/**
 * Ton d'une demande de confirmation.
 *
 * Le ton ne decide pas du texte : il dit ce que la reponse "oui" va
 * maitriser. Une suppression annoncee en bleu primaires donne l'impression
 * d'ajouter quelque chose. `danger` est reserve a ce qui se perd -- une
 * suppression -- et `warning` a ce qui se fige, comme une soumission.
 */
export type ConfirmTone = 'primary' | 'danger' | 'warning'

/**
 * Ce que la modale affiche.
 *
 * Un objet unique plutot que six props : une page qui demande dix
 * confirmations en aurait dix, et le risque est qu'un auteur oublie le
 * message de detail et livre une modale qui dit "Confirmer ?" a l'utilisateur.
 * En passant un objet, l forget impossible : tout ce qui est obligatoire ici
 * l'est aussi dans le type.
 */
export interface ConfirmDemande {
    /** Ce qui va se passer, en une phrase. */
    titre: string

    /** Ce qui est concerne, et ce qu'on perd ou ce qu'on fige. */
    message: string

    /** Libelle du bouton qui confirme. */
    libelleConfirmer: string

    /** Ton de l'icone et du bouton de confirmation. */
    ton: ConfirmTone

    /** Libelle du bouton d'annulation. */
    libelleAnnuler?: string

    /**
     * Ce qui rend l'action difficile a defaire.
     *
     * Remplace le `?` final par un fait : "Cette action est irreversible" donne
     * a l'utilisateur de quoi decider, alors que "Confirmer ?" ne lui donne
     * que la decision.
     */
    consequence?: string

    /** Icone bootstrap, sans la classe. */
    icone?: string
}

export const baseConfirmProps = {
    modelValue: {
        type: Boolean,
        default: false
    },

    demande: {
        type: Object as PropType<ConfirmDemande | null>,
        default: null
    },

    loading: {
        type: Boolean,
        default: false
    }
} as const

/**
 * Variante du bouton de confirmation, deduite du ton.
 *
 * Exporte pour que les tests et les pages puissent verifier le ton rendu sans
 * reconstruire la correspondance.
 */
export const VARIANTE_PAR_TON: Record<ConfirmTone, ButtonVariant> = {
    primary: 'primary',
    danger: 'danger',
    warning: 'warning'
}

/** Couleur d'icone par ton, pour que l'icone suive le bouton. */
export const ICONE_PAR_TON: Record<ConfirmTone, string> = {
    primary: 'bi bi-question-circle',
    danger: 'bi bi-exclamation-triangle',
    warning: 'bi bi-exclamation-circle'
}

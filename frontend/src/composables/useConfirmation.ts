import { ref, type Ref } from 'vue'

import type { ConfirmDemande, ConfirmTone } from '@/components/base'

/**
 * Pose une confirmation devant une action.
 *
 * CE QUE CE COMPOSANT FAIT
 * Il tient la demande courante et repond a une seule question : quelle action
 * est en attente de confirmation. La page appelle `demander(...)` au lieu de
 * lancer l'action, et n'execute qu'apres `confirmer()`.
 *
 * CE QU IL NE FAIT PAS
 * Il n'execute rien. Une confirmation qui executait l'action elle-meme
 * attacherait chaque page a son propre mecanisme d'annulation, et
 * l'annulation deviendrait impossible a tester.
 *
 * POURQUOI UNE DEMANDE ET UN NOM D'ACTION
 * Une confirmation doit nommer ce qu'elle confirme. On pourrait passer une
 * fonction et l'appeler au clic sur "Confirmer", mais alors le nom de l'action
 * n'existerait qu'a l'execution : le message ne pourrait pas dire "Supprimer
 * ACT-004" avant que l'utilisateur ait clique. D'ou deux entrees : un nom pour
 * le message, une fonction pour le faire.
 *
 * Exemple :
 * ```ts
 * const confirmation = useConfirmation()
 *
 * function supprimer(id: number, code: string) {
 *     confirmation.demander({
 *         titre: 'Supprimer l\'activité',
 *         message: `L'activité ${code} et ses sous-activités seront supprimées.`,
 *         consequence: 'Cette action est irréversible.',
 *         libelleConfirmer: 'Supprimer',
 *         ton: 'danger',
 *         action: () => vraimentSupprimer(id),
 *     })
 * }
 * ```
 */
/** Une demande d'action, avec l'action a executer si l'utilisateur confirme. */
export interface DemandeConfirmation extends Omit<ConfirmDemande, 'ton'> {
    /** Ce que la reponse affirmative declenche. */
    action: () => void | Promise<void>

    /** Ton de la modale. `warning` par defaut : ni neutre, ni destructeur. */
    ton?: ConfirmTone
}

export interface ConfirmationEnCours {
    /** La demande affichee, ou null si aucune confirmation n'est ouverte. */
    demande: Ref<ConfirmDemande | null>

    /** La modale est-elle ouverte ? */
    ouvert: Ref<boolean>

    /** L'action est-elle en cours d'execution ? */
    enCours: Ref<boolean>

    /** Poser une demande : la modale s'ouvre, l'action attend. */
    demander: (demande: DemandeConfirmation) => void

    /** Executer l'action en attente, puis refermer. */
    confirmer: () => Promise<void>

    /** Refermer sans rien faire. */
    annuler: () => void
}

export function useConfirmation(): ConfirmationEnCours {
    const demande = ref<ConfirmDemande | null>(null)
    const ouvert = ref(false)
    const enCours = ref(false)

    // L'action a executer est tenue a part de la demande : celle-ci ne contient
    // que ce qui s'affiche, donc elle reste serialisable et testable.
    let actionEnAttente: (() => void | Promise<void>) | null = null

    function demander(aDemander: DemandeConfirmation): void {
        const { action, ton = 'warning', ...reste } = aDemander

        actionEnAttente = action
        demande.value = { ...reste, ton }
        ouvert.value = true
    }

    async function confirmer(): Promise<void> {
        const action = actionEnAttente

        // Elle est remise a null avant l'appel : une confirmation double, ou un
        // re-rendu pendant l'appel, ne doit pas rejouer l'action.
        actionEnAttente = null

        if (!action) {
            ouvert.value = false
            return
        }

        enCours.value = true

        try {
            await action()
        } finally {
            enCours.value = false
            // La modale se referme dans tous les cas. Une erreur remontee par
            // l'action remonte aussi : c'est a la page de l'afficher, et la
            // confirmation n'a plus rien a dire une fois l'action tentee.
            ouvert.value = false
            demande.value = null
        }
    }

    function annuler(): void {
        actionEnAttente = null
        ouvert.value = false
        demande.value = null
    }

    return { demande, ouvert, enCours, demander, confirmer, annuler }
}

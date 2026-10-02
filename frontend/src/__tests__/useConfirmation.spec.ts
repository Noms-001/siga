import { describe, it, expect, beforeEach } from 'vitest'

import { useConfirmation } from '@/composables/useConfirmation'

/**
 * Le composable ne rend rien : il tient une demande et repond a deux questions.
 * Ces tests le pilotent donc directement, sans monter de composant.
 */
describe('useConfirmation', () => {
    beforeEach(() => {
        // La modale se teleporte dans le body : sans ce nettoyage, une demande
        // laissee ouverte par un test apparaitrait dans le suivant.
        document.body.innerHTML = ''
    })

    function demandeValide() {
        return {
            titre: 'Supprimer',
            message: 'L\'activité ACT-001 sera supprimée.',
            libelleConfirmer: 'Supprimer',
            action: () => undefined,
        }
    }

    it('ouvre la modale et rend la demande', () => {
        const confirmation = useConfirmation()

        expect(confirmation.ouvert.value).toBe(false)
        expect(confirmation.demande.value).toBeNull()

        confirmation.demander(demandeValide())

        expect(confirmation.ouvert.value).toBe(true)
        expect(confirmation.demande.value?.message).toContain('ACT-001')
    })

    it('applique le ton warning par defaut', () => {
        const confirmation = useConfirmation()

        confirmation.demander(demandeValide())

        // Sans repli, une demande sans ton n'aurait pas de variante de bouton
        // et afficherait un ton muet, alors que la regle est celle de l'alerte.
        expect(confirmation.demande.value?.ton).toBe('warning')
    })

    it('conserve le ton demande quand il est donne', () => {
        const confirmation = useConfirmation()

        confirmation.demander({ ...demandeValide(), ton: 'danger' })

        expect(confirmation.demande.value?.ton).toBe('danger')
    })

    it("n'execute rien tant que l'utilisateur n'a pas confirme", async () => {
        let executes = 0

        const confirmation = useConfirmation()

        confirmation.demander({ ...demandeValide(), action: () => { executes += 1 } })

        expect(executes).toBe(0)

        await confirmation.confirmer()

        expect(executes).toBe(1)
    })

    it("n'execute rien sur une annulation", () => {
        let executes = 0

        const confirmation = useConfirmation()

        confirmation.demander({ ...demandeValide(), action: () => { executes += 1 } })
        confirmation.annuler()

        expect(executes).toBe(0)
        expect(confirmation.ouvert.value).toBe(false)
        expect(confirmation.demande.value).toBeNull()
    })

    it('referme apres une confirmation', async () => {
        const confirmation = useConfirmation()

        confirmation.demander(demandeValide())
        await confirmation.confirmer()

        expect(confirmation.ouvert.value).toBe(false)
        expect(confirmation.demande.value).toBeNull()
    })

    it('attend une action asynchrone avant de refermer', async () => {
        const etapes: string[] = []

        const confirmation = useConfirmation()

        confirmation.demander({
            ...demandeValide(),
            action: async () => {
                etapes.push('debut')
                await Promise.resolve()
                etapes.push('fin')
            },
        })

        const promesse = confirmation.confirmer()

        expect(confirmation.enCours.value).toBe(true)
        expect(confirmation.ouvert.value).toBe(true)

        await promesse

        // La modale reste ouverte pendant l'appel : la fermer tot ferait croire
        // que rien n'a ete fait.
        expect(etapes).toEqual(['debut', 'fin'])
        expect(confirmation.enCours.value).toBe(false)
        expect(confirmation.ouvert.value).toBe(false)
    })

    it("referme meme quand l'action echoue", async () => {
        const confirmation = useConfirmation()

        confirmation.demander({
            ...demandeValide(),
            action: () => Promise.reject(new Error('refus')),
        })

        await expect(confirmation.confirmer()).rejects.toThrow('refus')

        // La confirmation n'a plus rien a dire une fois l'action tentee, et la
        // laisser ouverte empecherait de retenter.
        expect(confirmation.ouvert.value).toBe(false)
        expect(confirmation.enCours.value).toBe(false)
    })

    it('n execute pas deux fois la meme action', async () => {
        let executes = 0

        const confirmation = useConfirmation()

        confirmation.demander({ ...demandeValide(), action: () => { executes += 1 } })

        await confirmation.confirmer()
        await confirmation.confirmer()

        // Un double clic sur "Confirmer" ne doit pas doubler l'ecriture.
        expect(executes).toBe(1)
    })

    it("n'execute pas une demande annulee entre-temps", async () => {
        let executes = 0

        const confirmation = useConfirmation()

        confirmation.demander({ ...demandeValide(), action: () => { executes += 1 } })
        confirmation.annuler()

        await confirmation.confirmer()

        // L'action a ete oubliee avec l'annulation : une confirmation ne doit
        // pas rejouer une demande que l'utilisateur a refusee.
        expect(executes).toBe(0)
    })

    it('remplace la demande precedente plutot que de les empiler', async () => {
        let premier = 0
        let second = 0

        const confirmation = useConfirmation()

        confirmation.demander({ ...demandeValide(), action: () => { premier += 1 } })
        confirmation.demander({
            titre: 'Autre',
            message: ' autre demande',
            libelleConfirmer: 'Autre',
            action: () => { second += 1 },
        })

        await confirmation.confirmer()

        expect(premier).toBe(0)
        expect(second).toBe(1)
    })
})

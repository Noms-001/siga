import { describe, it, expect, vi, afterEach, beforeEach } from 'vitest'

import {
    enableAutoUnmount,
    flushPromises,
    mount,
    type DOMWrapper,
    type VueWrapper,
} from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'

import AValider from '@/views/frontoffice/activites/AValider.vue'
import * as activiteService from '@/services/activite'
/*
    Chaque composant monte est demonte apres son test : BaseConfirm et
    RejetActiviteModal se teleportent dans document.body, donc une modale laissee
    ouverte par un test anterior survivrait dans le body, et le test suivant
    cliquerait sur une confirmation qui n'est pas la sienne.
*/
enableAutoUnmount(afterEach)

import type { ApiResponse } from '@/services/api-client'
import type {
    ActiviteAutocomplete,
    ActiviteListItem,
    DecisionResultat,
    PageResult,
} from '@/types/activite'

/**
 * Tests de la page des activites a valider.
 *
 * Le service est mocke : cette page ne contient aucune regle metier, elle ne
 * fait qu'afficher une liste et appeler une ecriture. Ce qui est teste ici est
 * donc ce qui lui appartient : la liste forcee sur le bon statut, le sort d'une
 * carte apres decision, et le parcours de rejet -- qui est le seul a deux
 * etapes, parce qu'il demande un choix puis un motif.
 */
vi.mock('@/services/activite', () => ({
    listerActivitesAValider: vi.fn<
        () => Promise<ApiResponse<PageResult<ActiviteListItem>>>
    >(),
    deciderValidation: vi.fn<
        (id: number, decision: string, commentaire: string | null) => Promise<
            ApiResponse<DecisionResultat>
        >
    >(),
    autocompleterActivites: vi.fn<
        (q: string) => Promise<ApiResponse<ActiviteAutocomplete[]>>
    >().mockResolvedValue({ success: true, data: [] }),
}))

const mockLister = vi.mocked(activiteService.listerActivitesAValider)
const mockDecider = vi.mocked(activiteService.deciderValidation)

function activite(overrides: Partial<ActiviteListItem> = {}): ActiviteListItem {
    return {
        id: 1,
        code: 'ACT-001',
        reference: 'REF-001',
        designation: 'Reduire le delai de traitement',
        dateDebutPrevue: '2027-01-04',
        dateFinPrevue: '2027-03-31',
        dateDebutReelle: null,
        dateFinReelle: null,
        dateCreation: '2027-01-02T08:00:00',
        objectifSpecifique: { id: 3, code: 'OS-01', libelle: 'Maitriser le risque' },
        service: { id: 1, libelle: 'Gestion des Risques' },
        typeActivite: { id: 1, libelle: 'Projet' },
        site: null,
        priorite: { id: 2, code: 'HAUTE', libelle: 'Haute' },
        statut: {
            id: 3,
            code: 'EN_ATTENTE_VALIDATION',
            libelle: 'En attente de validation',
        },
        avancement: 0,
        ...overrides,
    }
}

function page(
    items: ActiviteListItem[],
    totalElements = items.length
): ApiResponse<PageResult<ActiviteListItem>> {
    return {
        success: true,
        data: {
            content: items,
            page: 0,
            size: 9,
            totalElements,
            totalPages: 1,
            first: true,
            last: true,
        },
    }
}

/** Accuse de decision reussie. */
function decisionOk(
    overrides: Partial<DecisionResultat> = {}
): ApiResponse<DecisionResultat> {
    return {
        success: true,
        data: {
            idActivite: 1,
            code: 'ACT-001',
            decision: 'VALIDE',
            statut: 'VALIDEE',
            dateDecision: '2027-02-01T10:00:00',
            ...overrides,
        },
    }
}

/**
 * findAll()[i] est undefined selon le type : on le traite comme une absence
 * reelle plutot que de le masquer par un !, sinon un test qui ne trouve plus
 * le bouton echouerait sur "cannot read trigger of undefined" au lieu de dire
 * ce qu'il cherche.
 */
function bouton(wrapper: VueWrapper, texte: string) {
    const trouve = wrapper.findAll('button').find(b => b.text().includes(texte))

    if (!trouve) {
        throw new Error(`Bouton "${texte}" introuvable`)
    }

    return trouve
}

/**
 * Bouton d'UNE card, cherche dans cette card seulement.
 *
 * Indispensable ici : les libelles se contiennent les uns dans les autres --
 * "Valider la sélection" contient "Valider". Une recherche sur toute la page
 * renverrait le bouton groupe, qui n'agit pas sur la card visee et ne ferait
 * rien du tout quand rien n'est coche.
 */
function boutonCard(wrapper: VueWrapper, code: string, texte: string) {
    const carte = wrapper.findAll('.carte').find(c => c.text().includes(code))

    if (!carte) {
        throw new Error(`Card ${code} introuvable`)
    }

    const trouve = carte.findAll('button').find(b => b.text().includes(texte))

    if (!trouve) {
        throw new Error(`Bouton "${texte}" introuvable sur ${code}`)
    }

    return trouve
}

async function monter(items?: ActiviteListItem[]) {
    const router = createRouter({
        history: createMemoryHistory(),
        routes: [
            { path: '/', name: 'racine', component: { template: '<div />' } },
            {
                path: '/activites/a-valider',
                name: 'activites-a-valider',
                component: AValider,
            },
            {
                path: '/activites/:id',
                name: 'activite-detail',
                component: { template: '<div />' },
            },
        ],
    })

    await router.push('/activites/a-valider')
    await router.isReady()

    // Sans liste en parametre, le mock pose par le test -- liste vide, panne
    // reseau -- est conserve : le montage ne doit pas ecraser le scenario que
    // le test met en place.
    if (items) {
        mockLister.mockResolvedValue(page(items))
    }

    const wrapper = mount(AValider, { global: { plugins: [router] } })
    await flushPromises()

    return { wrapper, router }
}

/**
 * Le dernier element correspondant : la modale ouverte est la plus recente,
 * et c'est elle que l'utilisateur vient de poser.
 */
function dernier(selecteur: string): Element | undefined {
    const trouves = document.body.querySelectorAll(selecteur)

    return trouves.length ? trouves[trouves.length - 1] : undefined
}

/**
 * Valider la confirmation posee par la page.
 *
 * Le pied du bouton vit dans BaseModal, qui se teleporte dans le body : il se
 * cherche donc dans document.body. Le bouton se repere a son texte et non a sa
 * position : l'ordre des deux boutons du pied n'est pas une promesse, et
 * cliquer le mauvais reviendrait a annuler l'action qu'on veut prouver.
 */
async function confirmer() {
    const pied = dernier('.confirmation .modal-footer')

    const cible = pied
        ? Array.from(pied.querySelectorAll('button')).find(
            b => !(b.textContent ?? '').includes('Annuler')
        )
        : undefined

    if (!cible) {
        throw new Error('Bouton de confirmation introuvable')
    }

    cible.click()

    /*
        La chaine est emit -> confirmer -> action asynchrone -> retrait de la
        card. On attend que la modale soit partie, qui est sa derniere etape,
        plutot qu'un nombre fixe de flush : celui-ci serait trop court pour une
        action lente et inutile pour une rapide.
    */
    for (let i = 0; i < 20 && dernier('.confirmation'); i++) {
        await flushPromises()
    }
}

/** Bouton d'annulation de la confirmation, s'il y en a un. */
function annulerConfirmation() {
    const pied = dernier('.confirmation .modal-footer')

    const cible = Array.from(pied?.querySelectorAll('button') ?? []).find(b =>
        (b.textContent ?? '').includes('Annuler')
    )

    if (!cible) {
        throw new Error("Bouton d'annulation introuvable")
    }

    cible.click()
}

/** La case a cocher de la card portant ce code. */
function caseDe(wrapper: VueWrapper, code: string): DOMWrapper<HTMLInputElement> {
    const trouve = wrapper
        .findAll('input[type="checkbox"]')
        .find(c => c.attributes('aria-label') === `Sélectionner ${code}`)

    if (!trouve) {
        throw new Error(`Case de sélection pour ${code} introuvable`)
    }

    return trouve as DOMWrapper<HTMLInputElement>
}

/**
 * Remplir le motif et choisir une issue dans la modale de rejet.
 *
 * Les radios natifs sont coches via leur input et le modele est relu par
 * l'evenement change : c'est ce que fait un utilisateur, et surtout cela
 * evite de modifier l'etat interne du composant -- un test qui l'ecarterait
 * passerait sans que la page ne reponde reellement.
 */
async function choisirIssue(issue: 'REJETE' | 'RETOUR_MODIFICATION', motif: string) {
    const modal = dernier('.rejet-activite')

    if (!modal) {
        throw new Error('Modale de rejet introuvable')
    }

    const radios = Array.from(
        modal.querySelectorAll<HTMLInputElement>('input[type="radio"]')
    )

    const radio = radios.find(r => r.getAttribute('value') === issue)

    if (!radio) {
        throw new Error(`Issue ${issue} introuvable dans la modale`)
    }

    radio.checked = true
    radio.dispatchEvent(new Event('change', { bubbles: true }))
    await flushPromises()

    if (motif) {
        const zone = modal.querySelector('textarea')

        if (!zone) {
            throw new Error('Zone de motif introuvable')
        }

        zone.value = motif
        zone.dispatchEvent(new Event('input', { bubbles: true }))
        await flushPromises()
    }

    return modal
}

/** Cliquer le bouton de rejet de la modale de rejet. */
function validerRejet() {
    const modal = dernier('.rejet-activite')

    const cible = Array.from(modal?.querySelectorAll('button') ?? []).find(b =>
        (b.textContent ?? '').includes('Rejeter')
    )

    if (!cible) {
        throw new Error('Bouton de rejet introuvable')
    }

    cible.click()
}

/**
 * Arguments du n-ieme appel a la liste, ou echec explicite.
 *
 * Un appel manquant doit se voir dans le message du test, pas dans une erreur
 * d'indexation qui ne dit rien de ce qui a ete cherche.
 */
function appelLister(index: number): unknown[] {
    const args = mockLister.mock.calls[index]

    if (!args) {
        throw new Error(`La liste n'a pas ete demandee ${index + 1} fois`)
    }

    return args
}

/**
 * Arguments du n-ieme appel a la decision, ou echec explicite.
 *
 * Un appel manquant doit se voir dans le message du test, pas dans une erreur
 * d'indexation qui ne dit rien de ce qui a ete cherche.
 */
function appelDecision(index: number): unknown[] {
    const args = mockDecider.mock.calls[index]

    if (!args) {
        throw new Error(`Le service n'a pas ete appele ${index + 1} fois`)
    }

    return args
}

describe('AValider', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mockLister.mockResolvedValue(page([activite()]))
        mockDecider.mockResolvedValue(decisionOk())
    })

    /**
     * Trois activites distinctes : la selection se juge sur des cartes
     * multiples, sinon on ne verrait pas qu'une seule carte cochee suffit a
     * declencher une decision groupee.
     */
    const TROIS = () => [
        activite(),
        activite({ id: 2, code: 'ACT-002', reference: 'REF-002' }),
        activite({ id: 3, code: 'ACT-003', reference: 'REF-003' }),
    ]

    // ------------------------------------------------------------------
    // Chargement
    // ------------------------------------------------------------------

    describe('Le chargement', () => {
        it('affiche les activites en attente', async () => {
            const { wrapper } = await monter(TROIS())

            expect(wrapper.findAll('.carte')).toHaveLength(3)
            expect(wrapper.text()).toContain('ACT-001')
            expect(wrapper.text()).toContain('ACT-003')
        })

        it("interroge le service de liste a valider, et non celui des brouillons", async () => {
            await monter(TROIS())

            expect(mockLister).toHaveBeenCalledTimes(1)
        })

        it('ne restreint pas la liste a une annee ni a une periode', async () => {
            await monter(TROIS())

            const [filtres] = appelLister(0)

            /*
                Une activite en attente appartient a son exercice, mais la page
                qui la traite vide une file d'attente : la restreindre a une
                annee ferait disparaitre de la page ce qu'on y cherche.
            */
            expect(filtres).toMatchObject({
                annee: 0,
                trimestre: null,
                dateDebut: '',
                dateFin: '',
            })
        })

        it("affiche un etat vide quand il n'y a rien a valider", async () => {
            const { wrapper } = await monter([])

            expect(wrapper.find('.vide').exists()).toBe(true)
            expect(wrapper.text()).toContain('Aucune activité en attente de validation')
        })

        it('affiche un message quand la liste ne peut pas etre chargee', async () => {
            mockLister.mockResolvedValue({
                success: false,
                data: null,
                error: 'Service indisponible',
                status: 500,
            })

            const { wrapper } = await monter()

            expect(wrapper.text()).toContain('Service indisponible')
        })

        it("n'affiche pas d'etat vide quand le chargement echoue", async () => {
            mockLister.mockResolvedValue({
                success: false,
                data: null,
                error: 'Service indisponible',
                status: 500,
            })

            const { wrapper } = await monter()

            // Un vide tranquille ferait croire qu'il n'y a rien a valider,
            // alors qu'on ne sait pas.
            expect(wrapper.find('.vide').exists()).toBe(false)
        })
    })

    // ------------------------------------------------------------------
    // Selection
    // ------------------------------------------------------------------

    describe('La selection', () => {
        it('coche et decoche une card', async () => {
            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            expect(wrapper.text()).toContain('1 / 3 sélectionnée(s)')

            await caseDe(wrapper, 'ACT-001').setValue(false)
            expect(wrapper.text()).toContain('0 / 3 sélectionnée(s)')
        })

        it('selectionne toutes les cards affichees', async () => {
            const { wrapper } = await monter(TROIS())

            await bouton(wrapper, 'Tout sélectionner').trigger('click')

            expect(wrapper.text()).toContain('3 / 3 sélectionnée(s)')
        })

        it('decoche tout', async () => {
            const { wrapper } = await monter(TROIS())

            await bouton(wrapper, 'Tout sélectionner').trigger('click')
            await bouton(wrapper, 'Tout décocher').trigger('click')

            expect(wrapper.text()).toContain('0 / 3 sélectionnée(s)')
        })

        it("desactive la validation groupee tant que rien n'est coche", async () => {
            const { wrapper } = await monter(TROIS())

            expect(bouton(wrapper, 'Valider la sélection').attributes('disabled')).toBeDefined()
        })

        it('active la validation groupee des la premiere coche', async () => {
            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)

            expect(bouton(wrapper, 'Valider la sélection').attributes('disabled')).toBeUndefined()
        })
    })

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    describe('La validation', () => {
        it('demande confirmation avant de valider', async () => {
            const { wrapper } = await monter(TROIS())

            await boutonCard(wrapper, 'ACT-001', 'Valider').trigger('click')

            expect(dernier('.confirmation')).toBeDefined()
            expect(mockDecider).not.toHaveBeenCalled()
        })

        it("n'envoie rien si la confirmation est annulee", async () => {
            const { wrapper } = await monter(TROIS())

            await boutonCard(wrapper, 'ACT-001', 'Valider').trigger('click')
            annulerConfirmation()
            await flushPromises()

            expect(mockDecider).not.toHaveBeenCalled()
        })

        it('valide l activite et retire sa card', async () => {
            const { wrapper } = await monter(TROIS())

            await boutonCard(wrapper, 'ACT-001', 'Valider').trigger('click')
            await confirmer()

            expect(mockDecider).toHaveBeenCalledWith(1, 'VALIDE', null)
            expect(wrapper.findAll('.carte')).toHaveLength(2)
        })

        it('annonce la validation en nommant l activite', async () => {
            const { wrapper } = await monter(TROIS())

            await boutonCard(wrapper, 'ACT-001', 'Valider').trigger('click')
            await confirmer()

            // Nommee, et non comptée : sur une seule card, un total n'apprend
            // rien et l'utilisateur ne sait pas laquelle a ete traitee.
            expect(wrapper.text()).toContain('Activité ACT-001 validée')
        })

        it('retire aussi la card sur un 409, car elle a deja ete tranchee', async () => {
            mockDecider.mockResolvedValue({
                success: false,
                data: null,
                error: 'Cette activité a déjà été tranchée',
                status: 409,
            })

            const { wrapper } = await monter(TROIS())

            await boutonCard(wrapper, 'ACT-001', 'Valider').trigger('click')
            await confirmer()

            // Elle resterait avec une action qui echouera toujours.
            expect(wrapper.findAll('.carte')).toHaveLength(2)
        })

        it('garde la card sur un autre refus', async () => {
            mockDecider.mockResolvedValue({
                success: false,
                data: null,
                error: 'Motif manquant',
                status: 400,
            })

            const { wrapper } = await monter(TROIS())

            await boutonCard(wrapper, 'ACT-001', 'Valider').trigger('click')
            await confirmer()

            // L'utilisateur doit pouvoir corriger et reessayer.
            expect(wrapper.findAll('.carte')).toHaveLength(3)
            expect(wrapper.text()).toContain('Motif manquant')
        })

        it('garde la card quand le serveur est injoignable', async () => {
            mockDecider.mockRejectedValue(new Error('network'))

            const { wrapper } = await monter(TROIS())

            await boutonCard(wrapper, 'ACT-001', 'Valider').trigger('click')
            await confirmer()

            // Rien n'a ete decide : l'effacer ferait croire le contraire.
            expect(wrapper.findAll('.carte')).toHaveLength(3)
            expect(wrapper.text()).toContain('Connexion au serveur impossible')
        })
    })

    // ------------------------------------------------------------------
    // Rejet
    // ------------------------------------------------------------------

    describe('Le rejet', () => {
        it('ouvre la modale de rejet et ne decide de rien sans confirmation', async () => {
            const { wrapper } = await monter(TROIS())

            await boutonCard(wrapper, 'ACT-001', 'Rejeter').trigger('click')

            expect(dernier('.rejet-activite')).toBeDefined()
            expect(mockDecider).not.toHaveBeenCalled()
            expect(wrapper.findAll('.carte')).toHaveLength(3)
        })

        it('desactive la validation du rejet tant qu il n y a ni issue ni motif', async () => {
            const { wrapper } = await monter(TROIS())

            await boutonCard(wrapper, 'ACT-001', 'Rejeter').trigger('click')
            const modal = dernier('.rejet-activite') as HTMLElement

            const rejeter = Array.from(modal.querySelectorAll('button')).find(b =>
                (b.textContent ?? '').includes('Rejeter')
            )

            expect(rejeter?.hasAttribute('disabled')).toBe(true)
        })

        it('accepte une issue sans motif', async () => {
            const { wrapper } = await monter(TROIS())

            await boutonCard(wrapper, 'ACT-001', 'Rejeter').trigger('click')

            const modal = dernier('.rejet-activite') as HTMLElement
            const radios = Array.from(modal.querySelectorAll('input[type="radio"]'))
            const premier = radios[0] as HTMLInputElement

            premier.checked = true
            premier.dispatchEvent(new Event('change', { bubbles: true }))
            await flushPromises()

            const rejeter = Array.from(modal.querySelectorAll('button')).find(b =>
                (b.textContent ?? '').includes('Rejeter')
            )

            // Sans motif, le refus n'est pas opposable a l'auteur.
            expect(rejeter?.hasAttribute('disabled')).toBe(true)
        })

        it('envoie le rejet definitif avec son motif', async () => {
            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            await boutonCard(wrapper, 'ACT-001', 'Rejeter').trigger('click')
            await choisirIssue('REJETE', 'Budget incoherent')
            validerRejet()
            await flushPromises()
            await confirmer()

            expect(appelDecision(0)).toEqual([1, 'REJETE', 'Budget incoherent'])
        })

        it('envoie le retour pour modification quand il est choisi', async () => {
            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            await boutonCard(wrapper, 'ACT-001', 'Rejeter').trigger('click')
            await choisirIssue('RETOUR_MODIFICATION', 'Dates a corriger')
            validerRejet()
            await flushPromises()
            await confirmer()

            expect(appelDecision(0)).toEqual([1, 'RETOUR_MODIFICATION', 'Dates a corriger'])
        })

        it('rappelle la consequence de l issue choisie', async () => {
            const { wrapper } = await monter(TROIS())

            await boutonCard(wrapper, 'ACT-001', 'Rejeter').trigger('click')
            await choisirIssue('RETOUR_MODIFICATION', 'Dates a corriger')

            expect(dernier('.rejet-activite')?.textContent).toContain(
                'redevient modifiable'
            )
        })

        it('retire la card rejetee', async () => {
            mockDecider.mockResolvedValue(
                decisionOk({ decision: 'REJETE', statut: 'REJETE' })
            )

            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            await boutonCard(wrapper, 'ACT-001', 'Rejeter').trigger('click')
            await choisirIssue('REJETE', 'Hors perimetre')
            validerRejet()
            await flushPromises()
            await confirmer()

            expect(wrapper.findAll('.carte')).toHaveLength(2)
        })

        it("n'envoie rien si la confirmation finale est annulee", async () => {
            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            await boutonCard(wrapper, 'ACT-001', 'Rejeter').trigger('click')
            await choisirIssue('REJETE', 'Hors perimetre')
            validerRejet()
            await flushPromises()
            annulerConfirmation()
            await flushPromises()

            expect(mockDecider).not.toHaveBeenCalled()
        })

        it('repart de zero a chaque ouverture', async () => {
            const { wrapper } = await monter(TROIS())

            await boutonCard(wrapper, 'ACT-001', 'Rejeter').trigger('click')
            await choisirIssue('REJETE', 'Premier motif')

            // L'utilisateur annule puis rouvre sur une autre card.
            const fermer = dernier('.rejet-activite .close-btn') as HTMLElement
            fermer.click()
            await flushPromises()

            await boutonCard(wrapper, 'ACT-001', 'Rejeter').trigger('click')

            const modal = dernier('.rejet-activite') as HTMLElement
            const zone = modal.querySelector('textarea') as HTMLTextAreaElement

            // Un motif pre-rempli pour une activite sans rapport.
            expect(zone.value).toBe('')
        })
    })

    // ------------------------------------------------------------------
    // Lot
    // ------------------------------------------------------------------

    describe('Les decisions groupees', () => {
        it('valide les activites cochees dans l ordre de la selection', async () => {
            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-002').setValue(true)
            await caseDe(wrapper, 'ACT-003').setValue(true)
            await bouton(wrapper, 'Valider la sélection').trigger('click')
            await confirmer()

            expect(mockDecider).toHaveBeenCalledTimes(2)
            expect(appelDecision(0)[0]).toBe(2)
            expect(appelDecision(1)[0]).toBe(3)
        })

        it('vide la page apres un lot entierement reussi', async () => {
            const { wrapper } = await monter(TROIS())

            await bouton(wrapper, 'Tout sélectionner').trigger('click')
            await bouton(wrapper, 'Valider la sélection').trigger('click')
            await confirmer()

            expect(wrapper.findAll('.carte')).toHaveLength(0)
        })

        it("n'annule pas les decisions deja prises quand une card echoue", async () => {
            mockDecider
                .mockResolvedValueOnce(decisionOk({ idActivite: 1 }))
                .mockResolvedValueOnce({
                    success: false,
                    data: null,
                    error: 'Cette activité a déjà été tranchée',
                    status: 409,
                })

            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            await caseDe(wrapper, 'ACT-002').setValue(true)
            await bouton(wrapper, 'Valider la sélection').trigger('click')
            await confirmer()

            // Chaque requete est sa propre transaction : les reussies restent.
            expect(wrapper.findAll('.carte')).toHaveLength(1)
            expect(mockDecider).toHaveBeenCalledTimes(2)
        })

        it('interrompt le lot si la connexion tombe, et garde la selection', async () => {
            mockDecider
                .mockResolvedValueOnce(decisionOk({ idActivite: 1 }))
                .mockRejectedValueOnce(new Error('network'))

            const { wrapper } = await monterEtVider(TROIS())

            await bouton(wrapper, 'Tout sélectionner').trigger('click')
            await bouton(wrapper, 'Valider la sélection').trigger('click')
            await confirmer()

            // Le reste partirait sur une connexion morte.
            expect(mockDecider).toHaveBeenCalledTimes(2)
            expect(wrapper.findAll('.carte')).toHaveLength(2)
        })

        it('rejette toute la selection avec un seul motif', async () => {
            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            await caseDe(wrapper, 'ACT-002').setValue(true)
            await bouton(wrapper, 'Rejeter la sélection').trigger('click')
            await choisirIssue('REJETE', 'Meme motif pour les deux')
            validerRejet()
            await flushPromises()
            await confirmer()

            expect(appelDecision(0)).toEqual([1, 'REJETE', 'Meme motif pour les deux'])
            expect(appelDecision(1)).toEqual([2, 'REJETE', 'Meme motif pour les deux'])
        })

        it('desactive les actions groupees pendant une decision', async () => {
            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            await bouton(wrapper, 'Valider la sélection').trigger('click')
            await confirmer()

            // La carte decidee a quitte la page et la selection avec elle :
            // il ne reste rien a decisionner, donc plus rien a confirmer.
            expect(wrapper.text()).toContain('0 / 2 sélectionnée(s)')
            expect(bouton(wrapper, 'Valider la sélection').attributes('disabled')).toBeDefined()
        })
    })

    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------

    describe('La navigation', () => {
        it('ouvre le detail au clic sur la card', async () => {
            const { wrapper, router } = await monter(TROIS())

            const lien = wrapper.findAll('.carte__lien')[0]

            if (!lien) {
                throw new Error('Lien de la card introuvable')
            }

            await lien.trigger('click')
            await flushPromises()

            expect(router.currentRoute.value.name).toBe('activite-detail')
            expect(router.currentRoute.value.params.id).toBe('1')
        })
    })
})

/**
 * Variante de monter() qui attend la resolution de toutes les promesses en
 * cours, pour les tests dont la chaine emit -> action asynchrone doit etre
 * videe avant d'observer l'etat.
 */
async function monterEtVider(items?: ActiviteListItem[]) {
    const { wrapper, router } = await monter(items)

    for (let i = 0; i < 10; i++) {
        await flushPromises()
    }

    return { wrapper, router }
}

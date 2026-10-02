import { describe, it, expect, vi, afterEach, beforeEach } from 'vitest'

import {
    enableAutoUnmount,
    flushPromises,
    mount,
    type DOMWrapper,
    type VueWrapper,
} from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'

import Brouillons from '@/views/frontoffice/activites/Brouillons.vue'
import * as activiteService from '@/services/activite'
/*
    Chaque composant monte est demonte apres son test. C'est indispensable ici :
    BaseConfirm se teleporte dans document.body, donc une modale laissée ouverte
    par un test anterior survival dans le body et le test suivant cliquerait sur
    une confirmation qui n'est pas la sienne.
*/
enableAutoUnmount(afterEach)

import type { ApiResponse } from '@/services/api-client'
import type {
    ActiviteAutocomplete,
    ActiviteFiltres,
    ActiviteListItem,
    PageResult,
    SoumissionResultat,
} from '@/types/activite'

/**
 * Tests de la page des activites creees.
 *
 * Le service est mocke : cette page ne contient aucune regle metier, elle ne
 * fait qu'afficher une liste et appeler une ecriture. Ce qui est teste ici
 * est donc ce qui lui appartient : la bascule PTA / NON PTA, la navigation
 * vers le detail, et surtout le sort d'une carte apres soumission -- retiree
 * en cas de succes, retiree aussi en cas de 409, puisque l'activite a dans
 * les deux cas quitte les brouillons.
 */
vi.mock('@/services/activite', () => ({
    ANNEE_DEFAUT: 2027,
    listerActivitesBrouillon: vi.fn<
        () => Promise<ApiResponse<PageResult<ActiviteListItem>>>
    >(),
    soumettreValidation: vi.fn<
        (id: number) => Promise<ApiResponse<SoumissionResultat>>
    >(),
    autocompleterActivites: vi.fn<
        (q: string) => Promise<ApiResponse<ActiviteAutocomplete[]>>
    >().mockResolvedValue({ success: true, data: [] }),
}))

const mockLister = vi.mocked(activiteService.listerActivitesBrouillon)
const mockSoumettre = vi.mocked(activiteService.soumettreValidation)

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
        statut: { id: 1, code: 'BROUILLON', libelle: 'Brouillon' },
        avancement: 0,
        ...overrides,
    }
}

function page(items: ActiviteListItem[], totalElements = items.length): ApiResponse<PageResult<ActiviteListItem>> {
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

/**
 * findAll()[i] est undefined selon le type : on le traite comme une absence
 * reelle plutot que de le masquer par un !, sinon un test qui ne trouve plus
 * le bouton echouerait sur "cannot read trigger of undefined" au lieu de
 * dire ce qu il cherche.
 */
function bouton(wrapper: VueWrapper, texte: string) {
    const trouve = wrapper.findAll('button').find(b => b.text().includes(texte))

    if (!trouve) {
        throw new Error(`Bouton "${texte}" introuvable`)
    }

    return trouve
}

async function monter(items?: ActiviteListItem[]) {
    const router = createRouter({
        history: createMemoryHistory(),
        routes: [
            { path: '/', name: 'racine', component: { template: '<div />' } },
            {
                path: '/activites/brouillons',
                name: 'activites-brouillons',
                component: Brouillons,
            },
            {
                path: '/activites/:id',
                name: 'activite-detail',
                component: { template: '<div />' },
            },
        ],
    })

    await router.push('/activites/brouillons')
    await router.isReady()

    // Sans liste en parametre, le mock pose par le test -- liste vide, panne
    // reseau -- est conserve : le montage ne doit pas ecraser le scenario que
    // le test met en place.
    if (items) {
        mockLister.mockResolvedValue(page(items))
    }

    const wrapper = mount(Brouillons, { global: { plugins: [router] } })
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
 * Refuser la confirmation posee par la page.
 *
 * Le bouton se repere a son texte et non a sa position : l'ordre des deux
 * boutons du pied n'est pas une promesse, et cliquer le mauvais reviendrait a
 * valider l'action qu'on voulait annuler.
 */
function annuler() {
    const pied = dernier('.confirmation .modal-footer')

    const bouton = Array.from(pied?.querySelectorAll('button') ?? []).find(b =>
        (b.textContent ?? '').includes('Annuler')
    )

    if (!bouton) {
        throw new Error('Bouton d\'annulation introuvable')
    }

    bouton.click()
}

/**
 * Valider la confirmation posee par la page.
 *
 * Le pied du bouton vit dans BaseModal, qui se teleporte dans le body : il se
 * cherche donc dans document.body, comme les autres boutons de modale de ce
 * fichier. Sans cette etape aucun appel ne partirait -- ce qui est precisement
 * ce que doit prouver un test de confirmation.
 */
async function confirmer() {
    const pied = dernier('.confirmation .modal-footer')

    const bouton = pied
        ? Array.from(pied.querySelectorAll('button')).find(
            b => !(b.textContent ?? '').includes('Annuler')
        )
        : undefined

    if (!bouton) {
        throw new Error('Bouton de confirmation introuvable')
    }

    bouton.click()

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
 * Arguments du n-ieme appel au service, ou echec explicite.
 *
 * Un appel manquant doit se voir dans le message du test, pas dans une
 * erreur d'indexation qui ne dit rien de ce qui a ete cherche.
 */
function appel(index: number): unknown[] {
    const args = mockLister.mock.calls[index]

    if (!args) {
        throw new Error(`Le service n'a pas ete appele ${index + 1} fois`)
    }

    return args
}

describe('Brouillons', () => {

    beforeEach(() => {
        vi.clearAllMocks()
        mockLister.mockResolvedValue(page([activite()]))
    })

    /**
     * Trois activites distinctes : la selection se juge sur des cartes
     * multiples, sinon on ne verrait pas qu'une seule carte cochee suffit a
     * declencher une soumission groupee.
     */
    const TROIS = () => [
        activite(),
        activite({ id: 2, code: 'ACT-002', reference: 'REF-002' }),
        activite({ id: 3, code: 'ACT-003', reference: 'REF-003' }),
    ]

    // ------------------------------------------------------------------
    // Selection des cards
    // ------------------------------------------------------------------

    describe('La selection des cards', () => {
        it('propose une case sur chaque card, en haut a droite', async () => {

            const { wrapper } = await monter(TROIS())

            expect(wrapper.findAll('input[type="checkbox"]')).toHaveLength(3)

            const premiere = caseDe(wrapper, 'ACT-001')

            // La case est posee hors du flux, au coin : elle ne decale pas le
            // code et la reference de l'entete.
            expect(premiere.classes()).toContain('brouillon__case')
            expect(wrapper.find('.brouillon__selection').exists()).toBe(true)
        })

        it('coche et decoche une card', async () => {

            const { wrapper } = await monter(TROIS())

            const cible = caseDe(wrapper, 'ACT-002')

            await cible.setValue(true)
            expect(cible.element.checked).toBe(true)
            expect(wrapper.text()).toContain('1 / 3')

            await cible.setValue(false)
            expect(wrapper.text()).toContain('0 / 3')
        })

        it('marque visuellement la card cochee', async () => {

            const { wrapper } = await monter(TROIS())

            const card = wrapper.findAll('.brouillon')[1]

            await caseDe(wrapper, 'ACT-002').setValue(true)

            expect(card?.classes()).toContain('brouillon--selectionnee')
        })

        it('ne selectionne pas les autres cards', async () => {

            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-002').setValue(true)

            expect(caseDe(wrapper, 'ACT-001').element.checked).toBe(false)
            expect(caseDe(wrapper, 'ACT-003').element.checked).toBe(false)
        })

        it('n ouvre pas le detail quand on coche une card', async () => {

            const { wrapper, router } = await monter(TROIS())

            // Le lien etire couvre toute la card : sans le arret de
            // propagation, cocher ouvrirait le detail au lieu de cocher.
            await caseDe(wrapper, 'ACT-002').setValue(true)
            await flushPromises()

            expect(router.currentRoute.value.name).not.toBe('activite-detail')
        })

        it('coche les trois cards d un clic', async () => {

            const { wrapper } = await monter(TROIS())

            await bouton(wrapper, 'Tout sélectionner').trigger('click')

            for (const code of ['ACT-001', 'ACT-002', 'ACT-003']) {
                expect(caseDe(wrapper, code).element.checked).toBe(true)
            }

            expect(wrapper.text()).toContain('3 / 3')
        })

        it('n affiche le lot que s il y a des cards', async () => {

            mockLister.mockResolvedValue(page([]))

            const { wrapper } = await monter()

            // Aucun bouton de lot sur un etat vide : ils n'auraient rien a
            // selectionner ni a soumettre.
            expect(wrapper.find('.lot').exists()).toBe(false)
        })

        it('decoche tout', async () => {

            const { wrapper } = await monter(TROIS())

            await bouton(wrapper, 'Tout sélectionner').trigger('click')
            await bouton(wrapper, 'Tout décocher').trigger('click')

            expect(wrapper.text()).toContain('0 / 3')
        })

        it('desactive la soumission tant que rien n est coche', async () => {

            const { wrapper } = await monter(TROIS())

            const soumettreLot = bouton(wrapper, 'Soumettre la sélection')

            expect(soumettreLot.attributes('disabled')).toBeDefined()

            await caseDe(wrapper, 'ACT-001').setValue(true)

            expect(bouton(wrapper, 'Soumettre la sélection').attributes('disabled')).toBeUndefined()
        })

        it('ne propose pas de decocher quand rien n est coche', async () => {

            const { wrapper } = await monter(TROIS())

            // Le bouton n'apparait qu'a partir du moment ou il sert : un
            // bouton inactif au meme endroit occuperait la meme place.
            expect(wrapper.findAll('button').some(b => b.text().includes('Tout décocher'))).toBe(false)
        })
    })

    // ------------------------------------------------------------------
    // Soumission de la selection
    // ------------------------------------------------------------------

    describe('La soumission groupee', () => {
        beforeEach(() => {
            mockSoumettre.mockImplementation(async (id: number) => ({
                success: true,
                data: {
                    idActivite: id,
                    code: `ACT-00${id}`,
                    statut: 'EN_ATTENTE_VALIDATION',
                    dateSoumission: '2027-01-02T08:00:00',
                },
                error: null,
            }))
        })

        it('soumet les activites cochees et ne soumet que celles-la', async () => {

            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            await caseDe(wrapper, 'ACT-003').setValue(true)

            await bouton(wrapper, 'Soumettre la sélection').trigger('click')
            await confirmer()

            expect(mockSoumettre).toHaveBeenCalledTimes(2)
            expect(mockSoumettre).toHaveBeenCalledWith(1)
            expect(mockSoumettre).toHaveBeenCalledWith(3)
            expect(mockSoumettre).not.toHaveBeenCalledWith(2)
        })

        it('retire les cards soumises', async () => {

            const { wrapper } = await monter(TROIS())

            await bouton(wrapper, 'Tout sélectionner').trigger('click')
            await bouton(wrapper, 'Soumettre la sélection').trigger('click')
            await confirmer()

            expect(wrapper.find('.brouillon').exists()).toBe(false)
            expect(wrapper.text()).toContain('3 activit')
        })

        it('conserve une card refusee et affiche le refus', async () => {

            mockSoumettre.mockImplementation(async (id: number) =>
                id === 2
                    ? {
                        success: false,
                        data: null,
                        error: 'Seule une activite en brouillon peut etre soumise a la validation',
                        status: 400,
                    }
                    : {
                        success: true,
                        data: {
                            idActivite: id,
                            code: `ACT-00${id}`,
                            statut: 'EN_ATTENTE_VALIDATION',
                            dateSoumission: '2027-01-02T08:00:00',
                        },
                        error: null,
                    }
            )

            const { wrapper } = await monter(TROIS())

            await bouton(wrapper, 'Tout sélectionner').trigger('click')
            await bouton(wrapper, 'Soumettre la sélection').trigger('click')
            await confirmer()

            // Un refus ne doit pas faire disparaitre l'activite : le message
            // porte sur elle, et elle doit rester la pour etre corrigee.
            expect(wrapper.findAll('.brouillon')).toHaveLength(1)
            expect(wrapper.text()).toContain('ACT-002')
            expect(wrapper.text()).toContain('Seule une activite en brouillon')
        })

        it('garde la carte refusee cochee, pour voir pourquoi', async () => {

            mockSoumettre.mockResolvedValue({
                success: false,
                data: null,
                error: 'Refus',
                status: 400,
            })

            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-002').setValue(true)
            await bouton(wrapper, 'Soumettre la sélection').trigger('click')
            await confirmer()

            // Elle reste cochee : c'est elle qui a echoue, la decocher ferait
            // perdre le lien entre le refus et la carte.
            expect(caseDe(wrapper, 'ACT-002').element.checked).toBe(true)
            expect(wrapper.text()).toContain('1 / 3')
        })

        it('interrompt le lot sur une panne reseau et ne perd pas la selection', async () => {

            mockSoumettre.mockRejectedValue(new Error('NetworkError'))

            const { wrapper } = await monter(TROIS())

            await bouton(wrapper, 'Tout sélectionner').trigger('click')
            await bouton(wrapper, 'Soumettre la sélection').trigger('click')
            await confirmer()

            expect(wrapper.text()).toContain('Connexion au serveur impossible')

            // Aucune activite n'a ete soumise : le lot s'arrete plutot que de
            // continuer en pretendant que la premiere a reussi.
            expect(mockSoumettre).toHaveBeenCalledTimes(1)
            expect(wrapper.findAll('.brouillon')).toHaveLength(3)

            // La selection survit a la panne : l'utilisateur peut reessayer
            // d'un clic, sans recocher les trois cards.
            expect(wrapper.text()).toContain('3 / 3')
        })

        it('n envoie rien quand le bouton est inactif', async () => {

            const { wrapper } = await monter(TROIS())

            // Le bouton desactive ne se déclenche pas : le control est que le
            // service ne soit pas appele, pas que le clic soit ignore.
            expect(bouton(wrapper, 'Soumettre la sélection').attributes('disabled')).toBeDefined()
            expect(mockSoumettre).not.toHaveBeenCalled()
        })

        it('prend en compte les cards affichees seulement', async () => {

            const items = TROIS()
            items.push(activite({ id: 4, code: 'ACT-004', reference: 'REF-004' }))

            mockLister.mockResolvedValue(page(items, 12))

            const { wrapper } = await monter(items)

            await bouton(wrapper, 'Tout sélectionner').trigger('click')
            await bouton(wrapper, 'Soumettre la sélection').trigger('click')
            await confirmer()

            // La page est paginee : "tout selectionner" porte sur ce qui est a
            // l'ecran, sinon l'utilisateur soumettrait des activites qu'il
            // n'a pas vues.
            expect(mockSoumettre).toHaveBeenCalledTimes(4)
        })
    })

    // ------------------------------------------------------------------
    // Confirmation avant soumission
    // ------------------------------------------------------------------

    describe('La confirmation avant soumission', () => {
        it('ne soumet rien tant que la demande n est pas validee', async () => {

            mockSoumettre.mockResolvedValue({
                success: true,
                data: {
                    idActivite: 1,
                    code: 'ACT-001',
                    statut: 'EN_ATTENTE_VALIDATION',
                    dateSoumission: '2027-01-02T08:00:00',
                },
                error: null,
            })

            const { wrapper } = await monter(TROIS())

            await bouton(wrapper, 'Tout sélectionner').trigger('click')
            await bouton(wrapper, 'Soumettre la sélection').trigger('click')
            await flushPromises()

            // Le clic seul ne part rien : c'est le point de la confirmation.
            expect(mockSoumettre).not.toHaveBeenCalled()

            // Elle nomme l'activite, sinon l'utilisateur ne sait pas ce qu il
            // valide -- et le message doit changer selon la cardinalite.
            expect(document.body.textContent).toContain('3 activité(s) seront figées')
        })

        it('part au clic sur le bouton de confirmation', async () => {

            const { wrapper } = await monter(TROIS())

            await bouton(wrapper, 'Tout sélectionner').trigger('click')
            await bouton(wrapper, 'Soumettre la sélection').trigger('click')
            await flushPromises()
            await confirmer()

            expect(mockSoumettre).toHaveBeenCalledTimes(3)
        })

        it('ne part pas si la demande est annulee', async () => {

            const { wrapper } = await monter(TROIS())

            await bouton(wrapper, 'Tout sélectionner').trigger('click')
            await bouton(wrapper, 'Soumettre la sélection').trigger('click')
            await flushPromises()

            annuler()
            await flushPromises()

            expect(mockSoumettre).not.toHaveBeenCalled()

            // La selection est intacte : annuler ne doit pas faire perdre le
            // travail de cocher les cards.
            expect(wrapper.text()).toContain('3 / 3')
        })

        it('ferme la modale apres une annulation', async () => {

            const { wrapper } = await monter(TROIS())

            await bouton(wrapper, 'Tout sélectionner').trigger('click')
            await bouton(wrapper, 'Soumettre la sélection').trigger('click')
            await flushPromises()

            annuler()
            await flushPromises()

            expect(dernier('.confirmation')).toBeUndefined()
        })

        it('propose un libelle de confirmation qui porte le nombre', async () => {

            const { wrapper } = await monter(TROIS())

            await bouton(wrapper, 'Tout sélectionner').trigger('click')
            await bouton(wrapper, 'Soumettre la sélection').trigger('click')
            await flushPromises()

            // "Soumettre" seul ne dirait pas si l'on valide une activite ou
            // dix : le compte est dans le libelle.
            expect(document.body.textContent).toContain('Soumettre 3')
        })

        it('affiche la confirmation d une card seule avec son code', async () => {

            const { wrapper } = await monter(TROIS())

            await bouton(wrapper, 'Soumettre en validation').trigger('click')
            await flushPromises()

            expect(document.body.textContent).toContain('ACT-001')
            expect(document.body.textContent).toContain('Soumettre')
        })
    })

    it('liste les activites en brouillon', async () => {

        const { wrapper } = await monter()

        expect(mockLister).toHaveBeenCalledOnce()
        expect(wrapper.text()).toContain('ACT-001')
        expect(wrapper.text()).toContain('Reduire le delai de traitement')
        expect(wrapper.text()).toContain('OS-01')
    })

    it('n envoie ni annee ni periode', async () => {

        await monter()

        // Un brouillon n'appartient pas a un exercice de suivi : le restreindre
        // a une annee le ferait disparaitre de la page qui sert a le soumettre.
        const filtres = appel(0)[0] as ActiviteFiltres
        expect(filtres.annee).toBe(0)
        expect(filtres.trimestre).toBeNull()
        expect(filtres.dateDebut).toBe('')
        expect(filtres.dateFin).toBe('')
    })

    it('bascule entre PTA et NON PTA', async () => {

        const { wrapper } = await monter()

        const ongletNonPta = bouton(wrapper, 'NON PTA')
        await ongletNonPta.trigger('click')
        await flushPromises()

        expect(mockLister).toHaveBeenCalledTimes(2)
        expect(appel(1)[1]).toBe('NON_PTA')
    })

    it('ouvre le detail au clic sur une card', async () => {

        const { wrapper, router } = await monter()

        await wrapper.find('.brouillon__lien').trigger('click')
        await flushPromises()

        expect(router.currentRoute.value.name).toBe('activite-detail')
        expect(router.currentRoute.value.params.id).toBe('1')
    })

    it('retire la card et confirme apres une soumission reussie', async () => {

        mockSoumettre.mockResolvedValue({
            success: true,
            data: {
                idActivite: 1,
                code: 'ACT-001',
                statut: 'EN_ATTENTE_VALIDATION',
                dateSoumission: '2027-01-02T08:00:00',
            },
            error: null,
        })

        const { wrapper } = await monter()

        await bouton(wrapper, 'Soumettre en validation').trigger('click')
        await confirmer()

        expect(mockSoumettre).toHaveBeenCalledWith(1)
        expect(wrapper.find('.brouillon').exists()).toBe(false)
        expect(wrapper.text()).toContain('soumise à la validation')
    })

    it('retire aussi la card sur un conflit, l activite ayant quitte les brouillons', async () => {

        mockSoumettre.mockResolvedValue({
            success: false,
            data: null,
            error: 'Seule une activite en brouillon peut etre soumise a la validation',
            status: 409,
        })

        const { wrapper } = await monter()

        await bouton(wrapper, 'Soumettre en validation').trigger('click')
        await confirmer()

        expect(wrapper.find('.brouillon').exists()).toBe(false)
        expect(wrapper.text()).toContain('Seule une activite en brouillon')
    })

    it('garde la card sur un refus de regle metier, hors conflit', async () => {

        // 400 : regle non respectee, l activite est toujours en brouillon.
        // La retirer ferait perdre une activite a completer.
        mockSoumettre.mockResolvedValue({
            success: false,
            data: null,
            error: "L'activite doit comporter au moins une sous-activite",
            status: 400,
        })

        const { wrapper } = await monter()

        await bouton(wrapper, 'Soumettre en validation').trigger('click')
        await confirmer()

        expect(wrapper.find('.brouillon').exists()).toBe(true)
        expect(wrapper.text()).toContain("au moins une sous-activite")
    })

    it('affiche un etat vide explicite', async () => {

        mockLister.mockResolvedValue(page([]))

        const { wrapper } = await monter()

        expect(wrapper.find('.vide').exists()).toBe(true)
        expect(wrapper.text()).toContain('Aucune activité en cours de rédaction')
    })

    it('remonte une panne reseau sans laisser la page en chargement', async () => {

        mockLister.mockRejectedValue(new Error('NetworkError'))

        const { wrapper } = await monter()

        expect(wrapper.text()).toContain('Connexion au serveur impossible')
        expect(wrapper.text()).not.toContain('Chargement')
    })
})

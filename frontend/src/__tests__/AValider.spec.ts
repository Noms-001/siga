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
    RejetActiviteModal se teleportent dans document.body, donc une modale
    laissee ouverte par un test anterior survivrait dans le body, et le test
    suivant cliquerait sur une confirmation qui n'est pas la sienne.
*/
enableAutoUnmount(afterEach)

import type { ApiResponse } from '@/services/api-client'
import type {
    ActiviteListItem,
    DecisionValidation,
    PageResult,
    ValidationActiviteResponse,
} from '@/types/activite'

/**
 * Tests de la page des activites a valider.
 *
 * Le service est mocke : cette page ne contient aucune regle metier, elle ne
 * fait qu'afficher une liste et appeler une ecriture. Ce qui est teste ici
 * est donc ce qui lui appartient : la liste chargee depuis le bon endpoint,
 * le sort d'une ligne apres decision, et le parcours de rejet -- qui est le
 * seul a deux etapes, parce qu'il demande un motif.
 *
 * LE CONTRAT DE /a-valider EST UNE LISTE GROUPEE
 *
 * La reponse est un tableau d'objets a UNE cle : la designation de l'etape.
 * Le mock doit donc renvoyer cette forme, et non une liste plate -- sinon
 * les tests passeraient sur une structure que le backend ne produit pas, et
 * la page serait cassee des la premiere reponse reelle.
 */
vi.mock('@/services/activite', () => ({
    listerActivitesAValider: vi.fn<
        () => Promise<ApiResponse<PageResult<Record<string, ActiviteListItem>>>>
    >(),
    validerActivite: vi.fn<
        (id: number, rejeter: boolean, commentaire?: string | null) => Promise<
            ApiResponse<ValidationActiviteResponse>
        >
    >(),
    soumettreValidationRetour: vi.fn<
        (id: number, commentaire?: string | null) => Promise<
            ApiResponse<ValidationActiviteResponse>
        >
    >(),
}))

const mockLister = vi.mocked(activiteService.listerActivitesAValider)
const mockValider = vi.mocked(activiteService.validerActivite)
const mockRetour = vi.mocked(activiteService.soumettreValidationRetour)

/**
 * Fixture d'activite.
 *
 * Les trois etapes sont portees dans le DTO : sur /a-valider, le backend
 * les renseigne toutes les trois -- etapeSuivante et etapePrecedente
 * peuvent etre null aux deux bouts du circuit, mais etapeCourante est
 * toujours presente. Les fixtures la gardent donc non nulle par defaut.
 */
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
        etapeCourante: { id: 3, libelle: 'Contrôle qualité' },
        etapeSuivante: { id: 4, libelle: 'Validation finale' },
        etapePrecedente: { id: 2, libelle: 'Préparation' },
        ...overrides,
    }
}

/**
 * Reponse paginee pour /a-valider, dans la forme REELLE du backend.
 *
 * Chaque activite est enveloppee dans un objet a UNE cle -- la designation
 * de son etape. Le groupement est fait cote backend pour que l'affichage
 * puisse separer par etape sans re-parcourir ; le front aplatit ensuite.
 *
 * Le helper est volontairement le SEUL endroit ou cette forme est
 * construite : si le contrat change, un seul helper a corriger.
 */
function page(
    items: ActiviteListItem[]
): ApiResponse<PageResult<Record<string, ActiviteListItem>>> {
    return {
        success: true,
        data: {
            content: items.map(item => ({
                [item.etapeCourante?.libelle ?? 'Sans étape']: item,
            })),
            page: 0,
            size: 9,
            totalElements: items.length,
            totalPages: 1,
            first: true,
            last: true,
        },
    }
}

/**
 * Accuse d'une decision reussie.
 *
 * Le statut reflete ce qui a ete decide : le backend renvoie le statut
 * APRES la decision, et c'est lui qui permet a la page de retirer la
 * ligne sans relire l'activite.
 */
function decisionOk(
    overrides: Partial<ValidationActiviteResponse> = {}
): ApiResponse<ValidationActiviteResponse> {
    return {
        success: true,
        data: {
            idValidationActivite: 1,
            decision: 'VALIDE' as DecisionValidation,
            commentaire: null,
            dateDemande: '2027-01-15T09:00:00',
            dateDecision: '2027-02-01T10:00:00',
            idActivite: 1,
            idEtapeValidation: 3,
            designationEtape: 'Contrôle qualité',
            derniereDecision: 'RETOUR_MODIFICATION',
            niveau: 3,
            obligatoire: true,
            idDemandeur: 5,
            idDecideur: 7,
            ...overrides,
        },
    }
}

/**
 * Bouton de la barre d'outils (hors menu par ligne).
 *
 * Les libelles se contiennent les uns dans les autres -- "Valider" est un
 * prefixe de "Valider la sélection" -- donc toute recherche doit etre
 * restreinte a sa zone. Ici la barre de lot.
 */
function boutonToolbar(wrapper: VueWrapper, texte: string) {
    const toolbar = wrapper.find('.lot')

    if (!toolbar.exists()) {
        throw new Error("Barre de lot absente -- aucune sélection en cours ?")
    }

    const trouve = toolbar.findAll('button').find(b => b.text().includes(texte))

    if (!trouve) {
        throw new Error(`Bouton "${texte}" introuvable dans la barre de lot`)
    }

    return trouve
}

/** Localise la ligne du tableau portant ce code. */
function ligne(wrapper: VueWrapper, code: string) {
    const trouve = wrapper
        .findAll('.tableau__tr')
        .find(tr => tr.text().includes(code))

    if (!trouve) {
        throw new Error(`Ligne ${code} introuvable`)
    }

    return trouve
}

/**
 * Ouvre le menu d'actions d'une ligne et rend sa racine.
 *
 * Le clic sur le bouton stop la propagation (voir le composant) : le
 * listener document qui ferme le menu ne s'en mele pas. L'appel reste
 * neanmoins asynchrone pour laisser Vue basculer l'etat.
 */
async function ouvrirMenu(wrapper: VueWrapper, code: string) {
    const tr = ligne(wrapper, code)
    const toggle = tr.find('.menu__toggle')

    if (!toggle.exists()) {
        throw new Error(`Bouton de menu introuvable sur ${code}`)
    }

    await toggle.trigger('click')
    await flushPromises()

    const menu = tr.find('.menu')

    if (!menu.exists()) {
        throw new Error(`Menu de ${code} non ouvert`)
    }

    return menu
}

/** Ouvre le menu d'une ligne et clique l'action nommee. */
async function actionMenu(wrapper: VueWrapper, code: string, action: string) {
    const menu = await ouvrirMenu(wrapper, code)

    const item = menu.findAll('button').find(b => b.text().includes(action))

    if (!item) {
        throw new Error(`Action "${action}" introuvable pour ${code}`)
    }

    await item.trigger('click')
    await flushPromises()
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

    /*
        Sans liste en parametre, le mock pose par le test -- liste vide,
        panne reseau -- est conserve : le montage ne doit pas ecraser le
        scenario que le test met en place.
    */
    if (items) {
        mockLister.mockResolvedValue(page(items))
    }

    const wrapper = mount(AValider, { global: { plugins: [router] } })
    await flushPromises()

    return { wrapper, router }
}

/**
 * Dernier element correspondant : la modale ouverte est la plus recente, et
 * c'est elle que l'utilisateur vient de poser.
 */
function dernier(selecteur: string): Element | undefined {
    const trouves = document.body.querySelectorAll(selecteur)

    return trouves.length ? trouves[trouves.length - 1] : undefined
}

/**
 * Valider la confirmation posee par la page.
 *
 * Le pied du bouton vit dans BaseModal, qui se teleporte dans le body : il
 * se cherche donc dans document.body. Le bouton se repere a son texte et
 * non a sa position : l'ordre des deux boutons du pied n'est pas une
 * promesse, et cliquer le mauvais reviendrait a annuler l'action qu'on veut
 * prouver.
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
        La chaine est emit -> confirmer -> action asynchrone -> retrait de
        la ligne. On attend que la modale soit partie, qui est sa derniere
        etape, plutot qu'un nombre fixe de flush : celui-ci serait trop
        court pour une action lente et inutile pour une rapide.
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

/** La case a cocher de la ligne portant ce code. */
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
 * Remplir le motif dans la modale de rejet ou de retour.
 *
 * La modale recoit desormais l'issue en prop : le groupe radio est cache,
 * et la page a deja tranche en ouvrant. Ce helper ne coche donc plus
 * d'issue -- il remplit le motif, ce qui est la seule chose qui reste a
 * faire dans la modale.
 */
async function saisirMotif(motif: string) {
    const modal = dernier('.rejet-activite') as HTMLElement | undefined

    if (!modal) {
        throw new Error('Modale de rejet introuvable')
    }

    const zone = modal.querySelector('textarea')

    if (!zone) {
        throw new Error('Zone de motif introuvable')
    }

    zone.value = motif
    zone.dispatchEvent(new Event('input', { bubbles: true }))
    await flushPromises()

    return modal
}

/**
 * Cliquer le bouton de confirmation de la modale de rejet ou de retour.
 *
 * Le libelle du bouton suit l'issue : "Rejeter" pour un rejet definitif,
 * "Renvoyer" pour un retour. Un helper distinct de la recherche libre
 * evite qu'un test confonde les deux -- le clic se fait sur le dernier
 * bouton du pied, qui est toujours celui de l'action.
 */
function validerModale() {
    const modal = dernier('.rejet-activite .modal-footer')

    const boutons = Array.from(modal?.querySelectorAll('button') ?? [])
    const cible = boutons.find(b => !(b.textContent ?? '').includes('Annuler'))

    if (!cible) {
        throw new Error('Bouton de confirmation introuvable dans la modale')
    }

    cible.click()
}

/**
 * Arguments du n-ieme appel a la liste, ou echec explicite.
 *
 * Un appel manquant doit se voir dans le message du test, pas dans une
 * erreur d'indexation qui ne dit rien de ce qui a ete cherche.
 */
function appelLister(index: number): unknown[] {
    const args = mockLister.mock.calls[index]

    if (!args) {
        throw new Error(`La liste n'a pas ete demandee ${index + 1} fois`)
    }

    return args
}

/** Arguments du n-ieme appel a validerActivite, ou echec explicite. */
function appelValider(index: number): unknown[] {
    const args = mockValider.mock.calls[index]

    if (!args) {
        throw new Error(`validerActivite n'a pas ete appele ${index + 1} fois`)
    }

    return args
}

/** Arguments du n-ieme appel a soumettreValidationRetour, ou echec explicite. */
function appelRetour(index: number): unknown[] {
    const args = mockRetour.mock.calls[index]

    if (!args) {
        throw new Error(`soumettreValidationRetour n'a pas ete appele ${index + 1} fois`)
    }

    return args
}

describe('AValider', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mockLister.mockResolvedValue(page([activite()]))
        mockValider.mockResolvedValue(decisionOk())
        mockRetour.mockResolvedValue(
            decisionOk({
                decision: 'RETOUR_MODIFICATION' as DecisionValidation,
                commentaire: null,
            })
        )
    })

    /**
     * Trois activites distinctes : la selection se juge sur des lignes
     * multiples, sinon on ne verrait pas qu'une seule cochee suffit a
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

            expect(wrapper.findAll('.tableau__tr')).toHaveLength(3)
            expect(wrapper.text()).toContain('ACT-001')
            expect(wrapper.text()).toContain('ACT-003')
        })

        it('interroge le service de la file a valider', async () => {
            await monter(TROIS())

            expect(mockLister).toHaveBeenCalledTimes(1)
        })

        it("n'envoie aucun argument de filtre", async () => {
            await monter(TROIS())

            /*
                Le endpoint /a-valider ne prend rien : ni annee, ni vue, ni
                page. Une activite en attente appartient a son exercice,
                mais la page qui la traite vide une file -- la restreindre
                ferait disparaitre de la page ce qu'on y cherche.
            */
            expect(appelLister(0)).toEqual([])
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

        it('aplatit le groupement par etape du backend', async () => {
            /*
                Le backend renvoie un tableau de { designationEtape: activite }.
                Deux activites a la meme etape doivent apparaitre comme deux
                lignes distinctes, chacune avec son code.
            */
            const { wrapper } = await monter([
                activite({ id: 1, code: 'ACT-001' }),
                activite({ id: 2, code: 'ACT-002' }),
            ])

            expect(wrapper.findAll('.tableau__tr')).toHaveLength(2)
            expect(wrapper.text()).toContain('ACT-001')
            expect(wrapper.text()).toContain('ACT-002')
        })

        it('affiche les etapes courante et suivante', async () => {
            const { wrapper } = await monter([activite()])

            expect(wrapper.text()).toContain('Contrôle qualité')
            expect(wrapper.text()).toContain('Validation finale')
        })
    })

    // ------------------------------------------------------------------
    // Menu d'actions
    // ------------------------------------------------------------------

    describe("Le menu d'actions", () => {
        it("ne s'ouvre pas avant clic", async () => {
            const { wrapper } = await monter(TROIS())

            expect(wrapper.find('.menu').exists()).toBe(false)
        })

        it("s'ouvre au clic sur les trois points", async () => {
            const { wrapper } = await monter(TROIS())

            await ouvrirMenu(wrapper, 'ACT-001')

            const menu = ligne(wrapper, 'ACT-001').find('.menu')

            expect(menu.exists()).toBe(true)
            expect(menu.text()).toContain('Valider')
            expect(menu.text()).toContain('Rejeter')
            expect(menu.text()).toContain("Soumettre à l'étape précédente")
        })

        it("ne laisse qu'un menu ouvert a la fois", async () => {
            const { wrapper } = await monter(TROIS())

            await ouvrirMenu(wrapper, 'ACT-001')
            await ouvrirMenu(wrapper, 'ACT-002')

            expect(wrapper.findAll('.menu')).toHaveLength(1)
            expect(ligne(wrapper, 'ACT-002').find('.menu').exists()).toBe(true)
        })

        it('se referme au second clic sur les trois points', async () => {
            const { wrapper } = await monter(TROIS())

            await ouvrirMenu(wrapper, 'ACT-001')
            await ouvrirMenu(wrapper, 'ACT-001')

            expect(wrapper.find('.menu').exists()).toBe(false)
        })

        it('se referme au clic ailleurs sur la page', async () => {
            const { wrapper } = await monter(TROIS())

            await ouvrirMenu(wrapper, 'ACT-001')

            document.body.click()
            await flushPromises()

            expect(wrapper.find('.menu').exists()).toBe(false)
        })

        it('se referme sur Echap', async () => {
            const { wrapper } = await monter(TROIS())

            await ouvrirMenu(wrapper, 'ACT-001')

            document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
            await flushPromises()

            expect(wrapper.find('.menu').exists()).toBe(false)
        })
    })

    // ------------------------------------------------------------------
    // Selection
    // ------------------------------------------------------------------

    describe('La selection', () => {
        it('coche et decoche une ligne', async () => {
            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            expect(wrapper.text()).toContain('1 / 3 sélectionnée(s)')

            await caseDe(wrapper, 'ACT-001').setValue(false)
            expect(wrapper.text()).toContain('0 / 3 sélectionnée(s)')
        })

        it('selectionne toutes les lignes affichees', async () => {
            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            await boutonToolbar(wrapper, 'Tout sélectionner').trigger('click')

            expect(wrapper.text()).toContain('3 / 3 sélectionnée(s)')
        })

        it('decoche tout', async () => {
            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            await boutonToolbar(wrapper, 'Tout décocher').trigger('click')

            expect(wrapper.text()).toContain('0 / 3 sélectionnée(s)')
        })

        it("n'affiche pas la barre de lot tant que rien n'est coche", async () => {
            const { wrapper } = await monter(TROIS())

            expect(wrapper.find('.lot').exists()).toBe(false)
        })

        it('affiche la barre de lot des la premiere coche', async () => {
            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)

            expect(wrapper.find('.lot').exists()).toBe(true)
        })

        it('la case d en-tete coche toutes les lignes', async () => {
            const { wrapper } = await monter(TROIS())

            const entete = wrapper.find('.tableau__th--case input')
            await entete.setValue(true)

            expect(wrapper.text()).toContain('3 / 3 sélectionnée(s)')
        })
    })

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    describe('La validation', () => {
        it('demande confirmation avant de valider', async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', 'Valider')

            expect(dernier('.confirmation')).toBeDefined()
            expect(mockValider).not.toHaveBeenCalled()
        })

        it("n'envoie rien si la confirmation est annulee", async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', 'Valider')
            annulerConfirmation()
            await flushPromises()

            expect(mockValider).not.toHaveBeenCalled()
        })

        it('valide la ligne et la retire du tableau', async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', 'Valider')
            await confirmer()

            expect(appelValider(0)).toEqual([1, false, null])
            expect(wrapper.findAll('.tableau__tr')).toHaveLength(2)
        })

        it("annonce la validation en nommant l'activite", async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', 'Valider')
            await confirmer()

            // Nommee, et non comptee : sur une seule ligne, un total
            // n'apprend rien et l'utilisateur ne sait pas laquelle a ete
            // traitee.
            expect(wrapper.text()).toContain('Activité ACT-001 validée')
        })

        it('retire aussi la ligne sur un 409, car elle a deja ete tranchee', async () => {
            mockValider.mockResolvedValue({
                success: false,
                data: null,
                error: 'Cette activité a déjà été tranchée',
                status: 409,
            })

            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', 'Valider')
            await confirmer()

            // Elle resterait avec une action qui echouera toujours.
            expect(wrapper.findAll('.tableau__tr')).toHaveLength(2)
        })

        it('garde la ligne sur un autre refus', async () => {
            mockValider.mockResolvedValue({
                success: false,
                data: null,
                error: 'Motif manquant',
                status: 400,
            })

            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', 'Valider')
            await confirmer()

            // L'utilisateur doit pouvoir corriger et reessayer.
            expect(wrapper.findAll('.tableau__tr')).toHaveLength(3)
            expect(wrapper.text()).toContain('Motif manquant')
        })

        it('garde la ligne quand le serveur est injoignable', async () => {
            mockValider.mockRejectedValue(new Error('network'))

            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', 'Valider')
            await confirmer()

            // Rien n'a ete decide : l'effacer ferait croire le contraire.
            expect(wrapper.findAll('.tableau__tr')).toHaveLength(3)
            expect(wrapper.text()).toContain('Connexion au serveur impossible')
        })
    })

    // ------------------------------------------------------------------
    // Rejet (issue imposee REJETE)
    // ------------------------------------------------------------------

    describe('Le rejet', () => {
        it('ouvre la modale sur l\'issue "Rejeter"', async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', 'Rejeter')

            const modal = dernier('.rejet-activite')
            expect(modal).toBeDefined()
            expect(modal?.textContent).toContain("Rejeter l'activité")
        })

        it("n'affiche pas le choix d'issue, deja tranche par le menu", async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', 'Rejeter')

            const modal = dernier('.rejet-activite') as HTMLElement

            /*
                Le menu a deja choisi "Rejeter" : reproposer le choix serait
                redondant, et dangereux -- un utilisateur qui a clique
                "Rejeter" ne s'attend pas a pouvoir renvoyer par megarde.
            */
            expect(modal.querySelector('input[type="radio"]')).toBeNull()
        })

        it('desactive le bouton tant que le motif est vide', async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', 'Rejeter')

            const pied = dernier('.rejet-activite .modal-footer') as HTMLElement

            const bouton = Array.from(pied.querySelectorAll('button')).find(b =>
                (b.textContent ?? '').includes('Rejeter')
            )

            // Sans motif, un refus n'est pas opposable a l'auteur.
            expect(bouton?.hasAttribute('disabled')).toBe(true)
        })

        it('active le bouton des que le motif est saisi', async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', 'Rejeter')
            await saisirMotif('Budget incoherent')

            const pied = dernier('.rejet-activite .modal-footer') as HTMLElement

            const bouton = Array.from(pied.querySelectorAll('button')).find(b =>
                (b.textContent ?? '').includes('Rejeter')
            )

            expect(bouton?.hasAttribute('disabled')).toBe(false)
        })

        it('pose une confirmation apres le motif, pas avant', async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', 'Rejeter')
            await saisirMotif('Budget incoherent')

            // Pas encore de confirmation tant que la modale n'a pas ete
            // validee : la confirmation ne confirme que ce qui est saisi.
            expect(dernier('.confirmation')).toBeUndefined()

            validerModale()
            await flushPromises()

            expect(dernier('.confirmation')).toBeDefined()
        })

        it('envoie le rejet avec son motif', async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', 'Rejeter')
            await saisirMotif('Budget incoherent')
            validerModale()
            await flushPromises()
            await confirmer()

            expect(appelValider(0)).toEqual([1, true, 'Budget incoherent'])
        })

        it('retire la ligne rejetee', async () => {
            mockValider.mockResolvedValue(
                decisionOk({ decision: 'REJETE' as DecisionValidation })
            )

            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', 'Rejeter')
            await saisirMotif('Hors perimetre')
            validerModale()
            await flushPromises()
            await confirmer()

            expect(wrapper.findAll('.tableau__tr')).toHaveLength(2)
        })

        it("n'envoie rien si la confirmation finale est annulee", async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', 'Rejeter')
            await saisirMotif('Hors perimetre')
            validerModale()
            await flushPromises()
            annulerConfirmation()
            await flushPromises()

            expect(mockValider).not.toHaveBeenCalled()
        })

        it('repart de zero a chaque ouverture', async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', 'Rejeter')
            await saisirMotif('Premier motif')

            // L'utilisateur annule, puis rouvre sur la meme ligne.
            const annuler = Array.from(
                dernier('.rejet-activite .modal-footer')?.querySelectorAll('button') ?? []
            ).find(b => (b.textContent ?? '').includes('Annuler')) as HTMLButtonElement | undefined

            annuler?.click()
            await flushPromises()

            await actionMenu(wrapper, 'ACT-001', 'Rejeter')

            const zone = dernier('.rejet-activite textarea') as HTMLTextAreaElement

            // Un motif pre-rempli pour une activite sans rapport.
            expect(zone.value).toBe('')
        })
    })

    // ------------------------------------------------------------------
    // Retour a l'etape precedente (issue imposee RETOUR_MODIFICATION)
    // ------------------------------------------------------------------

    describe("Le retour à l'étape précédente", () => {
        it('ouvre la modale sur l\'issue "Renvoyer"', async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', "Soumettre à l'étape précédente")

            const modal = dernier('.rejet-activite')
            expect(modal).toBeDefined()
            expect(modal?.textContent).toContain("Renvoyer l'activité")
        })

        it("n'affiche pas le choix d'issue", async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', "Soumettre à l'étape précédente")

            const modal = dernier('.rejet-activite') as HTMLElement
            expect(modal.querySelector('input[type="radio"]')).toBeNull()
        })

        it("affiche le libelle du bouton aligne sur l'issue", async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', "Soumettre à l'étape précédente")

            const pied = dernier('.rejet-activite .modal-footer') as HTMLElement

            const bouton = Array.from(pied.querySelectorAll('button')).find(b =>
                (b.textContent ?? '').includes('Renvoyer')
            )

            expect(bouton).toBeDefined()
        })

        it("appelle soumettreValidationRetour, pas validerActivite", async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', "Soumettre à l'étape précédente")
            await saisirMotif('Dates a corriger')
            validerModale()
            await flushPromises()
            await confirmer()

            // Le sens de l'ecriture est inverse : un endpoint par direction
            // reste plus lisible qu'un parametre sur la meme route.
            expect(appelRetour(0)).toEqual([1, 'Dates a corriger'])
            expect(mockValider).not.toHaveBeenCalled()
        })

        it('retire la ligne renvoyee', async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', "Soumettre à l'étape précédente")
            await saisirMotif('Dates a corriger')
            validerModale()
            await flushPromises()
            await confirmer()

            expect(wrapper.findAll('.tableau__tr')).toHaveLength(2)
        })

        it("annonce le renvoi en nommant l'activite", async () => {
            const { wrapper } = await monter(TROIS())

            await actionMenu(wrapper, 'ACT-001', "Soumettre à l'étape précédente")
            await saisirMotif('Dates a corriger')
            validerModale()
            await flushPromises()
            await confirmer()

            expect(wrapper.text()).toContain("ACT-001")
            expect(wrapper.text()).toContain('renvoyée')
        })
    })

    // ------------------------------------------------------------------
    // Decisions groupees
    // ------------------------------------------------------------------

    describe('Les décisions groupées', () => {
        it('valide les lignes cochees, une requete par ligne', async () => {
            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            await caseDe(wrapper, 'ACT-002').setValue(true)
            await boutonToolbar(wrapper, 'Valider').trigger('click')
            await confirmer()

            expect(mockValider).toHaveBeenCalledTimes(2)
            expect(appelValider(0)[0]).toBe(1)
            expect(appelValider(1)[0]).toBe(2)
        })

        it('vide le tableau apres un lot entierement reussi', async () => {
            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            await boutonToolbar(wrapper, 'Tout sélectionner').trigger('click')
            await boutonToolbar(wrapper, 'Valider').trigger('click')
            await confirmer()

            expect(wrapper.findAll('.tableau__tr')).toHaveLength(0)
        })

        it("n'annule pas les decisions deja prises quand une ligne echoue", async () => {
            mockValider
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
            await boutonToolbar(wrapper, 'Valider').trigger('click')
            await confirmer()

            // Chaque requete est sa propre transaction : les reussies restent.
            expect(wrapper.findAll('.tableau__tr')).toHaveLength(1)
            expect(mockValider).toHaveBeenCalledTimes(2)
        })

        it('interrompt le lot si la connexion tombe', async () => {
            mockValider
                .mockResolvedValueOnce(decisionOk({ idActivite: 1 }))
                .mockRejectedValueOnce(new Error('network'))

            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            await caseDe(wrapper, 'ACT-002').setValue(true)
            await caseDe(wrapper, 'ACT-003').setValue(true)
            await boutonToolbar(wrapper, 'Valider').trigger('click')
            await confirmer()

            // Le reste partirait sur une connexion morte.
            expect(mockValider).toHaveBeenCalledTimes(2)
            expect(wrapper.findAll('.tableau__tr')).toHaveLength(2)
        })

        it('rejette toute la selection avec un seul motif', async () => {
            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            await caseDe(wrapper, 'ACT-002').setValue(true)
            await boutonToolbar(wrapper, 'Rejeter').trigger('click')
            await saisirMotif('Meme motif pour les deux')
            validerModale()
            await flushPromises()
            await confirmer()

            expect(appelValider(0)).toEqual([1, true, 'Meme motif pour les deux'])
            expect(appelValider(1)).toEqual([2, true, 'Meme motif pour les deux'])
        })

        it('renvoie toute la selection a l etape precedente', async () => {
            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            await caseDe(wrapper, 'ACT-002').setValue(true)
            await boutonToolbar(wrapper, "Soumettre").trigger('click')
            await saisirMotif('Corriger les dates')
            validerModale()
            await flushPromises()
            await confirmer()

            expect(appelRetour(0)).toEqual([1, 'Corriger les dates'])
            expect(appelRetour(1)).toEqual([2, 'Corriger les dates'])
        })

        it('desactive les actions groupees pendant une decision', async () => {
            const { wrapper } = await monter(TROIS())

            await caseDe(wrapper, 'ACT-001').setValue(true)
            await boutonToolbar(wrapper, 'Valider').trigger('click')

            // La ligne decidee a quitte la page et la selection avec elle :
            // la barre disparait -- il ne reste rien a decisionner.
            await confirmer()

            expect(wrapper.find('.lot').exists()).toBe(false)
        })
    })

    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------

    describe('La navigation', () => {
        it('ouvre le detail au clic sur le titre de la ligne', async () => {
            const { wrapper, router } = await monter(TROIS())

            const lien = ligne(wrapper, 'ACT-001').find('.activite__lien')
            await lien.trigger('click')
            await flushPromises()

            expect(router.currentRoute.value.name).toBe('activite-detail')
            expect(router.currentRoute.value.params.id).toBe('1')
        })
    })
})
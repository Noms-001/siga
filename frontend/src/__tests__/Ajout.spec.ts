import { describe, it, expect, vi, beforeEach } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'

import Ajout from '@/views/frontoffice/activites/Ajout.vue'
import * as activiteService from '@/services/activite'
import type { ApiResponse } from '@/services/api-client'
import type {
    ActiviteAutocomplete,
    ActiviteEcritureResultat,
    ActiviteFormulaire,
    ActiviteOptions,
    CodePropose,
    ObjectifSpecifiqueEcriture,
} from '@/types/activite'

/**
 * Tests du champ "Objectif specifique" du formulaire d'activite.
 *
 * Deux comportements sont verrouilles ici, tous deux demandes explicitement :
 *
 * 1. L'objectif choisi reste AFFICHE DANS LE CHAMP. Il etait remplace par un
 *    badge qui reprenait le texte hors du champ : l'utilisateur lisait son
 *    choix mais ne le voyait plus dans le champ, sans pouvoir le corriger ni
 *    le retirer. Le champ est donc l'unique affiche de l'etat, et la croix
 *    native suffit a le vider.
 *
 * 2. Un objectif manquant se cree sans quitter la page. Sans cela, un PTA dont
 *    l'objectif n'existe pas encore ne peut pas etre enregistre -- meme pas en
 *    brouillon -- et l'utilisateur ne peut que sortir du formulaire.
 *
 * Le service est mocke : ce qui est teste appartient au formulaire. Le contrat
 * de l'API est verifie separement, par appel reel au backend.
 */
vi.mock('@/services/activite', () => ({
    // Le formulaire s'en sert pour l'annee du code NON PTA propose : sans
    // elle dans le mock, la vue lit undefined et appelle l'API avec NaN.
    ANNEE_DEFAUT: 2027,
    chargerOptions: vi.fn<() => Promise<ApiResponse<ActiviteOptions>>>(),
    formulaireActivite: vi.fn<
        (id: number) => Promise<ApiResponse<ActiviteFormulaire>>
    >(),
    creerActivite: vi.fn<() => Promise<ApiResponse<ActiviteEcritureResultat>>>(),
    modifierActivite: vi.fn<() => Promise<ApiResponse<ActiviteEcritureResultat>>>(),
    autocompleterObjectifs: vi.fn<
        (q: string) => Promise<ApiResponse<ActiviteAutocomplete[]>>
    >(),
    creerObjectifSpecifique: vi.fn<
        (objectif: ObjectifSpecifiqueEcriture) => Promise<ApiResponse<ActiviteAutocomplete>>
    >(),
    prochainCodeNonPta: vi.fn<(annee: number) => Promise<ApiResponse<CodePropose>>>(),
}))

const mockOptions = vi.mocked(activiteService.chargerOptions)
const mockAutocomplete = vi.mocked(activiteService.autocompleterObjectifs)
const mockCreerObjectif = vi.mocked(activiteService.creerObjectifSpecifique)
const mockProchainCodeNonPta = vi.mocked(activiteService.prochainCodeNonPta)

const SUGGESTION: ActiviteAutocomplete = {
    id: 3,
    code: 'BCT/1',
    libelle: 'Maitriser le risque',
    libelleSecondaire: '2027',
    annee: 2027,
    prochainNumero: 1,
}

type VueTestee = Awaited<ReturnType<typeof monter>>

function creerRouter() {
    return createRouter({
        history: createMemoryHistory(),
        routes: [
            { path: '/', component: { template: '<div />' } },
            { path: '/activites/nouvelle', name: 'activite-creation', component: Ajout },
            { path: '/activites/:id/modifier', name: 'activite-modifier', component: Ajout },
            {
                path: '/activites',
                name: 'activites-brouillons',
                component: { template: '<div />' },
            },
            {
                path: '/activites/:id',
                name: 'activite-detail',
                component: { template: '<div />' },
            },
        ],
    })
}

async function monter() {
    const router = creerRouter()
    await router.push('/activites/nouvelle')
    await router.isReady()

    const wrapper = mount(Ajout, { global: { plugins: [router] } })
    await flushPromises()

    return wrapper
}

function champObjectif(wrapper: VueTestee) {
    return wrapper.find('input[type="search"]')
}

function suggestions(wrapper: VueTestee) {
    return wrapper.findAll('ul.autocomplete button')
}

/**
 * Saisir dans le champ, puis attendre la fin du delai de saisie (300 ms dans
 * useAutocomplete) : le champ se remplit avant que la liste ne soit demandee,
 * ce qui est le comportement attendu du composant.
 */
async function rechercherObjectif(wrapper: VueTestee, terme: string) {
    await champObjectif(wrapper).setValue(terme)
    await new Promise((resolve) => setTimeout(resolve, 400))
    await flushPromises()
}

/**
 * Cliquer la suggestion proposee.
 *
 * L'acces par indice est verifie plutot que caste : le projet compile avec
 * noUncheckedIndexedAccess, et une liste vide doit se lire comme un echec de
 * test, pas comme une erreur de type. `?` suffit donc apres le controle.
 */
async function cliquerSuggestion(wrapper: VueTestee): Promise<void> {
    const items = suggestions(wrapper)

    if (items.length !== 1) {
        throw new Error(`${items.length} suggestion(s) proposee(s), 1 attendue`)
    }

    await items[0]?.trigger('click')
    await flushPromises()
}

/**
 * Renseigner un input de la modale.
 *
 * La modale est projetee dans body par BaseModal : elle n'appartient pas au
 * wrapper, et son modele n'est donc pas accessible par les utilitaires du
 * wrapper. L'evenement input declenche par le composant est en revanche le
 * meme que celui du navigateur.
 */
function remplir(input: HTMLInputElement, valeur: string) {
    input.value = valeur
    input.dispatchEvent(new Event('input', { bubbles: true }))
}

/** Les trois champs de la modale, dans l'ordre ou ils sont declares. */
function champsModale() {
    const inputs = Array.from(document.body.querySelectorAll<HTMLInputElement>('input'))

    expect(inputs).toHaveLength(3)

    const [code, designation] = inputs.filter((i) => i.type === 'text')
    const annee = inputs.find((i) => i.type === 'number')

    return { code, designation, annee } as {
        code: HTMLInputElement
        designation: HTMLInputElement
        annee: HTMLInputElement
    }
}

/**
 * Valider la confirmation posee par la page.
 *
 * Le pied du bouton est dans BaseConfirm, teleporte dans le body comme la
 * modale de l'objectif : il se cherche donc au meme endroit.
 */
function confirmer() {
    const pied = document.body.querySelector('.confirmation .modal-footer')

    const bouton = Array.from(pied?.querySelectorAll('button') ?? []).find(
        b => !(b.textContent ?? '').includes('Annuler')
    )

    if (!bouton) {
        throw new Error('Bouton de confirmation introuvable')
    }

    bouton.click()
}

function boutonModale(texte: string) {
    return Array.from(document.body.querySelectorAll<HTMLButtonElement>('button')).find((b) =>
        (b.textContent ?? '').includes(texte)
    )
}

/** Valeur des champs du formulaire, dans l'ordre du DOM. */
function valeurs(wrapper: VueTestee): string[] {
    return wrapper
        .findAll('input')
        .map((i) => (i.element as HTMLInputElement).value)
}

/** Les codes de sous-activite affiches, dans l'ordre des lignes. */
function codesSousActivites(wrapper: VueTestee): string[] {
    return wrapper
        .findAll('.sous-activite input')
        .map((i) => (i.element as HTMLInputElement).value)
        .filter((valeur) => valeur !== '')
}

/**
 * Changer le type d'activite : les deux boutons du selecteur de type.
 *
 *Choisis par position plutot que par texte, parce que les deux boutons se
 * ressemblent -- l'un dit « PTA », l'autre « Non PTA » -- et qu'un fragment de
 * texte selectionnerait le mauvais des qu'un mot change.
 */
async function choisirType(wrapper: VueTestee, pta: boolean) {
    const boutons = wrapper.findAll('.choix-type')

    expect(boutons).toHaveLength(2)

    // Le bouton est recherche par position, puis NOMME : sous
    // noUncheckedIndexedAccess un index donne un type qui peut porter «
    // undefined », et le declic sur cette valeur possible echouerait sans
    // message. `find` avec une assertion attend un element unique et envoie
    // une erreur explicite si la position est vide.
    const bouton = boutons.find((_, index) => index === (pta ? 0 : 1))

    expect(bouton, `bouton « ${pta ? 'PTA' : 'Non PTA'} »`).toBeDefined()

    await bouton?.trigger('click')
    await flushPromises()
}

/**
 * Ajouter une ligne de sous-activite.
 *
 * Le texte est compare EXACTEMENT, et non par « contient » : la carte rend son
 * contenu avant son action, donc des qu'une ligne existe, le bouton
 * « Ajouter un livrable » de cette ligne passe devant celui de la section. Un
 * texte partiel selectionnerait ce dernier et n'ajouterait aucune ligne.
 *
 * La comparaison exacte selectionne le seul bouton dont le texte est
 * « Ajouter » : celui des resultats intermediaires est un bouton natif, et
 * celui d'un livrable en dit plus.
 */
async function ajouterUneSousActivite(wrapper: VueTestee) {
    const boutons = wrapper
        .findAllComponents({ name: 'BaseButton' })
        .filter((b) => (b.text() ?? '').trim() === 'Ajouter')

    expect(boutons).toHaveLength(1)

    await boutons[0]?.trigger('click')
    await flushPromises()
}

beforeEach(() => {
    vi.clearAllMocks()
    document.body.innerHTML = ''

    mockOptions.mockResolvedValue({
        success: true,
        data: {
            services: [],
            priorites: [],
            typesActivite: [],
            sites: [],
            statuts: [],
        },
    })
    mockAutocomplete.mockResolvedValue({ success: true, data: [SUGGESTION] })
    mockProchainCodeNonPta.mockResolvedValue({
        success: true,
        data: { code: 'A-2027-51', annee: 2027 },
    })
})

describe('Champ objectif specifique', () => {
    it("affiche l'objectif choisi dans le champ, et non a cote", async () => {
        const wrapper = await monter()

        await rechercherObjectif(wrapper, 'BCT')
        await cliquerSuggestion(wrapper)

        const champ = champObjectif(wrapper)
        const valeur = (champ.element as HTMLInputElement).value

        expect(champ.exists()).toBe(true)
        expect(valeur).toContain('BCT/1')
        expect(valeur).toContain('Maitriser le risque')

        // Le choix n'est plus repris ailleurs : le champ en est l'unique
        // affiche, et le badge qui le remplacait a disparu.
        expect(wrapper.find('.objectif-choisi').exists()).toBe(false)

        // Le code est propose d'apres l'objectif choisi.
        const code = wrapper
            .findAll('input')
            .find((i) => (i.element as HTMLInputElement).value.startsWith('A-2027-01'))
        expect(code).toBeDefined()
    })

    it('vide le champ et retire l’objectif, qui ne resterait pas orphelin', async () => {
        const wrapper = await monter()

        await rechercherObjectif(wrapper, 'BCT')
        await cliquerSuggestion(wrapper)

        await champObjectif(wrapper).setValue('')
        await flushPromises()

        // Le champ est vide et l'identifiant part avec : un PTA garde sans
        // objectif serait refuse a l'enregistrement, sans que l'utilisateur
        // puisse voir pourquoi.
        expect((champObjectif(wrapper).element as HTMLInputElement).value).toBe('')

        // Le code deja propose n'est pas efface pour autant -- c'est une valeur
        // affichee, il appartient a l'utilisateur. Ce qui compte est qu'un
        // nouvel objectif le repropose, sinon il resterait construit sur un
        // objectif qui ne l'est plus.
        await rechercherObjectif(wrapper, 'BCT')
        await cliquerSuggestion(wrapper)

        const code = wrapper
            .findAll('input')
            .find((i) => (i.element as HTMLInputElement).value.startsWith('A-2027-01'))
        expect(code).toBeDefined()
    })
})

describe('Creation d un objectif', () => {
    it('cree l’objectif depuis la modale et le rattache a l’activite', async () => {
        const cree: ActiviteAutocomplete = {
            id: 42,
            code: 'BCT/42',
            libelle: 'Objectif cree au formulaire',
            libelleSecondaire: '2028',
            annee: 2028,
            prochainNumero: 1,
        }
        mockCreerObjectif.mockResolvedValue({ success: true, data: cree })

        const wrapper = await monter()

        await wrapper.find('.objectif-champ__creer').trigger('click')
        await flushPromises()

        expect(document.body.textContent).toContain('Créer un objectif spécifique')

        const { code, designation, annee } = champsModale()
        remplir(code, '  BCT/42  ')
        remplir(designation, 'Objectif cree au formulaire')
        remplir(annee, '2028')
        await flushPromises()

        boutonModale('Créer et rattacher')?.click()
        await flushPromises()
        confirmer()
        await flushPromises()
        await flushPromises()

        // Les espaces saisis sont retires : un code enregistre avec des
        // espaces ne serait plus retrouve par une recherche, ni identique a
        // celui propose ensuite.
        expect(mockCreerObjectif).toHaveBeenCalledWith({
            code: 'BCT/42',
            designation: 'Objectif cree au formulaire',
            annee: 2028,
        })

        // L'objectif cree est retenu, comme un choix dans la liste, et le
        // code de l'activite est propose d'apres lui.
        const champ = (champObjectif(wrapper).element as HTMLInputElement).value
        expect(champ).toContain('BCT/42')
        expect(champ).toContain('2028')

        const codeActivite = wrapper
            .findAll('input')
            .find((i) => (i.element as HTMLInputElement).value.startsWith('A-2028-42-01'))
        expect(codeActivite).toBeDefined()

        // La modale se referme, l'objectif etant retenu.
        expect(document.body.textContent).not.toContain('Créer un objectif spécifique')
    })

    it('affiche le refus du serveur et garde la modale ouverte', async () => {
        mockCreerObjectif.mockResolvedValue({
            success: false,
            data: null,
            error: 'Un objectif porte déjà le code « BCT/7 »',
            status: 400,
        })

        const wrapper = await monter()

        await wrapper.find('.objectif-champ__creer').trigger('click')
        await flushPromises()

        const { code, designation } = champsModale()
        remplir(code, 'BCT/7')
        remplir(designation, 'Doublon')
        await flushPromises()

        boutonModale('Créer et rattacher')?.click()
        await flushPromises()
        confirmer()
        await flushPromises()
        await flushPromises()

        // Le message du serveur est rendu tel quel, et rien n'est selectionne.
        expect(document.body.textContent).toContain(
            'Un objectif porte déjà le code « BCT/7 »'
        )
        expect(document.body.textContent).toContain('Créer un objectif spécifique')
        expect((champObjectif(wrapper).element as HTMLInputElement).value).toBe('')
    })

    it("refuse une annee hors bornes avant d'appeler l'API", async () => {
        const wrapper = await monter()

        await wrapper.find('.objectif-champ__creer').trigger('click')
        await flushPromises()

        const { code, designation, annee } = champsModale()
        remplir(code, 'BCT/7')
        remplir(designation, 'Annee invalide')
        remplir(annee, '2200')
        await flushPromises()

        boutonModale('Créer et rattacher')?.click()
        await flushPromises()

        // La regle est annoncee dans le champ, sans appel : la colonne la
        // refuse de la meme facon, mais ici l'utilisateur corrige au lieu de
        // decouvrir un refus global apres coup.
        expect(mockCreerObjectif).not.toHaveBeenCalled()

        // Aucune confirmation n'est meme posee : une annee invalide ne peut pas
        // amener a decider d'une action que l'utilisateur n'a pas le droit de
        // tenter.
        expect(document.body.querySelector('.confirmation')).toBeNull()
        expect(document.body.textContent).toContain(
            "L'année doit être comprise entre 1900 et 2100"
        )
        expect(document.body.textContent).toContain('Créer un objectif spécifique')
    })
})

describe('Codes de sous-activite', () => {
    it("propose un code d'apres celui de l'activite, des l'ajout de la ligne", async () => {
        const wrapper = await monter()

        await rechercherObjectif(wrapper, 'BCT')
        await cliquerSuggestion(wrapper)

        await ajouterUneSousActivite(wrapper)
        await ajouterUneSousActivite(wrapper)

        // Le code de l'activite est A-2027-01-01 : les sous-activites en
        // reprennent les groupes, suivi de leur numero sur deux chiffres.
        expect(codesSousActivites(wrapper)).toEqual(['SA-2027-01-01-01', 'SA-2027-01-01-02'])
    })

    it('repropose les codes quand un nouvel objectif est choisi', async () => {
        const wrapper = await monter()

        await rechercherObjectif(wrapper, 'BCT')
        await cliquerSuggestion(wrapper)
        await ajouterUneSousActivite(wrapper)

        expect(codesSousActivites(wrapper)).toEqual(['SA-2027-01-01-01'])

        // Un second objectif, dont l'activite est A-2027-02-04.
        mockAutocomplete.mockResolvedValue({
            success: true,
            data: [
                {
                    id: 9,
                    code: 'BCT/2',
                    libelle: 'Autre objectif',
                    libelleSecondaire: '2027',
                    annee: 2027,
                    prochainNumero: 4,
                },
            ],
        })

        await rechercherObjectif(wrapper, 'BCT/2')
        await cliquerSuggestion(wrapper)

        // Sans cela, la sous-activite garderait un code construit sur
        // l'objectif precedent, et l'utilisateur ne le verrait pas passer.
        expect(codesSousActivites(wrapper)).toEqual(['SA-2027-02-04-01'])
    })

    it('ne retouche pas un code que l’utilisateur a saisi', async () => {
        const wrapper = await monter()

        await rechercherObjectif(wrapper, 'BCT')
        await cliquerSuggestion(wrapper)
        await ajouterUneSousActivite(wrapper)

        const champSousActivite = wrapper.find('.sous-activite input')
        await champSousActivite.setValue('MON-CODE')

        mockAutocomplete.mockResolvedValue({
            success: true,
            data: [
                {
                    id: 9,
                    code: 'BCT/2',
                    libelle: 'Autre objectif',
                    libelleSecondaire: '2027',
                    annee: 2027,
                    prochainNumero: 4,
                },
            ],
        })
        await rechercherObjectif(wrapper, 'BCT/2')
        await cliquerSuggestion(wrapper)

        expect(codesSousActivites(wrapper)).toEqual(['MON-CODE'])
    })

    it('propose un code en NON PTA, sans objectif', async () => {
        const wrapper = await monter()

        await choisirType(wrapper, false)

        // A-<annee>-<suite du dernier> : la regle d'une NON PTA, portee par le
        // backend, qui la renvoie prete.
        expect(mockProchainCodeNonPta).toHaveBeenCalledWith(2027)
        expect(valeurs(wrapper)).toContain('A-2027-51')
    })

    it('ne propose aucun code de sous-activite en NON PTA', async () => {
        const wrapper = await monter()

        await choisirType(wrapper, false)
        await ajouterUneSousActivite(wrapper)

        // Le code d'une NON PTA a trois groupes, donc sa sous-activite en
        // aurait trois aussi : SA-2027-51-01, qui est deja porte par cinq
        // lignes du jeu de donnees. Proposer ce code ferait refuser
        // l'enregistrement par l'utilisateur lui-meme. Le champ reste vide,
        // et le backend genere un code libre a partir du rang.
        expect(codesSousActivites(wrapper)).toEqual([])
        expect(wrapper.findAll('.sous-activite')).toHaveLength(1)
    })

    it('conserve un code saisi quand on passe en NON PTA', async () => {
        const wrapper = await monter()

        const champCode = wrapper
            .findAll('input')
            .find((i) => (i.element as HTMLInputElement).placeholder === 'A-2027-01-01')

        expect(champCode).toBeDefined()
        await champCode?.setValue('MON-CODE-A-MOI')

        await choisirType(wrapper, false)

        expect(valeurs(wrapper)).toContain('MON-CODE-A-MOI')
        expect(mockProchainCodeNonPta).not.toHaveBeenCalled()
    })
})

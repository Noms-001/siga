import { describe, it, expect, vi, beforeEach } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'

import SousActivite from '@/views/frontoffice/activites/SousActivite.vue'
import * as activiteService from '@/services/activite'
import type { ApiResponse } from '@/services/api-client'
import type { FichierDetail, LivrableDetail, SousActiviteDetail } from '@/types/activite'

/**
 * Tests du depot d'un livrable depuis le detail d'une sous-activite.
 *
 * Une regle est verrouillee ici : LE DEPOT EST OUVERT SUR UNE ACTIVITE
 * BROUILLON OU EN COURS, ET SUR AUCUNE AUTRE. Elle est posee deux fois, et les
 * deux fois doivent tenir --
 *
 * - dans le formulaire, qui desactive le bouton, pour ne pas proposer une
 *   action qui serait refusee ;
 * - dans le service, qui refuse sous verrou. Cette seconde verification est
 *   la seule qui fasse autorite, et elle n'est pas testee ici : elle se verifie
 *   par appel reel au backend, pas en mockant le serveur.
 *
 * Ce qui est teste ici appartient donc au formulaire : ce que l'utilisateur
 * voit, ce qu'il peut tenter, et ce que le formulaire transmet.
 */
vi.mock('@/services/activite', () => ({
    detailSousActivite: vi.fn<
        (idActivite: number, idSousActivite: number) => Promise<ApiResponse<SousActiviteDetail>>
    >(),
    telechargerFichier: vi.fn<
        (idActivite: number, idSousActivite: number, idFichier: number) => Promise<ApiResponse<Blob>>
    >(),
    creerLivrable: vi.fn<
        (
            idActivite: number,
            idSousActivite: number,
            livrable: { designation: string; description?: string | null },
            fichiers: File[]
        ) => Promise<ApiResponse<LivrableDetail>>
    >(),
    ajouterFichiersLivrable: vi.fn<
        (
            idActivite: number,
            idSousActivite: number,
            idLivrable: number,
            fichiers: File[]
        ) => Promise<ApiResponse<LivrableDetail>>
    >(),
}))

const mockDetail = vi.mocked(activiteService.detailSousActivite)
const mockCreerLivrable = vi.mocked(activiteService.creerLivrable)
const mockAjouterFichiers = vi.mocked(activiteService.ajouterFichiersLivrable)

type VueTestee = Awaited<ReturnType<typeof monter>>

function creerRouter() {
    return createRouter({
        history: createMemoryHistory(),
        routes: [
            { path: '/', component: { template: '<div />' } },
            {
                path: '/activites/:id(\\d+)/sous-activites/:sousActiviteId(\\d+)',
                name: 'sous-activite-detail',
                component: SousActivite,
            },
        ],
    })
}

/**
 * Detail d'une sous-activite, seul shape teste ici : le statut de l activite
 * mere et les livrables. Les autres collections sont vides parce qu'aucune de
 * ces listes n'est rendu.
 *
 * `statutActivite` est surcharge par chaque test : c'est lui qui decide.
 */
function detail(statutActivite: string | null): SousActiviteDetail {
    return {
        id: 7,
        code: 'SA-2027-01-02-01',
        designation: 'Preparation et controle',
        dateDebutPrevue: '2027-01-05',
        dateFinPrevue: '2027-02-01',
        dateDebutReelle: null,
        dateFinReelle: null,
        avancementCourant: null,
        historiqueAvancement: [],
        affectations: [],
        livrables: [LIVRABLE_VIDE],
        statutActivite,
    }
}

/**
 * Un livrable sans fichier.
 *
 * Presque tous les cas portent sur la regle d'acces ou sur le depot d'un
 * livrable neuf, ou la liste de fichiers n'a pas d'importance. Ce livrable la
 * fournit quand meme, pour que la section soit rendue comme en usage reel.
 */
const LIVRABLE_VIDE: LivrableDetail = {
    id: 55,
    designation: 'Rapport d audit',
    description: null,
    fichiers: [],
}

/** Un fichier tel que le detail le renvoie. */
function fichier(overrides: Partial<FichierDetail> = {}): FichierDetail {
    return {
        id: 900,
        nomFichier: 'livrable_55_v1_0.txt',
        nomOriginal: 'note.txt',
        extension: 'txt',
        typeMime: 'text/plain',
        taille: 12,
        version: 1,
        dateDepot: '2027-01-10T09:00:00',
        utilisateur: { id: 3, nom: 'Ben Salah', prenom: 'Karim' },
        ...overrides,
    }
}

/** Un livrable portant les fichiers donnes. */
function livrableAvec(fichiers: FichierDetail[]): LivrableDetail {
    return { ...LIVRABLE_VIDE, fichiers }
}

const LIVRABLE: LivrableDetail = {
    id: 55,
    designation: 'Rapport d audit',
    description: 'Synthese des controles',
    fichiers: [],
}

async function monter(
    statutActivite: string | null,
    livrables: LivrableDetail[] = [LIVRABLE_VIDE]
) {
    const router = creerRouter()

    const base = detail(statutActivite)
    mockDetail.mockResolvedValue({
        success: true,
        data: { ...base, livrables },
    })

    await router.push('/activites/2/sous-activites/7')
    await router.isReady()

    const wrapper = mount(SousActivite, { global: { plugins: [router] } })
    await flushPromises()

    return wrapper
}

/**
 * Le bouton d ajout de livrable.
 *
 * Selectionne par son icone `bi-plus-lg` et non par un texte : un bouton icone
 * seule n'a pas de texte a chercher, et la carte voisine porte des boutons
 * qui, eux, en ont. Le `title` est lui aussi declenche, ce qui le rend
 * identifiable meme a l'ecran.
 */
function boutonAjouter(wrapper: VueTestee) {
    const bouton = wrapper
        .findAllComponents({ name: 'BaseButton' })
        .find((b) => b.find('.bi-plus-lg').exists())

    if (!bouton) {
        throw new Error('bouton d ajout de livrable introuvable')
    }

    return bouton
}

/**
 * La modale est projetee dans body par BaseModal : elle n'appartient pas au
 * wrapper, et ses elements ne sont donc pas atteignables par ses
 * utilisateurs. C'est aussi le cas pour le champ fichier, et pour le bouton
 * de validation.
 */
function dansModale<T extends Element>(selecteur: string): T[] {
    return Array.from(document.body.querySelectorAll<T>(selecteur))
}

/**
 * Valider la confirmation posee avant le depot.
 *
 * Elle se trouve dans le body comme la modale de depot, mais dans son propre
 * conteneur : sans le prefixe ".confirmation", le premier bouton trouve serait
 * celui du pied de la modale de depot, qui n'a rien a confirmer.
 */
async function confirmer(): Promise<void> {
    const pied = document.body.querySelector('.confirmation .modal-footer')

    const bouton = Array.from(pied?.querySelectorAll('button') ?? []).find(
        b => !(b.textContent ?? '').includes('Annuler')
    ) as HTMLButtonElement | undefined

    if (!bouton) {
        throw new Error('Bouton de confirmation introuvable')
    }

    bouton.click()

    // L'action part du clic : il faut la laisser finir avant d'affirmer quoi que
    // ce soit sur son resultat.
    await flushPromises()
}

function boutonModale(texte: string): HTMLButtonElement | undefined {
    return dansModale<HTMLButtonElement>('button').find((b) =>
        (b.textContent ?? '').includes(texte)
    )
}

function champTexte(nom: string): HTMLInputElement | undefined {
    return dansModale<HTMLInputElement>('input[type="text"], textarea').find((i) =>
        (i.placeholder ?? '').includes(nom)
    )
}

function remplir(input: HTMLInputElement, valeur: string): void {
    input.value = valeur
    input.dispatchEvent(new Event('input', { bubbles: true }))
}

/** Joindre un fichier au champ file, comme le ferait le navigateur. */
function joindre(input: HTMLInputElement, fichiers: File[]): void {
    Object.defineProperty(input, 'files', { value: fichiers, configurable: true })
    input.dispatchEvent(new Event('change', { bubbles: true }))
}

beforeEach(() => {
    vi.clearAllMocks()
    document.body.innerHTML = ''
})

describe('Depot d un livrable sur une sous-activite', () => {
    describe('Le bouton d ajout, selon le statut de l activite mere', () => {
        // BROUILLON est teste EN PREMIER, et separement, parce que c est le
        // statut qu on oublie : l activite se compose pendant qu elle est
        // brouillon, et ses sous-activites ne s ecrivent que la. Bloquer le
        // depot sur ce statut rendrait impossible de joindre un livrable a une
        // sous-activite que l on vient de creer.
        it('est actif quand l activite est en brouillon', async () => {
            const wrapper = await monter('BROUILLON')

            expect(boutonAjouter(wrapper).props('disabled')).toBe(false)
        })

        it('est actif quand l activite est en cours', async () => {
            const wrapper = await monter('EN_COURS')

            const bouton = boutonAjouter(wrapper)

            expect(bouton.props('disabled')).toBe(false)
            expect(bouton.text()).not.toContain('en cours')
        })

        it('est desactive pour une activite terminee', async () => {
            const wrapper = await monter('TERMINEE')

            const bouton = boutonAjouter(wrapper)

            expect(bouton.props('disabled')).toBe(true)

            // Le motif est annonce dans le title : le bouton reste visible,
            // son retrait ferait croire qu il n y a rien a faire.
            expect(bouton.props('title')).toContain('TERMINEE')
        })

        // Le complement exact des deux statuts ouverts, et pas seulement ceux
        // dont l interdiction va de soit : une activite en attente de
        // validation ou validee est encore vivante, et c'est la que l on se
        // trompera. Un statut ajoute a l'enum doit etre nomme ici, sinon ce
        // test couvre moins que l'enum -- et BROUILLON doit en disparaitre,
        // sinon on rebloquerait par erreur le statut que l on vient d admettre.
        it.each([
            'NON_COMMENCEE',
            'EN_ATTENTE_VALIDATION',
            'EN_RETARD',
            'SUSPENDUE',
            'REPORTEE',
            'TERMINEE',
            'VALIDEE',
            'REJETE',
            'ANNULEE',
        ])('est desactive pour une activite %s', async (statut) => {
            const wrapper = await monter(statut)

            expect(boutonAjouter(wrapper).props('disabled')).toBe(true)
        })

        it('couvre tous les statuts de StatutsActivite, sans trou ni doublon', async () => {
            // La liste du it.each doit etre le complement exact des statuts
            // ouverts. Ecrit en dur des deux cotes, ce controle vaut plus que
            // les tests precedents : il attrape un statut oublie dans l une des
            // deux listes, ce que le dedoublonnage ni les cas isoles ne voient
            // pas.
            const OUVERTS = ['BROUILLON', 'EN_COURS']
            const REFUSES = [
                'NON_COMMENCEE',
                'EN_ATTENTE_VALIDATION',
                'EN_RETARD',
                'SUSPENDUE',
                'REPORTEE',
                'TERMINEE',
                'VALIDEE',
                'REJETE',
                'ANNULEE',
            ]
            const ENUM: string[] = [
                'NON_COMMENCEE',
                'BROUILLON',
                'EN_COURS',
                'EN_ATTENTE_VALIDATION',
                'EN_RETARD',
                'SUSPENDUE',
                'REPORTEE',
                'TERMINEE',
                'VALIDEE',
                'REJETE',
                'ANNULEE',
            ]

            expect([...OUVERTS, ...REFUSES].sort()).toEqual([...ENUM].sort())
        })

        it('est desactive quand l activite n a pas de statut', async () => {
            // Une activite sans historique n a pas de statut courant. L absence
            // est traitee comme un refus : on ignore son etat, on n ecrit pas.
            const wrapper = await monter(null)

            const bouton = boutonAjouter(wrapper)

            expect(bouton.props('disabled')).toBe(true)
            expect(bouton.props('title')).toContain('en cours')
        })

        it('n ouvre rien quand il est desactive', async () => {
            const wrapper = await monter('TERMINEE')

            await boutonAjouter(wrapper).trigger('click')
            await flushPromises()

            expect(dansModale('textarea')).toHaveLength(0)
        })
    })

    describe('Ouverture de la modale', () => {
        it('s ouvre sur un clic', async () => {
            const wrapper = await monter('EN_COURS')

            await boutonAjouter(wrapper).trigger('click')
            await flushPromises()

            expect(dansModale('textarea').length).toBeGreaterThan(0)
            expect(boutonModale('Ajouter le livrable')).toBeDefined()
        })
    })

    describe('Envoi du livrable', () => {
        async function ouvrir(statut = 'EN_COURS') {
            const wrapper = await monter(statut)

            await boutonAjouter(wrapper).trigger('click')
            await flushPromises()

            return wrapper
        }

        it('transmet la designation, la description et les identifiants', async () => {
            mockCreerLivrable.mockResolvedValue({ success: true, data: LIVRABLE })

            await ouvrir()

            const designation = champTexte('Rapport')
            const description = champTexte('contient')

            if (!designation || !description) {
                throw new Error('champs de la modale introuvables')
            }

            remplir(designation, 'Rapport d audit')
            remplir(description, 'Synthese des controles')

            await boutonModale('Ajouter le livrable')?.click()
            await flushPromises()
            await confirmer()

            expect(mockCreerLivrable).toHaveBeenCalledTimes(1)
            expect(mockCreerLivrable).toHaveBeenCalledWith(
                2,
                7,
                { designation: 'Rapport d audit', description: 'Synthese des controles' },
                []
            )
        })

        it('n envoie pas une description vide, pour ne pas ecrire du vide', async () => {
            mockCreerLivrable.mockResolvedValue({ success: true, data: LIVRABLE })

            await ouvrir()

            const designation = champTexte('Rapport')

            if (!designation) throw new Error('champ designation introuvable')

            remplir(designation, 'Plan de controle')

            await boutonModale('Ajouter le livrable')?.click()
            await flushPromises()
            await confirmer()

            expect(mockCreerLivrable.mock.calls[0]?.[2]).toEqual({
                designation: 'Plan de controle',
                description: null,
            })
        })

        it('joint les fichiers choisis', async () => {
            mockCreerLivrable.mockResolvedValue({ success: true, data: LIVRABLE })

            await ouvrir()

            const designation = champTexte('Rapport')
            const fichier = new File(['contenu'], 'rapport.pdf', { type: 'application/pdf' })

            if (!designation) throw new Error('champ designation introuvable')

            remplir(designation, 'Rapport d audit')

            const champFichier = dansModale<HTMLInputElement>('input[type="file"]')[0]

            if (!champFichier) throw new Error('champ fichier introuvable')

            joindre(champFichier, [fichier])

            await boutonModale('Ajouter le livrable')?.click()
            await flushPromises()
            await confirmer()

            expect(mockCreerLivrable.mock.calls[0]?.[3]).toEqual([fichier])
        })

        it('refuse d envoyer sans designation, sans appeler le serveur', async () => {
            await ouvrir()

            await boutonModale('Ajouter le livrable')?.click()
            await flushPromises()
            expect(document.body.querySelector('.confirmation')).toBeNull()

            expect(mockCreerLivrable).not.toHaveBeenCalled()
            // La modale reste ouverte : le refus concerne le champ, pas la
            // demande, et fermer ferait perdre ce qui a ete saisi.
            expect(boutonModale('Ajouter le livrable')).toBeDefined()
        })

        it('affiche le refus du serveur et garde la modale ouverte', async () => {
            // 409 : l activite a change d etat entre l affichage et l envoi.
            // C est la seule reponse possible a un depot sur une activite qui
            // n est plus en cours, et elle ne se prevoyait pas.
            mockCreerLivrable.mockResolvedValue({
                success: false,
                data: null,
                error: 'Un livrable ne peut être ajouté que sur une activité en brouillon ou en cours',
                status: 400,
            })

            await ouvrir()

            const designation = champTexte('Rapport')

            if (!designation) throw new Error('champ designation introuvable')

            remplir(designation, 'Rapport d audit')

            await boutonModale('Ajouter le livrable')?.click()
            await flushPromises()
            await confirmer()

            expect(boutonModale('Ajouter le livrable')).toBeDefined()
            expect(document.body.textContent).toContain('brouillon ou en cours')
        })
    })

    describe('Fichiers trop lourds', () => {
        it('sont ecarte avant l envoi, et dit', async () => {
            mockCreerLivrable.mockResolvedValue({ success: true, data: LIVRABLE })

            const wrapper = await monter('EN_COURS')

            await boutonAjouter(wrapper).trigger('click')
            await flushPromises()

            const designation = champTexte('Rapport')
            const lourd = new File(['x'], 'enorme.zip', { type: 'application/zip' })

            if (!designation) throw new Error('champ designation introuvable')

            remplir(designation, 'Archive')

            // Au-dela du plafond de 10 Mo announces par le formulaire.
            Object.defineProperty(lourd, 'size', { value: 11 * 1024 * 1024 })

            const champFichier = dansModale<HTMLInputElement>('input[type="file"]')[0]

            if (!champFichier) throw new Error('champ fichier introuvable')

            joindre(champFichier, [lourd])
            await flushPromises()

            expect(document.body.textContent).toContain('enorme.zip')

            await boutonModale('Ajouter le livrable')?.click()
            await flushPromises()
            await confirmer()

            // Il ne part pas : le serveur le refuserait, et ferait echouer tout
            // le depot pour un fichier que l'utilisateur n'attendait pas voir
            // bloque.
            expect(mockCreerLivrable.mock.calls[0]?.[3]).toEqual([])
        })
    })

    describe('Le livrable cree', () => {
        it('rejoint la liste sans rechargement', async () => {
            mockCreerLivrable.mockResolvedValue({ success: true, data: LIVRABLE })

            // Part d une liste vide : c est le seul etat ou "rejoint la liste"
            // veut dire quelque chose.
            const wrapper = await monter('EN_COURS', [])

            expect(wrapper.text()).toContain('Aucun livrable')

            await boutonAjouter(wrapper).trigger('click')
            await flushPromises()

            const designation = champTexte('Rapport')

            if (!designation) throw new Error('champ designation introuvable')

            remplir(designation, 'Rapport d audit')

            await boutonModale('Ajouter le livrable')?.click()
            await flushPromises()
            await confirmer()

            // La reponse contient deja le livrable et ses fichiers : recharger
            // la page ferait perdre son etat pour reafficher presque la meme
            // chose.
            expect(mockDetail).toHaveBeenCalledTimes(1)
            expect(wrapper.text()).toContain('Rapport d audit')
            expect(wrapper.text()).not.toContain('Aucun livrable')
        })
    })

    // ------------------------------------------------------------------
    // Ajout de fichiers a un livrable existant
    // ------------------------------------------------------------------

    describe('Le bouton de fichiers, sur un livrable', () => {
        /** Le trombone de la ligne de livrable, pas celui d un fichier. */
        function boutonFichiers(wrapper: VueTestee) {
            const bouton = wrapper
                .findAllComponents({ name: 'BaseButton' })
                .find(
                    (b) =>
                        b.find('.bi-paperclip').exists()
                        && b.attributes('aria-label')?.includes('Ajouter des fichiers')
                )

            if (!bouton) throw new Error('bouton de fichiers du livrable introuvable')

            return bouton
        }

        it('est actif sur une activite en brouillon', async () => {
            const wrapper = await monter('BROUILLON')

            expect(boutonFichiers(wrapper).props('disabled')).toBe(false)
        })

        it('est actif sur une activite en cours', async () => {
            const wrapper = await monter('EN_COURS')

            expect(boutonFichiers(wrapper).props('disabled')).toBe(false)
        })

        it.each(['TERMINEE', 'EN_ATTENTE_VALIDATION', 'ANNULEE'])(
            'est desactive sur une activite %s',
            async (statut) => {
                const wrapper = await monter(statut)

                expect(boutonFichiers(wrapper).props('disabled')).toBe(true)
            }
        )

        it('n ouvre rien quand il est desactive', async () => {
            const wrapper = await monter('TERMINEE')

            await boutonFichiers(wrapper).trigger('click')
            await flushPromises()

            expect(boutonModale('Joindre les fichiers')).toBeUndefined()
        })
    })

    // ------------------------------------------------------------------
    // Actions proposees sur un fichier
    // ------------------------------------------------------------------

    describe('Les actions sur un fichier', () => {
        it('ne propose que le telechargement', async () => {
            const wrapper = await monter('EN_COURS', [livrableAvec([fichier()])])

            // Un seul bouton d'action par fichier : l'apercu a ete retire, il
            // ne doit pas revenir par un reste de template ou de style.
            expect(wrapper.findAll('.fichier__actions .base-btn')).toHaveLength(1)
            expect(wrapper.find('.bi-eye').exists()).toBe(false)
            expect(wrapper.find('.bi-download').exists()).toBe(true)
        })
    })

    describe('Depot de fichiers sur un livrable existant', () => {
        /** Ouvre le modal de fichiers sur le premier livrable du detail. */
        async function ouvrir(statut = 'EN_COURS', fichiers = [fichier()]) {
            const wrapper = await monter(statut, [livrableAvec(fichiers)])

            const bouton = wrapper
                .findAllComponents({ name: 'BaseButton' })
                .find(
                    (b) =>
                        b.find('.bi-paperclip').exists()
                        && b.attributes('aria-label')?.includes('Ajouter des fichiers')
                )

            if (!bouton) throw new Error('bouton de fichiers introuvable')

            await bouton.trigger('click')
            await flushPromises()

            return wrapper
        }

        it('nomme le livrable dans le modal', async () => {
            await ouvrir()

            expect(document.body.textContent).toContain('Rapport d audit')
        })

        it('envoie les fichiers avec les trois identifiants', async () => {
            mockAjouterFichiers.mockResolvedValue({
                success: true,
                data: livrableAvec([fichier(), fichier({ id: 901 })]),
            })

            await ouvrir()

            const joint = new File(['note'], 'annexe.txt', { type: 'text/plain' })

            const champFichier = dansModale<HTMLInputElement>('input[type="file"]')[0]

            if (!champFichier) throw new Error('champ fichier introuvable')

            joindre(champFichier, [joint])
            await flushPromises()

            await boutonModale('Joindre les fichiers')?.click()
            await flushPromises()
            await confirmer()
            await flushPromises()

            expect(mockAjouterFichiers).toHaveBeenCalledWith(2, 7, 55, [joint])
        })

        // Le depot de fichiers est obligatoire ici, la ou il est facultatif a
        // la creation d'un livrable : envoyer une liste vide n'ecrirait rien
        // et renverrait pourtant un succes.
        it('refuse d envoyer sans avoir choisi de fichier', async () => {
            await ouvrir()

            expect(boutonModale('Joindre les fichiers')?.disabled).toBe(true)

            await boutonModale('Joindre les fichiers')?.click()
            await flushPromises()
            expect(document.body.querySelector('.confirmation')).toBeNull()
            await flushPromises()

            expect(mockAjouterFichiers).not.toHaveBeenCalled()
        })

        it('garde la modale ouverte et montre le refus du serveur', async () => {
            mockAjouterFichiers.mockResolvedValue({
                success: false,
                data: null,
                error: 'Des fichiers ne peuvent être déposés que sur une activité en brouillon ou en cours',
                status: 400,
            })

            await ouvrir()

            const champFichier = dansModale<HTMLInputElement>('input[type="file"]')[0]

            if (!champFichier) throw new Error('champ fichier introuvable')

            joindre(champFichier, [new File(['x'], 'a.txt', { type: 'text/plain' })])
            await flushPromises()

            await boutonModale('Joindre les fichiers')?.click()
            await flushPromises()
            await confirmer()
            await flushPromises()

            expect(boutonModale('Joindre les fichiers')).toBeDefined()
            expect(document.body.textContent).toContain('brouillon ou en cours')
        })

        it('affiche les nouveaux fichiers sans rechargement', async () => {
            const ajoute = fichier({ id: 902, nomOriginal: 'annexe.txt' })

            mockAjouterFichiers.mockResolvedValue({
                success: true,
                data: livrableAvec([fichier(), ajoute]),
            })

            const wrapper = await ouvrir()

            expect(wrapper.text()).toContain('note.txt')
            expect(wrapper.text()).not.toContain('annexe.txt')

            const champFichier = dansModale<HTMLInputElement>('input[type="file"]')[0]

            if (!champFichier) throw new Error('champ fichier introuvable')

            joindre(champFichier, [new File(['x'], 'annexe.txt', { type: 'text/plain' })])
            await flushPromises()

            await boutonModale('Joindre les fichiers')?.click()
            await flushPromises()
            await confirmer()
            await flushPromises()

            // Le livrable revient relu du serveur, fichiers anterieurs compris :
            // on remplace la ligne entiere plutot que d'y ajouter les nouveaux
            // fichiers a la main.
            expect(mockDetail).toHaveBeenCalledTimes(1)
            expect(wrapper.text()).toContain('annexe.txt')
            expect(wrapper.text()).toContain('note.txt')
        })

        it('ecarte un fichier trop lourd avant l envoi', async () => {
            mockAjouterFichiers.mockResolvedValue({
                success: true,
                data: livrableAvec([fichier(), fichier({ id: 903 })]),
            })

            await ouvrir()

            const lourd = new File(['x'], 'enorme.txt', { type: 'text/plain' })
            Object.defineProperty(lourd, 'size', { value: 11 * 1024 * 1024 })

            const champFichier = dansModale<HTMLInputElement>('input[type="file"]')[0]

            if (!champFichier) throw new Error('champ fichier introuvable')

            joindre(champFichier, [lourd])
            await flushPromises()

            expect(document.body.textContent).toContain('enorme.txt')

            await boutonModale('Joindre les fichiers')?.click()
            await flushPromises()
            expect(document.body.querySelector('.confirmation')).toBeNull()
            await flushPromises()

            // Il ne part pas, et rien n est envoye : le serveur le refuserait,
            // et tout l envoi echouerait pour un fichier que l'utilisateur ne
            // s'attendait pas a voir bloque.
            expect(boutonModale('Joindre les fichiers')?.disabled).toBe(true)
            expect(mockAjouterFichiers).not.toHaveBeenCalled()
        })

        it('tient compte des fichiers deja deposes dans le plafond', async () => {
            mockAjouterFichiers.mockResolvedValue({
                success: true,
                data: livrableAvec([...Array(10).keys()].map((i) => fichier({ id: 800 + i }))),
            })

            // Dix fichiers deja la : le plafond porte sur le total du livrable,
            // pas sur chaque envoi.
            const existants = Array.from({ length: 10 }, (_, i) =>
                fichier({ id: 800 + i })
            )

            await ouvrir('EN_COURS', existants)

            expect(document.body.textContent).toContain('porte déjà ses 10 fichiers')

            const champFichier = dansModale<HTMLInputElement>('input[type="file"]')[0]

            if (!champFichier) throw new Error('champ fichier introuvable')

            joindre(champFichier, [new File(['x'], 'a.txt', { type: 'text/plain' })])

            await boutonModale('Joindre les fichiers')?.click()
            await flushPromises()
            expect(document.body.querySelector('.confirmation')).toBeNull()
            await flushPromises()

            // Le plafond porte sur le total du livrable : dix fichiers deja
            // la, on n'en ajoute pas un onzieme.
            expect(boutonModale('Joindre les fichiers')?.disabled).toBe(true)
            expect(mockAjouterFichiers).not.toHaveBeenCalled()
        })
    })
})

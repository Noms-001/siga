import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'

import Profile from '@/views/frontoffice/Profile.vue'
import * as authService from '@/services/auth'
import { useAuthStore } from '@/stores/auth'
import { enableAutoUnmount } from '@vue/test-utils'

/*
    Chaque composant monte est demonte apres son test : BaseConfirm se teleporte
    dans le body, et une modale laissee ouverte par un test parasiterait le
    suivant.
*/
enableAutoUnmount(afterEach)

vi.mock('@/services/auth', () => ({
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
    changePassword: vi.fn(),
}))

const mockGetProfile = vi.mocked(authService.getProfile)
const mockUpdateProfile = vi.mocked(authService.updateProfile)
const mockChangePassword = vi.mocked(authService.changePassword)

const PROFIL = {
    idUtilisateur: 1,
    nom: 'Rakoto',
    prenom: 'Naina',
    email: 'naina@example.mg',
    telephone: '034 00 000 00',
    role: 'ADMIN' as const,
    service: null,
    postes: [],
    actif: true,
    dateCreation: '2026-01-01T08:00:00',
    dateDerniereConnexion: null,
    dateDesactivation: null,
}

/**
 * Le dernier element du body : la modale ouverte est la plus recente, et c'est
 * elle que le test vient de poser.
 */
function dernier(selecteur: string): Element | undefined {
    const trouves = document.body.querySelectorAll(selecteur)

    return trouves.length ? trouves[trouves.length - 1] : undefined
}

function piedConfirmation(): Element | undefined {
    return dernier('.confirmation .modal-footer')
}

function boutonConfirmation(texte: string): HTMLButtonElement {
    const pied = piedConfirmation()

    const bouton = Array.from(pied?.querySelectorAll('button') ?? []).find(b =>
        (b.textContent ?? '').includes(texte)
    ) as HTMLButtonElement | undefined

    if (!bouton) {
        throw new Error(`Bouton "${texte}" introuvable dans la confirmation`)
    }

    return bouton
}

function confirmer(): void {
    boutonConfirmation('Enregistrer').click()
}

/*
    Les champs sont designes par BaseInput, qui ne pose pas d'attribut "name" :
    ils se reperent donc par leur rang dans le formulaire, celui du profil
    d'abord puis celui du mot de passe. BaseInput garde l'ordre du template.
*/
const CHAMPS_PROFIL = { nom: 0, prenom: 1, email: 2, telephone: 3 }

/*
    Les deux sections ne sont pas ouvertes ensemble dans ces tests : le rang d'un
    champ est donc relatif a la seule section ouverte, et repartirait de zero si
    l'autre l'etait aussi.
*/
const CHAMPS_MOT_DE_PASSE = { ancien: 0, nouveau: 1, confirmation: 2 }

function remplirChamp(rang: number, valeur: string): void {
    const input = wrapper.findAll('input')[rang]

    if (!input || input.element === undefined) {
        throw new Error(
            `champ de rang ${rang} introuvable parmi ${wrapper.findAll('input').length}`
        )
    }

    input.element.value = valeur
    input.element.dispatchEvent(new Event('input'))
}

let wrapper: ReturnType<typeof mount>

async function monter(): Promise<void> {
    mockGetProfile.mockResolvedValue({
        success: true,
        data: PROFIL,
        error: null,
    })

    const pinia = createPinia()
    setActivePinia(pinia)

    const store = useAuthStore()

    store.user = PROFIL

    wrapper = mount(Profile, { global: { plugins: [pinia] } }) as never

    await flushPromises()
}

/** Ouvrir la zone d edition du profil, puis remplir un champ. */
async function editerProfil(): Promise<void> {
    await wrapper.findAll('button').find(b => (b.text() ?? '').includes('Modifier mes informations'))?.trigger('click')
    await flushPromises()
}

describe('Profile - confirmation avant modification', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        document.body.innerHTML = ''
        localStorage.clear()
    })

    it('n appelle pas le service tant que la confirmation n est pas validee', async () => {
        await monter()
        await editerProfil()

        remplirChamp(CHAMPS_PROFIL.prenom, 'Nainaina')

        await wrapper.findAll('button').find(b => (b.text() ?? '').includes('Enregistrer'))?.trigger('click')
        await flushPromises()

        expect(mockUpdateProfile).not.toHaveBeenCalled()
        expect(piedConfirmation()).toBeDefined()
    })

    it('enregistre une fois la confirmation validee', async () => {
        mockUpdateProfile.mockResolvedValue({
            success: true,
            data: {
                profil: { ...PROFIL, prenom: 'Nainaina' },
                jetonRenouvele: false,
                accessToken: null,
                refreshToken: null,
            },
            error: null,
        })

        await monter()
        await editerProfil()

        remplirChamp(CHAMPS_PROFIL.prenom, 'Nainaina')

        await wrapper.findAll('button').find(b => (b.text() ?? '').includes('Enregistrer'))?.trigger('click')
        await flushPromises()
        confirmer()
        await flushPromises()

        expect(mockUpdateProfile).toHaveBeenCalledTimes(1)
    })

    it("n'appelle pas le service si la confirmation est annulee", async () => {
        await monter()
        await editerProfil()

        remplirChamp(CHAMPS_PROFIL.prenom, 'Nainaina')

        await wrapper.findAll('button').find(b => (b.text() ?? '').includes('Enregistrer'))?.trigger('click')
        await flushPromises()
        boutonConfirmation('Annuler').click()
        await flushPromises()

        expect(mockUpdateProfile).not.toHaveBeenCalled()
        expect(piedConfirmation()).toBeUndefined()
    })

    it("n'annonce une confirmation email que si l'email change", async () => {
        await monter()
        await editerProfil()

        remplirChamp(CHAMPS_PROFIL.prenom, 'Nainaina')

        await wrapper.findAll('button').find(b => (b.text() ?? '').includes('Enregistrer'))?.trigger('click')
        await flushPromises()

        // Sans changement d'email, il n'y a rien d'irrversible : la consequence
        // sur les sessions serait fausse.
        expect(document.body.textContent).not.toContain('sessions')

        boutonConfirmation('Annuler').click()
        await flushPromises()

        remplirChamp(CHAMPS_PROFIL.email, 'autre@example.mg')

        await wrapper.findAll('button').find(b => (b.text() ?? '').includes('Enregistrer'))?.trigger('click')
        await flushPromises()

        // Changer d'email deconnecte les autres sessions : c'est annonce avant
        // le clic, pas decouvert apres une session perdue.
        expect(document.body.textContent).toContain('sessions')
    })

    it('n appelle pas le service si le formulaire est invalide', async () => {
        await monter()
        await editerProfil()

        remplirChamp(CHAMPS_PROFIL.prenom, '')

        await wrapper.findAll('button').find(b => (b.text() ?? '').includes('Enregistrer'))?.trigger('click')
        await flushPromises()

        // Demander "voulez-vous enregistrer ?" a un formulaire invalide
        // obligerait a corriger une erreur pour une action non decidee.
        expect(mockUpdateProfile).not.toHaveBeenCalled()
        expect(piedConfirmation()).toBeUndefined()
    })

    it('confirme aussi le changement de mot de passe', async () => {
        mockChangePassword.mockResolvedValue({
            success: true,
            data: null,
            error: null,
        })

        await monter()

        await wrapper.findAll('button').find(b => (b.text() ?? '').includes('Modifier mon mot de passe'))?.trigger('click')
        await flushPromises()

        remplirChamp(CHAMPS_MOT_DE_PASSE.ancien, 'Ancien1!')
        remplirChamp(CHAMPS_MOT_DE_PASSE.nouveau, 'Nouveau1!')
        remplirChamp(CHAMPS_MOT_DE_PASSE.confirmation, 'Nouveau1!')

        // Le libelle exact, et non une recherche parciale : "Modifier mes
        // informations" contient "Modifier" et aurait ouvert l'autre section.
        await wrapper.findAll('button').find(b => (b.text() ?? '').trim() === 'Modifier')?.trigger('click')
        await flushPromises()

        expect(mockChangePassword).not.toHaveBeenCalled()
        expect(piedConfirmation()).toBeDefined()

        boutonConfirmation('Changer').click()
        await flushPromises()

        expect(mockChangePassword).toHaveBeenCalledTimes(1)
    })
})

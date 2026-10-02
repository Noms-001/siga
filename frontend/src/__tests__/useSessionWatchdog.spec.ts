import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { defineComponent, h } from 'vue'

import {
    INACTIVITY_TIMEOUT_MS,
    useSessionWatchdog
} from '@/composables/useSessionWatchdog'
import { UNAUTHORIZED_EVENT } from '@/services/api-client'
import { useAuthStore } from '@/stores/auth'
import type { ProfileResponse } from '@/types/auth'

const TIMEOUT = 10_000

const profil = (postes: string[] = ['Utilisateur']): ProfileResponse => ({
    idUtilisateur: 1,
    nom: 'Gharbi',
    prenom: 'Sonia',
    email: 'sonia.gharbi@bfm.tn',
    telephone: null,
    service: 'Gestion des Risques et Procédures',
    postes,
    actif: true,
    dateCreation: '2021-01-10 08:00:00',
    dateDerniereConnexion: '2027-03-13 08:45:00',
    dateDesactivation: null,
})

/**
 * Montage du composable dans un composant jetable, avec un vrai router et une
 * vraie instance Pinia : le composable lit le store et pousse une route, les
 * mocker ici ne testerait que le mock.
 */
const monter = () => {

    const pinia = createPinia()
    setActivePinia(pinia)

    const router = createRouter({
        history: createMemoryHistory(),
        routes: [
            { path: '/', name: 'accueil', component: { template: '<div />' } },
            { path: '/login', name: 'login', component: { template: '<div />' } },
            {
                path: '/backoffice/login',
                name: 'backoffice-login',
                component: { template: '<div />' },
            },
        ],
    })

    const composant = defineComponent({
        setup() {
            useSessionWatchdog(TIMEOUT)
            return () => h('div')
        },
    })

    const wrapper = mount(composant, {
        global: { plugins: [pinia, router] },
    })

    return { wrapper, router, store: useAuthStore() }
}

const ouvrirSession = (store: ReturnType<typeof useAuthStore>, postes?: string[]) => {
    localStorage.setItem('accessToken', 'jeton')
    localStorage.setItem('refreshToken', 'refresh')
    store.setUser(profil(postes))
}

describe('useSessionWatchdog', () => {

    beforeEach(() => {
        vi.useFakeTimers()
        localStorage.clear()
    })

    afterEach(() => {
        vi.useRealTimers()
        localStorage.clear()
    })

    it('expire la session et redirige vers le login après le délai', async () => {

        const { router, store } = monter()
        ouvrirSession(store)

        await vi.advanceTimersByTimeAsync(TIMEOUT)
        await flushPromises()

        expect(store.isAuthenticated).toBe(false)
        expect(localStorage.getItem('accessToken')).toBeNull()
        expect(localStorage.getItem('refreshToken')).toBeNull()
        expect(router.currentRoute.value.name).toBe('login')
        expect(router.currentRoute.value.query.session).toBe('expiree')
    })

    it('repousse la déconnexion à chaque activité', async () => {

        const { router, store } = monter()
        ouvrirSession(store)

        // Presque le délai écoulé, puis une interaction.
        await vi.advanceTimersByTimeAsync(TIMEOUT - 1000)
        window.dispatchEvent(new Event('mousemove'))

        // Le compte à rebours repart de zéro : rien ne se passe encore.
        await vi.advanceTimersByTimeAsync(TIMEOUT - 1000)
        await flushPromises()

        expect(store.isAuthenticated).toBe(true)
        expect(router.currentRoute.value.name).toBe('accueil')

        // Cette fois le délai complet passe sans activité.
        await vi.advanceTimersByTimeAsync(TIMEOUT)
        await flushPromises()

        expect(store.isAuthenticated).toBe(false)
        expect(router.currentRoute.value.name).toBe('login')
    })

    it('redirige vers le login du backoffice pour un administrateur', async () => {

        const { router, store } = monter()
        ouvrirSession(store, ['Administrateur'])

        await vi.advanceTimersByTimeAsync(TIMEOUT)
        await flushPromises()

        expect(router.currentRoute.value.name).toBe('backoffice-login')
    })

    it('ferme la session sur un 401, sans attendre le délai', async () => {

        const { router, store } = monter()
        ouvrirSession(store)

        window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
        await flushPromises()

        expect(store.isAuthenticated).toBe(false)
        expect(localStorage.getItem('accessToken')).toBeNull()
        expect(router.currentRoute.value.name).toBe('login')
    })

    it('ignore le 401 quand aucune session n’est ouverte', async () => {

        /*
         * Un échec de connexion répond 401 : sans cette garde, la page de login
         * se redirigerait vers elle-même en boucle.
         */
        const { router } = monter()

        window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
        await flushPromises()

        expect(router.currentRoute.value.name).toBe('accueil')
    })

    it('ne compte pas pour une session fermée manuellement', async () => {

        const { router, store } = monter()
        ouvrirSession(store)

        store.clearUser()

        await vi.advanceTimersByTimeAsync(TIMEOUT * 2)
        await flushPromises()

        expect(router.currentRoute.value.name).toBe('accueil')
    })

    it('aligne le délai par défaut sur l’expiration du token', () => {

        // jwt.access-token-expiration = 900000 ms côté backend.
        expect(INACTIVITY_TIMEOUT_MS).toBe(15 * 60 * 1000)
    })
})

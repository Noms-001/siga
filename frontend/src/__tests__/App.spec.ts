import { describe, it, expect } from 'vitest'

import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'

import App from '../App.vue'

/**
 * App.vue monte le watchdog de session, qui utilise le store (Pinia) et le
 * router : les deux plugins doivent être fournis, sinon le montage echoue sur
 * `getActivePinia()`.
 */
const monterApp = async () => {

    const router = createRouter({
        history: createMemoryHistory(),
        routes: [
            { path: '/', name: 'accueil', component: { template: '<div>accueil</div>' } },
        ],
    })

    router.push('/')
    await router.isReady()

    const wrapper = mount(App, {
        global: { plugins: [createPinia(), router] },
    })

    await flushPromises()

    return wrapper
}

describe('App', () => {

    it('rend la vue de la route courante', async () => {

        const wrapper = await monterApp()

        expect(wrapper.text()).toContain('accueil')
    })
})

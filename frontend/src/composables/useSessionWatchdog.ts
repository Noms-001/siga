import { onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'

import { UNAUTHORIZED_EVENT } from '@/services/api-client'
import { useAuthStore } from '@/stores/auth'

/**
 * Événements considérés comme une activité de l'utilisateur.
 *
 * `mousemove` est le plus bavard (il se déclenche en continu pendant un
 * simple déplacement de souris) : c'est lui qui fait que le compte à rebours
 * repart en permanence sur une page où l'on bouge la souris sans cliquer.
 * `scroll` n'est pas dans la liste par défaut des navigateurs sans
 * `passive`, on l'écoute donc explicitement.
 */
const ACTIVITY_EVENTS = [
    'mousemove',
    'mousedown',
    'keydown',
    'wheel',
    'scroll',
    'touchstart'
] as const

/**
 * Délai d'inactivité par défaut : 15 minutes.
 *
 * Aligné sur `jwt.access-token-expiration` (900000 ms) côté backend. Passé ce
 * délai sans action, le token est expiré : le serveur renverrait 401 à la
 * prochaine requête. Fermer la session ici évite de laisser l'utilisateur
 * devant une page morte jusqu'au rechargement.
 */
export const INACTIVITY_TIMEOUT_MS = 15 * 60 * 1000

/**
 * Surveille l'inactivité de l'utilisateur et ferme la session quand elle
 * dépasse le délai, avec redirection immédiate vers la page de connexion.
 *
 * Deux déclencheurs, tous deux nécessaires :
 *
 * - le timer local, qui déconnecte même sans requête (l'utilisateur lit une
 *   page au calme, aucune requête ne part, aucun 401 ne peut survenir) ;
 * - l'événement 401 de api-client, qui rattrape le cas où le token est déjà
 *   expiré alors qu'une requête part (onglet resté ouvert, retour sur
 *   l'onglet plus tard).
 */
export const useSessionWatchdog = (
    timeoutMs: number = INACTIVITY_TIMEOUT_MS
) => {

    const authStore = useAuthStore()
    const router = useRouter()

    let timer: ReturnType<typeof setTimeout> | null = null

    /**
     * Annule le compte à rebours en cours.
     */
    const disarm = () => {

        if (timer !== null) {
            clearTimeout(timer)
            timer = null
        }
    }

    /**
     * Ferme la session et renvoie vers la page de connexion.
     *
     * La destination est choisie avant le nettoyage du store : `isBackoffice`
     * s'appuie sur `user`, vidé par `logoutLocal`.
     *
     * Le 401 est ignoré si aucune session n'est ouverte : un échec de
     * connexion répond justement 401, et rediriger à ce moment-là créerait une
     * boucle sur la page de login.
     */
    const disconnect = async () => {

        disarm()

        if (!authStore.isAuthenticated) {
            return
        }

        const destination = authStore.isBackoffice
            ? { name: 'backoffice-login' }
            : { name: 'login', query: { session: 'expiree' } }

        authStore.logoutLocal()

        try {
            await router.push(destination)
        } catch {
            /*
             * Une redirection déjà en cours rejette la promesse. La session
             * est fermée dans tous les cas, l'utilisateur finira sur la page
             * de connexion.
             */
        }
    }

    /**
     * (Re)lance le compte à rebours d'inactivité.
     *
     * Un seul timer à la fois : chaque activité repart de zéro au lieu
     * d'empiler des minuteries.
     */
    const arm = () => {

        if (!authStore.isAuthenticated) {
            disarm()
            return
        }

        disarm()

        timer = setTimeout(disconnect, timeoutMs)
    }

    const onActivity = () => {
        arm()
    }

    /**
     * Arme à la connexion, désarme à la déconnexion. `immediate` couvre le
     * cas d'un chargement de page sur une session restaurée.
     */
    watch(
        () => authStore.isAuthenticated,
        (connecte: boolean) => (connecte ? arm() : disarm()),
        { immediate: true }
    )

    ACTIVITY_EVENTS.forEach(event => {
        window.addEventListener(event, onActivity, { passive: true })
    })

    window.addEventListener(UNAUTHORIZED_EVENT, disconnect)

    onBeforeUnmount(() => {

        disarm()

        ACTIVITY_EVENTS.forEach(event => {
            window.removeEventListener(event, onActivity)
        })

        window.removeEventListener(UNAUTHORIZED_EVENT, disconnect)
    })

    return {
        /** Force la fermeture de session sans attendre le délai. */
        disconnect,
    }
}

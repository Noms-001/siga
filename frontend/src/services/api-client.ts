export const BASE_URL = import.meta.env.VITE_API_URL

/**
 * Structure standard de toutes les réponses API
 */
export type ApiResponse<T> = ApiSuccess<T> | ApiError

interface ApiSuccess<T> {
    success: true
    data: T
    error?: null
}

interface ApiError {
    success: false
    data: null
    error: string

    /**
     * Code HTTP de la reponse, ou 0 si elle n'a pas pu etre lue.
     *
     * Le message seul ne suffit pas a decider quoi faire d'un echec. Un 409
     * sur une soumission signifie "cette activite a deja quitte les
     * brouillons" et la carte doit partir ; un 422 ou un 400 signifie "regle
     * metier non respectee" et l'activite doit rester, avec son message. Sans
     * ce nombre, la page doit choisir au hasard -- et choisit generalement de
     * tout retirer.
     *
     * 0 et non null : une reponse illisible est une erreur comme une autre, et
     * `status === 0` se teste sans se soucier de undefined.
     */
    status: number
}

/**
 * Récupère le JWT depuis le stockage local
 */
function getAccessToken(): string | null {
    return localStorage.getItem('accessToken')
}

/**
 * Nom de l'événement émis quand le backend répond 401.
 *
 * api-client.ts est un utilitaire sans accès au store ni au router : importer
 * le store ici créerait un cycle (le store utilise api-client via
 * services/auth). Un événement window casse la dépendance et laisse
 * useSessionWatchdog décider de la redirection.
 */
export const UNAUTHORIZED_EVENT = 'saga:unauthorized'

/**
 * Signale une réponse 401 à l'application.
 *
 * Le 401 est aussi la réponse normale d'un échec de connexion : l'événement
 * n'est donc qu'un signal, et c'est au watchdog de ne rien faire si aucune
 * session n'est ouverte.
 */
function notifyIfUnauthorized(response: Response): void {
    if (response.status !== 401) {
        return
    }

    window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
}

/**
 * Construit les headers HTTP
 */
function getHeaders(
    includeContentType = false
): HeadersInit {

    const headers: HeadersInit = {}

    if (includeContentType) {
        headers['Content-Type'] = 'application/json'
    }

    const accessToken = getAccessToken()

    if (accessToken) {
        headers['Authorization'] = `Bearer ${accessToken}`
    }

    return headers
}

/**
 * Traite la réponse HTTP du backend.
 */
async function handleResponse<T>(
    response: Response
): Promise<ApiResponse<T>> {

    notifyIfUnauthorized(response)

    let result: ApiResponse<T>

    try {
        result = await response.json()
    } catch {
        return {
            success: false,
            data: null,
            error: 'Réponse invalide du serveur',
            status: response.status,
        }
    }

    /*
        Un succes declare par le corps sur une reponse HTTP en echec --
        200 dans le corps contre 409 reel, par exemple -- doit rester un echec.
        On ajoute le statut plutot que d'ecraser le corps : la distinction
        reste visible, et un appelant qui teste `success` ne peut pas se
        tromper.
    */
    if (result.success && !response.ok) {
        return {
            success: false,
            data: null,
            error: 'Réponse invalide du serveur',
            status: response.status,
        }
    }

    if (result.success) {
        return result
    }

    return { ...result, status: response.status }
}

/**
 * Requête GET
 */
export async function get<T>(
    url: string
): Promise<ApiResponse<T>> {

    const response = await fetch(`${BASE_URL}${url}`, {
        method: 'GET',
        headers: getHeaders(),
    })

    return handleResponse<T>(response)
}

/**
 * Requête GET renvoyant un blob (contenu d'un fichier).
 *
 * Contrairement à get(), le succès n'est pas JSON : le blob est brut. Seules
 * les erreurs gardent l'enveloppe { success, error } habituelle, ce qui
 * laisse l'appelant afficher la même erreur que sur un GET JSON.
 */
export async function getBlob(
    url: string
): Promise<ApiResponse<Blob>> {

    const response = await fetch(`${BASE_URL}${url}`, {
        method: 'GET',
        headers: getHeaders(),
    })

    if (response.ok) {
        return { success: true, data: await response.blob() }
    }

    notifyIfUnauthorized(response)

    try {
        const corps = await response.json()

        if (corps && typeof corps === 'object' && 'error' in corps) {
            return {
                success: false,
                data: null,
                error: String(corps.error),
                status: response.status,
            }
        }
    } catch {
        // Réponse d'erreur non JSON : traitee comme reponse invalide.
    }

    return {
        success: false,
        data: null,
        error: 'Réponse invalide du serveur',
        status: response.status,
    }
}

/**
 * Requête POST
 */
export async function post<T>(
    url: string,
    body: unknown
): Promise<ApiResponse<T>> {

    const response = await fetch(`${BASE_URL}${url}`, {
        method: 'POST',
        headers: getHeaders(true),
        body: JSON.stringify(body),
    })

    return handleResponse<T>(response)
}

/**
 * Requête PUT
 */
export async function put<T>(
    url: string,
    body: unknown
): Promise<ApiResponse<T>> {

    const response = await fetch(`${BASE_URL}${url}`, {
        method: 'PUT',
        headers: getHeaders(true),
        body: JSON.stringify(body),
    })

    return handleResponse<T>(response)
}

/**
 * Requête DELETE
 */
export async function del<T = unknown>(
    url: string,
    body?: unknown
): Promise<ApiResponse<T>> {

    const response = await fetch(`${BASE_URL}${url}`, {
        method: 'DELETE',
        headers: body
            ? getHeaders(true)
            : getHeaders(),
        body: body
            ? JSON.stringify(body)
            : undefined,
    })

    return handleResponse<T>(response)
}

/**
 * Requête POST avec FormData
 *
 * Utilisée notamment pour l'envoi de fichiers/images.
 */
export async function postForm<T>(
    url: string,
    formData: FormData
): Promise<ApiResponse<T>> {

    const response = await fetch(`${BASE_URL}${url}`, {
        method: 'POST',
        headers: getHeaders(),
        body: formData,
    })

    return handleResponse<T>(response)
}
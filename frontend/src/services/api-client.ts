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
    status: number
}

/**
 * Récupère le JWT depuis le stockage local
 */
function getAccessToken(): string | null {
    return localStorage.getItem('accessToken')
}

export const UNAUTHORIZED_EVENT = 'saga:unauthorized'

export interface ApiRequestOptions {
    /**
     * Appel d'authentification (login, activation, reset...).
     *
     * Un 401 y signifie « identifiants invalides », pas « session expirée » :
     * on laisse remonter le message d'erreur du backend.
     */
    authRequest?: boolean
}

function handleAuthFailure(): void {
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
    response: Response,
    options: ApiRequestOptions = {}
): Promise<ApiResponse<T>> {

    // 1. Cas explicite : 401 → session morte, sauf sur un appel
    //    d'authentification où c'est un échec d'identifiants.
    if (response.status === 401 && !options.authRequest) {
        handleAuthFailure()
        return {
            success: false,
            data: null,
            error: 'Session expirée',
            status: 401,
        }
    }

    // 2. Tentative de lecture JSON
    let result: ApiResponse<T>

    try {
        result = await response.json()
    } catch {
        if (!options.authRequest && getAccessToken() !== null) {
            handleAuthFailure()
            return {
                success: false,
                data: null,
                error: 'Session expirée',
                status: response.status,
            }
        }

        // Pas de token : vraie réponse serveur invalide
        return {
            success: false,
            data: null,
            error: 'Réponse invalide du serveur',
            status: response.status,
        }
    }

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
    url: string,
    options: ApiRequestOptions = {}
): Promise<ApiResponse<T>> {

    const response = await fetch(`${BASE_URL}${url}`, {
        method: 'GET',
        headers: getHeaders(),
    })

    return handleResponse<T>(response, options)
}

export async function getBlob(
    url: string,
    options: ApiRequestOptions = {}
): Promise<ApiResponse<Blob>> {

    const response = await fetch(`${BASE_URL}${url}`, {
        method: 'GET',
        headers: getHeaders(),
    })

    // 401 explicite
    if (response.status === 401 && !options.authRequest) {
        handleAuthFailure()
        return {
            success: false,
            data: null,
            error: 'Session expirée',
            status: 401,
        }
    }

    // Réponse OK : on vérifie qu'on n'a pas été redirigé vers du HTML
    if (response.ok) {
        const contentType = response.headers.get('content-type') ?? ''
        if (contentType.includes('text/html') && getAccessToken() !== null) {
            handleAuthFailure()
            return {
                success: false,
                data: null,
                error: 'Session expirée',
                status: response.status,
            }
        }
        return { success: true, data: await response.blob() }
    }

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
        if (getAccessToken() !== null) {
            handleAuthFailure()
            return {
                success: false,
                data: null,
                error: 'Session expirée',
                status: response.status,
            }
        }
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
    body: unknown,
    options: ApiRequestOptions = {}
): Promise<ApiResponse<T>> {

    const response = await fetch(`${BASE_URL}${url}`, {
        method: 'POST',
        headers: getHeaders(true),
        body: JSON.stringify(body),
    })

    return handleResponse<T>(response, options)
}

/**
 * Requête PUT
 */
export async function put<T>(
    url: string,
    body: unknown,
    options: ApiRequestOptions = {}
): Promise<ApiResponse<T>> {

    const response = await fetch(`${BASE_URL}${url}`, {
        method: 'PUT',
        headers: getHeaders(true),
        body: JSON.stringify(body),
    })

    return handleResponse<T>(response, options)
}

/**
 * Requête DELETE
 */
export async function del<T = unknown>(
    url: string,
    body?: unknown,
    options: ApiRequestOptions = {}
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

    return handleResponse<T>(response, options)
}

/**
 * Requête POST avec FormData
 */
export async function postForm<T>(
    url: string,
    formData: FormData,
    options: ApiRequestOptions = {}
): Promise<ApiResponse<T>> {

    const response = await fetch(`${BASE_URL}${url}`, {
        method: 'POST',
        headers: getHeaders(),
        body: formData,
    })

    return handleResponse<T>(response, options)
}

/**
 * Requête PATCH
 */
export async function patch<T>(
    url: string,
    body?: unknown,
    options: ApiRequestOptions = {}
): Promise<ApiResponse<T>> {

    const response = await fetch(`${BASE_URL}${url}`, {
        method: body ? 'PATCH' : 'PATCH',
        headers: body ? getHeaders(true) : getHeaders(),
        body: body ? JSON.stringify(body) : undefined,
    })

    return handleResponse<T>(response, options)
}
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
}

/**

* Traite la réponse HTTP du backend.
*
* Même si le backend retourne une erreur HTTP (400, 401, 403, 404, 500...),
* on récupère toujours la structure :
*
* {
* success: boolean,
* data: T | null,
* error: string | null
* }
  */
async function handleResponse<T>(
    response: Response
): Promise<ApiResponse<T>> {

    let result: ApiResponse<T>

    try {
        result = await response.json()
    } catch {
        return {
            success: false,
            data: null,
            error: 'Réponse invalide du serveur',
        }
    }

    // Le backend retourne déjà notre format standard
    return result
}

/**

* Requête GET
  */
export async function get<T>(
    url: string
): Promise<ApiResponse<T>> {

    const response = await fetch(`${BASE_URL}${url}`, {
        method: 'GET',
        credentials: 'include',
    })

    return handleResponse<T>(response)
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
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'include',
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
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'include',
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
            ? {
                'Content-Type': 'application/json',
            }
            : undefined,
        credentials: 'include',
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
        credentials: 'include',
        body: formData,
    })

    return handleResponse<T>(response)
}

import { get, post, type ApiResponse } from './api-client'

import type { ForgotPasswordRequest, LoginRequest, LoginResponse, ProfileResponse, ResetPasswordRequest } from '@/types/auth'

/**

* Connexion de l'utilisateur
  */
export async function login(
  data: LoginRequest
): Promise<ApiResponse<LoginResponse>> {

  const response = await post<LoginResponse>('/auth/login', data)

  console.log('=== AUTH LOGIN ===')
  console.log('Réponse complète :', response)
  console.log('success :', response.success)
  console.log('data :', response.data)
  console.log('error :', response.error)

  return response
}

/**

* Récupérer le profil de l'utilisateur connecté
  */
export async function getProfile(): Promise<ApiResponse<ProfileResponse>> {

  return await get<ProfileResponse>('/auth/me')
}

/**

* Déconnexion de l'utilisateur
  */
export async function logout(): Promise<ApiResponse<null>> {

  return await post<null>('/auth/logout', {})
}

/**
 * Demande de réinitialisation de mot de passe
 */
export function forgotPassword(
  data: ForgotPasswordRequest
): Promise<ApiResponse<null>> {
  return post<null>('/auth/password/forgot', data)
}

/**
 * Vérification de la validité du token de réinitialisation
 */
export function verifyResetPasswordToken(
  token: string
): Promise<ApiResponse<null>> {
  return get<null>(
    `/auth/password/reset/verify?token=${encodeURIComponent(token)}`
  )
}

/**
 * Réinitialisation du mot de passe
 */
export function resetPassword(
  data: ResetPasswordRequest
): Promise<ApiResponse<null>> {
  return post<null>('/auth/password/reset', data)
}

import { get, post, put, type ApiResponse } from './api-client'

import type {
  ChangerMotDePasseRequest,
  ForgotPasswordRequest,
  LoginRequest,
  LoginResponse,
  ModifierProfilRequest,
  ModifierProfilResponse,
  ProfileResponse,
  ResetPasswordRequest
} from '@/types/auth'

/**

* Connexion de l'utilisateur
  */
export async function login(
  data: LoginRequest
): Promise<ApiResponse<LoginResponse>> {

  const response = await post<LoginResponse>('/auth/login', data)

  return response
}

/**
 * Connexion de l'utilisateur backoffice
 */
export async function loginBackoffice(
  data: LoginRequest
): Promise<ApiResponse<LoginResponse>> {

  const response = await post<LoginResponse>('/backoffice/auth/login', data)

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

/**
 * Modification des informations personnelles de l'utilisateur connecté
 */
export function updateProfile(
  data: ModifierProfilRequest
): Promise<ApiResponse<ModifierProfilResponse>> {
  return put<ModifierProfilResponse>('/utilisateur/me', data)
}

/**
 * Changement du mot de passe de l'utilisateur connecté
 */
export function changePassword(
  data: ChangerMotDePasseRequest
): Promise<ApiResponse<null>> {
  return post<null>('/utilisateur/me/mot-de-passe', data)
}

import { reactive } from 'vue'
import { get, put } from './api-client'

export interface NotificationItem {
    id: number
    titre: string
    message: string
    dateCreation: string
    dateLecture: string | null
    idActivite: number | null
    activite: string | null
    prioriteCode: string | null
    prioriteLibelle: string | null
}

const state = reactive({ notifications: [] as NotificationItem[], unreadCount: 0 })

export interface NotificationPage {
    content: NotificationItem[]
    page: number
    size: number
    totalElements: number
    totalPages: number
    first: boolean
    last: boolean
}

export interface NotificationFiltres {
    search: string
    lecture: 'TOUTES' | 'NON_LUES' | 'LUES'
    priorite: string
}

export async function chargerPageNotifications(
    page: number,
    size = 10,
    filtres: NotificationFiltres = { search: '', lecture: 'TOUTES', priorite: '' },
    urgentes = false,
): Promise<NotificationPage | null> {
    const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        lecture: filtres.lecture,
    })
    if (filtres.search.trim()) params.set('search', filtres.search.trim())
    if (filtres.priorite) params.set('priorite', filtres.priorite)
    if (urgentes) params.set('urgentes', 'true')

    const rep = await get<NotificationPage>(`/notifications/page?${params.toString()}`)
    return rep.success ? rep.data : null
}

export async function actualiserNotifications(): Promise<void> {
    const [liste, compteur] = await Promise.all([
        get<NotificationItem[]>('/notifications'),
        get<number>('/notifications/non-lues/count'),
    ])
    if (liste.success) state.notifications = liste.data
    if (compteur.success) state.unreadCount = compteur.data
}

export async function marquerNotificationLue(id: number): Promise<boolean> {
    const notification = state.notifications.find(item => item.id === id)
    const etaitNonLue = !!notification && !notification.dateLecture
    const resultat = await put<boolean>(`/notifications/${id}/lue`, null)
    if (!resultat.success || !resultat.data) return false
    if (notification && etaitNonLue) {
        notification.dateLecture = new Date().toISOString()
        state.unreadCount = Math.max(0, state.unreadCount - 1)
    }
    return true
}

export async function marquerToutesNotificationsLues(): Promise<boolean> {
    const resultat = await put<number>('/notifications/lues', null)
    if (!resultat.success) return false
    state.notifications.forEach(notification => {
        if (!notification.dateLecture) notification.dateLecture = new Date().toISOString()
    })
    state.unreadCount = 0
    return true
}

export function useNotifications() { return state }

export interface NotificationContexte {
    [libelle: string]: string
}

export interface NotificationDetail {
    id: number
    titre: string
    message: string
    dateCreation: string
    dateLecture: string | null
    priorite: { id?: number; code?: string; libelle?: string } | null
    utilisateur: { id: number; nom: string; prenom: string } | null
    resource: string | null
    idReference: number | null
    libelleReference: string | null
    urlFront: string | null
    entiteDisponible: boolean
    contexte: NotificationContexte
}

export async function chargerDetailNotification(id: number): Promise<NotificationDetail | null> {
    const rep = await get<NotificationDetail>(`/notifications/${id}`)
    return rep.success ? rep.data : null
}

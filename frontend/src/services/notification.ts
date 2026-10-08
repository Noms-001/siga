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
}

const state = reactive({ notifications: [] as NotificationItem[], unreadCount: 0 })

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

export function useNotifications() { return state }

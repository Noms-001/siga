import { ref } from 'vue'
import type { ToastInstance, ToastItem, ToastType } from './BaseToast.types'

const toasts = ref<ToastItem[]>([])
let nextId = 0

const getTitle = (type: ToastType): string => {
    const titles: Record<ToastType, string> = {
        success: 'Succès',
        error: 'Erreur',
        info: 'Information',
        warning: 'Attention'
    }
    return titles[type]
}

const getIcon = (type: ToastType): string => {
    const icons: Record<ToastType, string> = {
        success: 'bi bi-check-circle-fill',
        error: 'bi bi-x-circle-fill',
        info: 'bi bi-info-circle-fill',
        warning: 'bi bi-exclamation-triangle-fill'
    }
    return icons[type]
}

const addToast = (message: string, type: ToastType = 'info', duration = 5000) => {
    const id = nextId++

    const toast: ToastItem = {
        id,
        message,
        type,
        time: new Date().toLocaleTimeString(),
        timer: null
    }

    toasts.value.push(toast)

    toast.timer = setTimeout(() => {
        removeToast(id)
    }, duration)

    return id
}

const removeToast = (id: number) => {
    const index = toasts.value.findIndex(t => t.id === id)

    if (index === -1) return

    const toast = toasts.value[index]
    if (!toast) return

    if (toast.timer) clearTimeout(toast.timer)

    toasts.value.splice(index, 1)
}

export function useBaseToast() {

    const success = (
        message: string,
        duration?: number
    ) => addToast(message, 'success', duration)

    const error = (
        message: string,
        duration?: number
    ) => addToast(message, 'error', duration)

    const info = (
        message: string,
        duration?: number
    ) => addToast(message, 'info', duration)

    const warning = (
        message: string,
        duration?: number
    ) => addToast(message, 'warning', duration)

    return {
        toasts,
        addToast,
        removeToast,
        getTitle,
        getIcon,
        success,
        error,
        info,
        warning
    }
}
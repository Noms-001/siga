import { ref, watch, type Ref } from 'vue'

import type { ToastInstance, ToastType } from "@/components/base/BaseToast"

let toastInstance: ToastInstance | null = null

export const useToast = () => {

    const showSuccess = (
        message: string,
        duration: number = 5000
    ): void => {
        if (toastInstance) {
            toastInstance.success(message, duration)
        } else {
            console.warn('Toast component not mounted yet')
        }
    }


    const showError = (
        message: string,
        duration: number = 5000
    ): void => {
        if (toastInstance) {
            toastInstance.error(message, duration)
        } else {
            console.warn('Toast component not mounted yet')
        }
    }


    const showInfo = (
        message: string,
        duration: number = 5000
    ): void => {
        if (toastInstance) {
            toastInstance.info(message, duration)
        } else {
            console.warn('Toast component not mounted yet')
        }
    }


    const showWarning = (
        message: string,
        duration: number = 5000
    ): void => {
        if (toastInstance) {
            toastInstance.warning(message, duration)
        } else {
            console.warn('Toast component not mounted yet')
        }
    }


    const setToastInstance = (
        instance: ToastInstance | null
    ): void => {
        toastInstance = instance
    }

    return {
        showSuccess,
        showError,
        showInfo,
        showWarning,
        setToastInstance
    }
}

/**
 * Ref de message qui affiche automatiquement son contenu dans un toast.
 *
 * Permet de remplacer un bandeau `alert` par une notification sans changer
 * la logique métier : les affectations existantes continuent d'alimenter la
 * ref, seul l'affichage change.
 */
export const useToastMessage = (
    type: ToastType,
    initial: string | null = null
): Ref<string | null> => {
    const message = ref<string | null>(initial)
    const { showSuccess, showError, showInfo, showWarning } = useToast()

    watch(message, (valeur) => {
        if (!valeur) return

        if (type === 'success') showSuccess(valeur)
        else if (type === 'error') showError(valeur)
        else if (type === 'warning') showWarning(valeur)
        else showInfo(valeur)
    })

    return message
}
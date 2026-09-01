import type { ToastInstance } from "@/components/base/BaseToast"

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
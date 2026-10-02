export type ToastType = 'success' | 'error' | 'info' | 'warning'

export interface ToastItem {
    id: number
    message: string
    type: ToastType
    time: string
    timer?: ReturnType<typeof setTimeout> | null
}

export interface ToastInstance {
    success(message: string, duration?: number): void
    error(message: string, duration?: number): void
    info(message: string, duration?: number): void
    warning(message: string, duration?: number): void
}
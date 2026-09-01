export type ModalSize = 'sm' | 'md' | 'lg' | 'xl'

export interface BaseModalProps {
    modelValue: boolean
    title?: string
    size?: ModalSize
    closeOnOutside?: boolean
    loading?: boolean
    customClass?: string | string[] | Record<string, boolean>
}
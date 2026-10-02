import type { PropType } from 'vue'

export type ButtonVariant =
    | 'primary'
    | 'secondary'
    | 'success'
    | 'danger'
    | 'warning'

export type ButtonSize =
    | 'sm'
    | 'md'
    | 'lg'

export type ButtonType =
    | 'button'
    | 'submit'
    | 'reset'

export const customClassType = [String, Array, Object] as PropType<
    string | string[] | Record<string, boolean>
>
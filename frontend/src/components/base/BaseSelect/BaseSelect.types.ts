export type SelectSize = 'sm' | 'md' | 'lg'

export type SelectOption =
    | string
    | number
    | Record<string, unknown>

export type SelectValue =
    | string
    | number
    | Record<string, any>
    | null

export interface BaseSelectProps {
    modelValue?: SelectValue

    label?: string
    placeholder?: string

    options?: SelectOption[]

    optionLabel?: string
    optionValue?: string

    optionFormatter?:
    | ((option: any) => string)
    | null

    required?: boolean
    disabled?: boolean

    error?: string

    customClass?: string | unknown[] | Record<string, any>

    size?: SelectSize

    autocomplete?: boolean

    allowCustom?: boolean
}
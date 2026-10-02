import type { PropType, Ref } from 'vue'

export type InputSize = 'sm' | 'md' | 'lg'

export type InputType =
    | 'text'
    | 'password'
    | 'email'
    | 'number'
    | 'tel'
    | 'url'
    | 'search'
    | 'date'
    | 'datetime-local'
    | 'time'
    | 'month'
    | 'week'
    | 'color'
    | 'textarea'
    | 'file'
    | 'checkbox'
    | 'radio'

export type InputModelValue =
    | string
    | number
    | boolean
    | object
    | unknown[]
    | File
    | File[]
    | null

export interface BaseInputProps {
    modelValue: InputModelValue
    value: string | number | boolean
    label: string
    placeholder: string
    type: InputType
    name: string
    required: boolean
    disabled: boolean
    readonly: boolean
    icon: string
    iconRight: string
    error: string
    customClass: any
    multiple: boolean
    accept: string
    helper: string
    size: InputSize
}

export interface BaseInputEmits {
    (e: 'update:modelValue', value: unknown): void
    (e: 'blur'): void
    (e: 'focus'): void
}

export interface UseBaseInputReturn {
    showPassword: Ref<boolean>
    isDragging: Ref<boolean>
    fileInput: Ref<HTMLInputElement | null>
}



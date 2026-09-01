import type { PropType } from 'vue'
import type { ModalSize } from './BaseModal.types'

export const baseModalProps = {
    modelValue: {
        type: Boolean,
        default: false
    },

    title: {
        type: String,
        default: ''
    },

    size: {
        type: String as PropType<ModalSize>,
        default: 'md'
    },

    closeOnOutside: {
        type: Boolean,
        default: true
    },

    loading: {
        type: Boolean,
        default: false
    },

    customClass: {
        type: [String, Array, Object] as PropType<
            string | string[] | Record<string, boolean>
        >,
        default: ''
    }
}

export const modalSizes: Record<ModalSize, string> = {
    sm: '420px',
    md: '620px',
    lg: '900px',
    xl: '1120px'
}
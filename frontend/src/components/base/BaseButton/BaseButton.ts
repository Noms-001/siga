import type { PropType } from 'vue'
import type {
    ButtonVariant,
    ButtonSize,
    ButtonType
} from './BaseButton.types'

export const baseButtonProps = {
    label: {
        type: String,
        default: ''
    },

    title: {
        type: String,
        default: ''
    },

    variant: {
        type: String as PropType<ButtonVariant>,
        default: 'primary'
    },

    size: {
        type: String as PropType<ButtonSize>,
        default: 'md'
    },

    loading: {
        type: Boolean,
        default: false
    },

    disabled: {
        type: Boolean,
        default: false
    },

    icon: {
        type: String,
        default: ''
    },

    type: {
        type: String as PropType<ButtonType>,
        default: 'button'
    },

    customClass: {
        type: [String, Array, Object],
        default: ''
    }
}
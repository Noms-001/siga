import { computed } from 'vue'
import type {
    ExtractPropTypes,
    PropType
} from 'vue'

import type {
    ProgressVariant
} from './BaseProgressBar.types'

export const baseProgressBarProps = {
    value: {
        type: Number,
        default: 0
    },

    label: {
        type: String,
        default: ''
    },

    variant: {
        type: String as PropType<ProgressVariant>,
        default: 'primary'
    },

    logs: {
        type: Array as PropType<{message: string, type: string, time: string}[]>,
        default: () => []
    },

    showLogs: {
        type: Boolean,
        default: true
    },

    max: {
        type: Number,
        default: 100
    }
}

export type BaseProgressBarPropsType =
    ExtractPropTypes<typeof baseProgressBarProps>

export function useBaseProgressBar(
    props: Readonly<BaseProgressBarPropsType>
) {
    const percent = computed(() => {
        const value = Number(props.value)

        if (Number.isNaN(value)) {
            return 0
        }

        return Math.min(
            props.max,
            Math.max(0, value)
        )
    })

    const variantClasses = computed(() => {
        const variants: Record<
            ProgressVariant,
            string
        > = {
            primary: 'progress-fill--primary',
            success: 'progress-fill--success',
            danger: 'progress-fill--danger',
            warning: 'progress-fill--warning',
            secondary: 'progress-fill--secondary'
        }

        return variants[
            props.variant as ProgressVariant
        ]
    })

    const statusText = computed(() => {
        return percent.value < props.max
            ? 'Chargement en cours...'
            : 'Terminé'
    })

    return {
        percent,
        variantClasses,
        statusText
    }
}
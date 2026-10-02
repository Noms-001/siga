import { computed, type PropType } from 'vue'
import type { ButtonVariant } from '../BaseButton/BaseButton.types'

export const baseCardProps = {
    variant: {
        type: String,
        default: 'default'
    },

    size: {
        type: String,
        default: 'md'
    },

    image: {
        type: String,
        default: ''
    },

    title: {
        type: String,
        default: ''
    },

    subtitle: {
        type: String,
        default: ''
    },

    description: {
        type: String,
        default: ''
    },

    price: {
        type: String,
        default: ''
    },

    oldPrice: {
        type: String,
        default: ''
    },

    value: {
        type: [String, Number],
        default: ''
    },

    label: {
        type: String,
        default: ''
    },

    icon: {
        type: String,
        default: ''
    },

    trend: {
        type: String,
        default: ''
    },

    trendType: {
        type: String,
        default: 'neutral'
    },

    badges: {
        type: Array,
        default: () => []
    },

    actionLabel: {
        type: String,
        default: ''
    },

    actionVariant: {
        type: String as PropType<ButtonVariant>,
        default: 'primary'
    },

    disabled: {
        type: Boolean,
        default: false
    },

    loading: {
        type: Boolean,
        default: false
    },

    customClass: {
        type: [String, Array, Object],
        default: ''
    },

    fullHeight: {
        type: Boolean,
        default: false
    }
}

export const baseCardEmits = ['click', 'actionClick']

export function useBaseCard(props: any, emit: any) {

    const handleActionClick = (event: Event) => {
        emit('actionClick')
        event.stopPropagation()
    }

    const badgeItems = computed(() => {
        return props.badges.map((badge: any) => {

            if (typeof badge === 'string') {
                return {
                    label: badge,
                    variant: 'secondary'
                }
            }

            return {
                label: badge.label || '',
                variant: badge.variant || 'secondary',
                icon: badge.icon || ''
            }
        })
    })

    const variantClasses = computed(() => {

        const variants: Record<string, string> = {
            primary: 'border-primary',
            secondary: 'border-info',
            success: 'border-success',
            danger: 'border-danger'
        }

        return variants[props.variant] || ''
    })

    const sizeClasses = computed(() => {

        const sizes: Record<string, string> = {
            sm: 'rounded-3',
            lg: 'rounded-4'
        }

        return sizes[props.size] || ''
    })

    const badgeVariantClasses = (variant: string) => {

        const variants: Record<string, string> = {
            primary: 'bg-primary bg-opacity-10 text-primary',
            success: 'bg-success bg-opacity-10 text-success',
            danger: 'bg-danger bg-opacity-10 text-danger',
            warning: 'bg-warning bg-opacity-10 text-warning',
            secondary: 'bg-secondary bg-opacity-10 text-secondary'
        }

        return variants[variant] || variants.secondary
    }

    const trendVariantClasses = computed(() => {

        const variants: Record<string, string> = {
            positive: 'bg-success bg-opacity-10 text-success',
            negative: 'bg-danger bg-opacity-10 text-danger',
            partial: 'bg-warning bg-opacity-10 text-warning',
            neutral: 'bg-secondary bg-opacity-10 text-secondary'
        }

        return variants[props.trendType] || variants.neutral
    })

    const hasValue = computed(() =>
        props.value !== '' &&
        props.value !== null &&
        props.value !== undefined
    )

    const hasPrice = computed(() =>
        props.price !== '' &&
        props.price !== null &&
        props.price !== undefined
    )

    const showValueSection = computed(() =>
        hasValue.value ||
        props.label ||
        props.trend ||
        props.icon ||
        badgeItems.value.length > 0
    )

    const showPriceSection = computed(() =>
        props.price || props.oldPrice
    )

    const handleClick = () => {

        if (!props.disabled && !props.loading) {
            emit('click')
        }
    }

    return {
        badgeItems,
        variantClasses,
        sizeClasses,
        trendVariantClasses,
        hasValue,
        hasPrice,
        showValueSection,
        showPriceSection,
        handleClick,
        handleActionClick,
        badgeVariantClasses
    }
}

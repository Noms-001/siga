import { computed, ref } from 'vue'
import type {
    BaseInputProps,
    InputModelValue,
    InputSize,
    InputType
} from './BaseInput.types'
import type { PropType } from 'vue'

export const baseInputProps = {
    modelValue: {
        type: [String, Number, Boolean, Object, Array] as PropType<InputModelValue>,
        default: null
    },

    value: {
        type: [Boolean, String, Number],
        default: ''
    },

    label: {
        type: String,
        default: ''
    },

    placeholder: {
        type: String,
        default: ''
    },

    type: {
        type: String as PropType<InputType>,
        default: 'text'
    },

    name: {
        type: String,
        default: ''
    },

    required: {
        type: Boolean,
        default: false
    },

    disabled: {
        type: Boolean,
        default: false
    },

    readonly: {
        type: Boolean,
        default: false
    },

    icon: {
        type: String,
        default: ''
    },

    iconRight: {
        type: String,
        default: ''
    },

    error: {
        type: String,
        default: ''
    },

    customClass: {
        type: [String, Array, Object],
        default: ''
    },

    multiple: {
        type: Boolean,
        default: false
    },

    accept: {
        type: String,
        default: ''
    },

    helper: {
        type: String,
        default: ''
    },

    size: {
        type: String as PropType<InputSize>,
        default: 'md'
    }
}

export const baseInputEmits = [
    'update:modelValue',
    'blur',
    'focus'
]

export function useBaseInput(props: BaseInputProps, emit: any) {

    const showPassword = ref(false)
    const isDragging = ref(false)
    const fileInput = ref<HTMLInputElement | null>(null)

    const sizeClass = computed(() =>
        `base-input-${props.size}`
    )

    const inputType = computed(() => {

        if (props.type !== 'password') {
            return props.type
        }

        return showPassword.value ? 'text' : 'password'
    })

    const isCheckable = computed(() =>
        ['checkbox', 'radio'].includes(props.type)
    )

    const files = computed(() => {

        if (props.type !== 'file') {
            return []
        }

        if (!props.modelValue) {
            return []
        }

        return Array.isArray(props.modelValue)
            ? props.modelValue
            : [props.modelValue]
    })

    const fileCountLabel = computed(() => {

        if (!files.value.length) {
            return props.placeholder ||
                'Glissez-déposez un fichier ou cliquez ici'
        }

        return `${files.value.length} fichier(s) sélectionné(s)`
    })

    const updateValue = (event: Event) => {

        if (props.type === 'file') {

            const target = event.target as HTMLInputElement
            const dropped = target.files

            const value = props.multiple
                ? Array.from(dropped ?? [])
                : dropped?.[0] ?? null

            emit('update:modelValue', value)

            return
        }

        const target = event.target as HTMLInputElement

        emit('update:modelValue', target.value)
    }

    const handleCheckableChange = (
        event: Event
    ) => {

        const target = event.target as HTMLInputElement

        if (props.type === 'radio') {

            emit('update:modelValue',props.value)

            return
        }

        if (
            props.type === 'checkbox' &&
            Array.isArray(props.modelValue)
        ) {

            const updatedArray = [
                ...props.modelValue
            ]

            const itemToToggle = props.value

            const index =
                updatedArray.findIndex(
                    item => {

                        if (
                            typeof itemToToggle === 'object' &&
                            typeof item === 'object'
                        ) {

                            return (
                                JSON.stringify(item)
                                ===
                                JSON.stringify(itemToToggle)
                            )
                        }

                        return item === itemToToggle
                    }
                )

            if (index > -1) {

                updatedArray.splice(
                    index,
                    1
                )

            } else {

                updatedArray.push(
                    itemToToggle
                )
            }

            emit(
                'update:modelValue',
                updatedArray
            )

            return
        }

        emit(
            'update:modelValue',
            target.checked
        )
    }

    const isItemChecked = computed(() => {

        if (!Array.isArray(props.modelValue)) {
            return false
        }

        return props.modelValue.some(item => {

            if (
                typeof props.value === 'object' &&
                typeof item === 'object'
            ) {

                return (
                    JSON.stringify(item)
                    ===
                    JSON.stringify(props.value)
                )
            }

            return item === props.value
        })
    })

    const triggerFileSelect = () => {
        fileInput.value?.click()
    }

    const handleDragOver = (
        event: DragEvent
    ) => {

        event.preventDefault()

        isDragging.value = true
    }

    const handleDragLeave = () => {

        isDragging.value = false
    }

    const handleDrop = (
        event: DragEvent
    ) => {

        event.preventDefault()

        isDragging.value = false

        const dropped =
            Array.from(
                event.dataTransfer?.files ?? []
            )

        if (!dropped.length) {
            return
        }

        const value = props.multiple
            ? dropped
            : dropped[0]

        emit(
            'update:modelValue',
            value
        )
    }

    const removeFile = (
        index: number
    ) => {

        const currentFiles = [
            ...files.value
        ]

        currentFiles.splice(
            index,
            1
        )

        const value = props.multiple
            ? currentFiles
            : currentFiles[0] || null

        emit(
            'update:modelValue',
            value
        )
    }

    return {

        showPassword,
        isDragging,
        fileInput,

        sizeClass,
        inputType,
        isCheckable,

        files,
        fileCountLabel,
        isItemChecked,

        updateValue,
        handleCheckableChange,

        triggerFileSelect,

        handleDragOver,
        handleDragLeave,
        handleDrop,

        removeFile
    }
}


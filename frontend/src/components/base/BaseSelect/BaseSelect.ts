import {
    computed,
    ref,
    watch,
    onMounted,
    onBeforeUnmount,
    type PropType
} from 'vue'

import type {
    BaseSelectProps,
    SelectOption,
    SelectSize
} from './BaseSelect.types'

export const baseSelectProps = {
    modelValue: {
        type: [String, Number, Object],
        default: null
    },

    label: {
        type: String,
        default: ''
    },

    placeholder: {
        type: String,
        default: 'Sélectionner'
    },

    options: {
        type: Array as PropType<SelectOption[]>,
        default: () => []
    },

    optionLabel: {
        type: String,
        default: 'label'
    },

    optionValue: {
        type: String,
        default: 'value'
    },

    optionFormatter: {
        type: Function as unknown as PropType<
            ((option: any) => string) | null
        >,
        default: null
    },

    required: {
        type: Boolean,
        default: false
    },

    disabled: {
        type: Boolean,
        default: false
    },

    error: {
        type: String,
        default: ''
    },

    customClass: {
        type: [String, Array, Object],
        default: ''
    },

    size: {
        type: String as PropType<SelectSize>,
        default: 'md'
    },

    autocomplete: {
        type: Boolean,
        default: false
    },

    allowCustom: {
        type: Boolean,
        default: false
    }
}

export const baseSelectEmits = [
    'update:modelValue'
]

export function useBaseSelect(
    props: Readonly<BaseSelectProps>,
    emit: (event: 'update:modelValue', value: any) => void
) {
    const searchText = ref('')
    const showDropdown = ref(false)

    const highlightedIndex = ref(-1)

    const inputRef = ref<HTMLInputElement | null>(null)

    const dropdownRef = ref<HTMLElement | null>(null)

    const getOptionLabel = (
        option: SelectOption
    ): string => {

        if (props.optionFormatter) {
            return props.optionFormatter(option)
        }

        if (
            typeof option === 'object' &&
            option !== null
        ) {
            return String(
                option[props.optionLabel ?? 'label']
            )
        }

        return String(option)
    }

    const getOptionValue = (
        option: SelectOption
    ) => {

        if (
            typeof option === 'object' &&
            option !== null
        ) {
            return option[
                props.optionValue ?? 'value'
            ]
        }

        return option
    }

    const selectedOption = computed(() => {

        if (
            props.modelValue === null ||
            props.modelValue === undefined ||
            props.modelValue === ''
        ) {
            return null
        }

        return props.options?.find(
            option =>
                getOptionValue(option) === props.modelValue
        ) ?? null
    })

    const updateSearchTextFromValue = () => {

        if (selectedOption.value) {
            searchText.value = getOptionLabel(selectedOption.value)
            return
        }

        if (
            props.autocomplete &&
            props.allowCustom &&
            typeof props.modelValue === 'string'
        ) {
            searchText.value = props.modelValue
            return
        }

        searchText.value = ''
    }
    const updateValue = (
        event: Event
    ) => {

        const target = event.target as HTMLSelectElement

        let value: string | number = target.value

        if (value !== '' && !isNaN(Number(value))) {
            value = Number(value)
        }

        emit('update:modelValue', value)
    }

    const handleInput = (
        event: Event
    ) => {

        const target =
            event.target as HTMLInputElement

        searchText.value =
            target.value

        showDropdown.value = true

        highlightedIndex.value = -1

        if (props.autocomplete && props.allowCustom) {

            const trimmedValue = target.value.trim()

            const matchingOption =
                props.options?.some(
                    option =>
                        getOptionLabel(option)
                            .toLowerCase() ===
                        trimmedValue.toLowerCase()
                )

            if (trimmedValue !== '' && !matchingOption) {
                emit('update:modelValue', trimmedValue)
            }
        }
    }

    const filteredOptions = computed(() => {

        if (
            !searchText.value.trim()
        ) {
            return props.options ?? []
        }

        const search =
            searchText.value.toLowerCase()

        return (
            props.options ?? []
        ).filter(option =>
            getOptionLabel(option)
                .toLowerCase()
                .includes(search)
        )
    })

    const selectOption = (
        option?: SelectOption
    ) => {

        if (!option) {
            return
        }

        if (props.disabled) {
            return
        }

        const value = getOptionValue(option)

        emit('update:modelValue', value)

        searchText.value = getOptionLabel(option)

        showDropdown.value = false

        highlightedIndex.value = -1
    }

    const canAcceptCustomValue =
        () => {

            return (
                props.autocomplete &&
                props.allowCustom &&
                searchText.value.trim() !== ''
            )
        }

    const submitCustomValue =
        () => {

            if (!canAcceptCustomValue()) {
                return
            }

            emit(
                'update:modelValue',
                searchText.value.trim()
            )

            showDropdown.value = false

            highlightedIndex.value = -1
        }
    const handleKeydown = (
        event: KeyboardEvent
    ) => {

        if (!showDropdown.value || filteredOptions.value.length === 0) {
            return
        }

        if (event.key === 'ArrowDown') {
            event.preventDefault()
            highlightedIndex.value = (highlightedIndex.value + 1) % filteredOptions.value.length
        }

        else if (event.key === 'ArrowUp') {
            event.preventDefault()
            highlightedIndex.value =
                highlightedIndex.value <= 0
                    ? filteredOptions.value.length - 1
                    : highlightedIndex.value - 1
        }

        else if (event.key === 'Enter') {
            event.preventDefault()
            if (highlightedIndex.value >= 0) {

                selectOption(
                    filteredOptions.value[
                    highlightedIndex.value
                    ]
                )
            }

            else if (canAcceptCustomValue()) {
                submitCustomValue()
            }
        }

        else if (event.key === 'Escape') {
            showDropdown.value = false
            highlightedIndex.value = -1
        }
    }
    const handleClickOutside = (
        event: MouseEvent
    ) => {

        const target =
            event.target as Node

        if (
            inputRef.value &&
            !inputRef.value.contains(target) &&
            dropdownRef.value &&
            !dropdownRef.value.contains(target)
        ) {

            showDropdown.value = false

            if (
                !selectedOption.value ||
                searchText.value !==
                getOptionLabel(selectedOption.value)
            ) {

                if (
                    props.autocomplete &&
                    props.allowCustom &&
                    searchText.value.trim() !== ''
                ) {

                    emit('update:modelValue', searchText.value.trim())
                }

                updateSearchTextFromValue()
            }
        }
    }
    watch(
        () => props.modelValue,
        (newValue) => {

            if (!props.autocomplete) {
                return
            }

            if (
                newValue === null ||
                newValue === undefined ||
                newValue === ''
            ) {

                searchText.value = ''

                showDropdown.value = false

                highlightedIndex.value = -1

                return
            }

            updateSearchTextFromValue()
        },
        {
            immediate: true
        }
    )

    onMounted(() => {

        if (!props.autocomplete) {
            return
        }

        updateSearchTextFromValue()

        document.addEventListener(
            'click',
            handleClickOutside
        )
    })

    onBeforeUnmount(() => {

        document.removeEventListener(
            'click',
            handleClickOutside
        )
    })

    return {
        searchText,
        showDropdown,
        highlightedIndex,
        inputRef,
        dropdownRef,
        filteredOptions,
        selectedOption,

        getOptionLabel,
        getOptionValue,
        updateValue,
        handleInput,
        selectOption,
        handleKeydown,
        handleClickOutside,
        updateSearchTextFromValue,
        canAcceptCustomValue,
        submitCustomValue
    }
}
<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import {
    baseSelectProps,
    baseSelectEmits,
    useBaseSelect
} from './BaseSelect'

const props = defineProps(baseSelectProps)

const emit = defineEmits(baseSelectEmits)

const {
    searchText,
    showDropdown,
    highlightedIndex,
    inputRef,
    dropdownRef,
    filteredOptions,
    getOptionLabel,
    getOptionValue,
    updateValue,
    handleInput,
    selectOption,
    handleKeydown,
    hasValue,
    clear
} = useBaseSelect(props, emit)

const handleClickOutside = (event: MouseEvent) => {
    const target = event.target as Node

    if (
        !inputRef.value?.contains(target) &&
        !dropdownRef.value?.contains(target)
    ) {
        showDropdown.value = false
    }
}

onMounted(() => {
    document.addEventListener('mousedown', handleClickOutside)
})

onBeforeUnmount(() => {
    document.removeEventListener('mousedown', handleClickOutside)
})
</script>

<template>
    <div :class="['base-select-container', props.customClass]">

        <label v-if="props.label" class="base-select-label">
            {{ props.label }}
            <span v-if="props.required" class="required">*</span>
        </label>

        <!-- SELECT NATIF -->
        <div v-if="!props.autocomplete" class="input-wrapper">
            <i v-if="props.icon" :class="['input-icon-left', props.icon]" />

            <select :value="props.modelValue" :disabled="props.disabled" :class="[
                'base-select',
                `base-select-${props.size}`,
                {
                    'base-select-error': props.error,
                    'has-left-icon': props.icon,
                    'has-right-icon': props.iconRight || (props.clearable && hasValue),
                    'has-custom-right-icon': props.iconRight
                }
            ]" @change="updateValue">
                <option value="">{{ props.placeholder }}</option>
                <option v-for="option in props.options" :key="String(getOptionValue(option))"
                    :value="getOptionValue(option)">
                    {{ props.optionFormatter ? props.optionFormatter(option) : getOptionLabel(option) }}
                </option>
            </select>

            <button v-if="props.clearable && hasValue && !props.disabled" type="button"
                class="input-icon-right clear-btn" aria-label="Effacer" title="Effacer" @click.stop.prevent="clear">
                <i class="bi bi-x-lg"></i>
            </button>
            <i v-else-if="props.iconRight" :class="['input-icon-right', props.iconRight]" />
        </div>

        <!-- AUTOCOMPLETE -->
        <div v-else class="autocomplete-wrapper position-relative w-100">
            <i v-if="props.icon" :class="['input-icon-left', props.icon]" />

            <input ref="inputRef" v-model="searchText" type="text" :placeholder="props.placeholder"
                :disabled="props.disabled" :class="[
                    'form-control',
                    `input-${props.size}`,
                    {
                        'is-invalid': props.error,
                        'has-left-icon': props.icon,
                        'has-right-icon': props.iconRight || (props.clearable && hasValue)
                    }
                ]" @focus="showDropdown = true" @keydown="handleKeydown" @input="handleInput">

            <button v-if="props.clearable && hasValue && !props.disabled" type="button"
                class="input-icon-right clear-btn" aria-label="Effacer" title="Effacer" @click.stop.prevent="clear">
                <i class="bi bi-x-lg"></i>
            </button>
            <i v-else-if="props.iconRight" :class="['input-icon-right', props.iconRight]" />

            <ul v-if="showDropdown && filteredOptions.length > 0" ref="dropdownRef"
                class="dropdown-menu show w-100 autocomplete-dropdown">
                <li v-for="(option, index) in filteredOptions" :key="String(getOptionValue(option))" :class="[
                    'dropdown-item',
                    { active: index === highlightedIndex }
                ]" @click="selectOption(option)" @mouseenter="highlightedIndex = index">
                    {{ getOptionLabel(option) }}
                </li>
            </ul>

            <small v-if="props.error" class="error-message text-danger mt-1">
                {{ props.error }}
            </small>
        </div>

        <small v-if="props.error && !props.autocomplete" class="error-message">
            {{ props.error }}
        </small>
    </div>
</template>

<style scoped>
.base-select-container {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.base-select-label {
    font-weight: 600;
    color: var(--text-color);
}

.required {
    color: var(--danger-color);
    margin-left: 0.2rem;
}

.input-wrapper {
    position: relative;
    width: 100%;
}

/* --- Icônes --- */
.input-icon-left,
.input-icon-right {
    position: absolute;
    inset-block-start: 50%;
    transform: translateY(-50%);
    color: var(--text-muted-custom);
    font-size: 1rem;
    pointer-events: none;
    z-index: 2;
}

.input-icon-left {
    inset-inline-start: 1rem;
}

.input-icon-right {
    inset-inline-end: 1rem;
}

.has-left-icon {
    padding-left: 3rem !important;
}

.has-right-icon {
    padding-right: 3rem !important;
}

/* --- Bouton effacer (X) --- */
.clear-btn {
    pointer-events: auto;
    display: grid;
    place-items: center;
    width: 22px;
    height: 22px;
    padding: 0;
    border: none;
    border-radius: 50%;
    background: transparent;
    color: var(--text-muted-custom);
    font-size: 0.85rem;
    cursor: pointer;
    transition: background var(--transition-fast), color var(--transition-fast);
}

.clear-btn:hover {
    background: rgba(239, 68, 68, 0.12);
    color: var(--danger-color);
}

/* --- Select natif --- */
.base-select {
    width: 100%;
    border-radius: var(--radius-lg);
    border: 1px solid var(--border-color);
    background: var(--surface-color);
    color: var(--text-color);
    transition:
        border-color var(--transition-fast),
        box-shadow var(--transition-fast);
}

.base-select:focus {
    outline: none;
    border-color: var(--primary-color);
    box-shadow: 0 0 0 4px #4664e514;
}

.base-select-error {
    border-color: var(--danger-color);
}

.error-message {
    color: var(--danger-color);
    font-size: 0.85rem;
}

.base-select-sm {
    padding: 0.5rem 0.95rem;
    font-size: 0.92rem;
}

.base-select-md {
    padding: 0.75rem 1rem;
    font-size: 0.95rem;
}

.base-select-lg {
    padding: 1.15rem 1.1rem;
    font-size: 1rem;
}

/* Flèche native désactivée si iconRight ou clearable visible */
select.base-select.has-custom-right-icon {
    appearance: none;
    -webkit-appearance: none;
    -moz-appearance: none;
    background-image: none;
}

/* --- Autocomplete --- */
.input-sm {
    padding: 0.5rem 0.95rem;
    font-size: 0.92rem;
}

.input-md {
    padding: 0.7rem 1rem;
    font-size: 0.95rem;
}

.input-lg {
    padding: 1.15rem 1.1rem;
    font-size: 1rem;
}

.autocomplete-dropdown {
    margin-top: 4px;
    max-height: 250px;
    overflow-y: auto;
    position: absolute;
    z-index: 1000;
    background: var(--surface-color);
    border: 1px solid var(--border-color);
    width: 100%;
}

.dropdown-item {
    cursor: pointer;
}

.dropdown-item.active,
.dropdown-item:active {
    background: var(--primary-color);
    color: white;
}
</style>
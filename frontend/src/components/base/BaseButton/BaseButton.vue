<script setup lang="ts">
import { baseButtonProps } from './BaseButton'

defineProps(baseButtonProps)
</script>

<template>
    <button :type="type" :disabled="disabled || loading" :title="title" :class="[
        'base-btn',
        `base-btn--${variant}`,
        `base-btn--${size}`,
        customClass,
        'no-row-click'
    ]">
        <span v-if="loading" class="spinner"></span>

        <template v-else>
            <i v-if="icon" :class="icon"></i>

            <template v-if="$slots.default">
                <slot />
            </template>

            <template v-else>
                {{ label }}
            </template>
        </template>
    </button>
</template>

<style scoped>
.base-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.65rem;
    border-radius: var(--radius-lg);
    border: 1px solid transparent;
    background: var(--primary-color);
    color: #fff;
    font-weight: 700;
    letter-spacing: 0.01em;
    transition:
        transform var(--transition-fast),
        box-shadow var(--transition-fast),
        filter var(--transition-fast);
    box-shadow: var(--shadow-sm);
    cursor: pointer;
}

.base-btn:hover:not(:disabled) {
    transform: translateY(-1px);
    filter: brightness(1.05);
}

.base-btn:disabled {
    opacity: 0.72;
    cursor: not-allowed;
    filter: none;
}

.base-btn--sm {
    padding: 0.5rem 1.1rem;
    font-size: 0.8rem;
}

.base-btn--md {
    padding: 0.75rem 1.5rem;
    font-size: 0.9rem;
}

.base-btn--lg {
    padding: 1rem 1.9rem;
    font-size: 1rem;
}

.base-btn--primary {
    background: var(--primary-color);
}

.base-btn--secondary {
    background: transparent;
    color: var(--secondary-color);
    border: 2px solid var(--secondary-color);
}

.base-btn--secondary:hover:not(:disabled) {
    background: #4693e51f;
}

.base-btn--success {
    background: var(--success-color);
}

.base-btn--danger {
    background: var(--danger-color);
}

.base-btn--warning {
    background: var(--warning-color);
}

.spinner {
    width: 16px;
    height: 16px;
    border: 2px solid rgba(255, 255, 255, 0.8);
    border-top-color: transparent;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
}

@keyframes spin {
    from {
        transform: rotate(0deg);
    }

    to {
        transform: rotate(360deg);
    }
}
</style>
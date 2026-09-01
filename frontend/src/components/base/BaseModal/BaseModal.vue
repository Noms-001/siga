<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import {
    baseModalProps,
    modalSizes
} from './BaseModal'

const props = defineProps(baseModalProps)

const emit = defineEmits<{
    (e: 'update:modelValue', value: boolean): void
    (e: 'close'): void
}>()

const close = () => {
    emit('update:modelValue', false)
    emit('close')
}

const handleOverlayClick = () => {
    if (props.closeOnOutside) {
        close()
    }
}

const handleEscape = (event: KeyboardEvent) => {
    if (event.key === 'Escape') {
        close()
    }
}

onMounted(() => {
    window.addEventListener('keydown', handleEscape)
})

onUnmounted(() => {
    window.removeEventListener('keydown', handleEscape)
})
</script>

<template>
    <Teleport to="body">
        <div v-if="modelValue" class="modal-overlay" @click="handleOverlayClick">
            <div class="modal-content" :class="customClass" :style="{ width: modalSizes[size] }" @click.stop>
                <div class="modal-header">

                    <slot name="header">
                        <h3>{{ title }}</h3>
                    </slot>

                    <button class="close-btn" @click="close" aria-label="Fermer">
                        ×
                    </button>

                </div>

                <div class="modal-body" :aria-busy="loading">

                    <template v-if="!loading">
                        <slot />
                    </template>

                    <template v-else>
                        <div class="modal-skeleton">
                            <div class="skeleton skeleton-title"></div>
                            <div class="skeleton skeleton-line"></div>
                            <div class="skeleton skeleton-line"></div>
                            <div class="skeleton skeleton-line short"></div>
                        </div>
                    </template>

                </div>

                <div v-if="$slots.footer" class="modal-footer">
                    <slot name="footer" />
                </div>

            </div>
        </div>
    </Teleport>
</template>

<style scoped>
.modal-overlay {
    position: fixed;
    inset: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    background: rgba(2, 12, 27, 0.7);
    backdrop-filter: blur(6px);
    z-index: 9999;
}

.modal-content {
    max-width: 95%;
    max-height: 90vh;
    overflow: auto;
    border-radius: var(--radius-xl);
    background: var(--card-bg);
    box-shadow: var(--shadow-lg);
}

.modal-header,
.modal-footer {
    padding: 1.25rem 1.5rem;
}

.modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid var(--border-color);
}

.modal-body {
    padding: 1.5rem;
}

.modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
    border-top: 1px solid var(--border-color);
}

.close-btn {
    width: 42px;
    height: 42px;
    border: none;
    border-radius: 14px;
    background: #4664e514;
    cursor: pointer;
}
</style>
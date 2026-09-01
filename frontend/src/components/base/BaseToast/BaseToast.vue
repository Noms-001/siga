<script setup lang="ts">
import { useBaseToast } from './BaseToast'

const {
    toasts,
    removeToast,
    getIcon,
    getTitle,
    success,
    error,
    info,
    warning
} = useBaseToast()

defineExpose({
    success,
    error,
    info,
    warning
})
</script>

<template>
    <Teleport to="body">
        <div class="toast-container">
            <div v-for="toast in toasts" :key="toast.id" class="toast show" :class="`toast-${toast.type}`" role="alert"
                @click="removeToast(toast.id)">
                <div class="toast-accent"></div>

                <div class="toast-content">
                    <div class="toast-icon">
                        <i :class="getIcon(toast.type)" />
                    </div>

                    <div class="toast-text">
                        <div class="toast-title">
                            {{ getTitle(toast.type) }}
                        </div>

                        <div class="toast-message">
                            {{ toast.message }}
                        </div>

                        <div class="toast-time">
                            {{ toast.time }}
                        </div>
                    </div>

                    <button class="toast-close" @click.stop="removeToast(toast.id)">
                        <i class="bi bi-x-lg" />
                    </button>
                </div>
            </div>
        </div>
    </Teleport>
</template>

<style scoped>
/* Container des toasts */
.toast-container {
    position: fixed;
    top: 1.5rem;
    right: 1.5rem;
    z-index: 999999;
    width: 420px;
    max-width: calc(100vw - 3rem);
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    pointer-events: none;
}

/* Toast de base */
.toast {
    position: relative;
    overflow: hidden;
    pointer-events: auto;
    cursor: pointer;
    background: var(--card-bg);
    backdrop-filter: blur(16px);
    border-radius: 1rem;
    border: 1px solid rgba(255, 255, 255, 0.3);
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.12), 0 4px 12px rgba(0, 0, 0, 0.08);
    transition: all 0.25s ease;
    animation: toastIn 0.35s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

.toast:hover {
    transform: translateX(-4px);
    box-shadow: 0 20px 35px -8px rgba(0, 0, 0, 0.2), 0 6px 15px -5px rgba(0, 0, 0, 0.1);
}

/* Contenu principal */
.toast-content {
    display: flex;
    align-items: flex-start;
    gap: 0.875rem;
    padding: 1rem;
}

/* Icône */
.toast-icon {
    width: 40px;
    height: 40px;
    flex-shrink: 0;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1.25rem;
    transition: transform 0.2s ease;
}

.toast:hover .toast-icon {
    transform: scale(1.05);
}

/* Texte */
.toast-text {
    flex: 1;
    min-width: 0;
}

.toast-title {
    font-weight: 700;
    font-size: 0.9375rem;
    margin-bottom: 0.25rem;
    line-height: 1.3;
    letter-spacing: -0.01em;
}

.toast-message {
    font-size: 0.875rem;
    line-height: 1.45;
    opacity: 0.85;
}

.toast-time {
    margin-top: 0.375rem;
    font-size: 0.6875rem;
    opacity: 0.55;
    font-weight: 500;
}

/* Bouton fermeture */
.toast-close {
    border: none;
    background: transparent;
    opacity: 0.4;
    transition: all 0.2s ease;
    padding: 0.25rem;
    cursor: pointer;
    border-radius: 6px;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.toast-close:hover {
    opacity: 1;
    background: rgba(0, 0, 0, 0.05);
    transform: scale(1.1);
}

/* Accent color bar */
.toast-accent {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;
}

/* ============================================
   VARIANTS
   ============================================ */

/* SUCCESS */
.toast-success {
    color: #059669;
}

.toast-success .toast-accent {
    background: linear-gradient(135deg, #10b981, #059669);
}

.toast-success .toast-icon {
    background: rgba(16, 185, 129, 0.12);
    color: #059669;
}

.toast-success .toast-title {
    color: #065f46;
}

/* ERROR */
.toast-error {
    color: #dc2626;
}

.toast-error .toast-accent {
    background: linear-gradient(135deg, #ef4444, #dc2626);
}

.toast-error .toast-icon {
    background: rgba(220, 38, 38, 0.12);
    color: #dc2626;
}

.toast-error .toast-title {
    color: #991b1b;
}

/* INFO */
.toast-info {
    color: #0284c7;
}

.toast-info .toast-accent {
    background: linear-gradient(135deg, #0ea5e9, #0284c7);
}

.toast-info .toast-icon {
    background: rgba(14, 165, 233, 0.12);
    color: #0284c7;
}

.toast-info .toast-title {
    color: #075985;
}

/* WARNING */
.toast-warning {
    color: #ea580c;
}

.toast-warning .toast-accent {
    background: linear-gradient(135deg, #f97316, #ea580c);
}

.toast-warning .toast-icon {
    background: rgba(249, 115, 22, 0.12);
    color: #ea580c;
}

.toast-warning .toast-title {
    color: #9a3412;
}

/* ============================================
   ANIMATIONS
   ============================================ */

@keyframes toastIn {
    0% {
        opacity: 0;
        transform: translateX(100%) scale(0.96);
    }

    100% {
        opacity: 1;
        transform: translateX(0) scale(1);
    }
}

@keyframes countdown {
    from {
        transform: scaleX(1);
    }

    to {
        transform: scaleX(0);
    }
}

/* ============================================
   RESPONSIVE
   ============================================ */

/* Tablette */
@media (max-width: 768px) {
    .toast-container {
        top: 1rem;
        right: 1rem;
        width: 380px;
        max-width: calc(100vw - 2rem);
    }

    .toast-content {
        padding: 0.875rem;
        gap: 0.75rem;
    }

    .toast-icon {
        width: 36px;
        height: 36px;
        font-size: 1.125rem;
    }

    .toast-title {
        font-size: 0.875rem;
    }

    .toast-message {
        font-size: 0.8125rem;
    }
}

/* Mobile */
@media (max-width: 480px) {
    .toast-container {
        top: 0.75rem;
        right: 0.75rem;
        left: 0.75rem;
        width: auto;
        max-width: none;
    }

    .toast-content {
        padding: 0.75rem;
        gap: 0.625rem;
    }

    .toast-icon {
        width: 32px;
        height: 32px;
        font-size: 1rem;
        border-radius: 10px;
    }

    .toast-title {
        font-size: 0.8125rem;
    }

    .toast-message {
        font-size: 0.75rem;
    }

    .toast-time {
        font-size: 0.625rem;
    }

    .toast-close {
        width: 20px;
        height: 20px;
    }
}
</style>
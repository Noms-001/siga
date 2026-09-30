<template>
    <footer class="back-footer">
        <div class="back-footer__container">
            <span class="back-footer__text">
                &copy; {{ currentYear }} DTS — Département Etudes Sureté et Sécurité.
                <span class="back-footer__badge">Backoffice</span>
            </span>
            <span class="back-footer__text">Version {{ appVersion }}</span>
        </div>
    </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'

// --- Props ---
const props = defineProps<{
    appVersion?: string
}>()

// --- Computed ---
const currentYear = computed(() => new Date().getFullYear())
const appVersion = computed(() => props.appVersion || '1.0.0')
</script>

<style scoped>
/* ============================================
   BACKOFFICE FOOTER — Design DTS-TSS
   ============================================ */

.back-footer {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
    padding: 0.9rem 1.5rem;
    background: var(--dts-navy);
    color: rgba(255, 255, 255, 0.78);
    font-size: 0.8rem;
}

.back-footer__container {
    display: contents;
}

.back-footer__text {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    font-size: 0.8rem;
    color: rgba(255, 255, 255, 0.78);
}

.back-footer__badge {
    font-size: 0.6rem;
    font-weight: 700;
    background: rgba(255, 255, 255, 0.14);
    color: rgba(255, 255, 255, 0.88);
    padding: 0.1rem 0.5rem;
    border-radius: 20px;
    letter-spacing: 0.05em;
    text-transform: uppercase;
}

@media (max-width: 575.98px) {
    .back-footer {
        flex-direction: column;
        align-items: flex-start;
        padding: 0.9rem 1rem;
    }

    .back-footer__text {
        font-size: 0.74rem;
    }
}
</style>

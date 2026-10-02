<template>
    <footer class="footer">
        <div class="container-fluid">
            <span class="text-muted-custom small">
                &copy; {{ currentYear }} DTS — Département Etudes Sureté et Sécurité. Tous droits réservés.
            </span>
            <span class="text-muted-custom small">Version {{ appVersion }}</span>
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
   FRONTOFFICE FOOTER — Design DTS-TSS
   ============================================ */

.footer {
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

.footer .container-fluid {
    display: contents;
}

.footer .text-muted-custom {
    color: rgba(255, 255, 255, 0.78) !important;
    font-size: 0.8rem;
}

@media (max-width: 575.98px) {
    .footer {
        flex-direction: column;
        align-items: flex-start;
        padding: 0.9rem 1rem;
    }

    .footer .text-muted-custom {
        font-size: 0.74rem;
    }
}
</style>

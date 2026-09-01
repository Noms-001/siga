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
   FOOTER - Styles spécifiques au composant
   ============================================ */

.footer {
    background-color: var(--card-bg);
    border-top: 1px solid var(--border-color);
    padding: 1rem 0;
    flex-shrink: 0;
}

.footer .container-fluid {
    padding: 0 24px;
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    align-items: center;
}

.footer .text-muted-custom {
    font-size: 0.85rem;
}

@media (max-width: 575.98px) {
    .footer .container-fluid {
        flex-direction: column;
        gap: 0.5rem;
        text-align: center;
    }
}
</style>
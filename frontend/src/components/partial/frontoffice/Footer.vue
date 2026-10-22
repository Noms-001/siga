<template>
    <footer class="footer">
        <span class="footer-left">
            © {{ currentYear }} DTS — Département Études Sûreté et Sécurité.
        </span>
        <span class="footer-right">
            <span class="status-dot" aria-hidden="true"></span>
            Tous les systèmes sont opérationnels
            <span class="footer-sep">·</span>
            Version {{ appVersion }}
        </span>
    </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ appVersion?: string }>()

const currentYear = computed(() => new Date().getFullYear())
const appVersion = computed(() => props.appVersion || '1.0.0')
</script>

<style scoped>
.footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
    padding: 18px 38px 20px;
    background: transparent;
    border-top: 1px solid #e6ebf3;
    color: #8a95a8;
    font-family: "DM Sans", system-ui, sans-serif;
    font-size: 10.5px;
}

.footer-left { color: #8a95a8; }

.footer-right {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
}

.status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #22a57a;
    box-shadow: 0 0 0 3px rgba(34, 165, 122, .18);
}

.footer-sep {
    padding: 0 4px;
    color: #c3cad5;
}

@media (max-width: 991.98px) {
    .footer { padding: 16px 24px; }
}

@media (max-width: 600px) {
    .footer {
        flex-direction: column;
        align-items: flex-start;
        gap: 8px;
        padding: 14px 14px 18px;
        font-size: 10px;
    }
}
</style>
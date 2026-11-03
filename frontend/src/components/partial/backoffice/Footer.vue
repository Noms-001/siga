<template>
    <footer class="footer">
        <!-- Identité institutionnelle -->
        <div class="footer-brand">
            <img src="/logo/bfm-logo.png" alt="Logo Banky Foiben’i Madagasikara" class="footer-logo" />

            <div class="footer-brand-copy">
                <strong class="bank-logo">Banky Foiben’i Madagasikara</strong>
                <span>Banque centrale de Madagascar</span>
            </div>
        </div>

        <!-- Rattachement administratif -->
        <div class="footer-department">
            <strong>Direction Technique, Sûreté et HSE</strong>
            <span>Département Études, Sûreté et Sécurité (TSS)</span>
            <span class="footer-app">Application interne GESTIA</span>
        </div>

        <!-- Informations techniques -->
        <div class="footer-meta">
            <span class="footer-copyright">
                © {{ currentYear }} GESTIA
            </span>

            <span class="footer-version">
                Version {{ appVersion }}
            </span>

            <a href="https://www.banky-foibe.mg/" target="_blank" rel="noopener noreferrer" class="footer-link"
                aria-label="Visiter le site officiel de BFM">
                Site officiel
                <span aria-hidden="true">↗</span>
            </a>
        </div>
    </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
    appVersion?: string
}>()

const currentYear = computed(() => new Date().getFullYear())
const appVersion = computed(() => props.appVersion || '1.0.0')
</script>

<style scoped>
.footer {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1.25fr) auto;
    align-items: center;
    gap: 20px;
    padding: 16px 38px;
    background: #fff;
    border-top: 1px solid #e6ebf3;
    color: #8a95a8;
    font-family: "DM Sans", system-ui, sans-serif;
    font-size: 10.5px;
}

/* Identité BFM */
.footer-brand {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
}

.footer-logo {
    width: 38px;
    height: 38px;
    flex: 0 0 38px;
    object-fit: contain;
}

.footer-brand-copy,
.footer-department {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
}

.footer-brand-copy strong,
.footer-department strong {
    color: #1a2a6c;
    font-size: 10.5px;
    font-weight: 700;
    line-height: 1.4;
}

.footer-brand-copy .bank-logo {
    color: #2577a7;
    text-transform: uppercase;
    font-weight: 900;
}

.footer-brand-copy span,
.footer-department span {
    color: #8a95a8;
    font-size: 10px;
    line-height: 1.5;
}

/* DTS / TSS */
.footer-department {
    padding-left: 20px;
    border-left: 1px solid #e6ebf3;
}

.footer-department .footer-app {
    color: #4a6cf7;
    font-weight: 600;
}

/* Version et lien */
.footer-meta {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 5px;
    white-space: nowrap;
}

.footer-copyright {
    color: #6b7280;
    font-weight: 600;
}

.footer-version {
    color: #8a95a8;
}

.footer-link {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    color: #4a6cf7;
    font-weight: 600;
    text-decoration: none;
    transition: color .15s ease;
}

.footer-link:hover {
    color: #1a2a6c;
    text-decoration: underline;
}

.footer-link:focus-visible {
    outline: 2px solid #d4af37;
    outline-offset: 3px;
    border-radius: 3px;
}

/* Tablettes */
@media (max-width: 1100px) {
    .footer {
        grid-template-columns: minmax(0, 1fr) minmax(0, 1.2fr);
        gap: 14px;
        padding: 16px 24px;
    }

    .footer-department {
        padding-left: 14px;
    }

    .footer-meta {
        grid-column: 1 / -1;
        flex-direction: row;
        align-items: center;
        justify-content: space-between;
        flex-wrap: wrap;
        padding-top: 10px;
        border-top: 1px solid #e6ebf3;
    }
}

/* Téléphones */
@media (max-width: 600px) {
    .footer {
        grid-template-columns: minmax(0, 1fr);
        gap: 14px;
        padding: 16px 14px 18px;
        font-size: 10px;
    }

    .footer-logo {
        width: 34px;
        height: 34px;
        flex-basis: 34px;
    }

    .footer-department {
        padding: 10px 0 0;
        border-left: none;
        border-top: 1px solid #e6ebf3;
    }

    .footer-meta {
        grid-column: auto;
        justify-content: flex-start;
        gap: 10px;
    }
}

@media (prefers-reduced-motion: reduce) {
    .footer-link {
        transition: none;
    }
}
</style>

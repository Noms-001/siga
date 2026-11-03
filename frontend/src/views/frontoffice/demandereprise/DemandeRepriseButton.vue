<!-- filepath: /home/nomena/Documents/Projet Stage/SAGA/frontend/src/views/frontoffice/demandereprise/DemandeRepriseButton.vue -->
<script setup lang="ts">
import { ref } from 'vue'
import { BaseButton, BaseModal } from '@/components/base'
import {
    creerDemandeRepriseActivite,
    creerDemandeReprisePlan,
} from '@/services/demandeReprise'

const props = defineProps<{
    kind: 'activite' | 'plan'
    id: number
    /** Cache le bouton si l'utilisateur est déjà le responsable actuel. */
    dejaResponsable?: boolean
}>()

const emit = defineEmits<{
    (e: 'cree'): void
}>()

const modalOpen = ref(false)
const motif = ref('')
const loading = ref(false)
const errorMessage = ref('')

function ouvrir(): void {
    motif.value = ''
    errorMessage.value = ''
    modalOpen.value = true
}

function fermer(): void {
    modalOpen.value = false
}

async function envoyer(): Promise<void> {
    loading.value = true
    errorMessage.value = ''

    const payload = { motif: motif.value.trim() || null }
    const res = props.kind === 'activite'
        ? await creerDemandeRepriseActivite(props.id, payload)
        : await creerDemandeReprisePlan(props.id, payload)

    loading.value = false

    if (!res.success) {
        errorMessage.value = res.error
        return
    }

    modalOpen.value = false
    emit('cree')
}
</script>

<template>
    <BaseButton v-if="!dejaResponsable" variant="secondary" icon="bi bi-arrow-left-right" @click="ouvrir">
        Demander la reprise
    </BaseButton>

    <BaseModal v-model="modalOpen" title="Demander la reprise" size="sm">
        <div class="reprise-modal__body">
            <p class="reprise-modal__text">
                Votre demande sera envoyée au responsable actuel et au chef de service.
                Elle devra être acceptée pour que vous deveniez responsable.
            </p>

            <div class="reprise-modal__field">
                <label for="motif-reprise" class="reprise-modal__label">
                    Motif (optionnel)
                </label>
                <textarea id="motif-reprise" v-model="motif" rows="3" class="reprise-modal__textarea"
                    placeholder="Expliquez pourquoi vous souhaitez reprendre…" />
            </div>

            <div v-if="errorMessage" class="reprise-modal__error" role="alert">
                <i class="bi bi-exclamation-triangle-fill"></i>
                {{ errorMessage }}
            </div>
        </div>

        <template #footer>
            <BaseButton variant="secondary" :disabled="loading" @click="fermer">
                Annuler
            </BaseButton>
            <BaseButton variant="primary" icon="bi bi-send" :loading="loading" :disabled="loading" @click="envoyer">
                Envoyer la demande
            </BaseButton>
        </template>
    </BaseModal>
</template>

<style scoped>
.reprise-modal__body {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.reprise-modal__text {
    margin: 0;
    color: #4a5568;
    font-size: 0.85rem;
    line-height: 1.5;
}

.reprise-modal__field {
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
}

.reprise-modal__label {
    color: #15233c;
    font-size: 0.78rem;
    font-weight: 600;
}

.reprise-modal__textarea {
    width: 100%;
    padding: 0.55rem 0.75rem;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    font: inherit;
    font-size: 0.85rem;
    resize: vertical;
}

.reprise-modal__textarea:focus {
    outline: none;
    border-color: #4a6cf7;
    box-shadow: 0 0 0 3px rgba(74, 108, 247, 0.1);
}

.reprise-modal__error {
    display: flex;
    gap: 0.5rem;
    padding: 0.6rem 0.85rem;
    border-radius: 8px;
    background: #fde9e9;
    color: #b03030;
    font-size: 0.82rem;
}
</style>
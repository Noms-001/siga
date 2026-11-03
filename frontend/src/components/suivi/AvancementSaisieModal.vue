<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { BaseButton, BaseInput } from '@/components/base'

const props = defineProps<{
    modelValue: boolean
    avancementActuel: number
}>()

const emit = defineEmits<{
    (e: 'update:modelValue', value: boolean): void
    (e: 'valide', payload: { valeurPourcentage: number; commentaire: string | null }): void
}>()

const valeur = ref<number>(props.avancementActuel)
const commentaire = ref('')
const erreur = ref('')

watch(() => props.modelValue, (ouvert) => {
    if (ouvert) {
        valeur.value = props.avancementActuel
        commentaire.value = ''
        erreur.value = ''
    }
})

const preview = computed(() => {
    const v = Number(valeur.value)
    if (isNaN(v) || v < 0 || v > 100) return null
    if (v === 0) return { code: 'NON_COMMENCEE', libelle: 'Non commencé', classe: 'badge-statut--non-commence' }
    if (v === 100) return { code: 'TERMINEE', libelle: 'Terminé', classe: 'badge-statut--termine' }
    return { code: 'EN_COURS', libelle: 'En cours', classe: 'badge-statut--en-cours' }
})

function fermer(): void {
    emit('update:modelValue', false)
}

function valider(): void {
    erreur.value = ''
    const v = Number(valeur.value)
    if (isNaN(v) || v < 0 || v > 100) {
        erreur.value = "L'avancement doit être compris entre 0 et 100."
        return
    }
    emit('valide', {
        valeurPourcentage: v,
        commentaire: commentaire.value.trim() || null,
    })
}
</script>

<template>
    <Teleport to="body">
        <div v-if="modelValue" class="modal-backdrop" role="dialog" aria-modal="true" @click.self="fermer">
            <div class="modal">
                <header class="modal__header">
                    <h2>Saisir un avancement</h2>
                    <button class="modal__close" type="button" @click="fermer" aria-label="Fermer">
                        <i class="bi bi-x-lg"></i>
                    </button>
                </header>

                <div class="modal__body">
                    <div class="modal__field">
                        <label class="modal__label">
                            Avancement (%)
                            <span class="modal__required">*</span>
                        </label>
                        <input v-model.number="valeur" type="number" min="0" max="100" step="1" class="modal__input" />
                        <div class="modal__slider">
                            <input v-model.number="valeur" type="range" min="0" max="100" step="1" />
                        </div>
                        <div v-if="preview" class="modal__preview">
                            Statut résultant :
                            <span :class="['badge-statut', preview.classe]">
                                {{ preview.libelle }}
                            </span>
                        </div>
                    </div>

                    <div class="modal__field">
                        <label class="modal__label">Commentaire</label>
                        <textarea v-model="commentaire" rows="3" placeholder="Décrivez ce qui a été réalisé…"
                            class="modal__textarea" />
                    </div>

                    <div v-if="erreur" class="modal__error">
                        <i class="bi bi-exclamation-triangle-fill"></i>
                        {{ erreur }}
                    </div>
                </div>

                <footer class="modal__footer">
                    <BaseButton variant="secondary" @click="fermer">Annuler</BaseButton>
                    <BaseButton variant="primary" icon="bi bi-check-lg" @click="valider">
                        Enregistrer
                    </BaseButton>
                </footer>
            </div>
        </div>
    </Teleport>
</template>

<style scoped>
.modal-backdrop {
    position: fixed;
    inset: 0;
    background: rgba(16, 24, 40, .55);
    backdrop-filter: blur(4px);
    display: grid;
    place-items: center;
    z-index: 1000;
    padding: 1rem;
}

.modal {
    background: #fff;
    border-radius: 16px;
    width: 100%;
    max-width: 480px;
    box-shadow: 0 20px 60px rgba(16, 24, 40, .3);
    display: flex;
    flex-direction: column;
    max-height: calc(100vh - 2rem);
    overflow: hidden;
}

.modal__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 1rem 1.25rem;
    border-bottom: 1px solid #e6ebf3;
}

.modal__header h2 {
    margin: 0;
    font: 700 1rem Manrope, sans-serif;
    color: #1a2a6c;
}

.modal__close {
    background: none;
    border: none;
    font-size: 1rem;
    color: #8a95a8;
    cursor: pointer;
    padding: 0.35rem;
    border-radius: 6px;
}

.modal__close:hover {
    background: #f5f7fa;
    color: #15233c;
}

.modal__body {
    padding: 1.25rem;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.modal__field {
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
}

.modal__label {
    font-size: 0.78rem;
    font-weight: 600;
    color: #15233c;
}

.modal__required {
    color: #d64545;
}

.modal__input,
.modal__textarea {
    width: 100%;
    padding: 0.55rem 0.75rem;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    font-size: 0.88rem;
    color: #15233c;
    font-family: inherit;
}

.modal__input:focus,
.modal__textarea:focus {
    outline: none;
    border-color: #4a6cf7;
    box-shadow: 0 0 0 3px rgba(74, 108, 247, .1);
}

.modal__textarea {
    resize: vertical;
    min-height: 60px;
}

.modal__slider {
    margin: 0.25rem 0;
}

.modal__slider input {
    width: 100%;
    accent-color: #4a6cf7;
}

.modal__preview {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.78rem;
    color: #6b7280;
    padding: 0.5rem 0.75rem;
    background: #f8fafc;
    border-radius: 6px;
}

.modal__error {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.6rem 0.85rem;
    background: #fde9e9;
    color: #b03030;
    border-radius: 8px;
    font-size: 0.82rem;
}

.modal__footer {
    display: flex;
    justify-content: flex-end;
    gap: 0.5rem;
    padding: 0.85rem 1.25rem;
    border-top: 1px solid #e6ebf3;
    background: #fafbfc;
}

.badge-statut {
    display: inline-block;
    font-size: 0.7rem;
    font-weight: 700;
    padding: 0.25rem 0.65rem;
    border-radius: 20px;
}

.badge-statut--non-commence {
    background: #eef1f5;
    color: #5b6b7d;
}

.badge-statut--en-cours {
    background: #e4f0fb;
    color: #135d95;
}

.badge-statut--termine {
    background: #e6f4ea;
    color: #1e7e34;
}
</style>
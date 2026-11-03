<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { BaseButton, BaseModal } from '@/components/base'
import {
    chargerDetailNotification,
    marquerNotificationLue,
    marquerToutesNotificationsLues,
    useNotifications,
    type NotificationDetail,
} from '@/services/notification'

const props = defineProps<{
    modelValue: boolean
    notificationId: number | null
}>()

const emit = defineEmits<{
    (e: 'update:modelValue', v: boolean): void
    (e: 'refresh'): void
}>()

const router = useRouter()
const state = useNotifications()

const detail = ref<NotificationDetail | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const marking = ref(false)
const markingAll = ref(false)

const ouvert = computed({
    get: () => props.modelValue,
    set: (v: boolean) => emit('update:modelValue', v),
})

/* Recharger à chaque ouverture ou changement d'id */
watch(
    () => [props.modelValue, props.notificationId] as const,
    async ([open, id]) => {
        if (!open || !id) {
            detail.value = null
            errorMessage.value = ''
            return
        }
        loading.value = true
        errorMessage.value = ''
        const rep = await chargerDetailNotification(id)
        loading.value = false
        if (!rep) {
            errorMessage.value = 'Impossible de charger le détail de cette notification.'
            return
        }
        detail.value = rep
    },
    { immediate: true },
)

async function marquerCouranteLue(): Promise<void> {
    if (!detail.value || detail.value.dateLecture) return
    marking.value = true
    const ok = await marquerNotificationLue(detail.value.id)
    marking.value = false
    if (ok) {
        detail.value.dateLecture = new Date().toISOString()
        emit('refresh')
    } else {
        errorMessage.value = 'Impossible de marquer cette notification comme lue.'
    }
}

async function toutMarquerLues(): Promise<void> {
    if (state.unreadCount === 0) return
    markingAll.value = true
    const ok = await marquerToutesNotificationsLues()
    markingAll.value = false
    if (ok) {
        if (detail.value && !detail.value.dateLecture) {
            detail.value.dateLecture = new Date().toISOString()
        }
        emit('refresh')
    } else {
        errorMessage.value = 'Impossible de marquer toutes les notifications comme lues.'
    }
}

function ouvrirEntite(): void {
    if (!detail.value?.urlFront) return
    ouvert.value = false
    void router.push(detail.value.urlFront)
}

function fermer(): void {
    ouvert.value = false
}

function classePriorite(code?: string | null): string {
    const c = (code ?? '').toUpperCase()
    if (c === 'CRITIQUE') return 'prio--critique'
    if (c === 'HAUTE') return 'prio--haute'
    if (c === 'NORMALE') return 'prio--normale'
    if (c === 'FAIBLE') return 'prio--faible'
    return 'prio--normale'
}

function libelleResource(r?: string | null): string {
    if (r === 'ACTIVITE') return 'Activité'
    if (r === 'SOUS_ACTIVITE') return 'Sous-activité'
    if (r === 'PLAN_ACTION') return "Plan d'action"
    if (r === 'INDICATEUR') return 'Indicateur'
    return 'Élément lié'
}

function formatDateHeure(iso?: string | null): string {
    if (!iso) return '—'
    const d = new Date(iso)
    if (isNaN(d.getTime())) return '—'
    return d.toLocaleString('fr-FR', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
    })
}
</script>

<template>
    <BaseModal v-model="ouvert" title="Détail de la notification" size="md" :closeOnOutside="!marking && !markingAll">
        <!-- Erreur -->
        <div v-if="errorMessage" class="detail-modal__error" role="alert">
            {{ errorMessage }}
        </div>

        <!-- Chargement -->
        <div v-if="loading" class="detail-modal__loading">
            <div class="spinner-border text-primary" role="status"></div>
            <p>Chargement…</p>
        </div>

        <!-- Contenu -->
        <div v-else-if="detail" class="detail-modal">
            <!-- En-tête : priorité + titre -->
            <header class="detail-modal__header">
                <span v-if="detail.priorite?.libelle" class="badge-prio" :class="classePriorite(detail.priorite.code)">
                    {{ detail.priorite.libelle }}
                </span>
                <h3 class="detail-modal__title">{{ detail.titre }}</h3>
                <span v-if="!detail.dateLecture" class="detail-modal__dot" aria-label="Non lue"></span>
            </header>

            <!-- Message -->
            <p class="detail-modal__message">{{ detail.message }}</p>

            <!-- Métadonnées -->
            <div class="detail-modal__meta">
                <span class="meta-item">
                    <i class="bi bi-clock"></i>
                    Créée le {{ formatDateHeure(detail.dateCreation) }}
                </span>
                <span v-if="detail.dateLecture" class="meta-item meta-item--read">
                    <i class="bi bi-check2-all"></i>
                    Lue le {{ formatDateHeure(detail.dateLecture) }}
                </span>
                <span v-if="detail.utilisateur" class="meta-item">
                    <i class="bi bi-person"></i>
                    Destinataire : {{ detail.utilisateur.prenom }} {{ detail.utilisateur.nom }}
                </span>
            </div>

            <!-- Entité concernée -->
            <section v-if="detail.resource" class="detail-modal__section">
                <h4 class="detail-modal__section-title">
                    <i class="bi bi-box-arrow-up-right"></i>
                    {{ libelleResource(detail.resource) }}
                </h4>

                <template v-if="detail.entiteDisponible">
                    <p class="detail-modal__reference">
                        {{ detail.libelleReference }}
                    </p>

                    <dl v-if="Object.keys(detail.contexte ?? {}).length > 0" class="detail-modal__contexte">
                        <div v-for="(valeur, libelle) in detail.contexte" :key="libelle" class="contexte-row">
                            <dt>{{ libelle }}</dt>
                            <dd>{{ valeur }}</dd>
                        </div>
                    </dl>

                    <BaseButton v-if="detail.urlFront" variant="primary" size="sm" class="detail-modal__link-btn"
                        @click="ouvrirEntite">
                        <i class="bi bi-arrow-right-circle"></i>
                        Voir la page
                    </BaseButton>
                </template>

                <p v-else class="detail-modal__absent">
                    <i class="bi bi-exclamation-triangle"></i>
                    L'élément concerné a été supprimé ou n'est plus accessible.
                </p>
            </section>

            <!-- Actions -->
            <div class="detail-modal__actions">
                <BaseButton v-if="!detail.dateLecture" variant="secondary" size="sm" :loading="marking"
                    :disabled="marking" @click="marquerCouranteLue">
                    <i class="bi bi-check2"></i>
                    Marquer comme lue
                </BaseButton>

                <BaseButton v-if="state.unreadCount > 0" variant="secondary" size="sm" :loading="markingAll"
                    :disabled="markingAll" @click="toutMarquerLues">
                    <i class="bi bi-check2-all"></i>
                    Tout marquer comme lu ({{ state.unreadCount }})
                </BaseButton>
            </div>
        </div>

        <template #footer>
            <BaseButton variant="secondary" :disabled="marking || markingAll" @click="fermer">
                Fermer
            </BaseButton>
        </template>
    </BaseModal>
</template>

<style scoped>
.detail-modal {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.detail-modal__header {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    flex-wrap: wrap;
    position: relative;
}

.detail-modal__title {
    margin: 0;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 1.05rem;
    font-weight: 700;
    color: #1a2a6c;
    line-height: 1.35;
    flex: 1;
    min-width: 0;
}

.detail-modal__dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #4a6cf7;
    box-shadow: 0 0 0 3px rgba(74, 108, 247, .15);
    flex: none;
}

.detail-modal__message {
    margin: 0;
    font-size: 0.9rem;
    line-height: 1.6;
    color: #384a5e;
    padding: 0.75rem 0.9rem;
    background: #f8fafc;
    border-left: 3px solid #e6ebf3;
    border-radius: 6px;
    overflow-wrap: anywhere;
}

.detail-modal__meta {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
    font-size: 0.78rem;
    color: #6b7280;
}

.meta-item {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
}

.meta-item i {
    color: #8a95a8;
    font-size: 0.85rem;
}

.meta-item--read i {
    color: #4ad8a7;
}

.detail-modal__section {
    padding: 0.9rem 1rem;
    background: #f8fafc;
    border: 1px solid #e6ebf3;
    border-radius: 10px;
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
}

.detail-modal__section-title {
    margin: 0;
    font-size: 0.72rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: #6b7280;
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
}

.detail-modal__reference {
    margin: 0;
    font-size: 0.9rem;
    font-weight: 700;
    color: #1a2a6c;
}

.detail-modal__contexte {
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
}

.contexte-row {
    display: flex;
    justify-content: space-between;
    gap: 1rem;
    font-size: 0.82rem;
    padding: 0.25rem 0;
    border-bottom: 1px dashed #e6ebf3;
}

.contexte-row:last-child {
    border-bottom: none;
}

.contexte-row dt {
    color: #8a95a8;
    font-weight: 600;
}

.contexte-row dd {
    margin: 0;
    color: #15233c;
    font-weight: 600;
    text-align: right;
}

.detail-modal__link-btn {
    align-self: flex-start;
}

.detail-modal__absent {
    margin: 0;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.82rem;
    color: #a61b16;
    background: #fdecea;
    padding: 0.55rem 0.7rem;
    border-radius: 6px;
}

.detail-modal__absent i {
    color: #d64545;
}

.detail-modal__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    padding-top: 0.5rem;
    border-top: 1px solid #eef1f5;
}

.detail-modal__loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 2.5rem 1rem;
    gap: 0.75rem;
    color: #6b7280;
    font-size: 0.85rem;
}

.detail-modal__error {
    padding: 0.65rem 0.85rem;
    border-radius: 8px;
    background: #fdecea;
    color: #a61b16;
    font-size: 0.82rem;
    border: 1px solid #f6cfcc;
}

/* Badges priorité */
.badge-prio {
    display: inline-flex;
    padding: 0.18rem 0.55rem;
    border-radius: 20px;
    font-size: 0.68rem;
    font-weight: 700;
    letter-spacing: 0.03em;
    text-transform: uppercase;
    white-space: nowrap;
}

.prio--critique {
    background: #fee2e2;
    color: #b42318;
    border: 1px solid #f6cfcc;
}

.prio--haute {
    background: #ffedd5;
    color: #c2410c;
    border: 1px solid #fbd7a9;
}

.prio--normale {
    background: #dbeafe;
    color: #1d4ed8;
    border: 1px solid #cde2f6;
}

.prio--faible {
    background: #dcfce7;
    color: #15803d;
    border: 1px solid #c8e5cf;
}
</style>
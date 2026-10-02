<script setup lang="ts">
/**
 * Modale de confirmation, posee avant toute action.
 *
 * CE QUE CE COMPOSANT EST
 * Une demande de confirmation : il affiche une demande, et rend deux reponses.
 * Il ne sait rien de l'action -- il ne l'execute pas. C'est la page qui
 * confirme, parce qu'elle seule sait ce que l'action doit faire ensuite
 * (retirer une card, recharger, ouvrir un formulaire).
 *
 * CE QUE CE COMPOSANT N'EST PAS
 * - Un garde-fou de securite. Le vrai controle est cote backend : une
 *   confirmation cote navigateur n'empeche rien, puisque l'appel part d'une
 *   page ouverte dans un autre onglet. Elle sert a ne pas faire une action par
 *   megarde, ce qui est une question d'interface et non de droit.
 * - Un modal d'attente. Il n'a pas d'action a suivre, donc il ne rend aucun
 *   resultat : il emet "confirme", et la page fait le reste.
 *
 * POURQUOI UNE SEULE INSTANCE PAR PAGE
 * Les demandes se suivent -- une confirmation d'ajout puis une de
 * suppression -- et deux modales superposees rendraient le focus et la
 * hierarchie des messages illisibles. Une instance unique, dont seul le contenu
 * change, evite d'avoir a empiler.
 */
import { computed, watch } from 'vue'

import BaseButton from '../BaseButton/BaseButton.vue'
import BaseModal from '../BaseModal/BaseModal.vue'
import {
    ICONE_PAR_TON,
    VARIANTE_PAR_TON,
    baseConfirmProps,
} from './BaseConfirm.types'

const props = defineProps(baseConfirmProps)

const emit = defineEmits<{
    (e: 'update:modelValue', value: boolean): void
    (e: 'confirme'): void
    (e: 'annule'): void
}>()

/**
 * L'icone vient de la demande si elle en nomme une, sinon de son ton.
 *
 * Sans ce repli, une demande sans icone n'afficherait rien, et le vide a cote
 * du titre ferait passer une confirmation pour une modale de chargement.
 */
const icone = computed(
    () => props.demande?.icone ?? (props.demande ? ICONE_PAR_TON[props.demande.ton] : '')
)

const variante = computed(() =>
    props.demande ? VARIANTE_PAR_TON[props.demande.ton] : 'primary'
)

const libelleAnnuler = computed(() => props.demande?.libelleAnnuler ?? 'Annuler')

/**
 * Fermeture sans confirmation.
 *
 * La modale se ferme sur annulation, sur Echap et sur le bouton de fermeture
 * de BaseModal : ces trois gestes veulent dire la meme chose. Le clic dehors
 * est desactive, lui, parce qu'une confirmation posee dans un coin d'ecran
 * disparaitrait d'un simple effleurement de souris.
 */
function fermer(): void {
    if (props.loading) {
        return
    }

    emit('update:modelValue', false)
    emit('annule')
}

function confirmer(): void {
    if (props.loading) {
        return
    }

    // Elle ne se ferme pas ici : la page peut avoir a repondre, et une modale
    // fermee pendant que l'appel est en vol donnerait l'impression que rien
    // n'a ete fait. Elle ferme sur le succes, cote page.
    emit('confirme')
}

watch(
    () => props.modelValue,
    (ouvert) => {
        if (!ouvert) {
            emit('annule')
        }
    }
)
</script>

<template>
    <BaseModal
        :modelValue="modelValue"
        :title="demande?.titre ?? 'Confirmer'"
        size="sm"
        :closeOnOutside="false"
        :loading="loading"
        custom-class="confirmation"
        @update:modelValue="(v: boolean) => !v && fermer()"
    >
        <div v-if="demande" class="confirmation__corps">
            <i
                class="confirmation__icone"
                :class="[icone, `confirmation__icone--${demande.ton}`]"
                aria-hidden="true"
            ></i>

            <p class="confirmation__message">{{ demande.message }}</p>

            <p
                v-if="demande.consequence"
                class="confirmation__consequence"
                role="note"
            >
                {{ demande.consequence }}
            </p>
        </div>

        <template #footer>
            <div class="confirmation__pied">
                <BaseButton variant="secondary" :disabled="loading" @click="fermer">
                    {{ libelleAnnuler }}
                </BaseButton>

                <BaseButton
                    :variant="variante"
                    :loading="loading"
                    @click="confirmer"
                >
                    {{ demande?.libelleConfirmer ?? 'Confirmer' }}
                </BaseButton>
            </div>
        </template>
    </BaseModal>
</template>

<style scoped>
.confirmation__corps {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.confirmation__icone {
    font-size: 1.6rem;
    line-height: 1;
}

.confirmation__icone--danger {
    color: var(--danger-color, #d64545);
}

.confirmation__icone--warning {
    color: var(--warning-color, #d98324);
}

.confirmation__icone--primary {
    color: var(--primary-color, #1e88a8);
}

.confirmation__message {
    margin: 0;
    color: #1a2b3c;
    line-height: 1.5;
}

/*
    La consequence est separee du message, en plus petit et en gras : c'est la
    partie qui doit etre lue avant de cliquer, et la perdre dans le paragraphe
    la rendrait invisible.
*/
.confirmation__consequence {
    margin: 0;
    padding: 0.6rem 0.75rem;
    background: #f6f8fa;
    border-left: 3px solid #dde5ec;
    border-radius: 4px;
    font-size: 0.82rem;
    color: #526579;
}

.confirmation__pied {
    display: flex;
    justify-content: flex-end;
    gap: 0.5rem;
    width: 100%;
}
</style>

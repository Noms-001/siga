<script setup lang="ts">
/**
 * Modal d'ajout de fichiers a un livrable deja declare.
 *
 * CE QU'IL FAIT, ET CE QU'IL NE FAIT PAS
 *
 * Il ne cree pas de livrable : le livrable existe deja, son identifiant est
 * passe en propriete, et l'icone qui ouvre ce modal est sur sa ligne. Ajouter
 * un livrable et y joindre des fichiers sont deux gestes distincts, et les
 * confondre ferait perdre le libelle saisi.
 *
 * Il ne decide pas si le depot est possible. La page n'ouvre ce modal que sur
 * une activite en brouillon ou en cours, et le serveur revérifie sous verrou.
 * La regle d'acces n'est donc ecrite qu'a un endroit qui fait autorite.
 *
 * CONTRAIREMENT A LA CREATION, LE DEPOT EST OBLIGATOIRE ICI
 *
 * Ajouter des fichiers et ne rien choisir n'ecrirait rien, tout en renvoyant
 * un succes. Le bouton de validation est donc desactive tant que la liste est
 * vide : le geste doit avoir un effet, sinon il n'a pas de sens de le permettre.
 */
import { computed, ref, watch } from 'vue'

import { BaseButton, BaseConfirm, BaseInput, BaseModal } from '@/components/base'
import { useConfirmation } from '@/composables/useConfirmation'
import * as activiteService from '@/services/activite'
import type { LivrableDetail } from '@/types/activite'

const props = defineProps<{
    modelValue: boolean
    idActivite: number
    idSousActivite: number
    idLivrable: number
    /** Libelle du livrable, pour que l'utilisateur sache ce qu'il complete. */
    designationLivrable: string | null
    /**
     * Nombre de fichiers deja deposes sur ce livrable.
     *
     * Passe par le parent plutot que redemande au serveur : il figure deja
     * dans le detail affiche, et le server ne peut pas en dire plus sans un
     * aller-retour. Sert a ne pas laisser choisir un fichier que le total
     * porterait au-dela du plafond.
     */
    fichiersExistants: number
}>()

const emit = defineEmits<{
    (e: 'update:modelValue', valeur: boolean): void
    /** Le livrable, relu avec ses fichiers, pour un affichage sans rechargement. */
    (e: 'ajoute', livrable: LivrableDetail): void
}>()

/**
 * Plafonds repris de la configuration serveur.
 *
 * Comme dans LivrableModal : ils evitent d envoyer un fichier que le serveur
 * refuserait apres coup, et ne remplacent pas son controle.
 */
const TAILLE_MAX = 10 * 1024 * 1024
const NOMBRE_MAX = 10

/**
 * Combien de fichiers peuvent ENCORE etre ajoutes.
 *
 * Le plafond porte sur le total du livrable, pas sur chaque envoi : c'est ce
 * que fait le serveur (LivrableEcritureService.deposerFichiers, qui compte les
 * depots anterieurs). Compter seulement la selection ici autoriserait a choisir
 * dix fichiers de plus sur un livrable deja complet, et le refus n'arriverait
 * qu'apres l'envoi.
 */
const placesRestantes = computed(() =>
    Math.max(0, NOMBRE_MAX - props.fichiersExistants)
)

/** Ce qui partira. Liste vide = rien a envoyer, et le bouton est desactive. */
const fichiers = ref<File[]>([])

/**
 * Selection en cours, portee par BaseInput.
 *
 * BaseInput remplace sa valeur a chaque choix et ne peut donc pas porter la
 * liste finale : deux choix successifs s'y ecraseraient. Il sert a choisir,
 * la liste ci-dessus garde le resultat, et sa valeur est videe aussitot.
 */
const selection = ref<File[]>([])

const enregistrement = ref(false)
const erreur = ref<string | null>(null)
const avertissement = ref<string | null>(null)

/** Remise a zero a chaque ouverture, pour ne pas heriter d un essai anterieur. */
watch(
    () => props.modelValue,
    (ouvert) => {
        if (!ouvert) return

        fichiers.value = []
        selection.value = []
        avertissement.value = null
        erreur.value = null
    }
)

const totalOctets = computed(() =>
    fichiers.value.reduce((total, f) => total + f.size, 0)
)

function formaterTaille(octets: number): string {
    if (octets < 1024) return `${octets} o`
    if (octets < 1024 * 1024) return `${(octets / 1024).toFixed(1)} Ko`
    return `${(octets / (1024 * 1024)).toFixed(1)} Mo`
}

/**
 * Fichiers choisis, ajoutes au lot.
 *
 * Un fichier trop lourd, ou au-dela des places restantes, est ecarte et dit.
 * Il ne part pas "quand meme" : le serveur le refuserait, et tout l envoi
 * echouerait pour un fichier que l'utilisateur n'attendait pas voir bloque. Le
 * retirer sans rien dire serait pire -- il partirait sans lui, et
 * l'utilisateur croirait l'avoir depose.
 *
 * L'avertissement decrit l'etat de la selection, pas un evenement : il est
 * efface par le prochain choix.
 */
function surSelectionFichiers(evenement: unknown): void {
    const choisis = (Array.isArray(evenement)
        ? evenement
        : evenement instanceof File
            ? [evenement]
            : []) as File[]

    selection.value = []

    const retenus: File[] = []
    const ecartes: string[] = []

    for (const fichier of choisis) {
        if (fichiers.value.length + retenus.length >= placesRestantes.value) {
            ecartes.push(`${fichier.name} (trop de fichiers)`)
            continue
        }

        if (fichier.size > TAILLE_MAX) {
            ecartes.push(`${fichier.name} (${formaterTaille(fichier.size)})`)
            continue
        }

        retenus.push(fichier)
    }

    if (retenus.length > 0) {
        fichiers.value = [...fichiers.value, ...retenus]
    }

    avertissement.value = ecartes.length > 0
        ? `Ignoré : ${ecartes.join(', ')}. Limite : ${NOMBRE_MAX} fichiers de 10 Mo.`
        : null
}

function retirerFichier(index: number): void {
    fichiers.value.splice(index, 1)
}

function viderFichiers(): void {
    fichiers.value = []
}

function fermer(): void {
    if (enregistrement.value) {
        return
    }

    emit('update:modelValue', false)
}

const confirmation = useConfirmation()

/**
 * Deposer les fichiers choisis, apres confirmation.
 *
 * UNE CONFIRMATION DANS UNE MODALE, ce qui est a double tour. Elle s'y impose
 * malgre tout parce que le depot fige la liste des fichiers du livrable et
 * qu'aucune annulation n'existe cote serveur une fois l'appel parti. Le nom du
 * livrable et le nombre de fichiers y sont repris : c'est ce qui permet de
 * verifier d'un coup d'oeil qu'on ne depose pas au mauvais endroit.
 */
function soumettre(): void {
    erreur.value = null

    // Defense cote client d'un envoi vide, que le serveur refuse de toute
    // facon : le retour rapide evite d'attendre une reponse pour rien.
    if (fichiers.value.length === 0) {
        erreur.value = 'Sélectionnez au moins un fichier à joindre.'
        return
    }

    const nom = props.designationLivrable || 'sans intitulé'
    const nombre = fichiers.value.length

    confirmation.demander({
        titre: 'Ajouter au livrable',
        message: `${nombre} fichier(s) seront ajoutés au livrable ${nom}.`,
        consequence: 'Les fichiers déjà présents sur ce livrable ne seront pas modifiés.',
        libelleConfirmer: 'Ajouter',
        ton: 'primary',
        icone: 'bi bi-file-earmark-arrow-up',
        action: deposer,
    })
}

async function deposer(): Promise<void> {
    enregistrement.value = true

    const reponse = await activiteService.ajouterFichiersLivrable(
        props.idActivite,
        props.idSousActivite,
        props.idLivrable,
        fichiers.value
    )

    enregistrement.value = false

    if (!reponse.success) {
        erreur.value = reponse.error
        return
    }

    emit('ajoute', reponse.data)
    emit('update:modelValue', false)
}
</script>

<template>
    <!--
        ============ CONFIRMATION ============

        Rendue hors de BaseModal et non dans son corps : BaseModal ferme ce qui
        passe par son slot a la fermeture, et la confirmation doit survivre au
        retour pour que l'utilisateur puisse se tromper de bouton.
    -->
    <BaseConfirm
        v-model="confirmation.ouvert.value"
        :demande="confirmation.demande.value"
        :loading="confirmation.enCours.value || enregistrement"
        @confirme="confirmation.confirmer"
        @annule="confirmation.annuler"
    />

    <!--
        closeOnOutside desactive : un clic a cote pendant la selection ferait
        perdre le choix en cours. Le pied annule explicitement.
    -->
    <BaseModal
        :modelValue="modelValue"
        title="Ajouter des fichiers au livrable"
        size="lg"
        :closeOnOutside="false"
        @update:modelValue="emit('update:modelValue', $event)"
    >
        <div class="fichiers-modal">
            <p class="helper-text mb-0">
                Livrable : {{ designationLivrable ?? 'sans intitulé' }}
            </p>

            <p v-if="placesRestantes === 0" class="fichiers-modal__avertissement">
                Ce livrable porte déjà ses {{ NOMBRE_MAX }} fichiers.
                Le dépôt sera refusé par le serveur.
            </p>

            <!--
                Meme composant que dans LivrableModal : BaseInput gere le
                glisser-deposer et le selecteur, et ne porte que la selection.
            -->
            <BaseInput
                :modelValue="selection"
                type="file"
                multiple
                :label="`Fichiers (${placesRestantes} place(s) restante(s))`"
                placeholder="Glissez-déposez des fichiers ou cliquez ici"
                @update:modelValue="surSelectionFichiers"
            />

            <p v-if="avertissement" class="fichiers-modal__avertissement">
                {{ avertissement }}
            </p>

            <ul v-if="fichiers.length" class="liste-fichiers">
                <li v-for="(fichier, index) in fichiers" :key="`${fichier.name}-${index}`">
                    <i class="bi bi-paperclip"></i>

                    <span class="liste-fichiers__nom">{{ fichier.name }}</span>

                    <span class="liste-fichiers__taille">{{ formaterTaille(fichier.size) }}</span>

                    <BaseButton
                        size="sm"
                        variant="secondary"
                        title="Retirer ce fichier"
                        :aria-label="`Retirer ${fichier.name}`"
                        @click="retirerFichier(index)"
                    >
                        <i class="bi bi-x-lg"></i>
                    </BaseButton>
                </li>

                <li class="liste-fichiers__pied">
                    <span class="helper-text mb-0">
                        {{ fichiers.length }} fichier(s), {{ formaterTaille(totalOctets) }}
                    </span>

                    <BaseButton size="sm" variant="secondary" @click="viderFichiers">
                        Tout retirer
                    </BaseButton>
                </li>
            </ul>

            <p v-else class="helper-text mb-0">
                Aucun fichier choisi. Choisissez-en au moins un pour continuer.
            </p>

            <p v-if="erreur" class="fichiers-modal__erreur">{{ erreur }}</p>
        </div>

        <template #footer>
            <BaseButton variant="secondary" :disabled="enregistrement" @click="fermer">
                Annuler
            </BaseButton>

            <BaseButton
                variant="primary"
                :loading="enregistrement"
                :disabled="fichiers.length === 0"
                @click="soumettre"
            >
                Joindre les fichiers
            </BaseButton>
        </template>
    </BaseModal>
</template>

<style scoped>
.fichiers-modal {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.fichiers-modal__erreur {
    margin: 0;
    font-size: 0.8125rem;
    color: var(--bs-danger, #dc3545);
}

.fichiers-modal__avertissement {
    margin: 0;
    font-size: 0.8125rem;
    color: var(--bs-warning-text-emphasis, #997404);
}

.liste-fichiers {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
}

.liste-fichiers li {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.35rem 0.5rem;
    border: 1px solid var(--bs-border-color, #dee2e6);
    border-radius: 0.375rem;
    font-size: 0.8125rem;
}

.liste-fichiers__nom {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.liste-fichiers__taille {
    color: var(--bs-secondary-color, #6c757d);
    font-size: 0.75rem;
}

.liste-fichiers__pied {
    justify-content: space-between;
    border: none;
    padding-top: 0;
}
</style>

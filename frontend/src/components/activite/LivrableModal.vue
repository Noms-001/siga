<script setup lang="ts">
/**
 * Modal d'ajout d'un livrable sur une sous-activite.
 *
 * Ce qu'il ne fait PAS, volontairement
 *
 * Il ne decide pas si le depot est possible. La page n'ouvre ce modal que si
 * le detail a renvoyé statutActivite = EN_COURS, et le serveur revérifie sous
 * verrou. Une regle d'accès réécrite ici serait une troisieme version de la
 * meme regle, et la seule qui fait autorite est celle du serveur.
 *
 * IL NE DEMANDE PAS DE FICHIER
 *
 * Les fichiers sont facultatifs, et c'est un choix de fond : un livrable peut
 * etre declare avant que la piece soit prete. La section Livrables affiche
 * alors « Aucun fichier depose », un etat deja prevu par la liste. Exiger un
 * depot ici interdirait d'annoncer ce qui est attendu.
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
    /** Libelle de la sous-activite, repris dans l aide pour situer le depot. */
    designationSousActivite?: string
}>()

const emit = defineEmits<{
    (e: 'update:modelValue', valeur: boolean): void
    /** Le livrable cree, avec ses fichiers, pour un affichage sans rechargement. */
    (e: 'ajoute', livrable: LivrableDetail): void
}>()

/**
 * Plafonds repris de la configuration serveur.
 *
 * Ils sont repetes ici pour ne pas laisser choisir un fichier que le serveur
 * refusera apres l avoir envoye. Le serveur reste l'arbitre : un message
 * annonce d'avance evite l'attente pour rien, il ne remplace pas le controle.
 */
const TAILLE_MAX = 10 * 1024 * 1024
const NOMBRE_MAX = 10

const form = ref({ designation: '', description: '' })

/** Ce qui partira avec le livrable. Liste vide = aucun depot. */
const fichiers = ref<File[]>([])

/**
 * Selection en cours, portee par BaseInput.
 *
 * BaseInput remplace sa valeur a chaque choix et affiche un apercu de ce qu il
 * contient. Il ne peut donc pas porter la liste finale : deux choix successifs
 * s y ecriraient. Il sert a choisir, la liste ci-dessus garde le resultat, et
 * sa valeur est videe aussitot -- sans quoi l utilisateur verrait deux listes
 * de fichiers, l une fugitive et l autre seule reelle.
 */
const selection = ref<File[]>([])

const enregistrement = ref(false)
const erreur = ref<string | null>(null)
const erreurs = ref({ designation: '', description: '' })
const avertissement = ref<string | null>(null)

/** Remise a zero a chaque ouverture, pour ne pas heriter d un essai anterieur. */
watch(
    () => props.modelValue,
    (ouvert) => {
        if (!ouvert) return

        form.value = { designation: '', description: '' }
        fichiers.value = []
        selection.value = []
        avertissement.value = null
        erreur.value = null
        erreurs.value = { designation: '', description: '' }
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
 * CE QUE CETTE FONCTION REFUSE, ET POURQUOI
 *
 * Un fichier trop lourd ou au-dela du nombre maximum est ecarte, et dit. Il ne
 * va pas dans la liste "a partir de laquelle l'envoi part" : le serveur le
 * refuserait, et un depot entier echouerait pour un fichier que l'utilisateur
 * n'attendait pas voir bloquer. Le retirer silencieusement serait pire -- il
 * partirait sans lui, et l'utilisateur croirait l'avoir depose.
 *
 * L'avertissement est cumulatif et efface par le prochain choix : il decrit
 * l'etat de la selection, pas un evenement.
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
        if (fichiers.value.length + retenus.length >= NOMBRE_MAX) {
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
 * Creer le livrable, apres confirmation.
 *
 * Comme pour le depot de fichiers, la creation est posee devant une modale :
 * elle ecrit une ligne dans le livrable de la sous-activite et son contenu ne
 * peut plus etre rattrape apres coup. Le message rappelle aussi la sous-activite
 * visee, car c'est l'information la plus facile a confondre d'un onglet a
 * l'autre.
 */
function soumettre(): void {
    erreurs.value = { designation: '', description: '' }
    erreur.value = null

    // Memes limites que les colonnes, verifiees ici pour etre annoncees dans le
    // champ concerne plutot qu'en un refus global du serveur.
    if (!form.value.designation.trim()) {
        erreurs.value.designation = 'La designation est obligatoire'
    } else if (form.value.designation.trim().length > 250) {
        erreurs.value.designation = 'La designation ne peut pas depasser 250 caracteres'
    }

    if (erreurs.value.designation) {
        return
    }

    const designation = form.value.designation.trim()
    const nombreFichiers = fichiers.value.length

    confirmation.demander({
        titre: 'Créer le livrable',
        message: `Le livrable ${designation} sera ajouté${props.designationSousActivite ? ` à ${props.designationSousActivite}` : ''}.`,
        consequence: nombreFichiers
            ? `${nombreFichiers} fichier(s) seront joints au livrable dès sa création.`
            : undefined,
        libelleConfirmer: 'Créer',
        ton: 'primary',
        icone: 'bi bi-folder-plus',
        action: creer,
    })
}

async function creer(): Promise<void> {
    enregistrement.value = true

    const reponse = await activiteService.creerLivrable(
        props.idActivite,
        props.idSousActivite,
        {
            designation: form.value.designation.trim(),
            description: form.value.description.trim() || null,
        },
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

        Hors de BaseModal : elle doit survivre au retour, pour que l'utilisateur
        puisse se tromper de bouton sans avoir a resaisir le livrable.
    -->
    <BaseConfirm
        v-model="confirmation.ouvert.value"
        :demande="confirmation.demande.value"
        :loading="confirmation.enCours.value || enregistrement"
        @confirme="confirmation.confirmer"
        @annule="confirmation.annuler"
    />

    <!--
        closeOnOutside est desactive : un clic a cote pendant la saisie ferait
        perdre ce qui vient d etre saisi. Le pied annule explicitement.
    -->
    <BaseModal
        :modelValue="modelValue"
        title="Ajouter un livrable"
        size="lg"
        :closeOnOutside="false"
        @update:modelValue="emit('update:modelValue', $event)"
    >
        <div class="livrable-modal">
            <p v-if="designationSousActivite" class="helper-text mb-0">
                Sous-activite : {{ designationSousActivite }}
            </p>

            <BaseInput
                v-model="form.designation"
                label="Designation du livrable (250 caracteres max)"
                required
                :error="erreurs.designation"
                placeholder="Rapport d'audit"
            />

            <BaseInput
                v-model="form.description"
                label="Description"
                type="textarea"
                :error="erreurs.description"
                placeholder="Ce que contient le livrable, son perimetre"
            />

            <!--
                BaseInput gere deja le glisser-deposer et le selecteur, avec le
                meme aspect que le reste du formulaire. Il ne porte que la
                selection : sa valeur est videe des la reception, la liste
                ci-dessous etant celle qui partira.
            -->
            <BaseInput
                :modelValue="selection"
                type="file"
                multiple
                label="Fichiers (facultatif)"
                placeholder="Glissez-deposez des fichiers ou cliquez ici"
                @update:modelValue="surSelectionFichiers"
            />

            <p v-if="avertissement" class="livrable-modal__avertissement">
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

            <p v-if="fichiers.length === 0" class="helper-text mb-0">
                Aucun fichier joint. Le livrable sera cree sans depot.
            </p>

            <p v-if="erreur" class="livrable-modal__erreur">{{ erreur }}</p>
        </div>

        <template #footer>
            <BaseButton variant="secondary" :disabled="enregistrement" @click="fermer">
                Annuler
            </BaseButton>

            <BaseButton variant="primary" :loading="enregistrement" @click="soumettre">
                Ajouter le livrable
            </BaseButton>
        </template>
    </BaseModal>
</template>

<style scoped>
.livrable-modal {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.livrable-modal__erreur {
    margin: 0;
    font-size: 0.8125rem;
    color: var(--bs-danger, #dc3545);
}

.livrable-modal__avertissement {
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

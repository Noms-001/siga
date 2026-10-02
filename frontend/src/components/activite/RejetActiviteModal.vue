<script setup lang="ts">
/**
 * Les deux issues possibles d'un rejet.
 *
 * Ni EN_ATTENTE_VALIDATION -- ce n'est pas une decision -- ni VALIDE -- ce n'est
 * pas un rejet. Les exclure de la table evite d'y ecrire des libelles qui
 * n'auraient aucun sens, et la fonction ne peut donc pas en demander un autre.
 */
type IssueRejet = 'REJETE' | 'RETOUR_MODIFICATION'

/**
 * Choix du sort d'une activite soumise, et son motif.
 *
 * CE QUE CE MODAL FAIT, ET CE QU'IL NE FAIT PAS
 *
 * Il ne decide pas. Il ne fait que recueillir deux choses : ce qu'on veut faire
 * de l'activite, et pourquoi. La demande part ensuite a la page, qui appelle le
 * backend. Le modal ne connait donc ni les regles du circuit, ni l'etat
 * courant de l'activite : une regle ecrite ici serait un second endroit ou la
 * changer, et le navigateur n'est pas un endroit ou verifier une regle metier.
 *
 * LE REJET A DEUX ISSUES, ET ELLES NE SE VALENT PAS
 *
 * Un rejet definitif sort l'activite du circuit : elle ne sera ni publiee, ni
 * reprise. Un retour en modification la rend a son auteur, qui peut la
 * corriger et la resoumettre. Les confondre serait grave -- l'utilisateur croit
 * avoir renvoye une activite corrigeable, et elle est en fait close. D'ou un
 * choix explicite plutot qu'un bouton "Rejeter", et deux libelles qui disent ce
 * qui se passe ensuite.
 *
 * LE MOTIF EST OBLIGATOIRE ICI, ET VERIFIE LA-BAS
 *
 * Un refus sans motif n'est pas opposable a l'auteur : il ne sait pas quoi
 * corriger. Le bouton reste donc desactive tant qu'il n'y a rien, et le backend
 * reclame le commentaire a son tour -- parce qu'un client peut toujours
 * contourner un bouton desactive.
 */
import { computed, ref, watch } from 'vue'

import { BaseButton, BaseInput, BaseModal } from '@/components/base'
const props = defineProps<{
    modelValue: boolean
    /**
     * Nombre d'activites concernees.
     *
     * Le libelle suit ce nombre : "rejeter" et "rejeter ces trois activites"
     * n'annoncent pas la meme consequence, et l'utilisateur doit savoir avant
     * de valider sur quoi il porte.
     */
    nombre: number
    loading?: boolean
}>()

const emit = defineEmits<{
    (e: 'update:modelValue', valeur: boolean): void
    (e: 'confirme', decision: IssueRejet, commentaire: string): void
}>()

/**
 * Issue choisie, null tant que l'utilisateur n'a pas repondu.
 *
 * Un null et non un defaut : toute valeur initiale ferait partie de la
 * confirmation sans que l'utilisateur l'ait demandee, et "Rejeter" serait
 * alors aussi facile a declencher que "tout decocher".
 */
const decision = ref<IssueRejet | null>(null)

const commentaire = ref('')

/**
 * Plafond du motif, aligne sur celui du backend.
 *
 * BaseInput ne sait pas poser un maxlength sur sa textarea, et un attribut
 * passe en trop atterrirait sur le div qui l'entoure, donc sur rien. La limite
 * est donc appliquee ici : sans elle, l'utilisateur ecrit son motif, clique, et
 * le backend refuse les mille premiers caracteres en trop -- un echec evitable
 * apres avoir tout saisi.
 */
const LONGUEUR_MAX_COMMENTAIRE = 1000

/** Caracteres restants, rappeles quand le motif approche du plafond. */
const caracteresRestants = computed(
    () => LONGUEUR_MAX_COMMENTAIRE - commentaire.value.length
)

/** Rappel du plafond, une fois le motif assez long pour qu'il compte. */
const helperMotif = computed(() =>
    caracteresRestants.value <= 100
        ? `${caracteresRestants.value} caractère(s) restant(s)`
        : ''
)

/**
 * Le motif est il suffisant ?
 *
 * Trimé avant test : un commentaire de espaces n'est pas un motif, et l'exiger
 * ici evite une erreur avoidable.
 */
const commentaireSuffisant = computed(() => commentaire.value.trim().length > 0)

const confirmationActive = computed(
    () => decision.value !== null && commentaireSuffisant.value
)

/**
 * Titles et consequences, par issue.
 *
 * Une seule table plutot qu'un libelle par branche dans le template : les
 * deux elements disent la meme chose, et les tenir a un seul endroit evite
 * qu'un titre promette une consequence qu'un libelle contredise.
 */
const TEXTES: Record<IssueRejet, {
    titre: string
    consequence: string
}> = {
    REJETE: {
        titre: 'Rejet définitif',
        consequence:
            "L'activité sort du circuit : elle ne sera ni publiée, ni reprise par son auteur.",
    },
    RETOUR_MODIFICATION: {
        titre: 'Retour pour modification',
        consequence:
            "L'activité redevient modifiable et repart chez son auteur, qui pourra la resoumettre.",
    },
}

const texteCourant = computed(() => (decision.value ? TEXTES[decision.value] : null))

/** Pluralisation : le libelle doit rester juste pour 1 comme pour 3. */
function libelleActivites(): string {
    return props.nombre > 1
        ? `${props.nombre} activités seront concernées`
        : "L'activité sélectionnée sera concernée"
}

/**
 * Tronque au-dela du plafond.
 *
 * Le retrait plutot qu'une simple erreur : sur un motif, la fin est la
 * precision, et interdire d'ecrire davantage vaut mieux qu'un refus apres
 * coup. Une.selection en fin de saisie laisse voir ce qui a ete coupe.
 */
watch(commentaire, valeur => {
    if (valeur.length > LONGUEUR_MAX_COMMENTAIRE) {
        commentaire.value = valeur.slice(0, LONGUEUR_MAX_COMMENTAIRE)
    }
})

/**
 * Chaque reouverture repart de zero.
 *
 * Sans cette remise a zero, un motif ecrit pour une activite resterait affiche
 * -- et surtout pre-rempli -- pour la suivante, qui n'a rien a voir. C'est le
 * piege classique des modales reutilisees.
 */
watch(
    () => props.modelValue,
    ouvert => {
        if (!ouvert) return

        decision.value = null
        commentaire.value = ''
    }
)

function surConfirmer(): void {
    if (!confirmationActive.value || decision.value === null) return

    emit('confirme', decision.value, commentaire.value.trim())
}

function fermer(): void {
    emit('update:modelValue', false)
}
</script>

<template>
    <!--
        custom-class comme BaseConfirm, et pour la meme raison : la modale est
        teleportee dans le body, donc elle n'est plus dans le sous-arbre du
        composant qui l'a ouverte. Sans cette classe, ni les tests ni une
        regle CSS ne pourraient la designer.
    -->
    <BaseModal
        :modelValue="modelValue"
        title="Rejeter l'activité"
        :closeOnOutside="false"
        custom-class="rejet-activite"
        @update:modelValue="emit('update:modelValue', $event)"
    >
        <div class="rejet">
            <p class="helper-text mb-0">{{ libelleActivites() }}</p>

            <!--
                Deux options, presentees comme des cartes et non comme des
                boutons : elles ne sont pas deux facons de faire la meme chose,
                mais deux issues opposees. Les libelles disent donc ce qui arrive
                a l'activite, pas seulement ce que fait le bouton.

                Le role="radiogroup" et le type="radio" font que les fleches et
                la tabulation se comportent comme dans un vrai groupe de choix,
                ce qu'un embellissement en <button> ne donnerait pas.
            -->
            <div class="rejet__choix" role="radiogroup" aria-label="Issue du rejet">
                <label v-for="(texte, valeur) in TEXTES" :key="valeur"
                    class="rejet__option" :class="{ 'rejet__option--active': decision === valeur }">
                    <input type="radio" name="issue-rejet" :value="valeur" v-model="decision"
                        class="rejet__radio" />

                    <span class="rejet__option-texte">
                        <span class="rejet__option-titre">{{ texte.titre }}</span>
                        <span class="rejet__option-consequence">{{ texte.consequence }}</span>
                    </span>
                </label>
            </div>

            <!--
                La consequence choisie est rappelee sous le motif, au moment ou
                l'utilisateur ecrit : c'est la que la decision se forme.
            -->
            <p v-if="texteCourant" class="rejet__rappel">
                <i class="bi bi-exclamation-triangle"></i>
                {{ texteCourant.consequence }}
            </p>

            <BaseInput
                v-model="commentaire"
                type="textarea"
                label="Motif du rejet (obligatoire)"
                placeholder="Ce qui doit être corrigé, ou pourquoi l'activité est rejetée"
                :helper="helperMotif"
            />

            <p class="helper-text rejet__aide">
                Ce motif sera lu par l'auteur dans l'historique, et par le
                responsable dans la décision enregistrée.
            </p>
        </div>

        <template #footer>
            <BaseButton variant="secondary" :disabled="loading" @click="fermer">
                Annuler
            </BaseButton>

            <!--
                Desactive tant qu'il n'y a ni issue ni motif : le bouton promet
                alors une action qui echouerait, et une erreur de validation
                pour un clic premature est la pire maniere de l'apprendre.
            -->
            <BaseButton variant="danger" :disabled="!confirmationActive" :loading="loading"
                @click="surConfirmer">
                <i class="bi bi-x-circle"></i>
                Rejeter
            </BaseButton>
        </template>
    </BaseModal>
</template>

<style scoped>
.rejet {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

/*
    Les options sont des <label>, pas des <button> : le clic sur toute la carte
    coche le radio, sans gestion d'evenement a ecrire, et le clavier atteint
    la carte par son radio natif.
*/
.rejet__choix {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.rejet__option {
    display: flex;
    align-items: flex-start;
    gap: 0.75rem;
    padding: 0.75rem;
    border: 1px solid #dbe4ec;
    border-radius: 8px;
    cursor: pointer;
    transition: border-color 0.15s, background-color 0.15s;
}

.rejet__option:hover {
    border-color: #b6c6d5;
}

.rejet__option--active {
    border-color: #dc3545;
    background-color: #fdf2f3;
}

.rejet__radio {
    margin-top: 0.2rem;
    flex-shrink: 0;
}

.rejet__option-texte {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
}

.rejet__option-titre {
    font-weight: 600;
    color: #1a2b3c;
}

.rejet__option-consequence {
    font-size: 0.85rem;
    color: #74879b;
}

.rejet__rappel {
    display: flex;
    align-items: flex-start;
    gap: 0.5rem;
    margin: 0;
    padding: 0.6rem 0.75rem;
    border-radius: 6px;
    background-color: #fff8e6;
    color: #8a6100;
    font-size: 0.88rem;
}

.rejet__aide {
    margin: 0;
    font-size: 0.82rem;
}
</style>

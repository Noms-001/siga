<script setup lang="ts">

import { computed, ref, watch } from 'vue'

import { BaseButton, BaseInput, BaseModal } from '@/components/base'

type IssueDecision = 'VALIDE' | 'REJETE' | 'RETOUR_MODIFICATION'

const props = defineProps<{
    modelValue: boolean

    issue: IssueDecision

    nombre: number

    loading?: boolean
}>()

const emit = defineEmits<{
    (e: 'update:modelValue', valeur: boolean): void
    (e: 'confirme', commentaire: string): void
}>()

const commentaire = ref('')

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

const TEXTES: Record<IssueDecision, {
    titre: string
    verbe: string
    messageSingulier: string
    messagePluriel: (n: number) => string
    consequence: string
    libelleConfirmer: string
    icone: string
    ton: 'success' | 'danger' | 'warning'
    classeRappel: string
}> = {
    VALIDE: {
        titre: "Valider l'activité",
        verbe: 'validée',
        messageSingulier: "L'activité sera validée et publiée au suivi.",
        messagePluriel: n => `${n} activités seront validées et publiées au suivi.`,
        consequence:
            'Elle ne pourra plus être modifiée : toute correction devra passer par son auteur.',
        libelleConfirmer: 'Valider',
        icone: 'bi bi-check-circle',
        ton: 'success',
        classeRappel: 'decision__rappel--valide',
    },
    REJETE: {
        titre: "Rejeter l'activité",
        verbe: 'rejetée',
        messageSingulier: "L'activité sera rejetée définitivement.",
        messagePluriel: n => `${n} activités seront rejetées définitivement.`,
        consequence:
            'Elle sortira du circuit et ne pourra plus être reprise.',
        libelleConfirmer: 'Rejeter',
        icone: 'bi bi-x-circle',
        ton: 'danger',
        classeRappel: 'decision__rappel--rejete',
    },
    RETOUR_MODIFICATION: {
        titre: "Renvoyer l'activité",
        verbe: 'renvoyée',
        messageSingulier: "L'activité sera renvoyée à l'étape précédente.",
        messagePluriel: n => `${n} activités seront renvoyées à l'étape précédente.`,
        consequence:
            'Elle redeviendra modifiable par son auteur, qui pourra la resoumettre.',
        libelleConfirmer: 'Renvoyer',
        icone: 'bi bi-arrow-counterclockwise',
        ton: 'warning',
        classeRappel: 'decision__rappel--retour',
    },
}

const texteCourant = computed(() => TEXTES[props.issue])

/**
 * Message d'entree, adapte au nombre d'activites concernees.
 *
 * On ne dit pas "L'activite sera..." quand il y en a trois, ni "3 activites
 * seront..." quand il y en a une. Le nombre doit apparaitre dans la phrase
 * qui decrit l'action, pas dans une note separee.
 */
const messageEntree = computed(() =>
    props.nombre > 1
        ? texteCourant.value.messagePluriel(props.nombre)
        : texteCourant.value.messageSingulier
)

/**
 * Chaque reouverture repart de zero.
 *
 * Un motif ecrit pour une activite ne doit jamais pre-remplir la suivante,
 * qui n'a rien a voir. C'est le piege classique des modales reutilisees.
 */
watch(
    () => props.modelValue,
    ouvert => {
        if (!ouvert) return
        commentaire.value = ''
    }
)

/**
 * Tronque au-dela du plafond.
 *
 * Le retrait plutot qu'une simple erreur : sur un motif, la fin est la
 * precision, et interdire d'ecrire davantage vaut mieux qu'un refus apres
 * coup.
 */
watch(commentaire, valeur => {
    if (valeur.length > LONGUEUR_MAX_COMMENTAIRE) {
        commentaire.value = valeur.slice(0, LONGUEUR_MAX_COMMENTAIRE)
    }
})

function surConfirmer(): void {
    emit('confirme', commentaire.value.trim())
}

function fermer(): void {
    emit('update:modelValue', false)
}
</script>

<template>
    <!--
        custom-class comme BaseConfirm, et pour la meme raison : la modale
        est teleportee dans le body, donc elle n'est plus dans le sous-arbre
        du composant qui l'a ouverte. Sans cette classe, ni les tests ni une
        regle CSS ne pourraient la designer.
    -->
    <BaseModal
        :modelValue="modelValue"
        :title="texteCourant.titre"
        :closeOnOutside="false"
        custom-class="decision-activite"
        @update:modelValue="emit('update:modelValue', $event)"
    >
        <div class="decision">
            <p class="decision__message">{{ messageEntree }}</p>

            <!--
                La consequence est repetee sous le message, dans une couleur
                qui dit sa gravite. C'est la seule phrase que l'utilisateur
                doit retenir avant de cliquer : "l'activite sortira du
                circuit" et "l'activite pourra etre resoumise" ne se lisent
                pas du meme oeil.
            -->
            <p
                class="decision__rappel"
                :class="texteCourant.classeRappel"
            >
                <i class="bi" :class="texteCourant.icone"></i>
                {{ texteCourant.consequence }}
            </p>

            <BaseInput
                v-model="commentaire"
                type="textarea"
                label="Commentaire (optionnel)"
                placeholder="Ce qui justifie la décision, ou ce qui doit être corrigé"
                :helper="helperMotif"
            />

            <p class="helper-text decision__aide">
                Ce commentaire sera enregistré dans l'historique de
                l'activité et lu par son auteur.
            </p>
        </div>

        <template #footer>
            <BaseButton variant="secondary" :disabled="loading" @click="fermer">
                Annuler
            </BaseButton>

            <!--
                Le bouton n'est jamais desactive pour motif manquant : le
                motif est optionnel dans les trois cas. Seul le chargement
                en cours le neutralise, pour eviter un double envoi.
            -->
            <BaseButton
                :variant="texteCourant.ton"
                :loading="loading"
                :disabled="loading"
                @click="surConfirmer"
            >
                <i class="bi" :class="texteCourant.icone"></i>
                {{ texteCourant.libelleConfirmer }}
            </BaseButton>
        </template>
    </BaseModal>
</template>

<style scoped>
.decision {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.decision__message {
    margin: 0;
    font-size: 0.95rem;
    color: #1a2b3c;
    line-height: 1.4;
}

.decision__rappel {
    display: flex;
    align-items: flex-start;
    gap: 0.5rem;
    margin: 0;
    padding: 0.6rem 0.75rem;
    border-radius: 6px;
    font-size: 0.88rem;
    line-height: 1.4;
}

/* Vert du succes : "elle sera publiee". */
.decision__rappel--valide {
    background-color: #e6f5eb;
    color: #1f7a3e;
}

/* Rouge de la cloture : "elle sortira du circuit". */
.decision__rappel--rejete {
    background-color: #fdeaea;
    color: #b3261e;
}

/* Jaune de l'avertissement : "elle repart chez son auteur". */
.decision__rappel--retour {
    background-color: #fff8e6;
    color: #8a6100;
}

.decision__aide {
    margin: 0;
    font-size: 0.82rem;
}
</style>
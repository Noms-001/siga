<script setup lang="ts">
/**
 * Activites soumises, en attente de decision.
 *
 * CE QUE CETTE PAGE EST, ET CE QU'ELLE N'EST PAS
 *
 * - Elle liste les activites au statut EN_ATTENTE_VALIDATION, et rien
 *   d'autre. Le statut n'est pas un filtre que l'utilisateur pourrait
 *   retirer : il est la definition de la page, au meme titre que BROUILLON
 *   l'est pour la page des brouillons.
 *
 * - Elle ne reclasse pas l'activite : la decision est une ecriture cote
 *   backend, et son issue est un succes ou un refus motive. D'ou le meme
 *   comportement que sur Brouillons -- la carte disparait apres succes, et
 *   reste en place avec son message apres echec.
 *
 * - Elle ne verifie AUCUNE REGLE DE VALIDATION. Ni dates, ni presence d'une
 *   sous-activite, ni completude d'un objectif. Elle affiche ce que le backend
 *   decide. Une regle ecrite ici serait un second endroit ou la changer, et le
 *   navigateur n'est pas un endroit ou l'on verifie une regle metier.
 *
 * AUCUN COMPTEUR DE SUIVI ICI
 * La page liste des activites en attente de decision, pas un exercice : des
 * cards d'avancement y seraient toujours a zero, et alourdiraient la page sans
 * rien apprendre. Meme raison que sur Brouillons.
 *
 * LE REJET PASSE PAR UNE MODALE, LA VALIDATION PAS
 * Valider n'a qu'un sens, donc qu'un bouton. Rejeter, non : rejeter
 * definitivement et renvoyer pour modification ne se consequences pas, et les
 * confondre serait grave. La modale fait choisir, et exige un motif.
 */
import { computed, onMounted, reactive, ref } from 'vue'

import { BaseButton, BaseConfirm, BaseInput, BasePagination } from '@/components/base'
import { RejetActiviteModal } from '@/components/activite'
import * as activiteService from '@/services/activite'
import { useAutocomplete, type Suggestion } from '@/composables/useAutocomplete'
import { useConfirmation } from '@/composables/useConfirmation'
import type {
    ActiviteFiltres,
    ActiviteListItem,
    DecisionValidation,
    VueActivite,
} from '@/types/activite'
import { formatDateSeule } from '@/utils/date'

// ------------------------------------------------------------------
// Constantes de presentation
// ------------------------------------------------------------------

const TAILLE_PAGE = 9

const VUES: Array<{ value: VueActivite; label: string }> = [
    { value: 'PTA', label: 'Activités PTA' },
    { value: 'NON_PTA', label: 'Activités NON PTA' },
]

/**
 * Une activite en attente appartient a son exercice, mais la page qui la
 * traite ne suit pas un exercice : elle vide une file d'attente. Annee a 0,
 * que le service traduit par "aucune contrainte", pour la meme raison que
 * sur Brouillons.
 */
function filtresVides(): ActiviteFiltres {
    return {
        search: '',
        objectifSearch: '',
        objectifSpecifiqueId: null,
        serviceId: null,
        prioriteId: null,
        typeActiviteId: null,
        siteId: null,
        annee: 0,
        trimestre: null,
        dateDebut: '',
        dateFin: '',
    }
}

// ------------------------------------------------------------------
// Etat
// ------------------------------------------------------------------

const vue = ref<VueActivite>('PTA')
const filtres = reactive<ActiviteFiltres>(filtresVides())

const activites = ref<ActiviteListItem[]>([])
const page = ref(1)
const totalPages = ref(0)
const totalElements = ref(0)

const chargement = ref(false)
const erreur = ref<string | null>(null)
const succes = ref<string | null>(null)

/**
 * Identifiants en cours de decision.
 *
 * Un Set et non un booleen global : plusieurs cards sont affichees, et deux
 * boutons ne doivent pas se neutraliser. Le backend refuse une double decision
 * (409), mais il ne faut pas faire attendre l'utilisateur pour le decouvrir.
 */
const enCours = ref<Set<number>>(new Set())

/**
 * Activites cochees, en attente d'une decision groupee.
 *
 * Un Set d'identifiants : la selection ne doit pas devenir fausse si la liste
 * se recharge entre-temps, et l'identifiant suffit a retrouver la card. Il ne
 * contient que des ids de la page affichee, donc tout id qu'il contient a une
 * card visible -- ce qui evite de decider sur des activites que
 * l'utilisateur ne voit plus.
 */
const selection = ref<Set<number>>(new Set())

/**
 * Confirmation posee avant toute decision.
 *
 * Deux demandes distinctes : la page decide aussi bien sur une card que sur un
 * lot, et un message qui parle de "l'activite" quand il y en a trois serait
 * faux. `confirmLot` et `confirmCard` savent chacune ce qu'elles demandent.
 */
const confirmation = useConfirmation()

/**
 * Rejet en cours de saisie.
 *
 * Porte l'issue et le motif saisis, pas l'identifiant de l'activite : la
 * modale prepare une decision, la page l'envoie. Tant que ce reference est
 * null, rien n'est ouvert -- un clic sur "Rejeter" puis une annulation ne
 * doit pas laisser une decision en suspens derriere elle.
 */
const rejet = ref<{ issue: DecisionValidation; commentaire: string } | null>(null)

// ------------------------------------------------------------------
// Chargement
// ------------------------------------------------------------------

let compteurRequete = 0

async function charger() {
    const jeton = ++compteurRequete

    chargement.value = true
    erreur.value = null

    let reponse

    try {
        reponse = await activiteService.listerActivitesAValider(
            { ...filtres },
            vue.value,
            page.value,
            TAILLE_PAGE
        )
    } catch {
        if (jeton !== compteurRequete) return
        erreur.value = 'Connexion au serveur impossible. Réessayez dans un instant.'
        activites.value = []
        chargement.value = false
        return
    }

    if (jeton !== compteurRequete) return

    if (!reponse.success) {
        erreur.value = reponse.error
        activites.value = []
        chargement.value = false
        return
    }

    activites.value = reponse.data.content
    totalElements.value = reponse.data.totalElements
    totalPages.value = reponse.data.totalPages

    chargement.value = false
}

// ------------------------------------------------------------------
// Actions
// ------------------------------------------------------------------

function changerVue(valeur: VueActivite) {
    if (vue.value === valeur) return

    vue.value = valeur
    page.value = 1
    void charger()
}

function changerPage(nouvelle: number) {
    page.value = nouvelle
    void charger()
}

function surRecherche() {
    filtres.search = autocomplete.saisie.value
    page.value = 1
    void charger()
}

function estEnCours(id: number): boolean {
    return enCours.value.has(id)
}

// ------------------------------------------------------------------
// Selection
// ------------------------------------------------------------------

function estSelectionne(id: number): boolean {
    return selection.value.has(id)
}

/**
 * Cocher ou decocher une card.
 *
 * On recree le Set plutot que de le modifier : Vue detecte le changement par
 * comparaison de reference, et une mutation sur place ne declencherait aucun
 * rendu -- la case resterait cochee a l'ecran alors que l'etat ne l'est plus.
 */
function basculerSelection(id: number): void {
    const suivant = new Set(selection.value)

    if (suivant.has(id)) {
        suivant.delete(id)
    } else {
        suivant.add(id)
    }

    selection.value = suivant
}

/**
 * Selectionner toutes les cards affichees.
 *
 * "Affichees" et non "toutes" : la page est paginee, et cocher des activites
 * invisibles reviendrait a les decider sans que l'utilisateur les ait vues. Le
 * libelle du bouton porte ce nombre, pour que l'action ne porte pas a confusion.
 */
function selectionnerTout(): void {
    selection.value = new Set(activites.value.map(a => a.id))
}

/** Tout decocher, sans toucher aux activites. */
function toutDeselectionner(): void {
    selection.value = new Set()
}

/**
 * Le bouton groupe est-il actif ?
 *
 * Il suit l'etat reel de la selection : au moins une card cochee, aucune en
 * cours de decision. Un bouton actif sans effet est le pire des deux -- il
 * promet une action que rien ne fait.
 */
const decisionGroupeeActive = computed(
    () => selection.value.size > 0 && enCours.value.size === 0
)

// ------------------------------------------------------------------
// Validation
// ------------------------------------------------------------------

/**
 * Valider une activite, puis la retirer de la page.
 *
 * Le retrait est local et non un rechargement : la reponse contient deja le
 * statut obtenu, et l'activite ne peut plus figurer dans une liste d'attente.
 * Un rechargement repasserait par le serveur pour un resultat deja connu, et
 * ferait clignoter toute la page.
 *
 * SEUL LE 409 RETIRE LA CARD, parce que lui seul signifie "cette activite a
 * deja ete tranchee" : elle y resterait avec une action qui echouera toujours.
 * Un 400 est un refus de regle metier -- motif manquant -- et l'activite doit
 * rester affichee, avec son message. C'est ce que le statut HTTP permet de
 * distinguer ; sans lui, les deux cas se confondaient et la card partait toujours.
 *
 * UNE PANNE LAISSE LA CARD : rien n'a ete decide, et l'effacer ferait croire le
 * contraire. L'utilisateur reessaie d'un clic.
 */
function valider(item: ActiviteListItem): void {
    if (estEnCours(item.id)) return

    confirmation.demander({
        titre: 'Valider l\'activité',
        message: `L'activité ${item.code} sera validée et publiée au suivi.`,
        consequence:
            "Elle ne pourra plus être modifiée : toute correction devra passer par son auteur.",
        libelleConfirmer: 'Valider',
        ton: 'primary',
        icone: 'bi bi-check-circle',
        action: () => deciderReellement(item.id, 'VALIDE', null),
    })
}

/**
 * Appliquer une decision sur une seule card, puis la retirer de la page.
 *
 * Enveloppe de decider() pour le cas unitaire : elle nomme l'activite
 * concernee dans le message, ce qu'un message de lot ne peut pas faire, et
 * elle retire la card. Le retrait est local et non un rechargement : la
 * reponse contient deja le statut obtenu, et l'activite ne peut plus figurer
 * dans une liste d'attente.
 *
 * Le code est releve AVANT le retrait : apres, la card a disparu de la liste
 * et il ne resterait qu'un identifiant, que l'utilisateur ne peut pas relier a
 * rien.
 */
async function deciderReellement(
    id: number,
    issue: DecisionValidation,
    commentaire: string | null
): Promise<void> {
    const code = codeDe(id)

    const resultat = await decider(id, issue, commentaire)

    if (resultat !== 'reussi') {
        return
    }

    retirer(id)

    succes.value = issue === 'VALIDE'
        ? `Activité ${code} validée et publiée au suivi.`
        : issue === 'REJETE'
            ? `Activité ${code} rejetée définitivement.`
            : `Activité ${code} renvoyée pour modification.`
}

/**
 * Valider les activites cochees, puis les retirer de la page.
 *
 * Les appels partent l'un apres l'autre et non en parallele : une decision est
 * une ecriture d'etat, et un lot simultane sur la meme activite verifierait deux
 * fois la meme transition.
 *
 * UNE REQUETE PAR ACTIVITE, et non une qui les regroupe : le backend n'expose
 * qu'un endpoint par activite. Une panne ou un refus n'annule donc pas les
 * autres, et chaque card est traitee selon son propre resultat.
 *
 * MME REGLE QUE LA VALIDATION UNITAIRE : un 409 retire la card, un autre refus
 * la garde avec son message. Voir valider().
 *
 * UNE PANNE INTERROMPT LE LOT : le reste partirait sur une connexion morte, et
 * l'utilisateur verrait des cards disparaitre sans confirmation. La selection
 * est alors conservee pour qu'il puisse reessayer d'un clic.
 */
function validerSelection(): void {
    if (!decisionGroupeeActive.value) return

    const nombre = selection.value.size

    confirmation.demander({
        titre: 'Valider la sélection',
        message: `${nombre} activité(s) seront validées et publiées au suivi.`,
        consequence: 'Elles ne pourront plus être modifiées.',
        libelleConfirmer: `Valider ${nombre}`,
        ton: 'primary',
        icone: 'bi bi-check-circle',
        action: deciderSelectionReellement,
    })
}

async function deciderSelectionReellement(): Promise<void> {
    erreur.value = null
    succes.value = null

    // Copie : la boucle modifie la selection, et parcourir l'original en
    // direct en sauterait une.
    const ids = [...selection.value]

    let reussies = 0
    const refus: string[] = []

    for (const id of ids) {
        if (enCours.value.has(id)) continue

        const issue = await decider(id, 'VALIDE', null)

        if (issue === 'panne') {
            // La selection est conservee pour qu'il puisse reessayer d'un clic.
            selection.value = new Set(selection.value).add(id)
            break
        }

        if (issue === 'refus') {
            refus.push(`${codeDe(id)} : décision refusée`)
            continue
        }

        reussies += 1
        retirer(id)
        selection.value = new Set([...selection.value].filter(x => x !== id))
    }

    if (refus.length) {
        succes.value = reussies > 0
            ? `${reussies} activité(s) validée(s), ${refus.length} refusée(s).`
            : null
        erreur.value = refus.join(' ')
        return
    }

    succes.value = `${reussies} activité(s) validée(s) et publiée(s) au suivi.`
}

/** Oter une activite du lot en cours, sans toucher a la selection. */
function retirerEnCours(id: number): void {
    enCours.value = new Set([...enCours.value].filter(x => x !== id))
}

// ------------------------------------------------------------------
// Rejet
// ------------------------------------------------------------------

/**
 * Ouvrir la modale de rejet pour une card.
 *
 * Le rejet passe par une modale et non par une confirmation, parce qu'il
 * demande une information que la validation n'exige pas : l'issue, puis le
 * motif. La confirmation est posee apres, une fois ce que l'utilisateur a
 * choisi -- confirmer une decision qu'il n'a pas encore choisie ne confirmerait
 * rien.
 */
function ouvrirRejet(item: ActiviteListItem): void {
    if (estEnCours(item.id)) return

    rejet.value = { issue: 'REJETE', commentaire: '' }
}

function ouvrirRejetSelection(): void {
    if (!decisionGroupeeActive.value) return

    rejet.value = { issue: 'REJETE', commentaire: '' }
}

/**
 * Fermer la modale sans decider.
 *
 * Le contenu est vide ici et non a la confirmation : l'annulation doit
 * effacer la saisie, sinon un motif ecrit pour une activite resterait
 * pre-rempli pour la suivante.
 */
function fermerRejet(): void {
    rejet.value = null
}

/**
 * Poser la confirmation une fois l'issue et le motif choisis.
 *
 * Le message nomme le nombre et l'issue, pour que le dernier ecran avant
 * l'ecriture dise ce qui va se passer.
 */
function surRejetChoisi(issue: DecisionValidation, commentaire: string): void {
    const nombre = selection.value.size

    const issueLisible = issue === 'REJETE'
        ? 'rejetées définitivement'
        : 'renvoyées à leur auteur pour modification'

    confirmation.demander({
        titre: issue === 'REJETE' ? 'Confirmer le rejet' : 'Confirmer le retour',
        message:
            `${nombre} activité(s) seront ${issueLisible}.`,
        consequence: issue === 'REJETE'
            ? "Elles sortiront du circuit et ne pourront plus être reprises."
            : "Elles redeviendront modifiables par leur auteur, qui pourra les resoumettre.",
        libelleConfirmer: issue === 'REJETE' ? 'Rejeter' : 'Renvoyer',
        ton: 'danger',
        icone: 'bi bi-x-circle',
        action: () => deciderReellementRejet(issue, commentaire),
    })
}

/**
 * Appliquer la decision collectee.
 *
 * Le lien entre la confirmation et l'action est fait ici, et non dans
 * surRejetChoisi : la confirmation peut vivre, et c'est au moment ou elle est
 * confirmee qu'il faut savoir ce qui sera reellement envoye.
 */
async function deciderReellementRejet(
    issue: DecisionValidation,
    commentaire: string
): Promise<void> {
    // La selection peut avoir bouge pendant que la confirmation etait posee.
    const ids = [...selection.value]

    if (!ids.length) {
        rejet.value = null
        return
    }

    let reussies = 0
    const refus: string[] = []

    for (const id of ids) {
        const resultat = await decider(id, issue, commentaire)

        if (resultat === 'panne') {
            selection.value = new Set(selection.value).add(id)
            break
        }

        if (resultat === 'refus') {
            refus.push(`${codeDe(id)} : décision refusée`)
            continue
        }

        reussies += 1
        retirer(id)
        selection.value = new Set([...selection.value].filter(x => x !== id))
    }

    if (refus.length) {
        succes.value = reussies > 0
            ? `${reussies} activité(s) traitée(s), ${refus.length} refusée(s).`
            : null
        erreur.value = refus.join(' ')
        return
    }

    succes.value = issue === 'REJETE'
        ? `${reussies} activité(s) rejetée(s).`
        : `${reussies} activité(s) renvoyée(s) pour modification.`
}

// ------------------------------------------------------------------
// Appel backend
// ------------------------------------------------------------------

/**
 * Une seule decision, quel que soit le nombre de cartes concernees.
 *
 * Le retour est un verdict, pas une reponse : ce que la page doit faire
 * ensuite ne depend que de lui -- retirer la card, garder un refus a afficher,
 * ou interrompre le lot parce que la connexion est morte. Eparpiller cette
 * decision ici la repeterait trois fois, et une repetition diverge toujours
 * un jour.
 */
async function decider(
    id: number,
    issue: DecisionValidation,
    commentaire: string | null
): Promise<'reussi' | 'refus' | 'panne'> {
    erreur.value = null
    succes.value = null
    enCours.value = new Set(enCours.value).add(id)

    let reponse

    try {
        reponse = await activiteService.deciderValidation(id, issue, commentaire)
    } catch {
        retirerEnCours(id)
        erreur.value =
            'Connexion au serveur impossible. La décision n\'a pas été enregistrée.'
        return 'panne'
    }

    retirerEnCours(id)

    if (reponse.success) {
        succes.value = null
        return 'reussi'
    }

    succes.value = null
    erreur.value = reponse.error

    // 409 : elle a deja ete tranchee, la card part aussi. Autre refus : la card
    // reste, cochee, avec son message.
    if (reponse.status === 409) {
        retirer(id)
        selection.value = new Set([...selection.value].filter(x => x !== id))
    }

    return 'refus'
}

/** Code lisible d'une activite, pour nommer un refus sans relire la liste. */
function codeDe(id: number): string {
    return activites.value.find(a => a.id === id)?.code ?? `#${id}`
}

/**
 * Retire une carte du tableau courant.
 *
 * Decremente aussi le total, sinon la pagination garde une page fantome : il
 * resterait "1-9 sur 9" alors qu'il n'y a plus rien a afficher.
 */
function retirer(id: number) {
    const avant = activites.value.length

    activites.value = activites.value.filter(a => a.id !== id)

    if (activites.value.length === avant) {
        return
    }

    totalElements.value = Math.max(0, totalElements.value - 1)
    totalPages.value = Math.max(1, Math.ceil(totalElements.value / TAILLE_PAGE))

    // La card retiree etait la derniere de la page courante : on recule,
    // sinon la page affiche un vide alors que la precedente existe.
    if (activites.value.length === 0 && page.value > 1) {
        page.value -= 1
        void charger()
    }
}

// ------------------------------------------------------------------
// Autocomplete
// ------------------------------------------------------------------

/**
 * Meme composable que la liste, avec le meme choix volontaire : la suggestion
 * fige le CODE dans le filtre, jamais le libelle, qui peut contenir des
 * espaces et evoluer.
 */
function formatSuggestion(s: Suggestion): string {
    return s.libelleSecondaire
        ? `${s.code} — ${s.libelle} (${s.libelleSecondaire})`
        : `${s.code} — ${s.libelle}`
}

const autocomplete = useAutocomplete({
    rechercher: async (terme: string) => {
        const reponse = await activiteService.autocompleterActivites(terme)
        return reponse.success ? reponse.data : []
    },
    onSelectionner: (s) => {
        filtres.search = s.code ?? ''
        page.value = 1
        void charger()
    },
    onEffacer: () => {
        if (filtres.search !== '') {
            filtres.search = ''
            page.value = 1
            void charger()
        }
    },
    delaiMs: 350,
})

onMounted(() => {
    void charger()
})
</script>

<template>
    <div class="a-valider">
        <!-- ============ EN-TETE ============ -->
        <div class="page-header">
            <h1 class="page-title">Activités à valider</h1>
            <p class="page-subtitle">
                Activités soumises, en attente de votre décision
            </p>
        </div>

        <!-- ============ MESSAGES ============ -->
        <div v-if="succes" class="alert alert-success d-flex align-items-center gap-2" role="status">
            <i class="bi bi-check-circle"></i>
            <span>{{ succes }}</span>
            <button type="button" class="btn-close ms-auto" @click="succes = null"></button>
        </div>

        <div v-if="erreur" class="alert alert-danger d-flex align-items-center gap-2" role="alert">
            <i class="bi bi-exclamation-triangle"></i>
            <span>{{ erreur }}</span>
            <button type="button" class="btn-close ms-auto" @click="erreur = null"></button>
        </div>

        <!-- ============ PTA / NON PTA ============ -->
        <div class="vue-switch mb-3" role="tablist" aria-label="Type d'activité">
            <button v-for="v in VUES" :key="v.value" type="button" role="tab" class="vue-switch__btn"
                :class="{ 'vue-switch__btn--active': vue === v.value }" :aria-selected="vue === v.value"
                @click="changerVue(v.value)">
                {{ v.label }}
            </button>
        </div>

        <!-- ============ RECHERCHE ET ACTIONS DE LOT ============ -->
        <!--
            Recherche et actions sur la meme ligne : les deux portent sur la
            liste affichee, et les eloigner ferait chercher l'utilisateur entre
            deux bandes de la page. La recherche garde sa largeur maximale pour
            lire une designation, et les boutons prennent le reste.
        -->
        <div class="mb-4 recherche">
            <div class="recherche__ligne">
                <div ref="autocomplete.racine" class="recherche__champ" @keydown="autocomplete.surTouche">
                    <BaseInput v-model="autocomplete.saisie.value" label="Recherche activité" type="search"
                        icon="bi bi-search" placeholder="Code, référence ou désignation" autocomplete="off"
                        :aria-expanded="autocomplete.ouvert.value" @input="surRecherche" />
                    <ul v-if="autocomplete.ouvert.value && autocomplete.suggestions.value.length"
                        class="autocomplete" role="listbox" aria-label="Suggestions d'activité">
                        <li v-for="(s, i) in autocomplete.suggestions.value" :key="s.id" role="option"
                            :aria-selected="i === autocomplete.indexActif.value"
                            :class="{ 'autocomplete__item--actif': i === autocomplete.indexActif.value }"
                            @mouseenter="autocomplete.survoler(i)">
                            <button type="button" tabindex="-1" @click="autocomplete.selectionner(s)">
                                {{ formatSuggestion(s) }}
                            </button>
                        </li>
                    </ul>
                </div>

                <!--
                    Les trois boutons n'apparaissent qu'a partir du moment ou il
                    y a une card a cocher : avant, ils seraient trois controles
                    sans effet.

                    "Tout selectionner" porte le nombre de cards affichees dans
                    son libelle, parce que la page est paginee : "tout" signifie
                    "tout ce qui est a l'ecran", et le nombre le dit sans avoir
                    a deviner.
                -->
                <div v-if="activites.length" class="lot">
                    <span class="lot__compte" role="status">
                        {{ selection.size }} / {{ activites.length }} sélectionnée(s)
                    </span>

                    <BaseButton variant="secondary" size="sm" :disabled="!activites.length"
                        :title="`Sélectionner les ${activites.length} activités affichées`"
                        @click="selectionnerTout">
                        <i class="bi bi-check2-square"></i>
                        Tout sélectionner
                    </BaseButton>

                    <BaseButton v-if="selection.size" variant="secondary" size="sm"
                        title="Retirer toutes les cartes cochées de la sélection" @click="toutDeselectionner">
                        <i class="bi bi-x-square"></i>
                        Tout décocher
                    </BaseButton>

                    <BaseButton variant="success" size="sm"
                        :disabled="!decisionGroupeeActive"
                        :title="selection.size
                            ? `Valider les ${selection.size} activité(s) sélectionnée(s)`
                            : 'Cochez au moins une activité'"
                        @click="validerSelection">
                        <i class="bi bi-check-circle"></i>
                        Valider la sélection
                    </BaseButton>

                    <BaseButton variant="danger" size="sm"
                        :disabled="!decisionGroupeeActive"
                        :title="selection.size
                            ? `Rejeter les ${selection.size} activité(s) sélectionnée(s)`
                            : 'Cochez au moins une activité'"
                        @click="ouvrirRejetSelection">
                        <i class="bi bi-x-circle"></i>
                        Rejeter la sélection
                    </BaseButton>
                </div>
            </div>
        </div>

        <!--
            ============ ETAT VIDE ============

            Masque sur une erreur : "aucune activite a valider" et "service
            indisponible" ne disent pas la meme chose. Apres un echec, la page ne
            sait rien de la file d'attente -- l'ignorer laisserait croire qu'elle
            est vide, et l'utilisateur ne reviendrait pas.
        -->
        <div v-if="!chargement && !activites.length && !erreur" class="vide">
            <i class="bi bi-check2-all"></i>
            <p class="vide__titre">Aucune activité en attente de validation</p>
            <p class="vide__detail">
                Les activités soumises apparaîtront ici jusqu'à leur décision
            </p>
        </div>

        <!-- ============ CARDS ============ -->
        <div class="row g-3">
            <div v-for="item in activites" :key="item.id" class="col-12 col-md-6 col-xl-4">
                <article class="carte" :class="{ 'carte--selectionnee': estSelectionne(item.id) }">
                    <!--
                        Le lien est etire sur toute la carte : le clic n'importe
                        ou ouvre le detail. Il doit rester un lien et non un
                        div cliquable, pour que le clavier, le menu contextuel
                        et "ouvrir dans un nouvel onglet" fonctionnent.
                    -->
                    <router-link class="carte__lien" :to="{ name: 'activite-detail', params: { id: String(item.id) } }">
                        <span class="visually-hidden">Ouvrir le détail de {{ item.code }}</span>
                    </router-link>

                    <!--
                        La case est a son propre tour au-dessus du lien etire
                        (position et z-index), comme les boutons du pied : sans
                        cela, cliquer la case ouvrirait le detail au lieu de
                        cocher. Elle est avant l'entete dans le DOM pour que le
                        clavier l'atteigne avant le contenu de la card, et le
                        libelle estreserve plutot que visible pour ne pas
                        peser sur un titre deja long.
                    -->
                    <div class="carte__selection">
                        <input type="checkbox" class="carte__case" :checked="estSelectionne(item.id)"
                            :aria-label="`Sélectionner ${item.code}`" @click.stop
                            @change.stop="basculerSelection(item.id)" />
                        <span class="visually-hidden">Sélectionner {{ item.code }}</span>
                    </div>

                    <header class="carte__entete">
                        <span class="carte__code">{{ item.code }}</span>
                        <span class="carte__reference">{{ item.reference }}</span>
                    </header>

                    <h2 class="carte__designation">{{ item.designation }}</h2>

                    <dl class="carte__champs">
                        <div class="carte__champ">
                            <dt>Objectif spécifique</dt>
                            <dd v-if="item.objectifSpecifique">
                                {{ item.objectifSpecifique.code }}
                                <span class="carte__libelle">{{ item.objectifSpecifique.libelle }}</span>
                            </dd>
                            <dd v-else class="text-muted">Activité non PTA</dd>
                        </div>

                        <div class="carte__champ">
                            <dt>Période prévue</dt>
                            <dd>
                                {{ formatDateSeule(item.dateDebutPrevue) }}
                                <span class="carte__separateur">→</span>
                                {{ formatDateSeule(item.dateFinPrevue) }}
                            </dd>
                        </div>
                    </dl>

                    <footer class="carte__pied">

                        <!--
                            Les deux boutons restent cliquables malgre le lien
                            etire de la carte : @click.stop evite aussi d'ouvrir
                            le detail en meme temps.
                        -->
                        <div class="carte__actions">
                            <BaseButton variant="success" size="sm" :loading="estEnCours(item.id)"
                                @click.stop="valider(item)">
                                <i class="bi bi-check-circle"></i>
                                Valider
                            </BaseButton>

                            <BaseButton variant="danger" size="sm" :loading="estEnCours(item.id)"
                                @click.stop="ouvrirRejet(item)">
                                <i class="bi bi-x-circle"></i>
                                Rejeter
                            </BaseButton>
                        </div>
                    </footer>
                </article>
            </div>
        </div>

        <!--
            ============ REJET ============

            Rendue hors de la boucle et non sur chaque card : une seule modale
            pour toute la page, sinon deux rejets superposes rendraient
            impossible de savoir lequel est au premier plan. Elle porte le
            nombre d'activites concernees, car son libelle doit rester juste
            pour une seule comme pour trois.
        -->
        <RejetActiviteModal
            v-if="rejet"
            :modelValue="true"
            :nombre="selection.size"
            :loading="enCours.size > 0"
            @update:modelValue="fermerRejet"
            @confirme="surRejetChoisi"
        />

        <!--
            ============ CONFIRMATION ============

            Une seule instance pour toutes les demandes de la page : les
            decisions se suivent, et deux modales superposees rendraient
            impossible de savoir laquelle est au premier plan.
        -->
        <BaseConfirm
            v-model="confirmation.ouvert.value"
            :demande="confirmation.demande.value"
            :loading="confirmation.enCours.value"
            @confirme="confirmation.confirmer"
            @annule="confirmation.annuler"
        />

        <!-- ============ PAGINATION ============ -->
        <div v-if="!chargement && activites.length" class="mt-4">
            <BasePagination :page="page" :total-pages="totalPages" :total-elements="totalElements"
                :page-size="TAILLE_PAGE" :disabled="chargement" @change="changerPage" />
        </div>
    </div>
</template>

<style scoped>
.a-valider {
    padding: 1.5rem 0 2rem;
}

/* --- En-tete --- */
.page-header {
    margin-bottom: 1.5rem;
}

.page-title {
    font-size: 1.6rem;
    font-weight: 700;
    color: #1a2b3c;
    margin: 0;
}

.page-subtitle {
    font-size: 0.9rem;
    color: #74879b;
    margin: 0.2rem 0 0;
}

/* --- Bascule PTA / NON PTA --- */
.vue-switch {
    display: inline-flex;
    padding: 3px;
    background-color: #eef2f6;
    border-radius: 8px;
}

.vue-switch__btn {
    padding: 0.4rem 0.9rem;
    border: none;
    background: transparent;
    border-radius: 6px;
    font-size: 0.85rem;
    font-weight: 500;
    color: #74879b;
    cursor: pointer;
    transition: background-color 0.15s, color 0.15s;
}

.vue-switch__btn--active {
    background-color: #fff;
    color: #1a2b3c;
    box-shadow: 0 1px 3px rgba(26, 43, 60, 0.1);
}

/* --- Recherche et lot --- */
.recherche__ligne {
    display: flex;
    gap: 1rem;
    align-items: flex-start;
    flex-wrap: wrap;
}

.recherche__champ {
    position: relative;
    flex: 1;
    min-width: 260px;
}

.lot {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex-wrap: wrap;
}

.lot__compte {
    font-size: 0.85rem;
    color: #74879b;
    white-space: nowrap;
}

/* --- Autocomplete --- */
.autocomplete {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    z-index: 10;
    margin: 0.25rem 0 0;
    padding: 0.25rem;
    list-style: none;
    background: #fff;
    border: 1px solid #dbe4ec;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(26, 43, 60, 0.1);
    max-height: 280px;
    overflow-y: auto;
}

.autocomplete__item--actif {
    background-color: #f0f5fa;
}

.autocomplete button {
    display: block;
    width: 100%;
    padding: 0.45rem 0.6rem;
    border: none;
    background: transparent;
    border-radius: 6px;
    text-align: left;
    font-size: 0.88rem;
    color: #1a2b3c;
    cursor: pointer;
}

/* --- Etat vide --- */
.vide {
    text-align: center;
    padding: 3rem 1rem;
    color: #74879b;
}

.vide i {
    font-size: 2.5rem;
    opacity: 0.4;
}

.vide__titre {
    margin: 0.75rem 0 0;
    font-size: 1rem;
    font-weight: 600;
    color: #1a2b3c;
}

.vide__detail {
    margin: 0.25rem 0 0;
    font-size: 0.9rem;
}

/* --- Card --- */
.carte {
    position: relative;
    display: flex;
    flex-direction: column;
    height: 100%;
    padding: 1rem 1.1rem;
    background: #fff;
    border: 1px solid #dbe4ec;
    border-radius: 10px;
    transition: border-color 0.15s, box-shadow 0.15s;
}

.carte:hover {
    border-color: #b6c6d5;
    box-shadow: 0 2px 8px rgba(26, 43, 60, 0.07);
}

.carte--selectionnee {
    border-color: #1a73c4;
    box-shadow: 0 0 0 2px rgba(26, 115, 196, 0.15);
}

/*
    Le lien etire couvre toute la card : le clic n'importe ou ouvre le detail.
    Il doit rester un <a>, pour que le clavier, le menu contextuel et
    "ouvrir dans un nouvel onglet" fonctionnent comme attendu.
*/
.carte__lien {
    position: absolute;
    inset: 0;
    z-index: 1;
    border-radius: 10px;
}

/*
    La case et les boutons du pied sont au-dessus du lien etire, et stoppent
    la propagation. Sans cela, cocher une carte ou valider ouvrirait le detail
    au lieu de faire l'action.
*/
.carte__selection {
    position: absolute;
    top: 0.9rem;
    right: 0.9rem;
    z-index: 2;
}

.carte__case {
    width: 1.05rem;
    height: 1.05rem;
    cursor: pointer;
}

.carte__entete {
    display: flex;
    align-items: baseline;
    gap: 0.5rem;
    margin-bottom: 0.35rem;
    padding-right: 2rem;
}

.carte__code {
    font-size: 0.78rem;
    font-weight: 700;
    color: #1a73c4;
    letter-spacing: 0.02em;
}

.carte__reference {
    font-size: 0.75rem;
    color: #93a5b8;
}

.carte__designation {
    margin: 0 0 0.75rem;
    padding-right: 2rem;
    font-size: 0.98rem;
    font-weight: 600;
    line-height: 1.35;
    color: #1a2b3c;
}

.carte__champs {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    margin: 0 0 1rem;
    flex: 1;
}

.carte__champ dt {
    font-size: 0.72rem;
    text-transform: uppercase;
    letter-spacing: 0.03em;
    color: #93a5b8;
}

.carte__champ dd {
    margin: 0.15rem 0 0;
    font-size: 0.88rem;
    color: #1a2b3c;
}

.carte__libelle {
    color: #74879b;
}

.carte__separateur {
    margin: 0 0.35rem;
    color: #93a5b8;
}

.carte__pied {
    position: relative;
    z-index: 2;
    padding-top: 0.75rem;
    border-top: 1px solid #eef2f6;
}

.carte__actions {
    display: flex;
    gap: 0.5rem;
}

.carte__actions > * {
    flex: 1;
}
</style>

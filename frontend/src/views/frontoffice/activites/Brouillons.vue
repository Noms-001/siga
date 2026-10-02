<script setup lang="ts">
/**
 * Activites en cours de redaction, a soumettre a la validation.
 *
 * Ce que cette page est, et n'est pas :
 *
 * - Elle liste les activites au statut BROUILLON, et rien d'autre. Le
 *   statut n'est pas un filtre que l'utilisateur pourrait retirer : il est
 *   la definition de la page.
 *
 * - Elle n'affiche pas les cards de la liste de suivi. Un brouillon n'est
 *   pas publie, donc il n'a ni avancement ni compteur : des cartes de
 *   statistiques y seraient toujours a zero, et alourdiraient la page
 *   sans rien apprendre.
 *
 * - Elle ne reclasse pas l'activite : la soumission est une ecriture cote
 *   backend, et son issue est un succes ou un refus motive. C'est pourquoi
 *   la carte disparaît de la page apres succes, et reste en place avec son
 *   message apres echec.
 *
 * AUCUNE REGLE DE SOUMISSION ICI
 * Cette page ne verifie ni les dates, ni la presence d'une sous-activite,
 * ni la completude d'un objectif. Elle affiche ce que le backend decide.
 * Une reglePlacee ici serait un second endroit ou la changer, et le
 * navigateur n'est pas un endroit ou l'on verifie une regle metier.
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { BaseButton, BaseConfirm, BaseInput, BasePagination } from '@/components/base'
import * as activiteService from '@/services/activite'
import { useAutocomplete, type Suggestion } from '@/composables/useAutocomplete'
import { useConfirmation } from '@/composables/useConfirmation'
import type {
    ActiviteFiltres,
    ActiviteListItem,
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
 * Un brouillon n'est rattache a aucun exercice : annee a 0, que le service
 * traduit par "aucune contrainte". Voir listerActivitesBrouillon.
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
 * Identifiants en cours de soumission.
 *
 * Un Set et non un booléen global : la page affiche plusieurs cartes, et
 * deux boutons ne doivent pas se neutraliser. Le backend refuse une double
 * soumission (409), mais il ne faut pas faire attendre l'utilisateur pour le
 * découvrir.
 */
const enCours = ref<Set<number>>(new Set())

/**
 * Activites cochees, en attente d'une soumission groupee.
 *
 * Un Set d'identifiants et non un tableau d'objets : la selection ne doit
 * pas devenir fausse si la liste se recharge entre-temps, et l'identifiant
 * suffit a retrouver la carte. Il ne contient que des ids de la page affichee,
 * donc tout id qu il contient a une carte visible -- ce qui evite de soumettre
 * des activites que l'utilisateur ne voit plus.
 *
 * AUCUNE REGLE DE SOUMISSION ICI
 * La selection est une commodite d'ecran. Elle ne verifie ni dates, ni
 * sous-activites, ni completude : c'est le backend qui refuse, et dont le
 * message est affiche tel quel.
 */
const selection = ref<Set<number>>(new Set())

/**
 * Confirmation posee avant toute soumission.
 *
 * Deux demandes distinctes et non une seule : la page soumet aussi bien une
 * card qu'un lot, et un message qui parle de "l'activite" quand il y en a trois
 * serait faux. `confirmLot` et `confirmCard` savent chacune ce qu'elles
 * demandent.
 */
const confirmation = useConfirmation()

const router = useRouter()

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
        reponse = await activiteService.listerActivitesBrouillon(
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
 * invisibles reviendrait a les soumettre sans que l'utilisateur les ait vues.
 * Le libelle du bouton porte ce nombre, pour que l'action ne porte pas a
 * confusion.
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
 * cours de soumission. Un bouton actif sans effet est le pire des deux -- il
 * promet une action que rien ne fait.
 */
const soumissionGroupeeActive = computed(
    () => selection.value.size > 0 && enCours.value.size === 0
)

/**
 * Soumettre une activite, puis la retirer de la page.
 *
 * Le retrait est local et non un rechargement : la reponse contient deja le
 * statut obtenu, et l'activite ne peut plus figurer dans une liste de
 * brouillons. Un rechargement repasserait par le serveur pour un resultat
 * deja connu, et ferait clignoter toute la page.
 *
 * SEUL LE 409 RETIRE LA CARD, parce que lui seul signifie "cette activite a
 * deja quitté les brouillons" : elle y resterait avec une action qui echouera
 * toujours. Un 400 ou un 422 est un refus de regle metier -- dates, presence
 * d'une sous-activite -- et l'activite doit rester affichee, avec son message,
 * pour etre corrigee. C est ce que le statut HTTP permet de distinguer ; sans
 * lui, les deux cas se confondaient et la card partait toujours.
 *
 * UNE PANNE LAISSE LA CARD : rien n'a ete soumis, et l'effacer ferait croire le
 * contraire. L'utilisateur reessaie d'un clic.
 */
function soumettre(item: ActiviteListItem): void {
    if (estEnCours(item.id)) return

    confirmation.demander({
        titre: 'Soumettre à la validation',
        message: `L'activité ${item.code} et son contenu seront figés et transmis à validation.`,
        consequence: "Une fois soumise, elle ne sera plus modifiable depuis cette page.",
        libelleConfirmer: 'Soumettre',
        ton: 'warning',
        icone: 'bi bi-send',
        action: () => soumettreReellement(item),
    })
}

async function soumettreReellement(item: ActiviteListItem): Promise<void> {
    erreur.value = null
    succes.value = null
    enCours.value = new Set(enCours.value).add(item.id)

    let reponse

    try {
        reponse = await activiteService.soumettreValidation(item.id)
    } catch {
        retirerEnCours(item.id)
        erreur.value = 'Connexion au serveur impossible. La soumission n\'a pas été enregistrée.'
        return
    }

    retirerEnCours(item.id)

    if (reponse.success) {
        retirer(item.id)
        succes.value = `Activité ${reponse.data.code} soumise à la validation.`
        return
    }

    succes.value = null
    erreur.value = reponse.error

    if (reponse.status === 409) {
        retirer(item.id)
    }
}

/** Oter une activite du lot en cours, sans toucher a la selection. */
function retirerEnCours(id: number): void {
    enCours.value = new Set([...enCours.value].filter(x => x !== id))
}

/**
 * Soumettre les activites cochees, puis les retirer de la page.
 *
 * Les appels partent l'un apres l'autre et non en parallele : une soumission
 * est une ecriture d'etat, et un lot simultane sur la meme activite verifierait
 * deux fois la meme transition.
 *
 * UNE REQUETE PAR ACTIVITE, et non une qui les regroupe : le backend n'expose
 * qu'un endpoint par activite. Une panne ou un refus n'annule donc pas les
 * autres, et chaque card est traitee selon son propre resultat.
 *
 * MME REGLE QUE LA SOUMISSION UNITAIRE : un 409 retire la card, un autre refus
 * la garde avec son message. Voir soumettre().
 *
 * UNE PANNE INTERROMPT LE LOT : le reste partirait sur une connexion morte, et
 * l'utilisateur verrait des cards disparaitre sans confirmation. La selection
 * est alors conservee pour qu il puisse reessayer d un clic.
 */
function soumettreSelection(): void {
    if (!soumissionGroupeeActive.value) return

    const nombre = selection.value.size

    confirmation.demander({
        titre: 'Soumettre la sélection',
        message: `${nombre} activité(s) seront figées et transmises à validation.`,
        consequence: "Une fois soumises, elles ne seront plus modifiables depuis cette page.",
        libelleConfirmer: `Soumettre ${nombre}`,
        ton: 'warning',
        icone: 'bi bi-send',
        action: soumettreSelectionReellement,
    })
}

async function soumettreSelectionReellement(): Promise<void> {
    erreur.value = null
    succes.value = null

    // Copie : la boucle modifie la selection, et parcourir l'original en
    // direct en sauterait une.
    const ids = [...selection.value]

    let soumises = 0
    const refus: string[] = []

    for (const id of ids) {
        if (enCours.value.has(id)) continue

        enCours.value = new Set(enCours.value).add(id)

        let reponse

        try {
            reponse = await activiteService.soumettreValidation(id)
        } catch {
            retirerEnCours(id)
            selection.value = new Set(selection.value).add(id)
            erreur.value =
                'Connexion au serveur impossible. La soumission n\'a pas été enregistrée.'
            break
        }

        retirerEnCours(id)

        if (!reponse.success) {
            // 409 : elle a deja quitté les brouillons, la card part aussi.
            // Autre refus : la card reste, cochee, avec son message.
            if (reponse.status === 409) {
                retirer(id)
                selection.value = new Set([...selection.value].filter(x => x !== id))
            }

            refus.push(reponse.error)
            continue
        }

        soumises += 1
        retirer(id)
        selection.value = new Set([...selection.value].filter(x => x !== id))
    }

    if (refus.length) {
        succes.value = soumises > 0
            ? `${soumises} activit\u00e9(s) soumise(s), ${refus.length} refus\u00e9e(s).`
            : null
        erreur.value = refus.join(' ')
        return
    }

    succes.value = `${soumises} activit\u00e9(s) soumise(s) \u00e0 la validation.`
}

/**
 * Ouvrir le formulaire de modification de ce brouillon.
 *
 * La page liste des brouillons par definition, donc pas de test de statut
 * ici : la liste ne peut pas contenir une activite soumise. Le formulaire
 * recharge le statut depuis le serveur et refuse l'enregistrement s'il a
 * change entre-temps.
 */
function modifier(id: number): void {
    router.push({ name: 'activite-modifier', params: { id: String(id) } })
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

    // La carte retiree etait la derniere de la page courante : on recule,
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
 * Meme composable que la liste, avec le meme choix volontaire : la
 * suggestion fige le CODE dans le filtre, jamais le libelle, qui peut
 * contenir des espaces et evoluer.
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
    <div class="brouillons">
        <!-- ============ EN-TETE ============ -->
        <div class="page-header">
            <h1 class="page-title">Activités créées</h1>
            <p class="page-subtitle">
                Activités en cours de rédaction, à soumettre à la validation
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
                    Les deux boutons n'apparaissent qu'a partir du moment ou il
                    y a une card a cocher : avant, ils seraient deux controles
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

                    <BaseButton variant="primary" size="sm"
                        :disabled="!soumissionGroupeeActive"
                        :title="selection.size
                            ? `Soumettre les ${selection.size} activité(s) cochée(s) à la validation`
                            : 'Cochez au moins une activité'"
                        @click="soumettreSelection">
                        <i class="bi bi-send"></i>
                        Soumettre la sélection
                    </BaseButton>
                </div>
            </div>
        </div>

        <!-- ============ ETAT VIDE ============ -->
        <div v-if="!chargement && !activites.length" class="vide">
            <i class="bi bi-inbox"></i>
            <p class="vide__titre">Aucune activité en cours de rédaction</p>
            <p class="vide__detail">
                Les activités que vous créez apparaîtront ici jusqu'à leur soumission
                à la validation.
            </p>
        </div>

        <!-- ============ CARDS ============ -->
        <div class="row g-3">
            <div v-for="item in activites" :key="item.id" class="col-12 col-md-6 col-xl-4">
                <article class="brouillon" :class="{ 'brouillon--selectionnee': estSelectionne(item.id) }">
                    <!--
                        Le lien est etire sur toute la carte : le clic n'importe
                        ou ouvre le detail. Il doit rester un lien et non un
                        div cliquable, pour que le clavier, le menu contextuel
                        et "ouvrir dans un nouvel onglet" fonctionnent.
                    -->
                    <router-link class="brouillon__lien" :to="{ name: 'activite-detail', params: { id: String(item.id) } }">
                        <span class="visually-hidden">Ouvrir le détail de {{ item.code }}</span>
                    </router-link>

                    <!--
                        La case est a son propre tour au-dessus du lien etire
                        (position et z-index), comme les boutons du pied : sans
                        cela, cliquer la case ouvrirait le detail au lieu de
                        cocher. Elle est avant l'entete dans le DOM pour que le
                        clavier l'atteigne avant le contenu de la card, et le
                        libelle estReserve plutot que visible pour ne pas
                        peser sur un titre deja long.
                    -->
                    <div class="brouillon__selection">
                        <input type="checkbox" class="brouillon__case" :checked="estSelectionne(item.id)"
                            :aria-label="`Sélectionner ${item.code}`" @click.stop
                            @change.stop="basculerSelection(item.id)" />
                        <span class="visually-hidden">Sélectionner {{ item.code }}</span>
                    </div>

                    <header class="brouillon__entete">
                        <span class="brouillon__code">{{ item.code }}</span>
                        <span class="brouillon__reference">{{ item.reference }}</span>
                    </header>

                    <h2 class="brouillon__designation">{{ item.designation }}</h2>

                    <dl class="brouillon__champs">
                        <div class="brouillon__champ">
                            <dt>Objectif spécifique</dt>
                            <dd v-if="item.objectifSpecifique">
                                {{ item.objectifSpecifique.code }}
                                <span class="brouillon__libelle">{{ item.objectifSpecifique.libelle }}</span>
                            </dd>
                            <dd v-else class="text-muted">Activité non PTA</dd>
                        </div>

                        <div class="brouillon__champ">
                            <dt>Période prévue</dt>
                            <dd>
                                {{ formatDateSeule(item.dateDebutPrevue) }}
                                <span class="brouillon__separateur">→</span>
                                {{ formatDateSeule(item.dateFinPrevue) }}
                            </dd>
                        </div>
                    </dl>

                    <footer class="brouillon__pied">

                        <!--
                            Les deux boutons restent cliquables malgre le lien
                            etire de la carte : @click.stop evite aussi d'ouvrir
                            le detail en meme temps.
                        -->
                        <div class="brouillon__actions">
                            <BaseButton variant="secondary" size="sm" @click.stop="modifier(item.id)">
                                <i class="bi bi-pencil"></i>
                                Modifier
                            </BaseButton>

                            <BaseButton variant="primary" size="sm" :loading="estEnCours(item.id)"
                                @click.stop="soumettre(item)">
                                <i class="bi bi-send"></i>
                                Soumettre en validation
                            </BaseButton>
                        </div>
                    </footer>
                </article>
            </div>
        </div>

        <!--
            ============ CONFIRMATION ============

            Une seule instance pour toutes les demandes de la page : les
            soumissions se suivent, et deux modales superposees rendraient
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
.brouillons {
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
    background: #eef2f6;
    border-radius: 8px;
}

.vue-switch__btn {
    padding: 0.5rem 1.1rem;
    border: none;
    border-radius: 6px;
    background: transparent;
    color: #5a6b7f;
    font-size: 0.85rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.15s ease;
}

.vue-switch__btn--active {
    background: #fff;
    color: #1e88a8;
    box-shadow: 0 1px 3px rgba(26, 43, 60, 0.12);
}

/* --- Recherche et lot --- */
.recherche {
    max-width: 58rem;
}

/*
    La recherche garde une largeur bornee -- une designation se lit mieux sur
    une seule ligne -- et les boutons prennent le reste de la ligne, alignes a
    droite. flex-wrap les fait passer a la ligne sous la recherche sur un
    ecran etroit, plutot que de les ecraser.
*/
.recherche__ligne {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
}

.recherche__champ {
    flex: 1 1 22rem;
    min-width: 0;
}

.lot {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex-wrap: wrap;
}

/*
    Le compteur se trouve dans le lot plutot qu'au-dessus des cards : il
    decrit une selection en cours, pas la liste. Il s'affiche meme a zero, ce
    qui donne l'etat de la selection avant tout clic.
*/
.lot__compte {
    font-size: 0.8rem;
    color: #74879b;
    white-space: nowrap;
}

.autocomplete {
    position: absolute;
    z-index: 20;
    margin: 0.15rem 0 0;
    padding: 0.25rem 0;
    list-style: none;
    background: #fff;
    border: 1px solid #dde5ec;
    border-radius: 8px;
    box-shadow: 0 6px 18px rgba(26, 43, 60, 0.12);
    max-height: 16rem;
    overflow-y: auto;
}

.autocomplete__item--actif {
    background: #eef6fa;
}

.autocomplete button {
    display: block;
    width: 100%;
    padding: 0.45rem 0.75rem;
    border: none;
    background: transparent;
    text-align: left;
    font-size: 0.85rem;
    color: #1a2b3c;
    cursor: pointer;
}

/* --- Card --- */
.brouillon {
    position: relative;
    height: 100%;
    padding: 1rem 1.1rem;
    background: #fff;
    border: 1px solid #e3e9ef;
    border-radius: 10px;
    display: flex;
    flex-direction: column;
    gap: 0.7rem;
    transition: box-shadow 0.15s ease, transform 0.15s ease;
}

.brouillon:hover {
    box-shadow: 0 6px 18px rgba(26, 43, 60, 0.1);
    transform: translateY(-2px);
}

/*
    Le lien etire occupe toute la card, et le bouton du pied passe au-dessus.
    C'est ce qui permet a la card d'etre cliquable sans imbriquer un bouton
    dans un bouton -- structure invalide en HTML, et inatteignable au
    clavier.
*/
.brouillon__lien::after {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
}

.brouillon__lien:focus-visible::after {
    outline: 2px solid #1e88a8;
    outline-offset: 2px;
}

/*
    La case est en haut a droite de la card, hors du flux : la retirer de la
    mise en page evite de decaler l'entete, et le absolute la pose au coin sans
    reserve de place.
*/
.brouillon__selection {
    position: absolute;
    top: 0.55rem;
    right: 0.6rem;
    z-index: 1;
}

.brouillon__case {
    width: 1.05rem;
    height: 1.05rem;
    cursor: pointer;
    accent-color: #1e88a8;
}

.brouillon__case:focus-visible {
    outline: 2px solid #1e88a8;
    outline-offset: 2px;
}

/*
    Une card cochee se distingue par sa bordure, pas par sa couleur de fond :
    le fond est blanc et le hover l'eteint deja, une teinte de fond
    disparaitrait au survol et sur les ecrans en contraste force.
*/
.brouillon--selectionnee {
    border-color: #1e88a8;
    box-shadow: 0 0 0 1px #1e88a8;
}

/*
    L'entete s'ecarte de la case pour qu'un code long ne passe pas dessous.
*/
.brouillon__entete {
    padding-right: 1.6rem;

    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.brouillon__code {
    font-family: 'SFMono-Regular', Consolas, monospace;
    font-size: 0.8rem;
    font-weight: 700;
    color: #1e88a8;
}

.brouillon__reference {
    font-size: 0.75rem;
    color: #74879b;
}

.brouillon__designation {
    font-size: 0.95rem;
    font-weight: 600;
    color: #1a2b3c;
    margin: 0;
    line-height: 1.35;
}

.brouillon__champs {
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    font-size: 0.83rem;
}

.brouillon__champ dt {
    font-size: 0.7rem;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    color: #93a4b6;
    font-weight: 600;
}

.brouillon__champ dd {
    margin: 0.1rem 0 0;
    color: #33465c;
}

.brouillon__libelle {
    color: #74879b;
}

.brouillon__separateur {
    color: #93a4b6;
    margin: 0 0.2rem;
}

.brouillon__pied {
    margin-top: auto;
    padding-top: 0.7rem;
    border-top: 1px solid #eef2f6;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.5rem;
    position: relative;
    z-index: 1;
}

/*
    Modifier et Soumettre côte a côte : la largeur s'adapte a leur contenu,
    ils ne se partagent pas la ligne comme deux elements flex: 1 le
    feraient, ce qui etire les libelles pour rien.
*/
.brouillon__actions {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

/* --- Etat vide --- */
.vide {
    padding: 3rem 1rem;
    text-align: center;
    color: #74879b;
    background: #fff;
    border: 1px dashed #dde5ec;
    border-radius: 10px;
}

.vide i {
    font-size: 2rem;
    opacity: 0.5;
}

.vide__titre {
    margin: 0.6rem 0 0;
    font-weight: 600;
    color: #33465c;
}

.vide__detail {
    margin: 0.2rem 0 0;
    font-size: 0.85rem;
}
</style>

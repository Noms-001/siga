<script setup lang="ts">
/**
 * Activités soumises, en attente de décision.
 *
 * CE QUE CETTE PAGE EST, ET CE QU'ELLE N'EST PAS
 *
 * - Elle liste ce que le backend renvoie sur /activites/a-valider, et rien
 *   d'autre. Le endpoint ne prend AUCUN filtre : ni PTA, ni période, ni
 *   statut. La file est une file d'attente : on la vide, on ne la parcourt pas.
 *
 * - Elle NE VÉRIFIE AUCUNE RÈGLE DE VALIDATION. Ni dates, ni complétude
 *   d'objectif, ni ordre des étapes. Elle envoie une décision et affiche ce
 *   que le backend en fait. La seule règle qui fasse autorité est celle du
 *   serveur, sous verrou, au moment de l'écriture.
 *
 * LE TABLEAU VIENT DE BASETABLE, LA DÉCISION RESTE ICI
 *
 * BaseTable fournit la structure, la pagination, le tri et la sélection
 * multiple. La page garde la logique qui lui appartient : quelle issue
 * envoyer à quel endpoint, comment retirer une ligne après un 409, comment
 * nommer un refus. Mélanger les deux -- mettre une règle métier dans un
 * composant de présentation -- la rendrait invisible et non testable.
 *
 * TROIS ACTIONS, DEUX DESTINATIONS
 *
 * Valider et Rejeter tapent tous deux /{id}/valider -- un booléen les
 * distingue côté serveur. Soumettre à l'étape précédente tape
 * /{id}/soumettre-validation?retour=true, parce que le sens de l'écriture
 * est inverse : l'activité recule au lieu d'avancer.
 *
 * LE MENU D'ACTIONS EST TÉLÉPORTÉ, PAS ABSOLU DANS LA CELLULE
 *
 * Le conteneur du tableau porte overflow-x: auto, qui clippe tout menu en
 * position absolute dès qu'il déborde de la dernière ligne. Le menu est donc
 * un élément unique, rendu dans le body via Teleport, en position fixed, dont
 * la position est calculée au clic. Une seule instance pour toute la page --
 * pas une par ligne -- et la fermeture au clic ailleurs fonctionne par
 * listener document comme avant.
 *
 * LE RETOUR NOMME SON ÉTAPE, OU DISPARAÎT
 *
 * Soumettre à l'étape précédente n'a de sens que si l'activité n'est pas à
 * la première étape du circuit. Quand elle y est, l'action disparaît :
 * un bouton qui échouerait toujours est pire qu'un bouton absent -- il
 * promet une action que rien ne fait. Quand elle en a une, le libellé la
 * nomme : "Soumettre (Contrôle budgétaire)" dit où l'activité va atterrir,
 * "Soumettre à l'étape précédente" laisse l'utilisateur deviner.
 *
 * LE COMMENTAIRE EST AFFICHÉ, PAS SEULEMENT PORTÉ
 *
 * La dernière validation connue porte le motif de la décision précédente --
 * c'est ce que l'auteur a écrit, ou ce qu'un validateur d'amont a justifié.
 * Le lecteur doit pouvoir le lire AVANT de trancher à son tour, d'où une
 * colonne dédiée : un champ stocké mais invisible ne sert à rien.
 *
 * UNE SEULE MODALE POUR LES TROIS DÉCISIONS
 *
 * Valider, Rejeter et Soumettre passent tous par DecisionActiviteModal.
 * C'est un écran unique qui affiche la conséquence de la décision et
 * permet un commentaire optionnel. Un seul composant à maintenir, un seul
 * formulaire à faire évoluer, et trois tonalités qui cohabitent dans la
 * même mise en page.
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

import { BaseButton, BaseTable } from '@/components/base'
import type { TableColumn } from '@/components/base/BaseTable/BaseTable.types'
import { DecisionActiviteModal } from '@/components/activite'
import * as activiteService from '@/services/activite'
import type {
    ActiviteReference,
    ActiviteListItem,
    DecisionValidation,
} from '@/types/activite'
import { formatDateSeule } from '@/utils/date'

// ------------------------------------------------------------------
// Types locaux
// ------------------------------------------------------------------

/** Ligne de la table : l'item standard, plus son étage dans le circuit. */
interface LigneAValider extends ActiviteListItem {
    etapeCourante: ActiviteReference | null
    etapeSuivante: ActiviteReference | null
    etapePrecedente: ActiviteReference | null
}

/** Verdict d'une décision backend, tel que la page doit y réagir. */
type Verdict = 'reussi' | 'refus' | 'panne'

/**
 * Décision portée par la modale en cours.
 *
 * `issue` ne peut être que l'une des trois décisions réelles -- jamais
 * EN_ATTENTE_VALIDATION, qui n'est pas un acte. La page n'ouvre la modale
 * que pour décider.
 */
interface ModaleEnCours {
    issue: DecisionValidation
    ids: number[]
}

// ------------------------------------------------------------------
// Constantes
// ------------------------------------------------------------------

/**
 * Colonnes du tableau.
 *
 * Les clés `activite`, `periode`, `commentaire` et `actions` ne
 * correspondent à aucune propriété réelle : elles n'existent que pour
 * désigner un slot de rendu. `etapeCourante` et `etapeSuivante` existent en
 * revanche comme propriétés, ce qui permet à BaseTable de calculer
 * l'alignement automatiquement.
 *
 * LES CLASSES `colonne-activite` ET `colonne-commentaire` NE SONT PAS
 * DÉCORATIVES
 *
 * BaseTable pose `white-space: nowrap` sur tous les <td>. Sans lever cette
 * règle sur les deux colonnes qui portent du texte libre, un titre ou un
 * motif long reste sur une ligne et pousse les colonnes suivantes hors de
 * l'écran. Les classes permettent de cibler ces <td> précis depuis le CSS
 * scopé de la page, via :deep().
 *
 * LA COLONNE `commentaire` PORTE LE MOTIF DE LA DERNIÈRE DÉCISION
 *
 * C'est là qu'atterrit ce qu'un validateur d'amont a écrit en rejetant ou
 * en renvoyant pour modification. Le validateur suivant doit le lire avant
 * de trancher : le champ est stocké, il est donc affiché.
 */
const COLONNES: TableColumn<LigneAValider>[] = [
    { key: 'activite', label: 'Activité', class: 'colonne-activite', align: 'start' },
    { key: 'etapeCourante', label: 'Étape courante' },
    { key: 'etapeSuivante', label: 'Étape suivante' },
    { key: 'periode', label: 'Période prévue' },
    { key: 'commentaire', label: 'Commentaire', class: 'colonne-commentaire' },
    { key: 'actions', label: 'Actions', align: 'center' },
]

/** Largeur du menu, dont dépend le calcul de position au clic. */
const LARGEUR_MENU = 240

// ------------------------------------------------------------------
// État
// ------------------------------------------------------------------

const lignes = ref<LigneAValider[]>([])
const chargement = ref(false)
const erreur = ref<string | null>(null)
const succes = ref<string | null>(null)

/**
 * Ids en cours de décision.
 *
 * Un Set et non un booleen global : plusieurs lignes sont affichees, et deux
 * decisions ne doivent pas se neutraliser. Le backend refuse une double
 * decision (409), mais il ne faut pas faire attendre l'utilisateur pour le
 * decouvrir.
 */
const enCours = ref<Set<number>>(new Set())

/**
 * Ids coches.
 *
 * Source de vérité conservée en Set d'ids : BaseTable expose selectedItems
 * en tableau, mais la page n'a besoin que des ids pour retirer, refuser,
 * nommer un échec. La conversion est faite par le computed ci-dessous, dans
 * un seul sens, à un seul endroit.
 */
const selection = ref<Set<number>>(new Set())

/** Ligne dont le menu est ouvert, ou null. Une seule à la fois. */
const menuItem = ref<LigneAValider | null>(null)

/** Position du menu à l'écran, recalculée à chaque ouverture. */
const positionMenu = ref({ top: 0, left: 0 })

/** Modale de décision en cours : issue fixe, ids concernes. */
const modale = ref<ModaleEnCours | null>(null)

// ------------------------------------------------------------------
// En-tête : date du jour
// ------------------------------------------------------------------

const dateDuJour = computed(() =>
    new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric',
    })
        .format(new Date())
        .toUpperCase()
)

// ------------------------------------------------------------------
// Pont sélection <-> BaseTable
// ------------------------------------------------------------------

/**
 * Tableau d'items pour BaseTable, dérivé du Set d'ids.
 *
 * Le getter reconstruit le tableau depuis `lignes` : un item retiré du
 * tableau ne peut pas rester sélectionné, et un item rechargé sous le même
 * id réapparaît coché -- ce qui est le comportement voulu entre deux
 * chargements.
 *
 * Le setter traduit en Set d'ids ce que BaseTable émet (coche individuelle,
 * case d'en-tête, tout-décocher). Aucune copie superflue : le Set est
 * reconstruit d'un coup, ce qui déclenche le rendu Vue (comparaison par
 * référence).
 */
const itemsSelectionnes = computed<LigneAValider[]>({
    get: () => lignes.value.filter(l => selection.value.has(l.id)),
    set: (items) => {
        selection.value = new Set(items.map(l => l.id))
    },
})

// ------------------------------------------------------------------
// Dernière décision : lecture et conséquences
// ------------------------------------------------------------------

/**
 * Vrai si la dernière décision connue est un retour pour modification.
 *
 * L'information voyage sous forme de chaîne (`derniereDecision`) et non
 * d'énum : le backend peut y placer plusieurs libellés selon l'origine du
 * retour. On teste par sous-chaîne, insensible à la casse, pour ne pas
 * dépendre d'une valeur exacte qui peut évoluer côté serveur.
 *
 * REJETER UNE ACTIVITÉ DÉJÀ RENVOYÉE N'A PAS DE SENS
 *
 * Elle a déjà été traitée une fois ; l'auteur n'a pas encore eu la main
 * pour corriger. La rejeter à nouveau ne ferait qu'empiler un second refus
 * sans que personne n'ait rien pu faire entre les deux. Le bouton disparaît
 * plutôt que d'être désactivé : un contrôle actif sans effet est pire qu'un
 * contrôle absent, il promet une action que rien ne fera.
 */
function decisionContientRetour(item: LigneAValider): boolean {
    const decision = item.derniereValidation?.derniereDecision
    return typeof decision === 'string'
        && decision.toLowerCase().includes('retour')
}

/**
 * Vrai dès qu'au moins une ligne sélectionnée peut encore être rejetée.
 *
 * Le bouton de lot suit la même règle que le menu de ligne. Le masquer
 * quand toutes les lignes sélectionnées sont en retour évite de promettre
 * une action qui ne s'appliquerait à aucune d'elles.
 *
 * Une sélection mixte reste rejetable : les lignes en retour seront
 * ignorées par l'appel unitaire, les autres seront traitées. Le bouton
 * apparaît dès qu'il y a au moins une cible valable.
 */
const peutRejeterSelection = computed<boolean>(() => {
    if (!selection.value.size) return false

    for (const id of selection.value) {
        const item = lignes.value.find(l => l.id === id)
        if (item && !decisionContientRetour(item)) return true
    }

    return false
})

// ------------------------------------------------------------------
// Étapes précédentes de la sélection
// ------------------------------------------------------------------

/**
 * Libellé d'étape précédente commun aux lignes sélectionnées.
 *
 * TROIS CAS, ET LE LIBELLÉ DIT TOUJOURS LA VÉRITÉ
 *
 * - Aucune ligne sélectionnée n'a d'étape précédente → null. La barre
 *   masque le bouton : l'action n'aurait pas de sens pour un lot
 *   entièrement à la première étape du circuit.
 *
 * - Toutes celles qui en ont une partagent la même → on renvoie son nom,
 *   et le bouton dit exactement où les activités vont atterrir.
 *
 * - Elles diffèrent → chaîne vide (falsy mais pas null). Le libellé
 *   retombe sur "Soumettre" sans parenthèse : annoncer une étape précise
 *   quand trois activités vont à trois endroits différents serait faux.
 *
 * Le filtre `if (libelle)` est ce qui gère le cas mixte -- certaines lignes
 * avec étape précédente, d'autres non. Seules les premières comptent pour
 * le libellé, et `soumettreSelection` ne soumettra qu'elles.
 */
const etapePrecedenteSelection = computed<string | null>(() => {
    if (!selection.value.size) return null

    const etapes = new Set<string>()

    for (const id of selection.value) {
        const item = lignes.value.find(l => l.id === id)
        const libelle = item?.etapePrecedente?.libelle

        if (libelle) {
            etapes.add(libelle)
        }
    }

    if (etapes.size === 0) return null

    return etapes.size === 1 ? [...etapes][0]! : ''
})

// ------------------------------------------------------------------
// Chargement
// ------------------------------------------------------------------

async function charger(): Promise<void> {
    if (chargement.value) return

    chargement.value = true
    erreur.value = null

    let reponse

    try {
        reponse = await activiteService.listerActivitesAValider()
    } catch {
        erreur.value = 'Connexion au serveur impossible. Réessayez dans un instant.'
        lignes.value = []
        chargement.value = false
        return
    }

    if (!reponse.success) {
        erreur.value = reponse.error
        lignes.value = []
        chargement.value = false
        return
    }

    /*
     * Le backend groupe par étape : chaque entree de content est un objet a
     * UNE cle -- la designation de l'etape de validation -- dont la valeur
     * est l'activite, avec ses etapes courante / suivante / precedente.
     *
     * On aplatit en extrayant la designation de la cle : elle porte le nom
     * de l'etape meme quand l'objet etapeCourante est absent du DTO.
     */
    const aplaties: LigneAValider[] = []

    for (const entree of reponse.data.content) {
        for (const [designationEtape, item] of Object.entries(entree)) {
            aplaties.push({
                ...item,
                etapeCourante: item.etapeCourante
                    ?? { id: 0, libelle: designationEtape },
                etapeSuivante: item.etapeSuivante ?? null,
                etapePrecedente: item.etapePrecedente ?? null,
            })
        }
    }

    lignes.value = aplaties
    chargement.value = false
}

// ------------------------------------------------------------------
// Menu d'actions
// ------------------------------------------------------------------

/**
 * Ouvrir ou fermer le menu d'une ligne.
 *
 * La position est calculée depuis le rectangle du bouton et non depuis un
 * parent : le menu étant en fixed, `top` et `left` sont des coordonnées
 * écran. On borne la gauche pour que le menu reste dans la fenêtre même
 * quand le bouton est proche du bord droit.
 */
function basculerMenu(item: LigneAValider, event: MouseEvent): void {
    event.stopPropagation()

    if (menuItem.value?.id === item.id) {
        menuItem.value = null
        return
    }

    const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()

    positionMenu.value = {
        top: rect.bottom + 4,
        left: Math.max(
            8,
            Math.min(rect.right - LARGEUR_MENU, window.innerWidth - LARGEUR_MENU - 8)
        ),
    }

    menuItem.value = item
}

/**
 * Fermer le menu au clic ailleurs, au scroll, ou à Echap.
 *
 * Trois listeners plutôt qu'un backdrop : un backdrop plein écran capterait
 * les clics destinés à d'autres contrôles, et resterait incompatible avec le
 * Teleport. Le scroll est capté en phase de capture (`true`) pour intercepter
 * aussi les scrolls de conteneurs internes, pas seulement celui de window.
 */
function fermerMenu(): void {
    menuItem.value = null
}

function surToucheEchap(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
        fermerMenu()
    }
}

onMounted(() => {
    document.addEventListener('click', fermerMenu)
    document.addEventListener('keydown', surToucheEchap)
    window.addEventListener('scroll', fermerMenu, true)
    window.addEventListener('resize', fermerMenu)
    void charger()
})

onBeforeUnmount(() => {
    document.removeEventListener('click', fermerMenu)
    document.removeEventListener('keydown', surToucheEchap)
    window.removeEventListener('scroll', fermerMenu, true)
    window.removeEventListener('resize', fermerMenu)
})

/** Style du menu, recalculé à chaque déplacement. */
const styleMenu = computed(() => ({
    top: `${positionMenu.value.top}px`,
    left: `${positionMenu.value.left}px`,
    width: `${LARGEUR_MENU}px`,
}))

// ------------------------------------------------------------------
// Sélection
// ------------------------------------------------------------------

function toutSelectionner(): void {
    selection.value = new Set(lignes.value.map(l => l.id))
}

function toutDeselectionner(): void {
    selection.value = new Set()
}

// ------------------------------------------------------------------
// Décision backend
// ------------------------------------------------------------------

/**
 * Une seule ecriture, quel que soit le nombre de lignes concernees.
 *
 * Le retour est un verdict, pas une reponse : ce que la page doit faire
 * ensuite ne depend que de lui -- retirer la ligne, garder un refus a
 * afficher, ou interrompre le lot parce que la connexion est morte.
 * Eparpiller cette logique ici la repeterait trois fois, et une repetition
 * diverge toujours un jour.
 */
async function envoyerDecision(
    id: number,
    issue: DecisionValidation,
    commentaire: string
): Promise<Verdict> {
    enCours.value = new Set(enCours.value).add(id)
    erreur.value = null

    let reponse

    try {
        if (issue === 'VALIDE') {
            reponse = await activiteService.validerActivite(id, false, null)
        } else if (issue === 'REJETE') {
            reponse = await activiteService.validerActivite(id, true, commentaire)
        } else {
            reponse = await activiteService.soumettreValidationRetour(id, commentaire)
        }
    } catch {
        enCours.value = new Set([...enCours.value].filter(x => x !== id))
        erreur.value =
            'Connexion au serveur impossible. La décision n\'a pas été enregistrée.'
        return 'panne'
    }

    enCours.value = new Set([...enCours.value].filter(x => x !== id))

    if (reponse.success) {
        return 'reussi'
    }

    erreur.value = reponse.error

    /*
     * 409 : l'activite a deja ete tranchee (deux validateurs ouverts sur la
     * meme file). Elle ne reviendra pas dans la file, donc on la retire.
     * Les autres refus (400 motif manquant, 403 hors perimetre) laissent la
     * ligne en place : l'utilisateur doit pouvoir corriger et reessayer.
     */
    if (reponse.status === 409) {
        retirer(id)
    }

    return 'refus'
}

/**
 * Retire une ligne du tableau courant.
 *
 * Retire aussi l'id de la selection : sinon un « tout deselectionner »
 * suivant remettrait un id fantome dans le Set, et un item supprimé
 * referait surface au prochain chargement sans raison.
 */
function retirer(id: number): void {
    lignes.value = lignes.value.filter(l => l.id !== id)
    selection.value = new Set([...selection.value].filter(x => x !== id))
}

/**
 * Code lisible d'une activite, pour nommer un refus sans relire la liste.
 *
 * Releve AVANT tout retrait, sinon la ligne a disparu et il ne resterait
 * qu'un identifiant que l'utilisateur ne peut pas relier a rien.
 */
function codeDe(id: number): string {
    return lignes.value.find(l => l.id === id)?.code ?? `#${id}`
}

// ------------------------------------------------------------------
// Décisions unitaires
// ------------------------------------------------------------------

function validerLigne(item: LigneAValider): void {
    fermerMenu()
    modale.value = { issue: 'VALIDE', ids: [item.id] }
}

function rejeterLigne(item: LigneAValider): void {
    fermerMenu()
    modale.value = { issue: 'REJETE', ids: [item.id] }
}

function soumettreLigne(item: LigneAValider): void {
    fermerMenu()
    modale.value = { issue: 'RETOUR_MODIFICATION', ids: [item.id] }
}

// ------------------------------------------------------------------
// Décisions groupées
// ------------------------------------------------------------------

function validerSelection(): void {
    if (!selection.value.size) return
    modale.value = { issue: 'VALIDE', ids: [...selection.value] }
}

/**
 * Rejeter les lignes sélectionnées qui peuvent encore l'être.
 *
 * ON FILTRE AVANT D'OUVRIR LA MODALE
 *
 * Une ligne déjà renvoyée pour modification ne peut pas être rejetée à
 * nouveau. L'inclure dans le compte ferait afficher "3 activités seront
 * rejetées" alors que deux seulement le seraient -- le seul endroit où
 * l'utilisateur peut encore annuler doit dire la vérité.
 *
 * Le bouton n'apparaît de toute façon que si au moins une ligne peut
 * l'être (voir peutRejeterSelection), donc cette fonction n'est jamais
 * appelée avec une liste vide.
 */
function rejeterSelection(): void {
    const ids = lignes.value
        .filter(l => selection.value.has(l.id) && !decisionContientRetour(l))
        .map(l => l.id)

    if (!ids.length) return

    modale.value = { issue: 'REJETE', ids }
}

/**
 * Ouvrir la modale de retour sur les lignes qui ont une étape précédente.
 *
 * ON FILTRE AVANT D'OUVRIR LA MODALE
 *
 * Soumettre une activité déjà à la première étape n'a pas de sens, et
 * l'inclure dans le compte ferait afficher "3 activités seront renvoyées"
 * alors qu'une seule partirait. Le compte doit dire ce qui va réellement
 * se passer -- c'est le seul endroit où l'utilisateur peut encore annuler.
 *
 * Le bouton n'apparaît de toute façon que si au moins une ligne
 * sélectionnée a une étape précédente (voir etapePrecedenteSelection), donc
 * cette fonction n'est jamais appelée avec une liste vide.
 */
function soumettreSelection(): void {
    const ids = lignes.value
        .filter(l => selection.value.has(l.id) && l.etapePrecedente)
        .map(l => l.id)

    if (!ids.length) return

    modale.value = { issue: 'RETOUR_MODIFICATION', ids }
}

// ------------------------------------------------------------------
// Modale (motif)
// ------------------------------------------------------------------

function fermerModale(): void {
    modale.value = null
}

/**
 * Envoyer la décision une fois le motif saisi dans la modale.
 *
 * PAS DE SECONDE CONFIRMATION
 *
 * La modale affiche la conséquence, permet un commentaire, et dispose d'un
 * bouton Annuler. C'est suffisant : le geste est délibéré, la conséquence
 * est écrite noir sur blanc, et revenir en arrière reste possible tant que
 * le bouton n'a pas été cliqué. Empiler une seconde confirmation par-dessus
 * ferait perdre à la première sa valeur -- l'utilisateur s'habituerait à
 * cliquer deux fois sans lire.
 *
 * Le verbe du message final est choisi selon l'issue, pour que le retour à
 * l'utilisateur nomme ce qui vient de se passer.
 */
function surModaleConfirmee(commentaire: string): void {
    if (!modale.value) return

    const { issue, ids } = modale.value
    modale.value = null

    void executerLot(ids, issue, commentaire)
}

// ------------------------------------------------------------------
// Exécution d'un lot
// ------------------------------------------------------------------

/**
 * Envoyer une decision a plusieurs activites, l'une apres l'autre.
 *
 * SEQUENTIEL ET NON PARALLELE : une decision est une ecriture d'etat, et un
 * lot simultane verrait le meme verrou plusieurs fois -- au mieux pour rien,
 * au pire avec un 409 sur une activite pourtant bien dans la file.
 *
 * UNE REQUETE PAR ACTIVITE, et non une qui les regroupe : le backend n'expose
 * qu'un endpoint par activite. Une panne ou un refus n'annule donc pas les
 * autres, et chaque ligne est traitee selon son propre resultat.
 *
 * UNE PANNE INTERROMPT LE LOT : le reste partirait sur une connexion morte,
 * et l'utilisateur verrait des lignes disparaitre sans confirmation. La
 * selection est alors conservee pour qu'il puisse reessayer d'un clic.
 */
async function executerLot(
    ids: number[],
    issue: DecisionValidation,
    commentaire: string
): Promise<void> {
    erreur.value = null
    succes.value = null

    let reussies = 0
    const refus: string[] = []

    for (const id of ids) {
        if (enCours.value.has(id)) continue

        const verdict = await envoyerDecision(id, issue, commentaire)

        if (verdict === 'panne') {
            // La selection est conservee : un clic suffira a reessayer.
            break
        }

        if (verdict === 'refus') {
            refus.push(`${codeDe(id)} : décision refusée`)
            continue
        }

        reussies += 1
        retirer(id)
    }

    const verbe = issue === 'VALIDE'
        ? 'validée(s) et publiée(s) au suivi'
        : issue === 'REJETE'
            ? 'rejetée(s)'
            : 'renvoyée(s) à l\'étape précédente'

    if (refus.length) {
        succes.value = reussies > 0
            ? `${reussies} activité(s) ${verbe}, ${refus.length} refusée(s).`
            : null
        erreur.value = refus.join(' ')
        return
    }

    if (reussies > 0) {
        succes.value = `${reussies} activité(s) ${verbe}.`
    }
}
</script>

<template>
    <div class="a-valider">
        <!-- ============ EN-TÊTE ============ -->
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>Activités à valider</h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    Activités soumises, en attente de votre décision
                </p>
            </div>

            <div class="welcome-row__right">
                <BaseButton
                    variant="secondary"
                    :loading="chargement"
                    :disabled="chargement"
                    @click="charger"
                >
                    <i class="bi bi-arrow-clockwise"></i>
                    Actualiser
                </BaseButton>
            </div>
        </header>

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

        <!-- ============ CHARGEMENT ============ -->
        <div v-if="chargement && !lignes.length && !erreur" class="detail-state">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="mt-3 mb-0">Chargement de la file de validation…</p>
        </div>

        <!-- ============ BARRE DE LOT ============ -->
        <div
            v-else-if="selection.size > 0"
            class="lot"
            role="toolbar"
            aria-label="Actions sur la sélection"
        >
            <span class="lot__compte" role="status">
                {{ selection.size }} / {{ lignes.length }} sélectionnée(s)
            </span>

            <div class="lot__actions">
                <BaseButton variant="secondary" size="sm" @click="toutSelectionner">
                    <i class="bi bi-check2-square"></i>
                    Tout sélectionner
                </BaseButton>

                <BaseButton variant="secondary" size="sm" @click="toutDeselectionner">
                    <i class="bi bi-x-square"></i>
                    Tout désélectionner
                </BaseButton>

                <span class="lot__separateur" aria-hidden="true"></span>

                <BaseButton
                    variant="success"
                    size="sm"
                    :loading="enCours.size > 0"
                    :disabled="enCours.size > 0"
                    @click="validerSelection"
                >
                    <i class="bi bi-check-circle"></i>
                    Valider
                </BaseButton>

                <BaseButton
                    v-if="peutRejeterSelection"
                    variant="danger"
                    size="sm"
                    :disabled="enCours.size > 0"
                    @click="rejeterSelection"
                >
                    <i class="bi bi-x-circle"></i>
                    Rejeter
                </BaseButton>

                <BaseButton
                    v-if="etapePrecedenteSelection !== null"
                    variant="warning"
                    size="sm"
                    :disabled="enCours.size > 0"
                    @click="soumettreSelection"
                >
                    <i class="bi bi-arrow-counterclockwise"></i>
                    Soumettre{{ etapePrecedenteSelection ? ` (${etapePrecedenteSelection})` : '' }}
                </BaseButton>
            </div>
        </div>

        <!-- ============ ÉTAT VIDE ============ -->
        <div v-if="!chargement && !lignes.length && !erreur" class="vide">
            <i class="bi bi-check2-all"></i>
            <p class="vide__titre">Aucune activité en attente de validation</p>
            <p class="vide__detail">
                Les activités soumises apparaîtront ici jusqu'à leur décision
            </p>
        </div>

        <!-- ============ TABLEAU ============ -->
        <BaseTable
            v-if="lignes.length"
            v-model:selectedItems="itemsSelectionnes"
            :items="lignes"
            :columns="COLONNES"
            :loading="chargement"
            :page-size="20"
            :sortable="false"
            multi-select
            custom-class="a-valider__table"
        >
            <template #cell-activite="{ item }">
                <router-link
                    class="activite__lien"
                    :to="{ name: 'activite-detail', params: { id: String(item.id) } }"
                >
                    <span class="activite__entete">
                        <span class="activite__code">{{ item.code }}</span>
                        <span v-if="item.reference" class="activite__ref">
                            {{ item.reference }}
                        </span>
                    </span>
                    <span class="activite__designation">{{ item.designation }}</span>
                </router-link>
            </template>

            <template #cell-etapeCourante="{ item }">
                <span v-if="item.etapeCourante" class="etape etape--courante">
                    {{ item.etapeCourante.libelle }}
                </span>
                <span v-else class="text-muted">—</span>
            </template>

            <template #cell-etapeSuivante="{ item }">
                <span v-if="item.etapeSuivante" class="etape etape--suivante">
                    {{ item.etapeSuivante.libelle }}
                </span>
                <span v-else class="text-muted">—</span>
            </template>

            <template #cell-periode="{ item }">
                <template v-if="item.dateDebutPrevue">
                    <span>{{ formatDateSeule(item.dateDebutPrevue) }}</span>
                    <span class="periode__separateur">→</span>
                    <span>{{ formatDateSeule(item.dateFinPrevue) }}</span>
                </template>
                <span v-else class="text-muted">—</span>
            </template>

            <template #cell-commentaire="{ item }">
                <span
                    v-if="item.derniereValidation?.commentaire"
                    class="commentaire"
                    :title="item.derniereValidation.commentaire"
                >
                    {{ item.derniereValidation.commentaire }}
                </span>
                <span v-else class="text-muted">—</span>
            </template>

            <template #cell-actions="{ item }">
                <button
                    type="button"
                    class="menu__toggle"
                    :aria-expanded="menuItem?.id === item.id"
                    aria-haspopup="menu"
                    :aria-label="`Actions pour ${item.code}`"
                    :disabled="enCours.has(item.id)"
                    @click="basculerMenu(item, $event)"
                >
                    <i
                        v-if="enCours.has(item.id)"
                        class="bi bi-arrow-repeat spin"
                        aria-hidden="true"
                    ></i>
                    <i v-else class="bi bi-three-dots-vertical" aria-hidden="true"></i>
                </button>
            </template>
        </BaseTable>

        <!-- ============ MENU D'ACTIONS (teleporté) ============ -->
        <Teleport to="body">
            <ul
                v-if="menuItem"
                class="menu-global"
                role="menu"
                :style="styleMenu"
                @click.stop
            >
                <li role="none">
                    <button
                        type="button"
                        role="menuitem"
                        class="menu-global__item menu-global__item--valider"
                        @click="validerLigne(menuItem)"
                    >
                        <i class="bi bi-check-circle"></i>
                        Valider
                    </button>
                </li>

                <li v-if="!decisionContientRetour(menuItem)" role="none">
                    <button
                        type="button"
                        role="menuitem"
                        class="menu-global__item menu-global__item--rejeter"
                        @click="rejeterLigne(menuItem)"
                    >
                        <i class="bi bi-x-circle"></i>
                        Rejeter
                    </button>
                </li>

                <li v-if="menuItem.etapePrecedente" role="none">
                    <button
                        type="button"
                        role="menuitem"
                        class="menu-global__item menu-global__item--retour"
                        @click="soumettreLigne(menuItem)"
                    >
                        <i class="bi bi-arrow-counterclockwise"></i>
                        Soumettre ({{ menuItem.etapePrecedente.libelle }})
                    </button>
                </li>
            </ul>
        </Teleport>

        <!-- ============ MODALE DE DÉCISION ============ -->
        <DecisionActiviteModal
            v-if="modale"
            :modelValue="true"
            :issue="modale.issue === 'VALIDE'
                ? 'VALIDE'
                : modale.issue === 'REJETE'
                    ? 'REJETE'
                    : 'RETOUR_MODIFICATION'"
            :nombre="modale.ids.length"
            :loading="enCours.size > 0"
            @update:modelValue="fermerModale"
            @confirme="surModaleConfirmee"
        />
    </div>
</template>

<style scoped>
.a-valider {
    --primary-color: #1a2a6c;
    --secondary-color: #2a3f94;
    --bg-color: #f5f7fa;
    --surface-color: #fff;
    --card-bg: #fff;
    --text-color: #15233c;
    --text-muted-custom: #6b7280;
    --border-color: #e6ebf3;
    --radius-xl: 16px;
    --dts-navy: #1a2a6c;
    --dts-blue: #1769c2;
    --dts-muted: #7b8799;
    --dts-surface: #fff;
    --dts-border: #e6ebf3;
    min-height: calc(100vh - 68px);
    padding: 34px 38px 24px;
    color: #15233c;
    font-family: "DM Sans", "Segoe UI", sans-serif;
    background:
        radial-gradient(circle at 12% 8%, rgba(26, 42, 108, .055) 0, transparent 38%),
        radial-gradient(circle at 92% 94%, rgba(212, 175, 55, .065) 0, transparent 36%),
        #f5f7fa;
}

/* ============================================================
   EN-TÊTE — le welcome-row global a margin-bottom: 4px,
   insuffisant pour une page racine. On rétablit 1.5rem.
   Le sélecteur composé gagne sur le .welcome-row global.
   ============================================================ */
.a-valider .welcome-row {
    margin-bottom: 1.5rem;
}

/* ============================================================
   ÉTAT DE CHARGEMENT
   ============================================================ */
.detail-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 3.5rem 1.5rem;
    text-align: center;
    color: #7b8799;
}

/* ============================================================
   BARRE DE LOT
   ============================================================ */
.lot {
    display: flex;
    align-items: center;
    gap: 1rem;
    flex-wrap: wrap;
    padding: 0.7rem 1rem;
    margin-bottom: 1rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 16px;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .04),
        0 8px 24px rgba(26, 42, 108, .06);
}

.lot__compte {
    font-size: 0.9rem;
    font-weight: 600;
    color: #1a2a6c;
    white-space: nowrap;
}

.lot__actions {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
    margin-left: auto;
}

.lot__separateur {
    width: 1px;
    align-self: stretch;
    background: #e6ebf3;
    margin: 0 0.25rem;
}

/* ============================================================
   ÉTAT VIDE
   ============================================================ */
.vide {
    text-align: center;
    padding: 3rem 1rem;
    color: #7b8799;
}

.vide i {
    font-size: 2.5rem;
    opacity: 0.4;
}

.vide__titre {
    margin: 0.75rem 0 0;
    font-size: 1rem;
    font-weight: 600;
    color: #1a2a6c;
}

.vide__detail {
    margin: 0.25rem 0 0;
    font-size: 0.9rem;
}

/* ============================================================
   ADAPTATION DU TABLEAU
   ============================================================ */
.a-valider :deep(.base-table-container) table {
    min-width: 860px;
}

/* BaseTable pose white-space: nowrap sur TOUS les <td>.
   On lève la règle sur les deux colonnes de texte libre :
   - colonne-activite : désignation longue
   - colonne-commentaire : motif long
   Sans ça, un titre ou un motif long reste sur une ligne
   et pousse les colonnes suivantes hors de l'écran. */
.a-valider :deep(.colonne-activite) {
    white-space: normal;
    min-width: 220px;
    max-width: 360px;
}

.a-valider :deep(.colonne-commentaire) {
    white-space: normal;
    max-width: 260px;
    min-width: 180px;
}

/* ============================================================
   TEXTE DE LA DÉSIGNATION
   ============================================================ */
.activite__lien {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
    text-decoration: none;
    color: inherit;
}

.activite__lien:hover .activite__designation {
    color: #4a6cf7;
}

.activite__entete {
    display: flex;
    align-items: baseline;
    gap: 0.5rem;
    flex-wrap: wrap;
}

.activite__code {
    font-size: 0.78rem;
    font-weight: 700;
    color: #3b63d9;
    letter-spacing: 0.02em;
}

.activite__ref {
    font-size: 0.72rem;
    color: #8a95a8;
}

.activite__designation {
    font-size: 0.9rem;
    font-weight: 500;
    line-height: 1.3;
    transition: color 0.12s;
    overflow-wrap: anywhere;
}

/* ============================================================
   BADGES D'ÉTAPE
   ============================================================ */
.etape {
    display: inline-block;
    padding: 0.2rem 0.55rem;
    border-radius: 999px;
    font-size: 0.75rem;
    font-weight: 600;
    white-space: nowrap;
}

.etape--courante {
    background: #eaf0ff;
    color: #3b63d9;
}

.etape--suivante {
    background: #fff6dd;
    color: #b8860b;
}

/* ============================================================
   PÉRIODE
   ============================================================ */
.periode__separateur {
    margin: 0 0.35rem;
    color: #8a95a8;
}

/* ============================================================
   COMMENTAIRE DE LA DERNIÈRE VALIDATION
   Le white-space: normal vient du <td> via .colonne-commentaire.
   Le clamp à deux lignes borne la hauteur, le title lit le texte
   complet au survol.
   ============================================================ */
.commentaire {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    font-size: 0.85rem;
    line-height: 1.35;
    color: #40516b;
    overflow-wrap: anywhere;
}

/* ============================================================
   BOUTON DU MENU
   ============================================================ */
.menu__toggle {
    width: 32px;
    height: 32px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: none;
    background: transparent;
    border-radius: 8px;
    color: #7b8799;
    cursor: pointer;
    transition: background-color 0.12s, color 0.12s;
}

.menu__toggle:hover:not(:disabled) {
    background: #f3f6fc;
    color: #1a2a6c;
}

.menu__toggle:disabled {
    cursor: wait;
    color: #b6c6d5;
}

.menu__toggle[aria-expanded='true'] {
    background: #edf1fd;
    color: #1a2a6c;
}

.spin {
    animation: spin 1s linear infinite;
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
}

/* ============================================================
   MENU GLOBAL (teleporté dans le body)
   ============================================================ */
.menu-global {
    position: fixed;
    z-index: 1000;
    padding: 0.35rem;
    margin: 0;
    list-style: none;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 12px;
    box-shadow: 0 12px 28px rgba(26, 42, 108, .16);
}

.menu-global__item {
    display: flex;
    align-items: center;
    gap: 0.55rem;
    width: 100%;
    padding: 0.5rem 0.65rem;
    border: none;
    background: transparent;
    border-radius: 8px;
    font-size: 0.88rem;
    text-align: left;
    color: #40516b;
    cursor: pointer;
    transition: background-color 0.12s, color 0.12s;
}

.menu-global__item:hover {
    background: #f3f6fc;
}

.menu-global__item--valider:hover {
    background: #e4f7f0;
    color: #138a68;
}

.menu-global__item--rejeter:hover {
    background: #fff0f0;
    color: #d84e5b;
}

.menu-global__item--retour:hover {
    background: #fff6dd;
    color: #b8860b;
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 1199.98px) {
    .a-valider {
        padding-left: 24px;
        padding-right: 24px;
    }
}

@media (max-width: 767.98px) {
    .a-valider {
        padding: 24px 16px 20px;
    }
}

@media (max-width: 575.98px) {
    .a-valider {
        padding: 20px 12px 16px;
    }
}

@media (prefers-reduced-motion: reduce) {
    .spin,
    .menu__toggle {
        animation: none !important;
        transition: none !important;
    }
}
</style>
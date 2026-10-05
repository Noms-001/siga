<script setup lang="ts">
/**
 * Activités en cours de rédaction, à soumettre à la validation.
 *
 * STRUCTURE IDENTIQUE À /a-valider, CONTENU OPPOSÉ
 *
 * Même tableau, même menu téléporté, même logique de retrait après écriture,
 * même modale de confirmation. Ce qui change, c'est ce qu'on y fait :
 * /a-valider tranche (Valider / Rejeter / Renvoyer), /a-soumettre fait
 * circuler l'activité dans le circuit, dans un sens ou dans l'autre.
 *
 * DEUX DESTINATIONS, UN SEUL ENDPOINT
 *
 * Les deux actions tapent /{id}/soumettre-validation. Le paramètre `retour`
 * distingue : sans, l'activité avance vers l'étape suivante ; avec, elle
 * recule vers l'étape précédente.
 *
 * LES BOUTONS NOMMENT LEUR ÉTAPE
 *
 * "Soumettre à l'étape suivante" laisse l'utilisateur deviner où l'activité
 * va atterrir. "Soumettre à [Contrôle budgétaire]" le dit. Le libellé vient
 * du DTO, pas d'une table locale : c'est le serveur qui sait où mène
 * chaque flèche.
 *
 * QUAND IL N'Y A PLUS D'ÉTAPE SUIVANTE, L'ACTION CHANGE DE NOM
 *
 * Une activité à la dernière étape du circuit n'a nulle part où aller :
 * l'action vers l'avant n'est plus une soumission, c'est la validation
 * finale. Le backend ne fait pas la différence (le même endpoint gère les
 * deux cas), mais l'utilisateur, lui, doit savoir ce qu'il fait. Le libellé
 * bascule donc sur "Valider (validation finale)" quand etapeSuivante est
 * nulle -- et la modale de confirmation reste celle de toutes les autres
 * décisions.
 *
 * SÉLECTION MULTIPLE, COMME DANS /a-valider
 *
 * Coche individuelle ou "tout sélectionner", puis jusqu'à trois actions de
 * lot : soumettre, valider en final, renvoyer. Chaque bouton n'apparaît que
 * si au moins une ligne sélectionnée peut effectivement le porter.
 *
 * LE MENU EST TÉLÉPORTÉ, PAS ABSOLU DANS LA CELLULE
 *
 * Le conteneur du tableau porte overflow-x: auto, qui clippe tout menu en
 * position absolute dès qu'il dépasse la dernière ligne. Un seul menu pour
 * toute la page, rendu dans le body via Teleport, position calculée au clic.
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

import { BaseButton, BaseTable } from '@/components/base'
import type { TableColumn } from '@/components/base/BaseTable/BaseTable.types'
import { DecisionActiviteModal } from '@/components/activite'
import * as activiteService from '@/services/activite'
import type {
    ActiviteReference,
    ActiviteListItem,
} from '@/types/activite'
import { formatDateSeule } from '@/utils/date'

// ------------------------------------------------------------------
// Types locaux
// ------------------------------------------------------------------

/**
 * Ligne de la table : l'item standard, plus ses deux étapes voisines.
 *
 * Contrairement à /a-valider, pas d'`etapeCourante` ni de
 * `derniereValidation` : un brouillon n'a pas d'étape où se trouver (il
 * n'est pas dans le circuit), et aucune décision n'a encore été prise sur
 * lui.
 */
interface LigneASoumettre extends ActiviteListItem {
    etapeSuivante: ActiviteReference | null
    etapePrecedente: ActiviteReference | null
}

/** Verdict d'une décision backend, tel que la page doit y réagir. */
type Verdict = 'reussi' | 'refus' | 'panne'

/** Sens de circulation de l'écriture. */
type SensSoumission = 'AVANCER' | 'RECULER'

/**
 * Décision portée par la modale en cours.
 *
 * `sens` dit dans quelle direction l'écriture va partir : AVANCER tape
 * /soumettre-validation, RECULER tape /soumettre-validation?retour=true.
 * La distinction est locale -- le backend reçoit un booléen, pas cette
 * énum.
 */
interface ModaleEnCours {
    sens: SensSoumission
    ids: number[]
}

// ------------------------------------------------------------------
// Constantes
// ------------------------------------------------------------------

/**
 * Colonnes du tableau.
 *
 * Les clés `activite`, `periode` et `actions` sont virtuelles : elles
 * désignent des slots de rendu. `etapeSuivante` et `etapePrecedente`
 * existent comme propriétés, ce qui permet à BaseTable de calculer
 * l'alignement automatiquement.
 */
const COLONNES: TableColumn<LigneASoumettre>[] = [
    { key: 'activite', label: 'Activité', class: 'colonne-activite', align: 'start' },
    { key: 'etapeSuivante', label: 'Étape suivante' },
    { key: 'etapePrecedente', label: 'Étape précédente' },
    { key: 'periode', label: 'Période prévue' },
    { key: 'actions', label: 'Actions', align: 'center' },
]

/** Largeur du menu, dont dépend le calcul de position au clic. */
const LARGEUR_MENU = 280

// ------------------------------------------------------------------
// État
// ------------------------------------------------------------------

const lignes = ref<LigneASoumettre[]>([])
const chargement = ref(false)
const erreur = ref<string | null>(null)
const succes = ref<string | null>(null)

/**
 * Ids en cours de soumission.
 *
 * Un Set et non un booléen global : plusieurs lignes sont affichées, et
 * deux soumissions ne doivent pas se neutraliser.
 */
const enCours = ref<Set<number>>(new Set())

/**
 * Ids cochés.
 *
 * Source de vérité conservée en Set d'ids : BaseTable expose selectedItems
 * en tableau, mais la page n'a besoin que des ids.
 */
const selection = ref<Set<number>>(new Set())

/** Ligne dont le menu est ouvert, ou null. Une seule à la fois. */
const menuItem = ref<LigneASoumettre | null>(null)

/** Position du menu à l'écran, recalculée à chaque ouverture. */
const positionMenu = ref({ top: 0, left: 0 })

/** Modale de décision en cours : sens fixe, ids concernés. */
const modale = ref<ModaleEnCours | null>(null)

// ------------------------------------------------------------------
// Pont sélection <-> BaseTable
// ------------------------------------------------------------------

/**
 * Tableau d'items pour BaseTable, dérivé du Set d'ids.
 *
 * Le getter reconstruit le tableau depuis `lignes` : un item retiré du
 * tableau ne peut pas rester sélectionné, et un item rechargé sous le même
 * id réapparaît coché.
 *
 * Le setter traduit en Set d'ids ce que BaseTable émet. Aucune copie
 * superflue : le Set est reconstruit d'un coup.
 */
const itemsSelectionnes = computed<LigneASoumettre[]>({
    get: () => lignes.value.filter(l => selection.value.has(l.id)),
    set: (items) => {
        selection.value = new Set(items.map(l => l.id))
    },
})

// ------------------------------------------------------------------
// Étapes communes à la sélection
// ------------------------------------------------------------------

/**
 * Libellé d'étape suivante commun aux lignes sélectionnées.
 *
 * Toutes les lignes de cette page partagent la même étape suivante : elle
 * ne dépend pas de chaque activité, mais du poste métier de l'utilisateur
 * connecté. Le calcul est donc défensif -- on prend la première ligne qui
 * en a une -- et non un vrai cas de divergence.
 */
const etapeSuivanteSelection = computed<string | null>(() => {
    if (!selection.value.size) return null

    for (const id of selection.value) {
        const item = lignes.value.find(l => l.id === id)
        if (item?.etapeSuivante?.libelle) return item.etapeSuivante.libelle
    }

    return null
})

/**
 * Libellé d'étape précédente commun aux lignes sélectionnées.
 */
const etapePrecedenteSelection = computed<string | null>(() => {
    if (!selection.value.size) return null

    for (const id of selection.value) {
        const item = lignes.value.find(l => l.id === id)
        if (item?.etapePrecedente?.libelle) return item.etapePrecedente.libelle
    }

    return null
})

// ------------------------------------------------------------------
// Éligibilité de la sélection
// ------------------------------------------------------------------

/**
 * Vrai dès qu'au moins une ligne sélectionnée peut avancer (a une étape
 * suivante).
 *
 * Le bouton de lot suit la même règle que le menu de ligne : pas d'action
 * sans cible.
 */
const peutSoumettreSelection = computed<boolean>(() => {
    if (!selection.value.size) return false

    for (const id of selection.value) {
        const item = lignes.value.find(l => l.id === id)
        if (item?.etapeSuivante) return true
    }

    return false
})

/**
 * Vrai dès qu'au moins une ligne sélectionnée est à la dernière étape du
 * circuit -- donc qu'elle n'a plus d'étape suivante.
 *
 * L'action vers l'avant n'est alors plus une soumission, c'est la validation
 * finale. On la distingue à l'écran parce que les deux gestes ont des
 * conséquences différentes -- l'un fait circuler, l'autre publie -- même si
 * l'appel backend reste identique.
 */
const peutValiderFinalSelection = computed<boolean>(() => {
    if (!selection.value.size) return false

    for (const id of selection.value) {
        const item = lignes.value.find(l => l.id === id)
        if (item && !item.etapeSuivante) return true
    }

    return false
})

/**
 * Vrai dès qu'au moins une ligne sélectionnée peut reculer.
 */
const peutRenvoyerSelection = computed<boolean>(() => {
    if (!selection.value.size) return false

    for (const id of selection.value) {
        const item = lignes.value.find(l => l.id === id)
        if (item?.etapePrecedente) return true
    }

    return false
})

// ------------------------------------------------------------------
// Chargement
// ------------------------------------------------------------------

async function charger(): Promise<void> {
    chargement.value = true
    erreur.value = null

    let reponse

    try {
        reponse = await activiteService.listerActivitesASoumettre()
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
     * Le backend renvoie déjà une liste plate, pas un Map<étape, item>
     * comme /a-valider. Rien à aplatir ici -- juste garantir le type des
     * deux étapes voisines, pour qu'elles ne soient jamais `undefined` dans
     * le template.
     */
    lignes.value = reponse.data.content.map(item => ({
        ...item,
        etapeSuivante: item.etapeSuivante ?? null,
        etapePrecedente: item.etapePrecedente ?? null,
    }))
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
function basculerMenu(item: LigneASoumettre, event: MouseEvent): void {
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
 * Fermer le menu au clic ailleurs, au scroll, ou à Échap.
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
// Décisions unitaires
// ------------------------------------------------------------------

/**
 * Ouvrir la modale pour avancer une ligne (soumission).
 *
 * Le sens est 'AVANCER' : la ligne a une étape suivante, on y va.
 */
function soumettreLigne(item: LigneASoumettre): void {
    fermerMenu()
    modale.value = { sens: 'AVANCER', ids: [item.id] }
}

/**
 * Ouvrir la modale pour valider définitivement une ligne.
 *
 * Le sens est aussi 'AVANCER' parce que l'action est dirigée vers l'avant --
 * l'activité quitte le circuit validée. Le libellé du bouton qui a ouvert
 * la modale diffère ("Valider (validation finale)"), mais l'écriture est la
 * même : `soumettreValidation` sans paramètre `retour`.
 */
function validerFinalLigne(item: LigneASoumettre): void {
    fermerMenu()
    modale.value = { sens: 'AVANCER', ids: [item.id] }
}

/**
 * Ouvrir la modale pour reculer une ligne (retour à l'étape précédente).
 */
function renvoyerLigne(item: LigneASoumettre): void {
    fermerMenu()
    modale.value = { sens: 'RECULER', ids: [item.id] }
}

// ------------------------------------------------------------------
// Décisions groupées
// ------------------------------------------------------------------

/**
 * Ouvrir la modale pour faire avancer la sélection.
 *
 * ON FILTRE AVANT D'OUVRIR LA MODALE
 *
 * Une ligne sans étape suivante ne peut pas avancer -- elle passe par
 * `validerFinalSelection`. L'inclure dans le compte ferait afficher "3
 * activités seront soumises" alors que deux seulement le seraient.
 */
function soumettreSelection(): void {
    const ids = lignes.value
        .filter(l => selection.value.has(l.id) && l.etapeSuivante)
        .map(l => l.id)

    if (!ids.length) return

    modale.value = { sens: 'AVANCER', ids }
}

/**
 * Ouvrir la modale pour valider définitivement la sélection.
 *
 * ON FILTRE SUR LES LIGNES SANS ÉTAPE SUIVANTE
 *
 * Une ligne qui a une étape suivante ne peut pas être "validée finalement" :
 * elle est en cours de circuit, pas à son terme. L'inclure dans le compte
 * ferait mentir la modale.
 */
function validerFinalSelection(): void {
    const ids = lignes.value
        .filter(l => selection.value.has(l.id) && !l.etapeSuivante)
        .map(l => l.id)

    if (!ids.length) return

    modale.value = { sens: 'AVANCER', ids }
}

/**
 * Ouvrir la modale pour faire reculer la sélection.
 *
 * Même filtre que `soumettreSelection`, dans l'autre sens : seules les
 * lignes avec une étape précédente sont retenues.
 */
function renvoyerSelection(): void {
    const ids = lignes.value
        .filter(l => selection.value.has(l.id) && l.etapePrecedente)
        .map(l => l.id)

    if (!ids.length) return

    modale.value = { sens: 'RECULER', ids }
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
 * La modale nomme l'étape cible, permet un commentaire, et dispose d'un
 * bouton Annuler. C'est suffisant : le geste est délibéré, la conséquence
 * est écrite noir sur blanc, et revenir en arrière reste possible tant que
 * le bouton n'a pas été cliqué.
 */
function surModaleConfirmee(commentaire: string): void {
    if (!modale.value) return

    const { sens, ids } = modale.value
    modale.value = null

    void executerLot(ids, sens, commentaire)
}

// ------------------------------------------------------------------
// Exécution d'un lot
// ------------------------------------------------------------------

/**
 * Envoyer une décision à plusieurs activités, l'une après l'autre.
 *
 * SÉQUENTIEL ET NON PARALLÈLE : une soumission est une écriture d'état, et
 * un lot simultané verrait le même verrou plusieurs fois.
 *
 * UNE REQUÊTE PAR ACTIVITÉ : le backend n'expose qu'un endpoint par
 * activité. Une panne ou un refus n'annule donc pas les autres.
 *
 * UNE PANNE INTERROMPT LE LOT : le reste partirait sur une connexion morte,
 * et l'utilisateur verrait des lignes disparaître sans confirmation.
 */
async function executerLot(
    ids: number[],
    sens: SensSoumission,
    commentaire: string
): Promise<void> {
    erreur.value = null
    succes.value = null

    let reussies = 0
    const refus: string[] = []

    for (const id of ids) {
        if (enCours.value.has(id)) continue

        const verdict = await envoyerUne(id, sens, commentaire)

        if (verdict === 'panne') break

        if (verdict === 'refus') {
            refus.push(`${codeDe(id)} : soumission refusée`)
            continue
        }

        reussies += 1
        retirer(id)
        selection.value = new Set([...selection.value].filter(x => x !== id))
    }

    const verbe = sens === 'AVANCER'
        ? 'soumise(s) à l\'étape suivante'
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

/**
 * Envoyer la soumission au backend, dans un sens ou dans l'autre.
 *
 * UN SEUL CHEMIN DE CODE POUR LES DEUX DIRECTIONS
 *
 * La seule différence entre avancer et reculer est le paramètre `retour` et
 * le service tapé (qui reste le même endpoint, distingué par ce booléen).
 * Séparer les deux traitements dupliquerait toute la mécanique de retrait,
 * de message et de 409.
 *
 * UN 409 RETIRE LA LIGNE : elle a déjà quitté les brouillons (soumise
 * ailleurs, ou ramenée par un autre onglet), la garder avec une action qui
 * échouera toujours serait mentir.
 *
 * UNE PANNE LAISSE LA LIGNE : rien n'a été écrit, l'effacer ferait croire
 * le contraire.
 */
async function envoyerUne(
    id: number,
    sens: SensSoumission,
    commentaire: string
): Promise<Verdict> {
    enCours.value = new Set(enCours.value).add(id)
    erreur.value = null

    let reponse

    try {
        reponse = sens === 'RECULER'
            ? await activiteService.soumettreValidationRetour(id, commentaire)
            : await activiteService.soumettreValidation(id, commentaire)
    } catch {
        enCours.value = new Set([...enCours.value].filter(x => x !== id))
        erreur.value =
            'Connexion au serveur impossible. La soumission n\'a pas été enregistrée.'
        return 'panne'
    }

    enCours.value = new Set([...enCours.value].filter(x => x !== id))

    if (reponse.success) {
        return 'reussi'
    }

    erreur.value = reponse.error

    if (reponse.status === 409) {
        retirer(id)
    }

    return 'refus'
}

/**
 * Retire une ligne du tableau courant.
 *
 * Retire aussi l'id de la sélection : sinon un « tout désélectionner »
 * suivant remettrait un id fantôme dans le Set.
 */
function retirer(id: number): void {
    lignes.value = lignes.value.filter(l => l.id !== id)
    selection.value = new Set([...selection.value].filter(x => x !== id))
}

/**
 * Code lisible d'une activité, pour nommer un refus sans relire la liste.
 *
 * Relevé AVANT tout retrait, sinon la ligne a disparu et il ne resterait
 * qu'un identifiant que l'utilisateur ne peut pas relier à rien.
 */
function codeDe(id: number): string {
    return lignes.value.find(l => l.id === id)?.code ?? `#${id}`
}
</script>

<template>
    <div class="a-soumettre">
        <!-- ============ EN-TETE ============ -->
        <div class="page-header">
            <h1 class="page-title">Activités à soumettre</h1>
            <p class="page-subtitle">
                Brouillons en attente de soumission au circuit de validation
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

        <!--
            ============ BARRE DE LOT ============

            N'apparaît qu'avec au moins une coche : avant, elle serait trois
            contrôles sans effet. Elle ne remplace pas le menu par ligne --
            les deux usages coexistent, traiter au cas par cas ou vider un
            lot d'un coup.

            TROIS ACTIONS, CHACUNE CONDITIONNELLE
            Les trois boutons (Soumettre, Valider final, Renvoyer) n'ont
            pas la même condition d'apparition :
              - Soumettre : au moins une ligne a une étape suivante
              - Valider : au moins une ligne n'a pas d'étape suivante
              - Renvoyer : au moins une ligne a une étape précédente
            Un bouton actif sans effet promet une action que rien ne fait.
        -->
        <div v-if="selection.size > 0" class="lot" role="toolbar" aria-label="Actions sur la sélection">
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

                <!--
                    Soumettre : pour les lignes qui ont une étape suivante.
                -->
                <BaseButton v-if="peutSoumettreSelection" variant="success" size="sm" :loading="enCours.size > 0"
                    :disabled="enCours.size > 0" @click="soumettreSelection">
                    <i class="bi bi-send"></i>
                    Soumettre{{ etapeSuivanteSelection ? ` (${etapeSuivanteSelection})` : '' }}
                </BaseButton>

                <!--
                    Valider (validation finale) : pour les lignes qui n'ont
                    PLUS d'étape suivante -- elles sont à la dernière étape
                    du circuit. Même teinte que "Soumettre" -- les deux sont
                    des actions vers l'avant -- ce qui les distingue, c'est
                    le libellé, pas la couleur.
                -->
                <BaseButton v-if="peutValiderFinalSelection" variant="success" size="sm" :loading="enCours.size > 0"
                    :disabled="enCours.size > 0" @click="validerFinalSelection">
                    <i class="bi bi-check-circle"></i>
                    Valider (validation finale)
                </BaseButton>

                <!--
                    Renvoyer : pour les lignes qui ont une étape précédente.
                -->
                <BaseButton v-if="peutRenvoyerSelection" variant="warning" size="sm" :disabled="enCours.size > 0"
                    @click="renvoyerSelection">
                    <i class="bi bi-arrow-counterclockwise"></i>
                    Renvoyer{{ etapePrecedenteSelection ? ` (${etapePrecedenteSelection})` : '' }}
                </BaseButton>
            </div>
        </div>

        <!--
            ============ ETAT VIDE ============

            Masque sur une erreur : "aucun brouillon" et "service
            indisponible" ne disent pas la même chose.
        -->
        <div v-if="!chargement && !lignes.length && !erreur" class="vide">
            <i class="bi bi-inbox"></i>
            <p class="vide__titre">Aucune activité à soumettre</p>
            <p class="vide__detail">
                Les activités que vous créez apparaîtront ici jusqu'à leur
                soumission à la validation.
            </p>
        </div>

        <!--
            ============ TABLEAU ============
        -->
        <BaseTable v-if="lignes.length" v-model:selectedItems="itemsSelectionnes" :items="lignes" :columns="COLONNES"
            :loading="chargement" :page-size="20" :sortable="false" multi-select custom-class="a-soumettre__table">
            <!-- ============ CELLULE ACTIVITE ============ -->
            <template #cell-activite="{ item }">
                <router-link class="activite__lien" :to="{
                    name: 'activite-detail',
                    params: { id: String(item.id) },
                }">
                    <span class="activite__entete">
                        <span class="activite__code">{{ item.code }}</span>
                        <span v-if="item.reference" class="activite__ref">
                            {{ item.reference }}
                        </span>
                    </span>
                    <span class="activite__designation">
                        {{ item.designation }}
                    </span>
                </router-link>
            </template>

            <!-- ============ CELLULE ETAPE SUIVANTE ============ -->
            <template #cell-etapeSuivante="{ item }">
                <span v-if="item.etapeSuivante" class="etape etape--suivante">
                    {{ item.etapeSuivante.libelle }}
                </span>
                <span v-else class="etape etape--finale">
                    Validation finale
                </span>
            </template>

            <!-- ============ CELLULE ETAPE PRECEDENTE ============ -->
            <template #cell-etapePrecedente="{ item }">
                <span v-if="item.etapePrecedente" class="etape etape--precedente">
                    {{ item.etapePrecedente.libelle }}
                </span>
                <span v-else class="text-muted">—</span>
            </template>

            <!-- ============ CELLULE PERIODE ============ -->
            <template #cell-periode="{ item }">
                <template v-if="item.dateDebutPrevue">
                    <span>{{ formatDateSeule(item.dateDebutPrevue) }}</span>
                    <span class="periode__separateur">→</span>
                    <span>{{ formatDateSeule(item.dateFinPrevue) }}</span>
                </template>
                <span v-else class="text-muted">—</span>
            </template>

            <!-- ============ CELLULE ACTIONS ============ -->
            <template #cell-actions="{ item }">
                <button type="button" class="menu__toggle" :aria-expanded="menuItem?.id === item.id"
                    aria-haspopup="menu" :aria-label="`Actions pour ${item.code}`" :disabled="enCours.has(item.id)"
                    @click="basculerMenu(item, $event)">
                    <i v-if="enCours.has(item.id)" class="bi bi-arrow-repeat spin" aria-hidden="true"></i>
                    <i v-else class="bi bi-three-dots-vertical" aria-hidden="true"></i>
                </button>
            </template>
        </BaseTable>

        <!--
            ============ MENU D'ACTIONS ============

            Téléporté dans le body et en position fixed : le conteneur du
            tableau porte un overflow-x: auto qui clippe tout menu en
            position absolute dès qu'il dépasse la dernière ligne.

            DEUX ITEMS POUR L'ACTION VERS L'AVANT, JAMAIS LES DEUX
            Une ligne qui a une étape suivante se soumet à cette étape --
            on nomme la cible. Une ligne qui n'en a pas valide définitivement
            -- il n'y a pas de cible à nommer. Le v-if / v-else est
            volontairement exclusif.
        -->
        <Teleport to="body">
            <ul v-if="menuItem" class="menu-global" role="menu" :style="styleMenu" @click.stop>
                <li v-if="menuItem.etapeSuivante" role="none">
                    <button type="button" role="menuitem" class="menu-global__item menu-global__item--suivante"
                        @click="soumettreLigne(menuItem)">
                        <i class="bi bi-send"></i>
                        Soumettre à {{ menuItem.etapeSuivante.libelle }}
                    </button>
                </li>

                <li v-else role="none">
                    <button type="button" role="menuitem" class="menu-global__item menu-global__item--finale"
                        @click="validerFinalLigne(menuItem)">
                        <i class="bi bi-check-circle"></i>
                        Valider (validation finale)
                    </button>
                </li>

                <li v-if="menuItem.etapePrecedente" role="none">
                    <button type="button" role="menuitem" class="menu-global__item menu-global__item--precedente"
                        @click="renvoyerLigne(menuItem)">
                        <i class="bi bi-arrow-counterclockwise"></i>
                        Renvoyer à {{ menuItem.etapePrecedente.libelle }}
                    </button>
                </li>
            </ul>
        </Teleport>

        <!--
            ============ MODALE DE DÉCISION ============

            Rendue hors de la boucle et non sur chaque ligne : une seule
            modale pour toute la page.

            MAPPING D'ISSUE
            La modale ne connaît que trois issues : VALIDE, REJETE,
            RETOUR_MODIFICATION. On mappe AVANCER sur VALIDE (action
            positive) et RECULER sur RETOUR_MODIFICATION (action de retour).
        -->
        <DecisionActiviteModal v-if="modale" :modelValue="true"
            :issue="modale.sens === 'RECULER' ? 'RETOUR_MODIFICATION' : 'VALIDE'" :nombre="modale.ids.length"
            :loading="enCours.size > 0" @update:modelValue="fermerModale" @confirme="surModaleConfirmee" />
    </div>
</template>

<style scoped>
.a-soumettre {
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

/* --- Barre de lot --- */
.lot {
    display: flex;
    align-items: center;
    gap: 1rem;
    flex-wrap: wrap;
    padding: 0.7rem 1rem;
    margin-bottom: 1rem;
    background: #f0f7ff;
    border: 1px solid #c7ddf5;
    border-radius: 10px;
}

.lot__compte {
    font-size: 0.9rem;
    font-weight: 600;
    color: #1a73c4;
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
    background: #c7ddf5;
    margin: 0 0.25rem;
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

/* ==============================================================
   ADAPTATION DU TABLEAU
   ============================================================== */
.a-soumettre :deep(.base-table-container) table {
    min-width: 720px;
}

/* ==============================================================
   COLONNE ACTIVITE -- AUTORISER LE RETOUR A LA LIGNE

   BaseTable pose `white-space: nowrap` sur TOUS les <td> du tableau.
   Sans lever cette règle sur la seule colonne de la désignation, un
   titre long reste sur une ligne et pousse les autres colonnes hors
   de l'écran.
   ============================================================== */
.a-soumettre :deep(.colonne-activite) {
    white-space: normal;
    min-width: 220px;
    max-width: 360px;
}

/* ==============================================================
   TEXTE DE LA DESIGNATION
   ============================================================== */

.activite__lien {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
    text-decoration: none;
    color: inherit;
}

.activite__lien:hover .activite__designation {
    color: #1a73c4;
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
    color: #1a73c4;
    letter-spacing: 0.02em;
}

.activite__ref {
    font-size: 0.72rem;
    color: #93a5b8;
}

.activite__designation {
    font-size: 0.9rem;
    font-weight: 500;
    line-height: 1.3;
    transition: color 0.12s;
    overflow-wrap: anywhere;
}

/* --- Badges d'etape --- */
.etape {
    display: inline-block;
    padding: 0.2rem 0.55rem;
    border-radius: 999px;
    font-size: 0.75rem;
    font-weight: 600;
    white-space: nowrap;
}

.etape--suivante {
    background: #e6f5eb;
    color: #1f7a3e;
}

.etape--precedente {
    background: #fef4e5;
    color: #9a6400;
}

/*
    Badge "Validation finale" dans la colonne Étape suivante.

    Il remplace le tiret quand etapeSuivante est nulle, pour que la
    cellule dise ce qui va se passer (validation terminale) plutôt que
    de laisser un vide qui se lit comme une donnée manquante.
*/
.etape--finale {
    background: #d6efe0;
    color: #15663a;
}

/* --- Periode --- */
.periode__separateur {
    margin: 0 0.35rem;
    color: #93a5b8;
}

/* --- Bouton du menu --- */
.menu__toggle {
    width: 32px;
    height: 32px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: none;
    background: transparent;
    border-radius: 6px;
    color: #74879b;
    cursor: pointer;
    transition: background-color 0.12s, color 0.12s;
}

.menu__toggle:hover:not(:disabled) {
    background: #eef2f6;
    color: #1a2b3c;
}

.menu__toggle:disabled {
    cursor: wait;
    color: #b6c6d5;
}

.menu__toggle[aria-expanded='true'] {
    background: #eef2f6;
    color: #1a2b3c;
}

.spin {
    animation: spin 1s linear infinite;
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
}

/* ==============================================================
   MENU GLOBAL (teleporte dans le body)
   ============================================================== */
.menu-global {
    position: fixed;
    z-index: 1000;
    padding: 0.35rem;
    margin: 0;
    list-style: none;
    background: #fff;
    border: 1px solid #dbe4ec;
    border-radius: 8px;
    box-shadow: 0 6px 20px rgba(26, 43, 60, 0.15);
}

.menu-global__item {
    display: flex;
    align-items: center;
    gap: 0.55rem;
    width: 100%;
    padding: 0.5rem 0.65rem;
    border: none;
    background: transparent;
    border-radius: 6px;
    font-size: 0.88rem;
    text-align: left;
    color: #1a2b3c;
    cursor: pointer;
    transition: background-color 0.12s, color 0.12s;
}

.menu-global__item:hover {
    background: #f0f5fa;
}

.menu-global__item--suivante:hover {
    background: #e6f5eb;
    color: #1f7a3e;
}

/*
    Vert plus appuyé que --suivante : l'action vers l'avant quand elle
    mène à la validation finale est plus engageante qu'une simple
    soumission à une étape intermédiaire. Le survol le signale, sans
    pour autant crier au danger -- c'est un succès, pas un refus.
*/
.menu-global__item--finale:hover {
    background: #d6efe0;
    color: #15663a;
}

.menu-global__item--precedente:hover {
    background: #fef4e5;
    color: #9a6400;
}
</style>
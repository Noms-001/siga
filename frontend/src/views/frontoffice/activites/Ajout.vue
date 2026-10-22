<script setup lang="ts">
/**
 * Formulaire de creation et de modification d'une activite.
 *
 * UN SEUL COMPOSANT, DEUX MODES
 *
 * Le mode est decide par la route, pas par un parametre : la page
 * /activites/nouvelle cree, /activites/:id/modifier reprend une activite
 * existante. Les deux partagent exactement les memes champs, donc un second
 * composant dupliquerait des regles -- notamment la coherence PTA, qui doit
 * etre la meme a la creation et a la modification.
 *
 * LE TYPE D'ACTIVITE EST CHOISI EN PREMIER
 *
 * PTA et NON PTA ne sont pas deux libellesCacheS dans un champ : ils
 * commandent la suite du formulaire. Une NON PTA n'a ni objectif, ni
 * reference, donc les deux champs disparaissent, et non ne se grisent pas.
 * Faire ce choix en tete evite de remplir un formulaire puis de decouvrir que
 * la moitie des champs ne s'appliquent pas.
 *
 * LE STATUT N'EST PAS UN CHAMP DU FORMULAIRE
 *
 * activite ne porte pas de colonne statut : le statut d'une activite neuve
 * est une premiere entree d'historique a BROUILLON, posee par le backend.
 * Le proposer ici reviendrait a laisser un choix dont l'effet n'est pas
 * visible dans la reponse.
 *
 * CODE ET REFERENCE SONT PROPOSES, JAMAIS IMPOSES
 *
 * Un PTA se rattache a un objectif, et son identifiant se deduit de cet
 * objectif : A-<annee>-<nObjectif>-<nActivite> et <codeObjectif>_<nActivite>,
 * la meme convention que le jeu de donnees. Le backend renvoie le prochain
 * numero libre dans l'objectif, ce qui evite de proposer un code deja pris.
 *
 * C'est une valeur INITIALE, pas une contrainte : les deux champs restent
 * modifiables, et une valeur que l'utilisateur a retapee n'est plus
 * regeneree quand il change d'objectif. Ecraser sa saisie en silence est le
 * pire des deux mondes -- il ne verrait pas pourquoi son code a change, et
 * une valeur saisie pour une raison metier particuliere disparaitrait.
 *
 * CE QUE LE BACKEND REFUSE, ET QUE LE FORMULAIRE ANNONCE
 *
 * Les regles ne sont pas reecrites ici : elles sont verifiees cote serveur, et
 * le message d'erreur remonte tel quel. Dupliquer le controle
 * metamorphoserait un changement de regle en deux corrections a faire, dont
 * une invisible. Le formulaire se contente donc des verifications de confort
 * -- champs requis et ordre des dates -- et rend le refus du serveur lisible.
 */
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { BaseButton, BaseCard, BaseConfirm, BaseInput, BaseModal, BaseSelect } from '@/components/base'
import { useAutocomplete, type Suggestion } from '@/composables/useAutocomplete'
import { useConfirmation } from '@/composables/useConfirmation'
import { ANNEE_DEFAUT } from '@/services/activite'
import * as activiteService from '@/services/activite'
import type {
    ActiviteEcriture,
    ActiviteOption,
    LivrableEcriture,
    ResultatIntermediaireEcriture,
    SousActiviteEcriture,
} from '@/types/activite'
import { getCurrentYear } from '@/utils/date'

/** Ligne de resultat intermediaire telle qu'elle vit dans le formulaire. */
interface LigneResultat {
    /**
     * null tant que la ligne n'est pas enregistree. Sert uniquement a
     * distinguer une ligne neuve d'une ligne deja enregistree a l'affichage :
     * le backend remplace l'ensemble, il ne reconcilie pas les identifiants.
     */
    idResultatIntermediaire: number | null
    designation: string
}

const route = useRoute()
/**
 * Une seule instance pour toute la page : l'utilisateur enregistre une activite
 * ou ajoute un objectif, jamais les deux a la fois.
 */
const confirmation = useConfirmation()

const router = useRouter()

/** Id d'activite en mode modification, null en mode creation. */
const idModification = computed<number | null>(() => {
    const brut = route.params.id
    const id = Array.isArray(brut) ? Number(brut[0]) : Number(brut)
    return Number.isInteger(id) && id > 0 ? id : null
})

const estModification = computed(() => idModification.value !== null)

const chargement = ref(false)
const enregistrement = ref(false)
const erreurGlobale = ref<string | null>(null)
const succes = ref<string | null>(null)
const erreurChargement = ref<string | null>(null)

const options = reactive({
    services: [] as ActiviteOption[],
    priorites: [] as ActiviteOption[],
    typesActivite: [] as ActiviteOption[],
    sites: [] as ActiviteOption[],
})

/**
 * Un utilisateur rattache a un service ne choisit pas son service : c'est le
 * sien, et le backend l'impose quoi qu'il envoie. options.services etant
 * vide pour lui, un select vide ferait croire qu'il a oublie un champ, donc
 * le champ est remplace par une mention non editable.
 */
const serviceImpose = computed(() => options.services.length === 0)

/** Nom du service affiche quand il est impose, et non choisi. */
const libelleServiceImpose = ref<string | null>(null)

const form = reactive({
    code: '',
    reference: '',
    designation: '',
    dateDebutPrevue: '',
    dateFinPrevue: '',
    pta: true,
    idObjectifSpecifique: null as number | null,
    idTypeActivite: null as number | null,
    idSite: null as number | null,
    idPriorite: null as number | null,
    idService: null as number | null,
    resultatsIntermediaires: [] as LigneResultat[],
    sousActivites: [] as LigneSousActivite[],
})

const erreurs = reactive<Record<string, string>>({})

// ------------------------------------------------------------------
// Code et reference : proposes, jamais imposes
// ------------------------------------------------------------------

/**
 * Le code et la reference ont-ils ete retapes ?
 *
 * Suivi separement et non globalement : un utilisateur peut corriger la
 * reference en laissant le code propose. Un seul drapeau forcerait a
 * regenerer les deux, ou a ne regenerer aucun.
 *
 * On ne peut pas se contenter d'un @input, qui n'existe pas sur BaseInput :
 * le composant n'emet que update:modelValue, blur et focus. Et se fier au
 * seul update:modelValue ne marche pas non plus, car ecrire dans form.code
 * emet exactement le meme evenement que la frappe : le drapeau passerait a
 * vrai au moment meme ou l'on propose la valeur, et plus jamais de
 * regeneration.
 *
 * La saisie est donc comptee par comparaison a la derniere valeur PROPOSEE.
 * Ce test est plus juste qu'un drapeau pose a la main : retaper la valeur
 * d'origine rend le champ a son comportement propose, ce qu'un utilisateur
 * qui a tape par erreur ne voudrait pas voir distingue de celui qui n'a
 * jamais touche au champ.
 */
const codePersonnalise = ref(false)
const referencePersonnalise = ref(false)

const dernierCodePropose = ref<string | null>(null)
const derniereReferenceProposee = ref<string | null>(null)

function surSaisieCode(valeur: string): void {
    codePersonnalise.value = valeur !== dernierCodePropose.value
}

function surSaisieReference(valeur: string): void {
    referencePersonnalise.value = valeur !== derniereReferenceProposee.value
}

/**
 * Objectif retenu, avec ce qu'il faut pour construire un code.
 *
 * `prochainNumero` vient du backend : c'est le nombre d'activites deja
 * rattachees a cet objectif, plus un. Le front ne peut pas le deduire, et le
 * proposer 1 systematiquement tomberait sur un code deja pris des la premiere
 * activite d'un objectif.
 */
const objectifRetenu = ref<{ code: string | null; annee: number | null; prochainNumero: number | null } | null>(
    null
)

/** Numero de l'objectif, a deux chiffres, lu dans son code. */
function numeroObjectif(): string | null {
    const code = objectifRetenu.value?.code

    if (!code) {
        return null
    }

    // Les codes d'objectifs sont de la forme BCT/1. On ne suppose pas le
    // prefixe -- il peut changer -- mais seulement qu'il y a un numero derriere
    // un separateur. Un code sans numero exploitable rend la proposition
    // impossible plutot que fausse.
    const separateur = code.lastIndexOf('/')
    const brut = separateur === -1 ? code : code.slice(separateur + 1)
    const numero = Number(brut)

    return Number.isInteger(numero) && numero > 0 ? String(numero).padStart(2, '0') : null
}

/** Chiffre a deux chiffres, sans jamais tronquer au-dela. */
function deuxChiffres(nombre: number | null | undefined): string | null {
    return typeof nombre === 'number' && Number.isFinite(nombre)
        ? String(nombre).padStart(2, '0')
        : null
}

/**
 * Proposer code et reference pour l'objectif qui vient d'etre choisi.
 *
 * Ncalled qu'a la creation : en modification, l'activite porte deja un code
 * et une reference, qui sont sa valeur en base et non une proposition a
 * recalculer. Les regenerer a l'ouverture du formulaire ecraserait une
 * donnee saisie pour une raison particuliere.
 *
 * Le mode NPTA ne genere rien : sans objectif, il n'y a ni numero d'objectif
 * ni convention de reference a appliquer. Le champ reste vide et se remplit a
 * la main, ce qui evite d'inventer un format que le backend ne connait pas.
 */
function proposerCodeEtReference(suggestion: Suggestion): void {
    if (estModification.value) {
        return
    }

    objectifRetenu.value = {
        code: suggestion.code ?? null,
        annee: suggestion.annee ?? null,
        prochainNumero: suggestion.prochainNumero ?? null,
    }

    const annee = objectifRetenu.value.annee
    const numeroObj = numeroObjectif()
    const numeroAct = objectifRetenu.value.prochainNumero

    if (annee !== null && numeroObj !== null && typeof numeroAct === 'number') {
        // Le zero de remplissage n'a lieu que dans le CODE, ou les deux
        // numeros sont groupes par paires de deux chiffres -- A-2027-01-01.
        // La REFERENCE reprend le numero d'activite tel quel, comme le fait
        // le jeu de donnees : BCT/1_1, BCT/1_2, ..., et non BCT/1_01.
        if (!codePersonnalise.value) {
            form.code = `A-${annee}-${numeroObj}-${deuxChiffres(numeroAct)}`
            dernierCodePropose.value = form.code
        }

        if (!referencePersonnalise.value) {
            form.reference = `${objectifRetenu.value.code}_${numeroAct}`
            derniereReferenceProposee.value = form.reference
        }
    }
}

/** Vide la proposition courante, sans toucher aux valeurs affichees. */
function oublierPropositionObjectif(): void {
    objectifRetenu.value = null
    codePersonnalise.value = false
    referencePersonnalise.value = false
    dernierCodePropose.value = null
    derniereReferenceProposee.value = null
}

// ------------------------------------------------------------------
// Objectif specifique : autocomplete
// ------------------------------------------------------------------

/** Ligne de suggestion, au format attendu par la liste de choix. */
function formatSuggestion(s: Suggestion): string {
    return s.libelleSecondaire
        ? `${s.code ?? ''} — ${s.libelle ?? ''} (${s.libelleSecondaire})`
        : `${s.code ?? ''} — ${s.libelle ?? ''}`
}

/**
 * Objectif choisi : on garde son libelle pour l'afficher.
 *
 * L'identifiant est ce qui est renvoye, mais afficher un nombre nu serait
 * illisible : la suggestion retenue fournit la meme ligne que celle du menu.
 */
function afficherObjectif(s: Suggestion): void {
    form.idObjectifSpecifique = s.id
    autocompleteObjectif.saisie.value = formatSuggestion(s)

    proposerCodeEtReference(s)

    // Le code des sous-activites derive du code de l'activite, qui vient
    // d'etre propose : elles sont donc reprises dans la meme operation, sans
    // quoi elles resteraient baties sur l'ancien objectif.
    proposerCodesSousActivites()
}

const autocompleteObjectif = useAutocomplete({
    rechercher: async (terme: string) => {
        const reponse = await activiteService.autocompleterObjectifs(terme)
        return reponse.success ? reponse.data : []
    },
    onSelectionner: afficherObjectif,
    // Effacer la saisie retire aussi l'objectif : laisser l'identifiant en
    // place produirait un PTA sans objectif, que le backend refuse.
    onEffacer: retirerObjectif,
    delaiMs: 300,
})

/**
 * Passage en NON PTA.
 *
 * Retirer l'objectif est fait ici et pas seulement a l'enregistrement : une
 * activite non PTA rattachee a un objectif est refusee, et laisser le champ
 * rempli en lecture seule ferait croire a un bug.
 *
 * La reference part avec, et l'indicateur de saisie manuelle avec elle : le
 * champ est vide, et repartir en PTA doit pouvoir le reproposer a partir du
 * nouvel objectif choisi.
 */
function changerPta(valeur: boolean): void {
    form.pta = valeur

    if (valeur) {
        // Revenir en PTA laisse le code propose par la NON PTA en place : il
        // ne vaut plus une fois l'objectif choisi, et sera remplace par
        // proposerCodeEtReference. Rien a faire ici, et le retirer
        // effacerait une saisie de l'utilisateur pour rien.
        return
    }

    // L'etat du code est lu AVANT de retirer l'objectif : retirer efface la
    // proposition en cours, et avec elle le drapeau "saisi par
    // l'utilisateur". Le lire apres ferait croire le champ libre alors qu'il
    // porte une saisie, et la proposition ci-dessous l'ecraserait.
    const codeEtaitSaisi = codePersonnalise.value

    retirerObjectif()
    form.reference = ''
    referencePersonnalise.value = false

    void proposerCodeNonPta(codeEtaitSaisi)
}

/**
 * Proposer le code d une activite NON PTA : la suite du dernier de l'annee.
 *
 * Une NON PTA n'a pas d'objectif, donc rien ne deduplique son code de la
 * facon dont l'objectif le fait pour un PTA. La regle du jeu de donnees est
 * neanmoins simple -- une sequence par annee, A-2027-01 a A-2027-50 -- et elle
 * suffit a ne pas demander a l'utilisateur d'inventer une valeur qui ne se
 * rappelle de rien.
 *
 * La regle elle-meme appartient a la base : le backend renvoie le code
 * complet. Lui demander le nombre et le recomposer ici_dupliquerait une
 * convention dans deux langages.
 *
 * L'annee est ANNEE_DEFAUT, comme l'annee par defaut des filtres de la liste :
 * une NON PTA n'a pas d'objectif qui la designe, et la date de debut prevue,
 * qui le ferait, se saisit apres le type d'activite. Un code propose pour une
 * annee est donc une valeur de depart, pas un engagement -- d'ou le
 * caractere MODIFIABLE, qui ne disparait pas.
 *
 * Le code deja saisi est conserve : en revenir sur le type d'activite ne doit
 * pas perdre une saisie.
 */
async function proposerCodeNonPta(codeSaisi: boolean): Promise<void> {
    if (codeSaisi || estModification.value) {
        return
    }

    const reponse = await activiteService.prochainCodeNonPta(ANNEE_DEFAUT)

    if (!reponse.success) {
        // Aucun bloc visible ici : le champ reste libre et saisissable, et
        // l'utilisateur qui n'a pas de reponse pourra y ecrire son code. Une
        // erreur affichee dans un formulaire par ailleurs valide serait plus
        // incomprehensible que le silence.
        return
    }

    form.code = reponse.data.code
    dernierCodePropose.value = reponse.data.code

    // Les sous-activites dependent du code de l'activite : elles suivent.
    proposerCodesSousActivites()
}

function retirerObjectif(): void {
    form.idObjectifSpecifique = null
    oublierPropositionObjectif()
    autocompleteObjectif.saisie.value = ''
    autocompleteObjectif.fermer()
}

// ------------------------------------------------------------------
// Creation d'un objectif specifique
// ------------------------------------------------------------------

/**
 * CREER L'OBJECTIF QUE L'ACTIVITE RATTACHE
 *
 * Un PTA se rattache a un objectif existant, or la liste de reference n'en
 * contient pas toujours un qui convienne : refuser d'enregistrer force alors a
 * quitter la page, et l'utilisateur ne peut meme pas enregistrer le brouillon
 * de son activite. Le bouton place a cote du champ cree donc l'objectif
 * manquant, et le rattache immediatement.
 *
 * L'annee est demandee, et non deduite du code : `BCT/1` ne la porte pas. La
 * proposer par defaut rangerait l'activite dans l'annee du jour sans que
 * l'utilisateur l'ait decidee, ce qui n'apparaitrait qu'a la lecture du code
 * propose, une fois le formulaire enregistre.
 *
 * Valeur 2027 : l'annee de travail du referentiel. Modifiable comme les deux
 * autres champs, et c'est le backend qui refuse une annee hors bornes.
 */
const modalObjectif = ref(false)
const enregistrementObjectif = ref(false)
const erreurObjectif = ref<string | null>(null)
const objectif = reactive({
    code: '',
    designation: '',
    // Le type admet une chaine : v-model.number laisse passer la saisie vide
    // d'un input number, et la valider ici evite un message borne a 1900-2100
    // sur un champ que l'utilisateur vient simplement de vider.
    annee: 2027 as number | string,
})
const erreursObjectif = reactive({
    code: '',
    designation: '',
    annee: '',
})

function ouvrirModalObjectif(): void {
    objectif.code = ''
    objectif.designation = ''
    objectif.annee = getCurrentYear()
    erreursObjectif.code = ''
    erreursObjectif.designation = ''
    erreursObjectif.annee = ''
    erreurObjectif.value = null
    modalObjectif.value = true
}

function fermerModalObjectif(): void {
    if (enregistrementObjectif.value) {
        return
    }

    modalObjectif.value = false
}

function creerObjectif(): void {
    erreursObjectif.code = ''
    erreursObjectif.designation = ''
    erreursObjectif.annee = ''

    // Mêmes limites que les colonnes, vérifiées ici pour être annoncées dans le
    // champ concerné plutôt qu'en un refus global du serveur.
    if (!objectif.code.trim()) {
        erreursObjectif.code = 'Le code est obligatoire'
    } else if (objectif.code.trim().length > 20) {
        erreursObjectif.code = 'Le code ne peut pas dépasser 20 caractères'
    }

    if (!objectif.designation.trim()) {
        erreursObjectif.designation = 'La désignation est obligatoire'
    } else if (objectif.designation.trim().length > 250) {
        erreursObjectif.designation = 'La désignation ne peut pas dépasser 250 caractères'
    }

    const annee = Number(objectif.annee)

    if (objectif.annee === null || objectif.annee === '' || Number.isNaN(annee)) {
        erreursObjectif.annee = "L'année est obligatoire"
    } else if (!Number.isInteger(annee) || annee < 1900 || annee > 2100) {
        erreursObjectif.annee = "L'année doit être comprise entre 1900 et 2100"
    }

    if (erreursObjectif.code || erreursObjectif.designation || erreursObjectif.annee) {
        return
    }

    erreurObjectif.value = null

    confirmation.demander({
        titre: 'Ajouter cet objectif',
        message: `L'objectif ${objectif.code.trim()} sera ajouté au référentiel des objectifs spécifiques.`,
        consequence: "Il pourra ensuite être choisi pour cette activité et pour les suivantes.",
        libelleConfirmer: 'Ajouter',
        ton: 'primary',
        icone: 'bi bi-plus-circle',
        action: () => creerObjectifReellement(objectif.code.trim(), objectif.designation.trim(), annee),
    })
}

async function creerObjectifReellement(code: string, designation: string, annee: number): Promise<void> {
    enregistrementObjectif.value = true

    const reponse = await activiteService.creerObjectifSpecifique({
        code,
        designation,
        annee,
    })

    enregistrementObjectif.value = false

    if (!reponse.success) {
        erreurObjectif.value = reponse.error
        return
    }

    // L'objectif cree est retenu comme l'aurait ete un choix dans la liste :
    // l'input affiche la ligne et le code, puis la reference, sont proposes.
    afficherObjectif(reponse.data)

    modalObjectif.value = false
}

// ------------------------------------------------------------------
// Resultats intermediaires
// ------------------------------------------------------------------

function ajouterResultatIntermediaire(): void {
    form.resultatsIntermediaires.push({
        idResultatIntermediaire: null,
        designation: '',
    })
}

function retirerResultatIntermediaire(index: number): void {
    form.resultatsIntermediaires.splice(index, 1)
}

/** Un resultat deja enregistre se distingue d'une ligne neuve. */
function estResultatEnregistre(ligne: LigneResultat): boolean {
    return ligne.idResultatIntermediaire !== null
}

// ------------------------------------------------------------------
// Sous-activites
// ------------------------------------------------------------------

/**
 * Prefixe des codes de sous-activite d'une activite PTA : SA-<code>-.
 *
 * Le code de l'activite suffit a designer celle-ci, il est unique, et il est
 * affiche dans le formulaire. C'est donc lui qui sert de base, plutot qu'un
 * rang d'activite : un rang ne s'obtient qu'apres enregistrement, et le code
 * d'une sous-activite resterait alors vide jusqu'a la creation, que
 * l'utilisateur ne peut pas prevoir.
 *
 * A-2027-01-02 donne SA-2027-01-02-, et la sous-activite SA-2027-01-02-01. Le
 * backend applique la meme regle de son cote, pour qu'une ligne envoyee sans
 * code soit construite comme celle affichee.
 *
 * UNE NON PTA N'A PAS DE PREFIXE, ET CE N'EST PAS UNE OMISSION
 *
 * Son code n'a que trois groupes -- A-2027-51, la suite de la derniere NON PTA
 * de l'annee -- donc sa sous-activite en aurait trois aussi, SA-2027-51-01 :
 * exactement la forme que donne le rang global dans le jeu de donnees, ou
 * `SA-2027-51-` est deja porte par cinq sous-activites. Proposer ce code
 * reviendrait a faire refuser l'enregistrement par l'utilisateur lui-meme,
 * qui n'y est pour rien.
 *
 * Une NON PTA n'ayant pas d'objectif, son code de sous-activite reste donc
 * genere a l'enregistrement, et le champ est vide : c'est la seule forme qui
 * reste libre, et la regle vaut aussi cote backend.
 *
 * Null egalement quand le code saisi n'a pas la forme attendue, plutot que de
 * fabriquer un prefixe sans queue : rien n'est propose, et le champ reste a
 * saisir.
 */
function prefixeSousActivite(): string | null {
    const code = form.code.trim()

    // Quatre groupes : A-<annee>-<objectif>-<n>. C'est aussi ce qui distingue
    // un PTA d'une NON PTA, dont le code n'en a que trois.
    return form.pta && /^A-\d{4}-\d+-\d+$/.test(code) ? `SA-${code.slice(2)}-` : null
}

/**
 * Proposer le code des sous-activites qui n'en ont pas de saisi.
 *
 * Appelee quand le code de l'activite change et a chaque ajout de ligne. Une
 * ligne dont le code a ete saisi est laissee intacte : c'est la meme regle que
 * pour le code de l'activite, et un code choisi pour une raison metier
 * particuliere disparaitrait sinon sans un mot.
 *
 * Le numero est la position de la ligne, sur deux chiffres. Les lignes sont
 * ordonnees et une ligne retiree ne se renumerote pas -- le backend fait de
 * meme, en ne renvoyant que les lignes encore la : un code doit decrire la
 * sous-activite, pas sa place dans un formulaire qui n'est pas enregistre.
 */
function proposerCodesSousActivites(): void {
    const prefixe = prefixeSousActivite()

    if (prefixe === null) {
        return
    }

    form.sousActivites.forEach((sous, index) => {
        const sequence = deuxChiffres(index + 1)

        if (!sous.codePropose || sequence === null) {
            return
        }

        sous.code = `${prefixe}${sequence}`
    })
}

/**
 * Un code de sous-activite saisi remplace la proposition, definitivement.
 *
 * Le drapeau se pose donc sur le seul evenement qui vient de l'utilisateur.
 * Ecrire `sous.code` ne le declenche pas : BaseInput n'emet que depuis les
 * evenements du input natif, et rien ne renvoie le modele quand la valeur
 * change a la source.
 *
 * Contrairement au code de l'activite, il n'y a pas de comparaison a une
 * derniere valeur proposee : une ligne dont le code a ete efface puis retape
 * tel quel reste saisie, ce qui est plus previsible que de distinguer les
 * deux cas.
 */
function surSaisieCodeSousActivite(sous: LigneSousActivite): void {
    sous.codePropose = false
}

function ajouterSousActivite(): void {
    const ligne: LigneSousActivite = {
        idSousActivite: null,
        // Par defaut, la periode de l'activite : une sous-activite qui
        // deborderait par defaut serait refusee par le schema.
        dateDebutPrevue: form.dateDebutPrevue,
        dateFinPrevue: form.dateFinPrevue,
        code: '',
        // Une ligne neuve n'a pas de code saisi : elle attend celui que la
        // regle deduit du code de l'activite. Une ligne de modification, elle,
        // garde le code de la base et n'est donc pas proposee.
        codePropose: false,
        designation: '',
        livrables: [],
    }

    form.sousActivites.push(ligne)

    // Propose a l'ajout plutot qu'a la prochaine modification du code de
    // l'activite : c'est le moment ou l'utilisateur regarde le formulaire.
    const prefixe = prefixeSousActivite()
    const sequence = deuxChiffres(form.sousActivites.length)

    if (prefixe !== null && sequence !== null) {
        ligne.codePropose = true
        ligne.code = `${prefixe}${sequence}`
    }
}

function retirerSousActivite(index: number): void {
    form.sousActivites.splice(index, 1)
}

/** Une sous-activite deja enregistree se distingue d'une ligne neuve. */
function estEnregistree(sous: SousActiviteEcriture): boolean {
    return sous.idSousActivite !== null
}

// ------------------------------------------------------------------
// Livrables
// ------------------------------------------------------------------

/**
 * Ligne de livrable telle qu'elle vit dans le formulaire.
 *
 * Le corps envoye ne porte que designation et description, comme
 * LivrableEcriture. `nombreFichiers` s'y ajoute parce que le formulaire doit
 * savoir tout de suite si la ligne est retirable : c'est la seule information
 * qui l'interdit, et elle n'est disponible qu'ici, au chargement.
 */
type LigneLivrable = LivrableEcriture & { nombreFichiers: number }

/**
 * Ligne de sous-activite telle qu'elle vit dans le formulaire.
 *
 * Distincte de SousActiviteEcriture, qui est le corps envoye : celui-ci ne
 * porte pas le nombre de fichiers, que seul le formulaire utilise. Le contrat
 * d'ecriture reste donc le contrat du backend, sans champ d'affichage.
 */
type LigneSousActivite = Omit<SousActiviteEcriture, 'livrables'> & {
    livrables: LigneLivrable[]

    /**
     * `codePropose` vrai = le code affiche vient de la regle, pas de
     * l'utilisateur. C'est ce qui permet de le refaire quand le code de
     * l'activite change, sans ecraser un code que l'utilisateur a saisi.
     *
     * Meme portee que `nombreFichiers` : un champ du formulaire, jamais
     * envoye.
     */
    codePropose: boolean
}

function ajouterLivrable(sous: LigneSousActivite): void {
    sous.livrables.push({ designation: '', description: '', nombreFichiers: 0 })
}

/**
 * Retirer un livrable du formulaire.
 *
 * Refuse si le livrable porte des fichiers, parce que le backend refuserait
 * l'enregistrement en 409 : mieux vaut ne pas proposer que d'annoncer un
 * echec apres coup. La ligne reste donc visible, avec le rappel de ses
 * fichiers, et c'est le suivi qui permet de deposer ou retirer un fichier.
 */
function retirerLivrable(sous: LigneSousActivite, index: number): void {
    const livrable = sous.livrables[index]

    if (livrable !== undefined && !estProtege(livrable)) {
        sous.livrables.splice(index, 1)
    }
}

/** Un livrable qui porte des depots fait partie de l'historique de l'activite. */
function estProtege(livrable: LigneLivrable): boolean {
    return livrable.nombreFichiers > 0
}

// ------------------------------------------------------------------
// Validation de confort
// ------------------------------------------------------------------

function viderErreurs(): void {
    for (const cle of Object.keys(erreurs)) {
        delete erreurs[cle]
    }
}

function valider(): boolean {
    viderErreurs()

    if (!form.code.trim()) erreurs.code = 'Le code est obligatoire'
    if (form.pta && !form.reference.trim()) erreurs.reference = 'La référence est obligatoire'
    if (!form.designation.trim()) erreurs.designation = 'La désignation est obligatoire'
    if (!form.dateDebutPrevue) erreurs.dateDebutPrevue = 'La date de début est obligatoire'

    // Meme regle que chk_activite_dates_prevues, annoncee avant l'appel.
    if (form.dateDebutPrevue && form.dateFinPrevue && form.dateFinPrevue < form.dateDebutPrevue) {
        erreurs.dateFinPrevue = 'La fin ne peut pas précéder le début'
    }

    if (form.idPriorite === null) erreurs.idPriorite = 'La priorité est obligatoire'

    if (form.pta && form.idObjectifSpecifique === null) {
        erreurs.idObjectifSpecifique = 'Un objectif spécifique est requis pour une activité PTA'
    }

    form.resultatsIntermediaires.forEach((resultat, index) => {
        // @NotBlank cote backend refuserait le corps entier en 400 : mieux
        // vaut montrer la ligne fautive que faire echouer tout l'enregistrement.
        if (!resultat.designation.trim()) {
            erreurs[`resultat_${index}`] = 'La désignation est obligatoire'
        }
    })

    form.sousActivites.forEach((sous, index) => {
        if (!sous.designation.trim()) {
            erreurs[`sous_${index}`] = 'La désignation est obligatoire'
        } else if (!sous.dateDebutPrevue || !sous.dateFinPrevue) {
            // Les deux dates sont obligatoires cote schema pour une
            // sous-activite, alors que celle de fin ne l'est pas pour
            // l'activite : la regle n'est pas la meme, et le vide se voit
            // mal sur un petit champ de date.
            erreurs[`sous_${index}`] = 'Les dates prévues sont obligatoires'
        } else if (sous.dateFinPrevue < sous.dateDebutPrevue) {
            erreurs[`sous_${index}`] = 'La fin ne peut pas précéder le début'
        }

        // @NotBlank sur la designation du livrable, cote backend : une ligne
        // vide ferait echouer tout l'enregistrement, pas seulement la ligne.
        sous.livrables.forEach((livrable, position) => {
            if (!livrable.designation.trim()) {
                erreurs[`livrable_${index}_${position}`] = 'La désignation est obligatoire'
            }
        })
    })

    return Object.keys(erreurs).length === 0
}

// ------------------------------------------------------------------
// Chargement et enregistrement
// ------------------------------------------------------------------

/**
 * Conversion vers le format attendu par BaseSelect, identique a celle de
 * Liste.vue : les memes referentiels dans les deux ecrans, donc les memes
 * libelles, et un utilisateur ne voit pas le meme site s'appeler deux
 * choses selon la page.
 */
function versOptions(list: ActiviteOption[]): Array<Record<string, unknown>> {
    return list.map((o) => ({
        id: o.id,
        libelle: o.libelle ?? o.code ?? '',
        ...(o.code ? { code: o.code } : {}),
    }))
}

const optionsPriorite = computed(() => versOptions(options.priorites))
const optionsType = computed(() => versOptions(options.typesActivite))
const optionsSite = computed(() => versOptions(options.sites))
const optionsService = computed(() => versOptions(options.services))

async function chargerReferentiels(): Promise<void> {
    const reponse = await activiteService.chargerOptions()

    if (!reponse.success) {
        erreurGlobale.value = reponse.error
        return
    }

    options.services = reponse.data.services ?? []
    options.priorites = reponse.data.priorites ?? []
    options.typesActivite = reponse.data.typesActivite ?? []
    options.sites = reponse.data.sites ?? []
}

async function chargerPourModification(): Promise<void> {
    const id = idModification.value

    if (id === null) {
        return
    }

    chargement.value = true
    erreurChargement.value = null

    const reponse = await activiteService.formulaireActivite(id)

    chargement.value = false

    if (!reponse.success) {
        erreurChargement.value = reponse.error
        return
    }

    const data = reponse.data

    form.code = data.code
    form.reference = data.reference ?? ''
    form.designation = data.designation
    form.dateDebutPrevue = data.dateDebutPrevue
    form.dateFinPrevue = data.dateFinPrevue ?? ''
    form.idTypeActivite = data.idTypeActivite
    form.idSite = data.idSite
    form.idPriorite = data.idPriorite
    form.idService = data.idService

    // PTA est derive de la presence d'un objectif, comme partout ailleurs.
    form.pta = data.idObjectifSpecifique !== null
    form.idObjectifSpecifique = data.idObjectifSpecifique
    autocompleteObjectif.saisie.value = data.idObjectifSpecifique === null
        ? ''
        : [
            data.objectifCode ?? '',
            data.objectifDesignation ?? '',
            data.objectifAnnee ? `(${data.objectifAnnee})` : '',
        ]
            .filter(Boolean)
            .join(' ')

    form.resultatsIntermediaires = (data.resultatsIntermediaires ?? []).map((r) => ({
        idResultatIntermediaire: r.idResultatIntermediaire,
        designation: r.designation,
    }))

    form.sousActivites = (data.sousActivites ?? []).map((s) => ({
        idSousActivite: s.idSousActivite,
        code: s.code,
        // Une ligne deja enregistree garde le code de la base, meme s'il
        // n'a pas la forme de la regle actuelle : le proposer de nouveau
        // changerait un code que personne n'a demande de toucher.
        codePropose: false,
        designation: s.designation,
        dateDebutPrevue: s.dateDebutPrevue,
        dateFinPrevue: s.dateFinPrevue,
        livrables: (s.livrables ?? []).map((l) => ({
            designation: l.designation,
            description: l.description ?? '',
            nombreFichiers: l.nombreFichiers,
        })),
    }))

    // Les valeurs viennent de la base, pas d'une proposition : les marquer
    // comme personnalisees empeche qu'un changement d'objectif les
    // regenererait. En modification, aucune regeneration n'a lieu non plus,
    // mais le drapeau rend l'intention explicite.
    codePersonnalise.value = true
    referencePersonnalise.value = true
    objectifRetenu.value = data.idObjectifSpecifique === null
        ? null
        : {
            code: data.objectifCode ?? null,
            annee: data.objectifAnnee ?? null,
            prochainNumero: null,
        }

    // options.services est vide pour un utilisateur de service : son service
    // n'est donc pas dans la liste, et le formulaire ne pretend pas l'avoir
    // choisi. La mention suffit : le backend imposera de toute facon.
    libelleServiceImpose.value = serviceImpose.value
        ? 'Votre service'
        : options.services.find((s) => s.id === data.idService)?.libelle ?? null
}

/**
 * Corps envoye.
 *
 * `idObjectifSpecifique` est mis a null hors mode PTA plutot que laisse en
 * l'etat : envoyer un objectif avec pta false est refuse par le backend, et
 * ce n'est pas a lui de deviner qu'on a change de mode.
 *
 * `reference` suit la meme regle : elle identifie une cible de PTA
 * (`BCT/X_YY`) et n'a pas de sens sur une NPTA, dont la colonne est
 * nullable. Le champ reste saisi pour ne pas perdre la saisie en cas de
 * retour au mode PTA, il n'est simplement pas envoye.
 *
 * `resultatsIntermediaires` ne porte que la designation : le backend remplace
 * l'ensemble et ne reconcilie aucun identifiant, l'envoyer serait ignoré.
 */
function construireCorps(): ActiviteEcriture {
    return {
        code: form.code.trim(),
        reference: form.pta ? form.reference.trim() : null,
        designation: form.designation.trim(),
        dateDebutPrevue: form.dateDebutPrevue,
        dateFinPrevue: form.dateFinPrevue || null,
        pta: form.pta,
        idObjectifSpecifique: form.pta ? form.idObjectifSpecifique : null,
        idTypeActivite: form.idTypeActivite,
        idSite: form.idSite,
        idPriorite: form.idPriorite as number,
        idService: serviceImpose.value ? null : form.idService,
        resultatsIntermediaires: form.resultatsIntermediaires.map<ResultatIntermediaireEcriture>(
            (resultat) => ({ designation: resultat.designation.trim() })
        ),
        sousActivites: form.sousActivites.map((s) => ({
            idSousActivite: s.idSousActivite,
            code: s.code.trim(),
            designation: s.designation.trim(),
            dateDebutPrevue: s.dateDebutPrevue,
            dateFinPrevue: s.dateFinPrevue,
            // Ni identifiant ni nombre de fichiers : le backend reconnait un
            // livrable a sa designation et le remplace tant qu'il ne porte
            // pas de depot. Renvoyer le compteur serait l'informer d'un etat
            // qu'il va recalculer.
            livrables: s.livrables.map<LivrableEcriture>((livrable) => ({
                designation: livrable.designation.trim(),
                description: livrable.description.trim(),
            })),
        })),
    }
}

/**
 * Enregistrer l'activite, apres confirmation.
 *
 * LA VALIDATION AVANT LA CONFIRMATION, et non l'inverse : demander "voulez-vous
 * enregistrer ?" a un formulaire invalide ferait indiquer une erreur pour une
 * action que l'utilisateur n'a pas encore decide de tenter.
 *
 * Le message change entre creation et modification : "Enregistrer" seul ne dit
 * pas si l'on pose une nouvelle activite ou si l'on ecrase une existente.
 */
function enregistrer(): void {
    succes.value = null
    erreurGlobale.value = null

    if (!valider()) {
        return
    }

    const modification = estModification.value

    confirmation.demander({
        titre: modification ? "Modifier l'activité" : "Créer l'activité",
        message: modification
            ? "Les modifications enregistrées seront visibles de tous les utilisateurs de l'activité."
            : `L'activité sera créée en brouillon sous le code ${form.code.trim()}.`,
        consequence: modification
            ? "Les champs modifiés remplaceront les valeurs actuelles."
            : "Une activité en brouillon pourra être modifiée puis soumise à la validation.",
        libelleConfirmer: modification ? 'Enregistrer' : 'Créer',
        ton: modification ? 'warning' : 'primary',
        icone: modification ? 'bi bi-pencil' : 'bi bi-plus-circle',
        action: enregistrerReellement,
    })
}

async function enregistrerReellement(): Promise<void> {
    enregistrement.value = true

    const corps = construireCorps()

    const reponse = idModification.value === null
        ? await activiteService.creerActivite(corps)
        : await activiteService.modifierActivite(idModification.value, corps)

    enregistrement.value = false

    if (!reponse.success) {
        // Message du backend tel quel : c'est lui qui connait la regle, et
        // une paraphrase ici finirait par diverger de la vraie.
        erreurGlobale.value = reponse.error
        return
    }

    succes.value = estModification.value
        ? 'Activité modifiée.'
        : 'Activité créée en brouillon.'

    // Redirection vers le detail : l'activite a maintenant un identifiant
    // et un statut reels, et c'est la que se trouve tout le reste.
    router.push({
        name: 'activite-detail',
        params: { id: reponse.data.idActivite },
    })
}

function annuler(): void {
    if (idModification.value !== null) {
        router.push({ name: 'activite-detail', params: { id: idModification.value } })
        return
    }

    router.push({ name: 'activites-brouillons' })
}

onMounted(async () => {
    await chargerReferentiels()
    await chargerPourModification()
})
</script>

<template>
    <div class="ajout-activite">
        <BaseConfirm
            v-model="confirmation.ouvert.value"
            :demande="confirmation.demande.value"
            :loading="confirmation.enCours.value || enregistrement || enregistrementObjectif"
            @confirme="confirmation.confirmer"
            @annule="confirmation.annuler"
        />

        <!-- ============ EN-TÊTE ============ -->
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    ESPACE DE TRAVAIL · ACTIVITÉS
                </div>
                <h1>
                    {{ estModification ? "Modifier l'activité" : "Nouvelle activité" }}
                    <span class="wave" aria-hidden="true">✳</span>
                </h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    {{
                        estModification
                            ? 'Seule une activité restée en brouillon peut être modifiée.'
                            : 'Renseignez les informations ci-dessous pour créer et planifier une activité.'
                    }}
                </p>
            </div>

            <div class="welcome-row__right">
                <BaseButton variant="secondary" @click="annuler">
                    <i class="bi bi-arrow-left"></i>
                    Retour
                </BaseButton>
            </div>
        </header>

        <!-- ============ MESSAGES ============ -->
        <div v-if="erreurGlobale" class="alert alert-danger alert-dismissible" role="alert">
            <i class="bi bi-exclamation-triangle me-2"></i>
            {{ erreurGlobale }}
            <button type="button" class="btn-close" @click="erreurGlobale = null"></button>
        </div>
        <div v-if="succes" class="alert alert-success" role="alert">
            <i class="bi bi-check-circle me-2"></i>
            {{ succes }}
        </div>

        <!-- ============ CHARGEMENT ============ -->
        <div v-if="chargement" class="detail-state">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="mt-3 mb-0">Chargement de l'activité…</p>
        </div>

        <div v-else-if="erreurChargement" class="detail-state">
            <i class="bi bi-exclamation-circle empty-icon"></i>
            <h3 class="empty-title">Formulaire indisponible</h3>
            <p class="empty-text">{{ erreurChargement }}</p>
            <BaseButton variant="secondary" @click="annuler">
                <i class="bi bi-arrow-left"></i>
                Revenir
            </BaseButton>
        </div>

        <!-- ============ FORMULAIRE ============ -->
        <form v-else class="form-layout" novalidate @submit.prevent="enregistrer">
            <div class="form-main">

                <!-- 01 — TYPE D'ACTIVITÉ -->
                <section class="panel form-section">
                    <div class="section-head">
                        <span class="section-num">01</span>
                        <div>
                            <h2>Type d'activité</h2>
                            <p>Ce choix détermine les champs applicables.</p>
                        </div>
                    </div>

                    <div class="choix-type-grid" role="radiogroup" aria-label="Type d'activité">
                        <button
                            type="button"
                            class="choix-type"
                            :class="{ 'choix-type--actif': form.pta }"
                            role="radio"
                            :aria-checked="form.pta"
                            @click="changerPta(true)"
                        >
                            <span class="choix-type__icone"><i class="bi bi-bullseye"></i></span>
                            <span class="choix-type__titre">PTA</span>
                            <span class="choix-type__detail">
                                Plan de travail annuel. Rattachée à un objectif spécifique.
                            </span>
                            <span v-if="form.pta" class="choix-type__check">
                                <i class="bi bi-check-lg"></i>
                            </span>
                        </button>

                        <button
                            type="button"
                            class="choix-type"
                            :class="{ 'choix-type--actif': !form.pta }"
                            role="radio"
                            :aria-checked="!form.pta"
                            @click="changerPta(false)"
                        >
                            <span class="choix-type__icone"><i class="bi bi-circle"></i></span>
                            <span class="choix-type__titre">Non PTA</span>
                            <span class="choix-type__detail">
                                Activité hors plan de travail. Aucun objectif, aucune référence.
                            </span>
                            <span v-if="!form.pta" class="choix-type__check">
                                <i class="bi bi-check-lg"></i>
                            </span>
                        </button>
                    </div>
                </section>

                <!-- 02 — INFORMATIONS GÉNÉRALES -->
                <section class="panel form-section">
                    <div class="section-head">
                        <span class="section-num">02</span>
                        <div>
                            <h2>Informations générales</h2>
                            <p>Identifiez l'activité et planifiez sa période.</p>
                        </div>
                    </div>

                    <div class="field-grid">
                        <!-- Objectif spécifique -->
                        <div v-if="form.pta" class="field full">
                            <div :ref="autocompleteObjectif.racine">
                                <div class="position-relative">
                                    <div class="objectif-champ">
                                        <BaseInput
                                            :model-value="autocompleteObjectif.saisie.value"
                                            label="Objectif spécifique"
                                            type="search"
                                            required
                                            placeholder="Code, désignation ou année"
                                            autocomplete="off"
                                            :error="erreurs.idObjectifSpecifique"
                                            :aria-expanded="autocompleteObjectif.ouvert.value"
                                            @update:model-value="(valeur: string) => {
                                                autocompleteObjectif.saisie.value = valeur
                                                autocompleteObjectif.charger()
                                            }"
                                        />
                                        <button
                                            type="button"
                                            class="objectif-champ__creer"
                                            title="Créer un objectif"
                                            aria-label="Créer un objectif"
                                            :disabled="enregistrementObjectif"
                                            @click="ouvrirModalObjectif"
                                        >
                                            <i class="bi bi-plus-lg"></i>
                                        </button>
                                    </div>

                                    <ul
                                        v-if="autocompleteObjectif.ouvert.value && autocompleteObjectif.suggestions.value.length"
                                        class="autocomplete"
                                        role="listbox"
                                        aria-label="Suggestions d'objectif"
                                    >
                                        <li
                                            v-for="(s, i) in autocompleteObjectif.suggestions.value"
                                            :key="s.id"
                                            role="option"
                                            :aria-selected="i === autocompleteObjectif.indexActif.value"
                                            :class="{ 'autocomplete__item--actif': i === autocompleteObjectif.indexActif.value }"
                                            @mouseenter="autocompleteObjectif.survoler(i)"
                                        >
                                            <button type="button" tabindex="-1" @click="autocompleteObjectif.selectionner(s)">
                                                {{ formatSuggestion(s) }}
                                            </button>
                                        </li>
                                    </ul>
                                </div>
                                <p class="hint">
                                    Le code et la référence sont proposés d'après cet objectif.
                                    <template v-if="form.idObjectifSpecifique !== null">
                                        Vider le champ le retire.
                                    </template>
                                </p>
                            </div>
                        </div>

                        <!-- Code + Référence (PTA) -->
                        <template v-if="form.pta">
                            <div class="field">
                                <BaseInput
                                    v-model="form.code"
                                    label="Code"
                                    required
                                    :error="erreurs.code"
                                    placeholder="A-2027-01-01"
                                    helper="Proposé après le choix de l'objectif. Modifiable."
                                    @update:model-value="surSaisieCode"
                                />
                            </div>
                            <div class="field">
                                <BaseInput
                                    v-model="form.reference"
                                    label="Référence"
                                    required
                                    :error="erreurs.reference"
                                    placeholder="BCT/1_1"
                                    helper="Code de l'objectif suivi du numéro d'activité. Modifiable."
                                    @update:model-value="surSaisieReference"
                                />
                            </div>
                        </template>

                        <!-- Code seul (NON PTA) -->
                        <div v-else class="field full">
                            <BaseInput
                                v-model="form.code"
                                label="Code"
                                required
                                :error="erreurs.code"
                                placeholder="A-2027-01"
                                helper="Proposé d'après la dernière activité non PTA de l'année. Modifiable."
                                @update:model-value="surSaisieCode"
                            />
                        </div>

                        <!-- Désignation -->
                        <div class="field full">
                            <BaseInput
                                v-model="form.designation"
                                label="Désignation"
                                required
                                :error="erreurs.designation"
                                placeholder="Objet de l'activité"
                            />
                        </div>

                        <!-- Résultats intermédiaires -->
                        <div class="field full">
                            <div class="bloc-ajoutable">
                                <div class="bloc-ajoutable__entete">
                                    <span class="label">Résultats intermédiaires</span>
                                    <button type="button" class="lien-ajout" @click="ajouterResultatIntermediaire">
                                        <i class="bi bi-plus-lg"></i>
                                        Ajouter
                                    </button>
                                </div>
                                <p class="hint">Facultatif. Un résultat est un état atteint en cours d'activité, pas sa finalité.</p>

                                <p v-if="form.resultatsIntermediaires.length === 0" class="aucun-resultat">
                                    Aucun résultat intermédiaire déclaré.
                                </p>

                                <div
                                    v-for="(resultat, index) in form.resultatsIntermediaires"
                                    :key="index"
                                    class="ligne-ajoutable"
                                >
                                    <div class="ligne-ajoutable__champ">
                                        <BaseInput
                                            v-model="resultat.designation"
                                            type="textarea"
                                            placeholder="Le système de contrôle d'accès est installé et opérationnel"
                                            :error="erreurs[`resultat_${index}`]"
                                        />
                                    </div>
                                    <div class="ligne-ajoutable__meta">
                                        <span v-if="estResultatEnregistre(resultat)" class="badge-enregistree">
                                            enregistrée
                                        </span>
                                        <button
                                            type="button"
                                            class="ligne-ajoutable__supprimer"
                                            :aria-label="`Retirer le résultat intermédiaire ${index + 1}`"
                                            @click="retirerResultatIntermediaire(index)"
                                        >
                                            <i class="bi bi-trash"></i>
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Dates -->
                        <div class="field">
                            <BaseInput
                                v-model="form.dateDebutPrevue"
                                label="Date de début prévue"
                                type="date"
                                required
                                :error="erreurs.dateDebutPrevue"
                            />
                        </div>
                        <div class="field">
                            <BaseInput
                                v-model="form.dateFinPrevue"
                                label="Date de fin prévue"
                                type="date"
                                :error="erreurs.dateFinPrevue"
                                helper="Facultatif : une activité peut être ouverte sans échéance."
                            />
                        </div>
                    </div>
                </section>

                <!-- 03 — CLASSEMENT -->
                <section class="panel form-section">
                    <div class="section-head">
                        <span class="section-num">03</span>
                        <div>
                            <h2>Classement</h2>
                            <p>Éléments de filtrage et de suivi, indépendamment du type d'activité.</p>
                        </div>
                    </div>

                    <div class="field-grid">
                        <div class="field">
                            <BaseSelect
                                :model-value="form.idPriorite ?? undefined"
                                label="Priorité"
                                required
                                placeholder="Sélectionner"
                                :options="optionsPriorite"
                                option-label="libelle"
                                option-value="id"
                                :error="erreurs.idPriorite"
                                @update:model-value="form.idPriorite = ($event as number | null) ?? null"
                            />
                        </div>

                        <div class="field">
                            <BaseSelect
                                :model-value="form.idTypeActivite ?? undefined"
                                label="Type d'activité"
                                placeholder="Aucun"
                                :options="optionsType"
                                option-label="libelle"
                                option-value="id"
                                @update:model-value="form.idTypeActivite = ($event as number | null) ?? null"
                            />
                        </div>

                        <div class="field">
                            <BaseSelect
                                :model-value="form.idSite ?? undefined"
                                label="Site"
                                placeholder="Aucun"
                                :options="optionsSite"
                                option-label="libelle"
                                option-value="id"
                                @update:model-value="form.idSite = ($event as number | null) ?? null"
                            />
                        </div>

                        <div class="field">
                            <template v-if="serviceImpose">
                                <span class="label">Service</span>
                                <BaseInput :model-value="libelleServiceImpose ?? 'Votre service'" disabled readonly />
                                <p class="hint">Imposé par votre rattachement.</p>
                            </template>
                            <BaseSelect
                                v-else
                                :model-value="form.idService ?? undefined"
                                label="Service"
                                required
                                placeholder="Sélectionner"
                                :options="optionsService"
                                option-label="libelle"
                                option-value="id"
                                @update:model-value="form.idService = ($event as number | null) ?? null"
                            />
                        </div>
                    </div>
                </section>

                <!-- 04 — SOUS-ACTIVITÉS -->
                <section class="panel form-section">
                    <div class="section-head section-head--with-action">
                        <span class="section-num">04</span>
                        <div>
                            <h2>Sous-activités</h2>
                            <p>Facultatif. Le code est proposé d'après celui de l'activité et reste modifiable.</p>
                        </div>
                        <button type="button" class="lien-ajout lien-ajout--cta" @click="ajouterSousActivite">
                            <i class="bi bi-plus-lg"></i>
                            Ajouter
                        </button>
                    </div>

                    <p v-if="form.sousActivites.length === 0" class="aucun-resultat">
                        Aucune sous-activité. L'activité peut être créée sans.
                    </p>

                    <div v-for="(sous, index) in form.sousActivites" :key="index" class="sous-activite">
                        <div class="sous-activite__entete">
                            <span class="sous-activite__titre">
                                Sous-activité {{ index + 1 }}
                                <span v-if="estEnregistree(sous)" class="badge-enregistree">enregistrée</span>
                            </span>
                            <button
                                type="button"
                                class="sous-activite__supprimer"
                                :aria-label="`Retirer la sous-activité ${index + 1}`"
                                @click="retirerSousActivite(index)"
                            >
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>

                        <div class="field-grid">
                            <div class="field">
                                <BaseInput
                                    v-model="sous.code"
                                    label="Code"
                                    placeholder="généré"
                                    @update:model-value="surSaisieCodeSousActivite(sous)"
                                />
                            </div>
                            <div class="field">
                                <BaseInput
                                    v-model="sous.designation"
                                    label="Désignation"
                                    required
                                    :error="erreurs[`sous_${index}`]"
                                />
                            </div>
                            <div class="field">
                                <BaseInput v-model="sous.dateDebutPrevue" label="Début" type="date" required />
                            </div>
                            <div class="field">
                                <BaseInput v-model="sous.dateFinPrevue" label="Fin" type="date" required />
                            </div>
                        </div>

                        <!-- Livrables -->
                        <div class="livrables">
                            <div class="livrables__entete">
                                <span class="livrables__titre">
                                    <i class="bi bi-box-seam"></i>
                                    Livrables
                                    <span v-if="sous.livrables.length > 0" class="livrables__compteur">
                                        {{ sous.livrables.length }}
                                    </span>
                                </span>
                                <button type="button" class="lien-ajout" @click="ajouterLivrable(sous)">
                                    <i class="bi bi-plus-lg"></i>
                                    Ajouter un livrable
                                </button>
                            </div>

                            <p v-if="sous.livrables.length === 0" class="livrables__vide">Aucun livrable déclaré.</p>

                            <div
                                v-for="(livrable, position) in sous.livrables"
                                :key="position"
                                class="livrable"
                            >
                                <div class="livrable__ligne">
                                    <div class="livrable__designation">
                                        <BaseInput
                                            v-model="livrable.designation"
                                            placeholder="Désignation du livrable"
                                            required
                                            :error="erreurs[`livrable_${index}_${position}`]"
                                        />
                                    </div>
                                    <button
                                        type="button"
                                        class="livrable__supprimer"
                                        :class="{ 'livrable__supprimer--bloque': estProtege(livrable) }"
                                        :disabled="estProtege(livrable)"
                                        :title="estProtege(livrable)
                                            ? 'Ce livrable porte des fichiers : il fait partie de l\'historique de l\'activité'
                                            : 'Retirer ce livrable'"
                                        :aria-label="`Retirer le livrable ${position + 1}`"
                                        @click="retirerLivrable(sous, position)"
                                    >
                                        <i class="bi bi-trash"></i>
                                    </button>
                                </div>
                                <BaseInput
                                    v-model="livrable.description"
                                    type="textarea"
                                    :rows="2"
                                    placeholder="Description (facultative) : contenu attendu, format, destinataire…"
                                />
                                <p v-if="estProtege(livrable)" class="livrable__note">
                                    <i class="bi bi-paperclip"></i>
                                    {{ livrable.nombreFichiers }} fichier{{ livrable.nombreFichiers > 1 ? 's' : '' }}
                                    déposé{{ livrable.nombreFichiers > 1 ? 's' : '' }} — non supprimable d'ici.
                                </p>
                            </div>
                        </div>
                    </div>
                </section>
            </div>

            <!-- ============ SIDEBAR ============ -->
            <aside class="form-side">
                <section class="panel side-card">
                    <h2 class="side-title">Récapitulatif</h2>
                    <p class="side-sub">Aperçu de la fiche avant enregistrement.</p>
                    <div class="recap">
                        <div class="recap-row">
                            <span>Type</span>
                            <strong>{{ form.pta ? 'PTA' : 'Non PTA' }}</strong>
                        </div>
                        <div class="recap-row">
                            <span>Code</span>
                            <strong>{{ form.code.trim() || '—' }}</strong>
                        </div>
                        <div class="recap-row">
                            <span>Désignation</span>
                            <strong>{{ form.designation.trim() || '—' }}</strong>
                        </div>
                        <div class="recap-row">
                            <span>Début</span>
                            <strong>{{ form.dateDebutPrevue || '—' }}</strong>
                        </div>
                        <div class="recap-row">
                            <span>Fin</span>
                            <strong>{{ form.dateFinPrevue || '—' }}</strong>
                        </div>
                        <div class="recap-row">
                            <span>Priorité</span>
                            <strong>{{ options.priorites.find(p => p.id === form.idPriorite)?.libelle ?? '—' }}</strong>
                        </div>
                        <div class="recap-row">
                            <span>Sous-activités</span>
                            <strong>{{ form.sousActivites.length }}</strong>
                        </div>
                    </div>
                </section>

                <section class="panel side-card">
                    <h2 class="side-title">Conseils</h2>
                    <ul class="tip-list">
                        <li><span>1</span> Le code et la référence sont proposés automatiquement d'après l'objectif choisi.</li>
                        <li><span>2</span> Un objectif spécifique est requis pour une activité PTA — il peut être créé à la volée.</li>
                        <li><span>3</span> Les sous-activités et leurs livrables sont facultatifs, mais facilitent le suivi.</li>
                    </ul>
                </section>
            </aside>

            <!-- ============ BARRE D'ACTIONS ============ -->
            <div class="form-actions">
                <span class="form-note">
                    <span class="req">*</span> Champs obligatoires —
                    Le statut n'est pas modifiable ici : une activité créée est un brouillon.
                </span>
                <div class="form-actions-buttons">
                    <BaseButton variant="secondary" :disabled="enregistrement" @click="annuler">
                        Annuler
                    </BaseButton>
                    <BaseButton variant="primary" type="submit" :loading="enregistrement">
                        <i class="bi bi-check-lg"></i>
                        {{ estModification ? 'Enregistrer les modifications' : 'Créer le brouillon' }}
                    </BaseButton>
                </div>
            </div>
        </form>

        <!-- ============ MODAL OBJECTIF ============ -->
        <BaseModal v-model="modalObjectif" title="Créer un objectif spécifique" size="sm" :closeOnOutside="false">
            <div class="modal-objectif">
                <p class="hint">L'activité sera rattachée à cet objectif, qui sera retenu dans le champ ci-dessus.</p>
                <BaseInput
                    v-model="objectif.code"
                    label="Code (20 caractères max)"
                    required
                    :error="erreursObjectif.code"
                    placeholder="BCT/7"
                />
                <BaseInput
                    v-model="objectif.designation"
                    label="Désignation (250 caractères max)"
                    required
                    :error="erreursObjectif.designation"
                    placeholder="Objet de l'objectif"
                />
                <BaseInput
                    v-model.number="objectif.annee"
                    label="Année"
                    type="number"
                    required
                    :error="erreursObjectif.annee"
                    placeholder="2027"
                />
                <p v-if="erreurObjectif" class="modal-objectif__erreur">{{ erreurObjectif }}</p>
            </div>

            <template #footer>
                <BaseButton variant="secondary" :disabled="enregistrementObjectif" @click="fermerModalObjectif">
                    Annuler
                </BaseButton>
                <BaseButton variant="primary" :loading="enregistrementObjectif" @click="creerObjectif">
                    Créer et rattacher
                </BaseButton>
            </template>
        </BaseModal>
    </div>
</template>

<style scoped>
/* ==========================================================================
   1. CONTENEUR PAGE — fond + padding (aligné dashboard / liste)
   ========================================================================== */
.ajout-activite {
    min-height: calc(100vh - 68px);
    padding: 34px 38px 24px;
    color: #15233c;
    font-family: "DM Sans", "Segoe UI", sans-serif;
    background:
        radial-gradient(circle at 12% 8%, rgba(26, 42, 108, .055) 0, transparent 38%),
        radial-gradient(circle at 92% 94%, rgba(212, 175, 55, .065) 0, transparent 36%),
        #f5f7fa;
}

/* Fallback minimaliste pour .panel (si absent du global) */
.panel {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 14px;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .04),
        0 8px 24px rgba(26, 42, 108, .06);
}

/* ==========================================================================
   2. MISE EN PAGE FORMULAIRE (ex form.css §1)
   ========================================================================== */
.form-layout {
    display: grid;
    grid-template-columns: minmax(0, 1.9fr) minmax(280px, 1fr);
    align-items: start;
    gap: 20px;
}

.form-main {
    display: grid;
    gap: 20px;
    min-width: 0;
}

.form-side {
    position: sticky;
    top: 88px;
    display: grid;
    gap: 18px;
    align-self: start;
}

/* ==========================================================================
   3. SECTIONS NUMÉROTÉES (ex form.css §2)
   ========================================================================== */
.form-section {
    padding: 24px 26px 26px;
}

.section-head {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    margin-bottom: 22px;
}

.section-head--with-action {
    flex-wrap: wrap;
}
.section-head--with-action > .lien-ajout--cta {
    margin-left: auto;
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.4rem 0.8rem;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    background: #fff;
    color: #1a2a6c;
    font-size: 0.78rem;
    font-weight: 600;
    cursor: pointer;
    transition: background .15s, border-color .15s;
}
.lien-ajout--cta:hover {
    background: #f3f6fc;
    border-color: #4a6cf7;
}

.section-num {
    flex: none;
    display: grid;
    place-items: center;
    width: 34px;
    height: 34px;
    border-radius: 10px;
    color: #fff;
    font: 800 12px Manrope, sans-serif;
    letter-spacing: .3px;
    background: linear-gradient(135deg, #1a2a6c 0%, #4a6cf7 100%);
    box-shadow:
        0 6px 14px rgba(26, 42, 108, .22),
        inset 0 -2px 0 var(--gold, #d4af37);
}

.section-head h2 {
    margin: 0 0 4px;
    font: 800 16px Manrope, sans-serif;
    letter-spacing: -.3px;
    color: var(--heading, #1a2a6c);
}

.section-head p {
    margin: 0;
    font-size: 11px;
    color: var(--soft, #8a95a8);
}

/* ==========================================================================
   4. GRILLE DE CHAMPS (ex form.css §3)
   ========================================================================== */
.field-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px;
}

.field {
    display: flex;
    flex-direction: column;
    gap: 7px;
    min-width: 0;
}

.field.full {
    grid-column: 1 / -1;
}

/* ==========================================================================
   5. LIBELLÉS / HINTS (ex form.css §4)
   ========================================================================== */
.label {
    font-size: 0.82rem;
    font-weight: 700;
    color: var(--heading, #1a2a6c);
}

.req {
    color: var(--red-fg, #c0392b);
    font-weight: 700;
}

.hint {
    margin: 0.3rem 0 0;
    font-size: 0.76rem;
    color: var(--soft, #6b7c93);
    line-height: 1.45;
}

/* ==========================================================================
   6. BARRE D'ACTIONS COLLANTE (ex form.css §11)
   ========================================================================== */
.form-actions {
    position: sticky;
    bottom: 14px;
    z-index: 3;
    grid-column: 1 / -1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
    margin-top: 4px;
    padding: 14px 20px;
    background: rgba(255, 255, 255, .92);
    border: 1px solid var(--line, #e6ebf3);
    border-radius: 14px;
    box-shadow: 0 10px 30px rgba(26, 42, 108, .10);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
}

.form-note {
    font-size: 10.5px;
    color: var(--soft, #8a95a8);
    line-height: 1.5;
}

.form-actions-buttons {
    display: flex;
    gap: 10px;
}

/* ==========================================================================
   7. COLONNE LATÉRALE — Récapitulatif + Conseils (ex form.css §10)
   ========================================================================== */
.side-card {
    padding: 20px 20px 18px;
}

.side-title {
    margin: 0 0 4px;
    font: 800 14px Manrope, sans-serif;
    letter-spacing: -.2px;
    color: var(--heading, #1a2a6c);
}

.side-sub {
    margin: 0 0 14px;
    font-size: 10.5px;
    color: var(--soft, #8a95a8);
}

.recap {
    display: grid;
}

.recap-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 11px 0;
    font-size: 11px;
    color: var(--muted, #6b7280);
    border-bottom: 1px dashed var(--line, #e6ebf3);
}
.recap-row:last-child {
    padding-bottom: 0;
    border-bottom: 0;
}
.recap-row strong {
    max-width: 62%;
    font-size: 11px;
    font-weight: 600;
    color: var(--heading, #1a2a6c);
    text-align: right;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.tip-list {
    list-style: none;
    display: grid;
    gap: 12px;
    margin: 14px 0 0;
    padding: 0;
}
.tip-list li {
    display: flex;
    gap: 10px;
    font-size: 10.8px;
    line-height: 1.55;
    color: var(--muted, #6b7280);
}
.tip-list li span {
    flex: none;
    display: grid;
    place-items: center;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: var(--gold-bg, #fff6dd);
    color: var(--gold-fg, #b8860b);
    font-size: 9.5px;
    font-weight: 800;
}

/* ==========================================================================
   8. CHOIX PTA / NON PTA (nouveau — inspiré du HTML de référence)
   ========================================================================== */
.choix-type-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
}
@media (max-width: 640px) {
    .choix-type-grid { grid-template-columns: 1fr; }
}

.choix-type {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 0.35rem;
    width: 100%;
    height: 100%;
    padding: 18px 20px;
    border: 1.5px solid #dde4ec;
    border-radius: 12px;
    background: #fff;
    text-align: left;
    cursor: pointer;
    transition: border-color .15s, box-shadow .15s, background .15s;
}
.choix-type:hover {
    border-color: #b6c6d6;
    background: #f8fbfd;
}
.choix-type--actif {
    border-color: #1a6fb0;
    background: #f2f8fc;
    box-shadow: inset 0 0 0 1px #1a6fb0;
}

.choix-type__icone {
    display: grid;
    place-items: center;
    width: 34px;
    height: 34px;
    border-radius: 10px;
    background: #eef5f8;
    color: #5b6b80;
    font-size: 1.1rem;
}
.choix-type--actif .choix-type__icone {
    background: #dceefb;
    color: #1a6fb0;
}

.choix-type__titre {
    margin-top: 4px;
    font-size: 1rem;
    font-weight: 700;
    color: #22364a;
}

.choix-type__detail {
    font-size: 0.78rem;
    line-height: 1.4;
    color: #6b7c93;
}

.choix-type__check {
    position: absolute;
    top: 14px;
    right: 16px;
    display: grid;
    place-items: center;
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: #1a6fb0;
    color: #fff;
    font-size: 0.8rem;
}

/* ==========================================================================
   9. CHAMP OBJECTIF + BOUTON "+" (nouveau)
   ========================================================================== */
.objectif-champ {
    display: flex;
    align-items: flex-start;
    gap: 8px;
}
.objectif-champ > :first-child {
    flex: 1;
    min-width: 0;
}
.objectif-champ__creer {
    flex-shrink: 0;
    width: 44px;
    height: 44px;
    margin-top: 24px;
    border: 1px solid #cfd8e3;
    border-radius: 10px;
    background: #fff;
    color: #24405e;
    font-size: 1.05rem;
    cursor: pointer;
    transition: background .15s, border-color .15s;
}
.objectif-champ__creer:hover:not(:disabled) {
    background: #f0f5fa;
    border-color: #a9bacd;
}
.objectif-champ__creer:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

/* ==========================================================================
   10. AUTOCOMPLETE (nouveau — inspiré du HTML de référence)
   ========================================================================== */
.autocomplete {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    z-index: 20;
    margin: 0.15rem 0 0;
    padding: 0.3rem;
    list-style: none;
    background: #fff;
    border: 1px solid #dde4ec;
    border-radius: 10px;
    box-shadow: 0 12px 28px rgba(26, 42, 108, .12);
    max-height: 260px;
    overflow-y: auto;
}
.autocomplete button {
    display: block;
    width: 100%;
    padding: 0.5rem 0.65rem;
    border: none;
    border-radius: 6px;
    background: transparent;
    text-align: left;
    font-size: 0.84rem;
    color: #33475c;
    cursor: pointer;
}
.autocomplete button:hover {
    background: #eef5f8;
}
.autocomplete__item--actif button {
    background: #eef5f8;
    font-weight: 600;
}

/* ==========================================================================
   11. BLOCS AJOUTABLES — Résultats intermédiaires (nouveau)
   ========================================================================== */
.bloc-ajoutable {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}
.bloc-ajoutable__entete {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
}

.lien-ajout {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
    padding: 0;
    border: none;
    background: transparent;
    font-size: 0.8rem;
    font-weight: 600;
    color: #1a6fb0;
    cursor: pointer;
}
.lien-ajout:hover { text-decoration: underline; }

.aucun-resultat {
    margin: 0;
    padding: 1rem 1.25rem;
    border: 1px dashed #dde4ec;
    border-radius: 10px;
    text-align: center;
    font-size: 0.84rem;
    color: #6b7c93;
    background: #fbfdfe;
}

.ligne-ajoutable {
    display: flex;
    align-items: flex-start;
    gap: 0.75rem;
    margin-bottom: 0.6rem;
}
.ligne-ajoutable__champ { flex: 1 1 auto; min-width: 0; }
.ligne-ajoutable__meta {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    padding-top: 0.55rem;
}
.ligne-ajoutable__supprimer {
    border: none;
    background: transparent;
    color: #c0392b;
    font-size: 0.95rem;
    cursor: pointer;
}
.ligne-ajoutable__supprimer:hover { color: #96281b; }

.badge-enregistree {
    padding: 0.15rem 0.5rem;
    border-radius: 5px;
    background: #eef5f8;
    color: #5b6b80;
    font-size: 0.7rem;
    font-weight: 600;
}

/* ==========================================================================
   12. SOUS-ACTIVITÉS (nouveau — inspiré du HTML de référence)
   ========================================================================== */
.sous-activite {
    padding: 1rem;
    margin-top: 0.85rem;
    border: 1px solid #e4eaf1;
    border-radius: 12px;
    background: #fbfdfe;
}
.sous-activite__entete {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 0.75rem;
}
.sous-activite__titre {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.85rem;
    font-weight: 700;
    color: #33475c;
}
.sous-activite__supprimer {
    border: none;
    background: transparent;
    color: #c0392b;
    font-size: 0.95rem;
    cursor: pointer;
}
.sous-activite__supprimer:hover { color: #96281b; }

/* ==========================================================================
   13. LIVRABLES (nouveau)
   ========================================================================== */
.livrables {
    margin-top: 0.9rem;
    padding-top: 0.75rem;
    border-top: 1px dashed #dde5ee;
}
.livrables__entete {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.5rem;
    margin-bottom: 0.6rem;
}
.livrables__titre {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    font-size: 0.78rem;
    font-weight: 700;
    color: #5b6b80;
    text-transform: uppercase;
    letter-spacing: 0.04em;
}
.livrables__compteur {
    padding: 0.05rem 0.45rem;
    border-radius: 999px;
    background: #eef5f8;
    color: #5b6b80;
    font-size: 0.7rem;
    letter-spacing: 0;
}
.livrables__vide {
    margin: 0;
    font-size: 0.8rem;
    color: #8a99ac;
    font-style: italic;
}

.livrable {
    padding: 0.7rem 0.85rem;
    margin-bottom: 0.6rem;
    border: 1px solid #e9eef4;
    border-radius: 10px;
    background: #fff;
}
.livrable:last-child { margin-bottom: 0; }
.livrable__ligne {
    display: flex;
    align-items: flex-start;
    gap: 0.5rem;
}
.livrable__designation { flex: 1; }
.livrable__supprimer {
    margin-top: 1.55rem;
    border: none;
    background: transparent;
    color: #c0392b;
    font-size: 0.95rem;
    cursor: pointer;
}
.livrable__supprimer:hover:not(.livrable__supprimer--bloque) { color: #96281b; }
.livrable__supprimer--bloque {
    color: #b9c4d1;
    cursor: not-allowed;
}
.livrable__note {
    margin: 0.45rem 0 0;
    font-size: 0.75rem;
    color: #7a8899;
}

/* ==========================================================================
   14. MODAL OBJECTIF (nouveau)
   ========================================================================== */
.modal-objectif {
    display: flex;
    flex-direction: column;
    gap: 0.9rem;
}
.modal-objectif__erreur {
    margin: 0;
    padding: 0.5rem 0.65rem;
    border: 1px solid #f0c2c2;
    border-radius: 8px;
    background: #fdf1f1;
    font-size: 0.82rem;
    color: #a33a3a;
}

/* ==========================================================================
   15. ÉTATS CHARGEMENT / ERREUR (repris de Liste.vue)
   ========================================================================== */
.detail-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 3.5rem 1.5rem;
    text-align: center;
}
.empty-icon {
    font-size: 2.6rem;
    color: #c8d6fb;
    margin-bottom: 0.75rem;
}
.empty-title {
    font-size: 1.05rem;
    font-weight: 600;
    color: #1a2a6c;
    margin: 0 0 0.35rem;
}
.empty-text {
    font-size: 0.85rem;
    color: #6b7280;
    margin: 0 0 1.1rem;
}

/* ==========================================================================
   16. RESPONSIVE (ex form.css §12 + ajouts)
   ========================================================================== */
@media (max-width: 1199.98px) {
    .ajout-activite {
        padding-left: 24px;
        padding-right: 24px;
    }
}

@media (max-width: 1100px) {
    .form-layout {
        grid-template-columns: 1fr;
    }
    .form-side {
        position: static;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 16px;
    }
}

@media (max-width: 900px) {
    .form-actions {
        bottom: 10px;
    }
}

@media (max-width: 767.98px) {
    .ajout-activite {
        padding: 24px 16px 20px;
    }
    .field-grid {
        grid-template-columns: 1fr;
        gap: 16px;
    }
    .form-side {
        grid-template-columns: 1fr;
    }
    .form-actions {
        flex-direction: column;
        align-items: stretch;
        gap: 10px;
        text-align: center;
    }
    .form-actions-buttons {
        flex-direction: column-reverse;
        gap: 8px;
    }
    .form-actions-buttons :deep(.base-btn),
    .form-actions-buttons :deep(.btn) {
        width: 100%;
    }
    .ligne-ajoutable {
        flex-direction: column;
        align-items: stretch;
        gap: 0.3rem;
    }
    .ligne-ajoutable__meta {
        justify-content: flex-end;
        padding-top: 0;
    }
}

@media (max-width: 700px) {
    .form-actions {
        flex-direction: column;
        align-items: stretch;
    }
}

@media (max-width: 600px) {
    .form-section {
        padding: 18px 14px 20px;
    }
    .section-head {
        gap: 11px;
        margin-bottom: 18px;
    }
    .section-num {
        width: 30px;
        height: 30px;
        font-size: 11px;
        border-radius: 9px;
    }
    .section-head h2 {
        font-size: 15px;
    }
    .side-card {
        padding: 17px 15px 15px;
    }
    .form-actions {
        bottom: 0;
        padding: 12px 14px;
        border-radius: 12px;
    }
}

@media (max-width: 575.98px) {
    .ajout-activite {
        padding: 20px 12px 16px;
    }
    .objectif-champ__creer {
        margin-top: 20px;
        width: 40px;
        height: 40px;
    }
    .livrable__supprimer {
        margin-top: 0;
    }
}

/* ==========================================================================
   17. ACCESSIBILITÉ
   ========================================================================== */
@media (prefers-reduced-motion: reduce) {
    .form-layout *,
    .form-layout *::before,
    .form-layout *::after,
    .choix-type,
    .objectif-champ__creer {
        transition: none !important;
    }
}
</style>

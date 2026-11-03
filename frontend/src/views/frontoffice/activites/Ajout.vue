<script setup lang="ts">

import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { BaseButton, BaseConfirm, BaseInput, BaseModal, BaseSelect } from '@/components/base'
import { useAutocomplete, type Suggestion } from '@/composables/useAutocomplete'
import { useConfirmation } from '@/composables/useConfirmation'
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
    idResultatIntermediaire: number | null
    designation: string
}

const route = useRoute()
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

const serviceImpose = computed(() => options.services.length === 0)

const libelleServiceImpose = ref<string | null>(null)

const form = reactive({
    code: '',
    reference: '',
    designation: '',
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
// Période dérivée des sous-activités (computed, jamais envoyée)
// ------------------------------------------------------------------

/**
 * Date de début de l'activité = minimum des dates de début prévues de ses
 * sous-activités.
 *
 * **Calculée uniquement pour l'affichage et la validation front.** Elle
 * n'est PAS envoyée au backend : `ActiviteEcritureRequest` ne porte plus
 * ces champs, c'est `ActiviteEcritureService.resoudreDatesPrevues()` qui
 * recalcule min/max côté serveur. Le front reflète ce que le backend
 * produira, sans en être la source.
 */
const dateDebutCalculee = computed<string>(() => {
    const debuts = form.sousActivites
        .map(s => s.dateDebutPrevue)
        .filter((d): d is string => !!d && d.trim().length > 0)
        .sort()
    return debuts[0] ?? ''
})

/** Date de fin de l'activité = maximum des dates de fin prévues. Même logique. */
const dateFinCalculee = computed<string>(() => {
    const fins = form.sousActivites
        .map(s => s.dateFinPrevue)
        .filter((d): d is string => !!d && d.trim().length > 0)
        .sort()
    return fins[fins.length - 1] ?? ''
})

// ------------------------------------------------------------------
// La référence PTA est proposée ici ; les codes sont attribués par le backend.
// ------------------------------------------------------------------

const referencePersonnalise = ref(false)

const derniereReferenceProposee = ref<string | null>(null)

function surSaisieReference(valeur: string): void {
    referencePersonnalise.value = valeur !== derniereReferenceProposee.value
}

const objectifRetenu = ref<{ code: string | null; annee: number | null; prochainNumero: number | null } | null>(
    null
)

function proposerReference(suggestion: Suggestion): void {
    if (estModification.value) return

    objectifRetenu.value = {
        code: suggestion.code ?? null,
        annee: suggestion.annee ?? null,
        prochainNumero: suggestion.prochainNumero ?? null,
    }

    const annee = objectifRetenu.value.annee
    const numeroAct = objectifRetenu.value.prochainNumero

    if (annee !== null && typeof numeroAct === 'number') {
        if (!referencePersonnalise.value) {
            form.reference = `${objectifRetenu.value.code}_${numeroAct}`
            derniereReferenceProposee.value = form.reference
        }
    }
}

function oublierPropositionObjectif(): void {
    objectifRetenu.value = null
    referencePersonnalise.value = false
    derniereReferenceProposee.value = null
}

// ------------------------------------------------------------------
// Objectif specifique : autocomplete
// ------------------------------------------------------------------

function formatSuggestion(s: Suggestion): string {
    return s.libelleSecondaire
        ? `${s.code ?? ''} — ${s.libelle ?? ''} (${s.libelleSecondaire})`
        : `${s.code ?? ''} — ${s.libelle ?? ''}`
}

function afficherObjectif(s: Suggestion): void {
    form.idObjectifSpecifique = s.id
    autocompleteObjectif.saisie.value = formatSuggestion(s)

    proposerReference(s)
}

const autocompleteObjectif = useAutocomplete({
    rechercher: async (terme: string) => {
        const reponse = await activiteService.autocompleterObjectifs(terme)
        return reponse.success ? reponse.data : []
    },
    onSelectionner: afficherObjectif,
    onEffacer: retirerObjectif,
    delaiMs: 300,
})

function changerPta(valeur: boolean): void {
    form.pta = valeur
    if (valeur) return

    retirerObjectif()
    form.reference = ''
    referencePersonnalise.value = false
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

const modalObjectif = ref(false)
const enregistrementObjectif = ref(false)
const erreurObjectif = ref<string | null>(null)
const objectif = reactive({
    code: '',
    designation: '',
    annee: 2027 as number | string,
})
const erreursObjectif = reactive({
    code: '',
    designation: '',
    annee: '',
})

/**
 * Valeurs mémorisées pendant la confirmation : si l'utilisateur annule,
 * ou si l'API échoue, on rouvre la modal avec les mêmes valeurs.
 * Null quand aucune confirmation n'est en cours.
 */
const objectifEnCoursDeCreation = ref<{ code: string; designation: string; annee: number } | null>(null)

function ouvrirModalObjectif(): void {
    objectif.code = ''
    objectif.designation = ''
    objectif.annee = getCurrentYear()
    erreursObjectif.code = ''
    erreursObjectif.designation = ''
    erreursObjectif.annee = ''
    erreurObjectif.value = null
    objectifEnCoursDeCreation.value = null
    modalObjectif.value = true
}

function fermerModalObjectif(): void {
    if (enregistrementObjectif.value) return
    objectifEnCoursDeCreation.value = null
    modalObjectif.value = false
}

function creerObjectif(): void {
    erreursObjectif.code = ''
    erreursObjectif.designation = ''
    erreursObjectif.annee = ''

    if (!objectif.code.trim()) {
        erreursObjectif.code = 'Le code est obligatoire'
    } else if (objectif.code.trim().length > 20) {
        erreursObjectif.code = 'Le code ne peut pas dépasser 20 caractères'
    }

    if (!objectif.designation.trim()) {
        erreursObjectif.designation = 'La désignation est obligatoire'
    }

    const annee = Number(objectif.annee)

    if (objectif.annee === null || objectif.annee === '' || Number.isNaN(annee)) {
        erreursObjectif.annee = "L'année est obligatoire"
    } else if (!Number.isInteger(annee) || annee < 1900 || annee > 2100) {
        erreursObjectif.annee = "L'année doit être comprise entre 1900 et 2100"
    }

    if (erreursObjectif.code || erreursObjectif.designation || erreursObjectif.annee) return

    erreurObjectif.value = null

    const code = objectif.code.trim()
    const designation = objectif.designation.trim()

    // Mémorise les valeurs pour pouvoir rouvrir la modal en cas d'annulation.
    objectifEnCoursDeCreation.value = { code, designation, annee }

    // ⚠️ FERMER la modal AVANT d'ouvrir la confirmation : sinon les deux
    // fenêtres se superposent et la confirmation passe derrière la première.
    modalObjectif.value = false

    confirmation.demander({
        titre: 'Ajouter cet objectif',
        message: `L'objectif ${code} sera ajouté au référentiel des objectifs spécifiques.`,
        consequence: "Il pourra ensuite être choisi pour cette activité et pour les suivantes.",
        libelleConfirmer: 'Ajouter',
        ton: 'primary',
        icone: 'bi bi-plus-circle',
        action: () => creerObjectifReellement(code, designation, annee),
    })
}

async function creerObjectifReellement(code: string, designation: string, annee: number): Promise<void> {
    enregistrementObjectif.value = true

    const reponse = await activiteService.creerObjectifSpecifique({ code, designation, annee })

    enregistrementObjectif.value = false

    if (!reponse.success) {
        erreurObjectif.value = reponse.error
        // On laisse objectifEnCoursDeCreation non-null : le watcher ci-dessous
        // rouvrira la modal pour afficher l'erreur.
        return
    }

    // Succès : plus rien à rouvrir.
    objectifEnCoursDeCreation.value = null
    afficherObjectif(reponse.data)
    modalObjectif.value = false
}

/**
 * Rouvre la modal « Créer un objectif » quand la confirmation se ferme
 * sans succès (annulation ou erreur API).
 *
 * Cas couverts :
 *  - Annuler → confirmation.ouvert passe à false, objectifEnCoursDeCreation
 *    est encore rempli → on rouvre la modal avec les valeurs saisies.
 *  - Erreur API → erreurObjectif est positionnée, objectifEnCoursDeCreation
 *    reste rempli → on rouvre la modal pour afficher l'erreur.
 *  - Succès → objectifEnCoursDeCreation a été mis à null dans
 *    creerObjectifReellement, donc le watcher ne fait rien.
 */
watch(
    () => confirmation.ouvert.value,
    (ouvert, ancien) => {
        if (ancien && !ouvert && objectifEnCoursDeCreation.value) {
            const valeurs = objectifEnCoursDeCreation.value
            objectif.code = valeurs.code
            objectif.designation = valeurs.designation
            objectif.annee = valeurs.annee
            modalObjectif.value = true
            objectifEnCoursDeCreation.value = null
        }
    }
)

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

function estResultatEnregistre(ligne: LigneResultat): boolean {
    return ligne.idResultatIntermediaire !== null
}

// ------------------------------------------------------------------
// Sous-activites
// ------------------------------------------------------------------

function ajouterSousActivite(): void {
    const ligne: LigneSousActivite = {
        idSousActivite: null,
        // Période par défaut = période courante dérivée des SA existantes.
        // Vide si aucune SA n'a de dates (l'utilisateur devra les saisir).
        dateDebutPrevue: dateDebutCalculee.value,
        dateFinPrevue: dateFinCalculee.value,
        code: '',
        designation: '',
        livrables: [],
    }

    form.sousActivites.push(ligne)
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

type LigneLivrable = LivrableEcriture & { nombreFichiers: number }

type LigneSousActivite = Omit<SousActiviteEcriture, 'livrables'> & {
    livrables: LigneLivrable[]
}

function ajouterLivrable(sous: LigneSousActivite): void {
    sous.livrables.push({ designation: '', description: '', nombreFichiers: 0 })
}

function retirerLivrable(sous: LigneSousActivite, index: number): void {
    const livrable = sous.livrables[index]
    if (livrable !== undefined && !estProtege(livrable)) {
        sous.livrables.splice(index, 1)
    }
}

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

    if (form.pta && !form.reference.trim()) erreurs.reference = 'La référence est obligatoire'
    if (!form.designation.trim()) erreurs.designation = 'La désignation est obligatoire'

    if (form.sousActivites.length === 0 || !dateDebutCalculee.value) {
        erreurs.sousActivites = "Au moins une sous-activité avec des dates prévues est requise pour déterminer la période de l'activité"
    }

    if (form.pta && form.idObjectifSpecifique === null) {
        erreurs.idObjectifSpecifique = 'Un objectif spécifique est requis pour une activité PTA'
    }

    form.resultatsIntermediaires.forEach((resultat, index) => {
        if (!resultat.designation.trim()) {
            erreurs[`resultat_${index}`] = 'La désignation est obligatoire'
        }
    })

    form.sousActivites.forEach((sous, index) => {
        if (!sous.designation.trim()) {
            erreurs[`sous_${index}`] = 'La désignation est obligatoire'
        } else if (!sous.dateDebutPrevue || !sous.dateFinPrevue) {
            erreurs[`sous_${index}`] = 'Les dates prévues sont obligatoires'
        } else if (sous.dateFinPrevue < sous.dateDebutPrevue) {
            erreurs[`sous_${index}`] = 'La fin ne peut pas précéder le début'
        }

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
    if (id === null) return

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
    form.idTypeActivite = data.idTypeActivite
    form.idSite = data.idSite
    form.idPriorite = data.idPriorite
    form.idService = data.idService

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
        designation: s.designation,
        dateDebutPrevue: s.dateDebutPrevue,
        dateFinPrevue: s.dateFinPrevue,
        livrables: (s.livrables ?? []).map((l) => ({
            designation: l.designation,
            description: l.description ?? '',
            nombreFichiers: l.nombreFichiers,
        })),
    }))

    referencePersonnalise.value = true
    objectifRetenu.value = data.idObjectifSpecifique === null
        ? null
        : {
            code: data.objectifCode ?? null,
            annee: data.objectifAnnee ?? null,
            prochainNumero: null,
        }

    libelleServiceImpose.value = serviceImpose.value
        ? 'Votre service'
        : options.services.find((s) => s.id === data.idService)?.libelle ?? null
}

function construireCorps(): ActiviteEcriture {
    return {
        code: estModification.value ? form.code.trim() : '',
        reference: form.pta ? form.reference.trim() : null,
        designation: form.designation.trim(),
        pta: form.pta,
        idObjectifSpecifique: form.pta ? form.idObjectifSpecifique : null,
        idTypeActivite: form.idTypeActivite,
        idSite: form.idSite,
        idPriorite: form.idPriorite ?? null,
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
            livrables: s.livrables.map<LivrableEcriture>((livrable) => ({
                designation: livrable.designation.trim(),
                description: livrable.description.trim(),
            })),
        })),
    }
}

function enregistrer(): void {
    succes.value = null
    erreurGlobale.value = null

    if (!valider()) return

    const modification = estModification.value

    confirmation.demander({
        titre: modification ? "Modifier l'activité" : "Créer l'activité",
        message: modification
            ? "Les modifications enregistrées seront visibles de tous les utilisateurs de l'activité."
            : "L'activité sera créée en brouillon et son code sera attribué automatiquement.",
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
        erreurGlobale.value = reponse.error
        return
    }

    succes.value = estModification.value
        ? 'Activité modifiée.'
        : 'Activité créée en brouillon.'

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
        <BaseConfirm v-model="confirmation.ouvert.value" :demande="confirmation.demande.value"
            :loading="confirmation.enCours.value || enregistrement || enregistrementObjectif"
            @confirme="confirmation.confirmer" @annule="confirmation.annuler" />

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

        <div v-if="erreurGlobale" class="alert alert-danger alert-dismissible" role="alert">
            <i class="bi bi-exclamation-triangle me-2"></i>
            {{ erreurGlobale }}
            <button type="button" class="btn-close" @click="erreurGlobale = null"></button>
        </div>
        <div v-if="succes" class="alert alert-success" role="alert">
            <i class="bi bi-check-circle me-2"></i>
            {{ succes }}
        </div>

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
                        <button type="button" class="choix-type" :class="{ 'choix-type--actif': form.pta }" role="radio"
                            :aria-checked="form.pta" @click="changerPta(true)">
                            <span class="choix-type__icone"><i class="bi bi-bullseye"></i></span>
                            <span class="choix-type__titre">PTA</span>
                            <span class="choix-type__detail">
                                Plan de travail annuel. Rattachée à un objectif spécifique.
                            </span>
                            <span v-if="form.pta" class="choix-type__check">
                                <i class="bi bi-check-lg"></i>
                            </span>
                        </button>

                        <button type="button" class="choix-type" :class="{ 'choix-type--actif': !form.pta }"
                            role="radio" :aria-checked="!form.pta" @click="changerPta(false)">
                            <span class="choix-type__icone"><i class="bi bi-circle"></i></span>
                            <span class="choix-type__titre">Courante</span>
                            <span class="choix-type__detail">
                                Activité répondant aux besoins quotidiens et aux nécessités opérationnelles.
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
                            <p>Identifiez l'activité. Sa période est calculée à partir des sous-activités.</p>
                        </div>
                    </div>

                    <div class="field-grid">
                        <!-- Objectif spécifique -->
                        <div v-if="form.pta" class="field full">
                            <div :ref="autocompleteObjectif.racine">
                                <div class="position-relative">
                                    <div class="objectif-champ">
                                        <div class="objectif-champ__input">
                                            <BaseInput :model-value="autocompleteObjectif.saisie.value"
                                                label="Objectif spécifique" type="search" required
                                                placeholder="Code, désignation ou année" autocomplete="off"
                                                :error="erreurs.idObjectifSpecifique"
                                                :aria-expanded="autocompleteObjectif.ouvert.value" @update:model-value="(valeur: string) => {
                                                    autocompleteObjectif.saisie.value = valeur
                                                    autocompleteObjectif.charger()
                                                }" />
                                        </div>
                                        <button type="button" class="objectif-champ__creer" title="Créer un objectif"
                                            aria-label="Créer un objectif" :disabled="enregistrementObjectif"
                                            @click="ouvrirModalObjectif">
                                            <i class="bi bi-plus-lg"></i>
                                        </button>
                                    </div>

                                    <ul v-if="autocompleteObjectif.ouvert.value && autocompleteObjectif.suggestions.value.length"
                                        class="autocomplete" role="listbox" aria-label="Suggestions d'objectif">
                                        <li v-for="(s, i) in autocompleteObjectif.suggestions.value" :key="s.id"
                                            role="option" :aria-selected="i === autocompleteObjectif.indexActif.value"
                                            :class="{ 'autocomplete__item--actif': i === autocompleteObjectif.indexActif.value }"
                                            @mouseenter="autocompleteObjectif.survoler(i)">
                                            <button type="button" tabindex="-1"
                                                @click="autocompleteObjectif.selectionner(s)">
                                                {{ formatSuggestion(s) }}
                                            </button>
                                        </li>
                                    </ul>
                                </div>
                                <p class="hint">
                                    La référence est proposée d'après cet objectif ; le code sera attribué par le serveur.
                                    <template v-if="form.idObjectifSpecifique !== null">
                                        Vider le champ le retire.
                                    </template>
                                </p>
                            </div>
                        </div>

                        <!-- Code + Référence (PTA) -->
                        <template v-if="form.pta">
                            <div class="field">
                                <BaseInput v-model="form.code" label="Code" readonly
                                    placeholder="Attribué à l'enregistrement"
                                    helper="Généré automatiquement par le serveur." />
                            </div>
                            <div class="field">
                                <BaseInput v-model="form.reference" label="Référence" required
                                    :error="erreurs.reference" placeholder="BCT/1_1"
                                    helper="Code de l'objectif suivi du numéro d'activité. Modifiable."
                                    @update:model-value="surSaisieReference" />
                            </div>
                        </template>

                        <!-- Code seul (NON PTA) -->
                        <div v-else class="field full">
                            <BaseInput v-model="form.code" label="Code" readonly
                                placeholder="Attribué à l'enregistrement"
                                helper="Généré automatiquement par le serveur." />
                        </div>

                        <!-- Désignation -->
                        <div class="field full">
                            <BaseInput v-model="form.designation" label="Désignation" required
                                :error="erreurs.designation" placeholder="Objet de l'activité" />
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
                                <p class="hint">Facultatif. Un résultat est un état atteint en cours d'activité, pas sa
                                    finalité.</p>

                                <p v-if="form.resultatsIntermediaires.length === 0" class="aucun-resultat">
                                    Aucun résultat intermédiaire déclaré.
                                </p>

                                <div v-for="(resultat, index) in form.resultatsIntermediaires" :key="index"
                                    class="ligne-ajoutable">
                                    <div class="ligne-ajoutable__champ">
                                        <BaseInput v-model="resultat.designation" type="textarea"
                                            placeholder="Le système de contrôle d'accès est installé et opérationnel"
                                            :error="erreurs[`resultat_${index}`]" />
                                    </div>
                                    <div class="ligne-ajoutable__meta">
                                        <span v-if="estResultatEnregistre(resultat)" class="badge-enregistree">
                                            enregistrée
                                        </span>
                                        <button type="button" class="ligne-ajoutable__supprimer"
                                            :aria-label="`Retirer le résultat intermédiaire ${index + 1}`"
                                            @click="retirerResultatIntermediaire(index)">
                                            <i class="bi bi-trash"></i>
                                        </button>
                                    </div>
                                </div>
                            </div>
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
                            <BaseSelect :model-value="form.idPriorite ?? undefined" label="Priorité"
                                placeholder="Aucune" :options="optionsPriorite" option-label="libelle" option-value="id"
                                :error="erreurs.idPriorite"
                                @update:model-value="form.idPriorite = ($event as number | null) ?? null" />
                        </div>

                        <div class="field">
                            <BaseSelect :model-value="form.idTypeActivite ?? undefined" label="Type d'activité"
                                placeholder="Aucun" :options="optionsType" option-label="libelle" option-value="id"
                                @update:model-value="form.idTypeActivite = ($event as number | null) ?? null" />
                        </div>

                        <div class="field">
                            <BaseSelect :model-value="form.idSite ?? undefined" label="Site" placeholder="Aucun"
                                :options="optionsSite" option-label="libelle" option-value="id"
                                @update:model-value="form.idSite = ($event as number | null) ?? null" />
                        </div>

                        <div class="field">
                            <template v-if="serviceImpose">
                                <span class="label">Service</span>
                                <BaseInput :model-value="libelleServiceImpose ?? 'Votre service'" disabled readonly />
                                <p class="hint">Imposé par votre rattachement.</p>
                            </template>
                            <BaseSelect v-else :model-value="form.idService ?? undefined" label="Service" required
                                placeholder="Sélectionner" :options="optionsService" option-label="libelle"
                                option-value="id"
                                @update:model-value="form.idService = ($event as number | null) ?? null" />
                        </div>
                    </div>
                </section>

                <!-- 04 — SOUS-ACTIVITÉS -->
                <section class="panel form-section">
                    <div class="section-head section-head--with-action">
                        <span class="section-num">04</span>
                        <div>
                            <h2>Sous-activités</h2>
                            <p>Facultatif. Le code sera généré à partir des références de l'activité parente.</p>
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
                            <button type="button" class="sous-activite__supprimer"
                                :aria-label="`Retirer la sous-activité ${index + 1}`"
                                @click="retirerSousActivite(index)">
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>

                        <div class="field-grid">
                            <div class="field">
                                <BaseInput v-model="sous.code" label="Code" readonly
                                    placeholder="Attribué à l'enregistrement" />
                            </div>
                            <div class="field">
                                <BaseInput v-model="sous.designation" label="Désignation" required
                                    :error="erreurs[`sous_${index}`]" />
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

                            <div v-for="(livrable, position) in sous.livrables" :key="position" class="livrable">
                                <div class="livrable__ligne">
                                    <div class="livrable__designation">
                                        <BaseInput v-model="livrable.designation" placeholder="Désignation du livrable"
                                            required :error="erreurs[`livrable_${index}_${position}`]" />
                                    </div>
                                    <button type="button" class="livrable__supprimer"
                                        :class="{ 'livrable__supprimer--bloque': estProtege(livrable) }"
                                        :disabled="estProtege(livrable)" :title="estProtege(livrable)
                                            ? 'Ce livrable porte des fichiers : il fait partie de l\'historique de l\'activité'
                                            : 'Retirer ce livrable'"
                                        :aria-label="`Retirer le livrable ${position + 1}`"
                                        @click="retirerLivrable(sous, position)">
                                        <i class="bi bi-trash"></i>
                                    </button>
                                </div>
                                <BaseInput v-model="livrable.description" type="textarea" :rows="2"
                                    placeholder="Description (facultative) : contenu attendu, format, destinataire…" />
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

            <!-- SIDEBAR -->
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
                            <strong>{{ dateDebutCalculee || '—' }}</strong>
                        </div>
                        <div class="recap-row">
                            <span>Fin</span>
                            <strong>{{ dateFinCalculee || '—' }}</strong>
                        </div>
                        <div class="recap-row">
                            <span>Priorité</span>
                            <strong>{{options.priorites.find(p => p.id === form.idPriorite)?.libelle ?? '—'}}</strong>
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
                        <li><span>1</span> Le code et la référence sont proposés automatiquement d'après l'objectif
                            choisi.</li>
                        <li><span>2</span> Un objectif spécifique est requis pour une activité PTA — il peut être créé à
                            la volée.
                        </li>
                        <li><span>3</span> La période de l'activité (début, fin) est calculée à partir des
                            sous-activités.</li>
                    </ul>
                </section>
            </aside>

            <!-- BARRE D'ACTIONS -->
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

        <!-- MODAL OBJECTIF -->
        <BaseModal v-model="modalObjectif" title="Créer un objectif spécifique" size="md" :closeOnOutside="false">
            <div class="modal-objectif">
                <p class="hint">L'activité sera rattachée à cet objectif, qui sera retenu dans le champ ci-dessus.</p>
                <BaseInput v-model="objectif.code" label="Code (20 caractères max)" required
                    :error="erreursObjectif.code" placeholder="BCT/7" />
                <BaseInput v-model="objectif.designation" label="Désignation" type="textarea" :rows="4" required
                    :error="erreursObjectif.designation"
                    placeholder="Description complète de l'objectif (cibles chiffrées, contexte, portée…)" />
                <BaseInput v-model.number="objectif.annee" label="Année" type="number" required
                    :error="erreursObjectif.annee" placeholder="2027" />
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
   1. CONTENEUR PAGE
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

.panel {
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 14px;
    box-shadow:
        0 1px 2px rgba(26, 42, 108, .04),
        0 8px 24px rgba(26, 42, 108, .06);
}

/* ==========================================================================
   2. MISE EN PAGE
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
   3. SECTIONS
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

.section-head--with-action>.lien-ajout--cta {
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
   4. GRILLE
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
   5. LIBELLÉS
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
   6. BARRE D'ACTIONS
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
   7. SIDEBAR
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
   8. CHOIX PTA / NON PTA
   ========================================================================== */
.choix-type-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
}

@media (max-width: 640px) {
    .choix-type-grid {
        grid-template-columns: 1fr;
    }
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
   9. CHAMP OBJECTIF + BOUTON "+" — fusionnés et bleu
   L'input et le bouton forment un seul composant visuel : coins droits de
   l'input aplatis, coins gauches du bouton aplatis, bordure commune bleue.
   ========================================================================== */
.objectif-champ {
    display: flex;
    align-items: flex-end;
    gap: 0;
    /* ← fusion, plus d'espace */
}

.objectif-champ__input {
    flex: 1;
    min-width: 0;
    position: relative;
}

/* Aplatit les coins droits de l'input pour coller au bouton.
   :deep() est nécessaire car l'input est rendu par BaseInput. */
.objectif-champ__input :deep(input),
.objectif-champ__input :deep(textarea),
.objectif-champ__input :deep(.base-input__field),
.objectif-champ__input :deep(.form-input) {
    border-top-right-radius: 0 !important;
    border-bottom-right-radius: 0 !important;
}

/* Quand l'input est focus, garder une bordure continue avec le bouton. */
.objectif-champ__input :deep(input:focus),
.objectif-champ__input :deep(.base-input__field:focus),
.objectif-champ__input :deep(.form-input:focus) {
    border-right-color: #4a6cf7 !important;
    z-index: 1;
    position: relative;
}

.objectif-champ__creer {
    flex-shrink: 0;
    width: 44px;
    height: 48px;
    margin-top: var(--objectif-label-height);
    border: 1px solid #1a2a6c;
    border-left: none;
    /* ← colle à l'input */
    border-top-left-radius: 0;
    border-bottom-left-radius: 0;
    border-top-right-radius: 10px;
    border-bottom-right-radius: 10px;
    background: #1a2a6c;
    /* ← bleu du projet */
    color: #fff;
    /* ← icône blanche */
    font-size: 1.05rem;
    cursor: pointer;
    transition: background .15s, border-color .15s;
}

.objectif-champ__creer:hover:not(:disabled) {
    background: #223580;
    border-color: #223580;
}

.objectif-champ__creer:focus-visible {
    outline: 2px solid #1a2a6c;
    outline-offset: 2px;
}

.objectif-champ__creer:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

@media (max-width: 575.98px) {
    .objectif-champ {
        --objectif-label-height: 20px;
    }

    .objectif-champ__creer {
        width: 40px;
        height: 40px;
    }
}

/* ==========================================================================
   10. AUTOCOMPLETE
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
   11. BLOCS AJOUTABLES
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

.lien-ajout:hover {
    text-decoration: underline;
}

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

.ligne-ajoutable__champ {
    flex: 1 1 auto;
    min-width: 0;
}

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

.ligne-ajoutable__supprimer:hover {
    color: #96281b;
}

.badge-enregistree {
    padding: 0.15rem 0.5rem;
    border-radius: 5px;
    background: #eef5f8;
    color: #5b6b80;
    font-size: 0.7rem;
    font-weight: 600;
}

/* ==========================================================================
   12. SOUS-ACTIVITÉS
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

.sous-activite__supprimer:hover {
    color: #96281b;
}

/* ==========================================================================
   13. LIVRABLES
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

.livrable:last-child {
    margin-bottom: 0;
}

.livrable__ligne {
    display: flex;
    align-items: flex-start;
    gap: 0.5rem;
}

.livrable__designation {
    flex: 1;
}

.livrable__supprimer {
    margin-top: 1.55rem;
    border: none;
    background: transparent;
    color: #c0392b;
    font-size: 0.95rem;
    cursor: pointer;
}

.livrable__supprimer:hover:not(.livrable__supprimer--bloque) {
    color: #96281b;
}

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
   14. MODAL OBJECTIF
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
   15. ÉTATS CHARGEMENT / ERREUR
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
   16. RESPONSIVE
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

    .livrable__supprimer {
        margin-top: 0;
    }
}

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
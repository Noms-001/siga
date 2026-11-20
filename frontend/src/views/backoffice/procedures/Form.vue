<script setup lang="ts">
import { useToastMessage } from '@/composables/useToast'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BaseInput, BaseButton } from '@/components/base'
import {
    creerProcedure,
    getProcedure,
    modifierProcedure,
} from '@/services/procedure'
import { listerPostes } from '@/services/poste'
import type {
    EtapeValidationRequest,
    ProcedureRequest,
} from '@/types/backoffice/procedure'
import type { Poste } from '@/types/backoffice/poste'
import { useFloatingDropdown } from '@/composables/useFloatingDropdown'


const route = useRoute()
const router = useRouter()

const isEdition = computed(() => route.name === 'backoffice-procedures-modifier')
const id = computed<number | null>(() =>
    isEdition.value ? Number(route.params.id) : null
)

/* -------------------- formulaire principal -------------------- */

const form = ref<{
    designation: string
    description: string
    etapes: EtapeValidationRequest[]
}>({
    designation: '',
    description: '',
    etapes: [],
})

/* -------------------- état -------------------- */

const postes = ref<Poste[]>([])
const errors = ref<Record<string, string>>({})
const globalError = useToastMessage('error')
const loading = ref(false)
const submitting = ref(false)

const postesActifs = computed(() => postes.value.filter(p => p.actif))

/* -------------------- récapitulatif -------------------- */

/** Récapitulatif live des valeurs saisies. */
const resume = computed(() => {
    const designation = form.value.designation.trim() || 'Nouvelle procédure'
    const nbEtapes = form.value.etapes.length
    const totalDecideurs = form.value.etapes.reduce(
        (sum, e) => sum + e.idPostesDecideurs.length,
        0,
    )
    const etapesIncompletes = form.value.etapes.filter(
        e => !e.designation.trim() || e.idPostesDecideurs.length === 0,
    ).length
    return { designation, nbEtapes, totalDecideurs, etapesIncompletes }
})

/* -------------------- chargement -------------------- */

async function charger(): Promise<void> {
    loading.value = true

    const postesRes = await listerPostes()
    if (postesRes.success) postes.value = postesRes.data

    if (isEdition.value && id.value != null) {
        const res = await getProcedure(id.value)
        if (!res.success) {
            loading.value = false
            globalError.value = res.error
            return
        }

        form.value = {
            designation: res.data.designation,
            description: res.data.description ?? '',
            etapes: res.data.etapes.map(e => ({
                id: e.id,
                designation: e.designation,
                description: e.description ?? '',
                obligatoire: e.obligatoire,
                retour: e.retour,
                idPostesDecideurs: e.decideurs.map(d => d.id),
            })),
        }
    }

    loading.value = false
}

/* -------------------- gestion des étapes -------------------- */

function ajouterEtape(): void {
    form.value.etapes.push({
        id: null,
        designation: '',
        description: '',
        obligatoire: true,
        retour: false,
        idPostesDecideurs: [],
    })
}

function retirerEtape(index: number): void {
    form.value.etapes.splice(index, 1)
}

function deplacerEtape(index: number, direction: -1 | 1): void {
    const cible = index + direction
    if (cible < 0 || cible >= form.value.etapes.length) return

    const etape = form.value.etapes[index]

    if (!etape) return

    form.value.etapes.splice(index, 1)
    form.value.etapes.splice(cible, 0, etape)
}

function basculerDecideur(etape: EtapeValidationRequest, idPoste: number): void {
    const idx = etape.idPostesDecideurs.indexOf(idPoste)
    if (idx >= 0) etape.idPostesDecideurs.splice(idx, 1)
    else etape.idPostesDecideurs.push(idPoste)
}

/* -------------------- validation & soumission -------------------- */

function valider(): boolean {
    errors.value = {}

    if (!form.value.designation.trim()) {
        errors.value.designation = 'La désignation est obligatoire'
    }

    form.value.etapes.forEach((e, i) => {
        if (!e.designation.trim()) {
            errors.value[`etape-${i}-designation`] = 'Désignation obligatoire'
        }
        if (e.idPostesDecideurs.length === 0) {
            errors.value[`etape-${i}-decideurs`] = 'Au moins un décideur est requis'
        }
    })

    return Object.keys(errors.value).length === 0
}

async function soumettre(): Promise<void> {
    globalError.value = ''
    if (!valider()) return
    submitting.value = true

    const payload: ProcedureRequest = {
        designation: form.value.designation.trim(),
        description: form.value.description?.trim() || null,
        etapes: form.value.etapes.map(e => ({
            id: e.id,
            designation: e.designation.trim(),
            description: e.description?.trim() || null,
            obligatoire: e.obligatoire,
            retour: e.retour,
            idPostesDecideurs: [...e.idPostesDecideurs],
        })),
    }

    const res = isEdition.value && id.value != null
        ? await modifierProcedure(id.value, payload)
        : await creerProcedure(payload)
    submitting.value = false

    if (!res.success) { globalError.value = res.error; return }
    router.push({
        name: 'backoffice-procedures',
        query: isEdition.value ? { updated: '1' } : { created: '1' },
    })
}

function annuler(): void {
    router.push({ name: 'backoffice-procedures' })
}

/** Index de l'étape dont le dropdown est ouvert — un seul à la fois. */
const decidersOuvert = ref<number | null>(null)
/** Texte de recherche par étape (index → texte). */
const decidersRecherche = ref<Record<number, string>>({})

/**
 * Postes filtrés par la recherche propre à une étape.
 *
 * Chaque étape a sa propre recherche : l'utilisateur peut chercher
 * « chef » pour l'étape 1 puis « tech » pour l'étape 2 sans que les deux
 * textes ne se mélangent.
 */
function postesFiltres(index: number) {
    const q = (decidersRecherche.value[index] ?? '').trim().toLowerCase()
    if (!q) return postesActifs.value
    return postesActifs.value.filter(p => p.nom.toLowerCase().includes(q))
}

function toggleDeciders(index: number): void {
    decidersOuvert.value = decidersOuvert.value === index ? null : index
    // Réinitialise la recherche à chaque ouverture : rouvrir un select doit
    // repartir de la liste complète, pas de la dernière recherche.
    if (decidersOuvert.value === index) {
        decidersRecherche.value[index] = ''
    }
}

function fermerDeciders(): void {
    decidersOuvert.value = null
}

/**
 * Nom d'un poste à partir de son id.
 *
 * Le poste peut avoir été désactivé depuis la dernière sauvegarde :
 * il n'apparaît alors plus dans `postesActifs`, mais l'id reste dans
 * `idPostesDecideurs`. On affiche « Poste #12 » plutôt que rien, pour
 * signaler à l'utilisateur qu'un décideur a disparu du référentiel actif.
 */
function nomPoste(idPoste: number): string {
    const p = postes.value.find(x => x.id === idPoste)
    return p ? p.nom : `Poste #${idPoste}`
}

function posteInactif(idPoste: number): boolean {
    const p = postes.value.find(x => x.id === idPoste)
    return p ? !p.actif : false
}
// …

/** Une ref par étape pour le trigger (le bouton cliqué). */
const decidersTriggers = ref<Record<number, HTMLElement | null>>({})
/** Une ref par étape pour le dropdown (rendu dans le Teleport). */
const decidersDropdowns = ref<Record<number, HTMLElement | null>>({})

/**
 * Le composable ne gère qu'un seul dropdown à la fois : le nôtre ne peut
 * en avoir qu'un d'ouvert (decidersOuvert est un index unique).
 */
const decidersIsOpen = computed(() => decidersOuvert.value !== null)

const decidersTriggerRef = computed<HTMLElement | null>(() => {
    const idx = decidersOuvert.value
    return idx === null ? null : (decidersTriggers.value[idx] ?? null)
})

const decidersDropdownRef = computed<HTMLElement | null>(() => {
    const idx = decidersOuvert.value
    return idx === null ? null : (decidersDropdowns.value[idx] ?? null)
})

const { style: decidersStyle } = useFloatingDropdown(
    decidersTriggerRef,
    decidersDropdownRef,
    decidersIsOpen,
    { estimatedHeight: 260, zIndex: 1050 },
)

/** Raccourci pour poser la ref sur le trigger d'une étape. */
function setTriggerRef(index: number, el: Element | null): void {
    decidersTriggers.value[index] = el as HTMLElement | null
}

/** Idem pour le dropdown. */
function setDropdownRef(index: number, el: Element | null): void {
    decidersDropdowns.value[index] = el as HTMLElement | null
}

onMounted(() => {
    charger()
    document.addEventListener('click', fermerDecidersSiExterne)
})

onBeforeUnmount(() => {
    document.removeEventListener('click', fermerDecidersSiExterne)
})

function fermerDecidersSiExterne(event: MouseEvent): void {
    const cible = event.target as HTMLElement

    if (!cible.closest('.deciders-select, .deciders-select__dropdown')) {
        decidersOuvert.value = null
    }
}
</script>

<template>
    <div class="bo-page">
        <header class="bo-page__header">
            <div>
                <h1 class="bo-page__title">
                    {{ isEdition ? 'Modifier une procédure' : 'Nouvelle procédure' }}
                </h1>
                <p class="bo-page__subtitle">
                    {{ isEdition
                        ? 'Mettre à jour la procédure et ses étapes de validation'
                        : 'Créer une procédure et ses étapes de validation' }}
                </p>
            </div>
        </header>

        <div class="bo-layout">
            <div class="bo-layout__main">
                <div class="bo-form">
                    <BaseInput v-model="form.designation" label="Désignation"
                        placeholder="Ex : Procédure de validation d'activité" required :error="errors.designation"
                        :disabled="loading || submitting" />

                    <BaseInput v-model="form.description" type="textarea" label="Description"
                        placeholder="Description de la procédure" :disabled="loading || submitting" />
                </div>

                <!-- Étapes de validation -->
                <div class="bo-etapes">
                    <header class="bo-etapes__head">
                        <div>
                            <h2 class="bo-etapes__title">Étapes de validation</h2>
                            <p class="bo-etapes__hint">
                                Définissez les étapes dans l'ordre. Le niveau est attribué
                                automatiquement selon la position.
                            </p>
                        </div>
                        <BaseButton icon="bi bi-plus-lg" size="sm" :disabled="submitting" @click="ajouterEtape">
                            Ajouter une étape
                        </BaseButton>
                    </header>

                    <div v-if="form.etapes.length === 0" class="bo-etapes__empty">
                        Aucune étape définie. Une procédure sans étape ne pourra pas
                        être utilisée pour valider des activités.
                    </div>

                    <div v-else class="bo-etapes__list">
                        <div v-for="(etape, index) in form.etapes" :key="index" class="bo-etape">

                            <div class="bo-etape__head">
                                <span class="bo-etape__niveau">Niveau {{ index + 1 }}</span>

                                <div class="bo-etape__actions">
                                    <button type="button" class="table-action" title="Monter"
                                        :disabled="index === 0 || submitting" @click="deplacerEtape(index, -1)">
                                        <i class="bi bi-arrow-up"></i>
                                    </button>
                                    <button type="button" class="table-action" title="Descendre"
                                        :disabled="index === form.etapes.length - 1 || submitting"
                                        @click="deplacerEtape(index, 1)">
                                        <i class="bi bi-arrow-down"></i>
                                    </button>
                                    <button type="button" class="table-action table-action--danger" title="Retirer"
                                        :disabled="submitting" @click="retirerEtape(index)">
                                        <i class="bi bi-trash"></i>
                                    </button>
                                </div>
                            </div>

                            <div class="bo-etape__body">
                                <BaseInput v-model="etape.designation" label="Désignation"
                                    placeholder="Ex : Validation par le chef de service" required
                                    :error="errors[`etape-${index}-designation`]" :disabled="submitting" />

                                <BaseInput v-model="etape.description" type="textarea" label="Description"
                                    placeholder="Optionnel" :disabled="submitting" />

                                <div class="bo-etape__switches">
                                    <label class="bo-check">
                                        <input type="checkbox" v-model="etape.obligatoire" :disabled="submitting">
                                        <span>Étape obligatoire</span>
                                    </label>
                                    <label class="bo-check">
                                        <input type="checkbox" v-model="etape.retour" :disabled="submitting">
                                        <span>Retour possible</span>
                                    </label>
                                </div>

                                <div class="bo-etape__decideurs">
                                    <label class="bo-etape__decideurs-label">
                                        Décideurs <span class="required">*</span>
                                    </label>

                                    <div class="deciders-select">
                                        <!-- Trigger -->
                                        <button :ref="(el) => setTriggerRef(index, el as Element | null)" type="button"
                                            class="deciders-select__trigger"
                                            :class="{ 'deciders-select__trigger--open': decidersOuvert === index }"
                                            :disabled="submitting || postesActifs.length === 0"
                                            @click.stop="toggleDeciders(index)">
                                            <span v-if="etape.idPostesDecideurs.length === 0"
                                                class="deciders-select__placeholder">
                                                {{ postesActifs.length === 0
                                                    ? 'Aucun poste actif disponible'
                                                    : 'Sélectionner un ou plusieurs décideurs' }}
                                            </span>
                                            <span v-else class="deciders-select__count">
                                                {{ etape.idPostesDecideurs.length }}
                                                décideur{{ etape.idPostesDecideurs.length > 1 ? 's' : '' }}
                                                sélectionné{{
                                                    etape.idPostesDecideurs.length > 1 ? 's' : '' }}
                                            </span>
                                            <i class="bi bi-chevron-down deciders-select__caret"></i>
                                        </button>

                                        <!-- Dropdown : rendu dans le body via Teleport, positionné en fixed -->
                                        <Teleport to="body">
                                            <div v-if="decidersOuvert === index"
                                                :ref="(el) => setDropdownRef(index, el as Element | null)"
                                                class="deciders-select__dropdown" :style="decidersStyle" @click.stop>
                                                <div class="deciders-select__search">
                                                    <i class="bi bi-search"></i>
                                                    <input type="text" placeholder="Rechercher un poste…"
                                                        v-model="decidersRecherche[index]" />
                                                </div>

                                                <ul class="deciders-select__list">
                                                    <li v-if="postesFiltres(index).length === 0"
                                                        class="deciders-select__empty">
                                                        Aucun poste ne correspond à la recherche.
                                                    </li>
                                                    <li v-for="p in postesFiltres(index)" :key="p.id"
                                                        class="deciders-select__item"
                                                        :class="{ 'deciders-select__item--selected': etape.idPostesDecideurs.includes(p.id) }"
                                                        @click="basculerDecideur(etape, p.id)">
                                                        <span class="deciders-select__check">
                                                            <i v-if="etape.idPostesDecideurs.includes(p.id)"
                                                                class="bi bi-check-lg"></i>
                                                        </span>
                                                        <span class="deciders-select__nom">
                                                            {{ p.nom }}
                                                            <span v-if="!p.isMetier"
                                                                class="deciders-select__tag">support</span>
                                                        </span>
                                                    </li>
                                                </ul>
                                            </div>
                                        </Teleport>
                                    </div>

                                    <!-- Badges : s'affichent sous le select, un par décideur retenu -->
                                    <div v-if="etape.idPostesDecideurs.length > 0" class="deciders-badges">
                                        <span v-for="idPoste in etape.idPostesDecideurs" :key="idPoste"
                                            class="deciders-badge"
                                            :class="{ 'deciders-badge--inactive': posteInactif(idPoste) }">
                                            <i class="bi bi-person-badge"></i>
                                            <span>{{ nomPoste(idPoste) }}</span>
                                            <button type="button" class="deciders-badge__remove" :disabled="submitting"
                                                :aria-label="`Retirer ${nomPoste(idPoste)}`"
                                                @click="basculerDecideur(etape, idPoste)">
                                                <i class="bi bi-x"></i>
                                            </button>
                                        </span>
                                    </div>

                                    <small v-if="errors[`etape-${index}-decideurs`]" class="error-message">
                                        {{ errors[`etape-${index}-decideurs`] }}
                                    </small>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="bo-form__actions">
                    <BaseButton variant="secondary" :disabled="submitting" @click="annuler">
                        Annuler
                    </BaseButton>
                    <BaseButton :loading="submitting" @click="soumettre">
                        {{ isEdition ? 'Enregistrer' : 'Créer' }}
                    </BaseButton>
                </div>
            </div>

            <!-- Panneau récapitulatif -->
            <aside class="bo-resume" aria-label="Récapitulatif">
                <header class="bo-resume__head">
                    <i class="bi bi-clipboard-check"></i>
                    <h2 class="bo-resume__title">Récapitulatif</h2>
                </header>

                <div class="bo-resume__section">
                    <div class="bo-resume__label">Procédure</div>
                    <div class="bo-resume__value bo-resume__value--strong">{{ resume.designation }}</div>
                    <div v-if="form.description.trim()" class="bo-resume__desc">
                        {{ form.description }}
                    </div>
                </div>

                <hr class="bo-resume__sep">

                <div class="bo-resume__stats">
                    <div class="bo-resume__stat">
                        <span class="bo-resume__stat-value">{{ resume.nbEtapes }}</span>
                        <span class="bo-resume__stat-label">
                            Étape{{ resume.nbEtapes > 1 ? 's' : '' }}
                        </span>
                    </div>
                    <div class="bo-resume__stat">
                        <span class="bo-resume__stat-value">{{ resume.totalDecideurs }}</span>
                        <span class="bo-resume__stat-label">
                            Décideur{{ resume.totalDecideurs > 1 ? 's' : '' }}
                        </span>
                    </div>
                </div>

                <div v-if="form.etapes.length > 0" class="bo-resume__section">
                    <div class="bo-resume__label">Détail des étapes</div>
                    <ol class="bo-resume__steps">
                        <li v-for="(etape, index) in form.etapes" :key="index" class="bo-resume__step"
                            :class="{ 'bo-resume__step--incomplete': !etape.designation.trim() || etape.idPostesDecideurs.length === 0 }">
                            <div class="bo-resume__step-head">
                                <span class="bo-resume__step-niveau">N{{ index + 1 }}</span>
                                <span class="bo-resume__step-designation">
                                    {{ etape.designation.trim() || 'Sans désignation' }}
                                </span>
                            </div>
                            <div class="bo-resume__step-meta">
                                <span class="bo-resume__tag" :class="{ 'bo-resume__tag--required': etape.obligatoire }">
                                    {{ etape.obligatoire ? 'Obligatoire' : 'Optionnelle' }}
                                </span>
                                <span v-if="etape.retour" class="bo-resume__tag">Retour</span>
                            </div>
                            <div v-if="etape.idPostesDecideurs.length > 0" class="bo-resume__step-decideurs">
                                <i class="bi bi-person-badge"></i>
                                <span>{{etape.idPostesDecideurs.map(id => nomPoste(id)).join(', ')}}</span>
                            </div>
                            <div v-else class="bo-resume__step-warning">
                                <i class="bi bi-exclamation-circle"></i> Aucun décideur
                            </div>
                        </li>
                    </ol>
                </div>

                <div v-else class="bo-resume__empty">
                    Aucune étape définie
                </div>

                <div v-if="resume.etapesIncompletes > 0" class="bo-resume__warning">
                    <i class="bi bi-exclamation-triangle"></i>
                    {{ resume.etapesIncompletes }}
                    étape{{ resume.etapesIncompletes > 1 ? 's' : '' }}
                    incomplète{{ resume.etapesIncompletes > 1 ? 's' : '' }}
                </div>
            </aside>
        </div>
    </div>
</template>

<style scoped>
.bo-page {
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.bo-page__header {
    display: flex;
    flex-wrap: wrap;
    gap: 1rem;
    justify-content: space-between;
}

.bo-page__title {
    font-size: 1.4rem;
    font-weight: 700;
    color: var(--dts-navy);
    margin: 0;
}

.bo-page__subtitle {
    color: var(--dts-muted);
    margin: 0;
    font-size: 0.85rem;
}

/* ============ Layout 2 colonnes ============ */
.bo-layout {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 300px;
    gap: 1.5rem;
    align-items: start;
    max-width: 1144px;
    /* 820 (form) + 300 (aside) + 24 (gap) */
    width: 100%;
}

.bo-layout__main {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    min-width: 0;
}

.bo-form {
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

/* --- étapes --- */

.bo-etapes {
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.bo-etapes__head {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 1rem;
    flex-wrap: wrap;
}

.bo-etapes__title {
    font-size: 1rem;
    font-weight: 700;
    color: var(--dts-navy);
    margin: 0;
}

.bo-etapes__hint {
    color: var(--dts-muted);
    font-size: 0.8rem;
    margin: 0.15rem 0 0;
}

.bo-etapes__empty {
    padding: 1.25rem;
    text-align: center;
    font-size: 0.85rem;
    color: var(--dts-muted);
    background: var(--dts-bg);
    border: 1px dashed var(--dts-border-2);
    border-radius: var(--dts-radius-sm);
}

.bo-etapes__list {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.bo-etape {
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius-sm);
    background: var(--dts-bg);
}

.bo-etape__head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.5rem 0.85rem;
    background: var(--dts-surface);
    border-bottom: 1px solid var(--dts-border);
}

.bo-etape__niveau {
    font-size: 0.75rem;
    font-weight: 700;
    color: var(--dts-navy);
    letter-spacing: 0.03em;
    text-transform: uppercase;
}

.bo-etape__actions {
    display: flex;
    gap: 0.35rem;
}

.bo-etape__body {
    padding: 1rem 1.15rem;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.bo-etape__switches {
    display: flex;
    flex-wrap: wrap;
    gap: 1.25rem;
    padding: 0.65rem 0.85rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius-sm);
}

.bo-etape__decideurs {
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
}

.bo-etape__decideurs-label {
    font-size: 0.9rem;
    font-weight: 600;
    color: var(--text-color);
}

/* --- Multiselect décideurs --- */

.deciders-select {
    position: relative;
}

.deciders-select__trigger {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.75rem;
    padding: 0.65rem 0.9rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius-sm);
    color: var(--dts-text);
    font-size: 0.85rem;
    text-align: left;
    cursor: pointer;
    transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.deciders-select__trigger:hover:not(:disabled) {
    border-color: #9dc2e0;
}

.deciders-select--open .deciders-select__trigger {
    border-color: var(--dts-blue);
    box-shadow: 0 0 0 3px rgba(31, 111, 178, 0.12);
}

.deciders-select__trigger:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.deciders-select__placeholder {
    color: var(--dts-muted);
}

.deciders-select__count {
    color: var(--dts-blue);
    font-weight: 600;
}

.deciders-select__caret {
    color: var(--dts-muted);
    font-size: 0.75rem;
    transition: transform 0.15s ease;
}

.deciders-select--open .deciders-select__caret {
    transform: rotate(180deg);
}

.deciders-select__trigger--open {
    border-color: var(--dts-blue);
    box-shadow: 0 0 0 3px rgba(31, 111, 178, 0.12);
}

/* --- Dropdown --- */

.deciders-select__dropdown {
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius-sm);
    box-shadow: 0 8px 24px rgba(13, 43, 78, 0.12);
    overflow: hidden;
}

.deciders-select__search {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 0.75rem;
    border-bottom: 1px solid var(--dts-border);
    background: var(--dts-bg);
}

.deciders-select__search i {
    color: var(--dts-muted);
    font-size: 0.85rem;
}

.deciders-select__search input {
    width: 100%;
    border: none;
    outline: none;
    background: transparent;
    font-size: 0.85rem;
    color: var(--dts-text);
}

.deciders-select__search input::placeholder {
    color: var(--dts-muted);
}

.deciders-select__list {
    list-style: none;
    margin: 0;
    padding: 0.25rem 0;
    max-height: 220px;
    overflow-y: auto;
}

.deciders-select__item {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    padding: 0.45rem 0.9rem;
    font-size: 0.85rem;
    cursor: pointer;
    transition: background-color 0.12s ease;
}

.deciders-select__item:hover {
    background: var(--dts-blue-50);
}

.deciders-select__item--selected {
    color: var(--dts-blue);
    font-weight: 600;
}

.deciders-select__check {
    width: 16px;
    height: 16px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: 1px solid var(--dts-border);
    border-radius: 4px;
    background: var(--dts-surface);
    color: var(--dts-blue);
    flex-shrink: 0;
}

.deciders-select__item--selected .deciders-select__check {
    background: var(--dts-blue);
    border-color: var(--dts-blue);
    color: #fff;
}

.deciders-select__nom {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    min-width: 0;
}

.deciders-select__tag {
    display: inline-block;
    padding: 0.05rem 0.4rem;
    font-size: 0.65rem;
    font-weight: 600;
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
    border-radius: 20px;
}

.deciders-select__empty {
    padding: 0.75rem;
    text-align: center;
    font-size: 0.8rem;
    color: var(--dts-muted);
}

/* --- Badges sous le select --- */

.deciders-badges {
    display: flex;
    flex-wrap: wrap;
    gap: 0.35rem;
    margin-top: 0.5rem;
}

.deciders-badge {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.25rem 0.35rem 0.25rem 0.6rem;
    background: var(--dts-blue-50);
    color: var(--dts-blue);
    border: 1px solid #cde2f6;
    border-radius: 20px;
    font-size: 0.78rem;
    font-weight: 500;
}

.deciders-badge i.bi-person-badge {
    font-size: 0.8rem;
}

.deciders-badge--inactive {
    background: #fdf3e2;
    color: #9a6409;
    border-color: #f4e2c2;
}

.deciders-badge__remove {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 18px;
    height: 18px;
    padding: 0;
    border: none;
    background: transparent;
    color: currentColor;
    opacity: 0.65;
    border-radius: 50%;
    cursor: pointer;
    transition: background-color 0.15s ease, opacity 0.15s ease;
}

.deciders-badge__remove:hover:not(:disabled) {
    opacity: 1;
    background: rgba(0, 0, 0, 0.08);
}

.deciders-badge__remove:disabled {
    opacity: 0.35;
    cursor: not-allowed;
}

/* --- cases à cocher --- */

.bo-check {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.85rem;
    color: var(--dts-text);
    cursor: pointer;
    user-select: none;
}

.bo-check input {
    width: 16px;
    height: 16px;
    accent-color: var(--dts-blue);
    cursor: pointer;
}

.bo-check--compact {
    font-size: 0.83rem;
}

.badge-support-inline {
    display: inline-block;
    margin-left: 0.35rem;
    padding: 0.05rem 0.4rem;
    font-size: 0.65rem;
    font-weight: 600;
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
    border-radius: 20px;
}

.required {
    color: var(--dts-danger);
    margin-left: 0.15rem;
}

.error-message {
    color: var(--dts-danger);
    font-size: 0.78rem;
}

/* --- actions --- */

.table-action {
    width: 30px;
    height: 30px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 6px;
    border: 1px solid var(--dts-border);
    background: transparent;
    color: var(--dts-text-2);
    cursor: pointer;
    transition: background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.table-action:hover:not(:disabled) {
    background: var(--dts-blue-50);
    color: var(--dts-blue);
    border-color: #bdd3e6;
}

.table-action:disabled {
    opacity: 0.35;
    cursor: not-allowed;
}

.table-action--danger:hover:not(:disabled) {
    background: #fdecea;
    color: var(--dts-danger);
    border-color: #f6cfcc;
}

.bo-form__actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
}

/* ============ Panneau récapitulatif ============ */
.bo-resume {
    position: sticky;
    top: 1.5rem;
    background: var(--dts-surface);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius);
    padding: 1.15rem;
    display: flex;
    flex-direction: column;
    gap: 0.85rem;
}

.bo-resume__head {
    display: flex;
    align-items: center;
    gap: 0.55rem;
    padding-bottom: 0.65rem;
    border-bottom: 1px solid var(--dts-border);
}

.bo-resume__head i {
    color: var(--dts-blue);
    font-size: 1rem;
}

.bo-resume__title {
    margin: 0;
    font-size: 0.85rem;
    font-weight: 700;
    color: var(--dts-navy);
    letter-spacing: 0.02em;
    text-transform: uppercase;
}

.bo-resume__section {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
}

.bo-resume__label {
    font-size: 0.7rem;
    text-transform: uppercase;
    letter-spacing: 0.4px;
    color: var(--dts-muted);
    font-weight: 700;
}

.bo-resume__value {
    font-size: 0.88rem;
    color: var(--dts-text);
    word-break: break-word;
}

.bo-resume__value--strong {
    font-weight: 600;
    color: var(--dts-navy);
}

.bo-resume__desc {
    font-size: 0.78rem;
    color: var(--dts-muted);
    line-height: 1.45;
    margin-top: 0.15rem;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.bo-resume__sep {
    border: none;
    border-top: 1px solid var(--dts-border);
    margin: 0;
}

/* --- Statistiques --- */
.bo-resume__stats {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.6rem;
}

.bo-resume__stat {
    background: var(--dts-bg);
    border: 1px solid var(--dts-border);
    border-radius: var(--dts-radius-sm);
    padding: 0.6rem 0.5rem;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.1rem;
}

.bo-resume__stat-value {
    font-size: 1.15rem;
    font-weight: 700;
    color: var(--dts-navy);
    line-height: 1;
}

.bo-resume__stat-label {
    font-size: 0.68rem;
    color: var(--dts-muted);
    text-transform: uppercase;
    letter-spacing: 0.3px;
    font-weight: 600;
}

/* --- Liste des étapes --- */
.bo-resume__steps {
    list-style: none;
    margin: 0.15rem 0 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 0.55rem;
    max-height: 340px;
    overflow-y: auto;
}

.bo-resume__step {
    padding: 0.55rem 0.65rem;
    border: 1px solid var(--dts-border);
    border-left: 3px solid var(--dts-blue);
    border-radius: 6px;
    background: var(--dts-bg);
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
}

.bo-resume__step--incomplete {
    border-left-color: var(--dts-danger);
    background: #fdf5f5;
}

.bo-resume__step-head {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    min-width: 0;
}

.bo-resume__step-niveau {
    flex-shrink: 0;
    font-size: 0.65rem;
    font-weight: 700;
    background: var(--dts-navy);
    color: #fff;
    padding: 0.1rem 0.4rem;
    border-radius: 4px;
    letter-spacing: 0.03em;
}

.bo-resume__step-designation {
    font-size: 0.8rem;
    font-weight: 600;
    color: var(--dts-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    min-width: 0;
}

.bo-resume__step--incomplete .bo-resume__step-designation {
    color: var(--dts-danger);
}

.bo-resume__step-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 0.3rem;
}

.bo-resume__tag {
    font-size: 0.65rem;
    font-weight: 600;
    padding: 0.1rem 0.4rem;
    border-radius: 20px;
    background: #eef1f5;
    color: #5b6b7d;
    border: 1px solid #dfe5ec;
}

.bo-resume__tag--required {
    background: #e7f1fb;
    color: var(--dts-blue);
    border-color: #cde2f6;
}

.bo-resume__step-decideurs {
    display: flex;
    align-items: flex-start;
    gap: 0.35rem;
    font-size: 0.72rem;
    color: var(--dts-text-2);
    line-height: 1.35;
}

.bo-resume__step-decideurs i {
    color: var(--dts-blue);
    flex-shrink: 0;
    margin-top: 0.05rem;
}

.bo-resume__step-warning {
    display: flex;
    align-items: center;
    gap: 0.3rem;
    font-size: 0.72rem;
    color: var(--dts-danger);
    font-weight: 500;
}

.bo-resume__empty {
    padding: 0.85rem;
    text-align: center;
    font-size: 0.78rem;
    color: var(--dts-muted);
    background: var(--dts-bg);
    border: 1px dashed var(--dts-border-2);
    border-radius: var(--dts-radius-sm);
}

.bo-resume__warning {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.55rem 0.7rem;
    background: #fdf3e2;
    border: 1px solid #f4e2c2;
    border-radius: 6px;
    font-size: 0.75rem;
    color: #9a6409;
    font-weight: 500;
}

.bo-resume__warning i {
    flex-shrink: 0;
}

/* ============ Responsive ============ */
@media (max-width: 992px) {
    .bo-layout {
        grid-template-columns: 1fr;
        max-width: none;
    }

    .bo-resume {
        position: static;
        order: 2;
    }

    .bo-layout__main {
        order: 1;
    }

    .bo-resume__steps {
        max-height: none;
    }
}

@media (max-width: 576px) {
    .bo-page {
        padding: 1rem;
    }

    .bo-form,
    .bo-etapes {
        padding: 1rem;
    }
}
</style>
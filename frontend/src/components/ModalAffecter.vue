<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { BaseButton, BaseModal, BaseConfirm } from '@/components/base'
import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'
import {
    listerUtilisateursDisponibles,
    affecterActivitesBatch,
} from '@/services/affectation'
import type { UtilisateurAffectable } from '@/types/affectation'

/* ==========================================================================
   PROPS / EMITS  (inchangés)
   ========================================================================== */
const props = defineProps<{
    modelValue: boolean
    idActivite: number
    codeActivite: string
    designationActivite: string
}>()

const emit = defineEmits<{
    (e: 'update:modelValue', v: boolean): void
    (e: 'affecte'): void
}>()

const modalValue = computed({
    get: () => props.modelValue,
    set: (value: boolean) => emit('update:modelValue', value),
})

/* ==========================================================================
   RÔLES  (identiques au fichier existant)
   ========================================================================== */

const ROLES = [
    { id: 1, code: 'RESPONSABLE', libelle: 'Responsable' },
    { id: 2, code: 'SUPPLEANT', libelle: 'Suppléant' },
    { id: 3, code: 'PARTICIPANT', libelle: 'Participant' },
] as const

type RoleCode = (typeof ROLES)[number]['code']

const ROLE_META: Record<RoleCode, {
    icon: string
    color: 'primary' | 'warning' | 'neutral'
    description: string
}> = {
    RESPONSABLE: {
        icon: 'bi-person-check-fill',
        color: 'primary',
        description: 'Pilote l’activité — une seule personne',
    },
    SUPPLEANT: {
        icon: 'bi-person-plus',
        color: 'warning',
        description: 'Remplace le responsable en cas d’absence',
    },
    PARTICIPANT: {
        icon: 'bi-people',
        color: 'neutral',
        description: 'Contributeurs multiples',
    },
}

/* ==========================================================================
   ÉTAT  (reorganisé par rôle, pas une liste plate)
   ========================================================================== */

interface SelectionUtilisateur {
    utilisateurId: number
    utilisateurLabel: string
    roleId: number
    roleCode: RoleCode
    roleLabel: string
}

const utilisateurs = ref<UtilisateurAffectable[]>([])
const responsable = ref<SelectionUtilisateur | null>(null)
const suppleant = ref<SelectionUtilisateur | null>(null)
const participants = ref<SelectionUtilisateur[]>([])

/** Rôle en cours d'affectation : ouvre le panneau de recherche ciblé. */
const roleEnCours = ref<RoleCode | null>(null)
const recherche = ref('')

const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')

/* ==========================================================================
   ÉTAT CALCULÉ
   ========================================================================== */

/** Toutes les sélections, dans l'ordre d'envoi (resp, suppl, puis participants). */
const lignes = computed<SelectionUtilisateur[]>(() => {
    const arr: SelectionUtilisateur[] = []
    if (responsable.value) arr.push(responsable.value)
    if (suppleant.value) arr.push(suppleant.value)
    arr.push(...participants.value)
    return arr
})

const nombreAffectations = computed(() => lignes.value.length)

const aUnResponsable = computed(() => responsable.value !== null)

/** Vrai si l'utilisateur est déjà utilisé sur n'importe quel rôle. */
function estDejaAffecte(utilisateurId: number): boolean {
    if (responsable.value?.utilisateurId === utilisateurId) return true
    if (suppleant.value?.utilisateurId === utilisateurId) return true
    return participants.value.some(p => p.utilisateurId === utilisateurId)
}

/** Utilisateurs filtrés par la recherche, hors ceux déjà affectés. */
const utilisateursFiltres = computed(() => {
    const q = recherche.value.trim().toLowerCase()
    return utilisateurs.value
        .filter(u => !estDejaAffecte(u.id))
        .filter(u => {
            if (!q) return true
            const nom = `${u.prenom} ${u.nom}`.toLowerCase()
            return nom.includes(q)
        })
})

/** Un utilisateur peut-il être proposé pour le rôle en cours ? */
function peutEtrePropose(u: UtilisateurAffectable): boolean {
    return !estDejaAffecte(u.id)
}

/* ==========================================================================
   CHARGEMENT  (inchangé)
   ========================================================================== */

watch(() => props.modelValue, async (ouvert) => {
    if (!ouvert) return

    responsable.value = null
    suppleant.value = null
    participants.value = []
    roleEnCours.value = null
    recherche.value = ''
    errorMessage.value = ''

    loading.value = true
    const res = await listerUtilisateursDisponibles(props.idActivite)
    loading.value = false

    if (!res.success) {
        errorMessage.value = res.error
        return
    }
    utilisateurs.value = res.data
}, { immediate: true })

/* ==========================================================================
   INTERACTIONS — ouverture / affectation / retrait
   ========================================================================== */

function ouvrirRecherche(role: RoleCode): void {
    errorMessage.value = ''
    roleEnCours.value = role
    recherche.value = ''
}

function annulerRecherche(): void {
    roleEnCours.value = null
    recherche.value = ''
}

function construireSelection(utilisateur: UtilisateurAffectable, roleCode: RoleCode): SelectionUtilisateur {
    const role = ROLES.find(r => r.code === roleCode)!
    return {
        utilisateurId: utilisateur.id,
        utilisateurLabel: `${utilisateur.prenom} ${utilisateur.nom}`,
        roleId: role.id,
        roleCode: role.code,
        roleLabel: role.libelle,
    }
}

function selectionner(utilisateur: UtilisateurAffectable): void {
    if (!roleEnCours.value) return

    // Garde-fou : déjà affecté sur un autre rôle.
    if (estDejaAffecte(utilisateur.id)) {
        errorMessage.value = `${utilisateur.prenom} ${utilisateur.nom} est déjà affecté à un autre rôle.`
        return
    }

    const selection = construireSelection(utilisateur, roleEnCours.value)

    switch (roleEnCours.value) {
        case 'RESPONSABLE':
            responsable.value = selection
            break
        case 'SUPPLEANT':
            suppleant.value = selection
            break
        case 'PARTICIPANT':
            participants.value.push(selection)
            break
    }

    // Si on vient de choisir un suppléant sans responsable, on prévient
    if (roleEnCours.value === 'SUPPLEANT' && !responsable.value) {
        errorMessage.value = 'Pensez à désigner un responsable pour ce suppléant.'
    }

    annulerRecherche()
}

function retirerResponsable(): void {
    responsable.value = null
    errorMessage.value = ''
}

function retirerSuppleant(): void {
    suppleant.value = null
    errorMessage.value = ''
}

function retirerParticipant(index: number): void {
    participants.value.splice(index, 1)
}

function initiales(label: string): string {
    const parts = label.trim().split(/\s+/)
    if (parts.length === 0) return '?'
    if (parts.length === 1) return parts[0]!.slice(0, 2).toUpperCase()
    return (parts[0]![0]! + parts[parts.length - 1]![0]!).toUpperCase()
}

/* ==========================================================================
   CONFIRMATION  (message enrichi, même logique)
   ========================================================================== */

const confirmOpen = ref(false)
const confirmDemande = ref<ConfirmDemande | null>(null)

function ouvrirConfirmation(): void {
    errorMessage.value = ''

    if (lignes.value.length === 0) {
        errorMessage.value = 'Ajoutez au moins une affectation.'
        return
    }

    if (!responsable.value) {
        errorMessage.value = 'Un responsable est obligatoire pour valider l’affectation.'
        return
    }

    const recap: string[] = []
    if (responsable.value) recap.push(`Responsable : ${responsable.value.utilisateurLabel}`)
    if (suppleant.value) recap.push(`Suppléant : ${suppleant.value.utilisateurLabel}`)
    if (participants.value.length > 0) {
        recap.push(`Participants : ${participants.value.map(p => p.utilisateurLabel).join(', ')}`)
    }

    confirmDemande.value = {
        titre: 'Confirmer les affectations',
        message: `${lignes.value.length} affectation${lignes.value.length > 1 ? 's' : ''} sur l’activité « ${props.codeActivite} ».`,
        consequence: recap.join('\n') + '\n\nToutes les affectations seront enregistrées dans une seule transaction. En cas d’erreur, aucune ne sera appliquée.',
        libelleConfirmer: 'Confirmer',
        libelleAnnuler: 'Retour',
        ton: 'primary',
        icone: 'bi bi-people',
    }

    confirmOpen.value = true
}

async function confirmer(): Promise<void> {
    if (!confirmDemande.value) return

    submitting.value = true
    errorMessage.value = ''

    const res = await affecterActivitesBatch(props.idActivite, {
        affectations: lignes.value.map(l => ({
            utilisateurId: l.utilisateurId,
            roleId: l.roleId,
        })),
    })

    submitting.value = false
    confirmOpen.value = false

    if (!res.success) {
        errorMessage.value = res.error
        return
    }

    emit('affecte')
    fermer()
}

function fermer(): void {
    emit('update:modelValue', false)
}
</script>

<template>
    <BaseModal v-model="modalValue" title="Affecter des utilisateurs" size="lg" :close-on-outside="!submitting">
        <template #header>
            <div class="modal-head">
                <div class="modal-head__main">
                    <h2 class="modal-head__title">Affecter des utilisateurs</h2>
                    <p class="modal-head__subtitle">
                        <code>{{ codeActivite }}</code>
                        <span>{{ designationActivite }}</span>
                    </p>
                </div>
                <span class="modal-head__status" :class="{
                    'modal-head__status--ok': aUnResponsable,
                    'modal-head__status--partial': !aUnResponsable && nombreAffectations > 0,
                    'modal-head__status--empty': nombreAffectations === 0,
                }">
                    <i class="bi"
                        :class="aUnResponsable ? 'bi-check-circle-fill' : (nombreAffectations > 0 ? 'bi-exclamation-circle-fill' : 'bi-circle')"></i>
                    <span v-if="aUnResponsable">Affectation complète</span>
                    <span v-else-if="nombreAffectations > 0">Responsable requis</span>
                    <span v-else>Aucune affectation</span>
                </span>
            </div>
        </template>

        <div class="affectation-body">
            <div v-if="loading" class="loading">
                <span class="spinner"></span>
                Chargement des utilisateurs…
            </div>

            <template v-else>
                <!-- Panneau de recherche ciblé (s'ouvre au clic sur un rôle) -->
                <div v-if="roleEnCours" class="search-panel" @keydown.esc="annulerRecherche">
                    <div class="search-panel__head">
                        <div>
                            <span class="search-panel__label">Affecter un</span>
                            <strong class="search-panel__role">
                                {{ROLES.find(r => r.code === roleEnCours)?.libelle}}
                            </strong>
                        </div>
                        <button type="button" class="search-panel__close" @click="annulerRecherche">
                            <i class="bi bi-x-lg"></i>
                        </button>
                    </div>

                    <div class="search-panel__input">
                        <i class="bi bi-search"></i>
                        <input v-model="recherche" type="text" placeholder="Rechercher par nom ou prénom…" autofocus />
                    </div>

                    <div class="search-panel__results">
                        <button v-for="u in utilisateursFiltres" :key="u.id" type="button" class="user-option"
                            :disabled="!peutEtrePropose(u)" @click="selectionner(u)">
                            <span class="user-option__avatar">{{ initiales(`${u.prenom} ${u.nom}`) }}</span>
                            <span class="user-option__name">{{ u.prenom }} {{ u.nom }}</span>
                            <i class="bi bi-plus-circle user-option__plus"></i>
                        </button>

                        <p v-if="utilisateursFiltres.length === 0" class="search-panel__empty">
                            <i class="bi bi-search"></i>
                            Aucun utilisateur disponible ne correspond.
                        </p>
                    </div>
                </div>

                <!-- Vue d'ensemble des 3 rôles -->
                <div v-else class="roles-view">
                    <!-- Responsable -->
                    <section class="role-card" :class="{ 'role-card--required-missing': !responsable }">
                        <header class="role-card__head">
                            <div class="role-card__badge role-card__badge--primary">
                                <i class="bi bi-person-check-fill"></i>
                            </div>
                            <div class="role-card__title">
                                <h3>Responsable <span class="role-card__req">*</span></h3>
                                <p>{{ ROLE_META.RESPONSABLE.description }}</p>
                            </div>
                        </header>

                        <div class="role-card__body">
                            <div v-if="responsable" class="user-chip">
                                <span class="user-chip__avatar user-chip__avatar--primary">
                                    {{ initiales(responsable.utilisateurLabel) }}
                                </span>
                                <span class="user-chip__name">{{ responsable.utilisateurLabel }}</span>
                                <button type="button" class="user-chip__remove" title="Retirer" :disabled="submitting"
                                    @click="retirerResponsable">
                                    <i class="bi bi-x-lg"></i>
                                </button>
                            </div>
                            <button v-else type="button" class="add-btn" :disabled="submitting"
                                @click="ouvrirRecherche('RESPONSABLE')">
                                <i class="bi bi-plus-lg"></i>
                                Sélectionner un responsable
                            </button>
                        </div>
                    </section>

                    <!-- Suppléant -->
                    <section class="role-card">
                        <header class="role-card__head">
                            <div class="role-card__badge role-card__badge--warning">
                                <i class="bi bi-person-plus"></i>
                            </div>
                            <div class="role-card__title">
                                <h3>Suppléant</h3>
                                <p>{{ ROLE_META.SUPPLEANT.description }}</p>
                            </div>
                        </header>

                        <div class="role-card__body">
                            <div v-if="suppleant" class="user-chip">
                                <span class="user-chip__avatar user-chip__avatar--warning">
                                    {{ initiales(suppleant.utilisateurLabel) }}
                                </span>
                                <span class="user-chip__name">{{ suppleant.utilisateurLabel }}</span>
                                <button type="button" class="user-chip__remove" title="Retirer" :disabled="submitting"
                                    @click="retirerSuppleant">
                                    <i class="bi bi-x-lg"></i>
                                </button>
                            </div>
                            <button v-else type="button" class="add-btn add-btn--muted" :disabled="submitting"
                                @click="ouvrirRecherche('SUPPLEANT')">
                                <i class="bi bi-plus-lg"></i>
                                Sélectionner un suppléant
                            </button>
                        </div>
                    </section>

                    <!-- Participants -->
                    <section class="role-card">
                        <header class="role-card__head">
                            <div class="role-card__badge role-card__badge--neutral">
                                <i class="bi bi-people"></i>
                            </div>
                            <div class="role-card__title">
                                <h3>
                                    Participants
                                    <span v-if="participants.length > 0" class="role-card__count">
                                        {{ participants.length }}
                                    </span>
                                </h3>
                                <p>{{ ROLE_META.PARTICIPANT.description }}</p>
                            </div>
                        </header>

                        <div class="role-card__body">
                            <ul v-if="participants.length > 0" class="user-chips">
                                <li v-for="(p, i) in participants" :key="`${p.utilisateurId}-${i}`" class="user-chip">
                                    <span class="user-chip__avatar user-chip__avatar--neutral">
                                        {{ initiales(p.utilisateurLabel) }}
                                    </span>
                                    <span class="user-chip__name">{{ p.utilisateurLabel }}</span>
                                    <button type="button" class="user-chip__remove" title="Retirer"
                                        :disabled="submitting" @click="retirerParticipant(i)">
                                        <i class="bi bi-x-lg"></i>
                                    </button>
                                </li>
                            </ul>

                            <button type="button" class="add-btn add-btn--muted" :disabled="submitting"
                                @click="ouvrirRecherche('PARTICIPANT')">
                                <i class="bi bi-plus-lg"></i>
                                {{ participants.length > 0 ? 'Ajouter un participant' : 'Ajouter des participants' }}
                            </button>
                        </div>
                    </section>
                </div>

                <!-- Message d'erreur -->
                <div v-if="errorMessage" class="error" role="alert">
                    <i class="bi bi-exclamation-triangle-fill"></i>
                    {{ errorMessage }}
                </div>

                <!-- Résumé compact quand l'affectation est prête -->
                <div v-if="aUnResponsable && nombreAffectations > 0" class="recap-strip">
                    <i class="bi bi-info-circle"></i>
                    <span>
                        <strong>{{ nombreAffectations }}</strong>
                        affectation{{ nombreAffectations > 1 ? 's' : '' }} prête{{ nombreAffectations > 1 ? 's' : '' }}
                        à être enregistrée{{ nombreAffectations > 1 ? 's' : '' }}.
                    </span>
                </div>
            </template>
        </div>

        <template #footer>
            <BaseButton variant="secondary" :disabled="submitting" @click="fermer">
                Annuler
            </BaseButton>
            <BaseButton variant="primary" icon="bi bi-check-lg"
                :disabled="nombreAffectations === 0 || submitting || loading || !aUnResponsable"
                @click="ouvrirConfirmation">
                {{ nombreAffectations === 0 ? 'Aucune affectation' : `Confirmer les ${nombreAffectations}
                affectation${nombreAffectations > 1 ? 's' : ''}` }}
            </BaseButton>
        </template>
    </BaseModal>

    <BaseConfirm v-model="confirmOpen" :demande="confirmDemande" :loading="submitting" @confirme="confirmer"
        @annule="confirmOpen = false" />
</template>

<style scoped>
/* ==========================================================================
   EN-TÊTE
   ========================================================================== */
.modal-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 1rem;
    width: 100%;
    flex-wrap: wrap;
}

.modal-head__main {
    min-width: 0;
    flex: 1;
}

.modal-head__title {
    margin: 0 0 0.25rem;
    font: 800 1.05rem Manrope, "DM Sans", sans-serif;
    color: #1a2a6c;
    letter-spacing: -0.2px;
}

.modal-head__subtitle {
    margin: 0;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex-wrap: wrap;
    font-size: 0.8rem;
    color: #6b7280;
}

.modal-head__subtitle code {
    font-family: ui-monospace, Menlo, monospace;
    background: #eef1f5;
    padding: 0.1rem 0.4rem;
    border-radius: 4px;
    font-size: 0.72rem;
    font-weight: 700;
    color: #1a2a6c;
}

.modal-head__status {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.35rem 0.75rem;
    border-radius: 20px;
    font-size: 0.72rem;
    font-weight: 700;
    white-space: nowrap;
    flex: none;
}

.modal-head__status--ok {
    background: #e4f7f0;
    color: #138a68;
}

.modal-head__status--partial {
    background: #fff6dd;
    color: #b8860b;
}

.modal-head__status--empty {
    background: #f1f5f9;
    color: #6b7280;
}

/* ==========================================================================
   CORPS
   ========================================================================== */
.affectation-body {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

/* ==========================================================================
   CARTES DE RÔLE
   ========================================================================== */
.roles-view {
    display: flex;
    flex-direction: column;
    gap: 0.85rem;
}

.role-card {
    padding: 1rem 1.15rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 12px;
    transition: border-color .15s, box-shadow .15s;
}

.role-card:hover {
    border-color: #d4ddea;
}

.role-card--required-missing {
    border-color: #f4d3d3;
    background: #fefafa;
}

.role-card__head {
    display: flex;
    align-items: flex-start;
    gap: 0.75rem;
    margin-bottom: 0.75rem;
}

.role-card__badge {
    display: grid;
    place-items: center;
    width: 36px;
    height: 36px;
    border-radius: 10px;
    font-size: 1rem;
    flex: none;
}

.role-card__badge--primary {
    background: #eaf0ff;
    color: #3b63d9;
}

.role-card__badge--warning {
    background: #fff6dd;
    color: #b8860b;
}

.role-card__badge--neutral {
    background: #f1f5f9;
    color: #5b6b7d;
}

.role-card__title {
    min-width: 0;
    flex: 1;
}

.role-card__title h3 {
    margin: 0;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.92rem;
    font-weight: 700;
    color: #1a2a6c;
    letter-spacing: -0.1px;
}

.role-card__req {
    color: #d84e5b;
    font-weight: 700;
}

.role-card__count {
    padding: 0.05rem 0.45rem;
    border-radius: 999px;
    background: #eaf0ff;
    color: #3b63d9;
    font-size: 0.7rem;
    font-weight: 700;
}

.role-card__title p {
    margin: 0.15rem 0 0;
    font-size: 0.74rem;
    color: #8a95a8;
    line-height: 1.4;
}

.role-card__body {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

/* ==========================================================================
   CHIP UTILISATEUR
   ========================================================================== */
.user-chip {
    display: inline-flex;
    align-items: center;
    gap: 0.6rem;
    padding: 0.4rem 0.65rem 0.4rem 0.45rem;
    background: #f8fafc;
    border: 1px solid #e6ebf3;
    border-radius: 20px;
    font-size: 0.84rem;
    max-width: 100%;
}

.user-chip__avatar {
    display: grid;
    place-items: center;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    font-size: 0.7rem;
    font-weight: 800;
    color: #fff;
    flex: none;
    letter-spacing: 0.02em;
}

.user-chip__avatar--primary {
    background: linear-gradient(135deg, #4a6cf7 0%, #2a3f94 100%);
}

.user-chip__avatar--warning {
    background: linear-gradient(135deg, #f4b91f 0%, #b8860b 100%);
}

.user-chip__avatar--neutral {
    background: linear-gradient(135deg, #8a95a8 0%, #5b6b7d 100%);
}

.user-chip__name {
    font-weight: 600;
    color: #15233c;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.user-chip__remove {
    display: grid;
    place-items: center;
    width: 22px;
    height: 22px;
    padding: 0;
    border: none;
    border-radius: 50%;
    background: transparent;
    color: #8a95a8;
    font-size: 0.7rem;
    cursor: pointer;
    transition: background .15s, color .15s;
    flex: none;
}

.user-chip__remove:hover:not(:disabled) {
    background: #fde9e9;
    color: #b03030;
}

.user-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 0.4rem;
    list-style: none;
    margin: 0;
    padding: 0;
}

/* ==========================================================================
   BOUTON AJOUTER
   ========================================================================== */
.add-btn {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.55rem 0.85rem;
    border: 1.5px dashed #4a6cf7;
    border-radius: 10px;
    background: #f8faff;
    color: #3b63d9;
    font-size: 0.82rem;
    font-weight: 600;
    cursor: pointer;
    transition: background .15s, border-color .15s;
    align-self: flex-start;
}

.add-btn:hover:not(:disabled) {
    background: #eaf0ff;
    border-color: #2a3f94;
    border-style: solid;
}

.add-btn--muted {
    border-color: #cbd5e1;
    background: #f8fafc;
    color: #6b7280;
}

.add-btn--muted:hover:not(:disabled) {
    background: #f1f5f9;
    border-color: #8a95a8;
    border-style: solid;
    color: #1a2a6c;
}

.add-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

/* ==========================================================================
   PANNEAU DE RECHERCHE
   ========================================================================== */
.search-panel {
    padding: 1rem 1.15rem;
    background: #f8faff;
    border: 1px solid #d4ddea;
    border-radius: 12px;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.search-panel__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 0.5rem;
}

.search-panel__label {
    font-size: 0.72rem;
    font-weight: 600;
    color: #8a95a8;
    text-transform: uppercase;
    letter-spacing: 0.05em;
}

.search-panel__role {
    display: block;
    margin-top: 0.15rem;
    font-size: 0.92rem;
    font-weight: 700;
    color: #1a2a6c;
}

.search-panel__close {
    display: grid;
    place-items: center;
    width: 28px;
    height: 28px;
    border: 1px solid #e6ebf3;
    border-radius: 6px;
    background: #fff;
    color: #6b7280;
    cursor: pointer;
    transition: background .15s, color .15s;
    font-size: 0.75rem;
}

.search-panel__close:hover {
    background: #fde9e9;
    color: #b03030;
    border-color: #f4d3d3;
}

.search-panel__input {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 0.75rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    transition: border-color .15s, box-shadow .15s;
}

.search-panel__input:focus-within {
    border-color: #4a6cf7;
    box-shadow: 0 0 0 3px rgba(74, 108, 247, .12);
}

.search-panel__input i {
    color: #8a95a8;
    font-size: 0.85rem;
}

.search-panel__input input {
    flex: 1;
    border: none;
    outline: none;
    background: transparent;
    font-family: inherit;
    font-size: 0.85rem;
    color: #15233c;
}

.search-panel__input input::placeholder {
    color: #aeb8c6;
}

.search-panel__results {
    max-height: 260px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
}

.search-panel__empty {
    margin: 0.5rem 0;
    padding: 1rem;
    text-align: center;
    font-size: 0.8rem;
    color: #8a95a8;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.4rem;
}

.user-option {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    width: 100%;
    padding: 0.5rem 0.7rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    text-align: left;
    cursor: pointer;
    transition: background .12s, border-color .12s;
    font-family: inherit;
}

.user-option:hover:not(:disabled) {
    background: #eaf0ff;
    border-color: #4a6cf7;
}

.user-option:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.user-option__avatar {
    display: grid;
    place-items: center;
    width: 30px;
    height: 30px;
    border-radius: 50%;
    background: linear-gradient(135deg, #4a6cf7 0%, #2a3f94 100%);
    color: #fff;
    font-size: 0.7rem;
    font-weight: 800;
    flex: none;
}

.user-option__name {
    flex: 1;
    font-size: 0.85rem;
    font-weight: 600;
    color: #15233c;
}

.user-option__plus {
    color: #4a6cf7;
    font-size: 1rem;
    flex: none;
}

/* ==========================================================================
   STRIP DE RÉCAP
   ========================================================================== */
.recap-strip {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.6rem 0.85rem;
    background: #f8faff;
    border-left: 3px solid #4a6cf7;
    border-radius: 6px;
    font-size: 0.82rem;
    color: #40516b;
}

.recap-strip i {
    color: #4a6cf7;
    flex: none;
}

.recap-strip strong {
    color: #1a2a6c;
    font-weight: 700;
}

/* ==========================================================================
   ERREUR / LOADING
   ========================================================================== */
.error {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.6rem 0.85rem;
    background: #fde9e9;
    color: #b03030;
    border-radius: 8px;
    font-size: 0.82rem;
    border-left: 3px solid #d84e5b;
}

.loading {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    padding: 1.5rem;
    justify-content: center;
    color: #6b7280;
    font-size: 0.86rem;
}

.spinner {
    width: 16px;
    height: 16px;
    border: 2px solid #e6ebf3;
    border-top-color: #4a6cf7;
    border-radius: 50%;
    animation: spin 0.7s linear infinite;
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
}

/* ==========================================================================
   RESPONSIVE
   ========================================================================== */
@media (max-width: 575.98px) {
    .modal-head {
        flex-direction: column;
        align-items: flex-start;
    }

    .modal-head__status {
        align-self: flex-start;
    }

    .role-card {
        padding: 0.85rem 0.95rem;
    }

    .user-chip {
        max-width: 100%;
    }

    .search-panel {
        padding: 0.85rem 0.95rem;
    }
}
</style>
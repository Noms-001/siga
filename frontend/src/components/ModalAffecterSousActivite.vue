<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { BaseButton, BaseModal, BaseConfirm } from '@/components/base'
import type { ConfirmDemande } from '@/components/base/BaseConfirm/BaseConfirm.types'
import {
    listerUtilisateursDisponibles,
    listerAffectationsSousActivite,
    affecterSousActivitesBatch,
    desaffecterSousActivite,
} from '@/services/affectation'
import type { AffectationActivite, UtilisateurAffectable } from '@/types/affectation'

/* ==========================================================================
   PROPS / EMITS
   ========================================================================== */
const props = defineProps<{
    modelValue: boolean
    idActivite: number
    idSousActivite: number
    codeSousActivite: string
    designationSousActivite: string
}>()

const emit = defineEmits<{
    (e: 'update:modelValue', v: boolean): void
    (e: 'affecte'): void
}>()

const modalValue = computed({
    get: () => props.modelValue,
    set: (v: boolean) => emit('update:modelValue', v),
})

/* ==========================================================================
   RÔLES
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
   ÉTAT
   ========================================================================== */
interface Ligne {
    utilisateurId: number
    utilisateurLabel: string
    roleId: number
    roleCode: RoleCode
    roleLabel: string
}

const utilisateurs = ref<UtilisateurAffectable[]>([])
const affectations = ref<AffectationActivite[]>([])
const lignes = ref<Ligne[]>([])

const roleEnCours = ref<RoleCode | null>(null)
const recherche = ref('')

const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')

/* ==========================================================================
   ÉTAT CALCULÉ
   ========================================================================== */
const affectationsActives = computed(() => affectations.value.filter(a => a.active))
const nombreNouvelles = computed(() => lignes.value.length)

const statutHeader = computed(() => {
    if (nombreNouvelles.value > 0) {
        return {
            modifier: 'partial' as const,
            icon: 'bi-exclamation-circle-fill',
            label: `${nombreNouvelles.value} à confirmer`,
        }
    }
    if (affectationsActives.value.length > 0) {
        return {
            modifier: 'ok' as const,
            icon: 'bi-check-circle-fill',
            label: 'À jour',
        }
    }
    return {
        modifier: 'empty' as const,
        icon: 'bi-circle',
        label: 'Aucune affectation',
    }
})

function affectationsExistantes(code: RoleCode): AffectationActivite[] {
    return affectationsActives.value.filter(a => a.role?.code === code)
}

function lignesEnCours(code: RoleCode): { ligne: Ligne; index: number }[] {
    return lignes.value
        .map((ligne, index) => ({ ligne, index }))
        .filter(x => x.ligne.roleCode === code)
}

function totalPourRole(code: RoleCode): number {
    return affectationsExistantes(code).length + lignesEnCours(code).length
}

function roleEstDisponible(code: RoleCode): boolean {
    if (code === 'PARTICIPANT') return true
    if (affectationsActives.value.some(a => a.role?.code === code)) return false
    if (lignes.value.some(l => l.roleCode === code)) return false
    return true
}

function estDejaAffecte(user: UtilisateurAffectable): boolean {
    if (lignes.value.some(l => l.utilisateurId === user.id)) return true
    const label = `${user.prenom} ${user.nom}`
    return affectationsActives.value.some(a => a.utilisateur?.libelle === label)
}

const utilisateursFiltres = computed(() => {
    const q = recherche.value.trim().toLowerCase()
    return utilisateurs.value
        .filter(u => !estDejaAffecte(u))
        .filter(u => {
            if (!q) return true
            return `${u.prenom} ${u.nom}`.toLowerCase().includes(q)
        })
})

/* ==========================================================================
   CHARGEMENT
   ========================================================================== */
watch(() => props.modelValue, async (ouvert) => {
    if (!ouvert) return

    utilisateurs.value = []
    affectations.value = []
    lignes.value = []
    roleEnCours.value = null
    recherche.value = ''
    errorMessage.value = ''

    loading.value = true
    const [resUsers, resAff] = await Promise.all([
        listerUtilisateursDisponibles(props.idActivite),
        listerAffectationsSousActivite(props.idActivite, props.idSousActivite),
    ])
    loading.value = false

    if (!resUsers.success) {
        errorMessage.value = resUsers.error
        return
    }
    utilisateurs.value = resUsers.data
    if (resAff.success) affectations.value = resAff.data
}, { immediate: true })

/* ==========================================================================
   INTERACTIONS
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

function initiales(label: string): string {
    const parts = label.trim().split(/\s+/)
    if (parts.length === 0) return '?'
    if (parts.length === 1) return parts[0]!.slice(0, 2).toUpperCase()
    return (parts[0]![0]! + parts[parts.length - 1]![0]!).toUpperCase()
}

function selectionner(user: UtilisateurAffectable): void {
    if (!roleEnCours.value) return

    if (estDejaAffecte(user)) {
        errorMessage.value = `${user.prenom} ${user.nom} est déjà affecté à cette sous-activité.`
        return
    }

    if (!roleEstDisponible(roleEnCours.value)) {
        const libelle = ROLES.find(r => r.code === roleEnCours.value)?.libelle ?? 'rôle'
        errorMessage.value = `Un « ${libelle} » est déjà affecté. Retirez-le avant d’en ajouter un autre.`
        return
    }

    const role = ROLES.find(r => r.code === roleEnCours.value)!
    lignes.value.push({
        utilisateurId: user.id,
        utilisateurLabel: `${user.prenom} ${user.nom}`,
        roleId: role.id,
        roleCode: role.code,
        roleLabel: role.libelle,
    })

    annulerRecherche()
}

function retirerLigne(index: number): void {
    lignes.value.splice(index, 1)
    errorMessage.value = ''
}

async function desaffecter(a: AffectationActivite): Promise<void> {
    if (!confirm(`Retirer ${a.utilisateur?.libelle} de cette sous-activité ?`)) return

    submitting.value = true
    errorMessage.value = ''

    const res = await desaffecterSousActivite(
        props.idActivite, props.idSousActivite, a.id)
    submitting.value = false

    if (!res.success) {
        errorMessage.value = res.error
        return
    }

    const re = await listerAffectationsSousActivite(props.idActivite, props.idSousActivite)
    if (re.success) affectations.value = re.data
    emit('affecte')
}

/* ==========================================================================
   CONFIRMATION
   ========================================================================== */
const confirmOpen = ref(false)
const confirmDemande = ref<ConfirmDemande | null>(null)

function ouvrirConfirmation(): void {
    errorMessage.value = ''

    if (lignes.value.length === 0) {
        errorMessage.value = 'Ajoutez au moins une affectation.'
        return
    }

    const recap = lignes.value
        .map(l => `• ${l.utilisateurLabel} — ${l.roleLabel}`)
        .join('\n')

    confirmDemande.value = {
        titre: 'Confirmer les affectations',
        message: `${lignes.value.length} affectation${lignes.value.length > 1 ? 's' : ''} sur la sous-activité « ${props.codeSousActivite} ».`,
        consequence: recap + '\n\nToutes les affectations seront enregistrées dans une seule transaction.',
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

    const res = await affecterSousActivitesBatch(
        props.idActivite,
        props.idSousActivite,
        { affectations: lignes.value.map(l => ({ utilisateurId: l.utilisateurId, roleId: l.roleId })) },
    )

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
    <BaseModal v-model="modalValue" title="Affecter à la sous-activité" size="lg" :close-on-outside="!submitting">
        <template #header>
            <div class="modal-head">
                <div class="modal-head__main">
                    <h2 class="modal-head__title">Affecter à la sous-activité</h2>
                    <p class="modal-head__subtitle">
                        <code>{{ codeSousActivite }}</code>
                        <span>{{ designationSousActivite }}</span>
                    </p>
                </div>
                <span class="modal-head__status" :class="`modal-head__status--${statutHeader.modifier}`">
                    <i class="bi" :class="statutHeader.icon"></i>
                    <span>{{ statutHeader.label }}</span>
                </span>
            </div>
        </template>

        <div class="affectation-body">
            <div v-if="loading" class="loading">
                <span class="spinner"></span>
                Chargement des affectations…
            </div>

            <template v-else>
                <!-- Panneau de recherche ciblé -->
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
                            @click="selectionner(u)">
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
                    <section class="role-card">
                        <header class="role-card__head">
                            <div class="role-card__badge role-card__badge--primary">
                                <i :class="ROLE_META.RESPONSABLE.icon"></i>
                            </div>
                            <div class="role-card__title">
                                <h3>Responsable</h3>
                                <p>{{ ROLE_META.RESPONSABLE.description }}</p>
                            </div>
                        </header>

                        <div class="role-card__body">
                            <div v-for="a in affectationsExistantes('RESPONSABLE')" :key="`ex-${a.id}`"
                                class="user-chip">
                                <span class="user-chip__avatar user-chip__avatar--primary">
                                    {{ initiales(a.utilisateur?.libelle ?? '') }}
                                </span>
                                <span class="user-chip__name">{{ a.utilisateur?.libelle }}</span>
                                <span class="user-chip__tag user-chip__tag--existing">actuel</span>
                                <button type="button" class="user-chip__remove" title="Retirer" :disabled="submitting"
                                    @click="desaffecter(a)">
                                    <i class="bi bi-x-lg"></i>
                                </button>
                            </div>

                            <div v-for="{ ligne, index } in lignesEnCours('RESPONSABLE')" :key="`new-${index}`"
                                class="user-chip user-chip--new">
                                <span class="user-chip__avatar user-chip__avatar--primary">
                                    {{ initiales(ligne.utilisateurLabel) }}
                                </span>
                                <span class="user-chip__name">{{ ligne.utilisateurLabel }}</span>
                                <span class="user-chip__tag user-chip__tag--new">nouveau</span>
                                <button type="button" class="user-chip__remove" title="Retirer" :disabled="submitting"
                                    @click="retirerLigne(index)">
                                    <i class="bi bi-x-lg"></i>
                                </button>
                            </div>

                            <button v-if="roleEstDisponible('RESPONSABLE')" type="button" class="add-btn"
                                :disabled="submitting" @click="ouvrirRecherche('RESPONSABLE')">
                                <i class="bi bi-plus-lg"></i>
                                Sélectionner un responsable
                            </button>
                        </div>
                    </section>

                    <!-- Suppléant -->
                    <section class="role-card">
                        <header class="role-card__head">
                            <div class="role-card__badge role-card__badge--warning">
                                <i :class="ROLE_META.SUPPLEANT.icon"></i>
                            </div>
                            <div class="role-card__title">
                                <h3>Suppléant</h3>
                                <p>{{ ROLE_META.SUPPLEANT.description }}</p>
                            </div>
                        </header>

                        <div class="role-card__body">
                            <div v-for="a in affectationsExistantes('SUPPLEANT')" :key="`ex-${a.id}`" class="user-chip">
                                <span class="user-chip__avatar user-chip__avatar--warning">
                                    {{ initiales(a.utilisateur?.libelle ?? '') }}
                                </span>
                                <span class="user-chip__name">{{ a.utilisateur?.libelle }}</span>
                                <span class="user-chip__tag user-chip__tag--existing">actuel</span>
                                <button type="button" class="user-chip__remove" title="Retirer" :disabled="submitting"
                                    @click="desaffecter(a)">
                                    <i class="bi bi-x-lg"></i>
                                </button>
                            </div>

                            <div v-for="{ ligne, index } in lignesEnCours('SUPPLEANT')" :key="`new-${index}`"
                                class="user-chip user-chip--new">
                                <span class="user-chip__avatar user-chip__avatar--warning">
                                    {{ initiales(ligne.utilisateurLabel) }}
                                </span>
                                <span class="user-chip__name">{{ ligne.utilisateurLabel }}</span>
                                <span class="user-chip__tag user-chip__tag--new">nouveau</span>
                                <button type="button" class="user-chip__remove" title="Retirer" :disabled="submitting"
                                    @click="retirerLigne(index)">
                                    <i class="bi bi-x-lg"></i>
                                </button>
                            </div>

                            <button v-if="roleEstDisponible('SUPPLEANT')" type="button" class="add-btn add-btn--muted"
                                :disabled="submitting" @click="ouvrirRecherche('SUPPLEANT')">
                                <i class="bi bi-plus-lg"></i>
                                Sélectionner un suppléant
                            </button>
                        </div>
                    </section>

                    <!-- Participants -->
                    <section class="role-card">
                        <header class="role-card__head">
                            <div class="role-card__badge role-card__badge--neutral">
                                <i :class="ROLE_META.PARTICIPANT.icon"></i>
                            </div>
                            <div class="role-card__title">
                                <h3>
                                    Participants
                                    <span v-if="totalPourRole('PARTICIPANT') > 0" class="role-card__count">
                                        {{ totalPourRole('PARTICIPANT') }}
                                    </span>
                                </h3>
                                <p>{{ ROLE_META.PARTICIPANT.description }}</p>
                            </div>
                        </header>

                        <div class="role-card__body">
                            <div v-if="totalPourRole('PARTICIPANT') > 0" class="user-chips">
                                <div v-for="a in affectationsExistantes('PARTICIPANT')" :key="`ex-${a.id}`"
                                    class="user-chip">
                                    <span class="user-chip__avatar user-chip__avatar--neutral">
                                        {{ initiales(a.utilisateur?.libelle ?? '') }}
                                    </span>
                                    <span class="user-chip__name">{{ a.utilisateur?.libelle }}</span>
                                    <span class="user-chip__tag user-chip__tag--existing">actuel</span>
                                    <button type="button" class="user-chip__remove" title="Retirer"
                                        :disabled="submitting" @click="desaffecter(a)">
                                        <i class="bi bi-x-lg"></i>
                                    </button>
                                </div>

                                <div v-for="{ ligne, index } in lignesEnCours('PARTICIPANT')" :key="`new-${index}`"
                                    class="user-chip user-chip--new">
                                    <span class="user-chip__avatar user-chip__avatar--neutral">
                                        {{ initiales(ligne.utilisateurLabel) }}
                                    </span>
                                    <span class="user-chip__name">{{ ligne.utilisateurLabel }}</span>
                                    <span class="user-chip__tag user-chip__tag--new">nouveau</span>
                                    <button type="button" class="user-chip__remove" title="Retirer"
                                        :disabled="submitting" @click="retirerLigne(index)">
                                        <i class="bi bi-x-lg"></i>
                                    </button>
                                </div>
                            </div>

                            <button type="button" class="add-btn add-btn--muted" :disabled="submitting"
                                @click="ouvrirRecherche('PARTICIPANT')">
                                <i class="bi bi-plus-lg"></i>
                                {{ totalPourRole('PARTICIPANT') > 0 ? 'Ajouter un participant' : 'Ajouter des participants' }}
                            </button>
                        </div>
                    </section>
                </div>

                <!-- Message d'erreur -->
                <div v-if="errorMessage" class="error" role="alert">
                    <i class="bi bi-exclamation-triangle-fill"></i>
                    {{ errorMessage }}
                </div>

                <!-- Résumé compact -->
                <div v-if="nombreNouvelles > 0" class="recap-strip">
                    <i class="bi bi-info-circle"></i>
                    <span>
                        <strong>{{ nombreNouvelles }}</strong>
                        affectation{{ nombreNouvelles > 1 ? 's' : '' }} prête{{ nombreNouvelles > 1 ? 's' : '' }}
                        à être enregistrée{{ nombreNouvelles > 1 ? 's' : '' }}.
                    </span>
                </div>
            </template>
        </div>

        <template #footer>
            <BaseButton variant="secondary" :disabled="submitting" @click="fermer">
                Annuler
            </BaseButton>
            <BaseButton variant="primary" icon="bi bi-check-lg"
                :disabled="nombreNouvelles === 0 || submitting || loading" @click="ouvrirConfirmation">
                {{ nombreNouvelles === 0 ? 'Aucune affectation' : `Confirmer les ${nombreNouvelles}
                affectation${nombreNouvelles > 1 ? 's' : ''}` }}
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
.user-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 0.4rem;
    list-style: none;
    margin: 0;
    padding: 0;
}

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

.user-chip--new {
    background: #f0f7ff;
    border-color: #cde2f6;
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

.user-chip__tag {
    padding: 0.1rem 0.4rem;
    border-radius: 999px;
    font-size: 0.6rem;
    font-weight: 800;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    flex: none;
    line-height: 1.2;
}

.user-chip__tag--existing {
    background: #eef1f5;
    color: #5b6b7d;
}

.user-chip__tag--new {
    background: #eaf0ff;
    color: #3b63d9;
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
    font-family: inherit;
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
<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { rechercherUtilisateurs } from '@/services/utilisateur'
import type { UtilisateurAffectableItem } from '@/types/planaction'

const props = defineProps<{
    modelValue: number | null
    disabled?: boolean
}>()

const emit = defineEmits<{
    (e: 'update:modelValue', value: number | null): void
}>()

const recherche = ref('')
const resultats = ref<UtilisateurAffectableItem[]>([])
const selectionne = ref<UtilisateurAffectableItem | null>(null)
const loading = ref(false)
const ouvert = ref(false)

let timer: ReturnType<typeof setTimeout> | null = null

async function charger(): Promise<void> {
    loading.value = true
    const res = await rechercherUtilisateurs(recherche.value.trim() || undefined)
    loading.value = false
    if (res.success) resultats.value = res.data
}

function surSaisie(): void {
    if (timer) clearTimeout(timer)
    timer = setTimeout(charger, 250)
}

function choisir(u: UtilisateurAffectableItem): void {
    selectionne.value = u
    emit('update:modelValue', u.id)
    ouvert.value = false
    recherche.value = ''
}

function retirer(): void {
    selectionne.value = null
    emit('update:modelValue', null)
}

function initiales(nom: string, prenom: string): string {
    return (prenom[0] ?? '') + (nom[0] ?? '')
}

watch(recherche, surSaisie)
onMounted(() => {
    if (props.modelValue) charger()
})
</script>

<template>
    <div class="utilisateur-select">
        <label class="select-label">
            Responsable <span class="required">*</span>
        </label>

        <!-- Sélection courante -->
        <div v-if="selectionne" class="selection">
            <div class="avatar">{{ initiales(selectionne.nom, selectionne.prenom) }}</div>
            <div class="selection__info">
                <span class="selection__nom">{{ selectionne.prenom }} {{ selectionne.nom }}</span>
            </div>
            <button type="button" class="selection__remove" @click="retirer" :disabled="disabled">
                <i class="bi bi-x-lg"></i>
            </button>
        </div>

        <!-- Recherche -->
        <template v-else>
            <div class="search-wrap">
                <i class="bi bi-search search-wrap__icon"></i>
                <input v-model="recherche" type="text" class="search-wrap__input"
                    placeholder="Rechercher par nom ou prénom…" :disabled="disabled" @focus="ouvert = true" />
                <span v-if="loading" class="search-wrap__loader">
                    <span class="spinner"></span>
                </span>
            </div>

            <div v-if="ouvert && resultats.length > 0" class="results">
                <button v-for="u in resultats" :key="u.id" type="button" class="result-item" @click="choisir(u)">
                    <div class="avatar avatar--sm">{{ initiales(u.nom, u.prenom) }}</div>
                    <span class="result-item__label">{{ u.prenom }} {{ u.nom }}</span>
                </button>
            </div>
            <div v-else-if="ouvert && !loading && resultats.length === 0" class="empty">
                Aucun utilisateur ne correspond.
            </div>
        </template>
    </div>
</template>

<style scoped>
.utilisateur-select {
    display: flex;
    flex-direction: column;
    gap: 0.55rem;
}

.select-label {
    font-size: 0.78rem;
    font-weight: 600;
    color: #15233c;
}

.required {
    color: #d64545;
}

.selection {
    display: flex;
    align-items: center;
    gap: 0.65rem;
    padding: 0.6rem 0.75rem;
    background: #f8fafc;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
}

.selection__info {
    flex: 1;
    min-width: 0;
}

.selection__nom {
    font-size: 0.86rem;
    font-weight: 600;
    color: #15233c;
}

.selection__remove {
    background: none;
    border: none;
    color: #8a95a8;
    cursor: pointer;
    padding: 0.25rem;
    border-radius: 4px;
    font-size: 0.7rem;
}

.selection__remove:hover {
    background: #fde9e9;
    color: #d64545;
}

.avatar {
    flex: none;
    display: grid;
    place-items: center;
    width: 34px;
    height: 34px;
    border-radius: 50%;
    background: linear-gradient(135deg, #4a6cf7, #6b8dff);
    color: #fff;
    font-size: 0.72rem;
    font-weight: 800;
    letter-spacing: 0.02em;
}

.avatar--sm {
    width: 28px;
    height: 28px;
    font-size: 0.65rem;
}

.search-wrap {
    position: relative;
    display: flex;
    align-items: center;
}

.search-wrap__icon {
    position: absolute;
    left: 0.75rem;
    color: #8a95a8;
    pointer-events: none;
    font-size: 0.85rem;
}

.search-wrap__input {
    width: 100%;
    padding: 0.55rem 2.5rem 0.55rem 2.15rem;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    font-size: 0.85rem;
    color: #15233c;
    font-family: inherit;
}

.search-wrap__input:focus {
    outline: none;
    border-color: #4a6cf7;
    box-shadow: 0 0 0 3px rgba(74, 108, 247, .1);
}

.search-wrap__loader {
    position: absolute;
    right: 0.75rem;
    display: grid;
    place-items: center;
}

.spinner {
    display: inline-block;
    width: 14px;
    height: 14px;
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

.results {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    max-height: 260px;
    overflow-y: auto;
    padding: 0.35rem;
    background: #fff;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
}

.result-item {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    padding: 0.5rem 0.65rem;
    background: none;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    text-align: left;
    font-size: 0.85rem;
    color: #15233c;
    transition: background .12s;
}

.result-item:hover {
    background: #f8fafc;
}

.result-item__label {
    font-weight: 600;
}

.empty {
    padding: 0.75rem;
    text-align: center;
    color: #8a95a8;
    font-size: 0.82rem;
    background: #f8fafc;
    border-radius: 8px;
}
</style>
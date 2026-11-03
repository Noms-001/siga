<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { rechercherOrigines } from '@/services/origine'
import type { OrigineListeItem } from '@/types/planaction'

const props = defineProps<{
    modelValue: number[]
    disabled?: boolean
}>()

const emit = defineEmits<{
    (e: 'update:modelValue', value: number[]): void
}>()

const recherche = ref('')
const resultats = ref<OrigineListeItem[]>([])
const selectionnees = ref<OrigineListeItem[]>([])
const loading = ref(false)
const ouvert = ref(false)

let timer: ReturnType<typeof setTimeout> | null = null

async function charger(): Promise<void> {
    loading.value = true
    const res = await rechercherOrigines(recherche.value.trim() || undefined)
    loading.value = false
    console.log(res.data)
    if (res.success) {
        // Exclut les déjà sélectionnées
        resultats.value = res.data.filter(o => !props.modelValue.includes(o.id))
    }
}

function surSaisie(): void {
    if (timer) clearTimeout(timer)
    timer = setTimeout(charger, 250)
}

function basculer(o: OrigineListeItem): void {
    const nouvelles = [...props.modelValue, o.id]
    emit('update:modelValue', nouvelles)
    selectionnees.value = [...selectionnees.value, o]
    resultats.value = resultats.value.filter(r => r.id !== o.id)
}

function retirer(id: number): void {
    emit('update:modelValue', props.modelValue.filter(x => x !== id))
    selectionnees.value = selectionnees.value.filter(o => o.id !== id)
}

function classeType(type: string): string {
    const t = type.toUpperCase()
    if (t === 'INCIDENT') return 'tag--incident'
    if (t === 'RISQUE') return 'tag--risque'
    return 'tag--autre'
}

watch(recherche, surSaisie)

onMounted(charger)
</script>

<template>
    <div class="origines-select" :class="{ 'origines-select--disabled': disabled }">
        <label class="select-label">
            Origines <span class="required">*</span>
        </label>

        <!-- Chips des sélectionnées -->
        <div v-if="selectionnees.length > 0" class="chips">
            <div v-for="o in selectionnees" :key="o.id" class="chip">
                <span :class="['tag', classeType(o.typeOrigine)]">{{ o.typeOrigine }}</span>
                <code class="chip__code">{{ o.code }}</code>
                <span class="chip__label">{{ o.designation }}</span>
                <button type="button" class="chip__remove" @click="retirer(o.id)" :disabled="disabled">
                    <i class="bi bi-x-lg"></i>
                </button>
            </div>
        </div>

        <!-- Champ de recherche -->
        <div class="search-wrap">
            <i class="bi bi-search search-wrap__icon"></i>
            <input v-model="recherche" type="text" class="search-wrap__input"
                placeholder="Rechercher une origine (code ou désignation)…" :disabled="disabled"
                @focus="ouvert = true" />
            <span v-if="loading" class="search-wrap__loader">
                <span class="spinner"></span>
            </span>
        </div>

        <!-- Résultats -->
        <div v-if="ouvert && resultats.length > 0" class="results">
            <button v-for="o in resultats" :key="o.id" type="button" class="result-item" @click="basculer(o)">
                <span :class="['tag', classeType(o.typeOrigine)]">{{ o.typeOrigine }}</span>
                <code class="result-item__code">{{ o.code }}</code>
                <span class="result-item__label">{{ o.designation }}</span>
                <i class="bi bi-plus-circle result-item__add"></i>
            </button>
        </div>

        <div v-else-if="ouvert && !loading && resultats.length === 0" class="empty">
            Aucune origine ne correspond à votre recherche.
        </div>
    </div>
</template>

<style scoped>
.origines-select {
    display: flex;
    flex-direction: column;
    gap: 0.55rem;
}

.origines-select--disabled {
    opacity: 0.6;
    pointer-events: none;
}

.select-label {
    font-size: 0.78rem;
    font-weight: 600;
    color: #15233c;
}

.required {
    color: #d64545;
}

.chips {
    display: flex;
    flex-wrap: wrap;
    gap: 0.4rem;
}

.chip {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.35rem 0.5rem 0.35rem 0.65rem;
    background: #f8fafc;
    border: 1px solid #e6ebf3;
    border-radius: 8px;
    font-size: 0.8rem;
    max-width: 100%;
}

.chip__code {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.72rem;
    font-weight: 600;
    color: #1a2a6c;
    background: #eef1f5;
    padding: 0.1rem 0.35rem;
    border-radius: 3px;
}

.chip__label {
    color: #15233c;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 260px;
}

.chip__remove {
    background: none;
    border: none;
    color: #8a95a8;
    cursor: pointer;
    padding: 0.2rem;
    border-radius: 4px;
    font-size: 0.65rem;
}

.chip__remove:hover {
    background: #fde9e9;
    color: #d64545;
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
    gap: 0.5rem;
    padding: 0.5rem 0.65rem;
    background: none;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    text-align: left;
    font-size: 0.82rem;
    transition: background .12s;
}

.result-item:hover {
    background: #f8fafc;
}

.result-item__code {
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.72rem;
    font-weight: 600;
    color: #1a2a6c;
    background: #eef1f5;
    padding: 0.1rem 0.35rem;
    border-radius: 3px;
    flex: none;
}

.result-item__label {
    flex: 1;
    color: #15233c;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.result-item__add {
    color: #4a6cf7;
    font-size: 0.85rem;
    flex: none;
}

.empty {
    padding: 0.75rem;
    text-align: center;
    color: #8a95a8;
    font-size: 0.82rem;
    background: #f8fafc;
    border-radius: 8px;
}

.tag {
    display: inline-block;
    padding: 0.1rem 0.5rem;
    border-radius: 20px;
    font-size: 0.62rem;
    font-weight: 800;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    flex: none;
}

.tag--incident {
    background: #fde9e9;
    color: #b03030;
}

.tag--risque {
    background: #fdf3e2;
    color: #9a6409;
}

.tag--autre {
    background: #eef1f5;
    color: #5b6b7d;
}
</style>
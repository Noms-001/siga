<script setup lang="ts">
/**
 * Pagination pilotee par le backend.
 *
 * Volontairement sans logique de slice : le composant ne connait que l'etat
 * de la page courante et emet une demande. C'est le serveur qui applique
 * LIMIT/OFFSET apres les filtres.
 */
import { computed } from 'vue'

const props = withDefaults(defineProps<{
    page: number
    totalPages: number
    totalElements?: number
    pageSize?: number
    disabled?: boolean
}>(), {
    totalElements: 0,
    pageSize: 10,
    disabled: false,
})

const emit = defineEmits<{
    (e: 'change', page: number): void
}>()

/** Fenetre de pages affichee autour de la page courante. */
const visiblePages = computed<(number | '…')[]>(() => {
    const total = props.totalPages

    if (total <= 7) {
        return Array.from({ length: total }, (_, i) => i + 1)
    }

    const current = props.page
    const pages: (number | '…')[] = [1]

    const debut = Math.max(2, current - 1)
    const fin = Math.min(total - 1, current + 1)

    if (debut > 2) pages.push('…')
    for (let p = debut; p <= fin; p++) pages.push(p)
    if (fin < total - 1) pages.push('…')

    pages.push(total)

    return pages
})

const canPrevious = computed(() => props.page > 1 && !props.disabled)
const canNext = computed(() => props.page < props.totalPages && !props.disabled)

const rangeLabel = computed(() => {
    if (props.totalElements === 0) return 'Aucun résultat'

    const first = (props.page - 1) * props.pageSize + 1
    const last = Math.min(props.page * props.pageSize, props.totalElements)

    return `${first}–${last} sur ${props.totalElements}`
})

function goTo(page: number) {
    if (props.disabled) return
    if (page < 1 || page > props.totalPages) return
    if (page === props.page) return

    emit('change', page)
}
</script>

<template>
    <nav v-if="totalPages > 0" class="base-pagination" aria-label="Pagination">
        <span class="base-pagination__range">{{ rangeLabel }}</span>

        <ul class="base-pagination__list">
            <li>
                <button
                    type="button"
                    class="base-pagination__btn"
                    :disabled="!canPrevious"
                    aria-label="Page précédente"
                    @click="goTo(page - 1)">
                    <i class="bi bi-chevron-left"></i>
                </button>
            </li>

            <li v-for="(item, index) in visiblePages" :key="`${item}-${index}`">
                <span v-if="item === '…'" class="base-pagination__ellipsis">…</span>
                <button
                    v-else
                    type="button"
                    class="base-pagination__btn"
                    :class="{ 'base-pagination__btn--active': item === page }"
                    :aria-current="item === page ? 'page' : undefined"
                    :disabled="disabled"
                    @click="goTo(item as number)">
                    {{ item }}
                </button>
            </li>

            <li>
                <button
                    type="button"
                    class="base-pagination__btn"
                    :disabled="!canNext"
                    aria-label="Page suivante"
                    @click="goTo(page + 1)">
                    <i class="bi bi-chevron-right"></i>
                </button>
            </li>
        </ul>
    </nav>
</template>

<style scoped>
.base-pagination {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 1rem;
    padding: 1rem 0;
}

.base-pagination__range {
    font-size: 0.82rem;
    color: #74879b;
}

.base-pagination__list {
    display: flex;
    align-items: center;
    gap: 0.3rem;
    list-style: none;
    margin: 0;
    padding: 0;
}

.base-pagination__btn {
    min-width: 2.1rem;
    height: 2.1rem;
    padding: 0 0.5rem;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #dde4ec;
    border-radius: 6px;
    background: #fff;
    color: #42556b;
    font-size: 0.82rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
}

.base-pagination__btn:hover:not(:disabled) {
    border-color: #1e88a8;
    color: #1e88a8;
}

.base-pagination__btn:disabled {
    opacity: 0.45;
    cursor: not-allowed;
}

.base-pagination__btn--active {
    background: #1e88a8;
    border-color: #1e88a8;
    color: #fff;
}

.base-pagination__ellipsis {
    padding: 0 0.25rem;
    color: #74879b;
    font-size: 0.82rem;
}
</style>

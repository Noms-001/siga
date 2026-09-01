<script setup lang="ts">
import { computed, watch } from 'vue'
import BaseButton from '../BaseButton'
import type { BaseTableProps, TableAlign, TableColumn } from './BaseTable.types'
import { useBaseTable } from './BaseTable'

const props = defineProps<BaseTableProps<any>>()

const emit = defineEmits<{
    (e: 'update:selectedItems', value: any[]): void
}>()

const {
    search,
    currentPage,
    sort,
    filteredItems,
    normalizedColumns,
    paginatedItems,
    totalPages,
    sortKey,
    sortDirection,
    getValue
} = useBaseTable(props, emit)

/* reset pagination */
watch(search, () => {
    currentPage.value = 1
})

watch(
    () => props.items,
    () => {
        currentPage.value = 1
    }
)

/* =========================
   HELPERS (UI SAFE)
========================= */

const isSelected = (item: any) => {
    return (props.selectedItems ?? []).some((s: any) =>
        JSON.stringify(s) === JSON.stringify(item)
    )
}

const toggleItem = (item: any) => {
    const selected = [...(props.selectedItems ?? [])]

    const index = selected.findIndex((s: any) =>
        JSON.stringify(s) === JSON.stringify(item)
    )

    if (index >= 0) {
        selected.splice(index, 1)
    } else {
        selected.push(item)
    }

    emit('update:selectedItems', selected)
}

const allSelected = () => {
    if (!paginatedItems.value.length) return false
    return paginatedItems.value.every(isSelected)
}

const toggleAll = () => {
    const selected = [...(props.selectedItems ?? [])]
    const page = paginatedItems.value

    const all = allSelected()

    page.forEach(item => {
        const index = selected.findIndex((s: any) =>
            JSON.stringify(s) === JSON.stringify(item)
        )

        if (all && index >= 0) {
            selected.splice(index, 1)
        }

        if (!all && index === -1) {
            selected.push(item)
        }
    })

    emit('update:selectedItems', selected)
}

/* =========================
   COLUMN HELPERS
========================= */

const getHeadAlignment = (column: TableColumn, items: any[]) => {
    if (!items.length) return ''
    return getCellAlignment(column, items[0])
}

const isDateValue = (value: any) => {
    if (value instanceof Date) {
        return !Number.isNaN(value.getTime())
    }
    if (typeof value !== 'string') {
        return false
    }
    const trimmed = value.trim()
    return /^\d{4}[-\/]\d{2}[-\/]\d{2}/.test(trimmed) || /^\d{2}[-\/]\d{2}[-\/]\d{4}/.test(trimmed)
}

const getCellAlignment = (column: TableColumn, item: any) => {
    const alignClass = normalizeAlignment(column.align)
    if (alignClass) return alignClass

    const rawValue = getValue(item, column.key)
    if (column.total || isNumericValue(rawValue) || isDateValue(rawValue)) {
        return 'text-end'
    }
    return 'text-start'
}

const normalizeAlignment = (align: TableAlign | undefined) => {
    if (!align) return null
    if (typeof align !== 'string') return null
    const normalized = align.trim().toLowerCase()
    if (normalized.startsWith('text-')) {
        return normalized
    }
    if (normalized === 'left' || normalized === 'start') {
        return 'text-start'
    }
    if (normalized === 'right' || normalized === 'end') {
        return 'text-end'
    }
    if (normalized === 'center') {
        return 'text-center'
    }
    return null
}

const isNumericValue = (value: any) => {
    if (typeof value === 'number' && isFinite(value)) {
        return true
    }
    if (typeof value !== 'string') {
        return false
    }
    const trimmed = value.trim()
    return trimmed !== '' && /^-?\d+(?:[\.,]\d+)?$/.test(trimmed)
}

const getFooterCellAlignment = (column: TableColumn) => {
    const alignClass = normalizeAlignment(column.align)
    if (alignClass) return alignClass
    if (column.total) return 'text-end'
    if (isNumericValue(column.footer)) return 'text-end'
    return 'text-start'
}

const getTotal = (columnKey: string) => {
    const total = filteredItems.value.reduce((sum, item) => {
        const value = Number(item?.[columnKey]) || 0
        return sum + value
    }, 0)

    return formatNumber(total)
}

const getPageTotal = (columnKey: string) => {
    return paginatedItems.value.reduce((sum, item) => {
        const value = Number(item?.[columnKey]) || 0;
        return sum + value;
    }, 0);
};

const formatNumber = (num: any) => {
    return Number(parseFloat(num).toFixed(5))
}

const handleRowClick = (event: Event, item: any) => {
    const ignored = (event.target as HTMLElement).closest(
        'button, input, select, textarea, a, .no-row-click'
    )
    if (ignored) return

    props.onRowClick?.(item, event)
}

const handleColumnClick = (column: TableColumn, item: any, event: Event) => {
    column.onClick?.(getValue(item, column.key), item, event)
    event.stopPropagation()
}

const totalItems = computed(() => filteredItems.value.length ? filteredItems.value.length : props.items.length)
</script>

<template>
    <div :class="['base-table-container', customClass]">

        <!-- SEARCH -->
        <div v-if="searchable" class="table-search">
            <div class="search-box">
                <span class="search-icon">
                    <i class="bi bi-search"></i>
                </span>
                <input v-model="search" type="text" placeholder="Rechercher..." />
            </div>
        </div>

        <div class="table-scroll">
            <table class="table table-modern align-middle no-total">

                <!-- HEADER -->
                <thead>
                    <tr>
                        <th v-if="multiSelect" class="text-center">
                            <input type="checkbox" :checked="allSelected()" @change="toggleAll" />
                        </th>

                        <th v-for="column in normalizedColumns" :key="column.key" @click="sort(column.key)"
                            :class="getHeadAlignment(column, paginatedItems)">
                            {{ column.label }}

                            <span v-if="sortKey === column.key">
                                {{ sortDirection === 'asc' ? '▲' : '▼' }}
                            </span>
                        </th>
                    </tr>
                </thead>

                <!-- BODY -->
                <tbody>

                    <!-- LOADING -->
                    <template v-if="loading">
                        <tr v-for="i in 4" :key="i">
                            <td v-if="multiSelect"></td>
                            <td v-for="col in normalizedColumns" :key="col.key">
                                <div class="skeleton"></div>
                            </td>
                        </tr>
                    </template>

                    <!-- EMPTY -->
                    <tr v-else-if="paginatedItems.length === 0">
                        <td :colspan="normalizedColumns.length + (multiSelect ? 1 : 0)" class="text-center py-5">
                            Aucune donnée
                        </td>
                    </tr>

                    <!-- ROWS -->
                    <tr v-else v-for="(item, index) in paginatedItems" :key="index"
                        @click="handleRowClick($event, item)" :class="{ clickable: onRowClick }">

                        <!-- SELECT -->
                        <td v-if="multiSelect" class="text-center no-row-click">
                            <input type="checkbox" :checked="isSelected(item)" @change="toggleItem(item)" />
                        </td>

                        <!-- CELLS -->
                        <td v-for="column in normalizedColumns" :key="column.key" :class="[
                            column.class,
                            getCellAlignment(column, item)
                        ]" @click="handleColumnClick(column, item, $event)">
                            <slot :name="`cell-${column.key}`" :item="item" :value="getValue(item, column.key)">
                                {{ column.formatter ? column.formatter(getValue(item, column.key), item) :
                                    getValue(item, column.key) }}
                            </slot>
                        </td>
                    </tr>
                    <slot name="line" />
                    <tr>
                        <td :colspan="normalizedColumns.length + (multiSelect ? 1 : 0)" class="pt-1 pb-1 pe-4">
                            <div class="d-flex justify-content-end">
                                <div class="table-count text-white">
                                    <i class="bi bi-list-ul"></i>
                                    {{ paginatedItems.length }} élément{{ paginatedItems.length > 1 ? 's' : '' }}
                                </div>
                            </div>
                        </td>
                    </tr>

                </tbody>

                <tfoot v-if="!loading && normalizedColumns.some(col => col.total || col.footer)">
                    <tr>
                        <td v-if="multiSelect"></td>
                        <td v-for="column in normalizedColumns" :key="column.key"
                            :class="[getFooterCellAlignment(column), totalPages > 1 ? 'pad-red' : '']">

                            <template v-if="column.total">
                                <span class="grand-total mb-2" title="Total global">
                                    {{ formatNumber(getPageTotal(column.key)) }}
                                </span>
                            </template>

                            <template v-else-if="column.footer">
                                {{ column.footer }}
                            </template>
                        </td>
                    </tr>
                    <tr v-if="totalPages > 1">
                        <td v-if="multiSelect"></td>
                        <td v-for="column in normalizedColumns" :key="column.key"
                            :class="['page-total', column.footer ? 'align-start' : '']">
                            <template v-if="column.total" title="Total sur toute les page">
                                All-page: {{ column.totalFormatter ? column.totalFormatter(getTotal(column.key),
                                    paginatedItems) : getTotal(column.key) }}
                            </template>
                        </td>
                    </tr>
                </tfoot>
            </table>
        </div>

        <!-- PAGINATION -->
        <div class="table-pagination" v-if="totalPages > 1">
            <div class="table-pagination-info">
                <strong>{{ totalItems }}</strong> éléments
            </div>

            <div class="table-pagination-center">
                <BaseButton label="Précédent" size="sm" :disabled="currentPage === 1" @click="currentPage--" />

                <span>
                    Page {{ currentPage }} / {{ totalPages }}
                </span>

                <BaseButton label="Suivant" size="sm" :disabled="currentPage === totalPages" @click="currentPage++" />
            </div>
        </div>

    </div>
</template>

<style scoped>
/* ===================================
   CONTAINER
=================================== */

.base-table-container {
    width: 100%;
    background: var(--surface-color);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 1px 3px rgba(0, 0, 0, .04), 0 1px 8px rgba(0, 0, 0, .03);
}

/* ===================================
   TOOLBAR
=================================== */

.table-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: .75rem 1rem;
    background: var(--surface-color);
    border-bottom: 1px solid var(--border-color);
}

.toolbar-left {
    display: flex;
    align-items: center;
    gap: .5rem;
}

.toolbar-btn {
    display: flex;
    align-items: center;
    gap: .4rem;
    border: 1px solid var(--border-color);
    background: transparent;
    color: var(--text-muted-custom);
    padding: .4rem .75rem;
    border-radius: 8px;
    font-size: .8rem;
    transition: background .15s, color .15s, border-color .15s;
    cursor: pointer;
}

.toolbar-btn:hover {
    background: rgba(59, 115, 247, .07);
    color: var(--primary-color);
    border-color: rgba(59, 115, 247, .3);
}

/* ===================================
   SEARCH
=================================== */

.table-search {
    padding: .875rem 1rem;
    background: var(--surface-color);
    border-bottom: 1px solid var(--border-color);
}

.search-box {
    position: relative;
    max-width: 340px;
    width: 100%;
}

.search-icon {
    position: absolute;
    left: .85rem;
    top: 50%;
    transform: translateY(-50%);
    color: var(--text-muted-custom);
    font-size: .85rem;
    pointer-events: none;
}

.search-box input {
    width: 100%;
    height: 38px;
    padding-left: 2.4rem;
    padding-right: 1rem;
    border: 1px solid var(--border-color);
    border-radius: 8px;
    background: var(--light-color);
    color: var(--text-color);
    font-size: .875rem;
    transition: border-color .15s, box-shadow .15s;
}

.search-box input::placeholder {
    color: var(--text-muted-custom);
}

.search-box input:focus {
    outline: none;
    border-color: var(--primary-color);
    box-shadow: 0 0 0 3px rgba(59, 115, 247, .1);
}

/* ===================================
   TABLE
=================================== */

.table-scroll {
    overflow-x: auto;
}

.table {
    width: 100%;
    min-width: 900px;
    border-collapse: collapse;
}

/* HEADER */

thead {
    background: var(--bg-gray);
}

thead th {
    padding: .75rem .875rem;
    font-size: .7rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: .05em;
    color: var(--text-muted-custom);
    border-bottom: 1px solid var(--border-color);
    white-space: nowrap;
    user-select: none;
    transition: color .15s;
    cursor: pointer;
}

thead th:hover {
    color: var(--text-color);
}

/* BODY */

tbody td {
    padding: .85rem .875rem;
    color: var(--text-color);
    font-size: .875rem;
    border-bottom: 1px solid var(--border-color);
    white-space: nowrap;
    background-color: var(--light-color);
}

tbody tr {
    transition: background .1s;
}

tbody tr:hover {
    background: rgba(59, 115, 247, .035);
}

tbody tr:hover td {
    background: rgba(59, 115, 247, .035);
}

tbody tr:last-child td {
    border-bottom: none;
}

/* FOOTER */

tfoot td {
    padding: .85rem .875rem;
    font-weight: 600;
    color: var(--text-color);
    border-top: 1px solid var(--border-color);
    border-bottom: none;
    background: var(--light-color);
}

.page-total {
    font-size: 0.75rem;
    color: var(--text-muted-custom);
    font-weight: normal;
    text-align: end;
    padding-top: 5px;
    border-top-style: dashed;
}

.pad-red {
    padding-bottom: 5px;
}

.align-start {
    text-align: start;
}

.grand-total {
    font-size: 0.9rem;
    color: var(--text-color);
    font-weight: 700;
}

/* ===================================
   EMPTY STATE
=================================== */

tbody tr td.text-center.py-5 {
    padding-top: 3rem;
    padding-bottom: 3rem;
    color: var(--text-muted-custom);
    font-size: .875rem;
}

/* ===================================
   ROW CLICK
=================================== */

.clickable {
    cursor: pointer;
}

/* ===================================
   PAGINATION
=================================== */

.table-pagination {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    padding: .875rem 1rem;
    border-top: 1px solid var(--border-color);
    background: rgb(226, 237, 255);
}

.table-pagination-center {
    display: flex;
    align-items: center;
    gap: .875rem;
    margin: 0 auto;
}

.table-pagination-info {
    font-size: .8rem;
    color: var(--text-muted-custom);
    white-space: nowrap;
}

.table-pagination-info strong {
    color: var(--text-color);
    font-weight: 600;
}

.table-count {
    display: inline-flex;
    align-items: center;
    gap: .4rem;
    font-size: .78rem;
    font-weight: 500;
    color: var(--text-muted-custom);
    background: var(--bg-gray);
    padding: .3rem .7rem;
    border-radius: 20px;
    white-space: nowrap;
}

/* ===================================
   SKELETON
=================================== */

.skeleton {
    width: 100%;
    height: 14px;
    border-radius: 6px;
    background:
        linear-gradient(90deg,
            var(--skeleton-bg) 25%,
            var(--skeleton-highlight) 50%,
            var(--skeleton-bg) 75%);
    background-size: 200% 100%;
    animation: skeleton-loading 1.5s infinite;
}

.skeleton-checkbox {
    width: 16px;
    height: 16px;
    margin: 0 auto;
}

@keyframes skeleton-loading {
    from {
        background-position: 200% 0;
    }

    to {
        background-position: -200% 0;
    }
}

/* ===================================
   CHECKBOXES
=================================== */

thead th input[type="checkbox"],
tbody td input[type="checkbox"] {
    width: 16px;
    height: 16px;
    cursor: pointer;
    accent-color: var(--primary-color);
    border-radius: 4px;
}

thead th input[type="checkbox"]:disabled,
tbody td input[type="checkbox"]:disabled {
    cursor: not-allowed;
    opacity: 0.5;
}

th {
    background-color: var(--primary-color) !important;
    color: var(--w-color) !important;
}

/* ===================================
   RESPONSIVE
=================================== */

@media (max-width: 768px) {

    .table-toolbar {
        flex-direction: column;
        align-items: flex-start;
        gap: .75rem;
    }

    .search-box {
        max-width: 100%;
    }

    .table {
        min-width: 700px;
    }
}

.no-total {
    margin-bottom: 0 !important;
}
</style>
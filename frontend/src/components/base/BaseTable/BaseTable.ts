import { computed, ref, onMounted, onUnmounted } from 'vue'
import type { BaseTableProps, TableColumn } from './BaseTable.types'

export function useBaseTable(props: BaseTableProps<any>, emit: any) {
    const search = ref('')
    const currentPage = ref(1)
    const sortKey = ref('')
    const sortDirection = ref<'asc' | 'desc'>('asc')

    const normalizedColumns = computed<TableColumn[]>(() => {
        return (props.columns as any[]).map((col, i) => {
            if (typeof col === 'string') {
                return { key: col, label: col }
            }
            return col
        })
    })

    const filteredItems = computed(() => {
        if (!search.value) return props.items
        return props.items.filter(item =>
            JSON.stringify(item).toLowerCase().includes(search.value.toLowerCase())
        )
    })

    const sortedItems = computed(() => {
        if (!sortKey.value) return filteredItems.value

        return [...filteredItems.value].sort((a: any, b: any) => {
            const va = a?.[sortKey.value]
            const vb = b?.[sortKey.value]

            if (va < vb) return sortDirection.value === 'asc' ? -1 : 1
            if (va > vb) return sortDirection.value === 'asc' ? 1 : -1
            return 0
        })
    })

    const totalPages = computed(() =>
        Math.ceil(sortedItems.value.length / (props.pageSize ?? 10))
    )

    const paginatedItems = computed(() => {
        const start = (currentPage.value - 1) * (props.pageSize ?? 10)
        return sortedItems.value.slice(start, start + (props.pageSize ?? 10))
    })

    // --- Nouvelle logique de pagination avec numéros ---
    const visiblePages = computed(() => {
        const total = totalPages.value
        const current = currentPage.value
        const maxVisible = 5

        if (total <= maxVisible) {
            return Array.from({ length: total }, (_, i) => i + 1)
        }

        let start = Math.max(1, current - Math.floor(maxVisible / 2))
        let end = start + maxVisible - 1

        if (end > total) {
            end = total
            start = Math.max(1, end - maxVisible + 1)
        }

        return Array.from({ length: end - start + 1 }, (_, i) => start + i)
    })

    const goToPage = (page: number) => {
        if (page >= 1 && page <= totalPages.value) {
            currentPage.value = page
        }
    }

    const sort = (key: string) => {
        if (!props.sortable) return

        if (sortKey.value === key) {
            sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
            return
        }

        sortKey.value = key
        sortDirection.value = 'asc'
    }

    const getValue = (item: any, key: string) => item?.[key]

    // --- Gestion du clavier pour la navigation ---
    const handleKeydown = (event: KeyboardEvent) => {
        // Navigation avec les flèches gauche/droite
        if (event.key === 'ArrowLeft' && currentPage.value > 1) {
            event.preventDefault()
            goToPage(currentPage.value - 1)
        } else if (event.key === 'ArrowRight' && currentPage.value < totalPages.value) {
            event.preventDefault()
            goToPage(currentPage.value + 1)
        }
    }

    // Monter/démonter l'écouteur d'événements
    onMounted(() => {
        document.addEventListener('keydown', handleKeydown)
    })

    onUnmounted(() => {
        document.removeEventListener('keydown', handleKeydown)
    })

    return {
        search,
        currentPage,
        sortKey,
        sortDirection,
        normalizedColumns,
        filteredItems,
        sortedItems,
        paginatedItems,
        totalPages,
        visiblePages,
        goToPage,
        sort,
        getValue
    }
}
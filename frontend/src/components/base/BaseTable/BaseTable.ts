import { computed, ref } from 'vue'
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
        sort,
        getValue
    }
}
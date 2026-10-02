export type TableAlign = 'left' | 'right' | 'center' | 'start' | 'end' | 'text-start' | 'text-end' | 'text-center'

export type TableVariant = 'primary' | 'success' | 'danger' | 'warning' | 'secondary'

export interface TableColumn<T = any> {
    key: string
    label: string
    class?: string
    align?: TableAlign

    badge?: boolean
    badgeVariant?: string | ((value: any, item: T) => string)
    badgeColor?: string
    badgeTextColor?: string

    formatter?: (value: any, item?: T) => string
    total?: boolean
    totalFormatter?: (total: number, items: T[]) => string

    footer?: string | number

    onClick?: (value: any, item: T, event: Event) => void
}

export interface BaseTableProps<T = any> {
    items: T[]
    columns: Array<TableColumn<T> | string>

    pageSize?: number
    searchable?: boolean
    sortable?: boolean
    loading?: boolean

    customClass?: string | string[] | Record<string, boolean>

    onRowClick?: ((item: T, event: Event) => void) | null

    multiSelect?: boolean
    selectedItems?: T[]
}
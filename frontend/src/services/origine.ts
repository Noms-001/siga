import { get, type ApiResponse } from '@/services/api-client'
import type { OrigineListeItem } from '@/types/planaction'

const BASE = '/origines'

function query(params: Record<string, unknown>): string {
    const qs = new URLSearchParams()
    for (const [k, v] of Object.entries(params)) {
        if (v === undefined || v === null || v === '') continue
        qs.set(k, String(v))
    }
    const q = qs.toString()
    return q ? `?${q}` : ''
}

export function rechercherOrigines(
    search?: string,
    type?: string
): Promise<ApiResponse<OrigineListeItem[]>> {
    return get<OrigineListeItem[]>(
        `${BASE}${query({ search, type })}`
    )
}
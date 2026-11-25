/* ============================================================
   src/utils/importTree.ts
   Fusion générique des feuilles Excel en un arbre d'import.
   ============================================================ */

import type { ImportCible } from '@/types/importActivite'
import { normalizeImportDate } from '@/utils/importDate'

/* ============================================================
   TYPES PUBLICS
   ============================================================ */
export type SheetRole =
    | 'ACTIVITES'
    | 'SOUS_ACTIVITES'
    | 'LIVRABLES'
    | 'RESULTATS_INTERMEDIAIRES'
    | 'INDICATEURS'

export interface MappedFieldInput {
    sheet: string
    field: string
}

export interface SheetAnalysis {
    sheet: string
    roles: Set<SheetRole>
    hasActiviteRef: boolean
    hasSousActiviteRef: boolean
    /** Vrai si la feuille porte la colonne « sousActivites[].designation » (désignation SA). */
    hasSousActiviteDesignation: boolean
}

export interface ParsedField {
    level1: string
    isArray1: boolean
    level2: string | null
    isArray2: boolean
    level3: string | null
    leaf: string
    simple: boolean
}

export interface BuildTreeInput {
    cible: ImportCible
    mapped: MappedFieldInput[]
    rowsBySheet: Map<string, Record<string, any>[]>
}

export interface ImportTreeResult {
    activites: Record<string, any>[]
    errors: string[]
    warnings: string[]
    analysis: Map<string, SheetAnalysis>
}

/* ============================================================
   CONSTANTES
   ============================================================ */
export const DATE_LEAVES = new Set(['dateDebut', 'dateFin'])

const CIBLE_TO_LEVEL: Record<Exclude<ImportCible, 'ACTIVITES'>, string> = {
    SOUS_ACTIVITES: 'sousActivites',
    RESULTATS_INTERMEDIAIRES: 'resultatsIntermediaires',
    INDICATEURS: 'indicateurs',
}

const COLLECTION_KEY: Record<
    Exclude<ImportCible, 'ACTIVITES'>,
    'resultatsIntermediaires' | 'indicateurs' | 'sousActivites'
> = {
    SOUS_ACTIVITES: 'sousActivites',
    RESULTATS_INTERMEDIAIRES: 'resultatsIntermediaires',
    INDICATEURS: 'indicateurs',
}

/* ============================================================
   PARSEUR DE CHEMIN
   ============================================================ */
export function parseField(name: string): ParsedField {
    if (name.startsWith('_')) {
        return { level1: name, isArray1: false, level2: null, isArray2: false, level3: null, leaf: name, simple: true }
    }
    const parts = name.split('.')
    if (parts.length === 1) {
        return { level1: parts[0]!, isArray1: false, level2: null, isArray2: false, level3: null, leaf: parts[0]!, simple: true }
    }
    if (parts.length === 2) {
        const isArray1 = parts[0]!.endsWith('[]')
        const level1 = isArray1 ? parts[0]!.slice(0, -2) : parts[0]!
        return { level1, isArray1, level2: parts[1]!, isArray2: false, level3: null, leaf: parts[1]!, simple: false }
    }
    const isArray1 = parts[0]!.endsWith('[]')
    const level1 = isArray1 ? parts[0]!.slice(0, -2) : parts[0]!
    const isArray2 = parts[1]!.endsWith('[]')
    const level2 = isArray2 ? parts[1]!.slice(0, -2) : parts[1]!
    return { level1, isArray1, level2, isArray2, level3: parts[2]!, leaf: parts[2]!, simple: false }
}

/* ============================================================
   HELPERS
   ============================================================ */
function isEmpty(v: any): boolean {
    return v === null || v === undefined || String(v).trim() === ''
}

function safeDate(v: any, sheet: string, errors: string[], field: string): any {
    if (isEmpty(v)) return v
    try {
        return normalizeImportDate(v)
    } catch (e: any) {
        errors.push(`Date invalide « ${v} » pour ${field} (feuille ${sheet}) : ${e?.message ?? 'format non reconnu'}`)
        return v
    }
}

function rowHasAnyData(row: Record<string, any>): boolean {
    for (const v of Object.values(row)) if (!isEmpty(v)) return true
    return false
}

function rowHasLevel(row: Record<string, any>, level: string): boolean {
    for (const [k, v] of Object.entries(row)) {
        if (isEmpty(v)) continue
        if (parseField(k).level1 === level) return true
    }
    return false
}

function hasActivityFields(row: Record<string, any>): boolean {
    for (const [k, v] of Object.entries(row)) {
        if (isEmpty(v) || k.startsWith('_')) continue
        const p = parseField(k)
        if (p.level1 === 'sousActivites') continue
        if (p.level1 === 'resultatsIntermediaires') continue
        if (p.level1 === 'indicateurs') continue
        return true
    }
    return false
}

function hasSAFields(row: Record<string, any>): boolean {
    for (const [k, v] of Object.entries(row)) {
        if (isEmpty(v)) continue
        if (parseField(k).level1 === 'sousActivites') return true
    }
    return false
}

/** Vrai si la ligne porte au moins un champ SA « direct » (non livrable). */
function isSARow(row: Record<string, any>): boolean {
    for (const [k, v] of Object.entries(row)) {
        if (isEmpty(v)) continue
        const p = parseField(k)
        if (p.level1 !== 'sousActivites') continue
        if (p.isArray2) continue
        return true
    }
    return false
}

/** Vrai si la ligne porte au moins un champ livrable. */
function isLivrableRow(row: Record<string, any>): boolean {
    for (const [k, v] of Object.entries(row)) {
        if (isEmpty(v)) continue
        const p = parseField(k)
        if (p.level1 === 'sousActivites' && p.isArray2 && p.level2 === 'livrables') return true
    }
    return false
}

function groupByField(rows: Record<string, any>[], field: string): Map<string, Record<string, any>[]> {
    const map = new Map<string, Record<string, any>[]>()
    for (const row of rows) {
        const ref = String(row[field] ?? '').trim()
        if (!ref) continue
        if (!map.has(ref)) map.set(ref, [])
        map.get(ref)!.push(row)
    }
    return map
}

/* ============================================================
   ANALYSE DES FEUILLES
   ============================================================ */
export function analyzeSheets(mapped: MappedFieldInput[]): Map<string, SheetAnalysis> {
    const out = new Map<string, SheetAnalysis>()
    const get = (sheet: string): SheetAnalysis => {
        let a = out.get(sheet)
        if (!a) {
            a = {
                sheet,
                roles: new Set(),
                hasActiviteRef: false,
                hasSousActiviteRef: false,
                hasSousActiviteDesignation: false,
            }
            out.set(sheet, a)
        }
        return a
    }

    for (const m of mapped) {
        if (!m.field) continue
        const a = get(m.sheet)
        if (m.field === '_referenceActivite') { a.hasActiviteRef = true; continue }
        if (m.field === '_referenceSousActivite') { a.hasSousActiviteRef = true; continue }
        if (m.field.startsWith('_')) continue

        const p = parseField(m.field)

        // Info : feuille porteuse de la désignation SA
        if (p.level1 === 'sousActivites' && p.level2 === 'designation' && !p.isArray2) {
            a.hasSousActiviteDesignation = true
        }

        switch (p.level1) {
            case 'sousActivites':
                a.roles.add(p.level2 === 'livrables' ? 'LIVRABLES' : 'SOUS_ACTIVITES')
                break
            case 'resultatsIntermediaires': a.roles.add('RESULTATS_INTERMEDIAIRES'); break
            case 'indicateurs': a.roles.add('INDICATEURS'); break
            default:
                if (p.simple) a.roles.add('ACTIVITES')
        }
    }
    return out
}

/* ============================================================
   STRUCTURES DE TRAVAIL INTERNES
   ============================================================ */
interface WorkingSA {
    ref: string
    data: Record<string, any>
    livrables: Record<string, any>[]
}

interface WorkingActivity {
    ref: string
    data: Record<string, any>
    objectifSpecifique: Record<string, any>
    sousActivites: WorkingSA[]
    resultatsIntermediaires: Record<string, any>[]
    indicateurs: Record<string, any>[]
}

function makeActivity(ref: string): WorkingActivity {
    return {
        ref,
        data: {},
        objectifSpecifique: {},
        sousActivites: [],
        resultatsIntermediaires: [],
        indicateurs: [],
    }
}

function makeSA(ref: string): WorkingSA {
    return { ref, data: {}, livrables: [] }
}

/* ============================================================
   MERGE D'UNE LIGNE DANS UNE ACTIVITÉ
   ============================================================ */
function mergeActivityRow(a: WorkingActivity, row: Record<string, any>, errors: string[], sheet: string): void {
    for (const [k, v] of Object.entries(row)) {
        if (isEmpty(v) || k.startsWith('_')) continue
        const p = parseField(k)
        if (p.level1 === 'sousActivites') continue
        if (p.level1 === 'resultatsIntermediaires') continue
        if (p.level1 === 'indicateurs') continue

        const value = DATE_LEAVES.has(p.leaf) ? safeDate(v, sheet, errors, k) : v

        if (p.level1 === 'objectifSpecifique') {
            const key = p.level2 ?? p.leaf
            if (!(key in a.objectifSpecifique) || isEmpty(a.objectifSpecifique[key])) {
                a.objectifSpecifique[key] = value
            }
        } else if (p.simple) {
            if (!(p.level1 in a.data) || isEmpty(a.data[p.level1])) {
                a.data[p.level1] = value
            }
        }
    }
}

/* ============================================================
   MERGE D'UNE LIGNE DANS UNE SOUS-ACTIVITÉ
   (gère aussi les livrables sur la même ligne)
   ============================================================ */
function mergeSousActiviteRow(sa: WorkingSA, row: Record<string, any>, errors: string[], sheet: string): void {
    const livrable: Record<string, any> = {}
    let hasLivrable = false

    for (const [k, v] of Object.entries(row)) {
        if (isEmpty(v) || k.startsWith('_')) continue
        const p = parseField(k)
        if (p.level1 !== 'sousActivites') continue

        const value = DATE_LEAVES.has(p.leaf) ? safeDate(v, sheet, errors, k) : v

        if (p.isArray2 && p.level2 === 'livrables') {
            livrable[p.level3 ?? ''] = value
            hasLivrable = true
        } else if (!p.isArray2) {
            const key = p.level2 ?? p.leaf
            if (!(key in sa.data) || isEmpty(sa.data[key])) {
                sa.data[key] = value
            }
        }
    }

    if (hasLivrable) sa.livrables.push(livrable)
}

/* ============================================================
   COLLECTIONS (résultats, indicateurs)
   ============================================================ */
function buildCollectionItemFromRow(
    row: Record<string, any>,
    level: string,
    errors: string[],
    sheet: string,
): Record<string, any> | null {
    const item: Record<string, any> = {}
    for (const [k, v] of Object.entries(row)) {
        if (isEmpty(v) || k.startsWith('_')) continue
        const p = parseField(k)
        if (p.level1 !== level) continue
        const key = p.level2 ?? p.leaf
        item[key] = DATE_LEAVES.has(p.leaf) ? safeDate(v, sheet, errors, k) : v
    }
    return Object.keys(item).length ? item : null
}

function pushCollectionsFromRow(
    a: WorkingActivity,
    row: Record<string, any>,
    errors: string[],
    sheet: string,
): void {
    const kinds = [
        { level: 'resultatsIntermediaires', key: 'resultatsIntermediaires' as const },
        { level: 'indicateurs', key: 'indicateurs' as const },
    ]
    for (const { level, key } of kinds) {
        if (!rowHasLevel(row, level)) continue
        const item = buildCollectionItemFromRow(row, level, errors, sheet)
        if (item) (a[key] as any[]).push(item)
    }
}

/* ============================================================
   RÉSOLUTION DU PARENT D'UNE SOUS-ACTIVITÉ
   ============================================================ */
function resolveParentRef(
    groupRows: Record<string, any>[],
    activityMap: Map<string, WorkingActivity>,
    sheet: string,
    errors: string[],
): string | null {
    for (const row of groupRows) {
        const ref = String(row['_referenceActivite'] ?? '').trim()
        if (!ref) continue
        if (!activityMap.has(ref)) {
            errors.push(`Feuille « ${sheet} » : référence d'activité « ${ref} » introuvable.`)
            return null
        }
        return ref
    }
    if (activityMap.size === 1) return [...activityMap.keys()][0]!
    errors.push(
        `Feuille « ${sheet} » : impossible de déterminer l'activité parente pour une sous-activité. Mappez « Référence (activité) » ou n'ayez qu'une seule activité.`
    )
    return null
}

/* ============================================================
   SÉRIALISATION
   ============================================================ */
function serializeActivity(a: WorkingActivity): Record<string, any> {
    const obj: Record<string, any> = {}
    if (!a.ref.startsWith('__auto_')) obj._referenceActivite = a.ref
    Object.assign(obj, a.data)
    if (Object.keys(a.objectifSpecifique).length) obj.objectifSpecifique = a.objectifSpecifique
    if (a.sousActivites.length) {
        obj.sousActivites = a.sousActivites.map(sa => ({
            ...sa.data,
            ...(sa.livrables.length ? { livrables: sa.livrables } : {}),
        }))
    }
    if (a.resultatsIntermediaires.length) obj.resultatsIntermediaires = a.resultatsIntermediaires
    if (a.indicateurs.length) obj.indicateurs = a.indicateurs
    return obj
}

/* ============================================================
   POINT D'ENTRÉE
   ============================================================ */
export function buildImportTree(input: BuildTreeInput): ImportTreeResult {
    const { cible, mapped, rowsBySheet } = input
    const analysis = analyzeSheets(mapped)
    const errors: string[] = []
    const warnings: string[] = []

    const activityMap = new Map<string, WorkingActivity>()
    /** Index des sous-activités construites, clé = _referenceSousActivite (pour rattacher les livrables autonomes). */
    const saByRef = new Map<string, WorkingSA>()
    let autoSeq = 0
    const nextAutoRef = () => `__auto_${++autoSeq}`
    const ensureActivity = (ref: string): WorkingActivity => {
        let a = activityMap.get(ref)
        if (!a) { a = makeActivity(ref); activityMap.set(ref, a) }
        return a
    }

    // ==========================================================
    // PHASE 1 — Feuilles REF : jointure par références
    // ==========================================================

    // 1a. Activités
    for (const [sheet, sa] of analysis.entries()) {
        if (!sa.hasActiviteRef || !sa.roles.has('ACTIVITES')) continue
        const allRows = rowsBySheet.get(sheet) ?? []
        const activityRows = allRows.filter(r => hasActivityFields(r))
        const withRef = activityRows.filter(r => String(r['_referenceActivite'] ?? '').trim())
        if (activityRows.length > withRef.length) {
            warnings.push(
                `Feuille « ${sheet} » : ${activityRows.length - withRef.length} ligne(s) sans référence d'activité ignorée(s).`
            )
        }
        for (const [ref, groupRows] of groupByField(withRef, '_referenceActivite')) {
            const a = ensureActivity(ref)
            for (const row of groupRows) mergeActivityRow(a, row, errors, sheet)
        }
    }

    // 1a-bis. Coquilles d'activité pour toutes les refs présentes (utile pour cibles non-ACTIVITES)
    for (const [sheet, sa] of analysis.entries()) {
        if (!sa.hasActiviteRef) continue
        const rows = rowsBySheet.get(sheet) ?? []
        for (const row of rows) {
            const ref = String(row['_referenceActivite'] ?? '').trim()
            if (ref) ensureActivity(ref)
        }
    }

    // 1b. Sous-activités — feuilles portant le rôle SOUS_ACTIVITES
    for (const [sheet, sa] of analysis.entries()) {
        if (!sa.roles.has('SOUS_ACTIVITES')) continue
        if (!sa.hasActiviteRef && !sa.hasSousActiviteRef) continue
        // On ne garde que les lignes qui portent réellement un champ SA « direct »
        // (les lignes livrable-only d'une feuille mixte seront traitées en 1b-bis)
        const rows = (rowsBySheet.get(sheet) ?? []).filter(isSARow)
        if (!rows.length) continue

        if (sa.hasSousActiviteRef) {
            for (const [saRef, groupRows] of groupByField(rows, '_referenceSousActivite')) {
                const parentRef = resolveParentRef(groupRows, activityMap, sheet, errors)
                if (!parentRef) continue
                const parent = activityMap.get(parentRef)!
                const saNode = makeSA(saRef)
                for (const row of groupRows) mergeSousActiviteRow(saNode, row, errors, sheet)
                parent.sousActivites.push(saNode)
                if (saRef && !saByRef.has(saRef)) saByRef.set(saRef, saNode)
            }
        } else {
            for (const row of rows) {
                const parentRef = String(row['_referenceActivite'] ?? '').trim()
                const parent = activityMap.get(parentRef)
                if (!parent) {
                    errors.push(`Feuille « ${sheet} » : référence d'activité « ${parentRef} » introuvable pour une sous-activité.`)
                    continue
                }
                const saNode = makeSA('')
                mergeSousActiviteRow(saNode, row, errors, sheet)
                parent.sousActivites.push(saNode)
            }
        }
    }

    // 1b-bis. Livrables autonomes (feuille dédiée, jointure par Code SA)
    // Uniquement les lignes livrable-only (une ligne mixte SA+livrable est déjà
    // traitée en 1b, il ne faut pas dupliquer le livrable).
    for (const [sheet, sa] of analysis.entries()) {
        if (!sa.roles.has('LIVRABLES')) continue
        const rows = (rowsBySheet.get(sheet) ?? []).filter(r => isLivrableRow(r) && !isSARow(r))
        if (!rows.length) continue

        if (!sa.hasSousActiviteRef) {
            errors.push(
                `Feuille « ${sheet} » : livrables sur lignes séparées sans « Référence (sous-activité) ». Mappez cette colonne pour joindre chaque livrable à sa sous-activité.`
            )
            continue
        }

        for (const row of rows) {
            const saRef = String(row['_referenceSousActivite'] ?? '').trim()
            if (!saRef) continue
            const saNode = saByRef.get(saRef)
            if (!saNode) {
                errors.push(`Feuille « ${sheet} » : sous-activité « ${saRef} » introuvable pour ce livrable.`)
                continue
            }
            const livrable: Record<string, any> = {}
            for (const [k, v] of Object.entries(row)) {
                if (isEmpty(v) || k.startsWith('_')) continue
                const p = parseField(k)
                if (p.level1 === 'sousActivites' && p.isArray2 && p.level2 === 'livrables') {
                    livrable[p.level3 ?? ''] = DATE_LEAVES.has(p.leaf) ? safeDate(v, sheet, errors, k) : v
                }
            }
            if (Object.keys(livrable).length > 0) saNode.livrables.push(livrable)
        }
    }

    // 1c. Collections (résultats, indicateurs)
    const collectionKinds: { role: SheetRole; key: 'resultatsIntermediaires' | 'indicateurs' }[] = [
        { role: 'RESULTATS_INTERMEDIAIRES', key: 'resultatsIntermediaires' },
        { role: 'INDICATEURS', key: 'indicateurs' },
    ]
    for (const { role, key } of collectionKinds) {
        for (const [sheet, sa] of analysis.entries()) {
            if (!sa.roles.has(role)) continue

            // Une feuille unique d'import des activités peut contenir
            // les activités et leurs collections sur les mêmes lignes.
            // La PHASE 2 se chargera de construire l'arbre complet.
            if (cible === 'ACTIVITES' && !sa.hasActiviteRef) {
                continue
            }
            const rows = (rowsBySheet.get(sheet) ?? []).filter(r =>
                rowHasLevel(r, CIBLE_TO_LEVEL[role as Exclude<ImportCible, 'ACTIVITES'>]),
            )
            if (!rows.length) continue

            if (sa.hasActiviteRef) {
                const withRef = rows.filter(r => String(r['_referenceActivite'] ?? '').trim())
                for (const [ref, groupRows] of groupByField(withRef, '_referenceActivite')) {
                    const parent = activityMap.get(ref)
                    if (!parent) {
                        errors.push(`Feuille « ${sheet} » : référence d'activité « ${ref} » introuvable.`)
                        continue
                    }
                    for (const row of groupRows) {
                        const item = buildCollectionItemFromRow(row, key, errors, sheet)
                        if (item) (parent[key] as any[]).push(item)
                    }
                }
            } else if (activityMap.size === 1) {
                const parent = [...activityMap.values()][0]!
                for (const row of rows) {
                    const item = buildCollectionItemFromRow(row, key, errors, sheet)
                    if (item) (parent[key] as any[]).push(item)
                }
            } else {
                errors.push(`Feuille « ${sheet} » : impossible de rattacher les lignes à une activité. Mappez « Référence (activité) ».`)
            }
        }
    }

    // ==========================================================
    // PHASE 2 — Feuilles FLAT (aucune référence mappée)
    // ==========================================================
    for (const [sheet, sa] of analysis.entries()) {
        if (sa.hasActiviteRef || sa.hasSousActiviteRef) continue
        const rows = rowsBySheet.get(sheet) ?? []

        for (const row of rows) {
            if (!rowHasAnyData(row)) continue

            if (cible === 'ACTIVITES') {
                const a = ensureActivity(nextAutoRef())
                mergeActivityRow(a, row, errors, sheet)
                if (hasSAFields(row)) {
                    const saNode = makeSA('')
                    mergeSousActiviteRow(saNode, row, errors, sheet)
                    a.sousActivites.push(saNode)
                }
                pushCollectionsFromRow(a, row, errors, sheet)
            } else {
                if (activityMap.size !== 1) {
                    errors.push(
                        `Feuille « ${sheet} » : impossible de rattacher la ligne à une activité (${cible}). Mappez « Référence (activité) ».`
                    )
                    continue
                }
                const parent = [...activityMap.values()][0]!
                if (cible === 'SOUS_ACTIVITES') {
                    if (!hasSAFields(row)) continue
                    const saNode = makeSA('')
                    mergeSousActiviteRow(saNode, row, errors, sheet)
                    parent.sousActivites.push(saNode)
                } else {
                    const level = CIBLE_TO_LEVEL[cible as Exclude<ImportCible, 'ACTIVITES'>]
                    if (!rowHasLevel(row, level)) continue
                    const item = buildCollectionItemFromRow(row, level, errors, sheet)
                    if (item) (parent[COLLECTION_KEY[cible as Exclude<ImportCible, 'ACTIVITES'>]] as any[]).push(item)
                }
            }
        }
    }

    // ==========================================================
    // PHASE 3 — Validation
    // ==========================================================
    if (activityMap.size === 0) {
        errors.push("Aucune activité n'a pu être construite. Mappez au moins un champ d'activité ou une référence d'activité.")
        return { activites: [], errors, warnings, analysis }
    }

    if (cible === 'ACTIVITES') {
        const missing = [...activityMap.values()].filter(a => isEmpty(a.data.designation))
        if (missing.length) {
            errors.push(`${missing.length} activité(s) sans désignation.`)
        }
    } else {
        const missingRef = [...activityMap.values()].filter(a => a.ref.startsWith('__auto_'))
        if (missingRef.length) {
            errors.push(
                `Référence d'activité manquante pour ${missingRef.length} activité(s). Mappez « Référence (activité) ».`
            )
        }
    }

    const activites = [...activityMap.values()].map(serializeActivity)
    return { activites, errors, warnings, analysis }
}
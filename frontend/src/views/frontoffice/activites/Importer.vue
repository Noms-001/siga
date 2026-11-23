<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { BaseButton, BaseModal } from '@/components/base'
import { listerServices } from '@/services/service'
import { confirmerImport, validerImport } from '@/services/importActivite'
import type { Service } from '@/types/backoffice/service'
import type {
    ImportCible,
    ImportResult,
    ImportValidationReport,
} from '@/types/importActivite'
import {
    buildImportTree,
    analyzeSheets,
    parseField,
    type SheetRole,
    type SheetAnalysis,
    type ImportTreeResult,
} from '@/utils/importTree'

/* ============================================================
   TYPES
   ============================================================ */
interface Range { r1: number; r2: number; c1: number; c2: number }

const CIBLE_LABELS: Record<ImportCible, string> = {
    ACTIVITES: 'Activités',
    SOUS_ACTIVITES: 'Sous-activités',
    RESULTATS_INTERMEDIAIRES: 'Résultats intermédiaires',
    INDICATEURS: 'Indicateurs',
    ORIGINES: 'Origines',
}

const CIBLE_LIST: ImportCible[] = [
    'ACTIVITES',
    'SOUS_ACTIVITES',
    'RESULTATS_INTERMEDIAIRES',
    'INDICATEURS',
    'ORIGINES',
]

interface DestinationField {
    name: string
    label: string
    mandatory: boolean
    fill: boolean
    category: string
    cibles: ImportCible[]
}

interface MappingItem {
    sheet: string
    colIndex: number
    colLetter: string
    excelName: string
    field: string
}

interface CellSel {
    confirmed: boolean
    preview: boolean
    top: boolean
    bottom: boolean
    left: boolean
    right: boolean
}

interface ImportConfig {
    version: string
    service: number | null
    procedure: number | null
    annee: number
    typeImport: 'PTA' | 'NON_PTA'
    activitesDejaValidees: boolean
    cible: ImportCible
}

type DragMode = 'cell' | 'row' | 'column' | 'shift'

/* ============================================================
   CONFIGURATION — CHAMPS MAPPABLES
   ============================================================ */
const destinationFields: DestinationField[] = [
    { name: 'designation', label: 'Désignation', mandatory: true, fill: false, category: 'Activité', cibles: ['ACTIVITES'] },
    { name: 'priorite', label: 'Priorité', mandatory: false, fill: false, category: 'Activité', cibles: ['ACTIVITES'] },
    { name: 'typeActivite', label: "Type d'activité", mandatory: false, fill: false, category: 'Activité', cibles: ['ACTIVITES'] },
    { name: 'site', label: 'Site', mandatory: false, fill: false, category: 'Activité', cibles: ['ACTIVITES'] },

    { name: 'objectifSpecifique.code', label: 'Code', mandatory: false, fill: true, category: 'Objectif spécifique', cibles: ['ACTIVITES'] },
    { name: 'objectifSpecifique.designation', label: 'Désignation', mandatory: false, fill: true, category: 'Objectif spécifique', cibles: ['ACTIVITES'] },

    { name: 'sousActivites[].designation', label: 'Désignation', mandatory: false, fill: false, category: 'Sous-activité', cibles: ['ACTIVITES', 'SOUS_ACTIVITES'] },
    { name: 'sousActivites[].dateDebut', label: 'Date début prévue', mandatory: false, fill: false, category: 'Sous-activité', cibles: ['ACTIVITES', 'SOUS_ACTIVITES'] },
    { name: 'sousActivites[].dateFin', label: 'Date fin prévue', mandatory: false, fill: false, category: 'Sous-activité', cibles: ['ACTIVITES', 'SOUS_ACTIVITES'] },

    { name: 'sousActivites[].livrables[].designation', label: 'Désignation', mandatory: false, fill: false, category: 'Livrable', cibles: ['ACTIVITES', 'SOUS_ACTIVITES'] },
    { name: 'sousActivites[].livrables[].description', label: 'Description', mandatory: false, fill: false, category: 'Livrable', cibles: ['ACTIVITES', 'SOUS_ACTIVITES'] },

    { name: 'resultatsIntermediaires[].designation', label: 'Désignation', mandatory: false, fill: false, category: 'Résultat intermédiaire', cibles: ['ACTIVITES', 'RESULTATS_INTERMEDIAIRES'] },

    { name: 'indicateurs[].designation', label: 'Désignation', mandatory: false, fill: false, category: 'Indicateur', cibles: ['ACTIVITES', 'INDICATEURS'] },
    { name: 'indicateurs[].typeIndicateur', label: "Type d'indicateur", mandatory: false, fill: false, category: 'Indicateur', cibles: ['ACTIVITES', 'INDICATEURS'] },
    { name: 'indicateurs[].uniteMesure', label: 'Unité de mesure', mandatory: false, fill: false, category: 'Indicateur', cibles: ['ACTIVITES', 'INDICATEURS'] },
    { name: 'indicateurs[].frequenceVerification', label: 'Fréquence de vérification', mandatory: false, fill: false, category: 'Indicateur', cibles: ['ACTIVITES', 'INDICATEURS'] },
    { name: 'indicateurs[].frequenceAgregation', label: "Fréquence d'agrégation", mandatory: false, fill: false, category: 'Indicateur', cibles: ['ACTIVITES', 'INDICATEURS'] },
    { name: 'indicateurs[].definition', label: 'Définition', mandatory: false, fill: false, category: 'Indicateur', cibles: ['ACTIVITES', 'INDICATEURS'] },
    { name: 'indicateurs[].methodeDetermination', label: 'Méthode de détermination', mandatory: false, fill: false, category: 'Indicateur', cibles: ['ACTIVITES', 'INDICATEURS'] },
    { name: 'indicateurs[].objectif', label: 'Objectif', mandatory: false, fill: false, category: 'Indicateur', cibles: ['ACTIVITES', 'INDICATEURS'] },
    { name: 'indicateurs[].valeurCible', label: 'Valeur cible', mandatory: false, fill: false, category: 'Indicateur', cibles: ['ACTIVITES', 'INDICATEURS'] },
    { name: 'indicateurs[].seuilMin', label: 'Seuil minimum', mandatory: false, fill: false, category: 'Indicateur', cibles: ['ACTIVITES', 'INDICATEURS'] },
    { name: 'indicateurs[].seuilMax', label: 'Seuil maximum', mandatory: false, fill: false, category: 'Indicateur', cibles: ['ACTIVITES', 'INDICATEURS'] },

    { name: 'origines[].designation', label: 'Désignation', mandatory: false, fill: false, category: 'Origine', cibles: ['ACTIVITES', 'ORIGINES'] },

    { name: '_referenceActivite', label: 'Référence (activité)', mandatory: false, fill: true, category: 'Clé de jointure', cibles: ['ACTIVITES', 'SOUS_ACTIVITES', 'RESULTATS_INTERMEDIAIRES', 'INDICATEURS', 'ORIGINES'] },
    { name: '_referenceSousActivite', label: 'Référence (sous-activité)', mandatory: false, fill: true, category: 'Clé de jointure', cibles: ['ACTIVITES'] },
]

const STEPS = ['Prévisualisation', 'Sélection', 'Correspondance', 'Configuration', 'Import']

const CATEGORY_CLASS: Record<string, string> = {
    'Activité': 'activite',
    'Objectif spécifique': 'objectif',
    'Sous-activité': 'sous-activite',
    'Livrable': 'livrable',
    'Résultat intermédiaire': 'resultat',
    'Indicateur': 'indicateur',
    'Origine': 'origine',
    'Clé de jointure': 'cle',
}

const ROLE_LABELS: Record<SheetRole, string> = {
    ACTIVITES: 'Activités',
    SOUS_ACTIVITES: 'Sous-activités',
    LIVRABLES: 'Livrables',
    RESULTATS_INTERMEDIAIRES: 'Résultats inter.',
    INDICATEURS: 'Indicateurs',
    ORIGINES: 'Origines',
}

const MAX_HISTORY = 50

/* ============================================================
   ÉTAT
   ============================================================ */
const currentStep = ref(1)
const fileName = ref('')
const workbookRef = shallowRef<any>(null)
const sheets = ref<string[]>([])
const sheetData = ref<Record<string, unknown[][]>>({})
const sheetMaxRow = ref<Record<string, number>>({})
const sheetMaxCol = ref<Record<string, number>>({})
const headerRow = ref<Record<string, number>>({})
const selectedRanges = ref<Record<string, Range[]>>({})
const activeSheet = ref(0)

const previewFullscreen = ref(false)
const selectionFullscreen = ref(false)
const showRangeModal = ref(false)

const selectionHistory = ref<Record<string, Range[]>[]>([])
const lastSelectionAnchor = ref<{ r: number; c: number } | null>(null)

const rangeInput = ref('')
const rangeInputError = ref('')
const columnsInput = ref('')
const columnsInputError = ref('')
const linesInput = ref('')
const linesInputError = ref('')

const mapping = ref<MappingItem[]>([])
const result = ref<Record<string, any>[]>([])
const resultFields = ref<string[]>([])
const errors = ref<string[]>([])
const warnings = ref<string[]>([])

/** Arbre d'import fusionné — source de vérité pour l'aperçu et le JSON final */
const treeResult = ref<ImportTreeResult | null>(null)

const isDragging = ref(false)
const dragMode = ref<DragMode | null>(null)
const dragAnchor = ref<{ r: number; c: number } | null>(null)
const dragCurrent = ref<{ r: number; c: number } | null>(null)
const dragAdd = ref(false)

const gridScrollRef = ref<HTMLDivElement | null>(null)
const gridScrollRefFs = ref<HTMLDivElement | null>(null)

const dragPointer = ref<{ x: number; y: number } | null>(null)
let autoScrollFrame: number | null = null

const isDragover = ref(false)
const showModal = ref(false)
const importing = ref(false)
const validatingImport = ref(false)
const validationReport = ref<ImportValidationReport | null>(null)
const importResult = ref<ImportResult | null>(null)

const services = ref<Service[]>([])
const loadingServices = ref(false)

const config = ref<ImportConfig>({
    version: '1.0',
    service: null,
    procedure: 1,
    annee: 2026,
    typeImport: 'PTA',
    activitesDejaValidees: false,
    cible: 'ACTIVITES',
})

/* ============================================================
   COMPUTED
   ============================================================ */
const dateDuJour = computed(() =>
    new Intl.DateTimeFormat('fr-FR', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    }).format(new Date()).toUpperCase()
)

const activeSheetName = computed(() => sheets.value[activeSheet.value] ?? '')
const currentMaxRow = computed(() => sheetMaxRow.value[activeSheetName.value] ?? 0)
const currentMaxCol = computed(() => sheetMaxCol.value[activeSheetName.value] ?? 0)

const headerRowLabel = computed(() => {
    const r = headerRow.value[activeSheetName.value] ?? -1
    return r >= 0 ? `ligne ${r + 1}` : 'aucun'
})

const currentSelectedRangeCount = computed(() =>
    (selectedRanges.value[activeSheetName.value] ?? []).length
)

const currentSelectedRowCount = computed(() =>
    getSelectedRowsForSheet(activeSheetName.value).size
)

const hasAnySelection = computed(() =>
    sheets.value.some(name => getSelectedRowsForSheet(name).size > 0)
)

const canUndo = computed(() => selectionHistory.value.length > 0)

const rangesBySheet = computed(() => {
    const out: Record<string, Range[]> = {}
    for (const name of sheets.value) {
        const ranges = selectedRanges.value[name] ?? []
        if (ranges.length) out[name] = ranges
    }
    return out
})

const selectionInfoLabel = computed(() => {
    const ranges = selectedRanges.value[activeSheetName.value] ?? []
    if (ranges.length === 0) return 'Aucune sélection'
    if (ranges.length === 1) return rangeToA1(ranges[0]!)
    return `${ranges.length} plages`
})

const selectionInfoDetail = computed(() => {
    const ranges = selectedRanges.value[activeSheetName.value] ?? []
    if (ranges.length === 0) return '—'
    if (ranges.length === 1) {
        const r = ranges[0]!
        const rows = r.r2 - r.r1 + 1
        const cols = r.c2 - r.c1 + 1
        return `${rows} ligne${rows > 1 ? 's' : ''} × ${cols} colonne${cols > 1 ? 's' : ''}`
    }
    let totalRows = 0
    let totalCols = 0
    for (const r of ranges) {
        totalRows += r.r2 - r.r1 + 1
        totalCols += r.c2 - r.c1 + 1
    }
    return `${totalRows} ligne${totalRows > 1 ? 's' : ''} × ${totalCols} colonne${totalCols > 1 ? 's' : ''} au total`
})

const mappedCount = computed(() => mapping.value.filter(m => m.field).length)

const resultSummary = computed(() => {
    if (errors.value.length) return 'Corrigez les erreurs ci-dessous'
    if (result.value.length === 0) return 'Aucune donnée à afficher'
    const label = CIBLE_LABELS[config.value.cible].toLowerCase()
    const warn = warnings.value.length > 0 ? ` · ${warnings.value.length} avertissement(s)` : ''
    return `${result.value.length} ligne(s) · ${label} · ${resultFields.value.length} colonne(s)${warn}`
})

const serviceLabel = computed(() => {
    if (config.value.service === null) return '—'
    const s = services.value.find(x => x.id === config.value.service)
    return s?.nom ?? `#${config.value.service}`
})

const cibleLabel = computed(() => CIBLE_LABELS[config.value.cible])

const uniqueReferenceCount = computed(() => {
    if (config.value.cible === 'ACTIVITES') return result.value.length
    const refs = new Set<string>()
    for (const row of result.value) {
        const ref = String(row['_referenceActivite'] ?? '').trim()
        if (ref) refs.add(ref)
    }
    return refs.size
})

/** Rôles détectés automatiquement par feuille (affichage étape 3) */
const sheetAnalysis = computed<Map<string, SheetAnalysis>>(() =>
    analyzeSheets(mapping.value.filter(m => m.field).map(m => ({ sheet: m.sheet, field: m.field })))
)

const hasWarnings = computed(() => warnings.value.length > 0)

/* ============================================================
   SÉLECTION (map de cellules)
   ============================================================ */
const selectionMap = computed<Map<string, CellSel>>(() => {
    const map = new Map<string, CellSel>()
    const sheetName = activeSheetName.value
    if (!sheetName) return map

    const ensure = (key: string): CellSel => {
        let s = map.get(key)
        if (!s) {
            s = { confirmed: false, preview: false, top: false, bottom: false, left: false, right: false }
            map.set(key, s)
        }
        return s
    }

    const markRange = (range: Range, isPreview: boolean) => {
        const { r1, r2, c1, c2 } = range
        for (let r = r1; r <= r2; r++) {
            for (let c = c1; c <= c2; c++) {
                const s = ensure(`${r}_${c}`)
                if (isPreview) s.preview = true
                else s.confirmed = true
            }
        }
    }

        ; (selectedRanges.value[sheetName] ?? []).forEach(r => markRange(r, false))

    if (isDragging.value && dragAnchor.value && dragCurrent.value) {
        markRange(normalizeRange({
            r1: dragAnchor.value.r, c1: dragAnchor.value.c,
            r2: dragCurrent.value.r, c2: dragCurrent.value.c,
        }), true)
    }

    map.forEach((sel, key) => {
        const [row, column] = key.split('_')
        const r = Number(row)
        const c = Number(column)
        const selectedAt = (row: number, col: number) => {
            const neighbor = map.get(`${row}_${col}`)
            return Boolean(neighbor && ((sel.confirmed && neighbor.confirmed) || (sel.preview && neighbor.preview)))
        }
        sel.top = !selectedAt(r - 1, c)
        sel.bottom = !selectedAt(r + 1, c)
        sel.left = !selectedAt(r, c - 1)
        sel.right = !selectedAt(r, c + 1)
    })

    return map
})

const highlightedColumns = computed<Set<number>>(() => {
    const cols = new Set<number>()
    selectionMap.value.forEach((sel, key) => {
        if (sel.confirmed || sel.preview) {
            const c = Number(key.split('_')[1])
            cols.add(c)
        }
    })
    return cols
})

const highlightedRows = computed<Set<number>>(() => {
    const rows = new Set<number>()
    selectionMap.value.forEach((sel, key) => {
        if (sel.confirmed || sel.preview) {
            const r = Number(key.split('_')[0])
            rows.add(r)
        }
    })
    return rows
})

/* ============================================================
   HELPERS
   ============================================================ */
function colName(n: number): string {
    let name = ''
    let x = n + 1
    while (x > 0) {
        const r = (x - 1) % 26
        name = String.fromCharCode(65 + r) + name
        x = Math.floor((x - 1) / 26)
    }
    return name
}

function letterToIndex(letters: string): number {
    if (!letters || !/^[A-Z]+$/i.test(letters.trim())) return -1
    let n = 0
    for (const ch of letters.trim().toUpperCase()) {
        n = n * 26 + (ch.charCodeAt(0) - 64)
    }
    return n - 1
}

function normalizeRange(range: Range): Range {
    return {
        r1: Math.min(range.r1, range.r2),
        r2: Math.max(range.r1, range.r2),
        c1: Math.min(range.c1, range.c2),
        c2: Math.max(range.c1, range.c2),
    }
}

function rangeToA1(range: Range): string {
    const a = colName(range.c1) + (range.r1 + 1)
    const b = colName(range.c2) + (range.r2 + 1)
    return a === b ? a : `${a}:${b}`
}

function rangesEqual(a: Range, b: Range): boolean {
    return a.r1 === b.r1 && a.r2 === b.r2 && a.c1 === b.c1 && a.c2 === b.c2
}

function isEmptyCell(v: any): boolean {
    return v === null || v === undefined || String(v).trim() === ''
}

function isFillPlaceholder(v: any): boolean {
    if (isEmptyCell(v)) return true
    const s = String(v).trim()
    return /^[-_–—]+$/.test(s)
}

function getActiveGridEl(): HTMLDivElement | null {
    return selectionFullscreen.value ? gridScrollRefFs.value : gridScrollRef.value
}

/* ============================================================
   HISTORIQUE SÉLECTION
   ============================================================ */
function pushSelectionHistory(): void {
    const snapshot: Record<string, Range[]> = {}
    for (const key of Object.keys(selectedRanges.value)) {
        snapshot[key] = (selectedRanges.value[key] ?? []).map(r => ({ ...r }))
    }
    selectionHistory.value.push(snapshot)
    if (selectionHistory.value.length > MAX_HISTORY) {
        selectionHistory.value.shift()
    }
}

function undoSelection(): void {
    const prev = selectionHistory.value.pop()
    if (prev) {
        selectedRanges.value = prev
    }
}

/* ============================================================
   MUTATIONS SÉLECTION
   ============================================================ */
function addRange(sheetName: string, inputRange: Range, additive: boolean): void {
    const range = normalizeRange(inputRange)
    const current = selectedRanges.value[sheetName] ?? []

    if (!additive) {
        pushSelectionHistory()
        selectedRanges.value = { ...selectedRanges.value, [sheetName]: [range] }
        return
    }

    const exists = current.some(r => rangesEqual(r, range))
    if (exists) return

    pushSelectionHistory()
    selectedRanges.value = { ...selectedRanges.value, [sheetName]: [...current, range] }
}

function removeRange(sheetName: string, index: number): void {
    const ranges = [...(selectedRanges.value[sheetName] ?? [])]
    ranges.splice(index, 1)
    pushSelectionHistory()
    selectedRanges.value = { ...selectedRanges.value, [sheetName]: ranges }
}

function clearAllSelections(): void {
    const next: Record<string, Range[]> = {}
    for (const name of sheets.value) next[name] = []
    pushSelectionHistory()
    selectedRanges.value = next
}

function clearActiveSheetSelection(): void {
    pushSelectionHistory()
    selectedRanges.value = { ...selectedRanges.value, [activeSheetName.value]: [] }
}

function selectAllInSheet(): void {
    const sheetName = activeSheetName.value
    if (!sheetName) return
    const maxR = sheetMaxRow.value[sheetName] ?? 0
    const maxC = sheetMaxCol.value[sheetName] ?? 0
    if (maxR === 0 || maxC === 0) return
    addRange(sheetName, {
        r1: 0, r2: Math.max(0, maxR - 1),
        c1: 0, c2: Math.max(0, maxC - 1),
    }, false)
}

/* ============================================================
   PARSEURS DE PLAGES / COLONNES / LIGNES
   ============================================================ */
function parseA1(spec: string): Range | null {
    const m = spec.trim().match(/^([A-Z]+)(\d+)(?::([A-Z]+)(\d+))?$/i)
    if (!m) return null
    const c1 = letterToIndex(m[1]!)
    const r1 = Number(m[2]!) - 1
    const c2 = m[3] ? letterToIndex(m[3]) : c1
    const r2 = m[4] ? Number(m[4]!) - 1 : r1
    if (c1 < 0 || c2 < 0 || r1 < 0 || r2 < 0) return null
    return normalizeRange({ r1, c1, r2, c2 })
}

function parseColumnsSpec(spec: string): number[] {
    const result: number[] = []
    const parts = spec.split(',').map(s => s.trim()).filter(Boolean)
    for (const part of parts) {
        const dashIdx = part.indexOf('-')
        if (dashIdx > 0) {
            const from = letterToIndex(part.slice(0, dashIdx).trim())
            const to = letterToIndex(part.slice(dashIdx + 1).trim())
            if (from < 0 || to < 0) return []
            const lo = Math.min(from, to)
            const hi = Math.max(from, to)
            for (let i = lo; i <= hi; i++) result.push(i)
        } else {
            const idx = letterToIndex(part)
            if (idx < 0) return []
            result.push(idx)
        }
    }
    return result
}

function parseLinesSpec(spec: string): number[] {
    const result: number[] = []
    const parts = spec.split(',').map(s => s.trim()).filter(Boolean)
    for (const part of parts) {
        const dashIdx = part.indexOf('-')
        if (dashIdx > 0) {
            const from = Number(part.slice(0, dashIdx).trim())
            const to = Number(part.slice(dashIdx + 1).trim())
            if (isNaN(from) || isNaN(to)) return []
            const lo = Math.min(from, to)
            const hi = Math.max(from, to)
            for (let i = lo; i <= hi; i++) result.push(i - 1)
        } else {
            const n = Number(part)
            if (isNaN(n)) return []
            result.push(n - 1)
        }
    }
    return result
}

/* ============================================================
   APPLICATION DES INPUTS
   ============================================================ */
function applyRangeInput(additive: boolean): void {
    rangeInputError.value = ''
    const spec = rangeInput.value.trim()
    if (!spec) { rangeInputError.value = 'Plage vide'; return }
    const parsed = parseA1(spec)
    if (!parsed) { rangeInputError.value = 'Format invalide (ex: A1:E10)'; return }
    const sheetName = activeSheetName.value
    if (!sheetName) { rangeInputError.value = 'Aucune feuille active'; return }
    addRange(sheetName, parsed, additive)
    rangeInput.value = ''
}

function applyColumnsInput(additive: boolean): void {
    columnsInputError.value = ''
    const spec = columnsInput.value.trim()
    if (!spec) { columnsInputError.value = 'Colonnes vides'; return }
    const cols = parseColumnsSpec(spec)
    if (!cols.length) { columnsInputError.value = 'Format invalide (ex: A,B,C ou A-D)'; return }
    const sheetName = activeSheetName.value
    if (!sheetName) { columnsInputError.value = 'Aucune feuille active'; return }
    const maxR = sheetMaxRow.value[sheetName] ?? 0
    const hr = headerRow.value[sheetName] ?? -1
    const firstRow = hr >= 0 ? Math.max(0, hr + 1) : 0
    const lastRow = Math.max(firstRow, maxR - 1)

    if (!additive) {
        pushSelectionHistory()
        const next: Range[] = cols.map(c => ({ r1: firstRow, r2: lastRow, c1: c, c2: c }))
        selectedRanges.value = { ...selectedRanges.value, [sheetName]: next }
    } else {
        const current = [...(selectedRanges.value[sheetName] ?? [])]
        pushSelectionHistory()
        for (const c of cols) {
            const r: Range = { r1: firstRow, r2: lastRow, c1: c, c2: c }
            if (!current.some(x => rangesEqual(x, r))) current.push(r)
        }
        selectedRanges.value = { ...selectedRanges.value, [sheetName]: current }
    }
    columnsInput.value = ''
}

function applyLinesInput(additive: boolean): void {
    linesInputError.value = ''
    const spec = linesInput.value.trim()
    if (!spec) { linesInputError.value = 'Lignes vides'; return }
    const lines = parseLinesSpec(spec)
    if (!lines.length) { linesInputError.value = 'Format invalide (ex: 1-10 ou 1,3,5)'; return }
    const sheetName = activeSheetName.value
    if (!sheetName) { linesInputError.value = 'Aucune feuille active'; return }
    const maxC = sheetMaxCol.value[sheetName] ?? 0
    const hr = headerRow.value[sheetName] ?? -1

    const validLines = lines.filter(r => r !== hr && r >= 0 && r < (sheetMaxRow.value[sheetName] ?? 0))

    if (!additive) {
        pushSelectionHistory()
        const next: Range[] = validLines.map(r => ({ r1: r, r2: r, c1: 0, c2: maxC - 1 }))
        selectedRanges.value = { ...selectedRanges.value, [sheetName]: next }
    } else {
        const current = [...(selectedRanges.value[sheetName] ?? [])]
        pushSelectionHistory()
        for (const r of validLines) {
            const range: Range = { r1: r, r2: r, c1: 0, c2: maxC - 1 }
            if (!current.some(x => rangesEqual(x, range))) current.push(range)
        }
        selectedRanges.value = { ...selectedRanges.value, [sheetName]: current }
    }
    linesInput.value = ''
}

/* ============================================================
   CELLULES / RENDU GRILLE
   ============================================================ */
function cellValue(r: number, c: number): string {
    const data = sheetData.value[activeSheetName.value]
    if (!data) return ''
    const v = data[r]?.[c]
    return v === undefined || v === null ? '' : String(v)
}

function cellClass(r: number, c: number): string {
    const s = selectionMap.value.get(`${r}_${c}`)
    if (!s) return 'grid-cell'
    const classes = ['grid-cell']
    if (s.confirmed) classes.push('grid-cell--selected')
    if (s.preview) classes.push('grid-cell--preview')
    return classes.join(' ')
}

function cellSelectionStyle(r: number, c: number): Record<string, string> | undefined {
    const s = selectionMap.value.get(`${r}_${c}`)
    if (!s) return undefined
    const color = s.preview && !s.confirmed ? '#93c5fd' : '#4a6cf7'
    const parts: string[] = []
    if (s.top) parts.push(`inset 0 2px 0 0 ${color}`)
    if (s.bottom) parts.push(`inset 0 -2px 0 0 ${color}`)
    if (s.left) parts.push(`inset 2px 0 0 0 ${color}`)
    if (s.right) parts.push(`inset -2px 0 0 0 ${color}`)
    if (parts.length === 0) return undefined
    return { boxShadow: parts.join(', ') }
}

function rangeLabel(sheetName: string, range: Range): string {
    const fullRows = range.c1 === 0 && range.c2 === (sheetMaxCol.value[sheetName] ?? 1) - 1
    return fullRows ? `Lignes ${range.r1 + 1}-${range.r2 + 1}` : rangeToA1(range)
}

function getFieldConfig(name: string): DestinationField | null {
    return destinationFields.find(f => f.name === name) ?? null
}

function fieldLabel(fieldName: string): string {
    if (fieldName === '_sousActivitesSummary') return 'Sous-activités'
    if (fieldName === '_referenceActivite') return 'Réf. activité'
    if (fieldName === '_referenceSousActivite') return 'Réf. SA'
    const cfg = getFieldConfig(fieldName)
    if (!cfg) return fieldName
    if (!cfg.name.includes('.')) return cfg.label
    return `${cfg.category} — ${cfg.label}`
}

function categoryClass(fieldName: string): string {
    const cfg = getFieldConfig(fieldName)
    if (!cfg) return ''
    return CATEGORY_CLASS[cfg.category] || 'default'
}

function fieldPath(fieldName: string): string {
    if (fieldName === '_referenceActivite') return 'clé de jointure · activité'
    if (fieldName === '_referenceSousActivite') return 'clé de jointure · sous-activité'
    if (!fieldName.includes('.')) return ''
    return fieldName
        .replace(/\[\]\./g, ' › ')
        .replace(/\[\]/g, '')
        .replace(/\./g, ' › ')
}

/* ============================================================
   GROUPES DE CHAMPS POUR LE SELECT
   ============================================================ */
function availableFieldsFor(index: number): DestinationField[] {
    const item = mapping.value[index]
    if (!item) return destinationFields
    const applicable = destinationFields.filter(f => f.cibles.includes(config.value.cible))
    const used = new Set(
        mapping.value
            .filter((m, i) => i !== index && m.sheet === item.sheet && m.field)
            .map(m => m.field)
    )
    return applicable.filter(f => !used.has(f.name))
}

function onCibleChange(): void {
    const applicable = new Set(
        destinationFields.filter(f => f.cibles.includes(config.value.cible)).map(f => f.name)
    )
    const willClear = mapping.value.filter(m => m.field && !applicable.has(m.field))
    if (willClear.length > 0) {
        const ok = window.confirm(
            `Changer de cible va retirer ${willClear.length} mapping(s) incompatible(s). Continuer ?`
        )
        if (!ok) return
    }
    mapping.value = mapping.value.map(m => ({
        ...m,
        field: m.field && !applicable.has(m.field) ? '' : m.field,
    }))
    // Invalider l'arbre : le changement de cible peut changer la structure
    treeResult.value = null
    warnings.value = []
    result.value = []
    resultFields.value = []
}

interface FieldGroup { category: string; fields: DestinationField[] }

function groupedOptions(index: number): FieldGroup[] {
    const available = availableFieldsFor(index)
    const groups = new Map<string, DestinationField[]>()
    for (const f of available) {
        if (!groups.has(f.category)) groups.set(f.category, [])
        groups.get(f.category)!.push(f)
    }
    return Array.from(groups.entries()).map(([category, fields]) => ({ category, fields }))
}

/* ============================================================
   CHARGEMENT FICHIER
   ============================================================ */
async function getXLSX(): Promise<any> {
    const w = window as any
    if (w.XLSX) return w.XLSX
    const mod = await import('xlsx')
    return mod.default || mod
}

async function handleFile(file: File): Promise<void> {
    if (!file) return
    if (!/\.(xlsx|xls|csv)$/i.test(file.name)) {
        errors.value = ['Format non supporté. Choisissez un fichier .xlsx, .xls ou .csv.']
        return
    }
    try {
        const XLSX = await getXLSX()
        const buffer = await file.arrayBuffer()
        const wb = XLSX.read(new Uint8Array(buffer), { type: 'array', cellDates: true })
        if (!wb.SheetNames.length) {
            errors.value = ['Le classeur ne contient aucune feuille.']
            return
        }
        workbookRef.value = wb
        sheets.value = wb.SheetNames.slice()
        activeSheet.value = 0

        const data: Record<string, unknown[][]> = {}
        const maxR: Record<string, number> = {}
        const maxC: Record<string, number> = {}
        const hdr: Record<string, number> = {}
        const sel: Record<string, Range[]> = {}

        for (const sheetName of wb.SheetNames) {
            const ws = wb.Sheets[sheetName]
            const rows: any[][] = XLSX.utils.sheet_to_json(ws, {
                header: 1, defval: '', raw: true, blankrows: true, dateNF: 'yyyy-mm-dd',
            })
            const mR = Math.max(rows.length, 1)
            const mC = Math.max(1, ...rows.map(r => Array.isArray(r) ? r.length : 0))
            const normalized: unknown[][] = []
            for (let i = 0; i < mR; i++) {
                const src = rows[i] || []
                const out: unknown[] = []
                for (let j = 0; j < mC; j++) {
                    const v = src[j]
                    out.push(v === undefined || v === null ? '' : v)
                }
                normalized.push(out)
            }
            data[sheetName] = normalized
            maxR[sheetName] = mR
            maxC[sheetName] = mC
            hdr[sheetName] = 0
            sel[sheetName] = []
        }

        sheetData.value = data
        sheetMaxRow.value = maxR
        sheetMaxCol.value = maxC
        headerRow.value = hdr
        selectedRanges.value = sel
        selectionHistory.value = []
        lastSelectionAnchor.value = null
        mapping.value = []
        result.value = []
        resultFields.value = []
        errors.value = []
        warnings.value = []
        treeResult.value = null
        fileName.value = file.name
        currentStep.value = 1
    } catch (e: any) {
        console.error(e)
        errors.value = ['Erreur lors de la lecture : ' + (e?.message ?? e)]
    }
}

async function loadServices(): Promise<void> {
    loadingServices.value = true
    try {
        const res = await listerServices({ inclureInactifs: false })
        if (res.success) services.value = res.data
    } finally {
        loadingServices.value = false
    }
}

function onFileChange(e: Event): void {
    const f = (e.target as HTMLInputElement).files?.[0]
    if (f) void handleFile(f)
}

function onDrop(e: DragEvent): void {
    isDragover.value = false
    const f = e.dataTransfer?.files?.[0]
    if (f) void handleFile(f)
}

/* ============================================================
   ÉTAPE 1 — EN-TÊTE
   ============================================================ */
function setHeaderRow(r: number): void {
    headerRow.value = { ...headerRow.value, [activeSheetName.value]: r }
}

function clearHeader(): void {
    headerRow.value = { ...headerRow.value, [activeSheetName.value]: -1 }
}

/* ============================================================
   PLEIN ÉCRAN — PRÉVISUALISATION (étape 1)
   ============================================================ */
function ouvrirPleinEcranPreview(): void {
    previewFullscreen.value = true
    document.body.style.overflow = 'hidden'
}

function fermerPleinEcranPreview(): void {
    previewFullscreen.value = false
    if (!selectionFullscreen.value) {
        document.body.style.overflow = ''
    }
}

/* ============================================================
   PLEIN ÉCRAN — SÉLECTION (étape 2)
   ============================================================ */
function ouvrirPleinEcranSelection(): void {
    selectionFullscreen.value = true
    document.body.style.overflow = 'hidden'
}

function fermerPleinEcranSelection(): void {
    selectionFullscreen.value = false
    showRangeModal.value = false
    if (!previewFullscreen.value) {
        document.body.style.overflow = ''
    }
}

function ouvrirRangeModal(): void {
    showRangeModal.value = true
}

function fermerRangeModal(): void {
    showRangeModal.value = false
}

function suivantDepuisPleinEcran(): void {
    if (!hasAnySelection.value) return
    fermerPleinEcranSelection()
    goToStep(3)
}

/* ============================================================
   STEPPER
   ============================================================ */
function onStepClick(stepIndex: number): void {
    const target = stepIndex + 1
    if (target >= currentStep.value) return
    if (!workbookRef.value) return
    if (currentStep.value === 2) fermerPleinEcranSelection()
    currentStep.value = target
}

/* ============================================================
   ÉTAPE 2 — SÉLECTION (drag)
   ============================================================ */
function onGridMouseDown(e: MouseEvent): void {
    if (e.button !== 0) return
    const target = e.target as HTMLElement | null
    if (!target) return

    const rowHeader = target.closest('.grid-row-head--selectable') as HTMLElement | null
    const columnHeader = target.closest('.grid-col-head--selectable') as HTMLElement | null
    const cell = target.closest('td[data-row]') as HTMLElement | null

    let anchor: { r: number; c: number }
    let mode: DragMode

    if (e.shiftKey && lastSelectionAnchor.value && cell) {
        anchor = lastSelectionAnchor.value
        mode = 'shift'
    } else if (rowHeader) {
        anchor = { r: Number(rowHeader.dataset.row), c: 0 }
        mode = 'row'
    } else if (columnHeader) {
        anchor = { r: 0, c: Number(columnHeader.dataset.col) }
        mode = 'column'
    } else if (cell) {
        anchor = { r: Number(cell.dataset.row), c: Number(cell.dataset.col) }
        mode = 'cell'
    } else {
        return
    }

    isDragging.value = true
    dragMode.value = mode
    dragAnchor.value = anchor

    if (mode === 'row') {
        dragCurrent.value = { r: anchor.r, c: currentMaxCol.value - 1 }
    } else if (mode === 'column') {
        dragCurrent.value = { r: currentMaxRow.value - 1, c: anchor.c }
    } else if (mode === 'shift' && cell) {
        dragCurrent.value = { r: Number(cell.dataset.row), c: Number(cell.dataset.col) }
    } else {
        dragCurrent.value = { ...anchor }
    }

    dragAdd.value = e.ctrlKey || e.metaKey
    dragPointer.value = { x: e.clientX, y: e.clientY }
    startAutoScroll()
    e.preventDefault()
}

function updateDragFromPointer(x: number, y: number): void {
    if (!isDragging.value || !dragAnchor.value || !dragMode.value) return
    const grid = getActiveGridEl()
    if (!grid) return
    const rect = grid.getBoundingClientRect()
    const hitX = Math.max(rect.left + 48, Math.min(x, rect.right - 2))
    const hitY = Math.max(rect.top + 32, Math.min(y, rect.bottom - 2))
    const hit = document.elementFromPoint(hitX, hitY) as HTMLElement | null
    const cell = hit?.closest('td[data-row]') as HTMLElement | null
    if (!cell) return

    const row = Number(cell.dataset.row)
    const col = Number(cell.dataset.col)
    if (dragMode.value === 'cell' || dragMode.value === 'shift') {
        dragCurrent.value = { r: row, c: col }
    } else if (dragMode.value === 'row') {
        dragCurrent.value = { r: row, c: currentMaxCol.value - 1 }
    } else {
        dragCurrent.value = { r: currentMaxRow.value - 1, c: col }
    }
}

function autoScrollTick(): void {
    autoScrollFrame = null
    if (!isDragging.value || !dragPointer.value) return
    const grid = getActiveGridEl()
    if (!grid) return
    const rect = grid.getBoundingClientRect()
    const edge = 42
    const maxSpeed = 18
    const speedForDistance = (d: number) => Math.min(maxSpeed, Math.max(4, (edge - d) * 0.45))
    const { x, y } = dragPointer.value
    if (y < rect.top + edge && grid.scrollTop > 0) {
        grid.scrollTop -= speedForDistance(Math.max(0, y - rect.top))
    } else if (y > rect.bottom - edge && grid.scrollTop < grid.scrollHeight - grid.clientHeight) {
        grid.scrollTop += speedForDistance(Math.max(0, rect.bottom - y))
    }
    if (x < rect.left + edge && grid.scrollLeft > 0) {
        grid.scrollLeft -= speedForDistance(Math.max(0, x - rect.left))
    } else if (x > rect.right - edge && grid.scrollLeft < grid.scrollWidth - grid.clientWidth) {
        grid.scrollLeft += speedForDistance(Math.max(0, rect.right - x))
    }
    updateDragFromPointer(x, y)
    autoScrollFrame = requestAnimationFrame(autoScrollTick)
}

function startAutoScroll(): void {
    if (autoScrollFrame === null) autoScrollFrame = requestAnimationFrame(autoScrollTick)
}

function stopAutoScroll(): void {
    if (autoScrollFrame !== null) cancelAnimationFrame(autoScrollFrame)
    autoScrollFrame = null
}

function onGlobalMouseMove(e: MouseEvent): void {
    if (!isDragging.value) return
    dragPointer.value = { x: e.clientX, y: e.clientY }
    updateDragFromPointer(e.clientX, e.clientY)
}

function onGlobalMouseUp(): void {
    if (!isDragging.value) return
    stopAutoScroll()
    if (dragAnchor.value && dragCurrent.value) {
        const sheetName = activeSheetName.value
        if (sheetName) {
            addRange(sheetName, {
                r1: dragAnchor.value.r, c1: dragAnchor.value.c,
                r2: dragCurrent.value.r, c2: dragCurrent.value.c,
            }, dragAdd.value)
        }
        if (dragMode.value !== 'shift') {
            lastSelectionAnchor.value = dragAnchor.value
        }
    }
    isDragging.value = false
    dragMode.value = null
    dragAnchor.value = null
    dragCurrent.value = null
    dragAdd.value = false
    dragPointer.value = null
}

/* ============================================================
   RACCOURCIS CLAVIER
   ============================================================ */
function onGlobalKeyDown(e: KeyboardEvent): void {
    if (e.key === 'Escape') {
        if (showRangeModal.value) {
            e.preventDefault()
            showRangeModal.value = false
            return
        }
        if (selectionFullscreen.value) {
            e.preventDefault()
            fermerPleinEcranSelection()
            return
        }
        if (previewFullscreen.value) {
            e.preventDefault()
            fermerPleinEcranPreview()
            return
        }
    }

    const target = e.target as HTMLElement | null
    const tag = target?.tagName
    if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return

    const mod = e.ctrlKey || e.metaKey
    if (!mod) return

    if ((e.key === 'a' || e.key === 'A') && currentStep.value === 2) {
        e.preventDefault()
        selectAllInSheet()
        return
    }
    if ((e.key === 'z' || e.key === 'Z') && currentStep.value === 2) {
        if (selectionHistory.value.length > 0) {
            e.preventDefault()
            undoSelection()
        }
        return
    }
}

function getSelectedRowsForSheet(sheetName: string): Set<number> {
    const rows = new Set<number>()
        ; (selectedRanges.value[sheetName] ?? []).forEach(range => {
            for (let r = range.r1; r <= range.r2; r++) rows.add(r)
        })
    const hr = headerRow.value[sheetName]
    if (hr !== undefined && hr >= 0) rows.delete(hr)
    return rows
}

/* ============================================================
   ÉTAPE 3 — MAPPING
   ============================================================ */
function buildMapping(): MappingItem[] {
    const items: MappingItem[] = []
    for (const sheetName of sheets.value) {
        const ranges = selectedRanges.value[sheetName] ?? []
        if (!ranges.length) continue
        const colSet = new Set<number>()
        const rowSet = new Set<number>()
        ranges.forEach(r => {
            for (let i = r.r1; i <= r.r2; i++) rowSet.add(i)
            for (let j = r.c1; j <= r.c2; j++) colSet.add(j)
        })
        const hr = headerRow.value[sheetName] ?? -1
        if (hr >= 0) rowSet.delete(hr)
        if (!rowSet.size || !colSet.size) continue
        Array.from(colSet).sort((a, b) => a - b).forEach(colIndex => {
            let excelName = ''
            if (hr >= 0) {
                const v = sheetData.value[sheetName]?.[hr]?.[colIndex]
                excelName = v === undefined || v === null ? '' : String(v).trim()
            }
            if (!excelName) excelName = `Colonne ${colName(colIndex)}`
            items.push({ sheet: sheetName, colIndex, colLetter: colName(colIndex), excelName, field: '' })
        })
    }
    return items
}

function goToStep(n: number): void {
    if (n === 3) {
        const fresh = buildMapping()
        const previous = new Map(mapping.value.map(m => [`${m.sheet}|${m.colIndex}`, m]))
        fresh.forEach(item => {
            const old = previous.get(`${item.sheet}|${item.colIndex}`)
            if (old) item.field = old.field
        })
        mapping.value = fresh
        // Retour au mapping = invalidation de l'arbre
        treeResult.value = null
        warnings.value = []
    }
    currentStep.value = n
}

function onMappingChange(): void {
    mapping.value = [...mapping.value]
    // Invalider l'arbre si on revient modifier le mapping (sécurité)
    if (treeResult.value) {
        treeResult.value = null
        warnings.value = []
    }
}

function moveMapping(index: number, delta: number): void {
    const target = index + delta
    if (target < 0 || target >= mapping.value.length) return
    const arr = [...mapping.value]
    const tmp = arr[index]!
    arr[index] = arr[target]!
    arr[target] = tmp
    mapping.value = arr
}

/* ============================================================
   FILL + LECTURE COLONNE
   ============================================================ */
function getNonEmptySelectedRows(sheetName: string): number[] {
    const data = sheetData.value[sheetName] ?? []
    const ranges = selectedRanges.value[sheetName] ?? []
    const hr = headerRow.value[sheetName] ?? -1
    const rowSet = new Set<number>()
    ranges.forEach(r => {
        for (let i = r.r1; i <= r.r2; i++) if (i !== hr) rowSet.add(i)
    })
    return Array.from(rowSet).sort((a, b) => a - b).filter(row =>
        ranges.some(r => {
            if (row < r.r1 || row > r.r2) return false
            for (let c = r.c1; c <= r.c2; c++) {
                if (!isEmptyCell(data[row]?.[c])) return true
            }
            return false
        })
    )
}

function getColumnValues(sheetName: string, colIndex: number, fill: boolean): {
    rows: number[]; values: unknown[]
} {
    const data = sheetData.value[sheetName] ?? []
    const ranges = selectedRanges.value[sheetName] ?? []
    const rows = getNonEmptySelectedRows(sheetName)
    let previous: unknown = ''
    const values = rows.map(row => {
        const selected = ranges.some(r =>
            row >= r.r1 && row <= r.r2 && colIndex >= r.c1 && colIndex <= r.c2
        )
        if (!selected) return ''
        let v = data[row]?.[colIndex]
        if (v === undefined || v === null) v = ''
        if (fill && isFillPlaceholder(v) && !isFillPlaceholder(previous)) v = previous
        if (!isFillPlaceholder(v)) previous = v
        return v
    })
    return { rows, values }
}

function previewForMapping(item: MappingItem): string {
    const cfg = getFieldConfig(item.field)
    const preview = getColumnValues(item.sheet, item.colIndex, cfg?.fill ?? false).values.slice(0, 3)
    return preview.map(v => isEmptyCell(v) ? '∅' : String(v)).join(' · ') || '—'
}

/* ============================================================
   VALIDATION MAPPING — Fusion générique via importTree
   ============================================================ */
function validateMapping(): void {
    errors.value = []
    warnings.value = []
    treeResult.value = null

    const mapped = mapping.value.filter(m => m.field)
    if (!mapped.length) { errors.value = ['Veuillez mapper au moins une colonne.']; return }

    // 1. Champs obligatoires mappés
    const applicableFields = destinationFields.filter(f => f.cibles.includes(config.value.cible))
    const missingMandatory = applicableFields.filter(f => f.mandatory)
        .filter(f => !mapped.some(m => m.field === f.name))
    if (missingMandatory.length) {
        errors.value = missingMandatory.map(f => `Le champ « ${fieldLabel(f.name)} » est obligatoire.`)
        return
    }

    // 2. Doublons
    const seen = new Set<string>()
    for (const item of mapped) {
        const key = `${item.sheet}|${item.field}`
        if (seen.has(key)) {
            errors.value.push(`Champ « ${fieldLabel(item.field)} » mappé deux fois (feuille ${item.sheet}).`)
        }
        seen.add(key)
    }
    if (errors.value.length) return

    // 3. Construction des rowsBySheet (valeurs brutes, l'arbre gère les dates)
    const bySheet = new Map<string, MappingItem[]>()
    mapped.forEach(m => {
        if (!bySheet.has(m.sheet)) bySheet.set(m.sheet, [])
        bySheet.get(m.sheet)!.push(m)
    })

    const rowsBySheet = new Map<string, Record<string, any>[]>()
    const dataErrors: string[] = []

    for (const [sheetName, sheetMapping] of bySheet.entries()) {
        const columnData: Record<string, { rows: number[]; values: unknown[] }> = {}
        sheetMapping.forEach(m => {
            const cfg = getFieldConfig(m.field)
            columnData[m.field] = getColumnValues(sheetName, m.colIndex, cfg?.fill ?? false)
        })
        sheetMapping.forEach(m => {
            const cfg = getFieldConfig(m.field)
            if (cfg?.mandatory) {
                columnData[m.field]!.values.forEach((v, i) => {
                    if (isEmptyCell(v)) {
                        dataErrors.push(`Champ « ${fieldLabel(m.field)} » vide (feuille ${sheetName}, ligne ${columnData[m.field]!.rows[i]! + 1}).`)
                    }
                })
            }
        })
        const firstField = sheetMapping[0]!.field
        const rowCount = columnData[firstField]!.values.length
        const sheetRows: Record<string, any>[] = []
        for (let i = 0; i < rowCount; i++) {
            const out: Record<string, any> = {}
            let hasValue = false
            sheetMapping.forEach(m => {
                const source = columnData[m.field]!.values[i] ?? ''
                out[m.field] = source
                if (!isEmptyCell(source)) hasValue = true
            })
            if (hasValue) sheetRows.push(out)
        }
        rowsBySheet.set(sheetName, sheetRows)
    }
    if (dataErrors.length) { errors.value = dataErrors; return }

    // 4. Fusion générique — TOUTE la logique métier est dans importTree
    const tree = buildImportTree({
        cible: config.value.cible,
        mapped: mapped.map(m => ({ sheet: m.sheet, field: m.field })),
        rowsBySheet,
    })
    treeResult.value = tree
    warnings.value = tree.warnings

    if (tree.errors.length) { errors.value = tree.errors; return }

    // 5. Préparation de l'aperçu
    preparePreview(tree, rowsBySheet, mapped)
    currentStep.value = 4
}

/**
 * Prépare result/resultFields à partir de l'arbre fusionné.
 * - ACTIVITES : aperçu par activité avec résumé des sous-activités
 * - Autres    : aperçu plat par entité enfant rattachée à son activité
 */
function preparePreview(
    tree: ImportTreeResult,
    rowsBySheet: Map<string, Record<string, any>[]>,
    mapped: MappingItem[],
): void {
    if (config.value.cible === 'ACTIVITES') {
        const rows = tree.activites.map(a => {
            const sas = (a.sousActivites as any[]) ?? []
            return {
                ...a,
                _sousActivitesSummary: sas.length === 0 ? '—' : sas.map((s: any) => s.designation || '?').join(', '),
            }
        })
        const fields: string[] = []
        const seen = new Set<string>()
        for (const m of mapped) {
            if (m.field.startsWith('_')) continue
            const p = parseField(m.field)
            if (p.level1 === 'sousActivites') continue
            if (p.level1 === 'resultatsIntermediaires') continue
            if (p.level1 === 'indicateurs') continue
            if (p.level1 === 'origines') continue
            if (seen.has(m.field)) continue
            seen.add(m.field)
            fields.push(m.field)
        }
        fields.push('_sousActivitesSummary')
        result.value = rows
        resultFields.value = fields
        return
    }

    // Non-ACTIVITES : aplatir les collections de l'arbre en lignes d'aperçu
    const collKey = ({
        SOUS_ACTIVITES: 'sousActivites',
        RESULTATS_INTERMEDIAIRES: 'resultatsIntermediaires',
        INDICATEURS: 'indicateurs',
        ORIGINES: 'origines',
    } as const)[config.value.cible as Exclude<ImportCible, 'ACTIVITES'>]

    const previewRows: Record<string, any>[] = []
    for (const a of tree.activites) {
        const items = (a[collKey] as any[]) ?? []
        for (const item of items) {
            previewRows.push({ _referenceActivite: a._referenceActivite, ...item })
        }
    }

    if (previewRows.length === 0) {
        // Fallback : lignes brutes de la première feuille
        const sheetName = [...rowsBySheet.keys()][0]
        result.value = sheetName ? (rowsBySheet.get(sheetName) ?? []) : []
    } else {
        result.value = previewRows
    }
    resultFields.value = [...new Set(mapped.map(m => m.field))]
}

/* ============================================================
   ÉTAPE 4 — CONFIGURATION
   ============================================================ */
async function goToImportStep(): Promise<void> {
    const errs: string[] = []
    if (!config.value.version?.trim()) errs.push('La version est obligatoire.')

    if (config.value.cible === 'ACTIVITES') {
        if (config.value.service === null) errs.push('Le service est obligatoire.')
        if (config.value.procedure === null) errs.push('La procédure est obligatoire.')
        if (!config.value.annee || config.value.annee < 1900 || config.value.annee > 2100) {
            errs.push("L'année doit être comprise entre 1900 et 2100.")
        }
    }

    if (config.value.cible === 'SOUS_ACTIVITES') {
        if (!config.value.annee || config.value.annee < 1900 || config.value.annee > 2100) {
            errs.push("L'année doit être comprise entre 1900 et 2100.")
        }
    }

    errors.value = errs
    if (errs.length) return

    if (!treeResult.value) {
        errors.value = ["L'arbre d'import est introuvable — revenez à l'étape Correspondance."]
        return
    }

    validatingImport.value = true
    validationReport.value = null
    importResult.value = null
    try {
        const response = await validerImport(buildFinalJSON(), config.value.cible)
        if (!response.success) { errors.value = [response.error]; return }
        validationReport.value = response.data
        currentStep.value = 5
    } catch (error) {
        errors.value = [error instanceof Error ? error.message : "Impossible de contacter l'API d'import."]
    } finally {
        validatingImport.value = false
    }
}

/* ============================================================
   CONSTRUCTION DU JSON FINAL — depuis l'arbre fusionné
   ============================================================ */
function buildFinalJSON(): any {
    const tree = treeResult.value
    if (!tree) throw new Error("Aucun arbre d'import — relancez la validation.")

    switch (config.value.cible) {
        case 'ACTIVITES':
            return {
                version: config.value.version.trim(),
                service: config.value.service!,
                procedure: config.value.procedure!,
                annee: config.value.annee,
                typeImport: config.value.typeImport,
                activitesDejaValidees: config.value.activitesDejaValidees,
                activites: tree.activites.map(a => {
                    const out: Record<string, any> = {}
                    for (const [k, v] of Object.entries(a)) {
                        if (k === '_referenceActivite') continue
                        if (k === '_sousActivitesSummary') continue
                        out[k] = v
                    }
                    return out
                }),
            }
        case 'SOUS_ACTIVITES':
            return {
                version: config.value.version.trim(),
                annee: config.value.annee,
                activites: tree.activites
                    .filter(a => a._referenceActivite && (a.sousActivites?.length ?? 0) > 0)
                    .map(a => ({
                        reference: a._referenceActivite,
                        sousActivites: a.sousActivites,
                    })),
            }
        case 'RESULTATS_INTERMEDIAIRES':
            return {
                version: config.value.version.trim(),
                activites: tree.activites
                    .filter(a => a._referenceActivite && (a.resultatsIntermediaires?.length ?? 0) > 0)
                    .map(a => ({
                        reference: a._referenceActivite,
                        resultatsIntermediaires: a.resultatsIntermediaires,
                    })),
            }
        case 'INDICATEURS':
            return {
                version: config.value.version.trim(),
                activites: tree.activites
                    .filter(a => a._referenceActivite && (a.indicateurs?.length ?? 0) > 0)
                    .map(a => ({
                        reference: a._referenceActivite,
                        indicateurs: a.indicateurs,
                    })),
            }
        case 'ORIGINES':
            return {
                version: config.value.version.trim(),
                activites: tree.activites
                    .filter(a => a._referenceActivite && (a.origines?.length ?? 0) > 0)
                    .map(a => ({
                        reference: a._referenceActivite,
                        origines: a.origines,
                    })),
            }
    }
}

/* ============================================================
   MODAL D'IMPORT
   ============================================================ */
function openImportModal(): void {
    if (result.value.length === 0 || errors.value.length > 0 || !validationReport.value?.valide) return
    showModal.value = true
}

async function confirmImport(): Promise<void> {
    if (!validationReport.value?.valide) return
    importing.value = true
    errors.value = []
    try {
        const response = await confirmerImport(buildFinalJSON(), config.value.cible)
        if (!response.success) { errors.value = [response.error]; showModal.value = false; return }
        importResult.value = response.data
        showModal.value = false
    } catch (error) {
        errors.value = [error instanceof Error ? error.message : "Impossible de contacter l'API d'import."]
        showModal.value = false
    } finally {
        importing.value = false
    }
}

function resetAll(): void {
    previewFullscreen.value = false
    selectionFullscreen.value = false
    showRangeModal.value = false
    document.body.style.overflow = ''
    workbookRef.value = null
    sheets.value = []
    sheetData.value = {}
    sheetMaxRow.value = {}
    sheetMaxCol.value = {}
    headerRow.value = {}
    selectedRanges.value = {}
    selectionHistory.value = []
    lastSelectionAnchor.value = null
    activeSheet.value = 0
    mapping.value = []
    result.value = []
    resultFields.value = []
    errors.value = []
    warnings.value = []
    treeResult.value = null
    rangeInput.value = ''
    rangeInputError.value = ''
    columnsInput.value = ''
    columnsInputError.value = ''
    linesInput.value = ''
    linesInputError.value = ''
    fileName.value = ''
    currentStep.value = 1
    validationReport.value = null
    importResult.value = null
    config.value = {
        version: '1.0', service: null, procedure: 1, annee: 2026,
        typeImport: 'PTA', activitesDejaValidees: false,
        cible: 'ACTIVITES',
    }
}

/* ============================================================
   LIFECYCLE
   ============================================================ */
onMounted(() => {
    document.addEventListener('mousemove', onGlobalMouseMove)
    document.addEventListener('mouseup', onGlobalMouseUp)
    document.addEventListener('keydown', onGlobalKeyDown)
    void loadServices()
})
onBeforeUnmount(() => {
    stopAutoScroll()
    document.body.style.overflow = ''
    document.removeEventListener('mousemove', onGlobalMouseMove)
    document.removeEventListener('mouseup', onGlobalMouseUp)
    document.removeEventListener('keydown', onGlobalKeyDown)
})
</script>
<template>
    <div class="import-page">
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    {{ dateDuJour }}
                </div>
                <h1>Import — {{ cibleLabel }}</h1>
                <span class="title-divider" aria-hidden="true"></span>
                <p class="subheading">
                    Importez vos {{ cibleLabel.toLowerCase() }} depuis un fichier Excel (.xlsx,
                    .xls, .csv)
                </p>
            </div>
            <div class="welcome-row__right">
                <BaseButton v-if="fileName" variant="secondary" @click="resetAll">
                    <i class="bi bi-arrow-counterclockwise"></i>
                    Recommencer
                </BaseButton>
            </div>
        </header>

        <nav class="import-stepper" aria-label="Étapes de l'import">
            <button v-for="(label, i) in STEPS" :key="i" type="button" class="import-stepper__item" :class="{
                'import-stepper__item--active': currentStep === i + 1,
                'import-stepper__item--done': currentStep > i + 1,
                'import-stepper__item--clickable': currentStep > i + 1 && workbookRef,
            }" :disabled="currentStep <= i + 1 || !workbookRef"
                :aria-current="currentStep === i + 1 ? 'step' : undefined" @click="onStepClick(i)">
                <span class="import-stepper__num">
                    <i v-if="currentStep > i + 1" class="bi bi-check-lg"></i>
                    <span v-else>{{ i + 1 }}</span>
                </span>
                <span class="import-stepper__label">{{ label }}</span>
            </button>
        </nav>

        <div v-if="errors.length && currentStep !== 5" class="alert alert--danger" role="alert">
            <i class="bi bi-exclamation-triangle-fill"></i>
            <div>
                <div v-for="(e, i) in errors" :key="i">{{ e }}</div>
            </div>
        </div>

        <div v-if="hasWarnings && currentStep !== 5" class="alert alert--warning" role="status">
            <i class="bi bi-info-circle-fill"></i>
            <div>
                <div v-for="(w, i) in warnings" :key="i">{{ w }}</div>
            </div>
        </div>

        <!-- ============================ ÉTAPE 1 ============================ -->
        <section v-if="currentStep === 1" class="card">
            <header class="card__head">
                <span class="card__icon"><i class="bi bi-file-earmark-excel"></i></span>
                <h2 class="card__title">
                    Étape 1 — Prévisualisation
                    <span class="card__sub">Choisissez la cible, chargez un fichier et définissez la ligne
                        d'en-tête</span>
                </h2>
            </header>
            <div class="card__body">
                <div class="import-target">
                    <div class="import-target__intro">
                        <i class="bi bi-bullseye"></i>
                        <div>
                            <strong>Que souhaitez-vous importer ?</strong>
                            <small>Ce choix conditionne les feuilles à sélectionner et les champs
                                disponibles au mapping.</small>
                        </div>
                    </div>
                    <div class="config-radio-group import-target__options" role="radiogroup">
                        <label v-for="c in CIBLE_LIST" :key="c" class="config-radio"
                            :class="{ 'config-radio--active': config.cible === c }">
                            <input v-model="config.cible" type="radio" :value="c" @change="onCibleChange" />
                            <span class="config-radio__dot"></span>
                            <span class="config-radio__body">
                                <strong>{{ CIBLE_LABELS[c] }}</strong>
                                <small>
                                    <template v-if="c === 'ACTIVITES'">1 ou 2 feuilles — avec ou sans
                                        sous-activités</template>
                                    <template v-else-if="c === 'SOUS_ACTIVITES'">1 feuille — rattachées à une activité
                                        par référence</template>
                                    <template v-else-if="c === 'RESULTATS_INTERMEDIAIRES'">1 feuille — rattachés à une
                                        activité par référence</template>
                                    <template v-else-if="c === 'INDICATEURS'">1 feuille — rattachés à une activité par
                                        référence</template>
                                    <template v-else>1 feuille — rattachées à une activité par référence</template>
                                </small>
                            </span>
                        </label>
                    </div>
                </div>

                <div v-if="!workbookRef" class="dropzone" :class="{ 'dropzone--dragover': isDragover }"
                    @dragover.prevent="isDragover = true" @dragleave.prevent="isDragover = false"
                    @drop.prevent="onDrop">
                    <div class="dropzone__icon"><i class="bi bi-cloud-arrow-up"></i></div>
                    <p class="dropzone__title">
                        <strong>Glissez-déposez</strong> un fichier Excel ou CSV
                    </p>
                    <p class="dropzone__hint">
                        Formats acceptés : .xlsx, .xls, .csv · Tout reste dans votre navigateur.
                    </p>
                    <label class="btn btn--primary">
                        <i class="bi bi-folder2-open"></i>
                        Choisir un fichier
                        <input type="file" accept=".xlsx,.xls,.csv" hidden @change="onFileChange" />
                    </label>
                </div>

                <div v-else class="preview-area">
                    <div class="preview-toolbar">
                        <div class="preview-toolbar__file">
                            <i class="bi bi-file-earmark-spreadsheet"></i>
                            <span class="preview-toolbar__name">{{ fileName }}</span>
                            <span class="preview-toolbar__badge">{{ sheets.length }} feuille(s)</span>
                        </div>
                        <div class="preview-toolbar__actions">
                            <BaseButton size="sm" variant="primary" @click="ouvrirPleinEcranPreview">
                                <i class="bi bi-arrows-fullscreen"></i>
                                Plein écran
                            </BaseButton>
                            <label class="btn btn--secondary btn--sm">
                                <i class="bi bi-arrow-repeat"></i>
                                Changer
                                <input type="file" accept=".xlsx,.xls,.csv" hidden @change="onFileChange" />
                            </label>
                        </div>
                    </div>

                    <div class="sheet-tabs">
                        <button v-for="(name, i) in sheets" :key="name" type="button" class="sheet-tab"
                            :class="{ 'sheet-tab--active': activeSheet === i }" @click="activeSheet = i">
                            {{ name }}
                        </button>
                    </div>

                    <div class="preview-info">
                        <span class="preview-info__label">
                            <i class="bi bi-list-columns-reverse"></i>
                            En-tête : {{ headerRowLabel }}
                        </span>
                        <span class="preview-info__hint">Cliquez sur un <strong>numéro de ligne</strong> pour définir la
                            ligne
                            d'en-tête</span>
                        <button v-if="(headerRow[activeSheetName] ?? -1) >= 0" type="button"
                            class="btn btn--ghost btn--sm" @click="clearHeader">
                            <i class="bi bi-x-lg"></i>
                            Aucun en-tête
                        </button>
                    </div>

                    <div class="grid-wrapper">
                        <div class="grid-scroll">
                            <table class="import-grid">
                                <thead>
                                    <tr>
                                        <th class="grid-corner"></th>
                                        <th v-for="c in currentMaxCol" :key="c" class="grid-col-head">
                                            {{ colName(c - 1) }}
                                        </th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr v-for="r in currentMaxRow" :key="r">
                                        <th class="grid-row-head grid-row-head--clickable" :class="{
                                            'grid-row-head--header':
                                                r - 1 === (headerRow[activeSheetName] ?? -1),
                                        }" :title="'Définir la ligne ' + r + ' comme en-tête'"
                                            @click="setHeaderRow(r - 1)">
                                            {{ r }}
                                        </th>
                                        <td v-for="c in currentMaxCol" :key="c" class="grid-cell" :class="{
                                            'grid-cell--header': r - 1 === (headerRow[activeSheetName] ?? -1),
                                        }">
                                            {{ cellValue(r - 1, c - 1) }}
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <div class="status-bar">
                        <span class="status-bar__item">{{ currentMaxRow }} ligne(s) × {{ currentMaxCol }}
                            colonne(s)</span>
                        <span class="status-bar__item status-bar__item--muted">{{
                            activeSheetName
                        }}</span>
                        <span class="status-bar__spacer"></span>
                        <BaseButton variant="primary" @click="goToStep(2)">
                            Suivant : sélection des données
                            <i class="bi bi-arrow-right"></i>
                        </BaseButton>
                    </div>
                </div>
            </div>
        </section>

        <!-- ============================ ÉTAPE 2 ============================ -->
        <section v-else-if="currentStep === 2" class="card">
            <header class="card__head">
                <span class="card__icon"><i class="bi bi-ui-checks"></i></span>
                <h2 class="card__title">
                    Étape 2 — Sélection
                    <span class="card__sub">Sélectionnez les cellules à importer</span>
                </h2>
                <BaseButton variant="secondary" size="sm" :disabled="!canUndo" @click="undoSelection">
                    <i class="bi bi-arrow-counterclockwise"></i>
                    Annuler (Ctrl+Z)
                </BaseButton>
                <BaseButton variant="primary" size="sm" @click="ouvrirPleinEcranSelection">
                    <i class="bi bi-arrows-fullscreen"></i>
                    Plein écran
                </BaseButton>
            </header>
            <div class="card__body">
                <div class="info-banner">
                    <i class="bi bi-info-circle"></i>
                    <span>
                        <strong>Glisser</strong> pour sélectionner · <strong>Shift+clic</strong> pour
                        étendre · <strong>Ctrl/Cmd+clic</strong> pour ajouter une plage ·
                        <strong>Ctrl/Cmd+A</strong> tout sélectionner ·
                        <strong>Ctrl/Cmd+Z</strong> annuler la dernière sélection · Saisir une plage /
                        colonnes / lignes ci-dessous
                    </span>
                </div>

                <div class="sheet-tabs">
                    <button v-for="(name, i) in sheets" :key="name" type="button" class="sheet-tab" :class="{
                        'sheet-tab--active': activeSheet === i,
                        'sheet-tab--has-sel':
                            (selectedRanges[name]?.length ?? 0) > 0 && activeSheet !== i,
                    }" @click="activeSheet = i">
                        {{ name }}
                    </button>
                </div>

                <div class="range-inputs">
                    <div class="range-input-group">
                        <label class="range-input-label">
                            <i class="bi bi-bounding-box"></i>
                            Plage
                        </label>
                        <input v-model="rangeInput" type="text" class="range-input"
                            :class="{ 'range-input--error': rangeInputError }" placeholder="A1:E10"
                            @keydown.enter.prevent="applyRangeInput(true)" />
                        <BaseButton size="sm" variant="secondary" @click="applyRangeInput(false)">Remplacer</BaseButton>
                        <BaseButton size="sm" variant="primary" @click="applyRangeInput(true)">Ajouter</BaseButton>
                    </div>
                    <div v-if="rangeInputError" class="range-input-error">
                        {{ rangeInputError }}
                    </div>

                    <div class="range-input-group">
                        <label class="range-input-label">
                            <i class="bi bi-layout-three-columns"></i>
                            Colonnes
                        </label>
                        <input v-model="columnsInput" type="text" class="range-input"
                            :class="{ 'range-input--error': columnsInputError }" placeholder="A,B,C ou A-D"
                            @keydown.enter.prevent="applyColumnsInput(true)" />
                        <BaseButton size="sm" variant="secondary" @click="applyColumnsInput(false)">Remplacer
                        </BaseButton>
                        <BaseButton size="sm" variant="primary" @click="applyColumnsInput(true)">Ajouter</BaseButton>
                    </div>
                    <div v-if="columnsInputError" class="range-input-error">
                        {{ columnsInputError }}
                    </div>

                    <div class="range-input-group">
                        <label class="range-input-label">
                            <i class="bi bi-list-ol"></i>
                            Lignes
                        </label>
                        <input v-model="linesInput" type="text" class="range-input"
                            :class="{ 'range-input--error': linesInputError }" placeholder="1-10 ou 1,3,5"
                            @keydown.enter.prevent="applyLinesInput(true)" />
                        <BaseButton size="sm" variant="secondary" @click="applyLinesInput(false)">Remplacer</BaseButton>
                        <BaseButton size="sm" variant="primary" @click="applyLinesInput(true)">Ajouter</BaseButton>
                    </div>
                    <div v-if="linesInputError" class="range-input-error">
                        {{ linesInputError }}
                    </div>
                </div>

                <div class="preview-info">
                    <span class="preview-info__label">
                        <i class="bi bi-bounding-box"></i>
                        {{ selectionInfoLabel }}
                    </span>
                    <span class="preview-info__hint">{{ selectionInfoDetail }}</span>
                    <span v-if="isDragging" class="drag-mode-badge" :class="{ 'drag-mode-badge--add': dragAdd }">
                        <i class="bi" :class="dragAdd ? 'bi-plus-circle' : 'bi-arrow-repeat'"></i>
                        {{
                            dragMode === "shift"
                                ? "Extension"
                                : dragAdd
                                    ? "Ajout à la sélection"
                                    : "Nouvelle sélection"
                        }}
                    </span>
                </div>

                <div class="grid-wrapper">
                    <div ref="gridScrollRef" class="grid-scroll" @mousedown="onGridMouseDown">
                        <table class="import-grid">
                            <thead>
                                <tr>
                                    <th class="grid-corner"></th>
                                    <th v-for="c in currentMaxCol" :key="c"
                                        class="grid-col-head grid-col-head--selectable"
                                        :class="{ 'grid-col-head--selected': highlightedColumns.has(c - 1) }"
                                        :data-col="c - 1" :title="'Sélectionner la colonne ' + colName(c - 1)">
                                        {{ colName(c - 1) }}
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="r in currentMaxRow" :key="r">
                                    <th class="grid-row-head grid-row-head--selectable" :class="{
                                        'grid-row-head--header':
                                            r - 1 === (headerRow[activeSheetName] ?? -1),
                                        'grid-row-head--selected':
                                            highlightedRows.has(r - 1) &&
                                            r - 1 !== (headerRow[activeSheetName] ?? -1),
                                    }" :data-row="r - 1" :data-col="0">
                                        {{ r }}
                                    </th>
                                    <td v-for="c in currentMaxCol" :key="c" :class="[
                                        cellClass(r - 1, c - 1),
                                        {
                                            'grid-cell--header': r - 1 === (headerRow[activeSheetName] ?? -1),
                                        },
                                    ]" :style="cellSelectionStyle(r - 1, c - 1)" :data-row="r - 1" :data-col="c - 1">
                                        {{ cellValue(r - 1, c - 1) }}
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>

                <div v-if="Object.keys(rangesBySheet).length > 0" class="selection-list">
                    <div v-for="(ranges, sheetName) in rangesBySheet" :key="sheetName" class="selection-group">
                        <strong class="selection-group__title">{{ sheetName }}</strong>
                        <span v-for="(range, i) in ranges" :key="i" class="selection-chip">
                            <i class="bi bi-bounding-box"></i>
                            {{ rangeLabel(sheetName, range) }}
                            <button type="button" class="selection-chip__remove" title="Supprimer"
                                @click="removeRange(sheetName, i)">
                                <i class="bi bi-x"></i>
                            </button>
                        </span>
                    </div>
                </div>

                <div class="status-bar">
                    <span class="status-bar__item">{{ currentSelectedRangeCount }} plage(s) ·
                        {{ currentSelectedRowCount }} ligne(s)</span>
                    <span class="status-bar__item status-bar__item--muted">{{
                        activeSheetName
                    }}</span>
                    <span class="status-bar__spacer"></span>
                    <BaseButton variant="secondary" size="sm" @click="clearActiveSheetSelection">
                        Effacer cette feuille
                    </BaseButton>
                    <BaseButton variant="secondary" size="sm" @click="clearAllSelections">
                        Tout effacer
                    </BaseButton>
                    <BaseButton variant="primary" :disabled="!hasAnySelection" @click="goToStep(3)">
                        Suivant : mapping
                        <i class="bi bi-arrow-right"></i>
                    </BaseButton>
                </div>
            </div>
        </section>

        <!-- ============================ ÉTAPE 3 ============================ -->
        <section v-else-if="currentStep === 3" class="card">
            <header class="card__head">
                <span class="card__icon"><i class="bi bi-diagram-3"></i></span>
                <h2 class="card__title">
                    Étape 3 — Mapping
                    <span class="card__sub">Associez chaque colonne à un champ — cible : {{ cibleLabel }}</span>
                </h2>
                <span class="badge">{{ mappedCount }} colonne(s) associée(s)</span>
            </header>
            <div class="card__body">
                <div class="info-banner">
                    <i class="bi bi-info-circle"></i>
                    <span v-if="config.cible === 'ACTIVITES'">
                        <strong>Sans sous-activités</strong> : mappez les colonnes des activités sur
                        <strong>une seule feuille</strong> (aucune clé de jointure requise).<br />
                        <strong>Avec sous-activités</strong> : mappez
                        <strong>« Référence (activité) »</strong> et
                        <strong>« Référence (sous-activité) »</strong> sur
                        <strong>deux feuilles distinctes</strong>
                        pour joindre les activités à leurs sous-activités.
                    </span>
                    <span v-else>
                        <strong>Une seule feuille</strong> — mappez les colonnes des
                        {{ cibleLabel.toLowerCase() }} ET la
                        <strong>« Référence (activité) »</strong> pour rattacher chaque ligne à son
                        activité. Les lignes sont automatiquement regroupées par référence avant
                        l'envoi.
                    </span>
                </div>

                <div class="sheet-roles">
                    <div v-for="[sheet, analysis] in sheetAnalysis" :key="sheet" class="sheet-roles__item">
                        <span class="sheet-roles__sheet">
                            <i class="bi bi-file-earmark-spreadsheet"></i>
                            {{ sheet }}
                        </span>
                        <span v-for="role in [...analysis.roles]" :key="role" class="sheet-roles__badge">
                            {{ ROLE_LABELS[role] }}
                        </span>
                        <span v-if="analysis.hasActiviteRef" class="sheet-roles__badge sheet-roles__badge--ref">
                            <i class="bi bi-link-45deg"></i> Réf. activité
                        </span>
                        <span v-if="analysis.hasSousActiviteRef" class="sheet-roles__badge sheet-roles__badge--ref">
                            <i class="bi bi-link-45deg"></i> Réf. SA
                        </span>
                    </div>
                </div>

                <div class="mapping-wrap">
                    <table class="mapping-table">
                        <thead>
                            <tr>
                                <th>Feuille</th>
                                <th>Colonne</th>
                                <th>Nom Excel</th>
                                <th>Aperçu</th>
                                <th>Champ destination</th>
                                <th class="mapping-order-col">Ordre</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="(item, index) in mapping" :key="index" :class="[
                                item.field
                                    ? `mapping-row mapping-row--${categoryClass(item.field)}`
                                    : 'mapping-row',
                                { 'mapping-row--empty': !item.field },
                            ]">
                                <td class="mapping-sheet">{{ item.sheet }}</td>
                                <td class="mapping-letter">{{ item.colLetter }}</td>
                                <td class="mapping-name">{{ item.excelName }}</td>
                                <td class="mapping-preview">{{ previewForMapping(item) }}</td>
                                <td class="mapping-field">
                                    <select v-model="item.field" class="mapping-select"
                                        :class="{ 'mapping-select--filled': item.field }" @change="onMappingChange">
                                        <option value="">— Ignorer —</option>
                                        <optgroup v-for="g in groupedOptions(index)" :key="g.category"
                                            :label="g.category">
                                            <option v-for="f in g.fields" :key="f.name" :value="f.name">
                                                {{ f.label }}{{ f.mandatory ? " *" : "" }}{{ f.fill ? " ↕" : "" }}
                                            </option>
                                        </optgroup>
                                    </select>
                                    <div v-if="item.field" class="mapping-field-meta">
                                        <span class="mapping-field-path">
                                            <i class="bi bi-diagram-3"></i>
                                            {{ fieldPath(item.field) }}
                                        </span>
                                        <span v-if="getFieldConfig(item.field)?.mandatory"
                                            class="mapping-badge mapping-badge--required">Obligatoire</span>
                                        <span v-if="getFieldConfig(item.field)?.fill"
                                            class="mapping-badge mapping-badge--fill">Remplissage auto</span>
                                    </div>
                                </td>
                                <td class="mapping-order-col">
                                    <div class="order-controls">
                                        <button type="button" :disabled="index === 0" title="Monter"
                                            @click="moveMapping(index, -1)">
                                            <i class="bi bi-arrow-up"></i>
                                        </button>
                                        <button type="button" :disabled="index === mapping.length - 1" title="Descendre"
                                            @click="moveMapping(index, 1)">
                                            <i class="bi bi-arrow-down"></i>
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <div class="hint-row">
                    <span class="hint-row__item"><span class="hint-row__mark">*</span> Champ obligatoire</span>
                    <span class="hint-row__item"><span class="hint-row__mark">↕</span> Remplissage automatique</span>
                    <span class="hint-row__item"><span class="hint-row__mark">›</span> Hiérarchie imbriquée</span>
                    <span class="hint-row__item"><span class="hint-row__mark">⚙</span> Code, référence et dates
                        d'activité
                        générés</span>
                    <span class="hint-row__item"><span class="hint-row__mark">🔗</span> Clé de jointure</span>
                </div>

                <div class="form-actions-bar">
                    <BaseButton variant="secondary" @click="goToStep(2)">
                        <i class="bi bi-arrow-left"></i>
                        Retour
                    </BaseButton>
                    <span class="status-bar__spacer"></span>
                    <BaseButton variant="primary" @click="validateMapping">
                        Valider les correspondances
                        <i class="bi bi-arrow-right"></i>
                    </BaseButton>
                </div>
            </div>
        </section>

        <!-- ============================ ÉTAPE 4 ============================ -->
        <section v-else-if="currentStep === 4" class="card">
            <header class="card__head">
                <span class="card__icon"><i class="bi bi-gear"></i></span>
                <h2 class="card__title">
                    Étape 4 — Configuration
                    <span class="card__sub">Paramètres du lot — cible : {{ cibleLabel }}</span>
                </h2>
            </header>
            <div class="card__body">
                <form class="config-form" @submit.prevent="goToImportStep">
                    <div class="config-grid">
                        <div class="config-field">
                            <label class="config-label">Version <span class="config-required">*</span></label>
                            <input v-model="config.version" class="config-input" type="text" placeholder="1.0"
                                maxlength="20" />
                        </div>

                        <template v-if="config.cible === 'ACTIVITES'">
                            <div class="config-field">
                                <label class="config-label">Service <span class="config-required">*</span></label>
                                <select v-model="config.service" class="config-input" :disabled="loadingServices">
                                    <option :value="null">— Sélectionner —</option>
                                    <option v-for="s in services" :key="s.id" :value="s.id">
                                        {{ s.nom
                                        }}<template v-if="s.departement?.nom">
                                            · {{ s.departement.nom }}</template>
                                    </option>
                                </select>
                                <p class="config-hint">
                                    <template v-if="loadingServices">Chargement…</template>
                                    <template v-else-if="services.length === 0">Aucun service disponible.</template>
                                    <template v-else>Toutes les activités seront rattachées à ce service.</template>
                                </p>
                            </div>

                            <div class="config-field">
                                <label class="config-label">Procédure <span class="config-required">*</span></label>
                                <input v-model.number="config.procedure" class="config-input" type="number"
                                    placeholder="1" min="1" />
                            </div>

                            <div class="config-field">
                                <label class="config-label">Année <span class="config-required">*</span></label>
                                <input v-model.number="config.annee" class="config-input" type="number"
                                    placeholder="2026" min="1900" max="2100" />
                                <p class="config-hint">
                                    Exercice d'import — sert aux objectifs absents et aux codes.
                                </p>
                            </div>

                            <div class="config-field config-field--full">
                                <label class="config-label">Type d'import</label>
                                <div class="config-radio-group" role="radiogroup">
                                    <label class="config-radio"
                                        :class="{ 'config-radio--active': config.typeImport === 'PTA' }">
                                        <input v-model="config.typeImport" type="radio" value="PTA" />
                                        <span class="config-radio__dot"></span>
                                        <span class="config-radio__body">
                                            <strong>PTA</strong>
                                            <small>Rattaché à un objectif spécifique</small>
                                        </span>
                                    </label>
                                    <label class="config-radio"
                                        :class="{ 'config-radio--active': config.typeImport === 'NON_PTA' }">
                                        <input v-model="config.typeImport" type="radio" value="NON_PTA" />
                                        <span class="config-radio__dot"></span>
                                        <span class="config-radio__body">
                                            <strong>NON PTA</strong>
                                            <small>Hors plan de travail annuel</small>
                                        </span>
                                    </label>
                                </div>
                            </div>

                            <div class="config-field config-field--full">
                                <label class="config-switch">
                                    <input type="checkbox" v-model="config.activitesDejaValidees" />
                                    <span class="config-switch__track"><span class="config-switch__thumb"></span></span>
                                    <span class="config-switch__text">
                                        <strong>Activités déjà validées</strong>
                                        <small>Si activé, statut initial <code>NON_COMMENCE</code> au lieu de
                                            <code>BROUILLON</code>.</small>
                                    </span>
                                </label>
                            </div>
                        </template>

                        <template v-else-if="config.cible === 'SOUS_ACTIVITES'">
                            <div class="config-field config-field--full">
                                <label class="config-label">Année <span class="config-required">*</span></label>
                                <input v-model.number="config.annee" class="config-input" type="number"
                                    placeholder="2026" min="1900" max="2100" />
                                <p class="config-hint">
                                    Utilisée pour générer les codes de sous-activités
                                    (<code>SA-AAAA-RANG-SEQ</code>).
                                </p>
                            </div>
                        </template>
                    </div>
                </form>

                <div class="config-summary">
                    <div class="config-summary__item">
                        <span class="config-summary__label">Cible</span>
                        <strong class="config-summary__value config-summary__value--sm">{{
                            cibleLabel
                        }}</strong>
                    </div>
                    <div class="config-summary__item">
                        <span class="config-summary__label">Lignes à importer</span>
                        <strong class="config-summary__value">{{ result.length }}</strong>
                    </div>
                    <div class="config-summary__item">
                        <span class="config-summary__label">Colonnes mappées</span>
                        <strong class="config-summary__value">{{ resultFields.length }}</strong>
                    </div>
                    <div class="config-summary__item">
                        <span class="config-summary__label">
                            {{ config.cible === "ACTIVITES" ? "Service cible" : "Activités cibles" }}
                        </span>
                        <strong class="config-summary__value config-summary__value--sm">
                            {{ config.cible === "ACTIVITES" ? serviceLabel : uniqueReferenceCount }}
                        </strong>
                    </div>
                </div>

                <div class="form-actions-bar">
                    <BaseButton variant="secondary" @click="goToStep(3)">
                        <i class="bi bi-arrow-left"></i>
                        Retour aux correspondances
                    </BaseButton>
                    <span class="status-bar__spacer"></span>
                    <BaseButton variant="primary" :loading="validatingImport" :disabled="validatingImport"
                        @click="goToImportStep">
                        Continuer vers l'import
                        <i class="bi bi-arrow-right"></i>
                    </BaseButton>
                </div>
            </div>
        </section>

        <!-- ============================ ÉTAPE 5 ============================ -->
        <section v-else-if="currentStep === 5" class="card">
            <header class="card__head">
                <span class="card__icon"><i class="bi bi-check2-square"></i></span>
                <h2 class="card__title">
                    Étape 5 — Prévisualisation finale
                    <span class="card__sub">{{ resultSummary }}</span>
                </h2>
            </header>
            <div class="card__body">
                <div v-if="errors.length" class="alert alert--danger">
                    <i class="bi bi-exclamation-triangle-fill"></i>
                    <div>
                        <div v-for="(e, i) in errors" :key="i">{{ e }}</div>
                    </div>
                </div>

                <div v-if="validationReport" class="validation-report">
                    <div class="validation-report__summary" :class="validationReport.valide
                        ? 'validation-report__summary--valid'
                        : 'validation-report__summary--invalid'
                        ">
                        <i class="bi" :class="validationReport.valide
                            ? 'bi-check-circle-fill'
                            : 'bi-exclamation-triangle-fill'
                            "></i>
                        <div>
                            <strong>{{ validationReport.message }}</strong>
                            <span>
                                {{ validationReport.nombreActivites }} élément(s) ·
                                {{ validationReport.nombreErreurs }} erreur(s) ·
                                {{ validationReport.nombreAvertissements }} avertissement(s)
                            </span>
                        </div>
                    </div>

                    <div v-if="validationReport.erreurs?.length" class="alert alert--danger">
                        <i class="bi bi-x-octagon-fill"></i>
                        <div class="validation-report__issues">
                            <strong>Erreurs de validation</strong>
                            <div v-for="(issue, index) in validationReport.erreurs" :key="index">
                                <span v-if="issue.activite">{{ issue.activite }} · </span>
                                <span v-if="issue.champ">{{ issue.champ }} — </span>
                                {{ issue.message }}
                            </div>
                        </div>
                    </div>

                    <div v-if="validationReport.avertissements?.length" class="info-banner validation-report__warnings">
                        <i class="bi bi-info-circle-fill"></i>
                        <div class="validation-report__issues">
                            <strong>Avertissements</strong>
                            <div v-for="(issue, index) in validationReport.avertissements" :key="index">
                                <span v-if="issue.activite">{{ issue.activite }} · </span>
                                <span v-if="issue.champ">{{ issue.champ }} — </span>
                                {{ issue.message }}
                            </div>
                        </div>
                    </div>
                </div>

                <div v-if="importResult" class="import-success">
                    <i class="bi bi-check-circle-fill"></i>
                    <div>
                        <strong>{{ importResult.message }}</strong>
                        <span v-if="config.cible === 'ACTIVITES'">
                            {{ importResult.nombreActivitesImportees }} activité(s),
                            {{ importResult.nombreSousActivitesImportees }} sous-activité(s),
                            {{ importResult.nombreLivrablesImportes }} livrable(s) importé(s).
                        </span>
                        <span v-else-if="config.cible === 'SOUS_ACTIVITES'">
                            {{ importResult.nombreSousActivitesImportees }} sous-activité(s)
                            importée(s).
                        </span>
                        <span v-else-if="config.cible === 'RESULTATS_INTERMEDIAIRES'">
                            {{ importResult.nombreResultatsIntermediairesImportes }} résultat(s)
                            importé(s).
                        </span>
                        <span v-else-if="config.cible === 'INDICATEURS'">
                            {{ importResult.indicateursCrees?.length ?? 0 }} indicateur(s) importé(s).
                        </span>
                        <span v-else-if="config.cible === 'ORIGINES'">
                            {{ importResult.originesCreees?.length ?? 0 }} origine(s) importée(s).
                        </span>
                        <span v-if="importResult.statutInitial">Statut initial : {{ importResult.statutInitial }}</span>
                    </div>
                </div>

                <div class="config-recap">
                    <span class="config-recap__item"><i class="bi bi-bullseye"></i><strong>Cible</strong> {{ cibleLabel
                    }}</span>
                    <span class="config-recap__item"><i class="bi bi-gear"></i><strong>Service</strong> {{ serviceLabel
                    }}</span>
                    <span class="config-recap__item"><i class="bi bi-calendar3"></i><strong>Année</strong>
                        {{ config.annee }}</span>
                    <span class="config-recap__item"><i class="bi bi-tag"></i><strong>Type</strong> {{ config.typeImport
                    }}</span>
                    <span class="config-recap__item"><i class="bi bi-list-ol"></i><strong>Procédure</strong> #{{
                        config.procedure
                    }}</span>
                </div>

                <div v-if="result.length === 0 && errors.length === 0" class="empty-state">
                    <i class="bi bi-inbox"></i>
                    <p>Aucune donnée à prévisualiser.</p>
                </div>

                <div v-else-if="result.length > 0" class="result-wrap">
                    <table class="result-table">
                        <thead>
                            <tr>
                                <th v-for="field in resultFields" :key="field">
                                    {{ fieldLabel(field) }}
                                </th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="(row, i) in result" :key="i">
                                <td v-for="field in resultFields" :key="field">{{ row[field] || "" }}</td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <div class="form-actions-bar">
                    <BaseButton variant="secondary" @click="goToStep(4)">
                        <i class="bi bi-arrow-left"></i>
                        Modifier la configuration
                    </BaseButton>
                    <BaseButton variant="secondary" @click="goToStep(3)">
                        <i class="bi bi-arrow-left"></i>
                        Modifier les correspondances
                    </BaseButton>
                    <span class="status-bar__spacer"></span>
                    <BaseButton variant="primary" :disabled="result.length === 0 ||
                        errors.length > 0 ||
                        !validationReport?.valide ||
                        !!importResult
                        " @click="openImportModal">
                        <i class="bi bi-upload"></i>
                        Importer
                    </BaseButton>
                </div>
            </div>
        </section>

        <!-- ============================ MODAL D'IMPORT ============================ -->
        <BaseModal v-model="showModal" :title="`Confirmer l'import — ${cibleLabel}`" size="md">
            <div class="import-modal">
                <div class="import-modal__icon"><i class="bi bi-cloud-arrow-up"></i></div>
                <h3 class="import-modal__title">Prêt à importer</h3>
                <p class="import-modal__text">
                    <strong>{{ result.length }}</strong> ligne(s) de
                    <strong>{{ cibleLabel.toLowerCase() }}</strong>
                    seront importées dans le service <strong>{{ serviceLabel }}</strong> pour
                    l'année <strong>{{ config.annee }}</strong>.
                </p>
                <div class="import-modal__recap">
                    <div class="import-modal__recap-row">
                        <span>Cible</span><strong>{{ cibleLabel }}</strong>
                    </div>
                    <div class="import-modal__recap-row">
                        <span>Version</span><strong>{{ config.version }}</strong>
                    </div>
                    <div v-if="config.cible === 'ACTIVITES'" class="import-modal__recap-row">
                        <span>Type d'import</span><strong>{{ config.typeImport }}</strong>
                    </div>
                    <div v-if="config.cible === 'ACTIVITES'" class="import-modal__recap-row">
                        <span>Procédure</span><strong>#{{ config.procedure }}</strong>
                    </div>
                    <div v-if="config.cible === 'ACTIVITES'" class="import-modal__recap-row">
                        <span>Statut initial</span>
                        <strong>{{
                            config.activitesDejaValidees ? "NON_COMMENCEE" : "BROUILLON"
                        }}</strong>
                    </div>
                    <div v-if="config.cible !== 'ACTIVITES'" class="import-modal__recap-row">
                        <span>Activité(s) cible(s)</span><strong>{{ uniqueReferenceCount }}</strong>
                    </div>
                    <div class="import-modal__recap-row">
                        <span>Lignes</span><strong>{{ result.length }}</strong>
                    </div>
                </div>
            </div>
            <template #footer>
                <BaseButton variant="secondary" :disabled="importing" @click="showModal = false">Annuler</BaseButton>
                <BaseButton variant="primary" :loading="importing" @click="confirmImport">
                    <i class="bi bi-check-lg"></i>
                    Confirmer l'import
                </BaseButton>
            </template>
        </BaseModal>
    </div>

    <!-- ============================================================
         PRÉVISUALISATION PLEIN ÉCRAN (étape 1)
         ============================================================ -->
    <Teleport to="body">
        <div v-if="previewFullscreen && workbookRef" class="preview-fs" role="dialog" aria-modal="true"
            aria-label="Prévisualisation plein écran">
            <header class="preview-fs__header">
                <div class="preview-fs__file">
                    <i class="bi bi-file-earmark-spreadsheet"></i>
                    <span class="preview-fs__name">{{ fileName }}</span>
                    <span class="preview-fs__badge">{{ sheets.length }} feuille(s)</span>
                </div>

                <div class="preview-fs__info">
                    <i class="bi bi-list-columns-reverse"></i>
                    En-tête : {{ headerRowLabel }}
                    <button v-if="(headerRow[activeSheetName] ?? -1) >= 0" type="button" class="preview-fs__info-clear"
                        @click="clearHeader">
                        <i class="bi bi-x-lg"></i>
                        Aucun en-tête
                    </button>
                </div>

                <button type="button" class="preview-fs__close" @click="fermerPleinEcranPreview"
                    aria-label="Fermer le plein écran">
                    <i class="bi bi-x-lg"></i>
                    <span class="preview-fs__close-label">Fermer</span>
                </button>
            </header>

            <div class="preview-fs__grid">
                <table class="import-grid">
                    <thead>
                        <tr>
                            <th class="grid-corner"></th>
                            <th v-for="c in currentMaxCol" :key="c" class="grid-col-head">
                                {{ colName(c - 1) }}
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="r in currentMaxRow" :key="r">
                            <th class="grid-row-head grid-row-head--clickable" :class="{
                                'grid-row-head--header': r - 1 === (headerRow[activeSheetName] ?? -1),
                            }" :title="'Définir la ligne ' + r + ' comme en-tête'" @click="setHeaderRow(r - 1)">
                                {{ r }}
                            </th>
                            <td v-for="c in currentMaxCol" :key="c" class="grid-cell" :class="{
                                'grid-cell--header': r - 1 === (headerRow[activeSheetName] ?? -1),
                            }">
                                {{ cellValue(r - 1, c - 1) }}
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <footer class="preview-fs__tabs">
                <button v-for="(name, i) in sheets" :key="name" type="button" class="preview-fs__tab"
                    :class="{ 'preview-fs__tab--active': activeSheet === i }" @click="activeSheet = i">
                    <i class="bi bi-grid-3x3-gap-fill"></i>
                    {{ name }}
                </button>
            </footer>
        </div>
    </Teleport>

    <!-- ============================================================
         SÉLECTION PLEIN ÉCRAN (étape 2) — NOUVEAU
         ============================================================ -->
    <Teleport to="body">
        <div v-if="selectionFullscreen && workbookRef" class="preview-fs" role="dialog" aria-modal="true"
            aria-label="Sélection plein écran">
            <header class="preview-fs__header">
                <div class="preview-fs__file">
                    <i class="bi bi-ui-checks"></i>
                    <span class="preview-fs__name">{{ fileName }}</span>
                    <span class="preview-fs__badge">{{ sheets.length }} feuille(s)</span>
                </div>

                <div class="preview-fs__info">
                    <i class="bi bi-bounding-box"></i>
                    {{ selectionInfoLabel }} · {{ selectionInfoDetail }}
                </div>

                <button type="button" class="preview-fs__btn preview-fs__btn--primary" @click="ouvrirRangeModal">
                    <i class="bi bi-lightning-charge-fill"></i>
                    Saisie rapide
                </button>

                <button type="button" class="preview-fs__close" @click="fermerPleinEcranSelection"
                    aria-label="Fermer le plein écran">
                    <i class="bi bi-x-lg"></i>
                    <span class="preview-fs__close-label">Fermer</span>
                </button>
            </header>

            <div ref="gridScrollRefFs" class="preview-fs__grid preview-fs__grid--selectable"
                @mousedown="onGridMouseDown">
                <table class="import-grid">
                    <thead>
                        <tr>
                            <th class="grid-corner"></th>
                            <th v-for="c in currentMaxCol" :key="c" class="grid-col-head grid-col-head--selectable"
                                :class="{ 'grid-col-head--selected': highlightedColumns.has(c - 1) }" :data-col="c - 1"
                                :title="'Sélectionner la colonne ' + colName(c - 1)">
                                {{ colName(c - 1) }}
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="r in currentMaxRow" :key="r">
                            <th class="grid-row-head grid-row-head--selectable" :class="{
                                'grid-row-head--header': r - 1 === (headerRow[activeSheetName] ?? -1),
                                'grid-row-head--selected':
                                    highlightedRows.has(r - 1) &&
                                    r - 1 !== (headerRow[activeSheetName] ?? -1),
                            }" :data-row="r - 1" :data-col="0">
                                {{ r }}
                            </th>
                            <td v-for="c in currentMaxCol" :key="c" :class="[
                                cellClass(r - 1, c - 1),
                                { 'grid-cell--header': r - 1 === (headerRow[activeSheetName] ?? -1) },
                            ]" :style="cellSelectionStyle(r - 1, c - 1)" :data-row="r - 1" :data-col="c - 1">
                                {{ cellValue(r - 1, c - 1) }}
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <footer class="preview-fs__footer">
                <div class="preview-fs__footer-info">
                    <span class="preview-fs__footer-label">
                        <i class="bi bi-bounding-box"></i>
                        {{ currentSelectedRangeCount }} plage(s) ·
                        {{ currentSelectedRowCount }} ligne(s)
                    </span>
                    <span class="preview-fs__footer-muted">{{ activeSheetName }}</span>
                    <span v-if="isDragging" class="drag-mode-badge" :class="{ 'drag-mode-badge--add': dragAdd }">
                        <i class="bi" :class="dragAdd ? 'bi-plus-circle' : 'bi-arrow-repeat'"></i>
                        {{
                            dragMode === "shift"
                                ? "Extension"
                                : dragAdd
                                    ? "Ajout"
                                    : "Nouvelle sélection"
                        }}
                    </span>
                </div>
                <div class="preview-fs__footer-actions">
                    <BaseButton variant="secondary" size="sm" :disabled="!canUndo" @click="undoSelection">
                        <i class="bi bi-arrow-counterclockwise"></i>
                        Annuler
                    </BaseButton>
                    <BaseButton variant="secondary" size="sm" @click="clearActiveSheetSelection">
                        Effacer cette feuille
                    </BaseButton>
                    <BaseButton variant="secondary" size="sm" @click="clearAllSelections">
                        Tout effacer
                    </BaseButton>
                    <BaseButton variant="primary" :disabled="!hasAnySelection" @click="suivantDepuisPleinEcran">
                        Suivant : correspondance
                        <i class="bi bi-arrow-right"></i>
                    </BaseButton>
                </div>
            </footer>

            <div class="preview-fs__tabs">
                <button v-for="(name, i) in sheets" :key="name" type="button" class="preview-fs__tab" :class="{
                    'preview-fs__tab--active': activeSheet === i,
                    'preview-fs__tab--has-sel':
                        (selectedRanges[name]?.length ?? 0) > 0 && activeSheet !== i,
                }" @click="activeSheet = i">
                    <i class="bi bi-grid-3x3-gap-fill"></i>
                    {{ name }}
                </button>
            </div>

            <!-- Modal « Saisie rapide » -->
            <div v-if="showRangeModal" class="range-quick-overlay" @click.self="fermerRangeModal">
                <div class="range-quick-modal" role="dialog" aria-modal="true" aria-label="Saisie rapide">
                    <header class="range-quick-modal__head">
                        <div class="range-quick-modal__title">
                            <i class="bi bi-lightning-charge-fill"></i>
                            Saisie rapide
                            <span class="range-quick-modal__sheet">{{ activeSheetName }}</span>
                        </div>
                        <button type="button" class="preview-fs__close" @click="fermerRangeModal" aria-label="Fermer">
                            <i class="bi bi-x-lg"></i>
                        </button>
                    </header>
                    <div class="range-quick-modal__body">
                        <div class="config-field">
                            <label class="config-label">
                                <i class="bi bi-bounding-box"></i>
                                Plage
                            </label>
                            <input v-model="rangeInput" type="text" class="config-input" placeholder="A1:E10"
                                :class="{ 'range-input--error': rangeInputError }"
                                @keydown.enter.prevent="applyRangeInput(true)" />
                            <div v-if="rangeInputError" class="range-input-error">
                                {{ rangeInputError }}
                            </div>
                            <div class="range-quick-modal__actions">
                                <BaseButton size="sm" variant="secondary" @click="applyRangeInput(false)">
                                    Remplacer
                                </BaseButton>
                                <BaseButton size="sm" variant="primary" @click="applyRangeInput(true)">
                                    Ajouter
                                </BaseButton>
                            </div>
                        </div>

                        <div class="config-field">
                            <label class="config-label">
                                <i class="bi bi-layout-three-columns"></i>
                                Colonnes
                            </label>
                            <input v-model="columnsInput" type="text" class="config-input" placeholder="A,B,C ou A-D"
                                :class="{ 'range-input--error': columnsInputError }"
                                @keydown.enter.prevent="applyColumnsInput(true)" />
                            <div v-if="columnsInputError" class="range-input-error">
                                {{ columnsInputError }}
                            </div>
                            <div class="range-quick-modal__actions">
                                <BaseButton size="sm" variant="secondary" @click="applyColumnsInput(false)">
                                    Remplacer
                                </BaseButton>
                                <BaseButton size="sm" variant="primary" @click="applyColumnsInput(true)">
                                    Ajouter
                                </BaseButton>
                            </div>
                        </div>

                        <div class="config-field">
                            <label class="config-label">
                                <i class="bi bi-list-ol"></i>
                                Lignes
                            </label>
                            <input v-model="linesInput" type="text" class="config-input" placeholder="1-10 ou 1,3,5"
                                :class="{ 'range-input--error': linesInputError }"
                                @keydown.enter.prevent="applyLinesInput(true)" />
                            <div v-if="linesInputError" class="range-input-error">
                                {{ linesInputError }}
                            </div>
                            <div class="range-quick-modal__actions">
                                <BaseButton size="sm" variant="secondary" @click="applyLinesInput(false)">
                                    Remplacer
                                </BaseButton>
                                <BaseButton size="sm" variant="primary" @click="applyLinesInput(true)">
                                    Ajouter
                                </BaseButton>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </Teleport>
</template>

<style scoped>
.import-page {
    --navy: #1a2a6c;
    --blue: #4a6cf7;
    --blue-soft: #eaf0ff;
    --gold: #d4af37;
    --gold-soft: #fff6dd;
    --amber: #b45309;
    --amber-soft: #fef3c7;
    --purple: #8b5cf6;
    --pink: #ec4899;
    --cyan: #0ea5e9;
    --orange: #f97316;
    --slate: #64748b;
    --slate-soft: #f1f5f9;
    --bg: #f5f7fa;
    --line: #e6ebf3;
    --line-2: #eef1f5;
    --ink: #15233c;
    --ink-2: #384a5e;
    --muted: #6b7280;
    --soft: #8a95a8;
    --red: #a61b16;
    --red-soft: #fdecea;
    --green: #1e7e34;
    --green-soft: #e6f4ea;
    --radius: 16px;
    --radius-sm: 12px;
    --shadow-card: 0 1px 2px rgba(26, 42, 108, 0.04), 0 8px 24px rgba(26, 42, 108, 0.06);

    min-height: calc(100vh - 68px);
    padding: 34px 38px 24px;
    display: flex;
    flex-direction: column;
    gap: 1.15rem;
    color: var(--ink);
    font-family: "DM Sans", "Segoe UI", sans-serif;
    background: radial-gradient(circle at 12% 8%,
            rgba(26, 42, 108, 0.055) 0,
            transparent 38%),
        radial-gradient(circle at 92% 94%, rgba(212, 175, 55, 0.065) 0, transparent 36%),
        var(--bg);
}

/* ============================================================
   STEPPER
   ============================================================ */
.import-stepper {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.75rem 1rem;
    background: #fff;
    border: 1px solid var(--line);
    border-radius: var(--radius);
    box-shadow: var(--shadow-card);
    overflow-x: auto;
}

.import-stepper__item {
    display: flex;
    align-items: center;
    gap: 0.55rem;
    padding: 0.45rem 0.85rem;
    border-radius: 10px;
    border: none;
    background: transparent;
    font-size: 0.8rem;
    font-weight: 600;
    color: var(--soft);
    white-space: nowrap;
    flex: 1;
    justify-content: center;
    cursor: default;
    transition: background 0.15s, color 0.15s, transform 0.12s;
    font-family: inherit;
}

.import-stepper__item--active {
    background: linear-gradient(135deg, var(--navy) 0%, var(--blue) 100%);
    color: #fff;
    box-shadow: 0 4px 12px rgba(26, 42, 108, 0.18);
}

.import-stepper__item--done {
    color: var(--green);
    background: var(--green-soft);
}

.import-stepper__item--clickable {
    cursor: pointer;
}

.import-stepper__item--clickable:hover {
    background: var(--blue-soft);
    color: var(--navy);
    transform: translateY(-1px);
}

.import-stepper__num {
    display: grid;
    place-items: center;
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: currentColor;
    color: #fff;
    font-size: 0.75rem;
    font-weight: 800;
    flex: none;
}

.import-stepper__item--active .import-stepper__num {
    background: rgba(255, 255, 255, 0.22);
}

.import-stepper__item--done .import-stepper__num {
    background: var(--green);
}

.import-stepper__label {
    font-weight: 600;
}

/* ============================================================
   WELCOME ROW
   ============================================================ */
.welcome-row {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
    margin-bottom: 0.25rem;
}

.welcome-row__left {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
    min-width: 0;
}

.welcome-row__right {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.eyebrow {
    display: inline-flex;
    align-items: center;
    gap: 0.45rem;
    font-size: 0.68rem;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: var(--muted);
}

.live-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: var(--green);
    box-shadow: 0 0 0 3px rgba(30, 126, 52, 0.18);
    animation: livePulse 2s ease-in-out infinite;
}

@keyframes livePulse {

    0%,
    100% {
        box-shadow: 0 0 0 3px rgba(30, 126, 52, 0.18);
    }

    50% {
        box-shadow: 0 0 0 6px rgba(30, 126, 52, 0.06);
    }
}

.welcome-row h1 {
    margin: 0;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 1.55rem;
    font-weight: 800;
    color: var(--navy);
    letter-spacing: -0.02em;
}

.title-divider {
    display: inline-block;
    width: 46px;
    height: 3px;
    border-radius: 2px;
    background: linear-gradient(90deg, var(--navy), var(--gold));
    margin: 0.25rem 0 0.35rem;
}

.subheading {
    margin: 0;
    font-size: 0.85rem;
    color: var(--muted);
    line-height: 1.5;
}

/* ============================================================
   CARD
   ============================================================ */
.card {
    background: #fff;
    border: 1px solid var(--line);
    border-radius: var(--radius);
    box-shadow: var(--shadow-card);
    overflow: hidden;
}

.card__head {
    display: flex;
    align-items: center;
    gap: 0.65rem;
    padding: 0.85rem 1.25rem;
    background: linear-gradient(140deg, #f7f9fd, #fff);
    border-bottom: 1px solid var(--line);
    flex-wrap: wrap;
}

.card__icon {
    display: grid;
    place-items: center;
    width: 32px;
    height: 32px;
    border-radius: 9px;
    background: var(--blue-soft);
    color: #3b63d9;
    font-size: 0.95rem;
    flex: none;
}

.card__title {
    margin: 0;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 0.92rem;
    font-weight: 700;
    color: var(--navy);
    flex: 1;
}

.card__sub {
    display: block;
    margin-top: 2px;
    font-size: 0.72rem;
    font-weight: 400;
    color: var(--muted);
    letter-spacing: 0;
}

.card__body {
    padding: 1rem 1.25rem 1.15rem;
}

.badge {
    display: inline-block;
    padding: 0.25rem 0.65rem;
    border-radius: 20px;
    background: var(--blue-soft);
    color: #1d4ed8;
    font-size: 0.7rem;
    font-weight: 700;
    letter-spacing: 0.03em;
    white-space: nowrap;
}

/* ============================================================
   IMPORT TARGET
   ============================================================ */
.import-target {
    margin-bottom: 1rem;
    padding: 0.9rem 1rem;
    background: linear-gradient(140deg, #f8fbff, #fff);
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.import-target__intro {
    display: flex;
    align-items: flex-start;
    gap: 0.55rem;
    margin-bottom: 0.75rem;
    font-size: 0.82rem;
}

.import-target__intro i {
    color: var(--blue);
    font-size: 1rem;
    margin-top: 1px;
    flex: none;
}

.import-target__intro strong {
    display: block;
    color: var(--navy);
}

.import-target__intro small {
    color: var(--muted);
    font-size: 0.72rem;
}

.import-target__options {
    grid-template-columns: repeat(5, minmax(0, 1fr));
}

/* ============================================================
   DROPZONE
   ============================================================ */
.dropzone {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    min-height: 240px;
    padding: 2.5rem 1.5rem;
    border: 2px dashed #b8c7dc;
    border-radius: var(--radius-sm);
    background: #f8fbff;
    text-align: center;
    transition: background 0.15s, border-color 0.15s;
}

.dropzone--dragover {
    border-color: var(--blue);
    background: var(--blue-soft);
}

.dropzone__icon {
    display: grid;
    place-items: center;
    width: 64px;
    height: 64px;
    border-radius: 18px;
    background: #fff;
    color: var(--blue);
    font-size: 1.85rem;
    box-shadow: 0 4px 12px rgba(26, 42, 108, 0.08);
    margin-bottom: 0.5rem;
}

.dropzone__title {
    margin: 0;
    font-size: 0.95rem;
    color: var(--navy);
}

.dropzone__hint {
    margin: 0 0 1rem;
    font-size: 0.78rem;
    color: var(--muted);
}

/* ============================================================
   BOUTONS LOCAUX
   ============================================================ */
.btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.45rem;
    height: 38px;
    padding: 0 16px;
    border-radius: 10px;
    border: 1px solid transparent;
    font-family: inherit;
    font-size: 0.82rem;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.15s, color 0.15s, border-color 0.15s, transform 0.15s;
    white-space: nowrap;
}

.btn--primary {
    background: linear-gradient(135deg, var(--navy) 0%, var(--blue) 100%);
    color: #fff;
    box-shadow: 0 4px 12px rgba(26, 42, 108, 0.22);
}

.btn--primary:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 20px rgba(26, 42, 108, 0.28);
}

.btn--secondary {
    background: #fff;
    color: var(--navy);
    border-color: var(--line);
}

.btn--secondary:hover {
    background: #f3f6fc;
    border-color: #c6d2ff;
}

.btn--ghost {
    background: transparent;
    color: var(--muted);
}

.btn--ghost:hover {
    background: #f3f6fc;
    color: var(--navy);
}

.btn--sm {
    height: 32px;
    padding: 0 12px;
    font-size: 0.75rem;
}

/* ============================================================
   PREVIEW AREA (étape 1)
   ============================================================ */
.preview-area {
    display: flex;
    flex-direction: column;
    gap: 0.85rem;
}

.preview-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
    padding: 0.75rem 1rem;
    background: var(--blue-soft);
    border: 1px solid #cfe0ee;
    border-radius: var(--radius-sm);
}

.preview-toolbar__file {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    min-width: 0;
}

.preview-toolbar__file i {
    color: var(--blue);
    font-size: 1.1rem;
}

.preview-toolbar__name {
    font-size: 0.85rem;
    font-weight: 600;
    color: var(--navy);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.preview-toolbar__badge {
    padding: 0.15rem 0.55rem;
    border-radius: 20px;
    background: #fff;
    color: #1d4ed8;
    font-size: 0.68rem;
    font-weight: 700;
}

.preview-toolbar__actions {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.preview-info {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex-wrap: wrap;
    padding: 0.6rem 0.9rem;
    background: #f8fafc;
    border: 1px solid var(--line-2);
    border-radius: 10px;
    font-size: 0.78rem;
}

.preview-info__label {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    font-weight: 700;
    color: var(--navy);
    padding: 0.2rem 0.6rem;
    border-radius: 6px;
    background: #fff;
    border: 1px solid var(--line);
}

.preview-info__label i {
    color: var(--blue);
}

.preview-info__hint {
    color: var(--muted);
    flex: 1;
    min-width: 0;
}

.preview-info__hint strong {
    color: var(--ink-2);
}

.drag-mode-badge {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.25rem 0.6rem;
    border-radius: 20px;
    background: var(--blue-soft);
    color: #1d4ed8;
    font-size: 0.72rem;
    font-weight: 700;
    animation: pulseBadge 1.5s ease-in-out infinite;
}

.drag-mode-badge--add {
    background: var(--green-soft);
    color: var(--green);
}

@keyframes pulseBadge {

    0%,
    100% {
        opacity: 1;
    }

    50% {
        opacity: 0.7;
    }
}

.info-banner {
    display: flex;
    align-items: flex-start;
    gap: 0.55rem;
    padding: 0.75rem 0.9rem;
    background: var(--blue-soft);
    border-left: 3px solid var(--blue);
    border-radius: 8px;
    font-size: 0.78rem;
    color: var(--ink-2);
    line-height: 1.5;
    margin-bottom: 0.85rem;
}

.info-banner i {
    color: var(--blue);
    margin-top: 1px;
    flex: none;
    font-size: 0.9rem;
}

.info-banner strong {
    color: var(--navy);
}

/* ============================================================
   RANGE INPUTS
   ============================================================ */
.range-inputs {
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
    padding: 0.85rem;
    background: #f8fafc;
    border: 1px solid var(--line-2);
    border-radius: 10px;
    margin-bottom: 0.85rem;
}

.range-input-group {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex-wrap: wrap;
}

.range-input-label {
    min-width: 110px;
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    font-size: 0.75rem;
    font-weight: 700;
    color: var(--navy);
}

.range-input-label i {
    color: var(--blue);
}

.range-input {
    flex: 1;
    min-width: 160px;
    height: 32px;
    padding: 0 0.7rem;
    border: 1px solid var(--line);
    border-radius: 8px;
    background: #fff;
    font-family: ui-monospace, Menlo, monospace;
    font-size: 0.82rem;
    color: var(--ink);
    transition: border-color 0.15s, box-shadow 0.15s;
}

.range-input:focus {
    outline: none;
    border-color: var(--blue);
    box-shadow: 0 0 0 3px rgba(74, 108, 247, 0.14);
}

.range-input--error {
    border-color: var(--red);
    box-shadow: 0 0 0 3px rgba(166, 27, 22, 0.1);
}

.range-input-error {
    font-size: 0.72rem;
    color: var(--red);
    padding-left: 110px;
    margin-top: -0.3rem;
}

/* ============================================================
   SHEET TABS
   ============================================================ */
.sheet-tabs {
    display: flex;
    gap: 4px;
    padding: 6px;
    background: #f8fafc;
    border: 1px solid var(--line-2);
    border-radius: 10px;
    overflow-x: auto;
}

.sheet-tab {
    padding: 0.45rem 0.85rem;
    border-radius: 6px;
    border: 1px solid transparent;
    background: transparent;
    color: var(--muted);
    font-size: 0.78rem;
    font-weight: 600;
    white-space: nowrap;
    cursor: pointer;
    transition: background 0.15s, color 0.15s, border-color 0.15s;
}

.sheet-tab:hover:not(.sheet-tab--active) {
    background: #fff;
    border-color: var(--line);
    color: var(--navy);
}

.sheet-tab--active {
    background: linear-gradient(135deg, var(--navy) 0%, var(--blue) 100%);
    color: #fff;
    box-shadow: 0 2px 8px rgba(26, 42, 108, 0.15);
}

.sheet-tab--has-sel:not(.sheet-tab--active)::after {
    content: " •";
    color: var(--green);
    margin-left: 2px;
}

/* ============================================================
   GRILLE (étape 1 & 2)
   ============================================================ */
.grid-wrapper {
    border: 1px solid var(--line);
    border-radius: 10px;
    overflow: hidden;
    margin-top: 0.5rem;
}

.grid-scroll {
    overflow: auto;
    max-height: 60vh;
    user-select: none;
}

.import-grid {
    border-collapse: separate;
    border-spacing: 0;
    width: max-content;
    min-width: 100%;
    font-size: 12px;
}

.import-grid th,
.import-grid td {
    height: 30px;
    min-width: 100px;
    max-width: 280px;
    padding: 4px 9px;
    border-right: 1px solid #e5eaf0;
    border-bottom: 1px solid #e5eaf0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    text-align: left;
    background: #fff;
    color: var(--ink-2);
}

.import-grid thead .grid-col-head {
    position: sticky;
    top: 0;
    z-index: 2;
    background: #edf2f7;
    color: #334155;
    text-align: center;
    font-weight: 700;
    font-size: 11px;
    transition: background 0.12s, color 0.12s;
}

.grid-col-head--selectable {
    cursor: crosshair;
    user-select: none;
}

.grid-col-head--selectable:hover {
    background: var(--blue-soft) !important;
    color: var(--blue) !important;
}

.grid-col-head--selected {
    background: linear-gradient(180deg, #cfe2ff 0%, #b8d4ff 100%) !important;
    color: var(--navy) !important;
    font-weight: 800;
}

.grid-corner {
    position: sticky;
    top: 0;
    left: 0;
    z-index: 4;
    min-width: 44px !important;
    max-width: 44px !important;
    width: 44px;
    background: #edf2f7 !important;
}

.grid-row-head {
    position: sticky;
    left: 0;
    z-index: 1;
    min-width: 44px !important;
    max-width: 44px !important;
    width: 44px;
    text-align: center !important;
    font-size: 11px;
    font-weight: 600;
    background: #f1f5f9 !important;
    color: #64748b;
    transition: background 0.12s, color 0.12s;
}

.grid-row-head--clickable {
    cursor: pointer;
    user-select: none;
}

.grid-row-head--clickable:hover {
    background: var(--blue-soft) !important;
    color: var(--blue) !important;
}

.grid-row-head--selectable {
    cursor: crosshair;
    user-select: none;
}

.grid-row-head--selectable:hover {
    background: var(--blue-soft) !important;
    color: var(--blue) !important;
}

.grid-row-head--header {
    background: var(--gold-soft) !important;
    color: #92400e !important;
}

.grid-row-head--selected {
    background: linear-gradient(90deg, #cfe2ff 0%, #b8d4ff 100%) !important;
    color: var(--navy) !important;
    font-weight: 800;
}

.grid-cell {
    transition: background-color 0.1s ease;
    background: #fff;
}

.grid-cell--header {
    background: #fff8df !important;
}

.grid-cell--selected {
    background: rgba(74, 108, 247, 0.08);
}

.grid-cell--preview {
    background: rgba(74, 108, 247, 0.18) !important;
    transition: none;
}

/* ============================================================
   SÉLECTION — chips récap
   ============================================================ */
.selection-list {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    padding: 0.85rem 0 0;
}

.selection-group {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex-wrap: wrap;
}

.selection-group__title {
    min-width: 100px;
    font-size: 0.78rem;
    color: var(--navy);
    font-weight: 700;
}

.selection-chip {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.35rem 0.55rem 0.35rem 0.65rem;
    background: var(--blue-soft);
    color: #1d4ed8;
    border: 1px solid #bfdbfe;
    border-radius: 6px;
    font-size: 0.75rem;
    font-weight: 600;
}

.selection-chip i {
    font-size: 0.72rem;
}

.selection-chip__remove {
    display: grid;
    place-items: center;
    width: 16px;
    height: 16px;
    border: none;
    border-radius: 50%;
    background: transparent;
    color: #1d4ed8;
    font-size: 0.9rem;
    cursor: pointer;
    transition: background 0.15s;
}

.selection-chip__remove:hover {
    background: rgba(29, 78, 216, 0.14);
}

/* ============================================================
   STATUS BAR
   ============================================================ */
.status-bar {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex-wrap: wrap;
    padding: 0.75rem 0.9rem;
    background: #f8fafc;
    border: 1px solid var(--line-2);
    border-radius: 10px;
    margin-top: 0.75rem;
    font-size: 0.78rem;
}

.status-bar__item {
    color: var(--ink-2);
    font-weight: 600;
}

.status-bar__item--muted {
    color: var(--soft);
    font-weight: 400;
}

.status-bar__spacer {
    flex: 1;
}

/* ============================================================
   MAPPING TABLE
   ============================================================ */
.mapping-wrap {
    border: 1px solid var(--line);
    border-radius: 10px;
    overflow: auto;
}

.mapping-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.82rem;
}

.mapping-table thead th {
    padding: 0.7rem 0.85rem;
    text-align: left;
    font-size: 0.68rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--soft);
    background: #f8fafc;
    border-bottom: 1px solid var(--line);
    white-space: nowrap;
}

.mapping-table tbody td {
    padding: 0.7rem 0.85rem;
    border-bottom: 1px solid var(--line-2);
    vertical-align: middle;
}

.mapping-row:last-child td {
    border-bottom: none;
}

.mapping-row:hover td {
    background: #f8fafc;
}

.mapping-row td:first-child {
    box-shadow: none;
    transition: box-shadow 0.15s;
}

.mapping-row--activite td:first-child {
    box-shadow: inset 3px 0 0 0 var(--blue);
}

.mapping-row--objectif td:first-child {
    box-shadow: inset 3px 0 0 0 var(--amber);
}

.mapping-row--sous-activite td:first-child {
    box-shadow: inset 3px 0 0 0 var(--green);
}

.mapping-row--livrable td:first-child {
    box-shadow: inset 3px 0 0 0 var(--purple);
}

.mapping-row--resultat td:first-child {
    box-shadow: inset 3px 0 0 0 var(--pink);
}

.mapping-row--indicateur td:first-child {
    box-shadow: inset 3px 0 0 0 var(--cyan);
}

.mapping-row--origine td:first-child {
    box-shadow: inset 3px 0 0 0 var(--orange);
}

.mapping-row--cle td:first-child {
    box-shadow: inset 3px 0 0 0 var(--slate);
}

.mapping-row--empty td:first-child {
    box-shadow: inset 3px 0 0 0 #e5eaf0;
}

.mapping-sheet {
    font-weight: 600;
    color: var(--navy);
}

.mapping-letter {
    font-family: ui-monospace, Menlo, monospace;
    font-weight: 700;
    color: var(--blue);
}

.mapping-name {
    color: var(--ink-2);
}

.mapping-preview {
    color: var(--muted);
    max-width: 260px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 0.78rem;
    font-family: ui-monospace, Menlo, monospace;
}

.mapping-field {
    min-width: 260px;
}

.mapping-select {
    width: 100%;
    min-width: 240px;
    padding: 0.45rem 0.6rem;
    border: 1px solid var(--line);
    border-radius: 6px;
    background: #fff;
    font-size: 0.8rem;
    color: var(--ink);
    font-family: inherit;
    cursor: pointer;
    transition: border-color 0.15s, background 0.15s;
}

.mapping-select:hover {
    border-color: #c6d2ff;
}

.mapping-select--filled {
    border-color: var(--blue);
    background: var(--blue-soft);
    color: var(--navy);
    font-weight: 600;
}

.mapping-field-meta {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    flex-wrap: wrap;
    margin-top: 0.35rem;
}

.mapping-field-path {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
    padding: 0.15rem 0.5rem;
    border-radius: 5px;
    background: #f1f5f9;
    color: var(--soft);
    font-size: 0.68rem;
    font-family: ui-monospace, Menlo, monospace;
}

.mapping-field-path i {
    font-size: 0.65rem;
}

.mapping-badge {
    display: inline-block;
    padding: 0.15rem 0.5rem;
    border-radius: 20px;
    font-size: 0.65rem;
    font-weight: 700;
    letter-spacing: 0.02em;
    white-space: nowrap;
}

.mapping-badge--required {
    background: var(--red-soft);
    color: var(--red);
    border: 1px solid #f6cfcc;
}

.mapping-badge--fill {
    background: var(--gold-soft);
    color: var(--amber);
    border: 1px solid #f4e2c2;
}

.mapping-order-col {
    width: 80px;
    text-align: center;
}

.order-controls {
    display: inline-flex;
    gap: 4px;
}

.order-controls button {
    width: 26px;
    height: 26px;
    display: grid;
    place-items: center;
    border: 1px solid var(--line);
    border-radius: 6px;
    background: #fff;
    color: var(--ink-2);
    font-size: 0.72rem;
    cursor: pointer;
    transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.order-controls button:hover:not(:disabled) {
    background: var(--blue-soft);
    border-color: #93c5fd;
    color: var(--blue);
}

.order-controls button:disabled {
    opacity: 0.4;
    cursor: not-allowed;
}

.hint-row {
    display: flex;
    align-items: center;
    gap: 1.25rem;
    flex-wrap: wrap;
    padding: 0.65rem 0;
    font-size: 0.75rem;
    color: var(--muted);
}

.hint-row__item {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
}

.hint-row__mark {
    display: inline-grid;
    place-items: center;
    min-width: 18px;
    height: 18px;
    padding: 0 0.35rem;
    border-radius: 4px;
    background: var(--blue-soft);
    color: #1d4ed8;
    font-weight: 800;
    font-size: 0.7rem;
}

/* ============================================================
   CONFIGURATION (étape 4)
   ============================================================ */
.config-form {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.config-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1rem 1.25rem;
}

.config-field {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
}

.config-field--full {
    grid-column: 1 / -1;
}

.config-label {
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    color: var(--muted);
}

.config-required {
    color: var(--red);
    margin-left: 1px;
}

.config-input {
    height: 42px;
    padding: 0 0.9rem;
    border: 1px solid var(--line);
    border-radius: 10px;
    background: #fff;
    font-family: inherit;
    font-size: 0.88rem;
    color: var(--ink);
    transition: border-color 0.15s, box-shadow 0.15s;
}

.config-input:hover:not(:disabled) {
    border-color: #c6d2ff;
}

.config-input:focus {
    outline: none;
    border-color: var(--blue);
    box-shadow: 0 0 0 3px rgba(74, 108, 247, 0.14);
}

.config-input:disabled {
    background: #f8fafc;
    cursor: not-allowed;
    opacity: 0.7;
}

select.config-input {
    cursor: pointer;
    appearance: none;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2.4' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: right 14px center;
    padding-right: 36px;
}

.config-hint {
    margin: 0;
    font-size: 0.7rem;
    color: var(--soft);
    line-height: 1.45;
}

.config-radio-group {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0.75rem;
}

.config-radio {
    position: relative;
    display: flex;
    align-items: flex-start;
    gap: 0.7rem;
    padding: 0.85rem 1rem;
    border: 1.5px solid var(--line);
    border-radius: 12px;
    background: #fff;
    cursor: pointer;
    transition: border-color 0.15s, background 0.15s, box-shadow 0.15s;
}

.config-radio:hover {
    border-color: #c6d2ff;
    background: #f8fafc;
}

.config-radio--active {
    border-color: var(--blue);
    background: var(--blue-soft);
    box-shadow: 0 0 0 3px rgba(74, 108, 247, 0.12);
}

.config-radio input[type="radio"] {
    position: absolute;
    opacity: 0;
    pointer-events: none;
}

.config-radio__dot {
    flex: none;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    border: 2px solid #cbd5e1;
    background: #fff;
    margin-top: 2px;
    position: relative;
    transition: border-color 0.15s;
}

.config-radio__dot::after {
    content: "";
    position: absolute;
    inset: 3px;
    border-radius: 50%;
    background: var(--blue);
    transform: scale(0);
    transition: transform 0.15s;
}

.config-radio--active .config-radio__dot {
    border-color: var(--blue);
}

.config-radio--active .config-radio__dot::after {
    transform: scale(1);
}

.config-radio__body {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
    min-width: 0;
}

.config-radio__body strong {
    font-size: 0.88rem;
    font-weight: 700;
    color: var(--navy);
}

.config-radio__body small {
    font-size: 0.72rem;
    color: var(--muted);
    line-height: 1.4;
}

.config-switch {
    display: flex;
    align-items: flex-start;
    gap: 0.85rem;
    padding: 0.85rem 1rem;
    border: 1px solid var(--line);
    border-radius: 12px;
    background: #fff;
    cursor: pointer;
    transition: border-color 0.15s, background 0.15s;
}

.config-switch:hover {
    border-color: #c6d2ff;
    background: #f8fafc;
}

.config-switch input[type="checkbox"] {
    position: absolute;
    opacity: 0;
    pointer-events: none;
}

.config-switch__track {
    flex: none;
    position: relative;
    width: 40px;
    height: 22px;
    border-radius: 20px;
    background: #cbd5e1;
    transition: background 0.2s;
    margin-top: 2px;
}

.config-switch__thumb {
    position: absolute;
    top: 3px;
    left: 3px;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: #fff;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
    transition: transform 0.2s;
}

.config-switch input:checked~.config-switch__track {
    background: var(--blue);
}

.config-switch input:checked~.config-switch__track .config-switch__thumb {
    transform: translateX(18px);
}

.config-switch__text {
    display: flex;
    flex-direction: column;
    gap: 0.2rem;
    min-width: 0;
}

.config-switch__text strong {
    font-size: 0.88rem;
    font-weight: 700;
    color: var(--navy);
}

.config-switch__text small {
    font-size: 0.72rem;
    color: var(--muted);
    line-height: 1.4;
}

.config-switch__text code {
    padding: 0.1rem 0.35rem;
    background: #f1f5f9;
    border-radius: 3px;
    font-size: 0.7rem;
    color: var(--ink-2);
}

.config-summary {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 0.75rem;
    margin-top: 1rem;
    padding: 0.85rem 1rem;
    background: #f8fafc;
    border: 1px solid var(--line-2);
    border-radius: 12px;
}

.config-summary__item {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
}

.config-summary__label {
    font-size: 0.68rem;
    font-weight: 700;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    color: var(--soft);
}

.config-summary__value {
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 1.15rem;
    font-weight: 800;
    color: var(--navy);
    font-variant-numeric: tabular-nums;
}

.config-summary__value--sm {
    font-size: 0.85rem;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.config-recap {
    display: flex;
    flex-wrap: wrap;
    gap: 0.65rem;
    padding: 0.75rem 0.9rem;
    margin-bottom: 0.85rem;
    background: #f8fafc;
    border: 1px solid var(--line-2);
    border-radius: 10px;
    font-size: 0.78rem;
    color: var(--ink-2);
}

.config-recap__item {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.25rem 0.6rem;
    background: #fff;
    border: 1px solid var(--line);
    border-radius: 6px;
}

.config-recap__item i {
    color: var(--blue);
    font-size: 0.8rem;
}

.config-recap__item strong {
    color: var(--navy);
    font-weight: 700;
}

/* ============================================================
   RÉSULTAT (étape 5)
   ============================================================ */
.result-wrap {
    border: 1px solid var(--line);
    border-radius: 10px;
    overflow: auto;
    max-height: 60vh;
}

.result-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.82rem;
}

.result-table thead th {
    padding: 0.7rem 0.85rem;
    text-align: left;
    font-size: 0.68rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--soft);
    background: #f8fafc;
    border-bottom: 1px solid var(--line);
    white-space: nowrap;
    position: sticky;
    top: 0;
    z-index: 1;
}

.result-table tbody td {
    padding: 0.65rem 0.85rem;
    border-bottom: 1px solid var(--line-2);
    color: var(--ink);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 260px;
}

.result-table tbody tr:last-child td {
    border-bottom: none;
}

.result-table tbody tr:hover td {
    background: #f8fafc;
}

/* ============================================================
   ALERTES / EMPTY STATE
   ============================================================ */
.alert {
    display: flex;
    align-items: flex-start;
    gap: 0.55rem;
    padding: 0.75rem 0.9rem;
    border-radius: 10px;
    font-size: 0.82rem;
    line-height: 1.5;
}

.alert i {
    flex: none;
    margin-top: 1px;
    font-size: 0.95rem;
}

.alert--danger {
    background: var(--red-soft);
    border: 1px solid #fecaca;
    color: var(--red);
}

.alert--danger i {
    color: var(--red);
}

.validation-report {
    display: flex;
    flex-direction: column;
    gap: 0.65rem;
    margin-bottom: 0.85rem;
}

.validation-report__summary,
.import-success {
    display: flex;
    align-items: flex-start;
    gap: 0.65rem;
    padding: 0.8rem 0.95rem;
    border: 1px solid var(--line);
    border-radius: 10px;
    font-size: 0.82rem;
    line-height: 1.5;
}

.validation-report__summary>i,
.import-success>i {
    flex: none;
    margin-top: 2px;
}

.validation-report__summary>div,
.import-success>div {
    display: flex;
    flex-direction: column;
    gap: 0.1rem;
}

.validation-report__summary--valid,
.import-success {
    background: var(--green-soft);
    border-color: #b7dfc1;
    color: var(--green);
}

.validation-report__summary--invalid {
    background: var(--red-soft);
    border-color: #fecaca;
    color: var(--red);
}

.validation-report__summary span,
.import-success span {
    color: var(--ink-2);
}

.validation-report__issues {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
}

.validation-report__issues strong {
    margin-bottom: 0.15rem;
}

.validation-report__warnings {
    margin: 0;
}

.import-success {
    margin-bottom: 0.85rem;
}

.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 3rem 1.5rem;
    text-align: center;
    color: var(--soft);
    background: #f8fafc;
    border: 1px dashed #dde4ec;
    border-radius: 10px;
}

.empty-state i {
    font-size: 2rem;
    opacity: 0.5;
    margin-bottom: 0.5rem;
}

.empty-state p {
    margin: 0;
    font-size: 0.85rem;
}

/* ============================================================
   BARRE D'ACTIONS COMMUNE
   ============================================================ */
.form-actions-bar {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    flex-wrap: wrap;
    padding: 0.85rem 0 0;
    margin-top: 0.85rem;
    border-top: 1px solid var(--line-2);
}

/* ============================================================
   MODAL D'IMPORT
   ============================================================ */
.import-modal {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 0.65rem;
}

.import-modal__icon {
    display: grid;
    place-items: center;
    width: 64px;
    height: 64px;
    border-radius: 18px;
    background: var(--blue-soft);
    color: var(--blue);
    font-size: 1.85rem;
    margin-bottom: 0.35rem;
}

.import-modal__title {
    margin: 0;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 1.15rem;
    font-weight: 800;
    color: var(--navy);
}

.import-modal__text {
    margin: 0;
    font-size: 0.85rem;
    color: var(--ink-2);
    line-height: 1.55;
    max-width: 380px;
}

.import-modal__text strong {
    color: var(--navy);
    font-weight: 700;
}

.import-modal__recap {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 0.1rem;
    margin-top: 0.75rem;
    padding: 0.75rem 1rem;
    background: #f8fafc;
    border-radius: 10px;
    text-align: left;
}

.import-modal__recap-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0.45rem 0;
    font-size: 0.82rem;
    border-bottom: 1px dashed var(--line-2);
}

.import-modal__recap-row:last-child {
    border-bottom: none;
}

.import-modal__recap-row span {
    color: var(--muted);
}

.import-modal__recap-row strong {
    color: var(--navy);
    font-weight: 700;
}

/* ============================================================
   PRÉVISUALISATION & SÉLECTION PLEIN ÉCRAN
   ============================================================ */
.preview-fs {
    position: fixed;
    inset: 0;
    z-index: 9999;
    display: flex;
    flex-direction: column;
    background: #f1f5f9;
    color: var(--ink);
    font-family: "DM Sans", "Segoe UI", sans-serif;
}

.preview-fs__header {
    display: flex;
    align-items: center;
    gap: 1rem;
    flex-wrap: wrap;
    padding: 0.75rem 1.15rem;
    background: #fff;
    border-bottom: 1px solid var(--line);
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06);
}

.preview-fs__file {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    min-width: 0;
    flex: 1;
}

.preview-fs__file i {
    color: var(--blue);
    font-size: 1.1rem;
}

.preview-fs__name {
    font-size: 0.9rem;
    font-weight: 700;
    color: var(--navy);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.preview-fs__badge {
    padding: 0.15rem 0.55rem;
    border-radius: 20px;
    background: var(--blue-soft);
    color: #1d4ed8;
    font-size: 0.68rem;
    font-weight: 700;
}

.preview-fs__info {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.35rem 0.7rem;
    background: #f8fafc;
    border: 1px solid var(--line);
    border-radius: 8px;
    font-size: 0.78rem;
    color: var(--ink-2);
    font-weight: 600;
}

.preview-fs__info i {
    color: var(--blue);
}

.preview-fs__info-clear {
    display: inline-flex;
    align-items: center;
    gap: 0.25rem;
    margin-left: 0.35rem;
    padding: 0.15rem 0.4rem;
    border: none;
    border-radius: 4px;
    background: transparent;
    color: var(--red);
    font-size: 0.72rem;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.15s;
}

.preview-fs__info-clear:hover {
    background: var(--red-soft);
}

/* Bouton « Saisie rapide » dans le header FS */
.preview-fs__btn {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    height: 36px;
    padding: 0 0.9rem;
    border: 1px solid var(--line);
    border-radius: 8px;
    background: #fff;
    color: var(--ink-2);
    font-size: 0.8rem;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.15s, color 0.15s, border-color 0.15s, transform 0.15s;
}

.preview-fs__btn:hover {
    border-color: #c6d2ff;
    color: var(--navy);
}

.preview-fs__btn--primary {
    background: linear-gradient(135deg, var(--navy) 0%, var(--blue) 100%);
    border-color: transparent;
    color: #fff;
    box-shadow: 0 4px 12px rgba(26, 42, 108, 0.22);
}

.preview-fs__btn--primary:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 20px rgba(26, 42, 108, 0.28);
    color: #fff;
}

.preview-fs__close {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    height: 36px;
    padding: 0 0.9rem;
    border: 1px solid var(--line);
    border-radius: 8px;
    background: #fff;
    color: var(--ink-2);
    font-size: 0.8rem;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.15s, color 0.15s, border-color 0.15s;
}

.preview-fs__close:hover {
    background: var(--red-soft);
    color: var(--red);
    border-color: #fecaca;
}

.preview-fs__close-label {
    font-size: 0.78rem;
}

.preview-fs__grid {
    flex: 1;
    overflow: auto;
    background: #fff;
}

.preview-fs__grid--selectable {
    user-select: none;
    cursor: crosshair;
}

.preview-fs__grid--selectable .import-grid td,
.preview-fs__grid--selectable .import-grid th {
    cursor: crosshair;
}

.preview-fs__grid .import-grid {
    width: max-content;
    min-width: 100%;
}

/* Footer dans la sélection plein écran */
.preview-fs__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
    padding: 0.75rem 1.15rem;
    background: #fff;
    border-top: 1px solid var(--line);
    box-shadow: 0 -1px 3px rgba(15, 23, 42, 0.05);
}

.preview-fs__footer-info {
    display: flex;
    align-items: center;
    gap: 0.65rem;
    flex-wrap: wrap;
    font-size: 0.78rem;
}

.preview-fs__footer-label {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.3rem 0.65rem;
    background: #f8fafc;
    border: 1px solid var(--line);
    border-radius: 6px;
    font-weight: 700;
    color: var(--navy);
}

.preview-fs__footer-label i {
    color: var(--blue);
}

.preview-fs__footer-muted {
    color: var(--soft);
    font-size: 0.78rem;
}

.preview-fs__footer-actions {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex-wrap: wrap;
}

.preview-fs__tabs {
    display: flex;
    gap: 4px;
    padding: 6px 1rem;
    background: #fff;
    border-top: 1px solid var(--line);
    overflow-x: auto;
}

.preview-fs__tab {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.45rem 0.85rem;
    border-radius: 6px;
    border: 1px solid transparent;
    background: transparent;
    color: var(--muted);
    font-size: 0.78rem;
    font-weight: 600;
    white-space: nowrap;
    cursor: pointer;
    transition: background 0.15s, color 0.15s, border-color 0.15s;
}

.preview-fs__tab i {
    font-size: 0.7rem;
}

.preview-fs__tab:hover:not(.preview-fs__tab--active) {
    background: #f8fafc;
    border-color: var(--line);
    color: var(--navy);
}

.preview-fs__tab--active {
    background: linear-gradient(135deg, var(--navy) 0%, var(--blue) 100%);
    color: #fff;
    box-shadow: 0 2px 8px rgba(26, 42, 108, 0.15);
}

.preview-fs__tab--has-sel:not(.preview-fs__tab--active)::after {
    content: " •";
    color: var(--green);
    margin-left: 2px;
    font-weight: 800;
}

/* ============================================================
   MODAL « SAISIE RAPIDE » (dans le FS sélection)
   ============================================================ */
.range-quick-overlay {
    position: fixed;
    inset: 0;
    z-index: 10000;
    background: rgba(15, 23, 42, 0.55);
    backdrop-filter: blur(2px);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 1.5rem;
    animation: rangeQuickFadeIn 0.15s ease-out;
}

@keyframes rangeQuickFadeIn {
    from {
        opacity: 0;
    }

    to {
        opacity: 1;
    }
}

.range-quick-modal {
    width: 100%;
    max-width: 480px;
    max-height: calc(100vh - 3rem);
    display: flex;
    flex-direction: column;
    background: #fff;
    border-radius: var(--radius);
    box-shadow: 0 20px 60px rgba(15, 23, 42, 0.35);
    overflow: hidden;
    animation: rangeQuickScaleIn 0.18s ease-out;
}

@keyframes rangeQuickScaleIn {
    from {
        transform: scale(0.96);
        opacity: 0;
    }

    to {
        transform: scale(1);
        opacity: 1;
    }
}

.range-quick-modal__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    padding: 0.85rem 1.15rem;
    background: linear-gradient(140deg, #f7f9fd, #fff);
    border-bottom: 1px solid var(--line);
}

.range-quick-modal__title {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 0.95rem;
    font-weight: 800;
    color: var(--navy);
}

.range-quick-modal__title i {
    color: var(--gold);
    font-size: 1rem;
}

.range-quick-modal__sheet {
    display: inline-block;
    margin-left: 0.4rem;
    padding: 0.15rem 0.55rem;
    border-radius: 20px;
    background: var(--blue-soft);
    color: #1d4ed8;
    font-size: 0.7rem;
    font-weight: 700;
    font-family: "DM Sans", "Segoe UI", sans-serif;
}

.range-quick-modal__body {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    padding: 1rem 1.15rem 1.15rem;
    overflow-y: auto;
}

.range-quick-modal__actions {
    display: flex;
    gap: 0.5rem;
    justify-content: flex-end;
    margin-top: 0.35rem;
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 991.98px) {

    .config-grid,
    .config-summary,
    .config-radio-group {
        grid-template-columns: 1fr;
    }

    .import-target__options {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .preview-fs__footer {
        flex-direction: column;
        align-items: stretch;
    }

    .preview-fs__footer-actions {
        justify-content: flex-end;
    }
}

@media (max-width: 767.98px) {
    .import-page {
        padding: 24px 16px 20px;
    }

    .import-stepper {
        padding: 0.5rem;
        gap: 0.25rem;
    }

    .import-stepper__item {
        flex: 0 0 auto;
        padding: 0.4rem 0.6rem;
        font-size: 0.72rem;
    }

    .import-stepper__label {
        display: none;
    }

    .preview-toolbar,
    .status-bar,
    .form-actions-bar {
        flex-direction: column;
        align-items: stretch;
    }

    .grid-scroll {
        max-height: 50vh;
    }

    .range-input-label {
        min-width: auto;
    }

    .range-input-error {
        padding-left: 0;
    }

    .import-target__options {
        grid-template-columns: 1fr;
    }

    .preview-fs__close-label {
        display: none;
    }

    .preview-fs__header {
        gap: 0.5rem;
        padding: 0.65rem 0.85rem;
    }

    .preview-fs__btn span {
        display: none;
    }

    .preview-fs__footer {
        padding: 0.65rem 0.85rem;
    }

    .preview-fs__footer-actions {
        justify-content: stretch;
    }

    .preview-fs__footer-actions>* {
        flex: 1;
    }

    .range-quick-overlay {
        padding: 0.75rem;
        align-items: flex-end;
    }

    .range-quick-modal {
        max-height: calc(100vh - 1.5rem);
        border-radius: 16px 16px 0 0;
    }
}

@media (prefers-reduced-motion: reduce) {
    * {
        transition: none !important;
        animation: none !important;
    }
}
</style>

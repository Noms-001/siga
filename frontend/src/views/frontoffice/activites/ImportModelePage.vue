<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BaseButton, BaseModal } from '@/components/base'
import { listerServices } from '@/services/service'
import { confirmerImport, validerImport } from '@/services/importActivite'
import type { Service } from '@/types/backoffice/service'
import type { ImportResult, ImportValidationReport } from '@/types/importActivite'
import {
    buildImportTree,
    type ImportTreeResult,
    type MappedFieldInput,
} from '@/utils/importTree'

const router = useRouter()

/* ---------- Types ---------- */
type LotType = 'PTA' | 'NON_PTA'
type LotStatus = 'idle' | 'ready' | 'validating' | 'valid' | 'invalid' | 'importing' | 'imported' | 'error'

interface Lot {
    type: LotType
    tree: ImportTreeResult | null
    status: LotStatus
    report: ImportValidationReport | null
    result: ImportResult | null
    error: string | null
    nbActivites: number
    nbSa: number
    nbLivrables: number
    nbResultats: number
    nbIndicateurs: number
}

function emptyLot(type: LotType): Lot {
    return {
        type,
        tree: null,
        status: 'idle',
        report: null,
        result: null,
        error: null,
        nbActivites: 0,
        nbSa: 0,
        nbLivrables: 0,
        nbResultats: 0,
        nbIndicateurs: 0,
    }
}

/* ---------- State ---------- */
const fileName = ref('')
const parsing = ref(false)
const parsed = ref(false)
const parseErrors = ref<string[]>([])
const parseWarnings = ref<string[]>([])
const isDragover = ref(false)

const ptaLot = ref<Lot>(emptyLot('PTA'))
const nonPtaLot = ref<Lot>(emptyLot('NON_PTA'))

const summary = ref({
    os: 0,
    activites: 0,
    pta: 0,
    nonPta: 0,
    sa: 0,
    livrables: 0,
    resultats: 0,
    indicateurs: 0,
})

const services = ref<Service[]>([])
const loadingServices = ref(false)

const config = ref({
    version: '1.0',
    service: null as number | null,
    procedure: 1,
    annee: 2026,
    activitesDejaValidees: false,
})

const validatingImport = ref(false)
const importing = ref(false)
const showModal = ref(false)
const errors = ref<string[]>([])

/* ---------- Helpers ---------- */
async function getXLSX(): Promise<any> {
    const w = window as any
    if (w.XLSX) return w.XLSX
    const mod = await import('xlsx')
    return mod.default || mod
}

function readSheet(wb: any, XLSX: any, name: string): any[][] {
    const ws = wb.Sheets[name]
    if (!ws) return []
    return XLSX.utils.sheet_to_json(ws, { header: 1, defval: '', raw: true, blankrows: true })
}

/* ---------- Parser du template ---------- */
interface ParsedTemplate {
    activityRows: Record<string, any>[]
    saRows: Record<string, any>[]
    livRows: Record<string, any>[]
    riRows: Record<string, any>[]
    indRows: Record<string, any>[]
    errors: string[]
    warnings: string[]
    summary: typeof summary.value
}

function parseTemplateWorkbook(wb: any, XLSX: any): ParsedTemplate {
    const errs: string[] = []
    const warnings: string[] = []

    // 1. Objectifs Spécifiques
    const osSheet = readSheet(wb, XLSX, 'Objectif Spécifique')
    if (!osSheet.length) errs.push('Feuille « Objectif Spécifique » introuvable.')
    const osMap = new Map<string, string>()
    for (let i = 4; i < osSheet.length; i++) {
        const code = String(osSheet[i]?.[0] ?? '').trim()
        const designation = String(osSheet[i]?.[1] ?? '').trim()
        if (code) osMap.set(code, designation)
    }

    // 2. Activités
    const actSheet = readSheet(wb, XLSX, 'Activité')
    if (!actSheet.length) errs.push('Feuille « Activité » introuvable.')
    const activityRows: Record<string, any>[] = []
    let ptaCount = 0
    for (let i = 4; i < actSheet.length; i++) {
        const row = actSheet[i] ?? []
        const codeActivite = String(row[4] ?? '').trim()
        const designation = String(row[5] ?? '').trim()
        if (!codeActivite || !designation) continue

        const estPTA = String(row[1] ?? '').trim().toUpperCase() === 'OUI'
        const codeOS = String(row[0] ?? '').trim()

        const out: Record<string, any> = {
            '_referenceActivite': codeActivite,
            'designation': designation,
        }
        if (row[6]) out['priorite'] = row[6]
        if (row[7]) out['typeActivite'] = row[7]
        if (row[8]) out['site'] = row[8]

        if (estPTA) {
            if (codeOS) {
                out['objectifSpecifique.code'] = codeOS
                const osDes = osMap.get(codeOS)
                if (osDes) out['objectifSpecifique.designation'] = osDes
                ptaCount++
            } else {
                warnings.push(
                    `Activité « ${codeActivite} » marquée PTA=OUI sans code OS — sera traitée comme NON-PTA.`
                )
            }
        } else if (codeOS) {
            warnings.push(
                `Activité « ${codeActivite} » marquée PTA=NON avec un code OS renseigné — le code OS est ignoré.`
            )
        }
        activityRows.push(out)
    }
    if (activityRows.length === 0) errs.push('Aucune activité valide dans la feuille « Activité ».')

    // 3. Sous-activités
    const saSheet = readSheet(wb, XLSX, 'Sous-Activité')
    const saRows: Record<string, any>[] = []
    for (let i = 4; i < saSheet.length; i++) {
        const row = saSheet[i] ?? []
        const codeActivite = String(row[0] ?? '').trim()
        const designation = String(row[3] ?? '').trim()
        const codeSA = String(row[2] ?? '').trim()
        if (!codeActivite || !designation) continue
        const out: Record<string, any> = {
            '_referenceActivite': codeActivite,
            'sousActivites[].designation': designation,
        }
        if (codeSA) out['_referenceSousActivite'] = codeSA
        if (row[4]) out['sousActivites[].dateDebut'] = row[4]
        if (row[5]) out['sousActivites[].dateFin'] = row[5]
        saRows.push(out)
    }

    // 4. Livrables
    const livSheet = readSheet(wb, XLSX, 'Livrable')
    const livRows: Record<string, any>[] = []
    for (let i = 4; i < livSheet.length; i++) {
        const row = livSheet[i] ?? []
        const codeSA = String(row[0] ?? '').trim()
        const designation = String(row[1] ?? '').trim()
        if (!codeSA || !designation) continue
        const out: Record<string, any> = {
            '_referenceSousActivite': codeSA,
            'sousActivites[].livrables[].designation': designation,
        }
        if (row[2]) out['sousActivites[].livrables[].description'] = row[2]
        livRows.push(out)
    }

    // 5. Résultats
    const riSheet = readSheet(wb, XLSX, 'Résultat Intermédiaire')
    const riRows: Record<string, any>[] = []
    for (let i = 4; i < riSheet.length; i++) {
        const row = riSheet[i] ?? []
        const codeActivite = String(row[0] ?? '').trim()
        const designation = String(row[1] ?? '').trim()
        if (!codeActivite || !designation) continue
        riRows.push({
            '_referenceActivite': codeActivite,
            'resultatsIntermediaires[].designation': designation,
        })
    }

    // 6. Indicateurs
    const indSheet = readSheet(wb, XLSX, 'Indicateur')
    const indRows: Record<string, any>[] = []
    for (let i = 4; i < indSheet.length; i++) {
        const row = indSheet[i] ?? []
        const codeActivite = String(row[0] ?? '').trim()
        const designation = String(row[1] ?? '').trim()
        if (!codeActivite || !designation) continue
        indRows.push({
            '_referenceActivite': codeActivite,
            'indicateurs[].designation': designation,
        })
    }

    return {
        activityRows,
        saRows,
        livRows,
        riRows,
        indRows,
        errors: errs,
        warnings,
        summary: {
            os: osMap.size,
            activites: activityRows.length,
            pta: ptaCount,
            nonPta: activityRows.length - ptaCount,
            sa: saRows.length,
            livrables: livRows.length,
            resultats: riRows.length,
            indicateurs: indRows.length,
        },
    }
}

/* ---------- Split PTA / NON-PTA ---------- */
interface SplitSet {
    activities: Record<string, any>[]
    sas: Record<string, any>[]
    livrables: Record<string, any>[]
    resultats: Record<string, any>[]
    indicateurs: Record<string, any>[]
}

function splitParsedByType(parsed: ParsedTemplate) {
    const activityType = new Map<string, LotType>()
    const ptaActivities: Record<string, any>[] = []
    const nonPtaActivities: Record<string, any>[] = []

    for (const r of parsed.activityRows) {
        const ref = String(r['_referenceActivite'] ?? '').trim()
        if (!ref) continue
        const isPta = Object.keys(r).some(k => k.startsWith('objectifSpecifique.'))
        activityType.set(ref, isPta ? 'PTA' : 'NON_PTA')
        if (isPta) ptaActivities.push(r)
        else nonPtaActivities.push(r)
    }

    const saToActivity = new Map<string, string>()
    for (const r of parsed.saRows) {
        const saRef = String(r['_referenceSousActivite'] ?? '').trim()
        const actRef = String(r['_referenceActivite'] ?? '').trim()
        if (saRef && actRef) saToActivity.set(saRef, actRef)
    }

    const partition = <T extends Record<string, any>>(
        rows: T[],
        getActRef: (r: T) => string | undefined,
    ) => {
        const pta: T[] = []
        const nonPta: T[] = []
        const orphans: T[] = []
        for (const r of rows) {
            const actRef = getActRef(r)
            const type = actRef ? activityType.get(actRef) : undefined
            if (type === 'PTA') pta.push(r)
            else if (type === 'NON_PTA') nonPta.push(r)
            else orphans.push(r)
        }
        return { pta, nonPta, orphans }
    }

    const saSplit = partition(parsed.saRows, r => String(r['_referenceActivite'] ?? '').trim() || undefined)
    const livSplit = partition(parsed.livRows, r => {
        const saRef = String(r['_referenceSousActivite'] ?? '').trim()
        return saRef ? saToActivity.get(saRef) : undefined
    })
    const riSplit = partition(parsed.riRows, r => String(r['_referenceActivite'] ?? '').trim() || undefined)
    const indSplit = partition(parsed.indRows, r => String(r['_referenceActivite'] ?? '').trim() || undefined)

    const pta: SplitSet = {
        activities: ptaActivities,
        sas: saSplit.pta,
        livrables: livSplit.pta,
        resultats: riSplit.pta,
        indicateurs: indSplit.pta,
    }
    const nonPta: SplitSet = {
        activities: nonPtaActivities,
        sas: saSplit.nonPta,
        livrables: livSplit.nonPta,
        resultats: riSplit.nonPta,
        indicateurs: indSplit.nonPta,
    }

    const orphanWarnings: string[] = []
    const countOrphans = saSplit.orphans.length + livSplit.orphans.length + riSplit.orphans.length + indSplit.orphans.length
    if (countOrphans > 0) {
        orphanWarnings.push(
            `${countOrphans} ligne(s) enfant(s) sans activité parente reconnue (référence introuvable ou activité absente du fichier) — ignorée(s).`
        )
    }

    return { pta, nonPta, orphanWarnings }
}

/* ---------- Construction de l'arbre pour un lot ---------- */
function buildTreeFromSplit(split: SplitSet): ImportTreeResult | null {
    if (!split.activities.length) return null

    const rowsBySheet = new Map<string, Record<string, any>[]>()
    const mapped: MappedFieldInput[] = []
    const seen = new Set<string>()

    const add = (sheet: string, rows: Record<string, any>[]) => {
        if (!rows.length) return
        rowsBySheet.set(sheet, rows)
        for (const row of rows) {
            for (const k of Object.keys(row)) {
                const key = `${sheet}|${k}`
                if (seen.has(key)) continue
                seen.add(key)
                mapped.push({ sheet, field: k })
            }
        }
    }

    add('Activité', split.activities)
    add('Sous-Activité', split.sas)
    add('Livrable', split.livrables)
    add('Résultat Intermédiaire', split.resultats)
    add('Indicateur', split.indicateurs)

    return buildImportTree({ cible: 'ACTIVITES', mapped, rowsBySheet })
}

function fillLotMeta(lot: Lot, split: SplitSet) {
    lot.nbActivites = split.activities.length
    lot.nbSa = split.sas.length
    lot.nbLivrables = split.livrables.length
    lot.nbResultats = split.resultats.length
    lot.nbIndicateurs = split.indicateurs.length
}

/* ---------- Upload ---------- */
async function handleFile(file: File) {
    if (!file) return
    if (!/\.(xlsx|xls|csv)$/i.test(file.name)) {
        parseErrors.value = ['Format non supporté. Utilisez le modèle .xlsx.']
        return
    }
    parsing.value = true
    parsed.value = false
    parseErrors.value = []
    parseWarnings.value = []
    ptaLot.value = emptyLot('PTA')
    nonPtaLot.value = emptyLot('NON_PTA')
    errors.value = []
    fileName.value = file.name

    try {
        const XLSX = await getXLSX()
        const buffer = await file.arrayBuffer()
        const wb = XLSX.read(new Uint8Array(buffer), { type: 'array', cellDates: true })

        const parsedTpl = parseTemplateWorkbook(wb, XLSX)
        summary.value = parsedTpl.summary
        parseWarnings.value = parsedTpl.warnings
        if (parsedTpl.errors.length) {
            parseErrors.value = parsedTpl.errors
            return
        }

        const { pta, nonPta, orphanWarnings } = splitParsedByType(parsedTpl)
        parseWarnings.value = [...parseWarnings.value, ...orphanWarnings]

        const ptaTree = buildTreeFromSplit(pta)
        const nonPtaTree = buildTreeFromSplit(nonPta)

        const treeErrors: string[] = []
        if (ptaTree?.errors.length) treeErrors.push(...ptaTree.errors.map(e => `[PTA] ${e}`))
        if (nonPtaTree?.errors.length) treeErrors.push(...nonPtaTree.errors.map(e => `[NON-PTA] ${e}`))
        if (treeErrors.length) {
            parseErrors.value = treeErrors
            return
        }

        if (ptaTree) {
            fillLotMeta(ptaLot.value, pta)
            ptaLot.value.tree = ptaTree
            ptaLot.value.status = 'ready'
        }
        if (nonPtaTree) {
            fillLotMeta(nonPtaLot.value, nonPta)
            nonPtaLot.value.tree = nonPtaTree
            nonPtaLot.value.status = 'ready'
        }

        if (ptaTree?.warnings.length) parseWarnings.value.push(...ptaTree.warnings.map(w => `[PTA] ${w}`))
        if (nonPtaTree?.warnings.length) parseWarnings.value.push(...nonPtaTree.warnings.map(w => `[NON-PTA] ${w}`))

        parsed.value = true
    } catch (e: any) {
        console.error(e)
        parseErrors.value = ['Erreur de lecture : ' + (e?.message ?? e)]
    } finally {
        parsing.value = false
    }
}

function onFileChange(e: Event) {
    const f = (e.target as HTMLInputElement).files?.[0]
    if (f) void handleFile(f)
}

function onDrop(e: DragEvent) {
    isDragover.value = false
    const f = e.dataTransfer?.files?.[0]
    if (f) void handleFile(f)
}

function telechargerModele() {
    const link = document.createElement('a')
    link.href = '/templates/modele_import_activites_v5.xlsx'
    link.download = 'modele_import_activites_v5.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
}

/* ---------- Services ---------- */
async function loadServices() {
    loadingServices.value = true
    try {
        const res = await listerServices({ inclureInactifs: false })
        if (res.success) services.value = res.data
    } finally {
        loadingServices.value = false
    }
}

const serviceLabel = computed(() => {
    if (config.value.service === null) return '—'
    const s = services.value.find(x => x.id === config.value.service)
    return s?.nom ?? `#${config.value.service}`
})

/* ---------- Computed sur les lots ---------- */
const activeLots = computed(() =>
    [ptaLot.value, nonPtaLot.value].filter(l => l.tree !== null),
)

const hasMixed = computed(() => summary.value.pta > 0 && summary.value.nonPta > 0)

/* Alias conservé pour compatibilité avec le template existant */
const isMixed = computed(() => hasMixed.value)

/* Type unique détecté (utile pour l'affichage « lot homogène ») */
const detectedTypeImport = computed<'PTA' | 'NON_PTA' | 'MIXTE' | '—'>(() => {
    const { pta, nonPta } = summary.value
    if (pta > 0 && nonPta > 0) return 'MIXTE'
    if (pta > 0) return 'PTA'
    if (nonPta > 0) return 'NON_PTA'
    return '—'
})

const allLotsValid = computed(() => {
    const lots = activeLots.value
    return lots.length > 0 && lots.every(l => l.status === 'valid')
})

const allLotsImported = computed(() => {
    const lots = activeLots.value
    return lots.length > 0 && lots.every(l => l.status === 'imported')
})

const canImport = computed(() =>
    activeLots.value.length > 0
    && activeLots.value.every(l => l.status === 'valid')
    && !importing.value
    && !allLotsImported.value,
)

const totalImported = computed(() => {
    const r = { activites: 0, sa: 0, livrables: 0 }
    for (const lot of activeLots.value) {
        if (lot.result) {
            r.activites += lot.result.nombreActivitesImportees ?? 0
            r.sa += lot.result.nombreSousActivitesImportees ?? 0
            r.livrables += lot.result.nombreLivrablesImportes ?? 0
        }
    }
    return r
})

/* ---------- Rapport de validation agrégé (par lot) ---------- */
const validationReport = computed<ImportValidationReport | null>(() => {
    const invalidLots = activeLots.value.filter(l => l.report && !l.report.valide)
    if (invalidLots.length === 0) return null

    const erreurs: Array<{ activite?: string; champ?: string; message: string }> = []
    for (const lot of invalidLots) {
        const list = lot.report?.erreurs ?? []
        for (const e of list) {
            const prefix = `[${LOT_LABEL[lot.type]}]`
            erreurs.push({
                activite: e.activite ? `${prefix} ${e.activite}` : prefix,
                champ: e.champ || undefined,
                message: e.message,
            })
        }
    }

    const message = invalidLots
        .map(l => `[${LOT_LABEL[l.type]}] ${l.report?.message ?? 'invalide'}`)
        .join(' · ')

    return {
        valide: false,
        message,
        nombreErreurs: erreurs.length,
        erreurs,
    } as ImportValidationReport
})

/* ---------- Résultat d'import agrégé (par lot) ---------- */
const importResult = computed<ImportResult | null>(() => {
    if (!allLotsImported.value) return null
    return {
        message: 'Import terminé',
        nombreActivitesImportees: totalImported.value.activites,
        nombreSousActivitesImportees: totalImported.value.sa,
        nombreLivrablesImportes: totalImported.value.livrables,
    } as ImportResult
})

/* ---------- JSON final par lot ---------- */
function buildFinalJSON(tree: ImportTreeResult, type: LotType): any {
    return {
        version: config.value.version.trim(),
        service: config.value.service!,
        procedure: config.value.procedure!,
        annee: config.value.annee,
        typeImport: type,
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
}

/* ---------- Validation ---------- */
async function valider() {
    const errs: string[] = []
    if (!config.value.version?.trim()) errs.push('La version est obligatoire.')
    if (config.value.service === null) errs.push('Le service est obligatoire.')
    if (config.value.procedure === null) errs.push('La procédure est obligatoire.')
    if (!config.value.annee || config.value.annee < 1900 || config.value.annee > 2100) {
        errs.push("L'année doit être comprise entre 1900 et 2100.")
    }
    errors.value = errs
    if (errs.length) return

    if (activeLots.value.length === 0) {
        errors.value = ['Aucun lot à importer.']
        return
    }

    validatingImport.value = true
    try {
        for (const lot of activeLots.value) {
            lot.status = 'validating'
            lot.error = null
            lot.report = null
            try {
                const json = buildFinalJSON(lot.tree!, lot.type)
                const res = await validerImport(json, 'ACTIVITES')
                if (!res.success) {
                    lot.status = 'error'
                    lot.error = res.error
                } else {
                    lot.report = res.data
                    lot.status = res.data.valide ? 'valid' : 'invalid'
                }
            } catch (e: any) {
                lot.status = 'error'
                lot.error = e instanceof Error ? e.message : 'Erreur inconnue.'
            }
        }

        if (allLotsValid.value) showModal.value = true
    } finally {
        validatingImport.value = false
    }
}

/* ---------- Import ---------- */
async function confirmer() {
    if (!allLotsValid.value) return
    importing.value = true
    errors.value = []
    try {
        // Import séquentiel : PTA d'abord, NON-PTA ensuite
        for (const lot of activeLots.value) {
            lot.status = 'importing'
            lot.error = null
            try {
                const json = buildFinalJSON(lot.tree!, lot.type)
                const res = await confirmerImport(json, 'ACTIVITES')
                if (!res.success) {
                    lot.status = 'error'
                    lot.error = res.error
                    errors.value.push(`[${lot.type}] ${res.error}`)
                } else {
                    lot.result = res.data
                    lot.status = 'imported'
                }
            } catch (e: any) {
                lot.status = 'error'
                lot.error = e instanceof Error ? e.message : 'Erreur inconnue.'
                errors.value.push(`[${lot.type}] ${lot.error}`)
            }
        }
        showModal.value = false
    } finally {
        importing.value = false
    }
}

/* ---------- Reset ---------- */
function reset() {
    fileName.value = ''
    parsed.value = false
    parseErrors.value = []
    parseWarnings.value = []
    ptaLot.value = emptyLot('PTA')
    nonPtaLot.value = emptyLot('NON_PTA')
    errors.value = []
    summary.value = { os: 0, activites: 0, pta: 0, nonPta: 0, sa: 0, livrables: 0, resultats: 0, indicateurs: 0 }
}

/* ---------- Labels ---------- */
const LOT_LABEL: Record<LotType, string> = {
    PTA: 'PTA',
    NON_PTA: 'NON-PTA',
}

const LOT_STATUS_LABEL: Record<LotStatus, string> = {
    idle: '—',
    ready: 'Prêt',
    validating: 'Validation…',
    valid: 'Validé',
    invalid: 'Invalide',
    importing: 'Import…',
    imported: 'Importé',
    error: 'Erreur',
}

const LOT_STATUS_CLASS: Record<LotStatus, string> = {
    idle: 'muted',
    ready: 'info',
    validating: 'info',
    valid: 'success',
    invalid: 'danger',
    importing: 'info',
    imported: 'success',
    error: 'danger',
}

onMounted(() => { void loadServices() })
</script>

<template>
    <div class="import-page">
        <!-- Header -->
        <header class="welcome-row">
            <div class="welcome-row__left">
                <div class="eyebrow">
                    <span class="live-dot"></span>
                    Mode modèle Excel
                </div>
                <h1>Import depuis le modèle standard</h1>
                <span class="title-divider"></span>
                <p class="subheading">
                    Téléchargez le modèle, remplissez-le, puis importez-le. Les colonnes sont mappées automatiquement.
                </p>
            </div>
            <div class="welcome-row__right">
                <BaseButton variant="secondary" @click="router.push({ name: 'activites-importer' })">
                    <i class="bi bi-arrow-left"></i>
                    Import manuel
                </BaseButton>
                <BaseButton v-if="parsed || fileName" variant="secondary" @click="reset">
                    <i class="bi bi-arrow-counterclockwise"></i>
                    Recommencer
                </BaseButton>
            </div>
        </header>

        <!-- Alertes -->
        <div v-if="parseErrors.length" class="alert alert--danger" role="alert">
            <i class="bi bi-exclamation-triangle-fill"></i>
            <div>
                <div v-for="(e, i) in parseErrors" :key="i">{{ e }}</div>
            </div>
        </div>
        <div v-if="parseWarnings.length" class="alert alert--warning" role="status">
            <i class="bi bi-info-circle-fill"></i>
            <div>
                <div v-for="(w, i) in parseWarnings" :key="i">{{ w }}</div>
            </div>
        </div>
        <div v-if="errors.length" class="alert alert--danger">
            <i class="bi bi-exclamation-triangle-fill"></i>
            <div>
                <div v-for="(e, i) in errors" :key="i">{{ e }}</div>
            </div>
        </div>

        <!-- Étape 1 — Télécharger + Uploader -->
        <section v-if="!parsed" class="card">
            <header class="card__head">
                <span class="card__icon"><i class="bi bi-file-earmark-excel"></i></span>
                <h2 class="card__title">
                    1. Téléchargez le modèle, remplissez-le, importez-le
                    <span class="card__sub">Les 7 feuilles doivent conserver leur structure</span>
                </h2>
            </header>
            <div class="card__body">
                <div class="tpl-download">
                    <div class="tpl-download__icon"><i class="bi bi-download"></i></div>
                    <div class="tpl-download__text">
                        <strong>Modèle officiel v5</strong>
                        <small>7 feuilles : Guide · Objectif Spécifique · Activité · Sous-Activité · Livrable · Résultat
                            · Indicateur</small>
                    </div>
                    <BaseButton variant="primary" @click="telechargerModele">
                        <i class="bi bi-download"></i>
                        Télécharger le modèle
                    </BaseButton>
                </div>

                <div class="dropzone" :class="{ 'dropzone--dragover': isDragover }"
                    @dragover.prevent="isDragover = true" @dragleave.prevent="isDragover = false"
                    @drop.prevent="onDrop">
                    <div class="dropzone__icon"><i class="bi bi-cloud-arrow-up"></i></div>
                    <p class="dropzone__title"><strong>Glissez-déposez</strong> le modèle rempli</p>
                    <p class="dropzone__hint">Formats : .xlsx, .xls · Le fichier reste dans votre navigateur.</p>
                    <label class="btn btn--primary">
                        <i class="bi bi-folder2-open"></i>
                        Choisir un fichier
                        <input type="file" accept=".xlsx,.xls" hidden @change="onFileChange" />
                    </label>
                    <p v-if="parsing" class="dropzone__hint">Analyse en cours…</p>
                </div>
            </div>
        </section>

        <!-- Étape 2 — Aperçu + Configuration -->
        <section v-else class="card">
            <header class="card__head">
                <span class="card__icon"><i class="bi bi-check2-square"></i></span>
                <h2 class="card__title">
                    2. Vérifiez et configurez
                    <span class="card__sub">{{ fileName }}</span>
                </h2>
            </header>
            <div class="card__body">
                <!-- Résumé -->
                <div class="tpl-summary">
                    <div class="tpl-summary__item">
                        <span class="tpl-summary__label">Objectifs</span>
                        <strong class="tpl-summary__value">{{ summary.os }}</strong>
                    </div>
                    <div class="tpl-summary__item">
                        <span class="tpl-summary__label">Activités</span>
                        <strong class="tpl-summary__value">{{ summary.activites }}</strong>
                    </div>
                    <div class="tpl-summary__item">
                        <span class="tpl-summary__label">Sous-activités</span>
                        <strong class="tpl-summary__value">{{ summary.sa }}</strong>
                    </div>
                    <div class="tpl-summary__item">
                        <span class="tpl-summary__label">Livrables</span>
                        <strong class="tpl-summary__value">{{ summary.livrables }}</strong>
                    </div>
                    <div class="tpl-summary__item">
                        <span class="tpl-summary__label">Résultats</span>
                        <strong class="tpl-summary__value">{{ summary.resultats }}</strong>
                    </div>
                    <div class="tpl-summary__item">
                        <span class="tpl-summary__label">Indicateurs</span>
                        <strong class="tpl-summary__value">{{ summary.indicateurs }}</strong>
                    </div>
                </div>

                <!-- Répartition PTA / NON-PTA -->
                <div class="pta-breakdown" :class="{ 'pta-breakdown--mixed': isMixed }">
                    <div class="pta-breakdown__head">
                        <i class="bi bi-diagram-3"></i>
                        <strong>Répartition PTA / NON-PTA détectée dans le fichier</strong>
                    </div>
                    <div class="pta-breakdown__bars">
                        <div class="pta-chip pta-chip--pta">
                            <span class="pta-chip__dot"></span>
                            <span class="pta-chip__label">PTA</span>
                            <strong class="pta-chip__value">{{ summary.pta }}</strong>
                            <span class="pta-chip__hint">avec objectif spécifique</span>
                        </div>
                        <div class="pta-chip pta-chip--nonpta">
                            <span class="pta-chip__dot"></span>
                            <span class="pta-chip__label">NON-PTA</span>
                            <strong class="pta-chip__value">{{ summary.nonPta }}</strong>
                            <span class="pta-chip__hint">sans objectif spécifique</span>
                        </div>
                    </div>
                    <p class="pta-breakdown__note">
                        <i class="bi bi-info-circle"></i>
                        Le type est porté par chaque activité dans le fichier (colonne « Est PTA ? »).
                        <template v-if="isMixed">
                            Le lot est <strong>mixte</strong> : les activités sans objectif spécifique seront traitées
                            comme NON-PTA par l'API.
                        </template>
                        <template v-else>
                            Le lot est homogène : <strong>{{ detectedTypeImport }}</strong>.
                        </template>
                    </p>
                </div>

                <!-- Statut des lots PTA / NON-PTA -->
                <div class="lots-panel" :class="{ 'lots-panel--mixed': hasMixed }">
                    <div class="lots-panel__head">
                        <i class="bi bi-diagram-3"></i>
                        <strong>Répartition et statut des lots</strong>
                        <span class="lots-panel__total">{{ summary.activites }} activité(s)</span>
                    </div>

                    <div class="lots-grid">
                        <!-- Lot PTA -->
                        <div class="lot-card lot-card--pta" :class="`lot-card--${LOT_STATUS_CLASS[ptaLot.status]}`">
                            <div class="lot-card__head">
                                <span class="lot-card__dot"></span>
                                <strong class="lot-card__title">PTA</strong>
                                <span class="lot-card__status"
                                    :class="`lot-card__status--${LOT_STATUS_CLASS[ptaLot.status]}`">
                                    <i class="bi" :class="{
                                        'bi-hourglass-split': ptaLot.status === 'validating' || ptaLot.status === 'importing',
                                        'bi-check-circle-fill': ptaLot.status === 'valid' || ptaLot.status === 'imported',
                                        'bi-x-circle-fill': ptaLot.status === 'invalid' || ptaLot.status === 'error',
                                        'bi-dot': ptaLot.status === 'idle',
                                    }"></i>
                                    {{ LOT_STATUS_LABEL[ptaLot.status] }}
                                </span>
                            </div>
                            <div v-if="ptaLot.tree" class="lot-card__stats">
                                <span><strong>{{ ptaLot.nbActivites }}</strong> activité(s)</span>
                                <span><strong>{{ ptaLot.nbSa }}</strong> SA</span>
                                <span><strong>{{ ptaLot.nbLivrables }}</strong> livrable(s)</span>
                                <span><strong>{{ ptaLot.nbResultats }}</strong> résultat(s)</span>
                                <span><strong>{{ ptaLot.nbIndicateurs }}</strong> indicateur(s)</span>
                            </div>
                            <div v-else class="lot-card__empty">
                                Aucune activité PTA dans le fichier
                            </div>
                            <div v-if="ptaLot.error" class="lot-card__error">
                                <i class="bi bi-exclamation-triangle-fill"></i>
                                {{ ptaLot.error }}
                            </div>
                            <div v-if="ptaLot.report && !ptaLot.report.valide" class="lot-card__error">
                                <i class="bi bi-x-octagon-fill"></i>
                                {{ ptaLot.report.nombreErreurs }} erreur(s) de validation
                            </div>
                            <div v-if="ptaLot.status === 'imported' && ptaLot.result" class="lot-card__success">
                                <i class="bi bi-check-circle-fill"></i>
                                {{ ptaLot.result.message }}
                            </div>
                        </div>

                        <!-- Lot NON-PTA -->
                        <div class="lot-card lot-card--nonpta"
                            :class="`lot-card--${LOT_STATUS_CLASS[nonPtaLot.status]}`">
                            <div class="lot-card__head">
                                <span class="lot-card__dot"></span>
                                <strong class="lot-card__title">NON-PTA</strong>
                                <span class="lot-card__status"
                                    :class="`lot-card__status--${LOT_STATUS_CLASS[nonPtaLot.status]}`">
                                    <i class="bi" :class="{
                                        'bi-hourglass-split': nonPtaLot.status === 'validating' || nonPtaLot.status === 'importing',
                                        'bi-check-circle-fill': nonPtaLot.status === 'valid' || nonPtaLot.status === 'imported',
                                        'bi-x-circle-fill': nonPtaLot.status === 'invalid' || nonPtaLot.status === 'error',
                                        'bi-dot': nonPtaLot.status === 'idle',
                                    }"></i>
                                    {{ LOT_STATUS_LABEL[nonPtaLot.status] }}
                                </span>
                            </div>
                            <div v-if="nonPtaLot.tree" class="lot-card__stats">
                                <span><strong>{{ nonPtaLot.nbActivites }}</strong> activité(s)</span>
                                <span><strong>{{ nonPtaLot.nbSa }}</strong> SA</span>
                                <span><strong>{{ nonPtaLot.nbLivrables }}</strong> livrable(s)</span>
                                <span><strong>{{ nonPtaLot.nbResultats }}</strong> résultat(s)</span>
                                <span><strong>{{ nonPtaLot.nbIndicateurs }}</strong> indicateur(s)</span>
                            </div>
                            <div v-else class="lot-card__empty">
                                Aucune activité NON-PTA dans le fichier
                            </div>
                            <div v-if="nonPtaLot.error" class="lot-card__error">
                                <i class="bi bi-exclamation-triangle-fill"></i>
                                {{ nonPtaLot.error }}
                            </div>
                            <div v-if="nonPtaLot.report && !nonPtaLot.report.valide" class="lot-card__error">
                                <i class="bi bi-x-octagon-fill"></i>
                                {{ nonPtaLot.report.nombreErreurs }} erreur(s) de validation
                            </div>
                            <div v-if="nonPtaLot.status === 'imported' && nonPtaLot.result" class="lot-card__success">
                                <i class="bi bi-check-circle-fill"></i>
                                {{ nonPtaLot.result.message }}
                            </div>
                        </div>
                    </div>

                    <p v-if="hasMixed" class="lots-panel__note">
                        <i class="bi bi-info-circle"></i>
                        Le fichier contient les deux types : <strong>2 appels API distincts</strong> seront effectués
                        (PTA puis NON-PTA), chacun avec son propre <code>typeImport</code>.
                    </p>
                    <p v-else-if="summary.pta > 0" class="lots-panel__note">
                        <i class="bi bi-info-circle"></i>
                        Fichier 100% <strong>PTA</strong> — un seul appel API avec <code>typeImport: 'PTA'</code>.
                    </p>
                    <p v-else-if="summary.nonPta > 0" class="lots-panel__note">
                        <i class="bi bi-info-circle"></i>
                        Fichier 100% <strong>NON-PTA</strong> — un seul appel API avec
                        <code>typeImport: 'NON_PTA'</code>.
                    </p>
                </div>

                <!-- Config -->
                <div class="config-grid">
                    <div class="config-field">
                        <label class="config-label">Version <span class="config-required">*</span></label>
                        <input v-model="config.version" class="config-input" type="text" maxlength="20" />
                    </div>

                    <div class="config-field">
                        <label class="config-label">Service <span class="config-required">*</span></label>
                        <select v-model="config.service" class="config-input" :disabled="loadingServices">
                            <option :value="null">— Sélectionner —</option>
                            <option v-for="s in services" :key="s.id" :value="s.id">
                                {{ s.nom }}<template v-if="s.departement?.nom"> · {{ s.departement.nom }}</template>
                            </option>
                        </select>
                    </div>

                    <div class="config-field">
                        <label class="config-label">Procédure <span class="config-required">*</span></label>
                        <input v-model.number="config.procedure" class="config-input" type="number" min="1" />
                    </div>

                    <div class="config-field">
                        <label class="config-label">Année <span class="config-required">*</span></label>
                        <input v-model.number="config.annee" class="config-input" type="number" min="1900" max="2100" />
                    </div>

                    <div class="config-field config-field--full">
                        <label class="config-switch">
                            <input type="checkbox" v-model="config.activitesDejaValidees" />
                            <span class="config-switch__track"><span class="config-switch__thumb"></span></span>
                            <span class="config-switch__text">
                                <strong>Activités déjà validées</strong>
                                <small>Statut initial <code>NON_COMMENCE</code> au lieu de
                                    <code>BROUILLON</code>.</small>
                            </span>
                        </label>
                    </div>
                </div>

                <!-- Succès global -->
                <div v-if="allLotsImported" class="import-success" style="margin-top: 1rem;">
                    <i class="bi bi-check-circle-fill"></i>
                    <div>
                        <strong>Import terminé</strong>
                        <span v-if="totalImported.activites > 0">
                            {{ totalImported.activites }} activité(s),
                            {{ totalImported.sa }} SA,
                            {{ totalImported.livrables }} livrable(s) importés au total.
                        </span>
                    </div>
                </div>

                <!-- Actions -->
                <div class="form-actions-bar">
                    <BaseButton variant="secondary" @click="reset" :disabled="importing">
                        <i class="bi bi-arrow-counterclockwise"></i>
                        Changer de fichier
                    </BaseButton>
                    <span class="status-bar__spacer"></span>
                    <BaseButton variant="primary" :loading="validatingImport || importing"
                        :disabled="validatingImport || importing || allLotsImported" @click="valider">
                        <i class="bi bi-check2-circle"></i>
                        {{ allLotsValid ? 'Valider à nouveau' : 'Valider les lots' }}
                    </BaseButton>
                    <BaseButton v-if="allLotsValid" variant="primary" :loading="importing" :disabled="!canImport"
                        @click="confirmer">
                        <i class="bi bi-cloud-arrow-up"></i>
                        Importer {{ activeLots.length === 2 ? 'les 2 lots' : 'le lot' }}
                    </BaseButton>
                </div>

                <!-- Validation report -->
                <div v-if="validationReport && !validationReport.valide" class="alert alert--danger"
                    style="margin-top: 1rem;">
                    <i class="bi bi-x-octagon-fill"></i>
                    <div>
                        <strong>{{ validationReport.message }}</strong>
                        <div v-for="(issue, i) in validationReport.erreurs" :key="i">
                            <span v-if="issue.activite">{{ issue.activite }} · </span>
                            <span v-if="issue.champ">{{ issue.champ }} — </span>
                            {{ issue.message }}
                        </div>
                    </div>
                </div>

                <BaseModal v-model="showModal" title="Confirmer l'import — Modèle Excel" size="md">
                    <div class="import-modal">
                        <div class="import-modal__icon"><i class="bi bi-cloud-arrow-up"></i></div>
                        <h3 class="import-modal__title">Prêt à importer</h3>
                        <p class="import-modal__text">
                            Deux appels API seront effectués (PTA d'abord, NON-PTA ensuite) :
                        </p>
                        <div class="import-modal__lots">
                            <div v-for="lot in activeLots" :key="lot.type" class="import-modal__lot">
                                <strong>{{ LOT_LABEL[lot.type] }}</strong>
                                <span>{{ lot.nbActivites }} activité(s), {{ lot.nbSa }} SA, {{ lot.nbLivrables }}
                                    livrable(s)</span>
                            </div>
                        </div>
                        <p class="import-modal__text" style="margin-top: 0.75rem;">
                            Service : <strong>{{ serviceLabel }}</strong> · Année : <strong>{{ config.annee }}</strong>
                        </p>
                    </div>
                    <template #footer>
                        <BaseButton variant="secondary" :disabled="importing" @click="showModal = false">Annuler
                        </BaseButton>
                        <BaseButton variant="primary" :loading="importing" @click="confirmer">
                            <i class="bi bi-check-lg"></i>
                            Confirmer l'import
                        </BaseButton>
                    </template>
                </BaseModal>
            </div>
        </section>
    </div>
</template>

<style scoped>
.import-page {
    --navy: #1a2a6c;
    --blue: #4a6cf7;
    --blue-soft: #eaf0ff;
    --gold: #d4af37;
    --gold-soft: #fff6dd;
    --slate: #64748b;
    --bg: #f5f7fa;
    --line: #e6ebf3;
    --ink: #15233c;
    --muted: #6b7280;
    --red: #a61b16;
    --red-soft: #fdecea;
    --green: #1e7e34;
    --green-soft: #e6f4ea;
    --radius: 16px;
    --radius-sm: 12px;
    --shadow-card: 0 1px 2px rgba(26, 42, 108, .04), 0 8px 24px rgba(26, 42, 108, .06);

    min-height: calc(100vh - 68px);
    padding: 34px 38px 24px;
    display: flex;
    flex-direction: column;
    gap: 1.15rem;
    color: var(--ink);
    font-family: "DM Sans", "Segoe UI", sans-serif;
    background:
        radial-gradient(circle at 12% 8%, rgba(26, 42, 108, .055) 0, transparent 38%),
        radial-gradient(circle at 92% 94%, rgba(212, 175, 55, .065) 0, transparent 36%),
        var(--bg);
}

.welcome-row {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
}

.welcome-row__left {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
}

.welcome-row__right {
    display: flex;
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
    box-shadow: 0 0 0 3px rgba(30, 126, 52, .18);
}

.welcome-row h1 {
    margin: 0;
    font-family: Manrope, "DM Sans", sans-serif;
    font-size: 1.55rem;
    font-weight: 800;
    color: var(--navy);
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
}

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
}

.card__icon {
    display: grid;
    place-items: center;
    width: 32px;
    height: 32px;
    border-radius: 9px;
    background: var(--blue-soft);
    color: #3b63d9;
}

.card__title {
    margin: 0;
    font-size: 0.92rem;
    font-weight: 700;
    color: var(--navy);
}

.card__sub {
    display: block;
    margin-top: 2px;
    font-size: 0.72rem;
    font-weight: 400;
    color: var(--muted);
}

.card__body {
    padding: 1rem 1.25rem 1.15rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.alert {
    display: flex;
    gap: 0.65rem;
    padding: 0.75rem 1rem;
    border-radius: 10px;
    font-size: 0.82rem;
}

.alert--danger {
    background: var(--red-soft);
    color: var(--red);
    border: 1px solid #f5c6c2;
}

.alert--warning {
    background: var(--gold-soft);
    color: #92400e;
    border: 1px solid #fde68a;
}

.tpl-download {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 1rem 1.15rem;
    background: linear-gradient(135deg, #f0f7ff, #fff);
    border: 1px solid #cfe0ee;
    border-radius: var(--radius-sm);
}

.tpl-download__icon {
    display: grid;
    place-items: center;
    width: 44px;
    height: 44px;
    border-radius: 12px;
    background: var(--blue);
    color: #fff;
    font-size: 1.2rem;
    flex: none;
}

.tpl-download__text {
    flex: 1;
}

.tpl-download__text strong {
    display: block;
    color: var(--navy);
}

.tpl-download__text small {
    color: var(--muted);
    font-size: 0.75rem;
}

.dropzone {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    min-height: 200px;
    padding: 2rem 1.5rem;
    border: 2px dashed #b8c7dc;
    border-radius: var(--radius-sm);
    background: #f8fbff;
    text-align: center;
    transition: background .15s, border-color .15s;
}

.dropzone--dragover {
    border-color: var(--blue);
    background: var(--blue-soft);
}

.dropzone__icon {
    display: grid;
    place-items: center;
    width: 56px;
    height: 56px;
    border-radius: 16px;
    background: #fff;
    color: var(--blue);
    font-size: 1.6rem;
    box-shadow: 0 4px 12px rgba(26, 42, 108, .08);
}

.dropzone__title {
    margin: 0;
    font-size: 0.95rem;
    color: var(--navy);
}

.dropzone__hint {
    margin: 0;
    font-size: 0.78rem;
    color: var(--muted);
}

.btn {
    display: inline-flex;
    align-items: center;
    gap: 0.45rem;
    height: 38px;
    padding: 0 16px;
    border-radius: 10px;
    border: 1px solid transparent;
    font-size: 0.82rem;
    font-weight: 600;
    cursor: pointer;
}

.btn--primary {
    background: linear-gradient(135deg, var(--navy) 0%, var(--blue) 100%);
    color: #fff;
    box-shadow: 0 4px 12px rgba(26, 42, 108, .22);
}

.tpl-summary {
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 0.5rem;
    padding: 0.75rem;
    background: #f8fbff;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
}

.tpl-summary__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    padding: 0.5rem;
    background: #fff;
    border-radius: 8px;
}

.tpl-summary__label {
    font-size: 0.7rem;
    color: var(--muted);
    text-transform: uppercase;
    letter-spacing: 0.03em;
}

.tpl-summary__value {
    font-size: 1.1rem;
    color: var(--navy);
    font-weight: 800;
}

/* ---------- Panneau PTA / NON-PTA ---------- */
.pta-breakdown {
    padding: 0.85rem 1rem;
    background: #f8fbff;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    display: flex;
    flex-direction: column;
    gap: 0.65rem;
}

.pta-breakdown--mixed {
    background: linear-gradient(135deg, #fff8e6, #fff);
    border-color: #fde68a;
}

.pta-breakdown__head {
    display: flex;
    align-items: center;
    gap: 0.45rem;
    font-size: 0.82rem;
    color: var(--navy);
}

.pta-breakdown__head i {
    color: var(--blue);
}

.pta-breakdown--mixed .pta-breakdown__head i {
    color: #b45309;
}

.pta-breakdown__bars {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0.5rem;
}

.pta-chip {
    display: flex;
    align-items: center;
    gap: 0.55rem;
    padding: 0.55rem 0.85rem;
    background: #fff;
    border: 1px solid var(--line);
    border-radius: 10px;
    font-size: 0.8rem;
}

.pta-chip__dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    flex: none;
}

.pta-chip--pta .pta-chip__dot {
    background: var(--blue);
    box-shadow: 0 0 0 3px var(--blue-soft);
}

.pta-chip--nonpta .pta-chip__dot {
    background: var(--slate);
    box-shadow: 0 0 0 3px #eef1f5;
}

.pta-chip__label {
    font-weight: 700;
    color: var(--navy);
    letter-spacing: 0.02em;
}

.pta-chip__value {
    font-size: 1rem;
    color: var(--navy);
    font-weight: 800;
    margin-left: auto;
}

.pta-chip__hint {
    font-size: 0.7rem;
    color: var(--muted);
    display: none;
}

@media (min-width: 900px) {
    .pta-chip__hint {
        display: inline;
        margin-left: 0.4rem;
    }
}

.pta-breakdown__note {
    margin: 0;
    font-size: 0.74rem;
    color: var(--muted);
    display: flex;
    align-items: flex-start;
    gap: 0.4rem;
    line-height: 1.5;
}

.pta-breakdown__note i {
    color: var(--blue);
    margin-top: 2px;
    flex: none;
}

.pta-breakdown--mixed .pta-breakdown__note i {
    color: #b45309;
}

.config-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0.85rem;
}

.config-field--full {
    grid-column: 1 / -1;
}

.config-label {
    display: block;
    font-size: 0.78rem;
    font-weight: 600;
    color: var(--ink);
    margin-bottom: 0.35rem;
}

.config-required {
    color: var(--red);
}

.config-input {
    width: 100%;
    height: 38px;
    padding: 0 12px;
    border: 1px solid var(--line);
    border-radius: 9px;
    background: #fbfcfe;
    font-size: 0.85rem;
}

.config-input:focus {
    outline: none;
    border-color: var(--blue);
    box-shadow: 0 0 0 3px var(--blue-soft);
}

.config-switch {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    cursor: pointer;
}

.config-switch input {
    display: none;
}

.config-switch__track {
    position: relative;
    width: 40px;
    height: 22px;
    border-radius: 999px;
    background: #cbd5e1;
    transition: background .15s;
    flex: none;
}

.config-switch input:checked+.config-switch__track {
    background: var(--blue);
}

.config-switch__thumb {
    position: absolute;
    top: 2px;
    left: 2px;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: #fff;
    box-shadow: 0 1px 3px rgba(0, 0, 0, .15);
    transition: transform .15s;
}

.config-switch input:checked+.config-switch__track .config-switch__thumb {
    transform: translateX(18px);
}

.config-switch__text {
    display: flex;
    flex-direction: column;
    gap: 1px;
}

.config-switch__text strong {
    font-size: 0.85rem;
    color: var(--navy);
}

.config-switch__text small {
    font-size: 0.72rem;
    color: var(--muted);
}

.form-actions-bar {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding-top: 0.75rem;
    border-top: 1px solid var(--line);
}

.status-bar__spacer {
    flex: 1;
}

.import-success {
    display: flex;
    gap: 0.75rem;
    padding: 0.85rem 1rem;
    background: var(--green-soft);
    border: 1px solid #b7e0c0;
    border-radius: 10px;
    font-size: 0.82rem;
    color: var(--green);
}

.import-modal {
    text-align: center;
    padding: 0.5rem 0;
}

.import-modal__icon {
    display: inline-grid;
    place-items: center;
    width: 56px;
    height: 56px;
    border-radius: 16px;
    background: var(--blue-soft);
    color: var(--blue);
    font-size: 1.5rem;
    margin-bottom: 0.75rem;
}

.import-modal__title {
    margin: 0 0 0.4rem;
    font-size: 1rem;
    color: var(--navy);
}

.import-modal__text {
    margin: 0;
    font-size: 0.85rem;
    color: var(--muted);
    line-height: 1.5;
}

/* ---------- Panneau statut des lots ---------- */
.lots-panel {
    padding: 0.85rem 1rem;
    background: #f8fbff;
    border: 1px solid var(--line);
    border-radius: var(--radius-sm);
    display: flex;
    flex-direction: column;
    gap: 0.65rem;
}
.lots-panel--mixed { background: linear-gradient(135deg, #fff8e6, #fff); border-color: #fde68a; }
.lots-panel__head {
    display: flex;
    align-items: center;
    gap: 0.45rem;
    font-size: 0.82rem;
    color: var(--navy);
}
.lots-panel__head i { color: var(--blue); }
.lots-panel--mixed .lots-panel__head i { color: #b45309; }
.lots-panel__total {
    margin-left: auto;
    font-size: 0.72rem;
    color: var(--muted);
}

.lots-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.65rem; }

.lot-card {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    padding: 0.7rem 0.85rem;
    background: #fff;
    border: 1px solid var(--line);
    border-radius: 10px;
}
.lot-card--success { border-color: #b7e0c0; background: var(--green-soft); }
.lot-card--danger { border-color: #f5c6c2; background: var(--red-soft); }
.lot-card--info { border-color: #cfe0ee; }

.lot-card__head { display: flex; align-items: center; gap: 0.5rem; }
.lot-card__dot { width: 10px; height: 10px; border-radius: 50%; flex: none; }
.lot-card--pta .lot-card__dot { background: var(--blue); box-shadow: 0 0 0 3px var(--blue-soft); }
.lot-card--nonpta .lot-card__dot { background: var(--slate); box-shadow: 0 0 0 3px #eef1f5; }
.lot-card__title { font-size: 0.82rem; color: var(--navy); letter-spacing: 0.02em; }
.lot-card__status {
    margin-left: auto;
    display: inline-flex;
    align-items: center;
    gap: 0.25rem;
    font-size: 0.7rem;
    font-weight: 700;
    padding: 0.15rem 0.5rem;
    border-radius: 20px;
}
.lot-card__status--muted { background: #eef1f5; color: var(--muted); }
.lot-card__status--info { background: var(--blue-soft); color: #1d4ed8; }
.lot-card__status--success { background: var(--green-soft); color: var(--green); }
.lot-card__status--danger { background: var(--red-soft); color: var(--red); }

.lot-card__stats {
    display: flex;
    flex-wrap: wrap;
    gap: 0.35rem 0.75rem;
    font-size: 0.72rem;
    color: var(--muted);
}
.lot-card__stats strong { color: var(--navy); }

.lot-card__empty { font-size: 0.72rem; color: var(--muted); font-style: italic; }
.lot-card__error, .lot-card__success {
    display: flex;
    gap: 0.4rem;
    align-items: flex-start;
    font-size: 0.74rem;
    padding-top: 0.35rem;
    border-top: 1px dashed rgba(0,0,0,.08);
}
.lot-card__error { color: var(--red); }
.lot-card__success { color: var(--green); }

.lots-panel__note {
    margin: 0;
    font-size: 0.74rem;
    color: var(--muted);
    display: flex;
    align-items: flex-start;
    gap: 0.4rem;
    line-height: 1.5;
}
.lots-panel__note i { color: var(--blue); margin-top: 2px; flex: none; }
.lots-panel--mixed .lots-panel__note i { color: #b45309; }

.import-modal__lots {
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
    margin-top: 0.75rem;
    text-align: left;
}
.import-modal__lot {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: 0.6rem 0.85rem;
    background: #f8fbff;
    border: 1px solid var(--line);
    border-radius: 10px;
    font-size: 0.8rem;
}
.import-modal__lot strong { color: var(--navy); font-size: 0.82rem; }
.import-modal__lot span { color: var(--muted); font-size: 0.75rem; }
</style>
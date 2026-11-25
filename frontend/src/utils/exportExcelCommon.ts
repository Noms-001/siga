import { strFromU8, strToU8, unzipSync, zipSync } from 'fflate'
import * as XLSX from 'xlsx-js-style'
import { injecterGraphiques, type GraphiqueExcel } from './exportExcelCharts'

export type StyleCellule = Record<string, unknown>
export type ValeurCellule = string | number | Date | boolean | null | undefined

export const STYLE_TITRE: StyleCellule = {
    fill: { fgColor: { rgb: 'FF14213D' } },
    font: { color: { rgb: 'FFFFFFFF' }, bold: true, sz: 16 },
    alignment: { horizontal: 'left', vertical: 'center' },
}

export const STYLE_ENTETE: StyleCellule = {
    fill: { fgColor: { rgb: 'FF1A2A6C' } },
    font: { color: { rgb: 'FFFFFFFF' }, bold: true, sz: 10 },
    alignment: { horizontal: 'center', vertical: 'center', wrapText: true },
    border: { bottom: { style: 'medium', color: { rgb: 'FF0B1640' } } },
}

export const STYLE_SECTION: StyleCellule = {
    fill: { fgColor: { rgb: 'FFEFF4FF' } },
    font: { color: { rgb: 'FF1A2A6C' }, bold: true, sz: 11 },
    alignment: { horizontal: 'left', vertical: 'center' },
}

export const STYLE_LIGNE: StyleCellule = {
    fill: { fgColor: { rgb: 'FFFFFFFF' } },
    font: { color: { rgb: 'FF1F2937' } },
    alignment: { vertical: 'center', wrapText: true },
}

export const STYLE_LIGNE_ALTRE: StyleCellule = {
    fill: { fgColor: { rgb: 'FFF3F6FA' } },
    font: { color: { rgb: 'FF1F2937' } },
    alignment: { vertical: 'center', wrapText: true },
}

export const STYLE_NEUTRE: StyleCellule = {
    fill: { fgColor: { rgb: 'FFF2F4F8' } },
    font: { color: { rgb: 'FF475467' } },
    alignment: { horizontal: 'center', vertical: 'center' },
}

export const STYLE_ROUGE: StyleCellule = {
    fill: { fgColor: { rgb: 'FFFCE8E6' } },
    font: { color: { rgb: 'FFB42318' }, bold: true },
    alignment: { horizontal: 'center', vertical: 'center' },
}

export const STYLE_ORANGE: StyleCellule = {
    fill: { fgColor: { rgb: 'FFFFF4E5' } },
    font: { color: { rgb: 'FFB54708' }, bold: true },
    alignment: { horizontal: 'center', vertical: 'center' },
}

export const STYLE_VERT: StyleCellule = {
    fill: { fgColor: { rgb: 'FFE7F6EC' } },
    font: { color: { rgb: 'FF067647' }, bold: true },
    alignment: { horizontal: 'center', vertical: 'center' },
}

export const STYLE_BLEU: StyleCellule = {
    fill: { fgColor: { rgb: 'FFEFF4FF' } },
    font: { color: { rgb: 'FF175CD3' }, bold: true },
    alignment: { horizontal: 'center', vertical: 'center' },
}

const BORDURE_CARTE = {
    top: { style: 'thin', color: { rgb: 'FFD7DEEA' } },
    bottom: { style: 'thin', color: { rgb: 'FFD7DEEA' } },
    left: { style: 'thin', color: { rgb: 'FFD7DEEA' } },
    right: { style: 'thin', color: { rgb: 'FFD7DEEA' } },
}

export const STYLE_DASH_TITRE: StyleCellule = {
    fill: { fgColor: { rgb: 'FF14213D' } },
    font: { color: { rgb: 'FFFFFFFF' }, bold: true, sz: 18 },
    alignment: { horizontal: 'left', vertical: 'center', indent: 1 },
}

export const STYLE_DASH_SOUS_TITRE: StyleCellule = {
    fill: { fgColor: { rgb: 'FFEFF4FF' } },
    font: { color: { rgb: 'FF475467' }, sz: 10 },
    alignment: { horizontal: 'left', vertical: 'center', indent: 1 },
}

export const STYLE_CARTE_LABEL: StyleCellule = {
    fill: { fgColor: { rgb: 'FFFFFFFF' } },
    font: { color: { rgb: 'FF475467' }, bold: true, sz: 9 },
    alignment: { horizontal: 'center', vertical: 'center', wrapText: true },
    border: BORDURE_CARTE,
}

export const STYLE_CARTE_VALEUR: StyleCellule = {
    fill: { fgColor: { rgb: 'FFFFFFFF' } },
    font: { color: { rgb: 'FF1A2A6C' }, bold: true, sz: 18 },
    alignment: { horizontal: 'center', vertical: 'center' },
    border: BORDURE_CARTE,
}

export const STYLE_CARTE_TOTALE: StyleCellule = {
    fill: { fgColor: { rgb: 'FF1A2A6C' } },
    font: { color: { rgb: 'FFFFFFFF' }, bold: true, sz: 20 },
    alignment: { horizontal: 'center', vertical: 'center' },
    border: BORDURE_CARTE,
}

export const STYLE_SECTION_DASH: StyleCellule = {
    fill: { fgColor: { rgb: 'FFEFF4FF' } },
    font: { color: { rgb: 'FF1A2A6C' }, bold: true, sz: 11 },
    alignment: { horizontal: 'left', vertical: 'center', indent: 1 },
}

export const STYLE_PROGRESSION_REMPLI: StyleCellule = {
    fill: { fgColor: { rgb: 'FF1A2A6C' } },
    font: { color: { rgb: 'FFFFFFFF' }, sz: 8 },
    alignment: { horizontal: 'center', vertical: 'center' },
}

export const STYLE_PROGRESSION_VIDE: StyleCellule = {
    fill: { fgColor: { rgb: 'FFE6EAF2' } },
    font: { color: { rgb: 'FFE6EAF2' }, sz: 8 },
    alignment: { horizontal: 'center', vertical: 'center' },
}

export function ecrireCellule(
    feuille: XLSX.WorkSheet,
    ligne: number,
    colonne: number,
    valeur: string | number,
    style?: StyleCellule,
): void {
    const cellule: XLSX.CellObject = {
        t: typeof valeur === 'number' ? 'n' : 's',
        v: valeur,
    }
    if (style) cellule.s = style
    feuille[XLSX.utils.encode_cell({ r: ligne, c: colonne })] = cellule
}

export function fusionnerCellules(
    feuille: XLSX.WorkSheet,
    ligneDebut: number,
    colonneDebut: number,
    ligneFin: number,
    colonneFin: number,
): void {
    const fusions = (feuille['!merges'] ?? []) as XLSX.Range[]
    fusions.push({
        s: { r: ligneDebut, c: colonneDebut },
        e: { r: ligneFin, c: colonneFin },
    })
    feuille['!merges'] = fusions
}

export function appliquerStyleZone(
    feuille: XLSX.WorkSheet,
    ligneDebut: number,
    colonneDebut: number,
    ligneFin: number,
    colonneFin: number,
    style: StyleCellule,
): void {
    for (let ligne = ligneDebut; ligne <= ligneFin; ligne++) {
        for (let colonne = colonneDebut; colonne <= colonneFin; colonne++) {
            const adresse = XLSX.utils.encode_cell({ r: ligne, c: colonne })
            const cellule = (feuille[adresse] as XLSX.CellObject | undefined) ?? { t: 's', v: '' }
            cellule.s = style
            feuille[adresse] = cellule
        }
    }
}

export function definirLargeurs(feuille: XLSX.WorkSheet, largeurs: number[]): void {
    feuille['!cols'] = largeurs.map(largeur => ({ wch: largeur }))
}

export function definirHauteurs(feuille: XLSX.WorkSheet, hauteurs: number[]): void {
    feuille['!rows'] = hauteurs.map(hpt => ({ hpt }))
}

export function appliquerStyleCellule(
    feuille: XLSX.WorkSheet,
    ligne: number,
    colonne: number,
    style: StyleCellule,
): void {
    const cellule = feuille[XLSX.utils.encode_cell({ r: ligne, c: colonne })] as
        | XLSX.CellObject
        | undefined
    if (cellule) cellule.s = style
}

export function appliquerStyleLigne(
    feuille: XLSX.WorkSheet,
    ligne: number,
    nombreColonnes: number,
    style: StyleCellule,
): void {
    for (let colonne = 0; colonne < nombreColonnes; colonne++) {
        appliquerStyleCellule(feuille, ligne, colonne, style)
    }
}

export function convertirDate(valeur: string | null | undefined): Date | string | null | undefined {
    if (!valeur) return valeur
    const correspondance = /^(\d{4})-(\d{2})-(\d{2})/.exec(valeur)
    if (!correspondance) return valeur
    const date = new Date(
        Number(correspondance[1]),
        Number(correspondance[2]) - 1,
        Number(correspondance[3]),
    )
    return Number.isNaN(date.getTime()) ? valeur : date
}

function definirFormat(feuille: XLSX.WorkSheet, ligne: number, colonne: number, format: string): void {
    const cellule = feuille[XLSX.utils.encode_cell({ r: ligne, c: colonne })] as
        | XLSX.CellObject
        | undefined
    if (cellule && cellule.t !== 's') cellule.z = format
}

export interface VueFeuille {
    fichier: string
    ligneEntete: number
    autofiltre: boolean
    derniereCellule: string
}

export interface OptionsFeuille {
    titre?: string
    autofiltre?: boolean
    styleCellule?: (ligne: number, colonne: number, valeur: ValeurCellule) => StyleCellule | undefined
    colonnesDate?: number[]
    colonnesPourcentage?: number[]
}

function ajusterColonnes(feuille: XLSX.WorkSheet, lignes: ValeurCellule[][]): void {
    const nombreColonnes = lignes.reduce((maximum, ligne) => Math.max(maximum, ligne.length), 0)
    feuille['!cols'] = Array.from({ length: nombreColonnes }, (_, colonne) => {
        let largeur = 10
        for (const ligne of lignes) {
            const valeur = ligne[colonne]
            if (valeur == null) continue
            const texte = String(valeur)
            for (const morceau of texte.split('\n')) {
                largeur = Math.max(largeur, morceau.length + 2)
            }
        }
        return { wch: Math.min(largeur, 52) }
    })
}

export function creerFeuilleTableau(
    classeur: XLSX.WorkBook,
    nom: string,
    enTete: string[],
    lignes: ValeurCellule[][],
    options: OptionsFeuille = {},
): Omit<VueFeuille, 'fichier'> {
    const avecTitre = Boolean(options.titre)
    const ligneEntete = avecTitre ? 1 : 0
    const lignesPourLargeur = avecTitre ? lignes : lignes
    const lignesExcel: ValeurCellule[][] = []

    if (options.titre) lignesExcel.push([options.titre])
    lignesExcel.push(enTete)
    for (const ligne of lignes) lignesExcel.push(ligne)

    const feuille = XLSX.utils.aoa_to_sheet(lignesExcel as unknown[][]) as XLSX.WorkSheet

    if (options.titre) {
        appliquerStyleCellule(feuille, 0, 0, STYLE_TITRE)
        feuille['!merges'] = [{
            s: { r: 0, c: 0 },
            e: { r: 0, c: Math.max(0, enTete.length - 1) },
        }] as XLSX.Range[]
    }

    appliquerStyleLigne(feuille, ligneEntete, enTete.length, STYLE_ENTETE)

    lignes.forEach((ligne, indexLigne) => {
        const ligneExcel = ligneEntete + 1 + indexLigne
        const styleLigne = indexLigne % 2 === 0 ? STYLE_LIGNE : STYLE_LIGNE_ALTRE
        appliquerStyleLigne(feuille, ligneExcel, enTete.length, styleLigne)

        if (!options.styleCellule) return
        ligne.forEach((valeur, colonne) => {
            const style = options.styleCellule?.(indexLigne, colonne, valeur)
            if (style) appliquerStyleCellule(feuille, ligneExcel, colonne, style)
        })
    })

    for (const colonne of options.colonnesDate ?? []) {
        lignes.forEach((_, indexLigne) => {
            definirFormat(feuille, ligneEntete + 1 + indexLigne, colonne, 'dd/mm/yyyy')
        })
    }

    for (const colonne of options.colonnesPourcentage ?? []) {
        lignes.forEach((_, indexLigne) => {
            definirFormat(feuille, ligneEntete + 1 + indexLigne, colonne, '0" %"')
        })
    }

    ajusterColonnes(feuille, avecTitre ? [enTete, ...lignesPourLargeur] : [enTete, ...lignes])
    feuille['!rows'] = lignesExcel.map((_, index) => ({
        hpt: index === 0 && avecTitre ? 34 : index === ligneEntete ? 30 : 22,
    }))

    XLSX.utils.book_append_sheet(classeur, feuille, nom)

    const derniereLigne = ligneEntete + 1 + lignes.length
    return {
        ligneEntete,
        autofiltre: options.autofiltre !== false,
        derniereCellule: `${XLSX.utils.encode_col(Math.max(0, enTete.length - 1))}${derniereLigne}`,
    }
}

function injecterVueFeuille(xml: string, vue: Omit<VueFeuille, 'fichier'>): string {
    const lignesFigees = vue.ligneEntete + 1
    const premiereCellule = `A${lignesFigees + 1}`
    const sheetView = '<sheetView showGridLines="0" workbookViewId="0">'
        + `<pane ySplit="${lignesFigees}" topLeftCell="${premiereCellule}" activePane="bottomLeft" state="frozen"/>`
        + '<selection pane="bottomLeft" activeCell="' + premiereCellule + '" sqref="' + premiereCellule + '"/>'
        + '</sheetView>'

    let contenu = xml.replace(/<sheetViews>[\s\S]*?<\/sheetViews>/, `<sheetViews>${sheetView}</sheetViews>`)

    if (vue.autofiltre && !contenu.includes('<autoFilter')) {
        contenu = contenu.replace(
            '</worksheet>',
            `<autoFilter ref="A${vue.ligneEntete + 1}:${vue.derniereCellule}"/></worksheet>`,
        )
    }

    return contenu
}

export function telechargerClasseur(
    classeur: XLSX.WorkBook,
    nomFichier: string,
    vues: Omit<VueFeuille, 'fichier'>[],
    graphiques: GraphiqueExcel[] = [],
): void {
    const tampon = XLSX.write(classeur, { bookType: 'xlsx', type: 'array' }) as ArrayBuffer
    const fichiers = unzipSync(new Uint8Array(tampon))

    vues.forEach((vue, index) => {
        const chemin = `xl/worksheets/sheet${index + 1}.xml`
        const contenu = fichiers[chemin]
        if (!contenu) return
        fichiers[chemin] = strToU8(injecterVueFeuille(strFromU8(contenu), vue))
    })

    injecterGraphiques(fichiers, graphiques)

    const archive = zipSync(fichiers, { level: 6 })
    const blob = new Blob([archive as BlobPart], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    const url = URL.createObjectURL(blob)
    const lien = document.createElement('a')
    lien.href = url
    lien.download = nomFichier
    document.body.appendChild(lien)
    lien.click()
    lien.remove()
    URL.revokeObjectURL(url)
}

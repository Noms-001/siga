import { strFromU8, strToU8, unzipSync, zipSync } from 'fflate'
import { jsPDF } from 'jspdf'
import autoTable from 'jspdf-autotable'
import * as XLSX from 'xlsx-js-style'

import type {
    ActiviteExport,
    ActiviteExportResponse,
    ServiceExport,
} from '@/types/exportActivite'

function formaterDateCourte(valeur: string | null | undefined): string {
    if (!valeur) return '—'

    const jourSeul = /^(\d{4})-(\d{2})-(\d{2})$/.exec(valeur)
    if (jourSeul) {
        return `${jourSeul[3]}/${jourSeul[2]}/${jourSeul[1]}`
    }

    const date = new Date(valeur)
    if (Number.isNaN(date.getTime())) return '—'

    return new Intl.DateTimeFormat('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
    }).format(date)
}

function formaterDateHeure(valeur: string | null | undefined): string {
    if (!valeur) return '—'

    const date = new Date(valeur)
    if (Number.isNaN(date.getTime())) return '—'

    return new Intl.DateTimeFormat('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    }).format(date)
}

function texteOuTiret(valeur: string | null | undefined): string {
    return valeur?.trim() ? valeur : '—'
}

function responsablesTexte(responsables: string[] | null | undefined): string {
    return responsables?.length ? responsables.join(', ') : '—'
}

function libelleObjectif(activite: ActiviteExport): string {
    if (!activite.objectifSpecifiqueCode && !activite.objectifSpecifiqueDesignation) {
        return '—'
    }

    if (activite.objectifSpecifiqueCode && activite.objectifSpecifiqueDesignation) {
        return `${activite.objectifSpecifiqueCode} - ${activite.objectifSpecifiqueDesignation}`
    }

    return activite.objectifSpecifiqueCode ?? activite.objectifSpecifiqueDesignation ?? '—'
}

function assainirNom(valeur: string): string {
    return valeur
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[^a-zA-Z0-9]+/g, '_')
        .replace(/^_+|_+$/g, '')
        .slice(0, 80)
}

type StyleCellule = Record<string, unknown>

const STYLE_ENTETE: StyleCellule = {
    fill: { fgColor: { rgb: 'FF1A2A6C' } },
    font: { color: { rgb: 'FFFFFFFF' }, bold: true, sz: 10 },
    alignment: { horizontal: 'center', vertical: 'center', wrapText: true },
    border: { bottom: { style: 'medium', color: { rgb: 'FF0B1640' } } },
}

const STYLE_LIGNE: StyleCellule = {
    fill: { fgColor: { rgb: 'FFFFFFFF' } },
    font: { color: { rgb: 'FF1F2937' } },
    alignment: { vertical: 'center', wrapText: true },
}

const STYLE_LIGNE_ALTRE: StyleCellule = {
    fill: { fgColor: { rgb: 'FFF3F6FA' } },
    font: { color: { rgb: 'FF1F2937' } },
    alignment: { vertical: 'center', wrapText: true },
}

const STYLE_NEUTRE: StyleCellule = {
    fill: { fgColor: { rgb: 'FFF2F4F8' } },
    font: { color: { rgb: 'FF475467' } },
    alignment: { horizontal: 'center', vertical: 'center' },
}

const STYLE_ROUGE: StyleCellule = {
    fill: { fgColor: { rgb: 'FFFCE8E6' } },
    font: { color: { rgb: 'FFB42318' }, bold: true },
    alignment: { horizontal: 'center', vertical: 'center' },
}

const STYLE_ORANGE: StyleCellule = {
    fill: { fgColor: { rgb: 'FFFFF4E5' } },
    font: { color: { rgb: 'FFB54708' }, bold: true },
    alignment: { horizontal: 'center', vertical: 'center' },
}

const STYLE_VERT: StyleCellule = {
    fill: { fgColor: { rgb: 'FFE7F6EC' } },
    font: { color: { rgb: 'FF067647' }, bold: true },
    alignment: { horizontal: 'center', vertical: 'center' },
}

const STYLE_BLEU: StyleCellule = {
    fill: { fgColor: { rgb: 'FFEFF4FF' } },
    font: { color: { rgb: 'FF175CD3' }, bold: true },
    alignment: { horizontal: 'center', vertical: 'center' },
}

function appliquerStyleEntete(feuille: XLSX.WorkSheet, nombreColonnes: number): void {
    for (let colonne = 0; colonne < nombreColonnes; colonne++) {
        const cellule = feuille[XLSX.utils.encode_cell({ r: 0, c: colonne })] as
            | XLSX.CellObject
            | undefined
        if (cellule) cellule.s = STYLE_ENTETE
    }
    feuille['!rows'] = [{ hpt: 30 }]
}

function appliquerStyleCellule(
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

function appliquerStyleLigne(
    feuille: XLSX.WorkSheet,
    ligne: number,
    nombreColonnes: number,
    style: StyleCellule,
): void {
    for (let colonne = 0; colonne < nombreColonnes; colonne++) {
        appliquerStyleCellule(feuille, ligne, colonne, style)
    }
}

function appliquerHauteursLignes(
    feuille: XLSX.WorkSheet,
    nombreLignes: number,
    hauteur: number,
): void {
    feuille['!rows'] = Array.from({ length: nombreLignes }, (_, index) =>
        index === 0 ? { hpt: 30 } : { hpt: hauteur },
    )
}

function stylePriorite(valeur: string | null | undefined): StyleCellule {
    const texte = (valeur ?? '').toLowerCase()

    if (texte.includes('critique') || texte.includes('haute')) return STYLE_ROUGE
    if (texte.includes('normale') || texte.includes('moyenne')) return STYLE_ORANGE
    if (texte.includes('faible') || texte.includes('basse')) return STYLE_VERT

    return STYLE_NEUTRE
}

function styleStatut(valeur: string | null | undefined): StyleCellule {
    const texte = (valeur ?? '').toLowerCase()

    if (texte.includes('retard') || texte.includes('rejet') || texte.includes('annul')) {
        return STYLE_ROUGE
    }
    if (texte.includes('attente') || texte.includes('report') || texte.includes('suspend')) {
        return STYLE_ORANGE
    }
    if (texte.includes('termin') || texte.includes('valid')) return STYLE_VERT
    if (texte.includes('cours')) return STYLE_BLEU

    return STYLE_NEUTRE
}

function styleAvancement(valeur: number): StyleCellule {
    if (valeur >= 100) return STYLE_VERT
    if (valeur >= 50) return STYLE_ORANGE
    return STYLE_ROUGE
}

interface VueFeuille {
    fichier: string
    colonnesFigees: number
    derniereCellule: string
}

function injecterVueFeuille(
    xml: string,
    colonnesFigees: number,
    derniereCellule: string,
): string {
    const topLeftCell = `${XLSX.utils.encode_col(colonnesFigees)}2`
    const sheetView = '<sheetView showGridLines="0" workbookViewId="0">'
        + `<pane xSplit="${colonnesFigees}" ySplit="1" topLeftCell="${topLeftCell}" activePane="bottomRight" state="frozen"/>`
        + '<selection pane="topRight"/><selection pane="bottomLeft"/>'
        + '<selection pane="bottomRight" activeCell="A1" sqref="A1"/>'
        + '</sheetView>'

    let contenu = xml.replace(/<sheetViews>[\s\S]*?<\/sheetViews>/, `<sheetViews>${sheetView}</sheetViews>`)

    if (!contenu.includes('<autoFilter')) {
        contenu = contenu.replace(
            '</worksheet>',
            `<autoFilter ref="A1:${derniereCellule}"/></worksheet>`,
        )
    }

    return contenu
}

function telechargerClasseur(
    classeur: XLSX.WorkBook,
    nomFichier: string,
    vues: VueFeuille[],
): void {
    const tampon = XLSX.write(classeur, { bookType: 'xlsx', type: 'array' }) as ArrayBuffer
    const fichiers = unzipSync(new Uint8Array(tampon))

    for (const vue of vues) {
        const chemin = `xl/worksheets/${vue.fichier}`
        const contenu = fichiers[chemin]
        if (!contenu) continue

        fichiers[chemin] = strToU8(
            injecterVueFeuille(strFromU8(contenu), vue.colonnesFigees, vue.derniereCellule),
        )
    }

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

function nomFichierExcel(data: ActiviteExportResponse): string {
    const service = data.service ?? 'Tous_Services'
    return `Export_Activites_${assainirNom(service)}_${data.annee}.xlsx`
}

function nomFichierPdf(service: ServiceExport, annee: number): string {
    return `Rapport_Activites_${assainirNom(service.nom)}_${annee}.pdf`
}

export function exporterExcel(data: ActiviteExportResponse): void {
    const inclureService = data.serviceId === null

    const entetesActivites = [
        'Code',
        'Désignation',
        'Type',
        'Priorité',
        'Objectif spécifique',
        'Procédure',
        'Date début',
        'Date fin',
        'Statut',
        'Responsable(s)',
    ]

    if (inclureService) {
        entetesActivites.push('Service')
    }

    const lignesActivites: Array<Array<string | number>> = [entetesActivites]

    for (const activite of data.activites) {
        const ligne: Array<string | number> = [
            activite.code,
            activite.designation,
            activite.pta ? 'PTA' : 'NON_PTA',
            texteOuTiret(activite.priorite),
            libelleObjectif(activite),
            texteOuTiret(activite.procedure),
            formaterDateCourte(activite.dateDebutPrevue),
            formaterDateCourte(activite.dateFinPrevue),
            texteOuTiret(activite.statutLibelle),
            responsablesTexte(activite.responsables),
        ]

        if (inclureService) {
            ligne.push(texteOuTiret(activite.serviceNom))
        }

        lignesActivites.push(ligne)
    }

    const lignesSousActivites: Array<Array<string | number>> = [[
        'Code',
        'Code activité',
        'Désignation',
        'Date début',
        'Date fin',
        'Statut',
        'Responsable(s)',
        "Taux d'avancement (%)",
    ]]

    const datesSuivi = new Map<string, string>()
    const suivisParSousActivite = new Map<
        string,
        {
            codeActivite: string
            codeSousActivite: string
            designation: string
            avancements: Map<string, number>
        }
    >()

    for (const activite of data.activites) {
        for (const sousActivite of activite.sousActivites) {
            lignesSousActivites.push([
                sousActivite.code,
                activite.code,
                sousActivite.designation,
                formaterDateCourte(sousActivite.dateDebutPrevue),
                formaterDateCourte(sousActivite.dateFinPrevue),
                texteOuTiret(sousActivite.statutLibelle),
                responsablesTexte(sousActivite.responsables),
                Math.round(sousActivite.avancement ?? 0),
            ])

            const cle = `${activite.code}|${sousActivite.code}`
            const ligneSuivi = suivisParSousActivite.get(cle) ?? {
                codeActivite: activite.code,
                codeSousActivite: sousActivite.code,
                designation: sousActivite.designation,
                avancements: new Map<string, number>(),
            }
            suivisParSousActivite.set(cle, ligneSuivi)

            for (const suivi of sousActivite.suivis) {
                if (!suivi.dateChangement) continue

                const jour = suivi.dateChangement.slice(0, 10)
                if (!datesSuivi.has(jour)) {
                    datesSuivi.set(jour, formaterDateCourte(suivi.dateChangement))
                }
                if (!ligneSuivi.avancements.has(jour)) {
                    ligneSuivi.avancements.set(
                        jour,
                        Math.round(suivi.valeurPourcentage ?? 0),
                    )
                }
            }
        }
    }

    const joursSuivi = [...datesSuivi.keys()].sort()

    const lignesSuivis: Array<Array<string | number>> = [[
        'Code activité',
        'Code sous-activité',
        'Désignation sous-activité',
        ...joursSuivi.map(jour => datesSuivi.get(jour) ?? jour),
    ]]

    for (const ligne of suivisParSousActivite.values()) {
        lignesSuivis.push([
            ligne.codeActivite,
            ligne.codeSousActivite,
            ligne.designation,
            ...joursSuivi.map(jour => ligne.avancements.get(jour) ?? ''),
        ])
    }

    const classeur = XLSX.utils.book_new()

    const feuilleActivites = XLSX.utils.aoa_to_sheet(lignesActivites)
    feuilleActivites['!cols'] = [
        { wch: 16 },
        { wch: 42 },
        { wch: 12 },
        { wch: 14 },
        { wch: 38 },
        { wch: 28 },
        { wch: 13 },
        { wch: 13 },
        { wch: 16 },
        { wch: 32 },
        ...(inclureService ? [{ wch: 28 }] : []),
    ]

    const feuilleSousActivites = XLSX.utils.aoa_to_sheet(lignesSousActivites)
    feuilleSousActivites['!cols'] = [
        { wch: 16 },
        { wch: 16 },
        { wch: 42 },
        { wch: 13 },
        { wch: 13 },
        { wch: 16 },
        { wch: 32 },
        { wch: 22 },
    ]

    const feuilleSuivis = XLSX.utils.aoa_to_sheet(lignesSuivis)
    feuilleSuivis['!cols'] = [
        { wch: 18 },
        { wch: 20 },
        { wch: 42 },
        ...joursSuivi.map(() => ({ wch: 15 })),
    ]

    appliquerStyleEntete(feuilleActivites, entetesActivites.length)
    appliquerStyleEntete(feuilleSousActivites, 8)
    appliquerStyleEntete(feuilleSuivis, 3 + joursSuivi.length)

    data.activites.forEach((activite, index) => {
        appliquerStyleLigne(
            feuilleActivites,
            index + 1,
            entetesActivites.length,
            index % 2 === 0 ? STYLE_LIGNE : STYLE_LIGNE_ALTRE,
        )
        appliquerStyleCellule(feuilleActivites, index + 1, 3, stylePriorite(activite.priorite))
        appliquerStyleCellule(feuilleActivites, index + 1, 8, styleStatut(activite.statutLibelle))
    })

    let indexSousActivite = 1
    for (const activite of data.activites) {
        for (const sousActivite of activite.sousActivites) {
            appliquerStyleLigne(
                feuilleSousActivites,
                indexSousActivite,
                8,
                (indexSousActivite - 1) % 2 === 0 ? STYLE_LIGNE : STYLE_LIGNE_ALTRE,
            )
            appliquerStyleCellule(
                feuilleSousActivites,
                indexSousActivite,
                5,
                styleStatut(sousActivite.statutLibelle),
            )
            appliquerStyleCellule(
                feuilleSousActivites,
                indexSousActivite,
                7,
                styleAvancement(Math.round(sousActivite.avancement ?? 0)),
            )
            indexSousActivite++
        }
    }

    Array.from(suivisParSousActivite.values()).forEach((ligne, index) => {
        appliquerStyleLigne(
            feuilleSuivis,
            index + 1,
            3 + joursSuivi.length,
            index % 2 === 0 ? STYLE_LIGNE : STYLE_LIGNE_ALTRE,
        )

        joursSuivi.forEach((jour, colonne) => {
            const valeur = ligne.avancements.get(jour)
            if (valeur !== undefined) {
                appliquerStyleCellule(
                    feuilleSuivis,
                    index + 1,
                    colonne + 3,
                    styleAvancement(valeur),
                )
            }
        })
    })

    appliquerHauteursLignes(feuilleActivites, lignesActivites.length, 44)
    appliquerHauteursLignes(feuilleSousActivites, lignesSousActivites.length, 32)
    appliquerHauteursLignes(feuilleSuivis, lignesSuivis.length, 24)

    XLSX.utils.book_append_sheet(classeur, feuilleActivites, 'Activités')
    XLSX.utils.book_append_sheet(classeur, feuilleSousActivites, 'Sous-activités')
    XLSX.utils.book_append_sheet(classeur, feuilleSuivis, 'Suivis')

    const derniereCelluleActivites =
        `${XLSX.utils.encode_col(entetesActivites.length - 1)}${lignesActivites.length}`
    const derniereCelluleSousActivites =
        `${XLSX.utils.encode_col(7)}${lignesSousActivites.length}`
    const derniereCelluleSuivis =
        `${XLSX.utils.encode_col(2 + joursSuivi.length)}${lignesSuivis.length}`

    telechargerClasseur(classeur, nomFichierExcel(data), [
        { fichier: 'sheet1.xml', colonnesFigees: 2, derniereCellule: derniereCelluleActivites },
        { fichier: 'sheet2.xml', colonnesFigees: 3, derniereCellule: derniereCelluleSousActivites },
        { fichier: 'sheet3.xml', colonnesFigees: 3, derniereCellule: derniereCelluleSuivis },
    ])
}

type DocPdf = jsPDF & { lastAutoTable?: { finalY: number } }

function finTableau(doc: DocPdf, defaut: number): number {
    return doc.lastAutoTable?.finalY ?? defaut
}

function garantirEspace(doc: DocPdf, y: number, besoin = 22): number {
    const hauteur = doc.internal.pageSize.getHeight()
    if (y + besoin > hauteur - 20) {
        doc.addPage()
        return 24
    }
    return y
}

type CouleurPdf = [number, number, number]

function couleur(r: number, g: number, b: number): CouleurPdf {
    return [r, g, b]
}

const PDF_COULEURS = {
    encre: couleur(21, 35, 60),
    navy: couleur(22, 50, 79),
    navyClair: couleur(34, 77, 123),
    bleu: couleur(43, 108, 176),
    bleuClair: couleur(234, 241, 250),
    gris: couleur(107, 114, 128),
    grisClair: couleur(227, 232, 239),
    ligne: couleur(220, 226, 238),
    blanc: couleur(255, 255, 255),
    rouge: couleur(179, 38, 30),
    rougeClair: couleur(253, 240, 238),
    orange: couleur(180, 83, 9),
    orangeClair: couleur(253, 227, 203),
    vert: couleur(23, 112, 58),
    vertClair: couleur(230, 245, 235),
}

function rectanglePdf(
    doc: jsPDF,
    x: number,
    y: number,
    largeur: number,
    hauteur: number,
    fond: CouleurPdf,
    contour?: CouleurPdf,
): void {
    doc.setFillColor(...fond)
    doc.rect(x, y, largeur, hauteur, 'F')
    if (contour) {
        doc.setDrawColor(...contour)
        doc.setLineWidth(0.2)
        doc.rect(x, y, largeur, hauteur, 'S')
    }
}

function lignePdf(
    doc: jsPDF,
    x1: number,
    y1: number,
    x2: number,
    y2: number,
    couleur: CouleurPdf,
    epaisseur = 0.2,
): void {
    doc.setDrawColor(...couleur)
    doc.setLineWidth(epaisseur)
    doc.line(x1, y1, x2, y2)
}

function couleurPrioritePdf(priorite: string): CouleurPdf {
    const valeur = priorite.toLowerCase()
    if (valeur.includes('critique')) return PDF_COULEURS.rouge
    if (valeur.includes('haute')) return PDF_COULEURS.orange
    if (valeur.includes('moyenne')) return PDF_COULEURS.bleu
    if (valeur.includes('faible')) return PDF_COULEURS.vert
    return PDF_COULEURS.encre
}

function couleurStatutPdf(statut: string): CouleurPdf {
    const valeur = statut.toLowerCase()
    if (valeur.includes('retard')) return PDF_COULEURS.rouge
    if (valeur.includes('non commenc')) return PDF_COULEURS.gris
    if (valeur.includes('termin') || valeur.includes('réalis')) return PDF_COULEURS.vert
    if (valeur.includes('en cours')) return PDF_COULEURS.bleu
    return PDF_COULEURS.encre
}

function ajouterEnTete(
    doc: DocPdf,
    data: ActiviteExportResponse,
    service: ServiceExport,
): void {
    if (doc.getCurrentPageInfo().pageNumber === 1) return

    const largeur = doc.internal.pageSize.getWidth()
    doc.setFont('helvetica', 'bold')
    doc.setFontSize(9)
    doc.setTextColor(...PDF_COULEURS.navy)
    doc.text('Rapport des activités', 14, 11)
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(8)
    doc.setTextColor(...PDF_COULEURS.gris)
    doc.text(`${service.nom}   Exercice ${data.annee}`, largeur - 14, 11, { align: 'right' })
    lignePdf(doc, 14, 14, largeur - 14, 14, PDF_COULEURS.ligne, 0.3)
    doc.setTextColor(...PDF_COULEURS.encre)
}

function ajouterPiedDePage(doc: DocPdf, data: ActiviteExportResponse): void {
    const total = doc.getNumberOfPages()
    const largeur = doc.internal.pageSize.getWidth()
    const hauteur = doc.internal.pageSize.getHeight()
    const pageCourante = doc.getCurrentPageInfo().pageNumber

    for (let page = 1; page <= total; page++) {
        doc.setPage(page)
        doc.setFont('helvetica', 'normal')
        doc.setFontSize(7.5)
        doc.setTextColor(...PDF_COULEURS.gris)
        doc.text(
            `${data.direction} - ${data.departement ?? 'Département'} - Généré le ${formaterDateHeure(data.dateGeneration)}`,
            14,
            hauteur - 7,
        )
        doc.text(`Page ${page} / ${total}`, largeur - 14, hauteur - 7, { align: 'right' })
    }

    doc.setPage(pageCourante)
    doc.setTextColor(...PDF_COULEURS.encre)
}

function dessinerCarteKpi(
    doc: jsPDF,
    x: number,
    y: number,
    largeur: number,
    hauteur: number,
    valeur: string,
    libelle: string,
    accent: CouleurPdf,
): void {
    rectanglePdf(doc, x, y, largeur, hauteur, PDF_COULEURS.bleuClair, PDF_COULEURS.ligne)
    rectanglePdf(doc, x, y, 1.4, hauteur, accent)
    doc.setFont('helvetica', 'bold')
    doc.setFontSize(17)
    doc.setTextColor(...PDF_COULEURS.navy)
    doc.text(valeur, x + 5, y + 11)
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(7.2)
    doc.setTextColor(...PDF_COULEURS.gris)
    const lignes = doc.splitTextToSize(libelle, largeur - 9).slice(0, 2)
    doc.text(lignes, x + 5, y + 18)
    doc.setTextColor(...PDF_COULEURS.encre)
}

interface EntreeGraphiquePdf {
    label: string
    valeur: number
    couleur: CouleurPdf
}

function dessinerMiniGraphique(
    doc: jsPDF,
    x: number,
    y: number,
    largeur: number,
    titre: string,
    entrees: EntreeGraphiquePdf[],
): void {
    const hauteur = 50
    rectanglePdf(doc, x, y, largeur, hauteur, PDF_COULEURS.blanc, PDF_COULEURS.ligne)
    doc.setFont('helvetica', 'bold')
    doc.setFontSize(8.5)
    doc.setTextColor(...PDF_COULEURS.navy)
    doc.text(titre.toUpperCase(), x + 3, y + 6)

    const total = entrees.reduce((somme, entree) => somme + entree.valeur, 0) || 1
    let position = y + 14

    for (const entree of entrees.slice(0, 3)) {
        doc.setFont('helvetica', 'normal')
        doc.setFontSize(7.2)
        doc.setTextColor(...PDF_COULEURS.gris)
        doc.text(entree.label, x + 3, position)
        doc.setFont('helvetica', 'bold')
        doc.setTextColor(...PDF_COULEURS.encre)
        doc.text(String(entree.valeur), x + largeur - 3, position, { align: 'right' })
        rectanglePdf(doc, x + 3, position + 1.5, largeur - 6, 2.2, PDF_COULEURS.grisClair)
        if (entree.valeur > 0) {
            rectanglePdf(
                doc,
                x + 3,
                position + 1.5,
                ((largeur - 6) * entree.valeur) / total,
                2.2,
                entree.couleur,
            )
        }
        position += 11
    }

    doc.setTextColor(...PDF_COULEURS.encre)
}

function dessinerBandeActivite(
    doc: DocPdf,
    activite: ActiviteExport,
    y: number,
): number {
    const largeur = doc.internal.pageSize.getWidth()
    const position = garantirEspace(doc, y, 20)
    rectanglePdf(doc, 14, position, largeur - 28, 12, PDF_COULEURS.bleuClair, PDF_COULEURS.ligne)
    rectanglePdf(doc, 14, position, 1.4, 12, PDF_COULEURS.bleu)

    doc.setFont('helvetica', 'bold')
    doc.setFontSize(8)
    doc.setTextColor(...PDF_COULEURS.navy)
    const designation = `${activite.code}  ${activite.designation}`
    const lignes = doc.splitTextToSize(designation, largeur - 110) as string[]
    doc.text(lignes.slice(0, 2), 18, position + 5)

    doc.setFont('helvetica', 'normal')
    doc.setFontSize(7)
    doc.setTextColor(...PDF_COULEURS.gris)
    const periode = `${formaterDateCourte(activite.dateDebutPrevue)} - ${formaterDateCourte(activite.dateFinPrevue)}   ${texteOuTiret(activite.statutLibelle)}`
    doc.text(periode, largeur - 18, position + 5, { align: 'right' })
    doc.setTextColor(...PDF_COULEURS.encre)

    return position + 15
}

function ajouterTitrePartie(
    doc: DocPdf,
    numero: number,
    titre: string,
    sousTitre: string,
    y: number,
): number {
    const position = garantirEspace(doc, y, 24)
    doc.setFont('helvetica', 'bold')
    doc.setFontSize(8)
    doc.setTextColor(...PDF_COULEURS.bleu)
    doc.text(`PARTIE ${numero}`, 14, position)
    doc.setFontSize(15)
    doc.setTextColor(...PDF_COULEURS.navy)
    doc.text(titre, 14, position + 8)
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(8)
    doc.setTextColor(...PDF_COULEURS.gris)
    doc.text(sousTitre, 14, position + 14)
    doc.setTextColor(...PDF_COULEURS.encre)
    return position + 20
}

function compterPar<T>(elements: T[], cle: (element: T) => string): Map<string, number> {
    const resultat = new Map<string, number>()
    for (const element of elements) {
        const valeur = cle(element) || 'Non renseigné'
        resultat.set(valeur, (resultat.get(valeur) ?? 0) + 1)
    }
    return resultat
}

function dessinerPageGarde(
    doc: DocPdf,
    data: ActiviteExportResponse,
    service: ServiceExport,
    activites: ActiviteExport[],
): void {
    const largeur = doc.internal.pageSize.getWidth()
    const marge = 14
    const contenu = largeur - marge * 2

    doc.setFont('helvetica', 'bold')
    doc.setFontSize(10)
    doc.setTextColor(...PDF_COULEURS.navy)
    doc.text('Banque centrale de Madagascar', largeur / 2, 12, { align: 'center' })
    doc.setFontSize(9)
    doc.text(
        `${data.direction}  ·  ${(data.departement ?? '').toUpperCase()}`,
        largeur / 2,
        20,
        { align: 'center' },
    )
    lignePdf(doc, marge, 25, largeur - marge, 25, PDF_COULEURS.navyClair, 0.4)

    doc.setFontSize(20)
    doc.text('Rapport des activités', largeur / 2, 42, { align: 'center' })
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(10.5)
    doc.setTextColor(...PDF_COULEURS.gris)
    doc.text(`${service.nom} — Exercice ${data.annee}`, largeur / 2, 50, { align: 'center' })

    const metadonnees: [string, string][] = [
        ['DIRECTION', data.direction],
        ['DÉPARTEMENT', texteOuTiret(data.departement)],
        ['SERVICE', service.nom],
        ['EXERCICE', String(data.annee)],
        ['DATE DE GÉNÉRATION', formaterDateHeure(data.dateGeneration)],
    ]
    const largeurColonne = contenu / metadonnees.length
    rectanglePdf(doc, marge, 57, contenu, 20, PDF_COULEURS.bleuClair, PDF_COULEURS.ligne)
    metadonnees.forEach(([libelle, valeur], index) => {
        const x = marge + index * largeurColonne
        doc.setFont('helvetica', 'bold')
        doc.setFontSize(6.8)
        doc.setTextColor(...PDF_COULEURS.gris)
        doc.text(libelle, x + 3, 62)
        doc.setFont('helvetica', 'bold')
        doc.setFontSize(8)
        doc.setTextColor(...PDF_COULEURS.navy)
        const lignes = doc.splitTextToSize(valeur, largeurColonne - 6) as string[]
        doc.text(lignes.slice(0, 2), x + 3, 70)
    })
    if (metadonnees.length > 1) {
        for (let index = 1; index < metadonnees.length; index++) {
            lignePdf(
                doc,
                marge + index * largeurColonne,
                60,
                marge + index * largeurColonne,
                74,
                PDF_COULEURS.ligne,
                0.2,
            )
        }
    }

    const sousActivites = activites.flatMap(activite => activite.sousActivites)
    const suivis = sousActivites.flatMap(sousActivite => sousActivite.suivis)
    const avancementMoyen = activites.length > 0
        ? Math.round(
            activites.reduce((somme, activite) => somme + Number(activite.avancement ?? 0), 0) /
            activites.length,
        )
        : 0
    const largeurCarte = (contenu - 12) / 4
    const cartes: [string, string, CouleurPdf][] = [
        [String(activites.length), 'Activités', PDF_COULEURS.bleu],
        [String(sousActivites.length), 'Sous-activités', PDF_COULEURS.navyClair],
        [String(suivis.length), 'Suivis enregistrés', PDF_COULEURS.vert],
        [`${avancementMoyen} %`, 'Avancement moyen des activités', PDF_COULEURS.orange],
    ]
    cartes.forEach(([valeur, libelle, accent], index) => {
        dessinerCarteKpi(
            doc,
            marge + index * (largeurCarte + 4),
            83,
            largeurCarte,
            26,
            valeur,
            libelle,
            accent,
        )
    })

    const statutsActivite = compterPar(activites, activite => texteOuTiret(activite.statutLibelle))
    const statutsSousActivite = compterPar(sousActivites, sousActivite =>
        texteOuTiret(sousActivite.statutLibelle),
    )
    const prioritesActivite = compterPar(activites, activite => texteOuTiret(activite.priorite))
    const largeurGraphique = (contenu - 8) / 3

    const construireEntrees = (
        comptages: Map<string, number>,
        total: number,
    ): EntreeGraphiquePdf[] => [...comptages.entries()]
        .sort((a, b) => b[1] - a[1])
        .map(([label, valeur]) => ({
            label,
            valeur,
            couleur: couleurStatutPdf(label),
        }))
        .concat(total > 0 && comptages.size === 0
            ? [{ label: 'Aucune donnée', valeur: 0, couleur: PDF_COULEURS.grisClair }]
            : [])

    dessinerMiniGraphique(
        doc,
        marge,
        115,
        largeurGraphique,
        'Activités par statut',
        construireEntrees(statutsActivite, activites.length),
    )
    dessinerMiniGraphique(
        doc,
        marge + largeurGraphique + 4,
        115,
        largeurGraphique,
        'Sous-activités par statut',
        construireEntrees(statutsSousActivite, sousActivites.length),
    )
    dessinerMiniGraphique(
        doc,
        marge + (largeurGraphique + 4) * 2,
        115,
        largeurGraphique,
        'Activités par priorité',
        [...prioritesActivite.entries()]
            .sort((a, b) => b[1] - a[1])
            .map(([label, valeur]) => ({
                label,
                valeur,
                couleur: couleurPrioritePdf(label),
            })),
    )

    const sousActivitesEnRetard = sousActivites.filter(sousActivite =>
        /retard/i.test(texteOuTiret(sousActivite.statutLibelle)),
    ).length
    const activitesAvecRetard = activites.filter(activite =>
        activite.sousActivites.some(sousActivite =>
            /retard/i.test(texteOuTiret(sousActivite.statutLibelle)),
        ),
    ).length
    rectanglePdf(doc, marge, 171, contenu, 13, PDF_COULEURS.rougeClair, PDF_COULEURS.rouge)
    doc.setFont('helvetica', 'bold')
    doc.setFontSize(8.2)
    doc.setTextColor(...PDF_COULEURS.rouge)
    doc.text(
        `Point d'attention : ${sousActivitesEnRetard} sous-activités en retard sur ${sousActivites.length}, réparties sur ${activitesAvecRetard} activités.`,
        marge + 4,
        179,
    )
    doc.setTextColor(...PDF_COULEURS.encre)
}

function genererPdfService(
    data: ActiviteExportResponse,
    service: ServiceExport,
    activites: ActiviteExport[],
): void {
    const doc = new jsPDF({
        orientation: 'landscape',
        unit: 'mm',
        format: 'a4',
    }) as DocPdf

    dessinerPageGarde(doc, data, service, activites)
    doc.addPage()

    const sousActivites = activites.flatMap(activite => activite.sousActivites)
    const avancementMoyen = activites.length > 0
        ? Math.round(
            activites.reduce((somme, activite) => somme + Number(activite.avancement ?? 0), 0) /
            activites.length,
        )
        : 0
    let y = 24

    y = ajouterTitrePartie(
        doc,
        1,
        'Liste des activités',
        `${activites.length} activités · avancement moyen ${avancementMoyen} %`,
        y,
    )

    autoTable(doc, {
        startY: y,
        head: [[
            'CODE',
            'DÉSIGNATION',
            'TYPE',
            'PRIORITÉ',
            'RESPONSABLE(S)',
            'DÉBUT',
            'ÉCHÉANCE',
            'STATUT',
            'AVANC.',
        ]],
        body: activites.map(activite => [
            activite.code,
            activite.designation,
            activite.pta ? 'PTA' : 'NON_PTA',
            texteOuTiret(activite.priorite),
            responsablesTexte(activite.responsables),
            formaterDateCourte(activite.dateDebutPrevue),
            formaterDateCourte(activite.dateFinPrevue),
            texteOuTiret(activite.statutLibelle),
            `${Math.round(activite.avancement ?? 0)} %`,
        ]),
        theme: 'grid',
        headStyles: {
            fillColor: PDF_COULEURS.navy,
            textColor: PDF_COULEURS.blanc,
            fontSize: 7.3,
            halign: 'left',
            fontStyle: 'bold',
        },
        styles: {
            fontSize: 7.2,
            cellPadding: 1.6,
            overflow: 'linebreak',
            valign: 'middle',
            textColor: PDF_COULEURS.encre,
            lineColor: PDF_COULEURS.ligne,
            lineWidth: 0.1,
        },
        alternateRowStyles: { fillColor: PDF_COULEURS.bleuClair },
        columnStyles: {
            0: { cellWidth: 24 },
            1: { cellWidth: 70 },
            2: { cellWidth: 17 },
            3: { cellWidth: 20 },
            4: { cellWidth: 48 },
            5: { cellWidth: 20 },
            6: { cellWidth: 20 },
            7: { cellWidth: 26 },
            8: { cellWidth: 24 },
        },
        didParseCell: cellData => {
            if (cellData.section !== 'body') return
            if (cellData.column.index === 3) {
                cellData.cell.styles.textColor = couleurPrioritePdf(
                    String(cellData.cell.raw ?? ''),
                )
                cellData.cell.styles.fontStyle = 'bold'
            }
            if (cellData.column.index === 7) {
                cellData.cell.styles.textColor = couleurStatutPdf(
                    String(cellData.cell.raw ?? ''),
                )
                cellData.cell.styles.fontStyle = 'bold'
            }
        },
        rowPageBreak: 'avoid',
        margin: { left: 14, right: 14, top: 20, bottom: 18 },
        willDrawPage: () => ajouterEnTete(doc, data, service),
    })

    doc.addPage()
    y = 24
    y = ajouterTitrePartie(
        doc,
        2,
        'Liste des sous-activités',
        `${sousActivites.length} sous-activités réparties sur ${activites.length} activités`,
        y,
    )

    if (activites.length === 0) {
        doc.setFont('helvetica', 'italic')
        doc.setFontSize(9)
        doc.setTextColor(...PDF_COULEURS.gris)
        doc.text('Aucune activité pour ce service.', 14, y)
        doc.setTextColor(...PDF_COULEURS.encre)
    }

    for (const activite of activites) {
        y = dessinerBandeActivite(doc, activite, y)

        if (activite.sousActivites.length === 0) {
            doc.setFont('helvetica', 'italic')
            doc.setFontSize(8)
            doc.setTextColor(...PDF_COULEURS.gris)
            doc.text('Aucune sous-activité.', 18, y)
            doc.setTextColor(...PDF_COULEURS.encre)
            y += 8
            continue
        }

        autoTable(doc, {
            startY: y,
            head: [[
                'CODE',
                'DÉSIGNATION',
                'RESPONSABLE(S)',
                'DÉBUT',
                'ÉCHÉANCE',
                'STATUT',
                'AVANC.',
            ]],
            body: activite.sousActivites.map(sousActivite => [
                sousActivite.code,
                sousActivite.designation,
                responsablesTexte(sousActivite.responsables),
                formaterDateCourte(sousActivite.dateDebutPrevue),
                formaterDateCourte(sousActivite.dateFinPrevue),
                texteOuTiret(sousActivite.statutLibelle),
                `${Math.round(sousActivite.avancement ?? 0)} %`,
            ]),
            theme: 'grid',
            headStyles: {
                fillColor: PDF_COULEURS.navyClair,
                textColor: PDF_COULEURS.blanc,
                fontSize: 7.3,
                halign: 'left',
                fontStyle: 'bold',
            },
            styles: {
                fontSize: 7.2,
                cellPadding: 1.6,
                overflow: 'linebreak',
                valign: 'middle',
                textColor: PDF_COULEURS.encre,
                lineColor: PDF_COULEURS.ligne,
                lineWidth: 0.1,
            },
            alternateRowStyles: { fillColor: PDF_COULEURS.bleuClair },
            columnStyles: {
                0: { cellWidth: 24 },
                1: { cellWidth: 92 },
                2: { cellWidth: 55 },
                3: { cellWidth: 20 },
                4: { cellWidth: 20 },
                5: { cellWidth: 32 },
                6: { cellWidth: 26 },
            },
            didParseCell: cellData => {
                if (cellData.section !== 'body' || cellData.column.index !== 5) return
                cellData.cell.styles.textColor = couleurStatutPdf(
                    String(cellData.cell.raw ?? ''),
                )
                cellData.cell.styles.fontStyle = 'bold'
            },
            rowPageBreak: 'avoid',
            margin: { left: 14, right: 14, top: 20, bottom: 18 },
            willDrawPage: () => ajouterEnTete(doc, data, service),
        })

        y = finTableau(doc, y) + 7
    }

    doc.addPage()
    y = 24
    const totalSuivis = sousActivites.reduce(
        (somme, sousActivite) => somme + sousActivite.suivis.length,
        0,
    )
    y = ajouterTitrePartie(
        doc,
        3,
        'Historique des suivis',
        `${totalSuivis} suivis enregistrés sur ${sousActivites.length} sous-activités`,
        y,
    )

    let auMoinsUnSuivi = false

    for (const activite of activites) {
        const sousActivitesAvecSuivis = activite.sousActivites.filter(
            sousActivite => sousActivite.suivis.length > 0,
        )
        if (sousActivitesAvecSuivis.length === 0) continue

        auMoinsUnSuivi = true
        y = dessinerBandeActivite(doc, activite, y)

        for (const sousActivite of sousActivitesAvecSuivis) {
            y = garantirEspace(doc, y, 18)
            doc.setFont('helvetica', 'bold')
            doc.setFontSize(8)
            doc.setTextColor(...PDF_COULEURS.navy)
            doc.text(`${sousActivite.code}  ${sousActivite.designation}`, 18, y)
            doc.setTextColor(...PDF_COULEURS.encre)
            y += 4

            autoTable(doc, {
                startY: y,
                head: [[
                    'DATE DU SUIVI',
                    'MEMBRE',
                    'AVANC.',
                    'OBSERVATION',
                ]],
                body: sousActivite.suivis.map(suivi => [
                    formaterDateHeure(suivi.dateChangement),
                    texteOuTiret(suivi.utilisateur),
                    `${Math.round(suivi.valeurPourcentage ?? 0)} %`,
                    texteOuTiret(suivi.commentaire),
                ]),
                theme: 'grid',
                headStyles: {
                    fillColor: PDF_COULEURS.navyClair,
                    textColor: PDF_COULEURS.blanc,
                    fontSize: 7.3,
                    halign: 'left',
                    fontStyle: 'bold',
                },
                styles: {
                    fontSize: 7.2,
                    cellPadding: 1.6,
                    overflow: 'linebreak',
                    valign: 'middle',
                    textColor: PDF_COULEURS.encre,
                    lineColor: PDF_COULEURS.ligne,
                    lineWidth: 0.1,
                },
                alternateRowStyles: { fillColor: PDF_COULEURS.bleuClair },
                columnStyles: {
                    0: { cellWidth: 34 },
                    1: { cellWidth: 55 },
                    2: { cellWidth: 26 },
                    3: { cellWidth: 154 },
                },
                didParseCell: cellData => {
                    if (cellData.section !== 'body' || cellData.column.index !== 2) return
                    const valeur = Number(
                        String(cellData.cell.raw ?? '').replace('%', '').trim(),
                    ) || 0
                    cellData.cell.styles.textColor = valeur >= 80
                        ? PDF_COULEURS.vert
                        : valeur >= 50
                            ? PDF_COULEURS.orange
                            : PDF_COULEURS.rouge
                    cellData.cell.styles.fontStyle = 'bold'
                },
                rowPageBreak: 'avoid',
                margin: { left: 14, right: 14, top: 20, bottom: 18 },
                willDrawPage: () => ajouterEnTete(doc, data, service),
            })

            y = finTableau(doc, y) + 6
        }
    }

    if (!auMoinsUnSuivi) {
        doc.setFont('helvetica', 'italic')
        doc.setFontSize(9)
        doc.setTextColor(...PDF_COULEURS.gris)
        doc.text('Aucun suivi enregistré pour ce service.', 14, y)
        doc.setTextColor(...PDF_COULEURS.encre)
    }

    ajouterPiedDePage(doc, data)
    doc.save(nomFichierPdf(service, data.annee))
}

function servicesDepuisActivites(activites: ActiviteExport[]): ServiceExport[] {
    const services = new Map<number, string>()

    for (const activite of activites) {
        if (activite.serviceId !== null && activite.serviceNom) {
            services.set(activite.serviceId, activite.serviceNom)
        }
    }

    return [...services.entries()].map(([id, nom]) => ({ id, nom }))
}

export function exporterPdf(data: ActiviteExportResponse): void {
    const services = data.serviceId !== null
        ? [{ id: data.serviceId, nom: data.service ?? 'Service' }]
        : (data.services.length > 0 ? data.services : servicesDepuisActivites(data.activites))

    for (const service of services) {
        const activites = data.activites.filter(
            activite => activite.serviceId === service.id,
        )

        genererPdfService(data, service, activites)
    }
}

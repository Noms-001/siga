import { strFromU8, strToU8, unzipSync, zipSync } from 'fflate'
import { jsPDF } from 'jspdf'
import autoTable from 'jspdf-autotable'
import * as XLSX from 'xlsx-js-style'

import type {
    PlanActionExport,
    PlanActionExportResponse,
    ServiceExportPlan,
} from '@/types/exportPlanAction'

function formaterDateCourte(valeur: string | null | undefined): string {
    if (!valeur) return '—'
    const jourSeul = /^(\d{4})-(\d{2})-(\d{2})$/.exec(valeur)
    if (jourSeul) return `${jourSeul[3]}/${jourSeul[2]}/${jourSeul[1]}`
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
    return valeur && valeur.trim() ? valeur.trim() : '—'
}

function joindreListe(valeurs: string[]): string {
    return valeurs.length > 0 ? valeurs.join(', ') : '—'
}

function assainirNom(valeur: string): string {
    return valeur
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[^a-zA-Z0-9]+/g, '_')
        .replace(/^_+|_+$/g, '')
        .slice(0, 80)
}

function origineTexte(plan: PlanActionExport): string {
    const sources = plan.origines.filter(origine => origine.estPrincipale)
    const origines = sources.length > 0 ? sources : plan.origines
    if (origines.length === 0) return '—'
    return joindreListe(origines.map(origine => {
        if (origine.designation && origine.typeOrigine) {
            return `${origine.designation} (${origine.typeOrigine})`
        }
        return texteOuTiret(origine.designation ?? origine.typeOrigine ?? origine.code)
    }))
}

function activiteTexte(plan: PlanActionExport): string {
    if (plan.activites.length === 0) return '—'
    return joindreListe(plan.activites.map(activite =>
        `${texteOuTiret(activite.code)} ${texteOuTiret(activite.designation)}`.trim(),
    ))
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

function styleStatut(valeur: string | null | undefined): StyleCellule {
    const texte = (valeur ?? '').toLowerCase()
    if (texte.includes('retard') || texte.includes('annul') || texte.includes('rejet')) {
        return STYLE_ROUGE
    }
    if (texte.includes('attente') || texte.includes('brouillon')) return STYLE_ORANGE
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

function nomFichierExcel(data: PlanActionExportResponse): string {
    const service = data.service ?? 'Tous_Services'
    return `Export_Plans_Action_${assainirNom(service)}_${data.annee}.xlsx`
}

function nomFichierPdf(service: ServiceExportPlan, annee: number): string {
    return `Rapport_Plans_Action_${assainirNom(service.nom)}_${annee}.pdf`
}

export function exporterExcelPlans(data: PlanActionExportResponse): void {
    const classeur = XLSX.utils.book_new()
    const inclureService = data.serviceId === null

    const entetesPlans = [
        'Code',
        'Désignation',
        'Origine',
        'Activité(s) associée(s)',
        'Responsable(s)',
        ...(inclureService ? ['Service'] : []),
        'Date de début',
        'Date d’échéance',
        'Statut',
        'Taux d’avancement',
    ]
    const lignesPlans = data.plans.map(plan => [
        plan.code,
        texteOuTiret(plan.designation),
        origineTexte(plan),
        activiteTexte(plan),
        joindreListe(plan.responsables),
        ...(inclureService ? [texteOuTiret(plan.serviceNom ?? plan.servicesLies)] : []),
        formaterDateCourte(plan.dateDebutPrevue),
        formaterDateCourte(plan.dateFinPrevue),
        texteOuTiret(plan.statutLibelle),
        `${Math.round(plan.avancement)} %`,
    ])

    const entetesDetails = [
        'Code du plan',
        'Origine',
        'Type d’origine',
        'Activité associée',
        'Service de l’activité',
    ]
    const lignesDetails: (string | number)[][] = []
    for (const plan of data.plans) {
        if (plan.origines.length === 0 && plan.activites.length === 0) {
            lignesDetails.push([plan.code, '—', '—', '—', '—'])
            continue
        }
        for (const origine of plan.origines) {
            const activitesLiees = plan.activites.filter(
                activite => activite.origineCode === origine.code,
            )
            if (activitesLiees.length === 0) {
                lignesDetails.push([
                    plan.code,
                    texteOuTiret(origine.designation),
                    texteOuTiret(origine.typeOrigine),
                    '—',
                    '—',
                ])
                continue
            }
            for (const activite of activitesLiees) {
                lignesDetails.push([
                    plan.code,
                    texteOuTiret(origine.designation),
                    texteOuTiret(origine.typeOrigine),
                    `${texteOuTiret(activite.code)} ${texteOuTiret(activite.designation)}`.trim(),
                    texteOuTiret(activite.serviceNom),
                ])
            }
        }
    }

    const entetesSuivis = [
        'Date du suivi',
        'Code du plan',
        'Membre',
        'Taux d’avancement',
        'Observation',
        'Motif',
        'Statut',
    ]
    const lignesSuivis: (string | number)[][] = []
    for (const plan of data.plans) {
        for (const suivi of plan.suivis) {
            lignesSuivis.push([
                formaterDateHeure(suivi.dateChangement),
                plan.code,
                texteOuTiret(suivi.utilisateur),
                `${Math.round(suivi.valeurPourcentage)} %`,
                texteOuTiret(suivi.commentaire),
                texteOuTiret(suivi.motif),
                texteOuTiret(suivi.statutLibelle),
            ])
        }
    }

    const feuillePlans = XLSX.utils.aoa_to_sheet([entetesPlans, ...lignesPlans])
    feuillePlans['!cols'] = [
        { wch: 18 }, { wch: 42 }, { wch: 36 }, { wch: 42 }, { wch: 34 },
        ...(inclureService ? [{ wch: 30 }] : []),
        { wch: 14 }, { wch: 16 }, { wch: 18 }, { wch: 18 },
    ]

    const feuilleDetails = XLSX.utils.aoa_to_sheet([entetesDetails, ...lignesDetails])
    feuilleDetails['!cols'] = [
        { wch: 18 }, { wch: 36 }, { wch: 20 }, { wch: 52 }, { wch: 30 },
    ]

    const feuilleSuivis = XLSX.utils.aoa_to_sheet([entetesSuivis, ...lignesSuivis])
    feuilleSuivis['!cols'] = [
        { wch: 20 }, { wch: 18 }, { wch: 30 }, { wch: 18 }, { wch: 46 }, { wch: 36 }, { wch: 20 },
    ]

    appliquerStyleEntete(feuillePlans, entetesPlans.length)
    appliquerStyleEntete(feuilleDetails, entetesDetails.length)
    appliquerStyleEntete(feuilleSuivis, entetesSuivis.length)

    data.plans.forEach((plan, index) => {
        appliquerStyleLigne(
            feuillePlans,
            index + 1,
            entetesPlans.length,
            index % 2 === 0 ? STYLE_LIGNE : STYLE_LIGNE_ALTRE,
        )
        appliquerStyleCellule(feuillePlans, index + 1, 8, styleStatut(plan.statutLibelle))
        appliquerStyleCellule(feuillePlans, index + 1, 9, styleAvancement(plan.avancement))
    })

    lignesDetails.forEach((_, index) => {
        appliquerStyleLigne(
            feuilleDetails,
            index + 1,
            entetesDetails.length,
            index % 2 === 0 ? STYLE_LIGNE : STYLE_LIGNE_ALTRE,
        )
    })

    lignesSuivis.forEach((ligne, index) => {
        appliquerStyleLigne(
            feuilleSuivis,
            index + 1,
            entetesSuivis.length,
            index % 2 === 0 ? STYLE_LIGNE : STYLE_LIGNE_ALTRE,
        )
        const valeur = Number(String(ligne[3] ?? '').replace('%', '').trim()) || 0
        appliquerStyleCellule(feuilleSuivis, index + 1, 3, styleAvancement(valeur))
    })

    appliquerHauteursLignes(feuillePlans, lignesPlans.length + 1, 42)
    appliquerHauteursLignes(feuilleDetails, lignesDetails.length + 1, 32)
    appliquerHauteursLignes(feuilleSuivis, lignesSuivis.length + 1, 30)

    XLSX.utils.book_append_sheet(classeur, feuillePlans, 'Plans d’action')
    XLSX.utils.book_append_sheet(classeur, feuilleDetails, 'Origines et activités')
    XLSX.utils.book_append_sheet(classeur, feuilleSuivis, 'Suivis')

    const dernierePlans = `${XLSX.utils.encode_col(entetesPlans.length - 1)}${lignesPlans.length + 1}`
    const derniereDetails = `${XLSX.utils.encode_col(entetesDetails.length - 1)}${lignesDetails.length + 1}`
    const derniereSuivis = `${XLSX.utils.encode_col(entetesSuivis.length - 1)}${lignesSuivis.length + 1}`

    telechargerClasseur(classeur, nomFichierExcel(data), [
        { fichier: 'sheet1.xml', colonnesFigees: 2, derniereCellule: dernierePlans },
        { fichier: 'sheet2.xml', colonnesFigees: 2, derniereCellule: derniereDetails },
        { fichier: 'sheet3.xml', colonnesFigees: 2, derniereCellule: derniereSuivis },
    ])
}

type CouleurPdf = [number, number, number]

type DocPdf = jsPDF & { lastAutoTable?: { finalY: number } }

function couleur(r: number, g: number, b: number): CouleurPdf {
    return [r, g, b]
}

const PDF = {
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
    vert: couleur(23, 112, 58),
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
    couleurTrait: CouleurPdf,
    epaisseur = 0.2,
): void {
    doc.setDrawColor(...couleurTrait)
    doc.setLineWidth(epaisseur)
    doc.line(x1, y1, x2, y2)
}

function couleurStatutPdf(statut: string): CouleurPdf {
    const valeur = statut.toLowerCase()
    if (valeur.includes('retard') || valeur.includes('annul')) return PDF.rouge
    if (valeur.includes('attente') || valeur.includes('brouillon')) return PDF.orange
    if (valeur.includes('termin') || valeur.includes('valid')) return PDF.vert
    if (valeur.includes('cours')) return PDF.bleu
    return PDF.encre
}

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

function ajouterEnTete(
    doc: DocPdf,
    data: PlanActionExportResponse,
    service: ServiceExportPlan,
): void {
    if (doc.getCurrentPageInfo().pageNumber === 1) return
    const largeur = doc.internal.pageSize.getWidth()
    doc.setFont('helvetica', 'bold')
    doc.setFontSize(9)
    doc.setTextColor(...PDF.navy)
    doc.text('Rapport des plans d’action', 14, 11)
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(8)
    doc.setTextColor(...PDF.gris)
    doc.text(`${service.nom}   Exercice ${data.annee}`, largeur - 14, 11, { align: 'right' })
    lignePdf(doc, 14, 14, largeur - 14, 14, PDF.ligne, 0.3)
    doc.setTextColor(...PDF.encre)
}

function ajouterPiedDePage(doc: DocPdf, data: PlanActionExportResponse): void {
    const total = doc.getNumberOfPages()
    const largeur = doc.internal.pageSize.getWidth()
    const hauteur = doc.internal.pageSize.getHeight()
    const pageCourante = doc.getCurrentPageInfo().pageNumber

    for (let page = 1; page <= total; page++) {
        doc.setPage(page)
        doc.setFont('helvetica', 'normal')
        doc.setFontSize(7.5)
        doc.setTextColor(...PDF.gris)
        doc.text(
            `${data.direction} - ${data.departement ?? 'Département'} - Généré le ${formaterDateHeure(data.dateGeneration)}`,
            14,
            hauteur - 7,
        )
        doc.text(`Page ${page} / ${total}`, largeur - 14, hauteur - 7, { align: 'right' })
    }

    doc.setPage(pageCourante)
    doc.setTextColor(...PDF.encre)
}

function dessinerCarteKpi(
    doc: jsPDF,
    x: number,
    y: number,
    largeur: number,
    valeur: string,
    libelle: string,
    accent: CouleurPdf,
): void {
    rectanglePdf(doc, x, y, largeur, 25, PDF.bleuClair, PDF.ligne)
    rectanglePdf(doc, x, y, 1.4, 25, accent)
    doc.setFont('helvetica', 'bold')
    doc.setFontSize(16)
    doc.setTextColor(...PDF.navy)
    doc.text(valeur, x + 5, y + 11)
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(7.2)
    doc.setTextColor(...PDF.gris)
    const lignes = doc.splitTextToSize(libelle, largeur - 9) as string[]
    doc.text(lignes.slice(0, 2), x + 5, y + 18)
    doc.setTextColor(...PDF.encre)
}

function dessinerPageGarde(
    doc: DocPdf,
    data: PlanActionExportResponse,
    service: ServiceExportPlan,
    plans: PlanActionExport[],
): void {
    const largeur = doc.internal.pageSize.getWidth()
    const marge = 14
    const contenu = largeur - marge * 2

    doc.setFont('helvetica', 'bold')
    doc.setFontSize(10)
    doc.setTextColor(...PDF.navy)
    doc.text('Banque centrale de Madagascar', largeur / 2, 12, { align: 'center' })
    doc.setFontSize(9)
    doc.text(
        `${data.direction}  ·  ${(data.departement ?? '').toUpperCase()}`,
        largeur / 2,
        20,
        { align: 'center' },
    )
    lignePdf(doc, marge, 25, largeur - marge, 25, PDF.navyClair, 0.4)

    doc.setFontSize(20)
    doc.text('Rapport des plans d’action', largeur / 2, 42, { align: 'center' })
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(10.5)
    doc.setTextColor(...PDF.gris)
    doc.text(`${service.nom} — Exercice ${data.annee}`, largeur / 2, 50, { align: 'center' })

    const metadonnees: [string, string][] = [
        ['DIRECTION', data.direction],
        ['DÉPARTEMENT', texteOuTiret(data.departement)],
        ['SERVICE', service.nom],
        ['EXERCICE', String(data.annee)],
        ['DATE DE GÉNÉRATION', formaterDateHeure(data.dateGeneration)],
    ]
    const largeurColonne = contenu / metadonnees.length
    rectanglePdf(doc, marge, 57, contenu, 20, PDF.bleuClair, PDF.ligne)
    metadonnees.forEach(([libelle, valeur], index) => {
        const x = marge + index * largeurColonne
        doc.setFont('helvetica', 'bold')
        doc.setFontSize(6.8)
        doc.setTextColor(...PDF.gris)
        doc.text(libelle, x + 3, 62)
        doc.setFontSize(8)
        doc.setTextColor(...PDF.navy)
        const lignes = doc.splitTextToSize(valeur, largeurColonne - 6) as string[]
        doc.text(lignes.slice(0, 2), x + 3, 70)
    })
    for (let index = 1; index < metadonnees.length; index++) {
        lignePdf(
            doc,
            marge + index * largeurColonne,
            60,
            marge + index * largeurColonne,
            74,
            PDF.ligne,
            0.2,
        )
    }

    const nombreOrigines = plans.reduce((somme, plan) => somme + plan.origines.length, 0)
    const nombreActivites = plans.reduce((somme, plan) => somme + plan.activites.length, 0)
    const avancementMoyen = plans.length > 0
        ? Math.round(plans.reduce((somme, plan) => somme + plan.avancement, 0) / plans.length)
        : 0
    const largeurCarte = (contenu - 12) / 4
    const cartes: [string, string, CouleurPdf][] = [
        [String(plans.length), 'Plans d’action', PDF.bleu],
        [String(nombreOrigines), 'Origines enregistrées', PDF.navyClair],
        [String(nombreActivites), 'Activités associées', PDF.vert],
        [`${avancementMoyen} %`, 'Avancement moyen', PDF.orange],
    ]
    cartes.forEach(([valeur, libelle, accent], index) => {
        dessinerCarteKpi(
            doc,
            marge + index * (largeurCarte + 4),
            83,
            largeurCarte,
            valeur,
            libelle,
            accent,
        )
    })

    const aujourdHui = new Date()
    aujourdHui.setHours(0, 0, 0, 0)
    const plansEnRetard = plans.filter(plan => {
        if (!plan.dateFinPrevue) return false
        if (plan.statutCode === 'TERMINEE' || plan.statutCode === 'ANNULEE') return false
        return new Date(plan.dateFinPrevue) < aujourdHui
    }).length

    rectanglePdf(doc, marge, 115, contenu, 13, PDF.rougeClair, PDF.rouge)
    doc.setFont('helvetica', 'bold')
    doc.setFontSize(8.2)
    doc.setTextColor(...PDF.rouge)
    doc.text(
        `Point d’attention : ${plansEnRetard} plan(s) d’action ont une échéance dépassée sur ${plans.length}.`,
        marge + 4,
        123,
    )
    doc.setTextColor(...PDF.encre)
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
    doc.setTextColor(...PDF.bleu)
    doc.text(`PARTIE ${numero}`, 14, position)
    doc.setFontSize(15)
    doc.setTextColor(...PDF.navy)
    doc.text(titre, 14, position + 8)
    doc.setFont('helvetica', 'normal')
    doc.setFontSize(8)
    doc.setTextColor(...PDF.gris)
    doc.text(sousTitre, 14, position + 14)
    doc.setTextColor(...PDF.encre)
    return position + 20
}

function dessinerBandePlan(doc: DocPdf, plan: PlanActionExport, y: number): number {
    const largeur = doc.internal.pageSize.getWidth()
    const position = garantirEspace(doc, y, 20)
    rectanglePdf(doc, 14, position, largeur - 28, 12, PDF.bleuClair, PDF.ligne)
    rectanglePdf(doc, 14, position, 1.4, 12, PDF.bleu)

    doc.setFont('helvetica', 'bold')
    doc.setFontSize(8)
    doc.setTextColor(...PDF.navy)
    const designation = `${plan.code}  ${plan.designation}`
    const lignes = doc.splitTextToSize(designation, largeur - 120) as string[]
    doc.text(lignes.slice(0, 2), 18, position + 5)

    doc.setFont('helvetica', 'normal')
    doc.setFontSize(7)
    doc.setTextColor(...PDF.gris)
    const periode = `${formaterDateCourte(plan.dateFinPrevue)}   ${texteOuTiret(plan.statutLibelle)}`
    doc.text(periode, largeur - 18, position + 5, { align: 'right' })
    doc.setTextColor(...PDF.encre)
    return position + 15
}

function optionsTableau(
    doc: DocPdf,
    data: PlanActionExportResponse,
    service: ServiceExportPlan,
) {
    return {
        theme: 'grid' as const,
        headStyles: {
            fillColor: PDF.navyClair,
            textColor: PDF.blanc,
            fontSize: 7.3,
            halign: 'left' as const,
            fontStyle: 'bold' as const,
        },
        styles: {
            fontSize: 7.2,
            cellPadding: 1.6,
            overflow: 'linebreak' as const,
            valign: 'middle' as const,
            textColor: PDF.encre,
            lineColor: PDF.ligne,
            lineWidth: 0.1,
        },
        alternateRowStyles: { fillColor: PDF.bleuClair },
        rowPageBreak: 'avoid' as const,
        margin: { left: 14, right: 14, top: 20, bottom: 18 },
        willDrawPage: () => ajouterEnTete(doc, data, service),
    }
}

function genererPdfService(
    data: PlanActionExportResponse,
    service: ServiceExportPlan,
    plans: PlanActionExport[],
): void {
    const doc = new jsPDF({
        orientation: 'landscape',
        unit: 'mm',
        format: 'a4',
    }) as DocPdf

    dessinerPageGarde(doc, data, service, plans)
    doc.addPage()

    const options = optionsTableau(doc, data, service)
    let y = 24
    const avancementMoyen = plans.length > 0
        ? Math.round(plans.reduce((somme, plan) => somme + plan.avancement, 0) / plans.length)
        : 0

    y = ajouterTitrePartie(
        doc,
        1,
        'Liste des plans d’action',
        `${plans.length} plans · avancement moyen ${avancementMoyen} %`,
        y,
    )

    autoTable(doc, {
        ...options,
        startY: y,
        head: [[
            'CODE', 'DÉSIGNATION', 'ORIGINE', 'ACTIVITÉ ASSOCIÉE', 'RESPONSABLE(S)',
            'DÉBUT', 'ÉCHÉANCE', 'STATUT', 'AVANC.',
        ]],
        body: plans.map(plan => [
            plan.code,
            texteOuTiret(plan.designation),
            origineTexte(plan),
            activiteTexte(plan),
            joindreListe(plan.responsables),
            formaterDateCourte(plan.dateDebutPrevue),
            formaterDateCourte(plan.dateFinPrevue),
            texteOuTiret(plan.statutLibelle),
            `${Math.round(plan.avancement)} %`,
        ]),
        columnStyles: {
            0: { cellWidth: 20 },
            1: { cellWidth: 58 },
            2: { cellWidth: 34 },
            3: { cellWidth: 38 },
            4: { cellWidth: 35 },
            5: { cellWidth: 17 },
            6: { cellWidth: 17 },
            7: { cellWidth: 25 },
            8: { cellWidth: 25 },
        },
        didParseCell: cellData => {
            if (cellData.section !== 'body') return
            if (cellData.column.index === 7) {
                cellData.cell.styles.textColor = couleurStatutPdf(String(cellData.cell.raw ?? ''))
                cellData.cell.styles.fontStyle = 'bold'
            }
            if (cellData.column.index === 8) {
                const valeur = Number(String(cellData.cell.raw ?? '').replace('%', '').trim()) || 0
                cellData.cell.styles.textColor = valeur >= 100
                    ? PDF.vert
                    : valeur >= 50
                        ? PDF.orange
                        : PDF.rouge
                cellData.cell.styles.fontStyle = 'bold'
            }
        },
    })

    doc.addPage()
    y = 24
    const nombreDetails = plans.reduce(
        (somme, plan) => somme + Math.max(plan.origines.length, plan.activites.length, 1),
        0,
    )
    y = ajouterTitrePartie(
        doc,
        2,
        'Origines et activités associées',
        `${nombreDetails} liaison(s) répartie(s) sur ${plans.length} plans`,
        y,
    )

    for (const plan of plans) {
        y = dessinerBandePlan(doc, plan, y)
        const lignes: string[][] = []
        for (const origine of plan.origines) {
            const activitesLiees = plan.activites.filter(
                activite => activite.origineCode === origine.code,
            )
            if (activitesLiees.length === 0) {
                lignes.push([
                    texteOuTiret(origine.designation),
                    texteOuTiret(origine.typeOrigine),
                    '—',
                    '—',
                ])
                continue
            }
            for (const activite of activitesLiees) {
                lignes.push([
                    texteOuTiret(origine.designation),
                    texteOuTiret(origine.typeOrigine),
                    `${texteOuTiret(activite.code)} ${texteOuTiret(activite.designation)}`.trim(),
                    texteOuTiret(activite.serviceNom),
                ])
            }
        }
        if (lignes.length === 0) {
            doc.setFont('helvetica', 'italic')
            doc.setFontSize(8)
            doc.setTextColor(...PDF.gris)
            doc.text('Aucune origine ni activité associée.', 18, y)
            doc.setTextColor(...PDF.encre)
            y += 8
            continue
        }

        autoTable(doc, {
            ...options,
            startY: y,
            head: [['ORIGINE', 'TYPE', 'ACTIVITÉ ASSOCIÉE', 'SERVICE']],
            body: lignes,
            columnStyles: {
                0: { cellWidth: 55 },
                1: { cellWidth: 30 },
                2: { cellWidth: 104 },
                3: { cellWidth: 80 },
            },
        })
        y = finTableau(doc, y) + 7
    }

    doc.addPage()
    y = 24
    const totalSuivis = plans.reduce((somme, plan) => somme + plan.suivis.length, 0)
    y = ajouterTitrePartie(
        doc,
        3,
        'Historique des suivis',
        `${totalSuivis} suivi(s) enregistré(s) sur ${plans.length} plans`,
        y,
    )

    let auMoinsUnSuivi = false
    for (const plan of plans) {
        if (plan.suivis.length === 0) continue
        auMoinsUnSuivi = true
        y = dessinerBandePlan(doc, plan, y)

        autoTable(doc, {
            ...options,
            startY: y,
            head: [['DATE DU SUIVI', 'MEMBRE', 'AVANC.', 'OBSERVATION', 'MOTIF']],
            body: plan.suivis.map(suivi => [
                formaterDateHeure(suivi.dateChangement),
                texteOuTiret(suivi.utilisateur),
                `${Math.round(suivi.valeurPourcentage)} %`,
                texteOuTiret(suivi.commentaire),
                texteOuTiret(suivi.motif),
            ]),
            columnStyles: {
                0: { cellWidth: 34 },
                1: { cellWidth: 50 },
                2: { cellWidth: 25 },
                3: { cellWidth: 90 },
                4: { cellWidth: 70 },
            },
            didParseCell: cellData => {
                if (cellData.section !== 'body' || cellData.column.index !== 2) return
                const valeur = Number(String(cellData.cell.raw ?? '').replace('%', '').trim()) || 0
                cellData.cell.styles.textColor = valeur >= 100
                    ? PDF.vert
                    : valeur >= 50
                        ? PDF.orange
                        : PDF.rouge
                cellData.cell.styles.fontStyle = 'bold'
            },
        })
        y = finTableau(doc, y) + 6
    }

    if (!auMoinsUnSuivi) {
        doc.setFont('helvetica', 'italic')
        doc.setFontSize(9)
        doc.setTextColor(...PDF.gris)
        doc.text('Aucun suivi enregistré pour ce service.', 14, y)
        doc.setTextColor(...PDF.encre)
    }

    ajouterPiedDePage(doc, data)
    doc.save(nomFichierPdf(service, data.annee))
}

function servicesDepuisPlans(data: PlanActionExportResponse): ServiceExportPlan[] {
    const services = new Map<number, string>()
    for (const plan of data.plans) {
        if (plan.serviceId !== null) {
            services.set(plan.serviceId, plan.serviceNom ?? 'Service')
        }
        for (const activite of plan.activites) {
            if (activite.serviceId !== null && activite.serviceNom) {
                services.set(activite.serviceId, activite.serviceNom)
            }
        }
    }
    return [...services.entries()].map(([id, nom]) => ({ id, nom }))
}

export function exporterPdfPlans(data: PlanActionExportResponse): void {
    const services = data.serviceId !== null
        ? [{ id: data.serviceId, nom: data.service ?? 'Service' }]
        : (data.services.length > 0 ? data.services : servicesDepuisPlans(data))

    for (const service of services) {
        const plans = data.plans.filter(plan =>
            plan.serviceId === service.id
            || plan.activites.some(activite => activite.serviceId === service.id),
        )
        genererPdfService(data, service, plans)
    }
}

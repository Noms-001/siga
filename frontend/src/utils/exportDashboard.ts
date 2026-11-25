import { strFromU8, strToU8, unzipSync, zipSync } from 'fflate'
import type {
    DashboardAvancement,
    DashboardEcheance,
    DashboardEcheances,
    DashboardKpi,
    DashboardPerimetre,
    DashboardPlanActionOrigine,
    DashboardStatut,
    DashboardPtaComparaison,
    DashboardService,
    DashboardStatutPriorite,
    DashboardTypeActivite,
    PeriodeDashboard,
} from '@/types/dashboard'
import type { SuiviGlobalPlanAction } from '@/types/planaction'

export interface DonneesExportDashboard {
    periode: PeriodeDashboard | null
    periodeLibelle: string
    perimetre: DashboardPerimetre | null
    perimetreLibelle: string
    serviceLibelle: string
    kpi: DashboardKpi | null
    avancement: DashboardAvancement | null
    statuts: DashboardStatut[]
    statutsPriorites: DashboardStatutPriorite[]
    services: DashboardService[]
    comparaisonPta: DashboardPtaComparaison | null
    echeances: DashboardEcheances | null
    suiviPlans: SuiviGlobalPlanAction | null
    typesActivite: DashboardTypeActivite[]
    plansOrigine: DashboardPlanActionOrigine[]
}

type ValeurIndicateur = {
    formule?: string
    cache: number
}

type Indicateurs = {
    total: ValeurIndicateur
    enCours: ValeurIndicateur
    terminees: ValeurIndicateur
    enRetard: ValeurIndicateur
    reportees: ValeurIndicateur
    nonCommencees: ValeurIndicateur
    suspendues: ValeurIndicateur
    annulees: ValeurIndicateur
    attente: ValeurIndicateur
    avancement: ValeurIndicateur
    sousTotal: ValeurIndicateur
    sousTerminees: ValeurIndicateur
    sousAvancement: ValeurIndicateur
    planTotal: ValeurIndicateur
    planNonCommencees: ValeurIndicateur
    planEnCours: ValeurIndicateur
    planEnRetard: ValeurIndicateur
    planTerminees: ValeurIndicateur
    planAvancement: ValeurIndicateur
}

type ContexteExport = {
    matrice: number[][]
    sommeStatuts: number
    services: DashboardService[]
    types: DashboardTypeActivite[]
    origines: DashboardPlanActionOrigine[]
    serviceDebut: number
    serviceFin: number
    typeDebut: number
    typeFin: number
    ptaDebut: number
    ptaFin: number
    origineDebut: number
    origineFin: number
    indicateurDebut: number
    noteLigne: number
    derniereActivite: number
    dernierePlan: number
    indicateurs: Indicateurs
}

type ActiviteExport = DashboardEcheance & {
    situation: string
}

type PlanEcheance = {
    situation: string
    code: string
    designation: string
    responsableLibelle: string | null
    dateFinPrevue: string
    jours: number
    avancement: number
}

const STATUTS_MODELE = [
    { code: 'EN_ATTENTE_VALIDATION', libelle: 'En attente de validation' },
    { code: 'NON_COMMENCEE', libelle: 'Non commencée' },
    { code: 'EN_COURS', libelle: 'En cours' },
    { code: 'SUSPENDUE', libelle: 'Suspendue' },
    { code: 'TERMINEE', libelle: 'Terminée' },
    { code: 'REPORTEE', libelle: 'Reportée' },
    { code: 'ANNULEE', libelle: 'Annulée' },
] as const

function echapperXml(valeur: string): string {
    return valeur
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&apos;')
}

function nombreXml(valeur: number): string {
    if (!Number.isFinite(valeur)) return '0'
    return String(Math.round(valeur * 1e12) / 1e12)
}

function attributStyle(style: number | string): string {
    return ` s="${style}"`
}

function celluleTexte(reference: string, style: number | string, valeur: string): string {
    return `<c r="${reference}"${attributStyle(style)} t="inlineStr"><is><t>${echapperXml(valeur)}</t></is></c>`
}

function celluleNombre(reference: string, style: number | string, valeur: number): string {
    return `<c r="${reference}"${attributStyle(style)} t="n"><v>${nombreXml(valeur)}</v></c>`
}

function celluleFormule(
    reference: string,
    style: number | string,
    formule: string,
    cache: string | number,
    cacheTexte = false,
): string {
    const type = cacheTexte ? ' t="str"' : ''
    const valeur = typeof cache === 'string' ? echapperXml(cache) : nombreXml(cache)
    return `<c r="${reference}"${attributStyle(style)}${type}><f>${echapperXml(formule)}</f><v>${valeur}</v></c>`
}

function ligneXml(numero: number, cellules: string[], hauteur?: number): string {
    const attribut = hauteur == null ? '' : ` ht="${hauteur}" customHeight="1"`
    return `<row r="${numero}"${attribut}>${cellules.join('')}</row>`
}

function remplacerSheetData(xml: string, contenu: string): string {
    return xml.replace(/<sheetData>[\s\S]*?<\/sheetData>/, `<sheetData>${contenu}</sheetData>`)
}

function remplacerDimension(xml: string, reference: string): string {
    return xml.replace(/<dimension ref="[^"]*"\s*\/>/, `<dimension ref="${reference}"/>`)
}

function remplacerAutoFilter(xml: string, reference: string): string {
    return xml.replace(/<autoFilter ref="[^"]*"\s*\/>/, `<autoFilter ref="${reference}"/>`)
}

function remplacerSqref(xml: string, colonne: string, derniereLigne: number): string {
    const motif = new RegExp(`sqref="${colonne}3:${colonne}\\d+"`, 'g')
    return xml.replace(motif, `sqref="${colonne}3:${colonne}${derniereLigne}"`)
}

function extraireStyleCellule(xml: string, reference: string): string {
    const cellule = new RegExp(`<c r="${reference}"([^>]*)`).exec(xml)
    if (!cellule) return '0'
    const style = /s="(\d+)"/.exec(cellule[1] ?? '')
    return style?.[1] ?? '0'
}

function remplacerCellule(xml: string, reference: string, cellule: string): string {
    const motif = new RegExp(`<c r="${reference}"(?:\\s[^>]*)?(?:/>|>[\\s\\S]*?</c>)`)
    return xml.replace(motif, () => cellule)
}

function fichierClasseur(fichiers: Record<string, Uint8Array>, chemin: string): Uint8Array {
    const fichier = fichiers[chemin]
    if (!fichier) throw new Error(`Fichier du modèle introuvable : ${chemin}.`)
    return fichier
}

function patcherCelluleFormule(
    xml: string,
    reference: string,
    formule: string,
    cache: string | number,
    cacheTexte = false,
): string {
    const style = extraireStyleCellule(xml, reference)
    return remplacerCellule(xml, reference, celluleFormule(reference, style, formule, cache, cacheTexte))
}

function dateSerie(valeur: string | Date): number {
    const date =typeof valeur === 'string'
        ? new Date(valeur)
        : valeur
    if (Number.isNaN(date.getTime())) return 0
    const jour = Date.UTC(date.getFullYear(), date.getMonth(), date.getDate())
    return Math.floor((jour - Date.UTC(1899, 11, 30)) / 86400000)
}

function fraction(valeur: number | null | undefined): number {
    const nombre = Number(valeur)
    if (!Number.isFinite(nombre)) return 0
    return nombre / 100
}

function ouTiret(valeur: string | null | undefined): string {
    return valeur == null || valeur === '' ? '—' : valeur
}

function normaliser(valeur: string | null | undefined): string {
    return (valeur ?? '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toUpperCase()
        .replace(/[^A-Z]/g, '')
}

function indexStatut(statut: string | null | undefined, libelle: string | null | undefined): number {
    const code = normaliser(statut)
    const texte = normaliser(libelle)
    const equivalences: Record<string, number> = {
        ENATTENTEDEVALIDATION: 0,
        ATTENTEVALIDATION: 0,
        ENATTENTE: 0,
        NONCOMMENCEE: 1,
        ENCOURS: 2,
        SUSPENDUE: 3,
        TERMINEE: 4,
        VALIDEE: 4,
        REPORTEE: 5,
        ANNULEE: 6,
        REJETEE: 6,
        REJETE: 6,
    }
    if (equivalences[code] != null) return equivalences[code]
    if (equivalences[texte] != null) return equivalences[texte]
    return -1
}

function indexPriorite(priorite: string | null | undefined, libelle: string | null | undefined): number {
    const code = normaliser(priorite)
    const texte = normaliser(libelle)
    if (code === 'HAUTE' || code === 'CRITIQUE' || texte === 'HAUTE' || texte === 'CRITIQUE') return 0
    if (code === 'FAIBLE' || texte === 'FAIBLE') return 2
    return 1
}

function libelleJours(jours: number): string {
    if (jours > 0) return `${jours} j de retard`
    if (jours === 0) return "Aujourd'hui"
    return `dans ${Math.abs(jours)} j`
}

function nomFichier(): string {
    const maintenant = new Date()
    const date = [
        maintenant.getFullYear(),
        String(maintenant.getMonth() + 1).padStart(2, '0'),
        String(maintenant.getDate()).padStart(2, '0'),
    ].join('-')
    return `GESTIA_Tableau_de_Bord_${date}.xlsx`
}

function construireMatrice(statutsPriorites: DashboardStatutPriorite[]): number[][] {
    const matrice = STATUTS_MODELE.map(() => [0, 0, 0])
    for (const element of statutsPriorites) {
        const ligne = indexStatut(element.statut, element.statutLibelle)
        if (ligne < 0) continue
        const colonne = indexPriorite(element.priorite, element.prioriteLibelle)
        const lignee = matrice[ligne]
        if (!lignee) continue
        lignee[colonne] = (lignee[colonne] ?? 0) + (Number(element.nombre) || 0)
    }
    return matrice
}

function sommeLigne(matrice: number[][], index: number): number {
    return matrice[index]?.reduce((total, valeur) => total + valeur, 0) ?? 0
}

function sommeColonne(matrice: number[][], index: number): number {
    return matrice.reduce((total, ligne) => total + (ligne[index] ?? 0), 0)
}

function preparerContexte(donnees: DonneesExportDashboard): ContexteExport {
    const matrice = construireMatrice(donnees.statutsPriorites)
    const sommeStatuts = matrice.reduce((total, ligne) => total + ligne.reduce((somme, valeur) => somme + valeur, 0), 0)

    const activitesEnRetard = donnees.echeances?.enRetard ?? []
    const activitesProches = donnees.echeances?.echeanceProche ?? []
    const plansEnRetard = donnees.suiviPlans?.enRetard ?? []
    const plansProches = donnees.suiviPlans?.echeancesProchaines ?? []
    const derniereActivite = 2 + Math.max(5, activitesEnRetard.length) + activitesProches.length
    const dernierePlan = 2 + Math.max(5, plansEnRetard.length) + plansProches.length

    const serviceDebut = 24
    const serviceFin = serviceDebut + Math.max(1, donnees.services.length) - 1
    const typeSection = serviceFin + 2
    const typeDebut = typeSection + 2
    const typeFin = typeDebut + Math.max(1, donnees.typesActivite.length) - 1
    const ptaSection = typeFin + 2
    const ptaDebut = ptaSection + 2
    const ptaFin = ptaDebut + 1
    const origineSection = ptaFin + 2
    const origineDebut = origineSection + 2
    const origineFin = origineDebut + Math.max(1, donnees.plansOrigine.length) - 1
    const sectionIndicateurs = origineFin + 2
    const indicateurDebut = sectionIndicateurs + 2
    const noteLigne = indicateurDebut + 20

    const totalServices = donnees.services.reduce((total, service) => total + service.totalActivites, 0)
    const avancementServices = totalServices
        ? donnees.services.reduce((total, service) => total + service.totalActivites * fraction(service.avancement), 0) / totalServices
        : fraction(donnees.kpi?.avancementGlobal ?? donnees.avancement?.avancementGlobal)

    const totalOrigines = donnees.plansOrigine.reduce((total, origine) => total + origine.total, 0)
    const avancementOrigines = totalOrigines
        ? donnees.plansOrigine.reduce((total, origine) => total + origine.total * fraction(origine.avancement), 0) / totalOrigines
        : fraction(donnees.suiviPlans?.resume.avancementMoyen)

    const sousTotal = Number(donnees.avancement?.totalSousActivites) || 0
    const sousTerminees = Number(donnees.avancement?.sousActivitesTerminees) || 0
    const ind = (offset: number) => indicateurDebut + offset

    const indicateurs: Indicateurs = {
        total: { formule: 'E20', cache: sommeStatuts || Number(donnees.kpi?.totalActivites) || 0 },
        enCours: { formule: 'E15', cache: sommeStatuts ? sommeLigne(matrice, 2) : Number(donnees.kpi?.enCours) || 0 },
        terminees: { formule: 'E17', cache: sommeStatuts ? sommeLigne(matrice, 4) : Number(donnees.kpi?.terminees) || 0 },
        enRetard: {
            formule: `COUNTIF(Activités!$A$3:$A$${derniereActivite},"En retard")`,
            cache: Number(donnees.kpi?.enRetard) || activitesEnRetard.length,
        },
        reportees: { formule: 'E18', cache: sommeStatuts ? sommeLigne(matrice, 5) : Number(donnees.kpi?.reportees) || 0 },
        nonCommencees: { formule: 'E14', cache: sommeStatuts ? sommeLigne(matrice, 1) : Number(donnees.kpi?.nonCommencees) || 0 },
        suspendues: { formule: 'E16', cache: sommeStatuts ? sommeLigne(matrice, 3) : Number(donnees.kpi?.suspendues) || 0 },
        annulees: { formule: 'E19', cache: sommeStatuts ? sommeLigne(matrice, 6) : Number(donnees.kpi?.annulees) || 0 },
        attente: { formule: 'E13', cache: sommeStatuts ? sommeLigne(matrice, 0) : Number(donnees.kpi?.enAttenteValidation) || 0 },
        avancement: {
            formule: donnees.services.length
                ? `SUMPRODUCT(B${serviceDebut}:B${serviceFin},G${serviceDebut}:G${serviceFin})/SUM(B${serviceDebut}:B${serviceFin})`
                : undefined,
            cache: avancementServices,
        },
        sousTotal: { cache: sousTotal },
        sousTerminees: { cache: sousTerminees },
        sousAvancement: {
            formule: sousTotal ? `B${ind(12)}/B${ind(10)}` : undefined,
            cache: sousTotal ? sousTerminees / sousTotal : 0,
        },
        planTotal: {
            formule: donnees.plansOrigine.length ? `SUM(B${origineDebut}:B${origineFin})` : undefined,
            cache: totalOrigines || Number(donnees.suiviPlans?.resume.total) || 0,
        },
        planNonCommencees: { cache: Number(donnees.suiviPlans?.resume.nonCommences) || 0 },
        planEnCours: { cache: Number(donnees.suiviPlans?.resume.enCours) || 0 },
        planEnRetard: {
            formule: `COUNTIF(Plans!$A$3:$A$${dernierePlan},"En retard")`,
            cache: Number(donnees.suiviPlans?.resume.enRetard) || plansEnRetard.length,
        },
        planTerminees: { cache: Number(donnees.suiviPlans?.resume.termines) || 0 },
        planAvancement: {
            formule: totalOrigines ? `SUMPRODUCT(B${origineDebut}:B${origineFin},C${origineDebut}:C${origineFin})/SUM(B${origineDebut}:B${origineFin})` : undefined,
            cache: avancementOrigines,
        },
    }

    return {
        matrice,
        sommeStatuts,
        services: donnees.services,
        types: donnees.typesActivite,
        origines: donnees.plansOrigine,
        serviceDebut,
        serviceFin,
        typeDebut,
        typeFin,
        ptaDebut,
        ptaFin,
        origineDebut,
        origineFin,
        indicateurDebut,
        noteLigne,
        derniereActivite,
        dernierePlan,
        indicateurs,
    }
}

function genererSheetDataDonnees(donnees: DonneesExportDashboard, contexte: ContexteExport): string {
    const lignes: string[] = []
    const maintenant = new Date()

    lignes.push(ligneXml(1, [celluleTexte('A1', 45, 'Données source du tableau de bord')]))
    lignes.push(ligneXml(2, [celluleTexte('A2', 46, 'Texte bleu = valeur saisie (issue de l’export GESTIA) · texte noir = formule calculée. Les graphiques et indicateurs du tableau de bord lisent cette feuille.')]))
    lignes.push(ligneXml(4, [celluleTexte('A4', 47, 'Paramètres')]))
    lignes.push(ligneXml(5, [celluleTexte('A5', 48, 'Période'), celluleTexte('B5', 49, ouTiret(donnees.periodeLibelle))]))
    lignes.push(ligneXml(6, [celluleTexte('A6', 48, 'Du'), celluleNombre('B6', 50, dateSerie(donnees.periode?.debut ?? maintenant))]))
    lignes.push(ligneXml(7, [celluleTexte('A7', 48, 'Au'), celluleNombre('B7', 50, dateSerie(donnees.periode?.fin ?? maintenant))]))
    lignes.push(ligneXml(8, [celluleTexte('A8', 48, 'Périmètre'), celluleTexte('B8', 49, ouTiret(donnees.perimetreLibelle))]))
    lignes.push(ligneXml(9, [celluleTexte('A9', 48, 'Date de génération (référence des délais)'), celluleNombre('B9', 50, dateSerie(maintenant))]))

    lignes.push(ligneXml(11, [celluleTexte('A11', 47, 'Activités par statut et par priorité')]))
    lignes.push(ligneXml(12, [
        celluleTexte('A12', 51, 'Statut'),
        celluleTexte('B12', 52, 'Haute'),
        celluleTexte('C12', 52, 'Normale'),
        celluleTexte('D12', 52, 'Faible'),
        celluleTexte('E12', 52, 'Nombre'),
        celluleTexte('F12', 52, 'Part'),
    ], 30))

    STATUTS_MODELE.forEach((statut, index) => {
        const numero = 13 + index
        const valeurs = contexte.matrice[index] ?? [0, 0, 0]
        const total = valeurs.reduce((somme, valeur) => somme + valeur, 0)
        const part = contexte.sommeStatuts ? total / contexte.sommeStatuts : 0
        lignes.push(ligneXml(numero, [
            celluleTexte(`A${numero}`, 48, statut.libelle),
            celluleNombre(`B${numero}`, 53, valeurs[0] ?? 0),
            celluleNombre(`C${numero}`, 53, valeurs[1] ?? 0),
            celluleNombre(`D${numero}`, 53, valeurs[2] ?? 0),
            celluleFormule(`E${numero}`, 54, `SUM(B${numero}:D${numero})`, total),
            celluleFormule(`F${numero}`, 55, `IF($E$20=0,0,E${numero}/$E$20)`, part),
        ]))
    })

    lignes.push(ligneXml(20, [
        celluleTexte('A20', 56, 'Total'),
        celluleFormule('B20', 57, 'SUM(B13:B19)', sommeColonne(contexte.matrice, 0)),
        celluleFormule('C20', 57, 'SUM(C13:C19)', sommeColonne(contexte.matrice, 1)),
        celluleFormule('D20', 57, 'SUM(D13:D19)', sommeColonne(contexte.matrice, 2)),
        celluleFormule('E20', 57, 'SUM(E13:E19)', contexte.sommeStatuts),
        celluleFormule('F20', 58, 'SUM(F13:F19)', contexte.sommeStatuts ? 1 : 0),
    ]))

    const sectionServices = 22
    lignes.push(ligneXml(sectionServices, [celluleTexte(`A${sectionServices}`, 47, 'Activités par service')]))
    lignes.push(ligneXml(sectionServices + 1, [
        celluleTexte(`A${sectionServices + 1}`, 51, 'Service'),
        celluleTexte(`B${sectionServices + 1}`, 52, 'Total activités'),
        celluleTexte(`C${sectionServices + 1}`, 52, 'En cours'),
        celluleTexte(`D${sectionServices + 1}`, 52, 'Terminées'),
        celluleTexte(`E${sectionServices + 1}`, 52, 'En retard'),
        celluleTexte(`F${sectionServices + 1}`, 52, 'Non commencées'),
        celluleTexte(`G${sectionServices + 1}`, 52, 'Avancement'),
    ], 30))

    const services = contexte.services.length
        ? contexte.services
        : [{
            idService: 0,
            service: 'Aucune donnée de service sur la période.',
            totalActivites: 0,
            terminees: 0,
            enCours: 0,
            enRetard: 0,
            nonCommencees: 0,
            avancement: 0,
        }]
    services.forEach((service, index) => {
        const numero = contexte.serviceDebut + index
        lignes.push(ligneXml(numero, [
            celluleTexte(`A${numero}`, 48, ouTiret(service.service)),
            celluleNombre(`B${numero}`, 53, service.totalActivites),
            celluleNombre(`C${numero}`, 53, service.enCours),
            celluleNombre(`D${numero}`, 53, service.terminees),
            celluleNombre(`E${numero}`, 53, service.enRetard),
            celluleNombre(`F${numero}`, 53, service.nonCommencees),
            celluleNombre(`G${numero}`, 59, fraction(service.avancement)),
        ]))
    })

    const sectionTypes = contexte.typeDebut - 2
    lignes.push(ligneXml(sectionTypes, [celluleTexte(`A${sectionTypes}`, 47, 'Activités par type')]))
    lignes.push(ligneXml(sectionTypes + 1, [
        celluleTexte(`A${sectionTypes + 1}`, 51, 'Type d’activité'),
        celluleTexte(`B${sectionTypes + 1}`, 52, 'Total activités'),
        celluleTexte(`C${sectionTypes + 1}`, 52, 'Avancement'),
    ], 30))

    const types = contexte.types.length
        ? contexte.types
        : [{ idTypeActivite: 0, typeActivite: 'Aucune donnée de type d’activité sur la période.', totalActivites: 0, avancement: 0 }]
    types.forEach((type, index) => {
        const numero = contexte.typeDebut + index
        lignes.push(ligneXml(numero, [
            celluleTexte(`A${numero}`, 48, ouTiret(type.typeActivite)),
            celluleNombre(`B${numero}`, 53, type.totalActivites),
            celluleNombre(`C${numero}`, 60, fraction(type.avancement)),
        ]))
    })

    const sectionPta = contexte.ptaDebut - 2
    lignes.push(ligneXml(sectionPta, [celluleTexte(`A${sectionPta}`, 47, 'PTA / activité courante')]))
    lignes.push(ligneXml(sectionPta + 1, [
        celluleTexte(`A${sectionPta + 1}`, 51, 'Catégorie'),
        celluleTexte(`B${sectionPta + 1}`, 52, 'Total activités'),
        celluleTexte(`C${sectionPta + 1}`, 52, 'Terminées'),
        celluleTexte(`D${sectionPta + 1}`, 52, 'En cours'),
        celluleTexte(`E${sectionPta + 1}`, 52, 'En retard'),
        celluleTexte(`F${sectionPta + 1}`, 52, 'Non commencées'),
        celluleTexte(`G${sectionPta + 1}`, 52, 'Avancement'),
    ], 30))

    const sectionsPta: Array<[string, DashboardPtaComparaison['pta'] | null]> = [
        ['PTA', donnees.comparaisonPta?.pta ?? null],
        ['COURANTE', donnees.comparaisonPta?.nonPta ?? null],
    ]
    sectionsPta.forEach(([libelle, section], index) => {
        const numero = contexte.ptaDebut + index
        lignes.push(ligneXml(numero, [
            celluleTexte(`A${numero}`, 48, libelle),
            celluleNombre(`B${numero}`, 53, section?.total ?? 0),
            celluleNombre(`C${numero}`, 53, section?.terminees ?? 0),
            celluleNombre(`D${numero}`, 53, section?.enCours ?? 0),
            celluleNombre(`E${numero}`, 53, section?.enRetard ?? 0),
            celluleNombre(`F${numero}`, 53, section?.nonCommencees ?? 0),
            celluleNombre(`G${numero}`, 59, fraction(section?.avancement)),
        ]))
    })

    const sectionOrigines = contexte.origineDebut - 2
    lignes.push(ligneXml(sectionOrigines, [celluleTexte(`A${sectionOrigines}`, 47, 'Plans d’action par origine')]))
    lignes.push(ligneXml(sectionOrigines + 1, [
        celluleTexte(`A${sectionOrigines + 1}`, 51, 'Origine'),
        celluleTexte(`B${sectionOrigines + 1}`, 52, 'Total plans'),
        celluleTexte(`C${sectionOrigines + 1}`, 52, 'Avancement'),
    ], 30))

    const origines = contexte.origines.length
        ? contexte.origines
        : [{ origine: 'AUCUNE', libelle: 'Aucune donnée d’origine sur la période.', total: 0, avancement: 0 }]
    origines.forEach((origine, index) => {
        const numero = contexte.origineDebut + index
        lignes.push(ligneXml(numero, [
            celluleTexte(`A${numero}`, 48, ouTiret(origine.libelle)),
            celluleNombre(`B${numero}`, 53, origine.total),
            celluleNombre(`C${numero}`, 59, fraction(origine.avancement)),
        ]))
    })

    const sectionIndicateurs = contexte.indicateurDebut - 2
    lignes.push(ligneXml(sectionIndicateurs, [celluleTexte(`A${sectionIndicateurs}`, 47, 'Indicateurs clés')]))
    lignes.push(ligneXml(sectionIndicateurs + 1, [
        celluleTexte(`A${sectionIndicateurs + 1}`, 51, 'Indicateur'),
        celluleTexte(`B${sectionIndicateurs + 1}`, 52, 'Valeur'),
    ], 30))

    const libellesIndicateurs = [
        'Activités suivies',
        'En cours',
        'Terminées',
        'En retard (échéance dépassée)',
        'Reportées',
        'Non commencées',
        'Suspendues',
        'Annulées',
        'En attente de validation',
        'Avancement moyen des activités',
        'Total sous-activités',
        'Sous-activités terminées',
        'Avancement sous-activités',
        'Plans d’action : total',
        'Plans non commencés',
        'Plans en cours',
        'Plans en retard',
        'Plans terminés',
        'Avancement moyen des plans',
    ]
    const valeursIndicateurs = [
        contexte.indicateurs.total,
        contexte.indicateurs.enCours,
        contexte.indicateurs.terminees,
        contexte.indicateurs.enRetard,
        contexte.indicateurs.reportees,
        contexte.indicateurs.nonCommencees,
        contexte.indicateurs.suspendues,
        contexte.indicateurs.annulees,
        contexte.indicateurs.attente,
        contexte.indicateurs.avancement,
        contexte.indicateurs.sousTotal,
        contexte.indicateurs.sousTerminees,
        contexte.indicateurs.sousAvancement,
        contexte.indicateurs.planTotal,
        contexte.indicateurs.planNonCommencees,
        contexte.indicateurs.planEnCours,
        contexte.indicateurs.planEnRetard,
        contexte.indicateurs.planTerminees,
        contexte.indicateurs.planAvancement,
    ]
    const stylesIndicateurs = [54, 54, 54, 54, 54, 54, 54, 54, 54, 61, 53, 53, 61, 54, 53, 53, 54, 53, 61]
    libellesIndicateurs.forEach((libelle, index) => {
        const numero = contexte.indicateurDebut + index
        const indicateur: ValeurIndicateur = valeursIndicateurs[index] ?? { formule: undefined, cache: 0 }
        const celluleB = indicateur.formule
            ? celluleFormule(`B${numero}`, stylesIndicateurs[index] ?? 54, indicateur.formule, indicateur.cache)
            : celluleNombre(`B${numero}`, stylesIndicateurs[index] ?? 54, indicateur.cache)
        lignes.push(ligneXml(numero, [celluleTexte(`A${numero}`, 48, libelle), celluleB]))
    })

    lignes.push(ligneXml(contexte.noteLigne, [celluleTexte(`A${contexte.noteLigne}`, 46, 'Note : « En retard » recoupe les statuts (une activité en retard est aussi en cours, non commencée, etc.), la somme des cartes n’égale donc pas le total.')]))

    return lignes.join('')
}

function activitesPourExport(donnees: DonneesExportDashboard): Array<ActiviteExport | null> {
    const resultat: Array<ActiviteExport | null> = []
    const enRetard = donnees.echeances?.enRetard ?? []
    for (let index = 0; index < Math.max(5, enRetard.length); index++) {
        const activite = enRetard[index]
        resultat.push(activite ? { ...activite, situation: 'En retard' } : null)
    }
    resultat.push(...(donnees.echeances?.echeanceProche ?? []).map(activite => ({
        ...activite,
        situation: 'Échéance prochaine',
    })))
    return resultat
}

function plansPourExport(donnees: DonneesExportDashboard): Array<PlanEcheance | null> {
    const resultat: Array<PlanEcheance | null> = []
    const enRetard: PlanEcheance[] = (donnees.suiviPlans?.enRetard ?? []).map(plan => ({
        situation: 'En retard',
        code: plan.code,
        designation: plan.designation,
        responsableLibelle: plan.responsableLibelle,
        dateFinPrevue: plan.dateFinPrevue,
        jours: -plan.joursDeRetard,
        avancement: plan.avancement,
    }))
    for (let index = 0; index < Math.max(5, enRetard.length); index++) {
        resultat.push(enRetard[index] ?? null)
    }
    resultat.push(...(donnees.suiviPlans?.echeancesProchaines ?? []).map(plan => ({
        situation: 'Échéance prochaine',
        code: plan.code,
        designation: plan.designation,
        responsableLibelle: plan.responsableLibelle,
        dateFinPrevue: plan.dateFinPrevue,
        jours: plan.joursRestants,
        avancement: plan.avancement,
    })))
    return resultat
}

function genererSheetDataActivites(donnees: DonneesExportDashboard): string {
    const lignes: string[] = []
    lignes.push(ligneXml(1, [celluleTexte('A1', 62, 'Activités en retard et échéances proches')], 32))
    lignes.push(ligneXml(2, [
        celluleTexte('A2', 63, 'Situation'),
        celluleTexte('B2', 63, 'Référence'),
        celluleTexte('C2', 63, 'Activité'),
        celluleTexte('D2', 63, 'Service'),
        celluleTexte('E2', 63, 'Échéance'),
        celluleTexte('F2', 63, 'Délai'),
        celluleTexte('G2', 63, 'Jours restants'),
        celluleTexte('H2', 63, 'Priorité'),
        celluleTexte('I2', 63, 'Statut'),
        celluleTexte('J2', 63, 'Avancement'),
        celluleTexte('L2', 46, 'Jours restants = échéance − date de génération (négatif = en retard).'),
    ], 28))

    activitesPourExport(donnees).forEach((activite, index) => {
        const numero = 3 + index
        const styleTexte = index % 2 === 0 ? 64 : 69
        const styleLong = index % 2 === 0 ? 65 : 70
        const styleDate = index % 2 === 0 ? 66 : 71
        const styleJours = index % 2 === 0 ? 67 : 72
        const styleAvancement = index % 2 === 0 ? 68 : 73
        const joursRestants = activite ? -activite.jours : 0
        lignes.push(ligneXml(numero, [
            celluleTexte(`A${numero}`, styleTexte, activite?.situation ?? ''),
            celluleTexte(`B${numero}`, styleTexte, activite ? ouTiret(activite.code) : ''),
            celluleTexte(`C${numero}`, styleLong, activite ? ouTiret(activite.designation) : ''),
            celluleTexte(`D${numero}`, styleLong, activite ? ouTiret(activite.service) : ''),
            activite
                ? celluleNombre(`E${numero}`, styleDate, dateSerie(activite.dateEcheance))
                : celluleTexte(`E${numero}`, styleDate, ''),
            celluleTexte(`F${numero}`, styleTexte, activite ? libelleJours(activite.jours) : ''),
            activite
                ? celluleFormule(`G${numero}`, styleJours, `E${numero}-Données!$B$9`, joursRestants)
                : celluleTexte(`G${numero}`, styleJours, ''),
            celluleTexte(`H${numero}`, styleTexte, activite ? ouTiret(activite.prioriteCode ?? activite.priorite) : ''),
            celluleTexte(`I${numero}`, styleTexte, activite ? ouTiret(activite.statutLibelle ?? activite.statut) : ''),
            activite
                ? celluleNombre(`J${numero}`, styleAvancement, fraction(activite.avancement))
                : celluleTexte(`J${numero}`, styleAvancement, ''),
        ]))
    })

    return lignes.join('')
}

function genererSheetDataPlans(donnees: DonneesExportDashboard): string {
    const lignes: string[] = []
    lignes.push(ligneXml(1, [celluleTexte('A1', 62, 'Plans d’action en retard et échéances proches')], 32))
    lignes.push(ligneXml(2, [
        celluleTexte('A2', 63, 'Situation'),
        celluleTexte('B2', 63, 'Code'),
        celluleTexte('C2', 63, 'Désignation'),
        celluleTexte('D2', 63, 'Responsable'),
        celluleTexte('E2', 63, 'Échéance'),
        celluleTexte('F2', 63, 'Jours restants'),
        celluleTexte('G2', 63, 'Délai'),
        celluleTexte('H2', 63, 'Avancement'),
        celluleTexte('J2', 46, 'Jours restants = échéance − date de génération (négatif = en retard).'),
    ], 28))

    plansPourExport(donnees).forEach((plan, index) => {
        const numero = 3 + index
        const styleTexte = index % 2 === 0 ? 64 : 69
        const styleLong = index % 2 === 0 ? 65 : 70
        const styleDate = index % 2 === 0 ? 66 : 71
        const styleJours = index % 2 === 0 ? 67 : 72
        const styleAvancement = index % 2 === 0 ? 68 : 73
        lignes.push(ligneXml(numero, [
            celluleTexte(`A${numero}`, styleTexte, plan?.situation ?? ''),
            celluleTexte(`B${numero}`, styleTexte, plan ? ouTiret(plan.code) : ''),
            celluleTexte(`C${numero}`, styleLong, plan ? ouTiret(plan.designation) : ''),
            celluleTexte(`D${numero}`, styleLong, plan ? ouTiret(plan.responsableLibelle) : ''),
            plan
                ? celluleNombre(`E${numero}`, styleDate, dateSerie(plan.dateFinPrevue))
                : celluleTexte(`E${numero}`, styleDate, ''),
            plan
                ? celluleFormule(`F${numero}`, styleJours, `E${numero}-Données!$B$9`, plan.jours)
                : celluleTexte(`F${numero}`, styleJours, ''),
            celluleTexte(`G${numero}`, styleTexte, plan ? libelleJours(plan.jours) : ''),
            plan
                ? celluleNombre(`H${numero}`, styleAvancement, fraction(plan.avancement))
                : celluleTexte(`H${numero}`, styleAvancement, ''),
        ]))
    })

    return lignes.join('')
}

function patcherTableauDeBord(xml: string, donnees: DonneesExportDashboard, contexte: ContexteExport): string {
    const maintenant = new Date()
    const indicateurs = contexte.indicateurs
    const ind = (offset: number) => contexte.indicateurDebut + offset
    let contenu = xml

    contenu = patcherCelluleFormule(contenu, 'B4', 'Données!$B$5', ouTiret(donnees.periodeLibelle), true)
    contenu = patcherCelluleFormule(contenu, 'D4', 'Données!$B$6', dateSerie(donnees.periode?.debut ?? maintenant))
    contenu = patcherCelluleFormule(contenu, 'F4', 'Données!$B$7', dateSerie(donnees.periode?.fin ?? maintenant))
    contenu = patcherCelluleFormule(contenu, 'H4', 'Données!$B$8', ouTiret(donnees.perimetreLibelle), true)
    contenu = patcherCelluleFormule(contenu, 'K4', 'Données!$B$9', dateSerie(maintenant))

    const cartesActivites: Array<[string, ValeurIndicateur, number]> = [
        ['B8', indicateurs.total, 0],
        ['D8', indicateurs.enCours, 1],
        ['F8', indicateurs.terminees, 2],
        ['H8', indicateurs.enRetard, 3],
        ['J8', indicateurs.nonCommencees, 5],
        ['L8', indicateurs.avancement, 9],
    ]
    for (const [reference, indicateur, offset] of cartesActivites) {
        contenu = patcherCelluleFormule(contenu, reference, `Données!$B$${ind(offset)}`, indicateur.cache)
    }

    const total = indicateurs.total.cache || 1
    const secondary = [
        ['B9', `Données!$B$${ind(10)}`, indicateurs.sousTotal.cache],
        ['D9', `Données!$B$${ind(1)}/Données!$B$${ind(0)}`, indicateurs.enCours.cache / total],
        ['F9', `Données!$B$${ind(2)}/Données!$B$${ind(0)}`, indicateurs.terminees.cache / total],
        ['H9', `Données!$B$${ind(3)}/Données!$B$${ind(0)}`, indicateurs.enRetard.cache / total],
        ['J9', `Données!$B$${ind(5)}/Données!$B$${ind(0)}`, indicateurs.nonCommencees.cache / total],
        ['L9', `Données!$B$${ind(11)}`, indicateurs.sousTerminees.cache],
    ] as const
    for (const [reference, formule, cache] of secondary) {
        contenu = patcherCelluleFormule(contenu, reference, formule, cache)
    }

    const cartesPlans: Array<[string, ValeurIndicateur, number]> = [
        ['B13', indicateurs.planTotal, 13],
        ['D13', indicateurs.planNonCommencees, 14],
        ['F13', indicateurs.planEnCours, 15],
        ['H13', indicateurs.planEnRetard, 16],
        ['J13', indicateurs.planTerminees, 17],
        ['L13', indicateurs.planAvancement, 18],
    ]
    for (const [reference, indicateur, offset] of cartesPlans) {
        contenu = patcherCelluleFormule(contenu, reference, `Données!$B$${ind(offset)}`, indicateur.cache)
    }

    const enRetardActivites = donnees.echeances?.enRetard ?? []
    for (let index = 0; index < 5; index++) {
        const ligne = 69 + index
        const activite = enRetardActivites[index]
        const champs: Array<[string, string, string | number, boolean]> = activite
            ? [
                [`B${ligne}`, `Activités!B${3 + index}`, ouTiret(activite.code), true],
                [`C${ligne}`, `Activités!C${3 + index}`, ouTiret(activite.designation), true],
                [`G${ligne}`, `Activités!D${3 + index}`, ouTiret(activite.service), true],
                [`J${ligne}`, `Activités!E${3 + index}`, dateSerie(activite.dateEcheance), false],
                [`K${ligne}`, `Activités!G${3 + index}`, -activite.jours, false],
                [`L${ligne}`, `Activités!H${3 + index}`, ouTiret(activite.prioriteCode ?? activite.priorite), true],
                [`M${ligne}`, `Activités!J${3 + index}`, fraction(activite.avancement), false],
            ]
            : [
                [`B${ligne}`, '""', '', true],
                [`C${ligne}`, '""', '', true],
                [`G${ligne}`, '""', '', true],
                [`J${ligne}`, '""', '', true],
                [`K${ligne}`, '""', '', true],
                [`L${ligne}`, '""', '', true],
                [`M${ligne}`, '""', '', true],
            ]
        for (const [reference, formule, cache, cacheTexte] of champs) {
            contenu = patcherCelluleFormule(contenu, reference, formule, cache, cacheTexte)
        }
    }

    const enRetardPlans = donnees.suiviPlans?.enRetard ?? []
    for (let index = 0; index < 5; index++) {
        const ligne = 77 + index
        const plan = enRetardPlans[index]
        const champs: Array<[string, string, string | number, boolean]> = plan
            ? [
                [`B${ligne}`, `Plans!B${3 + index}`, ouTiret(plan.code), true],
                [`C${ligne}`, `Plans!C${3 + index}`, ouTiret(plan.designation), true],
                [`G${ligne}`, `Plans!D${3 + index}`, ouTiret(plan.responsableLibelle), true],
                [`J${ligne}`, `Plans!E${3 + index}`, dateSerie(plan.dateFinPrevue), false],
                [`K${ligne}`, `Plans!F${3 + index}`, -plan.joursDeRetard, false],
                [`L${ligne}`, `Plans!H${3 + index}`, fraction(plan.avancement), false],
            ]
            : [
                [`B${ligne}`, '""', '', true],
                [`C${ligne}`, '""', '', true],
                [`G${ligne}`, '""', '', true],
                [`J${ligne}`, '""', '', true],
                [`K${ligne}`, '""', '', true],
                [`L${ligne}`, '""', '', true],
            ]
        for (const [reference, formule, cache, cacheTexte] of champs) {
            contenu = patcherCelluleFormule(contenu, reference, formule, cache, cacheTexte)
        }
    }

    return contenu
}

function patcherClasseur(fichiers: Record<string, Uint8Array>, donnees: DonneesExportDashboard): void {
    const contexte = preparerContexte(donnees)

    fichiers['xl/worksheets/sheet1.xml'] = strToU8(patcherTableauDeBord(
        strFromU8(fichierClasseur(fichiers, 'xl/worksheets/sheet1.xml')),
        donnees,
        contexte,
    ))

    let feuilleDonnees = strFromU8(fichierClasseur(fichiers, 'xl/worksheets/sheet2.xml'))
    feuilleDonnees = remplacerSheetData(feuilleDonnees, genererSheetDataDonnees(donnees, contexte))
    feuilleDonnees = remplacerDimension(feuilleDonnees, `A1:G${contexte.noteLigne}`)
    fichiers['xl/worksheets/sheet2.xml'] = strToU8(feuilleDonnees)

    let feuilleActivites = strFromU8(fichierClasseur(fichiers, 'xl/worksheets/sheet3.xml'))
    feuilleActivites = remplacerSheetData(feuilleActivites, genererSheetDataActivites(donnees))
    feuilleActivites = remplacerDimension(feuilleActivites, `A1:L${contexte.derniereActivite}`)
    feuilleActivites = remplacerAutoFilter(feuilleActivites, `A2:J${contexte.derniereActivite}`)
    for (const colonne of ['A', 'H', 'J']) {
        feuilleActivites = remplacerSqref(feuilleActivites, colonne, contexte.derniereActivite)
    }
    fichiers['xl/worksheets/sheet3.xml'] = strToU8(feuilleActivites)

    let feuillePlans = strFromU8(fichierClasseur(fichiers, 'xl/worksheets/sheet4.xml'))
    feuillePlans = remplacerSheetData(feuillePlans, genererSheetDataPlans(donnees))
    feuillePlans = remplacerDimension(feuillePlans, `A1:J${contexte.dernierePlan}`)
    feuillePlans = remplacerAutoFilter(feuillePlans, `A2:H${contexte.dernierePlan}`)
    for (const colonne of ['A', 'H']) {
        feuillePlans = remplacerSqref(feuillePlans, colonne, contexte.dernierePlan)
    }
    fichiers['xl/worksheets/sheet4.xml'] = strToU8(feuillePlans)

    let classeur = strFromU8(fichierClasseur(fichiers, 'xl/workbook.xml'))
    classeur = classeur.replace(
        /('Activités'!\$A\$2:\$J\$)\d+/,
        `$1${contexte.derniereActivite}`,
    )
    classeur = classeur.replace(
        /('Plans'!\$A\$2:\$H\$)\d+/,
        `$1${contexte.dernierePlan}`,
    )
    fichiers['xl/workbook.xml'] = strToU8(classeur)

    const remplacerReference = (chemin: string, ancienne: string, nouvelle: string) => {
        const contenu = strFromU8(fichierClasseur(fichiers, chemin))
        fichiers[chemin] = strToU8(contenu.split(ancienne).join(nouvelle))
    }

    remplacerReference('xl/charts/chart3.xml', `'Données'!$A$24:$A$25`, `'Données'!$A$${contexte.serviceDebut}:$A$${contexte.serviceFin}`)
    remplacerReference('xl/charts/chart3.xml', `'Données'!$G$24:$G$25`, `'Données'!$G$${contexte.serviceDebut}:$G$${contexte.serviceFin}`)
    remplacerReference('xl/charts/chart4.xml', `'Données'!$A$42:$A$43`, `'Données'!$A$${contexte.ptaDebut}:$A$${contexte.ptaFin}`)
    remplacerReference('xl/charts/chart4.xml', `'Données'!$G$42:$G$43`, `'Données'!$G$${contexte.ptaDebut}:$G$${contexte.ptaFin}`)
    remplacerReference('xl/charts/chart5.xml', `'Données'!$A$47:$A$49`, `'Données'!$A$${contexte.origineDebut}:$A$${contexte.origineFin}`)
    remplacerReference('xl/charts/chart5.xml', `'Données'!$C$47:$C$49`, `'Données'!$C$${contexte.origineDebut}:$C$${contexte.origineFin}`)
    remplacerReference('xl/charts/chart6.xml', `'Données'!$A$29:$A$38`, `'Données'!$A$${contexte.typeDebut}:$A$${contexte.typeFin}`)
    remplacerReference('xl/charts/chart6.xml', `'Données'!$C$29:$C$38`, `'Données'!$C$${contexte.typeDebut}:$C$${contexte.typeFin}`)
}

async function chargerModele(): Promise<Uint8Array> {
    const url = `${import.meta.env.BASE_URL}templates/GESTIA_Tableau_de_Bord.xlsx`
    const reponse = await fetch(url)
    if (!reponse.ok) {
        throw new Error(`Modèle GESTIA introuvable (${reponse.status}).`)
    }
    return new Uint8Array(await reponse.arrayBuffer())
}

function telecharger(archive: Uint8Array): void {
    const blob = new Blob([archive as BlobPart], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    const url = URL.createObjectURL(blob)
    const lien = document.createElement('a')
    lien.href = url
    lien.download = nomFichier()
    document.body.appendChild(lien)
    lien.click()
    lien.remove()
    URL.revokeObjectURL(url)
}

export async function exporterExcelDashboard(donnees: DonneesExportDashboard): Promise<void> {
    const modele = await chargerModele()
    const fichiers = unzipSync(modele)
    patcherClasseur(fichiers, donnees)
    telecharger(zipSync(fichiers, { level: 6 }))
}

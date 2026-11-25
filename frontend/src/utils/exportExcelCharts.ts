import { strFromU8, strToU8 } from 'fflate'

export interface SerieGraphiqueExcel {
    nom: string
    plageCategories: string
    plageValeurs: string
    categories?: Array<string | number>
    valeurs?: number[]
    couleur?: string
}

export interface GraphiqueExcel {
    titre: string
    type: 'donut' | 'bar'
    series: SerieGraphiqueExcel[]
    col: number
    row: number
    largeurCm?: number
    hauteurCm?: number
    legende?: boolean
}

const RELATIONSHIPS = 'http://schemas.openxmlformats.org/package/2006/relationships'
const REL_DRAWING = 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing'
const REL_CHART = 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/chart'

function echapperXml(valeur: string): string {
    return valeur
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
}

function cm(valeur: number): number {
    return Math.round(valeur * 360000)
}

function titreXml(titre: string): string {
    return '<c:title><c:tx><c:rich><a:bodyPr/><a:lstStyle/><a:p>'
        + '<a:pPr><a:defRPr sz="1100" b="1"><a:solidFill><a:srgbClr val="1A2A6C"/></a:solidFill></a:defRPr></a:pPr>'
        + '<a:r><a:rPr lang="fr-FR" sz="1100" b="1"><a:solidFill><a:srgbClr val="1A2A6C"/></a:solidFill></a:rPr>'
        + `<a:t>${echapperXml(titre)}</a:t></a:r></a:p></c:rich></c:tx><c:overlay val="0"/></c:title>`
}

function cacheTexte(valeurs: Array<string | number> | undefined): string {
    if (!valeurs?.length) return ''
    const points = valeurs.map((valeur, index) =>
        `<c:pt idx="${index}"><c:v>${echapperXml(String(valeur))}</c:v></c:pt>`
    ).join('')
    return `<c:strCache><c:ptCount val="${valeurs.length}"/>${points}</c:strCache>`
}

function cacheNombres(valeurs: number[] | undefined): string {
    if (!valeurs?.length) return ''
    const points = valeurs.map((valeur, index) =>
        `<c:pt idx="${index}"><c:v>${Number.isFinite(valeur) ? valeur : 0}</c:v></c:pt>`
    ).join('')
    return `<c:numCache><c:formatCode>General</c:formatCode><c:ptCount val="${valeurs.length}"/>${points}</c:numCache>`
}

function referenceTexte(plage: string, valeurs: Array<string | number> | undefined): string {
    return `<c:strRef><c:f>${echapperXml(plage)}</c:f>${cacheTexte(valeurs)}</c:strRef>`
}

function referenceNombres(plage: string, valeurs: number[] | undefined): string {
    return `<c:numRef><c:f>${echapperXml(plage)}</c:f>${cacheNombres(valeurs)}</c:numRef>`
}

function serieXml(serie: SerieGraphiqueExcel, index: number, categories?: Array<string | number>): string {
    const style = serie.couleur
        ? '<c:spPr><a:solidFill><a:srgbClr val="' + serie.couleur + '"/></a:solidFill><a:ln><a:noFill/></a:ln></c:spPr>'
        : ''
    return '<c:ser>'
        + `<c:idx val="${index}"/><c:order val="${index}"/>`
        + `<c:tx><c:v>${echapperXml(serie.nom)}</c:v></c:tx>`
        + style
        + `<c:cat>${referenceTexte(serie.plageCategories, categories ?? serie.categories)}</c:cat>`
        + `<c:val>${referenceNombres(serie.plageValeurs, serie.valeurs)}</c:val>`
        + '</c:ser>'
}

function axesXml(): string {
    return '<c:catAx>'
        + '<c:axId val="111"/><c:scaling><c:orientation val="minMax"/></c:scaling><c:delete val="0"/>'
        + '<c:axPos val="b"/><c:tickLblPos val="nextTo"/><c:crossAx val="222"/><c:crosses val="autoZero"/>'
        + '<c:auto val="1"/><c:lblAlgn val="ctr"/><c:lblOffset val="100"/>'
        + '</c:catAx>'
        + '<c:valAx>'
        + '<c:axId val="222"/><c:scaling><c:orientation val="minMax"/></c:scaling><c:delete val="0"/>'
        + '<c:axPos val="l"/><c:numFmt formatCode="General" sourceLinked="0"/>'
        + '<c:majorTickMark val="none"/><c:minorTickMark val="none"/><c:tickLblPos val="nextTo"/>'
        + '<c:crossAx val="111"/><c:crosses val="autoZero"/>'
        + '</c:valAx>'
}

function graphiqueXml(graphique: GraphiqueExcel): string {
    const categories = graphique.series[0]?.categories
    const series = graphique.series
        .map((serie, index) => serieXml(serie, index, categories))
        .join('')

    let corps: string
    if (graphique.type === 'donut') {
        const etiquettes = '<c:dLbls><c:showLegendKey val="0"/><c:showVal val="0"/>'
            + '<c:showCatName val="0"/><c:showSerName val="0"/><c:showPercent val="1"/>'
            + '<c:showBubbleSize val="0"/></c:dLbls>'
        corps = '<c:doughnutChart><c:varyColors val="1"/>' + series + etiquettes
            + '<c:firstSliceAng val="0"/><c:holeSize val="55"/></c:doughnutChart>'
    } else {
        corps = '<c:barChart><c:barDir val="col"/><c:grouping val="clustered"/><c:varyColors val="0"/>'
            + series + '<c:gapWidth val="80"/><c:overlap val="0"/>'
            + '<c:axId val="111"/><c:axId val="222"/></c:barChart>' + axesXml()
    }

    const legende = graphique.legende === false
        ? ''
        : '<c:legend><c:legendPos val="b"/><c:overlay val="0"/></c:legend>'

    return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        + '<c:chartSpace xmlns:c="http://schemas.openxmlformats.org/drawingml/2006/chart"'
        + ' xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"'
        + ' xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
        + '<c:chart>' + titreXml(graphique.titre)
        + '<c:plotArea><c:layout/>' + corps + '</c:plotArea>'
        + legende + '<c:plotVisOnly val="1"/><c:dispBlanksAs val="gap"/>'
        + '</c:chart></c:chartSpace>'
}

function dessinXml(graphiques: GraphiqueExcel[], identifiantsRelations: string[]): string {
    const ancres = graphiques.map((graphique, index) => {
        return '<xdr:oneCellAnchor>'
            + '<xdr:from>'
            + `<xdr:col>${graphique.col}</xdr:col><xdr:colOff>0</xdr:colOff>`
            + `<xdr:row>${graphique.row}</xdr:row><xdr:rowOff>0</xdr:rowOff>`
            + '</xdr:from>'
            + `<xdr:ext cx="${cm(graphique.largeurCm ?? 14)}" cy="${cm(graphique.hauteurCm ?? 8)}"/>`
            + '<xdr:graphicFrame macro="">'
            + '<xdr:nvGraphicFramePr>'
            + `<xdr:cNvPr id="${index + 2}" name="Graphique ${index + 1}"/>`
            + '<xdr:cNvGraphicFramePr/>'
            + '</xdr:nvGraphicFramePr>'
            + '<xdr:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/></xdr:xfrm>'
            + '<a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/chart">'
            + `<c:chart r:id="${identifiantsRelations[index]}"/>`
            + '</a:graphicData></a:graphic>'
            + '</xdr:graphicFrame>'
            + '<xdr:clientData/>'
            + '</xdr:oneCellAnchor>'
    }).join('')

    return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        + '<xdr:wsDr xmlns:xdr="http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing"'
        + ' xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"'
        + ' xmlns:c="http://schemas.openxmlformats.org/drawingml/2006/chart"'
        + ' xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
        + ancres + '</xdr:wsDr>'
}

function relationsVides(): string {
    return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        + `<Relationships xmlns="${RELATIONSHIPS}"></Relationships>`
}

function ajouterRelation(
    fichiers: Record<string, Uint8Array>,
    chemin: string,
    type: string,
    cible: string,
): string {
    const xml = fichiers[chemin] ? strFromU8(fichiers[chemin]) : relationsVides()
    const identifiants = [...xml.matchAll(/Id="rId(\d+)"/g)]
        .map(correspondance => Number(correspondance[1]))
    const prochain = identifiants.length ? Math.max(...identifiants) + 1 : 1
    const identifiant = `rId${prochain}`
    const misAJour = xml.replace(
        '</Relationships>',
        `<Relationship Id="${identifiant}" Type="${type}" Target="${cible}"/></Relationships>`,
    )
    fichiers[chemin] = strToU8(misAJour)
    return identifiant
}

function ajouterTypes(fichiers: Record<string, Uint8Array>, graphiques: GraphiqueExcel[]): void {
    const chemin = '[Content_Types].xml'
    const contenu = fichiers[chemin]
    if (!contenu) return
    const xml = strFromU8(contenu)
    let misAJour = xml
    const overrides: string[] = []

    for (let index = 0; index < graphiques.length; index++) {
        overrides.push(`<Override PartName="/xl/charts/chart${index + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.drawingml.chart+xml"/>`)
    }
    overrides.push('<Override PartName="/xl/drawings/drawing1.xml" ContentType="application/vnd.openxmlformats-officedocument.drawing+xml"/>')

    for (const override of overrides) {
        if (!misAJour.includes(override)) {
            misAJour = misAJour.replace('</Types>', override + '</Types>')
        }
    }

    fichiers[chemin] = strToU8(misAJour)
}

export function injecterGraphiques(
    fichiers: Record<string, Uint8Array>,
    graphiques: GraphiqueExcel[],
): void {
    if (graphiques.length === 0) return

    const relationsDessin: string[] = []
    graphiques.forEach((graphique, index) => {
        const cheminChart = `xl/charts/chart${index + 1}.xml`
        fichiers[cheminChart] = strToU8(graphiqueXml(graphique))
        const identifiant = ajouterRelation(
            fichiers,
            'xl/drawings/_rels/drawing1.xml.rels',
            REL_CHART,
            `../charts/chart${index + 1}.xml`,
        )
        relationsDessin.push(identifiant)
    })

    fichiers['xl/drawings/drawing1.xml'] = strToU8(dessinXml(graphiques, relationsDessin))

    const identifiantDessin = ajouterRelation(
        fichiers,
        'xl/worksheets/_rels/sheet1.xml.rels',
        REL_DRAWING,
        '../drawings/drawing1.xml',
    )

    const cheminFeuille = 'xl/worksheets/sheet1.xml'
    const contenuFeuille = fichiers[cheminFeuille]
    if (!contenuFeuille) return
    let xmlFeuille = strFromU8(contenuFeuille)
    if (!xmlFeuille.includes('<drawing ')) {
        xmlFeuille = xmlFeuille.replace(
            '</worksheet>',
            `<drawing r:id="${identifiantDessin}"/></worksheet>`,
        )
    }
    fichiers[cheminFeuille] = strToU8(xmlFeuille)

    ajouterTypes(fichiers, graphiques)
}

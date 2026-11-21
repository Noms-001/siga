import { describe, expect, it } from 'vitest'
import { normalizeImportDate } from './importDate'

describe('normalizeImportDate', () => {
    it.each([
        ['1/15/27', '2027-01-15'],
        ['1/2/27', '2027-01-02'],
        ['01/15/2027', '2027-01-15'],
        ['15/1/2027', '2027-01-15'],
        ['2027-01-15', '2027-01-15'],
        ['2027-1-5', '2027-01-05'],
        ['12/31/69', '2069-12-31'],
        ['12/31/70', '1970-12-31'],
    ])('converts %s to ISO %s', (input, expected) => {
        expect(normalizeImportDate(input)).toBe(expected)
    })

    it('normalizes a JavaScript Date using calendar components rather than UTC serialization', () => {
        const localCalendarDate = new Date(2027, 0, 15, 0, 30)
        expect(normalizeImportDate(localCalendarDate)).toBe('2027-01-15')
    })

    it('converts an Excel serial date to ISO without a timezone shift', () => {
        const excelEpoch = Date.UTC(1899, 11, 30)
        const serial = (Date.UTC(2027, 0, 15) - excelEpoch) / 86_400_000
        expect(normalizeImportDate(serial)).toBe('2027-01-15')
    })

    it.each([
        '2/30/27',
        '13/15/27',
        '2027-02-29',
        '1/2/2027',
        'not a date',
    ])('rejects invalid or ambiguous date %s', (input) => {
        expect(() => normalizeImportDate(input)).toThrow(/date|limites|ambigu/)
    })

    it('serializes planned, actual, and sub-activity dates in the final API shape as ISO strings', () => {
        const payload = {
            activites: [{
                dateDebut: normalizeImportDate('1/2/27'),
                dateFin: normalizeImportDate('12/31/27'),
                dateDebutReelle: normalizeImportDate('2027-01-03'),
                dateFinReelle: normalizeImportDate('2027-12-30'),
                sousActivites: [{
                    dateDebut: normalizeImportDate('1/15/27'),
                    dateFin: normalizeImportDate('2027-01-31'),
                    dateDebutReelle: normalizeImportDate('1/16/27'),
                    dateFinReelle: normalizeImportDate('1/30/27'),
                }],
            }],
        }
        const json = JSON.stringify(payload)
        expect(json).not.toMatch(/"date(?:Debut|Fin)(?:Reelle)?":"\d{1,2}\/\d{1,2}\/\d{2,4}"/)
        expect(json).toContain('"dateDebut":"2027-01-02"')
        expect(json).toContain('"dateFin":"2027-12-31"')
    })
})
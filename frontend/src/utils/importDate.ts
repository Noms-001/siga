/** Normalize spreadsheet date values for the API's java.time.LocalDate fields. */
export function normalizeImportDate(value: unknown): string {
    if (value instanceof Date) {
        if (Number.isNaN(value.getTime())) {
            throw new Error('date invalide');
        }

        // Read local calendar components: toISOString() can move a local
        // midnight to the previous/next UTC day.
        return formatDate(value.getFullYear(), value.getMonth() + 1, value.getDate());
    }

    if (typeof value === 'number' && Number.isFinite(value)) {
        // Excel 1900 date system; serial fractions represent time and are
        // intentionally discarded because the API field is a LocalDate.
        const epoch = Date.UTC(1899, 11, 30);
        const date = new Date(epoch + Math.floor(value) * 86_400_000);
        return formatDate(date.getUTCFullYear(), date.getUTCMonth() + 1, date.getUTCDate());
    }

    if (typeof value !== 'string') {
        throw new Error('format de date non reconnu');
    }

    const input = value.trim();
    const iso = input.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/);
    if (iso) {
        return formatDate(Number(iso[1]), Number(iso[2]), Number(iso[3]));
    }

    const slashDate = input.match(/^(\d{1,2})[/-](\d{1,2})[/-](\d{2}|\d{4})$/);
    if (!slashDate) {
        throw new Error(`« ${input || '(vide)'} » n'est pas une date reconnue (attendu : yyyy-MM-dd ou M/D/YY).`);
    }

    const first = Number(slashDate[1]);
    const second = Number(slashDate[2]);
    const yearText = slashDate[3]!;
    const year = yearText.length === 2 ? expandTwoDigitYear(Number(yearText)) : Number(yearText);

    // Two-digit year values in these files follow the Excel US format M/D/YY.
    if (yearText.length === 2) {
        return formatDate(year, first, second);
    }

    // With four-digit years accept only unambiguous D/M or M/D values.
    if (first <= 12 && second <= 12) {
        throw new Error(`« ${input} » est ambiguë; utilisez le format yyyy-MM-dd.`);
    }
    return first > 12
        ? formatDate(year, second, first)
        : formatDate(year, first, second);
}

function expandTwoDigitYear(year: number): number {
    // Pivot used for common two-digit Excel years: 00–69 => 2000s, 70–99 => 1900s.
    return year <= 69 ? 2000 + year : 1900 + year;
}

function formatDate(year: number, month: number, day: number): string {
    const date = new Date(Date.UTC(year, month - 1, day));
    if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) {
        throw new Error('jour, mois ou année hors limites');
    }
    return `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

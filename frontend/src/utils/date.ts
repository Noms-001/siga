/**
 * Formate une date en français.
 *
 * Exemple :
 * 2026-08-31T12:14:20
 * → 31 août 2026 à 12:14
 */
export const formatDate = (
    date: string | null | undefined
): string => {

    if (!date) {
        return 'Jamais connecté'
    }

    const parsedDate = new Date(date)

    if (isNaN(parsedDate.getTime())) {
        return 'Date inconnue'
    }

    return new Intl.DateTimeFormat('fr-FR', {
        day: 'numeric',
        month: 'long',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    }).format(parsedDate)
}

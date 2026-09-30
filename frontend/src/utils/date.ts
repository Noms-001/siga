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

/**
 * Formate une date seule, sans heure.
 *
 * Distinct de formatDate, qui est prevu pour un horodatage ( derniere
 * connexion, changement de statut). Utiliser formatDate sur une date de
 * planning afficherait "00:00" et renverrait "Jamais connecté" pour une
 * date absente, ce qui n'a aucun sens pour une periode d'activite.
 */
export const formatDateSeule = (
    date: string | null | undefined,
    defaut = '—'
): string => {

    if (!date) {
        return defaut
    }

    const parsedDate = new Date(date)

    if (isNaN(parsedDate.getTime())) {
        return defaut
    }

    return new Intl.DateTimeFormat('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    }).format(parsedDate)
}

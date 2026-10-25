package mg.bank.backend.utils;

import mg.bank.backend.model.utilisateur.Utilisateur;

public final class TextUtils {

    private TextUtils() {
    }

    /**
     * Retourne la chaine trimee, ou null si elle est vide ou blank.
     * Aucun appelant n'attend une chaine vide : null vaut "pas de filtre".
     */
    public static String videSiBlanc(String valeur) {
        return (valeur == null || valeur.isBlank()) ? null : valeur.trim();
    }

    /**
     * Libelle usuel d'un utilisateur : "prenom nom".
     * Null si l'utilisateur est null.
     */
    public static String formatNom(Utilisateur u) {
        return u == null ? null : u.getPrenom() + " " + u.getNom();
    }
}
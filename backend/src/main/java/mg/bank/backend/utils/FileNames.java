package mg.bank.backend.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class FileNames {

    private FileNames() {
    }

    /** Longueur de la colonne nom_original en base (VARCHAR(255)). */
    public static final int LONGUEUR_MAX_NOM_ORIGINAL = 255;

    /** Longueur de la colonne extension en base (VARCHAR(20)). */
    public static final int LONGUEUR_MAX_EXTENSION = 20;

    /** Nom par defaut quand aucun nom d'origine n'est fourni. */
    public static final String NOM_PAR_DEFAUT = "fichier";

    public static String dispositionInline(String nom) {
        String assaini = nom
                .replace("\"", "_")
                .replace("\r", "_")
                .replace("\n", "_");

        String ascii = assaini.replaceAll("[^\\x20-\\x7E]", "_");
        String utf8 = URLEncoder.encode(assaini, StandardCharsets.UTF_8)
                .replace("+", "%20");

        return String.format(
                "inline; filename=\"%s\"; filename*=UTF-8''%s",
                ascii, utf8);
    }

    public static String nomOriginalSecurise(String nom) {
        String sain = (nom == null) ? "" : nom.trim();

        if (sain.length() > LONGUEUR_MAX_NOM_ORIGINAL) {
            sain = sain.substring(0, LONGUEUR_MAX_NOM_ORIGINAL);
        }

        return sain.isEmpty() ? NOM_PAR_DEFAUT : sain;
    }

    public static String extension(String nomFichier) {
        if (nomFichier == null) {
            return null;
        }

        int dernierPoint = nomFichier.lastIndexOf('.');

        if (dernierPoint < 0 || dernierPoint == nomFichier.length() - 1) {
            return null;
        }

        String extension = nomFichier.substring(dernierPoint + 1)
                .toLowerCase(Locale.ROOT);

        return extension.length() > LONGUEUR_MAX_EXTENSION ? null : extension;
    }

    public static String nomDeStockage(
            int idLivrable,
            int version,
            int rang,
            String extension) {

        return "livrable_" + idLivrable
                + "_v" + version
                + "_" + rang
                + (extension == null || extension.isBlank() ? "" : "." + extension);
    }
}
package mg.bank.backend.security;

/**
 * Actions disponibles dans le systeme de permissions.
 *
 * Les valeurs sont volontairement en majuscules pour rester lisibles dans
 * les annotations et correspondent a la colonne permission.action.
 */
public final class PermissionAction {

    public static final String AFFECTER = "AFFECTER";
    public static final String CREER = "CREER";
    public static final String MODIFIER = "MODIFIER";
    public static final String DESACTIVER = "DESACTIVER";
    public static final String ACTIVER = "ACTIVER";
    public static final String VALIDER = "VALIDER";
    public static final String IMPORTER = "IMPORTER";
    public static final String EXPORTER = "EXPORTER";
    public static final String CONSULTER = "CONSULTER";

    private PermissionAction() {
    }
}

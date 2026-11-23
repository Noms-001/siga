package mg.bank.backend.security;

/**
 * Portee de la requete HTTP en cours.
 *
 * Positionnee par {@link PermissionInterceptor} apres le controle de
 * permission, puis nettoyee a la fin du traitement. En dehors d'une requete
 * protegee (scheduler, test service), aucune portee n'est posee : le
 * comportement historique, fonde sur le rattachement de l'utilisateur, reste
 * alors inchange.
 */
public final class ContextePortee {

    private static final ThreadLocal<PorteeAcces> COURANT = new ThreadLocal<>();

    private ContextePortee() {
    }

    public static void poser(PorteeAcces portee) {
        COURANT.set(portee);
    }

    /** Portee courante, ou null si la requete n'est pas protegee. */
    public static PorteeAcces courant() {
        return COURANT.get();
    }

    public static void nettoyer() {
        COURANT.remove();
    }
}

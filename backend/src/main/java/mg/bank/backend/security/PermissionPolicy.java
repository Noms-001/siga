package mg.bank.backend.security;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/**
 * Regles transverses de portee qui ne dependent pas du controller.
 *
 * <p>Certaines ressources ne sont rattachees ni a un service ni a un
 * departement : postes, roles, permissions, parametres, procedures, sites,
 * priorites et statuts. Les administrer n'a de sens qu'avec une portee totale,
 * sinon un utilisateur pourrait modifier le referentiel commun depuis un
 * perimetre restreint.</p>
 */
public final class PermissionPolicy {

    private static final Set<String> RESSOURCES_GLOBALES = Set.of(
            PermissionResource.POSTE,
            PermissionResource.ROLE,
            PermissionResource.PERMISSION,
            PermissionResource.POSTE_PERMISSION,
            PermissionResource.PARAMETRE,
            PermissionResource.PROCEDURE,
            PermissionResource.SITE,
            PermissionResource.PRIORITE,
            PermissionResource.STATUT);

    private PermissionPolicy() {
    }

    /**
     * Vrai si toutes les permissions alternatives portent sur une ressource
     * globale. Un endpoint qui autorise aussi une ressource hierarchique
     * (activite, utilisateur, ...) n'est pas concerne : l'acces par la
     * ressource hierarchique reste possible avec une portee plus fine.
     */
    public static boolean exigePorteeTotale(
            Collection<PermissionRequirement> permissions) {

        return permissions != null
                && !permissions.isEmpty()
                && permissions.stream().allMatch(
                        permission -> estRessourceGlobale(permission.ressource()));
    }

    private static boolean estRessourceGlobale(String ressource) {
        return ressource != null
                && RESSOURCES_GLOBALES.contains(
                        ressource.trim().toUpperCase(Locale.ROOT));
    }
}

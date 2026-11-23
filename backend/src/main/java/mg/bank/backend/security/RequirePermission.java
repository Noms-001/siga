package mg.bank.backend.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declare une permission requise pour acceder a un endpoint.
 *
 * L'annotation est repetable : plusieurs permissions peuvent etre declarees
 * sur un meme endpoint. Elles sont alors alternatives (OU logique), ce qui
 * permet d'exprimer l'heritage des ressources :
 *
 * <pre>
 * &#64;RequirePermission(ressource = PermissionResource.SOUS_ACTIVITE,
 *         action = PermissionAction.CONSULTER)
 * &#64;RequirePermission(ressource = PermissionResource.ACTIVITE,
 *         action = PermissionAction.CONSULTER)
 * </pre>
 *
 * Ici, un utilisateur ayant le droit de consulter l'activite parente peut
 * egalement consulter ses sous-activites.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RequirePermissions.class)
public @interface RequirePermission {

    /** Ressource concernee, par exemple ACTIVITE. */
    String ressource();

    /** Action concernee, par exemple CONSULTER. */
    String action();
}

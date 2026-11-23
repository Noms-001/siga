package mg.bank.backend.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Conteneur des annotations {@link RequirePermission} repetees.
 *
 * Permet aussi la forme explicite :
 * <pre>
 * &#64;RequirePermissions({
 *     &#64;RequirePermission(ressource = "ACTIVITE", action = "CONSULTER"),
 *     &#64;RequirePermission(ressource = "SOUS_ACTIVITE", action = "CONSULTER")
 * })
 * </pre>
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermissions {

    RequirePermission[] value();
}

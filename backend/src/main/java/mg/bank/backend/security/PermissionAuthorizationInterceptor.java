package mg.bank.backend.security;

import java.util.Arrays;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.service.utilisateur.CurrentUserService;
import mg.bank.backend.service.utilisateur.PermissionService;

/** Enforces the permission catalog for every matched API endpoint. */
@Component
@RequiredArgsConstructor
public class PermissionAuthorizationInterceptor implements HandlerInterceptor {

    private final CurrentUserService currentUserService;
    private final PermissionService permissionService;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String path = request.getRequestURI();
        if (isAuthenticationOrSelfService(path)) {
            return true;
        }

        PermissionRequirement requirement = resolve(path, request.getMethod());
        if (requirement == null) {
            throw new ApiException(
                    "Aucune permission n'est configurée pour cet endpoint",
                    HttpStatus.FORBIDDEN);
        }

        Integer idPoste = currentUserService.getIdPoste();
        if (!permissionService.hasPermission(
                idPoste, requirement.resource(), requirement.action())) {
            throw new ApiException(
                    "Permission requise : " + requirement.resource() + ":" + requirement.action(),
                    HttpStatus.FORBIDDEN);
        }
        return true;
    }

    private boolean isAuthenticationOrSelfService(String path) {
        return path.startsWith("/api/auth/")
                || path.equals("/api/backoffice/auth/login")
                || path.equals("/api/utilisateur/me")
                || path.startsWith("/api/utilisateur/me/");
    }

    private PermissionRequirement resolve(String rawPath, String httpMethod) {
        String path = rawPath.toLowerCase(Locale.ROOT);
        String method = httpMethod.toUpperCase(Locale.ROOT);

        if (path.startsWith("/api/backoffice/")) {
            String rest = path.substring("/api/backoffice/".length());
            if (rest.startsWith("postes/") && rest.contains("/permissions")) {
                return backofficeAction("poste_permission", method);
            }
            String resource = switch (rest.split("/")[0]) {
                case "departements" -> "departement";
                case "services" -> "service";
                case "postes" -> "poste";
                case "roles" -> "role";
                case "types-activite" -> "type_activite";
                case "sites" -> "site";
                case "indicateurs" -> "indicateur";
                case "priorites" -> "priorite";
                case "statuts" -> "statut";
                case "procedures" -> "procedure";
                case "parametres" -> "parametre";
                case "permissions" -> "permission";
                case "utilisateurs" -> "utilisateur";
                default -> rest.split("/")[0].replace('-', '_');
            };
            return backofficeAction(resource, method);
        }
        if (path.startsWith("/api/activites/affectations")) {
            return new PermissionRequirement(
                    path.contains("/sous-activites/") ? "affectation_sous_activite" : "affectation_activite",
                    actionFor(method));
        }
        if (path.startsWith("/api/activites/import")) {
            return new PermissionRequirement("activite", "CREATE");
        }
        if (path.startsWith("/api/activites/objectifs")) {
            return new PermissionRequirement(
                    "objectif_specifique", method.equals("POST") ? "CREATE" : "READ");
        }
        if (path.startsWith("/api/activites/") && isActivityValidation(path)) {
            return new PermissionRequirement("validation_activite", "VALIDATE");
        }
        if (path.startsWith("/api/activites/") && path.contains("/sous-activites/")) {
            if (path.contains("/livrables/") || path.endsWith("/livrables")
                    || path.contains("/fichiers/")) {
                return new PermissionRequirement("livrable", actionFor(method));
            }
            return new PermissionRequirement("sous_activite", actionFor(method));
        }
        if (path.startsWith("/api/activites/") && path.contains("/indicateurs")) {
            return new PermissionRequirement("activite", "UPDATE");
        }
        if (path.equals("/api/activites") || path.startsWith("/api/activites/")) {
            if (path.endsWith("/a-valider") || path.endsWith("/a-soumettre")) {
                return new PermissionRequirement("validation_activite", "READ");
            }
            return new PermissionRequirement("activite", actionFor(method));
        }
        if (path.startsWith("/api/sous-activites/")) {
            return new PermissionRequirement("avancement_sous_activite", "CREATE");
        }
        if (path.startsWith("/api/demandes-reprise-plans")) {
            return new PermissionRequirement("demande_reprise_plan", actionFor(method));
        }
        if (path.startsWith("/api/demandes-reprise")) {
            return new PermissionRequirement("demande_reprise", actionFor(method));
        }
        if (path.startsWith("/api/plans-action")) {
            if (method.equals("POST") && path.contains("/avancements")) {
                return new PermissionRequirement("avancement_plan_action", "CREATE");
            }
            return new PermissionRequirement("plan_action", actionFor(method));
        }
        if (path.startsWith("/api/indicateurs/")) {
            if (method.equals("POST") && path.contains("/valeurs")) {
                return new PermissionRequirement("valeur_indicateur", "CREATE");
            }
            return new PermissionRequirement("indicateur", actionFor(method));
        }
        if (path.equals("/api/indicateurs")) {
            return new PermissionRequirement("indicateur", actionFor(method));
        }
        if (path.startsWith("/api/dashboard")) {
            return new PermissionRequirement("dashboard", "READ");
        }
        if (path.startsWith("/api/signalements")) {
            return new PermissionRequirement("signalement", actionFor(method));
        }
        if (path.startsWith("/api/notifications")) {
            return new PermissionRequirement("notification", actionFor(method));
        }
        if (path.startsWith("/api/utilisateurs/")) {
            return new PermissionRequirement("utilisateur", "READ");
        }
        if (path.equals("/api/utilisateur/me") || path.startsWith("/api/utilisateur/me/")) {
            return null;
        }
        return null;
    }

    private PermissionRequirement backofficeAction(String resource, String method) {
        String action = switch (method) {
            case "GET", "HEAD" -> "READ";
            case "POST" -> "CREATE";
            case "DELETE" -> "DELETE";
            case "PUT", "PATCH" -> "UPDATE";
            default -> "MANAGE";
        };
        return new PermissionRequirement(resource, action);
    }

    private String actionFor(String method) {
        return switch (method) {
            case "GET", "HEAD" -> "READ";
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "MANAGE";
        };
    }

    private boolean isActivityValidation(String path) {
        return Arrays.stream(path.split("/"))
                .anyMatch(segment -> segment.equals("valider") || segment.equals("soumettre-validation"));
    }

    private record PermissionRequirement(String resource, String action) { }
}

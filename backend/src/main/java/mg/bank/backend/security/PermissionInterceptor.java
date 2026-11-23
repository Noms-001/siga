package mg.bank.backend.security;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import mg.bank.backend.enums.PorteePermission;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.utilisateur.Utilisateur;
import mg.bank.backend.service.utilisateur.CurrentUserService;
import mg.bank.backend.service.utilisateur.PermissionService;

/**
 * Controle les permissions declarees par {@link RequirePermission} sur les
 * endpoints Spring MVC.
 *
 * Les permissions portees par la methode priment sur celles de la classe.
 * Aucune annotation signifie que l'endpoint n'est pas soumis au controle de
 * permission applicative (l'authentification reste assuree par Spring
 * Security).
 */
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final CurrentUserService currentUserService;
    private final PermissionService permissionService;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequirePermission[] required = handlerMethod.getMethod()
                .getAnnotationsByType(RequirePermission.class);

        if (required.length == 0) {
            required = handlerMethod.getBeanType()
                    .getAnnotationsByType(RequirePermission.class);
        }

        if (required.length == 0) {
            return true;
        }

        List<PermissionRequirement> permissions = Arrays.stream(required)
                .map(annotation -> new PermissionRequirement(
                        annotation.ressource(),
                        annotation.action()))
                .toList();

        Integer idPoste = currentUserService.getIdPoste();

        Optional<PorteePermission> portee = permissionService.autoriser(idPoste, permissions);
        if (portee.isEmpty()) {
            throw new ApiException(
                    "Vous n'avez pas les permissions requises pour cette action",
                    HttpStatus.FORBIDDEN);
        }

        if (portee.get() != PorteePermission.TOUS
                && PermissionPolicy.exigePorteeTotale(permissions)) {
            throw new ApiException(
                    "Cette ressource globale nécessite une portée totale",
                    HttpStatus.FORBIDDEN);
        }

        Utilisateur utilisateur = currentUserService.get();
        ContextePortee.poser(PorteeAcces.de(
                portee.get(),
                utilisateur.getIdUtilisateur(),
                utilisateur.getService() == null ? null : utilisateur.getService().getIdService(),
                utilisateur.getDepartement() == null ? null : utilisateur.getDepartement().getIdDepartement()));

        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        ContextePortee.nettoyer();
    }
}

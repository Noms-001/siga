package mg.bank.backend.service.utilisateur;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.common.PerimetreUtilisateur;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.utilisateur.Utilisateur;
import mg.bank.backend.repository.utilisateur.UtilisateurRepository;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UtilisateurRepository utilisateurRepository;

    /**
     * Utilisateur authentifie. Leve 401 si le contexte de securite est vide
     * ou si l'email du jeton ne correspond plus a un utilisateur en base.
     */
    public Utilisateur get() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new ApiException("Utilisateur non authentifie", HttpStatus.UNAUTHORIZED);
        }

        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException(
                        "Utilisateur introuvable", HttpStatus.UNAUTHORIZED));
    }

    /** Identifiant de l'utilisateur authentifie. */
    @Transactional(readOnly = true)
    public Integer getId() {
        return get().getIdUtilisateur();
    }

    /**
     * Perimetre de l'utilisateur authentifie : service + departement, ou
     * null si non rattache. Voir PerimetreUtilisateur pour la semantique.
     */
    @Transactional(readOnly = true)
    public PerimetreUtilisateur getPerimetre() {
        Utilisateur utilisateur = get();

        Integer idService = utilisateur.getService() == null
                ? null
                : utilisateur.getService().getIdService();

        Integer idDepartement = utilisateur.getDepartement() == null
                ? null
                : utilisateur.getDepartement().getIdDepartement();

        return PerimetreUtilisateur.builder()
                .idService(idService)
                .idDepartement(idDepartement)
                .build();
    }
}
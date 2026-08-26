package mg.bank.backend.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.repository.UtilisateurRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final AuthenticationManager authenticationManager;

    public AuthResult authenticate(String email, String password) {

        Utilisateur utilisateur = getUtilisateurByEmail(email);

        if (!Boolean.TRUE.equals(utilisateur.getActif())) {
            throw new ApiException(
                    "Le compte utilisateur est désactivé",
                    HttpStatus.FORBIDDEN);
        }

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            email,
                            password));
        } catch (AuthenticationException e) {
            throw new ApiException(
                    "Email ou mot de passe incorrect",
                    HttpStatus.UNAUTHORIZED);
        }

        utilisateur.setDateDerniereConnexion(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);

        return new AuthResult(authentication, utilisateur);
    }

    public Utilisateur getUtilisateurByEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(
                "Email ou mot de passe incorrect",
                HttpStatus.UNAUTHORIZED));
    }

    public record AuthResult(
            Authentication authentication,
            Utilisateur utilisateur) {

    }
}

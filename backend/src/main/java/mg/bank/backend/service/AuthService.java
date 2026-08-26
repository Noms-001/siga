package mg.bank.backend.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.enums.TypeTokenEnum;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.TokenAuth;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.repository.TokenAuthRepository;
import mg.bank.backend.repository.UtilisateurRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final TokenAuthRepository tokenAuthRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private static final String INVALID_TOKEN_MESSAGE = "Token invalide ou expiré";

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

    public Utilisateur activateAccount(
            String token,
            String password,
            String confirmPassword
    ) {

        if (!password.equals(confirmPassword)) {
            throw new ApiException(
                    "Les mots de passe ne correspondent pas",
                    HttpStatus.BAD_REQUEST
            );
        }

        TokenAuth tokenAuth = verifyActivationToken(token);

        Utilisateur utilisateur = tokenAuth.getUtilisateur();

        utilisateur.setMotDePasse(passwordEncoder.encode(password));
        utilisateur.setActif(true);
        utilisateur.setDateDesactivation(null);

        tokenAuth.setDateUtilisation(LocalDateTime.now());

        utilisateurRepository.save(utilisateur);
        tokenAuthRepository.save(tokenAuth);

        return utilisateur;
    }

    public TokenAuth verifyActivationToken(String token) {

        return verificationTypeToken(
                token,
                TypeTokenEnum.ACTIVATION_COMPTE
        );
    }

    private TokenAuth verificationTypeToken(
            String token,
            TypeTokenEnum type
    ) {
        TokenAuth tokenAuth = tokenAuthRepository.findByToken(token)
                .orElseThrow(()
                        -> new ApiException(
                        INVALID_TOKEN_MESSAGE,
                        HttpStatus.BAD_REQUEST
                )
                );

        if (!type.getCode().equals(tokenAuth.getTypeToken().getCode())
                || tokenAuth.getDateExpiration() == null
                || tokenAuth.getDateExpiration().isBefore(LocalDateTime.now())
                || tokenAuth.getDateUtilisation() != null) {

            throw new ApiException(
                    INVALID_TOKEN_MESSAGE,
                    HttpStatus.BAD_REQUEST
            );
        }

        return tokenAuth;
    }

    public record AuthResult(
            Authentication authentication,
            Utilisateur utilisateur) {

    }
}

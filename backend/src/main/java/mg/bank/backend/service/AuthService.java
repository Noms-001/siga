package mg.bank.backend.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.TokenAuth;
import mg.bank.backend.model.Utilisateur;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurService utilisateurService;
    private final TokenAuthService tokenAuthService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    public AuthResult authenticate(String email, String password, Boolean isBackoffice) {

        Utilisateur utilisateur = utilisateurService.getUtilisateurByEmail(email);

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
        utilisateurService.updateUtilisateur(utilisateur);

        return new AuthResult(authentication, utilisateur);
    }

    @Transactional
    public Utilisateur resetPassword(
            String token,
            String password,
            String confirmPassword) {

        if (!password.equals(confirmPassword)) {
            throw new ApiException(
                    "Les mots de passe ne correspondent pas",
                    HttpStatus.BAD_REQUEST);
        }

        TokenAuth tokenAuth = tokenAuthService.verifyResetPasswordToken(token);

        Utilisateur utilisateur = tokenAuth.getUtilisateur();

        utilisateur.setMotDePasse(passwordEncoder.encode(password));

        tokenAuth.setDateUtilisation(LocalDateTime.now());

        utilisateurService.updateUtilisateur(utilisateur);
        tokenAuthService.saveTokenAuth(tokenAuth);

        return utilisateur;
    }

    /**
     * Change le mot de passe d'un utilisateur connecté.
     *
     * L'ancien mot de passe est exigé : un mot de passe oublié ne se change pas
     * depuis la session, cela passe par le flux /password/forgot. Ici on vérifie
     * que l'utilisateur est bien lui-même, même si un jeton a été dérobé.
     *
     * @return l'utilisateur dont le mot de passe a été changé
     */
    @Transactional
    public Utilisateur changerMotDePasse(
            String email,
            String ancienMotDePasse,
            String nouveauMotDePasse,
            String confirmMotDePasse) {

        if (!nouveauMotDePasse.equals(confirmMotDePasse)) {
            throw new ApiException(
                    "Les mots de passe ne correspondent pas",
                    HttpStatus.BAD_REQUEST);
        }

        if (nouveauMotDePasse.equals(ancienMotDePasse)) {
            throw new ApiException(
                    "Le nouveau mot de passe doit être différent de l'ancien",
                    HttpStatus.BAD_REQUEST);
        }

        Utilisateur utilisateur = utilisateurService.getUtilisateurByEmail(email);

        if (!passwordEncoder.matches(ancienMotDePasse, utilisateur.getMotDePasse())) {
            throw new ApiException(
                    "L'ancien mot de passe est incorrect",
                    HttpStatus.UNAUTHORIZED);
        }

        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        utilisateur.setDateModification(LocalDateTime.now());

        return utilisateurService.updateUtilisateur(utilisateur);
    }

    @Transactional
    public Utilisateur activateAccount(
            String token,
            String password,
            String confirmPassword) {

        if (!password.equals(confirmPassword)) {
            throw new ApiException(
                    "Les mots de passe ne correspondent pas",
                    HttpStatus.BAD_REQUEST);
        }

        TokenAuth tokenAuth = tokenAuthService.verifyActivationToken(token);

        Utilisateur utilisateur = tokenAuth.getUtilisateur();

        utilisateur.setMotDePasse(passwordEncoder.encode(password));
        utilisateur.setActif(true);
        utilisateur.setDateDesactivation(null);

        tokenAuth.setDateUtilisation(LocalDateTime.now());

        utilisateurService.updateUtilisateur(utilisateur);
        tokenAuthService.saveTokenAuth(tokenAuth);

        return utilisateur;
    }

    public void forgotPassword(String email) {

        Utilisateur utilisateur = utilisateurService.getUtilisateurByEmail(email);

        if (!Boolean.TRUE.equals(utilisateur.getActif())) {
            throw new ApiException(
                    "Le compte n'est pas activé",
                    HttpStatus.FORBIDDEN);
        }

        TokenAuth tokenAuth = tokenAuthService.createResetPasswordToken(utilisateur);

        tokenAuthService.saveTokenAuth(tokenAuth);

        emailService.sendResetPasswordEmail(
                utilisateur,
                tokenAuth);
    }

    public record AuthResult(
            Authentication authentication,
            Utilisateur utilisateur) {

    }
}

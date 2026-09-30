package mg.bank.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ApiResponse;
import mg.bank.backend.dto.ChangerMotDePasseRequest;
import mg.bank.backend.dto.ModifierProfilRequest;
import mg.bank.backend.dto.ModifierProfilResponse;
import mg.bank.backend.dto.ProfileResponse;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.service.AuthService;
import mg.bank.backend.service.CustomUserDetailsService;
import mg.bank.backend.service.JwtService;
import mg.bank.backend.service.UtilisateurService;

@RestController
@RequestMapping("/api/utilisateur")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final AuthService authService;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Met à jour les informations personnelles de l'utilisateur connecté.
     *
     * L'email est le sujet du JWT : s'il change, le jeton en cours devient
     * invalide et l'utilisateur serait déconnecté. De nouveaux jetons sont
     * donc émis dans ce cas, et uniquement dans ce cas.
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<ModifierProfilResponse>> modifierProfil(
            Authentication authentication,
            @Valid @RequestBody ModifierProfilRequest request) {

        String emailCourant = authentication.getName();

        Utilisateur utilisateur = utilisateurService.modifierProfil(
                emailCourant,
                request);

        boolean emailModifie = !utilisateur.getEmail().equalsIgnoreCase(emailCourant);

        String accessToken = null;
        String refreshToken = null;

        if (emailModifie) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(
                    utilisateur.getEmail());

            accessToken = jwtService.generateAccessToken(userDetails);
            refreshToken = jwtService.generateRefreshToken(userDetails);
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        ModifierProfilResponse.builder()
                                .profil(ProfileResponse.from(utilisateur))
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .jetonRenouvele(emailModifie)
                                .build()));
    }

    /**
     * Change le mot de passe de l'utilisateur connecté.
     */
    @PostMapping("/me/mot-de-passe")
    public ResponseEntity<ApiResponse<Void>> changerMotDePasse(
            Authentication authentication,
            @Valid @RequestBody ChangerMotDePasseRequest request) {

        authService.changerMotDePasse(
                authentication.getName(),
                request.getAncienMotDePasse(),
                request.getPassword(),
                request.getConfirmPassword());

        return ResponseEntity.ok(
                ApiResponse.success(null));
    }
}

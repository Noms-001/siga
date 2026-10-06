package mg.bank.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ApiResponse;
import mg.bank.backend.dto.ChangerMotDePasseRequest;
import mg.bank.backend.dto.ModifierProfilRequest;
import mg.bank.backend.dto.ModifierProfilResponse;
import mg.bank.backend.dto.ProfileResponse;
import mg.bank.backend.dto.UtilisateurRequest;
import mg.bank.backend.dto.backoffice.UtilisateurResponse;
import mg.bank.backend.mapper.UtilisateurMapper;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.service.AuthService;
import mg.bank.backend.service.CustomUserDetailsService;
import mg.bank.backend.service.JwtService;
import mg.bank.backend.service.UtilisateurService;

@RestController
@RequestMapping("/api")
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
    @PutMapping("/utilisateur/me")
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
    @PostMapping("/utilisateur/me/mot-de-passe")
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

    @GetMapping("/backoffice/utilisateurs")
    public ResponseEntity<ApiResponse<List<UtilisateurResponse>>> lister(
            @RequestParam(name = "actif", required = false) Boolean actif,
            @RequestParam(name = "idDepartement", required = false) Integer idDepartement,
            @RequestParam(name = "idService", required = false) Integer idService) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        utilisateurService.lister(actif, idDepartement, idService)));
    }

    @GetMapping("/backoffice/utilisateurs/{id}")
    public ResponseEntity<ApiResponse<UtilisateurResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(utilisateurService.getById(id)));
    }

    /* -------------------- création -------------------- */

    /**
     * Création d'un utilisateur depuis le backoffice.
     *
     * Délègue à UtilisateurService.createUtilisateur — la logique de hash,
     * de génération de token d'activation et d'envoi d'email reste
     * centralisée dans le service existant. Ne pas dupliquer.
     *
     * Retourne 201 avec la vue backoffice de l'utilisateur créé. Aucun mot
     * de passe, aucun token n'est renvoyé.
     */
    @PostMapping("/backoffice/utilisateurs")
    public ResponseEntity<ApiResponse<UtilisateurResponse>> creer(
            @Valid @RequestBody UtilisateurRequest request) {
        Utilisateur cree = utilisateurService.createUtilisateur(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(UtilisateurMapper.toResponse(cree)));
    }

    /* -------------------- bascule -------------------- */

    @PatchMapping("/backoffice/utilisateurs/{id}/desactiver")
    public ResponseEntity<ApiResponse<UtilisateurResponse>> desactiver(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(utilisateurService.desactiverUtilisateur(id)));
    }

    @PatchMapping("/backoffice/utilisateurs/{id}/activer")
    public ResponseEntity<ApiResponse<UtilisateurResponse>> activer(@PathVariable Integer id) {
        return ResponseEntity.ok(
                ApiResponse.success(utilisateurService.activerUtilisateur(id)));
    }
}

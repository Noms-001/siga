package mg.bank.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.ActivationRequest;
import mg.bank.backend.dto.ApiResponse;
import mg.bank.backend.dto.ForgotPasswordRequest;
import mg.bank.backend.dto.LoginRequest;
import mg.bank.backend.dto.LoginResponse;
import mg.bank.backend.dto.ProfileResponse;
import mg.bank.backend.dto.ResetPasswordRequest;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.service.AuthService;
import mg.bank.backend.service.TokenAuthService;
import mg.bank.backend.service.UtilisateurService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UtilisateurService utilisateurService;
    private final TokenAuthService tokenAuthService;

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        AuthService.AuthResult result = authService.authenticate(
                request.getEmail(),
                request.getPassword());

        Authentication authentication = result.authentication();

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);

        SecurityContextHolder.setContext(securityContext);

        securityContextRepository.saveContext(
                securityContext,
                httpRequest,
                httpResponse);

        return ResponseEntity.ok(
                ApiResponse.success(
                        LoginResponse.from(result.utilisateur())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request
    ) {
        request.getSession().invalidate();

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(
                ApiResponse.success(null)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> me(
            Authentication authentication
    ) {
        String email = authentication.getName();

        Utilisateur utilisateur = utilisateurService.getUtilisateurByEmail(email);

        return ResponseEntity.ok(
                ApiResponse.success(
                        ProfileResponse.from(utilisateur)
                )
        );
    }

    @GetMapping("/activation/verify")
    public ResponseEntity<ApiResponse<Void>> verifyActivationToken(
            @RequestParam String token
    ) {
        tokenAuthService.verifyActivationToken(token);

        return ResponseEntity.ok(
                ApiResponse.success(null)
        );
    }

    @PostMapping("/activation")
    public ResponseEntity<ApiResponse<Void>> activate(
            @Valid @RequestBody ActivationRequest request
    ) {
        authService.activateAccount(
                request.getToken(),
                request.getPassword(),
                request.getConfirmPassword()
        );

        return ResponseEntity.ok(
                ApiResponse.success(null)
        );
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.forgotPassword(request.getEmail());

        return ResponseEntity.ok(
                ApiResponse.success(null)
        );
    }

    @GetMapping("/password/reset/verify")
    public ResponseEntity<ApiResponse<Void>> verifyResetPasswordToken(
            @RequestParam String token
    ) {
        tokenAuthService.verifyResetPasswordToken(token);

        return ResponseEntity.ok(
                ApiResponse.success(null)
        );
    }

    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(
                request.getToken(),
                request.getPassword(),
                request.getConfirmPassword()
        );

        return ResponseEntity.ok(
                ApiResponse.success(null)
        );
    }
}

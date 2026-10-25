package mg.bank.backend.controller.utilisateur;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.bank.backend.dto.auth.LoginRequest;
import mg.bank.backend.dto.auth.LoginResponse;
import mg.bank.backend.dto.common.ApiResponse;
import mg.bank.backend.service.utilisateur.AuthService;
import mg.bank.backend.service.utilisateur.JwtService;

@RestController
@RequestMapping("/api/backoffice/auth")
@RequiredArgsConstructor
public class BackAuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthService.AuthResult result = authService.authenticate(
                request.getEmail(),
                request.getPassword(),
                true
        );

        Authentication authentication = result.authentication();

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        String accessToken =
                jwtService.generateAccessToken(userDetails);

        String refreshToken =
                jwtService.generateRefreshToken(userDetails);

        LoginResponse response = LoginResponse.from(
                result.utilisateur(),
                accessToken,
                refreshToken
        );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }
}

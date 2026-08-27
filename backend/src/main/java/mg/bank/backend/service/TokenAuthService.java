package mg.bank.backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.enums.ParametreEnum;
import mg.bank.backend.enums.TypeTokenEnum;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.TokenAuth;
import mg.bank.backend.model.Utilisateur;

import mg.bank.backend.repository.TokenAuthRepository;
import mg.bank.backend.repository.TypeTokenRepository;

@Service
@RequiredArgsConstructor
public class TokenAuthService {

    private final TokenAuthRepository tokenAuthRepository;
    private final TypeTokenRepository typeTokenRepository;
    private final ParametreService parametreService;
    private static final String INVALID_TOKEN_MESSAGE = "Token invalide ou expiré";

    public TokenAuth createResetPasswordToken(Utilisateur utilisateur) {

        TokenAuth tokenAuth = new TokenAuth();

        tokenAuth.setUtilisateur(utilisateur);

        tokenAuth.setTypeToken(
                typeTokenRepository.findByCode(
                        TypeTokenEnum.RESET_MOT_DE_PASSE.getCode()
                ).orElseThrow(()
                        -> new ApiException(
                                "Type de token introuvable",
                                HttpStatus.INTERNAL_SERVER_ERROR
                        )
                )
        );

        tokenAuth.setToken(UUID.randomUUID().toString());
        tokenAuth.setDateCreation(LocalDateTime.now());

        long duree = parametreService.getLongValue(
                ParametreEnum.DUREE_TOKEN_RESET_MOT_DE_PASSE
        );

        tokenAuth.setDateExpiration(
                LocalDateTime.now().plusMinutes(duree)
        );

        return tokenAuth;
    }

    public TokenAuth createActivationToken(Utilisateur utilisateur) {

        TokenAuth tokenAuth = new TokenAuth();

        tokenAuth.setUtilisateur(utilisateur);

        tokenAuth.setTypeToken(
                typeTokenRepository.findByCode(
                        TypeTokenEnum.ACTIVATION_COMPTE.getCode()
                ).orElseThrow(()
                        -> new ApiException(
                                "Type de token introuvable",
                                HttpStatus.INTERNAL_SERVER_ERROR
                        )
                )
        );

        tokenAuth.setToken(UUID.randomUUID().toString());
        tokenAuth.setDateCreation(LocalDateTime.now());

        long duree = parametreService.getLongValue(
                ParametreEnum.DUREE_TOKEN_ACTIVATION_COMPTE
        );

        tokenAuth.setDateExpiration(
                LocalDateTime.now().plusMinutes(duree)
        );

        return tokenAuthRepository.save(tokenAuth);
    }

    public TokenAuth verifyActivationToken(String token) {

        return verificationTypeToken(
                token,
                TypeTokenEnum.ACTIVATION_COMPTE
        );
    }

    public TokenAuth verifyResetPasswordToken(String token) {

        return verificationTypeToken(
                token,
                TypeTokenEnum.RESET_MOT_DE_PASSE
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

    public void saveTokenAuth(TokenAuth tokenAuth) {
        tokenAuthRepository.save(tokenAuth);
    }
}

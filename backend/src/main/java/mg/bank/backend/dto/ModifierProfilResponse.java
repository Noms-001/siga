package mg.bank.backend.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Réponse de la mise à jour du profil.
 *
 * Le sujet du JWT est l'email. Modifier l'email rend donc le jeton courant
 * invalide, ce qui déconnecterait l'utilisateur en pleine session. Pour éviter
 * cela, de nouveaux jetons sont émis lorsque l'email a changé ; ils valent
 * null sinon, car le jeton existant reste valable.
 */
@Getter
@Builder
public class ModifierProfilResponse {

    private ProfileResponse profil;

    private String accessToken;

    private String refreshToken;

    /**
     * Vrai lorsque de nouveaux jetons ont été émis (donc quand l'email a changé).
     */
    private Boolean jetonRenouvele;
}

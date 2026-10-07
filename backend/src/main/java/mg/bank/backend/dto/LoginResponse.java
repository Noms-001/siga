package mg.bank.backend.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import mg.bank.backend.model.Utilisateur;

@Getter
@Builder
public class LoginResponse {

    private Integer idUtilisateur;
    private String nom;
    private String prenom;
    private String poste;
    private String service;

    private String accessToken;
    private String refreshToken;

    public static LoginResponse from(
            Utilisateur utilisateur,
            String accessToken,
            String refreshToken) {

        return LoginResponse.builder()
                .idUtilisateur(utilisateur.getIdUtilisateur())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .poste(utilisateur.getPoste().getLibelle())
                .service(utilisateur.getService() != null
                        ? utilisateur.getService().getNom()
                        : null)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}

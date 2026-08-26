package mg.bank.backend.dto;

import lombok.Builder;
import lombok.Getter;
import mg.bank.backend.model.Utilisateur;

@Getter
@Builder
public class LoginResponse {

    private Integer idUtilisateur;
    private String nom;
    private String prenom;

    public static LoginResponse from(Utilisateur utilisateur) {
        return LoginResponse.builder()
                .idUtilisateur(utilisateur.getIdUtilisateur())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .build();
    }
}
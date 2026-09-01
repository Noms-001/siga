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
    private String poste;
    private String service;

    public static LoginResponse from(Utilisateur utilisateur) {
        return LoginResponse.builder()
                .idUtilisateur(utilisateur.getIdUtilisateur())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .poste(utilisateur.getPoste() != null
                        ? utilisateur.getPoste().getLibelle()
                        : null)
                .service(utilisateur.getService() != null
                        ? utilisateur.getService().getNom()
                        : null)
                .build();
    }
}
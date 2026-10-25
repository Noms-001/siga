package mg.bank.backend.dto.utilisateur;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import mg.bank.backend.model.utilisateur.Utilisateur;

@Getter
@Builder
public class ProfileResponse {

    private Integer idUtilisateur;

    private String nom;
    private String prenom;
    private String email;
    private String telephone;

    private String service;
    private String poste;

    private Boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDesactivation;
    private LocalDateTime dateDerniereConnexion;

    public static ProfileResponse from(Utilisateur utilisateur) {
        return ProfileResponse.builder()
                .idUtilisateur(utilisateur.getIdUtilisateur())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .email(utilisateur.getEmail())
                .telephone(utilisateur.getTelephone())
                .service(
                        utilisateur.getService() != null
                        ? utilisateur.getService().getNom()
                        : null
                )
                .poste(utilisateur.getPoste().getLibelle())
                .actif(utilisateur.getActif())
                .dateCreation(utilisateur.getDateCreation())
                .dateDesactivation(utilisateur.getDateDesactivation())
                .dateDerniereConnexion(utilisateur.getDateDerniereConnexion())
                .build();
    }
}

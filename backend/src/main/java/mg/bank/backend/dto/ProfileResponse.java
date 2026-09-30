package mg.bank.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import mg.bank.backend.model.Utilisateur;

@Getter
@Builder
public class ProfileResponse {

    private Integer idUtilisateur;

    private String nom;
    private String prenom;
    private String email;
    private String telephone;

    private String service;
    private List<String> postes;

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
                .postes(
                        utilisateur.getPostes()
                                .stream()
                                .map(poste -> poste.getLibelle())
                                .toList()
                )
                .actif(utilisateur.getActif())
                .dateCreation(utilisateur.getDateCreation())
                .dateDesactivation(utilisateur.getDateDesactivation())
                .dateDerniereConnexion(utilisateur.getDateDerniereConnexion())
                .build();
    }
}

package mg.bank.backend.dto.backoffice;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UtilisateurResponse {
    private Integer id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private Boolean actif;
    private Boolean enAttenteActivation;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private LocalDateTime dateDesactivation;
    private LocalDateTime dateDerniereConnexion;
    private DepartementSummaryResponse departement;
    private ServiceSummaryResponse service;
    private PosteSummaryResponse poste;
}
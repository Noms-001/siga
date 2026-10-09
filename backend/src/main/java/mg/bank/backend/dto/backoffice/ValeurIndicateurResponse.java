package mg.bank.backend.dto.backoffice;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ValeurIndicateurResponse {
    private Integer id;
    private Double valeur;
    private LocalDateTime periodeDebut;
    private LocalDateTime periodeFin;
    private String commentaire;
    private LocalDateTime dateSaisie;
    private Integer idUtilisateur;
    private String nomUtilisateur;
    private String prenomUtilisateur;
}
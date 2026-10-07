package mg.bank.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResponsableSuiviDTO {
    private Integer idUtilisateur;
    private String nom;
    private String prenom;
}
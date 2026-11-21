package mg.bank.backend.dto.export;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SuiviExportDTO {

    private LocalDateTime dateChangement;

    private String utilisateur;

    private Double valeurPourcentage;

    private String commentaire;

    private String motif;

    private String statutCode;

    private String statutLibelle;
}

package mg.bank.backend.dto.dashboard;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardValidationDTO {
    private Integer idValidation;
    private Integer idActivite;
    private String codeActivite;
    private String designationActivite;
    private String demandeur;
    private String etape;
    private Integer niveau;
    private LocalDateTime dateDemande;
    private String decision;
}
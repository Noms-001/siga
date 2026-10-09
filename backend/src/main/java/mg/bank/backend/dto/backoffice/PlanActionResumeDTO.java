package mg.bank.backend.dto.backoffice;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlanActionResumeDTO {
    private Integer id;
    private String code;
    private String designation;
    private Boolean estPrincipale;
}
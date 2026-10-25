package mg.bank.backend.dto.activite;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SousActiviteSuiviDTO {
    private Integer id;
    private String code;
    private String designation;
    private Double avancement;
}
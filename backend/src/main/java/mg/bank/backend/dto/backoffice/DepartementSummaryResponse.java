package mg.bank.backend.dto.backoffice;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartementSummaryResponse {
    private Integer id;
    private String code;
    private String nom;
}
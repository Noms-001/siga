package mg.bank.backend.dto.backoffice;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServiceSummaryResponse {
    private Integer id;
    private String nom;
    private Boolean actif;
    private DepartementSummaryResponse departement;
}
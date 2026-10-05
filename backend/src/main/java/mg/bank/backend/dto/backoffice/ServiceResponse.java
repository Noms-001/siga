package mg.bank.backend.dto.backoffice;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServiceResponse {
    private Integer id;
    private String nom;
    private String description;
    private Boolean actif;
    private LocalDateTime dateDesactivation;
    private DepartementSummaryResponse departement;
}
package mg.bank.backend.dto.backoffice;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TypeActiviteResponse {
    private Integer id;
    private String designation;
    private String description;
    private Boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDesactivation;
    private List<ServiceSummaryResponse> services;
}
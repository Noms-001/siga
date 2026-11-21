package mg.bank.backend.dto.export;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlanActionExportResponse {

    private String direction;

    private String departement;

    private String departementCode;

    private String service;

    private Integer serviceId;

    private Integer annee;

    private LocalDateTime dateGeneration;

    private List<ServiceExportDTO> services;

    private List<PlanActionExportDTO> plans;
}

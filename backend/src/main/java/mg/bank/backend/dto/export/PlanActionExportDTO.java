package mg.bank.backend.dto.export;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlanActionExportDTO {

    private Integer id;

    private String code;

    private String designation;

    private LocalDate dateDebutPrevue;

    private LocalDate dateFinPrevue;

    private LocalDate dateDebutReelle;

    private LocalDate dateFinReelle;

    private String statutCode;

    private String statutLibelle;

    private Double avancement;

    private Integer serviceId;

    private String serviceNom;

    private String servicesLies;

    private List<String> responsables;

    private String motifReprise;

    private List<OrigineExportDTO> origines;

    private List<ActiviteLieeExportDTO> activites;

    private List<SuiviPlanActionExportDTO> suivis;
}

package mg.bank.backend.dto.planaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.bank.backend.dto.affectation.AffectationPlanActionDTO;
import mg.bank.backend.dto.common.ReferenceDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanActionDetailDTO {
    private Integer id;
    private String code;
    private String designation;
    private LocalDate dateDebutPrevue;
    private LocalDate dateFinPrevue;
    private LocalDate dateDebutReelle;
    private LocalDate dateFinReelle;
    private ReferenceDTO statut;
    private BigDecimal avancement;
    private ReferenceDTO responsable;
    private List<OrigineLienDTO> origines;
    private List<AffectationPlanActionDTO> affectations;
    private List<AvancementPlanActionDTO> historique;
}
package mg.bank.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumePlansActionDTO {
    private int total;
    private int nonCommences;
    private int enCours;
    private int enRetard;
    private int termines;
    private BigDecimal avancementMoyenGlobal;
    private List<PlanActionAvancementDTO> details;
}
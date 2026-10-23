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
public class ResumeActivitesDTO {
    private int total;
    private int nonCommencees;
    private int enCours;
    private int enRetard;
    private int terminees;
    private BigDecimal avancementMoyenGlobal;
    private List<ActiviteAvancementDTO> details;
}
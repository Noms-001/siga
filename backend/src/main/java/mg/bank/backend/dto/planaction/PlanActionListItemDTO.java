package mg.bank.backend.dto.planaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.bank.backend.dto.ReferenceDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanActionListItemDTO {
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
}
package mg.bank.backend.dto.planaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.bank.backend.dto.common.ReferenceDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanActionAvancementDTO {
    private Integer id;
    private String code;
    private String designation;
    private ReferenceDTO role;
    private ReferenceDTO statut;
    private BigDecimal avancement;
    private LocalDate dateDebutPrevue;
    private LocalDate dateFinPrevue;
    private LocalDate dateDebutReelle;
    private LocalDate dateFinReelle;
    private long joursDeRetard;
    private boolean enRetard;
    private LocalDateTime dateAffectation;
}
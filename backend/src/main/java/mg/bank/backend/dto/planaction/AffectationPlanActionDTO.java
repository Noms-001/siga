package mg.bank.backend.dto.planaction;

import java.time.LocalDateTime;

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
public class AffectationPlanActionDTO {
    private Integer id;
    private LocalDateTime dateAffectation;
    private LocalDateTime dateDesaffectation;
    private ReferenceDTO role;
    private ReferenceDTO utilisateur;
    private boolean active;
}
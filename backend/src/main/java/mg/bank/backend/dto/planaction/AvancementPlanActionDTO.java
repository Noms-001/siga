package mg.bank.backend.dto.planaction;

import java.math.BigDecimal;
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
public class AvancementPlanActionDTO {
    private Integer id;
    private BigDecimal valeurPourcentage;
    private String commentaire;
    private LocalDateTime dateChangement;
    private ReferenceDTO statut;
    private ReferenceDTO utilisateur;
}
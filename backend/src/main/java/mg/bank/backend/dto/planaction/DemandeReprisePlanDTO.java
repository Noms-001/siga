package mg.bank.backend.dto.planaction;

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
public class DemandeReprisePlanDTO {
    private Integer id;
    private LocalDateTime dateDemande;
    private LocalDateTime dateDecision;
    private String statut;
    private String motif;
    private String commentaireDecision;
    private ReferenceDTO planAction;
    private ReferenceDTO demandeur;
    private ReferenceDTO responsableActuel;
    private ReferenceDTO decideur;
}
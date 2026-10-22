package mg.bank.backend.dto;

import java.time.LocalDateTime;

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
public class DemandeRepriseDTO {
    private Integer id;
    private LocalDateTime dateDemande;
    private LocalDateTime dateDecision;
    private String statut;
    private String motif;
    private String commentaireDecision;
    private ReferenceDTO activite;
    private ReferenceDTO demandeur;
    private ReferenceDTO responsableActuel;
    private ReferenceDTO decideur;
}
package mg.bank.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import mg.bank.backend.enums.DecisionValidation;

/**
 * Passage d une activite par une etape de validation.
 *
 * La liste renvoyee par le detail est triee par niveau d etape croissant :
 * c est l ordre du circuit, et il est stable. A l inverse de l historique,
 * le decroissant n aurait aucun sens ici.
 *
 * decideur est non nul en base, y compris sur une decision en attente, ou il
 * designe l utilisateur prevu pour decider. Le champ est donc toujours
 * present ; seule dateDecision distingue une decision prise d une demande
 * encore ouverte.
 */
@Getter
@Builder
@AllArgsConstructor
public class ValidationActiviteDTO {

    private Integer id;

    private DecisionValidation decision;

    private String commentaire;

    private LocalDateTime dateDemande;

    /** Nulle tant que la decision est EN_ATTENTE_VALIDATION. */
    private LocalDateTime dateDecision;

    private EtapeValidationDTO etape;

    private UtilisateurResumeDTO demandeur;

    private UtilisateurResumeDTO decideur;

}

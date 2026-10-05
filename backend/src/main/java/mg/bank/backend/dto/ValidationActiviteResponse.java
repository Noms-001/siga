package mg.bank.backend.dto;

import java.time.LocalDateTime;

import mg.bank.backend.enums.DecisionValidation;
import mg.bank.backend.model.ValidationActivite;

public record ValidationActiviteResponse(

        Integer idValidationActivite,

        DecisionValidation decision,
        String derniereDecision,

        String commentaire,

        LocalDateTime dateDemande,

        LocalDateTime dateDecision,

        Integer idActivite,

        Integer idEtapeValidation,

        String designationEtape,

        Integer niveau,

        Boolean obligatoire,

        Integer idDemandeur,

        Integer idDecideur

) {
        public ValidationActiviteResponse(ValidationActivite validation, String derniereDecision) {
        this(
                validation.getIdValidationActivite(),
                validation.getDecision(),
                derniereDecision,
                validation.getCommentaire(),
                validation.getDateDemande(),
                validation.getDateDecision(),
                validation.getActivite().getIdActivite(),
                validation.getEtapeValidation().getIdEtapeValidation(),
                validation.getEtapeValidation().getDesignation(),
                validation.getEtapeValidation().getNiveau(),
                validation.getEtapeValidation().getObligatoire(),
                validation.getDemandeur().getIdUtilisateur(),
                validation.getDecideur() != null
                        ? validation.getDecideur().getIdUtilisateur()
                        : null
        );
    }
}
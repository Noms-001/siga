package mg.bank.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Resultat d une decision de validation.
 *
 * CE QUE LA PAGE A BESOIN
 * L'identifiant et le code de l'activite, la decision prise, le statut que
 * cette decision a produit, et l'instant de la decision.
 *
 * LA DATE EST CELLE DE LA DECISION, ET NON CELLE DU CHANGEMENT DE STATUT
 * Les deux sont posees dans la meme transaction, donc egales a la seconde pres.
 * La distinction ne se justifierait pas ici : elle apparaitrait le jour ou la
 * decision et le statut cesseraient d'etre poses ensemble.
 */
@Getter
@Builder
@AllArgsConstructor
public class ActiviteDecisionDTO {

    private Integer idActivite;

    private String code;

    private String decision;

    private String statut;

    private LocalDateTime dateDecision;

}

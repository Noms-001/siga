package mg.bank.backend.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Code propose pour une activite NON PTA.
 *
 * `annee` est renvoyee avec le code parce que la proposition vaut pour elle
 * seule : le formulaire qui change d'annee doit savoir que la valeur affichee
 * ne lui correspond plus, plutot que de la laisser en place comme si elle
 * restait valable.
 *
 * Aucun identifiant : ce n'est pas une ressource, c'est une valeur
 * calculee a la demande.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class CodeProposeDTO {
    private String code;
    private Integer annee;
}

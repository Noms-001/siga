package mg.bank.backend.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Ligne de resultat intermediaire pre-remplie dans le formulaire de
 * modification.
 *
 * Meme contour que ResultatIntermediaireEcritureRequest, plus l identifiant.
 * L identifiant n est pas renvoye pour etre reenvoye, mais pour que le front
 * puisse distinguer une ligne deja enregistree d une ligne neuve et ne
 * propose pas de la supprimer une fois le formulaire enregistre.
 */
@Getter
@Builder
public class ResultatIntermediaireFormulaireDTO {

    private Integer idResultatIntermediaire;

    private String designation;

}

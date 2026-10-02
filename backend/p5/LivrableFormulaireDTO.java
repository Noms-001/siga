package mg.bank.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Livrable pre-rempli dans le formulaire de modification.
 *
 * Meme contour que LivrableEcritureRequest, plus l'identifiant et le nombre
 * de fichiers deposes.
 *
 * Le nombre de fichiers n'est pas un compteur d'affichage : c'est ce qui
 * permet au formulaire de ne pas proposer de supprimer un livrable qui porte
 * des depots, que le backend refuserait ensuite en 409. Il est donc renvoye au
 * moment du chargement, et non relu a l enregistrement -- entre les deux, un
 * depot peut avoir ete ajoute.
 */
@Getter
@Builder
@AllArgsConstructor
public class LivrableFormulaireDTO {

    private Integer idLivrable;

    private String designation;

    private String description;

    /**
     * Vrai si au moins un fichier a ete depose sur ce livrable, et si sa
     * suppression est donc refusee.
     */
    private boolean fichiersDeposes;

}

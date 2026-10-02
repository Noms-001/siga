package mg.bank.backend.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Livrable pre-rempli dans le formulaire de modification.
 *
 * Meme contour que LivrableEcritureRequest, plus l'identifiant et le nombre
 * de fichiers deposes.
 *
 * POURQUOI UN COMPTEUR ET NON UN BOOLIEN
 *
 * Le formulaire ne doit pas proposer de supprimer un livrable qui porte des
 * depots, que le backend refuserait ensuite. Un compteur dit la meme chose et
 * en plus : il affiche "2 fichiers" plutot qu'un vide, ce qui evite a
 * l'utilisateur de croire qu'un depot a disparu. Le backend, lui, recompte
 * a l'enregistrement : entre le chargement et l'enregistrement, un depot peut
 * avoir ete ajoute, et c'est la valeur de ce moment-la qui fait foi.
 */
@Getter
@Builder
public class LivrableFormulaireDTO {

    private Integer idLivrable;

    private String designation;

    private String description;

    /**
     * Nombre de fichiers deposes sur ce livrable. La suppression est refusee
     * des qu il est superieur a zero.
     */
    private long nombreFichiers;

}

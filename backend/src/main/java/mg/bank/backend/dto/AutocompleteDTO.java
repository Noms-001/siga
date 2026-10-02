package mg.bank.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Suggestion d'autocomplete.
 *
 * libelleSecondaire porte le second axe de recherche (designation d'un
 * objectif, ou type d'activite pour une activite) afin que le front
 * affiche les deux lignes sans Having a refaire l'association.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class AutocompleteDTO {

    private Integer id;
    private String code;
    private String libelle;
    private String libelleSecondaire;

    /**
     * Annee de l'objectif, pour l'autocomplete des objectifs uniquement.
     *
     * Distincte de libelleSecondaire, qui n'en est que la version affichee
     * dans le menu : le code d'une activite doit reprendre l'annee reelle,
     * et la reextraire d'un libelle destine a l'ecran serait fragile.
     */
    private Integer annee;

    /**
     * Prochain numero disponible dans l'objectif, pour l'autocomplete des
     * objectifs uniquement : reste null sur l'autocomplete des activites,
     * qui n'a pas d'objectif.
     *
     * Renvoye en nombre et non en chaine : le zero de remplissage a deux
     * chiffres appartient au format du code d'activite, qui est une regle du
     * formulaire, et non une propriete de l'objectif.
     */
    private Integer prochainNumero;

}

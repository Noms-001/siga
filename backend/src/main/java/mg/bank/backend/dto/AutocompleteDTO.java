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

}

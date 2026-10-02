package mg.bank.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Option d'un filtre deroulant, construite a partir des referentiels.
 * Aucune valeur n'est codee en dur.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class OptionDTO {

    private Integer id;
    private String code;
    private String libelle;

}

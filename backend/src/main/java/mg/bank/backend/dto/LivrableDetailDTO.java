package mg.bank.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Livrable attendu d une sous-activite, avec ses fichiers.
 *
 * Les fichiers sont imbriques dans le livrable et non dans une collection
 * separee de la sous-activite : la table fichier_sous_activite ne depend que
 * de livrable_sous_activite, jamais directement de la sous-activite. Les
 * regrouper ici evite au front de reconstruire la filiation.
 */
@Getter
@Builder
@AllArgsConstructor
public class LivrableDetailDTO {

    private Integer id;

    private String designation;

    private String description;

    /** Jamais null : une liste vide signifie aucun depot. */
    private List<FichierDetailDTO> fichiers;

}

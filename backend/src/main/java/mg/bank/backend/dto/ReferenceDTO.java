package mg.bank.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Reference complete d'un objet cible, exposee pour eviter au front de
 * reconstruire un libelle a partir de plusieurs champs.
 */
@Getter
@Builder
@AllArgsConstructor
public class ReferenceDTO {

    private Integer id;
    private String code;
    private String libelle;
    private Integer annee;
}

package mg.bank.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Etape du circuit de validation atteinte par une activite.
 *
 * Reproduit etape_validation, completee par le nom de la procedure
 * parente : sans lui, deux etapes de meme designation mais de procedures
 * differentes seraient indiscernables.
 */
@Getter
@Builder
@AllArgsConstructor
public class EtapeValidationDTO {

    private Integer id;

    private String designation;

    private String description;

    /** Rang de l etape dans la procedure : 1 est la premiere. */
    private Integer niveau;

    private Boolean obligatoire;

    private String procedureLibelle;

}

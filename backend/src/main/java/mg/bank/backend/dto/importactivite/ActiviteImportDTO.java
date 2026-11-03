package mg.bank.backend.dto.importactivite;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiviteImportDTO {

    /** Code facultatif : généré à l'import s'il est absent, validé sinon. */
    private String code;

    private String designation;

    /** Code de la priorite. Facultatif : NORMALE si absent. */
    private String priorite;

    /** Designation du type d'activite. Facultatif. */
    private String typeActivite;

    /** Nom du site. Facultatif. */
    private String site;

    /** Nullable = activite NON PTA. Obligatoire pour un PTA. */
    private ObjectifSpecifiqueImportReferenceDTO objectifSpecifique;

    /** Obligatoire et non vide : les dates de l'activite en sont derivees. */
    private List<SousActiviteImportDTO> sousActivites;

    private List<ResultatIntermediaireImportDTO> resultatsIntermediaires;

    private List<IndicateurImportReferenceDTO> indicateurs;

    private List<OrigineImportReferenceDTO> origines;
}
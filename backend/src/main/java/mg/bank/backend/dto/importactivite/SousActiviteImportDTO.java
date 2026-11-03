package mg.bank.backend.dto.importactivite;

import java.time.LocalDate;
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
public class SousActiviteImportDTO {

    /** Code facultatif : généré à partir de l'activité parente s'il est absent. */
    private String code;

    private String designation;

    /** Obligatoire : sert au calcul de la date de debut de l'activite. */
    private LocalDate dateDebut;

    /** Obligatoire : sert au calcul de la date de fin de l'activite. */
    private LocalDate dateFin;

    private List<LivrableImportDTO> livrables;
}
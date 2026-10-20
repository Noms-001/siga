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
public class RapportValidationDTO {

    private boolean valide;

    private String message;

    private int nombreActivites;

    private int nombreErreurs;

    private int nombreAvertissements;

    private List<ErreurImportDTO> erreurs;

    private List<AvertissementImportDTO> avertissements;

    private List<ApercuActiviteDTO> apercu;
}
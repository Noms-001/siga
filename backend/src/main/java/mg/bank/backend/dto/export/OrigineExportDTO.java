package mg.bank.backend.dto.export;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrigineExportDTO {

    private String code;

    private String designation;

    private String typeOrigine;

    private Boolean estPrincipale;
}

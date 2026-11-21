package mg.bank.backend.dto.export;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActiviteLieeExportDTO {

    private Integer id;

    private String code;

    private String designation;

    private Integer serviceId;

    private String serviceNom;

    private String origineCode;

    private String typeOrigine;
}

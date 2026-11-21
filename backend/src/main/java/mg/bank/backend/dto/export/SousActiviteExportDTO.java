package mg.bank.backend.dto.export;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SousActiviteExportDTO {

    private Integer id;

    private String code;

    private String designation;

    private LocalDate dateDebutPrevue;

    private LocalDate dateFinPrevue;

    private String statutCode;

    private String statutLibelle;

    private List<String> responsables;

    private Double avancement;

    private List<SuiviExportDTO> suivis;
}

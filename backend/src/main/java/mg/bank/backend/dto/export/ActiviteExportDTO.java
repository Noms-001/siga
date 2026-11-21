package mg.bank.backend.dto.export;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActiviteExportDTO {

    private Integer id;

    private String code;

    private String reference;

    private String designation;

    private Boolean pta;

    private String objectifSpecifiqueCode;

    private String objectifSpecifiqueDesignation;

    private String procedure;

    private LocalDate dateDebutPrevue;

    private LocalDate dateFinPrevue;

    private String statutCode;

    private String statutLibelle;

    private Integer serviceId;

    private String serviceNom;

    private String priorite;

    private List<String> responsables;

    private Double avancement;

    private List<SousActiviteExportDTO> sousActivites;
}

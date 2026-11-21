package mg.bank.backend.dto.common;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanActionExportFiltres {

    private String statut;
    private String search;
    private Integer annee;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Integer origineId;
    private String typeOrigine;
    private Integer responsableId;
    private Integer serviceId;
}

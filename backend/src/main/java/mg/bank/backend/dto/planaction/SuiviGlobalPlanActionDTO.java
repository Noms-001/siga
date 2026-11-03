package mg.bank.backend.dto.planaction;

import java.math.BigDecimal;
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
public class SuiviGlobalPlanActionDTO {

    private ResumeGlobalDTO resume;
    private List<RepartitionOrigineDTO> parTypeOrigine;
    private List<RepartitionResponsableDTO> parResponsable;
    private List<PlanActionEnRetardDTO> enRetard;
    private List<PlanActionEcheanceDTO> echeancesProchaines;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResumeGlobalDTO {
        private int total;
        private int nonCommences;
        private int enCours;
        private int enRetard;
        private int termines;
        private BigDecimal avancementMoyen;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RepartitionOrigineDTO {
        private String typeOrigine;
        private int nombre;
        private BigDecimal avancementMoyen;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RepartitionResponsableDTO {
        private Integer idUtilisateur;
        private String libelle;
        private int total;
        private int enRetard;
        private BigDecimal avancementMoyen;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlanActionEnRetardDTO {
        private Integer id;
        private String code;
        private String designation;
        private String dateFinPrevue;
        private long joursDeRetard;
        private BigDecimal avancement;
        private String responsableLibelle;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlanActionEcheanceDTO {
        private Integer id;
        private String code;
        private String designation;
        private String dateFinPrevue;
        private long joursRestants;
        private BigDecimal avancement;
        private String responsableLibelle;
    }
}
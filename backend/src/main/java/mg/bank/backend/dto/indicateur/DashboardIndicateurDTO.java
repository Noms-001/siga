package mg.bank.backend.dto.indicateur;

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
public class DashboardIndicateurDTO {

    private ResumeDTO resume;
    private List<IndicateurAvecAtteinteDTO> indicateurs;
    private List<IndicateurEnRetardDTO> enRetard;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResumeDTO {
        private int total;
        private int atteints;
        private int nonAtteints;
        private int indetermines;
        private int enRetard;
        private int jamaisSaisis;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndicateurAvecAtteinteDTO {
        private Integer id;
        private String code;
        private String designation;
        private String typeIndicateur;
        private String uniteMesure;
        private BigDecimal valeurCible;
        private BigDecimal valeurCourante;
        private BigDecimal ecart;
        private String statut;
        private String frequenceVerification;
    }
}
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
public class CalendrierIndicateurDTO {
    private Integer idIndicateur;
    private String code;
    private String designation;
    private Integer annee;
    private String frequenceVerification;
    private String typeIndicateur;
    private String uniteMesure;
    private BigDecimal valeurCible;
    private BigDecimal seuilMin;
    private BigDecimal seuilMax;

    private int nombrePeriodes;
    private int nombreSaisies;
    private BigDecimal valeurMoyenne;

    private List<PeriodeCalendrierDTO> periodes;
}
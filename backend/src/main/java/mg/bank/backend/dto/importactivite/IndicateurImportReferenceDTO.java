package mg.bank.backend.dto.importactivite;

import java.math.BigDecimal;

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
public class IndicateurImportReferenceDTO {

    private String designation;

    private String typeIndicateur;
    private String uniteMesure;
    private String frequenceVerification;
    private String frequenceAggregation;
    private String definition;
    private String methodeDetermination;
    private String objectif;
    private BigDecimal valeurCible;
    private BigDecimal seuilMin;
    private BigDecimal seuilMax;
}
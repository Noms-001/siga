package mg.bank.backend.dto.backoffice;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IndicateurResponse {
    private Integer id;
    private String code;
    private String designation;
    private String codeHopex;
    private String indicateurHopex;
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
    private Boolean actif;
}
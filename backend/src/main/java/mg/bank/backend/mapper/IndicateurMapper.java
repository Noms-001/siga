package mg.bank.backend.mapper;

import mg.bank.backend.dto.backoffice.IndicateurResponse;
import mg.bank.backend.model.Indicateur;

public final class IndicateurMapper {

    private IndicateurMapper() {}

    public static IndicateurResponse toResponse(Indicateur i) {
        if (i == null) return null;
        return IndicateurResponse.builder()
                .id(i.getIdIndicateur())
                .code(i.getCode())
                .designation(i.getDesignation())
                .codeHopex(i.getCodeHopex())
                .indicateurHopex(i.getIndicateurHopex())
                .typeIndicateur(i.getTypeIndicateur())
                .uniteMesure(i.getUniteMesure())
                .frequenceVerification(i.getFrequenceVerification())
                .frequenceAggregation(i.getFrequenceAggregation())
                .definition(i.getDefinition())
                .methodeDetermination(i.getMethodeDetermination())
                .objectif(i.getObjectif())
                .valeurCible(i.getValeurCible())
                .seuilMin(i.getSeuilMin())
                .seuilMax(i.getSeuilMax())
                .actif(i.getActif())
                .build();
    }
}
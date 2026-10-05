package mg.bank.backend.mapper;

import mg.bank.backend.dto.backoffice.PrioriteResponse;
import mg.bank.backend.model.Priorite;

public final class PrioriteMapper {

    private PrioriteMapper() {}

    public static PrioriteResponse toResponse(Priorite p) {
        if (p == null) return null;
        return PrioriteResponse.builder()
                .id(p.getIdPriorite())
                .code(p.getCode())
                .libelle(p.getLibelle())
                .description(p.getDescription())
                .actif(p.getActif())
                .dateCreation(p.getDateCreation())
                .dateDesactivation(p.getDateDesactivation())
                .build();
    }
}
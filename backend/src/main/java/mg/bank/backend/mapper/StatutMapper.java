package mg.bank.backend.mapper;

import mg.bank.backend.dto.backoffice.StatutResponse;
import mg.bank.backend.model.Statut;

public final class StatutMapper {

    private StatutMapper() {}

    public static StatutResponse toResponse(Statut s) {
        if (s == null) return null;
        return StatutResponse.builder()
                .id(s.getIdStatut())
                .code(s.getCode())
                .libelle(s.getLibelle())
                .description(s.getDescription())
                .actif(s.getActif())
                .dateCreation(s.getDateCreation())
                .dateModification(s.getDateModification())
                .dateDesactivation(s.getDateDesactivation())
                .build();
    }
}
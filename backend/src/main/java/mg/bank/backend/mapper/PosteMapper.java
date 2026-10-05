package mg.bank.backend.mapper;

import mg.bank.backend.dto.backoffice.PosteResponse;
import mg.bank.backend.model.Poste;

public final class PosteMapper {

    private PosteMapper() {}

    public static PosteResponse toResponse(Poste p) {
        if (p == null) return null;
        return PosteResponse.builder()
                .id(p.getIdPoste())
                .nom(p.getLibelle())          // colonne poste.nom
                .effectifPrevu(p.getEffectifPrevu())
                .effectifReel(p.getEffectifReel())
                .isMetier(p.getIsMetier())
                .actif(p.getActif())
                .dateCreation(p.getDateCreation())
                .dateModification(p.getDateModification())
                .dateDesactivation(p.getDateDesactivation())
                .build();
    }
}
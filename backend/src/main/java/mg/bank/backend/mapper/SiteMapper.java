package mg.bank.backend.mapper;

import mg.bank.backend.dto.backoffice.SiteResponse;
import mg.bank.backend.model.Site;

public final class SiteMapper {

    private SiteMapper() {}

    public static SiteResponse toResponse(Site s) {
        if (s == null) return null;
        return SiteResponse.builder()
                .id(s.getIdSite())
                .nom(s.getNom())
                .actif(s.getActif())
                .dateCreation(s.getDateCreation())
                .dateDesactivation(s.getDateDesactivation())
                .build();
    }
}
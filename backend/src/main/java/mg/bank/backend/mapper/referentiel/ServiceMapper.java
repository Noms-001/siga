package mg.bank.backend.mapper.referentiel;

import mg.bank.backend.dto.backoffice.ServiceResponse;
import mg.bank.backend.dto.backoffice.ServiceSummaryResponse;
import mg.bank.backend.model.referentiel.Departement;

public final class ServiceMapper {

    private ServiceMapper() {
    }

    public static ServiceResponse toResponse(mg.bank.backend.model.referentiel.Service s) {
        if (s == null)
            return null;
        Departement d = s.getDepartement();
        return ServiceResponse.builder()
                .id(s.getIdService())
                .nom(s.getNom())
                .description(s.getDescription())
                .actif(s.getActif())
                .dateDesactivation(s.getDateDesactivation())
                .departement(DepartementMapper.toSummary(d))
                .build();
    }

    public static ServiceSummaryResponse toSummary(mg.bank.backend.model.referentiel.Service s) {
        if (s == null)
            return null;
        return ServiceSummaryResponse.builder()
                .id(s.getIdService())
                .nom(s.getNom())
                .actif(s.getActif())
                .departement(DepartementMapper.toSummary(s.getDepartement()))
                .build();
    }
}
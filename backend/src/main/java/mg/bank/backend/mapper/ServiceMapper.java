package mg.bank.backend.mapper;

import mg.bank.backend.dto.backoffice.ServiceResponse;
import mg.bank.backend.model.Departement;

public final class ServiceMapper {

    private ServiceMapper() {}

    public static ServiceResponse toResponse(mg.bank.backend.model.Service s) {
        if (s == null) return null;
        Departement d = s.getDepartement(); // chargé en LAZY → mapper depuis la même transaction
        return ServiceResponse.builder()
                .id(s.getIdService())
                .nom(s.getNom())
                .description(s.getDescription())
                .actif(s.getActif())
                .dateDesactivation(s.getDateDesactivation())
                .departement(DepartementMapper.toSummary(d))
                .build();
    }
}
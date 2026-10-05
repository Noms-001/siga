package mg.bank.backend.mapper;

import java.util.Comparator;
import java.util.List;

import mg.bank.backend.dto.backoffice.ServiceSummaryResponse;
import mg.bank.backend.dto.backoffice.TypeActiviteResponse;
import mg.bank.backend.model.TypeActivite;

public final class TypeActiviteMapper {

    private TypeActiviteMapper() {}

    public static TypeActiviteResponse toResponse(TypeActivite t) {
        if (t == null) return null;

        List<ServiceSummaryResponse> services = t.getServices().stream()
                .map(TypeActiviteMapper::toServiceSummary)
                .sorted(Comparator.comparing(ServiceSummaryResponse::getNom,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        return TypeActiviteResponse.builder()
                .id(t.getIdTypeActivite())
                .designation(t.getDesignation())
                .description(t.getDescription())
                .actif(t.getActif())
                .dateCreation(t.getDateCreation())
                .dateDesactivation(t.getDateDesactivation())
                .services(services)
                .build();
    }

    private static ServiceSummaryResponse toServiceSummary(mg.bank.backend.model.Service s) {
        return ServiceSummaryResponse.builder()
                .id(s.getIdService())
                .nom(s.getNom())
                .actif(s.getActif())
                .departement(DepartementMapper.toSummary(s.getDepartement()))
                .build();
    }
}
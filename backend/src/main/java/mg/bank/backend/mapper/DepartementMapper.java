package mg.bank.backend.mapper;

import mg.bank.backend.dto.backoffice.DepartementRequest;
import mg.bank.backend.dto.backoffice.DepartementResponse;
import mg.bank.backend.dto.backoffice.DepartementSummaryResponse;
import mg.bank.backend.model.Departement;

public final class DepartementMapper {

    private DepartementMapper() {}

    public static DepartementResponse toResponse(Departement d) {
        if (d == null) return null;
        return DepartementResponse.builder()
                .id(d.getIdDepartement())
                .code(d.getCode())
                .nom(d.getNom())
                .description(d.getDescription())
                .build();
    }

    public static DepartementSummaryResponse toSummary(Departement d) {
        if (d == null) return null;
        return DepartementSummaryResponse.builder()
                .id(d.getIdDepartement())
                .code(d.getCode())
                .nom(d.getNom())
                .build();
    }

    public static Departement toEntity(DepartementRequest r) {
        return Departement.builder()
                .code(r.getCode().trim())
                .nom(r.getNom().trim())
                .description(r.getDescription())
                .build();
    }
}
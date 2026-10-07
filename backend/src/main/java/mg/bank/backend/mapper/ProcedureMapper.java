package mg.bank.backend.mapper;

import java.util.Comparator;
import java.util.List;

import mg.bank.backend.dto.backoffice.EtapeValidationResponse;
import mg.bank.backend.dto.backoffice.ProcedureResponse;
import mg.bank.backend.model.Procedure;

public final class ProcedureMapper {

    private ProcedureMapper() {}

    public static ProcedureResponse toResponse(Procedure p) {
        if (p == null) return null;

        List<EtapeValidationResponse> etapes = p.getEtapesValidation().stream()
                .sorted(Comparator.comparing(
                        mg.bank.backend.model.EtapeValidation::getNiveau,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(EtapeValidationMapper::toResponse)
                .toList();

        return ProcedureResponse.builder()
                .id(p.getIdProcedure())
                .designation(p.getDesignation())
                .description(p.getDescription())
                .actif(p.getActif())
                .dateCreation(p.getDateCreation())
                .dateModification(p.getDateModification())
                .dateDesactivation(p.getDateDesactivation())
                .etapes(etapes)
                .build();
    }
}
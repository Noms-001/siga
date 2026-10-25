package mg.bank.backend.mapper.referentiel;

import java.util.Comparator;
import java.util.List;

import mg.bank.backend.dto.backoffice.ServiceSummaryResponse;
import mg.bank.backend.dto.backoffice.TypeActiviteResponse;
import mg.bank.backend.model.referentiel.TypeActivite;

public final class TypeActiviteMapper {

        private TypeActiviteMapper() {
        }

        public static TypeActiviteResponse toResponse(TypeActivite t) {
                if (t == null)
                        return null;

                List<ServiceSummaryResponse> services = t.getServices().stream()
                                .map(ServiceMapper::toSummary)
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
}
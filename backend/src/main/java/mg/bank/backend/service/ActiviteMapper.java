package mg.bank.backend.service;

import mg.bank.backend.dto.ActiviteListItemDTO;
import mg.bank.backend.dto.ActiviteStatistiquesDTO;
import mg.bank.backend.dto.ReferenceDTO;
import mg.bank.backend.repository.projection.ActiviteListRow;
import mg.bank.backend.repository.projection.ActiviteStatistiquesRow;

public final class ActiviteMapper {

    private ActiviteMapper() {
    }

    public static ActiviteListItemDTO versDto(ActiviteListRow row) {
        return ActiviteListItemDTO.builder()
                .id(row.getId())
                .code(row.getCode())
                .reference(row.getReference())
                .designation(row.getDesignation())
                .dateDebutPrevue(row.getDateDebutPrevue())
                .dateFinPrevue(row.getDateFinPrevue())
                .dateDebutReelle(row.getDateDebutReelle())
                .dateFinReelle(row.getDateFinReelle())
                .dateCreation(row.getDateCreation())
                .objectifSpecifique(row.getOsId() == null ? null : ReferenceDTO.builder()
                        .id(row.getOsId())
                        .code(row.getOsCode())
                        .libelle(row.getOsDesignation())
                        .annee(row.getOsAnnee())
                        .build())
                .service(ReferenceDTO.builder()
                        .id(row.getServiceId())
                        .libelle(row.getServiceLibelle())
                        .build())
                .typeActivite(row.getTypeActiviteId() == null ? null : ReferenceDTO.builder()
                        .id(row.getTypeActiviteId())
                        .libelle(row.getTypeActiviteLibelle())
                        .build())
                .site(row.getSiteId() == null ? null : ReferenceDTO.builder()
                        .id(row.getSiteId())
                        .libelle(row.getSiteLibelle())
                        .build())
                .priorite(row.getPrioriteId() == null ? null : ReferenceDTO.builder()
                        .id(row.getPrioriteId())
                        .code(row.getPrioriteCode())
                        .libelle(row.getPrioriteLibelle())
                        .build())
                .statut(row.getStatutId() == null ? null : ReferenceDTO.builder()
                        .id(row.getStatutId())
                        .code(row.getStatutCode())
                        .libelle(row.getStatutLibelle())
                        .build())
                .avancement(row.getAvancement() == null ? 0d : row.getAvancement())
                .build();
    }

    public static ActiviteStatistiquesDTO mapperStatistiques(ActiviteStatistiquesRow row) {
        return ActiviteStatistiquesDTO.builder()
                .total(ActiviteQuerySupport.nulSiAbsent(row == null ? null : row.getTotal()))
                .nonCommencees(ActiviteQuerySupport.nulSiAbsent(row == null ? null : row.getNonCommencees()))
                .enCours(ActiviteQuerySupport.nulSiAbsent(row == null ? null : row.getEnCours()))
                .terminees(ActiviteQuerySupport.nulSiAbsent(row == null ? null : row.getTerminees()))
                .enRetard(ActiviteQuerySupport.nulSiAbsent(row == null ? null : row.getEnRetard()))
                .annulees(ActiviteQuerySupport.nulSiAbsent(row == null ? null : row.getAnnulees()))
                .reportees(ActiviteQuerySupport.nulSiAbsent(row == null ? null : row.getReportees()))
                .suspendues(ActiviteQuerySupport.nulSiAbsent(row == null ? null : row.getSuspendues()))
                .build();
    }
}

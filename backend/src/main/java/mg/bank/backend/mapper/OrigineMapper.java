package mg.bank.backend.mapper;

import java.util.List;

import mg.bank.backend.dto.backoffice.OrigineResponse;
import mg.bank.backend.dto.backoffice.PlanActionResumeDTO;
import mg.bank.backend.dto.backoffice.SignalementDetailResponse;
import mg.bank.backend.model.Origine;
import mg.bank.backend.model.Utilisateur;
import mg.bank.backend.repository.projection.PlanActionOrigineRow;

public final class OrigineMapper {

    private OrigineMapper() {}

    public static OrigineResponse toResponse(Origine o) {
        if (o == null) return null;
        Utilisateur u = o.getUtilisateur();
        return OrigineResponse.builder()
                .id(o.getIdOrigine())
                .code(o.getCode())
                .designation(o.getDesignation())
                .typeOrigine(o.getTypeOrigine())
                .description(o.getDescription())
                .dateCreation(o.getDateCreation())
                .idUtilisateur(u == null ? null : u.getIdUtilisateur())
                .nomUtilisateur(u == null ? null : u.getNom())
                .prenomUtilisateur(u == null ? null : u.getPrenom())
                .build();
    }


    public static SignalementDetailResponse toDetail(
            Origine o,
            List<PlanActionOrigineRow> plansRows) {

        if (o == null) return null;

        List<PlanActionResumeDTO> plans = plansRows.stream()
                .map(r -> PlanActionResumeDTO.builder()
                        .id(r.getId())
                        .code(r.getCode())
                        .designation(r.getDesignation())
                        .estPrincipale(r.getEstPrincipale())
                        .build())
                .toList();

        Utilisateur u = o.getUtilisateur();

        return SignalementDetailResponse.builder()
                .id(o.getIdOrigine())
                .code(o.getCode())
                .designation(o.getDesignation())
                .typeOrigine(o.getTypeOrigine())
                .description(o.getDescription())
                .dateCreation(o.getDateCreation())
                .idUtilisateur(u == null ? null : u.getIdUtilisateur())
                .nomUtilisateur(u == null ? null : u.getNom())
                .prenomUtilisateur(u == null ? null : u.getPrenom())
                .plansAction(plans)
                .build();
    }
}
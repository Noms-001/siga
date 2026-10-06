package mg.bank.backend.mapper;

import java.util.Comparator;
import java.util.List;

import mg.bank.backend.dto.backoffice.DepartementSummaryResponse;
import mg.bank.backend.dto.backoffice.PosteSummaryResponse;
import mg.bank.backend.dto.backoffice.ServiceSummaryResponse;
import mg.bank.backend.dto.backoffice.UtilisateurResponse;
import mg.bank.backend.model.Poste;
import mg.bank.backend.model.Utilisateur;

public final class UtilisateurMapper {

    private UtilisateurMapper() {}

    public static UtilisateurResponse toResponse(Utilisateur u) {
        if (u == null) return null;

        DepartementSummaryResponse dep = u.getDepartement() == null
                ? null
                : DepartementMapper.toSummary(u.getDepartement());

        ServiceSummaryResponse srv = u.getService() == null
                ? null
                : ServiceSummaryResponse.builder()
                        .id(u.getService().getIdService())
                        .nom(u.getService().getNom())
                        .actif(u.getService().getActif())
                        .departement(DepartementMapper.toSummary(u.getService().getDepartement()))
                        .build();

        List<PosteSummaryResponse> postes = u.getPostes().stream()
                .map(UtilisateurMapper::toPosteSummary)
                .sorted(Comparator.comparing(PosteSummaryResponse::getNom,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        return UtilisateurResponse.builder()
                .id(u.getIdUtilisateur())
                .nom(u.getNom())
                .prenom(u.getPrenom())
                .email(u.getEmail())
                .telephone(u.getTelephone())
                .actif(u.getActif())
                .enAttenteActivation(u.getMotDePasse() == null)
                .dateCreation(u.getDateCreation())
                .dateModification(u.getDateModification())
                .dateDesactivation(u.getDateDesactivation())
                .dateDerniereConnexion(u.getDateDerniereConnexion())
                .departement(dep)
                .service(srv)
                .postes(postes)
                .build();
    }

    private static PosteSummaryResponse toPosteSummary(Poste p) {
        return PosteSummaryResponse.builder()
                .id(p.getIdPoste())
                .nom(p.getLibelle())     // colonne poste.nom
                .isMetier(p.getIsMetier())
                .build();
    }
}
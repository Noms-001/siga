package mg.bank.backend.mapper.utilisateur;

import java.util.Comparator;
import java.util.List;

import mg.bank.backend.dto.backoffice.DepartementSummaryResponse;
import mg.bank.backend.dto.backoffice.PosteSummaryResponse;
import mg.bank.backend.dto.backoffice.ServiceSummaryResponse;
import mg.bank.backend.dto.backoffice.UtilisateurResponse;
import mg.bank.backend.mapper.referentiel.DepartementMapper;
import mg.bank.backend.mapper.referentiel.ServiceMapper;
import mg.bank.backend.model.utilisateur.Poste;
import mg.bank.backend.model.utilisateur.Utilisateur;

public final class UtilisateurMapper {

        private UtilisateurMapper() {
        }

        public static UtilisateurResponse toResponse(Utilisateur u) {
                if (u == null)
                        return null;

                DepartementSummaryResponse dep = u.getDepartement() == null
                                ? null
                                : DepartementMapper.toSummary(u.getDepartement());

                ServiceSummaryResponse srv = ServiceMapper.toSummary(u.getService());

                PosteSummaryResponse poste = u.getPoste() == null
                                ? null
                                : toPosteSummary(u.getPoste());

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
                                .poste(poste)
                                .build();
        }

        private static PosteSummaryResponse toPosteSummary(Poste p) {
                return PosteSummaryResponse.builder()
                                .id(p.getIdPoste())
                                .nom(p.getLibelle()) // colonne poste.nom
                                .isMetier(p.getIsMetier())
                                .build();
        }
}